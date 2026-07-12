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

//awt imports

/*
 * Dialog, based on HouseNameDialog, which allows players
 * to search for players using partial strings. Takes a
 * boolean to indicate whether to use all players, or only
 * those in the player's faction.
 *
 * @urgru 6.17.05
 */

import java.awt.event.ActionListener;
import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import javax.swing.*;

import mekwars.common.campaign.CUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Modal "type-ahead" picker dialog used to select a connected player's username, based on {@link HouseNameDialog}.
 * <p>
 * Shows a text field and a list of candidate usernames; the candidate set is restricted up front by
 * {@code playerType} to either all visible players, only players in the local player's own faction, or only
 * mercenary players. As the player types, the list is filtered (via a caret listener) down to names containing
 * the typed substring, auto-selecting the first name that starts with the typed text. Used anywhere a command
 * needs a player name as input, so the caller isn't required to type an exact, correctly-cased username.
 * <p>
 * After the dialog is closed via OK, callers should call {@link #getPlayerName()} to retrieve the chosen
 * username (or {@code null} if the dialog was cancelled).
 */
public class PlayerNameDialog extends JDialog implements ActionListener {

    /** Include every visible player, regardless of faction. */
    public static final int ANY_PLAYER = 0;
    /** Restrict candidates to players in the local player's own faction. */
    public static final int FACTION_ONLY = 1;
    /** Restrict candidates to players flagged as mercenaries ({@link CUser#isMerc()}). */
    public static final int MERCS_ONLY = 2;
    /**
     * Serialization id for this {@link JDialog} subclass.
     */
    @Serial
    private static final long serialVersionUID = -2185532842152633162L;
    //variables
    /** List box showing the player names currently matching the text field's filter. */
    private final JList<String> matchingPlayersList;
    private final JTextField nameField;//input field
    /** Action command string used by the OK button so {@link #actionPerformed} can identify it. */
    private final String okayCommand = "Okay";
    /** Result of the dialog once OK is pressed and a valid player is resolved; null otherwise. */
    private String toReturn = null;
    /** Most recent set of names matching the text field's filter, rebuilt on every caret update. */
    private ArrayList<String> possiblePlayers = null;

    //constructor
    /**
     * Builds and displays the player-name search dialog.
     *
     * @param client     active client connection; supplies the full user list, the local player's identity/faction,
     *                   and the owning frame used to center the dialog.
     * @param boxText    title text shown on the dialog's title bar.
     * @param playerType one of {@link #ANY_PLAYER}, {@link #FACTION_ONLY}, or {@link #MERCS_ONLY}, controlling
     *                   which subset of connected users populate the candidate list.
     */
    public PlayerNameDialog(IClient client, String boxText, int playerType) {

        /*
         * NOTE: variables are final to allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner

        //loop through all players, checking faction, if needed
        // In every branch below, a user flagged isInvisible() is skipped unless the local player's user level
        // is at least as high as that user's level - i.e. invisible players only hide from lower-privileged viewers.
        Vector<String> factionPlayers = new Vector<>(1, 1);
        Iterator<CUser> i = client.getUsers().iterator();
        if (playerType == FACTION_ONLY) {
            while (i.hasNext()) {
                CUser user = i.next();
                if (user.isInvisible() &&
                          user.getUserLevel() > client.getUser(client.getPlayer().getName()).getUserLevel()) {continue;}
                if (user.getHouse().equalsIgnoreCase(client.getPlayer().getHouse()) &&
                          !user.getName().equals(client.getPlayer().getName())) {factionPlayers.add(user.getName());}
            }
        } else if (playerType == MERCS_ONLY) {
            while (i.hasNext()) {
                CUser user = i.next();
                if (user.isInvisible() &&
                          user.getUserLevel() > client.getUser(client.getPlayer().getName()).getUserLevel()) {continue;}
                if (user.isMerc()) {factionPlayers.add(user.getName());}
            }
        } else {
            while (i.hasNext()) {
                CUser user = i.next();
                if (user.isInvisible() &&
                          user.getUserLevel() > client.getUser(client.getPlayer().getName()).getUserLevel()) {continue;}
                factionPlayers.add(user.getName());
            }
        }

        //alpha sort the users
        java.util.Collections.sort(factionPlayers);

        //setup the a list of names to feed into a list
        final String[] playerNames = factionPlayers.toArray(new String[factionPlayers.size()]);


        //construct the faction name list
        matchingPlayersList = new JList<>(playerNames);
        matchingPlayersList.setVisibleRowCount(10);
        matchingPlayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //the name field, for user input. caretUpdate
        //does most of the work to update list contents
        nameField = new JTextField();//field for user input
        // Every caret movement (including typed characters) spawns a new background Thread that re-filters
        // playerNames against the current text and refreshes the JList.
        // QUIRK: this mutates Swing components (matchingPlayersList) off the Event Dispatch Thread, which is
        // not thread-safe per Swing's threading rules; it "works" in practice but is technically unsafe.
        nameField.addCaretListener(caretEvent -> new Thread(() -> {
            String text = nameField.getText();
            if (text == null || text.isEmpty()) {
                matchingPlayersList.setListData(playerNames);
                return;
            }

            possiblePlayers = new ArrayList<>();
            text = text.toLowerCase();

            for (String currPlayer : playerNames) {
                if (currPlayer.toLowerCase().contains(text)) {
                    possiblePlayers.add(currPlayer);
                }
            }

            matchingPlayersList.setListData(possiblePlayers.toArray(new String[possiblePlayers.size()]));

            /*
             * Try to select a player with a STARTING string which matched
             * the search index. If none is available, use the first faction.
             *
             * Hacky, but functional. @urgru 5.2.05
             */
            boolean shouldContinue = true;
            int element = 0;
            Iterator<String> it = possiblePlayers.iterator();
            while (it.hasNext() && shouldContinue) {
                String name = it.next();
                if (name.toLowerCase().startsWith(text)) {
                    matchingPlayersList.setSelectedIndex(element);
                    shouldContinue = false;
                }
                element++;
            }

            //looped through without finding a starting match. set 0.
            if (shouldContinue) {
                matchingPlayersList.setSelectedIndex(0);
            }

        }).start());

        //put the list in a scroll pane
        //holds the JList
        JScrollPane scrollPane = new JScrollPane(matchingPlayersList);
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
     * Handles both the OK and Cancel buttons (Cancel has no action command set, so it falls through to the
     * final {@code dispose()} without resolving a player).
     * <p>
     * On OK: prefers the currently-selected list item, falling back to the raw text field contents if nothing
     * is selected; if the filtered list contains exactly one entry, that entry wins regardless of what was
     * selected/typed. Unlike {@link HouseNameDialog}, the resolved name is <b>not</b> validated against the
     * known player list - whatever non-empty string is resolved is accepted as-is via
     * {@link #setPlayerName(String)}, and the dialog is hidden (but not disposed, so {@link #getPlayerName()}
     * still returns a value after {@code setVisible(false)} returns). An empty resolved name simply returns
     * without doing anything, leaving the dialog open.
     */
    public void actionPerformed(java.awt.event.ActionEvent event) {

        String command = event.getActionCommand();

        if (command.equals(okayCommand)) {

            String selectedPlayer = matchingPlayersList.getSelectedValue();
            if (selectedPlayer == null) {selectedPlayer = nameField.getText();}

            if (matchingPlayersList.getModel().getSize() == 1) {
                selectedPlayer = matchingPlayersList.getModel().getElementAt(0);
            }

            if (selectedPlayer == null || selectedPlayer.isEmpty()) {return;}

            this.setPlayerName(selectedPlayer);
            this.setVisible(false);
            return;
        }

        //dispose of the dialog
        this.dispose();

    }//end actionPerformed

    /**
     * @return the player name chosen by the player, or {@code null} if the dialog was cancelled or closed
     *       without a selection/entry.
     */
    public String getPlayerName() {
        return this.toReturn;
    }

    /**
     * Stores the resolved player name so {@link #getPlayerName()} can return it after the dialog closes.
     *
     * @param name the resolved username.
     */
    private void setPlayerName(String name) {
        this.toReturn = name;
    }
}
