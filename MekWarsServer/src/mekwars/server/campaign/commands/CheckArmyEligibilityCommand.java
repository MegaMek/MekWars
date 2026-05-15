/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.server.campaign.commands;

import common.campaign.operations.Operation;
import mekwars.server.campaign.CampaignMain;

public class CheckArmyEligibilityCommand implements Command {


    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);

        int armyid = -1;
        String opName = "";
        try {
            armyid = Integer.parseInt(command.nextToken());
            opName = command.nextToken();
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper command. Try: /c checkarmyeligibility#id#operation",
                  Username,
                  true);
            return;
        }

        server.campaign.SArmy currA = p.getArmy(armyid);
        if (currA == null) {
            CampaignMain.campaignMain.toUser("AM:Could not find Army #" + armyid + ".", Username, true);
            return;
        }

        Operation currO = CampaignMain.campaignMain.getOpsManager().getOperation(opName);
        if (currO == null) {
            CampaignMain.campaignMain.toUser("AM:Operation Type: " + opName + " does not exist.", Username, true);
            return;
        }

        //breaks passed. check the op.
        String s = CampaignMain.campaignMain.getOpsManager()
                         .validateShortAttack(p, currA, currO, null, -1, false);
        if (s != null && !s.trim().equals("")) {
            CampaignMain.campaignMain.toUser("AM:" + opName + " is illegal for Army #" + armyid + " " + s,
                  Username,
                  true);
            return;
        }

        //else
        CampaignMain.campaignMain.toUser("AM:" + opName + " is legal for Army #" + armyid + ".", Username, true);

    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end CheckAttackCommand
