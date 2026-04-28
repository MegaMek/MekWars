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

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class BM extends Command {

    public BM(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer st = decode(input);
        String cmd = st.nextToken();

        if (!st.hasMoreTokens()) {
            return;
        } else if (cmd.equals("AD")) {
            client.getCampaign().setBMData(st.nextToken());
        } else if (cmd.equals("AU")) {
            client.getCampaign().addBMUnit(st.nextToken());
        } else if (cmd.equals("RU")) {
            client.getCampaign().removeBMUnit(st.nextToken());
        } else if (cmd.equals("CU")) {
            client.getCampaign().changeBMUnit(st.nextToken());
        }

        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER_PANEL);
        client.refreshGUI(IClient.REFRESH_BM_PANEL);

    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {
        
    }

    /**
     * @param mwClient
     */
    @Override
    public void setClient(IClient mwClient) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
