/*
 * MekWars - Copyright (C) 2006 
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
import java.util.TreeMap;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlanet;
import server.campaign.commands.Command;


public class AdminSetPlanetOpFlagsCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Planet Name#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!";
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
		
        if ( !command.hasMoreTokens() ){
            CampaignMain.cm.toUser("Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!" , Username);
            return;
        }
        
		SPlanet planet =  (SPlanet) CampaignMain.cm.getData().getPlanetByName(command.nextToken());
		if ( planet == null ) {
			CampaignMain.cm.toUser("Unknown Planet",Username,true);
			return;
		}
		
        TreeMap<String, String> map = new TreeMap<String, String>();
        try{
            while (command.hasMoreTokens()){
                String key = command.nextToken();
                if ( CampaignMain.cm.getData().getPlanetOpFlags().containsKey(key) )
                    map.put(key, CampaignMain.cm.getData().getPlanetOpFlags().get(key));
                else
                    CampaignMain.cm.toUser(key+" is not a valid plant ops flag!", Username);
            }
        }catch (Exception ex){
            CampaignMain.cm.toUser("Syntax AdminSetPlanetOpFlags#Planet#FlagCode#FlagCode#...<br>NOTE: you can repeat FlagCode multiple times.<br>NOTE:This will reset all the flags for the planet to these flags!" , Username);
            return;
        }
        
        planet.setPlanetFlags(map);
		CampaignMain.cm.toUser("Op flags set for "+planet.getName(),Username,true);
		//server.MWLogger.modLog(Username + " set the op flags for "+planet.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set the op flags for "+planet.getName());
		
        planet.updated();
	}
}