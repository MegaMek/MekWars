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

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.util.SpringLayoutHelper;

public class InfluencePanel extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -5359808432287239311L;

    private JTextField baseTextField = new JTextField(5);

    public InfluencePanel(MWClient mwclient) {
		super();
		/*
         * INFLUENCE PANEL CONSTRUCTION Influence panel, where admins set influence gain controls (bv limits, etc) and action costs (bm bid, attack, and so on). Use nested layouts. A Box containing a Flow, which in turn contains two Springs
         */
        JPanel influenceBoxPanel = new JPanel();
        JPanel influenceFlowPanel = new JPanel();
        JPanel influenceSpring1 = new JPanel(new SpringLayout());// 7 items
        JPanel influenceSpring2 = new JPanel(new SpringLayout());// 7 items
        influenceBoxPanel.setLayout(new BoxLayout(influenceBoxPanel, BoxLayout.Y_AXIS));
        influenceBoxPanel.add(influenceFlowPanel);
        influenceFlowPanel.add(influenceSpring1);
        influenceFlowPanel.add(influenceSpring2);

        // load spring1 first
        baseTextField = new JTextField(5);
        influenceSpring1.add(new JLabel("Max Player " + mwclient.moneyOrFluMessage(false, false, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, false, -1) + " ceiling");
        baseTextField.setName("InfluenceCeiling");
        influenceSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        influenceSpring1.add(new JLabel("XP Rollover:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of XP that will trigger 1 Flu to be given to player");
        baseTextField.setName("FluXPRollOverCap");
        influenceSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        influenceSpring1.add(new JLabel("Min Time for " + mwclient.moneyOrFluMessage(false, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum active time to receive flu @ check.");
        baseTextField.setName("InfluenceTimeMin");
        influenceSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        influenceSpring1.add(new JLabel("Floor Penalty:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount removed from TotalArmies when an Army abutts the MinBV");
        baseTextField.setName("FloorPenalty");
        influenceSpring1.add(baseTextField);

		baseTextField = new JTextField(5); //@salient
		influenceSpring1.add(new JLabel(mwclient.moneyOrFluMessage(true, true, -1) + " per " + mwclient.moneyOrFluMessage(false, true, -1), SwingConstants.TRAILING));
		baseTextField.setToolTipText("The ability to convert Flu to CB and the number of CB given per 1 flu. Disabled if set to zero. ");
		baseTextField.setName("Cbills_Per_Flu");
		influenceSpring1.add(baseTextField);

		baseTextField = new JTextField(5); //@salient
		influenceSpring1.add(new JLabel(mwclient.moneyOrFluMessage(false, true, -1) + " to refresh", SwingConstants.TRAILING));
		baseTextField.setToolTipText("The amount of " + mwclient.moneyOrFluMessage(false, true, -1) + " needed to refresh a factory. Disabled if set to zero.");
		baseTextField.setName("FluToRefreshFactory");
		influenceSpring1.add(baseTextField);

//		baseTextField = new JTextField(5);
//		influenceSpring1.add(new JLabel("Repod Cost:", SwingConstants.TRAILING));
//		baseTextField.setToolTipText("<html>Set to 0 to disable.<br>How much flu needed to repod omni mech<br>Random repods costs 1/2 this value</html>");
//		baseTextField.setName("FluToRepod");
//		influenceSpring1.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(influenceSpring1, 2);

        // then set up spring2
        baseTextField = new JTextField(5);
        influenceSpring2.add(new JLabel("Ceiling Penalty:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount removed from TotalArmies when an Army abutts the MaxBV");
        baseTextField.setName("CeilingPenalty");
        influenceSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        influenceSpring2.add(new JLabel("Overlap Penalty:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount removed from TotalArmies when 2 armies overlap");
        baseTextField.setName("OverlapPenalty");
        influenceSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        influenceSpring2.add(new JLabel(mwclient.moneyOrFluMessage(false, true, -1) + " Per Army:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Base amount of " + mwclient.moneyOrFluMessage(false, false, -1) + " given for each army");
        baseTextField.setName("BaseInfluence");
        influenceSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(influenceSpring2, 2);

        // springs are it for now. if CBoxes come later, stick them in the box
        // =)
        add(influenceBoxPanel);
	}

}
