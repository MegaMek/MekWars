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

package mekwars.client.gui.dialog;

//awt imports

import common.Unit;
import common.util.SpringLayoutHelper;
import megamek.common.Infantry;

//util imports
//swing imports
//mekwars imports

/*
 * Dialog, based on HouseNameDialog, which allows players
 * to search for players using partial strings. Takes a
 * boolean to indicate whether to use all players, or only
 * those in the player's faction.
 *
 * @urgru 6.17.05
 */

public class UnitSelectionDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    //variables

    /**
     *
     */
    private static final long serialVersionUID = 16880146524838545L;

    //combo box to pick unit from
    private javax.swing.JComboBox possibleUnits = new javax.swing.JComboBox();

    //buttons
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final String okayCommand = "Okay";

    //label
    private javax.swing.JLabel comboLabel = new javax.swing.JLabel();

    private String toReturn = "-1";
    //private boolean factionOnly = false;

    //constructor
    public UnitSelectionDialog(client.MWClient mwclient, String boxText, String labelText) {

        //super, and variable saves
        super(mwclient.getMainFrame(), boxText, true);//dummy frame as owner

        //make the label
        comboLabel = new javax.swing.JLabel(labelText, javax.swing.SwingConstants.CENTER);
        comboLabel.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        comboLabel.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);

        //populate the combo box
        possibleUnits.setModel(new javax.swing.DefaultComboBoxModel(mwclient.getPlayer().getHangar()) {
            /**
             *
             */
            private static final long serialVersionUID = -3752642922668363196L;

            @Override
            public Object getElementAt(int index) {
                client.campaign.CUnit mm = (client.campaign.CUnit) super.getElementAt(index);
                if (mm.getType() == Unit.MEK || mm.getType() == Unit.VEHICLE || mm.getType() == Unit.AERO) {
                    return (mm.getId() +
                                  " " +
                                  mm.getModelName() +
                                  " [" +
                                  mm.getPilot().getGunnery() +
                                  "/" +
                                  mm.getPilot().getPiloting() +
                                  "]");
                }

                if (mm.getType() == Unit.INFANTRY || mm.getType() == Unit.BATTLEARMOR) {
                    if (((Infantry) mm.getEntity()).canMakeAntiMekAttacks()) {
                        return (mm.getId() +
                                      " " +
                                      mm.getModelName() +
                                      " [" +
                                      mm.getPilot().getGunnery() +
                                      "/" +
                                      mm.getPilot().getPiloting() +
                                      "]");
                    }
                    return (mm.getId() + " " + mm.getModelName() + " [" + mm.getPilot().getGunnery() + "]");
                }
                return (mm.getId() + " " + mm.getModelName() + " [" + mm.getPilot().getGunnery() + "]");
            }
        });

        java.awt.Dimension newDim = new java.awt.Dimension();
        newDim.setSize(possibleUnits.getMinimumSize().getWidth() * 1.25, possibleUnits.getMinimumSize().getHeight());
        possibleUnits.setMaximumSize(newDim);

        //set up listeners for the buttons
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
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
        this.setLocationRelativeTo(mwclient.getMainFrame());
        this.checkMinimumSize();
        this.setResizable(true);

    }

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {

            int index = possibleUnits.getSelectedIndex();
            if (index < 0) {return;}

            String mms = (String) possibleUnits.getSelectedItem();
            java.util.StringTokenizer st = new java.util.StringTokenizer(mms);
            String id = st.nextToken();

            this.setUnitID(id);
            this.setVisible(false);
            //this.dispose();
            return;
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height = 0;
        int width = 0;
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

    private void setUnitID(String id) {
        this.toReturn = id;
    }

    public String getUnitID() {
        return this.toReturn;
    }
}
