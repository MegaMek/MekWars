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
package client.gui.dialog.buildtableviewer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import client.MWClient;
import common.VerticalLayout;

/**
 * A JDialog displaying a set of selectable build tables
 * @author Spork
 *
 */
public class BuildTableViewer extends JDialog implements Runnable {

	private static final long serialVersionUID = 4541668930226551934L;
	JFrame mainframe;
	MWClient client;
	
	private SelectorPanel selectorPanel;
	private TablePanel tablePanel;
	
	/**
	 * Constructor to create a new viewer
	 * @param mainframe the main display panel in the client
	 * @param c the client
	 */
	public BuildTableViewer(JFrame mainframe, MWClient c) {
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
            client.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminRequestBuildTable#list#true");
        }
        else if (client.getUserLevel() >= client.getData().getAccessLevel("RequestBuildTable")) {
            client.sendChat(MWClient.CAMPAIGN_PREFIX + "c RequestBuildTable#list#true");
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
		selectorPanel = new SelectorPanel(client);
		tablePanel = new TablePanel(this, selectorPanel, client);
		add(selectorPanel);
		add(tablePanel);
		
		this.setResizable(true);
		this.pack();
		this.setLocationRelativeTo(mainframe);
		this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	/**
	 * Redraw the window
	 */
	public void refresh() {
		this.pack();
	}
}
