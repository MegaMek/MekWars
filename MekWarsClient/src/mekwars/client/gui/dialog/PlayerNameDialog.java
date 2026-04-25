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

import common.util.SpringLayoutHelper;
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

public class PlayerNameDialog extends javax.swing.JDialog implements java.awt.event.ActionListener {

    /**
     *
     */
    private static final long serialVersionUID = -2185532842152633162L;
    //variables
    private javax.swing.JList<String> matchingPlayersList;
    private javax.swing.JScrollPane scrollPane;//holds the JList
    private javax.swing.JTextField nameField;//input field

    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final String okayCommand = "Okay";

    public static final int ANY_PLAYER = 0;
    public static final int FACTION_ONLY = 1;
    public static final int MERCS_ONLY = 2;

    private String toReturn = null;
    private java.util.ArrayList<String> possiblePlayers = null;

    //constructor
    public PlayerNameDialog(client.MWClient client, String boxText, int playerType) {

        /*
         * NOTE: variables are final in order to
         * allow access by caretUpdate()
         */

        //super, and variable saves
        super(client.getMainFrame(), boxText, true);//dummy frame as owner

        //loop through all players, checking faction, if needed
        java.util.Vector<String> factionPlayers = new java.util.Vector<String>(1, 1);
        java.util.Iterator<client.CUser> i = client.getUsers().iterator();
        if (playerType == FACTION_ONLY) {
            while (i.hasNext()) {
                client.CUser user = i.next();
                if (user.isInvis() &&
                          user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel()) {continue;}
                if (user.getHouse().equalsIgnoreCase(client.getPlayer().getHouse()) &&
                          !user.getName().equals(client.getPlayer().getName())) {factionPlayers.add(user.getName());}
            }
        } else if (playerType == MERCS_ONLY) {
            while (i.hasNext()) {
                client.CUser user = (client.CUser) i.next();
                if (user.isInvis() &&
                          user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel()) {continue;}
                if (user.isMerc()) {factionPlayers.add(user.getName());}
            }
        } else {
            while (i.hasNext()) {
                client.CUser user = (client.CUser) i.next();
                if (user.isInvis() &&
                          user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel()) {continue;}
                factionPlayers.add(user.getName());
            }
        }

        //alpha sort the users
        java.util.Collections.sort(factionPlayers);

        //setup the a list of names to feed into a list
        final String[] playerNames = factionPlayers.toArray(new String[factionPlayers.size()]);


        //construct the faction name list
        matchingPlayersList = new javax.swing.JList<String>(playerNames);
        matchingPlayersList.setVisibleRowCount(10);
        matchingPlayersList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);

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
                            matchingPlayersList.setListData(playerNames);
                            return;
                        }

                        possiblePlayers = new java.util.ArrayList<String>();
                        text = text.toLowerCase();

                        int until = playerNames.length;
                        for (int i = 0; i < until; i++) {
                            String currPlayer = (String) playerNames[i];
                            if (currPlayer.toLowerCase().indexOf(text) != -1) {possiblePlayers.add(currPlayer);}
                        }

                        matchingPlayersList.setListData(possiblePlayers.toArray(new String[possiblePlayers.size()]));

                        /*
                         * Try to select a player with a STARTING string which matched
                         * the seach index. If none is available, use the first faction.
                         *
                         * Hacky, but functional. @urgru 5.2.05
                         */
                        boolean shouldContinue = true;
                        int element = 0;
                        java.util.Iterator<String> it = possiblePlayers.iterator();
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

                    }
                }.start();
            }
        });

        //put the list in a scroll pane
        scrollPane = new javax.swing.JScrollPane(matchingPlayersList);
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

            String selectedPlayer = (String) matchingPlayersList.getSelectedValue();
            if (selectedPlayer == null) {selectedPlayer = nameField.getText();}

            if (matchingPlayersList.getModel().getSize() == 1) {
                selectedPlayer = (String) matchingPlayersList.getModel().getElementAt(0);
            }

            if (selectedPlayer == null || selectedPlayer.equals("")) {return;}

            this.setPlayerName(selectedPlayer);
/*			for (Iterator it = mwclient.getUsers().iterator(); it.hasNext();) {
				CUser currUser = (CUser)it.next();
				if (selectedPlayer.equals(currUser.getName())) {
					this.setPlayerName(currUser.getName());*/
            this.setVisible(false);
            //this.dispose();
            return;
/*				}
			}*/
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

    private void setPlayerName(String name) {
        this.toReturn = name;
    }

    public String getPlayerName() {
        return this.toReturn;
    }
}
