package mekwars.common.campaign.clientutils.protocol;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import megamek.common.game.Game;
import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.IClientConfig;
import mekwars.common.campaign.clientutils.IClientUser;
import mekwars.common.gui.CMainFrame;
import mekwars.common.util.RepairManagmentThread;
import mekwars.common.util.SalvageManagmentThread;

public interface IClient {
    /**
     * The delimiter.  A tab character.
     */
    String DELIMITER = "\t";

    int STATUS_DISCONNECTED = 0;
    int STATUS_LOGGED_OUT = 1;
    int STATUS_RESERVE = 2;
    int STATUS_ACTIVE = 3;
    int STATUS_FIGHTING = 4;

    String CAMPAIGN_PREFIX = "/"; // prefix for campaign commands
    String CAMPAIGN_PATH = "data/campaign/";
    String COMMAND_DELIMITER = "|"; // delimiter for client commands

    String PROTOCOL_DELIMITER = "\t"; // delimiter for protocol commands
    String PROTOCOL_PREFIX = "/"; // prefix for protocol commands
    String CLIENT_VERSION = "0.8.0.0"; // change this with

    /**
     * If you understand this, you are a 1.1-compliant client. Following DEFLATED + DELIMITER is the number of bytes in
     * the undeflated text. This will be a maximum of 29,999, so you don't have to buffer more than that.
     * com.carnageblender.chat.net gives an example implementation.
     */
    String DEFLATED = "/deflated";

    // called when there's a system message to show
    void systemMessage(String message);

    // called when there's an error message to show
    void errorMessage(String message);

    // called when there's server input to process
    void processIncoming(String incoming);

    // called when connection is lost
    void connectionLost();

    // called when connection is established
    void connectionEstablished();

    CConnector getConnector();

    void startHost(boolean dedicated, boolean deploy, boolean loadSavedGame);

    boolean isDedicated();

    void setUsername(String name);

    void doParseDataInput(String input);

    void parseDedDataInput(String input);

    void setLastPing(long lastPing);

    void sendChat(String string);

    CPlayer getPlayer();

    String moneyOrFluMessage(boolean b, boolean b1, int i);

    String moneyOrFluMessage(boolean b, boolean b1, int i, boolean b2);

    String getServerConfigs(String rpShortName);

    CampaignData getData();

    void loadBannedAmmo();

    boolean getTargetSystemBanStatus(int type);

    CMainFrame getMainFrame();

    void loadServerCommands();

    IClientUser getUser(String name);

    void getBlackMarketSettings();

    Map<String, Equipment> getBlackMarketEquipmentList();

    void reloadData();

    void getServerConfigData();

    void putServerConfigs(String config, String text);

    void refreshData();

    void addToChat(String s);

    IClientConfig getConfig();

    void processTick(int time);

    void setWaiting(boolean b);

    int getPlayerStartingEdge();

    boolean isUsingAdvanceRepairs();

    String getConfigParam(String primaryHQSortOrder);

    String getUsername();

    void updateOpData(boolean b);

    int getMyStatus();

    List<CUser> getUsers();

    TreeMap<String, String[]> getAllOps();

    boolean isWaiting();

    String getCacheDir();

    void loadServerTraitFiles();

    void loadMegaMekClient();

    void setConfig();

    int getUserLevel();

    RepairManagmentThread getRMT();

    double getAmmoCost(String internalName);

    SalvageManagmentThread getSMT();

    boolean isMod();

    boolean isAdmin();

    CCampaign getCampaign();

    void setPassword(String s);

    void setIgnoreHouse();

    void setIgnorePrivate();

    void setIgnorePublic();

    void setKeyWords();

    void setLookAndFeel(boolean b);

    void showInfoWindow(String s);

    Game getGame();
}
