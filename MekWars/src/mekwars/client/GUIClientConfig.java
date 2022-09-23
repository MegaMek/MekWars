/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package client;

import java.awt.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import common.campaign.clientutils.IClientConfig;
import common.util.MWLogger;


/**
 * Class for Client's configuration.
 */
public class GUIClientConfig implements IClientConfig {

    /**
     *
     */
    private static final long serialVersionUID = 415432969624634387L;
    // VARIABLES
    public static final String IMAGE_PATH = "data/images/";
    public static final String CAMO_PATH = "data/images/camo/";
    private Properties config; // config. player values.
    private TreeMap<String, ImageIcon> images; // treemap with images

    // CONSTRUCTOR
    public GUIClientConfig(boolean dedicated) {

        config = setDefaults();
        images = new TreeMap<String, ImageIcon>();

        // check to see if a config is present. if not, make one.
        if (!(new File(CONFIG_FILE).exists()) && !(new File(CONFIG_BACKUP_FILE).exists())) {
            createConfig();
        }

        // load the saved mwconfig.txt file
        try {
            File configfile = new File(CONFIG_FILE);
            FileInputStream fis = new FileInputStream(configfile);

            config.load(fis);  // Here's the change.
            fis.close();
        } catch (IOException ie) {
            try {
                File configfile = new File(CONFIG_BACKUP_FILE);
                FileInputStream fis = new FileInputStream(configfile);
                config.load(fis);
                fis.close();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
                JOptionPane.showMessageDialog(null, "Unable to load Backup config file");
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            JOptionPane.showMessageDialog(null, "Unable to load main config file");
        }

        // check for a serverdata.dat
        try {
            File configfile = new File("serverdata.dat");
            FileInputStream fis = new FileInputStream(configfile);
            config.load(fis);
            fis.close();
            configfile.delete();
            saveConfig();
        } catch (FileNotFoundException fnfe) {
            // Exception simply means serverdata.dat is not present.
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

        // if a -d arg was passed, set dedicated to true
        if (dedicated) {
            setParam("DEDICATED", "TRUE");
        }

        // deds have no gui, so dont load images.
        if (isParam("DEDICATED")) {
            return;
        }

        // not a ded, so fill the images treemap
        images = new TreeMap<String, ImageIcon>();

        loadImage(IMAGE_PATH + "logout_colored.gif", "LOGOUT", 20, 20);
        loadImage(IMAGE_PATH + "reserve_colored.gif", "RESERVE", 20, 20);
        loadImage(IMAGE_PATH + "active_colored.gif", "ACTIVE", 20, 20);
        loadImage(IMAGE_PATH + "fighting_colored.gif", "FIGHT", 20, 20);
        loadImage(IMAGE_PATH + getParam("LOGOIMAGE"), "LOGO", 100, 100);
        loadImage(IMAGE_PATH + getParam("TRAYIMAGE"), "TRAY", 20, 20);
        loadImage(IMAGE_PATH + getParam("REPAIRIMAGE"), "REPAIR", 100, 100);
        if (getParam("UNITCAMO").trim().length() != 0) {
            loadImage(CAMO_PATH + getParam("UNITCAMO"), "CAMO", 84, 72);
        }
    }

    // METHODS
    /**
     * Private method that loads hardcoded defaults. These are loaded before the
     * players config values, adding any new configs in their default position
     * and ensuring that no config value is even missing.
     */
    private Properties setDefaults() {
        Properties defaults = new Properties();
        // general properties
        defaults.setProperty("NAME", "");
        defaults.setProperty("NAMEPASSWORD", "");
        defaults.setProperty("PORT", "2346");
        defaults.setProperty("SERVERIP", "");
        defaults.setProperty("SERVERPORT", "2347");
        defaults.setProperty("DATAPORT", "4867");
        defaults.setProperty("AUTOCONNECT", "NO");
        defaults.setProperty("TIMEOUT", "180");
        // GUI properties
        defaults.setProperty("LOOKANDFEEL", "system"); // look and feel type
        defaults.setProperty("TIMESTAMP", "YES");
        // dedicated properties
        defaults.setProperty("DEDICATED", "NO");
        defaults.setProperty("DEDICATEDOWNERNAME", "");
        // MainFrame properties
        defaults.setProperty("SPLITTERSIZE", "7"); // divider thickness
        defaults.setProperty("PLAYERPANEL", "YES"); // visible player panel
        defaults.setProperty("PLAYERPANELHEIGHT", "130"); // player panel height
                                                          // (excluding logo
                                                          // height)
        defaults.setProperty("LOGO", "NO"); // logo visible
        // defaults.setProperty("LOGOIMAGE","logo.jpg");
        defaults.setProperty("POPUPONATTACK", "YES"); // pop up dialog on attack
        defaults.setProperty("POPUPONMESSAGE", "NO"); // pop up dialog on popup
                                                      // message
        // HQ Panel properties
        defaults.setProperty("UNITCAMO", "Flame.jpg"); // camouflage for units
                                                       // in hangar bay
        defaults.setProperty("UNITHEX", "NO"); // hexes for units in hangar bay
        defaults.setProperty("HQCOLORSCHEME", "grey");// colors for unit
                                                      // backgrounds in HQ
        defaults.setProperty("UNITAMOUNT", "10"); // hexes for units in hangar
                                                  // bay
        // UserList properties
        defaults.setProperty("USERLISTBOLD", "YES"); // bold names on userlist
        defaults.setProperty("USERLISTCOLOR", "YES"); // colored names on
                                                      // userlist
        defaults.setProperty("USERLISTIMAGE", "YES"); // images on userlist
        defaults.setProperty("USERLISTCOUNT", "YES"); // player count on
                                                      // userlist
        defaults.setProperty("USERLISTDEDICATEDS", "NO"); // show dedicated
                                                          // hosts on userlist
        defaults.setProperty("USERLISTACTIVITYBTN", "YES");// show
                                                           // activate/deactivate
                                                           // button
        // Chat properties
        defaults.setProperty("MAINCHANNELHM", "NO"); // show factionmail in main
                                                     // channel
        defaults.setProperty("MAINCHANNELPM", "NO"); // show privatemail in main
                                                     // channel
        defaults.setProperty("MAINCHANNELSM", "NO"); // show system messages in
                                                     // main channel
        defaults.setProperty("MAINCHANNELMISC", "NO"); // show misc messages in
                                                       // main channel
        defaults.setProperty("MAINCHANNELRPG", "NO"); // show in char messages
                                                      // in main channel
        defaults.setProperty("MAINCHANNELMM", "NO"); // show modmail in main
                                                      // channel
        defaults.setProperty("AUTOSCROLL", "NO"); // automatic chat scrolling
        defaults.setProperty("REPLYTOSENDER", "YES"); // PM tab replies to last
                                                      // mail sender
        defaults.setProperty("REPLYTORECEIVER", "NO"); // PM tab replies to last
                                                       // mail sender
        defaults.setProperty("CHATFONTSIZE", "+0");
        defaults.setProperty("CHATFONTCOLOR", "black");
        // sound properties
        defaults.setProperty("SOUNDONCALL", "./data/sounds/call.wav");
        defaults.setProperty("SOUNDONKEYWORD", "./data/sounds/call.wav");
        defaults.setProperty("SOUNDONMESSAGE", "./data/sounds/mail.wav");
        defaults.setProperty("SOUNDONATTACK", "./data/sounds/attack.wav");
        defaults.setProperty("SOUNDONBMWIN", "./data/sounds/radarping.wav");
        defaults.setProperty("SOUNDONACTIVATE", "./data/sounds/activate.wav");
        defaults.setProperty("SOUNDONDEACTIVATE", "./data/sounds/deactivate.wav");
        defaults.setProperty("SOUNDONENEMYDETECTED", "./data/sounds/enemy detected.wav");
        defaults.setProperty("SOUNDONEXITCLIENT", "./data/sounds/exit client.wav");
        defaults.setProperty("SOUNDONMENU", "./data/sounds/menu.wav");
        defaults.setProperty("SOUNDONMENUPOPUP", "./data/sounds/menu popup.wav");

        defaults.setProperty("ENABLECALLSOUND", "YES");
        defaults.setProperty("ENABLEKEYWORDSOUND", "YES");
        defaults.setProperty("ENABLEMESSAGESOUND", "YES");
        defaults.setProperty("ENABLEATTACKSOUND", "YES");
        defaults.setProperty("ENABLEBMSOUND", "YES");
        defaults.setProperty("ENABLEACTIVATESOUND", "YES");
        defaults.setProperty("ENABLEDEACTIVATESOUND", "YES");
        defaults.setProperty("ENABLEENEMYDETECTEDSOUND", "YES");
        defaults.setProperty("ENABLEEXITCLIENTSOUND", "YES");
        defaults.setProperty("ENABLEMENUSOUND", "YES");
        defaults.setProperty("ENABLEMENUPOPUPSOUND", "YES");

        defaults.setProperty("SOUNDSFROMSYSMESSAGES", "NO");
        // tab properties
        defaults.setProperty("HQTABVISIBLE", "YES");
        defaults.setProperty("HQTABNAME", "Headquarters");
        defaults.setProperty("HQINTOPROW", "YES");
        defaults.setProperty("HQMNEMONIC", "Q");
        defaults.setProperty("RULESTABVISIBLE", "YES"); //@Salient
        defaults.setProperty("RULESTABNAME", "Rules");
        defaults.setProperty("RULESINTOPROW", "YES");
        defaults.setProperty("RULESMNEMONIC", "Z");
        defaults.setProperty("BMTABVISIBLE", "YES");
        defaults.setProperty("BMTABNAME", "Black Market");
        defaults.setProperty("BMINTOPROW", "YES");
        defaults.setProperty("BMMNEMONIC", "L");
        defaults.setProperty("BMETABVISIBLE", "YES");
        defaults.setProperty("BMETABNAME", "Parts Black Market");
        defaults.setProperty("BMEINTOPROW", "YES");
        defaults.setProperty("BMEMNEMONIC", "P");
        defaults.setProperty("HSTATUSTABVISIBLE", "YES");
        defaults.setProperty("HSTATUSTABNAME", "Faction Status");
        defaults.setProperty("HSTATUSINTOPROW", "Yes");
        defaults.setProperty("HSTATUSMNEMONIC", "U");
        defaults.setProperty("BATTLETABVISIBLE", "YES");
        defaults.setProperty("BATTLETABNAME", "Battles");
        defaults.setProperty("BATTLEINTOPROW", "YES");
        defaults.setProperty("BATTLEMNEMONIC", "B");
        defaults.setProperty("MAPTABVISIBLE", "YES");
        defaults.setProperty("MAPTABONCLICK", "YES");
        defaults.setProperty("MAPTABNAME", "Map");
        defaults.setProperty("MAPINTOPROW", "NO");
        defaults.setProperty("MAPMNEMONIC", "A");
        defaults.setProperty("PANELDIVIDER", "200");
        defaults.setProperty("PLAYERPANELDIVIDER", "-1");
        defaults.setProperty("VERTICALDIVIDER", "800");
        defaults.setProperty("MAINCHANNELTABNAME", "Main Channel");
        defaults.setProperty("MAINCHANNELMNEMONIC", "M");
        defaults.setProperty("HOUSEMAILVISIBLE", "YES");
        defaults.setProperty("HOUSEMAILTABNAME", "Faction Channel");
        defaults.setProperty("HOUSEMAILMNEMONIC", "H");
        defaults.setProperty("PRIVATEMAILVISIBLE", "YES");
        defaults.setProperty("PRIVATEMAILTABNAME", "Private Channel");
        defaults.setProperty("PRIVATEMAILMNEMONIC", "P");
        defaults.setProperty("PERSONALLOGVISIBLE", "YES");
        defaults.setProperty("PERSONALLOGTABNAME", "Personal Log");
        defaults.setProperty("PERSONALLOGMNEMONIC", "L");
        defaults.setProperty("SYSTEMLOGVISIBLE", "NO");
        defaults.setProperty("SYSTEMLOGTABNAME", "System Log");
        defaults.setProperty("SYSTEMLOGMNEMONIC", "Y");
        defaults.setProperty("MISCELLANEOUSVISIBLE", "YES");
        defaults.setProperty("MISCELLANEOUSTABNAME", "Miscellaneous");
        defaults.setProperty("MISCELLANEOUSMNEMONIC", "A");
        defaults.setProperty("RPGVISIBLE", "NO");
        defaults.setProperty("RPGTABNAME", "RP Channel");
        defaults.setProperty("RPGMNEMONIC", "R");
        defaults.setProperty("F1BIND", "mystatus");
        defaults.setProperty("F2BIND", "");
        defaults.setProperty("F3BIND", "");
        defaults.setProperty("F4BIND", "");
        defaults.setProperty("F5BIND", "");
        defaults.setProperty("DEFAULTARMYNAME", "");
        defaults.setProperty("DEDAUTORESTART", "10");
        defaults.setProperty("CAMPAIGNSERVERNAME", "MekWars Server");
        defaults.setProperty("STATUSINTRAYICON", "NO");
        defaults.setProperty("TRAYIMAGE", "reserve_colored.gif");
        defaults.setProperty("REPAIRIMAGE", "repair.gif");
        defaults.setProperty("DISABLEALLSOUND", "false");// option menu, mute.
        defaults.setProperty("SORTMODE", "NAME");// player list sort
        defaults.setProperty("SORTORDER", "ASCENDING");// player list order
        defaults.setProperty("BMSORTCOLUMN", "-2");
        defaults.setProperty("BMSORTORDER", "true");
        defaults.setProperty("BATTLESSORTCOLUMN", "0");
        defaults.setProperty("BATTLESSORTORDER", "true");
        defaults.setProperty("TABLEBROWSERSORTCOLUMN", "3");
        defaults.setProperty("TABLEBROWSERSORTORDER", "false");
        defaults.setProperty("BMESORTCOLUMN", "-2");
        defaults.setProperty("BMESORTORDER", "true");
        defaults.setProperty("UNITVIEWERWEIGHT", "All");
        defaults.setProperty("UNITVIEWERTECH", "All");
        defaults.setProperty("UNITVIEWERTYPE", "Mek");
        defaults.setProperty("UNITVIEWERSORT", "Name");
        defaults.setProperty("UNITVIEWERUNIT", "-1");

        defaults.setProperty("TABLEVIEWERFACTION", "");
        defaults.setProperty("TABLEVIEWERTYPE", "Mek");
        defaults.setProperty("TALEVIEWERWEIGHT", "Light");

        defaults.setProperty("PRIMARYHQSORTORDER", "name");
        defaults.setProperty("SECONDARYHQSORTORDER", "none");
        defaults.setProperty("TERTIARYHQSORTORDER", "none");
        defaults.setProperty("PRIMARYARMYSORTORDER", "id number");
        defaults.setProperty("DARKERMAP", "false");
        defaults.setProperty("BMPREVIEWIMAGE", "false");
        defaults.setProperty("PLAYERCHATCOLORMODE", "playercolors");
        defaults.setProperty("COLOREDEMOTES", "false");
        defaults.setProperty("SELECTEDPLANET", "");
        defaults.setProperty("MAPZOOMLEVEL", "1");
        defaults.setProperty("MAPYOFFSET", "0");
        defaults.setProperty("MAPXOFFSET", "0");
        defaults.setProperty("SHOWENTERANDEXIT", "true");
        defaults.setProperty("CHALLENGESTRING", "Looking for a game at");
        defaults.setProperty("SYSMESSAGECOLOR", "red");
        defaults.setProperty("NOIMGINCHAT", "false");
        defaults.setProperty("SERVERPORT", "2347");
        defaults.setProperty("DATAPORT", "4867");
        defaults.setProperty("PORT", "2346");
        defaults.setProperty("MAXPLAYERS", "12");
        defaults.setProperty("MAXSAVEDGAMEDAYS", "30");
        defaults.setProperty("VIEWFLUFF", "false");
        defaults.setProperty("USEMULTIPLEPM", "false");
        defaults.setProperty("MAXPMTABS", "5");
        defaults.setProperty("MAXPMMESSAGE", "Sorry I Am Busy Try Again Later.");
        defaults.setProperty("MAPFILTER1", "true$false$true$true$true$true$true");
        defaults.setProperty("MAPFILTER2", "true$false$true$true$true$true$true");
        defaults.setProperty("TRAYIMAGE", "reserve_colored.gif");

        // empty ignore & keyword lists
        defaults.setProperty("IGNOREPUBLIC", "");
        defaults.setProperty("IGNOREHOUSE", "");
        defaults.setProperty("IGNOREPRIVATE", "");
        defaults.setProperty("KEYWORDS", "");

        defaults.setProperty("DEDUPDATECOMMANDFILE", "");
        defaults.setProperty("AUTOUPDATECOMMANDFILE", "");

        defaults.setProperty("MAPOVERLAYCOLOR", "#D3D3D3");
        defaults.setProperty("SOCKETTIMEOUTDELAY", "2000");

        // Star Map Image Over Lay Settings
        defaults.setProperty("MAPIMAGEX", "0");
        defaults.setProperty("MAPIMAGEY", "0");
        defaults.setProperty("MAPIMAGEHEIGHT", "100");
        defaults.setProperty("MAPIMAGEWIDTH", "100");

        defaults.setProperty("UPDATEKEY", "-1");
        defaults.setProperty("DEDMEMORY", "256");  //Had to up this - new MM is taking way more memory than before

        // unitstatus setting
        // Right column
        defaults.setProperty("RIGHTCOLUMNDYNAMIC", "false");
        defaults.setProperty("RIGHTPILOTEJECT", "false");
        defaults.setProperty("RIGHTREPAIR", "false");
        defaults.setProperty("RIGHTENGINE", "false");
        defaults.setProperty("RIGHTEQUIPMENT", "false");
        defaults.setProperty("RIGHTARMOR", "false");
        defaults.setProperty("RIGHTAMMO", "false");
        defaults.setProperty("RIGHTCOMMANDER", "false");
        // Left Column
        defaults.setProperty("LEFTCOLUMNDYNAMIC", "false");
        defaults.setProperty("LEFTPILOTEJECT", "false");
        defaults.setProperty("LEFTREPAIR", "false");
        defaults.setProperty("LEFTENGINE", "false");
        defaults.setProperty("LEFTEQUIPMENT", "false");
        defaults.setProperty("LEFTARMOR", "false");
        defaults.setProperty("LEFTAMMO", "false");
        defaults.setProperty("LEFTCOMMANDER", "false");

        // enable or disable splash screen
        defaults.setProperty("ENABLESPLASHSCREEN", "true");

        // Window Locations
        defaults.setProperty("WINDOWSTATE", "0");
        defaults.setProperty("WINDOWHEIGHT", "100");
        defaults.setProperty("WINDOWWIDTH", "100");
        defaults.setProperty("WINDOWLEFT", "0");
        defaults.setProperty("WINDOWTOP", "0");

        // Bulk Repair Options
        defaults.setProperty("REPAIRARMORTECH", "0");
        defaults.setProperty("REPAIRARMORROLL", "9");
        defaults.setProperty("REPAIRINTERNALTECH", "0");
        defaults.setProperty("REPAIRINTERNALROLL", "9");
        defaults.setProperty("REPAIRWEAPONSTECH", "0");
        defaults.setProperty("REPAIRWEAPONSROLL", "9");
        defaults.setProperty("REPAIREQUIPMENTTECH", "0");
        defaults.setProperty("REPAIREQUIPMENTROLL", "9");
        defaults.setProperty("REPAIRSYSTEMSTECH", "0");
        defaults.setProperty("REPAIRSYSTEMSROLL", "9");
        defaults.setProperty("REPAIRENGINESTECH", "0");
        defaults.setProperty("REPAIRENGINESROLL", "9");

        // Salvage Options
        defaults.setProperty("SALVAGEARMORTECH", "0");
        defaults.setProperty("SALVAGEINTERNALTECH", "0");
        defaults.setProperty("SALVAGEWEAPONSTECH", "0");
        defaults.setProperty("SALVAGEEQUIPMENTTECH", "0");
        defaults.setProperty("SALVAGESYSTEMSTECH", "0");
        defaults.setProperty("SALVAGEENGINESTECH", "0");

        // Client colors
        defaults.setProperty("BACKGROUNDCOLOR", "#FFFFFF");

        defaults.setProperty("USERDEFINDMESSAGETAB", "0");
        defaults.setProperty("INVERTCHATCOLOR", "NO");

        defaults.setProperty("ShowUnitBaseBV", "false");

        // Developer stuffs
        defaults.setProperty("USETESTBUILDTABLEVIEWER", "false");
        defaults.setProperty("EXPANDEDUNITTOOLTIP","true");

        return defaults;
    }

    // Creates a new config file
    /*
     * All this does ATM is create an empty mwconfig.txt. Lines commented out
     * are old MMNET options that the client code supports, but which are not
     * presented to the user in the MekWars client GUI. The vast majority are
     * totally unused because the players don't know about them. Over time, the
     * options will be made public or removed.
     */
    /* (non-Javadoc)
	 * @see client.IClientConfig#createConfig()
	 */
    @Override
	public void createConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            PrintStream ps = new PrintStream(fos);

            /*
             * Options below are supported in code, but not yet in config
             * dialog.
             */
            // ps.println("#If you want a Password for your game, enter it here");
            // ps.println("#GAMEPASSWORD: ");
            // ps.println("#Size of chat font (as in html font tag)");
            // ps.println("#CHATFONTSIZE: +0");
            // ps.println("#Color of chat font (as in html font tag)");
            // ps.println("#CHATFONTCOLOR: black");
            // ps.println("#If you don't want the Player Panel to be visible, set it to NO");
            // ps.println("PLAYERPANEL: YES");
            // ps.println("#Put player panel height in pixels (excluding logo height) here (default is 130)");
            // ps.println("#PLAYERPANELHEIGHT: 130");
            // ps.println("#If you don't want the Logo to be visible, set it to NO");
            // ps.println("LOGO: NO");
            // ps.println("#A picture (from /data/images) to be shown as your logo in client. If you comment it out, it will download your unit logo!");
            // ps.println("LOGOIMAGE: logo.jpg");
            // ps.println("#The thickness of splitters dividing client windows");
            // ps.println("#SPLITTERSIZE: 7");
            // ps.println("#set to NO if you do not want server messages to appear as popup.");
            // ps.println("#if turned off, messages will appear in main chat.");
            // ps.println("POPUPONMESSAGE: NO");
            /*
             * Options below are supported in config dialog.
             */
            // ps.println("#Your Color used for your name in the chat ");
            // ps.println("#Color of your name in chat.");
            // ps.println("#Choices: standard HTML colours, any hex colour with a Red Value under AA)");
            // ps.println("COLOR: black");
            // ps.println("#IP of MekWars Server you are connecting to");
            // ps.println("#Servers listed on forums @ http://www.sourceforge.net/projects/mekwars");
            // ps.println("SERVERIP: SEE THE MEKWARS PROJECT PAGE FOR A LIST OF KNOWN SERVERS");
            // ps.println("#MegaMek host settings");
            // ps.println("#IF and only IF your ip isn't detected correctly, you may edit this setting (very unlikely that this happens)");
            // ps.println("#This is your current IP, needed to host games. You can use a Dynamically assigned DNS entry or just your plain IP here.");
            // ps.println("#If you don't know your IP-Address try this website: http://www.whatismyip.com");
            // ps.println("#You only need to enable that line if your IP isn't shown correctly when you create a game.");
            // ps.println("#IP: 127.0.0.1");
            // ps.println("#The maximum number of players you want to join your host. (If you host a game) Default: 12");
            // ps.println("MAXPLAYERS: 12");
            // ps.println("#A comment for your game (If you host a game)");
            // ps.println("COMMENT: ");
            // ps.println("#SOUND SETTINGS");
            // ps.println("#Play this file if anyone calls my name");
            // ps.println("SOUNDONCALL: ./data/sounds/call.wav");
            // ps.println("#Play this file when a Player joins the room");
            // ps.println("#SOUNDONJOIN: ./data/sounds/join.wav");
            // ps.println("#Play this file when a Player exits the room");
            // ps.println("#SOUNDONEXIT: ./data/sounds/exit.wav");
            // ps.println("#Play this file when someone sends you a message");
            // ps.println("SOUNDONMESSAGE: ./data/sounds/mail.wav");
            // ps.println("#Play this file when someone attacks you");
            // ps.println("SOUNDONATTACK: ./data/sounds/attack.wav");
            // ps.println("#Dedicated server settings");
            // ps.println("#Should this be a Dedicated Server ONLY?");
            // ps.println("DEDICATED: NO");
            // ps.println("#(Only if Dedicated Only) Put names of people allowed to reset him here, separated with commas");
            // ps.println("DEDICATEDOWNERNAME: ");
            // ps.println("#If you don't want to see news and statuses in Main Channel, set it to NO");
            // ps.println("MAINCHANNELNEWS: YES");
            // ps.println("#If you don't want to see faction mails in Main Channel, set it to NO");
            // ps.println("MAINCHANNELHM: NO");
            // ps.println("#If you don't want to see private mails in Main Channel, set it to NO");
            // ps.println("MAINCHANNELPM: NO");
            // ps.println("#If you don't want to see system messages in Main Channel, set it to NO");
            // ps.println("MAINCHANNELSM: NO");
            // ps.println("#If you don't want to see misc messages in Main Channel, set it to NO");
            // ps.println("MAINCHANNELMISC: NO");
            // ps.println("#If you want to hear a sound when specific word is received, put them in here, separated with commas");
            // ps.println("#REPLYTOSENDER: YES");
            // ps.println("#If you don't want PM tab reply to last mail receiver, set it to NO");
            // ps.println("#REPLYTORECEIVER: NO");
            // ps.println("#If you don't want dialog to popup on when you are attacked, set it to NO");
            // ps.println("#POPUPONATTACK: YES");
            // ps.println("#PanelDivider set between 1-100");
            // ps.println("PANELDIVIDER: 40");
            // ps.println("#bind commands to Function keys. /c is automatically added.");
            // ps.println("#Example binding for F1 to mysatus and transfermomey follow.");
            // ps.println("#F1BIND: transfermoney#urgru#30");
            // ps.println("F1BIND: mystatus");
            // ps.println("F2BIND:");
            // ps.println("F3BIND:");
            // ps.println("F4BIND:");
            // ps.println("F5BIND:");
            // ps.println("#number of games a ded will play before it restarts");
            // ps.println("DEDAUTORESTART: 20");
            // these should be pre-empted by the serverdata.dat values set by
            // server op
            // ps.println("CAMPAIGNSERVERNAME: MekWars Server");
            // ps.println("TRAYIMAGE: reserve_colored.gif");
            // ps.println("UPDATEKEY: -1");
            ps.close();
            fos.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Failed to create config file. Check folder write access privledges?");
            System.exit(0);
        }
    }

