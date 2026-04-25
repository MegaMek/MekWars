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

import java.awt.GridLayout;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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
public class ArtilleryPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2029107013395017158L;
	private JTextField baseTextField = new JTextField(5);
	private JCheckBox BaseCheckBox = new JCheckBox();
	
	public ArtilleryPanel() {
		super();
        /*
         * ARTILLERY TAB CONSTRUCTION Enable autoassigned artillery, and set up loadout options.
         */
        JPanel artyBox = new JPanel();
        artyBox.setLayout(new BoxLayout(artyBox, BoxLayout.Y_AXIS));
        JPanel artyCBoxGrid = new JPanel(new GridLayout(1, 2));
        JPanel artySpring = new JPanel(new SpringLayout());
        JPanel gunEmplacementCBoxGrid = new JPanel(new GridLayout(1, 2));
        JPanel gunEmplacementSpring = new JPanel(new SpringLayout());

        // set up check boxes
        BaseCheckBox = new JCheckBox("Heavy First");

        BaseCheckBox.setToolTipText("If checked, server tries to assign assault pieces before light");
        BaseCheckBox.setName("HeaviestArtilleryFirst");
        artyCBoxGrid.add(BaseCheckBox);

        // set up the spring
        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Assault File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as assault artillery seperated by $");
        baseTextField.setName("AssaultArtilleryFile");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Heavy File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as heavy artillery seperated by $");
        baseTextField.setName("HeavyArtilleryFile");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Medium File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as medium artillery seperated by $");
        baseTextField.setName("MediumArtilleryFile");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Light File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as light artillery seperated by $");
        baseTextField.setName("LightArtilleryFile");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Max Assault:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of assault pieces the server will assign");
        baseTextField.setName("MaxAssaultArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Max Heavy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of heavy pieces the server will assign");
        baseTextField.setName("MaxHeavyArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Max Medium:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of medium pieces the server will assign");
        baseTextField.setName("MaxMediumArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Max Light:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of light pieces the server will assign");
        baseTextField.setName("MaxLightArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("BV for Assault:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when assault is assigned");
        baseTextField.setName("BVForAssaultArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("BV for Heavy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when heavy is assigned");
        baseTextField.setName("BVForHeavyArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("BV for Medium:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when medium is assigned");
        baseTextField.setName("BVForMediumArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("BV for Light:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when light is assigned");
        baseTextField.setName("BVForLightArtillery");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Distance:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Distance, in mapsheets, that Arty is deployed from a players home edge");
        baseTextField.setName("DistanceFromMap");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Artillery Over Run:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<HTML><BODY>The chance of an offboard unit getting over run.<BR>This is modified by the number of hexs the unit is off the board</HTML></BODY>");
        baseTextField.setName("ArtilleryOffBoardOverRun");
        artySpring.add(baseTextField);

        baseTextField = new JTextField(5);
        artySpring.add(new JLabel("Off Board Capture:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("The chance of a off board unit being salvaged.");
        baseTextField.setName("OffBoardChanceOfCapture");
        artySpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(artySpring, 8, 4);

        // set up check boxes
        BaseCheckBox = new JCheckBox("Heavy First");

        BaseCheckBox.setToolTipText("If checked, server tries to assign assault pieces before light");
        BaseCheckBox.setName("HeaviestGunEmplacementFirst");
        gunEmplacementCBoxGrid.add(BaseCheckBox);

        // set up the spring
        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Assault File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as assault Guns seperated by $");
        baseTextField.setName("AssaultGunEmplacementFile");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Heavy File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as heavy Guns seperated by $");
        baseTextField.setName("HeavyGunEmplacementFile");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Medium File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as medium Guns seperated by $");
        baseTextField.setName("MediumGunEmplacementFile");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Light File:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Filename of the units to load as light Guns seperated by $");
        baseTextField.setName("LightGunEmplacementFile");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Max Assault:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of assault pieces the server will assign");
        baseTextField.setName("MaxAssaultGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Max Heavy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of heavy pieces the server will assign");
        baseTextField.setName("MaxHeavyGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Max Medium:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of medium pieces the server will assign");
        baseTextField.setName("MaxMediumGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("Max Light:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("Max number of light pieces the server will assign");
        baseTextField.setName("MaxLightGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("BV for Assault:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when assault is assigned");
        baseTextField.setName("BVForAssaultGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("BV for Heavy:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when heavy is assigned");
        baseTextField.setName("BVForHeavyGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("BV for Medium:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when medium is assigned");
        baseTextField.setName("BVForMediumGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        baseTextField = new JTextField(5);
        gunEmplacementSpring.add(new JLabel("BV for Light:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("BV removed from task total (all players) when light is assigned");
        baseTextField.setName("BVForLightGunEmplacement");
        gunEmplacementSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(gunEmplacementSpring, 4);

        // finalize layout
        artyBox.add(artyCBoxGrid);
        artyBox.add(artySpring);

        artyBox.add(gunEmplacementCBoxGrid);
        artyBox.add(gunEmplacementSpring);

        add(artyBox);
	}

}