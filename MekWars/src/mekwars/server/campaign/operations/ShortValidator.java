/*
 * MekWars - Copyright (C) 2005 
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)
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
 * A utility which checks to see whether armies are valid
 * for particular short operations @ launch/join.
 * 
 * There are seperate validators for Long and Modifying
 * Operations.
 */
package server.campaign.operations;

// IMPORTS
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;

import common.House;
import common.Planet;
import common.Unit;
import common.campaign.operations.Operation;
import common.flags.PlayerFlags;
import common.util.MWLogger;
import megamek.common.BattleArmor;
import megamek.common.Protomech;
import server.campaign.CampaignMain;
import server.campaign.NewbieHouse;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.mercenaries.ContractInfo;
import server.campaign.operations.validation.I_SpreadValidator;
import server.campaign.operations.validation.PercentBVSpreadValidator;
import server.campaign.operations.validation.StandardBVSpreadValidator;

public class ShortValidator {

    // IVARS
    // back reference to the manager
    private OperationManager manager;

    /*
     * Shared failure reasons, common to checks of both attacking and defending
     * players/armies.
     * 
     * 0-199 reserved for common usage in the future.
     */
    public static final int SFAIL_COMMON_PROTOGROUPS = 1;// "ProtosMustbeGrouped",
    // whether protos
    // must be a
    // multiple of move
    // clumps
    public static final int SFAIL_COMMON_ELODIFFERENCE = 2;// "MaxELODifference",
    // Max difference in
    // ELO between
    // attacker and
    // defender.
    public static final int SFAIL_COMMON_TEAM_BV_EXCEEDED = 3;// The max
    // amount of the
    // BV for the op
    // + max
    // difference
    // for the
    // server is the
    // max bv of a
    // team.
    public static final int SFAIL_COMMON_NOT_ENOUGH_COMMANDERS = 4;// the army
    // does not
    // have
    // enough
    // unit
    // commanders
    // in it.
    public static final int SFAIL_COMMON_TOO_MANY_COMMANDERS = 5;// The army
    // has too
    // many unit
    // commanders
    // in it.
    public static final int SFAIL_COMMON_INSUFFICENT_SUBFACTION_ACCESS_LEVEL = 6;// cannot
    // defend/launch
    // this
    // Op
    // due
    // to
    // your
    // sub
    // factions
    // accesslevel
    // being
    // too
    // low
    public static final int SFAIL_COMMON_MAX_BV_DIFFERENCE = 7; // max bv or max
    // bv %
    // difference
    // has bene
    // exceeded
    public static final int SFAIL_COMMON_INFACTION_ATTACK = 8; // Intra-faction
    // attacks not
    // allowed.
    // public static final int SFAIL_COMMON = 9;
    // public static final int SFAIL_COMMON = 10;
    // public static final int SFAIL_COMMON = 11;
    // public static final int SFAIL_COMMON = 12;
    // public static final int SFAIL_COMMON = 13;
    // public static final int SFAIL_COMMON = 14;
    // public static final int SFAIL_COMMON = 15;
    // public static final int SFAIL_COMMON = 16;
    // public static final int SFAIL_COMMON = 17;
    // public static final int SFAIL_COMMON = 18;
    // public static final int SFAIL_COMMON = 19;
    // public static final int SFAIL_COMMON = 20;
    // public static final int SFAIL_COMMON = 21;
    // public static final int SFAIL_COMMON = 22;
    // public static final int SFAIL_COMMON = 23;
    // public static final int SFAIL_COMMON = 24;
    // public static final int SFAIL_COMMON = 25;
    // public static final int SFAIL_COMMON = 26;
    // public static final int SFAIL_COMMON = 27;
    // public static final int SFAIL_COMMON = 28;
    // public static final int SFAIL_COMMON = 29;
    // public static final int SFAIL_COMMON = 30;

    /*
     * Failure codes for attacker-specific checks.
     * 
     * 200-399 reserved for attacker usage in the future.
     */
    public static final int SFAIL_ATTACK_MAXBV = 200;// "MaxAttackerBV" - BV
    // ceiling, contruction
    // prop
    public static final int SFAIL_ATTACK_MINBV = 201;// "MinAttackerBV" - BV
    // floor, contruction
    // prop
    public static final int SFAIL_ATTACK_MAXMEKS = 202;// "MaxAttackerMeks" -
    // unit ceiling,
    // construction prop
    public static final int SFAIL_ATTACK_MINMEKS = 203;// "MinAttackerMeks" -
    // unit floor,
    // contruction prop
    public static final int SFAIL_ATTACK_MINSPEED = 204;// "MinAttackerWalk" and
    // "MinAttackerJump",
    // construction props
    public static final int SFAIL_ATTACK_MAXUNITTON = 205;// "MaxAttackerUnitTonnage",
    // construction prop
    public static final int SFAIL_ATTACK_MINUNITTON = 206;// "MinAttackerUnitTonnage",
    // construction prop
    public static final int SFAIL_ATTACK_NOMEKS = 207;// "AttackerAllowedMeks",
    // construction prop
    public static final int SFAIL_ATTACK_NOVEHS = 208;// "AttackerAllowedVehs",
    // construction prop
    public static final int SFAIL_ATTACK_NOINF = 209;// "AttackerAllowedInf",construction
    // prop
    public static final int SFAIL_ATTACK_NONORMINF = 210;// "AttackerPoweredInfAllowed,"
    // but unarmored inf
    // present,
    // contruction prop
    public static final int SFAIL_ATTACK_MAXARMYTON = 211;// "MaxTotalAttackerTonnage",
    // construction prop
    public static final int SFAIL_ATTACK_MINARMYTON = 212;// "MinTotalAttackerTonnage",
    // contruction prop
    public static final int SFAIL_ATTACK_MONEY = 213;// "AttackerCostMoney",
    // cost ... in MU
    public static final int SFAIL_ATTACK_INFLUENCE = 214;// "AttackerCostInfluence",
    // cost ... in Flu
    public static final int SFAIL_ATTACK_REWARD = 215;// "AttackerCostReward",
    // cost ... in RP
    public static final int SFAIL_ATTACK_MAXRATING = 216;// "MaxAttackerRating",
    // milestone
    public static final int SFAIL_ATTACK_MINRATING = 217;// "MinAttackerRating",
    // milestone
    public static final int SFAIL_ATTACK_MAXXP = 218;// "MaxAttackerXP",
    // milestone
    public static final int SFAIL_ATTACK_MINXP = 219;// "MinAttackerXP",
    // milestone
    public static final int SFAIL_ATTACK_MAXGAMES = 220;// "MaxAttackerGamesPlayed",
    // milestone
    public static final int SFAIL_ATTACK_MINGAMES = 221;// "MinAttackerGamesPlayed",
    // milestone
    public static final int SFAIL_ATTACK_OUTOFRANGE = 222;// Catchall for
    // range properties.
    public static final int SFAIL_ATTACK_NOOPPONENT = 223;// No matching
    // opponents for the
    // army, at all
    public static final int SFAIL_ATTACK_NOPLANDEF = 224;// Opponents, but
    // none eligible for
    // this planet
    public static final int SFAIL_ATTACK_NOTYPEDEF = 225;// Opponents, but
    // none eligible for
    // this op type
    public static final int SFAIL_ATTACK_SOLCANTATT = 226;// "AllowSOLToUse",
    // newbie player
    // cannot make the
    // attack
    public static final int SFAIL_ATTACK_NON_CONQ_A = 227;// "AllowNonConqToUse",
    // nonconq player
    // cannot make the
    // attack
    public static final int SFAIL_ATTACK_NEEDSFAC = 228;// "OnlyAgainstFactoryWorlds",
    // planet needs a
    // factory
    public static final int SFAIL_ATTACK_HASFAC = 229;// "OnlyAgainstNonFactoryWorlds",
    // may not have
    // factories
    public static final int SFAIL_ATTACK_MINUNITBV = 230;// "MinAttackerUnitBV"
    // - BV floor for
    // single units
    public static final int SFAIL_ATTACK_MAXUNITBV = 231;// "MaxAttackerUnitBV"
    // - BV ceiling for
    // single units
    public static final int SFAIL_ATTACK_MAXSPREAD = 232;// "MaxAttackerUnitBVSpread",
    // max difference
    // between high/low
    // unit BVs
    public static final int SFAIL_ATTACK_MINSPREAD = 233;// "MinAttackerUnitBVSpread",
    // min difference
    // between high/low
    // unit BVs
    public static final int SFAIL_ATTACK_FACTION = 234;// "LegalAttackFactions"
    // and
    // "IllegalAttackFactions"
    public static final int SFAIL_ATTACK_MINOWNERSHIP = 235;// "MinPlanetOwnership",
    // misc
    public static final int SFAIL_ATTACK_MAXOWNERSHIP = 236;// "MaxPlanetOwnership",
    // misc
    public static final int SFAIL_ATTACK_NEEDSHOME = 237;// "OnlyAgainstHomeWorlds",
    // planet must be a
    // homeworld
    public static final int SFAIL_ATTACK_HASHOME = 238;// "OnlyAgainstNonHomeWorlds",
    // planet may not not be
    // a homeworld
    public static final int SFAIL_ATTACK_MAXVEHICLES = 239;// "MaxAttackerVehicles"
    // - unit ceiling,
    // construction prop
    public static final int SFAIL_ATTACK_MINVEHICLES = 240;// "MinAttackerVehicles"
    // - unit floor,
    // contruction prop
    public static final int SFAIL_ATTACK_MAXINFANTRY = 241;// "MaxAttackerInfantry"
    // - unit ceiling,
    // construction prop
    public static final int SFAIL_ATTACK_MININFANTRY = 242;// "MinAttackerInfantry"
    // - unit floor,
    // contruction prop
    public static final int SFAIL_ATTACK_OMNIONLY = 243;// "AttackerOmniMeksOnly"
    // - attackers meks must
    // be omnis,
    // construction prop
    public static final int SFAIL_ATTACK_OPFLAGS = 244;// "AllowedPlanetFlags"-
    // planet does not
    // contain flags that
    // are allowed
    public static final int SFAIL_ATTACK_DOPFLAGS = 245;// "DisallowedPlanetFlags"
    // - planet does contain
    // flags that are not
    // allowed
    public static final int SFAIL_ATTACK_NOPOWERINF = 246;// "AttackerStandardInfAllowed",
    // but BA or protos
    // present
    public static final int SFAIL_ATTACK_NOCOUNTERS = 247;// "ForbidCounterAttacks",player
    // is under attack
    // and cannot
    // counter with this
    // op
    public static final int SFAIL_ATTACK_AFRONLY = 248;// "OnlyAllowedFromReserve",
    // but player is trying
    // to use while active
    public static final int SFAIL_ATTACK_ACTIVEONLY = 249;// "OnlyAllowedFromActive",
    // but player is
    // trying to use via
    // AttackFromReserve
    public static final int SFAIL_ATTACK_MAXNONINFANTRY = 250;// "MaxAttackerNonInfantry"
    // - unit
    // ceiling,
    // construction
    // prop
    public static final int SFAIL_ATTACK_MINNONINFANTRY = 251;// "MinAttackerNonInfantry"
    // - unit floor,
    // contruction
    // prop
    public static final int SFAIL_ATTACK_ELITE_PILOTS = 252;// Attacker has pilots that are below the
                                                            //  lowest skill total allowed thresh hold
    public static final int SFAIL_ATTACK_GREEN_PILOTS = 253;// Attacker has pilots that are above the
                                                            // highest skill total allowed thresh hold

    public static final int SFAIL_ATTACK_MAX_AERO = 254;// Max Number of Aeros an Attacker can have in an army
    public static final int SFAIL_ATTACK_MIN_AERO = 255;// Min Number of Aeros an Attacker can have in an army
    public static final int SFAIL_ATTACK_NOAEROS = 256;// "AttackerAllowedAeros",
    public static final int SFAIL_ATTACK_TECHBASE_TOO_MUCH_CLAN = 257; // Too many clan units
    public static final int SFAIL_ATTACK_TECHBASE_TOO_LITTLE_CLAN = 258; // Not enough clan units
    public static final int SFAIL_ATTACK_TOO_MANY_SUPPORT_UNITS = 259; // Too many designated support units
    public static final int SFAIL_ATTACK_TOO_FEW_SUPPORT_UNITS = 260; // Not enough designated support units
    
    public static final int SFAIL_ATTACK_MISSING_REQUIRED_FLAG = 261; // Attacker is missing a required flag
    public static final int SFAIL_ATTACK_HAS_BANNED_FLAG = 262; // Attacker has a flag set that is banned from the attack
    
    public static final int SFAIL_ATTACK_MAXJUMP = 263; // Attacker has a unit with too large a jump
    public static final int SFAIL_ATTACK_TOO_MANY_NONSUPPORT_UNITS = 264;
    public static final int SFAIL_ATTACK_TOO_FEW_NONSUPPORT_UNITS = 265;
    public static final int SFAIL_ATTACK_SKILLSUM_TOOHIGH = 266;
    public static final int SFAIL_ATTACK_SKILLSUM_TOOLOW = 267;
    
