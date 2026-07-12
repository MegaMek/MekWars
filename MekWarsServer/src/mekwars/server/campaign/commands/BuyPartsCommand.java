/*
 * MekWars - Copyright (C) 2007
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - JTighe (Torren@users.sourceforge.net)
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


import java.util.StringTokenizer;

import megamek.common.equipment.EquipmentType;
import mekwars.common.BMEquipment;
import mekwars.common.util.UnitUtils;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;

public class BuyPartsCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser(String.format("AM:Insufficient access level for command. Level: %s. Required: %s.", userLevel, accessLevel),
                      Username,
                      true);
                return;
            }
        }

        if (!CampaignMain.campaignMain.isUsingAdvanceRepair()) {
            return;
        }

        //use /c buyparts#part#numbertobuy
        int numtobuy = 1;//default to 1 if no number present
        String partName = "";
        SPlayer player = CampaignMain.campaignMain.getPlayer(Username);

        try {
            partName = command.nextToken();
            numtobuy = Integer.parseInt(command.nextToken());
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("AM:Invalid Syntax: /BuyParts Name#Amount", Username);
            return;
        }
        boolean allowTechCrossOver = CampaignMain.campaignMain.getBooleanConfig("AllowCrossOverTech");
        BMEquipment bme = CampaignMain.campaignMain.getPartsMarket().getEquipmentList().get(partName);

        if (bme == null ||
                  (!allowTechCrossOver &&
                         !UnitUtils.isSameTech(bme.getTechLevel(), player.getMyHouse().getTechLevel()))) {
            CampaignMain.campaignMain.toUser(String.format("AM:%s not found on the black market", partName), Username);
            return;
        }

        if (numtobuy < 1) {
            CampaignMain.campaignMain.toUser("AM:You cannot buy negative parts!", Username);
            return;
        }

        int cost = (int) Math.ceil(numtobuy * bme.getCost());
        if (numtobuy > bme.getAmount()) {
            CampaignMain.campaignMain.toUser("AM:There are only " +
                                                   bme.getAmount() +
                                                   " crits left select a smaller amount", Username);
            return;
        }

        if (cost > player.getMoney()) {
            CampaignMain.campaignMain.toUser("AM:It will cost you " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         cost) +
                                                   " you only have " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         player.getMoney()) +
                                                   ".", Username);
            return;
        }

        EquipmentType eq = EquipmentType.get(bme.getEquipmentInternalName());

        bme.setAmount(bme.getAmount() - numtobuy);
        player.addMoney(-cost);

        if (eq == null) {
            CampaignMain.campaignMain.toUser("AM:You have bought " +
                                                   numtobuy +
                                                   " " +
                                                   bme.getEquipmentInternalName() +
                                                   " crits for " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         cost) +
                                                   ".", Username);
            player.updatePartsCache(bme.getEquipmentInternalName(), numtobuy);
        } else {
            CampaignMain.campaignMain.toUser("AM:You have bought " +
                                                   numtobuy +
                                                   " " +
                                                   eq.getName() +
                                                   " crits for " +
                                                   CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                         true,
                                                         cost) +
                                                   ".", Username);
            player.updatePartsCache(eq.getInternalName(), numtobuy);
        }
        CampaignMain.campaignMain.getPartsMarket().updatePartsBlackMarketAllPlayers();
    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end BuyPartsCommand()
