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

import common.campaign.pilot.skills.PilotSkill;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.skills.EdgeSkill;

public class SetEdgeSkillsCommand implements Command {
	
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
		
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		
		int unitid= 0;//ID# of the mech which is to set autoeject;
        SPilot pilot = null;
        boolean edge_when_tac = true;
        boolean edge_when_ko = true;
        boolean edge_when_headhit = true;
        boolean edge_when_explosion = true;
        
		try {
			unitid= Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:SetEdgeSkills command failed. Check your input. It should be something like this: /c SetEdgeSkills#unitid#true/false#true/false#true/false#true/false",Username,true);
			return;
		}//end catch
		
		try {
            edge_when_tac = Boolean.parseBoolean(command.nextToken());
            edge_when_ko = Boolean.parseBoolean(command.nextToken());
            edge_when_headhit = Boolean.parseBoolean(command.nextToken());
            edge_when_explosion = Boolean.parseBoolean(command.nextToken());
		}//end try
		catch (Exception ex){
			CampaignMain.cm.toUser("AM:SetAutoEject Command failed. Check your input. It should be something like this: /c SetEdgeSkills#unitid#true/false#true/false#true/false#true/false",Username,true);
			return;
		}//end catch
		
		SUnit unit = p.getUnit(unitid);
        pilot = (SPilot)unit.getPilot();

        if (!pilot.getSkills().has(PilotSkill.EdgeSkillID))
            return;
        
        ((EdgeSkill)pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID)).setTac(edge_when_tac);
        ((EdgeSkill)pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID)).setKO(edge_when_ko);
        ((EdgeSkill)pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID)).setHeadHit(edge_when_headhit);
        ((EdgeSkill)pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID)).setExplosion(edge_when_explosion);

        CampaignMain.cm.toUser("PL|UU|"+unit.getId()+"|"+unit.toString(true),Username,false);

        CampaignMain.cm.toUser("AM:Edge set for "+ unit.getModelName(),Username,true);
		
	}//end process() 
}//end SetEdgeSkillsCommand class

