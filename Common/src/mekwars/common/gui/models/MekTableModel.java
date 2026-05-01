package mekwars.common.gui.models;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import megamek.client.ui.tileset.MekTileset;
import mekwars.common.Army;
import mekwars.common.Unit;
import mekwars.common.campaign.CArmy;
import mekwars.common.campaign.CUnit;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.gui.MekInfo;
import mekwars.common.gui.panels.CHQPanel;
import mekwars.common.util.MWLogger;
import mekwars.common.util.TokenReader;
import mekwars.common.util.UnitUtils;

public class MekTableModel extends AbstractTableModel {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -7918520064078379615L;

    private final CHQPanel chqPanel;

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
        return Integer.parseInt(chqPanel.getClient().getConfigParam("UNITAMOUNT")) + 1;
        // return this.columnNames.length;
    }

    public Object getValueAt(int row, int col) {

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
                        range = STR."\{army.getAmountOfUnits() - army.getLowerLimiter()}";
                    }

                    // divider
                    range += " - ";

                    // upper limit
                    if (army.getUpperLimiter() == Army.NO_LIMIT) {
                        range += "No Upper";
                    } else {
                        range += STR."\{army.getAmountOfUnits() + army.getUpperLimiter()}";
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

                String toReturn = STR."<html><center><b>Army #\{lid}</b><br>";
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
                        toReturn += STR."\{armyName.subSequence(0, 9)}...<br>";
                    } else {
                        toReturn += STR."\{armyName}<br>";
                    }
                }

                boolean useOpRule = Boolean.parseBoolean(chqPanel.getClient().getServerConfigs("UseOperationsRule"));
                String modifiedBV = "";
                if (useOpRule && (army.getOpForceSize() < army.getUnits().size()) && (army.getOpForceSize() > 0)) {
                    modifiedBV = STR."(\{Math.round((army.getBV() *
                                                           army.forceSizeModifier(army.getOpForceSize())))})";
                }

                toReturn += STR."BV: \{army.getBV()}\{modifiedBV}<br>\{range}</center>";
                if (useOpRule &&
                          (army.getOpForceSize() < army.getUnits().size()) &&
                          (army.getOpForceSize() > 0)) {
                    toReturn += STR."Force Size: \{army.getOpForceSize()}<br>";
                }

                // Put in the tonnage info
                toReturn += STR."Tons: \{army.getTotalTonnage()}<br>";
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
            int hangernum = (((row - getRowsForArmies()) * (getColumnCount() - 1)) + col) - 1;
            if (hangernum == chqPanel.getPlayer().getHangar().size()) {// only show in
                // first free
                // cell
                if (chqPanel.useAdvanceRepairs()) {
                    return STR."Free Bays: \{chqPanel.getClient().getPlayer().getFreeBays()}";
                }
                // else
                return STR."Idle Techs: \{chqPanel.getClient().getPlayer().getFreeBays()}";
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
            if (!Boolean.parseBoolean(chqPanel.getClient().getConfig().getParam("RIGHTCOMMANDER")) &&
                      !Boolean.parseBoolean(chqPanel.getClient().getConfig().getParam("LEFTCOMMANDER")) &&
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
        return STR."Unit \{col}";
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
                java.util.Vector<Unit> meks = new java.util.Vector<>(army.getUnits());
                int offset = (getOffset(row) + col) - 1;
                if (offset < meks.size()) {
                    return (CUnit) meks.elementAt(offset);
                }
                return null;
            }
            int hangernum = (((row - getRowsForArmies()) * (getColumnCount() - 1)) + col) - 1;
            if ((hangernum >= 0) && (hangernum < chqPanel.getPlayer().getHangar().size())) {
                return chqPanel.getPlayer().getHangar().get(hangernum);
            }
        }
        return null;
    }

    public void refreshModel() {
        fireTableDataChanged();
    }

    public Renderer getRenderer() {
        return new Renderer(chqPanel.getClient());
    }

    public class Renderer extends MekInfo implements TableCellRenderer, Serializable {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -300922977373422309L;

        int meknum;

        MekTileset mt = new MekTileset(new File("data/images/units/"));
        java.awt.Color dcolor = new java.awt.Color(220, 220, 220);

        public Renderer(IClient client) {
            super(client);
            try {
                mt.loadFromFile("mechset.txt");
            } catch (java.io.IOException ex) {
                MWLogger.errLog("Unable to read data/images/units/mechset.txt");
            }
        }

        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {

            java.awt.Component c = this;
            chqPanel.setOpaque(true);
            setText(getValueAt(row, column).toString());
            chqPanel.setToolTipText(null);
            c.setBackground(dcolor);
            String scheme = chqPanel.getClient().getConfig().getParam("HQCOLORSCHEME").toLowerCase();
            CArmy l = getArmyAt(row);

            if (l != null) {
                if (column == 0) {
                    setImageVisible(false);
                    chqPanel.setToolTipText(l.getSkillInfoForDisplay());
                    if (l.isLocked()) {
                        c.setBackground(new java.awt.Color(235, 225, 5));
                    }
                    return c;
                }
            } else if (column == 0) {
                // Hangar Color (pale purple)
                c.setBackground(new java.awt.Color(dcolor.getRed() - 33,
                      dcolor.getBlue() - 33,
                      dcolor.getGreen() - 7));
                return c;
            }
            CUnit cm = getMekAt(row, column);
            if (cm != null) {

                int inNumberofArmies = chqPanel.getPlayer().getAmountOfTimesUnitExistsInArmies(cm.getId());
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

                    if ((l != null) && (l.getC3Network().get(cm.getId())) != null) {
                        Integer master = l.getC3Network().get(cm.getId());
                        if (cm.getC3Level() == Unit.C3_IMPROVED) {
                            C3Text.append(" linked to #").append(master);
                        } else {
                            C3Text.append(" to #").append(master);
                        }
                    }

                    if ((l != null) && cm.hasBeenC3LinkedTo(l)) {
                        if (cm.getC3Level() == Unit.C3_IMPROVED) {
                            C3Text.append(" master for");
                        } else {
                            C3Text.append(" for");
                        }

                        java.util.Enumeration<Integer> c3Key = l.getC3Network().keys();
                        java.util.Enumeration<Integer> c3Unit = l.getC3Network().elements();
                        while (c3Key.hasMoreElements()) {
                            Integer slave = c3Key.nextElement();
                            Integer master = c3Unit.nextElement();
                            if (master == cm.getId()) {
                                C3Text.append(" #").append(slave.intValue());
                            }
                        }

                    }
                }
                if (chqPanel.getClient().getPlayer().getMyHouse().getNonFactionUnitsCostMore()) {
                    String techCostString = "";
                    if (cm.getC3Level() > 0) {
                        techCostString = STR."\{C3Text}<br>";
                    }

                    String techAmount = STR."TechsFor\{Unit.getWeightClassDesc(cm.getWeightClass())}\{Unit.getTypeClassDesc(
                          cm.getType())}";
                    int numTechs = (int) (Integer.parseInt(chqPanel.getClient().getServerConfigs(techAmount)) *
                                                (chqPanel.getClient().getPlayer()
                                                       .getMyHouse()
                                                       .houseSupportsUnit(cm.getUnitFilename()) ?
                                                       1 :
                                                       Float.parseFloat(chqPanel.getClient().getServerConfigs(
                                                             "NonFactionUnitsIncreasedTechs"))));

                    techCostString += STR."Techs required: \{numTechs}";
                    C3Text.setLength(0);
                    C3Text.append(techCostString);
                }
                if (Boolean.parseBoolean(chqPanel.getClient().getConfigParam("ShowUnitTechBase"))) {
                    if (chqPanel.getClient().getPlayer().getMyHouse().getNonFactionUnitsCostMore()) {
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
                if (Boolean.parseBoolean(chqPanel.getClient().getConfig().getParam("EXPANDEDUNITTOOLTIP"))) {
                    C3Text.append("<font color=\"purple\">");
                    C3Text.append("<b>[General]</b><br>");
                    C3Text.append(STR."Weight: \{cm.getEntity().getWeight()} Tons (\{cm.getEntity()
                                                                                           .getWeightClassName()})<br>");
                    C3Text.append(STR."Armor: \{cm.getEntity().getArmorWeight()} Tons (\{cm.getEntity()
                                                                                               .getTotalArmor()} Pts)<br>");
                    int walk = cm.getEntity().getWalkMP();
                    int run = cm.getEntity().getRunMPWithoutMASC();
                    int jump = cm.getEntity().getJumpMP();
                    int masc = cm.getEntity().getRunMP();
                    C3Text.append(STR."Movement: \{walk}/\{run}");

                    if (cm.getEntity().getMASC() != null) {C3Text.append("(").append(masc).append(")");}

                    if (jump != 0) {C3Text.append("/").append(jump).append("<br>");} else {C3Text.append("<br>");}

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
                        } else {C3Text.append(cm.getEntity().getLocationAbbr(weapon.getLocation())).append(")<br>");}

                    });
                    C3Text.append("</font>");
                    //End Weapons (blue)

                    //Quirks...
                    if (Boolean.parseBoolean(chqPanel.getClient().getServerConfigs("EnableQuirks"))) {
                        C3Text.append("<font color=\"teal\">");
                        C3Text.append("<b>[Quirks]</b><br>");

                        java.util.StringTokenizer st = new java.util.StringTokenizer(cm.getHtmlQuirksList(), "*");

                        while (st.hasMoreTokens()) {C3Text.append(TokenReader.readString(st));}


                        //C3Text.append(cm.quirkCheck());
                        C3Text.append("</font>");
                        //End Quirks (teal)
                    }
                }

                // If you have a unit in more then one army, list all the
                // armies it is in.
                if (inNumberofArmies > 1) {
                    String armiesText = "";
                    if (cm.getC3Level() > 0) {
                        armiesText = STR."\{C3Text}<br>";
                    }

                    armiesText += STR."In armies \{chqPanel.getPlayer().getArmiesUnitIsIn(cm.getId())}";
                    description = cm.getDisplayInfo(armiesText);
                } else {
                    description = cm.getDisplayInfo(C3Text.toString());
                }
                chqPanel.setToolTipText(description);
                setUnit(cm, l);
                setImageVisible(true);

                if (cm.getStatus() == Unit.STATUS_FOR_SALE) {
                    // a mild green for units that are on sale
                    c.setBackground(new java.awt.Color(50, 170, 35));
                } else if (cm.getStatus() == Unit.STATUS_UNMAINTAINED) {
                    // a nice rusty orange for unmaintained units
                    c.setBackground(new java.awt.Color(190, 150, 55));
                } else if (chqPanel.isUseUnitLocking() && cm.isLocked()) { //@Salient - mini campaign lock
                    c.setBackground(new java.awt.Color(128, 0, 128)); //purple, i think.
                } else if (!chqPanel.getClient().getConfig().isUsingStatusIcons()) {

                    if (cm.getPilot().getName().equals("Vacant")) {
                        // RFE 1545928 -Color for pilotless units
                        c.setBackground(new java.awt.Color(160, 190, 115));
                    } else if (chqPanel.useAdvanceRepairs() && UnitUtils.isRepairing(cm.getEntity())) {
                        c.setBackground(new java.awt.Color(0, 255, 127));
                    } else if (chqPanel.useAdvanceRepairs() &&
                                     (chqPanel.getClient().getRMT() != null) &&
                                     chqPanel.getClient().getRMT().hasQueuedOrders(cm.getId())) {
                        c.setBackground(new java.awt.Color(75, 0, 130));
                    } else if (chqPanel.useAdvanceRepairs() && UnitUtils.hasCriticalDamage(cm.getEntity())) {
                        c.setBackground(java.awt.Color.red);
                    } else if (chqPanel.useAdvanceRepairs() && UnitUtils.hasArmorDamage(cm.getEntity())) {
                        c.setBackground(new java.awt.Color(238, 238, 0));
                    } else if (chqPanel.useAdvanceRepairs() && !UnitUtils.hasAllAmmo(cm.getEntity())) {
                        c.setBackground(new java.awt.Color(255, 128, 255));
                    } else if ((l == null) && (inNumberofArmies > 0)) {
                        if (scheme.equals("classic")) {
                            c.setBackground(new java.awt.Color(65, 170, 55));// dark
                            // green
                        } else {
                            // all non-classic sets (light blue)
                            c.setBackground(new java.awt.Color(dcolor.getRed() - 43,
                                  dcolor.getBlue() - 33,
                                  dcolor.getGreen() - 4));
                        }
                    } else {

                        // TAN SET. Tan gradients.
                        if (scheme.equals("tan")) {
                            switch (cm.getWeightClass()) {
                                case Unit.LIGHT:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 10,
                                          dcolor.getBlue() - 10,
                                          dcolor.getGreen() - 30));
                                    break;
                                case Unit.MEDIUM:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 30,
                                          dcolor.getBlue() - 30,
                                          dcolor.getGreen() - 50));
                                    break;
                                case Unit.HEAVY:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 55,
                                          dcolor.getBlue() - 55,
                                          dcolor.getGreen() - 75));
                                    break;
                                case Unit.ASSAULT:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 75,
                                          dcolor.getBlue() - 75,
                                          dcolor.getGreen() - 95));
                                    break;

                            }// end Tan Switch
                        }

                        // GREY SET. Grey gradients.
                        else if (scheme.equals("grey")) {
                            switch (cm.getWeightClass()) {

                                case Unit.LIGHT:
                                    c.setBackground(dcolor);
                                    break;
                                case Unit.MEDIUM:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 17,
                                          dcolor.getBlue() - 17,
                                          dcolor.getGreen() - 17));
                                    break;
                                case Unit.HEAVY:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 40,
                                          dcolor.getBlue() - 40,
                                          dcolor.getGreen() - 40));
                                    break;
                                case Unit.ASSAULT:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 65,
                                          dcolor.getBlue() - 65,
                                          dcolor.getGreen() - 65));
                                    break;

                            }// end Grey Switch
                        } else {// CLASSIC COLORS. White/Tan/Blue/Purple.

                            switch (cm.getWeightClass()) {

                                case Unit.LIGHT:
                                    c.setBackground(dcolor);
                                    break;
                                case Unit.MEDIUM:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 30,
                                          dcolor.getBlue() - 30,
                                          dcolor.getGreen() - 50));
                                    break;
                                case Unit.HEAVY:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 65,
                                          dcolor.getBlue() - 65,
                                          dcolor.getGreen() - 25));
                                    break;
                                case Unit.ASSAULT:
                                    c.setBackground(new java.awt.Color(dcolor.getRed() - 45,
                                          dcolor.getBlue() - 93,
                                          dcolor.getGreen() - 45));
                                    break;

                            }// end Classic Switch
                        }
                    }// end else(should fill by weight)
                } else if ((l == null) && (inNumberofArmies > 0)) {
                    if (scheme.equals("classic")) {
                        c.setBackground(new java.awt.Color(65, 170, 55));// dark
                        // green
                    } else {
                        // all non-classic sets (light blue)
                        c.setBackground(new java.awt.Color(dcolor.getRed() - 43,
                              dcolor.getBlue() - 33,
                              dcolor.getGreen() - 4));
                    }
                } else {

                    // TAN SET. Tan gradients.
                    if (scheme.equals("tan")) {
                        switch (cm.getWeightClass()) {

                            case Unit.LIGHT:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 10,
                                      dcolor.getBlue() - 10,
                                      dcolor.getGreen() - 30));
                                break;
                            case Unit.MEDIUM:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 30,
                                      dcolor.getBlue() - 30,
                                      dcolor.getGreen() - 50));
                                break;
                            case Unit.HEAVY:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 55,
                                      dcolor.getBlue() - 55,
                                      dcolor.getGreen() - 75));
                                break;
                            case Unit.ASSAULT:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 75,
                                      dcolor.getBlue() - 75,
                                      dcolor.getGreen() - 95));
                                break;

                        }// end Tan Switch
                    }

                    // GREY SET. Grey gradients.
                    else if (scheme.equals("grey")) {
                        switch (cm.getWeightClass()) {

                            case Unit.LIGHT:
                                c.setBackground(dcolor);
                                break;
                            case Unit.MEDIUM:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 17,
                                      dcolor.getBlue() - 17,
                                      dcolor.getGreen() - 17));
                                break;
                            case Unit.HEAVY:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 40,
                                      dcolor.getBlue() - 40,
                                      dcolor.getGreen() - 40));
                                break;
                            case Unit.ASSAULT:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 65,
                                      dcolor.getBlue() - 65,
                                      dcolor.getGreen() - 65));
                                break;

                        }// end Grey Switch
                    } else {// CLASSIC COLORS. White/Tan/Blue/Purple.

                        switch (cm.getWeightClass()) {

                            case Unit.LIGHT:
                                c.setBackground(dcolor);
                                break;
                            case Unit.MEDIUM:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 30,
                                      dcolor.getBlue() - 30,
                                      dcolor.getGreen() - 50));
                                break;
                            case Unit.HEAVY:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 65,
                                      dcolor.getBlue() - 65,
                                      dcolor.getGreen() - 25));
                                break;
                            case Unit.ASSAULT:
                                c.setBackground(new java.awt.Color(dcolor.getRed() - 45,
                                      dcolor.getBlue() - 93,
                                      dcolor.getGreen() - 45));
                                break;

                        }// end Classic Switch
                    }
                }// end else(should fill by weight)
            } else {
                setImageVisible(false);
                meknum = (((row - getRowsForArmies()) * getColumnCount()) - 1) + column;
                int freebays = chqPanel.getPlayer().getFreeBays();
                if (freebays < 0) {
                    freebays = 0;
                }
                if (meknum > (freebays + chqPanel.getPlayer().getHangar().size())) {
                    setText("");
                }
            }
            return c;
        }
    }// end Renderer

}// end MekTableModel
