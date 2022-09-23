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

package client.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

public final class ConfigurationDialog implements ActionListener {

    // store the client backlink for other things to use
    private MWClient mwclient = null;

    private final static String okayCommand = "Okay";
    private final static String cancelCommand = "Cancel";
    private final static String camoCommand = "Camo";
    private final static String lookAndFeelCommand = "LAF";

    private final static String windowName = "MekWars Configuration";

    // BUTTONS
    private final JButton okayButton = new JButton("OK");
    private final JButton cancelButton = new JButton("Cancel");
    private final JButton camoButton = new JButton("Select Camo");

    // TEXT FIELDS
    // tab names
    private final JTextField hqTabNameField = new JTextField(10);
    private final JTextField rulesTabNameField = new JTextField(10); //@salient
    private final JTextField bmTabNameField = new JTextField(10);
    private final JTextField bmeTabNameField = new JTextField(10);
    private final JTextField hsTabNameField = new JTextField(10);
    private final JTextField batTabNameField = new JTextField(10);
    private final JTextField mapTabNameField = new JTextField(10);
    private final JTextField mcTabNameField = new JTextField(10);
    private final JTextField hmTabNameField = new JTextField(10);
    private final JTextField pmTabNameField = new JTextField(10);
    private final JTextField pLogTabNameField = new JTextField(10);
    private final JTextField sysLogTabNameField = new JTextField(10);
    private final JTextField miscTabNameField = new JTextField(10);
    private final JTextField RPGTabNameField = new JTextField(10);

    // tab mnemonic
    private final JTextField hqTabMnemonicField = new JTextField(1);
    private final JTextField rulesTabMnemonicField = new JTextField(1); //@salient
    private final JTextField bmTabMnemonicField = new JTextField(1);
    private final JTextField bmeTabMnemonicField = new JTextField(1);
    private final JTextField hsTabMnemonicField = new JTextField(1);
    private final JTextField batTabMnemonicField = new JTextField(1);
    private final JTextField mapTabMnemonicField = new JTextField(1);
    private final JTextField mcTabMnemonicField = new JTextField(1);
    private final JTextField hmTabMnemonicField = new JTextField(1);
    private final JTextField pmTabMnemonicField = new JTextField(1);
    private final JTextField pLogTabMnemonicField = new JTextField(1);
    private final JTextField sysLogTabMnemonicField = new JTextField(1);
    private final JTextField miscTabMnemonicField = new JTextField(1);
    private final JTextField RPGTabMnemonicField = new JTextField(1);

    // user config pane text fields
    private final JTextField uNameField = new JTextField(11);
    private final JPasswordField passwordField = new JPasswordField();
    private final JTextField chatNameColorField = new JTextField();
    private final JTextField foregroundColorField = new JTextField();
    private final JTextField backgroundColorField = new JTextField();
    private final JTextField chatFontField = new JTextField();
    private final JTextField defaultArmyNameField = new JTextField();
    private final JTextField mapOverLayField = new JTextField(3);

    // non-campaign GUI options (divider, etc);
    private final JTextField hqColumnsField = new JTextField(3);
    private final JCheckBox showUnitTechBaseCheckBox = new JCheckBox();
    private final JCheckBox showUnitBaseBVCheckBox = new JCheckBox();
    // keywords
    private final JTextField keywordsField = new JTextField();

    // ignore list fields, etc.
    private final JTextField ignorePublicField = new JTextField();
    private final JTextField ignoreHouseField = new JTextField();
    private final JTextField ignorePrivateField = new JTextField();
    private final JTextField challengeStringField = new JTextField();
    private final JTextField maxMailTabStringField = new JTextField();
    private final JTextField maxNumberOfMailTabsField = new JTextField();

    // paths to sound files
    private final JTextField soundOnCallField = new JTextField();
    private final JTextField soundOnKeywordField = new JTextField();
    private final JTextField soundOnMessageField = new JTextField();
    private final JTextField soundOnAttackField = new JTextField();
    private final JTextField soundOnBMWinField = new JTextField();
    private final JTextField soundOnActivateField = new JTextField();
    private final JTextField soundOnDeactivateField = new JTextField();
    private final JTextField soundOnEnemyDetectedField = new JTextField();
    private final JTextField soundOnExitClientField = new JTextField();
    private final JTextField soundOnMenuPopupField = new JTextField();
    private final JTextField soundOnMenuField = new JTextField();

    // host options
    private final JTextField maxPlayersField = new JTextField(11);
    private final JTextField hostCommentsField = new JTextField(11);

    // function keys
    private final JTextField f1Field = new JTextField(30);
    private final JTextField f2Field = new JTextField(30);
    private final JTextField f3Field = new JTextField(30);
    private final JTextField f4Field = new JTextField(30);
    private final JTextField f5Field = new JTextField(30);

    // COMBO BOXES
    private final String[] schemeChoices = { "Grey", "Tan", "Classic" };
    private final JComboBox schemeComboBox = new JComboBox(schemeChoices);

    private final String[] lookandFeelChoices = { "Native", "CDE/Motif", "Java/Ocean", "Metouia", "Plastic (Desert)", "Plastic (Sky)", "Plastic XP", "Steel", "Windows - J", "Skins" };
    private final JComboBox lookandfeelComboBox = new JComboBox(lookandFeelChoices);

    private final String[] playerChatColorChoices = { "Player Defined", "Faction Colors", "Mixed (Faction Tag)", "Mixed (Faction Name)" };
    private final JComboBox playerChatColorComboBox = new JComboBox(playerChatColorChoices);

    private final String[] sysMessageColorChoices = { "Dark Green", "Gold", "Indigo", "Navy", "Orange", "Red", "Teal", "Black" };
    private final JComboBox sysMessageColorComboBox = new JComboBox(sysMessageColorChoices);

    private final String[] playerMessageTabChoices = { "Main", "Misc", "System", "Personal" };
    private final JComboBox playerMessageTabComboBox = new JComboBox(playerMessageTabChoices);

    private JComboBox skinComboBox = null;
    // CHECK BOXEN
    // tab visibility
    private final JCheckBox hqTabVisBox = new JCheckBox();
    private final JCheckBox rulesTabVisBox = new JCheckBox(); //@salient , top only?
    private final JCheckBox bmTabVisBox = new JCheckBox();
    private final JCheckBox bmeTabVisBox = new JCheckBox();
    private final JCheckBox hsTabVisBox = new JCheckBox();
    private final JCheckBox batTabVisBox = new JCheckBox();
    private final JCheckBox mapTabVisBox = new JCheckBox();
    private final JCheckBox hmTabVisBox = new JCheckBox();// bottom only
    private final JCheckBox pmTabVisBox = new JCheckBox();// bottom only
    private final JCheckBox pLogTabVisBox = new JCheckBox();// bottom only
    private final JCheckBox sysLogTabVisBox = new JCheckBox();// bottom only
    private final JCheckBox miscTabVisBox = new JCheckBox();// bottom only
    private final JCheckBox RPGTabVisBox = new JCheckBox();// bottom only

    // tab location
    private final JCheckBox hqTabonTopBox = new JCheckBox();
    private final JCheckBox rulesTabonTopBox = new JCheckBox();
    private final JCheckBox bmTabonTopBox = new JCheckBox();
    private final JCheckBox bmeTabonTopBox = new JCheckBox();
    private final JCheckBox hsTabonTopBox = new JCheckBox();
    private final JCheckBox batTabonTopBox = new JCheckBox();
    private final JCheckBox mapTabonTopBox = new JCheckBox();

    // user options
    private final JCheckBox timeStampBox = new JCheckBox();
    private final JCheckBox showHexinHQBox = new JCheckBox();
    private final JCheckBox useStatusForIconBox = new JCheckBox();
    private final JCheckBox darkenMapBox = new JCheckBox();
    private final JCheckBox bmPreviewImageBox = new JCheckBox();
    private final JCheckBox autoConnectBox = new JCheckBox();
    private final JCheckBox viewUnitFluffBox = new JCheckBox();
    private final JCheckBox viewLogoBox = new JCheckBox();
    private final JCheckBox armyPopUpBox = new JCheckBox();
    private final JCheckBox autoReOrder = new JCheckBox();
    private final JCheckBox testBuildTableBox = new JCheckBox();
    private final JCheckBox expandedUnitToolTipBox = new JCheckBox();

    // chat options
    private final JCheckBox hmInMainBox = new JCheckBox();
    private final JCheckBox pmInMainBox = new JCheckBox();
    private final JCheckBox miscInMainBox = new JCheckBox();
    private final JCheckBox RPGInMainBox = new JCheckBox();
    private final JCheckBox sysLogInMainBox = new JCheckBox();
    private final JCheckBox pmReplyToSender = new JCheckBox();
    private final JCheckBox pmReplyToReciever = new JCheckBox();
    private final JCheckBox pmUseMultipleTabs = new JCheckBox();
    private final JCheckBox colorEmotesBox = new JCheckBox();
    private final JCheckBox showEnterExitBox = new JCheckBox();
    private final JCheckBox blockImagesBox = new JCheckBox();
    private final JCheckBox mapOnClickBox = new JCheckBox();
    private final JCheckBox enableSoundOnCall = new JCheckBox();
    private final JCheckBox enableSoundOnMessage = new JCheckBox();
    private final JCheckBox enableSoundOnAttack = new JCheckBox();
    private final JCheckBox enableSoundOnKeyword = new JCheckBox();
    private final JCheckBox enableSoundOnBMWin = new JCheckBox();
    private final JCheckBox enableSoundOnActivate = new JCheckBox();
    private final JCheckBox enableSoundOnDeactivate = new JCheckBox();
    private final JCheckBox enableSoundOnEnemyDetected = new JCheckBox();
    private final JCheckBox enableSoundOnExitClient = new JCheckBox();
    private final JCheckBox enableSoundOnMenuPopup = new JCheckBox();
    private final JCheckBox enableSoundOnMenu = new JCheckBox();
    private final JCheckBox systemMessageKeyword = new JCheckBox();
    private final JCheckBox invertChatColors = new JCheckBox("Invert Chat Colors");

    // Dedicated Setup Tab
    private final JCheckBox enableDedicatedServerCB = new JCheckBox();
    private final JTextField portField = new JTextField();
    private final JTextField nameField = new JTextField();
    private final JTextField restartField = new JTextField();
    private final JTextField ownersField = new JTextField();
    private final JTextField memoryField = new JTextField();
    private final JTextField socketTimeOutField = new JTextField();

    // Unit Status Icons
    private final JCheckBox leftColumnDynamicCB = new JCheckBox();
    private final JCheckBox leftPilotEjectCB = new JCheckBox();
    private final JCheckBox leftRepairCB = new JCheckBox();
    private final JCheckBox leftEngineCB = new JCheckBox();
    private final JCheckBox leftEquipmentCB = new JCheckBox();
    private final JCheckBox leftArmorCB = new JCheckBox();
    private final JCheckBox leftAmmoCB = new JCheckBox();
    private final JCheckBox leftCommanderCB = new JCheckBox();

