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


import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Player;
import common.SubFaction;
import common.Unit;
import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.flags.PlayerFlags;
import common.util.MWLogger;
import common.util.TokenReader;
import common.util.UnitComponents;
import common.util.UnitUtils;
import megamek.common.Protomech;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.market2.IBuyer;
import server.campaign.market2.ISeller;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.mercenaries.MercHouse;
import server.campaign.pilot.SPilot;
import server.campaign.util.ExclusionList;
import server.campaign.util.OpponentListHelper;
import server.campaign.util.SerializedMessage;
import server.campaign.util.scheduler.UserActivityComponentsJob;
import server.campaign.util.scheduler.UserActivityInfluenceJob;
import server.util.MWPasswdRecord;
import server.util.QuirkHandler;



//import org.json.simple.JSONObject;

/**
 * A class representing a Player DOCU is not finished
 *
 * @author Helge Richter (McWizard)
 * @author Bob Eldred (Spork)
 * @version 2016.10.06
 *
 * Modifications:
 * - Moved slice flu generation to a Quartz task
 */

public final class SPlayer extends Player implements Comparable<Object>, IBuyer, ISeller {

    // STATIC VARIABLES
    // STATUS_DISCONNECTED, which is used by the client, is 0
    public static final int STATUS_LOGGEDOUT = 1;
    public static final int STATUS_RESERVE = 2;
    public static final int STATUS_ACTIVE = 3;
    public static final int STATUS_FIGHTING = 4;

    //@salient Mini Campaign Phases
    private static final String RESTOCK_MC = "restockmc"; //@salient for minicampaigns
    private static final String ACTIVE_MC = "activemc"; //@salient for minicampaigns

    // DATA VARIABLES (SAVED. Most have gets and sets.)
    private String name = "";
    private String fluffText = "";
    private String myLogo = "";
    private String lastISP = "";

    private int money = 0;
    private int experience = 0;
    private int influence = 0; //@salient - changed from 50 to 0, starting flu can be set in SO faction.
    private int rewardPoints = 0; // number of rewards a player has. //@salient changed name to rewardPoints
    private int xpTillReward = 0; // counter until next RP injection triggered by XP gains, see XPRollOverCap in server options
    private int xpTillFlu = 0; // @ Salient , same as above. counter until next flu injection triggered by XP gains.
    private int groupAllowance = 0;
    private int technicians = 0;// @urgru 7/17/04
    private int baysOwned = 0;
    private int currentTechPayment = -1;// num Cbills owed to techs after game

    private double rating = 1600;

    private long lastOnline = 0;

    private Vector<SUnit> units = new Vector<SUnit>(1, 1);
    private Vector<SArmy> armies = new Vector<SArmy>(1, 1);
    private Vector<Integer> totalTechs = new Vector<Integer>(4, 1);
    private Vector<Integer> availableTechs = new Vector<Integer>(4, 1);

    private SPersonalPilotQueues personalPilotQueue = new SPersonalPilotQueues();
    private ExclusionList exclusionList = new ExclusionList();

    // SEMI-PERMANENT VARIABLES. Not saved to String.

    //@salient , I foresee mini campaigns becoming ever more complex
    //this section will contain strings to be saved together as a
    //serialized message embedded into the player save.
    private String phaseMC = ACTIVE_MC;
    //Same goes for discord Info for use by bot
    private String discordID = ""; //@salient will be set by DiscordInfo


    private int scrapsThisTick = 0;
    private int donationsThisTick = 0;

    private double weightedArmyNumber = -1;

    private long lastTimeCommandSent = 0;
    private long lastAttackFromReserve = 0;
    private long activeSince = 0;
    private long attackRestrictionUntil = 0;

    private String sellingto = "";
    private String lastSentStatus = "";
    private String clientVersion = "";// version gets sent by the player and
    // set

    private SHouse myHouse;
    private MWPasswdRecord password = null;

    private UnitComponents unitParts = new UnitComponents();

    private int DBId = 0;
    private int forumID = 0;
    private boolean userValidated = false;

    boolean isLoading = false; // Player was getting saved multiple times
    // during loading. Just seemed silly. Adding this
    // back in, as saving during load is causing DB
    // issues.

    private String subFaction = "";

    private long lastPromoted = 0;

    public volatile int leechCount = 0;

