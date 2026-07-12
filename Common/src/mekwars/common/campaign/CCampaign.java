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

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.BMEquipment;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.ArmyViewerDialog;
import mekwars.common.gui.panels.CCommPanel;
import mekwars.common.util.ComponentToCritsConverter;
import mekwars.common.util.TokenReader;

/**
 * Class for Campaign object used by client
 * TODO: Rewrite command decoding. Its crazy right now.
 * TODO: Properly comment this class.
 * <p>
 * CCampaign is the client's top-level container for the state of a single MekWars campaign session: it owns the
 * (single) {@link CPlayer} representing the locally logged-in player, the client-side Black Market listings
 * ({@link CBMUnit}) and their tradeable parts ({@link BMEquipment}), and the component-to-criticals conversion table
 * used when interpreting salvage/part data. It is also the entry point for decoding top-level server protocol
 * messages ("PS|", "CC|", "CA|", "PL|", "MS|", etc.) received over the network via {@link #decodeCommand(String)},
 * dispatching player-specific sub-commands down to {@link CPlayer#decodeCommand(String)}.
 * <p>
 * Only one CCampaign/CPlayer pair exists per running client (see {@link IClient} usage), representing "my" side of
 * the connection; other users are tracked separately as {@link CUser} objects.
 */
public class CCampaign {
    private final static MMLogger LOGGER = MMLogger.create(CCampaign.class);

    /** The client connection this campaign belongs to; used for chat output, sounds, config lookups, and GUI refresh requests. */
    private final IClient client;
    /** Active Black Market auction listings, keyed by auction ID. Wholesale-replaced by {@link #setBMData}, incrementally updated by {@link #addBMUnit}/{@link #removeBMUnit}/{@link #changeBMUnit}. */
    private final TreeMap<Integer, CBMUnit> BlackMarket = new TreeMap<>();
    /** Tradeable equipment/parts currently available on the Black Market, keyed by part name. */
    private final TreeMap<String, BMEquipment> BlackMarketParts = new TreeMap<>();
    /** Lookup table mapping a critical-slot name to the rules for converting a salvaged component into that many crits, keyed by crit name. */
    private final TreeMap<String, ComponentToCritsConverter> ComponentConverter = new TreeMap<>();
    /** The single locally-controlled player's in-campaign state (units, house, personnel, etc.). */
    private CPlayer Player;

