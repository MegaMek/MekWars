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

package mekwars.common.gui.dialogs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.*;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.SpringLayoutHelper;

/**
 * Dialog for viewing (players) or authoring (GMs/admins) "Traits".
 * <p>
 * A Trait in MekWars is a named, per-faction bundle of chance modifiers applied to the
 * pilot skills defined in {@link PilotSkill} (Gunnery specialties, Iron Man, Pain Resistance,
 * Natural Aptitude, Edge-related skills, AsTech, MedTech, etc.). Each modifier changes the odds
 * that a newly generated pilot for that faction will roll the corresponding skill. Traits are
 * persisted server-side as flat, "*"-delimited text files (one per faction, named
 * {@code <faction>traitnames.txt}) and are cached locally in the client's cache directory.
 * <p>
 * When opened in "player" mode ({@code player == true}) the dialog is read-only: the user can
 * browse factions/traits and see the configured modifiers, but cannot add, edit or remove
 * entries, and the window is titled "Trait Viewer". When opened in GM/admin mode
 * ({@code player == false}) the trait name field and all modifier fields become editable, the
 * "Add" and "Remove" buttons are shown, and edits are sent to the server as chat commands
 * (see {@link #getResults(String, String)} and {@link #actionPerformed(ActionEvent)}).
 */
public final class TraitDialog implements ActionListener, KeyListener {
    private final static MMLogger LOGGER = MMLogger.create(TraitDialog.class);

    /** Action command for the "Add" button: submit the currently edited trait to the server. */
    private final static String okayCommand = "Add";
    /** Action command for the "Close"/"Exit" button: dismiss the dialog without further changes. */
    private final static String cancelCommand = "Close";
    /** Action command for the "Remove" button: delete the currently selected trait (with confirmation). */
    private final static String removeCommand = "Remove";
    /** Action command used by the trait combo box. */
    private final static String traitCommand = "Trait";
    /** Action command used by the faction combo box. */
    private final static String factionCommand = "Faction";
    /** Field/token separator used both in the on-disk trait files and in the server chat protocol strings built by {@link #getResults(String, String)}. */
    private final static String delimiter = "*";
    //store the client backlink for other things to use
    private final IClient client;
    private final JButton cancelButton = new JButton("Close");

    // One 3-column numeric text field per PilotSkill this dialog can modify. Each field holds the
    // chance modifier for that skill (as a plain integer; "0" means "no effect"/unset) and is
    // editable only when the dialog is opened in GM/admin mode. See the tooltip set on each field
    // below for the human-readable meaning of its abbreviation.
    /** Modifier for Gunnery/Laser specialty (see {@link PilotSkill#GunneryLaserSkillID}). */
    private final JTextField gunneryLaserText = new JTextField(3);
    /** Modifier for Gunnery/Ballistic specialty (see {@link PilotSkill#GunneryBallisticSkillID}). */
    private final JTextField gunneryBallisticText = new JTextField(3);
    /** Modifier for Gunnery/Missile specialty (see {@link PilotSkill#GunneryMissileSkillID}). */
    private final JTextField gunneryMissileText = new JTextField(3);
    /** Modifier for the AsTech skill (see {@link PilotSkill#AsTechSkillID}). */
    private final JTextField asTechText = new JTextField(3);
    /** Modifier for Tactical Genius (see {@link PilotSkill#TacticalGeniusSkillID}). */
    private final JTextField tacticalGeniusText = new JTextField(3);
    /** Modifier for Weapon Specialist (see {@link PilotSkill#WeaponSpecialistSkillID}). */
    private final JTextField weaponSpecialistText = new JTextField(3);
    /** Modifier for Melee Specialist (see {@link PilotSkill#MeleeSpecialistSkillID}). */
    private final JTextField meleeSpecialistText = new JTextField(3);
    /** Modifier for Dodge Maneuver (see {@link PilotSkill#DodgeManeuverSkillID}). */
    private final JTextField dodgeManeuverText = new JTextField(3);
    /** Modifier for Iron Man (see {@link PilotSkill#IronManSkillID}). */
    private final JTextField ironManText = new JTextField(3);
    /** Modifier for Maneuvering Ace (see {@link PilotSkill#ManeuveringAceSkillID}). */
    private final JTextField maneuveringAceText = new JTextField(3);
    /** Modifier for Natural Aptitude: Gunnery (see {@link PilotSkill#NaturalAptitudeGunnerySkillID}). */
    private final JTextField NAGText = new JTextField(3);
    /** Modifier for Natural Aptitude: Piloting (see {@link PilotSkill#NaturalAptitudePilotingSkillID}). */
    private final JTextField NAPText = new JTextField(3);
    /** Modifier for Pain Resistance (see {@link PilotSkill#PainResistanceSkillID}). */
    private final JTextField painResistanceText = new JTextField(3);
    /** Modifier for Survivalist (see {@link PilotSkill#SurvivalistSkillID}). */
    private final JTextField survivalistSkillText = new JTextField(3);
    /** Modifier for Enhanced Interface (see {@link PilotSkill#EnhancedInterfaceID}). */
    private final JTextField enhancedInterfaceText = new JTextField(3);
    /** Modifier for Quick Study (see {@link PilotSkill#QuickStudyID}). */
    private final JTextField quickStudyText = new JTextField(3);
    /** Modifier for Gifted (see {@link PilotSkill#GiftedID}). */
    private final JTextField giftedText = new JTextField(3);
    /** Modifier for MedTech (see {@link PilotSkill#MedTechID}). */
    private final JTextField medtechText = new JTextField(3);

