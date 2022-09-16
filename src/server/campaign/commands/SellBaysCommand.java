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

import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;

public class SellBaysCommand implements Command {
	
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
		
        if ( !CampaignMain.cm.isUsingAdvanceRepair() ){
            return;
        }

		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SHouse house = p.getMyHouse();
		
		//use /c firetech#numbertofire
		int numtosell = 1;//default to 1
		
		try {
			numtosell = Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:Couldn't tell how many bays to sell. Check your input. It should be something like this: /c sellbays#3",Username,true);
			return;
		}//end catch
		
		//Check to see if the player is selling too many bays
		if (p.getBaysOwned() < numtosell) {
			CampaignMain.cm.toUser("AM:You tried to return " + numtosell + " bays, but you only have " + p.getBaysOwned() + " bays " +
					". The rest were assigned to your force by your faction and can't be returned.",Username,true);
			return;
		}
		
		//Check to see if the player is fighting. Engaged players can't fire techs.
		if (p.getDutyStatus() == SPlayer.STATUS_FIGHTING) {
			CampaignMain.cm.toUser("AM:You may not return bays while you are engaged! Wait until your units are out of battle and fully repaired.",Username,true);
			return;
		}//end if(fighting)
		
		//dont want a unit being marked unmaintained while its in an active army, so only let reserve players fire techs
		if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
			CampaignMain.cm.toUser("AM:You may not return bays while you are active. Withdraw from the front lines " +
					"before reducing your support levels.",Username,true);
			return;
		}//end if(active)
		
        if (p.getFreeBays() < numtosell){
            CampaignMain.cm.toUser("AM:You need to free up some bay space before you can return anymore!",Username,true);
            return;
        }
        
		p.addBays(-numtosell);
		
        int sellbackprice = Integer.parseInt(house.getConfig("BaySellBackPrice"))*numtosell;
        p.addMoney(sellbackprice);
        
		if ( numtosell == 1)
			CampaignMain.cm.toUser("AM:You return a bay.  Your faction returns "+CampaignMain.cm.moneyOrFluMessage(true,true,sellbackprice)+" of your security deposit.",Username,true);
		else
			CampaignMain.cm.toUser("AM:You return " + numtosell + " bays.  Your faction returns "+CampaignMain.cm.moneyOrFluMessage(true,true,sellbackprice)+" of your security deposit.",Username,true);
        CampaignMain.cm.toUser("PL|SF|"+p.getFreeBays(),Username,false);
        CampaignMain.cm.toUser("PL|SB|"+p.getTotalMekBays(),Username,false);
        CampaignMain.cm.toUser("PL|ST|"+p.getBaysOwned(),Username,false);

	}//end process()
}//end SellBaysCommand()