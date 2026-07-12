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

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Low-level protocol command (name {@code "ack_sign_on"}) that handles the server's acknowledgement of a client's
 * sign-on/login handshake. Records the confirmed username on the client and, when running as a dedicated
 * (headless) host, automatically starts hosting a game after a short delay.
 */

public class AcknowledgeSignOnPlayerCommand extends CProtCommand {
    static private final MMLogger LOGGER = MMLogger.create(AcknowledgeSignOnPlayerCommand.class);

    /**
     * Creates the command bound to the given client and registers its protocol name as {@code "ack_sign_on"}.
     *
     * @param client the client being signed on
     */
    public AcknowledgeSignOnPlayerCommand(IClient client) {
        super(client);
        name = "ack_sign_on";
    }

    /**
     * Validates that {@code input}'s first token matches this command's name/prefix, then extracts the
     * acknowledged username and applies it to the client via {@code setUsername}, logging the message. If this
     * client is running as a dedicated host, it then blocks the current thread for 5 seconds (a fixed delay,
     * presumably to let the sign-on settle before hosting starts) and calls {@code client.startHost(true, false,
     * false)} to automatically start hosting a game; any exception from either the sleep or the host start is
     * caught and only logged, not propagated. Note the blocking {@code Thread.sleep(5000)} runs on whatever thread
     * invokes {@code execute}, which will stall that thread (e.g. a network/dispatch thread) for the duration.
     *
     * @param input the raw tab-delimited protocol line, e.g. {@code "/ack_sign_on<TAB>username"}
     *
     * @return {@code true} if the input matched this command and was handled; {@code false} otherwise
     */
    @Override
    public boolean execute(String input) {
        StringTokenizer ST = new StringTokenizer(input, delimiter);
        if (check(ST.nextToken()) && ST.hasMoreTokens()) {
            input = decompose(input);
            ST = new StringTokenizer(input, delimiter);
            client.setUsername(ST.nextToken());
            echo(input);
            if (client.isDedicated()) {

                try {Thread.sleep(5000);} catch (Exception ex) {
                    LOGGER.error(ex);
                }

                try {
                    client.startHost(true, false, false);
                } catch (Exception ex) {
                    LOGGER.error(ex, "AcknowledgeSignOnPlayerCommand: Error attempting to start host on sign on.");
                }
            }

            return true;
        }
        //else
        return false;
    }

    /**
     * Logs the sign-on acknowledgement payload at info level (no GUI display for this command).
     *
     * @param input the decomposed acknowledgement payload
     */
    @Override
    protected void echo(String input) {
        LOGGER.info("Message from server: {}", input);
    }
}
