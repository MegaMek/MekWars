/*
 * MekWars - Copyright (C) 2007
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
 *
 * Portions of this dialog derived from work done by Imanuel Schultz. Original
 * part of MegaMekNET's client.gui.actions pacakge as SearchHouseActionListener.java.
 * See http://www.sourceforge.net/projects/megameknet for more info.
 */

package mekwars.client.gui.dialog;

//awt imports

import common.House;
import common.util.SpringLayoutHelper;
//util imports
//swing imports
//mekwars imports


public class SubFactionNameDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 3552906075410667280L;
    //variables
    private final java.util.TreeSet<String> subFactionNames;
    private House faction = null;

    private javax.swing.JList<String> matchingHousesList;
    private javax.swing.JScrollPane scrollPane;//holds the JList
    private javax.swing.JTextField nameField;//input field
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final String okayCommand = "Okay";

    private String subFactionName = null;
    private boolean addblank = false;

    //constructor
    public SubFactionNameDialog(client.MWClient mwclient, String boxText, String factionName) {

        /*
         * NOTE: variables are final in order to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(mwclient.getMainFrame(), boxText, true);//dummy frame as owner
        this.faction = mwclient.getData().getHouseByName(factionName);

        //setup the a list of names to feed into a list
        this.subFactionNames = new java.util.TreeSet<String>();//tree to alpha sort

        if (faction == null) {return;}

        for (String subFaction : faction.getSubFactionList().keySet()) {
            subFactionNames.add(subFaction);
        }
        final String[] allSubFactionNames = subFactionNames.toArray(new String[subFactionNames.size()]);

        //construct the faction name list
        matchingHousesList = new javax.swing.JList<String>(allSubFactionNames);
        matchingHousesList.setVisibleRowCount(10);
        matchingHousesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new javax.swing.JTextField();//field for user input
        nameField.addCaretListener(new javax.swing.event.CaretListener() {
            public void caretUpdate(javax.swing.event.CaretEvent e) {
                new Thread() {
                    @Override
                    public void run() {
                        String text = nameField.getText();
                        if (text == null || text.equals("")) {
                            matchingHousesList.setListData(allSubFactionNames);
                            return;
                        }
                        java.util.ArrayList<String> possibleHouses = new java.util.ArrayList<String>();
                        text = text.toLowerCase();
                        for (String subFaction : faction.getSubFactionList().keySet()) {
                            if (subFaction.toLowerCase().indexOf(text) != -1) {possibleHouses.add(subFaction);}
                        }
                        matchingHousesList.setListData(possibleHouses.toArray(new String[possibleHouses.size()]));

                        /*
                         * Try to select a faction with a STARTING string which matched
                         * the seach index. If none is available, use the first faction.
                         *
                         * Hacky, but functional. @urgru 5.2.05
                         */
                        boolean shouldContinue = true;
                        int element = 0;
                        java.util.Iterator<String> it = possibleHouses.iterator();
                        while (it.hasNext() && shouldContinue) {
                            String name = it.next();
                            if (name.toLowerCase().startsWith(text)) {
                                matchingHousesList.setSelectedIndex(element);
                                shouldContinue = false;
                            }
                            element++;
                        }

                        //looped through without finding a starting match. set 0.
                        if (shouldContinue) {
                            matchingHousesList.setSelectedIndex(0);
                        }

                    }
                }.start();
            }
        });

        //put the list in a scroll pane
        scrollPane = new javax.swing.JScrollPane(matchingHousesList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        //do some formatting. rawr.
        javax.swing.JPanel springPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        springPanel.add(nameField);
        springPanel.add(scrollPane);
        SpringLayoutHelper.setupSpringGrid(springPanel, 2, 1);

        javax.swing.JPanel buttonFlow = new javax.swing.JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        javax.swing.JPanel generalLayout = new javax.swing.JPanel();
        generalLayout.setLayout(new javax.swing.BoxLayout(generalLayout, javax.swing.BoxLayout.Y_AXIS));
        generalLayout.add(springPanel);
        generalLayout.add(buttonFlow);
        this.getContentPane().add(generalLayout);
        this.pack();

        this.checkMinimumSize();
        this.setResizable(true);

        //set a default button
        this.getRootPane().setDefaultButton(okayButton);

        //center the dialog.
        this.setLocationRelativeTo(mwclient.getMainFrame());

    }


    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedHouse = (String) matchingHousesList.getSelectedValue();
            if (selectedHouse == null) {selectedHouse = nameField.getText();}
            if (!addblank && (selectedHouse == null || selectedHouse.equals(""))) {return;}
            if (matchingHousesList.getModel().getSize() == 1) {
                selectedHouse = (String) matchingHousesList.getModel().getElementAt(0);
            }
            for (String subFaction : faction.getSubFactionList().keySet()) {
                if (selectedHouse.equals(subFaction)) {
                    this.setSubFactionName(subFaction);
                    this.setVisible(false);
                    //this.dispose();
                    return;
                }
            }
            //New SubFaction
            this.setSubFactionName(nameField.getText());
            this.setVisible(false);
            return;
            //JOptionPane.showMessageDialog(null,"Unknown House");
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height = 0;
        int width = 0;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 300) {
            width = 300;
            shouldRedraw = true;
        } else {width = (int) curDim.getWidth();}

        if (curDim.getHeight() < 150) {
            height = 150;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new java.awt.Dimension(width, height));
        }

    }//end checkMinimumSize

    private void setSubFactionName(String name) {
        this.subFactionName = name;
    }

    public String getSubFactionName() {
        return this.subFactionName;
    }
}
