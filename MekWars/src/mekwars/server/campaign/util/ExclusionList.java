/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author - nmorris (urgru@users.sourceforge.net)
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

package server.campaign.util;

import java.io.File;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import server.campaign.CampaignMain;

/**
 * @author urgru
 * 
 * Simple class which contains two vectors. The first is a list of players that
 * an SPlayer is unwilling to play against (the "no-play" list). The second is
 * an *admin controlled* list with the same effect. This is used to bar players
 * from playing one another. It can be of unlimited length, and may or may not
 * count against a player's own exclusion cap.
 * 
 * Included in the utilities package because this is really just a glorified
 * vector bag which can to/from string itself. Not worthy of server.campaign.* =)
 */

public class ExclusionList{
	
	//final vars
	public static final int NO_EXCLUSION = 0;
	public static final int PLAYER_EXCLUDED = 1;
	public static final int ADMIN_EXCLUDED = 2;
	
	//ivars
	private int maxSize = 0;
	private boolean adminListCountsForCap = false;
	private String owner;
	private Vector<String> playerExcludes = null;
	private Vector<String> adminExcludes = null;
	
	public ExclusionList() {
		
		//get a proper maxsize and the admin list impact
		maxSize = CampaignMain.cm.getIntegerConfig("NoPlayListSize");
		adminListCountsForCap = CampaignMain.cm.getBooleanConfig("NoPlaysFromAdminsCountForMax");
		
		//set blank owner name [used to send messages to player]
		owner = "";
		
		//build the vectors
		playerExcludes = new Vector<String>(maxSize,1);
		adminExcludes = new Vector<String>(maxSize,1);
	}
	
	/**
	 * Method which sets an owner name. The name is
	 * used to send messaged
	 * 
	 */
	public void setOwnerName(String ownerName) {
		owner = ownerName;
	}
	
	/**
	 * Method which adds a player to exclude lists. Simple. Does
	 * NOT check size, etc. This should be done externally.
	 * 
	 * @param fromAdmin - boolean, indicating origin of add		
	 * @param name - String, name of player to exclude
	 */
	public void addExclude(boolean fromAdmin, String name) {
		if (fromAdmin)
			adminExcludes.add(name.toLowerCase());
		else
			playerExcludes.add(name.toLowerCase());
	}
	
	/**
	 * Method which checks to see if a player is already
	 * included on an Exclusion list. Outside classes which
	 * modify exclusions lists should check to ensure that
	 * duplications in Player and Admin lists are properly
	 * handled.
	 * 
	 * @param name - String, name to check
	 * @return - int indicating which list, if any, contains name
	 */
	public int checkExclude(String name) {
		if (playerExcludes.contains(name.toLowerCase()))
			return PLAYER_EXCLUDED;
		else if (adminExcludes.contains(name.toLowerCase()))
			return ADMIN_EXCLUDED;
		else
			return NO_EXCLUSION;
	}
	
	/**
	 * Method which removes a player from an exclude list. Note
	 * that an admin can remove from EITHER list, while a player
	 * can remove only from his own. Again, this is something
	 * that has to be enforced externally (relies on truthful
	 * passing of the fromAdmin boolean).
	 * 
	 * @param name - String, name to remove
	 * @return 
	 */
	public void removeExclude(boolean fromAdmin, String name) {
		if (fromAdmin) {
			playerExcludes.remove(name.toLowerCase());
			adminExcludes.remove(name.toLowerCase());
		} else
			playerExcludes.remove(name.toLowerCase());
	}
	
