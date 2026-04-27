package mekwars.common.campaign.clientutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
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
import megamek.server.Server;
import mekwars.common.MMGame;
import mekwars.common.campaign.Buildings;
import mekwars.common.campaign.clientutils.protocol.CConnector;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.clientutils.protocol.TransportCodec;
import mekwars.common.campaign.clientutils.protocol.commands.IProtCommand;
import mekwars.common.util.MWLogger;

public abstract class GameHost implements GameListener, IGameHost {
    public String myUsername = "";// public b/c used in RGTS command to set server status. HACK!

    protected TreeMap<String, IProtCommand> ProtCommands;

    protected IClientConfig Config;

    protected CConnector Connector;

    protected Server myServer = null;
    protected Date myTime = new Date(System.currentTimeMillis());
    protected TreeMap<String, MMGame> servers = new TreeMap<>();// hostname,mmgame
    protected Vector<String> decodeBuffer = new Vector<>(1, 1);// used to buffer incoming data until CMainFrame is built

    protected Buildings buildingTemplate = null;

    protected int savedGamesMaxDays = 30; // max number of days a save game can be before
    // its deleted.

    protected GamePhase currentPhase = GamePhase.DEPLOYMENT;
    protected int turn = 0;

    @Override
    public void gamePlayerConnected(GamePlayerConnectedEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gamePlayerDisconnected(GamePlayerDisconnectedEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gamePlayerChange(GamePlayerChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gamePlayerChat(GamePlayerChatEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (myServer != null) {
            if (turn == 0) {
                serverSend("SHS|" + getUsername() + "|Running");
            } else if ((myServer.getGame().getPhase() != currentPhase) &&
                             myServer.getGame().getOptions().booleanOption("paranoid_autosave")) {
                sendServerGameUpdate();
                currentPhase = myServer.getGame().getPhase();
            }
            turn += 1;

        }
    }

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        try {

            /*
             * Reporting phases show deaths - units that try to stand and blow their ammo, units that have ammo
             * explode from head, etc. This is also an opportune time to correct issues with the gameRemoveEntity
             * ISU's. Removals happen ASAP, even if the removal condition and final condition of the unit are
             * different (i.e. - remove on Engine crits even when a CT core comes later in the round).
             */
            sendServerGameUpdate();

        }// end try
        catch (Exception ex) {
            MWLogger.errLog("Error reporting game!");
            MWLogger.errLog(ex);
        }
    }

    @Override
    public void gameReport(GameReportEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameEnd(GameEndEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameBoardNew(GameBoardNewEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameBoardChanged(GameBoardChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameSettingsChange(GameSettingsChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameMapQuery(GameMapQueryEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameEntityNew(GameEntityNewEvent arg0) {
        // TODO Auto-generated method stub

    }

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
    public void gameEntityRemove(GameEntityRemoveEvent e) {
        // only send if the player is actually involved in the game

        // get the entity
        Entity removedE = e.getEntity();
        if (removedE.getOwner().getName().startsWith("War Bot")) {
            return;
        }

        String toSend = mekwars.common.campaign.clientutils.SerializeEntity.serializeEntity(removedE,
              true,
              false,
              isUsingAdvanceRepairs());
        serverSend("IPU|" + toSend);
    }

    @Override
    public void gameEntityChange(GameEntityChangeEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameNewAction(GameNewActionEvent arg0) {
        // TODO Auto-generated method stub

    }

    @Override
    public void gameClientFeedbackRequest(GameCFREvent arg0) {
        // TODO Auto-generated method stub

    }

    protected abstract boolean isUsingAdvanceRepairs();

    public void serverSend(String s) {
        try {
            Connector.send(IClient.PROTOCOL_PREFIX + "comm" + "\t" + TransportCodec.encode(s));
        } catch (Exception e) {
            MWLogger.errLog(e);
        }
    }

    protected abstract void sendServerGameUpdate();

    public void gameVictory(GameVictoryEvent e) {
        sendGameReport();
        MWLogger.infoLog("GAME END");
    }

    protected abstract void sendGameReport();

    public boolean isAdmin() {
        return getUser(getUsername()).getUserLevel() >= 200;
    }

    public boolean isMod() {
        return getUser(getUsername()).getUserLevel() >= 100;
    }

    public String getUsername() {
        return myUsername;
    }

    protected abstract IClientUser getUser(String name);

    public int getBuildingsLeft() {
        Enumeration<IBuilding> buildings = myServer.getGame().getBoard().getBuildings();
        int buildingCount = 0;
        while (buildings.hasMoreElements()) {
            buildings.nextElement();
            buildingCount++;
        }
        return buildingCount;
    }

    public void purgeOldLogs() {

        long daysInSeconds = ((long) savedGamesMaxDays) * 24 * 60 * 60 * 1000;

        File saveFiles = new File("./logs/backup");
        if (!saveFiles.exists()) {
            return;
        }
        File[] fileList = saveFiles.listFiles();
        for (File savedFile : fileList) {
            long lastTime = savedFile.lastModified();
            if (savedFile.exists()
                      && savedFile.isFile()
                      && (lastTime < (System.currentTimeMillis() - daysInSeconds))) {
                try {
                    MWLogger.infoLog("Purging File: "
                                           + savedFile.getName() + " Time: " + lastTime
                                           + " purge Time: "
                                           + (System.currentTimeMillis() - daysInSeconds));
                    savedFile.delete();
                } catch (Exception ex) {
                    MWLogger.errLog("Error trying to delete these files!");
                    MWLogger.errLog(ex);
                }
            }
        }
    }

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
        }

        sendChat(GameHost.CAMPAIGN_PREFIX + "c servergameoptions#" + packet);
    }

    public void sendChat(String s) {
        // Sends the content of the Chatfield to the server
        // We need the StringTokenizer to enable Mulitline comments
        StringTokenizer st = new StringTokenizer(s, "\n");

        while (st.hasMoreElements()) {
            String str = (String) st.nextElement();
            // don't send empty lines
            if (!str.trim().isEmpty()) {
                serverSend("CH|" + str);
            }
        }
    }

    public TreeMap<String, MMGame> getServers() {
        return servers;
    }

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

    public String doEscapeString(String t, int character, String replace) {

        // find all occurrences of character in t and replace them with replace
        int pos = t.indexOf(character);
        if (pos != -1) {
            String res = "";
            if (pos > 0) {
                res += t.substring(0, pos);
            }
            res += replace;
            if (pos < t.length()) {
                res += doEscapeString(t.substring(pos + 1), character, replace);
            }
            return res;
        }
        return t;
    }

    public CConnector getConnector() {
        return Connector;
    }
}
