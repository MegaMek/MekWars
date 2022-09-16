/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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

package client.gui.commands;

import javax.swing.AbstractAction;

import client.MWClient;
import common.campaign.clientutils.protocol.CConnector;

/**
 * Abstract class for GUI Commands
 */

public abstract class CGUICommand extends AbstractAction implements IGUICommand {
    /**
     * 
     */
    private static final long serialVersionUID = -8448209123971531879L;
    String name = "";
    String alias = "";
    String command = ""; // name of command sent to server (lyrisoft chat layer)
    String subcommand = ""; // name of command sent to server (MEKWARS layer)
    String GUIprefix = "";
    String delimiter = "";
    String prefix = "";
    MWClient mwclient;
    CConnector Connector;

    public CGUICommand(MWClient mwclient) {
        super();
        this.mwclient = mwclient;
        Connector = mwclient.getConnector();
        GUIprefix = MWClient.GUI_PREFIX;
        delimiter = MWClient.PROTOCOL_DELIMITER;
        prefix = MWClient.PROTOCOL_PREFIX;
    }

    public boolean check(String tname) {
        if (tname.startsWith(GUIprefix)) {
            tname = tname.substring(GUIprefix.length());
        }
        return (name.equals(tname) || alias.equals(tname));
    }

    // execute command
    public boolean execute(String input) {
        return true;
    }

    // echo command in GUI
    protected void echo(String input) {
    }

    // send command to server (directly through connector)
    protected void send(String input) {
        Connector.send(prefix + command + delimiter + input);
    }

    // send command to server (directly through connector)
    protected void send() {
        Connector.send(prefix + command);
    }

    // remove prefix and name/alias from input
    protected String decompose(String input) {
        if (input.startsWith(GUIprefix)) {
            input = input.substring(GUIprefix.length()).trim();
        }
        if (input.startsWith(name)) {
            input = input.substring(name.length()).trim();
        } else if (input.startsWith(alias)) {
            input = input.substring(alias.length()).trim();
        }
        return input;
    }

    public String getName() {
        return name;
    }

    public String getAlias() {
        return alias;
    }

    public boolean isAlias() {
        return (alias != null && !alias.equals(""));
    }
}