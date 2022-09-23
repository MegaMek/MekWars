	/*
	 * MekWars - Copyright (C) 2008 
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

package server.campaign.operations;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.Vector;

import common.CampaignData;
import common.Unit;
import common.util.MWLogger;
import megamek.common.IEntityRemovalConditions;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;

public class OperationReporter {

	private OperationReportEntry opData = new OperationReportEntry();
	private Vector<String> winnerSet = new Vector<String>();
	private Vector<String> loserSet = new Vector<String>();
	
	private TreeMap<Integer, String> attackerUnits = new TreeMap<Integer, String>();
	private TreeMap<Integer, String> defenderUnits = new TreeMap<Integer, String>();
	private TreeMap<String, String> attackerMap = new TreeMap<String, String>();
	private TreeMap<String, String> defenderMap = new TreeMap<String, String>();
	
	private void setWinners(TreeMap<String, SPlayer> winners) {
		
		for(String winner : winners.keySet()) {
			winnerSet.add(winner);
		}
		Iterator<String> it = winnerSet.iterator();
		int count = 0;
		StringBuilder wNames = new StringBuilder();
		
		while(it.hasNext()) {
			if (count > 0)
				wNames.append(", ");
			String name = it.next();
			
			if (name.equalsIgnoreCase("Draw") ) {
			    wNames.append("Draw");
			} else {
				try {
					wNames.append(winners.get(name).getName());
				} catch (Exception e) {
					if (wNames==null) {
						MWLogger.testLog("wNames is null");
					} else if (winners == null) {
						MWLogger.testLog("winners is null");
					} else {
						MWLogger.testLog("Winners must have returned a null object");
						MWLogger.testLog("looking for: " + name);
						MWLogger.testLog("size: " + winners.size());
						MWLogger.testLog("conents: " + winners.keySet().toString());
					}
				}
			}
			count++;
		}
		opData.setWinnerName(wNames.toString());
	}
	
	private void setLosers(TreeMap<String, SPlayer> losers) {
		for (String loser : losers.keySet())
			loserSet.add(loser);
		Iterator<String> it = loserSet.iterator();
		int count = 0;
		StringBuilder lNames = new StringBuilder();
		
		while(it.hasNext()) {
			if (count > 0)
				lNames.append(", ");
			lNames.append(losers.get(it.next()).getName());
			count++;
		}
		opData.setLoserName(lNames.toString());
	}
	
	public void setWinnersAndLosers(TreeMap<String, SPlayer> winners, TreeMap<String, SPlayer> losers) {
		setWinners(winners);
		setLosers(losers);
	}
	
	public void setPlanetInfo(String pName, String tName, String thName) {
		opData.setPlanetInfo(pName, tName, thName);
	}
	
	public void setAttackerStartBV(int BV) {
		opData.setBV(true, true, BV);
	}
	
	public void setDefenderStartBV(int BV) {
		opData.setBV(false, true, BV);
	}
	
	public void setAttackerEndBV(int BV) {
		opData.setBV(true, false, BV);
	}
	
	public void setDefenderEndBV(int BV) {
		opData.setBV(false, false, BV);
	}
	
	public void commit() {
		opData.setEndTime(System.currentTimeMillis());
				
		MWLogger.resultsLog("Operation Finished: ");
		MWLogger.resultsLog("  OpType: " + opData.getOpType());
		MWLogger.resultsLog("  Planet: " + opData.getPlanet() + ", Terrain: " + opData.getTerrain() + ", Theme: " + opData.getTheme());
		MWLogger.resultsLog("  Attacker(s): " + opData.getAttackers() + " (" + opData.getAttackerSize() + " units)  --  Defender(s): " + opData.getDefenders() + " (" + opData.getDefenderSize() + " units)");
		MWLogger.resultsLog("  BVs: Attacker: " + opData.getAttackerStartBV() + " / " + opData.getAttackerEndBV() + "  --  Defender: " + opData.getDefenderStartBV() + " / " + opData.getDefenderEndBV());
		MWLogger.resultsLog("  Attacker Won: " + Boolean.toString(opData.attackerIsWinner()));
		MWLogger.resultsLog("  Winner(s): " + opData.getWinners() + "  --  Loser(s): " + opData.getLosers());
		MWLogger.resultsLog("  Game Length: " + opData.getHumanReadableGameLength());
	}
	
	public void closeOperation(boolean draw, boolean attackerWon) {
		opData.setDrawGame(draw);
		opData.setAttackerWon(attackerWon);
		
	}
	
	public void setUpOperation(String operationName, String planetName, String terrainName, String themeName) {
         setPlanetInfo(planetName, terrainName, themeName);
         opData.setOpType(operationName);
         opData.setStartTime(System.currentTimeMillis());
	}
	
	public void addAttacker(String playerName, int armyID) {
		attackerMap.put(playerName, CampaignMain.cm.getPlayer(playerName).getHouseFightingFor().getName());
		String playerString = playerName + " (" + attackerMap.get(playerName) + ")"; 
		SArmy army = CampaignMain.cm.getPlayer(playerName).getArmy(armyID);
		if(army != null) {
			opData.addStartingBV(true, army.getBV());
			addArmy(true, army);
			String s = opData.getAttackers();
			if(s.length() == 0)
				opData.setAttackerName(playerString);
			else
				opData.setAttackerName(s + ", " + playerString);
		}
	}
	
	public void addDefender(String playerName, int armyID) {
		defenderMap.put(playerName, CampaignMain.cm.getPlayer(playerName).getHouseFightingFor().getName());
		String playerString = playerName + " (" + defenderMap.get(playerName) + ")"; 
		SArmy army = CampaignMain.cm.getPlayer(playerName).getArmy(armyID);
		if(army != null) {
			opData.addStartingBV(false, army.getBV());
			addArmy(false, army);
			String s = opData.getDefenders();
			if(s.length() == 0)
				opData.setDefenderName(playerString);
			else
				opData.setDefenderName(s + ", " + playerString);
		}
	}
		
	public void addArmy(boolean attackerArmy, SArmy army) {
		// Keep a list of units for each side
		int numUnits = army.getAmountOfUnits();
		if(attackerArmy)
			opData.setAttackerSize(numUnits);
		else
			opData.setDefenderSize(numUnits);
		
		for(Unit currU : army.getUnits()) {
			int ID = currU.getId();
			String model = ((SUnit)currU).getModelName();
			if(attackerArmy)
				attackerUnits.put(ID, model);
			else
				defenderUnits.put(ID, model);
		}
	}
	
	public void addEndingUnit(SUnit unit, int removalReason) {
		if(removalReason == IEntityRemovalConditions.REMOVE_DEVASTATED || removalReason == IEntityRemovalConditions.REMOVE_PUSHED || removalReason == IEntityRemovalConditions.REMOVE_EJECTED || removalReason == IEntityRemovalConditions.REMOVE_NEVER_JOINED) {
			// Nothing to do here, as the unit doesn't count for BV
			return;
		}
		// The unit is still part of an army.  Add it's current BV to the ending BVs
		addEndingBV(unit.getId(), unit.calcBV());
	}

	private void addEndingBV(int unitID, int BV) {
		// First, figure if it's an attacking or defending unit
		if(attackerUnits.containsKey(unitID))
			opData.addEndingBV(true, BV);
		else if(defenderUnits.containsKey(unitID))
			opData.addEndingBV(false, BV);
	}
	
	public OperationReporter () {

	}
}
