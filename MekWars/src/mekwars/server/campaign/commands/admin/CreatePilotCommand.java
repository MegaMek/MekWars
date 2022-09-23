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

package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.SPilotSkills;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;

// syntanx /c createunit#filename#flavortext#gunnery#pilot#skill1,skill2,skill3
public class CreatePilotCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "target player#gunnery#pilot#weightclass#type#skill1,skill2,skill3[Random]";
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
		
		
        SPlayer p = CampaignMain.cm.getPlayer(Username);
        SHouse h = p.getMyHouse();
        
        if ( !h.getBooleanConfig("AllowPersonalPilotQueues") )
            return;

        SPlayer target;
		String gunnery;
		String piloting;
		int type;
        int weight;

		try {
			target = CampaignMain.cm.getPlayer(command.nextToken());
			gunnery = command.nextToken();
			piloting = command.nextToken();
			type = SUnit.getTypeIDForName(command.nextToken());
	        weight = SUnit.getWeightIDForName(command.nextToken());
		}catch(Exception ex) {
			CampaignMain.cm.toUser(syntax, Username);
			return;
		}
		
		if ( target == null ){
		    CampaignMain.cm.toUser("Cannot find target player", Username);
		}
		
		
        if ( p.getPersonalPilotQueue().getPilotQueue(type,weight).size() > 0 
                && !h.getBooleanConfig("AllowPlayerToBuyPilotsFromHouseWhenPoolIsFull") ){
            CampaignMain.cm.toUser("AM:"+target.getName()+" does not have enough room for a new pilot.",Username,true);
            return;
        }

		SPilot pilot = null;
	    pilot = new SPilot(SPilot.getRandomPilotName(CampaignMain.cm.getR()),Integer.parseInt(gunnery),Integer.parseInt(piloting));
		
        pilot.setCurrentFaction("Common");
		if (command.hasMoreTokens()){
			String skillTokens = command.nextToken();
			StringTokenizer skillList = new StringTokenizer(skillTokens,",");
			
			while (skillList.hasMoreTokens()){
				String skill = skillList.nextToken();
				SPilotSkill pSkill = null; 
				if ( skill.equalsIgnoreCase("random") )
					pSkill = SPilotSkills.getRandomSkill(pilot, type );
				else					
					pSkill = SPilotSkills.getPilotSkill(skill);
				
				if ( pSkill != null ){
                    if ( pSkill instanceof TraitSkill){
                        ((TraitSkill)pSkill).assignTrait(pilot);
                    }
                    pSkill.addToPilot(pilot);
                    pSkill.modifyPilot(pilot);
                }
			}
		}
		
		target.getPersonalPilotQueue().addPilot(pilot,type, weight);
        CampaignMain.cm.toUser("PL|AP2PPQ|"+type+"|"+weight+"|"+pilot.toFileFormat("#",true),target.getName(),false);

		CampaignMain.cm.toUser("AM:"+SUnit.getWeightClassDesc(weight)+ " " + SUnit.getTypeClassDesc(type) +" Pilot created: " + pilot.getName()+ " for " + target.getName() + " (" + gunnery + "/" + piloting+") ["+pilot.getSkillString(true) + "].",Username);
        CampaignMain.cm.toUser("AM:"+Username+" has created a "+SUnit.getWeightClassDesc(weight)+ " " + SUnit.getTypeClassDesc(type) +" pilot for you.  " + pilot.getName()+ " (" + gunnery + "/" + piloting+") ["+pilot.getSkillString(true)+"]",target.getName());
		CampaignMain.cm.doSendModMail("NOTE",Username + " created a "+SUnit.getWeightClassDesc(weight)+ " " + SUnit.getTypeClassDesc(type) +" pilot for " + target.getName() + ".  " + pilot.getName() + " (" + gunnery + "/" + piloting+") ["+pilot.getSkillString(true)+"]");
	}
}
