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

package mekwars.server.campaign.mercenaries;

public class MercHouse extends server.campaign.SHouse {


    /**
     *
     */
    private static final long serialVersionUID = 5440419107794020517L;
    //merc vars
    private java.util.Hashtable<String, ContractInfo> OutstandingContracts = new java.util.Hashtable<String, ContractInfo>();

    //constructor
    public MercHouse(int id, String name, String HouseColor, int BaseGunner, int BasePilot, String abbreviation) {
        super(id, name, HouseColor, BaseGunner, BasePilot, abbreviation);
    }//end constructor

    public MercHouse(int id) {
        super(id);
    }

    /**
     * Constructor used for serialization
     */
    public MercHouse() {
        super();
    }

    /**
     * A method which overrides SHouse's getDistance. returns distance in terms of employer's reach, or a maxval if a
     * merc has no employer.
     *
     * @param p      - planet one is attempting to reach
     * @param player - used to draw out contract info, ascertain employer
     *
     * @return int - distance to the planet being checked
     */
    @Override
    public int getDistanceTo(server.campaign.SPlanet p, server.campaign.SPlayer player) {
        if (this.getHouseFightingFor(player).isMercHouse())//mercs cant hire other mercs, so this is a safe check
        {return Integer.MAX_VALUE - 2;}
        //else
        return this.getHouseFightingFor(player).getDistanceTo(p, player);
    }

    /**
     * A method which override's SHouse's isMerc() and returns true.
     *
     * @return boolean - true
     */
    @Override
    public boolean isMercHouse() {
        return true;
    }

    /**
     * COULD USE containsKey(k) IN PLACE OF NULL CHECK! SHOULD PROLLY CHANGE.
     * <p>
     * A method which determines which faction a mercenary is currently fighting for by checking his contract
     * information.
     *
     * @param player - a player to use as hash key
     *
     * @return SHouse - the faction the player is fighting for
     */
    @Override
    public server.campaign.SHouse getHouseFightingFor(server.campaign.SPlayer player) {

        ContractInfo playerContract = getOutstandingContracts().get(player.getName().toLowerCase());
        if (playerContract != null) { //if a contract exists, return employer
            return playerContract.getEmployingHouse();
        }
        return this;
    }

    /**
     * A method which adds a player contract to the OutstandingContracts hash
     *
     * @param cToAdd - contract to add to the hash
     * @param player - SPlayer to use as key in hash
     */
    public void setContract(ContractInfo cToAdd, server.campaign.SPlayer player) {
        //add contract to hash, with player as key.
        getOutstandingContracts().put(player.getName().toLowerCase(), cToAdd);
        setOutstandingContracts(getOutstandingContracts());
    }

    /**
     * A method which deletes contracts, making players employable as a result
     *
     * @param player - an SPlayer player, used as a key to search hash for the contract
     *
     * @return boolean - boolean indicating whether or not a contract was removed
     */
    public boolean endContract(server.campaign.SPlayer player) {
        boolean terminated = false;
        if (getOutstandingContracts().containsKey(player.getName().toLowerCase())) { // proced to remove
            getOutstandingContracts().remove(player.getName().toLowerCase());
            //and then add the player to the potential hires list
            //UnemployedPlayers.put(player, player.getName());
            //and set a boolean to return..
            terminated = true;
        }//end if(contract exists to terminate)
        return terminated;
    }

    /**
     * A method which returns the contract a given player is performing
     *
     * @param player - a player to search hash with
     *
     * @return ContractInfo - the player's contract
     */
    public ContractInfo getContractInfo(server.campaign.SPlayer player) {
        ContractInfo currentContract = null;
        currentContract = getOutstandingContracts().get(player.getName().toLowerCase());
        return currentContract;
    }

    /**
     * @return int number of bays given to SOLies
     *
     * @urgru 8/18/04
     *       <p>
     *       Override the getBaysProvided from SHouse and return MercHouseBays from the campaignconfig.txt
     */
    @Override
    public int getBaysProvided() {
        int mercBays = server.campaign.CampaignMain.cm.getIntegerConfig("MercHouseBays");
        return mercBays;
    }


    /**
     * Mercs get no welfare, instead a loan rec. urgru 11/11/02
     * <p>
     * A method which overrides standard faction welfare, removing financial assistance for mercs.
     *
     * @param p - player being checked
     *
     * @return string - a string, indicating result of welfare check
     */
    public String payWelfare(server.campaign.SPlayer p) {
        String s = "";
        if (p.getMoney() < 30) {
            s = "You're running low on funds. It may be time to secure a loan.";
        }//end if
        else {
            s = "";
        }//end else
        return s;
    }//end merc modified welfare

    //Wiz's save code.
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("[M]");
        result.append(super.toString());
        //Also save the contracts
        //First check all contracts if they're legal and delete illegal ones
        java.util.Enumeration<ContractInfo> e = getOutstandingContracts().elements();
        while (e.hasMoreElements()) {
            ContractInfo ci = e.nextElement();
            if (!ci.isLegal()) {
                getOutstandingContracts().remove(ci.getOfferingPlayerName().toLowerCase());

            }
        }
        //Store the contracts to the Save
        result.append(getOutstandingContracts().size());
        result.append("|");
        e = getOutstandingContracts().elements();
        while (e.hasMoreElements()) {
            ContractInfo ci = e.nextElement();
            result.append(ci.toString());
            result.append("|");
        }
        return result.toString();
    }

    public java.util.Hashtable<String, ContractInfo> getOutstandingContracts() {
        return OutstandingContracts;
    }

    public void setOutstandingContracts(java.util.Hashtable<String, ContractInfo> h) {
        OutstandingContracts = h;
    }

    public boolean canConquerPlanets() {
        return false;
    }

}//end MercHouse
