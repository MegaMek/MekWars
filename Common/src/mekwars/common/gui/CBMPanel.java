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

package mekwars.common.gui;

import client.gui.dialog.SellUnitDialog;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Entity;

/**
 * Black Market Panel
 */

public class CBMPanel extends javax.swing.JPanel {
    /**
     *
     */
    private static final long serialVersionUID = -432087180209544906L;
    public BlackMarketModel BlackMarketInfo;
    client.MWClient mwclient;
    client.campaign.CPlayer Player;
    long lastUpdate = -1;//update time for button
    private javax.swing.JTable tblMarket = new javax.swing.JTable();
    private javax.swing.JScrollPane spMarket = new javax.swing.JScrollPane();

    private javax.swing.JButton btnShowMek = new javax.swing.JButton();
    private javax.swing.JButton btnRecallBid = new javax.swing.JButton();
    private javax.swing.JButton btnRecallUnit = new javax.swing.JButton();
    private javax.swing.JButton btnSellUnit = new javax.swing.JButton();
    private javax.swing.JButton btnBid = new javax.swing.JButton();

    private javax.swing.JPanel pnlBuy = new javax.swing.JPanel();
    private javax.swing.JPanel pnlBuyBtns = new javax.swing.JPanel();
    private javax.swing.JPanel spacingPanel1 = new javax.swing.JPanel();
    private javax.swing.JPanel spacingPanel2 = new javax.swing.JPanel();
    private javax.swing.JPanel spacingPanel3 = new javax.swing.JPanel();
    private javax.swing.JPanel pnlMekIconHolder = new javax.swing.JPanel();
    private javax.swing.JPanel pnlMekIcon = new javax.swing.JPanel();

    private boolean factionBidsAllowed = true;
    private boolean hideBMUnits;

    private client.campaign.CCampaign theCampaign;
    private java.awt.GridBagConstraints gridBagConstraints;
    private client.campaign.CBMUnit mm;

