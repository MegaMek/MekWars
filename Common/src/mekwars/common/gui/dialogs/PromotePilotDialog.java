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

/**
 * "Pilot Promotion Dialog" &mdash; lets the player spend (or, in demoting mode, sell back) experience points to
 * buy incremental Gunnery/Piloting improvements and a large catalog of special pilot abilities (Astech, Edge,
 * Dodge Maneuver, Melee Specialist, Tactical Genius, VDNI, etc; see {@link PilotSkill}) for a single unit's pilot.
 * <p>
 * On construction it inspects the given unit's {@link Pilot} and, for each possible skill upgrade (or, when
 * {@link #demoting} is {@code true}, downgrade/sell-back), adds a checkbox if the server config indicates that
 * skill has a non-zero purchase "chance"/cost for this unit type and the pilot is eligible (doesn't already have
 * it, or has it below its max level). Checking boxes recalculates a running experience-point cost
 * (see {@link #calculateExpCost()}); pressing the "Buy"/"Sell" button sends the corresponding
 * {@code promotepilot}/{@code demotepilot} campaign commands for every checked box.
 * <p>
 * Note the trailing file comment "end BulkRepairDialog.java" suggests this class began life as a copy-paste of
 * {@code BulkRepairDialog} that was never fully renamed/cleaned up.
 */
