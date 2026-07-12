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
 * A simple, read-only "Rules" tab in the MekWars client. It loads an HTML rules document (whose classpath-relative
 * location is supplied by the server via the {@code Rules_Location} server config) into a non-editable
 * {@link JEditorPane} wrapped in a scroll pane, so the player can read the campaign's house rules without leaving
 * the client.
 *
 * @author Salient
 */

public class CRulesPanel extends JPanel {
    @Serial
    private static final long serialVersionUID = 5547551469995402891L;

    /** Connection/session handle used to look up server-provided configuration (here, the rules document path). */
    IClient client;

    /**
     * Builds the panel and immediately attempts to load and display the rules document referenced by the
     * {@code Rules_Location} server config. If the resource cannot be found or fails to load, an error is logged
     * to {@code System.err} and the panel is left with an empty editor pane (no exception is thrown).
     *
     * @param client client used to fetch the {@code Rules_Location} server config value
     */
    public CRulesPanel(IClient client) {
        this.client = client;

        setLayout(new BorderLayout());
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        String rulesLocation = this.client.getServerConfigs("Rules_Location");
        // Resource is looked up relative to this class on the classpath (e.g. bundled with the client jar).
        java.net.URL rulesURL = CRulesPanel.class.getResource(rulesLocation);

        if (rulesURL != null) {
            try {editorPane.setPage(rulesURL);} catch (java.io.IOException e) {
                System.err.println(String.format("Bad URL: %s", rulesURL));
            }
        } else {
            // NOTE: message is hardcoded to "ServerRules.html" regardless of the actual configured rulesLocation.
            System.err.println("Couldn't find: ServerRules.html");
        }

        JScrollPane editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(250, 145));
        editorScrollPane.setMinimumSize(new Dimension(10, 10));

        add(editorScrollPane);
    }
}
