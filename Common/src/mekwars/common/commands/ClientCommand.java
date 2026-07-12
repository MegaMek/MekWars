/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Contract for the client-side half of a {@code Command}: the behavior needed when a command is used to receive
 * and react to a message sent from the server to a MekWars client (as opposed to the server-side behavior declared
 * in {@link ServerCommand}). Concrete command classes (e.g. those in this package) typically implement both
 * {@code ClientCommand} and {@code ServerCommand} via the abstract {@code Command} base class, since the same
 * protocol prefix may be interpreted on either side of the connection.
 *
 * @author Administrator
 */
public interface ClientCommand extends ICommand {

    /**
     * Parses a raw, pipe-delimited reply line received from the server for this command and updates this command's
     * internal state (including error state) accordingly.
     *
     * @param s the raw reply text, including the command prefix
     */
    void parseReply(String s);

    /**
     * Marks this command as having timed out (no reply received in time). Sets the error state so
     * {@link ICommand#hasError()} / {@link ICommand#getErrorMessage()} report the timeout.
     */
    void timeout();

    /**
     * Sends this command to the server.
     *
     * @param blocking if {@code true}, the call should block until a reply is received; if {@code false} it should
     *       return immediately
     */
    void send(boolean blocking);

    /**
     * Associates this command instance with the client that will use it (e.g. to call back into client GUI/state
     * methods while executing).
     *
     * @param mwClient the owning client
     */
    void setClient(IClient mwClient);
}
