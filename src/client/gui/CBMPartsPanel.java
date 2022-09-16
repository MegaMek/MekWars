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


package client.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import client.MWClient;
import client.campaign.CCampaign;
import common.BMEquipment;
import common.util.SpringLayoutHelper;

/**
 * Black Market Parts Panel
 */

public class CBMPartsPanel extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -5553918525846016147L;

	MWClient mwclient;

	long lastUpdate = -1;//update time for button
	public BlackMarketPartsModel BlackMarketInfo;

	private JTable tblMarket = new JTable();
	private JScrollPane spMarket = new JScrollPane();

	private JButton btnBuy = new JButton("Buy");

	private JPanel pnlBuyBtns = new JPanel();
	private JPanel pnlBuy = new JPanel();
	private JPanel spacingPanel1 = new JPanel();
	private JPanel spacingPanel3 = new JPanel();

	private CCampaign theCampaign;
	private GridBagConstraints gridBagConstraints;
	private BMEquipment bme;

	public CBMPartsPanel(MWClient client, String type) {
		setLayout(new GridBagLayout());
		mwclient = client;

		BlackMarketInfo = new BlackMarketPartsModel(mwclient,type);
		theCampaign = client.getCampaign();
		TableSorter sorter = new TableSorter(BlackMarketInfo, client, TableSorter.SORTER_BMPARTS);
		tblMarket.setModel(sorter);

		btnBuy.addActionListener(new ActionListener()
		{public void actionPerformed(ActionEvent evt)
		{btnBuyPartsPerformed(evt);}});

		tblMarket.addMouseListener(new MouseAdapter(){
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
                    btnBuyPartsPerformed(new ActionEvent(btnBuy,0,""));
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

			/*	if (lsm.isSelectionEmpty())
                    ((MechInfo)pnlMekIcon).setImageVisible(false);
				*/
				int selectedRow = lsm.getMinSelectionIndex();
				String part = (String)tblMarket.getModel().getValueAt(selectedRow, BlackMarketPartsModel.PART);
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

		spacingPanel1 = new JPanel();
		spacingPanel1.setMaximumSize(new Dimension(20,1));
		//pnlBuyBtns.add(spacingPanel1);

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

	public BMEquipment getPartsAtRow(int row)
	{
		bme = null;
		String part = (String) tblMarket.getModel().getValueAt(row, BlackMarketPartsModel.INTERNALPART);

		if (part != null) {
			bme= theCampaign.getBlackMarketParts().get(part);
		}
		return bme;
	}

	/**
	 * Called from an action listener. Opens a dialo
	 * for input, checks the input, and places a bid
	 * with the server if the bid is sufficient.
	 *
	 * @param evt
	 */
	private void btnBuyPartsPerformed(ActionEvent evt) {
		int row = tblMarket.getSelectedRow();

		//shouldnt ever happen, but still trap
		if (row < 0) {
            return;
        }

		//get the unit
		bme = getPartsAtRow(tblMarket.getSelectedRow());

		if (bme != null) {

			//generate a new option dialog
			String playerAmountString = JOptionPane.showInputDialog(mwclient.getMainFrame(), "<HTML><center>How many units of " + bme.getEquipmentName() + " would you like to buy?","Amount to Buy", JOptionPane.PLAIN_MESSAGE);

            //Clicked Cancel
            if ( playerAmountString == null || playerAmountString.trim().length() == 0) {
                return;
            }

            try {
            	int amount = Integer.parseInt(playerAmountString);

            	if ( amount > bme.getAmount() ) {
            		JOptionPane.showMessageDialog(this, "There are only "+bme.getAmount()+" "+bme.getEquipmentName()+" parts available.");
            		return;
            	}

            	if ( amount * bme.getCost() > mwclient.getPlayer().getMoney() ) {
            		JOptionPane.showMessageDialog(this, "You only have "+mwclient.moneyOrFluMessage(true, true, mwclient.getPlayer().getMoney()));
            		return;
            	}

            }catch (Exception ex) {
            	//Trap the error
            	JOptionPane.showConfirmDialog(mwclient.getMainFrame(), "Invalid Syntax Try Again.");
            	return;
            }
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c buyparts#"+ bme.getEquipmentInternalName() +"#"+ playerAmountString);

		}
	}//end btnBuyPartsPerformed

	public void resetButtonBar() {

		/*
		 * no image, just buttons. this is a nice, simple,
		 * centered spring layout. no fuss, no muss.
		 */
		pnlBuyBtns.removeAll();

		pnlBuyBtns.add(btnBuy);
		SpringLayoutHelper.setupSpringGrid(pnlBuyBtns,8);

		pnlBuyBtns.validate();
		this.repaint();
	}

}