/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004-2006 Helge Richter (McWizard)
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import jakarta.annotation.Nonnull;
import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.OffBoardDirection;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Player;
import mekwars.common.SubFaction;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.CArmyComparator;
import mekwars.common.util.CUnitComparator;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitComponents;
import mekwars.common.util.UnitUtils;

/**
 * Client-side representation of a single player's campaign state.
 * <p>
 * A {@code CPlayer} is the in-memory mirror, on the client, of everything the server tracks about one
 * participant's campaign: currency (Money/C-Bills), Experience, Influence points, technician pool and its
 * upkeep cost, faction ({@link House}/{@link SubFaction}) membership, owned units ({@link CUnit}, held in the
 * {@code Hangar}), and how those units are organized into deployable formations ({@link CArmy}, held in
 * {@code Armies}). It also tracks personal pilots ({@link CPersonalPilotQueues}) and a handful of transient
 * repair/mine-quota fields used while the player is working through server-driven workflows.
 * <p>
 * Unlike a typical POJO, this class is not just constructed once and read - it is a decode target for the
 * MekWars client/server wire protocol. The server periodically streams pipe/tilde/hash-delimited command
 * strings (e.g. {@code PL|DA|...}, {@code PL|SAD|...}, {@code PL|HD|...}) which this class parses with
 * {@link TokenReader} and uses to mutate its own fields in place - see {@link #decodeCommand(String)} and
 * {@link #setData(String)} for the entry points. It extends {@link Player}, which holds protocol-agnostic
 * player data shared with the server-side player object.
 */
public class CPlayer extends Player {
    /** Protocol tag identifying a full player-data ("PL|DA|...") message in the client/server wire format. */
    public static final String PREFIX = "PL"; // prefix for player strings
    /** Field delimiter ("#") used when splitting/joining several player-related protocol strings. */
    public static final String DELIMITER = "#"; // delimiter for player strings
    private static final MMLogger LOGGER = MMLogger.create(CPlayer.class);
    /** Back-reference to the owning client session; used to reach server configs, GUI panels, and outbound chat/network calls. */
    private final IClient client;
    /**
     * Per-unit-type, per-weight-class purchase price penalty, indexed {@code [type][weightClass]}. Populated by
     * {@link #parseHangarPenaltyString(String)} under the sliding hangar-cost house rule, where buying more units
     * of a given type/weight makes the next one of that kind more expensive.
     */
    private final int[][] hangarPurchasePenalties = new int[6][4];
    /** Names of admin-level chat/notification exclusions configured by this player (see {@link #setAdminExcludes(String, String)}). */
    private final ArrayList<String> adminExcludes;
    /** Names of player-level chat/notification exclusions configured by this player (see {@link #setPlayerExcludes(String, String)}). */
    private final ArrayList<String> playerExcludes;
    /** Total technician headcount per tech skill tier (indices correspond to the tech tiers used by {@link UnitUtils}). */
    private final ArrayList<Integer> totalTechs = new ArrayList<>(4);
    /** Currently available (not otherwise occupied) technician headcount per tech skill tier. */
    private final ArrayList<Integer> availableTechs = new ArrayList<>(4);
    /** Cached inventory of spare parts owned by the player, used only when the "UsePartsRepair" house rule is enabled. */
    private final UnitComponents partsCache = new UnitComponents();
    /** The player's display/account name. */
    private String Name;
    /** Name of the {@link House} (faction) this player belongs to. */
    private String House;
    /** Path/URL to the player's chosen logo image, wrapped into an {@code <img>} tag by {@link #getLogo()}. */
    private String myLogo = "";
    /** Player's total Experience points. */
    private int Exp;
    /** Player's currency balance (C-Bills / Money Units). */
    private int Money;
    /** Total hangar bay capacity owned by the player. */
    private int Bays;
    /** Hangar bays not currently occupied by a unit. */
    private int FreeBays;
    /** Player's Influence Point balance, spent to acquire units under the influence-cost economy. */
    private int Influence;
    /** Number of technicians currently employed by the player. */
    private int Techs;
    /** Computed per-cycle pay cost for the current technician headcount; see {@link #doPayTechniciansMath()}. */
    private int TechCost;
    /** Player's Reward Point balance, a secondary currency earned from special events/achievements. */
    private int RewardPoints;
    /** Player's numeric skill/battle rating. */
    private double Rating;
    /** Extra technician-pay surcharge applied once hangar occupancy passes a sliding-cost threshold; added on top of {@link #TechCost}. */
    private int hangarPenalty;
    /** All units ({@link CUnit}) owned by the player, whether assigned to an army or not. Kept sorted by {@link #sortHangar()}. */
    private Vector<CUnit> Hangar;
    /** All formations ({@link CArmy}) the player has organized their units into. Kept sorted by {@link #sortArmies()}. */
    private Vector<CArmy> Armies;
    /** Client-assembled, non-persistent units (e.g. auto turrets/artillery) added to a scenario alongside the player's real armies. */
    private ArrayList<CUnit> AutoArmy;
    /** Queue of personal (named) pilots available to be assigned to this player's units. */
    private CPersonalPilotQueues personalPilotQueue;
    /** Resolved {@link House} object for the faction named by {@link #House}. */
    private House myHouse;
    /** Resolved {@link House} object for the faction the player is currently fighting for (may differ from {@link #myHouse}, e.g. mercenary contracts). */
    private House houseFightingFor;
    /** Transient: location index selected for an in-progress repair operation. */
    private int repairLocation = 0;
    /** Transient: technician type/skill selected for an in-progress repair operation. */
    private int repairTechType = 0;
    /** Transient: number of retries used for the current repair attempt. */
    private int repairRetries = 0;
    /** Quota of conventional minefields the player is currently allowed to deploy. */
    private int conventionalMinesAllowed = 0;
    /** Quota of vibrabombs the player is currently allowed to deploy. */
    private int vibraMinesAllowed = 0;
    /** Name of the {@link SubFaction} (within {@link #myHouse}) the player belongs to, if any. */
    private String subFactionName = "";

    /**
     * Creates a blank player bound to the given client session, with all currency/points zeroed and empty
     * hangar/army/exclusion collections. Real data is populated afterward by {@link #setData(String)} once the
     * server sends it.
     *
     * @param client owning client session, used for server config lookups and GUI callbacks
     */
    public CPlayer(IClient client) {
        this.client = client;
        Name = "";
        Exp = 0;
        Money = 0;
        Bays = 0;
        FreeBays = 0;
        Influence = 0;
        Rating = 0;
        House = "";
        Hangar = new Vector<>(1, 1);
        Armies = new Vector<>(1, 1);
        AutoArmy = new ArrayList<>();
        personalPilotQueue = new CPersonalPilotQueues();
        adminExcludes = new ArrayList<>();
        playerExcludes = new ArrayList<>();
        myHouse = new House();
        houseFightingFor = new House();
        for (int x = 0; x < 4; x++) {
            availableTechs.add(0);
            totalTechs.add(0);
        }
    }

    /**
     * Entry point for dispatching a raw "PL|..." protocol command to this player object. Currently only
     * recognizes the "DA" (full data dump) sub-command and delegates it to {@link #setData(String)}; any other
     * prefix or sub-command is rejected.
     *
     * @param command the raw command string, expected to start with "PL|"
     * @return {@code true} if the command was recognized and successfully applied; {@code false} otherwise
     */
    public boolean decodeCommand(String command) {
        StringTokenizer stringTokenizer;
        String element;

        stringTokenizer = new StringTokenizer(command, "|");
        element = TokenReader.readString(stringTokenizer);

        if (!element.equals("PL")) {
            return (false);
        }

        element = TokenReader.readString(stringTokenizer);
        command = command.substring(3);

        if (element.equals("DA")) {// is a PI|DA
            return setData(command);
        }

        return (false);
    }

