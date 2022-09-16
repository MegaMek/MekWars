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

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import client.MWClient;
import common.VerticalLayout;
import common.util.SpringLayoutHelper;

/**
 * @author jtighe
 * @author Spork
 */
public class PilotSkillsCardPanel extends JPanel {

	private static final long serialVersionUID = -841047215777559815L;
	private PilotSkillTextField baseTextField = null;
	
	public PilotSkillsCardPanel(MWClient mwclient) {
		super();
		
		JPanel topPanel = new JPanel();
		final JPanel bottomPanel = new JPanel();
		final CardLayout cl = new CardLayout();
		JPanel firstCard = new JPanel();
		JPanel secondCard = new JPanel();
        /*
         * PILOT SKILLS Panel
         */
        JPanel mekPilotSkillsSpring = new JPanel(new SpringLayout());
		JPanel vehiclePilotSkillsSpring = new JPanel(new SpringLayout());
        JPanel infantryPilotSkillsSpring = new JPanel(new SpringLayout());
        JPanel protomechPilotSkillsSpring = new JPanel(new SpringLayout());
        JPanel battlearmorPilotSkillsSpring = new JPanel(new SpringLayout());
        JPanel aeroPilotSkillsSpring = new JPanel(new SpringLayout());
        JPanel bannedWSWeaponsSpring = new JPanel(new SpringLayout());
        Dimension fieldSize = new Dimension(5, 10);
        
        PilotSkillsModPanel psModPanel = new PilotSkillsModPanel(mwclient);

        //JPanel mainSpring = new JPanel(new SpringLayout());

        /*
         * Meks
         */
        
        mekPilotSkillsSpring.add(new JLabel("Mek", SwingConstants.TRAILING));
        mekPilotSkillsSpring.add(new JLabel("Pilot Skills", SwingConstants.LEADING));
        
        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("DM", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain dodge maneuver. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain dodge maneuver</body></html>");
        }
        baseTextField.setName("chanceforDMforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("MS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain melee specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain melee specialist</body></html>");
        }
        baseTextField.setName("chanceforMSforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("PR", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain pain resistance. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain pain resistance</body></html>");
        }
        baseTextField.setName("chanceforPRforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("SV", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain survivalist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain survivalist</body></html>");
        }
        baseTextField.setName("chanceforSVforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("IM", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain iron man. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain iron man</body></html>");
        }
        baseTextField.setName("chanceforIMforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Astech. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Astech</body></html>");
        }
        baseTextField.setName("chanceforATforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/Ballistic. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/Ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain a trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain a trait</body></html>");
        }
        baseTextField.setName("chanceforTNforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("MT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Med Tech skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Med Tech skill</body></html>");
        }
        baseTextField.setName("chanceforMTforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("Edge", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Edge skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Edge skill</body></html>");
        }
        baseTextField.setName("chanceforEDforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("VDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforVDNIforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("BVDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the buffered VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the buffered VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforBVDNIforMek");
        mekPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        mekPilotSkillsSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Pain Shunt skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Pain Shunt skill</body></html>");
        }
        baseTextField.setName("chanceforPSforMek");
        mekPilotSkillsSpring.add(baseTextField);
        
        /*
         * Vehicles
         */
        vehiclePilotSkillsSpring.add(new JLabel("Vee", SwingConstants.TRAILING));
        vehiclePilotSkillsSpring.add(new JLabel("Crew Skills", SwingConstants.LEADING));

        baseTextField = new PilotSkillTextField(3);
        
        
        
        vehiclePilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforATforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist.  Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/ballistic. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain trait</body></html>");
        }
        baseTextField.setName("chanceforTNforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable skill</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("VDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforVDNIforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("BVDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the buffered VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the buffered VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforBVDNIforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        vehiclePilotSkillsSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Pain Shunt skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Pain Shunt skill</body></html>");
        }
        baseTextField.setName("chanceforPSforVehicle");
        vehiclePilotSkillsSpring.add(baseTextField);

        infantryPilotSkillsSpring.add(new JLabel("Inf", SwingConstants.TRAILING));
        infantryPilotSkillsSpring.add(new JLabel("Squad Skills", SwingConstants.LEADING));

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain astech. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain astech</body></html>");
        }
        baseTextField.setName("chanceforATforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/ballistic. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain a trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain a trait</body></html>");
        }
        baseTextField.setName("chanceforTNforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        infantryPilotSkillsSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Pain Shunt skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Pain Shunt skill</body></html>");
        }
        baseTextField.setName("chanceforPSforInfantry");
        infantryPilotSkillsSpring.add(baseTextField);

        protomechPilotSkillsSpring.add(new JLabel("Proto", SwingConstants.TRAILING));
        protomechPilotSkillsSpring.add(new JLabel("Pilot Skills", SwingConstants.LEADING));

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain astech. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain astech</body></html>");
        }
        baseTextField.setName("chanceforATforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/ballistic. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to a trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to a trait</body></html>");
        }
        baseTextField.setName("chanceforTNforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        protomechPilotSkillsSpring.add(new JLabel("MT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Med Tech Skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Med Tech Skill</body></html>");
        }
        baseTextField.setName("chanceforMTforProtoMek");
        protomechPilotSkillsSpring.add(baseTextField);

        battlearmorPilotSkillsSpring.add(new JLabel("BA", SwingConstants.TRAILING));
        battlearmorPilotSkillsSpring.add(new JLabel("Squad Skills", SwingConstants.LEADING));

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain astech. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain astech</body></html>");
        }
        baseTextField.setName("chanceforATforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/ballistic. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain a trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain a trait</body></html>");
        }
        baseTextField.setName("chanceforTNforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        battlearmorPilotSkillsSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Pain Shunt skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Pain Shunt skill</body></html>");
        }
        baseTextField.setName("chanceforPSforBattleArmor");
        battlearmorPilotSkillsSpring.add(baseTextField);

        aeroPilotSkillsSpring.add(new JLabel("Aero", SwingConstants.TRAILING));
        aeroPilotSkillsSpring.add(new JLabel("Pilot Skills", SwingConstants.LEADING));

        baseTextField = new PilotSkillTextField(3);
        
        
        baseTextField.setMinimumSize(fieldSize);
        aeroPilotSkillsSpring.add(new JLabel("MA", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain maneuvering ace. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain maneuvering ace</body></html>");
        }
        baseTextField.setName("chanceforMAforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("NAP", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Piloting. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Piloting</body></html>");
        }
        baseTextField.setName("chanceforNAPforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("NAG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Natural Aptitude Gunnery. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Natural Aptitude Gunnery</body></html>");
        }
        baseTextField.setName("chanceforNAGforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("AT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforATforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("TG", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain tactical genius. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain tactical genius</body></html>");
        }
        baseTextField.setName("chanceforTGforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("WS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain weapon specialist. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain weapon specialist</body></html>");
        }
        baseTextField.setName("chanceforWSforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("G/B", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/ballistic. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/ballistic</body></html>");
        }
        baseTextField.setName("chanceforGBforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("G/L", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/laser. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/laser</body></html>");
        }
        baseTextField.setName("chanceforGLforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("G/M", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gunnery/missile. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gunnery/missile</body></html>");
        }
        baseTextField.setName("chanceforGMforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("Trait", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain trait. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain trait</body></html>");
        }
        baseTextField.setName("chanceforTNforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("EI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Enhanced Interface. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Enhanced Interface</body></html>");
        }
        baseTextField.setName("chanceforEIforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("GT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain gifted. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain gifted</body></html>");
        }
        baseTextField.setName("chanceforGTforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("QS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain Quick Study. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain Quick Study</body></html>");
        }
        baseTextField.setName("chanceforQSforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("VDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforVDNIforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("BVDNI", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the buffered VDNI skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the buffered VDNI skill</body></html>");
        }
        baseTextField.setName("chanceforBVDNIforAero");
        aeroPilotSkillsSpring.add(baseTextField);

        baseTextField = new PilotSkillTextField(3);
        
        
        aeroPilotSkillsSpring.add(new JLabel("PS", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Pain Shunt skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Pain Shunt skill</body></html>");
        }
        baseTextField.setName("chanceforPSforAero");
        aeroPilotSkillsSpring.add(baseTextField);
        
        baseTextField = new PilotSkillTextField(3);
        aeroPilotSkillsSpring.add(new JLabel("MT", SwingConstants.TRAILING));
        if (Boolean.parseBoolean(mwclient.getserverConfigs("PlayersCanBuyPilotUpgrades"))) {
            baseTextField.setToolTipText("<html><body>Set cost for a pilot to gain the Med Tech skill. Zero to disable</body></html>");
        } else {
            baseTextField.setToolTipText("<html><body>Set Chance for a pilot to gain the Med Tech skill</body></html>");
        }
        baseTextField.setName("chanceforMTforAero");
        aeroPilotSkillsSpring.add(baseTextField);
        
        SpringLayoutHelper.setupSpringGrid(vehiclePilotSkillsSpring, 2);
        SpringLayoutHelper.setupSpringGrid(infantryPilotSkillsSpring, 2);
        SpringLayoutHelper.setupSpringGrid(protomechPilotSkillsSpring, 2);
        SpringLayoutHelper.setupSpringGrid(battlearmorPilotSkillsSpring, 2);
        SpringLayoutHelper.setupSpringGrid(aeroPilotSkillsSpring, 2);
        SpringLayoutHelper.setupSpringGrid(mekPilotSkillsSpring, 2);

        baseTextField = new PilotSkillTextField(50, new Dimension(50, 20));
        bannedWSWeaponsSpring.add(new JLabel("Banned WS Weapons:", SwingConstants.TRAILING));
        baseTextField.setToolTipText("<html><body>Add what Weapons you do not want pilots to get Weapon Specalist in/body></html>");
        baseTextField.setName("BannedWSWeapons");
        bannedWSWeaponsSpring.add(baseTextField);

        SpringLayoutHelper.setupSpringGrid(bannedWSWeaponsSpring, 2);

        // 1x5 grid
        JPanel skillGrid = new JPanel(new GridLayout(1, 6));

        skillGrid.add(mekPilotSkillsSpring);
        skillGrid.add(vehiclePilotSkillsSpring);
        skillGrid.add(infantryPilotSkillsSpring);
        skillGrid.add(protomechPilotSkillsSpring);
        skillGrid.add(battlearmorPilotSkillsSpring);
        skillGrid.add(aeroPilotSkillsSpring);
        
        firstCard.add(skillGrid);
        secondCard.add(bannedWSWeaponsSpring);
        secondCard.add(psModPanel);
        
        secondCard.setLayout(new VerticalLayout());
        
        bottomPanel.setLayout(cl);
        bottomPanel.add(firstCard, "1");
        bottomPanel.add(secondCard, "2");
        
        setLayout(new VerticalLayout());
        
        JButton skillsButton = new JButton("Skill Chances");
        skillsButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		cl.first(bottomPanel);
        	}
        });
        
        JButton otherButton = new JButton("Other");
        otherButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		cl.last(bottomPanel);
        	}
        });
        
        topPanel.add(skillsButton);
        topPanel.add(otherButton);
        topPanel.setBorder(BorderFactory.createEtchedBorder());

        //mainSpring.add(skillGrid);
        //mainSpring.add(bannedWSWeaponsSpring);
        //mainSpring.add(psModPanel);

        //SpringLayoutHelper.setupSpringGrid(mainSpring, 3, 1);

        //add(mainSpring);
        add(topPanel);
        add(bottomPanel);
	}

}