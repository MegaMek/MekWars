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
import mekwars.common.util.TokenReader;

/**
 * Client-side command that receives one or more server configuration key/value pairs and applies them to the
 * client's local configuration. Sent when the server pushes its config (e.g. on connect) so the client's settings
 * stay in sync with the server's.
 */
public class SetServerConfigCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(SetServerConfigCommand.class);

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client whose configuration will be updated
     */
    public SetServerConfigCommand(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} and repeatedly reads (key, value) string pairs via {@link TokenReader}, applying each
     * pair to the client via {@code setServerConfigs}, until the tokenizer is exhausted. Note: if the number of
     * remaining tokens is odd (malformed input), {@link TokenReader#readString} on the missing second value will
     * throw, which is caught below and only logged — the loop does not otherwise validate pairing. Any exception
     * during parsing is caught and logged rather than propagated.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        try {
            StringTokenizer stringTokenizer = decode(input);

            while (stringTokenizer.hasMoreTokens()) {
                client.setServerConfigs(TokenReader.readString(stringTokenizer),
                      TokenReader.readString(stringTokenizer));
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Error setting server config");
        }
        //client.setWaiting(false);
    }//end execute

    /**
     * No reply-argument parsing is needed for this command; intentionally a no-op.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * This command is never sent by a client to the server, so server-side argument parsing is a no-op.
     */
    @Override
    public void parseArguments(String s) {

    }
}//end SC.java
