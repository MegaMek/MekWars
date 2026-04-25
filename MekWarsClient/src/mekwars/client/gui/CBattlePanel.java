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

package mekwars.client.gui;

import common.MMGame;

/**
 * The panel where all currently active battles are shown
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */


public class CBattlePanel extends javax.swing.JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -1556406945897698254L;
    private final client.MWClient mwclient;
    private final javax.swing.JTable BattleTable;
    private final mekwars.client.gui.CBattlePanel.BattlesModel battleTableModel;
    private final javax.swing.JScrollPane battleScrollPane;
    private final TableSorter battleSorter;

    /**
     * Construct a new battle panel
     */
    public CBattlePanel(client.MWClient client) {
        this.mwclient = client;
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        //make table and set sorted model
        battleTableModel = new mekwars.client.gui.CBattlePanel.BattlesModel();
        BattleTable = new javax.swing.JTable();

        battleSorter = new TableSorter(battleTableModel, mwclient, TableSorter.SORTER_BATTLES);
        BattleTable.setModel(battleSorter);
        battleSorter.addMouseListenerToHeaderInTable(this.BattleTable);

        BattleTable.setDefaultRenderer(Object.class, battleTableModel.getRenderer());
        BattleTable.addMouseListener(new mekwars.client.gui.CBattlePanel.BattlePopupListener());
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
        BattleTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String curName = (String) battleSorter.getValueAt(BattleTable.rowAtPoint(e.getPoint()),
                          0);//host name
                    mwclient.startClient(curName, true);
                }
            }
        });
        BattleTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        battleScrollPane = new javax.swing.JScrollPane(BattleTable);
        battleScrollPane.add(BattleTable, null);
        battleScrollPane.setPreferredSize(new java.awt.Dimension(640, 190));
        battleScrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(java.awt.Color.black));
        battleScrollPane.getViewport().add(BattleTable, null);
        add(battleScrollPane, java.awt.BorderLayout.NORTH);
    }

    /**
     * @return Returns the battleScrollPane.
     */
    public javax.swing.JScrollPane getBattleScrollPane() {
        return battleScrollPane;
    }

    /**
     * Return the model. Used to refresh.
     */
    public mekwars.client.gui.CBattlePanel.BattlesModel getBattleTableModel() {
        return battleTableModel;
    }

    /**
     * @return Returns the battleTable.
     */
    public javax.swing.JTable getBattleTable() {
        return BattleTable;
    }

    class BattlePopupListener extends java.awt.event.MouseAdapter implements java.awt.event.ActionListener {

        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {maybeShowPopup(e);}

        @Override
        public void mouseReleased(java.awt.event.MouseEvent e) {maybeShowPopup(e);}

        private void maybeShowPopup(java.awt.event.MouseEvent e) {

            javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
            javax.swing.JMenuItem menuItem;

            if (e.isPopupTrigger()) {

                int currRow = BattleTable.rowAtPoint(e.getPoint());
                String curName = (String) battleSorter.getValueAt(currRow, 0);//host name

                MMGame curGame = mwclient.getServers().get(curName);
                if (curGame == null) {return;}

                if (curGame.getCurrentPlayers().size() < curGame.getMaxPlayers() || mwclient.isMod()) {

                    menuItem = new javax.swing.JMenuItem("View game");
                    menuItem.setActionCommand("V|" + curName);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    if (curGame.getStatus().equals("Open")) {
                        menuItem = new javax.swing.JMenuItem("Join game");
                        menuItem.setActionCommand("J|" + curName);
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                } else {
                    menuItem = new javax.swing.JMenuItem("Game is full");
                    popup.add(menuItem);
                }

                if (curGame.getHostName().equals(mwclient.getUsername())) {
                    menuItem = new javax.swing.JMenuItem("Stop Hosting");
                    menuItem.setActionCommand("S|" + curName);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                }

                if (curGame.getHostName().startsWith("[Dedicated]")) {

                    javax.swing.JMenu serviceMenu = new javax.swing.JMenu("Maintenance");
                    javax.swing.JMenu settingsMenu = new javax.swing.JMenu("Settings");
                    javax.swing.JMenu portMenu = new javax.swing.JMenu("Port");
                    javax.swing.JMenu ownersMenu = new javax.swing.JMenu("Owners");
                    javax.swing.JMenu miscMenu = new javax.swing.JMenu("Misc");
                    javax.swing.JMenu autoRestartMenu = new javax.swing.JMenu("AutoRestart");
                    javax.swing.JMenu updateMenu = new javax.swing.JMenu("Update");

                    popup.addSeparator();
                    menuItem = new javax.swing.JMenuItem("Restart Dedicated");
                    menuItem.setActionCommand("RESTART|" + curName);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Load Autosave");
                    menuItem.setActionCommand("LOADAUTOSAVE|" + curName);
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    popup.addSeparator();

                    menuItem = new javax.swing.JMenuItem("Reset Dedicated");
                    menuItem.setActionCommand("RESET|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Kill Dedicated");
                    menuItem.setActionCommand("DIE|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Start Dedicated");
                    menuItem.setActionCommand("START|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Stop Dedicated");
                    menuItem.setActionCommand("STOP|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Load Game");
                    menuItem.setActionCommand("LOADGAME|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Display Saved Games");
                    menuItem.setActionCommand("DSG|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    javax.swing.JMenu logMenu = new javax.swing.JMenu("Logs");

                    menuItem = new javax.swing.JMenuItem("Display MegaMek Log");
                    menuItem.setActionCommand("DMML|" + curName);
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Display Error Log");
                    menuItem.setActionCommand("DDEL|" + curName);
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Display Log");
                    menuItem.setActionCommand("DELL|" + curName);
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    serviceMenu.add(logMenu);

                    menuItem = new javax.swing.JMenuItem("Ping Dedicated");
                    menuItem.setActionCommand("PING|" + curName);
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Get Update URL");
                    menuItem.setActionCommand("GETUPDATEURL|" + curName);
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Set Update URL");
                    menuItem.setActionCommand("SETUPDATEURL|" + curName);
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Update Dedicated");
                    menuItem.setActionCommand("UPDATE|" + curName);
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Current Owners");
                    menuItem.setActionCommand("OWNERS|" + curName);
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Add Dedicated Owners");
                    menuItem.setActionCommand("ADDOWNERS|" + curName);
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Clear Dedicated Owners");
                    menuItem.setActionCommand("CLEAROWNERS|" + curName);
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Current Port");
                    menuItem.setActionCommand("GETPORT|" + curName);
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Set Dedicated Port");
                    menuItem.setActionCommand("SETPORT|" + curName);
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Set Dedicated Name");
                    menuItem.setActionCommand("SETNAME|" + curName);
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Set Dedicated Comment");
                    menuItem.setActionCommand("SETCOMMENT|" + curName);
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Set Dedicated Max Players");
                    menuItem.setActionCommand("SETPLAYERS|" + curName);
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Current Saved Games Purge Days");
                    menuItem.setActionCommand("GSGPD|" + curName);
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Set Saved Games Purge Days");
                    menuItem.setActionCommand("SSGPD|" + curName);
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new javax.swing.JMenuItem("Current Restart Count");
                    menuItem.setActionCommand("CURRENTRESTART|" + curName);
                    menuItem.addActionListener(this);
                    autoRestartMenu.add(menuItem);
                    menuItem = new javax.swing.JMenuItem("Set Dedicated Restart Count");
                    menuItem.setActionCommand("SETRESTART|" + curName);
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
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }

        public void actionPerformed(java.awt.event.ActionEvent actionEvent) {
            String s = actionEvent.getActionCommand();
            if (s.startsWith("V|")) {mwclient.startClient(s.substring(2), false);}
            if (s.startsWith("J|")) {mwclient.startClient(s.substring(2), true);}
            if (s.startsWith("S|")) {
                mwclient.getMainFrame().stopHost();
                mwclient.stopHost();
            }

            if (s.startsWith("RESTART|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to restart\n\r" + currName + "?",
                      "Restart?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",restart");
                }
            } else if (s.startsWith("RESET|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to reset\n\r" + currName + "?",
                      "Reset?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",reset");
                }
            } else if (s.startsWith("DIE|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to kill\n\r" + currName + "?",
                      "Kill?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",die");
                }
            } else if (s.startsWith("START|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to start\n\r" + currName + "?",
                      "Start?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",start");
                }
            } else if (s.startsWith("STOP|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to stop\n\r" + currName + "?",
                      "Stop?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",stop");
                }
            } else if (s.startsWith("OWNERS|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",owners");
            } else if (s.startsWith("CLEAROWNERS|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to clear the owners of\n\r" + currName + "?",
                      "Clear the owners?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",clearowners");
                }
            } else if (s.startsWith("ADDOWNERS|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a list of owners you want to add to\n\r" + currName + "\n\r(sperated by $)",
                      "Add Owners",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() > 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",owner " + result);
                }
            } else if (s.startsWith("GETPORT|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",port");
            } else if (s.startsWith("SETPORT|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new port for\n\r" + currName,
                      "New Port",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() > 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",port " + result);
                }
            } else if (s.startsWith("GSGPD|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",savegamepurge");
            } else if (s.startsWith("SSGPD|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new day for\n\r" + currName,
                      "New days out to purge",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() >= 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                            "mail " +
                                            currName +
                                            ",savegamepurge " +
                                            result);
                }
            } else if (s.startsWith("PING|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",ping");
            } else if (s.startsWith("UPDATE|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",update");
            } else if (s.startsWith("DSG|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",displaysavedgames");
            } else if (s.startsWith("DMML|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",displaymegameklog");
            } else if (s.startsWith("DDEL|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",displaydederrorlog");
            } else if (s.startsWith("DELL|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",displaydedlog");
            } else if (s.startsWith("LOADGAME|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = null;
                result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter name of the save file on\n\r" + currName + "\n\r(leave blank to load autosave.sav)",
                      "Load Game",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",loadgame " + result);
                }
            } else if (s.startsWith("LOADAUTOSAVE|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                int result = javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(),
                      "Are you sure you want to load the autosave game on\n\r" + currName + "?",
                      "Load Auto Saved Game?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",loadautosave");
                }
            } else if (s.startsWith("SETNAME|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new name for\n\r" +
                            currName +
                            "\n\rNote: This will kill the Ded. A restart will be required.",
                      "New Name",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() > 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",name " + result);
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",die");
                }
            } else if (s.startsWith("SETCOMMENT|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new comment for\n\r" + currName,
                      "New Comment",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() > 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",comment " + result);
                }
            } else if (s.startsWith("SETPLAYERS|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter the max number of players for\n\r" + currName,
                      "New Players",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() > 0) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",players " + result);
                }
            } else if (s.startsWith("CURRENTRESTART|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",restartcount");
            } else if (s.startsWith("SETRESTART|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new restart count for\n\r" + currName,
                      "New Restart",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() >= 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",restartcount " + result);
                }
            } else if (s.startsWith("GETUPDATEURL|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",getupdateurl");
            } else if (s.startsWith("SETUPDATEURL|")) {
                String currName = s.substring(s.indexOf('|') + 1);
                String result = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "Enter a new update url for\n\r" + currName,
                      "New Update URL",
                      javax.swing.JOptionPane.OK_CANCEL_OPTION);
                if (result != null && result.length() >= 1) {
                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "mail " + currName + ",setupdateurl " + result);
                }
            }

        }
    }


    class BattlesModel extends javax.swing.table.AbstractTableModel {

        /**
         *
         */
        private static final long serialVersionUID = -6384905445657195650L;

        public Object[] sortedGames; //not really though, sort is handled elsewhere...

        public final static int NAME = 0;
        public final static int PLAYERCOUNT = 1;
        public final static int VERSION = 2;
        public final static int COMMENT = 3;
        public final static int PLAYERNAMES = 4;

        final String[] columnNames = {
              "Name",
              "Players",
              "Version",
              "Comment",
              "Player Names"
        };

        final String[] longValues = {
              "XXXXXXXXXXXXXXXXX",
              "XXXXXXXXXXXXXXXXX",
              "XXXXXXXXXXXXXXXXX",
              "XXXXXXXXXXXXXXXXX",
              "XXXXXXXXXXXXXXXXX",
              };

        public int getColumnCount() {
            return this.columnNames.length;
        }

        public BattlesModel() {
            this.sortedGames = mwclient.getServers().values().toArray();
        }

        public void refreshModel() {
            //do a resort
            this.sortedGames = mwclient.getServers().values().toArray();
            this.fireTableDataChanged();
        }

        public int getRowCount() {
            return this.sortedGames.length;
        }

        @Override
        public String getColumnName(int col) {
            return (columnNames[col]);
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }

        public Object getValueAt(int row, int col) {

            if (row < 0) {return "";}

            if (row >= sortedGames.length) {return "";}

            MMGame aGame = (MMGame) this.sortedGames[row];

            switch (col) {
                case NAME:
                    return aGame.getHostName();
                case PLAYERCOUNT:
                    return aGame.getCurrentPlayers().size() + "/" + aGame.getMaxPlayers();
                case VERSION:
                    return aGame.getVersion();
                case COMMENT:
                    return aGame.getComment();
                case PLAYERNAMES:

                    StringBuffer result = new StringBuffer();
                    for (String currName : aGame.getCurrentPlayers()) {result.append(currName + ", ");}

                    String toReturn = result.toString().trim();
                    if (toReturn.lastIndexOf(",") >= 0) {toReturn = toReturn.substring(0, toReturn.lastIndexOf(","));}

                    return toReturn;
            }

            return "";
        }

        public mekwars.client.gui.CBattlePanel.BattlesModel.Renderer getRenderer() {
            return new mekwars.client.gui.CBattlePanel.BattlesModel.Renderer();
        }

        /*
         * Renderer cannot be static because it uses parent data structs.
         */

        class Renderer extends javax.swing.table.DefaultTableCellRenderer {

            /**
             *
             */
            private static final long serialVersionUID = -2353501701911884548L;

            @Override
            public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
                  boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table,
                      value,
                      isSelected,
                      hasFocus,
                      row,
                      column);

                if (sortedGames.length <= row) {return c;}
                if (isSelected) {return c;}

                String gameName = (String) battleSorter.getValueAt(row, 0);//host name
                MMGame game = mwclient.getServers().get(gameName);

                //set background color
                if (game.getCurrentPlayers().size() >= game.getMaxPlayers()) {
                    c.setBackground(java.awt.Color.red);
                } else if (game.getStatus().equals("Open")) {
                    c.setBackground(java.awt.Color.green);
                } else if (game.getStatus().equals("Running")) {c.setBackground(java.awt.Color.yellow);} else {
                    c.setBackground(getBackground());
                }

                return c;
            }
        }
    }

}
