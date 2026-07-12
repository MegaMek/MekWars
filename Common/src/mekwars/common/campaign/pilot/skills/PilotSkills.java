/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
     * @param id one of {@link PilotSkill}'s {@code *SkillID} constants
     *
     * @return {@code true} if a skill with this ID is present in the set
     */
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
     * Returns an Iterator for the skills
     *
     * @author Helge Richter
     *
     */
    public Iterator<PilotSkill> getSkillIterator() {
        return skills.iterator();
    }

    /**
     * Add a skill to the pilot's skill list. No-op if {@code pilotSkill} is {@code null} or an equal skill is
     * already present (duplicates are silently ignored rather than added twice).
     */
    public void add(PilotSkill pilotSkill) {
        if (pilotSkill != null && !has(pilotSkill)) {
            skills.add(pilotSkill);
        }
    }

    /**
     * Return whether a pilot gets a specific skill or not.
     */
    public boolean has(PilotSkill pilotSkill) {
        if (pilotSkill == null) {
            return false;
        }

        return skills.contains(pilotSkill);
    }

    /**
     * Removes a skill from the pilot's skill list
     */
    public void remove(PilotSkill pilotSkill) {
        if (pilotSkill != null) {
            skills.remove(pilotSkill);
        }
    }

    /**
     * Returns the number of Skills
     *
     * @author Helge Richter
     *
     */
    public int size() {
        return skills.size();
    }

    /** @return the backing list of skills directly; callers may mutate it in place. */
    public LinkedList<PilotSkill> getPilotSkills() {
        return skills;
    }

    /**
     * @param skillID one of {@link PilotSkill}'s {@code *SkillID} constants
     *
     * @return the matching skill, or {@code null} if the pilot does not have it
     */
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
