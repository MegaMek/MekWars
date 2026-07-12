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

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.gui.dialogs.AdvancedRepairDialog;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

/**
 * Handles the {@code "PL"} protocol prefix (confirmed on the server side, e.g. {@code SPlayer} sends
 * {@code "PL|UPPC|<part>#<amount>"}), which the server uses to push fine-grained state updates about the local
 * player — army composition, unit/hangar data, currency/experience/influence/rating totals, faction/house
 * membership, various UI-relevant flags, and more — down to this client. {@link #execute(String)} dispatches on a
 * short (2-5 letter) sub-command code immediately following the prefix; almost every case forwards straight to a
 * single {@link CPlayer} or {@link IClient} mutator method whose name documents its own effect (e.g. {@code "RA"}
 * calls {@link CPlayer#removeArmy}, {@code "SM"} calls {@link CPlayer#setMoney}). A few cases do more than a single
 * call:
 * <ul>
 * <li>{@code "FCU"} — triggers a full client update and returns immediately, skipping the trailing GUI refresh
 * calls described below.</li>
 * <li>{@code "VUI"}/{@code "VURD"} — build a unit from a sub-tokenized payload (delimited by {@code "#"}) to show
 * a battle-damage info window or an {@link AdvancedRepairDialog}.</li>
 * <li>{@code "SUD"} — gathers client/JVM diagnostic data (JAR checksums, OS/JVM system properties) and sends it
 * back to the server as {@code "c sendclientdata#..."}.</li>
 * <li>{@code "STS"} — sets a unit's targeting system and echoes a synthetic {@code "CH|AM: ..."} chat line so the
 * change is visible in the chat log.</li>
 * <li>{@code "USU"}/{@code "CSU"} — add/remove/clear the player's house's list of supported unit types.</li>
 * </ul>
 * For any sub-command not recognized, the {@code default} case returns immediately. Otherwise, after the switch,
 * three GUI panels ({@code REFRESH_HQ_PANEL}, {@code REFRESH_PLAYER_PANEL}, {@code REFRESH_BM_PANEL}) are always
 * refreshed — this happens even for cases whose own effect is unrelated to those panels. This class is
 * client-inbound only: {@link #parseReplyArgs(String)} and {@link #parseArguments(String)} are empty stubs.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PlayerCommand extends Command {
    private final static MMLogger LOGGER = MMLogger.create(PlayerCommand.class);

    /**
     * Constructs a client-side instance bound to {@code client}, as required by the {@link Command} contract.
     */
    public PlayerCommand(IClient client) {
        super(client);
    }

    /**
     * Dispatches on the sub-command code following the {@code "PL"} prefix; see the class-level docs for the
     * dispatch table and notable special cases. Returns early (without the trailing GUI refresh) if there are no
     * more tokens after the sub-command code, or if the sub-command is unrecognized ({@code default} case), or
     * for {@code "FCU"} (which triggers its own full client update instead).
     *
     * @param input the full raw protocol line, prefix included
     * @see Command#execute(String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer stringTokenizer = decode(input);

        String cmd = TokenReader.readString(stringTokenizer);
        CPlayer player = client.getPlayer();

        if (!stringTokenizer.hasMoreTokens()) {
            return;
        }

        switch (cmd) {
            case "FCU" -> {
                client.updateClient();
                return;
            }
            case "RA" -> player.removeArmy(TokenReader.readInt(stringTokenizer));
            case "LA" -> player.playerLockArmy(TokenReader.readInt(stringTokenizer));
            case "ULA" -> player.playerUnlockArmy(TokenReader.readInt(stringTokenizer));
            case "TAD" -> player.toggleArmyDisabled(TokenReader.readInt(stringTokenizer));
            case "SAD" -> player.setArmyData(TokenReader.readString(stringTokenizer));
            case "SABV" -> player.setArmyBV(TokenReader.readString(stringTokenizer));
            case "AAU" -> player.addArmyUnit(TokenReader.readString(stringTokenizer));
            case "RAU" -> player.removeArmyUnit(TokenReader.readString(stringTokenizer));
            case "HD" -> player.setHangarData(TokenReader.readString(stringTokenizer));
            case "RU" -> player.removeUnit(TokenReader.readInt(stringTokenizer));
            case "SE" -> player.setExp(TokenReader.readInt(stringTokenizer));
            case "SM" -> player.setMoney(TokenReader.readInt(stringTokenizer));
            case "UMT" -> player.setMekToken(TokenReader.readInt(stringTokenizer)); //@Salient
            case "SB" -> player.setBays(TokenReader.readInt(stringTokenizer));
            case "SF" -> player.setFreeBays(TokenReader.readInt(stringTokenizer));
            case "SI" -> player.setInfluence(TokenReader.readInt(stringTokenizer));
            case "SR" -> player.setRating(TokenReader.readDouble(stringTokenizer));
            case "SRP" -> player.setRewardPoints(TokenReader.readInt(stringTokenizer));
            case "SH" -> player.setHouse(TokenReader.readString(stringTokenizer));
            case "ST" -> player.setTechnicians(TokenReader.readInt(stringTokenizer));
            case "SSN" -> player.setSubFaction(TokenReader.readString(stringTokenizer));
            case "AAA" -> player.setAutoArmy(stringTokenizer);// give it the whole tokenizer
            case "AAM" -> player.setMines(stringTokenizer);// give it the whole tokenizer
            case "GEA" -> player.setAutoGunEmplacements(stringTokenizer);// give it the whole tokenizer
            case "SUS" -> player.setUnitStatus(TokenReader.readString(stringTokenizer));
            case "RNA" -> player.setArmyName(TokenReader.readString(stringTokenizer));
            case "SAB" -> player.setArmyLimit(TokenReader.readString(stringTokenizer));
            case "SAL" -> player.setArmyLock(TokenReader.readString(stringTokenizer));
            case "UU" -> player.updateUnitData(stringTokenizer);
            case "UUMG" -> player.updateUnitMachineGuns(stringTokenizer);
            case "BMW" -> {
                if (client.getConfig().isParam("ENABLE_BM_SOUND")) {
                    client.doPlaySound(client.getConfig().getParam("SOUND_ON_BM_WIN"));
                }
            }
            case "PPQ" -> player.getPersonalPilotQueue().fromString(TokenReader.readString(stringTokenizer));
            case "PEU" -> player.setPlayerExcludes(TokenReader.readString(stringTokenizer), "$");
            case "AEU" -> player.setAdminExcludes(TokenReader.readString(stringTokenizer), "$");
            case "RPU" -> player.repositionArmyUnit(TokenReader.readString(stringTokenizer));
            case "UOE" -> player.updateOperations(TokenReader.readString(stringTokenizer));
            case "UTT" -> player.updateTotalTechs(TokenReader.readString(stringTokenizer));
            case "UAT" -> player.updateAvailableTechs(TokenReader.readString(stringTokenizer));
            case "GBB" -> client.getConnector().closeConnection();
            case "UB" -> client.setUsingBots(TokenReader.readBoolean(stringTokenizer));
            case "BOST" -> client.setBotsOnSameTeam(TokenReader.readBoolean(stringTokenizer));
            case "SHFF" -> player.setHouseFightingFor(TokenReader.readString(stringTokenizer));
            case "SUL" -> {
                player.setLogo(TokenReader.readString(stringTokenizer));
                client.getMainFrame().getMainPanel().getPlayerPanel().refresh();
            }
            case "AP2PPQ" -> player.getPersonalPilotQueue().addPilot(stringTokenizer);
            case "RPPPQ" -> player.getPersonalPilotQueue().removePilot(stringTokenizer);
            case "RSOD" -> client.retrieveOpData("short", TokenReader.readString(stringTokenizer));
            case "UCP" -> client.updateParam(stringTokenizer);
            case "SOFL" -> client.setServerOpFlags(stringTokenizer);
            case "SAOFS" -> player.setArmyOpForceSize(TokenReader.readString(stringTokenizer));
            case "FC" -> player.setFactionConfigs(TokenReader.readString(stringTokenizer));
            case "UPBM" -> client.updatePartsBlackMarket(TokenReader.readString(stringTokenizer),
                  MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 3045));
            case "UPPC" -> client.updatePlayerPartsCache(TokenReader.readString(stringTokenizer));
            case "RPPC" -> client.getPlayer().getPartsCache().fromString(stringTokenizer);
            case "STN" -> client.getPlayer().setTeamNumber(TokenReader.readInt(stringTokenizer));
            case "VUI" -> {
                StringTokenizer data = new StringTokenizer(TokenReader.readString(stringTokenizer), "#");
                String filename = TokenReader.readString(data);
                int battleValue = TokenReader.readInt(data);
                int gunnery = TokenReader.readInt(data);
                int piloting = TokenReader.readInt(data);
                String damage = "";

                if (data.hasMoreElements()) {
                    damage = TokenReader.readString(data);
                }

                client.getMainFrame()
                      .getMainPanel()
                      .getHSPanel()
                      .showInfoWindow(filename, battleValue, gunnery, piloting, damage);
            }
            case "VURD" -> {
                StringTokenizer data = new StringTokenizer(TokenReader.readString(stringTokenizer), "#");
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
            case "RMF" -> client.retrieveMul(TokenReader.readString(stringTokenizer));
            case "SMFD" -> client.getMainFrame().showMulFileList(TokenReader.readString(stringTokenizer));
            case "CAFM" -> client.getMainFrame().createArmyFromMul(TokenReader.readString(stringTokenizer));
            case "USU" -> {
                // Update Supported Units
                while (stringTokenizer.hasMoreTokens()) {
                    boolean addSupport = TokenReader.readBoolean(stringTokenizer);
                    String unitName = TokenReader.readString(stringTokenizer);

                    if (addSupport) {
                        player.getMyHouse().addUnitSupported(unitName);
                    } else {
                        player.getMyHouse().removeUnitSupported(unitName);
                    }
                }
                LOGGER.info(player.getMyHouse().getSupportedUnits().toString());
            }
            case "CSU" -> {
                // clear supported units
                LOGGER.info("Clearing Supported Units");
                player.getMyHouse().supportedUnits.clear();
                player.getMyHouse()
                      .setNonFactionUnitsCostMore(MathUtility.parseBoolean(client.getServerConfigs(
                            "UseNonFactionUnitsIncreasedTechs"), false));
            }
            case "SMA" -> client.getPlayer().setMULCreatedArmy(stringTokenizer);
            case "ANH" -> client.createNewHouse(stringTokenizer);
            case "RPF" -> {
                int id = TokenReader.readInt(stringTokenizer);
                client.getData().removeHouse(id);
            }
            case "UDT" -> client.addToChat(TokenReader.readString(stringTokenizer),
                  client.getConfig().getIntParam("USER_DEFIND_MESSAGE_TAB"));
            case "CCC" -> client.getCampaign().setComponentConverter(stringTokenizer.nextToken());
            case "SUD" -> {
                try {
                    StringBuilder userData = new StringBuilder(String.format("%sc sendclientdata#", IClient.CAMPAIGN_PREFIX));
                    String clientMD5 = client.createFilenameChecksum("./MekWarsClient.jar");
                    String mmMD5 = client.createFilenameChecksum("./MegaMek.jar");
                    userData.append(client.getClass().getProtectionDomain().getCodeSource().getLocation().toURI())
                          .append("#");
                    userData.append(clientMD5).append("#");
                    userData.append(mmMD5).append("#");

                    String[] userDataSet =
                          { "user.name", "user.language", "user.country", "user.timezone", "os.name", "os.arch",
                            "os.version", "java.version" };

                    for (String string : userDataSet) {
                        String property = System.getProperty(string, "Unknown");
                        userData.append(property);
                        userData.append("#");
                    }
                    client.sendChat(userData.toString());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error creating MD5 Checksums for client data.");
                }
            }
            case "ROP" -> client.getPlayer().setAutoReorder(TokenReader.readBoolean(stringTokenizer));
            case "SHP" -> player.parseHangarPenaltyString(TokenReader.readString(stringTokenizer));
            case "STS" -> {
                int unitID = TokenReader.readInt(stringTokenizer);
                int targetType = TokenReader.readInt(stringTokenizer);
                player.getUnit(unitID).setTargetSystem(targetType);
                client.doParseDataInput(String.format("CH|AM: Targeting for unit %s set to %s", unitID, player.getUnit(unitID)
                                                                                                .getTargetSystemTypeDesc()));
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
     * No-op. This command is client-inbound only; it is never sent as a request awaiting a coded reply.
     */
    @Override
    public void parseReplyArgs(String s) {

    }

    /**
     * No-op. This command is never dispatched server-side through the {@link ServerCommand} path (see
     * {@link Command} class-level docs), so there are no server-bound arguments to parse.
     */
    @Override
    public void parseArguments(String s) {

    }
}
