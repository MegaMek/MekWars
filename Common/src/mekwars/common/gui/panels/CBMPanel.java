/*
 * MekWars - Copyright (C) 2004
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

package mekwars.common.gui.panels;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
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

import megamek.common.units.Entity;
import mekwars.common.campaign.CBMUnit;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.MekInfo;
import mekwars.common.gui.TableSorter;
import mekwars.common.gui.dialogs.SellUnitDialog;
import mekwars.common.gui.models.BlackMarketModel;
import mekwars.common.util.MWLogger;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Black Market Panel
 */

public class CBMPanel extends javax.swing.JPanel {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -432087180209544906L;
    private final JTable tblMarket = new JTable();
    private final JButton btnShowMek = new JButton();
    private final JButton btnRecallBid = new JButton();
    private final JButton btnRecallUnit = new JButton();
    private final JButton btnSellUnit = new JButton();
    private final JButton btnBid = new JButton();
    private final JPanel pnlBuyButtons = new JPanel();
    private final JPanel spacingPanel1;
    private final JPanel spacingPanel2;
    private final JPanel pnlMekIconHolder;
    private final boolean hideBMUnits;
    private final CCampaign theCampaign;
    public BlackMarketModel BlackMarketInfo;
    IClient client;
    CPlayer Player;
    private JPanel pnlMekIcon;
    private boolean factionBidsAllowed = true;
    private CBMUnit mm;

