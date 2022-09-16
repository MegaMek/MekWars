/*
 * MekWars - Copyright (C) 2016
 *
 * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package admin.dialog.serverConfigDialogs;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import common.util.SpringLayoutHelper;

/**
 * A JPanel for scheduled tasks.  In this panel, admins can set the frequency with which tasks will execute.
 * @author Spork
 * @version 2016.10.06
 */
public class SchedulerPanel extends JPanel {
	private JTextField tf;
	private static final long serialVersionUID = 4836798646993460609L;
	
	public SchedulerPanel() {
		super();
		JPanel factionsPanel;
		JPanel activityPanel;
		
		factionsPanel = new JPanel(new SpringLayout());
		factionsPanel.setBorder(BorderFactory.createTitledBorder("Factions"));
		//factionsPanel.setLayout(new SpringLayout());
		
		tf = new JTextField(5);
		tf.setName("Scheduler_FactionSave");
		tf.setToolTipText("How often (in seconds) factions save their status"); 
		//label = new JLabel("Save every");
		factionsPanel.add(new JLabel("Save every", SwingConstants.TRAILING));
		factionsPanel.add(tf);
		tf.setEnabled(false); // Not Yet Implemented
		
		activityPanel = new JPanel(new SpringLayout());
		activityPanel.setBorder(BorderFactory.createTitledBorder("Player Activity"));

		tf = new JTextField(5);
		tf.setName("Scheduler_PlayerActivity_flu");
		tf.setToolTipText("How often (in seconds) an active player will be granted influence");
		activityPanel.add(new JLabel("Flu every", SwingConstants.TRAILING));
		activityPanel.add(tf);
		
		tf = new JTextField(5);
		tf.setName("Scheduler_PlayerActivity_comps");
		tf.setToolTipText("How often (in seconds) an active player will generate componentss for his faction");
		activityPanel.add(new JLabel("Comps every", SwingConstants.TRAILING));
		activityPanel.add(tf);

		SpringLayoutHelper.setupSpringGrid(factionsPanel, 2);
		SpringLayoutHelper.setupSpringGrid(activityPanel, 2);
		this.add(factionsPanel);
		this.add(activityPanel);
		
	}

}
