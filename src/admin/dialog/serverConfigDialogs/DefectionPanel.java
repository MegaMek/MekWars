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
public class DefectionPanel extends JPanel {

	private static final long serialVersionUID = 4431690943581192710L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public DefectionPanel() {
		super();
        /*
         * DEFECTION PANEL CONSTRUCTION Panel which controls most defection-related matter. Some SOL-specific things handled in Newbie panel.
         */
        JPanel defectionTextPanel1 = new JPanel(new SpringLayout());
        JPanel defectionTextPanel2 = new JPanel(new SpringLayout());
        JPanel defectionBoxPanel = new JPanel(new SpringLayout());

        // set up the defection percent loss text boxes
        baseTextField = new JTextField(5);
        defectionTextPanel1.add(new JLabel("Unit Loss Percent:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of a player's units lost during defection.");
        baseTextField.setName("DefectionUnitLossPercent");
        defectionTextPanel1.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel1.add(new JLabel("Flu Loss Percent:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of a player's influence lost during defection.");
        baseTextField.setName("DefectionInfluenceLossPercent");
        defectionTextPanel1.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel1.add(new JLabel("RP Loss Percent:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of a player's RP lost during defection.");
        baseTextField.setName("DefectionRewardLossPercent");
        defectionTextPanel1.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel1.add(new JLabel("Money Loss Percent:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of a player's money lost during defection.");
        baseTextField.setName("DefectionCBillLossPercent");
        defectionTextPanel1.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel1.add(new JLabel("XP Loss Percent:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of a player's XP lost during defection.");
        baseTextField.setName("DefectionEXPLossPercent");
        defectionTextPanel1.add(baseTextField);

        // set up defection flat loss boxes
        baseTextField = new JTextField(5);
        defectionTextPanel2.add(new JLabel("Unit Loss Flat:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of player's units lost during defection.<br>No effect if % is > 0!</HTML>");
        baseTextField.setName("DefectionUnitLossFlat");
        defectionTextPanel2.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel2.add(new JLabel("Flu Loss Flat:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of player's influence lost during defection.<br>No effect if % is > 0!</HTML>");
        baseTextField.setName("DefectionInfluenceLossFlat");
        defectionTextPanel2.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel2.add(new JLabel("RP Loss Flat:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of player's RP lost during defection.<br>No effect if % is > 0!</HTML>");
        baseTextField.setName("DefectionRewardLossFlat");
        defectionTextPanel2.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel2.add(new JLabel("Money Loss Flat:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of player's money lost during defection.<br>No effect if % is > 0!</HTML>");
        baseTextField.setName("DefectionCBillLossFlat");
        defectionTextPanel2.add(baseTextField);

        baseTextField = new JTextField(5);
        defectionTextPanel2.add(new JLabel("XP Loss Flat:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of player's XP lost during defection.<br>No effect if % is > 0!</HTML>");
        baseTextField.setName("DefectionEXPLossFlat");
        defectionTextPanel2.add(baseTextField);

        // set up the springs for the text fields
        SpringLayoutHelper.setupSpringGrid(defectionTextPanel1, 5, 2);
        SpringLayoutHelper.setupSpringGrid(defectionTextPanel2, 5, 2);

        // set up checkboxen
        BaseCheckBox = new JCheckBox("Merc Penalty");

        BaseCheckBox.setToolTipText("Check to penalize players joining Mercenary factions.");
        BaseCheckBox.setName("PenalizeDefectToMerc");
        defectionBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Non-Conq Penalty");

        BaseCheckBox.setToolTipText("Check to penalize players joining non-conquer factions.");
        BaseCheckBox.setName("PenalizeDefectToNonConq");
        defectionBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Reset SOL");

        BaseCheckBox.setToolTipText("Check to reset player's units and PPQ when they leave training.");
        BaseCheckBox.setName("ReplaceUnitsLeavingSOL");
        defectionBoxPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("SOL Faction Units");

        BaseCheckBox.setToolTipText("<HTML>" + "If both this box and \"Reset Leaving SOL\" are checked, players will<br>receive faction units drawn from their new faction's tables instead of<br> SOL units on defection. Units will be taken from the Standard Folder ONLY.</HTML>");
        BaseCheckBox.setName("FactionUnitsLeavingSOL");
        defectionBoxPanel.add(BaseCheckBox);

        // set up the springs for the check boxes
        SpringLayoutHelper.setupSpringGrid(defectionBoxPanel, 4);

        // finalize layout
        JPanel defectTemp = new JPanel();
        defectTemp.setLayout(new BoxLayout(defectTemp, BoxLayout.Y_AXIS));

        JPanel defectTextFlow = new JPanel();
        defectTextFlow.add(defectionTextPanel1);
        defectTextFlow.add(defectionTextPanel2);

        defectTemp.add(defectTextFlow);
        defectTemp.add(defectionBoxPanel);

        add(defectTemp);
	}

}