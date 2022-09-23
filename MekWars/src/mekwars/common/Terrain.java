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

package common;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.Vector;

import common.util.BinReader;
import common.util.BinWriter;

/**
 * A Terrain Base Terrain container for all environments. Each environment can be a different theme to allow for different times of year.
 */

final public class Terrain {
    // id
    private int id = -1;
    private String Name = "";
    private Vector<PlanetEnvironment> environments = new Vector<PlanetEnvironment>(10, 1);

    /**
     * For Serialisation.
     */
    public Terrain() {
    }

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

    public String toString() {
        String result = "TE$";
        result += Name + "$";

        for (PlanetEnvironment env : environments) {
            result += env.toString();
        }
        return result;
    }

    /**
     * Writes as binary stream
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
     * Read from a binary stream
     */
    public void binIn(BinReader in, CampaignData data) throws IOException {
        id = in.readInt("id");
        Name = in.readLine("name");

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
     * @param id
     */
    public void setId(int id) {
        this.id = id;
        for ( PlanetEnvironment pe : environments ){
            pe.setId(id);
        }
    }

    /**
     * @return Returns the name.
     */
    public String getName() {
        return Name;
    }

    /**
     * @param name
     *            The name to set.
     */
    public void setName(String name) {
        Name = name;
    }

    /**
     * @return Vector<PlanetEnvironments>
     */
    public Vector<PlanetEnvironment> getEnvironments() {
        return this.environments;
    }

    public String toImageDescription() {
        if (environments.size() > 0)
            return environments.get(0).toImageDescription();

        return "";
    }

    public String toImageAbsolutePathDescription() {
        if (environments.size() > 0)
            return environments.get(0).toImageAbsolutePathDescription();

        return "";
    }
    
    /**
     * Return the total probability of all environments.
     */
    public int getTotalEnvironmentProbabilities() {
        int result = 0;
        for (PlanetEnvironment pe : environments )
            result += pe.getEnvironmentalProb();
        return result;
    }

}
