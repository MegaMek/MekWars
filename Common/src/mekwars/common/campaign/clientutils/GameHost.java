/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekwars.common.campaign.clientutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.enums.GamePhase;
import megamek.common.event.*;
import megamek.common.event.board.GameBoardChangeEvent;
import megamek.common.event.board.GameBoardNewEvent;
import megamek.common.event.entity.GameEntityChangeEvent;
import megamek.common.event.entity.GameEntityNewEvent;
import megamek.common.event.entity.GameEntityNewOffboardEvent;
import megamek.common.event.entity.GameEntityRemoveEvent;
import megamek.common.event.player.GamePlayerChangeEvent;
import megamek.common.event.player.GamePlayerChatEvent;
import megamek.common.event.player.GamePlayerConnectedEvent;
import megamek.common.event.player.GamePlayerDisconnectedEvent;
import megamek.common.units.Entity;
import megamek.common.units.IBuilding;
import megamek.logging.MMLogger;
import megamek.server.Server;
import mekwars.common.MMGame;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.commands.IProtCommand;

/**
 * Base class for anything in MekWars that hosts a MegaMek {@link Server} instance and needs to talk back to the
 * MekWars campaign server about the game in progress (e.g. reporting unit destruction/salvage, auto-saving,
 * relaying chat). It implements MegaMek's {@link GameListener} so it can be registered as a listener on the embedded
 * {@link Server}'s {@link megamek.common.game.Game}, and implements {@link IGameHost} so other MekWars code can
 * query host status/permissions without depending on a concrete subclass.
 * <p>
 * Concrete subclasses (e.g. the full GUI client and the headless dedicated host) must supply the abstract hooks
 * ({@link #isUsingAdvanceRepairs()}, {@link #sendServerGameUpdate()}, {@link #sendGameReport()},
 * {@link #getUser(String)}) and are responsible for actually starting/stopping the embedded {@link #myServer}.
 * <p>
 * Most of the {@link GameListener} callbacks here are unimplemented stubs (see individual methods) — only the
 * handlers that MekWars actually cares about (turn/phase change, entity removal, victory) do real work.
 */
public abstract class GameHost implements GameListener, IGameHost {
    private static final MMLogger LOGGER = MMLogger.create(GameHost.class);

    /**
     * The username of the account running this host. Public (rather than private/protected) because the RGTS
     * command reaches in directly to set server status — a known hack, not idiomatic encapsulation.
     */
    public String myUsername = "";// public b/c used in RGTS command to set server status. HACK!

    /** Registry of protocol commands ("/..." style commands) this host understands, keyed by command name. */
    protected TreeMap<String, IProtCommand> ProtCommands;

    /** The client-side configuration store (settings loaded from mwconfig.txt). */
    protected IClientConfig Config;

    /** The socket connection wrapper used to talk to the MekWars campaign server. */
    protected CConnector Connector;

    /** The embedded MegaMek game server this host is running, or null if no game is currently hosted. */
    protected Server myServer = null;
    /** Map of hostname to {@link MMGame} for every MekWars game server known to this host. */
    protected TreeMap<String, MMGame> servers = new TreeMap<>();// hostname,mmgame
    /** Buffers incoming protocol data until the client's main GUI frame (CMainFrame) has finished being built. */
    protected Vector<String> decodeBuffer = new Vector<>(1, 1);// used to buffer incoming data until CMainFrame is built

    /** Template describing the buildings available for the current map/game, used when reporting building state. */
    protected Buildings buildingTemplate = null;

    /** Maximum age, in days, of a saved-game backup file before {@link #purgeOldLogs()} deletes it. */
    protected int savedGamesMaxDays = 30; // max number of days a save game can be before
    // its deleted.

    /** The last game phase observed by {@link #gameTurnChange(GameTurnChangeEvent)}, used to detect phase changes. */
    protected GamePhase currentPhase = GamePhase.DEPLOYMENT;
    /** Number of turns that have elapsed in the currently hosted game. */
    protected int turn = 0;

