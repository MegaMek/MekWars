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

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import client.MWClient;
import common.VerticalLayout;

/**
 *
 * @author Salient - contains link area options options
 */
public class FreebuildPanel extends JPanel {

	private static final long serialVersionUID = -4626004177199981829L;

		private JTextField baseTextField = new JTextField(5);
		private JCheckBox baseCheckBox = new JCheckBox();

		public FreebuildPanel(MWClient mwclient) {
			super();

			setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));

			String description = "<HTML>Freebuild allows for several things. It can be used to allow SOL to pick whatever units<br>"
					+ " they want from either a defined build table OR all tables. Also post defection freebuild can be enabled.<br>"
					+ " This will allow for the player to build from the house table. A limit of how many free meks can be set.<br>"
					+ " For example, if you want to let the players hand pick their starting force, this will allow that.<br>";

			JPanel panel0 = new JPanel();
			JPanel panel1 = new JPanel();
			JPanel panel2 = new JPanel();
			JPanel panel3 = new JPanel();
			JPanel panel3a = new JPanel();
			JPanel panel3b = new JPanel();
			
//	        String fluName = mwclient.getserverConfigs("FluShortName");
//	        String rpName = mwclient.getserverConfigs("RPShortName");
//	        String cbName = mwclient.getserverConfigs("MoneyShortName");

	        panel0.setBorder(BorderFactory.createTitledBorder("Free Build"));
	        
	        panel0.add(new JLabel(description));

			panel1.setBorder(BorderFactory.createTitledBorder("SOL Free Build"));

			baseCheckBox = new JCheckBox("Allow Sol Free Build");
			baseCheckBox.setToolTipText("<HTML>Allows new players to create their own units based<br> on the build table of your choice, starting units should be set to 0</HTML>");
			baseCheckBox.setName("Sol_FreeBuild");
			panel1.add(baseCheckBox);

			baseCheckBox = new JCheckBox("Use All Tables");
			baseCheckBox.setToolTipText("<HTML>Lets Sol players build from all build tables instead of defining a table.</HTML>");
			baseCheckBox.setName("Sol_FreeBuild_UseAll");
			panel1.add(baseCheckBox);

			baseTextField = new JTextField(30);
			panel1.add(new JLabel("Build Table Name:", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>Set name of build table for new players to build units from.<br>default is Common, or you can create a new set of build tables if you'd like(ex Starter) <br>Starter_Light Starter_Medium and so on. Frequency is irrelavant, if creating a new build table, just set frequency to 1. </HTML>");
			baseTextField.setName("Sol_FreeBuild_BuildTable");
			panel1.add(baseTextField);

			baseTextField = new JTextField(5);
			panel1.add(new JLabel("Build Limit", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<html>How many units are players allowed to build. Zero or less disables limit.</html>");
			baseTextField.setName("FreeBuild_Limit");
			panel1.add(baseTextField);

			panel2.setBorder(BorderFactory.createTitledBorder("Post Defection Free Build"));

			baseCheckBox = new JCheckBox("Allow Free Build Post Defection");
			baseCheckBox.setToolTipText("<HTML>Set this if you want players to be able to build free units after defection. Uses house table.</HTML>");
			baseCheckBox.setName("FreeBuild_PostDefection");
			panel2.add(baseCheckBox);

			baseCheckBox = new JCheckBox("Limit Post Defection Only");
			baseCheckBox.setToolTipText("<HTML>Set this if you want SOL free build to work without limits, and only limit free build after defection</HTML>");
			baseCheckBox.setName("FreeBuild_LimitPostDefOnly");
			panel2.add(baseCheckBox);
			
			panel3.setBorder(BorderFactory.createTitledBorder("Misc Options"));
			panel3.setLayout(new VerticalLayout(5, VerticalLayout.CENTER, VerticalLayout.TOP));

			baseCheckBox = new JCheckBox("Enforce token usage before going Active");
			baseCheckBox.setToolTipText("<HTML>Set this if you want to disable going active if player has free meks remaining</HTML>");
			baseCheckBox.setName("FreeBuild_LimitGoActive");
			panel3a.add(baseCheckBox);
			
			baseCheckBox = new JCheckBox("Allow Dupes");
			baseCheckBox.setToolTipText("<HTML>Can only create 1 of each variant</HTML>");
			baseCheckBox.setName("FreeBuild_AllowDuplicates");
			panel3a.add(baseCheckBox);
			
			baseCheckBox = new JCheckBox("Use Dupe Limits");
			baseCheckBox.setToolTipText("<HTML>Set how many dupes are allowed</HTML>");
			baseCheckBox.setName("FreeBuild_DupeLimits");
			panel3a.add(baseCheckBox);
			
			panel3b.add(new JLabel("Dupe Limits -> ", SwingConstants.TRAILING));
			
			baseTextField = new JTextField(5);
			panel3b.add(new JLabel("Meks", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>(-1 for no limit) number of duplicate mek models that can be chosen using freebuild mektokens </HTML>");
			baseTextField.setName("FreeBuild_NumOfDuplicateMeks");
			panel3b.add(baseTextField);
			
			baseTextField = new JTextField(5);
			panel3b.add(new JLabel("Vees", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>(-1 for no limit) number of duplicate vee models that can be chosen using freebuild mektokens </HTML>");
			baseTextField.setName("FreeBuild_NumOfDuplicateVees");
			panel3b.add(baseTextField);
			
			baseTextField = new JTextField(5);
			panel3b.add(new JLabel("Inf", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>(-1 for no limit) number of duplicate inf models that can be chosen using freebuild mektokens </HTML>");
			baseTextField.setName("FreeBuild_NumOfDuplicateInf");
			panel3b.add(baseTextField);
			
			baseTextField = new JTextField(5);
			panel3b.add(new JLabel("BA", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>(-1 for no limit) number of duplicate BA models that can be chosen using freebuild mektokens </HTML>");
			baseTextField.setName("FreeBuild_NumOfDuplicateBA");
			panel3b.add(baseTextField);
			
			baseTextField = new JTextField(5);
			panel3b.add(new JLabel("Aero", SwingConstants.TRAILING));
			baseTextField.setToolTipText("<HTML>(-1 for no limit) number of duplicate Aero models that can be chosen using freebuild mektokens </HTML>");
			baseTextField.setName("FreeBuild_NumOfDuplicateAero");
			panel3b.add(baseTextField);
						
			panel3.add(panel3a);
			panel3.add(panel3b);
			
			add(panel0);
			add(panel1);
			add(panel2);
			add(panel3);

	}
}
