/*
 * MekWars - Copyright (C) 2006
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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

package server.campaign;

import java.util.ArrayList;
import java.util.TreeMap;

import common.util.MWLogger;
import server.campaign.util.OpponentListHelper;

/**
 * @author urgru
 * 
 * Private thread. Simple loop that keeps track of player's immunity. All
 * methods that modify the immunePlayers TreeMap lock the map in order to
 * prevent concurrent modification errors.
 */
public final class ImmunityThread extends Thread {//no extension
	
	//VARIABLES
	private TreeMap<String,Long> immunePlayers;
	
	//CONSTRUCTOR
	public ImmunityThread() {
	    super("Immunity Thread");
		this.immunePlayers = new TreeMap<String,Long>();
	}
	
	//METHODS
	/**
	 * Method that waits the Immunity thread and
	 * prints any interuptions to the error log.
	 */
	public void extendedWait(int time) {
		try {
			this.wait(time);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
	}
	
	/**
	 * Method that adds a newly immune player to the Thread. The
	 * player is informed of his immunity. If he's a newbie, he is
	 * also told if/how to reset his units.
	 * 
	 * @param p - player who is now immune.
	 */
	public void addImmunePlayer(SPlayer p) {
		
		//get name and immunity duration
		String lowername = p.getName().toLowerCase();
		int immunitySeconds = CampaignMain.cm.getIntegerConfig("ImmunityTime");
		
		//if immunity is disabled, just ignore and return.
		if (immunitySeconds < 1)
			return;
		
		//inform player. also, if newbie, tell him about potential unit resets
		CampaignMain.cm.toUser("You are immune to attack for " + immunitySeconds + " seconds. [<a href=\"MEKWARS/c deactivate\">Deactivate</a>]",p.getName(),true);
		if (p.getMyHouse().isNewbieHouse()) {
			int numResets = CampaignMain.cm.getIntegerConfig("NumResetsWhileImmune");
			if (numResets > 0) {
				
				//set the resets
				NewbieHouse pHouse = (NewbieHouse)p.getMyHouse();
				pHouse.addResetPlayer(p,numResets);
				
				//and inform the lucky player
				String toSend = "You may reset your units ";
				if (numResets == 1) {toSend += " once";}
				else {toSend += numResets + " times";}
				CampaignMain.cm.toUser(toSend += " while immune by selecting \"Reset Units\" in the HQ. You may only reset while in reserve.",p.getName(), true);
				
			}
		}
		
		//put name and end-time (in millis) in immune players hash. SYNCHED!
		synchronized (immunePlayers) {
			this.immunePlayers.put(lowername,System.currentTimeMillis() + (immunitySeconds * 1000));
		}
		
	}
	
	/**
	 * Check to see if a player is immune.
	 */
	public boolean isImmune(SPlayer p) {
		return immunePlayers.containsKey(p.getName().toLowerCase());
	}
	
	/**
	 * Remove a player from the Immunity list. This is called when logging in or
	 * activating, but shoud NEVER be called when deactivating. If de-activated
	 * players were removed using this call, SOL would not be able to reset units.
	 */
	public void removeImmunity(SPlayer p) {
		synchronized (immunePlayers) {
			immunePlayers.remove(p.getName().toLowerCase());
		} if (p.getMyHouse().isNewbieHouse()) {
			NewbieHouse pHouse = (NewbieHouse)p.getMyHouse();
			pHouse.removeResetPlayer(p);
		}
	}
	
	/**
	 * Override Thread's run() method to do what we want - periodically
	 * wake and poll the immune players hash, bumping anyone whose time
	 * has expired back to standard status. Runs forever.
	 */
	@Override
	public synchronized void run() {
		
		while(true) {
			
			//polle very 5 seconds
			this.extendedWait(5000);
			
			synchronized (immunePlayers) {
				
				//keep track of players to be removed
				ArrayList<String> toRemove = new ArrayList<String>();
				
				//loop through all players
				for (String currName : immunePlayers.keySet()) {
					
					//if the player's immunity has not expired, continue
					Long startTime = System.currentTimeMillis();
					if (startTime < immunePlayers.get(currName))
						continue;
					
					//immunity expired. add to removal list
					toRemove.add(currName);
					
				}
				
				//remove and inform expired players
				for (String currName : toRemove) {
					
					//load the player
					SPlayer p = CampaignMain.cm.getPlayer(currName);
					
					if ( p == null ){
						synchronized (immunePlayers) {
							immunePlayers.remove(currName);
						}
						continue;
					}
					
					/*
					 * make sure the player is still active (hasnt logged out, deactivated (voluntary
					 * of otherwise) and subsequently re-activated, attacked or joined a game.
					 */
					if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
						
						//tell the player
						CampaignMain.cm .toUser("[!] Your post-game immunity expired!",p.getName(), true);
						
						//alert other players
						OpponentListHelper olh = new OpponentListHelper(p,OpponentListHelper.MODE_ADD);
						olh.sendInfoToOpponents(" finished an R&R cycle and returned to the front. You may attack it with ");
						
						//newbie player handling. if he has resets left, tell him time has expired. always remove from the list.
						if (p.getMyHouse().isNewbieHouse()) {
							
							NewbieHouse currH = (NewbieHouse)p.getMyHouse();
							if (currH.getResetsRemaining(p) > 0)
								CampaignMain.cm .toUser("[!] Your post-game reset time expired!",p.getName(), true);
							
							currH.removeResetPlayer(p);
						}
					}
					
					synchronized (immunePlayers) {
						immunePlayers.remove(currName);
					}
				}
				
			}//end synch lock on immunePlayers
			
		}
	}// end run()
	
}// end ImmunityThread