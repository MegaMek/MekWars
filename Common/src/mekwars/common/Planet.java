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

/*
 * Created on 23.03.2004
 *
 */
package mekwars.common;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import jakarta.annotation.Nonnull;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;
import mekwars.common.util.Position;


/**
 * Represents a single planet in the campaign galaxy: its identity, galactic position, terrain makeup,
 * industrial output, and — most importantly for the political simulation — which {@link House} faction(s)
 * hold {@link Influences} (and therefore ownership) over it.
 * <p>
 * A Planet's surface terrain is described by a {@link PlanetEnvironments} collection of {@link Continent}s.
 * Its political state is tracked by an {@link Influences} instance, which records how much "influence" each
 * {@link House} has accumulated here; the house with a clear plurality is considered the owner (see
 * {@link #getPlanetOwner()}). {@link SubFaction}s are a further subdivision within a House and are not
 * directly referenced from Planet, but factories/production on a planet ultimately benefit whichever House
 * controls it.
 *
 * @author Helge Richter
 *
 */

public class Planet implements Comparable<Object>, MutableSerializable {

    // VARIABLES
    /**
     * Unique id of this planet. Mutable field (although it will not change, it has to be transfered)
     */
    private int id;

    /**
     * name of the planet. Should be unique among planets too.
     */
    private String name;

    /**
     * position of this planet in the inner sphere map. Ranges from about -700 to 700 in both directions.
     */
    private Position position; // distance calculates faster, also fewer casts

    /**
     * The unit factories on this planet. Type is the UnitFactory Mutable field (has to be transferred)
     */
    private Vector<UnitFactory> unitFactories = new Vector<>(1, 1);

    /**
     * The environment modifiers for the planet: the set of {@link Continent}s (terrain + climate + relative
     * weight) making up this planet's surface. See {@link PlanetEnvironments}.
     */
    private PlanetEnvironments environments = new PlanetEnvironments();

    /**
     * A human-readable description of the planet.
     */
    private String description = "";

    /**
     * Number of bays to add to the faction holding the planet.
     */
    private int baysProvided = 0;

    /**
     * Whether you can conquer the planet with Conquer - task.
     */
    private boolean conquerable = true;

    /**
     * How much components are produced through this planet.
     */
    private int compProduction = 0;

    /**
     * The influence each faction ({@link House}) has on this planet, which determines political
     * ownership. Mutable field (has to be transfered). See {@link Influences}.
     */
    private Influences influence;

    /**
     * Map and board sizes are now stored as diminsions for static map usage Torren
     */
    private Dimension MapSize = new Dimension(1, 1); // default megamek map
    // size
    private Dimension BoardSize = new Dimension(16, 17);// default megamek board
    // size

    /**
     * Min Planet ownership to allow a faction to use the planets resources defaults to -1 so that the server wide on is
     * used.
     */

    private int minPlanetOwnerShip = -1;

    /**
     * Boolean that states if a planet is a homeworld or not
     */

    private boolean homeWorld = false;

    /** Name of the faction that originally/historically owned this planet (e.g. for lore/flavor purposes). */
    private String originalOwner = "";

    /*
     * This allows SO's to set flags for planets and to be used in ops.
     */
    private TreeMap<String, String> planetFlags = new TreeMap<>();

    /*
     * Max Planet Points. this Allows SO's to set the conquer points of a planet
     * That way some planets are harder to conquer then others.
     */
    private int maxConquestPoints = 100;

    // CONSTRUCTORS
    /**
     * Creates a new planet with the given identity, galactic position, and starting influence/ownership
     * state. Other fields (terrain, factories, description, etc.) are left at their defaults and must be
     * set separately.
     *
     * @param id       unique identifier for this planet.
     * @param name     the planet's display name.
     * @param position galactic (x, y) coordinates.
     * @param influence the initial faction {@link Influences} for this planet.
     */
    public Planet(int id, String name, Position position, Influences influence) {
        setId(id);
        setName(name);
        setPosition(position);
        setInfluence(influence);
    }

    /**
     * Used for serialization
     */
    public Planet() {
        // no content
    }

