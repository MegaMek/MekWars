/*
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


package mekwars.common.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Properties;
import java.util.TreeMap;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.IClientConfig;

/**
 * Concrete {@link IClientConfig} implementation for the MekWars GUI client. Owns the client's persisted key/value
 * preferences ({@link #config}, backed by a flat {@code mwconfig.txt} file) as well as a small cache of
 * pre-scaled {@link ImageIcon}s used by the GUI (status icons, logo, camo, etc.). Defaults for every recognized
 * config key are seeded by {@link #setDefaults()} before the on-disk file (if any) is loaded over them, so new
 * config keys added over time automatically get sane defaults for players with an older config file. Not used at
 * all for dedicated (headless) server processes beyond the plain key/value store - image loading is skipped
 * entirely when running dedicated (see the constructor).
 */
public class GUIClientConfig implements IClientConfig, Serializable {
    // VARIABLES
    /** Base directory (relative to the working directory) where GUI image assets are loaded from. */
    public static final String IMAGE_PATH = "data/images/";
    /** Base directory (relative to the working directory) where camouflage images are loaded from. */
    public static final String CAMO_PATH = "data/images/camo/";
    private static final MMLogger LOGGER = MMLogger.create(GUIClientConfig.class);
    /**
     * Serialization version identifier for this {@link IClientConfig} implementation.
     */
    @Serial
    private static final long serialVersionUID = 415432969624634387L;
    private final Properties config; // config. player values.
    private TreeMap<String, ImageIcon> images; // treemap with images

