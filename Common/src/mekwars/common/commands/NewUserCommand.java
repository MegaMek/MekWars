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

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class NewUserCommand extends Command {

    /**
     *
     */
    public NewUserCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {

        StringTokenizer st = decode(input);

        CUser newUser = CUser(st.nextToken());

        //Check the Users and remove the User
        CUser user = client.getUser(newUser.getName());
        //delete every instance of that user from the list
        while (client.getUsers().remove(user)) {
            user = client.getUser(newUser.getName());
        }

        client.getUsers().add(newUser);

        if (client.isDedicated()) {
            return;
        }

        if (newUser.isInvisible() &&
                  newUser.getUserLevel() > client.getUser(client.getPlayer().getName()).getUserLevel()) {
            client.refreshGUI(IClient.REFRESH_USERLIST);
            return;
        }

        if (newUser.getName().startsWith("[Dedicated]")) {
            return;
        }

        //Print an entry message if the information is followed by NEW (NewUserCommand and UserGoneCommand are used for name changing, too)
        if (st.hasMoreTokens()) {

            String name = newUser.getName();

            if (!newUser.getCountry().equals("unknown")) {
                name += " (" + newUser.getCountry() + ")";
            }

            String toSend = "<font color=\"maroon\">>> Enter " + name + "</font>";

            if (client.getConfig().isParam("TIMESTAMP")) {
                toSend = client.getShortTime() + toSend;
            }

            if (client.getConfig().isParam("SHOWENTERANDEXIT")) {
                client.addToChat(toSend);
            }

            //play join sound, if one is configured
            client.doPlaySound(client.getConfigParam("SOUNDONJOIN"));
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
