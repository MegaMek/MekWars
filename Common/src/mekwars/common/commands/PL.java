/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

package mekwars.common.commands;

import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.util.MWLogger;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PL extends Command {

    /**
     * @param client
     */
    public PL(IClient client) {
        super(client);
    }

    /**
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        java.util.StringTokenizer st = decode(input);

        String cmd = TokenReader.readString(st);
        CPlayer player = client.getPlayer();

        if (!st.hasMoreTokens()) {
            return;
        }

        if (cmd.equals("FCU")) {
            client.updateClient();
            return;
        }

        if (cmd.equals("RA")) {
            player.removeArmy(TokenReader.readInt(st));
        } else if (cmd.equals("LA")) {
            player.playerLockArmy(TokenReader.readInt(st));
        } else if (cmd.equals("ULA")) {
            player.playerUnlockArmy(TokenReader.readInt(st));
        } else if (cmd.equals("TAD")) {
            player.toggleArmyDisabled(TokenReader.readInt(st));
        } else if (cmd.equals("SAD")) {
            player.setArmyData(TokenReader.readString(st));
        } else if (cmd.equals("SABV")) {
            player.setArmyBV(TokenReader.readString(st));
        } else if (cmd.equals("AAU")) {
            player.addArmyUnit(TokenReader.readString(st));
        } else if (cmd.equals("RAU")) {
            player.removeArmyUnit(TokenReader.readString(st));
        } else if (cmd.equals("HD")) {
            player.setHangarData(TokenReader.readString(st));
        } else if (cmd.equals("RU")) {
            player.removeUnit(TokenReader.readInt(st));
        } else if (cmd.equals("SE")) {
            player.setExp(TokenReader.readInt(st));
        } else if (cmd.equals("SM")) {
            player.setMoney(TokenReader.readInt(st));
        } else if (cmd.equals("UMT")) {
            player.setMekToken(TokenReader.readInt(st)); //@Salient
        } else if (cmd.equals("SB")) {
            player.setBays(TokenReader.readInt(st));
        } else if (cmd.equals("SF")) {
            player.setFreeBays(TokenReader.readInt(st));
        } else if (cmd.equals("SI")) {
            player.setInfluence(TokenReader.readInt(st));
        } else if (cmd.equals("SR")) {
            player.setRating(TokenReader.readDouble(st));
        } else if (cmd.equals("SRP")) {
            player.setRewardPoints(TokenReader.readInt(st));
        } else if (cmd.equals("SH")) {
            player.setHouse(TokenReader.readString(st));
        } else if (cmd.equals("ST")) {
            player.setTechnicians(TokenReader.readInt(st));
        } else if (cmd.equals("SSN")) {
            player.setSubFaction(TokenReader.readString(st));
        } else if (cmd.equals("AAA")) {
            player.setAutoArmy(st);// give it the whole tokenizer
        } else if (cmd.equals("AAM")) {
            player.setMines(st);// give it the whole tokenizer
        } else if (cmd.equals("GEA")) {
            player.setAutoGunEmplacements(st);// give it the whole tokenizer
        } else if (cmd.equals("SUS")) {
            player.setUnitStatus(TokenReader.readString(st));
        } else if (cmd.equals("RNA")) {
            player.setArmyName(TokenReader.readString(st));
        } else if (cmd.equals("SAB")) {
            player.setArmyLimit(TokenReader.readString(st));
        } else if (cmd.equals("SAL")) {
            player.setArmyLock(TokenReader.readString(st));
        } else if (cmd.equals("UU")) {
            player.updateUnitData(st);
        } else if (cmd.equals("UUMG")) {
            player.updateUnitMachineGuns(st);
        } else if (cmd.equals("BMW")) { // play a sound someone won the bm.
            if (client.getConfig().isParam("ENABLEBMSOUND")) {
                client.doPlaySound(client.getConfig().getParam("SOUNDONBMWIN"));
            }
        } else if (cmd.equals("PPQ")) {
            player.getPersonalPilotQueue().fromString(TokenReader.readString(st));
        } else if (cmd.equals("PEU")) {
            player.setPlayerExcludes(TokenReader.readString(st), "$");
        } else if (cmd.equals("AEU")) {
            player.setAdminExcludes(TokenReader.readString(st), "$");
        } else if (cmd.equals("RPU")) {
            player.repositionArmyUnit(TokenReader.readString(st));
        } else if (cmd.equals("UOE")) {
            player.updateOperations(TokenReader.readString(st));
        } else if (cmd.equals("UTT")) {
            player.updateTotalTechs(TokenReader.readString(st));
        } else if (cmd.equals("UAT")) {
            player.updateAvailableTechs(TokenReader.readString(st));
        } else if (cmd.equals("GBB")) {
            client.getConnector().closeConnection();
        } else if (cmd.equals("UB")) {
            client.setUsingBots(TokenReader.readBoolean(st));
        } else if (cmd.equals("BOST")) {
            client.setBotsOnSameTeam(TokenReader.readBoolean(st));
        } else if (cmd.equals("SHFF")) {
            player.setHouseFightingFor(TokenReader.readString(st));
        } else if (cmd.equals("SUL")) {// Players Unit Logo
            player.setLogo(TokenReader.readString(st));
            client.getMainFrame().getMainPanel().getPlayerPanel().refresh();
        } else if (cmd.equals("AP2PPQ")) {
            player.getPersonalPilotQueue().addPilot(st);
        } else if (cmd.equals("RPPPQ")) {
            player.getPersonalPilotQueue().removePilot(st);
        } else if (cmd.equals("RSOD")) {
            client.retrieveOpData("short", TokenReader.readString(st));
        } else if (cmd.equals("UCP")) {
            client.updateParam(st);
        } else if (cmd.equals("SOFL")) {
            client.setServerOpFlags(st);
        } else if (cmd.equals("SAOFS")) {
            player.setArmyOpForceSize(TokenReader.readString(st));
        } else if (cmd.equals("FC")) {
            player.setFactionConfigs(TokenReader.readString(st));
        } else if (cmd.equals("UPBM")) {
            client.updatePartsBlackMarket(TokenReader.readString(st), Integer.parseInt(client.getServerConfigs("CampaignYear")));
        } else if (cmd.equals("UPPC")) {
            client.updatePlayerPartsCache(TokenReader.readString(st));
        } else if (cmd.equals("RPPC")) {
            client.getPlayer().getPartsCache().fromString(st);
        } else if (cmd.equals("STN")) {
            client.getPlayer().setTeamNumber(TokenReader.readInt(st));
        } else if (cmd.equals("VUI")) {
            java.util.StringTokenizer data = new java.util.StringTokenizer(TokenReader.readString(st), "#");
            String filename = TokenReader.readString(data);
            int BV = TokenReader.readInt(data);
            int gunnery = TokenReader.readInt(data);
            int piloting = TokenReader.readInt(data);
            String damage = "";

            if (data.hasMoreElements()) {
                damage = TokenReader.readString(data);
            }

            client.getMainFrame().getMainPanel().getHSPanel().showInfoWindow(filename, BV, gunnery, piloting, damage);
        } else if (cmd.equals("VURD")) {
            java.util.StringTokenizer data = new java.util.StringTokenizer(TokenReader.readString(st), "#");
            String filename = TokenReader.readString(data);
            String damage = TokenReader.readString(data);
            client.campaign.CUnit unit = new client.campaign.CUnit(client);

            unit.setUnitFilename(filename);
            unit.createEntity();
            unit.setPilot(new Pilot("Jeeves", 4, 5));
            UnitUtils.applyBattleDamage(unit.getEntity(), damage, true);
            new client.gui.dialog.AdvancedRepairDialog(client, unit, unit.getEntity(), false);
        } else if (cmd.equals("CPPC")) {
            client.getPlayer().getPartsCache().clear();
        } else if (cmd.equals("UDAO")) {
            client.updateOpData(true);
            if (!client.isDedicated()) {
                client.getMainFrame().updateAttackMenu();
            }
        } else if (cmd.equals("RMF")) {
            client.retrieveMul(TokenReader.readString(st));
        } else if (cmd.equals("SMFD")) {
            client.getMainFrame().showMulFileList(TokenReader.readString(st));
        } else if (cmd.equals("CAFM")) {
            client.getMainFrame().createArmyFromMul(TokenReader.readString(st));
        } else if (cmd.equals("USU")) {
            // Update Supported Units
            while (st.hasMoreTokens()) {
                boolean addSupport = TokenReader.readBoolean(st);
                String unitName = TokenReader.readString(st);
                if (unitName != null) {
                    if (addSupport) {
                        player.getMyHouse().addUnitSupported(unitName);
                    } else {
                        player.getMyHouse().removeUnitSupported(unitName);
                    }
                }
            }
            MWLogger.infoLog(player.getMyHouse().getSupportedUnits().toString());
        } else if (cmd.equals("CSU")) {
            // clear supported units
            MWLogger.infoLog("Clearing Supported Units");
            player.getMyHouse().supportedUnits.clear();
            player.getMyHouse()
                  .setNonFactionUnitsCostMore(Boolean.parseBoolean(client.getServerConfigs(
                        "UseNonFactionUnitsIncreasedTechs")));
        } else if (cmd.equals("SMA")) {
            client.getPlayer().setMULCreatedArmy(st);
        } else if (cmd.equals("ANH")) {
            client.createNewHouse(st);
        } else if (cmd.equals("RPF")) {
            int id = TokenReader.readInt(st);
            client.getData().removeHouse(id);
        } else if (cmd.equals("UDT")) {
            client.addToChat(TokenReader.readString(st), client.getConfig().getIntParam("USERDEFINDMESSAGETAB"));
        } else if (cmd.equals("CCC")) {
            client.getCampaign().setComponentConverter(st.nextToken());
        } else if (cmd.equals("SUD")) {
            try {
                StringBuilder userData = new StringBuilder(IClient.CAMPAIGN_PREFIX + "c sendclientdata#");
                String clientMD5 = client.createFilenameChecksum("./MekWarsClient.jar");
                String mmMD5 = client.createFilenameChecksum("./MegaMek.jar");
                userData.append(client.getClass().getProtectionDomain().getCodeSource().getLocation().toURI() + "#");
                userData.append(clientMD5 + "#");
                userData.append(mmMD5 + "#");

                String[] userDataSet =
                      { "user.name", "user.language", "user.country", "user.timezone", "os.name", "os.arch",
                        "os.version", "java.version" };

                for (int pos = 0; pos < userDataSet.length; pos++) {
                    String property = System.getProperty(userDataSet[pos], "Unknown");
                    userData.append(property);
                    userData.append("#");
                }
                client.sendChat(userData.toString());
            } catch (Exception ex) {
            }
        } else if (cmd.equals("ROP")) {
            client.getPlayer().setAutoReorder(TokenReader.readBoolean(st));
        } else if (cmd.equals("SHP")) {
            player.parseHangarPenaltyString(TokenReader.readString(st));
        } else if (cmd.equals("STS")) {
            int unitID = TokenReader.readInt(st);
            int targetType = TokenReader.readInt(st);
            //MWLogger.errLog("Setting Targeting for Unit " + unitID + " to " + targetType);
            player.getUnit(unitID).setTargetSystem(targetType);
            client.doParseDataInput("CH|AM: Targeting for unit " +
                                          unitID +
                                          " set to " +
                                          player.getUnit(unitID).getTargetSystemTypeDesc());
        } else {
            return;
        }

        client.refreshGUI(IClient.REFRESH_HQ PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER PANEL);
        client.refreshGUI(IClient.REFRESH_BM PANEL);
    }

    /**
     * @param s
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * @param s
     */
    @Override
    public void parseArguments(String s) {

    }
}
