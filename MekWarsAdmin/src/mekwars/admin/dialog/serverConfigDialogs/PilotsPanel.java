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
 */
public class PilotsPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5196079223646097482L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public PilotsPanel(MWClient mwclient) {
		super();
        /*
         * Pilots options panel
         */
        JPanel pilotCBoxGrid = new JPanel(new GridLayout(4, 4));
        JPanel pilotOptionsSpring1 = new JPanel(new SpringLayout());
        JPanel pilotOptionsSpring2 = new JPanel(new SpringLayout());

        // pilotSpring1, 8 elements
        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Skill Change:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("% chance for a new pilot to have a maxtech skill");
        baseTextField.setName("BornSkillChance");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Skill Gain:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>% chance for a pilot to get a skill<br>instead of a gunnery/piloting upgrade</html>");
        baseTextField.setName("SkillLevelChance");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("XP Loss:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>% chance for a pilot to lose<br>accumulated XP in the Queue</html>");
        baseTextField.setName("ClearXPInQue");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Cost For Mek Pilot:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Cost to buy a new Mek pilot from the faction pools<HTML>");
        baseTextField.setName("CostToBuyNewPilot");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Max Pilots From House:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cap for pilots in a players personal queue<br>if they have less they can purchase<br>from the faction pools<br>if Allow Players to Buy<br>with full Queues is checked");
        baseTextField.setName("MaxAllowedPilotsInQueueToBuyFromHouse");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Base Pilot Survival:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Base Survival Rate for an ejected pilot<br>If the %planet control is less then this this<br>amount is used.</html>");
        baseTextField.setName("BasePilotSurvival");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Trapped In Mech Survival:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Unique to in-mech pilots (engine kills). Penalty" + "<br>for being in a stationary unit when the capture" + "<br>crews come around and sweep the field.</html>");
        baseTextField.setName("TrappedInMechSurvivalMod");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Convert Pilots:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>% Chance that captured pilots are converted<br>and sent to faction/player pools</html>");
        baseTextField.setName("ChanceToConvertCapturedPilots");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Damage Per Hit:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Amount of damage the pilot will take per hit they receive in game<br>NOTE: This amount will be translated back into CBT hits<br>When sent back to the clients.</html>");
        baseTextField.setName("AmountOfDamagePerPilotHit");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Cost For Proto Pilot:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Cost to buy a new Proto pilot from the faction pools<HTML>");
        baseTextField.setName("CostToBuyNewProtoPilot");
        pilotOptionsSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring1.add(new JLabel("Pilot Skil Sell Back Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Sets what percent of the original cost the pilot gets back in exp<br>when a skill is sold.<br>NOTE: This is a double filed .5 = 50%</html>");
        baseTextField.setName("PilotUpgradeSellBackPercent");
        pilotOptionsSpring1.add(baseTextField);

        // PilotSpring2 - 8 elements
        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Total Skill to Retire:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Total skill (Piloting + Gunnery) of pilot must be equal to or less than this number in order to retire for free.</html>");
        baseTextField.setName("TotalSkillForFreeRetirement");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Early Retire Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + mwclient.moneyOrFluMessage(true, true, -1) + " cost PER LEVEL to retire a pilot before free. For<br>" + "example, if Skill to Retire is 6, a pilot is 4/5 (Total:9)<br>" + "and the cost is 10, it will cost (9-6)*10=30 " + mwclient.moneyOrFluMessage(true, true, -1) + " to<br>" + "retire the 4/5.</html>");
        baseTextField.setName("CostPerLevelToRetireEarly");
        pilotOptionsSpring2.add(baseTextField);
        
        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Retired Pilot Takes Mech Chance:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html> Chance a retiring pilot takes his unit with him.</html>");
        baseTextField.setName("RetiredPilotTakesMechChance");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Best Gunnery:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Best Gunnery Skill allowed.");
        baseTextField.setName("BestGunnerySkill");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Best Piloting", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Best Piloting skill allowed.");
        baseTextField.setName("BestPilotingSkill");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Best Total Pilot", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Lowest skill total (Gunnery + Piloting = Total) allowed.");
        baseTextField.setName("BestTotalPilot");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Base level Up Roll", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Basic 1dX required used for level up. If roll<br>" + "is less than pilot XP, pilot gains a level.</html>");
        baseTextField.setName("BaseRollToLevel");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Roll Multiplier", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Base * Multiplier * (10 - total skill). If Base is 1000, and<br>" + "multiplier is 2, and skill is 3/4 (7), pilot will need to roll<br>" + "lower than his XP on 1d6000 (1000Base * 2Multi * 3Levels = 6000).</html>");
        baseTextField.setName("MultiplierPerPreviousLevel");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Health per Tick", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The number of points a pilot will heal in one tick<br>NOTE: with PPQ on pilots must be in the queue to heal<br>With PPQ off pilots will heal while in their units.</html>");
        baseTextField.setName("PilotAmountHealedPerTick");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("MedTech per Tick", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The number of points a pilot will heal in one tick if they have the medtech skill<br>NOTE: with PPQ on pilots must be in the queue to heal<br>With PPQ off pilots will heal while in their units.</html>");
        baseTextField.setName("MedTechAmountHealedPerTick");
        pilotOptionsSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        pilotOptionsSpring2.add(new JLabel("Max Pilot Upgrades:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Set the maximum numbers of skills a player can give a pilot.<br>Set to -1 for unlimited.</html>");
        baseTextField.setName("MaxPilotUpgrades");
        pilotOptionsSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(pilotOptionsSpring1, 2);
        SpringLayoutHelper.setupSpringGrid(pilotOptionsSpring2, 2);

        // pilot cboxes
        BaseCheckBox = new JCheckBox("Elite BV Mod");
        BaseCheckBox.setToolTipText("Increase BV of units which are <2/X or X/<2 above FASA levels.");
        BaseCheckBox.setName("ElitePilotsBVMod");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("MaxTech Skills");
        BaseCheckBox.setName("PilotSkills");
        BaseCheckBox.setToolTipText("Allow MaxTech pilot skills (Manuv. Ace, Pain Resist, etc)");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Unlevel@Queue");
        BaseCheckBox.setToolTipText("<HTML>" + "Unchecking allows Pilots to keep skills and XP in queue<br>" + "after their rides die.</HTML>");
        BaseCheckBox.setName("ReduceSkillsInQue");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Green Pilots");
        BaseCheckBox.setToolTipText("Check in order to allow green pilots. 4/6, 5/5, etc.");
        BaseCheckBox.setName("AllowGreenPilots");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Vet Pilots");
        BaseCheckBox.setToolTipText("Check in order to allow vet pilots. 3/5, 4/4, etc.");
        BaseCheckBox.setName("AllowVetPilots");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow PPQ");
        BaseCheckBox.setToolTipText("<HTML>Allow Personal Pilot Queues<br>Players are allowed to keep their own pilots instead of them going to the faction pools</HTML>");
        BaseCheckBox.setName("AllowPersonalPilotQueues");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Extra Pilots");
        BaseCheckBox.setToolTipText("<HTML>When checked the players can buy<br>pilots from the faction pool<br>even if they already have pilots of that<br>type/class in their pools</HTML>");
        BaseCheckBox.setName("AllowPlayerToBuyPilotsFromHouseWhenPoolIsFull");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Downed Pilots Roll");
        BaseCheckBox.setToolTipText("<HTML>When checked a downed pilot must make a survival roll<br>to see if they make it home<br>or are captured</HTML>");
        BaseCheckBox.setName("DownPilotsMustRollForSurvival");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Retirement");
        BaseCheckBox.setToolTipText("Allow players to retire their pilots.");
        BaseCheckBox.setName("PilotRetirementAllowed");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Early Retirement");
        BaseCheckBox.setToolTipText("Allow players to pay a fee in order to retire their pilots earlier than normal.");
        BaseCheckBox.setName("EarlyRetirementAllowed");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Elite Retirements");
        BaseCheckBox.setToolTipText("<html>Randomly retire elite pilots who can't level any more. Rolls to retire are<br>" + "against the same target as their final level up. This automated retirement is separate<br>" + "from player-initiated retirement and will work even if \"Allow Retirement\" is disabled.</html>");
        BaseCheckBox.setName("RandomRetirementOfElites");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Common Names Only");
        BaseCheckBox.setToolTipText("Pilot names are only pulled from the Pilotnames.txt");
        BaseCheckBox.setName("UseCommonPilotNameFileOnly");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Random Pilot Levels");
        BaseCheckBox.setToolTipText("<html>" + "Disable to use RPG style pilot levelling. Pilots must gain<br>" + "Base * Multiplier * (10-Skill) XP to reach next level.<br>" + "Random roll to level up is removed - only raw XP is used.</html>");
        BaseCheckBox.setName("UseRandomPilotLevelups");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Pilot Damage Transfers");
        BaseCheckBox.setToolTipText("<html>If a pilot takes damage in a game it'll transfer back to the campaign<br>and the pilot will need to heal up.</html>");
        BaseCheckBox.setName("AllowPilotDamageToTransfer");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Crews Stay With Units");
        BaseCheckBox.setToolTipText("<html>If Checked Crews stay with thier units after being donated.</html>");
        BaseCheckBox.setName("CrewsStayWithUnits");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("All Pilots Level");
        BaseCheckBox.setToolTipText("<html>If Checked Then even losing pilots will have a chance to level.</html>");
        BaseCheckBox.setName("LosingPilotsCheckToLevel");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Players Level Pilots");
        BaseCheckBox.setToolTipText("<html>If Checked Then pilots do not check for leveling after each Operation<br> instead they players can buy skills and attributes with the pilots exp.</html>");
        BaseCheckBox.setName("PlayersCanBuyPilotUpgrades");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Pilots Must level Evenly");
        BaseCheckBox.setToolTipText("<html>If Checked then players must level their pilots skills via stair step.<br>This means no more then 1 difference between gunnery and piloting<br>unless the Pilot has NAG or NAP.</html>");
        BaseCheckBox.setName("PilotsMustLevelEvenly");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Players Demote Pilots");
        BaseCheckBox.setToolTipText("<html>If Checked, as well as Players Level Pilots, Then players can sell back pilots skills.</html>");
        BaseCheckBox.setName("PlayersCanSellPilotUpgrades");
        pilotCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Asymmetric Levelling");
        BaseCheckBox.setToolTipText("<html>If checked, pilots will be able to level up asymmetrically (2/5, 1/5, 4/2, etc)</html>");
        BaseCheckBox.setName("AllowAsymmetricPilotLevels");
        pilotCBoxGrid.add(BaseCheckBox);
        
        
        
        // finalize the layout
        JPanel pilotBox = new JPanel(new SpringLayout());
        JPanel pilotFlow = new JPanel();
        pilotFlow.add(pilotOptionsSpring1);
        pilotFlow.add(pilotOptionsSpring2);
        pilotBox.add(pilotFlow);
        pilotBox.add(pilotCBoxGrid);

        SpringLayoutHelper.setupSpringGrid(pilotBox, 2, 1);

        add(pilotBox);
	}

}