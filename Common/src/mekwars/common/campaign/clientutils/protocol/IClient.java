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
package mekwars.common.campaign.clientutils.protocol;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import megamek.common.game.Game;
import megamek.common.options.IBasicOption;
import megamek.common.units.Entity;
import mekwars.common.AdvancedTerrain;
import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.Influences;
import mekwars.common.MMGame;
import mekwars.common.Planet;
import mekwars.common.PlanetEnvironment;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.IClientConfig;
import mekwars.common.campaign.clientutils.IClientUser;
import mekwars.common.gui.CMainFrame;
import mekwars.common.threads.ClientThread;
import mekwars.common.threads.RepairManagmentThread;
import mekwars.common.threads.SalvageManagmentThread;

/**
 * Central contract for a MekWars client: the single largest interface in the client/server architecture, and the
 * primary way the rest of the MekWars client-side code (GUI panels, dialogs, protocol handlers, campaign objects) talks
 * to "the client" without depending on a concrete implementation. It is implemented by the main GUI client class
 * ({@code mekwars.client.MWClient}) and by the headless dedicated host ({@code mekwars.dedicatedhost.MWDedHost}), both
 * of which also extend {@link mekwars.common.campaign.clientutils.GameHost}.
 * <p>
 * Broadly, an {@code IClient} implementation is responsible for:
 * <ul>
 * <li>Owning the socket connection to the campaign server (via {@link #getConnector()}, a {@link CConnector}) and
 * reacting to connection lifecycle events ({@link #connectionEstablished()}, {@link #connectionLost()}).</li>
 * <li>Parsing and dispatching incoming protocol traffic ({@link #processIncoming(String)},
 * {@link #doParseDataInput(String)}, {@link #parseDedDataInput(String)}) and outgoing chat/commands
 * ({@link #sendChat(String)}, {@link #serverSend(String)}, {@link #processGUIInput(String)}).</li>
 * <li>Exposing the logged-in user's campaign state: player ({@link #getPlayer()}), campaign data
 * ({@link #getData()}, {@link #getCampaign()}), configuration ({@link #getConfig()}), and permission level
 * ({@link #getUserLevel()}, {@link #isAdmin()}, {@link #isMod()}).</li>
 * <li>Starting/stopping an embedded MegaMek game host ({@link #startHost}, {@link #stopHost()}) and MegaMek game
 * clients ({@link #getMMClients()}, {@link #loadMegaMekClient()}).</li>
 * <li>Bridging UI concerns (refreshing panels, sounds, look-and-feel, info windows) back to whatever GUI toolkit
 * the concrete implementation uses.</li>
 * </ul>
 * Because implementations vary (full GUI client vs. dedicated host), some methods are meaningful only for one kind
 * of implementation (e.g. {@link #isDedicated()} gates dedicated-host-only behavior); callers should not assume
 * every method is fully supported by every implementation.
 */
public interface IClient {
    /**
     * The delimiter.  A tab character.
     */
    String DELIMITER = "\t";
    /** Prefix identifying a command typed into the GUI (as opposed to a raw chat message). */
    String GUI_PREFIX = "/"; // prefix for commands in GUI

    /** Status code: not connected to the campaign server. */
    int STATUS_DISCONNECTED = 0;
    /** Status code: connected but logged out (no active session). */
    int STATUS_LOGGED_OUT = 1;
    /** Status code: logged in but held in reserve (not actively fighting). */
    int STATUS_RESERVE = 2;
    /** Status code: logged in and actively available. */
    int STATUS_ACTIVE = 3;
    /** Status code: currently engaged in a game/battle. */
    int STATUS_FIGHTING = 4;

    /** GUI refresh code: refresh the connection/player status display. */
    int REFRESH_STATUS = 0;
    /** GUI refresh code: refresh the list of connected users. */
    int REFRESH_USERLIST = 1;
    /** GUI refresh code: refresh the player info panel. */
    int REFRESH_PLAYER_PANEL = 2;
    /** GUI refresh code: refresh the battle/games table. Note this is 4, not 3 — there is no code 3 in this list. */
    int REFRESH_BATTLE_TABLE = 4;
    /** GUI refresh code: refresh the headquarters (HQ) panel. */
    int REFRESH_HQ_PANEL = 5;
    /** GUI refresh code: refresh the black market panel. */
    int REFRESH_BM_PANEL = 6;

