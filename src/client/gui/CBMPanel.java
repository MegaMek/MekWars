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

package client.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.MWClient;
import client.campaign.CBMUnit;
import client.campaign.CCampaign;
import client.campaign.CPlayer;
import client.gui.dialog.SellUnitDialog;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
import megamek.client.ui.swing.unitDisplay.UnitDisplay;
import megamek.common.Entity;

/**
 * Black Market Panel
 */

public class CBMPanel extends JPanel {
    /**
     *
     */
    private static final long serialVersionUID = -432087180209544906L;
    MWClient mwclient;
    CPlayer Player;

    long lastUpdate = -1;//update time for button
    public BlackMarketModel BlackMarketInfo;

    private JTable tblMarket = new JTable();
    private JScrollPane spMarket = new JScrollPane();

    private JButton btnShowMek = new JButton();
    private JButton btnRecallBid = new JButton();
    private JButton btnRecallUnit = new JButton();
    private JButton btnSellUnit = new JButton();
    private JButton btnBid = new JButton();

    private JPanel pnlBuy = new JPanel();
    private JPanel pnlBuyBtns = new JPanel();
    private JPanel spacingPanel1 = new JPanel();
    private JPanel spacingPanel2 = new JPanel();
    private JPanel spacingPanel3 = new JPanel();
    private JPanel pnlMekIconHolder = new JPanel();
    private JPanel pnlMekIcon = new JPanel();

    private boolean factionBidsAllowed = true;
    private boolean hideBMUnits;

    private CCampaign theCampaign;
    private GridBagConstraints gridBagConstraints;
    private CBMUnit mm;

