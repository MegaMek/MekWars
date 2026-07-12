/*
 * MekWars - Copyright (C) 2008
 *
 * Original author - jtighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.common;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;


/**
 * A named terrain "theme" used by MekWars campaign maps (e.g. a planet or continent). A {@code Terrain} is really a
 * container of one or more {@link PlanetEnvironment} map-generation rulesets: each contained environment represents a
 * variant of this terrain theme (for example, different times of year or weighted "flavors" of the same biome), and
 * each carries its own probability weight (see {@link PlanetEnvironment#getEnvironmentalProb()}) used to randomly pick
 * which environment ruleset configures the MegaMek {@code MapSettings} when a battle is launched on this terrain.
 * <p>
 * Instances are persisted either as a single {@code "TE$..."} delimited string (see {@link #toString()}) or via the
 * {@link #binIn}/{@link #binOut} binary stream methods; both encodings must be kept in sync if fields are added.
 */

final public class Terrain {
    /** The set of environment rulesets (variants/themes) that make up this terrain; see class Javadoc. */
    private final Vector<PlanetEnvironment> environments = new Vector<>(10, 1);
    // id
    /** Unique identifier used to reference this terrain from persisted data; -1 means "unset". */
    private int id = -1;
    /** Display name of this terrain (e.g. "Temperate", "Arctic"). */
    private String Name = "";

    /**
     * For Serialisation.
     */
    public Terrain() {
    }

    /**
     * Reconstructs a {@code Terrain} from its {@code "TE$name$..."}-delimited string form as produced by
     * {@link #toString()}. The name is read first, and then every remaining {@code $}-delimited segment is consumed
     * by successive {@link PlanetEnvironment#PlanetEnvironment(StringTokenizer)} constructor calls until the
     * tokenizer is exhausted, so all contained environments must immediately follow the name in the string.
     *
     * @param s the delimited string to parse
     */
    public Terrain(String s) {
        StringTokenizer ST = new StringTokenizer(s, "$");
        // Read the TE$;
        ST.nextToken();
        // Read the Data

        Name = ST.nextToken();

        while (ST.hasMoreElements()) {
            PlanetEnvironment PE = new PlanetEnvironment(ST);
            environments.add(PE);
        }
    }

    /**
     * Serializes this terrain (name plus all contained environments) into the {@code "TE$name$..."}-delimited string
     * format consumed by {@link #Terrain(String)}.
     */
    public String toString() {
        StringBuilder result = new StringBuilder("TE$");
        result.append(Name).append("$");

        for (PlanetEnvironment env : environments) {
            result.append(env.toString());
        }

        return result.toString();
    }

    /**
     * Writes this terrain (id, name, and all contained environments) as a binary stream. Must stay in sync field-for-
     * field with {@link #binIn}.
     */
    public void binOut(BinWriter out) throws IOException {
        out.println(id, "id");
        out.println(Name, "name");

        out.println(environments.size(), "environmentsize");

        for (PlanetEnvironment env : environments) {
            env.binOut(out);
        }

    }

    /**
     * Reads this terrain (id, name, and all contained environments) back from a binary stream previously written by
     * {@link #binOut}. The environment count is read first so exactly that many {@link PlanetEnvironment} objects
     * can be deserialized in turn.
     *
     * @param in   the binary reader positioned at the start of this terrain's data
     * @param data the owning campaign data, forwarded to each {@link PlanetEnvironment#binIn} call
     */
    public void binIn(BinReader in, CampaignData data) throws IOException {
        id = in.readInt("id");
        Name = in.read("name");

        int environments = in.readInt("environmentsize");

        for (int pos = 0; pos < environments; pos++) {
            PlanetEnvironment PE = new PlanetEnvironment();

            PE.binIn(in, data);

            this.environments.add(PE);
        }
    }

    /**
     * @return Returns the id.
     */
    public int getId() {
        return id;
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return Name;
    }

    /**
     * @param name The name to set.
     */
    public void setName(String name) {
        Name = name;
    }

    /**
     * @return the mutable list of {@link PlanetEnvironment} variants that belong to this terrain theme; changes to
     * the returned {@code Vector} directly affect this terrain's contents.
     */
    public Vector<PlanetEnvironment> getEnvironments() {
        return this.environments;
    }

    /**
     * Delegates to the first contained environment's {@link PlanetEnvironment#toImageDescription()} (an HTML snippet
     * of representative terrain-feature icons), or returns an empty string if this terrain has no environments.
     */
    public String toImageDescription() {
        if (!environments.isEmpty()) {
            return environments.getFirst().toImageDescription();
        }

        return "";
    }

    /**
     * Delegates to the first contained environment's {@link PlanetEnvironment#toImageAbsolutePathDescription()}, or
     * returns an empty string if this terrain has no environments.
     */
    public String toImageAbsolutePathDescription() {
        if (!environments.isEmpty()) {
            return environments.getFirst().toImageAbsolutePathDescription();
        }

        return "";
    }

    /**
     * Return the total probability of all environments. Since each {@link PlanetEnvironment} carries its own
     * relative weight ({@link PlanetEnvironment#getEnvironmentalProb()}), this sum is the denominator callers use
     * when randomly selecting one environment to generate a battle map (e.g. picking a random number in
     * {@code [0, total)} and walking the list until the cumulative weight exceeds it).
     */
    public int getTotalEnvironmentProbabilities() {
        int result = 0;
        for (PlanetEnvironment pe : environments) {result += pe.getEnvironmentalProb();}
        return result;
    }

}
