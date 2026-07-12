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
 * Client-side handler for the {@code UserGoneCommand} protocol message, sent by the server whenever a user
 * disconnects, leaves the chat room, or (per the comment below) is being renamed. Executing it removes all
 * matching entries for that username from the client's local user list, refreshes the user list GUI, and
 * optionally prints a "user left" notice with a sound effect.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class UserGoneCommand extends Command {

    /**
     *
     */
    public UserGoneCommand(IClient client) {
        super(client);
    }

    /**
     * Parses the departing user's name (and constructs a throwaway {@link CUser} from it purely to reuse its
     * name/invisibility/level accessors) and removes every user list entry matching that name.
     * <p>
     * After the local list is updated and the user-list GUI refreshed, this method decides whether to announce
     * the departure in chat:
     * <ul>
     *     <li>Dedicated (headless) clients always return early — no chat/sound handling.</li>
     *     <li>Invisible users with a higher user level than the local client, and any user whose name starts with
     *     {@code "[Dedicated]"}, are never announced.</li>
     *     <li>Whether a "leave" announcement happens at all is gated on the presence of one more token after
     *     the username in the payload — the comment in the code explains this token distinguishes a genuine
     *     "Gone" (user left) event from a same-payload rename, though this method does not otherwise use the
     *     token's value.</li>
     * </ul>
     * When an announcement fires, an HTML-colored "Exit" message is optionally timestamped (per client config),
     * added to chat (gated on the {@code SHOW_ENTER_AND_EXIT} config flag and on the name not being
     * {@code "Nobody"}), and an exit sound is played via the {@code SOUND_ON_EXIT} config parameter.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        CUser cUser = new CUser((String) stringTokenizer.nextElement());

        //Check the Users and remove the User
        CUser user = (CUser) client.getUser(cUser.getName());
        //delete every instance of that user from the list
        while (client.getUsers().remove(user)) {
            user = (CUser) client.getUser(cUser.getName());
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);

        if (client.isDedicated()) {
            return;
        }

        if ((cUser.isInvisible() && cUser.getUserLevel() > client.getUserLevel()) ||
                  cUser.getName().startsWith("[Dedicated]")) {
            return;
        }

        //Since there are more Elements, it'll be a Gone, so the user has left the room.
        if (stringTokenizer.hasMoreTokens()) {
            //Print the User-gone Info using the Info-Color (Maroon)
            String toSend = String.format("<font color=\"maroon\">>> Exit %s</font>", cUser.getName());

            if (client.getConfig().isParam("TIMESTAMP")) {
                toSend = client.getShortTime() + toSend;
            }

            if (client.getConfig().isParam("SHOW_ENTER_AND_EXIT") && !cUser.getName().equalsIgnoreCase("Nobody")) {
                client.addToChat(toSend);
            }

            //Play the sound
            client.doPlaySound(client.getConfigParam("SOUND_ON_EXIT"));
        }
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
