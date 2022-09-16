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

package dedicatedhost;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;

import javax.swing.JOptionPane;

import common.campaign.clientutils.IClientConfig;
import common.util.MWLogger;

/**
 * Class for Client's configuration.
 */
public class DedConfig implements IClientConfig {
	
	public static final String CONFIG_FILE = "./data/mwconfig.txt";
    public static final String CONFIG_BACKUP_FILE = "./data/mwconfig.txt.bak";
	
	private Properties config;                //config. player values.
	
	//CONSTRUCTOR
	public DedConfig(boolean dedicated) {
		
		config = setDefaults();		

		//check to see if a config is present. if not, make one.
        if ( !(new File(CONFIG_FILE).exists()) && !(new File(CONFIG_BACKUP_FILE).exists()) )
			createConfig();
		
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
                MWLogger.errLog(ex);
                JOptionPane.showMessageDialog(null, "Unable to load Backup config file");
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
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
		}catch ( Exception ex){
            MWLogger.errLog(ex);
        }
		
		setParam("DEDICATED","TRUE");
		
	}
	
	//METHODS
	/**
	 * Private method that loads hardcoded defaults. These are loaded
	 * before the players config values, adding any new configs in their
	 * default position and ensuring that no config value is even missing.
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
		defaults.setProperty("SERVERPORT","2347");
		defaults.setProperty("DATAPORT","4867");
		defaults.setProperty("PORT","2346");
		defaults.setProperty("MAXPLAYERS","12");
		defaults.setProperty("MAXSAVEDGAMEDAYS","7");
        defaults.setProperty("UPDATEKEY","-1");
        defaults.setProperty("DEDMEMORY","256");

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
			
			/*
			 * Options below are supported in code, but not yet in config dialog.
			 */
			//ps.println("#If you want a Password for your game, enter it here");
			//ps.println("#GAMEPASSWORD: ");
			//ps.println("#Size of chat font (as in html font tag)");
			//ps.println("#CHATFONTSIZE: +0");
			//ps.println("#Color of chat font (as in html font tag)");
			//ps.println("#CHATFONTCOLOR: black");
			//ps.println("#If you don't want the Player Panel to be visible, set it to NO");
			//ps.println("PLAYERPANEL: YES");
			//ps.println("#Put player panel height in pixels (excluding logo height) here (default is 130)");
			//ps.println("#PLAYERPANELHEIGHT: 130");
			//ps.println("#If you don't want the Logo to be visible, set it to NO");
			//ps.println("LOGO: NO");
			//ps.println("#A picture (from /data/images) to be shown as your logo in client. If you comment it out, it will download your unit logo!");
			//ps.println("LOGOIMAGE: logo.jpg");
			//ps.println("#The thickness of splitters dividing client windows");
			//ps.println("#SPLITTERSIZE: 7");
			//ps.println("#set to NO if you do not want server messages to appear as popup.");
			//ps.println("#if turned off, messages will appear in main chat.");
			//ps.println("POPUPONMESSAGE: NO");

			/*
			 * Options below are supported in config dialog.
			 */
			//ps.println("#Your Color used for your name in the chat ");
			//ps.println("#Color of your name in chat.");
			//ps.println("#Choices: standard HTML colours, any hex colour with a Red Value under AA)");
			//ps.println("COLOR: black");
			//ps.println("#IP of MekWars Server you are connecting to");
			//ps.println("#Servers listed on forums @ http://www.sourceforge.net/projects/mekwars");
			//ps.println("SERVERIP: SEE THE MEKWARS PROJECT PAGE FOR A LIST OF KNOWN SERVERS");
			//ps.println("#MegaMek host settings");
			//ps.println("#IF and only IF your ip isn't detected correctly, you may edit this setting (very unlikely that this happens)");
			//ps.println("#This is your current IP, needed to host games. You can use a Dynamically assigned DNS entry or just your plain IP here.");
			//ps.println("#If you don't know your IP-Address try this website: http://www.whatismyip.com");
			//ps.println("#You only need to enable that line if your IP isn't shown correctly when you create a game.");
			//ps.println("#IP: 127.0.0.1");
			//ps.println("#The maximum number of players you want to join your host. (If you host a game) Default: 12");
			//ps.println("MAXPLAYERS: 12");
			//ps.println("#A comment for your game (If you host a game)");
			//ps.println("COMMENT: ");
			//ps.println("#SOUND SETTINGS");
			//ps.println("#Play this file if anyone calls my name");
			//ps.println("SOUNDONCALL: ./data/sounds/call.wav");
			//ps.println("#Play this file when a Player joins the room");
			//ps.println("#SOUNDONJOIN: ./data/sounds/join.wav");
			//ps.println("#Play this file when a Player exits the room");
			//ps.println("#SOUNDONEXIT: ./data/sounds/exit.wav");
			//ps.println("#Play this file when someone sends you a message");
			//ps.println("SOUNDONMESSAGE: ./data/sounds/mail.wav");
			//ps.println("#Play this file when someone attacks you");
			//ps.println("SOUNDONATTACK: ./data/sounds/attack.wav");
			//ps.println("#Dedicated server settings");
			//ps.println("#Should this be a Dedicated Server ONLY?");
			//ps.println("DEDICATED: NO");
			//ps.println("#(Only if Dedicated Only) Put names of people allowed to reset him here, separated with commas");
			//ps.println("DEDICATEDOWNERNAME: ");
			//ps.println("#If you don't want to see news and statuses in Main Channel, set it to NO");
			//ps.println("MAINCHANNELNEWS: YES");
			//ps.println("#If you don't want to see faction mails in Main Channel, set it to NO");
			//ps.println("MAINCHANNELHM: NO");
			//ps.println("#If you don't want to see private mails in Main Channel, set it to NO");
			//ps.println("MAINCHANNELPM: NO");
			//ps.println("#If you don't want to see system messages in Main Channel, set it to NO");
			//ps.println("MAINCHANNELSM: NO");
			//ps.println("#If you don't want to see misc messages in Main Channel, set it to NO");
			//ps.println("MAINCHANNELMISC: NO");
			//ps.println("#If you want to hear a sound when specific word is received, put them in here, separated with commas");
			//ps.println("#REPLYTOSENDER: YES");
			//ps.println("#If you don't want PM tab reply to last mail receiver, set it to NO");
			//ps.println("#REPLYTORECEIVER: NO");
			//ps.println("#If you don't want dialog to popup on when you are attacked, set it to NO");
			//ps.println("#POPUPONATTACK: YES");
			//ps.println("#PanelDivider set between 1-100");
			//ps.println("PANELDIVIDER: 40");
			//ps.println("#bind commands to Function keys. /c is automatically added.");
			//ps.println("#Example binding for F1 to mysatus and transfermomey follow.");
			//ps.println("#F1BIND: transfermoney#urgru#30");
			//ps.println("F1BIND: mystatus");
			//ps.println("F2BIND:");
			//ps.println("F3BIND:");
			//ps.println("F4BIND:");
			//ps.println("F5BIND:");
			//ps.println("#number of games a ded will play before it restarts");
			//ps.println("DEDAUTORESTART: 20");
					
			//these should be pre-empted by the serverdata.dat values set by server op
			//ps.println("CAMPAIGNSERVERNAME: MekWars Server");
			//ps.println("TRAYIMAGE: reserve_colored.gif");
			//ps.println("UPDATEKEY: -1");
            
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
		if (tparam.equalsIgnoreCase("YES") || tparam.equalsIgnoreCase("TRUE") || tparam.equalsIgnoreCase("ON"))
			return true;
		return false;
	}
	
	/**
	 * Return the int value of a given config property. Return
	 * a 0 if the property is a non-number. Used mostly by the
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
            config.store(ps,"Client Config Backup");
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
			config.store(ps,"Client Config");
			fos.close();
			ps.close();
		} catch (Exception ex) {
			MWLogger.errLog(ex);
			MWLogger.errLog("Failed saving config file");
		}
	}
	
}