    /**
     * Reconstructs a Planet by reading it back from a binary stream (delegates to {@link #binIn}).
     *
     * @param in       the binary stream reader.
     * @param factions unused directly here but part of the historical constructor signature (kept for
     *                 API compatibility); faction lookups during read are done via {@code data} instead.
     * @param data     campaign data used to resolve referenced objects (terrains, etc.) while reading.
     * @throws IOException if the underlying stream read fails.
     */
    public Planet(BinReader in, Map<Integer, House> factions, CampaignData data) throws IOException {
        this.binIn(in, data);
    }

    // METHODS

    /**
     * Populates this planet's full state (identity, position, factories, terrain, influence, flags, etc.)
     * by reading it from a binary stream previously written by {@link #binOut}.
     *
     * @param in   the binary stream reader.
     * @param data campaign data used to resolve referenced terrain/advanced-terrain objects.
     * @throws IOException if the underlying stream read fails.
     */
    public void binIn(BinReader in, CampaignData data) throws IOException {
        setId(in.readInt("id"));
        setName(in.read("name"));
        setPosition(new Position(in.readDouble("x"), in.readDouble("y")));
        int size = in.readInt("unitFactories.size");
        setUnitFactories(new Vector<>(size, 1));
        for (int i = 0; i < size; ++i) {
            UnitFactory uf = new UnitFactory();
            uf.binIn(in);
            getUnitFactories().add(uf);
        }
        setEnvironments(new PlanetEnvironments());
        getEnvironments().binIn(in, data);
        setDescription(in.read("description"));
        setBaysProvided(in.readInt("baysProvided"));
        setConquerable(in.readBoolean("conquerable"));
        setCompProduction(in.readInt("compProduction"));
        setInfluence(new Influences());
        getInfluence().binIn(in);
        setMinPlanetOwnerShip(in.readInt("minplanetownership"));
        setHomeWorld(in.readBoolean("homeworld"));
        setOriginalOwner(in.read("originalowner"));
        TreeMap<String, String> map = new TreeMap<>();
        size = in.readInt("PlanetFlags.size");
        for (int i = 0; i < size; ++i) {
            String key;
            String value;
            key = in.read("PlanetFlags.key");
            value = in.read("PlanetFlags.value");
            map.put(key, value);
        }
        setPlanetFlags(map);

        setConquestPoints(in.readInt("MaxInfluence"));
    }

    /**
     * @return Returns the Factories.
     */
    public Vector<UnitFactory> getUnitFactories() {
        return unitFactories;
    }

    /**
     * @param unitFactories The Factories to set.
     */
    public void setUnitFactories(Vector<UnitFactory> unitFactories) {
        this.unitFactories = unitFactories;
    }

    /**
     * @return Returns the environments (terrain makeup) of this planet.
     */
    public PlanetEnvironments getEnvironments() {
        return environments;
    }

    /**
     * @param environments The environments (terrain makeup) to set.
     */
    public void setEnvironments(PlanetEnvironments environments) {
        this.environments = environments;
    }

    /**
     * @return Returns the faction influence/ownership tracker for this planet.
     */
    public Influences getInfluence() {
        return influence;
    }

    /**
     * @param influence The faction influence/ownership tracker to set.
     */
    public void setInfluence(Influences influence) {
        this.influence = influence;
    }

    /**
     * Checks whether the given faction currently holds political ownership of this planet, i.e. has a
     * clear plurality of {@link Influences} here (see {@link Influences#getOwner()}).
     *
     * @param factionId the {@link House} id to test.
     * @return true if this planet is currently owned (uncontested) by the given faction; false if it is
     *         owned by someone else or the planet has no clear owner (contested/tied influence).
     *
     * @author Torren (Jason Tighe)
     */
    public boolean isOwner(int factionId) {
        Integer ownerID = getPlanetOwner();
        if (ownerID == null) {
            return false;
        }
        return ownerID == factionId;
    }

    /**
     * @return the id of the current owner of the planet, or {@code null} if there is no clear owner (e.g.
     *         two or more factions are tied on influence — a "hot zone").
     *
     * @author Torren (Jason Tighe)
     */
    public Integer getPlanetOwner() {
        return getInfluence().getOwner();
    }

