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

public class OperationReportEntry {

	private String attackerName = "";
	private String defenderName = "";
	private String winnerName = "";
	private String loserName = "";
	private String planetName = "";
	private String terrainName = "";
	private String themeName = "";
	
	private int attackerStartBV = 0;
	private int attackerEndBV = 0;
	private int defenderStartBV = 0;
	private int defenderEndBV = 0;
	
	private int attackerSize = 0;
	private int defenderSize = 0;
	
	private boolean attackerWon = false;
	private boolean drawGame = false;
	
	private long gameLength = 0;
	private long startTime = 0;
	private long endTime = 0;
	
	private String opType = "";
	
	
	public boolean gameIsDraw() {
		return drawGame;
	}
	
	public void setDrawGame(boolean draw) {
		drawGame = draw;
	}
	
	public int getAttackerSize() {
		return attackerSize;
	}
	
	public void setAttackerSize(int size) {
		attackerSize = size;
	}
	
	public int getDefenderSize() {
		return defenderSize;
	}
	
	public void setDefenderSize(int size) {
		defenderSize = size;
	}
	
	public String getOpType() {
		return opType;
	}
	
	public void setOpType(String op) {
		opType = op;
	}
	
	public int getAttackerStartBV() {
		return attackerStartBV;
	}
	
	public int getAttackerEndBV() {
		return attackerEndBV;
	}
	
	public int getDefenderStartBV() {
		return defenderStartBV;
	}
	
	public int getDefenderEndBV() {
		return defenderEndBV;
	}
	
	public boolean attackerIsWinner() {
		return attackerWon;
	}
	
	public long getGameLength() {
		return gameLength;
	}
	
	public String getAttackers() {
		return attackerName;
	}
	
	public String getDefenders() {
		return defenderName;
	}
	
	public String getWinners() {
		return winnerName;
	}
	
	public String getLosers() {
		return loserName;
	}
	
	public String getPlanet() {
		return planetName;
	}
	
	public String getTheme() {
		return themeName;
	}
	
	public String getTerrain() {
		return terrainName;
	}
	
	public void setAttackerName(String name) {
		attackerName = name;
	}
	
	public void setDefenderName(String name) {
		defenderName = name;
	}
	
	public void setWinnerName(String name) {
		winnerName = name;
	}
	
	public void setLoserName(String name) {
		loserName = name;
	}
	
	public void setBV (boolean attacker, boolean start, int BV) {
		if (attacker) {
			if(start)
				attackerStartBV = BV;
			else
				attackerEndBV = BV;	
		} else {
			if(start)
				defenderStartBV = BV;
			else
				defenderEndBV = BV;
		}
	}
	
	public void addEndingBV(boolean attacker, int BV) {
		if(attacker)
			attackerEndBV += BV;
		else
			defenderEndBV += BV;
	}
	
	public void addStartingBV(boolean attacker, int BV) {
		if(attacker)
			attackerStartBV += BV;
		else
			defenderStartBV += BV;
	}
	
	public void setPlanetInfo (String pName, String tName, String thName) {
		planetName = pName;
		terrainName = tName;
		themeName = thName;
	}
	
	public void setAttackerWon(boolean aWon) {
		attackerWon = aWon;
	}
	
	public void setStartTime(long time) {
		startTime = time;
	}
	
	public void setEndTime(long time) {
		endTime = time;
		gameLength = endTime - startTime;
	}
	
	public String getHumanReadableGameLength() {
		int seconds = (int)(gameLength / 1000);
		int hours = seconds / (60 * 60);
		seconds -= (hours * 60 * 60);
		int minutes = seconds / 60;
		seconds -= (minutes * 60);
		StringBuilder timeString = new StringBuilder();
		if(hours > 0)
			timeString.append(hours + "h");
		timeString.append(minutes + "m");
		timeString.append(seconds + "s");
		return timeString.toString();
	}
	
	public OperationReportEntry() {
		
	}
}