    /**
     * Decodes a full "CP~..." player-data payload (the body of a PL|DA command) and overwrites this player's
     * entire state from it: name, currency/points, the complete hangar ({@link CUnit} list), the complete set of
     * armies ({@link CArmy} list), technician counts, faction, logo, parts cache, etc. Called in response to a
     * PS| (or PL|DA) message sent by the server, typically at login or after a full resync. Existing hangar and
     * army contents are cleared and rebuilt from the tokens before returning.
     *
     * @param data tilde-delimited payload beginning with the "CP" tag
     * @return {@code true} if the payload started with the expected "CP" tag and was applied; {@code false} otherwise
     */
    public boolean setData(String data) {
        StringTokenizer stringTokenizer;
        String element;
        CUnit targetMek;
        int i, armiesCount, hangerCount;

        stringTokenizer = new StringTokenizer(data, "~");
        element = TokenReader.readString(stringTokenizer);

        if (!element.equals("CP")) {
            return false;
        }

        for (int x = 0; x < UnitUtils.TECH_ELITE; x++) {
            setTotalTechs(x, 0);
            setAvailableTechs(x, 0);
        }

        Armies.clear();
        Hangar.clear();

        Name = TokenReader.readString(stringTokenizer);

        Money = TokenReader.readInt(stringTokenizer);
        Exp = TokenReader.readInt(stringTokenizer);

        hangerCount = TokenReader.readInt(stringTokenizer);

        for (i = 0; i < hangerCount; i++) {
            targetMek = new CUnit(client);
            if (targetMek.setData(TokenReader.readString(stringTokenizer))) {
                Hangar.add(targetMek);
            }
        }

        armiesCount = (TokenReader.readInt(stringTokenizer));

        for (i = 0; i < armiesCount; i++) {
            CArmy army = new CArmy();
            army.fromString(TokenReader.readString(stringTokenizer), this, "%", client);
            Armies.add(army);
        }

        Bays = TokenReader.readInt(stringTokenizer);
        FreeBays = TokenReader.readInt(stringTokenizer);
        Rating = Double.parseDouble(TokenReader.readString(stringTokenizer));
        Influence = TokenReader.readInt(stringTokenizer);
        setTechnicians(TokenReader.readInt(stringTokenizer));
        doPayTechniciansMath();
        RewardPoints = TokenReader.readInt(stringTokenizer);
        String string = TokenReader.readString(stringTokenizer);
        setMekToken(Integer.parseInt(string));
        House = TokenReader.readString(stringTokenizer);
        setHouseFightingFor(TokenReader.readString(stringTokenizer));
        setLogo(TokenReader.readString(stringTokenizer));
        setInvisible(TokenReader.readBoolean(stringTokenizer));

        if (Boolean.parseBoolean(client.getServerConfigs("UsePartsRepair"))) {
            partsCache.fromString(TokenReader.readString(stringTokenizer), "|");
        } else {
            TokenReader.readString(stringTokenizer);
        }

        setAutoReorder(TokenReader.readBoolean(stringTokenizer));

        LOGGER.info(String.format("My Player Flags: %s", flags.export()));

        // traps run. sort the HQ. this isn't duplicative, b/c
        // direct loads (PS instead of PL) don't trigger sorts.
        sortHangar();
        return true;
    }

    /**
     * Sets the player's technician headcount and immediately recomputes the resulting pay cost.
     *
     * @param tech new number of technicians employed
     */
    @Override
    public void setTechnicians(int tech) {
        Techs = tech;
        doPayTechniciansMath();
    }

    /** @return the player's display name */
    public String getName() {
        return Name;
    }

    /**
     * Recalculates {@link #TechCost}, the per-cycle amount the player must pay to keep their current technician
     * headcount. The cost model is a rising marginal rate: each additional technician (1st, 2nd, 3rd, ...) costs
     * more than the last, controlled by the "AdditivePerTech" server config, up to an "AdditiveCostCeiling" flat
     * rate at which point all further technicians are charged that same flat (ceiling) amount instead of an ever
     * -increasing marginal rate. Any active {@link #hangarPenalty} (sliding hangar-cost surcharge) is then added
     * on top. Called whenever the technician count changes so {@link #getTechCost()} stays current without
     * recomputing on every read.
     */
    public void doPayTechniciansMath() {

        // don't even waste time on 0 cases. Just return.
        if (Techs <= 0) {
            TechCost = 0;
            return;
        }

        // starts as a double, gets cast back to an int for return.
        float amountToPay = 0;

        // load config variables needed to do the math ...
        float additive = MathUtility.parseFloat(client.getServerConfigs("AdditivePerTech"), 0.0f);
        float ceiling = MathUtility.parseFloat(client.getServerConfigs("AdditiveCostCeiling"), 0.0f);

        /*
         * divide the ceiling by the additive. techs past this number are all charged at the ceiling rate. Example:
         * (With 1.20 and .04, the result is 30). Every additional tech (31, 32, etc.) is paid at the ceiling
         * wage.
         */
        int techCeiling = (int) (ceiling / additive);
        if (Techs > techCeiling) {
            int techsPastCeiling = Techs - techCeiling;
            amountToPay += ceiling * techsPastCeiling;
        }// end if (some techs are paid @ ceiling price)

        /*
         * Add up the number of times the non-ceiling techs were increased,
         * then figure out their total cost. In cases where the ceiling is
         * passed, the flat fee techs are handled above, so only techs up to
         * that ceiling need to have the additive math done. If the ceiling isn't
         * reached, just use the number of techToPay from the param.
         */

        int totalAdditions = getTotalAdditions(techCeiling);

        // now figure out the final amount to pay ...
        amountToPay += totalAdditions * additive;

        // Add penalty if the player is over a sliding limit

        amountToPay += hangarPenalty;

        // now return the amount in INT form since we don't support fractional MU costs. also, set the
        // currentTechPayment to avoid doing this math again if possible

        TechCost = Math.round(amountToPay);
    }

    /**
     * Sums 1 + 2 + ... + n for n = min(current technician count, techCeiling), i.e. the total number of times the
     * per-technician additive rate has been "stacked" for technicians priced below the ceiling. Used by
     * {@link #doPayTechniciansMath()} to compute the marginal-rate portion of technician pay without repeated
     * floating point multiplication.
     *
     * @param techCeiling number of technicians priced under the marginal/additive scheme before the flat ceiling rate applies
     * @return the triangular-number sum of technician slots priced under the additive scheme
     */
    private int getTotalAdditions(int techCeiling) {
        int techsUsingAdditive = Math.min(Techs, techCeiling);

        /*
         * Faster to just to a for loop to determine the number of times the
         * additive was made (1 + 2 + 3 + 4, and so on) with ints, and THEN
         * multiply by the double additive than do a lot of floating point math
         * by for-in through and multiplying by the additive each time.
         */
        int totalAdditions = 0;

        for (int i = 1; i <= techsUsingAdditive; i++) {
            totalAdditions += i;
        }// end for(all counted techs)

        return totalAdditions;
    }

    /**
     * Called from PL after a PL|SAD command is received. Decodes a single army's data and either adds it as a
     * new {@link CArmy} or replaces the existing army sharing its ID. The old army's legal-operations set (which
     * scenario types the army may be used in) is preserved and copied onto the replacement, since that data is
     * not part of the resent army payload. The old army entry is removed and the new one is inserted at its ID
     * index if possible, otherwise appended.
     */
    public void setArmyData(String data) {

        CArmy newArmy = new CArmy();
        newArmy.fromString(data, this, "%", client);

        // Save the old army's legal operations.
        CArmy oldArmy = getArmy(newArmy.getID());

        if (oldArmy != null) {
            newArmy.setLegalOperations(oldArmy.getLegalOperations());
        }

        // swap the armies
        removeArmy(newArmy.getID());

        if (Armies.size() < newArmy.getID()) {
            Armies.add(newArmy);
        } else {
            Armies.add(newArmy.getID(), newArmy);
        }
    }