    /*
     * Failure codes for defender-specific checks.
     * 
     * 400-599 reserved for defender usage in the future.
     */
    public static final int SFAIL_DEFEND_MAXBV = 400;// property
    // "MaxDefenderBV" - BV
    // ceiling, contruction
    // property
    public static final int SFAIL_DEFEND_MINBV = 401;// property
    // "MinDefenderBV" - BV
    // floor, construction
    // property
    public static final int SFAIL_DEFEND_MAXMEKS = 402;// MaxDefenderMeks -
    // unit ceiling,
    // contruction prop
    public static final int SFAIL_DEFEND_MINMEKS = 403;// MinDefenderMeks -
    // unit floor,
    // contruction prop
    public static final int SFAIL_DEFEND_MINSPEED = 404;// MinDefenderJump and
    // MinDefenderWalk,
    // construction
    public static final int SFAIL_DEFEND_MAXUNITTON = 405;// MaxDefenderUnitTonnage,
    // construction prop
    public static final int SFAIL_DEFEND_MINUNITTON = 406;// MinDefenderUnitTonnage,
    // construction prop
    public static final int SFAIL_DEFEND_NOMEKS = 407;// "DefenderAllowedMeks",
    // construction prop
    public static final int SFAIL_DEFEND_NOVEHS = 408;// "DefenderAllowedVehs",
    // construction prop
    public static final int SFAIL_DEFEND_NOINF = 409;// "DefenderAllowedInf",
    // construction prop
    public static final int SFAIL_DEFEND_NONORMINF = 410;// "DefenderPoweredInfAllowed,"
    // but normal
    // infantry is
    // present
    public static final int SFAIL_DEFEND_MAXARMYTON = 411;// "MaxTotalDefenderTonnage",
    // construction prop
    public static final int SFAIL_DEFEND_MINARMYTON = 412;// "MinTotalDefenderTonnage",
    // construction prop
    public static final int SFAIL_DEFEND_MONEY = 413;// "DefenderCostMoney",
    // cost in MU
    public static final int SFAIL_DEFEND_INFLUENCE = 414;// "DefenderCostInfluence",
    // cost in Flu
    public static final int SFAIL_DEFEND_REWARD = 415;// "DefenderCostReward",
    // cost in RP
    public static final int SFAIL_DEFEND_MAXRATING = 416;// "MaxDefenderRating",
    // milestone
    public static final int SFAIL_DEFEND_MINRATING = 417;// "MinDefenderRating",
    // milestone
    public static final int SFAIL_DEFEND_MAXXP = 418;// "MaxDefenderXP",
    // milestone
    public static final int SFAIL_DEFEND_MINXP = 419;// "MinDefenderXP",
    // milestone
    public static final int SFAIL_DEFEND_MAXGAMES = 420;// "MaxDefenderGamesPlayed",
    // milestone
    public static final int SFAIL_DEFEND_MINGAMES = 421;// "MinDefenderGamesPlayed",
    // milestone
    // public static final int SFAIL_DEFEND = 422; --- HOLD! This synchs with
    // ATTACK_OUTOFRANGE. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 423; --- HOLD! This synchs with
    // ATTACK_NOOPPONENT. Skip/hold so other props can match
    public static final int SFAIL_DEFEND_NOTPLANDEF = 424;// --- HOLD! This
    // synchs with
    // ATTACK_NOPLANDEF.
    // public static final int SFAIL_DEFEND = 425; --- HOLD! This synchs with
    // ATTACK_NOTYPEDEF. Skip/hold so other props can match
    public static final int SFAIL_DEFEND_SOLCANTDEF = 426;// "AllowAgainstSOL",
    // newbie player
    // cannot defend the
    // type
    public static final int SFAIL_DEFEND_NON_CONQ_D = 427;// "AllowAgainstNonConq",
    // nonconq player
    // cannot defend the
    // type
    // public static final int SFAIL_DEFEND = 428; --- HOLD! This synchs with
    // SFAIL_ATTACK_NEEDSFAC. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 429; --- HOLD! This synchs with
    // SFAIL_ATTACK_HASFAC. Skip/hold so other props can match
    public static final int SFAIL_DEFEND_MINUNITBV = 430;// "MinDefenderUnitBV",
    // BV floor for
    // singe units
    public static final int SFAIL_DEFEND_MAXUNITBV = 431;// "MaxDefenderUnitBV",
    // BV ceiling for
    // single units
    public static final int SFAIL_DEFEND_MAXSPREAD = 432;// "MaxDefenderUnitBVSpread",
    // max difference
    // between high/low
    // unit BVs
    public static final int SFAIL_DEFEND_MINSPREAD = 433;// "MinDefenderUnitBVSpread",
    // min difference
    // between high/low
    // unit BVs
    public static final int SFAIL_DEFEND_FACTION = 434;// "LegalDefendFactions"
    // and
    // "IllegalDefendFactions"
    // public static final int SFAIL_DEFEND = 435; --- HOLD! This synchs with
    // ATTACK_MINOWNERSHIP. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 436; --- HOLD! This synchs with
    // ATTACK_MAXOWNERSHIP. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 437; --- HOLD! This synchs with
    // SFAIL_ATTACK_NEEDSHOME. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 438; --- HOLD! This synchs with
    // SFAIL_ATTACK_HASHOME. Skip/hold so other props can match
    public static final int SFAIL_DEFEND_MAXVEHICLES = 439;// MaxDefenderVehicles
    // - unit ceiling,
    // contruction prop
    public static final int SFAIL_DEFEND_MINVEHICLES = 440;// MinDefenderVehicles
    // - unit floor,
    // contruction prop
    public static final int SFAIL_DEFEND_MAXINFANTRY = 441;// MaxDefenderInfantry
    // - unit ceiling,
    // contruction prop
    public static final int SFAIL_DEFEND_MININFANTRY = 442;// MinDefenderInfantry
    // - unit floor,
    // contruction prop
    public static final int SFAIL_DEFEND_OMNIONLY = 443;// "DefenderOmniMeksOnly"
    // - defenders meks must
    // be omnis,
    // construction prop
    // public static final int SFAIL_DEFEND = 444; --- HOLD! This synchs with
    // ATTACK_OPFLAGS. Skip/hold so other props can match
    // public static final int SFAIL_DEFEND = 445; --- HOLD! This synchs with
    // ATTACK_DOPFLAGS. Skip/hold so other props can match
    public static final int SFAIL_DEFEND_NOPOWERINF = 446;// "DefenderStandardInfAllowed",
    // but BA or protos
    // present
    // public static final int SFAIL_DEFEND = 447; --- HOLD! This synchs with
    // ATTACK_NOCOUNTERS. Skip/hold so others props can match.
    // public static final int SFAIL_DEFEND = 448; --- HOLD! This synchs with
    // ATTACK_AFRONLY. Skip/hold so others props can match.
    // public static final int SFAIL_DEFEND = 449; --- HOLD! This synchs with
    // ATTACK_ACTIVEONLY. Skip/hold so others props can match.
    public static final int SFAIL_DEFEND_MAXNONINFANTRY = 450;// MaxDefenderInfantry
    // - unit ceiling, contruction prop
    public static final int SFAIL_DEFEND_MINNONINFANTRY = 451;// MinDefenderInfantry
    // - unit floor, contruction prop
    public static final int SFAIL_DEFEND_NON_CONQ_PLANET = 452;// Defending
    // this op would allow for conquer point exchange and planet isnon-conquer
    public static final int SFAIL_DEFEND_ELITE_PILOTS = 453;// Defender has pilots that are below the
                                                            // lowest skill total allowed thresh hold
    public static final int SFAIL_DEFEND_GREEN_PILOTS = 454;// Defender has pilots that are above the
                                                            // highest skill total allowed thresh hold

    public static final int SFAIL_DEFEND_MAX_AERO = 455;// Max Number of Aeros a defender can have in an army
    public static final int SFAIL_DEFEND_MIN_AERO = 456;// Min Number of Aeros a defender can have in an army
    public static final int SFAIL_DEFEND_NOAEROS  = 457;// "DefenderAllowedAeros",

    public static final int SFAIL_DEFEND_TECHBASE_TOO_MUCH_CLAN = 458; // Too many clan units
    public static final int SFAIL_DEFEND_TECHBASE_TOO_LITTLE_CLAN = 459; // Not enough clan units
    
    public static final int SFAIL_DEFEND_TOO_MANY_SUPPORT_UNITS = 460; // Too many designated support units
    public static final int SFAIL_DEFEND_TOO_FEW_SUPPORT_UNITS = 461; // Not enough designated support units
    
    public static final int SFAIL_DEFEND_MISSING_REQUIRED_FLAG = 462; // Defender is missing a required set flag
    public static final int SFAIL_DEFEND_HAS_BANNED_FLAG = 463; // Defender has set a banned flag
    
    public static final int SFAIL_DEFEND_MAXJUMP = 464; // Defender has a unit that jumps too far
    
    public static final int SFAIL_DEFEND_TOO_MANY_NONSUPPORT_UNITS = 465;
    public static final int SFAIL_DEFEND_TOO_FEW_NONSUPPORT_UNITS = 466;
    
    public static final int SFAIL_DEFEND_SKILLSUM_TOOHIGH = 467;
    public static final int SFAIL_DEFEND_SKILLSUM_TOOLOW = 468;
    
    
    // CONSTRUCTORS
    public ShortValidator(OperationManager m) {
        manager = m;
    }

