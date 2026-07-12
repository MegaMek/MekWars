/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

//@ Salient , copy of RewardPointsDialog

package mekwars.common.gui.dialogs;


import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.UnitFactory;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Modal dialog for spending a player's "Influence" points (the political/faction currency, server-labeled via
 * the "FluLongName"/"FluShortName" config keys - e.g. "Influence Points"/"FLU"). Built directly around a
 * {@link javax.swing.JOptionPane} rather than extending {@link javax.swing.JDialog}; the constructor builds the
 * whole UI, shows the dialog modally, and blocks until the player dismisses it.
 * <p>
 * The player picks one action from a combo box:
 * <ul>
 *     <li>Convert influence to money (only offered if the server's "Cbills_Per_Flu" conversion rate is
 *     positive) - the player types an amount of influence to spend and sees the resulting C-bill gain.</li>
 *     <li>Refresh a unit factory (only offered if "FluToRefreshFactory" is positive) - the player picks one of
 *     their faction's planet/factory combos that has refresh ticks remaining, and pays the fixed influence
 *     cost from "FluToRefreshFactory" to refresh it immediately.</li>
 *     <li>If neither option is available, only a placeholder "None Available" entry is shown and choosing it
 *     just sends a "no influence spent" chat notice.</li>
 * </ul>
 * On OK the corresponding {@code useinfluence} or {@code refreshFactory} campaign chat command is sent to the
 * server; on Cancel (or choosing a disabled option) nothing is sent. The dialog disposes itself before the
 * constructor returns.
 */
