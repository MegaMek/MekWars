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

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PI extends Command {

    /**
     * @param client
     */
    public PI(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {

        java.util.StringTokenizer st = decode(input);
        String task = st.nextToken();
        CUser user = null;
        if (task.equals("PL")) {
            while (st.hasMoreTokens()) {
                user = (CUser) client.getUser(st.nextToken());
                if (user != null) {
                    user.setCampaignData(client, st.nextToken());
                }
            }
        } else if (task.equals("DA")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null) {
                user.setCampaignData(client, st.nextToken());
                if (user.getName().equalsIgnoreCase(client.getPlayer().getName())) {
                    client.getMainFrame().enableMenu();
                }
            }
        } else if (task.equals("ChangeStatusCommand")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null) {
                user.setStatus(Integer.parseInt(st.nextToken()));
            }
        } else if (task.equals("FT")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setFluff(st.nextToken());
            }
        } else if (task.equals("SSN")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setSubFactionName(st.nextToken());
            }
        } else if (task.equals("EX")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setExp(Integer.parseInt(st.nextToken()));
            }
        } else if (task.equals("RA")) {
            user = (CUser) client.getUser(st.nextToken());
            if (user != null && st.hasMoreTokens()) {
                user.setRating(Float.parseFloat(st.nextToken()));
            }
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);
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
