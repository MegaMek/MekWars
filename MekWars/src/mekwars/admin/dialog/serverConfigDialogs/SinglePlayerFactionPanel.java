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

import common.Unit;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class SinglePlayerFactionPanel extends JPanel {

	private static final long serialVersionUID = -6458150681823841221L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public SinglePlayerFactionPanel() {
		super();
        /*
         * Single Player Faction Configuration Panel Construction
         */
        JPanel checkBoxPanel = new JPanel(new SpringLayout());
        JPanel playerFactionPanel = new JPanel(new SpringLayout());
        JPanel playerFactionPanel2 = new JPanel(new SpringLayout());

        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

        BaseCheckBox = new JCheckBox("Single Player Factions");
        BaseCheckBox.setToolTipText("If this is checked then each player will have their own faction");
        BaseCheckBox.setName("AllowSinglePlayerFactions");
        checkBoxPanel.add(BaseCheckBox);

        SpringLayoutHelper.setupSpringGrid(checkBoxPanel, 1);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Max Faction Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max Length for a faction name.");
        baseTextField.setName("MaxFactionName");
        playerFactionPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Max Short Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max Length for a factions short name");
        baseTextField.setName("MaxFactionShortName");
        playerFactionPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Base Factory Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Base Refresh Rate for New Factories");
        baseTextField.setName("BaseFactoryRefreshRate");
        playerFactionPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Base Components:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("How many components the new faction starts with for each type/class");
        baseTextField.setName("BaseFactoryComponents");
        playerFactionPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Base Common Table Chances:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Base number of shares for the common build table<br>in all of the starting factions build tables.</html>");
        baseTextField.setName("BaseCommonBuildTableShares");
        playerFactionPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        playerFactionPanel.add(new JLabel("Starting Bays:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of bays the players starting planet gets.</html>");
        baseTextField.setName("StartingPlanetBays");
        playerFactionPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(playerFactionPanel, 4);

        for (int type = 0; type < Unit.MAXBUILD; type++) {
            for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
                baseTextField = new JTextField(5);
                playerFactionPanel2.add(new JLabel("Starting " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type) + " Factory:", SwingConstants.TRAILING));
                baseTextField.setToolTipText("Number of " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type) + " factories a new faction starts with.");
                baseTextField.setName("Starting" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type) + "Factory");
                playerFactionPanel2.add(baseTextField);
            }
        }

        SpringLayoutHelper.setupSpringGrid(playerFactionPanel2, 4);

        masterPanel.add(checkBoxPanel);
        masterPanel.add(playerFactionPanel);
        masterPanel.add(playerFactionPanel2);
        add(masterPanel);
	}

}