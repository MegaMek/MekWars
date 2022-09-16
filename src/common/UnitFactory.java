/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author - Helge Richter (McWizard)
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
package common;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import common.campaign.operations.Operation;
import common.util.BinReader;
import common.util.BinWriter;

/**
 * @author Helge Richter
 *
 */

public class UnitFactory implements Serializable {
	
	/**
     * 
     */
    private static final long serialVersionUID = -5221016867627976085L;
    private String name;
	private String size;
	private String founder;

	private int ticksUntilRefresh;
	private int refreshSpeed = 100;//The Speed this factory refreshes
	
	static public final int BUILDALL = 0;
	static public final int BUILDMEK = 1;
	static public final int BUILDVEHICLES = 2;
	static public final int BUILDMEKnVEHICLES = 3; 
	static public final int BUILDINFANTRY = 4;
	static public final int BUILDMEKNInFANTRY = 5;
	static public final int BUILDVEHICLESnINFANTRY = 6;
	static public final int BUILDMEKnINFANTRYnVEHICLES = 7;
	static public final int BUILDPROTOMECHS = 8;
	static public final int BUILDMEKnPROTOMECHS = 9;
	static public final int BUILDVEHICLESnPROTOMECH = 10;
	static public final int BUILDMEKnVEHICLESnPROTOMECH = 11;
	static public final int BUILDINFANTRYnPROTOMECH = 12;
	static public final int BUILDMEKnINFANTRYnPROTOMECH = 13;
	static public final int BUILDVEHICLESnINFANTRYnPROTOMECH = 14;
	static public final int BUILDMEKnVEHICLESnINFANTRYnPROTOMECH = 15;
	static public final int BUILDBATTLEARMOR = 16;
	static public final int BUILDMEKnBATTLEARMOR = 17;
	static public final int BUILDVEHICLESnBATTLEARMOR = 18;
	static public final int BUILDMEKnVEHICLEsnBATTLEARMOR = 19;
	static public final int BUILDINFANTRYnBATTLEARMOR = 20;
	static public final int BUILDMEKnINFANTRYnBATTLEARMOR = 21;
	static public final int BUILDVEHICLESnINFANTRYnBATTLEARMOR = 22;
	static public final int BUILDMEKnVEHICLESnINFANTRYnBATTLEARMOR = 23;
	static public final int BUILDPROTOMECHSnBATTLEARMOR = 24;
	static public final int BUILDMEKnPROTOMECHSnBATTLEARMOR = 25;
	static public final int BUILDVEHICLESnPROTOMECHnBATTLEARMOR = 26;
	static public final int BUILDMEKnVEHICLESnPROTOMECHnBATTLEARMOR = 27;
	static public final int BUILDINFANTRYnPROTOMECHnBATTLEARMOR = 28;
	static public final int BUILDMEKnINFANTRYnPROTOMECHnBATTLEARMOR = 29;
	static public final int BUILDVEHICLESnINFANTRYnPROTOMECHnBATTLEARMOR = 30;
	static public final int BUILDMEKnVEHICLESnINFANTRYnPROTOMECHnBATTLEARMOR = 31;
	static public final int BUILDVTOL = 32;
	static public final int BUILDAERO = 33;
	
	
	/**
	 * Type = 0 means can produce everything
	 * Least significant bit = Mek
	 * Next Bit = Vehicle
	 * Next Bit = Infantry
	 */
	private int type;
	
	/**
	 * @author jtighe
	 * 
	 * This will allow admins to lock this factory
	 */
	private boolean factoryLocked = false;
	
	private String factoryID = "";
	//private int factoryID = 0;

	private int factoryAccessLevel = 0;
	private String buildTableFolder = "";

	/**
	 * @return Returns the faction .
	 */
	public String getFounder() {
		return founder;
	}
	/**
	 * @param faction The faction to set.
	 */
	public void setFounder(String faction) {
		this.founder = faction;
	}

	/**
	 * @return Returns the factoryID
	 */
	public String getID() {
	  return factoryID;
	}

