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
public class LossCompensationPanel extends JPanel {

	private static final long serialVersionUID = 5752395790893721813L;
	private JTextField baseTextField = new JTextField(5);
	
	public LossCompensationPanel() {
		super();
        /*
         * LOSS COMPENSATION setup defaults.setProperty("", "0");//int defaults.setProperty("", ".50");//float defaults.setProperty("", ".50");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "1.0");//float defaults.setProperty("", "0");//int. 0 ensures no payment by default.
         */
        JPanel lossCompSpring = new JPanel(new SpringLayout());

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Base Loss Payment:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Base compensation given for losses. float value.");
        baseTextField.setName("BaseUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Variable Loss Payment:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Percentage of the cost of a similar new unit to the added to the Base<br>" + "Payment. For example, if Base is 10 and a unit costs 40 CBills w/ a Variable<br>" + "Loss Payment of 0.50 the starting compensation (before multipliers and<br>" + "caps) will be (40 * .5) = 20 + 10 for a total of 30 CBills. float value</html>.");
        baseTextField.setName("NewCostMultiUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Salvage Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Multiplier applied to loss compensation (base + variable) if a unit is taken<br>" + "by the enemy (instead of destroyed). Example: a unit that has 30 CBill base + var<br>" + "is taken by an enemy and a multi of .5 is applied, reducing loss compensation to<br>" + "15 CBills (30 * .5 = 15) before other multis and caps.</html>");
        baseTextField.setName("SalvageMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Mek Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Multiplier applied to loss compensation (base + variable) if unit in question is a mek.<br>" + "Example 1: Mek w 40 CBill base + var is destroyed * 1.25 mek multi = 50 CBills loss comp.<br>" + "Example 2: Mek w 40 CBill base + var is destroyed * 0.5 mek multi = 20 CBills loss comp.<br>" + "Note: Other multis and caps are also applied/checked. float value.</html>");
        baseTextField.setName("MekMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Veh Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Multiplier applied to loss compensation (base + variable) if unit in question is a vehicle.<br>" + "Example 1: Veh w 20 CBill base + var is destroyed * 1.25 mek multi = 25 CBills loss comp.<br>" + "Example 2: Veh w 20 CBill base + var is destroyed * 0.5 mek multi = 10 CBills loss comp.<br>" + "Note: Other multis and caps are also applied/checked. float value.</html>");
        baseTextField.setName("VehMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Proto Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("See Mek Multi and Veh Multi examples. float value.");
        baseTextField.setName("ProtoMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("BA Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("See Mek Multi and Veh Multi examples. float value.");
        baseTextField.setName("BAMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Inf Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("See Mek Multi and Veh Multi examples. float value.");
        baseTextField.setName("InfMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Aero Multi:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("See Mek Multi and Veh Multi examples. float value.");
        baseTextField.setName("AeroMultiToUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Loss Cap (Multiple):", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Max amount of compensaton, expressed as a multiple of the cost of a similar<br>" + "new unit from the player's faction. The Flat cap is checked AFTER the multiple cap.<br><br>" + "Example 1: A new light mek costs 20 CBills. Base comp is 10 and var comp is<br>" + "set to .50, giving a base + var of 20 CBills. The Multiple Cap is .75 of a<br>" + "new unit's cost (20 * .75 = 15), so the compensation is capped at 15 CBills.<br>" + "<br>" + "Example 2: A new assault mek costs 100 CBills. Base compensation is 0 and var comp<br>" + "is .60, for a base + var of 60 CBills. There's also a Mek Multiplier if 1.50, which<br>" + "boosts compensation to 90 CBills. The Multiple Cap is .80 (100 * .80 = 80), so the<br>" + "compensation is reduced to 80 CBills.</html>");
        baseTextField.setName("NewCostMultiMaxUnitLossPayment");
        lossCompSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        lossCompSpring.add(new JLabel("Loss Cap (Flat):", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Max amount of compensaton, expressed as a a simple integer. The Flat cap is checked<br>" + "AFTER the multiple cap. Battle Loss Compensation can be disabled by setting a flat cap<br>" + "of 0 CBills.<br><br>" + "Example 1: A assault light mek costs 100 CBills. Base comp is 10 and var comp is<br>" + "set to .50, giving a base + var of 60 CBills. The Flat Cap 40, so the compensation is<br>" + "reduced to 40 CBills.<br>" + "<br>" + "Example 2: A new light mek costs 20 CBills. Base compensation is 10 and var comp<br>" + "is .50, for a base + var of 20 CBills. The flat cap is 40 CBills. Unless there's<br>" + "another multiplier (mek, salvage) or the payment is reduced by the Multiplier Cap,<br>" + "the player will receive more money (40 CBills) in loss comp than it cost to buy his<br>"
                + "unit in the first place. This would be bad. So set both the flat and multi caps!</html>");
        baseTextField.setName("FlatMaxUnitLossPayment");
        lossCompSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(lossCompSpring, 4);
        add(lossCompSpring);
	}

}