public class PromotePilotDialog extends javax.swing.JFrame
      implements java.awt.event.ActionListener, java.awt.event.KeyListener {

    /**
     * Required by {@link java.io.Serializable}; this dialog is never actually serialized over the wire.
     */
    @Serial
    private static final long serialVersionUID = -8988175448434842033L;
    /** Action command string for the "Buy"/"Sell" confirm button. */
    private final static String okayCommand = "Ok";
    /** Action command string for the "Close" cancel button. */
    private final static String cancelCommand = "Close";
    // store the client backlink for other things to use
    private final IClient client;
    /** The player's in-campaign unit whose pilot is being promoted or demoted. */
    private final CUnit playerUnit;
    /** {@code true} if this dialog is selling back/removing pilot skills rather than purchasing them. */
    private final boolean demoting;
    /** Panel holding the exp labels and one checkbox per purchasable/sellable skill, laid out in a spring grid. */
    private final javax.swing.JPanel MasterPanel = new javax.swing.JPanel(new javax.swing.SpringLayout());
    /** Read-only field displaying the pilot's current experience point total. */
    private final javax.swing.JTextField currentExp = new javax.swing.JTextField();
    /** Read-only field displaying the running experience cost of the currently checked skill upgrades/sell-backs. */
    private final javax.swing.JTextField expCost = new javax.swing.JTextField();
    /** The pilot being promoted/demoted; resolved from {@link #playerUnit} in {@link #loadPanel()}. */
    private Pilot pilot = null;

    /**
     * Builds and displays the pilot promotion/demotion dialog for a single unit's pilot.
     * <p>
     * Wires up the Buy/Sell and Close buttons (with tooltip/mnemonic text depending on {@code demoting}),
     * populates {@link #MasterPanel} with the current-exp/exp-cost fields and one checkbox per eligible skill via
     * {@link #loadPanel()}, wraps everything in a {@link javax.swing.JOptionPane}, and shows the dialog centered
     * on the client's main frame.
     *
     * @param client   the campaign client, used to read server configs (costs/eligibility) and send commands
     * @param unitID   ID of the player's unit whose pilot is being promoted/demoted
     * @param demoting {@code true} to sell back/remove skills (Sell button), {@code false} to purchase new ones
     *                 (Buy button)
     */
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

    /**
     * Populates {@link #MasterPanel} with the current-exp/exp-cost display fields and one checkbox per eligible
     * pilot skill purchase (or, when {@link #demoting}, sell-back).
     * <p>
     * Gunnery and Piloting improvements are offered first (when not demoting) if the pilot's current skill is
     * still worse than the server's configured {@code BestGunnerySkill}/{@code BestPilotingSkill} floor.
     * <p>
     * Then, unless the pilot has already hit the server's {@code MaxPilotUpgrades} cap (checked only when buying,
     * not demoting &mdash; note Gunnery/Piloting checkboxes above are added before this cap check and so are never
     * blocked by it), a checkbox is added for each special pilot skill (Astech, Dodge Maneuver, Melee Specialist,
     * Pain Resistance, Survivalist, Iron Man, Edge, Maneuvering Ace, Natural Aptitude Piloting/Gunnery, Weapon
     * Specialist, Tactical Genius, Gunnery Missile/Ballistic/Laser, Trait, Enhanced Interface, Gifted, Quick
     * Study, Med Tech, VDNI, Buffered VDNI, Pain Shunt) whenever:
     * <ul>
     *     <li>the server config {@code chancefor<ABBR>for<UnitType>} for that skill is greater than zero
     *     (i.e. the skill is enabled/purchasable for this unit type at all), and</li>
     *     <li>when demoting: the pilot currently has the skill (so it can be sold back); when buying: the pilot
     *     either doesn't have the skill yet, or (for the handful of skills with levels, like Edge and Astech)
     *     is below its configured maximum level.</li>
     * </ul>
     * Each checkbox's Swing "name" is set to the server config key itself (e.g. {@code "chanceforEDforMek"}), which
     * {@link #sendPromoteCommands()} later parses back out to build the campaign command, and its action listener
     * triggers {@link #calculateExpCost()} to keep the running total in {@link #expCost} up to date.
     */
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
            masterCB = new javax.swing.JCheckBox(String.format("Gunnery %s", pilot.getGunnery() - 1));
            masterCB.setName("gunnery");
            masterCB.addActionListener(this);
            MasterPanel.add(masterCB);
        }

        if (pilot.getPiloting() > Integer.parseInt(client.getServerConfigs("BestPilotingSkill")) && !demoting) {
            masterCB = new javax.swing.JCheckBox(String.format("Piloting %s", pilot.getPiloting() - 1));
            masterCB.setName("piloting");
            masterCB.addActionListener(this);
            MasterPanel.add(masterCB);
        }

        int maxSkills = Integer.parseInt(client.getServerConfigs("MaxPilotUpgrades"));
        if (maxSkills > -1 && pilot.getSkills().size() >= maxSkills && !demoting) {
            return;
        }

        if (demoting) {
            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforATfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      (pilot.getSkills().has(PilotSkill.AsTechSkillID))) {
                masterCB = new javax.swing.JCheckBox("Astech");
                masterCB.setName(String.format("chanceforATfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforDMfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.DodgeManeuverSkillID)) {
                masterCB = new javax.swing.JCheckBox("Dodge Maneuver");
                masterCB.setName(String.format("chanceforDMfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforMSfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.MeleeSpecialistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Melee Specialist");
                masterCB.setName(String.format("chanceforMSfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforPRfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.PainResistanceSkillID)) {
                masterCB = new javax.swing.JCheckBox("Pain Resistance");
                masterCB.setName(String.format("chanceforPRfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforSVfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.SurvivalistSkillID)) {
                masterCB = new javax.swing.JCheckBox("Survivalist");
                masterCB.setName(String.format("chanceforSVfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
                masterCB.addActionListener(this);
                MasterPanel.add(masterCB);

            }

            if (Integer.parseInt(client.getServerConfigs(String.format("chanceforIMfor%s", Unit.getTypeClassDesc(playerUnit.getType())))) >
                      0 &&
                      pilot.getSkills().has(PilotSkill.IronManSkillID)) {
                masterCB = new javax.swing.JCheckBox("Iron Man");
                masterCB.setName(String.format("chanceforIMfor%s", Unit.getTypeClassDesc(playerUnit.getType())));
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
                      (!pilot.getSkills().has(PilotSkill.AsTechSkillID) ||
                             pilot.getSkills().getPilotSkill(PilotSkill.AsTechSkillID).getLevel() < 2)) {
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

    /**
     * Handles the Buy/Sell button ({@link #sendPromoteCommands()} then closes the dialog), the Close button (just
     * closes the dialog without sending anything), and any checkbox toggle (recomputes the running experience
     * cost via {@link #calculateExpCost()}).
     *
     * @param e the triggering button click or checkbox toggle event
     */
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

    /**
     * Sends one {@code promotepilot} (or, when {@link #demoting}, {@code demotepilot}) campaign command per
     * checked skill checkbox in {@link #MasterPanel}.
     * <p>
     * For the special-skill checkboxes, the checkbox's name (a server config key of the form
     * {@code "chancefor<ABBR>for<UnitType>"}) is trimmed down to just the {@code <ABBR>} skill abbreviation before
     * being appended to the base command; the plain "gunnery"/"piloting" checkboxes are sent as-is since their
     * names don't start with {@code "chancefor"}.
     */
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

    /**
     * Recomputes the total experience point cost of every currently-checked skill checkbox and writes it into
     * {@link #expCost}. Effective Gunnery/Piloting (bumped up by one if the pilot already has the corresponding
     * Natural Aptitude skill) is used to look up the base Gunnery/Piloting upgrade price via the server's
     * {@code BaseRollToLevel} and {@code MultiplierPerPreviousLevel} configs. Astech and Edge costs additionally
     * scale with the pilot's current level in that skill. When {@link #demoting}, every individual cost is then
     * scaled down by the server's {@code PilotUpgradeSellBackPercent}. Finally, if the pilot has the Gifted skill
     * and this is a purchase (not a sell-back), the total is reduced according to the server's {@code GiftedPercent}.
     */
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
                        if (pilot.getSkills().has(PilotSkill.AsTechSkillID)) {
                            int level = pilot.getSkills().getPilotSkill(PilotSkill.AsTechSkillID).getLevel();

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
            // BUG: (1 - GiftedPercent) is cast to int before multiplying. For any GiftedPercent strictly between
            // 0 and 1 (the intended "discount fraction" range) this truncates to 0, zeroing out the entire cost
            // rather than applying a partial discount. Only GiftedPercent <= 0 (multiplier truncates to 1, i.e.
            // no discount) avoids this. Documenting actual behavior, not fixing it here.
            cost *= (int) (1 - Double.parseDouble(client.getServerConfigs("GiftedPercent")));
        }

        expCost.setText(Integer.toString(cost));
    }

    /**
     * Escape closes the dialog without buying/selling anything; Enter sends the promote/demote commands for all
     * currently-checked skills (note: unlike the Buy/Sell button, this does not also close the dialog afterward).
     *
     * @param arg0 the key-typed event for this window
     */
    public void keyTyped(java.awt.event.KeyEvent arg0) {

        if (arg0.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
            super.dispose();
        } else if (arg0.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            sendPromoteCommands();
        }

    }

    /** Unused; required by {@link java.awt.event.KeyListener}. */
    public void keyPressed(java.awt.event.KeyEvent arg0) {
    }

    /** Unused; required by {@link java.awt.event.KeyListener}. */
    public void keyReleased(java.awt.event.KeyEvent arg0) {
    }
}// end BulkRepairDialog.java
