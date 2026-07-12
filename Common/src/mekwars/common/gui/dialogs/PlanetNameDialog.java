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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Modal dialog that lets a user pick a planet by name, with live type-ahead filtering as they
 * type. Used wherever a command needs a planet-name argument — most commonly an admin/GM action,
 * or an "Operation" step that requires the player to choose an eligible target world.
 * <p>
 * When {@code opProps} is {@code null}, every known planet is selectable (a general-purpose planet
 * picker). When {@code opProps} is supplied, the constructor pre-filters the candidate list down
 * to only those planets that satisfy a fairly involved set of Operation-specific eligibility rules
 * (faction ownership thresholds, home world restrictions, factory presence, required/forbidden
 * planet flags, and reachability from an existing friendly world within a given range) — see the
 * constructor for details on each rule.
 * <p>
 * The user narrows the candidate list by typing into {@link #nameField}; {@link #matchingPlanetsList}
 * updates on every keystroke. Clicking OK (or Cancel) closes the dialog; the caller retrieves the
 * result afterward via {@link #getPlanetName()}, which is {@code null} if the dialog was cancelled
 * or no match could be resolved.
 */
public class PlanetNameDialog extends JDialog implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(PlanetNameDialog.class);

    @Serial
    private static final long serialVersionUID = 3344329118582475184L;
    //variables
    /** Alphabetically-sorted names of all planets currently eligible for selection. */
    private final TreeSet<String> planetNames;
    /** All planets known to the client; used to resolve a chosen name back to a {@link Planet}. */
    private final Collection<Planet> planets;

    /** Live-filtered list of planet names matching the current search text. */
    private final JList<String> matchingPlanetsList;
    private final JTextField nameField;//input field
    private final String okayCommand = "Okay";

    /** Result of the dialog: the chosen planet's name, or {@code null} if cancelled / unresolved. */
    private String planetName = null;

    //constructor
    /**
     * Builds and lays out the dialog, computing the eligible planet list up front.
     *
     * @param client   connection used to read the full planet list and the local player's house/influence
     * @param boxText  title of the dialog window
     * @param opProps  {@code null} for an unfiltered planet picker, otherwise an Operation's
     *                 property array; the indices actually consulted here are: {@code [0]} range,
     *                 {@code [3]} factory-presence filter ("none"/"only"/anything else = don't
     *                 care), {@code [4]} home-world filter ("none"/"only"), {@code [5]} minimum
     *                 on-planet influence to auto-qualify, {@code [6]} minimum influence on a
     *                 launch-point planet, {@code [7]}/{@code [8]} min/max percentage ownership,
     *                 {@code [9]} '$'-delimited legal defender house names (or {@code "allFactions"}
     *                 to skip the check), {@code [10]}/{@code [11]} '^'-delimited required/forbidden
     *                 planet flags. Indices {@code [1]} and {@code [2]} are not read by this
     *                 constructor at all — they may be used by the caller before building this
     *                 array, or may simply be unused placeholders; worth double-checking at the
     *                 call site if this ever looks like a bug.
     */
    public PlanetNameDialog(IClient client, String boxText, String[] opProps) {

        /*
         * NOTE: variables are final to allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner
        this.planets = client.getData().getAllPlanets();
        //set up a list of names to feed into a list
        planetNames = new TreeSet<>();//tree to alpha sort

        /*
         * Loop through planets. If there is no range info, the menu is
         * a general planet selection dialog and may ignore any faction
         * range info or other Operation specific filtering.
         */

        if (opProps == null) { //no filtering needed
            for (Planet planet : planets) {
                planetNames.add(planet.getName());
            }
        } else {//we need to filter. ugh.
            // For each known planet, evaluate a chain of eligibility checks (any failed check
            // skips the planet via `continue`); a planet that passes every check that applies to
            // it is added to planetNames. Note opProps is re-read on every loop iteration even
            // though its values don't change per-planet -- harmless but wasteful.
            for (Planet planet : planets) {
                //get the planet
                //load relevant properties.
                double range = MathUtility.parseDouble(opProps[0], 0.0);
                String facInfo = opProps[3];
                String homeInfo = opProps[4];
                int launchOn = MathUtility.parseInt(opProps[5], 0);
                int launchFrom = MathUtility.parseInt(opProps[6], 0);
                int minOwn = MathUtility.parseInt(opProps[7], 0);
                int maxOwn = MathUtility.parseInt(opProps[8], 0);
                String legalDefenders = opProps[9];
                String allowPlanetFlags = opProps[10];
                String disallowPlanetFlags = opProps[11];

                //only check for a legal defender limits if necessary
                if (!legalDefenders.equals("allFactions")) {
                    TreeMap<String, Object> legalDefTree = new TreeMap<>();
                    StringTokenizer legalDefTokenizer = new StringTokenizer(legalDefenders, "$");
                    while (legalDefTokenizer.hasMoreTokens()) {
                        legalDefTree.put(legalDefTokenizer.nextToken(), null);
                    }

                    Iterator<House> houseIt = planet.getInfluence().getHouses().iterator();
                    boolean foundDefender = false;

                    while (houseIt.hasNext() && !foundDefender) {
                        House currH = houseIt.next();
                        if (legalDefTree.containsKey(currH.getName())) {
                            foundDefender = true;
                        }
                    }

                    if (!foundDefender) {
                        continue;
                    }
                }

                //see if we even want to check this world ...
                if (facInfo.equals("none") && planet.getFactoryCount() > 0) {
                    continue;
                } else if (facInfo.equals("only") && planet.getFactoryCount() < 1) {
                    continue;
                }

                if (planet.isHomeWorld() && homeInfo.equals("none")) {
                    continue;
                } else if (!planet.isHomeWorld() && homeInfo.equals("only")) {
                    continue;
                }

                //save our player's house ID, since we'll be using it frequently.
                int houseID = client.getPlayer().getMyHouse().getId();

                // percentage of the planet's total conquest points currently held by the player's house
                double tpOwned = (double) 100 *
                                       ((double) planet.getInfluence().getInfluence(houseID) /
                                              (double) planet.getConquestPoints());
                //check the ownership requirements
                if (tpOwned < minOwn) {
                    continue;
                }

                if (tpOwned > maxOwn) {
                    continue;
                }

                // Shortcut: if the player's own influence on this planet already meets the
                // "launch on" threshold, it qualifies outright and none of the flag/launch-range
                // checks below need to run.
                if (planet.getInfluence().getInfluence(houseID) >= launchOn) {
                    planetNames.add(planet.getName());
                    continue;
                }

                //Check for allowed planet flags. the planet most have these flags.
                if (!allowPlanetFlags.isEmpty()) {
                    boolean allowOp = true;
                    StringTokenizer stringTokenizer = new StringTokenizer(allowPlanetFlags, "^");

                    while (stringTokenizer.hasMoreTokens()) {
                        String key = stringTokenizer.nextToken();

                        if (key.trim().isEmpty()) {
                            continue;
                        }

                        if (!planet.getPlanetFlags().containsKey(key)) {
                            LOGGER.debug(String.format("%s does not have flag: %s", planet.getName(), key));
                            allowOp = false;
                            break;
                        }
                    }

                    if (!allowOp) {
                        continue;
                    }
                }

                //Check for disallowed planet flags. If the planet has one of these flags
                // The planet will not be allowed.
                if (!disallowPlanetFlags.isEmpty()) {

                    boolean allowOp = true;
                    StringTokenizer stringTokenizer = new StringTokenizer(disallowPlanetFlags, "^");
                    while (stringTokenizer.hasMoreTokens()) {
                        String key = stringTokenizer.nextToken();

                        if (key.trim().isEmpty()) {
                            continue;
                        }

                        if (planet.getPlanetFlags().containsKey(key)) {
                            allowOp = false;
                            break;
                        }
                    }

                    if (!allowOp) {
                        continue;
                    }
                }

                // alas, we now devolve into O^2 and check all planets for possible launchpads to the target world.
                // Better to do this client side and verify once on the server than to force the server to repeatedly
                // make this loop, but my skin still crawls. @urgru 10.10.05
                // (i.e.: for every candidate target planet, scan every other planet looking for one
                // held strongly enough by the player's house, within `range` of the target, to serve
                // as a launch point. This is quadratic in planet count by design/necessity per the
                // comment above -- not something this documentation pass changes.)
                Iterator<Planet> i2 = planets.iterator();
                boolean launchFound = false;
                while (i2.hasNext() && !launchFound) {

                    Planet lp = i2.next();

                    if (lp.getInfluence().getInfluence(houseID) >= launchFrom) {
                        double dist = lp.getPosition().distanceSq(planet.getPosition());
                        if (dist <= range) {
                            launchFound = true;
                            planetNames.add(planet.getName());
                        }
                    }

                }//end while(still looking for launchpoints)

            }//end while(more planets)
        }//end else(must filter)

        final String[] allPlanetNames = planetNames.toArray(new String[planetNames.size()]);

        //construct the planet name list
        matchingPlanetsList = new JList<>(allPlanetNames);
        matchingPlanetsList.setVisibleRowCount(20);
        matchingPlanetsList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new JTextField();//field for user input
        // Live type-ahead filter: every caret event (i.e. every keystroke/edit) in nameField spawns
        // a brand-new background Thread that recomputes the filtered list and mutates
        // matchingPlanetsList directly. Note this touches Swing components off the Event Dispatch
        // Thread, which is not thread-safe in general, and creates a new Thread per keystroke rather
        // than reusing one worker -- both are pre-existing quirks of this implementation, not
        // something this documentation pass changes.
        nameField.addCaretListener(e -> new Thread() {
            @Override
            public void run() {
                String text = nameField.getText();
                if (text == null || text.isEmpty()) {
                    matchingPlanetsList.setListData(allPlanetNames);
                    return;
                }
                ArrayList<String> possiblePlanets = new ArrayList<>();
                text = text.toLowerCase();

                for (String curPlanet : planetNames) {
                    if (curPlanet.toLowerCase().contains(text)) {
                        possiblePlanets.add(curPlanet);
                    }
                }
                matchingPlanetsList.setListData(possiblePlanets.toArray(new String[possiblePlanets.size()]));

                /*
                 * Try to select a planet with a STARTING string which matched
                 * the search index. If none is available, use the first planet.
                 *
                 * Hacky, but functional. @urgru 5.2.05
                 */
                boolean shouldContinue = true;
                int element = 0;
                Iterator<String> it = possiblePlanets.iterator();

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
        JScrollPane scrollPane = new JScrollPane(matchingPlanetsList);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        //set up listeners for the buttons
        JButton okayButton = new JButton("OK");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);
        JButton cancelButton = new JButton("Cancel");
        // No explicit action command is set here, so Swing falls back to the button's label
        // ("Cancel") as its action command -- which deliberately never equals okayCommand
        // ("Okay"), so actionPerformed() below treats any Cancel click as "not OK" and just
        // disposes the dialog without resolving a planet.
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

    /** Enforces a minimum dialog size of 300x300 pixels, resizing only if the current size is smaller in either dimension. */
    private void checkMinimumSize() {
        Dimension curDim = this.getSize();

        int height;
        int width;
        boolean shouldRedraw = false;

        if (curDim.getWidth() < 300) {
            width = 300;
            shouldRedraw = true;
        } else {
            width = (int) curDim.getWidth();
        }

        if (curDim.getHeight() < 300) {
            height = 300;
            shouldRedraw = true;
        } else {
            height = (int) curDim.getHeight();
        }

        if (shouldRedraw) {
            this.setSize(new Dimension(width, height));
        }

    }//end checkMinimumSize

    /**
     * OK or CANCEL buttons pressed. Handle any changes and then close the dialouge.
     * <p>
     * Only the OK path ({@code command.equals(okayCommand)}) does anything before disposing;
     * Cancel (whose action command is its own label, not {@link #okayCommand} — see where
     * {@code cancelButton} is built) falls straight through to {@link #dispose()}.
     * <p>
     * On OK: prefers the highlighted list entry, falling back to the raw text field contents if
     * nothing is selected; if either is empty, the click is silently ignored and the dialog stays
     * open (note this early {@code return} skips the {@code dispose()} call at the bottom, unlike
     * every other path through this method). If exactly one planet remains in the filtered list,
     * that one is used regardless of whether it was actually highlighted (auto-resolves the
     * common "typed enough to be unambiguous but didn't click the list" case). The matched name is
     * then looked up against {@link #planets} to resolve the actual {@link Planet}; on a match,
     * {@link #planetName} is set and the dialog is hidden via {@code setVisible(false)} and the
     * method returns immediately -- notably <em>without</em> reaching the {@code dispose()} call
     * at the end, unlike the "no match found" and Cancel paths, both of which do dispose. This
     * asymmetry (successful selection only hides; every other outcome disposes) is existing
     * behavior and not changed here. If no planet matches the resolved name, {@link #planetName}
     * is explicitly cleared to {@code null} before falling through to {@code dispose()}.
     */
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {
            String selectedPlanet = matchingPlanetsList.getSelectedValue();
            if (selectedPlanet == null) {selectedPlanet = nameField.getText();}
            if (selectedPlanet == null || selectedPlanet.isEmpty()) {
                return;
            }

            if (matchingPlanetsList.getModel().getSize() == 1) {
                selectedPlanet = matchingPlanetsList.getModel().getElementAt(0);
            }

            for (Planet planet : planets) {
                if (selectedPlanet.equals(planet.getName())) {
                    this.setPlanetName(planet.getName());
                    this.setVisible(false);
                    return;
                }
            }

            this.setPlanetName(null);
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    /** @return the planet name chosen by the user, or {@code null} if cancelled / no match resolved */
    public String getPlanetName() {
        return this.planetName;
    }

    /** Sets the dialog's result; {@code null} means "no valid planet chosen". */
    private void setPlanetName(String name) {
        this.planetName = name;
    }
}
