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

package mekwars.client.commands;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.StringUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PrivateMessageCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public PrivateMessageCommand(IClient mwclient) {
        super(mwclient);

    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);
        javax.swing.JPanel mailTab = null;

        if (st.hasMoreElements()) {

            String name = st.nextToken(); // Parse the name
            if (!client.isIgnored(name, IClient.IGNORE_PRIVATE) && st.hasMoreElements()) {

                //update the reply default, if toSender
                if (client.getConfig().isParam("REPLYTOSENDER")) {
                    client.setLastQuery(name);
                }

                //set up strings for colours, and the user sending the message
                CUser sender = client.getUser(name);
                String usercolor = client.getUser(name).getColor();//preferred colour
                String addon = client.getUser(name).getAddon();//addon
                String message = st.nextToken(); // Parse the message --Torren
                String factioncolor = client.getConfig().getParam("CHATFONTCOLOR");
                String tabName = name;
                String fontSize = client.getConfig().getParam("CHATFONTSIZE");

                if (Boolean.parseBoolean(client.getConfigParam("INVERTCHATCOLOR"))) {
                    factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                    usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                }

                if (client.getConfig().isParam("USEMULTIPLEPM")) {
                    int maxTabs = client.getConfig().getIntParam("MAXPMTABS");
                    //Check to see if the Mail tab exists if not create a new one.
                    mailTab = client.getMainFrame().getMainPanel().getCommPanel().findMailTab(tabName);
                    if (mailTab == null) {
                        int count = client.getMainFrame().getMainPanel().getCommPanel().countMailTabs();

                        if (count >= maxTabs) {
                            client.sendChat(IClient.CAMPAIGN_PREFIX +
                                                  "mail " +
                                                  name +
                                                  ", " +
                                                  client.getConfigParam("MAXPMMESSAGE"));
                            String sysColour = client.getConfigParam("SYSMESSAGECOLOR");
                            message = "<font color=\"" +
                                            sysColour +
                                            "\"><b>" +
                                            name +
                                            " tried to PrivateMessageCommand you while you where busy</b></font>";
                            client.addToChat(message);
                            return;
                        }
                        client.getMainFrame().getMainPanel().getCommPanel().createMailTab(tabName);
                    }
                }
                //draw a factioncolour from the datafeed
                if (sender.getHouse().length() > 1 && client.getData().getHouseByName(sender.getHouse()) != null) {
                    factioncolor = client.getData().getHouseByName(sender.getHouse()).getHouseColor();
                }

                //set up the name and addon colours
                String colorSetting = client.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
                if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
                    addon = addon.isEmpty() ?
                                  "" :
                                  " <b><font color=\"" + factioncolor + "\">[" + addon + "]</b></font>";
                } else {
                    addon = addon.isEmpty() ? "" : " <b><font color=\"" + usercolor + "\">[" + addon + "]</b></font>";
                }

                if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
                    name = name.isEmpty() ? "" : " <b><font color=\"" + factioncolor + "\">" + name + "</b></font>";
                } else {
                    name = name.isEmpty() ? "" : " <b><font color=\"" + usercolor + "\">" + name + "</b></font>";
                }
                //faction mail emote. [does this work server side? never seen it used.]
                if (message.startsWith("#me")) {
                    if (client.getConfig().isParam("COLOREDEMOTES")) {
                        message = "*** " + name + message.substring(3);
                    } else {message = "*** " + tabName + message.substring(3);}
                    message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                } else {
                    //load and set chat font colour
                    message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                    message = name + addon + "<b>:</b> " + message.trim();
                }

                //if the user wants to, remove any img tags
                if (client.getConfig().isParam("NOIMGINCHAT")) {
                    int start = message.toLowerCase().indexOf("<img");
                    int finish = -1;

                    if (start != -1) {finish = message.indexOf(">", start);}

                    if (start != -1 && finish != -1) {
                        String firstHalf = message.substring(0, start);
                        String secondHalf = message.substring(finish + 1);

                        message = firstHalf + "(img blocked)" + secondHalf;
                    }
                }

                //add timestamp
                if (client.getConfig().isParam("TIMESTAMP")) {message = client.getShortTime() + message;}

                //put the message in PrivateMessageCommand panel
                client.addToChat(message, client.gui.CCommPanel.CHANNEL_PMAIL, tabName);

                //if PMs show in main, make it red and show there too
                if (client.getConfig().isParam("MAINCHANNELPM")) {
                    String sysColour = client.getConfigParam("SYSMESSAGECOLOR");
                    message = "<font color=\"" + sysColour + "\"><b>Private Mail: </b></font>" + message;
                    client.addToChat(message);
                }

                /*
                 * Sound priority -
                 * 1) PrivateMessageCommand
                 * 2) Name
                 * 3) Keyword
                 */
                if (client.getConfig().isParam("ENABLEMESSAGESOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUNDONMESSAGE"));
                } else if (message.contains(client.getUsername()) && client.getConfig().isParam("ENABLECALLSOUND")) {
                    client.doPlaySound("SOUNDONCALL");
                } else if (client.hasKeyWords(message) && client.getConfig().isParam("ENABLEKEYWORDSOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUNDONKEYWORD"));
                }
            }
        }
    }
}
