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

package mekwars.common.campaign.pilot;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;

import mekwars.common.MegaMekPilotOption;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.campaign.pilot.skills.PilotSkills;


/**
 * MekWars' own model of a MechWarrior/pilot: name, gunnery/piloting skill ratings, campaign-specific
 * {@link PilotSkill}s (Weapon Specialist, Trait, etc.), kill count, and current faction assignment.
 * <p>
 * This is distinct from MegaMek's own crew/pilot classes; it is the persistent, campaign-tracked record that
 * MekWars layers on top, and is what gets saved/loaded between game sessions and translated to/from a MegaMek
 * {@code Entity}'s crew when a battle is set up (see {@link #getMegaMekOptions()}).
 *
 * @author Helge Richter
 */
public class Pilot {

    /** MegaMek pilot options (SPAs, implants, etc.) accumulated for this pilot, applied to the crew at battle time. */
    private final LinkedList<MegaMekPilotOption> megaMekOptions = new LinkedList<>();
    /**
     * List of skills this pilot has got.
     */
    private final PilotSkills skills = new PilotSkills();

    /** Whether this pilot's Edge points may be spent to reroll a Through Armor Critical. */
    boolean edge_when_tac = true;

    /** Whether this pilot's Edge points may be spent to reroll a knockout/consciousness roll. */
    boolean edge_when_ko = true;

    /** Whether this pilot's Edge points may be spent to reroll a head-hit result. */
    boolean edge_when_head_hit = true;

    /** Whether this pilot's Edge points may be spent to reroll an ammo/fuel explosion. */
    boolean edge_when_explosion = true;
    private int gunnery = 4;
    private int piloting = 5;
    private String name = "John Doe";
    private int experience = 0;
    private int hits = 0;

    /** Weapon chosen for the "Weapon Specialist" skill; ignored unless that skill is present. */
    private String weapon = "Default";//for Weapon Specialist skill
    private String currentFaction = "none";

    /** Free-text trait descriptor used by the "Trait" skill; may contain multiple {@code *}-separated parts. */
    private String traitName = "none";

    /** In-memory/session identifier for this pilot. */
    private int id = -1;

    /** Identifier used to look this pilot up in the persistent database, separate from the session {@link #id}. */
    private int DBId = -1;

    /** Battle Value modifier contributed by this pilot (e.g. from skills/traits), applied when computing unit BV. */
    private double bvMod = 0.0;

    /** Modifier applied to this pilot's assigned bay/dropship slot weighting. */
    private int bayModifier = 0;
    private int kills = 0;

    /** The type of unit (Mek, vehicle, aero, etc.) this pilot is currently qualified/assigned for. */
    private int unitType = 0; //set the unit type good for checking stuff

    /**
     * Creates a pilot with the given name and skill ratings.
     */
    public Pilot(String name, int gunnery, int piloting) {
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

    /**
     * @see #getSkillString(boolean, String)
     */
    public String getSkillString(boolean abbreviated) {
        return getSkillString(abbreviated, "");
    }

    /**
     * Builds a human-readable, comma-separated summary of this pilot's skills, e.g. {@code "Weapon Specialist
     * Large Laser, Sniper 2"} or, abbreviated, {@code "WS,SN2"}.
     * <p>
     * "Weapon Specialist" is special-cased to append the pilot's chosen {@link #getWeapon()} instead of a level,
     * and "Trait" is special-cased to show only the first {@code *}-delimited segment of {@link #getTraitName()}.
     *
     * @param abbreviated whether to use each skill's short abbreviation instead of its full name
     * @param houseSkills skill names that belong to the player's house rather than the pilot personally; these are
     *                    excluded from the summary since they're already shown elsewhere
     *
     * @return the formatted skill summary, or {@code ""} if the pilot has no skills
     */
    public String getSkillString(boolean abbreviated, String houseSkills) {

        StringBuilder result = new StringBuilder();

        Iterator<PilotSkill> i = getSkills().getSkillIterator();
        if (!i.hasNext()) {return "";}

        while (i.hasNext()) {
            PilotSkill skill = i.next();
            //Do not list house skills for pilots
            if (houseSkills.contains(skill.getName())) {
                continue;
            }

            String lvl = "";
            if (skill.getLevel() != -1) {lvl += skill.getLevel();}
            if (abbreviated) {result.append(skill.getAbbreviation()).append(lvl);} else {
                if (skill.getName().equalsIgnoreCase("Weapon Specialist")) {
                    result.append(skill.getName().trim()).append(" ").append(this.getWeapon());
                } else if (skill.getName().equalsIgnoreCase("Trait")) {
                    StringTokenizer traitName = new StringTokenizer(getTraitName(), "*");
                    result.append(traitName.nextToken().trim());
                } else if (!lvl.isEmpty()) {
                    result.append(skill.getName().trim()).append(" ").append(lvl);
                } else {
                    result.append(skill.getName().trim());
                }
            }
            if (i.hasNext()) {
                result.append(",");
                if (!abbreviated) {result.append(" ");}
            }
        }

        if (result.toString().trim().endsWith(",")) {result.deleteCharAt(result.lastIndexOf(","));}
        return result.toString().trim();
    }

    /**
     * @return Returns the skills.
     */
    public PilotSkills getSkills() {
        return skills;
    }

    /** @return the weapon this pilot specializes in, used by the "Weapon Specialist" skill. */
    public String getWeapon() {
        return this.weapon;
    }

    /** @param weapon the weapon to associate with the "Weapon Specialist" skill. */
    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }

