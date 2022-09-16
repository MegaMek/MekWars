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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;

import client.MWClient;
import common.Unit;
import common.VerticalLayout;

/**
 * @author Spork
 * @author jtighe
 */
public class AutoProdPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2593134010133363970L;

    private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
	
	public AutoProdPanel(MWClient mwclient) {
		super();
        /*
         * AutoProduction Panel
         */
        
        setLayout(new VerticalLayout());
        
        // Choose Classic or New
        ButtonGroup autoProdType = new ButtonGroup();
        JRadioButton apTypeClassic = new JRadioButton("Use Classic Autoproduction");
        apTypeClassic.setToolTipText("Autoproduction is controlled only by weight");
        apTypeClassic.setName("UseAutoProdClassic");
        autoProdType.add(apTypeClassic);
        
        JRadioButton apTypeNew = new JRadioButton("Use New Autoproduction");
        apTypeNew.setToolTipText("Autoproduction done by type and weight");
        apTypeNew.setName("UseAutoProdNew");
        autoProdType.add(apTypeNew);
        

        
        JPanel selectionPanel = new JPanel();
        selectionPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        selectionPanel.add(apTypeClassic);
        selectionPanel.add(apTypeNew);
        
        if(Boolean.parseBoolean(mwclient.getserverConfigs("UseAutoProdNew"))) {
        	apTypeClassic.setSelected(false);
        	apTypeNew.setSelected(true);
        } else {
        	apTypeNew.setSelected(false);
        	apTypeClassic.setSelected(true);
        }
        
        //selectionPanel.setPreferredSize(new Dimension(selectionPanel.getMinimumSize()));
        JPanel apTopPanel = new JPanel();
        apTopPanel.add(selectionPanel);
        
        // Classic Menu
        JPanel apClassicPanel = new JPanel();
        apClassicPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        apClassicPanel.setLayout(new BoxLayout(apClassicPanel, BoxLayout.Y_AXIS));
        JLabel l = new JLabel("Classic AP");
        apClassicPanel.add(l);
        JPanel apClassicBoxPanel = new JPanel();
        //apClassicBoxPanel.setLayout(new BoxLayout(apClassicBoxPanel, BoxLayout.X_AXIS));
        
        baseTextField = new JTextField(5);
        apClassicBoxPanel.add(new JLabel("Lights to AP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of units worth of stored components to trigger an AP attempt for light units");
        baseTextField.setName("APAtMaxLightUnits");
        apClassicBoxPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        apClassicBoxPanel.add(new JLabel("Mediums to AP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of units worth of stored components to trigger an AP attempt for medium units");
        baseTextField.setName("APAtMaxMediumUnits");
        apClassicBoxPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        apClassicBoxPanel.add(new JLabel("Heavies to AP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of units worth of stored components to trigger an AP attempt for heavy units");
        baseTextField.setName("APAtMaxHeavyUnits");
        apClassicBoxPanel.add(baseTextField);

        baseTextField = new JTextField(5);
        apClassicBoxPanel.add(new JLabel("Assaults to AP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of units worth of stored components to trigger an AP attempt for assault units");
        baseTextField.setName("APAtMaxAssaultUnits");
        apClassicBoxPanel.add(baseTextField); 
        
        apClassicPanel.add(apClassicBoxPanel);
        
        JPanel failureRatePanel = new JPanel();
        
        baseTextField = new JTextField(5);
        failureRatePanel.add(new JLabel("AP Failure Rate:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("% of autoproduction attempts which fail and destroy components");
        baseTextField.setName("AutoProductionFailureRate");
        failureRatePanel.add(baseTextField);
        apClassicPanel.add(failureRatePanel);
        
        JPanel apMiddlePanel = new JPanel();
        apMiddlePanel.add(apClassicPanel);
        
        // New AP
        JPanel apNewPanel = new JPanel();
        apNewPanel.setLayout(new VerticalLayout());
        apNewPanel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED));
        apNewPanel.add(new JLabel("New Autoproduction Model"));
        
        JPanel apNewBoxPanel = new JPanel();
        apNewBoxPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        apNewBoxPanel.setLayout(new GridLayout(0, 9));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel("Light"));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel("Medium"));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel("Heavy"));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel("Assault"));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel(" "));
        apNewBoxPanel.add(new JLabel("Units"));
        apNewBoxPanel.add(new JLabel("Failure"));
        apNewBoxPanel.add(new JLabel("Units"));
        apNewBoxPanel.add(new JLabel("Failure"));
        apNewBoxPanel.add(new JLabel("Units"));
        apNewBoxPanel.add(new JLabel("Failure"));
        apNewBoxPanel.add(new JLabel("Units"));
        apNewBoxPanel.add(new JLabel("Failure"));
        
        for (int i = 0; i < Unit.MAXBUILD; i++) {
        	for (int j = 0; j <= Unit.ASSAULT; j++) {
        		if (j == 0) {
        			apNewBoxPanel.add(new JLabel(Unit.getTypeClassDesc(i)));
        		}
        		baseTextField = new JTextField();
        		baseTextField.setName("APAtMax" + Unit.getWeightClassDesc(j) + Unit.getTypeClassDesc(i));
        		baseTextField.setToolTipText("Number of units worth of stored components to trigger an AP attempt for " + Unit.getWeightClassDesc(j) + " " + Unit.getTypeClassDesc(i));
        		apNewBoxPanel.add(baseTextField);
        		
        		baseTextField = new JTextField();
        		baseTextField.setName("APFailureRate" + Unit.getWeightClassDesc(j) + Unit.getTypeClassDesc(i));
        		baseTextField.setToolTipText("Percent failure rate for " + Unit.getWeightClassDesc(j) + " " + Unit.getTypeClassDesc(i));
        		apNewBoxPanel.add(baseTextField);
        	}
        }
        
        apNewPanel.add(apNewBoxPanel);
        
        JPanel apBottomPanel = new JPanel();
        apBottomPanel.add(apNewPanel);
        
        JPanel checkBoxPanel = new JPanel();
        checkBoxPanel.setBorder(BorderFactory.createEtchedBorder());
        checkBoxPanel.add(new JLabel("Scrap Oldest Units First:", SwingConstants.TRAILING));
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("ScrapOldestUnitsFirst");
        BaseCheckBox.setToolTipText("<html>If checked, bay units will be scrapped/sold in order of unitID<br>If not checked, the unit chosen will be random.</html>");
        checkBoxPanel.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox();
        BaseCheckBox.setName("OnlyUseOriginalFactoriesForAutoprod");
        BaseCheckBox.setToolTipText("<html>If checked, autoproduction will only happen from originally-owned factories.");
        checkBoxPanel.add(new JLabel("Restrict Autoproduction to Faction-original factories"));
        checkBoxPanel.add(BaseCheckBox);
        
        add(apTopPanel);
        add(apMiddlePanel);
        add(apBottomPanel);
        add(checkBoxPanel);
	}
}
