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
package mekwars.admin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.StringTokenizer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.HouseNameDialog;
import mekwars.common.gui.dialogs.PlayerNameDialog;

public class StaffUserlistPopupMenu extends JMenu implements ActionListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 9145688971748962135L;
    // variables
    private IClient client;
    private CUser user = null;

    public StaffUserlistPopupMenu() {
        super("Staff");
    }

    public void createMenu(IClient client, CUser user) {

        // save things
        this.client = client;
        int userLevel = this.client.getUser(this.client.getUsername()).getUserLevel();
        String userName = user.getName();
        this.user = user;

        // format
        JMenuItem item;

        item = new JMenuItem("Kick");
        item.setActionCommand("KK|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("Kick")) {this.add(item);}

        item = new JMenuItem("Ignore");
        item.setActionCommand("MMUTE|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("Ignore")) {this.add(item);}
        this.addSeparator();

        JMenu playerMenu = new JMenu("Player");

        item = new JMenuItem("Check");
        item.setActionCommand("CKU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("Check")) {playerMenu.add(item);}

        item = new JMenuItem("Player Status");
        item.setActionCommand("PS|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("AdminPlayerStatus")) {
            playerMenu.add(item);
        }
        item = new JMenuItem("Set Fluff");
        item.setActionCommand("SF|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("Fluff")) {playerMenu.add(item);}

        item = new JMenuItem("Deactivate");
        item.setActionCommand("DAU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ModDeactivate")) {playerMenu.add(item);}

        item = new JMenuItem("Force Defect");
        item.setActionCommand("FD|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ForcedDefect")) {playerMenu.add(item);}

        item = new JMenuItem("Unlock Armies");
        item.setActionCommand("UUA|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("UnlockLances")) {playerMenu.add(item);}
        if (playerMenu.getItemCount() > 0) {this.add(playerMenu);}

        JMenu grantMenu = new JMenu();
        grantMenu.setText("Grant");

        item = new JMenuItem("Exp");
        item.setActionCommand("GE|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("GrantEXP")) {grantMenu.add(item);}
        item = new JMenuItem(client.getServerConfigs("FluLongName"));
        item.setActionCommand("GI|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("GrantInfluence")) {grantMenu.add(item);}
        item = new JMenuItem(client.getServerConfigs("MoneyLongName"));
        item.setActionCommand("GM|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("GrantMoney")) {grantMenu.add(item);}
        item = new JMenuItem(client.getServerConfigs("RPLongName"));
        item.setActionCommand("GRP|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("GrantReward")) {grantMenu.add(item);}
        item = new JMenuItem("Techs");
        item.setActionCommand("GTCH|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("GrantTechs")) {grantMenu.add(item);}
        if (grantMenu.getItemCount() > 0) {this.add(grantMenu);}

        // units submenu
        JMenu unitsMen = new JMenu();
        unitsMen.setText("Units");
        item = new JMenuItem("Strip All");
        item.setActionCommand("SAU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("StripUnits")) {unitsMen.add(item);}
        //@Salient added for free build
        item = new JMenuItem("Reset Free Unit Limit");
        item.setActionCommand("RFUL|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ResetFreeMeks")
                  && Integer.parseInt(this.client.getServerConfigs("FreeBuild_Limit")) > 0) {unitsMen.add(item);}
        //@Salient added for locked units
        item = new JMenuItem("Unlock Units [MC]");
        item.setActionCommand("UUM|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ADMINUNLOCKUNITSMC")
                  && Boolean.parseBoolean(this.client.getServerConfigs("LockUnits"))) {unitsMen.add(item);}
        //@Salient added for mini campaigns
        item = new JMenuItem("Recalc Hangar BV [MC]");
        item.setActionCommand("RBM|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ADMINRECALCHANGARBVMC")
                  && Boolean.parseBoolean(this.client.getServerConfigs("Enable_MiniCampaign"))) {unitsMen.add(item);}
        item = new JMenuItem("Donate");
        item.setActionCommand("DU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("AdminDonate")) {unitsMen.add(item);}
        item = new JMenuItem("Scrap");
        item.setActionCommand("SU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("AdminScrap")) {unitsMen.add(item);}
        item = new JMenuItem("Transfer");
        item.setActionCommand("TU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("AdminTransFer")) {unitsMen.add(item);}
        if (Boolean.parseBoolean(this.client.getServerConfigs("UseAdvanceRepair"))) {
            item = new JMenuItem("Repair");
            item.setActionCommand("FRU|" + userName);
            item.addActionListener(this);
            if (userLevel >= this.client.getData().getAccessLevel("ModFullRepair")) {unitsMen.add(item);}
        }
        item = new JMenuItem("Fix Ammo");
        item.setActionCommand("FUA|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("FixAmmo")) {unitsMen.add(item);}
        item = new JMenuItem("View");
        item.setActionCommand("VPU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ViewPlayerUnit")) {unitsMen.add(item);}
        item = new JMenuItem("Repair View");
        item.setActionCommand("VPUR|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ViewPlayerUnit")) {unitsMen.add(item);}

        if (unitsMen.getItemCount() > 0) {this.add(unitsMen);}

        // groups submenu
        JMenu groupsMen = new JMenu();
        groupsMen.setText("Multiplayer Groups");
        item = new JMenuItem("Add Player");
        item.setActionCommand("MPGAU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("SetMultiPlayerGroup")) {groupsMen.add(item);}
        item = new JMenuItem("Remove Player");
        item.setActionCommand("MPGRU|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("SetMultiPlayerGroup")) {groupsMen.add(item);}
        item = new JMenuItem("List");
        item.setActionCommand("MPGL|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ListMultiPlayerGroups")) {groupsMen.add(item);}

        if (groupsMen.getItemCount() > 0) {playerMenu.add(groupsMen);}

        // Pilots submenu
        JMenu pilotsMen = new JMenu();
        pilotsMen.setText("Pilots");
        item = new JMenuItem("View Pilot Queue");
        item.setActionCommand("VPPQ|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ViewPlayerPersonalPilotQueue")) {pilotsMen.add(item);}

        item = new JMenuItem("Remove Pilot");
        item.setActionCommand("RPPQ|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("RemovePilot")) {pilotsMen.add(item);}

        item = new JMenuItem("Create Pilot");
        item.setActionCommand("CPPQ|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("CreatePilot")) {pilotsMen.add(item);}

        if (pilotsMen.getItemCount() > 0 &&
                  Boolean.parseBoolean(this.client.getServerConfigs("AllowPersonalPilotQueues"))) {this.add(pilotsMen);}

        // Parts submenu
        JMenu partsMen = new JMenu();
        partsMen.setText("Parts");
        item = new JMenuItem("View Parts Cache");
        item.setActionCommand("VPC|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("ViewPlayerParts")) {partsMen.add(item);}
        item = new JMenuItem("Remove Part");
        item.setActionCommand("RPC|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("RemoveParts")) {partsMen.add(item);}
        item = new JMenuItem("Add Part");
        item.setActionCommand("APC|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("AddParts")) {partsMen.add(item);}
        item = new JMenuItem("Strip All Parts");
        item.setActionCommand("SAPC|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("StringParts")) {partsMen.add(item);}
        if (partsMen.getItemCount() > 0) {this.add(partsMen);}

        // Flags submenu
        JMenu flagsMen = new JMenu();
        flagsMen.setText("Flags");
        item = new JMenuItem("Toggle Player Flags");
        item.setActionCommand("PF|" + userName);
        item.addActionListener(this);
        if (userLevel >= this.client.getData().getAccessLevel("SetPlayerFlags")) {
            flagsMen.add(item);
        }
        if (flagsMen.getItemCount() > 0) {
            this.add(flagsMen);
        }
    }

    public void actionPerformed(ActionEvent actionEvent) {

        // command helpers
        String s = actionEvent.getActionCommand();
        StringTokenizer st = new StringTokenizer(s, "|");
        String command = st.nextToken();
        String userName;

        // mod commands
        if (command.equals("PF") && st.hasMoreElements()) {
            userName = st.nextToken();

            // Build a picklist of flags
            String fName = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Select a Flag",
                  "Player Flags",
                  JOptionPane.INFORMATION_MESSAGE,
                  null,
                  client.getPlayer().getFlags().getFlagNames().toArray(),
                  null);
            client.sendChat(IClient.CAMPAIGN_PREFIX + "SetPlayerFlags#" + userName + "#" + fName + "#toggle");
        }
        if (command.equals("KK") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                client.sendChat(IClient.CAMPAIGN_PREFIX + "kick " + userName);
            }
        }
        if (command.equals("MMUTE") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                client.sendChat(IClient.CAMPAIGN_PREFIX + "ignore " + userName);
            }
        }
        if (command.equals("CKU") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c check#" + userName);
            }
        }
        if (command.equals("DAU") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c moddeactivate#" + userName);
            }
        }
        if (command.equals("UUA") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c unlocklances#" + userName);
            }
        }

        if (command.equals("GI") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                String exp = JOptionPane.showInputDialog(client.getMainFrame(),
                      client.moneyOrFluMessage(false, true, -1) + " Amount,- to remove");
                if (exp == null || exp.isEmpty()) {return;}

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c grantinfluence#" + userName + "#" + exp);
            }
        }
        if (command.equals("GM") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                String exp = JOptionPane.showInputDialog(client.getMainFrame(),
                      client.moneyOrFluMessage(true, true, -1) + " Amount,- to remove");
                if (exp == null || exp.isEmpty()) {return;}

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c grantmoney#" + userName + "#" + exp);
            }
        }
        if (command.equals("GE") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                String exp = JOptionPane.showInputDialog(client.getMainFrame(), "Exp Amount,- to remove");
                if (exp == null || exp.isEmpty()) {return;}

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c grantexp#" + userName + "#" + exp);
            }
        }
        if (command.equals("GRP") && st.hasMoreElements()) {

            userName = st.nextToken();
            {

                String exp = JOptionPane.showInputDialog(client.getMainFrame(), "Reward Amount,- to remove");
                if (exp == null || exp.isEmpty()) {return;}

                client.sendChat(IClient.CAMPAIGN_PREFIX + "c grantreward#" + userName + "#" + exp);
            }
        }

        if (command.equals("GTCH") && st.hasMoreElements()) {

            userName = st.nextToken();
            String[] techTypes = { "Green", "Reg", "Vet", "Elite" };

            JComboBox<String> combo = new JComboBox<>(techTypes);

            combo.setEditable(false);
            JOptionPane jop = new JOptionPane(combo, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

            JDialog dlg = jop.createDialog(client.getMainFrame(), "Select Tech Type.");
            combo.grabFocus();
            combo.getEditor().selectAll();

            dlg.setVisible(true);

            int type = combo.getSelectedIndex();

            int value = (Integer) jop.getValue();

            if (value == JOptionPane.CANCEL_OPTION) {return;}

            String amount = JOptionPane.showInputDialog(client.getMainFrame(), "Tech Amount,- to remove");
            if (amount == null || amount.isEmpty()) {return;}

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c granttechs#" + userName + "#" + type + "#" + amount);
        }

        // mod commands
        if (command.equals("PS") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c adminplayerstatus#" + userName);
        }
        if (command.equals("FD") && st.hasMoreElements()) {

            userName = st.nextToken();
            // get user's faction
            HouseNameDialog factionDialog = new HouseNameDialog(client, "Faction", true, false);
            factionDialog.setVisible(true);
            String newfaction = factionDialog.getHouseName();
            factionDialog.dispose();

            client.sendChat(IClient.CAMPAIGN_PREFIX + "c forceddefect#" + userName + "#" + newfaction);
        }
        if (command.equals("SF") && st.hasMoreElements()) {

            userName = st.nextToken();
            // fluff to set
            String newfluff = JOptionPane.showInputDialog(client.getMainFrame(),
                  "Fluff? (Leave blank to remove)",
                  user.getFluff());

            if (newfluff != null) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c fluff#" + userName + "#" + newfluff);
            }
        }
        if (command.equals("SAU") && st.hasMoreElements()) {

            userName = st.nextToken();
            // confirm the strip
            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to strip " + userName + "'s units?");
            if (result == JOptionPane.YES_OPTION) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c stripunits#" + userName);
            }
        }
        if (command.equals("RFUL") && st.hasMoreElements()) { //@salient

            userName = st.nextToken();
            // confirm action
            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to reset " + userName + "'s free mek limit?");
            if (result == JOptionPane.YES_OPTION) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c resetfreemeks#" + userName);
            }
        }
        if (command.equals("UUM") && st.hasMoreElements()) { //@salient

            userName = st.nextToken();
            // confirm action
            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to unlock " + userName + "'s units?");
            if (result == JOptionPane.YES_OPTION) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c ADMINUNLOCKUNITSMC#" + userName);
            }
        }
        if (command.equals("RBM") && st.hasMoreElements()) { //@salient

            userName = st.nextToken();
            // confirm action
            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to recalc " + userName + "'s hangar bv?");
            if (result == JOptionPane.YES_OPTION) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c ADMINRECALCHANGARBVMC#" + userName);
            }
        }
        if (command.equals("SU") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#adminscrap");
        }
        if (command.equals("DU") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#admindonate");
        }
        if (command.equals("FUA") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#fixammo");
        }
        if (command.equals("VPU") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#viewplayerunit#" + false);
        }
        if (command.equals("VPUR") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#viewplayerunit#" + true);
        }

        if (command.equals("TU") && st.hasMoreElements()) {

            userName = st.nextToken();
            // receiving user
            PlayerNameDialog playerDialog = new PlayerNameDialog(client,
                  "Receiving Player",
                  PlayerNameDialog.ANY_PLAYER);
            playerDialog.setVisible(true);
            String receivingplayer = playerDialog.getPlayerName();
            playerDialog.dispose();

            if (receivingplayer == null || receivingplayer.isEmpty()) {return;}

            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                  "c getplayerunits#" +
                                  userName +
                                  "#admintransfer#" +
                                  receivingplayer);

        }

        if (command.equals("FRU") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getplayerunits#" + userName + "#ModFullRepair");
        }

        if (command.equals("MPGAU") && st.hasMoreElements()) {

            userName = st.nextToken();
            // fluff to set
            String groupID = JOptionPane.showInputDialog(this, "Group ID?", "0");

            if (groupID != null) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c SetMultiPlayerGroup#" + userName + "#" + groupID);
            }
        }
        if (command.equals("MPGRU") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c SetMultiPlayerGroup#" + userName + "#0");
        }
        if (command.equals("MPGL") && st.hasMoreElements()) {
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c ListMultiPlayerGroups");
        }

        if (command.equals("VPPQ") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c ViewPlayerPersonalPilotQueue#" + userName);
        }
        if (command.equals("RPPQ") && st.hasMoreElements()) {

            userName = st.nextToken();
            Object[] Types = { "All", "Mek", "ProtoMek" };
            Object[] Size = { "All", "Light", "Medium", "Heavy", "Assault" };

            String Typestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Select pilot unit type",
                  "Pilot Unit Type",
                  JOptionPane.INFORMATION_MESSAGE,
                  null,
                  Types,
                  Types[0]);

            if (Typestr == null || Typestr.isEmpty()) {return;}

            String Sizestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Select a pilot unit size",
                  "Pilot Unit Size",
                  JOptionPane.INFORMATION_MESSAGE,
                  null,
                  Size,
                  Size[0]);
            if (Sizestr == null || Sizestr.isEmpty()) {return;}

            String position = "ALL";

            if (!Typestr.equalsIgnoreCase("all") && !Sizestr.equalsIgnoreCase("all")) {
                position = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                      "Pilot Number?",
                      "Number,Range 1-9, or ALL",
                      JOptionPane.OK_OPTION,
                      null,
                      null,
                      "0");
                if (position == null || position.isEmpty()) {return;}
            }
            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                  "c RemovePilot#" +
                                  userName +
                                  "#" +
                                  Typestr +
                                  "#" +
                                  Sizestr +
                                  "#" +
                                  position);
        }
        if (command.equals("CPPQ") && st.hasMoreElements()) {

            userName = st.nextToken();
            Object[] Types = { "Mek", "ProtoMek" };
            Object[] Size = { "Light", "Medium", "Heavy", "Assault" };

            String Typestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Select pilot type",
                  "Pilot Type",
                  JOptionPane.INFORMATION_MESSAGE,
                  null,
                  Types,
                  Types[0]);

            if (Typestr == null || Typestr.isEmpty()) {return;}

            String Sizestr = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Select a pilot size",
                  "Pilot Size",
                  JOptionPane.INFORMATION_MESSAGE,
                  null,
                  Size,
                  Size[0]);
            if (Sizestr == null || Sizestr.isEmpty()) {return;}

            String gunnery = JOptionPane.showInputDialog(client.getMainFrame(), "Gunnery skill", 4);

            if (gunnery == null || gunnery.isEmpty()) {
                return;
            }

            String piloting = JOptionPane.showInputDialog(client.getMainFrame(), "Piloting Mod", 5);

            if (piloting == null || piloting.isEmpty()) {
                return;
            }

            String skills = JOptionPane.showInputDialog(client.getMainFrame(), "Skills Mod (comma delimited)");

            if (skills == null) {
                return;
            }

            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                  "c createpilot#" +
                                  userName +
                                  "#" +
                                  gunnery +
                                  "#" +
                                  piloting +
                                  "#" +
                                  Typestr +
                                  "#" +
                                  Sizestr +
                                  "#" +
                                  skills);
        }
        if (command.equals("VPC") && st.hasMoreElements()) {

            userName = st.nextToken();
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c ViewPlayerParts#" + userName);
        }
        if (command.equals("RPC") && st.hasMoreElements()) {

            userName = st.nextToken();
            String partName = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Part?",
                  "Part Name",
                  JOptionPane.OK_OPTION,
                  null,
                  null,
                  "");
            if (partName == null || partName.isEmpty()) {return;}

            String amount = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Amount",
                  "Amount To Remove",
                  JOptionPane.OK_OPTION,
                  null,
                  null,
                  "0");
            if (amount == null || amount.isEmpty()) {return;}
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c RemoveParts#" + userName + "#" + partName + "#" + amount);
        }
        if (command.equals("APC") && st.hasMoreElements()) {

            userName = st.nextToken();
            String partName = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Part?",
                  "Part Name",
                  JOptionPane.OK_OPTION,
                  null,
                  null,
                  "");
            if (partName == null || partName.isEmpty()) {return;}

            String amount = (String) JOptionPane.showInputDialog(client.getMainFrame(),
                  "Amount",
                  "Amount To Add",
                  JOptionPane.OK_OPTION,
                  null,
                  null,
                  "0");
            if (amount == null || amount.isEmpty()) {return;}
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c AddParts#" + userName + "#" + partName + "#" + amount);
        }
        if (command.equals("SAPC") && st.hasMoreElements()) {

            userName = st.nextToken();

            int result = JOptionPane.showConfirmDialog(client.getMainFrame(),
                  "Are you sure you want to strip " + userName + "'s parts?");
            if (result == JOptionPane.YES_OPTION) {
                client.sendChat(IClient.CAMPAIGN_PREFIX + "c StripAllPartsCache#" + userName + "#CONFIRM");
            }
        }

    }

}// end ModeratorPopupMenu class
