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

import java.io.Serial;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeListener;

import mekwars.common.CampaignData;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CMainFrame;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.util.MWLogger;

/**
 * Class used to display Stellar InnerStellarMap in GUI
 * <p>
 * The map is drawn online at demand. Hope the speed is ok.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class CMapPanel extends JPanel {

    /**
     *
     */
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

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                map.setSize(e.getComponent().getSize());
                slider.setBounds(e.getComponent().getWidth() - 155, 5, 150, slider.getPreferredSize().height);
            }

            @Override
            public void componentShown(java.awt.event.ComponentEvent e) {
                map.setSize(e.getComponent().getSize());
                slider.setBounds(e.getComponent().getWidth() - 155, 5, 150, slider.getPreferredSize().height);
            }
        });

        //if the map is visible, select the correct planet last(since this involves several updates)
        if (client.getConfig().isParam("MAPTABVISIBLE")) {
            try {
                map.activate(client.getData().getPlanet(map.getConf().getPlanetID()));
            } catch (Exception ex) {
                MWLogger.errLog(ex);
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

    private class ZoomSlider extends JSlider implements ChangeListener {
        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -2214264904474265394L;

        ZoomSlider() {
            super(HORIZONTAL, map.getConf().getReverseScaleMin(), map.getConf().getReverseScaleMax(),
                  map.getConf().getReverseScaleMin() +
                        (map.getConf().getReverseScaleMax() - map.getConf().getReverseScaleMin()) / 2);
            addChangeListener(this);
        }

        public void stateChanged(javax.swing.event.ChangeEvent e) {
            map.setScale(50 / (double) getValue());
            CMapPanel.this.repaint();
        }
    }
}
