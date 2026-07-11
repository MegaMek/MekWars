/*
 * Copyright (C) 2007 MekWars
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
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpringLayout;

import megamek.codeUtilities.MathUtility;
import mekwars.common.BMEquipment;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.TableSorter;
import mekwars.common.gui.models.BlackMarketPartsModel;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Black Market Parts Panel
 */

public class CBMPartsPanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -5553918525846016147L;
    private final JTable tblMarket = new JTable();
    private final IClient client;
    private final JButton btnBuy = new JButton("Buy");
    private final JPanel pnlBuyButtons = new JPanel();
    private final CCampaign theCampaign;
    private final BlackMarketPartsModel blackMarketPartsModel;
    private BMEquipment bme;

    public CBMPartsPanel(IClient client, String type) {
        setLayout(new java.awt.GridBagLayout());
        this.client = client;

        blackMarketPartsModel = new BlackMarketPartsModel(this.client, type);
        theCampaign = client.getCampaign();
        TableSorter sorter = new TableSorter(blackMarketPartsModel, client, TableSorter.SORTER_BM_PARTS);
        tblMarket.setModel(sorter);

        btnBuy.addActionListener(this::btnBuyPartsPerformed);

        tblMarket.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2) {
                    btnBuyPartsPerformed(new ActionEvent(btnBuy, 0, ""));
                }
            }
        });


        blackMarketPartsModel.initColumnSizes(tblMarket);

        for (int i = 0; i < blackMarketPartsModel.getColumnCount(); i++) {
            tblMarket.getColumnModel().getColumn(i).setCellRenderer(blackMarketPartsModel.getRenderer());
        }

        sorter.addMouseListenerToHeaderInTable(tblMarket);
        tblMarket.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ListSelectionModel rowSM = tblMarket.getSelectionModel();
        rowSM.addListSelectionListener(listSelectionEvent -> {
            //ignore dragging
            if (listSelectionEvent.getValueIsAdjusting()) {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) listSelectionEvent.getSource();

            int selectedRow = lsm.getMinSelectionIndex();
            String part = (String) tblMarket.getModel().getValueAt(selectedRow, BlackMarketPartsModel.PART);
            if (part != null) {
                bme = getPartsAtRow(tblMarket.getSelectedRow());
                btnBuy.setEnabled(bme != null);
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

        JPanel spacingPanel1 = new JPanel();
        spacingPanel1.setMaximumSize(new Dimension(20, 1));

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
     * Called from an action listener. Opens a dialo for input, checks the input, and places a bid with the server if
     * the bid is sufficient.
     *
     * @param evt
     */
    private void btnBuyPartsPerformed(ActionEvent evt) {
        int row = tblMarket.getSelectedRow();

        //shouldn't ever happen, but still trap
        if (row < 0) {
            return;
        }

        //get the unit
        bme = getPartsAtRow(tblMarket.getSelectedRow());

        if (bme != null) {

            //generate a new option dialog
            String playerAmountString = JOptionPane.showInputDialog(client.getMainFrame(),
                  String.format("<HTML><center>How many units of %s would you like to buy?", bme.getEquipmentName()),
                  "Amount to Buy",
                  JOptionPane.PLAIN_MESSAGE);

            //Clicked Cancel
            if (playerAmountString == null || playerAmountString.trim().isEmpty()) {
                return;
            }

            try {
                int amount = MathUtility.parseInt(playerAmountString, 0);

                if (amount > bme.getAmount()) {
                    JOptionPane.showMessageDialog(this,
                          String.format("There are only %s %s parts available.", bme.getAmount(), bme.getEquipmentName()));
                    return;
                }

                if (amount * bme.getCost() > client.getPlayer().getMoney()) {
                    JOptionPane.showMessageDialog(this,
                          String.format("You only have %s", client.moneyOrFluMessage(true, true, client.getPlayer().getMoney())));
                    return;
                }

            } catch (Exception ex) {
                //Trap the error
                JOptionPane.showConfirmDialog(client.getMainFrame(), "Invalid Syntax Try Again.");
                return;
            }
            client.sendChat(String.format("%sc buyparts#%s#%s", IClient.CAMPAIGN_PREFIX, bme.getEquipmentInternalName(), playerAmountString));

        }
    }//end btnBuyPartsPerformed

    public BMEquipment getPartsAtRow(int row) {
        bme = null;
        String part = (String) tblMarket.getModel().getValueAt(row, BlackMarketPartsModel.INTERNAL_PART);

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
        pnlBuyButtons.removeAll();

        pnlBuyButtons.add(btnBuy);
        SpringLayoutHelper.setupSpringGrid(pnlBuyButtons, 8);

        pnlBuyButtons.validate();
        this.repaint();
    }

    public void refresh() {
        fireMarketChanged();
    }

    public void fireMarketChanged() {
        //here's a problem MyBlackMarket has to be created somehow (by parsing BM or from Player data)
        blackMarketPartsModel.refreshModel();
        tblMarket.setPreferredSize(new java.awt.Dimension(tblMarket.getWidth(),
              tblMarket.getRowHeight() * (tblMarket.getRowCount())));
        tblMarket.revalidate();
    }

}
