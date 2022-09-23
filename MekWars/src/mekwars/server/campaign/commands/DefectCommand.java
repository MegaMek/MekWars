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

package server.campaign.commands;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeSet;

import common.Planet;
import common.UnitFactory;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.TechConstants;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.NewbieHouse;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.util.HouseRankingHelpContainer;
import server.util.MWPasswd;

public class DefectCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        if (command.hasMoreTokens()) {

            boolean isSingleFaction = CampaignMain.cm.getBooleanConfig("AllowSinglePlayerFactions");

            String HouseName = command.nextToken();
            String shortName = "";

            // check confirmation, used later
            boolean commandConfirmed = false;
            boolean allowDamagedUnits = CampaignMain.cm.isUsingAdvanceRepair() && CampaignMain.cm.getBooleanConfig("AllowDonatingOfDamagedUnits");

            if (command.hasMoreElements() && ((String) command.nextElement()).equals("CONFIRM"))
                commandConfirmed = true;

            if (command.hasMoreTokens())
                shortName = command.nextToken();

            SPlayer p = CampaignMain.cm.getPlayer(Username);
            if (CampaignMain.cm.getMarket().hasActiveListings(p)) {
                CampaignMain.cm.toUser("AM:You are not allowed to defect while you have units on the black market.", Username, true);
                return;
            }

            if (!isSingleFaction && CampaignMain.cm.getData().getHouseByName(HouseName) == null) {
                CampaignMain.cm.toUser("AM:Could not find a faction with that name. Try again?", Username, true);
                return;
            }

            SHouse newHouse = (SHouse) CampaignMain.cm.getData().getHouseByName(HouseName);
            SHouse oldHouse = p.getMyHouse();
            if (!oldHouse.getHouseDefectionFrom()) {
                CampaignMain.cm.toUser("AM:You may not defect from this faction!", Username, true);
                return;
            }

            if (newHouse != null && !newHouse.getHouseDefectionTo()) {
                CampaignMain.cm.toUser("AM:You may not defect to the " + HouseName + " faction.", Username, true);
                return;
            }

            if (CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
                CampaignMain.cm.toUser("AM:You may not defect while playing a game. Try again after your game is finished.", Username, true);
                return;
            }

