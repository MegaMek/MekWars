/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megamek)
 * Original author Helge Richter (McWizard)
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

/*
 * BlackMarketModel.java
 *
 * Created on June 21, 2002, 2:45 PrivateMessageCommand
 */

package mekwars.common.gui.models;


import java.awt.Component;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import mekwars.common.Unit;
import mekwars.common.campaign.CBMUnit;
import mekwars.common.campaign.CCampaign;
import mekwars.common.campaign.clientutils.protocol.IClient;
import org.jspecify.annotations.NonNull;

/**
 *
 * @author Steve Hawkins
 */
public class BlackMarketModel extends AbstractTableModel {

    public final static int MECH = 0;
    public final static int BV = 1;
    public final static int MIN = 2;
    public final static int TICKS = 3;
    public final static int BID = 4;
    public final static int AUCTION_ID = 5; //not shown in table
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -783116408720137035L;
    final String[] columnNames = {
          "Unit",
          "Stock BV",
          "Min Bid",
          "Ticks",
          "Your Bid",
          };
    final String[] longValues = {
          "XXXXXX-XXXX-XXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          };
    public IClient client;

    public TreeMap<Integer, CBMUnit> meks;
    //public TreeMap bids;
    public List<CBMUnit> sortedMeks; //not really though, sort is handled elsewhere...
    CCampaign theCampaign;
    private boolean hiddenUnits = false;

    public BlackMarketModel(IClient client, boolean hideBMUnits) {
        this.client = client;
        theCampaign = this.client.getCampaign();
        this.meks = theCampaign.getBlackMarket();
        hiddenUnits = hideBMUnits;
        //    this.bids = client.getMyBids();

        this.sortedMeks = new ArrayList<>(this.meks.values());
    }

    public void refreshModel() {
        //do a resort
        this.sortedMeks = new ArrayList<>(this.meks.values());
        this.fireTableDataChanged();
    }

    public void initColumnSizes(JTable table) {
        TableColumn column;
        Component comp;
        int headerWidth = 0;
        int cellWidth = 0;
        BlackMarketModel model = this;
        for (int i = 0; i < this.getColumnCount(); i++) {
            column = table.getColumnModel().getColumn(i);
            comp = table.getDefaultRenderer(model.getColumnClass(i)).
                         getTableCellRendererComponent(
                               table, longValues[i],
                               false, false, 0, i);
            cellWidth = comp.getPreferredSize().width;
            column.setPreferredWidth(Math.max(headerWidth, cellWidth));

            // This is a hack, but removeColumn() is throwing errors, so for the moment...
            if (hiddenUnits && i == BV) {
                column.setMinWidth(0);
                column.setMaxWidth(0);
                column.setPreferredWidth(0);
            }
        }

    }

    public int getRowCount() {
        return this.sortedMeks.size();
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public Object getValueAt(int row, int col) {
        if (row < 0) {
            return "";
        }

        if (row >= sortedMeks.size()) {
            return "";
        }

        CBMUnit mm = this.sortedMeks.get(row);
        switch (col) {
            case MECH:
                if (hiddenUnits) {
                    return mm.getHiddenUnitDescription();
                } else {
                    return mm.getModelName();
                }
            case BV:
                if (hiddenUnits) {
                    return " ";
                }
                return mm.getEmbeddedUnit().getEntity().calculateBattleValue();
            case MIN:
                return mm.getMinBid();
            case TICKS:
                return mm.getTicks();
            case BID:
                if (mm.getBid() > 0) {
                    return mm.getBid();
                }
                return null;
            case AUCTION_ID:
                return mm.getAuctionID();
        }
        return "";
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    public Renderer getRenderer() {
        return new Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    public class Renderer extends DefaultTableCellRenderer {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = 5506902358006897558L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {
            Component tableCellRendererComponent = super.getTableCellRendererComponent(table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column);

            JLabel label = new JLabel(); //use a new label for everything (should be made better later)
            label.setOpaque(true);
            if (meks.size() < row || row < 0) {
                return label;
            }

            if (table.getModel().getValueAt(row, column) != null) {
                label.setText(table.getModel().getValueAt(row, column).toString());
            }

            label.setToolTipText("");

            CBMUnit mm = meks.get(table.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID));
            String description = getDescription(mm);
            label.setToolTipText(description);

            if (isSelected) {
                label.setForeground(tableCellRendererComponent.getForeground());
                label.setBackground(tableCellRendererComponent.getBackground());
                return label;
            }

            if (mm.playerIsSeller()) {
                label.setBackground(java.awt.Color.lightGray);
            } else if (table.getModel().getValueAt(row, BID) != null) {
                label.setBackground(java.awt.Color.green);
            } else {
                label.setBackground(java.awt.Color.white);
            }
            return label;
        }

        private @NonNull String getDescription(CBMUnit mm) {
            String description = "";
            if (!hiddenUnits) {
                description = STR."<html><body>#\{mm.getAuctionID()} \{mm.getEmbeddedUnit()
                                                                             .getEntity()
                                                                             .getChassis()} (\{mm.getEmbeddedUnit()
                                                                                                     .getEntity()
                                                                                                     .getModel()})<br>";


                if (mm.getEmbeddedUnit().getC3Level() > Unit.C3_NONE) {
                    if (mm.getEmbeddedUnit().getC3Level() == Unit.C3_SLAVE) {
                        description += "<br>" + "C3 Slave";
                    } else if (mm.getEmbeddedUnit().getC3Level() == Unit.C3_MASTER) {
                        description += "<br>" + "C3 Master";
                    } else if (mm.getEmbeddedUnit().getC3Level() == Unit.C3_IMPROVED) {
                        description += "<br>" + "C3 Improved";
                    }
                }

                description += "</body></html>";
            }
            return description;
        }
    }
}
