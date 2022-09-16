/*
 * MekWars - Copyright (C) 2011
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/**
 * @author Spork
 * @author jtighe
 */

package admin.dialog.serverConfigDialogs;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.Unit;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

public class UnitsPanel extends JPanel {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2383908910758773550L;

    private JTextField baseTextField = new JTextField(5);
    private JCheckBox baseCheckBox = new JCheckBox();
    //private Dimension screenSize; //not used?
    
	private void init() {
		// Set up the costs (cbills, flu, PP)
		JPanel leftPanel = new JPanel();
		JPanel rightPanel = new JPanel();
		
		String quirksToolTip = "<html> Allows mekwars to 'see' quirks, also checks to make sure that the quirk file in use<br>" 
				+ "by the server is the same as the client/host. This option requires the following files to exist <br>"
				+ "on client AND server:<br>data/unitQuirksSchema.xsl <br>data/canonUnitQuirks.xml <br>"
				+ "mmconf/unitQuirksOverride.xml<br>"
				+ "Note that the override list is checked first, as such while you 'can' edit the canon list<br>"
				+ "you should use the override xml file to add or remove quirks.<br>"
				+ "See the xml files themselves for more information.<br>"
				+ "Note that the Host must have stratOps quirks enabled as well for quirks to work<br>"
				+ "Note that even if this option is disabled, a host can still enable megamek's quirks<br>"
				+ "</html>" ;
		
		// Left Panel Setup - unit costs
		JPanel costGrid = new JPanel(new SpringLayout());
		
		costGrid.add(new JLabel("Unit"));
		costGrid.add(new JLabel("CBills"));
		costGrid.add(new JLabel("Influence"));
		costGrid.add(new JLabel("PP"));
		costGrid.add(new JLabel("RP"));
		
		for (int unitType = Unit.MEK; unitType <= Unit.AERO; unitType++) {
			for (int unitWeight = Unit.LIGHT; unitWeight <= Unit.ASSAULT; unitWeight++) {
				String baseName = "";
				if (unitType == Unit.MEK) {
					baseName = Unit.getWeightClassDesc(unitWeight);
				} else {
					baseName = Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType);
				}
				
				costGrid.add(new JLabel(Unit.getWeightClassDesc(unitWeight) + " " + Unit.getTypeClassDesc(unitType)));
				baseTextField = new JTextField(4);
				baseTextField.setName(baseName + "Price");
				baseTextField.setToolTipText(Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "_cbills");
				costGrid.add(baseTextField);
				
				baseTextField = new JTextField(4);
				baseTextField.setName(baseName + "Inf");
				baseTextField.setToolTipText(Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "_flu");
				costGrid.add(baseTextField);
				
				baseTextField = new JTextField(4);
				baseTextField.setName(baseName + "PP");
				baseTextField.setToolTipText(Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "_PP");
				costGrid.add(baseTextField);
				
				baseTextField = new JTextField(4);
				baseTextField.setName(baseName + "RP");
				baseTextField.setToolTipText(Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "_RP");
				costGrid.add(baseTextField);
				
				
			}
		}
		
		SpringLayoutHelper.setupSpringGrid(costGrid, 5);
		leftPanel.add(costGrid);
		leftPanel.setBorder(BorderFactory.createTitledBorder("Unit Costs"));
		
		// Right Panel Setup - checkboxes and such		
		rightPanel.setLayout(new VerticalLayout(5, VerticalLayout.LEFT, VerticalLayout.TOP));
		
		JPanel unitUsePanel = new JPanel(new SpringLayout());
		
		baseCheckBox = new JCheckBox("Use Vehicles");
        baseCheckBox.setToolTipText("Uncheck to disable Vehs.");
        baseCheckBox.setName("UseVehicle");
        unitUsePanel.add(baseCheckBox);
		
		baseCheckBox = new JCheckBox("Use Infantry");
        baseCheckBox.setToolTipText("Uncheck to disable Infantry.");
        baseCheckBox.setName("UseInfantry");
        unitUsePanel.add(baseCheckBox);

		baseCheckBox = new JCheckBox("Use BattleArmor");
        baseCheckBox.setToolTipText("Uncheck to disable BattleArmor.");
        baseCheckBox.setName("UseBattleArmor");
        unitUsePanel.add(baseCheckBox);

