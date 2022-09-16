/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

package admin.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpringLayout;

import client.MWClient;
import common.House;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;

public final class BannedAmmoDialog implements ActionListener{
	
	//store the client backlink for other things to use
	private MWClient mwclient = null; 
	private House house = null;
    
	private final static String okayCommand = "Add";
	private final static String cancelCommand = "Close";

	private String windowName = "Server Banned Ammo Editor";	
    private ArrayList<JCheckBox> cBoxArrayList = new ArrayList<JCheckBox>();
    
	//BUTTONS
	private final JButton okayButton = new JButton("Save");
	private final JButton cancelButton = new JButton("Close");	
	
	//STOCK DIALOUG AND PANE
	private JDialog dialog;
	private JOptionPane pane;
	
	JTabbedPane ConfigPane = new JTabbedPane();
	
	public BannedAmmoDialog(MWClient c, House house) {
		
		//save the client
		this.mwclient = c;
        this.house = house;
        
		//stored values.

		//Set the tooltips and actions for dialouge buttons
		okayButton.setActionCommand(okayCommand);
		cancelButton.setActionCommand(cancelCommand);
		
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		okayButton.setToolTipText("Save");
        cancelButton.setToolTipText("Exit without saving changes");
		
		
		//CREATE THE PANELS
		JPanel banPanel = new JPanel();//player name, etc
		
		/*
		 * Format the Reward Points panel. Spring layout.
		 */
		banPanel.setLayout(new BoxLayout(banPanel,BoxLayout.Y_AXIS));
		
		JPanel ammoPanel = new JPanel(new SpringLayout());
		
        loadBannedAmmo();
        
        TreeSet<String> munitions = new TreeSet<String>(mwclient.getData().getMunitionsByName().keySet());
        for ( String munitionName : munitions){
            //String munitionName = munitionNames.nextElement();
            JCheckBox cBox = new JCheckBox();
            cBox.setText(munitionName);
            cBox.setSelected(checkAmmoBan(munitionName));
            ammoPanel.add(cBox);
            cBoxArrayList.add(cBox);
        }

        SpringLayoutHelper.setupSpringGrid(ammoPanel,2);

        banPanel.add(ammoPanel);
        
        // Set the user's options
		Object[] options = { okayButton, cancelButton };
		
		// Create the pane containing the buttons
		pane = new JOptionPane(banPanel,JOptionPane.PLAIN_MESSAGE,JOptionPane.DEFAULT_OPTION, null, options, null);
		
        if ( house != null  )
            windowName = this.house.getName() +" Banned Ammo Dialog";
		// Create the main dialog and set the default button
		dialog = pane.createDialog(ammoPanel, windowName);
		dialog.getRootPane().setDefaultButton(cancelButton);


		//Show the dialog and get the user's input
		dialog.setModal(true);
		dialog.pack();
		dialog.setVisible(true);
		
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
        Hashtable<String,Long> munitionTypes = mwclient.getData().getMunitionsByName();
        
        if ( command.equals(okayCommand)){
            if ( house == null ){
                Hashtable<String,String> bannedAmmo = mwclient.getData().getServerBannedAmmo();
                for ( JCheckBox tempBox : cBoxArrayList ){
                    String ammo = Long.toString(munitionTypes.get(tempBox.getText()));
                    
                    //Check box has been selected and should be updated to the server
                    if ( tempBox.isSelected() && !bannedAmmo.containsKey(ammo) )
                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetserverammoban#"
                        + munitionTypes.get(tempBox.getText()));
                    //Checkbox has been unselected and should be updated to the server
                    else if ( !tempBox.isSelected() && bannedAmmo.containsKey(ammo) )
                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsetserverammoban#"
                                + munitionTypes.get(tempBox.getText()));
                }
            }
            else{
                Hashtable<String,String> bannedAmmo = house.getBannedAmmo();
                for ( JCheckBox tempBox : cBoxArrayList ){
                    String ammo = Long.toString(munitionTypes.get(tempBox.getText()));
                    
                    //Check box has been selected and should be updated to the server
                    if ( tempBox.isSelected() && !bannedAmmo.containsKey(ammo) )
                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethouseammoban#"
                                +house.getName()+"#"+ munitionTypes.get(tempBox.getText()));
                    //Checkbox has been unselected and should be updated to the server
                    else if ( !tempBox.isSelected() && bannedAmmo.containsKey(ammo) )
                        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c adminsethouseammoban#"
                                +house.getName()+"#"+ munitionTypes.get(tempBox.getText()));
                    
                }
            }
            
            dialog.dispose();
            return;
        }
        else if (command.equals(cancelCommand)) {
            dialog.dispose();
		}

	}
	
    public void loadBannedAmmo(){
            mwclient.loadBannedAmmo();
    }
    
    public boolean checkAmmoBan(String ammo){
        
        if ( house == null ){
            try{
                
                //I did this for some silly reason. and now I'm paying for it. 
                //But I don't want to change all the code to long,string hashes
                //Generics would make it easy but I'm lazy and it works. --Torren.
                String munition = Long.toString(mwclient.getData().getMunitionsByName().get(ammo));
                if (  mwclient.getData().getServerBannedAmmo().containsKey(munition) )
                    return true;
                return false;
            }catch(Exception ex){
                MWLogger.errLog("Unable to find ammo "+ammo);
                return false;
            }
        }
        try{
            String munition = Long.toString(mwclient.getData().getMunitionsByName().get(ammo));
            if (  house.getBannedAmmo().containsKey(munition) )
                return true;
            return false;
        }catch(Exception ex){
            MWLogger.errLog("Unable to find ammo "+ammo);
            return false;
        }

    }
	
}//end BannedAmmoDialog.java
