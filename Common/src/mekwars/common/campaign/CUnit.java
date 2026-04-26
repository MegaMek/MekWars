/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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

package mekwars.common.campaign;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import megamek.common.CriticalSlot;
import megamek.common.OffBoardDirection;
import megamek.common.equipment.AmmoMounted;
import megamek.common.equipment.AmmoType;
import megamek.common.equipment.Mounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;
import megamek.common.units.Crew;
import megamek.common.units.CrewType;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekwars.common.House;
import mekwars.common.MegaMekPilotOption;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.campaign.targetsystems.TargetSystem;
import mekwars.common.campaign.targetsystems.TargetTypeOutOfBoundsException;
import mekwars.common.util.MWLogger;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

/**
 * Class for unit object used by Client
 */
public class CUnit extends Unit {

    // VARIABLES
    protected Entity unitEntity;

    private int BV;
    private int scrappableFor = 0;// value if scrapped
    private boolean pilotIsRepairing = false;
    private IClient client;
    private String htmlQuirkList = " ";
    private String quirkList = " ";

    // CONSTRUCTORS
    public CUnit() {
        init();
    }

    public CUnit(IClient client) {
        this.client = client;
        init();
    }

    // PRIVATE METHODS
    private void init() {
        unitEntity = null;
        BV = 0;
        setStatus(STATUS_OK);
        setProducer("unknown origin");
    }

