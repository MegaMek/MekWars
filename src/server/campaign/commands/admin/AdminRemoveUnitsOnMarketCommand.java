/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Helge Richter (McWizard)
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
 * Created on 17.04.2004
 *
 */
package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

/**
 * @author Helge Richter
 */
public class AdminRemoveUnitsOnMarketCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "[player][all][number]";
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
		
		//vars
		String toRemove = "";
		
		try {
			toRemove = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminremoveunitsonmarker#[player][all][number]", Username, true);
			return;
		}
		
        if (toRemove.equalsIgnoreCase("all")) {
            CampaignMain.cm.getMarket().removeAllListings();
            CampaignMain.cm.toUser("You removed all units from the market.", Username, true);
            CampaignMain.cm.doSendModMail("NOTE",Username + " removed all units from the BM.");
            CampaignMain.cm.getServer().sendChat(Username + " removed all units from the BM.");
            return;
        }
        
        
        //check to see if a specific auction is given.
        int auctionNumber = -1;
        try{
            auctionNumber = Integer.parseInt(toRemove);
        } catch(Exception ex){
        	//do nothing
        }
        
        if (auctionNumber > -1) {
            CampaignMain.cm.getMarket().removeListing(auctionNumber);
            CampaignMain.cm.toUser("You removed auction #" + auctionNumber +" from the market.", Username, true);
            CampaignMain.cm.doSendModMail("NOTE",Username + " removed auction #" + auctionNumber + " from the market.");
        } 
        
        else {
        	
    		SPlayer p = CampaignMain.cm.getPlayer(toRemove);
    		if (p == null) {
    			CampaignMain.cm.toUser("Couldn't find a player named " + toRemove + ".", Username, true);
    			return;
    		}
    		
    		if (!CampaignMain.cm.getMarket().hasActiveListings(p)) {
    			CampaignMain.cm.toUser(p.getName() + " doesn't have any running auctions.", Username, true);
    			return;
    		}
    		
		    CampaignMain.cm.getMarket().removePlayerListings(p);
        
    		CampaignMain.cm.toUser("You cancelled all of " + p.getName() + "'s auctions.", Username, true);
    		CampaignMain.cm.toUser(Username + " cancelled all of your running auctions.", p.getName(), true);
    		CampaignMain.cm.doSendModMail("NOTE",Username + " cancelled all of " + toRemove + "'s auctions.");
        }
	}
}
