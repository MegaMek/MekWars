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

package mekwars.dedicatedhost;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.IClientConfig;

/**
 * Class for client's configuration.
 */
public class DedConfig implements IClientConfig {
    public static final String CONFIG_FILE = "./data/mwconfig.txt";
    public static final String CONFIG_BACKUP_FILE = "./data/mwconfig.txt.bak";
    private static final MMLogger LOGGER = MMLogger.create(DedConfig.class);
    private Properties config;                //config. player values.

    //CONSTRUCTOR
    public DedConfig(boolean dedicated) {

        config = setDefaults();

        //check to see if a config is present. if not, make one.
        if (!(new File(CONFIG_FILE).exists()) && !(new File(CONFIG_BACKUP_FILE).exists())) {createConfig();}

        //load the saved mwconfig.txt file
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
                LOGGER.error(ex, "");
                JOptionPane.showMessageDialog(null, "Unable to load Backup config file");
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            JOptionPane.showMessageDialog(null, "Unable to load main config file");
        }
        //check for a serverdata.dat
        try {
            File configfile = new File("serverdata.dat");
            FileInputStream fis = new FileInputStream(configfile);
            config.load(fis);
            fis.close();
            configfile.delete();
            this.saveConfig();
        } catch (FileNotFoundException fnfe) {
            //Exception simply means serverdata.dat is not present.
        } catch (Exception ex) {
            LOGGER.error(ex, "");
        }

        setParam("DEDICATED", "TRUE");

    }

    //METHODS

    /**
     * Private method that loads hardcoded defaults. These are loaded before the players config values, adding any new
     * configs in their default position and ensuring that no config value is even missing.
     */
    private Properties setDefaults() {
        Properties defaults = new Properties();
        //general properties
        defaults.setProperty("NAME", "");
        defaults.setProperty("NAMEPASSWORD", "");
        defaults.setProperty("PORT", "2346");
        defaults.setProperty("SERVERIP", "");
        defaults.setProperty("SERVERPORT", "2347");
        defaults.setProperty("DATAPORT", "4867");
        defaults.setProperty("TIMEOUT", "180");
        //dedicated properties
        defaults.setProperty("DEDICATED", "YES");
        defaults.setProperty("DEDICATEDOWNERNAME", "");
        defaults.setProperty("DEDAUTORESTART", "10");
        defaults.setProperty("SERVERPORT", "2347");
        defaults.setProperty("DATAPORT", "4867");
        defaults.setProperty("PORT", "2346");
        defaults.setProperty("MAXPLAYERS", "12");
        defaults.setProperty("MAXSAVEDGAMEDAYS", "7");
        defaults.setProperty("UPDATEKEY", "-1");
        defaults.setProperty("DEDMEMORY", "256");

        return defaults;
    }

    //Creates a new config file
    /*
     * All this does ATM is create an empty mwconfig.txt. Lines commented out
     * are old MMNET options that the client code supports, but which are not
     * presented to the user in the MekWars client GUI. The vast majority are
     * totally unused because the players don't know about them. Over time, the
     * options will be made public or removed.
     */
    public void createConfig() {
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            PrintStream ps = new PrintStream(fos);

            ps.close();
            fos.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Failed to create config file. Check folder write access privledges?");
            System.exit(0);
        }
    }

    /**
     * Get a config value.
     */
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

    /**
     * Set a config value.
     */
    public void setParam(String param, String value) {
        config.setProperty(param, value);
    }

    /**
     * See if a paramater is enabled (YES, TRUE or ON).
     */
    public boolean isParam(String param) {
        String tparam = getParam(param);
        if (tparam.equalsIgnoreCase("YES") || tparam.equalsIgnoreCase("TRUE") || tparam.equalsIgnoreCase("ON")) {
            return true;
        }
        return false;
    }

    /**
     * Return the int value of a given config property. Return a 0 if the property is a non-number. Used mostly by the
     * misc. mail tab checks.
     */
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
     * Write the config file out to ./data/mwconfig.txt.
     */
    public void saveConfig() {

        try {

            FileOutputStream fos = new FileOutputStream(CONFIG_BACKUP_FILE);
            PrintStream ps = new PrintStream(fos);
            config.store(ps, "client Config Backup");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            LOGGER.error("Failed backingup config file");
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(CONFIG_FILE);
            PrintStream ps = new PrintStream(fos);
            config.store(ps, "client Config");
            fos.close();
            ps.close();
        } catch (Exception ex) {
            LOGGER.error(ex, "");
            LOGGER.error("Failed saving config file");
        }
    }

    /**
     * Load and return a cached image (e.g. a repair/status icon) by logical name.
     *
     * @param repair the logical image name/key to resolve
     *
     * @return the loaded icon, or null/placeholder if it could not be found (implementation-dependent)
     */
    @Override
    public ImageIcon getImage(String repair) {
        return null;
    }

    /**
     * @return true if the client is configured to show small status icons (e.g. next to unit/user entries) rather than
     *       plain text.
     */
    @Override
    public boolean isUsingStatusIcons() {
        return false;
    }

    /**
     * Load and cache an image, optionally applying a camo pattern, for later retrieval via {@link #getImage(String)}.
     *
     * @param s    the base image name/path to load
     * @param camo the camo pattern identifier to apply, or null/empty for none
     * @param i    implementation-specific sizing/index parameter (e.g. target width)
     * @param i1   implementation-specific sizing/index parameter (e.g. target height)
     */
    @Override
    public void loadImage(String s, String camo, int i, int i1) {

    }

}
