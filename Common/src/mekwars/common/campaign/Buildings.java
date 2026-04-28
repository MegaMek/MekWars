/*
 * MekWars - Copyright (C) 2004
 *
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

/**
 * @author jtighe
 *       <p>
 *       Building data for RMG's
 *
 */

package mekwars.common.campaign;

import java.util.StringTokenizer;


public final class Buildings {

    public static final int EDGE_UNKNOWN = -1;
    //Have to follow MM starting position protocols. Left out NW, NE, SW, SE for easier computations.
    public static final int NORTHWEST = 1;
    public static final int NORTH = 2;
    public static final int NORTHEAST = 3;
    public static final int EAST = 4;
    public static final int SOUTHEAST = 5;
    public static final int SOUTH = 6;
    public static final int SOUTHWEST = 7;
    public static final int WEST = 8;

    public static final int EDGE = 9;
    public static final int CENTER = 10;

    public static final int NORTHWEST_DEEP = 11;
    public static final int NORTH_DEEP = 12;
    public static final int NORTHEAST_DEEP = 13;
    public static final int EAST_DEEP = 14;
    public static final int SOUTHEAST_DEEP = 15;
    public static final int SOUTH_DEEP = 16;
    public static final int SOUTHWEST_DEEP = 17;
    public static final int WEST_DEEP = 18;

    private int totalBuildings = 0;
    private int minBuildings = 0;
    private int minFloors = 0;
    private int maxFloors = 0;
    private int minCF = 0;
    private int maxCF = 0;
    private int startingEdge = EDGE_UNKNOWN;
    private String buildingType = "1";
    private String cityType = "NONE";
    private int cityBlocks = 0;

    public Buildings() {
        super();
    }

    public String getCityType() {
        return cityType;
    }

    public void setCityType(String type) {
        cityType = type;
    }

    public int getCityBlocks() {
        return cityBlocks;
    }

    public void setCityBlocks(int blocks) {
        cityBlocks = blocks;
    }

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

    public int getTotalBuildings() {
        return totalBuildings;
    }

    public void setTotalBuildings(int total) {
        totalBuildings = total;
    }

    public int getMinBuildings() {
        return minBuildings;
    }

    public void setMinBuildings(int min) {
        minBuildings = min;
    }

    public int getMinFloors() {
        return minFloors;
    }

    public void setMinFloors(int min) {
        minFloors = min;
    }

    public int getMaxFloors() {
        return maxFloors;
    }

    public void setMaxFloors(int max) {
        maxFloors = max;
    }

    public int getMinCF() {
        return minCF;
    }

    public void setMinCF(int cf) {
        minCF = cf;
    }

    public int getMaxCF() {
        return maxCF;
    }

    public void setMaxCF(int cf) {
        maxCF = cf;
    }

    public String getBuildingType() {
        return buildingType;
    }

    public int getStartingEdge() {
        return startingEdge;
    }

    public void setStartingEdge(int edge) {
        startingEdge = edge;
    }

    public void setBuildingType(String type) {
        buildingType = type;
    }

    public void fromString(StringTokenizer buildingTemplate) {
        setTotalBuildings(Integer.parseInt(buildingTemplate.nextToken()));
        setMinBuildings(Integer.parseInt(buildingTemplate.nextToken()));
        setMinFloors(Integer.parseInt(buildingTemplate.nextToken()));
        setMaxFloors(Integer.parseInt(buildingTemplate.nextToken()));
        setMinCF(Integer.parseInt(buildingTemplate.nextToken()));
        setMaxCF(Integer.parseInt(buildingTemplate.nextToken()));
        setBuildingType(buildingTemplate.nextToken());
        setStartingEdge(Integer.parseInt(buildingTemplate.nextToken()));
    }
}

