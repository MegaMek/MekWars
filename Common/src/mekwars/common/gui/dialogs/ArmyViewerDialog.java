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

/**
 * Modal-ish dialog that lists the current player's {@link CArmy} entries and lets the player
 * pick one to use for a campaign "Operation" action: attacking, attacking from reserve, or
 * defending. It is opened by the client whenever the server requires an army selection to proceed
 * with one of those actions (e.g. after the player initiates an attack or is asked to pick a
 * defending force).
 * <p>
 * The dialog shows a fixed-width text list of the player's eligible armies (see
 * {@link #formatArmy(CArmy)}) and, for the currently-selected army, a roster breakdown in
 * {@link #armyView}. Clicking "Select" resolves which army/team was chosen and sends the
 * appropriate campaign chat command to the server; clicking "Close" (or the window close button,
 * indirectly) cancels the selection. The caller retrieves the outcome afterwards via
 * {@link #getSelectedArmyID()}.
 * <p>
 * Note: although the constructor calls {@code super(..., true)} (modal), it later explicitly
 * calls {@link #setModal(boolean)} with {@code false} just before showing the dialog, so in
 * practice this dialog does <em>not</em> block the caller the way a typical modal dialog would.
 */
public class ArmyViewerDialog extends JDialog implements ActionListener, ListSelectionListener, ItemListener {
    /** Viewer mode: army is being picked to defend against an incoming attack. */
    public static final int AVD_DEFEND = 0;
    /** Viewer mode: army is being picked to attack with. */
    public static final int AVD_ATTACK = 1;
    /** Viewer mode: army is being picked to attack from reserve. */
    public static final int AVD_ATTACK_FROM_RESERVE = 2;
    private static final MMLogger LOGGER = MMLogger.create(ArmyViewerDialog.class);
    @Serial
    private static final long serialVersionUID = -3851019509649287454L;
    /** Padding used by {@link #makeLength(String, int)} to right-pad fixed-width list columns. */
    private static final String SPACES = "                        ";
    /** Backing model for {@link #armyList}; one formatted line per selectable army. */
    private final DefaultListModel<String> defaultModel;
    /** List widget showing the player's selectable armies (formatted via {@link #formatArmy(CArmy)}). */
    private final JList<String> armyList;
    private final JButton bCancel = new JButton("Close");
    private final JButton bSelect = new JButton("Select");
    /** Read-only roster preview of the currently-selected army (see {@link #previewArmy(int)}). */
    private final JTextArea armyView;
    /** Combo box for choosing a team number, shown only when {@link #teamNumbers} > 1 (defend mode). */
    private final JComboBox<String> teamBox = new JComboBox<>();
    /** The local player whose armies are being listed. */
    private final CPlayer player;
    /** Name of the operation being attacked; {@code null} when the dialog is used for defending. */
    private final String opName;
    /** IDs of armies the player is allowed to defend with, as supplied by the server (defend mode only). */
    private final TreeSet<Integer> validArmyList = new TreeSet<>();
    private final IClient client;
    /** Planet involved in the operation, forwarded back to the server on selection. */
    private final String planetName;
    /** Name of the defending force, used when attacking from reserve. */
    private final String defenderName;
    /** ID of the operation being acted upon. */
    private final int opID;
    /** Number of teams available to pick from; if &gt; 1, {@link #teamBox} is shown. */
    private final int teamNumbers;
    /** Which of {@link #AVD_DEFEND}, {@link #AVD_ATTACK}, or {@link #AVD_ATTACK_FROM_RESERVE} this dialog is running as. */
    private final int viewerMode;
    /** Result of the dialog: the chosen army's ID, or -1 if none was chosen / the dialog was cancelled. */
    private int selectedArmyId = -1;

    /**
     * Builds and immediately shows the army viewer dialog.
     *
     * @param client         connection used to read the local player's armies and send the resulting
     *                       campaign chat command
     * @param opName         name of the operation being attacked, or {@code null} if this dialog is
     *                       being used to pick a defending army instead
     * @param validArmyList  tokenized list of army IDs the player may defend with (only meaningful
     *                       when {@code opName} is {@code null}); may be {@code null}
     * @param mode           one of {@link #AVD_DEFEND}, {@link #AVD_ATTACK}, {@link #AVD_ATTACK_FROM_RESERVE}
     * @param planet         planet involved in the operation
     * @param defender       name of the defending force (used for attack-from-reserve)
     * @param opID           ID of the operation
     * @param teamNumbers    number of teams to choose from; if &gt; 1 a team selector is shown
     */
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
        //Each token is an army ID; invalid/unparseable tokens fall back to -1 (which will
        //simply never match a real army ID in sortArmies()).
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