    /** Faction ("House") whose trait file is currently being browsed/edited; always includes a synthetic "Common" entry. */
    private final JComboBox<String> factionComboBox;
    /** Trait name selector; editable (free text entry allowed) only in GM/admin mode so a brand-new trait name can be typed in. */
    private final JComboBox<String> traitComboBox = new JComboBox<>();

    //STOCK DIALOG AND PANE
    /** The modal Swing dialog window hosting {@link #pane}. */
    private final JDialog dialog;
    /** The JOptionPane that supplies the Add/Remove/Close buttons and wraps the trait editor panel. */
    private final JOptionPane pane;

    /**
     * Builds and immediately displays the modal Trait dialog. Construction blocks (via
     * {@code dialog.setVisible(true)} at the end of this constructor) until the user closes the
     * dialog.
     *
     * @param client the client back-link, used to look up factions, load trait files from the
     *               server cache, and send chat commands for any add/remove edits
     * @param player {@code true} to open a read-only viewer (all fields disabled, no Add/Remove
     *               buttons, trait name not editable); {@code false} to open the full GM/admin
     *               editor where trait modifiers can be created, changed and removed
     */
    public TraitDialog(IClient client, boolean player) {

        //save the client
        this.client = client;

        //COMBO BOXES
        TreeSet<String> names = new TreeSet<>();
        names.add("Common"); //start with the common faction

        for (House house : this.client.getData().getAllHouses()) {
            names.add(house.getName());
        }

        factionComboBox = new JComboBox<>();
        names.forEach(traitComboBox::addItem);
        traitComboBox.setEditable(!player);

        //stored values.

        //Set the tooltips and actions for dialogue buttons
        //
        JButton okayButton = new JButton("Add");
        okayButton.setActionCommand(okayCommand);
        cancelButton.setActionCommand(cancelCommand);
        factionComboBox.setActionCommand(factionCommand);
        traitComboBox.setActionCommand(traitCommand);

        okayButton.addActionListener(this);
        cancelButton.addActionListener(this);
        JButton removeButton = new JButton("Remove");
        removeButton.addActionListener(this);
        okayButton.setToolTipText("Save Trait");
        String windowName = "Trait Editor";

        if (player) {
            cancelButton.setToolTipText("Exit");
            windowName = "Trait Viewer";
        } else {
            cancelButton.setToolTipText("Exit without saving changes");
        }

        removeButton.setToolTipText("Delete Trait");
        traitComboBox.addActionListener(this);
        factionComboBox.addActionListener(this);

        okayButton.setVisible(!player);
        removeButton.setVisible(!player);

        //CREATE THE PANELS
        JPanel traitsPanel = new JPanel();//player name, etc

        /*
         * Format the Reward Points panel. Spring layout.
         */
        traitsPanel.setLayout(new BoxLayout(traitsPanel, BoxLayout.Y_AXIS));

        JPanel skillPanel = new JPanel(new SpringLayout());
        JPanel comboPanel = new JPanel(new SpringLayout());

        //TEXT FIELDS
        //tab names
        JLabel factionLabel = new JLabel("faction:", SwingConstants.TRAILING);
        comboPanel.add(factionLabel);
        factionComboBox.setToolTipText("Select a faction");
        comboPanel.add(factionComboBox);

        JLabel traitLabel = new JLabel("Trait:", SwingConstants.TRAILING);
        comboPanel.add(traitLabel);

        if (player) {traitComboBox.setToolTipText("Select a trait.");} else {
            traitComboBox.setToolTipText("Select a trait or enter a new one.");
        }

        comboPanel.add(traitComboBox);

        JLabel asTechLabel = new JLabel("AT:", SwingConstants.TRAILING);
        skillPanel.add(asTechLabel);
        asTechText.setToolTipText("<html>AsTech<br>Modifies the chance for a pilot to receive this skill</html>");
        asTechText.setEditable(!player);
        skillPanel.add(asTechText);

        JLabel dodgeManeuverLabel = new JLabel("DM:", SwingConstants.TRAILING);
        skillPanel.add(dodgeManeuverLabel);
        dodgeManeuverText.setToolTipText(
              "<html>Dodge Maneuver<br>Modifies the chance for a pilot to receive this skill</html>");
        dodgeManeuverText.setEditable(!player);
        skillPanel.add(dodgeManeuverText);

        JLabel enhancedInterfaceLabel = new JLabel("EI:", SwingConstants.TRAILING);
        skillPanel.add(enhancedInterfaceLabel);
        enhancedInterfaceText.setToolTipText(
              "<html>Enhanced Interface<br>Modifies the chance for a pilot to receive this skill</html>");
        enhancedInterfaceText.setEditable(!player);
        skillPanel.add(enhancedInterfaceText);

        JLabel giftedLabel = new JLabel("GT:", SwingConstants.TRAILING);
        skillPanel.add(giftedLabel);
        giftedText.setToolTipText("<html>Gifted<br>Modifies the chance for a pilot to receive this skill</html>");
        giftedText.setEditable(!player);
        skillPanel.add(giftedText);

        JLabel gunneryLaserLabel = new JLabel("G/L:", SwingConstants.TRAILING);
        skillPanel.add(gunneryLaserLabel);
        gunneryLaserText.setToolTipText(
              "<html>Gunnery Laser<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryLaserText.setEditable(!player);
        skillPanel.add(gunneryLaserText);

        JLabel gunneryBallisticLabel = new JLabel("G/B:", SwingConstants.TRAILING);
        skillPanel.add(gunneryBallisticLabel);
        gunneryBallisticText.setToolTipText(
              "<html>Gunnery Ballistic<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryBallisticText.setEditable(!player);
        skillPanel.add(gunneryBallisticText);

        JLabel gunneryMissileLabel = new JLabel("G/M:", SwingConstants.TRAILING);
        skillPanel.add(gunneryMissileLabel);
        gunneryMissileText.setToolTipText(
              "<html>Gunnery Missile<br>Modifies the chance for a pilot to receive this skill</html>");
        gunneryMissileText.setEditable(!player);
        skillPanel.add(gunneryMissileText);

        JLabel ironManLabel = new JLabel("IM:", SwingConstants.TRAILING);
        skillPanel.add(ironManLabel);
        ironManText.setToolTipText("<html>Iron Man<br>Modifies the chance for a pilot to receive this skill</html>");
        ironManText.setEditable(!player);
        skillPanel.add(ironManText);

        JLabel maneuveringAceLabel = new JLabel("MA:", SwingConstants.TRAILING);
        skillPanel.add(maneuveringAceLabel);
        maneuveringAceText.setToolTipText(
              "<html>Maneuvering Ace<br>Modifies the chance for a pilot to receive this skill</html>");
        maneuveringAceText.setEditable(!player);
        skillPanel.add(maneuveringAceText);

        JLabel medtechLabel = new JLabel("MT:", SwingConstants.TRAILING);
        skillPanel.add(medtechLabel);
        medtechText.setToolTipText("<html>Medtech<br>Modifies the chance for a pilot to receive this skill</html>");
        medtechText.setEditable(!player);
        skillPanel.add(medtechText);

        JLabel meleeSpecialistLabel = new JLabel("MS:", SwingConstants.TRAILING);
        skillPanel.add(meleeSpecialistLabel);
        meleeSpecialistText.setToolTipText(
              "<html>Melee Specialist<br>Modifies the chance for a pilot to receive this skill</html>");
        meleeSpecialistText.setEditable(!player);
        skillPanel.add(meleeSpecialistText);

        JLabel NAGLabel = new JLabel("NAG:", SwingConstants.TRAILING);
        skillPanel.add(NAGLabel);
        NAGText.setToolTipText(
              "<html>Natural Aptitude: Gunnery<br>Modifies the chance for a pilot to receive this skill</html>");
        NAGText.setEditable(!player);
        skillPanel.add(NAGText);

        JLabel NAPLabel = new JLabel("NAP:", SwingConstants.TRAILING);
        skillPanel.add(NAPLabel);
        NAPText.setToolTipText(
              "<html>Natural Aptitude: Piloting<br>Modifies the chance for a pilot to receive this skill</html>");
        NAPText.setEditable(!player);
        skillPanel.add(NAPText);

        JLabel painResistanceLabel = new JLabel("PR:", SwingConstants.TRAILING);
        skillPanel.add(painResistanceLabel);
        painResistanceText.setToolTipText(
              "<html>Pain Resistance<br>Modifies the chance for a pilot to receive this skill</html>");
        painResistanceText.setEditable(!player);
        skillPanel.add(painResistanceText);

        JLabel quickStudyLabel = new JLabel("QS:", SwingConstants.TRAILING);
        skillPanel.add(quickStudyLabel);
        quickStudyText.setToolTipText("<html>Quick Study</html>");
        quickStudyText.setEditable(!player);
        skillPanel.add(quickStudyText);

        JLabel survivalistSkillLabel = new JLabel("SV:", SwingConstants.TRAILING);
        skillPanel.add(survivalistSkillLabel);
        survivalistSkillText.setToolTipText("<html>Survivalist</html>");
        survivalistSkillText.setEditable(!player);
        skillPanel.add(survivalistSkillText);

        JLabel tacticalGeniusLabel = new JLabel("TG:", SwingConstants.TRAILING);
        skillPanel.add(tacticalGeniusLabel);
        tacticalGeniusText.setToolTipText("<html>Tactical Genius</html>");
        tacticalGeniusText.setEditable(!player);
        skillPanel.add(tacticalGeniusText);

        JLabel weaponSpecialistLabel = new JLabel("WS:", SwingConstants.TRAILING);
        skillPanel.add(weaponSpecialistLabel);
        weaponSpecialistText.setToolTipText("<html>Weapon Specialist</html>");
        weaponSpecialistText.setEditable(!player);
        skillPanel.add(weaponSpecialistText);

        //run the spring layout
        SpringLayoutHelper.setupSpringGrid(comboPanel, 2);
        SpringLayoutHelper.setupSpringGrid(skillPanel, 8);

        traitsPanel.add(comboPanel);
        traitsPanel.add(skillPanel);

        JPanel mainPanel = new JPanel();

        // Set the user's options
        Object[] options = { okayButton, removeButton, cancelButton };

        // Create the pane containing the buttons
        pane = new JOptionPane(traitsPanel,
              JOptionPane.PLAIN_MESSAGE,
              JOptionPane.DEFAULT_OPTION,
              null,
              options,
              null);

        // Create the main dialog and set the default button
        dialog = pane.createDialog(mainPanel, windowName);
        dialog.getRootPane().setDefaultButton(cancelButton);


        loadAllFiles();

        factionComboBox.setSelectedIndex(0);
        dialog.setLocationRelativeTo(this.client.getMainFrame());
        //Show the dialog and get the user's input
        dialog.setModal(true);
        dialog.pack();
        dialog.setVisible(true);

        if (pane.getValue() != okayButton) {
            dialog.dispose();
        }
    }

    /** Tells the client to (re)load the server's trait files into the local cache before this dialog reads them. */
    private void loadAllFiles() {
        client.loadServerTraitFiles();
    }

    /** No-op; required by {@link KeyListener} but this dialog only reacts to key-release events. */
    public void keyTyped(KeyEvent keyEvent) {
    }

    /** No-op; required by {@link KeyListener} but this dialog only reacts to key-release events. */
    public void keyPressed(KeyEvent keyEvent) {
    }

    /**
     * Reacts to keyboard-driven navigation of the faction/trait combo boxes (e.g. arrow keys).
     * Note this dialog uses {@link KeyListener#keyReleased(KeyEvent)} rather than the more usual
     * {@code ItemListener}/{@code ActionListener} combo-box change notification, so changing a
     * selection purely via mouse click may not reliably trigger this refresh in all look-and-feels.
     * <p>
     * If the faction combo box was the source, reloads that faction's trait name list
     * ({@link #loadFactionTraits(String)}); otherwise (the trait combo box changed) reloads the
     * modifier fields for the newly selected trait ({@link #populateTraits(String, String)}).
     */
    public void keyReleased(KeyEvent keyEvent) {

        String faction = (String) factionComboBox.getSelectedItem();
        String trait = ((String) traitComboBox.getSelectedItem());

        if (faction != null && trait != null) {
            if (keyEvent.getComponent().equals(factionComboBox)) {
                loadFactionTraits(faction);
            } else {
                populateTraits(faction, trait.trim());
            }
        }
    }

    /**
     * Reloads the trait-name combo box for the given faction by reading
     * {@code <cacheDir>/<faction>traitnames.txt} (one trait per line, first "*"-delimited token is
     * the trait name) from the client's cache directory. Any {@link Exception} while reading is
     * logged and swallowed, leaving the combo box empty. Selects the first entry (if any) once
     * loaded.
     * <p>
     * Note: if the file cannot be opened at all, {@code dis} stays {@code null} and the
     * {@code finally} block's {@code dis.close()} will throw a {@link NullPointerException}
     * that is not caught here (only {@link java.io.IOException} is handled) — a pre-existing
     * quirk in this error path.
     *
     * @param faction the faction name whose trait file should be loaded (case-insensitive; the
     *                filename is lower-cased)
     */
    private void loadFactionTraits(String faction) {
        File traitFile = new File(String.format("%s/%straitnames.txt", client.getCacheDir(), faction.toLowerCase()));
        TreeSet<String> names = new TreeSet<>();

        if (traitComboBox.getItemCount() > 0) {
            traitComboBox.removeAllItems();
        }

        BufferedReader dis = null;

        try {
            FileInputStream fis = new FileInputStream(traitFile);
            dis = new BufferedReader(new InputStreamReader(fis));

            while (dis.ready()) {
                StringTokenizer traitName = new StringTokenizer(dis.readLine(), delimiter);
                names.add(traitName.nextToken());
            }

            names.forEach(traitComboBox::addItem);
        } catch (Exception ex) {
            LOGGER.error(ex, String.format("Unable to load faction %s", faction));
        } finally {
            try {
                dis.close();
            } catch (java.io.IOException e) {
                LOGGER.error(e);
            }
        }
        if (traitComboBox.getItemCount() > 0) {
            traitComboBox.setSelectedIndex(0);
        }

        traitComboBox.revalidate();
    }

    /**
     * Resets every skill-modifier text field to "0", then re-reads the faction's trait file
     * looking for the line whose name matches {@code trait} (case-insensitive) and, for each
     * {@code <skillId>*<modifier>} pair on that line, writes the modifier into the matching
     * field (matched by comparing against the {@link PilotSkill} ID constants). Any parsing
     * exception is logged and swallowed, leaving fields at whatever state they reached.
     *
     * @param faction the faction whose trait file to read
     * @param trait   the trait name to look up within that file
     */
    private void populateTraits(String faction, String trait) {
        File traitFile = new File(String.format("%s/%straitnames.txt", client.getCacheDir(), faction.toLowerCase()));

        gunneryLaserText.setText("0");
        gunneryBallisticText.setText("0");
        gunneryMissileText.setText("0");
        asTechText.setText("0");
        tacticalGeniusText.setText("0");
        weaponSpecialistText.setText("0");
        meleeSpecialistText.setText("0");
        dodgeManeuverText.setText("0");
        ironManText.setText("0");
        maneuveringAceText.setText("0");
        NAGText.setText("0");
        NAPText.setText("0");
        painResistanceText.setText("0");
        survivalistSkillText.setText("0");
        enhancedInterfaceText.setText("0");
        giftedText.setText("0");
        quickStudyText.setText("0");
        medtechText.setText("0");
        BufferedReader dis = null;

        try {
            FileInputStream fis = new FileInputStream(traitFile);
            dis = new BufferedReader(new InputStreamReader(fis));

            while (dis.ready()) {
                StringTokenizer traitNames = new StringTokenizer(dis.readLine(), delimiter);
                String traitName = traitNames.nextToken();

                if (traitName.equalsIgnoreCase(trait)) {
                    while (traitNames.hasMoreTokens()) {
                        int traitID = Integer.parseInt(traitNames.nextToken());
                        String traitMod = traitNames.nextToken();

                        if (traitID == PilotSkill.GunneryBallisticSkillID) {
                            gunneryBallisticText.setText(traitMod);
                        } else if (traitID == PilotSkill.GunneryLaserSkillID) {
                            gunneryLaserText.setText(traitMod);
                        } else if (traitID == PilotSkill.GunneryMissileSkillID) {
                            gunneryMissileText.setText(traitMod);
                        } else if (traitID == PilotSkill.AsTechSkillID) {
                            asTechText.setText(traitMod);
                        } else if (traitID == PilotSkill.DodgeManeuverSkillID) {
                            dodgeManeuverText.setText(traitMod);
                        } else if (traitID == PilotSkill.IronManSkillID) {
                            ironManText.setText(traitMod);
                        } else if (traitID == PilotSkill.ManeuveringAceSkillID) {
                            maneuveringAceText.setText(traitMod);
                        } else if (traitID == PilotSkill.MeleeSpecialistSkillID) {
                            meleeSpecialistText.setText(traitMod);
                        } else if (traitID == PilotSkill.NaturalAptitudeGunnerySkillID) {
                            NAGText.setText(traitMod);
                        } else if (traitID == PilotSkill.NaturalAptitudePilotingSkillID) {
                            NAPText.setText(traitMod);
                        } else if (traitID == PilotSkill.PainResistanceSkillID) {
                            painResistanceText.setText(traitMod);
                        } else if (traitID == PilotSkill.SurvivalistSkillID) {
                            survivalistSkillText.setText(traitMod);
                        } else if (traitID == PilotSkill.TacticalGeniusSkillID) {
                            tacticalGeniusText.setText(traitMod);
                        } else if (traitID == PilotSkill.WeaponSpecialistSkillID) {
                            weaponSpecialistText.setText(traitMod);
                        } else if (traitID == PilotSkill.EnhancedInterfaceID) {
                            enhancedInterfaceText.setText(traitMod);
                        } else if (traitID == PilotSkill.GiftedID) {
                            giftedText.setText(traitMod);
                        } else if (traitID == PilotSkill.QuickStudyID) {
                            quickStudyText.setText(traitMod);
                        } else if (traitID == PilotSkill.MedTechID) {
                            medtechText.setText(traitMod);
                        }
                    }
                }

            }
        } catch (Exception ex) {
            LOGGER.error(ex, "populate Traits error");
        } finally {
            try {
                dis.close();
            } catch (java.io.IOException e) {
                LOGGER.error(e);
            }
        }
    }

    /**
     * Central button/combo-box handler for this dialog (GM/admin mode only performs real edits;
     * the buttons themselves are hidden in player/view-only mode). Dispatches on the Swing action
     * command string:
     * <ul>
     *   <li>{@code okayCommand} ("Add") — validates a trait name was chosen/typed, serializes the
     *       current modifier fields via {@link #getResults(String, String)} and sends an
     *       {@code addtrait} chat command to the server, then reloads the trait files/list.</li>
     *   <li>{@code cancelCommand} ("Close") — records the cancel button as the pane's value and
     *       disposes the dialog.</li>
     *   <li>{@code removeCommand} ("Remove") — asks for confirmation, then sends a
     *       {@code removetrait} chat command for the selected faction/trait and reloads.</li>
     *   <li>{@code factionCommand} — a new faction was chosen; reloads that faction's trait list.</li>
     *   <li>{@code traitCommand} — a new trait was chosen; repopulates the modifier fields for it.</li>
     * </ul>
     */
    public void actionPerformed(ActionEvent actionEvent) {
        String command = actionEvent.getActionCommand();

        switch (command) {
            case okayCommand -> {
                String faction = (String) factionComboBox.getSelectedItem();
                String trait = ((String) traitComboBox.getSelectedItem());

                if (faction != null && trait != null) {
                    if (trait.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(null, "You did not enter a trait name!");
                        return;
                    }

                    String result = getResults(faction, trait.trim());
                    client.sendChat(String.format("%sc addtrait#%s", IClient.CAMPAIGN_PREFIX, result));
                    loadAllFiles();
                    loadFactionTraits(faction);
                }
            }
            case cancelCommand -> {
                pane.setValue(cancelButton);
                dialog.dispose();
            }
            case removeCommand -> {
                String faction = (String) factionComboBox.getSelectedItem();
                String trait = ((String) traitComboBox.getSelectedItem());

                if (faction != null && trait != null) {
                    if (trait.isEmpty()) {
                        JOptionPane.showMessageDialog(null, "You have to select a trait before you can remove it!");
                        return;
                    }

                    int choice = JOptionPane.showConfirmDialog(null,
                          "Are you sure you want to remove this trait?",
                          "Remove it?",
                          JOptionPane.YES_NO_OPTION);
                    if (choice == JOptionPane.OK_OPTION) {
                        client.sendChat(String.format("%sc removetrait#%s#%s#CONFIRM", IClient.CAMPAIGN_PREFIX, faction, trait.trim()));
                        loadAllFiles();
                        loadFactionTraits(faction);
                    }
                }
            }
            case factionCommand -> {
                String selection = (String) factionComboBox.getSelectedItem();
                if (selection != null) {
                    loadFactionTraits(selection);
                }
            }
            case traitCommand -> {
                String faction = (String) factionComboBox.getSelectedItem();

                if (traitComboBox.getSelectedItem() == null) {
                    return;
                }

                String trait = ((String) traitComboBox.getSelectedItem());

                if (faction != null && trait != null) {
                    populateTraits(faction, trait.trim());
                }
            }
        }
    }

    /**
     * Serializes the currently entered skill modifiers into the delimited protocol string sent
     * to the server for an add/edit trait request. The format is
     * {@code <faction>#<trait>#(<skillId>*<modifier>*)*#CONFIRM} — each skill whose text field
     * currently parses to a non-zero integer (via {@link MathUtility#parseInt}) contributes one
     * {@code <skillId>*<modifier>*} triple; skills left at "0" (the default) are omitted entirely,
     * so a modifier of 0 cannot be explicitly saved/distinguished from "not set".
     *
     * @param faction the faction the trait belongs to
     * @param trait   the trait's name
     * @return the fully built, "#CONFIRM"-terminated command payload describing this trait
     */
    public String getResults(String faction, String trait) {
        String result = String.format("%s#%s#", faction, trait);

        if (MathUtility.parseInt(gunneryBallisticText.getText(), 0) != 0) {
            result += PilotSkill.GunneryBallisticSkillID;
            result += delimiter;
            result += gunneryBallisticText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(gunneryLaserText.getText(), 0) != 0) {
            result += PilotSkill.GunneryLaserSkillID;
            result += delimiter;
            result += gunneryLaserText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(gunneryMissileText.getText(), 0) != 0) {
            result += PilotSkill.GunneryMissileSkillID;
            result += delimiter;
            result += gunneryMissileText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(asTechText.getText(), 0) != 0) {
            result += PilotSkill.AsTechSkillID;
            result += delimiter;
            result += asTechText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(dodgeManeuverText.getText(), 0) != 0) {
            result += PilotSkill.DodgeManeuverSkillID;
            result += delimiter;
            result += dodgeManeuverText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(ironManText.getText(), 0) != 0) {
            result += PilotSkill.IronManSkillID;
            result += delimiter;
            result += ironManText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(maneuveringAceText.getText(), 0) != 0) {
            result += PilotSkill.ManeuveringAceSkillID;
            result += delimiter;
            result += maneuveringAceText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(meleeSpecialistText.getText(), 0) != 0) {
            result += PilotSkill.MeleeSpecialistSkillID;
            result += delimiter;
            result += meleeSpecialistText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(NAGText.getText(), 0) != 0) {
            result += PilotSkill.NaturalAptitudeGunnerySkillID;
            result += delimiter;
            result += NAGText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(NAPText.getText(), 0) != 0) {
            result += PilotSkill.NaturalAptitudePilotingSkillID;
            result += delimiter;
            result += NAPText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(painResistanceText.getText(), 0) != 0) {
            result += PilotSkill.PainResistanceSkillID;
            result += delimiter;
            result += painResistanceText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(survivalistSkillText.getText(), 0) != 0) {
            result += PilotSkill.SurvivalistSkillID;
            result += delimiter;
            result += survivalistSkillText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(tacticalGeniusText.getText(), 0) != 0) {
            result += PilotSkill.TacticalGeniusSkillID;
            result += delimiter;
            result += tacticalGeniusText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(weaponSpecialistText.getText(), 0) != 0) {
            result += PilotSkill.WeaponSpecialistSkillID;
            result += delimiter;
            result += weaponSpecialistText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(enhancedInterfaceText.getText(), 0) != 0) {
            result += PilotSkill.EnhancedInterfaceID;
            result += delimiter;
            result += enhancedInterfaceText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(giftedText.getText(), 0) != 0) {
            result += PilotSkill.GiftedID;
            result += delimiter;
            result += giftedText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(quickStudyText.getText(), 0) != 0) {
            result += PilotSkill.QuickStudyID;
            result += delimiter;
            result += quickStudyText.getText();
            result += delimiter;
        }
        if (MathUtility.parseInt(medtechText.getText(), 0) != 0) {
            result += PilotSkill.MedTechID;
            result += delimiter;
            result += medtechText.getText();
            result += delimiter;
        }


        result += "#CONFIRM";
        return result;
    }
}//end TraitDialog.java
