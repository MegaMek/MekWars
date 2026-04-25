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

package admin.dialog.serverConfigDialogs;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import common.Unit;
import common.VerticalLayout;

/**
 * @author Spork
 */
public class UnitLimitsPanel extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = -8530331266870414139L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
    public UnitLimitsPanel() {
		super();
		// unitLimitsPanel construction
        
        JPanel uLimitsPanel = new JPanel();
        JPanel ulTopPanel = new JPanel();
        JPanel ulBottomPanel = new JPanel();
        
        ulTopPanel.add(new JLabel("Hangar Limits"));
        
        ulBottomPanel.setLayout(new GridLayout(7, 5));
        ulBottomPanel.add(new JLabel(" "));
        ulBottomPanel.add(new JLabel("Light"));
        ulBottomPanel.add(new JLabel("Medium"));
        ulBottomPanel.add(new JLabel("Heavy"));
        ulBottomPanel.add(new JLabel("Assault"));        
        
        for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
        	ulBottomPanel.add(new JLabel(Unit.getTypeClassDesc(type)));
        	for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
        		baseTextField = new JTextField(5);
        		baseTextField.setName("MaxHangar" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type));
        		baseTextField.setToolTipText("Limit hangar to this many " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type) + ((Unit.getTypeClassDesc(type) == "Infantry") ? "" : "s") + ".  -1 to disable limit");
        		ulBottomPanel.add(baseTextField);
        	}
        }
        ulTopPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        ulBottomPanel.setBorder(BorderFactory.createEmptyBorder(0,5,0,5));
        
        uLimitsPanel.setBorder(BorderFactory.createEtchedBorder());
        uLimitsPanel.setLayout(new VerticalLayout());
        uLimitsPanel.add(ulTopPanel);
        uLimitsPanel.add(ulBottomPanel);
        
        JPanel ulActionsPanel = new JPanel();
        JPanel ulAPTop = new JPanel();
        JPanel ulAPBottom = new JPanel();
        ulAPTop.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        ulAPBottom.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        
        BaseCheckBox = new JCheckBox("Disable Activation");
        BaseCheckBox.setName("DisableActivationIfOverHangarLimits");
        BaseCheckBox.setToolTipText("Players over the limits cannot go active.");
        ulAPTop.add(BaseCheckBox);
        BaseCheckBox = new JCheckBox("Disable AFR");
        BaseCheckBox.setName("DisableAFRIfOverHangarLimits");
        BaseCheckBox.setToolTipText("Players over the limits cannot initiate or defend Attack From Reserve.");
        ulAPTop.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Use Sliding Hangar Limits");
        BaseCheckBox.setName("UseSlidingHangarLimits");
        BaseCheckBox.setToolTipText("<html>Checking this box enables modified limits that increase in cost as more units are purchased.<br>See 'Using Sliding Hangar Limits.pdf'<br><br>Please note that at this time, this is an on/off switch - the per fight and on purchase options do nothing.</html>");
        ulAPBottom.add(BaseCheckBox);
        
        ulAPBottom.add(new JLabel("Multiplier:"));
        baseTextField = new JTextField(5);
        baseTextField.setName("SlidingHangarLimitModifier");
        baseTextField.setToolTipText("Multiplier for sliding hangar limits");
        ulAPBottom.add(baseTextField);
        
        BaseCheckBox = new JCheckBox("Apply to Purchase");
        BaseCheckBox.setName("SlidingHangarLimitsAffectPurchase");
        BaseCheckBox.setToolTipText("The over-limit penalty will be applied to purchase price");
        ulAPBottom.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Apply to Payout");
        BaseCheckBox.setName("SlidingHangarLimitsAffectPayout");
        BaseCheckBox.setToolTipText("The over-limit penalty will be applied to game payout");
        ulAPBottom.add(BaseCheckBox);
        
        ulActionsPanel.setLayout(new VerticalLayout());
        ulActionsPanel.add(ulAPTop);
        ulActionsPanel.add(ulAPBottom);
        ulActionsPanel.setBorder(BorderFactory.createEtchedBorder());
        
        JPanel limitsPanel = new JPanel();
        limitsPanel.setLayout(new VerticalLayout());
        limitsPanel.add(uLimitsPanel);
        limitsPanel.add(ulActionsPanel);
        
        JPanel bmLimitsPanel = new JPanel();
        bmLimitsPanel.setLayout(new GridLayout(7, 5));
        bmLimitsPanel.add(new JLabel(" "));
        bmLimitsPanel.add(new JLabel("Light"));
        bmLimitsPanel.add(new JLabel("Medium"));
        bmLimitsPanel.add(new JLabel("Heavy"));
        bmLimitsPanel.add(new JLabel("Assault"));
        
        bmLimitsPanel.add(new JLabel("Mechs: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightMeks");
        BaseCheckBox.setToolTipText("Players can buy Light Meks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumMeks");
        BaseCheckBox.setToolTipText("Players can buy Medium Meks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyMeks");
        BaseCheckBox.setToolTipText("Players can buy Heavy Meks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultMeks");
        BaseCheckBox.setToolTipText("Players can buy Assault Meks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        bmLimitsPanel.add(new JLabel("Vehicles: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightVehicles");
        BaseCheckBox.setToolTipText("Players can buy Light Vehicles from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumVehicles");
        BaseCheckBox.setToolTipText("Players can buy Medium Vehicles from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyVehicles");
        BaseCheckBox.setToolTipText("Players can buy Heavy Vehicles from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultVehicles");
        BaseCheckBox.setToolTipText("Players can buy Assault Vehicles from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        bmLimitsPanel.add(new JLabel("Infantry: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightInfantry");
        BaseCheckBox.setToolTipText("Players can buy Light Infantry from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumInfantry");
        BaseCheckBox.setToolTipText("Players can buy Medium Infantry from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyInfantry");
        BaseCheckBox.setToolTipText("Players can buy Heavy Infantry from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultInfantry");
        BaseCheckBox.setToolTipText("Players can buy Assault Infantry from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        bmLimitsPanel.add(new JLabel("BattleArmor: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightBA");
        BaseCheckBox.setToolTipText("Players can buy Light BA from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumBA");
        BaseCheckBox.setToolTipText("Players can buy Medium BA from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyBA");
        BaseCheckBox.setToolTipText("Players can buy Heavy BA from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultBA");
        BaseCheckBox.setToolTipText("Players can buy Assault BA from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        bmLimitsPanel.add(new JLabel("Protomeks: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightProtomeks");
        BaseCheckBox.setToolTipText("Players can buy Light Protomeks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumProtomeks");
        BaseCheckBox.setToolTipText("Players can buy Medium Protomeks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyProtomeks");
        BaseCheckBox.setToolTipText("Players can buy Heavy Protomeks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultProtomeks");
        BaseCheckBox.setToolTipText("Players can buy Assault Protomeks from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        bmLimitsPanel.add(new JLabel("Aero: "));
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMLightAero");
        BaseCheckBox.setToolTipText("Players can buy Light Aero from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMMediumAero");
        BaseCheckBox.setToolTipText("Players can buy Medium Aero from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMHeavyAero");
        BaseCheckBox.setToolTipText("Players can buy Heavy Aero from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("CanBuyBMAssaultAero");
        BaseCheckBox.setToolTipText("Players can buy Assault Aero from the BM");
        bmLimitsPanel.add(BaseCheckBox);
        
        JPanel bmLimitsBox = new JPanel();
        bmLimitsBox.setLayout(new BoxLayout(bmLimitsBox, BoxLayout.Y_AXIS));
        bmLimitsBox.setBorder(BorderFactory.createEtchedBorder());
        bmLimitsPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JPanel titlePanel = new JPanel();
        titlePanel.add(new JLabel("Black Market Limits"));
        bmLimitsBox.add(titlePanel);
        bmLimitsBox.add(bmLimitsPanel);
        
        setLayout(new VerticalLayout());
        setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
        add(limitsPanel);
        add(bmLimitsBox);
	}
}
