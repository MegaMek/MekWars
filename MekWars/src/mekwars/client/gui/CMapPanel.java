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

import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import client.MWClient;
import common.CampaignData;
import common.util.MWLogger;

/**
 * Class used to display Stellar InnerStellarMap in GUI
 *
 * The map is drawn online at demand. Hope the speed is ok.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class CMapPanel extends JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 5547551465585402891L;
    private class ZoomSlider extends JSlider implements ChangeListener {
        /**
         * 
         */
        private static final long serialVersionUID = -2214264904474265394L;

        ZoomSlider() {
            super(HORIZONTAL, map.conf.reverseScaleMin,map.conf.reverseScaleMax, 
                    map.conf.reverseScaleMin + 
                    (map.conf.reverseScaleMax - map.conf.reverseScaleMin) / 2);
            addChangeListener(this);
        }

        public void stateChanged(ChangeEvent e) {
            map.setScale(50/(double)getValue());
            CMapPanel.this.repaint();
        }
    }

    /**
     * The main map
     */
    private InnerStellarMap map;

    /**
     * The zoom slider
     */
    private JSlider slider;

    /**
     * Statistics of the current selected planet.
     */
    private PlanetPanel planetPanel;

    /**
     * The map control in topleft corner
     */
    private JPanel mapControl;

    /**
     * A vector of all planets to be drawn at demand.
     */
    private MWClient mwclient;

    public CMapPanel(MWClient client, CMainFrame mainFrame, int xsize, int ysize) {
        this.mwclient = client;
        setLayout(null);
        mapControl = new JPanel();
        mapControl.setOpaque(false);
        mapControl.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        add(mapControl);

        mapControl.setLayout(new BoxLayout(mapControl,BoxLayout.Y_AXIS));

        // ISMap
        map = new InnerStellarMap(this,mwclient,mainFrame);

        // planet info
        planetPanel = new PlanetPanel(this, mwclient);
        mapControl.add(planetPanel);
        planetPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        planetPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        // zoom slider
        slider = new ZoomSlider();
        slider.setValue((int)Math.round(50/map.conf.scale));
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);
        slider.setAlignmentY(Component.TOP_ALIGNMENT);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(slider.getMaximum()/5);
        slider.setMinorTickSpacing(slider.getMaximum()/10);
        //slider.createStandardLabels(10);
        //slider.setSnapToTicks(true);
        //slider.setPaintTrack(true);
        slider.setPaintTicks(true);
        add(slider);
        add(map);

        addComponentListener(new ComponentAdapter(){
            @Override
			public void componentResized(ComponentEvent e) {
                map.setSize(e.getComponent().getSize());
                slider.setBounds(e.getComponent().getWidth()-155,5,150,slider.getPreferredSize().height);
            }
            @Override
			public void componentShown(ComponentEvent e) {
                map.setSize(e.getComponent().getSize());
                slider.setBounds(e.getComponent().getWidth()-155,5,150,slider.getPreferredSize().height);
            }
        });

        //if the map is visible, select the correct planet last(since this involves several updates)
        if (client.getConfig().isParam("MAPTABVISIBLE")) {
        	try {
            	map.activate(client.getData().getPlanet(map.conf.planetID));
            } catch (Exception ex) {
            	MWLogger.errLog(ex);
            }
        }
        
    }

    /**
     * @return Returns the data.
     */
    public CampaignData getData() {
        return mwclient.getData();
    }
    /**
     * @return Returns the slider.
     */
    public JSlider getSlider() {
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
}
