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

package client.cmd;

import java.util.StringTokenizer;

import client.MWClient;
import client.campaign.CPlayer;
import client.campaign.CUnit;
import client.gui.dialog.AdvancedRepairDialog;
import common.campaign.pilot.Pilot;
import common.util.MWLogger;
import common.util.TokenReader;
import common.util.UnitUtils;

/**
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class PL extends Command {

    /**
     * @param client
     */
    public PL(MWClient mwclient) {
        super(mwclient);
    }

    /**
     * @see client.cmd.Command#execute(java.lang.String)
     */
    @Override
    public void execute(String input) {
        StringTokenizer st = decode(input);

        String cmd = TokenReader.readString(st);
        CPlayer player = mwclient.getPlayer();

        if (!st.hasMoreTokens()) {
            return;
        }

        if (cmd.equals("FCU")) {
            mwclient.updateClient();
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
            if (mwclient.getConfig().isParam("ENABLEBMSOUND")) {
                mwclient.doPlaySound(mwclient.getConfig().getParam("SOUNDONBMWIN"));
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
            mwclient.getConnector().closeConnection();
        } else if (cmd.equals("UB")) {
            mwclient.setUsingBots(TokenReader.readBoolean(st));
        } else if (cmd.equals("BOST")) {
            mwclient.setBotsOnSameTeam(TokenReader.readBoolean(st));
        } else if (cmd.equals("SHFF")) {
            player.setHouseFightingFor(TokenReader.readString(st));
        } else if (cmd.equals("SUL")) {// Players Unit Logo
            player.setLogo(TokenReader.readString(st));
            mwclient.getMainFrame().getMainPanel().getPlayerPanel().refresh();
        } else if (cmd.equals("AP2PPQ")) {
            player.getPersonalPilotQueue().addPilot(st);
        } else if (cmd.equals("RPPPQ")) {
            player.getPersonalPilotQueue().removePilot(st);
        } else if (cmd.equals("RSOD")) {
            mwclient.retrieveOpData("short", TokenReader.readString(st));
        } else if (cmd.equals("UCP")) {
            mwclient.updateParam(st);
        } else if (cmd.equals("SOFL")) {
            mwclient.setServerOpFlags(st);
        } else if (cmd.equals("SAOFS")) {
            player.setArmyOpForceSize(TokenReader.readString(st));
        } else if (cmd.equals("FC")) {
            player.setFactionConfigs(TokenReader.readString(st));
        } else if (cmd.equals("UPBM")) {
            mwclient.updatePartsBlackMarket(TokenReader.readString(st), Integer.parseInt(mwclient.getserverConfigs("CampaignYear")));
        } else if (cmd.equals("UPPC")) {
            mwclient.updatePlayerPartsCache(TokenReader.readString(st));
        } else if (cmd.equals("RPPC")) {
            mwclient.getPlayer().getPartsCache().fromString(st);
        } else if (cmd.equals("STN")) {
            mwclient.getPlayer().setTeamNumber(TokenReader.readInt(st));
        } else if (cmd.equals("VUI")) {
            StringTokenizer data = new StringTokenizer(TokenReader.readString(st), "#");
            String filename = TokenReader.readString(data);
            int BV = TokenReader.readInt(data);
            int gunnery = TokenReader.readInt(data);
            int piloting = TokenReader.readInt(data);
            String damage = "";

            if (data.hasMoreElements()) {
                damage = TokenReader.readString(data);
            }

            mwclient.getMainFrame().getMainPanel().getHSPanel().showInfoWindow(filename, BV, gunnery, piloting, damage);
        } else if (cmd.equals("VURD")) {
            StringTokenizer data = new StringTokenizer(TokenReader.readString(st), "#");
            String filename = TokenReader.readString(data);
            String damage = TokenReader.readString(data);
            CUnit unit = new CUnit(mwclient);

            unit.setUnitFilename(filename);
            unit.createEntity();
            unit.setPilot(new Pilot("Jeeves", 4, 5));
            UnitUtils.applyBattleDamage(unit.getEntity(), damage, true);
            new AdvancedRepairDialog(mwclient, unit, unit.getEntity(), false);
        } else if (cmd.equals("CPPC")) {
            mwclient.getPlayer().getPartsCache().clear();
        } else if (cmd.equals("UDAO")) {
            mwclient.updateOpData(true);
            if (!mwclient.isDedicated()) {
                mwclient.getMainFrame().updateAttackMenu();
            }
        } else if (cmd.equals("RMF")) {
            mwclient.retrieveMul(TokenReader.readString(st));
        } else if (cmd.equals("SMFD")) {
            mwclient.getMainFrame().showMulFileList(TokenReader.readString(st));
        } else if (cmd.equals("CAFM")) {
            mwclient.getMainFrame().createArmyFromMul(TokenReader.readString(st));
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
            player.getMyHouse().setNonFactionUnitsCostMore(Boolean.parseBoolean(mwclient.getserverConfigs("UseNonFactionUnitsIncreasedTechs")));
        } else if (cmd.equals("SMA")) {
            mwclient.getPlayer().setMULCreatedArmy(st);
        } else if (cmd.equals("ANH")) {
            mwclient.createNewHouse(st);
        } else if (cmd.equals("RPF")) {
            int id = TokenReader.readInt(st);
            mwclient.getData().removeHouse(id);
        } else if (cmd.equals("UDT")) {
            mwclient.addToChat(TokenReader.readString(st), mwclient.getConfig().getIntParam("USERDEFINDMESSAGETAB"));
        } else if (cmd.equals("CCC")) {
            mwclient.getCampaign().setComponentConverter(st.nextToken());
        } else if (cmd.equals("SUD")) {
            try {
                StringBuilder userData = new StringBuilder(MWClient.CAMPAIGN_PREFIX + "c sendclientdata#");
                String clientMD5 = mwclient.createFilenameChecksum("./MekWarsClient.jar");
                String mmMD5 = mwclient.createFilenameChecksum("./MegaMek.jar");
                userData.append(mwclient.getClass().getProtectionDomain().getCodeSource().getLocation().toURI() + "#");
                userData.append(clientMD5 + "#");
                userData.append(mmMD5 + "#");

                String[] userDataSet =
                    { "user.name", "user.language", "user.country", "user.timezone", "os.name", "os.arch", "os.version", "java.version" };

                for (int pos = 0; pos < userDataSet.length; pos++) {
                    String property = System.getProperty(userDataSet[pos], "Unknown");
                    userData.append(property);
                    userData.append("#");
                }
                mwclient.sendChat(userData.toString());
            } catch (Exception ex) {
            }
        } else if (cmd.equals("ROP")) {
            mwclient.getPlayer().setAutoReorder(TokenReader.readBoolean(st));
        } else if (cmd.equals("SHP")) {
            player.parseHangarPenaltyString(TokenReader.readString(st));
        } else if (cmd.equals("STS")) {
        	int unitID = TokenReader.readInt(st);
        	int targetType = TokenReader.readInt(st);
        	//MWLogger.errLog("Setting Targeting for Unit " + unitID + " to " + targetType);
        	player.getUnit(unitID).setTargetSystem(targetType);
        	mwclient.doParseDataInput("CH|AM: Targeting for unit " + unitID + " set to " + player.getUnit(unitID).getTargetSystemTypeDesc());
        } else {
            return;
        }

        mwclient.refreshGUI(MWClient.REFRESH_HQPANEL);
        mwclient.refreshGUI(MWClient.REFRESH_PLAYERPANEL);
        mwclient.refreshGUI(MWClient.REFRESH_BMPANEL);
    }
}
