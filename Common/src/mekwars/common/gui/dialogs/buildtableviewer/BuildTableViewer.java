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
    /** The client's main window, used only to center this dialog over it (see {@link #initComponents()}). */
    javax.swing.JFrame mainframe;
    /** The MekWars client, used to query permissions/data and to request build table listings from the server. */
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
     * Creates the viewer in a new Thread. Sends a request to the server for the build table listing — using the
     * admin variant of the command if the user's level meets the "AdminRequestBuildTable" access requirement,
     * otherwise the regular "RequestBuildTable" command if the user at least meets that lower requirement (if
     * neither threshold is met, no request is sent at all, and the loop below will spin until some other code
     * clears the waiting flag). Then marks the client as waiting and busy-waits, polling {@link IClient#isWaiting()}
     * every 100ms on this thread, until the server's response (elsewhere in the codebase) clears the flag via
     * {@link IClient#setWaiting(boolean)}. Once unblocked, builds the dialog's contents via
     * {@link #initComponents()}.
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
