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

package client.cmd;

import java.util.StringTokenizer;

import client.MWClient;
import client.gui.dialog.RepodSelectorDialog;
import common.util.MWLogger;
import megamek.client.ui.swing.UnitLoadingDialog;

/**
 * @@author jtighe
 */
public class RUD extends Command {

    /**
     * @param client
     */
    public RUD(MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(java.lang.String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer ST = decode(input);

        try {

            String unitId = ST.nextToken();

            if (!ST.hasMoreTokens()) {
                String toUser = "CH|CLIENT: Your faction has no repod options for Unit "
                        + unitId + ".";
                mwclient.doParseDataInput(toUser);
            } else {
                String chassieList = ST.nextToken();
                UnitLoadingDialog unitLoadingDialog = new UnitLoadingDialog(
                        mwclient.getMainFrame());
                RepodSelectorDialog repodSelector = new RepodSelectorDialog(
                        mwclient.getMainFrame(), unitLoadingDialog, mwclient,
                        chassieList, unitId);
                Thread.sleep(125);
                new Thread(repodSelector).start();
            }

        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to run Repod Dialog");
        }
    }
}