public final class InfluencePointsDialog implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    /** Action command for the OK button. */
    private final static String okayCommand = "Okay";
    /** Action command for the Cancel button. */
    private final static String cancelCommand = "Cancel";
    /** Combo box entry / action command identifying the "convert influence to reward/money" action. */
    private final static String rewardCommand = "Reward";
    /** Combo box entry / action command identifying the "refresh a factory" action. */
    private final static String refreshCommand = "Refresh";
    //store the client backlink for other things to use
    /** Back-link to the client used to read server configs, faction/planet data, and to send chat commands. */
    private final IClient client;
    private final javax.swing.JButton cancelButton = new javax.swing.JButton("Cancel");

    //TEXT FIELDS
    //tab names
    /** Displays the computed cost/result of the currently selected action (e.g. "Result: Gain X C-bills"). */
    private final javax.swing.JLabel costLabel = new javax.swing.JLabel();
    /** Label paired with {@link #refreshComboBox}; only visible when the "Refresh Factory" action is selected. */
    private final javax.swing.JLabel refreshLabel = new javax.swing.JLabel("Refresh:",
          javax.swing.SwingConstants.TRAILING);

    /** Top-level action selector: which use of influence points the player wants to perform. */
    private final javax.swing.JComboBox<String> rewardsComboBox;
    /** Amount of influence points the player wants to spend, when the "convert to money" action is chosen. */
    private final javax.swing.JTextField amountText = new javax.swing.JTextField(5);
    /** Label paired with {@link #amountText}, reading "&lt;FluShortName&gt; to use:". */
    private final javax.swing.JLabel amountLabel;
    //STOCK DIALOUG AND PANE
    /** The actual modal dialog window, created from {@link #pane}. */
    private final javax.swing.JDialog dialog;
    /** Underlying option pane hosting the reward panel and OK/Cancel buttons. */
    private final javax.swing.JOptionPane pane;
    /** Computed influence-point cost of the currently selected action. */
    int cost;
    /** Lists planet/factory combinations (belonging to the player's faction) with refresh ticks remaining. */
    private javax.swing.JComboBox<String> refreshComboBox = new javax.swing.JComboBox<>();
    //	private int fluToRepod;

    /**
     * Builds the dialog UI, populates the available actions based on server configuration, and shows it modally.
     * The constructor blocks (via {@code dialog.setVisible(true)}) until the player presses OK or Cancel, at
     * which point the corresponding chat command (if any) has already been sent and the dialog is disposed.
     *
     * @param client active client connection; supplies server configs, the player's faction/planets, and sends
     *               the resulting chat command.
     */
    public InfluencePointsDialog(IClient client) {

        //save the client
        this.client = client;
        String windowName = this.client.getServerConfigs("FluLongName");
        amountLabel = new javax.swing.JLabel(this.client.getServerConfigs("FluShortName") + " to use:",
              javax.swing.SwingConstants.TRAILING);

        //COMBO BOXES
        // Alpha-sorted set of top-level action names to offer in rewardsComboBox.
        java.util.TreeSet<String> names = new java.util.TreeSet<>();

        // Only offer "convert to money" if the server defines a positive influence->C-bill exchange rate.
        if (Integer.parseInt(this.client.getServerConfigs("Cbills_Per_Flu")) > 0) {
            names.add(this.client.getServerConfigs("MoneyLongName"));
        }

        //creates a list of factories that can be refreshed
        // Only offer "refresh factory" if the server charges a positive influence cost for it, and only
        // list factories on planets owned by the player's own faction that still have refresh ticks pending.
        if (Integer.parseInt(this.client.getServerConfigs("FluToRefreshFactory")) > 0) {
            java.util.TreeSet<String> factories = new java.util.TreeSet<>();
            House faction = this.client.getData().getHouseByName(this.client.getPlayer().getHouse());
            java.util.Iterator<Planet> planets = this.client.getData().getAllPlanets().iterator();
            names.add(refreshCommand);

            while (planets.hasNext()) {
                Planet planet = planets.next();

                if (!planet.isOwner(faction.getId())) {continue;}

                for (UnitFactory factory : planet.getUnitFactories()) {
                    if (factory.getTicksUntilRefresh() > 0) {
                        factories.add(String.format("%s: %s(%s)", planet.getName(), factory.getName(), factory.getTicksUntilRefresh()));
                    }
                }
            }
            refreshComboBox = new javax.swing.JComboBox<>();
            factories.forEach(refreshComboBox::addItem);
        }

        // Neither action is enabled server-side: show a disabled placeholder entry instead of an empty combo box.
        if (names.isEmpty()) {names.add("None Available");}

        rewardsComboBox = new javax.swing.JComboBox<>();
        names.forEach(rewardsComboBox::addItem);
        //stored values.
        cost = 0;

        //Set the tooltips and actions for dialogue buttons
        //BUTTONS
        javax.swing.JButton okayButton = new javax.swing.JButton("OK");
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        rewardsComboBox.setActionCommand(rewardCommand);
        refreshComboBox.setActionCommand(refreshCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        okayButton.setToolTipText("Save Options");
        cancelButton.setToolTipText("Exit without saving changes");
        rewardsComboBox.addActionListener(this);
        refreshComboBox.addActionListener(this);

        amountText.addKeyListener(this);

        //CREATE THE PANELS
        javax.swing.JPanel rewardPanel = new javax.swing.JPanel();//player name, etc

        /*
         * Format the Reward Points panel. Spring layout.
         */
        rewardPanel.setLayout(new javax.swing.BoxLayout(rewardPanel, javax.swing.BoxLayout.Y_AXIS));

        javax.swing.JPanel comboPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
        javax.swing.JPanel costPanel = new javax.swing.JPanel();

        javax.swing.JLabel rewardLabel = new javax.swing.JLabel("Choose Action:",
              javax.swing.SwingConstants.TRAILING);
        comboPanel.add(rewardLabel);
        rewardsComboBox.setToolTipText("Select your Reward Type");
        comboPanel.add(rewardsComboBox);

        comboPanel.add(refreshLabel);
        refreshComboBox.setToolTipText("Refresh Factory");
        comboPanel.add(refreshComboBox);

        comboPanel.add(amountLabel);
        comboPanel.add(amountText);

        //run the spring layout
        SpringLayoutHelper.setupSpringGrid(comboPanel, 2);

        rewardPanel.add(comboPanel);
        costPanel.add(costLabel);
        rewardPanel.add(costPanel);

        costLabel.setText("Result: no expenditure");

        rewardsComboBox.setSelectedIndex(0);

        javax.swing.JPanel mainPanel = new javax.swing.JPanel();

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        pane = new javax.swing.JOptionPane(rewardPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);
        dialog.setLocationRelativeTo(this.client.getMainFrame());

        //Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() != okayButton) {
            dialog.dispose();
        }
    }

    /**
     * Unused; required by {@link java.awt.event.KeyListener} but this dialog only reacts on key release.
     */
    public void keyTyped(java.awt.event.KeyEvent e) {
    }

    /**
     * Unused; required by {@link java.awt.event.KeyListener} but this dialog only reacts on key release.
     */
    public void keyPressed(java.awt.event.KeyEvent e) {
    }

    /**
     * Recomputes and displays the cost/result preview whenever the player edits {@link #amountText}.
     * <p>
     * QUIRK: {@code Integer.parseInt(amountText.getText())} is called unconditionally before checking the
     * current selection; if the field is empty or non-numeric this throws a {@link NumberFormatException}
     * that propagates out of the key listener (typically just logged/swallowed by the AWT event dispatcher,
     * but the cost preview will not update).
     */
    public void keyReleased(java.awt.event.KeyEvent e) {
        String selection = (String) rewardsComboBox.getSelectedItem();
        cost = Integer.parseInt(amountText.getText());
        if (selection != null) {
            if (selection.equals(refreshCommand)) {
                cost = Integer.parseInt(client.getServerConfigs("FluToRefreshFactory"));
                costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("FluLongName"), cost, client.getServerConfigs(
                      "FluShortName")));
                dialog.repaint();
            } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                int total = cost * Integer.parseInt(client.getServerConfigs("Cbills_Per_Flu"));
                costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(true, true, total)));
            }
        }
    }

    /**
     * Handles all interactive controls in this dialog: the OK/Cancel buttons and the two combo boxes
     * (which use {@link #rewardCommand} and {@link #refreshCommand} as their action commands).
     * <ul>
     *     <li>{@link #okayCommand}: dispatches based on the selected top-level action - sends a
     *     {@code refreshFactory} chat command (parsing the planet/factory name back out of the selected combo
     *     entry's display text), a {@code useinfluence} chat command, or (if the selection is neither known
     *     action - i.e. "None Available") an in-game mail notifying the player that influence options are
     *     disabled server-side. The dialog is disposed afterwards in all cases.</li>
     *     <li>{@link #cancelCommand}: records the cancel button as the pane's value and disposes without
     *     sending anything.</li>
     *     <li>{@link #rewardCommand}: fired when the top-level action combo box changes selection; updates the
     *     cost preview and toggles which sub-controls ({@link #refreshComboBox}/{@link #refreshLabel} vs
     *     {@link #amountLabel}/{@link #amountText}) are visible via {@link #makeVisible}.</li>
     * </ul>
     */
    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case okayCommand -> {
                String selection = (String) rewardsComboBox.getSelectedItem();

                if (selection != null) {
                    if (selection.equals(refreshCommand)) {
                        if (refreshComboBox.getComponentCount() < 1) {
                            dialog.dispose();
                        }

                        String factoryInfo = (String) refreshComboBox.getSelectedItem();

                        if (factoryInfo != null) {
                            // factoryInfo has the form "<planetName>: <factoryName>(<ticks>)"; split it back apart.
                            String planet = factoryInfo.substring(0, factoryInfo.indexOf(":")).trim();
                            String factory = factoryInfo.substring(planet.length() + 2, factoryInfo.indexOf("("))
                                                   .trim();
                            String useFlu = "true";
                            client.sendChat(String.format("%sc refreshFactory#%s#%s#%s", IClient.CAMPAIGN_PREFIX, planet, factory, useFlu));
                        }
                    } else if (selection.equals(client.getServerConfigs("MoneyLongName"))) {
                        client.sendChat(String.format("%sc useinfluence#4#%s", IClient.CAMPAIGN_PREFIX, amountText.getText()));
                    } else {
                        // Only "None Available" (or an unrecognized entry) reaches here: both influence
                        // actions are disabled server-side, so just notify the player via in-game mail.
                        client.sendChat(String.format("%smail %sNo Influence Spent. Options are disabled on this server.", IClient.CAMPAIGN_PREFIX, client.getUsername()));
                    }
                }

                dialog.dispose();
            }
            case cancelCommand -> {
                pane.setValue(cancelButton);
                dialog.dispose();
            }
            case rewardCommand -> {
                String selection = (String) rewardsComboBox.getSelectedItem();

                if (selection != null) {
                    if (selection.equals(refreshCommand)) {
                        if (refreshComboBox.getItemCount() >= 1) {refreshComboBox.setSelectedIndex(0);}
                        cost = Integer.parseInt(client.getServerConfigs("FluToRefreshFactory"));
                        costLabel.setText(String.format("%s Required: %s %s", client.getServerConfigs("FluLongName"), cost, client.getServerConfigs(
                              "FluShortName")));
                        makeVisible(false, false, true);
                    } else if (selection.equalsIgnoreCase(client.getServerConfigs("MoneyLongName"))) {
                        amountText.setText("0");
                        cost = Integer.parseInt(amountText.getText());
                        int total = cost * Integer.parseInt(client.getServerConfigs("Cbills_Per_Flu"));
                        costLabel.setText(String.format("Result: Gain %s", client.moneyOrFluMessage(true, true, total)));
                        makeVisible(false, false, false);
                    } else {
                        makeVisible(true, false, false);
                    }
                }
            }
        }
    }

    /**
     * Toggles visibility of the refresh-factory controls and the amount-to-spend controls, based on which
     * top-level action is currently selected.
     * <p>
     * NOTE: {@code rePod} is always called with {@code false} at every call site in this class (a leftover from
     * this dialog being adapted from a similarly-structured "RewardPointsDialog" - see the commented-out
     * {@code fluToRepod} field above - which presumably had a "re-pod" action of its own); it is kept here for
     * parity with that sibling dialog even though it's currently dead in this class.
     *
     * @param visible whether the "amount to spend" controls should be shown when neither {@code rePod} nor
     *                {@code refresh} is set (note the inverted sense: passing {@code true} here actually hides
     *                the amount controls, since they are set visible via {@code !visible}).
     * @param rePod   always {@code false} in this class; would force-hide the amount controls if set.
     * @param refresh whether the "refresh factory" combo box/label should be shown.
     */
    private void makeVisible(boolean visible, boolean rePod, boolean refresh) {

        refreshComboBox.setVisible(refresh);
        refreshLabel.setVisible(refresh);

        if (rePod || refresh) {
            amountLabel.setVisible(false);
            amountText.setVisible(false);
        } else {
            amountLabel.setVisible(!visible);
            amountText.setVisible(!visible);
        }

    }

}//end RewardPointsDialog.java
