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
 * Client-side representation of a single BattleTech unit ('Mek, vehicle, aerospace fighter, infantry, etc.)
 * owned by a player within the campaign.
 * <p>
 * {@code CUnit} wraps a MegaMek {@link Entity} (the actual game-mechanics object used on the battlefield) with
 * the extra campaign bookkeeping MekWars needs around it: its pilot/crew ({@link Pilot}), current condition and
 * repair state (damage string, scrap value, repair cost, whether a pilot is mid-repair), quirks, ammo/machine-gun
 * loadout, targeting system, and Battle Value. It extends {@link Unit}, which holds the protocol-agnostic unit
 * data (ID, status, weight class, filename, etc.) shared with the server-side unit object.
 * <p>
 * Like {@link CPlayer}, a {@code CUnit} is largely a decode target for the wire protocol: the server sends
 * encoded unit data (as part of a player's full data dump, or a standalone hangar/army update), and
 * {@link #setData(String)} parses it with {@link TokenReader} and reconstructs the backing MegaMek
 * {@link Entity} via {@link #createEntity()}. Units are owned by a {@link CPlayer} (kept in that player's
 * Hangar) and may additionally be referenced from one or more {@link CArmy} formations.
 */
public class CUnit extends Unit {
    private static final MMLogger LOGGER = MMLogger.create(CUnit.class);

    // VARIABLES
    /**
     * The underlying MegaMek game entity (the actual 'Mek/vehicle/etc. game object with armor, weapons, crits,
     * etc.) backing this campaign unit. Built by {@link #createEntity()} from the unit's filename and pilot/crew
     * data; {@code null} until then.
     */
    protected Entity unitEntity;

    /** Current (possibly damaged/modified) Battle Value of the unit, as reported by the server. */
    private int BV;
    /** Currency value the unit would fetch if scrapped; 0 if not currently scrappable. */
    private int scrappableFor = 0;// value if scrapped
    /** Whether the unit's pilot is currently occupied undergoing personal repair/recovery rather than available. */
    private boolean pilotIsRepairing = false;
    /** Owning client session; used for server config lookups (pricing, house rules) needed by several methods. */
    private IClient client;
    /** Pre-rendered HTML fragment describing the unit's quirks, for display in tooltips/panels. */
    private String htmlQuirkList = " ";
    /** Raw "&"-delimited list of quirk names currently applied to the unit. */
    private String quirkList = " ";

    // CONSTRUCTORS
    /** Creates an uninitialized unit not yet bound to a client session; see {@link #init()}. */
    public CUnit() {
        init();
    }

    // PRIVATE METHODS
    /** Resets the entity reference, BV, status, and producer to their default "freshly created" values. */
    private void init() {
        unitEntity = null;
        BV = 0;
        setStatus(STATUS_OK);
        setProducer("unknown origin");
    }

    /**
     * Creates an uninitialized unit bound to the given client session, needed by most methods that consult
     * server configs (pricing, house rules, etc.).
     *
     * @param client owning client session
     */
    public CUnit(IClient client) {
        this.client = client;
        init();
    }

    /**
     * A method that returns the MU cost of a specified campaign unit.
     * <p>
     * Looks up the base currency ("Money Unit") price for the given weight class/type from server configs (Meks
     * are looked up by weight-class-only config keys; other types combine weight class and type in the key),
     * then adjusts it by the producing {@link House}'s per-type/weight price modifier. Never returns a negative
     * price.
     *
     * @param client      client session used to read server config price tables
     * @param weightClass unit weight class
     * @param type_id     unit type (see {@link Unit} type constants)
     * @param producer    the {@link House} that manufactures/sells the unit, whose price modifier is applied
     * @return int - # of MU it takes to buy a unit of the given weight class
     */
    public static int getPriceForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;

        String classType = String.format("%s%sPrice", Unit.getWeightClassDesc(weightClass), Unit.getTypeClassDesc(type_id));

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(String.format("%sPrice", Unit.getWeightClassDesc(weightClass))),
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
     * <p>
     * Mirrors {@link #getPriceForUnit(IClient, int, int, House)}'s config lookup pattern but for Influence Point
     * cost instead of currency, adjusted by the producing house's Influence price modifier. Never returns a
     * negative cost.
     *
     * @param client      client session used to read server config Influence tables
     * @param weightClass unit weight class
     * @param type_id     unit type (see {@link Unit} type constants)
     * @param producer    the {@link House} whose Influence price modifier is applied
     * @return int - # if IP it takes to buy a mech of the given units weight class
     */
    public static int getInfluenceForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;
        String classType = String.format("%s%sInf", Unit.getWeightClassDesc(weightClass), Unit.getTypeClassDesc(type_id));

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(String.format("%sInf", Unit.getWeightClassDesc(weightClass))), 0);
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
     * <p>
     * (PP = "Purchase Points"/component points, a third campaign currency distinct from Money and Influence,
     * typically used to gate access to rarer equipment.) Same config-lookup-then-house-modifier pattern as the
     * Money/Influence variants above; never returns a negative cost.
     *
     * @param client      client session used to read server config PP tables
     * @param weightClass unit weight class
     * @param type_id     unit type (see {@link Unit} type constants)
     * @param producer    the {@link House} whose component/PP price modifier is applied
     */
    public static int getPPForUnit(IClient client, int weightClass, int type_id, House producer) {
        int result;
        String classType = String.format("%s%sPP", Unit.getWeightClassDesc(weightClass), Unit.getTypeClassDesc(type_id));

        if (type_id == Unit.MEK) {
            result = MathUtility.parseInt(client.getServerConfigs(String.format("%sPP", Unit.getWeightClassDesc(weightClass))), 0);
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

    /**
     * Per-point currency cost to repair one point of armor at the given location, under the "point repair"
     * economy. Returns 0 outright if the "UsePartsRepair" house rule is active (that economy prices repairs by
     * physical spare parts instead of a flat per-point rate).
     *
     * @param unit     entity whose armor type at {@code location} determines the cost lookup key
     * @param client   client session used to read the server config cost table
     * @param location armor location index to price
     * @return currency cost per point of armor, or 0 under parts-based repair
     */
    public static double getArmorCost(Entity unit, IClient client, int location) {
        double cost;

        if (MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false)) {
            return 0;
        }

        String armorCost = String.format("CostPoint%s", UnitUtils.getArmorShortName(unit, location));
        cost = MathUtility.parseDouble(client.getServerConfigs(armorCost), 0.0);

        return cost;
    }

    /**
     * Per-point currency cost to repair one point of internal structure, under the "point repair" economy.
     * Returns 0 outright if the "UsePartsRepair" house rule is active.
     *
     * @param unit   entity whose internal structure type determines the cost lookup key
     * @param client client session used to read the server config cost table
     * @return currency cost per point of internal structure, or 0 under parts-based repair
     */
    public static double getStructureCost(Entity unit, IClient client) {
        double cost;

        if (MathUtility.parseBoolean(client.getServerConfigs("UsePartsRepair"), false)) {
            return 0;
        }

        String armorCost = String.format("CostPoint%sIS", UnitUtils.getInternalShortName(unit));
        cost = MathUtility.parseDouble(client.getServerConfigs(armorCost), 0.0);

        return cost;
    }

    /**
     * Currency cost to repair or replace a single critical slot, under the "point repair" economy. Returns 0
     * under the "UsePartsRepair" house rule, for a {@code null} critical, or for a breached-but-not-damaged
     * critical (breach alone doesn't require paid repair). Otherwise the cost depends on what's in the slot:
     * engine crits, system crits, and equipment crits (further split into energy/ballistic/missile weapons vs.
     * generic equipment) each have distinct "repair" vs. "replace" (when the item is fully missing/destroyed)
     * server config costs. The final result is floored at 1 (repairs are never free once this method decides a
     * cost applies).
     *
     * @param unit   entity the critical slot belongs to (currently unused beyond being part of the call site context)
     * @param client client session used to read the server config cost tables
     * @param crit   the critical slot to price; may be {@code null}
     * @return currency cost to repair/replace the slot, at least 1, or 0 if no repair is chargeable
     */
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

    /** @return pre-rendered HTML fragment describing this unit's quirks */
    public String getHtmlQuirkList() {
        return htmlQuirkList;
    }

    /** @param htmlQuirkList new pre-rendered HTML quirks fragment */
    public void setHtmlQuirkList(String htmlQuirkList) {
        this.htmlQuirkList = htmlQuirkList;
    }

    // PUBLIC METHODS
    /**
     * Decodes a full "CM$..." unit-data payload and rebuilds this unit's entire state from it: filename, ID,
     * status, producer, pilot (name, experience, gunnery/piloting, skills, kills/hits), MegaMek pilot options,
     * type, BV, weight class, and then the backing MegaMek {@link Entity} itself (via {@link #createEntity()}),
     * followed by auto-eject setting, ammo loadout, rapid-fire machine gun settings, targeting system, support-
     * unit flag, scrap value, applied battle damage, pilot-repairing flag, repair costs, "Christmas unit" flag,
     * and (if quirks are enabled server-side) quirks.
     * <p>
     * If the decoded filename resolves to an "Error"/"OMG" placeholder model (i.e. the client couldn't find the
     * actual unit file), the method short-circuits after setting a minimal crew and returns {@code true} without
     * attempting to parse ammo/quirks/etc., since that data would be meaningless for a placeholder entity.
     *
     * @param data dollar-sign-delimited payload beginning with the "CM" tag
     * @return {@code true} if the payload was recognized and applied (including the placeholder-entity short-circuit
     *         case); {@code false} if the tag didn't match or the entity failed to load
     */
    public boolean setData(String data) {

        StringTokenizer stringTokenizer;
        String element;
        String unitDamage;
        LOGGER.info(String.format("PDATA: %s", data));

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

        // decode the pilot/crew sub-record: name, exp, gunnery/piloting skill, then a variable-length skill list
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

            // certain named skills carry extra encoded data beyond the generic PilotSkill fields
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
        // (placeholder entity: the client couldn't resolve the real unit file, so there's no real
        // ammo/crit/quirk data to parse - just stub in a minimal crew and bail out early, returning success.)
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

        // then set up ammo loadout: for each ammo-carrying crit slot, restore its chosen ammo type
        // (players can swap ammo types), shots remaining, and hot-load flag (for LRMs/SRMs that support it)
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

        // set up rapid fire Machine guns, if any (rapid fire mode triples MG shots/damage at the cost of ammo)
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

        TokenReader.readString(stringTokenizer);// unused (token reserved/legacy - value is discarded)

        // targeting system (e.g. for artillery/indirect-fire targeting) tied to this unit's entity
        targetSystem.setEntity(unitEntity);
        try {
            targetSystem.setTargetSystem(TokenReader.readInt(stringTokenizer));
        } catch (TargetTypeOutOfBoundsException e) {
            LOGGER.error("Error setting target system within setData");
        }

        int suppUnit = TokenReader.readInt(stringTokenizer);
        setSupportUnit(suppUnit == 1);// support units (e.g. non-combat vehicles) are flagged distinctly

        scrappableFor = TokenReader.readInt(stringTokenizer);// currency value if the unit is scrapped, 0 if not scrappable

        unitDamage = TokenReader.readString(stringTokenizer);// encoded battle-damage string, applied to the entity below

        pilotIsRepairing = TokenReader.readBoolean(stringTokenizer);

        setRepairCosts(TokenReader.readInt(stringTokenizer), TokenReader.readInt(stringTokenizer));

        setChristmasUnit(TokenReader.readBoolean(stringTokenizer));// cosmetic/seasonal-event flag

        //@salient Quirks - set unit quirks, or drop data if quirks have been turned off
        if (stringTokenizer.hasMoreTokens() && MathUtility.parseBoolean(client.getServerConfigs("EnableQuirks"),
              false)) {
            setUnitQuirks(TokenReader.readString(stringTokenizer));
        } else if (stringTokenizer.hasMoreTokens()) {
            TokenReader.readString(stringTokenizer);
        }

        // link the MegaMek entity's external ID back to this campaign unit's ID so game-side lookups can match them
        unitEntity.setExternalId(getId());

        // apply the encoded damage string, reconstructing the entity's current (damaged) condition
        UnitUtils.applyBattleDamage(unitEntity, unitDamage, true);

        getC3Type(unitEntity);// resolve/cache this unit's C3 network type from its equipment

        return true;
    }

    /**
     * Tries to set UnitEntity from the global MekFileName
     * <p>
     * (Re)builds {@link #unitEntity} from this unit's stored filename via {@link UnitUtils#createEntity(String)}
     * and attaches a freshly-built crew derived from this unit's {@link Pilot} data. If the filename can't be
     * resolved to a real unit file, MegaMek returns a placeholder entity with chassis "Error"; in that case the
     * unit's producer text is overwritten with a diagnostic message instead of throwing.
     */
    public void createEntity() {
        unitEntity = UnitUtils.createEntity(getUnitFilename());
        unitEntity.setCrew(UnitUtils.createEntityPilot(this));

        if (unitEntity.getChassis().equals("Error")) {
            setProducer(String.format("Unable to find %s on clients system!", getUnitFilename()));
        }

        getC3Type(unitEntity);
    }

    /**
     * Builds a short, human-readable model designation for this unit's underlying entity, with special handling
     * for OmniMeks (always show chassis + model) and blank-model units (fall back to chassis alone).
     *
     * @return chassis and/or model string suitable for display, trimmed of extra whitespace
     */
    public String getModelName() {

        if (getType() != MEK) {
            return (String.format("%s %s", getEntity().getChassis(), getEntity().getModel())).trim();
        }

        if (getEntity().isOmni()) {
            return (String.format("%s %s", getEntity().getChassis(), getEntity().getModel())).trim();
        }

        if (!getEntity().getModel().trim().isEmpty()) {
            return getEntity().getModel().trim();
        }

        // else
        return getEntity().getChassis().trim();

    }

    //@salient this method is only accessible when quirks are enabled.
    /**
     * Decodes the unit's quirk payload ("!"-delimited: HTML display fragment, then "&"-delimited quirk name
     * list) and applies each named quirk (other than the "none" sentinel) as a {@code true}-valued option on the
     * underlying entity's {@link Quirks}. Only invoked from {@link #setData(String)} when the "EnableQuirks"
     * server config is on.
     *
     * @param data "!"-delimited payload: HTML quirks fragment, then "&"-delimited quirk name list
     */
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

    /** @return the backing MegaMek {@link Entity} for this unit, or {@code null} if {@link #createEntity()} hasn't run yet */
    public Entity getEntity() {
        return unitEntity;
    }

    /** @return pre-rendered HTML fragment describing this unit's quirks (duplicate accessor of {@link #getHtmlQuirkList()}) */
    public String getHtmlQuirksList() {
        return htmlQuirkList;
    }

    /** @return the raw "&"-delimited list of quirk names currently applied to this unit */
    public String getQuirksList() {
        return quirkList;
    }

    //@salient debug method, I really just used this once to make sure the quirks were being set,
    //but I'll leave it in case one day someone needs it.
    /**
     * Debug/verification helper: walks the underlying entity's {@link Quirks} groups and rebuilds an
     * "&"-joined list of every quirk currently set to {@code true}. Not used by production code paths; kept
     * around for manually sanity-checking that {@link #setUnitQuirks(String)} applied quirks correctly.
     *
     * @return "&"-joined list of active quirk names
     */
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

    /**
     * @return {@code true} if the underlying entity currently has at least one quirk option set to {@code true}
     */
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
     * <p>
     * Gives the unit a generic "Autopilot" crew (gunnery 4 / piloting 5) and builds its entity locally, then
     * optionally moves it off-board (e.g. for auto-deployed artillery) by the given hex distance and edge.
     *
     * @param filename unit template filename to load
     * @param distance off-board distance in hexes; 0 (or less) leaves the unit on-board
     * @param edge     compass edge to place the unit off-board from, when {@code distance > 0}
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
     * <p>
     * Builds a compact "Model [Gunnery/Piloting]" label for 'Meks/vehicles/aero, or infantry/battle armor
     * capable of anti-Mek attacks (which also use a piloting skill); other infantry/battle armor units only
     * have a gunnery skill, so they get "Model [Gunnery]" instead.
     */
    public String getSmallDescription() {
        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            return String.format("%s [%s/%s]", getModelName(), getPilot().getGunnery(), getPilot().getPiloting());
        }

        if ((getType() == Unit.INFANTRY) || (getType() == Unit.BATTLEARMOR)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                return String.format("%s [%s/%s]", getModelName(), getPilot().getGunnery(), getPilot().getPiloting());
            }
            return String.format("%s [%s]", getModelName(), getPilot().getGunnery());
        }
        return String.format("%s [%s]", getModelName(), getPilot().getGunnery());
    }

    /**
     * Builds a full HTML tooltip/description block for this unit: chassis/model, pilot name and skills, BV
     * (base or current depending on the "UseBaseBVForMatching" house rule, optionally showing both), experience
     * and kill count, pilot skill list, hit count, an army-context caption supplied by the caller, cargo/capacity
     * info, lifetime repair cost, producer/origin text, and (if applicable) scrap value. Used to populate the
     * hangar/army unit tooltips shown to the player.
     *
     * @param armyText extra caption text describing the unit's army context, appended if non-empty
     * @return an HTML-formatted description string wrapped in {@code <html><body>...</body></html>}
     */
    public String getDisplayInfo(String armyText) {
        String targetInfo;

        if ((getType() == Unit.MEK) && !unitEntity.isOmni()) {
            targetInfo = String.format("<html><body>#%s %s, %s", getId(), unitEntity.getChassis(), getModelName());
        } else {
            targetInfo = String.format("<html><body>#%s %s", getId(), getModelName());
        }

        if ((getType() == Unit.MEK) || (getType() == Unit.VEHICLE) || (getType() == Unit.AERO)) {
            targetInfo += String.format(" (%s, %s/%s) <br>", getPilot().getName(), getPilot().getGunnery(), getPilot().getPiloting());
        } else if ((getType() == Unit.BATTLEARMOR) || (getType() == Unit.INFANTRY)) {
            if (((Infantry) unitEntity).canMakeAntiMekAttacks()) {
                targetInfo += String.format(" (%s, %s/%s) <br>", getPilot().getName(), getPilot().getGunnery(), getPilot().getPiloting());
            } else {
                targetInfo += String.format(" (%s, %s) <br>", getPilot().getName(), getPilot().getGunnery());
            }
        } else {
            targetInfo += String.format(" (%s, %s) <br>", getPilot().getName(), getPilot().getGunnery());
        }

        if (getType() == Unit.VEHICLE) {
            targetInfo += String.format(" Movement: %s<br>", getEntity().getMovementModeAsString());
        }

        targetInfo += "BV: ";

        if (MathUtility.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"), false)) {
            targetInfo += getBaseBV();
        } else {
            targetInfo += BV;
        }

        if (MathUtility.parseBoolean(client.getConfigParam("ShowUnitBaseBV"), false)) {
            if (getBV() != getBaseBV()) {
                targetInfo += String.format(" (%s)", getBaseBV());
            }
        }
        targetInfo += String.format(" // Exp: %s // Kills: %s<br> ", getPilot().getExperience(), getPilot().getKills());

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
            targetInfo += String.format("Hits: %s<br>", getPilot().getHits());
        }

        if (!armyText.isEmpty()) {
            targetInfo += String.format("%s<br>", armyText);
        }

        String capacity = getEntity().getUnusedString();

        if ((capacity != null) && (!capacity.trim().isEmpty())) {
            if (MathUtility.parseBoolean(client.getServerConfigs("UseFullCapacityDescription"), false)) {
                if (capacity.endsWith("<br>")) {
                    capacity = capacity.substring(0, capacity.length() - 4);
                }

                if (capacity.contains("<br>")) {
                    targetInfo += String.format("Cargo:<br>%s<br>", capacity);
                } else {
                    targetInfo += String.format("Cargo: %s<br>", capacity);
                }
            } else if (capacity.startsWith("Troops")) {
                capacity = capacity.substring(9);// strip "Troops -" from
                // string
                targetInfo += String.format("Cargo: %s<br>", capacity);
            }
        }

        if (getLifeTimeRepairCost() > 0) {
            targetInfo += String.format("Repair Costs: %s/%s<br>", getCurrentRepairCost(), getLifeTimeRepairCost());
        }
        targetInfo += getProducer();

        if ((scrappableFor > 0)
                  && !MathUtility.parseBoolean(client.getServerConfigs("UseAdvanceRepair"), false)
                  && !MathUtility.parseBoolean(client.getServerConfigs("UseSimpleRepair"), false)) {
            targetInfo += String.format("<br><br><b>Scrap Value: %s</b>", client.moneyOrFluMessage(true, false, scrappableFor));
        }

        targetInfo += "</body></html>";
        return (targetInfo);
    }

    /**
     * @return the unit's Battle Value recalculated fresh from its current entity state, ignoring C3 network
     *         bonuses/penalties (skip C3 = true) but including pilot skill (skip pilot = false)
     */
    public int getBaseBV() {
        return getEntity().calculateBattleValue(false, true);
    }

    // STATIC METHODS

    /** @return the unit's server-reported Battle Value, never negative */
    public int getBV() {
        return Math.max(BV, 0);
    }

    /**
     * @return the BV value to use when matching this unit against an opposing force: the freshly recalculated
     *         {@link #getBaseBV()} if the "UseBaseBVForMatching" house rule is on, otherwise the cached {@link #getBV()}
     */
    public int getBVForMatch() {
        if (MathUtility.parseBoolean(client.getServerConfigs("UseBaseBVForMatching"), false)) {
            return getBaseBV();
        }
        return getBV();
    }

    /**
     * @return whether this unit counts as an OmniMek/OmniVehicle. Delegates to the entity's own {@code isOmni()}
     *         for most types, but for non-omni-flagged vehicles additionally checks the vehicle's chassis name
     *         against a local {@code ./data/mechfiles/omnivehiclelist.txt} file - a workaround for vehicles whose
     *         omni status isn't otherwise encoded on the entity. Any I/O error reading that file is logged and
     *         treated as "not omni" for that check.
     */
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

    /**
     * @return Battle Value calculated with neither C3 bonuses nor pilot skill applied (skip C3 = false, skip
     *         pilot = false) - i.e. the unit's "stock"/unmodified BV, useful for comparisons independent of the
     *         current pilot or network.
     */
    public int getOriginalBV() {
        return unitEntity.calculateBattleValue(false, false);
    }

    /**
     * Rebuilds this unit's entity from scratch and re-applies a fresh battle-damage encoding to it. Used when
     * the server sends updated repair results for a unit already in the hangar.
     *
     * @param data encoded battle-damage string to apply after rebuilding the entity
     */
    public void applyRepairs(String data) {
        createEntity();
        UnitUtils.applyBattleDamage(unitEntity, data, true);
    }

    /** @return whether this unit's pilot is currently occupied undergoing personal repair/recovery */
    public boolean getPilotIsRepairing() {
        return pilotIsRepairing;
    }

    /**
     * Toggles the "anti_air" quirk on the underlying entity, used to mark units specially equipped/rated for
     * anti-aircraft fire.
     *
     * @param aa new anti-air quirk state
     */
    public void setAntiAir(boolean aa) {
        Quirks quirks = unitEntity.getQuirks();
        quirks.getOption("anti_air").setValue(aa);
    }

    /** @return human-readable name of the unit's currently selected targeting system type */
    public String getTargetSystemTypeDesc() {
        // TODO Auto-generated method stub
        return targetSystem.getCurrentTypeName();
    }

    /** @return this unit's {@link TargetSystem}, used for artillery/indirect-fire targeting */
    public TargetSystem getTargetSystem() {
        return targetSystem;
    }

    /**
     * Changes this unit's targeting system type. Any out-of-range type value is caught and logged rather than
     * propagated, leaving the previous targeting system type in place.
     *
     * @param type new targeting system type identifier
     */
    public void setTargetSystem(int type) {
        try {
            targetSystem.setTargetSystem(type);
        } catch (TargetTypeOutOfBoundsException e) {
            LOGGER.error("Error setting target system");
        }
    }
}// end CUnit.java
