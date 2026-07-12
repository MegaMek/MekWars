/*
 * MekWars - Copyright (C) 2008
 *
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

/**
 * @author jtighe
 *       <p>
 *       Basic and advanced dialog for converting components into crits
 */

package mekwars.common.gui.dialogs;

import java.util.Objects;

import megamek.common.TechConstants;
import mekwars.common.BMEquipment;
import mekwars.common.House;
import mekwars.common.Unit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.ComponentToCritsConverter;

/**
 * Modal admin/mod tool for configuring the "component to crits" conversion table used by
 * {@link ComponentToCritsConverter}: how many units of a given Black Market component are
 * required to yield one critical-slot-worth of salvage, per faction. Opened on demand (e.g. from
 * a menu action) rather than being embedded in another window; the constructor itself builds,
 * shows, and blocks on the dialog.
 * <p>
 * The dialog has two view modes, toggled via the "Advanced"/"Basic" button:
 * <ul>
 * <li><b>Basic</b> — a single row configuring one blanket conversion rule ("All") applied to
 * every component.</li>
 * <li><b>Advanced</b> — one row per known Black Market component ({@link BMEquipment}) that the
 * player's (or, for mods/admins, the selected faction's) tech level permits and that has a
 * nonzero cost, each independently configurable.</li>
 * </ul>
 * Regular players only see/edit their own house's configuration; moderators and admins
 * additionally get a faction selector ({@link #factionCombo}) to view/edit any house's table.
 * Clicking OK pushes any changed rows to the server as {@code Setcomponentconversion} chat
 * commands and requests the updated table; clicking Cancel simply closes the dialog.
 */
public final class ComponentConverterDialog implements java.awt.event.ActionListener {

    /** Action command used by the OK button; confirms and pushes edited configs to the server. */
    private final static String okayCommand = "okay";
    /** Action command used by the Cancel button; closes the dialog without saving. */
    private final static String cancelCommand = "cancel";
    /** Action command used by the mode-toggle button; switches between basic and advanced views. */
    private final static String selectorButtonCommand = "selectorbuttoncommand";
    /** Title of the dialog window. */
    private final static String windowName = "Component Crit Converter";

    private final javax.swing.JPanel mainPanel = new javax.swing.JPanel(); // main Panel for everything
    private final javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(); // the scrolly thingy
    /** Outer panel holding {@link #factionCombo} (if visible) and {@link #scrollPane}. */
    private final javax.swing.JPanel masterPanel = new javax.swing.JPanel();
    /** Faction picker, only added to the layout for mods/admins (see {@link #isMod}). */
    private final javax.swing.JComboBox factionCombo;
    private final javax.swing.JButton okayButton = new javax.swing.JButton("OK");
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");
    /** Toggles between "Basic" and "Advanced" editing views; its label reflects the mode you'd switch <em>to</em>. */
    private final javax.swing.JButton modeButton = new javax.swing.JButton("Advanced");
    /** Whether the current player is a moderator or admin, granting access to edit any faction's table. */
    private final boolean isMod;
    /** The actual dialog window, created from {@link #pane} via {@link javax.swing.JOptionPane#createDialog}. */
    private final javax.swing.JDialog dialog;
    /** Backing option pane whose {@code getValue()} reports which of the three buttons closed the dialog. */
    private final javax.swing.JOptionPane pane;
    /** Display names for the unit type combo used in each advanced-mode row. */
    String[] units = { Unit.getTypeClassDesc(Unit.MEK), Unit.getTypeClassDesc(Unit.VEHICLE),
                       Unit.getTypeClassDesc(Unit.INFANTRY), Unit.getTypeClassDesc(Unit.PROTOMEK),
                       Unit.getTypeClassDesc(Unit.BATTLEARMOR), Unit.getTypeClassDesc(Unit.AERO) };
    /** Display names for the weight class combo used in each row. */
    String[] weight = { Unit.getWeightClassDesc(Unit.LIGHT), Unit.getWeightClassDesc(Unit.MEDIUM),
                        Unit.getWeightClassDesc(Unit.HEAVY), Unit.getWeightClassDesc(Unit.ASSAULT) };
    IClient client;
    /** Current view mode: {@code false} = basic (single blanket rule), {@code true} = advanced (per-component). */
    private boolean isAdvanced = false;
    /** Last-used "basic" mode weight class; seeds new advanced-mode rows when switching from basic to advanced. */
    private int basicWeight = Unit.LIGHT;
    /** Last-used "basic" mode unit type; seeds new advanced-mode rows when switching from basic to advanced. */
    private int basicType = Unit.MEK;
    /** Last-used "basic" mode crit-level amount; seeds new advanced-mode rows when switching from basic to advanced. */
    private int basicAmount = 100;

