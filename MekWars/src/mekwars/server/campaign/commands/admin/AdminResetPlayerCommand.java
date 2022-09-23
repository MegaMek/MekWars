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

import java.io.File;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.commands.Command;

public class AdminResetPlayerCommand implements Command {
	
	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "Player Name/Faction Name/All#CONFIRM";
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
		
		//variables
		String resetType = "";
        String commandConfirmed = "";
		try {
            resetType = command.nextToken();
            commandConfirmed = command.nextToken();
		} catch (Exception e) {
			CampaignMain.cm.toUser("Improper command. Try: /c adminresetplayer#Player Name/Faction Name/All#CONFIRM", Username, true);
			return;
		}
		
        if ( !commandConfirmed.equals("CONFIRM") ){
            CampaignMain.cm.toUser("Improper command. Try: /c adminresetplayer#Player Name/Faction Name/All#CONFIRM", Username, true);
            return;
        }
        
        if ( resetType.equalsIgnoreCase("all") ){
            File[] playerList = new File("./campaign/players").listFiles();

            for ( int i = 0; i < playerList.length; i++){
                File playerFile = playerList[i];
                if ( playerFile.isDirectory() )
                    continue;
                
                SPlayer player = CampaignMain.cm.getPlayer(playerFile.getName().substring(0,playerFile.getName().indexOf(".dat")));
                
                if ( player == null )
                    continue;
                
                player.reset("CONFIRM");
                CampaignMain.cm.doLogoutPlayer(player.getName());
                
            }
            //server.MWLogger.modLog(Username + " has reset all player accounts.");
            CampaignMain.cm.doSendModMail("NOTE",Username + " has reset all player accounts.");
        }else if ( CampaignMain.cm.getHouseFromPartialString(resetType,null) != null ){
            File[] playerList = new File("./campaign/players").listFiles();

            for ( int i = 0; i < playerList.length; i++){
                File playerFile = playerList[i];
                if ( playerFile.isDirectory() )
                    continue;
                
                SPlayer player = CampaignMain.cm.getPlayer(playerFile.getName().substring(0,playerFile.getName().indexOf(".dat")));
                
                if ( player == null )
                    continue;
                
                if ( player.getMyHouse().getName().equalsIgnoreCase(resetType) ){
                    player.reset("CONFIRM");
                    CampaignMain.cm.doLogoutPlayer(player.getName());
                }
                
            }
            //server.MWLogger.modLog(Username + " has reset all player accounts for faction "+resetType);
            CampaignMain.cm.doSendModMail("NOTE",Username + " has reset all player accounts for faction "+resetType);
        }else if (CampaignMain.cm.getPlayer(resetType) != null ) {
            SPlayer player = CampaignMain.cm.getPlayer(resetType);
            player.reset("CONFIRM");
            CampaignMain.cm.doLogoutPlayer(player.getName());
            //server.MWLogger.modLog(Username + " has reset "+player.getName()+"'s account.");
            CampaignMain.cm.doSendModMail("NOTE",Username + " has reset "+player.getName()+"'s account.");
		}
		
        CampaignMain.cm.forceSavePlayers(Username);
	}
}