    /** Blanks the army roster preview (equivalent to no army being selected). */
    void clearArmyPreview() {
        armyView.setEditable(false);
        armyView.setText("");

        //Remove preview image.
        previewArmy(-1);

    }

    /**
     * Populates {@link #armyList} with the player's eligible armies, filtering out disabled
     * armies and, depending on mode, either armies that can't legally perform {@link #opName}
     * (attack mode) or armies not present in {@link #validArmyList} (defend mode). Automatically
     * selects the first entry if the resulting list is non-empty.
     */
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

    /**
     * Overridden so every visibility change re-centers the dialog on screen and re-packs it;
     * as a side effect the dialog will always snap back to the center of the screen, even if a
     * caller repeatedly toggles visibility.
     */
    @Override
    public void setVisible(boolean show) {
        this.setLocationRelativeTo(null);
        super.setVisible(show);
        this.pack();
    }

    /**
     * Renders a roster breakdown of the given army into {@link #armyView}, one line per unit:
     * unit ID, model name, and either Gunnery/Piloting (Mek/Vehicle/Aero and anti-Mek-capable
     * Infantry/BattleArmor) or just Gunnery (everything else), followed by the unit's BV.
     *
     * @param armyID ID of the army to preview, or -1 to show "No army selected"
     */
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

    /**
     * Formats an army as a fixed-width "#ID Name BV: n" line for display in {@link #armyList}.
     * <p>
     * Note: the ID column always starts with {@code #} followed by the numeric ID; this exact
     * shape is relied upon elsewhere (see {@link #actionPerformed(ActionEvent)} and
     * {@link #valueChanged(ListSelectionEvent)}), which recover the army ID by taking the
     * substring between the {@code #} and the first space. Changing this format without updating
     * those call sites would break ID parsing.
     */
    private String formatArmy(CArmy army) {
        return String.format("%s %s %s", makeLength(String.format("#%s", army.getID()), 3), makeLength(army.getName(),
              15), makeLength(String.format("BV: %s", army.getBV()), 10));
    }

    /** Pads {@code s} with trailing spaces to exactly {@code nLength} characters, or truncates and appends ".." if longer. */
    private String makeLength(String s, int nLength) {
        if (s.length() == nLength) {
            return s;
        } else if (s.length() > nLength) {
            return String.format("%s..", s.substring(0, nLength - 2));
        } else {
            return s + SPACES.substring(0, nLength - s.length());
        }
    }

    /**
     * Handles the Close/Select buttons. Close cancels (hides the dialog, resets
     * {@link #selectedArmyId} to -1). Select parses the army ID out of the currently-highlighted
     * list entry, requires a team pick first if {@link #teamNumbers} &gt; 1 and mode is
     * {@link #AVD_DEFEND}, then sends the corresponding campaign chat command
     * ("defend", "attack", or "attackfromreserve") to the server and hides the dialog.
     */
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
     * ListSelectionListener callback: whenever the highlighted army in {@link #armyList} changes,
     * parses the army ID back out of the formatted list entry and refreshes {@link #armyView}
     * via {@link #previewArmy(int)}; clears the preview if nothing is selected.
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

    /**
     * ItemListener callback. {@code armyList} is a JList, not a combo box, so this doesn't
     * actually react to a meaningful item-change; it simply re-applies the current selection to
     * itself. Effectively a no-op / vestigial handler in its current form.
     */
    public void itemStateChanged(ItemEvent itemEvent) {

        Object currSelection = armyList.getSelectedValue();

        armyList.setSelectedValue(currSelection, true);
    }

    /**
     * @return the ID of the army the user selected, or -1 if the dialog was cancelled or no
     *         selection has been made yet
     */
    public int getSelectedArmyID() {
        return selectedArmyId;
    }
}
