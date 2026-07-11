/*
 * Copyright (C) 2002, 2004 Josh Yockey
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekwars.common.gui.dialogs;


/*
 * Allows a user to sort through a list of MechSummaries and select one
 */

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serial;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import megamek.codeUtilities.MathUtility;
import megamek.common.units.Infantry;
import megamek.logging.MMLogger;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CPlayer;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.SpringLayoutHelper;

public class ArmyViewerDialog extends JDialog implements ActionListener, ListSelectionListener, ItemListener {
    public static final int AVD_DEFEND = 0;
    public static final int AVD_ATTACK = 1;
    public static final int AVD_ATTACK_FROM_RESERVE = 2;
    private static final MMLogger LOGGER = MMLogger.create(ArmyViewerDialog.class);
    @Serial
    private static final long serialVersionUID = -3851019509649287454L;
    private static final String SPACES = "                        ";
    private final DefaultListModel<String> defaultModel;
    private final JList<String> armyList;
    private final JButton bCancel = new JButton("Close");
    private final JButton bSelect = new JButton("Select");
    private final JTextArea armyView;
    private final JComboBox<String> teamBox = new JComboBox<>();
    private final CPlayer player;
    private final String opName;
    private final TreeSet<Integer> validArmyList = new TreeSet<>();
    private final IClient client;
    private final String planetName;
    private final String defenderName;
    private final int opID;
    private final int teamNumbers;
    private final int viewerMode;
    private int selectedArmyId = -1;

    public ArmyViewerDialog(IClient client, String opName, StringTokenizer validArmyList, int mode, String planet,
          String defender, int opID, int teamNumbers) {
        super(client.getMainFrame(), "Army Viewer", true);//dummy frame as owner

        //save params
        player = client.getPlayer();
        this.opName = opName;
        this.client = client;
        this.viewerMode = mode;
        this.planetName = planet;
        this.defenderName = defender;
        this.opID = opID;
        this.teamNumbers = teamNumbers;

        //We are defending and need to parse out which armies we can do that with!
        if (validArmyList != null) {
            while (validArmyList.hasMoreElements()) {
                this.validArmyList.add(MathUtility.parseInt(validArmyList.nextToken(), -1));
            }
        }

        teamBox.setPreferredSize(new Dimension(100, 22));
        teamBox.setMaximumSize(new Dimension(100, 22));
        teamBox.setMinimumSize(new Dimension(100, 22));

        if (teamNumbers > 1) {
            for (int team = 1; team <= teamNumbers; team++) {teamBox.addItem(String.format("Team #%s", team));}
        }

        //construct text boxes
        armyView = new JTextArea(15, 38);
        armyView.setAutoscrolls(true);
        //construct a model and list
        defaultModel = new DefaultListModel<>();
        armyList = new JList<>(defaultModel);

        ListSelectionModel listSelectionModel = armyList.getSelectionModel();
        armyList.setVisibleRowCount(5);
        listSelectionModel.addListSelectionListener(this);

        //place the list and text boxes in scroll panes
        JScrollPane listScrollPane = new JScrollPane(armyList);
        JScrollPane leftScrollPane = new JScrollPane(armyView);

        //set list/scroll options
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        leftScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        leftScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        armyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        //set fonts
        armyView.setFont(new Font("Monospaced", Font.PLAIN, 11));
        armyList.setFont(new Font("Monospaced", Font.PLAIN, 11));


        //panel w/ 1x3 SpringLayout for the mechView bits
        JPanel textBoxSpring = new JPanel(new SpringLayout());
        textBoxSpring.add(listScrollPane);
        textBoxSpring.add(leftScrollPane);
        SpringLayoutHelper.setupSpringGrid(textBoxSpring, 3);

        //set up a formatting holder for the cancel button
        JPanel buttonHolder = new JPanel();
        buttonHolder.add(bSelect);
        buttonHolder.add(bCancel);

        //set up the overall SpringLayout
        JPanel springHolder = new JPanel(new SpringLayout());

        if (this.teamNumbers > 1) {
            springHolder.add(teamBox);
            teamBox.setSelectedIndex(-1);
        }

        springHolder.add(textBoxSpring);
        springHolder.add(buttonHolder);
        SpringLayoutHelper.setupSpringGrid(springHolder, 1);
        this.getContentPane().add(springHolder);
        this.getRootPane().setDefaultButton(bSelect);

        clearArmyPreview();
        setSize(785, 560);
        setResizable(false);

        //add all the listeners
        armyList.addListSelectionListener(this);
        bCancel.addActionListener(this);
        bSelect.addActionListener(this);

        armyList.setSelectedIndex(-1);
        this.setModal(false);
        this.sortArmies();
        this.pack();
        this.setVisible(true);
        this.armyList.requestFocus();
    }

    void clearArmyPreview() {
        armyView.setEditable(false);
        armyView.setText("");

        //Remove preview image.
        previewArmy(-1);

    }

