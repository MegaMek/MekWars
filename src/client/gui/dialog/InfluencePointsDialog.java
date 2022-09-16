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

//@ Salient , copy of RewardPointsDialog

package client.gui.dialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Iterator;
//import java.util.StringTokenizer;
import java.util.TreeSet;
//import java.util.Vector;

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
import common.Planet;
//import common.Unit;
import common.UnitFactory;
import common.util.SpringLayoutHelper;
//import common.util.UnitUtils;


public final class InfluencePointsDialog implements ActionListener, KeyListener{

	//store the client backlink for other things to use
	private MWClient mwclient = null;

	private final static String okayCommand = "Okay";
	private final static String cancelCommand = "Cancel";
//	private final static String unitCommand = "Units";
//	private final static String weightCommand = "Weight";
	private final static String rewardCommand = "Reward";
//	private final static String factionCommand = "House";
//	private final static String repodCommand = "Repod";
	private final static String refreshCommand = "Refresh";
//    private final static String techComboCommand = "TechCombo";
//    private final static String repairCommand = "Repair";

	//private final static String amountCommand = "Amount";

	private static String windowName;


	//BUTTONS
	private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");

	//TEXT FIELDS
	//tab names
	private final JLabel costLabel= new JLabel();
	private final JLabel rewardLabel = new JLabel("Choose Action:",SwingConstants.TRAILING);
//	private final JLabel factionLabel = new JLabel("House Table:",SwingConstants.TRAILING);
//	private final JLabel unitLabel = new JLabel("Unit Type:",SwingConstants.TRAILING);
//	private final JLabel weightLabel = new JLabel("Weight Class:",SwingConstants.TRAILING);
//	private final JLabel repodLabel = new JLabel("Repod Selection:",SwingConstants.TRAILING);
//	private final JLabel pUnitsLabel = new JLabel("Unit:",SwingConstants.TRAILING);
	private final JLabel refreshLabel = new JLabel("Refresh:",SwingConstants.TRAILING);
//    private final JLabel techComboLabel = new JLabel("Tech Type:",SwingConstants.TRAILING);
//    private final JLabel repairLabel = new JLabel("Repair:",SwingConstants.TRAILING);

//	private JComboBox unitComboBox = new JComboBox();
//	private final String[] weightChoices = {"Light", "Medium", "Heavy", "Assault"};
//	private final JComboBox weightComboBox = new JComboBox(weightChoices);
	private JComboBox rewardsComboBox = new JComboBox();
//	private JComboBox factionComboBox = new JComboBox();
//	private JComboBox repodComboBox = new JComboBox();
//	private JComboBox pUnitsComboBox = new JComboBox();
	private JComboBox refreshComboBox = new JComboBox();
//    private JComboBox repairComboBox = new JComboBox();
//    private final String[] techChoices = {"Green", "Reg", "Vet", "Elite"};
//    private final JComboBox techComboBox = new JComboBox(techChoices);

	private final JTextField amountText = new JTextField(5);
	private JLabel amountLabel;
	int cost = 0;

	//STOCK DIALOUG AND PANE
	private JDialog dialog;
	private JOptionPane pane;
//	private int fluToRepod;

	JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

