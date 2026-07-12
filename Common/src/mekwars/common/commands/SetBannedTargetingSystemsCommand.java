/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2010 Helge Richter (McWizard)
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
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side handler for the {@code SetBannedTargetingSystemsCommand} protocol message, sent by the server to
 * tell the client which targeting-system types (e.g. advanced TS variants configured as disallowed by the campaign
 * rules) are currently banned. Executing it replaces the client's local banned-targeting-systems list with the
 * set received from the server.
 *
 * @author Spork (billypinhead@users.sourceforge.net)
 */
public class SetBannedTargetingSystemsCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public SetBannedTargetingSystemsCommand(IClient client) {
        super(client);
    }

    /**
     * Parses a list of integer targeting-system codes from the command payload and stores it as the client's
     * banned-targeting-systems list, discarding whatever was previously banned.
     * <p>
     * Each token is parsed with {@link MathUtility#parseInt(String, int)} using a default of {@code 0} on
     * failure; a parsed value of {@code 0} is treated as meaning "standard targeting system" and is deliberately
     * skipped so the standard TS can never end up banned, even if the token was unparsable garbage that fell back
     * to {@code 0}.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        client.getData().getBannedTargetingSystems().clear();
        Vector<Integer> bans = new Vector<>(1, 1);

        while (stringTokenizer.hasMoreTokens()) {
            int ban = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

            if (ban != 0) {
                // Don't ban standard TS
                bans.add(ban);
            }
        }

        client.getData().setBannedTargetingSystems(bans);
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String s) {

    }
}
