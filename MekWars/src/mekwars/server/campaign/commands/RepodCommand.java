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

package server.campaign.commands;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import common.House;
import common.Unit;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.Entity;
import megamek.common.Mech;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
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
        global = false;

        if (command.hasMoreElements()) {

            // vars to use during processing
            SPlayer p = CampaignMain.cm.getPlayer(Username);
            int unitid = Integer.parseInt(command.nextToken());
            SUnit m = p.getUnit(unitid);
            SHouse h = p.getHouseFightingFor();
            String target = "<none>";

            // blow out if player has a null unit
            if (m == null) {
                CampaignMain.cm.toUser("AM:You do not have a unit with ID# " + unitid + ".", Username, true);
                return;
            }

            if (p.mayAcquireWelfareUnits()) {
                CampaignMain.cm.toUser("AM:You may not repod your units while you are on welfare!", Username, true);
                return;
            }

            if (UnitUtils.hasArmorDamage(m.getEntity()) || UnitUtils.hasCriticalDamage(m.getEntity())) {
                CampaignMain.cm.toUser("AM:This unit is currently damaged and cannot be repodded until you repair it.", Username, true);
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
                CampaignMain.cm.toUser("AM:Your " + m.getVerboseModelName() + " is not an Omni.", Username, true);
                return;
            }

            // don't allow a player to repod a unit which is in an army
            // NOTE: repodding while in armies changes BV, leading to very funky
            // .checkOperations() circumstances (add, then remove, need to know
            // which armies are impacted, etc).
            if (p.getAmountOfTimesUnitExistsInArmies(m.getId()) > 0) {
                CampaignMain.cm.toUser("AM:You may not repod a unit while it is in an army.", Username, true);
                return;
            }

            // use /c repod#ID#Target Unit, or /c repod#ID#RANDOM
            Entity mEnt = m.getEntity();
            String targetChassis = mEnt.getChassis();

            // scan the owner's default build list for pod types.
            Vector<SUnitFactory> possible = new Vector<SUnitFactory>(1, 1);

            // now, to find the build tables, let's make a vector of them...
            Vector<String> tables = new Vector<String>(1, 1);
            if (global) {
                Iterator<House> Houses = CampaignMain.cm.getData().getAllHouses().iterator();
                String fileName = "";
                String timeZone = h.getConfig("RewardsRepodFolder");
                while (Houses.hasNext()) {
                    SHouse faction = (SHouse) Houses.next();

                    fileName = BuildTable.getFileName(faction.getName(), Unit.getWeightClassDesc(m.getWeightclass()), timeZone, m.getType());
                    // MWLogger.errLog("File: "+fileName);

                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
                if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                    fileName = BuildTable.getFileName("Common", Unit.getWeightClassDesc(m.getWeightclass()), timeZone, m.getType());
                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
            } else if (!Boolean.parseBoolean(h.getConfig("RepodUsesFactory"))) {
                String fileName = "";
                String timeZone = h.getConfig("NoFactoryRepodFolder");
                // MWLogger.errLog("TimeZone: "+timeZone);
                fileName = BuildTable.getFileName(h.getName(), Unit.getWeightClassDesc(m.getWeightclass()), timeZone, m.getType());
                // MWLogger.errLog("File: "+fileName);

                if (!tables.contains(fileName)) {
                    tables.add(fileName);
                }
                if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                    fileName = BuildTable.getFileName("Common", Unit.getWeightClassDesc(m.getWeightclass()), timeZone, m.getType());
                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }
                }
            } else {

                // scan the owner's default build list for pod types.
                possible = p.getMyHouse().getPossibleFactoryForProduction(m.getType(), m.getWeightclass(), false);
                for (SUnitFactory working : possible) {

                    String fileName = BuildTable.getFileName(working.getFounder(), Unit.getWeightClassDesc(m.getWeightclass()), working.getBuildTableFolder(), m.getType());

                    if (!tables.contains(fileName)) {
                        tables.add(fileName);
                    }

                    if (Boolean.parseBoolean(h.getConfig("UseCommonTableForRepod"))) {
                        fileName = BuildTable.getFileName("Common", Unit.getWeightClassDesc(m.getWeightclass()), working.getBuildTableFolder(), m.getType());
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
                    CampaignMain.cm.toUser("Random repodding is not allowed.", Username, true);
                    return;
                }
            } else if (Boolean.parseBoolean(h.getConfig("RandomRepodOnly"))) {// there
                // is
                // a
                // real
                // target
                CampaignMain.cm.toUser("Only random repods are allowed.", Username, true);
                return;
            }

            // now, fetch the actual lines from the files
            String result = "";
            Vector<String> variants = new Vector<String>(1, 1);
            // variants.add(mEnt.getModel());
            int i = tables.size();

            // MWLogger.errLog("table size is "+i);
            if (i < 1) {
                CampaignMain.cm.toUser("AM:Repod Failed: No acceptable factory currently available", Username, true);
                return;
            }

            while (i > 0) {

                i--;
                String prodFile = tables.elementAt(i);

                try {

                    FileInputStream fis = new FileInputStream(prodFile);
                    BufferedReader dis = new BufferedReader(new InputStreamReader(fis));

                    while (dis.ready()) {

                        String l = dis.readLine();
                        StringTokenizer ST = new StringTokenizer(l);

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
                                SUnit cm = new SUnit(unitid, m.getProducer(), Filename);

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
                                            String repodMoneyCfg = "RepodCost" + Unit.getWeightClassDesc(cm.getWeightclass());
                                            String repodInfluCfg = "RepodFlu" + Unit.getWeightClassDesc(cm.getWeightclass());
                                            String repodCompCfg = "RepodComp" + Unit.getWeightClassDesc(cm.getWeightclass());

                                            int repodMoneyMod = Integer.parseInt(h.getConfig(repodMoneyCfg));
                                            int repodFluMod = Integer.parseInt(h.getConfig(repodInfluCfg));
                                            int repodCompMod = Integer.parseInt(h.getConfig(repodCompCfg));

                                            if (CampaignMain.cm.getOmniVariantMods().get(Filename) != null) {
                                                String mods = CampaignMain.cm.getOmniVariantMods().get(Filename);
                                                StringTokenizer modlist = new StringTokenizer(mods, "$");
                                                repodMoneyMod += Integer.parseInt(modlist.nextToken());
                                                repodCompMod += Integer.parseInt(modlist.nextToken());
                                                repodFluMod += Integer.parseInt(modlist.nextToken());

                                            }

                                            result += Filename + "#" + repodMoneyMod + "$" + repodCompMod + "$" + repodFluMod + "#";
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
                } catch (FileNotFoundException ex) {
                    MWLogger.mainLog("File " + prodFile + " was not Found");
                } catch (IOException ex) {
                    MWLogger.mainLog("File " + prodFile + " had an I/O error");
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                    MWLogger.mainLog("File " + prodFile + " has a problem");
                }

                finally {
                    // nothing
                }
            }// tables -> variants iterator

            if (target.equals("RANDOM")) {

                int size = variants.size();
                if (size <= 0) {
                    CampaignMain.cm.toUser("AM:No random targets available for " + m.getModelName() + ".", Username, true);
                    return;
                }
                if (size == 1) {
                    String Filename = variants.elementAt(0);
                    createOmni(m, Filename, m.getId(), p, possible, true);
                    return;
                }

                int number = CampaignMain.cm.getRandomNumber(size);
                String Filename = variants.elementAt(number);
                createOmni(m, Filename, m.getId(), p, possible, true);
                return;
            }

            // didnt actually repod. send a RUD command to the client.
            if (global) {
                result += "#GLOBAL";
            }
            CampaignMain.cm.toUser("RUD|" + unitid + "|" + result, Username, false);
        }

        // CampaignMain.cm.toUser("Usage: <CODE>/c repod#{unitid}#{New Variant}</CODE>",
        // Username, true);
        return;
    }

    private void createOmni(SUnit m, String Filename, int unitid, SPlayer p, Vector<SUnitFactory> possible, boolean random) {

        // MWLogger.errLog("Filename "+Filename);
        SHouse h = p.getHouseFightingFor();
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
        SUnit cm = new SUnit(unitid, m.getProducer(), Filename);
        if (cm.getModelName().equals("OMG-UR-FD")) {
            CampaignMain.cm.toUser("AM:Invalid repod format try again!", Username, true);
            return;
        }

        if (!global) {

            String needPartsList = p.getUnitParts().canRepodUnit(m.getEntity(), cm.getEntity()).trim();
            if (CampaignMain.cm.getBooleanConfig("UsePartsRepair") && (needPartsList.length() > 0)) {
                CampaignMain.cm.toUser("You do not have enough parts to repod your " + m.getModelName() + " to " + cm.getModelName() + "<br> you need the following parts:<br>" + needPartsList, Username);
                return;
            }

            String repodMoneyCfg = "RepodCost" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodInfluCfg = "RepodFlu" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodCompCfg = "RepodComp" + Unit.getWeightClassDesc(m.getWeightclass());
            String repodRefreshCfg = "RepodRefreshTime" + Unit.getWeightClassDesc(m.getWeightclass());
            int repodMoneyMod = 0, repodCompMod = 0, repodFluMod = 0;

            if (CampaignMain.cm.getOmniVariantMods().get(cm.getModelName()) != null) {
                String mods = CampaignMain.cm.getOmniVariantMods().get(cm.getModelName());
                StringTokenizer modList = new StringTokenizer(mods, "$");
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
                    CampaignMain.cm.toUser("AM:You do not have enough money to repod this unit! It will cost " + CampaignMain.cm.moneyOrFluMessage(true, false, moneyCost) + " to repod this unit", Username, true);
                    return;
                }

                // deduct influence
                influenceCost = Integer.parseInt(h.getConfig(repodInfluCfg)) + repodFluMod;
                if (random) {
                    influenceCost = (influenceCost * repodRandomMod) / 100;
                }

                if (p.getInfluence() < influenceCost) {
                    CampaignMain.cm.toUser("AM:You do not have enough influence to repod this unit! It will cost " + CampaignMain.cm.moneyOrFluMessage(false, false, influenceCost) + " to repod this unit!", Username, true);
                    return;
                }

                if (Boolean.parseBoolean(h.getConfig("RepodUsesComp"))) {
                    compCost = Integer.parseInt(h.getConfig(repodCompCfg)) + repodCompMod;
                    if (random) {
                        compCost = (compCost * repodRandomMod) / 100;
                    }

                    if (h.getPP(m.getWeightclass(), m.getType()) < compCost) {
                        CampaignMain.cm.toUser("AM:Your faction doesn't have enough components to repod this unit. You need " + compCost + " components to repod.", Username, true);
                        return;
                    }
                }

                StringBuilder hsUpdates = new StringBuilder();
                if (Boolean.parseBoolean(h.getConfig("RepodUsesFactory"))) {
                    SUnitFactory working = possible.elementAt(0);
                    refreshTime = Integer.parseInt(h.getConfig(repodRefreshCfg));
                    if (random) {
                        refreshTime = (refreshTime * repodRandomMod) / 100;
                    }
                    hsUpdates.append(working.addRefresh(refreshTime, false));
                }

                hsUpdates.append(h.addPP(m.getWeightclass(), m.getType(), -compCost, false));
                if (hsUpdates.length() > 0) {
                    CampaignMain.cm.doSendToAllOnlinePlayers(h, "HS|" + hsUpdates.toString(), false);
                }

                p.addMoney(-moneyCost);
                p.addInfluence(-influenceCost);

            }// - end Repod costing
        } else {
            if (p.getReward() < rpCost) {
                CampaignMain.cm.toUser("AM:You do not have enough " + CampaignMain.cm.getConfig("RPLongName") + " to repod this unit!", Username, true);
                return;
            }
            p.addReward(-rpCost);
        }

        cm.setPilot((SPilot) m.getPilot());
        cm.setExperience(m.getExperience());

        Entity entity = p.getUnit(unitid).getEntity();

        // Take the parts for it.
        if (CampaignMain.cm.getBooleanConfig("UsePartsRepair")) {
            p.getUnitParts().repodUnit(entity, cm.getEntity());
            CampaignMain.cm.toUser("PL|RPPC|" + p.getUnitParts().toString(), Username, false);
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
        CampaignMain.cm.toUser("AM:Your " + m.getVerboseModelName() + " is now " + StringUtils.aOrAn(cm.getVerboseModelName(), true) + ".", Username, true);
        if (!global) {
        	if (Boolean.parseBoolean(h.getConfig("RepodUsesComp"))) {
                CampaignMain.cm.toUser("AM:Repodding cost " + CampaignMain.cm.moneyOrFluMessage(true, false, moneyCost) + " and " + CampaignMain.cm.moneyOrFluMessage(false, true, influenceCost) + " " + compCost + " Components.", Username, true);
            } else {
                CampaignMain.cm.toUser("AM:Repodding cost " + CampaignMain.cm.moneyOrFluMessage(true, false, moneyCost) + " and " + CampaignMain.cm.moneyOrFluMessage(false, true, influenceCost) + ".", Username, true);
            }
        } else {
            CampaignMain.cm.toUser("AM:Repodding cost " + rpCost + " " + CampaignMain.cm.getConfig("RPLongName") + ".", Username, true);
        }

        return;// break out of it all
    }
}