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

/**
 * @author Torren (Jason Tighe)
 * Created on 11.08.2005
 *  Allows the SO to change the number of bays/techs a planet offers the owning faction
 *  on the fly.
 */

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.commands.Command;

public class SetPlanetWareHouseCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#Number Of Warehouses";
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
        int warehouses = 0;
        SPlanet planet = CampaignMain.cm.getPlanetFromPartialString(PlanetName,Username);
        
        if ( planet == null ){
            CampaignMain.cm.toUser(PlanetName+" not found.",Username,true);
            return;
        }

        try{
            if ( command.hasMoreTokens() )
                warehouses = Integer.parseInt(command.nextToken());
        }catch (Exception ex){
            CampaignMain.cm.toUser("Invalid Syntax: SetPlanetWareHouse#PlanetName#NumberOfWareHouses",Username,true);
            return;
        }
        
        
        planet.setBaysProvided(warehouses);
        
        if ( planet.getOwner() != null ) {
        	SHouse house = planet.getOwner();
        	house.removePlanet(planet);
        	house.addPlanet(planet);
        	house.updated();
        }
        	
		CampaignMain.cm.toUser(planet.getName()+" has had its number of warehouses set to "+planet.getBaysProvided(),Username,true);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set planet " + PlanetName+" warehouses to "+planet.getBaysProvided());
		planet.updated();        
	}//end process
}//end class
