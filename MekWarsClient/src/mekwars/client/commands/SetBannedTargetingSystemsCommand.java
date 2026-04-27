/*
 * MekWars - Copyright (C) 2010
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

package mekwars.client.commands;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Spork (billypinhead@users.sourceforge.net)
 */
public class SetBannedTargetingSystemsCommand extends Command {

    /**
     * @see Command#Command(mekwars.common.campaign.clientutils.protocol.IClient)
     */
    public SetBannedTargetingSystemsCommand(IClient client) {
        super(client);
    }

    /**
     * @see client.cmd.Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        client.getData().getBannedTargetingSystems().clear();
        java.util.Vector<Integer> bans = new java.util.Vector<>(1, 1);

        while (st.hasMoreTokens()) {
            int ban = Integer.parseInt(st.nextToken());
            if (ban != 0) {
                // Don't ban standard TS
                bans.add(ban);
            }
        }

        client.getData().setBannedTargetingSystems(bans);
    }
}
