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
 * <p>
 * Unlike {@link Unit}/{@link Army} (which are base classes subclassed by client-side {@code CUnit}/{@code CArmy}
 * and server-side {@code SUnit}/{@code SArmy}), {@code CampaignData} is <em>not</em> subclassed. It is used
 * as-is, directly, by both the client and the server: each side constructs its own {@code CampaignData} instance
 * (the server builds one from XML resource files or a {@link BinReader} snapshot; the client rebuilds one from
 * the same binary encoding sent over the wire) and publishes it to the static {@link #cd} field so that any code
 * in the shared {@code mekwars.common} package can reach "the current campaign's houses/planets/terrains" without
 * needing a reference threaded through every call. It is also unrelated to
 * {@code mekwars.common.campaign.CCampaign}, which is a separate, client-only, per-connection object holding
 * session state (Black Market listings, chat, etc.) for a single logged-in player; {@code CCampaign} and other
 * client code simply read from {@link #cd} when they need static campaign-wide data such as a {@link House} or
 * {@link Planet} by id.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public class CampaignData implements TerrainProvider {
    private final static MMLogger LOGGER = MMLogger.create(CampaignData.class);

    /**
     * Global singleton pointing at the currently active campaign data set. Set by every constructor (so
     * constructing a new {@code CampaignData} always makes it "the" current one), and read from all over the
     * client and server code as the entry point for looking up houses, planets, terrains, etc. Being a mutable
     * public static field, this is effectively a global variable: only one campaign data set can be "active" per
     * JVM at a time.
     */
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

    /** List of all "advanced" terrains (a richer/extended terrain variant) usable on planet surfaces. */
    private final ArrayList<AdvancedTerrain> advTerrains = new ArrayList<>();

    /** Free-form per-planet operational flags. key=flag name, value=flag value (both arbitrary strings). */
    private final TreeMap<String, String> planetOpFlags = new TreeMap<>();

    /** Munition types the server administrator has banned from use campaign-wide; empty means nothing is banned. */
    private EnumSet<AmmoType.Munitions> serverBannedAmmo = EnumSet.noneOf(AmmoType.Munitions.class);

    /** IDs (see {@code TargetSystem} type constants) of targeting systems banned campaign-wide by the server. */
    private Vector<Integer> bannedTargetingSystems = new Vector<>();

    /** Command-name to minimum-access-level map, used to gate chat/admin commands. key=command name (uppercase), value=required access level. */
    private Hashtable<String, Integer> commands = new Hashtable<>();

    /** Miscellaneous server-side configuration key/value pairs, forwarded to and cached by the client. */
    private Properties serverConfigs = new Properties();

    /**
     * Creates an empty campaign data set (no terrains, houses, or planets yet) and immediately publishes it as
     * the active campaign via {@link #cd}. Used when a fresh server is being initialized from XML resource
     * files, which are then loaded in with the various {@code add*} methods below.
     */
    public CampaignData() {
        cd = this;
        PlanetEnvironments.data = this;
    }

    /**
     * Reconstructs a full campaign data set (terrains, advanced terrains, houses, and planets, in that order)
     * by reading the binary encoding previously written by {@link #binOut(BinWriter)}. Used by the client to
     * decode the campaign snapshot sent by the server on login, and by the server to reload a saved campaign.
     * Publishes itself as the active campaign via {@link #cd} as a side effect.
     *
     * @param in the binary stream to decode; must have been produced by a matching {@link #binOut(BinWriter)} call.
     * @throws IOException if the underlying stream fails or the data is malformed.
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
     * Registers a basic {@link Terrain} with this campaign's terrain catalog. Note: this method is not part of
     * the {@link TerrainProvider} interface (which only declares {@code addAdvancedTerrain}); it is a plain
     * helper specific to {@code CampaignData}. The original Javadoc here incorrectly referenced
     * {@code TerrainProvider#addAdvancedTerrain} via {@code @see} - that has been corrected.
     *
     * @param terrain the terrain to add; appended to the internal list as-is (no id/duplicate checking here).
     */
    public void addTerrain(@Nonnull Terrain terrain) {
        terrains.add(terrain);
        terrains.trimToSize();
    }

    /**
     * Adds a faction to the campaign storage. If it was already within the storage, it replaces the old object.
     * If the faction has no id yet (id == -1) and isn't the special "None" faction, an unused id is assigned via
     * {@link #getUnusedHouseID()} before storing.
     *
     * @param faction The faction to hold.
     *                <p>
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     TODO You should use XStream to initialize CampaignData
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
     * If the planet has no id yet (id == -1), an unused id is assigned via {@link #getUnusedPlanetID()} before
     * storing.
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
     *                                                                                                                                                                                                                                                                                                                                                                   TODO There should be no need for such function, since ID's should extracted from resource files. This
     *                                                                                                                                                                                                                                                                                                                                                                         function will vanish if ids are part of the resource.
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
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 TODO There should be no need for such function, since ID's should extracted from resource files. This
     *                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       function will vanish if ids are part of the resource.
     */
    public int getUnusedPlanetID() {
        int id = 0;

        while (planets.containsKey(id)) {
            id++;
        }

        return id;
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
     * Looks up a planet and one of its {@link UnitFactory} instances by name, and overwrites its
     * "ticks until refresh" countdown. Used to sync the client's view of factory production timers (e.g. when a
     * player spends Resource Points to rush a factory refresh) with the server's authoritative value. Silently
     * does nothing if the planet or factory name cannot be resolved.
     *
     * @param planetString the planet's name.
     * @param factory      the factory's name (looked up via {@link #getFactoryByName(Planet, String)}).
     * @param tick         the new number of ticks until the factory's next refresh.
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
     * choice). Name lookup is case-insensitive (matched via {@link #planetID}, keyed by lower-cased name). If the
     * name is unknown, {@code planetID.get(...)} yields {@code null}, which auto-unboxes to throw a
     * {@link NullPointerException} that is caught here and turned into a logged {@code null} return.
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
     * Finds a {@link UnitFactory} on the given planet by matching its name (case-insensitive).
     *
     * @param planet the planet whose factories are searched.
     * @param name   the factory name to match.
     * @return the matching factory, or {@code null} if none of the planet's factories has that name.
     *
     * @author jtighe
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
     * Check if the planet name was only partial and complete it. Iterates all planets (in id order, since
     * {@link #planets} is a {@link TreeMap}) and returns the first whose name equals or contains {@code name}
     * (case-sensitive, unlike {@link #getPlanetByName(String)}). Note there is no early-exit priority for an
     * exact match over a substring match beyond iteration order, so with an ambiguous partial name the result
     * depends on planet id ordering.
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
     *
     * @return a live view of all known planets, keyed internally by id (unmodifiable via this accessor,
     *         but backed by the same map used to add/remove planets).
     */
    public Collection<Planet> getAllPlanets() {
        return planets.values();
    }

    /**
     * BUMM - Blow up a planet. Removes it from both the id and name lookup tables. Note this does not remove any
     * references to the planet held elsewhere (e.g. neighbor lists on other planets); the id/name simply becomes
     * unresolvable in this campaign's storage.
     *
     * @param id The id of the blown-up planet. Must currently exist (NPE if not, since {@code getPlanet(id)} is
     *           dereferenced before the null check would apply).
     */
    public void removePlanet(int id) {
        planetID.remove(getPlanet(id).getName().toLowerCase());
        planets.remove(id);
    }

    /**
     * Remove all planets. Only clears the id-keyed {@link #planets} map; note this does NOT clear
     * {@link #planetID} (the name-to-id lookup table), which will keep stale entries after this call.
     */
    public void clearPlanets() {
        planets.clear();
    }

    /**
     * Retrieves all factions.
     *
     * @return a live view of all known Houses/factions.
     */
    public Collection<House> getAllHouses() {
        return factions.values();
    }

    /**
     * Remove a house from the server this is normally only for single faction servers. In addition to removing
     * the faction from in-memory storage (id and name lookup tables), this deletes the faction's persisted
     * {@code ./campaign/factions/<name>.dat} and {@code .bak} files from disk, if present.
     *
     * @param id the id of the House/faction to remove; must currently exist.
     */
    public void removeHouse(int id) {
        String factionName = getHouse(id).getName().toLowerCase();
        factionID.remove(factionName);
        factions.remove(id);

        File factionFile = new File(String.format("./campaign/factions/%s.dat", factionName));
        if (factionFile.exists()) {
            factionFile.delete();
        }

        factionFile = new File(String.format("./campaign/factions/%s.bak", factionName));
        if (factionFile.exists()) {
            factionFile.delete();
        }
    }

    /**
     * Retrieve a specific faction.
     *
     * @param id The id of the House.
     *
     * @return The requested faction, or {@code null} if no faction with that id exists.
     */
    public House getHouse(int id) {
        return factions.get(id);
    }

    /**
     * Retrieve a faction by its name (case-insensitive, via {@link #factionID}). Returns {@code null} both when
     * the name is unknown (auto-unboxing {@code null} throws NPE, caught below) and, technically, if the id
     * resolves to a faction id that no longer exists in {@link #factions}.
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
     * Remove all factions. Note: like {@link #clearPlanets()}, this only clears the id-keyed {@link #factions}
     * map and leaves {@link #factionID} (the name lookup table) with stale entries.
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
     *
     * @param out the sink to write the encoded campaign snapshot to. Writes, in order: all terrains and advanced
     *            terrains, all houses, then all planets. Must be decoded back with the matching
     *            {@link #CampaignData(BinReader)} constructor.
     */
    public void binOut(BinWriter out) throws IOException {
        binTerrainsOut(out);
        binHousesOut(out);
        binPlanetsOut(out);
    }

    /**
     * Outputs all basic and advanced terrains (in that order) to the given writer.
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
     * Outputs all factions currently held by this campaign (the full set, not a differential).
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
     * Outputs all planets currently held by this campaign (the full set, not a differential).
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
     * Overload of {@link #binHousesOut(BinWriter)} that writes an arbitrary/explicit subset of houses instead of
     * every house held by this campaign (e.g. only those that changed).
     *
     * @param houses the houses to write.
     * @param out    the sink to write to.
     * @see CampaignData#binOut(BinWriter)
     */
    public void binHousesOut(ArrayList<House> houses, BinWriter out) {
        out.println(houses.size(), "houses.size");
        for (House house : houses) {
            house.binOut(out);
        }
    }

    /**
     * Overload of {@link #binPlanetsOut(BinWriter)} that writes an arbitrary/explicit subset of planets instead
     * of every planet held by this campaign (e.g. only those that changed).
     *
     * @param planets the planets to write.
     * @param out     the sink to write to.
     * @see CampaignData#binOut(BinWriter)
     */
    public void binPlanetsOut(ArrayList<Planet> planets, BinWriter out) {
        out.println(planets.size(), "planets.size");
        for (Planet planet : planets) {
            planet.binOut(out);
        }
    }

    /**
     * Updates sent Planets due a differential update. Reads a count-prefixed list of (planetId, mutable-field-set)
     * pairs from {@code in} and applies each to the already-known planet with that id (looked up via
     * {@link #getPlanet(int)}); the planet must already exist in this campaign, since only its mutable fields are
     * being refreshed, not the whole object. For each updated planet, records the {@link Influences} delta
     * (before vs. after decoding) into {@code changesSinceLastRefresh} so callers can react to influence swings
     * (e.g. faction-control changes) without re-diffing everything themselves.
     *
     * @param in                      the stream to decode the differential update from.
     * @param changesSinceLastRefresh A map to hold the change in planet ids that got updated this refresh. Structure is
     *                                as follows: key=planetID(Integer), value=Influences(differential). Cleared at
     *                                the start of this call and populated as planets are processed.
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
     * Writes some planets due a differential update. Counterpart to {@link #decodeMutablePlanets}: for each id in
     * {@code ids}, writes the id followed by that planet's mutable fields only (not the full planet object).
     *
     * @param out the sink to write to.
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
     * Linear search of {@link #terrains} by id.
     *
     * @return the matching {@link Terrain}, or {@code null} if no terrain with that id is registered.
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
     * Assigns the given advanced terrain an unused id (via {@link #getUnusedAdvTerrainID()}) and registers it.
     * Note this always overwrites any id the terrain may already have.
     *
     * @see TerrainProvider#addAdvancedTerrain(AdvancedTerrain)
     */
    public void addAdvancedTerrain(AdvancedTerrain newAdvTerrain) {
        newAdvTerrain.setId(getUnusedAdvTerrainID());
        advTerrains.add(newAdvTerrain);
        advTerrains.trimToSize();
    }

    /*adding the advanced terrain to the campaign data*/

    /**
     * Linear search of {@link #advTerrains} by id.
     * <p>
     * Quirk: unlike {@link #getTerrain(int)}/{@link #getHouse(int)}/{@link #getPlanet(int)}, an unknown id here
     * does not return {@code null} but a brand new, empty {@link AdvancedTerrain} instance (id -1, no data). Callers
     * cannot distinguish "not found" from "found an empty advanced terrain" other than by inspecting the result.
     *
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

    /**
     * Linear search of {@link #terrains} by name (case-insensitive).
     *
     * @return the matching {@link Terrain}, or {@code null} if none matches.
     */
    public @Nullable Terrain getTerrainByName(String TerrainName) {
        for (Terrain env : terrains) {
            if (env.getName().equalsIgnoreCase(TerrainName)) {
                return env;
            }
        }
        return null;
    }

    /**
     * Linear search of {@link #advTerrains} by name (case-insensitive).
     * <p>
     * Quirk: same as {@link #getAdvancedTerrain(int)} - returns a fresh empty {@link AdvancedTerrain} rather than
     * {@code null} when nothing matches.
     */
    public AdvancedTerrain getAdvancedTerrainByName(String AdvTerrainName) {
        for (AdvancedTerrain env : advTerrains) {
            if (env.getName().equalsIgnoreCase(AdvTerrainName)) {
                return env;
            }
        }
        return new AdvancedTerrain();
    }

    /**
     * Builds a fresh lookup table (not cached - a new {@link Hashtable} is allocated on every call) mapping the
     * human-readable display name of each MegaMek {@link AmmoType.Munitions} value MekWars cares about (special
     * munition types such as LBX Cluster, Artemis, Inferno, Narc, Arrow IV variants, etc.) to its enum constant.
     * Used wherever a player-facing munition name needs to be resolved back to the MegaMek enum, e.g. when parsing
     * a loadout choice. Unrelated to {@link #getServerBannedAmmo()}/{@link #setServerBannedAmmo}, which track which
     * of these munitions are disallowed server-wide.
     *
     * @return Hashtable keyed by display name (e.g. "LBX Cluster"), valued by {@link AmmoType.Munitions}.
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
     * The inverse of {@link #getMunitionsByName()}: builds a fresh (uncached) lookup table mapping each
     * {@link AmmoType.Munitions} enum constant back to its human-readable display name, for the same fixed set of
     * munition types.
     *
     * @return Hashtable keyed by {@link AmmoType.Munitions}, valued by display name.
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

    /** @return the set of munition types banned campaign-wide by the server. */
    public EnumSet<AmmoType.Munitions> getServerBannedAmmo() {
        return serverBannedAmmo;
    }

    /** @param ban the new set of campaign-wide banned munition types (wholesale replaces the current set). */
    public void setServerBannedAmmo(EnumSet<AmmoType.Munitions> ban) {
        serverBannedAmmo = ban;
    }

    /** @return the ids of targeting systems banned campaign-wide by the server. */
    public Vector<Integer> getBannedTargetingSystems() {
        return bannedTargetingSystems;
    }

    /** @param ban the new list of campaign-wide banned targeting system ids (wholesale replaces the current list). */
    public void setBannedTargetingSystems(Vector<Integer> ban) {
        bannedTargetingSystems = ban;
    }

    /**
     * Extracts command access-level data from the BinReader and places it into the client side hash table
     * ({@link #commands}). Reads a count-prefixed list of (command name, access level) pairs and merges them
     * into the existing table (does not clear it first), then re-sets it via {@link #setCommandTable}.
     *
     * @param in the stream to decode the access level table from.
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

    /** @return the command-name (uppercase) to required-access-level table. */
    public Hashtable<String, Integer> getCommandTable() {
        return commands;
    }

    /** @param commands the new command-name to required-access-level table (wholesale replaces the current one). */
    public void setCommandTable(Hashtable<String, Integer> commands) {
        this.commands = commands;
    }

    /**
     * Looks up the minimum access level required to run a chat/admin command.
     *
     * @param command the command name (matched case-insensitively, uppercased before lookup).
     * @return the required access level, or {@code 200} (an intentionally high/restrictive default) if the
     *         command is not present in {@link #commands}.
     */
    public int getAccessLevel(@Nonnull String command) {
        int level = 200;

        if (getCommandTable().get(command.toUpperCase()) != null) {
            level = getCommandTable().get(command.toUpperCase()).intValue();
        }

        return level;
    }

    /** @return the free-form per-planet operational flags table (name to value). */
    public TreeMap<String, String> getPlanetOpFlags() {
        return planetOpFlags;
    }

    /** @return the miscellaneous server configuration properties (key/value pairs). */
    public Properties getServerConfigs() {
        return serverConfigs;

    }

    /** @param configs the new server configuration properties (wholesale replaces the current set). */
    public void setServerConfigs(Properties configs) {
        serverConfigs = configs;
    }

    /**
     * @param id a targeting system id (see {@code TargetSystem} type constants).
     * @return {@code true} if this targeting system id is on the campaign-wide ban list.
     */
    public boolean targetSystemIsBanned(int id) {
        return bannedTargetingSystems.contains(id);
    }
}