		baseCheckBox = new JCheckBox("Use ProtoMeks");
        baseCheckBox.setToolTipText("Uncheck to disable ProtoMeks.");
        baseCheckBox.setName("UseProtoMek");
        unitUsePanel.add(baseCheckBox);

		baseCheckBox = new JCheckBox("Use Aero");
        baseCheckBox.setToolTipText("Uncheck to disable Aero.");
        baseCheckBox.setName("UseAero");
        unitUsePanel.add(baseCheckBox);

        SpringLayoutHelper.setupSpringGrid(unitUsePanel, 5);
        unitUsePanel.setBorder(BorderFactory.createTitledBorder("Unit Use"));
        
        // Commanders
        JPanel commandersPanel = new JPanel(new SpringLayout());
        commandersPanel.setBorder(BorderFactory.createTitledBorder("Unit Commanders"));
        
        baseCheckBox = new JCheckBox("Allow Mek Commanders");
        baseCheckBox.setToolTipText("<html>Allow meks to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderMek");
        commandersPanel.add(baseCheckBox);


        baseCheckBox = new JCheckBox("Allow Vehicle Commanders");
        baseCheckBox.setToolTipText("<html>Allow vehicles to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderVehicle");
        commandersPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Infantry Commanders");
        baseCheckBox.setToolTipText("<html>Allow infantry to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderInfantry");
        commandersPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow ProtoMek Commanders");
        baseCheckBox.setToolTipText("<html>Allow protomeks to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderProtoMek");
        commandersPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow BattleArmor Commanders");
        baseCheckBox.setToolTipText("<html>Allow battlearmor to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderBattleArmor");
        commandersPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow VTOL Commanders");
        baseCheckBox.setToolTipText("<html>Allow VTOL to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderVTOL");
        commandersPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Aero Commanders");
        baseCheckBox.setToolTipText("<html>Allow aero to be set as unit commanders<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowUnitCommanderAero");
        commandersPanel.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Allow Fighting Without Commanders");
        baseCheckBox.setToolTipText("<html>Allow players to go active without any unit commanders set in their armies<br>for the kill all unit commanders operation victory condition</html>");
        baseCheckBox.setName("allowGoingActiveWithoutUnitCommanders");
        commandersPanel.add(baseCheckBox);
        
        SpringLayoutHelper.setupSpringGrid(commandersPanel, 2);
        
        
        JPanel miscCheckBoxPanel = new JPanel(new SpringLayout());
        
        baseCheckBox = new JCheckBox("Light Inf");
        baseCheckBox.setToolTipText("Check to have all inf count as light.");
        baseCheckBox.setName("UseOnlyLightInfantry");
        miscCheckBoxPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Light Vehs");
        baseCheckBox.setToolTipText("Check to have all vehs count as light.");
        baseCheckBox.setName("UseOnlyOneVehicleSize");
        miscCheckBoxPanel.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Free Foot");
        baseCheckBox.setToolTipText("Check to have Foot Inf take 0 techs/bays");
        baseCheckBox.setName("FootInfTakeNoBays");
        miscCheckBoxPanel.add(baseCheckBox);
		
        baseCheckBox = new JCheckBox("Real Cost");
        baseCheckBox.setToolTipText("<html>Check to use MM calculated costs for each unit.<br>The calculated cost will be used or the unit set price which ever is higher<br>Requires a reboot of the server.</html>");
        baseCheckBox.setName("UseCalculatedCosts");
        miscCheckBoxPanel.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Enable Quirks");
        baseCheckBox.setToolTipText(quirksToolTip);
        baseCheckBox.setName("EnableQuirks");
        miscCheckBoxPanel.add(baseCheckBox);
            
        SpringLayoutHelper.setupSpringGrid(miscCheckBoxPanel, 5);
        miscCheckBoxPanel.setBorder(BorderFactory.createTitledBorder("Unit Options"));
        
