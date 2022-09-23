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

public class RepodPanel extends JPanel {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 8749341330457544463L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
    
    public RepodPanel(MWClient mwclient) {
		super();
        /*
         * REPOD PANEL CONSTRUCTION Repod contols. Costs, factory usage, table options, etc. Use nested layouts. A Box containing a Flow and 3 Springs.
         */
        JPanel repodBoxPanel = new JPanel();
        JPanel repodCBoxGridPanel = new JPanel(new GridLayout(2, 3));
        JPanel repodSpringGrid = new JPanel(new GridLayout(2, 2));
        JPanel refreshSpring = new JPanel(new SpringLayout());
        JPanel cbillSpring = new JPanel(new SpringLayout());
        JPanel componentSpring = new JPanel(new SpringLayout());
        JPanel fluSpring = new JPanel(new SpringLayout());
        repodBoxPanel.setLayout(new BoxLayout(repodBoxPanel, BoxLayout.Y_AXIS));
        repodSpringGrid.add(cbillSpring);
        repodSpringGrid.add(fluSpring);
        repodSpringGrid.add(componentSpring);
        repodSpringGrid.add(refreshSpring);
        repodBoxPanel.add(repodCBoxGridPanel);
        repodBoxPanel.add(repodSpringGrid);

        // set up the flow panel
        BaseCheckBox = new JCheckBox("Cost " + mwclient.moneyOrFluMessage(true, true, -1));

        BaseCheckBox.setToolTipText("Check to enable " + mwclient.moneyOrFluMessage(true, true, -1) + " charges for repods");
        BaseCheckBox.setName("DoesRepodCost");
        repodCBoxGridPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Factory");

        BaseCheckBox.setToolTipText("Check to have repodding use a factory");
        BaseCheckBox.setName("RepodUsesFactory");
        repodCBoxGridPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Uses Comps");

        BaseCheckBox.setToolTipText("Check to have repodding consume components");
        BaseCheckBox.setName("RepodUsesComp");
        repodCBoxGridPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Common Table");

        BaseCheckBox.setToolTipText("Check to allow all factions to repod from common table.");
        BaseCheckBox.setName("UseCommonTableForRepod");
        repodCBoxGridPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Random");

        BaseCheckBox.setToolTipText("Check to allow random repods.");
        BaseCheckBox.setName("RandomRepodAllowed");
        repodCBoxGridPanel.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Random Only");

        BaseCheckBox.setToolTipText("If checked, only random repods are allowed.");
        BaseCheckBox.setName("RandomRepodOnly");
        repodCBoxGridPanel.add(BaseCheckBox);

        // and then the various springs. MU first.
        baseTextField = new JTextField(5);
        cbillSpring.add(new JLabel("Light " + mwclient.moneyOrFluMessage(true, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(true, true, -1) + " required to repod a light unit");
        baseTextField.setName("RepodCostLight");
        cbillSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        cbillSpring.add(new JLabel("Medium " + mwclient.moneyOrFluMessage(true, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(true, true, -1) + " required to repod a medium unit");
        baseTextField.setName("RepodCostMedium");
        cbillSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        cbillSpring.add(new JLabel("Heavy " + mwclient.moneyOrFluMessage(true, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(true, true, -1) + " required to repod a heavy unit");
        baseTextField.setName("RepodCostHeavy");
        cbillSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        cbillSpring.add(new JLabel("Assault " + mwclient.moneyOrFluMessage(true, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(true, true, -1) + " required to repod an assault unit");
        baseTextField.setName("RepodCostAssault");
        cbillSpring.add(baseTextField);

        // now the flu spring
        baseTextField = new JTextField(5);
        fluSpring.add(new JLabel("Light " + mwclient.moneyOrFluMessage(false, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, true, -1) + " required to repod a light unit");
        baseTextField.setName("RepodFluLight");
        fluSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        fluSpring.add(new JLabel("Medium " + mwclient.moneyOrFluMessage(false, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, true, -1) + " required to repod a medium unit");
        baseTextField.setName("RepodFluMedium");
        fluSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        fluSpring.add(new JLabel("Heavy " + mwclient.moneyOrFluMessage(false, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, true, -1) + " required to repod a heavy unit");
        baseTextField.setName("RepodFluHeavy");
        fluSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        fluSpring.add(new JLabel("Assault " + mwclient.moneyOrFluMessage(false, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, true, -1) + " required to repod an assault unit");
        baseTextField.setName("RepodFluAssault");
        fluSpring.add(baseTextField);

        // then the component spring ...
        baseTextField = new JTextField(5);
        componentSpring.add(new JLabel("Light Components:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Components required to repod a light unit");
        baseTextField.setName("RepodCompLight");
        componentSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        componentSpring.add(new JLabel("Medium Components:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Components required to repod a medium unit");
        baseTextField.setName("RepodCompMedium");
        componentSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        componentSpring.add(new JLabel("Heavy Components:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Components required to repod a heavy unit");
        baseTextField.setName("RepodCompHeavy");
        componentSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        componentSpring.add(new JLabel("Assault Components:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Components required to repod an assault unit");
        baseTextField.setName("RepodCompAssault");
        componentSpring.add(baseTextField);

        // then, the refresh times
        baseTextField = new JTextField(5);
        refreshSpring.add(new JLabel("Light Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Num. miniticks requires to refresh a factory which pods a light unit");
        baseTextField.setName("RepodRefreshTimeLight");
        refreshSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpring.add(new JLabel("Medium Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Num. miniticks requires to refresh a factory which pods a medium unit");
        baseTextField.setName("RepodRefreshTimeMedium");
        refreshSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpring.add(new JLabel("Heavy Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Num. miniticks requires to refresh a factory which pods a heavy unit");
        baseTextField.setName("RepodRefreshTimeHeavy");
        refreshSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        refreshSpring.add(new JLabel("Assault Refresh:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Num. miniticks requires to refresh a factory which pods an assault unit");
        baseTextField.setName("RepodRefreshTimeAssault");
        refreshSpring.add(baseTextField);

        // and last, the random modifier
        JPanel repodRandomFlowTemp = new JPanel(new SpringLayout());
        baseTextField = new JTextField(5);
        repodRandomFlowTemp.add(new JLabel("Percent of Cost for Random:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Amount to reduce repod costs when a pod is random, instead of targeted.<br>Example 70 would give you 70% of the current cost.</HTML>");
        baseTextField.setName("RepodRandomMod");
        repodRandomFlowTemp.add(baseTextField);

        baseTextField = new JTextField(5);
        repodRandomFlowTemp.add(new JLabel("No Factory Repod Folder:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>If repoding does not use factories then all repods will check this folder<br>for the build tables for the house.</html>");
        baseTextField.setName("NoFactoryRepodFolder");
        repodRandomFlowTemp.add(baseTextField);

        // finalize the layout.
        SpringLayoutHelper.setupSpringGrid(cbillSpring, 4, 2);
        SpringLayoutHelper.setupSpringGrid(fluSpring, 4, 2);
        SpringLayoutHelper.setupSpringGrid(refreshSpring, 4, 2);
        SpringLayoutHelper.setupSpringGrid(componentSpring, 4, 2);
        SpringLayoutHelper.setupSpringGrid(repodRandomFlowTemp, 1, 4);
        repodBoxPanel.add(repodRandomFlowTemp);// add the temp panel for the
        // mod. this needs to be
        // rewritten.
        add(repodBoxPanel);
	}
}
