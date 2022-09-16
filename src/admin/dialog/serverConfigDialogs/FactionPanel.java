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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

public class FactionPanel extends JPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = 6005564512507419589L;

    private JTextField baseTextField = new JTextField(5);
    private JCheckBox baseCheckBox = new JCheckBox();

    public FactionPanel(MWClient mwclient) {
		super();

        /*
         * FACTION TAB CONSTRUCTION
         */
        JPanel factionSpring1 = new JPanel(new SpringLayout());
        JPanel factionSpring2 = new JPanel(new SpringLayout());

        // faction spring #1 -- mostly SOL things
        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Starting " + mwclient.moneyOrFluMessage(true, true, -1) + ":", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of " + mwclient.moneyOrFluMessage(true, true, -1) + " given to a new SOL player");
        baseTextField.setName("PlayerBaseMoney");
        factionSpring1.add(baseTextField);

		//@Salient adding option to give new player starting RP
		baseTextField = new JTextField(5);
		factionSpring1.add(new JLabel("Starting "+ mwclient.getserverConfigs("RPShortName") + ":", SwingConstants.TRAILING));
		baseTextField.setToolTipText("Number of "+ mwclient.getserverConfigs("RPLongName") +" given to a new SOL player.");
		baseTextField.setName("PlayerBaseRP");
		factionSpring1.add(baseTextField);
		
		//@Salient adding option to give new player starting Flu
		baseTextField = new JTextField(5);
		factionSpring1.add(new JLabel("Starting "+ mwclient.getserverConfigs("FluShortName") + ":", SwingConstants.TRAILING));
		baseTextField.setToolTipText("Number of "+ mwclient.getserverConfigs("FluLongName") +" given to a new SOL player.");
		baseTextField.setName("PlayerBaseFlu");
		factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Max SOL XP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max amount of XP a player can earn in SOL");
        baseTextField.setName("MaxSOLExp");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Max SOL CBills:", SwingConstants.TRAILING));
        baseTextField.setName("MaxSOLCBills");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Min XP to Defect:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Min XP needed to leave SOL");
        baseTextField.setName("MinEXPforDefecting");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("XP per House Rank:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + "Multiplier used to determine how much additional XP a SOL<br>" + "player must earn to join a low ranked faction. XP to Defect is<br>" + "[Min To Defect] + [Rank of Target House * XP per faction Rank]</HTML>");
        baseTextField.setName("EXPNeededPerHouseRank");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Min House Techs:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" + "Minimum number of faction techs. Assigned to player<br>" + "*if* faction supplied bays are lower than the min.");
        baseTextField.setName("MinimumHouseBays");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Newbie Techs:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of faction techs given to SOL players");
        baseTextField.setName("NewbieHouseBays");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Merc Techs:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of faction techs given to mercenary players");
        baseTextField.setName("MercHouseBays");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("EXP for Bay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of experience a player needs to gain 1 bay.");
        baseTextField.setName("ExperienceForBay");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Max Bays from EXP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Maximum number of bays a player may get from experience.");
        baseTextField.setName("MaxBaysFromEXP");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Donations Allowed:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of donations players are allowed each tick.");
        baseTextField.setName("DonationsAllowed");
        factionSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring1.add(new JLabel("Scraps Allowed:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of scraps players are allowed each tick.");
        baseTextField.setName("ScrapsAllowed");
        factionSpring1.add(baseTextField);



        SpringLayoutHelper.setupSpringGrid(factionSpring1, 2);

        // faction spring #2
		baseTextField = new JTextField(5);
		factionSpring2.add(new JLabel("Light XP:", SwingConstants.TRAILING));
		baseTextField.setToolTipText("XP required to buy light units");
		baseTextField.setName("MinEXPforLight");
		factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Medium XP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("XP required to buy medium units");
        baseTextField.setName("MinEXPforMedium");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Heavy XP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("XP required to buy heavy units");
        baseTextField.setName("MinEXPforHeavy");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Assault XP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("XP required to buy assault units");
        baseTextField.setName("MinEXPforAssault");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Welfare Ceiling:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max # of Cbills a player can have to collect welfare");
        baseTextField.setName("WelfareCeiling");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Total Hangar BV for Welfare:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("The Max BV a player can have to collect welfare.");
        baseTextField.setName("WelfareTotalUnitBVCeiling");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Cost Multi @ Donate:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Cost to donate a unit is determined by multiplying the faction's base purchase<br>" + "cost by this value. Negative numbers will pay the player.<br>" + "Example: Purchase cost of 100 CBills * Multi of .25 = Pay 25 CB to donate.<br>" + "Example: Purchase cost of 50 CBills * Multi of 0 = Free to donate.<br>" + "Example: Purchase cost of 80 CBills * Multi of -.10 = Get paid 8 CB.</html>");
        baseTextField.setName("DonationCostMultiplier");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Cost Multi to Buy Used:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Cost to purchase a used unit, determined by multiplying a unit's new purchase<br>" + "cost by this value. Final cost cannot be negative.<br>" + "Example: New cost of 100 CBills * Multi of .50 = Costs 50 to buy used.<br>" + "Example: Purchase cost of 50 CBills * Multi of 0 = Free to buy used.</html>");
        baseTextField.setName("UsedPurchaseCostMulti");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Cost Multi @ Scrap:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Cost to scrap a unit is determined by multiplying the faction's base purchase<br>" + "cost by this value. Negative numbers will pay the player. This value is used for<br>" + "all units if AR is off, and for fully repaired meks if AR is on." + "Example: Purchase cost of 100 CBills * Multi of .50 = Pay 50 CB to scrap.<br>" + "Example: Purchase cost of 75 CBills * Multi of 0.0 = Free to scrap.<br>" + "Example: Purchase cost of 80 CBills * Multi of -.25 = Get paid 20 CB.</html>");
        baseTextField.setName("ScrapCostMultiplier");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Armor Scrap Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Float field, AR Only<br>Percent of a unit's buy price to charge someone for scrapping a unit with minor armor damage<br>Negative number will give money to the player<br>.1 = 10%</html>");
        baseTextField.setName("CostToScrapOnlArmorDamage");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Critical Scrap Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Float field, AR Only<br>Percent of a unit's buy price to charge someone for scrapping a unit with damaged criticals<br>Negative number will give money to the player<br>.1 = 10%</html>");
        baseTextField.setName("CostToScrapCriticallyDamaged");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Engined:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Float field, AR Only<br>Percent of a unit's buy price to charge someone for scrapping an engined unit<br>Negative number will give money to the player<br>.1 = 10%</html>");
        baseTextField.setName("CostToScrapEngined");
        factionSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Leader Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Integer Field, Access Level given to a player when they are promoted to the faction leadership<br>NOTE: if their access level is already higher then this it will not be changed.</html>");
        baseTextField.setName("factionLeaderLevel");
        factionSpring2.add(baseTextField);
        
        baseTextField = new JTextField(5);
        factionSpring2.add(new JLabel("Max MOTD Length:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of characters allowed in the MOTD.");
        baseTextField.setName("MaxMOTDLength");
        factionSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(factionSpring2, 2);

        // setup CBoxes
        JPanel factionCBoxSpring = new JPanel(new SpringLayout());

        baseCheckBox = new JCheckBox("Donate @ Unenroll");
        baseCheckBox.setToolTipText("<html>If checked, players that unenroll will donate<br>all their units to the house bays.</html>");
        baseCheckBox.setName("DonateUnitsUponUnenrollment");
        factionCBoxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Faction Names on Games");
        baseCheckBox.setToolTipText("<html>If checked, faction names will replace player names in<br>completed game descriptions.</html>");
        baseCheckBox.setName("ShowCompleteGameInfoOnTick");
        factionCBoxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Player Names in News");
        baseCheckBox.setToolTipText("<html>If checked, player names will replace faction names in<br>news feed description of games.</html>");
        baseCheckBox.setName("ShowCompleteGameInfoInNews");
        factionCBoxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Planets in MOTD");
        baseCheckBox.setToolTipText("If checked, players can use the new <planet> tags in their MOTD");
        baseCheckBox.setName("AllowPlanetsInMOTD");
        factionCBoxSpring.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Allow Links in MOTD");
        baseCheckBox.setToolTipText("If checked, players can add external links to their MOTD");
        baseCheckBox.setName("AllowLinksInMOTD");
        factionCBoxSpring.add(baseCheckBox);
        
        SpringLayoutHelper.setupSpringGrid(factionCBoxSpring, 3);
        
        //SubFaction Options
        JPanel subFactionMain = new JPanel(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));
        JPanel subFactionSpring0 = new JPanel();
        JPanel subFactionSpring = new JPanel(new SpringLayout());
        
        subFactionMain.setBorder(BorderFactory.createTitledBorder("SubFaction Options"));

        baseTextField = new JTextField(5);
        subFactionSpring0.add(new JLabel("Days Between Promotions:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Integer Field, How many days a player has to wait before they can be promoted again<br>after their last promotion/demotion.</html>");
        baseTextField.setName("daysbetweenpromotions");
        subFactionSpring0.add(baseTextField);

        baseCheckBox = new JCheckBox("Auto Promote Sub Factions");
        baseCheckBox.setToolTipText("<html>If checked, a player will be automatically promoted<br>to the next higher sub faction,<br>if they are qualified.</html>");
        baseCheckBox.setName("autoPromoteSubFaction");
        subFactionSpring.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Disable Player Demotion Notifications");
        baseCheckBox.setToolTipText("<html>If checked, House Leaders will not be notified <br>when a player no longer qualifies for a subfaction.</html>");
        baseCheckBox.setName("disableDemotionNotification");
        subFactionSpring.add(baseCheckBox);        
        
        baseCheckBox = new JCheckBox("Allow Subfaction Self Promotion");
        baseCheckBox.setToolTipText("If checked, a user will be allowed to self promote into a subfaction, can be used once.");
        baseCheckBox.setName("Self_Promote_Subfaction");
        subFactionSpring.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Disable Subfaction Std Promotion");
        baseCheckBox.setToolTipText("Meant to be enabled with self promote. Disables the normal methods of player subfaction promtion. Likely used with disable demotion to lock player into subfaction.");
        baseCheckBox.setName("Disable_Promote_Subfaction");
        subFactionSpring.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Disable Subfaction Std Demotion");
        baseCheckBox.setToolTipText("Disables the normal methods of player subfaction demotion. Likely used with disable premotion to lock player into subfaction.");
        baseCheckBox.setName("Disable_Demote_Subfaction");
        subFactionSpring.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Enforce Subfaction Factory Access");
        baseCheckBox.setToolTipText("If checked, subfaction level MUST be equal to factory level to use it");
        baseCheckBox.setName("Enforce_Subfaction_Factory_Access");
        subFactionSpring.add(baseCheckBox);
        
        baseCheckBox = new JCheckBox("Only Subfactions Can Activate");
        baseCheckBox.setToolTipText("Only players in subfactions can activate");
        baseCheckBox.setName("Activate_Subfaction_Only");
        subFactionSpring.add(baseCheckBox);
        
        
        SpringLayoutHelper.setupSpringGrid(subFactionSpring, 3);
        
        subFactionMain.add(subFactionSpring0);
        subFactionMain.add(subFactionSpring);

        // finalize the layout
        JPanel factionBox = new JPanel();
        factionBox.setLayout(new BoxLayout(factionBox, BoxLayout.Y_AXIS));
        JPanel factionSpringFlow = new JPanel();
        factionSpringFlow.add(factionSpring1);
        factionSpringFlow.add(factionSpring2);
        factionBox.add(factionSpringFlow);
        factionBox.add(factionCBoxSpring);
        factionBox.add(subFactionMain);

        add(factionBox);
	}
}
