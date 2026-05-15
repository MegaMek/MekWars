/*
 * MekWars - Copyright (C) 2004
 *
 * Original author - Nathan Morris (urgru@users.sourceforge.net)
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
 * The AutoArmy is a pseudo-army which is constructed on the fly, loading artillery units in proportion to a BV.
 * <p>
 * Takes the given BV, loads server configs and loops through a set of weightclasses generating new artillery units.
 * Units are created, and the BV counter reduced, until there is no longer enough BV to create a tube of any type.
 * <p>
 * NOTE: This uses a slew of static arrays. MUST be adjusted if the static weightclass numbers in Unit.java are changed
 * (curr: Light == 0, Medium == 1, Heavy == 2, Assault == 3). Since there's no sensible reason to ever actually change
 * these, aside from adding support for super-heavyweight tanks sometime down the road, this should not be a major
 * concern.
 *
 * @ urgru 1.4.05
 */
package mekwars.server.campaign;

import common.Unit;
import common.util.MWLogger;

public class AutoArmy {

    //VARIABLES
    private java.util.Vector<mekwars.server.campaign.SUnit> theUnits;


    //CONSTRUCTORS
    public AutoArmy(int i, boolean Guns) {
        if (Guns) {theUnits = this.generateGuns(i);} else {theUnits = this.generateAuto(i);}
    }//end constructor

    //METHODS
    private java.util.Vector<mekwars.server.campaign.SUnit> generateGuns(int i) {

        //BV to auto against
        int bvOfSArmy = i;

        //holder
        java.util.Vector<mekwars.server.campaign.SUnit> autoUnits = new java.util.Vector<mekwars.server.campaign.SUnit>(
              1,
              1);

        //no auto army if 0'ed BV
        if (i <= 0) {return autoUnits;}

        //max of each type
        int maxLight = CampaignMain.campaignMain.getIntegerConfig("MaxLightGunEmplacement");
        int maxMedium = CampaignMain.campaignMain.getIntegerConfig("MaxMediumGunEmplacement");
        int maxHeavy = CampaignMain.campaignMain.getIntegerConfig("MaxHeavyGunEmplacement");
        int maxAssault = CampaignMain.campaignMain.getIntegerConfig("MaxAssaultGunEmplacement");
        int maxNumOfEachWeight[] = { maxLight, maxMedium, maxHeavy, maxAssault };

        //amount of BV to get each type.
        int bvForLight = CampaignMain.campaignMain.getIntegerConfig("BVForLightGunEmplacement");
        int bvForMedium = CampaignMain.campaignMain.getIntegerConfig("BVForMediumGunEmplacement");
        int bvForHeavy = CampaignMain.campaignMain.getIntegerConfig("BVForHeavyGunEmplacement");
        int bvForAssault = CampaignMain.campaignMain.getIntegerConfig("BVForAssaultGunEmplacement");
        int bvForEachWeight[] = { bvForLight, bvForMedium, bvForHeavy, bvForAssault };

        //server's preferred load order.
        boolean topToBottom = CampaignMain.campaignMain.getBooleanConfig("HeaviestGunEmplacementFirst");

        //get the PLAYER's preferences.
        /*
         * No capacity to read/set this yet. Coming soon. The player preference IS
         * handled properly in the rest of the code. Just need a way to get it here.
         */


        /*
         * Now that we have all the configs, give the players artillery. Look first at his preferences.
         * If he prefers artillery of a certain weight class, grant it first. After that, work in the
         * preferred server order (top to bottom, or bottom to top).
         */
        int loadOrder[] = { Unit.LIGHT, Unit.MEDIUM, Unit.HEAVY, Unit.ASSAULT };

        /*
         * GET THE PLAYER PREF HERE AND MAKE IT ELEMENT 0. For now, we'll assume that the
         * player preference is for a Medium piece (Sniper in default files).
         */
        loadOrder[0] = Unit.MEDIUM;
        int remainingWeights[] = { Unit.MEDIUM, Unit.HEAVY, Unit.ASSAULT };

        //determine the remaining weight classes, lightest to heaviest.
        int preferedWeight = loadOrder[0];
        int currentWeight = Unit.LIGHT;
        for (int j = 0; j < 3; j++) {//hardbind the 3 to stop NPE.

            //if the curr weight is pref'ed move up one class.
            if (currentWeight == preferedWeight) {currentWeight++;}

            //then set the order.
            remainingWeights[j] = currentWeight;

            //then increment the weight so its one higher for the next loop
            currentWeight++;

        }//end for(remaining weights)


        //now we know what remains. add it to loadorder in the proper direction.
        if (topToBottom) {
            loadOrder[1] = remainingWeights[2];
            loadOrder[2] = remainingWeights[1];
            loadOrder[3] = remainingWeights[0];
        } else {//its bottom to top
            loadOrder[1] = remainingWeights[0];
            loadOrder[2] = remainingWeights[1];
            loadOrder[3] = remainingWeights[2];
        }

        /*
         * Now that we know the the complete load order, start making units.
         * Run the length of the loadOrder array ...
         */
        for (int k = 0; k < 4; k++) {

            //piece count. tracks how many of currWeight tubes have been added.
            int numOfCurrWeight = 0;

            //load the weighclass at the loadorder location
            int currWeight = loadOrder[k];

            //get the BV and ceilings for this weightclass
            int bvForCurrWeight = bvForEachWeight[currWeight];
            int maxNumOfCurrWeight = maxNumOfEachWeight[currWeight];

            //while we have enough BV to get another tube, and havent hit the server cap, continue ...
            while (bvOfSArmy >= bvForCurrWeight && numOfCurrWeight < maxNumOfCurrWeight) {

                //make the unit and add it to the units vector
                autoUnits.addAll(this.makeNewGunEmplacement(currWeight));

                //decrement the BV and boost the counter
                bvOfSArmy = bvOfSArmy - bvForCurrWeight;
                numOfCurrWeight = numOfCurrWeight + 1;
            }//end while(enough BV and uncapped.)

        }//end for(all weights in load order)

        //units constructed. return autounits.
        return autoUnits;
    }//end generateAuto()