    public CBMPanel(IClient client) {
        setLayout(new GridBagLayout());
        this.client = client;
        hideBMUnits = Boolean.parseBoolean(this.client.getServerConfigs("HiddenBMUnits"));

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

        tblMarket.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnShowMekActionPerformed(new java.awt.event.ActionEvent(btnShowMek, 0, ""));
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
                mm = getMarketMechAtRow(tblMarket.getSelectedRow());

                //if there is a unit in the row, update the dynamic buttons.
                if (mm != null) {

                    if (!hideBMUnits) {
                        btnShowMek.setEnabled(true);
                    }

                    btnRecallBid.setEnabled(mm.getBid() > 0);

                    if (mm.playerIsSeller()) {
                        btnBid.setEnabled(false);
                        btnRecallUnit.setEnabled(true);
                    } else {
                        //check cached faction ban value
                        if (factionBidsAllowed) {
                            btnBid.setEnabled(true);
                        } else {
                            btnBid.setEnabled(false);
                        }
                        btnRecallUnit.setEnabled(false);
                    }


                    //refresh the camo ... may have changed.
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
     * Called from an action listener. Opens a MechDetailDisplay for the unit at the currently selected row.
     *
     * @param evt
     */
    private void btnShowMekActionPerformed(ActionEvent evt) {
        if (hideBMUnits) {
            return;
        }
        mm = getMarketMechAtRow(tblMarket.getSelectedRow());
        if (mm == null) {
            return;
        }
        Entity theEntity = mm.getEmbeddedUnit().getEntity();
        theEntity.loadAllWeapons();

        JFrame infoWindow = new JFrame();
        MWUnitDisplay unitDisplay = new MWUnitDisplay(null, client);

        infoWindow.getContentPane().add(unitDisplay);
        infoWindow.setSize(300, 400);
        infoWindow.setResizable(false);

        infoWindow.setTitle(mm.getModelName());
        infoWindow.setLocationRelativeTo(client.getMainFrame());//center it
        infoWindow.setVisible(true);
        unitDisplay.displayEntity(theEntity);
    }

    public CBMUnit getMarketMechAtRow(int row) {
        mm = null;
        Integer auctionId = (Integer) tblMarket.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID);
        if (auctionId != null) {
            mm = theCampaign.getBlackMarket().get(auctionId);
        }
        return mm;
    }

    //refresh preview image
    public void resetCamo() {

        if (!hideBMUnits && client.getConfig().isParam("BMPREVIEWIMAGE")) {

            //refresh the camo ... may have been changed.
            pnlMekIcon = new MekInfo(client.getConfig().getImage("CAMO"));
            pnlMekIcon.setMinimumSize(new Dimension(84, 72));
            pnlMekIcon.setPreferredSize(new Dimension(84, 72));
            pnlMekIcon.setMaximumSize(new Dimension(84, 72));


            try {
                ((MekInfo) pnlMekIcon).setUnit(mm.getEmbeddedUnit().getEntity());
                ((MekInfo) pnlMekIcon).setImageVisible(true);
            } catch (Exception e) {
                //just means no entity has been selected yet
            }

            pnlMekIconHolder.removeAll();
            pnlMekIconHolder.add(pnlMekIcon);

        } else {
            ((MekInfo) pnlMekIcon).setImageVisible(false);
        }

        pnlBuyButtons.validate();
    }

    /**
     * Called from an action listener. Opens a dialo for input, checks the input, and places a bid with the server if
     * the bid is enough.
     *
     * @param evt
     */
    private void btnBidActionPerformed(ActionEvent evt) {
        int row = tblMarket.getSelectedRow();

        //shouldn't ever happen, but still trap
        if (row < 0) {
            return;
        }

        //get the unit
        mm = getMarketMechAtRow(tblMarket.getSelectedRow());

        //also shouldn't ever happen but, again, catch it
        if (mm.playerIsSeller()) {
            return;
        }

        if (mm != null) {

            int auctionID = mm.getAuctionID();
            if (auctionID != -1) {//-1 is the default value. indicates null auction.

                //generate a new option dialog
                String playerBidString = JOptionPane.showInputDialog(client.getMainFrame(),
                      STR."<HTML><center>How much would you like to bid on the \{mm.getModelName()}?<BR>Minimum is \{client.moneyOrFluMessage(
                            true,
                            true,
                            mm.getMinBid())}.</center></HTML>",
                      "Amount to Bid",
                      javax.swing.JOptionPane.PLAIN_MESSAGE);

                //Clicked Cancel
                if ((playerBidString == null) || (playerBidString.trim().isEmpty())) {
                    return;
                }

                try {
                    int playerBid = Integer.parseInt(playerBidString);

                    if (playerBid < mm.getMinBid()) {
                        String toUser = STR."CH|CLIENT: You tried to bid less than the minimum your contacts are willing to accept for the \{mm.getModelName()}. Try bidding \{client.moneyOrFluMessage(
                              true,
                              false,
                              mm.getMinBid())} or more.";
                        client.doParseDataInput(toUser);
                        return;
                    }

                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c bid#\{auctionID}#\{playerBid}");

                    //this clears the selection, so unhighlight the bid button. sometimes the
                    //bid and retract buttons will be active simultaneously, so disable both
                    btnBid.setEnabled(false);
                    btnRecallBid.setEnabled(false);
                } catch (NumberFormatException NFE) {
                    String toUser = "CH|CLIENT: Invalid Bid amount. Try using numbers next time!";
                    client.doParseDataInput(toUser);
                    return;
                } catch (Exception ex) {
                    MWLogger.errLog(ex);
                }
            }
        }
    }//end btnBidActionPerformed

    /**
     * Called by action listener. Retracts a bid placed on a unit.
     */
    private void btnRecallBidActionPerformed(ActionEvent evt) {

        mm = getMarketMechAtRow(tblMarket.getSelectedRow());

        //break out if no selection
        if (mm == null) {
            return;
        }

        //no bid. return.
        if (tblMarket.getValueAt(tblMarket.getSelectedRow(), BlackMarketModel.BID) == null) {
            return;
        }

        //returns passed. send the recall command and deselect the buttons.
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c recallbid#\{mm.getAuctionID()}");
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);

    }

    /**
     * Called by action listener. Rebinds a unit sale.
     */
    private void btnRecallUnitActionPerformed(ActionEvent evt) {

        mm = getMarketMechAtRow(tblMarket.getSelectedRow());

        //break out if no selection
        if (mm == null) {
            return;
        }

        //not the players unit, so he cant terminate the sale
        if (!mm.playerIsSeller()) {
            return;
        }

        //returns passed. send the recall command and deselect the buttons.
        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c recall#\{mm.getAuctionID()}");
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

    public void resetButtonBar() {

        if (client.getConfig().isParam("BMPREVIEWIMAGE")) {

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

    public void refresh() {
        fireMarketChanged();
    }

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
