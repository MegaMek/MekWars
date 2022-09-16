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

package server.campaign.commands.mod;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import common.House;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.commands.Command;


public class ListMultiPlayerGroupsCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "";
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
		
		//WARNING: CODE EFFECIENCY VERY BAD. COULD CAUSE HIGH SERVER LOAD IF USED OFTEN
		String toSend = "AM:List of Multiplayergroups:";
		
		/*
		 * INCREDIBLY EVIL!
		 */
		Hashtable<String,SPlayer> allPlayers = new Hashtable<String,SPlayer>();
		for (House vh : CampaignMain.cm.getData().getAllHouses()) {
			SHouse h = (SHouse)vh;
			allPlayers.putAll(h.getAllOnlinePlayers());
		}
		/*
		 * End PHENOMENAL EVIL.
		 */
		
		Hashtable<Integer,Vector<SPlayer>> result = new Hashtable<Integer, Vector<SPlayer>>();
		Enumeration<SPlayer> e = allPlayers.elements();
		while (e.hasMoreElements())
		{
			//Check all players for equal Groupentries..
			SPlayer p = e.nextElement();
			if (p.getGroupAllowance() != 0)
			{
				Vector<SPlayer> v;
				if (result.get(p.getGroupAllowance()) == null)
					v = new Vector<SPlayer>(1,1);
				else
					v = result.get(p.getGroupAllowance());
				v.add(p);
				result.put(p.getGroupAllowance(),v);
			}
		}
		
		Enumeration<Integer> groups = result.keys();
		while (groups.hasMoreElements()) {
			Integer GroupID = groups.nextElement();
			Vector<SPlayer> members = result.get(GroupID);
			toSend += "<br>Group #" + GroupID + ":";
			for (int i=0; i < members.size();i++) {
				SPlayer p = (SPlayer)members.elementAt(i);
				toSend += p.getName() + " + ";
			}
			toSend = toSend.substring(0,toSend.lastIndexOf("+")-1);
			
		}
		CampaignMain.cm.toUser(toSend,Username,true);
		
	}
}