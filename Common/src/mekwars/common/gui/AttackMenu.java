/*
 * Copyright (C) 2004 Helge Richter (McWizard)
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

package mekwars.common.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.MenuKeyListener;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.Planet;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.dialogs.ArmyViewerDialog;
import mekwars.common.gui.dialogs.PlanetNameDialog;
import mekwars.common.gui.dialogs.PlayerNameDialog;

/**
 * Create an "Attack" menu. Used in Map, CMainFramge, etc. to generate menus which show a player's currently available
 * operation options.
 * <p>
 * Unlike the old Task's dynamic menus, this is an actual class/object, not a static method which returns a contructed
 * menu. Also allows for clearing/updating.
 */

public class AttackMenu extends JMenu implements ActionListener {
    private static final MMLogger LOGGER = MMLogger.create(AttackMenu.class);

    /**
     * Serialization version identifier for this {@link JMenu}.
     */
    @Serial
    private static final long serialVersionUID = 7420602115238025725L;
    //Statics
    /*
     * Indexes into the per-operation {@code String[]} properties array returned by
     * {@link IClient#getAllOps()} (keyed by operation/game-type name). These describe an "Op" (a type of
     * attack/game a player can launch), as streamed from the server's OpList data. Note the indices are not
     * contiguous with index 2 (there is no {@code OP_*} constant for array slot 2) - whatever property lives
     * there is unused/unnamed by this class.
     */
    /** Index of the operation's maximum range property (distance an attacker may launch from). */
    private static final int OP_RANGE = 0;
    /** Index of the operation's menu-item color (HTML color name/hex) property. */
    private static final int OP_COLOR = 1;
    /** Index of the operation's "faction info" property: "none"/"only"/other, gating on target factory presence. */
    private static final int OP_FACTION_INFO = 3;
    /** Index of the operation's "home info" property: "none"/"only"/other, gating on target being a homeworld. */
    private static final int OP_HOME_INFO = 4;
    /** Index of the influence percentage at/above which the operation may be launched directly on the target. */
    private static final int OP_LAUNCH_ON = 5;
    /** Index of the influence percentage a friendly planet must have to be used as a launch point for the op. */
    private static final int OP_LAUNCH_FROM = 6;
    /** Index of the minimum-ownership percentage (of the target planet) required to use this operation. */
    private static final int OP_MIN_OWN = 7;
    /** Index of the maximum-ownership percentage (of the target planet) allowed to use this operation. */
    private static final int OP_MAX_OWN = 8;
    /** Index of the "$"-delimited list of house names allowed to defend against this operation. */
    private static final int OP_LEGAL_DEFENDERS = 9;
    /** Index of the "^"-delimited list of planet flags the target planet must have for the op to be allowed. */
    private static final int OP_ALLOW_ED_PLANET_FLAGS = 10;
    /** Index of the "^"-delimited list of planet flags that disqualify the target planet from this operation. */
    private static final int OP_DISALLOW_ED_PLANET_FLAGS = 11;
    /** Index of the boolean flag indicating this operation is only launchable from reserve status (AFR). */
    private static final int OP_AFR = 12;
    /** Index of the boolean flag indicating this operation is only launchable while the player is active. */
    private static final int OP_ACTIVE = 13;
    /** Index of the minimum sub-faction access level required to use this operation. */
    private static final int OP_ACCESS_LEVEL = 14;
    //VARS
    /** The client this menu queries for army/operation/planet data and through which chat commands are sent. */
    private final IClient client;
    /**
     * The army this menu was built for, or a negative value if it was built generically (e.g. from the main
     * menu bar or the star map) rather than for one specific army right-clicked in the HQ panel.
     */
    private int armyID;
    /**
     * The planet this menu's operations target, {@code "-1"} if unset/unspecified (in which case selecting an
     * operation will prompt the player to choose a planet), or {@code null} in some generic-menu construction
     * paths.
     */
    private String planetName;

    //CONSTRUCTOR
    /**
     * Creates an Attack menu for the given context.
     *
     * @param client     the client used to query available operations/armies/planets and send attack commands
     * @param armyID     the specific army this menu is for, or a negative number to build a generic menu covering
     *                   all of the player's armies (filtered by planet eligibility if {@code planetName} is given)
     * @param planetName the target planet name, {@code "-1"}/empty/{@code null} to defer planet selection to a
     *                   dialog shown when an operation is chosen
     */
    public AttackMenu(IClient client, int armyID, String planetName) {
        super("Attack");
        this.client = client;
        this.armyID = armyID;
        this.planetName = planetName;
    }

