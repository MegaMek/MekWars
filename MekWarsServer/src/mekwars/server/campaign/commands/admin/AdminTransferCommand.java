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

public class AdminTransferCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;
    String syntax = "Sending Player#Receiving Player#Unit ID";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

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

        String sendingPlayer = null;
        String receivingPlayer = null;
        int mechid = -1;

        try {
            sendingPlayer = (String) command.nextElement();
            receivingPlayer = (String) command.nextElement();
            mechid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c admintransfer#from#to#id",
                  Username,
                  true);
            return;
        }


        server.campaign.SPlayer sender = server.campaign.CampaignMain.cm.getPlayer(sendingPlayer);
        server.campaign.SPlayer receiver = server.campaign.CampaignMain.cm.getPlayer(receivingPlayer);

        if (sender == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Sending player could not be found. Try again.", Username, true);
            return;
        }

        if (receiver == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Receiving player could not be found. Try again.",
                  Username,
                  true);
            return;
        }

        server.campaign.SUnit m = sender.getUnit(mechid);
        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Sender doesn't have a unit with ID# " + mechid + ".",
                  Username,
                  true);
            return;
        }

        //passed all the breaks. discuss the transfer.
        server.campaign.CampaignMain.cm.toUser("AM:You transfered " +
                                                     sendingPlayer +
                                                     "'s " +
                                                     m.getModelName() +
                                                     " to " +
                                                     receiver.getName(), Username, true);
        server.campaign.CampaignMain.cm.toUser("AM:" +
                                                     Username +
                                                     " forced " +
                                                     sendingPlayer +
                                                     " to send you a " +
                                                     m.getModelName() +
                                                     ".", receivingPlayer, true);
        server.campaign.CampaignMain.cm.toUser("AM:" +
                                                     Username +
                                                     " forced you to send your " +
                                                     m.getModelName() +
                                                     " to " +
                                                     receivingPlayer +
                                                     ".", sendingPlayer, true);
        //server.MWLogger.modLog(Username + " transfers a " + m.getModelName() + "from " + sendingPlayer + " to " + receivingPlayer);
        server.campaign.CampaignMain.cm.doSendModMail("NOTE",
              Username + " transfers a " + m.getModelName() + " from " + sendingPlayer + " to " + receivingPlayer);

        //then do it ...
        sender.removeUnit(m.getId(), true);
        receiver.addUnit(m, true);

    }//end process()

}//end AdminTransfer
