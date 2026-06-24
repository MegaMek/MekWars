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

/**
 * Updates the faction status screen
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class FactionStatusScreenUpdateCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public FactionStatusScreenUpdateCommand(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String cmdName;
        String cmdData;

        while (stringTokenizer.hasMoreTokens()) {
            cmdName = stringTokenizer.nextToken();
            cmdData = stringTokenizer.nextToken();
            this.issueSubCommand(cmdName, cmdData);

            if (cmdName.equals("CA")) {
                return;//return without updating view
            }
        }

        //only update after all commands processed
        client.getMainFrame().getMainPanel().getHSPanel().updateDisplay();
    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String string) {

    }

    /**
     * FactionStatusScreenUpdateCommand| Commands can be issued in bulk. This allows short ops, etc to send ALL changes
     * they make at impacted house players at once instead of sending 5-10 separate updates.
     */
    private void issueSubCommand(String cmdName, String cmdData) {

        switch (cmdName) {
            case "FN" -> client.getMainFrame()
                               .getMainPanel()
                               .getHSPanel()
                               .setFactionName(cmdData);
            case "AU" -> client.getMainFrame()
                               .getMainPanel()
                               .getHSPanel()
                               .addFactionUnit(cmdData);
            case "RU" -> client.getMainFrame()
                               .getMainPanel()
                               .getHSPanel()
                               .removeFactionUnit(cmdData);
            case "CC" -> client.getMainFrame()
                               .getMainPanel()
                               .getHSPanel()
                               .changeFactionComponents(cmdData);
            case "AF" -> client.getMainFrame()
                               .getMainPanel()
                               .getHSPanel()
                               .addFactionFactory(cmdData);
            case "RF" -> client.getMainFrame().getMainPanel().getHSPanel().removeFactionFactory(cmdData);
            case "CF" -> client.getMainFrame().getMainPanel().getHSPanel().changeFactionFactory(cmdData);
            case "CA" -> client.getMainFrame().getMainPanel().getHSPanel().clearHouseStatusData();
        }
    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
