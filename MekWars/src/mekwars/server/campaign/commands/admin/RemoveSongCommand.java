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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax removesong#songname
public class RemoveSongCommand implements Command {
    
    int accessLevel = IAuthenticator.ADMIN;
    public int getExecutionLevel(){return accessLevel;}
    public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "Song Name";
	public String getSyntax() { return syntax;}

    public void process(StringTokenizer command,String Username) {
        
        //access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if(userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
            return;
        }
        
        File songList = new File("./data/songs.txt");

        //no song file no reason to use it.
        if ( !songList.exists() ){
                return;
        }
            
        String songName = command.nextToken();
        StringBuilder songBuffer = new StringBuilder();
        
        try{
            FileInputStream fis = new FileInputStream(songList);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()){
                String song = dis.readLine();
                if ( !song.toLowerCase().startsWith(songName.toLowerCase()) && song.trim().length() > 0)
                    songBuffer.append(song+"\n");
            }
            fis.close();
            dis.close();
        }
        catch(Exception ex){
            CampaignMain.cm.toUser("Unable to remove song!",Username,true);
            return;
        }
        
        try{
            FileOutputStream fos = new FileOutputStream(songList);
            PrintStream ps = new PrintStream(fos);
            ps.print(songBuffer.toString());
            
            ps.close();
            fos.close();
        }catch(Exception ex){
            CampaignMain.cm.toUser("Unable to create new songs.txt",Username,true);
            return;
        }
        
        CampaignMain.cm.doSendModMail("NOTE",Username+" has removed "+songName+" from the song list!");
    }
}
