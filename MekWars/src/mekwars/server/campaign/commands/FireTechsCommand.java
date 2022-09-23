/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru // nathan.morris@gmail.com)
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

import common.util.UnitUtils;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class FireTechsCommand implements Command {
	
	int accessLevel = 0;
	String syntax = "Syntax: /firetechs numberOfTech#techType";
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
		
        if ( CampaignMain.cm.isUsingAdvanceRepair() ){
            fireAdvanceTechs(command,Username);
            return;
        }

		SPlayer p = CampaignMain.cm.getPlayer(Username);
		
		//use /c firetech#numbertofire
		int numtofire = 1;//default to 1
		
		try {
			numtofire = Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:Couldn't tell how many techs to fire. Check your input. It should be something like this: /c firetechs#3",Username,true);
			return;
		}//end catch
		
		//Check to see if the player is firing too many technicians
		if (p.getTechnicians() < numtofire) {
			CampaignMain.cm.toUser("AM:You tried to fire " + numtofire + " technicians, but you only have " + p.getTechnicians() + " independent techs " +
					"on your payroll. The rest were assigned to your force by your faction and can't be dismissed.",Username,true);
			return;
		}
		
		//Check to see if the player is fighting. Engaged players can't fire techs.
		if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You may not fire technicians while you are engaged! Wait until your units are out of battle and fully repaired!",Username,true);
			return;
		}//end if(fighting)
		
		//dont want a unit being marked unmaintained while its in an active army, so only let reserve players fire techs
		if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You may not fire technicians while you are active! Withdraw from the front lines " +
					"before reducing your support levels!",Username,true);
			return;
		}//end if(active)
		
		/*
		 * Passed all the returns, so do the fire.
		 * 
		 * Might need to add some kind of severance pay for people who are wealthy and
		 * firing techs. I have visions of people using unmaintained units to get free
		 * shadowhawk scraps. *ponder* Perhaps a fine applied to people who have techs
		 * and just unmaintain units, so they have ot at least fire and rehire? (at which
		 * point scrapping may very well be cheaper than re-hiring).
		 */
		p.addTechnicians(-numtofire);
		
		if ( numtofire == 1)
			CampaignMain.cm.toUser("AM:You fire 1 technician.",Username,true);
		else
			CampaignMain.cm.toUser("AM:You fired " + numtofire + " technicians.",Username,true);
		
		if (p.getFreeBays() < 0) {
			int numUnmaintained = p.setRandomUnmaintained();
			String toSend = "There are no longer enough technicians to maintain all of your equipment. ";
			if (numUnmaintained == 1)
				toSend += " 1 unit was pulled from the active rotation (A unit is";
			else
				toSend += numUnmaintained + " units were pulled from the active rotation (" + numUnmaintained + " units are ";
			toSend += " now unmaintained! Check your status!)";
			
			CampaignMain.cm.toUser(toSend,Username,true);
		}//end if(firing creates negative support)
		
	}//end process()
    
    private void fireAdvanceTechs(StringTokenizer command, String Username){
        int numberOfTechs = 0;
        int techType = 0;
        SPlayer player = CampaignMain.cm.getPlayer(Username);
        
        if ( command.hasMoreElements()) {
        	numberOfTechs = Integer.parseInt(command.nextToken());
        } else {
        	CampaignMain.cm.toUser("AM: " + getSyntax(), Username, true);
        	return;
        }
        if (command.hasMoreElements()) {
        	techType = UnitUtils.TECH_GREEN;
        } else {
        	CampaignMain.cm.toUser("AM: " + getSyntax(),  Username, true);
        	return;
        }
        
        if ( command.hasMoreElements())
            techType = Integer.parseInt(command.nextToken());
        
        int totalTechsToFire = player.getTotalTechs().elementAt(techType);
        int availableTechsToFire = player.getAvailableTechs().elementAt(techType);
        
        if ( totalTechsToFire < numberOfTechs ){
            CampaignMain.cm.toUser("AM:You do not have enough "+UnitUtils.techDescription(techType)+" techs to fire! You only have "+totalTechsToFire+".",Username,true);
            return;
        }
        
        if ( availableTechsToFire < numberOfTechs){
            CampaignMain.cm.toUser("AM:While you do have enough techs to fire some of them are currently working and you must wait for them to finish before you can fire them.",Username,true);
            return;
        }
        
        player.addAvailableTechs(techType,-numberOfTechs);
        player.addTotalTechs(techType,-numberOfTechs);
        
        if ( numberOfTechs == 1)
            CampaignMain.cm.toUser("AM:You have fired a "+UnitUtils.techDescription(techType)+" tech.",Username,true);
        else
            CampaignMain.cm.toUser("AM:You have fired "+numberOfTechs+" "+UnitUtils.techDescription(techType)+" techs.",Username,true);
        
    }//end fireAdvanceTechs
    
}//end FireTechsCommand()