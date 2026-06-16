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

import megamek.client.ui.dialogs.UnitLoadingDialog;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.RePodSelectorDialog;

/**
 * @@author jtighe
 */
public class UnitRePodDialogCommand extends Command {
    private static final MMLogger LOGGER = MMLogger.create(UnitRePodDialogCommand.class);

    /**
     *
     */
    public UnitRePodDialogCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);

        try {
            String unitId = stringTokenizer.nextToken();

            if (!stringTokenizer.hasMoreTokens()) {
                String toUser = STR."CH|CLIENT: Your faction has no re-pod options for Unit \{unitId}.";
                client.doParseDataInput(toUser);
            } else {
                String chassisList = stringTokenizer.nextToken();
                UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
                RePodSelectorDialog rePodSelector = new RePodSelectorDialog(client.getMainFrame(),
                      unitLoadingDialog,
                      client,
                      chassisList,
                      unitId);
                sleep(125);
                new Thread(rePodSelector).start();
            }

        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to run RePod Dialog");
        }
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
