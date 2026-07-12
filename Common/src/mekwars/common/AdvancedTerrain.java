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
 * Describes the "planetary conditions" profile for a location (a planet, continent, or terrain) — everything about
 * the physical environment of a battlefield that isn't the hex-by-hex map layout itself: temperature range, gravity,
 * vacuum, atmosphere, visibility, the day/night cycle (dusk/full-moon/moonless/pitch-black chances), precipitation
 * (rain/snow/hail/sleet/ice-storm chances), wind (strength tiers up to tornado, direction, and whether either shifts
 * over the course of a battle), fog, and EMI (electromagnetic interference).
 * <p>
 * Most fields here are stored as MekWars-specific "chance" percentages (weights used to randomly roll one condition
 * per category when a battle starts — see {@link #WeatherForecast()} for the "most likely / worst case" roll logic),
 * while the {@code *Conditions}/{@code fog}/{@code windStrength} etc. fields mirror MegaMek's own
 * {@code megamek.common.planetaryConditions} enums directly and are what actually gets handed to the MegaMek game
 * engine (analogous to populating a MegaMek {@code PlanetaryConditions} object) when a battle is launched using this
 * profile.
 *
 * @author Torren (Jason Tighe) allows So's to set up each terrain on a planet.
 */

final public class AdvancedTerrain {

    /** Name shown to players for this planetary-conditions profile; kept in sync with {@link #Name} by {@link #setDisplayName} / {@link #setName}. */
    private String displayName = "none";
    /** Unique identifier for this profile (see {@link #getId()}/{@link #setId(int)}; note the setter's parameter is misleadingly named "unusedTerrainID"). */
    private int id = 0;
    /** Internal name, kept identical to {@link #displayName}; retained for compatibility with older code/serialization paths. */
    private String Name = "none";

    /** Low end of the ambient temperature range, in degrees Celsius. */
    private int lowTemp = 25;
    /** High end of the ambient temperature range, in degrees Celsius. */
    private int highTemp = 25;
    /** Surface gravity relative to Terran standard (1.0 = 1G); affects MegaMek movement/piloting rules. */
    private double gravity = 1.0;
    /** Whether this location is in a vacuum (no atmosphere for combustion/breathing); currently only stored, not read by any method in this class. */
    private boolean vacuum = false;
    /** Percent chance (x10 scale, i.e. stored value / 10 = actual percent) of dusk lighting conditions. */
    private int duskChance = 0;
    /** Percent chance (x10 scale) of full-moon night lighting; despite the name this backs {@link #getNightChance()}/{@link #setNightChance(int)}. */
    private int fullMoonChance = 0;
    /** Percent chance (x10 scale) of moonless-night lighting. */
    private int moonlessChance = 0;
    /** Percent chance (x10 scale) of pitch-black night lighting. */
    private int pitchBlackChance = 0;
    /** Temperature modifier applied at night. */
    private int nightTempMod = 0;
    /** Minimum sensor/visual visibility distance. */
    private int minVisibility = 100;
    /** Maximum sensor/visual visibility distance. */
    private int maxVisibility = 100;
    /** Planetary atmosphere type; maps directly to a MegaMek {@link Atmosphere} value. */
    private Atmosphere atmosphere = Atmosphere.STANDARD;

    /** Percent chance (x10 scale) of light rainfall. */
    private int lightRainfallChance = 0;
    /** Percent chance (x10 scale) of moderate rainfall. */
    private int moderateRainfallChance = 0;
    /** Percent chance (x10 scale) of heavy rainfall. */
    private int heavyRainfallChance = 0;
    /** Percent chance (x10 scale) of downpour-level rainfall. */
    private int downPourChance = 0;

    /** Percent chance (x10 scale) of light snowfall. */
    private int lightSnowfallChance = 0;
    /** Percent chance (x10 scale) of moderate snowfall. */
    private int moderateSnowfallChance = 0;
    /** Percent chance (x10 scale) of heavy snowfall. */
    private int heavySnowfallChance = 0;
    /** Percent chance (x10 scale) of sleet. */
    private int sleetChance = 0;
    /** Percent chance (x10 scale) of an ice storm. */
    private int iceStormChance = 0;
    /** Percent chance (x10 scale) of light hail. */
    private int lightHailChance = 0;
    /** Percent chance (x10 scale) of heavy hail. */
    private int heavyHailChance = 0;

    /** Percent chance (x10 scale) of light winds. */
    private int lightWindsChance = 0;
    /** Percent chance (x10 scale) of moderate winds. */
    private int moderateWindsChance = 0;
    /** Percent chance (x10 scale) of strong winds. */
    private int strongWindsChance = 0;
    /** Percent chance (x10 scale) of storm-force winds. */
    private int stormWindsChance = 0;
    /** Percent chance (x10 scale) of an F1-F3 tornado. */
    private int tornadoF13WindsChance = 0;
    /** Percent chance (x10 scale) of an F4 tornado. */
    private int tornadoF4WindsChance = 0;

    /** Percent chance (x10 scale) of light fog. */
    private int lightFogChance = 0;
    /** Percent chance (x10 scale) of heavy fog. */
    private int heavyFogChance = 0;

    /** Percent chance (x10 scale) of electromagnetic interference (EMI). */
    private int emiChance = 0;

    // MegaMek Planetary Conditions
    // set up the specific conditions
    /** The actual MegaMek lighting condition rolled/selected for a battle using this profile. */
    private Light lightConditions = Light.DAY;
    /** The actual MegaMek weather condition rolled/selected for a battle. */
    private Weather weatherConditions = Weather.CLEAR;
    /** The actual MegaMek wind strength rolled/selected for a battle. */
    private Wind windStrength = Wind.CALM;
    /** The actual MegaMek wind direction rolled/selected for a battle. */
    private WindDirection windDirection = WindDirection.RANDOM;
    /** Upper bound on wind strength the battle is allowed to escalate to if {@link #shiftWindStrength} is enabled. */
    private Wind maxWindStrength = Wind.TORNADO_F4;
    /** Whether wind direction is allowed to shift/change during the battle. */
    private boolean shiftWindDirection = false;
    /** Whether wind strength is allowed to shift/change during the battle. */
    private boolean shiftWindStrength = false;
    /** The actual MegaMek fog condition rolled/selected for a battle. */
    private Fog fog = Fog.FOG_NONE;
    /** The actual ambient temperature (degrees Celsius) rolled/selected for a battle. */
    private int temperature = 25;
    /** Whether electromagnetic interference is active for a battle using this profile. */
    private EMI emi = EMI.EMI_NONE;
    /** Whether the selected weather/lighting conditions actually affect terrain (e.g. snow cover); passed through to MegaMek. */
    private boolean terrainAffected = true;

    /**
     * Parses an {@code AdvancedTerrain} planetary-conditions profile from its {@code "$"}-delimited string form (as
     * produced by {@link #toString()}), reading fields in a fixed order via {@link TokenReader}. The MekWars-specific
     * "chance" percentages are read first, followed last by the fields that mirror MegaMek's own
     * {@code planetaryConditions} enums (light, weather, wind, fog, temperature, EMI, terrain-affected,
     * max wind strength) — the comment in the original code notes these must stay last since they were added later.
     *
     * @param s the delimited string to parse
     */
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

    /**
     * For Serialisation / building a profile programmatically via the setters.
     */
    public AdvancedTerrain() {
    }

    /** @param chance the F1-F3 tornado wind chance (x10 scale) to set. */
    public void setTornadoF13WindChance(int chance) {
        tornadoF13WindsChance = chance;
    }

    /** @param chance the light winds chance (x10 scale) to set. */
    public void setLightWindChance(int chance) {
        lightWindsChance = chance;
    }

    /** @param shift whether wind direction may shift during a battle. */
    public void setShiftingWindDirection(boolean shift) {
        shiftWindDirection = shift;
    }

    /** @param strength whether wind strength may shift during a battle. */
    public void setShiftingWindStrength(boolean strength) {
        shiftWindStrength = strength;
    }

    /** @param emi the EMI condition to set for a battle using this profile. */
    public void setEMI(EMI emi) {
        this.emi = emi;
    }

    /**
     * Reads this profile's MekWars "chance" fields (temperature, gravity, vacuum, visibility, precipitation, wind,
     * fog, EMI chances) back from a binary stream previously written by {@link #binOut}.
     * <p>
     * Quirk/gap: unlike the {@link #AdvancedTerrain(String)} string constructor and {@link #toString()}, this method
     * (and {@link #binOut}) does <b>not</b> read or write the MegaMek-facing "actual condition" fields
     * ({@link #lightConditions}, {@link #weatherConditions}, {@link #windStrength}, {@link #windDirection},
     * {@link #shiftWindDirection}, {@link #shiftWindStrength}, {@link #fog}, {@link #temperature}, {@link #emi},
     * {@link #terrainAffected}, {@link #maxWindStrength}) — those fields keep their default values
     * ({@code Light.DAY}, {@code Weather.CLEAR}, {@code Wind.CALM}, etc.) after a binary round-trip unless something
     * else sets them afterward.
     *
     * @param in the binary reader positioned at the start of this profile's data
     */
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

    /**
     * Writes this profile's MekWars "chance" fields as a binary stream. See {@link #binIn} for the corresponding gap
     * where the MegaMek-facing "actual condition" fields are not persisted by this pair of methods.
     */
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

    /** @return the display name of this profile ({@link #displayName}). */
    public String getDisplayName() {
        return displayName;
    }

    /** @param name sets both {@link #displayName} and the legacy {@link #Name} field to this value. */
    public void setDisplayName(String name) {
        displayName = name;
        Name = name;
    }

    /** @return low end of the temperature range in degrees Celsius ({@link #lowTemp}). */
    public int getLowTemp() {
        return lowTemp;
    }

    /** @param temp the low temperature (Celsius) to set. */
    public void setLowTemp(int temp) {
        lowTemp = temp;
    }

    /** @return high end of the temperature range in degrees Celsius ({@link #highTemp}). */
    public int getHighTemp() {
        return highTemp;
    }

    /** @param temp the high temperature (Celsius) to set. */
    public void setHighTemp(int temp) {
        highTemp = temp;
    }

    /** @return surface gravity relative to 1G ({@link #gravity}). */
    public double getGravity() {
        return gravity;
    }

    /** @param grav the surface gravity (relative to 1G) to set. */
    public void setGravity(double grav) {
        gravity = grav;
    }

    /** @return percent chance of dusk lighting (x10 scale, {@link #duskChance}). */
    public int getDuskChance() {
        return duskChance;
    }

    /** @param chance the dusk lighting chance to set. */
    public void setDuskChance(int chance) {
        duskChance = chance;
    }

    /** @return percent chance of full-moon night lighting (x10 scale); backed by {@link #fullMoonChance}. */
    public int getNightChance() {
        return fullMoonChance;
    }

    /** @param chance the full-moon night lighting chance to set. */
    public void setNightChance(int chance) {
        fullMoonChance = chance;
    }

    /** @return percent chance of moonless-night lighting (x10 scale, {@link #moonlessChance}). */
    public int getMoonLessNightChance() {
        return moonlessChance;
    }

    /** @param chance the moonless-night lighting chance to set. */
    public void setMoonLessNightChance(int chance) {
        moonlessChance = chance;
    }

    /** @return percent chance of pitch-black-night lighting (x10 scale, {@link #pitchBlackChance}). */
    public int getPitchBlackNightChance() {
        return pitchBlackChance;
    }

    /** @param chance the pitch-black-night lighting chance to set. */
    public void setPitchBlackNightChance(int chance) {
        pitchBlackChance = chance;
    }

    /** @return the temperature modifier applied at night ({@link #nightTempMod}). */
    public int getNightTempMod() {
        return nightTempMod;
    }

    /** @param mod the night temperature modifier to set. */
    public void setNightTempMod(int mod) {
        nightTempMod = mod;
    }

    /** @return minimum visibility distance ({@link #minVisibility}). */
    public int getMinVisibility() {
        return minVisibility;
    }

    /** @param minVisibility the minimum visibility distance to set. */
    public void setMinVisibility(int minVisibility) {
        this.minVisibility = minVisibility;
    }

    /** @return maximum visibility distance ({@link #maxVisibility}). */
    public int getMaxVisibility() {
        return maxVisibility;
    }

    /** @param maxVisibility the maximum visibility distance to set. */
    public void setMaxVisibility(int maxVisibility) {
        this.maxVisibility = maxVisibility;
    }

    /** @return percent chance of moderate snowfall (x10 scale, {@link #moderateSnowfallChance}). */
    public int getModerateSnowFallChance() {
        return moderateSnowfallChance;
    }

    /** @param chance the moderate snowfall chance to set. */
    public void setModerateSnowFallChance(int chance) {
        moderateSnowfallChance = chance;
    }

    /** @return percent chance of moderate rainfall (x10 scale, {@link #moderateRainfallChance}). */
    public int getModerateRainFallChance() {
        return moderateRainfallChance;
    }

    /** @param chance the moderate rainfall chance to set. */
    public void setModerateRainFallChance(int chance) {
        moderateRainfallChance = chance;
    }

    /** @return percent chance of heavy snowfall (x10 scale, {@link #heavySnowfallChance}). */
    public int getHeavySnowfallChance() {
        return heavySnowfallChance;
    }

    /** @param chance the heavy snowfall chance to set. */
    public void setHeavySnowfallChance(int chance) {
        heavySnowfallChance = chance;
    }

    /** @return percent chance of light rainfall (x10 scale, {@link #lightRainfallChance}). */
    public int getLightRainfallChance() {
        return lightRainfallChance;
    }

    /** @param chance the light rainfall chance to set. */
    public void setLightRainfallChance(int chance) {
        lightRainfallChance = chance;
    }

    /** @return percent chance of heavy rainfall (x10 scale, {@link #heavyRainfallChance}). */
    public int getHeavyRainfallChance() {
        return heavyRainfallChance;
    }

    /** @param chance the heavy rainfall chance to set. */
    public void setHeavyRainfallChance(int chance) {
        heavyRainfallChance = chance;
    }

    /** @return percent chance of moderate winds (x10 scale, {@link #moderateWindsChance}). */
    public int getModerateWindsChance() {
        return moderateWindsChance;
    }

    /** @param chance the moderate winds chance to set. */
    public void setModerateWindsChance(int chance) {
        moderateWindsChance = chance;
    }

    /** @return percent chance of strong winds (x10 scale, {@link #strongWindsChance}). */
    public int getStrongWindsChance() {
        return strongWindsChance;
    }

    /** @param chance the strong winds chance to set. */
    public void setStrongWindsChance(int chance) {
        strongWindsChance = chance;
    }

    /** @return percent chance of storm-force winds (x10 scale, {@link #stormWindsChance}). */
    public int getStormWindsChance() {
        return stormWindsChance;
    }

    /** @param chance the storm-force winds chance to set. */
    public void setStormWindsChance(int chance) {
        stormWindsChance = chance;
    }

    /** @return percent chance of light winds (x10 scale, {@link #lightWindsChance}); note there is no corresponding public getter-paired setter with this exact name — see {@link #setLightWindChance(int)}. */
    public int getLightWindsChance() {
        return lightWindsChance;
    }

    /** @return percent chance of an F1-F3 tornado (x10 scale, {@link #tornadoF13WindsChance}); set via {@link #setTornadoF13WindChance(int)}. */
    public int getTornadoF13WindsChance() {
        return tornadoF13WindsChance;
    }

    /** @return percent chance of an F4 tornado (x10 scale, {@link #tornadoF4WindsChance}). */
    public int getTornadoF4WindsChance() {
        return tornadoF4WindsChance;
    }

    /** @param chance the F4 tornado chance to set. */
    public void setTornadoF4WindsChance(int chance) {
        tornadoF4WindsChance = chance;
    }

    /** @return percent chance of downpour-level rainfall (x10 scale, {@link #downPourChance}). */
    public int getDownPourChance() {
        return downPourChance;
    }

    /** @param chance the downpour chance to set. */
    public void setDownPourChance(int chance) {
        downPourChance = chance;
    }

    /** @return percent chance of light snowfall (x10 scale, {@link #lightSnowfallChance}). */
    public int getLightSnowfallChance() {
        return lightSnowfallChance;
    }

    /** @param chance the light snowfall chance to set. */
    public void setLightSnowfallChance(int chance) {
        lightSnowfallChance = chance;
    }

    /** @return percent chance of sleet (x10 scale, {@link #sleetChance}). */
    public int getSleetChance() {
        return sleetChance;
    }

    /** @param chance the sleet chance to set. */
    public void setSleetChance(int chance) {
        sleetChance = chance;
    }

    /** @return percent chance of an ice storm (x10 scale, {@link #iceStormChance}). */
    public int getIceStormChance() {
        return iceStormChance;
    }

    /** @param chance the ice storm chance to set. */
    public void setIceStormChance(int chance) {
        iceStormChance = chance;
    }

    /** @return percent chance of light hail (x10 scale, {@link #lightHailChance}). */
    public int getLightHailChance() {
        return lightHailChance;
    }

    /** @param chance the light hail chance to set. */
    public void setLightHailChance(int chance) {
        lightHailChance = chance;
    }

    /** @return percent chance of heavy hail (x10 scale, {@link #heavyHailChance}). */
    public int getHeavyHailChance() {
        return heavyHailChance;
    }

    /** @param chance the heavy hail chance to set. */
    public void setHeavyHailChance(int chance) {
        heavyHailChance = chance;
    }

    /**
     * Creates a copy of this profile by constructing a new {@code AdvancedTerrain} and explicitly copying most
     * fields over via their setters. Field additions to the class must remember to also add a line here, or the
     * clone will silently miss that field — as of this writing {@link #id}, {@link #vacuum}, {@link #minVisibility},
     * {@link #maxVisibility}, and {@link #emiChance} are NOT copied by this method, so a cloned profile silently
     * reverts those to their class defaults even though the source object had different values.
     * <p>
     * Note this overrides {@link Object#clone()} without implementing {@link Cloneable} or declaring
     * {@code CloneNotSupportedException}, and without an {@code @Override} annotation — it works because it never
     * calls {@code super.clone()}, but static analysis tools may flag the missing {@code @Override}.
     */
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

    /**
     * Serializes the MekWars "chance" fields (and {@link #displayName}, defaulting to {@code "Terrain"} if blank) of
     * this profile to a {@code "$"}-delimited string. This alone is <b>not</b> the full round-trip counterpart of
     * {@link #AdvancedTerrain(String)}: the MegaMek-facing "actual condition" fields (light/weather/wind/fog/
     * temperature/EMI/terrain-affected/max wind strength) are appended separately by
     * {@link #toStringPlanetaryConditions()}, which calls this method first and appends the rest — use that method
     * (not this one) to get a string that {@link #AdvancedTerrain(String)} can fully parse back.
     * <p>
     * Quirk: {@link #atmosphere} is appended here via its {@code toString()} (whatever text representation the
     * MegaMek {@link Atmosphere} enum provides), while {@link #binOut} instead writes {@code atmosphere.ordinal()}
     * and the {@link #AdvancedTerrain(String)} constructor reads it back via {@code Atmosphere.getAtmosphere(int)}
     * from an integer token — if {@code Atmosphere.toString()} does not render as a plain integer, round-tripping a
     * profile purely through this string form could fail to parse the atmosphere field correctly.
     */
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

    /** @return the actual MegaMek lighting condition set for a battle ({@link #lightConditions}). */
    public Light getLightConditions() {
        return lightConditions;
    }

    /** @param light the MegaMek lighting condition to set. */
    public void setLightConditions(Light light) {
        lightConditions = light;
    }

    /** @return the actual MegaMek weather condition set for a battle ({@link #weatherConditions}). */
    public Weather getWeatherConditions() {
        return weatherConditions;
    }

    /** @param weather the MegaMek weather condition to set. */
    public void setWeatherConditions(Weather weather) {
        weatherConditions = weather;
    }

    /** @return the actual MegaMek wind strength set for a battle ({@link #windStrength}). */
    public Wind getWindStrength() {
        return windStrength;
    }

    /** @param wind the MegaMek wind strength to set. */
    public void setWindStrength(Wind wind) {
        windStrength = wind;
    }

    /** @return the actual MegaMek wind direction set for a battle ({@link #windDirection}). */
    public WindDirection getWindDirection() {
        return windDirection;
    }

    /** @param dir the MegaMek wind direction to set. */
    public void setWindDirection(WindDirection dir) {
        windDirection = dir;
    }

    /** @return whether wind direction is allowed to shift during a battle ({@link #shiftWindDirection}). */
    public boolean hasShiftingWindDirection() {
        return shiftWindDirection;
    }

    /** @return whether wind strength is allowed to shift during a battle ({@link #shiftWindStrength}). */
    public boolean hasShiftingWindStrength() {
        return shiftWindStrength;
    }

    /**
     * Builds the full {@code "$"}-delimited persistence string for this profile: the base {@link #toString()} output
     * (MekWars "chance" fields) followed by every MegaMek-facing "actual condition" field in the exact order expected
     * by {@link #AdvancedTerrain(String)}. This — not plain {@link #toString()} — is the method whose output can be
     * round-tripped through that constructor.
     */
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

    /** @return whether the selected weather/lighting actually affects terrain ({@link #terrainAffected}). */
    public boolean isTerrainAffected() {
        return terrainAffected;
    }

    /** @param terrain whether weather/lighting should affect terrain. */
    public void setTerrainAffected(boolean terrain) {
        terrainAffected = terrain;
    }

    /** @return the EMI condition active for a battle using this profile ({@link #emi}); despite the boolean-sounding name, this returns the {@link EMI} enum value, not a {@code boolean}. */
    public EMI hasEMI() {
        return emi;
    }

    /** @return the ambient temperature in degrees Celsius set for a battle ({@link #temperature}). */
    public int getTemperature() {
        return temperature;
    }

    /** @param temp the ambient temperature (Celsius) to set. */
    public void setTemperature(int temp) {
        temperature = temp;
    }

    /** @return the actual MegaMek fog condition set for a battle ({@link #fog}). */
    public Fog getFog() {
        return fog;
    }

    /** @param fog the MegaMek fog condition to set. */
    public void setFog(Fog fog) {
        this.fog = fog;
    }

    /** @return the planetary atmosphere type ({@link #atmosphere}). */
    public Atmosphere getAtmosphere() {
        return atmosphere;
    }

    /** @param atmo the planetary atmosphere type to set. */
    public void setAtmosphere(Atmosphere atmo) {
        atmosphere = atmo;
    }

    /** @return percent chance of light fog (x10 scale, {@link #lightFogChance}). */
    public int getLightFogChance() {
        return lightFogChance;
    }

    /** @param chance the light fog chance to set. */
    public void setLightFogChance(int chance) {
        lightFogChance = chance;
    }

    /** @return percent chance of heavy fog (x10 scale, {@link #heavyFogChance}). */
    public int getHeavyFogChance() {
        return heavyFogChance;
    }

    /** @param chance the heavy fog chance to set. */
    public void setHeavyFogChance(int chance) {
        heavyFogChance = chance;
    }

    /** @return percent chance of electromagnetic interference (x10 scale, {@link #emiChance}). */
    public int getEMIChance() {
        return emiChance;
    }

    /** @param chance the EMI chance to set. */
    public void setEMIChance(int chance) {
        emiChance = chance;
    }

    /** @return the upper bound wind strength a battle may escalate to ({@link #maxWindStrength}). */
    public Wind getMaxWindStrength() {
        return maxWindStrength;
    }

    /** @param wind the maximum wind strength to set. */
    public void setMaxWindStrength(Wind wind) {
        maxWindStrength = wind;
    }

    /** @return this profile's unique id ({@link #id}). */
    public int getId() {
        return id;
    }

    /**
     * @param unusedTerrainID the id to set; the parameter name is a holdover and does not imply this value is
     *                        actually unused — it is stored in {@link #id} and returned by {@link #getId()}.
     */
    public void setId(int unusedTerrainID) {
        id = unusedTerrainID;
    }

    /** @return this profile's name ({@link #Name}, kept identical to {@link #displayName}). */
    public String getName() {
        return Name;
    }

    /** @param name sets both {@link #Name} and {@link #displayName} to this value; equivalent to {@link #setDisplayName(String)}. */
    public void setName(String name) {
        displayName = name;
        Name = name;
    }

    /**
     * Builds a small HTML {@code <table>} of every MegaMek-facing "actual condition" field (lighting, weather, wind,
     * fog, temperature, EMI, terrain-affected, max wind strength) for display/debugging purposes.
     */
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

    /**
     * Builds a human-readable, {@code <br>}-separated "likely / worst case" forecast string summarizing this
     * profile's light, weather, and wind chances (each as "X% likely-condition / Y% worst-condition"), plus a fog
     * note if either fog chance is non-zero. For each category the "worst" value is simply the last condition tier
     * examined that had a non-zero chance (tiers are checked in increasing severity order), while "likely" is
     * whichever tier had the single highest chance percentage seen so far.
     * <p>
     * Bug: in the wind section, the comparisons for {@link #moderateWindsChance}, {@link #strongWindsChance},
     * {@link #stormWindsChance}, {@link #tornadoF13WindsChance}, and {@link #tornadoF4WindsChance} are each written
     * as {@code if (xChance > weatherProb)} — reusing the weather category's running-maximum variable instead of the
     * wind category's {@code windProb}. Since {@code weatherProb} was already finalized by the preceding weather
     * block, this means "likely wind" is effectively compared against the likely-weather percentage rather than the
     * previous likely-wind percentage, which can make {@code likelyWind} pick a different (often lower) tier than a
     * correct wind-only comparison would produce. Left as-is since this only affects the forecast text, not actual
     * game setup.
     */
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

    /**
     * Builds a compact {@code "/"}-separated summary of the actual rolled/selected conditions for a battle
     * (lighting/weather/wind/fog/atmosphere/gravity), followed by an "adverse conditions" count: the number of
     * those six values that differ from their fair-weather default (Day / Clear / Calm / No Fog / Standard
     * atmosphere / 1.0 gravity). A higher trailing count means a harsher environment overall.
     */
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
