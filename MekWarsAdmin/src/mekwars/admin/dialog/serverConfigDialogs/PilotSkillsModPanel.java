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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class PilotSkillsModPanel extends JPanel {

	private static final long serialVersionUID = 677126097682711286L;
	private PilotSkillTextField baseTextField = null;
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public PilotSkillsModPanel(MWClient mwclient) {
		super();

        
        /*
         * Pilot Skills Panel BV mods
         */

        JPanel SkillModSpring = new JPanel(new SpringLayout());

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("DM Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Flat BV Mod for Dodge Maneuver");
        baseTextField.setName("DodgeManeuverBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("MS Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Base BV Mod for Melee Specialist");
        baseTextField.setName("MeleeSpecialistBaseBVMod");
        SkillModSpring.add(baseTextField);
        
        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("Hatchet Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Base BV Mod per Hatchet/Sword<br> [(Base Increase)(unit tonage/10)]<br>+(hatchet mod * number of physical weapons)</html>");
        baseTextField.setName("HatchetRating");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("PR BV Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Base BV % multiplier for Pain Resistance. Use integer value.<br>Example: Use 5 to add 5% base bv to modified bv. Doubles if unit has CASE/CASE II.");
        baseTextField.setName("PainResistanceBaseBVMod");
        SkillModSpring.add(baseTextField);
        
        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("IM BV Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Base BV % multiplier for Iron Man. Use integer value.<br>Example: Use 5 to add 5% base bv to modified bv. Only adds BV if unit has CASE/CASE II. No cost if pilot has PR.");
        baseTextField.setName("IronManBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("MA BV Mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Maneuvering Ace Base BV mod<br>(\"Base Increase\")(\"Unit's top speed\"/\"Speed rating\")</html>");
        baseTextField.setName("ManeuveringAceBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("MA Speed Rating", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Maneuvering Ace Base BV mod<br>(\"Base Increase\")(\"Unit's top speed\"/\"Speed rating\")</html>");
        baseTextField.setName("ManeuveringAceSpeedRating");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("TG BV", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Flat BV amount added for Tactical Genius</html>");
        baseTextField.setName("TacticalGeniusBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("EI bv mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>BV mod added to the unit due to EI</html>");
        baseTextField.setName("EnhancedInterfaceBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("Edge bv mod", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>BV mod added to the unit due to Edge Skill</html>");
        baseTextField.setName("EdgeBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("Max Edge", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Max number of edges a pilot can have per game<br>This is akin to levels.</html>");
        baseTextField.setName("MaxEdgeChanges");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("VDNI", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>BV mod added to the unit due to VDNI.</html>");
        baseTextField.setName("VDNIBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("BVDNI", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>BV mod added to the unit due to Buffered VDNI.</html>");
        baseTextField.setName("BufferedVDNIBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>BV mod added to the unit due to Pain Shunt.</html>");
        baseTextField.setName("PainShuntBaseBVMod");
        SkillModSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(5);
        SkillModSpring.add(new JLabel("Gifted % Mod", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Note Double Field<br>The amount off the cost of other upgrades a Gifted Pilot gets.<br>Example .05 for 5% off</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Pilots receive an extra x% chance to gain a skill when they fail<br>to level Piloting or Gunnery after a win</body></html>");
        }
        baseTextField.setName("GiftedPercent");
        SkillModSpring.add(baseTextField);
        
        /**
         * New checkbox for TigerShark's contributed SpeedFactor
         */
        BaseCheckBox = new JCheckBox("MS SpeedRating");
        BaseCheckBox.setName("MeleeSpecialistUseSpeedFactor");
        BaseCheckBox.setToolTipText(
        		"<html>If checked, the MS BV mod will be calculated using the following formula:<br />"
        		+ "<br />"
        		+ "<b>&nbsp;&nbsp;&nbsp;&nbsp;Math.pow(1 + ((((double) unit.getRunMP() + (Math.round(Math.max(unit.getJumpMP(), unit.getActiveUMUCount()) / 2.0))) - 5) / 10), 1.2);<br />"
        		+ "&nbsp;&nbsp;&nbsp;&nbsp;double total = baseBV * ((tonnage / 10) * speedFactor) + (hatchetMod * numberOfHatchets);<br/>"
        		+ "</b><br />"
        		+ "If unchecked, the SpeedRating will be set to 1.0, effectively ignoring it.</html>");
        SkillModSpring.add(new JLabel(" "));
        SkillModSpring.add(BaseCheckBox);

        JPanel GunneryModPanel = new JPanel();
        
        GunneryModPanel.setLayout(new SpringLayout());
        
        BaseCheckBox = new JCheckBox("Flat G/B Mod");
        BaseCheckBox.setName("USEFLATGUNNERYBALLISTICMODIFIER");
        GunneryModPanel.add(BaseCheckBox);

        baseTextField = new PilotSkillTextField(5);
        baseTextField.setName("GunneryBallisticBaseBVMod");
        baseTextField.setToolTipText("BV Mod per Ballistic Weapon");
        GunneryModPanel.add(baseTextField);
        
        BaseCheckBox = new JCheckBox("Flat G/L Mod");
        BaseCheckBox.setName("USEFLATGUNNERYLASERMODIFIER");
        GunneryModPanel.add(BaseCheckBox);

        baseTextField = new PilotSkillTextField(5);
        baseTextField.setName("GunneryLaserBaseBVMod");
        baseTextField.setToolTipText("BV Mod per Laser Weapon");
        GunneryModPanel.add(baseTextField);
        
        BaseCheckBox = new JCheckBox("Flat G/M Mod");
        BaseCheckBox.setName("USEFLATGUNNERYMISSILEMODIFIER");
        GunneryModPanel.add(BaseCheckBox);

        baseTextField = new PilotSkillTextField(5);
        baseTextField.setName("GunneryMissileBaseBVMod");
        baseTextField.setToolTipText("BV Mod per Missile Weapon");
        GunneryModPanel.add(baseTextField);
        
        SpringLayoutHelper.setupSpringGrid(GunneryModPanel, 9);
        
        SpringLayoutHelper.setupSpringGrid(SkillModSpring, 10);

        setLayout(new VerticalLayout(10));
        
        add(SkillModSpring);
        add(GunneryModPanel);
        
        setBorder(BorderFactory.createTitledBorder("BV Mods"));
	}

}