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

import java.util.Collection;


/**
 * This interface enables the class to provide information about terrains. It
 * is used, as example by PlanetEnvironments to retrieve the real terrains 
 * behind the terrain id.
 * @author Imi (immanuel.scholz@gmx.de)
 */
public interface TerrainProvider {
    /**
     * Return the terrain to a given id.
     */
    public Terrain getTerrain(int id);

    /**
     * Return all terrains
     */
    public Collection<Terrain> getAllTerrains();

    /**
     * Add a terrain to the list
     */
    public void addAdvancedTerrain(AdvancedTerrain terrain);
    
    /*add the advanced terrain provisions*/
    /**
     * Return the terrain to a given id.
     */
    public AdvancedTerrain getAdvancedTerrain(int id);

    /**
     * Return all terrains
     */
    public Collection<AdvancedTerrain> getAllAdvancedTerrains();

    
}
