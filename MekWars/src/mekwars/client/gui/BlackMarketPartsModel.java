/*
 * MekWars - Copyright (C) 2007
 *
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */

package client.gui;

import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import client.MWClient;
import client.campaign.CCampaign;
import common.BMEquipment;

/**
 * Adapted from BlackMarketModel by Steve Hawkins
 */

public class BlackMarketPartsModel extends AbstractTableModel {

    private static final long serialVersionUID = -4312857440681697117L;
    public MWClient mwclient;
    CCampaign theCampaign;
    public TreeMap<String, BMEquipment> components; // this collection is backed
                                                    // by the main map, so it
                                                    // should always be good
    public Object[] sortedComponents = null; // not really though, sort is
                                             // handled elsewhere...
    private String type = "";

    public final static int PART = 0;
    public final static int TECH = 1;
    public final static int COST = 2;
    public final static int AMOUNT = 3;
    public final static int INTERNALPART = 4;

    final String[] columnNames = { "Part", "Tech", "Cost", "Amount", };

    final String[] longValues = { "XXXXXX-XXXX-XXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", };

    public int getColumnCount() {
        return columnNames.length;
    }

    public BlackMarketPartsModel(MWClient client, String type) {
        mwclient = client;
        theCampaign = mwclient.getCampaign();
        this.type = type;
        components = theCampaign.getBlackMarketParts();

        filter();
    }

    public void refreshModel() {
        // do a resort
        // this.sortedComponents = this.components.values().toArray();
        filter();
        fireTableDataChanged();
    }

    private void filter() {
        TreeMap<String, BMEquipment> tempTree = new TreeMap<String, BMEquipment>();
        for (String key : components.keySet()) {
            BMEquipment eq = components.get(key);
            if (eq.getEquipmentType().equals(type)) {
                tempTree.put(key, eq);
            }
        }
        // this.bids = client.getMyBids();

        if (tempTree.size() > 0) {
            sortedComponents = tempTree.values().toArray();
        }
    }

    public void initColumnSizes(JTable table) {
        TableColumn column = null;
        Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        BlackMarketPartsModel model = this;
        for (int i = 0; i < getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);
            comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(table, longValues[i], false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));
        }
    }

    public int getRowCount() {
        if (sortedComponents == null) {
            return 0;
        }
        return sortedComponents.length;
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Object getValueAt(int row, int col) {
        if (row < 0) {
            return "";
        }
        if (row >= sortedComponents.length) {
            return "";
        }
        BMEquipment bme = (BMEquipment) sortedComponents[row];
        switch (col) {
        case PART:
            return bme.getEquipmentName();
        case COST:
            DecimalFormat df = new DecimalFormat("#,###,###,##0.00");
            return df.format(bme.getCost());
        case TECH:
            return bme.getTech(Integer.parseInt(mwclient.getserverConfigs("CampaignYear")));
        case AMOUNT:
            if (mwclient.getPlayer().getPartsCache().getPartsCritCount(bme.getEquipmentInternalName()) < 1) {
                return bme.getAmount();
            }
            return bme.getAmount() + "(" + mwclient.getPlayer().getPartsCache().getPartsCritCount(bme.getEquipmentInternalName()) + ")";
        case INTERNALPART:
            return bme.getEquipmentInternalName();
        }
        return "";
    }

    public BlackMarketPartsModel.Renderer getRenderer() {
        return new Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    class Renderer extends DefaultTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 5506902358006897558L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component d = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int year = Integer.parseInt(mwclient.getserverConfigs("CampaignYear"));
            JLabel c = new JLabel(); // use a new label for everything (should
                                     // be made better later)
            c.setOpaque(true);
            if (components.size() < row || row < 0 || !components.containsKey(table.getModel().getValueAt(row, BlackMarketPartsModel.INTERNALPART))) {
                return c;
            }
            if (table.getModel().getValueAt(row, column) != null) {
                c.setText(table.getModel().getValueAt(row, column).toString());
            }
            c.setToolTipText("");

            BMEquipment bme = components.get(table.getModel().getValueAt(row, BlackMarketPartsModel.INTERNALPART));
            String description = "<html><body>" + bme.getEquipmentName() + " C:" + bme.getCost() + " A:" + bme.getAmount() + " T:" + bme.getTech(year) + "<br>";

            description += "</body></html>";
            c.setToolTipText(description);
            if (isSelected) {
                c.setForeground(d.getForeground());
                c.setBackground(d.getBackground());
                return c;
            }

            if (row % 2 == 0) {
                c.setBackground(Color.lightGray);
            } else {
                c.setBackground(Color.white);
            }

            if (bme.isCostUp()) {
                c.setForeground(new Color(195, 11, 00));
            } else {
                c.setForeground(new Color(03, 149, 50));
            }

            return c;
        }
    }
}