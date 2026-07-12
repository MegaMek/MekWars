/*
 * MekWars - Copyright (C) 2006
 *
 * Original author - Torren (torren@users.sourceforge.net)
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

package mekwars.common.util;

import java.util.Comparator;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.TechConstants;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Tracks a bag of salvaged/available unit "components" (armor, internal structure, individual pieces of
 * equipment, ammo, etc., keyed by their internal MegaMek name) together with a count of how many of each are on
 * hand, and provides operations to accumulate, spend, and report on them. Used by the campaign salvage/repair
 * system: components are harvested from a destroyed/salvaged {@link Entity} (see {@link #repoUnit}/
 * {@link #canRepoUnit}), stored here as a simple name-to-count table, and later consumed when repairing another
 * unit. Also provides HTML table rendering ({@link #tableComponents}) and pipe/token-delimited
 * serialization ({@link #toString(String)}/{@link #fromString}) for persisting this data.
 */
public class UnitComponents {
    private final static MMLogger LOGGER = MMLogger.create(UnitComponents.class);
    /** Component name (MegaMek internal equipment name, or a synthetic name like "Armor"/"IS") to count on hand. */
    private final Hashtable<String, Integer> components = new Hashtable<>();

    /**
     * Renders this instance's own {@link #components} table as an HTML table. See
     * {@link #tableComponents(Hashtable, int)}.
     *
     * @param year the in-universe year, used to determine each component's tech base (IS/Clan/All) for display
     * @return an HTML {@code <table>} listing every component and its count/tech base
     */
    public String tableComponents(int year) {
        return tableComponents(components, year);
    }

    /**
     * Builds an HTML table listing each component's display name, count, and tech base, laid out two
     * name/count/tech groups per row. Components are grouped by their resolved display name (via
     * {@link #getName(String)}) - if two different internal keys resolve to the same display name they end up
     * under a single {@code keys} entry, so one of the two counts silently overwrites the other in the
     * {@code keys} map (the returned counts come from {@code parts}, keyed by the original names, so counts
     * themselves are not lost, but the sort/iteration order only has one row per display name).
     *
     * @param parts the component name-to-count table to render (typically {@link #components}, but callers may
     *              pass an arbitrary table)
     * @param year  the in-universe year, used to resolve each component's tech base (IS/Clan/All) via
     *              {@link #getTech(String, int)}
     * @return an HTML {@code <table>} string; rows alternate between opening a new {@code <tr>} every second
     *     component
     */
    public String tableComponents(Hashtable<String, Integer> parts, int year) {

        StringBuilder result = new StringBuilder();

        Comparator<? super Object> stringCompare = UnitComponents.stringComparator();

        Hashtable<String, String> keys = new Hashtable<>();

        for (String key : parts.keySet()) {
            keys.put(UnitComponents.getName(key), key);
        }

        Vector<String> equipment = new Vector<>(keys.keySet());
        equipment.sort(stringCompare);

        result.append(
              "<table><tr><th>Component</th><th># of Crits</th><th>Tech</th><th>Component</th><th># of Crits</th><th>Tech</th></tr>");
        result.append("<tr><td>");
        for (int pos = 0; pos < equipment.size(); pos++) {
            String key = equipment.get(pos);
            result.append(key);
            result.append("</td><td>");
            result.append(parts.get(keys.get(key)));
            result.append("</td><td>");
            result.append(UnitComponents.getTech(keys.get(key), year));
            if ((pos % 2) == 1) {
                result.append("</td></tr>");
                result.append("<tr><td>");
            } else {
                result.append("</td><td>");
            }

        }
        result.append("</table>");

        return result.toString();
    }

    /**
     * @return a case-insensitive natural-order string comparator (compares by {@code toLowerCase()}), typed as a
     *     raw {@code Comparator<Object>} unsafely cast to {@code Comparator<? super Object>}; callers must only
     *     use it to compare {@link String} instances or a {@link ClassCastException} will result.
     */
    public static Comparator<? super Object> stringComparator() {
        return (Comparator<Object>) (o1, o2) -> {
            String s1 = ((String) o1).toLowerCase();
            String s2 = ((String) o2).toLowerCase();
            return s1.compareTo(s2);
        };
    }