    // CONSTRUCTORS
    /**
     * Stock constructor. Note that an SPlayer is data-less unless/until
     * fromString() or some sets are called. SPlayers are created in only two
     * places - CampaignMain's load method and the EnrollCommand.
     */
    public SPlayer() {

        // if using advanced repair, populate tech vectors and generate info
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            for (int x = 0; x < 4; x++) {
                getAvailableTechs().add(0);
                getTotalTechs().add(0);
            }
        }
        myHouse = CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"));

    }

    /**
     * Save player file immediatly.
     */
    public void setSave() {
        if (!isLoading) {
            CampaignMain.cm.forceSavePlayer(this);
        }
    }

    // PUBLIC METHODS
    /**
     * Override the standard Object.equals(), compare two instances of a player
     * by name only.
     */
    @Override
    public boolean equals(Object o) {

        SPlayer p = null;
        try {
            p = (SPlayer) o;
        } catch (ClassCastException e) {
            return false;
        }

        if (p == null) {
            return false;
        }

        if (p.getName().equals(name)) {
            return true;
        }

        // else
        return false;
    }

    /**
     * A Method that returns a rounded ELO rating for this player. Used to send
     * truncated doubles to the userlist.
     *
     * @return the rounded rating
     */
    public double getRatingRounded() {
        BigDecimal bd = new BigDecimal(rating);
        bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    /**
     * Determine whether or not a player can use a unit of a given weight class.
     * This is used to prevent new players from buying heavier/larger units and
     * sucking a house dry.
     *
     * @param - weight class to check.
     */
    public boolean mayUse(int weightClass) {
        //@Salient adding this in for Gunny
        if (weightClass == Unit.LIGHT) {
            if (Integer.parseInt(getMyHouse().getConfig("MinEXPforLight")) > experience) {
                return false;
            }
        }
        if (weightClass == Unit.MEDIUM) {
            if (Integer.parseInt(getMyHouse().getConfig("MinEXPforMedium")) > experience) {
                return false;
            }
        }
        if (weightClass == Unit.HEAVY) {
            if (Integer.parseInt(getMyHouse().getConfig("MinEXPforHeavy")) > experience) {
                return false;
            }
        }
        if (weightClass == Unit.ASSAULT) {
            if (Integer.parseInt(getMyHouse().getConfig("MinEXPforAssault")) > experience) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add a unit to the player. Pass-though to addUnit(SUnit,boolean,boolean).
     * This version should be called in almost all situations.
     */
    public void addUnit(SUnit m, boolean isNew) 
    {
			this.addUnit(m, isNew, true);
    }

    /**
     * Add a unit to the player. If the unit is new, make it immune to
     * maintenance scraps. Nearly all calls should send updates to a client;
     * however, in some instances (ex: when giving units to a SOL player),
     * bandwidth is saved by doing a single PS| at the end of a series of adds.
     */
    public String addUnit(SUnit m, boolean isNew, boolean sendUpdates) {

        if (isNew) {
            long immunityTime = Long.parseLong(getMyHouse().getConfig("ImmunityTime")) * 1000;
            m.setPassesMaintainanceUntil(System.currentTimeMillis() + immunityTime * 2);
        }

        // clear any scrap allowance
        m.setScrappableFor(-1);

        /*
         * OK if there's room, unmaintained if not. This also strips any
         * FOR_SALE from units purchased via the market.
         */
        if (getFreeBays() < (CampaignMain.cm.isUsingIncreasedTechs() ? SUnit.getHangarSpaceRequired(m, getMyHouse().houseSupportsUnit(m.getUnitFilename()), getMyHouse()) : SUnit.getHangarSpaceRequired(m, getMyHouse()))) {
            m.setUnmaintainedStatus();
        } else {
            m.setStatus(Unit.STATUS_OK);
        }

        // strip illegal ammo
        SUnit.checkAmmoForUnit(m, myHouse);
        
        //@salient add quirks to entity
        //as seen recently, an improperly setup quirks xml can cause a npe here.
        //so i'll add a try/catch
        try {
        	QuirkHandler.getInstance().setQuirks(m);        				
		} catch (Exception e) {
			MWLogger.errLog(e);
			MWLogger.errLog(m.getUnitFilename() + " " + m.getVerboseModelName() 
				+ " quirk error, check the XML files for this unit, likely xml error" );
		}


        m.setPosId(getFreeID());
        synchronized(units) 
        {   
        	units.add(m);
        }

        /*
         * Send PL|HD. Client-side reading of HD adds units to the hangar
         * instead of clearing/replacing the hangar, so we can send just this
         * one, if we like. Send status update to the client (status determined
         * above), along with total and free bay/tech info.
         */
        if (sendUpdates) {
            CampaignMain.cm.toUser("PL|HD|" + m.toString(true), name, false);
            CampaignMain.cm.toUser("PL|SUS|" + m.getId() + "#" + m.getStatus(), name, false);
            CampaignMain.cm.toUser("PL|SB|" + getTotalMekBays(), name, false);
            CampaignMain.cm.toUser("PL|SF|" + getFreeBays(), name, false);
        }

        // make sure to save the player, with his fancy new unit ...
        setSave();

        String penaltyString = buildHangarPenaltyString();
        CampaignMain.cm.toUser("PL|SHP|" + penaltyString, name, false);

        //MWLogger.debugLog("Checking Anti-Air");
        //m.isAntiAir();

        return "";// dummy string returned to comply with IBuyer
    }

    /**
     * Return an SUnit with a given unique ID. If the player doesn't own the
     * unit, return a null.
     *
     * @param int - id the the unit to return
     * @return the desired unit, or null.
     */
    public SUnit getUnit(int id) {

        for (SUnit currU : units) {
            if (currU.getId() == id) {
                return currU;
            }
        }

        return null;
    }

    /**
     * ISeller-compliant .removeUnit(). Simply get the unit ID and pass to
     * normal SPlayer.removeUnit(int,bool). Use the (int,boolean) version of
     * remove unit whenever possible in order to intelligently pass select the
     * army update option. ISeller assumes true and sends updates to all armies.
     *
     * @urgru 1.2.06
     */
    public String removeUnit(SUnit unitToRemove, boolean sendHouseStatusUpdate) {
        this.removeUnit(unitToRemove.getId(), true);
        String penaltyString = buildHangarPenaltyString();
        CampaignMain.cm.toUser("PL|SHP|" + penaltyString, name, false);
        return "";// dummy stirng returned for IBuyer
    }

    /**
     * Remove the Unit with ID unitid from the player. Ops are checked by
     * discrete commands (ie - SellUnit), unchecked by large blocks of code
     * which force a check on their own (ie - ShortResolver).
     *
     * @param unitid
     *            the ID of the unit to remove
     */
    public void removeUnit(int unitid, boolean sendArmyUpdate) {
        SUnit Mech = null;
        synchronized (units) {
        	for (int i = 0; i < units.size(); i++) {
        		Mech = units.elementAt(i);
        		if (Mech.getId() == unitid) {
        			units.removeElementAt(i);
        		}
            }
        }

        for (SArmy currA : getArmies()) {
            if (currA.getUnitPosition(unitid) > -1) {
                currA.removeUnit(unitid);
                if (sendArmyUpdate) {
                    CampaignMain.cm.toUser("PL|SAD|" + currA.toString(true, "%"), name, false);
                    CampaignMain.cm.getOpsManager().checkOperations(currA, true);// update
                    // legal
                    // ops
                }
            }
        }// end for(all armies)

        CampaignMain.cm.toUser("PL|RU|" + unitid, name, false);
        CampaignMain.cm.toUser("PL|SB|" + getTotalMekBays(), name, false);
        CampaignMain.cm.toUser("PL|SF|" + getFreeBays(), name, false);
        setSave();// save on remove (adminstrip, etc)
    }

    /**
     * Method which determines the number ot free bays/techs a player has.
     * Simple loop through the hangar.
     *
     * @return number of free bays/techs
     */
    public int getFreeBays() {

        int free = getTotalMekBays();
        int totalProtos = 0;
        boolean advanceRep = CampaignMain.cm.isUsingAdvanceRepair();

        /*
         * Loop through all units. Those with STATUS_OK and STATUS_FORSALE take
         * up space. Units with STATUS_UNMAINTAINED and STATUS_DESTROYED don't
         * require techs. Protos get special point-based handling. They're
         * counted and passed off to this.getTechRequiredForProtos(), which
         * determines exactly how many techs are needed for any ProtoMek
         * grouping.  Christmas gifts are excluded from cost.
         */
        for (SUnit currU : units) {

            if (((currU.getStatus() == Unit.STATUS_OK) || (currU.getStatus() == Unit.STATUS_FORSALE)) && (!currU.isChristmasUnit())) {
                if (CampaignMain.cm.isUsingIncreasedTechs()) {
                    free -= SUnit.getHangarSpaceRequired(currU, getMyHouse().houseSupportsUnit(currU.getUnitFilename()), getMyHouse());
                } else {
                    free -= SUnit.getHangarSpaceRequired(currU, getMyHouse());
                }

                // proto counting
                if ((currU.getEntity() instanceof Protomech) && !advanceRep) {
                    if (!currU.getPilot().getSkills().has(PilotSkill.AstechSkillID)) {
                        totalProtos++;
                    }
                } else if (currU.getEntity() instanceof Protomech) {
                    totalProtos++;
                }

            }
        }// end while(more unit data)

        /*
         * Adjust for proto points.
         */
        if (totalProtos > 0) {
            int techRatio = Integer.parseInt(CampaignMain.cm.getConfig("TechsToProtoPointRatio"));
            double ppoints = totalProtos / 5.0;// 5 protos in a point
            int ptechs = (int) (ppoints * techRatio);

            if (ptechs < 1) {
                ptechs = 1;
            }
            free -= ptechs;
        }

        return free;
    }

    /**
     * This can be calcualted in one of three "standard" ways: 1) House bays +
     * techs 2) House bays + experience 3) House bays + techs + experience Or,
     * two additional ways if using Advanced Repair: 4) House Bays + bays owned
     * by player 5) House bays + bays owned by player + experience
     *
     * @return the total amount of bays this player has
     */
    public int getTotalMekBays() {// return bay/support number
        int numBays = 0;// amount to return

        boolean usesXP = Boolean.parseBoolean(getMyHouse().getConfig("UseExperience"));
        boolean usesTechs = Boolean.parseBoolean(getMyHouse().getConfig("UseTechnicians"));
        boolean usesAdvanceRepairs = CampaignMain.cm.isUsingAdvanceRepair();

        if (usesAdvanceRepairs) {
            usesTechs = false;
        }

        // include the basic bays. flat amount for mercs/SOL, warehouse # for
        // GreatHouses
        int BASE_BAYS = myHouse.getBaysProvided();
        numBays += BASE_BAYS;

        /*
         * Make sure all non-merc players meet a minimum free bay standard.
         * Useful for small factions on large servers (Marians, etc) and
         * factions which lose a large number of their warehouse worlds,
         * dropping fresh-from-SOL players to an unacceptably low # of bays.
         * Don't give these to mercenaries.
         */
        if (!myHouse.isMercHouse()) {
            int minBays = Integer.parseInt(getMyHouse().getConfig("MinimumHouseBays"));
            if (numBays < minBays) {
                numBays = minBays;
            }
        }// end if(non-merc)

        // then add the bays from XP, if the config says to...
        if (usesXP) {
            int experienceForBay = Integer.parseInt(getMyHouse().getConfig("ExperienceForBay"));
            // check for stupid settings to avoid division by 0
            if (experienceForBay != 0) {
                int maxBaysFromXP = Integer.parseInt(getMyHouse().getConfig("MaxBaysFromEXP"));
                int expBays = (experience / experienceForBay);
                if (expBays > maxBaysFromXP) {
                    expBays = maxBaysFromXP;
                }
                numBays += expBays;
            } else {
                MWLogger.errLog("0 is invalid setting for EXP for Bay Setting when using xp for bays!");
            }
        }

        // and now add the bays from techs if config'ed...
        if (usesTechs) {
            numBays += getTechnicians();
        }

        // now add bays if you are using advanced repairs
        if (usesAdvanceRepairs) {
            numBays += baysOwned;
        }

        return numBays;
    }// end TotalMechBays()

    /**
     * This method does all the math to figure out how much the retainer fee,
     * maintenance cost, whathaveyou is for the current number of technicians.
     * The number itself is useful in some cases (let people know what they will
     * have to pay after hiring a new tech, for example), and thus separated
     * from the actual payment. For now, we have only one payment calculation
     * mechanism -- additive costing, whereby each tech costs as much as the
     * last, plus a constant kicker. A cap to this cost can be configured;
     * however, it must be a multiple of the per-tech additive (eg, if the
     * additive is .04, 1.20 would be a valid cap, but 1.30 wouldn't).
     *
     * @urgru 7/26/04
     */
    private void doPayTechniciansMath() {

        int techs = getTechnicians();

        // don't even waste time on 0 cases. Just return.
        if (techs <= 0) {
            setCurrentTechPayment(0);
            return;
        }

        // starts as a double, gets cast back to an int for return.
        float amountToPay = 0;

        // load config variables needed to do the math ...
        float additive = Float.parseFloat(getMyHouse().getConfig("AdditivePerTech"));
        float ceiling = Float.parseFloat(getMyHouse().getConfig("AdditiveCostCeiling"));

        /*
         * divide the ceiling by the addiive. techs past this number are all
         * charged at the ceiling rate. Example: (With 1.20 and .04, the result
         * is 30. Every additional tech (31, 32, etc.) is paid at the ceiling
         * wage.
         */
        int techCeiling = (int) (ceiling / additive);
        if (techs > techCeiling) {
            int techsPastCeiling = techs - techCeiling;
            amountToPay += ceiling * techsPastCeiling;
        }// end if(some techs are paid @ ceiling price)

        /*
         * Add up the number of times the non-ceiling techs were incremented,
         * then figure out their total cost. In cases where the ceiling is
         * passed, the flat fee techs are handled above, so only techs up to
         * that ceiling need to have the additive math done. If the ceiling isnt
         * reached, just use the number of techToPay from the param.
         */
        int techsUsingAdditive = 0;
        if (techs > techCeiling) {
            techsUsingAdditive = techCeiling;
        } else {
            techsUsingAdditive = techs;
        }

        /*
         * Faster to just to a for loop to determine the number of times the
         * additive was made (1 + 2 + 3 + 4, and so on) with ints, and THEN
         * multiply by the double additive than do alot of floating point math
         * by for-in through and multiplying by the additive each time.
         */
        int totalAdditions = 0;
        for (int i = 1; i <= techsUsingAdditive; i++) {
            totalAdditions += i;
        }

        // now figure out the final amount to pay ...
        amountToPay += totalAdditions * additive;

        // Add penalty if the player is over a sliding limit

        for(int type_id = Unit.MEK; type_id < Unit.MAXBUILD; type_id ++) {
            for (int weightclass = Unit.LIGHT; weightclass <= Unit.ASSAULT; weightclass++) {
                if (hasHangarPenalty(type_id, weightclass)) {
                    int costPenalty = calculateHangarPenalty(type_id, weightclass);
                    amountToPay += costPenalty;
                }
            }
        }
        /*
         * now return the amount in INT form since we don't support fractional
         * money. also, set the currentTechPayment, to avoid doing this math
         * again if possible.
         */
        int toSet = Math.round(amountToPay);
        if (toSet < 0) {
            toSet = 0;
        }// don't pay players to add techs.

        setCurrentTechPayment(toSet);

    }// end doPayTechnicians(arbitrary number)

    /**
     * Should be called only after an attempt to pay techs comes up short. At
     * present, only used by ShortResolver. Other times techs are paid (eg -
     * TransferCommand) shortfalls stop the player from acting. Does all the
     * dirty work of lowering the number of technicians and setting units as
     * unmaintained.
     *
     * @param amountofShortFall
     *            - the amount owed to techs which can't be paid. used to
     *            determine how many walk off / quit.
     * @return numLost - the number of techs or bays lost.
     */
    public int doFireUnpaidTechnicians(float amountOfShortFall) {

        // String toReturn = "";

        // layoffs all around! well, at least some. so reset the
        // currentTechPayment
        setCurrentTechPayment(-1);

        // load config variables needed to do the calculations
        float additive = Float.parseFloat(getMyHouse().getConfig("AdditivePerTech"));
        float ceiling = Float.parseFloat(getMyHouse().getConfig("AdditiveCostCeiling"));

        int currentTechs = getTechnicians();// current number of techs
        int techCeiling = (int) (ceiling / additive);// the ceiling

        /*
         * Start by getting rid of the most expensive techs (those at the
         * ceiling). Loop until the player is able to afford the bill, or all
         * techs above the ceiling have been dismissed.
         */
        while ((amountOfShortFall > 0) && (currentTechs > techCeiling)) {
            currentTechs = currentTechs - 1;
            amountOfShortFall -= ceiling;
        }

        /*
         * Now start getting rid of the less expensive techs. Each tech costs
         * his # times his additive amount. Loop until theyre all gone, or the
         * bill can be paid.
         */
        while (amountOfShortFall > 0) {

            // fire a tech and reduce shortfall by his cost
            float costOfCurrentTech = currentTechs * additive;
            currentTechs = currentTechs - 1;
            amountOfShortFall -= costOfCurrentTech;

            // catch zero techs, just in case there IS rounding funkiness
            if (currentTechs == 0) {
                amountOfShortFall = 0;
            }

        }

        int numberOfTechsFired = getTechnicians() - currentTechs;
        addTechnicians(-numberOfTechsFired);

        return numberOfTechsFired;
    }

    /**
     * Method that returns the current cost of hiring a new technician, after
     * adjustment for XP, etc. Used by HireTechsCommand, Requests and
     * SetMaintainedCommand.
     */
    public int getTechHiringFee() {
        // get the starting tech cost
        int techCost = Integer.parseInt(CampaignMain.cm.getConfig("BaseTechCost"));

        /*
         * Check to see if tech hiring costs should be decreased with
         * experience. If they should be, load the amount of XP for each
         * reduction, and the pricing floor. Loop through the XP amount reducing
         * cost until the floor is reached, or there isnt enough XP to reduce
         * price further.
         */
        boolean decreaseWithXP = Boolean.parseBoolean(getMyHouse().getConfig("DecreasingTechCost"));
        if (decreaseWithXP) {
            // if it decreases, see how much
            int xpToDecrease = Integer.parseInt(getMyHouse().getConfig("XPForDecrease"));
            int minTechCost = Integer.parseInt(getMyHouse().getConfig("MinimumTechCost"));

            int numDecreases = (int) Math.floor(experience / xpToDecrease);
            techCost -= numDecreases;

            if (techCost < minTechCost) {
                techCost = minTechCost;
            }

            // catch error, in case server is misconfigured
            if (techCost < 0) {
                techCost = 0;
            }

        }
        return techCost;
    }

    /**
     * A method which is called to randomly set some units as unmaintained when
     * support levels go negative. Continues until support number is positive
     * again, or all units are unsupported (catches odd problems with units on
     * the black market -- not an expecially graceful solution; however, the
     * alternative is allowing units on the BM to be scrapped mid-auction).
     *
     * @urgru 8/2/04
     */
    public int setRandomUnmaintained() {

        // holder.
        int numUnmaintained = 0;

        // filter out units which are already unmaintained, for_sale or
        // destroyed
        Vector<SUnit> okUnitsData = new Vector<SUnit>(1, 1);
        for (SUnit currU : units) {
            if (currU.getStatus() == Unit.STATUS_OK) {
                okUnitsData.add(currU);
            }
        }

        while (getFreeBays() < 0) {

            if (okUnitsData.size() == 0) {
                return numUnmaintained;
            }

            // passed the catch. unmaintain some units.
            int rnd = CampaignMain.cm.getRandomNumber(okUnitsData.size());// generate
            // a
            // RND
            SUnit unit = okUnitsData.elementAt(rnd);// get unit @ rnd location
            unit.setUnmaintainedStatus();// make it unmaintained
            numUnmaintained++;
            CampaignMain.cm.toUser("PL|UU|" + unit.getId() + "|" + unit.toString(true), name, false);
            okUnitsData.remove(rnd);// and remove it from the vector

        }// end while(no free bays)

        setSave();
        return numUnmaintained;

    }// end setRandomUnmaintained

    /**
     * Loop through the units and perform maintainance. Check status and adjust
     * maintainance level accordingly. This is called during slices. Check to
     * ses if units are maintained -- if so, improve maintainance levels. If
     * not, roll a random. If its greater than the maintainance level, scrap the
     * unit. If unit should be scrapped, or just have its mainainance level
     * reduced. Note that units on the BM arent included in the maintainance
     * loop. It should be impossible to add an unmaintained unit to the BM, but
     * just in case, they're excluded (STATUS_FORSALE is ignored). This prevents
     * off BM nulls.
     */
    public void doMaintainance() {

        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            return;
        }
        int increase = Integer.parseInt(getMyHouse().getConfig("MaintainanceIncrease"));
        int decrease = Integer.parseInt(getMyHouse().getConfig("MaintainanceDecrease"));

        ArrayList<SUnit> unitsToDestroy = new ArrayList<SUnit>();
        for (SUnit currUnit : units) {// loops through all units

            // if the unit is maintained, boost its level
            if (currUnit.getStatus() == Unit.STATUS_OK) {
                currUnit.addToMaintainanceLevel(increase);
            } else if (currUnit.getStatus() == Unit.STATUS_UNMAINTAINED) {
                int rnd = CampaignMain.cm.getRandomNumber(100) + 1;

                // immediately after a game, only decrement. don't scrap.
                long currTime = System.currentTimeMillis();
                if (CampaignMain.cm.getIThread().isImmune(this) || (currUnit.getPassesMaintainanceUntil() > currTime)) {
                    currUnit.addToMaintainanceLevel(-decrease);
                } else if (rnd <= currUnit.getMaintainanceLevel()) {
                    currUnit.addToMaintainanceLevel(-decrease);
                }

                // unmaintained and failed scrap check. blow 'er up.
                else {

                    if (myHouse.isNewbieHouse()) {
                        CampaignMain.cm.toUser("Your " + currUnit.getModelName() + " is badly maintained and failed a survival roll. In a normal faction, " + "failing these rolls <b>destroys</b> the unit. In the training faction you simply get this warning. Take heed.", name, true);
                        return;
                    }// break out if trying to scrap a SOL mech

                    // if scrapping costs bills, subtract the appropriate
                    // amount.
                    int mechscrapprice = Math.round(myHouse.getPriceForUnit(currUnit.getWeightclass(), currUnit.getType()) * Float.parseFloat(getMyHouse().getConfig("ScrapCostMultiplier")));
                    if (getMoney() < mechscrapprice) {
                        mechscrapprice = getMoney();
                    }
                    if (mechscrapprice > 0) {
                        addMoney(-mechscrapprice);
                    }

                    // remove all flu, even if scrapping is free
                    int flutolose = getInfluence();
                    addInfluence(-flutolose);

                    String toSend = "Lack of maintainance has forced your techs to scrap " + currUnit.getPilot().getName() + "'s " + currUnit.getModelName() + " for parts. HQ is displeased (";
                    if (mechscrapprice > 0) {
                        toSend += CampaignMain.cm.moneyOrFluMessage(true, false, -mechscrapprice, true) + ", ";
                    }
                    toSend += CampaignMain.cm.moneyOrFluMessage(false, false, -flutolose, true) + ").";
                    CampaignMain.cm.toUser(toSend, name, true);

                    myHouse.addDispossessedPilot(currUnit, false);
                    unitsToDestroy.add(currUnit);// actually removing now
                    // would cause conc mod
                    // error
                }// end else(failed scrap check)

            }// end else if(isnt maintained)
        }// end for(all elements)

        /*
         * remove those units which were destroyed. no need to send updates b/c
         * unmaintained units can't be in armies.
         */
        for (SUnit destroyedU : unitsToDestroy) {
            this.removeUnit(destroyedU.getId(), false);
        }

    }// end doMaintainance()

    /**
     * Method which checks to see if a player owns an unmaintained unit. Called
     * from Request, RequestDonated, Transfer and other commands. Hacky direct
     * access of SUnitData, but constructing an SUnit when we have direct access
     * to the status and no intent to change it is a bit wasteful.
     *
     * @return boolean indicating owndership of an unmaintained unit.
     */
    public boolean hasUnmaintainedUnit() {

        for (SUnit currU : units) {
            if (currU.getStatus() == Unit.STATUS_UNMAINTAINED) {
                return true;
            }
        }

        // no unmaintained unit found.
        return false;
    }

    /**
     * Transition a player from reserve to active, or vice versa. See in-line
     * comments for more detail.
     *
     * @param newStatus
     *            - true to activate, false to deac.
     */
    public void setActive(boolean newStatus) {

        // lower case the name only once
        String lowerName = name.toLowerCase();

        // de-activating
        if (!newStatus) {

            activeSince = 0;// deactivating. make a 0.
            setLastOnline(System.currentTimeMillis());

            /*
             * Player is being moved to ianctive status. This means he is no
             * longer an eligible attack target. Need to remove his oplists and
             * clear his entries on other players oplists.
             */
            OpponentListHelper olh = new OpponentListHelper(this, OpponentListHelper.MODE_REMOVE);
            olh.sendInfoToOpponents("left the front lines and may no longer be attacked");

            /*
             * The player also needs to be removed as a possible defender from
             * all outstanding operations. Loop through the ops, removing him
             * from their defender/chicken trees. It's safe to assume that any
             * deactivation in the face of attack deserves a penalty, so call
             * the punishing shutdown. NOTE: The chicken threads call
             * setActive(false) in order to turn off someone who has been
             * leeched. This means that the thread is calling its own
             * doPenalty() methods indirectly here ... but also lets the first
             * thread to hit the leach ceiling turn off any other attacks
             * against the player.
             */
            CampaignMain.cm.getOpsManager().removePlayerFromAllPossibleDefenderLists(name, true);

            /*
             * Remove the player from all attacker lists. It is presumed that a
             * player who is fighting could never finish the Deactivate command.
             * If a player has gotten this far, his attacks must be in WAITING
             * status, so we remove the player from the games and cancel if they
             * hit 0 attackers.
             */
            CampaignMain.cm.getOpsManager().removePlayerFromAllAttackerLists(this, null, true);

            // all done. remove the player from the active hash and put him in
            // reserve
            myHouse.getActivePlayers().remove(lowerName);
            myHouse.getReservePlayers().put(lowerName, this);

            // NOTE: Deactivation does NOT call IThread.removeImmunity(). This
            // lets SOL reset units.
            // We remove immunity when someone re-activates instead.
        }

        // activating
        else {

            // activating. set current timestamp and clear immunity.
            activeSince = System.currentTimeMillis();
            CampaignMain.cm.getIThread().removeImmunity(this);

            /*
             * Player is activating. His armies are all acceptable, and his
             * status has changed. Broadcast his army values to other players
             * and construct opponent vectors for the newly activated armies.
             * [NOTE: actual checks moved into a helper class so they can be run
             * as a player logs in w/ a running game and after games as well].
             */
            OpponentListHelper olh = new OpponentListHelper(this, OpponentListHelper.MODE_ADD);
            olh.sendInfoToOpponents("is headed to the front lines. You may attack it with ");

            // make the hash switch
            myHouse.getReservePlayers().remove(lowerName);
            myHouse.getActivePlayers().put(lowerName, this);
        }
    }

    /**
     * Standard active/fighting rotation. Use setFighting(bool,bool) to move a
     * player to reserve from fighting after an AFR game, and this method for
     * everything else.
     */
    public void setFighting(boolean newStatus) {
        this.setFighting(newStatus, false);
    }

    /**
     * Transition a player between fighting and active status.
     *
     * @param name
     */
    public void setFighting(boolean newStatus, boolean toReserve) {

        // lower case the name only once
        String lowerName = name.toLowerCase();

        // switch to fighting
        if (newStatus) {

            /*
             * remove the player (if present) from the active list, then add him
             * to the fighting hashtable.
             */
            myHouse.getActivePlayers().remove(lowerName);
            myHouse.getFightingPlayers().put(lowerName, this);

            // send status update to the user
            CampaignMain.cm.toUser("CS|" + +SPlayer.STATUS_FIGHTING, name, false);

            /*
             * Player is being moved to busy status. This means he is no longer
             * an eligible attack target. Need to remove his oplists and clear
             * his entries on other players oplists. Note that this has no
             * effect on players who are being set as Busy immediately after
             * logging in because they disconnected mid-game since they have
             * empty op lists.
             */
            OpponentListHelper olh = new OpponentListHelper(this, OpponentListHelper.MODE_REMOVE);
            olh.sendInfoToOpponents(" entered combat and may no longer be attacked");
        }

        // de-fight from AFR. Move to reserve.
        else if (toReserve) {
            activeSince = 0;
            myHouse.getFightingPlayers().remove(lowerName);
            myHouse.getReservePlayers().put(lowerName, this);
            // Unschedule his activity jobs
            UserActivityComponentsJob.stop(getName());
            UserActivityInfluenceJob.stop(getName());
        }

        else {

            /*
             * remove the player (if present) from the active list, then add him
             * to the fighting hashtable.
             */
            myHouse.getFightingPlayers().remove(lowerName);
            myHouse.getActivePlayers().put(lowerName, this);

            /*
             * If player was STATUS_FIGHTING and is being moved back into
             * STATUS_ACTIVE, either - a game was cancelled; or - a game was
             * finished. If we're dealing with a finished game, let the
             * ImmunityThread handle OpponentList issues. If a cancel, there
             * will not be any immunity and updates should be sent to all
             * players immediately.
             */
            if (!CampaignMain.cm.getIThread().isImmune(this)) {
                OpponentListHelper olh = new OpponentListHelper(this, OpponentListHelper.MODE_ADD);
                olh.sendInfoToOpponents(" halted combat operations and returned to its post. You may attack it with ");
            }
        }

    }// end setFighting(boolean b)

    /**
     * Method which sets a player to fighting without triggering Oplist
     * construction. DO NOT USE THIS METHOD. It is a special activation/business
     * sequence that is used only when a player is returning to the server and
     * already involved in a game and should only be called from ShortOperation.
     * All standard activations and ALL deactivations should be dealt with via
     * SPlayer.setActive(boolean), which sets up opponent lists, informs
     * potential attackers, etc.
     */
    public void setFightingNoOppList() {

        // no immunity from immediate activation
        CampaignMain.cm.getIThread().removeImmunity(this);

        // mark this as the time-of-activation
        activeSince = System.currentTimeMillis();

        // attempt to remove from both reserve AND active, just in case
        String lowerName = name.toLowerCase();
        myHouse.getReservePlayers().remove(lowerName);
        myHouse.getActivePlayers().remove(lowerName);

        // put the player in the fighting list and update status
        myHouse.getFightingPlayers().put(lowerName, this);
        CampaignMain.cm.toUser("CS|" + +SPlayer.STATUS_FIGHTING, name, false);
    }

    /**
     * Method that determines the weighted number or armies a player has active.
     * Each army gives an initial weight of 1. Weight for an army is reduced if
     * its BV +/- MaxBVDifference (from campaign configuration) overlaps another
     * armies BV, falls below MinCount or rises above MaxCount. In short, only
     * the portions of an army which may be *uniquely* targetted by opposing
     * forces with the Min/Max range count fully. The weight is automatically
     * reduced by the level of overlap, and server operators may declare
     * additional overlap penalties. Example: Player A has Armies of 3000 and
     * 3050 BV. MaxBVDifference is 150, and an OverlapPenalty of .20 is set in
     * campaignconfig.txt - Starting weight is 2 for two armies, - Raw amount of
     * overlap is (150-(3050-3000 = 50))/150 = .67 - Weight after raw overlap
     * adjustment is 2.0 - 0.67 = 1.33 - OverlapPenalty is applied (1.33 - .20 =
     * 1.13) In this case, the final weighted number of armies is 1.37.
     *
     * @return int the weighted army number
     * @author urgru 10/27/04
     */
    public double getWeightedArmyNumber() {

        // only get the weight if it hasnt been calculated already.
        if (weightedArmyNumber <= 0) {

            Vector<SArmy> orderedArmies = new Vector<SArmy>(1, 1);

            MWLogger.debugLog("Start getWeightedArmyNumber for " + getName());
            int MinCount = getMyHouse().getIntegerConfig("MinCountForTick");
            int MaxCount = getMyHouse().getIntegerConfig("MaxCountForTick");
            int MaxFlatDiff = 1;
            int legalOps = 0;
            double MaxPercentDiff = 0.0;

            for (SArmy currentArmy : getArmies()) {

                // only count armies within the defined Min/Max range
                int forceBV = currentArmy.getOperationsBV(null);
                if (forceBV <= MinCount) {
                    continue;
                }

                if (forceBV >= MaxCount) {
                    continue;
                }

                // Don't count the army if it's disabled
                if (currentArmy.isDisabled()) {
                    continue;
                }

                // if they army is only set up for ops that SO's do not deem
                // legal for component production then the player doesnt get
                // anything.
                boolean fLegalOp = false;
                for (String Opname : currentArmy.getLegalOperations().keySet()) {
                    if (!CampaignMain.cm.getOpsManager().getOperation(Opname).getBooleanValue("DoesNotCountForPP")) {
                        fLegalOp = true;
                        MaxFlatDiff += Math.max(0, CampaignMain.cm.getOpsManager().getOperation(Opname).getIntValue("MaxBVDifference"));
                        MaxPercentDiff += Math.max(0, CampaignMain.cm.getOpsManager().getOperation(Opname).getIntValue("MaxBVPercent"));
                        legalOps++;
                    }
                }

                // Army does is not used in a PP legal op.
                if (!fLegalOp) {
                    continue;
                }

                /*
                 * Sort the armies into BV order, least to greatest. Take an
                 * enumeration of all armies. 1st is added to orderedArmies by
                 * default. Additional armies are compared to previously sorted
                 * BVs and inserted in front of the first element which has a
                 * higher value. If currentForce is larger than previously
                 * sorted armies it is appended to end of the vector
                 */

                // if empty, add the first force by default
                if (orderedArmies.size() == 0) {
                    orderedArmies.add(currentArmy);
                } else {// size > 0
                    Enumeration<SArmy> f = orderedArmies.elements();
                    int forceNumber = 0;// number of current army
                    boolean forceSorted = false;
                    while (f.hasMoreElements() && !forceSorted) {
                        if (currentArmy.getOperationsBV(null) < (f.nextElement()).getOperationsBV(null)) {
                            orderedArmies.add(forceNumber, currentArmy);
                            forceSorted = true;
                        } else {
                            forceNumber++;
                        }
                    }// end while(more elements to compare to)

                    if (!forceSorted) {
                        orderedArmies.add(currentArmy);
                    }

                }// end else (not first)

            }// end for(each army)

            /*
             * Determine overlap of lances, now that they have been ordered.
             * Reduce payout modifier if forces cover similar value ranges. Only
             * do this if there are actually ordered armies!
             */
            if (legalOps != 0) {
                MaxFlatDiff /= legalOps;
                MaxPercentDiff /= legalOps;
            }
            weightedArmyNumber = orderedArmies.size();

            double weightMod = Math.max(0, getMyHouse().getDoubleConfig("BaseCountForProduction"));
            weightedArmyNumber *= weightMod;

            if (weightedArmyNumber > 0) {

                Enumeration<SArmy> e = orderedArmies.elements();
                SArmy currentArmy = e.nextElement();// get first army
                int currentBV = currentArmy.getOperationsBV(null);

                // holder for whichever is greater - flat diff or percent
                double currentMaxDiff = 0;

                /*
                 * compare first force to floor. get first army, determine
                 * percent and flat difference, then test against the BV-edge.
                 */
                double caPercentDiff = currentBV * MaxPercentDiff;
                if (MaxFlatDiff >= caPercentDiff) {
                    currentMaxDiff = MaxFlatDiff;
                } else {
                    currentMaxDiff = caPercentDiff;
                }

                if (currentBV - MinCount < currentMaxDiff) {
                    weightedArmyNumber -= getMyHouse().getDoubleConfig("FloorPenalty");
                    int overlap = currentBV - MinCount;
                    weightedArmyNumber -= (currentMaxDiff - overlap) / currentMaxDiff;
                }

                /*
                 * compare intermediate forces to each other...
                 */
                SArmy nextArmy = null;// for use in loop
                int nextBV = 0;// for use in loop
                while (e.hasMoreElements()) {// loop through remaining forces

                    // get the next army, and its BV
                    nextArmy = e.nextElement();
                    nextBV = nextArmy.getOperationsBV(null);

                    /*
                     * test whether flat or percent BV difference is larger for
                     * these two armies. compare based on larger window.
                     */
                    if (MaxPercentDiff <= 0) {
                        currentMaxDiff = MaxFlatDiff;
                    } else {
                        if (currentBV > nextBV) {
                            caPercentDiff = currentBV * MaxPercentDiff;
                        } else {
                            caPercentDiff = nextBV * MaxPercentDiff;
                        }

                        if (MaxFlatDiff >= caPercentDiff) {
                            currentMaxDiff = MaxFlatDiff;
                        } else {
                            currentMaxDiff = caPercentDiff;
                        }
                    }

                    if (nextBV - currentBV < currentMaxDiff) {
                        weightedArmyNumber -= getMyHouse().getDoubleConfig("OverlapPenalty");
                        int overlap = nextBV - currentBV;
                        weightedArmyNumber -= (currentMaxDiff - overlap) / currentMaxDiff;
                    }
                    currentArmy = nextArmy;// set up for the next iteration
                    currentBV = nextBV;// set up for the next iteration
                }// end while(more elements)

                /*
                 * compare last force to ceiling
                 */
                caPercentDiff = currentBV * MaxPercentDiff;
                if (MaxFlatDiff >= caPercentDiff) {
                    currentMaxDiff = MaxFlatDiff;
                } else {
                    currentMaxDiff = caPercentDiff;
                }
                if (MaxCount - currentBV < currentMaxDiff) {
                    weightedArmyNumber -= getMyHouse().getDoubleConfig("CeilingPenalty");
                    int overlap = MaxCount - currentBV;
                    weightedArmyNumber -= (currentMaxDiff - overlap) / currentMaxDiff;
                }

                /*
                 * Remove armies which cannot attack from the weighting AFTER
                 * overlap checks in order to discourage any abusive stacking.
                 */
                for (SArmy currA : orderedArmies) {
                    if (currA.getLegalOperations().size() <= 0) {
                        weightedArmyNumber -= weightMod;
                    }
                }

                // make sure at least 1 is returned, in case penalties create <1
                // cases.
                if (weightedArmyNumber < 0) {
                    weightedArmyNumber = weightMod;
                }

            }// end if(armies were ordered)
        }// end if (weighted <= 0)
        MWLogger.debugLog("End getWeightedArmyNumber for " + getName());
        return weightedArmyNumber;
    }

    /**
     * A method which resets the weightedArmyNumber to -1, forcing a
     * recalculation next time the above method (getWeightedArmyNumber) is
     * called. Should be triggered by anything which changes army BV or army
     * numbers - game resolution and EXM, etc.
     *
     * @urgru 11/12/04
     */
    public void resetWeightedArmyNumber() {
        weightedArmyNumber = -1;
    }

    public void reset(String confirm) {

        if (!confirm.equals("CONFIRM")) {
            return;
        }

        armies.clear();
        units.clear();
        money = 0;
        exclusionList.getAdminExcludes().clear();
        exclusionList.getPlayerExcludes().clear();
        experience = 0;
        baysOwned = 0;
        availableTechs.clear();
        totalTechs.clear();
        technicians = 0;
        fluffText = " ";
        rewardPoints = 0;
        groupAllowance = 0;
        influence = 0;
        myHouse = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getConfig("NewbieHouseName"), null);
        myLogo = " ";
        personalPilotQueue.flushQueue();
        rating = 1600;
        xpTillReward = 0;
        xpTillFlu = 0;
        setMekToken(0);
        sellingto = " ";
        weightedArmyNumber = 0;
        setSave();
    }

    /**
     * Add money to a player. Money is always modified relative to a previous
     * amount (this.fromString is an expetion, but sets the value directly), so
     * there is no need for a public SPlayer.setMoney() method.
     */
    public void addMoney(int i) {

        // holder, amount to store.
        int moneyToSet = money + i;

        // don't let SOL exceed cap, or anyone have negative cash
        int maxNewbieCbills = Integer.parseInt(getMyHouse().getConfig("MaxSOLCBills"));
        if (myHouse.isNewbieHouse() && (moneyToSet > maxNewbieCbills)) {
            moneyToSet = maxNewbieCbills;
        }
        if (moneyToSet < 0) {
            moneyToSet = 0;
        }

        // change the value and send an update
        money = moneyToSet;
        CampaignMain.cm.toUser("PL|SM|" + money, name, false);
        setSave();
    }

    /**
     * Get the amount of money the player currently has on hand. Required for
     * IBuyer.
     */
    public int getMoney() {
        return money;
    }

    /**
     * @ Salient for free build, mek tokens iterate up to the server limit. Updates CPlayer.
     */
    public void addMekToken(int i) {

        int tokenToSet = this.getMekToken() + i;
        this.setMekToken(tokenToSet);
        CampaignMain.cm.toUser("PL|UMT|" + tokenToSet, name, false); //UMT: Update Mek Token on cplayer
        setSave();

    }

    public void setPassword(MWPasswdRecord pass) {

        if (pass == null) {
            try {
                throw new Exception();
            } catch (Exception ex) {
                MWLogger.errLog(ex);
            }
        }
        password = pass;
        setSave();
    }

    public MWPasswdRecord getPassword() {
        return password;
    }

    /**
     * Method required for ISeller compliance. Used to distinguish between human
     * controlled actors (this class) and factions/automated actors (SHouse).
     */
    public boolean isHuman() {
        return true;
    }

    /**
     * Simple method that returns a player's faction.
     */
    public SHouse getMyHouse() {
        return myHouse;
    }

    /**
     * Method which determines which house a player is actually fighting for.
     * Used to display contracting house, instead of real faction, for
     * mercenaries.
     */
    public SHouse getHouseFightingFor() {
        return getMyHouse().getHouseFightingFor(this);
    }

    /**
     * Set the player's faction. Should only be used by Defect, ForcedDefect and
     * Enroll commands.
     */
    public void setMyHouse(SHouse h) {
        myHouse = h;
        setSave();
    }

    /**
     * A Method to get the current duty status of a player. Options are, from
     * lowest to hightest, STATUS_LOGGEDOUT, STATUS_RESERVE, STATUS_ACTIVE, and
     * STATUS_FIGHTING.
     */
    public int getDutyStatus() {

        String lowerName = name.toLowerCase();

        // Fighting
        if (myHouse.getFightingPlayers().containsKey(lowerName)) {
            return SPlayer.STATUS_FIGHTING;
        }

        // Active
        if (myHouse.getActivePlayers().containsKey(lowerName)) {
            return STATUS_ACTIVE;
        }

        // Logged into house
        if (myHouse.getReservePlayers().containsKey(lowerName)) {
            return SPlayer.STATUS_RESERVE;
        }

        // Not in any faction hash. he's logged out.
        return SPlayer.STATUS_LOGGEDOUT;
    }

    /**
     * Determines the weighted number of votes a player can cast. Draws a flat
     * config out of campaignconfig.txt to use as a base number. Additonal votes
     * may be assigned as a player gains XP, up to a configurable ceiling. Used
     * by the various vote cmds to block overvoting, etc.
     *
     * @return int representing total # of votes player is allowed to cast.
     */
    public int getNumberOfVotesAllowed() {

        int voteTotal = Integer.parseInt(getMyHouse().getConfig("StartingVotes"));
        int xpForVote = Integer.parseInt(getMyHouse().getConfig("XPForAdditionalVote"));
        int maxVotes = Integer.parseInt(getMyHouse().getConfig("MaximumVotes"));

        voteTotal += (int) Math.floor(experience / xpForVote);
        if (voteTotal > maxVotes) {
            voteTotal = maxVotes;
        }

        return voteTotal;
    }

    /**
     * Strip the player's units. They disappear forever and are NOT given to the
     * player's house.
     *
     * @param sendStatus
     *            - boolean. if true, send the player's status downstream.
     *            should usually be true. false when called from NewbieHouse,
     *            which send status on its own after granting new units.
     */
    public void stripOfAllUnits(boolean sendStatus) {
        units = new Vector<SUnit>(1, 1);
        armies = new Vector<SArmy>(1, 1);

        if (sendStatus) {
            CampaignMain.cm.toUser("PS|" + this.toString(true), name, false);
        }

        setSave();
        CampaignMain.cm.toUser("PL|SHP|" + buildHangarPenaltyString(), name, false);
    }

    // EXPERIENCE SET/ADD/GET Methods
    /**
     * Add experience to the player. Boolean param is used to prevent RP gain
     * from mod/admin XP additions.
     *
     * @param i
     *            - amount of RP to add
     * @param modAdded
     *            - true if added from a mod/admin command
     */
    public void addExperience(int i, boolean modAdded) {

        // change xp
        experience += i;

        // check floor
        if (experience < 0) {
            experience = 0;
        }

        // check SOL cap
        if (myHouse.isNewbieHouse() && (experience > Integer.parseInt(getMyHouse().getConfig("MaxSOLExp")))) {
            experience = Integer.parseInt(getMyHouse().getConfig("MaxSOLExp"));
        }

        // update client & all userlists
        CampaignMain.cm.toUser("PL|SE|" + experience, name, false);
        CampaignMain.cm.doSendToAllOnlinePlayers("PI|EX|" + name + "|" + experience, false);

        // update corresponding small player.
        SmallPlayer smallp = myHouse.getSmallPlayers().get(name.toLowerCase());
        if (smallp != null) {
            smallp.setExperience(experience);
        }

        // check and send mek bay numbers
        CampaignMain.cm.toUser("PL|SB|" + getTotalMekBays(), name, false);
        CampaignMain.cm.toUser("PL|SF|" + getFreeBays(), name, false);

        // check reward, if not mod added. never reduce rollover counter.
        if (!modAdded && (i > 0)) {

            int currentXP = xpTillReward + i;
            int rollOver = (Integer.parseInt(getMyHouse().getConfig("XPRollOverCap")));

            // if XP is over rollover point, reduce until below again
            if ((currentXP >= rollOver) && (rollOver > 0)) {

                int rpToAdd = 0;
                while (currentXP >= rollOver) {
                    currentXP -= rollOver;
                    rpToAdd++;
                }

                addReward(rpToAdd);

                // reset the counter
                setXpTillReward(currentXP);

                // set up and send upe rp link
                String toSend = "You earned " + rpToAdd + " experience " + CampaignMain.cm.getConfig("RPShortName");
                toSend += "[<a href=\"MWUSERP\">Use " + CampaignMain.cm.getConfig("RPShortName") + "</a>]";
                CampaignMain.cm.toUser(toSend, name, true);

            } else {
                setXpTillReward(currentXP);
            }
        }

        //@salient
        if (!modAdded && (i > 0))
        {

            int currentXP = xpTillFlu + i;
            int rollOver = (Integer.parseInt(getMyHouse().getConfig("FluXPRollOverCap")));

            // if XP is over rollover point, reduce until below again
            if ((currentXP >= rollOver) && (rollOver > 0))
            {

                int fluToAdd = 0;
                while (currentXP >= rollOver)
                {
                    currentXP -= rollOver;
                    fluToAdd++;
                }

                addInfluence(fluToAdd);

                // reset the counter
                setXpTillFlu(currentXP);

                String toSend = "You earned " + fluToAdd + CampaignMain.cm.getConfig("FluShortName") + " by gaining xp!";
                CampaignMain.cm.toUser(toSend, name, true);

            }
            else
            {
                setXpTillFlu(currentXP);
            }
        }

        setSave();
    }

    public int getExperience() {
        return experience;
    }

    // SPECIAL USE METHODS (PRIVATE OR PUBLIC&STATIC)
    /**
     * Determine the total BV of all units owned by the player. This is used by
     * the welfare checks to see whether a players units can form an army of
     * sufficient BV. Note that for_sale units are included in the BV total,
     * because skipping them would allow players to list a unit, get welfare
     * units, and then delist the sales unit in order. Freebies is something we
     * want to avoid, because people are evil and cheat.
     *
     * @author Jason Tighe.
     * @return the total bv of the player's units.
     */
    public int getHangarBV() {
        int bv = 0;
        for (SUnit currU : units) {
            bv += currU.getBVForMatch();
        }
        return bv;
    }

    //@salient - do the same as above but also some other BV calcs.
    public int getHangarBVforMC()
    {
        int bv = 0;
        boolean removeLockedBV = getMyHouse().getBooleanConfig("LockedUnits_RemoveBV");
        boolean ignoreAeroBV = getMyHouse().getBooleanConfig("IgnoreAeroBV");

        for (SUnit currU : units)
        {
            if(removeLockedBV) // do not add BV of units that are locked.
            {
            	if(currU.isLocked() == false) //if unit is locked, ignore it
            	{
            		if(ignoreAeroBV && currU.getType() == 5) // ignore aero units
            			continue;
            		else
            			bv += currU.getBVForMatch();
            	}
            }
            else // add up all unit bv
            {
        		if(ignoreAeroBV && currU.getType() == 5) // ignore aero units
        			continue;
        		else
        			bv += currU.getBVForMatch();
            }
        }

        return bv;
    }

    /**
     * Simple private method which returns the next available free position ID
     * (hangar location). While this seems pointless, and probably is, the
     * hangar ID is used by the client for all kinds of things and we're stuck
     * with it until someone takes the time to weed it out completely.
     */
    private int getFreeID() {
        int id = 0;
        boolean found = false;
        while (!found) {
            found = true;
            for (int i = 0; i < units.size(); i++) {
                if (units.get(i).getPosId() == id) {
                    found = false;
                    id++;
                }
            }
        }
        return id;
    }

    public int getFreeArmyId() {
        int i = 0;
        boolean free = false;
        while (!free) {
            free = true;
            for (int j = 0; j < getArmies().size(); j++) {
                if (getArmies().elementAt(j).getID() == i) {
                    free = false;
                    i++;
                }
            }
        }

        return i;
    }

    // METHODS TO CHECK/COMMENT
    /**
     * @author Jason Tighe aka Torren
     * @return if the player is eligible for welfare light meks from faction
     *         bays. due to lack of mechs in bay and they are all light
     */
    public boolean mayAcquireWelfareUnits() {

        if ((getHangarBV() < getMyHouse().getIntegerConfig("WelfareTotalUnitBVCeiling"))
        && (getMoney() < getMyHouse().getIntegerConfig("WelfareCeiling")))
        {
            return true;
        }

        // else
        return false;
    }

    //MINI CAMPAIGN CODE
    /**
     * @author Salient
     * @return if enabled, this method will initiate the Restock Phase (currency injection)
     * 		   if hangar is below a certain threshold. This occurs AFTER a match.
     */
    public void checkHangarRestockMC()
    {
    	boolean enabledMC = getMyHouse().getBooleanConfig("Enable_MiniCampaign");
    	boolean lockUnits = getMyHouse().getBooleanConfig("LockUnits");

    	//adding this in before method exit, since i want to be able to allow
    	//unit locking while mini campaign is disabled yes locked units is not
    	int lockedLimit = getMyHouse().getIntegerConfig("UnlockUnits_Percentage");
    	if ( !enabledMC && lockUnits && lockedLimit != -1)
    	{
    		if (percentLockedUnitsMC() >= lockedLimit)
    		{
    			unlockAllUnitsMC();
    			setSave();
    		}
    	}

    	if(!enabledMC)
    	{
    		toSelf("AM: Mini Campaigns are disabled on the server!");
    		return;
    	}

    	//debug
    	MWLogger.errLog(getName() + "'s BV: " + getHangarBVforMC());

    	//set states and cache configs
    	boolean restock = false;
    	boolean minBVRestock = false;
    	boolean percentRestock = false;
    	boolean unitRestock = false;

    	//tempted to make these global variables...
    	int minBVLimit = getMyHouse().getIntegerConfig("MinBV_HangarRestock");
    	int percentBVLimit = getMyHouse().getIntegerConfig("Percent_HangarRestock");
    	int minUnitLimit = getMinUnitResetMC();

    	int restockCB = getMyHouse().getIntegerConfig("RestockCB_Injection");
    	int restockRP = getMyHouse().getIntegerConfig("RestockRP_Injection");
    	int restockFLU = getMyHouse().getIntegerConfig("RestockFLU_Injection");
    	int restockMT = getMyHouse().getIntegerConfig("RestockMT_Injection");

    	//check if we should restock
    	if( minBVLimit != -1 && getHangarBVforMC() < minBVLimit )
    	{
    		restock = true;
    		minBVRestock = true;

    		MWLogger.modLog(getName() + " has gone under BV limit and a restock should occur");
    	}

    	if( percentBVLimit != -1 && getHangarBVforMC() < getBVResetPointMC() )
    	{
    		restock = true;
    		percentRestock = true;
    		setBVTracker(0); //return this to default zero. on activation, it will be set to new value.

    		MWLogger.modLog(getName() + " has gone under % BV limit and a restock should occur");
    	}

    	if( minUnitLimit != -1 && getUnitCountMC() < minUnitLimit )
    	{
    		restock = true;
    		unitRestock = true;

    		MWLogger.modLog(getName() + " has gone under Unit limit and a restock should occur");
    	}

    	if( !restock && !minBVRestock && minBVLimit != -1)
    	{
    		toSelf("AM: Your hangar is at "+ getHangarBVforMC() + "BV. When you drop below "
    				+ minBVLimit + "BV your mini campaign will restart");
    	}

    	if ( !restock && !percentRestock && percentBVLimit != -1)
    	{
    		toSelf("AM: Your hangar is at "+ getHangarBVforMC() + "BV. When you drop below "
    				+ getBVResetPointMC() + "BV your mini campaign will restart");
    	}

    	if ( !restock && !unitRestock && minUnitLimit != -1)
    	{
    		toSelf("AM: Your hangar is at "+ getUnitCountMC() + "Units. When you drop below "
    				+ minUnitLimit + "Units your mini campaign will restart");
    	}

    	//if too many of the players units are locked to continue, unlock all units
    	if ( !restock && lockUnits && lockedLimit != -1) //do only if feature enabled
    		if (percentLockedUnitsMC() >= lockedLimit)
    		{
    			unlockAllUnitsMC(); // sets save now
    			//setSave();
    		}

    	if( !restock )
    	{
    		setSave(); // needed since shortresolver handles unit locking
    					// though i have to imagine it also saves in shortresolver somewhere...
    		return;
    	}

    	if( restock ) //the way it's set up, may not need to clear currency since it should be clear already.
    	{
        	if( restockRP != -1 )
        	{
        		addReward(-getReward()); //clear before reset
        		addReward(restockRP);
        		toSelf("AM: You have received " + getReward() + " " + CampaignMain.cm.getConfig("RPLongName")
        			+ ". Restock your forces before continuing.");
        	}

        	if( restockFLU != -1 )
        	{
        		addInfluence(-getInfluence()); //clear before reset
        		addInfluence(restockFLU);
        		toSelf("AM: You have received " + getInfluence() + " " + CampaignMain.cm.getConfig("FluLongName")
        			+ ". Restock your forces before continuing.");
        	}

        	if( restockMT != -1 )
        	{
        		addMekToken(-getMekToken()); // clear
        		addMekToken(getMekTokenLimit());//have to go to limit to clear to 0, counts up
        		addMekToken(-restockMT); //subtract since it counts up
        		toSelf("AM: You have received " + getRemainingMekTokens()
        			+ " free mek tokens. Restock your forces before continuing.");
        	}

        	if( restockCB != -1 )
        	{
        		addMoney(-getMoney()); // clear
        		addMoney(restockCB);
        		toSelf("AM: You have received " + getMoney() + " " + CampaignMain.cm.getConfig("MoneyLongName")
        			+ ". Restock your forces before continuing.");
        	}

        	setPhaseRestockMC();
        	unlockAllUnitsMC(); // sets save
        	addRewardsMC(); // adds currencies not involved with injection/restocking
        	
    	}

    	return;
    }

    /**
     * @author Salient
     * @return checks if a player can go active for the next cycle in his/her mini campaign
     */
    public boolean canActivateForMiniCampaign()
    {
    	if(!getMyHouse().getBooleanConfig("Enable_MiniCampaign"))
    	{
    		toSelf("AM: Mini Campaigns are disabled on the server!");
    		return false;
    	}

    	int minBVLimit = getMyHouse().getIntegerConfig("MinBV_HangarRestock");
    	int percentBVLimit = getMyHouse().getIntegerConfig("Percent_HangarRestock");
    	int minUnitLimit = getMinUnitResetMC();

    	int restockCB = getMyHouse().getIntegerConfig("RestockCB_Injection");
    	int restockRP = getMyHouse().getIntegerConfig("RestockRP_Injection");
    	int restockFLU = getMyHouse().getIntegerConfig("RestockFLU_Injection");
    	int restockMT = getMyHouse().getIntegerConfig("RestockMT_Injection");

    	float percentCB = getMyHouse().getIntegerConfig("RestockCB_LeewayPercentage") / 100.0f;
    	float percentRP = getMyHouse().getIntegerConfig("RestockRP_LeewayPercentage") / 100.0f;
    	float percentFLU = getMyHouse().getIntegerConfig("RestockFLU_LeewayPercentage") / 100.0f;
    	float percentMT = getMyHouse().getIntegerConfig("RestockMT_LeewayPercentage") / 100.0f;

    	int leewayCB = (int) (restockCB * percentCB);
    	int leewayRP = (int) (restockRP * percentRP);
    	int leewayFLU = (int) (restockFLU * percentFLU);
    	int leewayMT = (int) (restockMT * percentMT);

    	boolean requireUnitsAtLimit = getMyHouse().getBooleanConfig("AtUnitLimitsMC");
    	boolean requireUnitsAtOrOverLimit = getMyHouse().getBooleanConfig("AtOrOverUnitLimitsMC");

    	//boolean canActivate = true;

    	//check if hangar BV has increased (maybe via salvage? or trades?), if so update to new value.
        if ( percentBVLimit != -1  && getHangarBVforMC() > getBVTracker() )
        {
        	setBVTracker(getHangarBVforMC());
        	MWLogger.modLog(getName() + "'s BV reset point set to " + getBVResetPointMC() + " BV");
        }
        
        if(isPhaseRestockMC())
        {       	
        	if ( minBVLimit != -1  && getHangarBVforMC() < minBVLimit )
        	{
        		toSelf("AM: To go active you must raise your hangar BV! You have " + getHangarBVforMC()
        		+ " and need at least " + minBVLimit + " to go active!");
        		return false;
        	}
        	
        	if ( minUnitLimit != -1  && getUnitCountMC() < minUnitLimit )
        	{
        		toSelf("AM: To go active you must raise your hangar Unit Count! You have " + getUnitCountMC()
        		+ " and need at least " + minUnitLimit + " to go active!");
        		return false;
        	}
        	
        	if( restockCB != -1 && leewayCB > 0f && getMoney() > leewayCB )
        	{
        		toSelf("AM: You have too many " + CampaignMain.cm.getCurrencyName("money", false) + " to go active!" );
        		return false;
        	}
        	
        	if( restockRP != -1 && leewayRP > 0f && getReward() > leewayRP )
        	{
        		toSelf("AM: You have too many " + CampaignMain.cm.getCurrencyName("rp", false) + " to go active!" );
        		return false;
        	}
        	
        	if( restockFLU != -1 && leewayFLU > 0f && getInfluence() > leewayFLU )
        	{
        		toSelf("AM: You have too much " + CampaignMain.cm.getCurrencyName("flu", false) + " to go active!" );
        		return false;
        	}
        	
        	if( restockMT != -1 && leewayMT > 0f && getRemainingMekTokens() > leewayMT )
        	{
        		toSelf("AM: You must use up more of your free meks to go active!" );
        		return false;
        	}
        	
        	if ( requireUnitsAtOrOverLimit && isPhaseRestockMC() && isAtOrOverUnitLimits() == false )
        	{
        		toSelf("AM: You must reach or exceed the limit for each unit type/weight before "
        				+ "restarting your mini campaign!");
        		return false;
        	}
        	else if ( requireUnitsAtLimit && isPhaseRestockMC() && isAtUnitLimits() == false )
        	{
        		toSelf("AM: You must reach the limit for each unit type/weight before restarting your "
        				+ "mini campaign!");
        		return false;
        	}
        	
        	//At this point we assume that the player can activate and leave restock state.
			removeInjectedCurrencyMC(restockCB, restockRP, restockFLU, restockMT);

			if ( percentBVLimit != -1 )
			{
				setBVTracker(getHangarBVforMC());// set new hangar BV for tracking
				MWLogger.modLog(getName() + "'s BV reset point set to " + getBVResetPointMC() + " BV");
			}
			
    		setPhaseActiveMC();
    		setSave();
        }

    	reportStatusMC();
    	return true;
    }

    //@salient will be used here and in a command.
    public void reportStatusMC()
    {
    	int minBVLimit = getMyHouse().getIntegerConfig("MinBV_HangarRestock");
    	int percentBVLimit = getMyHouse().getIntegerConfig("Percent_HangarRestock");
    	int minUnitLimit = getMinUnitResetMC();

		if ( percentBVLimit != -1 )
		{
			toSelf("AM: Current Hangar BV: " + getHangarBVforMC());
			toSelf("AM: Next mini campaign cycle will begin when your hangar BV falls below " + getBVResetPointMC());
		}

		if ( minUnitLimit != -1 )
		{
			toSelf("AM: Current Unit Count: " + getUnitCountMC());
			toSelf("AM: Next mini campaign cycle will if your Unit Count falls below " + minUnitLimit);
		}

        if ( minBVLimit != -1 )
        {
			toSelf("AM: Current Hangar BV: " + getHangarBVforMC());
			toSelf("AM: Next mini campaign cycle will begin when your hangar BV falls below " + minBVLimit);
        }

    }


    // -- MC DATA SAVE/LOAD --
    private String saveStatusMC()
    {
    	SerializedMessage result = new SerializedMessage("&");
    	result.append(phaseMC);
    	return result.toString();
    }

    private void loadStatusMC(String data)
    {
    	StringTokenizer st = new StringTokenizer(data, "&");
    	if(st.hasMoreTokens())
    		phaseMC = TokenReader.readString(st);
    	else
    		MWLogger.errLog("loadStatusMC failed! no token available for phaseMC");
    }

    private boolean isPhaseRestockMC()
    {
    	if(phaseMC.equalsIgnoreCase(RESTOCK_MC))
    		return true;
    	else
    		return false;
    }

    private void setPhaseActiveMC() {	phaseMC = ACTIVE_MC;	}
    private void setPhaseRestockMC() {	phaseMC = RESTOCK_MC;	}

    //@salient - made a new command called RG (refresh gui) not really sure it works tbh..
    public void refreshGUI()
    {
    	CampaignMain.cm.toUser("RG|" + " ", name, false);
    }

    //@salient
    public void unlockAllUnitsMC()
    {

    	if(!getMyHouse().getBooleanConfig("LockUnits"))
    		return;

    	for (SUnit aUnit : getUnits())
    	{
    		aUnit.setLocked(false);
    	}

        CampaignMain.cm.toUser("PS|" + this.toString(true), name, false);
        setSave();
        CampaignMain.cm.toUser("PL|SHP|" + buildHangarPenaltyString(), name, false);

    	//refreshGUI();
    	toSelf("AM: Units have been unlocked!");
    }

// doesnt work, dunno why... might work, just didn't work in shortresolver?
//    //@salient
//    public void removeLockedUnitsFromArmiesMC()
//    {
//    	if(!getMyHouse().getBooleanConfig("LockUnits"))
//    		return;
//
//    	getLockedArmy();
//        for (SArmy army : getArmies())
//        {
//        	for (Unit aUnit : army.getUnits())
//        	{
//        		if(aUnit.isLocked())
//        			army.removeUnit(aUnit.getId());
//        	}
//        }
//
//    	refreshGUI();
//    	toSelf("AM: Locked Units Removed From Army!");
//    }
    
    /**
     * @author Salient
     * 			adds rewards to player at end of mini campaign cycle
     */
    private void addRewardsMC()
    {        
        int rewardBays = this.getMyHouse().getIntegerConfig("MC_Reward_BAYS");
        int rewardTechs = this.getMyHouse().getIntegerConfig("MC_Reward_TECHS");
        int rewardXP = this.getMyHouse().getIntegerConfig("MC_Reward_XP");
        int rewardRP = this.getMyHouse().getIntegerConfig("MC_Reward_RP");
        int rewardFLU = this.getMyHouse().getIntegerConfig("MC_Reward_FLU");
        int rewardCB = this.getMyHouse().getIntegerConfig("MC_Reward_CB");
        int rewardMT = this.getMyHouse().getIntegerConfig("MC_Reward_MT");
        
        this.addBays(rewardBays);
        this.addTechnicians(rewardTechs);
        this.addExperience(rewardXP, false);
        this.addReward(rewardRP);
        this.addInfluence(rewardFLU);
        this.addMoney(rewardCB);
        this.addMekToken(-rewardMT); // counts up to limit
    	
    }

    //@salient - returns the percent of players units that are locked.
    public int percentLockedUnitsMC()
    {
    	int numLocked = 0;

    	for (SUnit aUnit : getUnits())
    	{
    		if(aUnit.isLocked())
    			numLocked++;
    	}

    	float result = getUnitCount() / numLocked;

    	return (int) (result * 100);

    }

    //@salient
    private void removeInjectedCurrencyMC(int restockCB, int restockRP, int restockFLU, int restockMT)
    {
    	if(hasMoney() && restockCB != -1)
    	{
    		addMoney(-getMoney());
    	}

    	if(hasRP() && restockRP != -1)
    	{
    		addReward(-getReward());
    	}

    	if(hasFlu() && restockFLU != -1)
    	{
    		addInfluence(-getInfluence());
    	}

    	if(hasMT() && restockMT != -1)
    	{
    		addMekToken(-getMekToken()); // clear
    		addMekToken(getMekTokenLimit());//have to go to limit to clear to 0, counts up
    	}
    }

    //@salient - using a percentage set by SO, this returns the BV at which point the mini campaign will end
    private int getBVResetPointMC()
    {
    	float percent = getMyHouse().getIntegerConfig("Percent_HangarRestock") / 100.0f;
    	int resetPt = (int) (getBVTracker() * percent);
    	return resetPt;
    }

    //@salient - using a value set by SO, this returns the Unit count at which point the mini campaign will end
    private int getMinUnitResetMC()
    {
    	int resetPt = getMyHouse().getIntegerConfig("Unit_HangarRestock");
    	return resetPt;
    }

    // -- DISCORD BOT DATA SAVE/LOAD --
    private String saveDiscordInfo()
    {
    	SerializedMessage result = new SerializedMessage("&");
    	result.append(discordID);
    	return result.toString();
    }

    private void loadDiscordInfo(String data)
    {
		StringTokenizer st = new StringTokenizer(data, "&");
		if(st.hasMoreTokens())
		{
			discordID = TokenReader.readString(st);
		}
		else
			MWLogger.debugLog("loadDiscordInfo failed! no token available!");   		
    }


    //@salient
    public void removeCurrency()
    {
    	addMoney(-getMoney());
    	addInfluence(-getInfluence());
    	addReward(-getReward());

		addMekToken(-getMekToken()); // clear
		addMekToken(getMekTokenLimit());//have to go to limit to clear to 0, counts up
    }

    //@salient
    public boolean hasCurrency()
    {
    	if( getMoney() != 0 || getInfluence() != 0 || getReward() != 0 || getRemainingMekTokens() != 0 )
    		return true;
    	else
    		return false;
    }

    //@salient
    public boolean hasMoney()
    {
    	if( getMoney() != 0 )
    		return true;
    	else
    		return false;
    }

    //@salient
    public boolean hasFlu()
    {
    	if( getInfluence() != 0 )
    		return true;
    	else
    		return false;
    }

    //@salient
    public boolean hasRP()
    {
    	if( getReward() != 0 )
    		return true;
    	else
    		return false;
    }

    //@salient
    public boolean hasMT()
    {
    	if( getRemainingMekTokens() != 0 )
    		return true;
    	else
    		return false;
    }

    //@salient
    public int getRemainingMekTokens()
    {
    	int limit = getMyHouse().getIntegerConfig("FreeBuild_Limit");

    	return limit - getMekToken(); //mek tokens count up to limit
    }

    //@salient
    public int getMekTokenLimit()
    {
    	int limit = Integer.parseInt(getMyHouse().getConfig("FreeBuild_Limit"));

    	return limit; //mek tokens count up to limit
    }

    /**
     * A method to add a specified amount of influence
     *
     * @param i
     *            - amount of influence to add
     */
    public void addInfluence(int i) {
        setInfluence(getInfluence() + i);
    }

    /**
     * @return current post-game payment to technicians, in Cbills
     */
    @Override
    public int getCurrentTechPayment() {

        // recalculate if -1
        if (currentTechPayment < 0) {
            doPayTechniciansMath();
        }

        return currentTechPayment;
    }

    /**
     * @param i
     *            - post-game payment to set, in Cbills
     */
    @Override
    public void setCurrentTechPayment(int i) {
        currentTechPayment = i;
        setSave();
    }

    /**
     * @return the number of technicians the player has
     */
    @Override
    public int getTechnicians() {
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            return getBaysOwned();
        }
        // else
        return technicians;
    }

    public Vector<Integer> getTotalTechs() {
        return totalTechs;
    }

    public Vector<Integer> getAvailableTechs() {
        return availableTechs;
    }

    public String totalTechsToString() {
        StringBuilder result = new StringBuilder();

        // Make sure that we keep it as size 4. Had some early issues with rouge
        // vectors.
        getTotalTechs().setSize(4);
        for (Integer tech : getTotalTechs()) {
            result.append(tech + "%");
        }

        return result.toString();
    }

    public String availableTechsToString() {
        StringBuilder result = new StringBuilder();

        // Make sure that we keep it as size 4. Had some early issues with rouge
        // vectors.
        getAvailableTechs().setSize(4);
        for (Integer tech : getAvailableTechs()) {
            result.append(tech + "%");
        }

        return result.toString();
    }

    public void addAvailableTechs(int type, int number) {

        if (type > UnitUtils.TECH_ELITE) {
            return;
        }

        int techs = getAvailableTechs().elementAt(type);

        techs += number;

        synchronized(availableTechs) {
        	getAvailableTechs().set(type, techs);
        }

        CampaignMain.cm.toUser("PL|UAT|" + availableTechsToString(), name, false);

    }

    public void setAvailableTechs(int type, int number) {

        if (type > UnitUtils.TECH_ELITE) {
            return;
        }

        synchronized (availableTechs) {
        	getAvailableTechs().set(type, number);
        }

        CampaignMain.cm.toUser("PL|UAT|" + availableTechsToString(), name, false);

    }

    public void addTotalTechs(int type, int number) {

        if (type > UnitUtils.TECH_ELITE) {
            return;
        }

        int techs = getTotalTechs().elementAt(type);
        techs += number;
        synchronized (totalTechs) {
        	getTotalTechs().set(type, techs);
        }

        CampaignMain.cm.toUser("PL|UTT|" + totalTechsToString(), name, false);
    }

    public void setTotalTechs(int type, int number) {
        if (type > UnitUtils.TECH_ELITE) {
            return;
        }

        synchronized(totalTechs) {
        	getTotalTechs().set(type, number);
        }
        CampaignMain.cm.toUser("PL|UTT|" + totalTechsToString(), name, false);
    }

    public void updateAvailableTechs(String data) {
        try {
            StringTokenizer techs = new StringTokenizer(data, "%");
            int techType = UnitUtils.TECH_GREEN;
            while (techs.hasMoreTokens()) {
                setAvailableTechs(techType, Integer.parseInt(techs.nextToken()));
                techType++;
            }
        } catch (Exception ex) {
        }

    }

    public void updateTotalTechs(String data) {
        try {
            StringTokenizer techs = new StringTokenizer(data, "%");
            int techType = UnitUtils.TECH_GREEN;

            while (techs.hasMoreTokens()) {
                setTotalTechs(techType, TokenReader.readInt(techs));
                techType++;
            }
        } catch (Exception ex) {
        }

    }

    public int getBaysOwned() {
        return baysOwned;
    }

    public void setBaysOwned(int bays) {

        int maxBays = 0;

        if (getMyHouse() != null) {
            maxBays = Integer.parseInt(getMyHouse().getConfig("MaxBaysToBuy"));
        } else {
            maxBays = CampaignMain.cm.getIntegerConfig("MaxBaysToBuy");
        }

        if (maxBays != -1) {
            baysOwned = Math.min(maxBays, bays);
        } else {
            baysOwned = bays;
        }
    }

    public void addBays(int bays) {
        setBaysOwned(baysOwned + bays);
    }

    public String getLastISP() {
        return lastISP;
    }

    public void setLastISP(String isp) {
        lastISP = isp;
    }

    /**
     * @param t
     *            - int to set technicians to.
     */
    @Override
    public void setTechnicians(int t) {

        int maxTechs = 0;

        // dont allow negative techs. always set negatives back to 0.
        if (t < 0) {
            t = 0;
        }

        if (getMyHouse() != null) {
            maxTechs = Integer.parseInt(getMyHouse().getConfig("MaxTechsToHire"));
        } else {
            maxTechs = CampaignMain.cm.getIntegerConfig("MaxTechsToHire");
        }

        if (maxTechs != -1) {
            technicians = Math.min(maxTechs, t);
        } else {
            technicians = t;
        }

        // clear the tech payment any time a new number of techs is set
        setCurrentTechPayment(-1);
        CampaignMain.cm.toUser("PL|ST|" + t, name, false);
        CampaignMain.cm.toUser("PL|SB|" + getTotalMekBays(), name, false);
        CampaignMain.cm.toUser("PL|SF|" + getFreeBays(), name, false);
        setSave();
    }

    /**
     * @param t
     *            - the number of technicians to add (subtract) from the
     *            player's total sub-zero cases are checked in setTechs(). no
     *            check here.
     */
    @Override
    public void addTechnicians(int t) {
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            addBays(t);
        } else {
            setTechnicians(getTechnicians() + t);
        }
    }

    // NAME GET/SET METHODS
    public String getName() {
        return name;
    }

    public String getColoredName() {
        return "<font color=\"" + getHouseFightingFor().getHouseColor() + "\">" + name + "</font>";
    }

    public String getColoredNameBold() { //@salient
        return "<font color=\"" + getHouseFightingFor().getHouseColor() + "\"><b>" + name + "</b></font>";
    }

    public void setName(String s) {

        if (s == null) {
            throw new NullPointerException();
        }
        name = s;
        setSave();
    }
    
    //@salient
    public String getDiscordID() {
        return discordID;
    }
    
    //@salient
    public void setDiscordID(String _discordID)
    {
        if ( _discordID == null || _discordID == "" )
        {
        	toSelf("AM:You must enter a discord ID to set!");
            return;
        }
        discordID = _discordID;
        setSave();
    }
    
    //@salient- compare client quirks with server
    // lol while this works, realized the way i'm doing things
    // makes this check meaningless... what needs to be checked is the hosts xmls, not the client quirks
    // which are already set by the server anyway....