    // METHODS
    /**
     * Method which checks a player's army vs. an operation type to see if the
     * army can be used to attack. This method may be called with 2 different
     * goals:
     * 
     * 1) "Real" validation compares the potential attacker with possible
     * defenders. This call is used when a player sends a /c attack command with
     * a specific ShortOp type intended.
     * 
     * 2) "Dry" validations look simply at the qualities of an attacking army to
     * see whether or not it is valid for a given operation type. Players can
     * trigger this run-mode by calling by /c eligibility. It's also used to
     * check armies as players add/remove units.
     * 
     * @param -
     *            SPlayer ap, attacking player
     * @param -
     *            SArmy aa, attacking army
     * @param -
     *            Operation o, operation type/settings
     * @param -
     *            Boolean dry, mode flag
     * 
     * @return - an ArrayList with reasons the player (basic checks) or army
     *         (advanced checks) is not eligible to participate in the
     *         operation.
     */
    public ArrayList<Integer> validateShortAttacker(SPlayer ap, SArmy aa, Operation o, SPlanet target, int longID, boolean joiningAttack) {

        /*
         * List failure reasons in a vector, in order to keep count and format
         * in response to size.
         */
        ArrayList<Integer> failureReasons = new ArrayList<Integer>();

        /*
         * Check run type. If target is null, this is a DRY check which only
         * considers whether or not the given player's army is able to *start*
         * an operation. It does not account for volatile factors (CBills/Flu
         * launch costs) and is used primarily to set up the player's GUI and
         * inform him when an army becomes eligible or ineligible for a certain
         * op.
         */
        if (target == null) {

            // first, milestones (XP, Rating, etc)
            this.checkAttackerMilestones(failureReasons, ap, o);

            // last, construction
            this.checkAttackerConstruction(failureReasons, aa, o);

        }// end Dry check

        /*
         * dp is not null. This is a live check of the attacker's ability to
         * perform an operation, called from an Attack command. At this point,
         * we also need to check and see whether or not a player can afford an
         * operation, if the target world is in range, etc.
         * 
         * NOTE: DEFENDER CHECKS ARE DONE HERE! The Validator informs the
         * Manager of possible defenders. The manager, in turn, send the
         * defenders their join links and starts running chicken threads.
         */
        else {

            // do all checks, including range and volatiles
            this.checkAttackerRange(failureReasons, ap, o, target);
            this.checkAttackerMilestones(failureReasons, ap, o);
            this.checkAttackerCosts(failureReasons, ap, o);
            this.checkAttackerConstruction(failureReasons, aa, o);
            this.checkAttackerFlags(failureReasons, ap, o);

            // don't check defenders if the attacker can't pass his own checks
            if (failureReasons.size() > 0)
                return failureReasons;

            /*
             * Loop through the armies which passed ths OLH's generic matching,
             * looking for those whose players are on the target world, and
             * whose construction properties match the defense requirements.
             * These are live calls to validateShortDefender.
             * 
             * If a valid defender is found, the operation is created and added
             * to the Manager, which will inform the players and start
             * chickenThreads.
             */
            if (aa.getOpponents() == null || aa.getOpponents().size() == 0 && !o.getBooleanValue("AttackerAllowAgainstUnclaimedLand")) {
                failureReasons.add(SFAIL_ATTACK_NOOPPONENT);
                return failureReasons;
            }

            // else

            ArrayList<SArmy> planetMatches = new ArrayList<SArmy>();
            ArrayList<SArmy> fullMatches = new ArrayList<SArmy>();

            Iterator<SArmy> i = aa.getOpponents().iterator();
            while (i.hasNext()) {

                // load the army/player/house
                SArmy currArmy = i.next();
                SPlayer currPlayer = CampaignMain.cm.getPlayer(currArmy.getPlayerName());
                // Weird ass bug with nameChange and unenroll some times it
                // doesn't remove their
                // Old armies.
                if (currPlayer == null)
                    continue;

                SHouse currHouse = currPlayer.getHouseFightingFor();

                // check the currHouse's planetary holdings
                if (o.getBooleanValue("FreeForAllOperation") || target.getInfluence().getInfluence(currHouse.getId()) > 0)
                    planetMatches.add(currArmy);
            }

            if (o.getBooleanValue("AttackerAllowAgainstUnclaimedLand") && target.getInfluence().getInfluence(-1) > 0) {
                int freeShortID = manager.getFreeShortID();
                ShortOperation newOp = new ShortOperation(o.getName(), target, ap, aa, fullMatches, freeShortID, longID, false);
                manager.addShortOperation(newOp, ap, o);
                newOp.changeStatus(ShortOperation.STATUS_INPROGRESS);
                newOp.changeStatus(ShortOperation.STATUS_REPORTING);
                int conquestCap = o.getIntValue("ConquestAmountCap");
                int totalConquest = o.getIntValue("AttackerBaseConquestAmount");
                int conquestUnitAdjust = o.getIntValue("AttackerConquestUnitAdjustment");
                int conquestBVAdjust = o.getIntValue("AttackerConquestBVAdjustment");
                if (conquestUnitAdjust > 0)
                    totalConquest += Math.floor(newOp.getStartingUnits() / conquestUnitAdjust);
                if (conquestBVAdjust > 0)
                    totalConquest += Math.floor(newOp.getStartingBV() / conquestBVAdjust);
                if (totalConquest > conquestCap)
                    totalConquest = conquestCap;

                totalConquest = newOp.getTargetWorld().doGainInfluence(ap.getHouseFightingFor(), CampaignMain.cm.getHouseById(-1), totalConquest, false);

                if (totalConquest > 0) {
                    String point = "points";
                    if (totalConquest == 1)
                        point = "point";
                    String winnerMetaString = " gained " + totalConquest + " " + point + " of " + newOp.getTargetWorld().getNameAsColoredLink();
                    newOp.checkMercContracts(ap, ContractInfo.CONTRACT_LAND, totalConquest);
                    CampaignMain.cm.toUser("You've" + winnerMetaString, ap.getName());
                    for (House house : newOp.getTargetWorld().getInfluence().getHouses()) {
                        SHouse h = (SHouse) house;
                        if (h.equals(ap.getHouseFightingFor()))
                            continue;
                        CampaignMain.cm.doSendToAllOnlinePlayers(h, ap.getName() + winnerMetaString, true);
                    }
                    String newsFeedTitle;
                    String newsFeedBody;
                    if (!CampaignMain.cm.getBooleanConfig("ShowCompleteGameInfoInNews")) {
                        newsFeedTitle = ap.getHouseFightingFor().getColoredNameAsLink() + " gained land on " + newOp.getTargetWorld().getName();
                        newsFeedBody = ap.getHouseFightingFor().getColoredNameAsLink() + " gained " + totalConquest + "cp on " + newOp.getTargetWorld().getName();
                    } else {
                        newsFeedTitle = ap.getName() + " gained land on " + newOp.getTargetWorld().getName();
                        newsFeedBody = ap.getName() + " gained " + totalConquest + "cp on " + newOp.getTargetWorld().getName();
                    }
                    if (o.getBooleanValue("ReportOpToNewsFeed")) {
                        CampaignMain.cm.addToNewsFeed(newsFeedTitle, "Operations News", newsFeedBody);
                        CampaignMain.cm.postToDiscord(newsFeedBody);
                    }
                    newOp.setCompleteFinishedInfo(ap.getName() + " gained " + totalConquest + "cp on " + newOp.getTargetWorld().getName());
                    newOp.setIncompleteFinishedInfo(ap.getHouseFightingFor().getColoredNameAsLink() + " gained " + totalConquest + "cp on " + newOp.getTargetWorld().getName());

                }
                newOp.changeStatus(ShortOperation.STATUS_FINISHED);
                ap.lockArmy(-1);

                return failureReasons;
            }

            // if there are no players able to defend the world,
            // add the proper failure message and return.
            if (planetMatches.size() == 0) {
                failureReasons.add(SFAIL_ATTACK_NOPLANDEF);
                return failureReasons;
            }

            /*
             * At this point we have generic matches, filtered for the ability
             * to defend the world. Now, lets make a live check of the players'
             * ability to defend this specific kind of operation.
             */
            int maxELO = o.getIntValue("MaxELODifference");
            for (SArmy currArmy : planetMatches) {
                SPlayer currPlayer = CampaignMain.cm.getPlayer(currArmy.getPlayerName());
                ArrayList<Integer> defenderFails = this.validateShortDefender(currPlayer, currArmy, o, target);
                if (maxELO > 0 && Math.abs(currPlayer.getRating() - ap.getRating()) > maxELO)
                    defenderFails.add(ShortValidator.SFAIL_COMMON_ELODIFFERENCE);
                if (!aa.matches(currArmy, o))
                    defenderFails.add(ShortValidator.SFAIL_COMMON_MAX_BV_DIFFERENCE);

                if (currPlayer.getHouseFightingFor().equals(ap.getHouseFightingFor()) && !o.getBooleanValue("AllowInFaction")) {
                    defenderFails.add(SFAIL_COMMON_INFACTION_ATTACK);
                }

                if (defenderFails.size() == 0)// if player can defend, add
                    fullMatches.add(currArmy);
                else if (o.getBooleanValue("DebugOp")) { // spamalama
                    MWLogger.errLog("Failed Defense reasons for Op: " + o.getName() + " Launched by player: " + ap.getName() + " with army: #" + aa.getID());
                    MWLogger.errLog("Defending Player: " + currPlayer.getName() + " Army id: #" + currArmy.getID());

                    Iterator<Integer> df = defenderFails.iterator();
                    while (df.hasNext()) {
                        MWLogger.errLog("Reason: " + this.decodeFailure((Integer) df.next()));
                    }
                }
            }

            if (fullMatches.size() == 0) {

                failureReasons.add(SFAIL_ATTACK_NOTYPEDEF);
                return failureReasons;
            }

            /*
             * We have defenders! Yay! Lets make a ShortOperation and add it to
             * the manager.
             * 
             * The manager will handle the remaining work, informing players,
             * adding an ID, etc.
             */
            if (!joiningAttack) {
                int freeShortID = manager.getFreeShortID();
                ShortOperation newOp = new ShortOperation(o.getName(), target, ap, aa, fullMatches, freeShortID, longID, false);
                manager.addShortOperation(newOp, ap, o);
            }

        }// end else(real check)

        /*
         * If this was a REA
         */

        return failureReasons;
    }// end validateShortAttacker()

    private void checkAttackerFlags(ArrayList<Integer> failureReasons,
			SPlayer ap, Operation o) {
    	PlayerFlags pFlags = ap.getFlags();
		String requiredFlags = o.getValue("AttackerFlags");
		// Loop through the flag string, checking settings
		StringTokenizer st = new StringTokenizer(requiredFlags, "$");
		while(st.hasMoreTokens()) {
			StringTokenizer element = new StringTokenizer(st.nextToken(), "#");
			String fName = element.nextToken();
			boolean value = Boolean.parseBoolean(element.nextToken());
			if (pFlags.getFlagStatus(fName) != value) {
				if (value) {
					failureReasons.add(SFAIL_ATTACK_MISSING_REQUIRED_FLAG);
				} else {
					failureReasons.add(SFAIL_ATTACK_HAS_BANNED_FLAG);
				}
			}
		}
	}

	/**
     * Method which checks the attacker's ability to reach the target world.
     * Factions may always reach worlds on which they have running long ops, as
     * they couldn't have started them if the targets were out of range. Players
     * may always always attack worlds they control.
     * 
     * "Range" check has evolved to include more than just range, and may now be
     * considered a general "Target Validity" check, which looks at ownership
     * %'s, factory presence, counterattack ability, etc.
     * 
     * NOTE: This is a change from the Task system, which allowed players to
     * attack from worlds they did not own (25%) or attack on any world which
     * they had a foothold on. Some operations will overload the default setting
     * and allow attacks with non-controlling territory on a world (suggested
     * op: Guerilla Engagement).
     */
    public void checkAttackerRange(ArrayList<Integer> failureReasons, SPlayer ap, Operation o, SPlanet target) {

        // Always allow attacks on newbie worlds
        if (target.getOwner() != null && target.getOwner() instanceof NewbieHouse)
            return;

        // Check for a long op. If present, autopass.
        // if (manager.factionHasLongRunningOn(target))
        // return;

        /*
         * Check to see if this is a counterattack.
         */
        if (o.getBooleanValue("ForbidCounterAttacks") && CampaignMain.cm.getOpsManager().playerHasActiveChickenThread(ap))
            failureReasons.add(SFAIL_ATTACK_NOCOUNTERS);

        /*
         * Check to ensure that the player is in a proper duty status
         */
        if (o.getBooleanValue("OnlyAllowedFromReserve") && ap.getDutyStatus() >= SPlayer.STATUS_ACTIVE)
            failureReasons.add(SFAIL_ATTACK_AFRONLY);
        if (o.getBooleanValue("OnlyAllowedFromActive") && ap.getDutyStatus() <= SPlayer.STATUS_RESERVE)
            failureReasons.add(SFAIL_ATTACK_ACTIVEONLY);

        // store the percent owned, since it's checked multiple times
        int ahID = ap.getHouseFightingFor().getId();
        double percentOwned = (double)100 * ((double)target.getInfluence().getInfluence(ahID) / (double)target.getConquestPoints()); 

        /*
         * Check the ownership limitations.
         */
        if (percentOwned < o.getIntValue("MinPlanetOwnership"))
            failureReasons.add(SFAIL_ATTACK_MINOWNERSHIP);

        if (percentOwned > o.getIntValue("MaxPlanetOwnership"))
            failureReasons.add(SFAIL_ATTACK_MAXOWNERSHIP);

        /*
         * Check the Have Fac/No Fac limitations.
         */
        boolean mustHaveFac = o.getBooleanValue("OnlyAgainstFactoryWorlds");
        boolean mustNotHaveFac = o.getBooleanValue("OnlyAgainstNonFactoryWorlds");
        if (mustHaveFac && target.getFactoryCount() <= 0)
            failureReasons.add(SFAIL_ATTACK_NEEDSFAC);
        if (mustNotHaveFac && target.getFactoryCount() > 0)
            failureReasons.add(SFAIL_ATTACK_HASFAC);

        /*
         * Check the Have Home/No worlds
         */
        boolean mustHaveHome = o.getBooleanValue("OnlyAgainstHomeWorlds");
        boolean mustNotHaveHome = o.getBooleanValue("OnlyAgainstNonHomeWorlds");
        if (mustHaveHome && !target.isHomeWorld())
            failureReasons.add(SFAIL_ATTACK_NEEDSHOME);
        if (mustNotHaveHome && target.isHomeWorld())
            failureReasons.add(SFAIL_ATTACK_HASHOME);

        // check for allowed op flags
        String allowPlanetFlags = o.getValue("AllowPlanetFlags");
        String disallowPlanetFlags = o.getValue("DisallowPlanetFlags");

        // Check for allowed planet flags. the planet most have these flags.
        if (allowPlanetFlags.length() > 0) {
            StringTokenizer st = new StringTokenizer(allowPlanetFlags, "^");
            while (st.hasMoreTokens())
                if (!target.getPlanetFlags().containsKey(st.nextToken()))
                    failureReasons.add(SFAIL_ATTACK_OPFLAGS);
        }

        // Check for disallowed planet flags. If the planet has one of these
        // flags
        // The planet will not be allowed.
        if (disallowPlanetFlags.length() > 0) {
            StringTokenizer st = new StringTokenizer(disallowPlanetFlags, "^");
            while (st.hasMoreTokens()) {
                if (target.getPlanetFlags().containsKey(st.nextToken()))
                    failureReasons.add(SFAIL_ATTACK_OPFLAGS);
            }
        }

        /*
         * If the operation allows non-control attacks, check the planetary
         * influences and return if the faction has enough control.
         */
        int percToAttackOnWorld = o.getIntValue("PercentageToAttackOnWorld");
        if (percentOwned >= percToAttackOnWorld)
            return;

        /*
         * Non-newbie world, no long op, and not enough % to attack on world.
         * Look at surrounding planets. Break out of this search as soon as a
         * legal jump off world has been found. We don't care WHERE we attack
         * from - just that we can.
         */
        // load op range values
        double opRange = o.getDoubleValue("OperationRange");
        double percToAttackOffWorld = o.getDoubleValue("PercentageToAttackOffWorld");

        Iterator<Planet> e = CampaignMain.cm.getData().getAllPlanets().iterator();
        while (e.hasNext()) {
            SPlanet currP = (SPlanet) e.next();
            percentOwned = (double)100 * ((double)currP.getInfluence().getInfluence(ahID) / (double)currP.getConquestPoints()); 

            if ( percentOwned >= percToAttackOffWorld && currP.getPosition().distanceSq(target.getPosition()) <= opRange)
                return;
        }// end while(planets remain)

        /*
         * We've now exhausted all possible planets and range justifications
         * without finding a match. Add a range failure to the list.
         */
        failureReasons.add(SFAIL_ATTACK_OUTOFRANGE);

    }

