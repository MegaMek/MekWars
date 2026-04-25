/*
 * MekWars - Copyright (C) 2005
 *
 * Original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/*
 * A set of default values for an Operation. If an Operation does not have a value specified for a paramater, the default is used.
 *
 * DefaultOperation is useful in several ways:
 *
 * First, it makes Ops fault tolerant. Typos may very well break results, but at least they won't crash a server.
 *
 * Second, makes adding new paramaters much simpler. So long as new params are added such that they have no impact on Operations (eg- default to disuse),
 * running servers can load new versions without updating/expanding their Operations datasets.
 *
 * NOTE: Defaults stored in a TreeMap, -not- a hash.
 */
package common.campaign.operations;

// IMPORTS
import java.util.TreeMap;

public class DefaultOperation {

    // IVARS

    // the property tree
    TreeMap<String, String> operationsDefaults;

    // CONSTRUCTORS
    public DefaultOperation() {

        // create the treemap
        operationsDefaults = new TreeMap<String, String>();

        // ADD DEFAULTS

        /*
         * Note: The following values MUST BE SET for each operation
         * and have no recourse to default. EVERY long operation MUST
         * have a matching short operation with the same name.
         *
         * 1) OperationName 	- name (eg - "Assault" or "Conquer"). SET BY FILENAME!
         * 2) OperationType		- long, short, special. SET BY DIRECTORY STRUCTURE!
         * 3) OperationLength	- *long only,* number of games to play before closing
         * 4) LongVictoryThresh	- *long only,* number of wins needed, out of Length, to "win."
         * 5) LinkedOperations	- *special only,* names of short ops that a special can
         *                        be applied to, ; delimited. any number.
         */

        /*
         * DebugOp - This is used to send debug message to error logs so the
         *           SO's can debug issues with their ops. Currently it only
         *           debugs failed defender messages
         */
        operationsDefaults.put("DebugOp", "false");

        /*
         * MISC. VARIABLES. These are params necessary for either a long
         * operation or stand alone shorts ... or simply don't belong
         * anywhere else in the set (eg - OnlyAgainstFactoryWorlds).
         *
         * COLOUR AND RANGE SET HERE!
         *
         * Unlike most values, these range/reach variable are non-overlapping
         * and are ALWAYS USED. The defaults do NOT mimic the previous task-ranging.
         *
         * Variables are:
         * -------------
         * -> Read in ShortValidator.checkAttackerRange()
         *
         * Color                       - HTML Colour, for Maps and Menus
         *
         * OperationRange              - Range, in LY, of the Op.
         * OnlyAgainstFactoryWorlds    - Limit attack to planets with factories    - AFC=228
         * OnlyAgainstNonFactoryWorlds - Limit attack to planets without factories - AFC=229
         * PercentageToAttackOnWorld   - % required to launch an attack on-planet
         * PercentageToAttackOffWorld  - % required to launch an attack off-planet
         * MinPlanetOwnership          - % a faction must own for world to be legal target     - AFC=235
         * MinPlanetOwnershipIgnoredByDefender - if partially owned and MinPlanetOwnership>0 then MinPlanetOwnership will be ignored for the defender's legibility for the op //Baruk Khazad! - 20151003
         * MaxPlanetOwnership          - If faction owns > max %, world is not a legal target  - AFC=236
         * OnlyAgainstHomeWorlds       - Limit attack to planets that are homeworlds
         * OnlyAgainstNonHomeWorlds    - Limit attack to planets that are not homeworlds
         * RealBlindDrop               - Set Real Blind Drop of this op
         * ReportOpToNewsFeed          - default to false. Set true if you want the op reported to the server's News Feed.
         * DoesNotCountForPP           - Armies built just for this OP will not count towards production.
         * AllowPlanetFlags            - Flags the planet must have to allow this op.
         * DisallowPlanetFlags         - Flags the planet cannont have for this op to be allowed.
         * ForbidCounterAttacks        - Stop players from using this attack if under attack themselves.
         * OnlyAllowedFromReserve      - Stop players from using this attack if they are on active duty.
         * OnlyAllowedFromActive       - Stop players from using this attack if they are on reserve duty.
         * IndividualInit              - Allow Individual Init to be set in MM for this game.
         * MaxELODifference            - Max Difference in ELO between attacker and defender.
         * MinSubFactionAccessLevel    - Your SubFactionAcessLevel must be this high to ride. Default 0
         * MaxBVDifference			   - Max BV difference between attacker and defender. Default 150
         * MaxBVPercent				   - Max % BV diffence between Attacker and defneder. default 0%
         * NightChance				   - Chance that operation will take place at night.
         * DuskChance				   - Chance that operation will take place at dusk.
         * AttackerBriefing            - Message attacking player receives when launching
         * DefenderBriefing            - Message defending player receives when launching
         * AutoresolveBattle           - Determines if the battle is played with megamek or autoresolved
         *
         * NOTE: Colours are HTML hexidecimal. Keywords cannot be used. # must
         *       lead the string or massive fuck-ups will ensue.
         *
         * [NOTE: Min/Max planet ownership is distinct from the launch percents. Launch is used
         *        to determine ranges and general ability to "reach" a planet. Min/Max ownership
         *        is intended to allow segretation of games based on the current state of the
         *        world.
         *
         *        Example segregated attacks:
         *        "Initial Assault" - MaxPlanetOwnership of "0"
         *        "Beachhead Expansion" - Min of 1, Max of 15
         *        "Planetary Invasion" - Min of 16, Max of 80. Requires large armies.
         *        "Planetary Mop-up" - Min of 81, Requires fast/light units.]
         *
         * TODO: Improve colour options by adding RGB support
         */
        operationsDefaults.put("OperationColor", "#33FFFF");// aqua-ish
        operationsDefaults.put("OperationRange", "30");// one jump
        operationsDefaults.put("OnlyAgainstFactoryWorlds", "false");
        operationsDefaults.put("OnlyAgainstNonFactoryWorlds", "false");
        operationsDefaults.put("PercentageToAttackOnWorld", "50");// own or push to attack
        operationsDefaults.put("PercentageToAttackOffWorld", "50");// own or push to attack
        operationsDefaults.put("MinPlanetOwnership", "0");// attack anywhere
        operationsDefaults.put("MinPlanetOwnershipIgnoredByDefender", "false");//Baruk Khazad! - 20151003
        operationsDefaults.put("MaxPlanetOwnership", "100");// attack anywhere
        operationsDefaults.put("OnlyAgainstHomeWorlds", "false");
        operationsDefaults.put("OnlyAgainstNonHomeWorlds", "false");
        operationsDefaults.put("RealBlindDrop", "false");
        operationsDefaults.put("ReportOpToNewsFeed", "false");
        operationsDefaults.put("DoesNotCountForPP", "false");
        operationsDefaults.put("AllowPlanetFlags", "");
        operationsDefaults.put("DisallowPlanetFlags", "");
        operationsDefaults.put("ForbidCounterAttacks", "false");
        operationsDefaults.put("OnlyAllowedFromReserve", "false");
        operationsDefaults.put("OnlyAllowedFromActive", "false");
        operationsDefaults.put("IndividualInit", "false");
        operationsDefaults.put("MaxELODifference", "0");
        operationsDefaults.put("MinSubFactionAccessLevel", "0");
        operationsDefaults.put("MaxBVDifference", "150");
        operationsDefaults.put("MaxBVPercent", "0.0");
        operationsDefaults.put("NightChance", "0");
        operationsDefaults.put("DuskChance", "0");
        operationsDefaults.put("AttackerBriefing", "");
        operationsDefaults.put("DefenderBriefing", "");
        operationsDefaults.put("AutoresolveBattle", "false");

        /*
         * SHORT VARIABLES. These are params which are necessary for
         * all operations. Note: Short and long variables will often
         * overlap.
         *
         * NOTE: Fail Codes are appended for convenience. These are the
         *       codes assigned in ShortValidator when a paramater check
         *       isn't passed. Codes and paramaters are not necessarily
         *       in the same order, as they're added at different times.
         *
         * Variable are:
         * ------------
         *
         * [Construction properties]
         * -> Read in ShortValidator.checkAttackerConstruction
         * -> and ShortValidator.checkDefenderConstruction.
         *
         * MaxAttackerBV - self evident - AFC=200
         * MaxDefenderBV - self evident - DFC=400
         * MinAttackerBV - self evident - AFC=201
         * MinDefenderBV - self evident - DFC=401
         *
         * MaxAttackerMeks - self evident - AFC=202
         * MaxDefenderMeks - self evident - DFC=402
         *
         * MinAttackerMeks - self evident - AFC=203
         * MinDefenderMeks - self evident - DFC=403
         *
         * MaxAttackerVehicles  - self evident - AFC= 239
         * MaxDefenderVehicles  - self evident - DFC= 437
         *
         * MinAttackerVehicles  - self evident - AFC= 240
         * MinDefenderVehicles  - self evident - DFC= 438
         *
         * MaxAttackerInfantry  - self evident - AFC= 241
         * MaxDefenderInfantry  - self evident - DFC= 439
         *
         * MinAttackerInfantry  - self evident - AFC= 242
         * MinDefenderInfantry  - self evident - DFC= 440
         *
         * MaxAttackerAero  - self evident - AFC= 254
         * MaxDefenderAero  - self evident - DFC= 255
         *
         * MinAttackerAero  - self evident - AFC= 455
         * MinDefenderAero  - self evident - DFC= 456
         *
         * MaxAttackerNonInfantry  - self evident - AFC= 250
         * MaxDefenderNonInfantry  - self evident - DFC= 450
         *
         * MinAttackerNonInfantry  - self evident - AFC= 251
         * MinDefenderNonInfantry  - self evident - DFC= 451
         *
         * [CAUTION: Setting these too far apart can create situations
         *  where players can see each other with check tools but are
         *  unable to launch attacks, and vice versa]
         *
         * MinAttackerWalk - self evident - AFC=204, shared with MinAttackerJump
         * MinDefenderWalk - self evident - DFC=404, shared with MinDefenderJump
         *
         * MinAttackerJump - self evident - AFC=204, shared with MinAttackerWalk
         * MinDefenderJump - self evident - DFC=404, shared with MinDefenderWalk
         *

         * [NOTE: A unit can qualify for speed through walk OR jump. Set
         *  either walk or jump extremely high in order to make this a
         *  single element check.
         *
         *  Examples:
         *  - set jump to 90 and walk to 7 to ensure a ground MP of 7.
         *  - set jump to 7 and walk to 9 to allow only units which can
         *    EITHER jump 7 (Wraith) or cruise/walk 9 (Hermes).
         *  - set walk to 90 and jump to 5 to ensure that all units in a
         *    force can jump at least 5 hexes.]
         *
         * MaxAttackerUnitTonnage - highest weight for a single unit - AFC=205
         * MaxDefenderUnitTonnage - highest weight for a single unit - DFC=405
         *
         * MinAttackerUnitTonnage - lowest weight for a single unit - AFC=206
         * MinDefenderUnitTonnage - lowest weight for a single unit - DFC=406
         *
         * MaxAttackerUnitBV - highest BV for a single unit - AFC=231
         * MaxDefenderUnitBV - highest BV for a single unit - DFC=431
         *
         * MinAttackerUnitBV - lowest BV for a single unit - AFC=230
         * MinDefenderUnitBV - lowest BV for a single unit - DFC=430
         *
         * MaxAttackerUnitBVSpread - [Spread is the BV difference between the highest
         * MaxDefenderUnitBVSpread - [unit in a force and the losest unit in a force.
         * AFC=232; DFC=432          [Set <= 0 to disable. Defaults to 0.
         *
         * MinAttackerUnitBVSpread - [Like above, but used to ensure a minimum level
         * MinDefenderUnitBVSpread - [of difference between units. EG - set to 100 to
         *                           [ensure that there is a difference of at least 100
         * AFC=233; DFC=433          [between the highest and lowest units. Probably only
         *                           [interesting to create "Buddy" games where a high BV
         *                           [mech has to pair with a low in a tag team.
         *                           [Set <= 0 to DISABLE. Defaults to 0.
         *
         * HighestAttackerPilotSkillTotal - Highest total skill, Gunnery + Piloting, that an
         *                                    attacking Units pilot can have
         * LowestAttackerPilotSkillTotal  - Lowest total skill, Gunnery + Piloting, that an
         *                                    attacking Units pilot can have
         * HighestAttackerPiloting - Highest Piloting that an
         *                                    attacking Units pilot can have
         * LowestAttackerPiloting  - Lowest Piloting that an
         *                                    attacking Units pilot can have
         * HighestAttackerGunnery - Highest Gunnery that an
         *                                    attacking Units pilot can have
         * LowestAttackerGunnery  - Lowest = Gunnery that an
         *                                    attacking Units pilot can have
         *
         * HighestDefenderPilotSkillTotal - Highest total skill, Gunnery + Piloting, that a
         *                                    defending Units pilot can have
         * LowestDefenderPilotSkillTotal  - Lowest total skill, Gunnery + Piloting, that a
         *                                    defending Units pilot can have
         * HighestDefenderPiloting - Highest Piloting that an
         *                                    defending Units pilot can have
         * LowestDefenderPiloting  - Lowest Piloting that an
         *                                    defending Units pilot can have
         * HighestDefenderGunnery - Highest Gunnery that an
         *                                    defending Units pilot can have
         * LowestDefenderGunnery  - Lowest = Gunnery that an
         *                                    defending Units pilot can have
         *
         * AttackerAverageArmySkillMax - The Max average total skills for the Attacker army
         * AttackerAverageArmySkillMin - The Min average total skills for the Attacker army
         * DefenderAverageArmySkillMax - The Max average total skills for the Defender army
         * DefenderAverageArmySkillMin - The Min average total skills for the Defender army
         *
         * CountVehsForSpread   - self evident
         * CountProtosForSpread - self evident
         * CountInfForSpread    - self evident
         * CountAerosForSpread    - self evident
         *
         * [NOTE: Spreads are a very dangerous feature. They're GREAT for stopping high/low
         *  unit pairs and "init sinks." However, they also make it virtually impossible to
         *  use infantry and protomeks, or small vehicles like the Vedette. Options exist to
         *  stop spread checks on these unit types (Meks will always be checked), but these
         *  may be unbalancing in and of themselves. The recommended solution for servers which
         *  want to use spreads AND combined arms is to set a very wide spread ... something in
         *  the range of 1000 BV ... not count protos/BV for spread, and enforce Infantry moves
         *  with mechs (MM option) and ProtosMustBeGroups (Operations option).]
         *
         * MaxTotalAttackerTonnage - max total tonnage for attacking force - AFC=211
         * MaxTotalDefenderTonnage - max total tonnage for defending force - DFC=411
         *
         * MinTotalAttackerTonnage - min total tonnage for attacking force - AFC=212
         * MinTotalDefenderTonnage - min total tonnage for defending force - DFC=412
         *
         * AttackerAllowedMeks - boolean, whether or not attacker can use meks - AFC=207
         * AttackerAllowedVehs - boolean, whether or not attacker can use vehs - AFC=208
         * AttackerAllowedInf  - boolean, whether or not attacker can use inf  - AFC=209
         * AttackerAllowedAeros - boolean, whether or not attacker can use inf  - AFC=256
         * AttackerOmniMeksOnly- boolean, whether or not attacker must use omnimeks only - AFC=243
         *
         * DefenderAllowedMeks - boolean, whether or not defender can use meks - DFC=407
         * DefenderAllowedVehs - boolean, whether or not defender can use vehs - DFC=408
         * DefenderAllowedInf  - boolean, whether or not defender can use inf  - DFC=409
         * DefenderAllowedAeros - boolean, whether or not defender can use inf  - DFC=457
         * DefenderOmniMeksOnly- boolean, whether or not defender must use omnimeksonly - DFC=443
         *
         * ProtosMustbeGrouped - boolean. If enabled, protos must be present in multiples
         *                       of server wide move group. For example, if the server is
         *                       set to have protos move in groupd of 5, and someone has 8
         *                       protos, attacks will fail if ProtosMustbeGrouped is true.
         *                       CFC=001
         * RepodOmniUnitsToBase - string. Omni's that where used in this op are repodded back to base configuration
         *                        Leave blank to disable this option
         *
         * MULArmiesOnly        - Players do not need armies to activate or attack/defend. All armies will be created
         *                        via MUL files.
         *
         * AttackerPoweredInfAllowed  - boolean. Overrides AttackerAllowedInf and admits BA/Protos. - AFC=210
         * DefenderPoweredInfAllowed  - boolean. Overrides DefenderAllowedInf and admits BA/Protos. - DFC=410
         * AttackerStandardInfAllowed - boolean. Overrides AttackerAllowedInf and admits conventional units - AFC=246
         * DefenderStandardInfAllowed - boolean. Overrides DefenderAllowedInf and admits conventional units - DFC=446
         *
         * [NOTE: Allowances are only checked if infantry are banned, generally. Set Attacker/DefenderAllowedInfantry
         *  to false first, then enable PoweredInfAllowed or StandardInfAllowed. These values are ignored entirely if
         *  attacker/defender has "allows" set to true.]
         *
         * [Construction done]++
         *
         * [Costs]
         * -> Read in ShortValidator.checkAttackerCosts/checkDefenderCosts
         * -> Also read on OperationManager.terminate()
         *
         * AttackerCostMoney     - monetary charge to attacker  - AFC=213
         * AttackerCostInfluence - influence charge to attacker - AFC=214
         * AttackerCostReward    - reward charge to attacker    - AFC=215
         *
         * DefenderCostMoney     - charge, in money, for defender to JOIN game     - DFC=413
         * DefenderCostInfluence - charge, in influence, for defender to JOIN game - DFC=414
         * DefenderCostReward    - charge, in reward points, for defender to join  - DFC=415
         *
         * [CAUTION: Using defender costs in normal operations is STRONGLY
         *  discouraged. These are best used as overloads from ModifyingOps]
         *
         * [Costs done]
         *
         * [Player properties - XP, Rating, etc]
         * -> Read in ShortValidator.checkAttackerMilestones/checkDefenderMilestones
         *
         * MaxAttackerRating - self evident - AFC=216
         * MinAttackerRating - self evident - AFC=217
         * MaxDefenderRating - self evident - DFC=416
         * MinDefenderRating - self evident - DFC=417
         *
         * MaxAttackerXP - self evident - AFC=218
         * MinAttackerXP - self evident - AFC=219
         * MaxDefenderXP - self evident - DFC=418
         * MinDefenderXP - self evident - DFC=419
         *
         * MaxAttackerGamesPlayed - self evident - AFC=220
         * MinAttackerGamesPlayed - self evident - AFC=221
         * MaxDefenderGamesPlayed - self evident - DFC=420
         * MinDefenderGamesPlayed - self evident - DFC=421
         *
         * [NOTE: Generally speaking, games played shouldnt be mixed w/ XP. Use
         *  either, avoid setting both fields in any given operation.]
         *
         * [Players done]
         *
         * [Scenario values - artillery, etc]
         * -> Read in ShortOperation's changeStatus() method
         *
         * BotControlsAll                - [If true any support units giving to the task
         *                                 [are given to a bot to be controlled by instead.
         *                                 [this includes arty gun emplacements and mines
         *                                 [all players will be given a bot and any support
         *                                 [that would have gone to that player will goto the bot
         *                                 [instead. NOTE: the bots will fire upon all players
         *                                 [and other bots.
         *
         * BotsAllOnSameTeam             - [If set all bots will be added to the same team
         *                                 [This way all bots will attack the players and not
         *                                 [other bots. Bots unite!
         *
         * DefenderReceivesAutoArtillery - [If true, attack/defend players will receive
         * AttackerReceivesAutoArtillery   [normal autoartillery. Set false to make the
         *                                 [grant lopsided.
         *
         * DefenderFlatArtilleryModifier - Amount of BV to shift defender
         * AttackerFlatArtilleryModifier - Amount of BV to shift attacker
         * DefenderPercentArtilleryModifier - % adjustment to relative assignment BV
         * AttackerPercentArtilleryModifier - % adjustment to relative assignment BV
         *
         * MinDefenderArtilleryBV - [Min BV to be presumed for attack/defender player
         * MinAttackerArtilleryBV   [when assigning autoartillery. Floor for modifiers.
         * MaxDefenderArtilleryBV - [Max BV to be presumed for attack/def player when
         * MaxAttackerArtilleryBV   [assigning artillery. Ceiling for modifiers.
         *
         * [NOTES (Artillery): These variables are simpler than they may seem. First,
         *  the true/false Receives variales are used to allow one party to have arty
         *  while denying it to annother.
         *
         *  % and flat modifiers can be used simultaneously. The % modifier is applied
         *  to the player's combined BV first, then the flat modifier is added. Finally,
         *  the ceilings and floors are checked.
         *
         *  Depending on ceiling and floor settings, and server-configured caps on the
         *  number of artillery pieces to be assigned, its possible for both players to
         *  have the same amount of artillery, even when the modifiers and caps are
         *  weighted in favour of the defender.]
         *
         * [Gun Emplacements]
         *
         * DefenderReceivesGunEmplacements - [If true, attack/defend players will receive
         * AttackerReceivesGunEmplacement  - [normal gun emplacement. Set false to make the
         *                                   [grant lopsided.
         *
         * DefenderFlatGunEmplacementModifier - Amount of BV to shift defender
         * AttackerFlatGunEmplacementModifier - Amount of BV to shift attacker
         * DefenderPercentGunEmplacementModifier - % adjustment to relative assignment BV
         * AttackerPercentGunEmplacementModifier - % adjustment to relative assignment BV
         *
         * MinDefenderGunEmplacementBV - [Min BV to be presumed for attack/defender player
         * MinAttackerGunEmplacementBV   [when assigning gun emplacement. Floor for modifiers.
         * MaxDefenderGunEmplacementBV - [Max BV to be presumed for attack/def player when
         * MaxAttackerGunEmplacementBV   [assigning gun emplacement. Ceiling for modifiers.
         *
         * [NOTES (Gun Emplacement): These variables are simpler than they may seem. First,
         *  the true/false Receives variales are used to allow one party to have guns
         *  while denying it to another.
         *
         *  % and flat modifiers can be used simultaneously. The % modifier is applied
         *  to the player's combined BV first, then the flat modifier is added. Finally,
         *  the ceilings and floors are checked.
         *
         *  Depending on ceiling and floor settings, and server-configured caps on the
         *  number of gun emplacement pieces to be assigned, its possible for both players to
         *  have the same amount of gun emplacement, even when the modifiers and caps are
         *  weighted in favour of the defender.]
         *
         * [Mines]
         * DefenderReceivesMines       [Allow attacker or defender to recieve mines
         * AttackerReceivesMines
         *
         * DefenderBVPerConventional   [Set the BV amount for 1 conventional mine i.e set to 100
         * AttackerBVPerConventional   [and the total of both armies bv is 10k you get 100 mines
         * DefenderTonPerConventional  [Set the Ton amount for 1 conventional mine i.e set to 100
         * AttackerTonPerConventional  [and the total of both armies ton is 500 you get 5 mines
         *
         * DefenderBVPerVibra   [Set the BV amount for 1 vibra mine i.e set to 100
         * AttackerBVPerVibra   [and the total of both armies bv is 10k you get 100 mines
         * DefenderTonPerVibra  [Set the Ton amount for 1 vibra mine i.e set to 100
         * AttackerTonPerVibra  [and the total of both armies ton is 500 you get 5 mines
         *
         * [MUL Armies]
         * DefenderReceivesMULArmy      [Allow attacker or defender to recieve a full army
         * AttackerReceivesMULArmy
         *
         * MinDefenderMulArmies			[Min number of MUL armies a Defender can receive
         * MaxDefenderMulArmies			[Max number of MUL armies a Defender can receive
         *                              [These files exist in the servers data\armies folder]
         * DefenderMulArmyList			[List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulMeks           [Min number of MUL armies a Defender can receive
         * MaxDefenderMulMeks           [Max number of MUL armies a Defender can receive
         * DefenderMulMekList           [List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulVehicles       [Min number of MUL armies a Defender can receive
         * MaxDefenderMulVehicles       [Max number of MUL armies a Defender can receive
         * DefenderMulVehicleList       [List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulInf            [Min number of MUL armies a Defender can receive
         * MaxDefenderMulInf            [Max number of MUL armies a Defender can receive
         * DefenderMulInfList           [List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulBA             [Min number of MUL armies a Defender can receive
         * MaxDefenderMulBA             [Max number of MUL armies a Defender can receive
         * DefenderMulBAList            [List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulAero           [Min number of MUL armies a Defender can receive
         * MaxDefenderMulAero           [Max number of MUL armies a Defender can receive
         * DefenderMulAeroList          [List of the MUL files to choose from separated by ;
         *
         * MinDefenderMulProto          [Min number of MUL armies a Defender can receive
         * MaxDefenderMulProto          [Max number of MUL armies a Defender can receive
         * DefenderMulProtoList         [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulArmies			[Min number of MUL armies an Attacker can receive
         * MaxAttackerMulArmies			[Max number of MUL armies an Attacker can receive
         *                              [These files exist in the servers data\armies folder]
         * AttackerMulArmyList			[List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulMeks           [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulMeks           [Max number of MUL armies an Attacker can receive
         * AttackerMulMekList           [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulVehicles       [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulVehicles       [Max number of MUL armies an Attacker can receive
         * AttackerMulVehicleList       [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulInf            [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulInf            [Max number of MUL armies an Attacker can receive
         * AttackerMulInfList           [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulBA             [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulBA             [Max number of MUL armies an Attacker can receive
         * AttackerMulBAList            [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulAero           [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulAero           [Max number of MUL armies an Attacker can receive
         * AttackerMulAeroList          [List of the MUL files to choose from separated by ;
         *
         * MinAttackerMulProto          [Min number of MUL armies an Attacker can receive
         * MaxAttackerMulProto          [Max number of MUL armies an Attacker can receive
         * AttackerMulProtoList         [List of the MUL files to choose from separated by ;
         *
         * [Scenario props. finished]
         *
         * [player result params - pay, etc]
         *
         * BaseAttackerPayCBills - flat amount attacker is paid for participating;
         * BaseDefenderPayCBills - flat amount defender is paid for participating;
         * BaseAttackerPayInfluence - as above, but in Flu;
         * BaseDefenderPayInfluence - as above, but in Flu;
         * BaseAttackerPayExperience - as above, but in Flu;
         * BaseDefenderPayExperience - as above, but in Flu;
         *
         * AttackerPayBVforCBill - [1 CBill added to Base pay for every complete;
         * DefenderPayBVforCBill - [increment. ie - if 2, will add BV/2 Cbills to pay;
         *
         * AttackerWinModifierCBillsFlat - flat boost to pay for winning attack;
         * DefenderWinModifierCBillsFlat - flat boost to pay for winning defense;
         * AttackerLossModifierCBillsFlat - flat drop in pay for losing attack;
         * DefenderLossModifierCBillsFlat - flat drop to pay for losing defense;
         *
         * AttackerWinModifierCBillsPercent  - [double multipliers. the base amounts + bv adjustments +
         * DefenderWinModifierCBillsPercent  - [flat adjustments are multiplied by these numbers. A Win
         * AttackerLossModifierCBillsPercent - [Modifier of 100 would double pay. A LossModifier of 50
         * DefenderLossModifierCBillsPercent - [would reduce payments by half.
         *
         * AttackerPayBVforInfluence - as above, but for influence;
         * DefenderPayBVforInfluence - ditto;
         *
         * AttackerWinModifierInfluenceFlat - as above, but for influence;
         * DefenderWinModifierInfluenceFlat - ditto;
         * AttackerLossModifierInfluenceFlat - ditto;
         * DefenderLossModifierInfluenceFlat - ditto;
         *
         * AttackerWinModifierInfluencePercent - as above, but for influence;
         * DefenderWinModifierInfluencePercent - ditto;
         * AttackerLossModifierInfluencePercent - ditto;
         * DefenderLossModifierInfluencePercent - ditto;
         *
         * AttackerPayBVforExperience - as above, but for experience;
         * DefenderPayBVforExperience - ditto;
         *
         * AttackerWinModifierExperienceFlat - as above, but for experience;
         * DefenderWinModifierExperienceFlat - ditto;
         * AttackerLossModifierExperienceFlat - ditto;
         * DefenderLossModifierExperienceFlat - ditto;
         *
         * AttackerWinModifierExperiencePercent - as above, but for experience;
         * DefenderWinModifierExperiencePercent - ditto;
         * AttackerLossModifierExperiencePercent - ditto;
         * DefenderLossModifierExperiencePercent - ditto;
         *
         * [NOTE: Be very cautious when using percent modifiers in conjunction with
         *        the flat adjustments and BV plus ups. Percent modifiers are applied
         *        after other changes and may result in funky funky pay.]
         *
         * RPForWinner - num RP to give winner;
         * RPForLoser - num RP to give Loser;
         * RPForDefender - num RP to give Defender(s);
         * RPForAttacker - num RP to give Attacker(s);
         * OnlyGiveRPtoWinners - boolean. use to restrict attack/def RP to winning players;
         *
         * MinBVDifferenceForFullPay  - This is the min BV difference between the start and end of hte fight
         *                            - That must be achieved for full payment
         * BVFailurePaymentModifier   - How much of the full payment players actually get if they do not meet the
         *                            - Min requirement.
         *
         * FledUnitSalvageChance         - Chance, out of 100, that a unit that flees the field is put into the salvage pool. Default 0
         * FledUnitScrappedChance		 - Chance, out of 100, that a unit that flees the field is scrapped. Default 0.
         * PushedUnitSalvageChance		 - Chance that a unit that is pushed off the field is put into the salvage pool. Default 0
         * PushedUnitScrappedChance		 - Chance, out of 100, that a unit that is pushed off the field is scrapped. Default 0.
         *
         * EnginedUnitsScrappedChance    - Chance, out of 100, that an engined unit is utterly destroyed while trying to be salvaged. Default 0
         * ForcedSalvageUnitsScrappedChance - Chance, out of 100, that a legged/gyroed unit is utterly destroyed while trying to be salvaged. Default 0
         *
         * [player result params finished]
         *
         * [Salvage props]
         * -> Read in ShortResolver.possibleSalvageFromReport()
         * -> and in ShortResolver.assembleSalvageStrings()
         *
         * WinnerAlwaysSalvagesOwnUnits - boolean, if true winner gets all his salvageables;
         * SupportUnitsAreSalvageable   - boolean, if true then support units can go into the salvage pool
         * DefenderAlwaysSalvagesOwnUnits - boolean, if true defender gets all his salvageables;
         * AttackerAlwaysSalvagesOwnUnits - boolean, if true attacker gets all his salvageables;
         *
         * BaseAttackerSalvagePercent - attacker starting salvage rate if he wins;
         * BaseDefenderSalvagePercent - defender starting salvage rate if he wins;
         *
         * AttackerSalvageAdjustment - [Adjustment after each salvage attempt. IE - if base;
         * DefenderSalvageAdjustment - [is 50, and adjust is 20, 2nd attempt will be 30 or 70;
         *
         * BVToBoostAttackerSalvageCost - [UnitBV/BVToBoost is the starting cost of salvage. If
         * BVToBoostDefenderSalvageCost - [set to 0, salvage will be *FREE*. IMPORTANT!
         *
         * AttackerSalvageCostModifier - [salvage cost multiplier. is a double. entry of .75 will
         * DefenderSalvageCostModifier - [reduct cost by 25%, entry of 2.00 will double the cost
         *                               [to salvage a unit.  If 0, salvage will be *FREE*
         *
         * NOTE: the modifiers are best used with special ops, increasing cost for attacks,
         *       lowering for certain kinds of special defenses, etc.
         *
         * [salvage props finished]
         *
         * [NEWBIE setup]
         *
         * AllowSOLToUse   - set true to allow a newbie to initiate
         * AllowAgainstSOL - set true to allow a player to launch this attack against SOL
         *
         * AllowNonConqToUse   - set false to forbid non-conquer players from using this attack
         * AllowAgainstNonConq - set false to forbid players from using this attack against a non-conq player
         *
         * SOLPilotsGainXP   - set false to prevent SOL player's units from gaining XP
         * HousePilotsGainXP - set false to prevent faction units from gaining XP
         *
         * SOLPilotsCheckLevelUp   - set false to bar SOL player's units from levelling after game
         * HousePilotsCheckLevelUp - set false to bar normal player's units from levelling after game
         *
         * PayAllAsWinners        - enable to play all players in a game involving a SOL as
         *                          game winners, regardless of outcome.
         * PayTechsForGame        - set false to turn off technician payments.
         * CountGameForRanking    - set to true in order to stop ELO changes.
         * CountGameForProduction - Double. Controls amount of production players generate
         *                          Set to 0 to disable production. Defaults to 1.0, the
         *                          same amount that a solitary army would generate.
         *
         * NoStatisticsMode  - if enabled, stats will not be kept in games involving SOLs.
         *                     Pilot/Unit kills will not be counted or published.
         * NoDestructionMode - if enabled, no units will be destroyed and no units will
         *                     be salvaged (essentially sets up a sim-match).
         * AllowInFaction		 - if enabled then players in the same faction will be able to attack each other
         *
         * NOTEs: for a training game, AllowSOLtoUse and AllowAgainsSOL should be
         *       set true simultaneously, otherwise player would be able to LAUNCH
         *       attack, but other SOLs would not be able to defend.
         *
         *       It is NOT recommended that PayAllAsWinners be used unless NoStats
         *       and NoDestruction are also enabled.
         *
         * [Newbie setup finished]
         *
         * [META setup - campaign outcomes]
         * -> Read in ShortResolver.assembleDescriptionStrings()
         *
         * NOTE: Meta outcomes are duplicated as LONG variables (LAttackerBaseConquest,
         *       for example). If a ShortOperation has a matching LongOperation, it will
         *       *NOT* have a meta impact, even if the variables below are configured.
         *
         * AttackerBaseConquestAmount		- Base % of a planet taken for winning attacker
         * AttackerConquestBVAdjustment		- Amount of BV needed for extra % point
         * AttackerConquestUnitAdjustment	- Number of units needed for extra % point
         *
         * DefenderBaseConquestAmount		- Base % of a planet taken for winning defender
         * DefenderConquestBVAdjustment		- Amount of BV needed for extra % point
         * DefenderConquestUnitAdjustment	- Number of units needed for extra % point
         *
         * NOTE: Adjustments are from COMBINED totals, not one player's. This prevents
         *       players for min/maxing % gains by coming in one BV over/under a threshold.
         *       This holds for the other meta-outcomes in this section as well.
         *
         * AttackerBaseDelayAmount		- Base miniticks delay caused by winning attacker
         * AttackerDelayBVAdjustment	- Amount of BV needed for extra minitick of delay
         * AttackerDelayUnitAdjustment	- Number of units needed for extra minitick of delay
         *
         * DefenderBaseDelayAmount		- Base miniticks REPAIR caused by winning defender
         * DefenderDelayBVAdjustment	- Amount of BV needed for extra minitick of REPAIR
         * DefenderDelayUnitAdjustment	- Number of units needed for extra minitick of REPAIR
         *
         * NOTE: Winning defenders can *ENHANCE* productivity. Factories which aren't
         *       refreshing will go negative, reducing future delays after production.
         *
         * AttackerBasePPAmount		- Base production points taken by winning attacker
         * AttackerPPBVAdjustment	- Amount of BV needed for extra batch of prodpoints
         * AttackerPPUnitAdjustment	- Number of units needed for extra batch of prodpoints
         *
         * DefenderBasePPAmount		- Base production points generated by winning defender
         * DefenderPPBVAdjustment	- Amount of BV needed for extra batch of prodpoints
         * DefenderPPUnitAdjustment	- Number of units needed for extra batch of prodpoints
         *
         * NOTE: as with delay raids, winning defenses can be configured to have a positive
         *       outcome. this is a shift from the task-system, which gave no option for
         *       defender gains in "raid" situations.
         *
         * AttackerBaseUnitsTaken		- base number of units taken by winning attacker
         * AttackerUnitsBVAdjustment	- BV to take an additonal unit
         * AttackerUnitsUnitAdjustment	- units to take an additional unit (confusing!)
         *
         * AttackerBaseFactoryUnitsTaken 		- base number of units taken from factories by winning attacker
         * AttackerFactoryUnitsBVAdjustment	    - BV to take an additonal unit
         * AttackerFactoryUnitsUnitAdjustment	- units to take an additional unit (confusing!)
         * AttackerUnitsTakenBeforeFightStarts  - The units are taken before the fight starts and the player must get them
         *                                        off the field
         * AttackerAllowAgainstUnclaimedLand    - If checked then this operation will be launched agianst a planet that has
         *                                        CP that is unclaimed and the Faction will automatically claim it.
         *
         * NOTE: No defender insta-production. Ops might want to consider using PP or
         *       delay payouts to defenders of unit-raid heavy attacks.
         *
         * AttackerTargetOpAdjustment	- adjustment for attacking winner
         * DefenderTargetOpAdjustment	- adjustment for defending winner
         *
         * NOTE: Op Adjustments are used to increase or decrease victory THRESHOLDS for
         *       targetted long-ops. Not recommended for individual games. Generlly, better
         *       for use as a long-op (w/ fewer games than target) set up as a counter-assault
         *       or spoling attack.
         *
         * ConquestAmountCap	- Max amount of % to take, regardless of BV/units involved.
         * DelayAmountCap		- Max amount of delay to apply, regardless of BV/units involved.
         * PPCapptureCap    	- Max # of PP to take/generate
         * UnitCaptureCap 		- Ceiling on number of units which may be raided
         *
         * NOTE: These caps apply to defenders as well as attackers, which can create situations
         *       in which attacker gets no benefit for additional BV while defender continues to
         * 		 gain, or vice versa (based on BV/Units needed for bonus, etc).
         *
         *
         * PPDestructionCap              - Max # of PP that can be destroyed by an Op
         * UnitDestructionCap            - Max # of units that can be destroyed by an Op.
         * BaseUnitsDestroyed            - base number of units destroyed by winning attacker
         * DestroyedUnitsBVAdjustment    - BV to Destroyed an additonal unit
         * DestroyedUnitsUnitAdjustment  - units to Destroyed an additional unit (confusing!)
         * BasePPDestroyed               - Base production points destroyed by winning attacker
         * DestroyedPPBVAdjustment       - Amount of BV needed to destroy extra prodpoints
         * DestroyedPPUnitAdjustment     - Number of units needed to destroy extra prodpoints
         *
         * NOTE: All Destruction parameters are only used by the Attacker and go into effect when the attacker wins.
         * [Meta setup finished]
         *
         * [Chicken/Non-Defender setup]
         * -> Read in OpsChickenThread class
         *
         * TimeToNondefensePenalty  - time, in SECONDS, from attack init to penalty.
         * LeechesToDeactivate 		- num leeches before a player is moved into reserve.
         *
         * FlatRPChickenPenalty				- # of RP to take. double.
         * FlatExpChickenPenalty			- # of EXP to take. double.
         * FlatInfluenceChickenPenalty		- # of Inf to take. double.
         * FlatCBillChickenPenalty			- # of CBills to take. double.
         *
         * PercentRPChickenPenalty			- % of RP to take. int.
         * PercentExpChickenPenalty			- % of EXP to take. int.
         * PercentInfluenceChickenPenalty	- % of Inf to take. int.
         * PercentCBillChickenPenalty		- % of CBills to take. int.
         *
         * NOTE: Penalties applied for EVERY leech, not only @ deactivation. Take this
         *       into consideration if allowing >1 leech before deactivation.
         *
         * ConquestPerLeech 	- % control transferred on each leech
         * DelayPerLeech		- delay to on-target facilities w/ each leech
         * ProdPointsPerLeech	- PP taken with each leech
         * UnitsPerLeech		- Units taken with each leech
         * FailurePenalty		- [PP taken if no other penalty is applied. Use it
         *                        [to ensure that a penalty is always given, even
         *                        [if there is no % to yeild or factory to delay.
         *
         * [Chicken setup finished]
         *
         * [Pilot XP setup]
         * -> Read in ShortResolver.assembleSalvageStrings()
         *
         * BaseUnitXP            - XP earned by all surviving units for playing the game
         * UnitXPUnitsAdjustment - [bonus XP given to all surviving units, based on the starting size of the game
         *                         [if there are 10 units in game, and Adjust is set to 10, all units will get +1
         *                         [XP. if there are 10 in game and Adjust is 5, all units will get +2 XP, etc.
         * UnitXPBVAdjustment    - [bonus XP given to all surviving units, based on the starting size of the game. if
         *                         [total BV in game is 30,000 and Adjust is 10,000, all units will get +3 XP.
         * WinnerBonusUnitXP     - bonux XP given to all surviving units owned by a winning player
         * DefenderBonusUnitXP   - bonus XP given to all surviving units owned by a defending player
         * KillBonusUnitXP       - [flat amount of XP given to a unit for each kill it earned. this is granted only
         *                         [to the killing unit, and only if it survives or is salvaged by original owner
         *                         [with original pilot.
         * KillBonusXPforBV      - [KilledUnitsBV/KillBonusXPforBV. bonus XP based on the BV of units killed. added
         *                         [only to the killing unit, and only if the unit survives or is salvaged by its
         *                         [original owner with original pilot.
         *
         * [Pilot XP setup finished]
         *
         * [Buildings setup]
         * This is setup to allow buildings to be both placed on the map and also count towards the operation rewards
         *
         * TotalBuildings        - Total number of buildings to place on the map
         * MinBuildingsForOp     - [Minimum number of buildings the Attack needs to destroy for the op to be a sucess
         *                         [I.E. 10 buildings 5 min. if the attacker kills only 4 they don't get any of the bonuses.
         * MinFloors             - Minimum number of floors in each building.
         * MaxFloors             - Maximum number of floors in each building.
         * MinCF                 - Minimum Construction Factor each building can have (How many points of damage it'll take)
         * MaxCF                 - Maximum Construction Factor each building can have (How many points of damage it'll take)
         * BuildingType          - 1: Light, 2: Medium, 3: Heavy, 4: Hardend
         * BuildingsStartOnMapEdge - [defaults to true. if selected a random edge is select and the defender is set to that
         *                           [ and the attacker to the opposite edge. If not selected the buildings will be placed
         *                           [ randomly around the map.
         *
         * DelayPerBuilding      - Number of Delay ticks set to the planet factories for each building destroyed
         *
         * AttackerMoneyPerBuilding - Amount of Money the attacker gets for each building destroyed, if they meet the min amount of buildings destroyed.
         * AttackerExpPerBuilding - Amount of Exp the attacker gets for each building destroyed, if they meet the min amount of buildings destroyed.
         * AttackerFluPerBuilding - Amount of Flu the attacker gets for each building destroyed, if they meet the min amount of buildings destroyed.
         * AttackerRPPerBuilding  - Amount of RP the attacker gets for each building destroyed, if they meet the min amount of buildings destroyed.
         *
         * DefenderMoneyPerBuilding - Amount of Money the defender gets for each building left standing.
         * DefenderExpPerBuilding - Amount of Exp the defender gets for each building left standing.
         * DefenderFluPerBuilding - Amount of Flu the defender gets for each building left standing.
         * DefenderRPPerBuilding  - Amount of RP the defender gets for each building left standing.
         *
         * [Building Setup End]
         *
         * [Faction Exclusivity setup]
         *
         * LegalAttackFactions   - self evident - AFC=234
         * IllegalAttackFactions - self evident - AFC=234 (Note: shared message)
         *
         * LegalDefendFactions   - self evident - DFC=434
         * IllegalDefendFactions - self evident - DFC=434 (Note: shared message)
         *
         * [NOTE: If "Legal" is set, "Illegal" is not checked. This allows operators to easily
         *       create games that a small set of factions may use (via legal) or a small group
         *       of factions may not use (via illegal). Both are CASE SENSITIVE and $ DELIMITED.
         *
         *       If both Legal and Illegal are left blank and/or contain only spaces, all facs
         *       will be able to attack&defend the type. This is the default behaviour.
         *
         *       Would work:     Liao$Davion$Steiner
         *       Would NOT work: Liao,Davion,Steiner
         *       Would NOT work: liao$davion$steiner]
         *
         * [End Faction Exclusivity setup]
         *
         * [Begin City Generation]
         *
         * [NOTE: This is used to generate a city for a this op using MM's city
         *        Generation code. Currently ops have been using the Building
         *        code to genereate cities and it is not pretty.]
         *
         * RGCUseCityGenerator      - Use the city generator for this Op.
         * RGCCityType              - City type are Metro/Grid/Hub
         * RGCCityBlocks            - Number of city blocks this effects how many roads are created.
         * RGCRGCMinCF              - Min CF any building can be.
         * RGCMaxCF                 - Max CF any building can be. buildings will be between min and max
         *                            This buildings Type will be selected based on the CF generated. L
         *                            Light/Medium/Heavy/Hardened
         * RGCMinFloors             - Min number of floors any one building can have
         * RGCMaxFloors             - Max number of floors any one building can have. Builds height will be generated
         *                            from a number between Max and Min Floors
         * RGCCityDensity           - 1-100 This is the % chance that a hex will contain a building the higher the number
         *                            the more buildings will be generated.
         *
         * [End City Generation]
         *
         * [Begin Multi Player Variables]
         *
         * FreeForAllOperation      - Fun opration Unlimited amount of players can join on their own team
         *                            The op starts when the attack chooses or when the leech time ends.
         * MinNumberOfPlayers       - Minimum number of players needed for the FFA to launch.
         *
         * TeamOperation		    - Determins if this Operation should be setup for teams
         *
         * NumberOfTeams			- The number of teams for  this operation, min 2.
         *
         * TeamSize                 - The maximum number of people allowed on each team.
         *
         * TeamsMustBeSameFaction   - Boolean if true all team members must be on the same faction
         *                            defaults to false.
         *
         * RandomTeamDetermination  - Players are placed on teams randomly.
         *
         * [End Multi Player Variables]
         *
         * [Begin Victory Conditions]
         *
         * NumberOfVictoryConditions - The number of victory conditions that must be achieved by the winning team.
         *                           - Default 0 (off)
         * UseDestroyEnemyBV         - If this is turned on DestroyEnemyBV field well be sent to MM
         * DestroyEnemyBV            - Destroy/damage a certain percentage of the enemy force to win, measured by current BV / original BV
         * UseBVRatioPercent         - If this is turned on UseBVRatioPercent field will be sent to MM.
         * BVRatioPercent            - Friendly forces outnumber enemy forces by a percentage ratio.  Measured by current BV. E.G. 300 means you have 3x the surviving BV of the enemy.
         * UseUnitCommander			 - If this is turned on the UseUnitCommand field will be sent to MM.
         * MinimumUnitCommanders     - Minimum Number Of unit commanders players must have to launch/defend this op
         * MaximumUnitCommanders     - Maximum Number of unit commanders players can have to launch/defend this op
         *
         * [End Victory Conditions]
         *
         * [Begin Deployment]
         * RandomDeployment            - Operation picks the map edge for the attackers and defenders
         * DeployNorthwest				- Chance to deploy Northwest
         * DeployNorth					- Chance to deploy North
         * DeployNortheast				- Chance to deploy Northeast
         * DeployEast					- Chance to deploy East
         * DeploySoutheast				- Chance to deploy SouthEast
         * DeploySouth					- Chance to deploy South
         * DeploySouthwest				- Chance to deploy Southwest
         * DeployWest					- Chance to deploy West
         * DeployEdge					- Chance to deploy on the Edge
         * DeployCenter					- Chance to deploy in the Center
         * DeployNorthwestdeep			- Chance to deploy Northwest (Deep)
         * DeployNorthdeep				- Chance to deploy North (Deep)
         * DeployNortheastdeep			- Chance to deploy Northeast (Deep)
         * DeployEastdeep				- Chance to deploy East (Deep)
         * DeploySoutheastdeep			- Chance to deploy Southeast (Deep)
         * DeploySouthdeep				- Chance to deploy South (Deep)
         * DeploySouthwestdeep			- Chance to deploy Southwest (Deep)
         * DeployWestdeep				- Chance to deploy West (Deep)
         *
         * [End Deployment
         *
         * [Map Settings
         *
         * UseOperationMap				- Use the Operation map instead of the terrain on the planet
         * MapName						- Name of the map to use or surprise/generated if using sizes
         * BoardSizeX					- Number of maps along the X axis of the Board
         * BoardSizeY					- Number of maps along the Y axis of the board
         * MapSizeX						- X Size of the map
         * MapSizeY						- Y Size of the map
         * MaxMedium                    - "Ground", "Atmosphere", "Space"
         *
         * [End Map Settings
         */

        // army contruction
        operationsDefaults.put("MaxAttackerBV", "10000000");
        operationsDefaults.put("MinAttackerBV", "0");
        operationsDefaults.put("MaxDefenderBV", "10000000");
        operationsDefaults.put("MinDefenderBV", "0");

        operationsDefaults.put("MaxAttackerMeks", "10000000");
        operationsDefaults.put("MinAttackerMeks", "1");// must have at least one unit
        operationsDefaults.put("MaxDefenderMeks", "10000000");
        operationsDefaults.put("MinDefenderMeks", "1");// must have at least one unit

        operationsDefaults.put("MaxAttackerVehicles", "10000000");
        operationsDefaults.put("MinAttackerVehicles", "0");
        operationsDefaults.put("MaxDefenderVehicles", "10000000");
        operationsDefaults.put("MinDefenderVehicles", "0");

        operationsDefaults.put("MaxAttackerInfantry", "10000000");
        operationsDefaults.put("MinAttackerInfantry", "0");
        operationsDefaults.put("MaxDefenderInfantry", "10000000");
        operationsDefaults.put("MinDefenderInfantry", "0");

        operationsDefaults.put("MaxAttackerAero", "0");
        operationsDefaults.put("MinAttackerAero", "0");
        operationsDefaults.put("MaxDefenderAero", "0");
        operationsDefaults.put("MinDefenderAero", "0");
        operationsDefaults.put("EnforceAttackerAeroRatio", "false");
        operationsDefaults.put("EnforceDefenderAeroRatio", "false");
        operationsDefaults.put("CountSupportUnitsInAeroRatio", "true");
        operationsDefaults.put("MaxDefenderAeroPercent", "100");
        operationsDefaults.put("MinDefenderAeroPercent", "0");
        operationsDefaults.put("MaxAttackerAeroPercent", "100");
        operationsDefaults.put("MinAttackerAeroPercent", "0");

        operationsDefaults.put("MaxAttackerNonInfantry", "10000000");
        operationsDefaults.put("MinAttackerNonInfantry", "0");
        operationsDefaults.put("MaxDefenderNonInfantry", "10000000");
        operationsDefaults.put("MinDefenderNonInfantry", "0");

        operationsDefaults.put("MinAttackerWalk", "0");
        operationsDefaults.put("MinDefenderWalk", "0");

        operationsDefaults.put("MinAttackerJump", "0");
        operationsDefaults.put("MinDefenderJump", "0");
        operationsDefaults.put("MaxAttackerJump", "99");
        operationsDefaults.put("MaxDefenderJump", "99");

        operationsDefaults.put("MaxAttackerUnitTonnage", "100");
        operationsDefaults.put("MaxDefenderUnitTonnage", "100");

        operationsDefaults.put("MinAttackerUnitTonnage", "0");
        operationsDefaults.put("MinDefenderUnitTonnage", "0");

        operationsDefaults.put("MaxAttackerUnitBV", "10000000");
        operationsDefaults.put("MaxDefenderUnitBV", "10000000");

        operationsDefaults.put("MinAttackerUnitBV", "0");
        operationsDefaults.put("MinDefenderUnitBV", "0");

        operationsDefaults.put("MaxAttackerUnitBVSpread", "10000000");
        operationsDefaults.put("MaxDefenderUnitBVSpread", "10000000");
        operationsDefaults.put("MinAttackerUnitBVSpread", "0");
        operationsDefaults.put("MinDefenderUnitBVSpread", "0");
        operationsDefaults.put("AttackerUsePercentageBVSpread", "false");
        operationsDefaults.put("DefenderUsePercentageBVSpread", "false");
        operationsDefaults.put("AttackerBVSpreadPercent", "0");
        operationsDefaults.put("DefenderBVSpreadPercent", "0");

        operationsDefaults.put("HighestAttackerPilotSkillTotal", "20");
        operationsDefaults.put("LowestAttackerPilotSkillTotal", "0");
        operationsDefaults.put("HighestAttackerPiloting", "9");
        operationsDefaults.put("LowestAttackerPiloting", "0");
        operationsDefaults.put("HighestAttackerGunnery", "9");
        operationsDefaults.put("LowestAttackerGunnery", "0");
        operationsDefaults.put("AttackerAverageArmySkillMax", "20");
        operationsDefaults.put("AttackerAverageArmySkillMin", "0");

        operationsDefaults.put("HighestDefenderPilotSkillTotal", "20");
        operationsDefaults.put("LowestDefenderPilotSkillTotal", "0");
        operationsDefaults.put("HighestDefenderPiloting", "9");
        operationsDefaults.put("LowestDefenderPiloting", "0");
        operationsDefaults.put("HighestDefenderGunnery", "9");
        operationsDefaults.put("LowestDefenderGunnery", "0");
        operationsDefaults.put("DefenderAverageArmySkillMax", "20");
        operationsDefaults.put("DefenderAverageArmySkillMin", "0");

        operationsDefaults.put("CountVehsForSpread", "true");
        operationsDefaults.put("CountProtosForSpread", "true");
        operationsDefaults.put("CountInfForSpread", "true");
        operationsDefaults.put("CountAerosForSpread", "true");

        operationsDefaults.put("ProtosMustbeGrouped", "true");
        operationsDefaults.put("MULArmiesOnly", "false");

        operationsDefaults.put("RepodOmniUnitsToBase", "");

        operationsDefaults.put("MaxTotalAttackerTonnage", "10000000");
        operationsDefaults.put("MaxTotalDefenderTonnage", "10000000");

        operationsDefaults.put("MinTotalAttackerTonnage", "0");
        operationsDefaults.put("MinTotalDefenderTonnage", "0");

        operationsDefaults.put("AttackerCostMoney", "0");
        operationsDefaults.put("AttackerCostInfluence", "0");
        operationsDefaults.put("AttackerCostReward", "0");

        operationsDefaults.put("DefenderCostMoney", "0");
        operationsDefaults.put("DefenderCostInfluence", "0");
        operationsDefaults.put("DefenderCostReward", "0");

        operationsDefaults.put("AttackerAllowedMeks", "true");
        operationsDefaults.put("AttackerAllowedVehs", "true");
        operationsDefaults.put("AttackerAllowedInf", "true");
        operationsDefaults.put("AttackerAllowedAeros", "false");
        operationsDefaults.put("AttackerOmniMeksOnly", "false");

        operationsDefaults.put("DefenderAllowedMeks", "true");
        operationsDefaults.put("DefenderAllowedVehs", "true");
        operationsDefaults.put("DefenderAllowedInf", "true");
        operationsDefaults.put("DefenderAllowedAeros", "false");
        operationsDefaults.put("DefenderOmniMeksOnly", "false");

        operationsDefaults.put("AttackerPoweredInfAllowed", "false");
        operationsDefaults.put("DefenderPoweredInfAllowed", "false");
        operationsDefaults.put("AttackerStandardInfAllowed", "false");
        operationsDefaults.put("DefenderStandardInfAllowed", "false");
        // end army construction

        // player values
        operationsDefaults.put("MinAttackerRating", "0");
        operationsDefaults.put("MaxAttackerRating", "10000000");
        operationsDefaults.put("MinDefenderRating", "0");
        operationsDefaults.put("MaxDefenderRating", "10000000");

        operationsDefaults.put("MinAttackerXP", "0");
        operationsDefaults.put("MaxAttackerXP", "10000000");
        operationsDefaults.put("MinDefenderXP", "0");
        operationsDefaults.put("MaxDefenderXP", "10000000");

        operationsDefaults.put("MinAttackerGamesPlayed", "0");
        operationsDefaults.put("MaxAttackerGamesPlayed", "10000000");
        operationsDefaults.put("MinDefenderGamesPlayed", "0");
        operationsDefaults.put("MaxDefenderGamesPlayed", "10000000");
        // end player values

        // scenario values [arty, etc]
        operationsDefaults.put("BotControlsAll", "false");
        operationsDefaults.put("BotsAllOnSameTeam", "false");

        operationsDefaults.put("DefenderReceivesAutoArtillery", "false");
        operationsDefaults.put("AttackerReceivesAutoArtillery", "false");

        operationsDefaults.put("DefenderFlatArtilleryModifier", "0");
        operationsDefaults.put("AttackerFlatArtilleryModifier", "0");

        operationsDefaults.put("DefenderPercentArtilleryModifier", "0");
        operationsDefaults.put("AttackerPercentArtilleryModifier", "0");

        operationsDefaults.put("MinDefenderArtilleryBV", "0");
        operationsDefaults.put("MinAttackerArtilleryBV", "0");

        operationsDefaults.put("MaxDefenderArtilleryBV", "10000000");
        operationsDefaults.put("MaxAttackerArtilleryBV", "10000000");

        // Gun Emplacements
        operationsDefaults.put("DefenderReceivesGunEmplacement", "false");
        operationsDefaults.put("AttackerReceivesGunEmplacement", "false");

        operationsDefaults.put("DefenderFlatGunEmplacementModifier", "0");
        operationsDefaults.put("AttackerFlatGunEmplacementModifier", "0");

        operationsDefaults.put("DefenderPercentGunEmplacementModifier", "0");
        operationsDefaults.put("AttackerPercentGunEmplacementModifier", "0");

        operationsDefaults.put("MinDefenderGunEmplacementBV", "0");
        operationsDefaults.put("MinAttackerGunEmplacementBV", "0");

        operationsDefaults.put("MaxDefenderGunEmplacementBV", "10000000");
        operationsDefaults.put("MaxAttackerGunEmplacementBV", "10000000");

        // Mines
        operationsDefaults.put("DefenderReceivesMines", "false");
        operationsDefaults.put("AttackerReceivesMines", "false");

        operationsDefaults.put("DefenderBVPerConventional", "0");
        operationsDefaults.put("AttackerBVPerConventional", "0");

        operationsDefaults.put("DefenderTonPerConventional", "0");
        operationsDefaults.put("AttackerTonPerConventional", "0");

        operationsDefaults.put("DefenderBVPerVibra", "0");
        operationsDefaults.put("AttackerBVPerVibra", "0");

        operationsDefaults.put("DefenderTonPerVibra", "0");
        operationsDefaults.put("AttackerTonPerVibra", "0");

        // MUL Armies
        operationsDefaults.put("DefenderReceivesMULArmy", "false");
        operationsDefaults.put("AttackerReceivesMULArmy", "false");

        operationsDefaults.put("MinDefenderMulArmies", "0");
        operationsDefaults.put("MaxDefenderMulArmies", "0");
        operationsDefaults.put("DefenderMulArmyList", "");

        operationsDefaults.put("MinDefenderMulMeks", "0");
        operationsDefaults.put("MaxDefenderMulMeks", "0");
        operationsDefaults.put("DefenderMulMekList", "");

        operationsDefaults.put("MinDefenderMulVehicles", "0");
        operationsDefaults.put("MaxDefenderMulVehicles", "0");
        operationsDefaults.put("DefenderMulVehicleList", "");

        operationsDefaults.put("MinDefenderMulInf", "0");
        operationsDefaults.put("MaxDefenderMulInf", "0");
        operationsDefaults.put("DefenderMulInfList", "");

        operationsDefaults.put("MinDefenderMulBA", "0");
        operationsDefaults.put("MaxDefenderMulBA", "0");
        operationsDefaults.put("DefenderMulBAList", "");

        operationsDefaults.put("MinDefenderMulAero", "0");
        operationsDefaults.put("MaxDefenderMulAero", "0");
        operationsDefaults.put("DefenderMulAeroList", "");

        operationsDefaults.put("MinDefenderMulProto", "0");
        operationsDefaults.put("MaxDefenderMulProto", "0");
        operationsDefaults.put("DefenderMulProtoList", "");

        operationsDefaults.put("MinAttackerMulArmies", "0");
        operationsDefaults.put("MaxAttackerMulArmies", "0");
        operationsDefaults.put("AttackerMulArmyList", "");

        operationsDefaults.put("MinAttackerMulMeks", "0");
        operationsDefaults.put("MaxAttackerMulMeks", "0");
        operationsDefaults.put("AttackerMulMekList", "");

        operationsDefaults.put("MinAttackerMulVehicles", "0");
        operationsDefaults.put("MaxAttackerMulVehicles", "0");
        operationsDefaults.put("AttackerMulVehicleList", "");

        operationsDefaults.put("MinAttackerMulInf", "0");
        operationsDefaults.put("MaxAttackerMulInf", "0");
        operationsDefaults.put("AttackerMulInfList", "");

        operationsDefaults.put("MinAttackerMulBA", "0");
        operationsDefaults.put("MaxAttackerMulBA", "0");
        operationsDefaults.put("AttackerMulBAList", "");

        operationsDefaults.put("MinAttackerMulAero", "0");
        operationsDefaults.put("MaxAttackerMulAero", "0");
        operationsDefaults.put("AttackerMulAeroList", "");

        operationsDefaults.put("MinAttackerMulProto", "0");
        operationsDefaults.put("MaxAttackerMulProto", "0");
        operationsDefaults.put("AttackerMulProtoList", "");

        // player outcome values [salvage, pay]
        operationsDefaults.put("BaseAttackerPayCBills", "0");
        operationsDefaults.put("BaseDefenderPayCBills", "0");

        operationsDefaults.put("BaseAttackerPayInfluence", "0");
        operationsDefaults.put("BaseDefenderPayInfluence", "0");

        operationsDefaults.put("BaseAttackerPayExperience", "0");
        operationsDefaults.put("BaseDefenderPayExperience", "0");

        operationsDefaults.put("AttackerPayBVforCBill", "0");
        operationsDefaults.put("DefenderPayBVforCBill", "0");

        operationsDefaults.put("AttackerPayBVforInfluence", "0");
        operationsDefaults.put("DefenderPayBVforInfluence", "0");

        operationsDefaults.put("AttackerPayBVforExperience", "0");
        operationsDefaults.put("DefenderPayBVforExperience", "0");

        operationsDefaults.put("AttackerPayBVforRP", "0");
        operationsDefaults.put("DefenderPayBVforRP", "0");

        operationsDefaults.put("AttackerWinModifierCBillsFlat", "0");
        operationsDefaults.put("DefenderWinModifierCBillsFlat", "0");
        operationsDefaults.put("AttackerLossModifierCBillsFlat", "0");
        operationsDefaults.put("DefenderLossModifierCBillsFlat", "0");

        operationsDefaults.put("AttackerWinModifierCBillsPercent", "0");
        operationsDefaults.put("DefenderWinModifierCBillsPercent", "0");
        operationsDefaults.put("AttackerLossModifierCBillsPercent", "0");
        operationsDefaults.put("DefenderLossModifierCBillsPercent", "0");

        operationsDefaults.put("AttackerWinModifierInfluenceFlat", "0");
        operationsDefaults.put("DefenderWinModifierInfluenceFlat", "0");
        operationsDefaults.put("AttackerLossModifierInfluenceFlat", "0");
        operationsDefaults.put("DefenderLossModifierInfluenceFlat", "0");

        operationsDefaults.put("AttackerWinModifierInfluencePercent", "0");
        operationsDefaults.put("DefenderWinModifierInfluencePercent", "0");
        operationsDefaults.put("AttackerLossModifierInfluencePercent", "0");
        operationsDefaults.put("DefenderLossModifierInfluencePercent", "0");

        operationsDefaults.put("AttackerWinModifierExperienceFlat", "0");
        operationsDefaults.put("DefenderWinModifierExperienceFlat", "0");
        operationsDefaults.put("AttackerLossModifierExperienceFlat", "0");
        operationsDefaults.put("DefenderLossModifierExperienceFlat", "0");

        operationsDefaults.put("AttackerWinModifierExperiencePercent", "0");
        operationsDefaults.put("DefenderWinModifierExperiencePercent", "0");
        operationsDefaults.put("AttackerLossModifierExperiencePercent", "0");
        operationsDefaults.put("DefenderLossModifierExperiencePercent", "0");

        operationsDefaults.put("RPForLoser", "0");
        operationsDefaults.put("RPForDefender", "0");
        operationsDefaults.put("RPForAttacker", "0");
        operationsDefaults.put("RPForWinner", "0");

        operationsDefaults.put("OnlyGiveRPtoWinners", "false");

        operationsDefaults.put("MinBVDifferenceForFullPay", "0");
        operationsDefaults.put("BVFailurePaymentModifier", "0");

        operationsDefaults.put("WinnerAlwaysSalvagesOwnUnits", "true");
        operationsDefaults.put("SupportUnitsAreSalvageable", "false");
        operationsDefaults.put("DefenderAlwaysSalvagesOwnUnits", "false");
        operationsDefaults.put("AttackerAlwaysSalvagesOwnUnits", "false");

        operationsDefaults.put("DestroyAllSalvage", "false"); //@salient

        operationsDefaults.put("BaseAttackerSalvagePercent", "50");
        operationsDefaults.put("BaseDefenderSalvagePercent", "50");

        operationsDefaults.put("AttackerSalvageAdjustment", "0");
        operationsDefaults.put("DefenderSalvageAdjustment", "0");

        operationsDefaults.put("BVToBoostAttackerSalvageCost", "150");
        operationsDefaults.put("BVToBoostDefenderSalvageCost", "150");

        operationsDefaults.put("AttackerSalvageCostModifier", "1");
        operationsDefaults.put("DefenderSalvageCostModifier", "1");

        operationsDefaults.put("FledUnitSalvageChance", "0");
        operationsDefaults.put("FledUnitScrappedChance", "0");
        operationsDefaults.put("PushedUnitSalvageChance", "0");
        operationsDefaults.put("PushedUnitScrappedChance", "0");

        operationsDefaults.put("EnginedUnitsScrappedChance", "0");
        operationsDefaults.put("ForcedSalvageUnitsScrappedChance", "0");
        operationsDefaults.put("GyroedUnitsScrappedChance", "0");
        operationsDefaults.put("LeggedUnitsScrappedChance", "0");
        operationsDefaults.put("UseSeparateLegAndGyroScrappedChance", "false");

        // newbie-match flags
        operationsDefaults.put("AllowSOLToUse", "false");
        operationsDefaults.put("AllowAgainstSOL", "false");

        operationsDefaults.put("AllowNonConqToUse", "true");
        operationsDefaults.put("AllowAgainstNonConq", "true");

        operationsDefaults.put("SOLPilotsGainXP", "true");
        operationsDefaults.put("HousePilotsGainXP", "true");
        operationsDefaults.put("SOLPilotsCheckLevelUp", "true");
        operationsDefaults.put("HousePilotsCheckLevelUp", "true");

        operationsDefaults.put("PayAllAsWinners", "false");
        operationsDefaults.put("PayTechsForGame", "true");
        operationsDefaults.put("CountGameForRanking", "true");
        operationsDefaults.put("CountGameForProduction", "1.0");
        operationsDefaults.put("NoStatisticsMode", "false");
        operationsDefaults.put("NoDestructionMode", "false");
        operationsDefaults.put("AllowInFaction", "false");

        // META outcome paramaters
        operationsDefaults.put("AttackerBaseConquestAmount", "0");
        operationsDefaults.put("AttackerConquestBVAdjustment", "0");
        operationsDefaults.put("AttackerConquestUnitAdjustment", "0");

        operationsDefaults.put("DefenderBaseConquestAmount", "0");
        operationsDefaults.put("DefenderConquestBVAdjustment", "0");
        operationsDefaults.put("DefenderConquestUnitAdjustment", "0");

        operationsDefaults.put("AttackerBaseDelayAmount", "0");
        operationsDefaults.put("AttackerDelayBVAdjustment", "0");
        operationsDefaults.put("AttackerDelayUnitAdjustment", "0");

        operationsDefaults.put("DefenderBaseDelayAmount", "0");
        operationsDefaults.put("DefenderDelayBVAdjustment", "0");
        operationsDefaults.put("DefenderDelayUnitAdjustment", "0");

        operationsDefaults.put("AttackerBasePPAmount", "0");
        operationsDefaults.put("AttackerPPBVAdjustment", "0");
        operationsDefaults.put("AttackerPPUnitAdjustment", "0");

        operationsDefaults.put("DefenderBasePPAmount", "0");
        operationsDefaults.put("DefenderPPBVAdjustment", "0");
        operationsDefaults.put("DefenderPPUnitAdjustment", "0");

        operationsDefaults.put("AttackerBaseUnitsTaken", "0");
        operationsDefaults.put("AttackerUnitsBVAdjustment", "0");
        operationsDefaults.put("AttackerUnitsUnitAdjustment", "0");

        operationsDefaults.put("AttackerBaseFactoryUnitsTaken", "0");
        operationsDefaults.put("AttackerFactoryUnitsBVAdjustment", "0");
        operationsDefaults.put("AttackerFactoryUnitsUnitAdjustment", "0");
        operationsDefaults.put("AttackerAwardFactoryUnitsTakenToPlayerBVPercent", "0");
        operationsDefaults.put("AttackerAwardFactoryUnitsTakenToPlayerMaxBVPercent", "0");
        operationsDefaults.put("AttackerAwardFactoryUnitsTakenToPlayerMax", "0");
        operationsDefaults.put("AttackerUnitsTakenBeforeFightStarts", "false");
        operationsDefaults.put("AttackerAllowAgainstUnclaimedLand", "false");

        operationsDefaults.put("ForceProduceAndCaptureMeks", "true");
        operationsDefaults.put("ForceProduceAndCaptureVees", "true");
        operationsDefaults.put("ForceProduceAndCaptureInfs", "true");
        operationsDefaults.put("ForceProduceAndCaptureBAs", "true");
        operationsDefaults.put("ForceProduceAndCaptureAeros", "true");
        operationsDefaults.put("ForceProduceAndCaptureProtos", "true");

        operationsDefaults.put("AttackerTargetOpAdjustment", "0");
        operationsDefaults.put("DefenderTargetOpAdjustment", "0");

        operationsDefaults.put("ConquestAmountCap", "10000000");
        operationsDefaults.put("DelayAmountCap", "10000000");
        operationsDefaults.put("PPCapptureCap", "10000000");
        operationsDefaults.put("UnitCaptureCap", "10000000");

        operationsDefaults.put("PPDestructionCap", "10000000");
        operationsDefaults.put("UnitDestructionCap", "10000000");
        operationsDefaults.put("BaseUnitsDestroyed", "0");
        operationsDefaults.put("DestroyedUnitsBVAdjustment", "0");
        operationsDefaults.put("DestroyedUnitsUnitAdjustment", "0");
        operationsDefaults.put("BasePPDestroyed", "0");
        operationsDefaults.put("DestroyedPPBVAdjustment", "0");
        operationsDefaults.put("DestroyedPPUnitAdjustment", "0");

        // CHICKEN/NON-DEFENSE paramaters
        operationsDefaults.put("TimeToNondefensePenalty", "60");
        operationsDefaults.put("LeechesToDeactivate", "1");

        operationsDefaults.put("FlatRPChickenPenalty", "0");
        operationsDefaults.put("FlatExpChickenPenalty", "0");
        operationsDefaults.put("FlatInfluenceChickenPenalty", "0");
        operationsDefaults.put("FlatCBillChickenPenalty", "0");

        operationsDefaults.put("PercentRPChickenPenalty", "0");
        operationsDefaults.put("PercentExpChickenPenalty", "0");
        operationsDefaults.put("PercentInfluenceChickenPenalty", "0");
        operationsDefaults.put("PercentCBillChickenPenalty", "0");

        operationsDefaults.put("ConquestPerLeech", "0");
        operationsDefaults.put("DelayPerLeech", "0");
        operationsDefaults.put("ProdPointsPerLeech", "0");
        operationsDefaults.put("UnitsPerLeech", "0");
        operationsDefaults.put("FailurePenalty", "1000");

        // Pilot Levelling paramaters
        operationsDefaults.put("BaseUnitXP", "0");
        operationsDefaults.put("UnitXPUnitsAdjustment", "0");
        operationsDefaults.put("UnitXPBVAdjustment", "0");
        operationsDefaults.put("WinnerBonusUnitXP", "0");
        operationsDefaults.put("DefenderBonusUnitXP", "0");
        operationsDefaults.put("KillBonusUnitXP", "5");
        operationsDefaults.put("KillBonusXPforBV", "0");

        // Buildings
        operationsDefaults.put("TotalBuildings", "0");
        operationsDefaults.put("MinBuildingsForOp", "0");
        operationsDefaults.put("MinFloors", "0");
        operationsDefaults.put("MaxFloors", "0");
        operationsDefaults.put("MinCF", "0");
        operationsDefaults.put("MaxCF", "0");
        operationsDefaults.put("BuildingType", "1");
        operationsDefaults.put("BuildingsStartOnMapEdge", "true");
        operationsDefaults.put("DelayPerBuilding", "0");
        operationsDefaults.put("AttackerExpPerBuilding", "0");
        operationsDefaults.put("AttackerMoneyPerBuilding", "0");
        operationsDefaults.put("AttackerFluPerBuilding", "0");
        operationsDefaults.put("AttackerRPPerBuilding", "0");
        operationsDefaults.put("AttackerMinBuildingsIfAttackerWins", "0");
        operationsDefaults.put("DefenderExpPerBuilding", "0");
        operationsDefaults.put("DefenderMoneyPerBuilding", "0");
        operationsDefaults.put("DefenderFluPerBuilding", "0");
        operationsDefaults.put("DefenderRPPerBuilding", "0");

        // faction exclusions
        operationsDefaults.put("LegalAttackFactions", "");
        operationsDefaults.put("IllegalAttackFactions", "");
        operationsDefaults.put("LegalDefendFactions", "");
        operationsDefaults.put("IllegalDefendFactions", "");

        // City Generation
        operationsDefaults.put("RCGUseCityGenerator", "false");
        operationsDefaults.put("RCGCityType", "GRID");
        operationsDefaults.put("RCGCityBlocks", "4");
        operationsDefaults.put("RCGMinCF", "20");
        operationsDefaults.put("RCGMaxCF", "40");
        operationsDefaults.put("RCGMinFloors", "1");// must have at least one floor
        operationsDefaults.put("RCGMaxFloors", "3");
        operationsDefaults.put("RCGCityDensity", "50");

        // Muli Player Ops

        operationsDefaults.put("FreeForAllOperation", "false");
        operationsDefaults.put("MinNumberOfPlayers", "3");
        operationsDefaults.put("TeamOperation", "false");
        operationsDefaults.put("NumberOfTeams", "2");
        operationsDefaults.put("TeamSize", "1");
        operationsDefaults.put("TeamsMustBeSameFaction", "false");
        operationsDefaults.put("RandomTeamDetermination", "false");

        // Victory Conditions
        operationsDefaults.put("NumberOfVictoryConditions", "0");
        operationsDefaults.put("UseDestroyEnemyBV", "false");
        operationsDefaults.put("DestroyEnemyBV", "100");
        operationsDefaults.put("UseBVRatioPercent", "false");
        operationsDefaults.put("BVRatioPercent", "300");
        operationsDefaults.put("UseUnitCommander", "false");
        operationsDefaults.put("MinimumUnitCommanders", "1");
        operationsDefaults.put("MaximumUnitCommanders", "1");
        operationsDefaults.put("UseGameTurnLimit", "false");
        operationsDefaults.put("GameTurnLimit", "0");
        operationsDefaults.put("UseKillCount", "false");
        operationsDefaults.put("KillCount", "0");

        // Deployment
        operationsDefaults.put("RandomDeployment", "false");
        operationsDefaults.put("DeployNorthwest", "1");
        operationsDefaults.put("DeployNorth", "1");
        operationsDefaults.put("DeployNortheast", "1");
        operationsDefaults.put("DeployEast", "1");
        operationsDefaults.put("DeploySoutheast", "1");
        operationsDefaults.put("DeploySouth", "1");
        operationsDefaults.put("DeploySouthwest", "1");
        operationsDefaults.put("DeployWest", "1");
        operationsDefaults.put("DeployEdge", "0");
        operationsDefaults.put("DeployCenter", "0");
        operationsDefaults.put("DeployNorthwestdeep", "0");
        operationsDefaults.put("DeployNorthdeep", "0");
        operationsDefaults.put("DeployNortheastdeep", "0");
        operationsDefaults.put("DeployEastdeep", "0");
        operationsDefaults.put("DeploySoutheastdeep", "0");
        operationsDefaults.put("DeploySouthdeep", "0");
        operationsDefaults.put("DeploySouthwestdeep", "0");
        operationsDefaults.put("DeployWestdeep", "0");

        // Map Settings
        operationsDefaults.put("UseOperationMap", "false");
        operationsDefaults.put("MapName", "generated");
        operationsDefaults.put("BoardSizeX", "0");
        operationsDefaults.put("BoardSizeY", "0");
        operationsDefaults.put("MapSizeX", "0");
        operationsDefaults.put("MapSizeY", "0");
        operationsDefaults.put("MapMedium", "0");

        /*
         * LONG VARIABLES. These params are necessary for operations
         * which span multiple games (eg - a 20 game Assault). LongOps
         * do not involve single-player payment, and player-specific
         * variables are ommitted from this block.
         *
         * [Meta paramaters]
         * - Many of these overlap w/ single game params. Can be used in
         *   combination; however, recommended approach is to use ONE meta
         *   payment appraoch for each individual operation.
         *
         * LAttackerBaseConquestAmount		- Base % of a planet taken for winning attacker
         * LAttackerConquestBVAdjustment	- Amount of BV needed for extra % point
         * LAttackerConquestUnitAdjustment	- Number of units needed for extra % point
         *
         * LDefenderBaseConquestAmount		- Base % of a planet taken for winning defender
         * LDefenderConquestBVAdjustment	- Amount of BV needed for extra % point
         * LDefenderConquestUnitAdjustment	- Number of units needed for extra % point
         *
         * LAttackerBaseDelayAmount		- Base miniticks delay caused by winning attacker
         * LAttackerDelayBVAdjustment	- Amount of BV needed for extra minitick of delay
         * LAttackerDelayUnitAdjustment	- Number of units needed for extra minitick of delay
         *
         * LDefenderBaseDelayAmount		- Base miniticks REPAIR caused by winning defender
         * LDefenderDelayBVAdjustment	- Amount of BV needed for extra minitick of REPAIR
         * LDefenderDelayUnitAdjustment	- Number of units needed for extra minitick of REPAIR
         *
         * LAttackerBasePPAmount		- Base production points taken by winning attacker
         * LAttackerPPBVAdjustment		- Amount of BV needed for extra batch of prodpoints
         * LAttackerPPUnitAdjustment	- Number of units needed for extra batch of prodpoints
         *
         * LDefenderBasePPAmount		- Base production points generated by winning defender
         * LDefenderPPBVAdjustment		- Amount of BV needed for extra batch of prodpoints
         * LDefenderPPUnitAdjustment	- Number of units needed for extra batch of prodpoints
         *
         * LAttackerBaseUnitsTaken		- base number of units taken by winning attacker
         * LAttackerUnitsBVAdjustment	- BV to take an additonal unit
         * LAttackerUnitsUnitAdjustment	- units to take an additional unit (confusing!)
         *
         * LConquestAmountCap	- Max amount of % to take, regardless of BV/units involved.
         * LDelayAmountCap		- Max amount of delay to apply, regardless of BV/units involved.
         * LPPCapptureCap		- Max # of PP to take/generate
         * LUnitCaptureCap 		- Ceiling on number of units which may be raided
         *
         * [Cost paramaters]
         * LHouseLaunchMoney 	- CBill cost to faction for attack launch - LFC=102
         * LHouseLaunchActions 	- Actions consumed by starting Long - LFC=103
         *
         * LPlayerLaunchMoney	- Cbills taken from player @ long start. nonreimbursable - LFC=104
         * LPlayerLaunchReward	- RP taken from player @ long start. nonreimbursable - LFC=105
         * LPlayerLaunchFlu		- Influence taken from player @ long start. nonreimbursable - LFC=106
         * LPlayerLaunchExp		- EXP taken from player @ long start. can't revert - LFC=107
         *
         * LPlayerLaunchMezzo	- Mezzo added to player @ long start. can't revert.
         *
         * NOTE: notes from short ops generally apply to these as well.
         *
         * CURRENT PARAM COUNT: 173 + mandatory
         */
        // META outcome paramaters
        operationsDefaults.put("LAttackerBaseConquestAmount", "0");
        operationsDefaults.put("LAttackerConquestBVAdjustment", "0");
        operationsDefaults.put("LAttackerConquestUnitAdjustment", "0");

        operationsDefaults.put("LDefenderBaseConquestAmount", "0");
        operationsDefaults.put("LDefenderConquestBVAdjustment", "0");
        operationsDefaults.put("LDefenderConquestUnitAdjustment", "0");

        operationsDefaults.put("LAttackerBaseDelayAmount", "0");
        operationsDefaults.put("LAttackerDelayBVAdjustment", "0");
        operationsDefaults.put("LAttackerDelayUnitAdjustment", "0");

        operationsDefaults.put("LDefenderBaseDelayAmount", "0");
        operationsDefaults.put("LDefenderDelayBVAdjustment", "0");
        operationsDefaults.put("LDefenderDelayUnitAdjustment", "0");

        operationsDefaults.put("LAttackerBasePPAmount", "0");
        operationsDefaults.put("LAttackerPPBVAdjustment", "0");
        operationsDefaults.put("LAttackerPPUnitAdjustment", "0");

        operationsDefaults.put("LDefenderBasePPAmount", "0");
        operationsDefaults.put("LDefenderPPBVAdjustment", "0");
        operationsDefaults.put("LDefenderPPUnitAdjustment", "0");

        operationsDefaults.put("LAttackerBaseUnitsTaken", "0");
        operationsDefaults.put("LAttackerUnitsBVAdjustment", "0");
        operationsDefaults.put("LAttackerUnitsUnitAdjustment", "0");

        operationsDefaults.put("LAttackerTargetOpAdjustment", "0");
        operationsDefaults.put("LDefenderTargetOpAdjustment", "0");

        operationsDefaults.put("LConquestAmountCap", "10000000");
        operationsDefaults.put("LDelayAmountCap", "10000000");
        operationsDefaults.put("LPPCapptureCap", "10000000");
        operationsDefaults.put("LUnitCaptureCap", "10000000");

        // Launch costs
        operationsDefaults.put("LHouseLaunchMoney", "0");
        operationsDefaults.put("LHouseLaunchActions", "0");

        operationsDefaults.put("LPlayerLaunchMoney", "0");
        operationsDefaults.put("LPlayerLaunchReward", "0");
        operationsDefaults.put("LPlayerLaunchFlu", "0");
        operationsDefaults.put("LPlayerLaunchExp", "0");

        operationsDefaults.put("LPlayerLaunchMezzo", "0");

        operationsDefaults.put("IgnorePilotsForBVSpread", "false");

        operationsDefaults.put("UseClanEquipmentRatios", "false");
        operationsDefaults.put("AttackerMaxClanEquipmentPercent", "100.0");
        operationsDefaults.put("AttackerMinClanEquipmentPercent", "0.0");
        operationsDefaults.put("DefenderMaxClanEquipmentPercent", "100.0");
        operationsDefaults.put("DefenderMinClanEquipmentPercent", "0.0");

        operationsDefaults.put("CountSupportUnits", "True");
        operationsDefaults.put("MinAttackerSupportUnits", "0");
        operationsDefaults.put("MaxAttackerSupportUnits", "999");
        operationsDefaults.put("MinDefenderSupportUnits", "0");
        operationsDefaults.put("MaxDefenderSupportUnits", "999");
        operationsDefaults.put("CountSupportUnitsForSpread", "true");
        operationsDefaults.put("EnforceAttackerSupportRatio", "false");
        operationsDefaults.put("EnforceDefenderSupportRatio", "false");
        operationsDefaults.put("MaxDefenderSupportPercent", "100");
        operationsDefaults.put("MinDefenderSupportPercent", "0");
        operationsDefaults.put("MaxAttackerSupportPercent", "100");
        operationsDefaults.put("MinAttackerSupportPercent", "0");
        operationsDefaults.put("MinAttackerNonSupportUnits", "0");
        operationsDefaults.put("MaxAttackerNonSupportUnits", "999");
        operationsDefaults.put("MinDefenderNonSupportUnits", "0");
        operationsDefaults.put("MaxDefenderNonSupportUnits", "999");


        // Flags
        operationsDefaults.put("AttackerFlags", "");
        operationsDefaults.put("DefenderFlags", "");
        operationsDefaults.put("WinnerFlags", "");
        operationsDefaults.put("LoserFlags", "");
    }

    // METHODS
    /**
     * Method which returns the default value of a given operation paramater. @urgru 5/30/05
     */
    public String getDefault(String valToGet) {
        return operationsDefaults.get(valToGet);
    }

}// end OperationsManager class
