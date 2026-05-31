/*
 * Copyright (C) 2004 MekWars
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.common.equipment.AmmoType;
import megamek.logging.MMLogger;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;

/**
 * TODO: It seems, that all operations done here are needed independent of the semantic of the underlying structure.
 * Planets are handled equal to factions and each function is doubled. If this is true, it should be managed in an
 * generic way to reduce code bloat and code replication.
 * <p>
 * Campaign is the base of the data holding classes for client and server. Here
 * all campaign relevant information as Houses, Planets and Player data is
 * stored.
 * <p>
 * In this base class some methods are provided to retrieve these informations
 * to use in common data classes like House or Planet when referring to
 * resources.
 * <p>
 * Notice: Please read the doc to binOut before adding new data types.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CampaignData implements TerrainProvider {
    private final static MMLogger LOGGER = MMLogger.create(CampaignData.class);

    public static CampaignData cd;

    /**
     * All different Houses are stored here. key=Integer (id), value=House
     */
    private final TreeMap<Integer, House> factions = new TreeMap<>();

    /**
     * All different House ids are stored here key=String (name), value=int id
     */
    private final TreeMap<String, Integer> factionID = new TreeMap<>();

    /**
     * This is a list with all planet information stored. key=Integer (id), value=Planet (or subclasses for server and
     * client)
     */
    private final TreeMap<Integer, Planet> planets = new TreeMap<>();

    /**
     * This is a list with planet id stored. key=String (name), value=int id
     */
    private final TreeMap<String, Integer> planetID = new TreeMap<>();

    /**
     * List of all terrains that can occur on surfaces of planets.
     */
    private final ArrayList<Terrain> terrains = new ArrayList<>();
    private final ArrayList<AdvancedTerrain> advTerrains = new ArrayList<>();
    private final TreeMap<String, String> planetOpFlags = new TreeMap<>();
    private EnumSet<AmmoType.Munitions> serverBannedAmmo = EnumSet.noneOf(AmmoType.Munitions.class);
    private Vector<Integer> bannedTargetingSystems = new Vector<>();
    private Hashtable<String, Integer> commands = new Hashtable<>();
    private Properties serverConfigs = new Properties();

    /**
     * Create empty campaign data.
     */
    public CampaignData() {
        cd = this;
        PlanetEnvironments.data = this;
    }

    /**
     * Generate the campaign data from an binary stream.
     */
    public CampaignData(@Nonnull BinReader in) throws IOException {
        cd = this;
        PlanetEnvironments.data = this;
        int size = in.readInt("terrains.size");

        for (int i = 0; i < size; ++i) {
            Terrain terrain = new Terrain();
            terrain.binIn(in, this);
            addTerrain(terrain);
        }

        int advTerrainSize = in.readInt("advTerrains.size");

        for (int i = 0; i < advTerrainSize; ++i) {
            AdvancedTerrain advancedTerrain = new AdvancedTerrain();
            advancedTerrain.binIn(in);
            addAdvancedTerrain(advancedTerrain);
        }

        size = in.readInt("factions.size");

        for (int i = 0; i < size; ++i) {
            addHouse(new House(in));
        }

        size = in.readInt("planets.size");

        for (int i = 0; i < size; ++i) {
            addPlanet(new Planet(in, factions, this));
        }
    }

    /**
     * @see TerrainProvider#addAdvancedTerrain(AdvancedTerrain)
     */
    public void addTerrain(@Nonnull Terrain terrain) {
        terrain.setId(getUnusedTerrainID());
        terrains.add(terrain);
        terrains.trimToSize();
    }

    /**
     * Adds a faction to the campaign storage. If it was already within the storage, it replaces the old object.
     *
     * @param faction The faction to hold.
     *                <p>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      TODO You should use XStream to initialize CampaignData
     */
    public void addHouse(@Nonnull House faction) {
        if (faction.getId() == -1 && !faction.getName().equalsIgnoreCase("None")) {
            faction.setId(getUnusedHouseID());
        }

        factions.put(faction.getId(), faction);
        factionID.put(faction.getName().toLowerCase(), faction.getId());
    }

    /**
     * Adds a planet to the campaign storage. If it was already within the storage, it replaces the old object.
     *
     * @param planet The planet to hold.
     *               <p>
     *               see You should use XStream to initialize CampaignData
     */
    public void addPlanet(@Nonnull Planet planet) {
        if (planet.getId() == -1) {
            planet.setId(getUnusedPlanetID());
        }

        planets.put(planet.getId(), planet);
        planetID.put(planet.getName().toLowerCase(), planet.getId());
    }

    /**
     * Retrieve an unused id for terrains. Only used upon start up of a new server using XML files.
     *
     * @return An terrain id not used yet.
     */
    public int getUnusedTerrainID() {
        int id = -1;
        int hid;

        for (Terrain terrain : terrains) {
            hid = terrain.getId();
            if (hid > id) {
                id = hid;
            }
        }
        id++;

        return id;
    }

    /**
     * Retrieve an unused id for adv terrains. Only used upon start up of a new server using XML files.
     *
     * @return An terrain id not used yet.
     */
    public int getUnusedAdvTerrainID() {
        int id = -1;
        int hid = -1;
        for (AdvancedTerrain e : advTerrains) {
            hid = e.getId();
            if (hid > id) {
                id = hid;
            }
        }
        id++;

        return id;
    }

    /**
     * Retrieve an unused id for factions.
     *
     * @return An House id not used yet.
     *       <p>
     *                                                                                                                                                                                                                                                                                                                                                             TODO There should be no need for such function, since ID's should extracted from resource files. This
     *                                                                                                                                                                                                                                                                                                                                                                   function will vanish if ids are part of the resource.
     */
    public int getUnusedHouseID() {
        int id = -1;
        int hid;
        for (House e : factions.values()) {
            hid = e.getId();
            if (hid > id) {
                id = hid;
            }
        }
        id++;

        return id;
    }

    /**
     * Retrieve an unused id for planets.
     *
     * @return An Planet id not used yet.
     *       <p>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           TODO There should be no need for such function, since ID's should extracted from resource files. This
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 function will vanish if ids are part of the resource.
     */
    public int getUnusedPlanetID() {
        int id = 0;

        while (planets.containsKey(id)) {
            id++;
        }

        return id;
    }

    /**
     * @param factory Updates the client side factories Useful for the factory Refresh with RP
     *
     * @author Torren (Jason Tighe)
     */
    public void updateFactoryTick(String planetString, String factory, int tick) {
        Planet planet = getPlanetByName(planetString);

        if (planet != null) {
            UnitFactory unitFactory = getFactoryByName(planet, factory);

            if (unitFactory != null) {
                unitFactory.setTicksUntilRefresh(tick);
            }
        }
    }

    /**
     * Retrieve a planet by its name. Please try to use planet Id's when lookup for a planet instead (if you have the
     * choice).
     */
    public @Nullable Planet getPlanetByName(String name) {
        try {
            Integer planetID = this.planetID.get(name.toLowerCase());
            return getPlanet(planetID);
        } catch (Exception ex) {
            LOGGER.debug("Planet not found: {}", name);
            return null;
        }
    }

    /**
     * @author jtighe Retrieve a factory by its name.
     *
     */
    public @Nullable UnitFactory getFactoryByName(Planet planet, String name) {
        for (UnitFactory unitFactory : planet.getUnitFactories()) {
            if (unitFactory.getName().equalsIgnoreCase(name)) {
                return unitFactory;
            }
        }

        return null;
    }

    /**
     * Retrieve a specific planet.
     *
     * @param id The id of the planet.
     *
     * @return The requested Planet. This is usually a subclass of Planet.
     */
    public Planet getPlanet(int id) {
        return planets.get(id);
    }

    /**
     * Check if the planet name was only partial and complete it..
     */
    public @Nullable Planet getPlanetByPartialName(String name) {
        for (Planet planet : getAllPlanets()) {
            if (planet.getName().equals(name)) {
                return planet;
            }

            if (planet.getName().contains(name)) {
                return planet;
            }
        }

        return null;
    }

    /**
     * Retrieves all planets.
     */
    public Collection<Planet> getAllPlanets() {
        return planets.values();
    }

    /**
     * BUMM - Blow up a planet.
     *
     * @param id The id of the blown-up planet.
     */
    public void removePlanet(int id) {
        planetID.remove(getPlanet(id).getName().toLowerCase());
        planets.remove(id);
    }

    /**
     * Remove all planets.
     */
    public void clearPlanets() {
        planets.clear();
    }

    /**
     * Retrieves all factions.
     */
    public Collection<House> getAllHouses() {
        return factions.values();
    }

    /**
     * Remove a house from the server this is normally only for single faction servers
     *
     */
    public void removeHouse(int id) {
        String factionName = getHouse(id).getName().toLowerCase();
        factionID.remove(factionName);
        factions.remove(id);

        File factionFile = new File(STR."./campaign/factions/\{factionName}.dat");
        if (factionFile.exists()) {
            factionFile.delete();
        }

        factionFile = new File(STR."./campaign/factions/\{factionName}.bak");
        if (factionFile.exists()) {
            factionFile.delete();
        }
    }

    /**
     * Retrieve a specific faction.
     *
     * @param id The id of the House.
     *
     * @return The requested faction.
     */
    public House getHouse(int id) {
        return factions.get(id);
    }

    /**
     * Retrieve a faction by its name.
     * <p>
     * TODO This seems to be only needed, because some serialization work with transmitting the factions name
     *       instead of its id.
     */
    public @Nullable House getHouseByName(String name) {
        try {
            return getHouse(factionID.get(name.toLowerCase()));
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Remove all factions.
     */
    public void clearHouses() {
        factions.clear();
    }

    /**
     * Since I have no idea how TinyXML is operating and since McWizard does not allow me to use my loved JDom and
     * finally, since Enkel does not like XML-Transfer anyway, I use this to encode/decode the whole object.. (Imi)
     * <p>
     * There is another aspect of binOut to keep in mind. Since a MD5 hash is build after each differential update to
     * keep the data in sync, this function has to provide THE SAME output each time it is run, regardless of the
     * underlying virtual machine. Currently this is done by only using container classes, that remain the elements in a
     * stable order. If you need to add a container with unstable order (as Hash*), you have to make sure, the data is
     * ordered before writing it out with binOut.
     * <p>
     * TODO: check http://jira.codehaus.org/secure/ViewIssue.jspa?key=XSTR-27 to
     * see whether a better way of serialization is available ;-)
     */
    public void binOut(BinWriter out) throws IOException {
        binTerrainsOut(out);
        binHousesOut(out);
        binPlanetsOut(out);
    }

    /**
     * Outputs all terrains
     *
     * @see CampaignData#binOut(BinWriter)
     */
    public void binTerrainsOut(BinWriter out) throws IOException {
        out.println(terrains.size(), "terrains.size");

        for (Terrain terrain : terrains) {
            terrain.binOut(out);
        }

        out.println(advTerrains.size(), "advTerrains.size");

        for (AdvancedTerrain advancedTerrain : advTerrains) {
            advancedTerrain.binOut(out);
        }
    }

    /**
     * Outputs all factions
     *
     * @see CampaignData#binOut(BinWriter)
     */
    public void binHousesOut(BinWriter out) {
        out.println(factions.size(), "factions.size");

        for (House house : factions.values()) {
            house.binOut(out);
        }
    }

    /**
     * Outputs all planets
     *
     * @see CampaignData#binOut(BinWriter)
     */
    public void binPlanetsOut(BinWriter out) {
        out.println(planets.size(), "planets.size");
        for (Planet planet : planets.values()) {
            planet.binOut(out);
        }
    }

    /**
     * Outputs updated houses
     *
     * @see CampaignData#binOut(BinWriter)
     */
    public void binHousesOut(ArrayList<House> houses, BinWriter out) {
        out.println(houses.size(), "houses.size");
        for (House house : houses) {
            house.binOut(out);
        }
    }

    /**
     * Outputs all planets
     *
     * @see CampaignData#binOut(BinWriter)
     */
    public void binPlanetsOut(ArrayList<Planet> planets, BinWriter out) {
        out.println(planets.size(), "planets.size");
        for (Planet planet : planets) {
            planet.binOut(out);
        }
    }

    /**
     * Updates sent Planets due a differential update.
     *
     * @param changesSinceLastRefresh A map to hold the change in planet ids that got updated this refresh. Structure is
     *                                as follows: key=planetID(Integer), value=Influences(differential)
     */
    public void decodeMutablePlanets(BinReader in, Map<Integer, Influences> changesSinceLastRefresh)
          throws IOException {
        int count = in.readInt("mutableplanetsize");
        LOGGER.info("Retrieving {} planets due differential update.", count);
        changesSinceLastRefresh.clear();

        for (int i = 0; i < count; ++i) {
            int id = in.readInt("planetID");
            Influences infOld = new Influences(getPlanet(id).getInfluence());
            getPlanet(id).decodeMutableFields(in, this);
            Influences infNew = getPlanet(id).getInfluence();
            changesSinceLastRefresh.put(id, infNew.difference(infOld));
        }
    }

    /**
     * Writes some planets due a differential update
     *
     * @param ids A collection of java.lang.Integer with the ids to send.
     */
    public void encodeMutablePlanets(BinWriter out, Collection<Integer> ids) {
        out.println(ids.size(), "mutableplanetsize");
        for (Integer id : ids) {
            out.println(id, "planetID");
            getPlanet(id).encodeMutableFields(out, this);
        }
    }

    /**
     * @see TerrainProvider#getTerrain(int)
     */
    public @Nullable Terrain getTerrain(int id) {

        for (Terrain env : terrains) {
            if (env.getId() == id) {
                return env;
            }
        }

        return null;

    }

    /**
     * @see TerrainProvider#getAllTerrains()
     */
    public Collection<Terrain> getAllTerrains() {
        return terrains;
    }

    /**
     * @see TerrainProvider#addAdvancedTerrain(AdvancedTerrain)
     */
    public void addAdvancedTerrain(AdvancedTerrain newAdvTerrain) {
        newAdvTerrain.setId(getUnusedAdvTerrainID());
        advTerrains.add(newAdvTerrain);
        advTerrains.trimToSize();
    }

    /*adding the advanced terrain to the campaign data*/

    /**
     * @see TerrainProvider#getAdvancedTerrain(int)
     */
    public AdvancedTerrain getAdvancedTerrain(int id) {
        for (AdvancedTerrain env : advTerrains) {
            if (env.getId() == id) {
                return env;
            }
        }

        return new AdvancedTerrain();

    }

    /**
     * @see TerrainProvider#getAllTerrains()
     */
    public Collection<AdvancedTerrain> getAllAdvancedTerrains() {
        return advTerrains;
    }

    public @Nullable Terrain getTerrainByName(String TerrainName) {
        for (Terrain env : terrains) {
            if (env.getName().equalsIgnoreCase(TerrainName)) {
                return env;
            }
        }
        return null;
    }

    public AdvancedTerrain getAdvancedTerrainByName(String AdvTerrainName) {
        for (AdvancedTerrain env : advTerrains) {
            if (env.getName().equalsIgnoreCase(AdvTerrainName)) {
                return env;
            }
        }
        return new AdvancedTerrain();
    }

    /**
     * @return Hashtable
     *
     * @author Torren (Jason Tighe)
     *       <p>
     *       this returns a hashtable of all current MM munitions 06/10/05 using the Name of the munition as the key
     */
    public Hashtable<String, AmmoType.Munitions> getMunitionsByName() {
        Hashtable<String, AmmoType.Munitions> munitions = new Hashtable<>();

        munitions.put("Standard", AmmoType.Munitions.M_STANDARD);

        // AC Munition Types
        munitions.put("LBX Cluster", AmmoType.Munitions.M_CLUSTER);
        munitions.put("AC Armor Piercing", AmmoType.Munitions.M_ARMOR_PIERCING);
        munitions.put("AC Flechette", AmmoType.Munitions.M_FLECHETTE);
        munitions.put("AC Incendiary", AmmoType.Munitions.M_INCENDIARY_AC);
        munitions.put("AC Precision", AmmoType.Munitions.M_PRECISION);
        munitions.put("AC Tracer", AmmoType.Munitions.M_TRACER);

        // ATM Munition Types
        munitions.put("ATM Extended Range", AmmoType.Munitions.M_EXTENDED_RANGE);
        munitions.put("ATM High Explosive", AmmoType.Munitions.M_HIGH_EXPLOSIVE);

        // LRM & SRM Munition Types
        munitions.put("LRM/SRM Fragmentation", AmmoType.Munitions.M_FRAGMENTATION);
        munitions.put("LRM/SRM Listen Kill", AmmoType.Munitions.M_LISTEN_KILL);
        munitions.put("LRM/SRM Anti-TSM", AmmoType.Munitions.M_ANTI_TSM);
        munitions.put("LRM/SRM Narc", AmmoType.Munitions.M_NARC_CAPABLE);
        munitions.put("LRM/SRM Artemis", AmmoType.Munitions.M_ARTEMIS_CAPABLE);
        munitions.put("LRM/SRM Heat-Seeking", AmmoType.Munitions.M_HEAT_SEEKING);
        munitions.put("LRM/SRM Dead-Fire", AmmoType.Munitions.M_DEAD_FIRE);
        munitions.put("LRM/SRM Tandem-Charge", AmmoType.Munitions.M_TANDEM_CHARGE);

        // LRM Munition Types
        // Incendiary is special, though...
        munitions.put("LRM Incendiary", AmmoType.Munitions.M_INCENDIARY_LRM);
        munitions.put("LRM Flare", AmmoType.Munitions.M_FLARE);
        munitions.put("LRM SemiGuided", AmmoType.Munitions.M_SEMIGUIDED);
        munitions.put("LRM Swarm", AmmoType.Munitions.M_SWARM);
        munitions.put("LRM Swarm I", AmmoType.Munitions.M_SWARM_I);
        munitions.put("LRM Thunder", AmmoType.Munitions.M_THUNDER);
        munitions.put("LRM Thunder Augmented", AmmoType.Munitions.M_THUNDER_AUGMENTED);
        munitions.put("LRM Thunder Inferno", AmmoType.Munitions.M_THUNDER_INFERNO);
        munitions.put("LRM Thunder VibraBomb", AmmoType.Munitions.M_THUNDER_VIBRABOMB);
        munitions.put("LRM Thunder Active", AmmoType.Munitions.M_THUNDER_ACTIVE);
        munitions.put("LRM Follow The Leader", AmmoType.Munitions.M_FOLLOW_THE_LEADER);
        munitions.put("Multi Purpose", AmmoType.Munitions.M_MULTI_PURPOSE);

        // SRM Munition Types
        munitions.put("SRM Inferno", AmmoType.Munitions.M_INFERNO);
        munitions.put("SRM Acid", AmmoType.Munitions.M_AX_HEAD);

        // Torpedoes
        munitions.put("LRT/SRT", AmmoType.Munitions.M_TORPEDO);

        // iNarc Munition Types
        munitions.put("iNarc Explosive", AmmoType.Munitions.M_EXPLOSIVE);
        munitions.put("iNarc ECM", AmmoType.Munitions.M_ECM);
        munitions.put("iNarc HayWire", AmmoType.Munitions.M_HAYWIRE);
        munitions.put("iNarc Nemesis", AmmoType.Munitions.M_NEMESIS);

        // Narc Munition Types
        munitions.put("Narc Explosive", AmmoType.Munitions.M_NARC_EX);

        // Arrow IV Munition Types
        munitions.put("Arrow IV Homing", AmmoType.Munitions.M_HOMING);
        munitions.put("Arrow IV FASCAM", AmmoType.Munitions.M_FASCAM);
        munitions.put("Arrow IV Inferno", AmmoType.Munitions.M_INFERNO_IV);
        munitions.put("Arrow IV VibraBomb", AmmoType.Munitions.M_VIBRABOMB_IV);
        munitions.put("Arrow IV Smoke", AmmoType.Munitions.M_SMOKE);
        munitions.put("Arrow IV Davy Crockett", AmmoType.Munitions.M_DAVY_CROCKETT_M);
        return munitions;
    }

    /**
     * @return Hashtable
     *
     * @author Torren (Jason Tighe)
     *       <p>
     *       this returns a hashtable of all current MM munitions 06/10/05 using the Number of the munition as the key
     */
    public Hashtable<AmmoType.Munitions, String> getMunitionsByNumber() {
        Hashtable<AmmoType.Munitions, String> munitions = new Hashtable<>();

        munitions.put(AmmoType.Munitions.M_STANDARD, "Standard");

        // AC Munition Types
        munitions.put(AmmoType.Munitions.M_CLUSTER, "LBX Cluster");
        munitions.put(AmmoType.Munitions.M_ARMOR_PIERCING, "AC Armor Piercing");
        munitions.put(AmmoType.Munitions.M_FLECHETTE, "AC Flechette");
        munitions.put(AmmoType.Munitions.M_INCENDIARY_AC, "AC Incendiary");
        munitions.put(AmmoType.Munitions.M_PRECISION, "AC Precision");
        munitions.put(AmmoType.Munitions.M_TRACER, "AC Tracer");

        // ATM Munition Types
        munitions.put(AmmoType.Munitions.M_EXTENDED_RANGE, "ATM Extended Range");
        munitions.put(AmmoType.Munitions.M_HIGH_EXPLOSIVE, "ATM High Explosive");

        // LRM & SRM Munition Types
        munitions.put(AmmoType.Munitions.M_FRAGMENTATION, "LRM/SRM Fragmentation");
        munitions.put(AmmoType.Munitions.M_LISTEN_KILL, "LRM/SRM Listen Kill");
        munitions.put(AmmoType.Munitions.M_ANTI_TSM, "LRM/SRM Anti-TSM");
        munitions.put(AmmoType.Munitions.M_NARC_CAPABLE, "LRM/SRM Narc");
        munitions.put(AmmoType.Munitions.M_ARTEMIS_CAPABLE, "LRM/SRM Artemis");
        munitions.put(AmmoType.Munitions.M_HEAT_SEEKING, "LRM/SRM Heat-Seeking");
        munitions.put(AmmoType.Munitions.M_TANDEM_CHARGE, "LRM/SRM Tandem-Charge");
        munitions.put(AmmoType.Munitions.M_DEAD_FIRE, "LRM/SRM Dead-Fire");

        // LRM Munition Types
        // Incendiary is special though...
        munitions.put(AmmoType.Munitions.M_INCENDIARY_LRM, "LRM Incendiary");
        munitions.put(AmmoType.Munitions.M_FLARE, "LRM Flare");
        munitions.put(AmmoType.Munitions.M_SEMIGUIDED, "LRM SemiGuided");
        munitions.put(AmmoType.Munitions.M_SWARM, "LRM Swarm");
        munitions.put(AmmoType.Munitions.M_SWARM_I, "LRM Swarm I");
        munitions.put(AmmoType.Munitions.M_THUNDER, "LRM Thunder");
        munitions.put(AmmoType.Munitions.M_THUNDER_AUGMENTED, "LRM Thunder Augmented");
        munitions.put(AmmoType.Munitions.M_THUNDER_INFERNO, "LRM Thunder Inferno");
        munitions.put(AmmoType.Munitions.M_THUNDER_VIBRABOMB, "LRM Thunder VibraBomb");
        munitions.put(AmmoType.Munitions.M_THUNDER_ACTIVE, "LRM Thunder Active");
        munitions.put(AmmoType.Munitions.M_FOLLOW_THE_LEADER, "LRM Follow The Leader");
        munitions.put(AmmoType.Munitions.M_MULTI_PURPOSE, "Multi Purpose");

        // SRM Munition Types
        munitions.put(AmmoType.Munitions.M_INFERNO, "SRM Inferno");
        munitions.put(AmmoType.Munitions.M_AX_HEAD, "SRM Acid");

        // Torpedoes
        munitions.put(AmmoType.Munitions.M_TORPEDO, "LRT/SRT");

        // iNarc Munition Types
        munitions.put(AmmoType.Munitions.M_EXPLOSIVE, "iNarc Explosive");
        munitions.put(AmmoType.Munitions.M_ECM, "iNarc ECM");
        munitions.put(AmmoType.Munitions.M_HAYWIRE, "iNarc HayWire");
        munitions.put(AmmoType.Munitions.M_NEMESIS, "iNarc Nemesis");

        // Narc Munition Types
        munitions.put(AmmoType.Munitions.M_NARC_EX, "Narc Explosive");

        // Arrow IV Munition Types
        munitions.put(AmmoType.Munitions.M_HOMING, "Arrow IV Homing");
        munitions.put(AmmoType.Munitions.M_FASCAM, "Arrow IV FASCAM");
        munitions.put(AmmoType.Munitions.M_INFERNO_IV, "Arrow IV Inferno");
        munitions.put(AmmoType.Munitions.M_VIBRABOMB_IV, "Arrow IV VibraBomb");
        munitions.put(AmmoType.Munitions.M_SMOKE, "Arrow IV Smoke");
        munitions.put(AmmoType.Munitions.M_DAVY_CROCKETT_M, "Arrow IV Davy Crockett");
        return munitions;
    }

    public EnumSet<AmmoType.Munitions> getServerBannedAmmo() {
        return serverBannedAmmo;
    }

    public void setServerBannedAmmo(EnumSet<AmmoType.Munitions> ban) {
        serverBannedAmmo = ban;
    }

    public Vector<Integer> getBannedTargetingSystems() {
        return bannedTargetingSystems;
    }

    public void setBannedTargetingSystems(Vector<Integer> ban) {
        bannedTargetingSystems = ban;
    }

    /**
     * extracts data from the BinReader and places it into the client side hash table.
     *
     */
    public void importAccessLevels(@Nonnull BinReader in) {
        Hashtable<String, Integer> commandTemp = getCommandTable();

        int size = in.readInt("CommandSize");
        for (int pos = 0; pos < size; pos++) {
            String commandName = in.read("CommandName");
            int accessLevel = in.readInt("AccessLevel");
            commandTemp.put(commandName, accessLevel);
        }// end while

        setCommandTable(commandTemp);
    }

    public Hashtable<String, Integer> getCommandTable() {
        return commands;
    }

    public void setCommandTable(Hashtable<String, Integer> commands) {
        this.commands = commands;
    }

    public int getAccessLevel(@Nonnull String command) {
        int level = 200;

        if (getCommandTable().get(command.toUpperCase()) != null) {
            level = getCommandTable().get(command.toUpperCase()).intValue();
        }

        return level;
    }

    public TreeMap<String, String> getPlanetOpFlags() {
        return planetOpFlags;
    }

    public Properties getServerConfigs() {
        return serverConfigs;

    }

    public void setServerConfigs(Properties configs) {
        serverConfigs = configs;
    }

    public boolean targetSystemIsBanned(int id) {
        return bannedTargetingSystems.contains(id);
    }
}
