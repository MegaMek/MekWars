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

package mekwars.admin.dialog.serverConfigDialogs;

import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import mekwars.common.util.SpringLayoutHelper;

/**
 * @author Spork
 * @author jtighe
 */
public class VotingPanel extends JPanel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7561000786384497587L;

    public VotingPanel() {
        /*
         * VOTE PANEL CONSTRUCTION
         */
        JPanel voteBoxPanel = new JPanel();
        voteBoxPanel.setLayout(new BoxLayout(voteBoxPanel, BoxLayout.Y_AXIS));
        JPanel voteSpring = new JPanel(new SpringLayout());

        // set up voting CBox
        JCheckBox baseCheckBox = new JCheckBox("Enable Voting");

        baseCheckBox.setToolTipText("If checked, players are able to cast votes.");
        baseCheckBox.setName("VotingEnabled");
        voteBoxPanel.add(baseCheckBox);

        // set up vote spring
        JTextField baseTextField = new JTextField(5);
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
