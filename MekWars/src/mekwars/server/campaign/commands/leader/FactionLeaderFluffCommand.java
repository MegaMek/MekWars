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

import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class FactionLeaderFluffCommand implements Command {
	
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
		String fluff = "";
		
		try {
			p = CampaignMain.cm.getPlayer(command.nextToken());
		} catch (Exception e) {
			CampaignMain.cm.toUser("AM:Improper command. Try: /c factionleaderfluff#PlayerName#text", Username, true);
			return;
		}
		
        if ( command.hasMoreElements() )
            fluff = command.nextToken();
        else
            fluff = null;
        
		if (p == null) {
			CampaignMain.cm.toUser("AM:Couldn't find a player with that name.", Username, true);
			return;
		}
		
		if (!leader.getMyHouse().getName().equalsIgnoreCase(p.getMyHouse().getName()) ){
			CampaignMain.cm.toUser("AM:You are not in the same faction as "+p.getName()+ " therefore you maynot change their fluff!",Username,true);
			return;
		}

        if ( fluff == null )
            p.setFluffText("");
        else{
    		if ( fluff.indexOf("~") > 0 ||
    		        fluff.indexOf("$") >0 ||
    		        fluff.indexOf("#") >0 ||
    		        fluff.indexOf("|") > 0){
    			CampaignMain.cm.toUser("AM:Illegal characters in the fluff text try again without '|','$','#', or '~' characters",Username,true);
    			return;
    		}
    		
    		//set the text
    		p.setFluffText(fluff);
        }
        
		CampaignMain.cm.toUser("AM:New fluff text for " + p.getName() + ": " + fluff,Username,true);
		CampaignMain.cm.toUser(Username + " set your fluff to: " + fluff,p.getName(),true);
		//server.MWLogger.modLog(Username + " set " + p.getName() + "'s fluff to '" + fluff + "'.");
		CampaignMain.cm.doSendModMail("NOTE",Username + " set " + p.getName() + "'s fluff to '" + fluff + "'.");
        CampaignMain.cm.doSendToAllOnlinePlayers("PI|FT|"+p.getName()+"|"+p.getFluffText(),false);
	}
}