    // PUBLIC METHODS
    public boolean setData(String data) {

        StringTokenizer ST;
        String element;
        String unitDamage = null;
        MWLogger.infoLog("PDATA: " + data);

        ST = new StringTokenizer(data, "$");
        element = TokenReader.readString(ST);
        if (!element.equals("CM")) {
            return (false);
        }

        setUnitFilename(TokenReader.readString(ST));
        setId((TokenReader.readInt(ST)));
        setStatus(TokenReader.readInt(ST));

        setProducer(TokenReader.readString(ST));
        String pilotName;
        int gunnery;
        int piloting;
        int exp;
        Pilot pilot;
        StringTokenizer STR = new StringTokenizer(TokenReader.readString(ST), "#");
        pilotName = TokenReader.readString(STR);
        exp = TokenReader.readInt(STR);
        gunnery = TokenReader.readInt(STR);
        piloting = TokenReader.readInt(STR);
        pilot = new Pilot(pilotName, gunnery, piloting);
        pilot.setExperience(exp);
        int skillAmount = TokenReader.readInt(STR);
        for (int i = 0; i < skillAmount; i++) {
            PilotSkill skill = new PilotSkill(TokenReader.readInt(STR),
                  TokenReader.readString(STR), TokenReader.readInt(STR),
                  TokenReader.readString(STR));

            if (skill.getName().equals("Weapon Specialist")) {
                pilot.setWeapon(TokenReader.readString(STR));
            }

            if (skill.getName().equals("Trait")) {
                pilot.setTraitName(TokenReader.readString(STR));
            }

            if (skill.getName().equals("Edge")) {
                pilot.setTac(TokenReader.readBoolean(STR));
                pilot.setKO(TokenReader.readBoolean(STR));
                pilot.setHeadHit(TokenReader.readBoolean(STR));
                pilot.setExplosion(TokenReader.readBoolean(STR));
            }
            pilot.getSkills().add(skill);
        }

        pilot.setKills(TokenReader.readInt(STR));

        pilot.setHits(TokenReader.readInt(STR));

        int mmOptionsAmount = TokenReader.readInt(ST);
        for (int i = 0; i < mmOptionsAmount; i++) {
            MegaMekPilotOption mo = new MegaMekPilotOption(
                  TokenReader.readString(ST),
                  Boolean.parseBoolean(TokenReader.readString(ST)));
            pilot.addMegaMekOption(mo);
        }

        setType(TokenReader.readInt(ST));
        setPilot(pilot);
        BV = Math.max(TokenReader.readInt(ST), 0);

        setWeightClass(TokenReader.readInt(ST));
        setId(TokenReader.readInt(ST));

        createEntity();
        if (unitEntity == null) {
            MWLogger.errLog("Cannot load entity!");
            return (false);
        }

        // don't try to set ammo and eject on an OMG
        if (getModelName().startsWith("Error") || getModelName().startsWith("OMG")) {
            unitEntity.setExternalId(getId());
            unitEntity.setCrew(new Crew(CrewType.SINGLE,
                  pilot.getName(),
                  1,
                  pilot.getGunnery(),
                  pilot.getPiloting()));
            return true;
        }

        // set auto eject if its a Mek
        if ((unitEntity instanceof Mek mek) && ST.hasMoreElements()) {
            mek.setAutoEject(Boolean.parseBoolean(TokenReader.readString(ST)));
        }

        // then set up ammo loadout
        {
            try {
                int maxCrits = TokenReader.readInt(ST);
                List<AmmoMounted> entityAmmo = unitEntity.getAmmo();
                for (int count = 0; count < maxCrits; count++) {
                    AmmoType.AmmoTypeEnum weaponType = AmmoType.AmmoTypeEnum.fromIndex(TokenReader.readInt(ST));
                    String ammoName = TokenReader.readString(ST);
                    int shots = TokenReader.readInt(ST);
                    boolean hotLoaded = TokenReader.readBoolean(ST);

                    AmmoMounted mWeapon = entityAmmo.get(count);

                    AmmoType ammoType = getEntityAmmo(weaponType, ammoName);
                    mWeapon.changeAmmoType(ammoType);
                    mWeapon.setShotsLeft(shots);
                    mWeapon.setHotLoad(hotLoaded);
                }
            } catch (Exception ex) {
                // ammo crits change or something bad. just continue with the
                // next unit
                return true;
            }
        }// end ammo

        // setup rapid fire Machine guns, if any
        {
            int maxMachineGuns = TokenReader.readInt(ST);
            for (int count = 0; count < maxMachineGuns; count++) {
                int location = TokenReader.readInt(ST);
                int slot = TokenReader.readInt(ST);
                boolean selection = TokenReader.readBoolean(ST);
                CriticalSlot criticalSlot = unitEntity.getCritical(location, slot);

                Mounted<?> mg = criticalSlot.getMount();

                mg.setRapidfire(selection);

            }
        }// Machine Guns

        TokenReader.readString(ST);// unused

        targetSystem.setEntity(unitEntity);
        try {
            targetSystem.setTargetSystem(TokenReader.readInt(ST));
        } catch (TargetTypeOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int suppUnit = TokenReader.readInt(ST);
        setSupportUnit(suppUnit == 1);

        scrappableFor = TokenReader.readInt(ST);

        unitDamage = TokenReader.readString(ST);

        pilotIsRepairing = TokenReader.readBoolean(ST);

        setRepairCosts(TokenReader.readInt(ST), TokenReader.readInt(ST));

        setChristmasUnit(TokenReader.readBoolean(ST));

        //@salient Quirks - set unit quirks, or drop data if quirks have been turned off
        if (ST.hasMoreTokens() && Boolean.parseBoolean(client.getServerConfigs("EnableQuirks"))) {
            setUnitQuirks(TokenReader.readString(ST));
        } else if (ST.hasMoreTokens()) {
            TokenReader.readString(ST);
        }

        unitEntity.setExternalId(getId());

        UnitUtils.applyBattleDamage(unitEntity, unitDamage, true);

        getC3Type(unitEntity);

        return (true);
    }

    //@salient this method is only accessible when quirks are enabled.
    private void setUnitQuirks(String data) {
        StringTokenizer st = new StringTokenizer(data, "!");
        if (st.hasMoreTokens()) {
            htmlQuirkList = TokenReader.readString(st);
            quirkList = TokenReader.readString(st);
        }

        if (quirkList != null) {
            st = new StringTokenizer(quirkList, "&");
            while (st.hasMoreTokens()) {
                String quirk = TokenReader.readString(st);
                if (!quirk.equalsIgnoreCase("none")) {
                    unitEntity.getQuirks().getOption(quirk).setValue(true);
                }

            }
        }

    }

    public String getHtmlQuirksList() {
        return htmlQuirkList;
    }

    public String getQuirksList() {
        return quirkList;
    }

    //@salient debug method, i really just used this once to make sure the quirks were being set
    //but i'll leave it in case one day someone needs it.
    public String quirkCheck() {
        StringJoiner quirksList = new StringJoiner("&");

        for (Enumeration<IOptionGroup> optionGroups = unitEntity.getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unitEntity.getQuirks().count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        quirksList.add(option.getName());
                    }
                }
            }
        }
        return quirksList.toString();
    }

    public boolean hasQuirks() {
        for (Enumeration<IOptionGroup> optionGroups = unitEntity.getQuirks().getGroups();
              optionGroups.hasMoreElements(); ) {
            IOptionGroup group = optionGroups.nextElement();
            if (unitEntity.getQuirks().count(group.getKey()) > 0) {
                for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements(); ) {
                    IOption option = options.nextElement();
                    if (option != null && option.booleanValue()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Method which generates data for an auto unit. Since auto units have no unique properties this can be assembled
     * client side rather than sent from the server.
     *
     * @urgru 1/4/05
     */
    public void setAutoUnitData(String filename, int distance, OffBoardDirection edge) {
        setUnitFilename(filename);
        setPilot(new Pilot("Autopilot", 4, 5));
        createEntity();// make the entity
        if (distance > 0) {
            unitEntity.setOffBoard(distance, edge);// move
            // it
            // offboard
        }
    }

    /**
     * @return a smaller description
     */
    public String getSmallDescription() {
        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            return getModelName() + " [" + getPilot().getGunnery() + "/" + getPilot().getPiloting() + "]";
        }

        if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                return STR."\{getModelName()} [\{getPilot().getGunnery()}/\{getPilot().getPiloting()}]";
            }
            return STR."\{getModelName()} [\{getPilot().getGunnery()}]";
        }
        return STR."\{getModelName()} [\{getPilot().getGunnery()}]";
    }

    public String getDisplayInfo(String armyText) {
        String tinfo;

        if ((getType() == Unit.MEK) && !unitEntity.isOmni()) {
            tinfo = STR."<html><body>#\{getId()} \{unitEntity.getChassis()}, \{getModelName()}";
        } else {
            tinfo = STR."<html><body>#\{getId()} \{getModelName()}";
        }

        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            tinfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}/\{getPilot().getPiloting()}) <br>";
        } else if ((getType() == Unit.BATTLEARMOR) || (getType() == Unit.INFANTRY)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                tinfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}/\{getPilot().getPiloting()}) <br>";
            } else {
                tinfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}) <br>";
            }
        } else {
            tinfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}) <br>";
        }

        if (getType() == Unit.VEHICLE) {
            tinfo += STR." Movement: \{getEntity().getMovementModeAsString()}<br>";
        }

        tinfo += "BV: ";

        if (Boolean.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"))) {
            tinfo += getBaseBV();
        } else {
            tinfo += BV;
        }

        if (Boolean.parseBoolean(client.getConfigParam("ShowUnitBaseBV"))) {
            if (getBV() != getBaseBV()) {
                tinfo += STR." (\{getBaseBV()})";
            }
        }
        tinfo += STR." // Exp: \{getPilot().getExperience()} // Kills: \{getPilot().getKills()}<br> ";

        if (getPilot().getSkills().size() > 0) {
            tinfo += "Skills: ";
            tinfo += getPilot().getSkillString(
                  false,
                  client.getData()
                        .getHouseByName(client.getPlayer().getHouse())
                        .getBasePilotSkill(getType()));
            tinfo += "<br>";
        }

        if (getPilot().getHits() > 0) {
            tinfo += STR."Hits: \{getPilot().getHits()}<br>";
        }

        if (!armyText.isEmpty()) {
            tinfo += STR."\{armyText}<br>";
        }

        String capacity = getEntity().getUnusedString();

        if ((capacity != null) && (!capacity.trim().isEmpty())) {
            if (Boolean.parseBoolean(client.getServerConfigs("UseFullCapacityDescription"))) {
                if (capacity.endsWith("<br>")) {
                    capacity = capacity.substring(0, capacity.length() - 4);
                }

                if (capacity.contains("<br>")) {
                    tinfo += STR."Cargo:<br>\{capacity}<br>";
                } else {
                    tinfo += STR."Cargo: \{capacity}<br>";
                }
            } else if (capacity.startsWith("Troops")) {
                capacity = capacity.substring(9);// strip "Troops - " from
                // string
                tinfo += STR."Cargo: \{capacity}<br>";
            }
        }

        if (getLifeTimeRepairCost() > 0) {
            tinfo += STR."Repair Costs: \{getCurrentRepairCost()}/\{getLifeTimeRepairCost()}<br>";
        }
        tinfo += getProducer();

        if ((scrappableFor > 0)
                  && !Boolean.parseBoolean(client.getServerConfigs("UseAdvanceRepair"))
                  && !Boolean.parseBoolean(client.getServerConfigs("UseSimpleRepair"))) {
            tinfo += STR."<br><br><b>Scrap Value: \{client.moneyOrFluMessage(true, false, scrappableFor)}</b>";
        }

        tinfo += "</body></html>";
        return (tinfo);
    }

    public String getModelName() {

        if (getType() != MEK) {
            return (STR."\{getEntity().getChassis()} \{getEntity().getModel()}").trim();
        }

        if (getEntity().isOmni()) {
            return (STR."\{getEntity().getChassis()} \{getEntity().getModel()}").trim();
        }

        if (!getEntity().getModel().trim().isEmpty()) {
            return getEntity().getModel().trim();
        }

        // else
        return getEntity().getChassis().trim();

    }

    public int getBV() {
        return Math.max(BV, 0);
    }

    public int getBaseBV() {
        return getEntity().calculateBattleValue(false, true);
    }

    public int getBVForMatch() {
        if (Boolean.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"))) {
            return getBaseBV();
        }
        return getBV();
    }

    public Entity getEntity() {
        return unitEntity;
    }

    /**
     * Tries to set UnitEntity from the global MekFileName
     */
    public void createEntity() {
        unitEntity = UnitUtils.createEntity(getUnitFilename());

        if (unitEntity == null) {
            MWLogger.errLog("Error unit failed to load. Exiting.");
            System.exit(1);
        }

        unitEntity.setCrew(UnitUtils.createEntityPilot(this));

        if (unitEntity.getChassis().equals("Error")) {
            setProducer(STR."Unable to find \{getUnitFilename()} on clients system!");
        }
        getC3Type(unitEntity);
    }

    public boolean isOmni() {
        boolean isOmni = getEntity().isOmni();
        String targetChassis = getEntity().getChassis();

        if ((getType() == Unit.VEHICLE) && !isOmni) {
            try {
                FileInputStream fis = new FileInputStream("./data/mechfiles/omnivehiclelist.txt");
                BufferedReader dis = new BufferedReader(new InputStreamReader(
                      fis));
                while (dis.ready()) {
                    String chassis = dis.readLine();
                    // check to see if the chassis listed in the file match
                    // omni vehicle chassis.
                    if (targetChassis.equalsIgnoreCase(chassis)) {
                        dis.close();
                        fis.close();
                        return true;
                    }
                }
                dis.close();
                fis.close();
            } catch (Exception ex) {

            }
        }

        return isOmni;
    }

    public int getOriginalBV() {
        return unitEntity.calculateBattleValue(false, false);
    }

    public void applyRepairs(String data) {
        createEntity();
        UnitUtils.applyBattleDamage(unitEntity, data, true);
    }

    public boolean getPilotIsRepairing() {
        return pilotIsRepairing;
    }

    // STATIC METHODS

    /**
     * A method which returns the MU cost of a specified campaign unit.
     *
     * @return int - # of MU it takes to buy a unit of the given weight class
     */
    public static int getPriceForUnit(IClient client, int weightClass,
          int type_id, House producer) {

        int result = Integer.MAX_VALUE;
        try {
            String classType = Unit.getWeightClassDesc(weightClass) + Unit.getTypeClassDesc(type_id) + "Price";

            if (type_id == Unit.MEK) {
                result = Integer.parseInt(client.getServerConfigs(Unit.getWeightClassDesc(weightClass) + "Price"));
            } else {
                result = Integer.parseInt(client.getServerConfigs(classType));
            }

            // modify the result by the faction price modifier
            result += producer.getHouseUnitPriceMod(type_id, weightClass);

            // dont allow negative pricing
            if (result < 0) {
                result = 0;
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        return result;
    }// end getPriceForCUnit()

    /**
     * A method which returns the influence cost of a specified campaign mech.
     *
     * @return int - # if IP it takes to buy a mech of the given units weight class
     */
    public static int getInfluenceForUnit(IClient mwclient, int weightClass, int type_id, House producer) {

        int result;
        String classType = Unit.getWeightClassDesc(weightClass) + Unit.getTypeClassDesc(type_id) + "Inf";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(mwclient.getServerConfigs(Unit.getWeightClassDesc(weightClass) + "Inf"));
        } else {
            result = Integer.parseInt(mwclient.getServerConfigs(classType));
        }

        // modify the result by the faction price modifier
        result += producer.getHouseUnitFluMod(type_id, weightClass);

        // dont allow negative pricing
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    /**
     * A method which returns the PP COST of a unit. Meks and Vehicles are segregated by weightClass. Infantry are flat
     * priced accross
     * <p>
     * all weight classes. @ param weight - the weight class to be checked @ return int - the PP cost
     */
    public static int getPPForUnit(IClient client, int weightClass,
          int type_id, House producer) {

        int result;
        String classType = Unit.getWeightClassDesc(weightClass) + Unit.getTypeClassDesc(type_id) + "PP";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(client.getServerConfigs(Unit.getWeightClassDesc(weightClass) + "PP"));
        } else {
            result = Integer.parseInt(client.getServerConfigs(classType));
        }

        // adjust PP cost by faction specific mod
        result += producer.getHouseUnitComponentMod(type_id, weightClass);

        // dont allow a unit to consume negative PP
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    public static double getArmorCost(Entity unit, IClient client, int location) {
        double cost;

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"))) {
            return 0;
        }

        String armorCost = "CostPoint" + UnitUtils.getArmorShortName(unit, location);
        cost = Double.parseDouble(client.getServerConfigs(armorCost));

        return cost;
    }

    public static double getStructureCost(Entity unit, IClient client) {
        double cost;

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"))) {
            return 0;
        }

        String armorCost = "CostPoint" + UnitUtils.getInternalShortName(unit) + "IS";
        cost = Double.parseDouble(client.getServerConfigs(armorCost));

        return cost;
    }

    public void setAntiAir(boolean aa) {
        Quirks quirks = unitEntity.getQuirks();
        quirks.getOption("anti_air").setValue(aa);
    }

    public void setTargetSystem(int type) {
        try {
            targetSystem.setTargetSystem(type);
        } catch (TargetTypeOutOfBoundsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static double getCritCost(Entity unit, IClient client,
          CriticalSlot crit) {
        double cost;

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"))) {
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
            cost = Double.parseDouble(client.getServerConfigs("EngineCritRepairCost"));
        } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (crit.isMissing()) {
                cost = Double.parseDouble(client.getServerConfigs("SystemCritReplaceCost"));
            } else {
                cost = Double.parseDouble(client.getServerConfigs("SystemCritRepairCost"));
            }
        } else {
            Mounted<?> mounted = crit.getMount();

            if (mounted.getType() instanceof WeaponType weapon) {
                if (weapon.hasFlag(WeaponType.F_ENERGY)) {
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client.getServerConfigs("EnergyWeaponCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client.getServerConfigs("EnergyWeaponCritRepairCost"));
                    }
                } else if (weapon.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client.getServerConfigs("BallisticCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client.getServerConfigs("BallisticCritRepairCost"));
                    }
                } else if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client.getServerConfigs("MissileCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client.getServerConfigs("MissileCritRepairCost"));
                    }
                } else // use the misc eq costs.
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client.getServerConfigs("EquipmentCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client.getServerConfigs("EquipmentCritRepairCost"));
                    }
            } else // use the misc eq costs.
                if (crit.isMissing()) {
                    cost = Double.parseDouble(client.getServerConfigs("EquipmentCritReplaceCost"));
                } else {
                    cost = Double.parseDouble(client.getServerConfigs("EquipmentCritRepairCost"));
                }
        }

        cost = Math.max(cost, 1);
        return cost;
    }

    public String getTargetSystemTypeDesc() {
        // TODO Auto-generated method stub
        return targetSystem.getCurrentTypeName();
    }

    public TargetSystem getTargetSystem() {
        return targetSystem;
    }
}// end CUnit.java
