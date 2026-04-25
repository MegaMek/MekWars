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

package mekwars.common.campaign.pilot.skills;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Encapsulates a set of piloting skills.
 *
 * @author Immanuel Scholz (immanuel.scholz@gmx.de)
 */

public class PilotSkills {
    /**
     * The data storage for the skills.
     */
    private final LinkedList<PilotSkill> skills = new LinkedList<>();

    /**
     * Return whether a pilot obtains a specific skill or not.
     */
    public boolean has(PilotSkill p) {
        if (p == null) {
            return false;
        }

        return skills.contains(p);
    }

    public boolean has(int id) {
        Iterator<PilotSkill> it = getSkillIterator();
        while (it.hasNext()) {
            if (it.next().getId() == id) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a skill to the pilot's skill list.
     */
    public void add(PilotSkill p) {
        if (p != null && !has(p)) {skills.add(p);}
    }

    /**
     * Removes a skill from the pilot's skill list
     */
    public void remove(PilotSkill p) {
        if (p != null) {skills.remove(p);}
    }

    /**
     * Returns the amount of Skills
     *
     * @author Helge Richter
     *
     */
    public int size() {
        return skills.size();
    }

    /**
     * Returns an Iterator for the skills
     *
     * @author Helge Richter
     *
     */
    public Iterator<PilotSkill> getSkillIterator() {
        return skills.iterator();
    }

    public LinkedList<PilotSkill> getPilotSkills() {
        return skills;
    }

    public PilotSkill getPilotSkill(int skillID) {

        PilotSkill pSkill;
        Iterator<PilotSkill> skills = this.getSkillIterator();

        while (skills.hasNext()) {
            pSkill = skills.next();
            if (pSkill.getId() == skillID) {
                return pSkill;
            }
        }
        return null;
    }

    /**
     * Returns a readable description of the skills
     *
     * @author Helge Richter
     *
     */
    public String getDescription() {
        Iterator<PilotSkill> i = getSkillIterator();
        StringBuilder result = new StringBuilder();

        if (i.hasNext()) {
            result = new StringBuilder(" Skills: ");
        }

        while (i.hasNext()) {
            PilotSkill skill = i.next();
            result.append(skill.getName());

            if (skill.getLevel() > 1) {
                result.append(" ").append(skill.getLevel());
            }

            if (i.hasNext()) {
                result.append(", ");
            }
        }
        
        return result.toString();
    }
}
