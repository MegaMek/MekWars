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

package mekwars.common.campaign;

import mekwars.common.BMEquipment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.CCommPanel;
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
    java.util.TreeMap<Integer, mekwars.common.campaign.CBMUnit> BlackMarket = new java.util.TreeMap<>();
    java.util.TreeMap<String, BMEquipment> BlackMarketParts = new java.util.TreeMap<>();
    java.util.TreeMap<String, ComponentToCritsConverter> ComponentConverter = new java.util.TreeMap<>();

    public CCampaign(IClient client) {
        this.client = client;
        Player = new CPlayer(this.client);
        java.io.File f = new java.io.File(IClient.CAMPAIGN_PATH);
        if (f.exists() && !f.isDirectory()) {
            f.delete();
        }

        if (!f.exists()) {
            try {f.mkdirs();} catch (Exception e) {MWLogger.errLog(e);}
        }
    }

    public boolean decodeCommand(String command) {
        java.util.StringTokenizer ST;
        String element;

        ST = new java.util.StringTokenizer(command, "|");
        element = TokenReader.readString(ST);
        command = command.substring(3);

        switch (element) {
            case "PS" -> {
                if (!Player.setData(command)) {
                    client.addToChat("Player data load failed!<br>");
                    return (false);
                }
                return (true);
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
                        //client.showInfoWindow("Your forces are under attack!");
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
                return (true);
            }
            case "CA" -> {
                if (!setData(command)) {
                    client.addToChat("<b>Error: Campaign data load failed.</b><br>");
                    return (false);
                }
                return (true);
            }
            case "PL" -> {
                if (!Player.decodeCommand(command)) {
                    client.addToChat("<b>Error: Player data load failed.</b><br>");
                    return (false);
                }
                return (true);
            }
            case "MS" -> {
                if (!showMsg(command)) {
                    client.addToChat("<b>Error: Message show failed.</b><br>");
                    return (false);
                }
                return (true);
            }
            case "ST" -> {
                if (!showStatus(command)) {
                    client.addToChat("<b>Error: Status show failed.</b><br>");
                    return (false);
                }
                return (true);
            }
        }

        client.addToChat("<b>Error: Wrong campaign command from server.</b><br>");
        return (false);
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
     * Method that reads data generated by Market2.getAutoMarketStatus() on the server. All data for all BM units sent
     * at once. "|" used to seperate units, * used to seperate fields inside each unit.
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
        CBMUnit bmUnit = new CBMUnit(command, this, Boolean.parseBoolean(client.getserverConfigs("HiddenBMUnits")));
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    /**
     * Method that repaces a CBMUnit. Called after a player bids on a unit in order to change colors and show amount.
     */
    public void changeBMUnit(String command) {
        CBMUnit bmUnit = new CBMUnit(command, this, Boolean.parseBoolean(client.getserverConfigs("HiddenBMUnits")));
        BlackMarket.remove(bmUnit.getAuctionID());
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    public CPlayer getPlayer() {
        return Player;
    }

    public void setPlayer(CPlayer player) {
        Player = player;
    }

    public java.util.TreeMap<Integer, mekwars.common.campaign.CBMUnit> getBlackMarket() {
        return BlackMarket;
    }

    public java.util.TreeMap<String, BMEquipment> getBlackMarketParts() {
        return BlackMarketParts;
    }

    public java.util.TreeMap<String, ComponentToCritsConverter> getComponentConverter() {
        return ComponentConverter;
    }

    public void setComponentConverter(String converterData) {
        try {
            java.util.StringTokenizer st = new java.util.StringTokenizer(converterData, "#");

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
