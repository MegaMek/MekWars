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

import java.io.File;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;

import common.Unit;
import common.util.MWLogger;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.NewbieHouse;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.pilot.SPilot;

public class RequestCommand implements Command {

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

    	int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
    	
        if (accessLevel != 0) {
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }

        /*
         * A command which builds a new mech for a player if: 1)he has enough
         * money and influence, and the room in which to place a new unit 2)the
         * given factory is open to make that weight unit, and the PP to do so
         *
         * Note that players may define the planet they want to produce with, if
         * not, the planet is selected at random from the avaliable pool of
         * producing planets. This is in order to allow players to select world
         * with specific tech biases, or specific faction tables, if they so
         * desire (ie - a Marik really wants a unit from that captured Steiner
         * assault world).
         *
         * USAGE: /c request#Weighclass#Unit Type#Planet#Factory Name - Planet &
         * Factory are optional, but fac must be given if a planet is given
         */

        // get the player
        SPlayer p = CampaignMain.cm.getPlayer(Username);

        // set some defaults ...
        SPlanet planet = null;
        SUnitFactory factory = null;
        boolean needsMoreTechs = false;// used to make clickthrough

        String result = "";

        // get the requested weightclass
        String weightstring = command.nextToken();

        // check to see if the player is in SOL and trying to get new units
        if (p.getMyHouse().isNewbieHouse()) {

            if (weightstring.equals("resetunits")) {
                NewbieHouse nh = (NewbieHouse) p.getMyHouse();
                CampaignMain.cm.toUser(nh.requestNewMech(p, false, null), Username, true);
                return;
            }

            // else
            result = CampaignMain.cm.getConfig("NewbieHouseName") + " players may not purchase new units; however, they may reset their units.";
            result += "<br><a href=\"MEKWARS/c request#resetunits\">Click here to request a reset of your units.</a>";
            CampaignMain.cm.toUser(result, Username, true);
            return;

        }

        // boot the player's request if he has unmaintained units
        if (p.hasUnmaintainedUnit()) {
            CampaignMain.cm.toUser("AM:Your faction refuses to assign new units to you force while units in your hangar are unmaintained!", Username, true);
            return;
        }

        // if the player qualifies for welfare, give him a unit.
        if (p.mayAcquireWelfareUnits()) {
            SHouse house = p.getMyHouse();
            SUnit unit = buildWelfareMek(house.getName());

            SPilot pilot = house.getNewPilot(unit.getType());
            unit.setPilot(pilot);
            p.addUnit(unit, true);
            CampaignMain.cm.toUser("AM:High Command has given you a new unit from its welfare rolls to help you get back on your feet!", Username, true);
            return;
        }

        int weightclass;
        try {// try as an int (eg - 4 for assault)
            weightclass = Integer.parseInt(weightstring);
        } catch (Exception ex) {// formatting error. look for a string.
            weightclass = Unit.getWeightIDForName(weightstring.toUpperCase());
        }

        // get the requested unit type
        int type_id = Unit.MEK;// default to Mek
        String typestring = command.nextToken();
        try {// try as int
            type_id = Integer.parseInt(typestring);
        } catch (Exception ex) {// ex, so retry as string
            type_id = Unit.getTypeIDForName(typestring);
        }

        // break out if player lacks experience to buy weightclass
        if (!p.mayUse(weightclass)) {
            CampaignMain.cm.toUser("AM:You are not experienced enough to use " + Unit.getWeightClassDesc(weightclass) + " units.", Username, true);
            return;
        }

        if (!Boolean.parseBoolean(p.getSubFaction().getConfig("CanBuyNew" + Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(type_id)))) {
            CampaignMain.cm.toUser("AM:Sorry as a member of " + p.getSubFactionName() + " you are unable to purchase this unit.", Username);
            return;
        }

        if (!p.hasRoomForUnit(type_id, weightclass)) {
            CampaignMain.cm.toUser("AM:Sorry, you already have the maximum number of " + Unit.getWeightClassDesc(weightclass) + " " + Unit.getTypeClassDesc(type_id) + "s", Username);
            return;
        }

        /*
         * Check to see if a planet and factory names are specified. If they
         * are, fetch the planet and factory and make sure the user entered a
         * legal combo.
         */
        String planetName = "";
        String factoryName = "";