    /** Ignore-list scope: ignore public chat channel messages from the target. */
    int IGNORE_PUBLIC = 0;
    /** Ignore-list scope: ignore house (faction) channel messages from the target. */
    int IGNORE_HOUSE = 1;
    /** Ignore-list scope: ignore private messages from the target. */
    int IGNORE_PRIVATE = 2;

    /** Prefix identifying a campaign-layer command (as opposed to a raw protocol/chat command). */
    String CAMPAIGN_PREFIX = "/"; // prefix for campaign commands
    /** Relative path to the directory containing campaign data files. */
    String CAMPAIGN_PATH = "data/campaign/";
    /** Field delimiter used within a single client command's arguments. */
    String COMMAND_DELIMITER = "|"; // delimiter for client commands

    /** Field delimiter used at the wire protocol level (identical value to {@link #DELIMITER}). */
    String PROTOCOL_DELIMITER = "\t"; // delimiter for protocol commands
    /** Prefix identifying a low-level protocol command. */
    String PROTOCOL_PREFIX = "/"; // prefix for protocol commands
    /** Version string reported by this client build; bump this when releasing a new client version. */
    String CLIENT_VERSION = "0.8.0.0"; // change this with

    /**
     * If you understand this, you are a 1.1-compliant client. Following DEFLATED + DELIMITER is the number of bytes in
     * the undeflated text. This will be a maximum of 29,999, so you don't have to buffer more than that.
     * com.carnageblender.chat.net gives an example implementation.
     */
    String DEFLATED = "/deflated";

    /**
     * Called when there's a system message to show (e.g. informational/status text, as opposed to chat or errors).
     */
    void systemMessage(String message);

    /**
     * Called when there's an error message to show to the user.
     */
    void errorMessage(String message);

    /**
     * Called (typically by {@link CConnector}) when there's server input to process — a raw incoming protocol line, not
     * yet split into command/arguments.
     */
    void processIncoming(String incoming);

    /**
     * Called when the connection to the campaign server is lost (e.g. socket closed unexpectedly).
     */
    void connectionLost();

    /**
     * Called when the connection to the campaign server has been successfully established.
     */
    void connectionEstablished();

    /**
     * @return the {@link CConnector} this client uses to talk to the campaign server.
     */
    CConnector getConnector();

    /**
     * Starts an embedded MegaMek game host (server) for this client to run a battle.
     *
     * @param dedicated     true if hosting for a dedicated-host style game (no local GUI player)
     * @param deploy        true to go straight into the deployment phase
     * @param loadSavedGame true to resume from a saved game rather than starting fresh
     */
    void startHost(boolean dedicated, boolean deploy, boolean loadSavedGame);

    /**
     * @return true if this client is running as a headless dedicated host rather than a full GUI client.
     */
    boolean isDedicated();

    /**
     * Parses a raw line of incoming protocol data for a normal (GUI) client and dispatches it accordingly.
     */
    void doParseDataInput(String input);

    /**
     * Parses a raw line of incoming protocol data specifically for the dedicated-host code path.
     */
    void parseDedDataInput(String input);

    /**
     * Records the timestamp of the most recent ping, used to detect a stale/dead connection.
     *
     * @param lastPing time (typically {@code System.currentTimeMillis()}) of the last ping received/sent
     */
    void setLastPing(long lastPing);

    /**
     * Sends a chat message or client command string to the server.
     */
    void sendChat(String string);

    /**
     * @return the {@link CPlayer} representing the locally logged-in player.
     */
    CPlayer getPlayer();

    /**
     * Builds a formatted string describing an amount of money or "flu" (an in-campaign currency/resource), depending on
     * campaign settings.
     *
     * @param b  implementation-specific flag (e.g. whether to abbreviate)
     * @param b1 implementation-specific flag (e.g. whether to show a sign)
     * @param i  the amount to format
     *
     * @return the formatted money/flu message
     */
    String moneyOrFluMessage(boolean b, boolean b1, int i);

