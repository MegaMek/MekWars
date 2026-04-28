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
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import mekwars.admin.dialog.OpFlagSelectionDialog;
import mekwars.admin.dialog.PlanetEditorDialog;
import mekwars.common.CampaignData;
import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMapPanel;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.gui.dialogs.PlanetNameDialog;

public class AdminMapPopupMenu extends JMenu {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 1L;
    //variables
    private IClient client;
    private InnerStellarMap isMap;
    private int xCord;
    private int yCord;
    private CMapPanel mp;
    private String pname;
    private Planet pplanet;
    private int userLevel = 0;

    public AdminMapPopupMenu() {
        super("Administration");
    }

    public void createMenu(IClient client, InnerStellarMap ISMap, Integer xCord, Integer yCord, Planet PPlanet) {

        //save params
        this.client = client;
        this.isMap = ISMap;
        this.xCord = xCord;
        this.yCord = yCord;
        this.pname = PPlanet.getName();
        pplanet = PPlanet;
        userLevel = this.client.getUser(this.client.getUsername()).getUserlevel();

        //save the underlying CMapPanel
        mp = isMap.getMapPanel();

        //start formatting
        JMenuItem item;//holder

        item = new JMenuItem("Rename Planet");
        item.addActionListener(ex -> {
            Planet cPlanet = pplanet;
            String newName = JOptionPane.showInputDialog(STR."Rename \{cPlanet.getName()} to:");
            if (newName != null && !newName.trim().isEmpty()) {
                AdminMapPopupMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}adminrenameplanet \{cPlanet.getId()}#\{cPlanet.getName()}#\{newName}");
                AdminMapPopupMenu.this.client.refreshData();
                mp.repaint();
            }
        });
        if (userLevel >= this.client.getData().getAccessLevel("AdminRenamePlanet")) {
            this.add(item);
        }

        item = new JMenuItem("Vertigos: Edit Planet");
        item.addActionListener(ex -> {
            Planet cPlanet = pplanet;
            new PlanetEditorDialog(AdminMapPopupMenu.this.client, cPlanet.getName(), cPlanet.getId());
        });
        if (userLevel >= this.client.getData().getAccessLevel("SetAdvancedPlanetTerrain")
                  && userLevel >= this.client.getData().getAccessLevel("AdminRemovePlanetOwnership")
                  && userLevel >= this.client.getData().getAccessLevel("AdminDestroyFactory")
                  && userLevel >= this.client.getData().getAccessLevel("AdminDestroyTerrain")
                  && userLevel >= this.client.getData().getAccessLevel("AdminUpdatePlanetOwnership")
                  && userLevel >= this.client.getData().getAccessLevel("AdminCreateFactory")
                  && userLevel >= this.client.getData().getAccessLevel("AdminCreateTerrain")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetBoardSize")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetGravity")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetMapSize")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetTemperature")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetVacuum")
                  && userLevel >= this.client.getData().getAccessLevel("AdminMovePlanet")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetOriginalOwner")
                  && userLevel >= this.client.getData().getAccessLevel("AdminSave")) {this.add(item);}

        item = new JMenuItem("Move Planet Here");
        item.addActionListener(ex -> {
            PlanetNameDialog pnd = new PlanetNameDialog(AdminMapPopupMenu.this.client, "Select a Planet", null);

            pnd.setVisible(true);

            String planet = pnd.getPlanetName();

            if (planet != null) {
                AdminMapPopupMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c adminmoveplanet#\{planet}#\{AdminMapPopupMenu.this.xCord}#\{AdminMapPopupMenu.this.yCord}");
                AdminMapPopupMenu.this.client.refreshData();
                mp.repaint();
            }
        });

        if (userLevel >= this.client.getData().getAccessLevel("adminmoveplanet")) {this.add(item);}

        item = new JMenuItem("Create Planet");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent ex) {
                String planetName = JOptionPane.showInputDialog(AdminMapPopupMenu.this.client.getMainFrame(),
                      "Planet Name?");
                if (planetName == null || planetName.isEmpty()) {return;}

                AdminMapPopupMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admincreateplanet#\{planetName}#\{AdminMapPopupMenu.this.xCord}#\{AdminMapPopupMenu.this.yCord}");
                AdminMapPopupMenu.this.client.refreshData();
                mp.repaint();
                int id = CampaignData.cd.getPlanetByName(planetName).getId();
                new PlanetEditorDialog(AdminMapPopupMenu.this.client, planetName, id);
            }
        });
        if (userLevel >= this.client.getData().getAccessLevel("AdminCreatePlanet")) {this.add(item);}

        item = new JMenuItem("Destroy Planet");
        item.addActionListener(ex -> {
            int result = JOptionPane.showConfirmDialog(new JFrame(),
                  "Are you Sure you want to Destroy this planet?");
            if (result == JOptionPane.YES_OPTION) {
                AdminMapPopupMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c admindestroyplanet#\{pname}");
                AdminMapPopupMenu.this.client.refreshData();
                mp.repaint();
            }
        });
        if (userLevel >= this.client.getData().getAccessLevel("AdminDestroyPlanet")) {this.add(item);}

        item = new JMenuItem("Set Planet Op Flags");
        item.addActionListener(ex -> {
            OpFlagSelectionDialog OFSD = new OpFlagSelectionDialog(AdminMapPopupMenu.this.client,
                  "Select Flags(s)!");
            OFSD.setVisible(true);

            Object[] flags = OFSD.getCommandName();
            OFSD.setVisible(false);

            if (flags == null || flags.length == 0) {return;}

            StringBuilder results = new StringBuilder();

            for (Object flag : flags) {
                String value = (String) flag;

                for (String key : AdminMapPopupMenu.this.client.getData().getPlanetOpFlags().keySet()) {
                    if (value.equals(AdminMapPopupMenu.this.client.getData().getPlanetOpFlags().get(key))) {
                        results.append(key);
                        results.append("#");
                        break;
                    }
                }
            }
            AdminMapPopupMenu.this.client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c AdminSetPlanetOpFlags#\{pname}#\{results.toString()}");
            AdminMapPopupMenu.this.client.refreshData();
            mp.repaint();
        });
        if (userLevel >= this.client.getData().getAccessLevel("AdminSetPlanetOpFlags")) {this.add(item);}

    }//end constructor

}//end AdminMapPopup
