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

import java.io.IOException;
import java.util.StringTokenizer;

import megamek.common.planetaryConditions.Atmosphere;
import megamek.common.planetaryConditions.EMI;
import megamek.common.planetaryConditions.Fog;
import megamek.common.planetaryConditions.Light;
import megamek.common.planetaryConditions.Weather;
import megamek.common.planetaryConditions.Wind;
import megamek.common.planetaryConditions.WindDirection;
import mekwars.common.persistence.BinReader;
import mekwars.common.persistence.BinWriter;
import mekwars.common.util.TokenReader;

/**
 * Advanced Environment for planets.
 *
 * @author Torren (Jason Tighe) allows So's to set up each terrain on a planet.
 */

final public class AdvancedTerrain {

    private String displayName = "none";
    private int id = 0;
    private String Name = "none";

    private int lowTemp = 25;
    private int highTemp = 25;
    private double gravity = 1.0;
    private boolean vacuum = false;
    private int duskChance = 0;
    private int fullMoonChance = 0;
    private int moonlessChance = 0;
    private int pitchBlackChance = 0;
    private int nightTempMod = 0;
    private int minVisibility = 100;
    private int maxVisibility = 100;
    private Atmosphere atmosphere = Atmosphere.STANDARD;

    private int lightRainfallChance = 0;
    private int moderateRainfallChance = 0;
    private int heavyRainfallChance = 0;
    private int downPourChance = 0;

    private int lightSnowfallChance = 0;
    private int moderateSnowfallChance = 0;
    private int heavySnowfallChance = 0;
    private int sleetChance = 0;
    private int iceStormChance = 0;
    private int lightHailChance = 0;
    private int heavyHailChance = 0;

    private int lightWindsChance = 0;
    private int moderateWindsChance = 0;
    private int strongWindsChance = 0;
    private int stormWindsChance = 0;
    private int tornadoF13WindsChance = 0;
    private int tornadoF4WindsChance = 0;

    private int lightFogChance = 0;
    private int heavyFogChance = 0;

    private int emiChance = 0;

    // MegaMek Planetary Conditions
    // set up the specific conditions
    private Light lightConditions = Light.DAY;
    private Weather weatherConditions = Weather.CLEAR;
    private Wind windStrength = Wind.CALM;
    private WindDirection windDirection = WindDirection.RANDOM;
    private Wind maxWindStrength = Wind.TORNADO_F4;
    private boolean shiftWindDirection = false;
    private boolean shiftWindStrength = false;
    private Fog fog = Fog.FOG_NONE;
    private int temperature = 25;
    private EMI emi = EMI.EMI_NONE;
    private boolean terrainAffected = true;

    public AdvancedTerrain(String s) {
        StringTokenizer command = new StringTokenizer(s, "$");

        setDisplayName(TokenReader.readString(command));
        setLowTemp(TokenReader.readInt(command));
        setHighTemp(TokenReader.readInt(command));
        setGravity(TokenReader.readDouble(command));
        setNightChance(TokenReader.readInt(command));
        setNightTempMod(TokenReader.readInt(command));
        setMinVisibility(TokenReader.readInt(command));
        setMaxVisibility(TokenReader.readInt(command));
        setModerateRainFallChance(TokenReader.readInt(command));
        setModerateSnowFallChance(TokenReader.readInt(command));
        setHeavySnowfallChance(TokenReader.readInt(command));
        setLightRainfallChance(TokenReader.readInt(command));
        setHeavyRainfallChance(TokenReader.readInt(command));
        setModerateWindsChance(TokenReader.readInt(command));
        setStrongWindsChance(TokenReader.readInt(command));
        setDownPourChance(TokenReader.readInt(command));
        setLightSnowfallChance(TokenReader.readInt(command));
        setSleetChance(TokenReader.readInt(command));
        setIceStormChance(TokenReader.readInt(command));
        setLightHailChance(TokenReader.readInt(command));
        setHeavyHailChance(TokenReader.readInt(command));
        setStormWindsChance(TokenReader.readInt(command));
        setTornadoF13WindChance(TokenReader.readInt(command));
        setTornadoF4WindsChance(TokenReader.readInt(command));
        setAtmosphere(Atmosphere.getAtmosphere(TokenReader.readInt(command)));
        setLightFogChance(TokenReader.readInt(command));
        setHeavyFogChance(TokenReader.readInt(command));
        setDuskChance(TokenReader.readInt(command));
        setMoonLessNightChance(TokenReader.readInt(command));
        setPitchBlackNightChance(TokenReader.readInt(command));
        setEMIChance(TokenReader.readInt(command));
        setLightWindChance(TokenReader.readInt(command));

        // MegaMek Planetary Conditions this should always be last
        setLightConditions(Light.getLight(TokenReader.readInt(command)));
        setWeatherConditions(Weather.getWeather(TokenReader.readInt(command)));
        setWindStrength(Wind.getWind(TokenReader.readInt(command)));
        setWindDirection(WindDirection.getWindDirection(TokenReader.readInt(command)));
        setShiftingWindDirection(TokenReader.readBoolean(command));
        setShiftingWindStrength(TokenReader.readBoolean(command));
        setFog(Fog.getFog(TokenReader.readInt(command)));
        setTemperature(TokenReader.readInt(command));
        setEMI(TokenReader.readBoolean(command) ? EMI.EMI : EMI.EMI_NONE);
        setTerrainAffected(TokenReader.readBoolean(command));
        setMaxWindStrength(Wind.getWind(TokenReader.readInt(command)));

    }

