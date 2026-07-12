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
 * Low-level protocol command (name {@code "ping"}) that handles an incoming ping request over the tab-delimited
 * protocol sub-layer (see {@link CProtCommand}) and immediately answers it with a {@code pong} carrying the same
 * sender and timestamp, so the sender (or the connection code, if the sender is the server itself) can measure
 * round-trip time via {@link PongPCmd}.
 */
public class PingPlayerCommand extends CProtCommand {
    private final static MMLogger LOGGER = MMLogger.create(PingPlayerCommand.class);

    /**
     * Creates the command bound to the given client and registers its protocol name as {@code "ping"}.
     *
     * @param client the client that will reply to pings
     */
    public PingPlayerCommand(IClient client) {
        super(client);
        setName("ping");
    }

    /**
     * Validates that {@code input}'s first token matches this command's name/prefix, then extracts the sender name
     * and original timestamp and immediately replies with a {@code pong} message echoing both, via the raw
     * connector. If the ping did not come from {@code "server"}, the request is also echoed to the user via
     * {@link #echo(String)}; if it did come from the server, the client instead records the current time as its
     * last-ping timestamp (used for connection keepalive/liveness tracking) without showing a message.
     *
     * @param input the raw tab-delimited protocol line, e.g. {@code "/ping<TAB>sender<TAB>timestamp"}
     *
     * @return {@code true} if the input matched this command and was handled; {@code false} otherwise
     */
    @Override
    public boolean execute(String input) {
        StringTokenizer stringTokenizer = new StringTokenizer(input, getDelimiter());
        if (check(stringTokenizer.nextToken()) && stringTokenizer.hasMoreTokens()) {
            input = decompose(input);
            stringTokenizer = new StringTokenizer(input, getDelimiter());
            String sender = stringTokenizer.nextToken();
            String stamp = stringTokenizer.nextToken();

            LOGGER.info("Received server ping.");

            getConnector().send(String.format("%spong%s%s%s%s", getPrefix(), getDelimiter(), sender, getDelimiter(), stamp));

            if (!sender.equals("server")) {
                echo(input);
            } else {
                getClient().setLastPing(System.currentTimeMillis() / 1000);
            }

            return true;
        }

        return false;
    }

    /**
     * Reports the incoming ping request to the user via a system message, showing who sent it.
     *
     * @param input the decomposed ping payload: sender name followed by the ping timestamp
     */
    @Override
    protected void echo(String input) {
        StringTokenizer stringTokenizer = new StringTokenizer(input, getDelimiter());
        String sender = stringTokenizer.nextToken();
        getClient().systemMessage(String.format("Ping request from %s", sender));
    }
}
