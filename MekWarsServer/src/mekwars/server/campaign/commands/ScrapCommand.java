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

/*
 * Created on 10.01.2004
 *
 */
package mekwars.server.campaign.commands;

import common.Unit;
import common.util.StringUtils;
import common.util.UnitUtils;
import server.campaign.pilot.SPilot;

/**
 * @author Helge Richter
 *
 */
public class ScrapCommand implements Command {

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

        int scrapsAllowed = Integer.parseInt(house.getConfig("ScrapsAllowed"));
        if (scrapsAllowed <= 0) {
            server.campaign.CampaignMain.cm.toUser("AM:Scrapping is not allowed on this server.", Username, true);
            return;
        }

        if (house.isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:SOL players may not Sell, Scrap or Donate their units!",
                  Username,
                  true);
            return;
        }

        if (p.mayAcquireWelfareUnits()) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not scrap any of your units while you are on welfare.",
                  Username,
                  true);
            return;
        }

        int mechid = -1;
        try {
            mechid = Integer.parseInt((String) command.nextElement());
        } catch (Exception e) {
            server.campaign.CampaignMain.cm.toUser("AM:Formatting error. Try: /c scrap#ID Number", Username, true);
            return;
        }

        String strConfirm = "";
        if (command.hasMoreTokens()) {
            strConfirm = command.nextToken();
        }

        server.campaign.SUnit m = p.getUnit(mechid);
        if (m == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Could not find a unit with the given ID.", Username, true);
            return;
        }


        if (UnitUtils.isRepairing(m.getEntity())) {
            server.campaign.CampaignMain.cm.toUser(
                  "AM:This unit is currently being repaired. You cannot scrap it until the repairs are complete!",
                  Username,
                  true);
            return;
        }

        if (m.getStatus() == Unit.STATUS_FORSALE) {
            server.campaign.CampaignMain.cm.toUser("AM:Units that are for sale on the Market may not be scrapped.",
                  Username,
                  true);
            return;
        }

        if (m.isChristmasUnit() && !server.campaign.CampaignMain.cm.getBooleanConfig("Christmas_AllowScrap")) {
            server.campaign.CampaignMain.cm.toUser("AM:Scrapping a Christmas gift?  Bad form.", Username);
            return;
        }

        if (p.getAmountOfTimesUnitExistsInArmies(mechid) > 0 &&
                  p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not scrap units which are in active armies.",
                  Username,
                  true);
            return;
        }//end (unit is in armies and player is active)

        if (p.isUnitInLockedArmy(m.getId())) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not scrap units which are in fighting armies.",
                  Username,
                  true);
            return;
        }

        //If he has not scrapped this tick and Scrapping is allowed, OR if the entity was salvaged recently
        if (p.getScrapsThisTick() >= scrapsAllowed && m.getScrappableFor() <= 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You may only scrap " + scrapsAllowed + " unit(s) per tick.",
                  Username,
                  true);
            return;
        }

        //Determine scrap cost multiplier
        float costMulti = 0;
        if (!server.campaign.CampaignMain.cm.isUsingAdvanceRepair() || m.getType() != Unit.MEK) {
            costMulti = Float.parseFloat(house.getConfig("ScrapCostMultiplier"));
        } else if (UnitUtils.getNumberOfDamagedEngineCrits(m.getEntity()) >= 3) {
            costMulti = Float.parseFloat(house.getConfig("CostToScrapEngined"));
        } else if (UnitUtils.hasCriticalDamage(m.getEntity())) {
            costMulti = Float.parseFloat(house.getConfig("CostToScrapCriticallyDamaged"));
        } else if (UnitUtils.hasArmorDamage(m.getEntity())) {
            costMulti = Float.parseFloat(house.getConfig("CostToScrapOnlArmorDamage"));
        } else {costMulti = Float.parseFloat(house.getConfig("ScrapCostMultiplier"));}

        //Now that we have the multimpliers, determine how much the scarp costs (or gives)
        int moneyToScrap = Math.round(p.getMyHouse().getPriceForUnit(m.getWeightclass(), m.getType()) * costMulti);
        int infToScrap = (int) (p.getMyHouse().getInfluenceForUnit(m.getWeightclass(), m.getType()) * costMulti);

        //Allow negative monetary costs (give money back), but don't allow scrapping to grant flu.
        if (infToScrap < 0) {infToScrap = 0;}

        //Check to ensure player can afford the scrap
        if (p.getMoney() < moneyToScrap || p.getInfluence() < infToScrap && m.getScrappableFor() < 0) {
            server.campaign.CampaignMain.cm.toUser("AM:You cannot afford to scrap this unit. You need " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               moneyToScrap) +
                                                         " and " +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                               true,
                                                               infToScrap) +
                                                         ".", Username, true);
            return;
        }

        //Give the player the amount the unit can be scrapped for (post-game), or add/deduct the standard cost
        if (m.getScrappableFor() >= 0) {
            p.addMoney(m.getScrappableFor());
            server.campaign.CampaignMain.cm.toUser("AM:You scrapped the " +
                                                         m.getModelName() +
                                                         " (" +
                                                         server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                               true,
                                                               m.getScrappableFor(),
                                                               true) +
                                                         ").", Username, true);
        } else {
            // it was intentional to allow the post-game scrap to process without a confirm... you don't want the guy getting a message "scrap for +1 cbill" then getting "scrapped for -1800cb" because of the time lag
            if (!strConfirm.equals("CONFIRM")) {
                String result = "AM:Quartermaster command will charge you " +
                                      server.campaign.CampaignMain.cm.moneyOrFluMessage(true, false, moneyToScrap) +
                                      " to scrap #" +
                                      m.getId() +
                                      " " +
                                      m.getModelName() +
                                      ".";
                result += "<br><a href=\"MEKWARS/c scrap#" + m.getId() + "#CONFIRM";
                result += "\">Click here to scrap the unit.</a>";
                server.campaign.CampaignMain.cm.toUser(result, Username, true);
                return;
            } else {
                p.addMoney(-moneyToScrap);
                p.addInfluence(-infToScrap);
                p.addScrapThisTick();
                server.campaign.CampaignMain.cm.toUser("AM:You scrapped the " +
                                                             m.getModelName() +
                                                             " (" +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   true,
                                                                   -moneyToScrap,
                                                                   true) +
                                                             ", " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                                   true,
                                                                   -infToScrap,
                                                                   true) +
                                                             ").", Username, true);
            }
        }

        //notify house and, if needed, send warning to mod channel
        server.campaign.CampaignMain.cm.doSendHouseMail(p.getMyHouse(),
              "NOTE",
              p.getName() + " scrapped " + StringUtils.aOrAn(m.getVerboseModelName(), true) + ".");
        if (p.mayAcquireWelfareUnits()) {
            server.campaign.CampaignMain.cm.doSendModMail("NOTE",
                  Username + " scrapped a unit and sent himself into welfare.");
        }

        //do the actual remove last, so the checkops show under the scrap string
        p.removeUnit(mechid, true);

        //if the player has an ops scrap thread running, try to remove this unit
        server.campaign.operations.OpsScrapThread scrapT = server.campaign.CampaignMain.cm.getOpsManager()
                                                                 .getScrapThreads()
                                                                 .get(p.getName().toLowerCase());
        if (scrapT != null) {scrapT.scrapUnit(mechid);}

        //if PPQ's are on the pilot goes to the players barraks - no free getting rid of pilots.
        if (Boolean.parseBoolean(house.getConfig("AllowPersonalPilotQueues"))
                  && !m.hasVacantPilot()
                  && m.isSinglePilotUnit()) {
            SPilot pilot = (SPilot) m.getPilot();
            p.getPersonalPilotQueue().addPilot(m.getPilot(), m.getWeightclass());
            server.campaign.CampaignMain.cm.toUser("PL|AP2PPQ|" +
                                                         m.getType() +
                                                         "|" +
                                                         m.getWeightclass() +
                                                         "|" +
                                                         pilot.toFileFormat("#", true), Username, false);
            server.campaign.CampaignMain.cm.toUser(pilot.getName() + " was moved to your barracks.", Username, true);
            p.getPersonalPilotQueue().checkQueueAndWarn(p.getName(), m.getType(), m.getWeightclass());

        } else {p.getMyHouse().addDispossessedPilot(m, false);}

        server.campaign.CampaignMain.cm.addMechStat(m.getUnitFilename(), m.getWeightclass(), 0, 0, 1);

        //add PP to the faction for the scrapped unit. 1/4th of original components.
        int initialPP = p.getMyHouse().getPPCost(m.getWeightclass(), m.getType());
        p.getMyHouse().addPP(m.getWeightclass(), m.getType(), initialPP / 4, true);
    }//end process()

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

}//end ScrapCommand
