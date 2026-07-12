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

package mekwars.common.util;

import mekwars.common.campaign.CUnit;

/**
 * Comparator used to sort {@link CUnit} instances (a single unit in a player's hangar/campaign roster) by one of a
 * fixed set of criteria, selected by an integer code passed to the constructor. Used by the campaign hangar UI
 * (e.g. {@code CPLayer.sortHangar()}) wherever the user can choose a sort column/option.
 */
public class CUnitComparator implements java.util.Comparator<CUnit> {

    //NOTE: This order must match order of
    //sort options in CPLayer.sortHangar()'s
    //"choices" array.
    /** Sort alphabetically by the unit's filename. */
    public static final int HQ_SORT_NAME = 0;
    /** Sort by the unit's Battle Value as used for matchmaking. */
    public static final int HQ_SORT_BV = 1;
    /** Sort by the assigned pilot's gunnery skill. */
    public static final int HQ_SORT_GUNNERY = 2;
    /** Sort by the unit's unique numeric ID. */
    public static final int HQ_SORT_ID = 3;
    /** Sort by the unit's jump movement points. */
    public static final int HQ_SORT_JUMP_MP = 4;
    /** Sort by the unit's walking movement points. */
    public static final int HQ_SORT_WALK_MP = 5;
    /** Sort by the assigned pilot's total kill count. */
    public static final int HQ_SORT_PILOT_KILLS = 6;
    /** Sort by unit type (Mek, vehicle, infantry, etc.). */
    public static final int HQ_SORT_TYPE = 7;
    /** Sort by weight class (light/medium/heavy/assault, etc.). */
    public static final int HQ_SORT_WEIGHT_CLASS = 8;
    /** Sort by the unit's exact tonnage. */
    public static final int HQ_SORT_WEIGHT_TONS = 9;
    /** No sorting is applied; {@link #compare} always reports the two units as equal. */
    public static final int HQ_SORT_NONE = 10;

    /** One of the {@code HQ_SORT_*} constants selecting which field to compare on. */
    private final int sortOrder;

    /**
     * @param sortOrder one of the {@code HQ_SORT_*} constants declared on this class, selecting the comparison
     *                  criterion used by {@link #compare(CUnit, CUnit)}.
     */
    public CUnitComparator(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Compares two units according to the criterion chosen via the constructor's {@code sortOrder} argument.
     * Any {@code sortOrder} value not matching one of the declared {@code HQ_SORT_*} constants falls through to
     * the {@code default} case and is treated as "equal" (returns 0), the same as {@link #HQ_SORT_NONE}.
     *
     * @param lhs the first unit to compare
     * @param rhs the second unit to compare
     * @return a negative, zero, or positive value per the {@link java.util.Comparator} contract, based on the
     *     selected field
     */
    public int compare(CUnit lhs, CUnit rhs) {
        switch (sortOrder) {
            case HQ_SORT_NAME: //the name
                return lhs.getUnitFilename().compareTo(rhs.getUnitFilename());

            case HQ_SORT_BV: //self-evident
                Integer unit1BV = lhs.getBVForMatch();
                Integer unit2BV = rhs.getBVForMatch();
                return unit1BV.compareTo(unit2BV);

            case HQ_SORT_GUNNERY: //gunnery
                Integer unit1Gunnery = lhs.getPilot().getGunnery();
                Integer unit2Gunnery = rhs.getPilot().getGunnery();
                return unit1Gunnery.compareTo(unit2Gunnery);

            case HQ_SORT_ID: //the unique unit ID
                Integer unit1ID = lhs.getId();
                Integer unit2ID = rhs.getId();
                return unit1ID.compareTo(unit2ID);

            case HQ_SORT_JUMP_MP: //unit's jump movement
                Integer unit1JMP = lhs.getEntity().getJumpMP();
                Integer unit2JMP = rhs.getEntity().getJumpMP();
                return unit1JMP.compareTo(unit2JMP);

            case HQ_SORT_WALK_MP: //unit's jump movement
                Integer unit1WMP = lhs.getEntity().getWalkMP();
                Integer unit2WMP = rhs.getEntity().getWalkMP();
                return unit1WMP.compareTo(unit2WMP);

            case HQ_SORT_PILOT_KILLS: //Pilot's Kills
                Integer unit1PK = lhs.getPilot().getKills();
                Integer unit2PK = rhs.getPilot().getKills();
                return unit1PK.compareTo(unit2PK);

            case HQ_SORT_TYPE: //type as in Mech, Veh, Inf, etc.
                Integer unit1Type = lhs.getType();
                Integer unit2Type = rhs.getType();
                return unit1Type.compareTo(unit2Type);

            case HQ_SORT_WEIGHT_CLASS: //sort by general class
                Integer unit1Class = lhs.getWeightClass();
                Integer unit2Class = rhs.getWeightClass();
                return unit1Class.compareTo(unit2Class);

            case HQ_SORT_WEIGHT_TONS: //sort by entity weight
                Float unit1Mass = (float) lhs.getEntity().getWeight();
                Float unit2Mass = (float) rhs.getEntity().getWeight();
                return unit1Mass.compareTo(unit2Mass);

            default:
                return 0;
        }//end switch
    }//end compare()

}
