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

package client.gui.commands;

import java.awt.event.ActionEvent;
import java.util.StringTokenizer;

import javax.swing.JPanel;

import client.CUser;
import client.MWClient;
import client.gui.CCommPanel;
import common.campaign.clientutils.IClientConfig;
import common.campaign.clientutils.protocol.TransportCodec;
import common.util.StringUtils;

/**
 * Mail command
 */
public class MailGCmd extends CGUICommand {
	
	private static final long serialVersionUID = 4728122923982632944L;
	IClientConfig Config = null;
	String fontcolor = "black";
	
	public MailGCmd(MWClient mwclient) {
		super(mwclient);
		Config = mwclient.getConfig();
		name = "mail";
		alias = "m";
		command = "comm\t";
		fontcolor = Config.getParam("CHATFONTCOLOR");
	}
	
	@Override
	public boolean execute(String input) {
		StringTokenizer ST = new StringTokenizer(input, " ");
		
		if (check(ST.nextToken())) {
			input = decompose(input);
			String receiver = input.substring(0, input.indexOf(","));
			
			if (mwclient.getConfig().isParam("USEMULTIPLEPM")){
				int maxTabs =  mwclient.getConfig().getIntParam("MAXPMTABS");
				if ( mwclient.getMainFrame().getMainPanel().getCommPanel().findMailTab(receiver) == null ){
					int count = mwclient.getMainFrame().getMainPanel().getCommPanel().countMailTabs();
					
					if ( count >= maxTabs ){
						String sysColour = mwclient.getConfigParam("SYSMESSAGECOLOR");
						mwclient.addToChat("<font color=\""+sysColour+"\"><b>You already have the max number of PM tabs open! Close one before starting a new session.</b></font>");
						return false;
					}
				}
				
			}
			if (Config.isParam("REPLYTORECEIVER") && receiver != null && !receiver.equals("")) {mwclient.setLastQuery(receiver);}
			send(TransportCodec.encode("CH|" + MWClient.CAMPAIGN_PREFIX + "mail " + input));
			echo(input);
			return true;
		}
		//else
		return false;
	}
	
	@Override
	protected void echo(String input) {
		String name, message,  tabName;
		JPanel mailTab;
		
		tabName = name = input.substring(0, input.indexOf(","));
		
		message = input.substring(input.indexOf(",") + 1);
		
		/*if (mwclient.getConfig().isParam("USEMULTIPLEPM")) 
		 name = mwclient.getPlayer().getName();
		 */
		CUser user = mwclient.getUser(name);
		
		//draw a factioncolour from the datafeed
		String factioncolor = "";
		String usercolor = mwclient.getUser(name).getColor();//preferred colour
		String addon = mwclient.getUser(name).getAddon();//addon
		
		if (user.getHouse().length() > 1 && mwclient.getData().getHouseByName(user.getHouse()) != null )
			factioncolor = mwclient.getData().getHouseByName(user.getHouse()).getHouseColor();
		else
			factioncolor = "black";
		
		//set up the name and addon colours
		String colorSetting = mwclient.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
		if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
			addon = addon.equals("") ? "" : " <b><font color=\"" + factioncolor + "\">[" + addon + "]</b></font>";
		} else {
			addon = addon.equals("") ? "" : " <b><font color=\"" + usercolor + "\">[" + addon + "]</b></font>";
		}
		
		if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
			name = name.equals("") ? "" : " <b><font color=\"" + factioncolor + "\">" + name + "</b></font>";
		} else {
			name = name.equals("") ? "" : " <b><font color=\"" + usercolor + "\">" + name + "</b></font>";
		}
		
		//load and set chat font colour
		String fontcolor = mwclient.getConfig().getParam("CHATFONTCOLOR");
        
		if (Boolean.parseBoolean(mwclient.getConfigParam("INVERTCHATCOLOR"))) {
            factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
            usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
        }

		message = "=> " + name + addon + "<b>:</b> " + message.trim();
		
		if (Config.isParam("TIMESTAMP"))
			message = mwclient.getShortTime() + message;
		
		message = "<font color=\""+fontcolor+"\">" + message + "</font>";
		
		if (mwclient.getConfig().isParam("USEMULTIPLEPM")) {
			mailTab = mwclient.getMainFrame().getMainPanel().getCommPanel().findMailTab(tabName); 
			if (mailTab == null)
				mwclient.getMainFrame().getMainPanel().getCommPanel().createMailTab(tabName);
		}
		
		//add to PM Channel
		mwclient.addToChat(message, CCommPanel.CHANNEL_PMAIL, tabName);
		
		//if should be shown in main, add there as well
		if (Config.isParam("MAINCHANNELPM")) {
			String sysColour = mwclient.getConfigParam("SYSMESSAGECOLOR");
			mwclient.addToChat("<font color=\""+sysColour+"\"><b>Private Mail: </b></font>" + message);
		}
		
	}
	
	public void actionPerformed(ActionEvent e) {}
}