    public AdvancedTerrain() {
    }

    public void setTornadoF13WindChance(int chance) {
        tornadoF13WindsChance = chance;
    }

    public void setLightWindChance(int chance) {
        lightWindsChance = chance;
    }

    public void setShiftingWindDirection(boolean shift) {
        shiftWindDirection = shift;
    }

    public void setShiftingWindStrength(boolean strength) {
        shiftWindStrength = strength;
    }

    public void setEMI(EMI emi) {
        this.emi = emi;
    }

    public void binIn(BinReader in) throws IOException {
        displayName = in.read("displayName");
        Name = displayName;
        lowTemp = in.readInt("lowTemp");
        highTemp = in.readInt("highTemp");
        gravity = in.readDouble("gravity");
        vacuum = in.readBoolean("vacuum");
        fullMoonChance = in.readInt("nightChance");
        nightTempMod = in.readInt("nightTempMod");
        minVisibility = in.readInt("minVisibility");
        maxVisibility = in.readInt("maxVisibility");
        moderateRainfallChance = in.readInt("moderateRainfallChance");
        moderateSnowfallChance = in.readInt("moderateSnowfallChance");
        heavySnowfallChance = in.readInt("heavySnowfallChance");
        lightRainfallChance = in.readInt("lightRainfallChance");
        heavyRainfallChance = in.readInt("heavyRainfallChance");
        lightWindsChance = in.readInt("lightWindsChance");
        moderateWindsChance = in.readInt("moderateWindsChance");
        strongWindsChance = in.readInt("strongWindsChance");
        downPourChance = in.readInt("downPourChance");
        lightSnowfallChance = in.readInt("lightSnowfallChance");
        sleetChance = in.readInt("sleetChance");
        iceStormChance = in.readInt("iceStormChance");
        lightHailChance = in.readInt("lightHailChance");
        heavyHailChance = in.readInt("heavyHailChance");
        stormWindsChance = in.readInt("stormWindsChance");
        tornadoF13WindsChance = in.readInt("tornadoF13WindsChance");
        tornadoF4WindsChance = in.readInt("tornadoF4WindsChance");
        atmosphere = Atmosphere.getAtmosphere(in.readInt("atmosphere"));
        lightFogChance = in.readInt("lightFogChance");
        heavyFogChance = in.readInt("heavyFogChance");
        duskChance = in.readInt("duskChance");
        moonlessChance = in.readInt("moonlessChance");
        pitchBlackChance = in.readInt("pitchBlackChance");
        emiChance = in.readInt("emiChance");
    }

