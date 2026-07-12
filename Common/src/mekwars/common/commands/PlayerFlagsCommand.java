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

import megamek.codeUtilities.MathUtility;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Client-side handler for the {@code PlayerFlagsCommand} protocol message, used by the server to synchronize a
 * player's "player flags" (arbitrary named boolean settings tracked per-player, with an associated default set).
 * Executing it dispatches on a sub-action code to add, remove, or update entries in the player's personal flag set
 * and/or the shared default flag set, and finishes by re-sorting the player's hangar.
 */
public class PlayerFlagsCommand extends Command {
    public PlayerFlagsCommand(IClient client) {
        super(client);
    }

    /**
     * input should be one of the following: SDF|<PlayerFlags.export()>  to set all default flags AF|name|id|value to
     * add a flag to both defaults and personal DF|name to delete a flag from both defaults and personal SF|name|value
     * to set a personal flag SSDF|name|value to set a new value for a default flag
     * <p>
     * Sub-action reference:
     * <ul>
     *     <li>{@code SDF} - bulk-load the entire default flag set from an exported blob and persist it.</li>
     *     <li>{@code AF} - add a flag (name/id/value) to both the personal flag set and the default flag set.</li>
     *     <li>{@code DF} - clear a flag (by name) from both the personal flag set and the default flag set.</li>
     *     <li>{@code SF} - set a personal flag's value only (defaults untouched).</li>
     *     <li>{@code SSDF} - set a single default flag's value only, without touching the personal flag of the
     *     same name.</li>
     * </ul>
     * An unrecognized action code is silently ignored (falls through with no match). Regardless of which branch
     * ran (or whether any branch matched), the method unconditionally calls
     * {@code client.getPlayer().sortHangar()} at the end; the surrounding comment notes this piggybacks on the
     * fact that this command is the last one sent during login, as a convenient hook to sort the hangar once
     * post-login.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String action = stringTokenizer.nextToken();
        if (action.equalsIgnoreCase("SDF")) {
            // Set Default Flags
            client.getPlayer().getDefaultPlayerFlags().loadDefaults(stringTokenizer.nextToken());
            client.getPlayer().getDefaultPlayerFlags().save();
        } else if (action.equalsIgnoreCase("AF")) {
            // Add a Flag
            // Should be a string with this format: name|id|value
            String name = stringTokenizer.nextToken();
            int id = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
            boolean value = MathUtility.parseBoolean(stringTokenizer.nextToken(), false);
            client.getPlayer().getFlags().addFlag(name, id, value);
            client.getPlayer().getDefaultPlayerFlags().addFlag(name, id, value);
        } else if (action.equalsIgnoreCase("DF")) {
            // Delete a Flag
            // Should be a string with this format: name
            String name = stringTokenizer.nextToken();
            client.getPlayer().getFlags().clearFlag(name);
            client.getPlayer().getDefaultPlayerFlags().clearFlag(name);

        } else if (action.equalsIgnoreCase("SF")) {
            // Set Flag
            // Should be a string with this format: name|value
            String name = stringTokenizer.nextToken();
            boolean value = MathUtility.parseBoolean(stringTokenizer.nextToken(), false);
            client.getPlayer().getFlags().setFlag(name, value);
        } else if (action.equalsIgnoreCase("SSDF")) {
            // Set Single Flag - used to change a single Default Flag
            // without changing status of the personal flag
            // Should be a string with this format: name|value
            String name = stringTokenizer.nextToken();
            boolean value = MathUtility.parseBoolean(stringTokenizer.nextToken(), false);
            client.getPlayer().getDefaultPlayerFlags().setFlag(name, value);
        }
        // As this is the last command sent on login, and since players' hangars aren't
        // being sorted when first logging in, it seems an appropriate time to send a
        // sortHangar command
        client.getPlayer().sortHangar();
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
