/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.commands;

import java.util.StringTokenizer;

import javax.swing.JPanel;

import megamek.codeUtilities.MathUtility;
import mekwars.common.House;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;
import mekwars.common.util.StringUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class PrivateMessageCommand extends Command {

    /**
     * @see Command#Command(IClient)
     */
    public PrivateMessageCommand(IClient client) {
        super(client);

    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);
        JPanel mailTab;

        if (stringTokenizer.hasMoreElements()) {
            String name = stringTokenizer.nextToken(); // Parse the name
            if (!client.isIgnored(name, IClient.IGNORE_PRIVATE) && stringTokenizer.hasMoreElements()) {

                //update the reply default, if toSender
                if (client.getConfig().isParam("REPLY_TO_SENDER")) {
                    client.setLastQuery(name);
                }

                //set up strings for colors, and the user sending the message
                CUser sender = (CUser) client.getUser(name);
                String usercolor = ((CUser) client.getUser(name)).getHtmlColor();//preferred colour
                String addon = ((CUser) client.getUser(name)).getAddon();//addon
                String message = stringTokenizer.nextToken(); // Parse the message --Torren
                String factioncolor = client.getConfig().getParam("CHAT_FONT_COLOR");
                String tabName = name;
                String fontSize = client.getConfig().getParam("CHAT_FONT_SIZE");

                if (MathUtility.parseBoolean(client.getConfigParam("INVERT_CHAT_COLOR"), false)) {
                    factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                    usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                }

                if (client.getConfig().isParam("USE_MULTIPLE_PM")) {
                    int maxTabs = client.getConfig().getIntParam("MAX_PM_TABS");
                    //Check to see if the Mail tab exists if not create a new one.
                    mailTab = client.getMainFrame().getMainPanel().getCommPanel().findMailTab(tabName);
                    if (mailTab == null) {
                        int count = client.getMainFrame().getMainPanel().getCommPanel().countMailTabs();

                        if (count >= maxTabs) {
                            client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}mail \{name}, \{client.getConfigParam(
                                  "MAX_PM_MESSAGE")}");
                            String sysColour = client.getConfigParam("SYS_MESSAGE_COLOR");
                            message = STR."<font color=\"\{sysColour}\"><b>\{name} tried to PrivateMessageCommand you while you where busy</b></font>";
                            client.addToChat(message);
                            return;
                        }
                        client.getMainFrame().getMainPanel().getCommPanel().createMailTab(tabName);
                    }
                }
                //draw a faction color from the datafeed
                if (sender.getHouse().length() > 1 && client.getData().getHouseByName(sender.getHouse()) != null) {
                    House currentHouse = client.getData().getHouseByName(sender.getHouse());

                    if (currentHouse != null) {
                        factioncolor = currentHouse.getHouseColor();
                    }
                }

                //set up the name and addon colours
                String colorSetting = client.getConfig().getParam("PLAYER_CHAT_COLOR_MODE").toLowerCase();
                if (colorSetting.equals("faction_add") || colorSetting.equals("faction_all")) {
                    addon = addon.isEmpty() ?
                                  "" :
                                  STR." <b><font color=\"\{factioncolor}\">[\{addon}]</b></font>";
                } else {
                    addon = addon.isEmpty() ? "" : STR." <b><font color=\"\{usercolor}\">[\{addon}]</b></font>";
                }

                if (colorSetting.equals("faction_name") || colorSetting.equals("faction_all")) {
                    name = name.isEmpty() ? "" : STR." <b><font color=\"\{factioncolor}\">\{name}</b></font>";
                } else {
                    name = name.isEmpty() ? "" : STR." <b><font color=\"\{usercolor}\">\{name}</b></font>";
                }
                //faction mail emote. [does this work server side? never seen it used.]
                if (message.startsWith("#me")) {
                    if (client.getConfig().isParam("COLORED_EMOTES")) {
                        message = STR."*** \{name}\{message.substring(3)}";
                    } else {
                        message = STR."*** \{tabName}\{message.substring(3)}";
                    }
                    message = STR."<font size=\"\{fontSize}\">\{message}</font>";
                } else {
                    //load and set chat font colour
                    message = STR."<font size=\"\{fontSize}\">\{message}</font>";
                    message = STR."\{name}\{addon}<b>:</b> \{message.trim()}";
                }

                //if the user wants to, remove any img tags
                if (client.getConfig().isParam("NO_IMG_IN_CHAT")) {
                    int start = message.toLowerCase().indexOf("<img");
                    int finish = -1;

                    if (start != -1) {finish = message.indexOf(">", start);}

                    if (start != -1 && finish != -1) {
                        String firstHalf = message.substring(0, start);
                        String secondHalf = message.substring(finish + 1);

                        message = STR."\{firstHalf}(img blocked)\{secondHalf}";
                    }
                }

                //add timestamp
                if (client.getConfig().isParam("TIMESTAMP")) {
                    message = client.getShortTime() + message;
                }

                //put the message in PrivateMessageCommand panel
                client.addToChat(message, CCommPanel.CHANNEL_PRIVATE_MAIL, tabName);

                //if PMs show in main, make it red and show there too
                if (client.getConfig().isParam("MAIN_CHANNEL_PM")) {
                    String sysColour = client.getConfigParam("SYS_MESSAGE_COLOR");
                    message = STR."<font color=\"\{sysColour}\"><b>Private Mail: </b></font>\{message}";
                    client.addToChat(message);
                }

                /*
                 * Sound priority -
                 * 1) PrivateMessageCommand
                 * 2) Name
                 * 3) Keyword
                 */
                if (client.getConfig().isParam("ENABLE_MESSAGE_SOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUND_ON_MESSAGE"));
                } else if (message.contains(client.getUsername()) && client.getConfig().isParam("ENABLE_CALL_SOUND")) {
                    client.doPlaySound("SOUND_ON_CALL");
                } else if (client.hasKeyWords(message) && client.getConfig().isParam("ENABLE_KEYWORD_SOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUND_ON_KEYWORD"));
                }
            }
        }
    }

    /**
     *
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}
