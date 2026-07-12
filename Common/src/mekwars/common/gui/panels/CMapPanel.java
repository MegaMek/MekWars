/*
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.gui.panels;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.Serial;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import megamek.logging.MMLogger;
import mekwars.common.CampaignData;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.InnerStellarMap;

/**
 * The "Map" tab of the MekWars client: hosts the interactive stellar map ({@link InnerStellarMap}), a zoom slider,
 * and an overlay panel showing stats for the currently-selected planet ({@link PlanetPanel}). This class itself
 * does no drawing; it lays out and wires together those three sub-components using absolute positioning
 * (a {@code null} layout manager), with bounds recalculated on resize via a {@link ComponentAdapter}.
 * <p>
 * The map is drawn online at demand. Hope the speed is ok.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class CMapPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CMapPanel.class);

    @Serial
    private static final long serialVersionUID = 5547551465585402891L;
    /**
     * The main map
     */
    private final InnerStellarMap map;
    /**
     * The zoom slider
     */
    private final JSlider slider;
    /**
     * Statistics of the current selected planet.
     */
    private final PlanetPanel planetPanel;
    /**
     * The map control in top left corner
     */
    private final JPanel mapControl;
    /**
     * A vector of all planets to be drawn at demand.
     */
    private final IClient client;

    /**
     * Assembles the map control overlay (containing {@link #planetPanel} and {@link #slider}) plus the
     * {@link InnerStellarMap} itself, wires a resize listener that keeps the map and slider bounds in sync with
     * this panel's size, and — if the map tab is configured as initially visible — activates (selects) the
     * previously-remembered planet from client config.
     *
     * @param client    used to read map/planet configuration and data
     * @param mainFrame the top-level client window, passed through to the map for dialog parenting etc.
     * @param xsize     unused; the panel uses a {@code null} layout and is sized by its container instead
     * @param ysize     unused; the panel uses a {@code null} layout and is sized by its container instead
     */
    public CMapPanel(IClient client, CMainFrame mainFrame, int xsize, int ysize) {
        this.client = client;
        setLayout(null);
        mapControl = new javax.swing.JPanel();
        mapControl.setOpaque(false);
        mapControl.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(mapControl);

        mapControl.setLayout(new javax.swing.BoxLayout(mapControl, javax.swing.BoxLayout.Y_AXIS));

        // ISMap
        map = new InnerStellarMap(this, this.client, mainFrame);

        // planet info
        planetPanel = new PlanetPanel(this, this.client);
        mapControl.add(planetPanel);
        planetPanel.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        planetPanel.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);

        // zoom slider
        slider = new CMapPanel.ZoomSlider();
        slider.setValue((int) Math.round(50 / map.getConf().getScale()));
        slider.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);
        slider.setAlignmentY(java.awt.Component.TOP_ALIGNMENT);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(slider.getMaximum() / 5);
        slider.setMinorTickSpacing(slider.getMaximum() / 10);
        slider.setPaintTicks(true);
        add(slider);
        add(map);

        // Keep the map filling this panel and the slider pinned to the top-right corner as the panel is resized
        // or first shown.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                map.setSize(componentEvent.getComponent().getSize());
                slider.setBounds(componentEvent.getComponent().getWidth() - 155,
                      5,
                      150,
                      slider.getPreferredSize().height);
            }

            @Override
            public void componentShown(ComponentEvent componentEvent) {
                map.setSize(componentEvent.getComponent().getSize());
                slider.setBounds(componentEvent.getComponent().getWidth() - 155,
                      5,
                      150,
                      slider.getPreferredSize().height);
            }
        });

        //if the map is visible, select the correct planet last (since this involves several updates)
        if (client.getConfig().isParam("MAP_TAB_VISIBLE")) {
            try {
                map.activate(client.getData().getPlanet(map.getConf().getPlanetID()));
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to access planet. {}", ex.getLocalizedMessage());
            }
        }

    }

    /**
     * @return Returns the data.
     */
    public CampaignData getData() {
        return client.getData();
    }

    /**
     * @return Returns the slider.
     */
    public javax.swing.JSlider getSlider() {
        return slider;
    }

    /**
     * @return Returns the planetPanel.
     */
    public PlanetPanel getPPanel() {
        return planetPanel;
    }

    /**
     * @return Returns the mapControl.
     */
    public JPanel getMapControl() {
        return mapControl;
    }

    /**
     * @return Returns the map.
     */
    public InnerStellarMap getMap() {
        return map;
    }

    /**
     * A {@link JSlider} that controls the stellar map's zoom level. Slider values are inversely related to the
     * map's scale factor (higher slider value = more zoomed in), via {@code scale = 50 / value}; range and default
     * value/midpoint come from the map's configuration ({@code getReverseScaleMin()}/{@code getReverseScaleMax()}).
     */
    private class ZoomSlider extends JSlider implements ChangeListener {
        /** Serialization version identifier. */
        @Serial
        private static final long serialVersionUID = -2214264904474265394L;

        /** Builds the slider with range and initial value derived from the map's configured zoom limits. */
        ZoomSlider() {
            super(HORIZONTAL, map.getConf().getReverseScaleMin(), map.getConf().getReverseScaleMax(),
                  map.getConf().getReverseScaleMin() +
                        (map.getConf().getReverseScaleMax() - map.getConf().getReverseScaleMin()) / 2);
            addChangeListener(this);
        }

        /**
         * Applies the slider's current value to the map's scale (inversely: {@code 50 / value}) and repaints the
         * enclosing {@link CMapPanel}.
         *
         * @param e the slider change event (unused; current value is read via {@link #getValue()})
         */
        public void stateChanged(javax.swing.event.ChangeEvent e) {
            map.setScale(50 / (double) getValue());
            CMapPanel.this.repaint();
        }
    }
}
