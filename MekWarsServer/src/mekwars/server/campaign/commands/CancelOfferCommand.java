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

public class CancelOfferCommand implements Command {
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

        boolean contractRemoved = false;
        String receivingPlayerName = "";
        for (int i = 0; i < server.campaign.CampaignMain.cm.getUnresolvedContracts().size(); i++) {
            server.campaign.mercenaries.ContractInfo info = server.campaign.CampaignMain.cm.getUnresolvedContracts()
                                                                  .get(i);
            if (info.getOfferingPlayerName().equalsIgnoreCase(Username)) {
                //if contract belong to offering player, remove the contract and set boolean to true
                receivingPlayerName = info.getPlayerName();
                server.campaign.CampaignMain.cm.getUnresolvedContracts().remove(i);
                contractRemoved = true;
                server.campaign.CampaignMain.cm.getUnresolvedContracts().trimToSize();
                break;
            }//end if(offering player has contract outstanding)
        }//end for(length of vector)
        if (contractRemoved == true) {
            server.campaign.CampaignMain.cm.toUser("AM:You have cancelled your offer to " + receivingPlayerName,
                  Username,
                  true);
            server.campaign.CampaignMain.cm.toUser(Username + " has rescinded his contract offer",
                  receivingPlayerName,
                  true);
        }//end if(a contract was removed)
        else if (contractRemoved == false) {
            server.campaign.CampaignMain.cm.toUser("AM:There was no outstanding contract to cancel!", Username, true);
        }//end elseif(no contract was removed)
    }//end process

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
