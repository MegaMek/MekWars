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

package server.campaign.util;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import common.AdvancedTerrain;
import common.Continent;
import common.Influences;
import common.PlanetEnvironments;
import common.UnitFactory;
import common.util.MWLogger;
import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SUnitFactory;

public class XMLPlanetDataParser implements XMLResponder {
    String lastElement = "";
    String lastInfFaction = "";
    String Name = "";
    String MFName = null;
    String MFSize = null;
    String MFFounder = null;
    String XCood = null;
    String YCood = null;

    int Income;
    int MFTicksUntilRefresh = 0;
    int MFRefreshSpeed = 100;
    int Type = 0;

    String buildTableFolder = "0";

    int accessLevel = 0;

    HashMap<Integer, Integer> Influence = new HashMap<Integer, Integer>();// House
    // ID,
    // Amount
    Vector<SPlanet> planets = new Vector<SPlanet>(1, 1);

    Vector<UnitFactory> unitFactories = new Vector<UnitFactory>(1, 1);

    private String filename;
    private String prefix;
    private String Description = "";
    private PlanetEnvironments PlanEnv = new PlanetEnvironments();
    private AdvancedTerrain AdvTerr = null;
    public TreeMap<Integer, AdvancedTerrain> AdvTerrTreeMap = new TreeMap<Integer, AdvancedTerrain>();
    private TreeMap<String, String> OpFlags = new TreeMap<String, String>();
    boolean conquerable = true;
    private int counter = 1;
    int xmap = 1;
    int ymap = 1;
    int xboard = 16;
    int yboard = 17;
    int lowtemp = 25;
    int hitemp = 25;
    double gravity = 1.0;
    boolean vacuum = false;
    int nightchance = 0;
    int nightmod = 0;
    int minVisibility = 100;
    int maxVisibility = 100;
    int blizzardChance = 0;
    int blowingSandChance = 0;
    int heavySnowfallChance = 0;
    int lightRainfallChance = 0;
    int heavyRainfallChance = 0;
    int moderateWindsChance = 0;
    int highWindsChance = 0;

    boolean map = false;
    String mapname = "";
    String aterrainName = "";

    int CompProduction = 0;
    int Warehousesize = 0;
    boolean inWarehouse = false;
    boolean inContinent = false;
    boolean hasAdvancedTerrain = false;
    int terrainProb = 0;
    String terrainName = "";
    String advTerrainName = "";
    String OriginalOwner = CampaignMain.cm.getConfig("NewbieHouseName");
    String OpFlag = "";
    String OpName = "";
    boolean isHomeWorld = false;
    boolean singlePlayerFactions = CampaignMain.cm.getBooleanConfig("AllowSinglePlayerFactions");

