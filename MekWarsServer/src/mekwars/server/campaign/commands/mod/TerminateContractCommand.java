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

package mekwars.server.campaign.commands.mod;


public class TerminateContractCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.MODERATOR;
    String syntax = "Player Name";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                         userLevel +
                                                         ". Required: " +
                                                         accessLevel +
                                                         ".", Username, true);
            return;
        }

        //forcibly cancel a mercenary contract, returning escrow funds to hiring faction.
        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(command.nextToken());
        server.campaign.SHouse faction = p.getMyHouse();

        if (!faction.isMercHouse()) {
            server.campaign.CampaignMain.cm.toUser("Only mercenary players have contracts. Nice try, though.",
                  Username,
                  true);
            return;
        }

        //cast the house and load the contract
        server.campaign.mercenaries.MercHouse mercFaction = (server.campaign.mercenaries.MercHouse) faction;
        server.campaign.mercenaries.ContractInfo contract = mercFaction.getContractInfo(p);

        if (contract == null) {
            server.campaign.CampaignMain.cm.toUser(p.getName() + " has no contract to cancel", Username, true);
            return;
        }

        //contract exists. terminate and return monies.
        int payment = contract.getPayment();
        server.campaign.SHouse employer = contract.getEmployingHouse();
        int refund = (int) (payment * .5);

        server.campaign.SPlayer contractingPlayer = contract.getOfferingPlayer();
        if (contractingPlayer != null) {
            contractingPlayer.addMoney(refund);
            server.campaign.CampaignMain.cm.toUser(Username +
                                                         " abrogated your contract with"
                                                         +
                                                         contract.getEmployingHouse().getName() +
                                                         ". Funds returned from escrow ("
                                                         +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               refund,
                                                               true) +
                                                         ").", p.getName(), true);
        } else {contract.getEmployingHouse().setMoney(employer.getMoney() + refund);}

        mercFaction.endContract(p);
        server.campaign.CampaignMain.cm.toUser(Username +
                                                     " abrogated your contract with" +
                                                     contract.getEmployingHouse().getName() +
                                                     ".", p.getName(), true);
        server.campaign.CampaignMain.cm.toUser("You revoked " +
                                                     p.getName() +
                                                     "'s contract with" +
                                                     contract.getEmployingHouse().getName() +
                                                     ".", Username, true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}
}//end TerminateContract
