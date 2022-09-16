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

package server.campaign.commands.leader;

import java.util.StringTokenizer;
import java.util.Vector;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class FactionLeaderMuteCommand implements Command {

    int accessLevel = CampaignMain.cm.getIntegerConfig("factionLeaderLevel");

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
		
		SPlayer leader = CampaignMain.cm.getPlayer(Username);
		SPlayer p = null;
		
		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper command. Try: /c factionleadermute#PlayerName", Username, true);
			return;
		}
		
		if (p == null) {
			CampaignMain.cm.toUser("AM:Couldn't find a player with that name.", Username, true);
			return;
		}
		
		if ( !leader.getMyHouse().getName().equalsIgnoreCase(p.getMyHouse().getName()) ){
			CampaignMain.cm.toUser("AM:You are not in the same faction as "+p.getName()+ ". You may not mute them!",Username,true);
			return;
		}
		
		
		Vector<String> factionIgnores = CampaignMain.cm.getServer().getFactionLeaderIgnoreList();
		
		//do the actual mute
		if (factionIgnores.indexOf(p.getName()) == -1) {
			factionIgnores.add(p.getName());
			//server.MWLogger.modLog(Username + " faction muted " + p.getName());
			CampaignMain.cm.doSendModMail("NOTE",Username + " faction muted " + p.getName());
			CampaignMain.cm.getServer().sendChat(Username + " muted " + p.getName() + " (faction mute).");
		} else { //unmute
			factionIgnores.remove(p.getName());
			MWLogger.modLog(Username + " faction unmuted " + p.getName());
			CampaignMain.cm.doSendModMail("NOTE",Username + " faction unmuted " + p.getName());
			CampaignMain.cm.getServer().sendChat(Username + " unmuted " + p.getName() + " (faction mute).");
		}
		
	}
}