    /**
     * Creates a new campaign for the given client connection, immediately constructing an empty {@link CPlayer}, and
     * ensures the local on-disk campaign data directory ({@link IClient#CAMPAIGN_PATH}) exists. If a plain file (not
     * a directory) is found at that path, it is deleted before the directory is (re)created; directory-creation
     * failures are logged but not rethrown.
     *
     * @param client the client connection this campaign is attached to.
     */
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
                LOGGER.error(e, "Error creating campaign directory:");
            }
        }
    }

    /**
     * Top-level dispatcher for server protocol commands. The incoming line is of the form {@code "XX|rest..."}
     * where {@code XX} (read via the first "|"-delimited token) selects the command family:
     * <ul>
     *   <li>{@code PS} — full player-state snapshot, delegated to {@link CPlayer#setData(String)}.</li>
     *   <li>{@code CC} — a "Campaign Command" sub-switch handling server pushes like an incoming attack
     *       notification ("AT", optionally playing a sound and/or popping an {@link ArmyViewerDialog}) and the
     *       "next tick" heartbeat ("NT", which advances the client's clock and optionally decrements Black Market
     *       sales-tick counters).</li>
     *   <li>{@code CA} — campaign data, delegated to {@link #setData(String)} (currently a no-op stub that always
     *       returns true).</li>
     *   <li>{@code PL} — player sub-commands, delegated to {@link CPlayer#decodeCommand(String)}.</li>
     *   <li>{@code MS} — a message to show, delegated to {@link #showMsg(String)} (currently a no-op stub).</li>
     *   <li>{@code stringTokenizer} — delegated to {@link #showStatus(String)}. This branch is almost certainly a
     *       leftover placeholder/typo: the literal token the server would need to send is the string
     *       {@code "stringTokenizer"}, which does not match any real two-letter command code used elsewhere in this
     *       method, so this case is effectively dead/unreachable in practice.</li>
     * </ul>
     * Note: {@code command.substring(3)} unconditionally strips the first 3 characters (assumed to be the 2-letter
     * code plus its delimiter) before the switch even runs, so the sub-handlers receive the command body without
     * needing to re-parse the leading code themselves. Any unmatched command falls through to a generic chat error
     * message.
     *
     * @param command the raw, undecoded command line received from the server (already known to start with a
     *       2-letter code and a delimiter).
     */
    public void decodeCommand(String command) {
        StringTokenizer stringTokenizer;
        String element;

        stringTokenizer = new StringTokenizer(command, "|");
        element = TokenReader.readString(stringTokenizer);
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
                String commandId = TokenReader.readString(stringTokenizer);
                if (commandId.equals("AT")) {//incoming attack
                    if (client.getConfig().isParam("ENABLE_ATTACK_SOUND")) {
                        client.doPlaySound(client.getConfigParam("SOUND_ON_ATTACK"));
                    }

                    client.addToChat("<font color=\"red\"><b>Your forces are under attack!</b></font>",
                          CCommPanel.CHANNEL_HOUSE_MAIL);
                    client.addToChat("<font color=\"red\"><b>Your forces are under attack!</b></font>",
                          CCommPanel.CHANNEL_PRIVATE_MAIL,
                          "Server");
                    if (client.getConfig().isParam("POP_UP_ON_ATTACK")) {
                        int opID = TokenReader.readInt(stringTokenizer);
                        int teams = TokenReader.readInt(stringTokenizer);
                        new ArmyViewerDialog(client,
                              null,
                              stringTokenizer,
                              ArmyViewerDialog.AVD_DEFEND,
                              null,
                              null,
                              opID,
                              teams);
                    }
                }
                if (commandId.equals("NT")) {//next tick
                    int time = TokenReader.readInt(stringTokenizer);
                    boolean decrement = TokenReader.readBoolean(stringTokenizer);
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
            case "stringTokenizer" -> {
                if (!showStatus(command)) {
                    client.addToChat("<b>Error: Status show failed.</b><br>");
                    return;
                }
                return;
            }
        }

        client.addToChat("<b>Error: Wrong campaign command from server.</b><br>");
    }

    /**
     * Handles a "CA" (campaign data) command. Currently an unimplemented stub: it ignores the command body entirely
     * and always reports success.
     *
     * @param command the command body (unused).
     * @return always {@code true}.
     */
    protected boolean setData(String command) {
        return true;
    }

    /**
     * Handles an "MS" (show message) command. Currently an unimplemented stub: it ignores the command body entirely
     * and always reports success.
     *
     * @param command the command body (unused).
     * @return always {@code true}.
     */
    protected boolean showMsg(String command) {
        return true;
    }

    /**
     * Handles a status-display command by echoing the raw command text directly into the client's chat window.
     *
     * @param command the text to display.
     * @return always {@code true}.
     */
    protected boolean showStatus(String command) {
        client.addToChat(command);
        return true;
    }

    /**
     * Method that reads data generated by Market2.getAutoMarketStatus() on the server. All data for all BM units is
     * sent at once. "|" used to separate units, * used to separate fields inside each unit.
     * <p>
     * Note: despite the Javadoc claim above and the field name, this method actually splits on {@code "$"}
     * (matching {@link CBMUnit}'s own per-unit "*"-delimited sub-parsing). This wholesale-replaces the entire
     * {@link #BlackMarket} map — all previous listings are cleared before the new ones are parsed in.
     *
     * @param command the full "$"-delimited Black Market status payload.
     */
    public void setBMData(String command) {

        //create tokenizer
        StringTokenizer mainTokenizer = new StringTokenizer(command, "$");

        //clear all current BM data
        BlackMarket.clear();

        while (mainTokenizer.hasMoreTokens()) {
            boolean hidden = MathUtility.parseBoolean(client.getServerConfigs("HiddenBMUnits"), false);
            CBMUnit currBMUnit = new CBMUnit(TokenReader.readString(mainTokenizer), this, hidden);
            BlackMarket.put(currBMUnit.getAuctionID(), currBMUnit);
        }
    }

    /**
     * Method that removes a unit from the client's BM representation.
     *
     * @param command the auction ID (as a string) of the listing to remove.
     */
    public void removeBMUnit(String command) {
        BlackMarket.remove(Integer.valueOf(command));
    }

    /**
     * Method that adds a unit to the client's BM representation. Whether the newly constructed {@link CBMUnit}
     * hides its embedded unit details is controlled by the server's "HiddenBMUnits" config flag.
     *
     * @param command a single unit's "*"-delimited listing data.
     */
    public void addBMUnit(String command) {
        CBMUnit bmUnit = new CBMUnit(command,
              this,
              MathUtility.parseBoolean(client.getServerConfigs("HiddenBMUnits"), false));
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    /**
     * Method that repaces a CBMUnit. Called after a player bids on a unit to change colors and show amount.
     * Functionally equivalent to {@link #addBMUnit(String)} (both remove then insert under the same auction ID key)
     * but kept as a separate method to make the calling intent — "bid placed, refresh this listing" — explicit at
     * call sites.
     *
     * @param command a single unit's "*"-delimited listing data, with updated bid information.
     */
    public void changeBMUnit(String command) {
        CBMUnit bmUnit = new CBMUnit(command,
              this,
              MathUtility.parseBoolean(client.getServerConfigs("HiddenBMUnits"), false));
        BlackMarket.remove(bmUnit.getAuctionID());
        BlackMarket.put(bmUnit.getAuctionID(), bmUnit);
    }

    /** @return the locally-controlled player's campaign state. */
    public CPlayer getPlayer() {
        return Player;
    }

    /** Replaces the locally-controlled player's campaign state wholesale. */
    public void setPlayer(CPlayer player) {
        Player = player;
    }

    /** @return the live map of active Black Market auction listings, keyed by auction ID. */
    public TreeMap<Integer, CBMUnit> getBlackMarket() {
        return BlackMarket;
    }

    /** @return the live map of tradeable Black Market equipment/parts, keyed by part name. */
    public TreeMap<String, BMEquipment> getBlackMarketParts() {
        return BlackMarketParts;
    }

    /** @return the live component-to-criticals conversion lookup table, keyed by critical-slot name. */
    public TreeMap<String, ComponentToCritsConverter> getComponentConverter() {
        return ComponentConverter;
    }

    /**
     * Parses a "#"-delimited server payload describing the full set of component-to-criticals conversion rules
     * (used when translating salvaged equipment into generic critical slots) and wholesale-replaces
     * {@link #ComponentConverter} with the result. Signals the client that a pending wait (e.g. a modal "loading"
     * state) is complete via {@link IClient#setWaiting(boolean)} once parsing succeeds. Parsing exceptions are
     * caught and logged rather than propagated, leaving the table in a possibly partially-updated state.
     *
     * @param converterData the "#"-delimited conversion rule payload from the server.
     */
    public void setComponentConverter(String converterData) {
        try {
            StringTokenizer stringTokenizer = new StringTokenizer(converterData, "#");

            ComponentConverter.clear();
            while (stringTokenizer.hasMoreTokens()) {
                ComponentToCritsConverter converter = new ComponentToCritsConverter();
                converter.setCritName(TokenReader.readString(stringTokenizer));
                converter.setMinCritLevel(TokenReader.readInt(stringTokenizer));
                converter.setComponentUsedType(TokenReader.readInt(stringTokenizer));
                converter.setComponentUsedWeight(TokenReader.readInt(stringTokenizer));
                ComponentConverter.put(converter.getCritName(), converter);
            }
            client.setWaiting(false);
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to set component converter: {}", ex.getLocalizedMessage());
        }
    }
}
