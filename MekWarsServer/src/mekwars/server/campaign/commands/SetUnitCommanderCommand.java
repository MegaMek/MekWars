/*
 * MekWars - Copyright (C) 2007
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

/*
 * Created on 08.22.2007
 *
 */
package mekwars.server.campaign.commands;

import common.Unit;
import megamek.common.VTOL;

/**
 * @author Jason Tighe
 *
 */
public class SetUnitCommanderCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                             userLevel +
                                                             ". Required: " +
                                                             accessLevel +
                                                             ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
        server.campaign.SHouse house = p.getMyHouse();
        int mechid = -1;
        int armyid = -1;
        boolean commander = false;

        try {
            mechid = Integer.parseInt(command.nextToken());
            armyid = Integer.parseInt(command.nextToken());
            commander = Boolean.parseBoolean(command.nextToken());

        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Incorrect syntax. Try: /setUnitCommander unit ID#army ID#true/false",
                  Username,
                  true);
            return;
        }

        server.campaign.SUnit m = p.getUnit(mechid);
        server.campaign.SArmy army = p.getArmy(armyid);

        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find a unit with the given ID.", Username, true);
            return;
        }

        if (m.getStatus() == Unit.STATUS_FORSALE) {
            server.campaign.CampaignMain.cm.toUser("AM:Units that are for sale on the Market.", Username, true);
            return;
        }

        if (m.getPilot() == null || m.getPilot().getName().equalsIgnoreCase("vacant")) {
            server.campaign.CampaignMain.cm.toUser("AM:This unit does not have a pilot to be a commander for!",
                  Username);
            return;
        }

        if (!house.getBooleanConfig("allowUnitCommander" + Unit.getTypeClassDesc(m.getType()))) {
            server.campaign.CampaignMain.cm.toUser(Unit.getTypeClassDesc(m.getType()) +
                                                         " units are not allowed to be set as unit commanders!",
                  Username);
            return;
        }

        if (!house.getBooleanConfig("allowUnitCommanderVTOL") && m.getEntity() instanceof VTOL) {
            server.campaign.CampaignMain.cm.toUser("AM:VTOL units are not allowed to be set as unit commanders!",
                  Username);
            return;
        }

        if (m.getEntity().isOffBoard()) {
            server.campaign.CampaignMain.cm.toUser("AM:Off board units are not allowed to be set as unit commanders!",
                  Username);
            return;
        }

        if (p.getAmountOfTimesUnitExistsInArmies(m.getId()) < 1) {
            server.campaign.CampaignMain.cm.toUser("AM:the " + m.getModelName() + " is not in any armies!", Username);
            return;
        }

        if (army.isCommander(m.getId()) && commander) {
            server.campaign.CampaignMain.cm.toUser("AM:" +
                                                         m.getModelName() +
                                                         " is already a unit commander for this army!", Username);
            return;
        }
        //start Baruk Khazad!  20151108b
        boolean isInArmy = false;
        for (server.campaign.SArmy currA : p.getArmies()) {
            if (currA.isUnitInArmy(m) && !currA.isDisabled()) {
                isInArmy = true;
                break;
            }
        }
        if (isInArmy && p.getDutyStatus() != server.campaign.SPlayer.STATUS_RESERVE) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:Your army is on patrol or fighting and needs to return to base first.",
                  Username,
                  true);
            return;
        }
        //end Baruk Khazad!  20151108b


        p.setSave();

        if (commander) {
            army.addCommander(m.getId());
            server.campaign.CampaignMain.cm.toUser("AM:Unit #" + m.getId() + " has been set as unit commander",
                  Username);
        } else {
            server.campaign.CampaignMain.cm.toUser("AM:Unit #" + m.getId() + " has been removed as unit commander",
                  Username);
            army.removeCommander(m.getId());
        }
        server.campaign.CampaignMain.cm.toUser("PL|SAD|" + army.toString(true, "%"), Username, false);
        server.campaign.CampaignMain.cm.getOpsManager().checkOperations(army, true);

    }// end process()

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

}// end ScrapCommand
