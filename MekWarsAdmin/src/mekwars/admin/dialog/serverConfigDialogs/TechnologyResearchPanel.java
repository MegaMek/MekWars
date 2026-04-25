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
public class TechnologyResearchPanel extends JPanel {

	private static final long serialVersionUID = 4491468840677110439L;
	private JTextField baseTextField = new JTextField(5);
	
	public TechnologyResearchPanel(MWClient mwclient) {
		super();
        /*
         * Technology Research Configuration Panel Construction
         */
        JPanel masterPanel = new JPanel(new SpringLayout());

        baseTextField = new JTextField(5);
        masterPanel.add(new JLabel("Points Per Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Total Number of Tech Points a faction needs to move<br>to the next tech level.</html>");
        baseTextField.setName("TechPointsNeedToLevel");
        masterPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        masterPanel.add(new JLabel(mwclient.moneyOrFluMessage(true, false, -1, false) + " Per Tech Point:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount of " + mwclient.moneyOrFluMessage(true, false, -1, false) + " need for 1 tech point</html>");
        baseTextField.setName("TechPointCost");
        masterPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        masterPanel.add(new JLabel(mwclient.moneyOrFluMessage(false, false, -1, false) + " Per Tech Point:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Total Number of " + mwclient.moneyOrFluMessage(false, false, -1, false) + " needed to buy 1 tech point.</html>");
        baseTextField.setName("TechPointFlu");
        masterPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        masterPanel.add(new JLabel("Point " + mwclient.moneyOrFluMessage(true, false, -1, false) + " cost mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Modifier to the cost o a tech point for each level above 1 the faction is.</html>");
        baseTextField.setName("TechLevelTechPointCostModifier");
        masterPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        masterPanel.add(new JLabel("Point " + mwclient.moneyOrFluMessage(false, false, -1, false) + " cost mod:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Modifier to the cost o a tech point for each level above 1 the faction is.</html>");
        baseTextField.setName("TechLevelTechPointFluModifier");
        masterPanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(masterPanel, 2);
        add(masterPanel);
	}

}