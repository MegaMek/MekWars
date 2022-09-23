/*
 * MekWars - Copyright (C) 2007
 * 
 * Original author - jtighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands;

import java.util.StringTokenizer;

import common.SubFaction;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;

public class RequestSubFactionPromotionCommand implements Command {

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

		SPlayer player = CampaignMain.cm.getPlayer(Username);
		SHouse faction = player.getMyHouse();
		String subFactionName;
		SubFaction subFaction = null;
		
		try{
			subFactionName = command.nextToken();
			subFaction = faction.getSubFactionList().get(subFactionName);
		}catch(Exception ex){
			CampaignMain.cm.toUser("AM:Invalid Syntax: /RequestSubFactionPromotion subFactionname", Username);
			return;
		}
		
		if( subFaction == null ){
			CampaignMain.cm.toUser("AM:That SubFaction does not exist for faction "+faction.getName()+".", Username);
			return;
		}
		
		int minELO = Integer.parseInt(subFaction.getConfig("MinELO"));
		int minEXP = Integer.parseInt(subFaction.getConfig("MinExp"));
		
		if ( player.getExperience() < minEXP || player.getRating() < minELO ){
			CampaignMain.cm.toUser("AM:Sorry but you are not skilled enough to join that SubFaction.", Username);
			return;
		}
		
		if ( player.getSubFactionAccess() > Integer.parseInt(subFaction.getConfig("AccessLevel")) ){
			CampaignMain.cm.toUser("AM:Sorry but you cannot demote yourself", Username);
			return;
		}
		
		if ( !player.canBePromoted() ){
            CampaignMain.cm.toUser("AM:Sorry but you've already been premoted once within a "+player.getMyHouse().getIntegerConfig("daysbetweenpromotions")+" day period.", Username);
            return;
		}
		
		if ( CampaignMain.cm.getBooleanConfig("autoPromoteSubFaction") ){
			player.setSubFaction(subFactionName);
			CampaignMain.cm.toUser("PL|SSN|"+subFactionName, Username,false);
			CampaignMain.cm.toUser("HS|CA|0", player.getName(), false);//clear old data
			CampaignMain.cm.toUser(player.getMyHouse().getCompleteStatus(),player.getName(), false);

			CampaignMain.cm.toUser("AM:Congratulations you have been promoted to SubFaction "+subFactionName+".", Username);
			CampaignMain.cm.doSendHouseMail(player.getMyHouse(), "NOTE", player.getName()+" has been promoted to subfaction "+subFactionName+"!");
		}else{
			CampaignMain.cm.toUser("AM:Your request for a promotion has been noted. Someone will be in touch.", Username);
			String msg = Username+" has requested for a promtion to subfaction "+subFactionName+" <a href=\"MEKWARS/c promoteplayer#" + Username + "#" + subFactionName +"\">Click here to promote.</a>";
			faction.sendMessageToHouseLeaders(msg);
			CampaignMain.cm.doSendModMail("NOTE", msg);
			
		}
	
	}
}//end RequestSubFactionPromotionCommand class