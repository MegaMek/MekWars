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

package server.campaign.commands.mod;

import java.util.StringTokenizer;

import common.Unit;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.commands.Command;

//Syntax sethousebasepilotskills house#pilotType#Gunnery#Piloting
public class SetHouseBasePilotSkillsCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	String syntax = "Faction Name#Pilot Type#Gunnery#Piloting";
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

        SHouse house;
        int pilotType;
        int gunnery;
        int piloting;
        
        try{
            house = CampaignMain.cm.getHouseFromPartialString(command.nextToken(), Username);
            pilotType = Integer.parseInt(command.nextToken());
            gunnery = Integer.parseInt(command.nextToken());
            piloting = Integer.parseInt(command.nextToken());
        }catch (Exception ex ){
            CampaignMain.cm.toUser("Invalid Syntax: sethousebasepilotskills house#pilotType#Gunnery#Piloting", Username);
            return;
        }

        if ( house == null )
            return;
        
        if ( pilotType >= Unit.MAXBUILD || pilotType < 0){
            CampaignMain.cm.toUser("Invalid unit type:<br>Mek "+Unit.MEK+"<br>Vehicle "+Unit.VEHICLE+"<br>Infantry "+Unit.INFANTRY+"<br>Battle Armor "+Unit.BATTLEARMOR+"<br>ProtoMek "+Unit.PROTOMEK+"<br>Aero "+Unit.AERO, Username);
            return;
        }
        
        house.getPilotQueues().setBaseGunnery(gunnery, pilotType);
        house.getPilotQueues().setBasePiloting(piloting, pilotType);
        
		//log, and inform mods.
		CampaignMain.cm.toUser("You set have set the gunnery and piloting for unit "+Unit.getTypeClassDesc(pilotType)+" for house "+house.getName()+" to "+gunnery+"/"+piloting,Username);
		CampaignMain.cm.doSendModMail("NOTE",Username + " has set the gunnery and piloting for unit "+Unit.getTypeClassDesc(pilotType)+" for house "+house.getName()+" to "+gunnery+"/"+piloting+ ".");
		
	}//end process
}