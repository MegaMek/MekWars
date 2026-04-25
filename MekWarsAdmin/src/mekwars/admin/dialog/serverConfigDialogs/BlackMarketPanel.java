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
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.Unit;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class BlackMarketPanel extends JPanel {

	private static final long serialVersionUID = 9055477134344550739L;

	private JTextField baseTextField = new JTextField(5);
    private JCheckBox BaseCheckBox = new JCheckBox();
    private JRadioButton baseRadioButton = new JRadioButton();
    
    public BlackMarketPanel(MWClient mwclient) {
		super();
        /*
         * BLACK MARKET setup
         */
        JPanel bmBox = new JPanel();
        //bmBox.setLayout(new BoxLayout(bmBox, BoxLayout.Y_AXIS));

        bmBox.setLayout(new VerticalLayout());
        
        JPanel bmCBoxSpring = new JPanel(new SpringLayout());
        JPanel bmTextSpring = new JPanel(new SpringLayout());

        bmCBoxSpring.setBorder(BorderFactory.createEtchedBorder());
        bmTextSpring.setBorder(BorderFactory.createEtchedBorder());
        
        // small text spring
        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Min BM Sale Length:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum sale time, in ticks.");
        baseTextField.setName("MinBMSalesTicks");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Min BM Sale Price:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum asking price.");
        baseTextField.setName("MinBMSalesPrice");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Max BM Sale Length:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Maximum sale time, in ticks.");
        baseTextField.setName("MaxBMSalesTicks");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Max BM Sale Price:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Maximum asking price. Bids CAN be higher.");
        baseTextField.setName("MaxBMSalesPrice");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Min XP to Buy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("XP required to buy units from the BM");
        baseTextField.setName("MinEXPforBMBuying");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Min XP to Sell:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("XP required to sell units on the BM");
        baseTextField.setName("MinEXPforBMSelling");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("BM Bid " + mwclient.moneyOrFluMessage(false, true, -1) + " Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText(mwclient.moneyOrFluMessage(false, false, -1) + " charge for bidding on the BM.");
        baseTextField.setName("BMBidFlu");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("BM Sale " + mwclient.moneyOrFluMessage(false, true, -1) + " Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Base " + mwclient.moneyOrFluMessage(false, true, -1) + " cost for a BM sale. Modified by weight.");
        baseTextField.setName("BMSellFlu");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("BM Size " + mwclient.moneyOrFluMessage(false, true, -1) + " Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("[SizeCost] * [Unit Weightclass] added to " + mwclient.moneyOrFluMessage(false, true, -1) + " cost of a BM sale.");
        baseTextField.setName("BMFluSizeCost");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Auction Fee:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Auction fee charged to the seller after a sucessful sale<br>This is a double number i.e. 0.15 is 15%</html>");
        baseTextField.setName("AuctionFee");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Rare Chance:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Percent chance of producing a rare unit and sending it to the Market.<br>This is a double Var 1.0 = 1%</html>");
        baseTextField.setName("RareChance");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Rare Sale Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is the minimum amount of ticks<br>a rare unit will be listed on the black marked.</html>");
        baseTextField.setName("RareMinSaleTime");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField(10);
        bmTextSpring.add(new JLabel("Chance unit goes to BM:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>This is the chance that over flow units from house bays<br>goto the BM instead of being scrapped.</html>");
        baseTextField.setName("ChanceToSendUnitToBM");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField();
        bmTextSpring.add(new JLabel("No Sales:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "List of factions that cannot sell on BM. $ deliminted and<br>" + "case sensitive. This stops all players in the faction from<br>" + "selling on the market as well as all sales from the faction<br>" + "when hangars/bays are full. Example: Liao$Davion$Marik$</html>");
        baseTextField.setName("BMNoSell");
        bmTextSpring.add(baseTextField);

        baseTextField = new JTextField();
        bmTextSpring.add(new JLabel("No Bids:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "List of factions that cannot buy from BM. $ deliminted and<br>" + "case sensitive. This stops players from placing bids on units.<br>" + "Example: Trinity Alliance$Lyran Alliance$Word of Blake$</html>");
        baseTextField.setName("BMNoBuy");
        bmTextSpring.add(baseTextField);
        
       SpringLayoutHelper.setupSpringGrid(bmTextSpring, 8);

        // cbox spring - 5 elements in a 3*2 arrangement
        BaseCheckBox = new JCheckBox("Infantry Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell Infantry on the BM");
        BaseCheckBox.setName("InfantryMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("BA Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell BA on the BM");
        BaseCheckBox.setName("BAMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Protos Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell Protos on the BM");
        BaseCheckBox.setName("ProtosMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Vehs Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell Vehs on the BM");
        BaseCheckBox.setName("VehsMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Meks Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell Meks on the BM");
        BaseCheckBox.setName("MeksMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Aeros Allowed");

        BaseCheckBox.setToolTipText("Check to allow player&houses to sell Aeros on the BM");
        BaseCheckBox.setName("AerosMayBeSoldOnBM");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Clan Unit Ban");

        BaseCheckBox.setToolTipText("<html>" + "Check to stop players from selling clan units on the<br>" + "BM. Faction overflow and random rares can include clan<br>" + "tech. Block faction sales entirely to stop overflow.</html>");
        BaseCheckBox.setName("BMNoClan");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Use Parts Market");
        BaseCheckBox.setToolTipText("Use the parts blackmarket this coencides with using parts to repair");
        BaseCheckBox.setName("UsePartsBlackMarket");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Tech Cross Over");

        BaseCheckBox.setToolTipText("If checked IS Player are allowed to buy clan tech on the BM and visa versa.");
        BaseCheckBox.setName("AllowCrossOverTech");
        bmCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Hide BM Units");
        BaseCheckBox.setToolTipText("If checked, unit models and BVs are hidden from the players");
        BaseCheckBox.setName("HiddenBMUnits");
        bmCBoxSpring.add(BaseCheckBox);
        
        SpringLayoutHelper.setupSpringGrid(bmCBoxSpring, 5);

        JPanel bmButtonSpring = new JPanel(new SpringLayout());
        bmButtonSpring.setBorder(BorderFactory.createEtchedBorder());
        
        ButtonGroup auctionTypes = new ButtonGroup();

        baseRadioButton = new JRadioButton("Vickery");

        baseRadioButton.setName("UseVickeryAuctionType");
        baseRadioButton.setToolTipText("<html>Vickrey auction is a modified highest sealed bid auction. Winner<br>" + "determination is the same (highest bid, earliest placement in the<br>" + "event of a tie), but the winner pays 2nd highest bid, plus one, in<br>" + "lieu of the amount he offered.<br>" + "NOTE: you must restart the server for this to take effect!</html");

        auctionTypes.add(baseRadioButton);
        bmButtonSpring.add(baseRadioButton);

        baseRadioButton = new JRadioButton("Highest Sealed Bid");

        baseRadioButton.setName("UseHighestSealedBidAuctionType");
        baseRadioButton.setToolTipText("<html>Winner is simply the highest offering person who can<br>" + "afford to pay. This, codewise, is a truncated Vickrey<br>" + "Auction. Same mechanism to find highest bidder, but no<br>" + "downward adjustment.<br>" + "NOTE: You must restart the server for this to take effect!</html>");

        auctionTypes.add(baseRadioButton);
        bmButtonSpring.add(baseRadioButton);

        SpringLayoutHelper.setupSpringGrid(bmButtonSpring, 2);

        JPanel BMWeightPanel = new JPanel();
        BMWeightPanel.setLayout(new BoxLayout(BMWeightPanel, BoxLayout.Y_AXIS));
        BMWeightPanel.setBorder(BorderFactory.createEtchedBorder());
        
        BaseCheckBox = new JCheckBox("Use BM Weighting Tables");
        BaseCheckBox.setName("UseBMWeightingTables");
        BMWeightPanel.add(BaseCheckBox);
        
        JPanel MekWeightPanel = new JPanel();
        
        baseTextField = new JTextField(5);
        baseTextField.setName("BMLightMekWeight");
        MekWeightPanel.add(new JLabel("Light Mek:", SwingConstants.TRAILING));
        MekWeightPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        baseTextField.setName("BMMediumMekWeight");
        MekWeightPanel.add(new JLabel("Medium Mek:", SwingConstants.TRAILING));
        MekWeightPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        baseTextField.setName("BMHeavyMekWeight");
        MekWeightPanel.add(new JLabel("Heavy Mek:", SwingConstants.TRAILING));
        MekWeightPanel.add(baseTextField);
        
        baseTextField = new JTextField(5);
        baseTextField.setName("BMAssaultMekWeight");
        MekWeightPanel.add(new JLabel("Assault Mek:", SwingConstants.TRAILING));
        MekWeightPanel.add(baseTextField);
        
        BMWeightPanel.add(MekWeightPanel);
        
        JPanel BMBayLimitPanel = new JPanel();
        BMBayLimitPanel.setBorder(BorderFactory.createEtchedBorder());
        baseTextField = new JTextField(5);
        baseTextField.setName("MaximumNegativeBaysFromBM");
        baseTextField.setToolTipText("-1 to disable check for negative bays.");
        BMBayLimitPanel.add(new JLabel("Maximum Negative Bays From BM:", SwingConstants.TRAILING));
        BMBayLimitPanel.add(baseTextField);
        
        JPanel BMPriceModPanel = new JPanel();
        JPanel BMPMPanel = new JPanel();
        BMPMPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        BMPriceModPanel.setBorder(BorderFactory.createEtchedBorder());
        BMPMPanel.setLayout(new GridLayout(7,5));
        BMPMPanel.add(new JLabel(" "));
        BMPMPanel.add(new JLabel("Light"));
        BMPMPanel.add(new JLabel("Medium"));
        BMPMPanel.add(new JLabel("Heavy"));
        BMPMPanel.add(new JLabel("Assault"));
        
        for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
        	BMPMPanel.add(new JLabel(Unit.getTypeClassDesc(type)));
        	for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
        		baseTextField = new JTextField(5);
        		baseTextField.setName("BMPriceMultiplier_" + Unit.getWeightClassDesc(weight) + Unit.getTypeClassDesc(type) );
        		baseTextField.setToolTipText("Multiplier for faction bay " + Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type) + " units sent to the BM.  (float value)");
        		BMPMPanel.add(baseTextField);
        	}
        }
        BMPriceModPanel.add(BMPMPanel);
        
        bmBox.add(bmTextSpring);
        bmBox.add(bmCBoxSpring);
        bmBox.add(bmButtonSpring);
        bmBox.add(BMWeightPanel);
        bmBox.add(BMBayLimitPanel);
        bmBox.add(BMPriceModPanel);
        
        add(bmBox);
    }
}
