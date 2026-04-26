/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.common.gui.dialogs;

import mekwars.common.VerticalLayout;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public final class ConfigurationDialog implements java.awt.event.ActionListener {

    // store the client backlink for other things to use
    private final IClient client;

    private final static String okayCommand = "Okay";
    private final static String cancelCommand = "Cancel";
    private final static String camoCommand = "Camo";
    private final static String lookAndFeelCommand = "LAF";

    private final static String windowName = "MekWars Configuration";

    // BUTTONS
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    private final javax.swing.JButton camoButton = new javax.swing.JButton("Select Camo");

    // TEXT FIELDS
    // tab names
    private final javax.swing.JTextField hqTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField rulesTabNameField = new javax.swing.JTextField(10); //@salient
    private final javax.swing.JTextField bmTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField bmeTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField hsTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField batTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField mapTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField mcTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField hmTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField pmTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField pLogTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField sysLogTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField miscTabNameField = new javax.swing.JTextField(10);
    private final javax.swing.JTextField RPGTabNameField = new javax.swing.JTextField(10);

    // tab mnemonic
    private final javax.swing.JTextField hqTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField rulesTabMnemonicField = new javax.swing.JTextField(1); //@salient
    private final javax.swing.JTextField bmTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField bmeTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField hsTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField batTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField mapTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField mcTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField hmTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField pmTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField pLogTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField sysLogTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField miscTabMnemonicField = new javax.swing.JTextField(1);
    private final javax.swing.JTextField RPGTabMnemonicField = new javax.swing.JTextField(1);

    // user config pane text fields
    private final javax.swing.JTextField uNameField = new javax.swing.JTextField(11);
    private final javax.swing.JPasswordField passwordField = new javax.swing.JPasswordField();
    private final javax.swing.JTextField chatNameColorField = new javax.swing.JTextField();
    private final javax.swing.JTextField foregroundColorField = new javax.swing.JTextField();
    private final javax.swing.JTextField backgroundColorField = new javax.swing.JTextField();
    private final javax.swing.JTextField chatFontField = new javax.swing.JTextField();
    private final javax.swing.JTextField defaultArmyNameField = new javax.swing.JTextField();
    private final javax.swing.JTextField mapOverLayField = new javax.swing.JTextField(3);

    // non-campaign GUI options (divider, etc);
    private final javax.swing.JTextField hqColumnsField = new javax.swing.JTextField(3);
    private final javax.swing.JCheckBox showUnitTechBaseCheckBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox showUnitBaseBVCheckBox = new javax.swing.JCheckBox();
    // keywords
    private final javax.swing.JTextField keywordsField = new javax.swing.JTextField();

    // ignore list fields, etc.
    private final javax.swing.JTextField ignorePublicField = new javax.swing.JTextField();
    private final javax.swing.JTextField ignoreHouseField = new javax.swing.JTextField();
    private final javax.swing.JTextField ignorePrivateField = new javax.swing.JTextField();
    private final javax.swing.JTextField challengeStringField = new javax.swing.JTextField();
    private final javax.swing.JTextField maxMailTabStringField = new javax.swing.JTextField();
    private final javax.swing.JTextField maxNumberOfMailTabsField = new javax.swing.JTextField();

    // paths to sound files
    private final javax.swing.JTextField soundOnCallField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnKeywordField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnMessageField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnAttackField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnBMWinField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnActivateField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnDeactivateField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnEnemyDetectedField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnExitClientField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnMenuPopupField = new javax.swing.JTextField();
    private final javax.swing.JTextField soundOnMenuField = new javax.swing.JTextField();

    // host options
    private final javax.swing.JTextField maxPlayersField = new javax.swing.JTextField(11);
    private final javax.swing.JTextField hostCommentsField = new javax.swing.JTextField(11);

    // function keys
    private final javax.swing.JTextField f1Field = new javax.swing.JTextField(30);
    private final javax.swing.JTextField f2Field = new javax.swing.JTextField(30);
    private final javax.swing.JTextField f3Field = new javax.swing.JTextField(30);
    private final javax.swing.JTextField f4Field = new javax.swing.JTextField(30);
    private final javax.swing.JTextField f5Field = new javax.swing.JTextField(30);

    // COMBO BOXES
    private final String[] schemeChoices = { "Grey", "Tan", "Classic" };
    private final javax.swing.JComboBox schemeComboBox = new javax.swing.JComboBox(schemeChoices);

    private final String[] lookandFeelChoices = { "Native", "CDE/Motif", "Java/Ocean", "Metouia", "Plastic (Desert)",
                                                  "Plastic (Sky)", "Plastic XP", "Steel", "Windows - J", "Skins" };
    private final javax.swing.JComboBox lookandfeelComboBox = new javax.swing.JComboBox(lookandFeelChoices);

    private final String[] playerChatColorChoices = { "Player Defined", "Faction Colors", "Mixed (Faction Tag)",
                                                      "Mixed (Faction Name)" };
    private final javax.swing.JComboBox playerChatColorComboBox = new javax.swing.JComboBox(playerChatColorChoices);

    private final String[] sysMessageColorChoices = { "Dark Green", "Gold", "Indigo", "Navy", "Orange", "Red", "Teal",
                                                      "Black" };
    private final javax.swing.JComboBox sysMessageColorComboBox = new javax.swing.JComboBox(sysMessageColorChoices);

    private final String[] playerMessageTabChoices = { "Main", "Misc", "System", "Personal" };
    private final javax.swing.JComboBox playerMessageTabComboBox = new javax.swing.JComboBox(playerMessageTabChoices);

    private javax.swing.JComboBox skinComboBox = null;
    // CHECK BOXEN
    // tab visibility
    private final javax.swing.JCheckBox hqTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rulesTabVisBox = new javax.swing.JCheckBox(); //@salient , top only?
    private final javax.swing.JCheckBox bmTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox bmeTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox hsTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox batTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox mapTabVisBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox hmTabVisBox = new javax.swing.JCheckBox();// bottom only
    private final javax.swing.JCheckBox pmTabVisBox = new javax.swing.JCheckBox();// bottom only
    private final javax.swing.JCheckBox pLogTabVisBox = new javax.swing.JCheckBox();// bottom only
    private final javax.swing.JCheckBox sysLogTabVisBox = new javax.swing.JCheckBox();// bottom only
    private final javax.swing.JCheckBox miscTabVisBox = new javax.swing.JCheckBox();// bottom only
    private final javax.swing.JCheckBox RPGTabVisBox = new javax.swing.JCheckBox();// bottom only

    // tab location
    private final javax.swing.JCheckBox hqTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rulesTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox bmTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox bmeTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox hsTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox batTabonTopBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox mapTabonTopBox = new javax.swing.JCheckBox();

    // user options
    private final javax.swing.JCheckBox timeStampBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox showHexinHQBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox useStatusForIconBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox darkenMapBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox bmPreviewImageBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox autoConnectBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox viewUnitFluffBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox viewLogoBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox armyPopUpBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox autoReOrder = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox testBuildTableBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox expandedUnitToolTipBox = new javax.swing.JCheckBox();

    // chat options
    private final javax.swing.JCheckBox hmInMainBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox pmInMainBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox miscInMainBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox RPGInMainBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox sysLogInMainBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox pmReplyToSender = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox pmReplyToReciever = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox pmUseMultipleTabs = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox colorEmotesBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox showEnterExitBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox blockImagesBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox mapOnClickBox = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnCall = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnMessage = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnAttack = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnKeyword = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnBMWin = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnActivate = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnDeactivate = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnEnemyDetected = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnExitClient = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnMenuPopup = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox enableSoundOnMenu = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox systemMessageKeyword = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox invertChatColors = new javax.swing.JCheckBox("Invert Chat Colors");

    // Dedicated Setup Tab
    private final javax.swing.JCheckBox enableDedicatedServerCB = new javax.swing.JCheckBox();
    private final javax.swing.JTextField portField = new javax.swing.JTextField();
    private final javax.swing.JTextField nameField = new javax.swing.JTextField();
    private final javax.swing.JTextField restartField = new javax.swing.JTextField();
    private final javax.swing.JTextField ownersField = new javax.swing.JTextField();
    private final javax.swing.JTextField memoryField = new javax.swing.JTextField();
    private final javax.swing.JTextField socketTimeOutField = new javax.swing.JTextField();

    // Unit Status Icons
    private final javax.swing.JCheckBox leftColumnDynamicCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftPilotEjectCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftRepairCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftEngineCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftEquipmentCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftArmorCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftAmmoCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox leftCommanderCB = new javax.swing.JCheckBox();

    private final javax.swing.JCheckBox rightColumnDynamicCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightPilotEjectCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightRepairCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightEngineCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightEquipmentCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightArmorCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightAmmoCB = new javax.swing.JCheckBox();
    private final javax.swing.JCheckBox rightCommanderCB = new javax.swing.JCheckBox();

    // STOCK DIALOG AND PANE
    private javax.swing.JDialog dialog;
    private javax.swing.JOptionPane pane;

    javax.swing.JTabbedPane ConfigPane = new javax.swing.JTabbedPane(javax.swing.SwingConstants.TOP);

    public ConfigurationDialog(IClient client) {

        // save the client
        this.client = client;

        // stored values.
        int originalColumns = Integer.parseInt(this.client.getConfigParam("UNITAMOUNT"));
        String originalUnitHex = this.client.getConfigParam("UNITHEX");
        String originalScheme = this.client.getConfigParam("HQCOLORSCHEME").toLowerCase();
        String originalLookAndFeel = this.client.getConfigParam("LOOKANDFEEL").toLowerCase();
        String originalSkin = this.client.getConfigParam("LOOKANDFEELSKIN").toLowerCase();
        // String originalMapBrightness =
        // client.getConfigParam("DARKERMAP").toLowerCase();
        String originalBMPreview = this.client.getConfigParam("BMPREVIEWIMAGE").toLowerCase();

        // Set the tooltips and actions for dialouge buttons
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        camoButton.setActionCommand(camoCommand);
        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        camoButton.addActionListener(this);
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving changes");
        camoButton.setToolTipText("Select Camo to be Preloaded in MegaMek");

        // CREATE THE PANELS
        javax.swing.JPanel playerPanel = new javax.swing.JPanel();// player name, etc
        javax.swing.JPanel chatPanel = new javax.swing.JPanel();// ignore options, into main options
        javax.swing.JPanel soundPanel = new javax.swing.JPanel();// sound options
        javax.swing.JPanel tabVisibilityPanel = new javax.swing.JPanel();// tab visibility and redirects
        javax.swing.JPanel tabNamingPanel = new javax.swing.JPanel();// naming and mnemonics
        javax.swing.JPanel keyBindPanel = new javax.swing.JPanel();// Funtion key binds
        javax.swing.JPanel dedicatedHostPanel = new javax.swing.JPanel();// Dedicated Host Panel
        javax.swing.JPanel unitHUDLayoutPanel = new javax.swing.JPanel();// Unit Status Panel
        javax.swing.JPanel devPanel = new javax.swing.JPanel(); // Dev options
        javax.swing.JPanel miscPanel = new javax.swing.JPanel(); // @salient - misc options

        /*
         * Format the PLAYER panel. Spring layout.
         */
        playerPanel.setLayout(new javax.swing.BoxLayout(playerPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JPanel playerFieldsPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel playerFieldsWrapper = new javax.swing.JPanel();
        playerFieldsWrapper.add(playerFieldsPanel);
        javax.swing.JPanel playerLowerCBoxesPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel playerUpperCBoxesPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // Make a dimension which sets the max vertical size same
        // as min vertical size.
        java.awt.Dimension newDim = new java.awt.Dimension();
        newDim.setSize(uNameField.getMaximumSize().getWidth(), uNameField.getMaximumSize().getHeight());
        // newDim.setSize(90,100);

        playerFieldsPanel.add(new javax.swing.JLabel("User Name:", javax.swing.SwingConstants.TRAILING));
        uNameField.setMaximumSize(newDim);
        uNameField.setToolTipText(
              "<HTML><BODY>User Name. Leave blank to be prompted<br>for a username/pass when connecting.</BODY></HTML>");
        playerFieldsPanel.add(uNameField);

        playerFieldsPanel.add(new javax.swing.JLabel("Password:", javax.swing.SwingConstants.TRAILING));
        passwordField.setMaximumSize(newDim);
        passwordField.setToolTipText("Account Password");
        playerFieldsPanel.add(passwordField);

        playerFieldsPanel.add(new javax.swing.JLabel("Name Color:", javax.swing.SwingConstants.TRAILING));
        chatNameColorField.setMaximumSize(newDim);
        chatNameColorField.setToolTipText(
              "<HTML>Chat name colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(chatNameColorField);

        playerFieldsPanel.add(new javax.swing.JLabel("Text Color:", javax.swing.SwingConstants.TRAILING));
        foregroundColorField.setMaximumSize(newDim);
        foregroundColorField.setToolTipText(
              "<HTML>Foreground text colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(foregroundColorField);

        playerFieldsPanel.add(new javax.swing.JLabel("Back Ground:", javax.swing.SwingConstants.TRAILING));
        backgroundColorField.setMaximumSize(newDim);
        backgroundColorField.setToolTipText(
              "<HTML>Background colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(backgroundColorField);

        playerFieldsPanel.add(new javax.swing.JLabel("Chat Font:", javax.swing.SwingConstants.TRAILING));
        chatFontField.setMaximumSize(newDim);
        chatFontField.setToolTipText("<HTML>Chat font size. Use +10 -10 or even size number</HTML>");
        playerFieldsPanel.add(chatFontField);

        if (this.client.getConfig().isParam("HQTABVISIBLE")) {
            playerFieldsPanel.add(new javax.swing.JLabel("HQ Columns:", javax.swing.SwingConstants.TRAILING));
            hqColumnsField.setMaximumSize(newDim);
            hqColumnsField.setToolTipText("Number of columns in Headquarters");
            playerFieldsPanel.add(hqColumnsField);

            playerFieldsPanel.add(new javax.swing.JLabel("New Army Name:", javax.swing.SwingConstants.TRAILING));
            defaultArmyNameField.setMaximumSize(newDim);
            defaultArmyNameField.setToolTipText("<HTML>" +
                                                      "Name given to each newly created army. Useful<br>" +
                                                      "for starter strings. For example, start all new<br>" +
                                                      "armies as 'Tau Ceti Rangers,' adding lance and<br>" +
                                                      "company descriptions afterwards.</HTML>");
            playerFieldsPanel.add(defaultArmyNameField);

        }

        if (new java.io.File("./data/mapoverlay.txt").exists()) {
            playerFieldsPanel.add(new javax.swing.JLabel("Map Overlay Color:", javax.swing.SwingConstants.TRAILING));
            mapOverLayField.setToolTipText("<HTML>Set the color of the star maps overlaying boarders</HTML>");
            playerFieldsPanel.add(mapOverLayField);
        }

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(playerFieldsPanel, 2);

        // add CBoxes, if relevant. keep a counter and determine how to format
        // at end.
        int upperCBoxesCounter = 0;

        if (this.client.getConfig().isParam("HQTABVISIBLE")) {
            showHexinHQBox.setText("Hexes in HQ");
            showHexinHQBox.setToolTipText("If enabled, hexes will be shows under units in HQ.");
            playerUpperCBoxesPanel.add(showHexinHQBox);
            upperCBoxesCounter++;
        }

        if (this.client.getConfig().isParam("BMTABVISIBLE")) {
            bmPreviewImageBox.setText("BM Preview");
            bmPreviewImageBox.setToolTipText("Check to show a unit preview image in BM tab.");
            playerUpperCBoxesPanel.add(bmPreviewImageBox);
            upperCBoxesCounter++;
        }

        if (this.client.getConfig().isParam("MAPTABVISIBLE")) {
            darkenMapBox.setText("Darken Map");
            darkenMapBox.setToolTipText("Check to reduce brightness of planets on map.");
            playerUpperCBoxesPanel.add(darkenMapBox);
            upperCBoxesCounter++;
        }

        // only show the status bit if map, bm or something else is visible.
        // if all are turned off, we're probably on a non-campaign server and
        // status is irrelevant
        if (upperCBoxesCounter != 0) {
            useStatusForIconBox.setText("Status For Icon");
            useStatusForIconBox.setToolTipText("<HTML>" +
                                                     "If checked, the server icon will be replaced with<br>" +
                                                     "an activity status image. This can be helpful, but<br>" +
                                                     "can make it harder to find the client window when<br>" +
                                                     "moving between multiple servers or programs.<br>" +
                                                     "<br>" +
                                                     "REQUIRES RESTART. DOES NOT WORK ON ALL PLATFORMS!</HTML>");
            playerUpperCBoxesPanel.add(useStatusForIconBox);
            upperCBoxesCounter++;
        }

        showUnitTechBaseCheckBox.setText("Show Unit Tech Base");
        showUnitTechBaseCheckBox.setToolTipText(
              "<html>When checked, unit Tool Tip will include Clan/IS indication.</html>");
        playerUpperCBoxesPanel.add(showUnitTechBaseCheckBox);

        showUnitBaseBVCheckBox.setText("Show Base BV");
        showUnitBaseBVCheckBox.setToolTipText(
              "<html>If selected, BV without pilot skills will be shown in the unit display</html>");
        playerUpperCBoxesPanel.add(showUnitBaseBVCheckBox);

        SpringLayoutHelper.setupSpringGrid(playerUpperCBoxesPanel, 2);

        // set up the color scheme panel/radio buttons
        javax.swing.JPanel schemeWrapper = new javax.swing.JPanel();
        schemeWrapper.setLayout(new javax.swing.BoxLayout(schemeWrapper, javax.swing.BoxLayout.Y_AXIS));

        java.awt.Dimension comboDim = new java.awt.Dimension();
        comboDim.setSize(lookandfeelComboBox.getMinimumSize().getWidth() * 1.6,
              uNameField.getMinimumSize().getHeight() + 2);
        lookandfeelComboBox.addActionListener(this);
        lookandfeelComboBox.setActionCommand(lookAndFeelCommand);

        javax.swing.JLabel schemeHeader = new javax.swing.JLabel("HQ Color Scheme:");
        schemeHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        schemeWrapper.add(schemeHeader);
        schemeWrapper.add(schemeComboBox);
        schemeComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        schemeComboBox.setMaximumSize(comboDim);

        javax.swing.JPanel statusWrapper = new javax.swing.JPanel();
        statusWrapper.setLayout(new javax.swing.BoxLayout(statusWrapper, javax.swing.BoxLayout.Y_AXIS));

        // set up the system skin combo box
        javax.swing.JPanel skinWrapper = new javax.swing.JPanel();
        skinWrapper.setLayout(new javax.swing.BoxLayout(skinWrapper, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JLabel skinHeader = new javax.swing.JLabel("Look and Feel:");
        skinHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        skinWrapper.add(skinHeader);
        skinWrapper.add(lookandfeelComboBox);
        lookandfeelComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        lookandfeelComboBox.setMaximumSize(comboDim);

        java.io.File skinFiles = new java.io.File("./data/skins");

        if (!skinFiles.exists()) {
            skinFiles.mkdir();
        }

        skinComboBox = new javax.swing.JComboBox(skinFiles.list());

        for (int pos = 0; pos < skinComboBox.getItemCount(); pos++) {
            if (skinComboBox.getItemAt(pos).toString().equalsIgnoreCase(originalSkin)) {
                skinComboBox.setSelectedIndex(pos);
                break;
            }
        }
        skinComboBox.setEnabled(false);
        skinWrapper.add(new javax.swing.JLabel("Skins:", javax.swing.SwingConstants.CENTER));
        skinWrapper.add(skinComboBox);
        skinComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        skinComboBox.setMaximumSize(comboDim);

        // sys message combo box
        javax.swing.JPanel sysMessageWrapper = new javax.swing.JPanel();
        sysMessageWrapper.setLayout(new javax.swing.BoxLayout(sysMessageWrapper, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JLabel sysMesHeader = new javax.swing.JLabel("System Message Color:");
        sysMesHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        sysMessageWrapper.add(sysMesHeader);
        sysMessageWrapper.add(sysMessageColorComboBox);
        sysMessageColorComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        sysMessageColorComboBox.setMaximumSize(comboDim);

        // set the checkboxes up
        autoConnectBox.setText("Autoconnect");
        autoConnectBox.setToolTipText("<HTML>" +
                                            "Check to send username and password automatically. If<br>" +
                                            "username and password are not saved, the signon dialog<br>" +
                                            "will be shown normally.</HTML>");

        timeStampBox.setText("Timestamps");
        timeStampBox.setToolTipText("If enabled, timestamps will be shown in chat.");

        viewUnitFluffBox.setText("View Unit Fluff");
        viewUnitFluffBox.setToolTipText(
              "<html>View the TRO test of units with HMP files<br>This is displayed in the unit viewer</html>");

        viewLogoBox.setText("View Logo");
        viewLogoBox.setToolTipText(
              "<html>When checked this will show your logo or your faction<br>logo in your player panel</html>");

        armyPopUpBox.setText("Army Dialog");
        armyPopUpBox.setToolTipText("<html>" +
                                          "If checked, an army dialog selection will open when<br>" +
                                          "you attack or have the opportunity to defend.</html>");

        autoReOrder.setText("Auto ReOrder");
        autoReOrder.setToolTipText(
              "<Html>If checked the system will continue to reorder parts<br>from the market while trying to repair your units</html>");

        playerLowerCBoxesPanel.add(autoConnectBox);
        playerLowerCBoxesPanel.add(timeStampBox);
        playerLowerCBoxesPanel.add(viewUnitFluffBox);
        playerLowerCBoxesPanel.add(viewLogoBox);
        playerLowerCBoxesPanel.add(armyPopUpBox);
        playerLowerCBoxesPanel.add(autoReOrder);
        SpringLayoutHelper.setupSpringGrid(playerLowerCBoxesPanel, 2);

        // lay out the main player panel
        playerPanel.add(playerFieldsWrapper);
        playerPanel.add(playerUpperCBoxesPanel);
        if (this.client.getConfig().isParam("HQTABVISIBLE")) {
            playerPanel.add(schemeWrapper);
            playerPanel.add(statusWrapper);
        } else {
            playerPanel.add(new javax.swing.JLabel("\n"));
            playerPanel.add(camoButton);
            camoButton.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
            camoButton.setAlignmentY(java.awt.Component.CENTER_ALIGNMENT);
            playerPanel.add(new javax.swing.JLabel("\n"));
        }
        playerPanel.add(sysMessageWrapper);
        playerPanel.add(skinWrapper);
        if (!this.client.getConfig().isParam("HQTABVISIBLE")) {
            playerPanel.add(new javax.swing.JLabel("\n"));
        }
        playerPanel.add(playerLowerCBoxesPanel);

        /*
         * Lay out the Host Setup Panel @ Torren
         */
        javax.swing.JPanel hostPanelWrapper = new javax.swing.JPanel();
        hostPanelWrapper.setLayout(new javax.swing.BoxLayout(hostPanelWrapper, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel dedInfoPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // fist, the options which are always avaliable. size and comment.
        javax.swing.JLabel genOptHeader = new javax.swing.JLabel("General Options:");
        genOptHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        javax.swing.JPanel genOptSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());

        genOptSpring.add(new javax.swing.JLabel("Max Players:", javax.swing.SwingConstants.TRAILING));
        maxPlayersField.setMaximumSize(newDim);
        maxPlayersField.setToolTipText("Number of players allowed to join host");
        genOptSpring.add(maxPlayersField);

        genOptSpring.add(new javax.swing.JLabel("Comment:", javax.swing.SwingConstants.TRAILING));
        hostCommentsField.setMaximumSize(newDim);
        hostCommentsField.setToolTipText("Host Comment");
        genOptSpring.add(hostCommentsField);

        /*
         * NOTE: If we were to include GAMEPASSWORD support on the config
         * dialog, this would the the appropriate place to do it. As of July
         * 2006, no server allows players to password protect theis hosts, so
         * inclusion in the GUI would be counter productive. @urgru 7/11/06
         */

        genOptSpring.add(new javax.swing.JLabel("Port:", javax.swing.SwingConstants.TRAILING));
        portField.setMaximumSize(newDim);
        portField.setToolTipText("<HTML>" +
                                       "The port which will be used to host.<br>" +
                                       "Must open this port if you are using<br>" +
                                       "a firewall and forwarded if you are<br>" +
                                       "behind a router.</HTML>");
        genOptSpring.add(portField);

        genOptSpring.add(new javax.swing.JLabel("Socket Timeout:", javax.swing.SwingConstants.TRAILING));
        socketTimeOutField.setMaximumSize(newDim);
        socketTimeOutField.setToolTipText("<HTML>" +
                                                "This is how long, in miliseconds,<br>" +
                                                "a data socket will wait for a response<br>" +
                                                "before closing. Increase this number if<br>" +
                                                "you are not downloading all the data from the server.<br>" +
                                                "Requires a reboot to take effect.</HTML>");
        genOptSpring.add(socketTimeOutField);

        SpringLayoutHelper.setupSpringGrid(genOptSpring, 2);

        hostPanelWrapper.add(new javax.swing.JLabel("\n"));
        hostPanelWrapper.add(genOptHeader);
        hostPanelWrapper.add(genOptSpring);
        hostPanelWrapper.add(new javax.swing.JLabel("\n"));

        // now, dedicated only options
        javax.swing.JLabel dedicatedServiceHeader = new javax.swing.JLabel("<HTML><body><CENTER><b>" +
                                                                                 "Warning: Do not set or change the options<br>" +
                                                                                 "below unless you want to use this client as<br>" +
                                                                                 "a dedicated host. Options above are general<br>" +
                                                                                 "and may be set any time.</b></CENTER></body></HTML>");
        dedicatedServiceHeader.setForeground(java.awt.Color.RED);
        dedicatedServiceHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        dedicatedServiceHeader.setBorder(javax.swing.BorderFactory.createCompoundBorder(
              javax.swing.BorderFactory.createLineBorder(java.awt.Color.BLACK, 1),
              javax.swing.BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        hostPanelWrapper.add(dedicatedServiceHeader);
        hostPanelWrapper.add(new javax.swing.JLabel("\n"));

        enableDedicatedServerCB.setText("Convert Client To Dedicated Host");
        enableDedicatedServerCB.setToolTipText(
              "<HTML>Warning: Only check this box if you<BR> want this client to be turned into a dedicated host!</HTML>");
        enableDedicatedServerCB.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        dedInfoPanel.add(new javax.swing.JLabel("Dedicated Name:", javax.swing.SwingConstants.TRAILING));
        nameField.setMaximumSize(newDim);
        nameField.setToolTipText("<HTML>Name of your Dedicated Host</HTML>");
        dedInfoPanel.add(nameField);

        dedInfoPanel.add(new javax.swing.JLabel("Restart Games:", javax.swing.SwingConstants.TRAILING));
        restartField.setMaximumSize(newDim);
        restartField.setToolTipText("<HTML>" +
                                          "Number of games to be played before<br>" +
                                          "the ded automatically restarts.</HTML>");
        dedInfoPanel.add(restartField);

        dedInfoPanel.add(new javax.swing.JLabel("Ded Owners:", javax.swing.SwingConstants.TRAILING));
        ownersField.setMaximumSize(newDim);
        ownersField.setToolTipText(
              "<HTML>List of people, sperated by $,<br>that you want to give control of your ded too</HTML>");
        dedInfoPanel.add(ownersField);

        dedInfoPanel.add(new javax.swing.JLabel("Memory:", javax.swing.SwingConstants.TRAILING));
        memoryField.setMaximumSize(newDim);
        memoryField.setToolTipText(
              "<HTML>The Maximum amount of memory, in MBs, that you want the ded to use<br> Note this will be translated into the -Xmx#m command line</HTML>");
        dedInfoPanel.add(memoryField);

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(dedInfoPanel, 2);
        hostPanelWrapper.add(enableDedicatedServerCB);
        hostPanelWrapper.add(dedInfoPanel);
        dedicatedHostPanel.add(hostPanelWrapper);

        /*
         * Format the CHAT panel. Box Layout w/ 3 sections. Top pane handles
         * ignores. Middle pane hadles "Include in Main" options. Mail options
         * (last sender, last receiver) added direct to the box layout as radio
         * buttons.
         */
        // set up the box layout
        // chatPanel.setLayout(new BoxLayout(chatPanel,BoxLayout.Y_AXIS));
        javax.swing.JPanel chatPanelWrapper = new javax.swing.JPanel();
        chatPanelWrapper.setLayout(new javax.swing.BoxLayout(chatPanelWrapper, javax.swing.BoxLayout.Y_AXIS));

        // make the sub panels.
        javax.swing.JPanel ignorePanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel inMainPanel = new javax.swing.JPanel();// put check boxes in a grid
        javax.swing.JPanel mailDirectionPanel = new javax.swing.JPanel();
        javax.swing.JPanel miscChatBoxesPanel = new javax.swing.JPanel();

        // the ignore boxes, spring layout
        ignorePanel.add(new javax.swing.JLabel("Ignore in Main:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(ignorePublicField);

        ignorePanel.add(new javax.swing.JLabel("Ignore in House:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(ignoreHouseField);

        ignorePanel.add(new javax.swing.JLabel("Ignore in Private:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(ignorePrivateField);

        ignorePanel.add(new javax.swing.JLabel("Keywords:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(keywordsField);
        keywordsField.setToolTipText(
              "<html>Enter words or phrases you want to be pinged on received in chat<br>The phrases are comma delimited i.e. cat,Play ball,dog</html>");

        ignorePanel.add(new javax.swing.JLabel("Challenge Text:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(challengeStringField);
        challengeStringField.setToolTipText("The string text which precedes auto-generated match requests.");

        ignorePanel.add(new javax.swing.JLabel("Max Mail Text:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(maxMailTabStringField);
        maxMailTabStringField.setToolTipText(
              "<html>If someone tries to mail you when you have<br>the max number of tabs open this message is sent to them</html>");

        ignorePanel.add(new javax.swing.JLabel("Max Mail Tabs:", javax.swing.SwingConstants.TRAILING));
        ignorePanel.add(maxNumberOfMailTabsField);
        maxNumberOfMailTabsField.setToolTipText(
              "<html>This is the maximum number of mail tabs you<br>want to have open at one time.<br>If Someone tries to send you mail<br>while you have this many mail tabs<br>open they will get the<br>Max Mail Tab Message as a reply.</html>");

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(ignorePanel, 2);

        javax.swing.JLabel intoMainHeader = new javax.swing.JLabel("Content from selected tabs shows in Main Channel:");
        intoMainHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        // the in-main-and-tab options
        hmInMainBox.setText("House");
        pmInMainBox.setText("Private");
        sysLogInMainBox.setText("System");
        miscInMainBox.setText("Misc.");
        RPGInMainBox.setText("RP");
        inMainPanel.add(hmInMainBox);
        inMainPanel.add(pmInMainBox);
        inMainPanel.add(sysLogInMainBox);
        inMainPanel.add(miscInMainBox);
        inMainPanel.add(RPGInMainBox);

        // private message options
        javax.swing.JLabel mailDirectionHeader = new javax.swing.JLabel("Private Message Options:");
        mailDirectionHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        pmReplyToSender.setText("To Last Sender");
        pmReplyToReciever.setText("To Last Recipient");
        pmUseMultipleTabs.setText("Use Multiple Mail Tabs");
        pmUseMultipleTabs.setToolTipText("Check this to have a mail tab for each user you are mailing");

        mailDirectionPanel.add(pmReplyToSender);
        mailDirectionPanel.add(pmReplyToReciever);
        mailDirectionPanel.add(pmUseMultipleTabs);

        // set up the chat color combo box
        comboDim.setSize(playerChatColorComboBox.getMinimumSize().getWidth() * 1.2,
              playerChatColorComboBox.getMinimumSize().getHeight() + 2);

        javax.swing.JLabel chatNameColorHeader = new javax.swing.JLabel("Chat Name Color Modes:");
        chatNameColorHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        playerChatColorComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        playerChatColorComboBox.setMaximumSize(comboDim);

        // emote option
        colorEmotesBox.setText("Color Emotes");
        colorEmotesBox.setToolTipText("If checked, names in /me's will be colored and bolded.");

        showEnterExitBox.setText("Enter & Exit");
        showEnterExitBox.setToolTipText("Uncheck to suppress Enter and Exit messages in chat.");

        blockImagesBox.setText("Block Images");
        blockImagesBox.setToolTipText("<HTML>" +
                                            "If checked, images in faction, private, rp and mod chat will<br>" +
                                            "be blocked. Images in userlist fluff will are also removed.</HTML>");

        mapOnClickBox.setText("Click to Map");
        mapOnClickBox.setToolTipText("If checked, clicking planet name links will activate the map tab.");

        // set up the chat tab combo box
        comboDim.setSize(playerMessageTabComboBox.getMinimumSize().getWidth() * 1.2,
              playerMessageTabComboBox.getMinimumSize().getHeight() + 2);

        javax.swing.JLabel playerMessageTabHeader = new javax.swing.JLabel("Tick Info Tab:");
        playerMessageTabHeader.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        playerMessageTabComboBox.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        playerMessageTabComboBox.setMaximumSize(comboDim);

        miscChatBoxesPanel.add(colorEmotesBox);
        miscChatBoxesPanel.add(showEnterExitBox);
        miscChatBoxesPanel.add(blockImagesBox);
        miscChatBoxesPanel.add(mapOnClickBox);

        // add to the primary panel
        chatPanelWrapper.add(ignorePanel);
        chatPanelWrapper.add(new javax.swing.JLabel("\n"));
        chatPanelWrapper.add(intoMainHeader);
        chatPanelWrapper.add(inMainPanel);
        chatPanelWrapper.add(new javax.swing.JLabel("\n"));
        chatPanelWrapper.add(mailDirectionHeader);
        chatPanelWrapper.add(mailDirectionPanel);
        chatPanelWrapper.add(new javax.swing.JLabel("\n"));
        chatPanelWrapper.add(chatNameColorHeader);
        chatPanelWrapper.add(playerChatColorComboBox);
        chatPanelWrapper.add(new javax.swing.JLabel("\n"));
        chatPanelWrapper.add(invertChatColors);
        chatPanelWrapper.add(new javax.swing.JLabel("\n"));
        chatPanelWrapper.add(miscChatBoxesPanel);
        chatPanelWrapper.add(playerMessageTabHeader);
        chatPanelWrapper.add(playerMessageTabComboBox);

        chatPanel.add(chatPanelWrapper);

        /*
         * Format the SOUND panel. File paths and Enable/Disable options for all
         * sounds.
         */
        javax.swing.JPanel soundPanelWrapper = new javax.swing.JPanel();
        soundPanelWrapper.setLayout(new javax.swing.BoxLayout(soundPanelWrapper, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel soundFieldsPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel soundCBoxPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // sound paths
        soundFieldsPanel.add(new javax.swing.JLabel(""));
        soundFieldsPanel.add(new javax.swing.JLabel("Sound Files:", javax.swing.SwingConstants.CENTER));
        soundFieldsPanel.add(new javax.swing.JLabel("Enabled:", javax.swing.SwingConstants.CENTER));

        soundFieldsPanel.add(new javax.swing.JLabel("Attacked:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnAttackField);
        enableSoundOnAttack.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnAttack);

        soundFieldsPanel.add(new javax.swing.JLabel("Called:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnCallField);
        enableSoundOnCall.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnCall);

        soundFieldsPanel.add(new javax.swing.JLabel("Keyword:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnKeywordField);
        enableSoundOnKeyword.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnKeyword);

        soundFieldsPanel.add(new javax.swing.JLabel("Message:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMessageField);
        enableSoundOnMessage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnMessage);

        soundFieldsPanel.add(new javax.swing.JLabel("BM Win:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnBMWinField);
        enableSoundOnBMWin.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnBMWin);

        soundFieldsPanel.add(new javax.swing.JLabel("Activate:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnActivateField);
        enableSoundOnActivate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnActivate);

        soundFieldsPanel.add(new javax.swing.JLabel("Deactivate:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnDeactivateField);
        enableSoundOnDeactivate.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnDeactivate);

        soundFieldsPanel.add(new javax.swing.JLabel("Enemy Detected:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnEnemyDetectedField);
        enableSoundOnEnemyDetected.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnEnemyDetected);

        soundFieldsPanel.add(new javax.swing.JLabel("Exit Client:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnExitClientField);
        enableSoundOnExitClient.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnExitClient);

        soundFieldsPanel.add(new javax.swing.JLabel("Menu Popup:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMenuPopupField);
        enableSoundOnMenuPopup.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnMenuPopup);

        soundFieldsPanel.add(new javax.swing.JLabel("Menu:", javax.swing.SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMenuField);
        enableSoundOnMenu.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnMenu);

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(soundFieldsPanel, 3);

        // set up additional checkboxes
        systemMessageKeyword.setText("Check System Messages for Keywords");
        systemMessageKeyword.setToolTipText("If enabled, all text in all channels will be searched for keywords.");
        soundCBoxPanel.add(systemMessageKeyword);

        SpringLayoutHelper.setupSpringGrid(soundCBoxPanel, 1, 1);

        // add to primary panel
        soundPanelWrapper.add(soundFieldsPanel);
        soundPanelWrapper.add(soundCBoxPanel);
        soundPanel.add(soundPanelWrapper);

        /*
         * HUD Panel Check boxes are used to display everything.
         */
        javax.swing.JPanel mainHUDSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());

        mainHUDSpring.add(new javax.swing.JLabel(" "));
        mainHUDSpring.add(new javax.swing.JLabel("<html>Left<br>Column:</html>"));
        mainHUDSpring.add(new javax.swing.JLabel("<html>Right<br>Column:</html>"));

        mainHUDSpring.add(new javax.swing.JLabel("Dynamic"));
        mainHUDSpring.add(leftColumnDynamicCB);
        mainHUDSpring.add(rightColumnDynamicCB);

        mainHUDSpring.add(new javax.swing.JLabel("<html><u>Status Groups</u></html>"));
        mainHUDSpring.add(new javax.swing.JLabel(" "));
        mainHUDSpring.add(new javax.swing.JLabel(" "));

        mainHUDSpring.add(new javax.swing.JLabel("Pilot & Eject"));
        mainHUDSpring.add(leftPilotEjectCB);
        mainHUDSpring.add(rightPilotEjectCB);

        mainHUDSpring.add(new javax.swing.JLabel("Repair / Maintanence"));
        mainHUDSpring.add(leftRepairCB);
        mainHUDSpring.add(rightRepairCB);

        mainHUDSpring.add(new javax.swing.JLabel("Engine"));
        mainHUDSpring.add(leftEngineCB);
        mainHUDSpring.add(rightEngineCB);

        mainHUDSpring.add(new javax.swing.JLabel("Equipment"));
        mainHUDSpring.add(leftEquipmentCB);
        mainHUDSpring.add(rightEquipmentCB);

        mainHUDSpring.add(new javax.swing.JLabel("Armor & Structure"));
        mainHUDSpring.add(leftArmorCB);
        mainHUDSpring.add(rightArmorCB);

        mainHUDSpring.add(new javax.swing.JLabel("Ammunition"));
        mainHUDSpring.add(leftAmmoCB);
        mainHUDSpring.add(rightAmmoCB);

        mainHUDSpring.add(new javax.swing.JLabel("Commander"));
        mainHUDSpring.add(leftCommanderCB);
        mainHUDSpring.add(rightCommanderCB);

        // Set up the springs
        SpringLayoutHelper.setupSpringGrid(mainHUDSpring, 3);

        unitHUDLayoutPanel.add(mainHUDSpring);

        /*
         * Format the TAB VISIBILITY panel. Uses Spring Layout for the
         * Top/Bottom and Vis/Invis options. Box Layout w/ embedded flow layouts
         * for the to-main options. Dummy check boxes are created in the Top
         * column for the chat tabs which are only avaliable in the lower pane.
         * A NEW dummy must be created for each, as the formatter blows up if
         * the same dummy is used multiple times. Example of the layout:
         * Visible: Top Panel: Headquarters [] []
         */
        javax.swing.JPanel tabVisibilitySpring = new javax.swing.JPanel(new javax.swing.SpringLayout());

        tabVisibilitySpring.add(new javax.swing.JLabel(""));
        tabVisibilitySpring.add(new javax.swing.JLabel(" Visible:  ", javax.swing.SwingConstants.CENTER));// extra
        // spaces
        // are
        // for
        // formatting.
        tabVisibilitySpring.add(new javax.swing.JLabel("Top Panel:", javax.swing.SwingConstants.CENTER));

        tabVisibilitySpring.add(new javax.swing.JLabel("Headquarters:", javax.swing.SwingConstants.TRAILING));
        hqTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        hqTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(hqTabVisBox);
        tabVisibilitySpring.add(hqTabonTopBox);

        tabVisibilitySpring.add(new javax.swing.JLabel("Black Market:", javax.swing.SwingConstants.TRAILING));
        bmTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        bmTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(bmTabVisBox);
        tabVisibilitySpring.add(bmTabonTopBox);

        if (Boolean.parseBoolean(this.client.getServerConfigs("UsePartsBlackMarket"))) {
            tabVisibilitySpring.add(new javax.swing.JLabel("Parts Market:", javax.swing.SwingConstants.TRAILING));
            bmeTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            bmeTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
            tabVisibilitySpring.add(bmeTabVisBox);
            tabVisibilitySpring.add(bmeTabonTopBox);
        }

        tabVisibilitySpring.add(new javax.swing.JLabel("House Status:", javax.swing.SwingConstants.TRAILING));
        hsTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        hsTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(hsTabVisBox);
        tabVisibilitySpring.add(hsTabonTopBox);

        tabVisibilitySpring.add(new javax.swing.JLabel("Battles:", javax.swing.SwingConstants.TRAILING));
        batTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        batTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(batTabVisBox);
        tabVisibilitySpring.add(batTabonTopBox);

        tabVisibilitySpring.add(new javax.swing.JLabel("Map:", javax.swing.SwingConstants.TRAILING));
        mapTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        mapTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(mapTabVisBox);
        tabVisibilitySpring.add(mapTabonTopBox);

        tabVisibilitySpring.add(new javax.swing.JLabel("House Channel:", javax.swing.SwingConstants.TRAILING));
        hmTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(hmTabVisBox);
        javax.swing.JCheckBox dopeCBox1 = new javax.swing.JCheckBox("", false);
        dopeCBox1.setEnabled(false);
        dopeCBox1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox1);

        tabVisibilitySpring.add(new javax.swing.JLabel("Role Play:", javax.swing.SwingConstants.TRAILING));
        RPGTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(RPGTabVisBox);
        javax.swing.JCheckBox dopeCBox2 = new javax.swing.JCheckBox("", false);
        dopeCBox2.setEnabled(false);
        dopeCBox2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox2);

        tabVisibilitySpring.add(new javax.swing.JLabel("Private Channel:", javax.swing.SwingConstants.TRAILING));
        pmTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(pmTabVisBox);
        javax.swing.JCheckBox dopeCBox3 = new javax.swing.JCheckBox("", false);
        dopeCBox3.setEnabled(false);
        dopeCBox3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox3);

        tabVisibilitySpring.add(new javax.swing.JLabel("Personal Log:", javax.swing.SwingConstants.TRAILING));
        pLogTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(pLogTabVisBox);
        javax.swing.JCheckBox dopeCBox4 = new javax.swing.JCheckBox("", false);
        dopeCBox4.setEnabled(false);
        dopeCBox4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox4);

        tabVisibilitySpring.add(new javax.swing.JLabel("System Log:", javax.swing.SwingConstants.TRAILING));
        sysLogTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(sysLogTabVisBox);
        javax.swing.JCheckBox dopeCBox5 = new javax.swing.JCheckBox("", false);
        dopeCBox5.setEnabled(false);
        dopeCBox5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox5);

        tabVisibilitySpring.add(new javax.swing.JLabel("Miscellaneous:", javax.swing.SwingConstants.TRAILING));
        miscTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(miscTabVisBox);
        javax.swing.JCheckBox dopeCBox6 = new javax.swing.JCheckBox("", false);
        dopeCBox6.setEnabled(false);
        dopeCBox6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox6);

        tabVisibilitySpring.add(new javax.swing.JLabel("Rules:", javax.swing.SwingConstants.TRAILING)); //@salient
        rulesTabVisBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        rulesTabonTopBox.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        tabVisibilitySpring.add(rulesTabVisBox);
        tabVisibilitySpring.add(rulesTabonTopBox);

        // Set up the springs
        SpringLayoutHelper.setupSpringGrid(tabVisibilitySpring, 3);

        // assemble final layout
        javax.swing.JPanel tabVisBox = new javax.swing.JPanel();
        tabVisBox.setLayout(new javax.swing.BoxLayout(tabVisBox, javax.swing.BoxLayout.Y_AXIS));
        tabVisBox.add(tabVisibilitySpring);
        tabVisBox.add(new javax.swing.JLabel("\n"));
        tabVisibilityPanel.add(tabVisBox);

        /*
         * Format the TABNAMING panel. Example of the layout: Name Displayed:
         * Shortcut: Headquarters: Headquarters H
         */

        // add the header
        javax.swing.JPanel tabNamingSpring = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel tabNamingBox = new javax.swing.JPanel();
        tabNamingBox.setLayout(new javax.swing.BoxLayout(tabNamingBox, javax.swing.BoxLayout.Y_AXIS));
        tabNamingBox.add(tabNamingSpring);
        tabNamingPanel.add(tabNamingBox);

        tabNamingSpring.add(new javax.swing.JLabel(""));
        tabNamingSpring.add(new javax.swing.JLabel("Name Displayed:", javax.swing.SwingConstants.CENTER));
        tabNamingSpring.add(new javax.swing.JLabel("Key:", javax.swing.SwingConstants.CENTER));

        tabNamingSpring.add(new javax.swing.JLabel("Headquarters:", javax.swing.SwingConstants.TRAILING));
        // hqTabNameField.setMaximumSize(newDim);
        // hqTabNameField.setMaximumSize(newDim);
        tabNamingSpring.add(hqTabNameField);
        tabNamingSpring.add(hqTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Black Market:", javax.swing.SwingConstants.TRAILING));
        // bmTabNameField.setMaximumSize(newDim);
        // bmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(bmTabNameField);
        tabNamingSpring.add(bmTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("House Status:", javax.swing.SwingConstants.TRAILING));
        // hsTabNameField.setMaximumSize(newDim);
        // hsTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(hsTabNameField);
        tabNamingSpring.add(hsTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Battles:", javax.swing.SwingConstants.TRAILING));
        // batTabNameField.setMaximumSize(newDim);
        // batTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(batTabNameField);
        tabNamingSpring.add(batTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Map:", javax.swing.SwingConstants.TRAILING));
        // mapTabNameField.setMaximumSize(newDim);
        // mapTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(mapTabNameField);
        tabNamingSpring.add(mapTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Main Channel:", javax.swing.SwingConstants.TRAILING));
        // mcTabNameField.setMaximumSize(newDim);
        // mcTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(mcTabNameField);
        tabNamingSpring.add(mcTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("House Channel:", javax.swing.SwingConstants.TRAILING));
        // hmTabNameField.setMaximumSize(newDim);
        // hmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(hmTabNameField);
        tabNamingSpring.add(hmTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Private Channel:", javax.swing.SwingConstants.TRAILING));
        // pmTabNameField.setMaximumSize(newDim);
        // pmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(pmTabNameField);
        tabNamingSpring.add(pmTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Personal Log:", javax.swing.SwingConstants.TRAILING));
        // pLogTabNameField.setMaximumSize(newDim);
        // pLogTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(pLogTabNameField);
        tabNamingSpring.add(pLogTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("System Log:", javax.swing.SwingConstants.TRAILING));
        // sysLogTabNameField.setMaximumSize(newDim);
        // sysLogTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(sysLogTabNameField);
        tabNamingSpring.add(sysLogTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Miscellaneous:", javax.swing.SwingConstants.TRAILING));
        tabNamingSpring.add(miscTabNameField);
        tabNamingSpring.add(miscTabMnemonicField);

        tabNamingSpring.add(new javax.swing.JLabel("Role Play:", javax.swing.SwingConstants.TRAILING));
        tabNamingSpring.add(RPGTabNameField);
        tabNamingSpring.add(RPGTabMnemonicField);

        if (Boolean.parseBoolean(this.client.getServerConfigs("UsePartsBlackMarket"))) {
            tabNamingSpring.add(new javax.swing.JLabel("Parts Market:", javax.swing.SwingConstants.TRAILING));
            tabNamingSpring.add(bmeTabNameField);
            tabNamingSpring.add(bmeTabMnemonicField);
        }

        tabNamingSpring.add(new javax.swing.JLabel("Rules:", javax.swing.SwingConstants.TRAILING)); //@salient
        tabNamingSpring.add(rulesTabNameField);
        tabNamingSpring.add(rulesTabMnemonicField);

        // Set up the actual layout
        SpringLayoutHelper.setupSpringGrid(tabNamingSpring, 3);

        /*
         * Construct a function key binding panel. Least complex panel. Woo =)
         */
        keyBindPanel.setLayout(new javax.swing.BoxLayout(keyBindPanel, javax.swing.BoxLayout.Y_AXIS));
        javax.swing.JPanel keySpringPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());

        // contruct a brief note explaining how the binds work.
        javax.swing.JLabel keyBindHeader1 = new javax.swing.JLabel("Function keys can be configured to run");
        javax.swing.JLabel keyBindHeader2 = new javax.swing.JLabel("commands. /client is prepended to the strings.");
        keyBindHeader1.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);
        keyBindHeader2.setAlignmentX(java.awt.Component.CENTER_ALIGNMENT);

        keySpringPanel.add(new javax.swing.JLabel("F1:", javax.swing.SwingConstants.TRAILING));
        f1Field.setMaximumSize(newDim);
        keySpringPanel.add(f1Field);

        keySpringPanel.add(new javax.swing.JLabel("F2:", javax.swing.SwingConstants.TRAILING));
        f2Field.setMaximumSize(newDim);
        keySpringPanel.add(f2Field);

        keySpringPanel.add(new javax.swing.JLabel("F3:", javax.swing.SwingConstants.TRAILING));
        f3Field.setMaximumSize(newDim);
        keySpringPanel.add(f3Field);

        keySpringPanel.add(new javax.swing.JLabel("F4:", javax.swing.SwingConstants.TRAILING));
        f4Field.setMaximumSize(newDim);
        keySpringPanel.add(f4Field);

        keySpringPanel.add(new javax.swing.JLabel("F5:", javax.swing.SwingConstants.TRAILING));
        f5Field.setMaximumSize(newDim);
        keySpringPanel.add(f5Field);

        // Set up the actual layout
        SpringLayoutHelper.setupSpringGrid(keySpringPanel, 5, 2);
        keyBindPanel.add(keyBindHeader1);
        keyBindPanel.add(keyBindHeader2);
        keyBindPanel.add(keySpringPanel);

        /*
         * ADD THE NEWLY FORMATTED PANELS AS TABS Because I'm a dolt, some of
         * the tabs formatted above are embedded within JPanels with flow
         * layouts. This makes them center, instead of aligning left.
         */
        javax.swing.JPanel tabVisWrapper = new javax.swing.JPanel();
        tabVisWrapper.add(tabVisibilityPanel);

        /*
         * Developer options panel.  First, a warning.
         */
        devPanel.setLayout(new VerticalLayout(5));

        javax.swing.JPanel panel = new javax.swing.JPanel();
        javax.swing.JLabel warning = new javax.swing.JLabel();
        warning.setText("Please don't check these unless you know what you are doing.");
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("WARNING!!!"));
        panel.add(warning);
        devPanel.add(panel);

        panel = new javax.swing.JPanel();
        panel.setLayout(new javax.swing.SpringLayout());
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Test Options"));
        panel.add(new javax.swing.JLabel("Test Build Table Viewer", javax.swing.SwingConstants.TRAILING));
        panel.add(testBuildTableBox);

        SpringLayoutHelper.setupSpringGrid(panel, 2);
        devPanel.add(panel);

        /*
         * Developer options panel.  First, a warning.
         */
        miscPanel.setLayout(new VerticalLayout(5));

        panel = new javax.swing.JPanel();
        warning = new javax.swing.JLabel();
        warning.setText("The place lazy devs cram in their options!");
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Miscellaneous Options"));
        panel.add(warning);
        miscPanel.add(panel);

        panel = new javax.swing.JPanel();
        panel.setLayout(new javax.swing.SpringLayout());
        panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Hangar"));
        panel.add(new javax.swing.JLabel("Expanded Unit Tool Tips", javax.swing.SwingConstants.TRAILING));
        panel.add(expandedUnitToolTipBox);

        SpringLayoutHelper.setupSpringGrid(panel, 2);
        miscPanel.add(panel);

        ConfigPane.addTab("User", null, playerPanel, "Player and Formatting options");
        ConfigPane.addTab("Chat", null, chatPanel, "Chat and Messaging options");
        ConfigPane.addTab("Sounds", null, soundPanel, "Sound options");
        ConfigPane.addTab("HUD Layout", null, unitHUDLayoutPanel, "Heads Up Display Layout");
        ConfigPane.addTab("Tab Layout", null, tabVisWrapper, "Tab visibility configurtion");
        ConfigPane.addTab("Tab Naming", null, tabNamingPanel, "Tab name configuration");
        ConfigPane.addTab("FKeys", null, keyBindPanel, "Function Key configuration");
        ConfigPane.addTab("Host Setup", null, dedicatedHostPanel, "Host Configuration");
        ConfigPane.addTab("Miscellaneous", null, miscPanel, "Miscellaneous Options");
        ConfigPane.addTab("Developer Options", null, devPanel, "Developer Options");


        // Create the panel that will hold the entire UI
        javax.swing.JPanel mainConfigPanel = new javax.swing.JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(ConfigPane,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              uNameField);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainConfigPanel, windowName);
        dialog.getRootPane().setDefaultButton(okayButton);

        showHexinHQBox.setSelected(this.client.getConfig().isParam("UNITHEX"));
        darkenMapBox.setSelected(this.client.getConfig().isParam("DARKERMAP"));
        bmPreviewImageBox.setSelected(this.client.getConfig().isParam("BMPREVIEWIMAGE"));
        useStatusForIconBox.setSelected(this.client.getConfig().isParam("STATUSINTRAYICON"));
        showUnitTechBaseCheckBox.setSelected(this.client.getConfig().isParam("ShowUnitTechBase"));
        showUnitBaseBVCheckBox.setSelected(this.client.getConfig().isParam("ShowUnitBaseBV"));
        hqColumnsField.setText(this.client.getConfig().getParam("UNITAMOUNT"));
        defaultArmyNameField.setText(this.client.getConfig().getParam("DEFAULTARMYNAME"));
        mapOverLayField.setText(this.client.getConfig().getParam("MAPOVERLAYCOLOR"));
        uNameField.setText(this.client.getConfig().getParam("NAME"));
        passwordField.setText(this.client.getConfig().getParam("NAMEPASSWORD"));
        ignorePrivateField.setText(this.client.getConfig().getParam("IGNOREPRIVATE"));
        ignoreHouseField.setText(this.client.getConfig().getParam("IGNOREHOUSE"));
        ignorePublicField.setText(this.client.getConfig().getParam("IGNOREPUBLIC"));
        challengeStringField.setText(this.client.getConfig().getParam("CHALLENGESTRING"));

        maxMailTabStringField.setText(this.client.getConfig().getParam("MAXPMMESSAGE"));
        maxNumberOfMailTabsField.setText(this.client.getConfig().getParam("MAXPMTABS"));

        timeStampBox.setSelected(this.client.getConfig().isParam("TIMESTAMP"));
        viewUnitFluffBox.setSelected(this.client.getConfig().isParam("VIEWFLUFF"));
        viewLogoBox.setSelected(this.client.getConfig().isParam("LOGO"));
        armyPopUpBox.setSelected(this.client.getConfig().isParam("POPUPONATTACK"));
        autoReOrder.setSelected(this.client.getPlayer().getAutoReorder());
        autoConnectBox.setSelected(this.client.getConfig().isParam("AUTOCONNECT"));
        soundOnCallField.setText(this.client.getConfig().getParam("SOUNDONCALL"));
        soundOnKeywordField.setText(this.client.getConfig().getParam("SOUNDONKEYWORD"));
        soundOnMessageField.setText(this.client.getConfig().getParam("SOUNDONMESSAGE"));
        soundOnAttackField.setText(this.client.getConfig().getParam("SOUNDONATTACK"));
        soundOnBMWinField.setText(this.client.getConfig().getParam("SOUNDONBMWIN"));
        soundOnActivateField.setText(this.client.getConfig().getParam("SOUNDONACTIVATE"));
        soundOnDeactivateField.setText(this.client.getConfig().getParam("SOUNDONDEACTIVATE"));
        soundOnEnemyDetectedField.setText(this.client.getConfig().getParam("SOUNDONENEMYDETECTED"));
        soundOnExitClientField.setText(this.client.getConfig().getParam("SOUNDONEXITCLIENT"));
        soundOnMenuPopupField.setText(this.client.getConfig().getParam("SOUNDONMENUPOPUP"));
        soundOnMenuField.setText(this.client.getConfig().getParam("SOUNDONMENU"));
        keywordsField.setText(this.client.getConfig().getParam("KEYWORDS"));

        // Set the selected HQ color scheme button
        String scheme = this.client.getConfigParam("HQCOLORSCHEME").toLowerCase();
        if (scheme.equals("tan")) {
            schemeComboBox.setSelectedIndex(1);
        } else if (scheme.equals("grey")) {
            schemeComboBox.setSelectedIndex(0);
        } else {// scheme is classic
            schemeComboBox.setSelectedIndex(2);
        }

        // set the selected Sys Message color scheme
        // sysMessageColorChoices = {"DarkGreen", "Gold", "Indigo", "Navy",
        // "Orange", "Red", "Teal"};
        String sysColor = this.client.getConfigParam("SYSMESSAGECOLOR");
        if (sysColor.equals("#006400")) {// dark green
            sysMessageColorComboBox.setSelectedIndex(0);
        } else if (sysColor.equals("#FFD700")) {// gold
            sysMessageColorComboBox.setSelectedIndex(1);
        } else if (sysColor.equals("#4B0082")) {// indigo
            sysMessageColorComboBox.setSelectedIndex(2);
        } else if (sysColor.equals("navy")) {
            sysMessageColorComboBox.setSelectedIndex(3);
        } else if (sysColor.equals("#FFA500")) {// orange
            sysMessageColorComboBox.setSelectedIndex(4);
        } else if (sysColor.equals("red")) {// red
            sysMessageColorComboBox.setSelectedIndex(5);
        } else if (sysColor.equals("teal")) {
            sysMessageColorComboBox.setSelectedIndex(6);
        } else {
            sysMessageColorComboBox.setSelectedIndex(7);
        }

        // Set the selected look and feel button
        String skin = this.client.getConfigParam("LOOKANDFEEL").toLowerCase();
        if (skin.equals("motif")) {
            lookandfeelComboBox.setSelectedIndex(1);
        } else if (skin.equals("metal")) {// note: this is actually Ocean in 1.5
            lookandfeelComboBox.setSelectedIndex(2);
        } else if (skin.equals("metouia")) {
            lookandfeelComboBox.setSelectedIndex(3);
        } else if (skin.equals("plastic")) {
            lookandfeelComboBox.setSelectedIndex(4);
        } else if (skin.equals("plastic3d")) {
            lookandfeelComboBox.setSelectedIndex(5);
        } else if (skin.equals("plasticxp")) {
            lookandfeelComboBox.setSelectedIndex(6);
        } else if (skin.equals("steel")) {
            lookandfeelComboBox.setSelectedIndex(7);
        } else if (skin.equals("jwindows")) {
            lookandfeelComboBox.setSelectedIndex(8);
        } else if (skin.equals("skins")) {
            lookandfeelComboBox.setSelectedIndex(9);
        } else {// scheme is system
            lookandfeelComboBox.setSelectedIndex(0);
        }

        // set the chat name color button
        String chatNameColorMode = this.client.getConfigParam("PLAYERCHATCOLORMODE").toLowerCase();
        if (chatNameColorMode.equals("factionname")) {
            playerChatColorComboBox.setSelectedIndex(3);
        } else if (chatNameColorMode.equals("factionadd")) {
            playerChatColorComboBox.setSelectedIndex(2);
        } else if (chatNameColorMode.equals("factionall")) {
            playerChatColorComboBox.setSelectedIndex(1);
        } else {// use the default, player choice
            playerChatColorComboBox.setSelectedIndex(0);
        }

        // set the chat tab mode
        int playerTabMode = Integer.parseInt(this.client.getConfigParam("USERDEFINDMESSAGETAB"));
        switch (playerTabMode) {
            case 3:
                playerMessageTabComboBox.setSelectedIndex(3);
                break;
            case 4:
                playerMessageTabComboBox.setSelectedIndex(2);
                break;
            case 5:
                playerMessageTabComboBox.setSelectedIndex(1);
                break;
            default:
                playerMessageTabComboBox.setSelectedIndex(0);
                break;
        }

        enableSoundOnCall.setSelected(this.client.getConfig().isParam("ENABLECALLSOUND"));
        enableSoundOnMessage.setSelected(this.client.getConfig().isParam("ENABLEMESSAGESOUND"));
        enableSoundOnAttack.setSelected(this.client.getConfig().isParam("ENABLEATTACKSOUND"));
        enableSoundOnKeyword.setSelected(this.client.getConfig().isParam("ENABLEKEYWORDSOUND"));
        enableSoundOnBMWin.setSelected(this.client.getConfig().isParam("ENABLEBMSOUND"));
        enableSoundOnActivate.setSelected(this.client.getConfig().isParam("ENABLEACTIVATESOUND"));
        enableSoundOnDeactivate.setSelected(this.client.getConfig().isParam("ENABLEDEACTIVATESOUND"));
        enableSoundOnEnemyDetected.setSelected(this.client.getConfig().isParam("ENABLEENEMYDETECTEDSOUND"));
        enableSoundOnExitClient.setSelected(this.client.getConfig().isParam("ENABLEEXITCLIENTSOUND"));
        enableSoundOnMenuPopup.setSelected(this.client.getConfig().isParam("ENABLEMENUPOPUPSOUND"));
        enableSoundOnMenu.setSelected(this.client.getConfig().isParam("ENABLEMENUSOUND"));

        systemMessageKeyword.setSelected(this.client.getConfig().isParam("SOUNDSFROMSYSMESSAGES"));
        colorEmotesBox.setSelected(this.client.getConfig().isParam("COLOREDEMOTES"));
        showEnterExitBox.setSelected(this.client.getConfig().isParam("SHOWENTERANDEXIT"));
        blockImagesBox.setSelected(this.client.getConfig().isParam("NOIMGINCHAT"));
        mapOnClickBox.setSelected(this.client.getConfig().isParam("MAPTABONCLICK"));

        invertChatColors.setSelected(this.client.getConfig().isParam("INVERTCHATCOLOR"));

        maxPlayersField.setText(this.client.getConfig().getParam("MAXPLAYERS"));
        hostCommentsField.setText(this.client.getConfig().getParam("COMMENT"));

        hqTabNameField.setText(this.client.getConfig().getParam("HQTABNAME"));
        hqTabMnemonicField.setText(this.client.getConfig().getParam("HQMNEMONIC"));
        hqTabonTopBox.setSelected(this.client.getConfig().isParam("HQINTOPROW"));
        hqTabVisBox.setSelected(this.client.getConfig().isParam("HQTABVISIBLE"));

        rulesTabNameField.setText(this.client.getConfig().getParam("RULESTABNAME")); //@salient
        rulesTabMnemonicField.setText(this.client.getConfig().getParam("RULESMNEMONIC"));
        rulesTabonTopBox.setSelected(this.client.getConfig().isParam("RULESINTOPROW"));
        rulesTabVisBox.setSelected(this.client.getConfig().isParam("RULESTABVISIBLE"));

        hsTabNameField.setText(this.client.getConfig().getParam("HSTATUSTABNAME"));
        hsTabMnemonicField.setText(this.client.getConfig().getParam("HSTATUSMNEMONIC"));
        hsTabonTopBox.setSelected(this.client.getConfig().isParam("HSTATUSINTOPROW"));
        hsTabVisBox.setSelected(this.client.getConfig().isParam("HSTATUSTABVISIBLE"));

        batTabNameField.setText(this.client.getConfig().getParam("BATTLETABNAME"));
        batTabMnemonicField.setText(this.client.getConfig().getParam("BATTLEMNEMONIC"));
        batTabonTopBox.setSelected(this.client.getConfig().isParam("BATTLEINTOPROW"));
        batTabVisBox.setSelected(this.client.getConfig().isParam("BATTLETABVISIBLE"));

        bmTabNameField.setText(this.client.getConfig().getParam("BMTABNAME"));
        bmTabMnemonicField.setText(this.client.getConfig().getParam("BMMNEMONIC"));
        bmTabonTopBox.setSelected(this.client.getConfig().isParam("BMINTOPROW"));
        bmTabVisBox.setSelected(this.client.getConfig().isParam("BMTABVISIBLE"));

        bmeTabNameField.setText(this.client.getConfig().getParam("BMETABNAME"));
        bmeTabMnemonicField.setText(this.client.getConfig().getParam("BMEMNEMONIC"));
        bmeTabonTopBox.setSelected(this.client.getConfig().isParam("BMEINTOPROW"));
        bmeTabVisBox.setSelected(this.client.getConfig().isParam("BMETABVISIBLE"));

        mapTabNameField.setText(this.client.getConfig().getParam("MAPTABNAME"));
        mapTabMnemonicField.setText(this.client.getConfig().getParam("MAPMNEMONIC"));
        mapTabonTopBox.setSelected(this.client.getConfig().isParam("MAPINTOPROW"));
        mapTabVisBox.setSelected(this.client.getConfig().isParam("MAPTABVISIBLE"));

        mcTabNameField.setText(this.client.getConfig().getParam("MAINCHANNELTABNAME"));
        mcTabMnemonicField.setText(this.client.getConfig().getParam("MAINCHANNELMNEMONIC"));

        hmTabNameField.setText(this.client.getConfig().getParam("HOUSEMAILTABNAME"));
        hmTabVisBox.setSelected(this.client.getConfig().isParam("HOUSEMAILVISIBLE"));
        hmTabMnemonicField.setText(this.client.getConfig().getParam("HOUSEMAILMNEMONIC"));

        pmTabNameField.setText(this.client.getConfig().getParam("PRIVATEMAILTABNAME"));
        pmTabVisBox.setSelected(this.client.getConfig().isParam("PRIVATEMAILVISIBLE"));
        pmTabMnemonicField.setText(this.client.getConfig().getParam("PRIVATEMAILMNEMONIC"));

        pLogTabNameField.setText(this.client.getConfig().getParam("PERSONALLOGTABNAME"));
        pLogTabVisBox.setSelected(this.client.getConfig().isParam("PERSONALLOGVISIBLE"));
        pLogTabMnemonicField.setText(this.client.getConfig().getParam("PRIVATEMAILMNEMONIC"));

        sysLogTabNameField.setText(this.client.getConfig().getParam("SYSTEMLOGTABNAME"));
        sysLogTabVisBox.setSelected(this.client.getConfig().isParam("SYSTEMLOGVISIBLE"));
        sysLogTabMnemonicField.setText(this.client.getConfig().getParam("SYSTEMLOGMNEMONIC"));

        miscTabNameField.setText(this.client.getConfig().getParam("MISCELLANEOUSTABNAME"));
        miscTabVisBox.setSelected(this.client.getConfig().isParam("MISCELLANEOUSVISIBLE"));
        miscTabMnemonicField.setText(this.client.getConfig().getParam("MISCELLANEOUSMNEMONIC"));

        RPGTabNameField.setText(this.client.getConfig().getParam("RPGTABNAME"));
        RPGTabVisBox.setSelected(this.client.getConfig().isParam("RPGVISIBLE"));
        RPGTabMnemonicField.setText(this.client.getConfig().getParam("RPGMNEMONIC"));

        hmInMainBox.setSelected(this.client.getConfig().isParam("MAINCHANNELHM"));
        pmInMainBox.setSelected(this.client.getConfig().isParam("MAINCHANNELPM"));
        sysLogInMainBox.setSelected(this.client.getConfig().isParam("MAINCHANNELSM"));
        miscInMainBox.setSelected(this.client.getConfig().isParam("MAINCHANNELMISC"));
        RPGInMainBox.setSelected(this.client.getConfig().isParam("MAINCHANNELRPG"));

        pmReplyToSender.setSelected(this.client.getConfig().isParam("REPLYTOSENDER"));
        pmReplyToReciever.setSelected(this.client.getConfig().isParam("REPLYTORECEIVER"));
        pmUseMultipleTabs.setSelected(this.client.getConfig().isParam("USEMULTIPLEPM"));

        chatNameColorField.setText(this.client.getConfig().getParam("COLOR"));
        foregroundColorField.setText(this.client.getConfig().getParam("CHATFONTCOLOR"));
        backgroundColorField.setText(this.client.getConfig().getParam("BACKGROUNDCOLOR"));
        chatFontField.setText(this.client.getConfig().getParam("CHATFONTSIZE"));

        f1Field.setText(this.client.getConfig().getParam("F1BIND"));
        f2Field.setText(this.client.getConfig().getParam("F2BIND"));
        f3Field.setText(this.client.getConfig().getParam("F3BIND"));
        f4Field.setText(this.client.getConfig().getParam("F4BIND"));
        f5Field.setText(this.client.getConfig().getParam("F5BIND"));

        // Dedicated Host Tab
        enableDedicatedServerCB.setSelected(this.client.isDedicated());
        portField.setText(this.client.getConfig().getParam("PORT").trim());
        nameField.setText(this.client.getConfig().getParam("NAME").trim());
        restartField.setText(this.client.getConfig().getParam("DEDAUTORESTART").trim());
        ownersField.setText(this.client.getConfig().getParam("DEDICATEDOWNERNAME").trim());
        socketTimeOutField.setText(this.client.getConfig().getParam("SOCKETTIMEOUTDELAY").trim());
        memoryField.setText(this.client.getConfig().getParam("DEDMEMORY").trim());

        pmReplyToSender.setSelected(this.client.getConfig().isParam("REPLYTOSENDER"));

        rightColumnDynamicCB.setSelected(this.client.getConfig().isParam("RIGHTCOLUMNDYNAMIC"));
        rightPilotEjectCB.setSelected(this.client.getConfig().isParam("RIGHTPILOTEJECT"));
        rightRepairCB.setSelected(this.client.getConfig().isParam("RIGHTREPAIR"));
        rightEngineCB.setSelected(this.client.getConfig().isParam("RIGHTENGINE"));
        rightEquipmentCB.setSelected(this.client.getConfig().isParam("RIGHTEQUIPMENT"));
        rightArmorCB.setSelected(this.client.getConfig().isParam("RIGHTARMOR"));
        rightAmmoCB.setSelected(this.client.getConfig().isParam("RIGHTAMMO"));
        rightCommanderCB.setSelected(this.client.getConfig().isParam("RIGHTCOMMANDER"));
        // Left Column
        leftColumnDynamicCB.setSelected(this.client.getConfig().isParam("LEFTCOLUMNDYNAMIC"));
        leftPilotEjectCB.setSelected(this.client.getConfig().isParam("LEFTPILOTEJECT"));
        leftRepairCB.setSelected(this.client.getConfig().isParam("LEFTREPAIR"));
        leftEngineCB.setSelected(this.client.getConfig().isParam("LEFTENGINE"));
        leftEquipmentCB.setSelected(this.client.getConfig().isParam("LEFTEQUIPMENT"));
        leftArmorCB.setSelected(this.client.getConfig().isParam("LEFTARMOR"));
        leftAmmoCB.setSelected(this.client.getConfig().isParam("LEFTAMMO"));
        leftCommanderCB.setSelected(this.client.getConfig().isParam("LEFTCOMMANDER"));

        testBuildTableBox.setSelected(this.client.getConfig().isParam("USETESTBUILDTABLEVIEWER"));
        expandedUnitToolTipBox.setSelected(this.client.getConfig().isParam("EXPANDEDUNITTOOLTIP"));

        // Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        // Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window
        // int h = dialog.getSize().height;
        // int y = (dim.height - h) / 2;

        dialog.setLocationRelativeTo(this.client.getMainFrame());
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            this.client.getConfig().setParam("UNITHEX", Boolean.toString(showHexinHQBox.isSelected()));
            this.client.getConfig().setParam("STATUSINTRAYICON", Boolean.toString(useStatusForIconBox.isSelected()));
            this.client.getConfig()
                  .setParam("ShowUnitTechBase", Boolean.toString(showUnitTechBaseCheckBox.isSelected()));
            this.client.getConfig().setParam("ShowUnitBaseBV", Boolean.toString(showUnitBaseBVCheckBox.isSelected()));
            this.client.getConfig().setParam("DARKERMAP", Boolean.toString(darkenMapBox.isSelected()));
            this.client.getConfig().setParam("BMPREVIEWIMAGE", Boolean.toString(bmPreviewImageBox.isSelected()));

            // don't let people do stupid things with the Columns and crash the
            // client.
            if (Integer.parseInt(hqColumnsField.getText()) < 1) {
                hqColumnsField.setText("8");// no negatives or 0's allowed
            }

            this.client.getConfig().setParam("UNITAMOUNT", hqColumnsField.getText());
            this.client.getConfig().setParam("DEFAULTARMYNAME", defaultArmyNameField.getText());
            this.client.getConfig().setParam("MAPOVERLAYCOLOR", mapOverLayField.getText());
            this.client.getConfig().setParam("NAME", uNameField.getText());
            this.client.getConfig().setParam("NAMEPASSWORD", new String(passwordField.getPassword()));
            this.client.setUsername(uNameField.getText());
            this.client.setPassword(new String(passwordField.getPassword()));
            this.client.getConfig().setParam("IGNOREPRIVATE", ignorePrivateField.getText());
            this.client.getConfig().setParam("IGNOREHOUSE", ignoreHouseField.getText());
            this.client.getConfig().setParam("IGNOREPUBLIC", ignorePublicField.getText());
            this.client.getConfig().setParam("CHALLENGESTRING", challengeStringField.getText());
            this.client.getConfig().setParam("TIMESTAMP", Boolean.toString(timeStampBox.isSelected()));
            this.client.getConfig().setParam("VIEWFLUFF", Boolean.toString(viewUnitFluffBox.isSelected()));
            this.client.getConfig().setParam("LOGO", Boolean.toString(viewLogoBox.isSelected()));
            this.client.getConfig().setParam("POPUPONATTACK", Boolean.toString(armyPopUpBox.isSelected()));
            this.client.getConfig().setParam("AUTOCONNECT", Boolean.toString(autoConnectBox.isSelected()));
            this.client.getConfig().setParam("SOUNDONCALL", soundOnCallField.getText());
            this.client.getConfig().setParam("SOUNDONKEYWORD", soundOnKeywordField.getText());
            this.client.getConfig().setParam("SOUNDONMESSAGE", soundOnMessageField.getText());
            this.client.getConfig().setParam("SOUNDONATTACK", soundOnAttackField.getText());
            this.client.getConfig().setParam("SOUNDONBMWIN", soundOnBMWinField.getText());
            this.client.getConfig().setParam("SOUNDONACTIVATE", soundOnActivateField.getText());
            this.client.getConfig().setParam("SOUNDONDEACTIVATE", soundOnDeactivateField.getText());
            this.client.getConfig().setParam("SOUNDONENEMYDETECTED", soundOnEnemyDetectedField.getText());
            this.client.getConfig().setParam("SOUNDONEXITCLIENT", soundOnExitClientField.getText());
            this.client.getConfig().setParam("SOUNDONMENUPOPUP", soundOnMenuPopupField.getText());
            this.client.getConfig().setParam("SOUNDONMENU", soundOnMenuField.getText());
            this.client.getConfig().setParam("MAXPLAYERS", maxPlayersField.getText());
            this.client.getConfig().setParam("COMMENT", hostCommentsField.getText());
            this.client.getConfig().setParam("KEYWORDS", keywordsField.getText());

            this.client.getConfig().setParam("MAXPMMESSAGE", maxMailTabStringField.getText());
            this.client.getConfig().setParam("MAXPMTABS", maxNumberOfMailTabsField.getText());

            this.client.getConfig()
                  .setParam("USETESTBUILDTABLEVIEWER", Boolean.toString(testBuildTableBox.isSelected()));
            this.client.getConfig()
                  .setParam("EXPANDEDUNITTOOLTIP", Boolean.toString(expandedUnitToolTipBox.isSelected()));

            // set the HQCOLORSCHEME based on selected button.
            // private final String[] schemeChoices = {"Grey", "Tan",
            // "Classic"};
            if (schemeComboBox.getSelectedIndex() == 1) {
                this.client.getConfig().setParam("HQCOLORSCHEME", "tan");
            } else if (schemeComboBox.getSelectedIndex() == 0) {
                this.client.getConfig().setParam("HQCOLORSCHEME", "grey");
            } else {// scheme is classic
                this.client.getConfig().setParam("HQCOLORSCHEME", "classic");
            }

            // set the LOOKANDFEEL based on selected button.
            if (lookandfeelComboBox.getSelectedIndex() == 1) {
                this.client.getConfig().setParam("LOOKANDFEEL", "motif");
            } else if (lookandfeelComboBox.getSelectedIndex() == 2) {
                this.client.getConfig().setParam("LOOKANDFEEL", "metal");// note:
                // this is
                // actually
                // "Ocean"
                // in Java
                // 1.5
            } else if (lookandfeelComboBox.getSelectedIndex() == 3) {
                this.client.getConfig().setParam("LOOKANDFEEL", "metouia");
            } else if (lookandfeelComboBox.getSelectedIndex() == 4) {
                this.client.getConfig().setParam("LOOKANDFEEL", "plastic");
            } else if (lookandfeelComboBox.getSelectedIndex() == 5) {
                this.client.getConfig().setParam("LOOKANDFEEL", "plastic3d");
            } else if (lookandfeelComboBox.getSelectedIndex() == 6) {
                this.client.getConfig().setParam("LOOKANDFEEL", "plasticxp");
            } else if (lookandfeelComboBox.getSelectedIndex() == 7) {
                this.client.getConfig().setParam("LOOKANDFEEL", "steel");
            } else if (lookandfeelComboBox.getSelectedIndex() == 8) {
                this.client.getConfig().setParam("LOOKANDFEEL", "jwindows");
            } else if (lookandfeelComboBox.getSelectedIndex() == 9) {
                this.client.getConfig().setParam("LOOKANDFEEL", "skins");
                this.client.getConfig().setParam("LOOKANDFEELSKIN", skinComboBox.getSelectedItem().toString());
            } else {// skin is system
                this.client.getConfig().setParam("LOOKANDFEEL", "system");
            }

            // set the SYSMESSAGECOLOR based on selected button
            // sysMessageColorChoices = {"Dark Green", "Gold", "Indigo", "Navy",
            // "Orange", "Red", "Teal"};
            if (sysMessageColorComboBox.getSelectedIndex() == 0) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "#006400");// dark
                // green
            } else if (sysMessageColorComboBox.getSelectedIndex() == 1) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "#FFD700");// gold
            } else if (sysMessageColorComboBox.getSelectedIndex() == 2) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "#4B0082");// indigo
            } else if (sysMessageColorComboBox.getSelectedIndex() == 3) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "navy");
            } else if (sysMessageColorComboBox.getSelectedIndex() == 4) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "#FFA500");// orange
            } else if (sysMessageColorComboBox.getSelectedIndex() == 6) {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "teal");
            } else if (sysMessageColorComboBox.getSelectedIndex() == 5) {// colour
                // is
                // red
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "red");
            } else {
                this.client.getConfig().setParam("SYSMESSAGECOLOR", "black");
            }

            // set the PLAYERCHATCOLORMODE based on selected button.
            // private final String[] playerChatColorChoices =
            // {"Player Defined", "Faction Colors", "Mixed (Faction Tag)",
            // "Mixed (Faction Name)"};
            if (playerChatColorComboBox.getSelectedIndex() == 2) {
                this.client.getConfig().setParam("PLAYERCHATCOLORMODE", "factionadd");
            } else if (playerChatColorComboBox.getSelectedIndex() == 1) {
                this.client.getConfig().setParam("PLAYERCHATCOLORMODE", "factionall");
            } else if (playerChatColorComboBox.getSelectedIndex() == 3) {
                this.client.getConfig().setParam("PLAYERCHATCOLORMODE", "factionname");
            } else {// skin is system
                this.client.getConfig().setParam("PLAYERCHATCOLORMODE", "playercolors");
            }

            switch (playerMessageTabComboBox.getSelectedIndex()) {
                case 1:
                    this.client.getConfig().setParam("USERDEFINDMESSAGETAB", "5");
                    break;
                case 2:
                    this.client.getConfig().setParam("USERDEFINDMESSAGETAB", "4");
                    break;
                case 3:
                    this.client.getConfig().setParam("USERDEFINDMESSAGETAB", "3");
                    break;
                default:
                    this.client.getConfig().setParam("USERDEFINDMESSAGETAB", "0");

            }

            this.client.getConfig().setParam("ENABLECALLSOUND", Boolean.toString(enableSoundOnCall.isSelected()));
            this.client.getConfig().setParam("ENABLEMESSAGESOUND", Boolean.toString(enableSoundOnMessage.isSelected()));
            this.client.getConfig().setParam("ENABLEATTACKSOUND", Boolean.toString(enableSoundOnAttack.isSelected()));
            this.client.getConfig().setParam("ENABLEKEYWORDSOUND", Boolean.toString(enableSoundOnKeyword.isSelected()));
            this.client.getConfig().setParam("ENABLEBMSOUND", Boolean.toString(enableSoundOnBMWin.isSelected()));
            this.client.getConfig()
                  .setParam("ENABLEACTIVATESOUND", Boolean.toString(enableSoundOnActivate.isSelected()));
            this.client.getConfig()
                  .setParam("ENABLEDEACTIVATESOUND", Boolean.toString(enableSoundOnDeactivate.isSelected()));
            this.client.getConfig()
                  .setParam("ENABLEENEMYDETECTEDSOUND", Boolean.toString(enableSoundOnEnemyDetected.isSelected()));
            this.client.getConfig()
                  .setParam("ENABLEEXITCLIENTSOUND", Boolean.toString(enableSoundOnExitClient.isSelected()));
            this.client.getConfig().setParam("ENABLEMENUSOUND", Boolean.toString(enableSoundOnMenu.isSelected()));
            this.client.getConfig()
                  .setParam("ENABLEMENUPOPUPSOUND", Boolean.toString(enableSoundOnMenuPopup.isSelected()));
            this.client.getConfig()
                  .setParam("SOUNDSFROMSYSMESSAGES", Boolean.toString(systemMessageKeyword.isSelected()));
            this.client.getConfig().setParam("COLOREDEMOTES", Boolean.toString(colorEmotesBox.isSelected()));
            this.client.getConfig().setParam("SHOWENTERANDEXIT", Boolean.toString(showEnterExitBox.isSelected()));
            this.client.getConfig().setParam("NOIMGINCHAT", Boolean.toString(blockImagesBox.isSelected()));
            this.client.getConfig().setParam("MAPTABONCLICK", Boolean.toString(mapOnClickBox.isSelected()));

            this.client.getConfig().setParam("INVERTCHATCOLOR", Boolean.toString(invertChatColors.isSelected()));

            this.client.getConfig().setParam("HQTABNAME", hqTabNameField.getText());
            this.client.getConfig().setParam("HQMNEMONIC", hqTabMnemonicField.getText());
            this.client.getConfig().setParam("HQINTOPROW", Boolean.toString(hqTabonTopBox.isSelected()));
            this.client.getConfig().setParam("HQTABVISIBLE", Boolean.toString(hqTabVisBox.isSelected()));

            this.client.getConfig().setParam("RULESTABNAME", rulesTabNameField.getText()); //@salient
            this.client.getConfig().setParam("RULESMNEMONIC", rulesTabMnemonicField.getText());
            this.client.getConfig().setParam("RULESINTOPROW", Boolean.toString(rulesTabonTopBox.isSelected()));
            this.client.getConfig().setParam("RULESTABVISIBLE", Boolean.toString(rulesTabVisBox.isSelected()));

            this.client.getConfig().setParam("BMTABNAME", bmTabNameField.getText());
            this.client.getConfig().setParam("BMMNEMONIC", bmTabMnemonicField.getText());
            this.client.getConfig().setParam("BMINTOPROW", Boolean.toString(bmTabonTopBox.isSelected()));
            this.client.getConfig().setParam("BMTABVISIBLE", Boolean.toString(bmTabVisBox.isSelected()));

            this.client.getConfig().setParam("BMETABNAME", bmeTabNameField.getText());
            this.client.getConfig().setParam("BMEMNEMONIC", bmeTabMnemonicField.getText());
            this.client.getConfig().setParam("BMEINTOPROW", Boolean.toString(bmeTabonTopBox.isSelected()));
            this.client.getConfig().setParam("BMETABVISIBLE", Boolean.toString(bmeTabVisBox.isSelected()));

            this.client.getConfig().setParam("HSTATUSTABNAME", hsTabNameField.getText());
            this.client.getConfig().setParam("HSTATUSMNEMONIC", hsTabMnemonicField.getText());
            this.client.getConfig().setParam("HSTATUSINTOPROW", Boolean.toString(hsTabonTopBox.isSelected()));
            this.client.getConfig().setParam("HSTATUSTABVISIBLE", Boolean.toString(hsTabVisBox.isSelected()));

            this.client.getConfig().setParam("BATTLETABNAME", batTabNameField.getText());
            this.client.getConfig().setParam("BATTLEMNEMONIC", batTabMnemonicField.getText());
            this.client.getConfig().setParam("BATTLEINTOPROW", Boolean.toString(batTabonTopBox.isSelected()));
            this.client.getConfig().setParam("BATTLETABVISIBLE", Boolean.toString(batTabVisBox.isSelected()));

            this.client.getConfig().setParam("MAPTABNAME", mapTabNameField.getText());
            this.client.getConfig().setParam("MAPMNEMONIC", mapTabMnemonicField.getText());
            this.client.getConfig().setParam("MAPINTOPROW", Boolean.toString(mapTabonTopBox.isSelected()));
            this.client.getConfig().setParam("MAPTABVISIBLE", Boolean.toString(mapTabVisBox.isSelected()));

            this.client.getConfig().setParam("MAINCHANNELTABNAME", mcTabNameField.getText());
            this.client.getConfig().setParam("MAINCHANNELMNEMONIC", mcTabMnemonicField.getText());

            this.client.getConfig().setParam("HOUSEMAILTABNAME", hmTabNameField.getText());
            this.client.getConfig().setParam("HOUSEMAILMNEMONIC", hmTabMnemonicField.getText());
            this.client.getConfig().setParam("HOUSEMAILVISIBLE", Boolean.toString(hmTabVisBox.isSelected()));

            this.client.getConfig().setParam("PRIVATEMAILTABNAME", pmTabNameField.getText());
            this.client.getConfig().setParam("PRIVATEMAILMNEMONIC", pmTabMnemonicField.getText());
            this.client.getConfig().setParam("PRIVATEMAILVISIBLE", Boolean.toString(pmTabVisBox.isSelected()));

            this.client.getConfig().setParam("PERSONALLOGTABNAME", pLogTabNameField.getText());
            this.client.getConfig().setParam("PERSONALLOGMNEMONIC", pLogTabMnemonicField.getText());
            this.client.getConfig().setParam("PERSONALLOGVISIBLE", Boolean.toString(pLogTabVisBox.isSelected()));

            this.client.getConfig().setParam("SYSTEMLOGTABNAME", sysLogTabNameField.getText());
            this.client.getConfig().setParam("SYSTEMLOGMNEMONIC", sysLogTabMnemonicField.getText());
            this.client.getConfig().setParam("SYSTEMLOGVISIBLE", Boolean.toString(sysLogTabVisBox.isSelected()));

            this.client.getConfig().setParam("MISCELLANEOUSTABNAME", miscTabNameField.getText());
            this.client.getConfig().setParam("MISCELLANEOUSMNEMONIC", miscTabMnemonicField.getText());
            this.client.getConfig().setParam("MISCELLANEOUSVISIBLE", Boolean.toString(miscTabVisBox.isSelected()));

            this.client.getConfig().setParam("RPGTABNAME", RPGTabNameField.getText());
            this.client.getConfig().setParam("RPGMNEMONIC", RPGTabMnemonicField.getText());
            this.client.getConfig().setParam("RPGVISIBLE", Boolean.toString(RPGTabVisBox.isSelected()));

            this.client.getConfig().setParam("MAINCHANNELHM", Boolean.toString(hmInMainBox.isSelected()));
            this.client.getConfig().setParam("MAINCHANNELPM", Boolean.toString(pmInMainBox.isSelected()));
            this.client.getConfig().setParam("MAINCHANNELSM", Boolean.toString(sysLogInMainBox.isSelected()));
            this.client.getConfig().setParam("MAINCHANNELMISC", Boolean.toString(miscInMainBox.isSelected()));
            this.client.getConfig().setParam("MAINCHANNELRPG", Boolean.toString(RPGInMainBox.isSelected()));

            this.client.getConfig().setParam("REPLYTOSENDER", Boolean.toString(pmReplyToSender.isSelected()));
            this.client.getConfig().setParam("REPLYTORECEIVER", Boolean.toString(pmReplyToReciever.isSelected()));
            this.client.getConfig().setParam("USEMULTIPLEPM", Boolean.toString(pmUseMultipleTabs.isSelected()));
            this.client.getConfig().setParam("COLOR", chatNameColorField.getText());
            this.client.getConfig().setParam("CHATFONTCOLOR", foregroundColorField.getText());
            this.client.getConfig().setParam("BACKGROUNDCOLOR", backgroundColorField.getText());
            this.client.getConfig().setParam("CHATFONTSIZE", chatFontField.getText());

            this.client.getConfig().setParam("F1BIND", f1Field.getText());
            this.client.getConfig().setParam("F2BIND", f2Field.getText());
            this.client.getConfig().setParam("F3BIND", f3Field.getText());
            this.client.getConfig().setParam("F4BIND", f4Field.getText());
            this.client.getConfig().setParam("F5BIND", f5Field.getText());

            // Dedicated Host
            this.client.getConfig().setParam("DEDICATED", Boolean.toString(enableDedicatedServerCB.isSelected()));
            this.client.getConfig().setParam("PORT", portField.getText().trim());
            this.client.getConfig().setParam("SOCKETTIMEOUTDELAY", socketTimeOutField.getText().trim());

            // only save a new host name if Dedicated is in use
            if (enableDedicatedServerCB.isSelected()) {
                this.client.getConfig().setParam("NAME", nameField.getText().trim());
            }

            this.client.getConfig().setParam("DEDAUTORESTART", restartField.getText().trim());
            this.client.getConfig().setParam("DEDICATEDOWNERNAME", ownersField.getText().trim());
            this.client.getConfig().setParam("DEDMEMORY", memoryField.getText().trim());

            // Right Column
            this.client.getConfig().setParam("RIGHTCOLUMNDYNAMIC", Boolean.toString(rightColumnDynamicCB.isSelected()));
            this.client.getConfig().setParam("RIGHTPILOTEJECT", Boolean.toString(rightPilotEjectCB.isSelected()));
            this.client.getConfig().setParam("RIGHTREPAIR", Boolean.toString(rightRepairCB.isSelected()));
            this.client.getConfig().setParam("RIGHTENGINE", Boolean.toString(rightEngineCB.isSelected()));
            this.client.getConfig().setParam("RIGHTEQUIPMENT", Boolean.toString(rightEquipmentCB.isSelected()));
            this.client.getConfig().setParam("RIGHTARMOR", Boolean.toString(rightArmorCB.isSelected()));
            this.client.getConfig().setParam("RIGHTAMMO", Boolean.toString(rightAmmoCB.isSelected()));
            this.client.getConfig().setParam("RIGHTCOMMANDER", Boolean.toString(rightCommanderCB.isSelected()));
            // Left Column
            this.client.getConfig().setParam("LEFTCOLUMNDYNAMIC", Boolean.toString(leftColumnDynamicCB.isSelected()));
            this.client.getConfig().setParam("LEFTPILOTEJECT", Boolean.toString(leftPilotEjectCB.isSelected()));
            this.client.getConfig().setParam("LEFTREPAIR", Boolean.toString(leftRepairCB.isSelected()));
            this.client.getConfig().setParam("LEFTENGINE", Boolean.toString(leftEngineCB.isSelected()));
            this.client.getConfig().setParam("LEFTEQUIPMENT", Boolean.toString(leftEquipmentCB.isSelected()));
            this.client.getConfig().setParam("LEFTARMOR", Boolean.toString(leftArmorCB.isSelected()));
            this.client.getConfig().setParam("LEFTAMMO", Boolean.toString(leftAmmoCB.isSelected()));
            this.client.getConfig().setParam("LEFTCOMMANDER", Boolean.toString(leftCommanderCB.isSelected()));

            this.client.setIgnoreHouse();
            this.client.setIgnorePrivate();
            this.client.setIgnorePublic();
            this.client.setKeyWords();
            this.client.getConfig().saveConfig();
            this.client.setConfig();
            this.client.sendChat(IClient.CAMPAIGN_PREFIX + "color " + chatNameColorField.getText());

            if (this.client.getPlayer().getAutoReorder() != autoReOrder.isSelected()) {
                this.client.sendChat(IClient.CAMPAIGN_PREFIX + "setautoreorder " + autoReOrder.isSelected());
            }

            this.client.getMainFrame().getMainPanel().recreateMainTPane(this.client.getMainFrame());

            /*
             * Last but not least, figure out which (if any) components need to
             * be redrawn and reinitialized. Checking is a pain, but redrawing
             * complex tables from scratch for no reason is even more wasteful,
             * so...
             */
            boolean columnsChanged = false;
            boolean schemeChanged = false;
            boolean unitHexChanged = false;
            boolean mapBrightnessChanged = false;

            if (!this.client.getConfigParam("LOOKANDFEEL").equalsIgnoreCase(originalLookAndFeel) ||
                      (this.client.getConfigParam("LOOKANDFEEL").equalsIgnoreCase("skins") &&
                             !this.client.getConfigParam("LOOKANDFEELSKIN").equalsIgnoreCase(originalSkin))) {
                this.client.setLookAndFeel(true);
            }

            if (!this.client.getConfigParam("UNITHEX").equals(originalUnitHex)) {
                unitHexChanged = true;
            }

            if (!this.client.getConfigParam("HQCOLORSCHEME").equalsIgnoreCase(originalScheme)) {
                schemeChanged = true;
            }

            if (!this.client.getConfigParam("DARKERMAP").equalsIgnoreCase(originalScheme)) {
                mapBrightnessChanged = true;
            }

            int currColumns = Integer.parseInt(this.client.getConfigParam("UNITAMOUNT"));
            if (currColumns != originalColumns) {
                columnsChanged = true;
            }

            if (columnsChanged || schemeChanged || unitHexChanged) {
                // only reinit. no image loading.
                this.client.getMainFrame().getMainPanel().selectFirstTab();
                this.client.getMainFrame().getMainPanel().getCommPanel().selectFirstTab();
                this.client.getMainFrame().getMainPanel().getHQPanel().reinitialize();
            }

            if (mapBrightnessChanged) {
                this.client.getMainFrame().getMainPanel().getMapPanel().repaint();
            }

            if (!this.client.getConfigParam("BMPREVIEWIMAGE").equalsIgnoreCase(originalBMPreview)) {
                this.client.getMainFrame().getMainPanel().getBMPanel().resetButtonBar();
            }

            this.client.addToChat("</BODY></html><html><BODY  TEXT=\"" +
                                        this.client.getConfig().getParam("CHATFONTCOLOR") +
                                        "\" BGCOLOR=\"" +
                                        this.client.getConfig().getParam("BACKGROUNDCOLOR") +
                                        "\"></BODY>");
        } else {
            dialog.dispose();
        }
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        } else if (command.equals(camoCommand)) {
            CamoSelectionDialog camoDialog = new CamoSelectionDialog(client.getMainFrame(), client);
            camoDialog.setVisible(true);
        } else if (command.equals(lookAndFeelCommand)) {
            if (lookandfeelComboBox.getSelectedIndex() == 9) {
                skinComboBox.setEnabled(true);
            } else {
                skinComboBox.setEnabled(false);
            }
        }
    }

}// end ConfigPage.java
