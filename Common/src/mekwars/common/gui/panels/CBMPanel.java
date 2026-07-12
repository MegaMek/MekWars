/*
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

package mekwars.common.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;

import megamek.codeUtilities.MathUtility;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekwars.common.campaign.CBMUnit;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.MekInfo;
import mekwars.common.gui.TableSorter;
import mekwars.common.gui.dialogs.SellUnitDialog;
import mekwars.common.gui.models.BlackMarketModel;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Backs the "Black Market" tab of the main client window (added to {@link CMainPanel} when {@code BM_TAB_VISIBLE}
 * is set). Shows a sortable table of units currently listed on the in-campaign black market — sourced from
 * {@link BlackMarketModel} (via a {@link TableSorter} wrapper) — and a row of buttons for the actions a player can
 * take on the selected listing: view the unit's details ("Show Unit"), bid on someone else's listing ("Place Bid"),
 * retract the player's own bid ("Retract Bid"), pull the player's own unit off the market ("Remove Unit"), or list
 * a new unit for sale ("Sell Unit", which opens {@link SellUnitDialog}). Each button's enabled state is recomputed
 * whenever the table selection changes, based on whether the selected listing belongs to the player, whether they
 * already have a bid on it, and whether their faction is currently permitted to buy/sell (see
 * {@link #checkFactionAccess()}).
 */

public class CBMPanel extends JPanel {
    private static final MMLogger LOGGER = MMLogger.create(CBMPanel.class);

    @Serial
    private static final long serialVersionUID = -432087180209544906L;
    /** The visible black-market listings table; its model is the {@link TableSorter}-wrapped {@link #BlackMarketInfo}. */
    private final JTable tblMarket = new JTable();
    /** Opens a detail viewer for the selected unit. Disabled entirely when {@link #hideBMUnits} is true. */
    private final JButton btnShowMek = new JButton();
    /** Retracts the player's bid on the selected listing. */
    private final JButton btnRecallBid = new JButton();
    /** Pulls the player's own unit off the market (only enabled when the player is the seller of the selection). */
    private final JButton btnRecallUnit = new JButton();
    /** Opens {@link SellUnitDialog} to list a new unit for sale. */
    private final JButton btnSellUnit = new JButton();
    /** Places a bid on the selected listing (only enabled when the player is not the seller and faction bidding is allowed). */
    private final JButton btnBid = new JButton();
    /** Container holding the button row (and, optionally, the unit preview icon); its contents are rebuilt by {@link #resetButtonBar()}. */
    private final JPanel pnlBuyButtons = new JPanel();
    /** Fixed-width spacer placed between the "Show Unit" and "Place Bid" buttons. */
    private final JPanel spacingPanel1;
    /** Fixed-width spacer placed between the "Retract Bid" and "Remove Unit" buttons. */
    private final JPanel spacingPanel2;
    /** Wraps {@link #pnlMekIcon} so the preview image can be swapped out via {@link #resetCamo()}. */
    private final JPanel pnlMekIconHolder;
    /** Server config flag ({@code HiddenBMUnits}): when true, unit identities/camo are hidden and "Show Unit" is disabled entirely. */
    private final boolean hideBMUnits;
    /** The player's campaign, used to resolve auction ids to {@link CBMUnit} listings. */
    private final CCampaign theCampaign;
    /** Client session, used for config lookups, server config lookups, and sending market chat commands. */
    private final IClient client;
    /** The local player, used to check faction restrictions on buying/selling. */
    private final CPlayer Player;
    /** Table model backing the market listings; sourced from the server's current black-market state. */
    public BlackMarketModel BlackMarketInfo;
    /** Unit preview icon (camo/mech silhouette) shown for the selected listing, when {@code BM_PREVIEW_IMAGE} is enabled and units aren't hidden. */
    private JPanel pnlMekIcon;
    /** Cached result of the faction buy-restriction check from {@link #checkFactionAccess()}; re-applied to {@link #btnBid} on each new selection so bidding stays correctly disabled after a faction change (e.g. defection). */
    private boolean factionBidsAllowed = true;
    /** The market listing currently under the mouse/selection; {@code null} when nothing usable is selected. */
    private CBMUnit cbmUnit;

