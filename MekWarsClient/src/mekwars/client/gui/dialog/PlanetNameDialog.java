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

package mekwars.client.gui.dialog;

//awt imports

import common.House;
import common.Planet;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;
//util imports
//swing imports
//mekwars imports

/*
 * Base dialog, derived from MMNET's SearchPlanetListener, allows players
 * to search for planets using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 *
 * @urgru 5.2.05
 *
 * used code that urgru started to make cookie cut dialog boxes for faction
 * and planets for commands requiring that input.
 *
 * @Torren 5.6.05
 */

public class PlanetNameDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = 3344329118582475184L;
    //variables
    private final java.util.TreeSet<String> planetNames;
    private final java.util.Collection<Planet> planets;

    private javax.swing.JList<String> matchingPlanetsList;
    private javax.swing.JScrollPane scrollPane;//holds the JList
    private javax.swing.JTextField nameField;//input field
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final String okayCommand = "Okay";

    private String planetName = null;

    //constructor
    public PlanetNameDialog(client.MWClient mwclient, String boxText, String[] opProps) {

        /*
         * NOTE: variables are final in order to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(mwclient.getMainFrame(), boxText, true);//dummy frame as owner
        this.planets = mwclient.getData().getAllPlanets();
        //setup the a list of names to feed into a list
        planetNames = new java.util.TreeSet<String>();//tree to alpha sort

        /*
         * Loop thorugh planets. If there is no range info, the menu is
         * a general planet selection dialog and may ignore any faction
         * range info or other Operation specific filtering.
         */

        if (opProps == null) { //no filtering needed
            for (java.util.Iterator<Planet> it = planets.iterator(); it.hasNext(); ) {
                planetNames.add(it.next().getName());
            }
        } else {//we need to filter. ugh.

            java.util.Iterator<Planet> i = planets.iterator();
            while (i.hasNext()) {

                //get the planet
                Planet tp = (Planet) i.next();

                //load relevant properties.
                double range = Double.parseDouble(opProps[0]);
                String facInfo = opProps[3];
                String homeInfo = opProps[4];
                int launchOn = Integer.parseInt(opProps[5]);
                int launchFrom = Integer.parseInt(opProps[6]);
                int minOwn = Integer.parseInt(opProps[7]);
                int maxOwn = Integer.parseInt(opProps[8]);
                String legalDefenders = opProps[9];
                String allowPlanetFlags = opProps[10];
                String disallowPlanetFlags = opProps[11];
                //boolean reserveOnly is [12], not used here
                //boolean activeOnly is [13], not used here

                //only check for a legal defender limits if necessary
                if (!legalDefenders.equals("allFactions")) {

                    java.util.TreeMap<String, Object> legalDefTree = new java.util.TreeMap<String, Object>();
                    java.util.StringTokenizer legalDefTokenizer = new java.util.StringTokenizer(legalDefenders, "$");
                    while (legalDefTokenizer.hasMoreTokens()) {legalDefTree.put(legalDefTokenizer.nextToken(), null);}

                    java.util.Iterator<House> houseIt = tp.getInfluence().getHouses().iterator();
                    boolean foundDefender = false;
                    while (houseIt.hasNext() && !foundDefender) {
                        House currH = houseIt.next();
                        if (legalDefTree.containsKey(currH.getName())) {foundDefender = true;}
                    }

                    if (!foundDefender) {continue;}
                }

                //see if we even want to check this world ...
                if (facInfo.equals("none") && tp.getFactoryCount() > 0) {continue;} else if (facInfo.equals("only") &&
                                                                                                   tp.getFactoryCount() < 1) {
                    continue;
                }

                if (tp.isHomeWorld() && homeInfo.equals("none")) {continue;} else if (!tp.isHomeWorld() &&
                                                                                            homeInfo.equals("only")) {
                    continue;
                }

                //save our player's house ID, since we'll be using it frequently.
                int houseID = mwclient.getPlayer().getMyHouse().getId();

                double tpOwned = (double) 100 *
                                       ((double) tp.getInfluence().getInfluence(houseID) /
                                              (double) tp.getConquestPoints());
                //check the ownership requirements
                if (tpOwned < minOwn) {continue;}
                if (tpOwned > maxOwn) {continue;}

                //check the on-planet launch
                if (tp.getInfluence().getInfluence(houseID) >= launchOn) {
                    planetNames.add(tp.getName());
                    continue;
                }

                //Check for allowed planet flags. the planet most have these flags.
                if (allowPlanetFlags.length() > 0) {
                    //MWLogger.errLog(currOpName+" AllowPlanetFlags: "+allowPlanetFlags);
                    boolean allowOp = true;
                    java.util.StringTokenizer st = new java.util.StringTokenizer(allowPlanetFlags, "^");
                    while (st.hasMoreTokens()) {
                        String key = st.nextToken();

                        if (key.trim().length() < 1) {continue;}
                        if (!tp.getPlanetFlags().containsKey(key)) {
                            MWLogger.errLog(tp.getName() + " does not have flag: " + key);
                            allowOp = false;
                            break;
                        }
                    }
                    if (!allowOp) {continue;}
                }

                //Check for disallowed planet flags. If the planet has one of these flags
                // The planet will not be allowed.
                if (disallowPlanetFlags.length() > 0) {
                    //MWLogger.errLog(currOpName+" DisallowPlanetFlags: "+disallowPlanetFlags);

                    boolean allowOp = true;
                    java.util.StringTokenizer st = new java.util.StringTokenizer(disallowPlanetFlags, "^");
                    while (st.hasMoreTokens()) {
                        String key = st.nextToken();

                        if (key.trim().length() < 1) {continue;}
                        if (tp.getPlanetFlags().containsKey(key)) {
                            allowOp = false;
                            break;
                        }
                    }
                    if (!allowOp) {continue;}
                }

                //alas, we now devolve into O^2 and check all planets for possible
                //launchpads to the target world. Better to do this client side and
                //verify once on the server than to force the server to repeatedly
                //make this loop, but my skin still crawls. @urgru 10.10.05
                java.util.Iterator<Planet> i2 = planets.iterator();
                boolean launchFound = false;
                while (i2.hasNext() && !launchFound) {

                    Planet lp = (Planet) i2.next();

                    if (lp.getInfluence().getInfluence(houseID) >= launchFrom) {
                        double tdist = lp.getPosition().distanceSq(tp.getPosition());
                        if (tdist <= range) {
                            launchFound = true;
                            planetNames.add(tp.getName());
                        }
                    }

                }//end while(still looking for launchpoints)

            }//end while(more planets)
        }//end else(must filter)

        //final Object[] allPlanetNames = planetNames.toArray();
        final String[] allPlanetNames = planetNames.toArray(new String[planetNames.size()]);

        //construct the planet name list
        matchingPlanetsList = new javax.swing.JList<String>(allPlanetNames);
        matchingPlanetsList.setVisibleRowCount(20);
        matchingPlanetsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

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
                            matchingPlanetsList.setListData(allPlanetNames);
                            return;
                        }
                        java.util.ArrayList<String> possiblePlanets = new java.util.ArrayList<String>();
                        text = text.toLowerCase();
                        for (java.util.Iterator<String> it = planetNames.iterator(); it.hasNext(); ) {
                            String curPlanet = it.next();
                            if (curPlanet.toLowerCase().indexOf(text) != -1) {possiblePlanets.add(curPlanet);}
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
                }.start();
            }
        });

        //put the list in a scroll pane
        scrollPane = new javax.swing.JScrollPane(matchingPlanetsList);
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
        this.setLocationRelativeTo(null);

    }


    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedPlanet = (String) matchingPlanetsList.getSelectedValue();
            if (selectedPlanet == null) {selectedPlanet = nameField.getText();}
            if (selectedPlanet == null || selectedPlanet.equals("")) {return;}
            if (matchingPlanetsList.getModel().getSize() == 1) {
                selectedPlanet = (String) matchingPlanetsList.getModel().getElementAt(0);
            }
            for (java.util.Iterator<Planet> it = planets.iterator(); it.hasNext(); ) {
                Planet planet = it.next();
                if (selectedPlanet.equals(planet.getName())) {
                    this.setPlanetName(planet.getName());
                    //this.dispose();
                    this.setVisible(false);
                    return;
                }
            }
            this.setPlanetName(null);
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

        if (curDim.getHeight() < 300) {
            height = 300;
            shouldRedraw = true;
        } else {height = (int) curDim.getHeight();}

        if (shouldRedraw) {
            this.setSize(new java.awt.Dimension(width, height));
        }

    }//end checkMinimumSize

    private void setPlanetName(String name) {
        this.planetName = name;
    }

    public String getPlanetName() {
        return this.planetName;
    }
}