    /**
     * Overload of {@link #moneyOrFluMessage(boolean, boolean, int)} with an additional formatting flag.
     */
    String moneyOrFluMessage(boolean b, boolean b1, int i, boolean b2);

    /**
     * Looks up a cached server-side configuration value by key.
     *
     * @param rpShortName the config key to look up
     *
     * @return the cached value for that key
     */
    String getServerConfigs(String rpShortName);

    /**
     * Sets/caches a server-side configuration value locally.
     *
     * @param rpShortName the config key to set
     * @param rpValue     the value to store
     */
    void setServerConfigs(String rpShortName, String rpValue);

    /**
     * @return the {@link CampaignData} holding the static rules/reference data for the current campaign.
     */
    CampaignData getData();

    /**
     * Loads the list of banned ammo types from disk/config into memory.
     */
    void loadBannedAmmo();

    /**
     * Checks whether a given (target-)system type is currently banned by this server's rules.
     *
     * @param type the system/equipment type identifier to check
     *
     * @return true if that type is banned
     */
    boolean getTargetSystemBanStatus(int type);

    /**
     * @return the client's main GUI window ({@link CMainFrame}). May not be meaningful for headless implementations.
     */
    CMainFrame getMainFrame();

    /**
     * Loads the set of chat/protocol commands supported by the connected server.
     */
    void loadServerCommands();

    /**
     * Looks up a user (account) by name.
     *
     * @param name the username to look up
     *
     * @return the matching {@link IClientUser}, or an implementation-defined result (e.g. null) if not found
     */
    IClientUser getUser(String name);

    /**
     * Requests/loads the current black market settings from the server.
     */
    void getBlackMarketSettings();

    /**
     * @return the map of internal equipment name to {@link Equipment} available on the black market.
     */
    Map<String, Equipment> getBlackMarketEquipmentList();

    /**
     * Reloads the client's campaign data from disk/server.
     */
    void reloadData();

    /**
     * Requests the full set of server configuration values from the server.
     */
    void getServerConfigData();

    /**
     * Sends a single server configuration key/value pair up to the server.
     *
     * @param config the config key
     * @param text   the value to set
     */
    void putServerConfigs(String config, String text);

    /**
     * Refreshes locally cached data from the server/campaign state.
     */
    void refreshData();

    /**
     * Appends a line to the default chat display.
     */
    void addToChat(String s);

    /**
     * Appends a line to a specific chat channel's display.
     *
     * @param s       the text to append
     * @param channel the channel identifier to append to
     */
    void addToChat(String s, int channel);

    /**
     * @return the {@link IClientConfig} holding this client's persisted settings.
     */
    IClientConfig getConfig();

    /**
     * Advances any time-based client-side processing by the given tick amount (e.g. countdown timers).
     *
     * @param time the elapsed time, in an implementation-defined unit (commonly milliseconds or seconds)
     */
    void processTick(int time);

    /**
     * @return the board edge the player's forces should start deployment from.
     */
    int getPlayerStartingEdge();

    /**
     * Sets the board edge the player's forces should start deployment from.
     *
     * @param edge the edge identifier (implementation/MegaMek-defined constant)
     */
    void setPlayerStartingEdge(int edge);

    /**
     * @return true if this client's campaign uses the "advance repairs" ruleset (affects repair cost/detail
     *       calculations and entity-status serialization; see
     *       {@link mekwars.common.campaign.clientutils.SerializeEntity}).
     */
    boolean isUsingAdvanceRepairs();

    /**
     * Looks up a named client-side config parameter (delegates to {@link IClientConfig#getParam(String)}).
     *
     * @param primaryHQSortOrder the config key to look up (name reflects a common caller, not a fixed key)
     *
     * @return the config value
     */
    String getConfigParam(String primaryHQSortOrder);

    /**
     * @return the username of the currently logged-in account.
     */
    String getUsername();

    /**
     * Sets the username of the currently logged-in account.
     */
    void setUsername(String name);

    /**
     * Refreshes cached operations/faction ("Op") data, optionally forcing a full reload.
     *
     * @param b true to force a full refresh rather than an incremental one
     */
    void updateOpData(boolean b);

    /**
     * @return the current status code for this client (one of the {@code STATUS_*} constants).
     */
    int getMyStatus();

