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

/**
 * @author jtighe
 * @author Spork
 */
public class NoPlayPanel extends JPanel {

    @Serial
    private static final long serialVersionUID = -1623867291283606083L;

    public NoPlayPanel(IClient client) {
        super();
        /*
         * NO PLAY setup
         */
        JPanel noPlayBox = new JPanel();
        noPlayBox.setLayout(new BoxLayout(noPlayBox, BoxLayout.Y_AXIS));

        JPanel noPlaySpring = new JPanel(new SpringLayout());

        JTextField baseTextField = new JTextField(5);
        noPlaySpring.add(new JLabel("Max Player No-Plays:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of players someone can add to no-play list.");
        baseTextField.setName("NoPlayListSize");
        noPlaySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        noPlaySpring.add(new JLabel("No-Play " + client.moneyOrFluMessage(true, true, -1) + " Cost:",
              SwingConstants.TRAILING));
        baseTextField.setToolTipText(client.moneyOrFluMessage(true, true, -1) +
                                           " charged to remove a player from the no-play list.");
        baseTextField.setName("NoPlayMUCost");
        noPlaySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        noPlaySpring.add(new JLabel("No-Play RP Cost:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("RP charged to remove a player from the no-play list.");
        baseTextField.setName("NoPlayRPCost");
        noPlaySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        noPlaySpring.add(new JLabel("No-Play " + client.moneyOrFluMessage(false, true, -1) + " Cost:",
              SwingConstants.TRAILING));
        baseTextField.setToolTipText(client.moneyOrFluMessage(false, false, -1) +
                                           " charged to remove a player from the no-play list.");
        baseTextField.setName("NoPlayInfluenceCost");
        noPlaySpring.add(baseTextField);

        JCheckBox baseCheckBox = new JCheckBox("Admin No-Plays Count");
        baseCheckBox.setToolTipText("<HTML>" +
                                          "Check to have no-plays added by admins count towards the<br>" +
                                          "maximum. Note that admins can add no-plays in excess of<br>" +
                                          "the cap. Enabling this simply prevents players from adding<br>" +
                                          "their own choices to their no-play lists if admins have<br>" +
                                          "been forced to make additions equal to, or in excess of,<br>" +
                                          "the max.</HTML>");
        baseCheckBox.setName("NoPlaysFromAdminsCountForMax");
        noPlaySpring.add(baseCheckBox);

        SpringLayoutHelper.setupSpringGrid(noPlaySpring, 2);

        // finalize layout
        noPlayBox.add(noPlaySpring);
        add(noPlayBox);
    }

}
