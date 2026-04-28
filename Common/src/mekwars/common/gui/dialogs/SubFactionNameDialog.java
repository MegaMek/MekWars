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

package mekwars.common.gui.dialogs;


import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import mekwars.common.House;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public class SubFactionNameDialog extends JDialog implements ActionListener {
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 3552906075410667280L;
    private final House faction;
    private final String okayCommand = "Okay";
    private JList<String> matchingHousesList;
    private JTextField nameField;//input field
    private String subFactionName = null;

    //constructor
    public SubFactionNameDialog(IClient client, String boxText, String factionName) {

        /*
         * NOTE: variables are final in order to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner
        this.faction = client.getData().getHouseByName(factionName);

        //setup the a list of names to feed into a list
        //variables

        if (faction == null) {return;}

        //tree to alpha sort
        TreeSet<String> subFactionNames = new TreeSet<>(faction.getSubFactionList().keySet());
        final String[] allSubFactionNames = subFactionNames.toArray(new String[subFactionNames.size()]);

        //construct the faction name list
        matchingHousesList = new JList<>(allSubFactionNames);
        matchingHousesList.setVisibleRowCount(10);
        matchingHousesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new JTextField();//field for user input
        nameField.addCaretListener(_ -> new Thread() {
            @Override
            public void run() {
                String text = nameField.getText();
                if (text == null || text.isEmpty()) {
                    matchingHousesList.setListData(allSubFactionNames);
                    return;
                }
                ArrayList<String> possibleHouses = new ArrayList<>();
                text = text.toLowerCase();
                for (String subFaction : faction.getSubFactionList().keySet()) {
                    if (subFaction.toLowerCase().contains(text)) {possibleHouses.add(subFaction);}
                }
                matchingHousesList.setListData(possibleHouses.toArray(new String[possibleHouses.size()]));

                /*
                 * Try to select a faction with a STARTING string which matched
                 * the search index. If none is available, use the first faction.
                 *
                 * Hacky, but functional. @urgru 5.2.05
                 */
                boolean shouldContinue = true;
                int element = 0;
                Iterator<String> it = possibleHouses.iterator();
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
        }.start());

        //put the list in a scroll pane
        //holds the JList
        JScrollPane scrollPane = new JScrollPane(matchingHousesList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        JButton okayButton = new JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
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
        this.setLocationRelativeTo(client.getMainFrame());
    }

    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height;
        int width;
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

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedHouse = matchingHousesList.getSelectedValue();
            if (selectedHouse == null) {selectedHouse = nameField.getText();}
            boolean addBlank = false;
            if (!addBlank && (selectedHouse == null || selectedHouse.isEmpty())) {return;}
            if (matchingHousesList.getModel().getSize() == 1) {
                selectedHouse = matchingHousesList.getModel().getElementAt(0);
            }
            for (String subFaction : faction.getSubFactionList().keySet()) {
                if (selectedHouse.equals(subFaction)) {
                    this.setSubFactionName(subFaction);
                    this.setVisible(false);
                    return;
                }
            }
            //New SubFaction
            this.setSubFactionName(nameField.getText());
            this.setVisible(false);
            return;
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    public String getSubFactionName() {
        return this.subFactionName;
    }

    private void setSubFactionName(String name) {
        this.subFactionName = name;
    }
}
