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

public final class TraitDialog implements ActionListener, KeyListener {
    private final static MMLogger LOGGER = MMLogger.create(TraitDialog.class);

    private final static String okayCommand = "Add";
    private final static String cancelCommand = "Close";
    private final static String removeCommand = "Remove";
    private final static String traitCommand = "Trait";
    private final static String factionCommand = "Faction";
    private final static String delimiter = "*";
    //store the client backlink for other things to use
    private final IClient client;
    private final JButton cancelButton = new JButton("Close");

    private final JTextField gunneryLaserText = new JTextField(3);
    private final JTextField gunneryBallisticText = new JTextField(3);
    private final JTextField gunneryMissileText = new JTextField(3);
    private final JTextField asTechText = new JTextField(3);
    private final JTextField tacticalGeniusText = new JTextField(3);
    private final JTextField weaponSpecialistText = new JTextField(3);
    private final JTextField meleeSpecialistText = new JTextField(3);
    private final JTextField dodgeManeuverText = new JTextField(3);
    private final JTextField ironManText = new JTextField(3);
    private final JTextField maneuveringAceText = new JTextField(3);
    private final JTextField NAGText = new JTextField(3);
    private final JTextField NAPText = new JTextField(3);
    private final JTextField painResistanceText = new JTextField(3);
    private final JTextField survivalistSkillText = new JTextField(3);
    private final JTextField enhancedInterfaceText = new JTextField(3);
    private final JTextField quickStudyText = new JTextField(3);
    private final JTextField giftedText = new JTextField(3);
    private final JTextField medtechText = new JTextField(3);

    private final JComboBox<String> factionComboBox;
    private final JComboBox<String> traitComboBox = new JComboBox<>();

    //STOCK DIALOG AND PANE
    private final JDialog dialog;
    private final JOptionPane pane;

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

    private void loadAllFiles() {
        client.loadServerTraitFiles();
    }

    public void keyTyped(KeyEvent keyEvent) {
    }

    public void keyPressed(KeyEvent keyEvent) {
    }

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

    private void loadFactionTraits(String faction) {
        File traitFile = new File(STR."\{client.getCacheDir()}/\{faction.toLowerCase()}traitnames.txt");
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
            LOGGER.error(ex, STR."Unable to load faction \{faction}");
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

    private void populateTraits(String faction, String trait) {
        File traitFile = new File(STR."\{client.getCacheDir()}/\{faction.toLowerCase()}traitnames.txt");

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
                        } else if (traitID == PilotSkill.AstechSkillID) {
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
                    client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c addtrait#\{result}");
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
                        client.sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c removetrait#\{faction}#\{trait.trim()}#CONFIRM");
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

    public String getResults(String faction, String trait) {
        String result = STR."\{faction}#\{trait}#";

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
            result += PilotSkill.AstechSkillID;
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
