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

/**
 * Base dialog, derived from MMNET's SearchPlanetListener, allows players
 * to search for planets using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 * <p>
 * The dialog shows a text field and a live-filtered {@code JList} of every known planet name.
 * As the player types, a background {@link Thread} is spawned on every caret update to
 * recompute the matching subset and auto-select the best prefix match (see the caret listener
 * in the constructor for details, including a documented "hacky but functional" selection
 * heuristic). Pressing OK jumps the given {@link InnerStellarMap} to the selected/typed planet
 * (activating and remembering it as the map's current selection) and closes the dialog;
 * pressing Cancel (or any non-OK action) just disposes the dialog without changing the map.
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
    /** The star map view to jump to the selected planet on. */
    private final InnerStellarMap map;
    /** All planets known to the client, used both to build the name list and resolve a name back to a Planet. */
    private final java.util.Collection<Planet> planets;
    /** Alphabetically-sorted set of all planet names, the unfiltered source for the search list. */
    private final java.util.TreeSet<String> planetNames;

    /** List box showing the planet names currently matching the search text. */
    private final javax.swing.JList<String> matchingPlanetsList;
    /** Text field the player types a partial planet name into to filter {@link #matchingPlanetsList}. */
    private final javax.swing.JTextField nameField;//input field
    /** Action command identifying the OK button in {@link #actionPerformed}. */
    private final String okayCommand = "Okay";

    /**
     * Builds the search dialog: a text field, a live-filtered list of planet names, and OK/Cancel
     * buttons, then packs, sizes, and centers it. Does not show the dialog automatically.
     *
     * @param map    the stellar map to update with the chosen planet on OK
     * @param client the client, used to obtain the main frame (as owner) and the full planet list
     */
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
        // Every caret movement (including each keystroke) spawns a brand-new background Thread
        // that recomputes the filtered planet list and picks a default selection. Note: since
        // this touches Swing components (matchingPlanetsList) off the Event Dispatch Thread,
        // it is not strictly EDT-safe, though in practice it tends to work due to timing.
        nameField.addCaretListener(caretEvent -> new Thread() {
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
     * Ensures the dialog is not shrunk below a usable minimum size (300x300) after packing.
     * Resizes the dialog only if the current size is smaller than the minimum in either dimension.
     */
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

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     * <p>
     * On OK: prefers the list selection; if nothing is selected in the list, falls back to the
     * raw text field contents. If that is still empty, the dialog stays open and does nothing.
     * If exactly one planet remains in the filtered list, that single planet is used regardless
     * of what's typed/selected. The chosen name is then matched (exact, case-sensitive) against
     * the full {@link #planets} collection; on a match, the planet becomes the map's selection
     * (selected, activated, and saved) and the dialog closes. If no planet matches the typed
     * name, an "Unknown Planet" message dialog is shown and then the dialog is disposed anyway
     * (falls through to the trailing {@code dispose()} below).
     * <p>
     * Any other action command (i.e. Cancel) just disposes the dialog without touching the map.
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

}
