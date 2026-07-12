/*
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

package mekwars.common.gui.adapters;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.FilteredImageSource;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.event.MouseInputAdapter;

import megamek.codeUtilities.MathUtility;
import megamek.common.units.Entity;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import mekwars.common.Army;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CBMUnit;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.campaign.pilot.Pilot;
import mekwars.common.gui.AttackMenu;
import mekwars.common.gui.MWUnitDisplay;
import mekwars.common.gui.MekInfo;
import mekwars.common.gui.dialogs.AdvancedRepairDialog;
import mekwars.common.gui.dialogs.BulkRepairDialog;
import mekwars.common.gui.dialogs.CustomUnitDialog;
import mekwars.common.gui.dialogs.PromotePilotDialog;
import mekwars.common.gui.filters.AlphaFilter;
import mekwars.common.gui.panels.CHQPanel;
import mekwars.common.util.UnitUtils;
import org.jspecify.annotations.NonNull;

/**
 * Mouse/action handler attached to the unit ("Mek") table shown on the {@link CHQPanel} (the player's
 * hangar/HQ screen). This single class is responsible for three distinct jobs on that table:
 * <ol>
 *   <li><b>Drag-and-drop</b> of units between the hangar and the player's armies (lances), and repositioning
 *   units within an army, implemented via {@link #mousePressed}, {@link #mouseDragged} and
 *   {@link #mouseReleased}. A custom cursor image is swapped in during the drag to indicate whether the
 *   drop target would add, exchange, reposition, or reject the unit (see the cursor fields below).</li>
 *   <li><b>Right-click (popup trigger) context menu construction</b> via {@link #maybeShowPopup}, which
 *   dynamically builds a {@link JPopupMenu} whose contents depend on which table cell was clicked (an army
 *   sort-order header, an army/lance row, a unit cell inside an army, a hangar-only unit cell, or an empty
 *   hangar bay) and on the current unit/army/player state and server configuration (e.g. whether advanced
 *   repairs, the black market, or pilot queues are enabled).</li>
 *   <li><b>Dispatch of the resulting menu actions</b> via {@link #actionPerformed}, which decodes the
 *   pipe-delimited {@code actionCommand} string set on each {@link JMenuItem} and either calls a handler
 *   method on {@link CHQPanel}'s client/main frame, or sends a raw campaign chat command to the server via
 *   {@code chqPanel.getClient().sendChat(...)} using the {@code IClient.CAMPAIGN_PREFIX} protocol.</li>
 * </ol>
 * Double-clicking a unit cell (see {@link #mouseClicked}) instead opens a read-only {@link MWUnitDisplay}
 * window showing the unit's full record sheet.
 * <p>
 * This class holds no persistent server-side state; all mutations are performed by sending chat/campaign
 * commands to the server and relying on a subsequent GUI refresh to reflect the new state.
 */
