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

public class MekTableMouseAdapter extends MouseInputAdapter implements ActionListener {

    private final CHQPanel chqPanel;
    private final Cursor exchangeCursor;
    private final Cursor positionCursor;
    private final Cursor addCursor;
    private final Cursor removeCursor;
    private final Cursor notAllowedCursor;
    private final Cursor dupeCursor;
    private final Cursor maxCursor;
    // VARS
    private boolean isDrag;
    private Image dragImage;
    private Rectangle2D dragRect;
    private Point offset;
    private CUnit dragUnit = null;
    private CArmy startArmy = null;
    private CArmy currArmy = null;

    // CONSTRUCTOR
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
     */
    private void maybeShowPopup(MouseEvent mouseEvent) {
        JPopupMenu popup = new JPopupMenu();
        if (mouseEvent.isPopupTrigger()) {
            int row = chqPanel.getTableMeks().rowAtPoint(mouseEvent.getPoint());
            int col = chqPanel.getTableMeks().columnAtPoint(mouseEvent.getPoint());
            JMenuItem menuItem;

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
                        menuName = STR."<HTML><i>\{menuName}</i></HTML>";
                        // selectionFound = false;
                    }
                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(STR."PHQS|\{choices[i]}");
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
                        menuName = STR."<HTML><i>\{menuName}</i></HTML>";
                        // selectionFound = false;
                    }

                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(STR."SHQS|\{choices[i]}");
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
                        menuName = STR."<HTML><i>\{menuName}</i></HTML>";
                    }

                    menuItem = new JMenuItem(menuName);
                    menuItem.setActionCommand(STR."THQS|\{choices[i]}");
                    menuItem.addActionListener(this);
                    tertiarySortMenu.add(menuItem);

                    if ((i + 2) == choices.length) {
                        tertiarySortMenu.addSeparator();
                    }
                }

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());

            } else if ((row < 0) || (col == 0)) {
                CArmy l = chqPanel.getMekTable().getArmyAt(row);
                if (l != null) {

                    int lid = l.getID();
                    if (l.getBV() > 0) {

                        menuItem = new JMenuItem("Attack Options");
                        menuItem.setActionCommand(STR."AO|\{lid}");
                        menuItem.addActionListener(this);
                        boolean canCheckFromReserve = MathUtility.parseBoolean(chqPanel.getClient()
                                                                                     .getServerConfigs("ProbeInReserve"),
                              false);
                        if ((chqPanel.getClient().getMyStatus() != IClient.STATUS_ACTIVE) && !canCheckFromReserve) {
                            menuItem.setEnabled(false);
                        }
                        popup.add(menuItem);

                        menuItem = new JMenuItem("Check Access");
                        menuItem.setActionCommand(STR."CAA|\{lid}");
                        menuItem.addActionListener(this);
                        popup.add(menuItem);

                        // only show "Limits" option if limits allowed
                        boolean limitsAllowed = MathUtility.parseBoolean(chqPanel.getClient()
                                                                               .getServerConfigs("AllowLimiters"),
                              false);
                        if (limitsAllowed) {
                            JMenu limitMenu = new JMenu("Limits");
                            popup.add(limitMenu);

                            menuItem = new JMenuItem("Set Lower Unit Limit");
                            menuItem.setActionCommand(STR."SLUL|\{lid}");
                            menuItem.addActionListener(this);
                            limitMenu.add(menuItem);

                            menuItem = new JMenuItem("Set Upper Unit Limit");
                            menuItem.setActionCommand(STR."SUUL|\{lid}");
                            menuItem.addActionListener(this);
                            limitMenu.add(menuItem);
                        }

                        // Only show when Force Size is used.
                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseOperationsRule"),
                              false)) {
                            menuItem = new JMenuItem("Force Size To Face");
                            popup.add(menuItem);
                            menuItem.setActionCommand(STR."SFS|\{lid}");
                            menuItem.addActionListener(this);
                        }

                        AttackMenu aMenu = new AttackMenu(chqPanel.getClient(), lid, "-1");
                        aMenu.updateMenuItems(false);
                        popup.add(aMenu);

                        popup.addSeparator();
                    }

                    menuItem = new JMenuItem("Lock Army");
                    menuItem.setActionCommand(STR."LA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (chqPanel.getClient().getPlayer().getArmy(lid).isPlayerLocked()) {
                        menuItem.setVisible(false);
                    }

                    menuItem = new JMenuItem("Unlock Army");
                    menuItem.setActionCommand(STR."ULA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (!chqPanel.getClient().getPlayer().getArmy(lid).isPlayerLocked()) {
                        menuItem.setVisible(false);
                    }

                    menuItem = new JMenuItem("Remove Army");
                    menuItem.setActionCommand(STR."RA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    menuItem = new JMenuItem("Rename Army");
                    menuItem.setActionCommand(STR."NA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    menuItem = new JMenuItem("Disable Army");
                    menuItem.setActionCommand(STR."DAA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (chqPanel.getClient().getPlayer().getArmy(lid).isDisabled()) {
                        menuItem.setVisible(false);
                    }

                    menuItem = new JMenuItem("Enable Army");
                    menuItem.setActionCommand(STR."DAA|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (!chqPanel.getClient().getPlayer().getArmy(lid).isDisabled()) {
                        menuItem.setVisible(false);
                    }

                    JMenu primeSortMenu = new JMenu("Sort (1st)");

                    popup.add(primeSortMenu);

                    // Choices [note - this array must be duplicated in
                    String[] choices = { "Name", "Battle Value", "ID Number", "Max Tonnage", "Avg Walk MP",
                                         "Avg Jump MP", "Number Of Units", "No Sort" };

                    // indicate current selections w/ Italics
                    String menuName;

                    // prime sort menu construction
                    for (int i = 0; i < choices.length; i++) {
                        menuName = choices[i];
                        if (chqPanel.getClient()
                                  .getConfigParam("PRIMARY_ARMY_SORT_ORDER")
                                  .equalsIgnoreCase(choices[i])) {
                            menuName = STR."<HTML><i>\{menuName}</i></HTML>";
                        }

                        menuItem = new JMenuItem(menuName);
                        menuItem.setActionCommand(STR."PAS|\{choices[i]}");
                        menuItem.addActionListener(this);
                        primeSortMenu.add(menuItem);

                        if ((i + 2) == choices.length) {
                            primeSortMenu.addSeparator();
                        }
                    }

                    // reset selectionFound

                    popup.addSeparator();

                    menuItem = new JMenuItem("Show To Faction");
                    menuItem.setActionCommand(STR."SATH|\{lid}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // disable show olfaction if army has 0 units
                    if (chqPanel.getClient().getPlayer().getArmy(lid).getUnits().isEmpty()) {
                        menuItem.setEnabled(false);
                    }

                    CArmy army = chqPanel.getClient().getPlayer().getArmy(lid);

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

                    JMenu submenu = new JMenu("Unit");

                    JMenu requestMenu = new JMenu("BV Only");

                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|1|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String op : army.getLegalOperations()) {
                        menuItem = new JMenuItem(op);
                        menuItem.setActionCommand(STR."MPC|1|\{lid}|\{op}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Count and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|2|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(STR."MPC|2|\{lid}|\{operation}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Classes and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|3|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(STR."MPC|3|\{lid}|\{operation}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Total Weight");
                    requestMenu = new JMenu("Total Weight");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|4|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(STR."MPC|4|\{lid}|\{operation}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|5|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(STR."MPC|5|\{lid}|\{operation}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight and Unit Count");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|6|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operation : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operation);
                        menuItem.setActionCommand(STR."MPC|6|\{lid}|\{operation}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Total Weight, Unit Count and BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|7|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|7|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Unit Types");
                    requestMenu = new JMenu("Unit Types");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|8|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|8|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Types with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|9|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|9|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Unit Models");
                    requestMenu = new JMenu("Unit Models");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|10|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|10|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Unit Models with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|11|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|11|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

                    submenu = new JMenu("Actual Weight");
                    requestMenu = new JMenu("Actual Unit Weights");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|12|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|12|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);

                    requestMenu = new JMenu("Actual Unit Weights with BV");
                    menuItem = new JMenuItem("None");
                    menuItem.setActionCommand(STR."MPC|13|\{lid}|none");
                    menuItem.addActionListener(this);
                    requestMenu.add(menuItem);

                    for (String operations : army.getLegalOperations()) {
                        menuItem = new JMenuItem(operations);
                        menuItem.setActionCommand(STR."MPC|13|\{lid}|\{operations}");
                        menuItem.addActionListener(this);
                        requestMenu.add(menuItem);
                    }

                    submenu.add(requestMenu);
                    singleArmy.add(submenu);

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
            } else if (row < chqPanel.getMekTable().getRowsForArmies()) {
                CUnit cUnit;
                CArmy cArmy = chqPanel.getMekTable().getArmyAt(row);
                int mid;
                int lid = cArmy.getID();
                cUnit = chqPanel.getMekTable().getMekAt(row, col);
                boolean hasUnitsFree = false;

                /*
                 * CONSTRUCT the ADD menu here. It will be added to the actual format later. @urgru 12/7/04
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
                                    menuItem = new JMenuItem(STR."\{element.getModelName()} (\{element.getPilot()
                                                                                                     .getGunnery()}/\{element.getPilot()
                                                                                                                            .getPiloting()}) \{element.getBVForMatch()} BV");
                                } else if ((element.getType() == Unit.INFANTRY) ||
                                                 (element.getType() == Unit.BATTLEARMOR)) {
                                    if (((Infantry) element.getEntity()).canMakeAntiMekAttacks()) {
                                        menuItem = new JMenuItem(STR."\{element.getModelName()} (\{element.getPilot()
                                                                                                         .getGunnery()}/\{element.getPilot()
                                                                                                                                .getPiloting()}) \{element.getBVForMatch()} BV");
                                    } else {
                                        menuItem = new JMenuItem(STR."\{element.getModelName()} (\{element.getPilot()
                                                                                                         .getGunnery()}) \{element.getBVForMatch()} BV");
                                    }
                                } else {
                                    menuItem = new JMenuItem(STR."\{element.getModelName()} (\{element.getPilot()
                                                                                                     .getGunnery()}) \{element.getBVForMatch()} BV");
                                }
                                menuItem.setActionCommand(STR."EXM|\{lid}|-1|\{element.getId()}");
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
                                menuItem = new JMenuItem(STR."\{mm.getModelName()} \{mm.getBVForMatch()} BV");
                                menuItem.setActionCommand(STR."LCN|\{lid}|\{cUnit.getId()}|\{mm.getId()}");
                                menuItem.addActionListener(this);
                                linkMenu.add(menuItem);
                            }
                        }
                    }// end Link menu construction

                    // Link menu has been preformed. Proceed with the usual
                    // bits.
                    mid = cUnit.getId();

                    // move to hangar
                    if (!cArmy.isLocked()) {
                        String text = "Move To Hangar";
                        menuItem = new JMenuItem(text);
                        menuItem.setActionCommand(STR."MH|\{lid}|\{mid}");
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    /*
                     * EXCHANGE. Derived from ADD. Same, but returns clicked unit to hangar.
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
                                        menuItem = new JMenuItem(STR."\{mek.getModelName()} (\{mek.getPilot()
                                                                                                     .getGunnery()}/\{mek.getPilot()
                                                                                                                            .getPiloting()}) \{mek.getBVForMatch()} BV");
                                    } else if ((mek.getType() == Unit.INFANTRY) ||
                                                     (mek.getType() == Unit.BATTLEARMOR)) {
                                        if (((Infantry) mek.getEntity()).canMakeAntiMekAttacks()) {
                                            menuItem = new JMenuItem(STR."\{mek.getModelName()} (\{mek.getPilot()
                                                                                                         .getGunnery()}/\{mek.getPilot()
                                                                                                                                .getPiloting()}) \{mek.getBVForMatch()} BV");
                                        } else {
                                            menuItem = new JMenuItem(STR."\{mek.getModelName()} (\{mek.getPilot()
                                                                                                         .getGunnery()}) \{mek.getBVForMatch()} BV");
                                        }
                                    } else {
                                        menuItem = new JMenuItem(STR."\{mek.getModelName()} (\{mek.getPilot()
                                                                                                     .getGunnery()}) \{mek.getBVForMatch()} BV");
                                    }
                                    menuItem.setActionCommand(STR."EXM|\{lid}|\{cUnit.getId()}|\{mek.getId()}");
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
                                            menuX = new JMenu(STR."\{Unit.getWeightClassDesc(i)} \{j + 1}");
                                        } else if (i == 4) {// proto
                                            menuX = new JMenu(STR."Proto \{j + 1}");
                                        } else {// BA, assume an i of 5
                                            menuX = new JMenu(STR."Infantry \{j + 1}");
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
                         */
                        if (cArmy.getAmountOfUnits() > 1) {
                            JMenu positionMenu = new JMenu("Position");
                            popup.add(positionMenu);
                            int currPos = 0;
                            for (Unit unit : cArmy.getUnits()) {
                                CUnit currUnit = (CUnit) unit;
                                if (currUnit.getId() != mid) {
                                    menuItem = new JMenuItem(STR."Move to #\{currPos + 1}");
                                    menuItem.setActionCommand(STR."RPU|\{lid}|\{cUnit.getId()}|\{currPos}");
                                    menuItem.addActionListener(this);
                                    positionMenu.add(menuItem);
                                }
                                currPos++;
                            }
                        }// end position menu construction

                    }// end code black for Exchange AND Add AND Position
                    if (cUnit.getC3Level() != Unit.C3_NONE) {
                        popup.add(linkMenu);
                    }
                    if (cUnit.hasBeenC3LinkedTo(cArmy) || (cArmy.getC3Network().get(cUnit.getId()) != null)) {
                        menuItem = new JMenuItem("Unlink");
                        menuItem.setActionCommand(STR."LCN|\{lid}|\{cUnit.getId()}|-1");
                        menuItem.addActionListener(this);
                        popup.add(menuItem);

                    }
                    // divide army composition/unit display options.
                    popup.addSeparator();

                    // Add Show Mek Option
                    menuItem = new JMenuItem("View Unit");
                    menuItem.setActionCommand(STR."SM|\{row}|\{col}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Customize Unit Option
                    menuItem = new JMenuItem("Customize Unit");
                    menuItem.setActionCommand(STR."CMU|\{row}|\{col}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Auto Eject Option
                    if (cUnit.getEntity() instanceof Mek mek) {
                        if (mek.isAutoEject()) {
                            menuItem = new JMenuItem("Disable Auto Eject");
                            menuItem.setActionCommand(STR."DAE|\{row}|\{col}");
                        } else {
                            menuItem = new JMenuItem("Enable Auto Eject");
                            menuItem.setActionCommand(STR."EAE|\{row}|\{col}");
                        }
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    if (cArmy.isCommander(cUnit.getId())) {
                        menuItem = new JMenuItem("Remove Commander");
                        menuItem.setActionCommand(STR."REMOVEUNITCOMMANDER|\{row}|\{col}|\{lid}");
                    } else {
                        menuItem = new JMenuItem("Set Commander");
                        menuItem.setActionCommand(STR."SETUNITCOMMANDER|\{row}|\{col}|\{lid}");
                    }
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                }// end if (cUnit in click area != null)
                else {
                    popup.add(addMenu);
                }

                popup.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
            } else {
                CUnit cm = chqPanel.getMekTable().getMekAt(row, col);
                if (cm != null) {

                    menuItem = new JMenuItem("View Unit");
                    menuItem.setActionCommand(STR."SM|\{row}|\{col}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    // Add Customize Unit Option
                    menuItem = new JMenuItem("Customize Unit");
                    menuItem.setActionCommand(STR."CMU|\{row}|\{col}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    if (chqPanel.useAdvanceRepairs()) {

                        JMenu repairs = new JMenu("Repairs");
                        if (UnitUtils.hasArmorDamage(cm.getEntity()) ||
                                  UnitUtils.hasCriticalDamage(cm.getEntity())) {
                            if (!Boolean.parseBoolean(chqPanel.getClient().getServerConfigs("UseSimpleRepair"))) {
                                // Add repair unit option
                                menuItem = new JMenuItem("Repair Unit");
                                menuItem.setActionCommand(STR."ARU|\{row}|\{col}");
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);

                                menuItem = new JMenuItem("Bulk Repair");
                                menuItem.setActionCommand(STR."BUR|\{row}|\{col}");
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);
                            } else {
                                menuItem = new JMenuItem("Repair Unit");
                                menuItem.setActionCommand(STR."SUR|\{row}|\{col}");
                                menuItem.addActionListener(this);
                                repairs.add(menuItem);
                            }

                        }

                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UsePartsRepair"), false) &&
                                  ((cm.getType() == Unit.MEK) || (cm.getType() == Unit.VEHICLE))) {
                            menuItem = new JMenuItem("Salvage Unit Crits");
                            menuItem.setActionCommand(STR."SUC|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);

                            menuItem = new JMenuItem("Bulk Salvage");
                            menuItem.setActionCommand(STR."BSU|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        if (UnitUtils.isRepairing(cm.getEntity())) {
                            // Add display repair job option
                            menuItem = new JMenuItem("Display Repair Jobs");
                            menuItem.setActionCommand(STR."DRJ|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        if (((chqPanel.getClient().getRMT() != null) &&
                                   chqPanel.getClient().getRMT().hasQueuedOrders(cm.getId())) ||
                                  ((chqPanel.getClient().getSMT() != null) &&
                                         chqPanel.getClient().getSMT().hasQueuedOrders(cm.getId()))) {
                            // Add display pending job option
                            menuItem = new JMenuItem("Display Pending Work Orders");
                            menuItem.setActionCommand(STR."DPWO|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);

                            // Add stop all pending jobs
                            menuItem = new JMenuItem("Stop All Pending Work Orders");
                            menuItem.setActionCommand(STR."SAPWO|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        if (!UnitUtils.hasAllAmmo(cm.getEntity())) {
                            menuItem = new JMenuItem("Reload All Ammo");
                            menuItem.setActionCommand(STR."RAA|\{row}|\{col}");
                            menuItem.addActionListener(this);
                            repairs.add(menuItem);
                        }

                        if (repairs.getItemCount() > 0) {
                            popup.add(repairs);
                        }
                    }

                    // Add Auto eject Option
                    if (cm.getEntity() instanceof Mek mek) {
                        if (mek.isAutoEject()) {
                            menuItem = new JMenuItem("Disable Auto-eject");
                            menuItem.setActionCommand(STR."DAE|\{row}|\{col}");
                        } else {
                            menuItem = new JMenuItem("Enable Auto-eject");
                            menuItem.setActionCommand(STR."EAE|\{row}|\{col}");
                        }

                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    popup.addSeparator();

                    if (!chqPanel.useAdvanceRepairs()) {
                        if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                            menuItem = new JMenuItem("Maintain");
                            menuItem.setActionCommand(STR."MM|\{cm.getId()}");
                        } else {
                            menuItem = new JMenuItem("Unmaintained");
                            menuItem.setActionCommand(STR."UMM|\{cm.getId()}");
                        }

                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }
                    if (cm.isOmni()) {
                        menuItem = new JMenuItem("RePod Unit");
                        menuItem.setActionCommand(STR."RM|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        popup.add(menuItem);
                    }

                    JMenu transactionsMenu = new JMenu("Transactions");
                    int numItems = 0;
                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowDonate")
                                    , false)) {
                        menuItem = new JMenuItem("Donate Unit");
                        menuItem.setActionCommand(STR."DO|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }
                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowScrap"),
                                    false)) {
                        menuItem = new JMenuItem("Scrap Unit");
                        menuItem.setActionCommand(STR."S|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }
                    //@Salient for SOL free build
                    if (chqPanel.getPlayer().getHouse()
                              .equalsIgnoreCase(chqPanel.getClient().getServerConfigs("NewbieHouseName")) &&
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Sol_FreeBuild"), false)) {
                        menuItem = new JMenuItem("Delete Unit");
                        menuItem.setActionCommand(STR."DL|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }

                    if (!cm.isChristmasUnit() ||
                              MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("Christmas_AllowTransfer"),
                                    false)) {
                        menuItem = new JMenuItem("Transfer Unit");
                        menuItem.setActionCommand(STR."TM|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                        numItems++;
                    }

                    if (numItems > 0) {
                        popup.add(transactionsMenu);
                    }

                    // Test unit for BM access
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
                        menuItem.setActionCommand(STR."AB|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    if (cm.getStatus() == Unit.STATUS_FOR_SALE) {
                        menuItem = new JMenuItem("Recall from BM");
                        menuItem.setActionCommand(STR."RFM|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseDirectSell"), false) &&
                              (cm.getStatus() != Unit.STATUS_FOR_SALE)) {
                        menuItem = new JMenuItem("Direct Sell Unit");
                        menuItem.setActionCommand(STR."DSU|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        transactionsMenu.add(menuItem);
                    }

                    JMenu pilotMenu = new JMenu("Pilot");
                    popup.add(pilotMenu);
                    // Cannot Retire or rename Vacant pilots.
                    if (!cm.hasVacantPilot()) {
                        menuItem = new JMenuItem("Retire");
                        menuItem.setActionCommand(STR."RT|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        pilotMenu.add(menuItem);

                        menuItem = new JMenuItem("Rename");
                        menuItem.setActionCommand(STR."RP|\{cm.getId()}");
                        menuItem.addActionListener(this);
                        pilotMenu.add(menuItem);

                        if (MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs(
                              "PlayersCanBuyPilotUpgrades"), false)) {
                            menuItem = new JMenuItem("Promote Pilot");
                            menuItem.setActionCommand(STR."PP|\{cm.getId()}");
                            menuItem.addActionListener(this);
                            pilotMenu.add(menuItem);

                            if (MathUtility.parseBoolean(chqPanel.getClient()
                                                               .getServerConfigs("PlayersCanSellPilotUpgrades"),
                                  false)) {
                                menuItem = new JMenuItem("Demote Pilot");
                                menuItem.setActionCommand(STR."DP|\{cm.getId()}");
                                menuItem.addActionListener(this);
                                pilotMenu.add(menuItem);
                            }
                        }
                    }

                    // Pilot Queues Block
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
                            menuItem.setActionCommand(STR."EXP|\{cm.getId()}|-1");
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

                                menuItem.setActionCommand(STR."EXP|\{cm.getId()}|\{i}");
                                menuItem.addActionListener(this);
                                exchangeMenu.add(menuItem);
                            }
                        }
                    }

                    popup.addSeparator();

                    menuItem = new JMenuItem("Show To Faction");
                    menuItem.setActionCommand(STR."SUTH|\{cm.getId()}");
                    menuItem.addActionListener(this);
                    popup.add(menuItem);

                    menuItem = new JMenuItem("Remove From All");
                    menuItem.setActionCommand(STR."RFAA|\{cm.getId()}");
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

    private static @NonNull JMenu getMenuX(int i, int j) {
        JMenu menuX;
        if (i < 4) {
            menuX = new JMenu(STR."\{Unit.getWeightClassDesc(i)} \{j + 1}");
        } else if (i == 4) {// proto
            menuX = new JMenu(STR."Proto \{j + 1}");
        } else {// BA, can assume this is i ==
            // 5.
            menuX = new JMenu(STR."Infantry \{j + 1}");
        }
        return menuX;
    }

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

    private static @NonNull String getPilotString(CUnit cm, Pilot pilot) {
        String pilotString;
        String skills = pilot.getSkillString(true);
        if (cm.getType() == Unit.MEK) {
            pilotString = STR."\{pilot.getName()} (\{pilot.getGunnery()}/\{pilot.getPiloting()}";
            if (skills.trim().isEmpty()) {
                pilotString += ")";
            } else {
                pilotString += STR.", \{skills})";
            }

            if (pilot.getHits() > 0) {
                pilotString += STR." Hits: \{pilot.getHits()}";
            }

        } else {
            pilotString = STR."\{pilot.getName()} (\{pilot.getGunnery()}";
            if (skills.trim().isEmpty()) {
                pilotString += ")";
            } else {
                pilotString += STR.", \{skills})";
            }
        }
        return pilotString;
    }

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
                          .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{startArmy.getID()},\{dragUnit.getId()}");
                }

            }// end if (release over hangar)

            // finish army exists
            else if (validRelease) {

                // from hangar to an army
                if (startArmy == null) {

                    // army # or empty space. add the unit.
                    if (exchangeUnit == null) {
                        chqPanel.getClient().sendChat(
                              STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{currArmy.getID()},-1#\{dragUnit.getId()}");
                    } else if (dragUnit.getId() != exchangeUnit.getId()) {
                        chqPanel.getClient()
                              .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{currArmy.getID()},\{exchangeUnit.getId()}#\{dragUnit.getId()}");
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
                          STR."\{IClient.CAMPAIGN_PREFIX}c unitposition#\{startArmy.getID()}#\{dragUnit.getId()}#\{newPosition}");
                }

            }// end else(target army exists)

            // revert to normal cursor
            chqPanel.getTableMeks().setCursor(Cursor.getDefaultCursor());

        }// end if(isDrag)

        isDrag = false;
        maybeShowPopup(mouseEvent);
    }

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

    public void actionPerformed(ActionEvent actionEvent) {

        String actionCommand = actionEvent.getActionCommand();
        StringTokenizer stringTokenizer = new StringTokenizer(actionCommand, "|");
        String command = stringTokenizer.nextToken().toUpperCase();

        // exchange mek
        switch (command) {
            case "EXM" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int hid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{lid},\{mid}#\{hid}");
                // move to hanger
            }
            case "MH" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{lid},\{mid}");
                // add lance
            }
            case "AA" -> chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c cra#\{chqPanel.getClient()
                                                                                                    .getConfigParam(
                                                                                                          "DEFAULT_ARMY_NAME")}");

            // set lance active
            case "SA" -> {
            }
            case "SI" -> {
            }
            case "AO" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderCheckAttack_actionPerformed(lid);
                // check access
            }
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
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c checkarmyeligibility#\{armyID}#\{attackName}");
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
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c sth#a#\{lid}");
                // make public challenge
            }
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
                        toSend.append(STR." \{Math.round(currArmy.getBV() * forceSizeMod)} BV");

                        if (forceSizeMod > 1) {
                            toSend.append(STR." vs \{opForceSize} units");
                        }

                        toSend.append(".");
                    } else if (mode == 2) {
                        int armySize = currArmy.getUnits().size();
                        toSend.append(" ").append(Math.round(currArmy.getBV() * forceSizeMod)).append(" BV");

                        if (forceSizeMod > 1) {
                            toSend.append(" vs ").append(opForceSize).append(" units");
                        }

                        toSend.append(", with  ").append(armySize).append(" unit");

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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");

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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                        toSend = new StringBuilder(STR."\{toSend.substring(0, sendLength)}.");
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
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c sth#u#\{mid}");
                // rename pilot
            }
            case "RP" -> {
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().getMainFrame().jMenuCommanderNamePilot_actionPerformed(mid);
                // Promote Pilot
            }
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
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c retirepilot#\{mid}");// send
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
            case "ARU" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new AdvancedRepairDialog(chqPanel.getClient(), mek.getId(), false);
            }
            case "BUR" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_BULK,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
            case "SUR" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_SIMPLE,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
            case "BSU" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                new BulkRepairDialog(chqPanel.getClient(),
                      mek.getId(),
                      BulkRepairDialog.TYPE_SALVAGE,
                      BulkRepairDialog.UNIT_TYPE_SINGLE);
            }
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

                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c DisplayUnitRepairJobs#\{mek.getId()}");
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
                      STR."Are you sure you want to reload all the ammo on this unit \{chqPanel.getClient()
                                                                                             .getPlayer()
                                                                                             .getName()}?",
                      "Reload it?",
                      JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    chqPanel.getClient()
                          .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c RELOADALLAMMO#\{mek.getId()}");
                }
            }

            // Estimate Unit Repairs
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
                                              STR."<table><tr><th>Green Tech:</th><th>\{chqPanel.getClient()
                                                                                              .moneyOrFluMessage(true,
                                                                                                    true,
                                                                                                    (int) repairCost,
                                                                                                    false)}" +
                                              STR."parts and \{chqPanel.getClient()
                                                                     .moneyOrFluMessage(true,
                                                                           true,
                                                                           greenTechCost,
                                                                           false)}" +
                                              STR." labor for a total of \{chqPanel.getClient()
                                                                                 .moneyOrFluMessage(true,
                                                                                       true,
                                                                                       (int) repairCost + greenTechCost,
                                                                                       false)}.</th></tr>" +
                                              STR."<tr><th>Reg Tech: </th>" +
                                              STR."<th>\{chqPanel.getClient()
                                                               .moneyOrFluMessage(true, true, (int) repairCost,
                                                                     false)} in parts " +
                                              STR." \{chqPanel.getClient()
                                                            .moneyOrFluMessage(true,
                                                                  true,
                                                                  regTechCost,
                                                                  false)} in labor for a total " +
                                              STR." \{chqPanel.getClient().moneyOrFluMessage(true, true,
                                                    (int) repairCost + regTechCost, false)}.</th>" +
                                              STR."</tr><tr><th>Vet Tech: </th><th>\{chqPanel.getClient()
                                                                                           .moneyOrFluMessage(true,
                                                                                                 true,
                                                                                                 (int) repairCost,
                                                                                                 false)} in parts and" +
                                              STR."\{chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 vetTechCost,
                                                                 false)} in labor for a total of" +
                                              STR."\{chqPanel.getClient().moneyOrFluMessage(true, true,
                                                    (int) repairCost + vetTechCost, false)}.</th></tr>" +
                                              STR."<tr><th>Elite Tech: </th><th>\{chqPanel.getClient()
                                                                                        .moneyOrFluMessage(true
                                                                                              ,
                                                                                              true,
                                                                                              (int) repairCost,
                                                                                              false)} in parts and" +
                                              STR."\{chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 eliteTechCost,
                                                                 false)} in labor for a total of " +
                                              STR."\{chqPanel.getClient()
                                                           .moneyOrFluMessage(true,
                                                                 true,
                                                                 (int) repairCost + eliteTechCost,
                                                                 false)}.</th></tr></table>";
                chqPanel.getClient().systemMessage(repairEstimate);
            }
            case "RFAA" -> {

                // id of selected unit
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);

                // check all armies for the selected unit
                for (CArmy currA : chqPanel.getClient().getPlayer().getArmies()) {
                    if (currA.getUnit(mid) != null) {
                        chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXM#\{currA.getID()},\{mid}");
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
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c repod#\{mid}");
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
                        chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c recall#\{currU.getAuctionID()}");
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
                    chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c scrap#\{num}");
                    // Maintain Mek
                }
                //@Salient for SOL free build option
            }
            case "DL" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}SOLDELETEUNIT \{num}");
            }
            case "MM" -> {
                int num = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setmaintained#\{num}");
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
                          .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setunmaintained#\{num}");
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
                    chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c donate#\{mid}");
                    // buy mek
                }
            }
            case "LCN" -> {
                int lid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int mid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int hid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c linkunit#\{lid}#\{mid}#\{hid}");
            }
            case "EAE" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit cUnit = chqPanel.getMekTable().getMekAt(row, col);
                Mek mek = (Mek) cUnit.getEntity();
                mek.setAutoEject(true);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setautoeject#\{mek.getExternalId()}#true");
            }
            case "DAE" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                CUnit cUnit = chqPanel.getMekTable().getMekAt(row, col);
                Mek mek = (Mek) cUnit.getEntity();
                mek.setAutoEject(false);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setautoeject#\{mek.getExternalId()}#false");
                // exchange pilot
            }
            case "EXP" -> {
                int uid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int pid = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient().sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c EXP#\{uid}#\{pid}");
            }
            case "FET" -> // fire excess techs
                  chqPanel.getClient().getMainFrame().jMenuCommanderFireTechs_actionPerformed();
            case "SEB" -> // sell excess bays
                  chqPanel.getClient().getMainFrame().jMenuCommanderSellBays_actionPerformed();
            case "RPU" -> {
                int armyID = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int unitID = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int newPos = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c unitposition#\{armyID}#\{unitID}#\{newPos}");
            }
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
            case "REMOVE_UNIT_COMMANDER" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                String armyId = stringTokenizer.nextToken();
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setunitcommander#\{mek.getId()}#\{armyId}#false");
                // exchange pilot
            }
            case "SET_UNIT_COMMANDER" -> {
                int row = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                int col = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
                String armyId = stringTokenizer.nextToken();
                CUnit mek = chqPanel.getMekTable().getMekAt(row, col);
                chqPanel.getClient()
                      .sendChat(STR."\{IClient.CAMPAIGN_PREFIX}c setunitcommander#\{mek.getId()}#\{armyId}#true");
                // exchange pilot
            }
        }

        chqPanel.getTableMeks().repaint();
    }
}
