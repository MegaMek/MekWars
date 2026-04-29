/*
 * MekWars - Copyright (C) 2007
 *
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

import mekwars.common.BMEquipment;
import mekwars.common.campaign.CCampaign;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Black Market Parts Panel
 */

public class CBMPartsPanel extends javax.swing.JPanel {

    /**
     *
     */
    private static final long serialVersionUID = -5553918525846016147L;
    public BlackMarketPartsModel BlackMarketInfo;
    client.MWClient mwclient;
    long lastUpdate = -1;//update time for button
    private javax.swing.JTable tblMarket = new javax.swing.JTable();
    private javax.swing.JScrollPane spMarket = new javax.swing.JScrollPane();

    private javax.swing.JButton btnBuy = new javax.swing.JButton("Buy");

    private javax.swing.JPanel pnlBuyBtns = new javax.swing.JPanel();
    private javax.swing.JPanel pnlBuy = new javax.swing.JPanel();
    private javax.swing.JPanel spacingPanel1 = new javax.swing.JPanel();
    private javax.swing.JPanel spacingPanel3 = new javax.swing.JPanel();

    private CCampaign theCampaign;
    private java.awt.GridBagConstraints gridBagConstraints;
    private BMEquipment bme;

    public CBMPartsPanel(client.MWClient client, String type) {
        setLayout(new java.awt.GridBagLayout());
        mwclient = client;

        BlackMarketInfo = new BlackMarketPartsModel(mwclient, type);
        theCampaign = client.getCampaign();
        TableSorter sorter = new TableSorter(BlackMarketInfo, client, TableSorter.SORTER_BMPARTS);
        tblMarket.setModel(sorter);

        btnBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {btnBuyPartsPerformed(evt);}
        });

        tblMarket.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnBuyPartsPerformed(new java.awt.event.ActionEvent(btnBuy, 0, ""));
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

			/*	if (lsm.isSelectionEmpty())
                    ((MechInfo)pnlMekIcon).setImageVisible(false);
				*/
                int selectedRow = lsm.getMinSelectionIndex();
                String part = (String) tblMarket.getModel().getValueAt(selectedRow, BlackMarketPartsModel.PART);
                if (part != null) {
                    bme = getPartsAtRow(tblMarket.getSelectedRow());

                    //if there is a unit in the row, update the dynamic buttons.
                    if (bme != null) {
                        btnBuy.setEnabled(true);
                    } else {//dim them all
                        btnBuy.setEnabled(false);
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

        spacingPanel1 = new javax.swing.JPanel();
        spacingPanel1.setMaximumSize(new java.awt.Dimension(20, 1));
        //pnlBuyBtns.add(spacingPanel1);

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
     * Called from an action listener. Opens a dialo for input, checks the input, and places a bid with the server if
     * the bid is sufficient.
     *
     * @param evt
     */
    private void btnBuyPartsPerformed(java.awt.event.ActionEvent evt) {
        int row = tblMarket.getSelectedRow();

        //shouldnt ever happen, but still trap
        if (row < 0) {
            return;
        }

        //get the unit
        bme = getPartsAtRow(tblMarket.getSelectedRow());

        if (bme != null) {

            //generate a new option dialog
            String playerAmountString = javax.swing.JOptionPane.showInputDialog(mwclient.getMainFrame(),
                  "<HTML><center>How many units of " + bme.getEquipmentName() + " would you like to buy?",
                  "Amount to Buy",
                  javax.swing.JOptionPane.PLAIN_MESSAGE);

            //Clicked Cancel
            if (playerAmountString == null || playerAmountString.trim().length() == 0) {
                return;
            }

            try {
                int amount = Integer.parseInt(playerAmountString);

                if (amount > bme.getAmount()) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                          "There are only " + bme.getAmount() + " " + bme.getEquipmentName() + " parts available.");
                    return;
                }

                if (amount * bme.getCost() > mwclient.getPlayer().getMoney()) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                          "You only have " + mwclient.moneyOrFluMessage(true, true, mwclient.getPlayer().getMoney()));
                    return;
                }

            } catch (Exception ex) {
                //Trap the error
                javax.swing.JOptionPane.showConfirmDialog(mwclient.getMainFrame(), "Invalid Syntax Try Again.");
                return;
            }
            mwclient.sendChat(client.MWClient.CAMPAIGN_PREFIX +
                                    "c buyparts#" +
                                    bme.getEquipmentInternalName() +
                                    "#" +
                                    playerAmountString);

        }
    }//end btnBuyPartsPerformed

    public BMEquipment getPartsAtRow(int row) {
        bme = null;
        String part = (String) tblMarket.getModel().getValueAt(row, BlackMarketPartsModel.INTERNALPART);

        if (part != null) {
            bme = theCampaign.getBlackMarketParts().get(part);
        }
        return bme;
    }

    public void resetButtonBar() {

        /*
         * no image, just buttons. this is a nice, simple,
         * centered spring layout. no fuss, no muss.
         */
        pnlBuyBtns.removeAll();

        pnlBuyBtns.add(btnBuy);
        SpringLayoutHelper.setupSpringGrid(pnlBuyBtns, 8);

        pnlBuyBtns.validate();
        this.repaint();
    }

    public void refresh() {
        fireMarketChanged();
    }

    public void fireMarketChanged() {
        //here's a problem MyBlackMarket has to be created somehow (by parsing BM or from Player data)
        BlackMarketInfo.refreshModel();
        tblMarket.setPreferredSize(new java.awt.Dimension(tblMarket.getWidth(),
              tblMarket.getRowHeight() * (tblMarket.getRowCount())));
        tblMarket.revalidate();
    }

}
