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

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;
import mekwars.common.util.StringUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CH extends Command {
    private static final MMLogger LOGGER = MMLogger.create(CH.class);

    public CH(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        boolean isInvisible = false;

        StringTokenizer stringTokenizer = decode(input);
        if (stringTokenizer.hasMoreTokens()) {

            // reserve variables
            String name;
            String uncoloredName = "";
            StringBuilder message = new StringBuilder();
            String usercolor;
            String factioncolor;
            String addon;

            String nextString = stringTokenizer.nextToken();
            String defaultColor = client.getConfig().getParam("CHAT_FONT_COLOR");
            String fontSize = client.getConfig().getParam("CHAT_FONT_SIZE");

            boolean wasSystemMessage = false;
            boolean wasICMessage = false;

            if (nextString.startsWith("(House Mail)") || nextString.startsWith("(Moderator Mail)")) {
                boolean isModMail = nextString.startsWith("(Moderator Mail)");

                // get the name
                if (isModMail) {
                    name = nextString.substring(16, nextString.indexOf(":")).trim();
                } else {
                    name = nextString.substring(11, nextString.indexOf(":"));
                }

                uncoloredName = name;

                // only proceed if player isn't muted
                CUser user = (CUser) client.getUser(name);
                if (user.isInvisible() && (user.getUserLevel() > client.getUser(client.getUsername()).getUserLevel())) {
                    isInvisible = true;
                }

                if (!client.isIgnored(name, IClient.IGNORE_HOUSE) || isModMail) {
                    if (!isInvisible) {
                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getHtmlColor(); // Get the Color

                        // draw a faction color from the datafeed
                        if ((user.getHouse().length() > 1) &&
                                  (client.getData().getHouseByName(user.getHouse()) != null)) {
                            House currentHouse = client.getData().getHouseByName(user.getHouse());

                            if (currentHouse != null) {
                                factioncolor = currentHouse.getHouseColor();
                            } else {
                                LOGGER.info("House could not be found, defaulting the color");
                                factioncolor = defaultColor;
                            }
                        } else {
                            factioncolor = defaultColor;
                        }

                    } else {
                        usercolor = defaultColor;
                        factioncolor = defaultColor;
                        addon = "";
                        name = "Someone";
                    }

                    /*
                     * Set up colors. Sadly, all the color code is extremely duplicative, but a necessary evil ATM
                     * since the messages come in with wildly different formatting. Should consider rewriting the
                     * messages to standardize things like name placement and headers.
                     */

                    if (MathUtility.parseBoolean(client.getConfigParam("INVERT_CHAT_COLOR"), false)) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(
                              factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = client.getConfig().getParam("PLAYER_CHAT_COLOR_MODE").toLowerCase();
                    if (colorSetting.equals("faction_add") || colorSetting.equals("faction_all")) {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", factioncolor, fontSize, addon);
                    } else {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", usercolor, fontSize, addon);
                    }

                    if (colorSetting.equals("faction_name") || colorSetting.equals("faction_all")) {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", factioncolor, fontSize, name);
                    } else {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", usercolor, fontSize, name);
                    }

                    // load the message
                    message = new StringBuilder(nextString.substring(nextString.indexOf(":") + 1).trim());
                    while (stringTokenizer.hasMoreTokens()) {
                        message.append("|").append(stringTokenizer.nextToken());
                    }

                    // faction mail emote. [does this work server side? never seen it used.]
                    if (message.toString().startsWith("#me")) {
                        if (client.getConfig().isParam("COLORED_EMOTES")) {
                            message = new StringBuilder(String.format("*** %s%s", name, message.substring(3)));
                        } else {
                            message = new StringBuilder(String.format("*** %s%s", uncoloredName, message.substring(3)));
                        }
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));
                    }

                    // normal faction mail message
                    else {
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));
                        message = new StringBuilder(String.format("%s%s<b>:</b> %s", name, addon, message.toString().trim()));
                    }

                    // if the user wants to, remove any img tags
                    if (client.getConfig().isParam("NO_IMG_IN_CHAT")) {

                        int start = message.toString().toLowerCase().indexOf("<img");
                        int finish = -1;

                        if (start != -1) {
                            finish = message.indexOf(">", start);
                        }

                        if ((start != -1) && (finish != -1)) {
                            String firstHalf = message.substring(0, start);
                            String secondHalf = message.substring(finish + 1, message.length());

                            message = new StringBuilder(String.format("%s(img blocked)%s", firstHalf, secondHalf));
                        }
                    }

                    // add timestamp
                    if (client.getConfig().isParam("TIMESTAMP")) {
                        message.insert(0, client.getShortTime());
                    }

                    if (isModMail) {
                        client.addToChat(message.toString(), CCommPanel.CHANNEL_MOD);
                        // also add to main, if configured to do so
                        if (client.getConfig().isParam("MAIN_CHANNEL_MM")) {
                            client.addToChat(String.format("<font color=\"red\" size=\"%s\"><b>Mod Mail: </b></font>%s", fontSize, message));
                        }
                    } else {
                        // add message to faction panel
                        client.addToChat(message.toString(), CCommPanel.CHANNEL_HOUSE_MAIL);

                        // also add to main, if configured to do so
                        if (client.getConfig().isParam("MAIN_CHANNEL_HM")) {
                            client.addToChat(String.format("<font color=\"red\" size=\"%s\"><b>House Mail: </b></font>%s", fontSize, message));
                        }
                    }
                }
            } else if (nextString.startsWith("(Error Log):")) {
                message = new StringBuilder(nextString.substring(nextString.indexOf(":") + 1));
                message.insert(0, client.getShortTime());
                client.addToChat(message.toString(), CCommPanel.CHANNEL_ERROR);
            } else if (nextString.startsWith("(In Character)")) {
                wasICMessage = true;

                name = nextString.substring(14, nextString.indexOf(":"));
                uncoloredName = name;

                // only proceed if player isnt muted
                CUser user = (CUser) client.getUser(name);
                if (user.isInvisible() && (user.getUserLevel() > client.getUser(client.getUsername()).getUserLevel())) {
                    isInvisible = true;
                }

                if (!client.isIgnored(name, IClient.IGNORE_PUBLIC)) {
                    if (!isInvisible) {

                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getHtmlColor(); // Get the Color

                        // draw a faction colour from the datafeed
                        if ((user.getHouse().length() > 1) &&
                                  (client.getData().getHouseByName(user.getHouse()) != null)) {
                            House currentHouse = client.getData().getHouseByName(user.getHouse());

                            if (currentHouse != null) {
                                factioncolor = currentHouse.getHouseColor();
                            } else {
                                LOGGER.info("House could not be found, defaulting the color");
                                factioncolor = defaultColor;
                            }
                        } else {
                            factioncolor = defaultColor;
                        }

                    } else {
                        usercolor = defaultColor;
                        factioncolor = defaultColor;
                        addon = "";
                        name = "Someone";
                    }

                    /*
                     * Set up colors. Sadly, all the color code is extremely duplicative, but a necessary evil ATM
                     * since the messages come in with wildly different formatting. Should consider rewriting the
                     * messages to standardize things like name placement and headers.
                     */

                    if (MathUtility.parseBoolean(client.getConfigParam("INVERT_CHAT_COLOR"), false)) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(
                              factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = client.getConfig().getParam("PLAYER_CHAT_COLOR_MODE").toLowerCase();
                    if (colorSetting.equals("faction_add") || colorSetting.equals("faction_all")) {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", factioncolor, fontSize, addon);
                    } else {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", usercolor, fontSize, addon);
                    }

                    if (colorSetting.equals("faction_name") || colorSetting.equals("faction_all")) {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", factioncolor, fontSize, name);
                    } else {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", usercolor, fontSize, name);
                    }

                    // load the message
                    message = new StringBuilder(nextString.substring(nextString.indexOf(":") + 1));

                    while (stringTokenizer.hasMoreTokens()) {
                        message.append("|").append(stringTokenizer.nextToken());
                    }

                    // strip HTML from the chat in order to stop javascripts
                    message = new StringBuilder(client.doEscape(message.toString()));

                    // IC emote. [does this work server side? never seen it used.]
                    if (message.toString().startsWith("#me")) {
                        if (client.getConfig().isParam("COLORED_EMOTES")) {
                            message = new StringBuilder(String.format("*** %s%s", name, message.substring(3)));
                        } else {
                            message = new StringBuilder(String.format("*** %s%s", uncoloredName, message.substring(3)));
                        }
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));

                    } else {
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));

                        message = new StringBuilder(String.format("%s%s<b>:</b> %s", name, addon, message.toString().trim()));
                    }

                    // if the user wants to, remove any img tags
                    if (client.getConfig().isParam("NO_IMG_IN_CHAT")) {

                        int start = message.toString().toLowerCase().indexOf("<img");
                        int finish = -1;

                        if (start != -1) {
                            finish = message.indexOf(">", start);
                        }

                        if ((start != -1) && (finish != -1)) {
                            String firstHalf = message.substring(0, start);
                            String secondHalf = message.substring(finish + 1, message.length());

                            message = new StringBuilder(String.format("%s(img blocked)%s", firstHalf, secondHalf));
                        }
                    }

                    // add timestamp
                    if (client.getConfig().isParam("TIMESTAMP")) {
                        message.insert(0, client.getShortTime());
                    }

                    client.addToChat(message.toString(), CCommPanel.CHANNEL_RPG);

                    // also add to main, if configured to do so
                    if (client.getConfig().isParam("MAIN_CHANNEL_RPG")) {
                        client.addToChat(String.format("<font color=\"red\" size=\"%s\"><b>In Character: </b></font>%s", fontSize, message));
                    }
                }
            }// end In Character

            /*
             * Block for "normal" chat messages, which have a name as a lead in. This includes both standard chat and /me's.
             */
            else if (stringTokenizer.hasMoreTokens()) {

                // set the name
                name = nextString;
                uncoloredName = nextString;

                // don't display anything from a muted user
                if (!client.isIgnored(name, IClient.IGNORE_PUBLIC)) {

                    CUser user = (CUser) client.getUser(name);

                    isInvisible = user.isInvisible() &&
                                        (user.getUserLevel() > client.getUser(client.getUsername()).getUserLevel());

                    if ((user != null) && !isInvisible) {

                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getHtmlColor(); // Get the Color

                        // draw a faction color from the datafeed
                        if ((user.getHouse().length() > 1) &&
                                  (client.getData().getHouseByName(user.getHouse()) != null)) {
                            House currentHouse = client.getData().getHouseByName(user.getHouse());

                            if (currentHouse != null) {
                                factioncolor = currentHouse.getHouseColor();
                            } else {
                                LOGGER.info("House could not be found, defaulting the color");
                                factioncolor = defaultColor;
                            }
                        } else {
                            factioncolor = defaultColor;
                        }

                    } else {
                        // user is null. usually means player isn't
                        // logged into the campaign.
                        usercolor = defaultColor;
                        factioncolor = defaultColor;
                        addon = "";
                        name = "Someone";
                    }

                    /*
                     * Load name coloration setting. Used to mix and match various faction/player color combinations. Options are: 1) normal - use the colour info from CUser for name and addon. 2) factionall - faction colour completely supercedes player colour. 3) factionadd - player colour is used for name, but faction colour for label. 4) factionname - playercolour is replaced with faction color for name only. Colour the elements according.
                     */

                    if (MathUtility.parseBoolean(client.getConfigParam("INVERT_CHAT_COLOR"), false)) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(
                              factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = client.getConfig().getParam("PLAYER_CHAT_COLOR_MODE").toLowerCase();
                    if (colorSetting.equals("faction_add") || colorSetting.equals("faction_all")) {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", factioncolor, fontSize, addon);
                    } else {
                        addon = addon.isEmpty() ?
                                      "" :
                                      String.format(" <b><font color=\"%s\" size=\"%s\">[%s]</b></font>", usercolor, fontSize, addon);
                    }

                    if (colorSetting.equals("faction_name") || colorSetting.equals("faction_all")) {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", factioncolor, fontSize, name);
                    } else {
                        name = name.isEmpty() ?
                                     "" :
                                     String.format(" <b><font color=\"%s\" size=\"%s\">%s</b></font>", usercolor, fontSize, name);
                    }

                    message = new StringBuilder(stringTokenizer.nextToken());
                    while (stringTokenizer.hasMoreTokens()) {
                        message.append("|").append(stringTokenizer.nextToken());
                    }

                    // strip HTML from the chat to stop JavaScript
                    message = new StringBuilder(client.doEscape(message.toString()));

                    // handle emote ("me" command) formatting
                    if (message.toString().startsWith("#me")) {
                        if (client.getConfig().isParam("COLORED_EMOTES")) {
                            message = new StringBuilder(String.format("*** %s%s", name, message.substring(3)));
                        } else {
                            message = new StringBuilder(String.format("*** %s%s", uncoloredName, message.substring(3)));
                        }
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));
                    }

                    // if not me, its a normal message
                    else {
                        message = new StringBuilder(String.format("<font size=\"%s\">%s</font>", fontSize, message));
                        message = new StringBuilder(String.format("%s%s<b>:</b> %s", name, addon, message.toString().trim()));
                    }

                    // add timestamp
                    if (client.getConfig().isParam("TIMESTAMP")) {
                        message.insert(0, client.getShortTime());
                    }

                    // append message to chat
                    client.addToChat(message.toString());
                }
            }

            /*
             * Block for "Server" messages [errors, server feedback] and special chat channels. Housemail, Modmail, etc.
             */
            // else, this is a system message
            else {

                // load the message colour

                if (nextString.startsWith("AM:")) {
                    String sysColour = client.getConfigParam("SYS_MESSAGE_COLOR");
                    message = new StringBuilder(String.format("<font color=\"%s\"><b>%s</b></font>", sysColour, nextString.substring(3)));
                } else if (nextString.startsWith("ED:")) {
                    message = new StringBuilder(nextString.substring(3));
                    if (client.getConfig().isParam("ENABLE_ENEMY_DETECTED_SOUND")) {
                        client.doPlaySound(client.getConfigParam("SOUND_ON_ENEMY_DETECTED"));
                    }
                } else {
                    message = new StringBuilder(nextString);
                }
                wasSystemMessage = true;
                client.addToChat(message.toString());
            }

            /*
             * Check for sound triggers. 2 methods to fire a sound: - someone else calls the player's name - a word
             * from the player's keyword list is in the message Check for opt-outs before triggering. If a message
             * has both the player name and a keyword, the name sound dominates
             */
            boolean checkSysMessages = client.getConfig().isParam("SOUNDS_FROM_SYS_MESSAGES");
            if (!wasSystemMessage || checkSysMessages) {

                // MWLogger.errLog("uncoloredName: "+uncoloredName);
                if (wasICMessage &&
                          !client.getConfig().isParam("RPG_VISIBLE") &&
                          !client.getConfig().isParam("MAIN_CHANNEL_RPG")) {
                    // do nothing
                } else if ((!uncoloredName.equalsIgnoreCase(client.getUsername()) ||
                                  (uncoloredName.trim().isEmpty())) &&
                                 (message.indexOf(client.getUsername()) > -1)) {
                    if (client.getConfig().isParam("ENABLE_CALL_SOUND")) {
                        client.doPlaySound(client.getConfig().getParam("SOUND_ON_CALL"));
                    }

                    // keep logging, even if sound is disabled
                    client.addToChat(message.toString(), CCommPanel.CHANNEL_PERSONAL_LOG);// log the message
                } else if (client.hasKeyWords(message.toString())) {

                    if (client.getConfig().isParam("ENABLE_KEYWORD_SOUND")) {
                        client.doPlaySound(client.getConfig().getParam("SOUND_ON_KEYWORD"));
                    }

                    // keep logging, even if sound is disabled
                    client.addToChat(message.toString(), CCommPanel.CHANNEL_PERSONAL_LOG);
                }
            }

        }// end stringTokenizer has more tokens

    }// end execute

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     *
     */
    @Override
    public void setClient(IClient mwClient) {

    }

    /**
     *
     */
    @Override
    public void parseArguments(String s) {

    }
}// end CH class