    /**
     * Resolves a component key to its human-readable display name.
     *
     * @param crit the component key: either a MegaMek internal equipment name (resolvable via
     *             {@link EquipmentType#get(String)}) or a synthetic name for things that aren't real
     *             {@link EquipmentType}s in MegaMek (e.g. "Armor", "IS", "Engines", "Actuators", "Cockpit",
     *             "Sensors")
     * @return the equipment's display name if {@code crit} resolves to a known {@link EquipmentType}; otherwise
     *     {@code crit} itself is returned unchanged (used for the synthetic non-equipment names above)
     */
    public static String getName(String crit) {
        EquipmentType eq = EquipmentType.get(crit);
        //Armor,IS,Engines,Actuators,Cockpit,Sensors anything that doesn't
        //make a normal object in MM
        if (eq == null) {
            return crit;
        }

        return eq.getName();
    }

    /**
     * Determines the tech base label to display for a component in the given in-universe year.
     *
     * @param crit the component key (see {@link #getName(String)} for the two kinds of key this accepts)
     * @param year the in-universe year used to evaluate tech level/Clan status
     * @return {@code "All"} if {@code crit} does not resolve to a known {@link EquipmentType} (synthetic
     *     component names), or if it resolves but its tech level in {@code year} is {@link TechConstants#T_ALL}
     *     or at/below {@link TechConstants#T_TW_ALL}; {@code "Clan"} if {@link UnitUtils#isClanEQ} reports it as
     *     Clan tech for {@code year}; otherwise {@code "IS"}
     */
    public static String getTech(String crit, int year) {
        EquipmentType eq = EquipmentType.get(crit);


        //Armor, IS, Engines, Actuators, Cockpit, Sensors anything that doesn't
        //make a normal object in MM
        if (eq == null) {

            return "All";
        } else {

            if (UnitUtils.isClanEQ(eq, year)) {
                return "Clan";
            }
            if ((eq.getTechLevel(year) == TechConstants.T_ALL) ||
                      (eq.getTechLevel(year) <= TechConstants.T_TW_ALL)) {
                return "All";
            }

            return "IS";

        }

    }

    /**
     * Serializes the current {@link #components} table to a flat delimited string, in undefined (hashtable
     * iteration) order, as repeating {@code <key><token><count><token>} groups. Components with a count less
     * than 1 are skipped.
     *
     * @param token the delimiter to place after each key and after each count
     * @return the delimited string, or {@code "<token> <token>"} (token, a space, then token again) if
     *     {@link #components} is empty
     */
    public String toString(String token) {
        StringBuilder result = new StringBuilder();

        if (components.isEmpty()) {
            return token + " " + token;
        }
        for (String key : components.keySet()) {

            if (components.get(key) < 1) {
                continue;
            }
            result.append(key);
            result.append(token);
            result.append(components.get(key));
            result.append(token);
        }
        return result.toString();

    }

