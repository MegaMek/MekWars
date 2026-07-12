/*
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

package mekwars.common.campaign;

import java.util.StringTokenizer;

import megamek.common.enums.BuildingType;

/**
 * Describes the building-generation parameters used to auto-populate structures (buildings/city blocks) on a
 * planetary battlefield map for a MekWars operation. This is a small, self-contained value/template object: it does
 * not hold live map state, just the ruleset (min/max floor counts, construction factor range, building type, city
 * type/size, and preferred deployment edge) that the map generator uses when building or describing a scenario.
 * <p>
 * Instances are typically built up field-by-field via the setters (e.g. when parsed out of planet/scenario
 * definition data) and can be round-tripped to/from a pipe-delimited string via {@link #toString()} and
 * {@link #fromString(StringTokenizer)}. Note that {@link #toString()} and {@link #fromString(StringTokenizer)} are
 * NOT full inverses of each other: {@code cityType} and {@code cityBlocks} are settable/gettable but are not
 * included in either the serialized {@link #toString()} output or the {@link #fromString(StringTokenizer)} parser.
 */
public final class Buildings {

    /** Sentinel value meaning no starting/deployment edge has been assigned yet. */
    public static final int EDGE_UNKNOWN = -1;
    //Have to follow MM starting position protocols. Left out NW, NE, SW, SE for easier computations.
    /** Compass-direction constants matching MegaMek's board starting-position protocol (shallow edge). */
    public static final int NORTHWEST = 1;
    public static final int NORTH = 2;
    public static final int NORTHEAST = 3;
    public static final int EAST = 4;
    public static final int SOUTHEAST = 5;
    public static final int SOUTH = 6;
    public static final int SOUTHWEST = 7;
    public static final int WEST = 8;

    /** Any board edge, without regard to specific compass direction. */
    public static final int EDGE = 9;
    /** Deployment in the center of the board, rather than along an edge. */
    public static final int CENTER = 10;

    /** Compass-direction constants for "deep" deployment (further from the board edge than the shallow variants above). */
    public static final int NORTHWEST_DEEP = 11;
    public static final int NORTH_DEEP = 12;
    public static final int NORTHEAST_DEEP = 13;
    public static final int EAST_DEEP = 14;
    public static final int SOUTHEAST_DEEP = 15;
    public static final int SOUTH_DEEP = 16;
    public static final int SOUTHWEST_DEEP = 17;
    public static final int WEST_DEEP = 18;

    /** Total number of buildings to place on the generated map. */
    private int totalBuildings = 0;
    /** Minimum number of buildings that must be present. */
    private int minBuildings = 0;
    /** Minimum number of floors (above-ground levels) any generated building may have. */
    private int minFloors = 0;
    /** Maximum number of floors any generated building may have. */
    private int maxFloors = 0;
    /** Minimum Construction Factor (CF, structural toughness) for generated buildings. */
    private int minCF = 0;
    /** Maximum Construction Factor (CF) for generated buildings. */
    private int maxCF = 0;
    /** Preferred/assigned deployment edge; one of the direction constants above, or {@link #EDGE_UNKNOWN}. */
    private int startingEdge = EDGE_UNKNOWN;
    /** MegaMek building material/type (e.g. wood, light, medium, heavy, hardened). */
    private BuildingType buildingType = BuildingType.UNKNOWN;
    /** Descriptive city/settlement type label (e.g. "NONE", or a city-size category); not serialized by toString/fromString. */
    private String cityType = "NONE";
    /** Number of city blocks associated with this building template; not serialized by toString/fromString. */
    private int cityBlocks = 0;

    /** Creates a Buildings template with all fields at their defaults (no buildings, unknown edge). */
    public Buildings() {
        super();
    }

    /** @return the descriptive city/settlement type label. */
    public String getCityType() {
        return cityType;
    }

    /** Sets the descriptive city/settlement type label. */
    public void setCityType(String type) {
        cityType = type;
    }

    /** @return the number of city blocks associated with this template. */
    public int getCityBlocks() {
        return cityBlocks;
    }

    /** Sets the number of city blocks associated with this template. */
    public void setCityBlocks(int blocks) {
        cityBlocks = blocks;
    }