        JPanel weightPanel = new JPanel();
        weightPanel.setBorder(BorderFactory.createTitledBorder("Factory Weight Names"));
        weightPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
        for (int i = Unit.LIGHT; i <= Unit.ASSAULT; i++) {
        	baseTextField = new JTextField(5);
        	weightPanel.add(new JLabel(Unit.getWeightClassDesc(i) + ":", SwingConstants.TRAILING));
        	baseTextField.setToolTipText("<html>Title to be displayed of " + Unit.getWeightClassDesc(i).toLowerCase() + " factories<br>in the Client House Bays Tab</html>");
        	baseTextField.setName(Unit.getWeightClassDesc(i) + "FactoryTypeTitle");
        	weightPanel.add(baseTextField);
        }
        
        
        JPanel typesPanel = new JPanel(new SpringLayout());
        typesPanel.setBorder(BorderFactory.createTitledBorder("Factory Type Names"));
        
        for (int i = Unit.MEK; i < Unit.MAXBUILD; i++) {
            baseTextField = new JTextField(5);
            typesPanel.add(new JLabel(Unit.getTypeClassDesc(i) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<html>Title to be displayed of " + Unit.getTypeClassDesc(i) + " factories<br>in the Client House Bays Tab</html>");
            baseTextField.setName(Unit.getTypeClassDesc(i) + "FactoryClassTitle");
            typesPanel.add(baseTextField);
        }
        
        SpringLayoutHelper.setupSpringGrid(typesPanel, 6);
        
        JPanel miscPanel = new JPanel(new SpringLayout());
        miscPanel.setBorder(BorderFactory.createTitledBorder("Miscellaneous"));
        
        baseTextField = new JTextField(5);
        miscPanel.add(new JLabel("NonOrig Money Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Cost multiplier for units purchased from a factory not originally<br>" + "owned by purchasing player's faction. Examples:<br>" + "1: 80 CBill Faction Base * 1.15 CBillMultiplier = 92 CBill final cost.<br>" + "2: 80 CBill Faction Base * 1.00 CBillMultiplier = 80 CBill final cost.<br>" + "3: 80 CBill Faction Base * 0.75 CBillMultiplier = 60 CBill final cost.</html>");
        baseTextField.setName("NonOriginalCBillMultiplier");
        miscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        miscPanel.add(new JLabel("NonOrig Flu Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Flu price multiplier for units purchased from a factory not originally<br>" + "owned by purchasing player's faction. See Money Multi for examples.</html>");
        baseTextField.setName("NonOriginalInfluenceMultiplier");
        miscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        miscPanel.add(new JLabel("NonOrig PP Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Component use multiplier for units purchased from a factory not originally<br>" + "owned by purchasing player's faction. See Money Multi for examples.</html>");
        baseTextField.setName("NonOriginalComponentMultiplier");
        miscPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        miscPanel.add(new JLabel("Max Armies:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max # of armies a unit can join");
        baseTextField.setName("UnitsInMultipleArmiesAmount");
        miscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        miscPanel.add(new JLabel("Cost Multiplier:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is set to lower or raise the calculated cost.<br>i.e. cost is 10 mil for a unit .1(10%) will set it to 1 mil.</html>");
        baseTextField.setName("CostModifier");
        miscPanel.add(baseTextField);
        
        SpringLayoutHelper.setupSpringGrid(miscPanel, 6);
        
        commandersPanel.setPreferredSize(new Dimension(unitUsePanel.getPreferredSize().width, commandersPanel.getPreferredSize().height));
        miscCheckBoxPanel.setPreferredSize(new Dimension(unitUsePanel.getPreferredSize().width, miscCheckBoxPanel.getPreferredSize().height));
        weightPanel.setPreferredSize(new Dimension(unitUsePanel.getPreferredSize().width, weightPanel.getPreferredSize().height));
        typesPanel.setPreferredSize(new Dimension(unitUsePanel.getPreferredSize().width, typesPanel.getPreferredSize().height));
        miscPanel.setPreferredSize(new Dimension(unitUsePanel.getPreferredSize().width, miscPanel.getPreferredSize().height));
        
        rightPanel.add(unitUsePanel);
        rightPanel.add(commandersPanel);
        rightPanel.add(miscCheckBoxPanel);
        rightPanel.add(weightPanel);
        rightPanel.add(typesPanel);
        rightPanel.add(miscPanel);
        
        // Even up the heights
        rightPanel.setPreferredSize(new Dimension(rightPanel.getPreferredSize().width, leftPanel.getPreferredSize().height));
		add(leftPanel);
		add(rightPanel);
	}
	
	public UnitsPanel(MWClient mwclient) {
		super();
		init();
	}
}
