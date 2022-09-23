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

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class MiscOptionsPanel extends JPanel {

	private static final long serialVersionUID = -5493634146928452778L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public MiscOptionsPanel() {
		super();
        /*
         * MISC PANEL CONSTRUCTION options that are difficult to categorize ...
         */
        JPanel miscBoxPanel = new JPanel();
        miscBoxPanel.setLayout(new BoxLayout(miscBoxPanel, BoxLayout.Y_AXIS));
        JPanel miscSpring1 = new JPanel(new SpringLayout());
        JPanel miscSpring2 = new JPanel(new SpringLayout());
        JPanel miscSpringGrid = new JPanel(new GridLayout(1, 2));
        JPanel miscCBoxSpring = new JPanel(new SpringLayout());

        // set up spring 1
        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Campaign Year:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Year of Campaign.  This will modify the TechLevel ratings in MM");
        baseTextField.setName("CampaignYear");
        miscSpring1.add(baseTextField);
        
        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Tick Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of Ticks, in ms");
        baseTextField.setName("TickTime");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Slice Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Length of Slices, in ms");
        baseTextField.setName("SliceTime");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Min Count BV:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum BV for an army to be counted during ticks and slices");
        baseTextField.setName("MinCountForTick");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Max Count BV:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Maximum BV for an army to be counted during ticks and slices");
        baseTextField.setName("MaxCountForTick");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Min Active Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of time (in seconds) a player must remain active before returning to reserve");
        baseTextField.setName("MinActiveTime");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Max Armies:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max num. of armies a player may have");
        baseTextField.setName("MaxLancesPerPlayer");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Base Army Weight:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("How much each army counts for while figuring production");
        baseTextField.setName("BaseCountForProduction");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Startup Miniticks:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Number of miniticks performed 1st time a server is run");
        baseTextField.setName("FreeMinticksOnStartup");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Allowed MM Version:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>MM version clients should have in order to host (and play)<br>This is Set by the server from the MM file in the server folder<br>Set to -1 to disable</html>");
        baseTextField.setName("AllowedMegaMekVersion");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Money Short Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>short name you want displayed for your servers money<br>i.e. CB for CBills</html>");
        baseTextField.setName("MoneyShortName");
        miscSpring1.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("Flu Short Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>short name you want displayed for your servers influence<br>i.e. flu for influence</html>");
        baseTextField.setName("FluShortName");
        miscSpring1.add(baseTextField);
        
        baseTextField = new JTextField(5);
        miscSpring1.add(new JLabel("RP Short Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>short name you want displayed for your server's RP</html>");
        baseTextField.setName("RPShortName");
        miscSpring1.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(miscSpring1, 2);

        // set up spring 2
        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Max Idle Time:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Max amount of time before an Idle playere is kicked<br>Set to 0 to turn off this option</html>");
        baseTextField.setName("MaxIdleTime");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Save Every X:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Save PLAYER data every X slices. Houses save on ticks.");
        baseTextField.setName("SaveEverySlice");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Min Merc XP:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum XP for a player to join a mercenary outfit");
        baseTextField.setName("MinEXPforMercenaries");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Min Contract Duration:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Minimum Merc contract duration. NOTE: Unit, PP, and Land are divided by 10.");
        baseTextField.setName("MinContractEXP");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Newbie House Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Name of training faction. Changing from SOL isn't recommended.");
        baseTextField.setName("NewbieHouseName");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Money Long Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Name for your Servers Money");
        baseTextField.setName("MoneyLongName");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Flu Long Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Name of your servers influence");
        baseTextField.setName("FluLongName");
        miscSpring2.add(baseTextField);
        
        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("RP Long Name:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Name of your server's RP");
        baseTextField.setName("RPLongName");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Last Backup Ran:", SwingConstants.TRAILING));
        baseTextField.enableInputMethods(false);
        baseTextField.setName("LastAutomatedBackup");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Hours between backups:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Number of hours between each backup.<br>The backup will run and create zip files for planets.dat<br>houses.dat and all of the playerfiles<br>The zip files will be stored in ./campaign/backup</html>");
        baseTextField.setName("AutomaticBackupHours");
        miscSpring2.add(baseTextField);

        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Purge Player Files:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Number of days a player is inactive before they get purged.</html>");
        baseTextField.setName("PurgePlayerFilesDays");
        miscSpring2.add(baseTextField);

        miscSpring2.add(baseTextField);
        baseTextField = new JTextField(5);
        miscSpring2.add(new JLabel("Min MOTD Exp:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>Min amount of EXP to set your houses MOTD</html>");
        baseTextField.setName("MinMOTDExp");
        miscSpring2.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(miscSpring2, 2);

        // set up CBoxen
        BaseCheckBox = new JCheckBox("IP Check");

        BaseCheckBox.setToolTipText("<HTML>" + "If checked, players who share an IP will not be" + "able to play games against each other, transfer" + "units, send money, etc.</HTML>");
        BaseCheckBox.setName("IPCheck");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Hide Active");

        BaseCheckBox.setToolTipText("If checked, all players are shown as active in the player list.");
        BaseCheckBox.setName("HideActiveStatus");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Hide ELO");
        BaseCheckBox.setToolTipText("If checked, rating/ELO will not be shown to players.");
        BaseCheckBox.setName("HideELO");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Ranks on Tick");
        BaseCheckBox.setToolTipText("Disable to stop showing Faction Ranks on Tick");
        BaseCheckBox.setName("ShowFactionRanks");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Veh Weightclass in challenges");
        BaseCheckBox.setToolTipText("Enable to show Veh Weightclass in challenges");
        BaseCheckBox.setName("ShowVehWeightclassInChallenges");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow Staff to See all Messages");
        BaseCheckBox.setToolTipText("<HTML>If checked all Staff Memebers<br>, despite user level, will be able to see all command messages<br>from other staff</html>");
        BaseCheckBox.setName("AllowLowerLevelUsersToSeeUpperLevelUsersDoings");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("House ticks done at slice");
        BaseCheckBox.setToolTipText("<HTML>If checked house ticks are done incrementally each slice from 1 to X<br>will be done depending on how many can be processed in the 1/2 the slice time.</html>");
        BaseCheckBox.setName("ProcessHouseTicksAtSlice");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Send single commands at a time");
        BaseCheckBox.setToolTipText("<HTML>If checked the first message in the message queue is sent to the player instead of appending<br>the whole queue to a single message sent to the player<br>NOTE: This could slow down the messages a player receives</html>");
        BaseCheckBox.setName("SendSingleCommandAtATime");
        miscCBoxSpring.add(BaseCheckBox);

        BaseCheckBox = new JCheckBox("Allow RP transfer");
        BaseCheckBox.setToolTipText("<HTML>Allow players to transfer reward points in the same manner as they can transfer cbills</html>");
        BaseCheckBox.setName("AllowRPTransfer");
        miscCBoxSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Allow Flu transfer");
        BaseCheckBox.setToolTipText("<HTML>Allow players to transfer influence in the same manner as they can transfer cbills</html>");
        BaseCheckBox.setName("AllowFluTransfer");
        miscCBoxSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Disconnect idle users");
        BaseCheckBox.setToolTipText("<html>Disconnect users after [MAXIDLETIME]?<br>Unchecked logs them out, but leaves them connected.</html>");
        BaseCheckBox.setName("DisconnectIdleUsers");
        miscCBoxSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Show full capacity in unit popups");
        BaseCheckBox.setToolTipText("<html>Show the complete capacity description for units in the overlay popup</html>");
        BaseCheckBox.setName("UseFullCapacityDescription");
        miscCBoxSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Show full capacity in detail display");
        BaseCheckBox.setToolTipText("<html>Show the complete capacity description for units when viewing</html>");
        BaseCheckBox.setName("UseFullCapacityInDetailDisplay");
        miscCBoxSpring.add(BaseCheckBox);
        
        BaseCheckBox = new JCheckBox("Enable Emojis");
        BaseCheckBox.setToolTipText("<html>allows use of the emoji commands, see /ec#list </html>");
        BaseCheckBox.setName("AllowEmoji");
        miscCBoxSpring.add(BaseCheckBox);
        
        
        SpringLayoutHelper.setupSpringGrid(miscCBoxSpring, 3);

        // finalize layout
        miscSpringGrid.add(miscSpring1);
        miscSpringGrid.add(miscSpring2);
        miscBoxPanel.add(miscSpringGrid);
        miscBoxPanel.add(miscCBoxSpring);
        add(miscBoxPanel);
	}

}