/*
 *  MekWars - Copyright (C) 2007
 *
 *  original author - Nathan Morris (urgru@users.sourceforge.net)
 *  Change by Jason Tighe (torren@users.sourceforge.net)
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
 *
 *  Sort used for Armies
 */

package mekwars.common.util;

import java.util.Comparator;

import mekwars.common.campaign.CArmy;

/**
 * Comparator used to sort {@link CArmy} instances (a player's collection of units grouped into an army) by one of a
 * fixed set of criteria, selected by an integer code passed to the constructor. Used by the campaign/army-listing UI
 * (e.g. player army lists) wherever the user can choose a sort column/option.
 */
public class CArmyComparator implements Comparator<CArmy> {

    //NOTE: This order must match order of
    //sort options in CPLayer.sortArmies()'s
    //"choices" array.
    /** Sort alphabetically by army name. */
    public static final int ARMY_SORT_NAME = 0;
    /** Sort by total Battle Value of the army. */
    public static final int ARMY_SORT_BV = 1;
    /** Sort by the army's unique numeric ID. */
    public static final int ARMY_SORT_ID = 2;
    /** Sort by the army's total tonnage. */
    public static final int ARMY_SORT_TONNAGE = 3;
    /** Sort by the army's average walking movement points. */
    public static final int ARMY_SORT_AVG_MP_WALK = 4;
    /** Sort by the army's average jumping movement points. */
    public static final int ARMY_SORT_AVG_MP_JUMP = 5;
    /** Sort by the number of units contained in the army. */
    public static final int ARMY_SORT_UNITS = 6;
    /** No sorting is applied; {@link #compare} always reports the two armies as equal. */
    public static final int ARMY_SORT_NONE = 7;

    /** One of the {@code ARMY_SORT_*} constants selecting which field to compare on. */
    private final int sortOrder;

    /**
     * @param sortOrder one of the {@code ARMY_SORT_*} constants declared on this class, selecting the comparison
     *                  criterion used by {@link #compare(CArmy, CArmy)}.
     */
    public CArmyComparator(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Compares two armies according to the criterion chosen via the constructor's {@code sortOrder} argument.
     * Any {@code sortOrder} value not matching one of the declared {@code ARMY_SORT_*} constants falls through to
     * the {@code default} case and is treated as "equal" (returns 0), the same as {@link #ARMY_SORT_NONE}.
     *
     * @param lhs the first army to compare
     * @param rhs the second army to compare
     * @return a negative, zero, or positive value per the {@link Comparator} contract, based on the selected field
     */
    public int compare(CArmy lhs, CArmy rhs) {
        switch (sortOrder) {
            case ARMY_SORT_NAME: //the name
                return lhs.getName().compareTo(rhs.getName());

            case ARMY_SORT_BV: //self-evident
                Integer army1BV = lhs.getBV();
                Integer army2BV = rhs.getBV();
                return army1BV.compareTo(army2BV);

            case ARMY_SORT_ID: //the unique unit ID
                Integer army1ID = lhs.getID();
                Integer army2ID = rhs.getID();
                return army1ID.compareTo(army2ID);

            case ARMY_SORT_TONNAGE: //Total tonnage of the army
                Double army1Ton = lhs.getTotalTonnage();
                Double army2Ton = rhs.getTotalTonnage();
                return army1Ton.compareTo(army2Ton);

            case ARMY_SORT_AVG_MP_WALK: //average walk MP for the army
                Integer army1MP = lhs.getAverageWalk();
                Integer army2MP = rhs.getAverageWalk();
                return army1MP.compareTo(army2MP);

            case ARMY_SORT_AVG_MP_JUMP: //average jump mp of the army
                Integer army1JP = lhs.getAverageJump();
                Integer army2JP = rhs.getAverageJump();
                return army1JP.compareTo(army2JP);

            case ARMY_SORT_UNITS:
                Integer army1Size = lhs.getUnits().size();
                Integer army2Size = rhs.getUnits().size();
                return army1Size.compareTo(army2Size);

            default:
                return 0;
        }//end switch
    }//end compare()

}
