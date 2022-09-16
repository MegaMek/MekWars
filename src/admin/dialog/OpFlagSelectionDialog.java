/*
 * MekWars - Copyright (C) 2006
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

package admin.dialog;

//awt imports
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpringLayout;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import client.MWClient;
import common.util.SpringLayoutHelper;
//util imports
//swing imports
//mekwars imports

/*
 * Base dialog, derived from MMNET's SearchHouseListener, allows players
 * to search for commands using partial strings. Eventually, I'd like to
 * expand this to allow searching in other modes (selectable via combo box),
 * like "Active Operations" and "Contested Worlds," w/ appropriate fields
 * for selection input.
 * 
 * @urgru 5.2.05
 * used code that urgru started to make cookie cut dialog boxes for command
 * and planets for commands requiring that input.
 *  
 * @Torren 5.6.05
 * 
 * Created to list all of the Commands for the SO's
 * 
 * @Torren 11.8.05
 * 
 * This allows SO's to select multiple OP flags on a planet at one time.
 * 
 * @Torren 09.02.06
 */

public class OpFlagSelectionDialog extends JDialog implements ActionListener {

	/**
     * 
     */
    private static final long serialVersionUID = -1024120117465498506L;
    //variables
	private final TreeSet<String> names;
    
	private JList<String> matchingCommandList;
	private JScrollPane scrollPane;//holds the JList
	private JTextField nameField;//input field
	private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");	
	private final String okayCommand = "Okay";
    private MWClient client = null;
	private Object[] commandName = null;
    
	//constructor
	public OpFlagSelectionDialog(MWClient mwclient, String boxText) {
		
		/*
		 * NOTE: variables are final in order to
		 * allow access by caretUpdate()
		 */
        
		//super, and variable saves
		super(new JFrame(),boxText,true);//dummy frame as owner
        
        client = mwclient;
        loadOpFlags();
        names = new TreeSet<String>();
        for (String key : mwclient.getData().getPlanetOpFlags().values())
            names.add(key);

        final String[] allCommandNames = names.toArray(new String[names.size()]);

		//construct the command name list
        matchingCommandList = new JList<String>(allCommandNames);
        matchingCommandList.setVisibleRowCount(10);
        matchingCommandList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

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
                            matchingCommandList.setListData(allCommandNames);
							return;
						}
						ArrayList<String> possibleCommands = new ArrayList<String>();
						text = text.toLowerCase();
						for (String curCommand : names ) {
							if (curCommand.toLowerCase().indexOf(text) != -1)
								possibleCommands.add(curCommand.substring(0,1)+curCommand.substring(1).toLowerCase());
						}
						matchingCommandList.setListData(possibleCommands.toArray(new String[possibleCommands.size()]));
						
						/*
						 * Try to select a command with a STARTING string which matched
						 * the seach index. If none is available, use the first command.
						 * 
						 * Hacky, but functional. @urgru 5.2.05
						 */
						boolean shouldContinue = true;
						int element = 0;
						for (String name : possibleCommands) {
							if (name.toLowerCase().startsWith(text)) {
								matchingCommandList.setSelectedIndex(element);
								shouldContinue = false;
								break;
							}
							element++;
						}
                       // MWLogger.errLog("7");

						//looped through without finding a starting match. set 0.
						if (shouldContinue) {
							matchingCommandList.setSelectedIndex(0);
						}
						
					}
				}.start();
			}
		});

		//put the list in a scroll pane
		scrollPane = new JScrollPane(matchingCommandList);
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
			 String selectedCommand = (String)matchingCommandList.getSelectedValue();
		        if (selectedCommand == null)
		        	selectedCommand = nameField.getText();
		        if (selectedCommand == null || selectedCommand.equals(""))
		        	return;
		        if (matchingCommandList.getModel().getSize() >= 1){
		        	// JList.getSelectedValues deprecated
                    //setCommandName(matchingCommandList.getSelectedValues());
                    setCommandName(matchingCommandList.getSelectedValuesList().toArray());
		        }
                else
                    JOptionPane.showMessageDialog(null,"Unknown Terrain");
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

	private void setCommandName(Object[] terrains){
	    this.commandName = terrains;
	}
	
	public Object[] getCommandName(){
	    return this.commandName;
	}
    
    private void loadOpFlags(){
        client.getData().getPlanetOpFlags().clear();
        
        client.sendChat(MWClient.CAMPAIGN_PREFIX + "c getserveropflags");
        
        int count = 0;
        while ( client.getData().getPlanetOpFlags().isEmpty() && count < 1000){
            try{
                Thread.sleep(125);
            }catch(Exception ex){}
            count++;
        }
    }
    
}
