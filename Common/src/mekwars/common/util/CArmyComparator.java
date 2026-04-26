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

public class CArmyComparator implements Comparator<CArmy> {

    //NOTE: This order must match order of
    //sort options in CPLayer.sortArmies()'s
    //"choices" array.
    public static final int ARMY_SORT_NAME = 0;
    public static final int ARMY_SORT_BV = 1;
    public static final int ARMY_SORT_ID = 2;
    public static final int ARMY_SORT_TONNAGE = 3;
    public static final int ARMY_SORT_AVG_MP_WALK = 4;
    public static final int ARMY_SORT_AVG_MP_JUMP = 5;
    public static final int ARMY_SORT_UNITS = 6;
    public static final int ARMY_SORT_NONE = 7;

    private final int sortOrder;

    public CArmyComparator(int sortOrder) {
        this.sortOrder = sortOrder;
    }

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
                Float army1Ton = lhs.getTotalTonnage();
                Float army2Ton = rhs.getTotalTonnage();
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
