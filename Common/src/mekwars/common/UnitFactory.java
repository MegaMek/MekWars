/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author - Helge Richter (McWizard)
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

import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;

import mekwars.common.campaign.operations.Operation;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * Represents a unit-production factory (typically owned/located by a faction on a planet) within a MekWars
 * campaign. A factory has a faction owner ("founder"), a size/weight class, a build-type bitmask describing which
 * categories of units it is capable of producing, a countdown of ticks until its next unit is ready, and
 * lock/access-level controls used by admins and raid mechanics (see {@link #canBeRaided}).
 * <p>
 * Instances are persisted using MekWars' custom binary format via {@link #binOut(BinWriter)} and
 * {@link #binIn(BinReader)}.
 *
 * @author Helge Richter
 */

public class UnitFactory implements Serializable {

    /**
     * The {@code BUILD_*} constants below encode which unit categories a factory can produce as a bitmask, where
     * each named "base" category contributes one bit: Mek = 1, Vehicle = 2, Infantry = 4, ProtoMek = 8,
     * BattleArmor = 16. The combination constants (e.g. {@link #BUILD_MEK_AND_VEHICLES} = 3) are simply the OR of
     * their component bits, and {@link #getType()}/{@link #setType(int)} store one such combined value directly
     * (not a raw bitmask assembled by callers) — {@link #canProduce(int)} decodes it back into individual
     * categories by successive subtraction.
     * <p>
     * {@link #BUILD_VTOL} (32) and {@link #BUILD_AERO} (33) break this pattern: they are not additional bits that
     * combine with the others (the combination range only goes up to 31 = all five base bits set), and
     * {@code BUILD_AERO} (33) is not a power of two. As a result, a factory whose type is {@code BUILD_VTOL} (32)
     * is decoded by {@link #canProduce(int)} as equivalent to {@code BUILD_BATTLEARMOR} (16) plus leftover 16,
     * since {@code 32 - BUILD_AERO(33)} is negative but {@code 32 - BUILD_BATTLEARMOR(16) = 16 >= 0} — i.e. a VTOL
     * factory is (incorrectly) treated as able to produce BattleArmor, and {@code canProduce(Unit.VTOL)} is never
     * actually true for any factory (VTOL is not checked anywhere in {@link #canProduce}). This looks like a
     * pre-existing bug in the type-encoding scheme, left as-is here (see {@link #canProduce(int)} for details).
     */
    static public final int BUILD_ALL = 0;
    static public final int BUILD_MEK = 1;
    static public final int BUILD_VEHICLES = 2;
    static public final int BUILD_MEK_AND_VEHICLES = 3;
    static public final int BUILD_INFANTRY = 4;
    static public final int BUILD_MEK_AND_INFANTRY = 5;
    static public final int BUILD_VEHICLES_AND_INFANTRY = 6;
    static public final int BUILD_MEK_AND_INFANTRY_AND_VEHICLES = 7;
    static public final int BUILD_PROTOMEKS = 8;
    static public final int BUILD_MEK_AND_PROTOMEKS = 9;
    static public final int BUILD_VEHICLES_AND_PROTOMEK = 10;
    static public final int BUILD_MEK_AND_VEHICLES_AND_PROTOMEK = 11;
    static public final int BUILD_INFANTRY_AND_PROTOMEK = 12;
    static public final int BUILD_MEK_AND_INFANTRY_AND_PROTOMEK = 13;
    static public final int BUILD_VEHICLES_AND_INFANTRY_AND_PROTOMEK = 14;
    static public final int BUILD_MEK_AND_VEHICLES_AND_INFANTRY_AND_PROTOMEK = 15;
    static public final int BUILD_BATTLEARMOR = 16;
    static public final int BUILD_MEK_AND_BATTLEARMOR = 17;
    static public final int BUILD_VEHICLES_AND_BATTLEARMOR = 18;
    static public final int BUILD_MEK_AND_VEHICLES_AND_nBATTLEARMOR = 19;
    static public final int BUILD_INFANTRY_AND_BATTLEARMOR = 20;
    static public final int BUILD_MEK_AND_INFANTRY_AND_BATTLEARMOR = 21;
    static public final int BUILD_VEHICLES_AND_INFANTRY_AND_BATTLEARMOR = 22;
    static public final int BUILD_MEK_AND_VEHICLES_AND_INFANTRY_AND_BATTLEARMOR = 23;
    static public final int BUILD_PROTOMEKS_AND_BATTLEARMOR = 24;
    static public final int BUILD_MEK_AND_PROTOMEKS_AND_BATTLEARMOR = 25;
    static public final int BUILD_VEHICLES_AND_PROTOMEK_AND_BATTLEARMOR = 26;
    static public final int BUILD_MEK_AND_VEHICLES_AND_PROTOMEK_AND_BATTLEARMOR = 27;
    static public final int BUILD_INFANTRY_AND_PROTOMEK_AND_BATTLEARMOR = 28;
    static public final int BUILD_MEK_AND_INFANTRY_AND_PROTOMEK_AND_BATTLEARMOR = 29;
    static public final int BUILD_VEHICLES_AND_INFANTRY_AND_PROTOMEK_AND_BATTLEARMOR = 30;
    static public final int BUILD_MEK_AND_VEHICLES_AND_INFANTRY_AND_PROTOMEK_AND_BATTLEARMOR = 31;
    /** Factory type dedicated to VTOLs. See the class-level note above on {@code canProduce}'s handling of this. */
    static public final int BUILD_VTOL = 32;
    /** Factory type dedicated to aerospace units. Not part of the additive bitmask range (see note above). */
    static public final int BUILD_AERO = 33;
    /**
     * Serialization identifier for this {@link Serializable} class.
     */
    @Serial
    private static final long serialVersionUID = -5221016867627976085L;
    /** Display name of this factory. */
    private String name;
    /** Weight/size class label of this factory (e.g. "Light", "Medium", "Heavy", "Assault"); see {@link #getWeightclass()}. */
    private String size;
    /** Name of the faction that founded/owns this factory. */
    private String founder;
    /** Countdown of ticks remaining before this factory can produce its next unit. */
    private int ticksUntilRefresh;
    private int refreshSpeed = 100;//The Speed this factory refreshes
    /**
     * Type = 0 means can produce everything Least significant bit = Mek Next Bit = Vehicle Next Bit = Infantry
     */
    private int type;

    /**
     * <p>
     * This will allow admins to lock this factory
     */
    private boolean factoryLocked = false;

    /** Unique identifier string for this factory. */
    private String factoryID = "";
    //private int factoryID = 0;

    /** Minimum player access level required to interact with/use this factory. */
    private int factoryAccessLevel = 0;
    /** Sub-folder (under "standard") holding the build table used by this factory; empty means the standard table. */
    private String buildTableFolder = "";

    /**
     * @return Returns the factoryID
     */
    public String getID() {
        return factoryID;
    }

    /**
     * @param id The factoryID to set.
     */
    public void setID(String id) {
        this.factoryID = id;
    }

    /**
     * @return Returns the refreshSpeed.
     */
    public int getRefreshSpeed() {
        return refreshSpeed;
    }

    /**
     * @param refreshSpeed The refreshSpeed to set.
     */
    public void setRefreshSpeed(int refreshSpeed) {
        this.refreshSpeed = refreshSpeed;
    }

    /**
     * See if this factory can be raided by the particular operation
     * <p>
     * 13 Sept 2011 - Cord Awtry
     *
     * @param type_id the {@code Unit} type constant to check (e.g. {@link Unit#MEK}).
     * @param o       the operation whose "ForceProduceAndCapture*" flags gate which unit types may be captured.
     *
     * @return {@code true} if the operation allows capturing units of {@code type_id} and this factory is able to
     *       produce that type (or the factory is {@link #BUILD_ALL}, which always returns {@code true} as long as
     *       the operation permits capturing at least one category).
     */
    public boolean canBeRaided(int type_id, Operation o) {
        boolean capMeks = o.getBooleanValue("ForceProduceAndCaptureMeks");
        boolean capVees = o.getBooleanValue("ForceProduceAndCaptureVees");
        boolean capInfs = o.getBooleanValue("ForceProduceAndCaptureInfs");
        boolean capProtos = o.getBooleanValue("ForceProduceAndCaptureProtos");
        boolean capBAs = o.getBooleanValue("ForceProduceAndCaptureBAs");
        boolean capAeros = o.getBooleanValue("ForceProduceAndCaptureAeros");

        boolean canRaidAnything = capMeks || capVees || capInfs || capProtos || capBAs || capAeros;

        if (!canRaidAnything) {
            return false;
        }

        if (getType() == BUILD_ALL) {
            return true;
        }

        return switch (type_id) {
            case Unit.MEK -> capMeks && canProduce(type_id);
            case Unit.VEHICLE -> capVees && canProduce(type_id);
            case Unit.INFANTRY -> capInfs && canProduce(type_id);
            case Unit.PROTOMEK -> capProtos && canProduce(type_id);
            case Unit.BATTLEARMOR -> capBAs && canProduce(type_id);
            case Unit.AERO -> capAeros && canProduce(type_id);
            default -> false;
        };

    }

    /**
     * @return Returns the type.
     */
    public int getType() {
        return type;
    }

    /**
     * @param type The type to set. Values outside the valid {@code [BUILD_ALL, BUILD_AERO]} range silently fall
     *             back to {@link #BUILD_MEK} rather than being rejected.
     */
    public void setType(int type) {

        if (type < BUILD_ALL || type > BUILD_AERO) {this.type = BUILD_MEK;} else {this.type = type;}
    }

    /**
     * Test whether the factory can produce an unit.
     * <p>
     * Decodes the combined {@link #getType()} value back into individual unit categories by successively
     * subtracting {@link #BUILD_AERO}, {@link #BUILD_BATTLEARMOR}, {@link #BUILD_PROTOMEKS},
     * {@link #BUILD_INFANTRY}, {@link #BUILD_VEHICLES} and finally {@link #BUILD_MEK} (in that order) whenever the
     * remainder is still non-negative. See the class-level note on the {@code BUILD_*} constants for why this
     * produces an incorrect result for {@link #BUILD_VTOL} (32): it is treated as
     * {@code BUILD_BATTLEARMOR}-capable, and {@code Unit.VTOL} can never match here.
     *
     * @param type_id The type of the unit to test.
     *
     * @return {@code true} if this factory's type includes {@code type_id}, or unconditionally {@code true} if
     *       this factory's type is {@link #BUILD_ALL}.
     */
    public boolean canProduce(int type_id) {
        int test = getType();

        if (test == BUILD_ALL) {return true;}

        if (test - BUILD_AERO >= 0) {
            test -= BUILD_AERO;
            if (type_id == Unit.AERO) {
                return true;
            }
        }

        if (test - BUILD_BATTLEARMOR >= 0) {

            test -= BUILD_BATTLEARMOR;
            if (type_id == Unit.BATTLEARMOR) {return true;}
        }

        if (test - BUILD_PROTOMEKS >= 0) {

            test -= BUILD_PROTOMEKS;
            if (type_id == Unit.PROTOMEK) {return true;}
        }


        if (test - BUILD_INFANTRY >= 0) {
            test -= BUILD_INFANTRY;
            if (type_id == Unit.INFANTRY) {return true;}
        }

        if (test - BUILD_VEHICLES >= 0) {
            test -= BUILD_VEHICLES;
            if (type_id == Unit.VEHICLE) {return true;}
        }

        if (test - BUILD_MEK >= 0) {
            return type_id == Unit.MEK;
        }

        return false;
    }

    /**
     * Writes as a binary stream
     *
     * @param out the writer to serialize this factory's fields to, in a fixed field order matched by
     *            {@link #binIn(BinReader)}.
     */
    public void binOut(BinWriter out) {
        out.println(name, "name");
        out.println(size, "size");
        out.println(founder, "faction");
        out.println(ticksUntilRefresh, "ticksUntilRefresh");
        out.println(refreshSpeed, "refreshSpeed");
        out.println(type, "type");
        out.println(factoryLocked, "factorylock");
        out.println(factoryAccessLevel, "factoryaccess");
        out.println(buildTableFolder, "buildtablefolder");
        out.println(factoryID, "factoryID");
    }

    /**
     * Read from a binary stream
     *
     * @param in the reader to populate this factory's fields from; must have been written by
     *           {@link #binOut(BinWriter)} in the same field order.
     *
     * @throws IOException if the underlying stream fails or the data is malformed.
     */
    public void binIn(BinReader in) throws IOException {
        name = in.read("name");
        size = in.read("size");
        founder = in.read("faction");
        ticksUntilRefresh = in.readInt("ticksUntilRefresh");
        refreshSpeed = in.readInt("refreshSpeed");
        type = in.readInt("type");
        factoryLocked = in.readBoolean("factorylock");
        factoryAccessLevel = in.readInt("factoryaccess");
        buildTableFolder = in.read("buildtablefolder");
        factoryID = in.read("factoryID");
    }

    /**
     * @return a short abbreviation string built from single letters (M/V/I/P/B/A) for each unit category this
     *       factory can produce, in Mek/Vehicle/Infantry/ProtoMek/BattleArmor/Aero order.
     */
    public String getTypeString() {
        String result = "";
        if (this.canProduce(Unit.MEK)) {result += "M";}
        if (this.canProduce(Unit.VEHICLE)) {result += "V";}
        if (this.canProduce(Unit.INFANTRY)) {result += "I";}
        if (this.canProduce(Unit.PROTOMEK)) {result += "P";}
        if (this.canProduce(Unit.BATTLEARMOR)) {result += "B";}
        if (this.canProduce(Unit.AERO)) {result += "A";}

        return result;
    }

    //TODO: Fix the unit type system and all that stuff.. this is a big bunch of garbage..
    /**
     * @return a human-readable, space-separated list of full unit-category names (e.g. "Mek Vehicle ") this
     *       factory can produce. Functionally identical to {@link #typeString()}.
     */
    public String getFullTypeString() {
        String result = "";
        if (this.canProduce(Unit.MEK)) {result = "Mek ";}
        if (this.canProduce(Unit.VEHICLE)) {result += "Vehicle ";}
        if (this.canProduce(Unit.INFANTRY)) {result += "Infantry ";}
        if (this.canProduce(Unit.PROTOMEK)) {result += "ProtoMek ";}
        if (this.canProduce(Unit.BATTLEARMOR)) {result += "BattleArmor ";}
        if (this.canProduce(Unit.AERO)) {result += "Aero ";}
        return result;
    }

    /**
     * @return an HTML-formatted status blurb (name, size, founder, and either "ready to produce" or the remaining
     *       ticks) shown on a detailed planet view.
     */
    public String getStatus() {
        String result = getName() + "(" + getSize();
        if (getType() != Unit.MEK) {result += " " + typeString();}
        result += ") built by " + getFounder() + ".<br>";
        if (getTicksUntilRefresh() == 0) {result += "Factory is ready to produce a unit.<br>";} else {
            result += "Factory will be ready to produce a unit in " + getTicksUntilRefresh() + " miniticks.<br>";
        }
        return result;
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
     * @return Returns the size.
     */
    public String getSize() {
        return size;
    }

    /**
     * @param size The size to set.
     */
    public void setSize(String size) {
        this.size = size;
    }

    /**
     * Returns the name of all types this factory can produce seperated by space.
     */
    public String typeString() {
        String result = "";
        if (canProduce(Unit.MEK)) {result += "Mek ";}
        if (canProduce(Unit.VEHICLE)) {result += "Vehicle ";}
        if (canProduce(Unit.INFANTRY)) {result += "Infantry ";}
        if (this.canProduce(Unit.PROTOMEK)) {result += "ProtoMek ";}
        if (this.canProduce(Unit.BATTLEARMOR)) {result += "BattleArmor ";}
        if (this.canProduce(Unit.AERO)) {result += "Aero ";}
        return result;
    }

    /**
     * @return Returns the faction .
     */
    public String getFounder() {
        return founder;
    }

    /**
     * @param faction The faction to set.
     */
    public void setFounder(String faction) {
        this.founder = faction;
    }

    /**
     * @return {@link Integer#MAX_VALUE} if this factory is {@link #isLocked() locked} (so it never appears ready);
     *       otherwise the raw {@code ticksUntilRefresh} value, clamped to never report a negative countdown.
     */
    public int getTicksUntilRefresh() {
        if (isLocked()) {
            return Integer.MAX_VALUE;
        }

        return Math.max(ticksUntilRefresh, 0);
        //else
    }

    /**
     * @param ticksUntilRefresh The ticksUntilRefresh to set.
     */
    public void setTicksUntilRefresh(int ticksUntilRefresh) {
        this.ticksUntilRefresh = ticksUntilRefresh;
    }

	/*public void binOut(TreeWriter out) {
		out.write(getName(), "name");
		out.write(getSize(), "size");
		out.write(getFounder(), "founder");
		out.write(getTicksUntilRefresh(), "ticksuntilrefresh");
		out.write(getRefreshSpeed(), "refreshspeed");
		out.write(getType(),"type");
		out.write(isLocked(), "factorylock");
	}*/

    /**
     * @return {@code true} if an admin has locked this factory, preventing it from producing units (see
     *       {@link #getTicksUntilRefresh()}).
     */
    public boolean isLocked() {
        return factoryLocked;
    }

    /*for serializable
    public void binIn(TreeReader in, CampaignData data){
        //empty. todo.
    }
    */
    /**
     * @return the {@code Unit} weight-class constant ({@link Unit#LIGHT}, {@link Unit#MEDIUM}, {@link Unit#HEAVY},
     *       {@link Unit#ASSAULT}) corresponding to this factory's {@link #getSize()} label, or {@code 0} if the
     *       size string doesn't match any known weight class.
     */
    public int getWeightclass() {
        if (getSize().equalsIgnoreCase("Light")) {return Unit.LIGHT;} else if (getSize().equalsIgnoreCase("Medium")) {
            return Unit.MEDIUM;
        } else if (getSize().equalsIgnoreCase("Heavy")) {
            return Unit.HEAVY;
        } else if (getSize().equalsIgnoreCase("Assault")) {return Unit.ASSAULT;}
        return 0;
    }

    /**
     * @return the first unit-category constant this factory can produce, checked in this fixed preference order:
     *       {@link Unit#MEK}, {@link Unit#VEHICLE}, {@link Unit#AERO}, {@link Unit#BATTLEARMOR},
     *       {@link Unit#PROTOMEK}, {@link Unit#INFANTRY}; defaults to {@link Unit#MEK} if none match (which also
     *       covers the {@link #BUILD_VTOL} case, since {@code canProduce(Unit.VTOL)} is never checked here and
     *       never true regardless — see the class-level note on the {@code BUILD_*} constants).
     */
    public int getBestTypeProducable() {
        if (this.canProduce(Unit.MEK)) {return Unit.MEK;}
        if (this.canProduce(Unit.VEHICLE)) {return Unit.VEHICLE;}
        if (this.canProduce(Unit.AERO)) {return Unit.AERO;}
        if (this.canProduce(Unit.BATTLEARMOR)) {return Unit.BATTLEARMOR;}
        if (this.canProduce(Unit.PROTOMEK)) {return Unit.PROTOMEK;}
        if (this.canProduce(Unit.INFANTRY)) {return Unit.INFANTRY;}
        return Unit.MEK;
    }

    /**
     * @param lock the locked state to set for this factory (admin control).
     */
    public void setLock(boolean lock) {
        factoryLocked = lock;
    }

    /** @return the minimum player access level required to use this factory. */
    public int getAccessLevel() {
        return factoryAccessLevel;
    }

    /** @param access the minimum player access level to require. */
    public void setAccessLevel(int access) {
        this.factoryAccessLevel = access;
    }

    /**
     * @return the resolved path to this factory's build table folder: {@code "standard"} if no custom sub-folder
     *       is configured, otherwise {@code "standard" + File.separatorChar + buildTableFolder}.
     */
    public String getBuildTableFolder() {

        if (buildTableFolder.trim().isEmpty()) {return "standard";}

        return "standard" + File.separatorChar + buildTableFolder.trim();
    }

    /**
     * Sets the custom build-table sub-folder for this factory.
     * <p>
     * No-ops (leaves {@link #buildTableFolder} unchanged) if {@code folder} is exactly {@code "0"} or
     * {@code "standard"}. Otherwise strips a literal {@code "standard/" + File.separatorChar} prefix (note: this
     * uses {@link String#replaceAll(String, String)}, so the {@code "/"} and {@link File#separatorChar} are
     * treated as a regex, which can behave unexpectedly on platforms where the separator is a regex metacharacter
     * such as {@code \}), then clears the value entirely if what remains is exactly {@code "standard"}.
     *
     * @param folder the sub-folder name (or path fragment) to set.
     */
    public void setBuildTableFolder(String folder) {

        if (folder.equals("0") || folder.equals("standard")) {return;}

        buildTableFolder = folder.replaceAll("standard" + "/" + File.separatorChar, "");

        if (buildTableFolder.equals("standard")) {buildTableFolder = "";}
    }
}
