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

/*
 * Created on 04.05.2004
 *
 */
package mekwars.common;

/**
 * Represents one landmass/region on a {@link Planet}, described by a base {@link Terrain} type, a set of
 * climate/atmosphere details ({@link AdvancedTerrain}), and a relative "size" weight.
 * <p>
 * A planet does not have a single terrain; instead {@link PlanetEnvironments} holds a list of Continent
 * instances, each contributing a proportion (its {@link #size}) of the planet's surface. This weighting is
 * used both for percentage display (see {@code Planet#getLongDescription}) and for weighted-random terrain
 * selection when a scenario needs to pick a battlefield (see
 * {@link PlanetEnvironments#getRandomEnvironment(java.util.Random)}).
 *
 * @author Helge Richter
 */
public class Continent {
    /** Base terrain type (e.g. forest, desert, mountains) of this continent. */
    private Terrain environment;

    /** Climate/atmosphere/gravity details layered on top of {@link #environment}. */
    private AdvancedTerrain advTerrain;

    /**
     * Relative weight of this continent, NOT a physical size. It is used as the numerator/denominator in
     * weighted-random draws and percentage-of-surface calculations across all continents of a planet (see
     * {@link PlanetEnvironments#getTotalEnvironmentProbabilities()}). Defaults to 1.
     */
    private int size = 1;

    /** Unique identifier of this continent; -1 means "unassigned". */
    private int id = -1;

	/*
	 * Legacy/dead constructor, retained (commented out) for historical reference. It predates the
	 * AdvancedTerrain parameter now required by the constructor below and is no longer compiled.
	 */
	/*public Continent(int Size, Terrain env) {
		this.size = Size;
		environment = env;
		advTerrain = new AdvancedTerrain();
		advTerrain.setName("none");
		advTerrain.setId(0);
	}
	*/

    /**
     * Creates a continent with an explicit weight, base terrain, and advanced-terrain climate data.
     *
     * @param Size    the relative weight/probability of this continent (see {@link #size}).
     * @param env     the base terrain type.
     * @param advTerr the climate/atmosphere details.
     */
    public Continent(int Size, Terrain env, AdvancedTerrain advTerr) {
        this.size = Size;
        environment = env;
        advTerrain = advTerr;
    }


    /**
     * No-arg constructor required for serialization frameworks (e.g. XStream); fields are populated
     * afterwards via reflection or the binary {@code binIn}/{@code binOut} readers/writers.
     */
    public Continent() {
        // for serialization
    }

    /**
     * Compares two continents for equality.
     * <p>
     * <b>Quirk/bug:</b> the logic here is inverted from what "equals" normally means. It returns
     * {@code false} when the two continents' {@link Terrain} environments <em>are</em> equal (via the
     * {@code if (... .equals(...)) return false;} check), and its final line returns the negation of the
     * {@link AdvancedTerrain} comparison — i.e. it returns {@code true} when the advanced terrains differ.
     * As written, this method does not implement a correct equivalence relation. Documented as-is;
     * behavior is not changed here.
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Continent cont)) {
            return false;
        }

        if (cont.getSize() != getSize()) {
            return false;
        }

        if (cont.getEnvironment().equals(getEnvironment())) {
            return false;
        }

        return !cont.getAdvancedTerrain().equals(getAdvancedTerrain());
    }

    /**
     * @return the relative weight/probability of this continent (not a physical size).
     */
    public int getSize() {
        return size;
    }

    /**
     * @param size the relative weight/probability to set.
     */
    public void setSize(int size) {
        this.size = size;
    }

    /**
     * @return the base {@link Terrain} type of this continent.
     */
    public Terrain getEnvironment() {
        return environment;
    }

    /**
     * @return the {@link AdvancedTerrain} (climate/atmosphere/gravity) data of this continent.
     */
    public AdvancedTerrain getAdvancedTerrain() {
        return advTerrain;

    }

    /**
     * @return the unique identifier of this continent, or -1 if unassigned.
     */
    public int getID() {
        return id;
    }

    /**
     * Sets the continent ID.
     *
     * @param id the new identifier.
     */
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Builds a display label combining terrain name, advanced terrain name, and weight percentage;
     * used for UI drop-down/selection boxes when choosing a continent/battlefield.
     *
     * @return a string of the form {@code "<terrainName>(<advTerrainName>) %<size>"}.
     */
    public String getDropBoxName() {
        return getEnvironment().getName() + "(" + getAdvancedTerrain().getName() + ") %" + getSize();
    }
}
