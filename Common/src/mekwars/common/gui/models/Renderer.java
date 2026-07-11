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

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.tileset.MekTileset;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MekInfo;

public class Renderer extends MekInfo implements TableCellRenderer, Serializable {
    private static final MMLogger LOGGER = MMLogger.create(Renderer.class);
    @Serial
    private static final long serialVersionUID = -300922977373422309L;

    private final MekTableModel mekTableModel;
    int mekNum;

    MekTileset mekTileset = new MekTileset(new File("data/images/units/"));
    Color dcolor = new Color(220, 220, 220);

    public Renderer(MekTableModel mekTableModel, IClient client) {
        super(client);
        this.mekTableModel = mekTableModel;

        try {
            mekTileset.loadFromFile("mechset.txt");
        } catch (IOException ex) {
            LOGGER.error(ex, "Unable to read data/images/units/mechset.txt");
        }
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
          int row, int column) {
        Component component = this;
        mekTableModel.chqPanel.setOpaque(true);
        setText(mekTableModel.getValueAt(row, column));
        mekTableModel.chqPanel.setToolTipText(null);
        component.setBackground(dcolor);
        String scheme = mekTableModel.chqPanel.getClient().getConfig().getParam("HQ_COLOR_SCHEME").toLowerCase();
        CArmy armyAt = mekTableModel.getArmyAt(row);

        if (armyAt != null) {
            if (column == 0) {
                setImageVisible(false);
                mekTableModel.chqPanel.setToolTipText(armyAt.getSkillInfoForDisplay());

                if (armyAt.isLocked()) {
                    component.setBackground(new Color(235, 225, 5));
                }

                return component;
            }
        } else if (column == 0) {
            // Hangar Color (pale purple)
            component.setBackground(new Color(dcolor.getRed() - 33, dcolor.getBlue() - 33, dcolor.getGreen() - 7));
            return component;
        }
        CUnit cm = mekTableModel.getMekAt(row, column);
        if (cm != null) {
            int inNumberOfArmies = mekTableModel.chqPanel.getPlayer().getAmountOfTimesUnitExistsInArmies(cm.getId());
            StringBuilder C3Text = new StringBuilder();
            String description;

            if (cm.getC3Level() > 0) {

                if (cm.getC3Level() == Unit.C3_SLAVE) {
                    C3Text.append("C3 Slave");
                } else if (cm.getC3Level() == Unit.C3_MASTER) {
                    C3Text.append("C3 Master");
                } else if (cm.getC3Level() == Unit.C3M_MASTER) {
                    C3Text.append("C3 Dual Master");
                } else if (cm.getC3Level() == Unit.C3_IMPROVED) {
                    C3Text.append("C3 Improved");
                }

                if ((armyAt != null) && (armyAt.getC3Network().get(cm.getId())) != null) {
                    Integer master = armyAt.getC3Network().get(cm.getId());
                    if (cm.getC3Level() == Unit.C3_IMPROVED) {
                        C3Text.append(" linked to #").append(master);
                    } else {
                        C3Text.append(" to #").append(master);
                    }
                }

                if ((armyAt != null) && cm.hasBeenC3LinkedTo(armyAt)) {
                    if (cm.getC3Level() == Unit.C3_IMPROVED) {
                        C3Text.append(" master for");
                    } else {
                        C3Text.append(" for");
                    }

                    Enumeration<Integer> c3Key = armyAt.getC3Network().keys();
                    Enumeration<Integer> c3Unit = armyAt.getC3Network().elements();
                    while (c3Key.hasMoreElements()) {
                        Integer slave = c3Key.nextElement();
                        Integer master = c3Unit.nextElement();
                        if (master == cm.getId()) {
                            C3Text.append(" #").append(slave.intValue());
                        }
                    }

                }
            }
            if (mekTableModel.chqPanel.getClient().getPlayer().getMyHouse().getNonFactionUnitsCostMore()) {
                String techCostString = "";

                if (cm.getC3Level() > 0) {
                    techCostString = String.format("%s<br>", C3Text);
                }

                String techAmount = String.format("TechsFor%s%s", Unit.getWeightClassDesc(cm.getWeightClass()), Unit.getTypeClassDesc(
                      cm.getType()));
                int numTechs = (int) (MathUtility.parseInt(mekTableModel.chqPanel.getClient()
                                                                 .getServerConfigs(techAmount), 0) *
                                            (mekTableModel.chqPanel.getClient().getPlayer()
                                                   .getMyHouse()
                                                   .houseSupportsUnit(cm.getUnitFilename()) ?
                                                   1 :
                                                   MathUtility.parseFloat(mekTableModel.chqPanel.getClient()
                                                                                .getServerConfigs(
                                                                                      "NonFactionUnitsIncreasedTechs"),
                                                         0.0f)));

                techCostString += String.format("Techs required: %s", numTechs);
                C3Text.setLength(0);
                C3Text.append(techCostString);
            }
            if (MathUtility.parseBoolean(mekTableModel.chqPanel.getClient().getConfigParam("ShowUnitTechBase"),
                  false)) {
                if (mekTableModel.chqPanel.getClient().getPlayer().getMyHouse().getNonFactionUnitsCostMore()) {
                    C3Text.append("<br>");
                }

                if (cm.getEntity().isClan()) {
                    C3Text.append("Tech Base: Clan<br>");
                } else {
                    C3Text.append("Tech Base: IS<br>");
                }
            }
            C3Text.append("Targeting: ").append(cm.getTargetSystemTypeDesc()).append("<br>");
            if (cm.isSupportUnit()) {
                C3Text.append("[Support]<br>");
            }

            //@salient EXPANDEDUNITTOOLTIP
            if (MathUtility.parseBoolean(mekTableModel.chqPanel.getClient().getConfig().getParam(
                  "EXPANDED_UNIT_TOOLTIP"), false)) {
                C3Text.append("<font color=\"purple\">");
                C3Text.append("<b>[General]</b><br>");
                C3Text.append(String.format("Weight: %s Tons (%s)<br>", cm.getEntity().getWeight(), cm.getEntity()
                                                                                       .getWeightClassName()));
                C3Text.append(String.format("Armor: %s Tons (%s Pts)<br>", cm.getEntity().getArmorWeight(), cm.getEntity()
                                                                                           .getTotalArmor()));
                int walk = cm.getEntity().getWalkMP();
                int run = cm.getEntity().getRunMPWithoutMASC();
                int jump = cm.getEntity().getJumpMP();
                int masc = cm.getEntity().getRunMP();
                C3Text.append(String.format("Movement: %s/%s", walk, run));

                if (cm.getEntity().getMASC() != null) {
                    C3Text.append("(").append(masc).append(")");
                }

                if (jump != 0) {
                    C3Text.append("/").append(jump).append("<br>");
                } else {
                    C3Text.append("<br>");
                }

                C3Text.append("Heat Capacity: ").append(cm.getEntity().getHeatCapacity()).append("<br>");

                if (cm.getEntity().canFlipArms()) {
                    C3Text.append("Arms Flip: <font color=\"green\">YES</font><br>");
                } else {
                    C3Text.append("Arms Flip: <font color=\"red\">NO</font><br>");
                }

                C3Text.append("</font>");
                //End General (purple)

                C3Text.append("<font color=\"blue\">");
                C3Text.append("<b>[Weapons]</b><br>");
                cm.getEntity().getWeaponList().forEach(weapon -> {
                    C3Text.append(weapon.getName()).append(" (");

                    if (weapon.isRearMounted()) {
                        C3Text.append(cm.getEntity().getLocationAbbr(weapon.getLocation())).append(") (R)<br>");
                    } else {
                        C3Text.append(cm.getEntity().getLocationAbbr(weapon.getLocation())).append(")<br>");
                    }
                });
                C3Text.append("</font>");
                //End Weapons (blue)

                //Quirks...
                if (MathUtility.parseBoolean(mekTableModel.chqPanel.getClient().getServerConfigs("EnableQuirks"),
                      false)) {
                    C3Text.append("<font color=\"teal\">");
                    C3Text.append("<b>[Quirks]</b><br>");

                    StringTokenizer stringTokenizer = new StringTokenizer(cm.getHtmlQuirksList(), "*");

                    while (stringTokenizer.hasMoreTokens()) {
                        C3Text.append(mekwars.common.util.TokenReader.readString(stringTokenizer));
                    }

                    //C3Text.append(cm.quirkCheck());
                    C3Text.append("</font>");
                    //End Quirks (teal)
                }
            }

            // If you have a unit in more then one army, list all the
            // armies it is in.
            if (inNumberOfArmies > 1) {
                String armiesText = "";

                if (cm.getC3Level() > 0) {
                    armiesText = String.format("%s<br>", C3Text);
                }

                armiesText += String.format("In armies %s", mekTableModel.chqPanel.getPlayer().getArmiesUnitIsIn(cm.getId()));
                description = cm.getDisplayInfo(armiesText);
            } else {
                description = cm.getDisplayInfo(C3Text.toString());
            }

            mekTableModel.chqPanel.setToolTipText(description);
            setUnit(cm, armyAt);
            setImageVisible(true);

            if (cm.getStatus() == mekwars.common.Unit.STATUS_FOR_SALE) {
                // a mild green for units that are on sale
                component.setBackground(new java.awt.Color(50, 170, 35));
            } else if (cm.getStatus() == mekwars.common.Unit.STATUS_UNMAINTAINED) {
                // a nice rusty orange for unmaintained units
                component.setBackground(new java.awt.Color(190, 150, 55));
            } else if (mekTableModel.chqPanel.isUseUnitLocking() && cm.isLocked()) { //@Salient - mini campaign lock
                component.setBackground(new java.awt.Color(128, 0, 128)); //purple, i think.
            } else if (!mekTableModel.chqPanel.getClient().getConfig().isUsingStatusIcons()) {
                if (cm.getPilot().getName().equals("Vacant")) {
                    // RFE 1545928 -Color for pilotless units
                    component.setBackground(new java.awt.Color(160, 190, 115));
                } else if (mekTableModel.chqPanel.useAdvanceRepairs() &&
                                 mekwars.common.util.UnitUtils.isRepairing(cm.getEntity())) {
                    component.setBackground(new java.awt.Color(0, 255, 127));
                } else if (mekTableModel.chqPanel.useAdvanceRepairs() &&
                                 (mekTableModel.chqPanel.getClient().getRMT() != null) &&
                                 mekTableModel.chqPanel.getClient().getRMT().hasQueuedOrders(cm.getId())) {
                    component.setBackground(new java.awt.Color(75, 0, 130));
                } else if (mekTableModel.chqPanel.useAdvanceRepairs() &&
                                 mekwars.common.util.UnitUtils.hasCriticalDamage(cm.getEntity())) {
                    component.setBackground(java.awt.Color.red);
                } else if (mekTableModel.chqPanel.useAdvanceRepairs() &&
                                 mekwars.common.util.UnitUtils.hasArmorDamage(cm.getEntity())) {
                    component.setBackground(new java.awt.Color(238, 238, 0));
                } else if (mekTableModel.chqPanel.useAdvanceRepairs() &&
                                 !mekwars.common.util.UnitUtils.hasAllAmmo(cm.getEntity())) {
                    component.setBackground(new java.awt.Color(255, 128, 255));
                } else if ((armyAt == null) && (inNumberOfArmies > 0)) {
                    if (scheme.equals("classic")) {
                        component.setBackground(new java.awt.Color(65, 170, 55));// dark
                        // green
                    } else {
                        // all non-classic sets (light blue)
                        component.setBackground(new java.awt.Color(dcolor.getRed() - 43,
                              dcolor.getBlue() - 33,
                              dcolor.getGreen() - 4));
                    }
                } else {

                    // TAN SET. Tan gradients.
                    if (scheme.equals("tan")) {
                        switch (cm.getWeightClass()) {
                            case Unit.LIGHT:
                                component.setBackground(new Color(dcolor.getRed() - 10,
                                      dcolor.getBlue() - 10,
                                      dcolor.getGreen() - 30));
                                break;
                            case Unit.MEDIUM:
                                component.setBackground(new Color(dcolor.getRed() - 30,
                                      dcolor.getBlue() - 30,
                                      dcolor.getGreen() - 50));
                                break;
                            case Unit.HEAVY:
                                component.setBackground(new Color(dcolor.getRed() - 55,
                                      dcolor.getBlue() - 55,
                                      dcolor.getGreen() - 75));
                                break;
                            case Unit.ASSAULT:
                                component.setBackground(new Color(dcolor.getRed() - 75,
                                      dcolor.getBlue() - 75,
                                      dcolor.getGreen() - 95));
                                break;

                        }// end Tan Switch
                    }

                    // GREY SET. Grey gradients.
                    else if (scheme.equals("grey")) {
                        switch (cm.getWeightClass()) {
                            case Unit.LIGHT:
                                component.setBackground(dcolor);
                                break;
                            case Unit.MEDIUM:
                                component.setBackground(new Color(dcolor.getRed() - 17,
                                      dcolor.getBlue() - 17,
                                      dcolor.getGreen() - 17));
                                break;
                            case Unit.HEAVY:
                                component.setBackground(new Color(dcolor.getRed() - 40,
                                      dcolor.getBlue() - 40,
                                      dcolor.getGreen() - 40));
                                break;
                            case Unit.ASSAULT:
                                component.setBackground(new Color(dcolor.getRed() - 65,
                                      dcolor.getBlue() - 65,
                                      dcolor.getGreen() - 65));
                                break;

                        }// end Grey Switch
                    } else {// CLASSIC COLORS. White/Tan/Blue/Purple.
                        switch (cm.getWeightClass()) {
                            case Unit.LIGHT:
                                component.setBackground(dcolor);
                                break;
                            case Unit.MEDIUM:
                                component.setBackground(new Color(dcolor.getRed() - 30,
                                      dcolor.getBlue() - 30,
                                      dcolor.getGreen() - 50));
                                break;
                            case Unit.HEAVY:
                                component.setBackground(new Color(dcolor.getRed() - 65,
                                      dcolor.getBlue() - 65,
                                      dcolor.getGreen() - 25));
                                break;
                            case Unit.ASSAULT:
                                component.setBackground(new Color(dcolor.getRed() - 45,
                                      dcolor.getBlue() - 93,
                                      dcolor.getGreen() - 45));
                                break;

                        }// end Classic Switch
                    }
                }// end else (should fill by weight)
            } else if ((armyAt == null) && (inNumberOfArmies > 0)) {
                if (scheme.equals("classic")) {
                    component.setBackground(new Color(65, 170, 55));// dark
                    // green
                } else {
                    // all non-classic sets (light blue)
                    component.setBackground(new Color(dcolor.getRed() - 43,
                          dcolor.getBlue() - 33,
                          dcolor.getGreen() - 4));
                }
            } else {

                // TAN SET. Tan gradients.
                if (scheme.equals("tan")) {
                    switch (cm.getWeightClass()) {
                        case Unit.LIGHT:
                            component.setBackground(new Color(dcolor.getRed() - 10,
                                  dcolor.getBlue() - 10,
                                  dcolor.getGreen() - 30));
                            break;
                        case Unit.MEDIUM:
                            component.setBackground(new Color(dcolor.getRed() - 30,
                                  dcolor.getBlue() - 30,
                                  dcolor.getGreen() - 50));
                            break;
                        case Unit.HEAVY:
                            component.setBackground(new Color(dcolor.getRed() - 55,
                                  dcolor.getBlue() - 55,
                                  dcolor.getGreen() - 75));
                            break;
                        case Unit.ASSAULT:
                            component.setBackground(new Color(dcolor.getRed() - 75,
                                  dcolor.getBlue() - 75,
                                  dcolor.getGreen() - 95));
                            break;
                    }// end Tan Switch
                }

                // GREY SET. Grey gradients.
                else if (scheme.equals("grey")) {
                    switch (cm.getWeightClass()) {
                        case Unit.LIGHT:
                            component.setBackground(dcolor);
                            break;
                        case Unit.MEDIUM:
                            component.setBackground(new Color(dcolor.getRed() - 17,
                                  dcolor.getBlue() - 17,
                                  dcolor.getGreen() - 17));
                            break;
                        case Unit.HEAVY:
                            component.setBackground(new Color(dcolor.getRed() - 40,
                                  dcolor.getBlue() - 40,
                                  dcolor.getGreen() - 40));
                            break;
                        case Unit.ASSAULT:
                            component.setBackground(new Color(dcolor.getRed() - 65,
                                  dcolor.getBlue() - 65,
                                  dcolor.getGreen() - 65));
                            break;

                    }// end Grey Switch
                } else {// CLASSIC COLORS. White/Tan/Blue/Purple.
                    switch (cm.getWeightClass()) {
                        case Unit.LIGHT:
                            component.setBackground(dcolor);
                            break;
                        case Unit.MEDIUM:
                            component.setBackground(new Color(dcolor.getRed() - 30,
                                  dcolor.getBlue() - 30,
                                  dcolor.getGreen() - 50));
                            break;
                        case Unit.HEAVY:
                            component.setBackground(new Color(dcolor.getRed() - 65,
                                  dcolor.getBlue() - 65,
                                  dcolor.getGreen() - 25));
                            break;
                        case Unit.ASSAULT:
                            component.setBackground(new Color(dcolor.getRed() - 45,
                                  dcolor.getBlue() - 93,
                                  dcolor.getGreen() - 45));
                            break;

                    }// end Classic Switch
                }
            }// end else(should fill by weight)
        } else {
            setImageVisible(false);
            mekNum = (((row - mekTableModel.getRowsForArmies()) * mekTableModel.getColumnCount()) - 1) + column;
            int freebays = mekTableModel.chqPanel.getPlayer().getFreeBays();

            if (freebays < 0) {
                freebays = 0;
            }
            
            if (mekNum > (freebays + mekTableModel.chqPanel.getPlayer().getHangar().size())) {
                setText("");
            }
        }
        return component;
    }
}// end Renderer
