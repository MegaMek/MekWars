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

package mekwars.common.gui.models;

import java.io.Serial;
import java.util.Vector;
import javax.swing.table.AbstractTableModel;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.Army;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.gui.panels.CHQPanel;

/**
 * Table model backing the unit/army grid in {@link CHQPanel} (the player's "HQ" hangar screen).
 * This is not a conventional row-per-entity table: it lays out a fixed number of unit "slots" per
 * row (driven by the {@code UNIT_AMOUNT} server config), with column 0 reserved for an army
 * summary card and the remaining columns holding one unit each.
 * <p>
 * Rows are packed in blocks: each {@link CArmy} the player owns occupies
 * {@code ceil(unitCount / unitColumns)} rows (minimum 1, so even an empty army still shows a row),
 * one after another, followed by a block of "hangar" rows listing units not currently assigned to
 * any army plus a trailing indicator cell showing free repair bays or idle technicians. Column 0
 * shows the owning army's summary (name, BV, tonnage, locked/disabled state, unit-count limiter
 * range) on only the first row of that army's block, and simply "Hangar" once past all army rows.
 * Use {@link #getArmyAt(int)} and {@link #getMekAt(int, int)} to translate a grid cell back into
 * the {@link CArmy}/{@link CUnit} it represents.
 */
public class MekTableModel extends AbstractTableModel {
    private static final MMLogger LOGGER = MMLogger.create(MekTableModel.class);

    @Serial
    private static final long serialVersionUID = -7918520064078379615L;

    /** Owning HQ panel; supplies the client, player, armies, and hangar this model displays. */
    final CHQPanel chqPanel;

    /** @param chqPanel the HQ panel this model backs */
    public MekTableModel(CHQPanel chqPanel) {
        this.chqPanel = chqPanel;
    }

    /**
     * @return true if {@code armyName} is one of the recognized "no real name set" placeholder
     * values (blank, a single space, or case-insensitively "no name"/"none"/"clear"/"untitled"), in
     * which case {@link #getValueAt(int, int)} skips displaying it as a custom army name.
     */
    private static boolean isFakeName(String armyName) {
        boolean fakeName = false;

        if (armyName.isEmpty() || armyName.equals(" ")) {
            fakeName = true;
        } else if (armyName.equalsIgnoreCase("no name")) {
            fakeName = true;
        } else if (armyName.equalsIgnoreCase("none")) {
            fakeName = true;
        } else if (armyName.equalsIgnoreCase("clear")) {
            fakeName = true;
        } else if (armyName.equalsIgnoreCase("untitled")) {
            fakeName = true;
        }
        return fakeName;
    }

    // should be based on the number of meks you can own
    /** @return total grid rows: all army blocks ({@link #getRowsForArmies()}) followed by the
     *  hangar block ({@link #getRowsForHangar()}). */
    public int getRowCount() {
        int hangarRows = getRowsForHangar();
        int armyRows = getRowsForArmies();
        return hangarRows + armyRows;
    }

    /** @return number of unit columns (from the {@code UNIT_AMOUNT} server config) plus one for the
     *  leading army/label column. */
    public int getColumnCount() {
        return Integer.parseInt(chqPanel.getClient().getConfigParam("UNIT_AMOUNT")) + 1;
    }