        if (command.hasMoreElements()) {

            planetName = command.nextToken();
            planet = (SPlanet) CampaignMain.cm.getData().getPlanetByName(planetName);
            if (planet == null) {
                CampaignMain.cm.toUser("AM:Could not find planet: " + planetName + ".", Username, true);
                return;
            }

            // make sure the player's faction owns the world
            if (!planet.getOwner().equals(p.getMyHouse())) {
                CampaignMain.cm.toUser("AM:Your faction does not control " + planetName + ".", Username, true);
                return;
            }

            // make sure the planet makes units of the desired weightclass
            if (planet.getFactoriesOfWeighclass(weightclass).size() == 0) {
                CampaignMain.cm.toUser(planetName + " does not produce units of the weight class specified.", Username, true);
                return;
            }

            // check for a factory name token
            try {
                factoryName = command.nextToken();
            } catch (NoSuchElementException e) {
                CampaignMain.cm.toUser("AM:You requested a unit from " + planetName + ", but did not specifiy which factory to use.", Username, true);
                return;
            }

            // make sure the named factory exists
            Vector<SUnitFactory> namedFactories = planet.getFactoriesByName(factoryName);
            if (namedFactories.size() == 0) {
                CampaignMain.cm.toUser("AM:There is no " + factoryName + " on " + planetName + ".", Username, true);
                return;
            }

            /*
             * make sure one of the named factories produces units of the right
             * size NOTE: Because of how this is being done, the same name can
             * be used for more than one factory on a planet, but only if
             * they're produce unique weightclasses. Any name duping in a
             * weightclass will cause odd behaviour.
             */
            for (SUnitFactory currFac : namedFactories) {

                if (currFac.getWeightclass() == weightclass) {
                    factory = currFac;
                    break;
                }
            }
            if (factory == null) {
                CampaignMain.cm.toUser(factoryName + " on " + planetName + " does not produce units of the requested weightclass.", Username, true);
                return;
            }

            //Enforce_Subfaction_Factory_Access
            //@Salient
            if (CampaignMain.cm.getBooleanConfig("Enforce_Subfaction_Factory_Access") &&
            	factory.getAccessLevel() != p.getSubFactionAccess())
            {
                CampaignMain.cm.toUser("You do not have the correct rank to purchase a unit from " + factoryName + " on " + planetName + ".", Username);
                return;
            }

            if (!CampaignMain.cm.getBooleanConfig("Enforce_Subfaction_Factory_Access") &&
            	factory.getAccessLevel() > p.getSubFactionAccess())
            {
                CampaignMain.cm.toUser("You do not have sufficient rank to purchase a unit from " + factoryName + " on " + planetName + ".", Username);
                return;
            }

            // make sure the named factory can produce the requested type
            if (!factory.canProduce(type_id)) {
                CampaignMain.cm.toUser(factoryName + " on " + planetName + " does not produce units of the requested type.", Username, true);
                return;
            }

            // return if the factory is refreshing
            if (factory.getTicksUntilRefresh() > 0) {
                CampaignMain.cm.toUser(factoryName + " is currently refreshing. " + factory.getTicksUntilRefresh() + " miniticks remaining.", Username, true);
                return;
            }
        }

        /*
         * No more tokens were given. Pick a random factory from those which
         * originally belonged to the the faction. If we cannot find a factory,
         * we should return a failure.
         */
        else {
        	//@salient Enforce_Subfaction_Factory_Access
        	if(CampaignMain.cm.getBooleanConfig("Enforce_Subfaction_Factory_Access"))
        	{
                factory = p.getMyHouse().getNativeAccessableFactoryForProduction(type_id, weightclass, p.getSubFactionAccess(), Username);
                if (factory != null) 
                {
                    planet = factory.getPlanet();
                }
        	}
        	else
        	{
        		factory = p.getMyHouse().getNativeFactoryForProduction(type_id, weightclass, true);
        		if (factory != null) {
        			planet = factory.getPlanet();
        		}        		
        	}
        }
        if (planet == null || factory == null) {
            CampaignMain.cm.toUser("AM:No " + p.getMyHouse().getName() + " factory is available to fill your order at this time (Click on icon in House Status to use captured factories).", Username, true);
            return;
        }

        // get prices for the unit
        int mechCbills = factory.getPriceForUnit(weightclass, type_id);
        int mechInfluence = factory.getInfluenceForUnit(weightclass, type_id);
        int mechPP = factory.getPPCost(weightclass, type_id);

