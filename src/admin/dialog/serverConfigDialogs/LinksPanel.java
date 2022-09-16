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

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import common.VerticalLayout;

/**
 * 
 * @author Salient - contains link area options options
 */
public class LinksPanel extends JPanel {

	private static final long serialVersionUID = -4629994177197981829L;

	private JTextField baseTextField = new JTextField(5);
	private JCheckBox baseCheckBox = new JCheckBox();
	private JLabel baseLabel = new JLabel();

	private void init() {

		setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));

		String description = "<HTML>Enabling the Link Area will allow the SO to create up to 3 quick link buttons<br>"
				+ " in a new area on the very bottom right of the client window. You can then set the icon and<br>"
				+ " destination of these buttons which open the users default browser to view the content.<br>"
				+ " An example would be, a button for server homepage, one for discord, and another for<br>"
				+ " facebook. Icon Images should be placed in '/data/images/misc/' before client distribution<br>"
				+ " The Rules panel is a new client tab that you can use to display a simple HTML file that<br>"
				+ " should also be included with the client</HTML>";
		
		JPanel panel0 = new JPanel();
		JPanel panel1 = new JPanel();
		JPanel panel2 = new JPanel();
		JPanel panel2a = new JPanel();
		JPanel panel2b = new JPanel();
		JPanel panel3 = new JPanel();
		JPanel panel3a = new JPanel();
		JPanel panel3b = new JPanel();
		JPanel panel4 = new JPanel();
		JPanel panel4a = new JPanel();
		JPanel panel4b = new JPanel();
		//JPanel panel5 = new JPanel();
		//JPanel panel5a = new JPanel();
		//JPanel panel5b = new JPanel();

		panel0.add(new JLabel(description));

		panel1.setBorder(BorderFactory.createTitledBorder("Link Area Options"));
		
		baseCheckBox = new JCheckBox("Enable Link Area");
		baseCheckBox.setToolTipText("<HTML>Must be enabled if you wish to use this feature.</HTML>");
		baseCheckBox.setName("Enable_Link_Area");
		panel1.add(baseCheckBox);
		
		baseCheckBox = new JCheckBox("Enable Button1");
		baseCheckBox.setName("Enable_Link1_Button");
		panel1.add(baseCheckBox);

		baseCheckBox = new JCheckBox("Enable Button2");
		baseCheckBox.setName("Enable_Link2_Button");
		panel1.add(baseCheckBox);

		baseCheckBox = new JCheckBox("Enable Button3");
		baseCheckBox.setName("Enable_Link3_Button");
		panel1.add(baseCheckBox);
		
		baseLabel = new JLabel("Area Label:");
		baseTextField = new JTextField(10);
		baseTextField.setToolTipText("<HTML>The area label displayed to the user, should be able to use html tags if you wish to change font</HTML>");
		baseTextField.setName("Link_Area_Label");
		panel1.add(baseLabel);
		panel1.add(baseTextField);

		panel2.setLayout(new FlowLayout(FlowLayout.CENTER));
		panel2a.setLayout(new GridLayout(3,1,0,5));
		panel2b.setLayout(new GridLayout(3,1));
		panel2.setBorder(BorderFactory.createTitledBorder("Icon Location and File Names (Size 30x30 or less)"));

		baseLabel = new JLabel("Link1 Icon:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>Icon size should be 30x30 or less </HTML>");
		baseTextField.setName("Link1_Icon");
		panel2a.add(baseLabel);
		panel2b.add(baseTextField);

		baseLabel = new JLabel("Link2 Icon:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML> Icon size should be 30x30 or less  </HTML>");
		baseTextField.setName("Link2_Icon");
		panel2a.add(baseLabel);
		panel2b.add(baseTextField);

		baseLabel = new JLabel("Link3 Icon:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML> Icon size should be 30x30 or less   </HTML>");
		baseTextField.setName("Link3_Icon");
		panel2a.add(baseLabel);
		panel2b.add(baseTextField);
		
		panel2.add(panel2a);
		panel2.add(panel2b);
		
		panel3.setLayout(new FlowLayout(FlowLayout.CENTER));
		panel3a.setLayout(new GridLayout(3,1,0,5));
		panel3b.setLayout(new GridLayout(3,1));
		panel3.setBorder(BorderFactory.createTitledBorder("Set URL destination for buttons"));

		baseLabel = new JLabel("Link1 URL:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>  </HTML>");
		baseTextField.setName("Link1_URL");
		panel3a.add(baseLabel);
		panel3b.add(baseTextField);

		baseLabel = new JLabel("Link2 URL:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>  </HTML>");
		baseTextField.setName("Link2_URL");
		panel3a.add(baseLabel);
		panel3b.add(baseTextField);

		baseLabel = new JLabel("Link3 URL:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>  </HTML>");
		baseTextField.setName("Link3_URL");
		panel3a.add(baseLabel);
		panel3b.add(baseTextField);
		
		panel3.add(panel3a);
		panel3.add(panel3b);
		
		panel4.setLayout(new FlowLayout(FlowLayout.CENTER));
		panel4a.setLayout(new GridLayout(1,1,0,5));
		panel4b.setLayout(new GridLayout(1,1));
		panel4.setBorder(BorderFactory.createTitledBorder("Rules Panel"));

		baseLabel = new JLabel("Rules Location:");
		baseLabel.setHorizontalAlignment(JLabel.RIGHT);
		baseTextField = new JTextField(30);
		baseTextField.setToolTipText("<HTML>Needs to be a basic HTML. Linking to remote host may slow client load time </HTML>");
		baseTextField.setName("Rules_Location");
		panel4a.add(baseLabel);
		panel4b.add(baseTextField);
		
		panel4.add(panel4a);
		panel4.add(panel4b);
		

		
		//panel5.add(panel5a);
		//panel5.add(panel5b);
		
		add(panel0);
		add(panel1);
		add(panel2);
		add(panel3);
		add(panel4);
		//add(panel5);

	}

	public LinksPanel() {
		super();
		init();
	}
}
