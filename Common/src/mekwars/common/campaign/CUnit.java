/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
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


package mekwars.common.campaign;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.List;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import megamek.client.generator.RandomGenderGenerator;
import megamek.codeUtilities.MathUtility;
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
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.MegaMekPilotOption;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.campaign.targetsystems.TargetSystem;
import mekwars.common.campaign.targetsystems.TargetTypeOutOfBoundsException;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

/**
 * Class for unit object used by client
 */
public class CUnit extends Unit {
    private static final MMLogger LOGGER = MMLogger.create(CUnit.class);

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

    // PRIVATE METHODS
    private void init() {
        unitEntity = null;
        BV = 0;
        setStatus(STATUS_OK);
        setProducer("unknown origin");
    }

    public CUnit(IClient client) {
        this.client = client;
        init();
    }

    /**
     * A method that returns the MU cost of a specified campaign unit.
     *
     * @return int - # of MU it takes to buy a unit of the given weight class
     */
    public static int getPriceForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;

        String classType = STR."\{Unit.getWeightClassDesc(weightClass)}\{Unit.getTypeClassDesc(type_id)}Price";

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(STR."\{Unit.getWeightClassDesc(weightClass)}Price"),
                  0);
        } else {
            result = MathUtility.parseInt(client.getServerConfigs(classType), 0);
        }

        // modify the result by the faction price modifier
        result += producer.getHouseUnitPriceMod(type_id, weightClass);

        // dont allow negative pricing
        if (result < 0) {
            result = 0;
        }
        return result;
    }// end getPriceForCUnit()

    /**
     * A method that returns the influence cost of a specified campaign mech.
     *
     * @return int - # if IP it takes to buy a mech of the given units weight class
     */
    public static int getInfluenceForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;
        String classType = STR."\{Unit.getWeightClassDesc(weightClass)}\{Unit.getTypeClassDesc(type_id)}Inf";

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(STR."\{Unit.getWeightClassDesc(weightClass)}Inf"), 0);
        } else {
            result = MathUtility.parseInt(client.getServerConfigs(classType), 0);
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
     * A method that returns the PP COST of a unit. Meks and Vehicles are segregated by weightClass. Infantry are flat-
     * * priced across
     * <p>
     * all weight classes. @ param weight - the weight class to be checked @ return int - the PP cost
     */
    public static int getPPForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;
        String classType = STR."\{Unit.getWeightClassDesc(weightClass)}\{Unit.getTypeClassDesc(type_id)}PP";

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(STR."\{Unit.getWeightClassDesc(weightClass)}PP"), 0);
        } else {
            result = MathUtility.parseInt(client.getServerConfigs(classType), 0);
        }

        // adjust PP cost by faction-specific mod
        result += producer.getHouseUnitComponentMod(type_id, weightClass);

        // don't allow a unit to consume negative PP
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    public static double getArmorCost(Entity unit, IClient client, int location) {
        double cost;

        if (MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false)) {
            return 0;
        }

        String armorCost = STR."CostPoint\{UnitUtils.getArmorShortName(unit, location)}";
        cost = MathUtility.parseDouble(client.getServerConfigs(armorCost), 0.0);

        return cost;
    }

    public static double getStructureCost(Entity unit, IClient client) {
        double cost;

        if (MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false)) {
            return 0;
        }

        String armorCost = STR."CostPoint\{UnitUtils.getInternalShortName(unit)}IS";
        cost = MathUtility.parseDouble(client.getServerConfigs(armorCost), 0.0);

        return cost;
    }

    public static double getCritCost(Entity unit, IClient client, CriticalSlot crit) {
        double cost;

        if (MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false)) {
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
            cost = MathUtility.parseDouble(client.getServerConfigs("EngineCritRepairCost"), 0.0);
        } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (crit.isMissing()) {
                cost = MathUtility.parseDouble(client.getServerConfigs("SystemCritReplaceCost"), 0.0);
            } else {
                cost = MathUtility.parseDouble(client.getServerConfigs("SystemCritRepairCost"), 0.0);
            }
        } else {
            Mounted<?> mounted = crit.getMount();

            if (mounted.getType() instanceof WeaponType weapon) {
                if (weapon.hasFlag(WeaponType.F_ENERGY)) {
                    if (crit.isMissing()) {
                        cost = MathUtility.parseDouble(client.getServerConfigs("EnergyWeaponCritReplaceCost"), 0.0);
                    } else {
                        cost = MathUtility.parseDouble(client.getServerConfigs("EnergyWeaponCritRepairCost"), 0.0);
                    }
                } else if (weapon.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (crit.isMissing()) {
                        cost = MathUtility.parseDouble(client.getServerConfigs("BallisticCritReplaceCost"), 0.0);
                    } else {
                        cost = MathUtility.parseDouble(client.getServerConfigs("BallisticCritRepairCost"), 0.0);
                    }
                } else if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                    if (crit.isMissing()) {
                        cost = MathUtility.parseDouble(client.getServerConfigs("MissileCritReplaceCost"), 0.0);
                    } else {
                        cost = MathUtility.parseDouble(client.getServerConfigs("MissileCritRepairCost"), 0.0);
                    }
                } else // use the misc eq costs.
                    if (crit.isMissing()) {
                        cost = MathUtility.parseDouble(client.getServerConfigs("EquipmentCritReplaceCost"), 0.0);
                    } else {
                        cost = MathUtility.parseDouble(client.getServerConfigs("EquipmentCritRepairCost"), 0.0);
                    }
            } else // use the misc eq costs.
                if (crit.isMissing()) {
                    cost = MathUtility.parseDouble(client.getServerConfigs("EquipmentCritReplaceCost"), 0.0);
                } else {
                    cost = MathUtility.parseDouble(client.getServerConfigs("EquipmentCritRepairCost"), 0.0);
                }
        }

        cost = Math.max(cost, 1);
        return cost;
    }

    public String getHtmlQuirkList() {
        return htmlQuirkList;
    }

    public void setHtmlQuirkList(String htmlQuirkList) {
        this.htmlQuirkList = htmlQuirkList;
    }

    // PUBLIC METHODS
    public boolean setData(String data) {

        StringTokenizer stringTokenizer;
        String element;
        String unitDamage;
        LOGGER.info(STR."PDATA: \{data}");

        stringTokenizer = new StringTokenizer(data, "$");
        element = TokenReader.readString(stringTokenizer);

        if (!element.equals("CM")) {
            return (false);
        }

        setUnitFilename(TokenReader.readString(stringTokenizer));
        setId((TokenReader.readInt(stringTokenizer)));
        setStatus(TokenReader.readInt(stringTokenizer));

        setProducer(TokenReader.readString(stringTokenizer));
        String pilotName;
        int gunnery;
        int piloting;
        int exp;
        Pilot pilot;

        StringTokenizer STR = new StringTokenizer(TokenReader.readString(stringTokenizer), "#");
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

        int mmOptionsAmount = TokenReader.readInt(stringTokenizer);

        for (int i = 0; i < mmOptionsAmount; i++) {
            MegaMekPilotOption mo = new MegaMekPilotOption(
                  TokenReader.readString(stringTokenizer),
                  MathUtility.parseBoolean(TokenReader.readString(stringTokenizer), false));
            pilot.addMegaMekOption(mo);
        }

        setType(TokenReader.readInt(stringTokenizer));
        setPilot(pilot);
        BV = Math.max(TokenReader.readInt(stringTokenizer), 0);

        setWeightClass(TokenReader.readInt(stringTokenizer));
        setId(TokenReader.readInt(stringTokenizer));

        createEntity();

        if (unitEntity == null) {
            LOGGER.error("Cannot load entity!");
            return false;
        }

        // don't try to set ammo and eject on an OMG
        if (getModelName().startsWith("Error") || getModelName().startsWith("OMG")) {
            unitEntity.setExternalId(getId());
            unitEntity.setCrew(new Crew(CrewType.SINGLE,
                  pilot.getName(),
                  1,
                  pilot.getGunnery(),
                  pilot.getPiloting(),
                  RandomGenderGenerator.generate(),
                  false,
                  null));
            return true;
        }

        // set auto eject if it's a Mek
        if ((unitEntity instanceof Mek mek) && stringTokenizer.hasMoreElements()) {
            mek.setAutoEject(MathUtility.parseBoolean(TokenReader.readString(stringTokenizer), false));
        }

        // then set up ammo loadout
        {
            try {
                int maxCrits = TokenReader.readInt(stringTokenizer);
                List<AmmoMounted> entityAmmo = unitEntity.getAmmo();
                for (int count = 0; count < maxCrits; count++) {
                    AmmoType.AmmoTypeEnum weaponType = AmmoType.AmmoTypeEnum.fromIndex(TokenReader.readInt(
                          stringTokenizer));

                    String ammoName = TokenReader.readString(stringTokenizer);
                    int shots = TokenReader.readInt(stringTokenizer);
                    boolean hotLoaded = TokenReader.readBoolean(stringTokenizer);

                    AmmoMounted mWeapon = entityAmmo.get(count);

                    AmmoType ammoType = getEntityAmmo(weaponType, ammoName);
                    mWeapon.changeAmmoType(ammoType);
                    mWeapon.setShotsLeft(shots);
                    mWeapon.setHotLoad(hotLoaded);
                }
            } catch (Exception ex) {
                LOGGER.debug(ex, "Error setting ammo");
                return true;
            }
        }// end ammo

        // set up rapid fire Machine guns, if any
        {
            int maxMachineGuns = TokenReader.readInt(stringTokenizer);
            for (int count = 0; count < maxMachineGuns; count++) {
                int location = TokenReader.readInt(stringTokenizer);
                int slot = TokenReader.readInt(stringTokenizer);
                boolean selection = TokenReader.readBoolean(stringTokenizer);
                CriticalSlot criticalSlot = unitEntity.getCritical(location, slot);

                Mounted<?> mg = criticalSlot.getMount();

                mg.setRapidFire(selection);

            }
        }// Machine Guns

        TokenReader.readString(stringTokenizer);// unused

        targetSystem.setEntity(unitEntity);
        try {
            targetSystem.setTargetSystem(TokenReader.readInt(stringTokenizer));
        } catch (TargetTypeOutOfBoundsException e) {
            LOGGER.error("Error setting target system within setData");
        }

        int suppUnit = TokenReader.readInt(stringTokenizer);
        setSupportUnit(suppUnit == 1);

        scrappableFor = TokenReader.readInt(stringTokenizer);

        unitDamage = TokenReader.readString(stringTokenizer);

        pilotIsRepairing = TokenReader.readBoolean(stringTokenizer);

        setRepairCosts(TokenReader.readInt(stringTokenizer), TokenReader.readInt(stringTokenizer));

        setChristmasUnit(TokenReader.readBoolean(stringTokenizer));

        //@salient Quirks - set unit quirks, or drop data if quirks have been turned off
        if (stringTokenizer.hasMoreTokens() && MathUtility.parseBoolean(client.getServerConfigs("EnableQuirks"),
              false)) {
            setUnitQuirks(TokenReader.readString(stringTokenizer));
        } else if (stringTokenizer.hasMoreTokens()) {
            TokenReader.readString(stringTokenizer);
        }

        unitEntity.setExternalId(getId());

        UnitUtils.applyBattleDamage(unitEntity, unitDamage, true);

        getC3Type(unitEntity);

        return true;
    }

    /**
     * Tries to set UnitEntity from the global MekFileName
     */
    public void createEntity() {
        unitEntity = UnitUtils.createEntity(getUnitFilename());
        unitEntity.setCrew(UnitUtils.createEntityPilot(this));

        if (unitEntity.getChassis().equals("Error")) {
            setProducer(STR."Unable to find \{getUnitFilename()} on clients system!");
        }

        getC3Type(unitEntity);
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

    //@salient this method is only accessible when quirks are enabled.
    private void setUnitQuirks(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, "!");
        if (stringTokenizer.hasMoreTokens()) {
            htmlQuirkList = TokenReader.readString(stringTokenizer);
            quirkList = TokenReader.readString(stringTokenizer);
        }

        if (quirkList != null) {
            stringTokenizer = new StringTokenizer(quirkList, "&");
            while (stringTokenizer.hasMoreTokens()) {
                String quirk = TokenReader.readString(stringTokenizer);
                if (!quirk.equalsIgnoreCase("none")) {
                    unitEntity.getQuirks().getOption(quirk).setValue(true);
                }

            }
        }

    }

    public Entity getEntity() {
        return unitEntity;
    }

    public String getHtmlQuirksList() {
        return htmlQuirkList;
    }

    public String getQuirksList() {
        return quirkList;
    }

    //@salient debug method, I really just used this once to make sure the quirks were being set,
    //but I'll leave it in case one day someone needs it.
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
     * Method that generates data for an auto unit. Since auto units have no unique properties, these can be assembled
     * client side rather than sent from the server.
     *
     * @author urgru 1/4/05
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
            return STR."\{getModelName()} [\{getPilot().getGunnery()}/\{getPilot().getPiloting()}]";
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
        String targetInfo;

        if ((getType() == Unit.MEK) && !unitEntity.isOmni()) {
            targetInfo = STR."<html><body>#\{getId()} \{unitEntity.getChassis()}, \{getModelName()}";
        } else {
            targetInfo = STR."<html><body>#\{getId()} \{getModelName()}";
        }

        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            targetInfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}/\{getPilot().getPiloting()}) <br>";
        } else if ((getType() == Unit.BATTLEARMOR) || (getType() == Unit.INFANTRY)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                targetInfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}/\{getPilot().getPiloting()}) <br>";
            } else {
                targetInfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}) <br>";
            }
        } else {
            targetInfo += STR." (\{getPilot().getName()}, \{getPilot().getGunnery()}) <br>";
        }

        if (getType() == Unit.VEHICLE) {
            targetInfo += STR." Movement: \{getEntity().getMovementModeAsString()}<br>";
        }

        targetInfo += "BV: ";

        if (MathUtility.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"), false)) {
            targetInfo += getBaseBV();
        } else {
            targetInfo += BV;
        }

        if (MathUtility.parseBoolean(client.getConfigParam("ShowUnitBaseBV"), false)) {
            if (getBV() != getBaseBV()) {
                targetInfo += STR." (\{getBaseBV()})";
            }
        }
        targetInfo += STR." // Exp: \{getPilot().getExperience()} // Kills: \{getPilot().getKills()}<br> ";

        if (getPilot().getSkills().size() > 0) {
            House house = client.getData().getHouseByName(client.getPlayer().getHouse());

            if (house != null) {
                targetInfo += "Skills: ";
                targetInfo += getPilot().getSkillString(
                      false,
                      house.getBasePilotSkill(getType()));
            }
            targetInfo += "<br>";
        }

        if (getPilot().getHits() > 0) {
            targetInfo += STR."Hits: \{getPilot().getHits()}<br>";
        }

        if (!armyText.isEmpty()) {
            targetInfo += STR."\{armyText}<br>";
        }

        String capacity = getEntity().getUnusedString();

        if ((capacity != null) && (!capacity.trim().isEmpty())) {
            if (MathUtility.parseBoolean(client.getServerConfigs("UseFullCapacityDescription"), false)) {
                if (capacity.endsWith("<br>")) {
                    capacity = capacity.substring(0, capacity.length() - 4);
                }

                if (capacity.contains("<br>")) {
                    targetInfo += STR."Cargo:<br>\{capacity}<br>";
                } else {
                    targetInfo += STR."Cargo: \{capacity}<br>";
                }
            } else if (capacity.startsWith("Troops")) {
                capacity = capacity.substring(9);// strip "Troops -" from
                // string
                targetInfo += STR."Cargo: \{capacity}<br>";
            }
        }

        if (getLifeTimeRepairCost() > 0) {
            targetInfo += STR."Repair Costs: \{getCurrentRepairCost()}/\{getLifeTimeRepairCost()}<br>";
        }
        targetInfo += getProducer();

        if ((scrappableFor > 0)
                  && !MathUtility.parseBoolean(client.getServerConfigs("UseAdvanceRepair"), false)
                  && !MathUtility.parseBoolean(client.getServerConfigs("UseSimpleRepair"), false)) {
            targetInfo += STR."<br><br><b>Scrap Value: \{client.moneyOrFluMessage(true, false, scrappableFor)}</b>";
        }

        targetInfo += "</body></html>";
        return (targetInfo);
    }

    public int getBaseBV() {
        return getEntity().calculateBattleValue(false, true);
    }

    // STATIC METHODS

    public int getBV() {
        return Math.max(BV, 0);
    }

    public int getBVForMatch() {
        if (MathUtility.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"), false)) {
            return getBaseBV();
        }
        return getBV();
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
                LOGGER.error(ex, "Error reading omnivehiclelist.txt");
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

    public void setAntiAir(boolean aa) {
        Quirks quirks = unitEntity.getQuirks();
        quirks.getOption("anti_air").setValue(aa);
    }

    public String getTargetSystemTypeDesc() {
        // TODO Auto-generated method stub
        return targetSystem.getCurrentTypeName();
    }

    public TargetSystem getTargetSystem() {
        return targetSystem;
    }

    public void setTargetSystem(int type) {
        try {
            targetSystem.setTargetSystem(type);
        } catch (TargetTypeOutOfBoundsException e) {
            LOGGER.error("Error setting target system");
        }
    }
}// end CUnit.java
