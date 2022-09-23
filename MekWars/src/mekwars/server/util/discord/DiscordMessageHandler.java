/*
 * MekWars - Copyright (C) 2018 
 * 
 * Original author - Bob Eldred (spork@mekwars.org)
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
package server.util.discord;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;

import common.util.MWLogger;
import server.campaign.CampaignMain;

/**
 * Provides integration with a Discord webhook.  Status messages and
 * Operation outcome can be sent to the webhook.  
 * 
 * @author Spork
 *
 */
public class DiscordMessageHandler {
	private String webhookAddress = "";
	
	public DiscordMessageHandler() {
		if(!CampaignMain.cm.getBooleanConfig("DiscordEnable")) {
			return;
		}
		webhookAddress = CampaignMain.cm.getConfig("DiscordWebHookAddress");
	}
	
	/**
	 * Post a message to the webhook
	 * @param message the message to send
	 */
	public void post(String message) {		
		if(webhookAddress.equalsIgnoreCase("") || webhookAddress.length() < 1 || webhookAddress == null) {
			return;
		}
		
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(webhookAddress);

		// Request parameters and other properties.
		List<NameValuePair> params = new ArrayList<NameValuePair>(2);
		params.add(new BasicNameValuePair("content", message));
		try {
			httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			MWLogger.errLog(e);
		}

		try {
			httpclient.execute(httppost);
		} catch (IOException e) {
			MWLogger.errLog(e);
		}
	}
}
