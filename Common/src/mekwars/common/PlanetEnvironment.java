/*
 * Copyright (C) 2004 Helge Richter (McWizard)
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
import java.util.StringTokenizer;

import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;
import mekwars.common.util.HTMLHelper;

/**
 * Describes the procedural map-generation "recipe" for one variant of a planet/continent's environment (e.g. one
 * seasonal or biome variant of a {@link Terrain}). Every field is a knob that is fed into MegaMek's random board
 * generator (analogous to {@code MapSettings}) when a battle is launched: minimum/maximum counts of terrain "spots"
 * and their hex sizes for each terrain type (water, forest, rough, swamp, pavement, ice, rubble, fortified, sand,
 * planted field), crater and hill/mountain shaping parameters, building/city generation parameters, road/river/cliff
 * probabilities, and a handful of "special effect" chances (forest fire, freeze, flood, drought) plus an optional
 * static (pre-built) map override. Each instance also carries its own {@link #EnvironmentProb relative probability
 * weight} so a {@link Terrain} holding several environments can randomly pick one when a map is rolled.
 * <p>
 * Instances are persisted two ways that must be kept in lock-step: a legacy {@code "PE$..."} delimited string (see
 * {@link #toString()} / the {@code StringTokenizer} constructors) and a binary stream (see {@link #binIn}/
 * {@link #binOut}). The string form appends new fields at the end and guards each with
 * {@code hasMoreTokens()}/{@code hasMoreElements()} checks so that older, shorter saved strings can still be parsed
 * (missing trailing fields simply keep their compiled-in default).
 * <p>
 * Final simple because you should be aware to overwrite binIn and binOut properly if you subclass PlanetEnvironment.
 */

final public class PlanetEnvironment {
    // id
    /** Unique identifier used to reference this environment from persisted data; -1 means "unset". */
    private int id = -1;
    /** Display name of this environment variant (e.g. "Summer", "Badlands"). */
    private String Name = "";

    //Crater
    /** Percent chance (0-100 scale used elsewhere as x10, see {@link #toDescription()}) that craters are generated at all. */
    private int CraterProb = 0;
    /** Minimum number of craters to place when craters are generated. */
    private int CraterMinNum = 0;
    /** Maximum number of craters to place when craters are generated. */
    private int CraterMaxNum = 0;
    /** Minimum radius (in hexes) of a generated crater. */
    private int CraterMinRadius = 0;
    /** Maximum radius (in hexes) of a generated crater. */
    private int CraterMaxRadius = 0;

    //Hills
    /** Overall elevation/roughness intensity used to classify the landscape as plain/uneven/hilly/mountainous (see {@link #toDescription()}). */
    private int Hilliness = 100;
    /** Range of elevation steps hills can span. */
    private int HillElevationRange = 3;
    /** Percent chance hill elevation is inverted (valleys instead of hills, or vice versa). */
    private int HillInvertProb = 0;

    //Water
    /** Minimum number of distinct water features ("spots") to place. */
    private int WaterMinSpots = 3;
    /** Maximum number of distinct water features to place. */
    private int WaterMaxSpots = 8;
    /** Minimum size (in hexes) of each water spot. */
    private int WaterMinHexes = 2;
    /** Maximum size (in hexes) of each water spot. */
    private int WaterMaxHexes = 10;
    /** Percent chance a given water spot is deep water rather than shallow. */
    private int WaterDeepProb = 20;

    //Forest
    /** Minimum number of forest spots to place. */
    private int ForestMinSpots = 4;
    /** Maximum number of forest spots to place. */
    private int ForestMaxSpots = 8;
    /** Minimum size (in hexes) of each forest spot. */
    private int ForestMinHexes = 2;
    /** Maximum size (in hexes) of each forest spot. */
    private int ForestMaxHexes = 6;
    /** Percent chance a given forest spot is heavy woods/jungle rather than light woods. */
    private int ForestHeavyProb = 20;

    //Rough
    /** Minimum number of rough-terrain spots to place. */
    private int RoughMinSpots = 0;
    /** Maximum number of rough-terrain spots to place. */
    private int RoughMaxSpots = 5;
    /** Minimum size (in hexes) of each rough-terrain spot. */
    private int RoughMinHexes = 1;
    /** Maximum size (in hexes) of each rough-terrain spot. */
    private int RoughMaxHexes = 2;

    //Swamp
    /** Minimum number of swamp spots to place. */
    private int SwampMinSpots = 0;
    /** Maximum number of swamp spots to place. */
    private int SwampMaxSpots = 0;
    /** Minimum size (in hexes) of each swamp spot. */
    private int SwampMinHexes = 0;
    /** Maximum size (in hexes) of each swamp spot. */
    private int SwampMaxHexes = 0;

    //Pavement
    /** Minimum number of pavement spots to place. */
    private int PavementMinSpots = 0;
    /** Maximum number of pavement spots to place. */
    private int PavementMaxSpots = 0;
    /** Minimum size (in hexes) of each pavement spot. */
    private int PavementMinHexes = 0;
    /** Maximum size (in hexes) of each pavement spot. */
    private int PavementMaxHexes = 0;

    //Ice
    /** Minimum number of ice spots to place. */
    private int IceMinSpots = 0;
    /** Maximum number of ice spots to place. */
    private int IceMaxSpots = 0;
    /** Minimum size (in hexes) of each ice spot. */
    private int IceMinHexes = 0;
    /** Maximum size (in hexes) of each ice spot. */
    private int IceMaxHexes = 0;

    //Rubble
    /** Minimum number of rubble spots to place. */
    private int RubbleMinSpots = 0;
    /** Maximum number of rubble spots to place. */
    private int RubbleMaxSpots = 0;
    /** Minimum size (in hexes) of each rubble spot. */
    private int RubbleMinHexes = 0;
    /** Maximum size (in hexes) of each rubble spot. */
    private int RubbleMaxHexes = 0;

    //Fortified
    /** Minimum number of fortified-terrain spots to place. */
    private int FortifiedMinSpots = 0;
    /** Maximum number of fortified-terrain spots to place. */
    private int FortifiedMaxSpots = 0;
    /** Minimum size (in hexes) of each fortified-terrain spot. */
    private int FortifiedMinHexes = 0;
    /** Maximum size (in hexes) of each fortified-terrain spot. */
    private int FortifiedMaxHexes = 0;

    //Sand
    /** Minimum number of sand spots to place. */
    private int SandMinSpots = 0;
    /** Maximum number of sand spots to place. */
    private int SandMaxSpots = 0;
    /** Minimum size (in hexes) of each sand spot. */
    private int SandMinHexes = 0;
    /** Maximum size (in hexes) of each sand spot. */
    private int SandMaxHexes = 0;

    //Planted Field
    /** Minimum number of planted-field (farmland) spots to place. */
    private int PlantedFieldMinSpots = 0;
    /** Maximum number of planted-field spots to place. */
    private int PlantedFieldMaxSpots = 0;
    /** Minimum size (in hexes) of each planted-field spot. */
    private int PlantedFieldMinHexes = 0;
    /** Maximum size (in hexes) of each planted-field spot. */
    private int PlantedFieldMaxHexes = 0;

    //Buildings
    /** Minimum number of buildings to place when generating a settlement/city. */
    private int MinBuildings = 0;
    /** Maximum number of buildings to place. */
    private int MaxBuildings = 0;
    /** Minimum Construction Factor (structural toughness) of generated buildings. */
    private int MinCF = 0;
    /** Maximum Construction Factor of generated buildings. */
    private int MaxCF = 0;
    /** Minimum number of floors of generated buildings. */
    private int MinFloors = 0;
    /** Maximum number of floors of generated buildings. */
    private int MaxFloors = 0;
    /** Density (packing) of buildings within a city, 0-100 scale. */
    private int CityDensity = 50;
    /** City block layout type passed to the map generator (e.g. "NONE" for no city). */
    private String CityType = "NONE";
    /** Number/frequency of roads generated through a city. */
    private int Roads = 4;
    /** Size category of the generated town/city. */
    private int TownSize = 0;

    //Special Effects
    /** Special-effects modifier applied when generating the battle (implementation-defined scaling). */
    private int fxMod = 0;
    /** Percent chance the battle starts/develops a forest fire special effect. */
    private int probForestFire = 0;
    /** Percent chance of a freeze/ice special effect. */
    private int probFreeze = 0;
    /** Percent chance of a flood special effect. */
    private int probFlood = 0;
    /** Percent chance of a drought special effect. */
    private int probDrought = 0;
    /** Visual/graphical theme name applied to the generated board (e.g. tileset). */
    private String Theme = "";

    //Mountains
    /** Number of mountain peaks to generate. */
    private int MountPeaks = 0;
    /** Minimum width (in hexes) of a generated mountain range. */
    private int MountWidthMin = 0;
    /** Maximum width (in hexes) of a generated mountain range. */
    private int MountWidthMax = 0;
    /** Minimum height (elevation) of a generated mountain. */
    private int MountHeightMin = 0;
    /** Maximum height (elevation) of a generated mountain. */
    private int MountHeightMax = 0;
    /** Style/shape variant used when generating mountains. */
    private int MountStyle = 0;