	/**
	 * @param id The factoryID to set.
	 */
	public void setID(String id) {
	  this.factoryID = id;
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
	 * @return Returns the refreshSpeed.
	 */
	public int getRefreshSpeed() {
		return refreshSpeed;
	}
	
	/**
	 * @param refreshSpeed The refreshSpeed to set.
	 */
	public void setRefreshSpeed(int refreshSpeed) {
		this.refreshSpeed = refreshSpeed;
	}
	
	/**
	 * @return Returns the size.
	 */
	public String getSize() {
		return size;
	}
	
	/**
	 * @param size The size to set.
	 */
	public void setSize(String size) {
		this.size = size;
	}
	
	/**
	 * @return Returns the ticksUntilRefresh,
	 *         but hides any negative values.
	 */
	public int getTicksUntilRefresh() {
		if (isLocked())
			return Integer.MAX_VALUE;
		
		if (ticksUntilRefresh < 0)
			return 0;
		//else
		return ticksUntilRefresh;
	}
	
	/**
	 * @param ticksUntilRefresh The ticksUntilRefresh to set.
	 */
	public void setTicksUntilRefresh(int ticksUntilRefresh) {
		this.ticksUntilRefresh = ticksUntilRefresh;
	}
	
	/**
	 * @return Returns the type.
	 */
	public int getType() {
		return type;
	}
	
	/**
	 * @param type The type to set.
	 */
	public void setType(int type) {
	    
	    if ( type < BUILDALL || type > BUILDAERO )
	        this.type = BUILDMEK;
	    else
	        this.type = type;
	}
	
	/**
	 * Test whether the factory can produce an unit.
	 * @param type_id The type of the unit to test.
	 */
	public boolean canProduce(int type_id)
	{
		int test = getType();
		
		if ( test == BUILDALL )
			return true;
		
		if ( test - BUILDAERO >= 0 ){
		    test -= BUILDAERO;
		    if ( type_id == Unit.AERO ){
		        return true;
		    }
		}
		
		if ( test - BUILDBATTLEARMOR >= 0){
			
			test -= BUILDBATTLEARMOR;
			if (type_id == Unit.BATTLEARMOR)
				return true;
		}
		
		if ( test - BUILDPROTOMECHS >= 0 ){
			
			test -= BUILDPROTOMECHS;
			if (type_id == Unit.PROTOMEK)
				return true;
		}
		
		
		if (test - BUILDINFANTRY >= 0)
		{
			test -= BUILDINFANTRY;
			if (type_id == Unit.INFANTRY)
				return true;
		}
		
		if (test - BUILDVEHICLES >= 0)
		{
			test -= BUILDVEHICLES;
			if (type_id == Unit.VEHICLE)
				return true;
		}
		
		if (test - BUILDMEK >= 0)
		{
			if (type_id == Unit.MEK)
				return true;
		}
		
		return false;
	}
	
	/**
	 * See if this factory can be raided by the particular operation
	 * 
	 * 13 Sept 2011 - Cord Awtry
	 */
	public boolean canBeRaided(int type_id, Operation o) {
		boolean capMeks = o.getBooleanValue("ForceProduceAndCaptureMeks");
		boolean capVees = o.getBooleanValue("ForceProduceAndCaptureVees");
		boolean capInfs = o.getBooleanValue("ForceProduceAndCaptureInfs");
		boolean capProtos = o.getBooleanValue("ForceProduceAndCaptureProtos");
		boolean capBAs = o.getBooleanValue("ForceProduceAndCaptureBAs");
		boolean capAeros = o.getBooleanValue("ForceProduceAndCaptureAeros");
		
		boolean canRaidAnything = capMeks || capVees || capInfs || capProtos || capBAs || capAeros;

		if (!canRaidAnything) {
			return false;
		}
		
		if ( getType() == BUILDALL ) {
			return true;
		}
		
    	switch (type_id) {
		case Unit.MEK:
			return capMeks && canProduce(type_id);
		case Unit.VEHICLE:
			return capVees && canProduce(type_id);
		case Unit.INFANTRY:
			return capInfs && canProduce(type_id);
		case Unit.PROTOMEK:
			return capProtos && canProduce(type_id);
		case Unit.BATTLEARMOR:
			return capBAs && canProduce(type_id);
		case Unit.AERO:
			return capAeros && canProduce(type_id);
    	}
    	
		return false;
	}
	
	/**
	 * Writes as binary stream
	 */
	public void binOut(BinWriter out) {
		out.println(name, "name");
		out.println(size, "size");
		out.println(founder, "faction");
		out.println(ticksUntilRefresh, "ticksUntilRefresh");
		out.println(refreshSpeed, "refreshSpeed");
		out.println(type, "type");
		out.println(factoryLocked,"factorylock");
		out.println(factoryAccessLevel, "factoryaccess");
		out.println(buildTableFolder,"buildtablefolder");
		out.println(factoryID, "factoryID");
	}
	
	/**
	 * Read from a binary stream
	 */
	public void binIn(BinReader in) throws IOException {
		name = in.readLine("name");
		size = in.readLine("size");
		founder = in.readLine("faction");
		ticksUntilRefresh = in.readInt("ticksUntilRefresh");
		refreshSpeed = in.readInt("refreshSpeed");
		type = in.readInt("type");
		factoryLocked = in.readBoolean("factorylock");
		factoryAccessLevel = in.readInt("factoryaccess");
		buildTableFolder = in.readLine("buildtablefolder");
		factoryID = in.readLine("factoryID");
	}    
	
	public String getTypeString() {
		String result = "";
		if (this.canProduce(Unit.MEK))
			result += "M";
		if (this.canProduce(Unit.VEHICLE))
			result += "V";
		if (this.canProduce(Unit.INFANTRY))
			result += "I";
		if (this.canProduce(Unit.PROTOMEK))
			result += "P";
		if (this.canProduce(Unit.BATTLEARMOR))
			result += "B";
        if (this.canProduce(Unit.AERO))
            result += "A";
		
		return result;
	}
	
	//TODO: Fix the unit type system and all that stuff.. this is a big bunch of garbage..
	public String getFullTypeString() {
		String result = "";
		if (this.canProduce(Unit.MEK))
			result = "Mek ";
		if (this.canProduce(Unit.VEHICLE))
			result += "Vehicle ";
		if (this.canProduce(Unit.INFANTRY))
			result += "Infantry ";
		if (this.canProduce(Unit.PROTOMEK))
			result += "ProtoMek ";
		if (this.canProduce(Unit.BATTLEARMOR))
			result += "BattleArmor ";
        if (this.canProduce(Unit.AERO))
            result += "Aero ";
		return result;
	}
	
	/**
	 * @return the Status that is shown on a detailed planet view
	 */
	public String getStatus() {
		String result = getName() + "(" + getSize();
		if (getType() != Unit.MEK)
			result += " " + typeString();
		result += ") built by " + getFounder() + ".<br>";
		if (getTicksUntilRefresh() == 0)
			result += "Factory is ready to produce a unit.<br>";
		else
			result += "Factory will be ready to produce a unit in " + getTicksUntilRefresh() + " miniticks.<br>";
		return result;
	}
	
	/**
	 * Returns the name of all types this factory can produce seperated by space. 
	 */
	public String typeString() {
		String result = "";
		if (canProduce(Unit.MEK))
			result += "Mek ";
		if (canProduce(Unit.VEHICLE))
			result += "Vehicle ";
		if (canProduce(Unit.INFANTRY))
			result += "Infantry ";
		if (this.canProduce(Unit.PROTOMEK))
			result += "ProtoMek ";
		if (this.canProduce(Unit.BATTLEARMOR))
			result += "BattleArmor ";
        if (this.canProduce(Unit.AERO))
            result += "Aero ";
		return result;
	}
	
	/*public void binOut(TreeWriter out) {
		out.write(getName(), "name");
		out.write(getSize(), "size");
		out.write(getFounder(), "founder");
		out.write(getTicksUntilRefresh(), "ticksuntilrefresh");
		out.write(getRefreshSpeed(), "refreshspeed");
		out.write(getType(),"type");
		out.write(isLocked(), "factorylock");
	}*/
	
	/*for serializable
	public void binIn(TreeReader in, CampaignData data){
		//empty. todo.
	}
	*/
	public int getWeightclass() {
		if (getSize().equalsIgnoreCase("Light"))
			return Unit.LIGHT;
		else if (getSize().equalsIgnoreCase("Medium"))
			return Unit.MEDIUM;
		else if (getSize().equalsIgnoreCase("Heavy"))
			return Unit.HEAVY;
		else if (getSize().equalsIgnoreCase("Assault"))
			return Unit.ASSAULT;
		return 0;
	}
	
	public int getBestTypeProducable() {
		if (this.canProduce(Unit.MEK))
			return Unit.MEK;
		if (this.canProduce(Unit.VEHICLE))
			return Unit.VEHICLE;
        if (this.canProduce(Unit.AERO))
            return Unit.AERO;
		if (this.canProduce(Unit.BATTLEARMOR))
			return Unit.BATTLEARMOR;
		if (this.canProduce(Unit.PROTOMEK))
			return Unit.PROTOMEK;
		if (this.canProduce(Unit.INFANTRY))
			return Unit.INFANTRY;
		return Unit.MEK;
	}
	
	public boolean isLocked() {
		return factoryLocked;
	}
	
	public void setLock(boolean lock) {
		factoryLocked = lock;
	}
	
	public int getAccessLevel() {
		return factoryAccessLevel;
	}
	
	public void setAccessLevel(int access) {
		this.factoryAccessLevel = access;
	}

	public void setBuildTableFolder(String folder){
		
		if ( folder.equals("0") || folder.equals("standard") )
			return;
		
		buildTableFolder = folder.replaceAll("standard"+"\\"+File.separatorChar, "");
		
		if ( buildTableFolder.equals("standard") )
			buildTableFolder = "";
	}

	public String getBuildTableFolder(){
		
		if ( buildTableFolder.trim().length() < 1)
			return "standard";
			
		return "standard"+File.separatorChar+buildTableFolder.trim();
	}
}