    private final JCheckBox rightColumnDynamicCB = new JCheckBox();
    private final JCheckBox rightPilotEjectCB = new JCheckBox();
    private final JCheckBox rightRepairCB = new JCheckBox();
    private final JCheckBox rightEngineCB = new JCheckBox();
    private final JCheckBox rightEquipmentCB = new JCheckBox();
    private final JCheckBox rightArmorCB = new JCheckBox();
    private final JCheckBox rightAmmoCB = new JCheckBox();
    private final JCheckBox rightCommanderCB = new JCheckBox();

    // STOCK DIALOUG AND PANE
    private JDialog dialog;
    private JOptionPane pane;

    JTabbedPane ConfigPane = new JTabbedPane(SwingConstants.TOP);

    public ConfigurationDialog(MWClient c) {

        // save the client
        mwclient = c;

        // stored values.
        int originalColumns = Integer.parseInt(mwclient.getConfigParam("UNITAMOUNT"));
        String originalUnitHex = mwclient.getConfigParam("UNITHEX");
        String originalScheme = mwclient.getConfigParam("HQCOLORSCHEME").toLowerCase();
        String originalLookAndFeel = mwclient.getConfigParam("LOOKANDFEEL").toLowerCase();
        String originalSkin = mwclient.getConfigParam("LOOKANDFEELSKIN").toLowerCase();
        // String originalMapBrightness =
        // mwclient.getConfigParam("DARKERMAP").toLowerCase();
        String originalBMPreview = mwclient.getConfigParam("BMPREVIEWIMAGE").toLowerCase();

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
        JPanel playerPanel = new JPanel();// player name, etc
        JPanel chatPanel = new JPanel();// ignore options, into main options
        JPanel soundPanel = new JPanel();// sound options
        JPanel tabVisibilityPanel = new JPanel();// tab visibility and redirects
        JPanel tabNamingPanel = new JPanel();// naming and mnemonics
        JPanel keyBindPanel = new JPanel();// Funtion key binds
        JPanel dedicatedHostPanel = new JPanel();// Dedicated Host Panel
        JPanel unitHUDLayoutPanel = new JPanel();// Unit Status Panel
        JPanel devPanel = new JPanel(); // Dev options
        JPanel miscPanel = new JPanel(); // @salient - misc options

        /*
         * Format the PLAYER panel. Spring layout.
         */
        playerPanel.setLayout(new BoxLayout(playerPanel, BoxLayout.Y_AXIS));

        JPanel playerFieldsPanel = new JPanel(new SpringLayout());
        JPanel playerFieldsWrapper = new JPanel();
        playerFieldsWrapper.add(playerFieldsPanel);
        JPanel playerLowerCBoxesPanel = new JPanel(new SpringLayout());
        JPanel playerUpperCBoxesPanel = new JPanel(new SpringLayout());

        // Make a dimension which sets the max vertical size same
        // as min vertical size.
        Dimension newDim = new Dimension();
        newDim.setSize(uNameField.getMaximumSize().getWidth(), uNameField.getMaximumSize().getHeight());
        // newDim.setSize(90,100);

        playerFieldsPanel.add(new JLabel("User Name:", SwingConstants.TRAILING));
        uNameField.setMaximumSize(newDim);
        uNameField.setToolTipText("<HTML><BODY>User Name. Leave blank to be prompted<br>for a username/pass when connecting.</BODY></HTML>");
        playerFieldsPanel.add(uNameField);

        playerFieldsPanel.add(new JLabel("Password:", SwingConstants.TRAILING));
        passwordField.setMaximumSize(newDim);
        passwordField.setToolTipText("Account Password");
        playerFieldsPanel.add(passwordField);

        playerFieldsPanel.add(new JLabel("Name Color:", SwingConstants.TRAILING));
        chatNameColorField.setMaximumSize(newDim);
        chatNameColorField.setToolTipText("<HTML>Chat name colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(chatNameColorField);

        playerFieldsPanel.add(new JLabel("Text Color:", SwingConstants.TRAILING));
        foregroundColorField.setMaximumSize(newDim);
        foregroundColorField.setToolTipText("<HTML>Foreground text colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(foregroundColorField);

        playerFieldsPanel.add(new JLabel("Back Ground:", SwingConstants.TRAILING));
        backgroundColorField.setMaximumSize(newDim);
        backgroundColorField.setToolTipText("<HTML>Background colour. Can be any HTML keyword<br>colour (blue) or hex code (#003366)</HTML>");
        playerFieldsPanel.add(backgroundColorField);

        playerFieldsPanel.add(new JLabel("Chat Font:", SwingConstants.TRAILING));
        chatFontField.setMaximumSize(newDim);
        chatFontField.setToolTipText("<HTML>Chat font size. Use +10 -10 or even size number</HTML>");
        playerFieldsPanel.add(chatFontField);

        if (mwclient.getConfig().isParam("HQTABVISIBLE")) {
            playerFieldsPanel.add(new JLabel("HQ Columns:", SwingConstants.TRAILING));
            hqColumnsField.setMaximumSize(newDim);
            hqColumnsField.setToolTipText("Number of columns in Headquarters");
            playerFieldsPanel.add(hqColumnsField);

            playerFieldsPanel.add(new JLabel("New Army Name:", SwingConstants.TRAILING));
            defaultArmyNameField.setMaximumSize(newDim);
            defaultArmyNameField.setToolTipText("<HTML>" + "Name given to each newly created army. Useful<br>" + "for starter strings. For example, start all new<br>" + "armies as 'Tau Ceti Rangers,' adding lance and<br>" + "company descriptions afterwards.</HTML>");
            playerFieldsPanel.add(defaultArmyNameField);

        }

        if (new File("./data/mapoverlay.txt").exists()) {
            playerFieldsPanel.add(new JLabel("Map Overlay Color:", SwingConstants.TRAILING));
            mapOverLayField.setToolTipText("<HTML>Set the color of the star maps overlaying boarders</HTML>");
            playerFieldsPanel.add(mapOverLayField);
        }

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(playerFieldsPanel, 2);

        // add CBoxes, if relevant. keep a counter and determine how to format
        // at end.
        int upperCBoxesCounter = 0;

        if (mwclient.getConfig().isParam("HQTABVISIBLE")) {
            showHexinHQBox.setText("Hexes in HQ");
            showHexinHQBox.setToolTipText("If enabled, hexes will be shows under units in HQ.");
            playerUpperCBoxesPanel.add(showHexinHQBox);
            upperCBoxesCounter++;
        }

        if (mwclient.getConfig().isParam("BMTABVISIBLE")) {
            bmPreviewImageBox.setText("BM Preview");
            bmPreviewImageBox.setToolTipText("Check to show a unit preview image in BM tab.");
            playerUpperCBoxesPanel.add(bmPreviewImageBox);
            upperCBoxesCounter++;
        }

        if (mwclient.getConfig().isParam("MAPTABVISIBLE")) {
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
            useStatusForIconBox.setToolTipText("<HTML>" + "If checked, the server icon will be replaced with<br>" + "an activity status image. This can be helpful, but<br>" + "can make it harder to find the client window when<br>" + "moving between multiple servers or programs.<br>" + "<br>" + "REQUIRES RESTART. DOES NOT WORK ON ALL PLATFORMS!</HTML>");
            playerUpperCBoxesPanel.add(useStatusForIconBox);
            upperCBoxesCounter++;
        }

        showUnitTechBaseCheckBox.setText("Show Unit Tech Base");
        showUnitTechBaseCheckBox.setToolTipText("<html>When checked, unit Tool Tip will include Clan/IS indication.</html>");
        playerUpperCBoxesPanel.add(showUnitTechBaseCheckBox);

        showUnitBaseBVCheckBox.setText("Show Base BV");
        showUnitBaseBVCheckBox.setToolTipText("<html>If selected, BV without pilot skills will be shown in the unit display</html>");
        playerUpperCBoxesPanel.add(showUnitBaseBVCheckBox);

        SpringLayoutHelper.setupSpringGrid(playerUpperCBoxesPanel, 2);

        // set up the color scheme panel/radio buttons
        JPanel schemeWrapper = new JPanel();
        schemeWrapper.setLayout(new BoxLayout(schemeWrapper, BoxLayout.Y_AXIS));

        Dimension comboDim = new Dimension();
        comboDim.setSize(lookandfeelComboBox.getMinimumSize().getWidth() * 1.6, uNameField.getMinimumSize().getHeight() + 2);
        lookandfeelComboBox.addActionListener(this);
        lookandfeelComboBox.setActionCommand(lookAndFeelCommand);

        JLabel schemeHeader = new JLabel("HQ Color Scheme:");
        schemeHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        schemeWrapper.add(schemeHeader);
        schemeWrapper.add(schemeComboBox);
        schemeComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        schemeComboBox.setMaximumSize(comboDim);

        JPanel statusWrapper = new JPanel();
        statusWrapper.setLayout(new BoxLayout(statusWrapper, BoxLayout.Y_AXIS));

        // set up the system skin combo box
        JPanel skinWrapper = new JPanel();
        skinWrapper.setLayout(new BoxLayout(skinWrapper, BoxLayout.Y_AXIS));

        JLabel skinHeader = new JLabel("Look and Feel:");
        skinHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        skinWrapper.add(skinHeader);
        skinWrapper.add(lookandfeelComboBox);
        lookandfeelComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        lookandfeelComboBox.setMaximumSize(comboDim);

        File skinFiles = new File("./data/skins");

        if (!skinFiles.exists()) {
            skinFiles.mkdir();
        }

        skinComboBox = new JComboBox(skinFiles.list());

        for (int pos = 0; pos < skinComboBox.getItemCount(); pos++) {
            if (skinComboBox.getItemAt(pos).toString().equalsIgnoreCase(originalSkin)) {
                skinComboBox.setSelectedIndex(pos);
                break;
            }
        }
        skinComboBox.setEnabled(false);
        skinWrapper.add(new JLabel("Skins:", SwingConstants.CENTER));
        skinWrapper.add(skinComboBox);
        skinComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        skinComboBox.setMaximumSize(comboDim);

        // sys message combo box
        JPanel sysMessageWrapper = new JPanel();
        sysMessageWrapper.setLayout(new BoxLayout(sysMessageWrapper, BoxLayout.Y_AXIS));

        JLabel sysMesHeader = new JLabel("System Message Color:");
        sysMesHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        sysMessageWrapper.add(sysMesHeader);
        sysMessageWrapper.add(sysMessageColorComboBox);
        sysMessageColorComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        sysMessageColorComboBox.setMaximumSize(comboDim);

        // set the checkboxes up
        autoConnectBox.setText("Autoconnect");
        autoConnectBox.setToolTipText("<HTML>" + "Check to send username and password automatically. If<br>" + "username and password are not saved, the signon dialog<br>" + "will be shown normally.</HTML>");

        timeStampBox.setText("Timestamps");
        timeStampBox.setToolTipText("If enabled, timestamps will be shown in chat.");

        viewUnitFluffBox.setText("View Unit Fluff");
        viewUnitFluffBox.setToolTipText("<html>View the TRO test of units with HMP files<br>This is displayed in the unit viewer</html>");

        viewLogoBox.setText("View Logo");
        viewLogoBox.setToolTipText("<html>When checked this will show your logo or your faction<br>logo in your player panel</html>");

        armyPopUpBox.setText("Army Dialog");
        armyPopUpBox.setToolTipText("<html>" + "If checked, an army dialog selection will open when<br>" + "you attack or have the opportunity to defend.</html>");

        autoReOrder.setText("Auto ReOrder");
        autoReOrder.setToolTipText("<Html>If checked the system will continue to reorder parts<br>from the market while trying to repair your units</html>");

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
        if (mwclient.getConfig().isParam("HQTABVISIBLE")) {
            playerPanel.add(schemeWrapper);
            playerPanel.add(statusWrapper);
        } else {
            playerPanel.add(new JLabel("\n"));
            playerPanel.add(camoButton);
            camoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            camoButton.setAlignmentY(Component.CENTER_ALIGNMENT);
            playerPanel.add(new JLabel("\n"));
        }
        playerPanel.add(sysMessageWrapper);
        playerPanel.add(skinWrapper);
        if (!mwclient.getConfig().isParam("HQTABVISIBLE")) {
            playerPanel.add(new JLabel("\n"));
        }
        playerPanel.add(playerLowerCBoxesPanel);

        /*
         * Lay out the Host Setup Panel @ Torren
         */
        JPanel hostPanelWrapper = new JPanel();
        hostPanelWrapper.setLayout(new BoxLayout(hostPanelWrapper, BoxLayout.Y_AXIS));
        JPanel dedInfoPanel = new JPanel(new SpringLayout());

        // fist, the options which are always avaliable. size and comment.
        JLabel genOptHeader = new JLabel("General Options:");
        genOptHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel genOptSpring = new JPanel(new SpringLayout());

        genOptSpring.add(new JLabel("Max Players:", SwingConstants.TRAILING));
        maxPlayersField.setMaximumSize(newDim);
        maxPlayersField.setToolTipText("Number of players allowed to join host");
        genOptSpring.add(maxPlayersField);

        genOptSpring.add(new JLabel("Comment:", SwingConstants.TRAILING));
        hostCommentsField.setMaximumSize(newDim);
        hostCommentsField.setToolTipText("Host Comment");
        genOptSpring.add(hostCommentsField);

        /*
         * NOTE: If we were to include GAMEPASSWORD support on the config
         * dialog, this would the the appropriate place to do it. As of July
         * 2006, no server allows players to password protect theis hosts, so
         * inclusion in the GUI would be counter productive. @urgru 7/11/06
         */

        genOptSpring.add(new JLabel("Port:", SwingConstants.TRAILING));
        portField.setMaximumSize(newDim);
        portField.setToolTipText("<HTML>" + "The port which will be used to host.<br>" + "Must open this port if you are using<br>" + "a firewall and forwarded if you are<br>" + "behind a router.</HTML>");
        genOptSpring.add(portField);

        genOptSpring.add(new JLabel("Socket Timeout:", SwingConstants.TRAILING));
        socketTimeOutField.setMaximumSize(newDim);
        socketTimeOutField.setToolTipText("<HTML>" + "This is how long, in miliseconds,<br>" + "a data socket will wait for a response<br>" + "before closing. Increase this number if<br>" + "you are not downloading all the data from the server.<br>" + "Requires a reboot to take effect.</HTML>");
        genOptSpring.add(socketTimeOutField);

        SpringLayoutHelper.setupSpringGrid(genOptSpring, 2);

        hostPanelWrapper.add(new JLabel("\n"));
        hostPanelWrapper.add(genOptHeader);
        hostPanelWrapper.add(genOptSpring);
        hostPanelWrapper.add(new JLabel("\n"));

        // now, dedicated only options
        JLabel dedicatedServiceHeader = new JLabel("<HTML><body><CENTER><b>" + "Warning: Do not set or change the options<br>" + "below unless you want to use this client as<br>" + "a dedicated host. Options above are general<br>" + "and may be set any time.</b></CENTER></body></HTML>");
        dedicatedServiceHeader.setForeground(Color.RED);
        dedicatedServiceHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        dedicatedServiceHeader.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK, 1), BorderFactory.createEmptyBorder(4, 5, 4, 5)));
        hostPanelWrapper.add(dedicatedServiceHeader);
        hostPanelWrapper.add(new JLabel("\n"));

