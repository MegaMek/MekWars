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

/*
 * Created on 8/25/2004
 *
 */

package common;

import common.flags.PlayerFlags;


public class Player {
	
	private int technicians = 0;//@urgru 7/17/04
	private int currentTechPayment = -1;//num Cbills owed to techs after games
	private boolean isInvisible = false;//Evil command for Big brother err admins.
	private int teamNumber = -1;
	private boolean autoReorderParts = false;
	protected PlayerFlags flags = new PlayerFlags();
	protected PlayerFlags defaultPlayerFlags = new PlayerFlags(); // This is only going to be set for staff
	protected int mekToken = 0; // A counter for how many meks a player is allowed to create in freebuild
	protected int bvTracker = 0; // used to track hangar BV in mini campaigns
	
	/**
	 * @return bvTracker value
	 */
	public int getBVTracker() {
		return bvTracker;
	}

	/**
	 * @param set the bvTracker value
	 */
	public void setBVTracker(int bvtracker) {
		bvTracker = bvtracker;
	}
	
	/**
	 * @return the mekToken
	 */
	public int getMekToken() {
		return mekToken;
	}

	/**
	 * @param mekToken the mekToken to set
	 */
	public void setMekToken(int mekToken) {
		this.mekToken = mekToken;
	}

	/**
	 * @return current post-task payment to technicians, in Cbills
	 */
	public int getCurrentTechPayment() {
		return currentTechPayment;
	}
	
	/**
	 * @param i post-task payment to set, in Cbills
	 */
	public void setCurrentTechPayment(int i) {
		currentTechPayment = i;
	}
	  
	/**
	 * @return the number of technicians the player has
	 */
	public int getTechnicians() {
		return technicians;
	}//end getTechnicians()
     
	/**
	 * @param int to set technicians to.
	 */
	public void setTechnicians(int t) {

		if (t < 0)//dont allow negative techs. always set negatives back to 0.
			t = 0;
		technicians = t;
		
		//clear the tech payment any time a new number of techs is set
		currentTechPayment = -1;
	}//end setTechnicians()
      
	/**
	 * @param the number of technicians to add (subtract) from the player's total
	 * 
	 * NOTE: sub-zero cases are checked in setTechs(). no check here.
	 */
	public void addTechnicians(int t) {
		this.setTechnicians(technicians + t);
	}
    
    /**
     * Sets that a player now has the invis flag. 
     * of course players with access levels >= this player
     * will still beable to see them.
     * @param invis
     */
    public void setInvisible(boolean invis){
        isInvisible = invis;
    }
    
    /**
     * does the player have the invisible flag.
     * @return true/false.
     */
    public boolean isInvisible(){
        return isInvisible;
    }

    /**
     * Returns players team number
     * @return
     */
    public int getTeamNumber() {
    	return teamNumber;
    }
    
    /**
     * Set Players team number for the current op.
     * @param team
     */
    public void setTeamNumber(int team) {
    	this.teamNumber = team;
    }
    
    /**
     * Sets if the player wants to reorder parts.
     * @param reorder
     */
    public void setAutoReorder(boolean reorder){
    	this.autoReorderParts = reorder;
    }
    
    /**
     * Returns if the player has auto reorder parts turned on.
     * @return
     */
    public boolean getAutoReorder(){
    	return this.autoReorderParts;
    }
    
    /**
     * Sets a player flag
     * @param name - the name of the flag to set
     * @param value - true or false (String value)
     */
    public void setFlagStatus(String name, String value) {
    	setFlagStatus(name, Boolean.parseBoolean(value));
    }
    
    /**
     * Sets a player flag
     * @param name - the name of the flag to set
     * @param value - true or false (boolean value)
     */
    public void setFlagStatus(String name, boolean value) {
    	flags.setFlag(name, value);
    }
    
    /** Returns the value of a flag
     * 
     * @param name - the name of the flag to get
     * @return true or false (boolean value)
     */
    public boolean getFlagStatus(String name) {
    	return flags.getFlagStatus(name);
    }
    
    /**
     * Loads the set of server-defined players flags from a string.
     * This should probably only be called during player logon, and then
     * each flag can be set individually
     * @param data
     */
    public void loadFlags (String data) {
    	flags.loadDefaults(data);
    }
    
    /**
     * Exports the string of flags read by loadFlags
     * @return flag data
     */
    public String exportFlags() {
    	return flags.export();
    }

	/**
	 * @return the defaultPlayerFlags
	 */
	public PlayerFlags getDefaultPlayerFlags() {
		return defaultPlayerFlags;
	}
	
	/**
	 * @return the playerFlags
	 */
    public PlayerFlags getFlags() {
    	return flags;
    }
}//End Class Player