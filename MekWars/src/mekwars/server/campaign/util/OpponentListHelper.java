/*
 * MekWars - Copyright (C) 2004 
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

package server.campaign.util;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import common.House;
import common.campaign.operations.Operation;
import common.util.MWLogger;
import common.util.StringUtils;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.operations.OperationManager;
import server.campaign.operations.newopmanager.I_OperationManager;

/**
 * 
 * @author urgru
 *
 * OpponentListHelper takes an army (or set of armies) and compares
 * it/them to all other active armies in order to generate an opponent
 * list. The "list" is stored as a vector of armies in the given force's
 * SArmyData. The Helper also updates matching opposition armies w/ 
 * pointers to the newly active force.
 * 
 * Each activate command creates a new Helper, looping all of the
 * activating players.
 */

public class OpponentListHelper {

	//VARIABLES
	private SPlayer searchPlayer;
	private TreeMap<String,ArrayList<SArmy>> potentialOpponents;

	private int currentMode = -1;

	public static final int MODE_ADD = 0;
	public static final int MODE_REMOVE = 1;

	//CONSTRUCTOR
	public OpponentListHelper(SPlayer p, int mode) {
		searchPlayer = p;
		potentialOpponents = new TreeMap<String,ArrayList<SArmy>>();
		currentMode = mode;

		if (mode == MODE_ADD)
			this.getOpponentsForAll();
		if (mode == MODE_REMOVE)
			this.removeOpponentsForAll();
		
	}//end all-armies constructor

	//METHODS
	/**
	 * Method which finds and updates potential opponents
	 * for all of a given player's armies.
	 */
	private void getOpponentsForAll() {

		/*
		 * Check all active players from all factions for potential opponents.
		 * Don't add one self, housemates (unless in house attacks are enabled),
		 * immune players, people at the same IP, or excluded players to creep
		 * into the OLH.
		 */
		SHouse searchHouse = searchPlayer.getHouseFightingFor();

		for (House h : CampaignMain.cm.getData().getAllHouses()) {

			SHouse currHouse = (SHouse)h;

			//check all active player and ONLY active players
			playersLoop: for (SPlayer currPlayer : currHouse.getActivePlayers().values()) {

				//don't allow people to play against themselves
				if (searchPlayer.equals(currPlayer))
					continue playersLoop;

				//if the player is immune, skip him
				if (CampaignMain.cm.getIThread().isImmune(currPlayer))
					continue playersLoop;

				//if player has no armies, skip him
				if (currPlayer.getArmies().size() == 0)
					continue playersLoop;

				//check exclusion status of the players.
				if (currPlayer.getExclusionList().checkExclude(searchPlayer.getName()) != ExclusionList.NO_EXCLUSION)
					continue playersLoop;
				if (searchPlayer.getExclusionList().checkExclude(currPlayer.getName()) != ExclusionList.NO_EXCLUSION)
					continue playersLoop;

				//check for same IP if account involvement is barred
				if (CampaignMain.cm.getBooleanConfig("IPCheck")) {
					try {
						String p1 = CampaignMain.cm.getServer().getIP(searchPlayer.getName()).toString();
						String p2 = CampaignMain.cm.getServer().getIP(currPlayer.getName()).toString();
						if (p1.equals(p2))
							continue playersLoop;
					} catch (Exception e) {
						MWLogger.errLog("Exception while checking players' IPs in OLH.");
						MWLogger.errLog(e);
					}
				}

				/*
				 * Player passes all bars. Check his armies against those
				 * of the activating player. If the armies match, add the
				 * enemy army to possDefendArmies and crossing the armies
				 * as opponents in each others' lists.
				 */
				ArrayList<SArmy> possDefendArmies = new ArrayList<SArmy>();
				I_OperationManager manager = CampaignMain.cm.getOpsManager(); 

				for (SArmy searchArmy : searchPlayer.getArmies()) {
					for (SArmy enemyArmy : currPlayer.getArmies()) {
						if (!searchArmy.isDisabled())  {
							attackLoop: for ( String attack : searchArmy.getLegalOperations().keySet() ){
								Operation o = manager.getOperation(attack);

								//continue to next operation if this is our faction and attacks aren't allowed
								if (currHouse.equals(searchHouse) && !o.getBooleanValue("AllowInFaction") )
									continue attackLoop;


								if (searchArmy.matches(enemyArmy,o) && !enemyArmy.isDisabled()) {
								
									//cross link
									searchArmy.addOpponent(enemyArmy);
									enemyArmy.addOpponent(searchArmy);
								
									//only add each army one time
									if (!possDefendArmies.contains(enemyArmy))
										possDefendArmies.add(enemyArmy);
									break attackLoop;
								}
							}
						}
					}
				}
				
				/*
				 * If there are no armies that can be attacked, continue to the
				 * next player without making a possDefeders entry or sorting.
				 */
				if (possDefendArmies.size() == 0)
					continue playersLoop;
				
				/*
				 * Construct a potentialOpponents entry for this player. The
				 * entry built here will be used to generate the info message.
				 */
				ArrayList<SArmy> toStore = new ArrayList<SArmy>();
				
				//do insertion sort on the armies, by ID. inefficient.
				possDefLoop: for (SArmy currArmy : possDefendArmies) {
				
					/*first insertion is automatic. otherwise NPE @ toStore.get(0).getID() below.
					if (toStore.size() == 0) {
						toStore.add(currArmy);
						continue possDefLoop;
					}*/
					
					for (int j = 0; j < toStore.size(); j++) {
						
						//if the army's ID is less than the currently stored ID @ an index, insert there
						if (currArmy.getID() < toStore.get(j).getID()) {
							toStore.add(j, currArmy);
							continue possDefLoop;
						}
					
					}
                    //if the end of the array is reached, just insert. has highest ID so far.
                    toStore.add(currArmy);
                    
					
				}//end for(all armies in set)
				
				potentialOpponents.put(currPlayer.getName(), toStore);
				
			}//end for(all active players in house)
		}//end for(all houses)
		
	}//end getOpponentsForAll()