        enableDedicatedServerCB.setText("Convert Client To Dedicated Host");
        enableDedicatedServerCB.setToolTipText("<HTML>Warning: Only check this box if you<BR> want this client to be turned into a dedicated host!</HTML>");
        enableDedicatedServerCB.setAlignmentX(Component.CENTER_ALIGNMENT);

        dedInfoPanel.add(new JLabel("Dedicated Name:", SwingConstants.TRAILING));
        nameField.setMaximumSize(newDim);
        nameField.setToolTipText("<HTML>Name of your Dedicated Host</HTML>");
        dedInfoPanel.add(nameField);

        dedInfoPanel.add(new JLabel("Restart Games:", SwingConstants.TRAILING));
        restartField.setMaximumSize(newDim);
        restartField.setToolTipText("<HTML>" + "Number of games to be played before<br>" + "the ded automatically restarts.</HTML>");
        dedInfoPanel.add(restartField);

        dedInfoPanel.add(new JLabel("Ded Owners:", SwingConstants.TRAILING));
        ownersField.setMaximumSize(newDim);
        ownersField.setToolTipText("<HTML>List of people, sperated by $,<br>that you want to give control of your ded too</HTML>");
        dedInfoPanel.add(ownersField);

        dedInfoPanel.add(new JLabel("Memory:", SwingConstants.TRAILING));
        memoryField.setMaximumSize(newDim);
        memoryField.setToolTipText("<HTML>The Maximum amount of memory, in MBs, that you want the ded to use<br> Note this will be translated into the -Xmx#m command line</HTML>");
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
        JPanel chatPanelWrapper = new JPanel();
        chatPanelWrapper.setLayout(new BoxLayout(chatPanelWrapper, BoxLayout.Y_AXIS));

        // make the sub panels.
        JPanel ignorePanel = new JPanel(new SpringLayout());
        JPanel inMainPanel = new JPanel();// put check boxes in a grid
        JPanel mailDirectionPanel = new JPanel();
        JPanel miscChatBoxesPanel = new JPanel();

        // the ignore boxes, spring layout
        ignorePanel.add(new JLabel("Ignore in Main:", SwingConstants.TRAILING));
        ignorePanel.add(ignorePublicField);

        ignorePanel.add(new JLabel("Ignore in House:", SwingConstants.TRAILING));
        ignorePanel.add(ignoreHouseField);

        ignorePanel.add(new JLabel("Ignore in Private:", SwingConstants.TRAILING));
        ignorePanel.add(ignorePrivateField);

        ignorePanel.add(new JLabel("Keywords:", SwingConstants.TRAILING));
        ignorePanel.add(keywordsField);
        keywordsField.setToolTipText("<html>Enter words or phrases you want to be pinged on received in chat<br>The phrases are comma delimited i.e. cat,Play ball,dog</html>");

        ignorePanel.add(new JLabel("Challenge Text:", SwingConstants.TRAILING));
        ignorePanel.add(challengeStringField);
        challengeStringField.setToolTipText("The string text which precedes auto-generated match requests.");

        ignorePanel.add(new JLabel("Max Mail Text:", SwingConstants.TRAILING));
        ignorePanel.add(maxMailTabStringField);
        maxMailTabStringField.setToolTipText("<html>If someone tries to mail you when you have<br>the max number of tabs open this message is sent to them</html>");

        ignorePanel.add(new JLabel("Max Mail Tabs:", SwingConstants.TRAILING));
        ignorePanel.add(maxNumberOfMailTabsField);
        maxNumberOfMailTabsField.setToolTipText("<html>This is the maximum number of mail tabs you<br>want to have open at one time.<br>If Someone tries to send you mail<br>while you have this many mail tabs<br>open they will get the<br>Max Mail Tab Message as a reply.</html>");

        // run the spring layout
        SpringLayoutHelper.setupSpringGrid(ignorePanel, 2);

        JLabel intoMainHeader = new JLabel("Content from selected tabs shows in Main Channel:");
        intoMainHeader.setAlignmentX(Component.CENTER_ALIGNMENT);

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
        JLabel mailDirectionHeader = new JLabel("Private Message Options:");
        mailDirectionHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        pmReplyToSender.setText("To Last Sender");
        pmReplyToReciever.setText("To Last Recipient");
        pmUseMultipleTabs.setText("Use Multiple Mail Tabs");
        pmUseMultipleTabs.setToolTipText("Check this to have a mail tab for each user you are mailing");

        mailDirectionPanel.add(pmReplyToSender);
        mailDirectionPanel.add(pmReplyToReciever);
        mailDirectionPanel.add(pmUseMultipleTabs);

        // set up the chat color combo box
        comboDim.setSize(playerChatColorComboBox.getMinimumSize().getWidth() * 1.2, playerChatColorComboBox.getMinimumSize().getHeight() + 2);

        JLabel chatNameColorHeader = new JLabel("Chat Name Color Modes:");
        chatNameColorHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerChatColorComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerChatColorComboBox.setMaximumSize(comboDim);

        // emote option
        colorEmotesBox.setText("Color Emotes");
        colorEmotesBox.setToolTipText("If checked, names in /me's will be colored and bolded.");

        showEnterExitBox.setText("Enter & Exit");
        showEnterExitBox.setToolTipText("Uncheck to suppress Enter and Exit messages in chat.");

        blockImagesBox.setText("Block Images");
        blockImagesBox.setToolTipText("<HTML>" + "If checked, images in faction, private, rp and mod chat will<br>" + "be blocked. Images in userlist fluff will are also removed.</HTML>");

        mapOnClickBox.setText("Click to Map");
        mapOnClickBox.setToolTipText("If checked, clicking planet name links will activate the map tab.");

        // set up the chat tab combo box
        comboDim.setSize(playerMessageTabComboBox.getMinimumSize().getWidth() * 1.2, playerMessageTabComboBox.getMinimumSize().getHeight() + 2);

        JLabel playerMessageTabHeader = new JLabel("Tick Info Tab:");
        playerMessageTabHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerMessageTabComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        playerMessageTabComboBox.setMaximumSize(comboDim);

        miscChatBoxesPanel.add(colorEmotesBox);
        miscChatBoxesPanel.add(showEnterExitBox);
        miscChatBoxesPanel.add(blockImagesBox);
        miscChatBoxesPanel.add(mapOnClickBox);

        // add to the primary panel
        chatPanelWrapper.add(ignorePanel);
        chatPanelWrapper.add(new JLabel("\n"));
        chatPanelWrapper.add(intoMainHeader);
        chatPanelWrapper.add(inMainPanel);
        chatPanelWrapper.add(new JLabel("\n"));
        chatPanelWrapper.add(mailDirectionHeader);
        chatPanelWrapper.add(mailDirectionPanel);
        chatPanelWrapper.add(new JLabel("\n"));
        chatPanelWrapper.add(chatNameColorHeader);
        chatPanelWrapper.add(playerChatColorComboBox);
        chatPanelWrapper.add(new JLabel("\n"));
        chatPanelWrapper.add(invertChatColors);
        chatPanelWrapper.add(new JLabel("\n"));
        chatPanelWrapper.add(miscChatBoxesPanel);
        chatPanelWrapper.add(playerMessageTabHeader);
        chatPanelWrapper.add(playerMessageTabComboBox);

