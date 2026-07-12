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

import mekwars.server.campaign.CampaignMain;

public class CancelOfferCommand implements Command {
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

        boolean contractRemoved = false;
        String receivingPlayerName = "";
        for (int i = 0; i < CampaignMain.campaignMain.getUnresolvedContracts().size(); i++) {
            server.campaign.mercenaries.ContractInfo info = CampaignMain.campaignMain.getUnresolvedContracts()
                                                                  .get(i);
            if (info.getOfferingPlayerName().equalsIgnoreCase(Username)) {
                //if contract belong to offering player, remove the contract and set boolean to true
                receivingPlayerName = info.getPlayerName();
                CampaignMain.campaignMain.getUnresolvedContracts().remove(i);
                contractRemoved = true;
                CampaignMain.campaignMain.getUnresolvedContracts().trimToSize();
                break;
            }//end if(offering player has contract outstanding)
        }//end for(length of vector)
        if (contractRemoved == true) {
            CampaignMain.campaignMain.toUser("AM:You have cancelled your offer to " + receivingPlayerName,
                  Username,
                  true);
            CampaignMain.campaignMain.toUser(Username + " has rescinded his contract offer",
                  receivingPlayerName,
                  true);
        }//end if(a contract was removed)
        else if (contractRemoved == false) {
            CampaignMain.campaignMain.toUser("AM:There was no outstanding contract to cancel!", Username, true);
        }//end elseif(no contract was removed)
    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
