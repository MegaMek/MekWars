package common.campaign.clientutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import common.CampaignData;
import common.MMGame;
import common.campaign.Buildings;
import common.campaign.clientutils.protocol.CConnector;
import common.campaign.clientutils.protocol.IClient;
import common.campaign.clientutils.protocol.TransportCodec;
import common.campaign.clientutils.protocol.commands.IProtCommand;
import common.util.MWLogger;
import megamek.common.Building;
import megamek.common.Game;
import megamek.common.IGame;
import megamek.common.enums.GamePhase;
import megamek.common.event.GameBoardChangeEvent;
import megamek.common.event.GameBoardNewEvent;
import megamek.common.event.GameCFREvent;
import megamek.common.event.GameEndEvent;
import megamek.common.event.GameEntityChangeEvent;
import megamek.common.event.GameEntityNewEvent;
import megamek.common.event.GameEntityNewOffboardEvent;
import megamek.common.event.GameEntityRemoveEvent;
import megamek.common.event.GameListener;
import megamek.common.event.GameMapQueryEvent;
import megamek.common.event.GameNewActionEvent;
import megamek.common.event.GamePhaseChangeEvent;
import megamek.common.event.GamePlayerChangeEvent;
import megamek.common.event.GamePlayerChatEvent;
import megamek.common.event.GamePlayerConnectedEvent;
import megamek.common.event.GamePlayerDisconnectedEvent;
import megamek.common.event.GameReportEvent;
import megamek.common.event.GameSettingsChangeEvent;
import megamek.common.event.GameTurnChangeEvent;
import megamek.common.event.GameVictoryEvent;
import megamek.server.Server;

public abstract class GameHost implements GameListener, IGameHost {
    public static final int STATUS_DISCONNECTED = 0;
    public static final int STATUS_LOGGEDOUT = 1;
    public static final int STATUS_RESERVE = 2;
    public static final int STATUS_ACTIVE = 3;
    public static final int STATUS_FIGHTING = 4;
    
    public static final String CAMPAIGN_PREFIX = "/"; // prefix for campaign commands
    public static final String CAMPAIGN_PATH = "data/campaign/";
    public static final String COMMAND_DELIMITER = "|"; // delimiter for client commands
    
    public String myUsername = "";// public b/c used in RGTS command to set server status. HACK!
    
    protected TreeMap<String, IProtCommand> ProtCommands;
    
    protected IClientConfig Config;

    protected CConnector Connector;
    
    protected Server myServer = null;
    protected Date mytime = new Date(System.currentTimeMillis());
    protected TreeMap<String, MMGame> servers = new TreeMap<String, MMGame>();// hostname,mmgame
    protected Vector<String> decodeBuffer = new Vector<String>(1, 1);// used to buffer incoming data until CMainFrame is built

    protected Buildings buildingTemplate = null;
    
    protected int savedGamesMaxDays = 30; // max number of days a save game can be before
    // its deleted.
    
    protected GamePhase currentPhase = GamePhase.DEPLOYMENT;
    protected int turn = 0;
    
	@Override
	public void gameBoardChanged(GameBoardChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameBoardNew(GameBoardNewEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameClientFeedbackRquest(GameCFREvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEnd(GameEndEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameEntityChange(GameEntityChangeEvent arg0) {
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
     * When an entity is removed from play, check the reason. If the unit is
     * ejected, captured or devestated and the player is invovled in the game at
     * hand, report the removal to the server. The server stores these reports
     * in pilotTree and deathTree in order to auto-resolve games after a player
     * disconnects. NOTE: This send thefirst possible removal condition, which
     * means that a unit which is simultanously head killed and then CT cored
     * will show as salvageable.
     */
    public void gameEntityRemove(GameEntityRemoveEvent e) {// only send if the
        // player is
        // actually involved
        // in the game

        // get the entity
        megamek.common.Entity removedE = e.getEntity();
        if (removedE.getOwner().getName().startsWith("War Bot")) {
            return;
        }

        String toSend = SerializeEntity.serializeEntity(removedE, true, false, isUsingAdvanceRepairs());
        serverSend("IPU|" + toSend);
    }

	protected abstract boolean isUsingAdvanceRepairs();

	@Override
	public void gameMapQuery(GameMapQueryEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameNewAction(GameNewActionEvent arg0) {
		// TODO Auto-generated method stub
		
	}

    public void gamePhaseChange(GamePhaseChangeEvent e) {
        try {

            /*
             * Reporting phases show deaths - units that try to stand and blow
             * their ammo, units that have ammo explode from head, etc. This is
             * also an opportune time to correct isses with the gameRemoveEntity
             * ISU's. Removals happen ASAP, even if the removal condition and
             * final condition of the unit are not the same (ie - remove on
             * Engine crits even when a CT core comes later in the round).
             */
            sendServerGameUpdate();

        }// end try
        catch (Exception ex) {
            MWLogger.errLog("Error reporting game!");
            MWLogger.errLog(ex);
        }
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
	public void gamePlayerConnected(GamePlayerConnectedEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gamePlayerDisconnected(GamePlayerDisconnectedEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameReport(GameReportEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void gameSettingsChange(GameSettingsChangeEvent arg0) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void gameTurnChange(GameTurnChangeEvent e) {
        if (myServer != null) {
            if (turn == 0) {
                serverSend("SHS|" + getUsername() + "|Running");
            } else if ((myServer.getGame().getPhase() != currentPhase)
                    && myServer.getGame().getOptions()
                            .booleanOption("paranoid_autosave")) {
                sendServerGameUpdate();
                currentPhase = myServer.getGame().getPhase();
            }
            turn += 1;

        }
    }

	protected abstract void sendServerGameUpdate();

	public void gameVictory(GameVictoryEvent e) {
        sendGameReport();
        MWLogger.infoLog("GAME END");	
	}
    
    protected abstract void sendGameReport();

	public boolean isAdmin() {
        return getUser(getUsername()).getUserlevel() >= 200;
    }

    public boolean isMod() {
        return getUser(getUsername()).getUserlevel() >= 100;
    }
	
    public String getUsername() {
        return myUsername;
    }
    
    protected abstract IClientUser getUser(String name);
    
    public int getBuildingsLeft() {
        Enumeration<Building> buildings = ((Game)myServer.getGame()).getBoard()
                .getBuildings();
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
                packet.append(gameOptions.readLine() + "#");
            }
            gameOptions.close();
            gameOptionsFile.close();
        } catch (Exception ex) {
        }

        sendChat(GameHost.CAMPAIGN_PREFIX + "c servergameoptions#" + packet.toString());
    }
    

    public TreeMap<String, MMGame> getServers() {
        return servers;
    }
    
    public void sendChat(String s) {
        // Sends the content of the Chatfield to the server
        // We need the StringTokenizer to enable Mulitline comments
        StringTokenizer st = new StringTokenizer(s, "\n");

        while (st.hasMoreElements()) {
            String str = (String) st.nextElement();
            // don't send empty lines
            if (!str.trim().equals("")) {
                serverSend("CH|" + str);
            }
        }
    }
    
    public String doEscape(String str) {

        if (str.indexOf("<a href=\"MEKINFO") != -1) {
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

        // find all occurences of character in t and replace them with replace
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

    public void serverSend(String s) {
        try {
            Connector.send(IClient.PROTOCOL_PREFIX + "comm" + "\t" + TransportCodec.encode(s));
        } catch (Exception e) {
            MWLogger.errLog(e);
        }
    }
}
