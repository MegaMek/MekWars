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

package server.campaign.pilot.skills;

import common.campaign.pilot.Pilot;


/**
 * Subclass from this class to create a skill that has different levels.
 * 
 * Note, that while you write modifyPilot, you must only add differences 
 * between levels. An example:
 * 
 * If your Pilot should get a malus to gunnery for each level, you have to add
 * only +1 to gunnery in modifyPilot, since the malus for previous levels 
 * are already applied.
 *
 * @author Immanuel Scholz (immanuel.scholz@gmx.de)
 */
public abstract class SLevelPilotSkill extends SPilotSkill {

    /**
     * Return the chance to get an upgrade to level "level" (from level-1).
     * @param level The level to upgrade to. If this is 1, the chance to get
     *      the skill at all is requested.
     */
    protected abstract int getUpgradeChance(int level, int unitType, Pilot pilot);

    /**
     * Return the current skill level
     */
    @Override
	public int getLevel() {
        return level;
    }
    
    /**
     * Construct a level based skill.
     * @param previous The previous level or null if it is level 1 skill.
     */
    public SLevelPilotSkill(int id, String name, SLevelPilotSkill previous, String abbreviation) {
        super(id, name, abbreviation);
        this.previous = previous;
        for (SLevelPilotSkill p = previous; p != null; p = p.previous)
            level++;
    }
    
    public SLevelPilotSkill(){
    	//TODO: Remove when no longer necessary
    }

    /**
     * Remove the old level skill from the pilot and add a new level.
     */
    @Override
	public void addToPilot(Pilot pilot) {
        super.addToPilot(pilot);
        pilot.getSkills().remove(previous);
    }

    /**
     * Get the chance by asking of the chance of level upgrades.
     * Do not override, but use getUpgradeChance() instead.
     */
    @Override
	final public int getChance(int unitType, Pilot pilot) {
        int level = getLevel();
        if (level == 1)
            return getUpgradeChance(1,unitType,pilot); // obtaining new skill
        if (pilot.getSkills().has(previous))
            return getUpgradeChance(level+1, unitType, pilot);
        return 0; // cannot jump over levels
    }
    

    /**
     * All leveled skills of one category are linked to each other.
     * This defines the previous skill in this category or null, if it is
     * the first skill.
     */
    private SLevelPilotSkill previous;
    /**
     * The skill level. Determinated at construction time from the previous 
     * chain.
     */
    private int level = 1;
}
