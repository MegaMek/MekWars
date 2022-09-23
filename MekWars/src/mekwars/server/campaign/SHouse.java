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

package server.campaign;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import org.mekwars.libpk.logging.PKLogManager;

import common.BMEquipment;
import common.Planet;
import common.SubFaction;
import common.Unit;
import common.util.ComponentToCritsConverter;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.TokenReader;
import common.util.UnitComponents;
import common.util.UnitUtils;
import megamek.common.Entity;
import megamek.common.TechConstants;
import server.campaign.commands.Command;
import server.campaign.data.TimeUpdateHouse;
import server.campaign.market2.IBuyer;
import server.campaign.market2.ISeller;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;
import server.campaign.pilot.SPilot;
import server.campaign.util.SerializedMessage;

/**
 * A class holding a server-side representation of a House
 * @author Helge Richter (McWizard)
 * @author Torren
 * @author Bob Eldred (Spork)
 * @version 2016.10.06
 * 
 * Modifications:
 * - Changed addActivityPP to just keep track of PP, which is then used at the tick.
 *   Looping through the planets was taking way too long.
 * - Moved component prodution to addActivityPP to enable access from a Quartz task
 * 
 */
public class SHouse extends TimeUpdateHouse implements Comparable<Object>, ISeller, IBuyer, Serializable {

    private static final long serialVersionUID = -1558672678021355218L;
    // store all online players in *THREE* hashes, one for each primary status
    private ConcurrentHashMap<String, SPlayer> reservePlayers = new ConcurrentHashMap<String, SPlayer>();
    private ConcurrentHashMap<String, SPlayer> activePlayers = new ConcurrentHashMap<String, SPlayer>();
    private ConcurrentHashMap<String, SPlayer> fightingPlayers = new ConcurrentHashMap<String, SPlayer>();

    private ConcurrentHashMap<String, SPlanet> Planets = new ConcurrentHashMap<String, SPlanet>();
    private ConcurrentHashMap<Integer, Vector<Vector<SUnit>>> Hangar = new ConcurrentHashMap<Integer, Vector<Vector<SUnit>>>();

    private Hashtable<String, SmallPlayer> SmallPlayers = new Hashtable<String, SmallPlayer>();
    private Hashtable<Integer, Vector<Integer>> Components = new Hashtable<Integer, Vector<Integer>>();
    private Hashtable<Integer, Integer> unitComponents = new Hashtable<Integer, Integer>();

    private int Money;
    private int BaysProvided = 0;
    private int ComponentProduction = 0;
    private int showProductionCountNext = 0;
    private int initialHouseRanking = 0;

    private String motd = "";
    private String announcement = "";
    
    private PilotQueues pilotQueues = new PilotQueues(getBaseGunnerVect(), getBasePilotVect(), getBasePilotSkillVect());

    private boolean inHouseAttacks = false;
    private Properties config = new Properties();

    private Vector<String> leaders = new Vector<String>(1, 1);
    private int techResearchPoints = 0;
    private UnitComponents unitParts = new UnitComponents();
    private Hashtable<String, ComponentToCritsConverter> componentConverter = new Hashtable<String, ComponentToCritsConverter>();

    private int[][] unitLimits = new int[6][4];
    private boolean[][] bmLimits = new boolean[6][4];
    
    private double activityPP = 0.0;
    
    @Override
    public String toString() {
        SerializedMessage result = new SerializedMessage("|");
        // result.append("HS�");
        result.append(getName());
        result.append(getMoney());
        result.append(getHouseColor());
        result.append(this.getBaseGunner());
        result.append(getBasePilot());
        result.append(getAbbreviation());

        // Store the Meks
        for (int i = 0; i < 4; i++) {
            Vector<SUnit> tmpVec = getHangar(Unit.MEK).elementAt(i);

            tmpVec.trimToSize();
            result.append(tmpVec.size());

            for (SUnit currU : tmpVec) {
                result.append(currU.toString(false));
            }
        }

        // Store the Vehicles
        for (int i = 0; i < 4; i++) {
            Vector<SUnit> tmpVec = getHangar(Unit.VEHICLE).elementAt(i);

            tmpVec.trimToSize();
            result.append(tmpVec.size());

            for (SUnit currU : tmpVec) {
                result.append(currU.toString(false));
            }
        }

        // Store the Infantry
        if (Boolean.parseBoolean(this.getConfig("UseInfantry"))) {

            for (int i = 0; i < 4; i++) {
                Vector<SUnit> tmpVec = getHangar(Unit.INFANTRY).elementAt(i);

                tmpVec.trimToSize();
                result.append(tmpVec.size());

                for (SUnit currU : tmpVec) {
                    result.append(currU.toString(false));
                }
            }
        }

        result.append(getLogo());

        if (getAnnouncement().equals("")) {
        	result.append(" ");
        } else {
        	result.append(stripReturns(getAnnouncement()));
        }

        // Write the Components / BuildingPP's
        result.append("Components");
        Enumeration<Integer> e = getComponents().keys();
        while (e.hasMoreElements()) {
            Integer id = e.nextElement();
            Vector<Integer> v = getComponents().get(id);
            result.append(id.intValue());
            result.append(v.size());
            for (int i = 0; i < v.size(); i++) {
                result.append(v.elementAt(i).intValue());
            }
        }
        result.append("EndComponents");

        result.append(getInitialHouseRanking());
        result.append(isConquerable());
        result.append(isInHouseAttacks());
        result.append(getId());
        result.append(getHousePlayerColor());
        result.append(getHouseDefectionFrom());
        result.append(getPilotQueues().getQueueSize(Unit.MEK));// Mek pilots first
        LinkedList<SPilot> PilotList = getPilotQueues().getPilotQueue(Unit.MEK);
        for (SPilot currP : PilotList) {
            result.append(currP.toFileFormat("#", false));
        }// veehs next
        result.append(getPilotQueues().getQueueSize(Unit.VEHICLE));
        PilotList = getPilotQueues().getPilotQueue(Unit.VEHICLE);
        for (SPilot currP : PilotList) {
            result.append(currP.toFileFormat("#", false));
        }// inf
        result.append(getPilotQueues().getQueueSize(Unit.INFANTRY));
        PilotList = getPilotQueues().getPilotQueue(Unit.INFANTRY);
        for (SPilot currP : PilotList) {
            result.append(currP.toFileFormat("#", false));
        }

        result.append(getHouseFluFile());

        // Store the BattleArmor (Units)
        for (int i = 0; i < 4; i++) {
            Vector<SUnit> tmpVec = getHangar(Unit.BATTLEARMOR).elementAt(i);

            tmpVec.trimToSize();
            result.append(tmpVec.size());

            for (SUnit currU : tmpVec) {
                result.append(currU.toString(false));
            }
        }

        // Store the ProtoMeks (Units)
        for (int i = 0; i < 4; i++) {
            Vector<SUnit> tmpVec = getHangar(Unit.PROTOMEK).elementAt(i);

            tmpVec.trimToSize();
            result.append(tmpVec.size());
            for (SUnit currU : tmpVec) {
                result.append(currU.toString(false));
            }
        }

        // Store BattleArmor (Pilots)
        result.append(getPilotQueues().getQueueSize(Unit.BATTLEARMOR));
        PilotList = getPilotQueues().getPilotQueue(Unit.BATTLEARMOR);
        for (SPilot currPilot : PilotList) {
            result.append(currPilot.toFileFormat("#", false));
        }

        // Store ProtoMeks (Pilots)
        result.append(getPilotQueues().getQueueSize(Unit.PROTOMEK));
        PilotList = getPilotQueues().getPilotQueue(Unit.PROTOMEK);
        for (SPilot currPilot : PilotList) {
            result.append(currPilot.toFileFormat("#", false));
        }

        // Save faction MOTD
        if (getMotd().equals("")) {
            result.append(" ");
        } else {
            result.append(stripReturns(getMotd()));
        }

        
        result.append(getHouseDefectionTo());

        for (int pos = 0; pos < Unit.MAXBUILD; pos++) {
            result.append(getBaseGunner(pos));
            result.append(getBasePilot(pos));
        }

        for (int pos = 0; pos < Unit.MAXBUILD; pos++) {
            String skill = getBasePilotSkill(pos);
            if (skill.length() < 1) {
                result.append(" ");
            } else {
                result.append(skill);
            }
        }

        result.append(getTechLevel());

        result.append(getSubFactionList().size());

        for (String key : getSubFactionList().keySet()) {
            result.append(getSubFactionList().get(key).toString());
        }

        result.append(leaders.size());
        for (String leader : leaders) {
            result.append(leader);
        }
        result.append(techResearchPoints);
        result.append(unitParts.toString("#"));
        result.append(componentConverter.size());

        for (String key : componentConverter.keySet()) {
            result.append(componentConverter.get(key).toString());
        }

        // Store the Aero (Units)
        for (int i = 0; i < 4; i++) {
            Vector<SUnit> tmpVec = getHangar(Unit.AERO).elementAt(i);

            tmpVec.trimToSize();
            result.append(tmpVec.size());
 
            for (SUnit currU : tmpVec) {
                result.append(currU.toString(false));
            }
        }

        // Store Aero (Pilots)
        result.append(getPilotQueues().getQueueSize(Unit.AERO));
        PilotList = getPilotQueues().getPilotQueue(Unit.AERO);
        for (SPilot currPilot : PilotList) {
            result.append(currPilot.toFileFormat("#", false));
        }
     
        return result.toString();
    }

    
    /**
     * Carriage returns in the MOTD causing problems in house saves.
     * @param motd
     * @return sanitized String
     */
    private String stripReturns(String motd) {
		return motd.replaceAll("[\\r\\n]", "");
	}

	public Hashtable<Integer, Vector<Integer>> getComponents() {
        return Components;
    }

    public String fromString(String s, Random r) {
        try {

            StringTokenizer ST = new StringTokenizer(s, "|");
            setName(TokenReader.readString(ST));

            // start the chat logging.
            PKLogManager.getInstance().addLog(getName());

            setMoney(TokenReader.readInt(ST));
            setHouseColor(TokenReader.readString(ST));
            setBaseGunner(TokenReader.readInt(ST));
            setBasePilot(TokenReader.readInt(ST));

            setAbbreviation(TokenReader.readString(ST));

            getHangar().put(Unit.MEK, new Vector<Vector<SUnit>>(5, 1));
            getHangar().put(Unit.VEHICLE, new Vector<Vector<SUnit>>(5, 1));
            getHangar().put(Unit.INFANTRY, new Vector<Vector<SUnit>>(5, 1));
            getHangar().put(Unit.PROTOMEK, new Vector<Vector<SUnit>>(5, 1));
            getHangar().put(Unit.BATTLEARMOR, new Vector<Vector<SUnit>>(5, 1));
            getHangar().put(Unit.AERO, new Vector<Vector<SUnit>>(5, 1));
            // Init all of the hangars
            for (int i = 0; i < 4; i++) {

                getHangar(Unit.MEK).add(new Vector<SUnit>(1, 1));
                getHangar(Unit.VEHICLE).add(new Vector<SUnit>(1, 1));
                getHangar(Unit.INFANTRY).add(new Vector<SUnit>(1, 1));
                getHangar(Unit.BATTLEARMOR).add(new Vector<SUnit>(1, 1));
                getHangar(Unit.PROTOMEK).add(new Vector<SUnit>(1, 1));
                getHangar(Unit.AERO).add(new Vector<SUnit>(1, 1));
            }

            boolean newbieHouse = isNewbieHouse();

            // READ THE MEKS
            for (int i = 0; i < 4; i++) {
                // Vector v = new Vector();
                int numofmechs = (TokenReader.readInt(ST));
                SUnit m = null;
                for (int j = 0; j < numofmechs; j++) {
                    m = new SUnit();
                    m.fromString(TokenReader.readString(ST));

                    if (newbieHouse) {
                        int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                        int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                        CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                        m.setStatus(Unit.STATUS_FORSALE);
                    }
                    addUnit(m, false);

                }
            }

            // READ THE VEHICLES
            for (int i = 0; i < 4; i++) {
                // Vector v = new Vector();
                int numofvehicles = (TokenReader.readInt(ST));
                SUnit m;
                for (int j = 0; j < numofvehicles; j++) {
                    m = new SUnit();
                    m.fromString(TokenReader.readString(ST));

                    if (newbieHouse) {
                        int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                        int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                        CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                        m.setStatus(Unit.STATUS_FORSALE);
                    }
                    addUnit(m, false);

                }
            }

            // READ THE INFANTRY
            if (Boolean.parseBoolean(this.getConfig("UseInfantry"))) {
                for (int i = 0; i < 4; i++) {
                    int numofinfantry = (TokenReader.readInt(ST));
                    SUnit m;
                    for (int j = 0; j < numofinfantry; j++) {
                        m = new SUnit();
                        m.fromString(TokenReader.readString(ST));

                        if (newbieHouse) {
                            int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                            int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                            CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                            m.setStatus(Unit.STATUS_FORSALE);
                        }
                        addUnit(m, false);

                    }// end for(num infantry)
                }// end for(4 weight classes)
            }// end if("Use Infantry")

            setAnnouncement(TokenReader.readString(ST));

            /*
             * Another bad-old-code feature. "Components" will be the next token
             * on any modern server. Loop remains in case someone tries to use
             * old MMNET data with players saved in-line.
             */
            String next = TokenReader.readString(ST);
            while (!next.equals("Components")) {
                next = TokenReader.readString(ST);
            }

            // init the componet array(vectors)
            getComponents().put(Unit.MEK, new Vector<Integer>(4, 1));
            getComponents().put(Unit.VEHICLE, new Vector<Integer>(4, 1));
            getComponents().put(Unit.INFANTRY, new Vector<Integer>(4, 1));
            getComponents().put(Unit.BATTLEARMOR, new Vector<Integer>(4, 1));
            getComponents().put(Unit.PROTOMEK, new Vector<Integer>(4, 1));
            getComponents().put(Unit.AERO, new Vector<Integer>(4, 1));

            for (int i = 0; i < 4; i++) {
                getComponents().get(Unit.MEK).add(0);
                getComponents().get(Unit.VEHICLE).add(0);
                getComponents().get(Unit.INFANTRY).add(0);
                getComponents().get(Unit.BATTLEARMOR).add(0);
                getComponents().get(Unit.PROTOMEK).add(0);
                getComponents().get(Unit.AERO).add(0);
            }

            boolean finished = false;
            while (!finished) {
                next = TokenReader.readString(ST);
                if (!next.equals("EndComponents")) {
                    Integer id = Integer.parseInt(next);
                    int count = TokenReader.readInt(ST);
                    for (int i = 0; i < count; i++) {
                        Vector<Integer> v = getComponents().get(id);
                        int val = TokenReader.readInt(ST);
                        v.setElementAt(val, i);
                    }
                    // getComponents().put(id,v);
                } else {
                    finished = true;
                }
            }

            setInitialHouseRanking(TokenReader.readInt(ST));

            setConquerable(TokenReader.readBoolean(ST));

            setInHouseAttacks(TokenReader.readBoolean(ST));
            // Used to read the house id here but if you have to recreate a
            // house from
            // Pfiles this could cause issues. now we just set the ID to -1
            // and let the server pick an id. --Torren

            TokenReader.readString(ST);
            setId(-1);
            String housePlayerColor = TokenReader.readString(ST);
            try {
                int redColor = Integer.parseInt(housePlayerColor);
                int greenColor = TokenReader.readInt(ST);
                int blueColor = TokenReader.readInt(ST);

                setHousePlayerColors(Integer.toHexString(redColor) + Integer.toHexString(greenColor) + Integer.toHexString(blueColor));
            } catch (Exception ex) {
                setHousePlayerColors(housePlayerColor);
            }

            setHouseDefectionFrom(TokenReader.readBoolean(ST));

            // meks
            int pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.MEK, p);
            }