    //METHODS
    private java.util.Vector<mekwars.server.campaign.SUnit> generateAuto(int i) {

        //BV to auto against
        int bvOfSArmy = i;

        //holder
        java.util.Vector<mekwars.server.campaign.SUnit> autoUnits = new java.util.Vector<mekwars.server.campaign.SUnit>(
              1,
              1);

        //no auto army if 0'ed BV
        if (i <= 0) {return autoUnits;}

        //max of each type
        int maxLight = CampaignMain.campaignMain.getIntegerConfig("MaxLightArtillery");
        int maxMedium = CampaignMain.campaignMain.getIntegerConfig("MaxMediumArtillery");
        int maxHeavy = CampaignMain.campaignMain.getIntegerConfig("MaxHeavyArtillery");
        int maxAssault = CampaignMain.campaignMain.getIntegerConfig("MaxAssaultArtillery");
        int maxNumOfEachWeight[] = { maxLight, maxMedium, maxHeavy, maxAssault };

        //amount of BV to get each type.
        int bvForLight = CampaignMain.campaignMain.getIntegerConfig("BVForLightArtillery");
        int bvForMedium = CampaignMain.campaignMain.getIntegerConfig("BVForMediumArtillery");
        int bvForHeavy = CampaignMain.campaignMain.getIntegerConfig("BVForHeavyArtillery");
        int bvForAssault = CampaignMain.campaignMain.getIntegerConfig("BVForAssaultArtillery");
        int bvForEachWeight[] = { bvForLight, bvForMedium, bvForHeavy, bvForAssault };

        //server's preferred load order.
        boolean topToBottom = CampaignMain.campaignMain.getBooleanConfig("HeaviestArtilleryFirst");

        //get the PLAYER's preferences.
        /*
         * No capacity to read/set this yet. Coming soon. The player preference IS
         * handled properly in the rest of the code. Just need a way to get it here.
         */


        /*
         * Now that we have all the configs, give the players artillery. Look first at his preferences.
         * If he prefers artillery of a certain weight class, grant it first. After that, work in the
         * preferred server order (top to bottom, or bottom to top).
         */
        int loadOrder[] = { Unit.LIGHT, Unit.MEDIUM, Unit.HEAVY, Unit.ASSAULT };

        /*
         * GET THE PLAYER PREF HERE AND MAKE IT ELEMENT 0. For now, we'll assume that the
         * player preference is for a Medium piece (Sniper in default files).
         */
        loadOrder[0] = Unit.MEDIUM;
        int remainingWeights[] = { Unit.MEDIUM, Unit.HEAVY, Unit.ASSAULT };

        //determine the remaining weight classes, lightest to heaviest.
        int preferedWeight = loadOrder[0];
        int currentWeight = Unit.LIGHT;
        for (int j = 0; j < 3; j++) {//hardbind the 3 to stop NPE.

            //if the curr weight is pref'ed move up one class.
            if (currentWeight == preferedWeight) {currentWeight++;}

            //then set the order.
            remainingWeights[j] = currentWeight;

            //then increment the weight so its one higher for the next loop
            currentWeight++;

        }//end for(remaining weights)


        //now we know what remains. add it to loadorder in the proper direction.
        if (topToBottom) {
            loadOrder[1] = remainingWeights[2];
            loadOrder[2] = remainingWeights[1];
            loadOrder[3] = remainingWeights[0];
        } else {//its bottom to top
            loadOrder[1] = remainingWeights[0];
            loadOrder[2] = remainingWeights[1];
            loadOrder[3] = remainingWeights[2];
        }

        /*
         * Now that we know the the complete load order, start making units.
         * Run the length of the loadOrder array ...
         */
        for (int k = 0; k < 4; k++) {

            //piece count. tracks how many of currWeight tubes have been added.
            int numOfCurrWeight = 0;

            //load the weighclass at the loadorder location
            int currWeight = loadOrder[k];

            //get the BV and ceilings for this weightclass
            int bvForCurrWeight = bvForEachWeight[currWeight];
            int maxNumOfCurrWeight = maxNumOfEachWeight[currWeight];

            //while we have enough BV to get another tube, and havent hit the server cap, continue ...
            while (bvOfSArmy >= bvForCurrWeight && numOfCurrWeight < maxNumOfCurrWeight) {

                //make the unit and add it to the units vector
                autoUnits.add(this.makeNewArtilleryPiece(currWeight));

                //decrement the BV and boost the counter
                bvOfSArmy = bvOfSArmy - bvForCurrWeight;
                numOfCurrWeight = numOfCurrWeight + 1;
            }//end while(enough BV and uncapped.)

        }//end for(all weights in load order)

        //units constructed. return autounits.
        return autoUnits;
    }//end generateAuto()