    /**
     * Method which checks attacker milestones, like experience and rank. The
     * milestone check includes a few "static" status items - faction, etc.
     */
    public void checkAttackerMilestones(ArrayList<Integer> failureReasons, SPlayer ap, Operation o) {

        boolean solCanAttack = o.getBooleanValue("AllowSOLToUse");
        boolean nonConqCanAttack = o.getBooleanValue("AllowNonConqToUse");
        if (ap.getHouseFightingFor().isNewbieHouse() && !solCanAttack)
            failureReasons.add(SFAIL_ATTACK_SOLCANTATT);
        if (!ap.getHouseFightingFor().isConquerable() && !nonConqCanAttack)
            failureReasons.add(SFAIL_ATTACK_NON_CONQ_A);

        // faction checks
        String allowed = o.getValue("LegalAttackFactions");
        String notAllowed = o.getValue("IllegalAttackFactions");
        if (allowed.trim().length() != 0 && allowed.indexOf(ap.getHouseFightingFor().getName()) == -1)
            failureReasons.add(SFAIL_ATTACK_FACTION);
        else if (notAllowed.trim().length() != 0 && notAllowed.indexOf(ap.getHouseFightingFor().getName()) >= 0)
            failureReasons.add(SFAIL_ATTACK_FACTION);

        if (ap.getRating() > o.getDoubleValue("MaxAttackerRating"))
            failureReasons.add(SFAIL_ATTACK_MAXRATING);
        if (ap.getRating() < o.getDoubleValue("MinAttackerRating"))
            failureReasons.add(SFAIL_ATTACK_MINRATING);

        if (ap.getExperience() > o.getIntValue("MaxAttackerXP"))
            failureReasons.add(SFAIL_ATTACK_MAXXP);
        if (ap.getExperience() < o.getIntValue("MinAttackerXP"))
            failureReasons.add(SFAIL_ATTACK_MINXP);

        // subFaction Checks
        if (ap.getSubFactionAccess() < o.getIntValue("MinSubFactionAccessLevel"))
            failureReasons.add(SFAIL_COMMON_INSUFFICENT_SUBFACTION_ACCESS_LEVEL);

        // if (ap.getGamesPlayed() > o.getIntValue("MaxAttackerGamesPlayed")))
        // failureReasons.add(SFAIL_ATTACK_MAXGAMES);
        // if (ap.getGamesPlayed() < o.getIntValue("MinAttackerGamesPlayed")))
        // failureReasons.add(SFAIL_ATTACK_MINGAMES);
    }

    /**
     * Method which checks to ensure that an attacker can meet the costs
     * associated with begining an operation.
     * 
     * Random Trivia: This was MekWars' first use of Java 1.5 autoboxing.
     * Exciting!?
     */
    public void checkAttackerCosts(ArrayList<Integer> failureReasons, SPlayer ap, Operation o) {

        if (ap.getMoney() < o.getIntValue("AttackerCostMoney"))
            failureReasons.add(SFAIL_ATTACK_MONEY);

        if (ap.getInfluence() < o.getIntValue("AttackerCostInfluence"))
            failureReasons.add(SFAIL_ATTACK_INFLUENCE);

        if (ap.getReward() < o.getIntValue("AttackerCostReward"))
            failureReasons.add(SFAIL_ATTACK_REWARD);
    }

    /**
     * Method which checks army CONSTRUCTION params (contruction block of the
     * defaults). These dictate simple things like the max aggregate weight of
     * all units in the army and are specific to the given army, not relative to
     * the defending force.
     * 
     * Checks the following Operation properties: - MaxAttackerBV -
     * MinAttackerBV - MaxAttackerUnits - MinAttackerUnits - MinAttackerWalk -
     * MinAttackerJump - MaxAttackerUnitTonnage - MinAttackerUnitTonnage -
     * MaxTotalAttackerTonnage - MinTotalAttackerTonnage - AttackerAllowedMeks -
     * AttackerAllowedVehs - AttackerAllowedInf - AttackerOmniMeksOnly -
     * PoweredInfAllowed, if !AttackerAllowedInf
     */
    public void checkAttackerConstruction(ArrayList<Integer> failureReasons, SArmy aa, Operation o) {

        //There is no army to check the army will be provided by the server.
        if ( o.getBooleanValue("MULArmiesOnly") ) {
            return;
        }
        I_SpreadValidator isv;
        int spreadError = I_SpreadValidator.ERROR_NONE;
        if (o.getBooleanValue("AttackerUsePercentageBVSpread")) {
        	isv = new PercentBVSpreadValidator(o.getIntValue("MaxAttackerUnitBVSpread"), o.getDoubleValue("AttackerBVSpreadPercent"));
        } else {
        	isv = new StandardBVSpreadValidator(o.getIntValue("MinAttackerUnitBVSpread"), o.getIntValue("MaxAttackerUnitBVSpread"));
        }
        isv.setDebug(false); // Set this to false when we go live with it.
        boolean spreadValidates = isv.validate(aa, o);
        
        if (!spreadValidates) {
        	spreadError = isv.getError();
        }
        
        boolean countSupport = o.getBooleanValue("CountSupportUnits");
        
        /*
         * Can have too many units, or too few, but not both. As such, can use
         * "else if" here and skip the mincheck in some cases. Similar logic
         * applies to other min/max splits throughout the validation process.
         */
        // BV min/max. Remember - these are for op qualification, not army
        // matching.
        if (aa.getBV() > o.getIntValue("MaxAttackerBV"))
            failureReasons.add(SFAIL_ATTACK_MAXBV);
        else if (aa.getBV() < o.getIntValue("MinAttackerBV"))
            failureReasons.add(SFAIL_ATTACK_MINBV);

        // Mek min/max. Remember - these are for op qualification, not related
        // to limiters.
        if (aa.getNumberOfUnitTypes(Unit.MEK, countSupport) > o.getIntValue("MaxAttackerMeks"))
            failureReasons.add(SFAIL_ATTACK_MAXMEKS);
        else if (aa.getNumberOfUnitTypes(Unit.MEK, countSupport) < o.getIntValue("MinAttackerMeks"))
            failureReasons.add(SFAIL_ATTACK_MINMEKS);

        // Vehicle min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (aa.getNumberOfUnitTypes(Unit.VEHICLE, countSupport) > o.getIntValue("MaxAttackerVehicles"))
            failureReasons.add(SFAIL_ATTACK_MAXVEHICLES);
        else if (aa.getNumberOfUnitTypes(Unit.VEHICLE, countSupport) < o.getIntValue("MinAttackerVehicles"))
            failureReasons.add(SFAIL_ATTACK_MINVEHICLES);

        // Aero min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (aa.getNumberOfUnitTypes(Unit.AERO, countSupport) > o.getIntValue("MaxAttackerAero"))
            failureReasons.add(SFAIL_ATTACK_MAX_AERO);
        else if (aa.getNumberOfUnitTypes(Unit.AERO, countSupport) < o.getIntValue("MinAttackerAero"))
            failureReasons.add(SFAIL_ATTACK_MIN_AERO);
        
        // Check Aero ratios
        if (o.getBooleanValue("EnforceAttackerAeroRatio")) {
        	boolean countAeroSupport = o.getBooleanValue("CountSupportUnitsInAeroRatio");
        	int numAero = aa.getNumberOfUnitTypes(Unit.AERO, countAeroSupport);
        	int totalUnits = aa.getAmountOfUnits();
        	double minPercent = o.getDoubleValue("MinAttackerAeroPercent");
        	double maxPercent = o.getDoubleValue("MaxAttackerAeroPercent");
        	double actualPercent = (double) (((double)numAero / (double)totalUnits) * 100);
        	
        	if (actualPercent < minPercent) {
        		failureReasons.add(SFAIL_ATTACK_MIN_AERO);
        	} else if (actualPercent > maxPercent) {
        		failureReasons.add(SFAIL_ATTACK_MAX_AERO);
        	}
        }

        int infCount = aa.getNumberOfUnitTypes(Unit.INFANTRY, countSupport);
        int protoCount = aa.getNumberOfUnitTypes(Unit.PROTOMEK, countSupport);
        if (protoCount > 0)
            infCount += Math.max(1, protoCount / 5);
        infCount += aa.getNumberOfUnitTypes(Unit.BATTLEARMOR, countSupport);

        // Mek min/max. Remember - these are for op qualification, not related
        // to limiters.
        if (infCount > o.getIntValue("MaxAttackerInfantry"))
            failureReasons.add(SFAIL_ATTACK_MAXINFANTRY);
        else if (infCount < o.getIntValue("MinAttackerInfantry"))
            failureReasons.add(SFAIL_ATTACK_MININFANTRY);

        // NonInfantry min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (aa.getNumberOfUnitTypes(Unit.MEK) + aa.getNumberOfUnitTypes(Unit.VEHICLE) + aa.getNumberOfUnitTypes(Unit.AERO) > o.getIntValue("MaxAttackerNonInfantry"))
            failureReasons.add(SFAIL_ATTACK_MAXNONINFANTRY);
        else if (aa.getNumberOfUnitTypes(Unit.MEK) + aa.getNumberOfUnitTypes(Unit.VEHICLE) + aa.getNumberOfUnitTypes(Unit.AERO) < o.getIntValue("MinAttackerNonInfantry"))
            failureReasons.add(SFAIL_ATTACK_MINNONINFANTRY);

        // Support Unit min/max
        if (aa.getTotalSupportUnits() < o.getIntValue("MinAttackerSupportUnits")) {
        	failureReasons.add(SFAIL_ATTACK_TOO_FEW_SUPPORT_UNITS);
        } else if (aa.getTotalSupportUnits() > o.getIntValue("MaxAttackerSupportUnits")) {
        	failureReasons.add(SFAIL_ATTACK_TOO_MANY_SUPPORT_UNITS);
        }
        // Non-Support Unit min/max
        if ((aa.getAmountOfUnitsWithoutInfantry() - aa.getTotalSupportUnits()) < o.getIntValue("MinAttackerNonSupportUnits")) {
        	failureReasons.add(SFAIL_ATTACK_TOO_FEW_NONSUPPORT_UNITS);
        } else if ((aa.getAmountOfUnitsWithoutInfantry() - aa.getTotalSupportUnits()) > o.getIntValue("MaxAttackerNonSupportUnits")) {
        	failureReasons.add(SFAIL_ATTACK_TOO_MANY_NONSUPPORT_UNITS);
        }
        
        
        /*
         * loop through all units in the army, setting up remaining checks.
         */
        int totalWeight = 0;
        int largestWeight = 0;
        int numProtoMeks = aa.getNumberOfUnitTypes(Unit.PROTOMEK);

        int numberOfCommanders = 0;
        boolean hasMeks = false;
        boolean hasVehs = false;
        boolean hasAeros = false;
        boolean hasInf = false;
        boolean normInf = false;
        boolean powerInf = false;
        boolean speedFail = false;
        boolean jumpTooFar = false;
        boolean maxTonFail = false;
        boolean minTonFail = false;
        boolean maxBVFail = false;
        boolean minBVFail = false;
        boolean omniFail = false;
        boolean checkOmni = o.getBooleanValue("AttackerOmniMeksOnly");
        boolean vetPilots = false;
        boolean greenPilots = false;
        double averageArmySkills = 0.0;
        int numberOfValidUnits = 0;
        boolean checkClantech = o.getBooleanValue("UseClanEquipmentRatios");
        int numClanUnits = 0;
        int numTotalUnits = 0;

        
        Iterator<Unit> i = aa.getUnits().iterator();
        while (i.hasNext()) {

            // load the next unit
            SUnit currUnit = (SUnit) i.next();

            // Check to see if its a unit commander.
            numberOfCommanders = aa.getCommanders().size();

            // get the unit's weight, store
            int currWeight = (int) currUnit.getEntity().getWeight();
            totalWeight += currWeight;
            if (currWeight > largestWeight)
                largestWeight = currWeight;

            // check to see if the unit is a mek
            if (currUnit.getType() == Unit.MEK) {
                hasMeks = true;
                if (checkOmni && !omniFail)
                    if (!currUnit.isOmni())
                        omniFail = true;
            }
            // or, if it is a vehicle
            else if (currUnit.getType() == Unit.VEHICLE)
                hasVehs = true;
            else if (currUnit.getType() == Unit.AERO)
                hasAeros = true;
            
            // if infantry, check to see if powered or foot
            // and set the normInf variable, as appropriate.
            else if (currUnit.getType() == Unit.INFANTRY || currUnit.getType() == Unit.BATTLEARMOR || currUnit.getType() == Unit.PROTOMEK) {
                hasInf = true;
                if ((currUnit.getEntity() instanceof BattleArmor) || (currUnit.getEntity() instanceof Protomech))
                    powerInf = true;
                else
                    normInf = true;
            }

            // now, check the unit's walking and jumping speeds. only fail on
            // this once.
            if (!speedFail && !jumpTooFar) {
                try {
                    int walkMP = currUnit.getEntity().getWalkMP();
                    int jumpMP = currUnit.getEntity().getJumpMP();
                    if (walkMP < o.getIntValue("MinAttackerWalk") && jumpMP < o.getIntValue("MinAttackerJump"))
                        speedFail = true;
                    if (jumpMP > o.getIntValue("MaxAttackerJump")) {
                    	jumpTooFar = true;
                    }
                } catch (Exception ex) {
                }

            }// end if(hasn't already speedfail'ed)

            // check the unit's weight
            /*
             * This only check for Meks and Vehicles this will allow for
             * combined arms with heavier armies right now you can have a heavy
             * armie and add inf support Infantry/Protos can be regulated with
             * out options more easily.
             */
            if (currUnit.getType() == Unit.MEK || currUnit.getType() == Unit.VEHICLE || currUnit.getType() == Unit.AERO ) {
                if (currWeight > o.getIntValue("MaxAttackerUnitTonnage"))
                    maxTonFail = true;
                else if (currWeight < o.getIntValue("MinAttackerUnitTonnage"))
                    minTonFail = true;
            }

            // check the unit's BV
            int currBV = 0;
            
            if (o.getBooleanValue("IgnorePilotsForBVSpread")) {
            	currBV = currUnit.getBaseBV();
            } else {
            	currBV = currUnit.getBVForMatch();
            }
            
            if (currBV < o.getIntValue("MinAttackerUnitBV"))
                minBVFail = true;
            else if (currBV > o.getIntValue("MaxAttackerUnitBV"))
                maxBVFail = true;

            // check spreads. because the spreads can <code>continue</code> they
            // should be LAST
            int type = currUnit.getType();
            if (currUnit.isSupportUnit() && !o.getBooleanValue("CountSupportUnitsForSpread"))
            	continue;
            if (type == Unit.VEHICLE && !o.getBooleanValue("CountVehsForSpread"))
                continue;
            else if (type == Unit.PROTOMEK && !o.getBooleanValue("CountProtosForSpread"))
                continue;
            else if ((type == Unit.BATTLEARMOR || type == Unit.INFANTRY) && !o.getBooleanValue("CountInfForSpread"))
                continue;
            else if ( type == Unit.AERO && !o.getBooleanValue("CountAerosForSpread") )
                continue;
            
            if ( !currUnit.hasVacantPilot() ) {
                int piloting = currUnit.getPilot().getPiloting();
                int gunnery = currUnit.getPilot().getGunnery();
                int totalSkills = gunnery+piloting;
                
                averageArmySkills += totalSkills;
                numberOfValidUnits++;
                
                if ( piloting > o.getIntValue("HighestAttackerPiloting") )
                    greenPilots = true;
                
                if ( piloting < o.getIntValue("LowestAttackerPiloting") )
                    vetPilots = true;
                
                if ( gunnery > o.getIntValue("HighestAttackerGunnery") )
                    greenPilots = true;
                
                if ( gunnery < o.getIntValue("LowestAttackerGunnery") )
                    vetPilots = true;

                
                if ( totalSkills > o.getIntValue("HighestAttackerPilotSkillTotal") )
                    greenPilots = true;
                
                if ( totalSkills < o.getIntValue("LowestAttackerPilotSkillTotal") )
                    vetPilots = true;
            }

            // Count total units and clan units
            numTotalUnits++;
            if(currUnit.getEntity().isClan()) {
            	numClanUnits++;
            }
        }// end while(units remain)

        // add unit exclusion failures to list
        if (hasMeks && !o.getBooleanValue("AttackerAllowedMeks"))
            failureReasons.add(SFAIL_ATTACK_NOMEKS);
        if (hasVehs && !o.getBooleanValue("AttackerAllowedVehs"))
            failureReasons.add(SFAIL_ATTACK_NOVEHS);
        if (hasAeros && !o.getBooleanValue("AttackerAllowedAeros"))
            failureReasons.add(SFAIL_ATTACK_NOAEROS);
        if (hasInf && !o.getBooleanValue("AttackerAllowedInf")) {
            // powered allowed, but there's unarmored inf too
            if (o.getBooleanValue("AttackerPoweredInfAllowed") && normInf)
                failureReasons.add(SFAIL_ATTACK_NONORMINF);
            else if (o.getBooleanValue("AttackerStandardInfAllowed") && powerInf)
                failureReasons.add(SFAIL_ATTACK_NOPOWERINF);
            else if ( !normInf && !powerInf ) {
                // no infantry allowed, at all
                failureReasons.add(SFAIL_ATTACK_NOINF);
            }
        }// end if(!AllowedInf)

        if (o.getBooleanValue("UseUnitCommander")) {
            if (numberOfCommanders < o.getIntValue("MinimumUnitCommanders"))
                failureReasons.add(SFAIL_COMMON_NOT_ENOUGH_COMMANDERS);
            if (numberOfCommanders > o.getIntValue("MaximumUnitCommanders"))
                failureReasons.add(SFAIL_COMMON_TOO_MANY_COMMANDERS);
        }

        if (checkOmni && omniFail)
            failureReasons.add(SFAIL_ATTACK_OMNIONLY);

        // proto failures. wee.
        if (o.getBooleanValue("ProtosMustbeGrouped") && numProtoMeks > 0 && numProtoMeks % 5 != 0)
            failureReasons.add(SFAIL_COMMON_PROTOGROUPS);

        // add speed failure to list
        if (speedFail)
            failureReasons.add(SFAIL_ATTACK_MINSPEED);

        if (jumpTooFar) {
        	failureReasons.add(SFAIL_ATTACK_MAXJUMP);
        }
        
        // add max/min unit ton failures to list
        if (maxTonFail)
            failureReasons.add(SFAIL_ATTACK_MAXUNITTON);
        if (minTonFail)
            failureReasons.add(SFAIL_ATTACK_MINUNITTON);

        // add max/min unit BV failures to list
        if (maxBVFail)
            failureReasons.add(SFAIL_ATTACK_MAXUNITBV);
        if (minBVFail)
            failureReasons.add(SFAIL_ATTACK_MINUNITBV);

        // check total tonnage failures
        if (totalWeight > o.getIntValue("MaxTotalAttackerTonnage"))
            failureReasons.add(SFAIL_ATTACK_MAXARMYTON);
        else if (totalWeight < o.getIntValue("MinTotalAttackerTonnage"))
            failureReasons.add(SFAIL_ATTACK_MINARMYTON);

        // check unit BV difference failures
       
        if (spreadError == I_SpreadValidator.ERROR_SPREAD_TOO_LARGE) {
        	failureReasons.add(SFAIL_ATTACK_MAXSPREAD);
        } else if (spreadError == I_SpreadValidator.ERROR_SPREAD_TOO_SMALL) {
        	failureReasons.add(SFAIL_ATTACK_MINSPREAD);
        }
        
        averageArmySkills /= numberOfValidUnits;
        
        if ( vetPilots)
            failureReasons.add(SFAIL_ATTACK_ELITE_PILOTS );

        if (averageArmySkills > o.getDoubleValue("AttackerAverageArmySkillMax")) {
        	failureReasons.add(SFAIL_ATTACK_SKILLSUM_TOOHIGH);
        }
        
        if (averageArmySkills < o.getDoubleValue("AttackerAverageArmySkillMin")) {
        	failureReasons.add(SFAIL_ATTACK_SKILLSUM_TOOLOW);
        }
        
        if ( greenPilots )
            failureReasons.add(SFAIL_ATTACK_GREEN_PILOTS );
        
        if (checkClantech) {
        	double minClantech = o.getDoubleValue("AttackerMinClanEquipmentPercent");
        	double maxClantech = o.getDoubleValue("AttackerMaxClanEquipmentPercent");
        	double clanTechPercent = (((double)numClanUnits / (double)numTotalUnits) * 100.0);
        	
        	if (clanTechPercent < minClantech) {
        		failureReasons.add(SFAIL_ATTACK_TECHBASE_TOO_LITTLE_CLAN);
        	}
        	if (clanTechPercent > maxClantech) {
        		failureReasons.add(SFAIL_ATTACK_TECHBASE_TOO_MUCH_CLAN);
        	}
        }

    }// end CheckAttackerConstruction