    public CBMPanel(client.MWClient client) {
        setLayout(new java.awt.GridBagLayout());
        mwclient = client;
        hideBMUnits = Boolean.parseBoolean(mwclient.getserverConfigs("HiddenBMUnits"));

        pnlMekIconHolder = new javax.swing.JPanel();
        pnlMekIconHolder.setMaximumSize(new java.awt.Dimension(84, 72));
        pnlMekIconHolder.setMaximumSize(new java.awt.Dimension(84, 72));

        pnlMekIcon = new MechInfo(mwclient.getConfig().getImage("CAMO"));
        pnlMekIcon.setMinimumSize(new java.awt.Dimension(84, 72));
        pnlMekIcon.setPreferredSize(new java.awt.Dimension(84, 72));
        pnlMekIcon.setMaximumSize(new java.awt.Dimension(84, 72));

        BlackMarketInfo = new BlackMarketModel(mwclient, hideBMUnits);
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
        tblMarket.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        javax.swing.ListSelectionModel rowSM = tblMarket.getSelectionModel();
        rowSM.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent e) {

                //ignore dragging
                if (e.getValueIsAdjusting()) {
                    return;
                }

                javax.swing.ListSelectionModel lsm = (javax.swing.ListSelectionModel) e.getSource();

                if (lsm.isSelectionEmpty()) {
                    ((MechInfo) pnlMekIcon).setImageVisible(false);
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

                        if (mm.getBid() > 0) {
                            btnRecallBid.setEnabled(true);
                        } else {
                            btnRecallBid.setEnabled(false);
                        }

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
                        mwclient.getMainFrame().getMainPanel().getBMPanel().resetCamo();

                    } else {//dim them all
                        btnRecallBid.setEnabled(false);
                        btnRecallUnit.setEnabled(false);
                        btnBid.setEnabled(false);
                        btnShowMek.setEnabled(false);
                    }
                }

            }
        });

        pnlBuy.setLayout(new java.awt.GridBagLayout());

        spMarket.setToolTipText("Click on the column header to sort.");
        spMarket.setPreferredSize(new java.awt.Dimension(300, 370));
        tblMarket.setDoubleBuffered(true);
        spMarket.setViewportView(tblMarket);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        //gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlBuy.add(spMarket, gridBagConstraints);

        //lay out the buttons
        javax.swing.JPanel panelButtonWrapper = new javax.swing.JPanel();
        //panelButtonWrapper.setLayout(new BoxLayout(panelButtonWrapper, BoxLayout.Y_AXIS));
        pnlBuyBtns.setLayout(new javax.swing.SpringLayout());

        btnShowMek.setText("Show Unit");
        btnShowMek.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnShowMekActionPerformed(evt);}
        });

        spacingPanel1 = new javax.swing.JPanel();
        spacingPanel1.setMaximumSize(new java.awt.Dimension(20, 1));
        //pnlBuyBtns.add(spacingPanel1);

        btnBid.setText("Place Bid");
        btnBid.setEnabled(false);
        btnBid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnBidActionPerformed(evt);}
        });

        btnRecallBid.setText("Retract Bid");
        btnRecallBid.setEnabled(false);
        btnRecallBid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnRecallBidActionPerformed(evt);}
        });

        spacingPanel2 = new javax.swing.JPanel();
        spacingPanel2.setMaximumSize(new java.awt.Dimension(20, 1));

        btnRecallUnit.setText("Remove Unit");
        btnRecallUnit.setEnabled(false);
        btnRecallUnit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnRecallUnitActionPerformed(evt);}
        });

        btnSellUnit.setText("Sell Unit");
        btnSellUnit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnSellUnitActionPerformed(evt);}
        });

        spacingPanel3 = new javax.swing.JPanel();
        spacingPanel3.setMaximumSize(new java.awt.Dimension(20, 1));

        //do the actual button layout, springmanagement, etc.
        resetButtonBar();

        panelButtonWrapper.add(pnlBuyBtns);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        pnlBuy.add(panelButtonWrapper, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
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
    private void btnShowMekActionPerformed(java.awt.event.ActionEvent evt) {
        if (hideBMUnits) {
            return;
        }
        mm = getMarketMechAtRow(tblMarket.getSelectedRow());
        if (mm == null) {return;}
        Entity theEntity = mm.getEmbeddedUnit().getEntity();
        theEntity.loadAllWeapons();

        javax.swing.JFrame infoWindow = new javax.swing.JFrame();
        UnitDisplay unitDisplay = new MWUnitDisplay(null, mwclient);

        infoWindow.getContentPane().add(unitDisplay);
        infoWindow.setSize(300, 400);
        infoWindow.setResizable(false);

        infoWindow.setTitle(mm.getModelName());
        infoWindow.setLocationRelativeTo(mwclient.getMainFrame());//center it
        infoWindow.setVisible(true);
        unitDisplay.displayEntity(theEntity);
    }

    public client.campaign.CBMUnit getMarketMechAtRow(int row) {
        mm = null;
        Integer auctionId = (Integer) tblMarket.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID);
        if (auctionId != null) {
            mm = theCampaign.getBlackMarket().get(auctionId);
        }
        return mm;
    }

    /**
     * Called from an action listener. Opens a dialo for input, checks the input, and places a bid with the server if
     * the bid is sufficient.
     *
     * @param evt
     */
    private void btnBidActionPerformed(java.awt.event.ActionEvent evt) {
        int row = tblMarket.getSelectedRow();

        //shouldnt ever happen, but still trap
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
                String playerBidString = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                      "<HTML><center>How much would you like to bid on the " +
                            mm.getModelName() +
                            "?<BR>Minimum is " +
                            mwclient.moneyOrFluMessage(true, true, mm.getMinBid()) +
                            ".</center></HTML>",
                      "Amount to Bid",
                      javax.swing.JOptionPane.PLAIN_MESSAGE);

                //Clicked Cancel
                if ((playerBidString == null) || (playerBidString.trim().length() == 0)) {
                    return;
                }

                try {
                    int playerBid = Integer.parseInt(playerBidString);

                    if (playerBid < mm.getMinBid()) {
                        String toUser = "CH|CLIENT: You tried to bid less than the minimum your contacts are" +
                                              " willing to accept for the " +
                                              mm.getModelName() +
                                              ". Try bidding " +
                                              mwclient.moneyOrFluMessage(true, false, mm.getMinBid()) +
                                              " or more.";
                        mwclient.doParseDataInput(toUser);
                        return;
                    }

                    mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c bid#" + auctionID + "#" + playerBid);

                    //this clears the selection, so unhighlight the bid button. sometimes the
                    //bid and retract buttons will be active simultaneously, so disable both
                    btnBid.setEnabled(false);
                    btnRecallBid.setEnabled(false);
                } catch (NumberFormatException NFE) {
                    String toUser = "CH|CLIENT: Invalid Bid amount. Try using numbers next time!";
                    mwclient.doParseDataInput(toUser);
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
    private void btnRecallBidActionPerformed(java.awt.event.ActionEvent evt) {

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
        mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c recallbid#" + mm.getAuctionID());
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);

    }

    /**
     * Called by action listener. Recinds a unit sale.
     */
    private void btnRecallUnitActionPerformed(java.awt.event.ActionEvent evt) {

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
        mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX + "c recall#" + mm.getAuctionID());
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);
    }

    /**
     * Called from an action listener. Creates a SellUnitDialog. The dialog does all of the work ;-)
     */
    public void btnSellUnitActionPerformed(java.awt.event.ActionEvent evt) {
        SellUnitDialog sud = new SellUnitDialog(null, mwclient, null);
        sud.setVisible(true);
    }

    public void resetButtonBar() {

        if (mwclient.getConfig().isParam("BMPREVIEWIMAGE")) {

            /*
             * Pain in the ass layout. Have to keep the
             * button heights down, etc. Using a maxheight
             * isnt preferable, since it can screw up the
             * layout when no preview is in use ... so we
             * work with stupidly embedded panels instead.
             */

            //clear panels
            pnlBuyBtns.removeAll();
            pnlMekIconHolder.removeAll();

            //standard spring layout for the buttons
            javax.swing.JPanel buttonSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
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
            pnlBuyBtns.add(pnlMekIconHolder);

            SpringLayoutHelper.setupSpringGrid(buttonSpring, 1, 8);
            pnlBuyBtns.add(new javax.swing.JLabel("\n "));
            pnlBuyBtns.add(buttonSpring);
            SpringLayoutHelper.setupSpringGrid(pnlBuyBtns, 1, 3);

        } else {

            /*
             * no image, just buttons. this is a nice, simple,
             * centered spring layout. no fuss, no muss.
             */
            pnlBuyBtns.removeAll();

            if (!hideBMUnits) {
                pnlBuyBtns.add(btnShowMek);
            }
            pnlBuyBtns.add(btnBid);
            pnlBuyBtns.add(spacingPanel1);
            pnlBuyBtns.add(btnBid);
            pnlBuyBtns.add(btnRecallBid);
            pnlBuyBtns.add(spacingPanel2);
            pnlBuyBtns.add(btnRecallUnit);
            pnlBuyBtns.add(btnSellUnit);

            SpringLayoutHelper.setupSpringGrid(pnlBuyBtns, 1, 8);
        }

        pnlBuyBtns.validate();
        this.repaint();
    }

    public void refresh() {
        fireMarketChanged();
    }

    //refresh preview image
    public void resetCamo() {

        if (!hideBMUnits && mwclient.getConfig().isParam("BMPREVIEWIMAGE")) {

            //refresh the camo ... may have been changed.
            pnlMekIcon = new MechInfo(mwclient.getConfig().getImage("CAMO"));
            pnlMekIcon.setMinimumSize(new java.awt.Dimension(84, 72));
            pnlMekIcon.setPreferredSize(new java.awt.Dimension(84, 72));
            pnlMekIcon.setMaximumSize(new java.awt.Dimension(84, 72));


            try {
                ((MechInfo) pnlMekIcon).setUnit(mm.getEmbeddedUnit().getEntity());
                ((MechInfo) pnlMekIcon).setImageVisible(true);
            } catch (Exception e) {
                //just means no entity has been selected yet
            }

            pnlMekIconHolder.removeAll();
            pnlMekIconHolder.add(pnlMekIcon);

        } else {
            ((MechInfo) pnlMekIcon).setImageVisible(false);
        }

        pnlBuyBtns.validate();
    }

    public void fireMarketChanged() {
        //here's a problem MyBlackMarket has to be created somehow (by parsing BM or from Player data)
        BlackMarketInfo.refreshModel();

        tblMarket.setPreferredSize(new java.awt.Dimension(tblMarket.getWidth(),
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
        java.util.StringTokenizer blockedFactions = new java.util.StringTokenizer(mwclient.getserverConfigs("BMNoSell"),
              "$");
        while (blockedFactions.hasMoreTokens()) {
            if (Player.getMyHouse().getName().equals(blockedFactions.nextToken())) {
                sellingEnabled = false;
            }
        }

        if (sellingEnabled) {
            btnSellUnit.setEnabled(true);
        } else {
            btnSellUnit.setEnabled(false);
        }

        //check to see if buying is forbidds, and save boolean.
        boolean buyingEnabled = true;
        blockedFactions = new java.util.StringTokenizer(mwclient.getserverConfigs("BMNoBuy"), "$");
        while (blockedFactions.hasMoreTokens()) {
            if (Player.getMyHouse().getName().equals(blockedFactions.nextToken())) {
                buyingEnabled = false;
            }
        }

        //have to use an ivar and check the perm so bidding is re-enabled after defection
        if (buyingEnabled) {
            factionBidsAllowed = true;
        } else {
            factionBidsAllowed = false;
        }

    }

}
