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
 * A small info box, shown in the corner of the stellar map ({@link CMapPanel}), that displays summary statistics
 * (name, owning faction, and factory count) for whichever {@link Planet} is currently selected on the map.
 *
 * @author Imi
 */

public class PlanetPanel extends JPanel {

    /** Serialization version identifier. */
    @Serial
    private static final long serialVersionUID = -2092699724451823560L;
    /** The map this panel is attached to; used to look up the {@link mekwars.common.gui.InnerStellarMap}. */
    private final CMapPanel map;
    /** Connection/session handle used to resolve the owning house of the displayed planet. */
    private final IClient client;
    /** Displays the planet's name. */
    private final JLabel name;
    //private JLabel position;
    /** Displays the owning faction (or "Disputed"), colored to match that faction's map color. */
    private final JLabel influence;
    //private JLabel terrain;
    /** Displays the planet's factory count. */
    private final JLabel unitFactories;
    /** The planet currently being displayed; {@code null} (or stale) until {@link #update(Planet)} is called. */
    private Planet planet;

    /**
     * Builds the panel with placeholder ("???") labels; call {@link #update(Planet)} to populate real data once a
     * planet has been selected on the map.
     *
     * @param panel  the owning map panel, used to resolve map coloring
     * @param client used to resolve house/faction data when displaying ownership
     */
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

    /**
     * Refreshes all displayed labels to reflect the given planet: name, factory count, and ownership. Ownership is
     * shown as "Disputed" (in the server-configured disputed color) if there is no owning house, or if the owning
     * house's influence on the planet is below {@link IClient#getMinPlanetOwnerShip(Planet)}; otherwise the owning
     * faction's name is shown in that faction's map color.
     * <p>
     * As a side effect, this also resets the map control panel's bounds to a fixed 2000x2000 box (unrelated to
     * planet display, presumably to ensure the control panel stays large enough to lay out its children).
     *
     * @param planet the planet to display; must be non-null (its name/influence/factory count are read directly)
     */
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

    /** A non-opaque {@link JLabel} with white foreground text, used so labels blend into the map's dark background. */
    private static class WhiteLabel extends JLabel {
        /** Serialization version identifier. */
        @Serial
        private static final long serialVersionUID = -8911863558331233209L;

        /**
         * @param name the initial label text
         */
        WhiteLabel(String name) {
            super(name);
            setOpaque(false);
            setForeground(java.awt.Color.WHITE);
        }
    }
}