    /**
     * Builds the market table (sortable via {@link TableSorter}), the row-selection listener that enables/disables
     * the action buttons based on the selected listing, and the button bar itself (via {@link #resetButtonBar()}).
     * Also creates the initial unit-preview icon and triggers a first {@link #refresh()} of the listings.
     *
     * @param client the active client session
     */
    public CBMPanel(IClient client) {
        setLayout(new GridBagLayout());
        this.client = client;
        hideBMUnits = MathUtility.parseBoolean(this.client.getServerConfigs("HiddenBMUnits"), false);

        pnlMekIconHolder = new JPanel();
        pnlMekIconHolder.setMaximumSize(new Dimension(84, 72));
        pnlMekIconHolder.setMaximumSize(new Dimension(84, 72));

        pnlMekIcon = new MekInfo(this.client.getConfig().getImage("CAMO"));
        pnlMekIcon.setMinimumSize(new Dimension(84, 72));
        pnlMekIcon.setPreferredSize(new Dimension(84, 72));
        pnlMekIcon.setMaximumSize(new Dimension(84, 72));

        BlackMarketInfo = new BlackMarketModel(this.client, hideBMUnits);
        theCampaign = client.getCampaign();
        Player = theCampaign.getPlayer();
        TableSorter sorter = new TableSorter(BlackMarketInfo, client, TableSorter.SORTER_BM);
        tblMarket.setModel(sorter);

        //double-clicking a listing opens the same unit-detail viewer as pressing "Show Unit"
        tblMarket.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    btnShowMekActionPerformed(new ActionEvent(btnShowMek, 0, ""));
                }
            }
        });

        BlackMarketInfo.initColumnSizes(tblMarket);

        for (int i = 0; i < BlackMarketInfo.getColumnCount(); i++) {
            tblMarket.getColumnModel().getColumn(i).setCellRenderer(BlackMarketInfo.getRenderer());
        }

        sorter.addMouseListenerToHeaderInTable(tblMarket);
        tblMarket.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = tblMarket.getSelectionModel();
        //Recomputes which action buttons are enabled whenever the row selection changes; fires again after the
        //drag-in-progress adjustment settles. NOTE: when the selection becomes empty, this hides the preview
        //image but does NOT return early -- it falls through to getMinSelectionIndex() (-1) and then
        //tblMarket.getModel().getValueAt(-1, ...) below, which throws an ArrayIndexOutOfBoundsException. Since
        //this is a ListSelectionListener callback, the exception is caught and reported by Swing's default
        //uncaught-exception handler rather than crashing the client, but the button-enable/disable logic is
        //simply skipped in that case.
        rowSM.addListSelectionListener(event -> {

            //ignore dragging
            if (event.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) event.getSource();

            if (lsm.isSelectionEmpty()) {
                ((MekInfo) pnlMekIcon).setImageVisible(false);
            }

            int selectedRow = lsm.getMinSelectionIndex();
            Integer auctionId = (Integer) tblMarket.getModel().getValueAt(selectedRow, BlackMarketModel.AUCTION_ID);

            if (auctionId != null) {
                cbmUnit = getMarketMechAtRow(tblMarket.getSelectedRow());

                //if there is a unit in the row, update the dynamic buttons.
                if (cbmUnit != null) {
                    if (!hideBMUnits) {
                        btnShowMek.setEnabled(true);
                    }

                    btnRecallBid.setEnabled(cbmUnit.getBid() > 0);

                    if (cbmUnit.playerIsSeller()) {
                        btnBid.setEnabled(false);
                        btnRecallUnit.setEnabled(true);
                    } else {
                        //check cached faction ban value
                        btnBid.setEnabled(factionBidsAllowed);
                        btnRecallUnit.setEnabled(false);
                    }

                    //refresh the camo ... may have changed.
                    //Note: this reaches back through client -> main frame -> main panel -> BM panel instead of
                    //just calling CBMPanel.this.resetCamo() directly; in normal operation that chain resolves
                    //back to this same panel instance, so the roundabout call is equivalent to a direct one.
                    CBMPanel.this.client.getMainFrame().getMainPanel().getBMPanel().resetCamo();

                } else {//dim them all
                    btnRecallBid.setEnabled(false);
                    btnRecallUnit.setEnabled(false);
                    btnBid.setEnabled(false);
                    btnShowMek.setEnabled(false);
                }
            }

        });

        JPanel pnlBuy = new JPanel();
        pnlBuy.setLayout(new GridBagLayout());

        JScrollPane spMarket = new JScrollPane();
        spMarket.setToolTipText("Click on the column header to sort.");
        spMarket.setPreferredSize(new Dimension(300, 370));
        tblMarket.setDoubleBuffered(true);
        spMarket.setViewportView(tblMarket);
        GridBagConstraints gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        pnlBuy.add(spMarket, gridBagConstraints);

        //lay out the buttons
        JPanel panelButtonWrapper = new JPanel();
        pnlBuyButtons.setLayout(new SpringLayout());

        btnShowMek.setText("Show Unit");
        btnShowMek.addActionListener(this::btnShowMekActionPerformed);

        spacingPanel1 = new JPanel();
        spacingPanel1.setMaximumSize(new Dimension(20, 1));

        btnBid.setText("Place Bid");
        btnBid.setEnabled(false);
        btnBid.addActionListener(this::btnBidActionPerformed);

        btnRecallBid.setText("Retract Bid");
        btnRecallBid.setEnabled(false);
        btnRecallBid.addActionListener(this::btnRecallBidActionPerformed);

        spacingPanel2 = new JPanel();
        spacingPanel2.setMaximumSize(new Dimension(20, 1));

        btnRecallUnit.setText("Remove Unit");
        btnRecallUnit.setEnabled(false);
        btnRecallUnit.addActionListener(this::btnRecallUnitActionPerformed);

        btnSellUnit.setText("Sell Unit");
        btnSellUnit.addActionListener(this::btnSellUnitActionPerformed);

        JPanel spacingPanel3 = new JPanel();
        spacingPanel3.setMaximumSize(new Dimension(20, 1));

        //do the actual button layout, spring management, etc.
        resetButtonBar();

        panelButtonWrapper.add(pnlBuyButtons);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        pnlBuy.add(panelButtonWrapper, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 30;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        this.add(pnlBuy, gridBagConstraints);

        refresh();
    }

    /**
     * Called from the "Show Unit" button (and from the table's double-click handler). Opens a standalone,
     * fixed-size, non-resizable {@link JFrame} containing a {@link MWUnitDisplay} showing the full details
     * (weapons, armor, etc.) of the unit at the currently selected row. Loads the entity's weapons first since the
     * embedded unit data may not have them pre-loaded. Does nothing if {@link #hideBMUnits} is set (server-hidden
     * market) or if the selected row has no resolvable unit.
     *
     * @param evt the triggering action event (its source is unused; the selection is re-read from {@link #tblMarket})
     */
    private void btnShowMekActionPerformed(ActionEvent evt) {
        if (hideBMUnits) {
            return;
        }

        cbmUnit = getMarketMechAtRow(tblMarket.getSelectedRow());

        if (cbmUnit == null) {
            return;
        }

        Entity theEntity = cbmUnit.getEmbeddedUnit().getEntity();
        theEntity.loadAllWeapons();

        JFrame infoWindow = new JFrame();
        MWUnitDisplay unitDisplay = new MWUnitDisplay(null, client);

        infoWindow.getContentPane().add(unitDisplay);
        infoWindow.setSize(300, 400);
        infoWindow.setResizable(false);

        infoWindow.setTitle(cbmUnit.getModelName());
        infoWindow.setLocationRelativeTo(client.getMainFrame());//center it
        infoWindow.setVisible(true);
        unitDisplay.displayEntity(theEntity);
    }

    /**
     * Resolves the market listing ({@link CBMUnit}) for a given table row by reading its auction id column and
     * looking it up in the campaign's black market. Also updates {@link #cbmUnit} as a side effect.
     *
     * @param row the table row index (as displayed, i.e. sorted order)
     * @return the listing at that row, or {@code null} if the row has no auction id or it isn't found in the campaign's market
     */
    public CBMUnit getMarketMechAtRow(int row) {
        cbmUnit = null;
        Integer auctionId = (Integer) tblMarket.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID);

        if (auctionId != null) {
            cbmUnit = theCampaign.getBlackMarket().get(auctionId);
        }

        return cbmUnit;
    }

    /**
     * Rebuilds the unit preview icon ({@link #pnlMekIcon}) to reflect the currently selected listing's camo/unit
     * appearance, or hides the preview entirely if units are hidden or the preview feature is disabled. Called
     * whenever the selection changes and whenever the market data (and thus available camo) might have changed.
     * If no unit is currently selected, {@link MekInfo#setUnit} throws and is caught/logged at debug level — this
     * is the normal, expected path immediately after construction or when nothing is selected.
     */
    //refresh preview image
    public void resetCamo() {

        if (!hideBMUnits && client.getConfig().isParam("BM_PREVIEW_IMAGE")) {

            //refresh the camo ... may have been changed.
            pnlMekIcon = new MekInfo(client.getConfig().getImage("CAMO"));
            pnlMekIcon.setMinimumSize(new Dimension(84, 72));
            pnlMekIcon.setPreferredSize(new Dimension(84, 72));
            pnlMekIcon.setMaximumSize(new Dimension(84, 72));

            try {
                ((MekInfo) pnlMekIcon).setUnit(cbmUnit.getEmbeddedUnit().getEntity());
                ((MekInfo) pnlMekIcon).setImageVisible(true);
            } catch (Exception e) {
                LOGGER.debug(e, "Entity not selected yet. {}", e.getLocalizedMessage());
            }

            pnlMekIconHolder.removeAll();
            pnlMekIconHolder.add(pnlMekIcon);

        } else {
            ((MekInfo) pnlMekIcon).setImageVisible(false);
        }

        pnlBuyButtons.validate();
    }

    /**
     * Called from the "Place Bid" button. Prompts the player (via {@link JOptionPane#showInputDialog}) for a bid
     * amount on the selected listing, validates it against the listing's minimum bid, and if valid sends a
     * {@code /c bid#<auctionId>#<amount>} chat command to the server. On success (or on invalid input that isn't a
     * parse failure), disables the bid/retract buttons since the selection state is about to change. Cancelling
     * the dialog (blank/cancelled input) silently aborts.
     * <p>
     * <b>Quirk:</b> {@code cbmUnit.playerIsSeller()} is called before the {@code cbmUnit != null} check just below
     * it, so if {@link #getMarketMechAtRow} somehow returns {@code null} for a non-negative selected row (e.g. a
     * race with the market refreshing), this throws a {@link NullPointerException} instead of returning quietly
     * like the row-check above does.
     *
     * @param evt the triggering action event (unused; the selection is re-read from {@link #tblMarket})
     */
    private void btnBidActionPerformed(ActionEvent evt) {
        int row = tblMarket.getSelectedRow();

        //shouldn't ever happen, but still trap
        if (row < 0) {
            return;
        }

        //get the unit
        cbmUnit = getMarketMechAtRow(tblMarket.getSelectedRow());

        //also shouldn't ever happen but, again, catch it
        if (cbmUnit.playerIsSeller()) {
            return;
        }

        if (cbmUnit != null) {
            int auctionID = cbmUnit.getAuctionID();
            if (auctionID != -1) {//-1 is the default value. indicates null auction.

                //generate a new option dialog
                String playerBidString = JOptionPane.showInputDialog(client.getMainFrame(),
                      String.format("<HTML><center>How much would you like to bid on the %s?<BR>Minimum is %s.</center></HTML>", cbmUnit.getModelName(), client.moneyOrFluMessage(
                            true,
                            true,
                            cbmUnit.getMinBid())),
                      "Amount to Bid",
                      JOptionPane.PLAIN_MESSAGE);

                //Clicked Cancel
                if ((playerBidString == null) || (playerBidString.trim().isEmpty())) {
                    return;
                }

                try {
                    int playerBid = MathUtility.parseInt(playerBidString, -1);

                    if (playerBid < cbmUnit.getMinBid()) {
                        String toUser = new StringBuilder("CH|CLIENT: You tried to bid less than the minimum your contacts are willing to accept for the ")
                              .append(cbmUnit.getModelName())
                              .append(". Try bidding ")
                              .append(client.moneyOrFluMessage(true, false, cbmUnit.getMinBid()))
                              .append(" or more.")
                              .toString();
                        client.doParseDataInput(toUser);
                        return;
                    }

                    client.sendChat(String.format("%sc bid#%s#%s", IClient.CAMPAIGN_PREFIX, auctionID, playerBid));

                    //this clears the selection, so unhighlight the bid button. sometimes the
                    //bid and retract buttons will be active simultaneously, so disable both
                    btnBid.setEnabled(false);
                    btnRecallBid.setEnabled(false);
                } catch (Exception ex) {
                    LOGGER.error(ex, "Unhandled exception: {}", ex.getLocalizedMessage());
                }
            }
        }
    }//end btnBidActionPerformed

    /**
     * Called by action listener. Retracts a bid placed on a unit.
     */
    private void btnRecallBidActionPerformed(ActionEvent evt) {

        cbmUnit = getMarketMechAtRow(tblMarket.getSelectedRow());

        //break out if no selection
        if (cbmUnit == null) {
            return;
        }

        //no bid. return.
        if (tblMarket.getValueAt(tblMarket.getSelectedRow(), BlackMarketModel.BID) == null) {
            return;
        }

        //returns passed. send the recall command and deselect the buttons.
        client.sendChat(String.format("%sc recallbid#%s", IClient.CAMPAIGN_PREFIX, cbmUnit.getAuctionID()));
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);

    }

    /**
     * Called from the "Remove Unit" button. Pulls the player's own unit listing off the market by sending a
     * {@code /c recall#<auctionId>} chat command, but only if the selected listing actually belongs to the player.
     * Disables the bid-related buttons afterward since the selection state is about to change.
     *
     * @param evt the triggering action event (unused; the selection is re-read from {@link #tblMarket})
     */
    private void btnRecallUnitActionPerformed(ActionEvent evt) {

        cbmUnit = getMarketMechAtRow(tblMarket.getSelectedRow());

        //break out if no selection
        if (cbmUnit == null) {
            return;
        }

        //not the players unit, so he cant terminate the sale
        if (!cbmUnit.playerIsSeller()) {
            return;
        }

        //returns passed. send the recall command and deselect the buttons.
        client.sendChat(String.format("%sc recall#%s", IClient.CAMPAIGN_PREFIX, cbmUnit.getAuctionID()));
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);
    }

    /**
     * Called from an action listener. Creates a SellUnitDialog. The dialog does all the work ;-)
     */
    public void btnSellUnitActionPerformed(ActionEvent evt) {
        SellUnitDialog sud = new SellUnitDialog(null, client, null);
        sud.setVisible(true);
    }

    /**
     * Rebuilds the button bar layout, in one of two forms depending on whether {@code BM_PREVIEW_IMAGE} is enabled:
     * with the preview form, buttons go in their own {@code SpringLayout} panel alongside the unit-preview icon
     * (via {@link #resetCamo()}); without it, the buttons alone fill {@link #pnlBuyButtons} directly. Called once
     * from the constructor and again any time the preview-image config might have changed.
     * <p>
     * <b>Bug:</b> in both branches, {@code btnBid} is added to the layout twice in a row (once, then again after
     * the spacer) instead of the second call adding a different, presumably-intended component. Because a
     * {@link javax.swing.JComponent} can only have one parent, the second {@code add} call simply re-parents
     * {@code btnBid} to its later position rather than creating a duplicate, so this doesn't crash — but it means
     * the button bar ends up with only 6 or 7 distinct children even though
     * {@link SpringLayoutHelper#setupSpringGrid} is told to lay out a fixed 1x8 grid, leaving one grid cell
     * effectively empty/misaligned.
     */
    public void resetButtonBar() {

        if (client.getConfig().isParam("BM_PREVIEW_IMAGE")) {

            /*
             * Pain in the ass layout. Have to keep the
             * button heights down, etc. Using a max height
             * isn't preferable, since it can screw up the
             * layout when no preview is in use ... so we
             * work with stupidly embedded panels instead.
             */

            //clear panels
            pnlBuyButtons.removeAll();
            pnlMekIconHolder.removeAll();

            //standard spring layout for the buttons
            JPanel buttonSpring = new JPanel(new SpringLayout());

            if (!hideBMUnits) {
                buttonSpring.add(btnShowMek);
            }

            buttonSpring.add(btnBid);
            buttonSpring.add(spacingPanel1);
            buttonSpring.add(btnBid);
            buttonSpring.add(btnRecallBid);
            buttonSpring.add(spacingPanel2);
            buttonSpring.add(btnRecallUnit);
            buttonSpring.add(btnSellUnit);

            //refresh the camo, so an image shows if possible
            resetCamo();

            //stick the pnlMekIcon in its holder, and add
            //the combined struct inot the button bar.
            pnlMekIconHolder.add(pnlMekIcon);
            pnlBuyButtons.add(pnlMekIconHolder);

            SpringLayoutHelper.setupSpringGrid(buttonSpring, 1, 8);
            pnlBuyButtons.add(new javax.swing.JLabel("\n "));
            pnlBuyButtons.add(buttonSpring);
            SpringLayoutHelper.setupSpringGrid(pnlBuyButtons, 1, 3);
        } else {
            /*
             * no image, just buttons. this is a nice, simple,
             * centered spring layout. no fuss, no muss.
             */
            pnlBuyButtons.removeAll();

            if (!hideBMUnits) {
                pnlBuyButtons.add(btnShowMek);
            }

            pnlBuyButtons.add(btnBid);
            pnlBuyButtons.add(spacingPanel1);
            pnlBuyButtons.add(btnBid);
            pnlBuyButtons.add(btnRecallBid);
            pnlBuyButtons.add(spacingPanel2);
            pnlBuyButtons.add(btnRecallUnit);
            pnlBuyButtons.add(btnSellUnit);

            SpringLayoutHelper.setupSpringGrid(pnlBuyButtons, 1, 8);
        }

        pnlBuyButtons.validate();
        this.repaint();
    }

    /** Public entry point to refresh the market listings; simply delegates to {@link #fireMarketChanged()}. */
    public void refresh() {
        fireMarketChanged();
    }

    /**
     * Reloads the market table's underlying model from the campaign's current black-market state and resizes the
     * table to exactly fit its (possibly changed) row count.
     */
    public void fireMarketChanged() {
        //here's a problem MyBlackMarket has to be created somehow (by parsing BM or from Player data)
        BlackMarketInfo.refreshModel();

        tblMarket.setPreferredSize(new Dimension(tblMarket.getWidth(),
              tblMarket.getRowHeight() * (tblMarket.getRowCount())));
        tblMarket.revalidate();
    }

    /**
     * Method called from CPlayer after a faction is set. Enables or disables the Sell Unit button, as appropriate for
     * the faction.
     * <p>
     * Also sets a correct CBMPanel.factionBidsAllowed value so that future clicks on BM units give a correct "Place
     * Bid" button.
     */
    public void checkFactionAccess() {

        //check to see if selling is forbidden for the player's faction
        boolean sellingEnabled = true;
        StringTokenizer blockedFactions = new StringTokenizer(client.getServerConfigs("BMNoSell"), "$");
        while (blockedFactions.hasMoreTokens()) {
            if (Player.getMyHouse().getName().equals(blockedFactions.nextToken())) {
                sellingEnabled = false;
            }
        }

        btnSellUnit.setEnabled(sellingEnabled);

        //check to see if buying is forbids, and save boolean.
        boolean buyingEnabled = true;
        blockedFactions = new java.util.StringTokenizer(client.getServerConfigs("BMNoBuy"), "$");
        while (blockedFactions.hasMoreTokens()) {
            if (Player.getMyHouse().getName().equals(blockedFactions.nextToken())) {
                buyingEnabled = false;
            }
        }

        //have to use an ivar and check the perm so bidding is re-enabled after defection
        factionBidsAllowed = buyingEnabled;

    }

}
