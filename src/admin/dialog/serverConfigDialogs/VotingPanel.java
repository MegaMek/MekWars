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
 * @author Spork
 * @author jtighe
 */
public class VotingPanel extends JPanel {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -7561000786384497587L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
	
	public VotingPanel() {
		/*
		 * VOTE PANEL CONSTRUCTION
		 */
		JPanel voteBoxPanel = new JPanel();
		voteBoxPanel.setLayout(new BoxLayout(voteBoxPanel, BoxLayout.Y_AXIS));
		JPanel voteSpring = new JPanel(new SpringLayout());

		// set up voting CBox
		BaseCheckBox = new JCheckBox("Enable Voting");

		BaseCheckBox.setToolTipText("If checked, players are able to cast votes.");
		BaseCheckBox.setName("VotingEnabled");
		voteBoxPanel.add(BaseCheckBox);

		// set up vote spring
		baseTextField = new JTextField(5);
		voteSpring.add(new JLabel("Base Votes:", SwingConstants.TRAILING));
		baseTextField.setToolTipText("Starting number of votes");
		baseTextField.setName("StartingVotes");
		voteSpring.add(baseTextField);

		baseTextField = new JTextField(5);
		voteSpring.add(new JLabel("XP For Vote:", SwingConstants.TRAILING));
		baseTextField.setToolTipText("Amount of XP required to earn an additional vote");
		baseTextField.setName("XPForAdditionalVote");
		voteSpring.add(baseTextField);

		baseTextField = new JTextField(5);
		voteSpring.add(new JLabel("Max Votes:", SwingConstants.TRAILING));
		baseTextField.setToolTipText("Maximum number of votes a player can have");
		baseTextField.setName("MaximumVotes");
		voteSpring.add(baseTextField);

		SpringLayoutHelper.setupSpringGrid(voteSpring, 3, 2);

		// finalize the layout

		voteBoxPanel.add(voteSpring);
		add(voteBoxPanel);
	}
}
