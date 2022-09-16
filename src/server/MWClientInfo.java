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

package server;

//A Class Holding a Client Object might need some more information
//@Author Helge Richter (McWizard@gmx.de)
//@Version 0.1

import java.io.Serializable;
import java.net.InetAddress;



/*
 * TODO: As of 7.9.06, this class is referenced only by the server. Can
 *       probably be safely repackaged as a server.* class.
 */
public class MWClientInfo implements Serializable, Comparable<MWClientInfo> {
	
	/**
     * 
     */
    private static final long serialVersionUID = 831681934723434725L;
    //VARIABLES
	String name ="Nobody";//should be unique
	String color = "black";
	String country = "unknown";
	
	int level = 2;
	long Checktime;
	
	boolean isInvis = false;
	boolean latestVersion = true;
	
	InetAddress Adr;
	
	//CONSTRUCTORS
	public MWClientInfo() {
	}
	
	public MWClientInfo(String name, InetAddress Adr, long time, int level, boolean invis) {
		this.name = name;
		this.Adr = Adr;
		Checktime = time;
		this.level = level;
		this.isInvis = invis;
	}
	
	//METHODS
	public long getChecktime() {
		return Checktime;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setChecktime(long Checktime) {
		this.Checktime = Checktime;
	}
	
	public InetAddress getAdr() {
		return Adr;
	}
	
	public void setAdr(InetAddress Adr) {
		this.Adr = Adr;
	}
	
	@Override
	public String toString() {
		return name+"~"+color+"~"+country+"~"+level+"~"+isInvis;
	}
	
	public String getColor() {
		return color;
	}
	
	public void setColor(String c) {
		
		//check if its hex without the #
		if ( !c.startsWith("#") ){
			try{
                Integer.parseInt(c,16);
                c = "#"+c;
			}catch(Exception ex){}
		}
			
		color = c;
	}
	
	@Override
	public boolean equals(Object _mmci) {
		
		if (_mmci == null)
			return false;
		
		MWClientInfo mmci;
		try {
			mmci = (MWClientInfo)_mmci;
		} catch (ClassCastException e) {
			return false;
		}
		
		if ((mmci.getName().equals(getName())))
			return true;
		
		return false;
	}
	
	public String getCountry() {
		return country;
	}
	
	public void setCountry(String country) {
		this.country = country;
	}
	
	public int compareTo(MWClientInfo info) {
		return this.name.compareTo(info.getName());
	}
	
	public int getLevel() {
		return level;
	}
	
	public void setLevel(int level){
		this.level = level;
	}
	
	public boolean isLatestVersion() {
		return latestVersion;
	}
	
	public void setLatestVersion(boolean latestVersion) {
		this.latestVersion = latestVersion;
	}
	
	public boolean isInvis(){
		return this.isInvis;
	}
	
	public void setInvis(Boolean invis){
		this.isInvis = invis;
	}
	
}