    /**
     * Builds the display text for a single grid cell.
     * <p>
     * For {@code col == 0}: while still within an army's row block, returns an HTML "army card"
     * (army number, locked/disabled flags, custom name if not a {@link #isFakeName(String) fake
     * name}, BV (and operations-rule-adjusted BV in parentheses when applicable), unit-count
     * limiter range, and total tonnage) — but only on the first physical row of that army's block;
     * subsequent rows of a multi-row army return {@code ""} for column 0. Once past all army rows,
     * returns the literal string {@code "Hangar"}.
     * <p>
     * For other columns: resolves the {@link CUnit} at this cell via {@link #getMekAt(int, int)}.
     * If there is no unit there but the row still belongs to an army, returns {@code " - "}
     * (empty slot placeholder). If in the hangar section, the first empty slot immediately after
     * the last real hangar unit instead shows a "Free Bays: N" or "Idle Techs: N" summary
     * (depending on whether advanced repairs are enabled); all other empty hangar slots return
     * {@code ""}. When a unit is present, the returned text is the unit's model name plus:
     * one {@code *} per pilot skill listed in its skill string, a {@code |M|}/{@code |L|} suffix if
     * the unit is a C3 master/linked member of its army's network, and a trailing {@code " Cmdr"}
     * suffix if the unit is the army's commander and neither the "RIGHT_COMMANDER" nor
     * "LEFT_COMMANDER" icon-based indicator config is enabled (i.e. this text marker is only a
     * fallback for when no icon already conveys commander status).
     */
    public String getValueAt(int row, int col) {
        if (row < 0) {
            return "";
        }

        if (col == 0) {
            if (row < getRowsForArmies()) {
                CArmy army = getArmyAt(row);

                // only return the army description on the 1st row
                // ie - return w/ no content on 2nd/3rd/etc. row
                if (getRowsForArmy(army) > 1) {

                    // yes, i know this code blows. sod off.
                    int rowsUsed = 0;

                    for (CArmy currArmy : chqPanel.getPlayer().getArmies()) {
                        if ((currArmy.getID() == army.getID()) && (rowsUsed != row)) {
                            return "";
                        }
                        // else
                        rowsUsed += getRowsForArmy(currArmy);
                    }// end while
                }

                int lid = army.getID();
                String range = "";

                boolean limitsAllowed = Boolean.parseBoolean(chqPanel.getClient().getServerConfigs("AllowLimiters"));
                if (limitsAllowed) {
                    // lower limit
                    if (army.getLowerLimiter() == Army.NO_LIMIT) {
                        range = "No Lower";
                    } else if ((army.getAmountOfUnits() - army.getLowerLimiter()) < 1) {
                        range = "1";
                    } else {
                        range = String.format("%s", army.getAmountOfUnits() - army.getLowerLimiter());
                    }

                    // divider
                    range += " - ";

                    // upper limit
                    if (army.getUpperLimiter() == Army.NO_LIMIT) {
                        range += "No Upper";
                    } else {
                        range += String.format("%s", army.getAmountOfUnits() + army.getUpperLimiter());
                    }

                    // overwrite if there are no limits at all
                    if ((army.getLowerLimiter() == Army.NO_LIMIT) && (army.getUpperLimiter() == Army.NO_LIMIT)) {
                        range = "No Limits";
                    }
                }

                String armyName = army.getName();
                if (armyName.length() > 11) {
                    armyName = armyName.substring(0, 11);
                }

                String toReturn = String.format("<html><center><b>Army #%s</b><br>", lid);
                if (army.isPlayerLocked()) {
                    toReturn += "(locked)<br>";
                }

                if (army.isDisabled()) {
                    toReturn += "(disabled)<br>";
                }

                // only show army name if one is actually set
                boolean fakeName = isFakeName(armyName);

                if (!fakeName) {
                    if (armyName.length() > 10) {
                        toReturn += String.format("%s...<br>", armyName.subSequence(0, 9));
                    } else {
                        toReturn += String.format("%s<br>", armyName);
                    }
                }

                boolean useOpRule = MathUtility.parseBoolean(chqPanel.getClient().getServerConfigs("UseOperationsRule"),
                      false);
                String modifiedBV = "";
                if (useOpRule && (army.getOpForceSize() < army.getUnits().size()) && (army.getOpForceSize() > 0)) {
                    modifiedBV = String.format("(%s)", Math.round((army.getBV() *
                                                           army.forceSizeModifier(army.getOpForceSize()))));
                }

                toReturn += String.format("BV: %s%s<br>%s</center>", army.getBV(), modifiedBV, range);
                if (useOpRule &&
                          (army.getOpForceSize() < army.getUnits().size()) &&
                          (army.getOpForceSize() > 0)) {
                    toReturn += String.format("Force Size: %s<br>", army.getOpForceSize());
                }

                // Put in the tonnage info
                toReturn += String.format("Tons: %s<br>", army.getTotalTonnage());
                toReturn += "</HTML>";
                return toReturn;
            }
            // else
            return "Hangar";
        }

        CUnit cm = getMekAt(row, col);
        if ((cm == null) && (row < getRowsForArmies())) {
            return " - ";
        } else if (cm == null) {// and in hangar row
            int hangerNum = (((row - getRowsForArmies()) * (getColumnCount() - 1)) + col) - 1;
            if (hangerNum == chqPanel.getPlayer().getHangar().size()) {// only show in
                // first free
                // cell
                if (chqPanel.useAdvanceRepairs()) {
                    return String.format("Free Bays: %s", chqPanel.getClient().getPlayer().getFreeBays());
                }
                // else
                return String.format("Idle Techs: %s", chqPanel.getClient().getPlayer().getFreeBays());
            }
            // else
            return "";
        }

        // else
        CArmy army = getArmyAt(row);
        StringBuilder result = new StringBuilder(cm.getModelName());
        String skillSet = cm.getPilot()
                                .getSkillString(false,
                                      chqPanel.getClient().getData()
                                            .getHouseByName(chqPanel.getClient().getPlayer().getHouse())
                                            .getBasePilotSkill(cm.getType()));
        java.util.StringTokenizer skills = new java.util.StringTokenizer(skillSet, ",");
        while (skills.hasMoreElements()) {
            skills.nextElement();
            result.append("*");
        }
        if (army != null) {
            if (cm.hasBeenC3LinkedTo(army)) {
                result.append(" |M|");
            } else if (army.getC3Network().get(cm.getId()) != null) {
                result.append(" |L|");
            }
            if (!Boolean.parseBoolean(chqPanel.getClient().getConfig().getParam("RIGHT_COMMANDER")) &&
                      !Boolean.parseBoolean(chqPanel.getClient().getConfig().getParam("LEFT_COMMANDER")) &&
                      army.isCommander(cm.getId())) {
                result.append(" Cmdr");
            }
        }

        return result.toString();
    }

