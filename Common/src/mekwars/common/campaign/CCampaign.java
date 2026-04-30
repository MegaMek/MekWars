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


package mekwars.common.campaign;

import java.io.File;
import java.util.StringTokenizer;
import java.util.TreeMap;

import mekwars.common.BMEquipment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.panels.CCommPanel;
import mekwars.common.gui.dialogs.ArmyViewerDialog;
import mekwars.common.util.ComponentToCritsConverter;
import mekwars.common.util.MWLogger;
import mekwars.common.util.TokenReader;

/**
 * Class for Campaign object used by Client
 * TODO: Rewrite command decoding. Its crazy right now.
 * TODO: Properly comment this class.
 */
public class CCampaign {

    IClient client;
    CPlayer Player;
    TreeMap<Integer, CBMUnit> BlackMarket = new TreeMap<>();
    TreeMap<String, BMEquipment> BlackMarketParts = new TreeMap<>();
    TreeMap<String, ComponentToCritsConverter> ComponentConverter = new TreeMap<>();

    public CCampaign(IClient client) {
        this.client = client;
        Player = new CPlayer(this.client);
        File f = new File(IClient.CAMPAIGN_PATH);

        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }

        if (!f.exists()) {
            try {
                f.mkdirs();
            } catch (Exception e) {
                MWLogger.errLog(e);
            }
        }
    }

    public void decodeCommand(String command) {
        StringTokenizer ST;
        String element;

        ST = new StringTokenizer(command, "|");
        element = TokenReader.readString(ST);
        command = command.substring(3);

        switch (element) {
            case "PS" -> {
                if (!Player.setData(command)) {
                    client.addToChat("Player data load failed!<br>");
                    return;
                }
                return;
            }
            case "CC" -> {
                // Campaign Command

                String commandId = TokenReader.readString(ST);
                if (commandId.equals("AT")) {//incoming attack
                    if (client.getConfig().isParam("ENABLEATTACKSOUND")) {
                        client.doPlaySound(client.getConfigParam("SOUNDONATTACK"));
                    }

                    client.addToChat("<font color=\"red\"><b>Your forces are under attack!</b></font>",
                          CCommPanel.CHANNEL_HMAIL);
                    client.addToChat("<font color=\"red\"><b>Your forces are under attack!</b></font>",
                          CCommPanel.CHANNEL_PMAIL,
                          "Server");
                    if (client.getConfig().isParam("POPUPONATTACK")) {
                        int opID = TokenReader.readInt(ST);
                        int teams = TokenReader.readInt(ST);
                        new ArmyViewerDialog(client, null, ST, ArmyViewerDialog.AVD_DEFEND, null, null, opID, teams);
                    }
                }
                if (commandId.equals("NT")) {//next tick
                    int time = TokenReader.readInt(ST);
                    boolean decrement = TokenReader.readBoolean(ST);
                    client.processTick(time);

                    /*
                     * Decrements tick counters for units without explicit auction
                     * length being sent from the server to save a bit of bandwidth.
                     */
                    if (decrement) {
                        for (CBMUnit currUnit : BlackMarket.values()) {
                            currUnit.decrementSalesTicks();
                        }
                        client.refreshGUI(IClient.REFRESH_BM_PANEL);
                    }
                }
                return;
            }
            case "CA" -> {
                if (!setData(command)) {
                    client.addToChat("<b>Error: Campaign data load failed.</b><br>");
                    return;
                }
                return;
            }
            case "PL" -> {
                if (!Player.decodeCommand(command)) {
                    client.addToChat("<b>Error: Player data load failed.</b><br>");
                    return;
                }
                return;
            }
            case "MS" -> {
                if (!showMsg(command)) {
                    client.addToChat("<b>Error: Message show failed.</b><br>");
                    return;
                }
                return;
            }
            case "ST" -> {
                if (!showStatus(command)) {
                    client.addToChat("<b>Error: Status show failed.</b><br>");
                    return;
                }
                return;
            }
        }

        client.addToChat("<b>Error: Wrong campaign command from server.</b><br>");
    }

    protected boolean setData(String command) {
        return (true);
    }

    protected boolean showMsg(String command) {
        return (true);
    }

    protected boolean showStatus(String command) {
        client.addToChat(command);
        return (true);
    }

    /**
     * Method that reads data generated by Market2.getAutoMarketStatus() on the server. All data for all BM units is
     * sent at once. "|" used to separate units, * used to separate fields inside each unit.
     */
    public void setBMData(String command) {

        //create tokenizer
        java.util.StringTokenizer mainTokenizer = new java.util.StringTokenizer(command, "$");

        //clear all current BM data
        BlackMarket.clear();

        while (mainTokenizer.hasMoreTokens()) {
            boolean hidden = Boolean.parseBoolean(client.getServerConfigs("HiddenBMUnits"));
            CBMUnit currBMUnit = new CBMUnit(TokenReader.readString(mainTokenizer), this, hidden);
            BlackMarket.put(currBMUnit.getAuctionID(), currBMUnit);
        }
    }

    /**
     * Method that removes a unit from the Client's BM representation.
     */
    public void removeBMUnit(String command) {
        BlackMarket.remove(Integer.valueOf(command));
    }

    /**
     * Method that adds a unit to the client's BM representation.
     */
    public void addBMUnit(String command) {
        CBMUnit bmUnit = new CBMUnit(command, this, Boolean.parseBoolean(client.getServerConfigs("HiddenBMUnits")));
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    /**
     * Method that repaces a CBMUnit. Called after a player bids on a unit in order to change colors and show amount.
     */
    public void changeBMUnit(String command) {
        CBMUnit bmUnit = new CBMUnit(command, this, Boolean.parseBoolean(client.getServerConfigs("HiddenBMUnits")));
        BlackMarket.remove(bmUnit.getAuctionID());
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    public CPlayer getPlayer() {
        return Player;
    }

    public void setPlayer(CPlayer player) {
        Player = player;
    }

    public TreeMap<Integer, CBMUnit> getBlackMarket() {
        return BlackMarket;
    }

    public TreeMap<String, BMEquipment> getBlackMarketParts() {
        return BlackMarketParts;
    }

    public TreeMap<String, ComponentToCritsConverter> getComponentConverter() {
        return ComponentConverter;
    }

    public void setComponentConverter(String converterData) {
        try {
            StringTokenizer st = new StringTokenizer(converterData, "#");

            ComponentConverter.clear();
            while (st.hasMoreTokens()) {
                ComponentToCritsConverter converter = new ComponentToCritsConverter();
                converter.setCritName(TokenReader.readString(st));
                converter.setMinCritLevel(TokenReader.readInt(st));
                converter.setComponentUsedType(TokenReader.readInt(st));
                converter.setComponentUsedWeight(TokenReader.readInt(st));
                ComponentConverter.put(converter.getCritName(), converter);
            }
            client.setWaiting(false);
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }
}