    /**
     * Serializes the core generation parameters (excluding {@code cityType}/{@code cityBlocks}) as a
     * pipe-delimited string, in the same field order expected by {@link #fromString(StringTokenizer)}. Ends with a
     * trailing "|".
     */
    @Override
    public String toString() {
        String result = "";

        result += getTotalBuildings();
        result += "|";
        result += getMinBuildings();
        result += "|";
        result += getMinFloors();
        result += "|";
        result += getMaxFloors();
        result += "|";
        result += getMinCF();
        result += "|";
        result += getMaxCF();
        result += "|";
        result += getBuildingType();
        result += "|";
        result += getStartingEdge();
        result += "|";

        return result;
    }

    /** @return the total number of buildings to place on the generated map. */
    public int getTotalBuildings() {
        return totalBuildings;
    }

    /** Sets the total number of buildings to place on the generated map. */
    public void setTotalBuildings(int total) {
        totalBuildings = total;
    }

    /** @return the minimum number of buildings that must be present. */
    public int getMinBuildings() {
        return minBuildings;
    }

    /** Sets the minimum number of buildings that must be present. */
    public void setMinBuildings(int min) {
        minBuildings = min;
    }

    /** @return the minimum number of floors any generated building may have. */
    public int getMinFloors() {
        return minFloors;
    }

    /** Sets the minimum number of floors any generated building may have. */
    public void setMinFloors(int min) {
        minFloors = min;
    }

    /** @return the maximum number of floors any generated building may have. */
    public int getMaxFloors() {
        return maxFloors;
    }

    /** Sets the maximum number of floors any generated building may have. */
    public void setMaxFloors(int max) {
        maxFloors = max;
    }

    /** @return the minimum Construction Factor (CF) for generated buildings. */
    public int getMinCF() {
        return minCF;
    }

    /** Sets the minimum Construction Factor (CF) for generated buildings. */
    public void setMinCF(int cf) {
        minCF = cf;
    }

    /** @return the maximum Construction Factor (CF) for generated buildings. */
    public int getMaxCF() {
        return maxCF;
    }

    /** Sets the maximum Construction Factor (CF) for generated buildings. */
    public void setMaxCF(int cf) {
        maxCF = cf;
    }

    /** @return the MegaMek building material/type used for generated buildings. */
    public BuildingType getBuildingType() {
        return buildingType;
    }

    /** Sets the MegaMek building material/type used for generated buildings. */
    public void setBuildingType(BuildingType type) {
        buildingType = type;
    }

    /** @return the assigned deployment edge; one of the direction constants, {@link #EDGE}, {@link #CENTER}, or {@link #EDGE_UNKNOWN}. */
    public int getStartingEdge() {
        return startingEdge;
    }

    /** Sets the assigned deployment edge. */
    public void setStartingEdge(int edge) {
        startingEdge = edge;
    }

    /**
     * Populates this template's core fields (all except {@code cityType}/{@code cityBlocks}) by consuming tokens
     * from the given tokenizer, in the exact order written by {@link #toString()}: total buildings, min buildings,
     * min floors, max floors, min CF, max CF, building type (by {@link BuildingType} enum name), then starting edge.
     * Throws {@link NumberFormatException} or {@link IllegalArgumentException} if a token is malformed, and
     * {@link java.util.NoSuchElementException} if the tokenizer runs out of tokens early.
     */
    public void fromString(StringTokenizer buildingTemplate) {
        setTotalBuildings(Integer.parseInt(buildingTemplate.nextToken()));
        setMinBuildings(Integer.parseInt(buildingTemplate.nextToken()));
        setMinFloors(Integer.parseInt(buildingTemplate.nextToken()));
        setMaxFloors(Integer.parseInt(buildingTemplate.nextToken()));
        setMinCF(Integer.parseInt(buildingTemplate.nextToken()));
        setMaxCF(Integer.parseInt(buildingTemplate.nextToken()));
        setBuildingType(BuildingType.valueOf(buildingTemplate.nextToken()));
        setStartingEdge(Integer.parseInt(buildingTemplate.nextToken()));
    }
}

