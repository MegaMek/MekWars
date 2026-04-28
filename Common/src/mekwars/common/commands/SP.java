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

package mekwars.common.commands;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class SP extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public SP(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        if (client.getConfig().isParam("POPUPONMESSAGE")) {client.showInfoWindow(st.nextToken());} else {
            client.addToChat(st.nextToken(), client.gui.CCommPanel.CHANNEL_MISC);
        }
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {
        
    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
