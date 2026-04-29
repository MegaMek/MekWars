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
package mekwars.common.campaign.clientutils.protocol.commands;

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Pong command
 */

public class PongPCmd extends CProtCommand {

    public PongPCmd(IClient client) {
        super(client);
        name = "pong";
    }

    // execute command
    @Override
    public boolean execute(String input) {

        StringTokenizer ST = new StringTokenizer(input, delimiter);
        if (check(ST.nextToken()) && ST.hasMoreTokens()) {
            input = decompose(input);
            echo(input);
            return true;
        }

        //else
        return false;
    }

    // echo command in GUI
    @Override
    protected void echo(String input) {
        StringTokenizer ST = new StringTokenizer(input, delimiter);
        String sender = ST.nextToken();
        if (sender.equals("server")) {return;}
        float time = (float) (System.currentTimeMillis() - Long.parseLong(ST.nextToken())) / 1000;
        client.systemMessage(STR."Ping reply from \{sender}: \{time} s");
    }

}
