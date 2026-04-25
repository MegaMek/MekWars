/*
 * MekWars - Copyright (C) 2004
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

/**
 * @author jtighe
 * @author Spork
 *
 * Server Configuration Page. All new Server Options need to be added to this page or subPanels as well.
 */

package admin.dialog;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.jdatepicker.impl.JDatePickerImpl;

import admin.dialog.serverConfigDialogs.AdvancedRepairPanel;
import admin.dialog.serverConfigDialogs.ArtilleryPanel;
import admin.dialog.serverConfigDialogs.AutoProdPanel;
import admin.dialog.serverConfigDialogs.BattleValuePanel;
import admin.dialog.serverConfigDialogs.BlackMarketPanel;
import admin.dialog.serverConfigDialogs.ChristmasPanel;
import admin.dialog.serverConfigDialogs.CombatPanel;
import admin.dialog.serverConfigDialogs.DefectionPanel;
import admin.dialog.serverConfigDialogs.DirectSellPanel;
import admin.dialog.serverConfigDialogs.DisconnectionPanel;
import admin.dialog.serverConfigDialogs.DiscordAndDjangoPanel;
import admin.dialog.serverConfigDialogs.FactionPanel;
import admin.dialog.serverConfigDialogs.FactoryPurchasePanel;
import admin.dialog.serverConfigDialogs.FreebuildPanel;
import admin.dialog.serverConfigDialogs.InfluencePanel;
import admin.dialog.serverConfigDialogs.LinksPanel;
import admin.dialog.serverConfigDialogs.LossCompensationPanel;
import admin.dialog.serverConfigDialogs.MiniCampaignPanel;
import admin.dialog.serverConfigDialogs.MiscOptionsPanel;
import admin.dialog.serverConfigDialogs.NewbieHousePanel;
import admin.dialog.serverConfigDialogs.NoPlayPanel;
import admin.dialog.serverConfigDialogs.PathsPanel;
import admin.dialog.serverConfigDialogs.PayoutModPanel;
import admin.dialog.serverConfigDialogs.PilotSkillsCardPanel;
import admin.dialog.serverConfigDialogs.PilotSkillsPanel;
import admin.dialog.serverConfigDialogs.PilotsPanel;
import admin.dialog.serverConfigDialogs.ProductionPanel;
import admin.dialog.serverConfigDialogs.RepodPanel;
import admin.dialog.serverConfigDialogs.RewardPanel;
import admin.dialog.serverConfigDialogs.SchedulerPanel;
import admin.dialog.serverConfigDialogs.SinglePlayerFactionPanel;
import admin.dialog.serverConfigDialogs.TechnicianPanel;
import admin.dialog.serverConfigDialogs.TechnologyResearchPanel;
import admin.dialog.serverConfigDialogs.TrackerPanel;
import admin.dialog.serverConfigDialogs.UnitLimitsPanel;
import admin.dialog.serverConfigDialogs.UnitResearchPanel;
import admin.dialog.serverConfigDialogs.UnitsCardPanel;
import admin.dialog.serverConfigDialogs.UnitsPanel;
import admin.dialog.serverConfigDialogs.VotingPanel;
import client.MWClient;
import common.util.MWLogger;

public final class ServerConfigurationDialog implements ActionListener {

    private final static String okayCommand = "okay";
    private final static String cancelCommand = "cancel";
    private final static String windowName = "MekWars Server Configuration";

    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");

    private JDialog dialog;
    private JOptionPane pane;

    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

    MWClient mwclient = null;

    private Dimension screenSize;

    /**
     * @author jtighe Opens the server config page in the client.
     * @param client
     */