        chatPanel.add(chatPanelWrapper);

        /*
         * Format the SOUND panel. File paths and Enable/Disable options for all
         * sounds.
         */
        JPanel soundPanelWrapper = new JPanel();
        soundPanelWrapper.setLayout(new BoxLayout(soundPanelWrapper, BoxLayout.Y_AXIS));
        JPanel soundFieldsPanel = new JPanel(new SpringLayout());
        JPanel soundCBoxPanel = new JPanel(new SpringLayout());

        // sound paths
        soundFieldsPanel.add(new JLabel(""));
        soundFieldsPanel.add(new JLabel("Sound Files:", SwingConstants.CENTER));
        soundFieldsPanel.add(new JLabel("Enabled:", SwingConstants.CENTER));

        soundFieldsPanel.add(new JLabel("Attacked:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnAttackField);
        enableSoundOnAttack.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnAttack);

        soundFieldsPanel.add(new JLabel("Called:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnCallField);
        enableSoundOnCall.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnCall);

        soundFieldsPanel.add(new JLabel("Keyword:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnKeywordField);
        enableSoundOnKeyword.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnKeyword);

        soundFieldsPanel.add(new JLabel("Message:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMessageField);
        enableSoundOnMessage.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnMessage);

        soundFieldsPanel.add(new JLabel("BM Win:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnBMWinField);
        enableSoundOnBMWin.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnBMWin);

        soundFieldsPanel.add(new JLabel("Activate:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnActivateField);
        enableSoundOnActivate.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnActivate);

        soundFieldsPanel.add(new JLabel("Deactivate:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnDeactivateField);
        enableSoundOnDeactivate.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnDeactivate);

        soundFieldsPanel.add(new JLabel("Enemy Detected:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnEnemyDetectedField);
        enableSoundOnEnemyDetected.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnEnemyDetected);

        soundFieldsPanel.add(new JLabel("Exit Client:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnExitClientField);
        enableSoundOnExitClient.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnExitClient);

        soundFieldsPanel.add(new JLabel("Menu Popup:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMenuPopupField);
        enableSoundOnMenuPopup.setHorizontalAlignment(SwingConstants.CENTER);
        soundFieldsPanel.add(enableSoundOnMenuPopup);

        soundFieldsPanel.add(new JLabel("Menu:", SwingConstants.TRAILING));
        soundFieldsPanel.add(soundOnMenuField);
        enableSoundOnMenu.setHorizontalAlignment(SwingConstants.CENTER);
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
        JPanel mainHUDSpring = new JPanel(new SpringLayout());

        mainHUDSpring.add(new JLabel(" "));
        mainHUDSpring.add(new JLabel("<html>Left<br>Column:</html>"));
        mainHUDSpring.add(new JLabel("<html>Right<br>Column:</html>"));

        mainHUDSpring.add(new JLabel("Dynamic"));
        mainHUDSpring.add(leftColumnDynamicCB);
        mainHUDSpring.add(rightColumnDynamicCB);

        mainHUDSpring.add(new JLabel("<html><u>Status Groups</u></html>"));
        mainHUDSpring.add(new JLabel(" "));
        mainHUDSpring.add(new JLabel(" "));

        mainHUDSpring.add(new JLabel("Pilot & Eject"));
        mainHUDSpring.add(leftPilotEjectCB);
        mainHUDSpring.add(rightPilotEjectCB);

        mainHUDSpring.add(new JLabel("Repair / Maintanence"));
        mainHUDSpring.add(leftRepairCB);
        mainHUDSpring.add(rightRepairCB);

        mainHUDSpring.add(new JLabel("Engine"));
        mainHUDSpring.add(leftEngineCB);
        mainHUDSpring.add(rightEngineCB);

        mainHUDSpring.add(new JLabel("Equipment"));
        mainHUDSpring.add(leftEquipmentCB);
        mainHUDSpring.add(rightEquipmentCB);

        mainHUDSpring.add(new JLabel("Armor & Structure"));
        mainHUDSpring.add(leftArmorCB);
        mainHUDSpring.add(rightArmorCB);

        mainHUDSpring.add(new JLabel("Ammunition"));
        mainHUDSpring.add(leftAmmoCB);
        mainHUDSpring.add(rightAmmoCB);

        mainHUDSpring.add(new JLabel("Commander"));
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
        JPanel tabVisibilitySpring = new JPanel(new SpringLayout());

        tabVisibilitySpring.add(new JLabel(""));
        tabVisibilitySpring.add(new JLabel(" Visible:  ", SwingConstants.CENTER));// extra
        // spaces
        // are
        // for
        // formatting.
        tabVisibilitySpring.add(new JLabel("Top Panel:", SwingConstants.CENTER));

        tabVisibilitySpring.add(new JLabel("Headquarters:", SwingConstants.TRAILING));
        hqTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        hqTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(hqTabVisBox);
        tabVisibilitySpring.add(hqTabonTopBox);

        tabVisibilitySpring.add(new JLabel("Black Market:", SwingConstants.TRAILING));
        bmTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        bmTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(bmTabVisBox);
        tabVisibilitySpring.add(bmTabonTopBox);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket"))) {
            tabVisibilitySpring.add(new JLabel("Parts Market:", SwingConstants.TRAILING));
            bmeTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
            bmeTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
            tabVisibilitySpring.add(bmeTabVisBox);
            tabVisibilitySpring.add(bmeTabonTopBox);
        }

        tabVisibilitySpring.add(new JLabel("House Status:", SwingConstants.TRAILING));
        hsTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        hsTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(hsTabVisBox);
        tabVisibilitySpring.add(hsTabonTopBox);

        tabVisibilitySpring.add(new JLabel("Battles:", SwingConstants.TRAILING));
        batTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        batTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(batTabVisBox);
        tabVisibilitySpring.add(batTabonTopBox);

        tabVisibilitySpring.add(new JLabel("Map:", SwingConstants.TRAILING));
        mapTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        mapTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(mapTabVisBox);
        tabVisibilitySpring.add(mapTabonTopBox);

        tabVisibilitySpring.add(new JLabel("House Channel:", SwingConstants.TRAILING));
        hmTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(hmTabVisBox);
        JCheckBox dopeCBox1 = new JCheckBox("", false);
        dopeCBox1.setEnabled(false);
        dopeCBox1.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox1);

        tabVisibilitySpring.add(new JLabel("Role Play:", SwingConstants.TRAILING));
        RPGTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(RPGTabVisBox);
        JCheckBox dopeCBox2 = new JCheckBox("", false);
        dopeCBox2.setEnabled(false);
        dopeCBox2.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox2);

        tabVisibilitySpring.add(new JLabel("Private Channel:", SwingConstants.TRAILING));
        pmTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(pmTabVisBox);
        JCheckBox dopeCBox3 = new JCheckBox("", false);
        dopeCBox3.setEnabled(false);
        dopeCBox3.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox3);

        tabVisibilitySpring.add(new JLabel("Personal Log:", SwingConstants.TRAILING));
        pLogTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(pLogTabVisBox);
        JCheckBox dopeCBox4 = new JCheckBox("", false);
        dopeCBox4.setEnabled(false);
        dopeCBox4.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox4);

        tabVisibilitySpring.add(new JLabel("System Log:", SwingConstants.TRAILING));
        sysLogTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(sysLogTabVisBox);
        JCheckBox dopeCBox5 = new JCheckBox("", false);
        dopeCBox5.setEnabled(false);
        dopeCBox5.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox5);

        tabVisibilitySpring.add(new JLabel("Miscellaneous:", SwingConstants.TRAILING));
        miscTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(miscTabVisBox);
        JCheckBox dopeCBox6 = new JCheckBox("", false);
        dopeCBox6.setEnabled(false);
        dopeCBox6.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(dopeCBox6);

        tabVisibilitySpring.add(new JLabel("Rules:", SwingConstants.TRAILING)); //@salient
        rulesTabVisBox.setHorizontalAlignment(SwingConstants.CENTER);
        rulesTabonTopBox.setHorizontalAlignment(SwingConstants.CENTER);
        tabVisibilitySpring.add(rulesTabVisBox);
        tabVisibilitySpring.add(rulesTabonTopBox);

        // Set up the springs
        SpringLayoutHelper.setupSpringGrid(tabVisibilitySpring, 3);

        // assemble final layout
        JPanel tabVisBox = new JPanel();
        tabVisBox.setLayout(new BoxLayout(tabVisBox, BoxLayout.Y_AXIS));
        tabVisBox.add(tabVisibilitySpring);
        tabVisBox.add(new JLabel("\n"));
        tabVisibilityPanel.add(tabVisBox);

        /*
         * Format the TABNAMING panel. Example of the layout: Name Displayed:
         * Shortcut: Headquarters: Headquarters H
         */

        // add the header
        JPanel tabNamingSpring = new JPanel(new SpringLayout());
        JPanel tabNamingBox = new JPanel();
        tabNamingBox.setLayout(new BoxLayout(tabNamingBox, BoxLayout.Y_AXIS));
        tabNamingBox.add(tabNamingSpring);
        tabNamingPanel.add(tabNamingBox);

        tabNamingSpring.add(new JLabel(""));
        tabNamingSpring.add(new JLabel("Name Displayed:", SwingConstants.CENTER));
        tabNamingSpring.add(new JLabel("Key:", SwingConstants.CENTER));

        tabNamingSpring.add(new JLabel("Headquarters:", SwingConstants.TRAILING));
        // hqTabNameField.setMaximumSize(newDim);
        // hqTabNameField.setMaximumSize(newDim);
        tabNamingSpring.add(hqTabNameField);
        tabNamingSpring.add(hqTabMnemonicField);

        tabNamingSpring.add(new JLabel("Black Market:", SwingConstants.TRAILING));
        // bmTabNameField.setMaximumSize(newDim);
        // bmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(bmTabNameField);
        tabNamingSpring.add(bmTabMnemonicField);

        tabNamingSpring.add(new JLabel("House Status:", SwingConstants.TRAILING));
        // hsTabNameField.setMaximumSize(newDim);
        // hsTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(hsTabNameField);
        tabNamingSpring.add(hsTabMnemonicField);

        tabNamingSpring.add(new JLabel("Battles:", SwingConstants.TRAILING));
        // batTabNameField.setMaximumSize(newDim);
        // batTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(batTabNameField);
        tabNamingSpring.add(batTabMnemonicField);

        tabNamingSpring.add(new JLabel("Map:", SwingConstants.TRAILING));
        // mapTabNameField.setMaximumSize(newDim);
        // mapTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(mapTabNameField);
        tabNamingSpring.add(mapTabMnemonicField);

        tabNamingSpring.add(new JLabel("Main Channel:", SwingConstants.TRAILING));
        // mcTabNameField.setMaximumSize(newDim);
        // mcTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(mcTabNameField);
        tabNamingSpring.add(mcTabMnemonicField);

        tabNamingSpring.add(new JLabel("House Channel:", SwingConstants.TRAILING));
        // hmTabNameField.setMaximumSize(newDim);
        // hmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(hmTabNameField);
        tabNamingSpring.add(hmTabMnemonicField);

        tabNamingSpring.add(new JLabel("Private Channel:", SwingConstants.TRAILING));
        // pmTabNameField.setMaximumSize(newDim);
        // pmTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(pmTabNameField);
        tabNamingSpring.add(pmTabMnemonicField);

        tabNamingSpring.add(new JLabel("Personal Log:", SwingConstants.TRAILING));
        // pLogTabNameField.setMaximumSize(newDim);
        // pLogTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(pLogTabNameField);
        tabNamingSpring.add(pLogTabMnemonicField);

        tabNamingSpring.add(new JLabel("System Log:", SwingConstants.TRAILING));
        // sysLogTabNameField.setMaximumSize(newDim);
        // sysLogTabMnemonicField.setMaximumSize(newDim);
        tabNamingSpring.add(sysLogTabNameField);
        tabNamingSpring.add(sysLogTabMnemonicField);

        tabNamingSpring.add(new JLabel("Miscellaneous:", SwingConstants.TRAILING));
        tabNamingSpring.add(miscTabNameField);
        tabNamingSpring.add(miscTabMnemonicField);

        tabNamingSpring.add(new JLabel("Role Play:", SwingConstants.TRAILING));
        tabNamingSpring.add(RPGTabNameField);
        tabNamingSpring.add(RPGTabMnemonicField);

        if (Boolean.parseBoolean(mwclient.getserverConfigs("UsePartsBlackMarket"))) {
            tabNamingSpring.add(new JLabel("Parts Market:", SwingConstants.TRAILING));
            tabNamingSpring.add(bmeTabNameField);
            tabNamingSpring.add(bmeTabMnemonicField);
        }

        tabNamingSpring.add(new JLabel("Rules:", SwingConstants.TRAILING)); //@salient
        tabNamingSpring.add(rulesTabNameField);
        tabNamingSpring.add(rulesTabMnemonicField);

        // Set up the actual layout
        SpringLayoutHelper.setupSpringGrid(tabNamingSpring, 3);

        /*
         * Construct a function key binding panel. Least complex panel. Woo =)
         */
        keyBindPanel.setLayout(new BoxLayout(keyBindPanel, BoxLayout.Y_AXIS));
        JPanel keySpringPanel = new JPanel(new SpringLayout());

        // contruct a brief note explaining how the binds work.
        JLabel keyBindHeader1 = new JLabel("Function keys can be configured to run");
        JLabel keyBindHeader2 = new JLabel("commands. /c is prepended to the strings.");
        keyBindHeader1.setAlignmentX(Component.CENTER_ALIGNMENT);
        keyBindHeader2.setAlignmentX(Component.CENTER_ALIGNMENT);

        keySpringPanel.add(new JLabel("F1:", SwingConstants.TRAILING));
        f1Field.setMaximumSize(newDim);
        keySpringPanel.add(f1Field);

        keySpringPanel.add(new JLabel("F2:", SwingConstants.TRAILING));
        f2Field.setMaximumSize(newDim);
        keySpringPanel.add(f2Field);

        keySpringPanel.add(new JLabel("F3:", SwingConstants.TRAILING));
        f3Field.setMaximumSize(newDim);
        keySpringPanel.add(f3Field);

        keySpringPanel.add(new JLabel("F4:", SwingConstants.TRAILING));
        f4Field.setMaximumSize(newDim);
        keySpringPanel.add(f4Field);

        keySpringPanel.add(new JLabel("F5:", SwingConstants.TRAILING));
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
        JPanel tabVisWrapper = new JPanel();
        tabVisWrapper.add(tabVisibilityPanel);

        /*
         * Developer options panel.  First, a warning.
         */
        devPanel.setLayout(new VerticalLayout(5));

        JPanel panel = new JPanel();
        JLabel warning = new JLabel();
        warning.setText("Please don't check these unless you know what you are doing.");
        panel.setBorder(BorderFactory.createTitledBorder("WARNING!!!"));
        panel.add(warning);
        devPanel.add(panel);

        panel = new JPanel();
        panel.setLayout(new SpringLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Test Options"));
        panel.add(new JLabel("Test Build Table Viewer", SwingConstants.TRAILING));
        panel.add(testBuildTableBox);

        SpringLayoutHelper.setupSpringGrid(panel, 2);
        devPanel.add(panel);

        /*
         * Developer options panel.  First, a warning.
         */
        miscPanel.setLayout(new VerticalLayout(5));

        panel = new JPanel();
        warning = new JLabel();
        warning.setText("The place lazy devs cram in their options!");
        panel.setBorder(BorderFactory.createTitledBorder("Miscellaneous Options"));
        panel.add(warning);
        miscPanel.add(panel);

        panel = new JPanel();
        panel.setLayout(new SpringLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Hangar"));
        panel.add(new JLabel("Expanded Unit Tool Tips", SwingConstants.TRAILING));
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
        JPanel mainConfigPanel = new JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(ConfigPane, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options, uNameField);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainConfigPanel, windowName);
        dialog.getRootPane().setDefaultButton(okayButton);

        showHexinHQBox.setSelected(mwclient.getConfig().isParam("UNITHEX"));
        darkenMapBox.setSelected(mwclient.getConfig().isParam("DARKERMAP"));
        bmPreviewImageBox.setSelected(mwclient.getConfig().isParam("BMPREVIEWIMAGE"));
        useStatusForIconBox.setSelected(mwclient.getConfig().isParam("STATUSINTRAYICON"));
        showUnitTechBaseCheckBox.setSelected(mwclient.getConfig().isParam("ShowUnitTechBase"));
        showUnitBaseBVCheckBox.setSelected(mwclient.getConfig().isParam("ShowUnitBaseBV"));
        hqColumnsField.setText(mwclient.getConfig().getParam("UNITAMOUNT"));
        defaultArmyNameField.setText(mwclient.getConfig().getParam("DEFAULTARMYNAME"));
        mapOverLayField.setText(mwclient.getConfig().getParam("MAPOVERLAYCOLOR"));
        uNameField.setText(mwclient.getConfig().getParam("NAME"));
        passwordField.setText(mwclient.getConfig().getParam("NAMEPASSWORD"));
        ignorePrivateField.setText(mwclient.getConfig().getParam("IGNOREPRIVATE"));
        ignoreHouseField.setText(mwclient.getConfig().getParam("IGNOREHOUSE"));
        ignorePublicField.setText(mwclient.getConfig().getParam("IGNOREPUBLIC"));
        challengeStringField.setText(mwclient.getConfig().getParam("CHALLENGESTRING"));

        maxMailTabStringField.setText(mwclient.getConfig().getParam("MAXPMMESSAGE"));
        maxNumberOfMailTabsField.setText(mwclient.getConfig().getParam("MAXPMTABS"));

        timeStampBox.setSelected(mwclient.getConfig().isParam("TIMESTAMP"));
        viewUnitFluffBox.setSelected(mwclient.getConfig().isParam("VIEWFLUFF"));
        viewLogoBox.setSelected(mwclient.getConfig().isParam("LOGO"));
        armyPopUpBox.setSelected(mwclient.getConfig().isParam("POPUPONATTACK"));
        autoReOrder.setSelected(mwclient.getPlayer().getAutoReorder());
        autoConnectBox.setSelected(mwclient.getConfig().isParam("AUTOCONNECT"));
        soundOnCallField.setText(mwclient.getConfig().getParam("SOUNDONCALL"));
        soundOnKeywordField.setText(mwclient.getConfig().getParam("SOUNDONKEYWORD"));
        soundOnMessageField.setText(mwclient.getConfig().getParam("SOUNDONMESSAGE"));
        soundOnAttackField.setText(mwclient.getConfig().getParam("SOUNDONATTACK"));
        soundOnBMWinField.setText(mwclient.getConfig().getParam("SOUNDONBMWIN"));
        soundOnActivateField.setText(mwclient.getConfig().getParam("SOUNDONACTIVATE"));
        soundOnDeactivateField.setText(mwclient.getConfig().getParam("SOUNDONDEACTIVATE"));
        soundOnEnemyDetectedField.setText(mwclient.getConfig().getParam("SOUNDONENEMYDETECTED"));
        soundOnExitClientField.setText(mwclient.getConfig().getParam("SOUNDONEXITCLIENT"));
        soundOnMenuPopupField.setText(mwclient.getConfig().getParam("SOUNDONMENUPOPUP"));
        soundOnMenuField.setText(mwclient.getConfig().getParam("SOUNDONMENU"));
        keywordsField.setText(mwclient.getConfig().getParam("KEYWORDS"));

        // Set the selected HQ color scheme button
        String scheme = mwclient.getConfigParam("HQCOLORSCHEME").toLowerCase();
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
        String sysColor = mwclient.getConfigParam("SYSMESSAGECOLOR");
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
        String skin = mwclient.getConfigParam("LOOKANDFEEL").toLowerCase();
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
        String chatNameColorMode = mwclient.getConfigParam("PLAYERCHATCOLORMODE").toLowerCase();
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
        int playerTabMode = Integer.parseInt(mwclient.getConfigParam("USERDEFINDMESSAGETAB"));
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

        enableSoundOnCall.setSelected(mwclient.getConfig().isParam("ENABLECALLSOUND"));
        enableSoundOnMessage.setSelected(mwclient.getConfig().isParam("ENABLEMESSAGESOUND"));
        enableSoundOnAttack.setSelected(mwclient.getConfig().isParam("ENABLEATTACKSOUND"));
        enableSoundOnKeyword.setSelected(mwclient.getConfig().isParam("ENABLEKEYWORDSOUND"));
        enableSoundOnBMWin.setSelected(mwclient.getConfig().isParam("ENABLEBMSOUND"));
        enableSoundOnActivate.setSelected(mwclient.getConfig().isParam("ENABLEACTIVATESOUND"));
        enableSoundOnDeactivate.setSelected(mwclient.getConfig().isParam("ENABLEDEACTIVATESOUND"));
        enableSoundOnEnemyDetected.setSelected(mwclient.getConfig().isParam("ENABLEENEMYDETECTEDSOUND"));
        enableSoundOnExitClient.setSelected(mwclient.getConfig().isParam("ENABLEEXITCLIENTSOUND"));
        enableSoundOnMenuPopup.setSelected(mwclient.getConfig().isParam("ENABLEMENUPOPUPSOUND"));
        enableSoundOnMenu.setSelected(mwclient.getConfig().isParam("ENABLEMENUSOUND"));

        systemMessageKeyword.setSelected(mwclient.getConfig().isParam("SOUNDSFROMSYSMESSAGES"));
        colorEmotesBox.setSelected(mwclient.getConfig().isParam("COLOREDEMOTES"));
        showEnterExitBox.setSelected(mwclient.getConfig().isParam("SHOWENTERANDEXIT"));
        blockImagesBox.setSelected(mwclient.getConfig().isParam("NOIMGINCHAT"));
        mapOnClickBox.setSelected(mwclient.getConfig().isParam("MAPTABONCLICK"));

        invertChatColors.setSelected(mwclient.getConfig().isParam("INVERTCHATCOLOR"));

        maxPlayersField.setText(mwclient.getConfig().getParam("MAXPLAYERS"));
        hostCommentsField.setText(mwclient.getConfig().getParam("COMMENT"));

        hqTabNameField.setText(mwclient.getConfig().getParam("HQTABNAME"));
        hqTabMnemonicField.setText(mwclient.getConfig().getParam("HQMNEMONIC"));
        hqTabonTopBox.setSelected(mwclient.getConfig().isParam("HQINTOPROW"));
        hqTabVisBox.setSelected(mwclient.getConfig().isParam("HQTABVISIBLE"));

        rulesTabNameField.setText(mwclient.getConfig().getParam("RULESTABNAME")); //@salient
        rulesTabMnemonicField.setText(mwclient.getConfig().getParam("RULESMNEMONIC"));
        rulesTabonTopBox.setSelected(mwclient.getConfig().isParam("RULESINTOPROW"));
        rulesTabVisBox.setSelected(mwclient.getConfig().isParam("RULESTABVISIBLE"));

        hsTabNameField.setText(mwclient.getConfig().getParam("HSTATUSTABNAME"));
        hsTabMnemonicField.setText(mwclient.getConfig().getParam("HSTATUSMNEMONIC"));
        hsTabonTopBox.setSelected(mwclient.getConfig().isParam("HSTATUSINTOPROW"));
        hsTabVisBox.setSelected(mwclient.getConfig().isParam("HSTATUSTABVISIBLE"));

        batTabNameField.setText(mwclient.getConfig().getParam("BATTLETABNAME"));
        batTabMnemonicField.setText(mwclient.getConfig().getParam("BATTLEMNEMONIC"));
        batTabonTopBox.setSelected(mwclient.getConfig().isParam("BATTLEINTOPROW"));
        batTabVisBox.setSelected(mwclient.getConfig().isParam("BATTLETABVISIBLE"));

        bmTabNameField.setText(mwclient.getConfig().getParam("BMTABNAME"));
        bmTabMnemonicField.setText(mwclient.getConfig().getParam("BMMNEMONIC"));
        bmTabonTopBox.setSelected(mwclient.getConfig().isParam("BMINTOPROW"));
        bmTabVisBox.setSelected(mwclient.getConfig().isParam("BMTABVISIBLE"));

        bmeTabNameField.setText(mwclient.getConfig().getParam("BMETABNAME"));
        bmeTabMnemonicField.setText(mwclient.getConfig().getParam("BMEMNEMONIC"));
        bmeTabonTopBox.setSelected(mwclient.getConfig().isParam("BMEINTOPROW"));
        bmeTabVisBox.setSelected(mwclient.getConfig().isParam("BMETABVISIBLE"));

        mapTabNameField.setText(mwclient.getConfig().getParam("MAPTABNAME"));
        mapTabMnemonicField.setText(mwclient.getConfig().getParam("MAPMNEMONIC"));
        mapTabonTopBox.setSelected(mwclient.getConfig().isParam("MAPINTOPROW"));
        mapTabVisBox.setSelected(mwclient.getConfig().isParam("MAPTABVISIBLE"));

        mcTabNameField.setText(mwclient.getConfig().getParam("MAINCHANNELTABNAME"));
        mcTabMnemonicField.setText(mwclient.getConfig().getParam("MAINCHANNELMNEMONIC"));

        hmTabNameField.setText(mwclient.getConfig().getParam("HOUSEMAILTABNAME"));
        hmTabVisBox.setSelected(mwclient.getConfig().isParam("HOUSEMAILVISIBLE"));
        hmTabMnemonicField.setText(mwclient.getConfig().getParam("HOUSEMAILMNEMONIC"));

        pmTabNameField.setText(mwclient.getConfig().getParam("PRIVATEMAILTABNAME"));
        pmTabVisBox.setSelected(mwclient.getConfig().isParam("PRIVATEMAILVISIBLE"));
        pmTabMnemonicField.setText(mwclient.getConfig().getParam("PRIVATEMAILMNEMONIC"));

        pLogTabNameField.setText(mwclient.getConfig().getParam("PERSONALLOGTABNAME"));
        pLogTabVisBox.setSelected(mwclient.getConfig().isParam("PERSONALLOGVISIBLE"));
        pLogTabMnemonicField.setText(mwclient.getConfig().getParam("PRIVATEMAILMNEMONIC"));

        sysLogTabNameField.setText(mwclient.getConfig().getParam("SYSTEMLOGTABNAME"));
        sysLogTabVisBox.setSelected(mwclient.getConfig().isParam("SYSTEMLOGVISIBLE"));
        sysLogTabMnemonicField.setText(mwclient.getConfig().getParam("SYSTEMLOGMNEMONIC"));

        miscTabNameField.setText(mwclient.getConfig().getParam("MISCELLANEOUSTABNAME"));
        miscTabVisBox.setSelected(mwclient.getConfig().isParam("MISCELLANEOUSVISIBLE"));
        miscTabMnemonicField.setText(mwclient.getConfig().getParam("MISCELLANEOUSMNEMONIC"));

        RPGTabNameField.setText(mwclient.getConfig().getParam("RPGTABNAME"));
        RPGTabVisBox.setSelected(mwclient.getConfig().isParam("RPGVISIBLE"));
        RPGTabMnemonicField.setText(mwclient.getConfig().getParam("RPGMNEMONIC"));

        hmInMainBox.setSelected(mwclient.getConfig().isParam("MAINCHANNELHM"));
        pmInMainBox.setSelected(mwclient.getConfig().isParam("MAINCHANNELPM"));
        sysLogInMainBox.setSelected(mwclient.getConfig().isParam("MAINCHANNELSM"));
        miscInMainBox.setSelected(mwclient.getConfig().isParam("MAINCHANNELMISC"));
        RPGInMainBox.setSelected(mwclient.getConfig().isParam("MAINCHANNELRPG"));

        pmReplyToSender.setSelected(mwclient.getConfig().isParam("REPLYTOSENDER"));
        pmReplyToReciever.setSelected(mwclient.getConfig().isParam("REPLYTORECEIVER"));
        pmUseMultipleTabs.setSelected(mwclient.getConfig().isParam("USEMULTIPLEPM"));

        chatNameColorField.setText(mwclient.getConfig().getParam("COLOR"));
        foregroundColorField.setText(mwclient.getConfig().getParam("CHATFONTCOLOR"));
        backgroundColorField.setText(mwclient.getConfig().getParam("BACKGROUNDCOLOR"));
        chatFontField.setText(mwclient.getConfig().getParam("CHATFONTSIZE"));

        f1Field.setText(mwclient.getConfig().getParam("F1BIND"));
        f2Field.setText(mwclient.getConfig().getParam("F2BIND"));
        f3Field.setText(mwclient.getConfig().getParam("F3BIND"));
        f4Field.setText(mwclient.getConfig().getParam("F4BIND"));
        f5Field.setText(mwclient.getConfig().getParam("F5BIND"));

        // Dedicated Host Tab
        enableDedicatedServerCB.setSelected(mwclient.isDedicated());
        portField.setText(mwclient.getConfig().getParam("PORT").trim());
        nameField.setText(mwclient.getConfig().getParam("NAME").trim());
        restartField.setText(mwclient.getConfig().getParam("DEDAUTORESTART").trim());
        ownersField.setText(mwclient.getConfig().getParam("DEDICATEDOWNERNAME").trim());
        socketTimeOutField.setText(mwclient.getConfig().getParam("SOCKETTIMEOUTDELAY").trim());
        memoryField.setText(mwclient.getConfig().getParam("DEDMEMORY").trim());

        pmReplyToSender.setSelected(mwclient.getConfig().isParam("REPLYTOSENDER"));

        rightColumnDynamicCB.setSelected(mwclient.getConfig().isParam("RIGHTCOLUMNDYNAMIC"));
        rightPilotEjectCB.setSelected(mwclient.getConfig().isParam("RIGHTPILOTEJECT"));
        rightRepairCB.setSelected(mwclient.getConfig().isParam("RIGHTREPAIR"));
        rightEngineCB.setSelected(mwclient.getConfig().isParam("RIGHTENGINE"));
        rightEquipmentCB.setSelected(mwclient.getConfig().isParam("RIGHTEQUIPMENT"));
        rightArmorCB.setSelected(mwclient.getConfig().isParam("RIGHTARMOR"));
        rightAmmoCB.setSelected(mwclient.getConfig().isParam("RIGHTAMMO"));
        rightCommanderCB.setSelected(mwclient.getConfig().isParam("RIGHTCOMMANDER"));
        // Left Column
        leftColumnDynamicCB.setSelected(mwclient.getConfig().isParam("LEFTCOLUMNDYNAMIC"));
        leftPilotEjectCB.setSelected(mwclient.getConfig().isParam("LEFTPILOTEJECT"));
        leftRepairCB.setSelected(mwclient.getConfig().isParam("LEFTREPAIR"));
        leftEngineCB.setSelected(mwclient.getConfig().isParam("LEFTENGINE"));
        leftEquipmentCB.setSelected(mwclient.getConfig().isParam("LEFTEQUIPMENT"));
        leftArmorCB.setSelected(mwclient.getConfig().isParam("LEFTARMOR"));
        leftAmmoCB.setSelected(mwclient.getConfig().isParam("LEFTAMMO"));
        leftCommanderCB.setSelected(mwclient.getConfig().isParam("LEFTCOMMANDER"));

        testBuildTableBox.setSelected(mwclient.getConfig().isParam("USETESTBUILDTABLEVIEWER"));
        expandedUnitToolTipBox.setSelected(mwclient.getConfig().isParam("EXPANDEDUNITTOOLTIP"));

        // Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        // Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();

        // Determine the new location of the window
        // int h = dialog.getSize().height;
        // int y = (dim.height - h) / 2;

        dialog.setLocationRelativeTo(mwclient.getMainFrame());
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            mwclient.getConfig().setParam("UNITHEX", Boolean.toString(showHexinHQBox.isSelected()));
            mwclient.getConfig().setParam("STATUSINTRAYICON", Boolean.toString(useStatusForIconBox.isSelected()));
            mwclient.getConfig().setParam("ShowUnitTechBase", Boolean.toString(showUnitTechBaseCheckBox.isSelected()));
            mwclient.getConfig().setParam("ShowUnitBaseBV", Boolean.toString(showUnitBaseBVCheckBox.isSelected()));
            mwclient.getConfig().setParam("DARKERMAP", Boolean.toString(darkenMapBox.isSelected()));
            mwclient.getConfig().setParam("BMPREVIEWIMAGE", Boolean.toString(bmPreviewImageBox.isSelected()));

            // don't let people do stupid things with the Columns and crash the
            // client.
            if (Integer.parseInt(hqColumnsField.getText()) < 1) {
                hqColumnsField.setText("8");// no negatives or 0's allowed
            }

            mwclient.getConfig().setParam("UNITAMOUNT", hqColumnsField.getText());
            mwclient.getConfig().setParam("DEFAULTARMYNAME", defaultArmyNameField.getText());
            mwclient.getConfig().setParam("MAPOVERLAYCOLOR", mapOverLayField.getText());
            mwclient.getConfig().setParam("NAME", uNameField.getText());
            mwclient.getConfig().setParam("NAMEPASSWORD", new String(passwordField.getPassword()));
            mwclient.setUsername(uNameField.getText());
            mwclient.setPassword(new String(passwordField.getPassword()));
            mwclient.getConfig().setParam("IGNOREPRIVATE", ignorePrivateField.getText());
            mwclient.getConfig().setParam("IGNOREHOUSE", ignoreHouseField.getText());
            mwclient.getConfig().setParam("IGNOREPUBLIC", ignorePublicField.getText());
            mwclient.getConfig().setParam("CHALLENGESTRING", challengeStringField.getText());
            mwclient.getConfig().setParam("TIMESTAMP", Boolean.toString(timeStampBox.isSelected()));
            mwclient.getConfig().setParam("VIEWFLUFF", Boolean.toString(viewUnitFluffBox.isSelected()));
            mwclient.getConfig().setParam("LOGO", Boolean.toString(viewLogoBox.isSelected()));
            mwclient.getConfig().setParam("POPUPONATTACK", Boolean.toString(armyPopUpBox.isSelected()));
            mwclient.getConfig().setParam("AUTOCONNECT", Boolean.toString(autoConnectBox.isSelected()));
            mwclient.getConfig().setParam("SOUNDONCALL", soundOnCallField.getText());
            mwclient.getConfig().setParam("SOUNDONKEYWORD", soundOnKeywordField.getText());
            mwclient.getConfig().setParam("SOUNDONMESSAGE", soundOnMessageField.getText());
            mwclient.getConfig().setParam("SOUNDONATTACK", soundOnAttackField.getText());
            mwclient.getConfig().setParam("SOUNDONBMWIN", soundOnBMWinField.getText());
            mwclient.getConfig().setParam("SOUNDONACTIVATE", soundOnActivateField.getText());
            mwclient.getConfig().setParam("SOUNDONDEACTIVATE", soundOnDeactivateField.getText());
            mwclient.getConfig().setParam("SOUNDONENEMYDETECTED", soundOnEnemyDetectedField.getText());
            mwclient.getConfig().setParam("SOUNDONEXITCLIENT", soundOnExitClientField.getText());
            mwclient.getConfig().setParam("SOUNDONMENUPOPUP", soundOnMenuPopupField.getText());
            mwclient.getConfig().setParam("SOUNDONMENU", soundOnMenuField.getText());
            mwclient.getConfig().setParam("MAXPLAYERS", maxPlayersField.getText());
            mwclient.getConfig().setParam("COMMENT", hostCommentsField.getText());
            mwclient.getConfig().setParam("KEYWORDS", keywordsField.getText());

            mwclient.getConfig().setParam("MAXPMMESSAGE", maxMailTabStringField.getText());
            mwclient.getConfig().setParam("MAXPMTABS", maxNumberOfMailTabsField.getText());

            mwclient.getConfig().setParam("USETESTBUILDTABLEVIEWER", Boolean.toString(testBuildTableBox.isSelected()));
            mwclient.getConfig().setParam("EXPANDEDUNITTOOLTIP", Boolean.toString(expandedUnitToolTipBox.isSelected()));

            // set the HQCOLORSCHEME based on selected button.
            // private final String[] schemeChoices = {"Grey", "Tan",
            // "Classic"};
            if (schemeComboBox.getSelectedIndex() == 1) {
                mwclient.getConfig().setParam("HQCOLORSCHEME", "tan");
            } else if (schemeComboBox.getSelectedIndex() == 0) {
                mwclient.getConfig().setParam("HQCOLORSCHEME", "grey");
            } else {// scheme is classic
                mwclient.getConfig().setParam("HQCOLORSCHEME", "classic");
            }

            // set the LOOKANDFEEL based on selected button.
            if (lookandfeelComboBox.getSelectedIndex() == 1) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "motif");
            } else if (lookandfeelComboBox.getSelectedIndex() == 2) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "metal");// note:
                // this is
                // actually
                // "Ocean"
                // in Java
                // 1.5
            } else if (lookandfeelComboBox.getSelectedIndex() == 3) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "metouia");
            } else if (lookandfeelComboBox.getSelectedIndex() == 4) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "plastic");
            } else if (lookandfeelComboBox.getSelectedIndex() == 5) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "plastic3d");
            } else if (lookandfeelComboBox.getSelectedIndex() == 6) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "plasticxp");
            } else if (lookandfeelComboBox.getSelectedIndex() == 7) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "steel");
            } else if (lookandfeelComboBox.getSelectedIndex() == 8) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "jwindows");
            } else if (lookandfeelComboBox.getSelectedIndex() == 9) {
                mwclient.getConfig().setParam("LOOKANDFEEL", "skins");
                mwclient.getConfig().setParam("LOOKANDFEELSKIN", skinComboBox.getSelectedItem().toString());
            } else {// skin is system
                mwclient.getConfig().setParam("LOOKANDFEEL", "system");
            }

            // set the SYSMESSAGECOLOR based on selected button
            // sysMessageColorChoices = {"Dark Green", "Gold", "Indigo", "Navy",
            // "Orange", "Red", "Teal"};
            if (sysMessageColorComboBox.getSelectedIndex() == 0) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "#006400");// dark
                // green
            } else if (sysMessageColorComboBox.getSelectedIndex() == 1) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "#FFD700");// gold
            } else if (sysMessageColorComboBox.getSelectedIndex() == 2) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "#4B0082");// indigo
            } else if (sysMessageColorComboBox.getSelectedIndex() == 3) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "navy");
            } else if (sysMessageColorComboBox.getSelectedIndex() == 4) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "#FFA500");// orange
            } else if (sysMessageColorComboBox.getSelectedIndex() == 6) {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "teal");
            } else if (sysMessageColorComboBox.getSelectedIndex() == 5) {// colour
                // is
                // red
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "red");
            } else {
                mwclient.getConfig().setParam("SYSMESSAGECOLOR", "black");
            }

            // set the PLAYERCHATCOLORMODE based on selected button.
            // private final String[] playerChatColorChoices =
            // {"Player Defined", "Faction Colors", "Mixed (Faction Tag)",
            // "Mixed (Faction Name)"};
            if (playerChatColorComboBox.getSelectedIndex() == 2) {
                mwclient.getConfig().setParam("PLAYERCHATCOLORMODE", "factionadd");
            } else if (playerChatColorComboBox.getSelectedIndex() == 1) {
                mwclient.getConfig().setParam("PLAYERCHATCOLORMODE", "factionall");
            } else if (playerChatColorComboBox.getSelectedIndex() == 3) {
                mwclient.getConfig().setParam("PLAYERCHATCOLORMODE", "factionname");
            } else {// skin is system
                mwclient.getConfig().setParam("PLAYERCHATCOLORMODE", "playercolors");
            }

            switch (playerMessageTabComboBox.getSelectedIndex()) {
            case 1:
                mwclient.getConfig().setParam("USERDEFINDMESSAGETAB", "5");
                break;
            case 2:
                mwclient.getConfig().setParam("USERDEFINDMESSAGETAB", "4");
                break;
            case 3:
                mwclient.getConfig().setParam("USERDEFINDMESSAGETAB", "3");
                break;
            default:
                mwclient.getConfig().setParam("USERDEFINDMESSAGETAB", "0");

            }

            mwclient.getConfig().setParam("ENABLECALLSOUND", Boolean.toString(enableSoundOnCall.isSelected()));
            mwclient.getConfig().setParam("ENABLEMESSAGESOUND", Boolean.toString(enableSoundOnMessage.isSelected()));
            mwclient.getConfig().setParam("ENABLEATTACKSOUND", Boolean.toString(enableSoundOnAttack.isSelected()));
            mwclient.getConfig().setParam("ENABLEKEYWORDSOUND", Boolean.toString(enableSoundOnKeyword.isSelected()));
            mwclient.getConfig().setParam("ENABLEBMSOUND", Boolean.toString(enableSoundOnBMWin.isSelected()));
            mwclient.getConfig().setParam("ENABLEACTIVATESOUND", Boolean.toString(enableSoundOnActivate.isSelected()));
            mwclient.getConfig().setParam("ENABLEDEACTIVATESOUND", Boolean.toString(enableSoundOnDeactivate.isSelected()));
            mwclient.getConfig().setParam("ENABLEENEMYDETECTEDSOUND", Boolean.toString(enableSoundOnEnemyDetected.isSelected()));
            mwclient.getConfig().setParam("ENABLEEXITCLIENTSOUND", Boolean.toString(enableSoundOnExitClient.isSelected()));
            mwclient.getConfig().setParam("ENABLEMENUSOUND", Boolean.toString(enableSoundOnMenu.isSelected()));
            mwclient.getConfig().setParam("ENABLEMENUPOPUPSOUND", Boolean.toString(enableSoundOnMenuPopup.isSelected()));
            mwclient.getConfig().setParam("SOUNDSFROMSYSMESSAGES", Boolean.toString(systemMessageKeyword.isSelected()));
            mwclient.getConfig().setParam("COLOREDEMOTES", Boolean.toString(colorEmotesBox.isSelected()));
            mwclient.getConfig().setParam("SHOWENTERANDEXIT", Boolean.toString(showEnterExitBox.isSelected()));
            mwclient.getConfig().setParam("NOIMGINCHAT", Boolean.toString(blockImagesBox.isSelected()));
            mwclient.getConfig().setParam("MAPTABONCLICK", Boolean.toString(mapOnClickBox.isSelected()));

            mwclient.getConfig().setParam("INVERTCHATCOLOR", Boolean.toString(invertChatColors.isSelected()));

            mwclient.getConfig().setParam("HQTABNAME", hqTabNameField.getText());
            mwclient.getConfig().setParam("HQMNEMONIC", hqTabMnemonicField.getText());
            mwclient.getConfig().setParam("HQINTOPROW", Boolean.toString(hqTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("HQTABVISIBLE", Boolean.toString(hqTabVisBox.isSelected()));

            mwclient.getConfig().setParam("RULESTABNAME", rulesTabNameField.getText()); //@salient
            mwclient.getConfig().setParam("RULESMNEMONIC", rulesTabMnemonicField.getText());
            mwclient.getConfig().setParam("RULESINTOPROW", Boolean.toString(rulesTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("RULESTABVISIBLE", Boolean.toString(rulesTabVisBox.isSelected()));

            mwclient.getConfig().setParam("BMTABNAME", bmTabNameField.getText());
            mwclient.getConfig().setParam("BMMNEMONIC", bmTabMnemonicField.getText());
            mwclient.getConfig().setParam("BMINTOPROW", Boolean.toString(bmTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("BMTABVISIBLE", Boolean.toString(bmTabVisBox.isSelected()));

            mwclient.getConfig().setParam("BMETABNAME", bmeTabNameField.getText());
            mwclient.getConfig().setParam("BMEMNEMONIC", bmeTabMnemonicField.getText());
            mwclient.getConfig().setParam("BMEINTOPROW", Boolean.toString(bmeTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("BMETABVISIBLE", Boolean.toString(bmeTabVisBox.isSelected()));

            mwclient.getConfig().setParam("HSTATUSTABNAME", hsTabNameField.getText());
            mwclient.getConfig().setParam("HSTATUSMNEMONIC", hsTabMnemonicField.getText());
            mwclient.getConfig().setParam("HSTATUSINTOPROW", Boolean.toString(hsTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("HSTATUSTABVISIBLE", Boolean.toString(hsTabVisBox.isSelected()));

            mwclient.getConfig().setParam("BATTLETABNAME", batTabNameField.getText());
            mwclient.getConfig().setParam("BATTLEMNEMONIC", batTabMnemonicField.getText());
            mwclient.getConfig().setParam("BATTLEINTOPROW", Boolean.toString(batTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("BATTLETABVISIBLE", Boolean.toString(batTabVisBox.isSelected()));

            mwclient.getConfig().setParam("MAPTABNAME", mapTabNameField.getText());
            mwclient.getConfig().setParam("MAPMNEMONIC", mapTabMnemonicField.getText());
            mwclient.getConfig().setParam("MAPINTOPROW", Boolean.toString(mapTabonTopBox.isSelected()));
            mwclient.getConfig().setParam("MAPTABVISIBLE", Boolean.toString(mapTabVisBox.isSelected()));

            mwclient.getConfig().setParam("MAINCHANNELTABNAME", mcTabNameField.getText());
            mwclient.getConfig().setParam("MAINCHANNELMNEMONIC", mcTabMnemonicField.getText());

            mwclient.getConfig().setParam("HOUSEMAILTABNAME", hmTabNameField.getText());
            mwclient.getConfig().setParam("HOUSEMAILMNEMONIC", hmTabMnemonicField.getText());
            mwclient.getConfig().setParam("HOUSEMAILVISIBLE", Boolean.toString(hmTabVisBox.isSelected()));

            mwclient.getConfig().setParam("PRIVATEMAILTABNAME", pmTabNameField.getText());
            mwclient.getConfig().setParam("PRIVATEMAILMNEMONIC", pmTabMnemonicField.getText());
            mwclient.getConfig().setParam("PRIVATEMAILVISIBLE", Boolean.toString(pmTabVisBox.isSelected()));

            mwclient.getConfig().setParam("PERSONALLOGTABNAME", pLogTabNameField.getText());
            mwclient.getConfig().setParam("PERSONALLOGMNEMONIC", pLogTabMnemonicField.getText());
            mwclient.getConfig().setParam("PERSONALLOGVISIBLE", Boolean.toString(pLogTabVisBox.isSelected()));

            mwclient.getConfig().setParam("SYSTEMLOGTABNAME", sysLogTabNameField.getText());
            mwclient.getConfig().setParam("SYSTEMLOGMNEMONIC", sysLogTabMnemonicField.getText());
            mwclient.getConfig().setParam("SYSTEMLOGVISIBLE", Boolean.toString(sysLogTabVisBox.isSelected()));

            mwclient.getConfig().setParam("MISCELLANEOUSTABNAME", miscTabNameField.getText());
            mwclient.getConfig().setParam("MISCELLANEOUSMNEMONIC", miscTabMnemonicField.getText());
            mwclient.getConfig().setParam("MISCELLANEOUSVISIBLE", Boolean.toString(miscTabVisBox.isSelected()));

            mwclient.getConfig().setParam("RPGTABNAME", RPGTabNameField.getText());
            mwclient.getConfig().setParam("RPGMNEMONIC", RPGTabMnemonicField.getText());
            mwclient.getConfig().setParam("RPGVISIBLE", Boolean.toString(RPGTabVisBox.isSelected()));

            mwclient.getConfig().setParam("MAINCHANNELHM", Boolean.toString(hmInMainBox.isSelected()));
            mwclient.getConfig().setParam("MAINCHANNELPM", Boolean.toString(pmInMainBox.isSelected()));
            mwclient.getConfig().setParam("MAINCHANNELSM", Boolean.toString(sysLogInMainBox.isSelected()));
            mwclient.getConfig().setParam("MAINCHANNELMISC", Boolean.toString(miscInMainBox.isSelected()));
            mwclient.getConfig().setParam("MAINCHANNELRPG", Boolean.toString(RPGInMainBox.isSelected()));

            mwclient.getConfig().setParam("REPLYTOSENDER", Boolean.toString(pmReplyToSender.isSelected()));
            mwclient.getConfig().setParam("REPLYTORECEIVER", Boolean.toString(pmReplyToReciever.isSelected()));
            mwclient.getConfig().setParam("USEMULTIPLEPM", Boolean.toString(pmUseMultipleTabs.isSelected()));
            mwclient.getConfig().setParam("COLOR", chatNameColorField.getText());
            mwclient.getConfig().setParam("CHATFONTCOLOR", foregroundColorField.getText());
            mwclient.getConfig().setParam("BACKGROUNDCOLOR", backgroundColorField.getText());
            mwclient.getConfig().setParam("CHATFONTSIZE", chatFontField.getText());

            mwclient.getConfig().setParam("F1BIND", f1Field.getText());
            mwclient.getConfig().setParam("F2BIND", f2Field.getText());
            mwclient.getConfig().setParam("F3BIND", f3Field.getText());
            mwclient.getConfig().setParam("F4BIND", f4Field.getText());
            mwclient.getConfig().setParam("F5BIND", f5Field.getText());

            // Dedicated Host
            mwclient.getConfig().setParam("DEDICATED", Boolean.toString(enableDedicatedServerCB.isSelected()));
            mwclient.getConfig().setParam("PORT", portField.getText().trim());
            mwclient.getConfig().setParam("SOCKETTIMEOUTDELAY", socketTimeOutField.getText().trim());

            // only save a new host name if Dedicated is in use
            if (enableDedicatedServerCB.isSelected()) {
                mwclient.getConfig().setParam("NAME", nameField.getText().trim());
            }

            mwclient.getConfig().setParam("DEDAUTORESTART", restartField.getText().trim());
            mwclient.getConfig().setParam("DEDICATEDOWNERNAME", ownersField.getText().trim());
            mwclient.getConfig().setParam("DEDMEMORY", memoryField.getText().trim());

            // Right Column
            mwclient.getConfig().setParam("RIGHTCOLUMNDYNAMIC", Boolean.toString(rightColumnDynamicCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTPILOTEJECT", Boolean.toString(rightPilotEjectCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTREPAIR", Boolean.toString(rightRepairCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTENGINE", Boolean.toString(rightEngineCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTEQUIPMENT", Boolean.toString(rightEquipmentCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTARMOR", Boolean.toString(rightArmorCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTAMMO", Boolean.toString(rightAmmoCB.isSelected()));
            mwclient.getConfig().setParam("RIGHTCOMMANDER", Boolean.toString(rightCommanderCB.isSelected()));
            // Left Column
            mwclient.getConfig().setParam("LEFTCOLUMNDYNAMIC", Boolean.toString(leftColumnDynamicCB.isSelected()));
            mwclient.getConfig().setParam("LEFTPILOTEJECT", Boolean.toString(leftPilotEjectCB.isSelected()));
            mwclient.getConfig().setParam("LEFTREPAIR", Boolean.toString(leftRepairCB.isSelected()));
            mwclient.getConfig().setParam("LEFTENGINE", Boolean.toString(leftEngineCB.isSelected()));
            mwclient.getConfig().setParam("LEFTEQUIPMENT", Boolean.toString(leftEquipmentCB.isSelected()));
            mwclient.getConfig().setParam("LEFTARMOR", Boolean.toString(leftArmorCB.isSelected()));
            mwclient.getConfig().setParam("LEFTAMMO", Boolean.toString(leftAmmoCB.isSelected()));
            mwclient.getConfig().setParam("LEFTCOMMANDER", Boolean.toString(leftCommanderCB.isSelected()));

            mwclient.setIgnoreHouse();
            mwclient.setIgnorePrivate();
            mwclient.setIgnorePublic();
            mwclient.setKeyWords();
            mwclient.getConfig().saveConfig();
            mwclient.setConfig();
            mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "color " + chatNameColorField.getText());

            if (mwclient.getPlayer().getAutoReorder() != autoReOrder.isSelected()) {
                mwclient.sendChat(MWClient.CAMPAIGN_PREFIX + "setautoreorder " + autoReOrder.isSelected());
            }

            mwclient.getMainFrame().getMainPanel().recreateMainTPane(mwclient.getMainFrame());

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

            if (!mwclient.getConfigParam("LOOKANDFEEL").equalsIgnoreCase(originalLookAndFeel) || (mwclient.getConfigParam("LOOKANDFEEL").equalsIgnoreCase("skins") && !mwclient.getConfigParam("LOOKANDFEELSKIN").equalsIgnoreCase(originalSkin))) {
                mwclient.setLookAndFeel(true);
            }

            if (!mwclient.getConfigParam("UNITHEX").equals(originalUnitHex)) {
                unitHexChanged = true;
            }

            if (!mwclient.getConfigParam("HQCOLORSCHEME").equalsIgnoreCase(originalScheme)) {
                schemeChanged = true;
            }

            if (!mwclient.getConfigParam("DARKERMAP").equalsIgnoreCase(originalScheme)) {
                mapBrightnessChanged = true;
            }

            int currColumns = Integer.parseInt(mwclient.getConfigParam("UNITAMOUNT"));
            if (currColumns != originalColumns) {
                columnsChanged = true;
            }

            if (columnsChanged || schemeChanged || unitHexChanged) {
                // only reinit. no image loading.
                mwclient.getMainFrame().getMainPanel().selectFirstTab();
                mwclient.getMainFrame().getMainPanel().getCommPanel().selectFirstTab();
                mwclient.getMainFrame().getMainPanel().getHQPanel().reinitialize();
            }

            if (mapBrightnessChanged) {
                mwclient.getMainFrame().getMainPanel().getMapPanel().repaint();
            }

            if (!mwclient.getConfigParam("BMPREVIEWIMAGE").equalsIgnoreCase(originalBMPreview)) {
                mwclient.getMainFrame().getMainPanel().getBMPanel().resetButtonBar();
            }

            mwclient.addToChat("</BODY></html><html><BODY  TEXT=\"" + mwclient.getConfig().getParam("CHATFONTCOLOR") + "\" BGCOLOR=\"" + mwclient.getConfig().getParam("BACKGROUNDCOLOR") + "\"></BODY>");
        } else {
            dialog.dispose();
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
        } else if (command.equals(camoCommand)) {
            CamoSelectionDialog camoDialog = new CamoSelectionDialog(mwclient.getMainFrame(), mwclient);
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