    /**
     * Looks up one of this player's armies by its numeric ID.
     *
     * @param id army ID to find
     * @return the matching {@link CArmy}, or {@code null} if no army with that ID exists
     */
    public CArmy getArmy(int id) {
        for (CArmy currA : Armies) {
            if (currA.getID() == id) {
                return currA;
            }
        }
        return null;
    }

    /**
     * Remove an army from a player's set. This can be called directly from a PL|RA command, or indirectly by PL|SAD via
     * CPlayer.setArmyData(), which removes all old instances of an army before adding the new data. When a matching
     * army is removed, the client's attack menu is refreshed since the set of usable armies has changed.
     *
     * @param lanceID ID of the army (lance/formation) to remove
     */
    public void removeArmy(int lanceID) {
        for (Iterator<CArmy> i = Armies.iterator(); i.hasNext(); ) {
            if (i.next().getID() == lanceID) {
                i.remove();
                client.getMainFrame().updateAttackMenu();// removing an army
                return;
            }
        }
    }

    /**
     * Called by PL|HD - adds a single unit to the hangar. Unlike {@link #setData(String)} (a full resync), this
     * incrementally appends one newly-acquired {@link CUnit} and re-sorts the hangar. There is deliberately no
     * corresponding single-unit "addUnit" on the wire protocol beyond this hangar-data path; see the class
     * comments referenced below for why.
     */
    public void setHangarData(String data) {
        try {
            CUnit unit = new CUnit(client);
            if (unit.setData(data)) {
                Hangar.add(unit);
                sortHangar();// sort it!
            }
        } catch (Exception e) {
            LOGGER.error(e, "Unable to set hangar data");
        }
    }

    /**
     * Method that resorts every unit. Inefficient, but we hate clients. Because we're evil. So there.
     * <p>
     * Reads the player's configured primary/secondary/tertiary hangar sort keys (e.g. Name, Battle Value,
     * Weight) and applies them to a copy of the {@code Hangar} vector via {@link CUnitComparator}, from least to
     * most significant so that the primary key wins ties broken by the secondary, and so on. The result then
     * replaces {@code Hangar} outright. Client- and server-side hangars are not required to agree on ordering,
     * since all transactions after the initial data feed operate unit-by-unit rather than depending on hangar
     * position.
     *
     * @author urgru 4.4.05
     */
    public void sortHangar() {

        // load configs
        String primeSortOrder = client.getConfigParam("PRIMARY_HQ_SORT_ORDER");
        String secondarySortOrder = client.getConfigParam("SECONDARY_HQ_SORT_ORDER");
        String tertiarySortOrder = client.getConfigParam("TERTIARY_HQ_SORT_ORDER");

        // Choices [note - this array must be duplicated in CHQPanel's
        // maybeShowPopup()]
        String[] choices = { "Name", "Battle Value", "Gunnery Skill", "ID Number", "MP (Jumping)", "MP (Walking)",
                             "Pilot Kills", "Unit Type", "Weight (Class)", "Weight (Tons)", "No Sort" };

        // determine which sort will dominate
        int primarySort = CUnitComparator.HQ_SORT_NONE;

        for (int i = 0; i < choices.length; i++) {
            if (primeSortOrder.equals(choices[i])) {
                primarySort = i;
            }
        }

        // determine secondary sort
        int secondarySort = CUnitComparator.HQ_SORT_NONE;

        for (int i = 0; i < choices.length; i++) {
            if (secondarySortOrder.equals(choices[i])) {
                secondarySort = i;
            }
        }

        // determine tertiary sort
        int tertiarySort = CUnitComparator.HQ_SORT_NONE;

        for (int i = 0; i < choices.length; i++) {
            if (tertiarySortOrder.equals(choices[i])) {
                tertiarySort = i;
            }
        }

        // we know this holds CUnits. Can safely cast.
        Vector<CUnit> unitsArray = new Vector<>(Hangar);

        // run third sort
        if ((tertiarySort != primarySort) &&
                  (tertiarySort != secondarySort) &&
                  (tertiarySort != CUnitComparator.HQ_SORT_NONE)) {
            unitsArray.sort(new CUnitComparator(tertiarySort));
        }

        // run the second sort
        if ((primarySort != secondarySort) && (secondarySort != CUnitComparator.HQ_SORT_NONE)) {
            unitsArray.sort(new CUnitComparator(secondarySort));
        }

        // now the primary sort
        if (primarySort != CUnitComparator.HQ_SORT_NONE) {
            unitsArray.sort(new CUnitComparator(primarySort));
        }

        // overwrite the hangar with a new arraylist constructed from the
        // unitsArray.
        Vector<CUnit> Hangar2 = new Vector<>(1, 1);
        Hangar2.addAll(unitsArray);

        // replace the hangar and flush the array
        Hangar = Hangar2;
    }

    /**
     * Called by PL|UU - updates a unit's data. Reads a unit ID and a replacement data blob from the tokenizer,
     * looks up the existing {@link CUnit} in the hangar, and overwrites its state via {@link CUnit#setData(String)}.
     * The hangar is re-sorted afterward since sort-relevant properties may have changed. Any failure (e.g. unit
     * not found, resulting in a NullPointerException) is caught and logged rather than propagated.
     *
     * @param stringTokenizer tokenizer positioned at the unit ID, followed by the unit's encoded data string
     */
    public void updateUnitData(StringTokenizer stringTokenizer) {
        try {
            CUnit currUnit = getUnit(TokenReader.readInt(stringTokenizer));
            currUnit.setData(TokenReader.readString(stringTokenizer));
            sortHangar();// properties have changes. sort. YARR!
        } catch (Exception e) {
            LOGGER.error(e, "Unable to update unit data");
        }
    }

    /**
     * Looks up a unit owned by this player by its numeric ID.
     *
     * @param unitID ID of the unit to find
     * @return the matching {@link CUnit} from the hangar, or {@code null} if not found
     */
    public CUnit getUnit(int unitID) {
        for (CUnit currU : Hangar) {
            if (currU.getId() == unitID) {
                return currU;
            }
        }

        return null;
    }

    /**
     * Toggles the rapid-fire setting of a specific machine gun mount on one of the player's units, identified by
     * unit ID, critical-slot location, and slot index, then re-sorts the hangar. Any failure is caught and
     * logged rather than propagated.
     *
     * @param stringTokenizer tokenizer positioned at: unit ID, location, slot index, new rapid-fire selection
     */
    public void updateUnitMachineGuns(StringTokenizer stringTokenizer) {
        try {
            CUnit currUnit = getUnit(TokenReader.readInt(stringTokenizer));
            int location = TokenReader.readInt(stringTokenizer);
            int slot = TokenReader.readInt(stringTokenizer);
            boolean selection = TokenReader.readBoolean(stringTokenizer);

            CriticalSlot crit = currUnit.getEntity().getCritical(location, slot);
            crit.getMount().setRapidFire(selection);

            sortHangar();// properties have changes. sort. YARR!
        } catch (Exception e) {
            LOGGER.error(e, "Unable to Update Unit Machine Guns");
        }
    }

