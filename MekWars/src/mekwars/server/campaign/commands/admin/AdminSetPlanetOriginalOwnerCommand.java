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
 * Created on 10.27.2005
 *  
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class AdminSetPlanetOriginalOwnerCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#Faction Name";
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
		
		String PlanetName = command.nextToken();
        String originalOwner = CampaignMain.cm.getConfig("NewbieHouseName");
        SPlanet planet = CampaignMain.cm.getPlanetFromPartialString(PlanetName,Username);
        
        if ( planet == null )
            return;
        
        try{
            if ( command.hasMoreTokens() )
                originalOwner = command.nextToken();
        }catch (Exception ex){}
        
        
        planet.setOriginalOwner(originalOwner);
        
		CampaignMain.cm.toUser(planet.getName()+"'s original owner set to: " +originalOwner + ".",Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " changed " + PlanetName+" 's original owner to: " + originalOwner + ".");
		planet.updated();
	}
}
