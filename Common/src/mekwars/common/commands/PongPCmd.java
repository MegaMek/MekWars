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

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Low-level protocol command (name {@code "pong"}) that handles the reply half of the ping/pong keepalive exchange
 * started by {@link PingPlayerCommand}. Receiving a "pong" means some peer responded to a ping this client sent;
 * the round-trip time is computed and reported to the user.
 */

public class PongPCmd extends CProtCommand {

    /**
     * Creates the command bound to the given client and registers its protocol name as {@code "pong"}.
     *
     * @param client the client that will report ping round-trip times
     */
    public PongPCmd(IClient client) {
        super(client);
        name = "pong";
    }

    /**
     * Validates that {@code input}'s first token matches this command's name/prefix, then strips the
     * prefix/name (via {@link CProtCommand#decompose(String)}) and passes the remainder to {@link #echo(String)}
     * for reporting.
     *
     * @param input the raw tab-delimited protocol line, e.g. {@code "/pong<TAB>sender<TAB>timestamp"}
     *
     * @return {@code true} if the input matched this command and was handled; {@code false} otherwise
     */
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

    /**
     * Reports the ping round-trip time to the user via a system message, unless the sender is {@code "server"} (in
     * which case the pong is silently absorbed and no message is shown). The elapsed time is computed as the
     * difference between the current time and the timestamp echoed back by the sender, in seconds.
     *
     * @param input the decomposed pong payload: sender name followed by the original ping timestamp (millis)
     */
    @Override
    protected void echo(String input) {
        StringTokenizer ST = new StringTokenizer(input, delimiter);
        String sender = ST.nextToken();

        if (sender.equals("server")) {
            return;
        }

        float time = (float) (System.currentTimeMillis() - Long.parseLong(ST.nextToken())) / 1000;
        client.systemMessage(String.format("Ping reply from %s: %s s", sender, time));
    }

}