	public InfluencePointsDialog(MWClient c) {

		//save the client
		this.mwclient = c;
		windowName = mwclient.getserverConfigs("FluLongName");
		amountLabel = new JLabel(mwclient.getserverConfigs("FluShortName") + " to use:",SwingConstants.TRAILING);
//		fluToRepod = Integer.parseInt(mwclient.getserverConfigs("FluToRepod")); 
		//House house = c.getPlayer().getMyHouse();

		//COMBO BOXES
		TreeSet<String> names = new TreeSet<String>();
//		TreeSet<String> factionNames = new TreeSet<String>();
//		for (Iterator<House> factions = mwclient.getData().getAllHouses().iterator(); factions.hasNext();)
//			factionNames.add( factions.next().getName());

		//check for the use of rare and add if used
//		if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowRareUnitsForRewards")))
//		    factionNames.add("Rare");
//
//		factionComboBox = new JComboBox<Object>(factionNames.toArray());
//
//		
//		if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowTechsForRewards")))
//		    names.add("Techs");
//		if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowInfluenceForRewards")))
//		    names.add(mwclient.getserverConfigs("FluLongName"));
//		@Author Salient (mwosux@gmail.com) , Add RP for CBills

		if (Integer.parseInt(mwclient.getserverConfigs("Cbills_Per_Flu")) > 0)
			names.add(mwclient.getserverConfigs("MoneyLongName"));

//		if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowUnitsForRewards"))){
//          	names.add(unitCommand);
//
//          	//private final String[] unitChoices = {"Mek", "Vehicle", "Infantry", "ProtoMek","BattleArmor" };
//
//          Vector<String> unitList = new Vector<String>(5,1);
//
//          unitList.add("Mek");
//          if ( Boolean.parseBoolean(mwclient.getserverConfigs("UseVehicle")) )
//              unitList.add("Vehicle");
//          if ( Boolean.parseBoolean(mwclient.getserverConfigs("UseInfantry")) )
//              unitList.add("Infantry");
//          if ( Boolean.parseBoolean(mwclient.getserverConfigs("UseProtoMek")))
//              unitList.add("ProtoMek");
//          if ( Boolean.parseBoolean(mwclient.getserverConfigs("UseBattleArmor")))
//              unitList.add("BattleArmor");
//          if ( Boolean.parseBoolean(mwclient.getserverConfigs("UseAero")))
//              unitList.add("Aero");
//
//          unitComboBox = new JComboBox<Object>(unitList.toArray());
//       }

//		if (fluToRepod > 0) 
//		{
//		    names.add(repodCommand);
//		    TreeSet<String> repodOptions = new TreeSet<String>();
//
//		    if (Boolean.parseBoolean(mwclient.getserverConfigs("RandomRepodOnly")))
//		        repodOptions.add("Random");
//		    else
//		    {
//		        if (Boolean.parseBoolean(mwclient.getserverConfigs("RandomRepodAllowed")))
//		            repodOptions.add("Random");
//		        repodOptions.add("Select");
//		    }
//
//		    repodComboBox = new JComboBox<Object>(repodOptions.toArray());
//		    repodOptions.clear();
//		    Iterator<CUnit> units = c.getPlayer().getHangar().iterator();
//		    while (units.hasNext())
//		    {
//		        CUnit unit = units.next();
//		        if ( !unit.isOmni() )
//		            continue;
//		        repodOptions.add("#"+unit.getId()+" "+unit.getModelName());
//		    }
//		    pUnitsComboBox = new JComboBox<Object>(repodOptions.toArray());
//		}

//        if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowRepairsForRewards"))){
//            names.add(repairCommand);
//            TreeSet<String> damagedUnits = new TreeSet<String>();
//            for ( CUnit unit: mwclient.getPlayer().getHangar() ){
//                if ( UnitUtils.hasArmorDamage(unit.getEntity()) || UnitUtils.hasCriticalDamage(unit.getEntity()) )
//                    damagedUnits.add("#"+unit.getId()+" "+unit.getModelName());
//            }
//            repairComboBox = new JComboBox<Object>(damagedUnits.toArray());
//        }

		//creates a list of factories that can be refreshed
        if (Integer.parseInt(mwclient.getserverConfigs("FluToRefreshFactory")) > 0)
        {
		    TreeSet<String> factories = new TreeSet<String>();
		    House faction = mwclient.getData().getHouseByName(mwclient.getPlayer().getHouse());
		    Iterator<Planet> planets = mwclient.getData().getAllPlanets().iterator();
		    names.add(refreshCommand);

		    while ( planets.hasNext() )
		    {
		        Planet planet = planets.next();

		        if ( !planet.isOwner(faction.getId()) )
		            continue;

		        Iterator<UnitFactory> unitFactories = planet.getUnitFactories().iterator();
		        while (unitFactories.hasNext())
		        {
		            UnitFactory factory = unitFactories.next();
		            if ( factory.getTicksUntilRefresh() > 0)
		                factories.add(planet.getName()+": "+factory.getName()+ "("+factory.getTicksUntilRefresh()+")");
		        }
		    }
		    refreshComboBox = new JComboBox<Object>(factories.toArray());
		}
        
        if(names.isEmpty())
        	names.add("None Available");

		rewardsComboBox = new JComboBox<Object>(names.toArray());

		//stored values.
		cost = 0;

		//Set the tooltips and actions for dialouge buttons
		okayButton.setActionCommand(okayCommand);
		cancelButton.setActionCommand(cancelCommand);
		//factionComboBox.setActionCommand(factionCommand);
		rewardsComboBox.setActionCommand(rewardCommand);
		//weightComboBox.setActionCommand(weightCommand);
        //unitComboBox.setActionCommand(unitCommand);
        //repodComboBox.setActionCommand(repodCommand);
		refreshComboBox.setActionCommand(refreshCommand);
        //techComboBox.setActionCommand(techComboCommand);
        //repairComboBox.setActionCommand(repairCommand);
		//amountText.setActionCommand(amountCommand);

		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		okayButton.setToolTipText("Save Options");
		cancelButton.setToolTipText("Exit without saving changes");
		//factionComboBox.addActionListener(this);
		rewardsComboBox.addActionListener(this);
		//weightComboBox.addActionListener(this);
        //unitComboBox.addActionListener(this);
        //repodComboBox.addActionListener(this);
		//pUnitsComboBox.addActionListener(this);
		refreshComboBox.addActionListener(this);
        //techComboBox.addActionListener(this);
        //repairComboBox.addActionListener(this);

        amountText.addKeyListener(this);

		//CREATE THE PANELS
		JPanel rewardPanel = new JPanel();//player name, etc

		/*
		 * Format the Reward Points panel. Spring layout.
		 */
		rewardPanel.setLayout(new BoxLayout(rewardPanel,BoxLayout.Y_AXIS));

		JPanel comboPanel = new JPanel(new SpringLayout());
		JPanel costPanel = new JPanel();

		comboPanel.add(rewardLabel);
		rewardsComboBox.setToolTipText("Select your Reward Type");
		comboPanel.add(rewardsComboBox);

//		comboPanel.add(factionLabel);
//		factionComboBox.setToolTipText("Select the faction build table you wish to use");
//		comboPanel.add(factionComboBox);
//
//		comboPanel.add(unitLabel);
//		unitComboBox.setToolTipText("Select the Unit Type");
//		comboPanel.add(unitComboBox);
//
//		comboPanel.add(weightLabel);
//		weightComboBox.setToolTipText("Unit Weight Class");
//		comboPanel.add(weightComboBox);
//
//		comboPanel.add(pUnitsLabel);
//		pUnitsComboBox.setToolTipText("Unit");
//		comboPanel.add(pUnitsComboBox);
//
//        comboPanel.add(repodLabel);
//		repodComboBox.setToolTipText("Repod Selection Type");
//		comboPanel.add(repodComboBox);

		comboPanel.add(refreshLabel);
		refreshComboBox.setToolTipText("Refresh Factory");
		comboPanel.add(refreshComboBox);

//        if ( mwclient.isUsingAdvanceRepairs() ){
//            comboPanel.add(techComboLabel);
//            techComboBox.setToolTipText("Tech Selection Type");
//            techComboBox.setSelectedIndex(0);
//            comboPanel.add(techComboBox);
//        }
//
//        comboPanel.add(repairLabel);
//        repairComboBox.setToolTipText("Repair Unit with " + mwclient.getserverConfigs("RPShortName") + "s");
//        comboPanel.add(repairComboBox);

		comboPanel.add(amountLabel);
		comboPanel.add(amountText);

		//run the spring layout
		SpringLayoutHelper.setupSpringGrid(comboPanel,2);

		rewardPanel.add(comboPanel);
		costPanel.add(costLabel);
		rewardPanel.add(costPanel);

		costLabel.setText("Result: no expenditure");

//        try{
//            factionComboBox.setSelectedItem(mwclient.getPlayer().getHouse());
//        }catch (Exception ex){
//            factionComboBox.setSelectedIndex(0);
//        }

        //factionComboBox.setSelectedIndex(0);
        //techComboBox.setSelectedIndex(0);
        rewardsComboBox.setSelectedIndex(0);

//		if (Boolean.parseBoolean(mwclient.getserverConfigs("AllowUnitsForRewards"))) {
//			rewardsComboBox.setSelectedItem("Units");
//            weightComboBox.setSelectedIndex(0);
//            unitComboBox.setSelectedIndex(0);
//		}
//		else
//			rewardsComboBox.setSelectedIndex(0);

		JPanel mainPanel = new JPanel();

		// Set the user's options
		Object[] options = { okayButton, cancelButton };

		// Create the pane containing the buttons
		pane = new JOptionPane(rewardPanel,JOptionPane.PLAIN_MESSAGE,JOptionPane.DEFAULT_OPTION, null, options, null);

		// Create the main dialog and set the default button
		dialog = pane.createDialog(mainPanel, windowName);
		dialog.getRootPane().setDefaultButton(cancelButton);
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

	public void keyReleased(KeyEvent e)
	{
	    String selection = (String)rewardsComboBox.getSelectedItem();
	    cost = Integer.parseInt(amountText.getText());
//	    if (!selection.equals("Units")){
//		    if ( selection.equals("Techs")){
//                if ( !mwclient.isUsingAdvanceRepairs() ){
//    		        int total = cost * Integer.parseInt(mwclient.getserverConfigs("TechsForARewardPoint"));
//    		        costLabel.setText("Result: Hire " +total+" Techs");
//                }
//		    }
//		    if ( selection.equals(repodCommand) )
//		    {
//		        cost = fluToRepod;
//		        if ( ((String)repodComboBox.getSelectedItem()).equals("Random") )
//		            cost /= 2;
//				costLabel.setText(mwclient.getserverConfigs("FluLongName") + " Required: "+cost+" " + mwclient.getserverConfigs("FluShortName"));
//		    }
		    if ( selection.equals(refreshCommand) )
		    {
		        cost = Integer.parseInt(mwclient.getserverConfigs("FluToRefreshFactory"));
		        costLabel.setText(mwclient.getserverConfigs("FluLongName") + " Required: "+cost+" " + mwclient.getserverConfigs("FluShortName"));
		        dialog.repaint();
		    }
		    else if ( selection.equals(mwclient.getserverConfigs("MoneyLongName")) )
			{
				int total = cost * Integer.parseInt(mwclient.getserverConfigs("Cbills_Per_Flu"));
				costLabel.setText("Result: Gain "+mwclient.moneyOrFluMessage(true,true,total));
			}
//		    else
//		    {
//				int total = cost * Integer.parseInt(mwclient.getserverConfigs("Cbills_Per_Flu"));
//				costLabel.setText("Result: Gain "+mwclient.moneyOrFluMessage(true,true,total));
//		    }

	}

	public void keyPressed(KeyEvent e){
	}

	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();

		if (command.equals(okayCommand)) {
		    String selection = (String)rewardsComboBox.getSelectedItem();

//		    if ( selection.equals("Units") ){
//		        String type = (String)unitComboBox.getSelectedItem();
//		        String weight = (String)weightComboBox.getSelectedItem();
//		        String faction = (String)factionComboBox.getSelectedItem();
//			    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c userewardpoints#2#"+ type + "#" + weight + "#" + faction);
//		    }
//		    else if ( selection.equals("Techs")){
//                if ( mwclient.isUsingAdvanceRepairs() )
//                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c userewardpoints#0#"+ techComboBox.getSelectedIndex());
//                else
//                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c userewardpoints#0#"+ amountText.getText());
//		    }
//		    if ( selection.equals(repodCommand) )
//		    {
//		        if ( pUnitsComboBox.getComponentCount() < 1)
//		            dialog.dispose();
//		        String options = "#GLOBALFLU";
//		        if ( ((String)repodComboBox.getSelectedItem()).equals("Random") )
//		        	options +="#RANDOM";
//		        StringTokenizer unitid = new StringTokenizer((String)pUnitsComboBox.getSelectedItem()," ");
//		        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c repod"+unitid.nextToken()+options);
//		    }
		    if ( selection.equals(refreshCommand) )
		    {
		        if ( refreshComboBox.getComponentCount() < 1)
		            dialog.dispose();
		        String factoryInfo = (String)refreshComboBox.getSelectedItem();
		        String planet = factoryInfo.substring(0,factoryInfo.indexOf(":")).trim();
		        String factory = factoryInfo.substring(planet.length()+2,factoryInfo.indexOf("(")).trim();
		        String useFlu = "true";
		        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c refreshFactory#"+planet+"#"+factory+"#"+useFlu);
		    }
//            else if ( selection.equals(repairCommand)){
//                String selectionName = (String)repairComboBox.getSelectedItem();
//                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c userewardpoints#3#"+selectionName.trim().substring(0,selectionName.indexOf(" ")));
//            }
			// @Author Salient (mwosux@gmail.com) , Add RP for CBills
		    else if ( selection.equals(mwclient.getserverConfigs("MoneyLongName")) )
			{
		        mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c useinfluence#4#"+ amountText.getText());
            }
		    else
		    {
		    	mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "mail "+mwclient.getUsername()+ "No Influence Spent. Options are disabled on this server.");
		    }

			dialog.dispose();
		}
		else if (command.equals(cancelCommand)) {
			pane.setValue(cancelButton);
			dialog.dispose();
		}
		else if (command.equals(rewardCommand)){
		    String selection = (String)rewardsComboBox.getSelectedItem();
//		    if ( selection.equals("Units")){
//		        makeVisible(true,false,false);
//				unitComboBox.setSelectedIndex(0);
//				weightComboBox.setSelectedIndex(0);
//                try{
//                    factionComboBox.setSelectedItem(mwclient.getPlayer().getHouse());
//                }catch (Exception ex){
//                    factionComboBox.setSelectedIndex(0);
//                }
//
//				cost = getUnitRPCost();
//				costLabel.setText(mwclient.getserverConfigs("RPShortName") + " Required: "+cost+" " + mwclient.getserverConfigs("RPShortName"));
//		    }
//		    else if (selection.equals("Techs")){
//                makeVisible(false,false,false);
//                if ( mwclient.isUsingAdvanceRepairs() ){
//
//                    int type = techComboBox.getSelectedIndex();
//                    int total = Integer.parseInt(mwclient.getserverConfigs("RewardPointsFor"+UnitUtils.techDescription(type)));
//                    costLabel.setText("Hire 1 "+UnitUtils.techDescription(type)+" tech for "+total+" " + mwclient.getserverConfigs("RPShortName"));
//                    techComboBox.setVisible(true);
//                    techComboLabel.setVisible(true);
//
//                    amountText.setVisible(false);
//                    amountLabel.setVisible(false);
//                }else{
//                    amountText.setText("0");
//    				cost = Integer.parseInt(amountText.getText());
//    		        int total = cost * Integer.parseInt(mwclient.getserverConfigs("TechsForARewardPoint"));
//    		        costLabel.setText("Result: Hire "+total+" Techs");
//    				costLabel.repaint();
//                }
//		    }
//		    if ( selection.equals(repodCommand) )
//		    {
//		        if ( pUnitsComboBox.getItemCount() >= 1)
//		            pUnitsComboBox.setSelectedIndex(0);
//		        cost = fluToRepod;
//		        if ( ((String)repodComboBox.getSelectedItem()).equals("Random") )
//		            cost /= 2;
//				costLabel.setText(mwclient.getserverConfigs("FluShortName") + " Required: "+cost+" " + mwclient.getserverConfigs("FluShortName"));
//				makeVisible(false,true,false);
//		    }
		    if ( selection.equals(refreshCommand) )
		    {
		        if ( refreshComboBox.getItemCount() >= 1)
		           refreshComboBox.setSelectedIndex(0);
		        cost = Integer.parseInt(mwclient.getserverConfigs("FluToRefreshFactory"));
		        costLabel.setText(mwclient.getserverConfigs("FluLongName") + " Required: "+cost+" " + mwclient.getserverConfigs("FluShortName"));
		        makeVisible(false,false,true);
		    }
//            else if (selection.equals(repairCommand)){
//                makeVisible(false,false,false);
//
//                if ( repairComboBox.getItemCount() > 0)
//                    repairComboBox.setSelectedIndex(0);
//                costLabel.setText("Repair Cost: "+mwclient.getserverConfigs("RewardPointsForRepair"));
//                repairComboBox.setVisible(true);
//                repairLabel.setVisible(true);
//
//                amountText.setVisible(false);
//                amountLabel.setVisible(false);
//            }
		    else if (selection.equalsIgnoreCase(mwclient.getserverConfigs("MoneyLongName"))){
				amountText.setText("0");
				cost = Integer.parseInt(amountText.getText());
				int total = cost * Integer.parseInt(mwclient.getserverConfigs("Cbills_Per_Flu"));
				costLabel.setText("Result: Gain "+mwclient.moneyOrFluMessage(true,true,total));
				makeVisible(false,false,false);
			}
		    else
		    {
				makeVisible(true,false,false);
		    }
		}
//		else if ( command.equals(repodCommand)){
//	        cost = Integer.parseInt(mwclient.getserverConfigs("GlobalRepodWithRPCost"));
//	        if ( ((String)repodComboBox.getSelectedItem()).equals("Random") )
//	            cost /= 2;
//			costLabel.setText(mwclient.getserverConfigs("RPShortName") + " Required: "+cost+" " + mwclient.getserverConfigs("RPShortName"));
//		}
//		else if (command.equals(weightCommand)
//		        || command.equals(unitCommand)
//		        || command.equals(factionCommand)){
//			cost = getUnitRPCost();
//			costLabel.setText(mwclient.getserverConfigs("RPShortName") + " Required: "+cost+" " + mwclient.getserverConfigs("RPShortName"));
//		}
//        else if (command.equals(techComboCommand)){
//            makeVisible(false,false,false);
//
//            int type = techComboBox.getSelectedIndex();
//            int total = Integer.parseInt(mwclient.getserverConfigs("RewardPointsFor"+UnitUtils.techDescription(type)));
//            costLabel.setText("Hire 1 "+UnitUtils.techDescription(type)+" tech for "+total+" " + mwclient.getserverConfigs("RPShortName"));
//            techComboBox.setVisible(true);
//            techComboLabel.setVisible(true);
//
//            amountText.setVisible(false);
//            amountLabel.setVisible(false);
//        }
//        else if (command.equals(repairCommand)){
//            makeVisible(false,false,false);
//
//            costLabel.setText("Repair Cost: "+mwclient.getserverConfigs("RewardPointsForRepair"));
//            repairComboBox.setVisible(true);
//            repairLabel.setVisible(true);
//
//            amountText.setVisible(false);
//            amountLabel.setVisible(false);
//        }
	}

	private void makeVisible(boolean visible, boolean repod, boolean refresh){
//		unitComboBox.setVisible(visible);
//		weightComboBox.setVisible(visible);
//		factionComboBox.setVisible(visible);
//		unitLabel.setVisible(visible);
//		weightLabel.setVisible(visible);
//		factionLabel.setVisible(visible);
//
//		repodComboBox.setVisible(repod);
//		repodLabel.setVisible(repod);
//		pUnitsComboBox.setVisible(repod);
//		pUnitsLabel.setVisible(repod);
//
		refreshComboBox.setVisible(refresh);
		refreshLabel.setVisible(refresh);

		if ( repod || refresh ){
		    amountLabel.setVisible(false);
		    amountText.setVisible(false);
		}
		else{
		    amountLabel.setVisible(!visible);
		    amountText.setVisible(!visible);
		}

//        techComboBox.setVisible(false);
//        techComboLabel.setVisible(false);
//        repairComboBox.setVisible(false);
//        repairLabel.setVisible(false);

	}

