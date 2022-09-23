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
 * @author jtighe
 * @author Spork
 */
public class ProductionPanel extends JPanel {

    /**
	 * 
	 */
	private static final long serialVersionUID = 9048716063514829354L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
    public ProductionPanel() {
		super();
        /*
         * PRODUCTION/FACTORY PANEL CONSTRUCTION
         */
        JPanel refreshSpringPanel = new JPanel(new SpringLayout());
        JPanel salesSpringPanel = new JPanel(new SpringLayout());
        JPanel apSpringPanel = new JPanel(new SpringLayout());
        JPanel prodMiscPanel = new JPanel(new SpringLayout());
        JPanel prodCBoxSpring = new JPanel(new SpringLayout());
        JPanel prodCrit = new JPanel(new SpringLayout());

        // refresh spring
        baseTextField = new JTextField(5);
        refreshSpringPanel.add(new JLabel("Light Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Miniticks to refresh a light factory");
        baseTextField.setName("LightRefresh");
        refreshSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpringPanel.add(new JLabel("Medium Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Miniticks to refresh a medium factory");
        baseTextField.setName("MediumRefresh");
        refreshSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpringPanel.add(new JLabel("Heavy Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Miniticks to refresh a heavy factory");
        baseTextField.setName("HeavyRefresh");
        refreshSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpringPanel.add(new JLabel("Assault Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Miniticks to refresh a assault factory");
        baseTextField.setName("AssaultRefresh");
        refreshSpringPanel.add(baseTextField);

        // sales spring
        baseTextField = new JTextField(5);
        salesSpringPanel.add(new JLabel("Light Sale Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of faction bm sale of light unit, in ticks");
        baseTextField.setName("LightSaleTicks");
        salesSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        salesSpringPanel.add(new JLabel("Medium Sale Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of faction bm sale of medium unit, in ticks");
        baseTextField.setName("MediumSaleTicks");
        salesSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        salesSpringPanel.add(new JLabel("Heavy Sale Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of faction bm sale of heavy unit, in ticks");
        baseTextField.setName("HeavySaleTicks");
        salesSpringPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        salesSpringPanel.add(new JLabel("Assault Sale Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of faction bm sale of assault unit, in ticks");
        baseTextField.setName("AssaultSaleTicks");
        salesSpringPanel.add(baseTextField);

        // factory misc spring
        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Max Light Units:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max num. of light units, of each type, in factionbays.");
        baseTextField.setName("MaxLightUnits");
        prodMiscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Max Other Units:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max num. of non-light units, of each type, in factionbays.");
        baseTextField.setName("MaxOtherUnits");
        prodMiscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Comp Gain Every:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + "Number of ticks which should pass before component gains<br>" + "are aggregated and displayed to a faction. Recommended: 4</HTML>");
        baseTextField.setName("ShowComponentGainEvery");
        prodMiscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Disputed Planet Color:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Hex color a planet will show up as<br>When no single faction owns more<br>then the minimum amount of land.</html");
        baseTextField.setName("DisputedPlanetColor");
        prodMiscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Min Planet OwnerShip:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>The Least amount of land a Faction own on a planet to control it");
        baseTextField.setName("MinPlanetOwnerShip");
        prodMiscPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        prodMiscPanel.add(new JLabel("Auto Factory Refresh:", SwingConstants.TRAILING));
        baseTextField.setName("FactoryRefreshPoints");
        prodMiscPanel.add(baseTextField);

        // Check Box Spring
        BaseCheckBox = new JCheckBox();
        prodCBoxSpring.add(new JLabel("Produce w/o factory:", SwingConstants.TRAILING));
        BaseCheckBox.setToolTipText("If checked, components will be produced even if no factory of a type/weightclass is owned");
        BaseCheckBox.setName("ProduceComponentsWithNoFactory");
        prodCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox();
        prodCBoxSpring.add(new JLabel("Output Multipliers:", SwingConstants.TRAILING));
        BaseCheckBox.setToolTipText("If checked, personal production multipliers will be shown on ticks");
        BaseCheckBox.setName("ShowOutputMultiplierOnTick");
        prodCBoxSpring.add(BaseCheckBox);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Base Component to Money:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Double Field: Number of Cbills for 1 component</html>");
        baseTextField.setName("BaseComponentToMoneyRatio");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Mek Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Mek Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierMek");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Vehicle Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Vehicle Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierVehicle");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Infantry Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Infantry Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierInfantry");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component BA Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for BA Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierBattleArmor");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Proto Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for ProtoMek Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierProtoMek");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Light Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Light Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierLight");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Medium Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Medium Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierMedium");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Heavy Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Heavy Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierHeavy");
        prodCrit.add(baseTextField);

        baseTextField = new JTextField(5);
        prodCrit.add(new JLabel("Component Assault Mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Modifer for Assault Components to the base cost</html>");
        baseTextField.setName("ComponentToPartsModifierAssault");
        prodCrit.add(baseTextField);

        // lay out the springs
        SpringLayoutHelper.setupSpringGrid(refreshSpringPanel, 4, 2);
        SpringLayoutHelper.setupSpringGrid(salesSpringPanel, 4, 2);
        SpringLayoutHelper.setupSpringGrid(apSpringPanel, 5, 2);
        SpringLayoutHelper.setupSpringGrid(prodMiscPanel, 2);
        SpringLayoutHelper.setupSpringGrid(prodCBoxSpring, 1, 4);
        SpringLayoutHelper.setupSpringGrid(prodCrit, 4);

        // finalize the layout
        JPanel prodGrid = new JPanel(new GridLayout(2, 2));
        prodGrid.add(refreshSpringPanel);
        prodGrid.add(salesSpringPanel);
        prodGrid.add(apSpringPanel);
        prodGrid.add(prodMiscPanel);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(prodGrid);
        add(prodCBoxSpring);
        add(prodCrit);
	}
}
