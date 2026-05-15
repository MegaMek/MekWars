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


import mekwars.server.campaign.CampaignMain;

public class BuyBaysCommand implements Command {

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

        if (!CampaignMain.campaignMain.isUsingAdvanceRepair()) {
            return;
        }

        //use /c buybays#numbertobuy
        int numtobuy = 1;//default to 1 if no number present
        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse house = p.getMyHouse();

        int bayCost = 0;//default cost
        int maxBays = Integer.parseInt(house.getConfig("MaxBaysToBuy"));//Max number of bays that can be bought
        try {
            numtobuy = Integer.parseInt(command.nextToken());
        }//end try
        catch (NumberFormatException ex) {
            CampaignMain.campaignMain.toUser(
                  "AM:Lease Bays command failed. Check your input. It should be something like this: /c buybays#3",
                  Username,
                  true);
            return;
        }//end catch

        //get cost per bay, after XP adjustment
        bayCost = Integer.parseInt(house.getConfig("CostToBuyNewBay"));

        if (bayCost == -1) {
            CampaignMain.campaignMain.toUser(
                  "AM:Sorry but their are no bays available for leasing at this moment in time.",
                  Username,
                  true);
            return;
        }

        //-1 maxbays allows for unlimited bays
        if (maxBays != -1 && p.getBaysOwned() + numtobuy > maxBays) {
            CampaignMain.campaignMain.toUser("AM:Sorry but the max number of bays you can lease is " +
                                                   maxBays +
                                                   ".", Username, true);
            return;
        }

        //get the total cost to hire these bays (cost for each * number to hire)
        bayCost = bayCost * numtobuy;

        //send a message is the bays
        if (bayCost > p.getMoney()) {
            CampaignMain.campaignMain.toUser("AM:Leasing " +
                                                   numtobuy +
                                                   " bays will cost you " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         bayCost) +
                                                   " for a security deposit. You only have " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         p.getMoney()) +
                                                   ".", Username, true);
            return;
        }//end if(player doenst have enough money)


        //passed all of the return scenarios, so add the bay
        p.addBays(numtobuy);
        p.addMoney(-bayCost);

        if (numtobuy == 1) {
            CampaignMain.campaignMain.toUser("AM:You've leased a bay! After paying the security deposit of " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         bayCost), Username, true);
        } else {
            CampaignMain.campaignMain.toUser("AM:You've leased " +
                                                   numtobuy +
                                                   " bays! After paying the security deposit of " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         false,
                                                         bayCost), Username, true);
        }

        CampaignMain.campaignMain.toUser("PL|SF|" + p.getFreeBays(), Username, false);
        CampaignMain.campaignMain.toUser("PL|SB|" + p.getTotalMekBays(), Username, false);
        CampaignMain.campaignMain.toUser("PL|ST|" + p.getBaysOwned(), Username, false);
    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end BuyBaysCommand()
