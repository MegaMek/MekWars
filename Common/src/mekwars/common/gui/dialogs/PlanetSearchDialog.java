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
 * part of MegaMekNET's client.gui.actions pacakge as SearchPlanetActionListener.java.
 * See http://www.sourceforge.net/projects/megameknet for more info.
 */

package mekwars.common.gui.dialogs;

//awt imports

import java.io.Serial;

import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.InnerStellarMap;
import mekwars.common.util.SpringLayoutHelper;

/*
 * Base dialog, derived from MMNET's SearchPlanetListener, allows players
 * to search for planets using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 *
 * @urgru 5.2.05
 */

public class PlanetSearchDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7897295866660184584L;
    //variables
    private final InnerStellarMap map;
    private final java.util.Collection<Planet> planets;
    private final java.util.TreeSet<String> planetNames;

    private final javax.swing.JList<String> matchingPlanetsList;
    private final javax.swing.JTextField nameField;//input field
    private final String okayCommand = "Okay";

    //constructor
    public PlanetSearchDialog(InnerStellarMap map, IClient client) {

        /*
         * NOTE: variables are final in order to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), "Planet Search", true);//dummy frame as owner
        this.map = map;
        this.planets = client.getData().getAllPlanets();

        //setup the a list of names to feed into a list
        planetNames = new java.util.TreeSet<>();//tree to alpha sort
        for (Planet planet : planets) {planetNames.add(planet.getName());}
        final String[] allPlanetNames = planetNames.toArray(new String[planetNames.size()]);

        //construct the planet name list
        matchingPlanetsList = new javax.swing.JList<>(allPlanetNames);
        matchingPlanetsList.setVisibleRowCount(20);
        matchingPlanetsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new javax.swing.JTextField();//field for user input
        nameField.addCaretListener(_ -> new Thread() {
            @Override
            public void run() {
                String text = nameField.getText();
                if (text == null || text.isEmpty()) {
                    matchingPlanetsList.setListData(allPlanetNames);
                    return;
                }
                java.util.ArrayList<String> possiblePlanets = new java.util.ArrayList<>();
                text = text.toLowerCase();
                for (String curPlanet : planetNames) {
                    if (curPlanet.toLowerCase().contains(text)) {possiblePlanets.add(curPlanet);}
                }
                matchingPlanetsList.setListData(possiblePlanets.toArray(new String[possiblePlanets.size()]));

                /*
                 * Try to select a planet with a STARTING string which matched
                 * the seach index. If none is available, use the first planet.
                 *
                 * Hacky, but functional. @urgru 5.2.05
                 */
                boolean shouldContinue = true;
                int element = 0;
                java.util.Iterator<String> it = possiblePlanets.iterator();
                while (it.hasNext() && shouldContinue) {
                    String name = it.next();
                    if (name.toLowerCase().startsWith(text)) {
                        matchingPlanetsList.setSelectedIndex(element);
                        shouldContinue = false;
                    }
                    element++;
                }

                //looped through without finding a starting match. set 0.
                if (shouldContinue) {
                    matchingPlanetsList.setSelectedIndex(0);
                }

            }
        }.start());

        //put the list in a scroll pane
        //holds the JList
        javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(matchingPlanetsList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
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
        this.setLocationRelativeTo(null);

    }


    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedPlanet = matchingPlanetsList.getSelectedValue();
            if (selectedPlanet == null) {selectedPlanet = nameField.getText();}
            if (selectedPlanet == null || selectedPlanet.isEmpty()) {return;}
            if (matchingPlanetsList.getModel().getSize() == 1) {
                selectedPlanet = matchingPlanetsList.getModel().getElementAt(0);
            }
            for (Planet planet : planets) {
                if (selectedPlanet.equals(planet.getName())) {
                    map.setSelectedPlanet(planet);
                    map.activate(planet, true);
                    map.saveMapSelection(planet);
                    this.dispose();
                    return;
                }
            }
            javax.swing.JOptionPane.showMessageDialog(null, "Unknown Planet");
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    private void checkMinimumSize() {

        java.awt.Dimension curDim = this.getSize();

        int height;
        int width;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 300) {
            width = 300;
            shouldRedraw = true;
        } else {width = (int) curDim.getWidth();}

        if (curDim.getHeight() < 300) {
            height = 300;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new java.awt.Dimension(width, height));
        }

    }//end checkMinimumSize

}