    public CBMPanel(MWClient client) {
        setLayout(new GridBagLayout());
        mwclient = client;
        hideBMUnits = Boolean.parseBoolean(mwclient.getserverConfigs("HiddenBMUnits"));

        pnlMekIconHolder = new JPanel();
        pnlMekIconHolder.setMaximumSize(new Dimension(84,72));
        pnlMekIconHolder.setMaximumSize(new Dimension(84,72));

        pnlMekIcon = new MechInfo(mwclient.getConfig().getImage("CAMO"));
        pnlMekIcon.setMinimumSize(new Dimension(84,72));
        pnlMekIcon.setPreferredSize(new Dimension(84,72));
        pnlMekIcon.setMaximumSize(new Dimension(84,72));

        BlackMarketInfo = new BlackMarketModel(mwclient, hideBMUnits);
        theCampaign = client.getCampaign();
        Player = theCampaign.getPlayer();
        TableSorter sorter = new TableSorter(BlackMarketInfo, client, TableSorter.SORTER_BM);
        tblMarket.setModel(sorter);

        tblMarket.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    btnShowMekActionPerformed(new ActionEvent(btnShowMek,0,""));
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
        rowSM.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {

                //ignore dragging
                if (e.getValueIsAdjusting()) {
                    return;
                }

                ListSelectionModel lsm = (ListSelectionModel)e.getSource();

                if (lsm.isSelectionEmpty()) {
                    ((MechInfo)pnlMekIcon).setImageVisible(false);
                }

                int selectedRow = lsm.getMinSelectionIndex();
                Integer auctionId = (Integer)tblMarket.getModel().getValueAt(selectedRow, BlackMarketModel.AUCTION_ID);
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

        pnlBuy.setLayout(new GridBagLayout());

        spMarket.setToolTipText("Click on the column header to sort.");
        spMarket.setPreferredSize(new Dimension(300, 370));
        tblMarket.setDoubleBuffered(true);
        spMarket.setViewportView(tblMarket);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        //gridBagConstraints.insets = new Insets(0, 0, 0, 0);
        pnlBuy.add(spMarket,gridBagConstraints);

        //lay out the buttons
        JPanel panelButtonWrapper = new JPanel();
        //panelButtonWrapper.setLayout(new BoxLayout(panelButtonWrapper, BoxLayout.Y_AXIS));
        pnlBuyBtns.setLayout(new SpringLayout());

        btnShowMek.setText("Show Unit");
        btnShowMek.addActionListener(new ActionListener()
        {public void actionPerformed(ActionEvent evt)
        {btnShowMekActionPerformed(evt);}});

        spacingPanel1 = new JPanel();
        spacingPanel1.setMaximumSize(new Dimension(20,1));
        //pnlBuyBtns.add(spacingPanel1);

        btnBid.setText("Place Bid");
        btnBid.setEnabled(false);
        btnBid.addActionListener(new ActionListener()
        {public void actionPerformed(ActionEvent evt)
        {btnBidActionPerformed(evt);}});

        btnRecallBid.setText("Retract Bid");
        btnRecallBid.setEnabled(false);
        btnRecallBid.addActionListener(new ActionListener()
        {public void actionPerformed(ActionEvent evt)
        {btnRecallBidActionPerformed(evt);}});

        spacingPanel2 = new JPanel();
        spacingPanel2.setMaximumSize(new Dimension(20,1));

        btnRecallUnit.setText("Remove Unit");
        btnRecallUnit.setEnabled(false);
        btnRecallUnit.addActionListener(new ActionListener()
        {public void actionPerformed(ActionEvent evt)
        {btnRecallUnitActionPerformed(evt);}});

        btnSellUnit.setText("Sell Unit");
        btnSellUnit.addActionListener(new ActionListener()
        {public void actionPerformed(ActionEvent evt)
        {btnSellUnitActionPerformed(evt);}});

        spacingPanel3 = new JPanel();
        spacingPanel3.setMaximumSize(new Dimension(20,1));

        //do the actual button layout, springmanagement, etc.
        resetButtonBar();

        panelButtonWrapper.add(pnlBuyBtns);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.0;
        pnlBuy.add(panelButtonWrapper,gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagConstraints.ipadx = 30;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        this.add(pnlBuy,gridBagConstraints);

        refresh();
    }

    public void fireMarketChanged() {
        //here's a problem MyBlackMarket has to be created somehow (by parsing BM or from Player data)
        BlackMarketInfo.refreshModel();

        tblMarket.setPreferredSize(new Dimension(tblMarket.getWidth(), tblMarket.getRowHeight()*(tblMarket.getRowCount())));
        tblMarket.revalidate();
    }

    public void refresh() {
        fireMarketChanged();
    }

    /**
     * Called by action listener. Retracts
     * a bid placed on a unit.
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
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c recallbid#" + mm.getAuctionID());
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);

    }

    /**
     * Called by action listener. Recinds a unit sale.
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
        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c recall#" + mm.getAuctionID());
        btnRecallBid.setEnabled(false);
        btnRecallUnit.setEnabled(false);
        btnBid.setEnabled(false);
    }

    /**
     * Called from an action listener. Creates a SellUnitDialog.
     * The dialog does all of the work ;-)
     */
    public void btnSellUnitActionPerformed(ActionEvent evt) {
        SellUnitDialog sud = new SellUnitDialog(null, mwclient, null);
        sud.setVisible(true);
    }

    /**
     * Called from an action listener. Opens
     * a MechDetailDisplay for the unit at the
     * currently selected row.
     *
     * @param evt
     */
    private void btnShowMekActionPerformed(ActionEvent evt) {
    	if (hideBMUnits) {
            return;
        }
        mm = getMarketMechAtRow(tblMarket.getSelectedRow());
        if (mm == null) {return;}
        Entity theEntity = mm.getEmbeddedUnit().getEntity();
        theEntity.loadAllWeapons();

        JFrame infoWindow = new JFrame();
        UnitDisplay unitDisplay = new MWUnitDisplay(null, mwclient);

        infoWindow.getContentPane().add(unitDisplay);
        infoWindow.setSize(300,400);
        infoWindow.setResizable(false);

        infoWindow.setTitle(mm.getModelName());
        infoWindow.setLocationRelativeTo(mwclient.getMainFrame());//center it
        infoWindow.setVisible(true);
        unitDisplay.displayEntity(theEntity);
    }


    public CBMUnit getMarketMechAtRow(int row)
    {
        mm = null;
        Integer auctionId = (Integer)tblMarket.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID);
        if (auctionId != null) {
            mm = theCampaign.getBlackMarket().get(auctionId);
        }
        return mm;
    }

    /**
     * Called from an action listener. Opens a dialo
     * for input, checks the input, and places a bid
     * with the server if the bid is sufficient.
     *
     * @param evt
     */
    private void btnBidActionPerformed(ActionEvent evt) {
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
                String playerBidString = JOptionPane.showInputDialog(mwclient.getMainFrame(), "<HTML><center>How much would you like to bid on the " + mm.getModelName() + "?<BR>Minimum is " + mwclient.moneyOrFluMessage(true,true,mm.getMinBid())+".</center></HTML>","Amount to Bid", JOptionPane.PLAIN_MESSAGE);

                //Clicked Cancel
                if ( (playerBidString == null) || (playerBidString.trim().length() == 0)) {
                    return;
                }

                try{
                    int playerBid = Integer.parseInt(playerBidString);

                    if (playerBid < mm.getMinBid()) {
                        String toUser = "CH|CLIENT: You tried to bid less than the minimum your contacts are" +
                        " willing to accept for the " +  mm.getModelName() + ". Try bidding " + mwclient.moneyOrFluMessage(true,false, mm.getMinBid())+" or more.";
                        mwclient.doParseDataInput(toUser);
                        return;
                    }

                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c bid#"+ auctionID +"#"+ playerBid);

                    //this clears the selection, so unhighlight the bid button. sometimes the
                    //bid and retract buttons will be active simultaneously, so disable both
                    btnBid.setEnabled(false);
                    btnRecallBid.setEnabled(false);
                }catch (NumberFormatException NFE){
                    String toUser = "CH|CLIENT: Invalid Bid amount. Try using numbers next time!";
                    mwclient.doParseDataInput(toUser);
                    return;
                }catch (Exception ex){
                    MWLogger.errLog(ex);
                }
            }
        }
    }//end btnBidActionPerformed

    //refresh preview image
    public void resetCamo() {

        if (!hideBMUnits && mwclient.getConfig().isParam("BMPREVIEWIMAGE")) {

            //refresh the camo ... may have been changed.
            pnlMekIcon = new MechInfo(mwclient.getConfig().getImage("CAMO"));
            pnlMekIcon.setMinimumSize(new Dimension(84,72));
            pnlMekIcon.setPreferredSize(new Dimension(84,72));
            pnlMekIcon.setMaximumSize(new Dimension(84,72));


            try {
                ((MechInfo)pnlMekIcon).setUnit(mm.getEmbeddedUnit().getEntity());
                ((MechInfo)pnlMekIcon).setImageVisible(true);
            } catch (Exception e) {
                //just means no entity has been selected yet
            }

            pnlMekIconHolder.removeAll();
            pnlMekIconHolder.add(pnlMekIcon);

        } else {
            ((MechInfo)pnlMekIcon).setImageVisible(false);
        }

        pnlBuyBtns.validate();
    }

    /**
     * Method called from CPlayer after a faction is set. Enables or
     * disables the Sell Unit button, as appropriate for the faction.
     *
     * Also sets a correct CBMPanel.factionBidsAllowed value so that
     * future clicks on BM units give a correct "Place Bid" button.
     */
    public void checkFactionAccess() {

        //check to see if selling is forbidden for the player's faction
        boolean sellingEnabled = true;
        StringTokenizer blockedFactions = new StringTokenizer(mwclient.getserverConfigs("BMNoSell"), "$");
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
        blockedFactions = new StringTokenizer(mwclient.getserverConfigs("BMNoBuy"), "$");
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
            pnlBuyBtns.add(pnlMekIconHolder);

            SpringLayoutHelper.setupSpringGrid(buttonSpring,1,8);
            pnlBuyBtns.add(new JLabel("\n "));
            pnlBuyBtns.add(buttonSpring);
            SpringLayoutHelper.setupSpringGrid(pnlBuyBtns,1,3);

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

            SpringLayoutHelper.setupSpringGrid(pnlBuyBtns,1,8);
        }

        pnlBuyBtns.validate();
        this.repaint();
    }

}