    /**
     * {@link GameListener} callback for a player connecting to the embedded MegaMek server. Currently unimplemented
     * (no-op stub) — MekWars does not react to this event directly.
     */
    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback for a player disconnecting from the embedded MegaMek server. Currently
     * unimplemented (no-op stub).
     */
    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback for a change to a player's game-side state (team, color, etc.). Currently
     * unimplemented (no-op stub).
     */
    @Override
    public void gamePlayerChange(GamePlayerChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback for in-game chat sent through the MegaMek server. Currently unimplemented
     * (no-op stub); MekWars chat is instead routed through {@link #sendChat(String)}/{@link #serverSend(String)}.
     */
    @Override
    public void gamePlayerChat(GamePlayerChatEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when the game advances to a new turn. On the very first turn (turn 0)
     * this reports the host as "Running" to the campaign server. On subsequent turns, if the "paranoid_autosave"
     * game option is enabled and the game phase has changed since the last check, it triggers a full server game
     * update via {@link #sendServerGameUpdate()}. {@link #turn} is incremented unconditionally on every call
     * (including the very first) as long as {@link #myServer} is non-null.
     */
    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (myServer != null) {

            if (turn == 0) {
                serverSend(String.format("SHS|%s|Running", getUsername()));
            } else if ((myServer.getGame().getPhase() != currentPhase) &&
                             myServer.getGame().getOptions().booleanOption("paranoid_autosave")) {
                sendServerGameUpdate();
                currentPhase = myServer.getGame().getPhase();
            }

            turn += 1;

        }
    }

    /**
     * Callback fired when the game phase changes (note: unlike the other listener callbacks in this class, this one
     * is not marked {@code @Override} in the source, though it is still invoked by the same event-dispatch
     * mechanism). Always sends a full server game update via {@link #sendServerGameUpdate()} — this is the main
     * auto-reporting hook, since reporting phases are when unit deaths/destruction become visible. Any exception
     * raised while reporting is caught and logged rather than propagated.
     */
    public void gamePhaseChange(GamePhaseChangeEvent e) {
        try {

            /*
             * Reporting phases show deaths - units that try to stand and blow their ammo, units that have ammo
             * explode from the head, etc. This is also an opportune time to correct issues with the gameRemoveEntity
             * ISU's. Removals happen ASAP, even if the removal condition and final condition of the unit are
             * different (i.e. - remove on Engine crits even when a CT core comes later in the round).
             */
            sendServerGameUpdate();

        }// end try
        catch (Exception ex) {
            LOGGER.error(ex, "Error reporting game: {}", ex.getMessage());
        }
    }

    /**
     * {@link GameListener} callback for a textual game report becoming available. Currently unimplemented (no-op
     * stub); MekWars instead builds its own report via {@link #sendGameReport()}.
     */
    @Override
    public void gameReport(GameReportEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when the MegaMek game ends. Currently unimplemented (no-op stub); note
     * this is distinct from {@link #gameVictory(GameVictoryEvent)}, which is where MekWars actually reacts to the
     * game concluding.
     */
    @Override
    public void gameEnd(GameEndEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when a new game board is loaded. Currently unimplemented (no-op stub).
     */
    @Override
    public void gameBoardNew(GameBoardNewEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when the game board changes (e.g. terrain/building damage). Currently
     * unimplemented (no-op stub).
     */
    @Override
    public void gameBoardChanged(GameBoardChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when the game's rule options/settings change. Currently unimplemented
     * (no-op stub).
     */
    @Override
    public void gameSettingsChange(GameSettingsChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired for a map-related query from the server. Currently unimplemented (no-op
     * stub).
     */
    @Override
    public void gameMapQuery(GameMapQueryEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when a new entity (unit) is added to the game. Currently unimplemented
     * (no-op stub).
     */
    @Override
    public void gameEntityNew(GameEntityNewEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when a new off-board entity (e.g. off-board artillery) is added to the
     * game. Currently unimplemented (no-op stub).
     */
    @Override
    public void gameEntityNewOffboard(GameEntityNewOffboardEvent arg0) {
        // TODO Auto-generated method stub

    }

    /*
     * When an entity is removed from play, check the reason. If the unit is ejected, captured or devastated and the
     * player is involved in the game at hand, report the removal to the server. The server stores these reports in
     * pilotTree and deathTree to auto-resolve games after a player disconnects. NOTE: This sends the first possible
     * removal condition, which means that a unit which is simultaneously head killed and then CT cored will show as
     * salvageable.
     */
    /**
     * {@link GameListener} callback fired whenever an entity leaves play (destroyed, ejected, captured, fled,
     * etc.). Skips reporting for units owned by any "War Bot" (bot-controlled) player. For every other removal, it
     * serializes the entity's in-progress status (see {@link SerializeEntity#serializeEntity}) and sends it to the
     * campaign server as an "IPU" (in-progress update) command via {@link #serverSend(String)}. See the caveat
     * above: because only the first removal condition is reported, a unit that is both head-killed and later
     * CT-cored in the same resolution can be reported as merely "salvageable".
     *
     * @param e the removal event, including the removed entity and its removal condition
     */
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // only send if the player is actually involved in the game

        // get the entity
        Entity removedE = e.getEntity();
        if (removedE.getOwner().getName().startsWith("War Bot")) {
            return;
        }

        String toSend = SerializeEntity.serializeEntity(removedE, true, false, isUsingAdvanceRepairs());
        serverSend(String.format("IPU|%s", toSend));
    }

    /**
     * {@link GameListener} callback fired when an entity's state changes (e.g. movement, damage). Currently
     * unimplemented (no-op stub).
     */
    @Override
    public void gameEntityChange(GameEntityChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link GameListener} callback fired when a new game action (e.g. attack, movement) is submitted. Currently
     * unimplemented (no-op stub).
     */
    @Override
    public void gameNewAction(GameNewActionEvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * {@link IGameHost}/{@link GameListener} callback fired when the server requests feedback from a client
     * (Client Feedback Request). Currently unimplemented (no-op stub) at this level; subclasses that need to
     * respond to CFRs must override this.
     */
    @Override
    public void gameClientFeedbackRequest(GameCFREvent arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * @return whether this host's campaign is using the "advance repairs" ruleset, which affects how much repair
     *         detail is included when serializing entity status. Must be supplied by subclasses.
     */
    protected abstract boolean isUsingAdvanceRepairs();

    /**
     * Encodes and sends a raw command string to the campaign server over {@link #Connector}. The message is
     * Base64-encoded (see {@link CConnector#encode(String)}) and wrapped in the protocol's "comm" envelope. Any
     * exception during sending is caught and logged rather than propagated (a send failure is silently swallowed
     * from the caller's perspective).
     *
     * @param s the raw command payload to send
     */
    public void serverSend(String s) {
        try {
            Connector.send(String.format("%scomm\t%s", IClient.PROTOCOL_PREFIX, CConnector.encode(s)));
        } catch (Exception e) {
            LOGGER.error(e, "Error sending to server: {}", e.getMessage());
        }
    }

    /**
     * Builds and sends a full game-state update to the campaign server (used for auto-saving/reporting on turn and
     * phase changes). Must be supplied by subclasses.
     */
    protected abstract void sendServerGameUpdate();

    /**
     * {@link GameListener} callback fired when the game ends in victory. Triggers the final game report via
     * {@link #sendGameReport()} and logs that the game has ended.
     */
    public void gameVictory(GameVictoryEvent event) {
        sendGameReport();
        LOGGER.info("GAME END");
    }

    /**
     * Builds and sends the end-of-game report (kills, salvage, etc.) to the campaign server. Must be supplied by
     * subclasses.
     */
    protected abstract void sendGameReport();

    /**
     * @return true if the current user ({@link #getUsername()}) has a user level of 200 or higher, the convention
     *         this codebase uses for "administrator".
     */
    public boolean isAdmin() {
        return getUser(getUsername()).getUserLevel() >= 200;
    }

    /**
     * @return true if the current user ({@link #getUsername()}) has a user level of 100 or higher, the convention
     *         this codebase uses for "moderator or above" (admins also satisfy this check).
     */
    public boolean isMod() {
        return getUser(getUsername()).getUserLevel() >= 100;
    }

    /**
     * @return the username of the account running this host (see {@link #myUsername}).
     */
    public String getUsername() {
        return myUsername;
    }

    /**
     * Looks up a user by name. Must be supplied by subclasses, which typically hold the authoritative user list.
     *
     * @param name the username to look up
     * @return the matching user, or an implementation-defined result (e.g. null) if not found
     */
    protected abstract IClientUser getUser(String name);

    /**
     * @return the number of buildings still standing on the current game board, counted by exhausting the board's
     *         building enumeration.
     */
    public int getBuildingsLeft() {
        Enumeration<IBuilding> buildings = myServer.getGame().getBoard().getBuildings();
        int buildingCount = 0;
        while (buildings.hasMoreElements()) {
            buildings.nextElement();
            buildingCount++;
        }
        return buildingCount;
    }

    /**
     * Deletes saved-game backup files under {@code ./logs/backup} that are older than
     * {@link #savedGamesMaxDays}. Does nothing if the backup directory does not exist. Note: despite the variable
     * name {@code daysInSeconds}, the value computed is actually in milliseconds (days * 24 * 60 * 60 * 1000), which
     * matches {@link File#lastModified()}'s millisecond epoch time — the name is simply misleading, not a bug.
     * Deletion failures for individual files are caught and logged, not propagated.
     */
    public void purgeOldLogs() {

        long daysInSeconds = ((long) savedGamesMaxDays) * 24 * 60 * 60 * 1000;

        File saveFiles = new File("./logs/backup");
        if (!saveFiles.exists()) {
            return;
        }
        File[] fileList = saveFiles.listFiles();
        if (fileList != null) {
            for (File savedFile : fileList) {
                long lastTime = savedFile.lastModified();
                if (savedFile.exists()
                          && savedFile.isFile()
                          && (lastTime < (System.currentTimeMillis() - daysInSeconds))) {
                    try {
                        LOGGER.info(String.format("Purging File: %s Time: %s purge Time: %s", savedFile.getName(), lastTime, System.currentTimeMillis() -
                                                    daysInSeconds));
                        savedFile.delete();
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Error trying to delete these files! {}", savedFile.getName());
                    }
                }
            }
        }
    }

    /**
     * Reads the local {@code ./mmconf/gameoptions.xml} file line by line, joins the lines with "#" separators, and
     * sends the resulting blob to the campaign server as a "servergameoptions" campaign chat command (via
     * {@link #sendChat(String)}), so the server can record what game options this host is using. Read failures are
     * caught and logged; on failure the (possibly partial or empty) packet built so far is still sent.
     */
    public void sendGameOptionsToServer() {
        StringBuilder packet = new StringBuilder();

        try {
            FileInputStream gameOptionsFile = new FileInputStream("./mmconf/gameoptions.xml");
            BufferedReader gameOptions = new BufferedReader(new InputStreamReader(gameOptionsFile));

            while (gameOptions.ready()) {
                packet.append(gameOptions.readLine()).append("#");
            }
            gameOptions.close();
            gameOptionsFile.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Error sending game options to the server: {}", ex.getLocalizedMessage());
        }

        sendChat(String.format("%sc servergameoptions#%s", IClient.CAMPAIGN_PREFIX, packet));
    }

    /**
     * Sends a (possibly multi-line) chat/command string to the server, one line at a time, via
     * {@link #serverSend(String)} wrapped in a "CH|" (chat) command. Splits on newlines using a
     * {@link StringTokenizer} to support multi-line input; blank/whitespace-only lines are silently dropped and not
     * sent.
     *
     * @param s the chat text or command string to send, possibly containing embedded newlines
     */
    public void sendChat(String s) {
        // Sends the content of the Chatfield to the server
        // We need the StringTokenizer to enable Mulitline comments
        StringTokenizer st = new StringTokenizer(s, "\n");

        while (st.hasMoreElements()) {
            String str = (String) st.nextElement();
            // don't send empty lines
            if (!str.trim().isEmpty()) {
                serverSend(String.format("CH|%s", str));
            }
        }
    }

    /**
     * @return the map of hostname to {@link MMGame} for every MekWars game server known to this host.
     */
    public TreeMap<String, MMGame> getServers() {
        return servers;
    }

    /**
     * HTML-escapes a chat string so that raw HTML tags cannot be injected into the client's chat display (an XSS
     * safeguard). As a special case, strings already containing a MekWars-generated {@code <a href="MEKINFO"} link
     * (used for clickable unit-info links in chat) are returned unescaped, so that internal MekWars markup keeps
     * working. Otherwise escapes {@code &}, {@code <}, and {@code >} via {@link #doEscapeString}.
     *
     * @param str the raw string to escape
     * @return the escaped string, or the original string unchanged if it contains a MEKINFO link
     */
    public String doEscape(String str) {

        if (str.contains("<a href=\"MEKINFO")) {
            return str;
        }

        // This function removes HTML Tags from the Chat, so no code may harm
        // anyone
        str = doEscapeString(str, '&', "&amp;");
        str = doEscapeString(str, '<', "&lt;");
        str = doEscapeString(str, '>', "&gt;");
        return str;
    }

    /**
     * Recursively replaces every occurrence of a single character in a string with a replacement string. Implemented
     * via recursion on the substring following each match rather than iteration, so very long strings with many
     * matches could in principle risk deep recursion/stack growth.
     *
     * @param t         the string to search
     * @param character the character (as an int code point) to replace
     * @param replace   the replacement string to substitute for each occurrence
     * @return a new string with all occurrences replaced
     */
    public String doEscapeString(String t, int character, String replace) {

        // find all occurrences of character in t and replace them with replacement
        int pos = t.indexOf(character);
        if (pos != -1) {
            String res = "";
            if (pos > 0) {
                res += t.substring(0, pos);
            }
            res += replace;
            res += doEscapeString(t.substring(pos + 1), character, replace);
            return res;
        }
        return t;
    }

    /**
     * @return the {@link CConnector} used to communicate with the campaign server.
     */
    public CConnector getConnector() {
        return Connector;
    }
}
