/*
 * Copyright (C) 2004 MekWars
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

package mekwars.common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import megamek.logging.MMLogger;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * Represents the political influence each {@link House} faction holds over a single {@link Planet}. This
 * is the core data structure behind the campaign's ownership model: as factions perform actions that shift
 * control of a planet, their share of "influence points" here rises or falls, and whichever faction holds a
 * clear plurality (see {@link #getOwner()}) is considered the planet's current owner.
 * <p>
 * An {@code Influences} instance is also reused to represent the *difference* between two influence
 * snapshots (see {@link #difference(Influences)}), e.g. for reporting how much influence changed after a
 * battle or time-tick, rather than always representing an absolute total.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class Influences implements MutableSerializable {
    private final static MMLogger LOGGER = MMLogger.create(Influences.class);
    /**
     * Maps {@link House} id (key) to that faction's current influence points (value) on the associated
     * planet. Factions with zero influence are not listed (only entries greater than 0 are kept).
     */
    private HashMap<Integer, Integer> influences = new HashMap<>();

    /**
     * Creates a new Influence table with a preset key/value map (house id -&gt; influence points).
     *
     * @param influences the initial influence map to use.
     */
    public Influences(HashMap<Integer, Integer> influences) {
        setInfluence(influences);
    }

    /**
     * Replaces the whole influence table.
     *
     * @param influences The new influences. Key=House id, Value=influence points.
     */
    public void setInfluence(HashMap<Integer, Integer> influences) {
        this.influences = influences;
    }

    /**
     * Create an empty Influence table (no faction has any influence yet).
     */
    public Influences() {
    }

    /**
     * Copy constructor: creates an independent snapshot of another Influences' table (defensive copy of
     * the underlying map).
     *
     * @param influences the Influences instance to copy.
     */
    public Influences(Influences influences) {
        setInfluence(new HashMap<>(influences.influences));
    }

    /**
     * Determines which faction currently owns the planet by finding the house with the strictly highest
     * influence value. Ties are treated as "no clear owner" (contested/neutral), returning {@code null}.
     * <p>
     * Implementation note: this builds a sorted {@link TreeSet} of houses (by id, to get a stable order
     * for equal-influence tie-breaking during the subsequent array sort), converts to an array, then sorts
     * descending by influence amount so the top contender is at index 0. If there's more than one house
     * and the second-highest has the same influence as the highest, ownership is considered tied/undecided.
     *
     * @return the id of the faction with a clear plurality of influence, or {@code null} if there are no
     *         factions, the top faction reference is null, or the top two are tied.
     */
    public Integer getOwner() {
        try {
            TreeSet<House> houseTreeSet = new TreeSet<>((o1, o2) -> {
                int i1 = -1;
                int i2 = -1;

                if (o1 != null) {
                    i1 = o1.getId();
                }

                if (o2 != null) {
                    i2 = o2.getId();
                }

                return Integer.compare(i1, i2);
            });

            houseTreeSet.addAll(this.getHouses());
            House[] factions = new House[houseTreeSet.size()];

            int i = 0;

            for (House house : houseTreeSet) {
                factions[i++] = house;
            }

            Arrays.sort(factions, (o1, o2) -> {
                int h1Id = -1;
                int h2Id = -2;

                if (o1 != null) {
                    h1Id = o1.getId();
                }

                if (o2 != null) {
                    h2Id = o2.getId();
                }

                int i1 = getInfluence(h1Id);
                int i2 = getInfluence(h2Id);
                return Integer.compare(i2, i1);
            });

            if (factions.length < 1) {
                return null;
            }

            House faction = factions[0];
            if (faction == null) {
                return null;
            }

            // only one owner don't need to see who's the boss.
            if (factions.length == 1) {
                return faction.getId();
            }

            House faction2 = factions[1];

            if (faction2 != null && getInfluence((faction2.getId())) == getInfluence((faction.getId()))) {
                return null;
            }

            return faction.getId();
        } catch (Exception ex) {
            LOGGER.error(ex, "Error in Influenes.getOwner()");
            return null;
        }

    }

    /**
     * Resolves every {@link House} id currently tracked in this influence table into full {@link House}
     * objects, via the global {@link CampaignData#cd} registry.
     *
     * @return the set of houses that have a recorded (non-zero) influence entry on this planet.
     */
    public Set<House> getHouses() {
        Set<House> result = new HashSet<>();
        for (Integer integer : influences.keySet()) {
            House faction = CampaignData.cd.getHouse(integer);
            result.add(faction);
        }
        return result;
    }

    /**
     * Returns the influence points held by a specific faction on this planet.
     *
     * @param factionID the {@link House} id to look up.
     * @return the faction's influence points, or 0 if the faction has no recorded influence here.
     */
    public int getInfluence(int factionID) {
        if (!influences.containsKey(factionID)) {
            return 0;
        }

        return influences.get(factionID);
    }

    /**
     * Resets this planet's influence table and fairly (evenly) distributes {@code maxInfluence} points
     * across all the given factions, wiping out any previous influence data. If the total does not divide
     * evenly, the remainder ("bonus") is given entirely to {@code gainer} on top of its even share.
     *
     * @param factions     All of these factions gain as much as possible influence divided equal.
     * @param gainer       If there is a portion left, one faction get it all. This faction.
     * @param maxInfluence the total influence pool to distribute across {@code factions}.
     */
    public void setNeutral(List<House> factions, House gainer, int maxInfluence) {
        influences = new HashMap<>();
        for (int i = 0; i < factions.size(); i++) {
            House house = factions.get(i);
            influences.put((house.getId()), (maxInfluence / factions.size()));
        }

        if (maxInfluence % factions.size() != 0) {
            int bonus = maxInfluence % factions.size();
            if (influences.containsKey((gainer.getId()))) {
                influences.put((gainer.getId()), (influences.get(gainer.getId()) + bonus));
            } else {
                influences.put((gainer.getId()), (bonus));
            }
        }
    }

    /**
     * @return the number of distinct factions with recorded (non-zero) influence on this planet.
     */
    public int houseCount() {
        return influences.size();
    }

    /**
     * Transfers influence points into a winning faction on this planet, respecting the campaign's
     * influence caps: the winner's influence is never allowed to exceed {@code maxInfluence}, and the
     * source of the influence never drops below 0.
     * <p>
     * <b>Quirk:</b> the {@code loser} parameter is not always where the influence actually comes from.
     * The method first tries to draw the requested {@code amount} from the neutral/unclaimed influence
     * bucket (house id -1) rather than from {@code loser} directly. Only if the neutral bucket doesn't
     * hold enough does it fall back to {@code loser}: in that case, it removes the neutral entry, merges
     * whatever influence the neutral bucket had into {@code loser}'s existing total, and then draws the
     * (possibly further clamped) amount out of {@code loser}'s now-combined total. So when the neutral
     * pool is insufficient, {@code loser} can transiently gain influence (the leftover neutral pool) before
     * losing {@code amount} from it. If the requested {@code amount} would push the winner above
     * {@code maxInfluence}, it is first clamped down to whatever headroom remains. Any faction whose
     * resulting influence hits exactly 0 has its entry removed from the map (only non-zero entries are
     * kept). If {@code amount} is 0, this is a no-op that returns 0 immediately.
     * <p>
     * Use {@link #add(Influences)} instead if you don't want this clamping/neutral-pool-first behavior.
     *
     * @param winner       the faction gaining influence.
     * @param loser        the faction to draw from if the neutral (unclaimed) pool is insufficient.
     * @param amount       the requested amount of influence to move.
     * @param maxInfluence the influence cap that {@code winner} may not exceed.
     * @return the actual amount of influence moved (may be less than requested due to clamping).
     */
    public int moveInfluence(House winner, House loser, int amount, int maxInfluence) {
        if (amount == 0) {
            return 0;
        }

        int winnerId = winner.getId();
        int loserId = -1;

        int oldWinnerInfluence;
        int oldLoserInfluence;

        oldWinnerInfluence = getInfluence(winnerId);
        oldLoserInfluence = getInfluence(loserId);

        if (oldWinnerInfluence + amount >= maxInfluence) {
            amount = maxInfluence - oldWinnerInfluence;
        }

        if (amount > oldLoserInfluence) {
            influences.remove(loserId);
            loserId = loser.getId();
            influences.put(loserId, getInfluence(loserId) + oldLoserInfluence);
            oldLoserInfluence = getInfluence(loserId);
        }

        if (oldLoserInfluence < amount) {
            amount = oldLoserInfluence;
        }

        int winnerInfluence = oldWinnerInfluence + amount;
        int loserInfluence = oldLoserInfluence - amount;

        if (winnerInfluence == 0) {
            influences.remove(winnerId);
        } else {
            influences.put(winnerId, (winnerInfluence));
        }

        if (loserInfluence == 0) {
            influences.remove(loserId);
        } else {
            influences.put(loserId, (loserInfluence));
        }

        return amount;
    }

    /**
     * Returns whether the Influence zone belongs to a so-called "hot zone", which means, that it is in a critical
     * sector where ownership is not fully clear. Computed as the gap between the highest and second-highest
     * influence values among all tracked factions; a gap smaller than 20 points is considered contested.
     *
     * @return True, if it is a hotZone Planet.
     */
    public boolean isHotZone() {
        int maxflu = 0;
        int secondmaxflu = 0;
        for (int flu : influences.values()) {
            if (maxflu < flu) {
                secondmaxflu = maxflu;
                maxflu = flu;
            } else if (secondmaxflu < flu) {
                secondmaxflu = flu;
            }
        }
        return (maxflu - secondmaxflu) < 20;
    }

    /**
     * Writes every faction id / influence-amount pair to the binary stream. This is the mutable-state
     * counterpart used for incremental campaign-state transfer (see {@link MutableSerializable}).
     *
     * @see MutableSerializable#encodeMutableFields(BinWriter, CampaignData)
     */
    public void encodeMutableFields(BinWriter out, CampaignData dataProvider) {
        out.println(influences.size(), "influences.size");
        for (Integer i : influences.keySet()) {
            out.println(i, "id");
            out.println(influences.get(i), "amount");
        }
    }

    /**
     * Replaces this influence table by reading faction id / influence-amount pairs from the binary
     * stream, as written by {@link #encodeMutableFields}.
     *
     * @param in           the binary stream reader.
     * @param dataProvider unused directly; part of the {@link MutableSerializable} contract.
     * @throws IOException if the underlying stream read fails.
     */
    public void decodeMutableFields(BinReader in, CampaignData dataProvider) throws IOException {
        int s = in.readInt("influences.size");
        influences.clear();
        for (int i = 0; i < s; i++) {
            int factionID = in.readInt("id");
            int flu = in.readInt("amount");
            influences.put((factionID), (flu));
        }
    }

    /**
     * Outputs itself into an xml-Stream: one {@code <inf>} element per faction with its name and current
     * influence amount, wrapped in an {@code <influence>} element.
     *
     * @param out the print writer to write XML to.
     */
    public void xmlOut(PrintWriter out) {
        Iterator<House> inf = getHouses().iterator();
        out.println("\t<influence>");
        while (inf.hasNext()) {
            House h = inf.next();
            out.println("\t\t<inf>");
            out.println("\t\t<faction>" + h.getName() + "</faction>");
            out.println("\t\t<amount>" + getInfluence(h.getId()) + "</amount>");
            out.println("\t\t</inf>");
        }
        out.println("\t</influence>");
    }

    /**
     * Calculates the per-faction difference between this influence table and {@code infNew}: for every
     * faction present in either table, computes {@code this.influence - infNew.influence}. Factions whose
     * difference is 0 are omitted; factions present only in {@code infNew} get a negative entry (their
     * full influence there, negated, since this table has none of them).
     *
     * @param infNew the influence table to compare against (typically a "new" snapshot vs. this "old" one).
     * @return a new {@link Influences} instance holding the (possibly negative) per-faction differences.
     */
    public Influences difference(Influences infNew) {
        HashMap<Integer, Integer> diff = new HashMap<>();
        Collection<House> other = infNew.getHouses();
        Collection<House> thisHouse = getHouses();
        for (House house : thisHouse) {
            int d = getInfluence(house.getId()) - infNew.getInfluence(house.getId());
            if (d != 0) {
                diff.put(house.getId(), d);
            }
        }
        for (House house : other) {
            if (!thisHouse.contains(house)) {
                diff.put(house.getId(), (-infNew.getInfluence(house.getId())));
            }
        }
        return new Influences(diff);
    }

    /**
     * Merges another influence table into this one.
     * <p>
     * <b>Quirk:</b> despite the name/original intent ("adds the parameter's influence to the own"), this
     * does not sum influence amounts. For every faction already present in this table, its value is
     * overwritten (not added to) with {@code infNew}'s value for that faction. Factions present only in
     * {@code infNew} are copied in as new entries. Finally, any faction (including ones untouched by
     * {@code infNew}) whose resulting influence is 0 is removed from the map. In effect, this behaves like
     * applying {@code infNew} as a set of overrides/updates on top of this table (commonly used with
     * {@code infNew} being a {@link #difference(Influences)} result), not an arithmetic addition.
     *
     * @param infNew the influence table whose per-faction values should be applied onto this one.
     */
    public void add(Influences infNew) {
        for (House house : getHouses()) {
            influences.put((house.getId()), (infNew.getInfluence(house.getId())));
        }
        for (House house : infNew.getHouses()) {
            if (!getHouses().contains(house)) {
                influences.put((house.getId()), (infNew.getInfluence(house.getId())));
            }
        }
        for (House house : getHouses()) {
            if (getInfluence(house.getId()) == 0) {
                influences.remove((house.getId()));
            }
        }
    }

    /**
     * Writes this influence table to a binary stream: the entry count followed by each faction id and its
     * influence amount, in ascending id order (sorted for deterministic output).
     *
     * @param out the binary stream writer.
     */
    public void binOut(BinWriter out) {
        ArrayList<Integer> influencesIntegers = new ArrayList<>(influences.keySet());
        influencesIntegers.sort(null);
        out.println(influencesIntegers.size(), "influence.size");

        for (Integer factionID : influencesIntegers) {
            out.println(factionID, "faction");
            out.println(getInfluence(factionID), "amount");
        }
    }

    /**
     * Reads this influence table from a binary stream written by {@link #binOut}.
     *
     * @param in       the binary stream reader.
     * @param factions unused directly; kept for API-signature compatibility (resolution is by id only).
     * @throws IOException if the underlying stream read fails.
     */
    public void binIn(BinReader in, Map<Integer, House> factions) throws IOException {
        readInfluences(in);
    }

    /**
     * Shared helper that replaces this table's contents by reading faction id / amount pairs from the
     * stream, used by both {@link #binIn(BinReader, Map)} and {@link #binIn(BinReader)}.
     *
     * @param in the binary stream reader.
     */
    private void readInfluences(BinReader in) {
        influences = new HashMap<>();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put(hid, flu);
        }
    }

    /**
     * Reads this influence table from a binary stream written by {@link #binOut} (overload without a
     * faction map, for callers that don't need to pass one).
     *
     * @param in the binary stream reader.
     * @throws IOException if the underlying stream read fails.
     */
    public void binIn(BinReader in) throws IOException {
        readInfluences(in);
    }

    /**
     * Removes a faction's influence entry entirely (e.g. when a house is deleted/merged from the
     * campaign).
     *
     * @param house the faction whose influence entry should be removed.
     */
    public void removeHouse(House house) {
        influences.remove(house.getId());
    }

    /**
     * Sets (overwrites) a faction's influence amount directly, bypassing the clamping/transfer rules of
     * {@link #moveInfluence}. Used e.g. by {@code Planet#updateInfluences()} to assign leftover conquest
     * points to the neutral bucket (house id -1).
     *
     * @param id     the faction id to update.
     * @param amount the influence amount to set.
     */
    public void updateHouse(int id, int amount) {
        influences.put(id, amount);
    }
}
