/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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
 * Client-side command that changes the client's own status code (e.g. away/busy/ready — the exact meaning of each
 * integer status is defined by {@code IClient.changeStatus}), as pushed by the server.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class ChangeStatusCommand extends Command {

    /**
     * Creates the command bound to the given client.
     *
     * @param client the client whose status will be changed
     */
    public ChangeStatusCommand(IClient client) {
        super(client);
    }

    /**
     * Decodes {@code input} and parses the single remaining token as an integer status code, then applies it via
     * {@code client.changeStatus}. Note: unlike most commands in this package, this uses
     * {@link Integer#parseInt(String)} directly rather than a defaulting parse helper, so a non-numeric token will
     * throw a {@link NumberFormatException} instead of falling back to a default value.
     *
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        client.changeStatus(Integer.parseInt(decode(input).nextToken()));
    }

    /**
     * No reply-argument parsing is needed for this command; intentionally a no-op.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Overridden to intentionally do nothing: this command instance's client binding is fixed at construction and
     * is never reassigned.
     */
    @Override
    public void setClient(IClient mwClient) {

    }

    /**
     * This command is never sent by a client to the server, so server-side argument parsing is a no-op.
     */
    @Override
    public void parseArguments(String s) {

    }
}
