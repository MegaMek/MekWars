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

import java.util.Enumeration;
import java.util.StringTokenizer;

import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;

public class ExchangePilotInUnitCommand implements Command {
	
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
		
		if ( !Boolean.parseBoolean(CampaignMain.cm.getConfig("AllowPersonalPilotQueues")) )
			return;
		
		if (command.hasMoreElements()) {
			SPlayer p = CampaignMain.cm.getPlayer(Username);
			int mechid = Integer.parseInt(command.nextToken());
			int newPilotId = -1;
			
			if ( command.hasMoreElements())
				newPilotId = Integer.parseInt(command.nextToken());
			
			SUnit m = p.getUnit(mechid);
			if (m != null)
			{
                
                if ( !m.isSinglePilotUnit() ){
                    CampaignMain.cm.toUser("AM:You may not remove that pilot from this unit.",Username,true);
                    return;
                }
                
				if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE && p.getAmountOfTimesUnitExistsInArmies(mechid) > 0)
				{
					CampaignMain.cm.toUser(m.getModelName()+" cannot have its pilot switched out while active and in an exisiting army.",Username,true);
					return;
				}

                if ( m.getPilotIsReparing() ){
                    CampaignMain.cm.toUser(m.getPilot().getName()+" is currently repairing the "+m.getModelName()+" you may not remove them util the job is complete.",Username,true);
                    return;
                }
                
				SPilot pilot = (SPilot)m.getPilot();
				
				int capSize = CampaignMain.cm.getIntegerConfig("MaxAllowedPilotsInQueueToBuyFromHouse");
				
				if ( newPilotId == -1 && p.getPersonalPilotQueue().getPilotQueue(m.getType(), m.getWeightclass()).size() >= capSize ) {
					CampaignMain.cm.toUser("AM:There are no free beds in the barracks "+pilot.getName()+" will have to sleep in his unit.", Username);
					return;
				}
                //issues where protomeks are getting set to the wrong unit type so they become Mek pilots.
                pilot.setUnitType(m.getType());
				if ( !pilot.getName().equals("Vacant") )
				{
					p.getPersonalPilotQueue().addPilot(pilot,m.getWeightclass());
					
                    CampaignMain.cm.toUser("PL|AP2PPQ|"+m.getType()+"|"+m.getWeightclass()+"|"+pilot.toFileFormat("#",true),Username,false);
					CampaignMain.cm.toUser(pilot.getName() + " was moved from your "+ m.getModelName() + " to your barracks.",Username,true);
				}
				
				SPilot p2 = null;
				
				if ( newPilotId > -1 ){
					try{
						p2 = (SPilot) p.getPersonalPilotQueue().getPilot(m.getType(),m.getWeightclass(),newPilotId);
						if ( p2 != null ) {
	                        CampaignMain.cm.toUser("PL|RPPPQ|"+m.getType()+"|"+m.getWeightclass()+"|"+newPilotId,Username,false);
	                        m.setPilot(p2);
	                        CampaignMain.cm.toUser(p2.getName()+" is now assigned to the "+ m.getModelName()  + " [New BV: " + m.getBVForMatch() + "].",Username,true);
						}else {
							CampaignMain.cm.toUser("AM:Invalid Pilot try again!",Username,true);
							return;
						}
					} catch(Exception ex){
						MWLogger.errLog(ex);
						CampaignMain.cm.toUser("AM:Invalid Pilot try again!",Username,true);
						return;
					}
				}
				
				else {
					p2 = new SPilot("Vacant",99,99);
					m.setPilot(p2);
				}
				
				//m.setExperience(new Integer(0)); -- trying to decide if I want to keep it this way or the old way Torren.
				
                //CampaignMain.cm.toUser("PL|PPQ|"+p.getPersonalPilotQueue().toString(true),Username,false);
				CampaignMain.cm.toUser("PL|UU|"+m.getId()+"|"+m.toString(true),Username,false);
				
				Enumeration<SArmy> f = p.getArmies().elements();
				while (f.hasMoreElements()) {
					SArmy currArmy = f.nextElement();
					if (currArmy.getUnit(m.getId()) != null) {
						currArmy.setBV(0);//not null so recalc BV of the army
						CampaignMain.cm.toUser("PL|SAD|"+currArmy.toString(true,"%"),Username,false);
						CampaignMain.cm.getOpsManager().checkOperations(currArmy,true);//update legal operations
					}//end if(army contains the )
				}//end while(more armies to check)
			}
		}
	}
}