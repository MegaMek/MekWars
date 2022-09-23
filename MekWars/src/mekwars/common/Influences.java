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

package common;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import common.util.BinReader;
import common.util.BinWriter;
import common.util.MWLogger;

/**
 * Represents the influences of different Houses of a planet. This may be used
 * as total influences as well as influence differences between two total
 * influences.
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class Influences implements MutableSerializable {

    /**
     * A hash table with key=House and value=Integer of the influences of the
     * different factions. Only factions greater than 0% are listed.
     */
    private HashMap<Integer, Integer> influences = new HashMap<Integer, Integer>();

    /**
     * Creates a new Influence with a preset table.
     * 
     * @param influences
     */
    public Influences(HashMap<Integer, Integer> influences) {
        setInfluence(influences);
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
        setInfluence(new HashMap<Integer, Integer>(influences.influences));
    }

    /**
     * Return the influence of a specific faction.
     */
    public int getInfluence(int factionID) {

        if (!influences.containsKey(factionID))
            return 0;
        int i = influences.get(factionID);
        return i;
    }

    /**
     * Return the faction with the most influence.
     */
    public Integer getOwner() {

        try {
            TreeSet<House> sset = new TreeSet<House>(new Comparator<Object>() {
                public int compare(Object o1, Object o2) {
                    try {
                        int i1 = -1;
                        int i2 = -1;
                        
                        if ( o1 != null )
                            i1 = ((House) o1).getId();
                        
                        if ( o2 != null )
                            i2 = ((House) o2).getId();
                        
                        return (i1 < i2) ? -1 : (i1 == i2 ? 0 : 1);
                    } catch (Exception ex) {
                        return 0;
                    }
                }
            });
            sset.addAll(this.getHouses());
            House[] factions = new House[sset.size()];

            int i = 0;
            for (Iterator<House> it = sset.iterator(); it.hasNext();)
                factions[i++] = it.next();
            Arrays.sort(factions, new Comparator<Object>() {
                public int compare(Object o1, Object o2) {
                    int h1Id = -1;
                    int h2Id = -2;
                    
                    if ( o1 != null )
                        h1Id = ((House) o1).getId();
                    
                    if ( o2 != null )
                        h2Id = ((House) o2).getId();
                    
                    int i1 = getInfluence(h1Id);
                    int i2 = getInfluence(h2Id);
                    return (i1 > i2) ? -1 : (i1 == i2 ? 0 : 1);
                }
            });

            if ( factions.length < 1)
                return null;
            
            House faction = factions[0];
            if (faction == null)
                return null;

            // only one owner don't need to see whoes the boss.
            if (factions.length <= 1)
                return faction.getId();

            House faction2 = factions[1];

            if (faction2 != null && getInfluence((faction2.getId())) == getInfluence((faction.getId())))
                return null;

            return faction.getId();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Error in Influenes.getOwner()");
            return null;
        }

    }

    /**
     * Fairly distribute the influence under the factions in the list.
     * 
     * @param factions
     *            All of these factions gain as much as possible influece
     *            divided equal
     * @param gainer
     *            If there is a portion left, one faction get it all. This
     *            faction.
     */
    public void setNeutral(List<House> factions, House gainer, int maxInfluence) {
        influences = new HashMap<Integer, Integer>();
        for (int i = 0; i < factions.size(); i++) {
            House h = factions.get(i);
            influences.put((h.getId()), (maxInfluence / factions.size()));
        }
        if (maxInfluence % factions.size() != 0) {
            int bonus = maxInfluence % factions.size();
            if (influences.containsKey((gainer.getId())))
                influences.put((gainer.getId()), (((Integer) influences.get(gainer)).intValue() + bonus));
            else
                influences.put((gainer.getId()), (bonus));
        }
    }

    /**
     * Returns the present factions.
     */
    public Set<House> getHouses() {
        Set<House> result = new HashSet<House>();
        Iterator<Integer> it = influences.keySet().iterator();
        while (it.hasNext()) {
            House faction = CampaignData.cd.getHouse(it.next());
            result.add(faction);
        }
        return result;
    }

    /**
     * Returns the number of factions with ownership on world.
     */
    public int houseCount() {
        return influences.size();
    }

    /**
     * Move influence from one faction to a new faction. Note, that this make
     * sure, that nobody can have more influence than 100% and nobody may drop
     * below 0. If you not want to respect to this, use add() instead.
     */
    public int moveInfluence(House winner, House loser, int amount, int maxInfluence) {
        if (amount == 0)
            return 0;

        int winnerId = winner.getId();
        int loserId = -1;

        int oldwinnerinfluence = 0;
        int oldloserinfluence = 0;

        oldwinnerinfluence = getInfluence(winnerId);
        oldloserinfluence = getInfluence(loserId);

        if (oldwinnerinfluence + amount >= maxInfluence)
            amount = maxInfluence - oldwinnerinfluence;

        if (amount > oldloserinfluence) {
            influences.remove(loserId);
            loserId = loser.getId();
            influences.put(loserId, getInfluence(loserId)+oldloserinfluence);
            oldloserinfluence = getInfluence(loserId);
        }

        if (oldloserinfluence < amount)
            amount = oldloserinfluence;

        int winnerInfluence = oldwinnerinfluence + amount;
        int loserInfluence = oldloserinfluence - amount;

        if (winnerInfluence == 0)
            influences.remove(winnerId);
        else
            influences.put(winnerId, (winnerInfluence));

        if (loserInfluence == 0)
            influences.remove(loserId);
        else
            influences.put(loserId, (loserInfluence));
        return amount;
    }

    /**
     * Sets the whole influences.
     * 
     * @param influences
     *            The new influences. Key=TimeUpdateHouse, Value=Integer.
     */
    public void setInfluence(HashMap<Integer, Integer> influences) {
        this.influences = influences;
    }

    /**
     * Returns whether the Influence zone belongs to a so called "hot zone",
     * which means, that it is in a critical sector where ownership is not fully
     * clear.
     * 
     * @return True, if it is a hotZone Planet.
     */
    public boolean isHotZone() {
        int maxflu = 0;
        int secondmaxflu = 0;
        Iterator<Integer> e = influences.values().iterator();
        while (e.hasNext()) {
            int flu = e.next();
            if (maxflu < flu) {
                secondmaxflu = maxflu;
                maxflu = flu;
            } else if (secondmaxflu < flu)
                secondmaxflu = flu;
        }
        return (maxflu - secondmaxflu) < 20;
    }

    /**
     * @see common.MutableSerializable#encodeMutableFields(java.io.OutputStream)
     */
    public void encodeMutableFields(BinWriter out, CampaignData dataProvider) throws IOException {
        out.println(influences.size(), "influences.size");
        for (Iterator<Integer> it = influences.keySet().iterator(); it.hasNext();) {
            Integer i = it.next();
            // House h = (House)it.next();

            out.println(i.intValue(), "id");
            out.println(((Integer) influences.get(i)).intValue(), "amount");
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
        HashMap<Integer, Integer> diff = new HashMap<Integer, Integer>();
        Collection<House> other = infNew.getHouses();
        Collection<House> thisone = getHouses();
        for (Iterator<House> it = thisone.iterator(); it.hasNext();) {
            House h = it.next();
            int d = getInfluence(h.getId()) - infNew.getInfluence(h.getId());
            if (d != 0)
                diff.put(h.getId(), d);
        }
        for (Iterator<House> it = other.iterator(); it.hasNext();) {
            House h = it.next();
            if (!thisone.contains(h.getId()))
                diff.put(h.getId(), (-infNew.getInfluence(h.getId())));
        }
        return new Influences(diff);
    }

    /**
     * Adds the parameter's influence to the own.
     */
    public void add(Influences infNew) {
        for (Iterator<House> it = getHouses().iterator(); it.hasNext();) {
            House h = it.next();
            influences.put((h.getId()), (infNew.getInfluence(h.getId())));
        }
        for (Iterator<House> it = infNew.getHouses().iterator(); it.hasNext();) {
            House h = it.next();
            if (!getHouses().contains(h))
                influences.put((h.getId()), (infNew.getInfluence(h.getId())));
        }
        for (Iterator<House> it = getHouses().iterator(); it.hasNext();) {
            House h = it.next();
            if (getInfluence(h.getId()) == 0)
                influences.remove((h.getId()));
        }
    }

    /**
     * Write itself into the stream.
     */
    public void binOut(BinWriter out) throws IOException {
        Object h[] = influences.keySet().toArray();
        Arrays.sort(h, new Comparator<Object>() {
            public int compare(Object o1, Object o2) {
                int i1 = ((Integer) o1).intValue();
                int i2 = ((Integer) o2).intValue();
                return i1 == i2 ? 0 : (i1 < i2 ? -1 : 1);
            }
        });
        out.println(h.length, "influence.size");
        for (int i = 0; i < h.length; i++) {
            out.println(((Integer) h[i]).intValue(), "faction");
            out.println(getInfluence(((Integer) h[i]).intValue()), "amount");
        }
    }

    /**
     * Read from a binary stream
     */
    public void binIn(BinReader in, Map<Integer, House> factions) throws IOException {
        influences = new HashMap<Integer, Integer>();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put(hid, flu);
        }
    }

    public void binIn(BinReader in) throws IOException {
        influences = new HashMap<Integer, Integer>();
        int size = in.readInt("influence.size");
        for (int i = 0; i < size; i++) {
            int hid = in.readInt("faction");
            int flu = in.readInt("amount");
            influences.put(hid, flu);
        }
    }

    /**
     * @see common.persistence.MMNetSerializable#binOut(common.persistence.TreeWriter)
     * 
     * public void binOut(TreeWriter out) { Object h[] =
     * influences.keySet().toArray(); Arrays.sort(h, new Comparator(){ public
     * int compare(Object o1, Object o2) { int i1 = ((Integer)o1).intValue();
     * int i2 = ((Integer)o2).intValue(); return i1 == i2 ? 0 : (i1<i2?-1:1);
     * }}); out.write(h.length, "size"); for (int i = 0; i < h.length; i++) {
     * out.write(((Integer)h[i]).intValue(), "faction");
     * out.write(getInfluence(((Integer)h[i]).intValue()), "amount"); } }
     * 
     * /**
     * @see common.persistence.MMNetSerializable#binIn(common.persistence.TreeReader)
     * 
     * public void binIn(TreeReader in, CampaignData dataProvider) throws
     * IOException { influences = new HashMap(); int size = in.readInt("size");
     * for (int i = 0; i < size; i++) { int hid = in.readInt("faction"); int flu =
     * in.readInt("amount"); influences.put((hid),(flu)); } }
     */
    public void removeHouse(House house) {
        influences.remove(house.getId());
    }

    public void updateHouse(int id, int amount) {
        influences.put(id, amount);
    }
}
