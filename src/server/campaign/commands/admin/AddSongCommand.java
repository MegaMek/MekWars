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
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.commands.Command;

//Syntax addsong#songname#lyric1#lyric2....
public class AddSongCommand implements Command {
    
    int accessLevel = IAuthenticator.ADMIN;
    public int getExecutionLevel(){return accessLevel;}
    public void setExecutionLevel(int i) {accessLevel = i;}
	String syntax = "addsong#songname#lyric1#lyric2....";
	public String getSyntax() { return syntax;}

    public void process(StringTokenizer command,String Username) {
        
        //access level check
        int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
        if(userLevel < getExecutionLevel()) {
            CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
            return;
        }
        
        File songList = new File("./data/songs.txt");

        
        if ( !songList.exists() ){
            try{
                songList.createNewFile();
            }catch(Exception ex){
                return;
            }
        }
            
        String songName = command.nextToken();
        
        StringBuilder lyrics = new StringBuilder();
        
        while ( command.hasMoreTokens() ){
            lyrics.append(command.nextToken());
            lyrics.append("#");
        }
         
        //get rid of that last #
        lyrics.deleteCharAt(lyrics.length()-1);
        try{
            FileOutputStream fos = new FileOutputStream(songList,true);
            PrintStream ps = new PrintStream(fos);
            ps.println(songName+"|"+lyrics.toString().trim());
            
            ps.close();
            fos.close();
        }catch(Exception ex){
            CampaignMain.cm.toUser("Unable to append to songs.txt",Username,true);
            return;
        }
        
        CampaignMain.cm.doSendModMail("NOTE",Username+" has added "+songName+" to the song list!");
    }
}