    // CONSTRUCTOR
    /**
     * Loads (or creates, if missing) the client's configuration file and, unless running dedicated, the fixed set
     * of GUI status/logo/tray/repair/camo images referenced by the config.
     * <p>
     * Loading order/fallbacks:
     * <ol>
     *   <li>{@link #setDefaults()} seeds every known key with a hardcoded default.</li>
     *   <li>If neither {@code CONFIG_FILE} nor {@code CONFIG_BACKUP_FILE} exists on disk yet, an empty
     *   {@code CONFIG_FILE} is created via {@link #createConfig()}.</li>
     *   <li>The primary config file is loaded if present, otherwise the backup file is loaded instead; if neither
     *   exists an error is logged and a dialog is shown to the user (the defaults from step 1 remain in effect).
     *   Any other exception during this step is likewise logged with a dialog shown, and the defaults remain in
     *   effect.</li>
     *   <li>A legacy {@code serverdata.dat} file, if present in the working directory, is merged in on top of the
     *   already-loaded config, immediately saved via {@link #saveConfig()}, and then deleted - this looks like a
     *   one-time migration path for a config format used by an older client version.</li>
     * </ol>
     * If {@code dedicated} is {@code true}, the {@code "DEDICATED"} config key is forced to {@code "TRUE"}
     * regardless of what was loaded from disk. Then, if the (possibly just-forced) config says this is a
     * dedicated instance, the constructor returns immediately without loading any images - the {@link #images} map
     * is left as the empty map assigned at the top of the constructor in that case.
     *
     * @param dedicated whether this client is running in dedicated (headless server) mode; if {@code true}, forces
     *                  the {@code DEDICATED} config flag on and skips image loading
     */
    public GUIClientConfig(boolean dedicated) {
        config = setDefaults();
        images = new TreeMap<>();

        // check to see if a config is present. if not, make one.
        if (!(new File(CONFIG_FILE).exists()) && !(new File(CONFIG_BACKUP_FILE).exists())) {
            createConfig();
        }

        // load the saved mwconfig.txt file
        try {
            File configFile = new File(CONFIG_FILE);
            if (!configFile.exists()) {
                configFile = new File(CONFIG_BACKUP_FILE);
            }

            if (configFile.exists()) {
                FileInputStream fis = new FileInputStream(configFile);
                config.load(fis);  // Here's the change.
                fis.close();
            } else {
                LOGGER.error("Neither Main nor Backup Config File Exists.");
                JOptionPane.showMessageDialog(null, "Unable to load Main nor Backup Config File.");
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Unhandled by other means. {}", ex.getLocalizedMessage());
            JOptionPane.showMessageDialog(null, "Unhandled exception by other means.");
        }

        // check for a serverdata.dat
        try {
            File configfile = new File("serverdata.dat");

            if (configfile.exists()) {
                FileInputStream fis = new FileInputStream(configfile);
                config.load(fis);
                fis.close();

                if (configfile.delete()) {
                    saveConfig();
                }
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "File exists but something else happened. {}", ex.getLocalizedMessage());
        }

        // if a -d arg was passed, set dedicated to true
        if (dedicated) {
            setParam("DEDICATED", "TRUE");
        }

        // dedicated have no gui, so dont load images.
        if (isParam("DEDICATED")) {
            return;
        }

        // not a ded, so fill the images treemap
        images = new TreeMap<>();

        loadImage(String.format("%slogout_colored.gif", IMAGE_PATH), "LOGOUT", 20, 20);
        loadImage(String.format("%sreserve_colored.gif", IMAGE_PATH), "RESERVE", 20, 20);
        loadImage(String.format("%sactive_colored.gif", IMAGE_PATH), "ACTIVE", 20, 20);
        loadImage(String.format("%sfighting_colored.gif", IMAGE_PATH), "FIGHT", 20, 20);
        loadImage(IMAGE_PATH + getParam("LOGO_IMAGE"), "LOGO", 100, 100);
        loadImage(IMAGE_PATH + getParam("TRAY_IMAGE"), "TRAY", 20, 20);
        loadImage(IMAGE_PATH + getParam("REPAIR_IMAGE"), "REPAIR", 100, 100);

        if (!getParam("UNIT_CAMO").trim().isEmpty()) {
            loadImage(CAMO_PATH + getParam("UNIT_CAMO"), "CAMO", 84, 72);
        }
    }

    // METHODS

    /**
     * Private method that loads hardcoded defaults. These are loaded before the players config values, adding any new
     * configs in their default position and ensuring that no config value is even missing.
     */
    private Properties setDefaults() {
        Properties defaults = new Properties();
        // general properties
        defaults.setProperty("NAME", "");
        defaults.setProperty("NAME_PASSWORD", "");
        defaults.setProperty("PORT", "2346");
        defaults.setProperty("SERVER_IP", "");
        defaults.setProperty("SERVER_PORT", "2347");
        defaults.setProperty("DATA_PORT", "4867");
        defaults.setProperty("AUTO_CONNECT", "NO");
        defaults.setProperty("TIMEOUT", "180");
        // GUI properties
        defaults.setProperty("LOOK_AND_FEEL", "system"); // look and feel type
        defaults.setProperty("TIMESTAMP", "YES");
        // dedicated properties
        defaults.setProperty("DEDICATED", "NO");
        defaults.setProperty("DEDICATED_OWNER_NAME", "");
        // MainFrame properties
        defaults.setProperty("SPLITTER_SIZE", "7"); // divider thickness
        defaults.setProperty("PLAYER_PANEL", "YES"); // visible player panel
        defaults.setProperty("PLAYER_PANEL_HEIGHT", "130"); // player panel height
        // (excluding logo height)
        defaults.setProperty("LOGO", "NO"); // logo visible
        defaults.setProperty("POP_UP_ON_ATTACK", "YES"); // pop up dialog on attack
        defaults.setProperty("POP_UP_ON_MESSAGE", "NO"); // pop up dialog on popup
        // message
        // HQ Panel properties
        defaults.setProperty("UNIT_CAMO", "Flame.jpg"); // camouflage for units
        // in hangar bay
        defaults.setProperty("UNIT_HEX", "NO"); // hexes for units in hangar bay
        defaults.setProperty("HQ_COLOR_SCHEME", "grey");// colors for unit
        // backgrounds in HQ
        defaults.setProperty("UNIT_AMOUNT", "10"); // hexes for units in hangar
        // bay
        // UserList properties
        defaults.setProperty("USER_LIST_BOLD", "YES"); // bold names on userlist
        defaults.setProperty("USER_LIST_COLOR", "YES"); // colored names on
        // userlist
        defaults.setProperty("USER_LIST_IMAGE", "YES"); // images on userlist
        defaults.setProperty("USER_LIST_COUNT", "YES"); // player count on
        // userlist
        defaults.setProperty("USER_LIST_DEDICATEDS", "NO"); // show dedicated
        // hosts on userlist
        defaults.setProperty("USER_LIST_ACTIVITY_BUTTON", "YES");// show
        // activate/deactivate
        // button
        // Chat properties
        defaults.setProperty("MAIN_CHANNEL_HM", "NO"); // show factionmail in main
        // channel
        defaults.setProperty("MAIN_CHANNEL_PM", "NO"); // show privatemail in main
        // channel
        defaults.setProperty("MAIN_CHANNEL_SM", "NO"); // show system messages in
        // main channel
        defaults.setProperty("MAIN_CHANNEL_MISC", "NO"); // show misc messages in
        // main channel
        defaults.setProperty("MAIN_CHANNEL_RPG", "NO"); // show in char messages
        // in main channel
        defaults.setProperty("MAIN_CHANNEL_MM", "NO"); // show modmail in main
        // channel
        defaults.setProperty("AUTOSCROLL", "NO"); // automatic chat scrolling
        defaults.setProperty("REPLY_TO_SENDER", "YES"); // PM tab replies to last
        // mail sender
        defaults.setProperty("REPLY_TO_RECEIVER", "NO"); // PM tab replies to last
        // mail sender
        defaults.setProperty("CHAT_FONT_SIZE", "+0");
        defaults.setProperty("CHAT_FONT_COLOR", "black");
        // sound properties
        defaults.setProperty("SOUND_ON_CALL", "./data/sounds/call.wav");
        defaults.setProperty("SOUND_ON_KEYWORD", "./data/sounds/call.wav");
        defaults.setProperty("SOUND_ON_MESSAGE", "./data/sounds/mail.wav");
        defaults.setProperty("SOUND_ON_ATTACK", "./data/sounds/attack.wav");
        defaults.setProperty("SOUND_ON_BMWIN", "./data/sounds/radarping.wav");
        defaults.setProperty("SOUND_ON_ACTIVATE", "./data/sounds/activate.wav");
        defaults.setProperty("SOUND_ON_DEACTIVATE", "./data/sounds/deactivate.wav");
        defaults.setProperty("SOUND_ON_ENEMY_DETECTED", "./data/sounds/enemy detected.wav");
        defaults.setProperty("SOUND_ON_EXIT_CLIENT", "./data/sounds/exit client.wav");
        defaults.setProperty("SOUND_ON_MENU", "./data/sounds/menu.wav");
        defaults.setProperty("SOUND_ON_MENU_POP_UP", "./data/sounds/menu popup.wav");

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

        // client colors
        defaults.setProperty("BACKGROUNDCOLOR", "#FFFFFF");

        defaults.setProperty("USERDEFINDMESSAGETAB", "0");
        defaults.setProperty("INVERTCHATCOLOR", "NO");

        defaults.setProperty("ShowUnitBaseBV", "false");

        // Developer stuffs
        defaults.setProperty("USETESTBUILDTABLEVIEWER", "false");
        defaults.setProperty("EXPANDEDUNITTOOLTIP", "true");

        return defaults;
    }

    /**
     * Creates an empty {@code mwconfig.txt} file on disk at {@code CONFIG_FILE} (relying on {@link #setDefaults()}
     * plus subsequent {@link #setParam} calls to populate in-memory values; nothing is actually written to the
     * file here beyond opening/immediately closing the stream). If the file cannot be created (e.g. due to folder
     * permissions), an error dialog is shown and the entire client process is terminated via {@code System.exit(0)}.
     * <p>
     * Note: the Javadoc this method previously carried claimed it wrote "commented-out old MMNET options" into the
     * file, but the implementation below does not write anything to the file at all - that description does not
     * match the current code and appears stale.
     */
    @Override
    public void createConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            PrintStream ps = new PrintStream(fos);

            ps.close();
            fos.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                  "Failed to create config file. Check folder write access privledges?");
            System.exit(0);
        }
    }

    /**
     * Looks up a config value by key. If {@code param} ends with a colon, the trailing colon is stripped before
     * lookup (accommodating callers that pass keys in a "label:" form). Never returns {@code null}: an unknown key
     * yields an empty string rather than {@code null} or a thrown exception.
     *
     * @param param the config key to look up (a trailing ":" is ignored)
     *
     * @return the associated value, or an empty string if the key is not set
     *
     * @see IClientConfig#getParam(String)
     */
    @Override
    public String getParam(String param) {
        String tparam;

        if (param.endsWith(":")) {
            param = param.substring(0, param.lastIndexOf(":"));
        }
        tparam = config.getProperty(param);
        if (tparam == null) {
            tparam = "";
        }
        return tparam;
    }

    /**
     * Sets (or overwrites) a config key's value in memory. Does not persist to disk by itself - call
     * {@link #saveConfig()} separately to write changes out.
     *
     * @see IClientConfig#setParam(String, String)
     */
    @Override
    public void setParam(String param, String value) {
        config.setProperty(param, value);
    }

    /**
     * Interprets a config value as a boolean. Accepts (case-insensitively) {@code "YES"}, {@code "TRUE"}, or
     * {@code "ON"} as truthy; any other value (including an unset key, which {@link #getParam} resolves to an
     * empty string) is treated as {@code false}.
     *
     * @param param the config key to look up
     *
     * @return {@code true} if the value is one of the recognized truthy strings, {@code false} otherwise
     *
     * @see IClientConfig#isParam(String)
     */
    @Override
    public boolean isParam(String param) {
        String tparam = getParam(param);
        return tparam.equalsIgnoreCase("YES") || tparam.equalsIgnoreCase("TRUE") || tparam.equalsIgnoreCase("ON");
    }

    /**
     * Interprets a config value as an integer, returning {@code 0} (rather than throwing) if the value is missing
     * or not a valid integer.
     *
     * @param param the config key to look up
     *
     * @return the parsed integer value, or {@code 0} if it could not be parsed
     *
     * @see IClientConfig#getIntParam(String)
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

    /**
     * Persists the in-memory config to disk in two steps: first writes a full copy to {@code CONFIG_BACKUP_FILE}
     * (as a backup), then writes the same data to the primary {@code CONFIG_FILE}. If the backup write fails, an
     * error is logged and the method returns early without attempting the primary write (leaving the previous
     * on-disk primary config untouched). If the primary write then fails, an error is logged but no further
     * recovery is attempted.
     *
     * @see IClientConfig#saveConfig()
     */
    @Override
    public void saveConfig() {

        try {

            java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_BACKUP_FILE);
            java.io.PrintStream ps = new java.io.PrintStream(fos);
            config.store(ps, "client Config Backup");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed backing up config file");
            return;
        }
        try {
            java.io.FileOutputStream fos = new java.io.FileOutputStream(CONFIG_FILE);
            java.io.PrintStream ps = new java.io.PrintStream(fos);
            config.store(ps, "client Config");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "Failed saving config file");
        }
    }

    /**
     * Get an ImageIcon from the client image cache.
     *
     * @param image - name of image to fetch
     *
     * @return an ImageIcon. Null if no match.
     */
    public javax.swing.ImageIcon getImage(String image) {
        return images.get(image);
    }

    /**
     * Checks whether the player has enabled any of the per-column "unit status" indicator options (used by the
     * unit status display to decide whether to draw the right/left icon columns at all). Returns {@code true} as
     * soon as any one of the individual right- or left-column status toggles (pilot-eject, repair, engine,
     * equipment, armor, ammo) is enabled.
     * <p>
     * Possible oversight: {@code RIGHTCOMMANDER}/{@code LEFTCOMMANDER} are defined as config keys (see
     * {@link #setDefaults()}) but are not checked here alongside the other five per-column options, so enabling
     * only the "commander" indicator would not cause this method to report {@code true}.
     *
     * @return {@code true} if at least one right/left status-icon option (other than "commander") is enabled
     */
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
        return Boolean.parseBoolean(getParam("LEFTAMMO"));
    }

    /**
     * Loads an image file from disk, scales it to the given dimensions, and stores it in the in-memory
     * {@link #images} cache under the given key (overwriting any previous entry for that key). Used by this
     * constructor to load the fixed set of GUI status/logo/tray/repair/camo images (see {@link #GUIClientConfig});
     * the only external caller elsewhere in the client is the camo-selection dialog, which uses this to swap in a
     * newly-chosen camo image under the {@code "CAMO"} key at runtime. A no-op if {@code imagename} is empty; any
     * other failure to load/scale the image (e.g. missing file) is logged and swallowed rather than propagated -
     * the map simply keeps whatever was previously cached under {@code image} (or nothing, if this was the first
     * attempt).
     * <p>
     * (Note: the Javadoc previously attached to this method referred to "the CConfig constructor" - {@code CConfig}
     * appears to be an older name for this class, presumably renamed to {@code GUIClientConfig} at some point.)
     *
     * @param imagename path to the image file to load (relative to the working directory); no-op if empty
     * @param image     the cache key to store the scaled image under (e.g. {@code "LOGOUT"}, {@code "CAMO"})
     * @param width     target width, in pixels, to scale the image to
     * @param height    target height, in pixels, to scale the image to
     */
    public void loadImage(String imagename, String image, int width, int height) {
        if (imagename.isEmpty()) {
            return;
        }
        try {
            images.put(image,
                  new javax.swing.ImageIcon(new javax.swing.ImageIcon(imagename).getImage()
                                                  .getScaledInstance(width, height, java.awt.Image.SCALE_DEFAULT)));
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to load image: {}", imagename);
        }
    }

}
