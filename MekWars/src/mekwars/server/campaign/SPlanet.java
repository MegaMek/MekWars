/*
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

package server.campaign;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import common.AdvancedTerrain;
import common.CampaignData;
import common.Continent;
import common.House;
import common.Influences;
import common.Terrain;
import common.Unit;
import common.UnitFactory;
import common.util.MWLogger;
import common.util.Position;
import common.util.TokenReader;
import server.campaign.data.TimeUpdatePlanet;
import server.campaign.util.SerializedMessage;

public class SPlanet extends TimeUpdatePlanet implements Serializable, Comparable<Object> {

    /**
     * 
     */
    private static final long serialVersionUID = -2266871107987235842L;
    private SHouse owner = null;

    @Override
    public String toString() {
        SerializedMessage result = new SerializedMessage("#");
        result.append("PL");
        result.append(getName());
        result.append(getCompProduction());
        if (getUnitFactories() != null) {
            result.append(getUnitFactories().size());
            for (UnitFactory factory : getUnitFactories()) {
                // int i = 0; i < getUnitFactories().size(); i++) {
                // SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
                result.append(((SUnitFactory) factory).toString());
            }
        } else
            result.append("0");

        result.append(getPosition().getX());
        result.append(getPosition().getY());
        StringBuilder houseString = new StringBuilder();
        for (House house : getInfluence().getHouses()) {
            SHouse next = (SHouse) house;
            if (next == null)
                continue;
            houseString.append(next.getName());
            houseString.append("$"); // change for unusual influence
            houseString.append(getInfluence().getInfluence(next.getId()));
            houseString.append("$"); // change for unusual influence
        }
        // No Influences then set influence to NewbieHouse so the planet will
        // load.
        if (getInfluence().getHouses().size() < 1) {
        	houseString.append(CampaignMain.cm.getConfig("NewbieHouseName"));
        	houseString.append("$");
        	houseString.append(this.getConquestPoints());
        	houseString.append("$");
        }

        result.append(houseString.toString());
        result.append(getEnvironments().size());
        for (Continent t : getEnvironments().toArray()) {
            result.append(t.getSize());
            result.append(t.getEnvironment().getName());
            
            if(t.getAdvancedTerrain() != null)
            	if(t.getAdvancedTerrain().getName() != null)
            result.append(t.getAdvancedTerrain().getName());
        }
        if (getDescription().equals(""))
            result.append(" ");
        else
            result.append(getDescription());
        result.append(this.getBaysProvided());
        result.append(this.isConquerable());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        result.append(sdf.format(this.getLastChanged()));
        result.append(this.getId());
        result.append(this.getMinPlanetOwnerShip());
        result.append(isHomeWorld());
        result.append(getOriginalOwner());

        if (this.getPlanetFlags().size() > 0) {
            for (String key : this.getPlanetFlags().keySet()) {
                result.append(key + "^");
            }
        } else
            result.append("^^");

        result.append(this.getConquestPoints());

        return result.toString();
    }

    /**
     * 
     */
    public String fromString(String s, Random r, CampaignData data) {
        // debug

        boolean singleFaction = CampaignMain.cm.getBooleanConfig("AllowSinglePlayerFactions");
        MWLogger.mainLog(s);
        s = s.substring(3);
        StringTokenizer ST = new StringTokenizer(s, "#");
        setName(TokenReader.readString(ST));
        setCompProduction(TokenReader.readInt(ST));
        // Read Factories
        int hasMF = TokenReader.readInt(ST);
        for (int i = 0; i < hasMF; i++) {
            SUnitFactory mft = new SUnitFactory();
            mft.fromString(TokenReader.readString(ST), this, r);
            if (singleFaction && CampaignMain.cm.getHouseFromPartialString(mft.getFounder()) == null)
                continue;
            getUnitFactories().add(mft);
        }

        setPosition(new Position(TokenReader.readDouble(ST), TokenReader.readDouble(ST)));

//        int Infcount = 0;
        try {
            HashMap<Integer, Integer> influence = new HashMap<Integer, Integer>();
            {
                StringTokenizer influences = new StringTokenizer(TokenReader.readString(ST), "$");

                while (influences.hasMoreElements()) {
                    String HouseName = TokenReader.readString(influences);
                    SHouse h = (SHouse) data.getHouseByName(HouseName);
                    int HouseInf = TokenReader.readInt(influences);
//                    Infcount += HouseInf;
                    if (h != null)
                        influence.put(h.getId(), HouseInf);
                    else
                        MWLogger.errLog("House not found: " + HouseName);
                }
            }
            // getInfluence().setInfluence(influence);
            setInfluence(new Influences(influence));
        } catch (RuntimeException ex) {
            MWLogger.errLog("Problem on Planet: " + this.getName());
            MWLogger.errLog(ex);
        }
        int Envs = TokenReader.readInt(ST);
        for (int i = 0; i < Envs; i++) {
            int size = TokenReader.readInt(ST);
            String terrain = TokenReader.readString(ST);
            String advTerrain = TokenReader.readString(ST);
            int terrainNumber = 0;
            int advTerrainNumber = 0;
            Terrain planetEnvironment = null;
            AdvancedTerrain planetWeather = null; 
            /*
             * Bug reported if you screw with the positions of the terrains in terrain.xml you'll screw up the planet terrains this will now allow you to load via int and then save via name so the terrain will always be correct no matter the position of the terrain in the terrain.xml.
             */
            try {
                terrainNumber = Integer.parseInt(terrain);
                planetEnvironment = data.getTerrain(terrainNumber);
            } catch (Exception ex) {
                planetEnvironment = data.getTerrainByName(terrain);
            }            
            
            if ( planetEnvironment == null )
                planetEnvironment = data.getTerrain(0);

            try{
            	advTerrainNumber = Integer.parseInt(advTerrain);
            	planetWeather = data.getAdvancedTerrain(advTerrainNumber);
            } catch (Exception ex) {
            	MWLogger.mainLog("advTerrain is " + advTerrain);                	

            	planetWeather = data.getAdvancedTerrainByName(advTerrain);
            }
            
            if (planetWeather == null)
            	planetWeather  = data.getAdvancedTerrain(0);
            
            Continent PE = new Continent(size, planetEnvironment, planetWeather);                       
            getEnvironments().add(PE);
        }

        setDescription(TokenReader.readString(ST));

        this.setBaysProvided(TokenReader.readInt(ST));

        setConquerable(TokenReader.readBoolean(ST));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        try {
            setTimestamp(sdf.parse(TokenReader.readString(ST)));
        } catch (Exception ex) {
            // No biggy, but will cause senseless Data transfer, so:
            MWLogger.errLog("The following excepion on planet " + getName() + " is not critical, but will cause useless bandwith usage: please fix!");
            MWLogger.errLog(ex);
            setTimestamp(new Date(System.currentTimeMillis()));
        }

        int id = TokenReader.readInt(ST);
        if (id == -1) {
        	id = CampaignData.cd.getUnusedPlanetID();
        }
        setId(id);
       setMinPlanetOwnerShip(TokenReader.readInt(ST));

        setHomeWorld(TokenReader.readBoolean(ST));

        setOriginalOwner(TokenReader.readString(ST));

        StringTokenizer str = new StringTokenizer(TokenReader.readString(ST), "^");
        TreeMap<String, String> map = new TreeMap<String, String>();
        while (str.hasMoreTokens()) {
            String key = TokenReader.readString(str);
            if (CampaignMain.cm.getData().getPlanetOpFlags().containsKey(key))
                map.put(key, CampaignMain.cm.getData().getPlanetOpFlags().get(key));
        }
        this.setPlanetFlags(map);

        this.setConquestPoints(TokenReader.readInt(ST));

        updateInfluences();

        if (singleFaction) {

            if (isNullOwner()) {
                this.setConquestPoints(100);
                //this.setCompProduction(0);
                this.setBaysProvided(0);
                SHouse house = CampaignMain.cm.getHouseById(-1);
                this.getInfluence().moveInfluence(house, house, 100, 100);
            }
        }

        setOwner(null, checkOwner(), false);

        return s;
    }

    /**
     * Use the other constructor as soon as you do not need the manual serialization support through fromString() anymore.
     */
    public SPlanet() {
        // super(CampaignMain.cm.getData().getUnusedPlanetID(),"", new
        // Position(0,0), null);
        super();
        setTimestamp(new Date(0));
        setOriginalOwner(CampaignMain.cm.getConfig("NewbieHouseName"));
    }

    public SPlanet(int id, String name, Influences flu, int income, int CompProd, double xcood, double ycood) {
        super(id, name, new Position(xcood, ycood), flu);
        setCompProduction(CompProd);
        setTimestamp(new Date(0));
        setOriginalOwner(CampaignMain.cm.getConfig("NewbieHouseName"));
    }

    public SUnitFactory getRandomUnitFactory() {
        if (getUnitFactories().size() == 0)
            return null;
        // else
        return (SUnitFactory) getUnitFactories().get(CampaignMain.cm.getRandomNumber(getUnitFactories().size()));
    }

    public SUnitFactory getBestUnitFactory() {
        if (getUnitFactories().size() == 0)
            return null;
        SUnitFactory result = null;
        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            if (result == null)
                result = MF;
            else {
                if (MF.getWeightclass() > result.getWeightclass()) {
                    result = MF;
                } else if (MF.getWeightclass() == result.getWeightclass()) {
                    if (MF.getBestTypeProducable() < result.getBestTypeProducable())
                        result = MF;
                }
            }
        }
        return result;
    }

    public Vector<SUnitFactory> getFactoriesByName(String s) {
        Vector<SUnitFactory> result = new Vector<SUnitFactory>(getUnitFactories().size(), 1);
        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            if (MF.getName().equals(s))
                result.add(MF);
        }
        return result;
    }

    public Vector<SUnitFactory> getFactoriesOfWeighclass(int weightclass) {
        Vector<SUnitFactory> result = new Vector<SUnitFactory>(getUnitFactories().size(), 1);
        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            if (MF.getWeightclass() == weightclass)
                result.add(MF);
        }
        return result;
    }

    /**
     * @param Attacker -
     *            attacking faction
     * @return potential defending houses (ie - those with territory on the world)
     */
    public Vector<House> getDefenders(SHouse Attacker) {
        Vector<House> result = new Vector<House>(getInfluence().getHouses());
        result.trimToSize();
        /*
         * Iterator it = getInfluence().getHouses().iterator(); while (it.hasNext()) { SHouse h = (SHouse) it.next(); //if (!h.equals(Attacker) || Attacker.isInHouseAttacks()) result.add(h); }
         */
        return result;
    }

    @Override
    public boolean equals(Object o) {

        SPlanet p = null;
        try {
            p = (SPlanet) o;
        } catch (ClassCastException e) {
            return false;
        }

        if (o == null)
            return false;

        return p.getId() == this.getId();
    }

    /**
     * Do a tick - call tick on he planets MF, if it has one, and return the amount of income generated by the planet Income = base income * the number of miniticks registered at a tick
     */
    public String tick(int refreshminiticks) {
    	MWLogger.debugLog("Ticking " + getName() + " checking for factories and adding " + refreshminiticks + " miniticks.");
        // Tick all Factories
        StringBuilder hsUpdates = new StringBuilder();
        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            int total = 0;
            if (MF.canProduce(Unit.MEK))
                total += refreshminiticks;
            if (MF.canProduce(Unit.VEHICLE))
                total += refreshminiticks;
            if (MF.canProduce(Unit.INFANTRY))
                total += refreshminiticks;
            if (MF.canProduce(Unit.PROTOMEK))
                total += refreshminiticks;
            if (MF.canProduce(Unit.BATTLEARMOR))
                total += refreshminiticks;
            if (MF.canProduce(Unit.AERO))
                total += refreshminiticks;
            hsUpdates.append(MF.addRefresh(-total, false));
        }
        return hsUpdates.toString();
    }

    public String getSmallStatus(boolean useHTML) {

        StringBuilder result = new StringBuilder();
        if (useHTML)
            result.append(this.getNameAsColoredLink());
        else
            result.append(getName());

        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            result.append(" [" + MF.getSize() + "," + MF.getFounder() + "," + MF.getTypeString() + "]");
        }

        result.append(":");
        for (House h : getInfluence().getHouses()) {
            result.append(h.getName() + "(" + getInfluence().getInfluence(h.getId()) + "cp)");
            result.append(", ");

        }
        if (useHTML)
            result.replace(result.length() - 2, result.length(), "<br>");
        return result.toString();
    }

    public SHouse checkOwner() {

        if (getInfluence() == null) {
            MWLogger.errLog("getINF == null Planet: " + getName());
            return null;
        }

        SHouse h = null;
        Integer houseID = this.getInfluence().getOwner();

        if (houseID == null)
            return null;

        h = (SHouse) CampaignMain.cm.getData().getHouse(houseID);

        if (this.getInfluence().getInfluence(houseID) < this.getMinPlanetOwnerShip())
            return null;

        return h;
    }

    public SHouse getOwner() {
        /*
         * Null owner is possible, but should be uncommon. Check the owner again to make sure the this is true before returning.
         */
        if (owner == null)
            checkOwner();
        return owner;
    }

    public void setOwner(SHouse oldOwner, SHouse newOwner, boolean sendHouseUpdates) {

        if (owner != null)// this is the same as oldowner in most cases
            owner.removePlanet(this);

        if (newOwner != null) {
            owner = newOwner;
            owner.addPlanet(this);
        }

        if (sendHouseUpdates)
            this.sendHouseStatusUpdate(oldOwner, newOwner);
    }

    public int doGainInfluence(SHouse winner, SHouse loser, int amount, boolean adminExchange) {

        if (!winner.isConquerable() && !adminExchange)
            return 0;

        int infgain = getInfluence().moveInfluence(winner, loser, amount, this.getConquestPoints());
        // dont bother with updates if land has not changed hands.
        if (infgain > 0) {

            // winner.updated();
            // loser.updated();
            this.updated();

            SHouse oldOwner = owner;
            SHouse newOwner = checkOwner();
            setOwner(oldOwner, newOwner, true);
        }
        return infgain;
    }

    /*
     * Helper method that sends updates to online players when a world changes hands.
     */
    private void sendHouseStatusUpdate(SHouse oldOwner, SHouse newOwner) {

        // don't do anything if there's no change is ownership
        if (oldOwner != null && oldOwner.equals(newOwner))
            return;
        else if (oldOwner == null && newOwner == null)
            return;

        // if the world has factories, build strings to send
        StringBuilder oldOwnerHSUpdates = new StringBuilder();
        StringBuilder newOwnerHSUpdates = new StringBuilder();
        for (UnitFactory currUF : getUnitFactories()) {

            oldOwnerHSUpdates.append("RF|" + currUF.getWeightclass() + "$" + currUF.getType() + "$" + this.getName() + "$" + currUF.getName() + "|");

            newOwnerHSUpdates.append("AF|" + currUF.getWeightclass());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getType());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getFounder());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(this.getName());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getName());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getTicksUntilRefresh());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getAccessLevel());
            newOwnerHSUpdates.append("$");
            newOwnerHSUpdates.append(currUF.getID());
            newOwnerHSUpdates.append("|");
        }

        // send updates to non-null houses, so long as update strings have
        // length > 0 (real updates)
        if (oldOwner != null && oldOwnerHSUpdates.length() > 0)
            CampaignMain.cm.doSendToAllOnlinePlayers(oldOwner, "HS|" + oldOwnerHSUpdates.toString(), false);
        if (newOwner != null && newOwnerHSUpdates.length() > 0)
            CampaignMain.cm.doSendToAllOnlinePlayers(newOwner, "HS|" + newOwnerHSUpdates.toString(), false);

    }

    public String getShortDescription(boolean withTerrain) {
        StringBuilder result = new StringBuilder(getName());
        if (withTerrain) {
            Continent p = getEnvironments().getBiggestEnvironment();
            Terrain pe = p.getEnvironment();
            AdvancedTerrain ape = p.getAdvancedTerrain();
            if (pe != null && pe.getEnvironments().size() > 0) {
                result.append(" " + pe.getEnvironments().get(0).toImageDescription());
                result.append(" " + pe.getEnvironments().get(0).getName());
            }
            if (ape != null )
                result.append(" " + ape.WeatherForcast());
            	

            if (this.getUnitFactories().size() > 0) {
                for (int i = 0; i < this.getUnitFactories().size(); i++) {
                    SUnitFactory MF = ((SUnitFactory) this.getUnitFactories().get(i));
                    result.append(MF.getIcons());
                }
            }
            if (pe != null && getEnvironments().getTotalEnivronmentPropabilities() > 0)
                result.append(" (" + Math.round((double) p.getSize() * 100 / getEnvironments().getTotalEnivronmentPropabilities()) + "% correct)");
            else
                result.append(" (100% correct)");
        }
        return result.toString();
    }

    /**
     * Method which returns a coloured link name for a planet.
     */
    public String getNameAsColoredLink() {

        String colorString = "";
        if (owner == null) {
            colorString = CampaignMain.cm.getConfig("DisputedPlanetColor");// malformed
            // gets
            // you
            // black?
        } else
            colorString = owner.getHouseColor();

        String toReturn = "<font color=\"" + colorString + "\">" + getNameAsLink() + "</font>";
        return toReturn;
    }

    @Override
    public int getMinPlanetOwnerShip() {

        int ownership = super.getMinPlanetOwnerShip();
        if (ownership < 0)
            ownership = CampaignMain.cm.getIntegerConfig("MinPlanetOwnerShip");

        return ownership;
    }

    public boolean isNullOwner() {

        if (this.getInfluence().getInfluence(-1) == this.getConquestPoints())
            return true;

        return false;
    }

}