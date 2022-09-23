/*
 * MekWars - Copyright (C) 2005 
 * 
 * original author - nmorris (urgru@users.sourceforge.net)
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

package client.gui.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import client.campaign.CUnit;
import client.gui.WholeNumberField;
import common.Unit;
import common.util.SpringLayoutHelper;
import megamek.common.Infantry;

/*
 * 
 * @author urgru
 * 
 * inner class which sets up a camo selection dialog.
 */

public class SellUnitDialog extends JDialog implements ActionListener { 
	
	/**
     * 
     */
    private static final long serialVersionUID = 7292249744702852873L;
    //IVARS
	private MWClient mwclient;
	private final JButton okayButton = new JButton("OK");
	private final JButton cancelButton = new JButton("Cancel");	
	private final String okayCommand = "Okay";
	
	//text fields ...
	private JTextField minBidText = new WholeNumberField(0,5);
	private JTextField ticksText = new WholeNumberField(0,5);
    
	//combo box to pick unit from
	private JComboBox possibleSaleUnits = new JComboBox();
	
	//CONSTRUCTOR
	public SellUnitDialog(JFrame parent, MWClient mwclient, Vector<CUnit> toSell) {
		
		//init superclass
		super(parent, "Sell Unit", true);
		
		//save the client
		this.mwclient = mwclient;
		
		//set up the buttons
		okayButton.setActionCommand(okayCommand);
		okayButton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		//do the button layout
		JPanel buttonFlow = new JPanel();
		buttonFlow.add(okayButton);
		buttonFlow.add(cancelButton);
		
		//load all legal units, if no set is given
		if (toSell == null || toSell.size() == 0) {
			
			toSell = new Vector<CUnit>(1,1);
			for(CUnit currU : mwclient.getPlayer().getHangar()) {
				
				if(currU.getType() == Unit.MEK && !Boolean.parseBoolean(mwclient.getserverConfigs("MeksMayBeSoldOnBM")))
					continue;
				
				else if (currU.getType() == Unit.VEHICLE && !Boolean.parseBoolean(mwclient.getserverConfigs("VehsMayBeSoldOnBM")))
					continue;
				
				else if (currU.getType() == Unit.BATTLEARMOR && !Boolean.parseBoolean(mwclient.getserverConfigs("BAMayBeSoldOnBM")))
					continue;
				
				else if (currU.getType() == Unit.PROTOMEK && !Boolean.parseBoolean(mwclient.getserverConfigs("ProtosMayBeSoldOnBM")))
					continue;
				
				else if (currU.getType() == Unit.INFANTRY && !Boolean.parseBoolean(mwclient.getserverConfigs("InfantryMayBeSoldOnBM")))
					continue;
				
				if (currU.getStatus() == Unit.STATUS_FORSALE)
					continue;
				
				if (Boolean.parseBoolean(mwclient.getserverConfigs("BMNoClan")) && currU.getEntity().isClan())
					continue;
				
				//unit wasnt rejected, so add it to the sales list
				toSell.add(currU);
			}
		}
		
		//populate the combo box
		possibleSaleUnits.setModel(new DefaultComboBoxModel(toSell) {
			/**
             * 
             */
            private static final long serialVersionUID = 2012355422040841647L;

            @Override
			public Object getElementAt(int index) {
				CUnit mm = (CUnit)super.getElementAt(index);
				if ( mm.getType() == Unit.MEK || mm.getType() == Unit.VEHICLE || mm.getType() == Unit.AERO )
				    return (mm.getId() +" "+ mm.getModelName() + " [" + mm.getPilot().getGunnery() + "/" + mm.getPilot().getPiloting()+"]");
				
				if ( mm.getType() == Unit.INFANTRY || mm.getType() == Unit.BATTLEARMOR ){
				    if ( ((Infantry)mm.getEntity()).canMakeAntiMekAttacks() )
				        return (mm.getId() +" "+ mm.getModelName() + " [" + mm.getPilot().getGunnery() + "/" + mm.getPilot().getPiloting()+"]");
				    return (mm.getId() +" "+ mm.getModelName() + " [" + mm.getPilot().getGunnery() +"]");
				}
				return (mm.getId() +" "+ mm.getModelName() + " [" + mm.getPilot().getGunnery() +"]");
			}
		});
		
		//put the combo box in a sub-panel to make it autoformat better
		JPanel comboBoxHolder = new JPanel(new SpringLayout());
		JLabel selectUnitHeader = new JLabel("Unit to sell:",SwingConstants.CENTER);
		selectUnitHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
		comboBoxHolder.add(selectUnitHeader);
		comboBoxHolder.add(possibleSaleUnits);
		SpringLayoutHelper.setupSpringGrid(comboBoxHolder,2,1);
		
		Dimension newDim = new Dimension();
		newDim.setSize(possibleSaleUnits.getMinimumSize().getWidth() * 1.25, possibleSaleUnits.getMinimumSize().getHeight());
		possibleSaleUnits.setMaximumSize(newDim);
		
		//set up the text fields
		JPanel sellTextSpring = new JPanel(new SpringLayout());
		
		sellTextSpring.add(new JLabel("Minimum Bid:",SwingConstants.TRAILING));
		minBidText.setToolTipText("Minimum bid you're willing to accept for the unit.");
		minBidText.setText(mwclient.getserverConfigs("MinBMSalesPrice"));
		sellTextSpring.add(minBidText);
		
		sellTextSpring.add(new JLabel("Sale Ticks:",SwingConstants.TRAILING));
		ticksText.setToolTipText("Number of ticks the unit will remain on sale.");
		ticksText.setText(mwclient.getserverConfigs("MinBMSalesTicks"));
		sellTextSpring.add(ticksText);
		
		SpringLayoutHelper.setupSpringGrid(sellTextSpring,2);
		
		//set a default button
		this.getRootPane().setDefaultButton(cancelButton);
		
		//do the final layout
		JPanel generalLayout = new JPanel();
		generalLayout.setLayout(new BoxLayout(generalLayout, BoxLayout.Y_AXIS));
		generalLayout.add(new JLabel("\n"));
		generalLayout.add(comboBoxHolder);
		generalLayout.add(new JLabel("\n"));
		generalLayout.add(sellTextSpring);
		generalLayout.add(new JLabel("\n"));
		generalLayout.add(buttonFlow);
		this.getContentPane().add(generalLayout);
		this.pack();
		this.checkMinimumSize();
		this.setResizable(true);
		
		//center the dialog.
		this.setLocationRelativeTo(mwclient.getMainFrame());
	}
	
	
	/**
	 * OK or CANCEL buttons pressed. Handle any
	 * changes and then close the dialouge.
	 */
	public void actionPerformed(ActionEvent event) {
		
		String command = event.getActionCommand();
		
		if (command.equals(okayCommand)) {
			int index = possibleSaleUnits.getSelectedIndex();
			
			if (index < 0)
				return;
			
			String result = MWClient.CAMPAIGN_PREFIX + "c sell#";
			String mms = (String)possibleSaleUnits.getSelectedItem();
			StringTokenizer st = new StringTokenizer(mms);
			CUnit mm = mwclient.getPlayer().getUnit(Integer.parseInt(st.nextToken()));
			result += mm.getId();
			if (!ticksText.getText().equalsIgnoreCase(""))
				result += "#" + ticksText.getText();
			else
				result += "#" + mwclient.getserverConfigs("MinBMSalesTicks");
			if (!minBidText.getText().equalsIgnoreCase(""))
				result += "#" + minBidText.getText();
			else
				result += "#" + mwclient.getserverConfigs("MinBMSalesPrice");

			mwclient.sendChat(result);
		}
		
		//dispose of the dialog
		this.dispose();
		
	}//end actionPerformed
	
	private void checkMinimumSize() {
		
		Dimension curDim = this.getSize();
		
		int height = 0;
		int width = 0;
		boolean shouldRedraw = false;
		
		if (curDim.getWidth() < 220) {
			width = 220;
			shouldRedraw = true;
		} else
			width = (int)curDim.getWidth();
		
		if (curDim.getHeight() < 220) {
			height = 220;
			shouldRedraw = true;
		} else
			height = (int)curDim.getHeight();
		
		if (shouldRedraw) {
			this.setSize(new Dimension(width, height));
		}
		
	}//end checkMinimumSize
	
	
}//end SellUnitDialog