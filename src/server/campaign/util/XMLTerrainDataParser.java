/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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
 * Created on 04.05.2004
 *
 */
package server.campaign.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

import common.PlanetEnvironment;
import common.Terrain;
import common.util.MWLogger;
import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;
import server.campaign.CampaignMain;

/**
 * @author Helge Richter
 */
public class XMLTerrainDataParser implements XMLResponder {
    private String prefix;
    String lastElement = "";
    String name;
    String filename;

    // Planet Environment
    // Crater
    int CraterProb;
    int CraterMinNum;
    int CraterMaxNum;
    int CraterMinRadius;
    int CraterMaxRadius;

    // Hills
    int Hillyness;
    int HillElevationRange;
    int HillInvertProb;

    // Water
    int WaterMinSpots;
    int WaterMaxSpots;
    int WaterMinHexes;
    int WaterMaxHexes;
    int WaterDeepProb;

    // Forest
    int ForestMinSpots;
    int ForestMaxSpots;
    int ForestMinHexes;
    int ForestMaxHexes;
    int ForestHeavyProb;

    // Rough
    int RoughMinSpots;
    int RoughMaxSpots;
    int RoughMinHexes;
    int RoughMaxHexes;

    // Swamp
    int SwampMinSpots;
    int SwampMaxSpots;
    int SwampMinHexes;
    int SwampMaxHexes;

    // Pavement
    int PavementMinSpots;
    int PavementMaxSpots;
    int PavementMinHexes;
    int PavementMaxHexes;

    // Ice
    int IceMinSpots;
    int IceMaxSpots;
    int IceMinHexes;
    int IceMaxHexes;

    // Rubble
    int RubbleMinSpots;
    int RubbleMaxSpots;
    int RubbleMinHexes;
    int RubbleMaxHexes;

    // Sand
    int SandMinSpots;
    int SandMaxSpots;
    int SandMinHexes;
    int SandMaxHexes;

    // PlantedField
    int PlantedFieldMinSpots;
    int PlantedFieldMaxSpots;
    int PlantedFieldMinHexes;
    int PlantedFieldMaxHexes;
    
    // Fortified
    int FortifiedMinSpots;
    int FortifiedMaxSpots;
    int FortifiedMinHexes;
    int FortifiedMaxHexes;

    // Buildings
    int MinCF;
    int MaxCF;
    int MinFloors;
    int MaxFloors;
    int Roads = 4;
    int CityDensity = 50;
    String CityType = "NONE";
    int TownSize = 0;

    // Speical fx
    int FxMod;
    int ProbForestFire;
    int ProbFreeze;
    int ProbFlood;
    int ProbDrought;

    // Mountains
    private int MountPeaks = 0;
    private int MountWidthMin = 0;
    private int MountWidthMax = 0;
    private int MountHeightMin = 0;
    private int MountHeightMax = 0;
    private int MountStyle = 0;

    // Misc
    int RoadProb;
    int RiverProb;
    int Algorithm;
    int CliffProb;
    int InvertNegativeTerrain;
    int environmentProb = 1;
    
    //static maps
    int xmap = 1;
    int ymap = 1;
    int xboard = 16;
    int yboard = 17;
    boolean map = false;
    String mapname = "";

    
    String Theme = "";
    Terrain planetTerrain = new Terrain();
    
