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

package mekwars.common.gui;

/**
 * Class to display simple rules tab
 *
 * @author Salient
 */

public class CRulesPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = 5547551469995402891L;

    client.MWClient mwclient;

    public CRulesPanel(client.MWClient client) {
        mwclient = client;

        setLayout(new java.awt.BorderLayout());
        javax.swing.JEditorPane editorPane = new javax.swing.JEditorPane();
        editorPane.setEditable(false);
        String rulesLocation = mwclient.getserverConfigs("Rules_Location");
        java.net.URL rulesURL = CRulesPanel.class.getResource(rulesLocation);

        if (rulesURL != null) {
            try {editorPane.setPage(rulesURL);} catch (java.io.IOException e) {
                System.err.println("Bad URL: " + rulesURL);
            }
        } else {System.err.println("Couldn't find: ServerRules.html");}

        javax.swing.JScrollPane editorScrollPane = new javax.swing.JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(javax.swing.JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new java.awt.Dimension(250, 145));
        editorScrollPane.setMinimumSize(new java.awt.Dimension(10, 10));

        add(editorScrollPane);
    }
}