    /**
     * @return the list of currently known users ({@link CUser}) on the server.
     */
    List<CUser> getUsers();

    /**
     * @return a map of operation ("Op") short name to its associated data array.
     */
    TreeMap<String, String[]> getAllOps();

    /**
     * @return true if the client is currently blocked waiting on a server response (see {@link #setWaiting(boolean)}).
     */
    boolean isWaiting();

    /**
     * Sets whether the client is blocked waiting on a server response. Callers elsewhere in the codebase (e.g.
     * {@code BuildTableViewer.run()}) busy-wait in a sleep loop checking {@link #isWaiting()} until this is cleared.
     *
     * @param b true to enter the waiting state, false to clear it
     */
    void setWaiting(boolean b);

    /**
     * @return the path to this client's local cache directory.
     */
    String getCacheDir();

    /**
     * Loads server-defined "trait" files used by the campaign rules.
     */
    void loadServerTraitFiles();

    /**
     * Loads/initializes the embedded MegaMek game client used to actually play battles.
     */
    void loadMegaMekClient();

    /**
     * (Re)initializes this client's {@link IClientConfig} instance.
     */
    void setConfig();

    /**
     * @return the permission/access level of the currently logged-in user (see {@link IClientUser#getUserLevel()}).
     */
    int getUserLevel();

    /**
     * @return the {@link RepairManagmentThread} that processes background repair-queue work for this client.
     */
    RepairManagmentThread getRMT();

    /**
     * Looks up the campaign-adjusted cost of a piece of ammunition by its internal MegaMek name.
     *
     * @param internalName the ammo's internal MegaMek identifier
     *
     * @return the computed cost
     */
    double getAmmoCost(String internalName);

    /**
     * @return the {@link SalvageManagmentThread} that processes background salvage-queue work for this client.
     */
    SalvageManagmentThread getSMT();

    /**
     * @return true if the current user has at least moderator-level permissions (see
     *       {@link mekwars.common.campaign.clientutils.GameHost#isMod()}).
     */
    boolean isMod();

    /**
     * @return true if the current user has administrator-level permissions (see
     *       {@link mekwars.common.campaign.clientutils.GameHost#isAdmin()}).
     */
    boolean isAdmin();

    /**
     * @return the {@link CCampaign} representing the overall campaign state.
     */
    CCampaign getCampaign();

    /**
     * Sets the password to use when authenticating with the server.
     */
    void setPassword(String s);

    /**
     * Sets the chat ignore-list scope to "house" (see {@link #IGNORE_HOUSE}).
     */
    void setIgnoreHouse();

    /**
     * Sets the chat ignore-list scope to "private" (see {@link #IGNORE_PRIVATE}).
     */
    void setIgnorePrivate();

    /**
     * Sets the chat ignore-list scope to "public" (see {@link #IGNORE_PUBLIC}).
     */
    void setIgnorePublic();

    /**
     * (Re)loads the set of chat keyword filters this client watches for (see {@link #hasKeyWords(String)}).
     */
    void setKeyWords();

    /**
     * Switches the GUI's look-and-feel.
     *
     * @param b implementation-defined flag selecting which look-and-feel to use (e.g. native vs. cross-platform)
     */
    void setLookAndFeel(boolean b);

    /**
     * Displays a modal/non-modal informational window with the given text.
     */
    void showInfoWindow(String s);

    /**
     * @return the MegaMek {@link Game} currently associated with this client (rules/board/entity state).
     */
    Game getGame();

    /**
     * Sets the advanced terrain configuration used for the current/next map.
     */
    void setAdvancedTerrain(AdvancedTerrain aTerrain);

    /**
     * Requests a GUI refresh of the given panel/section (one of the {@code REFRESH_*} constants).
     *
     * @param refreshHqPanel the refresh code identifying what to redraw
     */
    void refreshGUI(int refreshHqPanel);

    /**
     * Checks whether a given user is on this client's ignore list for the given scope.
     *
     * @param name        the username to check
     * @param ignoreHouse the ignore scope (one of the {@code IGNORE_*} constants, despite the parameter name always
     *                    referring to "house")
     *
     * @return true if messages from that user in that scope should be ignored
     */
    boolean isIgnored(String name, int ignoreHouse);

