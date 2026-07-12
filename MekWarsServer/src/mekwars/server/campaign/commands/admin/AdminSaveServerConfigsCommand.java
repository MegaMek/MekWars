/*
 * Copyright (C) 2004 MekWars
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


/**
 * @author jtighe
 *       <p>
 *       Command Saves the server config to its defined file
 */
package mekwars.server.campaign.commands.admin;

import java.util.StringTokenizer;

import mekwars.server.MWChatServer.auth.IAuthenticator;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.DefaultServerOptions;
import mekwars.server.campaign.commands.Command;

public class AdminSaveServerConfigsCommand implements Command {
    int accessLevel = IAuthenticator.ADMIN;
    String syntax = "";

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {
        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser(String.format("AM:Insufficient access level for command. Level: %s. Required: %s.", userLevel, accessLevel),
                  Username,
                  true);
            return;
        }

        DefaultServerOptions defaultServerOptions = new DefaultServerOptions();
        defaultServerOptions.createConfig();
        CampaignMain.campaignMain.toUser("AM:Status saved!", Username, true);
        CampaignMain.campaignMain.doSendModMail("NOTE", String.format("%s has saved the server configs", Username));

    }//end process

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
