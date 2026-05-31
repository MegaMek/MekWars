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

package mekwars.server.campaign;

import java.io.Serial;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import jakarta.annotation.Nullable;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.AdvancedTerrain;
import mekwars.common.CampaignData;
import mekwars.common.Continent;
import mekwars.common.House;
import mekwars.common.Influences;
import mekwars.common.Terrain;
import mekwars.common.Unit;
import mekwars.common.UnitFactory;
import mekwars.common.util.Position;
import mekwars.common.util.TokenReader;
import mekwars.server.campaign.data.TimeUpdatePlanet;
import mekwars.server.campaign.util.SerializedMessage;

public class SPlanet extends TimeUpdatePlanet implements Serializable, Comparable<Object> {
    private static final MMLogger LOGGER = MMLogger.create(SPlanet.class);

    @Serial
    private static final long serialVersionUID = -2266871107987235842L;
    private SHouse owner = null;

    /**
     * Use the other constructor as soon as you do not need the manual serialization support through fromString()
     * anymore.
     */
    public SPlanet() {
        super();
        setTimestamp(new Date(0));
        setOriginalOwner(CampaignMain.campaignMain.getConfig("NewbieHouseName"));
    }

    public SPlanet(int id, String name, Influences flu, int income, int CompProd, double xCord, double yCord) {
        super(id, name, new Position(xCord, yCord), flu);
        setCompProduction(CompProd);
        setTimestamp(new Date(0));
        setOriginalOwner(CampaignMain.campaignMain.getConfig("NewbieHouseName"));
    }

    /**
     *
     */
    public String fromString(String string, Random random, CampaignData data) {
        // debug

        boolean singleFaction = CampaignMain.campaignMain.getBooleanConfig("AllowSinglePlayerFactions");
        LOGGER.info(string);
        string = string.substring(3);
        StringTokenizer stringTokenizer = new StringTokenizer(string, "#");
        setName(TokenReader.readString(stringTokenizer));
        setCompProduction(TokenReader.readInt(stringTokenizer));
        // Read Factories
        int hasMF = TokenReader.readInt(stringTokenizer);
        for (int i = 0; i < hasMF; i++) {
            SUnitFactory sUnitFactory = new SUnitFactory();
            sUnitFactory.fromString(TokenReader.readString(stringTokenizer), this, random);

            if (singleFaction &&
                      CampaignMain.campaignMain.getHouseFromPartialString(sUnitFactory.getFounder()) == null) {
                continue;
            }
            getUnitFactories().add(sUnitFactory);
        }

        setPosition(new Position(TokenReader.readDouble(stringTokenizer), TokenReader.readDouble(stringTokenizer)));

        try {
            HashMap<Integer, Integer> influence = new HashMap<>();
            {
                StringTokenizer influences = new StringTokenizer(TokenReader.readString(stringTokenizer), "$");

                while (influences.hasMoreElements()) {
                    String HouseName = TokenReader.readString(influences);
                    SHouse h = (SHouse) data.getHouseByName(HouseName);
                    int HouseInf = TokenReader.readInt(influences);
                    if (h != null) {
                        influence.put(h.getId(), HouseInf);
                    } else {
                        LOGGER.debug("House not found: {}", HouseName);
                    }
                }
            }
            setInfluence(new Influences(influence));
        } catch (RuntimeException ex) {
            LOGGER.error(ex, "Problem on Planet: {}", this.getName());
        }

        int Envs = TokenReader.readInt(stringTokenizer);

        for (int i = 0; i < Envs; i++) {
            int size = TokenReader.readInt(stringTokenizer);
            String terrain = TokenReader.readString(stringTokenizer);
            String advTerrain = TokenReader.readString(stringTokenizer);

            int terrainNumber;
            int advTerrainNumber;
            Terrain planetEnvironment;
            AdvancedTerrain planetWeather;
            /*
             * Bug reported if you screw with the positions of the terrains in terrain.xml you'll screw up the planet terrains this will now allow you to load via int and then save via name so the terrain will always be correct no matter the position of the terrain in the terrain.xml.
             */
            try {
                terrainNumber = MathUtility.parseInt(terrain, 0);
                planetEnvironment = data.getTerrain(terrainNumber);
            } catch (Exception ex) {
                planetEnvironment = data.getTerrainByName(terrain);
            }

            if (planetEnvironment == null) {
                planetEnvironment = data.getTerrain(0);
            }

            try {
                advTerrainNumber = MathUtility.parseInt(advTerrain, 0);
                planetWeather = data.getAdvancedTerrain(advTerrainNumber);
            } catch (Exception ex) {
                LOGGER.debug(ex, "advTerrain is {}", advTerrain);

                planetWeather = data.getAdvancedTerrainByName(advTerrain);
            }

            if (planetWeather == null) {
                planetWeather = data.getAdvancedTerrain(0);
            }

            Continent continent = new Continent(size, planetEnvironment, planetWeather);
            getEnvironments().add(continent);
        }

        setDescription(TokenReader.readString(stringTokenizer));

        this.setBaysProvided(TokenReader.readInt(stringTokenizer));

        setConquerable(TokenReader.readBoolean(stringTokenizer));

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

        try {
            setTimestamp(simpleDateFormat.parse(TokenReader.readString(stringTokenizer)));
        } catch (Exception ex) {
            LOGGER.error(ex,
                  STR."The following excepion on planet \{getName()} is not critical, but will cause useless bandwith usage: please fix!");
            setTimestamp(new Date(System.currentTimeMillis()));
        }

        int id = TokenReader.readInt(stringTokenizer);

        if (id == -1) {
            id = CampaignData.cd.getUnusedPlanetID();
        }

        setId(id);
        setMinPlanetOwnerShip(TokenReader.readInt(stringTokenizer));

        setHomeWorld(TokenReader.readBoolean(stringTokenizer));

        setOriginalOwner(TokenReader.readString(stringTokenizer));

        StringTokenizer carrotStringTokenizer = new StringTokenizer(TokenReader.readString(stringTokenizer), "^");
        TreeMap<String, String> map = new TreeMap<>();

        while (carrotStringTokenizer.hasMoreTokens()) {
            String key = TokenReader.readString(carrotStringTokenizer);

            if (CampaignMain.campaignMain.getData().getPlanetOpFlags().containsKey(key)) {
                map.put(key, CampaignMain.campaignMain.getData().getPlanetOpFlags().get(key));
            }
        }

        this.setPlanetFlags(map);

        this.setConquestPoints(TokenReader.readInt(stringTokenizer));

        updateInfluences();

        if (singleFaction) {
            if (isNullOwner()) {
                this.setConquestPoints(100);
                this.setBaysProvided(0);
                SHouse house = CampaignMain.campaignMain.getHouseById(-1);
                this.getInfluence().moveInfluence(house, house, 100, 100);
            }
        }

        setOwner(null, checkOwner(), false);

        return string;
    }