    /**
     * @return the current time formatted as a short display string (e.g. for chat timestamps).
     */
    String getShortTime();

    /**
     * Plays a named sound effect, if sound is enabled and not muted.
     */
    void doPlaySound(String soundName);

    /**
     * Changes this client's published status to the given code (one of the {@code STATUS_*} constants) and notifies the
     * server.
     */
    void changeStatus(int i);

    /**
     * @return the vector of MegaMek game options ({@link IBasicOption}) currently in effect.
     */
    Vector<IBasicOption> getGameOptions();

    /**
     * Sets the current planetary environment and map dimensions/medium for the game about to be played.
     *
     * @param planetEnvironment the environmental conditions (weather, light, etc.) to apply
     * @param dimension         the board size
     * @param mapMedium         the map medium/terrain type identifier
     */
    void setEnvironment(PlanetEnvironment planetEnvironment, Dimension dimension, int mapMedium);

    /**
     * Sends a raw command string to the campaign server (see
     * {@link mekwars.common.campaign.clientutils.GameHost#serverSend(String)} for the typical implementation).
     */
    void serverSend(String s);

    /**
     * Starts (or reconnects) the embedded MegaMek game client under the given player name.
     *
     * @param curName the player name to connect as
     * @param b       implementation-specific flag (e.g. whether this is a reconnect)
     */
    void startClient(String curName, boolean b);

    /**
     * @return the map of hostname to {@link MMGame} for every MekWars game server known to this client.
     */
    TreeMap<String, MMGame> getServers();

    /**
     * Stops the embedded MegaMek game host this client is running, if any.
     */
    void stopHost();

    /**
     * @return true if this client's embedded MegaMek server is currently running.
     */
    boolean isServerRunning();

    /**
     * Performs client shutdown/logout cleanup (closing connections, saving state, etc.) before exiting.
     */
    void goodbye();

    /**
     * @return the list of active {@link ClientThread}s connecting this client to in-progress MegaMek games.
     */
    List<ClientThread> getMMClients();

    /**
     * @return true if the local player is the leader/host of the current game/lobby.
     */
    boolean isLeader();

    /**
     * Shows the dialog for spending/viewing reward points.
     */
    void rewardPointsDialog();

    /**
     * Shows the dialog for spending/viewing influence points.
     */
    void influencePointsDialog();

    /**
     * Mutes or unmutes all client sound effects.
     */
    void setSoundMuted(boolean state);

    /**
     * Connects to the campaign server using previously configured connection settings.
     */
    void connectToServer();

    /**
     * Connects to the campaign server at the given address and port.
     */
    void connectToServer(String ip, int port);

    /**
     * @return a short, human-readable description of the client's current connection/session status.
     */
    String getStatus();

    /**
     * Processes a line of input typed by the user into the GUI (chat box or command line), dispatching it as a command
     * or plain chat as appropriate.
     */
    void processGUIInput(String s);

    /**
     * @return the current board/map size.
     */
    Dimension getMapSize();

    /**
     * @return the {@link PlanetEnvironment} currently in effect for the game being set up/played.
     */
    PlanetEnvironment getCurrentEnvironment();

    /**
     * @return the {@link Buildings} template describing buildings available on the current map.
     */
    Buildings getBuildingTemplate();

    /**
     * Sets the {@link Buildings} template to use for the current map.
     */
    void setBuildingTemplate(Buildings building);

    /**
     * @return the identifier of the current map medium/terrain type.
     */
    int getMapMedium();

    /**
     * @return the {@link AdvancedTerrain} configuration currently in effect.
     */
    AdvancedTerrain getCurrentAdvancedTerrain();

    /**
     * @return true if bot-controlled forces are enabled for this client's games.
     */
    boolean isUsingBots();

    /**
     * Enables or disables bot-controlled forces for this client's games.
     */
    void setUsingBots(boolean b);

    /**
     * HTML-escapes a string (see {@link mekwars.common.campaign.clientutils.GameHost#doEscape(String)} for the typical
     * string-returning counterpart). Note the unusual {@code int} return type for what is conceptually a
     * string-transforming operation — check the implementation for what value is actually returned.
     *
     * @param string the string to escape
     *
     * @return an implementation-defined integer result
     */
    String doEscape(String string);

