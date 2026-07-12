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

import java.util.StringTokenizer;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.AdvancedRepairDialog;

/**
 * @author Torren (Jason Tighe)
 *       <p>
 *       Client-side handler for the {@code AdvancedRepairDialogCommand} protocol message. The server sends this
 *       after a unit's repair-relevant data has been updated (e.g. after a repair job completes or unit state
 *       changes), and executing it pops open a new {@link AdvancedRepairDialog} for that unit so the player sees
 *       the refreshed repair options.
 *       <p>
 *       This command creates a new repair dialog once the unit has been updated.
 */

public class AdvancedRepairDialogCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public AdvancedRepairDialogCommand(IClient client) {
        super(client);
    }

    /**
     * Parses the unit ID from the command payload and opens an {@link AdvancedRepairDialog} for it. A second,
     * optional token in the payload (its value is never inspected, only its presence matters) flips the dialog into
     * a different mode: present means the dialog is opened with the boolean flag {@code true} (e.g. "read-only" /
     * "post-repair" display), absent means it's opened with {@code false}.
     *
     * @param input the raw, delimited protocol line for this command; see {@link Command#decode(String)}
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        int unitID = Integer.parseInt(stringTokenizer.nextToken());

        if (stringTokenizer.hasMoreElements()) {
            new AdvancedRepairDialog(client, unitID, true);
        } else {
            new AdvancedRepairDialog(client, unitID, false);
        }
    }

    /**
     * Unused on the client side; this command has no reply-argument parsing behavior.
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     * No-op override. Unlike most other client commands, this class does not store the passed-in client on
     * {@link Command#client}; the client reference set at construction time (via the constructor) remains in use.
     */
    @Override
    public void setClient(IClient client) {

    }

    /**
     * Unused on the client side; this command is never parsed as server-bound arguments.
     */
    @Override
    public void parseArguments(String string) {

    }
}
