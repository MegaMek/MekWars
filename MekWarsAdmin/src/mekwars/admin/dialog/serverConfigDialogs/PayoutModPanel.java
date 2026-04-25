/*
 * MekWars - Copyright (C) 2011
 *
 * Original author - Spork (billypinhead@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package admin.dialog.serverConfigDialogs;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import common.VerticalLayout;

public class PayoutModPanel extends JPanel {

	private static final long serialVersionUID = 7388409164181746448L;
	private JTextField baseTextField = new JTextField(5);
    private JCheckBox baseCheckBox = new JCheckBox();
    
    private final int TYPE_RP = 0;
    private final int TYPE_FLU = 1;
    private final int TYPE_MONEY = 2;
    private final int TYPE_EXP = 3;
    private final int TYPE_LAND = 4;
    private final int MAX_TYPE = 4;
    
    int maxWidth = 0;
    int maxLabelWidth = 0;
    
    private JPanel setUpPanel (JPanel panel, int type) {
    	if (type < 0 || type > MAX_TYPE) {
    		return panel;
    	}
    	
    	String typeName = "";
    	String ttText = ""; 
    	    	
    	switch (type) {
    	case TYPE_RP:
    		typeName = "RP";
    		ttText = "RP";
    		break;
    	case TYPE_FLU:
    		typeName = "Influence";
    		ttText = "influence";
    		break;
    	case TYPE_MONEY:
    		typeName = "Money";
    		ttText = "money";
    		break;
    	case TYPE_EXP:
    		typeName = "Exp";
    		ttText = "experience";
    		break;
    	case TYPE_LAND:
    		typeName = "Land";
    		ttText = "land";
    		break;
    	}
    	
    	baseCheckBox = new JCheckBox("Modify Op Payout");
    	baseCheckBox.setName("ModifyOpPayoutByELO_" + typeName);
    	baseCheckBox.setToolTipText("<html>If checked, " + ttText + " payout will be modified based on ELO</html>");
    	panel.add(baseCheckBox);
    	
    	
    	baseCheckBox = new JCheckBox("For Higher Player");
    	baseCheckBox.setName("ModifyOpPayoutByELO_" + typeName + "_Higher");
    	baseCheckBox.setToolTipText("<html>If checked, apply modified payout to higher-ranked player.<br>Otherwise, he'll get unmodified payout</html>");
    	panel.add(baseCheckBox);

    	baseCheckBox = new JCheckBox("For Lower Player");
    	baseCheckBox.setName("ModifyOpPayoutByELO_" + typeName + "_Lower");
    	baseCheckBox.setToolTipText("<html>If checked, apply modified payout to lower-ranked player.<br>Otherwise, he'll get unmodified payout</html>");
    	panel.add(baseCheckBox);

    	baseTextField = new JTextField(5);
    	baseTextField.setName("ModifyOpPayoutByELO_" + typeName + "_MinELO");
    	baseTextField.setToolTipText("<html>If a player's ELO is below this, payout will be calculated using this value</html>");
    	panel.add(new JLabel("Min ELO:"));
    	panel.add(baseTextField);

    	baseTextField = new JTextField(5);
    	baseTextField.setName("ModifyOpPayoutByELO_" + typeName + "_MaxELO");
    	baseTextField.setToolTipText("<html>If a player's ELO is above this, payout will be calculated using this value</html>");
    	panel.add(new JLabel("Max ELO:"));
    	panel.add(baseTextField);

    	panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), typeName));
    	
    	maxWidth = Math.max(maxWidth, panel.getPreferredSize().width);
    	
    	return panel;
    }
    
	public PayoutModPanel() {
        setLayout(new VerticalLayout());
        
        JPanel RPPanel = setUpPanel(new JPanel(), TYPE_RP);
        JPanel fluPanel = setUpPanel(new JPanel(), TYPE_FLU);
        JPanel moneyPanel = setUpPanel(new JPanel(), TYPE_MONEY);
        JPanel landPanel = setUpPanel(new JPanel(), TYPE_LAND);
        JPanel expPanel = setUpPanel(new JPanel(), TYPE_EXP);
        
        JPanel upperPanel = new JPanel();
        upperPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Options"));
        upperPanel.setLayout(new VerticalLayout());
                
        baseCheckBox = new JCheckBox("Modify Op Payout for ELO Difference?");
        baseCheckBox.setName("ModifyOpPayoutByELO");
        baseCheckBox.setToolTipText("<html>If checked, payout will be changed based on ELO of players involved.");
        upperPanel.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Always reduce land payout");
        baseCheckBox.setName("AlwaysReduceLandTransfer");
        baseCheckBox.setToolTipText("If checked, land transfer will be reduced no matter who wins");
        landPanel.add(baseCheckBox);
        
        baseTextField = new JTextField(5);
        baseTextField.setName("ModifyOpPayoutByELO_Multiplier");
        baseTextField.setToolTipText("<html><p>Not really a multiplier, but a power.  The ELO ratio (loser / winner) will be raised to this power to determine change of actual payout.</p><p>Please refer to documentation for a more complete discussion of this mechanic.</p></html>");
        upperPanel.add(new JLabel("Payout Multiplier:"));
        upperPanel.add(baseTextField);
        
        upperPanel.setPreferredSize(new Dimension(Math.max(upperPanel.getPreferredSize().width, fluPanel.getPreferredSize().width), upperPanel.getPreferredSize().height));
        
        // Exp Mod panel
        JPanel expModPanel = new JPanel();
        expModPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createRaisedBevelBorder(), "Experience Mod"));
        baseCheckBox = new JCheckBox("Experience Affects Land Exchange");
        baseCheckBox.setName("ModifyLandExchangeByExp");
        baseCheckBox.setToolTipText("<html>If checked, land exchange will be reduced until players meet an experience threshold<br />Formula used is:<br /><p align='center'><b>Math.min(1, ( (<i>base</i> + exp) / <i>maximum</i> ))</b></p></html>");
        expModPanel.add(baseCheckBox);
        
        expModPanel.add(new JLabel("Base: "));
        
        baseTextField = new JTextField(5);
        baseTextField.setName("ModifyLandExchangeByExp_Base");
        expModPanel.add(baseTextField);
        
        expModPanel.add(new JLabel("Max: "));
        
        baseTextField = new JTextField(5);
        baseTextField.setName("ModifyLandExchangeByExp_Max");
        expModPanel.add(baseTextField);
        
        add(upperPanel);
        add(RPPanel);
        add(fluPanel);
        add(expPanel);
        add(moneyPanel);
        add(landPanel);
        add(expModPanel);
        
	}
}
