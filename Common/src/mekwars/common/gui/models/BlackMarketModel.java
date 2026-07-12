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
 * Table model backing the black-market unit auction JTable. Each row represents one
 * {@link CBMUnit} (a unit consigned to the campaign's black market, whether by another player or
 * an NPC seller) that can be bid on; columns show the unit's identity, stock battle value, minimum
 * bid, remaining auction ticks, and the current player's own bid (if any).
 * <p>
 * When {@code hiddenUnits} is set (a "blind auction" mode), the unit's real identity and battle
 * value are withheld from the player and a generic {@link CBMUnit#getHiddenUnitDescription()} is
 * shown instead — see {@link #initColumnSizes(JTable)} and {@link #getValueAt(int, int)}.
 *
 * @author Steve Hawkins
 */
public class BlackMarketModel extends AbstractTableModel {

    /** Column index: unit chassis/model name (or hidden-unit placeholder text). */
    public final static int MECH = 0;
    /** Column index: the unit's stock battle value. */
    public final static int BV = 1;
    /** Column index: minimum acceptable bid. */
    public final static int MIN = 2;
    /** Column index: number of auction "ticks" (time units) remaining before the auction closes. */
    public final static int TICKS = 3;
    /** Column index: the current player's own bid on this unit, if one has been placed. */
    public final static int BID = 4;
    /**
     * Pseudo-column used internally to look up a row's underlying {@link CBMUnit} by auction ID
     * (e.g. from the {@link Renderer}). Not a real table column: {@link #columnNames} only has 5
     * entries, so {@link #getColumnCount()} never exposes this index to the JTable itself, but
     * {@link #getValueAt(int, int)} still handles it when called directly.
     */
    public final static int AUCTION_ID = 5; //not shown in table
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -783116408720137035L;
    /** Header labels; note there is no header for {@link #AUCTION_ID} since it isn't a real column. */
    final String[] columnNames = {
          "Unit",
          "Stock BV",
          "Min Bid",
          "Ticks",
          "Your Bid",
          };
    /**
     * Dummy "wide" strings used only to measure preferred column widths in
     * {@link #initColumnSizes(JTable)}; never displayed.
     */
    final String[] longValues = {
          "XXXXXX-XXXX-XXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          "XXXXXXXXX",
          };
    /** Client connection used to reach the campaign and the current player's data. */
    public IClient client;

    /** Live map of all black-market units, keyed by auction ID; sourced from the campaign. */
    public TreeMap<Integer, CBMUnit> meks;
    //public TreeMap bids;
    /** Row-order snapshot of {@link #meks}'s values. Despite the name, not actually kept sorted
     *  here — see comment below. */
    public List<CBMUnit> sortedMeks; //not really though, sort is handled elsewhere...
    /** The active campaign, used to reach the black-market unit map. */
    CCampaign theCampaign;
    /** When true, unit identity/BV are hidden from the viewing player (blind auction mode). */
    private boolean hiddenUnits = false;

    /**
     * @param client      client connection providing access to the campaign and player
     * @param hideBMUnits if true, unit identity and battle value are withheld from display
     *                    (blind-auction mode)
     */
    public BlackMarketModel(IClient client, boolean hideBMUnits) {
        this.client = client;
        theCampaign = this.client.getCampaign();
        this.meks = theCampaign.getBlackMarket();
        hiddenUnits = hideBMUnits;
        //    this.bids = client.getMyBids();

        this.sortedMeks = new ArrayList<>(this.meks.values());
    }

    /**
     * Re-snapshots {@link #sortedMeks} from the live {@link #meks} map and notifies listeners that
     * the entire table changed.
     */
    public void refreshModel() {
        //do a resort
        this.sortedMeks = new ArrayList<>(this.meks.values());
        this.fireTableDataChanged();
    }

    /**
     * Sizes each column to fit the wider of its header and a representative dummy value
     * ({@link #longValues}).
     * <p>
     * Hack: when {@code hiddenUnits} is active, the {@link #BV} column is additionally collapsed to
     * zero width (min/max/preferred all 0) to effectively hide it, since {@code removeColumn()} was
     * found to throw errors when attempted instead (see inline comment).
     *
     * @param table the JTable this model is installed on
     */
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

    /**
     * Returns the display value for a given cell, or {@code ""} for out-of-range rows. Behavior per
     * column:
     * <ul>
     *   <li>{@link #MECH} — hidden-unit placeholder text or the real model name, depending on
     *       {@code hiddenUnits}</li>
     *   <li>{@link #BV} — a single space when hidden, otherwise the battle value is recalculated
     *       from the embedded entity on every call (not cached)</li>
     *   <li>{@link #MIN}, {@link #TICKS} — passed straight through</li>
     *   <li>{@link #BID} — {@code null} (not zero) when the player has no active bid, so the cell
     *       renders blank rather than "0"</li>
     *   <li>{@link #AUCTION_ID} — the raw auction ID; only reachable when called directly (not
     *       through the JTable, since it isn't a real column — see {@link #AUCTION_ID})</li>
     * </ul>
     */
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

    /** @return a fresh cell renderer bound to this model's live data. */
    public Renderer getRenderer() {
        return new Renderer();
    }

    /*
     * Rendered cannot be static because it uses parent data structs.
     */
    /**
     * Cell renderer for the black-market table. Builds a tooltip describing the unit (auction ID,
     * chassis/model, C3 equipment) and colors the row: light gray if the viewing player is the
     * seller of this lot, green if the player currently has an active bid on it, otherwise white.
     * Selected rows keep the look-and-feel's selection colors instead.
     * <p>
     * A brand new {@link JLabel} is created on every render call rather than reusing/configuring
     * the {@code super}-provided component (see inline comment acknowledging this should be
     * improved).
     */
    public class Renderer extends DefaultTableCellRenderer {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = 5506902358006897558L;

        /**
         * Note the bounds check uses {@code meks.size()} (the full live map) rather than
         * {@code sortedMeks.size()} (the row-order snapshot actually indexed by {@code row}); if the
         * two ever differ in size this guard does not perfectly match the data actually rendered,
         * though {@link #getValueAt(int, int)} still guards against an out-of-range {@code row}
         * itself.
         */
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

            // Looks up the row's unit by auction ID via the AUCTION_ID pseudo-column, bypassing
            // the JTable's column model entirely (AUCTION_ID has no header and is out of range
            // of getColumnCount()).
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

        /**
         * Builds the HTML tooltip text for a black-market unit: auction ID, chassis/model, and any
         * C3 equipment level. Returns an empty string when {@code hiddenUnits} is active, so blind
         * auctions don't leak identity via the tooltip either.
         *
         * @param mm the unit to describe
         * @return HTML tooltip markup, or {@code ""} if units are currently hidden
         */
        private @NonNull String getDescription(CBMUnit mm) {
            String description = "";
            if (!hiddenUnits) {
                description = String.format("<html><body>#%s %s (%s)<br>", mm.getAuctionID(), mm.getEmbeddedUnit()
                                                                             .getEntity()
                                                                             .getChassis(), mm.getEmbeddedUnit()
                                                                                                     .getEntity()
                                                                                                     .getModel());


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
