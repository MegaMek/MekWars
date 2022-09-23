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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;

import common.util.MWLogger;
import gd.xml.ParseException;
import gd.xml.XMLParser;
import gd.xml.XMLResponder;
import server.campaign.NewbieHouse;
import server.campaign.SHouse;
import server.campaign.mercenaries.MercHouse;


public class XMLFactionDataParser implements XMLResponder {
	
	String lastElement = "";
	String lastInfFaction = "";
	String Name = null;
	String Color = "#00FF00";
	String Abbreviation = null;
	String Logo = null;
	String houseColor = "#000000";
	
	int Money = 0;
	int BaseGunner=4;
	int BasePilot=5;
	int idcounter = 0;
	
	boolean isMercenary = false;
	boolean canConquer = true;
	boolean inHouseAttacks = false;
	boolean canDefectFrom = true;
	boolean canDefectTo = true;
	boolean isNewbie = false;
	
	Vector<SHouse> Factions = new Vector<SHouse>(1,1);
	
	private String Filename;
	private String prefix;
	
	
	public XMLFactionDataParser(String Filename) {
		
		this.Filename = Filename;
		try {
			XMLParser xp = new XMLParser();
			xp.parseXML(this);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
	}
	
	
	/*  public void characters(char characters[],int start, int length)
	 {
	 String charData = (new String(characters,start,length)).trim();
	 if (!charData.equalsIgnoreCase(""))
	 MWLogger.mainLog(lastElement + " --> " + charData);
	 else
	 lastElement = "";
	 if (lastElement.equalsIgnoreCase("NAME"))
	 name = charData;
	 else if (lastElement.equalsIgnoreCase("MONEY"))
	 Money = Integer.parseInt(charData);
	 else if (lastElement.equalsIgnoreCase("COLOR"))
	 Color = charData;
	 else if (lastElement.equalsIgnoreCase("ABBREVIATION"))
	 Abbreviation = charData;
	 else if (lastElement.equalsIgnoreCase("LOGO"))
	 Logo = charData;
	 else if (lastElement.equalsIgnoreCase("ISMERCENARY"))
	 isMercenary = Boolean.parseBoolean(charData).booleanValue();
	 }*/
	
	public void endElement(String uri, String localName,String rawName)
	{
		if (rawName.equalsIgnoreCase("FACTION"))
		{
			/*
			 MWLogger.mainLog("FACTION READ");
			 SHouse h;
			 if (this.isMercenary)
			 h = new MercHouse(name,Color,BaseGunner,BasePilot,Abbreviation,myCampaign.getR(),myCampaign);
			 else
			 h = new SHouse(name,Color,BaseGunner,BasePilot,Abbreviation,myCampaign.getR(),myCampaign);
			 if (Logo != null)
			 h.setLogo(Logo);
			 h.setInHouseAttacks(inHouseAttacks);
			 h.setConquerable(canConquer);
			 //RESET VARIABLES
			  this.name = null;
			  this.Money = 0;
			  this.Color = "#00FF00";
			  this.Abbreviation = null;
			  this.isMercenary = false;
			  this.Logo = null;
			  this.BaseGunner=4;
			  this.BasePilot=5;
			  this.inHouseAttacks = false;
			  this.canConquer = true;
			  Factions.add(h);*/
		}
	}
	
	public Vector<SHouse> getFactions() {
		return Factions;
	}
	
	/* DTD METHODS */
	
	public void recordNotationDeclaration(String name, String pubID, String sysID) throws ParseException {
		System.out.print(prefix+"!NOTATION: "+name);
		if (pubID!=null) System.out.print("  pubID = "+pubID);
		if (sysID!=null) System.out.print("  sysID = "+sysID);
		MWLogger.mainLog("");
	}
	
	public void recordEntityDeclaration(String name, String value, String pubID, String sysID, String notation) throws ParseException {
		System.out.print(prefix+"!ENTITY: "+name);
		if (value!=null) System.out.print("  value = "+value);
		if (pubID!=null) System.out.print("  pubID = "+pubID);
		if (sysID!=null) System.out.print("  sysID = "+sysID);
		if (notation!=null) System.out.print("  notation = "+notation);
		MWLogger.mainLog("");
	}
	
	public void recordElementDeclaration(String name, String content) throws ParseException {
		System.out.print(prefix+"!ELEMENT: "+name);
		MWLogger.mainLog("  content = "+content);
	}
	
	public void recordAttlistDeclaration(String element, String attr, boolean notation, String type, String defmod, String def) throws ParseException {
		System.out.print(prefix+"!ATTLIST: "+element);
		System.out.print("  attr = "+attr);
		System.out.print("  type = " + ((notation) ? "NOTATIONS " : "") + type);
		System.out.print("  def. modifier = "+defmod);
		MWLogger.mainLog( (def==null) ? "" : "  def = "+notation);
	}
	
	public void recordDoctypeDeclaration(String name, String pubID, String sysID) throws ParseException {
		System.out.print(prefix+"!DOCTYPE: "+name);
		if (pubID!=null) System.out.print("  pubID = "+pubID);
		if (sysID!=null) System.out.print("  sysID = "+sysID);
		MWLogger.mainLog("");
		prefix = "";
	}
	
	
	
	/* DOC METHDODS */
	
	public void recordDocStart() {
	}
	
	public void recordDocEnd() {
		MWLogger.mainLog("");
		MWLogger.mainLog("Faction Parsing finished without error");
	}
	
	public void recordElementStart(String name, Hashtable attr) throws ParseException {
		MWLogger.mainLog(prefix+"Element: "+name);
		lastElement = name;
		/*        if (attr!=null) {
		 Enumeration e = attr.keys();
		 System.out.print(prefix);
		 String conj = "";
		 while (e.hasMoreElements()) {
		 Object k = e.nextElement();
		 System.out.print(conj+k+" = "+attr.get(k));
		 conj = ", ";
		 }
		 MWLogger.mainLog("");
		 }
		 prefix = prefix+"  ";*/
	}
	
	public void recordElementEnd(String name) throws ParseException {
		if (name.equalsIgnoreCase("FACTION"))
		{
			MWLogger.mainLog("FACTION READ");
			SHouse h;
			// search for an unused ID
			idcounter++;
			if (this.isMercenary)
				h = new MercHouse(idcounter, Name,Color,BaseGunner,BasePilot,Abbreviation);
			else if ( this.isNewbie )
				h = new NewbieHouse(idcounter, Name,Color,BaseGunner,BasePilot,Abbreviation);
			else
				h = new SHouse(idcounter, Name,Color,BaseGunner,BasePilot,Abbreviation);
			
			if (Logo != null)
				h.setLogo(Logo);
			h.setInHouseAttacks(inHouseAttacks);
			h.setConquerable(this.canConquer);
			h.setHousePlayerColors(houseColor);
			//RESET VARIABLES
			this.Name = null;
			this.Money = 0;
			this.Color = "#00FF00";
			this.isMercenary = false;
			this.isNewbie = false;
			this.Abbreviation = null;
			this.Logo = null;
			this.BaseGunner=4;
			this.BasePilot=5;
			this.canConquer = true;
			this.inHouseAttacks = false;
			this.canDefectFrom = true;
			this.canDefectTo = true;
			this.houseColor = "#000000";

			Factions.add(h);
		}
	}
	
	public void recordPI(String name, String pValue) {
		MWLogger.mainLog(prefix+"*"+name+" PI: "+pValue);
	}
	
	public void recordCharData(String charData) {
		MWLogger.mainLog(prefix+charData);
		if (!charData.equalsIgnoreCase(""))
			MWLogger.mainLog(lastElement + " --> " + charData);
		else
			lastElement = "";
		if (lastElement.equalsIgnoreCase("NAME"))
			Name = charData;
		else if (lastElement.equalsIgnoreCase("MONEY"))
			Money = Integer.parseInt(charData);
		else if (lastElement.equalsIgnoreCase("COLOR"))
			Color = charData;
		else if (lastElement.equalsIgnoreCase("ABBREVIATION"))
			Abbreviation = charData;
		else if (lastElement.equalsIgnoreCase("LOGO"))
			Logo = charData;
		else if (lastElement.equalsIgnoreCase("BASEGUNNER"))
			BaseGunner = Integer.parseInt(charData);
		else if (lastElement.equalsIgnoreCase("BASEPILOT"))
			BasePilot = Integer.parseInt(charData);
		else if (lastElement.equalsIgnoreCase("ISMERCENARY"))
			isMercenary = Boolean.parseBoolean(charData);
		else if (lastElement.equalsIgnoreCase("CONQUERABLE"))
			canConquer = Boolean.parseBoolean(charData);
		else if (lastElement.equalsIgnoreCase("INHOUSEATTACKS"))
			inHouseAttacks = Boolean.parseBoolean(charData);
		else if (lastElement.equalsIgnoreCase("HOUSEPLAYERCOLOR"))
			houseColor = charData;
		else if (lastElement.equalsIgnoreCase("DEFECTTO"))
			canDefectTo = Boolean.parseBoolean(charData);
		else if (lastElement.equalsIgnoreCase("DEFECTFROM"))
			canDefectFrom = Boolean.parseBoolean(charData);
		else if (lastElement.equalsIgnoreCase("ISNEWBIE"))
			isNewbie = Boolean.parseBoolean(charData);
	}
	
	public void recordComment(String comment) {
		MWLogger.mainLog(prefix+"*Comment: "+comment);
	}
	
	
	
	/* INPUT METHODS */
	
	public InputStream getDocumentStream() throws ParseException {
		try { return new FileInputStream(Filename); }
		catch (FileNotFoundException e) { throw new ParseException("could not find the specified file"); }
	}
	
	public InputStream resolveExternalEntity(String name, String pubID, String sysID) throws ParseException {
		if (sysID!=null) {
			File f = new File((new File(Filename)).getParent(), sysID);
			try { return new FileInputStream(f); }
			catch (FileNotFoundException e) { throw new ParseException("file not found ("+f+")"); }
		}
		//else
		return null;
	}
	
	public InputStream resolveDTDEntity(String name, String pubID, String sysID) throws ParseException {
		return resolveExternalEntity(name, pubID, sysID);
	}
	
	
	
}