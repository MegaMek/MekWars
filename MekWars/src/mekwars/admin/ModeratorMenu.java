/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
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
package admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import client.MWClient;
import client.gui.dialog.NewUnitViewerDialog;
import client.gui.dialog.PlanetNameDialog;
import client.gui.dialog.PlayerNameDialog;
import common.UnitFactory;
import megamek.client.ui.swing.UnitLoadingDialog;

public class ModeratorMenu extends JMenu {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    // moderator menu components
    JMenuItem jMenuModCheck = new JMenuItem();
    JMenuItem jMenuModGames = new JMenuItem();
    JMenuItem jMenuModGetModLog = new JMenuItem();
    JMenuItem jMenuModGrantEXP = new JMenuItem();
    JMenuItem jMenuModGrantInfluence = new JMenuItem();
    JMenuItem jMenuModGrantMoney = new JMenuItem();
    JMenuItem jMenuModGrantReward = new JMenuItem();
    JMenuItem jMenuModListCommands = new JMenuItem();
    JMenuItem jMenuModListMultiPlayerGroups = new JMenuItem();
    JMenuItem jMenuModTerminate = new JMenuItem();
    JMenuItem jMenuModDeactivate = new JMenuItem();
    JMenuItem jMenuModLog = new JMenuItem();
    JMenuItem jMenuModNoPlay = new JMenuItem();
    JMenuItem jMenuModSetElo = new JMenuItem();
    JMenuItem jMenuModSetPricemod = new JMenuItem();
    JMenuItem jMenuModTerminateContract = new JMenuItem();
    JMenuItem jMenuModTouch = new JMenuItem();
    JMenuItem jMenuModUnlockLances = new JMenuItem();
    JMenuItem jMenuModCreateUnit = new JMenuItem();
    JMenuItem jMenuModRefreshFactory = new JMenuItem();
    JMenuItem jMenuModUpdateServerUnitsCache = new JMenuItem();

    MWClient mwclient;

    private int userLevel = 0;

    // constructor
    public ModeratorMenu() {
        super("Player Config");
    }

