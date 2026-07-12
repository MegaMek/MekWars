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

/**
 * @author Spork
 * @author jtighe
 */

package mekwars.admin.dialog.serverConfigDialogs;

import java.io.Serial;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public class TechnicianPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 4472081938721953252L;

    public TechnicianPanel(IClient client) {
        super();
        /*
         * TECH PANEL CONSTRUCTION Technician (and bays from XP) options.
         */
        JPanel techsBox = new JPanel();
        techsBox.setLayout(new BoxLayout(techsBox, BoxLayout.Y_AXIS));
        JPanel techsCBoxFlow = new JPanel();
        JPanel techsSendRecPayFlow = new JPanel();
        JPanel techSpring = new JPanel(new SpringLayout());
        techsBox.add(techsCBoxFlow);
        techsBox.add(techsSendRecPayFlow);
        techsBox.add(techSpring);

        // the basic CBox flow
        JCheckBox baseCheckBox = new JCheckBox("Use Techs");
        baseCheckBox.setToolTipText("Unchecking disables technicians. Not advised.");

        baseCheckBox.setName("UseTechnicians");
        techsCBoxFlow.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Use XP");

        baseCheckBox.setToolTipText("Check grants additional technicians w/ XP.");
        baseCheckBox.setName("UseExperience");
        techsCBoxFlow.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Decreasing Cost");

        baseCheckBox.setToolTipText("Checking lowers tech hiring costs w/ XP.");
        baseCheckBox.setName("DecreasingTechCost");
        techsCBoxFlow.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Disable Tech Advancement");
        baseCheckBox.setToolTipText("Checking disables tech advancement and retiring");
        baseCheckBox.setName("DisableTechAdvancement");
        techsCBoxFlow.add(baseCheckBox);

        // the sendRecPay flow.
        baseCheckBox = new JCheckBox("Sender Pays");

        baseCheckBox.setToolTipText("If checked, a player sending a unit will pay techs.");
        baseCheckBox.setName("SenderPaysOnTransfer");
        techsSendRecPayFlow.add(baseCheckBox);

        baseCheckBox = new JCheckBox("Recipient Pays");

        baseCheckBox.setToolTipText("If checked, a player receiving a unit will pay techs.");
        baseCheckBox.setName("ReceiverPaysOnTransfer");
        techsSendRecPayFlow.add(baseCheckBox);

        // set up the spring
        JTextField baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Base Tech Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Starting cost to hire a technician");
        baseTextField.setName("BaseTechCost");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("XP for Decrease:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount of XP required to reduce hiring cost by 1 " +
                                           client.moneyOrFluMessage(true, true, -1));
        baseTextField.setName("XPForDecrease");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Minimum Tech Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Lowest hiring price. XP cannot reduce below this level.");
        baseTextField.setName("MinimumTechCost");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Additive Per Tech:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>" +
                                           "Use additive costs -- each tech costs as much as the last one, plus the additive. EG -<br>" +
                                           "with .05 set, the first tech would cost .05, the second .10, the third .15, the fourth .20,<br>" +
                                           "such that your first 4 techs cost haf a Cbill (total) to maintain, while the 10th tech costs<br>" +
                                           "half a " +
                                           client.moneyOrFluMessage(true, true, -1) +
                                           " all by himself. A cap on this price can be set, after which there is no further<br>" +
                                           "increase. The ceiling ABSOLUTELY MUST be a multiple of the additive.</HTML>");
        baseTextField.setName("AdditivePerTech");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Additive Ceiling:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Additive ceiling. Post-game per-tech costs don't increase past this level.");
        baseTextField.setName("AdditiveCostCeiling");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Transfer Payment:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Percentage of usual post-game cost charged if transfer fees are enabled.");
        baseTextField.setName("TransferPayment");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Maint Increase:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount maintainance level is increased each slice a unit is maintained");
        baseTextField.setName("MaintainanceIncrease");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Maint Decrease:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Amount maintainance level is lowered each slice a unit is unmaintained");
        baseTextField.setName("MaintainanceDecrease");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Base Unmaint Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Mainatainance level set when a unit is first unmaintained. Set to 100 to disable.");
        baseTextField.setName("BaseUnmaintainedLevel");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Unmaintain Penalty:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("" +
                                           "<HTML>Maintainance reduction for units which are already below 100. If the BaseLevel is lower than current<br>" +
                                           "level minus penalty, it is used instead. Example1: A unit has a maintainance level of 90 and is set to<br>" +
                                           "unmaintained status. The unmaint penalty is 10 and base elvel is 75. 90-10 = 80, so the base level of 75 is<br>" +
                                           "set. Example2: A unit has an mlevel of 80 and is set to unmaintained. 80 - 10 = 70. 70 is set and the base<br>" +
                                           "level (75) is ignored.</HTML>");
        baseTextField.setName("UnmaintainedPenalty");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Transfer Scrap Level:", SwingConstants.TRAILING));
        baseTextField.setToolTipText(
              "<HTML>Units @ or under this maint. level must survive a scrap check<br>to be transfered. Set to 0 to disable</HTML>");
        baseTextField.setName("TransferScrapLevel");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs To Proto Point Ratio:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Ratio of Techs to 5 Protos Default 1 tech</HTML>");
        baseTextField.setName("TechsToProtoPointRatio");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Light Mek:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a light Mek</HTML>");
        baseTextField.setName("TechsForLightMek");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Medium Mek:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a medium Mek</HTML>");
        baseTextField.setName("TechsForMediumMek");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Heavy Mek:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a heavy Mek</HTML>");
        baseTextField.setName("TechsForHeavyMek");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Assault Mek:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain an assault Mek</HTML>");
        baseTextField.setName("TechsForAssaultMek");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Light Vehicle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a light Vehicle</HTML>");
        baseTextField.setName("TechsForLightVehicle");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Medium Vehicle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a medium Vehicle</HTML>");
        baseTextField.setName("TechsForMediumVehicle");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Heavy Vehicle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a heavy Vehicle</HTML>");
        baseTextField.setName("TechsForHeavyVehicle");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Assault Vehicle:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain an assault Vehicle</HTML>");
        baseTextField.setName("TechsForAssaultVehicle");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Light Infantry:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a light Infantry</HTML>");
        baseTextField.setName("TechsForLightInfantry");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Medium Infantry:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a medium Infantry</HTML>");
        baseTextField.setName("TechsForMediumInfantry");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Heavy Infantry:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a heavy Infantry</HTML>");
        baseTextField.setName("TechsForHeavyInfantry");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Assault Infantry:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain an assault Infantry</HTML>");
        baseTextField.setName("TechsForAssaultInfantry");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Light BattleArmor:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a light BattleArmor</HTML>");
        baseTextField.setName("TechsForLightBattleArmor");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Medium BattleArmor:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a medium BattleArmor</HTML>");
        baseTextField.setName("TechsForMediumBattleArmor");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Heavy BattleArmor:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a heavy BattleArmor</HTML>");
        baseTextField.setName("TechsForHeavyBattleArmor");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Assault BattleArmor:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain an assault BattleArmor</HTML>");
        baseTextField.setName("TechsForAssaultBattleArmor");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Light Aero:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a light Aero</HTML>");
        baseTextField.setName("TechsForLightAero");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Medium Aero:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a medium Aero</HTML>");
        baseTextField.setName("TechsForMediumAero");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Heavy Aero:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain a heavy Aero</HTML>");
        baseTextField.setName("TechsForHeavyAero");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Techs per Assault Aero:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML>Number of Techs it takes to maintain an assault Aero</HTML>");
        baseTextField.setName("TechsForAssaultAero");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Non-House Unit Increased Techs:", SwingConstants.TRAILING));
        baseTextField.setToolTipText(
              "Float field.  Multiplier to tech cost of non-house units.  Only used with Tech Repair.");
        baseTextField.setName("NonFactionUnitsIncreasedTechs");
        techSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        techSpring.add(new JLabel("Max Techs to Hire:", SwingConstants.TRAILING));
        baseTextField.setToolTipText(
              "Integer field.  Max number of techs that can be hired.  Set to -1 for unlimited.  Users with more than this number of techs will lose them at next login.");
        baseTextField.setName("MaxTechsToHire");
        techSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(techSpring, 4);

        // finalize the layout
        add(techsBox);


    }
}