//    public boolean checkAllQuirkInfoForActivation(String data)
//    {
//    	StringTokenizer st = new StringTokenizer(data,"*");
//    	int debugCounter = 0;
//    	
//        while(st.hasMoreTokens()) 
//        {
//        	SUnit currU = this.getUnit(TokenReader.readInt(st));
//        	String quirks = QuirkHandler.getInstance().returnQuirkList(currU);
//        	if(quirks.equalsIgnoreCase(TokenReader.readString(st)))
//        	{
//        		if(debugCounter < 10)
//        		{
//        			debugCounter++;
//        			//MWLogger.debugLog(currU.getVerboseModelName()+quirks+" MATCHED");
//        		}
//        		continue;        		
//        	}
//        	else
//        		return false;
//        }
//
//        return true;
//    }

    public SArmy getArmy(int id) {

        for (SArmy currA : armies) {
            if (currA.getID() == id) {
                return currA;
            }
        }

        return null;
    }

    public Vector<SArmy> getArmies() {
        return armies;
    }

    public void removeArmy(int armyID) {

        Iterator<SArmy> i = armies.iterator();
        while (i.hasNext()) {
            SArmy currA = i.next();
            if (currA.getID() == armyID) {
                i.remove();
                break;
            }
        }
        CampaignMain.cm.toUser("PL|RA|" + armyID, name, false);
    }

    public void setArmies(Vector<SArmy> v) {
        armies = v;
        setSave();
    }

    public Vector<SUnit> getUnits() {
        return units;
    }

    //@salient
    public int getUnitCount() {
        return units.size();
    }

    //@salient - includes SO check to count only unlocked units LockedUnits_DecrementUnitCount
    private int getUnitCountMC()
    {
    	if(getMyHouse().getBooleanConfig("LockedUnits_DecrementUnitCount"))
    	{
	    	int count = 0;
	    	for(SUnit aUnit : units)
	    	{
	    		if(aUnit.isLocked() == false)
	    			count++;
	    	}
	    	return count;
    	}
    	else
    	{
    		return units.size();
    	}
    }

    // Comparable
    public int compareTo(Object o) {
        SPlayer p = (SPlayer) o;
        if (getRating() > p.getRating()) {
            return 1;
        } else if (getRating() < p.getRating()) {
            return -1;
        }
        return p.getName().compareTo(name);
    }

    public int getScrapsThisTick() {
        return scrapsThisTick;
    }

    public void addScrapThisTick() {
        scrapsThisTick += 1;
    }

    public void setScrapsThisTick(int scraps) {
        scrapsThisTick = scraps;
    }

    public int getDonationsThisTick() {
        return donationsThisTick;
    }

    public void addDonationThisTick() {
        donationsThisTick += 1;
    }

    public void setDonatonsThisTick(int donations) {
        donationsThisTick = donations;
    }

    public Date getLastOnlineDate() {
        return new Date(lastOnline);
    }

    public long getLastOnline() {
        return lastOnline;
    }

    public void setLastOnline(long l) {
        lastOnline = l;
        SmallPlayer smallp = null;
        if (myHouse.getSmallPlayers().containsKey(name.toLowerCase())) {
            // update the corresponding small player.
            smallp = myHouse.getSmallPlayers().get(name.toLowerCase());
        } else {
            smallp = new SmallPlayer(getExperience(), lastOnline, getRating(), getName(), getFluffText(), getMyHouse());
            myHouse.getSmallPlayers().put(name.toLowerCase(), smallp);
        }

        smallp.setLastOnline(lastOnline);
    }

    public long getAttackRestrictionUntil() {
        return attackRestrictionUntil;
    }

    public void setAttackRestrictionUntil(long l) {
        attackRestrictionUntil = l;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double d) {
        rating = d;

        // update the corresponding small player.
        SmallPlayer smallp = myHouse.getSmallPlayers().get(name.toLowerCase());
        smallp.setRating(rating);

        // if sharing ratings, send to clients
        if (!Boolean.parseBoolean(getMyHouse().getConfig("HideELO"))) {
            Double rounded = getRatingRounded();
            CampaignMain.cm.toUser("PL|SR|" + rounded, name, false);
            CampaignMain.cm.doSendToAllOnlinePlayers("PI|RA|" + name + "|" + rounded, false);
        }

        setSave();
    }

    public String getFluffText() {
        if ((fluffText.length() > 0) && !fluffText.equals(" ") && !fluffText.equals("0")) {
            return fluffText;
        }
        return "";
    }

    public void setFluffText(String s) {
        fluffText = s;

        // update the corresponding small player.
        SmallPlayer smallp = myHouse.getSmallPlayers().get(name.toLowerCase());
        smallp.setFluffText(fluffText);

        setSave();
    }

    public String getLastSentStatus() {
        return lastSentStatus;
    }

    public void setLastSentStatus(String s) {
        lastSentStatus = s;
    }

    /**
     * @return Returns the activeSince.
     */
    public long getActiveSince() {
        return activeSince;
    }

    public int getAmountOfTimesUnitExistsInArmies(int unitID) {
        int result = 0;
        Vector<SArmy> v = getArmies();
        for (int i = 0; i < v.size(); i++) {
            SArmy a = v.elementAt(i);
            if (a.getUnit(unitID) != null) {
                result++;
            }
        }
        return result;
    }

    public void checkAndUpdateArmies(SUnit unit) {

        for (SArmy army : armies) {
            if (army.isUnitInArmy(unit)) {
                army.setBV(0);
                CampaignMain.cm.toUser("PL|SABV|" + army.getID() + "#" + army.getBV(), name, false);
            }
        }
    }// end checkAndUpdateArmies

    // INFLUENCE SET/ADD/GET METHODS @urgru 1/30/03
    /**
     * A method which returns a players influence
     *
     * @return int - influence amount
     */
    public int getInfluence() {
        return influence;
    }

    /**
     * A method which directly sets the amount of influence a player has
     *
     * @param i
     *            - value to give influence
     */
    public void setInfluence(int i) {
        influence = i;
        if (influence > Integer.parseInt(getMyHouse().getConfig("InfluenceCeiling"))) {
            influence = (Integer.parseInt(getMyHouse().getConfig("InfluenceCeiling")));// set
            // to
            // ceiling
            // if
            // above
        }

        if (influence < 0) {
            influence = 0; // Set to 0 if below
        }

        CampaignMain.cm.toUser("PL|SI|" + influence, name, false);
        setSave();
    }

    public int getGroupAllowance() {
        return groupAllowance;
    }

    public void setGroupAllowance(int i) {
        groupAllowance = i;
    }

    // get current amount of reward points a player has
    public int getReward() {
        return rewardPoints;
    }

    // set the current amount of reward points a player has.
    public void setReward(int i) {
        rewardPoints = i;
        if (rewardPoints > (Integer.parseInt(getMyHouse().getConfig("XPRewardCap")))) {
            rewardPoints = (Integer.parseInt(getMyHouse().getConfig("XPRewardCap")));
        }

        if (rewardPoints < 0) {
            rewardPoints = 0;
        }

        CampaignMain.cm.toUser("PL|SRP|" + rewardPoints, name, false);
        setSave();
    }

    public void addReward(int toAdd) {
        setReward(getReward() + toAdd);
    }

    // sets counter to next RP injection triggered by XP gains.
    public void setXpTillReward(int xp) {
        xpTillReward = xp;
        setSave();
    }

    public int getXpTillReward() {
        return xpTillReward;
    }

    // @salient sets counter to next flu injection triggered by XP gains.
    public void setXpTillFlu(int xp) {
        xpTillFlu = xp;
        setSave();
    }

    public int getXpTillFlu() {
        return xpTillFlu;
    }

    public void setMyLogo(String s) {
        myLogo = s;
    }

    public String getMyLogo() {
        return myLogo;
    }

    public void setPlayerSellingto(String selling) {
        sellingto = selling;
    }

    public String getPlayerSellingto() {
        return sellingto;
    }

    public void setPlayerClientVersion(String version) {
        clientVersion = version;
    }

    public String getPlayerClientVersion() {
        return clientVersion;
    }

    public SPersonalPilotQueues getPersonalPilotQueue() {
        return personalPilotQueue;
    }

    public ExclusionList getExclusionList() {
        return exclusionList;
    }

    public void setLastTimeCommandSent(long l) {
        lastTimeCommandSent = l;
    }

    public long getLastTimeCommandSent() {
        return lastTimeCommandSent;
    }

    public void setLastAttackFromReserve(long time) {
        lastAttackFromReserve = time;
    }

    public long getLastAttackFromReserve() {
        return lastAttackFromReserve;
    }

    public boolean hasRepairingUnits() {
        return hasRepairingUnits(true);
    }

    /**
     * Method which returns a boolean indicating whether any units in all the
     * armies or any units period are being repaired.
     *
     * @param inArmy
     *            - if false, check all units. true, check units in armies.
     */
    public boolean hasRepairingUnits(boolean inArmy) {

        // if not using advanced repair don't spend the time checking.
        if (!CampaignMain.cm.isUsingAdvanceRepair()) {
            return false;
        }

        // only check for a repairing unit that is currently in an army
        if (inArmy) {

            for (SArmy army : armies) {
                for (Unit currU : army.getUnits()) {
                    // Needs to be done units are stripped of entity in the army
                    // Might be best to add that to Unit as well have to think
                    // about that --Torren.
                    // TODO: See comment above.
                    SUnit unit = getUnit(currU.getId());
                    if (UnitUtils.isRepairing(unit.getEntity())) {
                        return true;
                    }
                }
            }// end For
        } else {// check for any repairing units the player owns

            for (SUnit currU : getUnits()) {
                if (UnitUtils.isRepairing(currU.getEntity())) {
                    return true;
                }
            }

        }// end else

        return false;
    }// end hasRepairingUnits

    /**
     * Used for Advanced Repair cannot repair a unit that is in combat.
     *
     * @param unitID
     * @return
     */
    public boolean isUnitInLockedArmy(int unitID) {

        if (getUnit(unitID) == null) {
            return false;
        }

        // check all armies
        for (SArmy army : getArmies()) {
            if (!army.isLocked()) {
                continue;
            }
            if (army.getUnit(unitID) != null) {
                return true;
            }
        }

        return false;
    }// end isUnitInLockedArmy

    /**
     * if damage transfers is allowed then pilots to heal while off line.
     */
    public void healAllPilots() {
        try {
            if (!Boolean.parseBoolean(getMyHouse().getConfig("AllowPilotDamageToTransfer"))) {
                return;
            }
            Long timeGone = System.currentTimeMillis() - lastOnline;// timeGone
            // /=60000;
            int tickTime = CampaignMain.cm.getIntegerConfig("TickTime");
            if (timeGone > tickTime) {
                healAllPilots((int) (timeGone / tickTime));
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void healAllPilots(int numberOfHeals) {
        if (!Boolean.parseBoolean(getMyHouse().getConfig("AllowPilotDamageToTransfer"))) {
            return;
        }
        int health = Integer.parseInt(getMyHouse().getConfig("PilotAmountHealedPerTick")) * numberOfHeals;
        int medtechHeal = Integer.parseInt(getMyHouse().getConfig("MedTechAmountHealedPerTick")) * numberOfHeals;

        if (Boolean.parseBoolean(getMyHouse().getConfig("AllowPersonalPilotQueues"))) {
            int typeList[] = { Unit.MEK, Unit.PROTOMEK, Unit.AERO };
            for (int type : typeList) {
                for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                    List<Pilot> list = personalPilotQueue.getPilotQueue(type, weight);
                    for (Pilot pilot : list) {
                        if (pilot.getHits() <= 0) {
                            continue;
                        }
                        int hits = pilot.getHits();

                        hits -= health;
                        if (pilot.getSkills().has(PilotSkill.MedTechID)) {
                            hits -= medtechHeal;
                        }

                        pilot.setHits(Math.max(0, hits));
                    }// end For each
                }// end for weight
            }// end for type
        }

        for (SUnit unit : units) {
            Pilot pilot = unit.getPilot();

            if (pilot.getHits() <= 0) {
                continue;
            }
            int hits = pilot.getHits();

            hits -= health;
            if (pilot.getSkills().has(PilotSkill.MedTechID)) {
                hits -= medtechHeal;
            }

            pilot.setHits(Math.max(0, hits));
        }// end for each
    }// end healAllPilots

    public void healPilots() {
        if (!Boolean.parseBoolean(getMyHouse().getConfig("AllowPilotDamageToTransfer"))) {
            return;
        }
        int health = Integer.parseInt(getMyHouse().getConfig("PilotAmountHealedPerTick"));
        int medtechHeal = Integer.parseInt(getMyHouse().getConfig("MedTechAmountHealedPerTick"));

        if (Boolean.parseBoolean(getMyHouse().getConfig("AllowPersonalPilotQueues"))) {
            int typeList[] = { Unit.MEK, Unit.PROTOMEK, Unit.AERO };
            for (int type : typeList) {
                for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                    List<Pilot> list = personalPilotQueue.getPilotQueue(type, weight);
                    for (Pilot pilot : list) {
                        if (pilot.getHits() <= 0) {
                            continue;
                        }
                        int hits = pilot.getHits();

                        hits -= health;
                        if (pilot.getSkills().has(PilotSkill.MedTechID)) {
                            hits -= medtechHeal;
                        }

                        pilot.setHits(Math.max(0, hits));
                    }// end For each
                }// end for weight
            }// end for type
        } else {
            for (SUnit unit : units) {
                Pilot pilot = unit.getPilot();

                if (pilot.getHits() <= 0) {
                    continue;
                }
                int hits = pilot.getHits();

                hits -= health;
                if (pilot.getSkills().has(PilotSkill.MedTechID)) {
                    hits -= medtechHeal;
                }

                pilot.setHits(Math.max(0, hits));
            }// end for each
        }// end else
    }// end healPilots

    // STATUS DISPLAY METHODS
    /*
     * These would normally be under the PUBLIC METHODS heading; however,
     * they're important (and long) enough to justify their own heading.
     */
    /**
     * Complete human readable status of a player. Absolutely must be maintained
     * and properly updated at all times. /c mystatus is the best/only way to
     * accurately confirm a client's data representation vs. the player's state
     * according to the server.
     */
    public String getReadableStatus(boolean adminStatus) {
        DecimalFormat myFormatter = new DecimalFormat("####.##");
        StringBuilder s = new StringBuilder("<br><b>Status for: " + getColoredName() + " (" + myHouse.getColoredName());

        if (getSubFactionName().trim().length() > 0) {
            s.append("::");
            s.append(getSubFactionName());
        }

        s.append(")</b><br>");

        // if being checked by an admin, show his activity status.
        if (adminStatus) {
            s.append("Activity Status: ");
            if (getDutyStatus() == STATUS_FIGHTING) {
                s.append("fighting<br>");
            } else if (getDutyStatus() == STATUS_ACTIVE) {
                s.append("active<br>");
            } else {
                s.append("inactive<br>");
            }

            if (getGroupAllowance() > 0) {
                s.append("IP Group Allowance: " + getGroupAllowance() + "<br>");
            }
        }

        s.append("  " + CampaignMain.cm.moneyOrFluMessage(true, false, getMoney()) + " //  " + CampaignMain.cm.moneyOrFluMessage(false, false, influence) + " // " + experience + " Experience<br>");

        // advanced repair
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            s.append("Technicians (Green/Reg/Vet/Elite): " + getTotalTechs().elementAt(UnitUtils.TECH_GREEN) + "/" + getTotalTechs().elementAt(UnitUtils.TECH_REG) + "/" + getTotalTechs().elementAt(UnitUtils.TECH_VET) + "/" + getTotalTechs().elementAt(UnitUtils.TECH_ELITE) + "<br>");
            s.append("Idle Techs (Green/Reg/Vet/Elite):  " + getAvailableTechs().elementAt(UnitUtils.TECH_GREEN) + "/" + getAvailableTechs().elementAt(UnitUtils.TECH_REG) + "/" + getAvailableTechs().elementAt(UnitUtils.TECH_VET) + "/" + getAvailableTechs().elementAt(UnitUtils.TECH_ELITE) + "<br>");
            s.append("Bays: " + getFreeBays() + "/" + getTotalMekBays() + "<br>");
            s.append("Leased Bays: " + getBaysOwned() + " (Cost: " + CampaignMain.cm.moneyOrFluMessage(true, false, getCurrentTechPayment()) + "/Game)<br>");
        }

        // normal techs
        else {
            s.append("Technicians (Idle/Total): " + getFreeBays() + "/" + getTotalMekBays() + "<br>");
            s.append("Paid Technicians: " + getTechnicians() + " (Cost: " + CampaignMain.cm.moneyOrFluMessage(true, false, getCurrentTechPayment()) + "/Game)<br>");
        }

        // give the players some basic vote info. should use /c myvotes to get
        // full vote info
        if (Boolean.parseBoolean(getMyHouse().getConfig("VotingEnabled"))) {
            int votesCast = CampaignMain.cm.getVoteManager().getAllVotesBy(this).size();
            int votesAllowed = getNumberOfVotesAllowed();
            if (votesAllowed == votesCast) {
                s.append("Votes: All votes cast (" + votesCast + "/" + votesAllowed + ").<br>");
            } else {
                s.append("Votes: " + votesCast + " votes cast. " + votesAllowed + " votes allowed. (" + votesCast + "/" + votesAllowed + ").<br>");
            }
        }// end if(voting is allowed)

        if (!Boolean.parseBoolean(getMyHouse().getConfig("HideELO")) && !adminStatus) {
            s.append("Rating: " + myFormatter.format(getRating()) + "<br>");
        }
        if (Boolean.parseBoolean(getMyHouse().getConfig("ShowReward"))) {
            s.append("Current " + CampaignMain.cm.getConfig("RPLongName") + ": " + getReward() + " (Maximum  of " + Integer.parseInt(getMyHouse().getConfig("XPRewardCap")) + ")<br>");
        }

        // if merc show their status.
        if (myHouse.isMercHouse()) {
            s.append("<br>" + getReadableMercStatus());
        }

        s.append("<br>");

        if ((Integer.parseInt(getMyHouse().getConfig("NoPlayListSize")) > 0) || (exclusionList.getAdminExcludes().size() > 0)) {

            // player no-play
            s.append("<b>No-Play List:</b> ");
            Enumeration<String> en = exclusionList.getPlayerExcludes().elements();
            if (en.hasMoreElements()) {
                s.append(en.nextElement());
            } else {
                s.append("empty");
            }

            while (en.hasMoreElements()) {
                s.append(", " + en.nextElement());
            }
            s.append("<br>");

            // admin no-plays
            s.append("<b>No-Play (Admin):</b> ");
            en = exclusionList.getAdminExcludes().elements();
            if (en.hasMoreElements()) {
                s.append(en.nextElement());
            } else {
                s.append("empty");
            }
            while (en.hasMoreElements()) {
                s.append(", " + en.nextElement());
            }

            s.append("<br><br>");
        }

        s.append("<b>Current Armies:<br></b>");

        if (armies.size() == 0) {
            s.append("(No armies constructed)<br>");
        } else {
            // proceed to list lances and hangar contents
            for (SArmy currA : armies) {
                if (adminStatus) {
                    s.append(currA.getDescription(true, true, false) + "<br>");
                } else {
                    s.append(currA.getDescription(true, true, true) + "<br>");
                }
            }
        }

        s.append("<br><b>Contents of Hangar:</b><br>");
        for (SUnit currU : units) {

            if (currU.getStatus() == Unit.STATUS_FORSALE) {
                continue;
            }

            if (adminStatus) {
                s.append(currU.getDescription(false) + "<br>");
            } else {
                s.append(currU.getDescription(true) + "<br>");
            }
        }

        // Get info for units the player is selling on Market2
        StringBuilder saleUnits = new StringBuilder();
        for (SUnit currU : units) {

            if (currU.getStatus() != Unit.STATUS_FORSALE) {
                continue;
            }

            if (adminStatus) {
                saleUnits.append(currU.getDescription(false) + "<br>");
            } else {
                saleUnits.append(currU.getDescription(true) + "<br>");
            }
        }

        // only include sale heading if units are actually on market
        if (saleUnits.length() > 0) {
            s.append("<br><b>Units on Market:</b><br>");
            s.append(saleUnits);
        }

        s.append("<br>");

        // Return the player's PlayerFlags
        s.append("<b>Player Flags</b><br>");
        PlayerFlags pFlags = this.getFlags();
        for (String flag : pFlags.getFlagNames()) {
        	s.append(flag + ": " + Boolean.toString(pFlags.getFlagStatus(flag)) + "<br>");
        }

        return s.toString();
    }

    /**
     * Method that returns a human readable string containing special info
     * pertinent to mercenaries, such an employer and contract terms.
     */
    public String getReadableMercStatus() {
        String s = "";
        if (myHouse.isMercHouse()) {// if a merc
            s = "Mercenary information for " + getName() + ": <br>";// list name
            s += "Currently fighting for: " + (((MercHouse) myHouse).getHouseFightingFor(this)).getName() + "<br>";// list
            // employing
            // faction
            ContractInfo contract = (((MercHouse) myHouse).getContractInfo(this));
            if (contract != null) {
                s += contract.getInfo(this);
            } else {
                s += "Contract Status: Currently avaliable for hire <br>";
            }
            s += "<br>";
        }
        return s;
    }

    // TOSTRING AND FROMSTRING METHODS
    // @salient - "Seems like instead of a boolean, this should be two separate methods.
    //            One for client. one for server."
    /*
     * These would normally be under the "methods" heading; however, they're so
     * huge (and important) that they get a separate block.
     */
    public String toString(boolean toClient) {
    	SerializedMessage result = new SerializedMessage("~");
        result.append("CP");
        result.append(name);
        result.append(money);
        result.append(experience);
        result.append(units.size());
        if (units.size() > 0) {
            synchronized (units) {
                for (SUnit currU : units) {
                    currU.getPilot().setCurrentFaction(myHouse.getName());
                    result.append(currU.toString(toClient));
                }
            }
        }
        result.append(armies.size());
        for (int i = 0; i < armies.size(); i++) {
            result.append(armies.elementAt(i).toString(toClient, "%"));
        }
        if (!toClient) {
            if (getMyHouse() != null) {
                result.append(getMyHouse().getName());
            } else {
                result.append(CampaignMain.cm.getConfig("NewbieHouseName"));
            }
            result.append(lastOnline);

        }
        result.append(getTotalMekBays());
        result.append(getFreeBays());

        if (toClient) {
            if (Boolean.parseBoolean(getMyHouse().getConfig("HideELO"))) {
                result.append("0");
            } else {
                result.append(getRatingRounded());
            }
        } else {
            result.append(rating);
        }
        result.append(influence);
        if (!toClient) {
            result.append(fluffText + " ");
            /*
             * In older code, player-prefered game options were saved here. This
             * feature has been eliminated. Because of terrible coding (using
             * the standard ~ delimiter instead of an inner delimiter like $),
             * we can't eliminate the read in without endangering older saves.
             * We'll just save a 0 for now. Sometime in the future, this space
             * can be reclaimed. @urgru 12.28.05
             */
             //@salient - i'm going to reclaim it then for fluXProllover :)
            result.append(getXpTillFlu());
        }

        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            result.append(getBaysOwned());
        } else {
            result.append(technicians);// used when saving to houses.dat
        }
        // above is used when sending to client bad hack but needed for now
        result.append(rewardPoints); // saving current reward points
        /*
         * In older code, player's price modifier (mezzo) was saved here. This
         * feature has been eliminated, and the spaces can be reclaimed. @urgru
         * 9.30.06
         */
        //@salient reclaiming this for mini campaign data
        if (!toClient) {
            result.append(saveStatusMC());
        }

        result.append(getMekToken());

        result.append(myHouse.getName() + " ");
        if (toClient) {
            result.append(getHouseFightingFor().getName() + " ");
            if (getMyLogo().length() == 0) {
                result.append(myHouse.getLogo() + " ");
            } else {
                result.append(getMyLogo() + " ");
            }
        } else {
            result.append(xpTillReward);
            result.append(getBVTracker()); //@Salient for mini campaigns
            result.append(getPersonalPilotQueue().toString(toClient));
            result.append(getExclusionList().adminExcludeToString("$"));
            result.append(getExclusionList().playerExcludeToString("$"));

            if (CampaignMain.cm.isUsingAdvanceRepair()) {
                result.append(totalTechsToString());
                result.append(availableTechsToString());
                result.append(baysOwned);
            } else {
                result.append(" ");
                result.append(" ");
                result.append(technicians);
            }
            if (getMyLogo().trim().length() == 0) {
                result.append(myHouse.getLogo() + " ");
            } else {
                result.append(getMyLogo() + " ");
            }
            result.append(getLastAttackFromReserve());
            result.append(getGroupAllowance());
            if (lastISP.length() < 1) {
                result.append(" ");
            } else {
                result.append(lastISP);
            }
        }
        result.append(isInvisible());
        if (!toClient) {
            result.append(groupAllowance);
            if (password != null) {
                result.append(password.getAccess());
                result.append(password.getPasswd());
                result.append(password.getTime());
            } else {
                result.append("0");
                result.append(" ");
                result.append("0");
            }
        }
        result.append(unitParts.toString("|"));
        result.append(getAutoReorder());
        if (!toClient) {
            result.append(getTeamNumber());
            if (getSubFactionName().trim().length() < 1) {
                result.append(" ");
            } else {
                result.append(getSubFactionName());
            }
            result.append(getLastPromoted());
        }
        result.append(exportFlags().length() > 1 ? exportFlags() : CampaignMain.cm.getDefaultPlayerFlags().export());
        result.append(saveDiscordInfo()); //@salient adding new field to save
        return result.getMessage();
    }

    /**
     * @author jtighe
     * @param s
     *            - string from a pfile Used for sperate pfiles with faction
     *            name stuck on the end.
     */
    public void fromString(String s) {

        if (s == null) {
            throw new NullPointerException("SPlayer fromString(s) is null");
        }

        // print the player into the info log. only for Debug
        // MWLogger.infoLog("CSPlayer: " + s);
        isLoading = true;

        try {
            armies.clear();

            s = s.substring(3);
            StringTokenizer ST = new StringTokenizer(s, "~");
            setName(TokenReader.readString(ST));

            /*
             * name is set before the exclusion list is un-strung in
             * SPlayer.fromString(). Use this opportunity to set it in the
             * ExclusionList so strip/error messages can be sent back to the
             * player properly. Uber-Hacky, but functional.
             *
             * @urgru 4.2.05
             */
            exclusionList.setOwnerName(name);

            money = TokenReader.readInt(ST);
            experience = TokenReader.readInt(ST);

            int numofarmies = 0;
            int numofUnits = TokenReader.readInt(ST);
            units = new Vector<SUnit>(1, 1);

            for (int i = 0; i < numofUnits; i++) {
                SUnit m = new SUnit();
                m.fromString((String) ST.nextElement());
                units.add(m);
                CampaignMain.cm.toUser("PL|HD|" + m.toString(true), name, false);
            }

            numofarmies = (Integer.parseInt((String) ST.nextElement()));
            for (int i = 0; i < numofarmies; i++) {
                SArmy a = new SArmy(name);
                a.fromString((String) ST.nextElement(), "%", this);
                if (armies.size() < a.getID()) {
                    armies.add(a);
                } else {
                    armies.add(a.getID(), a);
                }
                CampaignMain.cm.toUser("PL|SAD|" + a.toString(true, "%"), name, false);
            }

            setMyHouse(CampaignMain.cm.getHouseFromPartialString(TokenReader.readString(ST), null));

            lastOnline = TokenReader.readLong(ST);
            // Just read it. It's not necessary to use it on the server..
            // It's useful for the client
            TokenReader.readString(ST);// Number of Bays
            TokenReader.readString(ST);// Number of Free Bays

            rating = TokenReader.readDouble(ST);
            influence = TokenReader.readInt(ST);

            fluffText = TokenReader.readString(ST).trim();

            //@Salient i've reclaimed unused token for xp till flu injection
            setXpTillFlu(TokenReader.readInt(ST));

            if (CampaignMain.cm.isUsingAdvanceRepair()) {
                int greenTechs = TokenReader.readInt(ST);
                int regTechs = greenTechs / 5;
                greenTechs -= regTechs;
                updateAvailableTechs(greenTechs + "%" + regTechs + "%0%0%");
                getTotalTechs().addAll(getAvailableTechs());
                // give them some bays
                setBaysOwned(greenTechs + regTechs);
            } else {
                // technicians = TokenReader.readInt(ST);
                int te = TokenReader.readInt(ST);
                int mt = CampaignMain.cm.getIntegerConfig("MaxTechsToHire");
                technicians = (mt != -1) ? Math.min(te, mt) : te;
            }

            rewardPoints = TokenReader.readInt(ST);

            //@salient reclaimed token for mini campaign data
            loadStatusMC(TokenReader.readString(ST));

            setMekToken(TokenReader.readInt(ST));

            myHouse = CampaignMain.cm.getHouseFromPartialString(TokenReader.readString(ST));

            if (myHouse == null) {
                myHouse = CampaignMain.cm.getHouseFromPartialString(CampaignMain.cm.getConfig("NewbieHouseName"));
            }

            setXpTillReward(TokenReader.readInt(ST));

            setBVTracker(TokenReader.readInt(ST)); // @Salient for mini campaigns
            //TokenReader.readString(ST);

            getPersonalPilotQueue().fromString(TokenReader.readString(ST), "$");

            getExclusionList().adminExcludeFromString(TokenReader.readString(ST), "$");

            getExclusionList().playerExcludeFromString(TokenReader.readString(ST), "$");

            {
                try {
                    if (CampaignMain.cm.isUsingAdvanceRepair()) {
                        updateTotalTechs(TokenReader.readString(ST));

                        updateAvailableTechs(TokenReader.readString(ST));

                        setBaysOwned(TokenReader.readInt(ST));
                    }// get rid of the 3 blanks
                    else {
                        TokenReader.readString(ST);

                        TokenReader.readString(ST);
                        // allow servers to go back and forth using Bays as
                        // techs since bays are what techs are.
                        {
                            if (technicians <= 0) {
                                technicians = TokenReader.readInt(ST);
                            } else {
                                TokenReader.readString(ST);
                            }
                        }
                    }
                }// Had alot of problems with advanced repair so lets just
                // use this.
                catch (Exception ex) {
                }
            }// get rid of the 2 blanks

            myLogo = TokenReader.readString(ST);

            // Stupid error with player logo if its blank it doesn't save
            // anything
            // and gets skipped.
            // Thats been fixed but for all the PFiles out there with the defect
            // this will allow them to
            // Still Load.
            try {

                setLastAttackFromReserve(TokenReader.readLong(ST));
                setGroupAllowance(TokenReader.readInt(ST));
                setLastISP(TokenReader.readString(ST));
                setInvisible(TokenReader.readBoolean(ST));
                setGroupAllowance(TokenReader.readInt(ST));
            } catch (Exception ex) {
            }

            try {
                int access = TokenReader.readInt(ST);
                String passwd = TokenReader.readString(ST);
                long time = TokenReader.readLong(ST);

                if (passwd.trim().length() > 2) {
                    setPassword(new MWPasswdRecord(name, access, passwd, time, ""));
                }
            } catch (Exception ex) {
                // Issue with password loading just stop now.
                isLoading = false;
                return;
            }
            if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
                unitParts.fromString(TokenReader.readString(ST), "|");
            } else {
                TokenReader.readString(ST);
            }

            setAutoReorder(TokenReader.readBoolean(ST));


            setTeamNumber(TokenReader.readInt(ST));
            subFaction = TokenReader.readString(ST);
            lastPromoted = TokenReader.readLong(ST);

            loadFlags(CampaignMain.cm.getDefaultPlayerFlags().export());


        	String flagString = TokenReader.readString(ST);
        	if (flagString.length() > 1)
        		flags.loadPersonal(flagString);


            if (ST.hasMoreTokens()) {
            	//@salient... i sure hope this doesnt break anything
            	String discordData = TokenReader.readString(ST);
            	if (CampaignMain.cm.getBooleanConfig("Enable_BotPlayerInfo")) {
            		loadDiscordInfo(discordData);
            	} 	
            }
            	

            if ((password != null) && (password.getPasswd().trim().length() <= 2)) {
                password.setAccess(IAuthenticator.GUEST);
            }

            CampaignMain.cm.toUser("PL|SB|" + getTotalMekBays(), name, false);
            CampaignMain.cm.toUser("PL|SF|" + getFreeBays(), name, false);
            if (CampaignMain.cm.isUsingAdvanceRepair()) {

                if (!this.hasRepairingUnits()) {
                    CampaignMain.cm.toUser("PL|UTT|" + totalTechsToString(), name, false);
                    CampaignMain.cm.toUser("PL|UAT|" + totalTechsToString(), name, false);
                    updateAvailableTechs(totalTechsToString());// make
                    // sure
                    // techs
                    // are
                    // in
                    // synch
                } else {
                    CampaignMain.cm.toUser("PL|UTT|" + totalTechsToString(), name, false);
                    CampaignMain.cm.toUser("PL|UAT|" + availableTechsToString(), name, false);
                }
            }

            healAllPilots();

            /*
             * Check all units for bad ammo or illegal/mis-set vacant pilots.
             * This was being done at the same time as the units are unstrung,
             * but caused a null b/c fixAmmo() uses .myHouse(), which is null at
             * that point in the unstring. If the units are changed as a result
             * of the checks, a PL|UU is sent, as well as a PL|SAD for each army
             * that includes the unit.
             */
            for (SUnit currU : units) {
                fixPilot(currU);
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        } finally {
            isLoading = false;
        }
    }

    /**
     * Issue with vacant pilots getting placed in !Mek and !Proto Units This
     * fixes it. Will also be helpful if future bugs cause vacant pilots.
     *
     * @param unit
     */
    private void fixPilot(SUnit unit) {

        if (!unit.hasVacantPilot()) {
            return;
        }

        if (Boolean.parseBoolean(getMyHouse().getConfig("AllowPersonalPilotQueues")) && unit.isSinglePilotUnit()) {
            return;
        }

        // set a new pilot
        SPilot pilot = getMyHouse().getNewPilot(unit.getType());
        unit.setPilot(pilot);

        // send an update to the player
        CampaignMain.cm.toUser("PL|UU|" + unit.getId() + " |" + unit.toString(true), name, false);

        // correct the BV of any army which contains the unit
        for (SArmy currA : armies) {
            if (currA.getUnit(unit.getId()) != null) {
                currA.setBV(0);
                CampaignMain.cm.toUser("PL|SAD|" + currA.toString(true, "%"), name, false);
                CampaignMain.cm.getOpsManager().checkOperations(currA, true);// update
                // legal
                // operations
            }
        }

    }

    public UnitComponents getUnitParts() {
        return unitParts;
    }

    public int getPartsAmount(String part) {
        int amount = 0;
        amount += getHouseFightingFor().getPartsAmount(part);
        amount += getUnitParts().getPartsCritCount(part);
        return amount;
    }

    public void updatePartsCache(String part, int amount) {

        if (amount < 0) {
            int playerAmount = getUnitParts().getPartsCritCount(part);

            if (playerAmount >= Math.abs(amount)) {
                getUnitParts().remove(part, amount);
            } else {
                amount += playerAmount;
                getHouseFightingFor().updatePartsCache(part, amount);
                getUnitParts().remove(part, playerAmount);
                amount = -playerAmount;
            }
        } else {
            getUnitParts().add(part, amount);
        }
        CampaignMain.cm.toUser("PL|UPPC|" + part + "#" + amount, getName(), false);

    }

    public SArmy getLockedArmy() {
        for (SArmy army : getArmies()) {
            if (!army.isLocked()) {
                continue;
            }
            return army;
        }

        return null;
    }

    public int getDBId() {
        return DBId;
    }

    public void setDBId(int id) {
        DBId = id;
        personalPilotQueue.setOwnerID(id);
    }

    public void setForumID(int id) {
        forumID = id;
    }

    public int getForumID() {
        return forumID;
    }

    @Override
    public void setTeamNumber(int team) {
        super.setTeamNumber(team);
        setSave();
    }

    public void setSubFaction(String subFaction) {
        this.subFaction = subFaction;
        if (getSubFactionAccess() != 0) {
            setLastPromoted(System.currentTimeMillis());
        }
        setSave();
    }

    public SubFaction getSubFaction() {

        SubFaction sub = getMyHouse().getSubFactionList().get(subFaction);

        if (sub == null) {
            return new SubFaction();
        }

        return sub;
    }

    public int getSubFactionAccess() {
        SubFaction sub = getMyHouse().getSubFactionList().get(subFaction);

        if (sub == null) {
            return 0;
        }

        return Integer.parseInt(sub.getConfig("AccessLevel"));

    }

    public String getSubFactionName() {
        return subFaction;
    }

    public boolean playerIsLoading() {
        return isLoading;
    }

    public boolean canBePromoted() {

        if (getMyHouse().getSubFactionList().size() < 1) {
            return false;
        }

        int days = getMyHouse().getIntegerConfig("daysbetweenpromotions");

        long day = 1000 * 60 * 60 * 24;

        try {
            long daysSinceLastPromoted = (System.currentTimeMillis() - getLastPromoted()) / day;

            // They've been promoted in the last number of days so they are not
            // eligible for a check.
            if (daysSinceLastPromoted < days) {
                return false;
            }
        } catch (Exception ex) {
            MWLogger.errLog(ex);
            return false;
        }

        return true;

    }

    public void checkForPromotion() {

    	if(CampaignMain.cm.getBooleanConfig("Disable_Promote_Subfaction")) //@salient
    	{
    		return;
    	}

        if (!canBePromoted()) {
            return;
        }

        int currentAccessLevel = getSubFactionAccess();

        for (SubFaction subFaction : getMyHouse().getSubFactionList().values()) {

            if ((currentAccessLevel < Integer.parseInt(subFaction.getConfig("AccessLevel"))) && (getRating() >= Integer.parseInt(subFaction.getConfig("MinELO"))) && (getExperience() >= Integer.parseInt(subFaction.getConfig("MinExp")))) {
                CampaignMain.cm.toUser("You are eligible for a promotion to subFaction " + subFaction.getConfig("Name") + ". <a href=\"MEKWARS/c RequestSubFactionPromotion#" + subFaction.getConfig("Name") + "\">Click here to request promotion.</a>", getName());
            }

        }
    }

    public void checkForDemotion() {

    	if(CampaignMain.cm.getBooleanConfig("Disable_Demote_Subfaction")) //@salient
    	{
    		return;
    	}

        SubFaction subfaction = getSubFaction();

        int access = getSubFactionAccess();
        int elo = Integer.parseInt(subfaction.getConfig("MinELO"));
        int exp = Integer.parseInt(subfaction.getConfig("MinExp"));

        // can go any lower
        if (access < 1) {
            return;
        }

        // Auto Promotes and Demotes no need to inform anyone
        if (CampaignMain.cm.getBooleanConfig("autoPromoteSubFaction")) {
        	SubFaction newSF = null;
        	for (SubFaction subFaction : getMyHouse().getSubFactionList().values()) {
        		if ((access > Integer.parseInt(subFaction.getConfig("AccessLevel"))) && (getRating() >= Integer.parseInt(subFaction.getConfig("MinELO"))) && (getExperience() >= Integer.parseInt(subFaction.getConfig("MinExp")))) {
        			if (newSF == null) {
        				newSF = subFaction;
        			} else if (Integer.parseInt(subFaction.getConfig("AccessLevel")) > Integer.parseInt(newSF.getConfig("AccessLevel"))) {
        				newSF = subFaction;
        			}
        		}
        	}

        	if(newSF == null) {
        		return;  // Nothing to demote him to
        	}
        	SubFaction subFaction = newSF;
            String subFactionName = subFaction.getConfig("Name");
            setSubFaction(subFactionName);
            CampaignMain.cm.toUser("PL|SSN|" + subFactionName, getName(), false);
            CampaignMain.cm.doSendToAllOnlinePlayers("PI|FT|" + getName() + "|" + getFluffText(), false);
            CampaignMain.cm.toUser("HS|CA|0", getName(), false);// clear
            // old
            // data
            CampaignMain.cm.toUser(getMyHouse().getCompleteStatus(), getName(), false);
            for (SArmy army : getArmies()) {
                CampaignMain.cm.getOpsManager().checkOperations(army, true);
            }

            CampaignMain.cm.toUser("AM:You have been demoted to SubFaction " + subFactionName + ".", getName());
            CampaignMain.cm.doSendHouseMail(getMyHouse(), "NOTE", getName() + " has been demoted to subfaction " + subFactionName + " by the Faction Leadership!");
            return;
        }

        if (((elo > getRating()) || (exp > getExperience()))  && !CampaignMain.cm.getBooleanConfig("disableDemotionNotification") && !CampaignMain.cm.getBooleanConfig("autoPromoteSubFaction") ) {
            StringBuilder message = new StringBuilder(name);
            message.append(" no longer meets the eligbility requirements for subfaction ");
            message.append(getSubFactionName());
            message.append(". He is eligible for the following:<br>");
            for (SubFaction subFaction : getMyHouse().getSubFactionList().values()) {

                if ((access > Integer.parseInt(subFaction.getConfig("AccessLevel"))) && (getRating() >= Integer.parseInt(subFaction.getConfig("MinELO"))) && (getExperience() >= Integer.parseInt(subFaction.getConfig("MinExp")))) {
                    message.append(subFaction.getConfig("Name"));
                    message.append(". <a href=\"MEKWARS/c demoteplayer#");
                    message.append(getName());
                    message.append("#");
                    message.append(subFaction.getConfig("Name"));
                    message.append("\">Click here to demote.</a><br>");
                }

            }


            message.append("None");
            message.append(". <a href=\"MEKWARS/c demoteplayer#");
            message.append(getName());
            message.append("#");
            message.append("None");
            message.append("\">Click here to demote.</a><br>");

            getMyHouse().sendMessageToHouseLeaders(message.toString());
        }
    }

    public long getLastPromoted() {
        return lastPromoted;
    }

    public void setLastPromoted(long promotedTime) {
        lastPromoted = promotedTime;
    }

    /**
     * A player may only have 1 army locked at a time. This will lock that army
     * and unlock any others Passing an armyId of -1 will unlock all armies.
     *
     * @param armyId
     */
    public void lockArmy(int armyId) {

        for (SArmy army : getArmies()) {

            if (army.getID() == armyId) {
                army.setLocked(true);
                CampaignMain.cm.toUser("PL|SAL|" + armyId + "#" + true, getName(), false);
            } else if (army.isLocked()) {
                army.setLocked(false);
                CampaignMain.cm.toUser("PL|SAL|" + army.getID() + "#" + false, getName(), false);
            }
        }
    }

    public void setUserValidated(boolean validated) {
        userValidated = validated;
    }

    public boolean isValidated() {
        return userValidated;
    }

    /**
     * A method to determine if a player is above or below the hangar limits for
     * units, based on type and weight.
     *
     * @param uType
     *            - type of unit (Unit.MEK, Unit.VEHICLE, etc.)
     * @param uWeightClass
     *            - weightclass of unit (Unit.LIGHT, Unit.MEDIUM, etc)
     * @return true if the player is below the limit, false if he's at or above
     */
    public boolean hasRoomForUnit(int uType, int uWeightClass) {
        if ((uType < 0) || (uType > Unit.AERO)) {
            MWLogger.errLog("Invalid uType in SPlayer.hasRoomForUnit: " + uType);
            return false;
        }
        if ((uWeightClass < 0) || (uWeightClass > Unit.ASSAULT)) {
            MWLogger.errLog("Invalid uWeightClass in SPlayer.hasRoomForUnit: " + uWeightClass);
            return false;
        }
        int limit = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getName()).getUnitLimit(uType, uWeightClass);

        if (limit < 0) {
            // Unlimited
            return true;
        }

        int inHangar = countUnits(uType, uWeightClass);

        if (inHangar < limit) {
            return true;
        }

        if ((inHangar >= limit)  && Boolean.parseBoolean(getMyHouse().getConfig("UseSlidingHangarLimits")) ) {
        	return true;
        }

        return false;
    }

    /**
     * A method to count the units of a given type and weight in a player's
     * hangar
     *
     * @param uType
     * @param uWeightClass
     * @return number of units
     */
    public int countUnits(int uType, int uWeightClass) {
        if ((uType < 0) || (uType > Unit.AERO)) {
            MWLogger.errLog("Invalid uType in SPlayer.countUnits: " + uType);
            return 0;
        }
        if ((uWeightClass < 0) || (uWeightClass > Unit.ASSAULT)) {
            MWLogger.errLog("Invalid uWeightClass in SPlayer.countUnits: " + uWeightClass);
            return 0;
        }
        // Actually count them now
        int count = 0;
        for (SUnit u : units) {
            if (!u.isChristmasUnit() && (u.getType() == uType) && (u.getWeightclass() == uWeightClass)) {
                count++;
            }
        }
        return count;
    }

    /**
     * A method to determine if any of the unit limits have been exceeded
     *
     * @return true if any limits are exceeded, false otherwise
     */
    public boolean isOverAnyUnitLimits() {
        for (int t = Unit.MEK; t <= Unit.AERO; t++) {
            for (int w = Unit.LIGHT; w <= Unit.ASSAULT; w++) {
                int limit = getMyHouse().getUnitLimit(t, w);
                int inHangar = countUnits(t, w);
                if ((limit != -1) && (inHangar > limit)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * @author Salient
     * A method to determine if player is at the unit limits
     *
     * @return true if at all limits, false otherwise
     */
    public boolean isAtUnitLimits()
    {
    	boolean result = false;

    	boolean dontCountAero = CampaignMain.cm.getBooleanConfig("IgnoreAeroUnitLimit");

    	int uType = Unit.AERO;

    	if (dontCountAero)
    		uType = Unit.BATTLEARMOR;

        for (int t = Unit.MEK; t <= uType; t++)
        {
            for (int w = Unit.LIGHT; w <= Unit.ASSAULT; w++)
            {
                int limit = getMyHouse().getUnitLimit(t, w);
                int inHangar = countUnits(t, w);

                if(limit != -1)
                {
                	if (inHangar == limit)
                		result = true;
                	else
                		return false;
                }
            }
        }
        return result;
    }

    /**
     * @author Salient
     * A method to determine if player is at or over the unit limits
     *
     * @return true if at or over all limits, false otherwise
     */
    public boolean isAtOrOverUnitLimits()
    {
    	boolean result = false;

    	boolean dontCountAero = CampaignMain.cm.getBooleanConfig("IgnoreAeroUnitLimit");

    	int uType = Unit.AERO;

    	if (dontCountAero)
    		uType = Unit.BATTLEARMOR;

        for (int t = Unit.MEK; t <= uType; t++)
        {
            for (int w = Unit.LIGHT; w <= Unit.ASSAULT; w++)
            {
                int limit = getMyHouse().getUnitLimit(t, w);
                int inHangar = countUnits(t, w);

                if(limit != -1)
                {
                	if (inHangar >= limit)
                		result = true;
                	else
                		return false;
                }
            }
        }
        return result;
    }

    /**
     * A method to determine if the player will be over the unit limit
     * after purchasing a new unit if the server is configured
     * to use sliding hangar cost increases
     *
     */
    public boolean willHaveHangarPenalty(int uType, int uWeight) {
    	// Always false if we're not using the sliding limits
    	if (!Boolean.parseBoolean(getMyHouse().getConfig("UseSlidingHangarLimits"))) {
    		return false;
    	}

    	int limit = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getName()).getUnitLimit(uType, uWeight);

    	// Always false if the particular limit is not checked
    	if (limit < 0) {
    		return false;
    	}

    	// Need to add one, since we're checking what it will be after a purchase
    	int numUnits = countUnits(uType, uWeight) + 1;
    	// False if we're below the limit
    	if (limit >= numUnits) {
    		return false;
    	}

    	return true;
    }

    /**
     * A method to determine if the player is over the unit limit
     * and the server is configured to use sliding hangar cost
     * increases
     *
     *
     */
    public boolean hasHangarPenalty(int uType, int uWeight) {
    	// Always false if we're not using the sliding limits
    	if (!Boolean.parseBoolean(getMyHouse().getConfig("UseSlidingHangarLimits"))) {
    		return false;
    	}

    	int limit = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getName()).getUnitLimit(uType, uWeight);

    	// Always false if the particular limit is not checked
    	if (limit < 0) {
    		return false;
    	}

    	int numUnits = countUnits(uType, uWeight);
    	// False if we're below the limit
    	if (limit >= numUnits) {
    		return false;
    	}

    	return true;
    }

    /**
     * Calculates and returns the string to be sent to the client to
     * set both the maintenance penalty and the purchase price penalty
     * for each unit type and weight.
     *
     * @return
     */
    public String buildHangarPenaltyString() {
    	StringBuilder toReturn = new StringBuilder();

    	toReturn.append(Integer.toString(calculateTotalHangarPenalty()));

    	for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
    		for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
    			toReturn.append("*" + Integer.toString(calculateHangarPenaltyForNextPurchase(type, weight)));
    		}
    	}

    	return toReturn.toString();
    }

    public int calculateHangarPenaltyForNextPurchase(int type, int weight) {
    	int penalty = 0;

		int limit = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getName()).getUnitLimit(type, weight);
		int numUnits = countUnits(type, weight) + 1;

		if ((limit == -1) || (numUnits <= limit)) {
			return 0;
		}

		int penaltyUnits = numUnits - limit;

		penalty = (int)(Math.pow(penaltyUnits, Double.parseDouble(getMyHouse().getConfig("SlidingHangarLimitModifier"))));


    	return penalty;
    }

	public int calculateHangarPenalty(int type_id, int weightclass) {
		if(!hasHangarPenalty(type_id, weightclass)) {
			return 0;
		}
		int penalty = 0;

		int limit = CampaignMain.cm.getHouseFromPartialString(getMyHouse().getName()).getUnitLimit(type_id, weightclass);
		int numUnits = countUnits(type_id, weightclass);

		if (numUnits <= limit) {
			return 0;
		}

		int penaltyUnits = numUnits - limit;

		penalty = (int)(Math.pow(penaltyUnits, Double.parseDouble(getMyHouse().getConfig("SlidingHangarLimitModifier"))));

		return penalty;
	}

	public int calculateTotalHangarPenalty() {
		int penalty = 0;
		for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
			for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
				penalty += calculateHangarPenalty(type, weight);
			}
		}
		return penalty;
	}

	//@salient
	public boolean hasUnusedMekTokens()
	{

		if( getMekToken() < Integer.parseInt(getMyHouse().getConfig("FreeBuild_Limit")) )
		{
			return true;
		}

		return false;
	}

	//@salient send msg to self
	public void toSelf(String msg)
	{
		CampaignMain.cm.toUser(msg, getName(), true);
	}
}// end SPlayer()
