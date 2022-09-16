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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

/**
 * A command to create a unit
 * <p>
 * This command allows an admin to create a unit, which is then dropped into his hangar
 * 
 * @version 2016.10.26
 */
public class CreateUnitCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "filename#flavortext#gunnery#pilot#weightclass#skill1,skill2,skill3[Random]";
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
		
        SPlayer p = CampaignMain.cm.getPlayer(Username);
		String filename;
		String FlavorText;
		String gunnery;
		String piloting;
		String skillTokens = null;

		try {
			filename = command.nextToken();
			FlavorText = command.nextToken();
			gunnery = command.nextToken();
			piloting = command.nextToken();
		}catch(Exception ex) {
			CampaignMain.cm.toUser(syntax, Username);
			return;
		}
		
		int weight = SUnit.LIGHT;
		
		if ( command.hasMoreElements() )
			weight = Integer.parseInt(command.nextToken());

		if (command.hasMoreTokens()){
			skillTokens = command.nextToken();
		}
		//cm.setPilot(pilot);
		SUnit cm = SUnit.create(filename, FlavorText, Integer.parseInt(gunnery), Integer.parseInt(piloting), weight, skillTokens);
		p.addUnit(cm, true);
		CampaignMain.cm.toUser("Unit created: " + filename + " " + FlavorText + " " + gunnery + " " + piloting+" "+cm.getPilot().getSkillString(true) + ". ID #" + cm.getId(),Username,true);
		//server.MWLogger.modLog(Username + " created a unit: " + filename + " " + FlavorText + " " + gunnery + " " + piloting+" "+pilot.getSkillString(true));	
		CampaignMain.cm.doSendModMail("NOTE",Username + " created a unit: " + filename + " " + FlavorText + " " + gunnery + " " + piloting+" "+cm.getPilot().getSkillString(true));
	}
}
