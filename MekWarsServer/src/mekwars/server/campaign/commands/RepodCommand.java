/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 *
 * Original Author: Dave Poole
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

import common.House;
import common.Unit;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.Entity;
import megamek.common.Mech;
import mekwars.server.campaign.CampaignMain;
import server.campaign.pilot.SPilot;

/*
 * Jun 10/04 - Dave Poole
 *
 * Updated creation of new Sunit to call new overloaded SUnit constructor to fix problem of
 * incrememnting the unitID needlessly.
 *
 */

public class RepodCommand implements Command {

    int accessLevel = 0;
    String syntax = "";
    private boolean global = false;

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
        global = false;

        if (command.hasMoreElements()) {

            // vars to use during processing
            server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
            int unitid = Integer.parseInt(command.nextToken());
            server.campaign.SUnit m = p.getUnit(unitid);
            server.campaign.SHouse h = p.getHouseFightingFor();
            String target = "<none>";

            // blow out if player has a null unit
            if (m == null) {
                CampaignMain.campaignMain.toUser("AM:You do not have a unit with ID# " + unitid + ".",
                      Username,
                      true);
                return;
            }

            if (p.mayAcquireWelfareUnits()) {
                CampaignMain.campaignMain.toUser("AM:You may not repod your units while you are on welfare!",
                      Username,
                      true);
                return;
            }

            if (UnitUtils.hasArmorDamage(m.getEntity()) || UnitUtils.hasCriticalDamage(m.getEntity())) {
                CampaignMain.campaignMain.toUser(
                      "AM:This unit is currently damaged and cannot be repodded until you repair it.",
                      Username,
                      true);
                return;
            }

            // decide if we're making up an Omni list, or trying to match
            if (command.hasMoreTokens()) {
                target = command.nextToken();
                if (target.equals("GLOBAL")) {
                    global = true;
                    if (command.hasMoreTokens()) {
                        target = command.nextToken();
                    } else {
                        target = "<none>";
                    }
                }
            }

            // MWLogger.errLog("repod target "+target+" Global: "+global);

            // return if the unit which is targetted is not an omni
            if (!m.isOmni()) {
                CampaignMain.campaignMain.toUser("AM:Your " + m.getVerboseModelName() + " is not an Omni.",
                      Username,
                      true);
                return;
            }

            // don't allow a player to repod a unit which is in an army
            // NOTE: repodding while in armies changes BV, leading to very funky
            // .checkOperations() circumstances (add, then remove, need to know
            // which armies are impacted, etc).
            if (p.getAmountOfTimesUnitExistsInArmies(m.getId()) > 0) {
                CampaignMain.campaignMain.toUser("AM:You may not repod a unit while it is in an army.",
                      Username,
                      true);
                return;
            }

            // use /c repod#ID#Target Unit, or /c repod#ID#RANDOM
            Entity mEnt = m.getEntity();
            String targetChassis = mEnt.getChassis();

            // scan the owner's default build list for pod types.
            java.util.Vector<server.campaign.SUnitFactory> possible = new java.util.Vector<server.campaign.SUnitFactory>(
                  1,
                  1);

            // now, to find the build tables, let's make a vector of them...
            java.util.Vector<String> tables = new java.util.Vector<String>(1, 1);
            if (global) {
                java.util.Iterator<House> Houses = CampaignMain.campaignMain.getData().getAllHouses().iterator();
                String fileName = "";
                String timeZone = h.getConfig("RewardsRepodFolder");
                while (Houses.hasNext()) {
                    server.campaign.SHouse faction = (server.campaign.SHouse) Houses.next();

                    fileName = server.campaign.BuildTable.getFileName(faction.getName(),
                          Unit.getWeightClassDesc(m.getWeightclass()),
                          timeZone,
                          m.getType());
                    // MWLogger.errLog("File: "+fileName);

                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
                if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                    fileName = server.campaign.BuildTable.getFileName("Common",
                          Unit.getWeightClassDesc(m.getWeightclass()),
                          timeZone,
                          m.getType());
                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
            } else if (!Boolean.parseBoolean(h.getConfig("RepodUsesFactory"))) {
                String fileName = "";
                String timeZone = h.getConfig("NoFactoryRepodFolder");
                // MWLogger.errLog("TimeZone: "+timeZone);
                fileName = server.campaign.BuildTable.getFileName(h.getName(),
                      Unit.getWeightClassDesc(m.getWeightclass()),
                      timeZone,
                      m.getType());
                // MWLogger.errLog("File: "+fileName);

                if (!tables.contains(fileName)) {
                    tables.add(fileName);
                }
                if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                    fileName = server.campaign.BuildTable.getFileName("Common",
                          Unit.getWeightClassDesc(m.getWeightclass()),
                          timeZone,
                          m.getType());
                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
            } else {

                // scan the owner's default build list for pod types.
                possible = p.getMyHouse().getPossibleFactoryForProduction(m.getType(), m.getWeightclass(), false);
                for (server.campaign.SUnitFactory working : possible) {

                    String fileName = server.campaign.BuildTable.getFileName(working.getFounder(),
                          Unit.getWeightClassDesc(m.getWeightclass()),
                          working.getBuildTableFolder(),
                          m.getType());

                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }

                    if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                        fileName = server.campaign.BuildTable.getFileName("Common",
                              Unit.getWeightClassDesc(m.getWeightclass()),
                              working.getBuildTableFolder(),
                              m.getType());
                        if (!tables.contains(fileName)) {
                            tables.add(fileName);
                        }
                    }
                }
            }
            // check the repod mode against server settings
            if (target.equals("<none>")) {
                // do nothing
            } else if (target.equals("RANDOM")) {
                if (!Boolean.parseBoolean(h.getConfig("RandomRepodAllowed"))) {
                    CampaignMain.campaignMain.toUser("Random repodding is not allowed.", Username, true);
                    return;
                }
            } else if (Boolean.parseBoolean(h.getConfig("RandomRepodOnly"))) {// there
                // is
                // a
                // real
                // target
                CampaignMain.campaignMain.toUser("Only random repods are allowed.", Username, true);
                return;
            }

