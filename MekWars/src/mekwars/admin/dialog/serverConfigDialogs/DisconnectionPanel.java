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

package admin.dialog.serverConfigDialogs;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class DisconnectionPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8909645618097617084L;
	private JTextField baseTextField = new JTextField(5);
	
	public DisconnectionPanel() {
		super();
        /*
         * DISCONNECTION setup
         */
        JPanel discoSpring = new JPanel(new SpringLayout());

        baseTextField = new JTextField(5);
        discoSpring.add(new JLabel("Additional Units Destroyed:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Number of disconnecting players' units destoyed,<br>" + "in addition to those already dead from IPUs.</html>");
        baseTextField.setName("DisconnectionAddUnitsDestroyed");
        discoSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        discoSpring.add(new JLabel("Additional Units Ejected:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Number of disconnecting players' units ejected,<br>" + "in addition to those already marked as salvage<br>" + "by the in-game status updates.</html>");
        baseTextField.setName("DisconnectionAddUnitsSalvage");
        discoSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        discoSpring.add(new JLabel("Time Before Report:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Amount of time, in seconds, disconnecting player<br>" + "has to return before games is autoresolved.</html>");
        baseTextField.setName("DisconnectionTimeToReport");
        discoSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        discoSpring.add(new JLabel("Reconnection Grace Period:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Amount of time added (sec) back to disconnection counter when<br>" + "a player returns. For example, if there are 10 minutes<br>" + "before a report and a grace period of 3 minutes, a player<br>" + "who leaves for 6 minutes and then returns will have 6 minutes<br>" + "(10m to report - 6 minutes offline + 2 min grace period = 6 min)<br>" + "to return if he leaves the server a second time. This keeps<br>" + "players who need to leave/recon because of a crach from being<br>" + "penalized but prevents people from repeatedly disconnecting for<br>" + "long periods of time as a delaying tactic.");
        baseTextField.setName("DisconnectionGracePeriod");
        discoSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        discoSpring.add(new JLabel("% of normal pay:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html>" + "Percentage of normal pay (0-100) given to players<br>" + "who disconnect. Loser modifiers are applied normally.</html>");
        baseTextField.setName("DisconnectionPayPercentage");
        discoSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(discoSpring, 5, 2);
        add(discoSpring);
	}

}