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

public interface IClient {
    /**
     * The delimiter.  A tab character.
     */
    String DELIMITER = "\t";
    String GUI_PREFIX = "/"; // prefix for commands in GUI

    int STATUS_DISCONNECTED = 0;
    int STATUS_LOGGED_OUT = 1;
    int STATUS_RESERVE = 2;
    int STATUS_ACTIVE = 3;
    int STATUS_FIGHTING = 4;

    int REFRESH_STATUS = 0;
    int REFRESH_USERLIST = 1;
    int REFRESH_PLAYER_PANEL = 2;
    int REFRESH_BATTLE_TABLE = 4;
    int REFRESH_HQ_PANEL = 5;
    int REFRESH_BM_PANEL = 6;

    int IGNORE_PUBLIC = 0;
    int IGNORE_HOUSE = 1;
    int IGNORE_PRIVATE = 2;

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

    void doParseDataInput(String input);

    void parseDedDataInput(String input);

    void setLastPing(long lastPing);

    void sendChat(String string);

    CPlayer getPlayer();

    String moneyOrFluMessage(boolean b, boolean b1, int i);

    String moneyOrFluMessage(boolean b, boolean b1, int i, boolean b2);

    String getServerConfigs(String rpShortName);

    void setServerConfigs(String rpShortName, String rpValue);

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

    void addToChat(String s, int channel);

    IClientConfig getConfig();

    void processTick(int time);

    int getPlayerStartingEdge();

    void setPlayerStartingEdge(int edge);

    boolean isUsingAdvanceRepairs();

    String getConfigParam(String primaryHQSortOrder);

    String getUsername();

    void setUsername(String name);

    void updateOpData(boolean b);

    int getMyStatus();

    List<CUser> getUsers();

    TreeMap<String, String[]> getAllOps();

    boolean isWaiting();

    void setWaiting(boolean b);

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

    void setAdvancedTerrain(AdvancedTerrain aTerrain);

    void refreshGUI(int refreshHqPanel);

    boolean isIgnored(String name, int ignoreHouse);

    String getShortTime();

    void doPlaySound(String soundName);

    void changeStatus(int i);

    Vector<IBasicOption> getGameOptions();

    void setEnvironment(PlanetEnvironment planetEnvironment, Dimension dimension, int mapMedium);

    void serverSend(String s);

    void startClient(String curName, boolean b);

    TreeMap<String, MMGame> getServers();

    void stopHost();

    boolean isServerRunning();

    void goodbye();

    List<ClientThread> getMMClients();

    boolean isLeader();

    void rewardPointsDialog();

    void influencePointsDialog();

    void setSoundMuted(boolean state);

    void connectToServer();

    void connectToServer(String ip, int port);

    String getStatus();

    void processGUIInput(String s);

    Dimension getMapSize();

    PlanetEnvironment getCurrentEnvironment();

    Buildings getBuildingTemplate();

    void setBuildingTemplate(Buildings building);

    int getMapMedium();

    AdvancedTerrain getCurrentAdvancedTerrain();

    boolean isUsingBots();

    void setUsingBots(boolean b);

    int doEscape(String string);

    void addToChat(String s, int channelMail, String server);

    boolean hasKeyWords(String string);

    void updateClient();

    void setBotsOnSameTeam(Boolean aBoolean);

    void retrieveOpData(String aShort, String s);

    void updateParam(StringTokenizer st);

    void setServerOpFlags(StringTokenizer st);

    void updatePartsBlackMarket(String s, int campaignYear);

    void updatePlayerPartsCache(String s);

    void retrieveMul(String s);

    void createNewHouse(StringTokenizer st);

    String createFilenameChecksum(String s) throws Exception;

    String getLastQuery();

    void setLastQuery(String name);

    ArrayList<String> getPartialUser(String text);

    Map<Integer, Influences> getChangesSinceLastRefresh();

    int getMinPlanetOwnerShip(Planet planet);

    int getTechLaborCosts(Entity entity, int techGreen);

    double getTotalRepairCosts(Entity entity);
}
