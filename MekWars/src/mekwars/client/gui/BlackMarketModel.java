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
 * Created on June 21, 2002, 2:45 PM
 */

package client.gui;


import java.awt.Color;
import java.awt.Component;
import java.util.TreeMap;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import client.MWClient;
import client.campaign.CBMUnit;
import client.campaign.CCampaign;
import common.Unit;

/**
 *
 * @author Steve Hawkins
 */
public class BlackMarketModel extends AbstractTableModel {

    /**
     * 
     */
    private static final long serialVersionUID = -783116408720137035L;
    public MWClient mwclient;
    CCampaign theCampaign;
    public TreeMap<Integer,CBMUnit> mechs; //this collection is backed by the main map, so it should always be good
    //public TreeMap bids;
    public Object[] sortedMechs; //not really though, sort is handled elsewhere...

    public final static int MECH = 0;
    public final static int BV = 1;
    public final static int MIN = 2;
    public final static int TICKS = 3;
    public final static int BID = 4;
    public final static int AUCTION_ID = 5; //not shown in table

    private boolean hiddenUnits = false;
    
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

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public BlackMarketModel(MWClient client, boolean hideBMUnits)
    {
        this.mwclient = client;
        theCampaign = mwclient.getCampaign();
        this.mechs = theCampaign.getBlackMarket();
        hiddenUnits = hideBMUnits;
        //    this.bids = client.getMyBids();

        this.sortedMechs = this.mechs.values().toArray();
    }

    public void refreshModel() {
        //do a resort
        this.sortedMechs = this.mechs.values().toArray();
        this.fireTableDataChanged();
    }

    public void initColumnSizes(JTable table) {
        TableColumn column = null;
        Component comp = null;
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
        return this.sortedMechs.length;
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
        if (row < 0) return "";
        if (row >= sortedMechs.length) return "";
        CBMUnit mm = (CBMUnit)this.sortedMechs[row];
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
                if (mm.getBid() > 0)
                    return mm.getBid();
                return null;
            case AUCTION_ID:
                return mm.getAuctionID();
        }
        return "";
    }

    public BlackMarketModel.Renderer getRenderer() {
        return new Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    private class Renderer extends DefaultTableCellRenderer {

        /**
         * 
         */
        private static final long serialVersionUID = 5506902358006897558L;

        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component d =  super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,column);

            JLabel c = new JLabel(); //use a new label for everything (should be made better later)
            c.setOpaque(true);
            if (mechs.size() < row || row < 0) return c;
            if (table.getModel().getValueAt(row, column) != null) {
                c.setText(table.getModel().getValueAt(row, column).toString());
            }
            c.setToolTipText("");

            CBMUnit mm = (CBMUnit)mechs.get(table.getModel().getValueAt(row, BlackMarketModel.AUCTION_ID));
            String description = "";
            if (!hiddenUnits) {
            	description="<html><body>#" + mm.getAuctionID() + " " + mm.getEmbeddedUnit().getEntity().getChassis()
            	+ " (" + mm.getEmbeddedUnit().getEntity().getModel() + ")<br>";
            

            	if (mm.getEmbeddedUnit().getC3Level() > Unit.C3_NONE){
            		if ( mm.getEmbeddedUnit().getC3Level() == Unit.C3_SLAVE)
            			description += "<br>" + "C3 Slave";
            		else if (mm.getEmbeddedUnit().getC3Level() == Unit.C3_MASTER)
            			description += "<br>" + "C3 Master";
            		else if (mm.getEmbeddedUnit().getC3Level() == Unit.C3_IMPROVED)
            			description += "<br>" + "C3 Improved";
            	}

            	description += "</body></html>";
            }
            c.setToolTipText(description);
            if (isSelected) {
                c.setForeground(d.getForeground());
                c.setBackground(d.getBackground());
                return c;
            }

            if (mm.playerIsSeller()) {
                c.setBackground(Color.lightGray);
            } else if (table.getModel().getValueAt(row, BID) != null) {
                c.setBackground(Color.green);
            } else {
                c.setBackground(Color.white);
            }
            return c;
        }
    }
}