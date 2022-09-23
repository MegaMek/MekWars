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
 * @author Spork
 * @author jtighe
 */
public class CombatPanel extends JPanel {
	
	private static final long serialVersionUID = 1556861707519790557L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
    
	public CombatPanel() {
		super();
        /*
         * COMBAT Panel Setup
         */
        JPanel combatBox = new JPanel();
        combatBox.setLayout(new BoxLayout(combatBox, BoxLayout.Y_AXIS));
        JPanel combatCBoxGrid = new JPanel(new GridLayout(4, 3));

        JPanel combatSpring1 = new JPanel(new SpringLayout());
        JPanel combatSpring2 = new JPanel(new SpringLayout());
        JPanel combatSpring3 = new JPanel(new SpringLayout());
        JPanel combatSpring4 = new JPanel(new SpringLayout());
        JPanel combatMMOptionsSpring = new JPanel(new SpringLayout());

        JPanel combatSpringFlow = new JPanel();

        combatSpringFlow.add(combatSpring1);
        combatSpringFlow.add(combatSpring2);
        combatSpringFlow.add(combatSpring3);
        combatSpringFlow.add(combatSpring4);

        BaseCheckBox = new JCheckBox("Probe In Reserve");

        BaseCheckBox.setToolTipText("Allow /c ca in reserve mode?");
        BaseCheckBox.setName("ProbeInReserve");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Real_Blind_Drops");

        BaseCheckBox.setToolTipText("<HTML>Check in order to use real_blind_drop option in MM,<br> hiding units from players until they appear on the map.<br>If this option is enabled, /c tasks and join messages will not show army composition.</HTML>");
        BaseCheckBox.setName("UseBlindDrops");
        combatCBoxGrid.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Show unit type counts");
        BaseCheckBox.setName("ShowUnitTypeCounts");
        BaseCheckBox.setToolTipText("<HTML>If checked, unit type totals will be<br>shown in the attack / defend notifications <br>in blind operations</HTML>");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Selectable Salvage");
        BaseCheckBox.setToolTipText("If set to true then players can recoup repair costs by scrapping salvaged units");

        BaseCheckBox.setName("SelectableSalvage");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Force Salvage");

        BaseCheckBox.setToolTipText("Count Mechs without a leg or 2 gyro hits as salvage?");
        BaseCheckBox.setName("ForceSalvage");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Static Maps");

        BaseCheckBox.setToolTipText("Use Already built maps vs terrain and RMG");
        BaseCheckBox.setName("UseStaticMaps");
        combatCBoxGrid.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Disable Weather");
        BaseCheckBox.setToolTipText("Disable all weather conditions");
        BaseCheckBox.setName("DisableWeather");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Show Inf In /c ca");

        BaseCheckBox.setName("ShowInfInCheckAttack");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Limiters");

        BaseCheckBox.setName("AllowLimiters");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Count Inf For Limits");

        BaseCheckBox.setName("CountInfForLimiters");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Unit Ratios");

        BaseCheckBox.setToolTipText("If checked ratios will be followed otherwise anything goes.");
        BaseCheckBox.setName("AllowRatios");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Prelim Op Report");

        BaseCheckBox.setToolTipText("<html>Check this to allow the players a chance<br>of receiving prelim data on a task they've accepted</html>");
        BaseCheckBox.setName("AllowPreliminaryOperationsReports");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Display Op Name");

        BaseCheckBox.setToolTipText("Display the Op name to the defender");
        BaseCheckBox.setName("DisplayOperationName");
        combatCBoxGrid.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Force Deactivate Players");

        BaseCheckBox.setToolTipText("<html>If this is checked then after combat players are<br>automatically deactivated immunity time is also ignored.</html>");
        BaseCheckBox.setName("ForcedDeactivation");
        combatCBoxGrid.add(BaseCheckBox);

        // spring1. 6 elements.
        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Upper Limit Buffer"));
        baseTextField.setToolTipText("<HTML>" + "Min Buffer On Upper Limiter. For example, a<br>" + "setting of 2 would prevent a player with a 4<br>" + "unit army from setting a limiter of 4 or 5.");
        baseTextField.setName("UpperLimitBuffer");
        combatSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Lower Limit Buffer"));
        baseTextField.setToolTipText("<HTML>" + "Min Buffer On Lower Limiter. For example, a<br>" + "setting of 2 would prevent a player with a 7<br>" + "unit army from setting a limiter of 5 or 6.");
        baseTextField.setName("LowerLimitBuffer");
        combatSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Default Upper Limit"));
        baseTextField.setName("DefaultUpperLimit");
        combatSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Default Lower Limit"));
        baseTextField.setName("DefaultLowerLimit");
        combatSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Mek to Inf Ratio:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html><body>Only Used if AllowRatios is checked<br>Set the %Ratio for Infantry to Mek if set at 50% 1 Infantry to every 2 Meks<br>If set at 200% 2 infantry to 1 Mek is allowed</body></html>");
        baseTextField.setName("MekToInfantryRatio");
        combatSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring1.add(new JLabel("Chance For Op Report:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The min chance a player will receive info on the planet<br>base chance is based on players factions<br>owner ship of the planet<br>if that is lower then this number this number<br>will be used</htlm>");
        baseTextField.setName("MinChanceForAccurateOperationsReports");
        combatSpring1.add(baseTextField);

        // spring2. 6 elements.
        baseTextField = new JTextField(5);
        combatSpring2.add(new JLabel("Immunity Time", SwingConstants.TRAILING));
        baseTextField.setToolTipText("How long shall a player be immune to attacks after he just finished a task? (in seconds)");
        baseTextField.setName("ImmunityTime");
        combatSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring2.add(new JLabel("Mek Map Factor", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        baseTextField.setName("MekMapSizeFactor");
        combatSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        combatSpring2.add(new JLabel("Veh Map Factor", SwingConstants.TRAILING));
        baseTextField.setName("VehicleMapSizeFactor");
        combatSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        combatSpring2.add(new JLabel("Inf Map Factor", SwingConstants.TRAILING));
        baseTextField.setName("InfantryMapSizeFactor");
        combatSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        combatSpring2.add(new JLabel("Aero Map Factor", SwingConstants.TRAILING));
        baseTextField.setName("AeroMapSizeFactor");
        combatSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring2.add(new JLabel("Game Log Name", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Name of the game log to save to the users system");
        baseTextField.setName("MMGameLogName");
        combatSpring2.add(baseTextField);

        // spring3. 5 elements.
        baseTextField = new JTextField(5);
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        combatSpring3.add(new JLabel("Proto Map Factor", SwingConstants.TRAILING));
        baseTextField.setName("ProtoMekMapSizeFactor");
        combatSpring3.add(baseTextField);

        baseTextField = new JTextField(5);
        baseTextField.setToolTipText("Map Size Factors (Those determine how big a map will be)");
        combatSpring3.add(new JLabel("BA Map Factor", SwingConstants.TRAILING));
        baseTextField.setName("BattleArmorMapSizeFactor");
        combatSpring3.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring3.add(new JLabel("Fast Hover Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Add X BV to all Hovers with 8/12 and more");
        baseTextField.setName("FastHoverBVMod");
        combatSpring3.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring3.add(new JLabel("Salvage Scrap Time", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Time, in seconds, that players have<br>" + "after game to scrap units w/o charge.</html>");
        baseTextField.setName("TimeToSelectSalvage");
        combatSpring3.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring3.add(new JLabel("Mek to Vehicle Ratio:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html><body>Only Used if AllowRatios is checked<br>Set the %Ratio for vehicle to Mek if set at 50% 1 vehcile to every 2 Meks<br>If set at 200% 2 Vehicles to 1 Mek is allowed</body></html>");
        baseTextField.setName("MekToVehicleRatio");
        combatSpring3.add(baseTextField);

        // pack the springs.
        SpringLayoutHelper.setupSpringGrid(combatSpring1, 2);
        SpringLayoutHelper.setupSpringGrid(combatSpring2, 2);
        SpringLayoutHelper.setupSpringGrid(combatSpring3, 2);

        BaseCheckBox = new JCheckBox("Show Unit Id?");

        BaseCheckBox.setToolTipText("Unit ID are displayed to help ID units");
        BaseCheckBox.setName("MMShowUnitId");
        combatMMOptionsSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Time Stamp Save Games?");

        BaseCheckBox.setToolTipText("All Save Games will have a timestamp on them");
        BaseCheckBox.setName("MMTimeStampLogFile");
        combatMMOptionsSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Keep Game Log?");

        BaseCheckBox.setToolTipText("Save game log to users system");
        BaseCheckBox.setName("MMKeepGameLog");
        combatMMOptionsSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow partial bins?");

        BaseCheckBox.setToolTipText("<html>Allow units in any army<br>to go active if they have partially full ammobins.</html>");
        BaseCheckBox.setName("AllowUnitsToActivateWithPartialBins");
        combatMMOptionsSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Allow damaged units to activate?");
        BaseCheckBox.setToolTipText("<html>Allow units in an army<br>to go active if they are damaged.</html>");
        BaseCheckBox.setName("AllowActivationWithDamagedUnits");
        combatMMOptionsSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Require attack-capable armies?");
        BaseCheckBox.setName("RequireAttackCapableArmiesForActivation");
        BaseCheckBox.setToolTipText("<html>Require all armies to be attack capable in order or disallow activation.</html>");
        combatMMOptionsSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Ignore pilots for BV calcs");
        BaseCheckBox.setName("UseBaseBVForMatching");
        BaseCheckBox.setToolTipText("<html>Checking this will always use a base 4/5 pilot for BV calcs</html>");
        combatMMOptionsSpring.add(BaseCheckBox);
        
        SpringLayoutHelper.setupSpringGrid(combatMMOptionsSpring, 3);

        BaseCheckBox = new JCheckBox("Allow Attacks From Reserve?");

        BaseCheckBox.setToolTipText("Allows players to arrange games and attack while in reserve");
        BaseCheckBox.setName("AllowAttackFromReserve");
        combatSpring4.add(BaseCheckBox);

        baseTextField = new JTextField(5);
        combatSpring4.add(new JLabel("Response Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The amount of time, in minutes, that the<br>defending player has to respond before the offer expires</html>");
        baseTextField.setName("AttackFromReserveResponseTime");
        combatSpring4.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring4.add(new JLabel("Wait Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>How long, in minutes, the attacking player<br>has to wait before they can attack again from reserve.</html>");
        baseTextField.setName("AttackFromReserveSleepTime");
        combatSpring4.add(baseTextField);

        baseTextField = new JTextField(5);
        combatSpring4.add(new JLabel("Max Negative Bays:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>How many negative bays a player may have<br>to engage in AFR.  Set to -1 to disable check");
        baseTextField.setName("MaxNegativeBaysForAFR");
        combatSpring4.add(baseTextField);
        
        baseTextField = new JTextField(5);
        combatSpring4.add(new JLabel("Max Negative Bays for activation:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>How many negative bays a player may have<br>to go active.  Set to -1 to disable check");
        baseTextField.setName("MaxNegativeBaysForActivation");
        combatSpring4.add(baseTextField);
        
        
        //SpringLayoutHelper.setupSpringGrid(combatSpring4, 5);
        
        SpringLayoutHelper.setupSpringGrid(combatSpring4, 7);

        // finalize layout
        combatBox.add(combatCBoxGrid);
        combatBox.add(combatSpringFlow);
        combatBox.add(combatMMOptionsSpring);
        combatBox.add(combatSpring4);
        add(combatBox);
	}
}