    /**
     * Remove a unit from the player's hangar. Called from PL after receipt of a PL|RU|ID (RemoveUnit#ID) command.
     * <p>
     * Note that there is NOT an analogous addUnit() method. Single additions are sent to the clients using (obtusely
     * enough) the PL|HD (hangar data) command. See .setHangarData()'s comments, as well as those in SUnit.addUnit(),
     * for details/explanation.
     */
    public boolean removeUnit(int unitID) {
        for (java.util.Iterator<mekwars.common.campaign.CUnit> i = Hangar.iterator(); i.hasNext(); ) {
            if (i.next().getId() == unitID) {
                i.remove();
                return (true);
            }
        }

        return (false);
    }

    /** @return the name of the {@link House} (faction) this player belongs to */
    public String getHouse() {
        return House;
    }

    /**
     * Changes the player's faction. Resolves and caches the {@link House} object for the given faction name,
     * requests fresh faction-specific server configs (since prices, unit access, etc. are faction dependent),
     * and refreshes any open Black Market and HQ panels so their buy/sell buttons and layout reflect the new
     * faction's access rules.
     *
     * @param faction name of the new {@link House} to join
     */
    public void setHouse(String faction) {
        myHouse = client.getData().getHouseByName(faction);
        House = faction;

        /*
         * Get the faction configs before starting anything else. I could pause
         * the client and wait for the configs, but I'll let it go. --Torren
         */
        client.sendChat(String.format("%sc getfactionconfigs#0%s", IClient.CAMPAIGN_PREFIX, client.getServerConfigs("TIMESTAMP")));

        /*
         * Now that we have a house set, we can check for BM access properly. Do
         * the BM buy and sell button checks.
         */
        if (client.getMainFrame().getMainPanel().getBMPanel() != null) {
            client.getMainFrame().getMainPanel().getBMPanel().checkFactionAccess();
        }

        /*
         * Same thing for the HQ. We have a house, so we can rebuild the button
         * bar w/ or w/o a reset button, as appropriate.
         */
        if (client.getMainFrame().getMainPanel().getHQPanel() != null) {
            client.getMainFrame().getMainPanel().getHQPanel().reinitialize();
        }
    }

    /** @return the resolved {@link House} the player is currently fighting for (may differ from their home faction, e.g. under a mercenary contract) */
    public House getHouseFightingFor() {
        return houseFightingFor;
    }

    /**
     * Resolves and stores the faction the player is currently fighting for, by name.
     *
     * @param faction name of the {@link House} to resolve and record
     */
    public void setHouseFightingFor(String faction) {
        houseFightingFor = client.getData().getHouseByName(faction);
    }

    /** @return the player's logo pre-wrapped in an HTML {@code <img>} tag, ready for display in a Swing HTML component */
    public String getLogo() {
        return String.format("<img height='140' width='130' src ='%s'>", myLogo);
    }

    /**
     * @param logo path/URL of the image to use as this player's logo
     */
    public void setLogo(String logo) {
        myLogo = logo;
    }

    /** @return the raw logo path/URL, without the HTML wrapper produced by {@link #getLogo()} */
    public String getMyLogo() {
        return myLogo;
    }

    /** @return the player's total Experience points */
    public int getExp() {
        return Exp;
    }

    /** @param exp new Experience point total */
    public void setExp(int exp) {
        Exp = exp;
    }

    /** @return the player's numeric skill/battle rating */
    public double getRating() {
        return Rating;
    }

    /** @param rating new skill/battle rating */
    public void setRating(double rating) {
        Rating = rating;
    }

    /** @return the player's Reward Point balance */
    public int getRewardPoints() {
        return RewardPoints;
    }

    /** @param rewards new Reward Point balance */
    public void setRewardPoints(int rewards) {
        RewardPoints = rewards;
    }

    /** @return the player's currency (C-Bill/MU) balance */
    public int getMoney() {
        return Money;
    }

    /** @param money new currency balance */
    public void setMoney(int money) {
        Money = money;
    }

    /** @return total hangar bay capacity owned by the player */
    public int getBays() {
        return Bays;
    }

    /** @param bays new total hangar bay capacity */
    public void setBays(int bays) {
        Bays = bays;
    }

    /** @return number of hangar bays not currently occupied by a unit */
    public int getFreeBays() {
        return FreeBays;
    }

    /** @param freeBays new count of unoccupied hangar bays */
    public void setFreeBays(int freeBays) {
        FreeBays = freeBays;
    }

    /** @return the player's Influence Point balance */
    public int getInfluence() {
        return Influence;
    }

    /** @param influence new Influence Point balance */
    public void setInfluence(int influence) {
        Influence = influence;
    }

    /** @return number of technicians currently employed by the player */
    public int getTechs() {
        return Techs;
    }

    /**
     * @return the total amount the player must pay to keep their current technicians, i.e. the base cost from
     *         {@link #doPayTechniciansMath()} plus any active sliding hangar-cost surcharge ({@link #getHangarPenalty()}).
     *         Never negative - clamps to 0 if the base cost is somehow negative.
     */
    public int getTechCost() {
        if (TechCost < 0) {
            return 0;
        }
        // else
        // If not using sliding hangar costs, hangarPenalty will be 0, so will still return the same.
        return TechCost + getHangarPenalty();
    }

    /** @return the current sliding hangar-cost surcharge applied to technician pay */
    public int getHangarPenalty() {
        return hangarPenalty;
    }

    /** @param hangarPenalty new sliding hangar-cost surcharge */
    public void setHangarPenalty(int hangarPenalty) {
        this.hangarPenalty = hangarPenalty;
    }

    /** @return the live, mutable collection of every unit ({@link CUnit}) this player owns */
    public Vector<CUnit> getHangar() {
        return Hangar;
    }

    /**
     * Calculate the ID that would be assigned to a newly created army. This is used by the army builder to construct /c
     * exm# commands for an as-yet non-existent army.
     * <p>
     * Scans upward from 0 for the first ID not already in use by an existing army in {@link #Armies}.
     *
     * @return the lowest non-negative army ID currently unused by this player
     */
    public int getNextNewArmyID() {
        int newID = -1;
        int possibleNewID = 0;

        while (newID == -1) {
            for (int i = 0; i < Armies.size(); i++) {
                if ((Armies.get(i)).getID() == possibleNewID) {
                    newID = i;
                }
            }

            if (newID == -1) {
                newID = possibleNewID;
            } else {
                possibleNewID++;
                newID = -1;
            }
        }

        return newID;
    }

    /**
     * Method that creates an autonomy gun emplacement. Takes in a string with weight classes and uses server configs
     * (path, filenames) to construct units of those weights.
     * <p>
     * Units are added to servers when a player joins a game, the same as units from locked armies.
     * <p>
     * Note: gun emplacements are appended to the shared {@link #AutoArmy} list without clearing it first (unlike
     * {@link #setAutoArmy(StringTokenizer)}); callers rely on {@code setAutoArmy} having already reset the list,
     * or on an explicit "CLEAR" token to stop processing early.
     *
     * @param stringTokenizer tokenizer of unit filenames, or {@code null}/a lone "CLEAR" token to do nothing/stop
     */
    public void setAutoGunEmplacements(StringTokenizer stringTokenizer) {

        // if it's a null, this was just a clearing call.
        if (stringTokenizer == null) {
            return;
        }

        while (stringTokenizer.hasMoreTokens()) {
            String filename = TokenReader.readString(stringTokenizer);

            if (filename.equals("CLEAR")) {
                return;
            }

            CUnit currUnit = new CUnit(client);
            currUnit.setAutoUnitData(filename, 0, OffBoardDirection.NORTH);
            AutoArmy.add(currUnit);
        }// end while(tokens)
    }// end setAutoArmy()

