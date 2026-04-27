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

import megamek.client.ui.dialogs.UnitLoadingDialog;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.RePodSelectorDialog;
import mekwars.common.util.MWLogger;

/**
 * @@author jtighe
 */
public class UnitRePodDialogCommand extends Command {

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
        java.util.StringTokenizer ST = decode(input);

        try {

            String unitId = ST.nextToken();

            if (!ST.hasMoreTokens()) {
                String toUser = "CH|CLIENT: Your faction has no re-pod options for Unit "
                                      + unitId + ".";
                client.doParseDataInput(toUser);
            } else {
                String chassisList = ST.nextToken();
                UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(client.getMainFrame());
                RePodSelectorDialog repodSelector = new RePodSelectorDialog(client.getMainFrame(),
                      unitLoadingDialog,
                      client,
                      chassisList,
                      unitId);
                Thread.sleep(125);
                new Thread(repodSelector).start();
            }

        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to run RePod Dialog");
        }
    }
}