    //Misc
    /** Percent chance (x10 scale, see usages) that roads are generated across the board. */
    private int RoadProb = 25;
    /** Percent chance (x10 scale) that rivers are generated across the board. */
    private int RiverProb = 25;
    /** Selects which underlying map-generation algorithm/strategy is used. */
    private int Algorithm = 0;
    /** Percent chance that cliffs are generated. */
    private int CliffProb = 0;
    /** Whether/how negative-elevation terrain is inverted during generation. */
    private int InvertNegativeTerrain = 0;
    /** Relative weight of this environment among its sibling environments in a {@link Terrain}; see {@link Terrain#getTotalEnvironmentProbabilities()}. */
    private int EnvironmentProb = 1;

    //static maps support
    /** Name of the static (pre-built, non-procedural) map to use when {@link #staticMap} is true. */
    private String staticMapName = "surprise";
    /** Width, in boards, of the static map (board-count units, not hexes), or -1 if unused. */
    private int xSize = -1;
    /** Height, in boards, of the static map, or -1 if unused. */
    private int ySize = -1;

    /** If true, this environment uses a pre-built static map ({@link #staticMapName}) instead of procedural generation. */
    private boolean staticMap = false;
    /** Width of the overall game board in individual boards, or -1 if unused. */
    private int xBoardSize = -1;
    /** Height of the overall game board in individual boards, or -1 if unused. */
    private int yBoardSize = -1;

    /**
     * For Serialisation.
     */
    public PlanetEnvironment() {
    }

