/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author - nmorris (urgru@users.sourceforge.net)
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

package server.campaign.commands.mod;


import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;
import server.campaign.util.ExclusionList;

public class ModNoPlayCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "mode[add/remove]#lister#excludee";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		//access level check
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		String mode = "";
		try {
			mode = command.nextToken().toLowerCase();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c noplay#mode#lister#excludee", Username, true);
			return;
		}
		
		String listerName = "";
		try {
			listerName = command.nextToken().toLowerCase();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c noplay#mode#lister#excludee", Username, true);
			return;
		}
		
		String excludeName = "";
		try {
			excludeName = command.nextToken().toLowerCase();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper format. Try: /c noplay#mode#lister#excludee", Username, true);
			return;
		}
		
		//get the listing SPlayer
		SPlayer lister = CampaignMain.cm.getPlayer(listerName);
		if (lister == null) {
			CampaignMain.cm.toUser("Couldn't find " + listerName, Username, true);
			return;
		}
		
		//load the exlusion list
		ExclusionList exList = lister.getExclusionList();
		if (exList == null) {
			CampaignMain.cm.toUser("ERROR. " + listerName + "'s no-play list was null.", Username, true);
			return;
		}
		
		//load the target players current exclude status
		int exclusionStatus = exList.checkExclude(excludeName);
		
		//ADD BLOCK
		if (mode.equals("add")) {
			
            boolean playerExists = false;
            playerExists = new File("./campaign/players/" + excludeName.toLowerCase() + ".dat").exists();
          
            if (!playerExists) {
                CampaignMain.cm.toUser(excludeName + " does not have a player file. cannot add to your no-play list.", Username, true);
                return;
            }

            //check to make sure the player isn't no-play'ing himself
			if (listerName.toLowerCase().equals(excludeName)) {
				CampaignMain.cm.toUser("You can't put someone on his own no-play list. Jackass.", Username, true);
				return;
			}
			
			//if the name is already admin excluded, break out
			if (exclusionStatus == ExclusionList.ADMIN_EXCLUDED) {
				CampaignMain.cm.toUser(excludeName + " is already on " + listerName + "'s mod/admin no-play list.", Username, true);
				return;
			}
			
			//if the name is player excluded, erase it in prep for move to the admin list
			if (exclusionStatus == ExclusionList.PLAYER_EXCLUDED) {
				exList.removeExclude(true, excludeName);//clear all priors
				CampaignMain.cm.toUser(Username + " removed " + excludeName + " from your no-play list.", listerName, true);
				CampaignMain.cm.toUser("PL|PEU|"+lister.getExclusionList().playerExcludeToString("$"),listerName,false);
			}
			
			//add the exclude and tell the player, then return
			try {
				exList.addExclude(true, excludeName);
				CampaignMain.cm.toUser(Username + " added " + excludeName + " to your mod/admin no-play list. You cannot alter this entry.", listerName, true);
				CampaignMain.cm.toUser(Username + " added you to " + listerName + "'s mod/admin no-play list. He cannot alter this entry.", excludeName, true);
				CampaignMain.cm.toUser(excludeName + " added to " + listerName + "'s mod/admin no-play list.", Username, true);
				CampaignMain.cm.toUser("PL|AEU|"+lister.getExclusionList().adminExcludeToString("$"),listerName,false);
				lister.setSave();
				
				//also inform mod channels
				//server.MWLogger.modLog(Username + " added " + excludeName + " to " + listerName + "'s mod/admin no-play list.");
				CampaignMain.cm.doSendModMail("NOTE",Username + " added " + excludeName + " to " + listerName + "'s mod/admin no-play list.");
				
			} catch (Exception e) {
				CampaignMain.cm.toUser("Error while adding " + excludeName + " to " + listerName + "'s no-play list.", Username, true);
			}
		}//end if(mode == add)
		
		else if (mode.equals("remove")) {
			
			if (exclusionStatus == ExclusionList.NO_EXCLUSION) {
				CampaignMain.cm.toUser(excludeName + " is not on either of " + listerName + "'s no-play lists.", Username, true);
				return;
			}
			
			//name is there, so remove it.
			try {
				exList.removeExclude(true, excludeName);
				if (exclusionStatus == ExclusionList.ADMIN_EXCLUDED) {
					CampaignMain.cm.toUser(Username + " removed " + excludeName + " from your Mod/Admin no-play list.", listerName, true);
					CampaignMain.cm.toUser(Username + " removed you from " + listerName + "'s Mod/Admin no-play list.", excludeName, true);
					CampaignMain.cm.toUser("Your removed " + excludeName + " from " + listerName + "'s Mod/Admin no-play list.", Username, true);
					CampaignMain.cm.toUser("PL|AEU|"+lister.getExclusionList().adminExcludeToString("$"),listerName,false);
					
					//also inform mod channels
					//server.MWLogger.modLog(Username + " removed " + excludeName + " from " + listerName + "'s Mod/Admin no-play list.");
					CampaignMain.cm.doSendModMail("NOTE",Username + " removed " + excludeName + " from " + listerName + "'s Mod/Admin no-play list.");
				}
				else {
					CampaignMain.cm.toUser(Username + " removed " + excludeName + " from your no-play list.", listerName, true);
					CampaignMain.cm.toUser("You removed " + excludeName + " from " + listerName + "'s standard no-play list.", Username, true);
					//NOTE: the player removed isn't informed, since this was a standard list entry and, thus, supposed to be anonymous.
					CampaignMain.cm.toUser("PL|PEU|"+lister.getExclusionList().playerExcludeToString("$"),listerName,false);
					
					//also inform mod channels
					//server.MWLogger.modLog(Username + " removed " + excludeName + " from " + listerName + "'s standard no-play list.");
					CampaignMain.cm.doSendModMail("NOTE",Username + " removed " + excludeName + " from " + listerName + "'s standard no-play list.");
				}
				lister.setSave();
			} catch (Exception e) {
				CampaignMain.cm.toUser("Error while removing " + excludeName + " from " + listerName + "'s no-play list.", Username, true);
				return;
			}
		}//end (if mode == remove)
		
		else {//mode is gibberish. alert the user.
			CampaignMain.cm.toUser("Improper format. Try: /c noplay#mode#lister#excludee", Username, true);
			return;
		}
		
	}
}