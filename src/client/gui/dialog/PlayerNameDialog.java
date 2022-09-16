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

package client.gui.dialog;

//awt imports
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import client.CUser;
import client.MWClient;
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

public class PlayerNameDialog extends JDialog implements ActionListener {
	
	/**
     * 
     */
    private static final long serialVersionUID = -2185532842152633162L;
    //variables
	private JList<String> matchingPlayersList;
	private JScrollPane scrollPane;//holds the JList
	private JTextField nameField;//input field
	
	private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");	
	private final String okayCommand = "Okay";
	
    public static final int ANY_PLAYER = 0;
    public static final int FACTION_ONLY = 1;
    public static final int MERCS_ONLY = 2;
    
	private String toReturn = null;
	private ArrayList<String> possiblePlayers = null;
	
	//constructor
	public PlayerNameDialog(MWClient client, String boxText, int playerType) {
		
		/*
		 * NOTE: variables are final in order to
		 * allow access by caretUpdate()
		 */
		
		//super, and variable saves
		super(client.getMainFrame(),boxText, true);//dummy frame as owner
		
		//loop through all players, checking faction, if needed
		Vector<String> factionPlayers = new Vector<String>(1,1);
		Iterator<CUser> i = client.getUsers().iterator();
		if (playerType == FACTION_ONLY) {
			while (i.hasNext()) {
				CUser user = i.next();
                if ( user.isInvis() && user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel() )
                    continue;
				if (user.getHouse().equalsIgnoreCase(client.getPlayer().getHouse()) && !user.getName().equals(client.getPlayer().getName()))
					factionPlayers.add(user.getName());
			}
		} else if ( playerType == MERCS_ONLY ) {
            while (i.hasNext()) {
                CUser user = (CUser)i.next();
                if ( user.isInvis() && user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel() )
                    continue;
                if ( user.isMerc())
                    factionPlayers.add(user.getName());
            }
        }
         else {
			while (i.hasNext()) {
				CUser user = (CUser)i.next();
                if ( user.isInvis() && user.getUserlevel() > client.getUser(client.getPlayer().getName()).getUserlevel() )
                    continue;
				factionPlayers.add(user.getName());
			}
		}
		
		//alpha sort the users
		Collections.sort(factionPlayers);
		
		//setup the a list of names to feed into a list
		final String[] playerNames = factionPlayers.toArray(new String[factionPlayers.size()]);
		
		
		//construct the faction name list
		matchingPlayersList = new JList<String>(playerNames);
		matchingPlayersList.setVisibleRowCount(10);
		matchingPlayersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		//the name field, for user input. caretUpdate
		//does most of the work to update list contents
		nameField = new JTextField();//field for user input
		nameField.addCaretListener(new CaretListener(){
			public void caretUpdate(CaretEvent e) {
				new Thread() {
					@Override
					public void run(){
						String text = nameField.getText();
						if (text == null || text.equals("")) {
							matchingPlayersList.setListData(playerNames);
							return;
						}
						
						possiblePlayers = new ArrayList<String>();
						text = text.toLowerCase();
						
						int until = playerNames.length;
						for (int i = 0; i < until; i++) {
							String currPlayer = (String)playerNames[i];
							if (currPlayer.toLowerCase().indexOf(text) != -1)
								possiblePlayers.add(currPlayer);
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
						
					}
				}.start();
			}
		});
		
		//put the list in a scroll pane
		scrollPane = new JScrollPane(matchingPlayersList);
		scrollPane.setAlignmentX(LEFT_ALIGNMENT);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		//set up listeners for the buttons
		okayButton.setActionCommand(okayCommand);
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		//do some formatting. rawr.
		JPanel springPanel = new JPanel(new SpringLayout());
		springPanel.add(nameField);
		springPanel.add(scrollPane);
		SpringLayoutHelper.setupSpringGrid(springPanel,2,1);
		
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
	 * OK or CANCEL buttons pressed. Handle any
	 * changes and then close the dialouge.
	 */
	public void actionPerformed(ActionEvent event) {
		
		String command = event.getActionCommand();
		
		if (command.equals(okayCommand)) {
			
			String selectedPlayer = (String)matchingPlayersList.getSelectedValue();
	        if (selectedPlayer == null)
	        	selectedPlayer = nameField.getText();
			
	        if (matchingPlayersList.getModel().getSize() == 1)
	        	selectedPlayer = (String)matchingPlayersList.getModel().getElementAt(0);
			
	        if (selectedPlayer == null || selectedPlayer.equals(""))
	        	return;
	        
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
		
		Dimension curDim = this.getSize();
		
		int height = 0;
		int width = 0;
		boolean shouldRedraw = false;
		
		if (curDim.getWidth() < 300) {
			width = 300;
			shouldRedraw = true;
		} else
			width = (int)curDim.getWidth();
		
		if (curDim.getHeight() < 150) {
			height = 150;
			shouldRedraw = true;
		} else
			height = (int)curDim.getHeight();
		
		if (shouldRedraw) {
			this.setSize(new Dimension(width, height));
		}
		
	}//end checkMinimumSize
	
	private void setPlayerName(String name){
		this.toReturn = name;
	}
	
	public String getPlayerName(){
		return this.toReturn;
	}
}
