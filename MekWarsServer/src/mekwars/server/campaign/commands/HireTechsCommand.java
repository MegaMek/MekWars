/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru)
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


import common.util.StringUtils;
import common.util.UnitUtils;

public class HireTechsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        if (server.campaign.CampaignMain.cm.isUsingAdvanceRepair()) {
            hireAdvanceTechs(command, Username);
            return;
        }


        //use /c hiretech#numbertohire
        int numtohire = 1;//default to 1 if no number present
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);

        int techCost = 0;//default cost

        //don't let SOL players hire techs
        if (p.getMyHouse().isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You are in a training faction, and may not hire techs until you join a normal faction.",
                  Username,
                  true);
            return;
        }

        try {
            numtohire = Integer.parseInt(command.nextToken());
        } catch (NumberFormatException ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Hire command failed. Check your input. It should be something like this: /c hiretechs#3",
                  Username,
                  true);
            return;
        }//end catch

        //get cost per tech, after XP adjustment
        techCost = p.getTechHiringFee();

        //get the total cost to hire these techs (cost for each * number to hire)
        techCost = techCost * numtohire;

        //send a message is the techs
        if (techCost > p.getMoney()) {
            server.campaign.CampaignMain.cm.toUser("AM:Hiring " +
                                                         numtohire +
                                                         " techs will cost you " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               techCost) +
                                                         ". You only have " +
                                                         p.getMoney() +
                                                         " " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               p.getMoney()) +
                                                         ".", Username, true);
            return;
        }//end if(player doenst have enough money)


        int maxTechs = Integer.parseInt(p.getMyHouse().getConfig("MaxTechsToHire"));
        if (maxTechs != -1 && (p.getTechnicians() + numtohire > maxTechs)) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry but the max number of technicians you can hire is " +
                                                         maxTechs +
                                                         ".", Username, true);
            return;
        }


        //passed all of the return scenarios, so add the technicians
        p.addTechnicians(numtohire);
        // had to do it grammer bad! Torren
        if (numtohire == 1) {
            server.campaign.CampaignMain.cm.toUser("AM:You've hired a technician! (-" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               techCost) +
                                                         ")", Username, true);
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:You've hired " +
                                                         numtohire +
                                                         " technicians! (-" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               techCost) +
                                                         ")", Username, true);
        }
        p.addMoney(-techCost); //Forgot to deduct the cost of Techs Torren.
    }//end process()

    private void hireAdvanceTechs(java.util.StringTokenizer command, String Username) {

        server.campaign.SPlayer player = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = player.getMyHouse();

        int numberToHire = Integer.parseInt(command.nextToken());
        int techType = UnitUtils.TECH_GREEN;

        int maxLevelTechHire = UnitUtils.TECH_REG;

        if (!Boolean.parseBoolean(house.getConfig("AllowRegTechsToBeHired"))) {maxLevelTechHire = UnitUtils.TECH_GREEN;}

        if (command.hasMoreElements()) {techType = Integer.parseInt(command.nextToken());}

        if (techType > maxLevelTechHire) {
            server.campaign.CampaignMain.cm.toUser("AM:Sorry there are no techs of that skill level on the market.",
                  Username,
                  true);
            return;
        }

        int hireCost = Integer.parseInt(house.getConfig(UnitUtils.techDescription(techType) + "TechHireCost"));

        hireCost *= numberToHire;

        if (player.getMoney() < hireCost) {
            server.campaign.CampaignMain.cm.toUser("AM:Hiring " +
                                                         numberToHire +
                                                         " " +
                                                         UnitUtils.techDescription(techType) +
                                                         " techs will cost you " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               hireCost) +
                                                         ". You only have " +
                                                         player.getMoney() +
                                                         " " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               player.getMoney()) +
                                                         ".", Username, true);
            return;
        }

        //passed all of the return scenarios, so add the technicians
        player.addTotalTechs(techType, numberToHire);
        player.addAvailableTechs(techType, numberToHire);
        player.addMoney(-hireCost);

        if (numberToHire == 1) {
            server.campaign.CampaignMain.cm.toUser("AM:You've hired " +
                                                         StringUtils.aOrAn(UnitUtils.techDescription(techType), true) +
                                                         " technician! (-" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               hireCost) +
                                                         ")", Username, true);
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:You've hired " +
                                                         numberToHire +
                                                         " " +
                                                         UnitUtils.techDescription(techType) +
                                                         " technicians! (-" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               false,
                                                               hireCost) +
                                                         ")", Username, true);
        }


    }//end hireAdvanceTech

}//end HireTechsCommand()
