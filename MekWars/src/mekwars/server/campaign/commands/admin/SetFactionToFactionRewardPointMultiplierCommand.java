/*
 * MekWars - Copyright (C) 2007 
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
import server.campaign.SHouse;
import server.campaign.commands.Command;

public class SetFactionToFactionRewardPointMultiplierCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#Faction Name#Multipler";
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
		
		SHouse faction1 = null;
		SHouse faction2 = null;
		double multiplier = 0.0;
		try {
			faction1 = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			faction2 = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
			multiplier = Double.parseDouble(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c SetFactionToFactionRewardPointMultiplier#"+syntax, Username, true);
			return;
		}
		
		if (faction1 == null || faction2 == null) {
			return;
		}

		String rewardMultiplier = faction1.getName()+"To"+faction2.getName()+"RewardPointMultiplier";
		
		CampaignMain.cm.getConfig().setProperty(rewardMultiplier,Double.toString(multiplier));

		CampaignMain.cm.toUser("You set the " + CampaignMain.cm.getConfig("RPShortName") + " multipler for "+faction1.getName()+" to "+faction2.getName()+" to "+multiplier,Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set " + CampaignMain.cm.getConfig("RPShortName") + " multipler for "+faction1.getName()+" to "+faction2.getName()+" to "+multiplier);

	}
}