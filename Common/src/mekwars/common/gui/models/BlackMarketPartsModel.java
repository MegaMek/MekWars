/*
 * Copyright (C) 2007 MekWars
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
import java.io.Serial;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import megamek.codeUtilities.MathUtility;
import mekwars.common.BMEquipment;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * Adapted from BlackMarketModel by Steve Hawkins
 */

public class BlackMarketPartsModel extends AbstractTableModel {

    public final static int PART = 0;
    public final static int TECH = 1;
    public final static int COST = 2;
    public final static int AMOUNT = 3;
    public final static int INTERNAL_PART = 4;

    @Serial
    private static final long serialVersionUID = -4312857440681697117L;
    final String[] columnNames = { "Part", "Tech", "Cost", "Amount", };
    final String[] longValues = { "XXXXXX-XXXX-XXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", };
    public IClient client;
    public TreeMap<String, BMEquipment> components; // this collection is backed
    // by the main map, so it
    // should always be good
    public List<BMEquipment> sortedComponents = new ArrayList<>(); // not really though, sort is
    CCampaign theCampaign;
    // handled elsewhere...
    private String type = "";

    public BlackMarketPartsModel(IClient client, String type) {
        this.client = client;
        theCampaign = this.client.getCampaign();
        this.type = type;
        components = theCampaign.getBlackMarketParts();

        filter();
    }

    private void filter() {
        TreeMap<String, BMEquipment> tempTree = new TreeMap<>();

        for (String key : components.keySet()) {
            BMEquipment bmEquipment = components.get(key);

            if (bmEquipment.getEquipmentType().equals(type)) {
                tempTree.put(key, bmEquipment);
            }
        }

        if (!tempTree.isEmpty()) {
            sortedComponents.clear();
            sortedComponents.addAll(tempTree.values());
        }
    }

    public void refreshModel() {
        filter();
        fireTableDataChanged();
    }

    public void initColumnSizes(JTable table) {
        TableColumn column;
        Component comp;
        int headerWidth = 0;
        int cellWidth;
        BlackMarketPartsModel model = this;

        for (int i = 0; i < getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);
            comp = table.getDefaultRenderer(model.getColumnClass(i))
                         .getTableCellRendererComponent(table, longValues[i], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }

    public int getRowCount() {
        if (sortedComponents == null) {
            return 0;
        }

        return sortedComponents.size();
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public Object getValueAt(int row, int col) {
        if (row < 0) {
            return "";
        }

        if (row >= sortedComponents.size()) {
            return "";
        }

        BMEquipment bme = sortedComponents.get(row);
        return switch (col) {
            case PART -> bme.getEquipmentName();
            case COST -> {
                DecimalFormat df = new DecimalFormat("#,###,###,##0.00");
                yield df.format(bme.getCost());
            }
            case TECH -> bme.getTech(MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 0));
            case AMOUNT -> {
                if (client.getPlayer().getPartsCache().getPartsCritCount(bme.getEquipmentInternalName()) < 1) {
                    yield bme.getAmount();
                }
                yield String.format("%s(%s)", bme.getAmount(), client.getPlayer()
                                                      .getPartsCache()
                                                      .getPartsCritCount(bme.getEquipmentInternalName()));
            }
            case INTERNAL_PART -> bme.getEquipmentInternalName();
            default -> "";
        };
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    public BlackMarketPartsModel.Renderer getRenderer() {
        return new BlackMarketPartsModel.Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    public class Renderer extends DefaultTableCellRenderer {
        @Serial
        private static final long serialVersionUID = 5506902358006897558L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {
            Component tableCellRendererComponent = super.getTableCellRendererComponent(table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column);
            int year = MathUtility.parseInt(client.getServerConfigs("CampaignYear"), 0);
            JLabel jLabel = new JLabel(); // use a new label for everything (should
            // be made better later)
            jLabel.setOpaque(true);
            if (components.size() < row ||
                      row < 0 ||
                      !components.containsKey(table.getModel().getValueAt(row, BlackMarketPartsModel.INTERNAL_PART))) {
                return jLabel;
            }

            if (table.getModel().getValueAt(row, column) != null) {
                jLabel.setText(table.getModel().getValueAt(row, column).toString());
            }

            jLabel.setToolTipText("");

            BMEquipment bme = components.get(table.getModel().getValueAt(row, BlackMarketPartsModel.INTERNAL_PART));
            String description = String.format("<html><body>%s C:%s A:%s T:%s<br>", bme.getEquipmentName(), bme.getCost(), bme.getAmount(), bme.getTech(
                  year));

            description += "</body></html>";
            jLabel.setToolTipText(description);

            if (isSelected) {
                jLabel.setForeground(tableCellRendererComponent.getForeground());
                jLabel.setBackground(tableCellRendererComponent.getBackground());
                return jLabel;
            }

            if (row % 2 == 0) {
                jLabel.setBackground(Color.lightGray);
            } else {
                jLabel.setBackground(Color.white);
            }

            if (bme.isCostUp()) {
                jLabel.setForeground(new Color(195, 11, 0));
            } else {
                jLabel.setForeground(new Color(3, 149, 50));
            }

            return jLabel;
        }
    }
}
