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

/**
 * Minimal contract shared by both directions of the {@link Command} pattern used throughout this package:
 * {@link ClientCommand} (execute/reply handling on the client) and {@link ServerCommand} (argument handling on
 * the server) both extend this interface. It defines just the two things every command needs regardless of which
 * side it runs on: a dispatch key ("prefix") used to route protocol lines to the right handler, and a simple
 * error-state protocol for the request/reply flavor of commands (see {@link Command} class-level docs for how the
 * prefix is used on the wire and how the error state is populated).
 *
 * @author Administrator
 */
public interface ICommand {

    /**
     * @return the short dispatch code (e.g. {@code "CH"}) that identifies this command's position in a
     *         {@link Command.Table}, or {@code null} if it was never assigned (see {@link Command#Command(IClient)}
     *         vs {@link Command#Command(String)}).
     */
    String getPrefix();

    /**
     * @return {@code true} if this command instance currently holds an error/timeout/malformed condition.
     */
    boolean hasError();

    /**
     * @return a human-readable description of the current error condition, or an empty string if there is none
     *         (see {@link Command#getErrorMessage()} for the base-class caveats around when this is populated).
     */
    String getErrorMessage();

    /**
     * Clears any recorded error state, preparing the instance to be reused for another exchange.
     */
    void reset();

}
