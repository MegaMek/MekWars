/*
 * MekWars - Copyright (C) 2006 
 *
 * Original author - jtighe (torren@users.sourceforge.net)
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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax updateoperations
public class UpdateOperationsCommand implements Command {
    
    int accessLevel = IAuthenticator.ADMIN;
    public int getExecutionLevel(){return accessLevel;}
    public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "";
	public String getSyntax() { return syntax;}

    public void process(StringTokenizer command,String Username) {
        
        //access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if(userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
            return;
        }
        
        if (!CampaignMain.cm.getBooleanConfig("CampaignLock"))  {
            CampaignMain.cm.toUser("The campaign must be locked before you can update the operations.",Username,true);
            return;
        }
        
        CampaignMain.cm.getOpsManager().loadOperations();
        
        CampaignMain.cm.doSendModMail("NOTE",Username+" ops manager updated.");
        
        CampaignMain.cm.updateAllOnlinePlayerArmies();
        CampaignMain.cm.doSendToAllOnlinePlayers("PL|UDAO|1",false);
    }
}//end RetrieveOperation