            // now, fetch the actual lines from the files
            String result = "";
            java.util.Vector<String> variants = new java.util.Vector<String>(1, 1);
            // variants.add(mEnt.getModel());
            int i = tables.size();

            // MWLogger.errLog("table size is "+i);
            if (i < 1) {
                CampaignMain.campaignMain.toUser("AM:Repod Failed: No acceptable factory currently available",
                      Username,
                      true);
                return;
            }

            while (i > 0) {

                i--;
                String prodFile = tables.elementAt(i);

                try {

                    java.io.FileInputStream fis = new java.io.FileInputStream(prodFile);
                    java.io.BufferedReader dis = new java.io.BufferedReader(new java.io.InputStreamReader(fis));

                    while (dis.ready()) {

                        String l = dis.readLine();
                        java.util.StringTokenizer ST = new java.util.StringTokenizer(l);

                        if (ST.hasMoreElements()) {

                            // although we don't use it here, we need to eat the
                            // weight
                            ST.nextElement();

                            String Filename = "";
                            while (ST.hasMoreElements()) {
                                Filename += (String) ST.nextElement();
                                if (ST.hasMoreElements()) {
                                    Filename += " ";
                                }
                            }

                            // compare this File name to the chassis type of our
                            // Omni
                            if (Filename.toLowerCase().startsWith(targetChassis.toLowerCase())) {

                                // now, check actual mech in this file, to see
                                // if it is Omni & same Chassis
                                server.campaign.SUnit cm = new server.campaign.SUnit(unitid, m.getProducer(), Filename);

                                Entity cme = cm.getEntity();
                                String chassis = cme.getChassis();
                                if (cm.isOmni() && chassis.equalsIgnoreCase(targetChassis)) {

                                    // good Omni possibility
                                    if (target.equals("<none>") || target.equals("RANDOM")) {

                                        if (target.equals("RANDOM") && cm.getModelName().equals(m.getModelName())) {
                                            continue;
                                        }

                                        // MWLogger.errLog("FileName: "+Filename+" Model: "+model);
                                        if (!variants.contains(Filename)) {
                                            variants.add(Filename);
                                            String repodMoneyCfg = "RepodCost" +
                                                                         Unit.getWeightClassDesc(cm.getWeightclass());
                                            String repodInfluCfg = "RepodFlu" +
                                                                         Unit.getWeightClassDesc(cm.getWeightclass());
                                            String repodCompCfg = "RepodComp" +
                                                                        Unit.getWeightClassDesc(cm.getWeightclass());

                                            int repodMoneyMod = Integer.parseInt(h.getConfig(repodMoneyCfg));
                                            int repodFluMod = Integer.parseInt(h.getConfig(repodInfluCfg));
                                            int repodCompMod = Integer.parseInt(h.getConfig(repodCompCfg));

                                            if (CampaignMain.campaignMain.getOmniVariantMods().get(Filename) !=
                                                      null) {
                                                String mods = CampaignMain.campaignMain.getOmniVariantMods()
                                                                    .get(Filename);
                                                java.util.StringTokenizer modlist = new java.util.StringTokenizer(mods,
                                                      "$");
                                                repodMoneyMod += Integer.parseInt(modlist.nextToken());
                                                repodCompMod += Integer.parseInt(modlist.nextToken());
                                                repodFluMod += Integer.parseInt(modlist.nextToken());

                                            }

                                            result += Filename +
                                                            "#" +
                                                            repodMoneyMod +
                                                            "$" +
                                                            repodCompMod +
                                                            "$" +
                                                            repodFluMod +
                                                            "#";
                                        }
                                    } else {
                                        if (target.equalsIgnoreCase(Filename)) {
                                            createOmni(m, Filename, m.getId(), p, possible, false);
                                            return;
                                        }
                                    }

                                }// end if(cm.isOmni() &&
                                // chassis.equals(targetChassis)
                            }// end if (Filename.startsWith(targetChassis))
                        } else {
                            MWLogger.mainLog("File " + prodFile + " has a problem with line:" + l);
                        }
                    }// end dis.ready()
                    dis.close();
                    fis.close();
                } catch (java.io.FileNotFoundException ex) {
                    MWLogger.mainLog("File " + prodFile + " was not Found");
                } catch (java.io.IOException ex) {
                    MWLogger.mainLog("File " + prodFile + " had an I/O error");
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.mainLog("File " + prodFile + " has a problem");
                } finally {
                    // nothing
                }
            }// tables -> variants iterator

