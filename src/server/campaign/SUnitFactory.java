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
 * original author - @McWizard
 * rewritten extensively on 2/04/03. @urgru. 
 * 
 * factories now produce on demand, and only decrement
 * their reset counters during ticks.
 */

package server.campaign;

import java.io.File;
import java.io.Serializable;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.Vector;

import common.Unit;
import common.UnitFactory;
import common.util.MWLogger;
import common.util.TokenReader;
import server.campaign.pilot.SPilot;
import server.campaign.util.SerializedMessage;


public class SUnitFactory extends UnitFactory implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1735176578439214960L;
    // VARIABLES
    private SPlanet planet;
    
    // CONSTRUCTORS
    public SUnitFactory() {
        // empty
    }

    public SUnitFactory(String Name, SPlanet P, String Size, String Faction, int ticksuntilrefresh, int refreshSpeed, int type, String buildTableFolder, int accessLevel) {
        setName(Name);
        setPlanet(P);
        setSize(Size);
        setFounder(Faction);
        setTicksUntilRefresh(ticksuntilrefresh);
        setRefreshSpeed(refreshSpeed);
        setType(type);
        setBuildTableFolder(buildTableFolder);
        setAccessLevel(accessLevel);
    }

    // STRING SAVE METHODS
    /**
     * Used for Serialisation
     * 
     * @return A Serialised form of the UnitFactory
     */
    @Override
    public String toString() {
        SerializedMessage result = new SerializedMessage("*");
        result.append("MF");
        result.append(getName());
        result.append(getSize());
        result.append(getFounder());
        result.append(getTicksUntilRefresh());
        result.append(getRefreshSpeed());

        String buildtablefolder = getBuildTableFolder().replaceAll(BuildTable.STANDARD + "\\" + File.separatorChar, "");

        if (buildtablefolder.trim().length() < 1 || buildtablefolder.equals(BuildTable.STANDARD))
            result.append("0");
        else
            result.append(buildtablefolder);

        result.append(getType());
        result.append(isLocked());
        result.append(getAccessLevel());
        result.append(getID());
        return result.toString();
    }

    /**
     * Used to DE-Serialise a MF
     * 
     * @param s
     *            The Serialised Version
     * @param p
     *            A SPlanet where this MF is placed upon
     * @param r
     *            The Random Object
     */
    public void fromString(String s, SPlanet p, Random r) {
        s = s.substring(3);
        StringTokenizer ST = new StringTokenizer(s, "*");
        setName(TokenReader.readString(ST));
        setSize(TokenReader.readString(ST));
        setFounder(TokenReader.readString(ST));
        setTicksUntilRefresh(TokenReader.readInt(ST));
        setRefreshSpeed(TokenReader.readInt(ST));

        setBuildTableFolder(TokenReader.readString(ST));

        setType(TokenReader.readInt(ST));
        setLock(TokenReader.readBoolean(ST));
        setAccessLevel(TokenReader.readInt(ST));
        if(ST.hasMoreTokens()) {
        	setID(TokenReader.readString(ST));
        } else {
        	setID(UUID.randomUUID().toString());
        }
        setPlanet(p);
    }

    // METHODS
    public String getIcons() {
        // TODO: Add more icons to make this unambiguous
        String sizeid = "";
        String result = "";
        int size = getWeightclass();
        if (size == Unit.LIGHT)
            sizeid += "l";
        else if (size == Unit.MEDIUM)
            sizeid += "m";
        else if (size == Unit.HEAVY)
            sizeid += "h";
        else if (size == Unit.ASSAULT)
            sizeid += "a";
        if (canProduce(Unit.MEK))
            sizeid += "m";
        else if (canProduce(Unit.VEHICLE))
            sizeid += "v";
        else if (canProduce(Unit.INFANTRY))
            sizeid += "li";// override size w/ light
        else if (canProduce(Unit.BATTLEARMOR))
            sizeid += "b";
        else if (canProduce(Unit.PROTOMEK))
            sizeid += "p";
        else if (canProduce(Unit.AERO))
            sizeid += "ae";

        result += "<img src=\"data/images/" + sizeid + ".gif\">";
        return result;
    }

    /**
     * Have the factory build a unit. This should be called only as the result
     * of a tick (overflow production) or RequestCommand. Any other use should
     * be avoided.
     * 
     * @return the Mek Produced
     */
    public Vector<SUnit> getMechProduced(int type_id, SPilot pilot) {

        // Build the fluff text for the mek
        String Filename = "";
        String producer = "Built by ";
        Vector<SUnit> units = new Vector<SUnit>(1, 1);

        if (this.getPlanet().getOwner() != null)
            producer += this.getPlanet().getOwner().getName();
        else
            producer += this.getFounder();

        /*
         * add a production location to the fluff, if from a normal planet. null
         * planet will normally be reward point production.
         */
        if (this.getPlanet().getName() != null)
            producer += " on " + this.getPlanet().getName();

        String unitSize = getSize();
        if (CampaignMain.cm.getBooleanConfig("UseOnlyOneVehicleSize") && type_id == Unit.VEHICLE)
            unitSize = Unit.getWeightClassDesc(CampaignMain.cm.getRandomNumber(4));

        Filename = BuildTable.getUnitFilename(this.getFounder(), unitSize, type_id, getBuildTableFolder());
        // log the creation
        String buildtableName = this.getFounder() + "_" + this.getSize();
        if (type_id != Unit.MEK)
            buildtableName += Unit.getTypeClassDesc(type_id);

		if(this.getPlanet().getOwner() != null)
			MWLogger.infoLog("New unit for " + this.getPlanet().getOwner().getName() + " on " + this.getPlanet().getName() + ": " + Filename + "(Table: " + buildtableName + ")");
		else 
			MWLogger.infoLog("New unit for " + this.getFounder() + " on " + this.getPlanet().getName() + ": " + Filename + "(Table: " + buildtableName + ")");
		
        if (Filename.toLowerCase().trim().endsWith(".mul")) {
            units.addAll(SUnit.createMULUnits(Filename, producer));
        } else {
            // Build the unit & create history entry
            SUnit cm = new SUnit(producer, Filename, this.getWeightclass());
            cm.setPilot(pilot);
            units.add(cm);
        }
        return units;
    }

    /**
     * Add or remove refresh time to a factory. This should ALWAYS be used in
     * lieu of super.setTicksUntilRefresh(), as it properly (albeit hackishly)
     * updates players' clients with accurate refresh times.
     */
    public String addRefresh(int i, boolean sendHSUpdate) {
    	MWLogger.debugLog("Starting refresh on " + getName() + ", adding " + i);
        int startRefresh = getTicksUntilRefresh();

        setTicksUntilRefresh(getTicksUntilRefresh() + i);
        if (getTicksUntilRefresh() < 0)
            setTicksUntilRefresh(0);

        if (getTicksUntilRefresh() == startRefresh)
            return "";

        /*
         * Change the factory's information (refresh time) Format:
         * HS|CF|weight$metatype$planet$name$timetorefresh$accessLevel|
         */
        String hsUpdate = "CF|" + getWeightclass() + "$" + getType() + "$" + getPlanet().getName() + "$" + getName() + "$" + getTicksUntilRefresh() + "$" + getAccessLevel() + "|";

        if (sendHSUpdate) {
            SHouse owner = getPlanet().getOwner();
            if (owner != null)
                CampaignMain.cm.doSendToAllOnlinePlayers(owner, "HS|" + hsUpdate, false);
        }

        return hsUpdate;
    }

    /**
     * @return Returns the planet.
     */
    public SPlanet getPlanet() {
        return planet;
    }

    /**
     * @param planet
     *            The planet to set.
     */
    public void setPlanet(SPlanet pl) {
        this.planet = pl;
    }

    /**
     * The cost (money) of a unit from this factory. Back referenced to the
     * faction that originally owned the world. Hacky. Ugly.
     */
    public int getPriceForUnit(int weightclass, int typeid) {
        SHouse originalHouse = (SHouse) CampaignMain.cm.getData().getHouseByName(this.getFounder());
        return originalHouse.getPriceForUnit(weightclass, typeid);
    }

    /**
     * The cost (flu) of a unit from this factory. Back referenced to the
     * faction that originally owned the world. Hacky. Ugly.
     */
    public int getInfluenceForUnit(int weightclass, int typeid) {
        SHouse originalHouse = (SHouse) CampaignMain.cm.getData().getHouseByName(this.getFounder());
        return originalHouse.getInfluenceForUnit(weightclass, typeid);
    }

    /**
     * The cost (PP) of a unit from this factory. Back referenced to the faction
     * that originally owned the world. Hacky. Ugly.
     */
    public int getPPCost(int weightclass, int typeid) {
        SHouse originalHouse = (SHouse) CampaignMain.cm.getData().getHouseByName(this.getFounder());
        return originalHouse.getPPCost(weightclass, typeid);
    }

    /**
     * This is used for tech raids. to increase other players build tables.
     * 
     * @param type_id
     * @return
     */
    public String getTechProduced(int type_id) {

        // Build the fluff text for the mek
        String Filename = "";

        String unitSize = getSize();
        if (CampaignMain.cm.getBooleanConfig("UseOnlyOneVehicleSize") && type_id == Unit.VEHICLE)
            unitSize = Unit.getWeightClassDesc(CampaignMain.cm.getRandomNumber(4));

        Filename = BuildTable.getUnitFilename(this.getFounder(), unitSize, type_id, getBuildTableFolder());

        return Filename;
    }
}