    /**
     * Populates {@link #AutoArmy} with fully-specified units decoded from MegaMek Unit List (MUL) encoded data
     * strings, as opposed to the filename-only units built by {@link #setAutoArmy(StringTokenizer)}. Used when a
     * scenario's opposing/auxiliary force was authored externally (e.g. imported from a MUL file) rather than
     * assembled from simple weight-class templates. Stops as soon as a "CLEAR" token is seen. Note that, unlike
     * {@code setAutoArmy}, this method does not clear {@link #AutoArmy} before appending.
     *
     * @param stringTokenizer tokenizer of per-unit encoded data strings, terminated by "CLEAR"
     */
    public void setMULCreatedArmy(StringTokenizer stringTokenizer) {

        while (stringTokenizer.hasMoreElements()) {
            String data = TokenReader.readString(stringTokenizer);
            if (data.equalsIgnoreCase("CLEAR")) {
                return;
            }

            CUnit cUnit = new CUnit();
            cUnit.setData(data);
            AutoArmy.add(cUnit);
        }
    }

    /**
     * Method that returns the autoArmy arraylist.
     *
     * @return the client-assembled, non-persistent units (e.g. auto turrets/artillery or MUL-created forces) currently staged for a scenario
     */
    public ArrayList<CUnit> getAutoArmy() {
        return AutoArmy;
    }

    /**
     * Method that creates an autonomy. Takes in a string with weight classes and uses server configs (path, filenames)
     * to construct units of those weights.
     * <p>
     * Units are added to servers when a player joins a game, the same as units from locked armies.
     * <p>
     * Always clears {@link #AutoArmy} first (this is the method expected to run before
     * {@link #setAutoGunEmplacements(StringTokenizer)} or {@link #setMULCreatedArmy(StringTokenizer)} append to
     * it), then builds one auto-unit per filename token, offsetting each from the map by a distance derived from
     * the "DistanceFromMap" server config and the player's starting edge.
     *
     * @param stringTokenizer tokenizer of unit filenames, or {@code null}/a lone "CLEAR" token to just clear the auto army
     */
    public void setAutoArmy(StringTokenizer stringTokenizer) {

        /*
         * clear the previous auto army. Auto army is always called first, and is
         * cleared correctly even if only gun emplacements are sent.
         */
        AutoArmy = new ArrayList<>();

        // if it's a null, this was just a clearing call.
        if (stringTokenizer == null) {
            return;
        }

        while (stringTokenizer.hasMoreTokens()) {
            String filename = TokenReader.readString(stringTokenizer);
            if (filename.equals("CLEAR")) {
                return;
            }

            // get the distance
            int distInBoards = MathUtility.parseInt(client.getServerConfigs("DistanceFromMap"), 0);
            int distInHexes = distInBoards * 17;// 17 hexes per board.

            CUnit currUnit = getCUnit(filename, distInHexes);
            AutoArmy.add(currUnit);
        }// end while(tokens)
    }// end setAutoArmy()

    /**
     * Builds a single auto-unit from a weight-class template filename, placed off-board at the given distance in
     * the direction implied by the player's starting deployment edge (mapped from {@code getPlayerStartingEdge()}
     * to a compass {@link OffBoardDirection}; edges 0-3 are treated as "no offset direction override" and left
     * as NORTH). Used for artillery/auto-deployed units so their off-board edge matches the player's chosen side.
     *
     * @param filename    unit template filename to load
     * @param distInHexes distance off-board, in hexes, to place the unit
     * @return the newly constructed auto {@link CUnit}
     */
    private @Nonnull CUnit getCUnit(String filename, int distInHexes) {
        CUnit currUnit = new CUnit(client);

        /*
         * This is needed to set the edge for auto arty when auto edge is
         * set for players. Else, arty edge is set in MM when the players
         * click on the edge they want.
         */
        OffBoardDirection direction = OffBoardDirection.NORTH;
        switch (client.getPlayerStartingEdge()) {
            case 0, 1, 2, 3:
                break;
            case 4:
                direction = OffBoardDirection.EAST;
                break;
            case 5:
            case 6:
            case 7:
                direction = OffBoardDirection.SOUTH;
                break;
            case 8:
                direction = OffBoardDirection.WEST;
                break;
        }

        currUnit.setAutoUnitData(filename, distInHexes, direction);
        return currUnit;
    }

    /**
     * Counts how many of the player's armies currently include the given unit. Since a unit can in principle be
     * referenced by more than one {@link CArmy}, this is used to detect that kind of duplication/overlap.
     *
     * @param unitID ID of the unit to search for
     * @return number of armies containing that unit
     */
    public int getAmountOfTimesUnitExistsInArmies(int unitID) {
        int result = 0;
        for (CArmy currA : Armies) {
            if (currA.getUnit(unitID) != null) {
                result++;
            }
        }
        return result;
    }

    /**
     * @param unitID ID of the unit to search for
     * @return a space-separated string of army IDs that currently contain the given unit (empty string if none)
     */
    public String getArmiesUnitIsIn(int unitID) {
        StringBuilder result = new StringBuilder();
        for (CArmy currA : Armies) {
            if (currA.getUnit(unitID) != null) {
                result.append(currA.getID()).append(" ");
            }
        }
        return result.toString();
    }

    /**
     * Collects every unit belonging to a locked army. A locked army is one committed/staged for deployment (e.g.
     * into a scenario), so its units cannot be freely rearranged; this method is used to figure out which units
     * are currently unavailable for other armies. Synchronized since armies can be mutated concurrently by
     * incoming network updates.
     *
     * @return a fresh list of all units in any currently-locked army
     */
    public synchronized ArrayList<Unit> getLockedUnits() {

        ArrayList<Unit> result = new java.util.ArrayList<>();
        for (CArmy currA : Armies) {
            if (currA.isLocked()) {
                result.addAll(currA.getUnits());
            }
        }
        return result;
    }

    /**
     * @return the first army found in locked state, or {@code null} if none are locked. Note this assumes at
     *         most one army is locked at a time; if multiple were locked, only the first encountered would be returned.
     */
    public synchronized CArmy getLockedArmy() {

        for (CArmy currA : Armies) {
            if (currA.isLocked()) {
                return currA;
            }
        }
        return null;
    }

    /**
     * Adds an existing hangar unit to one of the player's armies at an optional position, then updates that
     * army's cached battle value and re-sorts the armies list. Called in response to a server command that
     * assigns a unit (by ID) to an army (by ID).
     *
     * @param data delimiter-separated payload: army ID, unit ID, army BV, and insertion position (negative to append)
     */
    public void addArmyUnit(String data) {
        StringTokenizer ST = new StringTokenizer(data, DELIMITER);

        if (ST.hasMoreTokens()) {
            int army = TokenReader.readInt(ST);
            int unitId = TokenReader.readInt(ST);
            int bv = TokenReader.readInt(ST);
            int position = TokenReader.readInt(ST);
            if (position >= 0) {
                getArmy(army).addUnit(getUnit(unitId), position);
            } else {
                getArmy(army).addUnit(getUnit(unitId));
            }
            getArmy(army).setBV(bv);
            sortArmies();
        }
    }

    /**
     * Method that resorts every unit. Inefficient, but we hate clients. Because we're evil. So there.
     * <p>
     * Same layered primary/secondary/tertiary sort-key approach as {@link #sortHangar()}, but applied to the
     * {@code Armies} vector via {@link CArmyComparator} using the player's configured army sort keys (e.g. Name,
     * Battle Value, Max Tonnage).
     *
     * @author urgru 4.4.05
     */
    public void sortArmies() {

        // load configs
        String primeSortOrder = client.getConfigParam("PRIMARY_ARMY_SORT_ORDER");
        String secondarySortOrder = client.getConfigParam("SECONDARY_ARMY_SORT_ORDER");
        String tertiarySortOrder = client.getConfigParam("TERTIARY_ARMY_SORT_ORDER");

        // Choices [note - this array must be duplicated in CHQPanel's
        // maybeShowPopup()]
        String[] choices = { "Name", "Battle Value", "ID Number", "Max Tonnage", "Avg Walk MP", "Avg Jump MP",
                             "Number Of Units", "No Sort" };

        // determine which sort will dominate
        int primarySort = CArmyComparator.ARMY_SORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (primeSortOrder.equals(choices[i])) {
                primarySort = i;
            }
        }