            if (target.equals("RANDOM")) {

                int size = variants.size();
                if (size <= 0) {
                    CampaignMain.campaignMain.toUser("AM:No random targets available for " +
                                                           m.getModelName() +
                                                           ".", Username, true);
                    return;
                }
                if (size == 1) {
                    String Filename = variants.elementAt(0);
                    createOmni(m, Filename, m.getId(), p, possible, true);
                    return;
                }

                int number = CampaignMain.campaignMain.getRandomNumber(size);
                String Filename = variants.elementAt(number);
                createOmni(m, Filename, m.getId(), p, possible, true);
                return;
            }

            // didnt actually repod. send a RUD command to the client.
            if (global) {
                result += "#GLOBAL";
            }
            CampaignMain.campaignMain.toUser("RUD|" + unitid + "|" + result, Username, false);
        }

        // CampaignMain.cm.toUser("Usage: <CODE>/c repod#{unitid}#{New Variant}</CODE>",
        // Username, true);
        return;
    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    public String getSyntax() {
        return syntax;
    }

    private void createOmni(
          server.campaign.SUnit m, String Filename, int unitid, server.campaign.SPlayer p,
          java.util.Vector<server.campaign.SUnitFactory> possible, boolean random) {

        // MWLogger.errLog("Filename "+Filename);
        server.campaign.SHouse h = p.getHouseFightingFor();
        String Username = p.getName();

        // Optional Repod costing
        // Check server parameter to see if it is set to true
        // If so, lookup the fixed money and influence costs and apply them to
        // the player
        // D.Poole - Jan 23/2004

        int influenceCost = 0;
        int moneyCost = 0;
        int compCost = 0;
        int refreshTime = 0;
        int rpCost = Integer.parseInt(h.getConfig("GlobalRepodWithRPCost"));

        if (random) {
            rpCost /= 2;
        }

        // make sure a vaild unit is select for the repod first.
        server.campaign.SUnit cm = new server.campaign.SUnit(unitid, m.getProducer(), Filename);
        if (cm.getModelName().equals("OMG-UR-FD")) {
            CampaignMain.campaignMain.toUser("AM:Invalid repod format try again!", Username, true);
            return;
        }

        if (!global) {

            String needPartsList = p.getUnitParts().canRepodUnit(m.getEntity(), cm.getEntity()).trim();
            if (CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair") && (needPartsList.length() > 0)) {
                CampaignMain.campaignMain.toUser("You do not have enough parts to repod your " +
                                                       m.getModelName() +
                                                       " to " +
                                                       cm.getModelName() +
                                                       "<br> you need the following parts:<br>" +
                                                       needPartsList, Username);
                return;
            }

            String repodMoneyCfg = "RepodCost" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodInfluCfg = "RepodFlu" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodCompCfg = "RepodComp" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodRefreshCfg = "RepodRefreshTime" + Unit.getWeightClassDesc(m.getWeightclass());
            int repodMoneyMod = 0, repodCompMod = 0, repodFluMod = 0;

            if (CampaignMain.campaignMain.getOmniVariantMods().get(cm.getModelName()) != null) {
                String mods = CampaignMain.campaignMain.getOmniVariantMods().get(cm.getModelName());
                java.util.StringTokenizer modList = new java.util.StringTokenizer(mods, "$");
                repodMoneyMod = Integer.parseInt(modList.nextToken());
                repodCompMod = Integer.parseInt(modList.nextToken());
                repodFluMod = Integer.parseInt(modList.nextToken());
            }

            int repodRandomMod = Integer.parseInt(h.getConfig("RepodRandomMod"));

            if (Boolean.parseBoolean(h.getConfig("DoesRepodCost"))) {
                // deduct cost
                moneyCost = Integer.parseInt(h.getConfig(repodMoneyCfg)) + repodMoneyMod;
                if (random) {
                    moneyCost = (moneyCost * repodRandomMod) / 100;
                }

                if (p.getMoney() < moneyCost) {
                    CampaignMain.campaignMain.toUser(
                          "AM:You do not have enough money to repod this unit! It will cost " +
                                CampaignMain.campaignMain.moneyOrFluMessage(true, false, moneyCost) +
                                " to repod this unit",
                          Username,
                          true);
                    return;
                }

                // deduct influence
                influenceCost = Integer.parseInt(h.getConfig(repodInfluCfg)) + repodFluMod;
                if (random) {
                    influenceCost = (influenceCost * repodRandomMod) / 100;
                }

                if (p.getInfluence() < influenceCost) {
                    CampaignMain.campaignMain.toUser(
                          "AM:You do not have enough influence to repod this unit! It will cost " +
                                CampaignMain.campaignMain.moneyOrFluMessage(false, false, influenceCost) +
                                " to repod this unit!",
                          Username,
                          true);
                    return;
                }

                if (Boolean.parseBoolean(h.getConfig("RepodUsesComp"))) {
                    compCost = Integer.parseInt(h.getConfig(repodCompCfg)) + repodCompMod;
                    if (random) {
                        compCost = (compCost * repodRandomMod) / 100;
                    }

                    if (h.getPP(m.getWeightclass(), m.getType()) < compCost) {
                        CampaignMain.campaignMain.toUser(
                              "AM:Your faction doesn't have enough components to repod this unit. You need " +
                                    compCost +
                                    " components to repod.",
                              Username,
                              true);
                        return;
                    }
                }

                StringBuilder hsUpdates = new StringBuilder();
                if (Boolean.parseBoolean(h.getConfig("RepodUsesFactory"))) {
                    server.campaign.SUnitFactory working = possible.elementAt(0);
                    refreshTime = Integer.parseInt(h.getConfig(repodRefreshCfg));
                    if (random) {
                        refreshTime = (refreshTime * repodRandomMod) / 100;
                    }
                    hsUpdates.append(working.addRefresh(refreshTime, false));
                }

                hsUpdates.append(h.addPP(m.getWeightclass(), m.getType(), -compCost, false));
                if (hsUpdates.length() > 0) {
                    CampaignMain.campaignMain.doSendToAllOnlinePlayers(h, "HS|" + hsUpdates.toString(), false);
                }

                p.addMoney(-moneyCost);
                p.addInfluence(-influenceCost);

            }// - end Repod costing
        } else {
            if (p.getReward() < rpCost) {
                CampaignMain.campaignMain.toUser("AM:You do not have enough " +
                                                       CampaignMain.campaignMain.getConfig("RPLongName") +
                                                       " to repod this unit!", Username, true);
                return;
            }
            p.addReward(-rpCost);
        }

        cm.setPilot((SPilot) m.getPilot());
        cm.setExperience(m.getExperience());

        Entity entity = p.getUnit(unitid).getEntity();

        // Take the parts for it.
        if (CampaignMain.campaignMain.getBooleanConfig("UsePartsRepair")) {
            p.getUnitParts().repodUnit(entity, cm.getEntity());
            CampaignMain.campaignMain.toUser("PL|RPPC|" + p.getUnitParts().toString(), Username, false);
        }

        // remove the old unit *before* adding the new one, since they share a
        // unit id.
        if (cm.getType() == Unit.MEK) {
            ((Mech) cm.getEntity()).setAutoEject(((Mech) entity).isAutoEject());
        }

        cm.getEntity().setExternalSearchlight(entity.hasSearchlight());
        cm.getEntity().setSearchlightState(entity.isUsingSearchlight());
        cm.setWeightclass(m.getWeightclass());
        cm.setType(m.getType());
        // since unit cannot be in armies, no checkOperations on remove
        p.removeUnit(unitid, false);

        // and the unit and send informational messages to player.
        p.addUnit(cm, true);
        CampaignMain.campaignMain.toUser("AM:Your " +
                                               m.getVerboseModelName() +
                                               " is now " +
                                               StringUtils.aOrAn(cm.getVerboseModelName(), true) +
                                               ".", Username, true);
        if (!global) {
            if (Boolean.parseBoolean(h.getConfig("RepodUsesComp"))) {
                CampaignMain.campaignMain.toUser("AM:Repodding cost " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             false,
                                                             moneyCost) +
                                                       " and " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                             true,
                                                             influenceCost) +
                                                       " " +
                                                       compCost +
                                                       " Components.", Username, true);
            } else {
                CampaignMain.campaignMain.toUser("AM:Repodding cost " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             false,
                                                             moneyCost) +
                                                       " and " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(false,
                                                             true,
                                                             influenceCost) +
                                                       ".", Username, true);
            }
        } else {
            CampaignMain.campaignMain.toUser("AM:Repodding cost " +
                                                   rpCost +
                                                   " " +
                                                   CampaignMain.campaignMain.getConfig("RPLongName") +
                                                   ".", Username, true);
        }

        return;// break out of it all
    }
}
