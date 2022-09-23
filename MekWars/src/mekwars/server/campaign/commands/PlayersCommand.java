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

package server.campaign.commands;

import java.util.StringTokenizer;
import java.util.TreeMap;

import common.House;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;

public class PlayersCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}
	
	//NOTE: There Are Problems WIth This Code and the display of MERCENARY PLAYERS
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		String toSend = "";
		
		/*
		 * Fighting players are always broken into a 
		 * separate grouping, so we'll do them first.
		 */
		toSend += "<br><h2>Fighting Players:</h2><br>";
		for (House vh : CampaignMain.cm.getData().getAllHouses()) {
			
			SHouse h = (SHouse)vh;
			if (h.getFightingPlayers().size() > 0) {
				
				//alpha-sort the players
				TreeMap<String,SPlayer> sortedPlayers = new TreeMap<String,SPlayer>();
				sortedPlayers.putAll(h.getFightingPlayers());
				
				toSend += "<b>" + h.getColoredNameAsLink() + ":</b> ";
				for (SPlayer currP : sortedPlayers.values())
					toSend += currP.getName() + ", ";
				
				//strip the last comma
				toSend = toSend.substring(0, toSend.length() - 2) + "<br>";
			}		
		}
		
		/*
		 * Now, we care about whether or not activity status is
		 * being hidden. If not, split reserve and active lists.
		 */
		if (!(Boolean.parseBoolean(CampaignMain.cm.getConfig("HideActiveStatus")))) {
			
			toSend += ("<br><h2>Active Duty Players:</h2><br>");
			for (House vh : CampaignMain.cm.getData().getAllHouses()) {
				
				SHouse h = (SHouse)vh;
				if (h.getActivePlayers().size() > 0) {
					
					//alpha-sort the players
					TreeMap<String,SPlayer> sortedPlayers = new TreeMap<String,SPlayer>();
					sortedPlayers.putAll(h.getActivePlayers());
					
					toSend += ("<b>" + h.getColoredNameAsLink() + ":</b> ");
					for (SPlayer currP : sortedPlayers.values())
						toSend += (currP.getName() + ", ");
					
					//strip the last comma
					toSend = toSend.substring(0, toSend.length() - 2) + "<br>";
				}		
			}
			
			toSend += ("<br><h2>Reserve Duty Players:</h2><br>");
			for (House vh : CampaignMain.cm.getData().getAllHouses()) {
				
				SHouse h = (SHouse)vh;
				if (h.getReservePlayers().size() > 0) {
					
					//alpha-sort the players
					TreeMap<String,SPlayer> sortedPlayers = new TreeMap<String,SPlayer>();
					sortedPlayers.putAll(h.getReservePlayers());
					
					toSend += ("<b>" + h.getColoredNameAsLink() + ":</b> ");
					for (SPlayer currP : sortedPlayers.values()) {
						if (currP.isInvisible()) {continue;}
						toSend += (currP.getName() + ", ");
					}
					
					//strip the last comma
					toSend = toSend.substring(0, toSend.length() - 2) + "<br>";
				}		
			}
			
		}//end if(split reserve and active)
		
		/*
		 * Else, we're hiding the active status. Show Players who are online
		 * in all houses, but not fighting. Italicize the active players in
		 * the requestor's house.
		 */
		else {
			
			TreeMap<String,SPlayer> combinedTable;
			
			toSend += ("<br><h2>Online Players (Not Fighting):</h2><br>");
			for (House vh : CampaignMain.cm.getData().getAllHouses()) {
				
				SHouse h = (SHouse)vh;
				combinedTable = new TreeMap<String,SPlayer>();
				combinedTable.putAll(h.getReservePlayers());
				combinedTable.putAll(h.getActivePlayers());
				
				boolean playersFaction = h.equals(CampaignMain.cm.getPlayer(Username).getMyHouse());
				boolean isAdmin = CampaignMain.cm.getServer().isAdmin(Username);
				
				if (combinedTable.size() > 0) {
					
					//add all players
					toSend += ("<b>" + h.getColoredNameAsLink() + ":</b> ");
					for (SPlayer currP : combinedTable.values()) {
						
						if (currP.isInvisible())
							continue;
						
						if ((playersFaction || isAdmin) && currP.getDutyStatus() == SPlayer.STATUS_ACTIVE)
							toSend += ("<i>" + currP.getName() + "</i>, ");
						
						else
							toSend += (currP.getName() + ", ");
						
					}
					
					//strip the last comma. send number of active players to mods and admins
					toSend = toSend.substring(0, toSend.length() - 2);
					if (isAdmin || playersFaction)
						toSend += " (Active: " + h.getActivePlayers().size() + ")<br>";
					else
						toSend += "<br>";
				}		
			}
			
		}
		
		//send to the requestor
		CampaignMain.cm.toUser("SM|" + toSend,Username,false);
		
	}//end process()
}//end PlayersCommand