	/**
	 * Method which checks the excludes.
	 * 
	 * Both lists are checked for player who have left
	 * the campaign (deleted or unenrolled).
	 * 
	 * The player list is checked to ensure that it is
	 * not over maxSize(). If it is, names are pruned,
	 * back to front, until the list equals maxsize or
	 * has size() == 0.
	 */
	private void validateExcludes() {
		
		//first, look for missing players on the admin list.
		Iterator<String> e = adminExcludes.iterator();
		while (e.hasNext()) {
			String currName = e.next();
            boolean playerExists = false;
            playerExists = new File("./campaign/players/" + currName.toLowerCase() + ".dat").exists();
            
            if (!playerExists) {
				e.remove();
				CampaignMain.cm.toUser(currName + " has left the campaign. No-Play list updated.",owner,true);
			}
		}
		
		//next, look for missing players on the player list.
		e = playerExcludes.iterator();
		while (e.hasNext()) {
			String currName = e.next();
	         boolean playerExists = false;
             playerExists = new File("./campaign/players/" + currName.toLowerCase() + ".dat").exists();
	            
             if (!playerExists) {
				e.remove();
				CampaignMain.cm.toUser(currName + " has left the campaign. No-Play list updated.",owner,true);
			}
		}
		
		//finally, look for overflows in the player list.
		if (playerExcludes.size() > maxSize) {
			
			int excludeSize = 0;
			if (adminListCountsForCap) {
				excludeSize = playerExcludes.size() + adminExcludes.size();
			} else {
				excludeSize = playerExcludes.size();
			}
			
			while (excludeSize > maxSize && playerExcludes.size() > 0) {
				String currName = playerExcludes.get(playerExcludes.size() - 1);
				playerExcludes.remove(currName);
				CampaignMain.cm.toUser("Your No-Play list was too long. " + currName + " was removed.",owner,true);
				excludeSize = excludeSize - 1;
			}
		}
	}//end validateExcludes()
	
	/*
	 * simple sizechecks for the excludes. no external
	 * handling of the vectors.
	 */
	public Vector<String> getPlayerExcludes() {
		return playerExcludes;
	}
	
	public Vector<String> getAdminExcludes() {
		return adminExcludes;
	}
	
	/*
	 * The meat of things. To/From strong methods for both
	 * the player and admin Exclude sheets. These are run
	 * when a player is saved/loaded from disk.
	 */
	public void adminExcludeFromString(String buffer, String delimiter){
		
		//MWLogger.mainLog("** adminExcludeFROMStringCalled");
		
		StringTokenizer ST = new StringTokenizer(buffer,delimiter);
		while (ST.hasMoreElements()) {
			String curr = ST.nextToken();
			if (curr.equals("0"))
				return;
			//else
			this.addExclude(true,curr);
		}
	}
	
	public void playerExcludeFromString(String buffer, String delimiter) {
		
		//MWLogger.mainLog("** playerExcludeFROMStringCalled");
		
		StringTokenizer ST = new StringTokenizer(buffer,delimiter);
		while (ST.hasMoreElements()) {
			String curr = ST.nextToken();
			if (curr.equals("0"))
				return;
			//else
			this.addExclude(false,curr);
		}
		
		/*
		 * Player excludes are loaded after admin excludes, so we
		 * can assume that loading/filling of the ExclusionList is
		 * now complete. Call the validator in order to ensure that
		 * all the excludes are valid, and the lists aren't overful.
		 */
		this.validateExcludes();
	}
	
	public String adminExcludeToString(String token) {
		
		//MWLogger.mainLog("** adminExcludeToStringCalled");
		
		StringBuilder result = new StringBuilder();
		
		if (adminExcludes.size() == 0) {
			result.append("0");
			result.append(token);
		} else {
			for (String currName : adminExcludes) {
				result.append(currName);
				result.append(token);
			}
		}
		
		return result.toString();
	}
	
	public String playerExcludeToString(String token) {
		
		//MWLogger.mainLog("** playerExcludeToStringCalled");
		
		StringBuilder result = new StringBuilder();
		
		if (playerExcludes.size() == 0) {
			result.append("0");
			result.append(token);
		} else {
			for (String currName : playerExcludes) {
				result.append(currName);
				result.append(token);
			}
		}
		
		return result.toString();
	}
	
}//end ExcludeList