    /**
     * @author Torren (Jason Tighe) 12/29/2005 I've completely redone how the Server config dialog works There are 2 basic fields now baseTextField which is a
     *         JTextField and baseCheckBox which is a JCheckBox. When you add a new server config add the labels to the tab then use the base fields to add the
     *         ver. make sure to set the base field's name method this is used to populate and save. ex: BaseTextField.setName("DefaultServerOptionsVariable");
     *         Two recursive methods populate and save the data to the server findAndPopulateTextAndCheckBoxes(JPanel) findAndSaveConfigs(JPanel) This change to
     *         the code removes the tediousness of having to add a new var to 3 locations when it is use. Now only 1 location needs to added and that is the
     *         vars placement on the tab in the UI.
     *
     * @author Spork - refactored this completely, breaking each panel into its own class.
     *         This file was well over 5000 lines long, impossible to find anything in.
     */
    public ServerConfigurationDialog(MWClient mwclient) {

        this.mwclient = mwclient;

        // Get the screen dimensions - the Units tab is too tall for smaller than 1280 x 1024
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        screenSize = toolkit.getScreenSize();
        
        // @salient adding tooltip settings
		ToolTipManager.sharedInstance().setDismissDelay(7000);
		ToolTipManager.sharedInstance().setInitialDelay(400);
		//ToolTipManager.sharedInstance().setReshowDelay(100);

        // TAB PANELS (these are added to the root pane as tabs)
        JPanel unitsPanel;
        JPanel pilotSkillsPanel;
        PathsPanel pathsPanel = new PathsPanel();// file paths
        InfluencePanel influencePanel = new InfluencePanel(mwclient);// influence settings
        RepodPanel repodPanel = new RepodPanel(mwclient);
        TechnicianPanel technicianPanel = new TechnicianPanel(mwclient);
        if (screenSize.height >= 1024) {
        	unitsPanel = new UnitsPanel(mwclient);
        	pilotSkillsPanel = new PilotSkillsPanel(mwclient);
        } else {
        	unitsPanel = new UnitsCardPanel(mwclient);
        	pilotSkillsPanel = new PilotSkillsCardPanel(mwclient);
        }
        FactionPanel factionPanel = new FactionPanel(mwclient);
        DirectSellPanel directSellPanel = new DirectSellPanel();
        NewbieHousePanel newbieHousePanel = new NewbieHousePanel();
        VotingPanel votingPanel = new VotingPanel();
        AutoProdPanel autoProdPanel = new AutoProdPanel(mwclient); // Autoproduction
        CombatPanel combatPanel = new CombatPanel();// mm options, etc
        UnitLimitsPanel unitLimitsPanel = new UnitLimitsPanel(); // Set limits on units in a player's hangar
        ProductionPanel productionPanel = new ProductionPanel();// was factoryOptions
        BlackMarketPanel blackMarketPanel = new BlackMarketPanel(mwclient);
        RewardPanel rewardPanel = new RewardPanel(mwclient);
        FactoryPurchasePanel factoryPurchasePanel = new FactoryPurchasePanel(mwclient); // Allow players to purchase new factories.
        DefectionPanel defectionPanel = new DefectionPanel();// control defection access, losses therefrom, etc.
        MiscOptionsPanel miscOptionsPanel = new MiscOptionsPanel();// things which can't be easily categorized
        UnitResearchPanel unitResearchPanel = new UnitResearchPanel(mwclient); // Research Unit Panel
        ArtilleryPanel artilleryPanel = new ArtilleryPanel();
        TechnologyResearchPanel technologyResearchPanel = new TechnologyResearchPanel(mwclient); // Technology Reseach Panel
        BattleValuePanel battleValuePanel = new BattleValuePanel();// mekwars BV adjustments
        SinglePlayerFactionPanel singlePlayerFactionPanel = new SinglePlayerFactionPanel(); // Single Player Faction Configs
        DisconnectionPanel disconnectionPanel = new DisconnectionPanel();
        DiscordAndDjangoPanel discordAndDjangoPanel = new DiscordAndDjangoPanel(); //@salient
        PilotsPanel pilotsPanel = new PilotsPanel(mwclient);// allows SO's set up pilot options and personal pilot queue options
        NoPlayPanel noPlayPanel = new NoPlayPanel(mwclient);
        AdvancedRepairPanel advancedRepairPanel = new AdvancedRepairPanel();// Advanced Repair
        LossCompensationPanel lossCompensationPanel = new LossCompensationPanel();// battle loss compensation
        PayoutModPanel payoutModPanel = new PayoutModPanel();
        ChristmasPanel christmasPanel = new ChristmasPanel();
        SchedulerPanel schedulerPanel = new SchedulerPanel();
        LinksPanel linksPanel = new LinksPanel(); // @salient
        FreebuildPanel freebuildPanel = new FreebuildPanel(mwclient); // @salient
        MiniCampaignPanel miniCampaignPanel = new MiniCampaignPanel(mwclient); // @salient
        TrackerPanel trackerPanel = new TrackerPanel(mwclient.getserverConfigs("TrackerUUID"));

        // Set the actions to generate
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);

        /*
         * NEW OPTIONS - need to be sorted into proper menus.
         */

        // Set tool tips (balloon help)
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving options");

