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
import java.util.StringTokenizer;

import common.Unit;
import common.util.StringUtils;
import common.util.UnitUtils;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.pilot.SPilot;

public class RequestDonatedCommand implements Command {

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

        /*
         * A command which checks qualifications for a request for a DONATED
         * mech (XP/Cbills/IP, etc) and generates a string which describes the
         * outcome of the request. Note that donated units cost HALF as much as
         * new units in terms of Cbills and influence.
         * 
         * USAGE: /c requestdonated#WEIGHTCLASS#TYPEID
         */

        // load player, faction and defaults
        SPlayer p = CampaignMain.cm.getPlayer(Username);
        SHouse house = p.getMyHouse();
        int weightclass = Unit.LIGHT;
        int type_id = Unit.MEK;
        StringBuilder result = new StringBuilder();

        boolean useBays = CampaignMain.cm.isUsingAdvanceRepair();

        // get the weightclass
        String weightstring = command.nextToken().toUpperCase();
        try {
            weightclass = Integer.parseInt(weightstring);
        } catch (Exception ex) {
            weightclass = Unit.getWeightIDForName(weightstring);
        }

        // get the unit type
        String typestring = command.nextToken();
        try {
            type_id = Integer.parseInt(typestring);
        } catch (Exception ex) {
            type_id = Unit.getTypeIDForName(typestring);
        }

        // tell newbs they cant buy hangar units, but give them a link to
        // request new units.
        if (p.getMyHouse().isNewbieHouse()) {
            result.append("AM:Players in the training faction may not purchase used/donated units; however, they may reset their units.");
            result.append("AM:<br><a href=\"MEKWARS/c request#resetunits\">Click here to request a reset of your units.</a>");
            CampaignMain.cm.toUser(result.toString(), Username, true);
            return;
        }

        if (p.mayAcquireWelfareUnits()) {
            SUnit unit = buildWelfareMek(house.getName());

            SPilot pilot = house.getNewPilot(unit.getType());
            unit.setPilot(pilot);
            p.addUnit(unit, true);
            CampaignMain.cm.toUser("AM:High Command has given you a Mek from its welfare rolls to help you get back on your feet!", Username, true);

            return;
        }

        if (!p.mayUse(weightclass)) {
            result.append("AM:You are not experienced enough to use ");
            result.append(Unit.getWeightClassDesc(weightclass));
            result.append(" units.");
            CampaignMain.cm.toUser(result.toString(), Username, true);
            return;
        }

        if (!p.hasRoomForUnit(type_id, weightclass)) {
            CampaignMain.cm.toUser("AM:Sorry, you already have the maximum number of " + Unit.getWeightClassDesc(weightclass) + " " + Unit.getTypeClassDesc(type_id) + "s", Username);
            return;
        }

        // get the unit from the faction vector. break out if there is no unit
        // to be had.
        SUnit u = house.getEntity(weightclass, type_id);
        if (u == null) {// if getEntity returned null, there is none to give the
            // player
            result.append("AM:There is no unit of the requested weight class/type avaliable");
            CampaignMain.cm.toUser(result.toString(), Username, true);
            return;
        }

        if (!Boolean.parseBoolean(p.getSubFaction().getConfig("CanBuyUsed" + Unit.getWeightClassDesc(weightclass) + Unit.getTypeClassDesc(type_id)))) {
        	
            CampaignMain.cm.toUser("AM:Sorry as a member of " + p.getSubFactionName() + " you are unable to purchase this unit.", Username);
            return;
        }

        // boot the player's request if he has unmaintained units
        if (p.hasUnmaintainedUnit()) {
            result.append("AM:Your faction refuses to assign additional units to you force while existing resources are not being properly maintained!");
            house.addUnit(u, false);// add the retrieved mech back to the pool
            CampaignMain.cm.toUser(result.toString(), Username, true);
            return;
        }// end if(has an unmaintained unit)

        // get money and inf costs.
        int unitCbills = Math.round(Float.parseFloat(house.getConfig("UsedPurchaseCostMulti")) * house.getPriceForUnit(u.getWeightclass(), u.getType()));
        int unitInfluence = Math.round(Float.parseFloat(house.getConfig("UsedPurchaseCostMulti")) * house.getInfluenceForUnit(u.getWeightclass(), u.getType()));
        if (unitCbills < 0) {
            unitCbills = 0;
        }
        if (unitInfluence < 0) {
            unitInfluence = 0;
        }

        if (Boolean.parseBoolean(house.getConfig("UseCalculatedCosts"))) {
            double costMulti = house.getDoubleConfig("UsedPurchaseCostMulti");
            double unitCost = u.getEntity().getCost(false);
            if (unitCost < 1) {
                unitCost = house.getPriceForUnit(u.getWeightclass(), u.getType());
            }
            double costMod = house.getDoubleConfig("CostModifier");

            unitCbills = (int) Math.round(costMulti * unitCost * costMod);
        }
        if (CampaignMain.cm.isUsingAdvanceRepair()) {
            if (!UnitUtils.canStartUp(u.getEntity())) {
                unitCbills = Math.round(unitCbills * Float.parseFloat(house.getConfig("CostModifierToBuyEnginedUnit")));
            } else if (UnitUtils.hasCriticalDamage(u.getEntity())) {
                unitCbills = Math.round(unitCbills * Float.parseFloat(house.getConfig("CostModifierToBuyCritDamagedUnit")));
            } else if (UnitUtils.hasArmorDamage(u.getEntity())) {
                unitCbills = Math.round(unitCbills * Float.parseFloat(house.getConfig("CostModifierToBuyArmorDamagedUnit")));
            }
        }