    /**
     * Appends a line to a chat channel, tagging it with the originating server name (used in multi-server displays).
     *
     * @param s           the text to append
     * @param channelMail the channel identifier to append to
     * @param server      the name of the server the message originated from
     */
    void addToChat(String s, int channelMail, String server);

    /**
     * Checks whether a string contains any of this client's configured chat keyword filters (see
     * {@link #setKeyWords()}).
     */
    boolean hasKeyWords(String string);

    /**
     * Applies any pending client-side updates (e.g. after downloading a new version's data).
     */
    void updateClient();

    /**
     * Requests operations ("Op") data for a given short name from the server.
     *
     * @param aShort the operation's short name/identifier
     * @param s      additional implementation-defined request parameter
     */
    void retrieveOpData(String aShort, String s);

    /**
     * Applies an incoming "update parameter" protocol command, parsed from the given tokenizer.
     */
    void updateParam(StringTokenizer st);

    /**
     * Applies incoming server operation-flag settings, parsed from the given tokenizer.
     */
    void setServerOpFlags(StringTokenizer st);

    /**
     * Updates the cached black-market parts listing for a given campaign year.
     *
     * @param s            implementation-defined update payload
     * @param campaignYear the in-campaign year the update applies to
     */
    void updatePartsBlackMarket(String s, int campaignYear);

    /**
     * Updates the locally cached player parts inventory from an incoming update payload.
     */
    void updatePlayerPartsCache(String s);

    /**
     * Retrieves an MUL (MegaMek Unit List) file from the server by name/identifier.
     */
    void retrieveMul(String s);

    /**
     * Applies an incoming "create new house" (faction) protocol command, parsed from the given tokenizer.
     */
    void createNewHouse(StringTokenizer st);

    /**
     * Computes a checksum for a filename/string, used to validate file transfers/caches.
     *
     * @param s the input to checksum
     *
     * @return the computed checksum string
     *
     * @throws Exception if the checksum cannot be computed (e.g. algorithm unavailable, I/O error)
     */
    String createFilenameChecksum(String s) throws Exception;

    /**
     * @return the text of the last chat/user-list query issued, used to support incremental/auto-complete lookups.
     */
    String getLastQuery();

    /**
     * Records the text of the last chat/user-list query issued (see {@link #getLastQuery()}).
     */
    void setLastQuery(String name);

    /**
     * Finds usernames that partially match the given text, for chat auto-completion.
     *
     * @param text the partial username typed so far
     *
     * @return the list of matching usernames
     */
    ArrayList<String> getPartialUser(String text);

    /**
     * @return the map of house/faction ID to {@link Influences} changes accumulated since the last GUI refresh.
     */
    Map<Integer, Influences> getChangesSinceLastRefresh();

    /**
     * @return the minimum ownership percentage/threshold required to be considered an owner of the given planet.
     */
    int getMinPlanetOwnerShip(Planet planet);

    /**
     * Computes the technician labor cost (in hours or points, implementation-defined) to repair/build the given entity
     * at the given tech skill level.
     *
     * @param entity    the unit being worked on
     * @param techGreen the technician's skill level (name suggests a "green"/rookie skill constant, but the actual
     *                  meaning is defined by the implementation)
     *
     * @return the computed labor cost
     */
    int getTechLaborCosts(Entity entity, int techGreen);

    /**
     * Computes the total monetary cost to fully repair the given entity.
     */
    double getTotalRepairCosts(Entity entity);

    /**
     * @return true if bot-controlled forces are configured to be placed on the same team as their controlling player,
     *       rather than as a separate opposing side.
     */
    boolean isBotsOnSameTeam();

    /**
     * Sets whether bot-controlled forces are placed on the same team as their controlling player. Takes a boxed
     * {@link Boolean} rather than a primitive {@code boolean}.
     */
    void setBotsOnSameTeam(Boolean aBoolean);

    /**
     * Notifies the client that a hosted game on the given server is closing down, so it can perform any related
     * cleanup.
     */
    void closingGame(String serverName);
}