            if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
                CampaignMain.cm.toUser("AM:You may not defect while active. Try again after you've returned to reserve.", Username, true);
                return;
            }

            // establish XP minimums
            int mercEXPRequired = CampaignMain.cm.getIntegerConfig("MinEXPforMercenaries") - 10 * (int) (p.getRating() - 1600); // Min
            // EXP to become a merc is that standard amount (7500) minus 10 per
            // ELO above 1600.
            int minEXPRequired = CampaignMain.cm.getIntegerConfig("MinEXPforDefecting"); // basic
            // start
            // for
            // exp
            // needed

            TreeSet<HouseRankingHelpContainer> s = CampaignMain.cm.getHouseRanking();
            int factionPlace = s.size();
            for (HouseRankingHelpContainer h : s) {
                factionPlace--;
                if (h.getHouse().equals(newHouse))
                    minEXPRequired += factionPlace * CampaignMain.cm.getIntegerConfig("EXPNeededPerHouseRank");
            }

            // throw out if players has too little XP to defect
            if (p.getExperience() < minEXPRequired) {
                CampaignMain.cm.toUser("AM:You're too inexperienced to defect. You need at least " + minEXPRequired + " XP to join that faction.", p.getName(), true);
                return;
            }

            // throw out if going to same faction
            if (newHouse != null && newHouse.equals(oldHouse)) {
                CampaignMain.cm.toUser("AM:You're already in that faction!", p.getName(), true);
                return;
            }

            // throw out if player is going merc and lacks the requisite XP
            else if (newHouse != null && p.getExperience() < mercEXPRequired && newHouse.isMercHouse()) {
                if (Boolean.parseBoolean(CampaignMain.cm.getConfig("HideELO")))
                    CampaignMain.cm.toUser("AM:You're too inexperienced to defect to a Mercenary faction!", p.getName(), true);
                else
                    CampaignMain.cm.toUser("AM:You're too inexperienced to defect to a Mercenary faction!  You need " + mercEXPRequired + " experience with your current Rating!", p.getName(), true);
                return;
            }

            // break out if user tries to defect back to SOL
            if (newHouse != null && newHouse.isNewbieHouse()) {
                CampaignMain.cm.toUser("AM:You may not defect back to the training faction.", Username, true);
                return;
            }

            // DO NOT LET PLAYERS LEAVE SOL UNLESS THEY HAVE A PASSWORD!
            /*
             * Lifted this code from the server registration process wholesale.
             * Hacky and evil. Should add a hasPassword() check similar to
             * isAdmin() if the existance of passwds is checked in more
             * locations than reg and defect. @urgru
             */
            boolean regged = false;
            try {
                MWPasswd.getRecord(p.getName(), null);
            } catch (Exception ex) {
                regged = true;
            }

            if (!regged) {
                // SEND WARNING
                CampaignMain.cm.toUser("AM:<br>-----<br>" + "You may not join a faction until you set a password for your account. Click [<a href=\"MWREG\">HERE</a>] to register.<br>" + "NOTE: Passwords must be between 4 and 10 characters in length, and should use standard ASCII characters.<br>-----", Username, true);
                return;
            }// end if(not registered, send instructions)

            boolean penalizeDefection = true;

            // don't take things from non-SOL players unless resetting
            if (oldHouse.isNewbieHouse()) {
                penalizeDefection = false;
            }

            // check to see if merc defections should be penalized
            if (newHouse != null && newHouse.isMercHouse() && !CampaignMain.cm.getBooleanConfig("PenalizeDefectToMerc")) {
                penalizeDefection = false;
            }

            // check to see if non-conq defects should be penalized
            if (newHouse != null && !newHouse.isConquerable() && !CampaignMain.cm.getBooleanConfig("PenalizeDefectToNonConq")) {
                penalizeDefection = false;
            }

            // store so player can be told exactly how much was lost.
            int oldExp = p.getExperience();
            int oldMoney = p.getMoney();
            int oldFlu = p.getInfluence();
            int oldRP = p.getReward();

            int newExp = 0;
            int newMoney = 0;
            int newFlu = 0;
            int newRP = 0;

            int expLoss = 0;
            int mnyLoss = 0;
            int fluLoss = 0;
            int rwdLoss = 0;

            int startingUnits = p.getUnits().size();
            int unitsToLose = 0;

            // counter used for proper string formatting later
            int varsWhichChange = 0;

            /*
             * If leaving SOL and the player's units are to be reset, announce.
             */
            boolean solToBeReset = p.getMyHouse().isNewbieHouse() && CampaignMain.cm.getBooleanConfig("ReplaceUnitsLeavingSOL");
            boolean replaceWithFaction = p.getMyHouse().isNewbieHouse() && CampaignMain.cm.getBooleanConfig("FactionUnitsLeavingSOL");

            /*
             * If defection should be penalized, load the relevant server config
             * vars, save the player's start state, and begin prepping defection
             * string. Do not *set* the new values until confirmation is
             * checked.
             */
            if (penalizeDefection) {

                /*
                 * Check configs to see if theyre flat amounts or percentage
                 * losses. This is stupidly inefficient; however, its
                 * flexibility that the operators have asked for. Percentage
                 * mods supercede flat mods. Always.
                 */
                int expLossPerc = CampaignMain.cm.getIntegerConfig("DefectionEXPLossPercent");
                int expLossFlat = CampaignMain.cm.getIntegerConfig("DefectionEXPLossFlat");

                int mnyLossPerc = CampaignMain.cm.getIntegerConfig("DefectionCBillLossPercent");
                int mnyLossFlat = CampaignMain.cm.getIntegerConfig("DefectionCBillLossFlat");

                int fluLossPerc = CampaignMain.cm.getIntegerConfig("DefectionInfluenceLossPercent");
                int fluLossFlat = CampaignMain.cm.getIntegerConfig("DefectionInfluenceLossFlat");

                int rwdLossPerc = CampaignMain.cm.getIntegerConfig("DefectionRewardLossPercent");
                int rwdLossFlat = CampaignMain.cm.getIntegerConfig("DefectionRewardLossFlat");

                int unitLossPerc = CampaignMain.cm.getIntegerConfig("DefectionUnitLossPercent");
                int unitLossFlat = CampaignMain.cm.getIntegerConfig("DefectionUnitLossFlat");

                // determine new xp
                if (expLossPerc > 0) {
                    newExp = (oldExp * (100 - expLossPerc)) / 100;
                } else {
                    newExp = oldExp - expLossFlat;
                    if (newExp < 0) {
                        newExp = 0;
                    }
                }
                expLoss = oldExp - newExp;
                if (expLoss > 0)
                    varsWhichChange++;

                // determine new money
                if (mnyLossPerc > 0) {
                    newMoney = (oldMoney * (100 - mnyLossPerc)) / 100;
                } else {
                    newMoney = oldMoney - mnyLossFlat;
                    if (newMoney < 0) {
                        newMoney = 0;
                    }
                }
                mnyLoss = oldMoney - newMoney;
                if (mnyLoss > 0)
                    varsWhichChange++;

                // determine new flu
                if (fluLossPerc > 0) {
                    newFlu = (oldFlu * (100 - fluLossPerc)) / 100;
                } else {
                    newFlu = oldFlu - fluLossFlat;
                    if (newFlu < 0) {
                        newFlu = 0;
                    }
                }
                fluLoss = oldFlu - newFlu;
                if (fluLoss > 0)
                    varsWhichChange++;

                // determine new RP
                if (rwdLossPerc > 0) {
                    newRP = (oldRP * (100 - rwdLossPerc)) / 100;
                } else {
                    newRP = oldRP - rwdLossFlat;
                    if (newRP < 0) {
                        newRP = 0;
                    }
                }
                rwdLoss = oldRP - newRP;
                if (rwdLoss > 0)
                    varsWhichChange++;

                /*
                 * Unit calculations are goofier. Figure out how many would be
                 * lost, but don't actually draw the units until the move is
                 * being effectuated.
                 */
                if (unitLossPerc > 0) {
                    unitsToLose = (startingUnits * unitLossPerc) / 100;
                } else {
                    unitsToLose = unitLossFlat;
                    if (unitsToLose > startingUnits) {
                        unitsToLose = startingUnits;
                    }
                }

            }

            /*
             * Both confirmed and unconfirmed use the same general info about
             * what is being/would be lost, so build that here and save it.
             */
            // penalty amount string.
            int varsAddedToString = 0;
            String penString = "";
            if (penalizeDefection && varsWhichChange > 0) {
                penString = "";
                if (expLoss > 0) {
                    penString += expLoss + " XP";
                    varsAddedToString++;
                }

                if (varsAddedToString + 1 == varsWhichChange && unitsToLose == 0)
                    penString += " and ";
                else if (varsAddedToString > 0 && varsAddedToString != varsWhichChange)
                    penString += ", ";

                if (mnyLoss > 0) {
                    penString += CampaignMain.cm.moneyOrFluMessage(true, false, mnyLoss);
                    varsAddedToString++;
                }

                if (varsAddedToString + 1 == varsWhichChange && unitsToLose == 0)
                    penString += " and ";
                else if (varsAddedToString > 0 && varsAddedToString != varsWhichChange)
                    penString += ", ";

                if (fluLoss > 0) {
                    penString += CampaignMain.cm.moneyOrFluMessage(false, false, fluLoss);
                    varsAddedToString++;
                }

                if (varsAddedToString + 1 == varsWhichChange && unitsToLose == 0)
                    penString += " and ";
                else if (varsAddedToString > 0 && varsAddedToString != varsWhichChange)
                    penString += ", ";

                if (rwdLoss > 0) {
                    penString += rwdLoss + " " + CampaignMain.cm.getConfig("RPLongName");
                    varsAddedToString++;
                }

                if (varsAddedToString + 1 == varsWhichChange && unitsToLose == 0)
                    penString += " and ";
                else if (varsAddedToString > 0 && varsAddedToString != varsWhichChange)
                    penString += ", ";
            }

            /*
             * If command is not confirmed, construct a string showing the
             * player what he stands to lose, ask him to confirm, and return to
             * end this run of process().
             * 
             * If the command is confirmed, move on and do the move.
             */
            if (!commandConfirmed) {

                // no penalty. send a simple confirm link.
                if (!penalizeDefection && !solToBeReset) {
                    CampaignMain.cm.toUser("AM:Click [<a href=\"MWDEFECTDLG/c defect#" + HouseName + "#CONFIRM#" + shortName + "\">here</a>] to confirm your defection to " + HouseName + ".<br>", p.getName(), true);
                    return;
                } else if (solToBeReset && !replaceWithFaction) {
                    CampaignMain.cm.toUser("AM:Click [<a href=\"MWDEFECTDLG/c defect#" + HouseName + "#CONFIRM#" + shortName + "\">here</a>] to confirm your defection to " + HouseName + ". Your units will be reset.<br>", p.getName(), true);
                    return;
                } else if (solToBeReset && replaceWithFaction) {
                    CampaignMain.cm.toUser("AM:Click [<a href=\"MWDEFECTDLG/c defect#" + HouseName + "#CONFIRM#" + shortName + "\">here</a>] to confirm your defection to " + HouseName + ". " + HouseName + " will replace your units.", p.getName(), true);
                    return;
                }

                // could have a penalty, but apparently none would be applied
                // ... so ...
                if (varsWhichChange == 0 && unitsToLose == 0) {
                    CampaignMain.cm.toUser("AM:Click [<a href=\"MWDEFECTDLG/c defect#" + HouseName + "#CONFIRM#" + shortName + "\">here</a>] to confirm your defection to " + HouseName + ".<br>", p.getName(), true);
                    return;
                }

                // else, things change
                String toReturn = "If you defect to " + HouseName + "  you will lose " + penString;

                if (varsWhichChange != 0)
                    toReturn += " and ";

                if (unitsToLose == startingUnits)
                    toReturn += "all of your units.";
                else if (unitsToLose > 0)
                    toReturn += unitsToLose + " units.";
                else
                    toReturn = toReturn.trim() + ".";

                toReturn += " Click [<a href=\"MWDEFECTDLG/c defect#" + HouseName + "#CONFIRM\">here</a>] to confirm your defection to " + HouseName + ".<br>";
                CampaignMain.cm.toUser(toReturn, p.getName(), true);
                return;

            }// end if(unconfirmed)

            if (newHouse == null && isSingleFaction) {
                if ( HouseName.trim().length() < 5){
                    CampaignMain.cm.toUser("Your faction name must be Greater then 4 characters long!", Username);
                    return;
                }
                    
                if ( shortName.trim().length() < 2){
                    CampaignMain.cm.toUser("Your factions short name needs be to 2 or more characters long!", Username);
                    return;
                }
                
                newHouse = createSingleFaction(HouseName, shortName);

                if (newHouse == null) {
                    CampaignMain.cm.toUser("Sorry but their is already a house with the name of " + HouseName + " please try again.", Username);
                    return;
                }
                newHouse.addLeader(Username);
            }

            // setup the return info
            String toReturn = "You succesfully defected to " + HouseName + ".<br>";
            
            if ( isSingleFaction && !newHouse.getPlanets().values().isEmpty()){
                toReturn += "You've inherited planet ";
                for ( SPlanet planet : newHouse.getPlanets().values() )
                    toReturn += planet.getNameAsColoredLink();
                toReturn += "<br>";
            }

            // should be penalized, and the player is actually losing something
            // ...
            if ((penalizeDefection && (unitsToLose > 0 || varsWhichChange > 0)) || solToBeReset) {

                if (solToBeReset) {

                    NewbieHouse nh = (NewbieHouse) p.getMyHouse();

                    if (replaceWithFaction)
                        nh.requestNewMech(p, true, HouseName);
                    else
                        nh.requestNewMech(p, true, null);

                    toReturn += "Your units ";
                    if (CampaignMain.cm.getBooleanConfig("AllowPersonalPilotQueues"))
                        toReturn += " and pilot queue ";
                    toReturn += "were reset";
                }

                else {

                    // set new values. simple.
                    p.addExperience(-expLoss, false);// pos number for string
                    // setup. negate to
                    // reduce.
                    p.addMoney(-mnyLoss);// pos number for string setup.
                    // negate to reduce.
                    p.setInfluence(newFlu);
                    p.setReward(newRP);

                    // have string show those losses
                    toReturn += "You've lost " + penString;

                    /*
                     * set units. more complicated ...
                     * 
                     * If all units, put everything into factionbay and then
                     * strip the player.
                     * 
                     * If only some units, need to make a random selection and
                     * keep a list of whats gone to the faction for later
                     * display.
                     */
                    StringBuilder hsUpdates = new StringBuilder();
                    boolean damaged = false;
                    if (unitsToLose == startingUnits) {

                        // move all units to the faction bay
                        for (SUnit currU : p.getUnits()) {
                            damaged = (!UnitUtils.canStartUp(currU.getEntity()) || UnitUtils.hasArmorDamage(currU.getEntity()) || UnitUtils.hasCriticalDamage(currU.getEntity()));
                            if ((damaged && allowDamagedUnits) || !damaged)
                                hsUpdates.append(oldHouse.addUnit(currU, false));
                        }
                        p.stripOfAllUnits(false);

                        // add to string
                        toReturn += " and all of your units";
                    }

                    else if (unitsToLose > 0) {

                        // add to string
                        toReturn += " and the following units: ";

                        // remove some random units. wee.
                        int numRemoved = 0;

                        while (numRemoved < unitsToLose) {

                            if (numRemoved != 0)
                                toReturn += ", ";

                            // pick random unit
                            SUnit toRemove = p.getUnits().get(CampaignMain.cm.getRandomNumber(p.getUnits().size()));

                            // make an actual unit, store name, remove
                            toReturn += toRemove.getModelName();

                            damaged = (!UnitUtils.canStartUp(toRemove.getEntity()) || UnitUtils.hasArmorDamage(toRemove.getEntity()) || UnitUtils.hasCriticalDamage(toRemove.getEntity()));
                            if ((damaged && allowDamagedUnits) || !damaged)
                                hsUpdates.append(oldHouse.addUnit(toRemove, false));
                            p.removeUnit(toRemove.getId(), false);// check ops
                            // will
                            // execute
                            // on login
                            // to new
                            // house

                            // increment counter
                            numRemoved++;
                        }
                    }

                    // sned updates to housemates
                    if (hsUpdates.length() > 0)
                        CampaignMain.cm.doSendToAllOnlinePlayers(oldHouse, "HS|" + hsUpdates.toString(), false);

                }

                toReturn += ".";
            }

            /*
             * All returns passed, and command confirmed.
             * 
             * Move the player into his new faction and send out all the
             * relevant messages w/i the faction, to the player and on the RSS
             * feed.
             */
            p.getMyHouse().removeLeader(p.getName());

            String clientVersion = p.getPlayerClientVersion();

            p.getMyHouse().removePlayer(p, false);
            p.setMyHouse(newHouse);
            p.setSubFaction(newHouse.getZeroLevelSubFaction());
            // CampaignMain.cm.forceSavePlayer(p);

            // send the various messages
            CampaignMain.cm.toUser(toReturn, Username, true);
            CampaignMain.cm.doSendHouseMail(oldHouse, "NOTE: ", p.getName() + " defected to " + HouseName);
            CampaignMain.cm.doSendHouseMail(newHouse, "NOTE: ", p.getName() + " joined the faction! (Defected from " + oldHouse.getName() + ")");

            // do we really want to keep the RSS feed?
            CampaignMain.cm.addToNewsFeed("Player Defection", "Player News", Username + " defected from " + oldHouse.getName() + " to " + HouseName);
            CampaignMain.cm.postToDiscord(Username + " defected from " + oldHouse.getName() + " to " + HouseName);

            if ( p.getMyLogo().trim().equals(oldHouse.getLogo().trim()) ){
                p.setMyLogo(newHouse.getLogo().trim());
            }
            
            // for now, move defecting players back to standard-user access.
            // Don't
            // let people defect and retain faction leadership access, etc. May
            // be
            // a problem for mods, but better than the alternative ...
            if (p.getMyHouse().equals(newHouse) && !CampaignMain.cm.getServer().isAdmin(Username) && !isSingleFaction)
                MWPasswd.getRecord(Username).setAccess(2);

            /*
             * Player is part of his new house. Check the ammo in all of his
             * units, removing illegal ammos.
             */
            for (SUnit currU : p.getUnits())
                SUnit.checkAmmoForUnit(currU, newHouse);

            /*
             * Might as well just log the player into his new faction. Sort of
             * silly to make him redo /c login when we can duplicate the login
             * process right here.
             */

            CampaignMain.cm.forceSavePlayer(p);
            CampaignMain.cm.doLoginPlayer(Username);
            CampaignMain.cm.toUser("SP|Welcome to " + HouseName + "!", p.getName(), false);

            /*
             * Now that the player is in his new faction, check his tech status.
             * If the new faction grants fewer bays than his previous faction,
             * and he doesnt have enough hired techs to pick up the slack, some
             * units should be unmaintained.
             */
            if (p.getFreeBays() < 0) {
                p.setRandomUnmaintained();

                int factionBays = p.getMyHouse().getBaysProvided();
                int minBays = CampaignMain.cm.getIntegerConfig("MinimumHouseBays");

                String dismayMessage = "You are dismayed when you discover that " + p.getMyHouse().getName() + " only has ";
                if (minBays > factionBays)
                    dismayMessage += minBays;
                else
                    dismayMessage += factionBays;

                if (CampaignMain.cm.isUsingAdvanceRepair())
                    dismayMessage += " bays to house your units. You'll have to buy more bays if you want to go active!";
                else
                    dismayMessage += " technicians avaliable to assign to your force. You order your techs to " + "ignore the maintaince needs of some units, adjust the duty roster, and consider hiring more" + "technicians ... (Some units are now unmaintained! Check your status!)";

                CampaignMain.cm.toUser(dismayMessage, Username, true);
            }// end if(defection leaves player with negative bays)

            p.setPlayerClientVersion(clientVersion);
            CampaignMain.cm.forceSavePlayer(p);

        }// end if(more tokens)

    }// end process()

    public SHouse createSingleFaction(String houseName, String shortName) {

        SHouse house = new SHouse();
        int maxHouseName = CampaignMain.cm.getIntegerConfig("MaxFactionName");
        int maxShortName = CampaignMain.cm.getIntegerConfig("MaxFactionShortName");

        if (houseName.length() > maxHouseName)
            houseName = houseName.substring(0, maxHouseName);

        if (shortName.length() > maxShortName)
            shortName = shortName.substring(0, maxShortName);

        if (CampaignMain.cm.getData().getHouseByName(houseName) != null) {
            return null;
        }

        house.createNoneHouse();
        house.setName(houseName);
        house.setAbbreviation(shortName);
        house.setBaseGunner(4);
        house.setBasePilot(5);
        house.setConquerable(true);
        house.setTechLevel(TechConstants.T_INTRO_BOXSET);
        CampaignMain.cm.addHouse(house);
        CampaignMain.cm.doSendToAllOnlinePlayers("PL|ANH|" + house.addNewHouse(), false);
        for (int type = 0; type < SUnit.MAXBUILD; type++) {
            for (int weight = 0; weight <= SUnit.ASSAULT; weight++) {
                house.addPP(weight, type, CampaignMain.cm.getIntegerConfig("BaseFactoryComponents"), false);
            }
        }

        findEmptyPlanet(house);

        house.updated();

        return house;
    }

    public void findEmptyPlanet(SHouse house) {
        LinkedList<Planet> planetList = new LinkedList<Planet>();

        for (Planet newPlanet : CampaignMain.cm.getData().getAllPlanets()) {
            int totalCP = newPlanet.getConquestPoints();

            if (newPlanet.getInfluence().getInfluence(-1) == totalCP)
                planetList.add(newPlanet);
        }

        if (planetList.size() < 1) {
            MWLogger.errLog("Error Unable to find planet for new faction " + house.getName());
            CampaignMain.cm.doSendModMail("NOTE", "Error Unable to find planet for new faction " + house.getName());
        }

        String[] factoryNames = { "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa", "Lamda", "Mu", "Nu", "Xi", "Omikron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi", "Chi", "Psi", "Omega" };
        int planetNumber = CampaignMain.cm.getRandomNumber(planetList.size());

        SPlanet planet = (SPlanet) planetList.get(planetNumber);
        planet.setConquestPoints(1000);
        planet.setCompProduction(1000);
        int baseCommonBuildTableShare = CampaignMain.cm.getIntegerConfig("BaseCommonBuildTableShares");

        for (int weight = 0; weight <= SUnit.ASSAULT; weight++) {
            for (int type = 0; type < SUnit.MAXBUILD; type++) {
                String weightName = SUnit.getWeightClassDesc(weight);
                String typeName = SUnit.getTypeClassDesc(type);
                int maxFactories = CampaignMain.cm.getIntegerConfig("Starting" + weightName + typeName + "Factory");
                int factoryType = UnitFactory.BUILDMEK;

                switch (type) {
                case SUnit.VEHICLE:
                    factoryType = UnitFactory.BUILDVEHICLES;
                    break;
                case SUnit.INFANTRY:
                    factoryType = UnitFactory.BUILDINFANTRY;
                    break;
                case SUnit.BATTLEARMOR:
                    factoryType = UnitFactory.BUILDBATTLEARMOR;
                    break;
                case SUnit.AERO:
                    factoryType = UnitFactory.BUILDAERO;
                    break;
                case SUnit.PROTOMEK:
                    factoryType = UnitFactory.BUILDPROTOMECHS;
                    break;
                default:
                    factoryType = UnitFactory.BUILDMEK;
                    break;
                }

                for (int count = 0; count < maxFactories; count++) {
                    String factoryName = factoryNames[count] + " " + weightName + " " + typeName + " Factory";
                    SUnitFactory factory = new SUnitFactory(factoryName, planet, weightName, house.getName(), 0, CampaignMain.cm.getIntegerConfig("BaseFactoryRefreshRate"), factoryType, BuildTable.STANDARD, 0);
                    planet.getUnitFactories().add(factory);
                }

                try {
                    String buildTableName = "./data/buildtables/"+BuildTable.STANDARD+"/";
                    String rewardbuildTableName = "./data/buildtables/"+BuildTable.REWARD+"/";
                    if (type == SUnit.MEK) {
                        buildTableName += house.getName() + "_" + weightName + ".txt";
                        rewardbuildTableName += house.getName() + "_" + weightName + ".txt";
                    } else {
                        buildTableName += house.getName() + "_" + weightName + typeName + ".txt";
                        rewardbuildTableName += house.getName() + "_" + weightName + typeName + ".txt";
                    }
                    FileOutputStream out = new FileOutputStream(buildTableName);
                    PrintStream ps = new PrintStream(out);
                    ps.println(baseCommonBuildTableShare + " Common");
                    ps.close();
                    out.close();
                    out = new FileOutputStream(rewardbuildTableName);
                    ps = new PrintStream(out);
                    ps.println(baseCommonBuildTableShare + " Common");
                    ps.close();
                    out.close();
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }
        }

        HashMap<Integer,Integer> flu = new HashMap<Integer,Integer>();
        flu.put(house.getId(),planet.getConquestPoints());
        planet.getInfluence().setInfluence(flu);
        planet.setBaysProvided(CampaignMain.cm.getIntegerConfig("StartingPlanetBays"));
        
        planet.setOwner(null, planet.checkOwner(), true);
        house.setInitialHouseRanking(planet.getConquestPoints());
        
        planet.updated();
    }
}// end DefectCommand.java
