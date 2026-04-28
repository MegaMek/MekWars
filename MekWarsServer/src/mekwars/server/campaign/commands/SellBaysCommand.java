/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru // nathan.morris@gmail.com)
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


public class SellBaysCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

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

        if (!server.campaign.CampaignMain.cm.isUsingAdvanceRepair()) {
            return;
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = p.getMyHouse();

        //use /c firetech#numbertofire
        int numtosell = 1;//default to 1

        try {
            numtosell = Integer.parseInt(command.nextToken());
        }//end try
        catch (NumberFormatException ex) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Couldn't tell how many bays to sell. Check your input. It should be something like this: /c sellbays#3",
                  Username,
                  true);
            return;
        }//end catch

        //Check to see if the player is selling too many bays
        if (p.getBaysOwned() < numtosell) {
            server.campaign.CampaignMain.cm.toUser("AM:You tried to return " +
                                                         numtosell +
                                                         " bays, but you only have " +
                                                         p.getBaysOwned() +
                                                         " bays " +
                                                         ". The rest were assigned to your force by your faction and can't be returned.",
                  Username,
                  true);
            return;
        }

        //Check to see if the player is fighting. Engaged players can't fire techs.
        if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_FIGHTING) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You may not return bays while you are engaged! Wait until your units are out of battle and fully repaired.",
                  Username,
                  true);
            return;
        }//end if(fighting)

        //dont want a unit being marked unmaintained while its in an active army, so only let reserve players fire techs
        if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You may not return bays while you are active. Withdraw from the front lines " +
                        "before reducing your support levels.",
                  Username,
                  true);
            return;
        }//end if(active)

        if (p.getFreeBays() < numtosell) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:You need to free up some bay space before you can return anymore!",
                  Username,
                  true);
            return;
        }

        p.addBays(-numtosell);

        int sellbackprice = Integer.parseInt(house.getConfig("BaySellBackPrice")) * numtosell;
        p.addMoney(sellbackprice);

        if (numtosell == 1) {
            server.campaign.CampaignMain.cm.toUser("AM:You return a bay.  Your faction returns " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               sellbackprice) +
                                                         " of your security deposit.", Username, true);
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:You return " +
                                                         numtosell +
                                                         " bays.  Your faction returns " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               sellbackprice) +
                                                         " of your security deposit.", Username, true);
        }
        server.campaign.CampaignMain.cm.toUser("PL|SF|" + p.getFreeBays(), Username, false);
        server.campaign.CampaignMain.cm.toUser("PL|SB|" + p.getTotalMekBays(), Username, false);
        server.campaign.CampaignMain.cm.toUser("PL|ST|" + p.getBaysOwned(), Username, false);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}//end SellBaysCommand()
