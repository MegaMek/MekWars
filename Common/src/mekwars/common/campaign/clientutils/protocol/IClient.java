package mekwars.common.campaign.clientutils.protocol;

import java.awt.Dialog;
import java.util.Map;

import mekwars.common.CampaignData;
import mekwars.common.Equipment;
import mekwars.common.Player;
import mekwars.common.campaign.clientutils.IClientUser;

public interface IClient {
    /**
     * The delimiter.  A tab character.
     */
    String DELIMITER = "\t";

    String PROTOCOL_DELIMITER = "\t"; // delimiter for protocol commands
    String PROTOCOL_PREFIX = "/"; // prefix for protocol commands
    String CLIENT_VERSION = "0.8.0.0"; // change this with
    String CAMPAIGN_PREFIX = "/"; // prefix for campaign commands

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

    Player getPlayer();

    String moneyOrFluMessage(boolean b, boolean b1, int i);

    String moneyOrFluMessage(boolean b, boolean b1, int i, boolean b2);

    String getServerConfigs(String rpShortName);

    CampaignData getData();

    void loadBannedAmmo();

    boolean getTargetSystemBanStatus(int type);

    Dialog getMainFrame();

    void loadServerCommands();

    IClientUser getUser(String name);

    void getBlackMarketSettings();

    Map<String, Equipment> getBlackMarketEquipmentList();

    void reloadData();

    void getServerConfigData();

    void putServerConfigs(String config, String text);

    void refreshData();
}
