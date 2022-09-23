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

package server.campaign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;

import common.CampaignData;
import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.operations.Operation;
import common.campaign.pilot.skills.PilotSkill;
import common.campaign.targetsystems.TargetSystem;
import common.campaign.targetsystems.TargetTypeNotImplementedException;
import common.campaign.targetsystems.TargetTypeOutOfBoundsException;
import common.util.MWLogger;
import common.util.TokenReader;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.BattleArmor;
import megamek.common.Crew;
import megamek.common.CrewType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EntityListFile;
import megamek.common.Infantry;
import megamek.common.MULParser;
import megamek.common.Mech;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;
import megamek.common.Mounted;
import megamek.common.Tank;
import megamek.common.WeaponType;
import megamek.common.options.PilotOptions;
import server.campaign.pilot.SPilot;
import server.campaign.pilot.SPilotSkills;
import server.campaign.pilot.skills.SPilotSkill;
import server.campaign.pilot.skills.TraitSkill;
import server.campaign.pilot.skills.WeaponSpecialistSkill;
import server.campaign.util.SerializedMessage;
import server.util.QuirkHandler;

/**
 * A class representing an MM.Net Entity
 *
 * @author Helge Richter (McWizard) Jun 10/04 - Dave Poole added an overloaded
 *         constructor to allow creation of a new SUnit with the same UnitID as
 *         an existing Mech to facilitate repodding
 */

public final class SUnit extends Unit implements Comparable<SUnit> {

    // VARIABLES
    private Integer BV = 0;
    private Integer scrappableFor = -1;

    private long passesMaintainanceUntil = 0;
    private boolean pilotIsRepairing = false;

    private Entity unitEntity = null;
    private int lastCombatPilot = -1;

    // CONSTRUCTOR
    /**
     * For Serialization.
     */
    public SUnit() {
        super();
    }

    /**
     * Construct a new unit.
     *
     * @param p
     *            flavour string (es: Built by Kurita on An-Ting)
     * @param filename
     *            to read this entity from
     */
    public SUnit(String p, String Filename, int weightclass) {
        super();
        int gunnery = 4;
        int piloting = 5;

        SHouse house = CampaignMain.cm.getHouseFromPartialString(p, null);

        setUnitFilename(Filename);
        init();

        if (house != null) {
            setPilot(house.getNewPilot(getType()));
        } else {
            setPilot(new SPilot(SPilot.getRandomPilotName(CampaignMain.cm.getR()), gunnery, piloting));
        }

        setWeightclass(weightclass); // default weight class.

        setProducer(p);
        setId(CampaignMain.cm.getAndUpdateCurrentUnitID());

    }

    /**
     * Constructs a new Unit with the id for an existing unit (repod)
     *
     * @param p
     *            - flavour string (es: Built by Kurita on An-Ting)
     * @param Filename
     *            - filename to read this entity from
     * @param weightclass
     *            - int defining weightclass
     * @param replaceId
     *            - unitID to assign a new SUnit
     */
    public SUnit(int replaceId, String p, String Filename) {
        super();
        setUnitFilename(Filename);
        Entity ent = SUnit.loadMech(getUnitFilename());
        setEntity(ent);
        init();
        setPilot(new SPilot("Vacant", 99, 99));// only used for repods. A real
        // pilot is
        // transferred in later.
        setId(replaceId);
        setProducer(p);
        unitEntity = ent;
    }

    // STATIC METHODS
    /**
     * Method which checks a unit for illegal ammo and replaces it with default
     * ammo loads. useful for removing faction banned ammo from salvage. Note
     * that this is primarily designed to strip L2 ammo from L2 units (eg -
     * precision AC) and replace it with normal ammo. L3 ammos may lead to some
     * oddities and should be banned or allowed server wide rather than on a
     * house-by-house basis.
     *
     * @param u
     *            - unit to check
     * @param h
     *            - SHouse unit is joining
     */
    public static void checkAmmoForUnit(SUnit u, SHouse h) {

        Entity en = u.getEntity();
        int year = CampaignMain.cm.getIntegerConfig("CampaignYear");

        boolean wasChanged = false;

        for (Mounted mAmmo : en.getAmmo()) {

            AmmoType at = (AmmoType) mAmmo.getType();
            String munition = Long.toString(at.getMunitionType());

            if (at.getAmmoType() == AmmoType.T_ATM) {
                continue;
            }

            if (at.getAmmoType() == AmmoType.T_AC_LBX) {
                continue;
            }

            if (at.getAmmoType() == AmmoType.T_SRM_STREAK) {
                continue;
            }

            if (at.getAmmoType() == AmmoType.T_LRM_STREAK) {
                continue;
            }

            if (at.getAmmoType() == AmmoType.M_STANDARD) {
                continue;
            }

            if (CampaignMain.cm.getData().getServerBannedAmmo().containsKey(munition) || h.getBannedAmmo().containsKey(munition)) {

                Vector<AmmoType> types = AmmoType.getMunitionsFor(at.getAmmoType());
                Enumeration<AmmoType> allTypes = types.elements();

                boolean defaultFound = false;
                while (allTypes.hasMoreElements() && !defaultFound) {
                    AmmoType currType = allTypes.nextElement();

                    if ((currType.getTechLevel(year) <= en.getTechLevel()) && (currType.getMunitionType() == AmmoType.M_STANDARD) && (currType.getRackSize() == at.getRackSize())) {
                        mAmmo.changeAmmoType(currType);
                        if(mAmmo.byShot()) {
                        	mAmmo.setShotsLeft(mAmmo.getOriginalShots());
                        } else {
                        	mAmmo.setShotsLeft(at.getShots());
                        }
                        defaultFound = true;
                        wasChanged = true;
                    }
                }// end while
            }// end if(is banned)

        }
        if (wasChanged) {
            u.setEntity(en);
        }
    }