    public void binOut(BinWriter out) throws IOException {
        out.println(displayName, "displayName");
        out.println(lowTemp, "lowTemp");
        out.println(highTemp, "highTemp");
        out.println(gravity, "gravity");
        out.println(vacuum, "vacuum");
        out.println(fullMoonChance, "nightChance");
        out.println(nightTempMod, "nightTempMod");
        out.println(minVisibility, "minVisibility");
        out.println(maxVisibility, "maxVisibility");
        out.println(moderateRainfallChance, "moderateRainfallChance");
        out.println(moderateSnowfallChance, "moderateSnowfallChance");
        out.println(heavySnowfallChance, "heavySnowfallChance");
        out.println(lightRainfallChance, "lightRainfallChance");
        out.println(heavyRainfallChance, "heavyRainfallChance");
        out.println(lightWindsChance, "lightWindsChance");
        out.println(moderateWindsChance, "moderateWindsChance");
        out.println(strongWindsChance, "strongWindsChance");
        out.println(downPourChance, "downPourChance");
        out.println(lightSnowfallChance, "lightSnowfallChance");
        out.println(sleetChance, "sleetChance");
        out.println(iceStormChance, "iceStormChance");
        out.println(lightHailChance, "lightHailChance");
        out.println(heavyHailChance, "heavyHailChance");
        out.println(stormWindsChance, "stormWindsChance");
        out.println(tornadoF13WindsChance, "tornadoF13WindsChance");
        out.println(tornadoF4WindsChance, "tornadoF4WindsChance");
        out.println(atmosphere.ordinal(), "atmosphere");
        out.println(lightFogChance, "lightFogChance");
        out.println(heavyFogChance, "heavyFogChance");
        out.println(duskChance, "duskChance");
        out.println(moonlessChance, "moonlessChance");
        out.println(pitchBlackChance, "pitchBlackChance");
        out.println(emiChance, "emiChance");
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String name) {
        displayName = name;
        Name = name;
    }

    public int getLowTemp() {
        return lowTemp;
    }

    public void setLowTemp(int temp) {
        lowTemp = temp;
    }

    public int getHighTemp() {
        return highTemp;
    }

    public void setHighTemp(int temp) {
        highTemp = temp;
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double grav) {
        gravity = grav;
    }

    public int getDuskChance() {
        return duskChance;
    }

    public void setDuskChance(int chance) {
        duskChance = chance;
    }

    public int getNightChance() {
        return fullMoonChance;
    }

    public void setNightChance(int chance) {
        fullMoonChance = chance;
    }

    public int getMoonLessNightChance() {
        return moonlessChance;
    }

    public void setMoonLessNightChance(int chance) {
        moonlessChance = chance;
    }

    public int getPitchBlackNightChance() {
        return pitchBlackChance;
    }

    public void setPitchBlackNightChance(int chance) {
        pitchBlackChance = chance;
    }

    public int getNightTempMod() {
        return nightTempMod;
    }

    public void setNightTempMod(int mod) {
        nightTempMod = mod;
    }

    public int getMinVisibility() {
        return minVisibility;
    }

    public void setMinVisibility(int minVisibility) {
        this.minVisibility = minVisibility;
    }

    public int getMaxVisibility() {
        return maxVisibility;
    }

    public void setMaxVisibility(int maxVisibility) {
        this.maxVisibility = maxVisibility;
    }

    public int getModerateSnowFallChance() {
        return moderateSnowfallChance;
    }

    public void setModerateSnowFallChance(int chance) {
        moderateSnowfallChance = chance;
    }

    public int getModerateRainFallChance() {
        return moderateRainfallChance;
    }

    public void setModerateRainFallChance(int chance) {
        moderateRainfallChance = chance;
    }

    public int getHeavySnowfallChance() {
        return heavySnowfallChance;
    }

    public void setHeavySnowfallChance(int chance) {
        heavySnowfallChance = chance;
    }

    public int getLightRainfallChance() {
        return lightRainfallChance;
    }

    public void setLightRainfallChance(int chance) {
        lightRainfallChance = chance;
    }

    public int getHeavyRainfallChance() {
        return heavyRainfallChance;
    }

    public void setHeavyRainfallChance(int chance) {
        heavyRainfallChance = chance;
    }

    public int getModerateWindsChance() {
        return moderateWindsChance;
    }

    public void setModerateWindsChance(int chance) {
        moderateWindsChance = chance;
    }

    public int getStrongWindsChance() {
        return strongWindsChance;
    }

    public void setStrongWindsChance(int chance) {
        strongWindsChance = chance;
    }

    public int getStormWindsChance() {
        return stormWindsChance;
    }

