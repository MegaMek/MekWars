/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.common.gui;

import common.BMEquipment;
import common.util.MWLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Main panel
 */

public class CMainPanel extends javax.swing.JPanel
      implements javax.swing.event.ChangeListener, java.awt.event.ComponentListener {
    /**
     *
     */
    private static final long serialVersionUID = -7817596095411018999L;
    javax.swing.JSplitPane MainSPane;
    javax.swing.JSplitPane TabSPane;
    javax.swing.JSplitPane SideSPane;
    CPlayerPanel PlayerPanel;
    CUserListPanel UserListPanel;
    javax.swing.JTabbedPane MainTPane = new javax.swing.JTabbedPane(javax.swing.SwingConstants.BOTTOM);
    CCommPanel CommPanel;
    mekwars.common.gui.CMainPanel.CSelectTabAction CommSelect;
    CHQPanel HQPanel = null;
    CBMPanel BMPanel = null;
    CHSPanel HSPanel = null;
    CRulesPanel RulesPanel = null; //@salient
    javax.swing.JTabbedPane BMETabbed = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction HQSelect = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction RulesSelect = null; //@salient
    mekwars.common.gui.CMainPanel.CSelectTabAction BMSelect = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction HSSelect = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction BMESelect = null;

    CBattlePanel BattlePanel = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction BattleSelect = null;
    CMapPanel MapPanel = null;
    mekwars.common.gui.CMainPanel.CSelectTabAction MapSelect = null;

    IClient mwclient;

    mekwars.common.gui.CMainPanel.CTabForwardAction ForwardMainTab;
    mekwars.common.gui.CMainPanel.CTabBackwardAction BackwardMainTab;
    int panelDivider;
    int playerPanelDivider;
    int verticalPanelDivider;
    int sPanelDivider = 0;

    public CMainPanel(IClient client, CMainFrame mainFrame) {
        mwclient = client;
        setLayout(new java.awt.BorderLayout());
        setMinimumSize(new java.awt.Dimension(620, 400));
        addComponentListener(this);
        createMainTPane(mainFrame);
        PlayerPanel = new CPlayerPanel(mwclient);
        UserListPanel = new CUserListPanel(mwclient);
        add(PlayerPanel, java.awt.BorderLayout.NORTH);
        panelDivider = mwclient.getConfig().getIntParam("PANELDIVIDER");
        //panelDivider = panelDividerInt;//(double)panelDividerInt/100;
        sPanelDivider = mwclient.getConfig().getIntParam("SPLITTERSIZE");
        playerPanelDivider = mwclient.getConfig().getIntParam("PLAYERPANELDIVIDER");
        verticalPanelDivider = mwclient.getConfig().getIntParam("VERTICALDIVIDER");

        TabSPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, MainTPane, CommPanel);
        TabSPane.setOneTouchExpandable(true);
        TabSPane.setDividerLocation(panelDivider);
        TabSPane.setDividerSize(sPanelDivider);

        //    UserListPanel.setMinimumSize(new Dimension(100, 100));
        //    PlayerPanel.setMinimumSize(new Dimension(100, 100));
        SideSPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, PlayerPanel, UserListPanel);
        //    SideSPane.add(PlayerPanel);
        SideSPane.setOneTouchExpandable(true);
        SideSPane.setDividerLocation(playerPanelDivider);
        SideSPane.setDividerSize(sPanelDivider);
        MainSPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.HORIZONTAL_SPLIT, TabSPane, SideSPane);
        MainSPane.setOneTouchExpandable(true);
        MainSPane.setDividerLocation(verticalPanelDivider);
        MainSPane.setDividerSize(sPanelDivider);
        add(MainSPane, java.awt.BorderLayout.CENTER);

        ForwardMainTab = new mekwars.common.gui.CMainPanel.CTabForwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt X"), "TabForward");
        getActionMap().put("TabForward", ForwardMainTab);
        BackwardMainTab = new mekwars.common.gui.CMainPanel.CTabBackwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("shift alt X"), "TabBackward");
        getActionMap().put("TabBackward", BackwardMainTab);
        revalidate();
    }

    private void createMainTPane(CMainFrame mainFrame) {
        //int index;
        //int mnemo;

        CommPanel = new CCommPanel(mwclient);

        String tabText = "";
        String mnemonicText = "";
        if (mwclient.getConfig().isParam("HQTABVISIBLE")) {
            HQPanel = new CHQPanel(mwclient);
            HQSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(HQPanel);
            tabText = mwclient.getConfig().getParam("HQTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HQMNEMONIC");
            if (mwclient.getConfig().isParam("HQINTOPROW")) {
                addPanelMain(HQPanel,
                      HQSelect,
                      tabText,
                      "Command Center and Hangars (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      "Command Center and Hangars (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HQSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BMTABVISIBLE")) {
            BMPanel = new CBMPanel(mwclient);
            BMSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BMPanel);
            tabText = mwclient.getConfig().getParam("BMTABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMMNEMONIC");
            if (mwclient.getConfig().isParam("BMINTOPROW")) {
                addPanelMain(BMPanel,
                      BMSelect,
                      tabText,
                      "Buy and Sell Units (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      "Buy and Sell Units (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BMETABVISIBLE") &&
                  Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket"))) {
            BMETabbed = new javax.swing.JTabbedPane(javax.swing.SwingConstants.BOTTOM);
            BMETabbed.addTab("Ammo", new CBMPartsPanel(mwclient, BMEquipment.PART_AMMO));
            BMETabbed.addTab("Armor", new CBMPartsPanel(mwclient, BMEquipment.PART_ARMOR));
            BMETabbed.addTab("Weapons", new CBMPartsPanel(mwclient, BMEquipment.PART_WEAPON));
            BMETabbed.addTab("Misc", new CBMPartsPanel(mwclient, BMEquipment.PART_MISC));
            BMESelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BMETabbed);
            tabText = mwclient.getConfig().getParam("BMETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMEMNEMONIC");
            if (mwclient.getConfig().isParam("BMEINTOPROW")) {
                addPanelMain(BMETabbed,
                      BMESelect,
                      tabText,
                      "Buy and Sell Parts (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMESelect,
                      tabText,
                      "Buy and Sell Parts (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMESelect",
                      CommPanel);
            }
        }

        HSPanel = new CHSPanel(mwclient);
        if (mwclient.getConfig().isParam("HSTATUSTABVISIBLE")) {
            HSSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(HSPanel);
            tabText = mwclient.getConfig().getParam("HSTATUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HSTATUSMNEMONIC");
            if (mwclient.getConfig().isParam("HSTATUSINTOPROW")) {
                addPanelMain(HSPanel,
                      HSSelect,
                      tabText,
                      "Show current House Status (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      "Show current House Status (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HSSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BATTLETABVISIBLE")) {
            BattlePanel = new CBattlePanel(mwclient);
            BattleSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BattlePanel);
            tabText = mwclient.getConfig().getParam("BATTLETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BATTLEMNEMONIC");
            if (mwclient.getConfig().isParam("BATTLEINTOPROW")) {
                addPanelMain(BattlePanel,
                      BattleSelect,
                      tabText,
                      "Battles Intelligence Data (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      "Battles Intelligence Data (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BattleSelect",
                      CommPanel);
            }
        }

        MapPanel = new CMapPanel(mwclient, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());
        if (mwclient.getConfig().isParam("MAPTABVISIBLE")) {
            MapSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(MapPanel);
            tabText = mwclient.getConfig().getParam("MAPTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MAPMNEMONIC");
            if (mwclient.getConfig().isParam("MAPINTOPROW")) {
                addPanelMain(MapPanel,
                      MapSelect,
                      tabText,
                      "Star Map (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      "Star Map (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "MapSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("RULESTABVISIBLE")) //@salient
        {
            RulesPanel = new CRulesPanel(mwclient);
            RulesSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(RulesPanel);
            tabText = mwclient.getConfig().getParam("RULESTABNAME");
            mnemonicText = mwclient.getConfig().getParam("RULESMNEMONIC");
            if (mwclient.getConfig().isParam("RULESINTOPROW")) {
                addPanelMain(RulesPanel,
                      RulesSelect,
                      tabText,
                      "Rules Tab (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      "Rules Tab (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    private void addPanelMain(
          javax.swing.JPanel panel,
          mekwars.common.gui.CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemostr,
          String commandStr) {
        MainTPane.addTab(name, null, panel, tooltip);
        int index = MainTPane.indexOfComponent(panel);
        int mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
        if (mnemo == -1) {mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());}
        MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemostr), commandStr);
        getActionMap().put(commandStr, select);
    }

    private void addPanelCComm(
          javax.swing.JPanel panel,
          mekwars.common.gui.CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemostr,
          String commandStr,
          CCommPanel CommPanel) {
        CommPanel.CommTPane.addTab(name, null, panel, tooltip);
        int index = CommPanel.CommTPane.indexOfComponent(panel);
        int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
        if (mnemo == -1) {mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());}
        CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW)
              .put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemostr), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    //Why is this defined twice? - salient
    private void addPanelMain(
          javax.swing.JTabbedPane panel,
          mekwars.common.gui.CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemostr,
          String commandStr) {
        MainTPane.addTab(name, null, panel, tooltip);
        int index = MainTPane.indexOfComponent(panel);
        int mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
        if (mnemo == -1) {mnemo = MainTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());}
        MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemostr), commandStr);
        getActionMap().put(commandStr, select);
    }

    private void addPanelCComm(
          javax.swing.JTabbedPane panel,
          mekwars.common.gui.CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemostr,
          String commandStr,
          CCommPanel CommPanel) {
        CommPanel.CommTPane.addTab(name, null, panel, tooltip);
        int index = CommPanel.CommTPane.indexOfComponent(panel);
        int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toUpperCase());
        if (mnemo == -1) {mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemostr.toLowerCase());}
        CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW)
              .put(javax.swing.KeyStroke.getKeyStroke("alt " + mnemostr), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    public void changeStatus(int status, int laststatus) {

        if (status == IClient.STATUS_RESERVE) {
            if (laststatus == IClient.STATUS_LOGGEDOUT) {
                UserListPanel.setLoggedIn(true);
                UserListPanel.getUsers().getRenderer().setLoggedIn(true);
                if (mwclient.getConfig().isParam("PLAYERPANEL")) {PlayerPanel.setVisible(true);}
                TabSPane.setDividerLocation(panelDivider);
                SideSPane.setDividerLocation(playerPanelDivider);
            }
        }

        if (status == IClient.STATUS_DISCONNECTED || status == IClient.STATUS_LOGGEDOUT) {
            UserListPanel.setLoggedIn(false);
            UserListPanel.getUsers().getRenderer().setLoggedIn(false);
            PlayerPanel.setVisible(false);
        }

        if (status == IClient.STATUS_DISCONNECTED) {MainTPane.setVisible(false);} else {
            MainTPane.setVisible(true);
        }
    }

    /**
     * A method which selects the FIRST tab, whatever it may be.
     */
    public void selectFirstTab() {
        try {
            MainTPane.setSelectedIndex(0);
        } catch (Exception e) {
            //do nothing. just means no upper-level tabs.
        }
    }

    /**
     * A method which selects the map tab, whether it is in the main or comm panel, and sends it to the front.
     * <p>
     * Used by MMNETHyperLinkListener if MAPTABONCLICK is set.
     */
    public void selectMapTab() {

        //get the map name
        String nameToFind = mwclient.getConfigParam("MAPTABNAME");

        //look for map in main/top
        for (int i = MainTPane.getTabCount() - 1; 0 <= i; i--) {
            String currTitle = MainTPane.getTitleAt(i);
            if (currTitle.equals(nameToFind)) {
                MainTPane.setSelectedIndex(i);
                return;
            }
        }

        //look for map in chat/bottom
        for (int i = CommPanel.CommTPane.getTabCount() - 1; 0 <= i; i--) {
            String currTitle = CommPanel.CommTPane.getTitleAt(i);
            if (currTitle.equals(nameToFind)) {
                CommPanel.CommTPane.setSelectedIndex(i);
                return;
            }
        }
    }//end selectMapTab

    public CPlayerPanel getPlayerPanel() {return PlayerPanel;}

    public void setPlayerPanel(CPlayerPanel panel) {PlayerPanel = panel;}

    public CMapPanel getMapPanel() {return MapPanel;}

    public javax.swing.JSplitPane getTabSPane() {return TabSPane;}

    public javax.swing.JSplitPane getMainSPane() {return MainSPane;}

    public javax.swing.JSplitPane getSideSPane() {return SideSPane;}

    public CUserListPanel getUserListPanel() {return UserListPanel;}

    public CCommPanel getCommPanel() {return CommPanel;}

    public CHQPanel getHQPanel() {return HQPanel;}

    public CBMPanel getBMPanel() {return BMPanel;}

    public CHSPanel getHSPanel() {return HSPanel;}

    public void refreshBME() {
        if (BMETabbed == null) {return;}
        try {
            ((CBMPartsPanel) BMETabbed.getComponentAt(0)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(1)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(2)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(3)).refresh();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public javax.swing.JTable getBattleTable() {return BattlePanel.getBattleTable();}

    public void refreshBattleTable() {BattlePanel.getBattleTableModel().refreshModel();}


    // change listener
    public void stateChanged(javax.swing.event.ChangeEvent e) {
        if (MainTPane.getSelectedIndex() == -1) {return;}
        if (MainTPane.getSelectedComponent() == CommPanel) {CommPanel.getInputField().requestFocusInWindow();}
    }

    public void componentResized(java.awt.event.ComponentEvent e) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    public void componentMoved(java.awt.event.ComponentEvent e) {}

    public void componentShown(java.awt.event.ComponentEvent e) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    // component listener
    public void componentHidden(java.awt.event.ComponentEvent e) {}

    // component listener
    // actions

    public void recreateMainTPane(CMainFrame mainFrame) {
        //int index;
        //int mnemo;

        MainTPane.removeAll();
        CommPanel.CommTPane.removeAll();
        CommPanel.reload();

        String tabText = "";
        String mnemonicText = "";
        if (mwclient.getConfig().isParam("HQTABVISIBLE")) {
            if (HQPanel == null) {
                HQPanel = new CHQPanel(mwclient);
                HQSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(HQPanel);
            }
            tabText = mwclient.getConfig().getParam("HQTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HQMNEMONIC");
            if (mwclient.getConfig().isParam("HQINTOPROW")) {
                addPanelMain(HQPanel,
                      HQSelect,
                      tabText,
                      "Command Center and Hangars (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      "Command Center and Hangars (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HQSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BMTABVISIBLE")) {
            if (BMPanel == null) {
                BMPanel = new CBMPanel(mwclient);
                BMSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BMPanel);
            }
            tabText = mwclient.getConfig().getParam("BMTABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMMNEMONIC");
            if (mwclient.getConfig().isParam("BMINTOPROW")) {
                addPanelMain(BMPanel,
                      BMSelect,
                      tabText,
                      "Buy and Sell Units (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      "Buy and Sell Units (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BMETABVISIBLE") &&
                  Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket"))) {
            if (BMETabbed == null) {
                BMETabbed = new javax.swing.JTabbedPane(javax.swing.SwingConstants.BOTTOM);
                BMETabbed.addTab("Ammo", new CBMPartsPanel(mwclient, BMEquipment.PART_AMMO));
                BMETabbed.addTab("Armor", new CBMPartsPanel(mwclient, BMEquipment.PART_ARMOR));
                BMETabbed.addTab("Weapons", new CBMPartsPanel(mwclient, BMEquipment.PART_WEAPON));
                BMETabbed.addTab("Misc", new CBMPartsPanel(mwclient, BMEquipment.PART_MISC));
                BMESelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BMETabbed);
            }
            tabText = mwclient.getConfig().getParam("BMETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BMEMNEMONIC");
            if (mwclient.getConfig().isParam("BMEINTOPROW")) {
                addPanelMain(BMETabbed,
                      BMSelect,
                      tabText,
                      "Buy and Sell Parts (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMSelect,
                      tabText,
                      "Buy and Sell Parts (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BMESelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("HSTATUSTABVISIBLE")) {
            if (HSPanel == null) {
                HSPanel = new CHSPanel(mwclient);
                HSSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(HSPanel);
            } else if (HSSelect == null) {
                HSSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(HSPanel);
            }
            tabText = mwclient.getConfig().getParam("HSTATUSTABNAME");
            mnemonicText = mwclient.getConfig().getParam("HSTATUSMNEMONIC");
            if (mwclient.getConfig().isParam("HSTATUSINTOPROW")) {
                addPanelMain(HSPanel,
                      HSSelect,
                      tabText,
                      "Show current House Status (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      "Show current House Status (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "HSSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("BATTLETABVISIBLE")) {
            if (BattlePanel == null) {
                BattlePanel = new CBattlePanel(mwclient);
                BattleSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(BattlePanel);
            }
            tabText = mwclient.getConfig().getParam("BATTLETABNAME");
            mnemonicText = mwclient.getConfig().getParam("BATTLEMNEMONIC");
            if (mwclient.getConfig().isParam("BATTLEINTOPROW")) {
                addPanelMain(BattlePanel,
                      BattleSelect,
                      tabText,
                      "Battles Intelligence Data (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      "Battles Intelligence Data (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "BattleSelect",
                      CommPanel);
            }
        }

        if (MapPanel == null) {
            MapPanel = new CMapPanel(mwclient, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());
        }
        if (mwclient.getConfig().isParam("MAPTABVISIBLE")) {
            if (MapSelect == null) {MapSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(MapPanel);}
            tabText = mwclient.getConfig().getParam("MAPTABNAME");
            mnemonicText = mwclient.getConfig().getParam("MAPMNEMONIC");
            if (mwclient.getConfig().isParam("MAPINTOPROW")) {
                addPanelMain(MapPanel,
                      MapSelect,
                      tabText,
                      "Star Map (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      "Star Map (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "MapSelect",
                      CommPanel);
            }
        }

        if (mwclient.getConfig().isParam("RULESTABVISIBLE")) {
            if (RulesPanel == null) {
                RulesPanel = new CRulesPanel(mwclient);
                RulesSelect = new mekwars.common.gui.CMainPanel.CSelectTabAction(RulesPanel);
            }
            tabText = mwclient.getConfig().getParam("RULESTABNAME");
            mnemonicText = mwclient.getConfig().getParam("RULESMNEMONIC");
            if (mwclient.getConfig().isParam("RULESINTOPROW")) {
                addPanelMain(RulesPanel,
                      RulesSelect,
                      tabText,
                      "Rules Tab (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      "Rules Tab (Alt + " + mnemonicText + ")",
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    private class CTabForwardAction extends javax.swing.AbstractAction {

        /**
         *
         */
        private static final long serialVersionUID = -6816947698919825957L;

        public CTabForwardAction() {}

        public void actionPerformed(java.awt.event.ActionEvent e) {

            int count = MainTPane.getTabCount();
            if (count < 2) {return;}
            int index = MainTPane.getSelectedIndex();
            index++;
            if (index == count) {index = 0;}
            while (!MainTPane.isEnabledAt(index)) {
                index++;
                if (index == count) {index = 0;}
            }

            MainTPane.setSelectedIndex(index);
        }
    }

    private class CTabBackwardAction extends javax.swing.AbstractAction {

        /**
         *
         */
        private static final long serialVersionUID = -507793785645622171L;

        public CTabBackwardAction() {}

        public void actionPerformed(java.awt.event.ActionEvent e) {
            int count = MainTPane.getTabCount();
            if (count < 2) {return;}
            int index = MainTPane.getSelectedIndex();
            index--;
            if (index == -1) {index = count - 1;}
            while (!MainTPane.isEnabledAt(index)) {
                index--;
                if (index == -1) {index = count - 1;}
            }

            MainTPane.setSelectedIndex(index);
        }
    }

    // actions

    private class CSelectTabAction extends javax.swing.AbstractAction {

        /**
         *
         */
        private static final long serialVersionUID = -1191343876143323182L;
        java.awt.Component Tab = null;

        public CSelectTabAction(java.awt.Component tab) {Tab = tab;}

        public void actionPerformed(java.awt.event.ActionEvent e) {
            try {
                if (MainTPane.isEnabledAt(MainTPane.indexOfComponent(Tab))) {MainTPane.setSelectedComponent(Tab);}
            } catch (Exception ex) {
                if (CommPanel.CommTPane.isEnabledAt(CommPanel.CommTPane.indexOfComponent(Tab))) {
                    CommPanel.CommTPane.setSelectedComponent(Tab);
                }
            }
        }
    }

}
