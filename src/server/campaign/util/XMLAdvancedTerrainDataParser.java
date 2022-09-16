/**
 * 
 */
package server.campaign.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

import common.AdvancedTerrain;
import common.util.MWLogger;
import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;
import server.campaign.CampaignMain;

/**
 * @author mike
 *
 */
public class XMLAdvancedTerrainDataParser implements XMLResponder{
    private String prefix;
    String Name = "";

    String lastElement = "";
    String name;
    String filename;
	
    int lowtemp = 25;
    int hitemp = 25;
    double gravity = 1.0;
    boolean vacuum = false;
    int nightchance = 0;
    int nightmod = 0;
    int blizzardChance = 0;
    int blowingSandChance = 0;
    int heavySnowfallChance = 0;
    int lightRainfallChance = 0;
    int heavyRainfallChance = 0;
    int moderateWindsChance = 0;
    int highWindsChance = 0;
    int downPourChance = 0;
    int duskChance = 0;
    int atmo = 0;
    int emiChance = 0;
    boolean emi = false;
	private int windStrength;
	private int windDir;
	private int tornadoF4WindChance;
	private int tornadoF13WindChance;
	private boolean effectTerrain;
	private int stormWindsChance;
	private int strongWindsChance;
	private int temp;
	private int sleetChance;
	private boolean shiftingWindStrength;
	private boolean shiftingWindDirection;
	private int pitchBlackChance;
	private int moonlessNightChance;
	private int moderateSnowFallChance;
	private int moderateRainFallChance;
	private int maxWindStrength;
	private int lightWindChance;
	private int lightSnowfallChance;
	private int lightHailChance;
	private int lightFogChance;
	private int lightConditions;
	private int iceStormChance;
	private int heavyHailChance;
	private int heavyFogChance;
	private int fogChance; 
	
	private AdvancedTerrain planetTerrain;
    
