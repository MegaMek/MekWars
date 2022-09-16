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
 * Created on 07.04.2004
 *
 */
package server.campaign.util;

import java.util.Comparator;

import server.campaign.SPlanet;

/**
 * @author Helge Richter
 * 
 */
public class PlanetNameComparator implements Comparator<SPlanet> {
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(SPlanet p1, SPlanet p2) {
        return p1.getName().compareTo(p2.getName());
    }
}
