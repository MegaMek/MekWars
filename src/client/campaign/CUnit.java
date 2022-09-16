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

package client.campaign;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.StringJoiner;
import java.util.StringTokenizer;

import client.MWClient;
import common.House;
import common.MegaMekPilotOption;
import common.Unit;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.campaign.targetsystems.TargetSystem;
import common.campaign.targetsystems.TargetTypeNotImplementedException;
import common.campaign.targetsystems.TargetTypeOutOfBoundsException;
import common.util.MWLogger;
import common.util.TokenReader;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.CrewType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.OffBoardDirection;
import megamek.common.WeaponType;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import megamek.common.options.Quirks;

/**
 * Class for unit object used by Client
 */
public class CUnit extends Unit {

    // VARIABLES
    protected Entity unitEntity;

    private int BV;
    private int scrappableFor = 0;// value if scrapped
    private boolean pilotIsRepairing = false;
    private MWClient mwclient;
    private String htmlQuirkList = " ";
    private String quirkList = " ";

    // CONSTRUCTORS
    public CUnit() {
        init();
    }

    public CUnit(MWClient mwclient) {
        this.mwclient = mwclient;
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
        String pilotname = "John Denver";
        int gunnery = 4;
        int piloting = 5;
        int exp = 0;
        Pilot p = null;
        StringTokenizer STR = new StringTokenizer(TokenReader.readString(ST),
                "#");
        pilotname = TokenReader.readString(STR);
        exp = TokenReader.readInt(STR);
        gunnery = TokenReader.readInt(STR);
        piloting = TokenReader.readInt(STR);
        p = new Pilot(pilotname, gunnery, piloting);
        p.setExperience(exp);
        int skillAmount = TokenReader.readInt(STR);
        for (int i = 0; i < skillAmount; i++) {
            PilotSkill skill = new PilotSkill(TokenReader.readInt(STR),
                    TokenReader.readString(STR), TokenReader.readInt(STR),
                    TokenReader.readString(STR));

            if (skill.getName().equals("Weapon Specialist")) {
                p.setWeapon(TokenReader.readString(STR));
            }

            if (skill.getName().equals("Trait")) {
                p.setTraitName(TokenReader.readString(STR));
            }

            if (skill.getName().equals("Edge")) {
                p.setTac(TokenReader.readBoolean(STR));
                p.setKO(TokenReader.readBoolean(STR));
                p.setHeadHit(TokenReader.readBoolean(STR));
                p.setExplosion(TokenReader.readBoolean(STR));
            }
            p.getSkills().add(skill);
        }

        p.setKills(TokenReader.readInt(STR));

        p.setHits(TokenReader.readInt(STR));

        int mmoptionsamount = TokenReader.readInt(ST);
        for (int i = 0; i < mmoptionsamount; i++) {
            MegaMekPilotOption mo = new MegaMekPilotOption(
                    TokenReader.readString(ST),
                    Boolean.parseBoolean(TokenReader.readString(ST)));
            p.addMegamekOption(mo);
        }

        setType(TokenReader.readInt(ST));
        // setType(getEntityType(UnitEntity));
        setPilot(p);
        BV = Math.max(TokenReader.readInt(ST), 0);

        setWeightclass(TokenReader.readInt(ST));
        // if (this.getType() == Unit.MEK || this.getType() == Unit.VEHICLE)
        // setWeightclass(getEntityWeight(UnitEntity));
        setId(TokenReader.readInt(ST));

        createEntity();
        if (unitEntity == null) {
            MWLogger.errLog("Cannot load entity!");
            return (false);
        }

        // don't try to set ammo and eject on an OMG
        if (getModelName().startsWith("Error")
                || getModelName().startsWith("OMG")) {
            unitEntity.setExternalId(getId());
            unitEntity.setCrew(new megamek.common.Crew(CrewType.SINGLE, p.getName(), 1, p
                    .getGunnery(), p.getPiloting()));
            return true;
        }

        // set autoeject if its a mech
        if ((unitEntity instanceof Mech) && ST.hasMoreElements()) {
            ((Mech) unitEntity).setAutoEject(Boolean.parseBoolean(TokenReader
                    .readString(ST)));
        }

        // then set up ammo loadout
        {
            try {
                int maxCrits = TokenReader.readInt(ST);
                ArrayList<Mounted> e = unitEntity.getAmmo();
                for (int count = 0; count < maxCrits; count++) {
                    int weaponType = TokenReader.readInt(ST);
                    String ammoName = TokenReader.readString(ST);
                    int shots = TokenReader.readInt(ST);
                    boolean hotloaded = TokenReader.readBoolean(ST);

                    Mounted mWeapon = e.get(count);

                    AmmoType at = getEntityAmmo(weaponType, ammoName);
                    mWeapon.changeAmmoType(at);
                    mWeapon.setShotsLeft(shots);
                    mWeapon.setHotLoad(hotloaded);
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
                CriticalSlot cs = unitEntity.getCritical(location, slot);

                Mounted mg = cs.getMount();

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
        } catch (TargetTypeNotImplementedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int suppUnit = TokenReader.readInt(ST);
        if (suppUnit == 1) {
            setSupportUnit(true);
        } else {
            setSupportUnit(false);
        }

        scrappableFor = TokenReader.readInt(ST);

        unitDamage = TokenReader.readString(ST);

        pilotIsRepairing = TokenReader.readBoolean(ST);

        setRepairCosts(TokenReader.readInt(ST), TokenReader.readInt(ST));

        setChristmasUnit(TokenReader.readBoolean(ST));
        
        //@salient Quirks - set unit quirks, or drop data if quirks have been turned off
        if(ST.hasMoreTokens() && Boolean.parseBoolean(mwclient.getserverConfigs("EnableQuirks")))
        	setUnitQuirks(TokenReader.readString(ST));
        else if (ST.hasMoreTokens())
        	TokenReader.readString(ST);
        
        unitEntity.setExternalId(getId());

        if (unitDamage != null) {
            UnitUtils.applyBattleDamage(unitEntity, unitDamage, true);
        }

        getC3Type(unitEntity);

        return (true);
    }
    
    //@salient this method is only accessible when quirks are enabled.
    private void setUnitQuirks(String data) 
    {
		StringTokenizer st = new StringTokenizer(data, "!");
		if(st.hasMoreTokens())
		{
			htmlQuirkList = TokenReader.readString(st);
			quirkList = TokenReader.readString(st);
		}
		
		if(quirkList != null)
		{
			st = new StringTokenizer(quirkList, "&");
			while(st.hasMoreTokens())
			{
				String quirk = TokenReader.readString(st);
				if(quirk.equalsIgnoreCase("none") == false)
					unitEntity.getQuirks().getOption(quirk).setValue(true);
				//mwclient.sendChat(quirk); //debug
			}   		
		}   		
			
	}
    
    public String getHtmlQuirksList()
    {
    	return htmlQuirkList;
    }
    
    public String getQuirksList()
    {
    	return quirkList;
    }
    
    //@salient debug method, i really just used this once to make sure the quirks were being set
    //but i'll leave it in case one day someone needs it.
    public String quirkCheck()
    {
		StringJoiner quirksList = new StringJoiner("&");
        
        for (Enumeration<IOptionGroup> optionGroups = unitEntity.getQuirks().getGroups(); optionGroups.hasMoreElements();) 
        {
          IOptionGroup group = optionGroups.nextElement();
          if (unitEntity.getQuirks().count(group.getKey()) > 0) 
          {
            for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) 
            {
              IOption option = options.nextElement();
              if (option != null && option.booleanValue()) 
              {
                quirksList.add(option.getName());
              }
            }
          }
        }
		return quirksList.toString();
    }
    
    public boolean hasQuirks()
    {
		//StringJoiner quirksList = new StringJoiner("&");
        
        for (Enumeration<IOptionGroup> optionGroups = unitEntity.getQuirks().getGroups(); optionGroups.hasMoreElements();) 
        {
          IOptionGroup group = optionGroups.nextElement();
          if (unitEntity.getQuirks().count(group.getKey()) > 0) 
          {
            for (Enumeration<IOption> options = group.getOptions(); options.hasMoreElements();) 
            {
              IOption option = options.nextElement();
              if (option != null && option.booleanValue()) 
              {
                return true;
              }
            }
          }
        }
		return false;
    }

	/**
     * Method which generates data for an auto unit. Since auto units have no
     * unique properties this can be assembled client side rather than sent from
     * the server.
     *
     * @urgru 1/4/05
     */
    public void setAutoUnitData(String filename, int distance,
            OffBoardDirection edge) {
        setUnitFilename(filename);
        // setProducer("Autounit");
        setPilot(new Pilot("Autopilot", 4, 5));
        // setType(Unit.VEHICLE);//auto units are always vehs ...
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
        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE)
                || (getType() == Unit.AERO)) {
            return getModelName() + " [" + getPilot().getGunnery() + "/"
                    + getPilot().getPiloting() + "]";
        }

        if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                return getModelName() + " [" + getPilot().getGunnery() + "/"
                        + getPilot().getPiloting() + "]";
            }
            return getModelName() + " [" + getPilot().getGunnery() + "]";
        }
        return getModelName() + " [" + getPilot().getGunnery() + "]";
    }

    public String getDisplayInfo(String armyText) {
        String tinfo = "";

        if ((getType() == Unit.MEK) && !unitEntity.isOmni()) {
            tinfo = "<html><body>#" + getId() + " " + unitEntity.getChassis()
                    + ", " + getModelName();
        } else {
            tinfo = "<html><body>#" + getId() + " " + getModelName();
        }

        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE)
                || (getType() == Unit.AERO)) {
            tinfo += " (" + getPilot().getName() + ", "
                    + getPilot().getGunnery() + "/" + getPilot().getPiloting()
                    + ") <br>";
        } else if ((getType() == Unit.BATTLEARMOR)
                || (getType() == Unit.INFANTRY)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                tinfo += " (" + getPilot().getName() + ", "
                        + getPilot().getGunnery() + "/"
                        + getPilot().getPiloting() + ") <br>";
            } else {
                tinfo += " (" + getPilot().getName() + ", "
                        + getPilot().getGunnery() + ") <br>";
            }
        } else {
            tinfo += " (" + getPilot().getName() + ", "
                    + getPilot().getGunnery() + ") <br>";
        }

        if (getType() == Unit.VEHICLE) {
            tinfo += " Movement: " + getEntity().getMovementModeAsString()
                    + "<br>";
        }

        tinfo += "BV: ";

        if (Boolean.parseBoolean(mwclient
                .getserverConfigs("UseBaseBVForMatching"))) {
            tinfo += getBaseBV();
        } else {
            tinfo += BV;
        }

        // if (Boolean.parseBoolean(mwclient.getserverConfigs("RIGHTHERE")))
        if (Boolean.parseBoolean(mwclient.getConfigParam("ShowUnitBaseBV"))) {
            if (getBV() != getBaseBV()) {
                tinfo += " (" + getBaseBV() + ")";
            }
        }
        tinfo += " // Exp: " + getPilot().getExperience() + " // Kills: "
                + getPilot().getKills() + "<br> ";

        if (getPilot().getSkills().size() > 0) {
            tinfo += "Skills: ";
            /*
             * Iterator it = getPilot().getSkills().getSkillIterator(); while
             * (it.hasNext()) { tinfo += ((PilotSkill) it.next()).getName(); if
             * (it.hasNext()) tinfo += ", "; }
             */
            tinfo += getPilot().getSkillString(
                    false,
                    mwclient.getData()
                            .getHouseByName(mwclient.getPlayer().getHouse())
                            .getBasePilotSkill(getType()));
            tinfo += "<br>";
        }

        if (getPilot().getHits() > 0) {
            tinfo += "Hits: " + Integer.toString(getPilot().getHits()) + "<br>";
        }

        if (!armyText.equals("")) {
            tinfo += armyText + "<br>";
        }

        String capacity = getEntity().getUnusedString();

        if ((capacity != null) && (capacity.trim().length() > 0)) {
            if (Boolean.parseBoolean(mwclient
                    .getserverConfigs("UseFullCapacityDescription"))) {
                if (capacity.endsWith("<br>")) {
                    capacity = capacity.substring(0, capacity.length() - 4);
                }

                if (capacity.indexOf("<br>") > -1) {
                    tinfo += "Cargo:<br>" + capacity + "<br>";
                } else {
                    tinfo += "Cargo: " + capacity + "<br>";
                }
            } else if (capacity.startsWith("Troops")) {
                capacity = capacity.substring(9);// strip "Troops - " from
                                                 // string
                tinfo += "Cargo: " + capacity + "<br>";
            }
        }

        if (getLifeTimeRepairCost() > 0) {
            tinfo += "Repair Costs: " + getCurrentRepairCost() + "/"
                    + getLifeTimeRepairCost() + "<br>";
        }
        tinfo += getProducer();

        if ((scrappableFor > 0)
                && !Boolean.parseBoolean(mwclient
                        .getserverConfigs("UseAdvanceRepair"))
                && !Boolean.parseBoolean(mwclient
                        .getserverConfigs("UseSimpleRepair"))) {
            tinfo += "<br><br><b>Scrap Value: "
                    + mwclient.moneyOrFluMessage(true, false, scrappableFor)
                    + "</b>";
        }

        tinfo += "</body></html>";
        return (tinfo);
    }

    public String getModelName() {

        if (getType() != MEK) {
            return new String(getEntity().getChassis() + " "
                    + getEntity().getModel()).trim();
        }

        if (getEntity().isOmni()) {
            return new String(getEntity().getChassis() + " "
                    + getEntity().getModel()).trim();
        }

        if (getEntity().getModel().trim().length() > 0) {
            return getEntity().getModel().trim();
        }

        // else
        return getEntity().getChassis().trim();

    }

    public int getBV() {
        if (BV < 0) {
            return 0;
        }

        // else
        return BV;
    }

    public int getBaseBV() {
        return getEntity().calculateBattleValue(false, true);
    }

    public int getBVForMatch() {
        if (Boolean.parseBoolean(mwclient
                .getserverConfigs("UseBaseBVForMatching"))) {
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
        // MMClient.mwClientLog.clientErrLog("Filename: " + getUnitFilename());
        unitEntity = UnitUtils.createEntity(getUnitFilename());
        unitEntity.setCrew(UnitUtils.createEntityPilot(this));

        if (unitEntity == null) {
            MWLogger.errLog("Error unit failed to load. Exiting.");
            System.exit(1);
        }

        if (unitEntity.getChassis().equals("Error")) {
            setProducer("Unable to find " + getUnitFilename()
                    + " on clients system!");
        }
        // setType(getEntityType(UnitEntity));
        getC3Type(unitEntity);
    }

    public boolean isOmni() {
        boolean isOmni = getEntity().isOmni();
        String targetChassis = getEntity().getChassis();

        if ((getType() == Unit.VEHICLE) && !isOmni) {
            try {
                FileInputStream fis = new FileInputStream(
                        "./data/mechfiles/omnivehiclelist.txt");
                BufferedReader dis = new BufferedReader(new InputStreamReader(
                        fis));
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

    public boolean getPilotIsReparing() {
        return pilotIsRepairing;
    }

    // STATIC METHODS
    /**
     * A method which returns the MU cost of a specified campaign unit.
     *
     * @return int - # of MU it takes to buy a unit of the given weight class
     */
    public static int getPriceForUnit(MWClient mwclient, int weightclass,
            int type_id, House producer) {

        int result = Integer.MAX_VALUE;
        try {
            String classtype = Unit.getWeightClassDesc(weightclass)
                    + Unit.getTypeClassDesc(type_id) + "Price";

            if (type_id == Unit.MEK) {
                result = Integer.parseInt(mwclient.getserverConfigs(Unit
                        .getWeightClassDesc(weightclass) + "Price"));
            } else {
                result = Integer.parseInt(mwclient.getserverConfigs(classtype));
            }

            // modify the result by the faction price modifier
            result += producer.getHouseUnitPriceMod(type_id, weightclass);

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
     * @return int - # if IP it takes to buy a mech of the given units weight
     *         class
     */
    public static int getInfluenceForUnit(MWClient mwclient, int weightclass,
            int type_id, House producer) {

        int result = Integer.MAX_VALUE;
        String classtype = Unit.getWeightClassDesc(weightclass)
                + Unit.getTypeClassDesc(type_id) + "Inf";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(mwclient.getserverConfigs(Unit
                    .getWeightClassDesc(weightclass) + "Inf"));
        } else {
            result = Integer.parseInt(mwclient.getserverConfigs(classtype));
        }

        // modify the result by the faction price modifier
        result += producer.getHouseUnitFluMod(type_id, weightclass);

        // dont allow negative pricing
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    /**
     * A method which returns the PP COST of a unit. Meks and Vehicles are
     * segregated by weightclass. Infantry are flat priced accross
     *
     * all weight classes. @ param weight - the weight class to be checked @
     * return int - the PP cost
     */
    public static int getPPForUnit(MWClient mwclient, int weightclass,
            int type_id, House producer) {

        int result = Integer.MAX_VALUE;
        String classtype = Unit.getWeightClassDesc(weightclass)
                + Unit.getTypeClassDesc(type_id) + "PP";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(mwclient.getserverConfigs(Unit
                    .getWeightClassDesc(weightclass) + "PP"));
        } else {
            result = Integer.parseInt(mwclient.getserverConfigs(classtype));
        }

        // adjust PP cost by faction specific mod
        result += producer.getHouseUnitComponentMod(type_id, weightclass);

        // dont allow a unit to consume negative PP
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    public static double getArmorCost(Entity unit, MWClient client, int location) {
        double cost = 0.0;

        if (Boolean.parseBoolean(client.getserverConfigs("UsePartsRepair"))) {
            return 0;
        }

        String armorCost = "CostPoint"
                + UnitUtils.getArmorShortName(unit, location);
        cost = Double.parseDouble(client.getserverConfigs(armorCost));

        return cost;
    }

    public static double getStructureCost(Entity unit, MWClient client) {
        double cost = 0.0;

        if (Boolean.parseBoolean(client.getserverConfigs("UsePartsRepair"))) {
            return 0;
        }

        String armorCost = "CostPoint" + UnitUtils.getInternalShortName(unit)
                + "IS";
        cost = Double.parseDouble(client.getserverConfigs(armorCost));

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
        } catch (TargetTypeNotImplementedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static double getCritCost(Entity unit, MWClient client,
            CriticalSlot crit) {
        double cost = 0.0;

        if (Boolean.parseBoolean(client.getserverConfigs("UsePartsRepair"))) {
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
            cost = Double.parseDouble(client
                    .getserverConfigs("EngineCritRepairCost"));
        } else if (crit.getType() == CriticalSlot.TYPE_SYSTEM) {
            if (crit.isMissing()) {
                cost = Double.parseDouble(client
                        .getserverConfigs("SystemCritReplaceCost"));
            } else {
                cost = Double.parseDouble(client
                        .getserverConfigs("SystemCritRepairCost"));
            }
        } else {
            Mounted mounted = crit.getMount();

            if (mounted.getType() instanceof WeaponType) {
                WeaponType weapon = (WeaponType) mounted.getType();
                if (weapon.hasFlag(WeaponType.F_ENERGY)) {
                    if (crit.isMissing()) {
                        cost = Double
                                .parseDouble(client
                                        .getserverConfigs("EnergyWeaponCritReplaceCost"));
                    } else {
                        cost = Double
                                .parseDouble(client
                                        .getserverConfigs("EnergyWeaponCritRepairCost"));
                    }
                } else if (weapon.hasFlag(WeaponType.F_BALLISTIC)) {
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client
                                .getserverConfigs("BallisticCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client
                                .getserverConfigs("BallisticCritRepairCost"));
                    }
                } else if (weapon.hasFlag(WeaponType.F_MISSILE)) {
                    if (crit.isMissing()) {
                        cost = Double.parseDouble(client
                                .getserverConfigs("MissileCritReplaceCost"));
                    } else {
                        cost = Double.parseDouble(client
                                .getserverConfigs("MissileCritRepairCost"));
                    }
                } else // use the misc eq costs.
                if (crit.isMissing()) {
                    cost = Double.parseDouble(client
                            .getserverConfigs("EquipmentCritReplaceCost"));
                } else {
                    cost = Double.parseDouble(client
                            .getserverConfigs("EquipmentCritRepairCost"));
                }
            } else // use the misc eq costs.
            if (crit.isMissing()) {
                cost = Double.parseDouble(client
                        .getserverConfigs("EquipmentCritReplaceCost"));
            } else {
                cost = Double.parseDouble(client
                        .getserverConfigs("EquipmentCritRepairCost"));
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
