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
package server.util.rss;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import server.campaign.CampaignMain;

/**
 * A self-contained RSS message to be added to a feed
 *
 * @author Spork
 * @version 1.0
 * @since 2018-02-18
 */
public class FeedMessage {
	private String category;
	private String pubDate;
	private String guid;
	private String description;
	private String title;
	private String link;
	
	private StringBuffer msgBody;
	
	public FeedMessage (String title, String category, String description) {
		this.title = title;
		this.category = category;
		this.msgBody = new StringBuffer(description);
		
		if(description.isEmpty()) {
			this.description = " ";
		} else {
			// Delete any html tags in the body
			while (msgBody.indexOf("<") > -1) {
				msgBody.delete(msgBody.indexOf("<"), msgBody.indexOf(">") + 1);
			}
			this.description = msgBody.toString();
			}
		guid = UUID.randomUUID().toString();
		link = CampaignMain.cm.getConfig("NewsURL");
		pubDate = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(new Date());
	}
	
	/**
	 * Outputs the string for publication
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<item>\n");
		sb.append(addAttribute("title", title));
		sb.append(addAttribute("pubDate", pubDate));
		sb.append(addAttribute("category", category));
		sb.append("<guid isPermaLink=\"false\">" + guid + "</guid>\n");
		sb.append(addAttribute("link", link));
		sb.append(addAttribute("description", description));
		sb.append("</item>");
		return sb.toString();
	}
	
	/**
	 * A helper class so I don't have to type so much
	 * @param name the tag title
	 * @param body the tag body
	 * @return a completed xml tag
	 */
	private String addAttribute(String name, String body) {
		return "<" + name + ">" + body + "</" + name + ">\n";
	}
}
