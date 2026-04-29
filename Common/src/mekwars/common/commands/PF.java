/*
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

import mekwars.common.campaign.clientutils.protocol.IClient;

public class PF extends Command {
    public PF(IClient client) {
        super(client);
    }

    /**
     * input should be one of the following: SDF|<PlayerFlags.export()>  to set all default flags AF|name|id|value  to
     * add a flag to both defaults and personal DF|name  to delete a flag from both defaults and personal SF|name|value
     * to set a personal flag SSDF|name|value  to set a new value for a default flag
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        String action = st.nextToken();
        if (action.equalsIgnoreCase("SDF")) {
            // Set Default Flags
            client.getPlayer().getDefaultPlayerFlags().loadDefaults(st.nextToken());
            client.getPlayer().getDefaultPlayerFlags().save();
        } else if (action.equalsIgnoreCase("AF")) {
            // Add a Flag
            // Should be a string with this format:  name|id|value
            String name = st.nextToken();
            int id = Integer.parseInt(st.nextToken());
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getFlags().addFlag(name, id, value);
            client.getPlayer().getDefaultPlayerFlags().addFlag(name, id, value);
        } else if (action.equalsIgnoreCase("DF")) {
            // Delete a Flag
            // Should be a string with this format:  name
            String name = st.nextToken();
            client.getPlayer().getFlags().clearFlag(name);
            client.getPlayer().getDefaultPlayerFlags().clearFlag(name);

        } else if (action.equalsIgnoreCase("SF")) {
            // Set Flag
            // Should be a string with this format:  name|value
            String name = st.nextToken();
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getFlags().setFlag(name, value);
        } else if (action.equalsIgnoreCase("SSDF")) {
            // Set Single Flag - used to change a single Default Flag
            // without changing status of the personal flag
            // Should be a string with this format: name|value
            String name = st.nextToken();
            boolean value = Boolean.parseBoolean(st.nextToken());
            client.getPlayer().getDefaultPlayerFlags().setFlag(name, value);
        }
        // As this is the last command sent on login, and since players' hangars aren't
        // being sorted when first logging in, it seems an appropriate time to send a
        // sortHangar command
        client.getPlayer().sortHangar();
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
