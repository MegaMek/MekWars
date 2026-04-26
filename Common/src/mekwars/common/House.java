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
 * @author Helge Richter
 *
 */
public class House {

    public static final int RED_VALUE = 0;
    public static final int GREEN_VALUE = 1;
    public static final int BLUE_VALUE = 2;

    private String name = "none";
    private String logo = "";
    private String factionFluFile = "Common";

    private Integer id;
    private int dbId = 0;
    private final Vector<Integer> baseGunner = new Vector<>(Unit.MAX_BUILD, 1);
    private final Vector<Integer> basePilot = new Vector<>(Unit.MAX_BUILD, 1);
    private final Vector<String> basePilotSkills = new Vector<>(Unit.MAX_BUILD, 1);

    // private int factionPlayerColors[] = new int[3]; // [red,green,blue]

    private final int[][] factionUnitPriceMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]
    private final int[][] factionUnitFluMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]
    private final int[][] factionUnitComponentMod = new int[Unit.MAX_BUILD][4]; // [Type][Weight]

    private String factionColor = "#000000";
    private String abbreviation = "";
    private String factionPlayerColors = "#000000";

    private boolean conquerable = true;

    private final EnumSet<AmmoType.Munitions> bannedAmmo = EnumSet.noneOf(AmmoType.Munitions.class);
    private int techLevel = TechConstants.T_ALLOWED_ALL;
    private boolean allowDefectionsFrom = true;
    private boolean allowDefectionsTo = true;

    private final ConcurrentHashMap<String, SubFaction> subFactionList = new ConcurrentHashMap<>();

    public float usedMekBayMultiplier;

    public ConcurrentHashMap<String, Integer> supportedUnits = new ConcurrentHashMap<>();

    private boolean nonFactionUnitsCostMore = false;

    /**
     * @return Returns the baseGunner.
     */
    public int getBaseGunner() {
        return baseGunner.elementAt(0);
    }

    /**
     * @return baseGunner vector
     */
    public Vector<Integer> getBaseGunnerVector() {
        return baseGunner;
    }

    /**
     * @return Returns the baseGunner.
     */
    public int getBaseGunner(int type) {
        return baseGunner.elementAt(type);
    }

    /**
     * @return basePilotSkills vector
     */
    public Vector<String> getBasePilotSkillVect() {
        return basePilotSkills;
    }

    /**
     * @return Returns the basePilotSkill String.
     */
    public String getBasePilotSkill(int type) {
        return basePilotSkills.elementAt(type);
    }

    /**
     * @param baseGunner The baseGunner to set.
     */
    public void setBaseGunner(int baseGunner) {
        synchronized (this.baseGunner) {
            this.baseGunner.set(0, baseGunner);
        }
    }

    /**
     * @param basePilotSkill The base piloting skill for unit <code>type</code> to set.
     */
    public void setBasePilotSkill(String basePilotSkill, int type) {
        synchronized (this.basePilotSkills) {
            this.basePilotSkills.set(type, basePilotSkill);
        }
    }

    /**
     * @param baseGunner The baseGunner to set.
     */
    public void setBaseGunner(int baseGunner, int type) {
        synchronized (this.baseGunner) {
            this.baseGunner.set(type, baseGunner);
        }
    }

    /**
     * @return Returns the basePilot.
     */
    public int getBasePilot() {
        return basePilot.elementAt(0);
    }

    /**
     * @return Returns the basePilot.
     */
    public int getBasePilot(int type) {
        return basePilot.elementAt(type);
    }

    /**
     * @return basePilot vector
     */
    public Vector<Integer> getBasePilotVect() {
        return basePilot;
    }

    /**
     * @param basePilot The basePilot to set.
     */
    public void setBasePilot(int basePilot) {
        synchronized (this.basePilot) {
            this.basePilot.set(0, basePilot);
        }
    }

    /**
     * @param basePilot The basePilot to set.
     */
    public void setBasePilot(int basePilot, int type) {
        synchronized (this.basePilot) {
            this.basePilot.set(type, basePilot);
        }
    }

    /**
     * @return Returns the myAbbreviation.
     */
    public String getAbbreviation() {
        return abbreviation;
    }

    /**
     * @param myAbbreviation The myAbbreviation to set.
     */
    public void setAbbreviation(String myAbbreviation) {
        abbreviation = myAbbreviation;
    }

    /**
     * @return Returns the conquerable.
     */
    public boolean isConquerable() {
        return conquerable;
    }

    /**
     * @param conquerable The conquerable to set.
     */
    public void setConquerable(boolean conquerable) {
        this.conquerable = conquerable;
    }

    /**
     * @return Returns the factionColor.
     */
    public String getHouseColor() {
        return factionColor;
    }

    /**
     * @param factionColor The factionColor to set.
     */
    public void setHouseColor(String factionColor) {
        this.factionColor = factionColor;
    }

    /**
     * @return Returns the logo.
     */
    public String getLogo() {
        return logo;
    }

    /**
     * @param logo The logo to set.
     */
    public void setLogo(String logo) {
        this.logo = logo;
    }

    /**
     * @return Returns the logo.
     */
    public String getHouseFluFile() {
        return factionFluFile;
    }

    /**
     * @param factionFlu The logo to set.
     */
    public void setHouseFluFile(String factionFlu) {
        this.factionFluFile = factionFlu;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    public String getNameAsLink() {
        return "<a href=\"MEKWARS/c faction#" + name + "\">" + name + "</a>";
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        if (id == null) {
            return -1;
        }

        return id;
    }

    public int getDBId() {
        return dbId;
    }

    public void setDBId(int id) {
        dbId = id;
    }

    /**
     *
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
     * Constructor used for serialization
     */
    public House() {
        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            baseGunner.add(4);
            basePilot.add(5);
            basePilotSkills.add(" ");
        }
    }

    /**
     * Write itself to a binary stream.
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
                out.println(this.getHouseUnitComponentMod(type, weight), "componentMod" + type + weight);
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                out.println(this.getHouseUnitPriceMod(type, weight), "priceMod" + type + weight);
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                out.println(this.getHouseUnitFluMod(type, weight), "fluMod" + type + weight);
            }
        }

        out.println(this.getBannedAmmo().size(), "factionbannedammosize");
        for (String munition : this.getBannedAmmo().keySet()) {
            out.println(munition, "munition");
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
                    String setting = "CanBuyNew" +
                                           Unit.getWeightClassDesc(weight) +
                                           Unit.getTypeClassDesc(type);
                    out.println(subFaction.getConfig(setting), setting);
                    setting = "CanBuyUsed" +
                                    Unit.getWeightClassDesc(weight) +
                                    Unit.getTypeClassDesc(type);
                    out.println(subFaction.getConfig(setting), setting);
                }
            }
            out.println(subFaction.getConfig("MinELO"), "SubFactionMinELO");
            out.println(subFaction.getConfig("MinExp"), "SubFactionMinExp");
        }
    }

    /**
     * Read itself from a stream.
     */
    public House(BinReader in) throws IOException {

        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            baseGunner.add(4);
            basePilot.add(5);
            basePilotSkills.add(" ");
        }

        id = in.readInt("id");
        name = in.readLine("name");
        logo = in.readLine("logo");
        setBaseGunner(in.readInt("baseGunner"));
        setBasePilot(in.readInt("basePilot"));
        factionColor = in.readLine("factionColor");

        factionPlayerColors = in.readLine("factionPlayerColor");

        abbreviation = in.readLine("abbreviation");
        conquerable = in.readBoolean("conquerable");

        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitComponentMod(type, weight, in.readInt("componentMod" + type + weight));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitPriceMod(type, weight, in.readInt("priceMod" + type + weight));
            }
        }
        for (int type = 0; type < Unit.MAX_BUILD; type++) {
            for (int weight = 0; weight < 4; weight++) {
                this.setHouseUnitFluMod(type, weight, in.readInt("fluMod" + type + weight));
            }
        }

        int size = in.readInt("factionbannedammosize");
        for (; size > 0; size--) {bannedAmmo.put(in.readLine("munition"), "Banned");}

        for (int pos = 0; pos < Unit.MAX_BUILD; pos++) {
            basePilotSkills.set(pos, in.readLine("factionBasePilotSkill"));
        }

        this.setTechLevel(in.readInt("techLevel"));
        this.setHouseDefectionFrom(in.readBoolean("defectFrom"));
        this.setHouseDefectionTo(in.readBoolean("defectTo"));
        this.setUsedMekBayMultiplier((float) in.readDouble("usedMekBayMultiplier"));

        size = in.readInt("subfactionsize");

        this.subFactionList.clear();
        for (; size > 0; size--) {
            SubFaction subFaction = new SubFaction(in.readLine("SubFactionName"));
            subFaction.setConfig("AccessLevel", in.readLine("SubFactionAccessLevel"));
            for (int type = 0; type < Unit.MAX_BUILD; type++) {
                for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                    String setting = "CanBuyNew" +
                                           Unit.getWeightClassDesc(weight) +
                                           Unit.getTypeClassDesc(type);
                    subFaction.setConfig(setting, in.readLine(setting));
                    setting = "CanBuyUsed" +
                                    Unit.getWeightClassDesc(weight) +
                                    Unit.getTypeClassDesc(type);
                    subFaction.setConfig(setting, in.readLine(setting));
                }
            }
            subFaction.setConfig("MinELO", in.readLine("SubFactionMinELO"));
            subFaction.setConfig("MinExp", in.readLine("SubFactionMinExp"));
            this.subFactionList.put(subFaction.getConfig("Name"), subFaction);
        }

    }

    /**
     * @param id The id to set.
     *           <p>
     *                                                                                                                         TODO This is only a hack and should ONLY be used by experienced personnel!
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @see common.persistence.MMNetSerializable#binOut(common.persistence.TreeWriter)
     *
     * public void binOut(TreeWriter out) {
     *
     * out.write(id.intValue(), "id"); out.write(name, "name"); out.write(logo,
     * "logo"); out.write(baseGunner, "baseGunner"); out.write(basePilot,
     * "basePilot"); out.write(factionColor, "factionColor");
     *
     * out.write(factionPlayerColors,"factionPlayerColor");
     *
     * out.write(abbreviation, "abbreviation"); out.write(conquerable,
     * "conquerable");
     *
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * out.write(this.getHouseUnitComponentMod(type,weight),"componentMod"+type+weight);
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * out.write(this.getHouseUnitPriceMod(type,weight),"priceMod"+type+weight);
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * out.write(this.getHouseUnitFluMod(type,weight),"fluMod"+type+weight);
     *
     * out.write(this.getBannedAmmo().size(),"factionbannedammosize"); for
     * (String banned : this.getBannedAmmo().keySet())
     * out.write(banned,"munition");
     *
     * for( int pos = 0; pos < Unit.MAX_BUILD; pos++ ){
     * out.write(basePilotSkills.elementAt(pos),"factionBasePilotSkill"); }
     *  }
     *
     * /**
     * @see common.persistence.MMNetSerializable#binIn(common.persistence.TreeReader)
     *
     * public void binIn(TreeReader in, CampaignData dataProvider)throws
     * IOException {
     *
     * for( int pos = 0; pos < Unit.MAX_BUILD; pos++ ){ baseGunner.add(4);
     * basePilot.add(5); basePilotSkills.add(" "); }
     *
     * id = new Integer(in.readInt("id")); name =
     * HTML.br2cr(in.readString("name")); logo =
     * HTML.br2cr(in.readString("logo"));
     * setBaseGunner(in.readInt("baseGunner"));
     * setBasePilot(in.readInt("basePilot")); factionColor =
     * in.readString("factionColor");
     *
     * factionPlayerColors = in.readString("factionPlayerColor");
     *
     * abbreviation = in.readString("abbreviation"); conquerable =
     * in.readBoolean("conquerable");
     *
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * this.setHouseUnitComponentMod(type,weight,in.readInt("componentMod"+type+weight));
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * this.setHouseUnitPriceMod(type,weight,in.readInt("priceMod"+type+weight));
     * for ( int type = 0; type < 5; type++ ) for ( int weight = 0; weight < 4;
     * weight++)
     * this.setHouseUnitFluMod(type,weight,in.readInt("fluMod"+type+weight));
     *
     * int size = in.readInt("factionbannedammosize"); for( ; size > 0; size--)
     * BannedAmmo.put(in.readString("munition"),"Banned");
     *
     * for( int pos = 0; pos < Unit.MAX_BUILD; pos++ ){
     * basePilotSkills.set(pos,in.readString("factionBasePilotSkill")); } }
     */

    /**
     * @get the unit price mod for a faction
     */
    public int getHouseUnitPriceMod(int type, int weight) {
        return this.factionUnitPriceMod[type][weight];
    }

    /**
     * sets the unit price mod for a faction
     */
    public void setHouseUnitPriceMod(int type, int weight, int mod) {
        this.factionUnitPriceMod[type][weight] = mod;
    }

    /**
     * @get the unit price mod for a faction
     */
    public int getHouseUnitFluMod(int type, int weight) {
        return this.factionUnitFluMod[type][weight];
    }

    /**
     * sets the unit price mod for a faction
     */
    public void setHouseUnitFluMod(int type, int weight, int mod) {
        this.factionUnitFluMod[type][weight] = mod;
    }

    /**
     * gets the unit component mod for a faction
     */
    public int getHouseUnitComponentMod(int type, int weight) {
        return factionUnitComponentMod[type][weight];
    }

    /**
     * sets the unit component mod for a faction.
     */
    public void setHouseUnitComponentMod(int type, int weight, int mod) {
        this.factionUnitComponentMod[type][weight] = mod;
    }

    public void setHousePlayerColors(String factionPlayerColor) {
        if (factionPlayerColor.startsWith("#")) {this.factionPlayerColors = factionPlayerColor;} else {
            this.factionPlayerColors = "#" + factionPlayerColor;
        }
    }

    public String getHousePlayerColor() {
        return this.factionPlayerColors;
    }

    public EnumSet<AmmoType.Munitions> getBannedAmmo() {
        return bannedAmmo;
    }

    public void setTechLevel(int level) {
        if (level < TechConstants.T_INTRO_BOX_SET) {this.techLevel = TechConstants.T_ALL;} else {
            this.techLevel = level;
        }
    }

    public int getTechLevel() {
        return this.techLevel;
    }

    public boolean getHouseDefectionFrom() {
        return allowDefectionsFrom;
    }

    public void setHouseDefectionFrom(boolean defection) {
        allowDefectionsFrom = defection;
    }

    public boolean getHouseDefectionTo() {
        return allowDefectionsTo;
    }

    public void setHouseDefectionTo(boolean defection) {
        allowDefectionsTo = defection;
    }

    public void setUsedMekBayMultiplier(float mult) {
        this.usedMekBayMultiplier = mult;
    }

    public float getUsedMekBayMultiplier() {
        return this.usedMekBayMultiplier;
    }

    public ConcurrentHashMap<String, SubFaction> getSubFactionList() {
        return subFactionList;
    }

    public boolean houseSupportsUnit(String fileName) {
        if (fileName.indexOf("") > 0) {fileName = fileName.substring(0, fileName.indexOf(""));}
        return supportedUnits.containsKey(fileName);
    }

    public ConcurrentHashMap<String, Integer> getSupportedUnits() {
        return supportedUnits;
    }

    public void addUnitSupported(String fileName) {
        if (fileName.trim().isEmpty()) {return;}
        fileName = fileName.trim();
        if (houseSupportsUnit(fileName)) {
            int num = getSupportedUnits().get(fileName);
            supportedUnits.put(fileName, num + 1);
        } else {
            supportedUnits.put(fileName, 1);
        }
    }

    public void removeUnitSupported(String fileName) {
        if (fileName.trim().isEmpty()) {return;}
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

    public boolean getNonFactionUnitsCostMore() {
        return nonFactionUnitsCostMore;
    }

    public void setNonFactionUnitsCostMore(boolean answer) {
        nonFactionUnitsCostMore = answer;
    }

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
