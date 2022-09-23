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

package server.campaign.commands;

import java.util.StringTokenizer;

import server.campaign.CampaignMain;

/**
 * Moving the Color command from MWServ into the normal command structure.
 *
 * Syntax  /c Color#Color Schema
 */
public class ColorCommand implements Command {
	
	int accessLevel = 0;
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "";
	public String getSyntax() { return syntax;}
	
	public void process(StringTokenizer command,String Username) {
		
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}

        String text = "";
        
        try{
            text = command.nextToken();
        }catch (Exception ex){}
        
        if (text.trim().length() < 1)
            CampaignMain.cm.toUser("AM:You need to choose a color. /color blue", Username,true);
        else {
            //Hex numbers need a # in from of them. This is parsed out by the tokenizer
            try{
                Integer.parseInt(text);
                text = "#"+text;
            }catch (Exception ex){}
            CampaignMain.cm.getServer().getUser(Username).setColor(text);
            CampaignMain.cm.getServer().broadcastRaw("UC|" + Username + "|" + CampaignMain.cm.getServer().getUser(Username).getColor());
        }
	}
}