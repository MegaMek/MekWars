/*
 * MekWars - Copyright (C) 2004
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package admin.dialog.serverConfigDialogs;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 *
 * Server Configuration Page. All new Server Options need to be added to this page or subPanels as well.
 */
public class RewardPanel extends JPanel {


	private static final long serialVersionUID = 3402880281077089882L;

	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();

    public RewardPanel(MWClient mwclient) {
		super();
        /*
         * REWARD MENU CONSTRUCTION
         */
        JPanel rewardBox = new JPanel();
        rewardBox.setLayout(new BoxLayout(rewardBox, BoxLayout.Y_AXIS));
        JPanel rewardCBoxGrid = new JPanel(new SpringLayout());
        JPanel rewardGrid = new JPanel(new GridLayout(1, 2));
        JPanel rewardSpring1 = new JPanel(new SpringLayout());
        JPanel rewardSpring2 = new JPanel(new SpringLayout());

        JLabel rewardAllowHeader = new JLabel("Allow rewards to be used for:");
        rewardAllowHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        BaseCheckBox = new JCheckBox("DISPLAY");

        BaseCheckBox.setToolTipText("If checked, reward levels are shown to players. RECOMMENDED.");
        BaseCheckBox.setName("ShowReward");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox(mwclient.moneyOrFluMessage(false, true, -1));

        BaseCheckBox.setToolTipText("Check to allow players to exchange RP for flu");
        BaseCheckBox.setName("AllowInfluenceForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

		// @Author Salient (mwosux@gmail.com) , Add RP for CBills
		BaseCheckBox = new JCheckBox(mwclient.moneyOrFluMessage(true, true, -1));

		BaseCheckBox.setToolTipText("Check to allow players to exchange RP for CBills");
		BaseCheckBox.setName("AllowCBillsForRewards");
		rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Techs");

        BaseCheckBox.setToolTipText("Check to allow players to exchange RP for techs");
        BaseCheckBox.setName("AllowTechsForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Units");

        BaseCheckBox.setToolTipText("Check to allow players to exchange RP for units");
        BaseCheckBox.setName("AllowUnitsForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Rares");

        BaseCheckBox.setToolTipText("Check to allow players to get RARE units with RP");
        BaseCheckBox.setName("AllowRareUnitsForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Repods");

        BaseCheckBox.setToolTipText("<html>Check to allow players to repod units with RP<br>This allows a player to repod a unit<br>even if its not on their build table<br>Random repod options based<br>on the random repod settings</html>");
        BaseCheckBox.setName("GlobalRepodAllowed");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Refresh");

        BaseCheckBox.setToolTipText("Check to allow players to refresh factories with RP");
        BaseCheckBox.setName("AllowFactoryRefreshForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Repairs");

        BaseCheckBox.setToolTipText("Check to allow players to repair units with RP");
        BaseCheckBox.setName("AllowRepairsForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Crit Repairs");
        BaseCheckBox.setToolTipText("Check to allow players to individual crits with RP");
        BaseCheckBox.setName("AllowCritRepairsForRewards");
        rewardCBoxGrid.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(rewardCBoxGrid, 4);

        // set up spring1
        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("Max Reward Points:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("RP Cap");
        baseTextField.setName("XPRewardCap");
        rewardSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("XP Rollover:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of XP that will trigger 1 RP to be given to player");
        baseTextField.setName("XPRollOverCap");
        rewardSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("Techs per " + mwclient.getserverConfigs("RPShortName"), SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of techs hired with 1 RP");
        baseTextField.setName("TechsForARewardPoint");
        rewardSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel(mwclient.moneyOrFluMessage(false, true, -1) + " per " + mwclient.getserverConfigs("RPShortName"), SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of flu given in exhcange for 1 RP");
        baseTextField.setName("InfluenceForARewardPoint");
        rewardSpring1.add(baseTextField);

		// @Author Salient (mwosux@gmail.com) , Add RP for CBills
		baseTextField = new JTextField(5);
		rewardSpring1.add(new JLabel(mwclient.moneyOrFluMessage(true, true, -1) + " per " + mwclient.getserverConfigs("RPShortName"), SwingConstants.TRAILING));
		baseTextField.setToolTipText("Amount of CBills given in exhcange for 1 RP");
		baseTextField.setName("CBillsForARewardPoint");
		rewardSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("Rare Multiplier:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Rares units cost [Normal RP]*[Rare Multiplier]");
        baseTextField.setName("RewardPointMultiplierForRare");
        rewardSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("NonHouse Multiplier:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("How much should the unit cost be multiplied by if not using faction build tables");
        baseTextField.setName("RewardPointNonHouseMultiplier");
        rewardSpring1.add(baseTextField);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseAdvanceRepair"))) {
            baseTextField = new JTextField(5);
            rewardSpring1.add(new JLabel("RP to buy Green Tech:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("RP to buy 1 green tech.");
            baseTextField.setName("RewardPointsForGreen");
            rewardSpring1.add(baseTextField);

            baseTextField = new JTextField(5);
            rewardSpring1.add(new JLabel("RP to Buy Vet Tech:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("RP to buy 1 vet tech.");
            baseTextField.setName("RewardPointsForVet");
            rewardSpring1.add(baseTextField);

            baseTextField = new JTextField(5);
            rewardSpring1.add(new JLabel("RP to Repair a crit:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<html>RP to repair 1 crit.<br>NOTE: this is a double field!</html>");
            baseTextField.setName("RewardPointsForCritRepair");
            rewardSpring1.add(baseTextField);

        }

        baseTextField = new JTextField(5);
        rewardSpring1.add(new JLabel("Repod Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>How much should the repod cost<br>Random repods cost 1/2 this</html>");
        baseTextField.setName("GlobalRepodWithRPCost");
        rewardSpring1.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(rewardSpring1, 2);

        // set up spring2
        baseTextField = new JTextField(5);
        rewardSpring2.add(new JLabel("RP to refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("RP to refresh a factions factory.");
        baseTextField.setName("RewardPointToRefreshFactory");
        rewardSpring2.add(baseTextField);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UseAdvanceRepair"))) {
            baseTextField = new JTextField(5);
            rewardSpring2.add(new JLabel("RP to buy Reg Tech:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("RP to buy 1 reg tech.");
            baseTextField.setName("RewardPointsForReg");
            rewardSpring2.add(baseTextField);

            baseTextField = new JTextField(5);
            rewardSpring2.add(new JLabel("RP to Buy Elite Tech:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("RP to buy 1 elite tech.");
            baseTextField.setName("RewardPointsForElite");
            rewardSpring2.add(baseTextField);

            baseTextField = new JTextField(5);
            rewardSpring2.add(new JLabel("RP to Repair a unit:", SwingConstants.TRAILING));
            baseTextField.setToolTipText("RP to repair 1 unit.");
            baseTextField.setName("RewardPointsForRepair");
            rewardSpring2.add(baseTextField);

        }

        baseTextField = new JTextField(5);
        rewardSpring2.add(new JLabel("Rewards Repod Folder:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Folder to pull repod data from for use with rewards");
        baseTextField.setName("RewardsRepodFolder");
        rewardSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        rewardSpring2.add(new JLabel("Rewards Rare Build Table:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Build table that will be used in the rewards folder");
        baseTextField.setName("RewardsRareBuildTable");
        rewardSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(rewardSpring2, 2);

        // finalize the layout
        rewardGrid.add(rewardSpring1);
        rewardGrid.add(rewardSpring2);
        rewardBox.add(rewardAllowHeader);
        rewardBox.add(rewardCBoxGrid);
        rewardBox.add(rewardGrid);
        add(rewardBox);
	}
}
