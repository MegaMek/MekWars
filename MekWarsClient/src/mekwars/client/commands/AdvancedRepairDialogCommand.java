/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package mekwars.client.commands;

import java.util.StringTokenizer;

import mekwars.client.MWClient;
import mekwars.common.gui.dialogs.AdvancedRepairDialog;

/**
 * @@author Torren (Jason Tighe)
 *       <p>
 *       Used for Advanced Repair Dialog.
 *       <p>
 *       This command creates a new repair dialog once the unit has been updated.
 */

public class AdvancedRepairDialogCommand extends Command {

    /**
     * @see Command#Command(MWClient)
     */
    public AdvancedRepairDialogCommand(MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer st = decode(input);
        int unitID = Integer.parseInt(st.nextToken());

        if (st.hasMoreElements()) {
            new AdvancedRepairDialog(client, unitID, true);
        } else {
            new AdvancedRepairDialog(client, unitID, false);
        }
    }
}
