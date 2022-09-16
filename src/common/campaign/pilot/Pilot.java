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

/*
 * Created on 24.03.2004
 *
 */
package common.campaign.pilot;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import common.MegaMekPilotOption;
import common.campaign.pilot.skills.PilotSkill;
import common.campaign.pilot.skills.PilotSkills;


/**
 * @author Helge Richter
 *
 */

public class Pilot {

	private int gunnery = 4;
	private int piloting = 5;
	private String name = "John Doe";
	private int experience = 0;
	private int hits = 0; 
	private LinkedList<MegaMekPilotOption> megamekOptions = new LinkedList<MegaMekPilotOption>();
	private String weapon = "Default";//for Weapon Specialist skill
	private String currentFaction = "none";
	private String traitName = "none";
	private int id = -1;
	private int DBId = -1;
    boolean edge_when_tac = true;
    boolean edge_when_ko = true;
    boolean edge_when_headhit = true;
    boolean edge_when_explosion = true;
    
    /**
     * List of skills this pilot has obtained.
     */
	private PilotSkills skills = new PilotSkills();
	private double bvMod = 0.0;
	private int bayModifier = 0;
	private int kills = 0;
	private int unitType = 0; //set the units type good for checking stuff
	


	public Pilot(String name,int gunnery, int piloting) {
		setName(name);
		setGunnery(gunnery);
		setPiloting(piloting);
	}

    /**
     * Used for serialization
     */
    public Pilot() {
    }

	/**
	 * @return Returns the gunnery.
	 */
	public int getGunnery() {
		return gunnery;
	}

	/**
	 * @param gunnery The gunnery to set.
	 */
	public void setGunnery(int gunnery) {
		this.gunnery = gunnery;
	}
	
	public String getSkillString(boolean abbreviated) {
		return getSkillString(abbreviated,"");
	}
	
	public String getSkillString(boolean abbreviated, String houseSkills) {
		
		StringBuilder result = new StringBuilder();
		
		Iterator<PilotSkill> i = getSkills().getSkillIterator();
		if (!i.hasNext())
			return "";
		
		while (i.hasNext()) {
			PilotSkill skill = (PilotSkill) i.next();
			//Do not list house skills for pilots
			if ( houseSkills.indexOf(skill.getName()) >= 0)
				continue;
			
			String lvl = "";
			if (skill.getLevel() != -1)
				lvl += skill.getLevel();
			if (abbreviated)
				result.append(skill.getAbbreviation() + lvl);
			else{
			    if(skill.getName().equalsIgnoreCase("Weapon Specialist"))
			        result.append(skill.getName().trim() + " " + this.getWeapon());
			    else if (skill.getName().equalsIgnoreCase("Trait")){
			        StringTokenizer traitName = new StringTokenizer(getTraitName(),"*");
			        result.append(traitName.nextToken().trim());
			    } else if ( lvl.length() > 0 )
			        result.append(skill.getName().trim() + " " + lvl);
			    else
			    	result.append(skill.getName().trim());
			}
			if (i.hasNext()) {
				result.append(",");
				if (!abbreviated) {result.append(" ");}
			}
		}
		
		if ( result.toString().trim().endsWith(",") )
			result.deleteCharAt(result.lastIndexOf(","));
		return result.toString().trim();
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the piloting.
	 */
	public int getPiloting() {
		return piloting;
	}

	/**
	 * @param piloting The piloting to set.
	 */
	public void setPiloting(int piloting) {
		this.piloting = piloting;
	}

	/**
	 * @return Returns the hits.
	 */
	public int getHits() {
		return hits;
	}

	/**
	 * @param hits The hits to set.
	 */
	public void setHits(int hits) {
		this.hits = hits;
	}

	/**
	 * @return Returns the experience.
	 */
	public int getExperience() {
		return experience;
	}

	/**
	 * @param experience The experience to set.
	 */
	public void setExperience(int experience) {
		this.experience = experience;
	}
	
	public void addMegamekOption (MegaMekPilotOption op) {
		megamekOptions.add(op);
	}
	
	/**
	 * @return Returns the bvMod.
	 */
	public double getBVMod() {
		return bvMod;
	}
	/**
	 * @param bvMod The bvMod to set.
	 */
	public void setBvMod(double bvMod) {
		this.bvMod = bvMod;
	}
	/**
	 * @return Returns the bayModifier.
	 */
	public int getBayModifier() {
		return bayModifier;
	}
	/**
	 * @param bayModifier The bayModifier to set.
	 */
	public void setBayModifier(int bayModifier) {
		this.bayModifier = bayModifier;
	}
    
    /**
     * @return Returns the skills.
     */
    public PilotSkills getSkills() {
        return skills;
    }
	/**
	 * @return Returns the megamekOptions.
	 */
	public LinkedList<MegaMekPilotOption> getMegamekOptions() {
		return megamekOptions;
	}
	
    public int getKills(){
        return kills;
    }
    
    public void setKills(int kill){
        kills = kill;
    }
    
    public void addKill(int kill){
        setKills(getKills()+kill);
    }
    
    public void setWeapon(String weapon){
        this.weapon = weapon;
    }
    
    public String getWeapon(){
        return this.weapon;
    }
    
    public void setUnitType(int type){
        this.unitType = type;
    }
    
    public int getUnitType(){
        return this.unitType;
    }
    
    public String getCurrentFaction(){
        return currentFaction;
    }
    
    public void setCurrentFaction(String faction){
        currentFaction = faction;
    }
    
    public String getTraitName(){
        return traitName;
    }
    
    public void setTraitName(String Trait){
        traitName = Trait;
    }
    
    public int getPilotId(){
        return this.id;
    }
    
    public void setPilotId(int id){
        this.id = id;
    }

    public int getDBId() {
    	return this.DBId;
    }
    
    public void setDBId(int i) {
    	this.DBId = i;
    }
    
    public boolean getTac(){return edge_when_tac;}
    public boolean getKO(){return edge_when_ko;}
    public boolean getHeadHit(){return edge_when_headhit;}
    public boolean getExplosion(){return edge_when_explosion;}

    public void setTac(boolean value){ edge_when_tac = value;}
    public void setKO(boolean value){ edge_when_ko = value;}
    public void setHeadHit(boolean value){ edge_when_headhit = value;}
    public void setExplosion(boolean value){ edge_when_explosion = value;}
 }
