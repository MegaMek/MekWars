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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.Unit;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class UnitResearchPanel extends JPanel {

	private static final long serialVersionUID = 5088212805632411157L;
	private JTextField baseTextField = new JTextField(5);
    
	public UnitResearchPanel(MWClient mwclient) {
		super();
        /*
         * Unit Research Configuration Panel Construction
         */
        JPanel mainResearchPanel = new JPanel(new SpringLayout());
        JPanel researchPanel1 = new JPanel(new SpringLayout());
        JPanel researchPanel2 = new JPanel(new SpringLayout());
        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

        baseTextField = new JTextField(5);
        mainResearchPanel.add(new JLabel("Base Research " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " to buy 1 research point</html>");
        baseTextField.setName("BaseResearchCost");
        mainResearchPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        mainResearchPanel.add(new JLabel("Base Research " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " to buy 1 research point</html>");
        baseTextField.setName("BaseResearchFlu");
        mainResearchPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        mainResearchPanel.add(new JLabel("Tech Level " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for each<br>tech level above 1 that the faciton is</html>");
        baseTextField.setName("ResearchTechLevelCostModifer");
        mainResearchPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        mainResearchPanel.add(new JLabel("Tech Level " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for each<br>tech level above 1 that the faction is</html>");
        baseTextField.setName("ResearchTechLevelFluModifer");
        mainResearchPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        mainResearchPanel.add(new JLabel("Max Research:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Max number of shares per unit a faction can research.</html>");
        baseTextField.setName("MaxUnitResearchPoints");
        mainResearchPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(mainResearchPanel, 6);

        for (int type = 0; type < Unit.MAXBUILD; type++) {
            baseTextField = new JTextField(5);
            researchPanel1.add(new JLabel(Unit.getTypeClassDesc(type) + " unit " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getTypeClassDesc(type) + " units</html>");
            baseTextField.setName("ResearchCostModifier" + Unit.getTypeClassDesc(type));
            researchPanel1.add(baseTextField);
        }

        for (int size = 0; size <= Unit.ASSAULT; size++) {
            baseTextField = new JTextField(5);
            researchPanel1.add(new JLabel(Unit.getWeightClassDesc(size) + " unit " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getWeightClassDesc(size) + " units</html>");
            baseTextField.setName("ResearchCostModifier" + Unit.getWeightClassDesc(size));
            researchPanel1.add(baseTextField);
        }

        for (int type = 0; type < Unit.MAXBUILD; type++) {
            baseTextField = new JTextField(5);
            researchPanel2.add(new JLabel(Unit.getTypeClassDesc(type) + " unit " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getTypeClassDesc(type) + " units</html>");
            baseTextField.setName("ResearchFluModifier" + Unit.getTypeClassDesc(type));
            researchPanel2.add(baseTextField);
        }

        for (int size = 0; size <= Unit.ASSAULT; size++) {
            baseTextField = new JTextField(5);
            researchPanel2.add(new JLabel(Unit.getWeightClassDesc(size) + " unit " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(false, false, -1, false) + " modifier for " + Unit.getWeightClassDesc(size) + " units</html>");
            baseTextField.setName("ResearchFluModifier" + Unit.getWeightClassDesc(size));
            researchPanel2.add(baseTextField);
        }

        SpringLayoutHelper.setupSpringGrid(researchPanel1, 6);
        SpringLayoutHelper.setupSpringGrid(researchPanel2, 6);

        masterPanel.add(mainResearchPanel);
        masterPanel.add(researchPanel1);
        masterPanel.add(researchPanel2);
        add(masterPanel);
	}

}