/*
 * MekWars - Copyright (C) 2008
 *
 * Original author - Torren (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package mekwars.common.gui.dialogs;

import java.io.Serial;

import mekwars.common.Unit;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.campaign.pilot.skills.PilotSkill;
import mekwars.common.util.SpringLayoutHelper;

public class PromotePilotDialog extends javax.swing.JFrame
      implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -8988175448434842033L;
    // store the client backlink for other things to use
    private final IClient client;
    private final CUnit playerUnit;
    private Pilot pilot = null;
    private final boolean demoting;

    private final static String okayCommand = "Ok";
    private final static String cancelCommand = "Close";

    private final javax.swing.JPanel MasterPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
    private final javax.swing.JTextField currentExp = new javax.swing.JTextField();
    private final javax.swing.JTextField expCost = new javax.swing.JTextField();

    public PromotePilotDialog(IClient client, int unitID, boolean demoting) {

        // save the client
        this.client = client;
        playerUnit = client.getPlayer().getUnit(unitID);
        this.demoting = demoting;

        String windowName = "Pilot Promotion Dialog";

        addKeyListener(this);

        // Set the tooltips and actions for dialouge buttons
        // BUTTONS
        javax.swing.JButton okayButton = new javax.swing.JButton("Buy");
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);

        if (demoting) {
            okayButton.setText("Sell");
            okayButton.setToolTipText("Sell Pilot Skill.");
            okayButton.setMnemonic('S');
        } else {
            okayButton.setToolTipText("Buy Pilot Skill.");
            okayButton.setMnemonic('B');
        }

        javax.swing.JButton cancelButton = new javax.swing.JButton("Close");
        cancelButton.setActionCommand(cancelCommand);
        cancelButton.addActionListener(this);
        cancelButton.setToolTipText("Close dialog");
        cancelButton.setDefaultCapable(true);

        loadPanel();

        // CREATE THE PANELS
        SpringLayoutHelper.setupSpringGrid(MasterPanel, 4);

        // Set the user's options
        Object[] options = { okayButton, cancelButton };

        // Create the pane containing the buttons
        // STOCK DIALOUG AND PANE
        // private JDialog dialog;
        javax.swing.JOptionPane pane = new javax.swing.JOptionPane(MasterPanel,
              javax.swing.JOptionPane.PLAIN_MESSAGE,
              javax.swing.JOptionPane.OK_CANCEL_OPTION,
              null,
              options,
              null);

        setTitle(windowName);

        javax.swing.JPanel contentPane = (javax.swing.JPanel) getContentPane();
        contentPane.setLayout(new java.awt.BorderLayout());
        contentPane.add(pane, java.awt.BorderLayout.CENTER);
        setResizable(true);
        this.setSize(new java.awt.Dimension(369, 287));
        setExtendedState(java.awt.Frame.NORMAL);
        addKeyListener(this);
        contentPane.addKeyListener(this);
        pane.addKeyListener(this);
        MasterPanel.addKeyListener(this);
        cancelButton.addKeyListener(this);

        pane.getRootPane().setDefaultButton(cancelButton);
        addKeyListener(this);

        this.repaint();
        setLocationRelativeTo(this.client.getMainFrame());

        pack();
        setVisible(true);
    }

    public void actionPerformed(java.awt.event.ActionEvent e) {
        String command = e.getActionCommand();

        if (command.equals(okayCommand)) {
            sendPromoteCommands();
            super.dispose();
        } else if (command.equals(cancelCommand)) {
            super.dispose();
        } else if (e.getSource() instanceof javax.swing.JCheckBox) {
            calculateExpCost();
        }

    }

    private void loadPanel() {

        pilot = playerUnit.getPilot();

        java.awt.Dimension dim = new java.awt.Dimension(30, 10);

        MasterPanel.add(new javax.swing.JLabel("Current Exp", javax.swing.SwingConstants.TRAILING));
        currentExp.setEditable(false);
        currentExp.setText(Integer.toString(pilot.getExperience()));
        currentExp.setMinimumSize(dim);
        currentExp.setPreferredSize(dim);
        currentExp.setMaximumSize(dim);
        MasterPanel.add(currentExp);

        MasterPanel.add(new javax.swing.JLabel("Exp Cost", javax.swing.SwingConstants.TRAILING));
        expCost.setEditable(false);
        expCost.setMinimumSize(dim);
        expCost.setPreferredSize(dim);
        expCost.setMaximumSize(dim);
        MasterPanel.add(expCost);

        javax.swing.JCheckBox masterCB;
        if (pilot.getGunnery() > Integer.parseInt(client.getServerConfigs("BestGunnerySkill")) && !demoting) {
            masterCB = new javax.swing.JCheckBox(STR."Gunnery \{pilot.getGunnery() - 1}");
            masterCB.setName("gunnery");
            masterCB.addActionListener(this);
            MasterPanel.add(masterCB);
        }

        if (pilot.getPiloting() > Integer.parseInt(client.getServerConfigs("BestPilotingSkill")) && !demoting) {
            masterCB = new javax.swing.JCheckBox(STR."Piloting \{pilot.getPiloting() - 1}");
            masterCB.setName("piloting");
            masterCB.addActionListener(this);
            MasterPanel.add(masterCB);
        }

        int maxSkills = Integer.parseInt(client.getServerConfigs("MaxPilotUpgrades"));
        if (maxSkills > -1 && pilot.getSkills().size() >= maxSkills && !demoting) {
            return;
        }

        if (demoting) {
            if (Integer.parseInt(client.getServerConfigs(STR."chanceforATfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      (pilot.getSkills().has(PilotSkill.AstechSkillID))) {
                masterCB = new javax.swing.JCheckBox("Astech");
                masterCB.setName(STR."chanceforATfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(STR."chanceforDMfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.DodgeManeuverSkillID)) {
                masterCB = new javax.swing.JCheckBox("Dodge Maneuver");
                masterCB.setName(STR."chanceforDMfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(STR."chanceforMSfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.MeleeSpecialistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Melee Specialist");
                masterCB.setName(STR."chanceforMSfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(STR."chanceforPRfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.PainResistanceSkillID)) {
                masterCB = new javax.swing.JCheckBox("Pain Resistance");
                masterCB.setName(STR."chanceforPRfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(STR."chanceforSVfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.SurvivalistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Survivalist");
                masterCB.setName(STR."chanceforSVfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(STR."chanceforIMfor\{Unit.getTypeClassDesc(playerUnit.getType())}")) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.IronManSkillID)) {
                masterCB = new javax.swing.JCheckBox("Iron Man");
                masterCB.setName(STR."chanceforIMfor\{Unit.getTypeClassDesc(playerUnit.getType())}");
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforEDfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      (pilot.getSkills().has(PilotSkill.EdgeSkillID))) {
                masterCB = new javax.swing.JCheckBox("Edge");
                masterCB.setName("chanceforEDfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforMAfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.ManeuveringAceSkillID)) {
                masterCB = new javax.swing.JCheckBox("Maneuvering Ace");
                masterCB.setName("chanceforMAfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforNAPfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
                masterCB = new javax.swing.JCheckBox("Natural Aptitude Piloting");
                masterCB.setName("chanceforNAPfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforNAGfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
                masterCB = new javax.swing.JCheckBox("Natural Aptitude Gunnery");
                masterCB.setName("chanceforNAGfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforWSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.WeaponSpecialistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Weapon Specialist");
                masterCB.setName("chanceforWSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforTGfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.TacticalGeniusSkillID)) {
                masterCB = new javax.swing.JCheckBox("Tactical Genius");
                masterCB.setName("chanceforTGfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGMfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.GunneryMissileSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Missile");
                masterCB.addActionListener(this);
                masterCB.setName("chanceforGMfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGBfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.GunneryBallisticSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Ballistic");
                masterCB.setName("chanceforGBfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGLfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.GunneryLaserSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Laser");
                masterCB.setName("chanceforGLfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforTNfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.TraitID)) {
                masterCB = new javax.swing.JCheckBox("Trait");
                masterCB.setName("chanceforTNfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforEIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.EnhancedInterfaceID)) {
                masterCB = new javax.swing.JCheckBox("Enhanced Interface");
                masterCB.setName("chanceforEIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGTfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.GiftedID)) {
                masterCB = new javax.swing.JCheckBox("Gifted");
                masterCB.setName("chanceforGTfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforQSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.QuickStudyID)) {
                masterCB = new javax.swing.JCheckBox("Quick Study");
                masterCB.setName("chanceforQSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforMTfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.MedTechID)) {
                masterCB = new javax.swing.JCheckBox("Med Tech");
                masterCB.setName("chanceforMTfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforVDNIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.VDNIID)) {
                masterCB = new javax.swing.JCheckBox("VDNI");
                masterCB.setName("chanceforVDNIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforBVDNIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.BufferedVDNIID)) {
                masterCB = new javax.swing.JCheckBox("Buffered VDNI");
                masterCB.setName("chanceforBVDNIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforPSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      pilot.getSkills().has(PilotSkill.PainShuntID)) {
                masterCB = new javax.swing.JCheckBox("Pain Shunt");
                masterCB.setName("chanceforPSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

        } else {

            if (Integer.parseInt(client.getServerConfigs("chanceforATfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      (!pilot.getSkills().has(PilotSkill.AstechSkillID) ||
                             pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel() < 2)) {
                masterCB = new javax.swing.JCheckBox("Astech");
                masterCB.setName("chanceforATfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforDMfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.DodgeManeuverSkillID)) {
                masterCB = new javax.swing.JCheckBox("Dodge Maneuver");
                masterCB.setName("chanceforDMfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforMSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.MeleeSpecialistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Melee Specialist");
                masterCB.setName("chanceforMSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforPRfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.PainResistanceSkillID)) {
                masterCB = new javax.swing.JCheckBox("Pain Resistance");
                masterCB.setName("chanceforPRfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforSVfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.SurvivalistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Survivalist");
                masterCB.setName("chanceforSVfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforIMfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.IronManSkillID)) {
                masterCB = new javax.swing.JCheckBox("Iron Man");
                masterCB.setName("chanceforIMfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforEDfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      (!pilot.getSkills().has(PilotSkill.EdgeSkillID) ||
                             pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID).getLevel() <
                                   Integer.parseInt(client.getServerConfigs("MaxEdgeChanges")))) {
                masterCB = new javax.swing.JCheckBox("Edge");
                masterCB.setName("chanceforEDfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforMAfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.ManeuveringAceSkillID)) {
                masterCB = new javax.swing.JCheckBox("Maneuvering Ace");
                masterCB.setName("chanceforMAfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforNAPfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
                masterCB = new javax.swing.JCheckBox("Natural Aptitude Piloting");
                masterCB.setName("chanceforNAPfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforNAGfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
                masterCB = new javax.swing.JCheckBox("Natural Aptitude Gunnery");
                masterCB.setName("chanceforNAGfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforWSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.WeaponSpecialistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Weapon Specialist");
                masterCB.setName("chanceforWSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforTGfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.TacticalGeniusSkillID)) {
                masterCB = new javax.swing.JCheckBox("Tactical Genius");
                masterCB.setName("chanceforTGfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGMfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.GunneryMissileSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Missile");
                masterCB.addActionListener(this);
                masterCB.setName("chanceforGMfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGBfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.GunneryBallisticSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Ballistic");
                masterCB.setName("chanceforGBfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGLfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.GunneryLaserSkillID)) {
                masterCB = new javax.swing.JCheckBox("Gunnery Laser");
                masterCB.setName("chanceforGLfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforTNfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.TraitID)) {
                masterCB = new javax.swing.JCheckBox("Trait");
                masterCB.setName("chanceforTNfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforEIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.EnhancedInterfaceID)) {
                masterCB = new javax.swing.JCheckBox("Enhanced Interface");
                masterCB.setName("chanceforEIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforGTfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.GiftedID)) {
                masterCB = new javax.swing.JCheckBox("Gifted");
                masterCB.setName("chanceforGTfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforQSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.QuickStudyID)) {
                masterCB = new javax.swing.JCheckBox("Quick Study");
                masterCB.setName("chanceforQSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforMTfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.MedTechID)) {
                masterCB = new javax.swing.JCheckBox("Med Tech");
                masterCB.setName("chanceforMTfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforVDNIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.VDNIID)) {
                masterCB = new javax.swing.JCheckBox("VDNI");
                masterCB.setName("chanceforVDNIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforBVDNIfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.BufferedVDNIID)) {
                masterCB = new javax.swing.JCheckBox("Buffered VDNI");
                masterCB.setName("chanceforBVDNIfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs("chanceforPSfor" +
                                                               Unit.getTypeClassDesc(playerUnit.getType()))) > 0 &&
                      !pilot.getSkills().has(PilotSkill.PainShuntID)) {
                masterCB = new javax.swing.JCheckBox("Pain Shunt");
                masterCB.setName("chanceforPSfor" + Unit.getTypeClassDesc(playerUnit.getType()));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }
        }
    }

    public void keyPressed(java.awt.event.KeyEvent arg0) {
    }

    public void keyReleased(java.awt.event.KeyEvent arg0) {
    }

    public void keyTyped(java.awt.event.KeyEvent arg0) {

        if (arg0.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
            super.dispose();
        } else if (arg0.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            sendPromoteCommands();
        }

    }

    private void calculateExpCost() {

        int cost = 0;
        int gun = pilot.getGunnery();
        int piloting = pilot.getPiloting();
        if (pilot.getSkills().has(PilotSkill.NaturalAptitudeGunnerySkillID)) {
            gun++;
        }

        if (pilot.getSkills().has(PilotSkill.NaturalAptitudePilotingSkillID)) {
            piloting++;
        }

        for (Object object : MasterPanel.getComponents()) {
            int pilotCost;
            if (object instanceof javax.swing.JCheckBox checkBox) {
                if (checkBox.isSelected()) {
                    if (checkBox.getName().equalsIgnoreCase("gunnery") ||
                              checkBox.getName().equalsIgnoreCase("piloting")) {
                        int totalSkill = Math.min(9, gun + piloting);
                        pilotCost = Integer.parseInt(client.getServerConfigs("BaseRollToLevel"));
                        pilotCost *= Integer.parseInt(client.getServerConfigs("MultiplierPerPreviousLevel"));
                        pilotCost *= 10 - totalSkill;

                    } else if (checkBox.getName().startsWith("chanceforATfor")) {
                        if (pilot.getSkills().has(PilotSkill.AstechSkillID)) {
                            int level = pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel();

                            pilotCost = Integer.parseInt(client.getServerConfigs(checkBox.getName()));
                            pilotCost *= level + 2;
                        } else {
                            pilotCost = Integer.parseInt(client.getServerConfigs(checkBox.getName()));
                        }
                    } else if (checkBox.getName().startsWith("chanceforEDfor")) {
                        if (pilot.getSkills().has(PilotSkill.EdgeSkillID)) {
                            int level = pilot.getSkills().getPilotSkill(PilotSkill.EdgeSkillID).getLevel();

                            pilotCost = Integer.parseInt(client.getServerConfigs(checkBox.getName()));
                            pilotCost *= level + 1;
                        } else {
                            pilotCost = Integer.parseInt(client.getServerConfigs(checkBox.getName()));
                        }
                    } else {
                        pilotCost = Integer.parseInt(client.getServerConfigs(checkBox.getName()));
                    }

                    if (demoting) {
                        pilotCost = (int) Math.round(((double) pilotCost) *
                                                           Double.parseDouble(client.getServerConfigs(
                                                                 "PilotUpgradeSellBackPercent")));
                    }
                    cost += pilotCost;
                }
            }
        }

        if (pilot.getSkills().has(PilotSkill.GiftedID) && !demoting) {
            cost *= (int) (1 - Double.parseDouble(client.getServerConfigs("GiftedPercent")));
        }

        expCost.setText(Integer.toString(cost));
    }

    private void sendPromoteCommands() {

        String baseCommand = IClient.CAMPAIGN_PREFIX + "c promotepilot#" + playerUnit.getId() + "#";

        if (demoting) {
            baseCommand = IClient.CAMPAIGN_PREFIX + "c demotepilot#" + playerUnit.getId() + "#";
        }

        for (Object object : MasterPanel.getComponents()) {
            if (object instanceof javax.swing.JCheckBox checkBox) {
                if (checkBox.isSelected()) {
                    String cmd = checkBox.getName();

                    if (cmd.startsWith("chancefor")) {
                        int startPos = "chancefor".length();
                        cmd = cmd.substring(startPos, cmd.indexOf("for", startPos));
                    }
                    client.sendChat(baseCommand + cmd);
                }
            }
        }

    }
}// end BulkRepairDialog.java
