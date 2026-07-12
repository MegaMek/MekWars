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
 * The top-level container panel for the MekWars client's main game window.
 * <p>
 * This is the panel that {@code CMainFrame} places in its content pane once the player is connected. It lays out the
 * whole play screen using nested {@link JSplitPane}s: a horizontal split between the tabbed content area / chat
 * (left, {@link #TabSPane}) and the side column (right, {@link #SideSPane}) containing the {@link CPlayerPanel}
 * (player summary) above the {@link CUserListPanel} (connected users list). The tabbed content area itself is a
 * vertical split between {@link #MainTPane} (the main tab strip: HQ, Black Market, House Status, Battles, Map,
 * Rules) and the {@link CCommPanel} chat window below it.
 * <p>
 * Which optional tabs exist, and whether each one lives in the top {@link #MainTPane} row or is nested inside the
 * chat panel's own tab strip ({@code CommPanel.CommTPane}), is entirely driven by client configuration flags (e.g.
 * {@code HQ_TAB_VISIBLE}, {@code HQ_IN_TOP_ROW}) read from {@code client.getConfig()}. This class also restores
 * split-pane divider positions (also config-driven) whenever the window is resized or shown, and reacts to
 * login/logout/reserve status changes by showing or hiding the player/user-list panels.
 */

public class CMainPanel extends JPanel implements ChangeListener, ComponentListener {
    private static final MMLogger LOGGER = MMLogger.create(CMainPanel.class);

    @Serial
    private static final long serialVersionUID = -7817596095411018999L;
    /** Horizontal split: {@link #TabSPane} (tabs + chat) on the left, {@link #SideSPane} (player/user list) on the right. */
    private final JSplitPane MainSPane;
    /** Vertical split: {@link #MainTPane} (main content tabs) on top, {@link #CommPanel} (chat) below. */
    private final JSplitPane TabSPane;
    /** Vertical split: {@link #PlayerPanel} on top, {@link #UserListPanel} below. */
    private final JSplitPane SideSPane;
    /** The connected-players list shown in the lower half of {@link #SideSPane}; created once and never rebuilt. */
    private final CUserListPanel UserListPanel;
    /** Bottom-tabbed pane holding the main content tabs (HQ, Black Market, House Status, Battles, Map, Rules). */
    private final JTabbedPane MainTPane = new JTabbedPane(SwingConstants.BOTTOM);
    /** Client connection/session used for config lookups and sending commands to the server. */
    private final IClient client;
    /** Configured pixel location for the {@link #TabSPane} divider (between main tabs and chat). */
    private final int panelDivider;
    /** Configured pixel location for the {@link #SideSPane} divider (between player panel and user list). */
    private final int playerPanelDivider;
    /** Configured pixel location for the {@link #MainSPane} divider (between tab area and side column). */
    private final int verticalPanelDivider;
    /** Player summary panel at the top of the window (also placed at the top of {@link #SideSPane}). */
    private CPlayerPanel PlayerPanel;
    /** Chat/communications panel; also hosts its own nested tab strip ({@code CommTPane}) for tabs configured to live there instead of {@link #MainTPane}. */
    private CCommPanel CommPanel;
    /** HQ (Command Center / Hangars) tab; {@code null} unless {@code HQ_TAB_VISIBLE} is set. */
    private CHQPanel HQPanel = null;
    /** Black Market (unit buy/sell) tab; {@code null} unless {@code BM_TAB_VISIBLE} is set. */
    private CBMPanel BMPanel = null;
    /** House Status tab; always constructed, but only added to a tab strip when {@code HOUSE_STATUS_TAB_VISIBLE} is set. */
    private CHSPanel HSPanel = null;
    /** Rules tab; {@code null} unless {@code RULES_TAB_VISIBLE} is set. */
    private CRulesPanel RulesPanel = null; //@salient
    /** Sub-tabbed pane for the Black Market Equipment (parts) sub-tabs: Ammo/Armor/Weapons/Misc; {@code null} unless the BME tab and server-side parts market are both enabled. */
    private JTabbedPane BMETabbed = null;
    /** Action bound to the HQ tab's mnemonic keystroke; selects {@link #HQPanel}. */
    private CSelectTabAction HQSelect = null;
    /** Action bound to the Rules tab's mnemonic keystroke; selects {@link #RulesPanel}. */
    private CSelectTabAction RulesSelect = null; //@salient
    /** Action bound to the Black Market tab's mnemonic keystroke; selects {@link #BMPanel}. */
    private CSelectTabAction BMSelect = null;
    /** Action bound to the House Status tab's mnemonic keystroke; selects {@link #HSPanel}. */
    private CSelectTabAction HSSelect = null;
    /** Action bound to the BME tab's mnemonic keystroke; selects {@link #BMETabbed}. */
    private CSelectTabAction BMESelect = null;
    /** Battles/active-games list tab; {@code null} unless {@code BATTLE_TAB_VISIBLE} is set. */
    private CBattlePanel BattlePanel = null;
    /** Action bound to the Battles tab's mnemonic keystroke; selects {@link #BattlePanel}. */
    private CSelectTabAction BattleSelect = null;
    /** Star map tab; always constructed (used elsewhere even when not shown as a tab). */
    private CMapPanel MapPanel = null;
    /** Action bound to the Map tab's mnemonic keystroke; selects {@link #MapPanel}. */
    private CSelectTabAction MapSelect = null;

    /**
     * Builds the whole main-window layout: creates the player panel and user list, builds the main tab strip via
     * {@link #createMainTPane(CMainFrame)}, wires up the three nested split panes with divider positions read from
     * client config, and installs global keyboard shortcuts (Alt+X / Shift+Alt+X) for cycling forward/backward
     * through the main tabs.
     *
     * @param client    the active client session, used for config lookups and to build child panels
     * @param mainFrame the owning top-level frame, passed through to {@link #createMainTPane(CMainFrame)}
     */
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

    /**
     * Builds the chat panel and every optional main-content tab (HQ, Black Market, Black Market Equipment/parts,
     * House Status, Battles, Map, Rules), reading a matching {@code *_TAB_VISIBLE} config flag before creating each
     * one. For each visible tab, a further {@code *_IN_TOP_ROW} flag decides whether the tab is added to
     * {@link #MainTPane} (the main row, via {@link #addPanelMain}) or nested inside the chat panel's own tab strip
     * (via {@link #addPanelCComm}). The House Status panel and the Map panel are always constructed regardless of
     * their visibility flag (only whether they get an actual tab is conditional). Finally registers this panel as
     * the {@link ChangeListener} for {@link #MainTPane}.
     *
     * @param mainFrame the owning frame, needed to construct the {@link CMapPanel}
     */
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
                      String.format("Command Center and Hangars (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      String.format("Command Center and Hangars (Alt + %s)", mnemonicText),
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
                      String.format("Buy and Sell Units (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      String.format("Buy and Sell Units (Alt + %s)", mnemonicText),
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
                      String.format("Buy and Sell Parts (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMESelect,
                      tabText,
                      String.format("Buy and Sell Parts (Alt + %s)", mnemonicText),
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
                      String.format("Show current House Status (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      String.format("Show current House Status (Alt + %s)", mnemonicText),
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
                      String.format("Battles Intelligence Data (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      String.format("Battles Intelligence Data (Alt + %s)", mnemonicText),
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
                      String.format("Star Map (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      String.format("Star Map (Alt + %s)", mnemonicText),
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
                      String.format("Rules Tab (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      String.format("Rules Tab (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    /**
     * Adds a {@link JPanel} tab to the top {@link #MainTPane} tab strip, gives it a keyboard mnemonic, and binds
     * Alt+{@code mnemoStr} to an action that selects it. The mnemonic character is located inside the visible tab
     * title by searching for the upper-case form of {@code mnemoStr} first, then falling back to the lower-case
     * form; if neither is found, {@code setDisplayedMnemonicIndexAt} is called with {@code -1} (no visible
     * underline, but the keystroke binding below still works).
     *
     * @param panel      the panel to add as a new tab
     * @param select     the action to invoke (selects {@code panel}) when the mnemonic keystroke fires
     * @param name       the tab's visible title
     * @param tooltip    the tab's tooltip text
     * @param mnemoStr   the single-letter mnemonic to underline in the title and bind as Alt+letter
     * @param commandStr the key used to register {@code select} in this panel's action map
     */
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
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemoStr)), commandStr);
        getActionMap().put(commandStr, select);
    }

    /**
     * Same as {@link #addPanelMain(JPanel, CSelectTabAction, String, String, String, String)}, but adds the tab to
     * {@code CommPanel}'s own nested tab strip ({@code CommTPane}) instead of {@link #MainTPane}, and binds the
     * mnemonic keystroke on that nested tab strip's input map rather than this panel's. Used for tabs configured
     * (via {@code *_IN_TOP_ROW=false}) to live alongside chat rather than in the main row.
     *
     * @param panel      the panel to add as a new tab
     * @param select     the action to invoke (selects {@code panel}) when the mnemonic keystroke fires
     * @param name       the tab's visible title
     * @param tooltip    the tab's tooltip text
     * @param mnemoStr   the single-letter mnemonic to underline in the title and bind as Alt+letter
     * @param commandStr the key used to register {@code select} in the comm tab strip's action map
     * @param CommPanel  the chat panel whose nested {@code CommTPane} receives the tab
     */
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
              .put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemoStr)), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    /**
     * Overload of {@link #addPanelMain(JPanel, CSelectTabAction, String, String, String, String)} accepting a
     * {@link JTabbedPane} instead of a {@link JPanel}, needed because {@link #BMETabbed} (the BME parts sub-tabs)
     * is itself a {@code JTabbedPane} rather than a plain panel. Behaves identically otherwise.
     */
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
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(String.format("alt %s", mnemoStr)), commandStr);
        getActionMap().put(commandStr, select);
    }

    /**
     * Overload of {@link #addPanelCComm(JPanel, CSelectTabAction, String, String, String, String, CCommPanel)}
     * accepting a {@link JTabbedPane}, for the same reason as the {@code addPanelMain} overload above
     * ({@link #BMETabbed} is a {@code JTabbedPane}). Behaves identically otherwise.
     */
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
              .put(javax.swing.KeyStroke.getKeyStroke(String.format("alt %s", mnemoStr)), commandStr);
        CommPanel.CommTPane.getActionMap().put(commandStr, select);
    }

    /**
     * Reacts to a client login-status transition (called by the client/session logic, not a Swing listener).
     * <p>
     * When freshly transitioning into "reserve" status from a logged-out state, shows the player panel (if
     * configured) and restores the {@link #TabSPane}/{@link #SideSPane} divider positions. When transitioning to
     * disconnected or logged-out, hides the player panel and marks the user list as logged out. In all cases,
     * {@link #MainTPane} is hidden entirely while disconnected.
     *
     * @param status     the new client status ({@code IClient.STATUS_*})
     * @param lastStatus the previous client status, used to detect the logged-out to reserve transition
     */
    public void changeStatus(int status, int lastStatus) {
        if (status == IClient.STATUS_RESERVE) {
            if (lastStatus == IClient.STATUS_LOGGED_OUT) {
                UserListPanel.setLoggedIn(true);
                UserListPanel.getcUserListModel().getRenderer().setLoggedIn(true);

                if (client.getConfig().isParam("PLAYER_PANEL")) {
                    PlayerPanel.setVisible(true);
                }

                TabSPane.setDividerLocation(panelDivider);
                SideSPane.setDividerLocation(playerPanelDivider);
            }
        }

        if (status == IClient.STATUS_DISCONNECTED || status == IClient.STATUS_LOGGED_OUT) {
            UserListPanel.setLoggedIn(false);
            UserListPanel.getcUserListModel().getRenderer().setLoggedIn(false);
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

    /** @return the player summary panel. */
    public CPlayerPanel getPlayerPanel() {
        return PlayerPanel;
    }

    /** Replaces the player summary panel reference. Note: does not re-add it to {@link #SideSPane}'s layout. */
    public void setPlayerPanel(CPlayerPanel panel) {
        PlayerPanel = panel;
    }

    /** @return the star map panel (always non-null once the constructor has run). */
    public CMapPanel getMapPanel() {
        return MapPanel;
    }

    /** @return the vertical split pane between {@link #MainTPane} and {@link #CommPanel}. */
    public JSplitPane getTabSPane() {
        return TabSPane;
    }

    /** @return the outer horizontal split pane between the tab area and the side column. */
    public JSplitPane getMainSPane() {
        return MainSPane;
    }

    /** @return the vertical split pane between {@link #PlayerPanel} and {@link #UserListPanel}. */
    public JSplitPane getSideSPane() {
        return SideSPane;
    }

    /** @return the connected-users list panel. */
    public CUserListPanel getUserListPanel() {
        return UserListPanel;
    }

    /** @return the chat/communications panel. */
    public CCommPanel getCommPanel() {
        return CommPanel;
    }

    /** @return the HQ tab panel, or {@code null} if {@code HQ_TAB_VISIBLE} is disabled. */
    public CHQPanel getHQPanel() {
        return HQPanel;
    }

    /** @return the Black Market tab panel, or {@code null} if {@code BM_TAB_VISIBLE} is disabled. */
    public CBMPanel getBMPanel() {
        return BMPanel;
    }

    /** @return the House Status tab panel (always constructed, regardless of tab visibility). */
    public CHSPanel getHSPanel() {
        return HSPanel;
    }

    /**
     * Refreshes each of the four Black Market Equipment (parts) sub-tabs: Ammo, Armor, Weapons, Misc, in that
     * fixed index order. Does nothing if the BME tab was never created (server parts market or config flag
     * disabled). Any exception while refreshing (e.g. an unexpected component at one of the four indices) is
     * caught and logged rather than propagated.
     */
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

    /**
     * @return the battles/active-games table from {@link #BattlePanel}.
     * @throws NullPointerException if {@code BATTLE_TAB_VISIBLE} is disabled, since {@link #BattlePanel} is then
     *                               never constructed and remains {@code null}. Unlike {@link #refreshBME()}, there
     *                               is no null-guard here.
     */
    public JTable getBattleTable() {
        return BattlePanel.getBattleTable();
    }

    /**
     * Refreshes the battles table's underlying model so it picks up newly reported games.
     * <p>
     * Same caveat as {@link #getBattleTable()}: throws a {@link NullPointerException} if the Battles tab was never
     * created because {@code BATTLE_TAB_VISIBLE} is disabled.
     */
    public void refreshBattleTable() {
        BattlePanel.getBattleTableModel().refreshModel();
    }


    /**
     * {@link ChangeListener} callback fired whenever the selected tab in {@link #MainTPane} changes. When the
     * selection lands on the chat panel, moves keyboard focus to the chat input field so the player can start
     * typing immediately.
     */
    // change listener
    public void stateChanged(ChangeEvent changeEvent) {
        if (MainTPane.getSelectedIndex() == -1) {
            return;
        }

        if (MainTPane.getSelectedComponent() == CommPanel) {
            CommPanel.getInputField().requestFocusInWindow();
        }
    }

    /**
     * {@link ComponentListener} callback: re-applies the three config-defined divider locations whenever this
     * panel is resized, since Swing split panes otherwise tend to keep a stale/relative divider position that no
     * longer matches the configured layout after a resize.
     */
    public void componentResized(ComponentEvent componentEvent) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    /** {@link ComponentListener} callback; intentionally a no-op (move events don't affect layout here). */
    public void componentMoved(ComponentEvent componentEvent) {}

    /**
     * {@link ComponentListener} callback: re-applies the three config-defined divider locations when this panel is
     * shown (e.g. after being hidden while disconnected), for the same reason as {@link #componentResized}.
     */
    public void componentShown(ComponentEvent componentEvent) {
        TabSPane.setDividerLocation(panelDivider);
        SideSPane.setDividerLocation(playerPanelDivider);
        MainSPane.setDividerLocation(verticalPanelDivider);
    }

    /** {@link ComponentListener} callback; intentionally a no-op (nothing needs to react to being hidden). */
    // component listener
    public void componentHidden(ComponentEvent componentEvent) {}

    // component listener
    // actions

    /**
     * Tears down and rebuilds the main/comm tab structure from scratch, re-reading current config flags — used
     * when the client needs to reflect a configuration change (e.g. server settings changed after reconnect)
     * without recreating the whole {@code CMainPanel}. Removes every tab from both {@link #MainTPane} and
     * {@code CommPanel.CommTPane}, reloads the chat panel via {@link CCommPanel#reload()}, then re-adds each
     * optional tab. Unlike {@link #createMainTPane(CMainFrame)}, each sub-panel is only constructed if its field is
     * still {@code null} — panels already built earlier are reused rather than recreated.
     * <p>
     * <b>Known bug:</b> in the Black Market Equipment (BME) block below, both the {@code addPanelMain} and
     * {@code addPanelCComm} calls pass {@code BMSelect} (the Black Market unit-tab action) instead of
     * {@code BMESelect} (the BME/parts-tab action) as the action parameter. As a result, the BME tab's mnemonic
     * keystroke selects the Black Market unit tab instead of the BME parts tab. This mirrors real behavior and is
     * left unchanged per the documentation-only scope of this pass.
     *
     * @param mainFrame the owning frame, needed if the {@link CMapPanel} has to be (re)constructed
     */
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
                      String.format("Command Center and Hangars (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "HQSelect");
            } else {
                addPanelCComm(HQPanel,
                      HQSelect,
                      tabText,
                      String.format("Command Center and Hangars (Alt + %s)", mnemonicText),
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
                      String.format("Buy and Sell Units (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BMSelect");
            } else {
                addPanelCComm(BMPanel,
                      BMSelect,
                      tabText,
                      String.format("Buy and Sell Units (Alt + %s)", mnemonicText),
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
                      String.format("Buy and Sell Parts (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BMESelect");
            } else {
                addPanelCComm(BMETabbed,
                      BMSelect,
                      tabText,
                      String.format("Buy and Sell Parts (Alt + %s)", mnemonicText),
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
                      String.format("Show current House Status (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "HSSelect");
            } else {
                addPanelCComm(HSPanel,
                      HSSelect,
                      tabText,
                      String.format("Show current House Status (Alt + %s)", mnemonicText),
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
                      String.format("Battles Intelligence Data (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "BattleSelect");
            } else {
                addPanelCComm(BattlePanel,
                      BattleSelect,
                      tabText,
                      String.format("Battles Intelligence Data (Alt + %s)", mnemonicText),
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
                      String.format("Star Map (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "MapSelect");
            } else {
                addPanelCComm(MapPanel,
                      MapSelect,
                      tabText,
                      String.format("Star Map (Alt + %s)", mnemonicText),
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
                      String.format("Rules Tab (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "RulesSelect");
            } else {
                addPanelCComm(RulesPanel,
                      RulesSelect,
                      tabText,
                      String.format("Rules Tab (Alt + %s)", mnemonicText),
                      mnemonicText,
                      "RulesSelect",
                      CommPanel);
            }
        }

        MainTPane.addChangeListener(this);
    }

    /**
     * Swing {@link AbstractAction} bound to Alt+X in the constructor; moves the {@link #MainTPane} selection to the
     * next enabled tab, wrapping around to index 0 past the last tab. Does nothing if there are fewer than 2 tabs
     * (nothing to cycle to). Skips disabled tabs by looping until an enabled one is found; if every tab were
     * somehow disabled this would loop forever, but in practice at least the current tab is always enabled.
     */
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

    /**
     * Swing {@link AbstractAction} bound to Shift+Alt+X in the constructor; the mirror image of
     * {@link CTabForwardAction}, moving the {@link #MainTPane} selection to the previous enabled tab and wrapping
     * around to the last tab before index 0.
     */
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

    /**
     * Swing {@link AbstractAction} that selects one specific tab {@link Component} when invoked. One instance is
     * created per optional tab (see {@link #HQSelect}, {@link #BMSelect}, etc.) and bound to that tab's Alt+mnemonic
     * keystroke by {@link #addPanelMain} / {@link #addPanelCComm}.
     * <p>
     * Since a tab may live in either {@link #MainTPane} or {@code CommPanel.CommTPane} depending on config, this
     * action first tries to select the tab in {@link #MainTPane}. If the tab isn't there,
     * {@code MainTPane.indexOfComponent(Tab)} returns {@code -1} and the subsequent {@code isEnabledAt(-1)} call
     * throws, which is caught so the same lookup can be retried against {@code CommPanel.CommTPane} instead. This
     * exception-driven "not found" check is a deliberate (if unusual) way to support the tab living in either
     * location without tracking which one it was placed in.
     */
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