    /**
     *
     * @return a string w/ link and name (an HTML anchor that client UIs use to jump to this planet).
     */
    public String getNameAsLink() {
        return "<a href=\"JUMPTOPLANET" + name + "#\">" + name + "</a>";
    }

    /**
     * Checks for any conquest points (CP) not currently claimed by a house's influence and assigns the
     * remainder to the special "House None" (faction id -1), representing unclaimed/neutral influence.
     * Houses with id -1 are skipped when summing existing claimed influence (since -1 IS the neutral
     * bucket being computed). If the sum of all named houses' influence leaves a positive remainder of
     * {@link #getConquestPoints()}, that remainder is written into the influence table under id -1.
     */
    public void updateInfluences() {
        int totalCP = getConquestPoints();

        for (House house : getInfluence().getHouses()) {
            if (house.getId() == -1) {
                continue;
            }
            totalCP -= getInfluence().getInfluence(house.getId());
        }

        if (totalCP > 0) {
            getInfluence().updateHouse(-1, totalCP);
        }

    }

    /**
     * @return the maximum conquest/influence points obtainable on this planet (a difficulty knob: higher
     *         values mean more total influence must be accumulated to flip ownership).
     */
    public int getConquestPoints() {
        return maxConquestPoints;
    }

    /**
     * Sets the maximum conquest/influence points for this planet, clamped to a minimum of 1 so the value
     * is never zero or negative (which would break influence-percentage math elsewhere).
     *
     * @param points the desired maximum conquest points.
     */
    public void setConquestPoints(int points) {
        maxConquestPoints = Math.max(1, points);
    }