    public XMLPlanetDataParser(String filename) {

        this.filename = filename;
        try {
            XMLParser xp = new XMLParser();
            xp.parseXML(this);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public Vector<SPlanet> getPlanets() {
        return planets;
    }

    /* DTD METHODS */

    public void recordNotationDeclaration(String name, String pubID, String sysID) throws ParseException {
        System.out.print(prefix + "!NOTATION: " + name);
        if (sysID != null) {
            System.out.print("  sysID = " + sysID);
        }
        MWLogger.mainLog("");
    }

    public void recordEntityDeclaration(String name, String value, String pubID, String sysID, String notation) throws ParseException {
        System.out.print(prefix + "!ENTITY: " + name);
        if (value != null) {
            System.out.print("  value = " + value);
        }
        if (pubID != null) {
            System.out.print("  pubID = " + pubID);
        }
        if (sysID != null) {
            System.out.print("  sysID = " + sysID);
        }
        if (notation != null) {
            System.out.print("  notation = " + notation);
        }
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
        if (pubID != null) {
            System.out.print("  pubID = " + pubID);
        }
        if (sysID != null) {
            System.out.print("  sysID = " + sysID);
        }
        MWLogger.mainLog("");
        prefix = "";
    }

    /* DOC METHDODS */

    public void recordDocStart() {
    }

    public void recordDocEnd() {
        MWLogger.mainLog("");
        MWLogger.mainLog("Planet Parsing finished without error");
    }

    public void recordElementStart(String name, Hashtable attr) throws ParseException {
        // MWLogger.mainLog(prefix+"Element: "+name);
        lastElement = name;
        if (name.equalsIgnoreCase("WAREHOUSE")) {
            inWarehouse = true;
        }
        if (name.equalsIgnoreCase("CONTINENT")) {
            inContinent = true;
        }

        /*
         * if (attr!=null) { Enumeration e = attr.keys();
         * System.out.print(prefix); String conj = ""; while
         * (e.hasMoreElements()) { Object k = e.nextElement();
         * System.out.print(conj+k+" = "+attr.get(k)); conj = ", "; }
         * MWLogger.mainLog(""); } prefix = prefix+" ";
         */
    }

    public void recordElementEnd(String name) throws ParseException {

        if (name.equalsIgnoreCase("TIMEZONE")) {
            MWLogger.errLog("planets.xml contains TIMEZONE field. No longer necessary!");
        }

        if (name.equalsIgnoreCase("UNITFACTORY")) {
            if (MFName != null && MFFounder != null && MFSize != null) {

                // if (Type == 0)
                // Type = Unit.MEK;

                SUnitFactory mf = new SUnitFactory(MFName, null, MFSize, MFFounder, MFTicksUntilRefresh, MFRefreshSpeed, Type, buildTableFolder, accessLevel);
                unitFactories.add(mf);

                // RESET VARIABLES
                MFName = null;
                MFSize = null;
                MFFounder = null;
                MFTicksUntilRefresh = 0;
                MFRefreshSpeed = 100;
                Type = 0;
                buildTableFolder = "0";
                accessLevel = 0;
            }
        }
        if (name.equalsIgnoreCase("CONTINENT")) {
          //TODO remove this later MDR
        	MWLogger.mainLog("continent to try and add is: "+ terrainProb +"% " +terrainName + "[" + CampaignMain.cm.getData().getTerrainByName(terrainName).getId() + "]"
        			+ "(" + advTerrainName +"[" + CampaignMain.cm.getData().getAdvancedTerrainByName(advTerrainName).getId() + "])");
        	
            Continent cont = new Continent(terrainProb, CampaignMain.cm.getData().getTerrainByName(terrainName),CampaignMain.cm.getData().getAdvancedTerrainByName(advTerrainName));
            PlanEnv.add(cont);
            terrainProb = 0;
            terrainName = "";
            advTerrainName = "";
            inContinent = false;
        }

        if (name.equalsIgnoreCase("OPNAME")) {
            OpFlags.put(OpFlag, OpName);
            OpFlag = "";
            OpName = "";
        }

        if (name.equalsIgnoreCase("PLANET")) {
            MWLogger.mainLog("PLANET READ");
            SPlanet p;
            p = new SPlanet(counter++, Name, null, Income, CompProduction, Double.parseDouble(XCood), Double.parseDouble(YCood));
            for (int i = 0; i < unitFactories.size(); i++) {
                SUnitFactory MF = (SUnitFactory) unitFactories.get(i);
                MF.setPlanet(p);
            }
            p.setUnitFactories(unitFactories);
            p.setEnvironments(PlanEnv);
            p.setDescription(Description);
            p.setBaysProvided(Warehousesize);
            MWLogger.mainLog("Influence: " + Influence);
            // This has to be called last since the Bays provided are added to
            // the faction then for instance
            p.setInfluence(new Influences(Influence));
            p.setConquerable(conquerable);
            p.setMapSize(new Dimension(xmap, ymap));
            p.setBoardSize(new Dimension(xboard, yboard));
            /*
             * for ( Integer id: AdvTerrTreeMap.keySet() ){
             * p.getAdvancedTerrain().put(id,AdvTerrTreeMap.get(id)); }
             */
            if (singlePlayerFactions && !OriginalOwner.equalsIgnoreCase(CampaignMain.cm.getConfig("NewbieHouseName"))) {
                p.setOriginalOwner("None");
            } else {
                p.setOriginalOwner(OriginalOwner);
            }
            p.setPlanetFlags(OpFlags);
            p.setHomeWorld(isHomeWorld);

            planets.add(p);

            // RESET VARIABLES
            conquerable = true;
            Name = null;
            Income = 0;
            XCood = null;
            YCood = null;
            Influence = new HashMap<Integer, Integer>();
            OpFlags.clear();
            unitFactories = new Vector<UnitFactory>(1, 1);
            Description = "";
            PlanEnv = new PlanetEnvironments();
            Warehousesize = 0;
            CompProduction = 0;
            hasAdvancedTerrain = false;
            xboard = -1;
            yboard = -1;
            nightchance = 0;
            nightmod = 0;
            map = false;
            mapname = "";
            aterrainName = "";
            advTerrainName = "";
            gravity = 1.0;
            vacuum = false;
            lowtemp = 25;
            hitemp = 25;
            ymap = -1;
            xmap = -1;
            isHomeWorld = false;
            minVisibility = 100;
            maxVisibility = 100;
            blizzardChance = 0;
            blowingSandChance = 0;
            heavySnowfallChance = 0;
            lightRainfallChance = 0;
            heavyRainfallChance = 0;
            moderateWindsChance = 0;
            highWindsChance = 0;

            OriginalOwner = CampaignMain.cm.getConfig("NewbieHouseName");
            AdvTerr = null;
            AdvTerrTreeMap.clear();

        }
        if (name.equalsIgnoreCase("WAREHOUSE")) {
            inWarehouse = false;
        }
    }

    public void recordPI(String name, String pValue) {
        MWLogger.mainLog(prefix + "*" + name + " PI: " + pValue);
    }

    public void recordCharData(String charData) {
        MWLogger.mainLog(prefix + charData);
        if (!charData.equalsIgnoreCase("")) {
            MWLogger.mainLog(lastElement + " --> " + charData);
        } else {
            lastElement = "";
        }

        if (lastElement.equalsIgnoreCase("NAME")) {
            Name = charData;
            MWLogger.mainLog(Name);
        } else if (lastElement.equalsIgnoreCase("INCOME")) {
            Income = Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("XCOOD")) {
            XCood = charData;
        } else if (lastElement.equalsIgnoreCase("YCOOD")) {
            YCood = charData;
        } else if (lastElement.equalsIgnoreCase("WAREHOUSE")) {
            Warehousesize = Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("FACTION")) {
            lastInfFaction = charData;
            if (singlePlayerFactions && !lastInfFaction.equalsIgnoreCase(CampaignMain.cm.getConfig("NewbieHouseName"))) {
                lastInfFaction = "None";
            }
        } else if (lastElement.equalsIgnoreCase("AMOUNT")) {
            SHouse h = CampaignMain.cm.getHouseFromPartialString(lastInfFaction, null);
            if (h != null) {
                Influence.put(h.getId(), Integer.parseInt(charData));
                MWLogger.mainLog("Parsed: " + h.toString() + " - " + charData);
            } else {
                MWLogger.mainLog("ERROR READING FACTION: " + lastInfFaction);
            }
        }

        else if (lastElement.equalsIgnoreCase("FACTORYNAME")) {
            MFName = charData;
        } else if (lastElement.equalsIgnoreCase("FOUNDER")) {
            MFFounder = charData;
        } else if (lastElement.equalsIgnoreCase("SIZE")) {
            if (inWarehouse) {
                Warehousesize = Integer.parseInt(charData);
            } else if (inContinent) {
                terrainProb = Integer.parseInt(charData);
            } else {
                MFSize = charData;
            }
        } else if (lastElement.equalsIgnoreCase("TICKSUNTILREFRESH")) {
            MFTicksUntilRefresh = Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("REFRESHSPEED")) {
            MFRefreshSpeed = Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("TYPE")) {
            if (charData.equalsIgnoreCase("VEHICLE")) {
                Type = Type + UnitFactory.BUILDVEHICLES;
            } else if (charData.equalsIgnoreCase("INFANTRY")) {
                Type = Type + UnitFactory.BUILDINFANTRY;
            } else if (charData.equalsIgnoreCase("BATTLEARMOR")) {
                Type = Type + UnitFactory.BUILDBATTLEARMOR;
            } else if (charData.equalsIgnoreCase("PROTOMEK")) {
                Type = Type + UnitFactory.BUILDPROTOMECHS;
            } else if (charData.equalsIgnoreCase("AERO")) {
                Type = Type + UnitFactory.BUILDAERO;
            } else {
                Type = Type + UnitFactory.BUILDMEK;
            }
        } else if (lastElement.equalsIgnoreCase("COMPPRODUCTION")) {
            CompProduction = Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("CONQUERABLE")) {
            conquerable = Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("TERRAIN")) {
            terrainName = charData;
        } else if (lastElement.equalsIgnoreCase("ADVTERRAIN")) {
            advTerrainName = charData;
        } else if (lastElement.endsWith("XMAP")) {
            xmap = Integer.parseInt(charData);
        } else if (lastElement.endsWith("YMAP")) {
            ymap = Integer.parseInt(charData);
        } else if (lastElement.endsWith("XBOARD")) {
            xboard = Integer.parseInt(charData);
        } else if (lastElement.endsWith("YBOARD")) {
            yboard = Integer.parseInt(charData);
        } else if (lastElement.endsWith("LOWTEMP")) {
            lowtemp = Integer.parseInt(charData);
        } else if (lastElement.endsWith("HITEMP")) {
            hitemp = Integer.parseInt(charData);
        } else if (lastElement.endsWith("GRAVITY")) {
            gravity = Double.parseDouble(charData);
        } else if (lastElement.endsWith("VACUUM")) {
            vacuum = Boolean.parseBoolean(charData);
        } else if (lastElement.endsWith("MAP")) {
            map = Boolean.parseBoolean(charData);
        } else if (lastElement.endsWith("NIGHTCHANCE")) {
            nightchance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("NIGHTMOD")) {
            nightmod = Integer.parseInt(charData);
        } else if (lastElement.endsWith("MAPNAME")) {
            mapname = charData;
        } else if (lastElement.endsWith("TERRAINNAME")) {
            aterrainName = charData;
        } else if (lastElement.endsWith("ORIGINALOWNER")) {
            OriginalOwner = charData;
        } else if (lastElement.endsWith("OPKEY")) {
            OpFlag = charData;
        } else if (lastElement.endsWith("OPNAME")) {
            OpName = charData;
        } else if (lastElement.endsWith("HOMEWORLD")) {
            isHomeWorld = Boolean.parseBoolean(charData);
        } else if (lastElement.endsWith("BUILDTABLEFOLDER")) {
            buildTableFolder = charData;
        } else if (lastElement.endsWith("ACCESSLEVEL")) {
            accessLevel = Integer.parseInt(charData);
        } else if (lastElement.endsWith("MINVISIBILITY")) {
            minVisibility = Integer.parseInt(charData);
        } else if (lastElement.endsWith("MAXVISIBILITY")) {
            maxVisibility = Integer.parseInt(charData);
        } else if (lastElement.endsWith("BLIZZARDCHANCE")) {
            blizzardChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("BLOWINGSANDCHANCE")) {
            blowingSandChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("HEAVYSNOWFALLCHANCE")) {
            heavySnowfallChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("LIGHTRAINFALLCHANCE")) {
            lightRainfallChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("HEAVYRAINFALLCHANCE")) {
            heavyRainfallChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("MODERATEWINDSCHANCE")) {
            moderateWindsChance = Integer.parseInt(charData);
        } else if (lastElement.endsWith("HIGHWINDSCHANCE")) {
            highWindsChance = Integer.parseInt(charData);
        }

    }

    public void recordComment(String comment) {
        MWLogger.mainLog(prefix + "*Comment: " + comment);
    }

    /* INPUT METHODS */
    public InputStream getDocumentStream() throws ParseException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new ParseException("could not find the specified file");
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
            if (tokened.hasMoreElements()) {
                result += "<BR>";
            }
        }
        return result;
    }

}