    /**
     * Method which checks a Player/Army vs. an operation type to see if the
     * army can be used to defend. This method may be called with 2 different
     * goals:
     * 
     * Unlike Attack validations, Defenses do NOT have a dry call. Every check
     * looks at volatiles.
     * 
     * @param -
     *            SPlayer dp, defending player
     * @param -
     *            SPlayer da, defending army
     * @param -
     *            Operation o, operation type/settings
     */
    public ArrayList<Integer> validateShortDefender(SPlayer dp, SArmy da, Operation o, SPlanet target) {

        ArrayList<Integer> failureReasons = new ArrayList<Integer>();

        // checks
        this.checkDefenderMilestones(failureReasons, dp, o, target);
        this.checkDefenderCosts(failureReasons, dp, o);
        this.checkDefenderConstruction(failureReasons, da, o);
        this.checkDefenderFlags(failureReasons, dp, o);

        return failureReasons;
    }

    private void checkDefenderFlags(ArrayList<Integer> failureReasons,
			SPlayer dp, Operation o) {
		// TODO Auto-generated method stub
    	PlayerFlags pFlags = dp.getFlags();
		String requiredFlags = o.getValue("DefenderFlags");
		// Loop through the flag string, checking settings
		StringTokenizer st = new StringTokenizer(requiredFlags, "$");
		while(st.hasMoreTokens()) {
			StringTokenizer element = new StringTokenizer(st.nextToken(), "#");
			String fName = element.nextToken();
			boolean value = Boolean.parseBoolean(element.nextToken());
			if (pFlags.getFlagStatus(fName) != value) {
				if (value) {
					failureReasons.add(SFAIL_DEFEND_MISSING_REQUIRED_FLAG);
				} else {
					failureReasons.add(SFAIL_DEFEND_HAS_BANNED_FLAG);
				}
			}
		}
	}

	/**
     * Method which checks defender milestones, like experience and rank.
     */
    private void checkDefenderMilestones(ArrayList<Integer> failureReasons, SPlayer dp, Operation o, SPlanet target) {

        boolean solCanDefend = o.getBooleanValue("AllowAgainstSOL");
        boolean nonConqCanDefend = o.getBooleanValue("AllowAgainstNonConq");
        double percentOwned = 0;
        if(target != null)
        	percentOwned = (double)100 * ((double)target.getInfluence().getInfluence(dp.getHouseFightingFor().getId()) / (double)target.getConquestPoints());
        else
        	percentOwned = 0;
        //Baruk Khazad! - 20151003 - start
        if (target != null && percentOwned < o.getIntValue("MinPlanetOwnership")) {
            if (percentOwned > 0 && o.getIntValue("MinPlanetOwnership") > 0 && o.getBooleanValue("MinPlanetOwnershipIgnoredByDefender")) {
                //do not apply a fail flag because 1) they own some 2) there is a non-zero min%Owned value 3) the ignore flag is set to true. This fits the condition described in the Operations Manager.
            } else {
                failureReasons.add(SFAIL_DEFEND_NOTPLANDEF); 
            }
        }
        //Baruk Khazad! - 20151003 - end
        if (dp.getHouseFightingFor().isNewbieHouse() && !solCanDefend)
            failureReasons.add(SFAIL_DEFEND_SOLCANTDEF);
        if (!dp.getHouseFightingFor().isConquerable() && !nonConqCanDefend)
            failureReasons.add(SFAIL_DEFEND_NON_CONQ_D);

        if ((o.getIntValue("AttackerBaseConquestAmount") > 0 || o.getIntValue("AttackerConquestBVAdjustment") > 0 || o.getIntValue("AttackerConquestUnitAdjustment") > 0 || o.getIntValue("DefenderBaseConquestAmount") > 0 || o.getIntValue("DefenderConquestBVAdjustment") > 0 || o.getIntValue("DefenderConquestUnitAdjustment") > 0) && (target != null && !target.isConquerable()))
            failureReasons.add(SFAIL_DEFEND_NON_CONQ_PLANET);

        // faction checks
        String allowed = o.getValue("LegalDefendFactions");
        String notAllowed = o.getValue("IllegalDefendFactions");
        if (allowed.trim().length() != 0 && allowed.indexOf(dp.getHouseFightingFor().getName()) == -1)
            failureReasons.add(SFAIL_DEFEND_FACTION);
        else if (notAllowed.trim().length() != 0 && notAllowed.indexOf(dp.getHouseFightingFor().getName()) >= 0)
            failureReasons.add(SFAIL_DEFEND_FACTION);

        if (dp.getRating() > o.getDoubleValue("MaxDefenderRating"))
            failureReasons.add(SFAIL_DEFEND_MAXRATING);
        if (dp.getRating() < o.getDoubleValue("MinDefenderRating"))
            failureReasons.add(SFAIL_DEFEND_MINRATING);

        if (dp.getExperience() > o.getIntValue("MaxDefenderXP"))
            failureReasons.add(SFAIL_DEFEND_MAXXP);
        if (dp.getExperience() < o.getIntValue("MinDefenderXP"))
            failureReasons.add(SFAIL_DEFEND_MINXP);

        // subFaction Checks
        if (dp.getSubFactionAccess() < o.getIntValue("MinSubFactionAccessLevel"))
            failureReasons.add(SFAIL_COMMON_INSUFFICENT_SUBFACTION_ACCESS_LEVEL);

        // if (dp.getGamesPlayed() > o.getIntValue("MaxDefenderGamesPlayed")))
        // failureReasons.add(SFAIL_DEFEND_MAXGAMES);
        // if (dp.getGamesPlayed() < o.getIntValue("MinDefenderGamesPlayed")))
        // failureReasons.add(SFAIL_DEFEND_MINGAMES);
    }

    /**
     * Method which checks to ensure that a defender can meet the costs
     * associated with begining an operation.
     */
    private void checkDefenderCosts(ArrayList<Integer> failureReasons, SPlayer dp, Operation o) {

        if (dp.getMoney() < o.getIntValue("DefenderCostMoney"))
            failureReasons.add(SFAIL_DEFEND_MONEY);

        if (dp.getInfluence() < o.getIntValue("DefenderCostInfluence"))
            failureReasons.add(SFAIL_DEFEND_INFLUENCE);

        if (dp.getReward() < o.getIntValue("DefenderCostReward"))
            failureReasons.add(SFAIL_DEFEND_REWARD);
    }

