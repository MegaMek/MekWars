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
public class FactoryPurchasePanel extends JPanel {

	private static final long serialVersionUID = 1471237707053913762L;
	
	private JTextField baseTextField = new JTextField(5);
    
    public FactoryPurchasePanel(MWClient mwclient) {
    	super();
        /*
         * Unit Research Configuration Panel Construction
         */
        JPanel mainPurchasePanel = new JPanel(new SpringLayout());
        JPanel purchasePanel1 = new JPanel(new SpringLayout());
        JPanel purchasePanel2 = new JPanel(new SpringLayout());
        JPanel masterPanel = new JPanel();
        masterPanel.setLayout(new BoxLayout(masterPanel, BoxLayout.Y_AXIS));

        baseTextField = new JTextField(5);
        mainPurchasePanel.add(new JLabel("New Factory " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " to buy 1 factory</html>");
        baseTextField.setName("NewFactoryBaseCost");
        mainPurchasePanel.add(baseTextField);

        baseTextField = new JTextField(5);
        mainPurchasePanel.add(new JLabel("New Factory " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " to buy 1 factory</html>");
        baseTextField.setName("NewFactoryBaseFlu");
        mainPurchasePanel.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(mainPurchasePanel, 4);

        for (int type = 0; type < Unit.MAXBUILD; type++) {
            baseTextField = new JTextField(5);
            purchasePanel1.add(new JLabel(Unit.getTypeClassDesc(type) + " unit " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getTypeClassDesc(type) + " unit factory</html>");
            baseTextField.setName("NewFactoryCostModifier" + Unit.getTypeClassDesc(type));
            purchasePanel1.add(baseTextField);
        }

        for (int size = 0; size <= Unit.ASSAULT; size++) {
            baseTextField = new JTextField(5);
            purchasePanel1.add(new JLabel(Unit.getWeightClassDesc(size) + " unit " + mwclient.moneyOrFluMessage(true, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getWeightClassDesc(size) + " unit factory</html>");
            baseTextField.setName("NewFactoryCostModifier" + Unit.getWeightClassDesc(size));
            purchasePanel1.add(baseTextField);
        }
        SpringLayoutHelper.setupSpringGrid(purchasePanel1, 6);

        for (int type = 0; type < Unit.MAXBUILD; type++) {
            baseTextField = new JTextField(5);
            purchasePanel2.add(new JLabel(Unit.getTypeClassDesc(type) + " unit " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(true, false, -1, false) + " modifier for " + Unit.getTypeClassDesc(type) + " unit factory</html>");
            baseTextField.setName("NewFactoryFluModifier" + Unit.getTypeClassDesc(type));
            purchasePanel2.add(baseTextField);
        }

        for (int size = 0; size <= Unit.ASSAULT; size++) {
            baseTextField = new JTextField(5);
            purchasePanel2.add(new JLabel(Unit.getWeightClassDesc(size) + " unit " + mwclient.moneyOrFluMessage(false, false, -1, false) + ":", SwingConstants.TRAILING));
            baseTextField.setToolTipText("<HTML>" + mwclient.moneyOrFluMessage(false, false, -1, false) + " modifier for " + Unit.getWeightClassDesc(size) + " unit factory</html>");
            baseTextField.setName("NewFactoryFluModifier" + Unit.getWeightClassDesc(size));
            purchasePanel2.add(baseTextField);
        }

        SpringLayoutHelper.setupSpringGrid(purchasePanel2, 6);

        masterPanel.add(mainPurchasePanel);
        masterPanel.add(purchasePanel1);
        masterPanel.add(purchasePanel2);
        add(masterPanel);

    }

}
