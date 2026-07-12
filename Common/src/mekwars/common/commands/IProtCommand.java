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

/**
 * Contract for the low-level "protocol" command layer, distinct from the higher-level chat-delimited
 * {@link Command}/{@link ICommand} family documented on {@link Command}. Protocol commands handle the
 * login/handshake style traffic sent with the client's protocol prefix (tokenized on a tab-delimited
 * {@code PROTOCOL_DELIMITER} rather than {@link Command#DELIM}) — examples elsewhere in this package include ping,
 * pong, and sign-on acknowledgement handlers, which typically extend a common {@code CProtCommand} base rather than
 * implementing this interface directly.
 * <p>
 * Dispatch (e.g. in {@code MWClient.processIncoming}) works by tokenizing an incoming protocol line, looking up the
 * registered {@code IProtCommand} whose {@link #getName()} matches the first token, calling {@link #check(String)}
 * to confirm it is willing to handle that token, and then invoking {@link #execute(String)} with the remaining
 * text. A {@code false} return from either method is treated as a dispatch failure and logged.
 */
public interface IProtCommand {
    /**
     * Confirms that this instance is the right handler for the given command name/token before
     * {@link #execute(String)} is invoked.
     *
     * @param name the leading token identifying the command
     * @return {@code true} if this instance recognizes and will handle {@code name}
     */
    // check if this is proper command
    boolean check(String name);

    /**
     * Invoked when command is executed. Performs whatever handshake/login-protocol action this command represents.
     *
     * @param command the command text (implementation-specific how much of the original line is included)
     * @return {@code true} on success; {@code false} signals a dispatch/execution failure to the caller
     */
    // invoked when command is executed
    boolean execute(String command);

    /**
     * @return the name/token this command answers to, matched against the leading token of an incoming protocol
     *         line during dispatch.
     */
    String getName();
}
