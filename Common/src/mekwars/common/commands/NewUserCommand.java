/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.util.StringTokenizer;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side handler for the {@code NewUserCommand} protocol message, sent by the server whenever a user
 * connects, joins the chat room, or (per the in-method comment) is being renamed. Executing it (re)adds the user
 * to the client's local user list, refreshes the user-list GUI, and optionally prints a "user joined" notice with
 * a sound effect.
 *
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
     * Parses the new/changed user's serialized data into a {@link CUser}, then removes any existing entries for
     * that username from the client's user list before adding the freshly parsed one back in — ensuring there is
     * always exactly one up-to-date entry per name (this same command is reused both for a genuine new
     * connection and for reflecting a username change, per the in-line comment).
     * <p>
     * Dedicated (headless) clients return immediately after the list update, skipping all GUI/chat/sound work.
     * For interactive clients: if the new user is invisible and outranks (has a higher user level than) the
     * local client's own user, the user-list GUI is still refreshed but no join announcement follows. Users whose
     * name starts with {@code "[Dedicated]"} are skipped entirely (not even a GUI refresh in that branch).
     * Otherwise, if the payload has an extra token beyond the user data (again, distinguishing a true "new user"
     * event from a rename), a "user joined" chat line is built (optionally annotated with country and a
     * timestamp per client config), added to chat if {@code SHOW_ENTER_AND_EXIT} is enabled, and a join sound is
     * played via the {@code SOUND_ON_JOIN} config parameter. The user-list GUI is refreshed at the very end of
     * this path.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {

        StringTokenizer stringTokenizer = decode(input);

        CUser newUser = new CUser(stringTokenizer.nextToken());

        //Check the Users and remove the User
        CUser user = (CUser) client.getUser(newUser.getName());
        //delete every instance of that user from the list
        while (client.getUsers().remove(user)) {
            user = (CUser) client.getUser(newUser.getName());
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
        if (stringTokenizer.hasMoreTokens()) {

            String name = newUser.getName();

            if (!newUser.getCountry().equals("unknown")) {
                name += String.format(" (%s)", newUser.getCountry());
            }

            String toSend = String.format("<font color=\"maroon\">>> Enter %s</font>", name);

            if (client.getConfig().isParam("TIMESTAMP")) {
                toSend = client.getShortTime() + toSend;
            }

            if (client.getConfig().isParam("SHOW_ENTER_AND_EXIT")) {
                client.addToChat(toSend);
            }

            //play join sound if one is configured
            client.doPlaySound(client.getConfigParam("SOUND_ON_JOIN"));
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
