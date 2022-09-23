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

import client.CUser;
import client.MWClient;
import client.gui.CCommPanel;
import common.util.StringUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CH extends Command {

    /**
     * @param client
     */
    public CH(MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(java.lang.String)
     */
    @Override
    public void execute(String input) {

        // EMERGENCY BLOCK - use if and only if splash goes apeshit again
        /*
         * if (!client.getConfig().isParam("DEDICATED")){ if ( !client.getMainFrame().isVisible() ) client.getMainFrame().setVisible(true); client.getMainFrame().getMainPanel().setVisible(true); client.getMainFrame().getMainPanel().getPlayerPanel().setVisible(true); client.getMainFrame().getMainPanel().getCommPanel().setVisible(true); client.getMainFrame().getMainPanel().getUserListPanel().setVisible(true); }
         */
        boolean isInvisible = false;

        StringTokenizer ST = decode(input);
        if (ST.hasMoreTokens()) {

            // reserve variables
            String name = "";
            String uncoloredName = "";
            String message = "";
            String usercolor = "";
            String factioncolor = "";
            String addon = "";

            String nextString = ST.nextToken();
            String defaultColor = mwclient.getConfig().getParam("CHATFONTCOLOR");
            String fontSize = mwclient.getConfig().getParam("CHATFONTSIZE");

            boolean wasSystemMessage = false;
            boolean wasICMessage = false;

            if (nextString.startsWith("(Housemail)") || nextString.startsWith("(Moderator Mail)")) {

                boolean isModMail = false;
                if (nextString.startsWith("(Moderator Mail)")) {
                    isModMail = true;
                }

                // get the name
                if (isModMail) {
                    name = nextString.substring(16, nextString.indexOf(":")).trim();
                } else {
                    name = nextString.substring(11, nextString.indexOf(":"));
                }

                uncoloredName = name;

                // only proceed if player isnt muted
                CUser user = mwclient.getUser(name);
                if (user.isInvis() && (user.getUserlevel() > mwclient.getUser(mwclient.getUsername()).getUserlevel())) {
                    isInvisible = true;
                }

                if (!mwclient.isIgnored(name, MWClient.IGNORE_HOUSE) || isModMail) {

                    if (!isInvisible) {

                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getColor(); // Get the Color

                        // draw a factioncolour from the datafeed
                        if ((user.getHouse().length() > 1) && (mwclient.getData().getHouseByName(user.getHouse()) != null)) {
                            factioncolor = mwclient.getData().getHouseByName(user.getHouse()).getHouseColor();
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
                     * Set up colours. Sadly, all the colour code is extremely duplicative, but a necessary evil ATM since the messages come in with wildly different formatting. Should consider rewritting the messages to standardize things like name placement and headers.
                     */

                    if (Boolean.parseBoolean(mwclient.getConfigParam("INVERTCHATCOLOR"))) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = mwclient.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
                    if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    } else {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    }

                    if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
                        name = name.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    } else {
                        name = name.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    }

                    // load the message
                    message = nextString.substring(nextString.indexOf(":") + 1).trim();
                    while (ST.hasMoreTokens()) {
                        message += "|" + ST.nextToken();
                    }

                    // faction mail emote. [does this work server side? never seen it used.]
                    if (message.startsWith("#me")) {
                        if (mwclient.getConfig().isParam("COLOREDEMOTES")) {
                            message = "*** " + name + message.substring(3);
                        } else {
                            message = "*** " + uncoloredName + message.substring(3);
                        }
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                    }

                    // normal factionmail message
                    else {
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                        message = name + addon + "<b>:</b> " + message.trim();
                    }

                    // if the user wants to, remove any img tags
                    if (mwclient.getConfig().isParam("NOIMGINCHAT")) {

                        int start = message.toLowerCase().indexOf("<img");
                        int finish = -1;

                        if (start != -1) {
                            finish = message.indexOf(">", start);
                        }

                        if ((start != -1) && (finish != -1)) {
                            String firstHalf = message.substring(0, start);
                            String secondHalf = message.substring(finish + 1, message.length());

                            message = firstHalf + "(img blocked)" + secondHalf;
                        }
                    }

                    // add timestamp
                    if (mwclient.getConfig().isParam("TIMESTAMP")) {
                        message = mwclient.getShortTime() + message;
                    }

                    if (isModMail) {
                        mwclient.addToChat(message, CCommPanel.CHANNEL_MOD);
                     // also add to main, if configured to do so
                        if (mwclient.getConfig().isParam("MAINCHANNELMM")) {
                            mwclient.addToChat("<font color=\"red\" size=\"" + fontSize + "\"><b>Mod Mail: </b></font>" + message);
                        }
                    }
                    else {
                        // add message to faction panel
                        mwclient.addToChat(message, CCommPanel.CHANNEL_HMAIL);

                        // also add to main, if configured to do so
                        if (mwclient.getConfig().isParam("MAINCHANNELHM")) {
                            mwclient.addToChat("<font color=\"red\" size=\"" + fontSize + "\"><b>House Mail: </b></font>" + message);
                        }
                    }
                }
            }// end HM and MM

            // else, this is a system message
            else if (nextString.startsWith("(Error Log):")) {
                message = nextString.substring(nextString.indexOf(":") + 1);
                message = mwclient.getShortTime() + message;
                mwclient.addToChat(message, CCommPanel.CHANNEL_ERROR);
            }// end ErrorLog

            else if (nextString.startsWith("(In Character)")) {

                wasICMessage = true;

                name = nextString.substring(14, nextString.indexOf(":"));
                uncoloredName = name;

                // only proceed if player isnt muted
                CUser user = mwclient.getUser(name);
                if (user.isInvis() && (user.getUserlevel() > mwclient.getUser(mwclient.getUsername()).getUserlevel())) {
                    isInvisible = true;
                }

                if (!mwclient.isIgnored(name, MWClient.IGNORE_PUBLIC)) {

                    if (!isInvisible) {

                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getColor(); // Get the Color

                        // draw a factioncolour from the datafeed
                        if ((user.getHouse().length() > 1) && (mwclient.getData().getHouseByName(user.getHouse()) != null)) {
                            factioncolor = mwclient.getData().getHouseByName(user.getHouse()).getHouseColor();
                        } else {
                            factioncolor = defaultColor;
                        }

                    } else {
                        usercolor = defaultColor;
                        factioncolor = defaultColor;
                        addon = "";
                        if (isInvisible) {
                            name = "Someone";
                        }
                    }

                    /*
                     * Set up colours. Sadly, all the colour code is extremely duplicative, but a necessary evil ATM since the messages come in with wildly different formatting. Should consider rewritting the messages to standardize things like name placement and headers.
                     */

                    if (Boolean.parseBoolean(mwclient.getConfigParam("INVERTCHATCOLOR"))) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = mwclient.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
                    if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    } else {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    }

                    if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
                        name = name.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    } else {
                        name = name.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    }

                    // load the message
                    message = nextString.substring(nextString.indexOf(":") + 1);

                    while (ST.hasMoreTokens()) {
                        message += "|" + ST.nextToken();
                    }

                    // strip HTML from the chat in order to stop javascripts
                    message = mwclient.doEscape(message);

                    // IC emote. [does this work server side? never seen it used.]
                    if (message.startsWith("#me")) {
                        if (mwclient.getConfig().isParam("COLOREDEMOTES")) {
                            message = "*** " + name + message.substring(3);
                        } else {
                            message = "*** " + uncoloredName + message.substring(3);
                        }
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";

                    }

                    // normal message
                    else {
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";

                        message = name + addon + "<b>:</b> " + message.trim();
                    }

                    // if the user wants to, remove any img tags
                    if (mwclient.getConfig().isParam("NOIMGINCHAT")) {

                        int start = message.toLowerCase().indexOf("<img");
                        int finish = -1;

                        if (start != -1) {
                            finish = message.indexOf(">", start);
                        }

                        if ((start != -1) && (finish != -1)) {
                            String firstHalf = message.substring(0, start);
                            String secondHalf = message.substring(finish + 1, message.length());

                            message = firstHalf + "(img blocked)" + secondHalf;
                        }
                    }

                    // add timestamp
                    if (mwclient.getConfig().isParam("TIMESTAMP")) {
                        message = mwclient.getShortTime() + message;
                    }

                    mwclient.addToChat(message, CCommPanel.CHANNEL_RPG);

                    // also add to main, if configured to do so
                    if (mwclient.getConfig().isParam("MAINCHANNELRPG")) {
                        mwclient.addToChat("<font color=\"red\" size=\"" + fontSize + "\"><b>In Character: </b></font>" + message);
                    }
                }
            }// end In Character

            /*
             * Block for "normal" chat messages, which have a name as a lead in. This includes both standard chat and /me's.
             */
            else if (ST.hasMoreTokens()) {

                // set the name
                name = nextString;
                uncoloredName = nextString;

                // don't display anything from a muted user
                if (!mwclient.isIgnored(name, MWClient.IGNORE_PUBLIC)) {

                    CUser user = mwclient.getUser(name);

                    if (user.isInvis() && (user.getUserlevel() > mwclient.getUser(mwclient.getUsername()).getUserlevel())) {
                        isInvisible = true;
                    } else {
                        isInvisible = false;
                    }

                    if ((user != null) && !isInvisible) {

                        // faction letter
                        addon = user.getAddon();

                        // user's colour
                        usercolor = user.getColor(); // Get the Color

                        // draw a factioncolour from the datafeed
                        if ((user.getHouse().length() > 1) && (mwclient.getData().getHouseByName(user.getHouse()) != null)) {
                            factioncolor = mwclient.getData().getHouseByName(user.getHouse()).getHouseColor();
                        } else {
                            factioncolor = defaultColor;
                        }

                    } else {
                        // user is null. usually means player isn't
                        // logged into the campaign.
                        usercolor = defaultColor;
                        factioncolor = defaultColor;
                        addon = "";
                        if (isInvisible) {
                            name = "Someone";
                        }
                    }

                    /*
                     * Load name coloration setting. Used to mix and match various faction/player color combinations. Options are: 1) normal - use the colour info from CUser for name and addon. 2) factionall - faction colour completely supercedes player colour. 3) factionadd - player colour is used for name, but faction colour for label. 4) factionname - playercolour is replaced with faction color for name only. Colour the elements according.
                     */

                    if (Boolean.parseBoolean(mwclient.getConfigParam("INVERTCHATCOLOR"))) {
                        factioncolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(factioncolor)));
                        usercolor = StringUtils.color2html(StringUtils.invertColor(StringUtils.html2Color(usercolor)));
                    }

                    String colorSetting = mwclient.getConfig().getParam("PLAYERCHATCOLORMODE").toLowerCase();
                    if (colorSetting.equals("factionadd") || colorSetting.equals("factionall")) {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    } else {
                        addon = addon.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">[" + addon + "]</b></font>";
                    }

                    if (colorSetting.equals("factionname") || colorSetting.equals("factionall")) {
                        name = name.equals("") ? "" : " <b><font color=\"" + factioncolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    } else {
                        name = name.equals("") ? "" : " <b><font color=\"" + usercolor + "\" size=\"" + fontSize + "\">" + name + "</b></font>";
                    }

                    message = ST.nextToken();
                    while (ST.hasMoreTokens()) {
                        message += "|" + ST.nextToken();
                    }

                    // strip HTML from the chat in order to stop javascripts
                    message = mwclient.doEscape(message);

                    // handle emote ("me" command) formatting
                    if (message.startsWith("#me")) {
                        if (mwclient.getConfig().isParam("COLOREDEMOTES")) {
                            message = "*** " + name + message.substring(3);
                        } else {
                            message = "*** " + uncoloredName + message.substring(3);
                        }
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                    }

                    // if not me, its a normal message
                    else {
                        message = "<font size=\"" + fontSize + "\">" + message + "</font>";
                        message = name + addon + "<b>:</b> " + message.trim();
                    }

                    // add timestamp
                    if (mwclient.getConfig().isParam("TIMESTAMP")) {
                        message = mwclient.getShortTime() + message;
                    }

                    // append message to chat
                    mwclient.addToChat(message);
                }
            }

            /*
             * Block for "Server" messages [errors, server feedback] and special chat channels. Housemail, Modmail, etc.
             */
            // else, this is a system message
            else {

                // load the message colour

                if (nextString.startsWith("AM:")) {
                    String sysColour = mwclient.getConfigParam("SYSMESSAGECOLOR");
                    message = "<font color=\"" + sysColour + "\"><b>" + nextString.substring(3) + "</b></font>";
                } else if (nextString.startsWith("ED:")) {
                    message = nextString.substring(3);
                    if (mwclient.getConfig().isParam("ENABLEENEMYDETECTEDSOUND")) {
                        mwclient.doPlaySound(mwclient.getConfigParam("SOUNDONENEMYDETECTED"));
                    }
                } else {
                    message = nextString;
                }
                wasSystemMessage = true;
                mwclient.addToChat(message);
            }

            /*
             * Check for sound triggers. 2 methods to fire a sound: - someone else calls the player's name - a word from the player's keyword list is in the message Check for opt outs before triggering. If a message has both the playername and a keyword, the namesound dominates
             */
            boolean checkSysMessages = mwclient.getConfig().isParam("SOUNDSFROMSYSMESSAGES");
            if (!wasSystemMessage || checkSysMessages) {

                // MWLogger.errLog("uncoloredName: "+uncoloredName);
                if (wasICMessage && !mwclient.getConfig().isParam("RPGVISIBLE") && !mwclient.getConfig().isParam("MAINCHANNELRPG")) {
                    // do nothing
                }

                else if ((!uncoloredName.equalsIgnoreCase(mwclient.getUsername()) || (uncoloredName.trim().length() < 1)) && (message.indexOf(mwclient.getUsername()) > -1)) {
                    if (mwclient.getConfig().isParam("ENABLECALLSOUND")) {
                        mwclient.doPlaySound(mwclient.getConfig().getParam("SOUNDONCALL"));
                    }

                    // keep logging, even if sound is disabled
                    mwclient.addToChat(message, CCommPanel.CHANNEL_PLOG);// log the message
                }

                else if (mwclient.hasKeyWords(message)) {

                    if (mwclient.getConfig().isParam("ENABLEKEYWORDSOUND")) {
                        mwclient.doPlaySound(mwclient.getConfig().getParam("SOUNDONKEYWORD"));
                    }

                    // keep logging, even if sound is disabled
                    mwclient.addToChat(message, CCommPanel.CHANNEL_PLOG);
                }
            }

        }// end ST has more tokens

    }// end execute

}// end CH class