    public boolean isNullOwner() {
        return this.getInfluence().getInfluence(-1) == this.getConquestPoints();
    }

    public void setOwner(SHouse oldOwner, SHouse newOwner, boolean sendHouseUpdates) {
        if (owner != null) {
            owner.removePlanet(this);
        }

        if (newOwner != null) {
            owner = newOwner;
            owner.addPlanet(this);
        }

        if (sendHouseUpdates) {
            this.sendHouseStatusUpdate(oldOwner, newOwner);
        }
    }

    public @Nullable SHouse checkOwner() {
        if (getInfluence() == null) {
            LOGGER.debug("getINF == null Planet: {}", getName());
            return null;
        }

        SHouse sHouse;
        Integer houseID = this.getInfluence().getOwner();

        if (houseID == null) {
            return null;
        }

        sHouse = (SHouse) CampaignMain.campaignMain.getData().getHouse(houseID);

        if (this.getInfluence().getInfluence(houseID) < this.getMinPlanetOwnerShip()) {
            return null;
        }

        return sHouse;
    }

    /*
     * Helper method that sends updates to online players when a world changes hands.
     */
    private void sendHouseStatusUpdate(SHouse oldOwner, SHouse newOwner) {

        // don't do anything if there's no change is ownership
        if (oldOwner != null && oldOwner.equals(newOwner)) {
            return;
        } else if (oldOwner == null && newOwner == null) {
            return;
        }

        // if the world has factories, build strings to send
        StringBuilder oldOwnerHSUpdates = new StringBuilder();
        StringBuilder newOwnerHSUpdates = new StringBuilder();
        for (UnitFactory currUF : getUnitFactories()) {
            oldOwnerHSUpdates.append("RF|")
                  .append(currUF.getWeightclass())
                  .append("$")
                  .append(currUF.getType())
                  .append("$")
                  .append(this.getName())
                  .append("$")
                  .append(currUF.getName())
                  .append("|");

            newOwnerHSUpdates.append("AF|").append(currUF.getWeightclass());
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
        if (oldOwner != null && !oldOwnerHSUpdates.isEmpty()) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(oldOwner,
                  STR."HS|\{oldOwnerHSUpdates.toString()}",
                  false);
        }
        if (newOwner != null && !newOwnerHSUpdates.isEmpty()) {
            CampaignMain.campaignMain.doSendToAllOnlinePlayers(newOwner,
                  STR."HS|\{newOwnerHSUpdates.toString()}",
                  false);
        }

    }

