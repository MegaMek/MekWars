/*
 * MekWars - Copyright (C) 2014 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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
 *
 */
package server.campaign.commands;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.Vector;

import common.campaign.operations.Operation;
import common.util.MWLogger;
import server.campaign.CampaignMain;

public class GetOpsCommand implements Command {

	int accessLevel = 2;
	public int getExecutionLevel() {
		return accessLevel;
	}
	
	public void setExecutionLevel(int i) {
		accessLevel = i;
	}
	
	String syntax = "/getops [getall, md5, getsome#list]";
	public String getSyntax() { return syntax;}
	@Override
	public void process(StringTokenizer command, String Username) {
		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}
		
		if (command.hasMoreTokens()) {
			String cmd = command.nextToken();
			switch (cmd.toLowerCase()) {
			case "getall":
				for (Operation o : CampaignMain.cm.getOpsManager().getOperations().values()) {
					CampaignMain.cm.toUser("OP|add|" + o.getName() + "|" + o.getXmlString(), Username, false);
				}
				CampaignMain.cm.toUser("OP|view", Username, false);
				break;
				
			case "getsome":
				Vector<Operation>opsToSend = new Vector<Operation>();
				while(command.hasMoreTokens()) {
					opsToSend.add(CampaignMain.cm.getOpsManager().getOperation(command.nextToken()));
				}
				for (Operation o : opsToSend) {
					CampaignMain.cm.toUser("OP|add|" + o.getName() + "|" + o.getXmlString(), Username, false);
				}
				CampaignMain.cm.toUser("OP|view", Username, false);
				break;
				
			case "md5":
				File md5File = new File("./data/operations/opsmd5.txt");
				if (!md5File.exists()) {
					// Calculate MD5s and write the file
					FileWriter fw = null;
					try {
						fw = new FileWriter(md5File);
						for (Operation o : CampaignMain.cm.getOpsManager().getOperations().values()) {
							MessageDigest md = null;
							try {
								md = MessageDigest.getInstance("MD5");
							} catch (NoSuchAlgorithmException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							byte[] array = md.digest(o.getXmlString().getBytes("UTF-8"));
							StringBuffer sb = new StringBuffer();
							for (int i = 0; i < array.length; ++i) {
								sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
							}
							fw.write(o.getName() + "#" + sb.toString() + "\n");
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							fw.close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				// Now, send it to them
				StringBuffer toReturn = new StringBuffer();
				try {
	                FileInputStream in = new FileInputStream(md5File);
	                BufferedReader br = new BufferedReader(new InputStreamReader(in));
	                try {
	                    while (br.ready()) {
	                        toReturn.append(br.readLine() + "#");
	                    }
	                    br.close();
	                    in.close();
	                } catch (IOException ex) {

	                }
	            } catch (FileNotFoundException e) {
	                // TODO Auto-generated catch block
	                MWLogger.errLog(e);
	            }
				CampaignMain.cm.toUser("OP|md5|" + toReturn.toString(), Username, false);
				
				break;
				
			default:
				CampaignMain.cm.toUser("AM: invalid syntax, use: " + getSyntax(), Username, true);
				break;
			}
		} else {
			CampaignMain.cm.toUser("AM: invalid syntax, use: " + getSyntax(), Username, true);
		}
	}

}
