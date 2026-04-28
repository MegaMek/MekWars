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

package mekwars.client.gui;

import common.BMEquipment;

/**
 * Adapted from BlackMarketModel by Steve Hawkins
 */

public class BlackMarketPartsModel extends javax.swing.table.AbstractTableModel {

    public final static int PART = 0;
    public final static int TECH = 1;
    public final static int COST = 2;
    public final static int AMOUNT = 3;
    public final static int INTERNALPART = 4;
    private static final long serialVersionUID = -4312857440681697117L;
    final String[] columnNames = { "Part", "Tech", "Cost", "Amount", };
    final String[] longValues = { "XXXXXX-XXXX-XXXXXX", "XXXXXXXXX", "XXXXXXXXX", "XXXXXXXXX", };
    public client.MWClient mwclient;
    public java.util.TreeMap<String, BMEquipment> components; // this collection is backed
    // by the main map, so it
    // should always be good
    public Object[] sortedComponents = null; // not really though, sort is
    client.campaign.CCampaign theCampaign;
    // handled elsewhere...
    private String type = "";

    public BlackMarketPartsModel(client.MWClient client, String type) {
        mwclient = client;
        theCampaign = mwclient.getCampaign();
        this.type = type;
        components = theCampaign.getBlackMarketParts();

        filter();
    }

    private void filter() {
        java.util.TreeMap<String, BMEquipment> tempTree = new java.util.TreeMap<String, BMEquipment>();
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

    public void refreshModel() {
        // do a resort
        // this.sortedComponents = this.components.values().toArray();
        filter();
        fireTableDataChanged();
    }

    public void initColumnSizes(javax.swing.JTable table) {
        javax.swing.table.TableColumn column = null;
        java.awt.Component comp = null;
        int headerWidth = 0;
        int cellWidth = 0;
        mekwars.client.gui.BlackMarketPartsModel model = this;
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
        return sortedComponents.length;
    }

    public int getColumnCount() {
        return columnNames.length;
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
                java.text.DecimalFormat df = new java.text.DecimalFormat("#,###,###,##0.00");
                return df.format(bme.getCost());
            case TECH:
                return bme.getTech(Integer.parseInt(mwclient.getServerConfigs("CampaignYear")));
            case AMOUNT:
                if (mwclient.getPlayer().getPartsCache().getPartsCritCount(bme.getEquipmentInternalName()) < 1) {
                    return bme.getAmount();
                }
                return bme.getAmount() +
                             "(" +
                             mwclient.getPlayer().getPartsCache().getPartsCritCount(bme.getEquipmentInternalName()) +
                             ")";
            case INTERNALPART:
                return bme.getEquipmentInternalName();
        }
        return "";
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public mekwars.client.gui.BlackMarketPartsModel.Renderer getRenderer() {
        return new mekwars.client.gui.BlackMarketPartsModel.Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    class Renderer extends javax.swing.table.DefaultTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = 5506902358006897558L;

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component d = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            int year = Integer.parseInt(mwclient.getServerConfigs("CampaignYear"));
            javax.swing.JLabel c = new javax.swing.JLabel(); // use a new label for everything (should
            // be made better later)
            c.setOpaque(true);
            if (components.size() < row ||
                      row < 0 ||
                      !components.containsKey(table.getModel()
                                                    .getValueAt(row,
                                                          mekwars.client.gui.BlackMarketPartsModel.INTERNALPART))) {
                return c;
            }
            if (table.getModel().getValueAt(row, column) != null) {
                c.setText(table.getModel().getValueAt(row, column).toString());
            }
            c.setToolTipText("");

            BMEquipment bme = components.get(table.getModel()
                                                   .getValueAt(row,
                                                         mekwars.client.gui.BlackMarketPartsModel.INTERNALPART));
            String description = "<html><body>" +
                                       bme.getEquipmentName() +
                                       " C:" +
                                       bme.getCost() +
                                       " A:" +
                                       bme.getAmount() +
                                       " T:" +
                                       bme.getTech(year) +
                                       "<br>";

            description += "</body></html>";
            c.setToolTipText(description);
            if (isSelected) {
                c.setForeground(d.getForeground());
                c.setBackground(d.getBackground());
                return c;
            }

            if (row % 2 == 0) {
                c.setBackground(java.awt.Color.lightGray);
            } else {
                c.setBackground(java.awt.Color.white);
            }

            if (bme.isCostUp()) {
                c.setForeground(new java.awt.Color(195, 11, 00));
            } else {
                c.setForeground(new java.awt.Color(03, 149, 50));
            }

            return c;
        }
    }
}