    public XMLAdvancedTerrainDataParser(String filename) {
        this.filename = filename;
        if (!(new File(filename).exists())){
        	return;
        }
        try {
            XMLParser xp = new XMLParser();
            xp.parseXML(this);
        } catch (Exception ex) {
            MWLogger.errLog("Error parsing " + filename);
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
        MWLogger.mainLog("Advanced Terrain READ");
        if (tagName.equals("ADVANCEDTERRAIN")) {
            planetTerrain = new AdvancedTerrain();
        	planetTerrain.setAtmosphere(atmo);
        	planetTerrain.setDownPourChance(downPourChance);
        	planetTerrain.setDuskChance(duskChance);
        	planetTerrain.setEMI(emi);
        	planetTerrain.setEMIChance(emiChance);
        	planetTerrain.setFog(fogChance);
        	planetTerrain.setGravity(gravity);
        	planetTerrain.setHeavyfogChance(heavyFogChance);
        	planetTerrain.setHeavyHailChance(heavyHailChance);
        	planetTerrain.setHeavyRainfallChance(heavyRainfallChance);
        	planetTerrain.setHeavySnowfallChance(heavySnowfallChance);
        	planetTerrain.setHighTemp(hitemp);
        	planetTerrain.setIceStormChance(iceStormChance);
        	planetTerrain.setLightConditions(lightConditions);
        	planetTerrain.setLightFogChance(lightFogChance);
        	planetTerrain.setLightHailChance(lightHailChance);
        	planetTerrain.setLightRainfallChance(lightRainfallChance);
        	planetTerrain.setLightSnowfallChance(lightSnowfallChance);
        	planetTerrain.setLightWindChance(lightWindChance);
        	planetTerrain.setLowTemp(lowtemp);
        	planetTerrain.setMaxWindStrength(maxWindStrength);
        	planetTerrain.setModerateRainFallChance(moderateRainFallChance);
        	planetTerrain.setModerateSnowFallChance(moderateSnowFallChance);
        	planetTerrain.setModerateWindsChance(moderateWindsChance);
        	planetTerrain.setMoonLessNightChance(moonlessNightChance);
        	planetTerrain.setNightChance(nightchance);
        	planetTerrain.setNightTempMod(nightmod);
        	planetTerrain.setPitchBlackNightChance(pitchBlackChance);
        	planetTerrain.setShiftingWindDirection(shiftingWindDirection);
        	planetTerrain.setShiftingWindStrength(shiftingWindStrength);
        	planetTerrain.setSleetChance(sleetChance);
        	planetTerrain.setStormWindsChance(stormWindsChance);
        	planetTerrain.setStrongWindsChance(strongWindsChance);
        	planetTerrain.setTemperature(temp);
        	planetTerrain.setTerrainAffected(effectTerrain);
        	planetTerrain.setTornadoF13WindChance(tornadoF13WindChance);
        	planetTerrain.setTornadoF4WindsChance(tornadoF4WindChance);
        	planetTerrain.setWindDirection(windDir);
        	planetTerrain.setWindStrength(windStrength);
        	planetTerrain.setName(name);
        	planetTerrain.setDisplayName(name);
        	MWLogger.mainLog("ADVTERRAIN: adding " + planetTerrain.getName());
            CampaignMain.cm.getData().addAdvancedTerrain(planetTerrain);
            name = "reset";
        }
        if (tagName.equals("ADVTERRAIN")) {
        }
    }

	@Override
	public InputStream getDocumentStream() throws ParseException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new ParseException("could not find the specified file: " + filename);
        }
	}

	@Override
	public void recordCharData(String charData) {
		MWLogger.mainLog(prefix + charData);
        if (!charData.equalsIgnoreCase("")) {
            MWLogger.mainLog(lastElement + " --> " + charData);
        } else {
            lastElement = "";
        }

        if (lastElement.equalsIgnoreCase("NAME")) {
            name = charData;
            MWLogger.mainLog(name);
        }  else if (lastElement.equalsIgnoreCase("lowtemp")) {
        	lowtemp =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("hitemp")) {
        	hitemp =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("gravity")) {
        	gravity =  Double.parseDouble(charData);        
        } else if (lastElement.equalsIgnoreCase("vacuum")) {
        	vacuum =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("nightchance")) {
        	nightchance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("nightmod")) {
        	nightmod =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("blizzardchance")) {
        	blizzardChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("blowingsandchance")) {
        	blowingSandChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("heavysnowfallchance")) {
        	heavySnowfallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("lightRainfallChance")) {
        	lightRainfallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("heavyRainfallChance")) {
        	heavyRainfallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("moderateWindsChance")) {
        	moderateWindsChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("highWindsChance")) {
        	highWindsChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("downPourChance")) {
        	downPourChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("duskChance")) {
        	duskChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("atmosphere")) {
        	atmo =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("emiChance")) {
        	emiChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("emi")) {
        	emi =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("windStrength")) {
        	windStrength =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("windDir")) {
        	windDir =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("tornadoF4WindChance")) {
        	tornadoF4WindChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("tornadoF13WindChance")) {
        	tornadoF13WindChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("effectTerrain")) {
        	effectTerrain =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("stormWindsChance")) {
        	stormWindsChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("strongWindsChance")) {
        	strongWindsChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("temp")) {
        	temp =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("sleetchance")) {
        	sleetChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("shiftingwindstrength")) {
        	shiftingWindStrength =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("shiftingWindDirection")) {
        	shiftingWindDirection =  Boolean.parseBoolean(charData);
        } else if (lastElement.equalsIgnoreCase("pitchBlackChance")) {
        	pitchBlackChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("moonlessNightChance")) {
        	moonlessNightChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("moderateSnowFallChance")) {
        	moderateSnowFallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("moderateRainFallChance")) {
        	moderateRainFallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("maxWindsStrength")) {
        	maxWindStrength =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("lightWindChance")) {
        	lightWindChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("ligthSnowfallChance")) {
        	lightSnowfallChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("lightHailChance")) {
        	lightHailChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("lightFogChance")) {
        	lightFogChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("lightConditions")) {
        	lightConditions =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("iceStormChance")) {
        	iceStormChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("HeavyHailChance")) {
        	heavyHailChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("HeavyFogChance")) {
        	heavyFogChance =  Integer.parseInt(charData);
        } else if (lastElement.equalsIgnoreCase("fogChance")) {
        	fogChance=  Integer.parseInt(charData);
        } 
	}

	@Override
    public void recordComment(String comment) {
        MWLogger.mainLog(prefix + "*Comment: " + comment);
    }

	@Override

    public void recordPI(String name, String pValue) {
        MWLogger.mainLog(prefix + "*" + name + " PI: " + pValue);
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


	@Override
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
}
