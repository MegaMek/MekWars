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

package server.campaign.commands;

import java.util.StringTokenizer;
import java.util.TreeMap;

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.operations.ShortOperation;

public class GamesCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		//load player
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		if (p == null) {
			CampaignMain.cm.toUser("AM:Null player. Contact an administrator to report this, immediately!",Username,true);
			return;
		}
		
		//sort for a specific faction?
		boolean factionSort = false;
		String factionName = "";
		
		//look for a filtration string
		if (command.hasMoreElements()) {
			try {
				factionName = command.nextToken().toLowerCase();
				if (factionName.trim().length() > 0)
					factionSort = true;
			} catch (Exception ex) {
				CampaignMain.cm.toUser("AM:Games command failed. Check your input. It should be:" +
						"/c games (to get all games) or /c games#faction (for a filtered list)",Username,true);
				return;
			}
		}
		
		//setup headers
		String runningGames = "<h2>Running Games:</h2>";
		String finishedGames = "<br><h2>Finished Games:</h2>";
		
		int runningGamesCount = 0;
		TreeMap<Long,ShortOperation> timeSort = new TreeMap<Long,ShortOperation>();
		
		for (ShortOperation currO : CampaignMain.cm.getOpsManager().getRunningOps().values()) {
			
			//filter, if we're sorting by faction
			if (factionSort && !currO.hasPlayerWhoseHouseBeginsWith(factionName))
				continue;
			
			//if the game is finished, add it to a finished map, using age as a key
			if (currO.getStatus() == ShortOperation.STATUS_FINISHED) 
				timeSort.put(currO.getCompletionTime(), currO);
			
			//add waiting, running and reporting games to runningGames
			else {
				
				//get the infoString appropriate for the player
				String currInfo = "";
				if (currO.hasPlayerFrom(p.getMyHouse()))
					currInfo += "<br>" + currO.getInfo(true,false);
				else
					currInfo += "<br>" + currO.getInfo(false,false);
				
				runningGames += currInfo;
				runningGamesCount++;
			}
			
		}
		
		if (runningGamesCount == 0)
			runningGames += "<br>- None";

		//list finished games
		if (timeSort.size() == 0)
			finishedGames += "<br>- None";
		else {
			for (ShortOperation currO : timeSort.values())
				finishedGames += "<br>" + currO.getInfo(true,false);
		}
		
		CampaignMain.cm.toUser("SM|" + runningGames + finishedGames, Username, false);
	}
}
