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

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class BattleValuePanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5581787185856926691L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public BattleValuePanel() {
		super();
        /*
         * BATTLE VALUE Panel
         */
        JPanel battleValueBox = new JPanel();
        battleValueBox.setLayout(new BoxLayout(battleValueBox, BoxLayout.Y_AXIS));
        JPanel battleValueCBoxGrid = new JPanel(new GridLayout(1, -1));
        JPanel battleValueSpring = new JPanel(new SpringLayout());

        BaseCheckBox = new JCheckBox("Use Force Size Rules");
        BaseCheckBox.setToolTipText("Use the Tech Manual Force Size BV Adjustments?");

        BaseCheckBox.setName("UseOperationsRule");
        battleValueCBoxGrid.add(BaseCheckBox);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("Mek Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element a mek counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("MekOperationsBVMod");
        battleValueSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("Vee Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element a vee counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("VehicleOperationsBVMod");
        battleValueSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("BA Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element a BA squad/star counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("BAOperationsBVMod");
        battleValueSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("Proto Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element a proto counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("ProtoOperationsBVMod");
        battleValueSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("Inf Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element a inf unit counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("InfantryOperationsBVMod");
        battleValueSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        battleValueSpring.add(new JLabel("Aero Force Size:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is how much of an element an aero counts as for the Force Size Calculation<br>Note this is a double field.</html>");
        baseTextField.setName("AeroOperationsBVMod");
        battleValueSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(battleValueSpring, 4);

        // finalize layout
        battleValueBox.add(battleValueCBoxGrid);
        battleValueBox.add(battleValueSpring);
        add(battleValueBox);
	}

}