/*
 * Copyright (C) 2004 Helge Richter (McWizard)
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

package mekwars.common.gui.panels;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.Serial;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import megamek.logging.MMLogger;
import mekwars.common.BMEquipment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;

/**
 * Main panel
 */

public class CMainPanel extends JPanel implements ChangeListener, ComponentListener {
    private static final MMLogger LOGGER = MMLogger.create(CMainPanel.class);

    @Serial
    private static final long serialVersionUID = -7817596095411018999L;
    private final JSplitPane MainSPane;
    private final JSplitPane TabSPane;
    private final JSplitPane SideSPane;
    private final CUserListPanel UserListPanel;
    private final JTabbedPane MainTPane = new JTabbedPane(SwingConstants.BOTTOM);
    private final IClient client;
    private final int panelDivider;
    private final int playerPanelDivider;
    private final int verticalPanelDivider;
    private CPlayerPanel PlayerPanel;
    private CCommPanel CommPanel;
    private CHQPanel HQPanel = null;
    private CBMPanel BMPanel = null;
    private CHSPanel HSPanel = null;
    private CRulesPanel RulesPanel = null; //@salient
    private JTabbedPane BMETabbed = null;
    private CSelectTabAction HQSelect = null;
    private CSelectTabAction RulesSelect = null; //@salient
    private CSelectTabAction BMSelect = null;
    private CSelectTabAction HSSelect = null;
    private CSelectTabAction BMESelect = null;
    private CBattlePanel BattlePanel = null;
    private CSelectTabAction BattleSelect = null;
    private CMapPanel MapPanel = null;
    private CSelectTabAction MapSelect = null;

    public CMainPanel(IClient client, CMainFrame mainFrame) {
        this.client = client;
        setLayout(new java.awt.BorderLayout());
        setMinimumSize(new java.awt.Dimension(620, 400));
        addComponentListener(this);
        createMainTPane(mainFrame);
        PlayerPanel = new CPlayerPanel(this.client);
        UserListPanel = new CUserListPanel(this.client);
        add(PlayerPanel, java.awt.BorderLayout.NORTH);
        panelDivider = this.client.getConfig().getIntParam("PANEL_DIVIDER");
        int sPanelDivider = this.client.getConfig().getIntParam("SPLIT_TER_SIZE");
        playerPanelDivider = this.client.getConfig().getIntParam("PLAYER_PANEL_DIVIDER");
        verticalPanelDivider = this.client.getConfig().getIntParam("VERTICAL_DIVIDER");

        TabSPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, MainTPane, CommPanel);
        TabSPane.setOneTouchExpandable(true);
        TabSPane.setDividerLocation(panelDivider);
        TabSPane.setDividerSize(sPanelDivider);

        SideSPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, PlayerPanel, UserListPanel);
        SideSPane.setOneTouchExpandable(true);
        SideSPane.setDividerLocation(playerPanelDivider);
        SideSPane.setDividerSize(sPanelDivider);
        MainSPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, TabSPane, SideSPane);
        MainSPane.setOneTouchExpandable(true);
        MainSPane.setDividerLocation(verticalPanelDivider);
        MainSPane.setDividerSize(sPanelDivider);
        add(MainSPane, java.awt.BorderLayout.CENTER);

        CTabForwardAction forwardMainTab = new CTabForwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("alt X"), "TabForward");
        getActionMap().put("TabForward", forwardMainTab);

        CTabBackwardAction backwardMainTab = new CTabBackwardAction();
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("shift alt X"), "TabBackward");
        getActionMap().put("TabBackward", backwardMainTab);
        revalidate();
    }

    private void createMainTPane(CMainFrame mainFrame) {
        CommPanel = new CCommPanel(client);

        String tabText = "";
        String mnemonicText = "";

        if (client.getConfig().isParam("HQ_TAB_VISIBLE")) {
            HQPanel = new CHQPanel(client);
            HQSelect = new CMainPanel.CSelectTabAction(HQPanel);
            tabText = client.getConfig().getParam("HQ_TAB_NAME");
            mnemonicText = client.getConfig().getParam("HQ_MNEMONIC");

            if (client.getConfig().isParam("HQ_IN_TOP_ROW")) {
                addPanelMain(HQPanel,
                      HQSelect,
                      tabText,
                      STR."Command Center and Hangars (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      STR."Command Center and Hangars (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HQSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BM_TAB_VISIBLE")) {
            BMPanel = new CBMPanel(client);
            BMSelect = new CMainPanel.CSelectTabAction(BMPanel);
            tabText = client.getConfig().getParam("BM_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BM_MNEMONIC");

            if (client.getConfig().isParam("BM_IN_TOP_ROW")) {
                addPanelMain(BMPanel,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Units (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Units (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BME_TAB_VISIBLE") &&
                  Boolean.parseBoolean(client.getServerConfigs("UsePartsBlackMarket"))) {
            BMETabbed = new JTabbedPane(SwingConstants.BOTTOM);
            BMETabbed.addTab("Ammo", new CBMPartsPanel(client, BMEquipment.PART_AMMO));
            BMETabbed.addTab("Armor", new CBMPartsPanel(client, BMEquipment.PART_ARMOR));
            BMETabbed.addTab("Weapons", new CBMPartsPanel(client, BMEquipment.PART_WEAPON));
            BMETabbed.addTab("Misc", new CBMPartsPanel(client, BMEquipment.PART_MISC));
            BMESelect = new CMainPanel.CSelectTabAction(BMETabbed);
            tabText = client.getConfig().getParam("BME_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BME_MNEMONIC");

            if (client.getConfig().isParam("BME_IN_TOP_ROW")) {
                addPanelMain(BMETabbed,
                      BMESelect,
                      tabText,
                      STR."Buy and Sell Parts (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMESelect,
                      tabText,
                      STR."Buy and Sell Parts (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMESelect",
                      CommPanel);
            }
        }

        HSPanel = new CHSPanel(client);
        if (client.getConfig().isParam("HOUSE_STATUS_TAB_VISIBLE")) {
            HSSelect = new CMainPanel.CSelectTabAction(HSPanel);
            tabText = client.getConfig().getParam("HOUSE_STATUS_TAB_NAME");
            mnemonicText = client.getConfig().getParam("HOUSE_STATUS_MNEMONIC");

            if (client.getConfig().isParam("HOUSE_STATUS_IN_TOP_ROW")) {
                addPanelMain(HSPanel,
                      HSSelect,
                      tabText,
                      STR."Show current House Status (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      STR."Show current House Status (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HSSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BATTLE_TAB_VISIBLE")) {
            BattlePanel = new CBattlePanel(client);
            BattleSelect = new CMainPanel.CSelectTabAction(BattlePanel);
            tabText = client.getConfig().getParam("BATTLE_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BATTLE_MNEMONIC");

            if (client.getConfig().isParam("BATTLE_IN_TOP_ROW")) {
                addPanelMain(BattlePanel,
                      BattleSelect,
                      tabText,
                      STR."Battles Intelligence Data (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      STR."Battles Intelligence Data (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BattleSelect",
                      CommPanel);
            }
        }

        MapPanel = new CMapPanel(client, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());

        if (client.getConfig().isParam("MAP_TAB_VISIBLE")) {
            MapSelect = new CMainPanel.CSelectTabAction(MapPanel);
            tabText = client.getConfig().getParam("MAP_TAB_NAME");
            mnemonicText = client.getConfig().getParam("MAP_MNEMONIC");
            if (client.getConfig().isParam("MAP_IN_TOP_ROW")) {
                addPanelMain(MapPanel,
                      MapSelect,
                      tabText,
                      STR."Star Map (Alt + \{mnemonicText})",
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      STR."Star Map (Alt + \{mnemonicText})",
                      mnemonicText,
                      "MapSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("RULES_TAB_VISIBLE")) {
            RulesPanel = new CRulesPanel(client);
            RulesSelect = new CMainPanel.CSelectTabAction(RulesPanel);
            tabText = client.getConfig().getParam("RULES_TAB_NAME");
            mnemonicText = client.getConfig().getParam("RULES_MNEMONIC");

            if (client.getConfig().isParam("RULES_IN_TOP_ROW")) {
                addPanelMain(RulesPanel,
                      RulesSelect,
                      tabText,
                      STR."Rules Tab (Alt + \{mnemonicText})",
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      STR."Rules Tab (Alt + \{mnemonicText})",
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    private void addPanelMain(
          JPanel panel,
          CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemoStr,
          String commandStr) {
        MainTPane.addTab(name, null, panel, tooltip);
        int index = MainTPane.indexOfComponent(panel);
        int mnemo = MainTPane.getTitleAt(index).indexOf(mnemoStr.toUpperCase());

        if (mnemo == -1) {
            mnemo = MainTPane.getTitleAt(index).indexOf(mnemoStr.toLowerCase());
        }

        MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemoStr}"), commandStr);
        getActionMap().put(commandStr, select);
    }

    private void addPanelCComm(
          JPanel panel,
          CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemoStr,
          String commandStr,
          CCommPanel CommPanel) {
        CommPanel.CommTPane.addTab(name, null, panel, tooltip);
        int index = CommPanel.CommTPane.indexOfComponent(panel);
        int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemoStr.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemoStr.toLowerCase());
        }

        CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW)
              .put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemoStr}"), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    //Why is this defined twice? - salient
    private void addPanelMain(
          JTabbedPane panel,
          CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemoStr,
          String commandStr) {
        MainTPane.addTab(name, null, panel, tooltip);
        int index = MainTPane.indexOfComponent(panel);
        int mnemo = MainTPane.getTitleAt(index).indexOf(mnemoStr.toUpperCase());

        if (mnemo == -1) {
            mnemo = MainTPane.getTitleAt(index).indexOf(mnemoStr.toLowerCase());
        }

        MainTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(STR."alt \{mnemoStr}"), commandStr);
        getActionMap().put(commandStr, select);
    }

    private void addPanelCComm(JTabbedPane panel,
          CMainPanel.CSelectTabAction select,
          String name,
          String tooltip,
          String mnemoStr,
          String commandStr,
          CCommPanel CommPanel) {
        CommPanel.CommTPane.addTab(name, null, panel, tooltip);
        int index = CommPanel.CommTPane.indexOfComponent(panel);
        int mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemoStr.toUpperCase());

        if (mnemo == -1) {
            mnemo = CommPanel.CommTPane.getTitleAt(index).indexOf(mnemoStr.toLowerCase());
        }

        CommPanel.CommTPane.setDisplayedMnemonicIndexAt(index, mnemo);
        CommPanel.CommTPane.getInputMap(WHEN_IN_FOCUSED_WINDOW)
              .put(javax.swing.KeyStroke.getKeyStroke(STR."alt \{mnemoStr}"), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    public void changeStatus(int status, int lastStatus) {
        if (status == IClient.STATUS_RESERVE) {
            if (lastStatus == IClient.STATUS_LOGGED_OUT) {
                UserListPanel.setLoggedIn(true);
                UserListPanel.getUsers().getRenderer().setLoggedIn(true);

                if (client.getConfig().isParam("PLAYER_PANEL")) {
                    PlayerPanel.setVisible(true);
                }

                TabSPane.setDividerLocation(panelDivider);
                SideSPane.setDividerLocation(playerPanelDivider);
            }
        }

        if (status == IClient.STATUS_DISCONNECTED || status == IClient.STATUS_LOGGED_OUT) {
            UserListPanel.setLoggedIn(false);
            UserListPanel.getUsers().getRenderer().setLoggedIn(false);
            PlayerPanel.setVisible(false);
        }

        MainTPane.setVisible(status != IClient.STATUS_DISCONNECTED);
    }

    /**
     * A method which selects the FIRST tab, whatever it may be.
     */
    public void selectFirstTab() {
        try {
            MainTPane.setSelectedIndex(0);
        } catch (Exception ignored) {
            LOGGER.info("Select First Tab Ignored as does not exist.");
        }
    }

    /**
     * A method which selects the map tab, whether it is in the main or comm panel, and sends it to the front.
     * <p>
     * Used by MMNETHyperLinkListener if MAPTABONCLICK is set.
     */
    public void selectMapTab() {

        //get the map name
        String nameToFind = client.getConfigParam("MAP_TAB_NAME");

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

    public CPlayerPanel getPlayerPanel() {
        return PlayerPanel;
    }

    public void setPlayerPanel(CPlayerPanel panel) {
        PlayerPanel = panel;
    }

    public CMapPanel getMapPanel() {
        return MapPanel;
    }

    public JSplitPane getTabSPane() {
        return TabSPane;
    }

    public JSplitPane getMainSPane() {
        return MainSPane;
    }

    public JSplitPane getSideSPane() {
        return SideSPane;
    }

    public CUserListPanel getUserListPanel() {
        return UserListPanel;
    }

    public CCommPanel getCommPanel() {
        return CommPanel;
    }

    public CHQPanel getHQPanel() {
        return HQPanel;
    }

    public CBMPanel getBMPanel() {
        return BMPanel;
    }

    public CHSPanel getHSPanel() {
        return HSPanel;
    }

    public void refreshBME() {
        if (BMETabbed == null) {
            return;
        }

        try {
            ((CBMPartsPanel) BMETabbed.getComponentAt(0)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(1)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(2)).refresh();
            ((CBMPartsPanel) BMETabbed.getComponentAt(3)).refresh();
        } catch (Exception ex) {
            LOGGER.error(ex, "Error refreshing components: {}", ex.getLocalizedMessage());
        }
    }

    public JTable getBattleTable() {
        return BattlePanel.getBattleTable();
    }

    public void refreshBattleTable() {
        BattlePanel.getBattleTableModel().refreshModel();
    }


    // change listener
    public void stateChanged(ChangeEvent changeEvent) {
        if (MainTPane.getSelectedIndex() == -1) {
            return;
        }

        if (MainTPane.getSelectedComponent() == CommPanel) {
            CommPanel.getInputField().requestFocusInWindow();
        }
    }

    public void componentResized(ComponentEvent componentEvent) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    public void componentMoved(ComponentEvent componentEvent) {}

    public void componentShown(ComponentEvent componentEvent) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    // component listener
    public void componentHidden(ComponentEvent componentEvent) {}

    // component listener
    // actions

    public void recreateMainTPane(CMainFrame mainFrame) {
        MainTPane.removeAll();
        CommPanel.CommTPane.removeAll();
        CommPanel.reload();

        String tabText = "";
        String mnemonicText = "";

        if (client.getConfig().isParam("HQ_TAB_VISIBLE")) {
            if (HQPanel == null) {
                HQPanel = new CHQPanel(client);
                HQSelect = new CMainPanel.CSelectTabAction(HQPanel);
            }

            tabText = client.getConfig().getParam("HQ_TAB_NAME");
            mnemonicText = client.getConfig().getParam("HQ_MNEMONIC");

            if (client.getConfig().isParam("HQ_IN_TOP_ROW")) {
                addPanelMain(HQPanel,
                      HQSelect,
                      tabText,
                      STR."Command Center and Hangars (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      STR."Command Center and Hangars (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HQSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BM_TAB_VISIBLE")) {
            if (BMPanel == null) {
                BMPanel = new CBMPanel(client);
                BMSelect = new CMainPanel.CSelectTabAction(BMPanel);
            }

            tabText = client.getConfig().getParam("BM_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BM_MNEMONIC");

            if (client.getConfig().isParam("BM_IN_TOP_ROW")) {
                addPanelMain(BMPanel,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Units (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Units (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BME_TAB_VISIBLE") &&
                  Boolean.parseBoolean(client.getServerConfigs("UsePartsBlackMarket"))) {
            if (BMETabbed == null) {
                BMETabbed = new JTabbedPane(SwingConstants.BOTTOM);
                BMETabbed.addTab("Ammo", new CBMPartsPanel(client, BMEquipment.PART_AMMO));
                BMETabbed.addTab("Armor", new CBMPartsPanel(client, BMEquipment.PART_ARMOR));
                BMETabbed.addTab("Weapons", new CBMPartsPanel(client, BMEquipment.PART_WEAPON));
                BMETabbed.addTab("Misc", new CBMPartsPanel(client, BMEquipment.PART_MISC));
                BMESelect = new CMainPanel.CSelectTabAction(BMETabbed);
            }

            tabText = client.getConfig().getParam("BME_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BME_MNEMONIC");

            if (client.getConfig().isParam("BME_IN_TOP_ROW")) {
                addPanelMain(BMETabbed,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Parts (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMSelect,
                      tabText,
                      STR."Buy and Sell Parts (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BMESelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("HOUSE_STATUS_TAB_VISIBLE")) {
            if (HSPanel == null) {
                HSPanel = new CHSPanel(client);
                HSSelect = new CMainPanel.CSelectTabAction(HSPanel);
            } else if (HSSelect == null) {
                HSSelect = new CMainPanel.CSelectTabAction(HSPanel);
            }

            tabText = client.getConfig().getParam("HOUSE_STATUS_TAB_NAME");
            mnemonicText = client.getConfig().getParam("HOUSE_STATUS_MNEMONIC");

            if (client.getConfig().isParam("HOUSE_STATUS_IN_TOP_ROW")) {
                addPanelMain(HSPanel,
                      HSSelect,
                      tabText,
                      STR."Show current House Status (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      STR."Show current House Status (Alt + \{mnemonicText})",
                      mnemonicText,
                      "HSSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("BATTLE_TAB_VISIBLE")) {
            if (BattlePanel == null) {
                BattlePanel = new CBattlePanel(client);
                BattleSelect = new CMainPanel.CSelectTabAction(BattlePanel);
            }

            tabText = client.getConfig().getParam("BATTLE_TAB_NAME");
            mnemonicText = client.getConfig().getParam("BATTLE_MNEMONIC");

            if (client.getConfig().isParam("BATTLE_IN_TOP_ROW")) {
                addPanelMain(BattlePanel,
                      BattleSelect,
                      tabText,
                      STR."Battles Intelligence Data (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      STR."Battles Intelligence Data (Alt + \{mnemonicText})",
                      mnemonicText,
                      "BattleSelect",
                      CommPanel);
            }
        }

        if (MapPanel == null) {
            MapPanel = new CMapPanel(client, mainFrame, CommPanel.getWidth(), CommPanel.getHeight());
        }

        if (client.getConfig().isParam("MAP_TAB_VISIBLE")) {
            if (MapSelect == null) {
                MapSelect = new CMainPanel.CSelectTabAction(MapPanel);
            }

            tabText = client.getConfig().getParam("MAP_TAB_NAME");
            mnemonicText = client.getConfig().getParam("MAP_MNEMONIC");

            if (client.getConfig().isParam("MAP_IN_TOP_ROW")) {
                addPanelMain(MapPanel,
                      MapSelect,
                      tabText,
                      STR."Star Map (Alt + \{mnemonicText})",
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      STR."Star Map (Alt + \{mnemonicText})",
                      mnemonicText,
                      "MapSelect",
                      CommPanel);
            }
        }

        if (client.getConfig().isParam("RULES_TAB_VISIBLE")) {
            if (RulesPanel == null) {
                RulesPanel = new CRulesPanel(client);
                RulesSelect = new CMainPanel.CSelectTabAction(RulesPanel);
            }

            tabText = client.getConfig().getParam("RULES_TAB_NAME");
            mnemonicText = client.getConfig().getParam("RULES_MNEMONIC");

            if (client.getConfig().isParam("RULES_IN_TOP_ROW")) {
                addPanelMain(RulesPanel,
                      RulesSelect,
                      tabText,
                      STR."Rules Tab (Alt + \{mnemonicText})",
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      STR."Rules Tab (Alt + \{mnemonicText})",
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    private class CTabForwardAction extends AbstractAction {

        @Serial
        private static final long serialVersionUID = -6816947698919825957L;

        public CTabForwardAction() {}

        public void actionPerformed(ActionEvent actionEvent) {
            int count = MainTPane.getTabCount();

            if (count < 2) {
                return;
            }

            int index = MainTPane.getSelectedIndex();

            do {
                index++;

                if (index == count) {
                    index = 0;
                }
            } while (!MainTPane.isEnabledAt(index));

            MainTPane.setSelectedIndex(index);
        }
    }

    private class CTabBackwardAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -507793785645622171L;

        public CTabBackwardAction() {}

        public void actionPerformed(ActionEvent actionEvent) {
            int count = MainTPane.getTabCount();

            if (count < 2) {
                return;
            }

            int index = MainTPane.getSelectedIndex();

            do {
                index--;
                if (index == -1) {index = count - 1;}
            } while (!MainTPane.isEnabledAt(index));

            MainTPane.setSelectedIndex(index);
        }
    }

    // actions

    private class CSelectTabAction extends AbstractAction {
        @Serial
        private static final long serialVersionUID = -1191343876143323182L;
        Component Tab;

        public CSelectTabAction(Component tab) {
            Tab = tab;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            try {
                if (MainTPane.isEnabledAt(MainTPane.indexOfComponent(Tab))) {
                    MainTPane.setSelectedComponent(Tab);
                }
            } catch (Exception ex) {
                if (CommPanel.CommTPane.isEnabledAt(CommPanel.CommTPane.indexOfComponent(Tab))) {
                    CommPanel.CommTPane.setSelectedComponent(Tab);
                }
            }
        }
    }

}
