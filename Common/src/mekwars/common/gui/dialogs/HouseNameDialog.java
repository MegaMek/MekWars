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

/**
 * Modal "type-ahead" picker dialog used to select a {@link House} (faction) by name.
 * <p>
 * The dialog shows a text field and a list of all known faction names; as the player types, the list is
 * filtered (via a caret listener) down to the faction names containing the typed substring, with the first
 * name that starts with the typed text auto-selected. Used anywhere a command needs a faction name as input
 * (e.g. admin/GM commands that operate on a House), so the caller doesn't have to make players type an exact,
 * correctly-cased faction name.
 * <p>
 * After the dialog is closed via OK, callers should call {@link #getHouseName()} to retrieve the chosen faction
 * name (or {@code null}/blank if none was resolved, depending on {@code addBlank}).
 */
public class HouseNameDialog extends JDialog implements ActionListener {

    /**
     * Serialization id for this {@link JDialog} subclass.
     */
    @Serial
    private static final long serialVersionUID = -1908461615647395978L;
    //variables
    /** All factions known to the client, used to validate the final selection and resolve it back to a House. */
    private final Collection<House> factions;
    /** Alphabetically-sorted (TreeSet) set of all candidate faction names shown/filtered in the list. */
    private final TreeSet<String> factionNames;

    /** List box showing the faction names currently matching the text field's filter. */
    private final JList<String> matchingHousesList;
    private final JTextField nameField;//input field
    /** Action command string used by the OK button so {@link #actionPerformed} can identify it. */
    private final String okayCommand = "Okay";
    /** If true, allows the dialog to accept an empty/blank selection as a valid result (e.g. "no faction"). */
    private final boolean addBlank;
    /** Result of the dialog once OK is pressed and a valid faction is resolved; null otherwise. */
    private String factionName = null;

    //constructor
    /**
     * Builds and displays the faction-name search dialog.
     *
     * @param client          active client connection; supplies the full list of known Houses and the owning
     *                        frame used to center the dialog.
     * @param boxText         title text shown on the dialog's title bar.
     * @param addBlank        if true, an empty selection/typed value is accepted as a valid (blank) result rather
     *                        than being rejected by {@link #actionPerformed}.
     * @param showCanDefectTo if true, restricts the candidate list to only those factions players are allowed to
     *                        defect to (see {@link House#getHouseDefectionTo()}); if false, all factions are shown.
     */
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
        // Every caret movement (including typed characters) spawns a new background Thread that
        // re-filters factionNames against the current text and refreshes the JList.
        // QUIRK: this mutates Swing components (matchingHousesList) off the Event Dispatch Thread,
        // which is not thread-safe per Swing's threading rules; it "works" in practice but is technically unsafe.
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

    /**
     * Enforces a minimum dialog size of 300x150, resizing the dialog if the packed layout ended up smaller.
     */
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
     * Handles both the OK and Cancel buttons (Cancel has no action command set, so it falls through to the
     * final {@code dispose()} without resolving a faction).
     * <p>
     * On OK: prefers the currently-selected list item, falling back to the raw text field contents if nothing
     * is selected; if the filtered list contains exactly one entry, that entry wins regardless of what was
     * selected/typed. The resolved name is matched (exact, case-sensitive) against the known factions; on a
     * match the result is stored via {@link #setHouseName(String)} and the dialog is hidden (but not disposed,
     * so {@link #getHouseName()} still returns a value after {@code setVisible(false)} returns). On no match, an
     * "Unknown House" message dialog is shown and the dialog falls through to {@code dispose()} without setting
     * a result. If {@code addBlank} is false, an empty resolved name simply returns without doing anything
     * (leaving the dialog open).
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

    /**
     * @return the faction name chosen by the player, or {@code null} if the dialog was cancelled or closed
     *       without resolving a valid faction.
     */
    public String getHouseName() {
        return this.factionName;
    }

    /**
     * Stores the resolved faction name so {@link #getHouseName()} can return it after the dialog closes.
     *
     * @param name the matched faction's name.
     */
    private void setHouseName(String name) {
        this.factionName = name;
    }
}