public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {

    /** The HQ panel that owns the unit table this adapter is attached to; provides access to the client, player, and table model. */
    private final CHQPanel chqPanel;
    /** Custom drag cursor shown when dropping a dragged unit onto a different unit already in an army (swaps positions). */
    private final Cursor exchangeCursor;
    /** Custom drag cursor shown when dragging a unit to a new position within the same army. */
    private final Cursor positionCursor;
    /** Custom drag cursor shown when a drop would add the dragged unit to an army (target cell is empty). */
    private final Cursor addCursor;
    /** Custom drag cursor shown when a drop would remove the dragged unit from its army (dropped back on the hangar). */
    private final Cursor removeCursor;
    /** Custom drag cursor shown when the current drop target is not a legal destination for the drag (e.g. wrong player status). */
    private final Cursor notAllowedCursor;
    /** Custom drag cursor shown when the dragged unit already exists elsewhere in the target army (duplicate not allowed). */
    private final Cursor dupeCursor;
    /** Custom drag cursor shown when the target army has already reached its allowed unit-in-multiple-armies limit. */
    private final Cursor maxCursor;
    // VARS
    /** True while a left-button drag of a unit is in progress (set in {@link #mousePressed}, cleared in {@link #mouseReleased}). */
    private boolean isDrag;
    /** Snapshot image of the dragged unit's table cell rendering, drawn under the cursor while dragging. */
    private Image dragImage;
    /** Screen rectangle currently occupied by {@link #dragImage}; repainted to erase the drag image on each move/release. */
    private Rectangle2D dragRect;
    /** Fixed pixel offset from the mouse position used to position {@link #dragImage} (currently a hard-coded re-centering value, not a true grab offset). */
    private Point offset;
    /** The unit currently being dragged, captured on {@link #mousePressed}; null if no drag is active. */
    private CUnit dragUnit = null;
    /** The army (or null for the hangar) the drag began from, captured on {@link #mousePressed}. */
    private CArmy startArmy = null;
    /** The army (or null for the hangar) currently under the mouse during a drag; recomputed continuously in {@link #mouseDragged}/{@link #mouseReleased}. */
    private CArmy currArmy = null;

    // CONSTRUCTOR
    /**
     * Creates the adapter for a given HQ panel and pre-loads the custom drag cursors from the
     * {@code ./data/images/} asset directory (add/remove/exchange/position/not-allowed/duplicate/max icons).
     * These cursors are swapped onto the Mek table during drag-and-drop in {@link #mouseDragged} to give the
     * user visual feedback about the outcome of dropping at the current location.
     *
     * @param chqPanel the HQ panel whose Mek table this adapter will listen to and build popup menus for
     */
    public MekTableMouseAdapter(CHQPanel chqPanel) {
        super();
        this.chqPanel = chqPanel;

        Image plusI = Toolkit.getDefaultToolkit().createImage("./data/images/hqadd.gif");
        Image minusI = Toolkit.getDefaultToolkit().createImage("./data/images/hqremove.gif");
        Image exchangeI = Toolkit.getDefaultToolkit().createImage("./data/images/hqexchange.gif");
        Image positionI = Toolkit.getDefaultToolkit().createImage("./data/images/hqposition.gif");
        Image notAllowedI = Toolkit.getDefaultToolkit().createImage("./data/images/hqnotallowed.gif");
        Image dupeI = Toolkit.getDefaultToolkit().createImage("./data/images/hqdouble.gif");
        Image maxI = Toolkit.getDefaultToolkit().createImage("./data/images/hqmax.gif");
        addCursor = Toolkit.getDefaultToolkit().createCustomCursor(plusI, new Point(0, 0), "add_cursor");
        removeCursor = Toolkit.getDefaultToolkit().createCustomCursor(minusI, new Point(0, 0), "remove_cursor");
        exchangeCursor = Toolkit.getDefaultToolkit().createCustomCursor(exchangeI, new Point(0, 0), "exchange_cursor");
        positionCursor = Toolkit.getDefaultToolkit().createCustomCursor(positionI, new Point(0, 0), "position_cursor");
        notAllowedCursor = Toolkit.getDefaultToolkit()
                                 .createCustomCursor(notAllowedI, new Point(0, 0), "no_allowed_cursor");
        dupeCursor = Toolkit.getDefaultToolkit().createCustomCursor(dupeI, new Point(0, 0), "dupe_cursor");
        maxCursor = Toolkit.getDefaultToolkit().createCustomCursor(maxI, new Point(0, 0), "max_cursor");
    }

    /**
     * Handles double-clicks on the Mek table: opens a read-only {@link MWUnitDisplay} record-sheet window
     * for the unit under the cursor. Single clicks (and any click on an empty cell) do nothing but trigger a
     * repaint of the table. Note this method does not check {@link MouseEvent#isPopupTrigger()}; the
     * context-menu logic lives separately in {@link #maybeShowPopup}, invoked from {@link #mousePressed}/
     * {@link #mouseReleased}.
     *
     * @param mouseEvent the originating Swing mouse event
     */
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
            int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
            CUnit mek = chqPanel.getMekTable().getMekAt(row, col);

            if (mek != null) {
                JFrame infoWindow = new JFrame();
                MWUnitDisplay unitDisplay = new MWUnitDisplay(null, chqPanel.getClient());
                Entity theEntity = mek.getEntity();
                theEntity.loadAllWeapons();
                infoWindow.getContentPane().add(unitDisplay);
                infoWindow.setSize(300, 400);
                infoWindow.setResizable(false);
                infoWindow.setTitle(mek.getModelName());
                infoWindow.setLocationRelativeTo(null);
                infoWindow.setVisible(true);
                unitDisplay.displayEntity(theEntity);
            }
        }
        chqPanel.getTableMeks().repaint();
    }

    // METHODS
    /**
     * Handles mouse-button-down on the Mek table. Two independent things can happen here:
     * <ol>
     *   <li>If the press was with the left mouse button on a cell containing a unit, drag state is armed
     *   ({@link #isDrag} = true): the origin unit/army are captured, and a semi-transparent snapshot image
     *   of that cell's rendering (obtained by invoking the table's own cell renderer, a {@link MekInfo}) is
     *   prepared for use as the drag-under image in {@link #mouseDragged}.</li>
     *   <li>Regardless of button, {@link #maybeShowPopup} is called so that on platforms where the popup
     *   trigger fires on press (e.g. most non-Windows look-and-feels) the right-click context menu appears
     *   immediately.</li>
     * </ol>
     *
     * @param mouseEvent the originating Swing mouse event
     */
    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
        int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
        dragUnit = chqPanel.getMekTable().getMekAt(row, col);
        startArmy = chqPanel.getMekTable().getArmyAt(row);

        if ((dragUnit != null) && (mouseEvent.getButton() == MouseEvent.BUTTON1)) {
            // make isDrag true and save origins
            isDrag = true;

            // determine the offset
            offset = new Point(28, 22);// TODO: Make this a real offset,
            // not a simple re-centering.

            // Get a MekInfo image from the table cell renderer. The
            // renderer sets entity, camo, etc. as part of a normal drawing.
            MekInfo unitImage = (MekInfo) chqPanel.getTableMeks().getCellRenderer(row, col)
                                                .getTableCellRendererComponent(chqPanel.getTableMeks(),
                                                      null,
                                                      false,
                                                      false,
                                                      row,
                                                      col);

            // save the image, drawn from MekInfo, to use as a drag
            // under-image
            dragImage = unitImage.getEmbeddedImage();
            dragRect = new Rectangle2D.Float();
            dragRect.setRect(mouseEvent.getX(), mouseEvent.getY(), 84, 72);

            // give the image some alpha
            AlphaFilter aFilter = new AlphaFilter(95);
            dragImage = Toolkit.getDefaultToolkit()
                              .createImage(new FilteredImageSource(dragImage.getSource(), aFilter));
        }

        /*
         * and ... check to see if this should trigger a popup.
         */
        maybeShowPopup(mouseEvent);
    }

    /**
     * Private method called on click and release. Checks to see if a mouse event should open a contextual menu (right
     * click, OS X control+click, etc.) and shows a popup menu if appropriate.
     * <p>
     * When {@link MouseEvent#isPopupTrigger()} is true, the table cell under the pointer determines which of
     * five distinct menus gets built and shown (each branch below constructs its own {@link JPopupMenu}
     * contents and immediately calls {@code popup.show(...)} at the end of its branch):
     * <ol>
     *   <li>Column 0, on a row below the army rows: the hangar's "Sort (1st/2nd/3rd)" column-header menu,
     *   letting the player choose the hangar unit sort criteria.</li>
     *   <li>Row &lt; 0, or column 0 on an army row: the army/lance context menu (Attack Options, Check
     *   Access, Limits, Force Size To Face, Lock/Unlock/Remove/Rename/Disable/Enable Army, army sort order,
     *   Show To Faction, and the large "Request Match" challenge-message submenu tree).</li>
     *   <li>Row within the army rows, on a unit column: the in-army unit context menu (Add/Exchange/Position
     *   unit, C3 link/unlink, View/Customize Unit, Auto Eject toggle, Set/Remove Commander).</li>
     *   <li>Otherwise (a hangar-only unit cell, below the army rows): the hangar unit context menu (View/
     *   Customize Unit, Repairs submenu, Auto Eject toggle, Maintain/Unmaintained, RePod, Transactions
     *   submenu, Pilot submenu, Show To Faction, Remove From All).</li>
     *   <li>An empty hangar bay cell (only the first free bay): "Sell Excess Bays" (advanced repairs) or
     *   "Fire Excess Techs" (simple repairs).</li>
     * </ol>
     * Every {@link JMenuItem} built here has {@code this} added as its {@link ActionListener} and an
     * {@code actionCommand} string encoding, pipe-delimited, the command mnemonic dispatched by
     * {@link #actionPerformed}.
     *
     * @param mouseEvent the originating Swing mouse event, tested for {@link MouseEvent#isPopupTrigger()}
     */
    private void maybeShowPopup(MouseEvent mouseEvent) {
        JPopupMenu popup = new JPopupMenu();
        if (mouseEvent.isPopupTrigger()) {
            int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
            int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
            JMenuItem menuItem;

            // --- Branch 1: hangar column-header cell -> hangar sort-order submenus ("PHQS"/"SHQS"/"THQS" commands) ---
            if ((col == 0) && (row >= chqPanel.getMekTable().getRowsForArmies())) {
                JMenu primeSortMenu = new JMenu("Sort (1st)");
                JMenu secondarySortMenu = new JMenu("Sort (2nd)");
                JMenu tertiarySortMenu = new JMenu("Sort (3rd)");

                popup.add(primeSortMenu);
                popup.add(secondarySortMenu);
                popup.add(tertiarySortMenu);

                // Choices [note - this array must be duplicated in
                // CPlayer's sortHangar()]
                String[] choices = { "Name", "Battle Value", "Gunnery Skill", "ID Number", "MP (Jumping)",
                                     "MP (Walking)", "Pilot Kills", "Unit Type", "Weight (Class)", "Weight (Tons)",
                                     "No Sort" };

                // indicate current selections w/ Italics
                String menuName;
                // boolean selectionFound = true;

                // prime sort menu construction
                for (int i = 0; i < choices.length; i++) {

                    menuName = choices[i];
                    if (chqPanel.getClient().getConfigParam("PRIMARY_HQ_SORT_ORDER").equals(choices[i])) {
                        menuName = String.format("<HTML><i>%s</i></HTML>", menuName);
                        // selectionFound = false;
                    }
                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(String.format("PHQS|%s", choices[i]));
                    menuItem.addActionListener(this);
                    primeSortMenu.add(menuItem);

                    if ((i + 2) == choices.length) {
                        primeSortMenu.addSeparator();
                    }
                }

                // secondary sort menu construction
                for (int i = 0; i < choices.length; i++) {

                    menuName = choices[i];

                    if (chqPanel.getClient().getConfigParam("SECONDARY_HQ_SORT_ORDER").equals(choices[i])) {
                        menuName = String.format("<HTML><i>%s</i></HTML>", menuName);
                        // selectionFound = false;
                    }

                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(String.format("SHQS|%s", choices[i]));
                    menuItem.addActionListener(this);
                    secondarySortMenu.add(menuItem);

                    if ((i + 2) == choices.length) {
                        secondarySortMenu.addSeparator();
                    }
                }

                // tertiary sort menu construction
                for (int i = 0; i < choices.length; i++) {

                    menuName = choices[i];

                    if (chqPanel.getClient().getConfigParam("TERTIARY_HQ_SORT_ORDER").equals(choices[i])) {
                        menuName = String.format("<HTML><i>%s</i></HTML>", menuName);
                    }

                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(String.format("THQS|%s", choices[i]));
                    menuItem.addActionListener(this);
                    tertiarySortMenu.add(menuItem);

                    if ((i + 2) == choices.length) {
                        tertiarySortMenu.addSeparator();
                    }
                }

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());

            // --- Branch 2: click was over an army/lance row (row header or negative row) -> army context menu ---
            } else if ((row < 0) || (col == 0)) {
                CArmy l = chqPanel.getMekTable().getArmyAt(row);
                if (l != null) {

                    int lid = l.getID();
                    // The following block (Attack Options / Check Access / Limits / Force Size To Face /
                    // per-operation Attack submenu) is only shown when the army has a non-zero total BV,
                    // i.e. it actually contains units that could fight.
                    if (l.getBV() > 0) {

                        // "Attack Options" opens the commander's attack-check dialog for this army; disabled
                        // unless the player is STATUS_ACTIVE, unless the server config "ProbeInReserve" allows
                        // checking eligibility while in reserve.
                        menuItem = new JMenuItem("Attack Options");
                        menuItem.setActionCommand(String.format("AO|%s", lid));
                        menuItem.addActionListener(this);
                        boolean canCheckFromReserve = MathUtility.parseBoolean(chqPanel.getClient()
                                                                                     .getServerConfigs("ProbeInReserve"),
                              false);
                        if ((chqPanel.getClient().getMyStatus() != IClient.STATUS_ACTIVE) && !canCheckFromReserve) {
                            menuItem.setEnabled(false);
                        }
                        popup.add(menuItem);

                        // "Check Access" prompts (via CAA handler) for an operation name and asks the server
                        // whether this army is eligible to participate in it.
                        menuItem = new JMenuItem("Check Access");
                        menuItem.setActionCommand(String.format("CAA|%s", lid));
                        menuItem.addActionListener(this);
                        popup.add(menuItem);

                        // only show "Limits" option if limits allowed
                        // "Limits" submenu (Set Lower/Upper Unit Limit) only appears when the server config
                        // "AllowLimiters" is enabled; lets the player cap how many units this army can field.
                        boolean limitsAllowed = MathUtility.parseBoolean(chqPanel.getClient()
                                                                               .getServerConfigs("AllowLimiters"),
                              false);
                        if (limitsAllowed) {
                            JMenu limitMenu = new JMenu("Limits");
                            popup.add(limitMenu);

                            menuItem = new JMenuItem("Set Lower Unit Limit");
                            menuItem.setActionCommand(String.format("SLUL|%s", lid));
                            menuItem.addActionListener(this);
                            limitMenu.add(menuItem);

                            menuItem = new JMenuItem("Set Upper Unit Limit");
                            menuItem.setActionCommand(String.format("SUUL|%s", lid));
                            menuItem.addActionListener(this);
                            limitMenu.add(menuItem);
                        }

                        // "Force Size To Face" is only shown under the "Operations" force-size rule; lets
                        // the player declare the expected opposing force size (used to scale BV/force mods).
                        // Only show when Force Size is used.
                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseOperationsRule"),
                              false)) {
                            menuItem = new JMenuItem("Force Size To Face");
                            popup.add(menuItem);
                            menuItem.setActionCommand(String.format("SFS|%s", lid));
                            menuItem.addActionListener(this);
                        }

                        // Per-operation "attack" submenu (checks/starts a match for a specific ruleset op).
                        AttackMenu aMenu = new AttackMenu(chqPanel.getClient(), lid, "-1");
                        aMenu.updateMenuItems(false);
                        popup.add(aMenu);

                        popup.addSeparator();
                    }

                    // The Lock/Unlock Army items are always both added, but only one is ever visible: whichever
                    // state (locked/unlocked) is NOT the army's current state is hidden via setVisible(false)
                    // immediately below its add() call.
                    menuItem = new JMenuItem("Lock Army");
                    menuItem.setActionCommand(String.format("LA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (chqPanel.getClient().getPlayer().getArmy(lid).isPlayerLocked()) {
                        menuItem.setVisible(false);
                    }

                    menuItem = new JMenuItem("Unlock Army");
                    menuItem.setActionCommand(String.format("ULA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (!chqPanel.getClient().getPlayer().getArmy(lid).isPlayerLocked()) {
                        menuItem.setVisible(false);
                    }

                    // Deletes the army outright (after confirmation in the main-frame handler).
                    menuItem = new JMenuItem("Remove Army");
                    menuItem.setActionCommand(String.format("RA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Prompts for a new name for this army.
                    menuItem = new JMenuItem("Rename Army");
                    menuItem.setActionCommand(String.format("NA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // As with Lock/Unlock above, Disable/Enable Army are both added but only one is left
                    // visible depending on current disabled state. NOTE: both items share the SAME action
                    // command ("DAA|<lid>"), so both dispatch to the identical DAA case in actionPerformed
                    // (jMenuCommanderDisableArmy_actionPerformed), which must itself toggle the state.
                    menuItem = new JMenuItem("Disable Army");
                    menuItem.setActionCommand(String.format("DAA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (chqPanel.getClient().getPlayer().getArmy(lid).isDisabled()) {
                        menuItem.setVisible(false);
                    }

                    menuItem = new JMenuItem("Enable Army");
                    menuItem.setActionCommand(String.format("DAA|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (!chqPanel.getClient().getPlayer().getArmy(lid).isDisabled()) {
                        menuItem.setVisible(false);
                    }

                    // Army-level "Sort (1st)" menu: choose the primary criterion armies are listed by.
                    // NOTE: unlike the hangar sort menu built above (which has 1st/2nd/3rd), only a primary
                    // sort submenu is constructed here; the "SAS"/"TAS" (secondary/tertiary army sort) cases
                    // handled in actionPerformed have no corresponding menu item and are effectively dead
                    // code from the UI's perspective.
                    JMenu primeSortMenu = new JMenu("Sort (1st)");

                    popup.add(primeSortMenu);

                    // Choices [note - this array must be duplicated in
                    String[] choices = { "Name", "Battle Value", "ID Number", "Max Tonnage", "Avg Walk MP",
                                         "Avg Jump MP", "Number Of Units", "No Sort" };

                    // indicate current selections w/ Italics
                    String menuName;

                    // prime sort menu construction: build one item per sort choice, italicizing (via inline
                    // HTML) whichever choice matches the player's current PRIMARY_ARMY_SORT_ORDER config.
                    for (int i = 0; i < choices.length; i++) {
                        menuName = choices[i];
                        if (chqPanel.getClient()
                                  .getConfigParam("PRIMARY_ARMY_SORT_ORDER")
                                  .equalsIgnoreCase(choices[i])) {
                            menuName = String.format("<HTML><i>%s</i></HTML>", menuName);
                        }

                        menuItem = new JMenuItem(menuName);
                        menuItem.setActionCommand(String.format("PAS|%s", choices[i]));
                        menuItem.addActionListener(this);
                        primeSortMenu.add(menuItem);

                        if ((i + 2) == choices.length) {
                            primeSortMenu.addSeparator();
                        }
                    }

                    // reset selectionFound

                    popup.addSeparator();

                    // "Show To Faction" broadcasts this army's roster to the player's faction chat channel
                    // (server command "sth#a#<armyId>"); disabled when the army has no units.
                    menuItem = new JMenuItem("Show To Faction");
                    menuItem.setActionCommand(String.format("SATH|%s", lid));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // disable show olfaction if army has 0 units
                    if (chqPanel.getClient().getPlayer().getArmy(lid).getUnits().isEmpty()) {
                        menuItem.setEnabled(false);
                    }

                    CArmy army = chqPanel.getClient().getPlayer().getArmy(lid);

                    /*
                     * "Request Match" builds a large tree of pre-formatted chat challenge messages ("Looking
                     * for a game at ..."), all dispatched through the single "MPC" (Make Public Challenge)
                     * action command in the form "MPC|<mode>|<armyId or -1>|<operationName or 'none'>". The
                     * numeric mode (1-13) selects which stats get embedded in the outgoing chat text -- BV
                     * only, unit count + BV, unit weight-class breakdown, total tonnage, unit type counts,
                     * per-unit hyperlinked model names, etc. (see the MPC case in actionPerformed for exactly
                     * what each mode formats). The tree is split into two top-level branches:
                     *   - "This Army" (singleArmy): one leaf per mode/operation combo, scoped to this army's
                     *     ID (lid), plus a leaf per legal operation reported by CArmy.getLegalOperations().
                     *   - "All Armies" (allArmies): the same modes but with armyId=-1, meaning actionPerformed
                     *     iterates and sends a challenge message for every one of the player's armies.
                     * Both branches are disabled entirely if this army currently has no units.
                     */
                    JMenu challengeMenu = new JMenu("Request Match");
                    popup.add(challengeMenu);

                    JMenu allArmies = new JMenu("All Armies");
                    JMenu singleArmy = new JMenu("This Army");

                    challengeMenu.add(singleArmy);
                    challengeMenu.add(allArmies);

                    // disable if army has 0 units
                    if (chqPanel.getClient().getPlayer().getArmy(lid).getUnits().isEmpty()) {
                        challengeMenu.setEnabled(false);
                    }

                    // Mode 1: "BV Only" -- one "None" (no operation filter) entry plus one entry per legal operation.
                    JMenu submenu = new JMenu("Unit");

                    JMenu requestMenu = new JMenu("BV Only");

                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|1|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String op : army.getLegalOperations()) {
                        menuItem = new JMenuItem(op);
                        menuItem.setActionCommand(String.format("MPC|1|%s|%s", lid, op));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Count and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|2|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(String.format("MPC|2|%s|%s", lid, operation));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Classes and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|3|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(String.format("MPC|3|%s|%s", lid, operation));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Total Weight");
                    requestMenu = new JMenu("Total Weight");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|4|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(String.format("MPC|4|%s|%s", lid, operation));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|5|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(String.format("MPC|5|%s|%s", lid, operation));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight and Unit Count");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|6|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(String.format("MPC|6|%s|%s", lid, operation));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight, Unit Count and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|7|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|7|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Unit Types");
                    requestMenu = new JMenu("Unit Types");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|8|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|8|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Types with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|9|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|9|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Unit Models");
                    requestMenu = new JMenu("Unit Models");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|10|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|10|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Models with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|11|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|11|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Actual Weight");
                    requestMenu = new JMenu("Actual Unit Weights");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|12|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|12|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Actual Unit Weights with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(String.format("MPC|13|%s|none", lid));
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(String.format("MPC|13|%s|%s", lid, operations));
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    // --- "All Armies" branch: same MPC modes, but with operation always "none" and
                    // armyId hard-coded to -1 so actionPerformed's MPC case loops over every army the
                    // player owns instead of scoping to a single lid. ---
                    submenu = new JMenu("Unit");

                    menuItem = new JMenuItem("BV Only");
                    // All armies so set the lid to -1;
                    menuItem.setActionCommand("MPC|1|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Unit Count and BV");
                    menuItem.setActionCommand("MPC|2|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Unit Classes and BV");
                    menuItem.setActionCommand("MPC|3|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    allArmies.add(submenu);

                    submenu = new JMenu("Total Weight");
                    menuItem = new JMenuItem("Total Weight");
                    menuItem.setActionCommand("MPC|4|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Total Weight with BV");
                    menuItem.setActionCommand("MPC|5|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Total Weight and Unit Count");
                    menuItem.setActionCommand("MPC|6|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Total Weight, Unit Count and BV");
                    menuItem.setActionCommand("MPC|7|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    allArmies.add(submenu);

                    submenu = new JMenu("Unit Types");
                    menuItem = new JMenuItem("Unit Types");
                    menuItem.setActionCommand("MPC|8|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Unit Types with BV");
                    menuItem.setActionCommand("MPC|9|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    allArmies.add(submenu);

                    submenu = new JMenu("Unit Models");
                    menuItem = new JMenuItem("Unit Models");
                    menuItem.setActionCommand("MPC|10|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Unit Models with BV");
                    menuItem.setActionCommand("MPC|11|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    allArmies.add(submenu);

                    submenu = new JMenu("Actual Weight");
                    menuItem = new JMenuItem("Actual Unit Weights");
                    menuItem.setActionCommand("MPC|12|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);

                    menuItem = new JMenuItem("Actual Unit Weights with BV");
                    menuItem.setActionCommand("MPC|13|-1|none");
                    menuItem.addActionListener(this);
                    submenu.add(menuItem);
                    allArmies.add(submenu);

                }

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            // --- Branch 3: click was on a unit column within one of the army rows -> in-army unit menu ---
            } else if (row < chqPanel.getMekTable().getRowsForArmies()) {
                CUnit cUnit;
                CArmy cArmy = chqPanel.getMekTable().getArmyAt(row);
                int mid;
                int lid = cArmy.getID();
                cUnit = chqPanel.getMekTable().getMekAt(row, col);
                boolean hasUnitsFree = false;

                /*
                 * CONSTRUCT the ADD menu here. It will be added to the actual format later. @urgru 12/7/04
                 *
                 * "Add" lets the player add an eligible hangar unit into this army/lance. Only built when the
                 * hangar is non-empty and the army isn't locked. Eligible hangar units exclude those that are
                 * unmaintained, for sale, already in this army, or that have already hit the
                 * "UnitsInMultipleArmiesAmount" cap for how many armies a single unit may simultaneously
                 * belong to. Each eligible unit becomes one "EXM|<lid>|-1|<hangarUnitId>" menu item (the "-1"
                 * signals "no unit is being replaced, just add"), grouped into per-weight-class (and
                 * ProtoMek/Infantry) submenus, further split into chunks of 10 if a class has more than 10
                 * candidates. The whole Add menu is disabled if no eligible units were found (hasUnitsFree).
                 */
                JMenu addMenu = new JMenu("Add");
                if ((!chqPanel.getClient().getPlayer().getHangar().isEmpty()) && !cArmy.isLocked()) {
                    Vector<CUnit> mekArray = chqPanel.getClient().getPlayer().getHangar();
                    if (!mekArray.isEmpty()) {
                        Vector<Vector<JMenuItem>> SubMenus = new Vector<>(1, 1);

                        /*
                         * 6 entries Weights: 0-3 ProtoMek: 4 Infantry: 5
                         */
                        for (int i = 0; i < 6; i++) {
                            SubMenus.add(new Vector<>(1, 1));
                        }

                        for (CUnit element : mekArray) {
                            if ((element.getStatus() == Unit.STATUS_UNMAINTAINED) ||
                                      (element.getStatus() == Unit.STATUS_FOR_SALE)) {
                                continue;
                            }

                            if (chqPanel.getPlayer().getAmountOfTimesUnitExistsInArmies(element.getId()) >=
                                      Integer.parseInt(chqPanel.getClient()
                                                             .getServerConfigs("UnitsInMultipleArmiesAmount"))) {
                                continue;
                            }
                            if (cArmy.getUnit(element.getId()) == null) {// only add
                                // if unit isn't already in army
                                hasUnitsFree = true;
                                if ((element.getType() == Unit.MEK) ||
                                          (element.getType() == Unit.VEHICLE) ||
                                          (element.getType() == Unit.AERO)) {
                                    menuItem = new JMenuItem(String.format("%s (%s/%s) %s BV", element.getModelName(), element.getPilot()
                                                                                                     .getGunnery(), element.getPilot()
                                                                                                                            .getPiloting(), element.getBVForMatch()));
                                } else if ((element.getType() == Unit.INFANTRY) ||
                                                 (element.getType() == Unit.BATTLEARMOR)) {
                                    if (((Infantry) element.getEntity()).canMakeAntiMekAttacks()) {
                                        menuItem = new JMenuItem(String.format("%s (%s/%s) %s BV", element.getModelName(), element.getPilot()
                                                                                                         .getGunnery(), element.getPilot()
                                                                                                                                .getPiloting(), element.getBVForMatch()));
                                    } else {
                                        menuItem = new JMenuItem(String.format("%s (%s) %s BV", element.getModelName(), element.getPilot()
                                                                                                         .getGunnery(), element.getBVForMatch()));
                                    }
                                } else {
                                    menuItem = new JMenuItem(String.format("%s (%s) %s BV", element.getModelName(), element.getPilot()
                                                                                                     .getGunnery(), element.getBVForMatch()));
                                }
                                menuItem.setActionCommand(String.format("EXM|%s|-1|%s", lid, element.getId()));
                                menuItem.addActionListener(this);

                                if (element.getType() == Unit.PROTOMEK) {
                                    SubMenus.elementAt(4).add(menuItem);// into
                                    // proto
                                    // slot
                                } else if ((element.getType() == Unit.INFANTRY) ||
                                                 (element.getType() == Unit.BATTLEARMOR)) {
                                    SubMenus.elementAt(5).add(menuItem);// into
                                    // BA
                                    // slot
                                } else {// else, sort by weight class
                                    int size = element.getWeightClass();
                                    SubMenus.elementAt(size).add(menuItem);
                                }
                            }
                        }
                        for (int i = 0; i < SubMenus.size(); i++) {
                            Vector<JMenuItem> SizeMenu = SubMenus.elementAt(i);
                            if (SizeMenu.size() > 10) {
                                // More than one menu of the given size
                                // class is needed
                                int iterations = (SizeMenu.size() / 10) + 1;
                                for (int j = 0; j < iterations; j++) {
                                    int mekCount = 0;

                                    JMenu menuX = getMenuX(i, j);

                                    while (!SizeMenu.isEmpty() && (mekCount < 10)) {
                                        menuX.add(SizeMenu.elementAt(0));
                                        SizeMenu.removeElementAt(0);
                                        SizeMenu.trimToSize();
                                        mekCount++;
                                    }

                                    // if adding proto or infantry menu,
                                    // check previous elements
                                    // to see if a divider should be added
                                    if (i >= 4) {
                                        Component[] components = addMenu.getMenuComponents();
                                        if ((i == 4) && (components.length != 0)) {
                                            addMenu.addSeparator();
                                        } else if (i == 5) {
                                            boolean hasProtoMenu = isProtoMenu(components);
                                            if (!hasProtoMenu &&
                                                      (components.length > 0) &&
                                                      (menuX.getComponentCount() > 0)) {
                                                addMenu.addSeparator();
                                            }
                                        }
                                    }

                                    addMenu.add(menuX);
                                }
                            } else {// Only one menu for the given size
                                // class is needed

                                JMenu menuX;
                                if (i < 4) {
                                    menuX = new JMenu(Unit.getWeightClassDesc(i));
                                } else if (i == 4) {// proto
                                    menuX = new JMenu("Proto");
                                } else {// BA, can assume i = 5.
                                    menuX = new JMenu("Infantry");
                                }

                                // if adding proto or infantry menu, check
                                // previous elements
                                // to see if a divider should be added
                                boolean hasProtoMenu = false;
                                Component[] components = addMenu.getMenuComponents();
                                if (i == 5) {
                                    for (Component currComponent : components) {
                                        if (currComponent instanceof JMenu currMenu) {
                                            if (currMenu.getText().startsWith("Proto")) {
                                                hasProtoMenu = true;
                                            }
                                        }
                                    }
                                }

                                for (int j = 0; j < SizeMenu.size(); j++) {
                                    menuX.add(SizeMenu.elementAt(j));

                                    if ((i == 5) && (j == 0) && !hasProtoMenu && (components.length > 0)) {
                                        addMenu.addSeparator();
                                    } else if ((i == 4) && (j == 0) && (components.length > 0)) {
                                        addMenu.addSeparator();
                                    }

                                    addMenu.add(menuX);
                                }
                            }
                        }
                    }

                    // disable the menu if there are no units to add
                    addMenu.setEnabled(hasUnitsFree);

                }// end ADD menu construction

                // if the unit isn't null, include remove/show/etc
                if (cUnit != null) {

                    /*
                     * the unit isn't null, so construct the link menu here. It will be added to the actual format later. @Torren 12/19/04
                     *
                     * "Link" offers C3/C3i network masters this unit could join. It scans every other unit in
                     * the army for a compatible, non-full C3 (standard/master) or C3i network relative to
                     * cUnit's own C3 level, and adds one "LCN|<lid>|<thisUnitId>|<masterUnitId>" item per
                     * candidate master found. The menu itself is only shown later if cUnit actually has some
                     * C3 equipment (see the `if (cUnit.getC3Level() != Unit.C3_NONE)` check further below).
                     */
                    JMenu linkMenu = new JMenu("Link");
                    if ((!cArmy.getUnits().isEmpty()) && !cArmy.isLocked()) {
                        Vector<CUnit> Masters = new Vector<>(1, 1);
                        Enumeration<Unit> c3M = cArmy.getUnits().elements();
                        while (c3M.hasMoreElements()) {
                            CUnit c3Unit = (CUnit) c3M.nextElement();
                            if (c3Unit.equals(cUnit)) {
                                continue;
                            }

                            if (cUnit.getC3Level() != Unit.C3_IMPROVED) {
                                if (((c3Unit.getC3Level() == Unit.C3_MASTER) ||
                                           (c3Unit.getC3Level() == Unit.C3M_MASTER)) &&
                                          c3Unit.checkC3mNetworkHasOpen(cArmy, cUnit.getC3Level())) {
                                    Masters.add(c3Unit);
                                }
                            } else if (cUnit.getC3Level() == Unit.C3_IMPROVED) {
                                if ((c3Unit.getC3Level() == Unit.C3_IMPROVED) && c3Unit.checkC3iNetworkHasOpen(cArmy)) {
                                    Masters.add(c3Unit);
                                }
                            }
                        }
                        for (int i = 0; i < Masters.size(); i++) {
                            CUnit mm = Masters.elementAt(i);
                            if (cArmy.getUnit(mm.getId()) != null) {
                                menuItem = new JMenuItem(String.format("%s %s BV", mm.getModelName(), mm.getBVForMatch()));
                                menuItem.setActionCommand(String.format("LCN|%s|%s|%s", lid, cUnit.getId(), mm.getId()));
                                menuItem.addActionListener(this);
                                linkMenu.add(menuItem);
                            }
                        }
                    }// end Link menu construction

                    // Link menu has been preformed. Proceed with the usual
                    // bits.
                    mid = cUnit.getId();

                    // "Move To Hangar" pulls this unit out of the army back into the general hangar pool
                    // (only offered while the army is unlocked). Reuses the "EXM" exchange command with
                    // hangarUnitId=-1, meaning "no hangar unit takes its place."
                    if (!cArmy.isLocked()) {
                        String text = "Move To Hangar";
                        menuItem = new JMenuItem(text);
                        menuItem.setActionCommand(String.format("MH|%s|%s", lid, mid));
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    /*
                     * EXCHANGE. Derived from ADD. Same, but returns clicked unit to hangar.
                     *
                     * Builds the same weight-class/ProtoMek/Infantry-grouped submenu tree as the "Add" menu
                     * above (same eligibility filters: not unmaintained/for-sale, not already in this army,
                     * under the multi-army cap), but each item's action command is "EXM|<lid>|<clickedUnitId>|
                     * <hangarUnitId>" -- i.e. the currently-clicked in-army unit (cUnit) is swapped out to the
                     * hangar and replaced by the chosen hangar unit, in one operation.
                     */
                    if ((!chqPanel.getClient().getPlayer().getHangar().isEmpty()) && !cArmy.isLocked()) {
                        JMenu jm = new JMenu("Exchange");
                        popup.add(jm);
                        Vector<CUnit> meks = chqPanel.getClient().getPlayer().getHangar();
                        if (!meks.isEmpty()) {
                            Vector<Vector<JMenuItem>> SubMenus = new Vector<>();

                            /*
                             * 6 entries Weights: 0-3 ProtoMek: 4 Infantry: 5
                             */
                            for (int i = 0; i < 6; i++) {
                                SubMenus.add(new Vector<>(1, 1));
                            }

                            for (CUnit mek : meks) {
                                if ((mek.getStatus() == Unit.STATUS_UNMAINTAINED) ||
                                          (mek.getStatus() == Unit.STATUS_FOR_SALE)) {
                                    continue;
                                }
                                if (chqPanel.getPlayer().getAmountOfTimesUnitExistsInArmies(mek.getId()) >=
                                          MathUtility.parseInt(chqPanel.getClient().getServerConfigs(
                                                "UnitsInMultipleArmiesAmount"), 0)) {
                                    continue;
                                }
                                if (cArmy.getUnit(mek.getId()) == null) {
                                    // only allow exchange if unit isn't already in army
                                    if ((mek.getType() == Unit.MEK) ||
                                              (mek.getType() == Unit.VEHICLE) ||
                                              (mek.getType() == Unit.AERO)) {
                                        menuItem = new JMenuItem(String.format("%s (%s/%s) %s BV", mek.getModelName(), mek.getPilot()
                                                                                                     .getGunnery(), mek.getPilot()
                                                                                                                            .getPiloting(), mek.getBVForMatch()));
                                    } else if ((mek.getType() == Unit.INFANTRY) ||
                                                     (mek.getType() == Unit.BATTLEARMOR)) {
                                        if (((Infantry) mek.getEntity()).canMakeAntiMekAttacks()) {
                                            menuItem = new JMenuItem(String.format("%s (%s/%s) %s BV", mek.getModelName(), mek.getPilot()
                                                                                                         .getGunnery(), mek.getPilot()
                                                                                                                                .getPiloting(), mek.getBVForMatch()));
                                        } else {
                                            menuItem = new JMenuItem(String.format("%s (%s) %s BV", mek.getModelName(), mek.getPilot()
                                                                                                         .getGunnery(), mek.getBVForMatch()));
                                        }
                                    } else {
                                        menuItem = new JMenuItem(String.format("%s (%s) %s BV", mek.getModelName(), mek.getPilot()
                                                                                                     .getGunnery(), mek.getBVForMatch()));
                                    }
                                    menuItem.setActionCommand(String.format("EXM|%s|%s|%s", lid, cUnit.getId(), mek.getId()));
                                    menuItem.addActionListener(this);

                                    if (mek.getType() == Unit.PROTOMEK) {
                                        SubMenus.elementAt(4).add(menuItem);// into
                                        // proto
                                        // slot
                                    } else if ((mek.getType() == Unit.INFANTRY) ||
                                                     (mek.getType() == Unit.BATTLEARMOR)) {
                                        SubMenus.elementAt(5).add(menuItem);// into
                                        // BA
                                        // slot
                                    } else {// else, sort by weight class
                                        int size = mek.getWeightClass();
                                        SubMenus.elementAt(size).add(menuItem);
                                    }
                                }
                            }
                            for (int i = 0; i < SubMenus.size(); i++) {
                                Vector<JMenuItem> SizeMenu = SubMenus.elementAt(i);
                                JMenu menuX;
                                if (SizeMenu.size() > 10) {
                                    // More than one menu of the given size
                                    // class is needed
                                    int iterations = (SizeMenu.size() / 10) + 1;
                                    for (int j = 0; j < iterations; j++) {
                                        int mekCount = 0;

                                        if (i < 4) {
                                            menuX = new JMenu(String.format("%s %s", Unit.getWeightClassDesc(i), j + 1));
                                        } else if (i == 4) {// proto
                                            menuX = new JMenu(String.format("Proto %s", j + 1));
                                        } else {// BA, assume an i of 5
                                            menuX = new JMenu(String.format("Infantry %s", j + 1));
                                        }

                                        while (!SizeMenu.isEmpty() && (mekCount < 10)) {
                                            menuX.add(SizeMenu.elementAt(0));
                                            SizeMenu.removeElementAt(0);
                                            SizeMenu.trimToSize();
                                            mekCount++;
                                        }

                                        // if adding proto or infantry menu,
                                        // check previous elements
                                        // to see if a divider should be
                                        // added
                                        if (i >= 4) {

                                            Component[] components = addMenu.getMenuComponents();
                                            if ((i == 4) && (components.length != 0)) {
                                                jm.addSeparator();
                                            } else if (i == 5) {
                                                boolean hasProtoMenu = isProtoMenu(components);
                                                if (!hasProtoMenu &&
                                                          (components.length > 0) &&
                                                          (menuX.getComponentCount() > 0)) {
                                                    jm.addSeparator();
                                                }
                                            }
                                        }

                                        jm.add(menuX);
                                    }
                                } else {// Only one menu for the given size
                                    // class is needed

                                    if (i < 4) {
                                        menuX = new JMenu(Unit.getWeightClassDesc(i));
                                    } else if (i == 4) {// proto
                                        menuX = new JMenu("Proto");
                                    } else {// BA, assume an i of 5.
                                        menuX = new JMenu("Infantry");
                                    }

                                    // if adding proto or infantry menu,
                                    // check previous elements
                                    // to see if a divider should be added
                                    boolean hasProtoMenu = false;
                                    Component[] components = jm.getMenuComponents();
                                    if (i == 5) {
                                        for (Component currComponent : components) {
                                            if (currComponent instanceof JMenu currMenu) {
                                                if (currMenu.getText().startsWith("Proto")) {
                                                    hasProtoMenu = true;
                                                }
                                            }
                                        }
                                    }

                                    for (int j = 0; j < SizeMenu.size(); j++) {
                                        menuX.add(SizeMenu.elementAt(j));

                                        if ((i == 5) && (j == 0) && !hasProtoMenu && (components.length > 0)) {
                                            jm.addSeparator();
                                        } else if ((i == 4) && (j == 0) && (components.length > 0)) {
                                            jm.addSeparator();
                                        }

                                        jm.add(menuX);
                                    }
                                }

                            }
                        }

                        // hasUnitsFree is set during add menu creation but
                        // applies equally to the Exchange menu.
                        jm.setEnabled(hasUnitsFree);

                        /*
                         * The ADD menu, constructed previously
                         */
                        popup.add(addMenu);

                        /*
                         * The POSITION menu. Moves units around -within- the army. Only shown if there are enough units to warrant movement (>1).
                         * One "Move to #N" item per other unit slot in the army (skipping the clicked unit's
                         * own slot); dispatches "RPU|<lid>|<unitId>|<targetIndex>" to reorder the army roster.
                         */
                        if (cArmy.getAmountOfUnits() > 1) {
                            JMenu positionMenu = new JMenu("Position");
                            popup.add(positionMenu);
                            int currPos = 0;
                            for (Unit unit : cArmy.getUnits()) {
                                CUnit currUnit = (CUnit) unit;
                                if (currUnit.getId() != mid) {
                                    menuItem = new JMenuItem(String.format("Move to #%s", currPos + 1));
                                    menuItem.setActionCommand(String.format("RPU|%s|%s|%s", lid, cUnit.getId(), currPos));
                                    menuItem.addActionListener(this);
                                    positionMenu.add(menuItem);
                                }
                                currPos++;
                            }
                        }// end position menu construction

                    }// end code black for Exchange AND Add AND Position
                    // "Link" submenu (built earlier) is only attached to the popup if this unit actually
                    // carries some form of C3 equipment.
                    if (cUnit.getC3Level() != Unit.C3_NONE) {
                        popup.add(linkMenu);
                    }
                    // "Unlink" appears only if this unit is currently part of a C3/C3i network; sends
                    // "LCN|<lid>|<unitId>|-1" (master id -1 signals disconnect).
                    if (cUnit.hasBeenC3LinkedTo(cArmy) || (cArmy.getC3Network().get(cUnit.getId()) != null)) {
                        menuItem = new JMenuItem("Unlink");
                        menuItem.setActionCommand(String.format("LCN|%s|%s|-1", lid, cUnit.getId()));
                        menuItem.addActionListener(this);
                        popup.add(menuItem);

                    }
                    // divide army composition/unit display options.
                    popup.addSeparator();

                    // Add Show Mek Option
                    // "View Unit" opens the read-only MWUnitDisplay record sheet for this unit/entity.
                    menuItem = new JMenuItem("View Unit");
                    menuItem.setActionCommand(String.format("SM|%s|%s", row, col));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Customize Unit Option
                    // "Customize Unit" opens the CustomUnitDialog editor (loadout/equipment changes).
                    menuItem = new JMenuItem("Customize Unit");
                    menuItem.setActionCommand(String.format("CMU|%s|%s", row, col));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Auto Eject Option
                    // Auto-eject toggle only applies to `Mek` entities; label/command flips based on the
                    // unit's current isAutoEject() state.
                    if (cUnit.getEntity() instanceof Mek mek) {
                        if (mek.isAutoEject()) {
                            menuItem = new JMenuItem("Disable Auto Eject");
                            menuItem.setActionCommand(String.format("DAE|%s|%s", row, col));
                        } else {
                            menuItem = new JMenuItem("Enable Auto Eject");
                            menuItem.setActionCommand(String.format("EAE|%s|%s", row, col));
                        }
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    // Toggles whether this unit is the army's commander. NOTE (apparent bug): the action
                    // commands generated here are "REMOVEUNITCOMMANDER|..." and "SETUNITCOMMANDER|..." (no
                    // underscores), but the corresponding cases in actionPerformed's switch are named
                    // "REMOVE_UNIT_COMMANDER" and "SET_UNIT_COMMANDER" (with underscores). Since the switch
                    // matches on the exact command token, neither menu item's command ever matches a case,
                    // so clicking "Set Commander"/"Remove Commander" currently does nothing.
                    if (cArmy.isCommander(cUnit.getId())) {
                        menuItem = new JMenuItem("Remove Commander");
                        menuItem.setActionCommand(String.format("REMOVEUNITCOMMANDER|%s|%s|%s", row, col, lid));
                    } else {
                        menuItem = new JMenuItem("Set Commander");
                        menuItem.setActionCommand(String.format("SETUNITCOMMANDER|%s|%s|%s", row, col, lid));
                    }
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                }// end if (cUnit in click area != null)
                else {
                    popup.add(addMenu);
                }

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            // --- Branch 4/5: click was below the army rows, i.e. a hangar-only cell ---
            } else {
                CUnit cm = chqPanel.getMekTable().getMekAt(row, col);
                if (cm != null) {
                    // Branch 4: the hangar cell contains a unit -> full hangar-unit context menu.

                    menuItem = new JMenuItem("View Unit");
                    menuItem.setActionCommand(String.format("SM|%s|%s", row, col));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Customize Unit Option
                    menuItem = new JMenuItem("Customize Unit");
                    menuItem.setActionCommand(String.format("CMU|%s|%s", row, col));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // "Repairs" submenu only appears under the advanced-repairs ruleset (chqPanel.useAdvanceRepairs()).
                    if (chqPanel.useAdvanceRepairs()) {

                        JMenu repairs = new JMenu("Repairs");
                        // Repair/Bulk Repair (or, under "UseSimpleRepair", a single simplified "Repair
                        // Unit" item) only appear if the unit actually has armor or critical damage.
                        if (UnitUtils.hasArmorDamage(cm.getEntity()) ||
                                  UnitUtils.hasCriticalDamage(cm.getEntity())) {
                            if (!Boolean.parseBoolean(chqPanel.getClient().getServerConfigs("UseSimpleRepair"))) {
                                // Add repair unit option
                                menuItem = new JMenuItem("Repair Unit");
                                menuItem.setActionCommand(String.format("ARU|%s|%s", row, col));
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);

                                menuItem = new JMenuItem("Bulk Repair");
                                menuItem.setActionCommand(String.format("BUR|%s|%s", row, col));
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);
                            } else {
                                menuItem = new JMenuItem("Repair Unit");
                                menuItem.setActionCommand(String.format("SUR|%s|%s", row, col));
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);
                            }

                        }

                        // Salvage options only appear under the "UsePartsRepair" ruleset and only for
                        // Meks/Vehicles.
                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UsePartsRepair"), false) &&
                                  ((cm.getType() == Unit.MEK) || (cm.getType() == Unit.VEHICLE))) {
                            menuItem = new JMenuItem("Salvage Unit Crits");
                            menuItem.setActionCommand(String.format("SUC|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);

                            menuItem = new JMenuItem("Bulk Salvage");
                            menuItem.setActionCommand(String.format("BSU|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        // Shown only if this unit currently has an active repair job in progress.
                        if (UnitUtils.isRepairing(cm.getEntity())) {
                            // Add display repair job option
                            menuItem = new JMenuItem("Display Repair Jobs");
                            menuItem.setActionCommand(String.format("DRJ|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        // Shown only if the repair (RMT) or salvage (SMT) work-order trackers report queued
                        // orders for this unit; lets the player view or cancel all pending work.
                        if (((chqPanel.getClient().getRMT() != null) &&
                                   chqPanel.getClient().getRMT().hasQueuedOrders(cm.getId())) ||
                                  ((chqPanel.getClient().getSMT() != null) &&
                                         chqPanel.getClient().getSMT().hasQueuedOrders(cm.getId()))) {
                            // Add display pending job option
                            menuItem = new JMenuItem("Display Pending Work Orders");
                            menuItem.setActionCommand(String.format("DPWO|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);

                            // Add stop all pending jobs
                            menuItem = new JMenuItem("Stop All Pending Work Orders");
                            menuItem.setActionCommand(String.format("SAPWO|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        // Shown only if the unit is missing some ammo (not fully loaded on all ammo bins).
                        if (!UnitUtils.hasAllAmmo(cm.getEntity())) {
                            menuItem = new JMenuItem("Reload All Ammo");
                            menuItem.setActionCommand(String.format("RAA|%s|%s", row, col));
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        // Only attach the Repairs submenu at all if at least one of the above populated it;
                        // an empty "Repairs" menu is never shown.
                        if (repairs.getItemCount() > 0) {
                            popup.add(repairs);
                        }
                    }

                    // Add Auto eject Option (identical toggle logic to the in-army branch above, duplicated here
                    // for hangar units).
                    if (cm.getEntity() instanceof Mek mek) {
                        if (mek.isAutoEject()) {
                            menuItem = new JMenuItem("Disable Auto-eject");
                            menuItem.setActionCommand(String.format("DAE|%s|%s", row, col));
                        } else {
                            menuItem = new JMenuItem("Enable Auto-eject");
                            menuItem.setActionCommand(String.format("EAE|%s|%s", row, col));
                        }

                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    popup.addSeparator();

                    // Maintain/Unmaintained toggle is only offered under the SIMPLE repair ruleset (i.e. not
                    // useAdvanceRepairs()); under advanced repairs, maintenance status is presumably managed
                    // elsewhere.
                    if (!chqPanel.useAdvanceRepairs()) {
                        if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                            menuItem = new JMenuItem("Maintain");
                            menuItem.setActionCommand(String.format("MM|%s", cm.getId()));
                        } else {
                            menuItem = new JMenuItem("Unmaintained");
                            menuItem.setActionCommand(String.format("UMM|%s", cm.getId()));
                        }

                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                    // "RePod Unit" only appears for OmniMeks/OmniVehicles; re-rolls/reassigns the unit's pod-mounted equipment.
                    if (cm.isOmni()) {
                        menuItem = new JMenuItem("RePod Unit");
                        menuItem.setActionCommand(String.format("RM|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    /*
                     * "Transactions" submenu: Donate/Scrap/Delete/Transfer/black-market sell-recall/direct-sell,
                     * each independently gated by server "Christmas_Allow*" configs (for units flagged as
                     * "Christmas" event units, each transaction type can be individually disabled) plus other
                     * per-transaction rules (e.g. black market unit-type/faction/clan restrictions below). The
                     * submenu itself is only attached to the popup if numItems ends up > 0.
                     */
                    JMenu transactionsMenu = new JMenu("Transactions");
                    int numItems = 0;
                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowDonate")
                                    , false)) {
                        menuItem = new JMenuItem("Donate Unit");
                        menuItem.setActionCommand(String.format("DO|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }
                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowScrap"),
                                    false)) {
                        menuItem = new JMenuItem("Scrap Unit");
                        menuItem.setActionCommand(String.format("S|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }
                    //@Salient for SOL free build
                    if (chqPanel.getPlayer().getHouse()
                              .equalsIgnoreCase(chqPanel.getClient().getServerConfigs("NewbieHouseName")) &&
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Sol_FreeBuild"), false)) {
                        menuItem = new JMenuItem("Delete Unit");
                        menuItem.setActionCommand(String.format("DL|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }

                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowTransfer"),
                                    false)) {
                        menuItem = new JMenuItem("Transfer Unit");
                        menuItem.setActionCommand(String.format("TM|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }

                    // NOTE (apparent bug): whether the Transactions submenu is attached to the popup at all is
                    // decided HERE, based only on whether Donate/Scrap/Delete/Transfer added anything
                    // (numItems). The black-market "Sell on BM"/"Recall from BM"/"Direct Sell Unit" items
                    // computed below are added to transactionsMenu regardless, but if numItems is still 0 at
                    // this point the menu was never popup.add()-ed, so those market items end up built into an
                    // orphaned JMenu that is never shown to the user.
                    if (numItems > 0) {
                        popup.add(transactionsMenu);
                    }

                    // Test unit for BM access
                    // Determine black-market sell eligibility: starts allowed unless this is a "Christmas"
                    // unit and Christmas_AllowBM forbids it, then is further restricted per unit-type config
                    // flags (MeksMayBeSoldOnBM, VehiclesMayBeSoldOnBM, etc.), a clan-unit ban (BMNoClan), and
                    // finally a per-faction ban list (BMNoSell, '$'-delimited) checked just below.
                    boolean canSellUnit = !cm.isChristmasUnit() ||
                                                MathUtility.parseBoolean(chqPanel.getClient()
                                                                               .getServerConfigs("Christmas_AllowBM"),
                                                      false);
                    if ((cm.getType() == Unit.MEK) &&
                              !MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("MeksMayBeSoldOnBM"),
                                    false)) {
                        canSellUnit = false;
                    } else if ((cm.getType() == Unit.VEHICLE) &&
                                     !MathUtility.parseBoolean(chqPanel.getClient()
                                                                     .getServerConfigs("VehiclesMayBeSoldOnBM"),
                                           false)) {
                        canSellUnit = false;
                    } else if ((cm.getType() == Unit.BATTLEARMOR) &&
                                     !MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("BAMayBeSoldOnBM"),
                                           false)) {
                        canSellUnit = false;
                    } else if ((cm.getType() == Unit.AERO) &&
                                     !MathUtility.parseBoolean(chqPanel.getClient()
                                                                     .getServerConfigs("AerospaceMayBeSoldOnBM"),
                                           false)) {
                        canSellUnit = false;
                    } else if ((cm.getType() == Unit.PROTOMEK) &&
                                     !MathUtility.parseBoolean(chqPanel.getClient()
                                                                     .getServerConfigs("ProtoMeksMayBeSoldOnBM"),
                                           false)) {
                        canSellUnit = false;
                    } else if ((cm.getType() == Unit.INFANTRY) &&
                                     !MathUtility.parseBoolean(chqPanel.getClient()
                                                                     .getServerConfigs("InfantryMayBeSoldOnBM"),
                                           false)) {
                        canSellUnit = false;
                    } else if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("BMNoClan"), false) &&
                                     cm.getEntity().isClan()) {
                        canSellUnit = false;
                    }

                    // Test for faction BM access
                    StringTokenizer blockedFactions = new StringTokenizer(chqPanel.getClient()
                                                                                .getServerConfigs("BMNoSell"), "$");
                    while (blockedFactions.hasMoreTokens()) {
                        if (chqPanel.getPlayer().getMyHouse().getName().equals(blockedFactions.nextToken())) {
                            canSellUnit = false;
                        }
                    }

                    if (canSellUnit && (cm.getStatus() != Unit.STATUS_FOR_SALE)) {
                        menuItem = new JMenuItem("Sell on BM");
                        menuItem.setActionCommand(String.format("AB|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    if (cm.getStatus() == Unit.STATUS_FOR_SALE) {
                        menuItem = new JMenuItem("Recall from BM");
                        menuItem.setActionCommand(String.format("RFM|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseDirectSell"), false) &&
                              (cm.getStatus() != Unit.STATUS_FOR_SALE)) {
                        menuItem = new JMenuItem("Direct Sell Unit");
                        menuItem.setActionCommand(String.format("DSU|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    // "Pilot" submenu: retire/rename/promote/demote the unit's assigned pilot, plus (further
                    // below) the personal pilot-queue exchange/assign feature.
                    JMenu pilotMenu = new JMenu("Pilot");
                    popup.add(pilotMenu);
                    // Cannot Retire or rename Vacant pilots.
                    if (!cm.hasVacantPilot()) {
                        menuItem = new JMenuItem("Retire");
                        menuItem.setActionCommand(String.format("RT|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        pilotMenu.add(menuItem);

                        menuItem = new JMenuItem("Rename");
                        menuItem.setActionCommand(String.format("RP|%s", cm.getId()));
                        menuItem.addActionListener(this);
                        pilotMenu.add(menuItem);

                        // Promote/Demote only offered when the server allows buying/selling pilot skill
                        // upgrades; Demote further requires selling upgrades to be allowed on top of buying.
                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs(
                              "PlayersCanBuyPilotUpgrades"), false)) {
                            menuItem = new JMenuItem("Promote Pilot");
                            menuItem.setActionCommand(String.format("PP|%s", cm.getId()));
                            menuItem.addActionListener(this);
                            pilotMenu.add(menuItem);

                            if (MathUtility.parseBoolean(chqPanel.getClient()
                                                               .getServerConfigs("PlayersCanSellPilotUpgrades"),
                                  false)) {
                                menuItem = new JMenuItem("Demote Pilot");
                                menuItem.setActionCommand(String.format("DP|%s", cm.getId()));
                                menuItem.addActionListener(this);
                                pilotMenu.add(menuItem);
                            }
                        }
                    }

                    // Pilot Queues Block
                    // Only relevant when the server allows "personal pilot queues" (a per-player pool of
                    // spare pilots by unit type/weight class) and the unit takes a single pilot. Builds a
                    // pilot-assignment/exchange menu ("Assign" if currently vacant, "Exchange" plus a
                    // "Remove" item otherwise) populated from chqPanel.getPlayer().getPersonalPilotQueue().
                    boolean personalPilotQueuesEnabled = MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs(
                          "AllowPersonalPilotQueues"), false);
                    if (personalPilotQueuesEnabled && (cm.isSinglePilotUnit())) {

                        // load possible pilots
                        LinkedList<Pilot> pilots = chqPanel.getPlayer().getPersonalPilotQueue()
                                                         .getPilotQueue(cm.getType(), cm.getWeightClass());
                        JMenu exchangeMenu = new JMenu("Exchange");

                        // option to remove pilot if that hasn't been done
                        // already
                        if (!cm.hasVacantPilot()) {
                            pilotMenu.addSeparator();
                            menuItem = new JMenuItem("Remove");
                            menuItem.setActionCommand(String.format("EXP|%s|-1", cm.getId()));
                            menuItem.addActionListener(this);
                            pilotMenu.add(menuItem);
                        } else {
                            exchangeMenu = new JMenu("Assign");
                        }

                        /*
                         * Set up the actual menu *IF* the pilot queue has a non-zero size.
                         */
                        if (pilots.isEmpty()) {
                            exchangeMenu.setEnabled(false);
                        } else {

                            /*
                             * Construction of EXCHANGE pilot. Derived from the other exchange options.
                             */
                            pilotMenu.add(exchangeMenu);

                            for (int i = 0; i < pilots.size(); i++) {
                                String pilotString = getPilotString(cm, pilots.get(i));
                                menuItem = new JMenuItem(pilotString);

                                menuItem.setActionCommand(String.format("EXP|%s|%s", cm.getId(), i));
                                menuItem.addActionListener(this);
                                exchangeMenu.add(menuItem);
                            }
                        }
                    }

                    popup.addSeparator();

                    // Broadcasts this unit's stats to the player's faction chat channel (server command "sth#u#<id>").
                    menuItem = new JMenuItem("Show To Faction");
                    menuItem.setActionCommand(String.format("SUTH|%s", cm.getId()));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // "Remove From All" (RFAA) pulls this unit out of every army it currently sits in,
                    // returning it to hangar-only status.
                    menuItem = new JMenuItem("Remove From All");
                    menuItem.setActionCommand(String.format("RFAA|%s", cm.getId()));
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    /*
                     * Disable RFAA option if unit isn't actually IN any of the player's armies.
                     */
                    boolean isInArmy = false;
                    for (CArmy currA : chqPanel.getClient().getPlayer().getArmies()) {
                        if (currA.getUnit(cm.getId()) != null) {
                            isInArmy = true;
                            break;
                        }
                    }

                    if (!isInArmy) {
                        menuItem.setEnabled(false);
                    }

                    popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                // Branch 5: hangar cell is empty AND the player still has at least one free bay -> offer to
                // pare down unused bay capacity, but only from the single cell that represents the very
                // first free (unoccupied) bay slot, to avoid repeating the option on every empty cell.
                } else if (chqPanel.getPlayer().getFreeBays() > 0) {
                    int hangerNum = (((row - chqPanel.getMekTable().getRowsForArmies()) *
                                            (chqPanel.getMekTable().getColumnCount() - 1)) +
                                           col) - 1;
                    if (hangerNum == chqPanel.getClient().getPlayer().getHangar().size()) {// only
                        // show in first free cell
                        if (chqPanel.useAdvanceRepairs()) {
                            menuItem = new JMenuItem("Sell Excess Bays");
                            menuItem.setActionCommand("SEB");
                        } else {
                            menuItem = new JMenuItem("Fire Excess Techs");
                            menuItem.setActionCommand("FET");
                        }
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                        popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
                    }
                }
            }
        }
    }

    /**
     * Builds an empty numbered {@link JMenu} label for the Nth overflow page of a weight-class/ProtoMek/
     * Infantry group in the Add/Exchange menus, used when a single category has more than 10 candidate units
     * and must be split across multiple submenus (e.g. "Heavy 2", "Proto 3", "Infantry 1").
     *
     * @param i weight-class/category index: 0-3 are the four weight classes (see {@link Unit#getWeightClassDesc}),
     *          4 is ProtoMek, 5 (or anything else) is treated as Infantry/BattleArmor
     * @param j zero-based page index within the category; displayed as {@code j + 1}
     * @return a new, empty {@link JMenu} with the appropriate numbered label
     */
    private static @NonNull JMenu getMenuX(int i, int j) {
        JMenu menuX;
        if (i < 4) {
            menuX = new JMenu(String.format("%s %s", Unit.getWeightClassDesc(i), j + 1));
        } else if (i == 4) {// proto
            menuX = new JMenu(String.format("Proto %s", j + 1));
        } else {// BA, can assume this is i ==
            // 5.
            menuX = new JMenu(String.format("Infantry %s", j + 1));
        }
        return menuX;
    }

    /**
     * Scans an array of already-added menu components (typically {@code addMenu.getMenuComponents()} or
     * {@code jm.getMenuComponents()}) for any {@link JMenu} whose label starts with "Proto", used to decide
     * whether a separator is needed before the Infantry group so ProtoMek and Infantry submenus aren't
     * visually run together.
     *
     * @param components the current top-level children of the Add/Exchange menu being built
     * @return true if at least one child menu's text starts with "Proto"
     */
    private static boolean isProtoMenu(Component[] components) {
        boolean hasProtoMenu = false;
        for (Component currComponent : components) {
            if (currComponent instanceof JMenu currMenu) {
                if (currMenu.getText().startsWith("Proto")) {
                    hasProtoMenu = true;
                }
            }
        }
        return hasProtoMenu;
    }

    /**
     * Formats a display label for one candidate pilot in the personal-pilot-queue "Assign"/"Exchange"
     * submenu, showing the pilot's name plus their relevant skill(s), and (for Mek pilots only) any
     * accumulated hit/injury count.
     *
     * @param cm    the unit the pilot would be assigned to (its type determines whether a piloting skill
     *              is shown alongside gunnery)
     * @param pilot the candidate pilot from the personal pilot queue
     * @return a formatted label such as {@code "Jenny Doe (4/5, Small Arms) Hits: 1"} for a Mek pilot, or
     *         {@code "Jenny Doe (4)"} for non-Mek crew
     */
    private static @NonNull String getPilotString(CUnit cm, Pilot pilot) {
        String pilotString;
        String skills = pilot.getSkillString(true);
        if (cm.getType() == Unit.MEK) {
            pilotString = String.format("%s (%s/%s", pilot.getName(), pilot.getGunnery(), pilot.getPiloting());
            if (skills.trim().isEmpty()) {
                pilotString += ")";
            } else {
                pilotString += String.format(", %s)", skills);
            }

            if (pilot.getHits() > 0) {
                pilotString += String.format(" Hits: %s", pilot.getHits());
            }

        } else {
            pilotString = String.format("%s (%s", pilot.getName(), pilot.getGunnery());
            if (skills.trim().isEmpty()) {
                pilotString += ")";
            } else {
                pilotString += String.format(", %s)", skills);
            }
        }
        return pilotString;
    }

    /**
     * Handles mouse-button-up: if a drag was in progress ({@link #isDrag}), completes the drag-and-drop
     * operation and sends the corresponding campaign chat command to the server based on where the mouse was
     * released:
     * <ul>
     *   <li>Released over the hangar area (no target army) while the unit came from an army: sends an
     *   "EXM" command with hangarUnitId=-1, removing the unit from its origin army.</li>
     *   <li>Released over an army coming from the hangar: adds the dragged unit to that army (optionally
     *   swapping out whatever unit was already in the target cell).</li>
     *   <li>Released over a different cell within the SAME origin army: reorders the unit to the new
     *   position via a "unitposition" command.</li>
     * </ul>
     * In every case the drag image is erased by repainting {@link #dragRect}, the cursor is reset to
     * default, {@link #isDrag} is cleared, and finally {@link #maybeShowPopup} is invoked (so that on
     * platforms where the popup trigger fires on release rather than press, e.g. Windows, the context menu
     * still appears).
     *
     * @param mouseEvent the originating Swing mouse event
     */
    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

        /*
         * If this was a drag, try to drop the unit into a target army or the hangar.
         */
        if (isDrag) {

            // regardless of outcome, clear drag image.
            chqPanel.getTableMeks().paintImmediately(dragRect.getBounds());

            boolean validRelease = chqPanel.getTableMeks().contains(mouseEvent.getPoint());

            int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
            int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
            CUnit exchangeUnit = chqPanel.getMekTable().getMekAt(row, col);
            currArmy = chqPanel.getMekTable().getArmyAt(row);

            // null finish army. moving to hangar.
            if ((currArmy == null) && validRelease) {

                // if the unit is from an army, remove it
                if (startArmy != null) {
                    chqPanel.getClient()
                          .sendChat(String.format("%sc EXM#%s,%s", IClient.CAMPAIGN_PREFIX, startArmy.getID(), dragUnit.getId()));
                }

            }// end if (release over hangar)

            // finish army exists
            else if (validRelease) {

                // from hangar to an army
                if (startArmy == null) {

                    // army # or empty space. add the unit.
                    if (exchangeUnit == null) {
                        chqPanel.getClient().sendChat(
                              String.format("%sc EXM#%s,-1#%s", IClient.CAMPAIGN_PREFIX, currArmy.getID(), dragUnit.getId()));
                    } else if (dragUnit.getId() != exchangeUnit.getId()) {
                        chqPanel.getClient()
                              .sendChat(String.format("%sc EXM#%s,%s#%s", IClient.CAMPAIGN_PREFIX, currArmy.getID(), exchangeUnit.getId(), dragUnit.getId()));
                    }
                }

                // within the same army, change positions
                else if ((currArmy.getID() == startArmy.getID()) &&
                               (exchangeUnit != null) &&
                               (dragUnit.getId() != exchangeUnit.getId())) {
                    int newPosition = 0;
                    for (Unit currU : currArmy.getUnits()) {
                        if (currU.getId() == exchangeUnit.getId()) {
                            break;
                        }
                        newPosition++;
                    }
                    chqPanel.getClient().sendChat(
                          String.format("%sc unitposition#%s#%s#%s", IClient.CAMPAIGN_PREFIX, startArmy.getID(), dragUnit.getId(), newPosition));
                }

            }// end else(target army exists)

            // revert to normal cursor
            chqPanel.getTableMeks().setCursor(Cursor.getDefaultCursor());

        }// end if(isDrag)

        isDrag = false;
        maybeShowPopup(mouseEvent);
    }

    /**
     * Handles mouse movement while a button is held during an active drag ({@link #isDrag}): redraws the
     * semi-transparent {@link #dragImage} at the new cursor position (erasing the old image first), and
     * updates the table's cursor to preview the outcome of dropping at the current location -- e.g. an "add"
     * icon over an empty army slot, a "duplicate" icon if the unit is already in that army, a "max" icon if
     * the multi-army unit cap has been reached, a "reposition" icon when reordering within the same army, or
     * a "not allowed" icon when the current player status forbids the action (see the per-branch comments
     * below for the exact cursor-selection rules, which largely mirror the drop logic in
     * {@link #mouseReleased}).
     *
     * @param mouseEvent the originating Swing mouse event
     */
    @Override
    public void mouseDragged(MouseEvent mouseEvent) {

        if (isDrag) {

            // repaint the old image location
            chqPanel.getTableMeks().paintImmediately(dragRect.getBounds());

            // determine new boundaries for the rectangle
            dragRect.setRect(mouseEvent.getX() - offset.x, mouseEvent.getY() - offset.y, 84, 72);

            // place the label in a new location
            Graphics2D g = (Graphics2D) chqPanel.getTableMeks().getGraphics();
            g.drawImage(dragImage,
                  AffineTransform.getTranslateInstance(dragRect.getX(), dragRect.getY()), null);

            /*
             * Update the cursor depending on the current drag status. If dragging a unit into an army which already contains the unit, mark ineligible. Else, show the drag cursor.
             */
            int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
            int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
            CUnit currUnit = chqPanel.getMekTable().getMekAt(row, col);
            currArmy = chqPanel.getMekTable().getArmyAt(row);

            // null curr army. is an attempt to move to the hangar.
            if (currArmy == null) {

                // if the unit is from an army, could remove. show minus.
                if ((startArmy != null) &&
                          (chqPanel.getClient().getMyStatus() == IClient.STATUS_RESERVE)) {
                    chqPanel.getTableMeks().setCursor(removeCursor);
                } else if (startArmy != null) {
                    chqPanel.getTableMeks().setCursor(notAllowedCursor);
                } else {
                    chqPanel.getTableMeks().setCursor(Cursor.getDefaultCursor());
                }

            }// end if (release over hangar)

            // currArmy exists
            else {

                // from hangar to an army
                if (startArmy == null) {

                    if (chqPanel.getClient().getMyStatus() != IClient.STATUS_RESERVE) {
                        chqPanel.getTableMeks().setCursor(notAllowedCursor);
                    } else if (chqPanel.getPlayer().getAmountOfTimesUnitExistsInArmies(dragUnit.getId()) >=
                                     MathUtility.parseInt(chqPanel.getClient()
                                                                .getServerConfigs("UnitsInMultipleArmiesAmount"), 0)) {
                        chqPanel.getTableMeks().setCursor(maxCursor);
                    } else if (currArmy.getUnit(dragUnit.getId()) != null) {
                        chqPanel.getTableMeks().setCursor(dupeCursor);
                    } else if (currUnit == null) {
                        chqPanel.getTableMeks().setCursor(addCursor);
                    } else if (dragUnit.getId() != currUnit.getId()) {
                        chqPanel.getTableMeks().setCursor(exchangeCursor);
                    }
                }

                // within the same army, change positions
                else if (currArmy.getID() == startArmy.getID()) {

                    if ((currUnit != null) &&
                              (dragUnit.getId() != currUnit.getId()) &&
                              (chqPanel.getClient().getMyStatus() != IClient.STATUS_FIGHTING)) {
                        chqPanel.getTableMeks().setCursor(positionCursor);
                    } else {
                        chqPanel.getTableMeks().setCursor(notAllowedCursor);
                    }
                } else {
                    chqPanel.getTableMeks().setCursor(Cursor.getDefaultCursor());
                }

            }// end else(target army exists)

        }

    }

    /**
     * Central dispatch point for every context-menu item built in {@link #maybeShowPopup} (all of which use
     * {@code this} as their {@link ActionListener}). The {@link ActionEvent}'s action command is a
     * pipe ({@code |})-delimited string whose first token (upper-cased) is the command mnemonic and whose
     * remaining tokens are its parameters, e.g. {@code "EXM|<armyId>|<unitId>|<hangarUnitId>"}. Most cases
     * either:
     * <ul>
     *   <li>forward directly to an existing handler method on {@code chqPanel.getClient().getMainFrame()}
     *   (typically opening a dialog or performing validation before sending a command), or</li>
     *   <li>build and send a raw campaign command string to the server via
     *   {@code chqPanel.getClient().sendChat(IClient.CAMPAIGN_PREFIX + "c " + ...)}, relying on the server's
     *   response/subsequent state push to update the GUI, or</li>
     *   <li>open a client-side dialog/window directly (e.g. unit display, repair dialogs, pilot promotion).</li>
     * </ul>
     * The table is repainted unconditionally at the end of the method, after the switch, regardless of which
     * case matched (or if none did).
     * <p>
     * Several cases here have no corresponding menu item anywhere in {@link #maybeShowPopup} and so cannot
     * currently be triggered by the popup menus in this class (they may be dead/legacy code, or invoked from
     * elsewhere): {@code AA} ("add army", sends a create-army command), {@code SA}/{@code SI} (empty
     * no-op placeholders, apparently vestigial "set active"/"set inactive" stubs), {@code EUR} ("Estimate
     * Unit Repairs", fully implemented but never wired to a menu item), and {@code SAS}/{@code TAS}
     * (secondary/tertiary army sort -- only a primary army sort submenu is ever built). Additionally, the
     * "Set Commander"/"Remove Commander" menu items built above send {@code SETUNITCOMMANDER}/
     * {@code REMOVEUNITCOMMANDER} (no underscores), which do not match the {@code SET_UNIT_COMMANDER}/
     * {@code REMOVE_UNIT_COMMANDER} case labels below, so those two menu items are currently no-ops.
     *
     * @param actionEvent the Swing action event carrying the pipe-delimited command string
     */
    public void actionPerformed(ActionEvent actionEvent) {

        String actionCommand = actionEvent.getActionCommand();
        StringTokenizer stringTokenizer = new StringTokenizer(actionCommand, "|");
        String command = stringTokenizer.nextToken().toUpperCase();

        // exchange mek
        switch (command) {
            // EXM: swap the unit currently in army <lid>'s slot occupied by hangar-unit-turned-army-unit
            // <hid> for hangar unit <hid> (or add if the slot -1 sentinel is used, or remove to hangar when
            // hid is -1); this single command backs Add, Exchange, and Move-To-Hangar menu items.
            case "EXM" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int hid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient()
                      .sendChat(String.format("%sc EXM#%s,%s#%s", IClient.CAMPAIGN_PREFIX, lid, mid, hid));
                // move to hanger
            }
            // MH ("Move To Hangar"): removes unit <mid> from army <lid> without replacement (reuses the "EXM" server command with no hangar-unit id).
            case "MH" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc EXM#%s,%s", IClient.CAMPAIGN_PREFIX, lid, mid));
                // add lance
            }
            // AA: creates a new army/lance using the configured default army name. Not currently reachable
            // from any menu item built in this file (see class-level note on dead cases above).
            case "AA" -> chqPanel.getClient().sendChat(String.format("%sc cra#%s", IClient.CAMPAIGN_PREFIX, chqPanel.getClient()
                                                                                                    .getConfigParam(
                                                                                                          "DEFAULT_ARMY_NAME")));

            // set lance active
            // SA/SI: empty no-op cases; not reachable from any menu item built in this file.
            case "SA" -> {
            }
            case "SI" -> {
            }
            // AO ("Attack Options"): opens the commander's attack-eligibility check dialog for army <lid>.
            case "AO" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderCheckAttack_actionPerformed(lid);
                // check access
            }
            // CAA ("Check Access"): prompts the player, via a JComboBox embedded in a JOptionPane, to pick
            // one of the server's known operations, then asks the server whether army <armyID> is eligible
            // to participate in it. Cancelling the dialog returns immediately without sending anything.
            case "CAA" -> {
                int armyID = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                JComboBox<String> attackCombo = new JComboBox<>(); //Barukkhazad! 2015-11-08 removed castings

                chqPanel.getClient().getAllOps().keySet().forEach(attackCombo::addItem);
                attackCombo.setEditable(false);

                attackCombo.grabFocus();
                attackCombo.getEditor().selectAll();

                JOptionPane jop = new JOptionPane(attackCombo,
                      JOptionPane.QUESTION_MESSAGE,
                      JOptionPane.OK_CANCEL_OPTION);
                JDialog dlg = jop.createDialog(chqPanel.getClient().getMainFrame(), "Select Operation.");
                attackCombo.grabFocus();
                attackCombo.getEditor().selectAll();
                dlg.setVisible(true);

                if ((Integer) jop.getValue() == JOptionPane.CANCEL_OPTION) {
                    return;
                }

                String attackName = (String) attackCombo.getSelectedItem();
                chqPanel.getClient()
                      .sendChat(String.format("%sc checkarmyeligibility#%s#%s", IClient.CAMPAIGN_PREFIX, armyID, attackName));
                // Remove Army
            }
            case "RA" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderRemoveLance_actionPerformed(lid);
                // rename army
            }
            case "LA" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderPlayerLockArmy_actionPerformed(lid);
                // lock army
            }
            case "ULA" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderPlayerUnlockArmy_actionPerformed(lid);
                // unlock army
            }
            // DAA: toggles army <lid>'s disabled state; shared by both the "Disable Army" and "Enable Army"
            // menu items (see note at their construction site), so jMenuCommanderDisableArmy_actionPerformed
            // must itself flip the current state rather than unconditionally disabling.
            case "DAA" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderDisableArmy_actionPerformed(lid);
            }
            case "NA" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderNameArmy_actionPerformed(mid);
                // set Lower Unit Limit
            }
            case "SLUL" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderSetLowerUnitLimit_actionPerformed(lid);
                // set upper Unit Limit
            }
            case "SUUL" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderSetUpperUnitLimit_actionPerformed(lid);
                // Set Force Size you plan on facing
            }
            case "SFS" -> {
                int aid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderSetForceSizeToFace_actionPerformed(aid);
                // show to faction - army
            }
            case "SATH" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc sth#a#%s", IClient.CAMPAIGN_PREFIX, lid));
                // make public challenge
            }
            // MPC ("Make Public Challenge"): builds and sends one formatted "Looking for a game at ..." chat
            // message per matching army (only army <lid> if lid != -1, otherwise every army the player owns
            // -- see the `if (lid != -1) break;` at the end of the loop body). `mode` (1-13) selects which
            // combination of stats gets embedded in the message; each `mode == N` branch below builds a
            // different summary (BV only, unit count, weight-class breakdown, tonnage, unit type counts,
            // hyperlinked per-unit model names, or per-tonnage groupings), optionally scaled/annotated by the
            // "Operations" force-size rule (opForceSize/forceSizeMod) when that ruleset is active. The chosen
            // `operation` name (or the literal "none") is appended in parentheses at the end if non-trivial.
            case "MPC" -> {
                int mode = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                boolean useForceSize = MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs(
                      "UseOperationsRule"), false);
                float opForceSize = Army.NO_LIMIT;
                double forceSizeMod = 1;

                String operation = stringTokenizer.nextToken();

                for (CArmy currArmy : chqPanel.getClient().getPlayer().getArmies()) {
                    if ((lid != -1) && (currArmy.getID() != lid)) {
                        continue;
                    }

                    StringBuilder toSend = new StringBuilder(chqPanel.getClient().getConfigParam("CHALLENGE_STRING"));

                    if (useForceSize) {
                        opForceSize = currArmy.getOpForceSize();

                        if (opForceSize > 0) {
                            forceSizeMod = currArmy.forceSizeModifier(opForceSize);
                        }
                    }

                    // load the default if a non-entry is set.
                    if (toSend.toString().trim().isEmpty()) {
                        toSend = new StringBuilder("Looking for a game at");// matches default
                        // config
                    }

                    // BV only
                    if (mode == 1) {
                        toSend.append(String.format(" %s BV", Math.round(currArmy.getBV() * forceSizeMod)));

                        if (forceSizeMod > 1) {
                            toSend.append(String.format(" vs %s units", opForceSize));
                        }

                        toSend.append(".");
                    } else if (mode == 2) {
                        int armySize = currArmy.getUnits().size();
                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(", with  ").append(armySize).append(" unit");

                        // BUG: when armySize > 1 this appends the literal text "actionCommand." instead of
                        // the intended plural suffix (e.g. "s."). This looks like an accidental artifact of a
                        // find/replace that renamed a local variable to `actionCommand` and clobbered a
                        // string literal in the process (see also the "BA'actionCommand" comment typo a few
                        // lines below in the mode == 3 branch). The outgoing chat message will literally read
                        // "...with 3 unitactionCommand." for armies with more than one unit.
                        if (armySize > 1) {
                            toSend.append("actionCommand.");
                        } else {
                            toSend.append(".");
                        }
                    } else if (mode == 3) {
                        int assaultM = 0;
                        int heavyM = 0;
                        int mediumM = 0;
                        int lightM = 0;
                        int protoM = 0;
                        int ba = 0;
                        int vehicles = 0;
                        int aero = 0;
                        int assaultV = 0;
                        int heavyV = 0;
                        int mediumV = 0;
                        int lightV = 0;
                        int inf = 0;

                        boolean showVeeWeights = MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs(
                              "ShowVehicleWeightClassInChallenges"), false);

                        for (Unit currUnit : currArmy.getUnits()) {
                            // Meks
                            if ((currUnit.getType() == Unit.MEK) || (currUnit.getType() == Unit.QUAD)) {
                                if (currUnit.getWeightClass() == Unit.ASSAULT) {
                                    assaultM++;
                                } else if (currUnit.getWeightClass() == Unit.HEAVY) {
                                    heavyM++;
                                } else if (currUnit.getWeightClass() == Unit.MEDIUM) {
                                    mediumM++;
                                } else {
                                    lightM++;
                                }
                            }

                            // ProtoMek
                            else if (currUnit.getType() == Unit.PROTOMEK) {
                                protoM++;
                            } else if (currUnit.getType() == Unit.VEHICLE) {
                                vehicles++;
                                if (showVeeWeights) {
                                    if (currUnit.getWeightClass() == Unit.ASSAULT) {
                                        assaultV++;
                                    } else if (currUnit.getWeightClass() == Unit.HEAVY) {
                                        heavyV++;
                                    } else if (currUnit.getWeightClass() == Unit.MEDIUM) {
                                        mediumV++;
                                    } else {
                                        lightV++;
                                    }
                                }
                            }
                            // BA'actionCommand
                            else if (currUnit.getType() == Unit.BATTLEARMOR) {
                                ba++;
                            } else if (currUnit.getType() == Unit.AERO) {
                                aero++;
                            } else {
                                // assume infantry
                                inf++;
                            }
                        }

                        // assemble the string
                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(".");

                        if (assaultM > 0) {
                            toSend.append(" ").append(assaultM).append("A,");
                        }

                        if (heavyM > 0) {
                            toSend.append(" ").append(heavyM).append("H,");
                        }

                        if (mediumM > 0) {
                            toSend.append(" ").append(mediumM).append("M,");
                        }

                        if (lightM > 0) {
                            toSend.append(" ").append(lightM).append("L,");
                        }

                        if (protoM > 0) {
                            toSend.append(" ").append(protoM).append(" ProtoMeks,");
                        }

                        if (ba > 0) {
                            toSend.append(" ").append(ba).append(" BAs,");
                        }

                        // BUG: this condition checks `ba > 0` again (copy-paste from the block above) instead
                        // of `aero > 0`. As written, the Aerospace count is only appended to the challenge
                        // message when the army also has at least one BattleArmor unit; an army with
                        // Aerospace units but no BA never gets its aero count mentioned.
                        if (ba > 0) {
                            toSend.append(" ").append(aero).append(" Aerospace,");
                        }

                        if (vehicles > 0) {
                            if (showVeeWeights) {
                                if (assaultV > 0) {
                                    toSend.append(" ").append(assaultV).append("A Vehicles,");
                                }

                                if (heavyV > 0) {
                                    toSend.append(" ").append(heavyV).append("H Vehicles,");
                                }

                                if (mediumV > 0) {
                                    toSend.append(" ").append(mediumV).append("M Vehicles,");
                                }

                                if (lightV > 0) {
                                    toSend.append(" ").append(lightV).append("L Vehicles,");
                                }
                            } else {
                                toSend.append(" ").append(vehicles).append(" Vehicles,");
                            }
                        } else if (inf > 0) {
                            toSend.append(" ").append(inf).append(" Inf,");
                        }

                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    } else if (mode == 4) {
                        int Tonnage = 0;

                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                Tonnage += (int) cUnit.getEntity().getWeight();
                            }
                        }

                        toSend.append(" ").append(Tonnage).append(" tons.");
                    } else if (mode == 5) {
                        int Tonnage = 0;
                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                Tonnage += (int) cUnit.getEntity().getWeight();
                            }
                        }

                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(", at ").append(Tonnage).append(" tons");
                    } else if (mode == 6) {
                        int Tonnage = 0;

                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                Tonnage += (int) cUnit.getEntity().getWeight();
                            }
                        }

                        toSend.append(" ").append(Tonnage).append(" tons, with ").append(currArmy.getUnits().size());

                        if (currArmy.getUnits().size() == 1) {
                            toSend.append(" unit.");
                        } else {
                            toSend.append(" units.");
                        }
                    } else if (mode == 7) {
                        int Tonnage = 0;

                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                Tonnage += (int) cUnit.getEntity().getWeight();
                            }
                        }

                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(", at ")
                              .append(Tonnage)
                              .append(" tons, with ")
                              .append(currArmy.getUnits().size());

                        if (currArmy.getUnits().size() == 1) {
                            toSend.append(" unit.");
                        } else {
                            toSend.append(" units.");
                        }
                    } else if (mode == 8) {
                        int assault = 0;
                        int heavy = 0;
                        int medium = 0;
                        int light = 0;

                        for (Unit unit : currArmy.getUnits()) {
                            switch (unit.getWeightClass()) {
                                case Unit.ASSAULT:
                                    assault++;
                                    break;
                                case Unit.LIGHT:
                                    light++;
                                    break;
                                case Unit.MEDIUM:
                                    medium++;
                                    break;
                                case Unit.HEAVY:
                                    heavy++;
                                    break;
                            }
                        }

                        if (assault > 0) {
                            toSend.append(" ").append(assault).append("A,");
                        }

                        if (heavy > 0) {
                            toSend.append(" ").append(heavy).append("H,");
                        }

                        if (medium > 0) {
                            toSend.append(" ").append(medium).append("M,");
                        }

                        if (light > 0) {
                            toSend.append(" ").append(light).append("L,");
                        }

                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    } else if (mode == 9) {
                        int assault = 0;
                        int heavy = 0;
                        int medium = 0;
                        int light = 0;

                        for (Unit unit : currArmy.getUnits()) {
                            switch (unit.getWeightClass()) {
                                case Unit.ASSAULT:
                                    assault++;
                                    break;
                                case Unit.LIGHT:
                                    light++;
                                    break;
                                case Unit.MEDIUM:
                                    medium++;
                                    break;
                                case Unit.HEAVY:
                                    heavy++;
                                    break;
                            }
                        }

                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(", with");

                        if (assault > 0) {
                            toSend.append(" ").append(assault).append("A,");
                        }

                        if (heavy > 0) {
                            toSend.append(" ").append(heavy).append("H,");
                        }

                        if (medium > 0) {
                            toSend.append(" ").append(medium).append("M,");
                        }

                        if (light > 0) {
                            toSend.append(" ").append(light).append("L,");
                        }
                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));

                    } else if (mode == 10) {
                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                toSend.append(" <a href=\"MEKINFO")
                                      .append(cUnit.getUnitFilename())
                                      .append("#")
                                      .append(cUnit.getBVForMatch())
                                      .append("#")
                                      .append(cUnit.getPilot().getGunnery())
                                      .append("#")
                                      .append(cUnit.getPilot().getPiloting())
                                      .append("\">")
                                      .append(cUnit.getModelName())
                                      .append("</a>,");
                            }
                        }
                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    } else if (mode == 11) {
                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(",");
                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                toSend.append(" <a href=\"MEKINFO")
                                      .append(cUnit.getUnitFilename())
                                      .append("#")
                                      .append(cUnit.getBVForMatch())
                                      .append("#")
                                      .append(cUnit.getPilot().getGunnery())
                                      .append("#")
                                      .append(cUnit.getPilot().getPiloting())
                                      .append("\">")
                                      .append(cUnit.getModelName())
                                      .append("</a>,");
                            }
                        }
                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    } else if (mode == 12) {
                        TreeMap<Double, Integer> unitWeights = new TreeMap<>();

                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                if (!unitWeights.containsKey(cUnit.getEntity().getWeight())) {
                                    unitWeights.put(cUnit.getEntity().getWeight(), 1);
                                } else {
                                    unitWeights.put(cUnit.getEntity().getWeight(),
                                          unitWeights.get(cUnit.getEntity().getWeight()) + 1);
                                }
                            }
                        }

                        for (Double weight : unitWeights.keySet()) {
                            toSend.append(" ")
                                  .append(unitWeights.get(weight))
                                  .append("x ")
                                  .append(weight.intValue())
                                  .append(" tons,");
                        }
                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    } else if (mode == 13) {
                        toSend.append(" ").append(currArmy.getBV()).append(" BV,");
                        TreeMap<Double, Integer> unitWeights = new TreeMap<>();

                        for (Unit unit : currArmy.getUnits()) {
                            if (unit instanceof CUnit cUnit) {
                                if (!unitWeights.containsKey(cUnit.getEntity().getWeight())) {
                                    unitWeights.put(cUnit.getEntity().getWeight(), 1);
                                } else {
                                    unitWeights.put(cUnit.getEntity().getWeight(),
                                          unitWeights.get(cUnit.getEntity().getWeight()) + 1);
                                }
                            }
                        }

                        for (Double weight : unitWeights.keySet()) {
                            toSend.append(" ")
                                  .append(unitWeights.get(weight))
                                  .append("x ")
                                  .append(weight.intValue())
                                  .append(" tons,");
                        }
                        // replace final comma with a period.
                        int sendLength = toSend.lastIndexOf(",");
                        toSend = new StringBuilder(String.format("%s.", toSend.substring(0, sendLength)));
                    }

                    if (!currArmy.getName().trim().isEmpty()) {
                        toSend.append(" \"").append(currArmy.getName()).append("\"");
                    }

                    if ((operation.length() > 1) && !operation.equalsIgnoreCase("none")) {
                        toSend.append(" (").append(operation).append(")");
                    }

                    chqPanel.getClient().sendChat(toSend.toString());
                    // if lid != -1, it means only send one army, and if we've gotten this far, that means we've matched
                    // the army with the correct ID.
                    if (lid != -1) {
                        break;
                    }
                }
                // show to faction - unit
            }
            case "SUTH" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc sth#u#%s", IClient.CAMPAIGN_PREFIX, mid));
                // rename pilot
            }
            case "RP" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderNamePilot_actionPerformed(mid);
                // Promote Pilot
            }
            // PP/DP: both open the same PromotePilotDialog for unit <mid>'s pilot; the boolean flag selects
            // demote (true) vs promote (false) mode.
            case "PP" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                new PromotePilotDialog(chqPanel.getClient(), mid, false);
                // Demote pilot
            }
            case "DP" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                new PromotePilotDialog(chqPanel.getClient(), mid, true);
                // retire pilot
            }
            case "RT" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc retirepilot#%s", IClient.CAMPAIGN_PREFIX, mid));// send
                // directly
                // show mek
            }
            case "SM" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                Entity theEntity = mek.getEntity();
                JFrame infoWindow = new JFrame();
                MWUnitDisplay unitDisplay = new MWUnitDisplay(null, chqPanel.getClient());
                theEntity.loadAllWeapons();
                infoWindow.getContentPane().add(unitDisplay);
                infoWindow.setSize(300, 400);
                infoWindow.setResizable(false);
                infoWindow.setTitle(mek.getModelName());
                infoWindow.setLocationRelativeTo(chqPanel.getClient().getMainFrame());
                infoWindow.setVisible(true);
                unitDisplay.displayEntity(theEntity);
            }
            case "CMU" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                Entity theEntity = mek.getEntity();
                // JFrame InfoWindow = new JFrame();
                theEntity.loadAllWeapons();
                CustomUnitDialog customizeUnit = new CustomUnitDialog(chqPanel.getClient(),
                      theEntity,
                      mek.getPilot(),
                      mek);
                customizeUnit.setVisible(true);
            }
            // ARU ("Repair Unit", advanced-repair ruleset): opens AdvancedRepairDialog in repair mode (salvage=false).
            case "ARU" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new AdvancedRepairDialog(chqPanel.getClient(), mek.getId(), false);
            }
            // BUR ("Bulk Repair"): opens BulkRepairDialog configured for a full (non-salvage) repair of a single unit.
            case "BUR" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_BULK,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
            // SUR ("Repair Unit", simple-repair ruleset): same dialog as BUR but in TYPE_SIMPLE mode.
            case "SUR" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_SIMPLE,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
            // BSU ("Bulk Salvage"): BulkRepairDialog in TYPE_SALVAGE mode, used to strip/salvage critical slots.
            case "BSU" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_SALVAGE,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
            // SUC ("Salvage Unit Crits"): AdvancedRepairDialog in salvage mode (salvage=true).
            case "SUC" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new AdvancedRepairDialog(chqPanel.getClient(), mek.getId(), true);
            }
            case "DRJ" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);

                chqPanel.getClient().sendChat(String.format("%sc DisplayUnitRepairJobs#%s", IClient.CAMPAIGN_PREFIX, mek.getId()));
            }
            case "DPWO" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);

                if (chqPanel.getClient().getRMT() != null) {
                    chqPanel.getClient().systemMessage(chqPanel.getClient().getRMT().getRepairQueue(mek.getId()));
                }

                if (chqPanel.getClient().getSMT() != null) {
                    chqPanel.getClient().systemMessage(chqPanel.getClient().getSMT().getSalvageQueue(mek.getId()));
                }
            }
            case "SAPWO" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);

                if (chqPanel.getClient().getRMT() != null) {
                    chqPanel.getClient().getRMT().removeAllWorkOrders(mek.getId());
                }

                if (chqPanel.getClient().getSMT() != null) {
                    chqPanel.getClient().getSMT().removeAllWorkOrders(mek.getId());
                }

                chqPanel.getClient().systemMessage("Cancelled all pending work orders.");
            }
            case "RAA" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                int result = JOptionPane.showConfirmDialog(chqPanel.getClient().getMainFrame(),
                      String.format("Are you sure you want to reload all the ammo on this unit %s?", chqPanel.getClient()
                                                                                             .getPlayer()
                                                                                             .getName()),
                      "Reload it?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chqPanel.getClient()
                          .sendChat(String.format("%sc RELOADALLAMMO#%s", IClient.CAMPAIGN_PREFIX, mek.getId()));
                }
            }

            // Estimate Unit Repairs
            // EUR: fully implemented handler that composes an HTML table estimating repair cost at each tech
            // skill level (Green/Regular/Veteran/Elite) for the clicked unit and posts it as a system
            // message. As noted at the top of this method, no menu item anywhere in maybeShowPopup currently
            // sets this action command, so this case is unreachable from the popup menus built here.
            case "EUR" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int year = MathUtility.parseInt(chqPanel.getClient().getServerConfigs("CampaignYear"), 3025);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                int greenTechCost;
                int regTechCost;
                int vetTechCost;
                int eliteTechCost;

                double repairCost;

                if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseRealRepairCosts"), false)) {
                    repairCost = UnitUtils.getTotalDamagedPartCost(mek.getEntity(), year);
                    repairCost *= MathUtility.parseDouble(chqPanel.getClient().getServerConfigs("RealRepairCostMod"),
                          0.0);
                } else {
                    repairCost = chqPanel.getClient().getTotalRepairCosts(mek.getEntity());
                }

                greenTechCost = chqPanel.getClient().getTechLaborCosts(mek.getEntity(), UnitUtils.TECH_GREEN);
                regTechCost = chqPanel.getClient().getTechLaborCosts(mek.getEntity(), UnitUtils.TECH_REG);
                vetTechCost = chqPanel.getClient().getTechLaborCosts(mek.getEntity(), UnitUtils.TECH_VET);
                eliteTechCost = chqPanel.getClient().getTechLaborCosts(mek.getEntity(), UnitUtils.TECH_ELITE);

                String repairEstimate = "It'll cost you at least the following to repair your " +
                                              mek.getModelName() +
                                              "<br>" +
                                              String.format("<table><tr><th>Green Tech:</th><th>%s", chqPanel.getClient()
                                                                                              .moneyOrFluMessage(true,
                                                                                                    true,
                                                                                                    (int) repairCost,
                                                                                                    false)) +
                                              String.format("parts and %s", chqPanel.getClient()
                                                                     .moneyOrFluMessage(true,
                                                                           true,
                                                                           greenTechCost,
                                                                           false)) +
                                              String.format(" labor for a total of %s.</th></tr>", chqPanel.getClient()
                                                                                 .moneyOrFluMessage(true,
                                                                                       true,
                                                                                       (int) repairCost + greenTechCost,
                                                                                       false)) +
                                              String.format("<tr><th>Reg Tech: </th>") +
                                              String.format("<th>%s in parts ", chqPanel.getClient()
                                                               .moneyOrFluMessage(true, true, (int) repairCost,
                                                                     false)) +
                                              String.format(" %s in labor for a total ", chqPanel.getClient()
                                                            .moneyOrFluMessage(true,
                                                                  true,
                                                                  regTechCost,
                                                                  false)) +
                                              String.format(" %s.</th>", chqPanel.getClient().moneyOrFluMessage(true, true,
                                                    (int) repairCost + regTechCost, false)) +
                                              String.format("</tr><tr><th>Vet Tech: </th><th>%s in parts and", chqPanel.getClient()
                                                                                           .moneyOrFluMessage(true,
                                                                                                 true,
                                                                                                 (int) repairCost,
                                                                                                 false)) +
                                              String.format("%s in labor for a total of", chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 vetTechCost,
                                                                 false)) +
                                              String.format("%s.</th></tr>", chqPanel.getClient().moneyOrFluMessage(true, true,
                                                    (int) repairCost + vetTechCost, false)) +
                                              String.format("<tr><th>Elite Tech: </th><th>%s in parts and", chqPanel.getClient()
                                                                                        .moneyOrFluMessage(true
                                                                                              ,
                                                                                              true,
                                                                                              (int) repairCost,
                                                                                              false)) +
                                              String.format("%s in labor for a total of ", chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 eliteTechCost,
                                                                 false)) +
                                              String.format("%s.</th></tr></table>", chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 (int) repairCost + eliteTechCost,
                                                                 false));
                chqPanel.getClient().systemMessage(repairEstimate);
            }
            case "RFAA" -> {

                // id of selected unit
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

                // check all armies for the selected unit
                for (CArmy currA : chqPanel.getClient().getPlayer().getArmies()) {
                    if (currA.getUnit(mid) != null) {
                        chqPanel.getClient().sendChat(String.format("%sc EXM#%s,%s", IClient.CAMPAIGN_PREFIX, currA.getID(), mid));
                    }
                }

                // transfer mek
            }
            case "TM" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderTransferUnit_actionPerformed(null, mid);
            }
            case "RM" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc repod#%s", IClient.CAMPAIGN_PREFIX, mid));
                // add to bm
            }
            case "AB" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderAddToBM_actionPerformed(mid);
                // remove from market
            }
            case "RFM" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                TreeMap<Integer, CBMUnit> marketUnits = chqPanel.getClient().getCampaign().getBlackMarket();

                for (CBMUnit currU : marketUnits.values()) {
                    if (currU.getUnitID() == mid) {
                        chqPanel.getClient().sendChat(String.format("%sc recall#%s", IClient.CAMPAIGN_PREFIX, currU.getAuctionID()));
                        break;
                    }
                }
                // direct sell unit
            }
            case "DSU" -> {
                String mid = stringTokenizer.nextToken();
                chqPanel.getClient().getMainFrame().jMenuCommanderDirectSell_actionPerformed(null, mid);
                // scrap mek
            }
            case "S" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int result = JOptionPane.showConfirmDialog(chqPanel.getClient().getMainFrame(),
                      "Are you sure you want to scrap this unit?",
                      "Scrap it?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chqPanel.getClient().sendChat(String.format("%sc scrap#%s", IClient.CAMPAIGN_PREFIX, num));
                    // Maintain Mek
                }
                //@Salient for SOL free build option
            }
            case "DL" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sSOLDELETEUNIT %s", IClient.CAMPAIGN_PREFIX, num));
            }
            case "MM" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc setmaintained#%s", IClient.CAMPAIGN_PREFIX, num));
                chqPanel.getClient().refreshGUI(IClient.REFRESH_HQ_PANEL);
                // unmaintained mek
            }
            case "UMM" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int result = JOptionPane.showConfirmDialog(chqPanel.getClient().getMainFrame(),
                      "Are you sure you want to stop maintaining this unit?",
                      "Un-maintain?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chqPanel.getClient()
                          .sendChat(String.format("%sc setunmaintained#%s", IClient.CAMPAIGN_PREFIX, num));
                }
                chqPanel.getClient().refreshGUI(IClient.REFRESH_HQ_PANEL);
                // donate mek
            }
            case "DO" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int result = JOptionPane.showConfirmDialog(chqPanel.getClient().getMainFrame(),
                      "Are you sure you want to donate this unit?",
                      "Donate?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chqPanel.getClient().sendChat(String.format("%sc donate#%s", IClient.CAMPAIGN_PREFIX, mid));
                    // buy mek
                }
            }
            // LCN ("Link"/"Unlink" C3 network): links unit <mid> to C3 master <hid> within army <lid>; a
            // master id of -1 (as sent by the "Unlink" menu item) disconnects the unit from its current network.
            case "LCN" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int hid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc linkunit#%s#%s#%s", IClient.CAMPAIGN_PREFIX, lid, mid, hid));
            }
            case "EAE" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit cUnit = chqPanel.getMekTable().getMekAt(row, col);
                Mek mek = (Mek) cUnit.getEntity();
                mek.setAutoEject(true);
                chqPanel.getClient()
                      .sendChat(String.format("%sc setautoeject#%s#true", IClient.CAMPAIGN_PREFIX, mek.getExternalId()));
            }
            case "DAE" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit cUnit = chqPanel.getMekTable().getMekAt(row, col);
                Mek mek = (Mek) cUnit.getEntity();
                mek.setAutoEject(false);
                chqPanel.getClient()
                      .sendChat(String.format("%sc setautoeject#%s#false", IClient.CAMPAIGN_PREFIX, mek.getExternalId()));
                // exchange pilot
            }
            // EXP ("Exchange"/"Assign"/"Remove" pilot from the personal pilot queue): assigns queue index
            // <pid> to unit <uid>'s cockpit; a pilot index of -1 (as sent by the "Remove" menu item) vacates
            // the unit's current pilot instead.
            case "EXP" -> {
                int uid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int pid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(String.format("%sc EXP#%s#%s", IClient.CAMPAIGN_PREFIX, uid, pid));
            }
            case "FET" -> // fire excess techs
                  chqPanel.getClient().getMainFrame().jMenuCommanderFireTechs_actionPerformed();
            case "SEB" -> // sell excess bays
                  chqPanel.getClient().getMainFrame().jMenuCommanderSellBays_actionPerformed();
            // RPU ("Move to #N" in the Position menu): reorders unit <unitID> within army <armyID> to roster index <newPos>.
            case "RPU" -> {
                int armyID = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int unitID = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int newPos = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient()
                      .sendChat(String.format("%sc unitposition#%s#%s#%s", IClient.CAMPAIGN_PREFIX, armyID, unitID, newPos));
            }
            // PHQS/SHQS/THQS: persist the chosen primary/secondary/tertiary hangar sort criterion to the
            // client config and immediately re-sort the hangar table.
            case "PHQS" -> {
                chqPanel.getClient().getConfig().setParam("PRIMARY_HQ_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortHangar();
            }
            case "SHQS" -> {
                chqPanel.getClient().getConfig().setParam("SECONDARY_HQ_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortHangar();
            }
            case "THQS" -> {
                chqPanel.getClient().getConfig().setParam("TERTIARY_HQ_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortHangar();
            }
            // PAS/SAS/TAS: same idea as PHQS/SHQS/THQS but for the army list's sort order. Only PAS is
            // reachable from the popup menus in this file (see the note at the top of this method); SAS/TAS
            // remain fully functional here but currently have no menu item to trigger them.
            case "PAS" -> {
                chqPanel.getClient().getConfig().setParam("PRIMARY_ARMY_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortArmies();
            }
            case "SAS" -> {
                chqPanel.getClient().getConfig().setParam("SECONDARY_ARMY_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortArmies();
            }
            case "TAS" -> {
                chqPanel.getClient().getConfig().setParam("TERTIARY_ARMY_SORT_ORDER", stringTokenizer.nextToken());
                chqPanel.getClient().getConfig().saveConfig();
                chqPanel.getClient().getPlayer().sortArmies();
            }
            // REMOVE_UNIT_COMMANDER / SET_UNIT_COMMANDER: intended to toggle whether the clicked unit is set
            // as its army's commander. As documented at the top of this method, the "Remove Commander"/"Set
            // Commander" menu items actually send "REMOVEUNITCOMMANDER"/"SETUNITCOMMANDER" (no underscores),
            // which never match these case labels -- these two cases are therefore currently unreachable
            // dead code from the popup menu's perspective.
            case "REMOVE_UNIT_COMMANDER" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                String armyId = stringTokenizer.nextToken();
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                chqPanel.getClient()
                      .sendChat(String.format("%sc setunitcommander#%s#%s#false", IClient.CAMPAIGN_PREFIX, mek.getId(), armyId));
                // exchange pilot
            }
            case "SET_UNIT_COMMANDER" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                String armyId = stringTokenizer.nextToken();
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                chqPanel.getClient()
                      .sendChat(String.format("%sc setunitcommander#%s#%s#true", IClient.CAMPAIGN_PREFIX, mek.getId(), armyId));
                // exchange pilot
            }
        }

        // Table is repainted unconditionally after the switch, whether or not a case matched, so any
        // GUI-visible state changes made above (or by the server's async response) are reflected promptly.
        chqPanel.getTableMeks().repaint();
    }
}
