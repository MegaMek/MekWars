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

import java.util.Collection;


/**
 * This interface enables the class to provide information about terrains. It is used, as example by PlanetEnvironments
 * to retrieve the real terrains behind the terrain id.
 * <p>
 * In the MekWars data model, a {@link Terrain} is a named terrain "theme" (e.g. a season or biome) that bundles one or
 * more {@link PlanetEnvironment} map-generation rulesets, while an {@link AdvancedTerrain} describes the planetary
 * conditions (weather, gravity, atmosphere, day/night cycle) that go with a location. Both are normally persisted with
 * only a numeric id, so anything that needs the full object (e.g. a {@code Planet} or {@code Continent}) implements
 * this interface to resolve that id back into the real {@code Terrain}/{@code AdvancedTerrain} instance when a battle
 * is being set up.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public interface TerrainProvider {
    /**
     * Resolves a stored terrain id back into its {@link Terrain} instance.
     *
     * @param id the terrain id to look up
     * @return the matching {@link Terrain}, or an implementation-defined value (e.g. {@code null}) if the id is unknown
     */
    Terrain getTerrain(int id);

    /**
     * @return all {@link Terrain} instances known to this provider
     */
    Collection<Terrain> getAllTerrains();

    /**
     * Registers an {@link AdvancedTerrain} (planetary-conditions profile) with this provider so it can later be
     * looked up by id via {@link #getAdvancedTerrain(int)}.
     *
     * @param terrain the advanced terrain profile to add
     */
    void addAdvancedTerrain(AdvancedTerrain terrain);

    /*add the advanced terrain provisions*/

    /**
     * Resolves a stored terrain id back into its {@link AdvancedTerrain} (planetary conditions) instance.
     *
     * @param id the advanced terrain id to look up
     * @return the matching {@link AdvancedTerrain}, or an implementation-defined value if the id is unknown
     */
    AdvancedTerrain getAdvancedTerrain(int id);

    /**
     * @return all {@link AdvancedTerrain} instances known to this provider
     */
    Collection<AdvancedTerrain> getAllAdvancedTerrains();


}
