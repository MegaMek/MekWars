/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Nathan Morris (urgru)
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

public class BuyBaysCommand implements Command {
	
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
        
		//use /c buybays#numbertobuy
		int numtobuy = 1;//default to 1 if no number present
		SPlayer p = CampaignMain.cm.getPlayer(Username);
		SHouse house = p.getMyHouse();
		
		int bayCost = 0;//default cost
		int maxBays = Integer.parseInt(house.getConfig("MaxBaysToBuy"));//Max number of bays that can be bought
		try {
			numtobuy = Integer.parseInt(command.nextToken());
		}//end try
		catch (NumberFormatException ex) {
			CampaignMain.cm.toUser("AM:Lease Bays command failed. Check your input. It should be something like this: /c buybays#3",Username,true);
			return;
		}//end catch
		
		//get cost per bay, after XP adjustment
		bayCost = Integer.parseInt(house.getConfig("CostToBuyNewBay"));
		
        if ( bayCost == -1){
            CampaignMain.cm.toUser("AM:Sorry but their are no bays available for leasing at this moment in time.",Username,true);
            return;
        }
        
        //-1 maxbays allows for unlimited bays
        if ( maxBays != -1 && p.getBaysOwned()+numtobuy > maxBays ){
            CampaignMain.cm.toUser("AM:Sorry but the max number of bays you can lease is "+maxBays+".",Username,true);
            return;
        }

        //get the total cost to hire these bays (cost for each * number to hire)
		bayCost = bayCost * numtobuy;
		
		//send a message is the bays 
		if (bayCost > p.getMoney()) {
			CampaignMain.cm.toUser("AM:Leasing " + numtobuy + " bays will cost you "+CampaignMain.cm.moneyOrFluMessage(true,false,bayCost)+" for a security deposit. You only have "+CampaignMain.cm.moneyOrFluMessage(true,false,p.getMoney())+".",Username,true);
			return;
		}//end if(player doenst have enough money) 	
		
		
		//passed all of the return scenarios, so add the bay
		p.addBays(numtobuy);
        p.addMoney(-bayCost); 

        if ( numtobuy == 1 )
			CampaignMain.cm.toUser("AM:You've leased a bay! After paying the security deposit of " +CampaignMain.cm.moneyOrFluMessage(true,false,bayCost),Username,true);
		else
			CampaignMain.cm.toUser("AM:You've leased " + numtobuy + " bays! After paying the security deposit of " +CampaignMain.cm.moneyOrFluMessage(true,false,bayCost),Username,true);
        
        CampaignMain.cm.toUser("PL|SF|"+p.getFreeBays(),Username,false);
        CampaignMain.cm.toUser("PL|SB|"+p.getTotalMekBays(),Username,false);
        CampaignMain.cm.toUser("PL|ST|"+p.getBaysOwned(),Username,false);
	}//end process()

}//end BuyBaysCommand()