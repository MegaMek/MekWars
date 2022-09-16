/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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

/*
 * Created on 10.26.2005
 *  
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminSetHomeWorldCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#[true/false]";
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
		
        boolean homeworld = false;
        SPlanet planet = null ;
        
        try{
        	planet = CampaignMain.cm.getPlanetFromPartialString(command.nextToken(),Username);
            if ( command.hasMoreTokens() )
                homeworld = Boolean.parseBoolean(command.nextToken());
            else
            	homeworld = !planet.isHomeWorld();
        }catch (Exception ex){
        	CampaignMain.cm.toUser("Invalid syntax. Try: adminsethomeworld#Planet#[true/false]",Username);
        }
        
        if ( planet == null )
            return;
        
        planet.setHomeWorld(homeworld);
        
		CampaignMain.cm.toUser(planet.getName()+"'s homeworld status set to: " + homeworld + ".",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set " + planet.getName() +"'s homeworld status to: " + homeworld + ".");
		planet.updated();
	}
}