    /**
     * Method which determines whether or not a given unit may be sold on the
     * black market. Any "false" return prevents house listings as well as
     * player sales.
     */
    public static boolean mayBeSoldOnMarket(SUnit u) {

        if ((u.getType() == Unit.BATTLEARMOR) && !CampaignMain.cm.getBooleanConfig("BAMayBeSoldOnBM")) {
            return false;
        } else if ((u.getType() == Unit.PROTOMEK) && !CampaignMain.cm.getBooleanConfig("ProtosMayBeSoldOnBM")) {
            return false;
        } else if ((u.getType() == Unit.AERO) && !CampaignMain.cm.getBooleanConfig("AerosMayBeSoldOnBM")) {
            return false;
        } else if ((u.getType() == Unit.INFANTRY) && !CampaignMain.cm.getBooleanConfig("InfantryMayBeSoldOnBM")) {
            return false;
        } else if ((u.getType() == Unit.VEHICLE) && !CampaignMain.cm.getBooleanConfig("VehsMayBeSoldOnBM")) {
            return false;
        } else if (((u.getType() == Unit.MEK) || (u.getType() == Unit.QUAD)) && !CampaignMain.cm.getBooleanConfig("MeksMayBeSoldOnBM")) {
            return false;
        }

        return true;
    }

    /**
     * Return the number of techs/bays required for a unit of given size/type.
     */
    public static int getHangarSpaceRequired(int typeid, int weightclass, int baymod, String model, SHouse faction) {

        if (typeid == Unit.PROTOMEK) {
            return 0;
        }
        
        if ((typeid == Unit.INFANTRY) && CampaignMain.cm.getBooleanConfig("FootInfTakeNoBays")) {

            // check types
            boolean isFoot = model.startsWith("Foot");
            boolean isAMFoot = model.startsWith("Anti-Mech Foot");

            if (isFoot || isAMFoot) {
                return 0;
            }
        }

        int result = 1;
        String techAmount = "TechsFor" + Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(typeid);
        if (faction != null) {
            result = faction.getIntegerConfig(techAmount);
        } else {
            result = CampaignMain.cm.getIntegerConfig(techAmount);
        }

        if (!CampaignMain.cm.isUsingAdvanceRepair()) {
            // skill)
            result += baymod;
        }

        // no negative techs
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    public static int getHangarSpaceRequired(int typeid, int weightclass, int baymod, String model, boolean unitSupported, SHouse faction) {
        if (unitSupported) {
            return SUnit.getHangarSpaceRequired(typeid, weightclass, baymod, model, faction);
        }
        return (int) (SUnit.getHangarSpaceRequired(typeid, weightclass, baymod, model, faction) * CampaignMain.cm.getFloatConfig("NonFactionUnitsIncreasedTechs"));
    }

    /**
     * Pass-through method that gets the number of bays/techs required for a
     * given unit by drawing its characteristics and feeding them to
     * getHangarSpaceRequired(int,int,int,String).
     */
    public static int getHangarSpaceRequired(SUnit u, SHouse faction) {
        return SUnit.getHangarSpaceRequired(u.getType(), u.getWeightclass(), u.getPilot().getBayModifier(), u.getModelName(), faction);
    }

    public static int getHangarSpaceRequired(SUnit u, boolean unitSupported, SHouse faction) {
        if (unitSupported) {
            return SUnit.getHangarSpaceRequired(u.getType(), u.getWeightclass(), u.getPilot().getBayModifier(), u.getModelName(), faction);
        }
        return SUnit.getHangarSpaceRequired(u.getType(), u.getWeightclass(), u.getPilot().getBayModifier(), u.getModelName(), unitSupported, faction);
    }

    /**
     * Simple static method that access configs and returns a unit's influence
     * on map size. Called by ShortOperation when changing status from Waiting
     * -> In_Progress.
     *
     * @return - configured map weighting
     */
    public static int getMapSizeModification(SUnit u) {
        if (u.getType() == Unit.VEHICLE) {
            return CampaignMain.cm.getIntegerConfig("VehicleMapSizeFactor");
        }
        if (u.getType() == Unit.INFANTRY) {
            return CampaignMain.cm.getIntegerConfig("InfantryMapSizeFactor");
        }
        if (u.getType() == Unit.MEK) {
            return CampaignMain.cm.getIntegerConfig("MekMapSizeFactor");
        }
        if (u.getType() == Unit.BATTLEARMOR) {
            return CampaignMain.cm.getIntegerConfig("BattleArmorMapSizeFactor");
        }
        if (u.getType() == Unit.AERO) {
            return CampaignMain.cm.getIntegerConfig("AeroMapSizeFactor");
        }
        if (u.getType() == Unit.PROTOMEK) {
            return CampaignMain.cm.getIntegerConfig("ProtoMekMapSizeFactor");
        }
        return 0;// no known type? return 0.
    }

    /*
     * AR-related statics.
     */
    public static double getArmorCost(Entity unit, int location) {
        double cost = 0.0;

        if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            return 0;
        }

        String armorCost = "CostPoint" + UnitUtils.getArmorShortName(unit, location);
        cost = CampaignMain.cm.getDoubleConfig(armorCost);

        return cost;
    }

    public static double getStructureCost(Entity unit) {
        double cost = 0.0;

        if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            return 0;
        }

        String armorCost = "CostPoint" + UnitUtils.getInternalShortName(unit) + "IS";
        cost = CampaignMain.cm.getDoubleConfig(armorCost);

