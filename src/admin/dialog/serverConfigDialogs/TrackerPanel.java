/*
 * MekWars - Copyright (C) 2018
 *
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 */

package admin.dialog.serverConfigDialogs;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import common.VerticalLayout;
import common.util.SpringLayoutHelper;

/**
 * 
 * @author Spork
 * 
 * A JPanel where we configure everything having to do with the tracker.  Currently
 * only recreating the old tracker information.  Next step is to add HPGNet capabilities
 * 
 */
public class TrackerPanel extends JPanel {

	private static final long serialVersionUID = -4629994177197981829L;

	private JTextField baseTextField = new JTextField(5);
	private JCheckBox baseCheckBox = new JCheckBox();
	
	private void init(String uuid) {

		setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));

		String description = "<HTML>The tracker information has been moved out of serverdata.txt and into<br />"
							+"campaignconfig.txt, allowing us to edit it while the server is running.<br /><br />"
							+"The UUID is system-generated. If you have copied your server from one already listed<br />"
							+"on the tracker, please regenerate the UUID, or you will over-write the information <br />"
							+"for the other server.</HTML>";
		
		JPanel descPanel = new JPanel();
		JPanel uuidPanel = new JPanel();
		JPanel detailsPanel = new JPanel();
		
		descPanel.add(new JLabel(description));
		descPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		uuidPanel.setLayout(new VerticalLayout());
		uuidPanel.add(new JLabel(" "));
		uuidPanel.add(new JLabel("UUID: " + uuid));
		
		baseCheckBox = new JCheckBox("Regenerate");
		baseCheckBox.setName("TrackerResetUUID");
		uuidPanel.add(baseCheckBox);
		
		detailsPanel.setLayout(new VerticalLayout());
		
		baseCheckBox = new JCheckBox("Register This Server on the Tracker");
		baseCheckBox.setName("UseTracker");
		detailsPanel.add(baseCheckBox);
		
		JPanel panel1 = new JPanel(new SpringLayout());

		baseTextField = new JTextField(30);
		baseTextField.setName("ServerName");
		baseTextField.setToolTipText("The Server's name.  This will show in the Tracker as well as at the beginning of MainChat.");
		panel1.add(new JLabel("Server Name:", SwingConstants.TRAILING));
		panel1.add(baseTextField);
		
		baseTextField = new JTextField(30);
		baseTextField.setName("TrackerAddress");
		baseTextField.setToolTipText("The tracker's IP address or host name.  The official one is at tracker.mekwars.org");
		panel1.add(new JLabel("Tracker Address", SwingConstants.TRAILING));
		panel1.add(baseTextField);
		
		baseTextField = new JTextField(30);
		baseTextField.setName("TrackerLink");
		baseTextField.setToolTipText("Link to your server, where users can download a client");
		panel1.add(new JLabel("Tracker Link", SwingConstants.TRAILING));
		panel1.add(baseTextField);
		
		baseTextField = new JTextField(30);
		baseTextField.setName("TrackerDesc");
		baseTextField.setToolTipText("Brief description of your server.");
		panel1.add(new JLabel("Description", SwingConstants.TRAILING));
		panel1.add(baseTextField);

		SpringLayoutHelper.setupSpringGrid(panel1, 2);
		detailsPanel.add(panel1);
		
		add(new JLabel(" "));		
		add(descPanel);
		add(uuidPanel);
		add(detailsPanel);
	}

	public TrackerPanel(String uuid) {
		super();
		init(uuid);
	}
}