    /** @return the raw trait descriptor used by the "Trait" skill; may be {@code *}-delimited. */
    public String getTraitName() {
        return traitName;
    }

    /** @param Trait the raw trait descriptor to associate with the "Trait" skill. */
    public void setTraitName(String Trait) {
        traitName = Trait;
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

    /** Adds a MegaMek pilot option (SPA, implant, etc.) to be applied to this pilot's crew at battle time. */
    public void addMegaMekOption(MegaMekPilotOption op) {
        megaMekOptions.add(op);
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
    public void setBVMod(double bvMod) {
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
     * @return Returns the megamekOptions.
     */
    public LinkedList<MegaMekPilotOption> getMegaMekOptions() {
        return megaMekOptions;
    }

    /** Increments this pilot's kill count by {@code kill}. */
    public void addKill(int kill) {
        setKills(getKills() + kill);
    }

    /** @return the total number of kills credited to this pilot. */
    public int getKills() {
        return kills;
    }

    /** @param kill the total kill count to set (not incremental; see {@link #addKill(int)} to add to it). */
    public void setKills(int kill) {
        kills = kill;
    }

    /** @return the unit type (Mek, vehicle, aero, etc.) this pilot is currently qualified/assigned for. */
    public int getUnitType() {
        return this.unitType;
    }

    /** @param type the unit type to record this pilot as being qualified/assigned for. */
    public void setUnitType(int type) {
        this.unitType = type;
    }

    /** @return the name of the faction this pilot currently belongs to. */
    public String getCurrentFaction() {
        return currentFaction;
    }

    /** @param faction the name of the faction to assign this pilot to. */
    public void setCurrentFaction(String faction) {
        currentFaction = faction;
    }

    /** @return the in-memory/session identifier for this pilot. */
    public int getPilotId() {
        return this.id;
    }

    /** @param id the in-memory/session identifier to assign to this pilot. */
    public void setPilotId(int id) {
        this.id = id;
    }

    /** @return the identifier used to look this pilot up in the persistent database. */
    public int getDBId() {
        return this.DBId;
    }

    /** @param i the database identifier to assign to this pilot. */
    public void setDBId(int i) {
        this.DBId = i;
    }

    /** @return whether this pilot may spend Edge to reroll a Through Armor Critical. */
    public boolean getTac() {return edge_when_tac;}

    /** @param value whether this pilot may spend Edge to reroll a Through Armor Critical. */
    public void setTac(boolean value) {edge_when_tac = value;}

    /** @return whether this pilot may spend Edge to reroll a knockout/consciousness roll. */
    public boolean getKO() {return edge_when_ko;}

    /** @param value whether this pilot may spend Edge to reroll a knockout/consciousness roll. */
    public void setKO(boolean value) {edge_when_ko = value;}

    /** @return whether this pilot may spend Edge to reroll a head-hit result. */
    public boolean getHeadHit() {return edge_when_head_hit;}

    /** @param value whether this pilot may spend Edge to reroll a head-hit result. */
    public void setHeadHit(boolean value) {edge_when_head_hit = value;}

    /** @return whether this pilot may spend Edge to reroll an ammo/fuel explosion. */
    public boolean getExplosion() {return edge_when_explosion;}

    /** @param value whether this pilot may spend Edge to reroll an ammo/fuel explosion. */
    public void setExplosion(boolean value) {edge_when_explosion = value;}
}
