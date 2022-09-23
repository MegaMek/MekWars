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

package common.campaign.pilot.skills;

/**
 * A description of a pilot skill visible to both, server and client.
 * 
 * @author Helge Richter and Immanuel Scholz
 */
public class PilotSkill {

	public final static int DodgeManeuverSkillID = 1;
	public final static int AstechSkillID = 2;
	public final static int MeleeSpecialistSkillID = 3;
	public final static int PainResistanceSkillID = 4;
	public final static int NaturalAptitudeGunnerySkillID = 5;
	public final static int NaturalAptitudePilotingSkillID = 6;
	public final static int ManeuveringAceSkillID = 7;
	public final static int TacticalGeniusSkillID = 8;
	public final static int GunneryBallisticSkillID = 9;
	public final static int GunneryLaserSkillID = 10;
	public final static int GunneryMissileSkillID = 11;
	public final static int WeaponSpecialistSkillID = 12;
	public final static int IronManSkillID = 13;
	public final static int SurvivalistSkillID = 14;
	public final static int TraitID = 15;
	public final static int EnhancedInterfaceID = 16;
    public final static int QuickStudyID = 17;
    public final static int GiftedID = 18;
    public final static int MedTechID = 19;
    public final static int EdgeSkillID = 20;
    public final static int ClanPilotTraingID = 21;
    public final static int VDNIID = 22;
    public final static int BufferedVDNIID = 23;
    public final static int PainShuntID = 24;

    /**
     * The unique ID of this skill
     */
    private int id;
    
    /**
     * Each skill has a name to display.
     */
	private String name = "Unnamed Skill";
   /**
    * Each skill has an abbreviation to display for when the name takes too much space.
    */
	private String abbreviation = "US";
	

    private String description = "None";

	/**
	 * A level if the skill has one or -1 if it doesn't have levels
	 */
	private int level = -1;
	
	/**
	 * get the Name of this skill
	 * @return
	 */
	final public String getName() {
		return name;
	}
	

	/**
	 * get the Abbreviation of this skill
	 * @return
	 */
	final public String getAbbreviation() {
		return abbreviation;
	}

    
    /**
     * Creates a skill with a given name and id.
     */
	
	public PilotSkill(int id, String name, int level) {
		this(id, name, level, "");
	}
    
    public PilotSkill(int id, String name, int level, String abbreviation) {
    	this.name = name;
    	this.id = id;
    	this.level = level;
    	this.abbreviation = abbreviation;
    }
    
    /**
     * Needed for serialization. Creates an unamed skill.
     */
    public PilotSkill() {
    }
    
    /**
     * @return Returns the id.
     */
    final public int getId() {
        return id;
    }
	/**
	 * @return Returns the level.
	 */
	public int getLevel() {
		return level;
	}
	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}
 	/**
 	 * @param level The level to set.
 	 */
 	public void setLevel(int level) {
       this.level = level;
 	}
    
    public String getDescription(){
        return description;
    }
    
    public void setDescription(String description){
        this.description = description;
    }
    
    public static int getMMSkillID(String skill) {
    	int skillID = -1;
    	
        if ( skill.equals("dodge_maneuver") )
        	return PilotSkill.DodgeManeuverSkillID;
        if ( skill.equals("maneuvering_ace") )
        	return PilotSkill.ManeuveringAceSkillID;
        if ( skill.equals("melee_specialist") )
        	return PilotSkill.MeleeSpecialistSkillID;
        if ( skill.equals("pain_resistance") )
        	return PilotSkill.PainResistanceSkillID;
        if ( skill.equals("tactical_genius") )
        	return PilotSkill.TacticalGeniusSkillID;
        if ( skill.equals("weapon_specialist") )
        	return PilotSkill.WeaponSpecialistSkillID;
        if ( skill.equals("gunnery_laser") )
        	return PilotSkill.GunneryLaserSkillID;
        if ( skill.equals("gunnery_missile") )
        	return PilotSkill.GunneryMissileSkillID;
        if ( skill.equals("gunnery_ballistic") )
        	return PilotSkill.GunneryBallisticSkillID;
        if ( skill.equals("iron_man") )
        	return PilotSkill.IronManSkillID;
        if ( skill.equals("ei_implant") )
        	return PilotSkill.EnhancedInterfaceID;
        if ( skill.equals("clan_pilot_training") )
        	return PilotSkill.ClanPilotTraingID;
        if ( skill.equals("edge") )
        	return PilotSkill.EdgeSkillID;
        if ( skill.equals("vdni") )
            return PilotSkill.VDNIID;
        if ( skill.equals("bvdni") )
            return PilotSkill.BufferedVDNIID;
        if ( skill.equals("pain_shunt") )
            return PilotSkill.PainShuntID;

    	
    	return skillID;
    }
}
