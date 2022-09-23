/*
 * MekWars - Copyright (C) 2004 
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
 */

package server.campaign.util;

import java.util.StringTokenizer;

import server.campaign.SUnit;

/**
 *
 * @author McWizard
 *
 * Represents a MechStats Entry
 */
public class MechStatistics implements Cloneable, Comparable<Object> {
  private Long ID;
  private String mechFileName;
  private int mechSize;
  private int gamesWon =0;
  private int gamesPlayed = 0;
  private int timesScrapped = 0;
  private long lastTimeUpdated = 0;
  private int OriginalBV = 0;
  private int currentGamesWon = 0;
  private int currentGamesPlayed = 0;
  private int timesDestroyed = 0;
  private int DBID = 0;
  
  public String getMechFileName() {
    return mechFileName;
  }
  public void setMechFileName(String mechFileName) {
    this.mechFileName = mechFileName;
  }

  @Override
public String toString()
  {
    String result = "";
    result += this.mechFileName;
    result += "*";
    result += this.mechSize;
    result += "*";
    result += this.gamesWon;
    result += "*";
    result += this.gamesPlayed;
    result += "*";
    result += this.timesScrapped;
    result += "*";
    result += this.lastTimeUpdated;
    result += "*";
    result += this.currentGamesWon;
    result += "*";
    result += this.currentGamesPlayed;
    result += "*";
    result += this.OriginalBV;
    result += "*";
    result += this.timesDestroyed;
    return result;
  }
  
  public int getDBId() {
	  return this.DBID;
  }
  
  public void setDBId(int ID) {
	  this.DBID = ID;
  }
  
  public MechStatistics(String Filename,int mechsize)
  {
    this.mechFileName = Filename;
    this.mechSize = mechsize;
    this.gamesPlayed = 0;
    this.gamesWon = 0;
    this.timesDestroyed = 0;
  }

  public MechStatistics(String s)
  {
  	StringTokenizer ST = new StringTokenizer(s,"*");
    this.mechFileName = ST.nextToken();
    this.mechSize = Integer.parseInt(ST.nextToken());
    this.gamesWon = Integer.parseInt(ST.nextToken());
    this.gamesPlayed = Integer.parseInt(ST.nextToken());
    this.timesScrapped = Integer.parseInt(ST.nextToken());
    if (ST.hasMoreElements())
      this.lastTimeUpdated = Long.parseLong(ST.nextToken());
    if (ST.hasMoreElements())
      this.currentGamesWon = Integer.parseInt(ST.nextToken());
    if (ST.hasMoreElements())
      this.currentGamesPlayed = Integer.parseInt(ST.nextToken());
    if (ST.hasMoreElements())
      this.OriginalBV = Integer.parseInt(ST.nextToken());
    if (ST.hasMoreElements())
        this.timesDestroyed = Integer.parseInt(ST.nextToken());
  }

  public int getBV()
  {
  	int baseBV = 0;

  	
	currentGamesPlayed = gamesPlayed;
	currentGamesWon = gamesWon;
	
	// OK, we want the BV to modify slowly through each 100 battles
  	// starting at the actual unit BV, or the ModBV
  	// and varying by the stated Percentage from the campaignconfig.txt

	baseBV = getOriginalBV();
  	
	return baseBV;

  }

  public String addStats(int gamesPlayed, int gamesWon, int originalBV) {
  	
  	String result = "";
  	this.setGamesPlayed(getGamesPlayed() + gamesPlayed);
  	this.setGamesWon(getGamesWon() + gamesWon);
  	
  	if (getOriginalBV() == 0)
  		setOriginalBV( originalBV);
  	
  	return result;
  }

  public int compareTo(Object o)
  {
   MechStatistics m = (MechStatistics)o;
   return (this.getMechFileName().compareTo(m.getMechFileName()));
  }

	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getMechSize() {
    return mechSize;
  }
	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getGamesPlayed() {
    return gamesPlayed;
  }
	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getGamesWon() {
    return gamesWon;
  }
  public void setGamesPlayed(int gamesPlayed) {
    //Update the last time this int was changed
    if (gamesPlayed != this.gamesPlayed)
      this.lastTimeUpdated = System.currentTimeMillis();
    this.gamesPlayed = gamesPlayed;
  }
  public void setGamesWon(int gamesWon) {
    this.gamesWon = gamesWon;
  }

	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getTimesScrapped() {
    return timesScrapped;
  }

  public void setTimesScrapped(int timesScrapped) {
    this.timesScrapped = timesScrapped;
  }
  
  public int getTimesDestroyed(){
      return timesDestroyed;
  }
  
  public void setTimesDestroyed(int timesDestroyed){
      this.timesDestroyed = timesDestroyed;
  }
  
	 /**
   * @hibernate.property
   * @return Integer
   */
  public long getLastTimeUpdated() {
    return lastTimeUpdated;
  }
  public void setLastTimeUpdated(long lastTimeUpdated) {
    this.lastTimeUpdated = lastTimeUpdated;
  }
	 /**
   * @hibernate.id generator-class="native"
   * @return Integer
   */
  public Long getID() {
    return ID;
  }
  public void setID(Long id) {
    this.ID = id;
  }
  public void setMechSize(int mechSize) {
    this.mechSize = mechSize;
  }

  /**
   * @hibernate.property
   * @return Integer
   */
  public int getOriginalBV() {
  	if (OriginalBV == 0 && getMechFileName() != null)
  	{
  		// make a MegaMek entity and get it's BV
  		OriginalBV = SUnit.loadMech(getMechFileName()).calculateBattleValue();
  	}
    return OriginalBV;
  }

  public void setOriginalBV(int OriginalBV) {
    this.OriginalBV = OriginalBV;
  }
	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getCurrentGamesPlayed() {
    return currentGamesPlayed;
  }
	 /**
   * @hibernate.property
   * @return Integer
   */
  public int getCurrentGamesWon() {
    return currentGamesWon;
  }
  public void setCurrentGamesWon(int currentGamesWon) {
    this.currentGamesWon = currentGamesWon;
  }
  public void setCurrentGamesPlayed(int currentGamesPlayed) {
    this.currentGamesPlayed = currentGamesPlayed;
  }
}