    // number of rows consumed by hangar
    /**
     * @return number of grid rows needed for the hangar block: the player's unassigned units plus
     * at most one extra "slot" representing free repair bays / idle technicians. Regardless of how
     * many free bays the player actually has, at most 1 is added here (that single cell later shows
     * the "Free Bays"/"Idle Techs" count as text) and negative bay counts are floored to 0 so they
     * never shrink the row count.
     */
    public int getRowsForHangar() {

        /*
         * no matter how many free bays a person has, return only one. this this solitary space shows players' remaining technicians. also - do not allow any adjustment in HQ display for negative bays.
         */
        int freebays = chqPanel.getPlayer().getFreeBays();

        if (freebays > 1) {
            freebays = 1;
        }
        if (freebays < 0) {
            freebays = 0;
        }

        return (int) Math.ceil((double) (freebays + chqPanel.getPlayer().getHangar().size()) / (getColumnCount() - 1));
    }

    /** @return total grid rows consumed by all of the player's armies, summing
     *  {@link #getRowsForArmy(CArmy)} over each. */
    public int getRowsForArmies() {

        int total = 0;
        for (CArmy currA : chqPanel.getPlayer().getArmies()) {
            total += getRowsForArmy(currA);
        }

        return total;
    }

    // number of rows consumed by given army
    /** @return rows needed to lay out {@code army}'s units at {@code getColumnCount() - 1} units
     *  per row, always at least 1 (so an empty army still occupies a labeled row). */
    public int getRowsForArmy(CArmy army) {
        int toReturn = (int) Math.ceil((double) army.getAmountOfUnits() / (double) (getColumnCount() - 1));
        return Math.max(toReturn, 1);
    }

    @Override
    public String getColumnName(int col) {
        if (col == 0) {
            return "Army";
        }
        return String.format("Unit %s", col);
    }

    /**
     * Walks the player's armies in order, subtracting each one's {@link #getRowsForArmy(CArmy)}
     * from {@code row} until the remainder falls within an army's block.
     *
     * @param row grid row to resolve
     * @return the {@link CArmy} occupying that row, or {@code null} if the row is past all army
     * blocks (i.e. it's in the hangar section)
     */
    public CArmy getArmyAt(int row) {

        for (CArmy currA : chqPanel.getPlayer().getArmies()) {
            int uses = getRowsForArmy(currA);
            if (uses > row) {
                return (currA);
            }
            row -= uses;
        }

        return null;
    }

    /**
     * Computes the starting unit-index offset for the army block row that {@code row} falls into
     * (the number of unit slots consumed by the earlier rows of that same army), for use by
     * {@link #getMekAt(int, int)} when indexing into that army's unit vector.
     *
     * @param row grid row to resolve
     * @return unit-index offset within the owning army's units, or {@code 0} if {@code row} is past
     * all army blocks
     */
    public int getOffset(int row) {

        for (CArmy currA : chqPanel.getPlayer().getArmies()) {

            int uses = getRowsForArmy(currA);
            if (uses > row) {
                return row * (getColumnCount() - 1);
            }

            row -= uses;
        }

        return 0;
    }

    /**
     * Resolves the actual {@link CUnit} occupying a grid cell, or {@code null} if the cell is
     * column 0 (army/label column, never a unit), empty, or out of range. For rows within an
     * army's block, indexes into a snapshot {@link Vector} of that army's units at
     * {@code getOffset(row) + col - 1}. For hangar rows, computes a flat hangar index
     * ({@code (row - getRowsForArmies()) * (getColumnCount() - 1) + col - 1}) into the player's
     * hangar list.
     */
    public CUnit getMekAt(int row, int col) {
        if (row < 0) {
            return null;
        }
        if (col != 0) {
            if (row < getRowsForArmies()) {
                CArmy army = getArmyAt(row);
                Vector<Unit> meks = new Vector<>(army.getUnits());
                int offset = (getOffset(row) + col) - 1;

                if (offset < meks.size()) {
                    return (CUnit) meks.elementAt(offset);
                }

                return null;
            }

            int hangerNum = (((row - getRowsForArmies()) * (getColumnCount() - 1)) + col) - 1;

            if ((hangerNum >= 0) && (hangerNum < chqPanel.getPlayer().getHangar().size())) {
                return chqPanel.getPlayer().getHangar().get(hangerNum);
            }
        }
        return null;
    }

    /** Notifies listeners that the entire grid's contents may have changed (e.g. after a unit is
     *  bought/sold/moved between army and hangar). */
    public void refreshModel() {
        fireTableDataChanged();
    }

    /** @return a fresh {@link Renderer} bound to this model and the HQ panel's client. */
    public Renderer getRenderer() {
        return new Renderer(this, chqPanel.getClient());
    }

}// end MekTableModel
