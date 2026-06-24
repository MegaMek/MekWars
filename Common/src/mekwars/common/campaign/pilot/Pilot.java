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
 * @author Helge Richter
 *
 */

public class Pilot {

    private final LinkedList<MegaMekPilotOption> megaMekOptions = new LinkedList<>();
    /**
     * List of skills this pilot has got.
     */
    private final PilotSkills skills = new PilotSkills();
    boolean edge_when_tac = true;
    boolean edge_when_ko = true;
    boolean edge_when_head_hit = true;
    boolean edge_when_explosion = true;
    private int gunnery = 4;
    private int piloting = 5;
    private String name = "John Doe";
    private int experience = 0;
    private int hits = 0;
    private String weapon = "Default";//for Weapon Specialist skill
    private String currentFaction = "none";
    private String traitName = "none";
    private int id = -1;
    private int DBId = -1;
    private double bvMod = 0.0;
    private int bayModifier = 0;
    private int kills = 0;
    private int unitType = 0; //set the unit type good for checking stuff

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

    public String getSkillString(boolean abbreviated) {
        return getSkillString(abbreviated, "");
    }

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

    public String getWeapon() {
        return this.weapon;
    }

    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }

    public String getTraitName() {
        return traitName;
    }

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

    public void addKill(int kill) {
        setKills(getKills() + kill);
    }

    public int getKills() {
        return kills;
    }

    public void setKills(int kill) {
        kills = kill;
    }

    public int getUnitType() {
        return this.unitType;
    }

    public void setUnitType(int type) {
        this.unitType = type;
    }

    public String getCurrentFaction() {
        return currentFaction;
    }

    public void setCurrentFaction(String faction) {
        currentFaction = faction;
    }

    public int getPilotId() {
        return this.id;
    }

    public void setPilotId(int id) {
        this.id = id;
    }

    public int getDBId() {
        return this.DBId;
    }

    public void setDBId(int i) {
        this.DBId = i;
    }

    public boolean getTac() {return edge_when_tac;}

    public void setTac(boolean value) {edge_when_tac = value;}

    public boolean getKO() {return edge_when_ko;}

    public void setKO(boolean value) {edge_when_ko = value;}

    public boolean getHeadHit() {return edge_when_head_hit;}

    public void setHeadHit(boolean value) {edge_when_head_hit = value;}

    public boolean getExplosion() {return edge_when_explosion;}

    public void setExplosion(boolean value) {edge_when_explosion = value;}
}