            // vees
            pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.VEHICLE, p);
            }

            // inf
            pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.INFANTRY, p);
            }

            setHouseFluFile(TokenReader.readString(ST));

            // READ THE BattleArmor

            for (int i = 0; i < 4; i++) {
                int numofmechs = TokenReader.readInt(ST);
                SUnit m = null;
                for (int j = 0; j < numofmechs; j++) {
                    m = new SUnit();
                    m.fromString(TokenReader.readString(ST));

                    if (newbieHouse) {
                        int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                        int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                        CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                        m.setStatus(Unit.STATUS_FORSALE);
                    }
                    addUnit(m, false);

                }
            }

            // READ THE Protos

            for (int i = 0; i < 4; i++) {
                int numofmechs = TokenReader.readInt(ST);
                SUnit m = null;
                for (int j = 0; j < numofmechs; j++) {
                    m = new SUnit();
                    m.fromString(TokenReader.readString(ST));

                    if (newbieHouse) {
                        int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                        int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                        CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                        m.setStatus(Unit.STATUS_FORSALE);
                    }
                    addUnit(m, false);

                }
            }

            // BattleArmor
            pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.BATTLEARMOR, p);
            }

            // ProtoMeks
            pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.PROTOMEK, p);
            }

            setMotd(TokenReader.readString(ST));
            
            setHouseDefectionTo(TokenReader.readBoolean(ST));

            try {
                for (int pos = 0; pos < Unit.MAXBUILD; pos++) {
                    setBaseGunner(TokenReader.readInt(ST), pos);
                    setBasePilot(TokenReader.readInt(ST), pos);
                }
            } catch (Exception ex) {
                setPilotQueues(new PilotQueues(getBaseGunnerVect(), getBasePilotVect(), getBasePilotSkillVect()));
                getPilotQueues().setFactionString(getName());// set the
                // faction
                // name for
                // the queue
            }

            try {
                for (int pos = 0; pos < Unit.MAXBUILD; pos++) {
                    String skill = TokenReader.readString(ST);
                    setBasePilotSkill(skill, pos);
                }
            } catch (Exception ex) {
                setPilotQueues(new PilotQueues(getBaseGunnerVect(), getBasePilotVect(), getBasePilotSkillVect()));
                getPilotQueues().setFactionString(getName());// set the
                // faction
                // name for
                // the queue
            }

            setTechLevel(TokenReader.readInt(ST));

            int amount = TokenReader.readInt(ST);

            for (; amount > 0; amount--) {
                SubFaction newSubFaction = new SubFaction();
                newSubFaction.fromString(TokenReader.readString(ST));
                getSubFactionList().put(newSubFaction.getConfig("Name"), newSubFaction);
            }

            amount = TokenReader.readInt(ST);
            for (; amount > 0; amount--) {
                leaders.add(TokenReader.readString(ST));
            }

            techResearchPoints = TokenReader.readInt(ST);

            if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
                unitParts.fromString(TokenReader.readString(ST), "#");
            } else {
                TokenReader.readString(ST);
            }

            int size = TokenReader.readInt(ST);
            for (; size > 0; size--) {
                ComponentToCritsConverter converter = new ComponentToCritsConverter();
                converter.setCritName(TokenReader.readString(ST));
                converter.setMinCritLevel(TokenReader.readInt(ST));
                converter.setComponentUsedType(TokenReader.readInt(ST));
                converter.setComponentUsedWeight(TokenReader.readInt(ST));
            }

            // READ THE Aero units on the BM

            for (int i = 0; i < 4; i++) {
                int numofmechs = TokenReader.readInt(ST);
                SUnit m = null;
                for (int j = 0; j < numofmechs; j++) {
                    m = new SUnit();
                    m.fromString(TokenReader.readString(ST));

                    if (newbieHouse) {
                        int priceForUnit = getPriceForUnit(m.getWeightclass(), m.getType());
                        int rareSalesTime = Integer.parseInt(this.getConfig("RareMinSaleTime"));
                        CampaignMain.cm.getMarket().addListing("Faction_" + getName(), m, priceForUnit, rareSalesTime);
                        m.setStatus(Unit.STATUS_FORSALE);
                    }
                    addUnit(m, false);

                }
            }

            // Aero's
            pilotCount = TokenReader.readInt(ST);
            for (; pilotCount > 0; pilotCount--) {
                SPilot p = new SPilot();
                p.fromFileFormat(TokenReader.readString(ST), "#");
                getPilotQueues().loadPilot(Unit.AERO, p);
            }

            if (getComponentConverter().size() < 1 && CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
                ComponentToCritsConverter converter = new ComponentToCritsConverter();
                converter.setComponentUsedType(SUnit.MEK);
                converter.setComponentUsedWeight(SUnit.LIGHT);
                converter.setMinCritLevel(100);
                getComponentConverter().put(converter.getCritName(), converter);
            }

            setPilotQueues(new PilotQueues(getBaseGunnerVect(), getBasePilotVect(), getBasePilotSkillVect()));
            getPilotQueues().setFactionString(getName());// set the
            // faction name
            // for the queue

            // Stuff for MercHouse.. Has to be here until someone tells me how
            // to move it :) - McWiz
            if (isMercHouse()) {
                MWLogger.mainLog("Merc House");
                int contractamount = 0;

                contractamount = TokenReader.readInt(ST);
                Hashtable<String, ContractInfo> merctable = new Hashtable<String, ContractInfo>();
                for (int i = 0; i < contractamount; i++) {
                    ContractInfo ci = new ContractInfo();
                    ci.fromString(TokenReader.readString(ST));
                    merctable.put(ci.getPlayerName(), ci);
                }
                ((MercHouse) this).setOutstandingContracts(merctable);
            }
            
            // if (CampaignMain.cm.isDebugEnabled())
            MWLogger.mainLog("House loaded: " + getName());

            /*
             * this.getPilotQueues().setBaseGunnery(this.getBaseGunner());
             * this.getPilotQueues().setBasePiloting(this.getBasePilot());
             */

            loadConfigFile();
            setUsedMekBayMultiplier(Float.parseFloat(getConfig("UsedPurchaseCostMulti")));

            return s;
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Error while loading faction: " + getName() + " Going forward anyway ...");
            return s;
        }
    }

    public SHouse(int id) {
        super(id);
    }

    /**
     * Constructor used for serialization
     */
    public SHouse() {
        reservePlayers = new ConcurrentHashMap<String, SPlayer>();
        activePlayers = new ConcurrentHashMap<String, SPlayer>();
        fightingPlayers = new ConcurrentHashMap<String, SPlayer>();
        SmallPlayers = new Hashtable<String, SmallPlayer>();
        for (int pos = 0; pos < Unit.MAXBUILD; pos++) {
            setBaseGunner(4, pos);
            setBasePilot(5, pos);
        }

    }

    /*
     * Players are stores in 3 seperate hashtables. Each hash is indicative of a
     * different activity level. As players move back and forth between these
     * levels, they are transferred from hash to hash. At NO TIME should a
     * player exist in multiple hashes.
     * 
     * This 3-hash system replaces the old fighting/logged in 2 hash system and
     * the SPlayer's activity boolean.
     * 
     * TODO: massively improve commenting here. @urgru 1.14.06
     */
    public ConcurrentHashMap<String, SPlayer> getReservePlayers() {
        return reservePlayers;
    }

    public ConcurrentHashMap<String, SPlayer> getActivePlayers() {
        return activePlayers;
    }

    public ConcurrentHashMap<String, SPlayer> getFightingPlayers() {
        return fightingPlayers;
    }

    public int getBaysProvided() {
        return BaysProvided;
    }

    public int getComponentProduction() {
        return ComponentProduction;
    }

    public void setPilotQueues(PilotQueues q) {
        pilotQueues = q;
    }

    public SHouse(int id, String name, String HouseColor, int BaseGunner, int BasePilot, String abbreviation) {
        super(id);
        setAbbreviation(abbreviation);
        setHouseColor(HouseColor);
        setName(name);

        PKLogManager.getInstance().addLog(getName());
        // Vehicles = new Vector();

        for (int j = 0; j < 5; j++) // Type
        {
            Vector<Integer> v = new Vector<Integer>();
            for (int i = 0; i < 4; i++) // Weight
            {
                v.add(0);
            }
            v.trimToSize();
            getComponents().put(j, v);
        }
        // currentPP = new Vector();
        setMoney(0);
        getHangar().put(Unit.MEK, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.VEHICLE, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.INFANTRY, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.PROTOMEK, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.BATTLEARMOR, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.AERO, new Vector<Vector<SUnit>>(1, 1));
        for (int i = 0; i < 4; i++) {
            getHangar(Unit.MEK).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.VEHICLE).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.INFANTRY).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.PROTOMEK).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.BATTLEARMOR).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.AERO).add(new Vector<SUnit>(1, 1));
        }

        // init the componet array(vectors)
        getComponents().put(Unit.MEK, new Vector<Integer>(4, 1));
        getComponents().put(Unit.VEHICLE, new Vector<Integer>(4, 1));
        getComponents().put(Unit.INFANTRY, new Vector<Integer>(4, 1));
        getComponents().put(Unit.BATTLEARMOR, new Vector<Integer>(4, 1));
        getComponents().put(Unit.AERO, new Vector<Integer>(4, 1));
        getComponents().put(Unit.PROTOMEK, new Vector<Integer>(4, 1));

        for (int i = 0; i < 4; i++) {
            getComponents().get(Unit.MEK).add(0);
            getComponents().get(Unit.VEHICLE).add(0);
            getComponents().get(Unit.INFANTRY).add(0);
            getComponents().get(Unit.BATTLEARMOR).add(0);
            getComponents().get(Unit.PROTOMEK).add(0);
            getComponents().get(Unit.AERO).add(0);
        }

    }

    public ConcurrentHashMap<Integer, Vector<Vector<SUnit>>> getHangar() {
        return Hangar;
    }

    public Vector<Vector<SUnit>> getHangar(int Type_id) {
        if (Hangar == null || Hangar.size() < Type_id) {
            return null;
        }
        return Hangar.get(Type_id);
    }

    public boolean isNewbieHouse() {
        return false;
    }

    public boolean isMercHouse() {
        return false;
    }

    public SHouse getHouseFightingFor(SPlayer player) {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        SHouse h = null;

        try {
            h = (SHouse) o;
        } catch (ClassCastException e) {
            return false;
        }

        if (h == null) {
            return false;
        }

        if (h.getName().equals(getName())) {
            return true;
        }

        return false;
    }

    public void addDispossessedPilot(SUnit u, boolean skipSkillChange) {

        if (u.hasVacantPilot()) {
            return;
        }

        if (skipSkillChange) {
            getPilotQueues().addPilot(u.getType(), (SPilot) u.getPilot(), true);
        } else {
            // normal de-levalling addition
            getPilotQueues().addPilot(u.getType(), (SPilot) u.getPilot());
        }
    }

    public PilotQueues getPilotQueues() {
        return pilotQueues;
    }

    public SPilot getNewPilot(int uType) {
        SPilot pilot = getPilotQueues().getPilot(uType);
        pilot.setCurrentFaction(getName());
        return pilot;
    }

    /**
     * Method which checks all three activity states to see if a player w/ a
     * given name is logged in to the faction
     */
    public boolean isLoggedIntoFaction(String playerName) {

        String lowerName = playerName.toLowerCase();
        if (getReservePlayers().containsKey(lowerName)) {
            return true;
        } else if (getActivePlayers().containsKey(lowerName)) {
            return true;
        } else if (getFightingPlayers().containsKey(lowerName)) {
            return true;
        }

        // not in the faction under any status.
        return false;
    }

    public int remainingHangarSpaceForWeightclass(int Weightclass, int TypeID) {

        // don't want to count units that are for sale.
        int trueHangarSize = getNumberOfNonSaleUnits(getHangar(TypeID).elementAt(Weightclass));

        if (Weightclass == Unit.LIGHT) {
            if (TypeID != Unit.MEK) {
                return (Integer.parseInt(this.getConfig("MaxLightUnits")) / 2) - trueHangarSize;
            }

            // else
            return Integer.parseInt(this.getConfig("MaxLightUnits")) - trueHangarSize;
        }

        // else (nonlight weighclass)
        if (TypeID != Unit.MEK) {
            return (Integer.parseInt(this.getConfig("MaxOtherUnits")) / 2) - trueHangarSize;
        }

        return Integer.parseInt(this.getConfig("MaxOtherUnits")) - trueHangarSize;
    }

    /**
     * Returns the number of players who count for mintick production. Called
     * from SHouse.tick(). Factored out to keep the tick more or less redable.
     * 
     * An active player counts if he has at least one army that is between min
     * and max BVs to count, and has been active for a whole tick.
     * 
     * Fighting players may or may not count, depending on the weight assigned
     * by the admins to the game-type they are playing.
     * 
     * 
     * @return double Amount of Production produced by players
     */
    private double getNumberOfPlayersWhoCountForProduction() {

        double result = activityPP;

//        MWLogger.debugLog("Getting all fighting players");
//        // now loop through all of the fighting players
//        for (SPlayer currP : getFightingPlayers().values()) {
//
//            /*
//             * Get the player's short op. He's fighing, so there should always
//             * be one, but check for a null just in case.
//             */
//            MWLogger.debugLog("checking short operation for " + currP.getName());
//            ShortOperation so = CampaignMain.cm.getOpsManager().getShortOpForPlayer(currP);
//            if (so == null) {
//                continue;
//            }
//
//            MWLogger.debugLog("Getting data for op " + so.getName() + " for player " + currP.getName());
//            Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
//            double value = o.getDoubleValue("CountGameForProduction");
//            if (value < 0) {
//                value = 0;
//            }
//
//            MWLogger.debugLog("adding value.");
//            // add the players weight to the total faction multiplier
//            result += value;
//
//            MWLogger.debugLog("Showing output");
//            // if enabled, show the player his personal worth
////            if (value > 0 && showOutput) {
////                String toReturn = "AM:You counted towards production this tick";
////                DecimalFormat myFormatter = new DecimalFormat("###.##");
////                String output = myFormatter.format(value);
////                toReturn += " (" + output + " points worth)";
////                CampaignMain.cm.toUser(toReturn + ".", currP.getName(), true);
////            }
//        }// end for(fighting players)

        MWLogger.debugLog("returning with results.");
        // pass back the aggregate value.
        return result;
    }

    /**
     * have the faction perform tick duties (gather income, referesh factories)
     * and clean out its hangars and PP excesses (either via scrapping,
     * industrial accidents, or BM sales), then report the tick results to all
     * of its faction members.
     */
    public String tick(boolean real, int tickid) {
        /*
         * Something in this block appears to be causing MMNet's hangs.
         * Unfortunately, it doesn't lend itself to very good logging. I'll see
         * what I can do.
         */
        MWLogger.debugLog("Inside SHouse.Tick for: " + getName());
        String result = "-------> <b>Tick! [" + tickid + "]</b><br>";
        StringBuilder hsUpdates = new StringBuilder();

        double tickworth = 0;

        MWLogger.debugLog("Getting number of players who count for production");

        // non-real ticks occur the first time a server starts, when free
        // minticks are given away
        if (!real) {
            tickworth = 10;// give 10 players worth ...
        } else {
            // if real, get the weighted number of valid players
            tickworth = getNumberOfPlayersWhoCountForProduction();
            resetActivityPP(); // Now that we have it, we need to clear it so they don't get counted twice.
        }
        
        
        MWLogger.debugLog("     -> " + tickworth);

        MWLogger.debugLog("Calculating refresh points");
        
        // Refresh factories
        calcActivityPP(tickworth);   
        
        /*
         * Loop throuhgh all hangars and component vectors, looking for
         * overages. Remove units (destroy or sell) and components (destroy or
         * build units) until under caps.
         * 
         * This block of code was formerly SHouse.cleanUpHangarAndPP. Moved
         * inline with the rest of tick() in order to facilities house status
         * updates. @urgru 6.10.06
         */

        // strings to build on, so info can be sorted in event/type/weight order
        StringBuilder mechsProduced = new StringBuilder();
        StringBuilder industrialAccidents = new StringBuilder();
        StringBuilder scrapExcuses = new StringBuilder();
        StringBuilder marketAdditions = new StringBuilder();

        MWLogger.debugLog("Checking for Unit Overflow");
        /*
         * Loop though every type and weight class, looking for overflow. If
         * there are more units than allowed in the hangar, dispose of random
         * units by scrapping or selling (on Market) until back at cap.
         */
        for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {
            for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {

                // keep scrapping/selling until we're at cap.
                while (remainingHangarSpaceForWeightclass(i, type_id) < 0) {

                    // get vector of units of the right weight, then select a
                    // random unit from the stack.
                    Vector<SUnit> v = this.getHangar(type_id).elementAt(i);
                    
                    // Get a unit.  If the SO has set the flag for selecting the oldest units,
                    // first, get that one, if not, get a random one.
                    SUnit randUnit;
                    
                    if (CampaignMain.cm.getBooleanConfig("ScrapOldestUnitsFirst")) {
                    	Collections.sort(v);
                    	// Crap.  This could loop, if every unit is on the BM already.
                    	// So, find the first unit thatis not already for sale
                    	int unitToGet = -1;
                    	for (int j = 0; j < v.size(); j++ ) {
                    		if (unitToGet == -1 && v.elementAt(j).getStatus() != Unit.STATUS_FORSALE) {
                    			unitToGet = j;
                    		}
                    	}
                    	if (unitToGet == -1) {
                    		// Nothing to see here, move along
                    		continue;
                    	}
                    	randUnit = v.elementAt(unitToGet);
                    } else {
                    	randUnit = v.elementAt(CampaignMain.cm.getRandomNumber(v.size()));
                    }

                    if (randUnit.getStatus() == Unit.STATUS_FORSALE) {
                        continue;
                    }

                    int bmPercent = Integer.parseInt(this.getConfig("ChanceToSendUnitToBM"));
                    if (maySellOnBM() && CampaignMain.cm.getRandomNumber(101) < bmPercent && SUnit.mayBeSoldOnMarket(randUnit)) {

                        // Use standard factory pricing for the unit, and
                        // configured ticks.
                        //int minPrice = getPriceForUnit(i, type_id);
                    	int minPrice = getBMPriceForUnit(i, type_id);
                        String saleTicksString = Unit.getWeightClassDesc(randUnit.getWeightclass()) + "SaleTicks";
                        // add 1 to the sale tick due to a quirk with the BM
                        // autoupdate.
                        // The the unit is sent to the player before the new
                        // tick counter so the clients
                        // are a tick ahead of the server.
                        int saleTicks = Integer.parseInt(this.getConfig(saleTicksString)) + 1;

                        // Add the unit to the market, and tell the faction
                        CampaignMain.cm.getMarket().addListing(getName(), randUnit, minPrice, saleTicks);
                        if (!Boolean.parseBoolean(CampaignMain.cm.getConfig("HiddenBMUnits"))) {
                        	marketAdditions.append(StringUtils.aOrAn(randUnit.getModelName(), false) + " was added to the black market.<br>");
                        }
                        hsUpdates.append(getHSUnitRemovalString(randUnit));// "remove"
                        // unit
                        // from
                        // client's
                        // perspective
                        randUnit.setStatus(Unit.STATUS_FORSALE);
                    } else {
                        String currScrapExcuse = getExcuseForUnitFailure(randUnit);
                        scrapExcuses.append(currScrapExcuse + "<br>");
                        hsUpdates.append(removeUnit(randUnit, false));
                    }
                }// end while(too many units)

            }// end weight class loop
        }// end unit type loop

        /*
         * Ok we've created components now lets see if we covert them into
         * crits.
         */
        if (getComponentConverter().size() > 0) {
            produceCrits();
        }

        MWLogger.debugLog("Doing Component Overflow");
        /*
         * Loop through all types/weightclasses as above, but look for component
         * overflow instead of hangar overage. Here we either scrap the
         * components (aka "industrial accident") or autoproduce a brand new
         * unit and drop it in the house hangar.
         * 
         * We look for component overflow after hangar overflow in order to be
         * sure that newly autoproduced units aren't immediately dumped onto the
         * market or nuked.
         */
        for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {
            for (int weight = 0; weight < 4; weight++) {

                while (getPP(weight, type_id) > getMaxAllowedPP(weight, type_id)) {

                    int randomLossFactor = CampaignMain.cm.getRandomNumber(getPPCost(weight, type_id)) + 1;

                    // see if we should have an accident
                    boolean accident = false;
                    SUnitFactory m = getNativeFactoryForProduction(type_id, weight, CampaignMain.cm.getBooleanConfig("OnlyUseOriginalFactoriesForAutoprod"));
                    int failureRateToUse;
                    if(Boolean.parseBoolean(this.getConfig("UseAutoProdClassic"))) {
                    	failureRateToUse = Integer.parseInt(this.getConfig("AutoProductionFailureRate"));
                    } else {
                    	failureRateToUse = Integer.parseInt(this.getConfig("APFailureRate" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type_id)));
                    }
                    if (CampaignMain.cm.getRandomNumber(100) + 1 <= failureRateToUse) {
                        accident = true;
                    }

                    // no factory to produce, or random accident
                    if (m == null || accident) {
                        hsUpdates.append(addPP(weight, type_id, -randomLossFactor, false));
                        if (type_id == Unit.INFANTRY) {
                            industrialAccidents.append("a cache of " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type_id) + " supplies is donated to the Salvation Army.<br>");
                        } else {
                            industrialAccidents.append("An industrial accident destroys a substantial cache of " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type_id) + " components.<br>");
                        }
                    }

                    // else, make a new unit
                    else {
                        Vector<SUnit> newUnits = m.getMechProduced(type_id, getNewPilot(type_id));
                        for (SUnit newUnit : newUnits) {
                            MWLogger.debugLog("AP Unit " + newUnit.getModelName());
                            hsUpdates.append(this.addUnit(newUnit, false));
                            hsUpdates.append(addPP(weight, type_id, -(getPPCost(weight, type_id)), false));
                            /*
                             * set refresh and add to back end of the HS update.
                             * if the refresh is added in-line in the
                             * SUnitFactory, the command is sent BEFORE the
                             * final HS command, which then overwrites the
                             * correct refresh time w/ an incorrect reflesh time
                             * that reflects player activity.
                             */
                            if (!Boolean.parseBoolean(this.getConfig("UseCalculatedCosts"))) {
                                // set the refresh miniticks
                                if (m.getWeightclass() == Unit.LIGHT) {
                                    hsUpdates.append(m.addRefresh((Integer.parseInt(this.getConfig("LightRefresh")) * 100) / m.getRefreshSpeed(), false));
                                } else if (m.getWeightclass() == Unit.MEDIUM) {
                                    hsUpdates.append(m.addRefresh((Integer.parseInt(this.getConfig("MediumRefresh")) * 100) / m.getRefreshSpeed(), false));
                                } else if (m.getWeightclass() == Unit.HEAVY) {
                                    hsUpdates.append(m.addRefresh((Integer.parseInt(this.getConfig("HeavyRefresh")) * 100) / m.getRefreshSpeed(), false));
                                } else if (m.getWeightclass() == Unit.ASSAULT) {
                                    hsUpdates.append(m.addRefresh((Integer.parseInt(this.getConfig("AssaultRefresh")) * 100) / m.getRefreshSpeed(), false));
                                }
                            }

                            if (type_id == Unit.INFANTRY) {
                                // exclusive message
                                mechsProduced.append("A militia unit [" + newUnit.getModelName() + "] from " + m.getPlanet().getName() + " activated for front line duty!<br>");
                            } else {
                                // non infantry, so use a standard build message
                                mechsProduced.append("Technicians assembled a " + newUnit.getModelName() + " at " + m.getName() + " on " + m.getPlanet().getName() + ".<br>");
                            }
                        }
                    }
                }// end while(PP > MaxPP)
            }// end for(all 4 weight classes)
        }// end for(all 3 types)

        // now, assemble the strings
        result += mechsProduced.toString() + marketAdditions.toString() + industrialAccidents.toString() + scrapExcuses.toString();

        MWLogger.debugLog("show Production Count");
        if ((getShowProductionCountNext() - 1) <= 0) {
            setShowProductionCountNext((Integer.parseInt(this.getConfig("ShowComponentGainEvery"))));

            // report how many mechs of each weight class the faction can
            // produce.
            int MekComponents = getComponentsProduced(Unit.MEK);
            int VehComponents = getComponentsProduced(Unit.VEHICLE);
            int InfComponents = getComponentsProduced(Unit.INFANTRY);
            int ProtoComponents = getComponentsProduced(Unit.PROTOMEK);
            int BAComponents = getComponentsProduced(Unit.BATTLEARMOR);
            int AeroComponents = getComponentsProduced(Unit.AERO);

            DecimalFormat myFormatter = new DecimalFormat("###.##");

            result += "<br><i><b>Your factories produced enough components to make:</b></i><br>";
            if (Boolean.parseBoolean(this.getConfig("UseMek"))) {
                result += myFormatter.format(MekComponents / (Double.parseDouble(this.getConfig("LightPP")))) + " Light meks<br>";
                result += myFormatter.format(MekComponents / (Double.parseDouble(this.getConfig("MediumPP")))) + " Medium meks<br>";
                result += myFormatter.format(MekComponents / (Double.parseDouble(this.getConfig("HeavyPP")))) + " Heavy meks<br>";
                result += myFormatter.format(MekComponents / (Double.parseDouble(this.getConfig("AssaultPP")))) + " Assault meks<br>";
            }
            if (Boolean.parseBoolean(this.getConfig("UseVehicle"))) {
                result += myFormatter.format(VehComponents / (Double.parseDouble(this.getConfig("LightVehiclePP")))) + " Light vehicles<br>";
                result += myFormatter.format(VehComponents / (Double.parseDouble(this.getConfig("MediumVehiclePP")))) + " Medium vehicles<br>";
                result += myFormatter.format(VehComponents / (Double.parseDouble(this.getConfig("HeavyVehiclePP")))) + " Heavy vehicles<br>";
                result += myFormatter.format(VehComponents / (Double.parseDouble(this.getConfig("AssaultVehiclePP")))) + " Assault vehicles<br>";
            }
            if (Boolean.parseBoolean(this.getConfig("UseInfantry"))) {

                // show only light, and no weightclass if UseOnlyLight
                if (Boolean.parseBoolean(this.getConfig("UseOnlyLightInfantry"))) {
                    result += myFormatter.format(InfComponents / (Double.parseDouble(this.getConfig("LightInfantryPP")))) + " Infantry<br>";
                } else {
                    result += myFormatter.format(InfComponents / (Double.parseDouble(this.getConfig("LightInfantryPP")))) + " Light infantry<br>";
                    result += myFormatter.format(InfComponents / (Double.parseDouble(this.getConfig("MediumInfantryPP")))) + " Medium infantry<br>";
                    result += myFormatter.format(InfComponents / (Double.parseDouble(this.getConfig("HeavyInfantryPP")))) + " Heavy infantry<br>";
                    result += myFormatter.format(InfComponents / (Double.parseDouble(this.getConfig("AssaultInfantryPP")))) + " Assault infantry<br>";
                }
            }// end if(UseInfantry)
            if (Boolean.parseBoolean(this.getConfig("UseProtoMek"))) {
                result += myFormatter.format(ProtoComponents / (Double.parseDouble(this.getConfig("LightProtoMekPP")))) + " Light protomechs<br>";
                result += myFormatter.format(ProtoComponents / (Double.parseDouble(this.getConfig("MediumProtoMekPP")))) + " Medium protomechs<br>";
                result += myFormatter.format(ProtoComponents / (Double.parseDouble(this.getConfig("HeavyProtoMekPP")))) + " Heavy protomechs<br>";
                result += myFormatter.format(ProtoComponents / (Double.parseDouble(this.getConfig("AssaultProtoMekPP")))) + " Assault protomechs<br>";
            }
            if (Boolean.parseBoolean(this.getConfig("UseBattleArmor"))) {
                result += myFormatter.format(BAComponents / (Double.parseDouble(this.getConfig("LightBattleArmorPP")))) + " Light battle armor<br>";
                result += myFormatter.format(BAComponents / (Double.parseDouble(this.getConfig("MediumBattleArmorPP")))) + " Medium battle armor<br>";
                result += myFormatter.format(BAComponents / (Double.parseDouble(this.getConfig("HeavyBattleArmorPP")))) + " Heavy battle armor<br>";
                result += myFormatter.format(BAComponents / (Double.parseDouble(this.getConfig("AssaultBattleArmorPP")))) + " Assault battle armor<br>";
            }

            if (Boolean.parseBoolean(this.getConfig("UseAero"))) {
                result += myFormatter.format(AeroComponents / (Double.parseDouble(this.getConfig("LightAeroPP")))) + " Light aero<br>";
                result += myFormatter.format(AeroComponents / (Double.parseDouble(this.getConfig("MediumAeroPP")))) + " Medium aero<br>";
                result += myFormatter.format(AeroComponents / (Double.parseDouble(this.getConfig("HeavyAeroPP")))) + " Heavy aero<br>";
                result += myFormatter.format(AeroComponents / (Double.parseDouble(this.getConfig("AssaultAeroPP")))) + " Assault aero<br>";
            }

            MWLogger.debugLog("SetComponentsProduced");
            // and return the result to CampaignMain in order to have it sent to
            // the players
            setComponentsProduced(Unit.MEK, 0);
            setComponentsProduced(Unit.VEHICLE, 0);
            setComponentsProduced(Unit.INFANTRY, 0);
            setComponentsProduced(Unit.PROTOMEK, 0);
            setComponentsProduced(Unit.BATTLEARMOR, 0);
            setComponentsProduced(Unit.AERO, 0);
        } else {
            addShowProductionCountNext(-1);
        }
        
        MWLogger.debugLog("Send House Updates: ");
        MWLogger.debugLog("     -> " + hsUpdates.toString());
        // send house updates, if not empty
        if (hsUpdates.length() > 0) {
            CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdates.toString(), false);
        }
        MWLogger.debugLog("returning from tick: " + getName());
        return result;
    }

    /**
     * @author V.I. Lenin aka Travis Shade
     * @param m
     * @return TODO: Refactor to reduce redundant code. Should use
     *         typename.toLowerCase() in place of explicit paths to filenames.
     */
    private String getExcuseForUnitFailure(SUnit m) {

        if (m.getType() == Unit.MEK) {
            return scrapExcuseHelper("./data/scrapmessages/mekscrapmessages.txt", m);
        } else if (m.getType() == Unit.VEHICLE) {
            return scrapExcuseHelper("./data/scrapmessages/vehiclescrapmessages.txt", m);
        } else if (m.getType() == Unit.PROTOMEK) {
            return scrapExcuseHelper("./data/scrapmessages/protoscrapmessages.txt", m);
        } else if (m.getType() == Unit.BATTLEARMOR) {
            return scrapExcuseHelper("./data/scrapmessages/bascrapmessages.txt", m);
        } else if (m.getType() == Unit.INFANTRY) {
            return scrapExcuseHelper("./data/scrapmessages/infantryscrapmessages.txt", m);
        } else if (m.getType() == Unit.AERO) {
            return scrapExcuseHelper("./data/scrapmessages/aeroscrapmessages.txt", m);
        }

        // This should never be reached :)
        return "A " + m.getModelName() + " was kidnapped by aliens from outer space";
    }

    /**
     * Helper method for SHouse.getExcuseForUnitFailure() that factors out
     * highly redundant input stream code.
     */
    private String scrapExcuseHelper(String filepath, SUnit unit) {

        try {

            // set up input buffers
            FileInputStream fis = new FileInputStream(filepath);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));

            // pick random message, given count from line 1
            int messages = Integer.parseInt(dis.readLine());
            int id = CampaignMain.cm.getRandomNumber(messages);

            // read lines until counter reaches randomly selected message
            String scrapMessage = "";
            while (dis.ready()) {
                scrapMessage = dis.readLine();
                if (id <= 0) {
                    break;
                }
                id--;
            }

            // close buffers
            dis.close();
            fis.close();

            // replace targetted text w/ unit & pilot specific messages and
            // return.
            String scrapMessageWithPilot = scrapMessage.replaceAll("PILOT", unit.getPilot().getName());
            String scrapMessageForPlayer = scrapMessageWithPilot.replaceAll("UNIT", unit.getModelName());
            return scrapMessageForPlayer;

        } catch (Exception e) {// ./data/scrapmessages/ is 21 chars. strip path
            // leader and just name file w/ problems.
            MWLogger.errLog("A problem occured with your " + filepath.substring(21, filepath.length()) + " file!");
            return "A " + unit.getModelName() + " was kidnapped by aliens from outer space";
        }
    }

    // VOTE AND RANKING METHODS @urgru 9/12/04
    /*
     * Need to make a few temp vectors when a faction is first created, which
     * hold ranking orders. Think about how to do this while still being
     * efficient w/i Hibernate. Looping through the entive vote vector for each
     * player to get a typecount seems too inefficient for words --- but may be
     * fine w/ SQL.
     * 
     * Talk about this with Helge before implementing anything.
     */

    // PRODUCTION POINT METHODS @urgru 02/03/03
    /**
     * A method which returns the number of PP a faction has for a specified
     * weight class
     * 
     * @param weight
     *            - the weight class to return PP for
     * @return type_id - number of PP the faction has for a given weight class
     */
    public int getPP(int weight, int type_id) {
        Vector<Integer> v = getComponents().get(type_id);
        if (v == null) {
            return 0;
        }
        Integer i = v.elementAt(weight);
        if (i == null) {
            return 0;
        }
        return i.intValue();
    }

    public Vector<SUnitFactory> getPossibleFactoryForProduction(int type, int weight, boolean ignoreRefresh) {
        Vector<SUnitFactory> possible = new Vector<SUnitFactory>(1, 1);
        Iterator<SPlanet> e = Planets.values().iterator();
        while (e.hasNext()) {
            SPlanet p = e.next();
            Vector<SUnitFactory> v = p.getFactoriesOfWeighclass(weight);
            for (int i = 0; i < v.size(); i++) {
                SUnitFactory MF = v.elementAt(i);
                if (MF.canProduce(type) && (ignoreRefresh || MF.getTicksUntilRefresh() < 1)) {
                    possible.add(MF);
                }
            }
        }
        return possible;
    }

    /**
     * Method that returns a factory originally owned by this faction which is
     * able to produce units of the requested tyoe and weight. This is used
     * during ticks and with a-specific requests (RequestCommand), so that units
     * build randomly on ticks or pursuant to a general purchase request are
     * from the faction's own tables.
     */
    public SUnitFactory getNativeFactoryForProduction(int type, int weight, boolean useOnlyOriginalFactories) {

        // get all possible @ weight and type and return if none exist
        Vector<SUnitFactory> allPossible = getPossibleFactoryForProduction(type, weight, false);
        if (allPossible.size() == 0) {
            return null;
        }

        // sort out non-faction factories and return if none exist
        Vector<SUnitFactory> factionPossible = new Vector<SUnitFactory>(1, 1);
        for (SUnitFactory currFac : allPossible) {
            if (!useOnlyOriginalFactories || currFac.getFounder().equalsIgnoreCase(getName())) {
                factionPossible.add(currFac);
            }
        }
        if (factionPossible.size() == 0) {
            return null;
        }

        // select a random factory to return
        int rand = CampaignMain.cm.getRandomNumber(factionPossible.size());
        return (factionPossible.elementAt(rand));
    }
    
    /**
     *@Salient , this is for subfaction enforcement rule when player presses buy new, it pulls from correct factory
     * 
     */
    public SUnitFactory getNativeAccessableFactoryForProduction(int type, int weight, int subFactionLvl, String Username) {

        //CampaignMain.cm.toUser("DEBUG: subfactionLvl:" + subFactionLvl , Username, true);

        // get all possible @ weight and type and return if none exist
        Vector<SUnitFactory> allPossible = getPossibleFactoryForProduction(type, weight, false);
        if (allPossible.size() == 0) {
            return null;
        }

        // sort out non-faction factories and return if none exist
        Vector<SUnitFactory> factionPossible = new Vector<SUnitFactory>(1, 1);
        for (SUnitFactory currFac : allPossible) 
        {
            //CampaignMain.cm.toUser("DEBUG: All List:" + currFac.getFounder() + " AccessLvL: " + currFac.getAccessLevel() , Username, true);

            if (currFac.getFounder().equalsIgnoreCase(getName())) 
            {
                factionPossible.add(currFac);
            }
        }
        if (factionPossible.size() == 0) {
            return null;
        }
        
        // sort out unaccessable factories and return if none exist
        Vector<SUnitFactory> accessPossible = new Vector<SUnitFactory>(1, 1);
        for (SUnitFactory currFac : factionPossible) 
        {
            //CampaignMain.cm.toUser("DEBUG: House List:" + currFac.getFounder() + " AccessLvL: " + currFac.getAccessLevel() , Username, true);

            if (currFac.getAccessLevel() == subFactionLvl) 
            {
                //CampaignMain.cm.toUser("DEBUG: ADDED TO ACCESS:" + currFac.getFounder() + " AccessLvL: " + currFac.getAccessLevel() , Username, true);
                accessPossible.add(currFac);
            }
        }
        if (accessPossible.size() == 0) {
            //CampaignMain.cm.toUser("DEBUG: NumFactories: " + accessPossible.size() , Username, true);
            return null;
        }

        // select a random factory to return
        int rand = CampaignMain.cm.getRandomNumber(accessPossible.size());
        return (accessPossible.elementAt(rand));
    }
    
    public int getMaxAllowedPP(int weight, int type_id) {
    	String unitAPMax = "";
        if(CampaignMain.cm.getBooleanConfig("UseAutoProdNew")) {
        	unitAPMax = "APAtMax" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type_id);
        } else {
        	unitAPMax = "APAtMax" + Unit.getWeightClassDesc(weight) + "Units";        	
        }
    	int maxUnits = Integer.parseInt(this.getConfig(unitAPMax));
        return maxUnits * getPPCost(weight, type_id);
    }

    /**
     * A method which returns the PP COST of a unit. Meks and Vehicles are
     * segregated by weightclass. Infantry are flat priced accross all weight
     * classes.
     * 
     * @param weight
     *            - the weight class to be checked
     * @return int - the PP cost
     */
    public int getPPCost(int weight, int type_id) {

        int result = Integer.MAX_VALUE;
        String classtype = Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type_id) + "PP";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(this.getConfig(Unit.getWeightClassDesc(weight) + "PP"));
        } else {
            result = Integer.parseInt(this.getConfig(classtype));
        }

        // modify the result by the faction price modifier
        result += getHouseUnitComponentMod(type_id, weight);

        // dont allow negative component use
        result = Math.max(1, result);

        return result;
    }

    /**
     * A method to keep track of Production Points due to player activity.  Called from a PlayerActivityComponentJob
     * @param armyWeight
     */
    public void addActivityPP(Double armyWeight) {
    	activityPP += armyWeight;
    	MWLogger.debugLog("Adding " + armyWeight + " in production. " + getName() + " total now " + activityPP);
    }
    
    public void resetActivityPP() {
    	activityPP = 0;
    }
    
    public void calcActivityPP(Double armyWeight) {
        double cComp = getComponentProduction();
        int componentsToAdd = (int) (armyWeight * cComp);
        int refreshToAdd = (int) Math.ceil(armyWeight);

        if (getIntegerConfig("FactoryRefreshPoints") > -1) {
            // Allow Servers to refresh factories without having active players.
            refreshToAdd = getIntegerConfig("FactoryRefreshPoints");
        }
        
        StringBuilder hsUpdates = new StringBuilder();
        // Get income, and refresh factories
        Iterator<SPlanet> e = getPlanets().values().iterator();
        while (e.hasNext()) {// loop through all planets which the faction
            // has territory on
            SPlanet p = e.next();
            if (equals(p.getOwner())) {
            	MWLogger.debugLog("Calling tick on " + p.getName() + " to add " + refreshToAdd + " refresh");
                hsUpdates.append(p.tick(refreshToAdd));// call the planetary
                // tick
            }
        }

        // then add to the faction PP pools
        boolean useMekPP = Boolean.parseBoolean(this.getConfig("UseMek"));
        boolean useVehiclePP = Boolean.parseBoolean(this.getConfig("UseVehicle"));
        boolean useInfantryPP = Boolean.parseBoolean(this.getConfig("UseInfantry"));
        boolean useProtoMekPP = Boolean.parseBoolean(this.getConfig("UseProtoMek"));
        boolean useBattleArmorPP = Boolean.parseBoolean(this.getConfig("UseBattleArmor"));
        boolean useAeroPP = Boolean.parseBoolean(this.getConfig("UseAero"));

        for (int i = 0; i < 4; i++) {// loop through each weight class,
            // adding PP
            if (useMekPP) {
                MWLogger.debugLog("Updating House Mek Parts: " + i);
                hsUpdates.append(addPP(i, Unit.MEK, componentsToAdd, true));
                addComponentsProduced(Unit.MEK, componentsToAdd);
            }

            if (useVehiclePP) {
                MWLogger.debugLog("Updating House Vehicle Parts: " + i);
                hsUpdates.append(addPP(i, Unit.VEHICLE, componentsToAdd, true));
                addComponentsProduced(Unit.VEHICLE, componentsToAdd);
            }

            if (useInfantryPP) {
                MWLogger.debugLog("Updating House Infantry: " + i);
                if (!Boolean.parseBoolean(this.getConfig("UseOnlyLightInfantry")) || i == Unit.LIGHT) {
                	hsUpdates.append(addPP(i, Unit.INFANTRY, componentsToAdd, true));
                }
                addComponentsProduced(Unit.INFANTRY, componentsToAdd);
            }

            if (useProtoMekPP) {
                MWLogger.debugLog("Updating House ProtoMek: " + i);
                hsUpdates.append(addPP(i, Unit.PROTOMEK, componentsToAdd, true));
                addComponentsProduced(Unit.PROTOMEK, componentsToAdd);
            }

            if (useBattleArmorPP) {
                MWLogger.debugLog("Updating House BA: " + i);
                hsUpdates.append(addPP(i, Unit.BATTLEARMOR, componentsToAdd, true));
                addComponentsProduced(Unit.BATTLEARMOR, componentsToAdd);
            }

            if (useAeroPP) {
                MWLogger.debugLog("Updating House Aero: " + i);
                hsUpdates.append(addPP(i, Unit.AERO, componentsToAdd, false));
                addComponentsProduced(Unit.AERO, componentsToAdd);
            }
        }
        // send house updates, if not empty
        if (hsUpdates.length() > 0) {
            CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdates.toString(), false);
        }
    }
    
    /**
     * A method which adds a specified number of PP to Stores of the given
     * weight class. Can send house status updates, but also returns cmd to be
     * added to longer lists of changes.
     * 
     * @param weight
     *            - int, the weight class to add to
     * @param type_id
     *            - int, type of of PP to add
     * @param quantity
     *            - int, number of components to add
     */
    public String addPP(int weight, int type_id, int val, boolean sendUpdate) {

        // store starting PP
        int startingPP = getPP(weight, type_id);

        try {

            // nothing to add if they have no factories.
            if (!Boolean.parseBoolean(this.getConfig("ProduceComponentsWithNoFactory")) && getPossibleFactoryForProduction(type_id, weight, true).size() < 1 && val > 0) {
                return "";
            }

            // standard addition
            Vector<Integer> v = getComponents().get(type_id);
            v.setElementAt(v.elementAt(weight).intValue() + val, weight);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Error in addPP()");
            MWLogger.errLog("weight: " + weight + " type: " + type_id + " value: " + val);
            Vector<Integer> v = new Vector<Integer>(4, 1);
            for (int i = 0; i < 4; i++) {
                // Weight
                v.add(0);
            }

            getComponents().put(type_id, v);
        }

        // if PP is unchanged, no need to send a real update
        if (startingPP == getPP(weight, type_id)) {
            return "";
        }

        // else, PP changed and we need to make an update string
        String hsUpdate = getHSPPChangeString(weight, type_id);
        if (sendUpdate) {
            CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdate, false);
        }

        return hsUpdate;
    }

    /**
     * A method which returns a unit from the SHouse's queue. This should only
     * be called from SHouse (during ticks) or RequestDonatedCommand (during an
     * ask). If there is no queue'd unit of the given weightclass/type, a null
     * is returned.
     * 
     * WARNING!! getEntity() returns a unit, which means it cannot return a HS|
     * command string like removeUnit() does. Code that makes use of getEntity
     * will need to set up and send one using getHSUnitRemovalString().
     */
    public SUnit getEntity(int weightclass, int type_id) {
        Vector<SUnit> s;
        try {
            s = this.getHangar(type_id).elementAt(weightclass);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Empty Vector in getEntity");
            return null;
        }

        if (s == null) {
            return null;
        }

        if (getNumberOfNonSaleUnits(s) > 0) {
            SUnit m = null;

            Vector<SUnit> unitsToBuy = new Vector<SUnit>(s.size(), 1);
            for (int pos = 0; pos < s.size(); pos++) {
                m = s.elementAt(pos);
                if (m.getStatus() != Unit.STATUS_FORSALE) {
                    unitsToBuy.add(m);
                    m.setStatus(Unit.STATUS_OK);
                }
            }
            unitsToBuy.trimToSize();
            int ran = CampaignMain.cm.getRandomNumber(unitsToBuy.size());
            m = unitsToBuy.elementAt(ran);
            s.removeElement(m);
            unitsToBuy.clear();
            return m;
        }
        return null;
    }

    private int getNumberOfNonSaleUnits(Vector<SUnit> units) {
        int count = 0;

        for (SUnit unit : units) {
            if (unit.getStatus() != Unit.STATUS_FORSALE) {
                count++;
            }
        }
        return count;
    }

    /**
     * Method required for ISeller compliance. Used to distinguish between human
     * controlled actors (SPlayer class) and factions/automated actors (this).
     */
    public boolean isHuman() {
        return false;
    }

    /**
     * Method required for compliance with ISeller. Loop through all house
     * queues and return a unit with matching ID, or null if no matching unit is
     * found.
     * 
     * NOTE: This should be used sparingly. Outside of the Market and various
     * admin commands, there are ALWAYS better ways to get a unit from SHouse.
     */
    public SUnit getUnit(int unitIDtoFind) {

        // for all types and weight classes
        for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {
            for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {

                // Loop through all units of the current type/weightclass
                Iterator<SUnit> it = (this.getHangar(type_id).elementAt(i)).iterator();
                while (it.hasNext()) {
                    SUnit currU = it.next();
                    if (currU.getId() == unitIDtoFind) {
                        return currU;
                    }
                }

            }// end weight class loop
        }// end unit type loop

        // no matching unit in any weight/type queue
        return null;
    }

    /**
     * Simple method which determines whether a given SHouse (and its players)
     * may access the market to SELL units. We check this loop continuously
     * instead of saving a value in the SHouse (inefficient) b/c the config may
     * change between checks.
     */
    public boolean maySellOnBM() {
        StringTokenizer blockedFactions = new StringTokenizer(this.getConfig("BMNoSell"), "$");
        while (blockedFactions.hasMoreTokens()) {
            if (getName().equals(blockedFactions.nextToken())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Simple method which determines whether a given SHouse (and its players)
     * may access the market to BUY units. We check this loop continuously
     * instead of saving a value in the SHouse (inefficient) b/c the config may
     * change between checks.
     */
    public boolean mayBuyFromBM() {
        StringTokenizer blockedFactions = new StringTokenizer(this.getConfig("BMNoBuy"), "$");
        while (blockedFactions.hasMoreTokens()) {
            if (getName().equals(blockedFactions.nextToken())) {
                return false;
            }
        }
        return true;
    }

    public SPlayer getPlayer(String s) {

        String lowerName = s.toLowerCase();
        if (getReservePlayers().containsKey(lowerName)) {
            return getReservePlayers().get(lowerName);
        }
        if (getActivePlayers().containsKey(lowerName)) {
            return getActivePlayers().get(lowerName);
        }
        if (getFightingPlayers().containsKey(lowerName)) {
            return getFightingPlayers().get(lowerName);
        }

        return null;
    }

    /**
     * A method which returns the MU cost of a specified campaign unit.
     * 
     * @return int - # of MU it takes to buy a unit of the given weight class
     */
    public int getPriceForUnit(int weightclass, int type_id) {
        int result = Integer.MAX_VALUE;
        String classtype = Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(type_id) + "Price";

        if (Boolean.parseBoolean(this.getConfig("UseCalculatedCosts"))) {
            double cost = 0;
            if (type_id == Unit.MEK) {
                cost = CampaignMain.cm.getUnitCostLists().getMinCostValue(weightclass, type_id);
                cost = Math.max(cost, getDoubleConfig(Unit.getWeightClassDesc(weightclass) + "Price"));
            } else if (type_id == Unit.VEHICLE) {
                cost = CampaignMain.cm.getUnitCostLists().getMinCostValue(weightclass, type_id);
                cost = Math.max(cost, getDoubleConfig(classtype));
            } else {
                cost = CampaignMain.cm.getUnitCostLists().getMinCostValue(Unit.LIGHT, type_id);
                cost = Math.max(cost, getDoubleConfig(classtype));
            }
            result = (int) (cost * Double.valueOf(this.getConfig("CostModifier")));
            return result;
        }

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(this.getConfig(Unit.getWeightClassDesc(weightclass) + "Price"));
        } else {
            result = Integer.parseInt(this.getConfig(classtype));
        }

        // modify the result by the faction price modifier
        result += getHouseUnitPriceMod(type_id, weightclass);

        // dont allow negative pricing
        if (result < 0) {
            result = 0;
        }

        return result;
    }// end getPriceForUnit()

    /**
     * A method which returns the influence cost of a specified campaign mech.
     * 
     * @return int - # of PP it takes to buy a mech of the given units weight
     *         class
     */
    public int getInfluenceForUnit(int weightclass, int type_id) {
        int result = Integer.MAX_VALUE;
        String classtype = Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(type_id) + "Inf";

        if (type_id == Unit.MEK) {
            result = Integer.parseInt(this.getConfig(Unit.getWeightClassDesc(weightclass) + "Inf"));
        } else {
            result = Integer.parseInt(this.getConfig(classtype));
        }

        // modify the result by the faction price modifier
        result += getHouseUnitFluMod(type_id, weightclass);

        // dont allow negative pricing
        if (result < 0) {
            result = 0;
        }

        return result;
    }

    private int getBMPriceForUnit(int weight, int type) {
    	int price = getPriceForUnit(weight, type);
    	double multiplier = CampaignMain.cm.getDoubleConfig("BMPriceMultiplier_" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type));
    	int finalPrice = (int)(price * multiplier);
    	return finalPrice;
    }
    
    private void parseSupportFile(String fileName, boolean addUnits) {
        File file = new File(fileName);
        if (!file.exists()) {
            return;
        }
        try {
            FileInputStream fis = new FileInputStream(file);
            BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
            while (dis.ready()) {
                if (addUnits) {
                    addUnitSupported(dis.readLine(), true);
                } else {
                    removeUnitSupported(dis.readLine(), true);
                }
            }
            dis.close();
            fis.close();
        } catch (FileNotFoundException fnfe) {
            MWLogger.mainLog("FNFE!!!!");
        } catch (IOException ioe) {
            MWLogger.mainLog("IOE!!!");
        }
    }

    public void addUnitSupported(String fileName, boolean sendMail) {
        if (fileName.trim().length() < 1) {
            return;
        }
        fileName = fileName.trim();
        StringBuilder toReturn = new StringBuilder();
        if (houseSupportsUnit(fileName)) {
            int num = getSupportedUnits().get(fileName);
            supportedUnits.put(fileName, num + 1);
        } else {
            supportedUnits.put(fileName, 1);
            toReturn.append(fileName);
        }
        if (toReturn.length() == 0) {
            return;
        }
        CampaignMain.cm.doSendToAllOnlinePlayers(this, "PL|USU|" + "|true|" + fileName, false);
        CampaignMain.cm.doSendHouseMail(this, "NOTE", "The faction is now able to support the " + toReturn.toString());
    }

    public void removeUnitSupported(String fileName, boolean sendMail) {
        if (fileName.trim().length() < 1) {
            return;
        }
        fileName = fileName.trim();
        StringBuilder toReturn = new StringBuilder();
        if (houseSupportsUnit(fileName)) {
            int num = supportedUnits.get(fileName);
            if (num == 1) {
                // Remove it from the HashMap
                supportedUnits.remove(fileName);
                toReturn.append(fileName);
            } else {
                supportedUnits.put(fileName, num - 1);
            }
        } else {
            // Error. We should never get here.
            MWLogger.mainLog("Error in House.removeUnitProduction(): trying to remove a unit that is not produced.");
            MWLogger.mainLog("  --> House: " + getName() + ", Unit: " + fileName);
        }
        if (toReturn.length() == 0) {
            return;
        }
        CampaignMain.cm.doSendToAllOnlinePlayers(this, "PL|USU|" + "|false|" + fileName, false);
        CampaignMain.cm.doSendHouseMail(this, "NOTE", "The faction has lost the ability to support the following units: " + toReturn.toString());
    }

    public void addPlanet(SPlanet p) {
        if (getPlanets().get(p.getName()) == null) {
            getPlanets().put(p.getName(), p);
            setBaysProvided(getBaysProvided() + p.getBaysProvided());
            setComponentProduction(getComponentProduction() + p.getCompProduction());

            // Add unit production here
            if (CampaignMain.cm.isUsingIncreasedTechs() && p.getFactoryCount() > 0) {
                modifyUnitSupport(p, true);
            }
        }
    }

    public void removePlanet(SPlanet p) {
        if (getPlanets().get(p.getName()) != null) {
            getPlanets().remove(p.getName());
            setBaysProvided(getBaysProvided() - p.getBaysProvided());
            setComponentProduction(getComponentProduction() - p.getCompProduction());

            // Remove unit production here
            if (CampaignMain.cm.isUsingIncreasedTechs() && p.getFactoryCount() > 0) {
                modifyUnitSupport(p, false);
            }
        }
    }

    public void transferMoney(SPlayer p, int amount) {
        if (p != null) {
            p.addMoney(amount);
            setMoney(getMoney() - amount);
        }
    }

    public String removeUnit(SUnit unitToRemove, boolean sendUpdate) {

        Vector<SUnit> Weightclass = this.getHangar(unitToRemove.getType()).elementAt(unitToRemove.getWeightclass());
        Weightclass.remove(unitToRemove);

        String hsUpdate = getHSUnitRemovalString(unitToRemove);
        if (sendUpdate) {
            CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdate, false);
        }

        return hsUpdate;
    }

    /**
     * Pass-though method. <code>boolean isNew</code> is unused in SHouse;
     * however, it is needed to comply with IBuyer.
     */
    public String addUnit(SUnit unit, boolean isNew, boolean sendUpdate) {
        return this.addUnit(unit, sendUpdate);
    }

    /**
     * Method which adds a unit to the house. If sendUpdate is true, all logged
     * in house members are sent an HS|AU|. AU| cmd is returned for use in bulk
     * commands by other methods, like SHouse.tick().
     */
    public String addUnit(SUnit unit, boolean sendUpdate) {

        if (Boolean.parseBoolean(this.getConfig("AllowPersonalPilotQueues")) && unit.isSinglePilotUnit() && !unit.hasVacantPilot()) {
            getPilotQueues().addPilot(unit.getType(), (SPilot) unit.getPilot());
            unit.setPilot(new SPilot("Vacant", 99, 99));
        }

        if (Boolean.parseBoolean(this.getConfig("UseOnlyOneVehicleSize")) && unit.getType() == Unit.VEHICLE) {
            unit.setWeightclass(Unit.LIGHT);
        }

        Vector<SUnit> weightClass = getHangar(unit.getType()).elementAt(unit.getWeightclass());
        if (weightClass.contains(unit)) {
            return "";
        }

        weightClass.add(unit);

        String hsUpdate = this.getHSUnitAdditionString(unit);
        if (sendUpdate && !(this.isNewbieHouse() && Boolean.parseBoolean(CampaignMain.cm.getConfig("HiddenBMUnits"))) ){
            CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdate, false);
        }

    return hsUpdate;
}

    /*
     * Log a player into the faction and put him on reserve (normal) status.
     * This should be called only from CM.doLoginPlayer(). If the player is
     * signing on, the SignOn command will handle the reconnectionCheck() and
     * adjust status to fighting if necessary.
     */
    protected String doLogin(SPlayer p) {

        // lowercase the name
        String realName = p.getName();
        String lowerName = realName.toLowerCase();

        /*
         * Player has logged into their house we no longer have to worry about
         * them.
         */
        CampaignMain.cm.releaseLostSoul(p.getName());

        // test to see if the player is already in the hashes
        if (isLoggedIntoFaction(lowerName)) {
            CampaignMain.cm.toUser("CS|" + SPlayer.STATUS_RESERVE, realName, false);
            return null;
        }

        if (p.getPassword() == null) {
            if (isLeader(p.getName())) {
                removeLeader(p.getName());
            }
        } else {
            if (isLeader(p.getName()) && p.getPassword().getAccess() < CampaignMain.cm.getIntegerConfig("factionLeaderLevel")) {
                CampaignMain.cm.updatePlayersAccessLevel(p.getName(), CampaignMain.cm.getIntegerConfig("factionLeaderLevel"));
            } else if (p.getPassword().getAccess() == CampaignMain.cm.getIntegerConfig("factionLeaderLevel") && !isLeader(p.getName())) {
                CampaignMain.cm.updatePlayersAccessLevel(p.getName(), 2);
            }
        }

        // update the player's myHouse
        CampaignMain.cm.toUser("PL|SH|" + getName(), realName, false);

        CampaignMain.cm.toUser("PL|SSN|" + p.getSubFactionName(), realName, false);

        Date d = new Date(System.currentTimeMillis());
        MWLogger.mainLog(d + ":" + "User Logged into House: " + realName);

        // Send the current servers MegaMek game Options
        CampaignMain.cm.toUser("GO|" + CampaignMain.cm.getMegaMekOptionsToString(), realName, false);

        /*
         * Remove from all status hashes and place in reserve, in case the
         * players was somewho disconnected and not recognized while signing
         * back on. The code will later check for a running game and escalate to
         * fighting state if needed.
         */
        reservePlayers.remove(lowerName);
        activePlayers.remove(lowerName);
        fightingPlayers.remove(lowerName);

        getReservePlayers().put(lowerName, p);
        p.setLastSentStatus("");

        CampaignMain.cm.toUser("CS|" + SPlayer.STATUS_RESERVE, realName, false);

        // send player his pilot lists and exclude lists
        CampaignMain.cm.toUser("PL|PPQ|" + p.getPersonalPilotQueue().toString(true), realName, false);
        CampaignMain.cm.toUser("PL|AEU|" + p.getExclusionList().adminExcludeToString("$"), realName, false);
        CampaignMain.cm.toUser("PL|PEU|" + p.getExclusionList().playerExcludeToString("$"), realName, false);

        /*
         * Old code used to look for a running task here, and send auto armies
         * and game options to players who had running games. Players who had
         * games were put in the fighting members hash, players who did not were
         * placed in the active hash.
         * 
         * Now we use doReconnectionCheck() in the Server's SignOn cmd after the
         * login is processed. This sends any autoarmies/options and stops
         * discon threads. It also removes fighting players from active and
         * places them in fighting, as appropriate.
         * 
         * In sum, we can put all players in the Reserve hash at this point, and
         * they will be properly moved afterwards when setBusyNoOpList() is run.
         */
        CampaignMain.cm.getIThread().removeImmunity(p);// logging in player
        // should NEVER be
        // immune

        // send player the MOTD
        Command c = CampaignMain.cm.getServerCommands().get("MOTD");
        c.process(new StringTokenizer("", ""), realName);

        // send the current BM and HS to the player
        CampaignMain.cm.getMarket().sendCompleteMarketStatus(p);
        CampaignMain.cm.toUser("HS|CA|0", realName, false);// clear old data
        CampaignMain.cm.toUser(getCompleteStatus(), realName, false);
        CampaignMain.cm.getPartsMarket().updatePartsBlackMarketPlayer(p);

        /*
         * Now that the player is loaded and has a fresh timestamp look for a
         * corresponding SmallPlayer.
         * 
         * If the smallplayer exists, nothing needs to be done. The
         * SmallPlayer's values will all (with the exception of faction, which
         * is hardset during generation) be over written with the latest
         * SPlayerData information when the various set() calls are made during
         * SPlayer.fromString() during player load.
         * 
         * Otherwise, make a new SmallPlayer with the SPlayer's info and insert
         * it into the Hashtable. @urgru
         */
        SmallPlayer smallp = SmallPlayers.get(lowerName);
        if (smallp == null) {// make a new one
            smallp = new SmallPlayer(p.getExperience(), p.getLastOnline(), p.getRating(), realName, p.getFluffText(), this);
            SmallPlayers.put(lowerName, smallp);
        }

        // Send supported units updates
        if (CampaignMain.cm.isUsingIncreasedTechs()) {
            CampaignMain.cm.toUser("PL|CSU|0", realName, false);
            StringBuilder toSend = new StringBuilder();
            toSend.append("PL|USU|");
            int num = 0;
            for (String unitName : getSupportedUnits().keySet()) {
                num = getSupportedUnits().get(unitName);
                for (; num > 0; num--) {
                    toSend.append("true|");
                    toSend.append(unitName + "|");
                }
            }
            CampaignMain.cm.toUser(toSend.toString(), realName, false);
        }

        if (isLeader(p.getName()) && CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            Command cmd = CampaignMain.cm.getServerCommands().get("GETCOMPONENTCONVERSION");
            cmd.process(new StringTokenizer("", "#"), p.getName());
        }

        // send the player the latest data from the factionbays
        p.setLastOnline(System.currentTimeMillis());// must be done after
        // smallplayer creation
        
        // Send the target system bans
        StringBuilder tsBans = new StringBuilder();
        tsBans.append("SBT|");
       
        for (int ban : CampaignMain.cm.getData().getBannedTargetingSystems()) {
        	tsBans.append(ban);
        	tsBans.append("|");
        }
        tsBans.append("|");
        CampaignMain.cm.toUser(tsBans.toString(), realName, false);
        
        // Send default player flags if it's an admin or mod
        if (CampaignMain.cm.getServer().isModerator(p.getName()) || CampaignMain.cm.getServer().isAdmin(p.getName())) {
        	if(!CampaignMain.cm.getDefaultPlayerFlags().isEmpty()) {
        		CampaignMain.cm.toUser("PF|SDF|" + CampaignMain.cm.getDefaultPlayerFlags().export(), p.getName(), false);
        	}
        }
        
        
        CampaignMain.cm.toUser("PF|S", p.getName(), false);
        return ("<b>[*] Logged into " + getColoredNameAsLink() + ".</b>");
    }

    /**
     * Remove a player from the house lists. Should be called only from
     * CampaignMain's .doLogout(), which sends needed status updates to all
     * players and sets up save information.
     * 
     * We don't need to worry about disconnections or oddly timed logouts (eg -
     * midgame). The only time that kind of abrupt removal should be allowed is
     * when a client closes of loses its connection, which is handled by
     * ServerWrapper.signOff().
     */
    protected void doLogout(SPlayer p) {

        // if the is already logged in, return
        String realName = p.getName();
        String lowerName = realName.toLowerCase();
        if (!isLoggedIntoFaction(lowerName)) {
            return;
        }

        // note: this removes the player from all attacker/defender lists.
        // if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE)
        p.setActive(false);

        // remove from all status hashes
        reservePlayers.remove(lowerName);
        activePlayers.remove(lowerName);
        fightingPlayers.remove(lowerName);

        CampaignMain.cm.forceSavePlayer(p);
        // add info to logs
        Date d = new Date(System.currentTimeMillis());
        MWLogger.mainLog(d + ":" + "User Logged out: " + realName);
        CampaignMain.cm.toUser("CS|" + SPlayer.STATUS_LOGGEDOUT, realName, false);
    }

    /**
     * Completely remove a player from the house. Very simple. Donate the
     * players units, clear out his votes, then nuke hims pfile.
     */
    public void removePlayer(SPlayer p, boolean donateMechs) {

        // check to make sure he's not null
        if (p == null) {
            return;
        }

        // log the player out of the house
        doLogout(p);

        removeLeader(p.getName());
        // Never send the newbie mechs back to the house bays.
        if (isNewbieHouse()) {
            donateMechs = false;
        }

        // if we're donating all units, do so
        if (donateMechs) {
            StringBuilder hsUpdates = new StringBuilder();
            boolean allowDamagedUnits = CampaignMain.cm.isUsingAdvanceRepair() && Boolean.parseBoolean(this.getConfig("AllowDonatingOfDamagedUnits"));
            for (SUnit currUnit : p.getUnits()) {

                boolean damaged = (!UnitUtils.canStartUp(currUnit.getEntity()) || UnitUtils.hasArmorDamage(currUnit.getEntity()) || UnitUtils.hasCriticalDamage(currUnit.getEntity()));

                if ((damaged && allowDamagedUnits) || !damaged) {
                    hsUpdates.append(addUnit(currUnit, false));
                }
            }

            // if units were donated, send updates to factionmates
            if (hsUpdates.length() > 0) {
                CampaignMain.cm.doSendToAllOnlinePlayers(this, "HS|" + hsUpdates.toString(), false);
            }
        }

        /*
         * The player is moving to a new faction (or quitting). Rather than
         * letting all of his votes remain and count, strip them.
         */
        CampaignMain.cm.getVoteManager().removeAllVotesByPlayer(p);
        CampaignMain.cm.getVoteManager().removeAllVotesForPlayer(p);

        // remove small player. don't delete the pfile.
        p.getMyHouse().getSmallPlayers().remove(p.getName().toLowerCase());

    }// end removePlayer()

    /*
     * Used by RangeCommand and CheckDistCommand.
     */
    public int getDistanceTo(SPlanet p, SPlayer player) {
        // Is the faction on the planet?
        if (p.getInfluence().getInfluence(getId()) > 10) {
            return 0;
        }

        double distSq = Integer.MAX_VALUE;
        double tdist;

        Iterator<Planet> e = CampaignMain.cm.getData().getAllPlanets().iterator();
        while (e.hasNext()) {
            SPlanet pl = (SPlanet) e.next();
            // Only consider planet if we control at least 25%
            if (pl.getInfluence().getInfluence(getId()) >= 25) {
                tdist = pl.getPosition().distanceSq(p.getPosition());
                if (tdist < distSq) {
                    distSq = tdist;
                }
            }
        }
        return (int) distSq;
    }

    /**
     * Generates serialized version of SHouse to send to clients for HouseStatus
     * tab. Complete status is sent on login. Afterwards, changes are
     * transmitted incremementally.
     */
    public String getCompleteStatus() {

        String cmdDelim = "|";// used to separate HS| subcommands
        String internalDelim = "$";// used to separate elements within
        // subcommands

        // first item, name
        StringBuilder result = new StringBuilder();
        result.append("HS|FN|" + getName() + cmdDelim);

        /*
         * Second, append misc. component information. Standard loop through all
         * weight classes and types.
         * 
         * Structure: CC|weight$type$components$producableunits|
         */
        for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {
            for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
                result.append(getHSPPChangeString(weight, type_id));
            }
        }

        /*
         * Third block - factories. Use AF| commands to add factories to each
         * type and weight class. Similar to component loop above, but factory
         * entries contain more information.
         * 
         * Loop through all worlds, check control, and send owned factories.
         * 
         * Structure: AF|weight$metatype$founder$planet$name$refreshtime$ID|
         */
        for (SPlanet currPlanet : getPlanets().values()) {

            // skip unowned & contested worlds
            if (!equals(currPlanet.getOwner())) {
                continue;
            }

            for (int i = 0; i < currPlanet.getUnitFactories().size(); i++) {
                SUnitFactory currFactory = (SUnitFactory) currPlanet.getUnitFactories().get(i);
                result.append("AF" + cmdDelim);// cmd header

                result.append(currFactory.getWeightclass() + internalDelim);
                result.append(currFactory.getType() + internalDelim);

                result.append(currFactory.getFounder() + internalDelim);
                result.append(currFactory.getPlanet().getName() + internalDelim);
                result.append(currFactory.getName() + internalDelim);
                result.append(currFactory.getTicksUntilRefresh() + internalDelim);
                result.append(currFactory.getAccessLevel() + internalDelim);
                result.append(currFactory.getID() + internalDelim);
                result.append(cmdDelim);
            }
        }

        /*
         * Fourth, and final, block - units in faction bays.
         */
        
        if (!(this.isNewbieHouse() && Boolean.parseBoolean(CampaignMain.cm.getConfig("HiddenBMUnits")))) {
			for (int type_id = 0; type_id < Unit.TOTALTYPES; type_id++) {

				for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {

					// skip units that are for sale. send all others.
					Vector<SUnit> unitSet = this.getHangar(type_id).elementAt(
							weight);
					for (SUnit currU : unitSet) {
						if (currU.getStatus() == Unit.STATUS_FORSALE) {
							continue;
						}
						result.append(getHSUnitAdditionString(currU));
					}
				}
			}
		}
		return result.toString();
    }

    /**
     * Construct a string to send to clients if unit is added. Format is:
     * AU|weight$type$chassis$model$damage|
     */
    public String getHSUnitAdditionString(SUnit u) {
        SerializedMessage result = new SerializedMessage("$");

        // header info
        result.append("AU|" + u.getWeightclass());
        result.append(u.getType());

        // unit information (note: no pilot info included)
        Entity currE = u.getEntity();
        result.append(u.getUnitFilename());
        result.append(u.getId());// ID used to remove units. Never shown to
        // players in GUI.

        if (!u.hasVacantPilot()) {
            result.append(u.getPilot().getGunnery());
            result.append(u.getPilot().getPiloting());
        } else {
            result.append(getBaseGunner(u.getType()));
            result.append(getBasePilot(u.getType()));
        }
        // if using AR, send damage information
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            result.append(UnitUtils.unitBattleDamage(currE, true));
        }

        // finalize and return
        return result.toString() + "|";
    }

    /**
     * Construct a string to send to clients if PP changes. Format is
     * CC|weight$type$components$producableunits|
     */
    public String getHSPPChangeString(int weight, int type_id) {

        StringBuilder result = new StringBuilder();

        int costPerUnit = Math.max(1, getPPCost(weight, type_id));
        int currentPP = getPP(weight, type_id);

        result.append("CC|");
        result.append(weight + "$" + type_id + "$");
        result.append(currentPP + "$" + (currentPP / costPerUnit));

        result.append("|");
        return result.toString();
    }

    /**
     * Construct a string to send to clients if unit is removed from a house.
     * Called by SHouse internally, but also outside of SHouse as as a follow-up
     * to SHouse.getEntity().
     */
    public String getHSUnitRemovalString(SUnit u) {

        StringBuilder result = new StringBuilder();

        // header info
        result.append("RU|");
        result.append(u.getWeightclass() + "$" + u.getType() + "$");
        result.append(u.getId());

        // fianlize and return
        result.append("|");
        return result.toString();
    }

    // Getter and Setter
    public int getMoney() {
        return Money;
    }

    public String getColoredName() {
        return "<font color=\"" + getHouseColor() + "\">" + getName() + "</font>";
    }

    public String getColoredNameAsLink() {
        return "<font color=\"" + getHouseColor() + "\">" + getNameAsLink() + "</font>";
    }

    public String getColoredAbbreviation(boolean includeBrackets) {
        String toReturn = "<font color=\"" + getHouseColor() + "\">";
        if (includeBrackets) {
            toReturn += "[";
        }
        toReturn += getAbbreviation();
        if (includeBrackets) {
            toReturn += "]";
        }
        return toReturn += "</font>";
    }

    public ConcurrentHashMap<String, SPlanet> getPlanets() {
        return Planets;
    }

    public void setMoney(int newMoney) {
        Money = newMoney;
    }

    public int getComponentsProduced(int unitType) {
        if (!unitComponents.containsKey(unitType)) {
            return 0;
        }
        // else
        int component = unitComponents.get(unitType);
        return component;
    }

    public int getShowProductionCountNext() {
        return showProductionCountNext;
    }

    /**
     * @return the small player hashtable
     */
    public Hashtable<String, SmallPlayer> getSmallPlayers() {
        synchronized (SmallPlayers) {
            return SmallPlayers;
        }
    }

    // Comparable
    public int compareTo(Object o) {
        SHouse h = (SHouse) o;
        if (getMoney() > h.getMoney()) {
            return 1;
        } else if (getMoney() < h.getMoney()) {
            return -1;
        }
        return getName().compareTo(h.getName());
    }

    public void addMoney(int amount) {
        setMoney(getMoney() + amount);
    }

    public void addComponentsProduced(int unitType, int amount) {
        setComponentsProduced(unitType, getComponentsProduced(unitType) + amount);
    }

    public void addShowProductionCountNext(int amount) {
        setShowProductionCountNext(getShowProductionCountNext() + amount);
    }

    /**
     * Returns all online players. Should be used sparingly.
     * 
     * TODO: Remove references to this method, where possible.
     */
    public Hashtable<String, SPlayer> getAllOnlinePlayers() {
        Hashtable<String, SPlayer> allPlayers = new Hashtable<String, SPlayer>();
        allPlayers.putAll(getReservePlayers());
        allPlayers.putAll(getActivePlayers());
        allPlayers.putAll(getFightingPlayers());
        return allPlayers;
    }

    /**
     * @param baysProvided
     *            - The baysProvided to set.
     */
    public void setBaysProvided(int baysProvided) {
        BaysProvided = baysProvided;
    }

    /**
     * @param componentProduction
     *            - The componentProduction to set.
     */
    public void setComponentProduction(int componentProduction) {
        ComponentProduction = componentProduction;
    }

    public void setComponentsProduced(int unitType, int components) {
        unitComponents.put(unitType, components);
    }

    public void setShowProductionCountNext(int i) {
        showProductionCountNext = i;
    }

    /**
     * @return Returns the initialHouseRanking.
     */
    public int getInitialHouseRanking() {
        return initialHouseRanking;
    }

    /**
     * @param initialHouseRanking
     *            - the initialHouseRanking to set.
     */
    public void setInitialHouseRanking(int initialHouseRanking) {
        this.initialHouseRanking = initialHouseRanking;
    }

    /**
     * @return Returns the MOTD.
     */
    public String getMotd() {
        return motd;
    }

    /**
     * @param motd
     *            - the MOTD to set.
     */
    public void setMotd(String motd) {
        this.motd = motd;
    }

    /**
     * @return Returns the inHouseAttacks.
     */
    public boolean isInHouseAttacks() {
        return inHouseAttacks;
    }

    /**
     * @param inHouseAttacks
     *            The inHouseAttacks to set.
     */
    public void setInHouseAttacks(boolean inHouseAttacks) {
        this.inHouseAttacks = inHouseAttacks;
    }

    public float getHighestUnitCost(int weight, int type) {
        float cost = 0;
        Vector<SUnit> s;

        try {
            s = this.getHangar(type).elementAt(weight);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            MWLogger.errLog("Empty Vector in getHighestUnitCost");
            return Float.MAX_VALUE;
        }
        if (s == null) {
            return Float.MAX_VALUE;
        }

        for (SUnit unit : s) {
            if (unit.getEntity().getCost(false) > cost) {
                cost = (float) unit.getEntity().getCost(false);
            }
        }

        return cost;

    }

    public Properties getConfig() {
        return config;
    }

    public boolean getBooleanConfig(String key) {
        try {
            return Boolean.parseBoolean(this.getConfig(key));
        } catch (Exception ex) {
            return false;
        }
    }

    public int getIntegerConfig(String key) {
        try {
            return Integer.parseInt(this.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public double getDoubleConfig(String key) {
        try {
            return Double.parseDouble(this.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public float getFloatConfig(String key) {
        try {
            return Float.parseFloat(this.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public float getLongConfig(String key) {
        try {
            return Long.parseLong(this.getConfig(key));
        } catch (Exception ex) {
            return -1;
        }
    }

    public String getConfig(String key) {

        if (config == null || config.getProperty(key) == null) {
            return CampaignMain.cm.getConfig(key);
        }
        return config.getProperty(key).trim();
    }

    public void saveConfigFile() {

        if (config == null) {
            return;
        }

        if (config.size() < 1) {
            config = null;
            return;
        }

        String fileName = "./data/" + getName().toLowerCase() + "_configs.dat";
        try {
            config.setProperty("TIMESTAMP", Long.toString((System.currentTimeMillis())));
            PrintStream ps = new PrintStream(new FileOutputStream(fileName));
            config.store(ps, "Faction Config");
            ps.close();
        } catch (FileNotFoundException fe) {
            MWLogger.errLog(fileName + " not found");
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }

    }

    public void loadConfigFile() {

        File configFile = new File("./data/" + getName().toLowerCase() + "_configs.dat");

        if (!configFile.exists()) {
            populateUnitLimits();
            populateBMLimits();
            return;
        }

        try {
            config = new Properties();
            config.load(new FileInputStream(configFile));
            populateUnitLimits();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
        populateUnitLimits();
        populateBMLimits();
    }

    /**
     * A method to fill the unitLimits array
     */
    public void populateUnitLimits() {
        unitLimits[Unit.MEK][Unit.LIGHT] = getIntegerConfig("MaxHangarLightMek");
        unitLimits[Unit.MEK][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumMek");
        unitLimits[Unit.MEK][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyMek");
        unitLimits[Unit.MEK][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultMek");

        unitLimits[Unit.VEHICLE][Unit.LIGHT] = getIntegerConfig("MaxHangarLightVehicle");
        unitLimits[Unit.VEHICLE][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumVehicle");
        unitLimits[Unit.VEHICLE][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyVehicle");
        unitLimits[Unit.VEHICLE][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultVehicle");

        unitLimits[Unit.INFANTRY][Unit.LIGHT] = getIntegerConfig("MaxHangarLightInfantry");
        unitLimits[Unit.INFANTRY][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumInfantry");
        unitLimits[Unit.INFANTRY][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyInfantry");
        unitLimits[Unit.INFANTRY][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultInfantry");

        unitLimits[Unit.BATTLEARMOR][Unit.LIGHT] = getIntegerConfig("MaxHangarLightBattleArmor");
        unitLimits[Unit.BATTLEARMOR][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumBattleArmor");
        unitLimits[Unit.BATTLEARMOR][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyBattleArmor");
        unitLimits[Unit.BATTLEARMOR][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultBattleArmor");

        unitLimits[Unit.PROTOMEK][Unit.LIGHT] = getIntegerConfig("MaxHangarLightProtoMek");
        unitLimits[Unit.PROTOMEK][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumProtoMek");
        unitLimits[Unit.PROTOMEK][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyProtoMek");
        unitLimits[Unit.PROTOMEK][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultProtoMek");

        unitLimits[Unit.AERO][Unit.LIGHT] = getIntegerConfig("MaxHangarLightAero");
        unitLimits[Unit.AERO][Unit.MEDIUM] = getIntegerConfig("MaxHangarMediumAero");
        unitLimits[Unit.AERO][Unit.HEAVY] = getIntegerConfig("MaxHangarHeavyAero");
        unitLimits[Unit.AERO][Unit.ASSAULT] = getIntegerConfig("MaxHangarAssaultAero");
    }

    /**
     * A method that returns the hangar limit for a given weight/type of unit
     * 
     * @param unitType
     * @param unitWeightClass
     * @return -1 if it is unlimited or a malformed request, the limit otherwise
     */
    public int getUnitLimit(int unitType, int unitWeightClass) {
        if (unitType < 0 || unitType > Unit.AERO) {
            MWLogger.errLog("Request for invalid unitType in SHouse.getUnitLimit: " + unitType);
            return -1;
        }
        if (unitWeightClass < 0 || unitWeightClass > Unit.ASSAULT) {
            MWLogger.errLog("Request for invalid unitWeightClass in SHouse.getUnitLimit: " + unitWeightClass);
            return -1;
        }
        return unitLimits[unitType][unitWeightClass];
    }

        
    public boolean canBuyFromBM(int unitType, int unitWeight) {
    	return bmLimits[unitType][unitWeight];
    }
    
    public void setCanBuyFromBM(int unitType, int unitWeight, boolean canBuy) {
    	bmLimits[unitType][unitWeight] = canBuy;
    }

    public void sendMessageToHouseLeaders(String msg) {

        if (leaders.size() < 1) {
            return;
        }

        for (String name : leaders) {
            CampaignMain.cm.toUser(msg, name);
        }
    }

    public void addLeader(String leader) {
        leaders.add(leader.toLowerCase());
    }

    public void removeLeader(String leader) {
        leaders.remove(leader.toLowerCase());
    }

    public boolean isLeader(String leader) {
        return leaders.contains(leader.toLowerCase());
    }

    public String getZeroLevelSubFaction() {

        if (getSubFactionList().size() < 1) {
            return "";
        }

        for (SubFaction subFac : getSubFactionList().values()) {
            if (Integer.parseInt(subFac.getConfig("AccessLevel")) == 0) {
                return subFac.getConfig("Name");
            }
        }

        return "";
    }

    public void addCommonUnitSupport() {
        parseSupportFile("./campaign/factions/support/common_meks.txt", true);
        parseSupportFile("./campaign/factions/support/common_vehicles.txt", true);
        parseSupportFile("./campaign/factions/support/common_infantry.txt", true);
        parseSupportFile("./campaign/factions/support/common_battlearmor.txt", true);
        parseSupportFile("./campaign/factions/support/common_protomeks.txt", true);
        parseSupportFile("./campaign/factions/support/common_aero.txt", true);
    }

    private void modifyUnitSupport(SPlanet p, boolean addProduction) {
        if (p.getFactoryCount() > 0) {
            for (int weightclass = Unit.LIGHT; weightclass <= Unit.ASSAULT; weightclass++) {
                for (SUnitFactory uf : p.getFactoriesOfWeighclass(weightclass)) {
                    String typeString = uf.getTypeString();
                    String dirName = "./campaign/factions/support/" + uf.getFounder() + "_" + uf.getSize() + "_";
                    dirName = dirName.toLowerCase();
                    if (typeString.contains("M")) {
                        parseSupportFile(dirName + "meks.txt", addProduction);
                    }
                    if (typeString.contains("V")) {
                        parseSupportFile(dirName + "vehicles.txt", addProduction);
                    }
                    if (typeString.contains("I")) {
                        parseSupportFile(dirName + "infantry.txt", addProduction);
                    }
                    if (typeString.contains("P")) {
                        parseSupportFile(dirName + "protomeks.txt", addProduction);
                    }
                    if (typeString.contains("B")) {
                        parseSupportFile(dirName + "battlearmor.txt", addProduction);
                    }
                    if (typeString.contains("A")) {
                        parseSupportFile(dirName + "aero.txt", addProduction);
                    }
                }
            }
        }
    }

    public String getAnnouncement() {
		return announcement;
	}

	public void setAnnouncement(String announcement) {
		this.announcement = announcement;
	}

	public void createNoneHouse() {
        setName("None");
        setId(-1);
        setConquerable(false);
        setHouseDefectionTo(false);
        setHouseDefectionFrom(false);
        setAbbreviation("None");
        setHouseColor(CampaignMain.cm.getConfig("DisputedPlanetColor"));
        setHousePlayerColors(CampaignMain.cm.getConfig("DisputedPlanetColor"));

        PKLogManager.getInstance().addLog(getName());
        // Vehicles = new Vector();

        for (int j = 0; j < 5; j++) // Type
        {
            Vector<Integer> v = new Vector<Integer>();
            for (int i = 0; i < 4; i++) // Weight
            {
                v.add(0);
            }
            v.trimToSize();
            getComponents().put(j, v);
        }
        // currentPP = new Vector();
        setMoney(0);
        getHangar().put(Unit.MEK, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.VEHICLE, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.INFANTRY, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.PROTOMEK, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.BATTLEARMOR, new Vector<Vector<SUnit>>(1, 1));
        getHangar().put(Unit.AERO, new Vector<Vector<SUnit>>(1, 1));
        for (int i = 0; i < 4; i++) {
            getHangar(Unit.MEK).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.VEHICLE).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.INFANTRY).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.PROTOMEK).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.BATTLEARMOR).add(new Vector<SUnit>(1, 1));
            getHangar(Unit.AERO).add(new Vector<SUnit>(1, 1));
        }

        // init the componet array(vectors)
        getComponents().put(Unit.MEK, new Vector<Integer>(4, 1));
        getComponents().put(Unit.VEHICLE, new Vector<Integer>(4, 1));
        getComponents().put(Unit.INFANTRY, new Vector<Integer>(4, 1));
        getComponents().put(Unit.BATTLEARMOR, new Vector<Integer>(4, 1));
        getComponents().put(Unit.PROTOMEK, new Vector<Integer>(4, 1));
        getComponents().put(Unit.AERO, new Vector<Integer>(4, 1));

        for (int i = 0; i < 4; i++) {
            getComponents().get(Unit.MEK).add(0);
            getComponents().get(Unit.VEHICLE).add(0);
            getComponents().get(Unit.INFANTRY).add(0);
            getComponents().get(Unit.BATTLEARMOR).add(0);
            getComponents().get(Unit.PROTOMEK).add(0);
            getComponents().get(Unit.AERO).add(0);
        }
        updated();
    }

    public void addTechResearchPoint(int points) {
        setTechResearchPoints(points + getTechResearchPoints());
    }

    public void setTechResearchPoints(int points) {
        techResearchPoints = points;
    }

    public int getTechResearchPoints() {
        return techResearchPoints;
    }

    public int getTechResearchLevel() {
        return this.getTechResearchLevel(getTechLevel());
    }

    public int getTechResearchLevel(int tech) {

        int techLevel = 1;
        switch (tech) {
        case TechConstants.T_INTRO_BOXSET:
        case TechConstants.T_TW_ALL:
        case TechConstants.T_IS_TW_NON_BOX:
            techLevel = 1;
            break;
        case TechConstants.T_IS_ADVANCED:
            techLevel = 2;
            break;
        case TechConstants.T_IS_EXPERIMENTAL:
            techLevel = 3;
            break;
        case TechConstants.T_IS_UNOFFICIAL:
            techLevel = 4;
            break;
        case TechConstants.T_CLAN_TW:
            techLevel = 5;
            break;
        case TechConstants.T_CLAN_ADVANCED:
            techLevel = 6;
            break;
        case TechConstants.T_CLAN_EXPERIMENTAL:
            techLevel = 7;
            break;
        case TechConstants.T_CLAN_UNOFFICIAL:
            techLevel = 8;
            break;
        case TechConstants.T_ALL:
        case TechConstants.T_ALLOWED_ALL:
            techLevel = 9;
            break;
        default:
            techLevel = 1;
        }

        return techLevel;
    }

    public void updateHouseTechLevel() {
        switch (getTechResearchLevel()) {
        case 1:
            setTechLevel(TechConstants.T_IS_TW_ALL);
            break;
        case 2:
            setTechLevel(TechConstants.T_IS_ADVANCED);
            break;
        case 3:
            setTechLevel(TechConstants.T_IS_EXPERIMENTAL);
            break;
        case 4:
            setTechLevel(TechConstants.T_IS_UNOFFICIAL);
            break;
        case 5:
            setTechLevel(TechConstants.T_CLAN_TW);
            break;
        case 6:
            setTechLevel(TechConstants.T_CLAN_ADVANCED);
            break;
        case 7:
            setTechLevel(TechConstants.T_CLAN_EXPERIMENTAL);
            break;
        case 8:
            setTechLevel(TechConstants.T_CLAN_UNOFFICIAL);
            break;
        case 9:
            setTechLevel(TechConstants.T_ALL);
            break;
        default:
            setTechLevel(TechConstants.T_IS_TW_ALL);
            break;
        }
        techResearchPoints = 0;
    }

    public UnitComponents getUnitParts() {
        return unitParts;
    }

    public void updatePartsCache(String part, int amount) {
        if (amount < 0) {
            getUnitParts().remove(part, amount);
        } else {
            getUnitParts().add(part, amount);
        }
    }

    public int getPartsAmount(String part) {
        int amount = 0;
        amount += getUnitParts().getPartsCritCount(part);
        return amount;
    }

	public void addComponentConverter(ComponentToCritsConverter converter) {
        componentConverter.put(converter.getCritName(), converter);
    }

    public Hashtable<String, ComponentToCritsConverter> getComponentConverter() {
        return componentConverter;
    }

    private void produceCrits() {
    	
        if (!CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            return;
        }
        int year = CampaignMain.cm.getIntegerConfig("CampaignYear");
        boolean cacheUpdate = false;

        double baseCost = CampaignMain.cm.getDoubleConfig("BaseComponentToMoneyRatio");

        if (getComponentConverter().containsKey("All")) {
            ComponentToCritsConverter converter = getComponentConverter().get("All");
            int minCrits = converter.getMinCritLevel();
            baseCost *= CampaignMain.cm.getDoubleConfig("ComponentToPartsModifier" + SUnit.getTypeClassDesc(converter.getComponentUsedType()));
            baseCost *= CampaignMain.cm.getDoubleConfig("ComponentToPartsModifier" + SUnit.getWeightClassDesc(converter.getComponentUsedWeight()));

            for (BMEquipment eq : CampaignMain.cm.getPartsMarket().getEquipmentList().values()) {

                // do not produce something that the is allowed in the BM
                if (eq.getCost() <= 0) {
                    continue;
                }

                // do bother producing what you cannot use.
                if (!CampaignMain.cm.getBooleanConfig("AllowCrossOverTech")) {
                    eq.getTech(year);
                    if (eq.getTechLevel() != TechConstants.T_ALL && eq.getTechLevel() > getTechLevel()) {
                        continue;
                    }
                }

                if (getPartsAmount(eq.getEquipmentInternalName()) < minCrits) {
                    int critsToAdd = minCrits - getPartsAmount(eq.getEquipmentInternalName());

                    double costInComponents = eq.getCost() / baseCost;
                    double components = critsToAdd * costInComponents;

                    components = Math.ceil(components);

                    components = Math.max(1, components);

                    if (getComponents().get(converter.getComponentUsedType()).get(converter.getComponentUsedWeight()) < components) {
                        continue;
                    }

                    addPP(converter.getComponentUsedWeight(), converter.getComponentUsedType(), (int) -components, false);
                    updatePartsCache(eq.getEquipmentInternalName(), critsToAdd);
                    cacheUpdate = true;
                }
            }
        } else {

            for (ComponentToCritsConverter converter : getComponentConverter().values()) {

                int minCrits = converter.getMinCritLevel();
                baseCost = CampaignMain.cm.getDoubleConfig("BaseComponentToMoneyRatio");
                baseCost *= CampaignMain.cm.getDoubleConfig("ComponentToPartsModifier" + SUnit.getTypeClassDesc(converter.getComponentUsedType()));
                baseCost *= CampaignMain.cm.getDoubleConfig("ComponentToPartsModifier" + SUnit.getWeightClassDesc(converter.getComponentUsedWeight()));

                BMEquipment eq = CampaignMain.cm.getPartsMarket().getEquipmentList().get(converter.getCritName());

                if (eq == null) {
                    continue;
                }

                // do not produce something that the is allowed in the BM
                if (eq.getCost() <= 0) {
                    continue;
                }
                // do bother producing what you cannot use.
                if (!CampaignMain.cm.getBooleanConfig("AllowCrossOverTech")) {
                    eq.getTech(year);
                    if (eq.getTechLevel() != TechConstants.T_ALL && eq.getTechLevel() > getTechLevel()) {
                        continue;
                    }
                }

                if (getPartsAmount(eq.getEquipmentInternalName()) < minCrits) {
                    int critsToAdd = minCrits - getPartsAmount(eq.getEquipmentInternalName());

                    double costInComponents = eq.getCost() / baseCost;
                    double components = critsToAdd * costInComponents;

                    components = Math.ceil(components);

                    components = Math.max(1, components);

                    if (getComponents().get(converter.getComponentUsedType()).get(converter.getComponentUsedWeight()) < components) {
                        continue;
                    }

                    addPP(converter.getComponentUsedWeight(), converter.getComponentUsedType(), (int) -components, false);
                    updatePartsCache(eq.getEquipmentInternalName(), critsToAdd);
                    cacheUpdate = true;
                }
            }
        }
        if (cacheUpdate) {
            CampaignMain.cm.doSendHouseMail(this, "NOTE", "The house crits have been updated");
            CampaignMain.cm.doSendToAllOnlinePlayers(this, getCompleteStatus(), false);
        }

    }
    
    public void populateBMLimits() {
    	bmLimits[Unit.MEK][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightMeks");
    	bmLimits[Unit.MEK][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumMeks");
    	bmLimits[Unit.MEK][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyMeks");
    	bmLimits[Unit.MEK][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultMeks");

    	bmLimits[Unit.VEHICLE][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightVehicles");
    	bmLimits[Unit.VEHICLE][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumVehicles");
    	bmLimits[Unit.VEHICLE][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyVehicles");
    	bmLimits[Unit.VEHICLE][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultVehicles");

    	bmLimits[Unit.INFANTRY][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightInfantry");
    	bmLimits[Unit.INFANTRY][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumInfantry");
    	bmLimits[Unit.INFANTRY][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyInfantry");
    	bmLimits[Unit.INFANTRY][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultInfantry");

    	bmLimits[Unit.BATTLEARMOR][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightBA");
    	bmLimits[Unit.BATTLEARMOR][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumBA");
    	bmLimits[Unit.BATTLEARMOR][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyBA");
    	bmLimits[Unit.BATTLEARMOR][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultBA");

    	bmLimits[Unit.PROTOMEK][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightProtomeks");
    	bmLimits[Unit.PROTOMEK][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumProtomeks");
    	bmLimits[Unit.PROTOMEK][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyProtomeks");
    	bmLimits[Unit.PROTOMEK][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultProtomeks");

    	bmLimits[Unit.AERO][Unit.LIGHT] = getBooleanConfig("CanBuyBMLightAero");
    	bmLimits[Unit.AERO][Unit.MEDIUM] = getBooleanConfig("CanBuyBMMediumAero");
    	bmLimits[Unit.AERO][Unit.HEAVY] = getBooleanConfig("CanBuyBMHeavyAero");
    	bmLimits[Unit.AERO][Unit.ASSAULT] = getBooleanConfig("CanBuyBMAssaultAero");

    }
    
}// end SHouse.java
