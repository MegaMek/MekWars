/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.common.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.Serial;
import java.util.Objects;

import javax.swing.*;

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.common.equipment.AmmoType;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.gui.dialogs.*;
import mekwars.common.gui.dialogs.buildtableviewer.BuildTableViewer;
import mekwars.common.gui.panels.CMainPanel;
import mekwars.common.sounds.MenuPopupSound;
import mekwars.common.sounds.MenuSound;
import mekwars.common.threads.ClientThread;
import mekwars.common.util.MWLogger;
import mekwars.common.util.StringUtils;

//import client.gui.dialog.TableViewerDialog;

public class CMainFrame extends JFrame {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -1198882220815512476L;
    private final MenuSound sound;
    private final MenuPopupSound popupSound;
    public IClient client;
    JPanel contentPane;
    JMenuBar jMenuBar1 = new JMenuBar();
    // FILE Menu
    JMenu jMenuFile = new JMenu();
    JMenuItem jMenuFileConnect = new JMenuItem();
    JMenuItem jMenuFileDisconnect = new JMenuItem();
    JMenuItem jMenuFileRegister = new JMenuItem();
    JMenuItem jMenuFileMail = new JMenuItem();
    JMenuItem jMenuFileLastOnline = new JMenuItem();
    JMenuItem jMenuFileExit = new JMenuItem();
    JMenuItem jMenuFileConfig = new JMenuItem();

    // CAMPAIGN Menu
    JMenu jMenuCampaign = new JMenu();
    JMenu jMenuCampaignSubStatus = new JMenu();// submenu in Campaign
    JMenu jMenuCampaignSubTechs = new JMenu();
    JMenu jMenuCampaignSubBays = new JMenu();
    JMenu jMenuCampaignSubTransfer = new JMenu();
    JMenu jMenuCampaignSubAttack = new JMenu();
    JMenu jMenuCampaignSubMerc = new JMenu();
    JMenu jMenuCampaignSubOther = new JMenu();
    JMenu jMenuCampaignPersonnelTechSubMenu = new JMenu("Techs");
    JMenu jMenuCampaignPersonnelPilotsSubMenu = new JMenu("Pilots");
    JMenuItem jMenuCampaignMyStatus = new JMenuItem();
    JMenuItem jMenuCampaignLogin = new JMenuItem();
    JMenuItem jMenuCampaignActivate = new JMenuItem();
    JMenuItem jMenuCampaignDeactivate = new JMenuItem();
    JMenuItem jMenuCampaignLogout = new JMenuItem();
    JMenuItem jMenuCampaignPlayers = new JMenuItem();
    JMenuItem jMenuCampaignISStatus = new JMenuItem();
    JMenuItem jMenuCampaignHouses = new JMenuItem();
    JMenuItem jMenuCampaignFactionStatus = new JMenuItem();
    JMenuItem jMenuCampaignCheckAttack = new JMenuItem();
    JMenuItem jMenuCampaignRange = new JMenuItem();
    JMenuItem jMenuFindContestedPlanets = new JMenuItem(); //BarukKhazad 20151129
    JMenuItem jMenuCampaignTransferUnit = new JMenuItem();
    JMenuItem jMenuCampaignTransferMoney = new JMenuItem();
    JMenuItem jMenuCampaignTransferPilot = new JMenuItem();
    JMenuItem jMenuCampaignLogo = new JMenuItem();
    JMenuItem jMenuCampaignPersonalPilotQueue = new JMenuItem();
    JMenuItem jMenuCampaignDonatePersonalPilot = new JMenuItem();
    JMenuItem jMenuCampaignDirectSell = new JMenuItem();
    JMenuItem jMenuCampaignDefect = new JMenuItem();
    JMenuItem jMenuCampaignSelfPromote = new JMenuItem(); //@salient
    JMenuItem jMenuCampaignReportStatusMC = new JMenuItem(); //@salient
    JMenuItem jMenuCampaignRewardPoints = new JMenuItem();
    JMenuItem jMenuCampaignInfluencePoints = new JMenuItem();
    JMenuItem jMenuCampaignPartsCache = new JMenuItem();
    JMenuItem jMenuSubCampaignFireTechs = new JMenuItem();
    JMenuItem jMenuSubCampaignHireTechs = new JMenuItem();
    JMenuItem jMenuSubCampaignSellBays = new JMenuItem();
    JMenuItem jMenuSubCampaignBuyBays = new JMenuItem();
    JMenuItem jMenuCampaignBuyPilots = new JMenuItem();
    JMenuItem jMenuMercStatus = new JMenuItem();
    JMenuItem jMenuMercUnemployed = new JMenuItem();
    JMenuItem jMenuMercContracted = new JMenuItem();
    JMenuItem jMenuMercOfferContract = new JMenuItem();

    /*
     * ATTACK/GAME Menu is a class unto itself and needs constant update calls.
     */
    AttackMenu jMenuAttackMenu;
    // HOST Menu
    JMenu jMenuHost = new JMenu();
    JMenuItem jMenuCSHostAndJoin = new JMenuItem();
    JMenuItem jMenuCSHostDedicated = new JMenuItem();
    JMenuItem jMenuCSHostLoad = new JMenuItem();
    JMenuItem jMenuCSHostLoadAndJoin = new JMenuItem();
    JMenuItem jMenuCSHostStop = new JMenuItem();

    // OPTIONS menu components
    JMenu jMenuOptions = new JMenu();
    JCheckBoxMenuItem jMenuOptionsAutoScroll = new JCheckBoxMenuItem();
    JCheckBoxMenuItem jMenuOptionsMute = new JCheckBoxMenuItem();
    JMenuItem jMenuOptionsReloadAllData = new JMenuItem();

    // Leadership Menu
    JMenu jMenuLeaderShip = new JMenu();
    JMenuItem jMenuLeaderPromote = new JMenuItem();
    JMenuItem jMenuLeaderDemote = new JMenuItem();
    JMenuItem jMenuLeaderFluff = new JMenuItem();
    JMenuItem jMenuLeaderMute = new JMenuItem();
    JMenuItem jMenuLeaderFactionColor = new JMenuItem();
    JMenuItem jMenuLeaderPlayerColor = new JMenuItem();
    JMenuItem jMenuLeaderPurchaseFactory = new JMenuItem();
    JMenuItem jMenuLeaderResearchTech = new JMenuItem();
    JMenuItem jMenuLeaderResearchUnit = new JMenuItem();
    JMenuItem jMenuLeaderSetComponentConversion = new JMenuItem();
    JMenuItem jMenuLeaderViewFactionPartsCache = new JMenuItem();

    // HELP Menu
    JMenu jMenuHelp = new JMenu();
    JMenuItem jMenuHelpAbout = new JMenuItem();
    JMenuItem jMenuHelpMemory = new JMenuItem();
    JMenuItem jMenuHelpHelp = new JMenuItem();
    JMenuItem jMenuHelpViewUnit = new JMenuItem();
    JMenuItem jMenuHelpViewBuildTables = new JMenuItem();
    JMenuItem jMenuHelpViewTraits = new JMenuItem();
    JMenuItem jMenuHelpPilotSkills = new JMenuItem();
    JMenuItem jMenuHelpOpViewer = new JMenuItem();

    // Emoji Menu
    JMenu jMenuEmoji = new JMenu();
    JMenuItem jMenuEmojiFlip = new JMenuItem();
    JMenuItem jMenuEmojiShrug = new JMenuItem();
    JMenuItem jMenuEmojiFingers = new JMenuItem();
    JMenuItem jMenuEmojiKiss = new JMenuItem();
    JMenuItem jMenuEmojiSmile = new JMenuItem();
    JMenuItem jMenuEmojiDeal = new JMenuItem();

    // These are simple holders for when real menus
    // is generated/returned from the admin plugin.
    JMenu jMenuMod = new JMenu();
    JMenu jMenuAdmin = new JMenu();
    JMenu jMenuOperations = new JMenu();

    CMainPanel MainPanel;
    CCampaign theCampaign;
    CPlayer thePlayer;
    boolean useAdvanceRepairs;
    boolean usePersonalPilotQueues;
    private int userLevel = 0;
    private boolean hasAdminMenus = false;

    // CONSTRUCTOR
    public CMainFrame(IClient myC) {
        client = myC;
        theCampaign = client.getCampaign();
        thePlayer = client.getPlayer();
        MainPanel = new CMainPanel(client, this);

        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        usePersonalPilotQueues = Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"));
        sound = new MenuSound(client);
        popupSound = new MenuPopupSound(client);

        /*
         * ATTACK/GAME Menu is a class unto itself and needs constant update
         * calls. Have to build it here so its client isn't null and it isn't
         * being handed to createMenu() as a null itself.
         */

        jMenuAttackMenu = new AttackMenu(client, -1, "-1");

        enableEvents(java.awt.AWTEvent.WINDOW_EVENT_MASK);
        setResizable(true);
        setSize(new java.awt.Dimension(640, 480));
        setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        setTitle(client.getConfigParam("CAMPAIGNSERVERNAME") + " (MekWars Client " + IClient.CLIENT_VERSION + ")");
        contentPane = (JPanel) getContentPane();
        contentPane.setLayout(new java.awt.BorderLayout());
        useAdvanceRepairs = client.isUsingAdvanceRepairs();
        usePersonalPilotQueues = Boolean.parseBoolean(client.getServerConfigs("AllowPersonalPilotQueues"));
        try {
            // factored out to reduce bloat
            createMenu();
        } catch (Exception e) {
            MWLogger.errLog(e);
        }
        setJMenuBar(jMenuBar1);
        enableMenu();
        repaint();
        contentPane.add(MainPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent evt) {
                if (client.isServerRunning()) {
                    int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                          "Are you sure you want to exit?",
                          "You are hosting a game!",
                          JOptionPane.YES_NO_OPTION);
                    if (result == JOptionPane.YES_OPTION) {
                        client.goodbye();
                        System.exit(0);
                    }
                } else {
                    client.goodbye();
                    System.exit(0);
                }
            }


            @Override
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {
                for (ClientThread mmClient : client.getMMClients()) {
                    mmClient.getMegaMekController().setIgnoreKeyPresses(true);
                }
            }


