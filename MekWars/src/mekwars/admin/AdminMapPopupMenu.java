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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import admin.dialog.OpFlagSelectionDialog;
import admin.dialog.PlanetEditorDialog;
import client.MWClient;
import client.gui.CMapPanel;
import client.gui.InnerStellarMap;
import client.gui.dialog.PlanetNameDialog;
import common.CampaignData;
import common.Planet;

public class AdminMapPopupMenu extends JMenu {
	
	/**
     * 
     */
    private static final long serialVersionUID = 1L;
    //variables
	private MWClient mwclient;
	private InnerStellarMap isMap;
	private int xcoord;
	private int ycoord;
	private CMapPanel mp;
	private String pname;
	private Planet pplanet;
	private int userLevel = 0;
    
	public AdminMapPopupMenu() {
		super("Administration");
	}
	
	public void createMenu(MWClient client, InnerStellarMap ISMap, Integer Xcoord, Integer Ycoord, Planet PPlanet) {
		
		//save params
		this.mwclient = client;
		this.isMap = ISMap;
		this.xcoord = Xcoord.intValue(); 
		this.ycoord = Ycoord.intValue();
		this.pname = PPlanet.getName();
		pplanet = PPlanet;
        userLevel = this.mwclient.getUser(this.mwclient.getUsername()).getUserlevel();
        
		//save the underlying CMapPanel
		mp = isMap.getMapPanel();
		
		//start formatting
		JMenuItem item;//holder
		
		item = new JMenuItem("Rename Planet");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ex) {
				Planet cPlanet = pplanet;
				String newName = (String)JOptionPane.showInputDialog("Rename " + cPlanet.getName() + " to:");
				if (newName != null && newName.trim().length() > 0) {
					mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "adminrenameplanet " + cPlanet.getId() + "#" + cPlanet.getName() + "#" + newName);
					mwclient.refreshData();
					mp.repaint();
				}
			}
		});
		if (userLevel >= mwclient.getData().getAccessLevel("AdminRenamePlanet")) {
			this.add(item);
		}
		
        item = new JMenuItem("Vertigos: Edit Planet");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ex) {
				Planet cPlanet = pplanet;
				new PlanetEditorDialog(mwclient, cPlanet.getName(), cPlanet.getId());
			}
		});
        if ( userLevel >= mwclient.getData().getAccessLevel("SetAdvancedPlanetTerrain") 
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminRemovePlanetOwnership")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminDestroyFactory")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminDestroyTerrain")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminUpdatePlanetOwnership")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminCreateFactory")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminCreateTerrain")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetBoardSize")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetGravity")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetMapSize")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetTemperature")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetVacuum")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminMovePlanet")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetOriginalOwner")
        		&& userLevel >= mwclient.getData().getAccessLevel("AdminSave"))
            this.add(item);

        item = new JMenuItem("Move Planet Here");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ex) {

                PlanetNameDialog pnd = new PlanetNameDialog(mwclient,"Select a Planet",null);

                pnd.setVisible(true);

                String planet = pnd.getPlanetName();

                if ( planet != null ){
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminmoveplanet#"+ planet +"#"+xcoord+"#"+ycoord);
                    mwclient.refreshData();
                    mp.repaint();
                }
            }
        });
        
        if ( userLevel >= mwclient.getData().getAccessLevel("adminmoveplanet") )
            this.add(item);
        
		item = new JMenuItem("Create Planet");
		item.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent ex) {
				String planetName = JOptionPane.showInputDialog(mwclient.getMainFrame(),"Planet Name?");
				if ( planetName == null || planetName.length() == 0 )
					return;
				
				mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admincreateplanet#"+planetName+"#"+xcoord+"#"+ycoord);
				mwclient.refreshData();
				mp.repaint();
				int id = CampaignData.cd.getPlanetByName(planetName).getId();
				new PlanetEditorDialog(mwclient, planetName, id);
			}
		});
        if ( userLevel >= mwclient.getData().getAccessLevel("AdminCreatePlanet") )
            this.add(item);
		
		item = new JMenuItem("Destroy Planet");
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ex) {
				int result = JOptionPane.showConfirmDialog(new JFrame(),"Are you Sure you want to Destroy this planet?");
				if (result == JOptionPane.YES_OPTION)
				{
					mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c admindestroyplanet#"+ pname);
					mwclient.refreshData();
					mp.repaint();
				}
			}
		});
        if ( userLevel >= mwclient.getData().getAccessLevel("AdminDestroyPlanet") )
               this.add(item);
		
        item = new JMenuItem("Set Planet Op Flags");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ex) {
                
                OpFlagSelectionDialog OFSD = new OpFlagSelectionDialog(mwclient,"Select Flags(s)!");
                OFSD.setVisible(true);
                
                Object[] flags = OFSD.getCommandName();
                OFSD.setVisible(false);
                
                if (flags == null || flags.length == 0)
                    return;
                
                StringBuffer results = new StringBuffer();
                
                for ( int pos  = 0; pos < flags.length; pos++ ){
                    String value = (String)flags[pos];
                    
                    for ( String key : mwclient.getData().getPlanetOpFlags().keySet() ){
                        if ( value.equals(mwclient.getData().getPlanetOpFlags().get(key)) ){
                            results.append(key);
                            results.append("#");
                            break;
                        }
                    }
                }
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSetPlanetOpFlags#"+ pname +"#"+results.toString());
                mwclient.refreshData();
                mp.repaint();
            }
        });
        if ( userLevel >= mwclient.getData().getAccessLevel("AdminSetPlanetOpFlags") )
            this.add(item);

	}//end constructor
	
}//end AdminMapPopup