    /**
     * Method which checks army construction params/properties for defenders.
     * Mirrors checkAttackerConstructon in many respects.
     * 
     * Checks the following Operation properties: - MaxDefenderBV -
     * MinDefenderBV - MaxDefenderUnits - MinDefenderUnits - MinDefenderWalk -
     * MinDefenderJump - MaxDefenderUnitTonnage - MinDefenderUnitTonnage -
     * MaxTotalDefenderTonnage - MinTotalDefenderTonnage - DefenderAllowedMeks -
     * DefenderAllowedVehs - DefenderAllowedInf - DefenderOmniMeksOnly -
     * PoweredInfAllowed, if !DefenderAllowedInf
     */
    private void checkDefenderConstruction(ArrayList<Integer> failureReasons, SArmy da, Operation o) {

        
        //There is no army to check. The army will be provided by the server
        if ( o.getBooleanValue("MULArmiesOnly")) {
            return;
        }
        
        I_SpreadValidator isv;
        int spreadError = I_SpreadValidator.ERROR_NONE;
        if (o.getBooleanValue("DefenderUsePercentageBVSpread")) {
        	isv = new PercentBVSpreadValidator(o.getIntValue("MaxDefenderUnitBVSpread"), o.getDoubleValue("DefenderBVSpreadPercent"));
        } else {
        	isv = new StandardBVSpreadValidator(o.getIntValue("MinDefenderUnitBVSpread"), o.getIntValue("MaxDefenderUnitBVSpread"));
        }
        isv.setDebug(false); // Set this to false when we go live with it.
        boolean spreadValidates = isv.validate(da, o);
        
        if (!spreadValidates) {
        	spreadError = isv.getError();
        }
        
        boolean countSupport = o.getBooleanValue("CountSupportUnits");
        
        // BV min/max. Remember - these are for op qualification, not army
        // matching.
        if (da.getBV() > o.getIntValue("MaxDefenderBV"))
            failureReasons.add(SFAIL_DEFEND_MAXBV);
        else if (da.getBV() < o.getIntValue("MinDefenderBV"))
            failureReasons.add(SFAIL_DEFEND_MINBV);

        // Meks min/max. Remember - these are for op qualification, not related
        // to limiters.
        if (da.getNumberOfUnitTypes(Unit.MEK, countSupport) > o.getIntValue("MaxDefenderMeks"))
            failureReasons.add(SFAIL_DEFEND_MAXMEKS);
        else if (da.getNumberOfUnitTypes(Unit.MEK, countSupport) < o.getIntValue("MinDefenderMeks"))
            failureReasons.add(SFAIL_DEFEND_MINMEKS);

        // Vehicles min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (da.getNumberOfUnitTypes(Unit.VEHICLE, countSupport) > o.getIntValue("MaxDefenderVehicles"))
            failureReasons.add(SFAIL_DEFEND_MAXVEHICLES);
        else if (da.getNumberOfUnitTypes(Unit.VEHICLE, countSupport) < o.getIntValue("MinDefenderVehicles"))
            failureReasons.add(SFAIL_DEFEND_MINVEHICLES);

        // Aero min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (da.getNumberOfUnitTypes(Unit.AERO, countSupport) > o.getIntValue("MaxDefenderAero"))
            failureReasons.add(SFAIL_DEFEND_MAX_AERO);
        else if (da.getNumberOfUnitTypes(Unit.AERO, countSupport) < o.getIntValue("MinDefenderAero"))
            failureReasons.add(SFAIL_DEFEND_MIN_AERO);

        // Check Aero ratios
        if (o.getBooleanValue("EnforceDefenderAeroRatio")) {
        	boolean countAeroSupport = o.getBooleanValue("CountSupportUnitsInAeroRatio");
        	int numAero = da.getNumberOfUnitTypes(Unit.AERO, countAeroSupport);
        	int totalUnits = da.getAmountOfUnits();
        	double minPercent = o.getDoubleValue("MinAttackerAeroPercent");
        	double maxPercent = o.getDoubleValue("MaxAttackerAeroPercent");
        	double actualPercent = (double) (((double)numAero / (double)totalUnits) * 100);
        	if (actualPercent < minPercent) {
        		failureReasons.add(SFAIL_DEFEND_MIN_AERO);
        	} else if (actualPercent > maxPercent) {
        		failureReasons.add(SFAIL_DEFEND_MAX_AERO);
        	}
        }

        
        int infCount = da.getNumberOfUnitTypes(Unit.INFANTRY, countSupport);
        infCount += da.getNumberOfUnitTypes(Unit.BATTLEARMOR, countSupport);
        int protoCount = da.getNumberOfUnitTypes(Unit.PROTOMEK, countSupport);
        if (protoCount > 0)
            infCount += Math.max(1, protoCount / 5);

        // Infantry min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (infCount > o.getIntValue("MaxDefenderInfantry"))
            failureReasons.add(SFAIL_DEFEND_MAXINFANTRY);
        else if (infCount < o.getIntValue("MinDefenderInfantry"))
            failureReasons.add(SFAIL_DEFEND_MININFANTRY);

        // NonInfantry min/max. Remember - these are for op qualification, not
        // related to limiters.
        if (da.getNumberOfUnitTypes(Unit.MEK) + da.getNumberOfUnitTypes(Unit.VEHICLE) + da.getNumberOfUnitTypes(Unit.AERO) > o.getIntValue("MaxDefenderNonInfantry"))
            failureReasons.add(SFAIL_DEFEND_MAXNONINFANTRY);
        else if (da.getNumberOfUnitTypes(Unit.MEK) + da.getNumberOfUnitTypes(Unit.VEHICLE) + da.getNumberOfUnitTypes(Unit.AERO) < o.getIntValue("MinDefenderNonInfantry"))
            failureReasons.add(SFAIL_DEFEND_MINNONINFANTRY);

        // Support Unit min/max
        if (da.getTotalSupportUnits() < o.getIntValue("MinDefenderSupportUnits")) {
        	failureReasons.add(SFAIL_DEFEND_TOO_FEW_SUPPORT_UNITS);
        } else if (da.getTotalSupportUnits() > o.getIntValue("MaxDefenderSupportUnits")) {
        	failureReasons.add(SFAIL_DEFEND_TOO_MANY_SUPPORT_UNITS);
        }
        
        // Non-Support Unit min/max
        if ((da.getAmountOfUnitsWithoutInfantry() - da.getTotalSupportUnits()) < o.getIntValue("MinDefenderNonSupportUnits")) {
        	failureReasons.add(SFAIL_DEFEND_TOO_FEW_NONSUPPORT_UNITS);
        } else if (da.getAmountOfUnitsWithoutInfantry() - da.getTotalSupportUnits() > o.getIntValue("MaxDefenderSupportUnits")) {
        	failureReasons.add(SFAIL_DEFEND_TOO_MANY_NONSUPPORT_UNITS);
        }        
        
        /*
         * loop through all units in the army, setting up remaining checks.
         */
        int totalWeight = 0;
        int largestWeight = 0;
        int numProtoMeks = da.getNumberOfUnitTypes(Unit.PROTOMEK);
        
        int numberOfCommanders = 0;
        boolean hasMeks = false;
        boolean hasVehs = false;
        boolean hasAeros = false;
        boolean hasInf = false;
        boolean normInf = false;
        boolean powerInf = false;
        boolean speedFail = false;
        boolean jumpTooFar = false;
        boolean maxTonFail = false;
        boolean minTonFail = false;
        boolean maxBVFail = false;
        boolean minBVFail = false;
        boolean omniFail = false;
        boolean checkOmni = o.getBooleanValue("DefenderOmniMeksOnly");
        boolean vetPilots = false;
        boolean greenPilots = false;
        double averageArmySkills = 0.0;
        int numberOfValidUnits = 0;
        boolean checkClantech = o.getBooleanValue("UseClanEquipmentRatios");
        int numClanUnits = 0;
        int numTotalUnits = 0;
        
        Iterator<Unit> i = da.getUnits().iterator();
        while (i.hasNext()) {

            // load the next unit
            SUnit currUnit = (SUnit) i.next();

            // Check to see if the unit is a commander
            numberOfCommanders = da.getCommanders().size();

            // get the unit's weight, store
            int currWeight = (int) currUnit.getEntity().getWeight();
            totalWeight += currWeight;
            if (currWeight > largestWeight)
                largestWeight = currWeight;

            if (currUnit.getType() == Unit.MEK) {
                hasMeks = true;
                if (checkOmni && !omniFail)
                    if (!currUnit.isOmni())
                        omniFail = true;
            } else if (currUnit.getType() == Unit.VEHICLE)
                hasVehs = true;
            else if ( currUnit.getType() == Unit.AERO )
                hasAeros = true;
            else if (currUnit.getType() == Unit.INFANTRY || currUnit.getType() == Unit.BATTLEARMOR || currUnit.getType() == Unit.PROTOMEK) {
                hasInf = true;
                if ((currUnit.getEntity() instanceof BattleArmor) || (currUnit.getEntity() instanceof Protomech))
                    powerInf = true;
                else
                    normInf = true;
            }

            // now, check the unit's walking and jumping speeds. only fail on
            // this once.
            if (!speedFail && !jumpTooFar) {
                try {
                    int walkMP = currUnit.getEntity().getWalkMP();
                    int jumpMP = currUnit.getEntity().getJumpMP();
                    if (walkMP < o.getIntValue("MinDefenderWalk") && jumpMP < o.getIntValue("MinDefenderJump"))
                        speedFail = true;
                    if (jumpMP > o.getIntValue("MaxDefenderJump")) {
                    	jumpTooFar = true;
                    }
                } catch (Exception ex) {
                }
            }// end if(hasn't already speedfail'ed)

            // check the unit's weight
            if (currUnit.getType() == Unit.MEK || currUnit.getType() == Unit.VEHICLE || currUnit.getType() == Unit.AERO) {
                if (currWeight > o.getIntValue("MaxDefenderUnitTonnage"))
                    maxTonFail = true;
                else if (currWeight < o.getIntValue("MinDefenderUnitTonnage"))
                    minTonFail = true;
            }
            // check the unit's BV
            int currBV = 0;
            if(o.getBooleanValue("IgnorePilotsForBVSpread")) {
            	currBV = currUnit.getBaseBV();
            } else {
            	currBV = currUnit.getBV();
            }
            if (currBV < o.getIntValue("MinDefenderUnitBV"))
                minBVFail = true;
            else if (currBV > o.getIntValue("MaxDefenderUnitBV"))
                maxBVFail = true;

            // check spreads. because the spreads can <code>continue</code> they
            // should be LAST
            int type = currUnit.getType();
            if (currUnit.isSupportUnit() && !o.getBooleanValue("CountSupportUnitsForSpread"))
            	continue;
            if (type == Unit.VEHICLE && !o.getBooleanValue("CountVehsForSpread"))
                continue;
            else if (type == Unit.PROTOMEK && !o.getBooleanValue("CountProtosForSpread"))
                continue;
            else if ((type == Unit.BATTLEARMOR || type == Unit.INFANTRY) && !o.getBooleanValue("CountInfForSpread"))
                continue;
            else if (type == Unit.AERO && !o.getBooleanValue("CountAerosForSpread"))
                continue;

            if ( !currUnit.hasVacantPilot() ) {
                int piloting = currUnit.getPilot().getPiloting();
                int gunnery = currUnit.getPilot().getGunnery();
                int totalSkills = gunnery+piloting;
                
                numberOfValidUnits++;
                averageArmySkills += totalSkills;
                
                if ( piloting > o.getIntValue("HighestDefenderPiloting") )
                    greenPilots = true;
                
                if ( piloting < o.getIntValue("LowestDefenderPiloting") )
                    vetPilots = true;
                
                if ( gunnery > o.getIntValue("HighestDefenderGunnery") )
                    greenPilots = true;
                
                if ( gunnery < o.getIntValue("LowestDefenderGunnery") )
                    vetPilots = true;

                if ( totalSkills > o.getIntValue("HighestDefenderPilotSkillTotal") )
                    greenPilots = true;
                
                if ( totalSkills < o.getIntValue("LowestDefenderPilotSkillTotal") )
                    vetPilots = true;
            }
            
            // Count total units and clan units
            numTotalUnits++;
            if(currUnit.getEntity().isClan()) {
            	numClanUnits++;
            }

        }// end while(units remain)

        // add unit exclusion failures to list
        if (hasMeks && !o.getBooleanValue("DefenderAllowedMeks"))
            failureReasons.add(SFAIL_DEFEND_NOMEKS);
        if (hasVehs && !o.getBooleanValue("DefenderAllowedVehs"))
            failureReasons.add(SFAIL_DEFEND_NOVEHS);
        if (hasAeros && !o.getBooleanValue("DefenderAllowedAeros"))
            failureReasons.add(SFAIL_DEFEND_NOAEROS);
        if (hasInf && !o.getBooleanValue("DefenderAllowedInf")) {
            // powered allowed, but there's unarmored inf too
            if (o.getBooleanValue("DefenderPoweredInfAllowed") && normInf)
                failureReasons.add(SFAIL_DEFEND_NONORMINF);
            if (o.getBooleanValue("DefenderStandardInfAllowed") && powerInf)
                failureReasons.add(SFAIL_DEFEND_NOPOWERINF);
            else if ( !normInf && !powerInf ) {
                // no infantry allowed, at all
                failureReasons.add(SFAIL_ATTACK_NOINF);
            }
        }// end if(!AllowedInf)
        if (checkOmni && omniFail)
            failureReasons.add(SFAIL_DEFEND_OMNIONLY);

        if (o.getBooleanValue("UseUnitCommander")) {
            if (numberOfCommanders < o.getIntValue("MinimumUnitCommanders"))
                failureReasons.add(SFAIL_COMMON_NOT_ENOUGH_COMMANDERS);
            if (numberOfCommanders > o.getIntValue("MaximumUnitCommanders"))
                failureReasons.add(SFAIL_COMMON_TOO_MANY_COMMANDERS);
        }

        // proto failures. wee.
        if (o.getBooleanValue("ProtosMustbeGrouped") && numProtoMeks > 0 && numProtoMeks % 5 != 0)
            failureReasons.add(SFAIL_COMMON_PROTOGROUPS);

        // add speed failure to list
        if (speedFail)
            failureReasons.add(SFAIL_DEFEND_MINSPEED);
        if (jumpTooFar) {
        	failureReasons.add(SFAIL_DEFEND_MAXJUMP);
        }
        // add max/min unit ton failures to list
        if (maxTonFail)
            failureReasons.add(SFAIL_DEFEND_MAXUNITTON);
        else if (minTonFail)
            failureReasons.add(SFAIL_DEFEND_MINUNITTON);

        // add max/min unit BV failures to list
        if (maxBVFail)
            failureReasons.add(SFAIL_DEFEND_MAXUNITBV);
        else if (minBVFail)
            failureReasons.add(SFAIL_DEFEND_MINUNITBV);

        // check total tonnage failures
        if (totalWeight > o.getIntValue("MaxTotalDefenderTonnage"))
            failureReasons.add(SFAIL_DEFEND_MAXARMYTON);
        else if (totalWeight < o.getIntValue("MinTotalDefenderTonnage"))
            failureReasons.add(SFAIL_DEFEND_MINARMYTON);

        // check unit BV difference failures
        if (spreadError == I_SpreadValidator.ERROR_SPREAD_TOO_LARGE) {
        	failureReasons.add(SFAIL_DEFEND_MAXSPREAD);
        } else if (spreadError == I_SpreadValidator.ERROR_SPREAD_TOO_SMALL) {
        	failureReasons.add(SFAIL_DEFEND_MINSPREAD);
        }
        
        averageArmySkills /= numberOfValidUnits;
        
        if (averageArmySkills > o.getDoubleValue("DefenderAverageArmySkillMax") ) {
        	failureReasons.add(SFAIL_DEFEND_SKILLSUM_TOOHIGH);
        }

        if (averageArmySkills < o.getDoubleValue("DefenderAverageArmySkillMin") ) {
        	failureReasons.add(SFAIL_DEFEND_SKILLSUM_TOOLOW);
        }

        
        if ( vetPilots )
            failureReasons.add(SFAIL_DEFEND_ELITE_PILOTS );
        
        if ( greenPilots )
            failureReasons.add(SFAIL_DEFEND_GREEN_PILOTS );
        
        if (checkClantech) {
        	double minClantech = o.getDoubleValue("DefenderMinClanEquipmentPercent");
        	double maxClantech = o.getDoubleValue("DefenderMaxClanEquipmentPercent");
        	double clanTechPercent = numClanUnits / numTotalUnits;
        	if (clanTechPercent < minClantech) {
        		failureReasons.add(SFAIL_DEFEND_TECHBASE_TOO_LITTLE_CLAN);
        	}
        	if (clanTechPercent > maxClantech) {
        		failureReasons.add(SFAIL_DEFEND_TECHBASE_TOO_MUCH_CLAN);
        	}
        }

    }// end checkDefenderConstruction

