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
package mekwars.server.util.rss;

import common.util.MWLogger;

/**
 * The Feed class implements an RSS feed, getting the functionality out of CampaignMain This feed is compliant with RSS
 * 2.0, and will work with ITTT-Discord integration
 *
 * @author Spork
 * @version 1.0
 * @since 2018-02-18
 */
public class Feed {
    private java.util.ArrayList<FeedMessage> messages = new java.util.ArrayList<FeedMessage>();

    private String header;
    private String footer;

    public Feed() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
        sb.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        header = sb.toString();

        sb = new StringBuilder();
        sb.append("</channel>\n");
        sb.append("</rss>\n");
        footer = sb.toString();

    }

    /**
     * getChannel returns the channel information at the head of the RSS feed
     */
    private String getChannel() {
        StringBuilder sb = new StringBuilder();
        sb.append("<channel>\n");
        sb.append("<atom:link href=\"" +
                        server.campaign.CampaignMain.cm.getConfig("NewsURL") +
                        "\" rel=\"self\" type=\"application/rss+xml\" />\n");
        sb.append("<title>");
        sb.append(server.campaign.CampaignMain.cm.getServer().getConfigParam("SERVERNAME") + " News Feed");
        sb.append("</title>\n");
        sb.append("<link>");
        sb.append(server.campaign.CampaignMain.cm.getServer().getConfigParam("TRACKERLINK"));
        sb.append("</link>\n");
        sb.append("<description>Campaign News</description>\n");
        return sb.toString();
    }

    /**
     * Writes the feed to disk
     */
    private void write() {
        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(server.campaign.CampaignMain.cm.getConfig(
                  "NewsPath"));
            java.io.PrintStream ps = new java.io.PrintStream(out);
            ps.println(header);
            ps.println(getChannel());

            java.util.Iterator<FeedMessage> iter = messages.iterator();
            while (iter.hasNext()) {
                ps.println(iter.next().toString());
            }

            ps.println(footer);
            ps.close();
        } catch (java.io.FileNotFoundException efnf) {
            // ignore
        } catch (Exception ex) {
            MWLogger.errLog("Problems writing the news feed");
        }


    }

    /**
     * Adds a message to the feed
     *
     * @param message
     */
    public void addMessage(FeedMessage message) {
        messages.add(message);
        if (messages.size() > 200) {
            messages.remove(0);
        }
        write();
    }
}