    private void sortArmies() {
        defaultModel.clear();
        int x = 0;

        if (opName != null) {
            for (CArmy currArmy : player.getArmies()) {
                //include only armies which can actually make an attack
                if (!currArmy.isDisabled() && currArmy.getLegalOperations().contains(opName)) {
                    defaultModel.add(x++, formatArmy(currArmy));
                }

            }
        } else {//Defend
            for (CArmy army : player.getArmies()) {
                if (!army.isDisabled() && this.validArmyList.contains(army.getID())) {
                    defaultModel.add(x++, formatArmy(army));
                }
            }
        }

        repaint();

        if (defaultModel.getSize() > 0) {
            armyList.setSelectedIndex(0);
        }
    }

    @Override
    public void setVisible(boolean show) {
        this.setLocationRelativeTo(null);
        super.setVisible(show);
        this.pack();
    }

    void previewArmy(int armyID) {
        armyView.setEditable(false);

        if (armyID > -1) {
            StringBuilder armyText = new StringBuilder();
            CArmy army = player.getArmy(armyID);
            for (Unit unit : army.getUnits()) {
                armyText.append(makeLength(String.format("#%s", unit.getId()), 7))
                      .append(" ")
                      .append(makeLength(((CUnit) unit).getModelName(), 12))
                      .append(" ");
                if (unit.getType() == Unit.VEHICLE || unit.getType() == Unit.MEK || unit.getType() == Unit.AERO) {
                    armyText.append(String.format(" (%s/%s)", unit.getPilot().getGunnery(), unit.getPilot().getPiloting()));
                } else if (unit.getType() == Unit.INFANTRY || unit.getType() == Unit.BATTLEARMOR) {
                    if (((Infantry) ((CUnit) unit).getEntity()).canMakeAntiMekAttacks()) {
                        armyText.append(String.format(" (%s/%s)", unit.getPilot().getGunnery(), unit.getPilot().getPiloting()));
                    } else {armyText.append(" (").append(unit.getPilot().getGunnery()).append(")");}
                } else {armyText.append(" (").append(unit.getPilot().getGunnery()).append(")");}
                armyText.append(" BV: ").append(((CUnit) unit).getBVForMatch()).append("\n");
            }
            armyView.setText(armyText.toString());
        } else {
            armyView.setText("No army selected");
        }
        armyView.setCaretPosition(0);

    }

    private String formatArmy(CArmy army) {
        return String.format("%s %s %s", makeLength(String.format("#%s", army.getID()), 3), makeLength(army.getName(),
              15), makeLength(String.format("BV: %s", army.getBV()), 10));
    }

    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return String.format("%s..", s.substring(0, nLength - 2));
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() == bCancel) {
            this.setVisible(false);
            selectedArmyId = -1;
        }

        if (ae.getSource() == bSelect) {
            try {

                if (armyList.getSelectedIndex() < 0) {
                    javax.swing.JOptionPane.showMessageDialog(this, "Select an army to defend with or click cancel");
                    return;
                }

                String armyID = armyList.getSelectedValue();

                armyID = armyID.substring(1, armyID.indexOf(" "));

                selectedArmyId = Integer.parseInt(armyID);

                if (viewerMode == AVD_DEFEND) {
                    int team = -1;

                    if (this.teamNumbers > 1) {
                        team = teamBox.getSelectedIndex();

                        if (team == -1) {
                            JOptionPane.showMessageDialog(this, "You must pick a Team!");
                            return;
                        } else {team++;}
                    }

                    client.sendChat(String.format("/c defend#%s#%s#%s", opID, armyID, team));
                } else if (viewerMode == AVD_ATTACK) {
                    client.sendChat(String.format("%sc attack#%s#%s#%s", IClient.CAMPAIGN_PREFIX, opName, armyID, planetName));
                } else {
                    client.sendChat(String.format("/c attackfromreserve#%s#%s#%s#%s", opName, armyID, planetName, defenderName));
                }
                this.setVisible(false);

            } catch (Exception ex) {
                LOGGER.error(ex, "Action Performed Error: {}", ex.getLocalizedMessage());
            }
        }// end unit selector if.
    }

    /**
     * for compliance with ListSelectionListener
     */
    public void valueChanged(ListSelectionEvent event) {

        int selected = armyList.getSelectedIndex();

        if (selected == -1) {
            clearArmyPreview();
            return;
        }

        String armyID = armyList.getSelectedValue();

        armyID = armyID.substring(1, armyID.indexOf(" "));
        previewArmy(Integer.parseInt(armyID));

    }

    public void itemStateChanged(ItemEvent itemEvent) {

        Object currSelection = armyList.getSelectedValue();

        armyList.setSelectedValue(currSelection, true);
    }

    public int getSelectedArmyID() {
        return selectedArmyId;
    }
}
