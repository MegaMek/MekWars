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

public class RefuseContractCommand implements Command {

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

        boolean contractCancelled = false;
        String receivingPlayerName = "";
        boolean offeringPlayerFound = false;
        String offeringPlayerName = command.nextToken();

        for (int i = 0; i < server.campaign.CampaignMain.cm.getUnresolvedContracts().size(); i++) {
            server.campaign.mercenaries.ContractInfo info = server.campaign.CampaignMain.cm.getUnresolvedContracts()
                                                                  .get(i);
            if (info.getOfferingPlayerName().equalsIgnoreCase(offeringPlayerName)) {
                offeringPlayerFound = true;
                //MWLogger.mainLog("CANCEL: Offering player found set to true");
                //if contract belong to offering player, check to see if it is for this player.
                receivingPlayerName = info.getPlayerName();
                if (server.campaign.CampaignMain.cm.getPlayer(receivingPlayerName) ==
                          server.campaign.CampaignMain.cm.getPlayer(Username)) {//player can kill contract offer
                    server.campaign.CampaignMain.cm.getUnresolvedContracts().remove(i);
                    contractCancelled = true;
                    server.campaign.CampaignMain.cm.toUser("AM:You refused the contract offered by " +
                                                                 offeringPlayerName, Username, true);
                    server.campaign.CampaignMain.cm.toUser(Username + " refused your contract offer",
                          offeringPlayerName,
                          true);
                    server.campaign.CampaignMain.cm.getUnresolvedContracts().trimToSize();
                    break;
                }//end if (contract is offered to player attempting to cancel)
            }//end if(contract offered by proper player)
        }//end for loop
        if (contractCancelled == false) {
            if (offeringPlayerFound == false) {//not found
                server.campaign.CampaignMain.cm.toUser("AM:This player has no outstanding contracts", Username, true);
            } else {//contract is for someone else
                server.campaign.CampaignMain.cm.toUser("AM:This player has not offered you a contract.",
                      Username,
                      true);
            }
        }//end if contract not cancelled.
    }//end process
}
