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

import mekwars.common.interfaces.IServer;

/**
 * Contract for the server-side half of a {@code Command}: the behavior needed when a command is used on the server
 * to interpret a message received from a client (as opposed to the client-side behavior declared in
 * {@link ClientCommand}). Concrete command classes typically implement both interfaces via the abstract
 * {@code Command} base class.
 *
 * @author Administrator
 */
public interface ServerCommand extends ICommand {

    /**
     * Parses the arguments portion of a command received from a client (prefix already stripped/identified) and
     * acts on it server-side.
     *
     * @param s the raw argument text for this command
     */
    void parseArguments(String s);

    /**
     * Associates this command instance with the server that will use it (e.g. to call back into server state or
     * broadcast to other clients while executing).
     *
     * @param server the owning server
     */
    void setServer(IServer server);

    /**
     * Sets the username of the client this command instance is currently being processed for.
     *
     * @param name the acting player's username
     */
    void setUsername(String name);

    /**
     * Sends a raw line of text back to the client associated with {@link #setUsername(String)}.
     *
     * @param txt the text to send
     */
    void clientSend(String txt);

}