    public void createMenu(MWClient client) {

        mwclient = client;

        userLevel = mwclient.getUser(mwclient.getUsername()).getUserlevel();
        /*
         * This is code extracted from CMainFrame. It could be dramatically
         * improced. There isn't much need for the seperate methods for each
         * action, for example.
         */
        jMenuModCheck.setText("Check Player");
        jMenuModCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModCheck_actionPerformed(e);
            }
        });

        jMenuModGames.setText("View Games");
        jMenuModGames.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGames_actionPerformed(e);
            }
        });

        jMenuModGetModLog.setText("Get Mod Log");
        jMenuModGetModLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGetModLog_actionPerformed(e);
            }
        });

        jMenuModGrantEXP.setText("Grant EXP");
        jMenuModGrantEXP.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGrantEXP_actionPerformed(e, null);
            }
        });

        jMenuModGrantInfluence.setText("Grant " + client.moneyOrFluMessage(false, true, -1) + "");
        jMenuModGrantInfluence.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGrantInfluence_actionPerformed(e, null);
            }
        });

        jMenuModGrantMoney.setText("Grant " + client.moneyOrFluMessage(true, true, -1));
        jMenuModGrantMoney.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGrantMoney_actionPerformed(e, null);
            }
        });

        jMenuModGrantReward.setText("Grant Reward");
        jMenuModGrantReward.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModGrantReward_actionPerformed(e, null);
            }
        });

        jMenuModListCommands.setText("List Commands");
        jMenuModListCommands.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModListCommands_actionPerformed(e);
            }
        });

        jMenuModListMultiPlayerGroups.setText("List Multi Player Groups");
        jMenuModListMultiPlayerGroups.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModListMultiPlayerGroups_actionPerformed(e);
            }
        });

        jMenuModTerminate.setText("Cancel Game");
        jMenuModTerminate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModTerminate_actionPerformed(e);
            }
        });

        jMenuModDeactivate.setText("Deactivate Player");
        jMenuModDeactivate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModDeactivate_actionPerformed(e);
            }
        });

        jMenuModLog.setText("Mod Log");
        jMenuModLog.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModLog_actionPerformed(e);
            }
        });

        jMenuModNoPlay.setText("No Play");
        jMenuModNoPlay.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModNoPlay_actionPerformed(e);
            }
        });

        jMenuModSetElo.setText("Set ELO");
        jMenuModSetElo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModSetElo_actionPerformed(e);
            }
        });

        jMenuModSetPricemod.setText("Set Price Mod");
        jMenuModSetPricemod.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModSetPricemod_actionPerformed(e);
            }
        });

        jMenuModTerminateContract.setText("Terminate Contract");
        jMenuModTerminateContract.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModTerminateContract_actionPerformed(e);
            }
        });

        jMenuModTouch.setText("Touch Player");
        jMenuModTouch.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModTouch_actionPerformed(e);
            }
        });

        jMenuModUnlockLances.setText("Unlock Armies");
        jMenuModUnlockLances.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModUnlockLances_actionPerformed(e);
            }
        });

        jMenuModCreateUnit.setText("Create Unit");
        jMenuModCreateUnit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModCreateUnit_actionPerformed(e);
            }
        });

        jMenuModRefreshFactory.setText("Refresh Factory");
        jMenuModRefreshFactory.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                jMenuModRefreshFactory_actionPerformed(e);
            }
        });

        jMenuModUpdateServerUnitsCache.setText("Update Server Units Cache");
        jMenuModUpdateServerUnitsCache.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c UpdateServerUnitsCache");
            }
        });

        // clear the menu, in case this is a reconstruction
        removeAll();

        // then name, add components, etc.
        JMenu subMenu = new JMenu();

        if (userLevel >= mwclient.getData().getAccessLevel("Check")) {
            this.add(jMenuModCheck);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModGames")) {
            this.add(jMenuModGames);
        }
        addSeparator();

        subMenu = new JMenu();
        subMenu.setText("Grant");
        if (userLevel >= mwclient.getData().getAccessLevel("GrantEXP")) {
            subMenu.add(jMenuModGrantEXP);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("GrantInfluence")) {
            subMenu.add(jMenuModGrantInfluence);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("GrantMoney")) {
            subMenu.add(jMenuModGrantMoney);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("GrantReward")) {
            subMenu.add(jMenuModGrantReward);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

        subMenu = new JMenu();
        subMenu.setText("List");
        if (userLevel >= mwclient.getData().getAccessLevel("ListCommands")) {
            subMenu.add(jMenuModListCommands);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ListMultiPlayerGroups")) {
            subMenu.add(jMenuModListMultiPlayerGroups);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

        subMenu = new JMenu();
        subMenu.setText("Games");
        if (userLevel >= mwclient.getData().getAccessLevel("UnlockLances")) {
            subMenu.add(jMenuModUnlockLances);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModTerminate")) {
            subMenu.add(jMenuModTerminate);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModDeactivate")) {
            subMenu.add(jMenuModDeactivate);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

        subMenu = new JMenu();
        subMenu.setText("Logs");
        if (userLevel >= mwclient.getData().getAccessLevel("GetModLog")) {
            subMenu.add(jMenuModGetModLog);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModLog")) {
            subMenu.add(jMenuModLog);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

        subMenu = new JMenu();
        subMenu.setText("Set");
        if (userLevel >= mwclient.getData().getAccessLevel("SetElo")) {
            subMenu.add(jMenuModSetElo);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("SetPriceMod")) {
            subMenu.add(jMenuModSetPricemod);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModNoPlay")) {
            subMenu.add(jMenuModNoPlay);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

        subMenu = new JMenu();
        subMenu.setText("Misc");
        if (userLevel >= mwclient.getData().getAccessLevel("TerminateContract")) {
            subMenu.add(jMenuModTerminateContract);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("Touch")) {
            subMenu.add(jMenuModTouch);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("CreateUnit")) {
            subMenu.add(jMenuModCreateUnit);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("ModRefreshFactory")) {
            subMenu.add(jMenuModRefreshFactory);
        }
        if (userLevel >= mwclient.getData().getAccessLevel("UpdateServerUnitsCache")) {
            subMenu.add(jMenuModUpdateServerUnitsCache);
        }
        if (subMenu.getItemCount() > 0) {
            this.add(subMenu);
        }

    }// end CreateMenu()

    /*
     * Various mod methods. These really don't need to be stand alone, and could
     * (should?) be worked back into a unified ActionPerformed command, or back
     * into the overloaded actionPerformed commands is createMenu();
     *
     * For now, these methods remain as they were in CMainFrame.
     *
     * @urgru, 6.26.05
     */

    public void jMenuModCheck_actionPerformed(ActionEvent e) {

        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c check#" + name);
    }

    public void jMenuModGames_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c modgames");
    }

    public void jMenuModCheckVersion_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();
        if ((name == null) || (name.length() == 0)) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c checkversion#" + name);
    }

    public void jMenuModGetModLog_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c getmodlog");
    }

    public void jMenuModGrantEXP_actionPerformed(ActionEvent e, String player) {

        String name = "";
        if (player == null) {
            PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
            playerDialog.setVisible(true);
            name = playerDialog.getPlayerName();
            playerDialog.dispose();
        } else {
            name = player;
        }

        if ((name == null) || (name.length() == 0)) {
            return;
        }
        String exp = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Exp Amount,- to remove");
        if ((exp == null) || (exp.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c grantexp#" + name + "#" + exp);
    }

    public void jMenuModGrantMoney_actionPerformed(ActionEvent e, String player) {

        String name = null;
        if (player == null) {
            PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
            playerDialog.setVisible(true);
            name = playerDialog.getPlayerName();
            playerDialog.dispose();
        } else {
            name = player;
        }

        if ((name == null) || (name.length() == 0)) {
            return;
        }
        String exp = JOptionPane.showInputDialog(mwclient.getMainFrame(), mwclient.moneyOrFluMessage(true, true, -1) + " Amount,- to remove");
        if ((exp == null) || (exp.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c grantmoney#" + name + "#" + exp);
    }

    public void jMenuModGrantInfluence_actionPerformed(ActionEvent e, String player) {
        String name = null;
        if (player == null) {
            PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
            playerDialog.setVisible(true);
            name = playerDialog.getPlayerName();
            playerDialog.dispose();
        } else {
            name = player;
        }

        if ((name == null) || (name.length() == 0)) {
            return;
        }
        String exp = JOptionPane.showInputDialog(mwclient.getMainFrame(), mwclient.moneyOrFluMessage(false, true, -1) + " Amount,- to remove");
        if ((exp == null) || (exp.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c grantinfluence#" + name + "#" + exp);
    }

    public void jMenuModGrantReward_actionPerformed(ActionEvent e, String player) {
        String name = null;
        if (player == null) {
            PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
            playerDialog.setVisible(true);
            name = playerDialog.getPlayerName();
            playerDialog.dispose();
        } else {
            name = player;
        }

        String exp = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Reward Amount,- to remove");
        if ((exp == null) || (exp.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c grantreward#" + name + "#" + exp);
    }

    public void jMenuModListCommands_actionPerformed(ActionEvent e) {
        String name = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Partial Command");
        if (name == null) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c listcommands#" + name);
    }

    public void jMenuModListMultiPlayerGroups_actionPerformed(ActionEvent e) {
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c listmultiplayergroups");
    }

    public void jMenuModTerminate_actionPerformed(ActionEvent e) {
        String id = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Game ID");
        if ((id == null) || (id.length() == 0)) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c modterminate#" + id);
    }

    public void jMenuModDeactivate_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose Player to Deactivate", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c moddeactivate#" + name);
    }

    public void jMenuModLog_actionPerformed(ActionEvent e) {
        String name = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Enter comments you would like to add the the mod log");
        if ((name == null) || (name.length() == 0)) {
            return;
        }
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c modlog#" + name);
    }

    public void jMenuModNoPlay_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        String mode = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Mode (add/remove)");
        if ((mode == null) || (mode.length() == 0)) {
            return;
        }

        String offender = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Player to add");

        if ((offender == null) || (offender.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c modnoplay#" + mode + "#" + name + "#" + offender);
    }

    public void jMenuModSetElo_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        String elo = JOptionPane.showInputDialog(mwclient.getMainFrame(), "ELO");
        if ((elo == null) || (elo.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c setelo#" + name + "#" + elo);
    }

    public void jMenuModSetPricemod_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        String elo = JOptionPane.showInputDialog(mwclient.getMainFrame(), "Price Mod Amount(- to remove)");
        if ((elo == null) || (elo.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c setpricemod#" + name + "#" + elo);
    }

    public void jMenuModTerminateContract_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c terminatecontract#" + name);
    }

    public void jMenuModTouch_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c touch#" + name);
    }

    public void jMenuModUnlockLances_actionPerformed(ActionEvent e) {
        PlayerNameDialog playerDialog = new PlayerNameDialog(mwclient, "Choose a Player", PlayerNameDialog.ANY_PLAYER);
        playerDialog.setVisible(true);
        String name = playerDialog.getPlayerName();
        playerDialog.dispose();

        if ((name == null) || (name.length() == 0)) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c unlocklances#" + name);
    }

    public void jMenuModCreateUnit_actionPerformed(ActionEvent e) {
        UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(mwclient.getMainFrame());
        NewUnitViewerDialog unitSelector = new NewUnitViewerDialog(mwclient.getMainFrame(), unitLoadingDialog, mwclient,NewUnitViewerDialog.UNIT_SELECTOR);
        unitSelector.setName("Unit Selector");
        new Thread(unitSelector).start();
    }

    public void jMenuModRefreshFactory_actionPerformed(ActionEvent e) {
        PlanetNameDialog planetDialog = new PlanetNameDialog(mwclient, "Select a Planet", null);
        planetDialog.setVisible(true);
        String planetNamestr = planetDialog.getPlanetName();
        planetDialog.dispose();

        if ((planetNamestr == null) || (planetNamestr.length() == 0)) {
            return;
        }

        JComboBox combo = new JComboBox();

        for (UnitFactory factory : mwclient.getData().getPlanetByName(planetNamestr).getUnitFactories()) {
            // if ( factory.getTicksUntilRefresh() > 0)
            combo.addItem(factory.getName());
        }

        combo.setEditable(false);
        JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

        JDialog dlg = jop.createDialog(mwclient.getMainFrame(), "Select a factory.");
        combo.grabFocus();
        combo.getEditor().selectAll();

        dlg.setVisible(true);

        int value = ((Integer) jop.getValue()).intValue();

        if (value == JOptionPane.CANCEL_OPTION) {
            return;
        }

        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c modrefreshFactory#" + planetNamestr + "#" + combo.getSelectedItem().toString());
    }

}// end AdminMenu class
