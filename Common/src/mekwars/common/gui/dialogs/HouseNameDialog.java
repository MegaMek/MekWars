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
 *
 * Portions of this dialog derived from work done by Imanuel Schultz. Original
 * part of MegaMekNET's client.gui.actions pacakge as SearchHouseActionListener.java.
 * See http://www.sourceforge.net/projects/megameknet for more info.
 */

package mekwars.common.gui.dialogs;

/*
 * Base dialog, derived from MMNET's SearchHouseListener, allows players
 * to search for factions using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 *
 * @urgru 5.2.05
 * used code that urgru started to make cookie cut dialog boxes for faction
 * and planets for commands requiring that input.
 *
 * @Torren 5.6.05
 */

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.Collection;
import java.util.TreeSet;
import javax.swing.*;

import mekwars.common.House;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public class HouseNameDialog extends JDialog implements ActionListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -1908461615647395978L;
    //variables
    private final Collection<House> factions;
    private final TreeSet<String> factionNames;

    private final JList<String> matchingHousesList;
    private final JTextField nameField;//input field
    private final String okayCommand = "Okay";
    private final boolean addBlank;
    private String factionName = null;

    //constructor
    public HouseNameDialog(IClient client, String boxText, boolean addBlank, boolean showCanDefectTo) {

        /*
         * NOTE: variables are final to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner
        this.factions = client.getData().getAllHouses();
        this.addBlank = addBlank;

        //set up a list of names to feed into a list
        factionNames = new java.util.TreeSet<>();//tree to alpha sort
        for (House house : factions) {
            if (showCanDefectTo && !house.getHouseDefectionTo()) {continue;}
            factionNames.add(house.getName());
        }
        final String[] allHouseNames = factionNames.toArray(new String[factionNames.size()]);

        //construct the faction name list
        matchingHousesList = new javax.swing.JList<>(allHouseNames);
        matchingHousesList.setVisibleRowCount(10);
        matchingHousesList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new javax.swing.JTextField();//field for user input
        nameField.addCaretListener(caretEvent -> new Thread() {
            @Override
            public void run() {
                String text = nameField.getText();
                if (text == null || text.isEmpty()) {
                    matchingHousesList.setListData(allHouseNames);
                    return;
                }
                java.util.ArrayList<String> possibleHouses = new java.util.ArrayList<>();
                text = text.toLowerCase();
                for (String curHouse : factionNames) {
                    if (curHouse.toLowerCase().contains(text)) {possibleHouses.add(curHouse);}
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
        }.start());

        //put the list in a scroll pane
        //holds the JList
        JScrollPane scrollPane = new JScrollPane(matchingHousesList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        JButton okayButton = new JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

        //do some formatting. rawr.
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.add(nameField);
        springPanel.add(scrollPane);
        SpringLayoutHelper.setupSpringGrid(springPanel, 2, 1);

        JPanel buttonFlow = new JPanel();
        buttonFlow.add(okayButton);
        buttonFlow.add(cancelButton);

        JPanel generalLayout = new JPanel();
        generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
        generalLayout.add(springPanel);
        generalLayout.add(buttonFlow);
        this.getContentPane().add(generalLayout);
        this.pack();

        this.checkMinimumSize();
        this.setResizable(true);

        //set a default button
        this.getRootPane().setDefaultButton(okayButton);

        //center the dialog.
        this.setLocationRelativeTo(null);

    }

    private void checkMinimumSize() {

        Dimension curDim = this.getSize();

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
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedHouse = matchingHousesList.getSelectedValue();
            if (selectedHouse == null) {
                selectedHouse = nameField.getText();
            }
            if (!addBlank && (selectedHouse == null || selectedHouse.isEmpty())) {return;}
            if (matchingHousesList.getModel().getSize() == 1) {
                selectedHouse = matchingHousesList.getModel().getElementAt(0);
            }
            for (House faction : factions) {
                if (selectedHouse.equals(faction.getName())) {
                    this.setHouseName(faction.getName());
                    this.setVisible(false);
                    return;
                }
            }
            JOptionPane.showMessageDialog(null, "Unknown House");
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    public String getHouseName() {
        return this.factionName;
    }

    private void setHouseName(String name) {
        this.factionName = name;
    }
}
