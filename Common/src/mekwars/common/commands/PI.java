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

import megamek.codeUtilities.MathUtility;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PI extends Command {

    /**
     *
     */
    public PI(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        String task = stringTokenizer.nextToken();
        CUser user;
        switch (task) {
            case "PL" -> {
                while (stringTokenizer.hasMoreTokens()) {
                    user = (CUser) client.getUser(stringTokenizer.nextToken());
                    if (user != null) {
                        user.setCampaignData(client, stringTokenizer.nextToken());
                    }
                }
            }
            case "DA" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null) {
                    user.setCampaignData(client, stringTokenizer.nextToken());
                    if (user.getName().equalsIgnoreCase(client.getPlayer().getName())) {
                        client.getMainFrame().enableMenu();
                    }
                }
            }
            case "ChangeStatusCommand" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null) {
                    user.setStatus(MathUtility.parseInt(stringTokenizer.nextToken(), 0));
                }
            }
            case "FT" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setFluff(stringTokenizer.nextToken());
                }
            }
            case "SSN" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setSubFactionName(stringTokenizer.nextToken());
                }
            }
            case "EX" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setExp(MathUtility.parseInt(stringTokenizer.nextToken(), 0));
                }
            }
            case "RA" -> {
                user = (CUser) client.getUser(stringTokenizer.nextToken());
                if (user != null && stringTokenizer.hasMoreTokens()) {
                    user.setRating(MathUtility.parseFloat(stringTokenizer.nextToken(), 0.0f));
                }
            }
        }

        client.refreshGUI(IClient.REFRESH_USERLIST);
    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
