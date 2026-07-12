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
 * Low-level protocol command (name {@code "comm"}) that wraps a nested campaign/chat payload so it can travel over
 * the tab-delimited protocol sub-layer (see {@link CProtCommand}) alongside control commands like ping/pong and
 * sign-on acknowledgement. Received "comm" messages are unwrapped and forwarded into the normal client data-input
 * pipeline for further parsing.
 */

public class CommPCmd extends CProtCommand {
    /**
     * Creates the command bound to the given client and registers its protocol name as {@code "comm"}.
     *
     * @param client the client that will receive dispatched payloads
     */
    public CommPCmd(IClient client) {
        super(client);
        name = "comm";
    }

    /**
     * Validates that {@code input}'s first token matches this command's name/prefix, then extracts the next token
     * (the nested payload) and hands it off to the client for parsing: {@code doParseDataInput} for a normal GUI
     * client, or {@code parseDedDataInput} when running as a dedicated (headless) host.
     *
     * @param input the raw tab-delimited protocol line, e.g. {@code "/comm<TAB>payload"}
     *
     * @return {@code true} if the input matched this command and was dispatched; {@code false} otherwise
     */
    @Override
    public boolean execute(String input) {

        StringTokenizer stringTokenizer = new StringTokenizer(input, delimiter);
        if (check(stringTokenizer.nextToken()) && stringTokenizer.hasMoreTokens()) {
            input = stringTokenizer.nextToken();

            if (!client.isDedicated()) {
                client.doParseDataInput(input);
            } else {
                client.parseDedDataInput(input);
            }

            return true;
        }

        return false;
    }
}