    /**
     * Parses a {@code PlanetEnvironment} out of its own dedicated {@code "PE$..."} delimited string (as opposed to
     * {@link #PlanetEnvironment(StringTokenizer)}, which consumes tokens from a shared tokenizer owned by a
     * containing {@link Terrain}). The leading {@code "PE$"} marker token is discarded, then fields are read in a
     * fixed legacy order; every field beyond {@link #Algorithm} is optional and guarded by
     * {@code hasMoreTokens()}/{@code hasMoreElements()} so that strings saved by older versions (with fewer trailing
     * fields) still parse correctly, leaving newer fields at their compiled-in defaults.
     *
     * @param string the {@code "PE$..."} delimited string to parse
     */
    public PlanetEnvironment(String string) {
        StringTokenizer stringTokenizer = new StringTokenizer(string, "$");
        //Read the PE$;
        stringTokenizer.nextToken();
        //Read the Data

        Name = stringTokenizer.nextToken();
        CraterProb = Integer.parseInt(stringTokenizer.nextToken());
        CraterMinNum = Integer.parseInt(stringTokenizer.nextToken());
        CraterMaxNum = Integer.parseInt(stringTokenizer.nextToken());
        CraterMinRadius = Integer.parseInt(stringTokenizer.nextToken());
        CraterMaxRadius = Integer.parseInt(stringTokenizer.nextToken());
        Hilliness = Integer.parseInt(stringTokenizer.nextToken());
        HillElevationRange = Integer.parseInt(stringTokenizer.nextToken());
        HillInvertProb = Integer.parseInt(stringTokenizer.nextToken());
        WaterMinSpots = Integer.parseInt(stringTokenizer.nextToken());
        WaterMaxSpots = Integer.parseInt(stringTokenizer.nextToken());
        WaterMinHexes = Integer.parseInt(stringTokenizer.nextToken());
        WaterMaxHexes = Integer.parseInt(stringTokenizer.nextToken());
        WaterDeepProb = Integer.parseInt(stringTokenizer.nextToken());
        ForestMinSpots = Integer.parseInt(stringTokenizer.nextToken());
        ForestMaxSpots = Integer.parseInt(stringTokenizer.nextToken());
        ForestMinHexes = Integer.parseInt(stringTokenizer.nextToken());
        ForestMaxHexes = Integer.parseInt(stringTokenizer.nextToken());
        ForestHeavyProb = Integer.parseInt(stringTokenizer.nextToken());
        RoughMinSpots = Integer.parseInt(stringTokenizer.nextToken());
        RoughMaxSpots = Integer.parseInt(stringTokenizer.nextToken());
        RoughMinHexes = Integer.parseInt(stringTokenizer.nextToken());
        RoughMaxHexes = Integer.parseInt(stringTokenizer.nextToken());
        RoadProb = Integer.parseInt(stringTokenizer.nextToken());
        RiverProb = Integer.parseInt(stringTokenizer.nextToken());
        Algorithm = Integer.parseInt(stringTokenizer.nextToken());
        if (stringTokenizer.hasMoreTokens()) {id = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {SwampMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {SwampMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {SwampMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {SwampMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {PavementMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {PavementMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {PavementMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {PavementMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {fxMod = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {probForestFire = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {probFreeze = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {probFlood = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {probDrought = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {Theme = stringTokenizer.nextToken();}
        if (stringTokenizer.hasMoreTokens()) {IceMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {IceMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {IceMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {IceMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}

        if (stringTokenizer.hasMoreTokens()) {RubbleMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {RubbleMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {RubbleMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {RubbleMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}

        if (stringTokenizer.hasMoreTokens()) {FortifiedMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {FortifiedMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {FortifiedMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {FortifiedMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}

        if (stringTokenizer.hasMoreTokens()) {MinBuildings = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {MaxBuildings = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {MinCF = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {MaxCF = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {MinFloors = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {MaxFloors = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {CityDensity = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreTokens()) {CityType = stringTokenizer.nextToken();}
        if (stringTokenizer.hasMoreTokens()) {Roads = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {CliffProb = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {InvertNegativeTerrain = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {TownSize = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountPeaks = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountWidthMin = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountWidthMax = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountHeightMin = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountHeightMax = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {MountStyle = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {EnvironmentProb = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {setStaticMap(Boolean.parseBoolean(stringTokenizer.nextToken()));}
        if (stringTokenizer.hasMoreElements()) {staticMapName = stringTokenizer.nextToken();}
        if (stringTokenizer.hasMoreElements()) {xSize = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {ySize = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {xBoardSize = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {yBoardSize = Integer.parseInt(stringTokenizer.nextToken());}

        if (stringTokenizer.hasMoreElements()) {SandMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {SandMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {SandMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {SandMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}

        if (stringTokenizer.hasMoreElements()) {PlantedFieldMinSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {PlantedFieldMaxSpots = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {PlantedFieldMinHexes = Integer.parseInt(stringTokenizer.nextToken());}
        if (stringTokenizer.hasMoreElements()) {PlantedFieldMaxHexes = Integer.parseInt(stringTokenizer.nextToken());}
    }

    /**
     * Parses a {@code PlanetEnvironment} by consuming {@code "PE$..."}-delimited tokens directly from a
     * {@link StringTokenizer} shared with the caller (used by {@link Terrain#Terrain(String)} to read a run of
     * environments one after another out of a single string). Behaves identically to
     * {@link #PlanetEnvironment(String)} field-for-field, including the same trailing-optional-field tolerance for
     * backward compatibility with older saved data.
     *
     * @param ST the shared tokenizer, positioned at this environment's leading {@code "PE$"} marker token
     */
    public PlanetEnvironment(StringTokenizer ST) {
        //Read the PE$;
        ST.nextToken();
        //Read the Data

        Name = ST.nextToken();
        CraterProb = Integer.parseInt(ST.nextToken());
        CraterMinNum = Integer.parseInt(ST.nextToken());
        CraterMaxNum = Integer.parseInt(ST.nextToken());
        CraterMinRadius = Integer.parseInt(ST.nextToken());
        CraterMaxRadius = Integer.parseInt(ST.nextToken());
        Hilliness = Integer.parseInt(ST.nextToken());
        HillElevationRange = Integer.parseInt(ST.nextToken());
        HillInvertProb = Integer.parseInt(ST.nextToken());
        WaterMinSpots = Integer.parseInt(ST.nextToken());
        WaterMaxSpots = Integer.parseInt(ST.nextToken());
        WaterMinHexes = Integer.parseInt(ST.nextToken());
        WaterMaxHexes = Integer.parseInt(ST.nextToken());
        WaterDeepProb = Integer.parseInt(ST.nextToken());
        ForestMinSpots = Integer.parseInt(ST.nextToken());
        ForestMaxSpots = Integer.parseInt(ST.nextToken());
        ForestMinHexes = Integer.parseInt(ST.nextToken());
        ForestMaxHexes = Integer.parseInt(ST.nextToken());
        ForestHeavyProb = Integer.parseInt(ST.nextToken());
        RoughMinSpots = Integer.parseInt(ST.nextToken());
        RoughMaxSpots = Integer.parseInt(ST.nextToken());
        RoughMinHexes = Integer.parseInt(ST.nextToken());
        RoughMaxHexes = Integer.parseInt(ST.nextToken());
        RoadProb = Integer.parseInt(ST.nextToken());
        RiverProb = Integer.parseInt(ST.nextToken());
        Algorithm = Integer.parseInt(ST.nextToken());
        if (ST.hasMoreTokens()) {id = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {SwampMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {SwampMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {SwampMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {SwampMaxHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {PavementMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {PavementMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {PavementMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {PavementMaxHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {fxMod = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {probForestFire = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {probFreeze = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {probFlood = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {probDrought = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {Theme = ST.nextToken();}
        if (ST.hasMoreTokens()) {IceMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {IceMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {IceMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {IceMaxHexes = Integer.parseInt(ST.nextToken());}

        if (ST.hasMoreTokens()) {RubbleMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {RubbleMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {RubbleMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {RubbleMaxHexes = Integer.parseInt(ST.nextToken());}

        if (ST.hasMoreTokens()) {FortifiedMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {FortifiedMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {FortifiedMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {FortifiedMaxHexes = Integer.parseInt(ST.nextToken());}

        if (ST.hasMoreTokens()) {MinBuildings = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {MaxBuildings = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {MinCF = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {MaxCF = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {MinFloors = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {MaxFloors = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {CityDensity = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreTokens()) {CityType = ST.nextToken();}
        if (ST.hasMoreTokens()) {Roads = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {CliffProb = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {InvertNegativeTerrain = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {TownSize = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountPeaks = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountWidthMin = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountWidthMax = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountHeightMin = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountHeightMax = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {MountStyle = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {EnvironmentProb = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {setStaticMap(Boolean.parseBoolean(ST.nextToken()));}
        if (ST.hasMoreElements()) {staticMapName = ST.nextToken();}
        if (ST.hasMoreElements()) {xSize = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {ySize = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {xBoardSize = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {yBoardSize = Integer.parseInt(ST.nextToken());}

        if (ST.hasMoreElements()) {SandMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {SandMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {SandMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {SandMaxHexes = Integer.parseInt(ST.nextToken());}

        if (ST.hasMoreElements()) {PlantedFieldMinSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {PlantedFieldMaxSpots = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {PlantedFieldMinHexes = Integer.parseInt(ST.nextToken());}
        if (ST.hasMoreElements()) {PlantedFieldMaxHexes = Integer.parseInt(ST.nextToken());}
    }


    /**
     * Builds a human-readable (HTML-flavored, {@code <br>}-separated) flavor-text description of this environment's
     * landscape, derived heuristically from the generation parameters (not the actual generated map). Computes rough
     * "amount of water/rough terrain/forest" scores from the average of each feature's min/max spot count times its
     * average hex size, then narrates hilliness/craters, followed by vegetation and water coverage.
     * <p>
     * Note: {@code water}, {@code rough}, and {@code forest} here reuse {@code WaterMaxSpots}/{@code RoughMaxSpots}/
     * {@code ForestMaxSpots} in the second half of each product instead of the corresponding {@code MinHexes}/
     * {@code MaxHexes} pairing (e.g. {@code WaterMinHexes + WaterMaxSpots} rather than
     * {@code WaterMinHexes + WaterMaxHexes}); this looks like a copy/paste slip but is left as-is since it only
     * affects descriptive text, not actual map generation.
     */
    public String toDescription() {
        String result = "";

        int water = (((WaterMaxSpots + WaterMinSpots) / 2) * (WaterMinHexes + WaterMaxSpots) / 2) + RiverProb / 10;
        int rough = (((RoughMaxSpots + RoughMinSpots) / 2) * (RoughMinHexes + RoughMaxSpots) / 2);
        int forest = (((ForestMaxSpots + ForestMinSpots) / 2) * (ForestMinHexes + ForestMaxSpots) / 2);
        /* generate the hilliness/crater description */
        result += "The landscape is ";

        if (Hilliness < 200) {
            result += "plain";
        } else if (Hilliness < 500) {
            result += "uneven";
        } else if (Hilliness <= 800) {
            result += "hilly";
        } else {
            result += "mountainous";
        }

        if (CraterProb == 0) {
            result += ". <br> ";
            if (rough > 0) {
                result += "Through tectonic activity of this continent, rough terrain is appearing";
                if (rough > 8) {
                    result += " everywhere";
                } else {
                    result += " sometimes";
                }
            }
        } else {
            if (CraterProb < 30) {
                result += ", which is seldom covered with";
            } else if (CraterProb < 60) {
                result += ", which is covered with";
            } else {
                result += ", often covered with";
            }

            int avgCraterSize = (CraterMinRadius + CraterMaxRadius) / 2;

            if (avgCraterSize < 4) {
                result += " small craters";
            } else if (avgCraterSize < 7) {
                result += " craters ";
            } else {
                result += " large craters";
            }

            if (rough > 0) {
                result += ". Another remaking of the ancient meteor impacts is the rough terrain appearing";

                if (rough > 8) {
                    result += " everywhere";
                } else {
                    result += " sometimes";
                }
            }
        } // craters
        result += ". <br>";

        /* woods */
        result += "Most facilities on this continent are lying";
        if (forest > 50) {
            result += " deep in the ";
            result += (ForestHeavyProb < 30) ? "woods" : "jungle";

            if (water > 20) {
                result += " mixed up with much water, because of heavy rain due too monsoon period";
            }

            result += "";
        } /* jungle */ else {
            if (water > 20) {
                result += " close to the coast.";
            } else if (water < 3) {
                if (forest < 15) {
                    result += " in the desert. So dont expect vegetation for cover or water for cooling.";
                } else {
                    result += " in an area moderately forested.";
                }
            } else {
                result += " in an area famous for its agriculture.";
            }
        } /* else */
        return result;
    } /* to Description */


    /**
     * Builds an HTML fragment of {@code <img>} tags (relative paths under {@code data/images/}) representing this
     * environment's dominant terrain features (hills, roughness, craters, forest, water, rivers, roads), for display
     * in a UI summary. Uses the same water/rough/forest heuristics as {@link #toDescription()} (including the same
     * mixed min/max hex-count quirk noted there).
     */
    public String toImageDescription() {
        String result = "";
        int water = (((WaterMaxSpots + WaterMinSpots) / 2) * (WaterMinHexes + WaterMaxSpots) / 2) + RiverProb / 10;
        int rough = (((RoughMaxSpots + RoughMinSpots) / 2) * (RoughMinHexes + RoughMaxSpots) / 2);
        int forest = (((ForestMaxSpots + ForestMinSpots) / 2) * (ForestMinHexes + ForestMaxSpots) / 2);
        /* generate the hilliness/crater description */

        if (Hilliness < 200) {
            result += HTMLHelper.imageTag("data/images/hill0.gif");
        } else if (Hilliness < 500) {
            result += HTMLHelper.imageTag("data/images/hill1.gif");
        } else if (Hilliness <= 800) {
            result += HTMLHelper.imageTag("data/images/hill2.gif");
        } else {
            result += HTMLHelper.imageTag("data/images/hill3.gif");
        }

        if (rough > 8) {
            result += HTMLHelper.imageTag("data/images/roug1.gif");
        }

        if (CraterProb > 30) {
            result += HTMLHelper.imageTag("data/images/crtr1.gif");
        }

        /* woods */
        if (forest > 15 && forest < 30) {
            result += HTMLHelper.imageTag("data/images/wood1.gif");
        } else if (forest >= 30 && forest < 50) {
            result += HTMLHelper.imageTag("data/images/wood2.gif");
        } else if (forest >= 50) {
            result += HTMLHelper.imageTag("data/images/wood3.gif");
        }

        /*water */
        if (water > 5 && water < 20) {
            result += HTMLHelper.imageTag("data/images/watr1.gif");
        } else if (water >= 20) {
            result += HTMLHelper.imageTag("data/images/watr2.gif");
        }

        if (getRiverProb() > 50) {
            result += HTMLHelper.imageTag("data/images/rivr1.gif");
        }

        if (getRoadProb() > 50) {
            result += HTMLHelper.imageTag("data/images/road1.gif");
        }

        return result;

    }

    /** @return {@link #RiverProb}, the river-generation probability. */
    public int getRiverProb() {
        return RiverProb;
    }

    /** @return {@link #RoadProb}, the road-generation probability. */
    public int getRoadProb() {
        return RoadProb;
    }

    /** @param RoadProb the road-generation probability to set. */
    public void setRoadProb(int RoadProb) {
        this.RoadProb = RoadProb;
    }

    /** @param RiverProb the river-generation probability to set. */
    public void setRiverProb(int RiverProb) {
        this.RiverProb = RiverProb;
    }

    /**
     * Same as {@link #toImageDescription()} but emits {@code file:///<absolute-path>/data/images/...} URLs (using
     * the JVM's current working directory) instead of relative paths, and additionally uses
     * {@link #HillElevationRange} rather than {@link #Hilliness} to choose the hill icon.
     * <p>
     * TODO: remove this code bloat - make a better way to get the images.
     */
    public String toImageAbsolutePathDescription() {
        String result = "";

        int water = (((WaterMaxSpots + WaterMinSpots) / 2) * (WaterMinHexes + WaterMaxSpots) / 2) + RiverProb / 10;
        int rough = (((RoughMaxSpots + RoughMinSpots) / 2) * (RoughMinHexes + RoughMaxSpots) / 2);
        int forest = (((ForestMaxSpots + ForestMinSpots) / 2) * (ForestMinHexes + ForestMaxSpots) / 2);

        /* generate the hilliness/crater description */
        String path = "file:///" + new File("").getAbsolutePath();

        if (HillElevationRange < 2) {
            result += HTMLHelper.imageTag(path, "/data/images/hill0.gif");
        } else if (HillElevationRange < 5) {
            result += HTMLHelper.imageTag(path, "/data/images/hill1.gif\">");
        } else if (HillElevationRange <= 8) {
            result += HTMLHelper.imageTag(path, "/data/images/hill2.gif");
        } else {
            result += HTMLHelper.imageTag(path, "/data/images/hill3.gif");
        }

        if (rough > 8) {
            result += HTMLHelper.imageTag(path, "/data/images/roug1.gif");
        }

        if (CraterProb > 30) {
            result += HTMLHelper.imageTag(path, "/data/images/crtr1.gif");
        }

        /* woods */
        if (forest > 15 && forest < 30) {
            result += HTMLHelper.imageTag(path, "/data/images/wood1.gif");
        } else if (forest >= 30 && forest < 50) {
            result += HTMLHelper.imageTag(path, "/data/images/wood2.gif");
        } else if (forest >= 50) {
            result += HTMLHelper.imageTag(path, "/data/images/wood3.gif");
        }

        /*water */
        if (water > 5 && water < 20) {
            result += HTMLHelper.imageTag(path, "/data/images/watr1.gif");
        } else if (water >= 20) {
            result += HTMLHelper.imageTag(path, "/data/images/watr2.gif");
        }

        if (getRiverProb() > 50) {
            result += HTMLHelper.imageTag(path, "/data/images/rivr1.gif");
        }

        if (getRoadProb() > 50) {
            result += HTMLHelper.imageTag(path, "/data/images/road1.gif");
        }

        return result;

    }

    /**
     * @return this environment serialized to its {@code "PE$..."} delimited string form, equivalent to
     * {@code toString(null)} (no city name embedded).
     */
    public String toString() {
        return this.toString(null);
    }

    /**
     * Serializes this environment to its {@code "PE$..."} delimited string form consumed by
     * {@link #PlanetEnvironment(String)} and {@link #PlanetEnvironment(StringTokenizer)}. Fields are appended in the
     * exact legacy order those constructors expect; if {@code city} is non-blank, it is spliced in immediately after
     * {@link #Roads} (matching where a {@code CityType} name would otherwise be optionally read) before the
     * remaining fields continue.
     *
     * @param city optional city/settlement name to embed in the string; ignored (omitted) if {@code null} or blank
     * @return the delimited string representation of this environment
     */
    public String toString(String city) {
        String result = "PE$";
        result += Name + "$";
        result += CraterProb + "$";
        result += CraterMinNum + "$";
        result += CraterMaxNum + "$";
        result += CraterMinRadius + "$";
        result += CraterMaxRadius + "$";
        result += Hilliness + "$";
        result += HillElevationRange + "$";
        result += HillInvertProb + "$";
        result += WaterMinSpots + "$";
        result += WaterMaxSpots + "$";
        result += WaterMinHexes + "$";
        result += WaterMaxHexes + "$";
        result += WaterDeepProb + "$";
        result += ForestMinSpots + "$";
        result += ForestMaxSpots + "$";
        result += ForestMinHexes + "$";
        result += ForestMaxHexes + "$";
        result += ForestHeavyProb + "$";
        result += RoughMinSpots + "$";
        result += RoughMaxSpots + "$";
        result += RoughMinHexes + "$";
        result += RoughMaxHexes + "$";
        result += RoadProb + "$";
        result += RiverProb + "$";
        result += Algorithm + "$";
        result += id + "$";
        result += SwampMinSpots + "$";
        result += SwampMaxSpots + "$";
        result += SwampMinHexes + "$";
        result += SwampMaxHexes + "$";
        result += PavementMinSpots + "$";
        result += PavementMaxSpots + "$";
        result += PavementMinHexes + "$";
        result += PavementMaxHexes + "$";
        result += fxMod + "$";
        result += probForestFire + "$";
        result += probFreeze + "$";
        result += probFlood + "$";
        result += probDrought + "$";
        result += Theme + "$";
        result += IceMinSpots + "$";
        result += IceMaxSpots + "$";
        result += IceMinHexes + "$";
        result += IceMaxHexes + "$";

        result += RubbleMinSpots + "$";
        result += RubbleMaxSpots + "$";
        result += RubbleMinHexes + "$";
        result += RubbleMaxHexes + "$";

        result += FortifiedMinSpots + "$";
        result += FortifiedMaxSpots + "$";
        result += FortifiedMinHexes + "$";
        result += FortifiedMaxHexes + "$";

        result += MinBuildings + "$";
        result += MaxBuildings + "$";
        result += MinCF + "$";
        result += MaxCF + "$";
        result += MinFloors + "$";
        result += MaxFloors + "$";
        result += CityDensity + "$";
        result += CityType + "$";
        result += Roads + "$";

        if (city != null && !city.trim().isEmpty()) {
            result += city + "$";
        }

        result += CliffProb + "$";
        result += InvertNegativeTerrain + "$";
        result += TownSize + "$";
        result += MountPeaks + "$";
        result += MountWidthMin + "$";
        result += MountWidthMax + "$";
        result += MountHeightMin + "$";
        result += MountHeightMax + "$";
        result += MountStyle + "$";
        result += EnvironmentProb + "$";
        result += staticMap;
        result += "$";
        result += staticMapName;
        result += "$";
        result += xSize;
        result += "$";
        result += ySize;
        result += "$";
        result += xBoardSize;
        result += "$";
        result += yBoardSize;
        result += "$";

        result += SandMinSpots + "$";
        result += SandMaxSpots + "$";
        result += SandMinHexes + "$";
        result += SandMaxHexes + "$";


        result += PlantedFieldMinSpots + "$";
        result += PlantedFieldMaxSpots + "$";
        result += PlantedFieldMinHexes + "$";
        result += PlantedFieldMaxHexes + "$";

        return result;
    }

    //Getter and Setter
    /** @return minimum number of water spots ({@link #WaterMinSpots}). */
    public int getWaterMinSpots() {
        return WaterMinSpots;
    }

    /** @param WaterMinSpots the minimum number of water spots to set. */
    public void setWaterMinSpots(int WaterMinSpots) {
        this.WaterMinSpots = WaterMinSpots;
    }

    /** @return minimum size in hexes of a water spot ({@link #WaterMinHexes}). */
    public int getWaterMinHexes() {
        return WaterMinHexes;
    }

    /** @param WaterMinHexes the minimum water spot size (hexes) to set. */
    public void setWaterMinHexes(int WaterMinHexes) {
        this.WaterMinHexes = WaterMinHexes;
    }

    /** @return maximum size in hexes of a water spot ({@link #WaterMaxHexes}). */
    public int getWaterMaxHexes() {
        return WaterMaxHexes;
    }

    /** @param WaterMaxHexes the maximum water spot size (hexes) to set. */
    public void setWaterMaxHexes(int WaterMaxHexes) {
        this.WaterMaxHexes = WaterMaxHexes;
    }

    /** @return maximum number of water spots ({@link #WaterMaxSpots}). */
    public int getWaterMaxSpots() {
        return WaterMaxSpots;
    }

    /** @param WaterMaxSpots the maximum number of water spots to set. */
    public void setWaterMaxSpots(int WaterMaxSpots) {
        this.WaterMaxSpots = WaterMaxSpots;
    }

    /** @return percent chance a water spot is deep water ({@link #WaterDeepProb}). */
    public int getWaterDeepProb() {
        return WaterDeepProb;
    }

    /** @param WaterDeepProb the deep-water probability to set. */
    public void setWaterDeepProb(int WaterDeepProb) {
        this.WaterDeepProb = WaterDeepProb;
    }

    /** @return minimum number of rough-terrain spots ({@link #RoughMinSpots}). */
    public int getRoughMinSpots() {
        return RoughMinSpots;
    }

    /** @param RoughMinSpots the minimum number of rough-terrain spots to set. */
    public void setRoughMinSpots(int RoughMinSpots) {
        this.RoughMinSpots = RoughMinSpots;
    }

    /** @return minimum size in hexes of a rough-terrain spot ({@link #RoughMinHexes}). */
    public int getRoughMinHexes() {
        return RoughMinHexes;
    }

    /** @param RoughMinHexes the minimum rough-terrain spot size (hexes) to set. */
    public void setRoughMinHexes(int RoughMinHexes) {
        this.RoughMinHexes = RoughMinHexes;
    }

    /** @return maximum number of rough-terrain spots ({@link #RoughMaxSpots}). */
    public int getRoughMaxSpots() {
        return RoughMaxSpots;
    }

    /** @param RoughMaxSpots the maximum number of rough-terrain spots to set. */
    public void setRoughMaxSpots(int RoughMaxSpots) {
        this.RoughMaxSpots = RoughMaxSpots;
    }

    /** @return maximum size in hexes of a rough-terrain spot ({@link #RoughMaxHexes}). */
    public int getRoughMaxHexes() {
        return RoughMaxHexes;
    }

    /** @param RoughMaxHexes the maximum rough-terrain spot size (hexes) to set. */
    public void setRoughMaxHexes(int RoughMaxHexes) {
        this.RoughMaxHexes = RoughMaxHexes;
    }

    /** @return minimum number of swamp spots ({@link #SwampMinSpots}). */
    public int getSwampMinSpots() {
        return SwampMinSpots;
    }

    /** @param SwampMinSpots the minimum number of swamp spots to set. */
    public void setSwampMinSpots(int SwampMinSpots) {
        this.SwampMinSpots = SwampMinSpots;
    }

    /** @return minimum size in hexes of a swamp spot ({@link #SwampMinHexes}). */
    public int getSwampMinHexes() {
        return SwampMinHexes;
    }

    /** @param SwampMinHexes the minimum swamp spot size (hexes) to set. */
    public void setSwampMinHexes(int SwampMinHexes) {
        this.SwampMinHexes = SwampMinHexes;
    }

    /** @return maximum number of swamp spots ({@link #SwampMaxSpots}). */
    public int getSwampMaxSpots() {
        return SwampMaxSpots;
    }

    /** @param SwampMaxSpots the maximum number of swamp spots to set. */
    public void setSwampMaxSpots(int SwampMaxSpots) {
        this.SwampMaxSpots = SwampMaxSpots;
    }

    /** @return maximum size in hexes of a swamp spot ({@link #SwampMaxHexes}). */
    public int getSwampMaxHexes() {
        return SwampMaxHexes;
    }

    /** @param SwampMaxHexes the maximum swamp spot size (hexes) to set. */
    public void setSwampMaxHexes(int SwampMaxHexes) {
        this.SwampMaxHexes = SwampMaxHexes;
    }

    /** @return minimum number of pavement spots ({@link #PavementMinSpots}). */
    public int getPavementMinSpots() {
        return PavementMinSpots;
    }

    /** @param PavementMinSpots the minimum number of pavement spots to set. */
    public void setPavementMinSpots(int PavementMinSpots) {
        this.PavementMinSpots = PavementMinSpots;
    }

    /** @return minimum size in hexes of a pavement spot ({@link #PavementMinHexes}). */
    public int getPavementMinHexes() {
        return PavementMinHexes;
    }

    /** @param PavementMinHexes the minimum pavement spot size (hexes) to set. */
    public void setPavementMinHexes(int PavementMinHexes) {
        this.PavementMinHexes = PavementMinHexes;
    }

    /** @return maximum number of pavement spots ({@link #PavementMaxSpots}). */
    public int getPavementMaxSpots() {
        return PavementMaxSpots;
    }

    /** @param PavementMaxSpots the maximum number of pavement spots to set. */
    public void setPavementMaxSpots(int PavementMaxSpots) {
        this.PavementMaxSpots = PavementMaxSpots;
    }

    /** @return maximum size in hexes of a pavement spot ({@link #PavementMaxHexes}). */
    public int getPavementMaxHexes() {
        return PavementMaxHexes;
    }

    /** @param PavementMaxHexes the maximum pavement spot size (hexes) to set. */
    public void setPavementMaxHexes(int PavementMaxHexes) {
        this.PavementMaxHexes = PavementMaxHexes;
    }

    /** @return minimum number of ice spots ({@link #IceMinSpots}). */
    public int getIceMinSpots() {
        return IceMinSpots;
    }

    /** @param IceMinSpots the minimum number of ice spots to set. */
    public void setIceMinSpots(int IceMinSpots) {
        this.IceMinSpots = IceMinSpots;
    }

    /** @return minimum size in hexes of an ice spot ({@link #IceMinHexes}). */
    public int getIceMinHexes() {
        return IceMinHexes;
    }

    /** @param IceMinHexes the minimum ice spot size (hexes) to set. */
    public void setIceMinHexes(int IceMinHexes) {
        this.IceMinHexes = IceMinHexes;
    }

    /** @return maximum number of ice spots ({@link #IceMaxSpots}). */
    public int getIceMaxSpots() {
        return IceMaxSpots;
    }

    /** @param IceMaxSpots the maximum number of ice spots to set. */
    public void setIceMaxSpots(int IceMaxSpots) {
        this.IceMaxSpots = IceMaxSpots;
    }

    /** @return maximum size in hexes of an ice spot ({@link #IceMaxHexes}). */
    public int getIceMaxHexes() {
        return IceMaxHexes;
    }

    /** @param IceMaxHexes the maximum ice spot size (hexes) to set. */
    public void setIceMaxHexes(int IceMaxHexes) {
        this.IceMaxHexes = IceMaxHexes;
    }

    /** @return minimum number of rubble spots ({@link #RubbleMinSpots}). */
    public int getRubbleMinSpots() {
        return RubbleMinSpots;
    }

    /** @param RubbleMinSpots the minimum number of rubble spots to set. */
    public void setRubbleMinSpots(int RubbleMinSpots) {
        this.RubbleMinSpots = RubbleMinSpots;
    }

    /** @return minimum size in hexes of a rubble spot ({@link #RubbleMinHexes}). */
    public int getRubbleMinHexes() {
        return RubbleMinHexes;
    }

    /** @param RubbleMinHexes the minimum rubble spot size (hexes) to set. */
    public void setRubbleMinHexes(int RubbleMinHexes) {
        this.RubbleMinHexes = RubbleMinHexes;
    }

    /** @return maximum number of rubble spots ({@link #RubbleMaxSpots}). */
    public int getRubbleMaxSpots() {
        return RubbleMaxSpots;
    }

    /** @param RubbleMaxSpots the maximum number of rubble spots to set. */
    public void setRubbleMaxSpots(int RubbleMaxSpots) {
        this.RubbleMaxSpots = RubbleMaxSpots;
    }

    /** @return maximum size in hexes of a rubble spot ({@link #RubbleMaxHexes}). */
    public int getRubbleMaxHexes() {
        return RubbleMaxHexes;
    }

    /** @param RubbleMaxHexes the maximum rubble spot size (hexes) to set. */
    public void setRubbleMaxHexes(int RubbleMaxHexes) {
        this.RubbleMaxHexes = RubbleMaxHexes;
    }

    /** @return minimum number of fortified-terrain spots ({@link #FortifiedMinSpots}). */
    public int getFortifiedMinSpots() {
        return FortifiedMinSpots;
    }

    /** @param FortifiedMinSpots the minimum number of fortified-terrain spots to set. */
    public void setFortifiedMinSpots(int FortifiedMinSpots) {
        this.FortifiedMinSpots = FortifiedMinSpots;
    }

    /** @return minimum size in hexes of a fortified-terrain spot ({@link #FortifiedMinHexes}). */
    public int getFortifiedMinHexes() {
        return FortifiedMinHexes;
    }

    /** @param FortifiedMinHexes the minimum fortified-terrain spot size (hexes) to set. */
    public void setFortifiedMinHexes(int FortifiedMinHexes) {
        this.FortifiedMinHexes = FortifiedMinHexes;
    }

    /** @return maximum number of fortified-terrain spots ({@link #FortifiedMaxSpots}). */
    public int getFortifiedMaxSpots() {
        return FortifiedMaxSpots;
    }

    /** @param FortifiedMaxSpots the maximum number of fortified-terrain spots to set. */
    public void setFortifiedMaxSpots(int FortifiedMaxSpots) {
        this.FortifiedMaxSpots = FortifiedMaxSpots;
    }

    public int getFortifiedMaxHexes() {
        return FortifiedMaxHexes;
    }

    /** @param FortifiedMaxHexes the maximum fortified-terrain spot size (hexes) to set. */
    public void setFortifiedMaxHexes(int FortifiedMaxHexes) {
        this.FortifiedMaxHexes = FortifiedMaxHexes;
    }

    /** @return maximum number of buildings ({@link #MaxBuildings}). */
    public int getMaxBuildings() {
        return MaxBuildings;
    }

    /** @param Buildings the maximum number of buildings to set. */
    public void setMaxBuildings(int Buildings) {
        this.MaxBuildings = Buildings;
    }

    /** @return minimum number of buildings ({@link #MinBuildings}). */
    public int getMinBuildings() {
        return MinBuildings;
    }

    /** @param Buildings the minimum number of buildings to set. */
    public void setMinBuildings(int Buildings) {
        this.MinBuildings = Buildings;
    }

    /** @return maximum building Construction Factor ({@link #MaxCF}). */
    public int getMaxCF() {
        return MaxCF;
    }

    /** @param CF the maximum building Construction Factor to set. */
    public void setMaxCF(int CF) {
        this.MaxCF = CF;
    }

    /** @return minimum building Construction Factor ({@link #MinCF}). */
    public int getMinCF() {
        return MinCF;
    }

    /** @param CF the minimum building Construction Factor to set. */
    public void setMinCF(int CF) {
        this.MinCF = CF;
    }

    /** @return maximum number of building floors ({@link #MaxFloors}). */
    public int getMaxFloors() {
        return MaxFloors;
    }

    /** @param Floors the maximum number of building floors to set. */
    public void setMaxFloors(int Floors) {
        this.MaxFloors = Floors;
    }

    /** @return minimum number of building floors ({@link #MinFloors}). */
    public int getMinFloors() {
        return MinFloors;
    }

    /** @param Floors the minimum number of building floors to set. */
    public void setMinFloors(int Floors) {
        this.MinFloors = Floors;
    }

    /** @return city building density, 0-100 scale ({@link #CityDensity}). */
    public int getCityDensity() {
        return CityDensity;
    }

    /** @param types the city density value to set. */
    public void setCityDensity(int types) {
        this.CityDensity = types;
    }

    /** @return road count/frequency in a generated city ({@link #Roads}). */
    public int getRoads() {
        return Roads;
    }

    /** @param Roads the road count/frequency to set. */
    public void setRoads(int Roads) {
        this.Roads = Roads;
    }

    /** @return city block layout type used by the map generator ({@link #CityType}). */
    public String getCityType() {
        return CityType;
    }

    /** @param types the city layout type to set. */
    public void setCityType(String types) {
        this.CityType = types;
    }

    /** @return the hilliness/roughness intensity used to classify the landscape ({@link #Hilliness}). */
    public int getHillyness() {
        return Hilliness;
    }

    /** @param Hillyness the hilliness intensity to set. */
    public void setHillyness(int Hillyness) {
        this.Hilliness = Hillyness;
    }

    /** @return minimum number of forest spots ({@link #ForestMinSpots}). */
    public int getForestMinSpots() {
        return ForestMinSpots;
    }

    /** @param ForestMinSpots the minimum number of forest spots to set. */
    public void setForestMinSpots(int ForestMinSpots) {
        this.ForestMinSpots = ForestMinSpots;
    }

    /** @return the hill elevation step range ({@link #HillElevationRange}). */
    public int getHillElevationRange() {
        return HillElevationRange;
    }

    /** @param HillElevationRange the hill elevation step range to set. */
    public void setHillElevationRange(int HillElevationRange) {
        this.HillElevationRange = HillElevationRange;
    }

    /** @return minimum size in hexes of a forest spot ({@link #ForestMinHexes}). */
    public int getForestMinHexes() {
        return ForestMinHexes;
    }

    /** @param ForestMinHexes the minimum forest spot size (hexes) to set. */
    public void setForestMinHexes(int ForestMinHexes) {
        this.ForestMinHexes = ForestMinHexes;
    }

    /** @return maximum number of forest spots ({@link #ForestMaxSpots}). */
    public int getForestMaxSpots() {
        return ForestMaxSpots;
    }

    /** @param ForestMaxSpots the maximum number of forest spots to set. */
    public void setForestMaxSpots(int ForestMaxSpots) {
        this.ForestMaxSpots = ForestMaxSpots;
    }

    /** @return maximum size in hexes of a forest spot ({@link #ForestMaxHexes}). */
    public int getForestMaxHexes() {
        return ForestMaxHexes;
    }

    /** @param ForestMaxHexes the maximum forest spot size (hexes) to set. */
    public void setForestMaxHexes(int ForestMaxHexes) {
        this.ForestMaxHexes = ForestMaxHexes;
    }

    /** @return percent chance a forest spot is heavy woods/jungle ({@link #ForestHeavyProb}). */
    public int getForestHeavyProb() {
        return ForestHeavyProb;
    }

    /** @param ForestHeavyProb the heavy-woods probability to set. */
    public void setForestHeavyProb(int ForestHeavyProb) {
        this.ForestHeavyProb = ForestHeavyProb;
    }

    /** @return percent chance craters are generated at all ({@link #CraterProb}). */
    public int getCraterProb() {
        return CraterProb;
    }

    /** @param CraterProb the crater-generation probability to set. */
    public void setCraterProb(int CraterProb) {
        this.CraterProb = CraterProb;
    }

    /** @return minimum crater radius in hexes ({@link #CraterMinRadius}). */
    public int getCraterMinRadius() {
        return CraterMinRadius;
    }

    /** @param CraterMinRadius the minimum crater radius (hexes) to set. */
    public void setCraterMinRadius(int CraterMinRadius) {
        this.CraterMinRadius = CraterMinRadius;
    }

    /** @return maximum crater radius in hexes ({@link #CraterMaxRadius}). */
    public int getCraterMaxRadius() {
        return CraterMaxRadius;
    }

    /** @param CraterMaxRadius the maximum crater radius (hexes) to set. */
    public void setCraterMaxRadius(int CraterMaxRadius) {
        this.CraterMaxRadius = CraterMaxRadius;
    }

    /** @return minimum number of craters ({@link #CraterMinNum}). */
    public int getCraterMinNum() {
        return CraterMinNum;
    }

    /** @param CraterMinNum the minimum number of craters to set. */
    public void setCraterMinNum(int CraterMinNum) {
        this.CraterMinNum = CraterMinNum;
    }

    /** @return maximum number of craters ({@link #CraterMaxNum}). */
    public int getCraterMaxNum() {
        return CraterMaxNum;
    }

    /** @param CraterMaxNum the maximum number of craters to set. */
    public void setCraterMaxNum(int CraterMaxNum) {
        this.CraterMaxNum = CraterMaxNum;
    }

    /** @return which map-generation algorithm/strategy is selected ({@link #Algorithm}). */
    public int getAlgorithm() {
        return Algorithm;
    }

    /** @param Algorithm the map-generation algorithm selector to set. */
    public void setAlgorithm(int Algorithm) {
        this.Algorithm = Algorithm;
    }

    /** @return percent chance cliffs are generated ({@link #CliffProb}). */
    public int getCliffProb() {
        return CliffProb;
    }

    /** @param prob the cliff-generation probability to set. */
    public void setCliffProb(int prob) {
        this.CliffProb = prob;
    }

    /** @return the negative-elevation terrain inversion setting ({@link #InvertNegativeTerrain}). */
    public int getInvertNegativeTerrain() {
        return InvertNegativeTerrain;
    }

    /** @param invert the negative-elevation terrain inversion setting to set. */
    public void setInvertNegativeTerrain(int invert) {
        this.InvertNegativeTerrain = invert;
    }

    /** @return the generated town/city size category ({@link #TownSize}). */
    public int getTownSize() {
        return TownSize;
    }

    /** @param amount the town/city size category to set. */
    public void setTownSize(int amount) {
        this.TownSize = amount;
    }

    /** @return number of mountain peaks to generate ({@link #MountPeaks}). */
    public int getMountPeaks() {
        return MountPeaks;
    }

    /** @param amount the number of mountain peaks to set. */
    public void setMountPeaks(int amount) {
        this.MountPeaks = amount;
    }

    /** @return minimum mountain range width in hexes ({@link #MountWidthMin}). */
    public int getMountWidthMin() {
        return MountWidthMin;
    }

    /** @param amount the minimum mountain range width (hexes) to set. */
    public void setMountWidthMin(int amount) {
        this.MountWidthMin = amount;
    }

    /** @return maximum mountain range width in hexes ({@link #MountWidthMax}). */
    public int getMountWidthMax() {
        return MountWidthMax;
    }

    /** @param amount the maximum mountain range width (hexes) to set. */
    public void setMountWidthMax(int amount) {
        this.MountWidthMax = amount;
    }

    /** @return minimum mountain height/elevation ({@link #MountHeightMin}). */
    public int getMountHeightMin() {
        return MountHeightMin;
    }

    /** @param amount the minimum mountain height to set. */
    public void setMountHeightMin(int amount) {
        this.MountHeightMin = amount;
    }

    /** @return maximum mountain height/elevation ({@link #MountHeightMax}). */
    public int getMountHeightMax() {
        return MountHeightMax;
    }

    /** @param amount the maximum mountain height to set. */
    public void setMountHeightMax(int amount) {
        this.MountHeightMax = amount;
    }

    /** @return the mountain generation style/shape variant ({@link #MountStyle}). */
    public int getMountStyle() {
        return MountStyle;
    }

    /** @param amount the mountain style variant to set. */
    public void setMountStyle(int amount) {
        this.MountStyle = amount;
    }

    /** @return this environment's relative selection weight among sibling environments ({@link #EnvironmentProb}). */
    public int getEnvironmentalProb() {
        return EnvironmentProb;
    }

    /** @param prob the relative selection weight to set. */
    public void setEnvironmentalProb(int prob) {
        EnvironmentProb = prob;
    }

    /** @return percent chance hill elevation is inverted ({@link #HillInvertProb}). */
    public int getHillInvertProb() {
        return HillInvertProb;
    }

    /** @param HillInvertProb the hill elevation inversion probability to set. */
    public void setHillInvertProb(int HillInvertProb) {
        this.HillInvertProb = HillInvertProb;
    }

    /** @return the special-effects modifier ({@link #fxMod}). */
    public int getFxMod() {
        return fxMod;
    }

    /** @param mod the special-effects modifier to set. */
    public void setFxMod(int mod) {
        this.fxMod = mod;
    }

    /** @return percent chance of a forest fire special effect ({@link #probForestFire}). */
    public int getProbForestFire() {
        return probForestFire;
    }

    /** @param prob the forest-fire probability to set. */
    public void setProbForestFire(int prob) {
        this.probForestFire = prob;
    }

    /** @return percent chance of a freeze/ice special effect ({@link #probFreeze}). */
    public int getProbFreeze() {
        return probFreeze;
    }

    /** @param prob the freeze probability to set. */
    public void setProbFreeze(int prob) {
        this.probFreeze = prob;
    }

    /** @return percent chance of a flood special effect ({@link #probFlood}). */
    public int getProbFlood() {
        return probFlood;
    }

    /** @param prob the flood probability to set. */
    public void setProbFlood(int prob) {
        this.probFlood = prob;
    }

    /** @return percent chance of a drought special effect ({@link #probDrought}). */
    public int getProbDrought() {
        return probDrought;
    }

    /** @param prob the drought probability to set. */
    public void setProbDrought(int prob) {
        this.probDrought = prob;
    }

    /**
     * Writes this environment's full generation ruleset (all terrain/building/mountain/special-effect/static-map
     * fields) as a binary stream. Must stay in sync field-for-field with {@link #binIn(BinReader, CampaignData)}.
     */
    public void binOut(BinWriter out) throws IOException {
        out.println(id, "id");
        out.println(Name, "name");
        out.println(CraterProb, "CraterProb");
        out.println(CraterMinNum, "CraterMinNum");
        out.println(CraterMaxNum, "CraterMaxNum");
        out.println(CraterMinRadius, "CraterMinRadius");
        out.println(CraterMaxRadius, "CraterMaxRadius");
        out.println(Hilliness, "Hilliness");
        out.println(HillElevationRange, "HillElevationRange");
        out.println(HillInvertProb, "HillInvertProb");
        out.println(WaterMinSpots, "WaterMinSpots");
        out.println(WaterMaxSpots, "WaterMaxSpots");
        out.println(WaterMinHexes, "WaterMinHexes");
        out.println(WaterMaxHexes, "WaterMaxHexes");
        out.println(WaterDeepProb, "WaterDeepProb");
        out.println(ForestMinSpots, "ForestMinSpots");
        out.println(ForestMaxSpots, "ForestMaxSpots");
        out.println(ForestMinHexes, "ForestMinHexes");
        out.println(ForestMaxHexes, "ForestMaxHexes");
        out.println(ForestHeavyProb, "ForestHeavyProb");
        out.println(RoughMinSpots, "RoughMinSpots");
        out.println(RoughMaxSpots, "RoughMaxSpots");
        out.println(RoughMinHexes, "RoughMinHexes");
        out.println(RoughMaxHexes, "RoughMaxHexes");
        out.println(SwampMinSpots, "SwampMinSpots");
        out.println(SwampMaxSpots, "SwampMaxSpots");
        out.println(SwampMinHexes, "SwampMinHexes");
        out.println(SwampMaxHexes, "SwampMaxHexes");
        out.println(PavementMinSpots, "PavementMinSpots");
        out.println(PavementMaxSpots, "PavementMaxSpots");
        out.println(PavementMinHexes, "PavementMinHexes");
        out.println(PavementMaxHexes, "PavementMaxHexes");
        out.println(fxMod, "fxMod");
        out.println(probForestFire, "probForestFire");
        out.println(probFreeze, "probFreeze");
        out.println(probFlood, "probFlood");
        out.println(probDrought, "probDrought");
        out.println(CliffProb, "CliffProb");
        out.println(InvertNegativeTerrain, "InvertNegativeTerrain");
        out.println(RoadProb, "RoadProb");
        out.println(RiverProb, "RiverProb");
        out.println(Algorithm, "Algorithm");
        out.println(Theme, "Theme");
        out.println(IceMinSpots, "IceMinSpots");
        out.println(IceMaxSpots, "IceMaxSpots");
        out.println(IceMinHexes, "IceMinHexes");
        out.println(IceMaxHexes, "IceMaxHexes");

        out.println(RubbleMinSpots, "RubbleMinSpots");
        out.println(RubbleMaxSpots, "RubbleMaxSpots");
        out.println(RubbleMinHexes, "RubbleMinHexes");
        out.println(RubbleMaxHexes, "RubbleMaxHexes");

        out.println(SandMinSpots, "SandMinSpots");
        out.println(SandMaxSpots, "SandMaxSpots");
        out.println(SandMinHexes, "SandMinHexes");
        out.println(SandMaxHexes, "SandMaxHexes");

        out.println(PlantedFieldMinSpots, "PlantedFieldMinSpots");
        out.println(PlantedFieldMaxSpots, "PlantedFieldMaxSpots");
        out.println(PlantedFieldMinHexes, "PlantedFieldMinHexes");
        out.println(PlantedFieldMaxHexes, "PlantedFieldMaxHexes");

        out.println(FortifiedMinSpots, "FortifiedMinSpots");
        out.println(FortifiedMaxSpots, "FortifiedMaxSpots");
        out.println(FortifiedMinHexes, "FortifiedMinHexes");
        out.println(FortifiedMaxHexes, "FortifiedMaxHexes");

        out.println(MinBuildings, "MinBuildings");
        out.println(MaxBuildings, "MaxBuildings");
        out.println(MinCF, "MinCF");
        out.println(MaxCF, "MaxCF");
        out.println(MinFloors, "MinFloors");
        out.println(MaxFloors, "MaxFloors");
        out.println(CityDensity, "CityDensity");
        out.println(CityType, "CityType");
        out.println(Roads, "Roads");
        out.println(TownSize, "TownSize");
        out.println(MountPeaks, "MountPeaks");
        out.println(MountWidthMin, "MountWidthMin");
        out.println(MountWidthMax, "MountWidthMax");
        out.println(MountHeightMin, "MountHeightMin");
        out.println(MountHeightMax, "MountHeightMax");
        out.println(MountStyle, "MountStyle");
        out.println(staticMap, "staticMap");
        out.println(staticMapName, "staticMapName");
        out.println(xSize, "xSize");
        out.println(ySize, "ySize");
        out.println(xBoardSize, "xBoardSize");
        out.println(yBoardSize, "yBoardSize");

    }

    /**
     * Reads this environment's full generation ruleset back from a binary stream previously written by
     * {@link #binOut}.
     * <p>
     * Note: {@link #EnvironmentProb} (the environment's relative selection weight) and the static-map name/size
     * fields' companion data are handled asymmetrically between the string ({@code toString}/constructor) and binary
     * ({@code binIn}/{@code binOut}) persistence forms in this class — double-check that a given deployment is
     * consistently using one persistence path, since {@code EnvironmentProb} in particular is not round-tripped here.
     *
     * @param in   the binary reader positioned at the start of this environment's data
     * @param data the owning campaign data (currently unused by this method but kept for a consistent signature with
     *             other {@code binIn} methods in the persistence layer)
     */
    public void binIn(BinReader in, CampaignData data) throws IOException {
        id = in.readInt("id");
        Name = in.read("name");
        CraterProb = in.readInt("CraterProb");
        CraterMinNum = in.readInt("CraterMinNum");
        CraterMaxNum = in.readInt("CraterMaxNum");
        CraterMinRadius = in.readInt("CraterMinRadius");
        CraterMaxRadius = in.readInt("CraterMaxRadius");
        Hilliness = in.readInt("Hilliness");
        HillElevationRange = in.readInt("HillElevationRange");
        HillInvertProb = in.readInt("HillInvertProb");
        WaterMinSpots = in.readInt("WaterMinSpots");
        WaterMaxSpots = in.readInt("WaterMaxSpots");
        WaterMinHexes = in.readInt("WaterMinHexes");
        WaterMaxHexes = in.readInt("WaterMaxHexes");
        WaterDeepProb = in.readInt("WaterDeepProb");
        ForestMinSpots = in.readInt("ForestMinSpots");
        ForestMaxSpots = in.readInt("ForestMaxSpots");
        ForestMinHexes = in.readInt("ForestMinHexes");
        ForestMaxHexes = in.readInt("ForestMaxHexes");
        ForestHeavyProb = in.readInt("ForestHeavyProb");
        RoughMinSpots = in.readInt("RoughMinSpots");
        RoughMaxSpots = in.readInt("RoughMaxSpots");
        RoughMinHexes = in.readInt("RoughMinHexes");
        RoughMaxHexes = in.readInt("RoughMaxHexes");
        SwampMinSpots = in.readInt("SwampMinSpots");
        SwampMaxSpots = in.readInt("SwampMaxSpots");
        SwampMinHexes = in.readInt("SwampMinHexes");
        SwampMaxHexes = in.readInt("SwampMaxHexes");
        PavementMinSpots = in.readInt("PavementMinSpots");
        PavementMaxSpots = in.readInt("PavementMaxSpots");
        PavementMinHexes = in.readInt("PavementMinHexes");
        PavementMaxHexes = in.readInt("PavementMaxHexes");
        fxMod = in.readInt("fxMod");
        probForestFire = in.readInt("probForestFire");
        probFreeze = in.readInt("probFreeze");
        probFlood = in.readInt("probFlood");
        probDrought = in.readInt("probDrought");
        CliffProb = in.readInt("CliffProb");
        InvertNegativeTerrain = in.readInt("InvertNegativeTerrain");
        RoadProb = in.readInt("RoadProb");
        RiverProb = in.readInt("RiverProb");
        Algorithm = in.readInt("Algorithm");
        Theme = in.read("Theme");
        IceMinSpots = in.readInt("IceMinSpots");
        IceMaxSpots = in.readInt("IceMaxSpots");
        IceMinHexes = in.readInt("IceMinHexes");
        IceMaxHexes = in.readInt("IceMaxHexes");

        RubbleMinSpots = in.readInt("RubbleMinSpots");
        RubbleMaxSpots = in.readInt("RubbleMaxSpots");
        RubbleMinHexes = in.readInt("RubbleMinHexes");
        RubbleMaxHexes = in.readInt("RubbleMaxHexes");

        SandMinSpots = in.readInt("SandMinSpots");
        SandMaxSpots = in.readInt("SandMaxSpots");
        SandMinHexes = in.readInt("SandMinHexes");
        SandMaxHexes = in.readInt("SandMaxHexes");

        PlantedFieldMinSpots = in.readInt("PlantedFieldMinSpots");
        PlantedFieldMaxSpots = in.readInt("PlantedFieldMaxSpots");
        PlantedFieldMinHexes = in.readInt("PlantedFieldMinHexes");
        PlantedFieldMaxHexes = in.readInt("PlantedFieldMaxHexes");

        FortifiedMinSpots = in.readInt("FortifiedMinSpots");
        FortifiedMaxSpots = in.readInt("FortifiedMaxSpots");
        FortifiedMinHexes = in.readInt("FortifiedMinHexes");
        FortifiedMaxHexes = in.readInt("FortifiedMaxHexes");

        MinBuildings = in.readInt("MinBuildings");
        MaxBuildings = in.readInt("MaxBuildings");
        MinCF = in.readInt("MinCF");
        MaxCF = in.readInt("MaxCF");
        MinFloors = in.readInt("MinFloors");
        MaxFloors = in.readInt("MaxFloors");
        CityDensity = in.readInt("CityDensity");
        CityType = in.read("CityType");
        Roads = in.readInt("Roads");
        TownSize = in.readInt("TownSize");
        MountPeaks = in.readInt("MountPeaks");
        MountWidthMin = in.readInt("MountWidthMin");
        MountWidthMax = in.readInt("MountWidthMax");
        MountHeightMin = in.readInt("MountHeightMin");
        MountHeightMax = in.readInt("MountHeightMax");
        MountStyle = in.readInt("MountStyle");
        staticMap = in.readBoolean("staticMap");
        staticMapName = in.read("staticMapName");
        xSize = in.readInt("xSize");
        ySize = in.readInt("ySize");
        xBoardSize = in.readInt("xBoardSize");
        yBoardSize = in.readInt("yBoardSize");

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

    /** @return the visual/graphical theme (tileset) name applied to the generated board ({@link #Theme}). */
    public String getTheme() {
        return Theme;
    }

    /**
     * @param theme the theme name to set. Quirk: if {@code theme} is empty or a single character (length &le; 1),
     *              it is silently replaced with a single space {@code " "} instead of being stored as given or
     *              rejected.
     */
    public void setTheme(String theme) {
        if (theme.length() <= 1) {
            theme = " ";
        }

        Theme = theme;
    }

    /** @return whether this environment uses a pre-built static map instead of procedural generation ({@link #staticMap}). */
    public boolean isStaticMap() {
        return staticMap;
    }

    /** @param map whether to use a static (pre-built) map. */
    public void setStaticMap(boolean map) {
        staticMap = map;
    }

    /** @return the static map's name ({@link #staticMapName}), used only when {@link #isStaticMap()} is true. */
    public String getStaticMapName() {
        return staticMapName;
    }

    /** @param name the static map name to set. */
    public void setStaticMapName(String name) {
        staticMapName = name;
    }

    /** @return static map width in boards ({@link #xSize}). */
    public int getXSize() {
        return xSize;
    }

    /** @param xSize the static map width (boards) to set. */
    public void setXSize(int xSize) {
        this.xSize = xSize;
    }

    /** @return static map height in boards ({@link #ySize}). */
    public int getYSize() {
        return ySize;
    }

    /** @param ySize the static map height (boards) to set. */
    public void setYSize(int ySize) {
        this.ySize = ySize;
    }

    /** @return overall game board width in boards ({@link #xBoardSize}). */
    public int getXBoardSize() {
        return xBoardSize;
    }

    /** @param size the overall board width (boards) to set. */
    public void setXBoardSize(int size) {
        xBoardSize = size;
    }

    /** @return overall game board height in boards ({@link #yBoardSize}). */
    public int getYBoardSize() {
        return yBoardSize;
    }

    /** @param size the overall board height (boards) to set. */
    public void setYBoardSize(int size) {
        yBoardSize = size;
    }

    /** @return minimum number of sand spots ({@link #SandMinSpots}). */
    public int getSandMinSpots() {
        return SandMinSpots;
    }

    /** @param sandMinSpots the minimum number of sand spots to set. */
    public void setSandMinSpots(int sandMinSpots) {
        SandMinSpots = sandMinSpots;
    }

    /** @return maximum number of sand spots ({@link #SandMaxSpots}). */
    public int getSandMaxSpots() {
        return SandMaxSpots;
    }

    /** @param sandMaxSpots the maximum number of sand spots to set. */
    public void setSandMaxSpots(int sandMaxSpots) {
        SandMaxSpots = sandMaxSpots;
    }

    /** @return minimum size in hexes of a sand spot ({@link #SandMinHexes}). */
    public int getSandMinHexes() {
        return SandMinHexes;
    }

    /** @param sandMinHexes the minimum sand spot size (hexes) to set. */
    public void setSandMinHexes(int sandMinHexes) {
        SandMinHexes = sandMinHexes;
    }

    /** @return maximum size in hexes of a sand spot ({@link #SandMaxHexes}). */
    public int getSandMaxHexes() {
        return SandMaxHexes;
    }

    /** @param sandMaxHexes the maximum sand spot size (hexes) to set. */
    public void setSandMaxHexes(int sandMaxHexes) {
        SandMaxHexes = sandMaxHexes;
    }

    /** @return minimum number of planted-field (farmland) spots ({@link #PlantedFieldMinSpots}). */
    public int getPlantedFieldMinSpots() {
        return PlantedFieldMinSpots;
    }

    /** @param plantedFieldMinSpots the minimum number of planted-field spots to set. */
    public void setPlantedFieldMinSpots(int plantedFieldMinSpots) {
        PlantedFieldMinSpots = plantedFieldMinSpots;
    }

    /** @return minimum size in hexes of a planted-field spot ({@link #PlantedFieldMinHexes}). */
    public int getPlantedFieldMinHexes() {
        return PlantedFieldMinHexes;
    }

    /** @param plantedFieldMinHexes the minimum planted-field spot size (hexes) to set. */
    public void setPlantedFieldMinHexes(int plantedFieldMinHexes) {
        PlantedFieldMinHexes = plantedFieldMinHexes;
    }

    /** @return maximum number of planted-field spots ({@link #PlantedFieldMaxSpots}). */
    public int getPlantedFieldMaxSpots() {
        return PlantedFieldMaxSpots;
    }

    /** @param plantedFieldMaxSpots the maximum number of planted-field spots to set. */
    public void setPlantedFieldMaxSpots(int plantedFieldMaxSpots) {
        PlantedFieldMaxSpots = plantedFieldMaxSpots;
    }

    /** @return maximum size in hexes of a planted-field spot ({@link #PlantedFieldMaxHexes}). */
    public int getPlantedFieldMaxHexes() {
        return PlantedFieldMaxHexes;
    }

    /** @param plantedFieldMaxHexes the maximum planted-field spot size (hexes) to set. */
    public void setPlantedFieldMaxHexes(int plantedFieldMaxHexes) {
        PlantedFieldMaxHexes = plantedFieldMaxHexes;
    }

}
