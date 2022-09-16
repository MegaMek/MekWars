/*
 *  MekWars - Copyright (C) 2004 
 * 
 *  original author - Nathan Morris (urgru@users.sourceforge.net)
 * 
 *  This program is free software; you can redistribute it and/or modify it 
 *  under the terms of the GNU General Public License as published by the Free 
 *  Software Foundation; either version 2 of the License, or (at your option) 
 *  any later version.
 * 
 *  This program is distributed in the hope that it will be useful, but 
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY 
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 *  for more details.
 */
 
 package client.util;
 
 import java.util.Comparator;

import client.campaign.CUnit;

 public class CUnitComparator implements Comparator<Object> {

 	//NOTE: This order must match order of
 	//sort options in CPLayer.sortHangar()'s
 	//"choices" array.
 	public static final int HQSORT_NAME = 0;
 	public static final int HQSORT_BV = 1;
 	public static final int HQSORT_GUNNERY = 2;
 	public static final int HQSORT_ID = 3;
 	public static final int HQSORT_JUMPMP = 4;
 	public static final int HQSORT_WALKMP = 5;
 	public static final int HQSORT_PILOTKILLS = 6;
 	public static final int HQSORT_TYPE = 7;
 	public static final int HQSORT_WEIGHTCLASS = 8;
 	public static final int HQSORT_WEIGHTTONS = 9;
 	public static final int HQSORT_NONE = 10;
 	
 	private int sortOrder;
 	
 	public CUnitComparator(int sortOrder) {
 		this.sortOrder = sortOrder;
 	}
 	
 	public int compare(Object obj1, Object obj2) {
 		
 		CUnit unit1 = (CUnit)obj1;
 		CUnit unit2 = (CUnit)obj2;
 		
 		switch (sortOrder) {
 		
 			case HQSORT_NAME : //the name
				return unit1.getUnitFilename().compareTo(unit2.getUnitFilename());
 		
 			case HQSORT_BV : //self evident
 				Integer unit1BV = unit1.getBVForMatch();
 				Integer unit2BV = unit2.getBVForMatch();
 				return unit1BV.compareTo(unit2BV);
 				
 			case HQSORT_GUNNERY : //gunnery
 				Integer unit1Gunnery = unit1.getPilot().getGunnery();
 				Integer unit2Gunnery = unit2.getPilot().getGunnery();
 				return unit1Gunnery.compareTo(unit2Gunnery);
 				
 			case HQSORT_ID : //the unique unit ID
 				Integer unit1ID = unit1.getId();
 				Integer unit2ID = unit2.getId();
 				return unit1ID.compareTo(unit2ID);	
 				
 			case HQSORT_JUMPMP : //unit's jump movement
 				Integer unit1JMP = unit1.getEntity().getJumpMP();
 				Integer unit2JMP = unit2.getEntity().getJumpMP();
 				return unit1JMP.compareTo(unit2JMP);
 				
 			case HQSORT_WALKMP : //unit's jump movement
 				Integer unit1WMP = unit1.getEntity().getWalkMP();
 				Integer unit2WMP = unit2.getEntity().getWalkMP();
 				return unit1WMP.compareTo(unit2WMP);
 				
 			case HQSORT_PILOTKILLS : //Pilot's Kills
 				Integer unit1PK = unit1.getPilot().getKills();
 				Integer unit2PK = unit2.getPilot().getKills();
 				return unit1PK.compareTo(unit2PK);
 				
 			case HQSORT_TYPE : //type as in Mech, Veh, Inf, etc.
 				Integer unit1Type = unit1.getType();
 				Integer unit2Type = unit2.getType();
 				return unit1Type.compareTo(unit2Type);	
 				
 			case HQSORT_WEIGHTCLASS : //sort by general class
 				Integer unit1Class = unit1.getWeightclass();
 				Integer unit2Class = unit2.getWeightclass();
 				return unit1Class.compareTo(unit2Class);	 				
 				
 			case HQSORT_WEIGHTTONS : //sort by entity weight
 				Float unit1Mass = (float)unit1.getEntity().getWeight();
 				Float unit2Mass = (float)unit2.getEntity().getWeight();
 				return unit1Mass.compareTo(unit2Mass);
 				
 			default :
 				return 0;
 		}//end switch
 	}//end compare()

}