        // adjust by multipliers if this is a non-original owner.
        SHouse playerHouse = p.getMyHouse();
        if (!factory.getFounder().equalsIgnoreCase(playerHouse.getName())) {
            mechCbills = Math.round(mechCbills * CampaignMain.cm.getFloatConfig("NonOriginalCBillMultiplier"));
            mechInfluence = Math.round(mechInfluence * CampaignMain.cm.getFloatConfig("NonOriginalInfluenceMultiplier"));
            mechPP = Math.round(mechPP * CampaignMain.cm.getFloatConfig("NonOriginalComponentMultiplier"));
        }

        // Add penalty if the player is over a sliding limit
        if (p.willHaveHangarPenalty(type_id, weightclass)) {
        	int costPenalty = p.calculateHangarPenaltyForNextPurchase(type_id, weightclass);
        	mechCbills += costPenalty;
        }

        // reduce flu cost to ceiling if over
        if (mechInfluence > CampaignMain.cm.getIntegerConfig("InfluenceCeiling")) {
            mechInfluence = CampaignMain.cm.getIntegerConfig("InfluenceCeiling");
        }

        // check to see if the player & house can afford the unit
        boolean hasEnoughMoney = false;
        boolean hasEnoughInfluence = false;
        boolean factionHasEnoughPP = false;

        if (p.getMoney() >= mechCbills) {
            hasEnoughMoney = true;
        }
        if (p.getInfluence() >= mechInfluence) {
            hasEnoughInfluence = true;
        }
        if (playerHouse.getPP(weightclass, type_id) >= mechPP) {
            factionHasEnoughPP = true;
        }

        if (hasEnoughMoney && hasEnoughInfluence && factionHasEnoughPP) {

            // check to make sure the player has enough support for the unit
            // requested
            int spaceTaken = SUnit.getHangarSpaceRequired(type_id, weightclass, 0, "null", p.getMyHouse());
            if (spaceTaken > p.getFreeBays()) {
                needsMoreTechs = true;
            }

            // find out if your are using advanced repair if so buy bays instead
            // of hiring techs.
            boolean useBays = CampaignMain.cm.isUsingAdvanceRepair();

            // if the player needs more techs/bays, make a compound link and
            // return
            if (needsMoreTechs) {

                int techCost = p.getTechHiringFee();
                if (useBays) {
                    techCost = CampaignMain.cm.getIntegerConfig("CostToBuyNewBay");
                }

                int numTechs = spaceTaken - p.getFreeBays();
                techCost = techCost * numTechs;
                int totalCost = techCost + mechCbills;
                StringBuilder toSend = new StringBuilder();

                // if the player can't afford to pay for more support, tell him
                // so and return w/o a link.
                if (totalCost > p.getMoney()) {

                    toSend.append("AM:Command will not release a new unit to you unless support is in place; however, you cannot afford to buy the unit *and* ");
                    if (useBays) {
                        toSend.append(" purchase the necessary bayspace");
                    } else {
                        toSend.append(" hire technicians");
                    }
                    toSend.append(". The total cost would be " + CampaignMain.cm.moneyOrFluMessage(true, true, totalCost) + ", but you only have " + CampaignMain.cm.moneyOrFluMessage(true, true, p.getMoney()) + ".");

                    CampaignMain.cm.toUser(toSend.toString(), Username, true);
                    return;
                }

                toSend.append("AM:Quartermaster command will not send a new unit to your force until support resources are in place. You will need to ");
                if (useBays) {
                    toSend.append("purchase " + numTechs + " more bays");
                } else {
                    toSend.append("hire " + numTechs + " more technicians");
                }

                toSend.append(" at a cost of " + CampaignMain.cm.moneyOrFluMessage(true, true, techCost) + ". Combined cost of the new unit and necessary ");
                if (useBays) {
                    toSend.append("bays");
                } else {
                    toSend.append("techs");
                }
                toSend.append(" is " + CampaignMain.cm.moneyOrFluMessage(true, true, (mechCbills + techCost)) + " and " + CampaignMain.cm.moneyOrFluMessage(false, true, mechInfluence) + ".");
                toSend.append("<br><a href=\"MEKWARS/c hireandrequestnew#" + numTechs + "#" + Unit.getWeightClassDesc(weightclass) + "#" + type_id + "#" + planet.getName() + "#" + factory.getName() + "\">Click here to purchase both the unit and the needed support.</a>");

                CampaignMain.cm.toUser(toSend.toString(), Username, true);

                return;
            }

            SPilot pilot = playerHouse.getNewPilot(type_id);

            Vector<SUnit> mechs = factory.getMechProduced(type_id, pilot);
            StringBuffer results = new StringBuffer();
            if (playerHouse.getBooleanConfig("UseCalculatedCosts")) {
                mechCbills = 0;
            }
            for (SUnit mech : mechs) {
                if (playerHouse.getBooleanConfig("UseCalculatedCosts")) {
                    double unitCost = mech.getEntity().getCost(false);
                    if (unitCost < 1) {
                        unitCost = playerHouse.getPriceForUnit(mech.getWeightclass(), mech.getType());
                    }
                    double costMod = playerHouse.getDoubleConfig("CostModifier");

                    mechCbills += (int) Math.round(unitCost * costMod);
                    if (mechCbills > p.getMoney()) {
                        CampaignMain.cm.toUser("You could not afford the selected unit. Please try again", Username);
                        return;
                    }
                }

                if (CampaignMain.cm.getBooleanConfig("AllowPersonalPilotQueues") && mech.isSinglePilotUnit()) {
                    SPilot pilot1 = (SPilot) mech.getPilot();
                    SPilot pilot2 = new SPilot("Vacant", 99, 99);
                    mech.setPilot(pilot2);

                    if (!pilot1.getName().equalsIgnoreCase("Vacant")) {
                        playerHouse.getPilotQueues().addPilot(mech.getType(), pilot1, true);
                    }
                }

                p.addUnit(mech, true);// give the actual unit...
                results.append(mech.getModelName());
                results.append(", ");
            }

            results.delete(results.length() - 2, results.length());
            p.addMoney(-mechCbills);// then take away money
            p.addInfluence(-mechInfluence);// and take away influence

            // we're going to build the unit. set up a houseupdate string.
            StringBuilder hsUpdates = new StringBuilder();

            // set the refresh miniticks
            hsUpdates.append(factory.addRefresh((CampaignMain.cm.getIntegerConfig(Unit.getWeightClassDesc(factory.getWeightclass()) + "Refresh") * 100) / factory.getRefreshSpeed(), false));
            hsUpdates.append(playerHouse.addPP(weightclass, type_id, -mechPP, false));// remove
                                                                                      // PP
                                                                                      // from
                                                                                      // the
                                                                                      // faction

            result = "AM:You've been granted the following " + results.toString() + ". (-";
            result += CampaignMain.cm.moneyOrFluMessage(true, false, mechCbills) + " / -" + CampaignMain.cm.moneyOrFluMessage(false, true, mechInfluence) + ")";
            MWLogger.mainLog(p.getName() + " bought the following " + results.toString() + " from " + factory.getName() + " on " + planet.getName());
            CampaignMain.cm.toUser(result, Username, true);
            CampaignMain.cm.doSendHouseMail(playerHouse, "NOTE", p.getName() + " bought the following " + results.toString() + " from " + factory.getName() + " on " + planet.getName() + "!");

            // send update to all players
            CampaignMain.cm.doSendToAllOnlinePlayers(playerHouse, "HS|" + hsUpdates.toString(), false);

            return;
        }// end if(enough money/influence/pp)