    /**
     * Replaces the contents of {@link #components} by parsing {@code data} as a sequence of
     * {@code key<token>count<token>} pairs, the inverse of {@link #toString(String)}. Clears any existing
     * contents first, so on parse failure the table may be left partially populated (whatever was parsed before
     * the exception) rather than restored to its prior state.
     *
     * @param data  the delimited string to parse
     * @param token the delimiter string used to split {@code data} into tokens
     */
    public void fromString(String data, String token) {

        StringTokenizer stringTokenizer = new StringTokenizer(data, token);

        try {
            components.clear();
            while (stringTokenizer.hasMoreTokens()) {
                String key = stringTokenizer.nextToken();
                if (!stringTokenizer.hasMoreElements()) {
                    return;
                }
                int value = MathUtility.parseInt(stringTokenizer.nextToken(), -1);
                components.put(key, value);
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to parse: {} {}", data, token);
        }

    }

    /**
     * Replaces the contents of {@link #components} by consuming key/value token pairs directly from an
     * already-constructed {@link StringTokenizer} (as opposed to {@link #fromString(String, String)}, which
     * builds its own tokenizer from a raw string and delimiter). Clears any existing contents first, so on parse
     * failure (e.g. an odd number of remaining tokens) the table may be left partially populated.
     *
     * @param stringTokenizer tokenizer positioned at the start of the key/value pairs to read
     */
    public void fromString(StringTokenizer stringTokenizer) {

        try {
            components.clear();
            while (stringTokenizer.hasMoreTokens()) {
                components.put(stringTokenizer.nextToken(), MathUtility.parseInt(stringTokenizer.nextToken(), -1));
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to parse from Tokenizer: {} {}", stringTokenizer.toString(),
                  stringTokenizer.toString());
        }

    }

    /**
     * Merges another name-to-count table into {@link #components}, summing counts for keys present in both.
     *
     * @param parts the components to add in
     */
    public void add(Hashtable<String, Integer> parts) {

        for (String part : parts.keySet()) {

            if (components.containsKey(part)) {
                components.put(part, components.get(part) + parts.get(part));
            } else {
                components.put(part, parts.get(part));
            }
        }
    }

    /**
     * Checks (without mutating anything) whether repairing {@code repoUnit} using parts drawn from
     * {@code mainUnit}'s intact/undamaged critical slots plus this instance's stored {@link #components} would
     * supply everything {@code repoUnit} needs, and reports any shortfall as an HTML fragment.
     * <p>
     * Builds a per-part inventory of {@code mainUnit} (one entry per undamaged critical slot, with ammo bins
     * additionally tallying remaining shots under the ammo's internal name, plus separate front/rear armor and
     * internal-structure totals) and a similar "needed" inventory for {@code repoUnit} (this one does not skip
     * damaged slots, since a damaged slot on the unit being repaired is exactly what needs replacing), then for
     * each part {@code repoUnit} needs, compares the amount available (this instance's stockpile plus
     * {@code mainUnit}'s matching parts) against the amount required.
     *
     * <p><b>Note:</b> the armor-total key for {@code mainUnit} is computed via
     * {@code UnitUtils.getCritName(mainUnit, UnitUtils.LOC_CENTER_TORSO, 0, true)} while the corresponding key for
     * {@code repoUnit} uses {@code UnitUtils.getCritName(repoUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true)} — different
     * "slot" constants. This happens to still line up today because {@code getCritName}'s armor branch only
     * special-cases {@code slot == LOC_INTERNAL_ARMOR}, treating every other slot value (including both
     * {@code LOC_CENTER_TORSO} and {@code LOC_FRONT_ARMOR}) identically; both calls end up deriving the key purely
     * from {@code getArmorType(location)}. It is fragile, since it silently relies on those two constants both
     * falling into the same "not internal armor" bucket in {@code getCritName} rather than being intentionally
     * unified.
     *
     * @param mainUnit the donor unit whose intact parts are treated as available inventory
     * @param repoUnit the unit being evaluated for repair; its required parts are computed from all of its
     *                 critical slots (including damaged ones) plus its armor/internal structure
     * @return {@code ""} if every part {@code repoUnit} needs is fully covered; otherwise an HTML
     *     {@code <table>} fragment listing each shortfall as "{@code <missing amount> of <part name>}"
     */
    public String canRepoUnit(Entity mainUnit, Entity repoUnit) {

        Hashtable<String, Integer> mainUnitParts = new Hashtable<>();
        Hashtable<String, Integer> repoUnitParts = new Hashtable<>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, mainUnit.getInternal(location));
            armor += Math.max(0, mainUnit.getArmor(location));
            rear += Math.max(0, mainUnit.getArmor(location, true));
            for (int slot = 0; slot < mainUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = mainUnit.getCritical(location, slot);
                if ((crit == null) || crit.isDamaged()) {
                    continue;
                }

                part = UnitUtils.getCritName(mainUnit, slot, location, false);

                if (part.equalsIgnoreCase("Ammo Bin")) {
                    Mounted<?> mount = crit.getMount();
                    String ammoName = mount.getType().getInternalName();

                    if (mainUnitParts.containsKey(ammoName)) {
                        mainUnitParts.put(ammoName, mainUnitParts.get(ammoName) + mount.getUsableShotsLeft());
                    } else {
                        mainUnitParts.put(ammoName, mount.getUsableShotsLeft());
                    }
                    if (mainUnitParts.containsKey(part)) {
                        mainUnitParts.put(part, mainUnitParts.get(part) + 1);
                    } else {
                        mainUnitParts.put(part, 1);
                    }
                } else {
                    if (mainUnitParts.containsKey(part)) {
                        mainUnitParts.put(part, mainUnitParts.get(part) + 1);
                    } else {
                        mainUnitParts.put(part, 1);
                    }
                }

            }
        }

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_CENTER_TORSO, 0, true);

        mainUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);
        mainUnitParts.put(part, IS);


        IS = 0;
        armor = 0;
        rear = 0;

        for (int location = 0; location < repoUnit.locations(); location++) {
            IS += Math.max(0, repoUnit.getInternal(location));
            armor += Math.max(0, repoUnit.getArmor(location));
            rear += Math.max(0, repoUnit.getArmor(location, true));
            for (int slot = 0; slot < repoUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = repoUnit.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }

                part = UnitUtils.getCritName(repoUnit, slot, location, false);

                if (part.equalsIgnoreCase("Ammo Bin")) {
                    Mounted<?> mount = crit.getMount();
                    String ammoName = mount.getType().getInternalName();

                    if (repoUnitParts.containsKey(ammoName)) {
                        repoUnitParts.put(ammoName, repoUnitParts.get(ammoName) + mount.getUsableShotsLeft());
                    } else {
                        repoUnitParts.put(ammoName, mount.getUsableShotsLeft());
                    }
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                } else {
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                }
            }
        }

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        repoUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);

        repoUnitParts.put(part, IS);

        StringBuilder result = new StringBuilder("<table><tr>");
        int count = 0;
        boolean missingCrits = false;
        for (String key : repoUnitParts.keySet()) {

            if (!components.containsKey(key) && !mainUnitParts.containsKey(key)) {
                missingCrits = true;
                result.append("<td>");
                result.append(repoUnitParts.get(key)).append(" of ").append(UnitComponents.getName(key));
                result.append("</td>");
                if ((count++ % 4) == 3) {
                    result.append("</tr><tr>");
                }
            } else {

                int partAmount = 0;

                if (components.containsKey(key)) {
                    partAmount += components.get(key);
                }
                if (mainUnitParts.containsKey(key)) {
                    partAmount += mainUnitParts.get(key);
                }

                if (partAmount < repoUnitParts.get(key)) {
                    missingCrits = true;
                    result.append("<td>");
                    result.append(repoUnitParts.get(key) - partAmount)
                          .append(" of ")
                          .append(UnitComponents.getName(key));
                    result.append("</td>");
                    if ((count++ % 4) == 3) {
                        result.append("</tr><tr>");
                    }
                }
            }
        }

        result.append("</table>");

        //Have all the parts return nothing.
        if (!missingCrits) {
            return "";
        }

        return result.toString();
    }

    /**
     * Scraps {@code mainUnit} into this instance's {@link #components} stockpile (unconditionally, via
     * {@link #add}), then computes what {@code repoUnit} needs and consumes (removes) that amount from the
     * (now-augmented) stockpile.
     * <p>
     * Mirrors the "needed parts" computation in {@link #canRepoUnit} but does not check first whether enough is
     * available: this method always returns {@code true} regardless of whether the stockpile actually covered
     * everything {@code repoUnit} required. {@link #remove(String, int)} clamps at zero rather than reporting a
     * shortfall, so if the stockpile does not have enough of a part, that part's count simply drops to (or stays
     * at) zero and no error/signal is raised. Callers wanting to know in advance whether a repair is fully
     * covered should check with {@link #canRepoUnit} first.
     * <p><b>Bug:</b> the second loop, which tallies {@code repoUnit}'s required parts (internal structure, armor,
     * critical slot contents), iterates {@code location} from {@code 0} to {@code mainUnit.locations()} (not
     * {@code repoUnit.locations()}) while reading from {@code repoUnit} inside the loop body
     * (e.g. {@code repoUnit.getInternal(location)}, {@code repoUnit.getNumberOfCriticalSlots(location)}). If
     * {@code repoUnit} has more locations than {@code mainUnit}, some of {@code repoUnit}'s locations are never
     * accounted for; if {@code repoUnit} has fewer locations than {@code mainUnit}, this will index into
     * locations that do not exist on {@code repoUnit}.
     *
     * @param mainUnit the unit being scrapped for parts
     * @param repoUnit the unit being repaired using those parts (plus this instance's existing stockpile)
     * @return always {@code true} (see note above; the return value does not reflect whether the repair was
     *     actually fully supplied)
     */
    public boolean repoUnit(Entity mainUnit, Entity repoUnit) {

        Hashtable<String, Integer> repoUnitParts = new Hashtable<>();

        int IS = 0;
        int armor = 0;
        int rear = 0;
        String part;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, mainUnit.getInternal(location));
            armor += Math.max(0, mainUnit.getArmor(location));
            rear += Math.max(0, mainUnit.getArmor(location, true));
            for (int slot = 0; slot < mainUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = mainUnit.getCritical(location, slot);
                if ((crit == null) || crit.isDamaged()) {
                    continue;
                }

                part = UnitUtils.getCritName(mainUnit, slot, location, false);

                if (part.contains("Ammo")) {
                    Mounted<?> mount = crit.getMount();

                    this.add(part, mount.getUsableShotsLeft());
                    this.add("Ammo Bin", 1);
                } else {
                    this.add(part, 1);
                }
            }
        }

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        this.add(part, armor + rear);

        part = UnitUtils.getCritName(mainUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);
        this.add(part, IS);

        IS = 0;
        armor = 0;
        rear = 0;

        for (int location = 0; location < mainUnit.locations(); location++) {
            IS += Math.max(0, repoUnit.getInternal(location));
            armor += Math.max(0, repoUnit.getArmor(location));
            rear += Math.max(0, repoUnit.getArmor(location, true));
            for (int slot = 0; slot < repoUnit.getNumberOfCriticalSlots(location); slot++) {
                CriticalSlot crit = repoUnit.getCritical(location, slot);
                if (crit == null) {
                    continue;
                }
                part = UnitUtils.getCritName(repoUnit, slot, location, false);

                if (part.contains("Ammo")) {
                    Mounted<?> mount = crit.getMount();

                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + mount.getUsableShotsLeft());
                    } else {
                        repoUnitParts.put(part, mount.getUsableShotsLeft());
                    }
                    if (repoUnitParts.containsKey("Ammo Bin")) {
                        repoUnitParts.put("Ammo Bin", repoUnitParts.get("Ammo Bin") + 1);
                    } else {
                        repoUnitParts.put("Ammo Bin", 1);
                    }
                } else {
                    if (repoUnitParts.containsKey(part)) {
                        repoUnitParts.put(part, repoUnitParts.get(part) + 1);
                    } else {
                        repoUnitParts.put(part, 1);
                    }
                }
            }
        }

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_FRONT_ARMOR, 0, true);

        repoUnitParts.put(part, armor + rear);

        part = UnitUtils.getCritName(repoUnit, UnitUtils.LOC_INTERNAL_ARMOR, 0, true);

        repoUnitParts.put(part, IS);

        for (String key : repoUnitParts.keySet()) {
            remove(key, repoUnitParts.get(key));
        }

        return true;
    }

    /**
     * Adds {@code amount} of {@code part} to the stockpile. No-op if {@code amount} is less than 1 (so this
     * cannot be used to subtract via a negative amount; use {@link #remove(String, int)} for that).
     *
     * @param part   the component key to credit
     * @param amount the quantity to add; ignored if less than 1
     */
    public void add(String part, int amount) {

        if (amount < 1) {
            return;
        }

        if (components.containsKey(part)) {
            components.put(part, Math.max(0, components.get(part) + amount));
        } else {
            components.put(part, amount);
        }

    }

    /**
     * Removes up to {@code amount} of {@code part} from the stockpile, clamped at zero: if fewer than
     * {@code amount} are on hand, the entry is simply removed entirely rather than going negative, and no
     * indication is given that the requested amount exceeded what was available.
     *
     * @param key    the component key to debit
     * @param amount the quantity to remove; the sign is ignored ({@link Math#abs(int)} is applied), so passing a
     *               negative amount still subtracts
     */
    public void remove(String key, int amount) {

        if (components.get(key) == null) {
            return;
        }

        int parts = components.get(key);

        parts -= Math.abs(amount);

        if (parts <= 0) {
            components.remove(key);
        } else {
            components.put(key, parts);
        }

    }

    /**
     * @param key the component key to look up
     * @return the number of {@code key} currently on hand, or {@code 0} if none are stored
     */
    public int getPartsCritCount(String key) {

        if (components.get(key) == null) {
            return 0;
        }
        return components.get(key);
    }

    /**
     * @param crit   the component key to check
     * @param amount the quantity needed
     * @return {@code true} if at least {@code amount} of {@code crit} are on hand, {@code false} otherwise
     *     (including if none are stored at all)
     */
    public boolean hasEnoughCrits(String crit, int amount) {

        if (components.get(crit) == null) {
            return false;
        }

        return components.get(crit) >= amount;
    }

    /**
     * Removes all stockpiled components, emptying {@link #components}.
     */
    public void clear() {
        components.clear();
    }
}