    /**
     * Method which takes a weightclass and loads a new SUnit
     *
     * @param i weightclass to make
     *
     * @return an artillery unit
     */
    public java.util.Vector<mekwars.server.campaign.SUnit> makeNewGunEmplacement(int i) {
        //file to get
        String filename = "";
        int size = 0;
        int position = 0;
        java.util.Vector<mekwars.server.campaign.SUnit> returnedUnits = new java.util.Vector<mekwars.server.campaign.SUnit>(
              1,
              1);

        if (Unit.LIGHT == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "LightGunEmplacementFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else if (Unit.MEDIUM == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "MediumGunEmplacementFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else if (Unit.HEAVY == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "HeavyGunEmplacementFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "AssaultGunEmplacementFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        }//assume assault

        //now build the unit.
        if (filename.toLowerCase().endsWith(".mul")) {
            returnedUnits.addAll(SUnit.createMULUnits(filename));
        } else {returnedUnits.add(new SUnit("autoassigned unit", filename, i));}
        return returnedUnits;
    }//end makenewgunemplacement

    /**
     * Method which takes a weightclass and loads a new SUnit
     *
     * @param i weightclass to make
     *
     * @return an artillery unit
     */
    public SUnit makeNewArtilleryPiece(int i) {

        //file to get
        String filename = "";
        int size = 0;
        int position = 0;

        if (Unit.LIGHT == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "LightArtilleryFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else if (Unit.MEDIUM == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "MediumArtilleryFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else if (Unit.HEAVY == i) {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "HeavyArtilleryFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        } else {
            java.util.StringTokenizer list = new java.util.StringTokenizer(CampaignMain.campaignMain.getConfig(
                  "AssaultArtilleryFile"), "$");

            size = list.countTokens();

            if (size == 1) {filename = list.nextToken();} else {
                position = CampaignMain.campaignMain.getRandomNumber(size) + 1;

                for (int count = 0; count < position; count++) {filename = list.nextToken();}
            }
        }//assume assault

        //now build the unit.
        SUnit cm = new SUnit("autoassigned unit", filename, i);
        return cm;
    }//end makenewartillerypiece

    /**
     * @return aggregate BV of the units
     */
    public int getBV() {
        int toReturn = 0;
        for (SUnit currU : this.getUnits()) {toReturn += currU.getBVForMatch();}

        return toReturn;
    }

    /**
     * Method which returns the autogenerated units (as a vector).
     */
    public java.util.Vector<mekwars.server.campaign.SUnit> getUnits() {
        return theUnits;
    }

    /**
     * Method which strings the AutoArmy for transport to a client.
     * <p>
     * Feed it the vector to use as a paramater.
     */
    public String toString(String delimiter) {

        StringBuilder result = new StringBuilder();

        /*
         * Just send a list of the weightclasses. client has the
         * file path and file names in the server config it received
         * during connection.
         */
        java.util.Vector<mekwars.server.campaign.SUnit> v = this.getUnits();

        try {
            v.elements();
        } catch (Exception e) {
            MWLogger.mainLog("AUTOARMY UNITS WERE NULL");
        }

        for (SUnit currU : this.getUnits()) {
            result.append(currU.getEntity().getChassis());
            result.append(" ");
            result.append(currU.getEntity().getModel());
            result.append(delimiter);
        }

        return result.toString();
    }//end toString()

}//end autoarmy class
