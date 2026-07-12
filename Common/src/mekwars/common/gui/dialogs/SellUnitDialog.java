/*
 * MekWars - Copyright (C) 2005
 *
 * original author - nmorris (urgru@users.sourceforge.net)
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

package mekwars.common.gui.dialogs;

/*
 *
 * @author urgru
 *
 * inner class which sets up a camo selection dialog.
 */

import java.io.Serial;
import java.util.Vector;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFrame;

import mekwars.common.Unit;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.WholeNumberField;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Modal dialog that lets a player list one of their units for sale on the Black Market.
 * <p>
 * The player picks a unit from a combo box (either a caller-supplied set, or - if none is supplied - every unit
 * in their hangar that is legal to sell under the current server configuration), and enters a minimum acceptable
 * bid and the number of "ticks" the auction should remain open. Pressing OK sends a {@code sell} campaign chat
 * command to the server; pressing Cancel (or closing) simply disposes the dialog without side effects.
 */
public class SellUnitDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    /**
     * Serialization id for this {@link javax.swing.JDialog} subclass.
     */
    @Serial
    private static final long serialVersionUID = 7292249744702852873L;
    //IVARS
    /** Back-link to the client used to read server configs, the player's hangar, and to send the sell command. */
    private final IClient client;
    /** Action command string used by the OK button so {@link #actionPerformed} can identify it. */
    private final String okayCommand = "Okay";

    //text fields ...
    /** User-entered minimum bid the seller will accept; pre-filled from the server's default minimum sale price. */
    private final javax.swing.JTextField minBidText = new WholeNumberField(0, 5);
    /** User-entered number of ticks the sale listing should remain active; pre-filled from the server default. */
    private final javax.swing.JTextField ticksText = new WholeNumberField(0, 5);

    //combo box to pick unit from
    /** Combo box listing the candidate units the player may put up for sale. */
    private final javax.swing.JComboBox<CUnit> possibleSaleUnits = new javax.swing.JComboBox<>();

    //CONSTRUCTOR
    /**
     * Builds and displays the sell-unit dialog.
     *
     * @param parent owning frame, used for centering the dialog.
     * @param client active client connection; supplies the player's hangar, server configs, and sends the
     *               resulting sell command.
     * @param toSell explicit set of units to offer for sale; if {@code null} or empty, the dialog instead builds
     *               the list itself from every unit in the player's hangar that passes the server's per-unit-type
     *               "may be sold on Black Market" checks, is not already for sale, and (if the server restricts
     *               it) is not Clan tech.
     */
    public SellUnitDialog(JFrame parent, IClient client, Vector<CUnit> toSell) {

        //init superclass
        super(parent, "Sell Unit", true);

        //save the client
        this.client = client;

        //set up the buttons
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(this);

        //do the button layout
        javax.swing.JPanel buttonFlow = new javax.swing.JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        //load all legal units, if no set is given
        if (toSell == null || toSell.isEmpty()) {

            toSell = new java.util.Vector<>(1, 1);
            for (CUnit currU : client.getPlayer().getHangar()) {

                if (currU.getType() == Unit.MEK &&
                          !Boolean.parseBoolean(client.getServerConfigs("MeksMayBeSoldOnBM"))) {
                    continue;
                } else if (currU.getType() == Unit.VEHICLE &&
                                 !Boolean.parseBoolean(client.getServerConfigs("VehsMayBeSoldOnBM"))) {
                    continue;
                } else if (currU.getType() == Unit.BATTLEARMOR &&
                                 !Boolean.parseBoolean(client.getServerConfigs("BAMayBeSoldOnBM"))) {
                    continue;
                } else if (currU.getType() == Unit.PROTOMEK &&
                                 !Boolean.parseBoolean(client.getServerConfigs("ProtosMayBeSoldOnBM"))) {
                    continue;
                } else if (currU.getType() == Unit.INFANTRY &&
                                 !Boolean.parseBoolean(client.getServerConfigs("InfantryMayBeSoldOnBM"))) {continue;}

                if (currU.getStatus() == Unit.STATUS_FOR_SALE) {continue;}

                if (Boolean.parseBoolean(client.getServerConfigs("BMNoClan")) && currU.getEntity().isClan()) {
                    continue;
                }

                //unit wasnt rejected, so add it to the sales list
                toSell.add(currU);
            }
        }

        //populate the combo box
        // NOTE: the model holds CUnit instances (not Strings). getElementAt() is overridden here as a no-op
        // (it just delegates to the superclass), so the override currently has no observable effect.
        possibleSaleUnits.setModel(new DefaultComboBoxModel<>(toSell) {
            @Serial
            private static final long serialVersionUID = 2012355422040841647L;

            @Override
            public CUnit getElementAt(int index) {
                return super.getElementAt(index);
            }
        });

        //put the combo box in a sub-panel to make it autoformat better
        javax.swing.JPanel comboBoxHolder = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JLabel selectUnitHeader = new javax.swing.JLabel("Unit to sell:",
              javax.swing.SwingConstants.CENTER);
        selectUnitHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        comboBoxHolder.add(selectUnitHeader);
        comboBoxHolder.add(possibleSaleUnits);
        SpringLayoutHelper.setupSpringGrid(comboBoxHolder, 2, 1);

        java.awt.Dimension newDim = new java.awt.Dimension();
        newDim.setSize(possibleSaleUnits.getMinimumSize().getWidth() * 1.25,
              possibleSaleUnits.getMinimumSize().getHeight());
        possibleSaleUnits.setMaximumSize(newDim);

        //set up the text fields
        javax.swing.JPanel sellTextSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());

        sellTextSpring.add(new javax.swing.JLabel("Minimum Bid:", javax.swing.SwingConstants.TRAILING));
        minBidText.setToolTipText("Minimum bid you're willing to accept for the unit.");
        minBidText.setText(client.getServerConfigs("MinBMSalesPrice"));
        sellTextSpring.add(minBidText);

        sellTextSpring.add(new javax.swing.JLabel("Sale Ticks:", javax.swing.SwingConstants.TRAILING));
        ticksText.setToolTipText("Number of ticks the unit will remain on sale.");
        ticksText.setText(client.getServerConfigs("MinBMSalesTicks"));
        sellTextSpring.add(ticksText);

        SpringLayoutHelper.setupSpringGrid(sellTextSpring, 2);

        //set a default button
        this.getRootPane().setDefaultButton(cancelButton);

        //do the final layout
        javax.swing.JPanel generalLayout = new javax.swing.JPanel();
        generalLayout.setLayout(new javax.swing.BoxLayout(generalLayout, javax.swing.BoxLayout.Y_AXIS));
        generalLayout.add(new javax.swing.JLabel("\n"));
        generalLayout.add(comboBoxHolder);
        generalLayout.add(new javax.swing.JLabel("\n"));
        generalLayout.add(sellTextSpring);
        generalLayout.add(new javax.swing.JLabel("\n"));
        generalLayout.add(buttonFlow);
        this.getContentPane().add(generalLayout);
        this.pack();
        this.checkMinimumSize();
        this.setResizable(true);

        //center the dialog.
        this.setLocationRelativeTo(client.getMainFrame());
    }

    /**
     * Enforces a minimum dialog size of 220x220, resizing the dialog if the packed layout ended up smaller.
     */
    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height;
        int width;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 220) {
            width = 220;
            shouldRedraw = true;
        } else {width = (int) curDim.getWidth();}

        if (curDim.getHeight() < 220) {
            height = 220;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new java.awt.Dimension(width, height));
        }

    }//end checkMinimumSize

    /**
     * Handles both the OK and Cancel buttons (Cancel has no action command set, so it falls through to the
     * final {@code dispose()} without sending anything).
     * <p>
     * On OK: builds a {@code sell#<unitId>#<ticks>#<minBid>} campaign chat command from the selected unit and the
     * tick/bid text fields (falling back to the server-configured defaults when those fields are blank), sends it
     * to the server, then closes the dialog.
     * <p>
     * QUIRK: {@code possibleSaleUnits.getSelectedItem()} is cast directly to {@code String}, but the combo box's
     * model actually holds {@link CUnit} objects (see the constructor). This cast will throw a
     * {@link ClassCastException} whenever a unit is actually selected, so in practice the OK path likely never
     * completes the intended tokenizing-by-unit-id logic below without an exception.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            int index = possibleSaleUnits.getSelectedIndex();

            if (index < 0) {return;}

            String result = String.format("%sc sell#", IClient.CAMPAIGN_PREFIX);
            String mms = (String) possibleSaleUnits.getSelectedItem();

            if (mms != null) {
                //expects the selected item's string form to start with the unit's numeric id
                java.util.StringTokenizer st = new java.util.StringTokenizer(mms);
                CUnit mm = client.getPlayer().getUnit(Integer.parseInt(st.nextToken()));
                result += mm.getId();
                if (!ticksText.getText().equalsIgnoreCase("")) {result += String.format("#%s", ticksText.getText());} else {
                    result += String.format("#%s", client.getServerConfigs("MinBMSalesTicks"));
                }
                if (!minBidText.getText().equalsIgnoreCase("")) {result += String.format("#%s", minBidText.getText());} else {
                    result += String.format("#%s", client.getServerConfigs("MinBMSalesPrice"));
                }
            }

            client.sendChat(result);
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed


}//end SellUnitDialog
