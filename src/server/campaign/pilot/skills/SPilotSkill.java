/*
 * MekWars - Copyright (C) 2004
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * Created on 18.04.2004
 */
package server.campaign.pilot.skills;

import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import megamek.common.Entity;
import server.campaign.pilot.SPilot;

/**
 * Base class for all skill implementations. Overide this if you want to create
 * a cool new skill. You may want to use SLevelPilotSkill if you want a skill
 * with different Levels.
 * 
 * Also note, that the skill should not hold any internal state depending on any
 * parameter passed to any function to it, since the instance created only once
 * and then shared among all pilots.
 * 
 * @author Helge Richter and Immanuel Scholz
 */
public abstract class SPilotSkill extends PilotSkill {

    /**
     * Creates a skill with the given skill description.
     */
    public SPilotSkill(int id, String name, String abbreviation) {
        super(id, name, -1, abbreviation);
    }

    public SPilotSkill() {
        // TODO: replace with ReflectionProvider
    }

    /**
     * Override this, if you have changes to be done to the pilot's stats.
     * 
     * Do not touch the skill list of a pilot here, but change the other stats.
     * The skill will be automatically add to the pilot's list.
     */
    public void modifyPilot(Pilot pilot) {
    }

    /**
     * Override this, if you want a special handling of how the skill is added
     * to the pilot's skill list. As example it could remove other skills on its
     * list (look at SLevelPilotSkill), or auto-add another skill (you have to
     * call modifyPilot for that auto-added by yourself!).
     */
    public void addToPilot(Pilot pilot) {

        // AstechSkills follow their own Rules.
        if (this instanceof AstechSkill) {
            ((AstechSkill) this).addToPilot(pilot);
        } else if (this instanceof EdgeSkill) {
            ((EdgeSkill) this).addToPilot(pilot);
        } else {
            setLevel(-1);
            pilot.getSkills().add(this);
        }
    }

    /**
     * Remove a skill from a pilot
     * 
     * @param pilot
     */
    public void removeFromPilot(Pilot pilot) {
        pilot.getSkills().remove(this);
    }

    /**
     * This is called by the pilot's level-up to determinate whether the class
     * can be used for this pilot. getChance should return 0 if this skill
     * cannot be used (as example because the pilot has already that skill).
     */
    public abstract int getChance(int unitType, Pilot pilot);

    public abstract int getBVMod(Entity unit);

    public int getBVMod(Entity unit, SPilot pilot) {
        return 0;
    }
}
