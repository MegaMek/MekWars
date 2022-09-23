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

package client.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.House;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.SpringLayoutHelper;

public final class TraitDialog implements ActionListener, KeyListener{
	
	//store the client backlink for other things to use
	private MWClient mwclient = null; 
	
	private final static String okayCommand = "Add";
	private final static String cancelCommand = "Close";
	private final static String removeCommand = "Remove";
	private final static String traitCommand = "Trait";
	private final static String factionCommand = "Faction";

	private String windowName = "Trait Editor";
	
	private final static String delimiter = "*";
	
	//BUTTONS
	private final JButton okayButton = new JButton("Add");
	private final JButton cancelButton = new JButton("Close");	
	private final JButton removeButton = new JButton("Remove");	
	
	//TEXT FIELDS
	//tab names
	private final JLabel factionLabel = new JLabel("faction:",SwingConstants.TRAILING);
	private final JLabel traitLabel = new JLabel("Trait:",SwingConstants.TRAILING);
	private final JLabel gunneryLaserLabel = new JLabel("G/L:",SwingConstants.TRAILING);
	private final JLabel gunneryBalisticLabel = new JLabel("G/B:",SwingConstants.TRAILING);
	private final JLabel gunneryMissileLabel = new JLabel("G/M:",SwingConstants.TRAILING);
	private final JLabel astechLabel = new JLabel("AT:",SwingConstants.TRAILING);
	private final JLabel tacticalGeniusLabel = new JLabel("TG:",SwingConstants.TRAILING);
	private final JLabel weaponSpecialistLabel = new JLabel("WS:",SwingConstants.TRAILING);
	private final JLabel meleeSpecialistLabel = new JLabel("MS:",SwingConstants.TRAILING);
	private final JLabel dodgeManeuverLabel = new JLabel("DM:",SwingConstants.TRAILING);
	private final JLabel ironManLabel = new JLabel("IM:",SwingConstants.TRAILING);
	private final JLabel maneuveringAceLabel = new JLabel("MA:",SwingConstants.TRAILING);
	private final JLabel NAGLabel = new JLabel("NAG:",SwingConstants.TRAILING);
	private final JLabel NAPLabel = new JLabel("NAP:",SwingConstants.TRAILING);
	private final JLabel painResistanceLabel = new JLabel("PR:",SwingConstants.TRAILING);
	private final JLabel survivalistSkillLabel = new JLabel("SV:",SwingConstants.TRAILING);
	private final JLabel enhancedInterfaceLabel = new JLabel("EI:",SwingConstants.TRAILING);
    private final JLabel quickStudyLabel = new JLabel("QS:",SwingConstants.TRAILING);
    private final JLabel giftedLabel = new JLabel("GT:",SwingConstants.TRAILING);
    private final JLabel medtechLabel = new JLabel("MT:",SwingConstants.TRAILING);

	
	private final JTextField gunneryLaserText = new JTextField(3);
	private final JTextField gunneryBalisticText = new JTextField(3);
	private final JTextField gunneryMissileText = new JTextField(3);
	private final JTextField astechText = new JTextField(3);
	private final JTextField tacticalGeniusText = new JTextField(3);
	private final JTextField weaponSpecialistText = new JTextField(3);
	private final JTextField meleeSpecialistText = new JTextField(3);
	private final JTextField dodgeManeuverText = new JTextField(3);
	private final JTextField ironManText = new JTextField(3);
	private final JTextField maneuveringAceText = new JTextField(3);
	private final JTextField NAGText = new JTextField(3);
	private final JTextField NAPText = new JTextField(3);
	private final JTextField painResistanceText = new JTextField(3);
	private final JTextField survivalistSkillText = new JTextField(3);
	private final JTextField enhancedInterfaceText = new JTextField(3);
    private final JTextField quickStudyText = new JTextField(3);
    private final JTextField giftedText = new JTextField(3);
    private final JTextField medtechText = new JTextField(3);

	private JComboBox<String> factionComboBox = new JComboBox<String>();
	private JComboBox<String> traitComboBox = new JComboBox<String>();
	
	//STOCK DIALOUG AND PANE
	private JDialog dialog;
	private JOptionPane pane;
	
	JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);
	
	public TraitDialog(MWClient c,boolean player) {
		
		//save the client
		this.mwclient = c;
		
		//COMBO BOXES
		TreeSet<String> names = new TreeSet<String>();
		names.add("Common"); //start with the common faction
		for (Iterator<House> factions = mwclient.getData().getAllHouses().iterator(); factions.hasNext();)
			names.add( factions.next().getName());
		
		factionComboBox = new JComboBox<String>(names.toArray(new String[names.size()]));
		traitComboBox.setEditable(!player);
		
		//stored values.

		//Set the tooltips and actions for dialouge buttons
		okayButton.setActionCommand(okayCommand);
		cancelButton.setActionCommand(cancelCommand);
		factionComboBox.setActionCommand(factionCommand);
		traitComboBox.setActionCommand(traitCommand);
		
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		removeButton.addActionListener(this);
		okayButton.setToolTipText("Save Trait");
		if ( player ){
		    cancelButton.setToolTipText("Exit");
		    windowName = "Trait Viewer";
		}
		else
		    cancelButton.setToolTipText("Exit without saving changes");
		removeButton.setToolTipText("Delete Trait");
		traitComboBox.addActionListener(this);
		factionComboBox.addActionListener(this);
		
		okayButton.setVisible(!player);
		removeButton.setVisible(!player);
		
		//CREATE THE PANELS
		JPanel traitsPanel = new JPanel();//player name, etc
		
		/*
		 * Format the Reward Points panel. Spring layout.
		 */
		traitsPanel.setLayout(new BoxLayout(traitsPanel,BoxLayout.Y_AXIS));
		
		JPanel skillPanel = new JPanel(new SpringLayout());
		
		JPanel comboPanel = new JPanel(new SpringLayout());
		
		comboPanel.add(factionLabel);
		factionComboBox.setToolTipText("Select a faction");
		comboPanel.add(factionComboBox);
		
		comboPanel.add(traitLabel);
        if ( player )
            traitComboBox.setToolTipText("Select a trait.");
        else
            traitComboBox.setToolTipText("Select a trait or enter a new one.");
		comboPanel.add(traitComboBox);
		
        skillPanel.add(astechLabel);
        astechText.setToolTipText("<html>Astech<br>Modifies the chance for a pilot to receive this skill</html>");
        astechText.setEditable(!player);
        skillPanel.add(astechText);
        
		skillPanel.add(dodgeManeuverLabel);
		dodgeManeuverText.setToolTipText("<html>Dodge Maneuver<br>Modifies the chance for a pilot to receive this skill</html>");
        dodgeManeuverText.setEditable(!player);
		skillPanel.add(dodgeManeuverText);

		skillPanel.add(enhancedInterfaceLabel);
		enhancedInterfaceText.setToolTipText("<html>Enhanced Interface<br>Modifies the chance for a pilot to receive this skill</html>");
        enhancedInterfaceText.setEditable(!player);
		skillPanel.add(enhancedInterfaceText);

        skillPanel.add(giftedLabel);
        giftedText.setToolTipText("<html>Gifted<br>Modifies the chance for a pilot to receive this skill</html>");
        giftedText.setEditable(!player);
        skillPanel.add(giftedText);

        skillPanel.add(gunneryLaserLabel);
        gunneryLaserText.setToolTipText("<html>Gunnery Laser<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryLaserText.setEditable(!player);
        skillPanel.add(gunneryLaserText);

        skillPanel.add(gunneryBalisticLabel);
        gunneryBalisticText.setToolTipText("<html>Gunnery Ballistic<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryBalisticText.setEditable(!player);
        skillPanel.add(gunneryBalisticText);
        
        skillPanel.add(gunneryMissileLabel);
        gunneryMissileText.setToolTipText("<html>Gunnery Missile<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryMissileText.setEditable(!player);
        skillPanel.add(gunneryMissileText);

        skillPanel.add(ironManLabel);
        ironManText.setToolTipText("<html>Iron Man<br>Modifies the chance for a pilot to receive this skill</html>");
        ironManText.setEditable(!player);
        skillPanel.add(ironManText);

        skillPanel.add(maneuveringAceLabel);
        maneuveringAceText.setToolTipText("<html>Maneuvering Ace<br>Modifies the chance for a pilot to receive this skill</html>");
        maneuveringAceText.setEditable(!player);
        skillPanel.add(maneuveringAceText);

        skillPanel.add(medtechLabel);
        medtechText.setToolTipText("<html>Medtech<br>Modifies the chance for a pilot to receive this skill</html>");
        medtechText.setEditable(!player);
        skillPanel.add(medtechText);

        skillPanel.add(meleeSpecialistLabel);
        meleeSpecialistText.setToolTipText("<html>Melee Specialist<br>Modifies the chance for a pilot to receive this skill</html>");
        meleeSpecialistText.setEditable(!player);
        skillPanel.add(meleeSpecialistText);

        skillPanel.add(NAGLabel);
        NAGText.setToolTipText("<html>Natural Aptitiude: Gunnery<br>Modifies the chance for a pilot to receive this skill</html>");
        NAGText.setEditable(!player);
        skillPanel.add(NAGText);

        skillPanel.add(NAPLabel);
        NAPText.setToolTipText("<html>Natural Aptitude: Piloting<br>Modifies the chance for a pilot to receive this skill</html>");
        NAPText.setEditable(!player);
        skillPanel.add(NAPText);

        skillPanel.add(painResistanceLabel);
        painResistanceText.setToolTipText("<html>Pain Resistance<br>Modifies the chance for a pilot to receive this skill</html>");
        painResistanceText.setEditable(!player);
        skillPanel.add(painResistanceText);

        skillPanel.add(quickStudyLabel);
        quickStudyText.setToolTipText("<html>Quick Study</html>");
        quickStudyText.setEditable(!player);
        skillPanel.add(quickStudyText);

        skillPanel.add(survivalistSkillLabel);
        survivalistSkillText.setToolTipText("<html>Survivalist</html>");
        survivalistSkillText.setEditable(!player);
        skillPanel.add(survivalistSkillText);

        skillPanel.add(tacticalGeniusLabel);
        tacticalGeniusText.setToolTipText("<html>Tactical Genius</html>");
        tacticalGeniusText.setEditable(!player);
        skillPanel.add(tacticalGeniusText);

        skillPanel.add(weaponSpecialistLabel);
        weaponSpecialistText.setToolTipText("<html>Weapon Specialist</html>");
        weaponSpecialistText.setEditable(!player);
        skillPanel.add(weaponSpecialistText);

        //run the spring layout
		SpringLayoutHelper.setupSpringGrid(comboPanel,2);
		SpringLayoutHelper.setupSpringGrid(skillPanel,8);
		
		traitsPanel.add(comboPanel);
		traitsPanel.add(skillPanel);
		
		JPanel mainPanel = new JPanel();
		
		// Set the user's options
		Object[] options = { okayButton, removeButton, cancelButton };
		
		// Create the pane containing the buttons
		pane = new JOptionPane(traitsPanel,JOptionPane.PLAIN_MESSAGE,JOptionPane.DEFAULT_OPTION, null, options, null);
		
		// Create the main dialog and set the default button
		dialog = pane.createDialog(mainPanel, windowName);
		dialog.getRootPane().setDefaultButton(cancelButton);


		loadAllFiles();
		
		factionComboBox.setSelectedIndex(0);
		dialog.setLocationRelativeTo(mwclient.getMainFrame());
		//Show the dialog and get the user's input
		dialog.setModal(true);
		dialog.pack();
		dialog.setVisible(true);
		
		if (pane.getValue() == okayButton) {
			
		}
		else 
			dialog.dispose();
	}
	
	public void keyTyped(KeyEvent e){
	}
	
	public void keyReleased(KeyEvent e){
	
	    String faction = (String)factionComboBox.getSelectedItem();
	    String trait  =  ((String)traitComboBox.getSelectedItem()).trim();
	    
	    if ( e.getComponent().equals(factionComboBox) ){
	        loadFactionTraits(faction);
	    }
	    else{
	        populateTraits(faction,trait);
	    }
	}

	public void keyPressed(KeyEvent e){
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if (command.equals(okayCommand)) {
		    String faction = (String)factionComboBox.getSelectedItem();
		    String trait = ((String)traitComboBox.getSelectedItem()).trim();
		    if ( trait.trim().length() < 1){
		       JOptionPane.showMessageDialog(null,"You did not enter a trait name!");
		       return;
		    }
		    
	    	String result = getResults(faction,trait);
	    	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c addtrait#"+result);
		    loadAllFiles();
		    loadFactionTraits(faction);
		} else if (command.equals(cancelCommand)) {
			pane.setValue(cancelButton);
			dialog.dispose();
		} else if ( command.equals(removeCommand)){
		    String faction = (String)factionComboBox.getSelectedItem();
		    String trait = ((String)traitComboBox.getSelectedItem()).trim();
		    
		    if ( trait.length() < 1){
		        JOptionPane.showMessageDialog(null,"You have to select a trait before you can remove it!");
		        return;
		    }
		    int choice = JOptionPane.showConfirmDialog(null,"Are you sure you want to remove this trait?","Remove it?",JOptionPane.YES_NO_OPTION);
		    if ( choice == JOptionPane.OK_OPTION ){
		    	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c removetrait#"+faction+"#"+trait+"#CONFIRM");
			    loadAllFiles();
			    loadFactionTraits(faction);
		  }
		} else if (command.equals(factionCommand)){
		    String selection = (String)factionComboBox.getSelectedItem();
		    loadFactionTraits(selection);
		}
		else if ( command.equals(traitCommand)){
		    String faction = (String)factionComboBox.getSelectedItem();
		    if ( traitComboBox.getSelectedItem() == null )
		        return;
			String trait = ((String)traitComboBox.getSelectedItem()).trim();
	        populateTraits(faction,trait);
		}
	}
	
	private void loadFactionTraits(String faction){
	    File traitFile = new File(mwclient.getCacheDir() + "/"+faction.toLowerCase()+"traitnames.txt");
	    TreeSet<String> names = new TreeSet<String>();
	    if ( traitComboBox.getItemCount() > 0)
	        traitComboBox.removeAllItems();
	    BufferedReader dis = null;
	    try{	
	        FileInputStream fis = new FileInputStream(traitFile);
	        dis = new BufferedReader(new InputStreamReader(fis));
	        while (dis.ready()){
	            StringTokenizer traitName = new StringTokenizer(dis.readLine(),delimiter);
	            names.add(traitName.nextToken());
	        }
	        //names.add(" ");
	        String[] tempArray = names.toArray(new String[names.size()]);
	        for ( int i =0; i < tempArray.length; i++)
	            traitComboBox.addItem(tempArray[i]);
	    }catch (Exception ex){
	        MWLogger.errLog("Unable to load faction "+faction);
	        MWLogger.errLog(ex);
	    } finally {
	    	try {
				dis.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
	    }
	    if ( traitComboBox.getItemCount() > 0 )
	        traitComboBox.setSelectedIndex(0);
	    traitComboBox.revalidate();
	}
	
	private void loadAllFiles(){
		mwclient.loadServerTraitFiles();
	}
	
	private void populateTraits(String faction, String trait){
	    File traitFile = new File(mwclient.getCacheDir() + "/"+faction.toLowerCase()+"traitnames.txt");

		gunneryLaserText.setText("0");
		gunneryBalisticText.setText("0");
		gunneryMissileText.setText("0");
		astechText.setText("0");
		tacticalGeniusText.setText("0");
		weaponSpecialistText.setText("0");
		meleeSpecialistText.setText("0");
		dodgeManeuverText.setText("0");
		ironManText.setText("0");
		maneuveringAceText.setText("0");
		NAGText.setText("0");
		NAPText.setText("0");
		painResistanceText.setText("0");
		survivalistSkillText.setText("0");
        enhancedInterfaceText.setText("0");
        giftedText.setText("0");
        quickStudyText.setText("0");
        medtechText.setText("0");
        BufferedReader dis = null;
		try{
	        FileInputStream fis = new FileInputStream(traitFile);
	        dis = new BufferedReader(new InputStreamReader(fis));
	        while (dis.ready()){
	            StringTokenizer traitNames = new StringTokenizer(dis.readLine(),delimiter);
	            String traitName = traitNames.nextToken();
	            if( traitName.equalsIgnoreCase(trait) ){
	                while ( traitNames.hasMoreTokens() ){
	                    int traitID = Integer.parseInt(traitNames.nextToken());
	                    String traitMod = traitNames.nextToken();
	                    
	                    if ( traitID == PilotSkill.GunneryBallisticSkillID )
	                        gunneryBalisticText.setText(traitMod);
	                    else if ( traitID == PilotSkill.GunneryLaserSkillID )
	                        gunneryLaserText.setText(traitMod);
	                    else if ( traitID == PilotSkill.GunneryMissileSkillID )
	                        gunneryMissileText.setText(traitMod);
	                    else if ( traitID == PilotSkill.AstechSkillID)
	                        astechText.setText(traitMod);
	                    else if ( traitID == PilotSkill.DodgeManeuverSkillID )
	                        dodgeManeuverText.setText(traitMod);
	                    else if ( traitID == PilotSkill.IronManSkillID )
	                        ironManText.setText(traitMod);
	                    else if ( traitID == PilotSkill.ManeuveringAceSkillID )
	                        maneuveringAceText.setText(traitMod);
	                    else if ( traitID == PilotSkill.MeleeSpecialistSkillID )
	                        meleeSpecialistText.setText(traitMod);
	                    else if ( traitID == PilotSkill.NaturalAptitudeGunnerySkillID )
	                        NAGText.setText(traitMod);
	                    else if ( traitID == PilotSkill.NaturalAptitudePilotingSkillID)
	                        NAPText.setText(traitMod);
	                    else if ( traitID == PilotSkill.PainResistanceSkillID )
	                        painResistanceText.setText(traitMod);
	                    else if ( traitID == PilotSkill.SurvivalistSkillID )
	                        survivalistSkillText.setText(traitMod);
	                    else if ( traitID == PilotSkill.TacticalGeniusSkillID )
	                        tacticalGeniusText.setText(traitMod);
	                    else if ( traitID == PilotSkill.WeaponSpecialistSkillID)
	                        weaponSpecialistText.setText(traitMod);
                        else if ( traitID == PilotSkill.EnhancedInterfaceID)
                            enhancedInterfaceText.setText(traitMod);
                        else if ( traitID == PilotSkill.GiftedID)
                            giftedText.setText(traitMod);
                        else if ( traitID == PilotSkill.QuickStudyID)
                            quickStudyText.setText(traitMod);
                        else if ( traitID == PilotSkill.MedTechID)
                            medtechText.setText(traitMod);
	                }
	            }
	                
	        }
	    }catch (Exception ex){
	        MWLogger.errLog("populate Traits error");
	        MWLogger.errLog(ex);
        } finally {
        	try {
				dis.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
        }
	}
	
	public String getResults(String faction, String trait){
	    String result = faction+"#"+trait+"#";

	    if ( Integer.parseInt(gunneryBalisticText.getText()) != 0 ){
            result+= PilotSkill.GunneryBallisticSkillID;
            result += delimiter;
            result += gunneryBalisticText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(gunneryLaserText.getText()) != 0 ){
            result+= PilotSkill.GunneryLaserSkillID;
            result += delimiter;
            result += gunneryLaserText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(gunneryMissileText.getText()) != 0 ){
            result+= PilotSkill.GunneryMissileSkillID;
            result += delimiter;
            result += gunneryMissileText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(astechText.getText()) != 0){
            result+= PilotSkill.AstechSkillID;
            result += delimiter;
            result += astechText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(dodgeManeuverText.getText()) != 0){
            result+= PilotSkill.DodgeManeuverSkillID;
            result += delimiter;
            result += dodgeManeuverText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(ironManText.getText()) != 0 ){
            result+= PilotSkill.IronManSkillID;
            result += delimiter;
            result += ironManText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(maneuveringAceText.getText()) != 0 ){
            result+= PilotSkill.ManeuveringAceSkillID;
            result += delimiter;
            result += maneuveringAceText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(meleeSpecialistText.getText()) != 0 ){
            result+= PilotSkill.MeleeSpecialistSkillID;
            result += delimiter;
            result += meleeSpecialistText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(NAGText.getText()) != 0 ){
            result+= PilotSkill.NaturalAptitudeGunnerySkillID;
            result += delimiter;
            result += NAGText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(NAPText.getText()) != 0){
            result+= PilotSkill.NaturalAptitudePilotingSkillID;
            result += delimiter;
            result += NAPText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(painResistanceText.getText()) != 0 ){
            result+= PilotSkill.PainResistanceSkillID;
            result += delimiter;
            result += painResistanceText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(survivalistSkillText.getText()) != 0 ){
            result+= PilotSkill.SurvivalistSkillID;
            result += delimiter;
            result += survivalistSkillText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(tacticalGeniusText.getText()) != 0 ){
            result+= PilotSkill.TacticalGeniusSkillID;
            result += delimiter;
            result += tacticalGeniusText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(weaponSpecialistText.getText()) != 0){
            result+= PilotSkill.WeaponSpecialistSkillID;
            result += delimiter;
            result += weaponSpecialistText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(enhancedInterfaceText.getText()) != 0){
            result+= PilotSkill.EnhancedInterfaceID;
            result += delimiter;
            result += enhancedInterfaceText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(giftedText.getText()) != 0){
            result+= PilotSkill.GiftedID;
            result += delimiter;
            result += giftedText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(quickStudyText.getText()) != 0){
            result+= PilotSkill.QuickStudyID;
            result += delimiter;
            result += quickStudyText.getText();
            result += delimiter;
        }
        if ( Integer.parseInt(medtechText.getText()) != 0){
            result+= PilotSkill.MedTechID;
            result += delimiter;
            result += medtechText.getText();
            result += delimiter;
        }


        result += "#CONFIRM";
        //MMClient.mwClientLog.clientErrLog("Result: "+result);
	    return result;
	}
}//end TraitDialog.java
