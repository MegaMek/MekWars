	/*
	 * MekWars - Copyright (C) 2007 
	 * 
	 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.StringTokenizer;

import client.MWClient;
import client.gui.CCommPanel;
import common.util.MWLogger;

/**
 * @author Spork
 * 
 * Handles Build Table up/downloading for admins/mods
 * 
 */
public class BT extends Command {

	/**
	 * @param client
	 */
	public BT(MWClient mwclient) {
		super(mwclient);
	}

	/**
	 * @see client.cmd.Command#execute(java.lang.String)
	 */
	@Override
	public void execute(String input) {
		StringTokenizer st = decode(input);
		
		String cmd = st.nextToken();
		boolean viewer = false;
		if(cmd.equalsIgnoreCase("LS")) {
			StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");
			if ( st.hasMoreTokens() ) {
			    viewer = Boolean.parseBoolean(st.nextToken());
			}
			while(folderT.hasMoreTokens()) {
				//Token 1 is the folder
				String dName = folderT.nextToken();
				MWLogger.infoLog(dName);
				// Token 2 is the names of the lists
				if(folderT.hasMoreTokens()) {
					StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
					while (listT.hasMoreTokens()) {
						String fileName = listT.nextToken();
						long time = 0;
						File file = new File("./data/buildtables/" + dName + "/" + fileName);
						
						if ( file.exists() ) {
						    time = file.lastModified();
						}
						mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "AdminRequestBuildTable get#" + dName + "#" + fileName + "#" + time);
					}
				}
					
			}
            if ( viewer ) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "AdminRequestBuildTable view");
            }

		} if(cmd.equalsIgnoreCase("PLS")) {
            StringTokenizer folderT = new StringTokenizer(st.nextToken(), "?");
            if ( st.hasMoreTokens() ) {
                viewer = Boolean.parseBoolean(st.nextToken());
            }
            while(folderT.hasMoreTokens()) {
                //Token 1 is the folder
                String dName = folderT.nextToken();
                MWLogger.infoLog(dName);
                // Token 2 is the names of the lists
                if(folderT.hasMoreTokens()) {
                    StringTokenizer listT = new StringTokenizer(folderT.nextToken(), "*");
                    while (listT.hasMoreTokens()) {
                        String fileName = listT.nextToken();
                        long time = 0;
                        File file = new File("./data/buildtables/" + dName + "/" + fileName);
                        
                        if ( file.exists() ) {
                            time = file.lastModified();
                        }
                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "RequestBuildTable get#" + dName + "#" + fileName + "#" + time);
                    }
                }
                    
            }
            if ( viewer ) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "RequestBuildTable view");
            }
        } else if (cmd.equalsIgnoreCase("BT")) {
			String folder = st.nextToken();
			String table = st.nextToken();
			boolean isMod = mwclient.isMod();
			File file = new File("./data/buildtables");
			if(!file.exists())
				file.mkdir();
			file = new File("./data/buildtables/" + folder);
			if(!file.exists())
				file.mkdir();
			file = new File("./data/buildtables/" + folder + "/" + table);
			try {
				file.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			FileOutputStream out;
			try {
				out = new FileOutputStream(file);
				PrintStream p = new PrintStream(out);
				while(st.hasMoreTokens())
					p.println(st.nextToken());
				if ( isMod ){
				    mwclient.addToChat("Received build table " + folder + "/" + table,CCommPanel.CHANNEL_MISC);
				}
				p.close();
				try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					MWLogger.errLog(e);
				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				MWLogger.errLog(e);
			}
		} else if ( cmd.equalsIgnoreCase("VS") ) {
		    mwclient.setWaiting(false);
		}
	}
}
