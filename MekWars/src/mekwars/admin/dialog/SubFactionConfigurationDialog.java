/*
 * MekWars - Copyright (C) 2007 
 * 
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

/**
 * @author jtighe
 * 
 */

package admin.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import client.campaign.CUnit;
import common.House;
import common.SubFaction;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;

public final class SubFactionConfigurationDialog implements ActionListener {
	
	private final static String okayCommand = "okay";
	private final static String cancelCommand = "cancel";
	private String windowName = "";
	
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox  BaseCheckBox = new JCheckBox();
    
    private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");
	
	private JDialog dialog;
	private JOptionPane pane;
	
	private String houseName = "";
	
	private SubFaction subFactionConfig = null;
	private House faction = null;

	private Hashtable<String, String> configChanges = new Hashtable<String, String>();
	
	JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);
	
    MWClient mwclient = null;
	/**
	 * @author jtighe
	 * 
	 * Opens the server config page in the client.
	 * @param client
	 */
    
	public SubFactionConfigurationDialog(MWClient mwclient, String houseName, String subFactionName) {
		
        this.mwclient = mwclient;
        this.houseName = houseName;
        this.windowName = "MekWars SubFaction Configuration";
        this.faction = mwclient.getData().getHouseByName(houseName);
        
        if ( faction == null )
        	return;
        
        if ( faction.getSubFactionList().containsKey(subFactionName) ) 
        	this.subFactionConfig = faction.getSubFactionList().get(subFactionName);
        else{
        	this.subFactionConfig = new SubFaction(subFactionName,"0");
        	this.subFactionConfig.setConfig("MinELO", "0");
        	this.subFactionConfig.setConfig("MinExp", "0");
        	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c CreateSubFaction#"+this.subFactionConfig.getConfig("Name")+"#0#"+this.houseName);
        }
        
		//TAB PANELS (these are added to the root pane as tabs)
		JPanel mainPanel = new JPanel();
		
		/* 
		 * REPOD PANEL CONSTRUCTION
		 * 
		 * Repod contols. Costs, factory usage, table options, etc.
		 * 
		 * Use nested layouts. A Box containing a Flow and 3 Springs.
		 */
		JPanel mainBoxPanel = new JPanel();
		JPanel mainCBoxGridPanel = new JPanel(new SpringLayout());
		JPanel mainTextBoxSpring = new JPanel(new SpringLayout());
		mainBoxPanel.setLayout(new BoxLayout(mainBoxPanel, BoxLayout.Y_AXIS));
		mainBoxPanel.add(mainCBoxGridPanel);
		mainBoxPanel.add(mainTextBoxSpring);
		
		//set up the flow panel
		
		//and then the various springs. MU first.
        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Sub faction name.");
        baseTextField.setName("Name");
        mainTextBoxSpring.add(baseTextField);
		
        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Access Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Sub faciton access level<br>This is used to determine what ops can be accessed</html>");
        baseTextField.setName("AccessLevel");
        mainTextBoxSpring.add(baseTextField);
		
        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Min Elo:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Min ELO needed to join this subfaction");
        baseTextField.setName("MinELO");
        mainTextBoxSpring.add(baseTextField);
		
        baseTextField = new JTextField(5);
        mainTextBoxSpring.add(new JLabel("Min Exp:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Min Exp required to join this subfaciton");
        baseTextField.setName("MinExp");
        mainTextBoxSpring.add(baseTextField);
		
		for (int type = 0; type < CUnit.MAXBUILD; type++) {
			for (int weight = 0; weight <= CUnit.ASSAULT; weight++) {
				BaseCheckBox = new JCheckBox("Can buy new "+CUnit.getWeightClassDesc(weight)+" "+CUnit.getTypeClassDesc(type));
				BaseCheckBox.setToolTipText("<html>Check to allow subfaction memebers to buy new<br>"+CUnit.getWeightClassDesc(weight)+" "+CUnit.getTypeClassDesc(type)+"</html>");
				BaseCheckBox.setName("CanBuyNew"+CUnit.getWeightClassDesc(weight)+CUnit.getTypeClassDesc(type));
				mainCBoxGridPanel.add(BaseCheckBox);
			}
		}
		
		for (int type = 0; type < CUnit.MAXBUILD; type++) {
			for (int weight = 0; weight <= CUnit.ASSAULT; weight++) {
				BaseCheckBox = new JCheckBox("Can buy used "+CUnit.getWeightClassDesc(weight)+" "+CUnit.getTypeClassDesc(type));
				BaseCheckBox.setToolTipText("<html>Check to allow subfaction memebers to buy used<br>"+CUnit.getWeightClassDesc(weight)+" "+CUnit.getTypeClassDesc(type)+"</html>");
				BaseCheckBox.setName("CanBuyUsed"+CUnit.getWeightClassDesc(weight)+CUnit.getTypeClassDesc(type));
				mainCBoxGridPanel.add(BaseCheckBox);
			}
		}
		
        //finalize the layout.
        SpringLayoutHelper.setupSpringGrid(mainTextBoxSpring,4);
        SpringLayoutHelper.setupSpringGrid(mainCBoxGridPanel,4);
        mainBoxPanel.add(mainTextBoxSpring);
        mainBoxPanel.add(mainCBoxGridPanel);
        mainPanel.add(mainBoxPanel);
		
		// Set the actions to generate
		okayButton.setActionCommand(okayCommand);
		cancelButton.setActionCommand(cancelCommand);
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		/*
		 * NEW OPTIONS - need to be sorted into proper menus.
		 */
		
		// Set tool tips (balloon help)	
		okayButton.setToolTipText("Save Options");
		cancelButton.setToolTipText("Exit without saving options");
	
		ConfigPane.addTab("Configs",null,mainBoxPanel,"Configs");
		
		//Create the panel that will hold the entire UI
		JPanel mainConfigPanel = new JPanel();
		
		// Set the user's options
		Object[] options = { okayButton, cancelButton };
		
		// Create the pane containing the buttons
		pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE,
				JOptionPane.DEFAULT_OPTION, null, options, null);
		
		// Create the main dialog and set the default button
		dialog = pane.createDialog(mainConfigPanel, windowName);
		dialog.getRootPane().setDefaultButton(cancelButton);
		
		
        for ( int pos = ConfigPane.getComponentCount()-1; pos >= 0; pos-- ){
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);
            
        }


        //Show the dialog and get the user's input
        dialog.setLocationRelativeTo(mwclient.getMainFrame());
		dialog.setModal(true);
		dialog.pack();
		dialog.setVisible(true);
		
		if (pane.getValue() == okayButton)
		{
            
            for ( int pos = ConfigPane.getComponentCount()-1; pos >= 0; pos-- ){
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel);
            }
            
            if ( configChanges.size() > 0){
		        StringBuffer configPairs = new StringBuffer();
		        
		        for (String key : configChanges.keySet() ){
		        	configPairs.append(key);
		        	configPairs.append("#");
		        	configPairs.append(configChanges.get(key));
		        	configPairs.append("#");
		        }
            
            	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c SetSubFactionConfig#"+this.subFactionConfig.getConfig("Name")+"#"+houseName+"#"+configPairs.toString());
            }
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX+ "c adminsave");
			mwclient.refreshData();
			
		}
		else 
			dialog.dispose();
	}

    /**
     * This Method tunnels through all of the panels to find the textfields
     * and checkboxes. Once it find one it grabs the Name() param of the object
     * and uses that to find out what the setting should be from the
     * mwclient.getserverConfigs() method.
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel){
        String key = null;
        
        for ( int fieldPos = panel.getComponentCount()-1; fieldPos >= 0; fieldPos--){
            
            Object field = panel.getComponent(fieldPos);
            
            if ( field instanceof JPanel)
                findAndPopulateTextAndCheckBoxes((JPanel)field);
            else if ( field instanceof JTextField){
                JTextField textBox = (JTextField)field;
                
                key = textBox.getName();
                if ( key == null )
                    continue;
                
                textBox.setMaximumSize(new Dimension(100,10));
                textBox.setText(this.subFactionConfig.getConfig(key));
            }else if ( field instanceof JCheckBox){
                JCheckBox checkBox = (JCheckBox)field;
                
                key = checkBox.getName();
                if ( key == null ){
                    MWLogger.errLog("Null Checkbox: "+checkBox.getToolTipText());
                    continue;
                }
                checkBox.setSelected(Boolean.parseBoolean(this.subFactionConfig.getConfig(key)));
                
            }else if ( field instanceof JRadioButton){
                JRadioButton radioButton = (JRadioButton)field;
                
                key = radioButton.getName();
                if ( key == null ){
                    MWLogger.errLog("Null RadioButton: "+radioButton.getToolTipText());
                    continue;
                }
                radioButton.setSelected(Boolean.parseBoolean(this.subFactionConfig.getConfig(key)));
                
            }//else continue
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI
     * to find any changed text fields or checkboxes. Then it will send the 
     * new configs to the server.
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel){
        String key = null;
        String value = null;
        for ( int fieldPos = panel.getComponentCount()-1; fieldPos >= 0; fieldPos--){
            
            Object field = panel.getComponent(fieldPos);

            //found another JPanel keep digging!
            if ( field instanceof JPanel )
                findAndSaveConfigs((JPanel)field);
            else if ( field instanceof JTextField){
                JTextField textBox = (JTextField)field;
                
                value = textBox.getText();
                key = textBox.getName();
                
                if ( key == null || value == null )
                    continue;
                
                //don't need to save this the system does it on its own
                // --Torren.
                if (key.equals("LastAutomatedBackup") )
                    continue;
                    
                //reduce bandwidth only send things that have changed.
                if ( !this.subFactionConfig.getConfig(key).equalsIgnoreCase(value) )
                    configChanges.put(key,value); 
            } else if ( field instanceof JCheckBox){
                JCheckBox checkBox = (JCheckBox)field;
                
                value = Boolean.toString(checkBox.isSelected());
                key = checkBox.getName();
                
                if ( key == null || value == null )
                    continue;
                //reduce bandwidth only send things that have changed.
                if ( !this.subFactionConfig.getConfig(key).equalsIgnoreCase(value) )
                    configChanges.put(key,value); 
            }else if ( field instanceof JRadioButton){
                JRadioButton radioButton = (JRadioButton)field;
                
                value = Boolean.toString(radioButton.isSelected());
                key = radioButton.getName();
                
                if ( key == null || value == null )
                    continue;
                //reduce bandwidth only send things that have changed.
                if ( !this.subFactionConfig.getConfig(key).equalsIgnoreCase(value) )
                    configChanges.put(key,value); 
            }//else continue
        }

    }
    
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals(okayCommand)) {
			pane.setValue(okayButton);
			dialog.dispose();
		} else if (command.equals(cancelCommand)) {
			pane.setValue(cancelButton);
			dialog.dispose();
		}
	}
}