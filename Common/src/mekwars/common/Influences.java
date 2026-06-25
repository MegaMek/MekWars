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
 * Represents the influences of different Houses of a planet. This may be used as total influences as well as influence
 * differences between two total influences.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class Influences implements MutableSerializable {
    private final static MMLogger LOGGER = MMLogger.create(Influences.class);
    /**
     * A hash table with key=House and value=Integer of the influences of the different factions. Only factions greater
     * than 0% are listed.
     */
    private HashMap<Integer, Integer> influences = new HashMap<>();

    /**
     * Creates a new Influence with a preset table.
     *
     */
    public Influences(HashMap<Integer, Integer> influences) {
        setInfluence(influences);
    }

    /**
     * Sets the whole influences.
     *
     * @param influences The new influences. Key=TimeUpdateHouse, Value=Integer.
     */
    public void setInfluence(HashMap<Integer, Integer> influences) {
        this.influences = influences;
    }

    /**
     * Create an empty Influence.
     */
    public Influences() {
    }

    /**
     * Copies the Influence
     */
    public Influences(Influences influences) {
        setInfluence(new HashMap<>(influences.influences));
    }

    /**
     * Return the faction with the most influence.
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
     * Returns the present factions.
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
     * Return the influence of a specific faction.
     */
    public int getInfluence(int factionID) {
        if (!influences.containsKey(factionID)) {
            return 0;
        }

        return influences.get(factionID);
    }

    /**
     * Fairly distribute the influence under the factions in the list.
     *
     * @param factions All of these factions gain as much as possible influence divided equal
     * @param gainer   If there is a portion left, one faction get it all. This faction.
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
     * Returns the number of factions with ownership on world.
     */
    public int houseCount() {
        return influences.size();
    }

    /**
     * Move influence from one faction to a new faction. Note, that this make sure, that nobody can have more influence
     * than 100% and nobody may drop below 0. If you not want to respect to this, use add() instead.
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
     * sector where ownership is not fully clear.
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
     * @see MutableSerializable#encodeMutableFields(BinWriter, CampaignData)
     */
    public void encodeMutableFields(BinWriter out, CampaignData dataProvider) {
        out.println(influences.size(), "influences.size");
        for (Integer i : influences.keySet()) {
            out.println(i, "id");
            out.println(influences.get(i), "amount");
        }
    }

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
     * Outputs itself into an xml-Stream.
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
     * Calculates the difference between this and the parameter.
     *
     * @return The influence difference.
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
     * Adds the parameter's influence to the own.
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
     * Write itself into the stream.
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
     * Read from a binary stream
     */
    public void binIn(BinReader in, Map<Integer, House> factions) throws IOException {
        readInfluences(in);
    }

    private void readInfluences(BinReader in) {
        influences = new HashMap<>();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put(hid, flu);
        }
    }

    public void binIn(BinReader in) throws IOException {
        readInfluences(in);
    }

    public void removeHouse(House house) {
        influences.remove(house.getId());
    }

    public void updateHouse(int id, int amount) {
        influences.put(id, amount);
    }
}