    public void setStormWindsChance(int chance) {
        stormWindsChance = chance;
    }

    public int getLightWindsChance() {
        return lightWindsChance;
    }

    public int getTornadoF13WindsChance() {
        return tornadoF13WindsChance;
    }

    public int getTornadoF4WindsChance() {
        return tornadoF4WindsChance;
    }

    public void setTornadoF4WindsChance(int chance) {
        tornadoF4WindsChance = chance;
    }

    public int getDownPourChance() {
        return downPourChance;
    }

    public void setDownPourChance(int chance) {
        downPourChance = chance;
    }

    public int getLightSnowfallChance() {
        return lightSnowfallChance;
    }

    public void setLightSnowfallChance(int chance) {
        lightSnowfallChance = chance;
    }

    public int getSleetChance() {
        return sleetChance;
    }

    public void setSleetChance(int chance) {
        sleetChance = chance;
    }

    public int getIceStormChance() {
        return iceStormChance;
    }

    public void setIceStormChance(int chance) {
        iceStormChance = chance;
    }

    public int getLightHailChance() {
        return lightHailChance;
    }

    public void setLightHailChance(int chance) {
        lightHailChance = chance;
    }

    public int getHeavyHailChance() {
        return heavyHailChance;
    }

    public void setHeavyHailChance(int chance) {
        heavyHailChance = chance;
    }

    public AdvancedTerrain clone() {
        AdvancedTerrain clone = new AdvancedTerrain();
        clone.setAtmosphere(atmosphere);
        clone.setDisplayName(displayName);
        clone.setDownPourChance(downPourChance);
        clone.setDuskChance(duskChance);
        clone.setEMI(emi);
        clone.setFog(fog);
        clone.setGravity(gravity);
        clone.setHeavyFogChance(heavyFogChance);
        clone.setHeavyHailChance(heavyHailChance);
        clone.setHeavyRainfallChance(heavyRainfallChance);
        clone.setHeavySnowfallChance(heavySnowfallChance);
        clone.setHighTemp(highTemp);
        clone.setIceStormChance(iceStormChance);
        clone.setLightConditions(lightConditions);
        clone.setLightFogChance(lightFogChance);
        clone.setLightHailChance(lightHailChance);
        clone.setLightRainfallChance(lightRainfallChance);
        clone.setLightSnowfallChance(lightSnowfallChance);
        clone.setLightWindChance(lightWindsChance);
        clone.setLowTemp(lowTemp);
        clone.setMaxWindStrength(maxWindStrength);
        clone.setModerateRainFallChance(moderateRainfallChance);
        clone.setModerateSnowFallChance(moderateSnowfallChance);
        clone.setModerateWindsChance(moderateWindsChance);
        clone.setMoonLessNightChance(moonlessChance);
        clone.setName(Name);
        clone.setNightChance(fullMoonChance);
        clone.setNightTempMod(nightTempMod);
        clone.setPitchBlackNightChance(pitchBlackChance);
        clone.setShiftingWindDirection(shiftWindDirection);
        clone.setShiftingWindStrength(shiftWindStrength);
        clone.setSleetChance(sleetChance);
        clone.setStormWindsChance(stormWindsChance);
        clone.setStrongWindsChance(strongWindsChance);
        clone.setTemperature(temperature);
        clone.setTerrainAffected(terrainAffected);
        clone.setTornadoF13WindChance(tornadoF13WindsChance);
        clone.setTornadoF4WindsChance(tornadoF4WindsChance);
        clone.setWeatherConditions(weatherConditions);
        clone.setWindDirection(windDirection);
        clone.setWindStrength(windStrength);

        return clone;
    }

