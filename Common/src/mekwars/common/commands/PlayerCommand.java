/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.common.commands;

import java.util.StringTokenizer;

import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.gui.dialogs.AdvancedRepairDialog;
import mekwars.common.util.MWLogger;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PlayerCommand extends Command {

    /**
     *
     */
    public PlayerCommand(IClient client) {
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

        switch (cmd) {
            case "FCU" -> {
                client.updateClient();
                return;
            }
            case "RA" -> player.removeArmy(TokenReader.readInt(st));
            case "LA" -> player.playerLockArmy(TokenReader.readInt(st));
            case "ULA" -> player.playerUnlockArmy(TokenReader.readInt(st));
            case "TAD" -> player.toggleArmyDisabled(TokenReader.readInt(st));
            case "SAD" -> player.setArmyData(TokenReader.readString(st));
            case "SABV" -> player.setArmyBV(TokenReader.readString(st));
            case "AAU" -> player.addArmyUnit(TokenReader.readString(st));
            case "RAU" -> player.removeArmyUnit(TokenReader.readString(st));
            case "HD" -> player.setHangarData(TokenReader.readString(st));
            case "RU" -> player.removeUnit(TokenReader.readInt(st));
            case "SE" -> player.setExp(TokenReader.readInt(st));
            case "SM" -> player.setMoney(TokenReader.readInt(st));
            case "UMT" -> player.setMekToken(TokenReader.readInt(st)); //@Salient
            case "SB" -> player.setBays(TokenReader.readInt(st));
            case "SF" -> player.setFreeBays(TokenReader.readInt(st));
            case "SI" -> player.setInfluence(TokenReader.readInt(st));
            case "SR" -> player.setRating(TokenReader.readDouble(st));
            case "SRP" -> player.setRewardPoints(TokenReader.readInt(st));
            case "SH" -> player.setHouse(TokenReader.readString(st));
            case "ST" -> player.setTechnicians(TokenReader.readInt(st));
            case "SSN" -> player.setSubFaction(TokenReader.readString(st));
            case "AAA" -> player.setAutoArmy(st);// give it the whole tokenizer
            case "AAM" -> player.setMines(st);// give it the whole tokenizer
            case "GEA" -> player.setAutoGunEmplacements(st);// give it the whole tokenizer
            case "SUS" -> player.setUnitStatus(TokenReader.readString(st));
            case "RNA" -> player.setArmyName(TokenReader.readString(st));
            case "SAB" -> player.setArmyLimit(TokenReader.readString(st));
            case "SAL" -> player.setArmyLock(TokenReader.readString(st));
            case "UU" -> player.updateUnitData(st);
            case "UUMG" -> player.updateUnitMachineGuns(st);
            case "BMW" -> {
                if (client.getConfig().isParam("ENABLEBMSOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUNDONBMWIN"));
                }
            }
            case "PPQ" -> player.getPersonalPilotQueue().fromString(TokenReader.readString(st));
            case "PEU" -> player.setPlayerExcludes(TokenReader.readString(st), "$");
            case "AEU" -> player.setAdminExcludes(TokenReader.readString(st), "$");
            case "RPU" -> player.repositionArmyUnit(TokenReader.readString(st));
            case "UOE" -> player.updateOperations(TokenReader.readString(st));
            case "UTT" -> player.updateTotalTechs(TokenReader.readString(st));
            case "UAT" -> player.updateAvailableTechs(TokenReader.readString(st));
            case "GBB" -> client.getConnector().closeConnection();
            case "UB" -> client.setUsingBots(TokenReader.readBoolean(st));
            case "BOST" -> client.setBotsOnSameTeam(TokenReader.readBoolean(st));
            case "SHFF" -> player.setHouseFightingFor(TokenReader.readString(st));
            case "SUL" -> {
                player.setLogo(TokenReader.readString(st));
                client.getMainFrame().getMainPanel().getPlayerPanel().refresh();
            }
            case "AP2PPQ" -> player.getPersonalPilotQueue().addPilot(st);
            case "RPPPQ" -> player.getPersonalPilotQueue().removePilot(st);
            case "RSOD" -> client.retrieveOpData("short", TokenReader.readString(st));
            case "UCP" -> client.updateParam(st);
            case "SOFL" -> client.setServerOpFlags(st);
            case "SAOFS" -> player.setArmyOpForceSize(TokenReader.readString(st));
            case "FC" -> player.setFactionConfigs(TokenReader.readString(st));
            case "UPBM" -> client.updatePartsBlackMarket(TokenReader.readString(st),
                  Integer.parseInt(client.getServerConfigs("CampaignYear")));
            case "UPPC" -> client.updatePlayerPartsCache(TokenReader.readString(st));
            case "RPPC" -> client.getPlayer().getPartsCache().fromString(st);
            case "STN" -> client.getPlayer().setTeamNumber(TokenReader.readInt(st));
            case "VUI" -> {
                StringTokenizer data = new StringTokenizer(TokenReader.readString(st), "#");
                String filename = TokenReader.readString(data);
                int BV = TokenReader.readInt(data);
                int gunnery = TokenReader.readInt(data);
                int piloting = TokenReader.readInt(data);
                String damage = "";

                if (data.hasMoreElements()) {
                    damage = TokenReader.readString(data);
                }

                client.getMainFrame()
                      .getMainPanel()
                      .getHSPanel()
                      .showInfoWindow(filename, BV, gunnery, piloting, damage);
            }
            case "VURD" -> {
                StringTokenizer data = new StringTokenizer(TokenReader.readString(st), "#");
                String filename = TokenReader.readString(data);
                String damage = TokenReader.readString(data);
                CUnit unit = new CUnit(client);

                unit.setUnitFilename(filename);
                unit.createEntity();
                unit.setPilot(new Pilot("Jeeves", 4, 5));
                UnitUtils.applyBattleDamage(unit.getEntity(), damage, true);
                new AdvancedRepairDialog(client, unit, unit.getEntity(), false);
            }
            case "CPPC" -> client.getPlayer().getPartsCache().clear();
            case "UDAO" -> {
                client.updateOpData(true);
                if (!client.isDedicated()) {
                    client.getMainFrame().updateAttackMenu();
                }
            }
            case "RMF" -> client.retrieveMul(TokenReader.readString(st));
            case "SMFD" -> client.getMainFrame().showMulFileList(TokenReader.readString(st));
            case "CAFM" -> client.getMainFrame().createArmyFromMul(TokenReader.readString(st));
            case "USU" -> {
                // Update Supported Units
                while (st.hasMoreTokens()) {
                    boolean addSupport = TokenReader.readBoolean(st);
                    String unitName = TokenReader.readString(st);
                    if (addSupport) {
                        player.getMyHouse().addUnitSupported(unitName);
                    } else {
                        player.getMyHouse().removeUnitSupported(unitName);
                    }
                }
                MWLogger.infoLog(player.getMyHouse().getSupportedUnits().toString());
            }
            case "CSU" -> {
                // clear supported units
                MWLogger.infoLog("Clearing Supported Units");
                player.getMyHouse().supportedUnits.clear();
                player.getMyHouse()
                      .setNonFactionUnitsCostMore(Boolean.parseBoolean(client.getServerConfigs(
                            "UseNonFactionUnitsIncreasedTechs")));
            }
            case "SMA" -> client.getPlayer().setMULCreatedArmy(st);
            case "ANH" -> client.createNewHouse(st);
            case "RPF" -> {
                int id = TokenReader.readInt(st);
                client.getData().removeHouse(id);
            }
            case "UDT" ->
                  client.addToChat(TokenReader.readString(st), client.getConfig().getIntParam("USERDEFINDMESSAGETAB"));
            case "CCC" -> client.getCampaign().setComponentConverter(st.nextToken());
            case "SUD" -> {
                try {
                    StringBuilder userData = new StringBuilder(STR."\{IClient.CAMPAIGN_PREFIX}c sendclientdata#");
                    String clientMD5 = client.createFilenameChecksum("./MekWarsClient.jar");
                    String mmMD5 = client.createFilenameChecksum("./MegaMek.jar");
                    userData.append(client.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                          .append("#");
                    userData.append(clientMD5).append("#");
                    userData.append(mmMD5).append("#");

                    String[] userDataSet =
                          { "user.name", "user.language", "user.country", "user.timezone", "os.name", "os.arch",
                            "os.version", "java.version" };

                    for (String s : userDataSet) {
                        String property = System.getProperty(s, "Unknown");
                        userData.append(property);
                        userData.append("#");
                    }
                    client.sendChat(userData.toString());
                } catch (Exception ex) {
                }
            }
            case "ROP" -> client.getPlayer().setAutoReorder(TokenReader.readBoolean(st));
            case "SHP" -> player.parseHangarPenaltyString(TokenReader.readString(st));
            case "STS" -> {
                int unitID = TokenReader.readInt(st);
                int targetType = TokenReader.readInt(st);
                //MWLogger.errLog("Setting Targeting for Unit " + unitID + " to " + targetType);
                player.getUnit(unitID).setTargetSystem(targetType);
                client.doParseDataInput(STR."CH|AM: Targeting for unit \{unitID} set to \{player.getUnit(unitID)
                                                                                                .getTargetSystemTypeDesc()}");
            }
            default -> {
                return;
            }
        }

        client.refreshGUI(IClient.REFRESH_HQ_PANEL);
        client.refreshGUI(IClient.REFRESH_PLAYER_PANEL);
        client.refreshGUI(IClient.REFRESH_BM_PANEL);
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