    /**
     * Comparable after the id
     */
    public int compareTo(@Nonnull Object o) {
        Planet p = (Planet) o;
        return Integer.compare(getId(), p.getId());
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Encode all mutable fields into the stream. Use as few bits as possible.
     * <p>
     * Note: despite the "few bits" intent in the comment, this delegates to the full {@link #binOut}
     * (after writing id and delegating influence encoding), so it currently writes the entire planet
     * state rather than a delta/compact form.
     *
     * @param out          the binary stream writer.
     * @param dataProvider campaign data used by nested encode calls.
     */
    public void encodeMutableFields(BinWriter out, CampaignData dataProvider) {
        out.println(getId(), "id");
        getInfluence().encodeMutableFields(out, dataProvider);
        binOut(out);
    }

    /**
     * Decode all mutable fields from the stream, mirroring {@link #encodeMutableFields}.
     *
     * @param in           the binary stream reader.
     * @param dataProvider campaign data used by nested decode calls.
     * @throws IOException if the underlying stream read fails.
     */
    public void decodeMutableFields(BinReader in, CampaignData dataProvider) throws IOException {
        setId(in.readInt("id"));
        getInfluence().decodeMutableFields(in, dataProvider);
        binIn(in, dataProvider);
    }

    /**
     * Writes this planet's full state (identity, position, factories, terrain, influence, flags, etc.)
     * to a binary stream, in the same field order expected by {@link #binIn}.
     *
     * @param out the binary stream writer.
     */
    public void binOut(BinWriter out) {
        out.println(getId(), "id");
        out.println(getName(), "name");
        out.println(getPosition().x, "x");
        out.println(getPosition().y, "y");
        out.println(getUnitFactories().size(), "unitFactories.size");

        for (UnitFactory unitFactory : getUnitFactories()) {
            unitFactory.binOut(out);
        }

        getEnvironments().binOut(out);
        out.println(getDescription(), "description");
        out.println(getBaysProvided(), "baysProvided");
        out.println(isConquerable(), "conquerable");
        out.println(getCompProduction(), "compProduction");
        getInfluence().binOut(out);
        out.println(getMinPlanetOwnerShip(), "minplanetownership");
        out.println(isHomeWorld(), "homeworld");
        out.println(getOriginalOwner(), "originalowner");
        out.println(getPlanetFlags().size(), "PlanetFlags.size");

        for (String key : getPlanetFlags().keySet()) {
            out.println(key, "PlanetFlags.key");
            out.println(getPlanetFlags().get(key), "PlayerFlags.value");
        }

        out.println(getConquestPoints(), "MaxInfluence");
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return Returns the position (galactic x/y coordinates, roughly -700 to 700).
     */
    public Position getPosition() {
        return position;
    }

    /**
     * @return Returns the human-readable description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Returns the number of extra unit bays this planet grants to its owning faction.
     */
    public int getBaysProvided() {
        return baysProvided;
    }

    /**
     * @param baysProvided The baysProvided to set.
     */
    public void setBaysProvided(int baysProvided) {
        this.baysProvided = baysProvided;
    }

    /**
     * @return Returns whether this planet can be conquered via the Conquer task.
     */
    public boolean isConquerable() {
        return conquerable;
    }

    /**
     * @return Returns the amount of components produced/exported by this planet's industry.
     */
    public int getCompProduction() {
        return compProduction;
    }

    /**
     * @param compProduction The component production amount to set.
     */
    public void setCompProduction(int compProduction) {
        this.compProduction = compProduction;
    }

    /**
     * @return the minimum planet-ownership setting for this planet, or -1 to defer to the server-wide
     *         default (this planet does not override it).
     */
    public int getMinPlanetOwnerShip() {
        return minPlanetOwnerShip;
    }

    /**
     * @param ownership the minimum planet-ownership requirement to set (-1 to defer to the server-wide
     *                   default).
     */
    public void setMinPlanetOwnerShip(int ownership) {
        minPlanetOwnerShip = ownership;
    }

    /**
     * @return true if this planet is flagged as a faction homeworld.
     */
    public boolean isHomeWorld() {
        return homeWorld;
    }

    /**
     * @param homeworld whether this planet should be flagged as a faction homeworld.
     */
    public void setHomeWorld(boolean homeworld) {
        homeWorld = homeworld;
    }

    /**
     * @return the name of the faction that historically/originally owned this planet.
     */
    public String getOriginalOwner() {
        return originalOwner;
    }

    /**
     * @param owner the original-owner faction name to set.
     */
    public void setOriginalOwner(String owner) {
        originalOwner = owner;
    }

    /**
     * @return the scenario-operator-defined flags/points-of-interest for this planet (key/value pairs
     *         used for custom campaign notes, e.g. in {@link #getLongDescription}).
     */
    public TreeMap<String, String> getPlanetFlags() {
        return planetFlags;
    }

    /**
     * @param flags the planet flags/points-of-interest map to set.
     */
    public void setPlanetFlags(TreeMap<String, String> flags) {
        planetFlags = flags;
    }

    /**
     * @param conquerable whether this planet can be conquered via the Conquer task.
     */
    public void setConquerable(boolean conquerable) {
        this.conquerable = conquerable;
    }

    /**
     * @param description The human-readable description to set.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @param position The galactic (x, y) position to set.
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Builds a long, HTML-formatted description of this planet for display to players/clients: name,
     * galactic location and distance from the galaxy center, industry (component production, extra bays,
     * factories), terrain breakdown (with weighted percentages, atmosphere, gravity, temperature, and
     * weather forecast per continent), current faction influence levels, and any planet flags/points of
     * interest.
     *
     * @param client if true, terrain images are rendered using absolute filesystem paths (for a local
     *               client); if false, a server-relative image description is used instead.
     * @return the assembled HTML description.
     */
    public StringBuilder getLongDescription(boolean client) {

        StringBuilder result = new StringBuilder("Information for Planet: <b>");
        result.append(getName()).append("</b><br><br>");
        // result.append("</b> ("+ getDescription() + ")<br><br>");
        result.append("<b>Location:</b> ")
              .append((int) getPosition().x)
              .append(" x ")
              .append((int) getPosition().y)
              .append(" y<br>")
              .append(Math.round(getPosition().distanceSq(0.0, 0.0)))
              .append(" Light years from the galaxy center <br><br>");

        result.append("<b>Industry:</b><br>");
        // factories
        if (getCompProduction() > 0) {
            result.append("Heavy industry allows an export of ").append(getCompProduction()).append(" parts.<br>");
        }
        if (getBaysProvided() > 0) {
            result.append("A base on this world provides all players with ")
                  .append(getBaysProvided())
                  .append(" extra bays.<br>");
        }

        if (!getUnitFactories().isEmpty()) {
            String founder;

            if (getUnitFactories().size() == 1) {
                result.append("<br><b>Factory:</b><br>");
            } else {
                result.append("<br><b>Factories:</b><br>");
            }

            for (UnitFactory u : getUnitFactories()) {
                founder = u.getFounder();
                String openImage = "./data/images/open" + founder + ".gif";

                if (!new File(openImage).exists()) {
                    openImage = "./data/images/open.gif";
                }

                result.append("<img src=\"file:///")
                      .append(new File(openImage).getAbsolutePath())
                      .append("\">")
                      .append(u.getSize())
                      .append(" ")
                      .append(u.getFullTypeString())
                      .append(u.getName())
                      .append(" built by ")
                      .append(founder)
                      .append("<br>");
            }
        }

        result.append("<br><b>Planetary Conditions</b><br>");

        result.append("<br><b>Terrain:</b><br>");
        int maxProbab = getEnvironments().getTotalEnvironmentProbabilities();
        if (getEnvironments().size() < 1) {
            result.append("nothing special");
        } else {
            for (Continent pe : getEnvironments().toArray()) {
                int curProb = (pe.getSize() * 100 / maxProbab);
                if (curProb < 10) {
                    result.append("0");
                }
                result.append(curProb).append("% ");
                if (client) {
                    result.append(pe.getEnvironment().toImageAbsolutePathDescription());
                } else {
                    result.append(pe.getEnvironment().toImageDescription());
                }
                String terrainName = pe.getEnvironment().getName();

                result.append(" ")
                      .append(terrainName)
                      .append(" (")
                      .append(pe.getAdvancedTerrain().getDisplayName())
                      .append(")");
                result.append("<br>");

                result.append("  Atmosphere: ");
                result.append(pe.getAdvancedTerrain().getAtmosphere());
                result.append("<br>");

                result.append("  Gravity: ").append(pe.getAdvancedTerrain().getGravity()).append("<br>");
                result.append("  Average Low: ").append(pe.getAdvancedTerrain().getLowTemp()).append("<br>");
                result.append("  Average High: ").append(pe.getAdvancedTerrain().getHighTemp()).append("<br>");
                result.append("<br>");
                result.append(pe.getAdvancedTerrain().WeatherForecast());
                result.append("<br>");


            }
        }

        // influence
        result.append("<br><b>Influence:</b><br>");
        for (House h : getInfluence().getHouses()) {
            String color = "#999999";
            String name = "None";
            int id = -1;

            if (h != null) {
                color = h.getHouseColor();
                name = h.getName();
                id = h.getId();
            }

            result.append("<font color=")
                  .append(color)
                  .append(">")
                  .append(name)
                  .append("</font> (")
                  .append(getInfluence().getInfluence(id))
                  .append(")");
            result.append(", ");
        } // End for Each

        result.replace(result.length() - 2, result.length(), "<br>");

        if (!getPlanetFlags().isEmpty()) {
            result.append("<br><b>Points of Interest:</b><br>");
            for (String value : getPlanetFlags().values()) {
                result.append(value).append(", ");
            }
            result.replace(result.length() - 2, result.length(), "<br> <br>");
        }// end if planet has flags
        return result;
    }

    /**
     * Builds an alternate, HTML-formatted "advanced" description of this planet, similar to
     * {@link #getLongDescription(boolean)} but always using absolute-path terrain images, including
     * additional terrain details (night temperature modifier), and optionally showing the planet's raw id.
     *
     * @param level an access/privilege level; if 100 or greater, the planet's numeric id is included in
     *              the output alongside its name.
     * @return the assembled HTML description.
     */
    public StringBuilder getAdvanceDescription(int level) {

        StringBuilder result = new StringBuilder();

        result.append("Information for Planet: <b>");
        result.append(getName()).append("</b>");

        if (level >= 100) {
            result.append(" (ID: ").append(getId()).append(")");
        }

        result.append("<br><br>");
        // result.append("</b> ("+ getDescription() + ")<br><br>");
        result.append("<b>Location:</b> ")
              .append((int) getPosition().x)
              .append(" x ")
              .append((int) getPosition().y)
              .append(" y<br>")
              .append(Math.round(getPosition().distanceSq(0.0, 0.0)))
              .append(" Light years from the galaxy center <br><br>");

        result.append("<b>Industry:</b><br>");
        // factories
        if (getCompProduction() > 0) {
            result.append("Heavy industry allows an export of ").append(getCompProduction()).append(" parts.<br>");
        }
        if (getBaysProvided() > 0) {
            result.append("A warehouse on this world provides all players with ")
                  .append(getBaysProvided())
                  .append(" extra .<br><br>");
        }
        if (!getUnitFactories().isEmpty()) {
            String founder;
            if (getUnitFactories().size() == 1) {
                result.append("<br><b>Factory:</b><br>");
            } else {
                result.append("<br><b>Factories:</b><br>");
            }
            for (UnitFactory u : getUnitFactories()) {
                founder = u.getFounder();
                String openImage = "./data/images/open" + founder + ".gif";

                if (!new File(openImage).exists()) {
                    openImage = "./data/images/open.gif";
                }

                result.append("<img src=\"file:///")
                      .append(new File(openImage).getAbsolutePath())
                      .append(">")
                      .append(u.getSize())
                      .append(" ")
                      .append(u.getFullTypeString())
                      .append(u.getName())
                      .append(" built by ")
                      .append(founder)
                      .append("<br>");
            }
        }

        result.append("<br><b>Terrain:</b><br>");
        int maxProbabilities = getEnvironments().getTotalEnvironmentProbabilities();
        if (getEnvironments().size() < 1) {
            result.append("nothing special");
        } else {
            for (Continent pe : getEnvironments().toArray()) {
                int curProb = (pe.getSize() * 100 / maxProbabilities);
                if (curProb < 10) {
                    result.append("0");
                }
                result.append(curProb).append("% ");
                result.append(pe.getEnvironment().toImageAbsolutePathDescription());
                result.append(" ").append(pe.getEnvironment().getName());
                result.append(" - ").append(pe.getAdvancedTerrain().getName());
                result.append("<br>Atmosphere: ");
                result.append(pe.getAdvancedTerrain().getAtmosphere());
                result.append("<br>");
                result.append("Gravity: ").append(pe.getAdvancedTerrain().getGravity());
                result.append("<br>Average Low: ").append(pe.getAdvancedTerrain().getLowTemp());
                result.append("<br>Average High: ").append(pe.getAdvancedTerrain().getHighTemp());
                result.append("<br>Night Temp Mod: ").append(pe.getAdvancedTerrain().getNightTempMod());
                result.append("<br>").append(pe.getAdvancedTerrain().WeatherForecast());
            }
        }

        // influence
        result.append("<br><br><b>Influence:</b><br>");
        for (House house : getInfluence().getHouses()) {
            String color = "#999999";
            String name = "None";
            int id = -1;

            if (house != null) {
                color = house.getHouseColor();
                name = house.getName();
                id = house.getId();
            }

            result.append("<font color=")
                  .append(color)
                  .append(">")
                  .append(name)
                  .append("</font> (")
                  .append(getInfluence().getInfluence(id))
                  .append(")");
            result.append(", ");
        } // while*/
        result.replace(result.length() - 2, result.length(), "<br>");
        if (!getPlanetFlags().isEmpty()) {
            result.append("<br><b>Points of Interest:</b><br>");
            for (String value : getPlanetFlags().values()) {
                result.append(value).append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append("<br><br>");
        }// end if planet has flags

        return result;
    }

    /**
     * @return the number of unit factories present on this planet.
     */
    public int getFactoryCount() {

        // int count = 0;
        return getUnitFactories().size();
        /*
         * for (Iterator i = getUnitFactories().iterator(); i.hasNext();) {
         * count++; i.next(); } return count;
         */
    }

    /**
     * @return the static-map size (in MegaMek map-sheet units) used when generating scenarios on this
     *         planet.
     */
    public Dimension getMapSize() {
        return MapSize;
    }

    /**
     * @param map the static-map size to set.
     */
    public void setMapSize(Dimension map) {
        MapSize = map;
    }

    /**
     * @return the board size (in hexes) used when generating scenarios on this planet.
     */
    public Dimension getBoardSize() {
        return BoardSize;
    }

    /**
     * @param board the board size (in hexes) to set.
     */
    public void setBoardSize(Dimension board) {
        BoardSize = board;
    }
}
