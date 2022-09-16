/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands.admin;

import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.util.MWLogger;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.DefaultServerOptions;
import server.campaign.commands.Command;


/**
 * Allows SO's to force clients to update without a major version change
 *
 * Syntax  /c forceupdate#Key#[Player/Dedicated/All]
 * <code>Player/Dedicated/All</code> are optional and will kick those entities
 * off so that they have to update right away.
 */
public class ForceUpdateCommand implements Command {
	
	int accessLevel = IAuthenticator.MODERATOR;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "Update Key#[Player/Dedicated/All]";
	public String getSyntax() { return syntax;}

	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
        
        String updateKey = "";
        String whoToKick = "";
        
        try{
            updateKey = command.nextToken();
        }catch(Exception ex){
            CampaignMain.cm.toUser("You must supply a Key<br>" +
                    "Syntax  /c forceupdate#Key[Clear]#[Player/Dedicated/All]<br>"+
                     "Player/Dedicated/All are optional and will kick those entities<br>"+
                     "off so that they have to update right away.",Username);
            return;
        }

        if ( updateKey.equalsIgnoreCase("Clear") || updateKey.equalsIgnoreCase("-1") )
        	updateKey = "";
        
        CampaignMain.cm.getConfig().setProperty("ForceUpdateKey",updateKey);
        DefaultServerOptions dso = new DefaultServerOptions();
        dso.createConfig();

        CampaignMain.cm.doSendModMail("NOTE",Username + " set the Force Update Key");
        CampaignMain.cm.toUser("Make sure to add UPDATEKEY="+updateKey+"<br>To the serverdata.dat",Username);
        if ( command.hasMoreTokens() ){
            whoToKick = command.nextToken();
            CampaignMain.cm.doSendModMail("NOTE",Username + " is kicking "+whoToKick);
            boolean players = false;
            boolean deds = false;
            
            if ( whoToKick.equalsIgnoreCase("all") ){
            	players = true;
            	deds = true;
            }
            else if ( whoToKick.toLowerCase().startsWith("player") )
        		players = true;
            else
            	deds = true;
            
            ConcurrentLinkedQueue<String> users = new ConcurrentLinkedQueue<String>(CampaignMain.cm.getServer().getUsers().keySet());
            for ( String toKick : users ){
                if ( CampaignMain.cm.getServer().isAdmin(toKick) )
                    continue;
            	if ( players && !toKick.toLowerCase().startsWith("[dedicated]") ){
	                CampaignMain.cm.toUser("You have been forced to update by " + Username+"!", toKick);
	                CampaignMain.cm.toUser("PL|FCU|Bye Bye", toKick,false);
            	}
            	else if ( deds && toKick.toLowerCase().startsWith("[dedicated]") ){
	                try{
	            		CampaignMain.cm.getServer().doStoreMail(toKick+",update", Username);
	                	Thread.sleep(120);
	                }catch (Exception ex){
	                    MWLogger.errLog(ex);
	                }
            	}
            }//end for
        }//end hasMore Commands
   	}
}