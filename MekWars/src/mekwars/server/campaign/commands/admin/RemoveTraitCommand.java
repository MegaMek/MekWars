/*
 * MekWars - Copyright (C) 2005
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands.admin;

import java.util.StringTokenizer;
import java.util.Vector;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;


public class RemoveTraitCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Faction Name#Trait Name#CONFIRM";
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

		//Syntax AddTrait Faction#TraitName#SkillList($)
		
		
	    String faction="common";
		String traitName="none";
		String confirmString ="";
		
		try{
		    faction = command.nextToken();
			traitName = command.nextToken();
			confirmString = command.nextToken();
		}catch (Exception ex){
		    MWLogger.errLog(ex);
		}		

		if ( !confirmString.equals("CONFIRM") )
		    return;
		
		Vector<String> traits = CampaignMain.cm.getFactionTraits(faction.toLowerCase());
		
		for (int pos = 0; pos < traits.size(); pos++){
		    StringTokenizer traitToken = new StringTokenizer(traits.elementAt(pos),"*");
		    if ( traitName.equalsIgnoreCase(traitToken.nextToken())){
		        traits.removeElementAt(pos);
		 		CampaignMain.cm.toUser("Trait "+ traitName+" has been removed.",Username,true);
				CampaignMain.cm.doSendModMail("NOTE",Username + " has removed trait "+ traitName+".");
				CampaignMain.cm.saveFactionTraits(faction,traits);
				return;
		    }
		}
	}
	
}