        return cost;
    }

    public static double getCritCost(Entity unit, CriticalSlot crit) {

        double cost = 0.0;
        if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            return 0;
        }

        if (crit == null) {
            return 0;
        }

        if (crit.isBreached() && !crit.isDamaged()) {
            return 0;
        }

        // else
        if (UnitUtils.isEngineCrit(crit)) {
            cost = CampaignMain.cm.getDoubleConfig("EngineCritRepairCost");
        } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (crit.isMissing()) {
                cost = CampaignMain.cm.getDoubleConfig("SystemCritReplaceCost");
            } else {
                cost = CampaignMain.cm.getDoubleConfig("SystemCritRepairCost");
            }
        } else {
            Mounted mounted = crit.getMount();

            if (mounted.getType() instanceof WeaponType) {
                WeaponType weapon = (WeaponType) mounted.getType();
                if (weapon.hasFlag(WeaponType.F_ENERGY)) {
                    if (crit.isMissing()) {
                        cost = CampaignMain.cm.getDoubleConfig("EnergyWeaponCritReplaceCost");
                    } else {
                        cost = CampaignMain.cm.getDoubleConfig("EnergyWeaponCritRepairCost");
                    }
                } else if (weapon.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (crit.isMissing()) {
                        cost = CampaignMain.cm.getDoubleConfig("BallisticCritReplaceCost");
                    } else {
                        cost = CampaignMain.cm.getDoubleConfig("BallisticCritRepairCost");
                    }
                } else if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                    if (crit.isMissing()) {
                        cost = CampaignMain.cm.getDoubleConfig("MissileCritReplaceCost");
                    } else {
                        cost = CampaignMain.cm.getDoubleConfig("MissileCritRepairCost");
                    }
                } else // use the misc eq costs.
                if (crit.isMissing()) {
                    cost = CampaignMain.cm.getDoubleConfig("EquipmentCritReplaceCost");
                } else {
                    cost = CampaignMain.cm.getDoubleConfig("EquipmentCritRepairCost");
                }
            } else // use the misc eq costs.
            if (crit.isMissing()) {
                cost = CampaignMain.cm.getDoubleConfig("EquipmentCritReplaceCost");
            } else {
                cost = CampaignMain.cm.getDoubleConfig("EquipmentCritRepairCost");
            }
        }

        cost = Math.max(cost, 1);
        return cost;
    }

    // METHODS
    /**
     * @return the Serialized Version of this entity
     */
    public String toString(boolean toPlayer) {

    	SerializedMessage msg = new SerializedMessage("$");
        // Recalculate the unit's bv. There is a reason we are sending new data
        // to the player
        if (toPlayer) {
            setBV(0);
            getBV();
        }

        msg.append("CM");
        msg.append(getUnitFilename());
        msg.append(getPosId());
        msg.append(getStatus());
        msg.append(getProducer());
        msg.append(((SPilot) getPilot()).toFileFormat("#", toPlayer));
        if (toPlayer) {
            LinkedList<MegaMekPilotOption> mmoptions = getPilot().getMegamekOptions();
            msg.append(mmoptions.size());
            Iterator<MegaMekPilotOption> i = mmoptions.iterator();
            while (i.hasNext()) {
                MegaMekPilotOption mmo = i.next();
                msg.append(mmo.getMmname());
                msg.append(mmo.isValue());
            }
            msg.append(getType());
            msg.append(getBV());
        }
        msg.append(getWeightclass());
        msg.append(getId());

        // error units don't need the rest of this data sent.
        if (getModelName().equals("OMG-UR-FD")) {
            return msg.getMessage();
        }

        if (getEntity() instanceof Mech) {
            unitEntity = getEntity();
            msg.append(((Mech) unitEntity).isAutoEject());
        }
        ArrayList<Mounted> en_Ammo = unitEntity.getAmmo();
        msg.append(en_Ammo.size());
        for (Mounted mAmmo : en_Ammo) {

            boolean hotloaded = mAmmo.isHotLoaded();
            if (!CampaignMain.cm.getMegaMekClient().getGame().getOptions().booleanOption("tacops_hotload")) {
                hotloaded = false;
            }

            AmmoType at = (AmmoType) mAmmo.getType();
            msg.append(at.getAmmoType());
            msg.append(at.getInternalName());
            msg.append(mAmmo.getUsableShotsLeft());
            msg.append(hotloaded);
        }

        if ((unitEntity instanceof Mech) || (unitEntity instanceof Tank)) {
            int mgCount = CampaignMain.cm.getMachineGunCount(unitEntity.getWeaponList());
            msg.append(mgCount);

            if (mgCount > 0) {

                for (int location = 0; location < unitEntity.locations(); location++) {
                    for (int slot = 0; slot < unitEntity.getNumberOfCriticals(location); slot++) {
                        CriticalSlot crit = unitEntity.getCritical(location, slot);

                        if ((crit == null) || (crit.getType() != CriticalSlot.TYPE_EQUIPMENT)) {
                            continue;
                        }

                        Mounted m = crit.getMount();

                        if ((m == null) || !(m.getType() instanceof WeaponType)) {
                            continue;
                        }

                        WeaponType wt = (WeaponType) m.getType();

                        if (!wt.hasFlag(WeaponType.F_MG)) {
                            continue;
                        }

                        msg.append(location);
                        msg.append(slot);
                        msg.append(m.isRapidfire());
                    }
                }
            }
        } else {
        	msg.append(0);
        }

        msg.append(0);
        msg.append(targetSystem.getCurrentType());
        msg.append(isSupportUnit()?"1":"0");
        msg.append(getScrappableFor());
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            // do not need to save ammo twice so set sendAmmo to False
        	msg.append(UnitUtils.unitBattleDamage(getEntity(), false));
        } else {
        	msg.append("%%-%%-%%-");
        }

        if (toPlayer) {
        	msg.append(getPilotIsReparing());
        }
        if (!toPlayer) {
        	msg.append(getLastCombatPilot());
        }

        msg.append(getCurrentRepairCost());
        msg.append(getLifeTimeRepairCost());
        msg.append(this.isChristmasUnit());
        //@salient
        msg.append(QuirkHandler.getInstance().returnQuirkSave(this));
        
        return msg.getMessage();
    }

    /**
     * Reads a Entity from a String
     *
     * @param s
     *            A string to read from
     * @return the remaining String
     */
    public String fromString(String s) {

        try {
            s = s.substring(3);

            StringTokenizer ST = new StringTokenizer(s, "$");
            setUnitFilename(TokenReader.readString(ST));

            setPosId(TokenReader.readInt(ST));
            int newstate = TokenReader.readInt(ST);// status read-in
            setProducer(TokenReader.readString(ST));
            SPilot p = new SPilot();
            p.fromFileFormat(TokenReader.readString(ST), "#");

            setWeightclass(TokenReader.readInt(ST));

            setId(TokenReader.readInt(ST));
            if (CampaignMain.cm.getCurrentUnitID() <= getId()) {
                CampaignMain.cm.setCurrentUnitID(getId() + 1);
            }

            if (getId() == 0) {
                setId(CampaignMain.cm.getAndUpdateCurrentUnitID());
            }
            /*
             * Handle unit status. FOR_SALE and AdvanceRepair both require
             * special handling. If the unit is FOR_SALE, make sure a listing
             * still exists. If not, the server probably crashed and the unit
             * should be returned to normal.
             */
            if ((newstate == STATUS_FORSALE) && (CampaignMain.cm.getMarket().getListingForUnit(getId()) == null)) {
                setStatus(STATUS_OK);
            } else if (CampaignMain.cm.isUsingAdvanceRepair()) {
                setStatus(STATUS_OK);
            } else {
                setStatus(newstate);
            }

            unitEntity = SUnit.loadMech(getUnitFilename());
            setEntity(unitEntity);
            init();
            this.setPilot(p);

            // if its an OMG unit it won't have Ammo
            if (getModelName().equals("OMG-UR-FD")) {
                return s;
            }

            if (unitEntity instanceof Mech) {
                ((Mech) unitEntity).setAutoEject(TokenReader.readBoolean(ST));
            }
            String defaultField = "0";
            if (ST.hasMoreElements()) {
                Entity en = getEntity();
                int maxCrits = TokenReader.readInt(ST);
                defaultField = TokenReader.readString(ST);
                ArrayList<Mounted> e = en.getAmmo();
                for (int count = 0; count < maxCrits; count++) {
                    int weaponType = Integer.parseInt(defaultField);
                    String ammoName = TokenReader.readString(ST);
                    int shots = TokenReader.readInt(ST);
                    boolean hotloaded = false;
                    // needed to make backwards compatibility better.
                    try {
                        defaultField = TokenReader.readString(ST);
                        hotloaded = Boolean.parseBoolean(defaultField);
                        defaultField = TokenReader.readString(ST);
                    } catch (Exception ex) {
                        hotloaded = false;
                    }

                    if (!CampaignMain.cm.getMegaMekClient().getGame().getOptions().booleanOption("tacops_hotload")) {
                        hotloaded = false;
                    }

                    Mounted mWeapon = e.get(count);

                    AmmoType at = getEntityAmmo(weaponType, ammoName);
                    if (at == null) {
                        // loaded --Torren.
                        continue;
                    }
                    String munition = Long.toString(at.getMunitionType());

                    // check banned ammo
                    if (CampaignMain.cm.getData().getServerBannedAmmo().get(munition) != null) {
                        continue;
                    }

                    mWeapon.changeAmmoType(at);
                    mWeapon.setShotsLeft(shots);
                    mWeapon.setHotLoad(hotloaded);
                }
                setEntity(en);
            }
            int maxMachineGuns = Integer.parseInt(defaultField);
            Entity en = getEntity();
            for (int count = 0; count < maxMachineGuns; count++) {
                int location = TokenReader.readInt(ST);
                int slot = TokenReader.readInt(ST);
                boolean selection = TokenReader.readBoolean(ST);
                try {
                    CriticalSlot cs = en.getCritical(location, slot);
                    Mounted m = cs.getMount();
                    m.setRapidfire(selection);
                } catch (Exception ex) {
                }
            }
            setEntity(en);
            targetSystem.setEntity(en);
            TokenReader.readString(ST);// unused
            int tsType = TokenReader.readInt(ST);
            if ((tsType != TargetSystem.TS_TYPE_STANDARD) && CampaignData.cd.targetSystemIsBanned(tsType)) {
            	tsType = TargetSystem.TS_TYPE_STANDARD;
            }
            targetSystem.setTargetSystem(tsType);

            TokenReader.readInt(ST); // Placeholder for isSupportUnit
            // Now we need to override this.  Needs to be set in the string,
            // so we don't need to keep a list of all support units client-side
            // but should be dynamic server-side.
            if (CampaignMain.cm.getSupportUnits().contains(getUnitFilename().trim().toLowerCase())) {
            	setSupportUnit(true);
            } else {
            	setSupportUnit(false);
            }


            setScrappableFor(TokenReader.readInt(ST));

            if (CampaignMain.cm.isUsingAdvanceRepair() && ((unitEntity instanceof Mech) || (unitEntity instanceof Tank))) {
                UnitUtils.applyBattleDamage(unitEntity, TokenReader.readString(ST), ((CampaignMain.cm.getRTT() != null) && (CampaignMain.cm.getRTT().unitRepairTimes(getId()) != null)));
            } else {
                TokenReader.readString(ST);
            }
            setLastCombatPilot(TokenReader.readInt(ST));

            setRepairCosts(TokenReader.readInt(ST), TokenReader.readInt(ST));
            
            setChristmasUnit(TokenReader.readBoolean(ST));
            
            // quirks might be changed by SO, drop old quirks, then reset them.
            if(ST.hasMoreTokens())
            	TokenReader.readString(ST); 
            QuirkHandler.getInstance().setQuirks(this); //checks if quirks are enabled, if not does nothing

            return s;
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Unable to Load SUnit: " + s);
            // the unit should still be good return what did get set
            return s;
        }
    }

    /**
     * @return a description of the entity including pilot
     */
    public String getDescription(boolean showLink) {
        String status = "";

        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            if (UnitUtils.hasCriticalDamage(getEntity())) {
                status = "Is Critically Damaged";
            } else if (UnitUtils.hasArmorDamage(getEntity())) {
                status = "Has Minor Armor Damage";
            } else if (UnitUtils.isRepairing(getEntity())) {
                status = "Is Currently Under Going Repairs";
            } else {
                status = "Is Fully Functional";
            }
        } else {
            if (getStatus() == Unit.STATUS_UNMAINTAINED) {
                status = "Unmaintained" + " (" + getMaintainanceLevel() + "%)";
            } else {
                status = "Maintained" + " (" + getMaintainanceLevel() + "%)";
            }
        }

        String idToShow = "";
        if (showLink) {
            idToShow = "<a href=\"MEKWARS/c sth#u#" + getId() + "\">#" + getId() + "</a>";
        } else {
            idToShow = "#" + getId();
        }
        String dialogBox = "<a href=\"MEKINFO" + getEntity().getChassis() + " " + getEntity().getModel().replace("\"", "%22") + "#" + getBVForMatch() + "#" + getPilot().getGunnery() + "#" + getPilot().getPiloting() + "\">" + getModelName() + "</a>";

        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE)) {
            return idToShow + " " + dialogBox + " (" + getPilot().getGunnery() + "/" + getPilot().getPiloting() + ") [" + getPilot().getExperience() + " EXP " + getPilot().getSkillString(false) + "] Kills: " + getPilot().getKills() + " " + getProducer() + ". BV: " + getBVForMatch() + " " + status;
        }

        if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) getEntity()).canMakeAntiMekAttacks()) {
                return idToShow + " " + dialogBox + " (" + getPilot().getGunnery() + "/" + getPilot().getPiloting() + ") [" + getPilot().getExperience() + " EXP " + getPilot().getSkillString(false) + "] Kills: " + getPilot().getKills() + " " + getProducer() + ". BV: " + getBVForMatch() + " " + status;
            }
        }
        // else
        return idToShow + " " + dialogBox + " (" + getPilot().getGunnery() + ") [" + getPilot().getExperience() + " EXP " + getPilot().getSkillString(false) + "] Kills: " + getPilot().getKills() + " " + getProducer() + ". BV: " + getBVForMatch() + " " + status;
    }

    /**
     * @return a smaller description
     */
    public String getSmallDescription() {
        String result;
        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            result = getModelName() + " [" + getPilot().getGunnery() + "/" + getPilot().getPiloting();
        } else if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) getEntity()).canMakeAntiMekAttacks()) {
                result = getModelName() + " [" + getPilot().getGunnery() + "/" + getPilot().getPiloting();
            } else {
                result = getModelName() + " [" + getPilot().getGunnery();
            }
        } else {
            result = getModelName() + " [" + getPilot().getGunnery();
        }

        if (!getPilot().getSkillString(true).equals(" ")) {
            result += getPilot().getSkillString(true);
        }
        result += "]";
        return result;
    }

    /**
     * Returns the Modelname for this Unit
     *
     * @return the Modelname
     */
    public String getModelName() {
        if (checkModelName() == null) {
            unitEntity = SUnit.loadMech(getUnitFilename());
            init();
        }

        return checkModelName();
    }

    public String getVerboseModelName() {
        // Includes Pilot Stats in ModelName
        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            return getModelName() + " (" + getPilot().getGunnery() + "/" + getPilot().getPiloting() + ")";
        }

        if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) getEntity()).canMakeAntiMekAttacks()) {
                return getModelName() + " (" + getPilot().getGunnery() + "/" + getPilot().getPiloting() + ")";
            }
        }

        return getModelName() + " (" + getPilot().getGunnery() + ")";
    }

    /**
     * @return the BV of this entity including all modifications
     */
    public int calcBV() {

        try {
            if (hasVacantPilot()) {
                getEntity().getCrew().setGunnery(4);
                getEntity().getCrew().setPiloting(5);
            } else {
                getEntity().setCrew(UnitUtils.createEntityPilot(this));
            }

            // get a base BV from MegaMek
            int calcedBV = getEntity().calculateBattleValue();

            // Boost BV of super-fast tanks if the "FastHoverBVMod" is a
            // positive
            // number.
            int FastHoverBVMod = CampaignMain.cm.getIntegerConfig("FastHoverBVMod");
            if ((FastHoverBVMod > 0) && (getType() == Unit.VEHICLE) && (getEntity().getMovementMode() == megamek.common.EntityMovementMode.HOVER)) {
                if (getEntity().getWalkMP() >= 8) {
                    calcedBV += FastHoverBVMod;
                }
            }

            // Increase elite BV's by 5% if the "ElitePilotsBVMod" is enabled.
            if (CampaignMain.cm.getBooleanConfig("ElitePilotsBVMod")) {
                if (getPilot().getGunnery() < 3) {
                    calcedBV = (int) Math.round(calcedBV * 1.05);
                } else if (getPilot().getPiloting() < 3) {
                    calcedBV = (int) Math.round(calcedBV * 1.05);
                }
            }

            // Increase BV if the pilot has MaxTech/MechWarrior skills.
            calcedBV += getPilotSkillBV();

            if (hasVacantPilot()) {
                getEntity().getCrew().setGunnery(99);
                getEntity().getCrew().setPiloting(99);
            }
            return calcedBV;
        } catch (Exception ex) {
            return Integer.MAX_VALUE;
        }
    }

    @Override
    public boolean equals(Object o) {

        SUnit m = null;
        try {
            m = (SUnit) o;
        } catch (ClassCastException e) {
            return false;
        }

        if (m == null) {
            return false;
        }

        if ((m.getId() == getId()) && m.getUnitFilename().equals(getUnitFilename()) && (m.getPilot().getGunnery() == getPilot().getGunnery()) && (m.getPilot().getPiloting() == getPilot().getPiloting())) {
            return true;
        }

        // else
        return false;
    }

    /**
     * Sets the Pilot of this entity
     *
     * @param p
     *            A pilot
     */
    public void setPilot(SPilot p) {

        // zero BV any time a new pilot is added
        setBV(0);

        if (p == null) {
            return;
        }

        // any time the pilot changes set the unit commander flag to false.
        Crew mPilot = new Crew(CrewType.SINGLE, p.getName(), 1, p.getGunnery(), p.getPiloting());
        Entity entity = getEntity();

        // Lazy Bug report. non Anti-Mek BA should not have a Piloting skill
        // better/worse then 5
		if ((getEntity() instanceof BattleArmor)
				&& !((BattleArmor) getEntity()).canMakeAntiMekAttacks() && !hasVacantPilot()) {
            mPilot.setPiloting(5);
        }

        entity.setCrew(mPilot);
        setEntity(entity);

        if (p.getSkills().has(PilotSkill.WeaponSpecialistSkillID)) {
            Iterator<PilotSkill> ski = p.getSkills().getSkillIterator();
            while (ski.hasNext()) {
                SPilotSkill skill = (SPilotSkill) ski.next();
                if (skill.getName().equals("Weapon Specialist") && p.getWeapon().equals("Default")) {
                    // MWLogger.errLog("setPilot inside");
                    p.getSkills().remove(skill);
                    ((WeaponSpecialistSkill) skill).assignWeapon(getEntity(), p);
                    skill.addToPilot(p);
                    skill.modifyPilot(p);
                    break;
                }
            }
        }

        p.setUnitType(getType());
        super.setPilot(p);
    }

    public void init() {

        setType(Unit.getEntityType(getEntity()));

        /*
         * if (this.getType() == Unit.MEK || this.getType() == Unit.VEHICLE)
         * setWeightclass(getEntityWeight(this.getEntity()));
         */
        // Set Modelname
        if ((getType() != Unit.MEK) || getEntity().isOmni()) {
            setModelname(new String(unitEntity.getChassis() + " " + unitEntity.getModel()).trim());
        } else {

            if (unitEntity.getModel().trim().length() > 0) {
                setModelname(unitEntity.getModel().trim());
            } else {
                setModelname(unitEntity.getChassis().trim());
            }

        }
        getC3Type(unitEntity);

        if (getModelName().equals("OMG-UR-FD")) {
            setProducer("Error loading unit. Tried to build from " + getUnitFilename());
            setWeightclass(Unit.LIGHT);
        }

        /*
         * //Set Weight this.weight = m.getWeight();
         */
    }

    /**
     * Sets status to unmaintained. Factors out repetetive code checking
     * maintainance status and decreasing as unit is moved to unmaintained.
     * Called from both Player and SetUnmaintainedCommand. It would possible to
     * bypass this code and set a unit as unmaintained without incurring any
     * maintainance penalty w/ Unit.setStatus(STATUS_UNMAINTAINED).
     *
     * @urgru 8/4/04
     */
    public void setUnmaintainedStatus() {

        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            setStatus(STATUS_OK);
            return;
        }

        // load configurables
        int baseUnmaintained = CampaignMain.cm.getIntegerConfig("BaseUnmaintainedLevel");
        int unmaintPenalty = CampaignMain.cm.getIntegerConfig("UnmaintainedPenalty");

        // set the actual status
        setStatus(STATUS_UNMAINTAINED);

        /*
         * now change the maintainance levels. if the unit is well maintained,
         * drop it to the basevalue. otherwise, apply the standard penalty.
         */
        if (getMaintainanceLevel() >= (baseUnmaintained + unmaintPenalty)) {
            setMaintainanceLevel(baseUnmaintained);
        } else {
            addToMaintainanceLevel(-unmaintPenalty);
        }

    }// end setUnmaintainedStatus()

    // GETTER AND SETTER

    public int getBVForMatch() {
    	if (CampaignMain.cm.getBooleanConfig("UseBaseBVForMatching")) {
    		return getBaseBV();
    	}
    	return getBV();
    }

    public int getBV() {

        int toReturn = 0;

        if (BV <= 0) {
            toReturn = calcBV();
            BV = toReturn;
        } else {
            toReturn = BV;
        }

        // if the BV is negative, send a 0 instead.
        return (toReturn < 0) ? 0 : toReturn;
    }

    public void setBV(Integer i) {
        if (i < 0) {
            BV = 0;
        }
        BV = i;
    }

    /**
     * @return the megamek.common.entity this Unit represents
     */
    public Entity getEntity() {

        // alreayd loaded. return.
        if (unitEntity != null) {
            return unitEntity;
        }

        // need to load. do so.
        unitEntity = SUnit.loadMech(getUnitFilename());
        return unitEntity;
    }

    public void setEntity(Entity unitEntity) {
        this.unitEntity = unitEntity;
    }

    public static Entity loadMech(String Filename) {

        if (Filename == null) {
            return null;
        }

        Entity ent = null;

        if (new File("./data/mechfiles").exists()) {

            try {
                MechSummary ms = MechSummaryCache.getInstance().getMech(Filename.trim());
                if (ms == null) {
                    MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                    // System.err.println("unit: "+getUnitFilename());
                    for (MechSummary unit : units) {
                        // System.err.println("Source file:
                        // "+unit.getSourceFile().getName());
                        // System.err.println("Model: "+unit.getModel());
                        // System.err.println("Chassis:
                        // "+unit.getChassis());
                        // System.err.flush();
                        if (unit.getEntryName().equalsIgnoreCase(Filename) || unit.getModel().trim().equalsIgnoreCase(Filename.trim()) || unit.getChassis().trim().equalsIgnoreCase(Filename.trim())) {
                            ms = unit;
                            break;
                        }
                    }
                }

                if (ms != null) {
                    ent = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
                }
            } catch (Exception exep) {
                ent = null;
            }

        }

        if (ent != null) {
            return ent;
        }

        // look for a mek first
        try {
            ent = new MechFileParser(new File("./data/mechfiles/Meks.zip"), Filename).getEntity();
        } catch (Exception ex) {

            // not a mek, see if file is a vehicle...
            try {
                ent = new MechFileParser(new File("./data/mechfiles/Vehicles.zip"), Filename).getEntity();
            } catch (Exception exe) {

                // neither mek nor veh. look for infantry.
                try {
                    ent = new MechFileParser(new File("./data/mechfiles/Infantry.zip"), Filename).getEntity();
                } catch (Exception exei) {

                    /*
                     * Unit cannot be found in Meks.zip, Vehicles.zip or
                     * Infantry.zip. Probably a bad filename (table type) or a
                     * missing unit. Either way, need to set up and return a
                     * failsafe unit.
                     */
                    MWLogger.errLog("Error loading: " + Filename);

                    try {
                        ent = UnitUtils.createOMG();// new MechFileParser(new
                    } catch (Exception exep) {

                        /*
                         * Can't even find the default unit file. Are all the
                         * .zip files missing? Misnamed? Read access is denied?
                         */
                        MWLogger.errLog("Unable to find default unit file. Server Exiting");
                        MWLogger.errLog(exep);
                        System.exit(1);
                    }
                }
            }
        }
        return ent;
    }// end loadMech

    public void setPassesMaintainanceUntil(long l) {
        passesMaintainanceUntil = l;
    }

    public long getPassesMaintainanceUntil() {
        return passesMaintainanceUntil;
    }

    public int getScrappableFor() {
        return scrappableFor;
    }

    public void setScrappableFor(int i) {
        scrappableFor = i;
    }

    /**
     * @return the amount of EXP the pilot has
     */
    public int getExperience() {
        return getPilot().getExperience();
    }

    /**
     * @param experience
     *            the experience to set the pilot to
     */
    public void setExperience(Integer experience) {
        getPilot().setExperience(experience.intValue());
        // this.experience = experience;
    }

    public boolean isOmni() {

        boolean isOmni = getEntity().isOmni();
        String targetChassis = getEntity().getChassis();

        // Check the vehicle list to see if they SO's want it to be an omni but
        // do not have it flagged
        // In the MM File.
        if ((getType() == Unit.VEHICLE) && !isOmni) {
            try {
                FileInputStream fis = new FileInputStream("./data/buildtables/omnivehiclelist.txt");
                BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
                while (dis.ready()) {
                    String chassie = dis.readLine();
                    // check to see if the chassies listed in the file match
                    // omni vehicle chassies.
                    if (targetChassis.equalsIgnoreCase(chassie)) {
                        dis.close();
                        fis.close();
                        return true;
                    }
                }
                dis.close();
                fis.close();
            } catch (Exception ex) {
                // Simply means no omniveh list present. Ignore.
            }
        }

        return isOmni;
    }

    public boolean hasTAG() {
        return getEntity().hasTAG();
    }

    public boolean hasHoming() {

        for (Mounted ammo : getEntity().getAmmo()) {
            if (((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_HOMING) {
                return true;
            }
        }
        return false;
    }

    public boolean hasSemiGuided() {
        for (Mounted ammo : getEntity().getAmmo()) {
            // MWLogger.errLog("ammo type:
            // "+((AmmoType)ammo.getType()).getMunitionType());
            if (((AmmoType) ammo.getType()).getMunitionType() == AmmoType.M_SEMIGUIDED) {
                return true;
            }
        }
        return false;

    }

    public int getBaseBV() {
    	return getEntity().calculateBattleValue(false, true);
    }

    public int getPilotSkillBV() {

        int skillBV = 0;
        Iterator<PilotSkill> pilotSkills = getPilot().getSkills().getSkillIterator();

        while (pilotSkills.hasNext()) {
            SPilotSkill skill = (SPilotSkill) pilotSkills.next();
            skillBV += skill.getBVMod(getEntity(), (SPilot) getPilot());
        }

        return skillBV;
    }

    public void setPilotIsRepairing(boolean repair) {
        pilotIsRepairing = repair;
    }

    public boolean getPilotIsReparing() {
        return pilotIsRepairing;
    }

    public int getLastCombatPilot() {
        return lastCombatPilot;
    }

    public void setLastCombatPilot(int pilot) {
        lastCombatPilot = pilot;
    }

    @Override
    public void setWeightclass(int i) {

        if ((i > Unit.ASSAULT) || (i < Unit.LIGHT)) {
            i = Unit.getEntityWeight(getEntity());
        }

        super.setWeightclass(i);
    }

    public static Vector<SUnit> createMULUnits(String filename) {
        return SUnit.createMULUnits(filename, "autoassigned unit");
    }

    public static Vector<SUnit> createMULUnits(String filename, String fluff) {
        Vector<SUnit> mulUnits = new Vector<SUnit>(1, 1);

        Vector<Entity> loadedUnits = null;
        File entityFile = new File("data/armies/" + filename);

        try {
            loadedUnits = new MULParser(entityFile,null).getEntities();
            loadedUnits.trimToSize();
        } catch (Exception ex) {
            MWLogger.errLog("Unable to load file " + entityFile.getName());
            MWLogger.errLog(ex);
            return mulUnits;
        }

        for (Entity en : loadedUnits) {

            SUnit cm = new SUnit();

            cm.setEntity(en);
            cm.setUnitFilename(UnitUtils.getEntityFileName(en));
            cm.setId(CampaignMain.cm.getAndUpdateCurrentUnitID());
            cm.init();
            cm.setProducer(fluff);

            SPilot pilot = null;
            pilot = new SPilot(en.getCrew().getName(), en.getCrew().getGunnery(), en.getCrew().getPiloting());

            if (pilot.getName().equalsIgnoreCase("Unnamed") || pilot.getName().equalsIgnoreCase("vacant")) {
                pilot.setName(SPilot.getRandomPilotName(CampaignMain.cm.getR()));
            }

            pilot.setCurrentFaction("Common");
            StringTokenizer skillList = new StringTokenizer(en.getCrew().getOptionList(",", PilotOptions.LVL3_ADVANTAGES), ",");

            while (skillList.hasMoreTokens()) {
                String skill = skillList.nextToken();

                if (skill.toLowerCase().startsWith("weapon_specialist")) {
                    pilot.addMegamekOption(new MegaMekPilotOption("weapon_specialist", true));
                    pilot.getSkills().add(SPilotSkills.getPilotSkill(PilotSkill.WeaponSpecialistSkillID));
                    pilot.setWeapon(skill.substring("weapon_specialist".length()).trim());
                } else if (skill.toLowerCase().startsWith("edge ")) {
                    pilot.addMegamekOption(new MegaMekPilotOption("edge", true));
                    pilot.getSkills().add(SPilotSkills.getPilotSkill(PilotSkill.EdgeSkillID));
                    try {
                        pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID).setLevel(Integer.parseInt(skill.substring("edge ".length()).trim()));
                    } catch (Exception ex) {
                        pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID).setLevel(1);
                    }
                } else if (skill.toLowerCase().equals("edge_when_headhit")) {
                    pilot.setHeadHit(true);
                } else if (skill.toLowerCase().equals("edge_when_tac")) {
                    pilot.setTac(true);
                } else if (skill.toLowerCase().equals("edge_when_ko")) {
                    pilot.setKO(true);
                } else if (skill.toLowerCase().equals("edge_when_explosion")) {
                    pilot.setExplosion(true);
                } else {
                    pilot.getSkills().add(SPilotSkills.getPilotSkill(PilotSkill.getMMSkillID(skill)));
                    pilot.addMegamekOption(new MegaMekPilotOption(skill, true));
                }
            }

            skillList = new StringTokenizer(en.getCrew().getOptionList(",", PilotOptions.MD_ADVANTAGES), ",");

            while (skillList.hasMoreTokens()) {
                String skill = skillList.nextToken();

                pilot.getSkills().add(SPilotSkills.getPilotSkill(PilotSkill.getMMSkillID(skill)));
                pilot.addMegamekOption(new MegaMekPilotOption(skill, true));
            }

            cm.setPilot(pilot);

            cm.setWeightclass(99);// let the SUnit code handle the weightclass

            mulUnits.add(cm);
        }
        return mulUnits;
    }

    /**
     * Compares SUnit IDs to support sorting of collections
     * @author Spork
     */
	public int compareTo(SUnit u) {
		return Integer.valueOf(getId()).compareTo(Integer.valueOf(u.getId()));
	}

    public void setTargetSystem(int type) {
    	if ((type != TargetSystem.TS_TYPE_STANDARD) && CampaignData.cd.targetSystemIsBanned(type)) {
    		setTargetSystem(TargetSystem.TS_TYPE_STANDARD);
    	}
    	try {
			targetSystem.setTargetSystem(type);
		} catch (TargetTypeOutOfBoundsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TargetTypeNotImplementedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    @Override
    public boolean isSupportUnit() {
    	return CampaignMain.cm.getSupportUnits().contains(getUnitFilename().toLowerCase());
    }

    public void reportStateToPlayer(SPlayer player){
    	CampaignMain.cm.toUser("PL|UU|" + getId() + "|" + toString(true), player.getName(), false);
    }
    public boolean isOMGUnit() {
    	return getModelName().equals("OMG-UR-FD");
    }

	public boolean canBeCapturedInOperation(Operation o) {
    	switch (getType()) {
		case Unit.MEK:
			return o.getBooleanValue("ForceProduceAndCaptureMeks");
		case Unit.VEHICLE:
			return o.getBooleanValue("ForceProduceAndCaptureVees");
		case Unit.INFANTRY:
			return o.getBooleanValue("ForceProduceAndCaptureInfs");
		case Unit.PROTOMEK:
			return o.getBooleanValue("ForceProduceAndCaptureProtos");
		case Unit.BATTLEARMOR:
			return o.getBooleanValue("ForceProduceAndCaptureBAs");
		case Unit.AERO:
			return o.getBooleanValue("ForceProduceAndCaptureAeros");
    	}

		return false;
	}
	
	/**
	 * Creates a unit
	 * <p>
	 * create() takes a number of variables and creates a unit.  This is called by both the Christmas code
	 * and the /CreateUnit command.
	 * @param filename     The file name of the unit
	 * @param fluff        Any flavor text
	 * @param gunnery      Gunnery skill of the pilot
	 * @param piloting     Piloting skill of the pilot
	 * @param weight       Weight class to be used (note: why?  Why can't we get rid of this?)
	 * @param skillTokens  Pilot skills
	 * @return             the created unit
	 */
	public static SUnit create(String filename, String fluff, int gunnery, int piloting, Integer weight, String skillTokens) {
		boolean refigureWeightClass = false;
		
		if (weight == null) {
			weight = SUnit.LIGHT;
			// This is stupid.  We should not have to specify weight classes.  So now we do not.
			refigureWeightClass = true;
		}
		
		SUnit cm = new SUnit(fluff,filename,weight);
		
		if (refigureWeightClass) {
			cm.setWeightclass(cm.getEntity().getWeightClass());
			MWLogger.debugLog("Setting " + cm.getEntity().getModel() + " to weight class " + cm.getEntity().getWeightClass());
		}
		
		SPilot pilot = null;
		if ( gunnery == 99 || piloting == 99 )
		    pilot = new SPilot("Vacant",99,99);
		else
		    pilot = new SPilot(SPilot.getRandomPilotName(CampaignMain.cm.getR()),gunnery,piloting);
		
        pilot.setCurrentFaction("Common");

		if(skillTokens != null) {
			StringTokenizer skillList = new StringTokenizer(skillTokens,",");
			while (skillList.hasMoreTokens()){
				String skill = skillList.nextToken();
				SPilotSkill pSkill = null; 
				if ( skill.equalsIgnoreCase("random") )
					pSkill = SPilotSkills.getRandomSkill(pilot, cm.getType() );
				else					
					pSkill = SPilotSkills.getPilotSkill(skill);
				
				if ( pSkill != null ){
	                if ( pSkill instanceof TraitSkill){
	                    ((TraitSkill)pSkill).assignTrait(pilot);
	                }
	                pSkill.addToPilot(pilot);
	                pSkill.modifyPilot(pilot);
	            }
			}
		}
			
		cm.setPilot(pilot);
		return cm;
	}
}