    /**
     * Builds a single {@link JMenuItem} for one operation, with its label formatted differently depending on
     * whether this is the compact popup-style menu ({@code fullMenu == false}, colored HTML text) or the full
     * "Games" menu bar entry ({@code fullMenu == true}, plain " - name" text prefixed by the "Attacks:" header
     * item added later in {@link #updateMenuItems(boolean)}). Despite the parameter name {@code settings}, only
     * the operation's color (a single value, not the full settings array) is passed in and used here.
     *
     * @param fullMenu whether this item is for the full "Games" menu (plain text) vs. the compact popup (colored)
     * @param currName the operation name to display
     * @param settings the operation's HTML color string (e.g. {@code "#FF0000"} or a color name)
     *
     * @return a new, unwired {@link JMenuItem} (the caller is responsible for attaching an action listener/command)
     */
    private static @org.jspecify.annotations.NonNull JMenuItem getJMenuItem(boolean fullMenu,
          String currName, String settings) {
        String color = settings;

        //we don't care about range here, but we do
        //want to take notice of longs and colors.
        String menuItemName;

        if (!fullMenu) {
            menuItemName = String.format("<html><font color=%s>%s</font></html>", color, currName);
        } else {
            menuItemName = String.format(" - %s", currName);
        }

        JMenuItem currItem = new JMenuItem(menuItemName);
        return currItem;
    }

