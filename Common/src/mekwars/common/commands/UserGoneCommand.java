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
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class UserGoneCommand extends Command {

    /**
     * @param client
     */
    public UserGoneCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer st = decode(input);
        CUser mmci = new CUser((String) st.nextElement());

        //Check the Users and remove the User
        CUser user = (CUser) client.getUser(mmci.getName());
        //delete every instance of that user from the list
        while (client.getUsers().remove(user)) {
            user = (CUser) client.getUser(mmci.getName());
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);

        if (client.isDedicated()) {
            return;
        }

        if ((mmci.isInvisible() && mmci.getUserLevel() > client.getUserLevel()) ||
                  mmci.getName().startsWith("[Dedicated]")) {
            return;
        }

        //Since there are more Elements, it'll be a Gone, so the user has left the room.
        if (st.hasMoreTokens()) {
            //Print the User-gone Info using the Info-Color (Maroon)
            String toSend = STR."<font color=\"maroon\">>> Exit \{mmci.getName()}</font>";

            if (client.getConfig().isParam("TIMESTAMP")) {toSend = client.getShortTime() + toSend;}

            if (client.getConfig().isParam("SHOWENTERANDEXIT") && !mmci.getName().equalsIgnoreCase("Nobody")) {
                client.addToChat(toSend);
            }

            //Play the sound
            client.doPlaySound(client.getConfigParam("SOUNDONEXIT"));
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