//	private int getUnitRPCost(){
//
//        if ( !Boolean.parseBoolean(mwclient.getserverConfigs("AllowUnitsForRewards")) )
//            return 0;
//
//        int type = Unit.getTypeIDForName((String)unitComboBox.getSelectedItem());
//	    int weight = Unit.getWeightIDForName((String)weightComboBox.getSelectedItem());
//	    String House = (String)factionComboBox.getSelectedItem();
//	    int cost = 0;
//
//
//	    String configName = "";
//	    if (type == Unit.MEK) {
//			configName = Unit.getWeightClassDesc(weight)+"RP";
//		} else {
//			configName = Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type)+"RP";
//		}
//	    cost = Integer.parseInt(mwclient.getserverConfigs(configName));
//
//	    if ( House.equals("Rare"))
//	        cost *= Double.parseDouble(mwclient.getserverConfigs("RewardPointMultiplierForRare"));
//	    else if ( !House.equals("Common") && !House.equals(mwclient.getPlayer().getHouse())){
//	    	double multiplier = Double.parseDouble(mwclient.getserverConfigs(mwclient.getPlayer().getHouse()+"To"+House+"RewardPointMultiplier"));
//
//	    	if ( multiplier < 0 )
//	    		multiplier = Double.parseDouble(mwclient.getserverConfigs("RewardPointNonHouseMultiplier"));
//	        cost *= multiplier;
//	    }
//
//	    return cost;
//	}

}//end RewardPointsDialog.java
