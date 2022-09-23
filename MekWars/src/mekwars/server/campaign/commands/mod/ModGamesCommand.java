/*
 * MekWars - Copyright (C) 2005
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

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;
import server.campaign.operations.ShortOperation;


public class ModGamesCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "[Faction]";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
		if(userLevel < getExecutionLevel()) {
			CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
			return;
		}
		
		//load player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("Null player. Contact an administrator to report this, immediately!",Username,true);
			return;
		}
		
		//sort for a specific faction?
		boolean factionSort = false;
		String factionName = "";
		
		//look for a filtration string
		if (command.hasMoreElements()) {
			try {
				factionName = command.nextToken().toLowerCase();
				if (!factionName.equals("") && !factionName.startsWith(" "))
					factionSort = true;
			} catch (Exception ex) {
				CampaignMain.cm.toUser("Games command failed. Check your input. It should be:" +
						"/c modames (to get all games) or /c modgames#faction (for a filtered list)",Username,true);
				return;
			}
		}
		
		//setup headers
		String runningGames = "<h2>Running Games (MOD VIEW):</h2><br>";
		String finishedGames = "<h2>Finished Games (MOD VIEW):</h2><br>";
		
		int runningGamesCount = 0;
		int finishedGamesCount = 0;
		
		for (ShortOperation currO : CampaignMain.cm.getOpsManager().getRunningOps().values()) {
			
			//filter, if we're sorting by faction
			if (factionSort && !currO.hasPlayerWhoseHouseBeginsWith(factionName))
				continue;
			
			//get the infoString appropriate for the player
			String currInfo = "";
			currInfo += currO.getInfo(true,true) + "<br>";
			
			//if the game is finished, add it to finishedGames
			if (currO.getStatus() == ShortOperation.STATUS_FINISHED) {
				finishedGames += currInfo;
				finishedGamesCount++;
			}
			
			//add waiting, running and reporting games to runningGames
			else {
				runningGames += currInfo;
				runningGamesCount++;
			}
		}
		
		if (runningGamesCount == 0)
			runningGames += "- None<br>";
		
		if (finishedGamesCount ==0)
			finishedGames += "- None<br>";
		
		CampaignMain.cm.toUser("SM|" + runningGames + finishedGames, Username, false);
		
		//show use of command to mods.
		//server.MWLogger.modLog(Username + " used /c modgames.");
		CampaignMain.cm.doSendModMail("NOTE",Username + " used /c modgames.");
	}
}