    /**
     * Builds the dialog, populates it for the local player's house (or, for mods, waits on the
     * faction combo's default selection), shows it modally, and — if the user clicks OK — walks
     * every row currently displayed and pushes any changed conversion settings to the server
     * before requesting a refreshed conversion table. Returns only after the dialog is closed.
     *
     * @param client connection used to read campaign/faction data and send chat commands
     */
    public ComponentConverterDialog(IClient client) {

        this.client = client;

        isMod = client.isMod() || client.isAdmin();

        java.util.Collection<House> factions = client.getData().getAllHouses();
        java.util.TreeSet<String> factionNames = new java.util.TreeSet<>();// tree to alpha sort
        for (House house : factions) {
            factionNames.add(house.getName());
        }
        factionCombo = new javax.swing.JComboBox(factionNames.toArray());
        factionCombo.addActionListener(this);

        if (isMod) {
            masterPanel.add(factionCombo);
        }

        scrollPane.add(mainPanel);
        scrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setViewportView(mainPanel);

        mainPanel.setLayout(new javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS));
        masterPanel.setLayout(new javax.swing.BoxLayout(masterPanel, javax.swing.BoxLayout.Y_AXIS));

        masterPanel.add(scrollPane);


        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);

        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(cancelCommand);

        modeButton.addActionListener(this);
        modeButton.setActionCommand(selectorButtonCommand);

        // Set the user's options
        Object[] options = { okayButton, cancelButton, modeButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(masterPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        java.awt.Dimension maxSize = new java.awt.Dimension(120, 50);

        factionCombo.setMaximumSize(maxSize);
        factionCombo.setPreferredSize(maxSize);
        // Create the main dialog and set the default button
        dialog = pane.createDialog(scrollPane, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);

        // Show the dialog and get the user's input
        dialog.setLocationRelativeTo(client.getMainFrame());
        dialog.setModal(true);
        dialog.setResizable(true);
        if (isMod) {
            factionCombo.setSelectedIndex(0);
        } else {
            requestComponents(client.getPlayer().getHouse());
        }
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() == okayButton) {

            for (int pos = mainPanel.getComponentCount() - 1; pos >= 0; pos--) {
                javax.swing.JPanel panel = (javax.swing.JPanel) mainPanel.getComponent(pos);
                findAndSaveConfigs(panel);
            }
            client.sendChat(IClient.CAMPAIGN_PREFIX + "c getcomponentconversion");
        } else {
            dialog.dispose();
        }
    }

    /**
     * This method will tunnel through all of the panels of the config UI to find any changed text fields or checkboxes.
     * Then it will send the new configs to the server.
     * <p>
     * Recurses into any nested {@link javax.swing.JPanel} first (depth-first), then reads the
     * current row's crit name (a JTextField whose name is the crit/equipment internal name),
     * amount (the JTextField named "amount"), and weight/type (two JComboBoxes named "weight" and,
     * implicitly, everything else being treated as the type combo). If the resulting values differ
     * from the server's currently-known {@link ComponentToCritsConverter} for that crit, sends a
     * {@code Setcomponentconversion} chat command (including the selected faction if the current
     * user {@link #isMod}). Only actually sends anything for the values found on the last (topmost)
     * panel visited in each recursive branch, since {@code crit}/{@code amount}/{@code weight}/
     * {@code type} are local to each invocation.
     *
     * @param panel a row panel (or a panel containing row panels) from {@link #mainPanel}
     */
    public void findAndSaveConfigs(javax.swing.JPanel panel) {
        String crit = null;
        String amount = null;
        int weight = 0;
        int type = 0;
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof javax.swing.JPanel) {
                findAndSaveConfigs((javax.swing.JPanel) field);
            } else if (field instanceof javax.swing.JTextField textBox) {

                if (textBox.getName().equals("amount")) {
                    amount = textBox.getText();
                } else {
                    crit = textBox.getName();
                }
            } else if (field instanceof javax.swing.JComboBox combo) {

                if (combo.getName().equals("weight")) {
                    weight = combo.getSelectedIndex();
                } else {
                    type = combo.getSelectedIndex();
                }

            }
        }

        ComponentToCritsConverter converter = client.getCampaign().getComponentConverter().get(crit);

        if (converter == null || converter.getComponentUsedType() != type
                  || converter.getComponentUsedWeight() != weight
                  || converter.getMinCritLevel() != Integer.parseInt(Objects.requireNonNull(amount))) {

            if (isMod) {
                client.sendChat(
                      String.format("%sc Setcomponentconversion#%s#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, crit, weight, type, amount, Objects.requireNonNull(
                            factionCombo.getSelectedItem()).toString()));
            } else {
                client.sendChat(
                      String.format("%sc Setcomponentconversion#%s#%s#%s#%s", IClient.CAMPAIGN_PREFIX, crit, weight, type, amount));
            }
        }

    }

    /**
     * Mirror of {@link #findAndSaveConfigs(javax.swing.JPanel)} used when leaving "basic" mode:
     * recurses through {@code panel} (the single basic-mode row) and caches its amount/weight/type
     * values into {@link #basicAmount}, {@link #basicWeight}, {@link #basicType} so that newly
     * created advanced-mode rows (see {@link #switchView()}) can be seeded with sensible defaults
     * rather than always falling back to the hardcoded {@link Unit#LIGHT}/{@link Unit#MEK}/100.
     *
     * @param panel the basic-mode row panel to read values from
     */
    public void findAndBasicConfigs(javax.swing.JPanel panel) {
        for (int fieldPos = panel.getComponentCount() - 1; fieldPos >= 0; fieldPos--) {

            Object field = panel.getComponent(fieldPos);

            // found another JPanel keep digging!
            if (field instanceof javax.swing.JPanel) {
                findAndBasicConfigs((javax.swing.JPanel) field);
            } else if (field instanceof javax.swing.JTextField textBox) {

                if (textBox.getName().equals("amount")) {
                    basicAmount = Integer.parseInt(textBox.getText());
                }
            } else if (field instanceof javax.swing.JComboBox combo) {

                if (combo.getName().equals("weight")) {
                    basicWeight = combo.getSelectedIndex();
                } else {
                    basicType = combo.getSelectedIndex();
                }

            }
        }
    }

    /**
     * Central dispatcher for the dialog's controls. OK/Cancel record which button was pressed on
     * {@link #pane} (read back by the constructor after {@code dialog.setVisible(true)} returns)
     * and close the dialog; the mode button toggles basic/advanced view via {@link #switchView()};
     * and — since {@link #factionCombo} is also routed through this listener — any other combo box
     * source (only {@link #factionCombo} in practice) triggers {@link #requestComponents(String)}
     * for the newly-selected faction, but only for mods/admins.
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();
        if (command.equals(okayCommand)) {
            pane.setValue(okayButton);
            dialog.dispose();
        } else if (command.equals(cancelCommand)) {
            pane.setValue(cancelButton);
            dialog.dispose();
        } else if (command.equals(selectorButtonCommand)) {
            switchView();
        } else if (e.getSource() instanceof javax.swing.JComboBox box) {
            if (isMod) {
                requestComponents(Objects.requireNonNull(box.getSelectedItem()).toString());
            }
        }

    }

    /**
     * Asks the server for the given faction's component-conversion table and blocks the calling
     * thread until the response arrives, polling {@link IClient#isWaiting()} every 100ms.
     * <p>
     * Note: this is a busy-wait loop with {@link Thread#sleep(long)}; if invoked on the Swing
     * event dispatch thread (as it is here, from {@link #actionPerformed}/the constructor) it will
     * freeze the UI for the duration of the round trip rather than yielding to the EDT. This is
     * existing behavior, not something this pass changes.
     * <p>
     * Once the response is in, determines whether the received table is basic-mode (i.e. contains
     * only an "All" entry) or advanced-mode, sets {@link #isAdvanced} accordingly, and rebuilds the
     * UI via {@link #switchView()}.
     *
     * @param faction name of the faction/house whose conversion table to fetch
     */
    private void requestComponents(String faction) {
        client.setWaiting(true);
        client.sendChat(String.format("%sc getcomponentconversion#%s", IClient.CAMPAIGN_PREFIX, faction));
        while (client.isWaiting()) {
            try {
                Thread.sleep(100);
            } catch (Exception ex) {

            }
        }
        isAdvanced = !client.getCampaign().getComponentConverter().containsKey("All");
        switchView();
    }


    /**
     * Rebuilds {@link #mainPanel} for the opposite mode from {@link #isAdvanced} and flips that
     * flag. When switching to basic mode, discards all existing rows and adds a single non-editable
     * "All" row seeded from the server's "All" converter (or sensible defaults if none exists yet).
     * When switching to advanced mode, first harvests the outgoing basic row's values via
     * {@link #findAndBasicConfigs(javax.swing.JPanel)}, then adds one row per known
     * {@link BMEquipment} whose tech level the player's house may use (or that is universally
     * available) and whose cost is greater than zero, seeded from that item's existing converter
     * or from the harvested basic values. Also resizes {@link #scrollPane} and {@link #dialog} to
     * fixed pixel dimensions appropriate to each mode (compact for basic, larger for the
     * potentially long advanced list) and updates {@link #modeButton}'s label.
     */
    public void switchView() {

        javax.swing.JPanel critPanel;
        javax.swing.JTextField baseTextField;
        javax.swing.JComboBox weightCombo;
        javax.swing.JComboBox typeCombo;
        if (!isAdvanced) {
            mainPanel.removeAll();
            scrollPane.setSize(300, 40);
            scrollPane.setPreferredSize(scrollPane.getSize());
            scrollPane.setMinimumSize(scrollPane.getSize());
            dialog.setSize(400, 140);
            dialog.setPreferredSize(dialog.getSize());
            dialog.setMinimumSize(dialog.getSize());

            ComponentToCritsConverter converter = client.getCampaign().getComponentConverter().get("All");

            if (converter == null) {
                converter = new ComponentToCritsConverter();
                converter.setCritName("All");
                converter.setComponentUsedType(Unit.MEK);
                converter.setComponentUsedWeight(Unit.LIGHT);
                converter.setMinCritLevel(100);
            }

            critPanel = new javax.swing.JPanel();
            baseTextField = new javax.swing.JTextField(5);
            baseTextField.setEditable(false);
            baseTextField.setName(converter.getCritName());
            baseTextField.setText(converter.getCritName());
            critPanel.add(baseTextField);

            weightCombo = new javax.swing.JComboBox(weight);
            weightCombo.setName("weight");
            weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
            critPanel.add(weightCombo);

            typeCombo = new javax.swing.JComboBox(units);
            typeCombo.setName("type");
            typeCombo.setSelectedIndex(converter.getComponentUsedType());
            critPanel.add(typeCombo);

            baseTextField = new javax.swing.JTextField(5);
            baseTextField.setName("amount");
            baseTextField.setText(Integer.toString(converter.getMinCritLevel()));
            critPanel.add(baseTextField);

            mainPanel.add(critPanel);
            modeButton.setText("Advanced");
        } else {
            findAndBasicConfigs(mainPanel);
            mainPanel.removeAll();
            for (BMEquipment eq : client.getCampaign().getBlackMarketParts().values()) {

                if ((Boolean.parseBoolean(client.getServerConfigs("AllowCrossOverTech"))
                           || client.getPlayer().getHouseFightingFor().getTechLevel() == TechConstants.T_ALL
                           || eq.getTechLevel() == TechConstants.T_ALL
                           || client.getPlayer().getHouseFightingFor().getTechLevel() >= eq.getTechLevel())
                          && eq.getCost() > 0) {

                    ComponentToCritsConverter converter = client.getCampaign()
                                                                .getComponentConverter()
                                                                .get(eq.getEquipmentInternalName());

                    if (converter == null) {
                        converter = new ComponentToCritsConverter();
                        converter.setCritName(eq.getEquipmentInternalName());
                        converter.setComponentUsedType(basicType);
                        converter.setComponentUsedWeight(basicWeight);
                        converter.setMinCritLevel(basicAmount);
                    }

                    critPanel = new javax.swing.JPanel();
                    baseTextField = new javax.swing.JTextField(25);
                    baseTextField.setEditable(false);
                    baseTextField.setName(eq.getEquipmentInternalName());
                    baseTextField.setText(eq.getEquipmentName());
                    critPanel.add(baseTextField);

                    weightCombo = new javax.swing.JComboBox(weight);
                    weightCombo.setName("weight");
                    weightCombo.setSelectedIndex(converter.getComponentUsedWeight());
                    critPanel.add(weightCombo);

                    typeCombo = new javax.swing.JComboBox(units);
                    typeCombo.setSelectedIndex(converter.getComponentUsedType());
                    typeCombo.setName("type");
                    critPanel.add(typeCombo);

                    baseTextField = new javax.swing.JTextField(5);
                    baseTextField.setName("amount");
                    baseTextField.setText(Integer.toString(converter.getMinCritLevel()));
                    critPanel.add(baseTextField);

                    mainPanel.add(critPanel);
                }
            }
            scrollPane.setSize(400, 400);
            scrollPane.setPreferredSize(scrollPane.getSize());
            scrollPane.setMinimumSize(scrollPane.getSize());
            dialog.setSize(500, 500);
            dialog.setPreferredSize(dialog.getSize());
            dialog.setMinimumSize(dialog.getSize());
            modeButton.setText("Basic");
        }
        isAdvanced = !isAdvanced;
        masterPanel.setVisible(false);
        masterPanel.setVisible(true);
    }
}