        // Add penalty if the player is over a sliding limit
        if (p.willHaveHangarPenalty(type_id, weightclass)) {
        	int costPenalty = p.calculateHangarPenaltyForNextPurchase(type_id, weightclass);
        	unitCbills += costPenalty;
        }
        
        if (unitCbills > p.getMoney() || unitInfluence > p.getInfluence()) {
            house.addUnit(u, false);// add the retrieved mech back to the pool
            CampaignMain.cm.toUser("AM:You cannot afford to purchase " + StringUtils.aOrAn(Unit.getWeightClassDesc(u.getWeightclass()), true) + " " + Unit.getTypeClassDesc(u.getType()) + " from the faction bay (Requires " + CampaignMain.cm.moneyOrFluMessage(true, false, unitCbills) + ", " + CampaignMain.cm.moneyOrFluMessage(false, true, unitInfluence) + ").", Username, true);
            return;
        }

        // check to make sure the player has enough support for the unit
        // requested. if not, send hire and buy links.
        int spaceTaken = SUnit.getHangarSpaceRequired(type_id, weightclass, 0, "null", p.getMyHouse());
        if (spaceTaken > p.getFreeBays()) {// if only needs more technicians
            int techCost = p.getTechHiringFee();
            if (useBays) {
                techCost = Integer.parseInt(house.getConfig("CostToBuyNewBay"));
            }

            int numTechs = spaceTaken - p.getFreeBays();
            techCost = techCost * numTechs;
            int totalCost = unitCbills + techCost;

            if (totalCost > p.getMoney()) {
                if (useBays) {
                    result.append("AM:Command will not assign the requested unit to your force unless support is in place; however, you cannot afford to buy the unit *and* purchase bays. Total cost would be ");
                    result.append(CampaignMain.cm.moneyOrFluMessage(true, false, totalCost));
                    result.append(" and you only have ");
                    result.append(+ p.getMoney());
                    result.append(".");
                } else {
                    result.append("AM:Command will not assign the requested unit to your force unless support is in place; however, you cannot afford to buy the unit *and* hire technicians. Total cost would be ");
                    result.append(CampaignMain.cm.moneyOrFluMessage(true, false, totalCost));
                    result.append(" and you only have ");
                    result.append(p.getMoney());
                    result.append(".");
                }
                house.addUnit(u, false);// couldnt afford, so add the retrieved
                // mech back to the pool
                CampaignMain.cm.toUser(result.toString(), Username, true);
                return;
            }

            if (useBays) {
                result.append("AM:Quartermaster command will not release the requested unit to your force unless support resources are in place. You will need to purchase ");
                result.append(numTechs);
                result.append(" more bays (total cost: ");
                result.append(CampaignMain.cm.moneyOrFluMessage(true, true, techCost));
                result.append("). Combined cost of the requested unit and necessary bays is ");
                result.append(CampaignMain.cm.moneyOrFluMessage(true, true, totalCost));
                result.append(" and ");
                result.append(CampaignMain.cm.moneyOrFluMessage(false, true, unitInfluence));
                result.append(".");
                result.append("<br><a href=\"MEKWARS/c hireandrequestused#");
                result.append(numTechs);
                result.append("#");
                result.append(Unit.getWeightClassDesc(weightclass));
                result.append("#");
                result.append(type_id);
                result.append("\">Click here to purchase the bays and purchase the unit.</a>");
            } else {
                result.append("AM:Quartermaster command will not release the requested unit to your force unless support resources are in place. You will need to hire ");
                result.append(numTechs);
                result.append(" more technicians (total tech cost: ");
                result.append(CampaignMain.cm.moneyOrFluMessage(true, true, techCost));
                result.append("). Combined cost of the requested unit and necessary technicians is ");
                result.append( CampaignMain.cm.moneyOrFluMessage(true, true, totalCost));
                result.append(" and ");
                result.append( CampaignMain.cm.moneyOrFluMessage(false, true, unitInfluence));
                result.append(".");
                result.append("<br><a href=\"MEKWARS/c hireandrequestused#");
                result.append(numTechs);
                result.append("#");
                result.append(Unit.getWeightClassDesc(weightclass));
                result.append("#");
                result.append(type_id);
                result.append("\">Click here to hire the technicians and purchase the unit.</a>");
            }

            house.addUnit(u, false);// didnt complete the buy, so add the
            // retrieved mech back to the pool
            CampaignMain.cm.toUser(result.toString(), Username, true);
            return;// break out ...
        }// end if (needsMoreTechs)

        if (u.hasVacantPilot() && (!u.isSinglePilotUnit() || !CampaignMain.cm.getBooleanConfig("AllowPersonalPilotQueues"))) {
            u.setPilot(p.getMyHouse().getNewPilot(type_id));
        }

        p.addUnit(u, true);// if both tests were passed, give the unit
        p.addMoney(-unitCbills);// then take away money
        p.addInfluence(-unitInfluence);// and take away influence

        result.append("AM:You've been granted a ");
        result.append(u.getModelName());
        result.append(". (-");
        result.append(CampaignMain.cm.moneyOrFluMessage(true, false, unitCbills));
        result.append(" / -");
        result.append(CampaignMain.cm.moneyOrFluMessage(false, true, unitInfluence));
        result.append(") ");
        CampaignMain.cm.toUser(result.toString(), Username, true);
        result.setLength(0);
        result.append(p.getName());
        result.append(" bought ");
        result.append(StringUtils.aOrAn(u.getVerboseModelName(), true));
        result.append(" from the faction bay!");
        CampaignMain.cm.doSendHouseMail(house, "NOTE", result.toString());

        // entity removed from SHouse. Send update to effected players
        CampaignMain.cm.doSendToAllOnlinePlayers(house, "HS|" + house.getHSUnitRemovalString(u), false);

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

}// end RequestDonatedCommand