    /**
     * Method which checks an Army's eligibility vs. all available Operations
     * and updates its legalOperations TreeMap. Changes are collected in two
     * ArrayLists (addList, removeList).
     * 
     * If <code>display</code> is true, the client receives a standard system
     * message as well as the silent update.
     */
    public void checkOperations(SArmy a, boolean display, TreeMap<String, Operation> operations) {

        SPlayer p = CampaignMain.cm.getPlayer(a.getPlayerName());
        if (p == null)
            return;

        /*
         * If the player is not logged in, skip the checks. This saves us from
         * sending updates to disconnected players when their games
         * auto-resolve.
         */
        if (p.getDutyStatus() < SPlayer.STATUS_RESERVE)
            return;

        ArrayList<String> addNames = new ArrayList<String>();
        ArrayList<String> removeNames = new ArrayList<String>();

        for (Operation currType : operations.values()) {

            // check for failures. contruction and milestones only.
            ArrayList<Integer> failures = this.validateShortAttacker(p, a, currType, null, -1, false);

            // if there were failures, try to remve
            if (failures.size() > 0) {
                String removal = a.getLegalOperations().remove(currType.getName());
                if (removal != null)
                    removeNames.add(removal);
            }

            // no failures. add to the tree. if the key was connected
            // to a null previously, we need to notify the player.
            else if (a.getLegalOperations().put(currType.getName(), currType.getName()) == null)
                addNames.add(currType.getName());

        }// end for(each operation)

        // if there were changes, pre updates
        if (addNames.size() > 0 || removeNames.size() > 0) {

            // assemble PL| command string
            String toSend = "PL|UOE|" + a.getID() + "*";
            for (String currName : addNames)
                toSend += "a*" + currName + "*";
            for (String currName : removeNames)
                toSend += "r*" + currName + "*";

            // send command
            CampaignMain.cm.toUser(toSend, p.getName(), false);

            // if verbose, inform the players
            if (display) {

                String addSend = "AM:Army #" + a.getID();
                String removeSend = "AM:Army #" + a.getID();

                // add messages
                if (addNames.size() == 1) {
                    addSend += " gained access to an attack: " + addNames.get(0) + ".";
                    CampaignMain.cm.toUser(addSend, p.getName(), true);
                } else if (addNames.size() > 1) {
                    addSend += " gained access to the following attacks: ";
                    Iterator<String> i = addNames.iterator();
                    while (i.hasNext()) {
                        addSend += i.next();
                        if (i.hasNext()) {
                            addSend += ", ";
                        }
                    }
                    // try to remove the last instance of ", "
                    int lastComma = addSend.lastIndexOf(", ");
                    if (lastComma >= 0) {
                        String front = addSend.substring(0, lastComma);
                        String back = addSend.substring(lastComma + 2, addSend.length());
                        addSend = front + " and " + back + ".";
                    }
                    CampaignMain.cm.toUser(addSend, p.getName(), true);
                }

                // remove messages
                if (removeNames.size() == 1) {
                    removeSend += " lost access to an attack: " + removeNames.get(0) + ".";
                    CampaignMain.cm.toUser(removeSend, p.getName(), true);
                } else if (removeNames.size() > 1) {
                    removeSend += " lost access to the following attacks: ";
                    Iterator<String> i = removeNames.iterator();
                    while (i.hasNext()) {
                        removeSend += i.next();
                        if (i.hasNext()) {
                            removeSend += ", ";
                        }
                    }
                    // try to remove the last instance of ", "
                    int lastComma = removeSend.lastIndexOf(", ");
                    if (lastComma >= 0) {
                        String front = removeSend.substring(0, lastComma);
                        String back = removeSend.substring(lastComma + 2, removeSend.length());
                        removeSend = front + " and " + back + ".";
                    }
                    CampaignMain.cm.toUser(removeSend, p.getName(), true);
                }

            }// end if(display)
        }// end (legal types were added or removed)
    }// end checkOperations()

    /**
     * Method which takes a failure arraylist and generates human-readible
     * reasons for an attack failure. Public.
     */
    public String failuresToString(ArrayList<Integer> failList) {

        String s = "";
        if (failList.size() == 1){
            return s += " because:<br>- " + this.decodeFailure((Integer) failList.get(0)) + ".";
        }
        
        s += "because:<br>";
        Iterator<Integer> i = failList.iterator();
        while (i.hasNext()) {
            s += "- " + this.decodeFailure(i.next());
            if (i.hasNext())
                s += "<br>";
        }

        return s;
    }

