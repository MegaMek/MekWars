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


import common.Unit;
import mekwars.server.campaign.CampaignMain;

public class SetMaintainedCommand implements Command {

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

        if (CampaignMain.campaignMain.isUsingAdvanceRepair()) {return;}

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        int numtoset = 0;//ID# of the mech which is to get set as maintained

        try {
            numtoset = Integer.parseInt(command.nextToken());
        }//end try
        catch (NumberFormatException ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:SetMaintained command failed. Check your input. It should be something like this: /c setmaintained#12",
                  Username,
                  true);
            return;
        }//end catch

        server.campaign.SUnit unitToSet = p.getUnit(numtoset);
        if (unitToSet == null) {
            CampaignMain.campaignMain.toUser("AM:Invalid id number. Make sure you're using the right unit number.",
                  Username,
                  true);
            return;
        }

        if (unitToSet.getStatus() == Unit.STATUS_OK) {
            CampaignMain.campaignMain.toUser("AM:This unit is already maintained.", Username, true);
            return;
        }

        if (unitToSet.getStatus() == Unit.STATUS_FORSALE) {
            CampaignMain.campaignMain.toUser(
                  "AM:You may not change the maintenance status of a unit which is being sold.",
                  Username,
                  true);
            return;
        }

        int unitSpace = server.campaign.SUnit.getHangarSpaceRequired(unitToSet, p.getMyHouse());
        if (p.getFreeBays() < unitSpace) {

            /*
             * Player doesnt have enough techs. determine how many he needs and how much they will cost.
             * If the player doesn't have enough money to maintain the unit in question, tell him. Else,
             * present him with a link which will hire more techs and retry the maintain command.
             */
            int techCost = p.getTechHiringFee();
            int numTechs = unitSpace - p.getFreeBays();
            techCost = techCost * numTechs;

            if (techCost > p.getMoney()) {

                String plural = "";
                if (numTechs == 1) {plural = "s";}

                CampaignMain.campaignMain.toUser("AM:You need to hire " +
                                                       numTechs +
                                                       " more technician" +
                                                       plural +
                                                       " in order to maintain this unit. Doing so would " +
                                                       " cost " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             false,
                                                             techCost) +
                                                       ", and you only have " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             true,
                                                             p.getMoney()) +
                                                       ".", Username, true);
                return;
            }

            String toReturn = "AM:You must hire " +
                                    numTechs +
                                    " more technicians in order to maintain this unit. Doing so will cost " +
                                    CampaignMain.campaignMain.moneyOrFluMessage(true, true, techCost) +
                                    ".<br>";
            toReturn += "AM:<a href=\"MEKWARS/c hireandmaintain#" +
                              numTechs +
                              "#" +
                              numtoset +
                              "\">Click here to hire the technicians and maintain the unit.</a>";
            CampaignMain.campaignMain.toUser(toReturn, Username, true);
            return;
        }//end if(not enough techs to maintain)

        //passes checks. now actually make the unit maintained.
        unitToSet.setStatus(Unit.STATUS_OK);
        CampaignMain.campaignMain.toUser("PL|SB|" + p.getTotalMekBays(), Username, false);
        CampaignMain.campaignMain.toUser("PL|SF|" + p.getFreeBays(), Username, false);
        CampaignMain.campaignMain.toUser("PL|SUS|" + unitToSet.getId() + "#" + Unit.STATUS_OK, Username, false);
        CampaignMain.campaignMain.toUser(unitToSet.getPilot().getName() +
                                               "'s " +
                                               unitToSet.getModelName() +
                                               " is now being maintained.", Username, true);
        p.setSave();

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end SetMaintainedCommand class