        // determine secondary sort
        int secondarySort = CArmyComparator.ARMY_SORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (secondarySortOrder.equals(choices[i])) {
                secondarySort = i;
            }
        }

        // determine tertiary sort
        int tertiarySort = CArmyComparator.ARMY_SORT_NONE;
        for (int i = 0; i < choices.length; i++) {
            if (tertiarySortOrder.equals(choices[i])) {
                tertiarySort = i;
            }
        }

        // we know this holds CUnits. Can safely cast.
        Vector<CArmy> armiesArray = new Vector<>(Armies);

        // run third sort
        if ((tertiarySort != primarySort) &&
                  (tertiarySort != secondarySort) &&
                  (tertiarySort != CArmyComparator.ARMY_SORT_NONE)) {
            armiesArray.sort(new CArmyComparator(tertiarySort));
        }

        // run the second sort
        if ((primarySort != secondarySort) && (secondarySort != CArmyComparator.ARMY_SORT_NONE)) {
            armiesArray.sort(new CArmyComparator(secondarySort));
        }

        // now the primary sort
        if (primarySort != CArmyComparator.ARMY_SORT_NONE) {
            armiesArray.sort(new CArmyComparator(primarySort));
        }

        // overwrite the hangar with a new arraylist constructed from the
        // unitsArray.
        Vector<CArmy> Army2 = new Vector<>(1, 1);
        Army2.addAll(armiesArray);

        // replace the hangar and flush the array
        Armies = Army2;
    }

    /**
     * Removes a single unit from one of the player's armies, updates that army's C3 network and commander
     * assignment bookkeeping accordingly, refreshes the army's cached BV, and triggers a GUI refresh of the HQ
     * panel so the change is reflected on screen.
     *
     * @param data delimiter-separated payload: army ID, unit ID, and the army's updated BV
     */
    public void removeArmyUnit(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);
            int unitId = TokenReader.readInt(stringTokenizer);
            int bv = TokenReader.readInt(stringTokenizer);

            java.util.Iterator<Unit> i = getArmy(army).getUnits().iterator();
            while (i.hasNext()) {
                if (i.next().getId() == unitId) {
                    i.remove();
                    getArmy(army).removeCommander(unitId); //Baruk Khazad!  20151108c it is safe to removeCommander regardless of whether it isCommander or not
                    break;
                }
            }

            getArmy(army).setBV(bv);
            getArmy(army).getC3Network().remove(unitId);
        }
        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
    }

    /**
     * Method called from PL| that updates a CArmy's legalOperations tree. The legal-operations set records
     * which scenario/operation types an army is currently eligible to be used in. A "CLEAR" payload wipes the
     * legal-operations set for every army the player owns; otherwise the payload targets one army by ID and
     * applies a sequence of add ("a")/remove ("r") operations by name. Finishes by refreshing the client's
     * attack menu, since which operations are launchable may have changed.
     *
     * @param data either the literal "CLEAR", or "*"-delimited: army ID followed by repeated (mode, operation name) pairs
     */
    public void updateOperations(String data) {

        if (data.equals("CLEAR")) {
            for (CArmy army : getArmies()) {
                army.getLegalOperations().clear();
            }
            return;
        }

        StringTokenizer tokenizer = new StringTokenizer(data, "*");
        int armyID = TokenReader.readInt(tokenizer);
        CArmy army = getArmy(armyID);

        // System.err.println(" ArmyID: "+armyID+ " Army: "+army);
        if (army == null) {
            return;
        }

        while (tokenizer.hasMoreTokens()) {
            String mode;
            String name;

            try {
                mode = TokenReader.readString(tokenizer);
                name = TokenReader.readString(tokenizer);
            } catch (NoSuchElementException e) {
                LOGGER.debug(e, "updateOperations: No more tokens");
                return;
            }

            if (mode.equals("a")) {
                army.getLegalOperations().add(name);
            } else if (mode.equals("r")) {
                army.getLegalOperations().remove(name);
            }
        }// end while(more tokens)

        // update the CMainFrame Attack menu
        client.getMainFrame().updateAttackMenu();

    }// end updateOperations

    /**
     * @return the live, mutable collection of every army ({@link CArmy}) the player has organized
     */
    public Vector<CArmy> getArmies() {
        return Armies;
    }

    /**
     * Moves a unit already present in an army to a new position/slot within that same army's unit list (e.g.
     * reordering formation slots in the UI). Implemented as a remove-then-reinsert rather than an in-place move.
     *
     * @param data delimiter-separated payload: army ID, unit ID, and the new position index
     */
    public void repositionArmyUnit(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);

        int army = TokenReader.readInt(stringTokenizer);
        int unitId = TokenReader.readInt(stringTokenizer);
        int position = TokenReader.readInt(stringTokenizer);

        CArmy cArmy = getArmy(army);

        // remove the unit
        Iterator<Unit> i = cArmy.getUnits().iterator();
        while (i.hasNext()) {
            if (i.next().getId() == unitId) {
                i.remove();
                break;
            }
        }

        // then re-add the unit
        getArmy(army).addUnit(getUnit(unitId), position);

    }

    /**
     * Sets a unit's status (e.g. OK, damaged, unmaintained). Under the "advance repairs" house rule, an
     * incoming STATUS_UNMAINTAINED is silently overridden back to STATUS_OK - i.e. that house rule doesn't track
     * an "unmaintained" state the same way, so the flag is effectively ignored/normalized rather than applied
     * verbatim. Does nothing if the referenced unit can't be found.
     *
     * @param data delimiter-separated payload: unit ID and new status code
     */
    public void setUnitStatus(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int unitId = TokenReader.readInt(stringTokenizer);
            int status = TokenReader.readInt(stringTokenizer);
            CUnit unit = getUnit(unitId);

            if (unit == null) {
                return;
            }

            if (client.isUsingAdvanceRepairs() && (status == Unit.STATUS_UNMAINTAINED)) {
                unit.setStatus(Unit.STATUS_OK);
            } else {
                unit.setStatus(status);
            }
        }
    }

    /**
     * Renames an army. A name of "-1" is treated as a sentinel for "no name" and converted to an empty string.
     * Does nothing if the army can't be found.
     *
     * @param data delimiter-separated payload: army ID and new name (or "-1" to clear the name)
     */
    public void setArmyName(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);
            String name = TokenReader.readString(stringTokenizer);

            if (name.equals("-1")) {
                name = "";
            }

            if (getArmy(army) != null) {
                getArmy(army).setName(name);
            }
        }
    }

    /**
     * Locks an army (player-initiated) so it can be committed to deployment; delegates to {@link CArmy#playerLockArmy()}.
     *
     * @param aid ID of the army to lock
     */
    public void playerLockArmy(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).playerLockArmy();
        }
    }

    /**
     * Unlocks a previously player-locked army; delegates to {@link CArmy#playerUnlockArmy()}.
     *
     * @param aid ID of the army to unlock
     */
    public void playerUnlockArmy(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).playerUnlockArmy();
        }
    }

    /**
     * Flips an army's disabled flag; delegates to {@link CArmy#toggleArmyDisabled()}.
     *
     * @param aid ID of the army to toggle
     */
    public void toggleArmyDisabled(int aid) {
        if (getArmy(aid) != null) {
            getArmy(aid).toggleArmyDisabled();
        }
    }

    /**
     * Overwrites an army's cached Battle Value with a server-provided figure.
     *
     * @param data delimiter-separated payload: army ID and new BV; if the army ID doesn't resolve, the bad ID is logged and the update is skipped
     */
    public void setArmyBV(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);

            if (getArmy(army) != null) {
                getArmy(army).setBV(TokenReader.readInt(stringTokenizer));
            } else {
                LOGGER.debug(String.format("Bad Army id: %s", army));
            }
        }
    }

    /**
     * Sets the BV matching-window bounds (lower/upper limiters) for an army, used to constrain what opposing
     * force size/strength is considered a "fair" match against this army.
     *
     * @param data delimiter-separated payload: army ID, lower limit, upper limit
     */
    public void setArmyLimit(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);
            int lowerLimit = TokenReader.readInt(stringTokenizer);
            int upperLimit = TokenReader.readInt(stringTokenizer);

            getArmy(army).setLowerLimiter(lowerLimit);
            getArmy(army).setUpperLimiter(upperLimit);
        }
    }

    /**
     * Sets the desired opposing-force size multiplier/ratio for an army (used when generating an AI/auto
     * opposing force for a scenario).
     *
     * @param data delimiter-separated payload: army ID and opposing-force size factor
     */
    public void setArmyOpForceSize(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);
            float opForceSize = TokenReader.readFloat(stringTokenizer);

            getArmy(army).setOpForceSize(opForceSize);
        }

    }

    /**
     * Sets an army's server-side lock flag directly (distinct from the player-initiated lock/unlock convenience
     * methods above; this is driven by an explicit server command carrying the desired lock state).
     *
     * @param data delimiter-separated payload: army ID and desired locked state
     */
    public void setArmyLock(String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        if (stringTokenizer.hasMoreTokens()) {
            int army = TokenReader.readInt(stringTokenizer);
            boolean lock = TokenReader.readBoolean(stringTokenizer);
            getArmy(army).setLocked(lock);
        }
    }

    /**
     * Replaces this player's entire personal pilot queue wholesale.
     *
     * @param queue new personal pilot queue to install
     */
    public void setPlayerPersonalPilotQueue(CPersonalPilotQueues queue) {
        personalPilotQueue = queue;
    }

    /** @return the player's queue of personal (named) pilots available for assignment to units */
    public CPersonalPilotQueues getPersonalPilotQueue() {
        return personalPilotQueue;
    }

    /*
     * Hangar sorting mechanisms. client and server need not order hangars in
     * the same fashion, since all transactions (after the initial data feed)
     * take place on a unit by unit basis.
     *
     * Sort options: - BV - Name - Type - Unit ID - Weight - No sort [load
     * order]
     *
     * BV is (for all intents and purposes) an exclusive sort. The others can
     * lead to significant clustering. Hence, secondary filters can be applied.
     */

    /**
     * Exclude method, called after receipt of PL|AEU| (Admin Exclude Update). Because NP lists are expected to be small
     * (2-5 players), the entire list is sent every time.
     *
     * @author urgru 4.3.05
     */
    /**
     * @param buffer delimited list of player names excluded from admin-level chat/notices ("0" entries are skipped)
     * @param token  delimiter used to tokenize {@code buffer}
     */
    public void setAdminExcludes(String buffer, String token) {
        adminExcludes.clear();
        StringTokenizer stringTokenizer = new StringTokenizer(buffer, token);

        while (stringTokenizer.hasMoreElements()) {
            String curr = TokenReader.readString(stringTokenizer);

            if (!curr.equals("0")) {
                adminExcludes.add(curr);
            }
        }

        client.getMainFrame().getMainPanel().getUserListPanel().repaint();
    }

    /*
     * Hangar sorting mechanisms. client and server need not order hangars in
     * the same fashion, since all transactions (after the initial data feed)
     * take place on a unit by unit basis.
     *
     * Sort options: - BV - Name - Type - Unit ID - Weight - No sort [load
     * order]
     *
     * BV is (for all intents and purposes) an exclusive sort. The others can
     * lead to significant clustering. Hence, secondary filters can be applied.
     */

    /**
     * Exclude method, called after receipt of PL|PEU| (Player Exclude Update). Because NP lists are expected to be
     * small (2-5 players), the entire list is sent every time.
     */
    /**
     * @param buffer delimited list of player names excluded from player-level chat/notices ("0" entries are skipped)
     * @param token  delimiter used to tokenize {@code buffer}
     */
    public void setPlayerExcludes(String buffer, String token) {
        playerExcludes.clear();
        StringTokenizer stringTokenizer = new StringTokenizer(buffer, token);
        while (stringTokenizer.hasMoreElements()) {
            String curr = TokenReader.readString(stringTokenizer);
            if (!curr.equals("0")) {
                playerExcludes.add(curr);
            }
        }
        client.getMainFrame().getMainPanel().getUserListPanel().repaint();
    }

    /** @return names of players excluded from admin-level chat/notices for this player */
    public ArrayList<String> getAdminExcludes() {
        return adminExcludes;
    }

    /** @return names of players excluded from player-level chat/notices for this player */
    public ArrayList<String> getPlayerExcludes() {
        return playerExcludes;
    }

    /**
     * Computes how many hangar bays (technicians' worth of space) a unit of the given type/weight class would
     * consume. ProtoMeks always take 0. Foot/Anti-Mek-Foot infantry can be configured (via
     * "FootInfTakeNoBays") to also take 0. Otherwise the base bay cost comes from server configs and, unless
     * advance repairs are in use, is further adjusted by a per-purchase bay modifier (e.g. an AsTech skill
     * discount). Result is clamped to be non-negative.
     *
     * @param typeID      unit type (see {@link Unit} type constants)
     * @param weightClass unit weight class
     * @param bayMod      additional bay modifier (e.g. from pilot skills), applied only when not using advance repairs
     * @param model       unit model name, used to special-case foot infantry variants
     * @return number of hangar bays required, never negative
     */
    public int getHangarSpaceRequired(int typeID, int weightClass, int bayMod, String model) {
        if (typeID == Unit.PROTOMEK) {
            return 0;
        }

        if ((typeID == Unit.INFANTRY) &&
                  MathUtility.parseBoolean(client.getServerConfigs("FootInfTakeNoBays"), false)) {

            // check types
            boolean isFoot = model.startsWith("Foot");
            boolean isAMFoot = model.startsWith("Anti-Mek Foot");

            if (isFoot || isAMFoot) {
                return 0;
            }
        }

        int result;
        String techAmount = String.format("TechsFor%s%s", Unit.getWeightClassDesc(weightClass), Unit.getTypeClassDesc(typeID));
        result = MathUtility.parseInt(client.getServerConfigs(techAmount), 0);

        // Apply Pilot Mods (AsTech skill)
        if (!client.isUsingAdvanceRepairs()) {
            result += bayMod;
        }

        // no negative techs
        if (result < 0) {
            result = 0;
        }

        return result;
    }// end getHangarSpaceRequired()

    /** @return the resolved {@link House} object for this player's home faction */
    public House getMyHouse() {
        return myHouse;
    }

    /**
     * Applies a repair-result payload to one of the player's units, then rebuilds that unit's underlying
     * MegaMek {@link megamek.common.units.Entity} to reflect the (now repaired) damage state.
     *
     * @param data tokenizer positioned at: unit ID, followed by the encoded repair/damage string
     */
    public void applyUnitRepairs(StringTokenizer data) {
        CUnit unit = getUnit(TokenReader.readInt(data));
        unit.applyRepairs(TokenReader.readString(data));
    }

    /**
     * Bulk-updates the total technician headcount per tech tier from a "%"-delimited list of counts, one per
     * slot in order.
     *
     * @param data "%"-delimited list of technician counts, indexed by tech tier
     */
    public void updateTotalTechs(String data) {
        StringTokenizer techs = new StringTokenizer(data, "%");
        int slot = 0;

        while (techs.hasMoreTokens()) {
            setTotalTechs(slot, TokenReader.readInt(techs));
            slot++;
        }
    }

    /**
     * @param slot  tech-tier index into {@link #totalTechs}
     * @param techs new total technician count for that tier
     */
    public void setTotalTechs(int slot, int techs) {
        totalTechs.set(slot, techs);
    }

    /** @return total technician headcount, indexed by tech tier */
    public ArrayList<Integer> getTotalTechs() {
        return totalTechs;
    }

    /**
     * Bulk-updates the currently-available (unoccupied) technician headcount per tech tier from a
     * "%"-delimited list of counts, one per slot in order.
     *
     * @param data "%"-delimited list of available technician counts, indexed by tech tier
     */
    public void updateAvailableTechs(String data) {
        StringTokenizer techs = new StringTokenizer(data, "%");
        int slot = 0;

        while (techs.hasMoreTokens()) {
            setAvailableTechs(slot, TokenReader.readInt(techs));
            slot++;
        }
    }

    /**
     * @param slot  tech-tier index into {@link #availableTechs}
     * @param techs new available technician count for that tier
     */
    public void setAvailableTechs(int slot, int techs) {
        availableTechs.set(slot, techs);
    }

    /** @return currently-available technician headcount, indexed by tech tier */
    public ArrayList<Integer> getAvailableTechs() {
        return availableTechs;
    }

    /** @return the location index currently selected for an in-progress repair operation */
    public int getRepairLocation() {
        return repairLocation;
    }

    /** @param loc new selected repair location index */
    public void setRepairLocation(int loc) {
        repairLocation = loc;
    }

    /** @return the technician type/skill currently selected for an in-progress repair operation */
    public int getRepairTechType() {
        return repairTechType;
    }

    /** @param type new selected repair technician type */
    public void setRepairTechType(int type) {
        repairTechType = type;
    }

    /** @return the number of retries used so far for the current repair attempt */
    public int getRepairRetries() {
        return repairRetries;
    }

    /** @param retries new retry count for the current repair attempt */
    public void setRepairRetries(int retries) {
        repairRetries = retries;
    }

    /** Clears all transient in-progress repair selection state (location, tech type, retries) back to defaults. */
    public void resetRepairs() {
        repairLocation = 0;
        repairTechType = 0;
        repairRetries = 0;
    }

    /** @return how many conventional minefields the player may currently deploy */
    public int getConventionalMinesAllowed() {
        return conventionalMinesAllowed;
    }

    /** @param mines new conventional minefield quota */
    public void setConventionalMinesAllowed(int mines) {
        conventionalMinesAllowed = mines;
    }

    /** @return how many vibrabombs the player may currently deploy */
    public int getVibraMinesAllowed() {
        return vibraMinesAllowed;
    }

    /** @param mines new vibrabomb quota */
    public void setVibraMinesAllowed(int mines) {
        vibraMinesAllowed = mines;
    }

    /**
     * Sets both minefield quotas from a single tokenizer, in order: conventional mines, then vibrabombs.
     *
     * @param stringTokenizer tokenizer positioned at the conventional mine quota, followed by the vibrabomb quota
     */
    public void setMines(StringTokenizer stringTokenizer) {
        setConventionalMinesAllowed(TokenReader.readInt(stringTokenizer));
        setVibraMinesAllowed(TokenReader.readInt(stringTokenizer));
    }

    /**
     * Applies a batch of faction-specific server config key/value pairs (requested by {@link #setHouse(String)}
     * after a faction change). A payload beginning with "DONE#DONE" is a sentinel signaling the batch is
     * complete, at which point the client's "waiting" flag is cleared so it can resume normal operation.
     *
     * @param data either "DONE#DONE..." to signal completion, or a delimiter-separated sequence of key/value pairs
     */
    public void setFactionConfigs(String data) {

        if (data.startsWith("DONE#DONE")) {
            client.setWaiting(false);
            return;
        }

        StringTokenizer stringTokenizer = new StringTokenizer(data, DELIMITER);
        while (stringTokenizer.hasMoreTokens()) {
            String key = TokenReader.readString(stringTokenizer);
            String value = TokenReader.readString(stringTokenizer);

            client.setServerConfigs(key, value);
        }
        client.setWaiting(false);
    }

    /** @return the player's cached spare-parts inventory, used only under the "UsePartsRepair" house rule */
    public UnitComponents getPartsCache() {
        return partsCache;
    }

    /**
     * Resolves the player's current {@link SubFaction} within their home {@link House}.
     *
     * @return the matching {@link SubFaction}, or a brand-new default {@code SubFaction} instance if the player's
     *         {@link #subFactionName} doesn't (or no longer) match anything in {@code myHouse}'s sub-faction list
     */
    public SubFaction getSubFaction() {

        SubFaction mySubFaction = myHouse.getSubFactionList().get(subFactionName);
        if (mySubFaction == null) {
            return new SubFaction();
        }

        return mySubFaction;
    }

    /**
     * @param name name of the {@link SubFaction} to join within the player's current {@link House}
     */
    public void setSubFaction(String name) {
        subFactionName = name;
    }

    /**
     * @return the "AccessLevel" config value of the player's current sub-faction, or 0 if the player has no
     *         resolvable sub-faction
     */
    public int getSubFactionAccess() {

        SubFaction mySubFaction = myHouse.getSubFactionList().get(subFactionName);

        if (mySubFaction == null) {
            return 0;
        }

        return MathUtility.parseInt(mySubFaction.getConfig("AccessLevel"), 0);

    }

    /** @return the name of the player's current {@link SubFaction} */
    public String getSubFactionName() {
        return subFactionName;
    }

    /**
     * @param type   unit type index (see {@link Unit} type constants)
     * @param weight unit weight class index
     * @return the current sliding purchase-price penalty for that (type, weight) combination
     */
    public int getHangarPurchasePenalty(int type, int weight) {
        return hangarPurchasePenalties[type][weight];
    }

    /**
     * Decodes a "*"-delimited hangar-penalty payload: first the overall {@link #hangarPenalty} surcharge, then
     * one purchase-price penalty value for every (unit type, weight class) combination, iterated type-major.
     * Refreshes the Hangar Status panel afterward so any displayed prices reflect the new penalties.
     *
     * @param readString "*"-delimited payload: hangar penalty, followed by a full type-by-weight-class penalty grid
     */
    public void parseHangarPenaltyString(String readString) {
        StringTokenizer stringTokenizer = new StringTokenizer(readString, "*");
        setHangarPenalty(Integer.parseInt(stringTokenizer.nextToken()));
        for (int type = Unit.MEK; type < Unit.MAX_BUILD; type++) {
            for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
                setHangarPurchasePenalty(type, weight, Integer.parseInt(stringTokenizer.nextToken()));
            }
        }
        client.getMainFrame().getMainPanel().getHSPanel().updateDisplay();
    }

    /**
     * @param type   unit type index (see {@link Unit} type constants)
     * @param weight unit weight class index
     * @param p      new purchase-price penalty for that (type, weight) combination
     */
    public void setHangarPurchasePenalty(int type, int weight, int p) {
        hangarPurchasePenalties[type][weight] = p;
    }
}