    /**
     * Load an image. Used by the CConfig constructor to load client images (eg
     * - player list icons). Only external call is from the camo dialog and is
     * used to replace the UNITCAMO image with the newly selected imageicon.
     */
    public void loadImage(String imagename, String image, int width, int height) {
        if (imagename.equals("")) {
            return;
        }
        try {
            images.put(image, new ImageIcon(new ImageIcon(imagename).getImage().getScaledInstance(width, height, Image.SCALE_DEFAULT)));
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    /**
     * Get an ImageIcon from the client image cache.
     *
     * @param image
     *            - name of image to fetch
     * @return an ImageIcon. Null if no match.
     */
    public ImageIcon getImage(String image) {
        return images.get(image);
    }

    /* (non-Javadoc)
	 * @see client.IClientConfig#getParam(java.lang.String)
	 */
    @Override
	public String getParam(String param) {
        String tparam = null;

        if (param.endsWith(":")) {
            param = param.substring(0, param.lastIndexOf(":"));
        }
        tparam = config.getProperty(param);
        if (tparam == null) {
            tparam = "";
        }
        return tparam;
    }

    /* (non-Javadoc)
	 * @see client.IClientConfig#setParam(java.lang.String, java.lang.String)
	 */
    @Override
	public void setParam(String param, String value) {
        config.setProperty(param, value);
    }

    /* (non-Javadoc)
	 * @see client.IClientConfig#isParam(java.lang.String)
	 */
    @Override
	public boolean isParam(String param) {
        String tparam = getParam(param);
        if (tparam.equalsIgnoreCase("YES") || tparam.equalsIgnoreCase("TRUE") || tparam.equalsIgnoreCase("ON")) {
            return true;
        }
        return false;
    }

    public boolean isUsingStatusIcons() {

        if (Boolean.parseBoolean(getParam("RIGHTPILOTEJECT"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("RIGHTREPAIR"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("RIGHTENGINE"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("RIGHTEQUIPMENT"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("RIGHTARMOR"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("RIGHTAMMO"))) {
            return true;
        }

        if (Boolean.parseBoolean(getParam("LEFTPILOTEJECT"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("LEFTREPAIR"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("LEFTENGINE"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("LEFTEQUIPMENT"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("LEFTARMOR"))) {
            return true;
        }
        if (Boolean.parseBoolean(getParam("LEFTAMMO"))) {
            return true;
        }

        return false;
    }

    /* (non-Javadoc)
	 * @see client.IClientConfig#getIntParam(java.lang.String)
	 */
    @Override
	public int getIntParam(String param) {
        int toReturn;
        try {
            toReturn = Integer.parseInt(getParam(param));
        } catch (Exception ex) {
            return 0;
        }
        return toReturn;
    }

    /* (non-Javadoc)
	 * @see client.IClientConfig#saveConfig()
	 */
    @Override
	public void saveConfig() {

        try {

            FileOutputStream fos = new FileOutputStream(CONFIG_BACKUP_FILE);
            PrintStream ps = new PrintStream(fos);
            config.store(ps, "Client Config Backup");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Failed backingup config file");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            PrintStream ps = new PrintStream(fos);
            config.store(ps, "Client Config");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Failed saving config file");
        }
    }

}