        ConfigPane.addTab("Advanced Repairs", null, advancedRepairPanel, "For all your Unit Care needs");
        ConfigPane.addTab("Autoproduction", null, autoProdPanel, "Set type and details of factory autoproduction");
        ConfigPane.addTab("Black Market", null, blackMarketPanel, "Black Market access controls");
        ConfigPane.addTab("BV Options", null, battleValuePanel, "Battle Value");
        ConfigPane.addTab("Christmas Units", null, christmasPanel, "Merry Christmas!");
        ConfigPane.addTab("Combat", null, combatPanel, "Combat");
        ConfigPane.addTab("Defection", null, defectionPanel, "Defection configuration");
        ConfigPane.addTab("Direct Sales", null, directSellPanel, "Units - the lifeblood of the game");
        ConfigPane.addTab("Disconnection", null, disconnectionPanel, "Disconnection autoresolution settings");
        ConfigPane.addTab("Discord & Django", null, discordAndDjangoPanel, "Discord & Django"); //@salient
        ConfigPane.addTab("Faction", null, factionPanel, "House Stuff");
        ConfigPane.addTab("Factory Options", null, productionPanel, "Factories That Can Do");
        ConfigPane.addTab("Factory Purchase", null, factoryPurchasePanel, "Factories For Sale");
        ConfigPane.addTab("File Paths", null, pathsPanel, "Paths");
        ConfigPane.addTab("Free Build", null, freebuildPanel, "Free Build"); //@salient
        ConfigPane.addTab("Influence", null, influencePanel, "Influence");
        ConfigPane.addTab("Links & Rules", null, linksPanel, "Configure/Enable Links Area and Rules Panel"); //@salient
        ConfigPane.addTab("Loss Compensation", null, lossCompensationPanel, "Extra Payments for salvaged/destroyed units.");
        ConfigPane.addTab("Mini Campaigns", null, miniCampaignPanel, "Mini Campaigns"); //@salient
        ConfigPane.addTab("Misc Options", null, miscOptionsPanel, "Misc Stuff");
        ConfigPane.addTab("No Play", null, noPlayPanel, "Personal Blacklist/Exclusion options");
        ConfigPane.addTab("Payout Mods", null, payoutModPanel, "Modifications to ops payout");
        ConfigPane.addTab("Pilots", null, pilotsPanel, "Pilot Options");
        ConfigPane.addTab("Pilot Skills", null, pilotSkillsPanel, "Server Configurable Pilot Skills");
        ConfigPane.addTab("Repodding", null, repodPanel, "Repod");
        ConfigPane.addTab("Rewards", null, rewardPanel, "Reward Points");
        ConfigPane.addTab("Scheduler",  null, schedulerPanel, "Repeating Tasks");
        ConfigPane.addTab("Single Player", null, singlePlayerFactionPanel, "Single Player Faction Configuration");
        ConfigPane.addTab("SOL Units", null, newbieHousePanel, "SOL Units and Attack Limits");
        ConfigPane.addTab("Support Units", null, artilleryPanel, "Artillery and Gun Emplacements and Mines oh my!");
        ConfigPane.addTab("Techs", null, technicianPanel, "Techs");
        ConfigPane.addTab("Tech Research", null, technologyResearchPanel, "Technology Research Configuration");
        ConfigPane.addTab("Tracker", null, trackerPanel, "Comstar HPGNet - enabling inter-server communiation and activity tracking");
        ConfigPane.addTab("Voting", null, votingPanel, "Voting Stuff");
        ConfigPane.addTab("Unit Limits", null, unitLimitsPanel, "Limits to unit ownership based on unit weightclass");
        ConfigPane.addTab("Unit Research", null, unitResearchPanel, "Unit Research Configuration");
        ConfigPane.addTab("Units", null, unitsPanel, "Consolidated Unit Information");
        // Create the panel that will hold the entire UI
        JPanel mainConfigPanel = new JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainConfigPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);

        // load any changes someone else might have made.
        mwclient.getServerConfigData();

        for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
            JPanel panel = (JPanel) ConfigPane.getComponent(pos);
            findAndPopulateTextAndCheckBoxes(panel);

        }

        // Show the dialog and get the user's input
        dialog.setLocationRelativeTo(mwclient.getMainFrame());
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            for (int pos = ConfigPane.getComponentCount() - 1; pos >= 0; pos--) {
                JPanel panel = (JPanel) ConfigPane.getComponent(pos);
                findAndSaveConfigs(panel);
            }
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminSaveServerConfigs");
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c CampaignConfig");

            mwclient.reloadData();

        } else {
            dialog.dispose();
        }
    }

    /**
     * This Method tunnels through all of the panels to find the textfields and checkboxes. Once it find one it grabs the Name() param of the object and uses
     * that to find out what the setting should be from the mwclient.getserverConfigs() method.
     *
     * @param panel
     */
    public void findAndPopulateTextAndCheckBoxes(JPanel panel) {
        String key = null;

        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            if (field instanceof JPanel && !(field instanceof JDatePickerImpl)) {
                findAndPopulateTextAndCheckBoxes((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                key = textBox.getName();
                if (key == null) {
                    continue;
                }

                textBox.setMaximumSize(new Dimension(100, 10));
                try {
                    // bad hack need to format the message for the last time the
                    // backup happened
                    if (key.equals("LastAutomatedBackup")) {
                        SimpleDateFormat sDF = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
                        Date date = new Date(Long.parseLong(mwclient.getserverConfigs(key)));
                        textBox.setText(sDF.format(date));
                    } else {
                        textBox.setText(mwclient.getserverConfigs(key));
                    }
                } catch (Exception ex) {
                    textBox.setText("N/A");
                }
            } else if (field instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) field;

                key = checkBox.getName();
                if (key == null) {
                    MWLogger.errLog("Null Checkbox: " + checkBox.getToolTipText());
                    continue;
                }
                checkBox.setSelected(Boolean.parseBoolean(mwclient.getserverConfigs(key)));

            } else if (field instanceof JRadioButton) {
                JRadioButton radioButton = (JRadioButton) field;

                key = radioButton.getName();
                if (key == null) {
                    MWLogger.errLog("Null RadioButton: " + radioButton.getToolTipText());
                    continue;
                }
                radioButton.setSelected(Boolean.parseBoolean(mwclient.getserverConfigs(key)));

            } else if (field instanceof JDatePickerImpl) {
            	JDatePickerImpl picker = (JDatePickerImpl) field;

            	key = picker.getName();
            	if (key == null) {
            		MWLogger.errLog("Null JDatePickerImpl: " + picker.getToolTipText());
            		continue;
            	}
            	String s = mwclient.getserverConfigs(key);
            	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            	Date date = new Date();
				try {
					date = sdf.parse(s);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            	Calendar cal = Calendar.getInstance();
            	cal.setTime(date);
            	int year = cal.get(Calendar.YEAR);
            	int month = cal.get(Calendar.MONTH);
            	int day = cal.get(Calendar.DAY_OF_MONTH);

            	picker.getModel().setYear(year);
            	picker.getModel().setMonth(month);
            	picker.getModel().setDay(day);
            	picker.getModel().setSelected(true);

            } else if (field instanceof JScrollPane) {
            	JScrollPane pane = (JScrollPane) field;
            	JTextArea area = (JTextArea)pane.getViewport().getView();
            	key = area.getName();
            	if(key == null) {
            		continue;
            	}
            	String s = mwclient.getserverConfigs(key);
            	String text = s.replace('$', '\n');
            	area.setText(text);
            } // else continue
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to find any changed text fields or checkboxes. Then it will send the new configs to
     * the server.
     *
     * @param panel
     */
    public void findAndSaveConfigs(JPanel panel) {
        String key = null;
        String value = null;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof JPanel && !(field instanceof JDatePickerImpl)) {
                findAndSaveConfigs((JPanel) field);
            } else if (field instanceof JTextField) {
                JTextField textBox = (JTextField) field;

                value = textBox.getText();
                key = textBox.getName();

                if (key == null || value == null) {
                    continue;
                }

                // don't need to save this the system does it on its own
                // --Torren.
                if (key.equals("LastAutomatedBackup")) {
                    continue;
                }

                // reduce bandwidth only send things that have changed.
                if (!mwclient.getserverConfigs(key).equalsIgnoreCase(value)) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminChangeServerConfig#" + key + "#" + value + "#CONFIRM");
                }
            } else if (field instanceof JCheckBox) {
                JCheckBox checkBox = (JCheckBox) field;

                value = Boolean.toString(checkBox.isSelected());
                key = checkBox.getName();

                if (key == null || value == null) {
                    continue;
                }
                // reduce bandwidth only send things that have changed.
                if (!mwclient.getserverConfigs(key).equalsIgnoreCase(value)) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminChangeServerConfig#" + key + "#" + value + "#CONFIRM");
                }
            } else if (field instanceof JRadioButton) {
                JRadioButton radioButton = (JRadioButton) field;

                value = Boolean.toString(radioButton.isSelected());
                key = radioButton.getName();

                if (key == null || value == null) {
                    continue;
                }
                // reduce bandwidth only send things that have changed.
                if (!mwclient.getserverConfigs(key).equalsIgnoreCase(value)) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminChangeServerConfig#" + key + "#" + value + "#CONFIRM");
                }
            } else if (field instanceof JDatePickerImpl) {
            	JDatePickerImpl picker = (JDatePickerImpl) field;
            	value = picker.getJFormattedTextField().getText();
            	key = picker.getName();
            	// reduce bandwidth only send things that have changed.
                if (!mwclient.getserverConfigs(key).equalsIgnoreCase(value)) {
                    mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminChangeServerConfig#" + key + "#" + value + "#CONFIRM");
                }
            } else if (field instanceof JScrollPane) {
            	JScrollPane pane = (JScrollPane) field;
            	JTextArea area = (JTextArea)pane.getViewport().getView();
            	value = area.getText();
            	key = area.getName();
            	if (!mwclient.getserverConfigs(key).equalsIgnoreCase(value)) {
            		String toSend = value.replace('\n', '$');
            		mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "c AdminChangeServerConfig#" + key + "#" + toSend + "#CONFIRM");
            	}
            } // else continue
        }
    }

    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        }
    }
}
