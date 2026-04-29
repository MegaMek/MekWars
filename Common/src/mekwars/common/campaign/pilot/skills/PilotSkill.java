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
    public final static int ClanPilotTrainingID = 21;
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
    private String abbreviation = "UsersCommand";


    private String description = "None";

    /**
     * A level if the skill has one or -1 if it doesn't have levels
     */
    private int level = -1;

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

    public static int getMMSkillID(String skill) {
        int skillID = -1;

        return switch (skill) {
            case "dodge_maneuver" -> PilotSkill.DodgeManeuverSkillID;
            case "maneuvering_ace" -> PilotSkill.ManeuveringAceSkillID;
            case "melee_specialist" -> PilotSkill.MeleeSpecialistSkillID;
            case "pain_resistance" -> PilotSkill.PainResistanceSkillID;
            case "tactical_genius" -> PilotSkill.TacticalGeniusSkillID;
            case "weapon_specialist" -> PilotSkill.WeaponSpecialistSkillID;
            case "gunnery_laser" -> PilotSkill.GunneryLaserSkillID;
            case "gunnery_missile" -> PilotSkill.GunneryMissileSkillID;
            case "gunnery_ballistic" -> PilotSkill.GunneryBallisticSkillID;
            case "iron_man" -> PilotSkill.IronManSkillID;
            case "ei_implant" -> PilotSkill.EnhancedInterfaceID;
            case "clan_pilot_training" -> PilotSkill.ClanPilotTrainingID;
            case "edge" -> PilotSkill.EdgeSkillID;
            case "vdni" -> PilotSkill.VDNIID;
            case "bvdni" -> PilotSkill.BufferedVDNIID;
            case "pain_shunt" -> PilotSkill.PainShuntID;
            default -> skillID;
        };


    }

    /**
     * get the Name of this skill
     *
     */
    final public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * get the Abbreviation of this skill
     *
     */
    final public String getAbbreviation() {
        return abbreviation;
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
     * @param level The level to set.
     */
    public void setLevel(int level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