    //METHODS
    /*
     * Most important method in the AttackMenu class. Rebuilds all of the
     * JMenuItems in the menu by getting army and ops data from the client.
     */
    /**
     * Clears and rebuilds this menu's items based on the current army/planet context and the operations data known
     * to the client. Behavior differs by construction context:
     * <ul>
     *   <li>If {@link #armyID} is negative (menu built generically, e.g. main menu bar or star map): computes the
     *   set of operations legal for <em>any</em> of the player's armies, then, if a specific {@link #planetName}
     *   was supplied, further filters that set down to operations that are actually eligible against that planet -
     *   checking sub-faction access level, legal-defender house list, factory/homeworld requirements, ownership
     *   percentage bounds (scaled by the planet's conquest points), required/disallowed planet flags, reserve-only
     *   ("AFR") exclusion, and finally range from any planet the player's house currently controls at or above the
     *   operation's launch-from influence threshold. If no operations are found eligible, a single disabled-looking
     *   "None" item is added instead.</li>
     *   <li>If {@link #armyID} is non-negative (menu built for one specific army, e.g. right-click in the HQ
     *   panel): simply lists that army's own legal operations (via {@link mekwars.common.campaign.CArmy#getLegalOperations()}),
     *   skipping any that are reserve-only ("AFR"), since these menus can only be used while the player is active.</li>
     * </ul>
     * Each resulting operation becomes a {@link JMenuItem} (built by {@link #getJMenuItem}) wired to fire
     * {@link #actionPerformed(ActionEvent)} with the operation name as its action command.
     * <p>
     * When {@code fullMenu} is {@code true}, this method also relabels the menu itself to "Games" (mnemonic 'G'),
     * inserts a non-interactive "Attacks:" header item at the top (its listeners are stripped immediately after
     * creation so it behaves as a label rather than a clickable entry), and appends fixed utility items: an
     * "Attack From Reserve" item (only if the server config {@code AllowAttackFromReserve} is enabled and the
     * player is currently in reserve status), "Check Access", "Games Status", and (only if the player is active or
     * beyond) "Cancel Game".
     * <p>
     * TODO (from the original source): operations are not currently sorted by color then name, which would let
     * server operators visually group similar game types together.
     *
     * @param fullMenu {@code true} to build the full "Games" menu (with header/utility items), {@code false} to
     *                 build just the compact list of attack options (e.g. for a popup menu)
     */
    public void updateMenuItems(boolean fullMenu) {

        //clear old items from this menu
        this.removeAll();
        TreeMap<String, String[]> allOps = client.getAllOps();

        /*
         * Army ID will be < 0 if the attack menu is being
         * generated by the MainFrame (Game menu) or the Map.
         *
         * The map will pass a planet name, in which case ops
         * should be filtered by range/factory eligibility, etc.
         */
        if (armyID < 0) {

            /*
             * Cross-reference available ShortOperations for CArmies with
             * complete OpList.txt streamed from the server. Filter out
             * out-of-range/factory ineligible operations if possible.
             */
            TreeSet<String> allEligible = new TreeSet<>();

            //no planet name. let the player select from all possible
            //choices, and his planet lists will be limited to valid
            //targets for the selected operation by PlanetNameDialog.
            if (planetName == null || planetName.trim().isEmpty() || planetName.equals("-1")) {
                for (CArmy currA : client.getPlayer().getArmies()) {
                    allEligible.addAll(currA.getLegalOperations());
                }
            }

            //else, filter out ops which can't reach/target the world
            else {

                //load the planet in question
                Planet tp = client.getData().getPlanetByName(planetName);

                //save the players house ID, which will be referenced frequently
                int houseID = client.getPlayer().getHouseFightingFor().getId();

                //Sub Faction Access Level;
                int accessLevel = client.getPlayer().getSubFactionAccess();

                //put all eligible into a temporary tree. this weeds out
                //duplicate entries and saves some loops through the planets.
                TreeSet<String> tempEligible = new TreeSet<>();
                for (CArmy currA : client.getPlayer().getArmies()) {
                    tempEligible.addAll(currA.getLegalOperations());
                }

                //loop through all armies and eligible
                for (String currOpName : tempEligible) {

                    String[] opProps = client.getAllOps().get(currOpName);

                    //load relevant properties, and the players house ID
                    double range = MathUtility.parseDouble(opProps[AttackMenu.OP_RANGE], 0.0);
                    String facInfo = opProps[AttackMenu.OP_FACTION_INFO];
                    String homeInfo = opProps[AttackMenu.OP_HOME_INFO];
                    int launchOn = MathUtility.parseInt(opProps[AttackMenu.OP_LAUNCH_ON], 0);
                    int launchFrom = MathUtility.parseInt(opProps[AttackMenu.OP_LAUNCH_FROM], 0);
                    double minOwn = MathUtility.parseDouble(opProps[AttackMenu.OP_MIN_OWN], 0.0);
                    double maxOwn = MathUtility.parseDouble(opProps[AttackMenu.OP_MAX_OWN], 0.0);
                    String legalDefenders = opProps[AttackMenu.OP_LEGAL_DEFENDERS];
                    String allowPlanetFlags = String.format("%s^", opProps[AttackMenu.OP_ALLOW_ED_PLANET_FLAGS]);
                    String disallowPlanetFlags = String.format("%s^", opProps[AttackMenu.OP_DISALLOW_ED_PLANET_FLAGS]);
                    boolean reserveOnly = MathUtility.parseBoolean(opProps[AttackMenu.OP_AFR], false);
                    int minAccessLevel = MathUtility.parseInt(opProps[AttackMenu.OP_ACCESS_LEVEL], 0);

                    //Your sub faction is not allowed to use this!
                    if (accessLevel < minAccessLevel) {
                        continue;
                    }

                    //only check for a legal defender limit if necessary
                    if (!legalDefenders.startsWith("allFactions")) {

                        TreeMap<String, Object> legalDefTree = new TreeMap<>();
                        StringTokenizer legalDefTokenizer = new StringTokenizer(legalDefenders, "$");

                        while (legalDefTokenizer.hasMoreTokens()) {
                            legalDefTree.put(legalDefTokenizer.nextToken(), null);
                        }

                        boolean foundDefender = false;
                        for (House currH : tp.getInfluence().getHouses()) {
                            if (legalDefTree.containsKey(currH.getName())) {
                                foundDefender = true;
                                break;
                            }
                        }

                        if (!foundDefender) {
                            continue;
                        }
                    }

                    //check the factory requirements
                    if (tp.getFactoryCount() > 0 && facInfo.equals("none")) {
                        continue;
                    } else if (tp.getFactoryCount() < 1 && facInfo.equals("only")) {
                        continue;
                    }

                    //check homeworld requirements
                    if (tp.isHomeWorld() && homeInfo.equals("none")) {
                        continue;
                    } else if (!tp.isHomeWorld() && homeInfo.equals("only")) {
                        continue;
                    }

                    // convert minOwn and maxOwn to a percentage
                    // wildj79 (James Allred) 2015-09-30
                    minOwn *= tp.getConquestPoints() /
                                    100.0D;   //Baruk 2015-11-7 modified to include planet CP in formula
                    maxOwn *= tp.getConquestPoints() /
                                    100.0D;   //Baruk 2015-11-7 modified to include planet CP in formula

                    //check the ownership requirements
                    if (tp.getInfluence().getInfluence(houseID) < minOwn) {
                        continue;
                    }

                    if (tp.getInfluence().getInfluence(houseID) > maxOwn) {
                        continue;
                    }

                    //check on-target launch
                    if (tp.getInfluence().getInfluence(houseID) >= launchOn) {
                        allEligible.add(currOpName);
                        continue;
                    }

                    //Check for allowed planet flags. the planet most have these flags.
                    if (!allowPlanetFlags.isEmpty()) {
                        boolean allowOp = true;
                        StringTokenizer stringTokenizer = new StringTokenizer(allowPlanetFlags, "^");
                        while (stringTokenizer.hasMoreTokens()) {
                            String key = stringTokenizer.nextToken();

                            if (key.trim().isEmpty()) {
                                continue;
                            }

                            if (!tp.getPlanetFlags().containsKey(key)) {
                                LOGGER.debug(String.format("%s does not have flag: %s", tp.getName(), key));
                                allowOp = false;
                                break;
                            }
                        }

                        if (!allowOp) {
                            continue;
                        }
                    }

                    //AFR games do not show up in the AttackMenu/Star Map Menu/HQ
                    if (reserveOnly) {
                        continue;
                    }

                    //Check for disallowed planet flags. If the planet has one of these flags
                    // The planet will not be allowed.
                    if (!disallowPlanetFlags.isEmpty()) {
                        boolean allowOp = true;
                        StringTokenizer stringTokenizer = new StringTokenizer(disallowPlanetFlags, "^");

                        while (stringTokenizer.hasMoreTokens()) {
                            String key = stringTokenizer.nextToken();

                            if (key.trim().isEmpty()) {
                                continue;
                            }

                            if (tp.getPlanetFlags().containsKey(key)) {
                                allowOp = false;
                                LOGGER.debug(String.format("%s has flag: %s", tp.getName(), key));
                                break;
                            }
                        }

                        if (!allowOp) {
                            continue;
                        }
                    }

                    //cant launch on world. see if the operation can be started
                    //from a planet the player's house currently owns.
                    for (Planet currP : client.getData().getAllPlanets()) {

                        //check to see if operation can reach target world from currP
                        if (currP.getInfluence().getInfluence(houseID) >= launchFrom) {
                            double tDist = currP.getPosition().distanceSq(tp.getPosition());

                            if (tDist <= range) {
                                allEligible.add(currOpName);
                                break;
                            }
                        }

                    }

                }
            }

            if (allEligible.isEmpty()) {
                JMenuItem filler = new JMenuItem("None");
                this.add(filler);
            }

            for (String currName : allEligible) {
                if (!allOps.containsKey(currName)) {
                    client.updateOpData(false);

                    if (checkAllOpsMenuItems(allOps, currName)) {continue;}
                }

                String[] settings = allOps.get(currName);
                JMenuItem currItem = getJMenuItem(fullMenu, currName, settings[AttackMenu.OP_COLOR]);
                currItem.addActionListener(this);
                currItem.setActionCommand(currName);
                this.add(currItem);
            }

        } else {

            /*
             * Else, this menu is being generated by the CHQPanel
             * when a player right-clicks on an army. Show only
             * the attacks which the army in question may make.
             */

            CArmy clickArmy = client.getPlayer().getArmy(armyID);

            if (clickArmy != null) {

                if (clickArmy.getLegalOperations().size() <= 0) {
                    JMenuItem filler = new JMenuItem("None");
                    this.add(filler);
                }

                for (String currName : clickArmy.getLegalOperations()) {

                    if (checkAllOpsMenuItems(allOps, currName)) {
                        continue;
                    }

                    String[] settings = allOps.get(currName);
                    /*
                     * Filter out games that are reserve-only. Attack menus in HQ, CMainFrame and
                     * on the map may only be used to start games when a player is active, so
                     */
                    if (Boolean.parseBoolean(settings[mekwars.common.gui.AttackMenu.OP_AFR])) {
                        continue;
                    }

                    JMenuItem currItem = getJMenuItem(fullMenu, currName, settings[AttackMenu.OP_COLOR]);
                    currItem.addActionListener(this);
                    currItem.setActionCommand(currName);
                    this.add(currItem);
                }
            }
        }

        /*
         * TODO: Sort the JMenuItems by colour, then by name. This
         *       would allow server operators to group similar games
         *       together.
         */

        if (fullMenu) {
            this.setText("Games");
            this.setMnemonic('G');

            // Build a plain, non-interactive "Attacks:" header item by stripping every listener a fresh
            // JMenuItem/JComponent normally has, so it behaves like a label rather than a clickable/focusable
            // menu entry.
            JMenuItem toAdd = new JMenuItem("Attacks:");
            MouseListener[] mouse = toAdd.getMouseListeners();
            FocusListener[] focus = toAdd.getFocusListeners();
            MenuKeyListener[] menuKey = toAdd.getMenuKeyListeners();
            PropertyChangeListener[] property = toAdd.getPropertyChangeListeners();

            for (MouseListener mouseListener : mouse) {
                toAdd.removeMouseListener(mouseListener);
            }

            for (FocusListener focusListener : focus) {
                toAdd.removeFocusListener(focusListener);
            }

            // Likely bug: this loop is sized to menuKey.length (the menu-key listeners) but removes from the
            // `focus` array a second time instead of calling removeMenuKeyListener(menuKey[i]); the menu-key
            // listeners captured above are therefore never actually removed, and if menuKey has more entries
            // than focus this would throw ArrayIndexOutOfBoundsException.
            for (int i = 0; i < menuKey.length; i++) {
                toAdd.removeFocusListener(focus[i]);
            }

            for (PropertyChangeListener propertyChangeListener : property) {
                toAdd.removePropertyChangeListener(propertyChangeListener);
            }

            this.add(toAdd, 0);

            if (Boolean.parseBoolean(client.getServerConfigs("AllowAttackFromReserve")) &&
                      client.getMyStatus() == IClient.STATUS_RESERVE) {
                this.add(new JSeparator());
                toAdd = new JMenuItem("Attack From Reserve");
                toAdd.addActionListener(this);
                toAdd.setActionCommand("cmdAttackFromReserve");
                this.add(toAdd);
            }

            this.add(new JSeparator());
            toAdd = new JMenuItem("Check Access");
            toAdd.addActionListener(this);
            toAdd.setActionCommand("cmdCheckAccess");
            this.add(toAdd);

            toAdd = new JMenuItem("Games Status");
            toAdd.addActionListener(this);
            toAdd.setActionCommand("cmdGamesStatus");
            toAdd.setMnemonic('G');
            this.add(toAdd);

            if (client.getMyStatus() >= IClient.STATUS_ACTIVE) {
                toAdd = new JMenuItem("Cancel Game");
                toAdd.addActionListener(this);
                toAdd.setActionCommand("cmdCancelGames");
                this.add(toAdd);
            }

        }
    }