    @Override
    public String toString() {
        String result;
        result = "$";

        if (displayName.trim().isEmpty()) {
            result += "Terrain";
        } else {
            result += displayName;
        }

        result += "$";
        result += lowTemp;
        result += "$";
        result += highTemp;
        result += "$";
        result += gravity;
        result += "$";
        result += vacuum;
        result += "$";
        result += fullMoonChance;
        result += "$";
        result += nightTempMod;
        result += "$";
        result += minVisibility;
        result += "$";
        result += maxVisibility;
        result += "$";
        result += moderateRainfallChance;
        result += "$";
        result += moderateSnowfallChance;
        result += "$";
        result += heavySnowfallChance;
        result += "$";
        result += lightRainfallChance;
        result += "$";
        result += heavyRainfallChance;
        result += "$";
        result += moderateWindsChance;
        result += "$";
        result += strongWindsChance;
        result += "$";
        result += downPourChance;
        result += "$";
        result += lightSnowfallChance;
        result += "$";
        result += sleetChance;
        result += "$";
        result += iceStormChance;
        result += "$";
        result += lightHailChance;
        result += "$";
        result += heavyHailChance;
        result += "$";
        result += stormWindsChance;
        result += "$";
        result += tornadoF13WindsChance;
        result += "$";
        result += tornadoF4WindsChance;
        result += "$";
        result += atmosphere;
        result += "$";
        result += lightFogChance;
        result += "$";
        result += heavyFogChance;
        result += "$";
        result += duskChance;
        result += "$";
        result += moonlessChance;
        result += "$";
        result += pitchBlackChance;
        result += "$";
        result += emiChance;
        result += "$";
        result += lightWindsChance;

        return result;
    }

    public Light getLightConditions() {
        return lightConditions;
    }

    public void setLightConditions(Light light) {
        lightConditions = light;
    }

    public Weather getWeatherConditions() {
        return weatherConditions;
    }

    public void setWeatherConditions(Weather weather) {
        weatherConditions = weather;
    }

    public Wind getWindStrength() {
        return windStrength;
    }

    public void setWindStrength(Wind wind) {
        windStrength = wind;
    }

    public WindDirection getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(WindDirection dir) {
        windDirection = dir;
    }

    public boolean hasShiftingWindDirection() {
        return shiftWindDirection;
    }

    public boolean hasShiftingWindStrength() {
        return shiftWindStrength;
    }

    public String toStringPlanetaryConditions() {

        return this +
                     "$" +
                     lightConditions +
                     "$" +
                     weatherConditions +
                     "$" +
                     windStrength +
                     "$" +
                     windDirection +
                     "$" +
                     shiftWindDirection +
                     "$" +
                     shiftWindStrength +
                     "$" +
                     fog +
                     "$" +
                     temperature +
                     "$" +
                     emi +
                     "$" +
                     terrainAffected +
                     "$" +
                     maxWindStrength;
    }

    public boolean isTerrainAffected() {
        return terrainAffected;
    }

    public void setTerrainAffected(boolean terrain) {
        terrainAffected = terrain;
    }

    public EMI hasEMI() {
        return emi;
    }

    public int getTemperature() {
        return temperature;
    }

    public void setTemperature(int temp) {
        temperature = temp;
    }

    public Fog getFog() {
        return fog;
    }

    public void setFog(Fog fog) {
        this.fog = fog;
    }

    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    public void setAtmosphere(Atmosphere atmo) {
        atmosphere = atmo;
    }

    public int getLightFogChance() {
        return lightFogChance;
    }

    public void setLightFogChance(int chance) {
        lightFogChance = chance;
    }

    public int getHeavyFogChance() {
        return heavyFogChance;
    }

    public void setHeavyFogChance(int chance) {
        heavyFogChance = chance;
    }

    public int getEMIChance() {
        return emiChance;
    }

    public void setEMIChance(int chance) {
        emiChance = chance;
    }

    public Wind getMaxWindStrength() {
        return maxWindStrength;
    }

    public void setMaxWindStrength(Wind wind) {
        maxWindStrength = wind;
    }

    public int getId() {
        return id;
    }

    public void setId(int unusedTerrainID) {
        id = unusedTerrainID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        displayName = name;
        Name = name;
    }