        else if (!hasEnoughMoney || !hasEnoughInfluence) {// tell the player
                                                          // what he needs to
                                                          // buy the unit
            result = "AM:You need at least " + CampaignMain.cm.moneyOrFluMessage(true, false, mechCbills) + " and " + CampaignMain.cm.moneyOrFluMessage(false, true, mechInfluence) + " to request a " + Unit.getTypeClassDesc(type_id) + " of this weight class from a factory.";
            CampaignMain.cm.toUser(result, Username, true);
            return;// break out ...
        }// end else(player has too few money or too little Influence)
        else if (!factionHasEnoughPP) {// tell the player that the faction needs
                                       // more PP
            result = "AM:Your faction does not have the components needed to produce such a unit at this time. Wait for your faction to gather more resources.";
            CampaignMain.cm.toUser(result, Username, true);
            return;// break out ...
        }// end else (not enough PP in faction)

    }// end process()

    /**
     * Private method which builds a welfare unit. Duplicated in RequestCommand.
     * Kept private in these classes in order to ensure that ONLY requests
     * generate welfare units (had previously been a public call in
     * CampaignMain).
     */
    private SUnit buildWelfareMek(String producer) {
        String Filename = "./data/buildtables/standard/" + producer + "_Welfare.txt";

        // Check for Faction Specific welfare tables first
        if (!(new File(Filename).exists())) {
            Filename = "./data/buildtables/standard/Welfare.txt";
        }

        String unitFileName = BuildTable.getUnitFilename(Filename);
        SUnit cm = new SUnit(producer, unitFileName, Unit.LIGHT);

        return cm;
    }

}// end RequestCommand class