	/**
	 * Method which removes all opponentlists for a player, and
	 * removes his armies from other player's opplists.
	 */
	private void removeOpponentsForAll() {

		/*
		 * Player is being moved to inactive or fighting status. This means
		 * he is no longer an eligible attack target. Need to remove his oplists
		 * and clear his entries on other players oplists.
		 */
		for (SArmy currArmy : searchPlayer.getArmies()) {

			//remove curr army from all opparmies which link it.
			Enumeration<SArmy> j = currArmy.getOpponents().elements();
			while (j.hasMoreElements()) {
				SArmy oppArmy = j.nextElement();
				oppArmy.removeOpponent(currArmy);

				//add to the 
				String currName = oppArmy.getPlayerName().toLowerCase();
				if (potentialOpponents.get(currName) == null)
					potentialOpponents.put(currName, new ArrayList<SArmy>());
			}

			//reset currArmy's OpponentList
			currArmy.setOpponents(new Vector<SArmy>(1,1));

		}//end while(more armies to unlink)
	}//end removeOpponentsForAll()


	/**
	 * Method which sends "New Opponent"-style messages to players who
	 * are already active, based on contents of possibleOpponents hash.
	 * 
	 * Note that this is a public method, whereas nearly all other Helper
	 * methods are private. Send info is called after the Helper is
	 * constrcuted.
	 */
	public void sendInfoToOpponents(String s) {

		for (String currOppName : potentialOpponents.keySet()) {

			StringBuilder output = new StringBuilder("ED:[!] ");
			SPlayer currOpp = CampaignMain.cm.getPlayer(currOppName);

			if (currOpp == null)
				continue;

			//if opponent doesn't meet min active time, don't send.
			long minActiveTime = Long.parseLong(CampaignMain.cm.getConfig("MinActiveTime")) * 1000;
			if (System.currentTimeMillis() - currOpp.getActiveSince() <= minActiveTime)
				continue;

			//get the colored faction name
			SHouse searchHouse = searchPlayer.getHouseFightingFor();
			String colHouseName = searchHouse.getColoredNameAsLink();
			output.append(StringUtils.aOrAn(searchHouse.getName(),false,false) + " " + colHouseName + " unit " + s);

			/*
			 * If its an add, show exactly which armies can attack. A remove
			 * just shows the faction which is leaving and won't need any more
			 * text.
			 */
			if (currentMode == MODE_ADD) {
				ArrayList<SArmy> currOppArmies = potentialOpponents.get(currOppName);	
				if (currOppArmies.size() > 1) {
				
					output.append("Armies ");
					Iterator<SArmy> i = currOppArmies.iterator();
					while (i.hasNext()) {
						output.append(i.next().getID());
						if (i.hasNext()) {output.append(", ");}
					}
					//try to remove the last instance of ", "
					int lastComma = output.lastIndexOf(", ");
					if (lastComma >= 0) {
						String front = output.substring(0, lastComma);
						String back = output.substring(lastComma + 2, output.length());
						output = new StringBuilder(front + " and " + back);
					}
					
				} else { // we can assume size == 1
					SArmy currArmy = currOppArmies.get(0);
					output.append("Army " + currArmy.getID());
				}
			}

			/*
			 * Also give the user a link which he can click in order
			 * to issue a checkattack command and see the matchups.
			 */
			output.append(". [<a href=\"MEKWARS/c checkattack\">Report</a>]");
			CampaignMain.cm.toUser(output.toString(),currOppName,true);

		}//end while(more opponents to inform)
	}//end sendInfoToOpponents()

}//end OpponentListHelper class