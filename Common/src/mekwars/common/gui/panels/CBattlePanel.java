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

package mekwars.common.gui.panels;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import javax.swing.*;

import mekwars.common.MMGame;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.TableSorter;
import mekwars.common.gui.models.BattlesModel;

/**
 * Backs the "Battles" tab of the main client window (added to {@link CMainPanel} when {@code BATTLE_TAB_VISIBLE} is
 * set). Shows a sortable table of every currently-hosted game the player's client knows about — host name, player
 * count, MegaMek version, host comment, and joined player names — sourced from {@link BattlesModel} (via a
 * {@link TableSorter} wrapper for click-to-sort column headers).
 * <p>
 * From this tab a player can double-click a row to join/view that game ({@link IClient#startClient}), or
 * right-click for a context menu offering "View game" / "Join game" (or "Game is full" if applicable), "Stop
 * Hosting" (if the player is the host), and — for servers whose host name starts with {@code "[Dedicated]"} — an
 * extensive set of remote-administration commands (restart, kill, load/save games, change port, manage owners,
 * view logs, etc.) sent to the dedicated server host as chat-borne "mail" commands.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */


public class CBattlePanel extends JPanel {

    /**
     * Swing serialization id. No further documentation was provided for this field by the original author.
     */
    @Serial
    private static final long serialVersionUID = -1556406945897698254L;
    /** Client session, used to look up hosted games and send chat/mail commands to dedicated hosts. */
    private final IClient client;
    /** The visible table of active battles/games. Its model is actually {@link #battleSorter}, not {@link #battleTableModel} directly. */
    private final JTable BattleTable;
    /** The underlying (unsorted) table model listing all known active games; wrapped by {@link #battleSorter} for display. */
    private final BattlesModel battleTableModel;
    /** Scroll pane wrapping {@link #BattleTable}. */
    private final JScrollPane battleScrollPane;
    /** Sortable wrapper around {@link #battleTableModel} that provides click-to-sort column headers. */
    private final TableSorter battleSorter;

