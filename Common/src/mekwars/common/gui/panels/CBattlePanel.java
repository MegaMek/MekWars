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
 * The panel where all currently active battles are shown
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */


public class CBattlePanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -1556406945897698254L;
    private final IClient client;
    private final JTable BattleTable;
    private final BattlesModel battleTableModel;
    private final JScrollPane battleScrollPane;
    private final TableSorter battleSorter;

    /**
     * Construct a new battle panel
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

    public IClient getClient() {
        return client;
    }

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

    class BattlePopupListener extends MouseAdapter implements ActionListener {

        @Override
        public void mousePressed(MouseEvent event) {
            maybeShowPopup(event);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            maybeShowPopup(event);
        }

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
                    menuItem.setActionCommand(STR."V|\{curName}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    if (curGame.getStatus().equals("Open")) {
                        menuItem = new javax.swing.JMenuItem("Join game");
                        menuItem.setActionCommand(STR."J|\{curName}");
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                } else {
                    menuItem = new JMenuItem("Game is full");
                    popup.add(menuItem);
                }

                if (curGame.getHostName().equals(client.getUsername())) {
                    menuItem = new JMenuItem("Stop Hosting");
                    menuItem.setActionCommand(STR."S|\{curName}");
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
                    menuItem.setActionCommand(STR."RESTART|\{curName}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    menuItem = new JMenuItem("Load Autosave");
                    menuItem.setActionCommand(STR."LOADAUTOSAVE|\{curName}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);
                    popup.addSeparator();

                    menuItem = new JMenuItem("Reset Dedicated");
                    menuItem.setActionCommand(STR."RESET|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Kill Dedicated");
                    menuItem.setActionCommand(STR."DIE|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Start Dedicated");
                    menuItem.setActionCommand(STR."START|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Stop Dedicated");
                    menuItem.setActionCommand(STR."STOP|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);
                    menuItem = new JMenuItem("Load Game");
                    menuItem.setActionCommand(STR."LOADGAME|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Saved Games");
                    menuItem.setActionCommand(STR."DSG|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    JMenu logMenu = new JMenu("Logs");

                    menuItem = new JMenuItem("Display MegaMek Log");
                    menuItem.setActionCommand(STR."DMML|\{curName}");
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Error Log");
                    menuItem.setActionCommand(STR."DDEL|\{curName}");
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    menuItem = new JMenuItem("Display Log");
                    menuItem.setActionCommand(STR."DELL|\{curName}");
                    menuItem.addActionListener(this);
                    logMenu.add(menuItem);

                    serviceMenu.add(logMenu);

                    menuItem = new JMenuItem("Ping Dedicated");
                    menuItem.setActionCommand(STR."PING|\{curName}");
                    menuItem.addActionListener(this);
                    serviceMenu.add(menuItem);

                    menuItem = new JMenuItem("Get Update URL");
                    menuItem.setActionCommand(STR."GETUPDATEURL|\{curName}");
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Set Update URL");
                    menuItem.setActionCommand(STR."SETUPDATEURL|\{curName}");
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Update Dedicated");
                    menuItem.setActionCommand(STR."UPDATE|\{curName}");
                    menuItem.addActionListener(this);
                    updateMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Owners");
                    menuItem.setActionCommand(STR."OWNERS|\{curName}");
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new JMenuItem("Add Dedicated Owners");
                    menuItem.setActionCommand(STR."ADDOWNERS|\{curName}");
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);
                    menuItem = new JMenuItem("Clear Dedicated Owners");
                    menuItem.setActionCommand(STR."CLEAROWNERS|\{curName}");
                    menuItem.addActionListener(this);
                    ownersMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Port");
                    menuItem.setActionCommand(STR."GETPORT|\{curName}");
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Port");
                    menuItem.setActionCommand(STR."SETPORT|\{curName}");
                    menuItem.addActionListener(this);
                    portMenu.add(menuItem);

                    menuItem = new JMenuItem("Set Dedicated Name");
                    menuItem.setActionCommand(STR."SETNAME|\{curName}");
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Comment");
                    menuItem.setActionCommand(STR."SETCOMMENT|\{curName}");
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Max Players");
                    menuItem.setActionCommand(STR."SETPLAYERS|\{curName}");
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Saved Games Purge Days");
                    menuItem.setActionCommand(STR."GSGPD|\{curName}");
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Saved Games Purge Days");
                    menuItem.setActionCommand(STR."SSGPD|\{curName}");
                    menuItem.addActionListener(this);
                    miscMenu.add(menuItem);

                    menuItem = new JMenuItem("Current Restart Count");
                    menuItem.setActionCommand(STR."CURRENTRESTART|\{curName}");
                    menuItem.addActionListener(this);
                    autoRestartMenu.add(menuItem);
                    menuItem = new JMenuItem("Set Dedicated Restart Count");
                    menuItem.setActionCommand(STR."SETRESTART|\{curName}");
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
                      STR."""
Are you sure you want to restart
\r\{currName}?""",
                      "Restart?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},restart");
                }
            } else if (actionCommand.startsWith("RESET|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to reset
\r\{currName}?""",
                      "Reset?",
                      javax.swing.JOptionPane.YES_NO_OPTION);
                if (result == javax.swing.JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},reset");
                }
            } else if (actionCommand.startsWith("DIE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to kill
\r\{currName}?""",
                      "Kill?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},die");
                }
            } else if (actionCommand.startsWith("START|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to start
\r\{currName}?""",
                      "Start?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},start");
                }
            } else if (actionCommand.startsWith("STOP|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to stop
\r\{currName}?""",
                      "Stop?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},stop");
                }
            } else if (actionCommand.startsWith("OWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},owners");
            } else if (actionCommand.startsWith("CLEAROWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to clear the owners of
\r\{currName}?""",
                      "Clear the owners?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},clearowners");
                }
            } else if (actionCommand.startsWith("ADDOWNERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a list of owners you want to add to
\r\{currName}
\r(sperated by $)""",
                      "Add Owners",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},owner \{result}");
                }
            } else if (actionCommand.startsWith("GETPORT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},port");
            } else if (actionCommand.startsWith("SETPORT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new port for
\r\{currName}""",
                      "New Port",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},port \{result}");
                }
            } else if (actionCommand.startsWith("GSGPD|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},savegamepurge");
            } else if (actionCommand.startsWith("SSGPD|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new day for
\r\{currName}""",
                      "New days out to purge",
                      JOptionPane.OK_CANCEL_OPTION);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},savegamepurge \{result}");
                }
            } else if (actionCommand.startsWith("PING|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},ping");
            } else if (actionCommand.startsWith("UPDATE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},update");
            } else if (actionCommand.startsWith("DSG|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},displaysavedgames");
            } else if (actionCommand.startsWith("DMML|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},displaymegameklog");
            } else if (actionCommand.startsWith("DDEL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},displaydederrorlog");
            } else if (actionCommand.startsWith("DELL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},displaydedlog");
            } else if (actionCommand.startsWith("LOADGAME|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = null;
                result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter name of the save file on
\r\{currName}
\r(leave blank to load autosave.sav)""",
                      "Load Game",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},loadgame \{result}");
                }
            } else if (actionCommand.startsWith("LOADAUTOSAVE|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                      STR."""
Are you sure you want to load the autosave game on
\r\{currName}?""",
                      "Load Auto Saved Game?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},loadautosave");
                }
            } else if (actionCommand.startsWith("SETNAME|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new name for
\r\{currName}
\rNote: This will kill the Ded. A restart will be required.""",
                      "New Name",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},name \{result}");
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},die");
                }
            } else if (actionCommand.startsWith("SETCOMMENT|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new comment for
\r\{currName}""",
                      "New Comment",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && result.length() > 1) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},comment \{result}");
                }
            } else if (actionCommand.startsWith("SETPLAYERS|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter the max number of players for
\r\{currName}""",
                      "New Players",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},players \{result}");
                }
            } else if (actionCommand.startsWith("CURRENTRESTART|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},restartcount");
            } else if (actionCommand.startsWith("SETRESTART|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new restart count for
\r\{currName}""",
                      "New Restart",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},restartcount \{result}");
                }
            } else if (actionCommand.startsWith("GETUPDATEURL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},getupdateurl");
            } else if (actionCommand.startsWith("SETUPDATEURL|")) {
                String currName = actionCommand.substring(actionCommand.indexOf('|') + 1);
                String result = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."""
Enter a new update url for
\r\{currName}""",
                      "New Update URL",
                      JOptionPane.WARNING_MESSAGE);
                if (result != null && !result.isEmpty()) {
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{currName},setupdateurl \{result}");
                }
            }
        }
    }
}