    /**
     * Guards against building a menu item for an operation name that has no corresponding entry in {@code allOps}
     * (i.e. the client's cached operations data is out of sync with the legal-operations names reported by an
     * army/planet). Logs a debug message (including a dump of every key currently in {@code allOps}) when this
     * happens, but otherwise does not attempt to recover the missing data itself - the caller in
     * {@link #updateMenuItems(boolean)} is expected to skip that one operation (via {@code continue}) rather than
     * fail the whole menu build.
     *
     * @param allOps   the full operations map keyed by operation name, as returned by {@link IClient#getAllOps()}
     * @param currName the operation name to check for
     *
     * @return {@code true} if {@code currName} is missing from {@code allOps} (caller should skip it),
     *       {@code false} if it is present and safe to use
     */
    private boolean checkAllOpsMenuItems(TreeMap<String, String[]> allOps, String currName) {
        if (!allOps.containsKey(currName)) {
            LOGGER.debug(String.format("Error in updateMenuItems(): no _%s_ in allOps.", currName));

            StringBuilder allOpsList = new StringBuilder("allOps contains: ");

            for (String currO : allOps.keySet()) {
                allOpsList.append(currO).append(" ");
            }

            LOGGER.debug(allOpsList.toString());

            //don't stop building the list just because of one bad apple...
            return true;
        }

        return false;
    }

