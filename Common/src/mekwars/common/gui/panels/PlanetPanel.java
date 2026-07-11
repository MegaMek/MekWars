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

import java.awt.Color;
import java.io.Serial;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.StringUtils;

/**
 * Draws statistic for a specific CPlanet in the stellar map
 *
 * @author Imi
 */

public class PlanetPanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -2092699724451823560L;
    private final CMapPanel map;
    private final IClient client;
    private final JLabel name;
    //private JLabel position;
    private final JLabel influence;
    //private JLabel terrain;
    private final JLabel unitFactories;
    private Planet planet;

    PlanetPanel(CMapPanel panel, IClient client) {
        this.client = client;

        setForeground(Color.WHITE);
        this.map = panel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 5, 5, 5));
        name = new WhiteLabel("Name: ???");

        add(name);

        influence = new WhiteLabel("Owner: ???");
        add(influence);
        unitFactories = new WhiteLabel("Factories: ???");
        setOpaque(false);
        add(unitFactories);
    }
    //private JLabel warehouses;

    public void update(Planet planet) {
        this.planet = planet;
        name.setText(String.format("Name: %s", planet.getName()));
        // influence
        StringBuilder stringBuilder = new StringBuilder("<html><body>Owner: ");

        Integer houseID = planet.getInfluence().getOwner();
        House faction = null;

        if (houseID != null) {
            faction = client.getData().getHouse(houseID);
        }

        Color color;
        String planetOwner;
        if (faction == null ||
                  planet.getInfluence().getInfluence(faction.getId()) < client.getMinPlanetOwnerShip(planet)) {
            color = StringUtils.html2Color(client.getServerConfigs("DisputedPlanetColor"));
            planetOwner = "Disputed";
        } else {
            color = StringUtils.html2Color(faction.getHouseColor());
            planetOwner = faction.getName();
        }
        stringBuilder.append("<font color=");
        stringBuilder.append(StringUtils.color2html(map.getMap().adjustColor(color))).append(">");
        stringBuilder.append("<b>").append(planetOwner).append("</b></font></body></html>");
        this.influence.setText(stringBuilder.toString());
        unitFactories.setText(String.format("Factories: %s", planet.getFactoryCount()));

        map.getMapControl().setBounds(0, 0, 2000, 2000);
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

    private static class WhiteLabel extends JLabel {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -8911863558331233209L;

        WhiteLabel(String name) {
            super(name);
            setOpaque(false);
            setForeground(java.awt.Color.WHITE);
        }
    }
}
