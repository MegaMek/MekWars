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
 * Table model backing the black-market equipment/parts JTable — a sibling of
 * {@link BlackMarketModel} but for individual pieces of equipment ({@link BMEquipment}, e.g.
 * weapons, ammo, or components) rather than whole units. Each instance is scoped to a single
 * equipment {@code type} (see constructor), so separate instances back separate tabs/panels for
 * different equipment categories (weapons, ammo, etc.). Columns show the part name, tech
 * level/rating, cost, and quantity available; a fifth pseudo-column carries the internal equipment
 * name used to look parts back up in {@link #components}.
 * <p>
 * Adapted from BlackMarketModel by Steve Hawkins
 */

public class BlackMarketPartsModel extends AbstractTableModel {

    /** Column index: display name of the equipment/part. */
    public final static int PART = 0;
    /** Column index: technology level/rating for the current campaign year. */
    public final static int TECH = 1;
    /** Column index: formatted market cost. */
    public final static int COST = 2;
    /** Column index: quantity available (optionally annotated with the player's cached crit count). */
    public final static int AMOUNT = 3;
    /**
     * Pseudo-column carrying the part's internal equipment name, used to re-look-up the
     * {@link BMEquipment} for a row (e.g. from the {@link Renderer}). Not a real table column:
     * {@link #columnNames} only has 4 entries, so this index is never exposed via
     * {@link #getColumnCount()}, but {@link #getValueAt(int, int)} still handles it when called
     * directly.
     */
    public final static int INTERNAL_PART = 4;

    @Serial
    private static final long serialVersionUID = -4312857440681697117L;
    /** Header labels; note there is no header for {@link #INTERNAL_PART} since it isn't a real column. */
    final String[] columnNames = { "Part", "Tech", "Cost", "Amount", };
    /**
     * Dummy "wide" strings used only to measure preferred column widths in
     * {@link #initColumnSizes(JTable)}; never displayed.
     */
    final String[] longValues = { "XXXXXX-XXXX-XXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", };
    /** Client connection used to reach the campaign, player, and server configuration. */
    public IClient client;
    public TreeMap<String, BMEquipment> components; // this collection is backed
    // by the main map, so it
    // should always be good
    /** Row-order snapshot containing only the {@link #components} entries matching {@link #type},
     *  rebuilt by {@link #filter()}. Despite the name, not actually kept sorted here — see comment
     *  below. */
    public List<BMEquipment> sortedComponents = new ArrayList<>(); // not really though, sort is
    /** The active campaign, used to reach the black-market parts map. */
    CCampaign theCampaign;
    // handled elsewhere...
    /** The equipment type this instance filters to (e.g. a weapon/ammo/component category); set once
     *  in the constructor. */
    private String type = "";

    /**
     * @param client client connection providing access to the campaign, player, and config
     * @param type   the equipment type this model instance should filter {@link #components} down to
     */
    public BlackMarketPartsModel(IClient client, String type) {
        this.client = client;
        theCampaign = this.client.getCampaign();
        this.type = type;
        components = theCampaign.getBlackMarketParts();

        filter();
    }

    /**
     * Rebuilds {@link #sortedComponents} from {@link #components}, keeping only entries whose
     * {@link BMEquipment#getEquipmentType()} matches {@link #type}.
     * <p>
     * Quirk: if the filtered result ({@code tempTree}) is empty, {@link #sortedComponents} is left
     * untouched rather than cleared — so if every part of this type is sold or removed from the
     * black market, the table keeps showing the previous (now stale) list of parts instead of
     * becoming empty.
     */
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

    /** Re-filters {@link #components} into {@link #sortedComponents} and notifies listeners that the
     *  entire table changed. */
    public void refreshModel() {
        filter();
        fireTableDataChanged();
    }

    /**
     * Sizes each column to fit the wider of its header and a representative dummy value
     * ({@link #longValues}).
     *
     * @param table the JTable this model is installed on
     */
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

    /**
     * Returns the display value for a given cell, or {@code ""} for out-of-range rows. Behavior per
     * column:
     * <ul>
     *   <li>{@link #PART} — the part's display name</li>
     *   <li>{@link #COST} — cost formatted as {@code #,###,###,##0.00}</li>
     *   <li>{@link #TECH} — tech level/rating computed for the current "CampaignYear" server
     *       config (defaults to year 0 if unparsable/unset)</li>
     *   <li>{@link #AMOUNT} — the raw quantity available, or {@code "amount(critCount)"} if the
     *       player has one or more of this part already reserved/cached in
     *       {@code getPartsCache().getPartsCritCount(...)}</li>
     *   <li>{@link #INTERNAL_PART} — internal equipment name; only reachable when called directly
     *       (not through the JTable, since it isn't a real column — see {@link #INTERNAL_PART})</li>
     * </ul>
     */
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

    /** @return a fresh cell renderer bound to this model's live data. */
    public BlackMarketPartsModel.Renderer getRenderer() {
        return new BlackMarketPartsModel.Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    /**
     * Cell renderer for the black-market parts table. Builds a tooltip summarizing the part's name,
     * cost, amount, and tech level; alternates row background (zebra striping: light gray on even
     * rows, white on odd) rather than using selection/ownership-based coloring; and colors the cell
     * text red when {@link BMEquipment#isCostUp()} (price trending up) or green otherwise (price
     * flat/trending down) as a simple buy-signal indicator.
     */
    public class Renderer extends DefaultTableCellRenderer {
        @Serial
        private static final long serialVersionUID = 5506902358006897558L;

        /**
         * Guards against an out-of-range or already-removed row/part before building the label;
         * returns a blank {@link JLabel} in that case.
         */
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