    public @Nullable SUnitFactory getRandomUnitFactory() {
        if (getUnitFactories().isEmpty()) {
            return null;
        }

        return (SUnitFactory) getUnitFactories().get(CampaignMain.campaignMain.getRandomNumber(getUnitFactories().size()));
    }

    public @Nullable SUnitFactory getBestUnitFactory() {
        if (getUnitFactories().isEmpty()) {
            return null;
        }

        SUnitFactory result = null;

        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory sUnitFactory = (SUnitFactory) getUnitFactories().get(i);

            if (result == null) {
                result = sUnitFactory;
            } else {
                if (sUnitFactory.getWeightclass() > result.getWeightclass()) {
                    result = sUnitFactory;
                } else if (sUnitFactory.getWeightclass() == result.getWeightclass()) {
                    if (sUnitFactory.getBestTypeProducable() < result.getBestTypeProducable()) {
                        result = sUnitFactory;
                    }
                }
            }
        }

        return result;
    }

    public Vector<SUnitFactory> getFactoriesByName(String string) {
        Vector<SUnitFactory> result = new Vector<>(getUnitFactories().size(), 1);

        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory sUnitFactory = (SUnitFactory) getUnitFactories().get(i);
            if (sUnitFactory.getName().equals(string)) {
                result.add(sUnitFactory);
            }
        }

        return result;
    }

    public Vector<SUnitFactory> getFactoriesOfWeightClass(int weightClass) {
        Vector<SUnitFactory> result = new Vector<>(getUnitFactories().size(), 1);

        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory sUnitFactory = (SUnitFactory) getUnitFactories().get(i);
            if (sUnitFactory.getWeightclass() == weightClass) {
                result.add(sUnitFactory);
            }
        }

        return result;
    }

    /**
     * @param Attacker - attacking faction
     *
     * @return potential defending houses (ie - those with territory on the world)
     */
    public Vector<House> getDefenders(SHouse Attacker) {
        Vector<House> result = new Vector<>(getInfluence().getHouses());
        result.trimToSize();
        return result;
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof SPlanet sPlanet) {
            return sPlanet.getId() == this.getId();
        }

        return false;
    }

    @Override
    public String toString() {
        SerializedMessage result = new SerializedMessage("#");
        result.append("PL");
        result.append(getName());
        result.append(getCompProduction());

        if (getUnitFactories() != null) {
            result.append(getUnitFactories().size());
            for (UnitFactory factory : getUnitFactories()) {
                result.append(factory.toString());
            }
        } else {
            result.append("0");
        }

        result.append(getPosition().getX());
        result.append(getPosition().getY());
        StringBuilder houseString = new StringBuilder();

        for (House house : getInfluence().getHouses()) {
            SHouse next = (SHouse) house;

            if (next == null) {
                continue;
            }

            houseString.append(next.getName());
            houseString.append("$"); // change for unusual influence
            houseString.append(getInfluence().getInfluence(next.getId()));
            houseString.append("$"); // change for unusual influence
        }
        // No Influences then set influence to NewbieHouse so the planet will
        // load.
        if (getInfluence().getHouses().isEmpty()) {
            houseString.append(CampaignMain.campaignMain.getConfig("NewbieHouseName"));
            houseString.append("$");
            houseString.append(this.getConquestPoints());
            houseString.append("$");
        }

        result.append(houseString.toString());
        result.append(getEnvironments().size());

        for (Continent continent : getEnvironments().toArray()) {
            result.append(continent.getSize());
            result.append(continent.getEnvironment().getName());

            if (continent.getAdvancedTerrain() != null) {
                if (continent.getAdvancedTerrain().getName() != null) {
                    result.append(continent.getAdvancedTerrain().getName());
                }
            }
        }

        if (getDescription().isEmpty()) {
            result.append(" ");
        } else {
            result.append(getDescription());
        }

        result.append(this.getBaysProvided());
        result.append(this.isConquerable());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        result.append(simpleDateFormat.format(this.getLastChanged()));
        result.append(this.getId());
        result.append(this.getMinPlanetOwnerShip());
        result.append(isHomeWorld());
        result.append(getOriginalOwner());

        if (!this.getPlanetFlags().isEmpty()) {
            for (String key : this.getPlanetFlags().keySet()) {
                result.append(STR."\{key}^");
            }
        } else {
            result.append("^^");
        }

        result.append(this.getConquestPoints());

        return result.toString();
    }

    @Override
    public int getMinPlanetOwnerShip() {
        int ownership = super.getMinPlanetOwnerShip();

        if (ownership < 0) {
            ownership = CampaignMain.campaignMain.getIntegerConfig("MinPlanetOwnerShip");
        }

        return ownership;
    }

    /**
     * Do a tick - call tick on the planet MF, if it has one, and return the amount of income generated by the planet
     * Income = base income * the number of mini ticks registered at a tick
     */
    public String tick(int refreshMiniTicks) {
        LOGGER.debug("Ticking {} checking for factories and adding {} miniticks.", getName(), refreshMiniTicks);
        // Tick all Factories
        StringBuilder hsUpdates = new StringBuilder();
        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory MF = (SUnitFactory) getUnitFactories().get(i);
            int total = 0;

            if (MF.canProduce(Unit.MEK)) {
                total += refreshMiniTicks;
            }

            if (MF.canProduce(Unit.VEHICLE)) {
                total += refreshMiniTicks;
            }

            if (MF.canProduce(Unit.INFANTRY)) {
                total += refreshMiniTicks;
            }

            if (MF.canProduce(Unit.PROTOMEK)) {
                total += refreshMiniTicks;
            }

            if (MF.canProduce(Unit.BATTLEARMOR)) {
                total += refreshMiniTicks;
            }

            if (MF.canProduce(Unit.AERO)) {
                total += refreshMiniTicks;
            }

            hsUpdates.append(MF.addRefresh(-total, false));
        }

        return hsUpdates.toString();
    }

    public String getSmallStatus(boolean useHTML) {
        StringBuilder result = new StringBuilder();

        if (useHTML) {
            result.append(this.getNameAsColoredLink());
        } else {
            result.append(getName());
        }

        for (int i = 0; i < getUnitFactories().size(); i++) {
            SUnitFactory sUnitFactory = (SUnitFactory) getUnitFactories().get(i);
            result.append(" [")
                  .append(sUnitFactory.getSize())
                  .append(",")
                  .append(sUnitFactory.getFounder())
                  .append(",")
                  .append(sUnitFactory.getTypeString())
                  .append("]");
        }

        result.append(":");
        for (House house : getInfluence().getHouses()) {
            result.append(house.getName()).append("(").append(getInfluence().getInfluence(house.getId())).append("cp)");
            result.append(", ");

        }
        if (useHTML) {
            result.replace(result.length() - 2, result.length(), "<br>");
        }

        return result.toString();
    }

    /**
     * Method that returns a colored link name for a planet.
     */
    public String getNameAsColoredLink() {
        String colorString = "";
        if (owner == null) {
            colorString = CampaignMain.campaignMain.getConfig("DisputedPlanetColor");
        } else {
            colorString = owner.getHouseColor();
        }

        return STR."<font color=\"\{colorString}\">\{getNameAsLink()}</font>";
    }

    public @Nullable SHouse getOwner() {
        /*
         * Null owner is possible but should be uncommon. Check the owner again to make sure the is true before returning.
         */
        if (owner == null) {
            checkOwner();
        }

        return owner;
    }

    public int doGainInfluence(SHouse winner, SHouse loser, int amount, boolean adminExchange) {
        if (!winner.isConquerable() && !adminExchange) {
            return 0;
        }

        int influenceGain = getInfluence().moveInfluence(winner, loser, amount, this.getConquestPoints());
        // dont bother with updates if land has not changed hands.
        if (influenceGain > 0) {
            this.updated();

            SHouse oldOwner = owner;
            SHouse newOwner = checkOwner();
            setOwner(oldOwner, newOwner, true);
        }

        return influenceGain;
    }

    public String getShortDescription(boolean withTerrain) {
        StringBuilder result = new StringBuilder(getName());
        if (withTerrain) {
            Continent biggestEnvironment = getEnvironments().getBiggestEnvironment();
            Terrain environment = biggestEnvironment.getEnvironment();
            AdvancedTerrain ape = biggestEnvironment.getAdvancedTerrain();

            if (environment != null && !environment.getEnvironments().isEmpty()) {
                result.append(" ").append(environment.getEnvironments().getFirst().toImageDescription());
                result.append(" ").append(environment.getEnvironments().getFirst().getName());
            }

            if (ape != null) {
                result.append(" ").append(ape.WeatherForecast());
            }


            if (!this.getUnitFactories().isEmpty()) {
                for (int i = 0; i < this.getUnitFactories().size(); i++) {
                    SUnitFactory MF = ((SUnitFactory) this.getUnitFactories().get(i));
                    result.append(MF.getIcons());
                }
            }

            if (environment != null && getEnvironments().getTotalEnvironmentProbabilities() > 0) {
                result.append(" (")
                      .append(Math.round((double) biggestEnvironment.getSize() * 100 /
                                               getEnvironments().getTotalEnvironmentProbabilities()))
                      .append("% correct)");
            } else {
                result.append(" (100% correct)");
            }
        }

        return result.toString();
    }

}
