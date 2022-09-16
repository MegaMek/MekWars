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

package client.gui;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import client.MWClient;
import common.House;
import common.Planet;
import common.util.StringUtils;

/**
 * Draws statistic for a specific CPlanet in the stellar map
 * @author Imi
 */

class PlanetPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = -2092699724451823560L;
    private CMapPanel map;
    private MWClient mwclient;
    
    private static class WhiteLabel extends JLabel {
        /**
         * 
         */
        private static final long serialVersionUID = -8911863558331233209L;

        WhiteLabel(String name) {
            super(name);
            setOpaque(false);
            setForeground(Color.WHITE);
        }
    }

    private Planet planet;
    private JLabel name;
    //private JLabel position;
    private JLabel influence;
    //private JLabel terrain;
    private JLabel unitFactories;
    //private JLabel warehouses;
    
    PlanetPanel(CMapPanel panel, MWClient client) {
    	mwclient = client;
        setForeground(Color.WHITE);
        this.map = panel;
        setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0,5,5,5));
        name = new WhiteLabel("Name: ???");
        //name.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        //name.setOpaque(false);
        add(name);
       // add(position = new WhiteLabel("Position: ??? x ???"));
        //add(new WhiteLabel("Conquered:"));
        influence = new WhiteLabel("Owner: ???");
        //influence.setBorder(BorderFactory.createEmptyBorder(2,5,5,5));
        add(influence);
        //add(new WhiteLabel("Terrain:"));
        //terrain = new WhiteLabel("");
        //terrain.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
       // add(terrain);
        //add(new WhiteLabel("Factories: "));
        unitFactories = new WhiteLabel("Factories: ???");
        //unitFactories.setBorder(BorderFactory.createEmptyBorder(2,5,5,5));
        setOpaque(false);
        add(unitFactories);
    }
    
    void update(Planet planet) {
        this.planet = planet;
        name.setText("Name: "+planet.getName());
        // influence
        StringBuilder sb = new StringBuilder("<html><body>Owner: ");
        
        Integer houseID = planet.getInfluence().getOwner();
        House faction = null;
        
        if ( houseID != null )
            faction = mwclient.getData().getHouse(houseID);
        
        Color c = Color.GRAY;
        String planetOwner = "Disputed";
        if ( faction == null ||
                planet.getInfluence().getInfluence(faction.getId()) < mwclient.getMinPlanetOwnerShip(planet) ){
            c = StringUtils.html2Color(mwclient.getserverConfigs("DisputedPlanetColor"));
            planetOwner = "Disputed";
        }
        else{    
            c = StringUtils.html2Color(faction.getHouseColor());
            planetOwner = faction.getName();
        }
        sb.append("<font color=");//+faction.getHouseColor()+">");
        sb.append(StringUtils.color2html(map.getMap().adjustColor(c))+">");
        sb.append("<b>"+planetOwner+"</b></font></body></html>");
        this.influence.setText(sb.toString());
       unitFactories.setText("Factories: " + planet.getFactoryCount());
         
        map.getMapControl().setBounds(0,0,2000,2000);
    }
    
    
    /**
     * @return Returns the planet.
     */
    public Planet getPlanet() {
        return planet;
    }
    /**
     * @param planet The planet to set.
     */
    public void setPlanet(Planet planet) {
        this.planet = planet;
    }
}