    public XMLTerrainDataParser(String filename) {
        this.filename = filename;
        try {
            XMLParser xp = new XMLParser();
            xp.parseXML(this);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void recordNotationDeclaration(String name, String pubID, String sysID) throws ParseException {
        System.out.print(prefix + "!NOTATION: " + name);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        MWLogger.mainLog("");
    }

    public void recordEntityDeclaration(String name, String value, String pubID, String sysID, String notation) throws ParseException {
        System.out.print(prefix + "!ENTITY: " + name);
        if (value != null)
            System.out.print("  value = " + value);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        if (notation != null)
            System.out.print("  notation = " + notation);
        MWLogger.mainLog("");
    }

    public void recordElementDeclaration(String name, String content) throws ParseException {
        System.out.print(prefix + "!ELEMENT: " + name);
        MWLogger.mainLog("  content = " + content);
    }

    public void recordAttlistDeclaration(String element, String attr, boolean notation, String type, String defmod, String def) throws ParseException {
        System.out.print(prefix + "!ATTLIST: " + element);
        System.out.print("  attr = " + attr);
        System.out.print("  type = " + ((notation) ? "NOTATIONS " : "") + type);
        System.out.print("  def. modifier = " + defmod);
        MWLogger.mainLog((def == null) ? "" : "  def = " + notation);
    }

    public void recordDoctypeDeclaration(String name, String pubID, String sysID) throws ParseException {
        System.out.print(prefix + "!DOCTYPE: " + name);
        if (pubID != null)
            System.out.print("  pubID = " + pubID);
        if (sysID != null)
            System.out.print("  sysID = " + sysID);
        MWLogger.mainLog("");
        prefix = "";
    }

    /* DOC METHDODS */

    public void recordDocStart() {
    }

    public void recordDocEnd() {
        MWLogger.mainLog("");
        MWLogger.mainLog("Parsing finished without error");
    }

    @SuppressWarnings("rawtypes")
	public void recordElementStart(String name, Hashtable attr) throws ParseException {
        // MWLogger.mainLog(prefix+"Element: "+name);
        lastElement = name;
    }

    public void recordElementEnd(String tagName) throws ParseException {
        MWLogger.mainLog("ENVIRONMENT READ");
        if (tagName.equals("ENVIRONMENT")) {
            PlanetEnvironment PE = new PlanetEnvironment();

            PE.setCraterProb(CraterProb);
            PE.setCraterMinNum(CraterMinNum);
            PE.setCraterMaxNum(CraterMaxNum);
            PE.setCraterMinRadius(CraterMinRadius);
            PE.setCraterMaxRadius(CraterMaxRadius);
            PE.setHillyness(Hillyness);
            PE.setHillElevationRange(HillElevationRange);
            PE.setHillInvertProb(HillInvertProb);
            PE.setWaterMinSpots(WaterMinSpots);
            PE.setWaterMaxSpots(WaterMaxSpots);
            PE.setWaterMinHexes(WaterMinHexes);
            PE.setWaterMaxHexes(WaterMaxHexes);
            PE.setWaterDeepProb(WaterDeepProb);
            PE.setForestMinSpots(ForestMinSpots);
            PE.setForestMaxSpots(ForestMaxSpots);
            PE.setForestMinHexes(ForestMinHexes);
            PE.setForestMaxHexes(ForestMaxHexes);
            PE.setForestHeavyProb(ForestHeavyProb);
            PE.setRoughMinSpots(RoughMinSpots);
            PE.setRoughMaxSpots(RoughMaxSpots);
            PE.setRoughMinHexes(RoughMinHexes);
            PE.setRoughMaxHexes(RoughMaxHexes);

            PE.setSwampMinSpots(SwampMinSpots);
            PE.setSwampMaxSpots(SwampMaxSpots);
            PE.setSwampMinHexes(SwampMinHexes);
            PE.setSwampMaxHexes(SwampMaxHexes);

            PE.setPavementMinSpots(PavementMinSpots);
            PE.setPavementMaxSpots(PavementMaxSpots);
            PE.setPavementMinHexes(PavementMinHexes);
            PE.setPavementMaxHexes(PavementMaxHexes);

            PE.setIceMinSpots(IceMinSpots);
            PE.setIceMaxSpots(IceMaxSpots);
            PE.setIceMinHexes(IceMinHexes);
            PE.setIceMaxHexes(IceMaxHexes);

            PE.setRubbleMinSpots(RubbleMinSpots);
            PE.setRubbleMaxSpots(RubbleMaxSpots);
            PE.setRubbleMinHexes(RubbleMinHexes);
            PE.setRubbleMaxHexes(RubbleMaxHexes);

            PE.setSandMinSpots(SandMinSpots);
            PE.setSandMaxSpots(SandMaxSpots);
            PE.setSandMinHexes(SandMinHexes);
            PE.setSandMaxHexes(SandMaxHexes);
            
            PE.setPlantedFieldMinSpots(PlantedFieldMinSpots);
            PE.setPlantedFieldMaxSpots(PlantedFieldMaxSpots);
            PE.setPlantedFieldMinHexes(PlantedFieldMinHexes);
            PE.setPlantedFieldMaxHexes(PlantedFieldMaxHexes);
            
            PE.setFortifiedMinSpots(FortifiedMinSpots);
            PE.setFortifiedMaxSpots(FortifiedMaxSpots);
            PE.setFortifiedMinHexes(FortifiedMinHexes);
            PE.setFortifiedMaxHexes(FortifiedMaxHexes);

            PE.setMinBuildings(0);
            PE.setMaxBuildings(0);
            PE.setMinCF(MinCF);
            PE.setMaxCF(MaxCF);
            PE.setMinFloors(MinFloors);
            PE.setMaxFloors(MaxFloors);
            PE.setCityDensity(CityDensity);
            PE.setCityType(CityType);
            PE.setRoads(Roads);
            PE.setTownSize(TownSize);

            PE.setMountPeaks(MountPeaks);
            PE.setMountWidthMin(MountWidthMin);
            PE.setMountWidthMax(MountWidthMax);
            PE.setMountHeightMin(MountHeightMin);
            PE.setMountHeightMax(MountHeightMax);
            PE.setMountStyle(MountStyle);

            PE.setFxMod(FxMod);
            PE.setProbForestFire(ProbForestFire);
            PE.setProbFreeze(ProbFreeze);
            PE.setProbFlood(ProbFlood);
            PE.setProbDrought(ProbDrought);
            PE.setRoadProb(RoadProb);
            PE.setCliffProb(CliffProb);
            PE.setRiverProb(RiverProb);
            PE.setAlgorithm(Algorithm);
            PE.setTheme(Theme);
            PE.setInvertNegativeTerrain(InvertNegativeTerrain);
            PE.setEnvironmentalProb(environmentProb);
            PE.setName(name);
            
        	PE.setStaticMap(map);
        	PE.setStaticMapName(mapname);
        	PE.setXBoardSize(xboard);
        	PE.setXSize(xmap);
        	PE.setYBoardSize(yboard);
        	PE.setYSize(ymap);

            
            planetTerrain.getEnvironments().add(PE);
            // Reset Variables
            doResetEnvironmentVariables();
        }
        if (tagName.equals("TERRAIN")) {
            planetTerrain.setName(name);
            CampaignMain.cm.getData().addTerrain(planetTerrain);
            planetTerrain = new Terrain();
            name = "";
        }
    }

    private void doResetEnvironmentVariables() {

        
        environmentProb = 1;
        CraterProb = 0;
        CraterMinNum = 0;
        CraterMaxNum = 0;
        CraterMinRadius = 0;
        CraterMaxRadius = 0;

        // Hills
        Hillyness = 100;
        HillElevationRange = 3;
        HillInvertProb = 0;

        // Water
        WaterMinSpots = 3;
        WaterMaxSpots = 8;
        WaterMinHexes = 2;
        WaterMaxHexes = 10;
        WaterDeepProb = 20;

        // Forest
        ForestMinSpots = 4;
        ForestMaxSpots = 8;
        ForestMinHexes = 2;
        ForestMaxHexes = 6;
        ForestHeavyProb = 20;

        // Rough
        RoughMinSpots = 0;
        RoughMaxSpots = 5;
        RoughMinHexes = 1;
        RoughMaxHexes = 2;

        // Swamp
        SwampMinSpots = 0;
        SwampMaxSpots = 0;
        SwampMinHexes = 0;
        SwampMaxHexes = 0;

        // Pavement
        PavementMinSpots = 0;
        PavementMaxSpots = 0;
        PavementMinHexes = 0;
        PavementMaxHexes = 0;

        // Ice
        IceMinSpots = 0;
        IceMaxSpots = 0;
        IceMinHexes = 0;
        IceMaxHexes = 0;

        // Rubble
        RubbleMinSpots = 0;
        RubbleMaxSpots = 0;
        RubbleMinHexes = 0;
        RubbleMaxHexes = 0;

        // Sand
        SandMinSpots = 0;
        SandMaxSpots = 0;
        SandMinHexes = 0;
        SandMaxHexes = 0;

        // PlantedField
        PlantedFieldMinSpots = 0;
        PlantedFieldMaxSpots = 0;
        PlantedFieldMinHexes = 0;
        PlantedFieldMaxHexes = 0;

        // Fortified
        FortifiedMinSpots = 0;
        FortifiedMaxSpots = 0;
        FortifiedMinHexes = 0;
        FortifiedMaxHexes = 0;

        // Buildings
        MinCF = 0;
        MaxCF = 0;
        MinFloors = 0;
        MaxFloors = 0;
        CityDensity = 50;
        Roads = 4;
        CityType = "NONE";
        TownSize = 0;

        // Mountains
        MountPeaks = 0;
        MountWidthMin = 0;
        MountWidthMax = 0;
        MountHeightMin = 0;
        MountHeightMax = 0;
        MountStyle = 0;

        // special FX
        FxMod = 0;
        ProbForestFire = 0;
        ProbFreeze = 0;
        ProbFlood = 0;
        ProbDrought = 0;

        // Misc
        RoadProb = 25;
        RiverProb = 25;
        CliffProb = 25;
        InvertNegativeTerrain = 0;
        Algorithm = 0;

        Theme = "";
    }

    public void recordPI(String name, String pValue) {
        MWLogger.mainLog(prefix + "*" + name + " PI: " + pValue);
    }

    public void recordCharData(String charData) {
        // MWLogger.mainLog(prefix+charData);
        if (!charData.equalsIgnoreCase("")) {
            // do nothing; //MWLogger.mainLog(lastElement + " --> " + charData);
        } else
            lastElement = "";
        if (lastElement.equalsIgnoreCase("NAME")) {
            name = charData;
            MWLogger.mainLog(name);
        } else if (lastElement.equalsIgnoreCase("CRATERPROB"))
            CraterProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CRATERMINNUM"))
            CraterMinNum = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CRATERMAXNUM"))
            CraterMaxNum = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CRATERMINRADIUS"))
            CraterMinRadius = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CRATERMAXRADIUS"))
            CraterMaxRadius = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("HILLYNESS")) {
            Hillyness = Integer.parseInt(charData);
            // fix for new version of MM RMG no uses a range of 0-99
            // Torren 05/27/05 MM version 0.29.79+
            if (Hillyness > 100)
                Hillyness /= 10;
        } else if (lastElement.equalsIgnoreCase("HILLELEVATIONRANGE"))
            HillElevationRange = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("HILLINVERTPROB"))
            HillInvertProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("WATERMINSPOTS"))
            WaterMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("WATERMAXSPOTS"))
            WaterMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("WATERMINHEXES"))
            WaterMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("WATERMAXHEXES"))
            WaterMaxHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("WATERDEEPPROB"))
            WaterDeepProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORESTMINSPOTS"))
            ForestMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORESTMAXSPOTS"))
            ForestMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORESTMINHEXES"))
            ForestMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORESTMAXHEXES"))
            ForestMaxHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORESTHEAVYPROB"))
            ForestHeavyProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ROUGHMINSPOTS"))
            RoughMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ROUGHMAXSPOTS"))
            RoughMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ROUGHMINHEXES"))
            RoughMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ROUGHMAXHEXES"))
            RoughMaxHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SWAMPMINSPOTS"))
            SwampMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SWAMPMAXSPOTS"))
            SwampMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SWAMPMINHEXES"))
            SwampMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SWAMPMAXHEXES"))
            SwampMaxHexes = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("PAVEMENTMINSPOTS"))
            PavementMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PAVEMENTMAXSPOTS"))
            PavementMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PAVEMENTMINHEXES"))
            PavementMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PAVEMENTMAXHEXES"))
            PavementMaxHexes = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("ICEMINSPOTS"))
            IceMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ICEMAXSPOTS"))
            IceMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ICEMINHEXES"))
            IceMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ICEMAXHEXES"))
            IceMaxHexes = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("RUBBLEMINSPOTS"))
            RubbleMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("RUBBLEMAXSPOTS"))
            RubbleMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("RUBBLEMINHEXES"))
            RubbleMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("RUBBLEMAXHEXES"))
            RubbleMaxHexes = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("SANDMINSPOTS"))
            SandMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SANDMAXSPOTS"))
            SandMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SANDMINHEXES"))
            SandMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("SANDMAXHEXES"))
            SandMaxHexes = Integer.parseInt(charData);
        
        else if (lastElement.equalsIgnoreCase("PLANTEDFIELDMINSPOTS"))
            PlantedFieldMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PLANTEDFIELDMAXSPOTS"))
            PlantedFieldMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PLANTEDFIELDMINHEXES"))
            PlantedFieldMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PLANTEDFIELDMAXHEXES"))
            PlantedFieldMaxHexes = Integer.parseInt(charData);
        
        else if (lastElement.equalsIgnoreCase("FORTIFIEDMINSPOTS"))
            FortifiedMinSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORTIFIEDMAXSPOTS"))
            FortifiedMaxSpots = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORTIFIEDMINHEXES"))
            FortifiedMinHexes = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("FORTIFIEDMAXHEXES"))
            FortifiedMaxHexes = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("CITYBLOCKS"))
            Roads = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MINCF"))
            MinCF = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MAXCF"))
            MaxCF = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MINFLOORS"))
            MinFloors = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MAXFLOORS"))
            MaxFloors = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CITYDENSITY"))
            CityDensity = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CITYTYPE"))
            CityType = charData;
        else if (lastElement.equalsIgnoreCase("TOWNSIZE"))
            TownSize = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("MOUNTPEAKS"))
            MountPeaks = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MOUNTWIDTHMIN"))
            MountWidthMin = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MOUNTWIDTHMAX"))
            MountWidthMax = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MOUNTHEIGHTMIN"))
            MountHeightMin = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MOUNTHEIGHTMAX"))
            MountHeightMax = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("MOUNTSTYLE"))
            MountStyle = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("FXMOD"))
            FxMod = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PROBFORESTFIRE"))
            ProbForestFire = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PROBFREEZE"))
            ProbFreeze = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PROBFLOOD"))
            ProbFlood = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("PROBDROUGHT"))
            ProbDrought = Integer.parseInt(charData);

        else if (lastElement.equalsIgnoreCase("THEME"))
            Theme = charData;
        else if (lastElement.equalsIgnoreCase("ROADPROB"))
            RoadProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("CLIFFPROB"))
            CliffProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("INVERTNEGATIVETERRAIN"))
            InvertNegativeTerrain = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("RIVERPROB"))
            RiverProb = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("ALGORITHM"))
            Algorithm = Integer.parseInt(charData);
        else if (lastElement.equalsIgnoreCase("staticmap")) {
        	map =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("mapname")) {
        	mapname = charData;
        } else if (lastElement.equalsIgnoreCase("xmap")) {
        	xmap =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("ymap")) {
        	ymap =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("xboard")) {
        	xboard =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("yboard")) {
        	yboard =  Integer.parseInt(charData);
        }
        else if (lastElement.equalsIgnoreCase("ENVIRONMENTPROBABILITY"))
            environmentProb = Integer.parseInt(charData);
    }

    public void recordComment(String comment) {
        MWLogger.mainLog(prefix + "*Comment: " + comment);
    }

    /* INPUT METHODS */

    public InputStream getDocumentStream() throws ParseException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new ParseException("could not find " + filename);
        }
    }

    public InputStream resolveExternalEntity(String name, String pubID, String sysID) throws ParseException {
        if (sysID != null) {
            File f = new File((new File(filename)).getParent(), sysID);
            try {
                return new FileInputStream(f);
            } catch (FileNotFoundException e) {
                throw new ParseException("file not found (" + f + ")");
            }
        }
        // else
        return null;
    }

    public InputStream resolveDTDEntity(String name, String pubID, String sysID) throws ParseException {
        return resolveExternalEntity(name, pubID, sysID);
    }

    public static String newLineToBR(String data) {
        StringTokenizer tokened = new StringTokenizer(data, "\n");
        String result = new String();
        while (tokened.hasMoreElements()) {
            result += tokened.nextElement();
            if (tokened.hasMoreElements())
                result += "<BR>";
        }
        return result;
    }
}