    /**
     * The actionCommand has to be of the following structure:
     * <p>
     * aid|pid *or* aid|pid|arg0|arg1|arg2...
     * <p>
     * "aid" is the ArmyID to use. If "-1", choice will be given. "pid" is the PlanetID to attack. If "-1", menu will
     * open.
     * <p>
     * cmd is the command and one of those:
     * <p>
     * if arg0 == "multi" arg1: max attackers arg2: min attackers arg3: max defenders arg4: min defenders
     * <p>
     * NOTE: the "aid|pid|..." description above does not match this method's actual implementation below (there is
     * no parsing of a pipe-delimited action command here) - it looks like it describes either an older version of
     * this class or the format of a related server command, and is left as-is since it may still be useful
     * historical/protocol context, but should not be relied on to understand the current code. The actual behavior
     * is:
     * <ul>
     *   <li>Fixed utility commands ("cmdCancelGames", "cmdCheckAccess", "cmdGamesStatus",
     *   "cmdAttackFromReserve") are dispatched by exact action-command match in the {@code switch} below, each
     *   sending an appropriate campaign chat command (see {@link IClient#sendChat}) or opening a picker dialog.</li>
     *   <li>Any other action command is treated as an operation/game-type name (as set by
     *   {@link #getJMenuItem}/{@link #updateMenuItems(boolean)}). Before acting on it, the player's status is
     *   checked: must be at least {@code STATUS_ACTIVE} and not {@code STATUS_FIGHTING}, otherwise an in-client
     *   chat message explains why the attack can't proceed and the method returns early.</li>
     *   <li>If {@link #planetName} is unset (i.e. {@code "-1"}), a {@link mekwars.common.gui.dialogs.PlanetNameDialog}
     *   is shown to let the player pick a target planet; if the player cancels/leaves it blank, the method returns
     *   without doing anything further.</li>
     *   <li>Finally, either an {@link ArmyViewerDialog} is opened (letting the player pick which army attacks, when
     *   {@link #armyID} is negative) or an attack chat command is sent directly for the specific army (when
     *   {@link #armyID} is already known).</li>
     * </ul>
     *
     * @param actionEvent the Swing action event carrying the action command to dispatch
     *
     * @see ActionListener#actionPerformed
     */
    public void actionPerformed(ActionEvent actionEvent) {

        String name = actionEvent.getActionCommand();

        switch (name) {
            case "cmdCancelGames" -> {
                client.sendChat(String.format("%sc terminate", IClient.CAMPAIGN_PREFIX));
                return;
            }


            /*
             * Check access is hacky and duplicative in some ways ...
             */
            case "cmdCheckAccess" -> {

                //if the player has no armies, tell him to make some ... *sigh*
                if (client.getPlayer().getArmies().size() <= 0) {
                    String toUser = "CH|CLIENT: It's impossible to check legality when you have no armies.";
                    client.doParseDataInput(toUser);
                    return;
                }

                //first, pick an army
                TreeSet<String> names = new TreeSet<>();
                for (CArmy currArmy : client.getPlayer().getArmies()) {
                    names.add(String.format("#%s - BV: %s", currArmy.getID(), currArmy.getBV()));
                }

                JComboBox<String> armyCombo = new JComboBox(names.toArray());
                armyCombo.setEditable(false);

                JComboBox<String> attackCombo = new JComboBox(client.getAllOps().keySet().toArray());
                attackCombo.setEditable(false);

                JPanel holderPanel = new JPanel();
                holderPanel.setLayout(new BoxLayout(holderPanel, BoxLayout.Y_AXIS));
                holderPanel.add(armyCombo);
                holderPanel.add(attackCombo);

                JOptionPane jop = new JOptionPane(holderPanel,
                      JOptionPane.QUESTION_MESSAGE,
                      JOptionPane.OK_CANCEL_OPTION);
                JDialog dlg = jop.createDialog(client.getMainFrame(), "Select army and attack type.");
                armyCombo.grabFocus();
                armyCombo.getEditor().selectAll();
                dlg.setVisible(true);

                if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {return;}

                String attackName = (String) attackCombo.getSelectedItem();
                String armyName = (String) armyCombo.getSelectedItem();
                // Note: this overwrites the AttackMenu's own armyID field (not just a local variable) with the
                // army chosen in this dialog, permanently changing which army later menu rebuilds/attacks will
                // apply to for the remainder of this menu instance's lifetime.
                if (armyName != null) {
                    armyID = Integer.parseInt(armyName.substring(1, armyName.indexOf(" ")).trim());
                }

                client.sendChat(String.format("%sc checkarmyeligibility#%s#%s", IClient.CAMPAIGN_PREFIX, armyID, attackName));
                return;
            }


            /*
             * Game status
             */
            case "cmdGamesStatus" -> {
                client.sendChat(String.format("%sc games", IClient.CAMPAIGN_PREFIX));
                return;
            }


            /*
             * Allows players to attack from reserve
             */
            case "cmdAttackFromReserve" -> {

                String planet;
                String target;
                String Op;
                TreeSet<String> allEligible = new TreeSet<>();

                for (CArmy currA : client.getPlayer().getArmies()) {
                    allEligible.addAll(currA.getLegalOperations());
                }

                /*
                 * Filter out games that are active-only
                 */
                Iterator<String> i = allEligible.iterator();
                while (i.hasNext()) {
                    String[] currProperties = client.getAllOps().get(i.next());
                    if (Boolean.parseBoolean(currProperties[AttackMenu.OP_ACTIVE]))//13 is active only
                    {i.remove();}
                }

                JComboBox<String> attackCombo = new JComboBox(allEligible.toArray());
                attackCombo.setEditable(false);
                JOptionPane jop = new JOptionPane(attackCombo,
                      JOptionPane.QUESTION_MESSAGE,
                      JOptionPane.OK_CANCEL_OPTION);
                JDialog dlg = jop.createDialog(client.getMainFrame(), "Select an operation.");
                dlg.setVisible(true);

                if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                Op = (String) attackCombo.getSelectedItem();

                String[] opProperties = client.getAllOps().get(Op);

                PlanetNameDialog planetDialog = new PlanetNameDialog(client, "Choose a planet", opProperties);
                planetDialog.setVisible(true);
                planet = planetDialog.getPlanetName();
                planetDialog.dispose();

                if (planet == null) {return;}

                PlayerNameDialog playerDialog = new PlayerNameDialog(client,
                      "Select an opponent",
                      PlayerNameDialog.ANY_PLAYER);
                playerDialog.setVisible(true);
                target = playerDialog.getPlayerName();
                playerDialog.dispose();

                if (target == null) {
                    return;
                }

                new ArmyViewerDialog(client,
                      Op,
                      null,
                      ArmyViewerDialog.AVD_ATTACK_FROM_RESERVE,
                      planet,
                      target,
                      -1,
                      -1);
                return;
            }
        }

        /*
         * Terminating is always a viable option; however,
         * anything else is a nonstarter unless the player
         * is actually active.
         */
        if (client.getMyStatus() < IClient.STATUS_ACTIVE) {
            String toUser = "CH|CLIENT: You must be active in order to initiate standard attacks.";
            client.doParseDataInput(toUser);
            return;
        } else if (client.getMyStatus() == IClient.STATUS_FIGHTING) {
            String toUser = "CH|CLIENT: You may not initiate an attack while you are in a game.";
            client.doParseDataInput(toUser);
            return;
        }

        //Check the planet name - if a number, open the menu.
        if (planetName.equals("-1")) {

            //load properties to send to the planet selection dialog
            String[] opProperties = client.getAllOps().get(name);

            PlanetNameDialog planetdialog = new PlanetNameDialog(client, "Select a planet to attack.", opProperties);
            planetdialog.setVisible(true);
            planetName = planetdialog.getPlanetName();
            planetdialog.dispose();

            if (planetName == null || planetName.trim().isEmpty()) {return;}
        }

        //check the army name/id
        if (armyID < 0) {
            new ArmyViewerDialog(client, name, null, ArmyViewerDialog.AVD_ATTACK, planetName, null, -1, -1);
        } else {
            client.sendChat(String.format("%sc attack#%s#%s#%s", IClient.CAMPAIGN_PREFIX, name, armyID, planetName));
        }
    }

}