    /**
     * Private helper which decodes failure codes.
     */
    private String decodeFailure(Integer code) {

        int decoded = code.intValue();
        switch (decoded) {

        /*
         * COMMON failure causes
         */
        case SFAIL_COMMON_PROTOGROUPS: // "MaxAttackerBV" - BV ceiling,
            // contruction prop
            return " the army includes an illegal number of protomechs. Protos must deploy in 5-unit points.";

        case SFAIL_COMMON_ELODIFFERENCE:
            return " Difference in ELO was too high.";

        case SFAIL_COMMON_TEAM_BV_EXCEEDED:
            return " Team total bv exceeded.";

        case SFAIL_COMMON_NOT_ENOUGH_COMMANDERS:
            return " not enough commanders in army";

        case SFAIL_COMMON_TOO_MANY_COMMANDERS:
            return " too many commanders in army";

        case SFAIL_COMMON_INSUFFICENT_SUBFACTION_ACCESS_LEVEL:
            return " Sub-Factions access level is lower then the operations min. required access level.";

        case SFAIL_COMMON_MAX_BV_DIFFERENCE:
            return " BV difference between attacking and defending army is too large.";

        case SFAIL_COMMON_INFACTION_ATTACK:
            return " Intra-Faction attacks not allowed.";

            /*
             * ATTACK failure causes
             */
        case SFAIL_ATTACK_MAXBV: // "MaxAttackerBV" - BV ceiling, contruction
            // prop
            return " the army is over the BV limit for this type of attack";

        case SFAIL_ATTACK_MINBV: // "MinAttackerBV" - BV floor, contruction
            // prop
            return " the army is under the BV limit for this type of attack";

        case SFAIL_ATTACK_MAXMEKS: // "MaxAttackerMeks" - unit ceiling,
            // construction prop
            return " the army has too many Meks";

        case SFAIL_ATTACK_MINMEKS: // "MinAttackerMeks" - unit floor,
            // contruction prop
            return " the army does not have enough Meks";

        case SFAIL_ATTACK_MAXVEHICLES: // "MaxAttackerVehicles" - unit ceiling,
            // construction prop
            return " the army has too many Vehicles";

        case SFAIL_ATTACK_MINVEHICLES: // "MinAttackerVehicles" - unit floor,
            // contruction prop
            return " the army does not have enough Vehicles";

        case SFAIL_ATTACK_MAXINFANTRY: // "MaxAttackerInfantry" - unit ceiling,
            // construction prop
            return " the army has too many Infantry";

        case SFAIL_ATTACK_MININFANTRY: // "MinAttackerInfantry" - unit floor,
            // contruction prop
            return " the army does not have enough Infantry";

        case SFAIL_ATTACK_MAXNONINFANTRY: // "MaxAttackerNonInfantry" - unit
            // ceiling, construction prop
            return " the army has too many non-Infantry";

        case SFAIL_ATTACK_MINNONINFANTRY: // "MinAttackerNonInfantry" - unit
            // floor, contruction prop
            return " the army does not have enough non-Infantry";

        case SFAIL_ATTACK_MINSPEED: // "MinAttackerWalk" and "MinAttackerJump",
            // construction props
            return " the army contains a unit which is too slow";

        case SFAIL_ATTACK_MAXUNITTON: // "MaxAttackerUnitTonnage",
            // construction prop
            return " the army contains a unit which is too heavy";

        case SFAIL_ATTACK_MINUNITTON: // "MinAttackerUnitTonnage",
            // construction prop
            return " the army contains a unit which is too light";

        case SFAIL_ATTACK_NOMEKS: // "AttackerAllowedMeks", construction prop
            return " the army contains meks, which may not participate in this type of attack";

        case SFAIL_ATTACK_NOVEHS: // "AttackerAllowedVehs", construction prop
            return " the army contains vehicles, which may not participate in this type of attack";

        case SFAIL_ATTACK_NOINF: // "AttackerAllowedInf",construction prop
            return " the army contains infantry, which may not participate in this type of attack";

        case SFAIL_ATTACK_NONORMINF:// "PoweredInfAllowed," but unarmored inf
            // present, contruction prop
            return " the army contains conventional infantry, which may not participate in this type of attack";

        case SFAIL_ATTACK_MAXARMYTON: // "MaxTotalAttackerTonnage",
            // construction prop
            return " the army is over the tonnage maximum for this type of attack";

        case SFAIL_ATTACK_MINARMYTON:// "MinTotalAttackerTonnage",
            // contruction prop
            return " the army is under the tonnage minimum for this type of attack";

        case SFAIL_ATTACK_MONEY:// "AttackerCostMoney", cost prop
            return " you cannot afford the attack";

        case SFAIL_ATTACK_INFLUENCE:// "AttackerCostInfluence", cost prop
            return " you do not have enough " + CampaignMain.cm.getConfig("FluLongName") + " for this type of attack";

        case SFAIL_ATTACK_REWARD:// "AttackerCostReward", cost prop
            return " you do not have enough " + CampaignMain.cm.getConfig("RPShortName") + " for this type of attack";

        case SFAIL_ATTACK_MAXRATING:// "MaxAttackerRating", milestone prop
            return " your rating is too high for this type of attack";

        case SFAIL_ATTACK_MINRATING:// "MinAttackerRating", milestone prop
            return " your rating is too low for this type of attack";

        case SFAIL_ATTACK_MAXXP:// "MaxAttackerXP", contruction prop
            return " you have too much experience for this type of attack";

        case SFAIL_ATTACK_MINXP:// "MinAttackerXP", contruction prop
            return " you have too little experience for this type of attack";

        case SFAIL_ATTACK_MAXGAMES:// "MaxAttackerGamesPlayed", contruction
            // prop
            return " you're played too many games to perform this type of attack";

        case SFAIL_ATTACK_MINGAMES:// "MinAttackerGamesPlayed", contruction
            // prop
            return " you've played too few games to perform this type of attack";

        case SFAIL_ATTACK_OUTOFRANGE:// Catchall for range property failures.
            return " the target world is out of range";

        case SFAIL_ATTACK_NOOPPONENT:// No matching opponents for the army,
            // at all
            return " no active opponents may fight against this Army";

        case SFAIL_ATTACK_NOPLANDEF:// Opponents, but none eligible for this
            // planet
            return " no active opponents may defend the target world";

        case SFAIL_ATTACK_NOTYPEDEF:// Opponents, but none eligible for this op
            // type
            return " no active opponents may defend this type of attack";

        case SFAIL_ATTACK_SOLCANTATT:// Attack type forbidden for training
            // players
            return " players in the training faction are not allowed to use this type of attack";

        case SFAIL_ATTACK_NON_CONQ_A:// Attack type forbidden for non-conquer
            // players
            return " players in non-conquer factions are not allowed to use this type of attack";

        case SFAIL_ATTACK_NEEDSFAC:// Attack type only legal on a world with a
            // factory
            return " the target has no factory";

        case SFAIL_ATTACK_HASFAC:// Attack type forbidden on worlds with
            // factories
            return " this type of attack may only be performed on worlds without factories";

        case SFAIL_ATTACK_NEEDSHOME:// Attack type only legal on a homeworld
            return " the target is not a homeworld";

        case SFAIL_ATTACK_HASHOME:// Attack type forbidden on homeworlds
            return " this type of attack may only be performed a homeworld";

        case SFAIL_ATTACK_MINUNITBV:// Attack type forbidden on worlds with
            // factories
            return " the army contains a unit with a BV too low for this type of attack";

        case SFAIL_ATTACK_MAXUNITBV:// Attack type forbidden on worlds with
            // factories
            return " the army contains a unit with a BV too high for this type of attack";

        case SFAIL_ATTACK_MAXSPREAD:// Attack type forbidden on worlds with
            // factories
            return " the BV difference between the largest and smallest counted units is too large";

        case SFAIL_ATTACK_MINSPREAD:// Attack type forbidden on worlds with
            // factories
            return " the BV difference between the largest and smallest counted units is too small";

        case SFAIL_ATTACK_FACTION:// Attack is forbidden for this faction
            return " your faction may not make this type of attack";

        case SFAIL_ATTACK_MINOWNERSHIP:// Attacker owns too little of target
            // world
            return " your faction controls too little of the target planet";

        case SFAIL_ATTACK_MAXOWNERSHIP:// Attacker controls too much of the
            // world
            return " your faction controls too much of the target planet";

        case SFAIL_ATTACK_OMNIONLY:// Attacker must only use Omnimeks for this
            // op
            return " your army can only have meks that are Omnimeks for this op";

        case SFAIL_ATTACK_OPFLAGS: // Planet must have specific ops.
            return " The target planet did not contain the correct Op Flags";

        case SFAIL_ATTACK_DOPFLAGS: // Planet must not have specific ops.
            return " the target planet contained an op flag that was not allowed for this operation.";

        case SFAIL_ATTACK_NOPOWERINF:// "StandardInfAllowed," but armored inf
            // present, contruction prop
            return " the army contains armored infantry, which may not participate in this type of attack";

        case SFAIL_ATTACK_NOCOUNTERS:
            return " you are under attack, and may not counterattack using this type of attack.";

        case SFAIL_ATTACK_AFRONLY:// "OnlyAllowedFromReserve", but player is
            // trying to use while active
            return " this operation must be initiated by a player on reserve duty (use AFR)";

        case SFAIL_ATTACK_ACTIVEONLY:// "OnlyAllowedFromActive", but player is trying to use via AttackFromReserve
            return " this operation may only be initated by a player on active duty";

        case SFAIL_ATTACK_ELITE_PILOTS:// Army has pilots whoes total skill is lower then the lowest allowed
            return " this army has eilte pilots.";

        case SFAIL_ATTACK_GREEN_PILOTS:// Army has pilots whoes total skill is higher then the highest allowed
            return " this army has green pilots.";

        case SFAIL_ATTACK_SKILLSUM_TOOHIGH: // Army's average skillsum is too high
        	return " this army's pilots are not skilled enough for the operation.";

        case SFAIL_ATTACK_SKILLSUM_TOOLOW: // Army's average skillsum is too low
        	return " this army's pilots are too skilled for the operation.";
            
        case SFAIL_ATTACK_MAX_AERO:
            return " the army has too many aeros";

        case SFAIL_ATTACK_MIN_AERO:
            return " the army does not have enough aeros";
        
        case SFAIL_ATTACK_NOAEROS:
            return " the army contains aeros, which may not participate in this type of attack";

        case SFAIL_ATTACK_TECHBASE_TOO_LITTLE_CLAN:
        	return " the army does not contain enough clantech";
        	
        case SFAIL_ATTACK_TECHBASE_TOO_MUCH_CLAN:
        	return " the army contains too much clantech";
        	
        case SFAIL_ATTACK_TOO_MANY_SUPPORT_UNITS:
        	return " the army has too many support units";
        
        case SFAIL_ATTACK_TOO_FEW_SUPPORT_UNITS:
        	return " the army does not have enough support units";
        	
        case SFAIL_ATTACK_TOO_MANY_NONSUPPORT_UNITS:
        	return " the army has too many non-support units";
        	
        case SFAIL_ATTACK_TOO_FEW_NONSUPPORT_UNITS:
        	return " the army does not have enough non-support units";
        	
        case SFAIL_ATTACK_MISSING_REQUIRED_FLAG:
        	return " you do not have a required Player Flag set";
        	
        case SFAIL_ATTACK_HAS_BANNED_FLAG:
        	return " you have a Player Flag set that is banned from this attack";
        
        case SFAIL_ATTACK_MAXJUMP:
        	return " the army contains a unit that jumps too far.";
        	
            /*
             * DEFENSE failure causes
             */
        case SFAIL_DEFEND_MAXBV: // "MaxDefenderBV" - BV ceiling, contruction
            // prop
            return " the army is over the BV limit for this type of defense";

        case SFAIL_DEFEND_MINBV: // "MinDefenderBV" - BV floor, contruction
            // prop
            return " the army is under the BV limit for this type of defense";

        case SFAIL_DEFEND_MAXMEKS: // "MaxDefenderMeks" - unit ceiling,
            // construction prop
            return " the army has too many meks";

        case SFAIL_DEFEND_MINMEKS: // "MinDefenderMeks" - unit floor,
            // contruction prop
            return " the army does not have enough meks";

        case SFAIL_DEFEND_MAXVEHICLES: // "MaxDefenderVehicles" - unit ceiling,
            // construction prop
            return " the army has too many vehicles";

        case SFAIL_DEFEND_MINVEHICLES: // "MinDefenderVehicles" - unit floor,
            // contruction prop
            return " the army does not have enough vehicles";

        case SFAIL_DEFEND_MAXINFANTRY: // "MaxDefenderInfantry" - unit ceiling,
            // construction prop
            return " the army has too many infantry";

        case SFAIL_DEFEND_MININFANTRY: // "MinDefenderInfantry" - unit floor,
            // contruction prop
            return " the army does not have enough infantry";

        case SFAIL_DEFEND_MAXNONINFANTRY: // "MaxDefenderNonInfantry" - unit
            // ceiling, construction prop
            return " the army has too many non-infantry";

        case SFAIL_DEFEND_MINNONINFANTRY: // "MinDefenderNonInfantry" - unit
            // floor, contruction prop
            return " the army does not have enough non-infantry";

        case SFAIL_DEFEND_MINSPEED: // "MinDefenderWalk" and "MinDefenderJump",
            // construction props
            return " the army contains a unit which is too slow";

        case SFAIL_DEFEND_MAXUNITTON: // "MaxDefenderUnitTonnage",
            // construction prop
            return " the army contains a unit which is too heavy";

        case SFAIL_DEFEND_MINUNITTON: // "MinDefenderUnitTonnage",
            // construction prop
            return " the army contains a unit which is too light";

        case SFAIL_DEFEND_NOMEKS: // "DefenderAllowedMeks", construction prop
            return " the army contains meks, which may not participate in this type of defense";

        case SFAIL_DEFEND_NOVEHS: // "DefenderAllowedVehs", construction prop
            return " the army contains vehicles, which may not participate in this type of defense";

        case SFAIL_DEFEND_NOINF: // "DefenderAllowedInf",construction prop
            return " the army contains infantry, which may not participate in this type of defense";

        case SFAIL_DEFEND_NONORMINF:// "PoweredInfAllowed," but unarmored inf
            // present, contruction prop
            return " the army contains conventional infantry, which may not participate in this type of defense";

        case SFAIL_DEFEND_MAXARMYTON: // "MaxTotalDefenderTonnage",
            // construction prop
            return " the army is over the tonnage maximum for this type of defense";

        case SFAIL_DEFEND_MINARMYTON:// "MinTotalDefenderTonnage",
            // contruction prop
            return " the army is under the tonnage minimum for this type of defense";

        case SFAIL_DEFEND_MONEY:// "AttackerCostMoney", cost prop
            return " you cannot afford the defense";

        case SFAIL_DEFEND_INFLUENCE:// "AttackerCostInfluence", cost prop
            return " you do not have enough influence for this type of defense";

        case SFAIL_DEFEND_REWARD:// "AttackerCostReward", cost prop
            return " you do not have enough " + CampaignMain.cm.getConfig("RPShortName") + " for this type of defense";

        case SFAIL_DEFEND_MAXRATING:// "MaxAttackerRating", milestone prop
            return " your rating is too high for this type of defense";

        case SFAIL_DEFEND_MINRATING:// "MinAttackerRating", milestone prop
            return " your rating is too low for this type of defense";

        case SFAIL_DEFEND_MAXXP:// "MaxAttackerXP", contruction prop
            return " you have too much experience for this type of defense";

        case SFAIL_DEFEND_MINXP:// "MinAttackerXP", contruction prop
            return " you have too little experience for this type of defense";

        case SFAIL_DEFEND_MAXGAMES:// "MaxAttackerGamesPlayed", contruction
            // prop
            return " you're played too many games to perform this type of defense";

        case SFAIL_DEFEND_MINGAMES:// "MinAttackerGamesPlayed", contruction
            // prop
            return " you've played too few games to perform this type of defense";

        case SFAIL_DEFEND_SOLCANTDEF:// Defend type forbidden for training
            // players
            return " players in the training faction are not allowed to defend against this type of attack";

        case SFAIL_DEFEND_NON_CONQ_D:// Defend type forbidden for non-conquer
            // players
            return " players in non-conquer factions are not allowed to defend against this type of attack";

        case SFAIL_DEFEND_MINUNITBV:// Attack type forbidden on worlds with
            // factories
            return " the army contains a unit with a BV too low for this type of defense";

        case SFAIL_DEFEND_MAXUNITBV:// Attack type forbidden on worlds with
            // factories
            return " the army contains a unit with a BV too high for this type of defense";

        case SFAIL_DEFEND_MAXSPREAD:// Attack type forbidden on worlds with
            // factories
            return " the BV difference between the largest and smallest counted units is too large";

        case SFAIL_DEFEND_MINSPREAD:// Attack type forbidden on worlds with
            // factories
            return " the BV difference between the largest and smallest counted units is too small";

        case SFAIL_DEFEND_FACTION:
            return " your faction may not defend this type of attack";

        case SFAIL_DEFEND_OMNIONLY:// Defender can only use Omnimeks in this op
            return " your army can only have meks that are Omnimeks for this op";

        case SFAIL_DEFEND_NOTPLANDEF:// Defenders faction does not own any
            // land of the target planet.
            return " defender does not have any ownship of target world";

        case SFAIL_DEFEND_NOPOWERINF:// "StandardAllowed," but BA or Protos
            // present, contruction prop
            return " the army contains armored infantry, which may not participate in this type of defense";

        case SFAIL_DEFEND_NON_CONQ_PLANET:// defender or attacker might gain
            // conquest points and the planet
            // cannot be conquered
            return " the planet is non-conqerable and this operation allows for conqest";

        case SFAIL_DEFEND_ELITE_PILOTS:// Army has pilots whoes total skill is lower then the lowest allowed
            return " this army has eilte pilots.";
            
        case SFAIL_DEFEND_SKILLSUM_TOOHIGH: // Army's average skillsum is too high
        	return " this army's pilots are not skilled enough for the operation.";

        case SFAIL_DEFEND_SKILLSUM_TOOLOW: // Army's average skillsum is too low
        	return " this army's pilots are too skilled for the operation.";

        case SFAIL_DEFEND_GREEN_PILOTS:// Army has pilots whoes total skill is higher then the highest allowed
            return " this army has green pilots.";

        case SFAIL_DEFEND_MAX_AERO:
            return " the army has too many aeros";

        case SFAIL_DEFEND_MIN_AERO:
            return " the army does not have enough aeros";
        
        case SFAIL_DEFEND_NOAEROS:
            return " the army contains aeros, which may not participate in this type of defense";
            
        case SFAIL_DEFEND_TECHBASE_TOO_LITTLE_CLAN:
        	return " the army does not contain enough clantech";
        	
        case SFAIL_DEFEND_TECHBASE_TOO_MUCH_CLAN:
        	return " the army contains too much clantech";
        	
        case SFAIL_DEFEND_TOO_FEW_SUPPORT_UNITS:
        	return " the army does not have enough support units";
        	
        case SFAIL_DEFEND_TOO_MANY_SUPPORT_UNITS:
        	return " the army has too many support units";
        	
        case SFAIL_DEFEND_TOO_FEW_NONSUPPORT_UNITS:
        	return " the army does not have enough non-support units";
        	
        case SFAIL_DEFEND_TOO_MANY_NONSUPPORT_UNITS:
        	return " the army has too many non-support units";

        case SFAIL_DEFEND_MISSING_REQUIRED_FLAG:
        	return " your opponent does not have a required Player Flag set";
        	
        case SFAIL_DEFEND_HAS_BANNED_FLAG:
        	return " your opponent has a Player Flag set that is banned from this attack";

        case SFAIL_DEFEND_MAXJUMP:
        	return " your opponent's army contains a unit that jumps too far.";
        }
        
        return "";
    }

}// end ShortValidator class