            @Override
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                for (ClientThread mmClient : client.getMMClients()) {
                    mmClient.getMegaMekController().setIgnoreKeyPresses(false);
                }
            }
        });
    }

    public CMainPanel getMainPanel() {
        return MainPanel;
    }

    // Initializing Components
    public void enableMenu() {
        boolean disconnected = false;
        boolean loggedout = false;
        boolean loggedin = false;
        boolean reserve = false;
        boolean active = false;
        boolean admin = false;
        boolean mod = false;

        userLevel = client.getUserLevel();
        client.loadServerCommands();

        if (client.getMyStatus() == IClient.STATUS_DISCONNECTED) {
            disconnected = true;
        }
        if (client.getMyStatus() == IClient.STATUS_LOGGED_OUT) {
            loggedout = true;
        }
        if (client.getMyStatus() == IClient.STATUS_RESERVE) {
            loggedin = true;
            reserve = true;
        }
        if (client.getMyStatus() == IClient.STATUS_ACTIVE) {
            loggedin = true;
            active = true;
        }
        if (client.getMyStatus() == IClient.STATUS_FIGHTING) {
            loggedin = true;
            // fighting = true;
        }
        if (client.isAdmin()) {
            admin = true;
        }
        if (client.isMod()) {
            mod = true;
        }

        if ((mod || admin) && !hasAdminMenus) {

            File loadJar = new File("./MekWarsAdmin.jar");

            // don't print an entire trace if the jar is missing.
            if (!loadJar.exists()) {
                MWLogger.errLog("Player/Server menu creation skipped. No MekWarsAdmin.jar present.");
            } else {
                // assume mod
                try {
                    java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {
                          loadJar.toURI().toURL()
                    });

                    Class<?> clazz = loader.loadClass("admin.ModeratorMenu");
                    Object object = clazz.getDeclaredConstructor().newInstance();
                    clazz.getDeclaredMethod("createMenu", new Class[] { IClient.class }).invoke(object, client);
                    jMenuBar1.remove(jMenuMod);
                    jMenuMod = (JMenu) object;
                    jMenuBar1.add(jMenuMod);
                    loader.close();
                } catch (Exception ex) {
                    MWLogger.errLog("ModeratorMenu creation FAILED!");
                    MWLogger.errLog(ex);
                }
                try {
                    java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {
                          loadJar.toURI().toURL() });
                    Class<?> clazz = loader.loadClass("admin.AdminMenu");
                    Object object = clazz.getDeclaredConstructor().newInstance();
                    clazz.getDeclaredMethod("createMenu", new Class[] { IClient.class }).invoke(object, client);
                    jMenuBar1.remove(jMenuAdmin);
                    jMenuAdmin = (JMenu) object;
                    jMenuBar1.add(jMenuAdmin);
                    loader.close();
                } catch (Exception ex) {
                    MWLogger.errLog("AdminMenu creation FAILED!");
                    MWLogger.errLog(ex);
                }
            }// end else(Admin.jar exists)

            if (new java.io.File("./MekWarsOpEditor.jar").exists()) {
                jMenuBar1.remove(jMenuOperations);
                jMenuOperations.setText("Operations");
                JMenuItem item = new JMenuItem("Op Editor");
                item.addActionListener(_ -> {
                    try {
                        java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {
                              new File("./MekWarsOpEditor.jar").toURI().toURL() });
                        Class<?> clazz = loader.loadClass("OperationsEditor.MainOperations");
                        Object object = clazz.getDeclaredConstructor().newInstance();
                        clazz.getDeclaredMethod("main", new Class[] { Object.class }).invoke(object, client);
                        loader.close();
                    } catch (Exception ex) {
                        MWLogger.errLog(ex);
                    }
                    // new
                    // OperationsEditor.dialog.OperationsDialog(client);
                });
                jMenuOperations.add(item);
                JMenuItem jMenuRetrieveOperationFile = new JMenuItem();
                JMenuItem jMenuSetOperationFile = new JMenuItem();
                JMenuItem jMenuSetNewOperationFile = new JMenuItem();
                JMenuItem jMenuSendAllOperationFiles = new JMenuItem();
                JMenuItem jMenuUpdateOperations = new JMenuItem();

                jMenuRetrieveOperationFile.setText("Retrieve Operation File");
                jMenuRetrieveOperationFile.addActionListener(this::jMenuRetrieveOperationFile_actionPerformed);

                jMenuSetOperationFile.setText("Set Operation File");
                jMenuSetOperationFile.addActionListener(this::jMenuSetOperationFile_actionPerformed);

                jMenuSetNewOperationFile.setText("Set New Operation File");
                jMenuSetNewOperationFile.addActionListener(this::jMenuSetNewOperationFile_actionPerformed);

                jMenuSendAllOperationFiles.setText("Send All Local Op Files");
                jMenuSendAllOperationFiles.addActionListener(this::jMenuSendAllOperationFiles_actionPerformed);

                jMenuUpdateOperations.setText("Update Operations");
                jMenuUpdateOperations.addActionListener(this::jMenuUpdateOperations_actionPerformed);

                int userLevel = client.getUserLevel();
                if (userLevel >= client.getData().getAccessLevel("RetrieveOperation")) {
                    jMenuOperations.add(jMenuRetrieveOperationFile);
                }
                if (userLevel >= client.getData().getAccessLevel("SetOperation")) {
                    jMenuOperations.add(jMenuSetOperationFile);
                    jMenuOperations.add(jMenuSetNewOperationFile);
                    jMenuOperations.add(jMenuSendAllOperationFiles);
                }
                if (userLevel >= client.getData().getAccessLevel("UpdateOperations")) {
                    jMenuOperations.add(jMenuUpdateOperations);
                }

                jMenuBar1.add(jMenuOperations);
            }
            hasAdminMenus = true;
        }// end if(is admin or mod)

        jMenuAdmin.setVisible(admin);
        jMenuMod.setVisible(mod);
        jMenuOperations.setVisible(mod);

        jMenuCampaign.setVisible(!disconnected);
        jMenuCampaignMyStatus.setVisible(loggedin);
        jMenuCampaignSubAttack.setVisible(loggedin);
        jMenuCampaignSubMerc.setVisible(loggedin);
        jMenuCampaignSubTransfer.setVisible(loggedin);
        jMenuCampaignSubStatus.setVisible(loggedin);
        jMenuCampaignSubTechs.setVisible(loggedin);
        jMenuCampaignSubBays.setVisible(useAdvanceRepairs && loggedin);
        jMenuCampaignSubOther.setVisible(loggedin);
        // jMenuTask.setVisible(loggedin);
        jMenuHost.setVisible(!disconnected);

        jMenuLeaderShip.setVisible(client.isLeader());

        jMenuLeaderDemote.setVisible(userLevel >= client.getData().getAccessLevel("DemotePlayer"));
        jMenuLeaderFluff.setVisible(userLevel >= client.getData().getAccessLevel("FactionLeaderFluff"));
        jMenuLeaderMute.setVisible(userLevel >= client.getData().getAccessLevel("FactionLeaderMute"));
        jMenuLeaderPromote.setVisible(userLevel >= client.getData().getAccessLevel("PromotePlayer"));
        jMenuLeaderFactionColor.setVisible(userLevel >= client.getData().getAccessLevel("ChangeHouseColor"));
        jMenuLeaderPlayerColor.setVisible(userLevel >= client.getData().getAccessLevel("AdminSetHousePlayerColor"));
        jMenuLeaderPurchaseFactory.setVisible(userLevel >= client.getData().getAccessLevel("PurchaseFactory"));
        jMenuLeaderResearchTech.setVisible(userLevel >= client.getData().getAccessLevel("ResearchTechLevel"));
        jMenuLeaderResearchUnit.setVisible(userLevel >= client.getData().getAccessLevel("ResearchUnit"));
        jMenuLeaderSetComponentConversion.setVisible(userLevel >=
                                                           client.getData().getAccessLevel("SetComponentConversion"));
        jMenuLeaderViewFactionPartsCache.setVisible(userLevel >=
                                                          client.getData().getAccessLevel("ViewFactionPartsCache"));

        jMenuFileConnect.setVisible(disconnected);
        jMenuFileDisconnect.setVisible(!disconnected);
        jMenuFileRegister.setVisible(!disconnected);
        jMenuFileMail.setVisible(!disconnected);
        jMenuFileLastOnline.setVisible(!disconnected);
        jMenuFileConfig.setVisible(true);

        jMenuCampaignLogin.setVisible(loggedout);
        jMenuCampaignActivate.setVisible(reserve);
        jMenuCampaignDeactivate.setVisible(active);
        jMenuCampaignLogout.setVisible(loggedin);
        jMenuCampaignPersonalPilotQueue.setVisible(usePersonalPilotQueues);
        jMenuCampaignTransferPilot.setVisible(usePersonalPilotQueues);
        jMenuCampaignDonatePersonalPilot.setVisible(usePersonalPilotQueues);
        jMenuCampaignDirectSell.setVisible(Boolean.parseBoolean(client.getServerConfigs("UseDirectSell")));

        addMenuListener(jMenuBar1.getComponents());
        this.repaint();
    }

    protected void createMenu() throws Exception {

        jMenuFile.setText("File");
        jMenuFile.setMnemonic('F');

        jMenuFileConnect.setText("Connect");
        jMenuFileConnect.setMnemonic('o');
        jMenuFileConnect.addActionListener(_ -> jMenuFileConnect_actionPerformed());

        jMenuFileDisconnect.setText("Disconnect");
        jMenuFileDisconnect.setMnemonic('D');
        jMenuFileDisconnect.addActionListener(_ -> client.getConnector().closeConnection());

        jMenuFileRegister.setText("Register Nickname");
        jMenuFileRegister.setMnemonic('R');
        jMenuFileRegister.addActionListener(_ -> jMenuFileRegister_actionPerformed());

        jMenuFileMail.setText("Mail User");
        jMenuFileMail.setMnemonic('M');
        jMenuFileMail.addActionListener(_ -> jMenuFileMail_actionPerformed(null));

        jMenuFileLastOnline.setText("Last Online");
        jMenuFileLastOnline.setMnemonic('L');
        jMenuFileLastOnline.addActionListener(_ -> jMenuFileLastOnline_actionPerformed());

        jMenuFileConfig.setText("Configuration");
        jMenuFileConfig.setMnemonic('C');
        jMenuFileConfig.addActionListener(_ -> new ConfigurationDialog(client));

        jMenuFileExit.setText("Exit");
        jMenuFileExit.setMnemonic('X');
        jMenuFileExit.addActionListener(_ -> jMenuFileExit_actionPerformed());

        jMenuCampaign.setText("Campaign");
        jMenuCampaign.setMnemonic('C');

        jMenuCampaignLogin.setText("Log in!");
        jMenuCampaignLogin.setMnemonic('I');
        jMenuCampaignLogin.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c login"));

        jMenuCampaignActivate.setText("Activate");
        jMenuCampaignActivate.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX +
                                                                           "c activate#" +
                                                                           IClient.CLIENT_VERSION));

        jMenuCampaignDeactivate.setText("Deactivate");
        jMenuCampaignDeactivate.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c deactivate"));

        jMenuCampaignLogout.setText("Log Out");
        jMenuCampaignLogout.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c logout"));

        jMenuCampaignPlayers.setText("Players Status");
        jMenuCampaignPlayers.setMnemonic('P');
        jMenuCampaignPlayers.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c players"));

        jMenuCampaignISStatus.setText("Planetary Control");
        jMenuCampaignISStatus.setMnemonic('C');
        jMenuCampaignISStatus.addActionListener(_ -> jMenuCampaignISStatus_actionPerformed());

        jMenuCampaignFactionStatus.setText("Faction Status");
        jMenuCampaignFactionStatus.setMnemonic('F');
        jMenuCampaignFactionStatus.addActionListener(_ -> jMenuCampaignFactionStatus_actionPerformed());

        jMenuCampaignHouses.setText("Factions List");
        jMenuCampaignHouses.setMnemonic('L');
        jMenuCampaignHouses.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c housestatus"));

        if (useAdvanceRepairs) {
            jMenuCampaignSubBays.setText("Bays");
            jMenuCampaignSubBays.setMnemonic('B');
        }

        jMenuCampaignSubTechs.setText("Personnel");
        jMenuCampaignSubTechs.setMnemonic('E');

        jMenuCampaignSubTransfer.setText("Transfer");
        jMenuCampaignSubTransfer.setMnemonic('T');

        jMenuCampaignSubAttack.setText("Front Line");
        jMenuCampaignSubAttack.setMnemonic('F');

        jMenuCampaignSubStatus.setText("Status");
        jMenuCampaignSubStatus.setMnemonic('U');

        jMenuCampaignSubOther.setText("Other");
        jMenuCampaignSubOther.setMnemonic('O');

        jMenuCampaignMyStatus.setText("My Status");
        jMenuCampaignMyStatus.setMnemonic('M');
        jMenuCampaignMyStatus.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c mystatus"));

        jMenuCampaignCheckAttack.setText("Attack Options");
        jMenuCampaignCheckAttack.setMnemonic('A');
        jMenuCampaignCheckAttack.addActionListener(_ -> jMenuCommanderCheckAttack_actionPerformed(-1));

        jMenuCampaignRange.setText("Range Calculator");
        jMenuCampaignRange.setMnemonic('R');
        jMenuCampaignRange.addActionListener(_ -> jMenuCommanderRange_actionPerformed());

        jMenuFindContestedPlanets.setText("Find Contested Planets");
        jMenuFindContestedPlanets.setMnemonic('Z');
        jMenuFindContestedPlanets.addActionListener(_ -> jMenuFindContestedPlanets_actionPerformed());  //BarukKhazad 20151129 - end 1

        jMenuCampaignTransferUnit.setText("Transfer Unit");
        jMenuCampaignTransferUnit.setMnemonic('U');
        jMenuCampaignTransferUnit.addActionListener(_ -> jMenuCommanderTransferUnit_actionPerformed(null, -1));

        jMenuCampaignTransferMoney.setText("Transfer " + client.moneyOrFluMessage(true, true, -2));
        jMenuCampaignTransferMoney.setMnemonic('C');
        jMenuCampaignTransferMoney.addActionListener(_ -> jMenuCommanderTransferMoney_actionPerformed(null));

        jMenuCampaignLogo.setText("Set Logo");
        jMenuCampaignLogo.setMnemonic('L');
        jMenuCampaignLogo.addActionListener(_ -> jMenuCommanderLogo_actionPerformed());

        jMenuCampaignPersonalPilotQueue.setText("View Pilot Queue");
        jMenuCampaignPersonalPilotQueue.setMnemonic('Q');
        jMenuCampaignPersonalPilotQueue.addActionListener(_ -> jMenuCommanderPersonalPilotQueue_actionPerformed());

        jMenuCampaignDonatePersonalPilot.setText("Fire Pilot");
        jMenuCampaignDonatePersonalPilot.setMnemonic('o');
        jMenuCampaignDonatePersonalPilot.addActionListener(_ -> jMenuCommanderDonatePersonalPilot_actionPerformed());

        jMenuCampaignDirectSell.setText("Direct Sell Unit");
        jMenuCampaignDirectSell.setMnemonic('S');
        jMenuCampaignDirectSell.addActionListener(_ -> jMenuCommanderDirectSell_actionPerformed(null, null));

        jMenuCampaignTransferPilot.setText("Transfer Pilot");
        jMenuCampaignTransferPilot.setMnemonic('T');
        jMenuCampaignTransferPilot.addActionListener(_ -> jMenuCommanderTransferPilot_actionPerformed(null));

        jMenuCampaignDefect.setText("Defect");
        jMenuCampaignDefect.setMnemonic('D');
        jMenuCampaignDefect.addActionListener(_ -> jMenuCommanderDefect_actionPerformed());

        jMenuCampaignSelfPromote.setText("Self Promote"); //@salient
        jMenuCampaignSelfPromote.addActionListener(_ -> jMenuCommanderSelfPromote_actionPerformed());

        jMenuCampaignReportStatusMC.setText("Check MiniCampaign Status"); //@salient for mini campaign
        jMenuCampaignReportStatusMC.addActionListener(_ -> jMenuCommanderReportStatusMC_actionPerformed());

        jMenuCampaignRewardPoints.setText("Use " + client.getServerConfigs("RPLongName"));
        jMenuCampaignRewardPoints.setMnemonic('P');
        jMenuCampaignRewardPoints.addActionListener(_ -> client.rewardPointsDialog());

        //@Salient
        jMenuCampaignInfluencePoints.setText("Use " + client.getServerConfigs("FluLongName"));
        jMenuCampaignInfluencePoints.addActionListener(_ -> client.influencePointsDialog());

        jMenuCampaignPartsCache.setText("View Parts");
        jMenuCampaignPartsCache.setMnemonic('V');
        jMenuCampaignPartsCache.addActionListener(_ -> jMenuCampaignPartsCache_actionPerformed());

        if (useAdvanceRepairs) {
            jMenuSubCampaignBuyBays.setText("Lease Bays");
            jMenuSubCampaignBuyBays.setMnemonic('L');
            jMenuSubCampaignBuyBays.addActionListener(_ -> jMenuCommanderBuyBays_actionPerformed());

            jMenuSubCampaignSellBays.setText("Return Bays");
            jMenuSubCampaignSellBays.setMnemonic('R');
            jMenuSubCampaignSellBays.addActionListener(_ -> jMenuCommanderSellBays_actionPerformed());
        }

        if (usePersonalPilotQueues) {
            jMenuCampaignBuyPilots.setText("Hire Pilots");
            jMenuCampaignBuyPilots.setMnemonic('P');
            jMenuCampaignBuyPilots.addActionListener(_ -> jMenuCampaignSubOtherBuyPilots_actionPerformed());
        }

        jMenuSubCampaignHireTechs.setText("Hire Techs");
        jMenuSubCampaignHireTechs.setMnemonic('H');
        jMenuSubCampaignHireTechs.addActionListener(_ -> jMenuCommanderHireTechs_actionPerformed());

        jMenuSubCampaignFireTechs.setText("Fire Techs");
        jMenuSubCampaignFireTechs.setMnemonic('F');
        jMenuSubCampaignFireTechs.addActionListener(_ -> jMenuCommanderFireTechs_actionPerformed());

        jMenuCampaignSubMerc.setText("Mercenaries");
        jMenuCampaignSubMerc.setMnemonic('r');

        jMenuMercOfferContract.setText("Offer a Mercenary Contract");
        jMenuMercOfferContract.setMnemonic('O');
        jMenuMercOfferContract.addActionListener(_ -> jMenuMercOfferContract_actionPerformed());

        jMenuMercStatus.setText("Mercenary Status");
        jMenuMercStatus.setMnemonic('M');
        jMenuMercStatus.addActionListener(_ -> jMenuMercStatus_actionPerformed());

        jMenuMercUnemployed.setText("Unemployed Mercs");
        jMenuMercUnemployed.setMnemonic('U');
        jMenuMercUnemployed.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c unemployedmercs"));

        jMenuMercContracted.setText("Contracted Mercs");
        jMenuMercContracted.setMnemonic('C');
        jMenuMercContracted.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "c housecontracts"));

        jMenuHost.setText("Host");
        jMenuHost.setMnemonic('S');

        jMenuCSHostAndJoin.setText("Start Hosting (and Join)");
        jMenuCSHostAndJoin.setMnemonic('H');
        jMenuCSHostAndJoin.addActionListener(_ -> {
            startHost();
            client.startHost(false, true, false);
        });

        jMenuCSHostDedicated.setText("Start Dedicated Host");
        jMenuCSHostDedicated.setMnemonic('D');
        jMenuCSHostDedicated.setEnabled(false);
        jMenuCSHostDedicated.setVisible(false);
        jMenuCSHostDedicated.addActionListener(_ -> {
            startHost();
            client.startHost(true, false, false);
        });

        jMenuCSHostLoad.setText("Start Dedicated Host (Load Savegame)");
        jMenuCSHostLoad.setMnemonic('L');
        jMenuCSHostLoad.setEnabled(false);
        jMenuCSHostLoad.setVisible(false);
        jMenuCSHostLoad.addActionListener(_ -> {
            startHost();
            client.startHost(true, false, true);
        });

        jMenuCSHostLoadAndJoin.setText("Start Hosting (Load Savegame and Join)");
        jMenuCSHostLoadAndJoin.setMnemonic('S');
        jMenuCSHostLoadAndJoin.addActionListener(_ -> {
            startHost();
            client.startHost(false, false, true);
        });

        jMenuCSHostStop.setText("Stop Hosting");
        jMenuCSHostStop.setMnemonic('S');
        jMenuCSHostStop.setEnabled(false);
        jMenuCSHostStop.setVisible(false);
        jMenuCSHostStop.addActionListener(_ -> {
            stopHost();
            client.stopHost();
        });

        jMenuOptions.setText("Options");
        jMenuOptions.setMnemonic('I');

        jMenuOptionsAutoScroll.setText("Auto Scroll");
        jMenuOptionsAutoScroll.setMnemonic('A');
        jMenuOptionsAutoScroll.setState(MainPanel.getCommPanel().autoTextUpdate);
        jMenuOptionsAutoScroll.addActionListener(_ -> {
            boolean newValue = !MainPanel.getCommPanel().autoTextUpdate;
            MainPanel.getCommPanel().autoTextUpdate = newValue;
            jMenuOptionsAutoScroll.setState(newValue);

            client.getConfig().setParam("AUTOSCROLL", Boolean.toString(newValue));
            client.getConfig().saveConfig();
        });

        jMenuOptionsMute.setText("Mute");
        jMenuOptionsMute.setMnemonic('M');
        jMenuOptionsMute.addActionListener(_ -> client.setSoundMuted(jMenuOptionsMute.getState()));

        jMenuOptionsReloadAllData.setText("Reload Data");
        jMenuOptionsReloadAllData.setMnemonic('D');
        jMenuOptionsReloadAllData.addActionListener(_ -> client.reloadData());

        jMenuLeaderShip.setText("Leadership");

        jMenuLeaderPromote.setText("Promote Player");
        jMenuLeaderPromote.addActionListener(_ -> jMenuLeaderPromote_actionPerformed());

        jMenuLeaderDemote.setText("Demote Player");
        jMenuLeaderDemote.addActionListener(_ -> jMenuLeaderDemote_actionPerformed());

        jMenuLeaderFluff.setText("Fluff Player");
        jMenuLeaderFluff.addActionListener(_ -> jMenuLeaderFluff_actionPerformed());

        jMenuLeaderMute.setText("Mute Player");
        jMenuLeaderMute.addActionListener(_ -> jMenuLeaderMute_actionPerformed());

        jMenuLeaderFactionColor.setText("Faction Color");
        jMenuLeaderFactionColor.addActionListener(_ -> jMenuLeaderFactionColor_actionPerformed());

        jMenuLeaderPlayerColor.setText("Player Color");
        jMenuLeaderPlayerColor.addActionListener(_ -> jMenuLeaderPlayerColor_actionPerformed());

        jMenuLeaderResearchUnit.setText("Research Unit");
        jMenuLeaderResearchUnit.addActionListener(_ -> jMenuLeaderResearchUnit_actionPerformed());

        jMenuLeaderResearchTech.setText("Research Tech");
        jMenuLeaderResearchTech.addActionListener(_ -> {
            int option = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Do you wish to research tech?",
                  "Research?",
                  JOptionPane.YES_NO_OPTION);

            if (option == JOptionPane.NO_OPTION) {
                return;
            }

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c researchtechlevel");
        });

        jMenuLeaderPurchaseFactory.setText("Purchase Factory");
        jMenuLeaderPurchaseFactory.addActionListener(_ -> jMenuLeaderPurchaseFactory_actionPerformed(null));

        jMenuLeaderSetComponentConversion.setText("Set Component Conversion");
        jMenuLeaderSetComponentConversion.addActionListener(_ -> jMenuLeaderSetComponentConversion_actionPerformed());

        jMenuLeaderViewFactionPartsCache.setText("View Faction Cache");
        jMenuLeaderViewFactionPartsCache.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX +
                                                                                      "c viewfactionpartscache"));

        jMenuHelp.setText("Help");
        jMenuHelp.setMnemonic('E');

        jMenuHelpAbout.setText("About");
        jMenuHelpAbout.setMnemonic('A');
        jMenuHelpAbout.addActionListener(_ -> jMenuHelpAbout_actionPerformed());

        jMenuHelpMemory.setText("Memory");
        jMenuHelpMemory.setMnemonic('M');
        jMenuHelpMemory.addActionListener(_ -> jMenuHelpMemory_actionPerformed());

        jMenuHelpHelp.setText("Online Help");
        jMenuHelpHelp.setMnemonic('H');
        jMenuHelpHelp.addActionListener(_ -> jMenuHelpHelp_actionPerformed());

        jMenuHelpViewUnit.setText("Unit Viewer");
        jMenuHelpViewUnit.setMnemonic('U');
        jMenuHelpViewUnit.addActionListener(_ -> jMenuHelpViewUnit_actionPerformed());

        jMenuHelpViewBuildTables.setText("Build Table Viewer");
        jMenuHelpViewBuildTables.setMnemonic('B');
        jMenuHelpViewBuildTables.addActionListener(_ -> jMenuHelpViewBuildTables_actionPerformed());

        jMenuHelpViewTraits.setText("View Faction Traits");
        jMenuHelpViewTraits.addActionListener(_ -> new TraitDialog(client, true));

        jMenuHelpPilotSkills.setText("Pilot Skill Descriptions");
        jMenuHelpPilotSkills.setMnemonic('P');
        jMenuHelpPilotSkills.addActionListener(_ -> jMenuHelpPilotSkills_actionPerformed());

        jMenuHelpOpViewer.setText("Operation Viewer");
        jMenuHelpOpViewer.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "getops md5"));

        /*
         * Display Report "MekWars Bug" and "Report MegaMek Bug" links in the Help Menu. Create the actual menu
         * options, with browsers calls, here to add them to the menu in the formatting blocks that follow. These are
         * hardcoded. Server ops can add their own links with the links.txt detailed above. @urgru 12.5.04
         */
        JMenuItem jMenuMekWarsBug = new JMenuItem("Report Bug/RFE (MekWars)");
        JMenuItem jMenuMegaMekBug = new JMenuItem("Report Bug/REF (MegaMek)");
        java.awt.event.ActionListener mekWarsListener = _ -> {
            try {
                Browser.displayURL("https://github.com/MegaMek/MekWars");
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        };
        java.awt.event.ActionListener megaMekListener = _ -> {
            try {
                Browser.displayURL("https://github.com/MegaMek/megamek");
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        };
        jMenuMekWarsBug.addActionListener(mekWarsListener);
        jMenuMegaMekBug.addActionListener(megaMekListener);

        //@sal emojis

        jMenuEmoji.setText("Emojis");

        if (Boolean.parseBoolean(client.getServerConfigs("AllowEmoji"))) {
            jMenuEmojiFlip.setText("(╯°□°)╯︵ ┻━┻");
            jMenuEmojiFlip.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#fl"));

            jMenuEmojiShrug.setText("¯\\_(ツ)_/¯");
            jMenuEmojiShrug.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#sh"));

            jMenuEmojiFingers.setText("t(-.-t)");
            jMenuEmojiFingers.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#fi"));

            jMenuEmojiKiss.setText("( ˘ ³˘)♥");
            jMenuEmojiKiss.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#ki"));

            jMenuEmojiSmile.setText("◉‿◉");
            jMenuEmojiSmile.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#sm"));

            jMenuEmojiDeal.setText("•_•) ( •_•)>⌐■-■ (⌐■_■)");
            jMenuEmojiDeal.addActionListener(_ -> client.sendChat(IClient.CAMPAIGN_PREFIX + "ec#de"));
        }


        /*
         * FORMATTING BLOCK
         */
        jMenuFile.add(jMenuFileConnect);
        jMenuFile.add(jMenuFileRegister);
        jMenuFile.add(jMenuFileMail);
        jMenuFile.add(jMenuFileLastOnline);
        jMenuFile.add(jMenuFileConfig);
        jMenuFile.addSeparator();
        jMenuFile.add(jMenuFileDisconnect);
        jMenuFile.add(jMenuFileExit);

        /*
         * Put together the campaign menu. Start by assembling the sub-menus, then add the manus and line items all
         * together ...
         */

        // front-line submenu
        jMenuCampaignSubAttack.add(jMenuCampaignCheckAttack);
        jMenuCampaignSubAttack.add(jMenuCampaignRange);
        jMenuCampaignSubAttack.add(jMenuFindContestedPlanets); //BarukKhazad 20151129

        // send submenu
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferMoney);
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferUnit);
        jMenuCampaignSubTransfer.add(jMenuCampaignTransferPilot);

        // techs sub menu
        jMenuCampaignPersonnelTechSubMenu.add(jMenuSubCampaignHireTechs);
        jMenuCampaignPersonnelTechSubMenu.add(jMenuSubCampaignFireTechs);
        jMenuCampaignSubTechs.add(jMenuCampaignPersonnelTechSubMenu);

        // Pilots sub menus.
        if (usePersonalPilotQueues) {
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignPersonalPilotQueue);
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignBuyPilots);
            jMenuCampaignPersonnelPilotsSubMenu.add(jMenuCampaignDonatePersonalPilot);
            jMenuCampaignSubTechs.add(jMenuCampaignPersonnelPilotsSubMenu);
        }

        // Bay sub menu
        jMenuCampaignSubBays.add(jMenuSubCampaignBuyBays);
        jMenuCampaignSubBays.add(jMenuSubCampaignSellBays);

        // status sub menu
        jMenuCampaignSubStatus.add(jMenuCampaignPlayers);
        jMenuCampaignSubStatus.add(jMenuCampaignFactionStatus);
        jMenuCampaignSubStatus.add(jMenuCampaignISStatus);
        jMenuCampaignSubStatus.add(jMenuCampaignHouses);

        // mercs sub menu
        jMenuCampaignSubMerc.add(jMenuMercStatus);
        jMenuCampaignSubMerc.add(jMenuMercUnemployed);
        jMenuCampaignSubMerc.add(jMenuMercContracted);
        jMenuCampaignSubMerc.add(jMenuMercOfferContract);

        // other sub menu
        jMenuCampaignSubOther.add(jMenuCampaignLogo);
        jMenuCampaignSubOther.add(jMenuCampaignDefect);

        if (Boolean.parseBoolean(client.getServerConfigs("Self_Promote_Subfaction"))) //@salient
        {
            jMenuCampaignSubOther.add(jMenuCampaignSelfPromote);
        }

        if (Boolean.parseBoolean(client.getServerConfigs("Enable_MiniCampaign"))) //@salient
        {
            jMenuCampaignSubOther.add(jMenuCampaignReportStatusMC);
        }

        jMenuCampaignSubOther.add(jMenuCampaignRewardPoints);
        jMenuCampaignSubOther.add(jMenuCampaignInfluencePoints);

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsBlackMarket"))) {
            jMenuCampaignSubOther.add(jMenuCampaignPartsCache);
        }

        jMenuCampaignSubOther.add(jMenuCampaignDirectSell);

        // assemble the actual campaign menu...
        jMenuCampaign.add(jMenuCampaignMyStatus);
        jMenuCampaign.add(jMenuCampaignSubAttack);
        jMenuCampaign.addSeparator();
        jMenuCampaign.add(jMenuCampaignSubMerc);
        jMenuCampaign.add(jMenuCampaignSubTransfer);
        jMenuCampaign.add(jMenuCampaignSubStatus);
        jMenuCampaign.add(jMenuCampaignSubBays);
        jMenuCampaign.add(jMenuCampaignSubTechs);
        jMenuCampaign.add(jMenuCampaignSubOther);
        jMenuCampaign.addSeparator();
        jMenuCampaign.add(jMenuCampaignLogin);
        jMenuCampaign.add(jMenuCampaignActivate);
        jMenuCampaign.add(jMenuCampaignDeactivate);
        jMenuCampaign.add(jMenuCampaignLogout);

        /*
         * Games menu is assembled in a Menu Factory
         */

        // assemble host menu
        jMenuHost.add(jMenuCSHostAndJoin);
        jMenuHost.add(jMenuCSHostLoadAndJoin);
        jMenuHost.add(jMenuCSHostDedicated);
        jMenuHost.add(jMenuCSHostLoad);
        jMenuHost.add(jMenuCSHostStop);

        jMenuOptions.add(jMenuOptionsAutoScroll);
        jMenuOptions.add(jMenuOptionsMute);
        jMenuOptions.addSeparator();
        jMenuOptions.add(jMenuOptionsReloadAllData);

        jMenuLeaderShip.add(jMenuLeaderFluff);
        jMenuLeaderShip.add(jMenuLeaderMute);
        jMenuLeaderShip.add(jMenuLeaderPromote);
        jMenuLeaderShip.add(jMenuLeaderDemote);
        jMenuLeaderShip.add(jMenuLeaderFactionColor);
        jMenuLeaderShip.add(jMenuLeaderPlayerColor);
        jMenuLeaderShip.add(jMenuLeaderPurchaseFactory);
        jMenuLeaderShip.add(jMenuLeaderResearchTech);
        jMenuLeaderShip.add(jMenuLeaderResearchUnit);
        jMenuLeaderShip.add(jMenuLeaderSetComponentConversion);
        jMenuLeaderShip.add(jMenuLeaderViewFactionPartsCache);

        jMenuHelp.add(jMenuHelpAbout);
        jMenuHelp.add(jMenuHelpMemory);
        jMenuHelp.addSeparator();
        jMenuHelp.add(jMenuHelpViewUnit);
        jMenuHelp.add(jMenuHelpViewBuildTables);

        /*
         * Only add the trait viewer if the server allows traits. We'll use BattleMek traits as a proxy for ALL
         * trait types when deciding whether to show.
         */

        if (Integer.parseInt(client.getServerConfigs("chanceforTNforMek")) > 0) {
            jMenuHelp.add(jMenuHelpViewTraits);
        }

        jMenuHelp.add(jMenuHelpHelp);
        jMenuHelp.add(jMenuHelpPilotSkills);
        jMenuHelp.add(jMenuHelpOpViewer);
        jMenuHelp.addSeparator();
        jMenuHelp.add(jMenuMekWarsBug);
        jMenuHelp.add(jMenuMegaMekBug);

        //@salient emoji
        jMenuEmoji.add(jMenuEmojiFlip);
        jMenuEmoji.add(jMenuEmojiShrug);
        jMenuEmoji.add(jMenuEmojiFingers);
        jMenuEmoji.add(jMenuEmojiKiss);
        jMenuEmoji.add(jMenuEmojiSmile);
        jMenuEmoji.add(jMenuEmojiDeal);

        /*
         * Admin menu setup used to be here. @urgru
         */

        /*
         * Mod menu setup used to be here. @urgru
         */

        jMenuBar1.add(jMenuFile);
        jMenuBar1.add(jMenuCampaign);
        jMenuBar1.add(jMenuAttackMenu);
        jMenuBar1.add(jMenuHost);
        jMenuBar1.add(jMenuOptions);
        jMenuBar1.add(jMenuLeaderShip);
        jMenuBar1.add(jMenuHelp);

        if (Boolean.parseBoolean(client.getServerConfigs("AllowEmoji"))) {
            jMenuBar1.add(jMenuEmoji);
        }

        jMenuBar1.add(jMenuOperations);
    }

    public void jMenuFileConnect_actionPerformed() {
        client.connectToServer();
        // Set Version upon reconnect.
        if (!client.getStatus().equals("Not connected")) {
            client.sendChat(
                  IClient.CAMPAIGN_PREFIX +
                        "c setclientversion#" +
                        client.getUsername().trim() +
                        "#" +
                        IClient.CLIENT_VERSION);
        }
    }

    public void jMenuFileRegister_actionPerformed() {
        new RegisterNameDialog(client);
    }

    public void jMenuFileMail_actionPerformed(String Nickname) {
        String message;
        if (Nickname == null) {
            Nickname = JOptionPane.showInputDialog(getContentPane(),
                  "Nickname",
                  "Send mail to whom?",
                  JOptionPane.PLAIN_MESSAGE);
            if (Nickname == null) {
                return;
            }
        }
        message = JOptionPane.showInputDialog(getContentPane(),
              "message",
              "Send mail to " + Nickname,
              JOptionPane.PLAIN_MESSAGE);
        if (message == null) {
            return;
        }
        client.processGUIInput(IClient.GUI_PREFIX + "mail " + Nickname + "," + message);
    }

    public void jMenuFileLastOnline_actionPerformed() {
        String Nickname;
        Nickname = JOptionPane.showInputDialog(getContentPane(), "Player name?");
        if (Nickname == null) {
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c lastonline#" + Nickname);
    }

    public void jMenuFileExit_actionPerformed() {
        client.goodbye();
        System.exit(0);
    }

    public void jMenuCampaignISStatus_actionPerformed() {

        String House;
        String House2;

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", true, false);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        if (!House.isEmpty()) {
            factionDialog = new HouseNameDialog(client, "Secondary Faction", true, false);
            factionDialog.setVisible(true);
            House2 = factionDialog.getHouseName();
            factionDialog.dispose();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c isstatus#" + House + "#" + House2);
        } else {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c isstatus");
        }
    }

    public void jMenuCampaignFactionStatus_actionPerformed() {

        String House;

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", true, false);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c faction#" + House);
    }

    public void jMenuMercStatus_actionPerformed() {
        PlayerNameDialog playerDialog = new PlayerNameDialog(client,
              "Which Merc do you want info on?",
              PlayerNameDialog.MERCS_ONLY);
        playerDialog.setVisible(true);
        String Merc = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (Merc == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c mstatus#" + Merc);
    }

    public void jMenuCommanderCheckAttack_actionPerformed(int lid) {
        if (lid == -1) {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c ca");
        } else {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c ca#" + lid);
        }
    }

    public void jMenuCommanderRange_actionPerformed() {
        String range;
        String faction;

        range = JOptionPane.showInputDialog(getContentPane(), "Max distance in Lightyears?");
        if ((range == null) || (range.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", false, false);
        factionDialog.setVisible(true);
        faction = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((faction == null) || (faction.isEmpty())) {
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c range#" + range + "#" + faction);
    }

    public void jMenuFindContestedPlanets_actionPerformed() { //BarukKhazad 20151129 - start 2
        String h1 = thePlayer.getHouse();
        String h2;
        String Perc = "20";
        Perc = (String) JOptionPane.showInputDialog(getContentPane(),
              "Minimum Attacker Planet Percentage? (1 to 100)",
              "",
              JOptionPane.PLAIN_MESSAGE,
              null,
              null,
              Perc);
        if ((Perc == null) || (Perc.isEmpty())) {
            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Target Faction", false, false);
        factionDialog.setVisible(true);
        h2 = factionDialog.getHouseName();
        factionDialog.dispose();

        if ((h2 == null) || (h2.isEmpty())) {
            return;
        }
        if (Objects.equals(h1, h2)) {
            client.addToChat("That is your faction. Target an enemy faction.");
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "findcp " + h1 + "#" + h2 + "#" + Perc);
    }  //BarukKhazad 20151129 - end 2

    public void jMenuCommanderTransferMoney_actionPerformed(String name) {

        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              "Send " + client.moneyOrFluMessage(true, true, -2) + " to " + targetPlayer,
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c transfermoney#" + targetPlayer + "#" + Amount);
    }

    public void jMenuCommanderTransferRewardPoints_actionPerformed(String name) {
        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              "Send " + client.getServerConfigs("RPShortName") + " to " + targetPlayer,
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c transferrewardpoints#" + targetPlayer + "#" + Amount);
    }

    //@Salient
    public void jMenuCommanderTransferInfluence_actionPerformed(String name) {
        String targetPlayer;
        String Amount;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "Amount",
              "Send " + client.getServerConfigs("FluShortName") + " to " + targetPlayer,
              JOptionPane.PLAIN_MESSAGE);

        if (Amount == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c transferinfluence#" + targetPlayer + "#" + Amount);
    }


    public void jMenuCommanderTransferUnit_actionPerformed(String name, int mid) {

        String targetPlayer;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        if (mid == -1) {
            UnitSelectionDialog usd = new UnitSelectionDialog(client, "Transfer Unit", "Select unit to transfer:");
            usd.setVisible(true);
            mid = Integer.parseInt(usd.getUnitID());
            usd.dispose();
        }

        if (mid == -1) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c transferunit#" + targetPlayer + "#" + mid);
    }

    public void jMenuCommanderAddToBM_actionPerformed(int mid) {

        java.util.Vector<CUnit> toSell = new java.util.Vector<>(1, 1);
        toSell.add(client.getPlayer().getUnit(mid));

        SellUnitDialog sud = new SellUnitDialog(this, client, toSell);
        sud.setVisible(true);
    }

    public void jMenuCommanderRemoveLance_actionPerformed(int lid) {
        String LanceID;
        if (lid == -1) {
            LanceID = JOptionPane.showInputDialog(getContentPane(), "Army ID?");
            if (LanceID == null) {
                return;
            }
            lid = Integer.parseInt(LanceID);
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c rma#" + lid);
    }

    /*
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * unit id.
     */
    public void jMenuCommanderNamePilot_actionPerformed(int uid) {
        String newName = JOptionPane.showInputDialog(getContentPane(), "Pilot's Name?");
        if (newName == null) {
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c namepilot#" + uid + "#" + newName);
    }

    /*
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     */
    public void jMenuCommanderNameArmy_actionPerformed(int aid) {
        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        String newName = JOptionPane.showInputDialog(getContentPane(),
              "New army name? [Leave blank to clear]",
              selectedArmy.getName());
        if (newName == null) {
            return;
        }

        if (newName.trim().isEmpty()) {
            newName = "clear";
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c namearmy#" + aid + "#" + newName);
    }

    public void jMenuCommanderPlayerLockArmy_actionPerformed(int aid) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c playerlockarmy#" + aid);
    }

    public void jMenuCommanderPlayerUnlockArmy_actionPerformed(int aid) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c playerunlockarmy#" + aid);
    }

    public void jMenuCommanderDisableArmy_actionPerformed(int aid) {
        // Toggle armyDisabled
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c togglearmydisabled#" + aid);
    }

    /*
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     */
    public void jMenuCommanderSetLowerUnitLimit_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        int newLimit;

        String example = "Example: An Army of 8 units with a Lower Limit of<br>" +
                               "4 will not be able to fight an Army with only 3 units.<br>" +
                               "This can be useful if you want to avoid fighting a<br>" +
                               "small number of super heavy/levelled units.";

        String limit = JOptionPane.showInputDialog(getContentPane(),
              "<HTML>" + "Lower Limit? [-1 to disable the limit]<i><br><br>" + example + "<br></i></HTML>",
              Integer.toString(selectedArmy.getLowerLimiter()),
              JOptionPane.PLAIN_MESSAGE);

        if (limit == null) {
            return;
        }

        newLimit = Integer.parseInt(limit);
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c all#" + aid + "#" + newLimit);
    }

    /*
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     */
    public void jMenuCommanderSetUpperUnitLimit_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        int newLimit;

        // generate an example string.
        String example = "Example: An Army of 4 units with an Upper Limit of 5<br>" +
                               "will not be able to fight against Armies with more than<br>" +
                               "9 units. This can be useful if you don't want to play<br>" +
                               "against swarms";

        String limit = JOptionPane.showInputDialog(getContentPane(),
              "<HTML>" + "Upper Limit? [-1 to disable the limit]<i><br><br>" + example + "<br></i></HTML>",
              Integer.toString(selectedArmy.getLowerLimiter()),
              JOptionPane.PLAIN_MESSAGE);

        if (limit == null) {
            return;
        }

        newLimit = Integer.parseInt(limit);
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c aul#" + aid + "#" + newLimit);
    }

    /*
     * Only called from HQ, via MechTableMouseAdapter. Will always have valid
     * army id.
     */
    public void jMenuCommanderSetForceSizeToFace_actionPerformed(int aid) {

        CArmy selectedArmy = client.getPlayer().getArmy(aid);
        if (selectedArmy == null) {
            return;
        }

        // generate an example string.
        String example = "This is the force size you expect to face when you request a match";

        String force = JOptionPane.showInputDialog(getContentPane(),
              "<HTML>" + "Force Size To Face	? [-1 to disable the limit]<i><br><br>" + example + "<br></i></HTML>",
              Float.toString(selectedArmy.getOpForceSize()),
              JOptionPane.PLAIN_MESSAGE);

        if (force == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c aofs#" + aid + "#" + force);

    }

    public void jMenuCommanderLogo_actionPerformed() {
        String LogoURL;
        LogoURL = JOptionPane.showInputDialog(getContentPane(),
              "URL? (i.e. http://www.mysite.com/mypic.jpg)",
              client.getPlayer().getMyLogo());
        if (LogoURL == null) {
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c setmylogo#" + LogoURL);
    }

    public void jMenuCommanderPersonalPilotQueue_actionPerformed() {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c displayplayerpersonalpilotqueue");
    }

    public void jMenuCommanderTransferPilot_actionPerformed(String name) {

        // get player
        String targetPlayer;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Transfer Recipient", PlayerNameDialog.FACTION_ONLY);
            pnd.setVisible(true);
            targetPlayer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            targetPlayer = name;
        }

        if (targetPlayer == null) {
            return;
        }

        // arrays for message box usage
        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType = { "Mek", "Proto" };

        int weightClass;
        int unitType;

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        if (pUnitTypeString.equals("Mek")) {
            unitType = Unit.MEK;
        } else {
            unitType = Unit.PROTOMEK;
        }

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        weightClass = Unit.getWeightIDForName(pWeightClassString);

        if (client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).isEmpty()) {
            JOptionPane.showMessageDialog(null,
                  "You do not have any pilots for " +
                        StringUtils.aOrAn(pWeightClassString, true) +
                        " " +
                        pUnitTypeString,
                  "No Pilots!",
                  JOptionPane.CLOSED_OPTION);
            return;
        }

        Object[] pilots = client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).toArray();

        JComboBox<String> combo = new JComboBox<>();

        for (Object pilot : pilots) {
            Pilot mm = (Pilot) pilot;
            if (unitType == Unit.MEK) {
                combo.addItem(mm.getName() +
                                    " (" +
                                    mm.getGunnery() +
                                    "/" +
                                    mm.getPiloting() +
                                    ")[" +
                                    mm.getSkillString(true) +
                                    "]");
            } else {
                combo.addItem(mm.getName() + " (" + mm.getGunnery() + ")[" + mm.getSkillString(true) + "]");
            }
        }

        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a pilot.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int position = combo.getSelectedIndex();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(
              IClient.CAMPAIGN_PREFIX +
                    "c transferpilot#" +
                    targetPlayer +
                    "#" +
                    unitType +
                    "#" +
                    weightClass +
                    "#" +
                    position);
    }

    public void jMenuCommanderDonatePersonalPilot_actionPerformed() {
        boolean allowProto = Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"));

        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType;

        if (allowProto) {
            pUnitType = new Object[] { "Mek", "Proto" };
        } else {
            pUnitType = new Object[] { "Mek" };
        }

        int weightClass;
        int unitType;

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        if (pUnitTypeString.equals("Mek")) {
            unitType = Unit.MEK;
        } else {
            unitType = Unit.PROTOMEK;
        }

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select a pilot unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        weightClass = Unit.getWeightIDForName(pWeightClassString);

        if (client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).isEmpty()) {
            JOptionPane.showMessageDialog(null,
                  "You do not have any pilots for " +
                        StringUtils.aOrAn(pWeightClassString, true) +
                        " " +
                        pUnitTypeString +
                        ".",
                  "No Pilots!",
                  JOptionPane.CLOSED_OPTION);
            return;
        }

        Object[] pilots = client.getPlayer().getPersonalPilotQueue().getPilotQueue(unitType, weightClass).toArray();

        JComboBox<String> combo = new JComboBox<>();

        for (Object pilot : pilots) {
            Pilot mm = (Pilot) pilot;
            if (unitType == Unit.MEK) {
                combo.addItem(mm.getName() +
                                    " (" +
                                    mm.getGunnery() +
                                    "/" +
                                    mm.getPiloting() +
                                    ")[" +
                                    mm.getSkillString(true) +
                                    "]");
            } else {
                combo.addItem(mm.getName() + " (" + mm.getGunnery() + ")[" + mm.getSkillString(true) + "]");
            }
        }

        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select a pilot.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int position = combo.getSelectedIndex();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX +
                              "c donatepilot#" +
                              unitType +
                              "#" +
                              weightClass +
                              "#" +
                              position);
    }

    public void jMenuCommanderDirectSell_actionPerformed(String name, String id) {

        // get player
        String buyer;
        String unitID;
        String price;

        if ((name == null) || name.trim().isEmpty()) {
            PlayerNameDialog pnd = new PlayerNameDialog(client, "Buyer", PlayerNameDialog.ANY_PLAYER);
            pnd.setVisible(true);
            buyer = pnd.getPlayerName();
            pnd.dispose();
        } else {
            buyer = name;
        }

        if (buyer == null) {
            return;
        }

        if ((id == null) || id.trim().isEmpty()) {
            UnitSelectionDialog usd = new UnitSelectionDialog(client, "Unit", "Select a unit to sell");
            usd.setVisible(true);
            unitID = usd.getUnitID();
            usd.dispose();
        } else {
            unitID = id;
        }

        CUnit unit = client.getPlayer().getUnit(Integer.parseInt(unitID));

        String serviceFee = "SellDirect" +
                                  Unit.getWeightClassDesc(unit.getWeightClass()) +
                                  Unit.getTypeClassDesc(unit.getType()) +
                                  "Price";
        price = JOptionPane.showInputDialog(getContentPane(),
              "How much do you wish to offer? (" +
                    client.moneyOrFluMessage(true, true, -2) +
                    ")\n\r" +
                    "Please note a service charge of " +
                    client.moneyOrFluMessage(true, true, Integer.parseInt(client.getServerConfigs(serviceFee))) +
                    " will be added.");

        if ((price == null) || (price.isEmpty())) {
            return;
        }

        client.sendChat(
              IClient.CAMPAIGN_PREFIX +
                    "c directsellunit#" +
                    buyer +
                    "#" +
                    client.getPlayer().getName() +
                    "#" +
                    unitID +
                    "#" +
                    price);
    }

    public void jMenuMercOfferContract_actionPerformed() {
        String Amount;
        String Duration;
        PlayerNameDialog playerDialog = new PlayerNameDialog(client,
              "Which Merc do you want to offer a contract?",
              PlayerNameDialog.MERCS_ONLY);
        playerDialog.setVisible(true);
        String Merc = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (Merc == null) {
            return;
        }

        Amount = JOptionPane.showInputDialog(getContentPane(),
              "How much do you wish to offer? (" + client.moneyOrFluMessage(true, true, -2) + ")");
        if (Amount == null) {
            return;
        }

        java.util.Vector<String> techTypes = new java.util.Vector<>(5, 1);
        techTypes.add("Exp");
        techTypes.add("Land");
        techTypes.add("Units");
        techTypes.add("Components");
        techTypes.add("Delay");
        JComboBox<String> combo = new JComboBox<>(techTypes);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(this, "Select contract type.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        String Type = (String) combo.getSelectedItem();

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        Duration = JOptionPane.showInputDialog(getContentPane(), "Duration of the contract?");
        if (Duration == null) {
            return;
        }

        client.sendChat(
              IClient.CAMPAIGN_PREFIX + "c offercontract#" + Merc + "#" + Amount + "#" + Duration + "#" + Type);
    }

    public void jMenuCommanderDefect_actionPerformed() {
        // String Confirmation;
        String House;

        if (Boolean.parseBoolean(client.getServerConfigs("AllowSinglePlayerFactions"))) {
            House = JOptionPane.showInputDialog(getContentPane(),
                  "Name of your new Faction?",
                  "New Faction Name?",
                  JOptionPane.QUESTION_MESSAGE);
            if (House == null) {
                return;
            }
            String shortName = JOptionPane.showInputDialog(getContentPane(),
                  House + "'s short name?",
                  "Short Name?",
                  JOptionPane.QUESTION_MESSAGE);
            if (shortName == null) {
                return;
            }
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c defect#" + House + "#newfaction#" + shortName);

            return;
        }

        HouseNameDialog factionDialog = new HouseNameDialog(client, "Defect to faction:", false, true);
        factionDialog.setVisible(true);
        House = factionDialog.getHouseName();
        factionDialog.dispose();

        if (House == null) {
            return;
        }

        // send unconfirmed defection command
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c defect#" + House);
    }

    public void jMenuCommanderSelfPromote_actionPerformed() {

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "SubFaction",
              client.getPlayer().getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c selfpromote#" + subFactionName);
    }

    public void jMenuCommanderReportStatusMC_actionPerformed() {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c reportstatusmc#");
    }

    public void jMenuCommanderFireTechs_actionPerformed() {

        String techsToFire = JOptionPane.showInputDialog(getContentPane(),
              "How many techs do you want to fire?");
        if ((techsToFire == null) || (techsToFire.trim().isEmpty())) {
            return;
        }

        int techs = Integer.parseInt(techsToFire);
        if (!useAdvanceRepairs && (thePlayer.getTechs() <= 0)) {
            client.addToChat("<b>You have no hired techs to fire.<b>");
            return;
        }

        if (!useAdvanceRepairs && ((techs < 1) || (techs > thePlayer.getTechs()))) {
            client.addToChat("<b>Try picking a number between 1 and " + thePlayer.getTechs() + "<b>");
            return;
        }

        if (useAdvanceRepairs) {

            java.util.Vector<String> techTypes = new java.util.Vector<>(4, 1);
            techTypes.add("Green");
            techTypes.add("Regular");
            techTypes.add("Vet");
            techTypes.add("Elite");
            JComboBox<String> combo = new JComboBox<>(techTypes);
            combo.setEditable(true);
            JOptionPane jop = new JOptionPane(combo,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(client.getMainFrame(), "Select tech to fire.");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int techType = combo.getSelectedIndex();

            if (techType < 0) {
                return;
            }

            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c firetechs#" + techs + "#" + techType);
        } else {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c firetechs#" + techs);
        }
    }

    public void jMenuCommanderHireTechs_actionPerformed() {
        boolean allowRegTechs = Boolean.parseBoolean(client.getServerConfigs("AllowRegTechsToBeHired"));

        String techsToHire;

        if (useAdvanceRepairs && !allowRegTechs) {
            techsToHire = JOptionPane.showInputDialog(getContentPane(),
                  "How many green techs do you want to hire?(" +
                        Integer.parseInt(client.getServerConfigs("GreenTechHireCost")) +
                        client.moneyOrFluMessage(true, true, -2) +
                        ")");
        } else {
            techsToHire = JOptionPane.showInputDialog(getContentPane(),
                  "How many techs do you want to hire?");
        }

        if ((techsToHire == null) || (techsToHire.isEmpty())) {
            return;
        }

        int techs = Integer.parseInt(techsToHire);
        if (techs < 1) {
            client.addToChat("Try picking a number greater then 0");
            return;
        }

        if (useAdvanceRepairs && allowRegTechs) {

            java.util.Vector<String> techTypes = new java.util.Vector<>(2, 1);
            techTypes.add("Green " +
                                Integer.parseInt(client.getServerConfigs("GreenTechHireCost")) +
                                client.moneyOrFluMessage(true, true, -2));
            techTypes.add("Regular " +
                                Integer.parseInt(client.getServerConfigs("RegTechHireCost")) +
                                client.moneyOrFluMessage(true, true, -2));
            JComboBox<String> combo = new JComboBox<>(techTypes);
            combo.setEditable(false);
            JOptionPane jop = new JOptionPane(combo,
                  JOptionPane.QUESTION_MESSAGE,
                  JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(client.getMainFrame(), "Select tech to hire.");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int techType = combo.getSelectedIndex();

            if (techType < 0) {
                return;
            }

            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {
                return;
            }

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c hiretechs#" + techs + "#" + techType);
        } else {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c hiretechs#" + techs);
        }
    }

    public void jMenuCampaignSubOtherBuyPilots_actionPerformed() {
        boolean allowProto = Boolean.parseBoolean(client.getServerConfigs("UseProtoMek"));
        boolean allowAero = Boolean.parseBoolean(client.getServerConfigs("UseAero"));
        int unitType;
        int unitClass;

        Object[] pWeightClass = { "Light", "Medium", "Heavy", "Assault" };
        Object[] pUnitType;

        if (allowProto && allowAero) {
            pUnitType = new Object[] { "Mek", "Proto", "Aero" };
        } else if (allowProto) {
            pUnitType = new Object[] { "Mek", "Proto" };
        } else if (allowAero) {
            pUnitType = new Object[] { "Mek", "Aero" };
        } else {
            pUnitType = new Object[] { "Mek" };
        }

        // determine the unit type to use
        String pUnitTypeString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select unit type",
              "Unit Type Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pUnitType,
              pUnitType[0]);

        if ((pUnitTypeString == null) || (pUnitTypeString.isEmpty())) {
            return;
        }

        unitType = Unit.getTypeIDForName(pUnitTypeString);

        // determine the weight class to use
        String pWeightClassString = (String) JOptionPane.showInputDialog(client.getMainFrame(),
              "Select unit size",
              "Weight Class Selection",
              JOptionPane.INFORMATION_MESSAGE,
              null,
              pWeightClass,
              pWeightClass[0]);

        if ((pWeightClassString == null) || (pWeightClassString.isEmpty())) {
            return;
        }

        unitClass = Unit.getWeightIDForName(pWeightClassString);

        String numberOfPilots = JOptionPane.showInputDialog(getContentPane(),
              "How many pilots do you want to hire?",
              1);

        if ((numberOfPilots == null) || (numberOfPilots.isEmpty())) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX +
                              "c buypilotsfromhouse#" +
                              unitType +
                              "#" +
                              unitClass +
                              "#" +
                              numberOfPilots);
    }

    public void jMenuCommanderSellBays_actionPerformed() {
        String baysToFire = JOptionPane.showInputDialog(getContentPane(),
              "How many bays do you want to return??");

        if ((baysToFire == null) || (baysToFire.isEmpty())) {
            return;
        }

        int bays = Integer.parseInt(baysToFire);
        if (thePlayer.getFreeBays() <= 0) {
            client.addToChat("<b>You have no free bays to return.<b>");
            return;
        }
        if ((bays < 1) || (bays > thePlayer.getFreeBays())) {
            client.addToChat("<b>Try picking a number between 1 and " + thePlayer.getFreeBays() + "<b>");
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c sellbays#" + bays);
    }

    public void jMenuCampaignPartsCache_actionPerformed() {
        CPlayer p = client.getPlayer();
        StringBuilder result = new StringBuilder();
        int year = Integer.parseInt(client.getServerConfigs("CampaignYear"));

        result.append(p.getPartsCache().tableizeComponents(year));
        client.doParseDataInput("SM|" + result);
    }

    public void jMenuCommanderBuyBays_actionPerformed() {
        String baysToHire = JOptionPane.showInputDialog(getContentPane(),
              "How many bays do you want to lease?(" +
                    Integer.parseInt(client.getServerConfigs("CostToBuyNewBay")) +
                    client.moneyOrFluMessage(true, true, -2) +
                    ")");

        if ((baysToHire == null) || (baysToHire.isEmpty())) {
            return;
        }

        int bays = Integer.parseInt(baysToHire);
        if (bays < 1) {
            client.addToChat("Try picking a number greater then 0");
            return;
        }
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c buybays#" + bays);
    }

    public void jMenuHelpViewBuildTables_actionPerformed() {
        /*
         * Show the client side GUI if the requisite file is available.
         * Otherwise, make use of server commands.
         */
        // User the new BuildTableViewer
        if ((userLevel >= client.getData().getAccessLevel("AdminRequestBuildTable")) ||
                  (userLevel >= client.getData().getAccessLevel("RequestBuildTable"))) {
            BuildTableViewer btv = new BuildTableViewer(this, client);
            btv.run();
        } else {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c buildtablelist");
        }

    }

    public void jMenuHelpViewUnit_actionPerformed() {

        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(this,
              unitLoadingDialog,
              client,
              NewUnitViewerDialog.UNIT_VIEWER);
        new Thread(unitSelector).start();
    }

    public void jMenuLeaderPromote_actionPerformed() {
        String targetPlayer;

        int menuType = client.isMod() ? PlayerNameDialog.ANY_PLAYER : PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Promote", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "SubFaction",
              client.getUser(targetPlayer).getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c promoteplayer#" + targetPlayer + "#" + subFactionName);

    }

    public void jMenuLeaderDemote_actionPerformed() {
        String targetPlayer;

        int menuType = client.isMod() ? PlayerNameDialog.ANY_PLAYER : PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Demote Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        SubFactionNameDialog subFactionDialog = new SubFactionNameDialog(client,
              "Use None to remove completely",
              client.getUser(targetPlayer).getHouse());
        subFactionDialog.setVisible(true);
        String subFactionName = subFactionDialog.getSubFactionName();
        subFactionDialog.dispose();

        if ((subFactionName == null) || (subFactionName.isEmpty())) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c demoteplayer#" + targetPlayer + "#" + subFactionName);

    }

    public void jMenuLeaderFluff_actionPerformed() {
        String targetPlayer;

        int menuType = PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Fluff Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        CUser user = (CUser) client.getUser(targetPlayer);

        String newFluff = JOptionPane.showInputDialog(this,
              "Fluff? (Leave blank to remove)",
              user.getFluff());

        if (newFluff != null) {
            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                  "c FactionLeaderFluff#" +
                                  targetPlayer +
                                  "#" +
                                  newFluff);
        }
    }

    public void jMenuLeaderMute_actionPerformed() {
        String targetPlayer;

        int menuType = PlayerNameDialog.FACTION_ONLY;

        PlayerNameDialog pnd = new PlayerNameDialog(client, "Mute Player", menuType);
        pnd.setVisible(true);
        targetPlayer = pnd.getPlayerName();
        pnd.dispose();

        if (targetPlayer == null) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c FactionLeaderMute#" + targetPlayer);
    }

    public void jMenuLeaderFactionColor_actionPerformed() {
        String newColor = JOptionPane.showInputDialog(this,
              "Faction Color?",
              "Faction Color?",
              JOptionPane.QUESTION_MESSAGE);

        if (newColor != null) {
            client.sendChat(
                  IClient.CAMPAIGN_PREFIX +
                        "c ChangeHouseColor#" +
                        client.getPlayer().getHouse() +
                        "#" +
                        newColor);
        }
    }

    public void jMenuLeaderPlayerColor_actionPerformed() {
        String newColor = JOptionPane.showInputDialog(this,
              "Player Color?",
              "Player Color?",
              JOptionPane.QUESTION_MESSAGE);

        if (newColor != null) {
            client.sendChat(
                  IClient.CAMPAIGN_PREFIX +
                        "c AdminSetHousePlayerColor#" +
                        client.getPlayer().getHouse() +
                        "#" +
                        newColor);
        }
    }

    public void jMenuLeaderResearchUnit_actionPerformed() {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(this,
              unitLoadingDialog,
              client,
              NewUnitViewerDialog.UNIT_VIEWER);
        unitSelector.setName("Unit Selector");
        new Thread(unitSelector).start();
    }

    public void jMenuLeaderSetComponentConversion_actionPerformed() {
        new ComponentConverterDialog(client);
    }

    public void jMenuLeaderPurchaseFactory_actionPerformed(String planet) {
        String[] units = { Unit.getTypeClassDesc(Unit.MEK), Unit.getTypeClassDesc(Unit.VEHICLE),
                           Unit.getTypeClassDesc(Unit.INFANTRY), Unit.getTypeClassDesc(Unit.PROTOMEK),
                           Unit.getTypeClassDesc(Unit.BATTLEARMOR) };
        String[] weight = { Unit.getWeightClassDesc(Unit.LIGHT), Unit.getWeightClassDesc(Unit.MEDIUM),
                            Unit.getWeightClassDesc(Unit.HEAVY), Unit.getWeightClassDesc(Unit.ASSAULT) };

        String factoryName = JOptionPane.showInputDialog(this,
              "Factory Name?",
              "Factory Name?",
              JOptionPane.QUESTION_MESSAGE);

        if (factoryName == null) {
            return;
        }

        JComboBox<String> combo = new JComboBox<>(units);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Unit Type");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int unitType = combo.getSelectedIndex();

        if (unitType < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        combo = new JComboBox<>(weight);
        combo.setEditable(false);
        jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        dlg = jop.createDialog(client.getMainFrame(), "Unit Weight");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int unitWeight = combo.getSelectedIndex();

        if (unitWeight < 0) {
            return;
        }

        value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        if (planet == null) {
            String[] opString = { "20000", " ", " ", " ", " ", "100", "100", "0", Integer.toString(Integer.MAX_VALUE),
                                  client.getPlayer().getHouse(), "", "" };
            PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Choose a planet", opString);
            planetDialog.setVisible(true);
            planet = planetDialog.getPlanetName();
            planetDialog.dispose();

            if (planet == null) {
                return;
            }
        }

        client.sendChat(
              IClient.CAMPAIGN_PREFIX +
                    "c purchaseFactory#" +
                    factoryName +
                    "#" +
                    unitType +
                    "#" +
                    unitWeight +
                    "#" +
                    planet);
    }

    // Show data about the mek wars client and server
    public void jMenuHelpAbout_actionPerformed() {

        // make the dialog
        JDialog dlg = new JDialog(this, "MekWars Client Info");

        // set up the contents
        JPanel child = new JPanel();
        child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));

        // set the text up.
        JLabel mekwars = new JLabel("MekWars Client Version: " +
                                          IClient.CLIENT_VERSION);
        JLabel version = new JLabel("MegaMek Version: " + megamek.SuiteConstants.VERSION);
        JLabel license1 = new JLabel("MekWars Client software is under GPL. See");
        JLabel license2 = new JLabel("license.txt in ./MekWars Docs/ for details.");
        JLabel license3 = new JLabel("Project Info and Server Packages:");
        JLabel license4 = new JLabel("       http://www.sourceforge.net/projects/mekwars       ");
        JLabel data1 = new JLabel("       Datasets are prepared by server operators.       ");
        JLabel data2 = new JLabel("       Contact a server administrator for information       ");
        JLabel data3 = new JLabel("       regarding data use and redistribution.       ");

        // center everything
        mekwars.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        version.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license3.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        license4.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        data3.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        // add to child panel
        child.add(new JLabel("\n"));
        child.add(mekwars);
        child.add(version);
        child.add(new JLabel("\n"));
        child.add(license1);
        child.add(license2);
        child.add(new JLabel("\n"));
        child.add(license3);
        child.add(license4);
        child.add(new JLabel("\n"));
        child.add(data1);
        child.add(data2);
        child.add(data3);
        child.add(new JLabel("\n"));

        // then add child panel to the content pane.
        dlg.getContentPane().add(child);

        // set the location of the dialog
        java.awt.Dimension dlgSize = dlg.getPreferredSize();
        java.awt.Dimension frmSize = getSize();
        java.awt.Point loc = getLocation();
        dlg.setLocation(((frmSize.width - dlgSize.width) / 2) + loc.x, ((frmSize.height - dlgSize.height) / 2) + loc.y);
        dlg.setModal(true);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setVisible(true);
    }

    // Show data about the mek wars client memory usage
    public void jMenuHelpMemory_actionPerformed() {

        // make the dialog
        JDialog dlg = new JDialog(this, "MekWars Memory Usage");

        // set up the contents
        JPanel child = new JPanel();
        child.setLayout(new BoxLayout(child, BoxLayout.Y_AXIS));

        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        java.text.DecimalFormat myFormatter = new java.text.DecimalFormat("#,### kb");
        // set the text up.
        JLabel freeMem = new JLabel("Free Memory:          " +
                                          myFormatter.format(freeMemory / 1024));
        JLabel allocatedMem = new JLabel("Allocated Memory:  " +
                                               myFormatter.format(allocatedMemory / 1024));
        JLabel maxMem = new JLabel("Max Memory:           " +
                                         myFormatter.format(maxMemory / 1024));
        JLabel totalFreeMem = new JLabel("Total Free Memory: " +
                                               myFormatter.format((freeMemory +
                                                                         (maxMemory -
                                                                                allocatedMemory)) /
                                                                        1024));

        // center everything
        freeMem.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        allocatedMem.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        maxMem.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        totalFreeMem.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

        // add to child panel
        child.add(new JLabel("\n"));
        child.add(freeMem);
        child.add(allocatedMem);
        child.add(maxMem);
        child.add(totalFreeMem);
        child.add(new JLabel("\n"));

        // then add child panel to the content pane.
        dlg.getContentPane().add(child);

        // set the location of the dialog
        dlg.setLocationRelativeTo(this);
        dlg.setModal(false);
        dlg.setResizable(false);
        dlg.pack();
        dlg.setVisible(true);

    }

    public void jMenuHelpHelp_actionPerformed() {
        CPlayer player = client.getPlayer();
        boolean trueCost = Boolean.parseBoolean(client.getServerConfigs("UseCalculatedCosts"));
        StringBuilder result = new StringBuilder();
        result.append("<font color=\"black\">");
        result.append("MEKWARS ONLINE HELP<br>");

        result.append("<table><tr><th>Name</th><th>")
              .append(client.moneyOrFluMessage(true, false, -2))
              .append("</th><th>")
              .append(client.moneyOrFluMessage(false, false, -2))
              .append("</th><th>Components</th>");

        if (useAdvanceRepairs) {
            result.append("<th>Bays</th></tr>");
        } else {
            result.append("<th>Techs</th></tr>");
        }

        int typeamount = Unit.MAX_BUILD;
        for (int type = 0; type < typeamount; type++) {
            String useIt = "Use" + Unit.getTypeClassDesc(type);

            if (!Boolean.parseBoolean(client.getServerConfigs(useIt))) {
                continue;
            }

            for (int weight = 0; weight < 4; weight++) {

                // No reason to cycle through med-assault infantry if you are
                // only using light
                if (Boolean.parseBoolean(client.getServerConfigs("UseOnlyLightInfantry")) &&
                          (type == Unit.INFANTRY) &&
                          (weight != Unit.LIGHT)) {
                    break;
                }

                // only using one vee size means only lights are used. so no
                // reason to keep cycling if we are past the lights.
                if (Boolean.parseBoolean(client.getServerConfigs("UseOnlyOneVehicleSize")) &&
                          (type == Unit.VEHICLE) &&
                          (weight != Unit.LIGHT)) {
                    break;
                }

                result.append("<tr><td>")
                      .append(Unit.getWeightClassDesc(weight))
                      .append(" ")
                      .append(Unit.getTypeClassDesc(type))
                      .append("</td><td>");
                if (trueCost) {
                    result.append("See Unit Viewer");
                } else {
                    result.append(CUnit.getPriceForUnit(client, weight, type, player.getMyHouse()));
                }
                result.append("</td><td>").append(CUnit.getInfluenceForUnit(client,
                      weight,
                      type,
                      player.getMyHouse())).append("</td>");
                result.append("<td>")
                      .append(CUnit.getPPForUnit(client, weight, type, player.getMyHouse()))
                      .append("</td><td>");
                if (type == Unit.PROTOMEK) {
                    result.append(client.getServerConfigs("TechsToProtoPointRatio")).append(" per 5");
                } else {
                    result.append(player.getHangarSpaceRequired(type, weight, 0, ""));
                }
                result.append("</td></tr>");

            }
        }
        result.append("</table>");

        result.append("<br><b><i>Repoding facts:</b></i>");
        result.append("<table><tr><th>Class</th><th>")
              .append(client.moneyOrFluMessage(true, false, -2))
              .append("</th><th>")
              .append(client.moneyOrFluMessage(false, false, -2))
              .append("</th><th>Components</th></tr>");
        typeamount = 1;
        for (int type = 0; type < typeamount; type++) {
            for (int weight = 0; weight < 4; weight++) {
                String rePodFlu = "RePodFlu" + Unit.getWeightClassDesc(weight);
                String rePodCost = "RePodCost" + Unit.getWeightClassDesc(weight);
                String rePodComponents = "RePodComp" + Unit.getWeightClassDesc(weight);

                int rePodCostInt = Integer.parseInt(client.getServerConfigs(rePodCost));
                int rePodComponentsInt = Integer.parseInt(client.getServerConfigs(rePodComponents));

                if (!Boolean.parseBoolean(client.getServerConfigs("DoesRePodCost"))) {
                    rePodCostInt = 0;
                }

                if (!Boolean.parseBoolean(client.getServerConfigs("RePodUsesComp"))) {
                    rePodComponentsInt = 0;
                }

                result.append("<tr><td>")
                      .append(Unit.getWeightClassDesc(weight))
                      .append("</td><td>")
                      .append(rePodCostInt)
                      .append("</td><td>")
                      .append(client.getServerConfigs(rePodFlu))
                      .append("</td><td>")
                      .append(rePodComponentsInt)
                      .append("</td></tr>");
            }
        }

        result.append("</table><br>");
        if (useAdvanceRepairs) {
            result.append("<br><b><i>Tech/BayCosts:</b></i>");
            result.append("<table><tr><th>Type</th><th>")
                  .append(client.moneyOrFluMessage(true, false, -2))
                  .append("</th>");
            result.append("<tr><td>Bay Cost</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("CostToBuyNewBay")))
                  .append("</td></tr>");
            result.append("<tr><td>Bay Sale</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("BaySellBackPrice")))
                  .append("</td></tr>");
            result.append("<tr><td>Green Tech</td><td>")
                  .append(Integer.parseInt(client.getServerConfigs("GreenTechHireCost")))
                  .append("</td></tr>");

            if (Boolean.parseBoolean(client.getServerConfigs("AllowRegTechsToBeHired"))) {
                result.append("<tr><td>Reg Tech</td><td>")
                      .append(Integer.parseInt(client.getServerConfigs("RegTechHireCost")))
                      .append("</td></tr>");
            }
            result.append("</table>");
        }
        result.append("<table><tr><th>Size of Unit</th><th>EXP needed</th></tr>");
        result.append("<tr><td>Light</td><td>")
              .append(client.getServerConfigs("MinEXPforLight"))
              .append("</td></tr>"); //@salient
        result.append("<tr><td>Medium</td><td>")
              .append(client.getServerConfigs("MinEXPforMedium"))
              .append("</td></tr>");
        result.append("<tr><td>Heavy</td><td>").append(client.getServerConfigs("MinEXPforHeavy")).append("</td></tr>");
        result.append("<tr><td>Assault</td><td>")
              .append(client.getServerConfigs("MinEXPforAssault"))
              .append("</td></tr>");
        result.append("</table>");
        result.append("EXP needed to Buy/Sell on the Black Market: ")
              .append(client.getServerConfigs("MinEXPforBMBuying"))
              .append("/")
              .append(client.getServerConfigs("MinEXPforBMSelling"))
              .append("<br>");
        result.append("EXP needed to defect from ")
              .append(client.getServerConfigs("NewbieHouseName"))
              .append(" to a Faction: ")
              .append(client.getServerConfigs("MinEXPforDefecting"))
              .append("<br>");

        result.append("</table><br>");

        result.append("<b><i>Unit Status Icons</B></i><br>");
        result.append("<table><tr><th>Icon</th><th>Description</th></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/ammo.gif\"></td><td>Unit has all ammo bins loaded</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/low.gif\"></td><td>Unit has 1 or more ammo bins that are low on ammo</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/empty.gif\"></td><td>Unit has 1 or more ammo bins that are empty</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/armor.gif\"></td><td>Unit has armor damage</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/structure.gif\"></td><td>Unit has IS damage</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/critical.gif\"></td><td>Unit has criticals damaged</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/engine.gif\"></td><td>Unit is engined</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/nopilot.gif\"></td><td>Unit has no pilot</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/pilot.gif\"></td><td>Unit has a pilot</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/wound.gif\"></td><td>Unit has a wounded pilot</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/eject.gif\"></td><td>Unit has autoejection enabled</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/noeject.gif\"></td><td>Unit has autoejection disabled</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/pending.gif\"></td><td>Unit has pending repairs</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/repairing.gif\"></td><td>Unit is currently under repair</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/unmaint.gif\"></td><td>Unit is unmaintained/damaged</td></tr>");
        result.append("<tr><td><img src=\"data/images/status/maint.gif\"></td><td>Unit is fully maintained</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/search.gif\"></td><td>Unit is equiped with a slite</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/searchon.gif\"></td><td>Unit is equiped with a slite and defaults to on</td></tr>");
        result.append(
              "<tr><td><img src=\"data/images/status/nosearch.gif\"></td><td>Unit is not equiped with a slite</td></tr>");
        result.append("</table><br>");

        result.append("</table><br>");

        if (!client.getData().getServerBannedAmmo().isEmpty()) {
            result.append("<b><i>Server Banned ammo</b></i><br>");
            for (AmmoType.Munitions bannedAmmo : client.getData().getServerBannedAmmo()) {
                result.append(client.getData().getMunitionsByNumber().get(bannedAmmo)).append("<br>");
            }
        }

        House faction = client.getData().getHouseByName(client.getPlayer().getHouse());
        if (!faction.getBannedAmmo().isEmpty()) {
            result.append("<b><i>House Banned Ammo</b></i><br>");
            for (AmmoType.Munitions bannedAmmo : faction.getBannedAmmo()) {
                result.append(client.getData().getMunitionsByNumber().get(bannedAmmo)).append("<br>");
            }
        }

        /*
         * use process incoming, instead of adding directly to misc, so that
         * output is directly to main if misc in main is enabled or players has
         * the misc. tab off.
         */
        client.doParseDataInput("SM|" + result);
    }

    private String pilotSkillBVBlurbLine(String a, String b) {//BK added
        // builds help menu's pilot skill bv blurb, wants a and b to build server config lookup and get the value
        int i = Integer.parseInt(client.getServerConfigs("chancefor" + a + "for" + b));

        if (i > 0) {
            return " " + b + " xp cost: " + i;
        } else {
            return "";
        }
    }

    private String pilotSkillBlurbLine(String skill, String fullName, String shortName) {//BK added
        // builds help menu's pilot skill blurb line, wants skill and fullName and shortName as skill, full name and skill shortname and
        // description, e.g. "Astech" and "AT" and "does this..."
        String s = " ";
        //find if there is any chance for this skill, and if yes, create entry
        s += pilotSkillBVBlurbLine(fullName, "Mek") +
                   pilotSkillBVBlurbLine(fullName, "Vehicle") +
                   pilotSkillBVBlurbLine(fullName, "Infantry") +
                   pilotSkillBVBlurbLine(fullName, "ProtoMek") +
                   pilotSkillBVBlurbLine(fullName, "BattleArmor") +
                   pilotSkillBVBlurbLine(fullName, "Aero");//not sure where to get skill reiterable list for unit types
        if (s.length() > 1) {
            s = "<tr><td>" + skill + "</td><td>" + fullName + "</td><td>" + shortName + "<br>" + s + "</td></tr>";
        }
        return s;
    }

    public void jMenuHelpPilotSkills_actionPerformed() {
        //BK;  would prefer to have this Help Menu list built using a reiteration of the pilot skills by pulling the
        // info from those classes step one was adding pilot xp costs to the help menu, step two will be adding bv
        // costs, drawn via the skill
        String result = "";
        result += "<font color=\"black\">";
        result += "<b><i>MekWars/MegaMek Pilot Skills</b></i><br>";
        result += "<table><tr><th>Name</th>" + "<th>Abbreviation</th>" + "<th>Description</th></tr>";
        if (useAdvanceRepairs) {
            result += pilotSkillBlurbLine("Astech", "AT", "Pilot acts as a tech with repairs only costing parts");
        } else {
            result += pilotSkillBlurbLine("Astech", "AT", "Reduces the number of techs needed to repair a unit by 1");
        }
        result += pilotSkillBlurbLine("MD Buffered VDNI", "BVDNI", "Allows pilots to take more damage.");
        result += pilotSkillBlurbLine("Dodge Maneuver",
              "DM",
              "Enables the unit to make a dodge maneuver instead of a physical attack.<br>This maneuver adds +2 to the BTH to physical attacks against the unit.");
        result += pilotSkillBlurbLine("Edge", "ED", "Allows Pilot to reroll 1 roll(per level) per game.");
        result += pilotSkillBlurbLine("Enhanced Interface",
              "EI",
              "Neural interface to the clan enhanced imaging system<br>-1 To PSR<br>+2 when targeting with TC instead of +3<br>Can Target without TC at +6<br>Reduces all forest and Smoke mods to 1<br>Pilot receives 1 point of damage every time Units IS is hit,<br>If you fail a roll of 7+<br>BA's recieve 1 extra point of damage every time they are hit.");
        result += pilotSkillBlurbLine("Gifted",
              "GT",
              "Pilots receive an extra " +
                    client.getServerConfigs("GiftedPercent") +
                    "% chance to gain a skill when they fail<br>to level Piloting or Gunnery after a win.");
        result += pilotSkillBlurbLine("Gunnery Ballistic",
              "GB",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>ballistic weapons (MGs, all ACs, Gaussrifles).");
        result += pilotSkillBlurbLine("Gunnery Laser",
              "GL",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>energy-based weapons (Laser, PPC, and Flamer).");
        result += pilotSkillBlurbLine("Gunnery Missile",
              "GM",
              "NOTE: This is an unofficial rule. Pilot gets a -1 to-hit bonus on all<br>missile weapons (LRM, MRM, SRM).");
        result += pilotSkillBlurbLine("Iron Man",
              "IM",
              "NOTE: This is an unofficial rule. A pilot with this skill receives only<br>1 pilot hit from ammunition explosions.");
        result += pilotSkillBlurbLine("Maneuvering Ace",
              "MA",
              "Enables the unit to move laterally like a Quad. Units also receive a -1<br>BTH to rolls against skidding.");
        result += pilotSkillBlurbLine("Melee Specialist",
              "MS",
              "Enables the unit to do 1 additional point of damage with physical attacks<br>and subtracts one from the attacker movement modifier (to a minimum of zero).");
        result += pilotSkillBlurbLine("MedTech",
              "MT",
              "A pilot with the MedTech skill will heal 1 extra point per tick.");
        result += pilotSkillBlurbLine("Natural Aptitude: Gunnery",
              "NAG",
              "The pilot checks leveling for gunnery at one level higher then current i.e.<br>5 instead of 4 for a 4/5 pilot.");
        result += pilotSkillBlurbLine("Natural Aptitude: Piloting",
              "NAP",
              "The pilot checks leveling for piloting at one level higher then current i.e.<br>6 instead of 5 for a 4/5 pilot.");
        result += pilotSkillBlurbLine("Pain Resistance",
              "PR",
              "When making consciousness rolls, 1 is added to all rolls. Also, damage received<BR>from ammo explosions is reduced to 1.");
        result += pilotSkillBlurbLine("Pain Shunt",
              "PS",
              "When making consciousness rolls, 1 is added to all rolls. Also, damage received<BR>from ammo explosions is reduced to 1.");
        result += pilotSkillBlurbLine("Quick Study",
              "QS",
              "Pilots with the Quick Study skill gain a 5% bonus to all XP earned.");
        result += pilotSkillBlurbLine("Survivalist",
              "SV",
              "If a pilot has this skill they will have a +20% of returning home if ejected and<br>left on the field.");
        result += pilotSkillBlurbLine("Tactical Genius",
              "TG",
              "A pilot who has a Tactical Genius may reroll their initiative once per turn.<br>The second roll must be accepted.");
        result += pilotSkillBlurbLine("Trait", "TN", "Pilot traits for use with moding the gaining of other skills.");
        result += pilotSkillBlurbLine("VDNI MD Skill", "VDNI", "Allows Pilot to Take more Damage.");
        result += pilotSkillBlurbLine("Weapon Specialist",
              "WS",
              "A pilot who specializes in a particular weapon receives a -2 to hit modifier<br>on all attacks with that weapon.");
        result += pilotSkillBlurbLine("Clan Pilot Training",
              "CPT",
              "Pilot has a +1 penalty for physical attacks,<br>because clans do not train for dishonourable combat.");
        result += "</table>";

        /*
         * use process incoming, instead of adding directly to misc, so that
         * output is directly to main if misc in main is enabled or player's
         * misc. tab is disabled.
         */
        client.doParseDataInput("SM|" + result);

    }

    public void showMulFileList(String data) {

        java.util.StringTokenizer mulList = new java.util.StringTokenizer(data, "#");

        java.util.Vector<String> list = new java.util.Vector<>(1, 1);

        while (mulList.hasMoreElements()) {
            list.add(mulList.nextToken());
        }

        JComboBox<String> combo = new JComboBox<>(list);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select mul file.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        if (combo.getSelectedIndex() < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }
        String selectedMul = list.elementAt(combo.getSelectedIndex());

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c retrievemul#" + selectedMul);
    }

    /*
     * Admin methods used to be here. These have been extracted into a seperate
     * .jar file. @urgru
     */

    /*
     * Moderator-specific methods used to be here. These have been extracted
     * into a seperate .jar file. @urgru
     */

    public void refreshBattleTable() {
        MainPanel.refreshBattleTable();
    }

    public void setSoundMuted(boolean b) {
        jMenuOptionsMute.setState(b);
    }

    public void updateAttackMenu() {

        // login call of UOE occures before the menu is
        // created. return to stop NPE's.
        if (jMenuAttackMenu == null) {
            MWLogger.errLog("Attack Menu is Null!");
            return;
        }

        jMenuAttackMenu.updateMenuItems(true);
    }

    public void startHost() {
        jMenuHost.setForeground(java.awt.Color.red);
        jMenuCSHostAndJoin.setEnabled(false);
        jMenuCSHostDedicated.setEnabled(false);
        jMenuCSHostStop.setEnabled(true);
        jMenuCSHostAndJoin.setVisible(false);
        jMenuCSHostDedicated.setVisible(false);
        jMenuCSHostLoad.setVisible(false);
        jMenuCSHostLoadAndJoin.setVisible(false);
        jMenuCSHostStop.setVisible(true);
    }

    public void stopHost() {
        jMenuHost.setForeground(java.awt.Color.black);
        jMenuCSHostAndJoin.setEnabled(true);
        jMenuCSHostDedicated.setEnabled(true);
        jMenuCSHostStop.setEnabled(false);
        jMenuCSHostAndJoin.setVisible(true);
        jMenuCSHostDedicated.setVisible(true);
        jMenuCSHostLoad.setVisible(true);
        jMenuCSHostLoadAndJoin.setVisible(true);
        jMenuCSHostStop.setVisible(false);
    }

    public void changeStatus(int status, int lastStatus) {

        if (client.getConfig().isParam("STATUSINTRAYICON")) {
            if (status == IClient.STATUS_RESERVE) {
                try {
                    setIconImage(client.getConfig().getImage("RESERVE").getImage());
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            } else if (status == IClient.STATUS_ACTIVE) {
                try {
                    setIconImage(client.getConfig().getImage("ACTIVE").getImage());
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            } else if (status == IClient.STATUS_FIGHTING) {
                try {
                    setIconImage(client.getConfig().getImage("FIGHT").getImage());
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            } else if ((status == IClient.STATUS_LOGGED_OUT) || (status == IClient.STATUS_DISCONNECTED)) {
                try {
                    setIconImage(client.getConfig().getImage("LOGOUT").getImage());
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }
        }

        // if not showing status, show the operator's custom icon
        else {
            try {
                setIconImage(client.getConfig().getImage("TRAY").getImage());
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }

        MainPanel.changeStatus(status, lastStatus);
        enableMenu();
        repaint();
    }

    public void createArmyFromMul(String data) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(client, "Choose a Player.", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String player = playerDialog.getPlayerName();
        playerDialog.dispose();

        if (player == null) {
            player = "";
        }

        System.err.println("String Tokenizer called");
        java.util.StringTokenizer mulList = new java.util.StringTokenizer(data, "#");

        java.util.Vector<String> list = new java.util.Vector<>(1, 1);

        // System.err.println("adding mul's to vector");
        while (mulList.hasMoreElements()) {
            list.add(mulList.nextToken());
        }

        // System.err.println("creating combo box.");

        JComboBox<String> combo = new JComboBox<>(list);
        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(client.getMainFrame(), "Select mul file.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        if (combo.getSelectedIndex() < 0) {
            return;
        }

        int value = (Integer) jop.getValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }
        String selectedMul = list.elementAt(combo.getSelectedIndex());

        String fluff = JOptionPane.showInputDialog(getContentPane(), "Army Name.");
        if ((fluff == null) || (fluff.isEmpty())) {
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX +
                              "createarmyfrommul " +
                              selectedMul +
                              "#" +
                              fluff +
                              "#" +
                              player);
    }

    public void jMenuSendAllOperationFiles_actionPerformed(java.awt.event.ActionEvent e) {

        int result = JOptionPane.showConfirmDialog(null,
              "Upload All local OpFiles?",
              "Upload Ops",
              JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.NO_OPTION) {
            return;
        }

        java.io.File opFiles = new java.io.File("./data/operations/short/");

        if (!opFiles.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        for (java.io.File opFile : opFiles.listFiles()) {
            try {
                if (!opFile.getName().endsWith(".txt")) {
                    continue;
                }
                java.io.FileInputStream fis = new java.io.FileInputStream(opFile);
                java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
                opData.append(opFile.getName(), 0, opFile.getName().lastIndexOf(".txt")).append("#");
                while (dis.ready()) {
                    opData.append(dis.readLine().replace("#", "(pound)")).append("#");
                }
                dis.close();
                fis.close();

            } catch (Exception ex) {
                MWLogger.errLog("Unable to read " + opFile);
                return;
            }
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c setoperation#short#" + opData);
            opData.setLength(0);
            opData.trimToSize();
        }
    }

    public void jMenuSetNewOperationFile_actionPerformed(java.awt.event.ActionEvent e) {

        String opName = JOptionPane.showInputDialog(client.getMainFrame().getContentPane(),
              "New Op Name?");

        if ((opName == null) || (opName.trim().isEmpty())) {
            return;
        }

        java.io.File opFile = new java.io.File("./data/operations/short/" + opName + ".txt");

        if (!opFile.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(opFile);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            opData.append(opName).append("#");
            while (dis.ready()) {
                opData.append(dis.readLine().replace("#", "(pound)")).append("#");
            }
            dis.close();
            fis.close();

        } catch (Exception ex) {
            MWLogger.errLog("Unable to read " + opFile);
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c setoperation#short#" + opData);
    }

    public void jMenuUpdateOperations_actionPerformed(java.awt.event.ActionEvent e) {
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c adminlockcampaign");
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c updateoperations");
        client.sendChat(IClient.CAMPAIGN_PREFIX + "c adminunlockcampaign");

    }

    public void jMenuRetrieveOperationFile_actionPerformed(java.awt.event.ActionEvent e) {
        JComboBox<String> opCombo = new JComboBox<>(client.getAllOps()
                                                          .keySet()
                                                          .toArray(new String[0]));
        opCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(opCombo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(this, "Select Op.");
        opCombo.grabFocus();
        opCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        String opName = (String) opCombo.getSelectedItem();

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c RETRIEVEOPERATION#short#" + opName);
    }

    public void jMenuSetOperationFile_actionPerformed(java.awt.event.ActionEvent e) {

        JComboBox<String> opCombo = new JComboBox<>();
        client.getAllOps().keySet().forEach(opCombo::addItem);
        opCombo.setEditable(false);

        JOptionPane jop = new JOptionPane(opCombo,
              JOptionPane.QUESTION_MESSAGE,
              JOptionPane.OK_CANCEL_OPTION);
        JDialog dlg = jop.createDialog(this, "Select Op.");
        opCombo.grabFocus();
        opCombo.getEditor().selectAll();

        dlg.setVisible(true);

        if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
            return;
        }

        String opName = (String) opCombo.getSelectedItem();

        java.io.File opFile = new java.io.File("./data/operations/short/" + opName + ".txt");

        if (!opFile.exists()) {
            return;
        }

        StringBuilder opData = new StringBuilder();

        try {
            java.io.FileInputStream fis = new java.io.FileInputStream(opFile);
            java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));
            opData.append(opName).append("#");
            while (dis.ready()) {
                opData.append(dis.readLine().replace("#", "(pound)")).append("#");
            }
            dis.close();
            fis.close();

        } catch (Exception ex) {
            MWLogger.errLog("Unable to read " + opFile);
            return;
        }

        client.sendChat(IClient.CAMPAIGN_PREFIX + "c setoperation#short#" + opData);
    }

    private void addMenuListener(Object[] components) {
        for (Object menu : components) {
            if (menu instanceof JMenu jmenu) {
                jmenu.removeMenuListener(sound);
                jmenu.addMenuListener(sound);
                addMenuItemListener(jmenu);
            }
        }
    }

    private void addMenuItemListener(JMenu menu) {
        for (int pos = 0; pos < menu.getItemCount(); pos++) {
            JMenuItem item = menu.getItem(pos);
            if (item instanceof JMenu) {
                ((JMenu) item).removeMenuListener(popupSound);
                ((JMenu) item).addMenuListener(popupSound);
                addMenuItemListener((JMenu) item);
            }
        }

    }
}

