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
 *
 * Building data for RMG's
 * 
 */

package common.campaign;

import java.util.StringTokenizer;


public final class Buildings{
    
    public static final int EDGE_UNKNOWN = -1;
    //Have to follow MM starting postion protocals. Left out NW, NE, SW, SE for easier computations.
    public static final int NORTHWEST = 1;
    public static final int NORTH = 2;
    public static final int NORTHEAST= 3;
    public static final int EAST = 4;
    public static final int SOUTHEAST = 5;
    public static final int SOUTH = 6;
    public static final int SOUTHWEST = 7;
    public static final int WEST = 8;

    public static final int EDGE = 9;
    public static final int CENTER = 10;

    public static final int NORTHWESTDEEP = 11;
    public static final int NORTHDEEP = 12;
    public static final int NORTHEASTDEEP= 13;
    public static final int EASTDEEP = 14;
    public static final int SOUTHEASTDEEP = 15;
    public static final int SOUTHDEEP = 16;
    public static final int SOUTHWESTDEEP = 17;
    public static final int WESTDEEP = 18;

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
    
    public Buildings(){
        super();
    }
    
    public void setTotalBuildings(int total){
        totalBuildings = total;
    }
    
    public int getTotalBuildings(){
        return totalBuildings;
    }
    
    public void setMinBuildings(int min){
        minBuildings = min;
    }
    
    public int getMinBuildings(){
        return minBuildings;
    }
    
    public void setMinFloors(int min){
        minFloors = min;
    }
    
    public int getMinFloors(){
        return minFloors;
    }
    
    public void setMaxFloors(int max){
        maxFloors = max;
    }
    
    public int getMaxFloors(){
        return maxFloors;
    }
    
    public void setMinCF(int cf){
        minCF = cf;
    }
    
    public int getMinCF(){
        return minCF;
    }
    
    public void setMaxCF(int cf){
        maxCF = cf;
    }
    
    public int getMaxCF(){
        return maxCF;
    }
    
    public void setStartingEdge(int edge){
        startingEdge = edge;
    }
    
    public int getStartingEdge(){
        return startingEdge;
    }
    
    public void setBuildingType(String type){
        buildingType = type;
    }
    
    public String getBuildingType(){
        return buildingType;
    }
    
    public void setCityType(String type){
        cityType = type;
    }
    
    public String getCityType(){
        return cityType;
    }
    
    public void setCityBlocks(int blocks){
        cityBlocks = blocks;
    }
    
    public int getCityBlocks(){
        return cityBlocks;
    }
    
    @Override
	public String toString(){
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
    
    public void fromString(StringTokenizer buildingTemplate){
        
        setTotalBuildings(Integer.valueOf(buildingTemplate.nextToken()));
        setMinBuildings(Integer.valueOf(buildingTemplate.nextToken()));
        setMinFloors(Integer.valueOf(buildingTemplate.nextToken()));
        setMaxFloors(Integer.valueOf(buildingTemplate.nextToken()));
        setMinCF(Integer.valueOf(buildingTemplate.nextToken()));
        setMaxCF(Integer.valueOf(buildingTemplate.nextToken()));
        setBuildingType(buildingTemplate.nextToken());
        setStartingEdge(Integer.valueOf(buildingTemplate.nextToken()));
        
    }
}

