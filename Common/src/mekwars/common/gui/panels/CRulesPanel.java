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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.Serial;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Class to display a simple rules tab
 *
 * @author Salient
 */

public class CRulesPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 5547551469995402891L;

    IClient client;

    public CRulesPanel(IClient client) {
        this.client = client;

        setLayout(new BorderLayout());
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        String rulesLocation = this.client.getServerConfigs("Rules_Location");
        java.net.URL rulesURL = CRulesPanel.class.getResource(rulesLocation);

        if (rulesURL != null) {
            try {editorPane.setPage(rulesURL);} catch (java.io.IOException e) {
                System.err.println(STR."Bad URL: \{rulesURL}");
            }
        } else {
            System.err.println("Couldn't find: ServerRules.html");
        }

        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(250, 145));
        editorScrollPane.setMinimumSize(new Dimension(10, 10));

        add(editorScrollPane);
    }
}