    public String toImageDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table><TR>");
        sb.append("<TD>lightConditions</TD><TD>weatherConditions</TD><TD>windStrength</TD><TD>windDirection</TD>");
        sb.append("<TD>shiftWindDirection</TD><TD>shiftWindStrength</TD><TD>fog</TD><TD>temperature</TD>");
        sb.append("<TD>emi</TD><TD>terrainAffected</TD><TD>maxWindStrength</TD></TR><TR>");
        sb.append("<TD>").append(lightConditions).append("</TD>");
        sb.append("<TD>").append(weatherConditions).append("</TD>");
        sb.append("<TD>").append(windStrength).append("</TD>");
        sb.append("<TD>").append(windDirection).append("</TD>");
        sb.append("<TD>").append(shiftWindDirection).append("</TD>");
        sb.append("<TD>").append(shiftWindStrength).append("</TD>");
        sb.append("<TD>").append(fog).append("</TD>");
        sb.append("<TD>").append(temperature).append("</TD>");
        sb.append("<TD>").append(emi).append("</TD>");
        sb.append("<TD>").append(terrainAffected).append("</TD>");
        sb.append("<TD>").append(maxWindStrength).append("</TR><table>");
        return sb.toString();
    }

    public String WeatherForecast() {
        Light worstLight = Light.DAY;
        float worstLightProb = 0;

        Light likelyLight = Light.DAY;
        float lightProb = 0;

        Weather worstWeather = Weather.CLEAR;
        float worstWeatherProb = 0;

        Weather likelyWeather = Weather.CLEAR;
        float weatherProb = 0;

        Wind worstWind = Wind.CALM;
        float worstWindProb = 0;

        Wind likelyWind = Wind.CALM;
        float windProb = 0;

        StringBuilder results = new StringBuilder();

        //find the worst light conditions and the most likely conditions (other than day)
        if (duskChance > 0) {
            likelyLight = worstLight = Light.DUSK;
            lightProb = worstLightProb = duskChance;
        }

        if (fullMoonChance > 0) {
            if (fullMoonChance > lightProb) {
                likelyLight = Light.FULL_MOON;
                lightProb = fullMoonChance;
            }
            worstLight = Light.FULL_MOON;
            worstLightProb = fullMoonChance;
        }

        if (moonlessChance > 0) {
            if (moonlessChance > lightProb) {
                likelyLight = Light.MOONLESS;
                lightProb = moonlessChance;
            }
            worstLight = Light.MOONLESS;
            worstLightProb = moonlessChance;
        }

        if (pitchBlackChance > 0) {
            if (pitchBlackChance > lightProb) {
                likelyLight = Light.PITCH_BLACK;
                lightProb = pitchBlackChance;
            }
            worstLight = Light.PITCH_BLACK;
            worstLightProb = pitchBlackChance;
        }

        results.append("likely / worst <br>");
        results.append("Light:");
        if (lightProb > 0) {
            results.append(lightProb / 10);
            results.append("% ");
            results.append(likelyLight);
            results.append(" / ");
            results.append(worstLightProb / 10);
            results.append("% ");
            results.append(worstLight);
            results.append("<br>");
        } else {
            results.append("100% Daylight");
            results.append("<br>");
        }

        if (lightRainfallChance > 0) {
            likelyWeather = worstWeather = Weather.LIGHT_RAIN;
            weatherProb = worstWeatherProb = lightRainfallChance;
        }

        if (lightSnowfallChance > 0) {
            if (lightSnowfallChance > weatherProb) {
                likelyWeather = Weather.LIGHT_SNOW;
                weatherProb = lightSnowfallChance;
            }
            worstWeather = Weather.LIGHT_SNOW;
            worstWeatherProb = lightSnowfallChance;
        }

        if (moderateRainfallChance > 0) {
            if (moderateRainfallChance > weatherProb) {
                likelyWeather = Weather.MOD_RAIN;
                weatherProb = moderateRainfallChance;
            }
            worstWeather = Weather.MOD_RAIN;
            worstWeatherProb = moderateRainfallChance;
        }

        if (moderateSnowfallChance > 0) {
            if (moderateSnowfallChance > weatherProb) {
                likelyWeather = Weather.MOD_SNOW;
                weatherProb = moderateSnowfallChance;
            }
            worstWeather = Weather.MOD_SNOW;
            worstWeatherProb = moderateSnowfallChance;
        }

        if (heavyRainfallChance > 0) {
            if (heavyRainfallChance > weatherProb) {
                likelyWeather = Weather.HEAVY_RAIN;
                weatherProb = heavyRainfallChance;
            }
            worstWeather = Weather.HEAVY_RAIN;
            worstWeatherProb = heavyRainfallChance;
        }

        if (heavySnowfallChance > 0) {
            if (heavySnowfallChance > weatherProb) {
                likelyWeather = Weather.HEAVY_SNOW;
                weatherProb = heavySnowfallChance;
            }
            worstWeather = Weather.HEAVY_SNOW;
            worstWeatherProb = heavySnowfallChance;
        }

        if (downPourChance > 0) {
            if (downPourChance > weatherProb) {
                likelyWeather = Weather.DOWNPOUR;
                weatherProb = downPourChance;
            }
            worstWeather = Weather.DOWNPOUR;
            worstWeatherProb = downPourChance;
        }

        results.append("Weather:");
        if (weatherProb > 0) {
            results.append(weatherProb / 10);
            results.append("% ");
            results.append(likelyWeather);
            results.append(" / ");
            results.append(worstWeatherProb / 10);
            results.append("% ");
            results.append(worstWeather);

            if (lightHailChance > 0 || heavyHailChance > 0) {
                results.append(" (hail)");
            }

            if (sleetChance > 0) {
                results.append(" (sleet)");
            }

            if (iceStormChance > 0) {
                results.append(" (ice storm)");
            }

            results.append("<br>");
        } else {
            results.append("100% Clear");
            results.append("<br>");
        }

        if (lightWindsChance > 0) {
            likelyWind = worstWind = Wind.LIGHT_GALE;
            windProb = worstWindProb = lightWindsChance;
        }

        if (moderateWindsChance > 0) {
            if (moderateWindsChance > weatherProb) {
                likelyWind = Wind.MOD_GALE;
                windProb = moderateWindsChance;
            }
            worstWind = Wind.MOD_GALE;
            worstWindProb = moderateWindsChance;
        }

        if (strongWindsChance > 0) {
            if (strongWindsChance > weatherProb) {
                likelyWind = Wind.STRONG_GALE;
                windProb = strongWindsChance;
            }
            worstWind = Wind.STRONG_GALE;
            worstWindProb = strongWindsChance;
        }

        if (stormWindsChance > 0) {
            if (stormWindsChance > weatherProb) {
                likelyWind = Wind.STORM;
                windProb = stormWindsChance;
            }
            worstWind = Wind.STORM;
            worstWindProb = stormWindsChance;
        }

        if (tornadoF13WindsChance > 0) {
            if (tornadoF13WindsChance > weatherProb) {
                likelyWind = Wind.TORNADO_F1_TO_F3;
                windProb = tornadoF13WindsChance;
            }
            worstWind = Wind.TORNADO_F1_TO_F3;
            worstWindProb = tornadoF13WindsChance;
        }

        if (tornadoF4WindsChance > 0) {
            if (tornadoF4WindsChance > weatherProb) {
                likelyWind = Wind.TORNADO_F4;
                windProb = tornadoF4WindsChance;
            }
            worstWind = Wind.TORNADO_F4;
            worstWindProb = tornadoF4WindsChance;
        }

        results.append("Wind:");
        if (windProb > 0) {
            results.append(windProb / 10);
            results.append("% ");
            results.append(likelyWind);
            results.append(" / ");
            results.append(worstWindProb / 10);
            results.append("% ");
            results.append(worstWind);
            results.append("<br>");
        } else {
            results.append("100% Calm");
            results.append("<br>");
        }

        if (lightFogChance > 0 || heavyFogChance > 0) {
            results.append("Fog:");
            results.append((float) Math.max(lightFogChance, heavyFogChance) / 10);
            results.append("% ");
        }

        results.append("<br>");

        return results.toString();
    }

    public String getHumanReadableWeather() {
        StringBuilder results = new StringBuilder();
        int adverse = 0;

        results.append(lightConditions);
        results.append("/").append(weatherConditions);
        results.append("/").append(windStrength);
        results.append("/").append(fog);
        results.append("/").append(atmosphere);
        results.append("/");
        results.append(gravity);

        if (lightConditions != Light.DAY) {
            adverse++;
        }

        if (weatherConditions != Weather.CLEAR) {
            adverse++;
        }

        if (windStrength != Wind.CALM) {
            adverse++;
        }

        if (fog != Fog.FOG_NONE) {
            adverse++;
        }

        if (atmosphere != Atmosphere.STANDARD) {
            adverse++;
        }

        if (gravity != 1.0) {
            adverse++;
        }

        results.append("/").append(adverse);

        return results.toString();
    }

}
