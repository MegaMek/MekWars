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

package mekwars.server.campaign.commands;

import mekwars.server.campaign.CampaignMain;
import server.campaign.pilot.SPilot;


public class RetirePilotCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        if (command.hasMoreElements()) {

            //load the player issuing the command
            server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);

            //try to load the mech whose pilot is being retired
            int mechid = -1;
            try {
                mechid = Integer.parseInt((String) command.nextElement());
            } catch (Exception e) {
                CampaignMain.campaignMain.toUser("AM:Improper format. Try: /c retirepilot#UnitID",
                      Username,
                      true);
                return;
            }

            //get the actual unit, and check for a null
            server.campaign.SUnit m = p.getUnit(mechid);
            if (m == null) {
                CampaignMain.campaignMain.toUser("AM:You do not have unit #" + mechid + ".", Username, true);
                return;
            }

            //see if its even possible to retire pilots
            if (Boolean.parseBoolean(CampaignMain.campaignMain.getConfig("PilotRetirementAllowed")) == false) {
                CampaignMain.campaignMain.toUser("AM:Pilot retirement is not allowed.", Username, true);
                return;
            }

            if (m.hasVacantPilot()) {
                CampaignMain.campaignMain.toUser("AM:This unit does not currently have pilot in it.", Username);
                return;
            }

            //don't let someone retire while active, if unit is in an army
            if (p.getDutyStatus() != server.campaign.SPlayer.STATUS_RESERVE) {

                //check the unit's armies.
                java.util.Enumeration<server.campaign.SArmy> f = p.getArmies().elements();
                while (f.hasMoreElements()) {
                    server.campaign.SArmy currArmy = f.nextElement();
                    if (currArmy.getUnit(m.getId()) != null) {
                        CampaignMain.campaignMain.toUser(
                              "AM:You may not dismiss/retire a pilot while his unit is part of an active army.",
                              Username,
                              true);
                        return;
                    }//end if(army contains the )
                }//end while(more armies to check)
            }//end if(player is active or fighting)


            //check to see if the command was confirmed. used later.
            boolean commandConfirmed = false;
            if (command.hasMoreElements()) {
                if (command.nextToken().equals("CONFIRM")) {commandConfirmed = true;}
            }

            /*
             * two possible paths for retirement.
             *
             * 1st - pilot has skills which add to (or are below) the
             * free retirement threshold. If the command is confirmed,
             * retire him. If not, send a confirmation link.
             *
             * 2nd - pilot is under the free threshold. check to see if
             * early retirement is possible, breaking out if it isnt, sending
             * cost info and a confirmed re-issue if not (and not confirmed).
             */

            int totalSkill = m.getPilot().getGunnery() + m.getPilot().getPiloting();
            int skillForFree = CampaignMain.campaignMain.getIntegerConfig("TotalSkillForFreeRetirement");
            int retirementCost = -1;//used later
            if (totalSkill <= skillForFree) {

                //qualify for free.
                if (!commandConfirmed) {//send confirm link
                    String toReturn = "AM:<br>Are you sure you want to retire " +
                                            m.getPilot().getName() +
                                            "?<br><a href=\"MEKWARS/c retirepilot#" +
                                            mechid +
                                            "#CONFIRM\">Click here to confirm the retirment order.</a>";
                    CampaignMain.campaignMain.toUser(toReturn, Username, true);
                    return;
                }

            } else { //didnt meet free requiremnt.

                //determine costs
                int numLevelsNeeded = totalSkill - skillForFree;
                int costPerLevelNeeded = CampaignMain.campaignMain.getIntegerConfig("CostPerLevelToRetireEarly");
                retirementCost = costPerLevelNeeded * numLevelsNeeded;

                //check to see if early is allowed. if not, break out.
                boolean earlyAllowed = Boolean.parseBoolean(CampaignMain.campaignMain.getConfig(
                      "EarlyRetirementAllowed"));
                if (!earlyAllowed) {
                    String toReturn = m.getPilot().getName() + " may not retire yet. He needs to " +
                                            "level up " + numLevelsNeeded + " more time";
                    if (numLevelsNeeded > 1) {toReturn += "s";}
                    toReturn += " before he can call it quits.";
                    CampaignMain.campaignMain.toUser(toReturn, Username, true);
                    return;
                }

                if (p.getMoney() < retirementCost) {
                    CampaignMain.campaignMain.toUser("AM:You don't have enough money to dismiss " +
                                                           m.getPilot().getName() +
                                                           ". Bribing HQ to reassign him would cost " +
                                                           CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                                 true,
                                                                 retirementCost) +
                                                           ".", Username, true);
                    return;
                }

                //else (early retirement allowed).
                if (!commandConfirmed) {
                    //not confirmed. send a link.
                    String toReturn = "AM:You can get " +
                                            m.getPilot().getName() +
                                            " transfered out of your unit, but you'll have " +
                                            "to bribe someone at HQ. Are you willing to spend " +
                                            CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                  true,
                                                  retirementCost) +
                                            " to make an \"accident\" happen?<br>" +
                                            "<a href=\"MEKWARS/c retirepilot#" +
                                            mechid +
                                            "#CONFIRM\">Click here to pay the " +
                                            CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                  true,
                                                  retirementCost) +
                                            ".</a>";
                    CampaignMain.campaignMain.toUser(toReturn, Username, true);
                    return;
                }

            }//end (couldnt be retired for free)

            /*
             * Every break passed and the command was confirmed. We
             * can go about the actual retirement now.
             */

            // Figure out if the pilot is going to take his unit with him
            Boolean retireMechToo = false;
            String unitName = m.getModelName();
            if (!CampaignMain.campaignMain.getBooleanConfig("AllowPersonalPilotQueues") && (
                  CampaignMain.campaignMain.getIntegerConfig("RetiredPilotTakesMechChance") > 0)) {
                if (CampaignMain.campaignMain.getRandomNumber(100) <=
                          CampaignMain.campaignMain.getIntegerConfig("RetiredPilotTakesMechChance")) {
                    retireMechToo = true;
                }
            }

            //set up the message for the player
            String toReturn = "";
            if (retirementCost <= 0) {
                toReturn = m.getPilot().getName() +
                                 " retired, went home to his family, and lived happily ever after." +
                                 (retireMechToo ?
                                        (" Unfortunately, he took his family's " + unitName + " with him.") :
                                        "");

            } else {
                toReturn = "AM:You dismissed " +
                                 m.getPilot().getName() +
                                 " (-" +
                                 CampaignMain.campaignMain.moneyOrFluMessage(true, true, retirementCost) +
                                 ")." +
                                 (retireMechToo ?
                                        (" Unfortunately, he took his family's " + unitName + " with him.") :
                                        "");
            }

            //take money away
            if (retirementCost > 0) {p.addMoney(-retirementCost);}

            //tell the user about the retirement
            CampaignMain.campaignMain.toUser(toReturn, Username, true);

            //now, handle the pilot. if PPQs are in use leave the unit vacant,
            //otherwise add a new pilot from the faction queue.
            boolean allowPPQs = Boolean.parseBoolean(CampaignMain.campaignMain.getConfig(
                  "AllowPersonalPilotQueues"));
            if (allowPPQs && m.isSinglePilotUnit()) {
                SPilot pilot = new SPilot("Vacant", 99, 99);
                m.setPilot(pilot);
            } else {//add a new pilot from the queue
                m.setPilot(p.getMyHouse().getNewPilot(m.getType()));
                m.setExperience(0);
            }

            //continue normally. update unit and its armies, etc.
            CampaignMain.campaignMain.toUser("PL|UU|" + m.getId() + "|" + m.toString(true), Username, false);

            java.util.Enumeration<server.campaign.SArmy> f = p.getArmies().elements();
            while (f.hasMoreElements()) {
                server.campaign.SArmy currArmy = f.nextElement();
                if (currArmy.getUnit(m.getId()) != null) {
                    currArmy.setBV(0);//not null so recalc BV of the army
                    CampaignMain.campaignMain.toUser("PL|SAD|" + currArmy.toString(true, "%"), Username, false);
                    CampaignMain.campaignMain.getOpsManager()
                          .checkOperations(currArmy, true);//update legal operations
                }//end if(army contains the )
            }//end while(more armies to check)

            if (retireMechToo) {
                p.removeUnit(m.getId(), true);
            }
        }
    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}
