/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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
package mekwars.common.gui.dialogs.buildtableviewer;

import java.io.Serial;
import javax.swing.JDialog;

import mekwars.common.VerticalLayout;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.TablePanel;

/**
 * A JDialog displaying a set of selectable build tables
 *
 * @author Spork
 *
 */
public class BuildTableViewer extends JDialog implements Runnable {

    @Serial
    private static final long serialVersionUID = 4541668930226551934L;
    javax.swing.JFrame mainframe;
    IClient client;

    /**
     * Constructor to create a new viewer
     *
     * @param mainframe the main display panel in the client
     * @param c         the client
     */
    public BuildTableViewer(javax.swing.JFrame mainframe, IClient c) {
        //JOptionPane.showMessageDialog(null, "Blah");
        this.setLayout(new VerticalLayout(5));
        this.mainframe = mainframe;
        this.client = c;
    }

    /**
     * Creates the viewer in a new Thread
     */
    @Override
    public void run() {
        if (client.getUserLevel() >= client.getData().getAccessLevel("AdminRequestBuildTable")) {
            client.sendChat(String.format("%sc AdminRequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        } else if (client.getUserLevel() >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(String.format("%sc RequestBuildTable#list#true", IClient.CAMPAIGN_PREFIX));
        }
        client.setWaiting(true);
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        initComponents();
    }

    /**
     * Build the display
     */
    public void initComponents() {
        SelectorPanel selectorPanel = new SelectorPanel(client);
        TablePanel tablePanel = new TablePanel(this, selectorPanel, client);
        add(selectorPanel);
        add(tablePanel);

        this.setResizable(true);
        this.pack();
        this.setLocationRelativeTo(mainframe);
        this.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        this.setVisible(true);
    }

    /**
     * Redraw the window
     */
    public void refresh() {
        this.pack();
    }
}
