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

package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class AdminTransferCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Sending Player#Receiving Player#Unit ID";

    public String getSyntax() {return syntax;}

    public void process(java.util.StringTokenizer command, String Username) {

        //access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                   userLevel +
                                                   ". Required: " +
                                                   accessLevel +
                                                   ".", Username, true);
            return;
        }

        String sendingPlayer = null;
        String receivingPlayer = null;
        int mechid = -1;

        try {
            sendingPlayer = (String) command.nextElement();
            receivingPlayer = (String) command.nextElement();
            mechid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c admintransfer#from#to#id",
                  Username,
                  true);
            return;
        }


        server.campaign.SPlayer sender = CampaignMain.campaignMain.getPlayer(sendingPlayer);
        server.campaign.SPlayer receiver = CampaignMain.campaignMain.getPlayer(receivingPlayer);

        if (sender == null) {
            CampaignMain.campaignMain.toUser("AM:Sending player could not be found. Try again.", Username, true);
            return;
        }

        if (receiver == null) {
            CampaignMain.campaignMain.toUser("AM:Receiving player could not be found. Try again.",
                  Username,
                  true);
            return;
        }

        server.campaign.SUnit m = sender.getUnit(mechid);
        if (m == null) {
            CampaignMain.campaignMain.toUser("AM:Sender doesn't have a unit with ID# " + mechid + ".",
                  Username,
                  true);
            return;
        }

        //passed all the breaks. discuss the transfer.
        CampaignMain.campaignMain.toUser("AM:You transfered " +
                                               sendingPlayer +
                                               "'s " +
                                               m.getModelName() +
                                               " to " +
                                               receiver.getName(), Username, true);
        CampaignMain.campaignMain.toUser("AM:" +
                                               Username +
                                               " forced " +
                                               sendingPlayer +
                                               " to send you a " +
                                               m.getModelName() +
                                               ".", receivingPlayer, true);
        CampaignMain.campaignMain.toUser("AM:" +
                                               Username +
                                               " forced you to send your " +
                                               m.getModelName() +
                                               " to " +
                                               receivingPlayer +
                                               ".", sendingPlayer, true);
        //server.MWLogger.modLog(Username + " transfers a " + m.getModelName() + "from " + sendingPlayer + " to " + receivingPlayer);
        CampaignMain.campaignMain.doSendModMail("NOTE",
              Username + " transfers a " + m.getModelName() + " from " + sendingPlayer + " to " + receivingPlayer);

        //then do it ...
        sender.removeUnit(m.getId(), true);
        receiver.addUnit(m, true);

    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

}//end AdminTransfer
