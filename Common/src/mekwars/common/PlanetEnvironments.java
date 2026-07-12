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
 * Represents a collection of {@link Continent}s, usually all belonging to one {@link Planet}. This is the
 * planet's overall terrain makeup: each planet has one {@code PlanetEnvironments} (see
 * {@code Planet#getEnvironments()}), and each {@link Continent} in it carries a base {@link Terrain} type, an
 * {@link AdvancedTerrain} (climate) description, and a relative weight used to pick which landmass a scenario
 * takes place on.
 * <p>
 * NOTE: despite the similar name, this class is unrelated to {@code PlanetEnvironment.java} (singular),
 * which describes a single environment/terrain sub-type used by {@link Terrain}. Do not confuse the two.
 *
 * @author Imi (immanuel.scholz@gmx.de) seen, modified and made totally bad by McWizard
 *       <p>
 *       Imi: *crhm*..."totally bad"... ;-)
 *                                                                                                                               TODO: simplify this class. subclass it from ArrayList or something like that
 */

public class PlanetEnvironments {

    /**
     * A terrain provider to get terrain information from.
     */
    public static TerrainProvider data;

    /**
     * The list of all continents belonging to a planet's environment set.
     */
    private final ArrayList<Continent> continents = new ArrayList<>();

    /**
     * @return an iterator over all continents in this set.
     */
    public Iterator<Continent> iterator() {
        return continents.iterator();
    }

    /**
     * @return the number of continents in this set.
     */
    public int size() {
        return continents.size();
    }

    /**
     * Returns all continents as an array. This is a shallow copy of the internal list (a new array
     * referencing the same {@link Continent} objects), so structural changes to the returned array
     * (adding/removing elements) have no effect on this set; mutating a returned Continent, however, does
     * affect the shared object.
     *
     * @return a new array containing all continents currently in this set.
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
     * Removes the first continent whose base {@link Terrain} name matches {@code terrain}.
     * <p>
     * If multiple continents share the same terrain name, only the first (lowest-index) match is removed.
     * If no continent matches, the loop runs to completion without a match and nothing is removed.
     *
     * @param terrain the terrain name to search for and remove.
     */
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

    /**
     * Removes all continents from this set, leaving the planet with no terrain data.
     */
    synchronized public void removeAll() {
        continents.clear();
    }

    /**
     * Finds the continent with the largest relative {@link Continent#getSize() size} weight, i.e. the
     * dominant landmass/terrain of the planet.
     *
     * @return the continent with the highest size weight, or a placeholder zero-size Continent if this
     *         set is empty.
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
     * Picks a continent at random, weighted by each continent's relative {@link Continent#getSize() size}
     * (a Knuth-style weighted "skewer draw"), and then further refines the pick against the
     * sub-environment probabilities of the chosen continent's {@link Terrain}. Used to select which
     * landmass/terrain a scenario is fought on for this planet.
     *
     * @param r the random source to draw from.
     * @return the randomly selected continent, or a degenerate {@code Continent(0, null, null)} if this
     *         set is empty or the draw falls through without a match.
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
     * Sums the {@link Continent#getSize() size} weights of every continent in this set. This total is the
     * denominator used both for weighted-random draws ({@link #getRandomEnvironment(Random)}) and for
     * computing each continent's percentage-of-surface for display purposes.
     *
     * @return the sum of all continents' size weights.
     */
    public int getTotalEnvironmentProbabilities() {
        int result = 0;

        for (Continent continent : continents) {
            result += continent.getSize();
        }

        return result;
    }

    /**
     * Serializes this environment set to a binary stream: the continent count followed by, for each
     * continent, its size weight, base {@link Terrain} id, and {@link AdvancedTerrain} id. Terrain objects
     * themselves are written elsewhere and looked up by id on read (see {@link #binIn}).
     *
     * @param out the binary stream writer to serialize to.
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
     * Reconstructs this environment set from a binary stream written by {@link #binOut}, resolving each
     * stored terrain/advanced-terrain id back into shared {@link Terrain}/{@link AdvancedTerrain} objects
     * via the supplied {@link CampaignData}, and appending a new {@link Continent} for each entry.
     *
     * @param in   the binary stream reader to read from.
     * @param data the campaign data used to resolve terrain and advanced-terrain ids.
     * @throws IOException if the underlying stream read fails.
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
     * Adds a continent to the current set. This will vanish, when Terrains are initialized through XStream.
     * <p>
     * TODO You should not need this and you should only initialize the terrain set with either XStream or
     *       binIn()
     *
     * @param newPE the continent to append to this set.
     */
    synchronized public void add(Continent newPE) {
        continents.add(newPE);
    }

    /*
     * The block below is legacy/dead code from an older tree-based (TreeWriter/TreeReader) serialization
     * mechanism, superseded by the BinWriter/BinReader-based binOut/binIn methods above. Retained
     * (commented out) for historical reference only; it does not compile as-is and is not used.
     */
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
