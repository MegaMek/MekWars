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
 * Created on 23.03.2004
 *
 */
package mekwars.common;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import megamek.common.TechConstants;
import megamek.common.equipment.AmmoType;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * Represents a major faction ("House") in the campaign galaxy — e.g. a BattleTech Great House, Clan, or
 * mercenary/independent power. A House owns the top-level campaign-wide settings for its members: base
 * pilot/gunner skill levels, per unit-type/weight price and component-cost modifiers, banned munitions,
 * tech-level restrictions, defection policy, and its display color/logo/abbreviation.
 * <p>
 * A House is further subdivided into named {@link SubFaction}s (regiments, commands, etc. — see
 * {@link #getSubFactionList()}), each of which can layer its own access-level and unit-purchase
 * restrictions on top of the House's settings. Political control of a {@link Planet} is tracked separately
 * via {@link Influences}, which records how much influence each House (by id) has accumulated on that
 * planet; the House with a clear plurality is considered the planet's owner.
 *
 * @author Helge Richter
 *
 */
public class House {

    /** Index of the red channel within a packed RGB color representation. */
    public static final int RED_VALUE = 0;
    /** Index of the green channel within a packed RGB color representation. */
    public static final int GREEN_VALUE = 1;
    /** Index of the blue channel within a packed RGB color representation. */
    public static final int BLUE_VALUE = 2;

    /** Base gunnery skill per unit build-type index (lower is better, MegaMek convention). */
    private final Vector<Integer> baseGunner = new Vector<>(Unit.MAX_BUILD, 1);

    /** Base piloting skill per unit build-type index (lower is better, MegaMek convention). */
    private final Vector<Integer> basePilot = new Vector<>(Unit.MAX_BUILD, 1);

    /** Base piloting special-skill string (e.g. Clan pilot abilities) per unit build-type index. */
    private final Vector<String> basePilotSkills = new Vector<>(Unit.MAX_BUILD, 1);

    /** Per [unit type][weight class] price modifier this faction applies when buying units. */
    private final int[][] factionUnitPriceMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]

    /** Per [unit type][weight class] influence/"flu" cost modifier this faction applies when buying units. */
    private final int[][] factionUnitFluMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]

    /** Per [unit type][weight class] component-cost modifier this faction applies when buying units. */
    private final int[][] factionUnitComponentMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]

    /** Ammunition/munition types this faction is forbidden from using. */
    private final EnumSet<AmmoType.Munitions> bannedAmmo = EnumSet.noneOf(AmmoType.Munitions.class);

    /** This faction's sub-factions (see {@link SubFaction}), keyed by sub-faction name. */
    private final ConcurrentHashMap<String, SubFaction> subFactionList = new ConcurrentHashMap<>();

    // private int factionPlayerColors[] = new int[3]; // [red,green,blue]
    /** Multiplier applied to the bay cost of used (as opposed to new) Mek purchases for this faction. */
    public float usedMekBayMultiplier;

    /** Tracks which unit file names this faction "supports" and a reference count of how many times added. */
    public ConcurrentHashMap<String, Integer> supportedUnits = new ConcurrentHashMap<>();
    private String name = "none";
    private String logo = "";
    private String factionFluFile = "Common";
    private Integer id;
    private int dbId = 0;
    private String factionColor = "#000000";
    private String abbreviation = "";
    private String factionPlayerColors = "#000000";
    private boolean conquerable = true;
    private int techLevel = TechConstants.T_ALLOWED_ALL;
    private boolean allowDefectionsFrom = true;
    private boolean allowDefectionsTo = true;
    private boolean nonFactionUnitsCostMore = false;

    /**
     * Creates a new faction with the given id, initializing default per-build-type base gunnery (4),
     * piloting (5), and pilot-skill (blank) values for every {@code Unit.MAX_BUILD} unit build type.
     *
     * @param id the unique faction id.
     */
    public House(int id) {
        this.id = id;
        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            baseGunner.add(4);
            basePilot.add(5);
            basePilotSkills.add(" ");
        }
    }

    /**
     * Constructor used for serialization. Initializes the same per-build-type defaults as
     * {@link #House(int)}, but leaves the id unset (fields are populated afterwards by the
     * serialization framework).
     */
    public House() {
        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            baseGunner.add(4);
            basePilot.add(5);
            basePilotSkills.add(" ");
        }
    }

    /**
     * Reads a full House definition back from a binary stream: identity, base skills, colors,
     * abbreviation, per unit-type/weight cost modifiers, banned ammo, tech level, defection policy, the
     * used-Mek-bay multiplier, and all of its {@link SubFaction}s.
     *
     * @param in the binary stream reader.
     * @throws IOException if the underlying stream read fails.
     */
    public House(BinReader in) throws IOException {

        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            baseGunner.add(4);
            basePilot.add(5);
            basePilotSkills.add(" ");
        }

        id = in.readInt("id");
        name = in.read("name");
        logo = in.read("logo");
        setBaseGunner(in.readInt("baseGunner"));
        setBasePilot(in.readInt("basePilot"));
        factionColor = in.read("factionColor");

        factionPlayerColors = in.read("factionPlayerColor");

        abbreviation = in.read("abbreviation");
        conquerable = in.readBoolean("conquerable");

        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitComponentMod(type, weight, in.readInt(String.format("componentMod%s%s", type, weight)));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitPriceMod(type, weight, in.readInt(String.format("priceMod%s%s", type, weight)));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitFluMod(type, weight, in.readInt(String.format("fluMod%s%s", type, weight)));
            }
        }

        int size = in.readInt("faction_banned_ammo_size");
        for (; size > 0; size--) {
            String munition = in.read("munition");
            AmmoType.Munitions mun = AmmoType.Munitions.valueOf(munition);
            bannedAmmo.add(mun);
        }

        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            basePilotSkills.set(pos, in.read("factionBasePilotSkill"));
        }

        this.setTechLevel(in.readInt("techLevel"));
        this.setHouseDefectionFrom(in.readBoolean("defectFrom"));
        this.setHouseDefectionTo(in.readBoolean("defectTo"));
        this.setUsedMekBayMultiplier((float) in.readDouble("usedMekBayMultiplier"));

        size = in.readInt("sub_faction_size");

        this.subFactionList.clear();
        for (; size > 0; size--) {
            SubFaction subFaction = new SubFaction(in.read("SubFactionName"));
            subFaction.setConfig("AccessLevel", in.read("SubFactionAccessLevel"));
            for (int type = 0; type < Unit.MAX_BUILD; type++) {
                for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                    String setting = String.format("CanBuyNew%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
                    subFaction.setConfig(setting, in.read(setting));
                    setting = String.format("CanBuyUsed%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
                    subFaction.setConfig(setting, in.read(setting));
                }
            }
            subFaction.setConfig("MinELO", in.read("SubFactionMinELO"));
            subFaction.setConfig("MinExp", in.read("SubFactionMinExp"));
            this.subFactionList.put(subFaction.getConfig("Name"), subFaction);
        }

    }

    /**
     * Sets the component-cost modifier this faction applies to a given unit type/weight combination.
     *
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @param mod    the component-cost modifier to apply.
     */
    public void setHouseUnitComponentMod(int type, int weight, int mod) {
        this.factionUnitComponentMod[type][weight] = mod;
    }

    /**
     * Sets the price modifier this faction applies to a given unit type/weight combination.
     *
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @param mod    the price modifier to apply.
     */
    public void setHouseUnitPriceMod(int type, int weight, int mod) {
        this.factionUnitPriceMod[type][weight] = mod;
    }

    /**
     * Sets the influence/"flu" cost modifier this faction applies to a given unit type/weight combination.
     *
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @param mod    the influence-cost modifier to apply.
     */
    public void setHouseUnitFluMod(int type, int weight, int mod) {
        this.factionUnitFluMod[type][weight] = mod;
    }

    /**
     * @return the full per-build-type base gunnery skill vector (mutable — modifying it affects this House).
     */
    public Vector<Integer> getBaseGunnerVector() {
        return baseGunner;
    }

    /**
     * @param type the unit build type index.
     * @return the base gunnery skill for the given unit build type.
     */
    public int getBaseGunner(int type) {
        return baseGunner.elementAt(type);
    }

    /**
     * @return the full per-build-type base pilot-skill string vector (mutable — modifying it affects this
     *         House).
     */
    public Vector<String> getBasePilotSkillVector() {
        return basePilotSkills;
    }

    /**
     * @param type the unit build type index.
     * @return the base pilot-skill string for the given unit build type.
     */
    public String getBasePilotSkill(int type) {
        return basePilotSkills.elementAt(type);
    }

    /**
     * Sets the base pilot-skill string for a given unit build type.
     *
     * @param basePilotSkill The base piloting skill for unit <code>type</code> to set.
     * @param type           the unit build type index to update.
     */
    public void setBasePilotSkill(String basePilotSkill, int type) {
        synchronized (this.basePilotSkills) {
            this.basePilotSkills.set(type, basePilotSkill);
        }
    }

    /**
     * Sets the base gunnery skill for a given unit build type.
     *
     * @param baseGunner The baseGunner value to set.
     * @param type       the unit build type index to update.
     */
    public void setBaseGunner(int baseGunner, int type) {
        synchronized (this.baseGunner) {
            this.baseGunner.set(type, baseGunner);
        }
    }

    /**
     * @param type the unit build type index.
     * @return the base piloting skill for the given unit build type.
     */
    public int getBasePilot(int type) {
        return basePilot.elementAt(type);
    }

    /**
     * @return the full per-build-type base piloting skill vector (mutable — modifying it affects this
     *         House).
     */
    public Vector<Integer> getBasePilotVector() {
        return basePilot;
    }

    /**
     * Sets the base piloting skill for a given unit build type.
     *
     * @param basePilot The basePilot value to set.
     * @param type      the unit build type index to update.
     */
    public void setBasePilot(int basePilot, int type) {
        synchronized (this.basePilot) {
            this.basePilot.set(type, basePilot);
        }
    }

    /**
     * @return Returns the faction's abbreviation (short display code).
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * @param myAbbreviation The abbreviation to set.
     */
    public void setAbbreviation(String myAbbreviation) {
        abbreviation = myAbbreviation;
    }

    /**
     * @return Returns whether planets owned by this faction can be conquered by others.
     */
    public boolean isConquerable() {
        return conquerable;
    }

    /**
     * @param conquerable whether planets owned by this faction can be conquered by others.
     */
    public void setConquerable(boolean conquerable) {
        this.conquerable = conquerable;
    }

    /**
     * @return Returns the faction's display color as an HTML hex string (e.g. "#000000").
     */
    public String getHouseColor() {
        return factionColor;
    }

    /**
     * @param factionColor The display color (HTML hex string) to set.
     */
    public void setHouseColor(String factionColor) {
        this.factionColor = factionColor;
    }

    /**
     * @return Returns the faction's logo (image file reference).
     */
    public String getLogo() {
        return logo;
    }

    /**
     * @param logo The logo (image file reference) to set.
     */
    public void setLogo(String logo) {
        this.logo = logo;
    }

    /**
     * @return Returns the name of the "flu"/flavor-text file used for this faction (defaults to "Common").
     */
    public String getHouseFluFile() {
        return factionFluFile;
    }

    /**
     * @param factionFlu The flavor-text file name to set.
     */
    public void setHouseFluFile(String factionFlu) {
        this.factionFluFile = factionFlu;
    }

    /**
     * @return Returns the faction's display name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The display name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return an HTML anchor linking to a client-side command that displays this faction's info.
     */
    public String getNameAsLink() {
        return String.format("<a href=\"MEKWARS/c faction#%s\">%s</a>", name, name);
    }

    /**
     * @return Returns the faction id, or -1 if the id has not been set (id is null).
     */
    public int getId() {
        if (id == null) {
            return -1;
        }

        return id;
    }

    /**
     * Sets the faction id.
     *
     * @param id The id to set.
     *           <p>
     *                                                                                                                                                                                                                                                                                                   TODO This is only a hack and should ONLY be used by experienced personnel!
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the database row id for this faction.
     */
    public int getDBId() {
        return dbId;
    }

    /**
     * @param id the database row id to set.
     */
    public void setDBId(int id) {
        dbId = id;
    }

    /**
     * Writes this faction's full state (identity, base skills, colors, abbreviation, per unit-type/weight
     * cost modifiers, banned ammo, tech level, defection policy, used-Mek-bay multiplier, and all of its
     * {@link SubFaction}s) to a binary stream, in the same order expected by {@link #House(BinReader)}.
     *
     * @param out the binary stream writer.
     */
    public void binOut(BinWriter out) {

        out.println(id, "id");
        out.println(name, "name");
        out.println(logo, "logo");
        out.println(getBaseGunner(), "baseGunner");
        out.println(getBasePilot(), "basePilot");
        out.println(factionColor, "factionColor");

        out.println(factionPlayerColors, "factionPlayerColor");

        out.println(abbreviation, "abbreviation");
        out.println(conquerable, "conquerable");

        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                out.println(this.getHouseUnitComponentMod(type, weight), String.format("componentMod%s%s", type, weight));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                out.println(this.getHouseUnitPriceMod(type, weight), String.format("priceMod%s%s", type, weight));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                out.println(this.getHouseUnitFluMod(type, weight), String.format("fluMod%s%s", type, weight));
            }
        }

        out.println(this.getBannedAmmo().size(), "faction_banned_ammo_size");
        for (AmmoType.Munitions munition : this.getBannedAmmo()) {
            out.println(munition.ordinal(), "munition");
        }

        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            out.println(basePilotSkills.elementAt(pos), "factionBasePilotSkill");
        }

        out.println(this.getTechLevel(), "techLevel");
        out.println(this.getHouseDefectionFrom(), "defectFrom");
        out.println(this.getHouseDefectionTo(), "defectTo");
        out.println(this.getUsedMekBayMultiplier(), "usedMekBayMultiplier");

        out.println(this.getSubFactionList().size(), "subfactionsize");

        for (SubFaction subFaction : this.getSubFactionList().values()) {
            out.println(subFaction.getConfig("Name"), "SubFactionName");
            out.println(subFaction.getConfig("AccessLevel"), "SubFactionAccessLevel");
            for (int type = 0; type < Unit.MAX_BUILD; type++) {
                for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                    String setting = String.format("CanBuyNew%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
                    out.println(subFaction.getConfig(setting), setting);
                    setting = String.format("CanBuyUsed%s%s", Unit.getWeightClassDesc(weight), Unit.getTypeClassDesc(type));
                    out.println(subFaction.getConfig(setting), setting);
                }
            }
            out.println(subFaction.getConfig("MinELO"), "SubFactionMinELO");
            out.println(subFaction.getConfig("MinExp"), "SubFactionMinExp");
        }
    }

    /**
     * @return Returns the base gunnery skill for build-type index 0 (the "default"/first unit type).
     */
    public int getBaseGunner() {
        return baseGunner.elementAt(0);
    }

    /**
     * Sets the base gunnery skill for build-type index 0 (the "default"/first unit type).
     *
     * @param baseGunner The baseGunner value to set.
     */
    public void setBaseGunner(int baseGunner) {
        synchronized (this.baseGunner) {
            this.baseGunner.set(0, baseGunner);
        }
    }

    /**
     * @return Returns the base piloting skill for build-type index 0 (the "default"/first unit type).
     */
    public int getBasePilot() {
        return basePilot.elementAt(0);
    }

    /**
     * Sets the base piloting skill for build-type index 0 (the "default"/first unit type).
     *
     * @param basePilot The basePilot value to set.
     */
    public void setBasePilot(int basePilot) {
        synchronized (this.basePilot) {
            this.basePilot.set(0, basePilot);
        }
    }

    /**
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @return the component-cost modifier this faction applies for the given unit type/weight.
     */
    public int getHouseUnitComponentMod(int type, int weight) {
        return factionUnitComponentMod[type][weight];
    }

    /**
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @return the price modifier this faction applies for the given unit type/weight.
     */
    public int getHouseUnitPriceMod(int type, int weight) {
        return this.factionUnitPriceMod[type][weight];
    }

    /**
     * @param type   the unit build type index.
     * @param weight the unit weight-class index.
     * @return the influence/"flu" cost modifier this faction applies for the given unit type/weight.
     */
    public int getHouseUnitFluMod(int type, int weight) {
        return this.factionUnitFluMod[type][weight];
    }

    /**
     * @return the set of ammunition/munition types this faction is forbidden from using.
     */
    public EnumSet<AmmoType.Munitions> getBannedAmmo() {
        return bannedAmmo;
    }

    /**
     * @return the maximum tech level ({@link TechConstants}) allowed for this faction's units.
     */
    public int getTechLevel() {
        return this.techLevel;
    }

    /**
     * Sets the maximum tech level allowed for this faction. Any level below
     * {@link TechConstants#T_INTRO_BOX_SET} is treated as invalid/too-restrictive and is coerced up to
     * {@link TechConstants#T_ALL} (i.e. no restriction) instead of being honored literally.
     *
     * @param level the desired tech level.
     */
    public void setTechLevel(int level) {
        if (level < TechConstants.T_INTRO_BOX_SET) {this.techLevel = TechConstants.T_ALL;} else {
            this.techLevel = level;
        }
    }

    /**
     * @return whether players may defect away from this faction.
     */
    public boolean getHouseDefectionFrom() {
        return allowDefectionsFrom;
    }

    /**
     * @param defection whether players may defect away from this faction.
     */
    public void setHouseDefectionFrom(boolean defection) {
        allowDefectionsFrom = defection;
    }

    /**
     * @return whether players may defect into this faction.
     */
    public boolean getHouseDefectionTo() {
        return allowDefectionsTo;
    }

    /**
     * @param defection whether players may defect into this faction.
     */
    public void setHouseDefectionTo(boolean defection) {
        allowDefectionsTo = defection;
    }

    /**
     * @return the multiplier applied to used-Mek bay costs for this faction.
     */
    public float getUsedMekBayMultiplier() {
        return this.usedMekBayMultiplier;
    }

    /**
     * @param mult the used-Mek bay cost multiplier to set.
     */
    public void setUsedMekBayMultiplier(float mult) {
        this.usedMekBayMultiplier = mult;
    }

    /**
     * @return this faction's {@link SubFaction}s, keyed by sub-faction name (mutable map — modifying it
     *         affects this House).
     */
    public ConcurrentHashMap<String, SubFaction> getSubFactionList() {
        return subFactionList;
    }

    /**
     * Sets this faction's player display color, normalizing the value to always start with "#" (an HTML
     * hex color). If the given string doesn't already start with "#", one is prepended.
     *
     * @param factionPlayerColor the color string to set (with or without a leading "#").
     */
    public void setHousePlayerColors(String factionPlayerColor) {
        if (factionPlayerColor.startsWith("#")) {
            this.factionPlayerColors = factionPlayerColor;
        } else {
            this.factionPlayerColors = String.format("#%s", factionPlayerColor);
        }
    }

    /**
     * @return this faction's player display color as an HTML hex string.
     */
    public String getHousePlayerColor() {
        return this.factionPlayerColors;
    }

    /**
     * Marks a unit (by file name) as supported by this faction, incrementing a reference count if it was
     * already supported, or adding it with count 1 otherwise. Blank/whitespace-only names are ignored.
     *
     * @param fileName the unit file name to add/increment.
     */
    public void addUnitSupported(String fileName) {
        if (fileName.trim().isEmpty()) {
            return;
        }

        fileName = fileName.trim();

        if (houseSupportsUnit(fileName)) {
            int num = getSupportedUnits().get(fileName);
            supportedUnits.put(fileName, num + 1);
        } else {
            supportedUnits.put(fileName, 1);
        }
    }

    /**
     * Checks whether this faction supports the given unit file name.
     * <p>
     * <b>Quirk/bug:</b> the intended truncation logic ({@code fileName.indexOf("") > 0}) never triggers,
     * because {@link String#indexOf(String)} with an empty search string always returns 0 (never a
     * positive index) unless called on... actually it always returns 0 for an empty needle, so this
     * condition is always false and the substring/truncation branch is unreachable dead code. The method
     * therefore always checks the full, untruncated {@code fileName} against {@link #supportedUnits}.
     *
     * @param fileName the unit file name to check.
     * @return true if this faction supports the given unit file name.
     */
    public boolean houseSupportsUnit(String fileName) {
        if (fileName.indexOf("") > 0) {
            fileName = fileName.substring(0, fileName.indexOf(""));
        }

        return supportedUnits.containsKey(fileName);
    }

    /**
     * @return the map of unit file names supported by this faction to their reference counts.
     */
    public ConcurrentHashMap<String, Integer> getSupportedUnits() {
        return supportedUnits;
    }

    /**
     * Decrements a unit's support reference count, removing it entirely once the count reaches 1 (i.e.
     * the last reference). Blank/whitespace-only names and unsupported units are ignored.
     *
     * @param fileName the unit file name to decrement/remove.
     */
    public void removeUnitSupported(String fileName) {
        if (fileName.trim().isEmpty()) {
            return;
        }

        fileName = fileName.trim();

        if (houseSupportsUnit(fileName)) {
            int num = supportedUnits.get(fileName);
            if (num == 1) {
                // Remove it from the HashMap
                supportedUnits.remove(fileName);
            } else {
                supportedUnits.put(fileName, num - 1);
            }
        }
    }

    /**
     * @return whether units not native to this faction cost more to purchase.
     */
    public boolean getNonFactionUnitsCostMore() {
        return nonFactionUnitsCostMore;
    }

    /**
     * @param answer whether units not native to this faction should cost more to purchase.
     */
    public void setNonFactionUnitsCostMore(boolean answer) {
        nonFactionUnitsCostMore = answer;
    }

    /**
     * Builds a "|"-delimited summary string of this faction's core identity/settings (id, name, logo, base
     * gunnery/piloting skills, colors, abbreviation, conquerable flag, tech level, defection policy, and
     * used-Mek-bay multiplier). Used for legacy client/server messaging when announcing a new faction.
     *
     * @return the pipe-delimited summary string.
     */
    public String addNewHouse() {
        StringBuilder result = new StringBuilder();

        result.append(id);
        result.append("|");

        result.append(name);
        result.append("|");

        if (logo.trim().isEmpty()) {
            result.append(" ");
        } else {
            result.append(logo);
        }

        result.append("|");
        result.append(getBaseGunner());
        result.append("|");
        result.append(getBasePilot());
        result.append("|");
        result.append(factionColor);
        result.append("|");
        result.append(factionPlayerColors);
        result.append("|");
        result.append(abbreviation);
        result.append("|");
        result.append(conquerable);
        result.append("|");
        result.append(this.getTechLevel());
        result.append("|");
        result.append(this.getHouseDefectionFrom());
        result.append("|");
        result.append(this.getHouseDefectionTo());
        result.append("|");
        result.append(this.getUsedMekBayMultiplier());
        result.append("|");

        return result.toString();
    }
}
