/*
 * MekWars - Copyright (C) 2005
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
 * Dialog, based on HouseNameDialog, which allows players
 * to search for players using partial strings. Takes a
 * boolean to indicate whether to use all players, or only
 * those in the player's faction.
 *
 * @urgru 6.17.05
 */

import java.io.Serial;

import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Modal dialog that lets a player pick a single unit from their own hangar via a combo box.
 * <p>
 * The combo box is populated directly from {@code client.getPlayer().getHangar()}, so the
 * choices always reflect the current player's unit roster. The combo box's items are
 * {@link CUnit} instances but {@link #actionPerformed(java.awt.event.ActionEvent)} reads the
 * selection back as a {@code String} and tokenizes it to extract the leading unit ID, so the
 * displayed unit's {@code toString()} is expected to start with its ID token.
 * <p>
 * Show the dialog (e.g. via {@code setVisible(true)}), then after it closes call
 * {@link #getUnitID()} to retrieve the chosen unit's ID, or {@code "-1"} if the user
 * cancelled or closed the dialog without picking anything.
 */
public class UnitSelectionDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    //variables

    @Serial
    private static final long serialVersionUID = 16880146524838545L;

    /** Combo box listing the units in the player's hangar to choose from. */
    private final javax.swing.JComboBox<CUnit> possibleUnits = new javax.swing.JComboBox<>();

    /** Action command string used to identify the OK button in {@link #actionPerformed}. */
    private final String okayCommand = "Okay";

    /** ID of the selected unit, or "-1" if none has been chosen yet (default/cancelled state). */
    private String toReturn = "-1";
    //private boolean factionOnly = false;

    /**
     * Builds and lays out the dialog: a label, a combo box of the player's hangar units, and
     * OK/Cancel buttons. The dialog is packed, resized to a sensible minimum, and centered
     * over the client's main frame, but is not shown automatically.
     *
     * @param client    the client whose player's hangar supplies the selectable units
     * @param boxText   text used as the dialog's title
     * @param labelText text shown in the label above the combo box, describing what to pick
     */
    public UnitSelectionDialog(IClient client, String boxText, String labelText) {

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner

        //make the label
        //label
        javax.swing.JLabel comboLabel = new javax.swing.JLabel(labelText, javax.swing.SwingConstants.CENTER);
        comboLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        comboLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

        //populate the combo box
        possibleUnits.setModel(new javax.swing.DefaultComboBoxModel<>(client.getPlayer().getHangar()) {
            @Serial
            private static final long serialVersionUID = -3752642922668363196L;

            @Override
            public CUnit getElementAt(int index) {
                return super.getElementAt(index);
            }
        });

        java.awt.Dimension newDim = new java.awt.Dimension();
        newDim.setSize(possibleUnits.getMinimumSize().getWidth() * 1.25, possibleUnits.getMinimumSize().getHeight());
        possibleUnits.setMaximumSize(newDim);

        //set up listeners for the buttons
        //buttons
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
        cancelButton.addActionListener(this);

        //do some formatting. rawr.
        javax.swing.JPanel springPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        springPanel.add(comboLabel);
        springPanel.add(possibleUnits);
        SpringLayoutHelper.setupSpringGrid(springPanel, 2, 1);

        javax.swing.JPanel buttonFlow = new javax.swing.JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        javax.swing.JPanel generalLayout = new javax.swing.JPanel();
        generalLayout.setLayout(new javax.swing.BoxLayout(generalLayout, javax.swing.BoxLayout.Y_AXIS));
        generalLayout.add(new javax.swing.JLabel("\n"));
        generalLayout.add(springPanel);
        generalLayout.add(new javax.swing.JLabel("\n"));
        generalLayout.add(buttonFlow);
        generalLayout.add(new javax.swing.JLabel("\n"));
        this.getContentPane().add(generalLayout);
        this.pack();
        this.setLocationRelativeTo(client.getMainFrame());
        this.checkMinimumSize();
        this.setResizable(true);

    }

    /**
     * Ensures the dialog is not shrunk below a usable minimum size (275x200) after packing.
     * Resizes the dialog only if the current size is smaller than the minimum in either dimension.
     */
    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height;
        int width;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 275) {
            width = 275;
            shouldRedraw = true;
        } else {width = (int) curDim.getWidth();}

        if (curDim.getHeight() < 200) {
            height = 200;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new java.awt.Dimension(width, height));
        }

    }//end checkMinimumSize

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     * <p>
     * On OK: if nothing is selected in the combo box, does nothing (dialog stays open). Otherwise,
     * the selected combo box item is cast to a {@code String} (note: the model's element type is
     * {@link CUnit}, so this relies on the combo box rendering/returning the unit's string form),
     * tokenizes it on whitespace, and treats the first token as the unit ID, which is stored via
     * {@link #setUnitID(String)}. The dialog is then hidden (not disposed) so callers can still
     * read {@link #getUnitID()} afterward.
     * <p>
     * Any other action command (i.e. Cancel) disposes of the dialog without setting a unit ID,
     * leaving {@link #getUnitID()} at its default of "-1".
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {

            int index = possibleUnits.getSelectedIndex();
            if (index < 0) {return;}

            String mms = (String) possibleUnits.getSelectedItem();

            if (mms != null) {
                java.util.StringTokenizer st = new java.util.StringTokenizer(mms);
                String id = st.nextToken();

                this.setUnitID(id);
            }

            this.setVisible(false);
            return;
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    /**
     * @return the ID of the unit selected via OK, or "-1" if the dialog was cancelled/closed
     *         without a selection.
     */
    public String getUnitID() {
        return this.toReturn;
    }

    /**
     * Stores the ID of the unit chosen by the user.
     *
     * @param id the selected unit's ID
     */
    private void setUnitID(String id) {
        this.toReturn = id;
    }
}