    /**
     * Builds the battles table: wraps {@link BattlesModel} in a {@link TableSorter} for sortable columns, sets
     * fixed column widths for host name/player count/version/comment/player names, wires a double-click handler
     * that joins the clicked game (equivalent to the popup menu's "Join game" — {@code startClient(name, true)}),
     * and attaches the right-click context menu via {@link BattlePopupListener}.
     *
     * @param client the active client session used to query hosted games and send commands
     */
    public CBattlePanel(IClient client) {
        this.client = client;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        //make table and set sorted model
        battleTableModel = new BattlesModel(this);
        BattleTable = new JTable();

        battleSorter = new TableSorter(battleTableModel, this.client, TableSorter.SORTER_BATTLES);
        BattleTable.setModel(battleSorter);
        battleSorter.addMouseListenerToHeaderInTable(this.BattleTable);

        BattleTable.setDefaultRenderer(Object.class, battleTableModel.getRenderer());
        BattleTable.addMouseListener(new CBattlePanel.BattlePopupListener());
        //Host name
        BattleTable.getColumnModel().getColumn(0).setMinWidth(10);
        BattleTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        //Player Count
        BattleTable.getColumnModel().getColumn(1).setMinWidth(10);
        BattleTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        //Version
        BattleTable.getColumnModel().getColumn(2).setMinWidth(10);
        BattleTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        BattleTable.getColumnModel().getColumn(2).setMaxWidth(200);
        //Comment
        BattleTable.getColumnModel().getColumn(3).setMinWidth(10);
        BattleTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        //Player Names
        BattleTable.getColumnModel().getColumn(4).setMinWidth(10);
        BattleTable.getColumnModel().getColumn(4).setPreferredWidth(300);

        //Double-clicking a row always attempts to join that game. Unlike the right-click "Join game" menu item,
        //this does not check whether the game is full or whether the player is a moderator first -- the join
        //attempt is simply sent, and any rejection is presumably handled server-side.
        BattleTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) {
                    String curName = (String) battleSorter.getValueAt(BattleTable.rowAtPoint(event.getPoint()),
                          0);//host name
                    CBattlePanel.this.client.startClient(curName, true);
                }
            }
        });

        BattleTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        battleScrollPane = new JScrollPane(BattleTable);
        battleScrollPane.add(BattleTable, null);
        battleScrollPane.setPreferredSize(new Dimension(640, 190));
        battleScrollPane.setBorder(BorderFactory.createLineBorder(Color.black));
        battleScrollPane.getViewport().add(BattleTable, null);
        add(battleScrollPane, BorderLayout.NORTH);
    }

    /**
     * @return Returns the battleScrollPane.
     */
    public javax.swing.JScrollPane getBattleScrollPane() {
        return battleScrollPane;
    }

    /** @return the client session this panel was built with. */
    public IClient getClient() {
        return client;
    }

    /** @return the sortable table model wrapper actually bound to {@link #BattleTable}. */
    public TableSorter getBattleSorter() {
        return battleSorter;
    }

    /**
     * Return the model. Used to refresh.
     */
    public BattlesModel getBattleTableModel() {
        return battleTableModel;
    }

    /**
     * @return Returns the battleTable.
     */
    public javax.swing.JTable getBattleTable() {
        return BattleTable;
    }

    /**
     * Right-click context menu for a row in the battles table. Offers actions appropriate to the clicked game:
     * "View game" / "Join game" (the latter only if the game's status is "Open") when the player can join (game
     * isn't full, or the player is a moderator per {@link IClient#isMod()}) — otherwise a disabled-looking
     * "Game is full" placeholder item; "Stop Hosting" if the current player is the game's host; and, if the host
     * name starts with {@code "[Dedicated]"}, a full "Maintenance" sub-menu of dedicated-server admin commands
     * (restart/reset/kill/start/stop, load/save game management, log viewing, port/owner/comment/name/max-players
     * configuration, auto-restart count, and update-URL management). All admin actions are sent as chat "mail"
     * commands to the dedicated host via {@link #actionPerformed(ActionEvent)}; destructive ones prompt for
     * confirmation first via {@link JOptionPane}.
     */
    class BattlePopupListener extends MouseAdapter implements ActionListener {

        /** Shows the popup on mouse-press if this is the platform's popup-trigger event (e.g. right mouse button). */
        @Override
        public void mousePressed(MouseEvent event) {
            maybeShowPopup(event);
        }

        /** Shows the popup on mouse-release if this is the platform's popup-trigger event (some platforms trigger on release rather than press). */
        @Override
        public void mouseReleased(MouseEvent event) {
            maybeShowPopup(event);
        }

        /**
         * Builds and displays the context menu described in the {@link BattlePopupListener} class comment for the
         * row under the mouse, if the event is a popup trigger. Silently does nothing if the row's host name no
         * longer maps to a known {@link MMGame} (e.g. the game disappeared between hover and click).
         */
        private void maybeShowPopup(MouseEvent event) {

            JPopupMenu popup = new JPopupMenu();
            JMenuItem menuItem;

            if (event.isPopupTrigger()) {

                int currRow = BattleTable.rowAtPoint(event.getPoint());
                String curName = (String) battleSorter.getValueAt(currRow, 0);//host name

                MMGame curGame = client.getServers().get(curName);
                if (curGame == null) {
                    return;
                }

                if (curGame.getCurrentPlayers().size() < curGame.getMaxPlayers() || client.isMod()) {

                    menuItem = new JMenuItem("View game");
                    menuItem.setActionCommand(String.format("V|%s", curName));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    if (curGame.getStatus().equals("Open")) {
                        menuItem = new javax.swing.JMenuItem("Join game");
                        menuItem.setActionCommand(String.format("J|%s", curName));
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                } else {
                    menuItem = new JMenuItem("Game is full");
                    popup.add(menuItem);
                }

                if (curGame.getHostName().equals(client.getUsername())) {
                    menuItem = new JMenuItem("Stop Hosting");
                    menuItem.setActionCommand(String.format("S|%s", curName));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (curGame.getHostName().startsWith("[Dedicated]")) {

                    JMenu serviceMenu = new JMenu("Maintenance");
                    JMenu settingsMenu = new JMenu("Settings");
                    JMenu portMenu = new JMenu("Port");
                    JMenu ownersMenu = new JMenu("Owners");
                    JMenu miscMenu = new JMenu("Misc");
                    JMenu autoRestartMenu = new JMenu("AutoRestart");
                    JMenu updateMenu = new JMenu("Update");

                    popup.addSeparator();
                    menuItem = new JMenuItem("Restart Dedicated");
                    menuItem.setActionCommand(String.format("RESTART|%s", curName));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    menuItem = new JMenuItem("Load Autosave");
                    menuItem.setActionCommand(String.format("LOADAUTOSAVE|%s", curName));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    popup.addSeparator();

                    menuItem = new JMenuItem("Reset Dedicated");
                    menuItem.setActionCommand(String.format("RESET|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Kill Dedicated");
                    menuItem.setActionCommand(String.format("DIE|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Start Dedicated");
                    menuItem.setActionCommand(String.format("START|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Stop Dedicated");
                    menuItem.setActionCommand(String.format("STOP|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Load Game");
                    menuItem.setActionCommand(String.format("LOADGAME|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Saved Games");
                    menuItem.setActionCommand(String.format("DSG|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    JMenu logMenu = new JMenu("Logs");

                    menuItem = new JMenuItem("Display MegaMek Log");
                    menuItem.setActionCommand(String.format("DMML|%s", curName));
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Error Log");
                    menuItem.setActionCommand(String.format("DDEL|%s", curName));
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Log");
                    menuItem.setActionCommand(String.format("DELL|%s", curName));
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    serviceMenu.add(logMenu);

                    menuItem = new JMenuItem("Ping Dedicated");
                    menuItem.setActionCommand(String.format("PING|%s", curName));
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new JMenuItem("Get Update URL");
                    menuItem.setActionCommand(String.format("GETUPDATEURL|%s", curName));
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Set Update URL");
                    menuItem.setActionCommand(String.format("SETUPDATEURL|%s", curName));
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Update Dedicated");
                    menuItem.setActionCommand(String.format("UPDATE|%s", curName));
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Owners");
                    menuItem.setActionCommand(String.format("OWNERS|%s", curName));
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new JMenuItem("Add Dedicated Owners");
                    menuItem.setActionCommand(String.format("ADDOWNERS|%s", curName));
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new JMenuItem("Clear Dedicated Owners");
                    menuItem.setActionCommand(String.format("CLEAROWNERS|%s", curName));
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Port");
                    menuItem.setActionCommand(String.format("GETPORT|%s", curName));
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Port");
                    menuItem.setActionCommand(String.format("SETPORT|%s", curName));
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);

                    menuItem = new JMenuItem("Set Dedicated Name");
                    menuItem.setActionCommand(String.format("SETNAME|%s", curName));
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Comment");
                    menuItem.setActionCommand(String.format("SETCOMMENT|%s", curName));
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Max Players");
                    menuItem.setActionCommand(String.format("SETPLAYERS|%s", curName));
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Saved Games Purge Days");
                    menuItem.setActionCommand(String.format("GSGPD|%s", curName));
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Saved Games Purge Days");
                    menuItem.setActionCommand(String.format("SSGPD|%s", curName));
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Restart Count");
                    menuItem.setActionCommand(String.format("CURRENTRESTART|%s", curName));
                    menuItem.addActionListener(this);
                    autoRestartMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Restart Count");
                    menuItem.setActionCommand(String.format("SETRESTART|%s", curName));
                    menuItem.addActionListener(this);
                    autoRestartMenu.add(menuItem);

                    settingsMenu.add(portMenu);
                    settingsMenu.add(ownersMenu);
                    settingsMenu.add(miscMenu);
                    settingsMenu.add(autoRestartMenu);
                    settingsMenu.add(updateMenu);

                    serviceMenu.add(settingsMenu);

                    popup.add(serviceMenu);
                }
                popup.show(event.getComponent(), event.getX(), event.getY());
            }
        }

        /**
         * Dispatches every context-menu action built in {@link #maybeShowPopup(MouseEvent)}. Each menu item's action
         * command is a {@code "PREFIX|hostName"} string; this method matches the prefix and either calls straight
         * into {@link IClient} ("V"/"J" view/join, "S" stop hosting) or, for the dedicated-server admin commands,
         * prompts the player (via {@link JOptionPane}, a confirmation dialog for destructive actions or an input
         * dialog when a value is needed) and then sends the corresponding {@code /c mail <host>,<command> [args]}
         * chat command to the server, which the dedicated host is expected to interpret. Cancelling any dialog
         * (returning null / not YES_OPTION) aborts that particular action.
         * <p>
         * Note: the "V|" and "J|" checks use independent {@code if} statements rather than {@code else if}, but
         * since the prefixes are mutually exclusive this has no observable effect.
         */
        public void actionPerformed(ActionEvent actionEvent) {
            String actionCommand = actionEvent.getActionCommand();
            if (actionCommand.startsWith("V|")) {client.startClient(actionCommand.substring(2), false);}
            if (actionCommand.startsWith("J|")) {client.startClient(actionCommand.substring(2), true);}
            if (actionCommand.startsWith("S|")) {
                client.getMainFrame().stopHost();
                client.stopHost();
            }

            if (actionCommand.startsWith("RESTART|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to restart
\r%s?""", currName),
                      "Restart?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,restart", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("RESET|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to reset
\r%s?""", currName),
                      "Reset?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,reset", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("DIE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to kill
\r%s?""", currName),
                      "Kill?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,die", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("START|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to start
\r%s?""", currName),
                      "Start?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,start", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("STOP|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to stop
\r%s?""", currName),
                      "Stop?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,stop", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("OWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,owners", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("CLEAROWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to clear the owners of
\r%s?""", currName),
                      "Clear the owners?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,clearowners", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("ADDOWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a list of owners you want to add to
\r%s
\r(sperated by $)""", currName),
                      "Add Owners",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(String.format("%smail %s,owner %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("GETPORT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,port", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("SETPORT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new port for
\r%s""", currName),
                      "New Port",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(String.format("%smail %s,port %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("GSGPD|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,savegamepurge", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("SSGPD|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new day for
\r%s""", currName),
                      "New days out to purge",
                      JOptionPane.OK_CANCEL_OPTION);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(String.format("%smail %s,savegamepurge %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("PING|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,ping", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("UPDATE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,update", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("DSG|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,displaysavedgames", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("DMML|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,displaymegameklog", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("DDEL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,displaydederrorlog", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("DELL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,displaydedlog", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("LOADGAME|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = null;
                result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter name of the save file on
\r%s
\r(leave blank to load autosave.sav)""", currName),
                      "Load Game",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null) {
                    client.sendChat(String.format("%smail %s,loadgame %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("LOADAUTOSAVE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      String.format("""
Are you sure you want to load the autosave game on
\r%s?""", currName),
                      "Load Auto Saved Game?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(String.format("%smail %s,loadautosave", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("SETNAME|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new name for
\r%s
\rNote: This will kill the Ded. A restart will be required.""", currName),
                      "New Name",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(String.format("%smail %s,name %s", IClient.CAMPAIGN_PREFIX, currName, result));
                    client.sendChat(String.format("%smail %s,die", IClient.CAMPAIGN_PREFIX, currName));
                }
            } else if (actionCommand.startsWith("SETCOMMENT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new comment for
\r%s""", currName),
                      "New Comment",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(String.format("%smail %s,comment %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("SETPLAYERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter the max number of players for
\r%s""", currName),
                      "New Players",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(String.format("%smail %s,players %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("CURRENTRESTART|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,restartcount", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("SETRESTART|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new restart count for
\r%s""", currName),
                      "New Restart",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(String.format("%smail %s,restartcount %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            } else if (actionCommand.startsWith("GETUPDATEURL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(String.format("%smail %s,getupdateurl", IClient.CAMPAIGN_PREFIX, currName));
            } else if (actionCommand.startsWith("SETUPDATEURL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("""
Enter a new update url for
\r%s""", currName),
                      "New Update URL",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(String.format("%smail %s,setupdateurl %s", IClient.CAMPAIGN_PREFIX, currName, result));
                }
            }
        }
    }
}
