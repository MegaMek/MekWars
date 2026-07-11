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

public class MekTableModel extends AbstractTableModel {
    private static final MMLogger LOGGER = MMLogger.create(MekTableModel.class);

    @Serial
    private static final long serialVersionUID = -7918520064078379615L;

    final CHQPanel chqPanel;

    public MekTableModel(CHQPanel chqPanel) {
        this.chqPanel = chqPanel;
    }

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
    public int getRowCount() {
        int hangarRows = getRowsForHangar();
        int armyRows = getRowsForArmies();
        return hangarRows + armyRows;
    }

    public int getColumnCount() {
        return Integer.parseInt(chqPanel.getClient().getConfigParam("UNIT_AMOUNT")) + 1;
    }

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

    public int getRowsForArmies() {

        int total = 0;
        for (CArmy currA : chqPanel.getPlayer().getArmies()) {
            total += getRowsForArmy(currA);
        }

        return total;
    }

    // number of rows consumed by given army
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

    public void refreshModel() {
        fireTableDataChanged();
    }

    public Renderer getRenderer() {
        return new Renderer(this, chqPanel.getClient());
    }

}// end MekTableModel
