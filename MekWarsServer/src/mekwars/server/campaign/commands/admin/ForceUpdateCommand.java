/*
 * Copyright (C) 2006 Jason Tighe (torren@users.sourceforge.net)
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


package mekwars.server.campaign.commands.admin;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

import megamek.logging.MMLogger;
import mekwars.server.MWChatServer.auth.IAuthenticator;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.DefaultServerOptions;
import mekwars.server.campaign.commands.Command;


/**
 * Allows SO's to force clients to update without a major version change
 * <p>
 * Syntax  /c forceupdate#Key#[Player/Dedicated/All]
 * <code>Player/Dedicated/All</code> are optional and will kick those entities
 * off so that they have to update right away.
 */
public class ForceUpdateCommand implements Command {
    private final static MMLogger LOGGER = MMLogger.create(ForceUpdateCommand.class);
    private final String syntax = "Update Key#[Player/Dedicated/All]";
    private IAuthenticator accessLevel = IAuthenticator.MODERATOR;

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {
        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(STR."AM:Insufficient access level for command. Level: \{userLevel}. Required: \{accessLevel}.",
                      Username,
                      true);
                return;
            }
        }

        String updateKey = "";
        String whoToKick = "";

        try {
            updateKey = command.nextToken();
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("You must supply a Key<br>" +
                                                   "Syntax  /c force update#Key[Clear]#[Player/Dedicated/All]<br>" +
                                                   "Player/Dedicated/All are optional and will kick those entities<br>" +
                                                   "off so that they have to update right away.", Username);
            return;
        }

        if (updateKey.equalsIgnoreCase("Clear") || updateKey.equalsIgnoreCase("-1")) {
            updateKey = "";
        }

        CampaignMain.campaignMain.getConfig().setProperty("ForceUpdateKey", updateKey);
        DefaultServerOptions defaultServerOptions = new DefaultServerOptions();
        defaultServerOptions.createConfig();

        CampaignMain.campaignMain.doSendModMail("NOTE", STR."\{Username} set the Force Update Key");
        CampaignMain.campaignMain.toUser(STR."Make sure to add UPDATEKEY=\{updateKey}<br>To the serverdata.dat",
              Username);

        if (command.hasMoreTokens()) {
            whoToKick = command.nextToken();
            CampaignMain.campaignMain.doSendModMail("NOTE", STR."\{Username} is kicking \{whoToKick}");
            boolean players = false;
            boolean deds = false;

            if (whoToKick.equalsIgnoreCase("all")) {
                players = true;
                deds = true;
            } else if (whoToKick.toLowerCase().startsWith("player")) {
                players = true;
            } else {
                deds = true;
            }

            ConcurrentLinkedQueue<String> users = new ConcurrentLinkedQueue<>(CampaignMain.campaignMain.getServer()
                                                                                    .getUsers()
                                                                                    .keySet());
            for (String toKick : users) {
                if (CampaignMain.campaignMain.getServer().isAdmin(toKick)) {
                    continue;
                }

                if (players && !toKick.toLowerCase().startsWith("[dedicated]")) {
                    CampaignMain.campaignMain.toUser(STR."You have been forced to update by \{Username}!", toKick);
                    CampaignMain.campaignMain.toUser("PL|FCU|Bye Bye", toKick, false);
                } else if (deds && toKick.toLowerCase().startsWith("[dedicated]")) {
                    try {
                        CampaignMain.campaignMain.getServer().doStoreMail(STR."\{toKick},update", Username);
                        Thread.sleep(120);
                    } catch (Exception ex) {
                        LOGGER.error(ex, "Thread interruption. Probably harmless. {}", ex.getLocalizedMessage());
                    }
                }
            }//end for
        }//end hasMore Commands
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
