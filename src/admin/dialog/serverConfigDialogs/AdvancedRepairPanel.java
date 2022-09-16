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
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class AdvancedRepairPanel extends JPanel {

	private static final long serialVersionUID = -8614798115843988091L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	private JRadioButton baseRadioButton = new JRadioButton();
    
	public AdvancedRepairPanel() {
		super();
        /*
         * ADVANCE REPAIR setup
         */
        JPanel masterBox = new JPanel();
        JPanel repairSpring = new JPanel(new SpringLayout());
        JPanel repairSpring2 = new JPanel(new SpringLayout());

        masterBox.setLayout(new BoxLayout(masterBox, BoxLayout.Y_AXIS));

        ButtonGroup repairTypes = new ButtonGroup();

        baseRadioButton = new JRadioButton("Use Techs");

        baseRadioButton.setToolTipText("<html>Use Techs as bays<br>NOTE: Save all player files and reboot<br>When turning on or off.</html>");
        baseRadioButton.setName("UseTechRepair");
        repairTypes.add(baseRadioButton);
        repairSpring.add(baseRadioButton);

        baseRadioButton = new JRadioButton("Use Statistcal Repair");

        baseRadioButton.setToolTipText("<html>Units Get damaged but they are repair all at once if the player chooses so.<br>NOTE: Save all player files and reboot<br>When turning on or off.</html>");
        baseRadioButton.setName("UseSimpleRepair");
        baseRadioButton.setSelected(true);
        repairTypes.add(baseRadioButton);
        repairSpring.add(baseRadioButton);

        baseRadioButton = new JRadioButton("Use Advanced Repair");

        baseRadioButton.setToolTipText("<html>Use Advanced Repair?<br>NOTE: Save all player files and reboot<br>When turning on or off.</html>");
        baseRadioButton.setName("UseAdvanceRepair");
        repairTypes.add(baseRadioButton);
        repairSpring.add(baseRadioButton);

        BaseCheckBox = new JCheckBox("Allow Reg Techs To Be Hired");

        BaseCheckBox.setToolTipText("Allow players to hire reg techs");
        BaseCheckBox.setName("AllowRegTechsToBeHired");
        repairSpring.add(BaseCheckBox);

        // Allow players to donate and sell damaged units.
        BaseCheckBox = new JCheckBox("Allow Selling Of Damaged Units");

        BaseCheckBox.setToolTipText("Allow players to sell damaged units on the BM");
        BaseCheckBox.setEnabled(false);
        BaseCheckBox.setName("AllowSellingOfDamagedUnits");
        repairSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Donating Of Damaged Units");

        BaseCheckBox.setToolTipText("Allow players to donate damaged units to their factions");
        BaseCheckBox.setName("AllowDonatingOfDamagedUnits");
        repairSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Parts For Repairs");

        BaseCheckBox.setToolTipText("Parts are pulled from the players cache to use for repairs.");
        BaseCheckBox.setName("UsePartsRepair");
        repairSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Non-Faction Units cost extra techs");
        BaseCheckBox.setToolTipText("Only used with Tech Repairs.  Increases the tech cost of non-faction units.");
        BaseCheckBox.setName("UseNonFactionUnitsIncreasedTechs");
        repairSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Do not allow salvage of undamaged units");
        BaseCheckBox.setToolTipText("Only used with Tech Repairs.  Players may not salvage undamaged units");
        BaseCheckBox.setName("DisallowFreshUnitSalvage");
        repairSpring.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(repairSpring, 3);

        // The base cost to hire a tech.

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Green Tech Hire Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Cost to hire 1 green tech");
        baseTextField.setName("GreenTechHireCost");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Reg Tech Hire Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Cost to hire 1 reg tech");
        baseTextField.setName("RegTechHireCost");
        repairSpring2.add(baseTextField);

        // The base cost for each of these techs to do a job.
        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Green Tech Pay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The base cost for a green tech to do a repair per crit.<br>Cost is doubled for the first crit.<br>i.e. if set to 1 it would cost 4 for 3 crits.</html>");
        baseTextField.setName("GreenTechRepairCost");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Reg Tech Pay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The base cost for a reg tech to do a repair per crit.<br>Cost is doubled for the first crit.<br>i.e. if set to 1 it would cost 4 for 3 crits.</html>");
        baseTextField.setName("RegTechRepairCost");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Vet Tech Pay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The base cost for a vet tech to do a repair per crit.<br>Cost is doubled for the first crit.<br>i.e. if set to 1 it would cost 4 for 3 crits.</html>");
        baseTextField.setName("VetTechRepairCost");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Elite Tech Pay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The base cost for a elite tech to do a repair per crit.<br>Cost is doubled for the first crit.<br>i.e. if set to 1 it would cost 4 for 3 crits.</html>");
        baseTextField.setName("EliteTechRepairCost");
        repairSpring2.add(baseTextField);

        // Hanger buy and sell back costs.
        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Bay Lease Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Security deposit for a new bay");
        baseTextField.setName("CostToBuyNewBay");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Bay Deposit Return:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of the Deposit a player gets back when returning a bay");
        baseTextField.setName("BaySellBackPrice");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Max Bays to Lease:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The Maximum number of bays a player can buy<br>Set to -1 for unlimited.<br>A player with more then the max will lose all the bays above the max.</html>");
        baseTextField.setName("MaxBaysToBuy");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Time for Each Repair:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>amount of time, in seconds, that it takes to repair each damaged crit<br>Note:When repairing engines all crits are counted<br>no matter how many are damaged</html>");
        baseTextField.setName("TimeForEachRepairPoint");
        repairSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        repairSpring2.add(new JLabel("Chance Tech Dies:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The chance out of 100 that a tech dies when a 2 is rolled on a repair roll</html>");
        baseTextField.setName("ChanceTechDiesOnFailedRepair");
        repairSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(repairSpring2, 6);

        JPanel masterPanel = new JPanel();

        JPanel masterArmorPanel = new JPanel();
        JPanel armorPanel = new JPanel(new SpringLayout());

        JPanel masterInternalPanel = new JPanel();
        JPanel internalPanel = new JPanel(new SpringLayout());

        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.X_AXIS));
        masterArmorPanel.setLayout(new BoxLayout(masterArmorPanel, BoxLayout.Y_AXIS));
        masterInternalPanel.setLayout(new BoxLayout(masterInternalPanel, BoxLayout.Y_AXIS));

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Standard:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of standard armor.<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointStandard");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("FF:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of Ferro-Fibrous armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointFF");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Reactive:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of reactive armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointReactive");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Reflective:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of reflective armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointReflective");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Hardened:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of hardened armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointHardened");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Light FF:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of light FF armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointLFF");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Heavy FF:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of heavy FF armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointHFF");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Patchwork:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of patchwork armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointPatchwork");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("Stealth:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of stealth armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointStealth");
        armorPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        armorPanel.add(new JLabel("FF Proto:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of Ferro-Fibrous Prototype armor..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointFFProto");
        armorPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(armorPanel, 6);

        masterArmorPanel.add(new JLabel("Armor"));
        masterArmorPanel.add(armorPanel);

        baseTextField = new JTextField(5);
        internalPanel.add(new JLabel("Standard:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of standard internal..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointStandardIS");
        internalPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        internalPanel.add(new JLabel("Endo:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of endo internal..<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointEndoIS");
        internalPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        internalPanel.add(new JLabel("Endo Proto:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of endo prototype internal.<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointEndoProtoIS");
        internalPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        internalPanel.add(new JLabel("Reinforced:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of reinforced internal.<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointReinforcedIS");
        internalPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        internalPanel.add(new JLabel("Composite:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair 1 point of composite internal.<br>Note This is a double field!</html>");
        baseTextField.setName("CostPointCompositeIS");
        internalPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(internalPanel, 4);

        masterInternalPanel.add(new JLabel("Internal"));
        masterInternalPanel.add(internalPanel);

        masterPanel.add(masterArmorPanel);
        masterPanel.add(masterInternalPanel);

        JPanel masterEquipmentPanel = new JPanel();
        JPanel equipmentTextPanel = new JPanel();
        JPanel equipmentPanel = new JPanel(new SpringLayout());
        JPanel replacementTextPanel = new JPanel();
        JPanel replacementPanel = new JPanel(new SpringLayout());

        masterEquipmentPanel.setLayout(new BoxLayout(masterEquipmentPanel, BoxLayout.Y_AXIS));

        equipmentTextPanel.add(new JLabel("Critical Slot Repair Costs"));

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("System:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<Html>Cost to repair each system crit.<br>Note Double Field</html>");
        baseTextField.setName("SystemCritRepairCost");
        equipmentPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("Equipment:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair each misc equipment crit, i.e. heat sinks, actuators, ammo bins<br>Note Double Field</html>");
        baseTextField.setName("EquipmentCritRepairCost");
        equipmentPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("Engine:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair each engine crit.<br>Note when repairing engines all crits are counted<br>I.E. XL engines will be more expensive then Standard<br>Note Double Field</html>");
        baseTextField.setName("EngineCritRepairCost");
        equipmentPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("Ballistic:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair each ballistic weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("BallisticCritRepairCost");
        equipmentPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("Energy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair each energy weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("EnergyWeaponCritRepairCost");
        equipmentPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        equipmentPanel.add(new JLabel("Missle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to repair each missle weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("MissileCritRepairCost");
        equipmentPanel.add(baseTextField);
        SpringLayoutHelper.setupSpringGrid(equipmentPanel, 6);

        replacementTextPanel.add(new JLabel("Critical Slot Replacement Costs"));

        baseTextField = new JTextField(5);
        replacementPanel.add(new JLabel("System:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<Html>Cost to replace each system crit.<br>Note Double Field</html>");
        baseTextField.setName("SystemCritReplaceCost");
        replacementPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        replacementPanel.add(new JLabel("Equipment:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to replace each misc replacement crit, i.e. heat sinks, actuators, ammo bins<br>Note Double Field</html>");
        baseTextField.setName("EquipmentCritReplaceCost");
        replacementPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        replacementPanel.add(new JLabel("Ballistic:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to replace each ballistic weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("BallisticCritReplaceCost");
        replacementPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        replacementPanel.add(new JLabel("Energy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to replace each energy weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("EnergyWeaponCritReplaceCost");
        replacementPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        replacementPanel.add(new JLabel("Missle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost to replace each missle weapon crit.<br>Note Double Field</html>");
        baseTextField.setName("MissileCritReplaceCost");
        replacementPanel.add(baseTextField);
        SpringLayoutHelper.setupSpringGrid(replacementPanel, 6);

        masterEquipmentPanel.add(equipmentTextPanel);
        masterEquipmentPanel.add(equipmentPanel);
        masterEquipmentPanel.add(replacementTextPanel);
        masterEquipmentPanel.add(replacementPanel);

        JPanel masterCostModPanel = new JPanel();
        JPanel CostModTextPanel = new JPanel();
        JPanel CostModPanel = new JPanel(new SpringLayout());

        masterCostModPanel.setLayout(new BoxLayout(masterCostModPanel, BoxLayout.Y_AXIS));

        CostModTextPanel.add(new JLabel("Cost Mods For Buying Damaged Unit"));

        baseTextField = new JTextField(5);
        CostModPanel.add(new JLabel("Armor:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<Html>Cost Modifier to buy a used unit with armor damage<br>Note Double Field</html>");
        baseTextField.setName("CostModifierToBuyArmorDamagedUnit");
        CostModPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        CostModPanel.add(new JLabel("Crit:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost Modifier to buy a used unit with damaged crits<br>Note Double Field</html>");
        baseTextField.setName("CostModifierToBuyCritDamagedUnit");
        CostModPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        CostModPanel.add(new JLabel("Engined:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Cost Modifier to buy a used unit that has been engined.<br>Note Double Field</html>");
        baseTextField.setName("CostModifierToBuyEnginedUnit");
        CostModPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(CostModPanel, 6);

        masterCostModPanel.add(CostModTextPanel);
        masterCostModPanel.add(CostModPanel);

        masterBox.add(repairSpring);
        masterBox.add(repairSpring2);
        masterBox.add(masterPanel);
        masterBox.add(masterEquipmentPanel);
        masterBox.add(masterCostModPanel);

        add(masterBox);
	}

}