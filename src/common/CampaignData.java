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

package common;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.Vector;

import common.util.BinReader;
import common.util.BinWriter;
import common.util.MWLogger;
import megamek.common.AmmoType;


/**
 * TODO: It seems, that all operations done here are needed independly of the
 * semantic of the underlying structure. Planets are handled equal to factions
 * and each function is doubled. If this is true, it should be managed in an
 * generic way to reduce code bloat and code replication.
 * 
 * Campaign is the base of the data holding classes for client and server. Here
 * all campaign relevant information as Houses, Planets and Player data is
 * stored.
 * 
 * In this base class some methods are provided to retrieve these informations
 * to use in common data classes like House or Planet when reffering to
 * ressources.
 * 
 * Notice: Please read the doc to binOut before adding new data types.
 * 
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CampaignData implements TerrainProvider {

    public static CampaignData cd;
    //public static final PKLogManager mwlog = PKLogManager.getInstance();

    /**
     * All different Houses are stored here. key=Integer (id), value=House
     */
    private TreeMap<Integer, House> factions = new TreeMap<Integer, House>();

    /**
     * All different House ids are stored here key=String (name), value=int id
     */
    private TreeMap<String, Integer> factionid = new TreeMap<String, Integer>();

    /**
     * This is a list with all planet information stored. key=Integer (id),
     * value=Planet (or subclasses for server and client)
     */
    private TreeMap<Integer, Planet> planets = new TreeMap<Integer, Planet>();

    /**
     * This is a list with planet id stored. key=String (name), value=int id
     */
    private TreeMap<String, Integer> planetid = new TreeMap<String, Integer>();

    /**
     * List of all terrains that can occur on surfaces of planets.
     */
    private ArrayList<Terrain> terrains = new ArrayList<Terrain>();
    private ArrayList<AdvancedTerrain> advTerrains = new ArrayList<AdvancedTerrain>();
    

    private Hashtable<String, String> ServerBannedAmmo = new Hashtable<String, String>();
    private Vector<Integer> bannedTargetingSystems = new Vector<Integer>();
    private Hashtable<String, Integer> commands = new Hashtable<String, Integer>();
    private TreeMap<String, String> planetOpFlags = new TreeMap<String, String>();

    private Properties serverConfigs = new Properties();

    /**
     * Retrieve a specific planet.
     * 
     * @param id
     *            The id of the planet.
     * @return The requested Planet. This is usually a subclass of Planet.
     */
    public Planet getPlanet(int id) {
        return planets.get(id);
    }

    /**
     * Retrieve a planet by its name. Please try to use planet Id's when lookup
     * for a planet instead (if you have the choice).
     */
    public Planet getPlanetByName(String name) {

        try {
            Integer planetID = planetid.get(name.toLowerCase());
            return getPlanet(planetID);
        } catch (Exception ex) {
            MWLogger.errLog("Looking for planet: " + name);
            // MWLogger.errLog(ex);
            return null;
        }
    }

    /**
     * @author jtighe Retrieve a factory by its name.
     * 
     */
    public UnitFactory getFactoryByName(Planet p, String name) {
        for (UnitFactory e : p.getUnitFactories()) {
            if (e.getName().equalsIgnoreCase(name)) {
                return e;
            }
        }
        return null;
    }

    /**
     * @author Torren (Jason Tighe)
     * @param planet
     * @param Factory
     * 
     *            Updates the Client side factories Useful for the factory
     *            Refresh with RP
     */
    public void updateFactoryTick(String planet, String factory, int tick) {
        Planet p = getPlanetByName(planet);
        UnitFactory unitFactory = getFactoryByName(p, factory);
        unitFactory.setTicksUntilRefresh(tick);
    }

    /**
     * Check if the planet name was only partial and complete it..
     */
    public Planet getPlanetByPartialName(String name) {
        for (Planet p : getAllPlanets()) {
            if (p.getName().equals(name)) {
                return p;
            }

            if (p.getName().indexOf(name) != -1) {
                return p;
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
     * Adds a planet to the campaign storage. If it was already within the
     * storage, it replaces the old object.
     * 
     * @param planet
     *            The planet to hold.
     * @see You should use XStream to initialize CampaignData
     */
    public void addPlanet(Planet planet) {
        if (planet.getId() == -1) {
            planet.setId(getUnusedPlanetID());
        }
        planets.put(planet.getId(), planet);
        planetid.put(planet.getName().toLowerCase(), planet.getId());
    }

    /**
     * BUMM - Blow up a planet.
     * 
     * @param id
     *            The id of the blown up planet.
     */
    public void removePlanet(int id) {
        planetid.remove(getPlanet(id).getName().toLowerCase());
        planets.remove(id);
    }

    /**
     * Remove all planets.
     */
    public void clearPlanets() {
        planets.clear();
    }

    /**
     * Retrieve an unused id for planets.
     * 
     * @TODO There should be no need for such function, since ID's should
     *       extracted from ressource files. This function will vanish if ids
     *       are part of the ressource.
     * @return An Planet id not used yet.
     */
    public int getUnusedPlanetID() {
        int id = 0;
        while (planets.keySet().contains(id)) {
            id++;
        }
        return id;
    }

    /**
     * Retrieve a specific faction.
     * 
     * @param id
     *            The id of the House.
     * @return The requested faction.
     */
    public House getHouse(int ID) {
        return factions.get(ID);
    }

    /**
     * Retrieves all factions.
     */
    public Collection<House> getAllHouses() {
        return factions.values();
    }

    /**
     * Adds a faction to the campaign storage. If it was already within the
     * storage, it replaces the old object.
     * 
     * @param planet
     *            The faction to hold.
     * @TODO You should use XStream to initialize CampaignData
     */
    public void addHouse(House faction) {
        if (faction.getId() == -1 && !faction.getName().equalsIgnoreCase("None")) {
            faction.setId(getUnusedHouseID());
        }
        factions.put(faction.getId(), faction);
        factionid.put(faction.getName().toLowerCase(), faction.getId());
    }

    /**
     * Remove a house from the server this is normally only for single faction
     * servers
     * 
     * @param Integer
     *            id
     */
    public void removeHouse(int id) {
        String factionName = getHouse(id).getName().toLowerCase();
        factionid.remove(factionName);
        factions.remove(id);

        File factionFile = new File("./campaign/factions/" + factionName + ".dat");
        if (factionFile.exists()) {
            factionFile.delete();
        }

        factionFile = new File("./campaign/factions/" + factionName + ".bak");
        if (factionFile.exists()) {
            factionFile.delete();
        }
    }

    /**
     * Retrieve a faction by its name.
     * 
     * @param name
     * @return
     * @TODO This seems to be only needed, because some serialization work with
     *       transmitting the factions name instead of its id.
     */
    public House getHouseByName(String name) {
        try {
            House h = getHouse(factionid.get(name.toLowerCase()));
            return h;
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
     * Retrieve an unused id for factions.
     * 
     * @TODO There should be no need for such function, since ID's should
     *       extracted from ressource files. This function will vanish if ids
     *       are part of the ressource.
     * @return An House id not used yet.
     */
    public int getUnusedHouseID() {
        int id = -1;
        int hid = 0;
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
     * Retrieve an unused id for terrains. Only used upon start up of a new
     * server using XML files.
     * 
     * @return An terrain id not used yet.
     */
    public int getUnusedTerrainID() {
        int id = -1;
        int hid = -1;
        for (Terrain e : terrains) {
            hid = e.getId();
            if (hid > id) {
                id = hid;
            }
        }
        id++;
        return id;
    }
    /**
     * Retrieve an unused id for advterrains. Only used upon start up of a new
     * server using XML files.
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
     * Since I have no idea how TinyXML is operating and since McWizard does not
     * allow me to use my loved JDom and finally since Enkel does not like
     * XML-Transfer anyway, I use this to encode/decode the whole object.. (Imi)
     * 
     * There is another aspect of binOut to keep in mind. Since a MD5 hash is
     * build after each differential update to keep the data in sync, this
     * function has to provide THE SAME output each time it is run, regardless
     * of the underlying virtual machine. Currently this is done by only using
     * container classes, that remain the elements in a stable order. If you
     * need to add a container with unstable order (as Hash*), you have to make
     * sure, the data is odered before writing it out with binOut.
     * 
     * TODO: check http://jira.codehaus.org/secure/ViewIssue.jspa?key=XSTR-27 to
     * see whether a better way of serialization is available ;-)
     */
    public void binOut(BinWriter out) throws IOException {
        binTerrainsOut(out);
        binHousesOut(out);
        binPlanetsOut(out);
    }

    /**
     * Outputs all factions
     * 
     * @see CampaignData.binOut()
     */
    public void binHousesOut(BinWriter out) throws IOException {
        out.println(factions.size(), "factions.size");
        for (House house : factions.values()) {
            house.binOut(out);
        }
    }

    /**
     * Outputs updated houses
     * 
     * @see CampaignData.binOut()
     */
    public void binHousesOut(ArrayList<House> houses, BinWriter out) throws IOException {
        out.println(houses.size(), "houses.size");
        for (House house : houses) {
            house.binOut(out);
        }
    }

    /**
     * Outputs all terrains
     * 
     * @see CampaignData.binOut()
     */
    public void binTerrainsOut(BinWriter out) throws IOException {
        out.println(terrains.size(), "terrains.size");
        for (Terrain pe : terrains) {
            pe.binOut(out);
        }
        out.println(advTerrains.size(), "advTerrains.size");
        for (AdvancedTerrain pe : advTerrains) {
            pe.binOut(out);
        }
        
    }

    /**
     * Outputs all planets
     * 
     * @see CampaignData.binOut()
     */
    public void binPlanetsOut(BinWriter out) throws IOException {
        out.println(planets.size(), "planets.size");
        for (Planet p : planets.values()) {
            p.binOut(out);
        }
    }

    /**
     * Outputs all planets
     * 
     * @see CampaignData.binOut()
     */
    public void binPlanetsOut(ArrayList<Planet> planets, BinWriter out) throws IOException {
        out.println(planets.size(), "planets.size");
        for (Planet planet : planets) {
            planet.binOut(out);
        }
    }

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
    public CampaignData(BinReader in) throws IOException {
        cd = this;
        PlanetEnvironments.data = this;
        int size = in.readInt("terrains.size");
        for (int i = 0; i < size; ++i) {
            Terrain pe = new Terrain();
            pe.binIn(in, this);            
            addTerrain(pe);
        }
        int Advsize = in.readInt("advTerrains.size");
        for (int i = 0; i < Advsize; ++i) {
            AdvancedTerrain pe = new AdvancedTerrain();
            pe.binIn(in);
            addAdvancedTerrain(pe);
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
     * Updates sent Planets due a differential update.
     * 
     * @param changesSinceLastRefresh
     *            A map to hold the change in planet ids that got updated this
     *            refresh. Structure is as follows: key=planetID(Integer),
     *            value=Influences(differential)
     */
    public void decodeMutablePlanets(BinReader in, Map<Integer, Influences> changesSinceLastRefresh) throws IOException {
        int count = in.readInt("mutableplanetsize");
        System.out.println("retrieving " + count + " planets due differential update.");
        changesSinceLastRefresh.clear();
        for (int i = 0; i < count; ++i) {
            int id = in.readInt("planetid");
            Influences infOld = new Influences(getPlanet(id).getInfluence());
            getPlanet(id).decodeMutableFields(in, this);
            Influences infNew = getPlanet(id).getInfluence();
            changesSinceLastRefresh.put(id, infNew.difference(infOld));
        }
    }

    /**
     * Writes some planets due a differential update
     * 
     * @param ids
     *            A collection of java.lang.Integer with the ids to send.
     */
    public void encodeMutablePlanets(BinWriter out, Collection<Integer> ids) throws IOException {
        out.println(ids.size(), "mutableplanetsize");
        for (Integer id : ids) {
            out.println(id, "planetid");
            getPlanet(id).encodeMutableFields(out, this);
        }
    }

    /**
     * Saves itself to disk. This uses the dynamic type of each object to make a
     * copy as close to the real data as possible. Use loadData() to read it
     * back to memory.
     * 
     * public void saveData(File directory) throws IOException { if (directory
     * == null) throw new
     * IllegalArgumentException("Please specify a directory."); if
     * (directory.exists() && !directory.isDirectory()) throw new
     * IllegalArgumentException(directory.getName()+" is not a directory."); if
     * (!directory.exists()) directory.mkdir(); MMNetXStream xml = new
     * MMNetXStream(new DomDriver()); xml.toXML(this,new
     * FileWriter(directory.getPath()+"/data.xml"));
     * 
     * DatWriter datWriter = new
     * DatWriter(directory.getPath()+"/CampaignData.dat");
     * datWriter.write(this,"CampaignData"); datWriter.close(); }
     */

    /**
     * @see common.TerrainProvider#getTerrain(int)
     */
    public Terrain getTerrain(int id) {
        for (Terrain env : terrains) {
            if (env.getId() == id) {
                return env;
            }
        }
        return null;

    }

    /**
     * @see common.TerrainProvider#getAllTerrains()
     */
    public Collection<Terrain> getAllTerrains() {
        return terrains;
    }

    /**
     * @see common.TerrainProvider#addTerrain(common.PlanetEnvironment)
     */
    public void addTerrain(Terrain terrain) {
        terrain.setId(getUnusedTerrainID());
        terrains.add(terrain);
        terrains.trimToSize();
    }

    public Terrain getTerrainByName(String TerrainName) {
        for (Terrain env : terrains) {
            if (env.getName().equalsIgnoreCase(TerrainName)) {
                return env;
            }
        }
        return null;
    }

    /*adding the advanced terrain to the campaign data*/
    /**
     * @see common.TerrainProvider#getAdvancedTerrain(int)
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
     * @see common.TerrainProvider#getAllTerrains()
     */
    public Collection<AdvancedTerrain> getAllAdvancedTerrains() {
        return advTerrains;
    }

    /**
     * @see common.TerrainProvider#addTerrain(common.PlanetEnvironment)
     */
    public void addAdvancedTerrain(AdvancedTerrain newAdvTerrain) {
    	newAdvTerrain.setId(getUnusedAdvTerrainID());
        advTerrains.add(newAdvTerrain);
        advTerrains.trimToSize();
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
     * @see common.persistence.MMNetSerializable#binOut(common.persistence.TreeWriter)
     * 
          public void binOut(TreeWriter out) { out.write(terrains, "terrains");
     *      out.startDataBlock("factions"); out.write(factions.size(),
     *      "factionsCount"); for (House h : factions.values()) {
     *      out.write(h.getClass().getName(), "factionType"); out.write(h,
     *      "faction"); } out.endDataBlock("factions");
     *      out.startDataBlock("planets"); out.write(factions.size(),
     *      "planetsCount"); for (Planet p : planets.values()) {
     *      out.write(p.getClass().getName(), "planetType"); out.write(p,
     *      "planet"); } out.endDataBlock("planets"); }
     */

    /**
     * @see common.persistence.MMNetSerializable#binIn(common.persistence.TreeReader)
     * 
          public void binIn(TreeReader in, CampaignData dataProvider) throws
     *      IOException { terrains.clear(); in.startDataBlock("factions");
     *      factions.clear(); int size = in.readInt("factionsCount"); for (int i
     *      = 0; i < size; ++i) { String type = in.readString("factionType");
     *      try { House h = (House) Class.forName(type).newInstance();
     *      in.readObject(h, this, "faction"); factions.put(new
     *      Integer(h.getId()), h); } catch (InstantiationException e) {
     *      MWLogger.errLog(e); } catch (IllegalAccessException e) {
     *      MWLogger.errLog(e); } catch (ClassNotFoundException e) {
     *      MWLogger.errLog(e); } } in.endDataBlock("factions");
     * 
     *      in.startDataBlock("planets"); planets.clear(); size =
     *      in.readInt("planetsCount"); for (int i = 0; i < size; ++i) { String
     *      type = in.readString("planetsType"); try { Planet p = (Planet)
     *      Class.forName(type).newInstance(); in.readObject(p, this, "planet");
     *      planets.put(new Integer(p.getId()), p); } catch
     *      (InstantiationException e) { MWLogger.errLog(e); } catch
     *      (IllegalAccessException e) { MWLogger.errLog(e); } catch
     *      (ClassNotFoundException e) { MWLogger.errLog(e); } }
     *      in.endDataBlock("planets"); }
     */
    /**
     * @author Torren (Jason Tighe)
     * 
     *         this returns a hashtable of all current MM munitions 06/10/05
     *         using the Name of the munition as the key
     * @return Hashtable
     */
    public Hashtable<String, Long> getMunitionsByName() {
        Hashtable<String, Long> munitions = new Hashtable<String, Long>();

        munitions.put("Standard", AmmoType.M_STANDARD);

        // AC Munition Types
        munitions.put("LBX Cluster", AmmoType.M_CLUSTER);
        munitions.put("AC Armor Piercing", AmmoType.M_ARMOR_PIERCING);
        munitions.put("AC Flechette", AmmoType.M_FLECHETTE);
        munitions.put("AC Incendiary", AmmoType.M_INCENDIARY_AC);
        munitions.put("AC Precision", AmmoType.M_PRECISION);
        munitions.put("AC Tracer", AmmoType.M_TRACER);

        // ATM Munition Types
        munitions.put("ATM Extended Range", AmmoType.M_EXTENDED_RANGE);
        munitions.put("ATM High Explosive", AmmoType.M_HIGH_EXPLOSIVE);

        // LRM & SRM Munition Types
        munitions.put("LRM/SRM Fragmentation", AmmoType.M_FRAGMENTATION);
        munitions.put("LRM/SRM Listen Kill", AmmoType.M_LISTEN_KILL);
        munitions.put("LRM/SRM Anti-TSM", AmmoType.M_ANTI_TSM);
        munitions.put("LRM/SRM Narc", AmmoType.M_NARC_CAPABLE);
        munitions.put("LRM/SRM Artemis", AmmoType.M_ARTEMIS_CAPABLE);
        munitions.put("LRM/SRM Heat-Seeking", AmmoType.M_HEAT_SEEKING);
        munitions.put("LRM/SRM Dead-Fire", AmmoType.M_DEAD_FIRE);
        munitions.put("LRM/SRM Tandem-Charge", AmmoType.M_TANDEM_CHARGE);

        // LRM Munition Types
        // Incendiary is special, though...
        munitions.put("LRM Incendiary", AmmoType.M_INCENDIARY_LRM);
        munitions.put("LRM Flare", AmmoType.M_FLARE);
        munitions.put("LRM SemiGuided", AmmoType.M_SEMIGUIDED);
        munitions.put("LRM Swarm", AmmoType.M_SWARM);
        munitions.put("LRM Swarm I", AmmoType.M_SWARM_I);
        munitions.put("LRM Thunder", AmmoType.M_THUNDER);
        munitions.put("LRM Thunder Augmented", AmmoType.M_THUNDER_AUGMENTED);
        munitions.put("LRM Thunder Inferno", AmmoType.M_THUNDER_INFERNO);
        munitions.put("LRM Thunder VibraBomb", AmmoType.M_THUNDER_VIBRABOMB);
        munitions.put("LRM Thunder Active", AmmoType.M_THUNDER_ACTIVE);
        munitions.put("LRM Follow The Leader", AmmoType.M_FOLLOW_THE_LEADER);
        munitions.put("Multi Purpose", AmmoType.M_MULTI_PURPOSE);

        // SRM Munition Types
        munitions.put("SRM Inferno", AmmoType.M_INFERNO);
        munitions.put("SRM Acid", AmmoType.M_AX_HEAD);

        // Torps
        munitions.put("LRT/SRT", AmmoType.M_TORPEDO);

        // iNarc Munition Types
        munitions.put("iNarc Explosive", AmmoType.M_EXPLOSIVE);
        munitions.put("iNarc ECM", AmmoType.M_ECM);
        munitions.put("iNarc HayWire", AmmoType.M_HAYWIRE);
        munitions.put("iNarc Nemesis", AmmoType.M_NEMESIS);

        // Narc Munition Types
        munitions.put("Narc Explosive", AmmoType.M_NARC_EX);

        // Arrow IV Munition Types
        munitions.put("Arrow IV Homing", AmmoType.M_HOMING);
        munitions.put("Arrow IV FASCAM", AmmoType.M_FASCAM);
        munitions.put("Arrow IV Inferno", AmmoType.M_INFERNO_IV);
        munitions.put("Arrow IV VibraBomb", AmmoType.M_VIBRABOMB_IV);
        munitions.put("Arrow IV Smoke", AmmoType.M_SMOKE);
        munitions.put("Arrow IV Davy Crockett", AmmoType.M_DAVY_CROCKETT_M);
        return munitions;
    }

    /**
     * @author Torren (Jason Tighe)
     * 
     *         this returns a hashtable of all current MM munitions 06/10/05
     *         using the Number of the munition as the key
     * @return Hashtable
     */
    public Hashtable<Long, String> getMunitionsByNumber() {
        Hashtable<Long, String> munitions = new Hashtable<Long, String>();

        munitions.put(AmmoType.M_STANDARD, "Standard");

        // AC Munition Types
        munitions.put(AmmoType.M_CLUSTER, "LBX Cluster");
        munitions.put(AmmoType.M_ARMOR_PIERCING, "AC Armor Piercing");
        munitions.put(AmmoType.M_FLECHETTE, "AC Flechette");
        munitions.put(AmmoType.M_INCENDIARY_AC, "AC Incendiary");
        munitions.put(AmmoType.M_PRECISION, "AC Precision");
        munitions.put(AmmoType.M_TRACER, "AC Tracer");

        // ATM Munition Types
        munitions.put(AmmoType.M_EXTENDED_RANGE, "ATM Extended Range");
        munitions.put(AmmoType.M_HIGH_EXPLOSIVE, "ATM High Explosive");

        // LRM & SRM Munition Types
        munitions.put(AmmoType.M_FRAGMENTATION, "LRM/SRM Fragmentation");
        munitions.put(AmmoType.M_LISTEN_KILL, "LRM/SRM Listen Kill");
        munitions.put(AmmoType.M_ANTI_TSM, "LRM/SRM Anti-TSM");
        munitions.put(AmmoType.M_NARC_CAPABLE, "LRM/SRM Narc");
        munitions.put(AmmoType.M_ARTEMIS_CAPABLE, "LRM/SRM Artemis");
        munitions.put(AmmoType.M_HEAT_SEEKING, "LRM/SRM Heat-Seeking");
        munitions.put(AmmoType.M_TANDEM_CHARGE, "LRM/SRM Tandem-Charge");
        munitions.put(AmmoType.M_DEAD_FIRE, "LRM/SRM Dead-Fire");

        // LRM Munition Types
        // Incendiary is special though...
        munitions.put(AmmoType.M_INCENDIARY_LRM, "LRM Incendiary");
        munitions.put(AmmoType.M_FLARE, "LRM Flare");
        munitions.put(AmmoType.M_SEMIGUIDED, "LRM SemiGuided");
        munitions.put(AmmoType.M_SWARM, "LRM Swarm");
        munitions.put(AmmoType.M_SWARM_I, "LRM Swarm I");
        munitions.put(AmmoType.M_THUNDER, "LRM Thunder");
        munitions.put(AmmoType.M_THUNDER_AUGMENTED, "LRM Thunder Augmented");
        munitions.put(AmmoType.M_THUNDER_INFERNO, "LRM Thunder Inferno");
        munitions.put(AmmoType.M_THUNDER_VIBRABOMB, "LRM Thunder VibraBomb");
        munitions.put(AmmoType.M_THUNDER_ACTIVE, "LRM Thunder Active");
        munitions.put(AmmoType.M_FOLLOW_THE_LEADER, "LRM Follow The Leader");
        munitions.put(AmmoType.M_MULTI_PURPOSE, "Multi Purpose");

        // SRM Munition Types
        munitions.put(AmmoType.M_INFERNO, "SRM Inferno");
        munitions.put(AmmoType.M_AX_HEAD, "SRM Acid");

        // Torps
        munitions.put(AmmoType.M_TORPEDO, "LRT/SRT");

        // iNarc Munition Types
        munitions.put(AmmoType.M_EXPLOSIVE, "iNarc Explosive");
        munitions.put(AmmoType.M_ECM, "iNarc ECM");
        munitions.put(AmmoType.M_HAYWIRE, "iNarc HayWire");
        munitions.put(AmmoType.M_NEMESIS, "iNarc Nemesis");

        // Narc Munition Types
        munitions.put(AmmoType.M_NARC_EX, "Narc Explosive");

        // Arrow IV Munition Types
        munitions.put(AmmoType.M_HOMING, "Arrow IV Homing");
        munitions.put(AmmoType.M_FASCAM, "Arrow IV FASCAM");
        munitions.put(AmmoType.M_INFERNO_IV, "Arrow IV Inferno");
        munitions.put(AmmoType.M_VIBRABOMB_IV, "Arrow IV VibraBomb");
        munitions.put(AmmoType.M_SMOKE, "Arrow IV Smoke");
        munitions.put(AmmoType.M_DAVY_CROCKETT_M, "Arrow IV Davy Crockett");
        return munitions;
    }

    public void setServerBannedAmmo(Hashtable<String, String> ban) {
        ServerBannedAmmo = ban;
    }

    public Hashtable<String, String> getServerBannedAmmo() {
        return ServerBannedAmmo;
    }

    public void setBannedTargetingSystems(Vector<Integer> ban) {
        bannedTargetingSystems = ban;
    }

    public Vector<Integer> getBannedTargetingSystems() {
        return bannedTargetingSystems;
    }

    /**
     * extracts data from the BinReader and places it into the client side hash
     * table.
     * 
     * @param in
     * @param userLevel
     */
    public void importAccessLevels(BinReader in) {
        Hashtable<String, Integer> commandTemp = getCommandTable();

        try {
            int size = in.readInt("CommandSize");
            for (int pos = 0; pos < size; pos++) {
                String commandName = in.readLine("CommandName");
                int accessLevel = in.readInt("AccessLevel");
                commandTemp.put(commandName, accessLevel);
            }// end while
        }// end try
        catch (Exception ex) {
        }// in is empty move on.
        setCommandTable(commandTemp);
    }

    public void setCommandTable(Hashtable<String, Integer> commands) {
        this.commands = commands;
    }

    public Hashtable<String, Integer> getCommandTable() {
        return commands;
    }

    public int getAccessLevel(String command) {
        int level = 200;

        if (getCommandTable().get(command.toUpperCase()) != null) {
            level = getCommandTable().get(command.toUpperCase()).intValue();
            // MWLogger.errLog("Command: "+command+" level: "+level);
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
		if(bannedTargetingSystems.contains(id)) {
			return true;
		}
		return false;
	}


}
