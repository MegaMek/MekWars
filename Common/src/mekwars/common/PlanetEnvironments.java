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

package mekwars.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * Represents a collection of continents, usually for one planet
 *
 * @author Imi (immanuel.scholz@gmx.de) seen, modified and made totally bad by McWizard
 *       <p>
 *       Imi: *crhm*..."totally bad"... ;-)
 *                                                                                                                         TODO: simplify this class. subclass it from ArrayList or something like that
 */

public class PlanetEnvironments {

    /**
     * A terrain provider to get terrain information from.
     */
    public static TerrainProvider data;

    /**
     * The list of all continents. Type=Continent
     */
    private final ArrayList<Continent> continents = new ArrayList<>();

    /**
     * Iterate over all terrains in this set.
     */
    public Iterator<Continent> iterator() {
        return continents.iterator();
    }

    /**
     * Returns the number of terrains in this set.
     */
    public int size() {
        return continents.size();
    }

    /**
     * Return all Environments as an array. You get a copy of the actual data, so modifying is pointless!
     */
    public Continent[] toArray() {

        int size = continents.size();
        Continent[] Counts = new Continent[size];
        for (int x = 0; x < size; x++) {
            Counts[x] = continents.get(x);
        }
        return Counts;
    }

    /**
     * Add a terrain to the current set. This will vanish, when Terrains are initialized through XStream.
     * <p>
     * TODO You should not need this and you should only initialize the terrain set with either XStream or
     *       binIn()
     */
    synchronized public void add(Continent newPE) {
        continents.add(newPE);
    }

    synchronized public void remove(String terrain) {

        int count = 0;
        for (Continent land : continents) {

            //Check for multiple terrains with the same name.
            if (land.getEnvironment().getName().equals(terrain)) {
                break;
            }
            count++;
        }

        if (count < continents.size()) {
            continents.remove(count);
            continents.trimToSize();
        }
    }

    synchronized public void removeAll() {
        continents.clear();
    }

    /**
     * Return the environment with the most probability to occur.
     */
    public Continent getBiggestEnvironment() {
        Continent result = new Continent(0, new Terrain(), new AdvancedTerrain());
        for (Continent continent : continents) {
            if (continent.getSize() > result.getSize()) {
                result = continent;
            }
        }
        return result;
    }

    /**
     * Return the total probability of all environments.
     */
    public int getTotalEnvironmentProbabilities() {
        int result = 0;

        for (Continent continent : continents) {
            result += continent.getSize();
        }

        return result;
    }

    /**
     * Returns a randomEnvironment based on the probability of each Environment.
     */
    public Continent getRandomEnvironment(Random r) {
        // use the skewer draw algorithm from Knuth.
        int probabilities = getTotalEnvironmentProbabilities();
        for (Continent continent : continents) {
            if (r.nextInt(probabilities) < continent.getSize()) {

                probabilities = continent.getEnvironment().getTotalEnvironmentProbabilities();
                for (PlanetEnvironment planetEnvironment : continent.getEnvironment().getEnvironments()) {

                    if (r.nextInt(probabilities) < planetEnvironment.getEnvironmentalProb()) {
                        return continent;
                    }

                    probabilities -= planetEnvironment.getEnvironmentalProb();
                }
            }

            probabilities -= continent.getSize();
        }

        return new Continent(0, null, null);
    }

    /**
     * Writes as binary stream
     */
    public void binOut(BinWriter out) {
        out.println(continents.size(), "terrain.size");
        for (Continent continent : continents) {
            out.println(continent.getSize(), "size");
            out.println(continent.getEnvironment().getId(), "id");
            out.println(continent.getAdvancedTerrain().getId(), "aid");
        }
    }

    /**
     * Read from a binary stream
     */
    public void binIn(BinReader in, CampaignData data) throws IOException {
        int size = in.readInt("terrain.size");
        for (int i = 0; i < size; ++i) {
            int percent = in.readInt("size");
            int id = in.readInt("id");
            int aid = in.readInt("aid");
            Terrain T = data.getTerrain(id);
            AdvancedTerrain AT = data.getAdvancedTerrain(aid);
            Continent continent = new Continent(percent, T, AT);
            add(continent);
        }
    }

    /**
     * @see common.persistence.MMNetSerializable#binOut(common.persistence.TreeWriter)
     *
    public void binOut(TreeWriter out) {
    out.write(size(), "terrain.size");
    for (Iterator it = continents.iterator(); it.hasNext();) {
    Continent cont = (Continent)it.next();
    out.write(cont.getSize(),"size");
    out.write(cont.getEnvironment().getId(),"id");
    }
    }

    /**
     * @see common.persistence.MMNetSerializable#binIn(common.persistence.TreeReader, common.CampaignData)
     *
    public void binIn(TreeReader in, CampaignData dataProvider) throws IOException {
    int size = in.readInt("terrain.size");
    for (int i = 0; i < size; ++i)
    add(new Continent(in.readInt("size"),dataProvider.getTerrain(in.readInt("id"))));
    }*/
}
