/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package client.cmd;

import java.util.StringTokenizer;

import javax.swing.JPanel;

import client.CUser;
import client.MWClient;
import client.gui.CCommPanel;
import common.util.StringUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PM extends Command {

	/**
	 * @see Command#Command(MMClient)
	 */
	public PM(MWClient mwclient) {
		super(mwclient);
		
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
        JPanel mailTab = null;

        if (st.hasMoreElements()) {
        	
            String name = st.nextToken(); // Parse the name
            if (!mwclient.isIgnored(name, MWClient.IGNORE_PRIVATE) && st.hasMoreElements()) {
            	
            	//update the reply default, if toSender
                if (mwclient.getConfig().isParam("REPLYTOSENDER")) 
                	mwclient.setLastQuery(name);
                
                //set up strings for colours, and the user sending the message
                CUser sender = mwclient.getUser(name);
                String usercolor = mwclient.getUser(name).getColor();//preferred colour
                String addon = mwclient.getUser(name).getAddon();//addon
                String message = st.nextToken(); // Parse the message --Torren
    			String factioncolor = mwclient.getConfig().getParam("CHATFONTCOLOR");
                String tabName = name;
        		String fontSize = mwclient.getConfig().getParam("CHATFONTSIZE");

                if (Boolean.parseBoolean(mwclient.getConfigParam("INVERTCHATCOLOR"))) {
                    factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                    usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                }

                if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {
                    int maxTabs =  mwclient.getConfig().getIntParam("MAXPMTABS");
                    //Check to see if the Mail tab exists if not create a new one.
                    mailTab = mwclient.getMainFrame().getMainPanel().getCommPanel().findMailTab(tabName); 
                    if (mailTab == null){
                        int count = mwclient.getMainFrame().getMainPanel().getCommPanel().countMailTabs();
                        
                        if ( count >= maxTabs ){
                            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "mail "+name+", "+mwclient.getConfigParam("MAXPMMESSAGE"));
                            String sysColour = mwclient.getConfigParam("SYSMESSAGECOLOR");
                            message = "<font color=\""+sysColour+"\"><b>"+name+" tried to PM you while you where busy</b></font>";
                            mwclient.addToChat(message);
                            return;
                        }
                        mwclient.getMainFrame().getMainPanel().getCommPanel().createMailTab(tabName);
                    }
                }                
                //draw a factioncolour from the datafeed
				if (sender.getHouse().length() > 1 && mwclient.getData().getHouseByName(sender.getHouse()) != null )
					factioncolor = mwclient.getData().getHouseByName(sender.getHouse()).getHouseColor();
				
					//set up the name and addon colours
                String colorSetting = mwclient.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
				if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
					addon = addon.equals("") ? "" : " <b><font color=\""+factioncolor+"\">[" + addon + "]</b></font>";
				} else {
					addon = addon.equals("") ? "" : " <b><font color=\""+usercolor+"\">[" + addon + "]</b></font>";
				}
				
				if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
					name = name.equals("") ? "" : " <b><font color=\"" + factioncolor + "\">" + name + "</b></font>";
				} else {
					name = name.equals("") ? "" : " <b><font color=\"" + usercolor + "\">" + name + "</b></font>";
				}
				//faction mail emote. [does this work server side? never seen it used.]
				if (message.startsWith("#me")) {
					if (mwclient.getConfig().isParam("COLOREDEMOTES"))
						message = "*** " + name + message.substring(3);
					else
						message = "*** " + tabName + message.substring(3);
					message = "<font size=\""+fontSize+"\">" + message +"</font>";
				}	
				else {
					//load and set chat font colour
					message = "<font size=\""+fontSize+"\">" + message +"</font>";
					message = name + addon + "<b>:</b> " + message.trim();
				}
				
				//if the user wants to, remove any img tags
				if (mwclient.getConfig().isParam("NOIMGINCHAT")) {
					
					int start = message.toLowerCase().indexOf("<img");
					int finish = -1;
				
					if (start != -1)
						finish = message.indexOf(">",start);
					
					if (start != -1 && finish != -1) {
						String firstHalf = message.substring(0,start);
						String secondHalf = message.substring(finish + 1, message.length());
						
						message = firstHalf + "(img blocked)" + secondHalf;
					}
				}
				
				//add timestamp
				if (mwclient.getConfig().isParam("TIMESTAMP")) 
					message = mwclient.getShortTime() + message;
				
				//put the message in PM panel
				mwclient.addToChat(message, CCommPanel.CHANNEL_PMAIL,tabName);
                
                //if PMs show in main, make it red and show there too
                if (mwclient.getConfig().isParam("MAINCHANNELPM")) {
                	String sysColour = mwclient.getConfigParam("SYSMESSAGECOLOR");
                    message = "<font color=\""+sysColour+"\"><b>Private Mail: </b></font>" + message;
                    mwclient.addToChat(message);
                }
                
                /*
                 * Sound priority -
                 * 1) PM
                 * 2) Name
                 * 3) Keyword
                 */
                if(mwclient.getConfig().isParam("ENABLEMESSAGESOUND"))
                	mwclient.doPlaySound(mwclient.getConfig().getParam("SOUNDONMESSAGE"));
                else if (message.indexOf(mwclient.getUsername()) > -1 && mwclient.getConfig().isParam("ENABLECALLSOUND"))
                	mwclient.doPlaySound("SOUNDONCALL");
                else if (mwclient.hasKeyWords(message) && mwclient.getConfig().isParam("ENABLEKEYWORDSOUND")) 
                	mwclient.doPlaySound(mwclient.getConfig().getParam("SOUNDONKEYWORD"));
            }
        }
	}
}
