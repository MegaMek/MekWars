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

package mekwars.common.gui;

import java.io.Serial;
import java.io.Serializable;
import java.util.Vector;
import javax.swing.table.TableModel;

import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;

/**
 * A sorter for TableModels. The sorter has a model (conforming to TableModel) and itself implements TableModel.
 * TableSorter does not store or copy the data in the TableModel, instead it maintains an array of integers which it
 * keeps the same size as the number of rows in its model. When the model changes it notifies the sorter that something
 * has changed eg. "rowsAdded" so that its internal array of integers can be reallocated. As requests are made of the
 * sorter (like getValueAt(row, col) it redirects them to its model via the mapping array. That way the TableSorter
 * appears to hold another copy of the table with the rows in a different order. The sorting algorthm used is stable
 * which means that it does not move around rows when its comparison function returns 0 to denote that they are
 * equivalent.
 *
 * @author Philip Milne
 * @version 1.5 12/17/97
 * <p>
 * In MekWars this class backs the sortable tables in the client UI (Black Market, Table Browser/build tables,
 * Battles list, and Black Market parts), providing click-on-column-header sorting via
 * {@link #addMouseListenerToHeaderInTable(javax.swing.JTable)}. Each usage is identified by one of the
 * {@code SORTER_*} mode constants, which determines which config keys are used to persist/restore the
 * user's last-used sort column and direction across sessions (see {@link #loadSavedSortPreferences(int)} and
 * {@link #saveSortPreferences()}).
 */
public class TableSorter extends TableMap implements Serializable {
    // VARIABLES
    /** Sort mode identifying the Black Market unit table. */
    public final static int SORTER_BM = 0;
    /** Sort mode identifying the build/table-browser table. */
    public final static int SORTER_BUILD_TABLES = 1;
    /** Sort mode identifying the Battles list table. */
    public final static int SORTER_BATTLES = 2;
    /** Sort mode identifying the Black Market parts table. */
    public final static int SORTER_BM_PARTS = 3;
    private final static MMLogger LOGGER = MMLogger.create(TableSorter.class);

    /**
     * Serialization version identifier for this {@link javax.swing.table.TableModel}.
     */
    @Serial
    private static final long serialVersionUID = -3715062654870040447L;
    /**
     * The list of column indexes currently used as sort keys, in priority order. In practice this codebase only
     * ever adds a single column (see {@link #sortByColumn(int, boolean)}), so multi-column sorting, while
     * supported by {@link #compare(int, int)}, is not currently exercised.
     */
    private final Vector<Integer> sortingColumns = new Vector<>(1, 1);
    /** Which table/screen this sorter instance belongs to; one of the {@code SORTER_*} constants. */
    private final int sortMode;
    /** Whether the currently-persisted sort ({@link #currentColumn}) is ascending. */
    private boolean currentOrder = true;// ascending
    /** Client reference used to read/write persisted sort-column config parameters. */
    private IClient client = null;
    /** The column index last used for sorting (persisted); {@code -2} means "no sort column selected yet". */
    private int currentColumn = -2;
    /** Row-index mapping from view row (as exposed by this sorter) to underlying model row. */
    private int[] indexes;
    /** Whether the sort currently being performed by {@link #compare(int, int)} is ascending. */
    private boolean ascending = true;
    /** Running count of comparisons performed during the last {@link #sort(Object)} call (diagnostic only). */
    private int compares;

    // CONSTRUCTOR
    /**
     * Creates a sorter wrapping the given table model.
     *
     * @param model the underlying table model to wrap and sort
     * @param client the client used to read/persist sort-column preferences
     * @param mode   one of the {@code SORTER_*} constants identifying which table this sorter is for (controls
     *               which config keys are used for persistence)
     */
    public TableSorter(TableModel model, IClient client, int mode) {
        setModel(model);

        this.client = client;
        this.sortMode = mode;
        this.loadSavedSortPreferences(mode);
    }

    /**
     * Sets the wrapped model and rebuilds the row-index mapping to match its current row count (identity mapping,
     * i.e. unsorted) via {@link #reallocateIndexes()}.
     */
    @Override
    public void setModel(TableModel model) {
        super.setModel(model);
        reallocateIndexes();
    }

    /**
     * Returns the value at the given view row/column by first translating the view row to a model row via
     * {@link #indexes}.
     *
     * @param aRow    the view (sorted) row index
     * @param aColumn the column index
     *
     * @return the cell value, or {@code null} if {@code aRow} is out of bounds of the current index mapping
     */
    @Override
    public Object getValueAt(int aRow, int aColumn) {
        if (aRow < 0 || aRow >= indexes.length) {
            return null;
        }

        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    /**
     * Sanity check that logs a debug message if the row-index mapping's length has drifted out of sync with the
     * underlying model's row count (i.e. the model changed without notifying this sorter). This is diagnostic only
     * - it does not repair the mismatch or throw.
     */
    public void checkModel() {
        if (indexes.length != model.getRowCount()) {
            LOGGER.debug("Sorter not informed of a change in model.");
        }
    }

    /**
     * Sets a value at the given view row/column, translating the view row to the underlying model row first.
     */
    @Override
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }

    /**
     * Handles change notifications from the wrapped model: rebuilds the identity row-index mapping (discarding any
     * current sort order), forwards the event to this sorter's own listeners, then immediately re-applies the
     * previously active sort column/direction via {@link #restorePreviousSort()} so that the displayed table stays
     * sorted after the underlying data is refreshed.
     *
     * @param e the table model event from the wrapped model
     */
    @Override
    public void tableChanged(javax.swing.event.TableModelEvent e) {
        reallocateIndexes();

        super.tableChanged(e);

        // table changed, now restore the old sort
        this.restorePreviousSort();
    }

    /**
     * Loads the previously-persisted sort column and direction for the given sort mode from the client's config,
     * populating {@link #currentColumn} and {@link #currentOrder}. Note the {@code SORTER_BATTLES} branch checks
     * the field {@code sortMode} rather than the {@code mode} parameter (both are set to the same value by the
     * constructor before this is called, so behavior is equivalent, but it is an inconsistency worth noting).
     * <p>
     * Possible bug: for {@code SORTER_BM} this reads config keys {@code "BM_SORT_COLUMN"}/{@code "BM_SORT_ORDER"}
     * (with underscores), whereas {@link #saveSortPreferences()} writes {@code "BMSORTCOLUMN"}/
     * {@code "BMSORTORDER"} (no underscores) - the same keys used as {@link GUIClientConfig} defaults. Since the
     * underscored keys are never populated, {@code client.getConfigParam("BM_SORT_COLUMN")} likely returns an empty
     * string, which would make the {@code Integer.parseInt} call here throw for the BM sorter unless the caller/
     * config layer tolerates that.
     *
     * @param mode one of the {@code SORTER_*} constants selecting which config keys to read
     */
    public void loadSavedSortPreferences(int mode) {

        if (mode == mekwars.common.gui.TableSorter.SORTER_BM) {
            currentColumn = Integer.parseInt(client.getConfigParam("BM_SORT_COLUMN"));
            currentOrder = Boolean.parseBoolean(client.getConfigParam("BM_SORT_ORDER"));
        } else if (mode == mekwars.common.gui.TableSorter.SORTER_BUILD_TABLES) {
            currentColumn = Integer.parseInt(client.getConfigParam("TABLEBROWSERSORTCOLUMN"));
            currentOrder = Boolean.parseBoolean(client.getConfigParam("TABLEBROWSERSORTORDER"));
        } else if (sortMode == SORTER_BATTLES) {
            currentColumn = Integer.parseInt(client.getConfigParam("BATTLESSORTCOLUMN"));
            currentOrder = Boolean.parseBoolean(client.getConfigParam("BATTLESSORTORDER"));
        } else if (mode == mekwars.common.gui.TableSorter.SORTER_BM_PARTS) {
            currentColumn = Integer.parseInt(client.getConfigParam("BMESORTCOLUMN"));
            currentOrder = Boolean.parseBoolean(client.getConfigParam("BMESORTORDER"));
        }
    }

    /**
     * Rebuilds the {@link #indexes} row-mapping array to match the wrapped model's current row count, resetting it
     * to the identity mapping (view row == model row, i.e. unsorted). Called whenever the model's row count may
     * have changed.
     */
    public void reallocateIndexes() {
        int rowCount = model.getRowCount();

        // Set up a new array of indexes with the right number of elements
        // for the new data model.
        indexes = new int[rowCount];

        // Initialise with the identity mapping.
        for (int row = 0; row < rowCount; row++) {
            indexes[row] = row;
        }
    }

    /**
     * Performs the actual sort of {@link #indexes} using the current {@link #sortingColumns}/{@link #ascending}
     * state, via the stable merge sort implemented in {@link #shuttleSort(int[], int[], int, int)}. Resets the
     * {@link #compares} diagnostic counter first.
     *
     * @param sender unused (kept for compatibility with the original Sun/Java-Tutorial API this class is derived
     *               from)
     */
    public void sort(Object sender) {
        checkModel();

        compares = 0;
        shuttleSort(indexes.clone(), indexes, 0, indexes.length);
    }

    /**
     * Alternate, unused O(n^2) bubble-style sort. Note this method is not called anywhere in {@link #sort(Object)}
     * or elsewhere in this class (the shuttle/merge sort is used instead) and appears to be dead code carried over
     * from the original example this class is derived from. It is also arguably buggy: it only swaps when
     * {@link #compare(int, int)} returns exactly {@code -1}, but {@link #compareRowsByColumn(int, int, int)} can
     * return other negative values (e.g. via {@code Double.compare}/{@code Long.compare}), so out-of-order rows
     * with a comparison result other than -1 would not be swapped.
     */
    public void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i + 1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    /**
     * Compares two model rows using the active sort columns (in priority order), applying {@link #ascending} to
     * flip the result if a descending sort is requested. Increments the {@link #compares} diagnostic counter.
     *
     * @param row1 first model row index
     * @param row2 second model row index
     *
     * @return negative/zero/positive per the usual {@link java.util.Comparator} contract; {@code 0} if all sort
     *       columns compare equal
     */
    public int compare(int row1, int row2) {
        compares++;
        for (int level = 0; level < sortingColumns.size(); level++) {
            int column = sortingColumns.elementAt(level);
            int result = compareRowsByColumn(row1, row2, column);
            if (result != 0) {
                return ascending ? result : -result;
            }
        }
        return 0;
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".

    /**
     * Swaps two entries of the {@link #indexes} row-mapping array (does not touch the underlying model data).
     */
    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    // METHODS
    /**
     * Compares two model rows on a single column, dispatching on the column's declared class:
     * {@link Number} subclasses are compared numerically (as doubles), {@link java.util.Date} by epoch millis,
     * {@link String} lexicographically, and {@link Boolean} with {@code false < true}. Any other column type falls
     * back to parsing as an {@link Integer} if the value is one, otherwise falls back to comparing
     * {@code toString()} representations (with a second fallback if that first attempt throws). {@code null} values
     * sort before non-null values; two {@code null}s compare equal.
     *
     * @param row1   first model row index
     * @param row2   second model row index
     * @param column the column to compare on
     *
     * @return negative/zero/positive per the usual comparison contract
     */
    public int compareRowsByColumn(int row1, int row2, int column) {
        Class<?> type = model.getColumnClass(column);
        javax.swing.table.TableModel data = model;

        // Check for nulls.

        Object o1 = data.getValueAt(row1, column);
        Object o2 = data.getValueAt(row2, column);

        // If both values are null, return 0.
        if (o1 == null && o2 == null) {
            return 0;
        } else if (o1 == null) { // Define null less than everything.
            return -1;
        } else if (o2 == null) {
            return 1;
        }

        /*
         * We copy all returned values from the getValue call in case an
         * optimised model is reusing one object to return many values. The
         * Number subclasses in the JDK are immutable and so will not be used in
         * this way but other subclasses of Number might want to do this to save
         * space and avoid unnecessary heap allocation.
         */

        if (type.getSuperclass() == Number.class) {
            Number n1 = (Number) data.getValueAt(row1, column);
            double d1 = n1.doubleValue();
            Number n2 = (Number) data.getValueAt(row2, column);
            double d2 = n2.doubleValue();

            return Double.compare(d1, d2);
        } else if (type == java.util.Date.class) {
            java.util.Date d1 = (java.util.Date) data.getValueAt(row1, column);
            long n1 = d1.getTime();
            java.util.Date d2 = (java.util.Date) data.getValueAt(row2, column);
            long n2 = d2.getTime();

            return Long.compare(n1, n2);
        } else if (type == String.class) {
            String s1 = (String) data.getValueAt(row1, column);
            String s2 = (String) data.getValueAt(row2, column);
            int result = s1.compareTo(s2);

            return Integer.compare(result, 0);
        } else if (type == Boolean.class) {
            boolean b1 = (Boolean) data.getValueAt(row1, column);
            boolean b2 = (Boolean) data.getValueAt(row2, column);

            if (b1 == b2) {
                return 0;
            } else if (b1) { // Define false < true
                return 1;
            } else {
                return -1;
            }
        } else {
            int result;
            try {
                Object v1 = data.getValueAt(row1, column);
                Object v2 = data.getValueAt(row2, column);

                if (v1 instanceof Integer) {
                    Integer i1 = Integer.parseInt(v1.toString());
                    Integer i2 = Integer.parseInt(v2.toString());
                    result = i1.compareTo(i2);
                } else {
                    String s1 = v1.toString();
                    String s2 = v2.toString();
                    result = s1.compareTo(s2);
                }
            } catch (Exception ex) {
                Object v1 = data.getValueAt(row1, column);
                String s1 = v1.toString();
                Object v2 = data.getValueAt(row2, column);
                String s2 = v2.toString();
                result = s1.compareTo(s2);
            }
            return Integer.compare(result, 0);
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
    /**
     * Recursive stable merge sort over the {@code from}/{@code to} index arrays, sorting the half-open range
     * {@code [low, high)}. See the block comment above for the original author's notes on its performance
     * characteristics and the rationale for using a stable sort instead of a faster unstable one (e.g. quicksort).
     *
     * @param from source array for this level of the merge
     * @param to   destination array for this level of the merge
     * @param low  inclusive lower bound of the range being sorted
     * @param high exclusive upper bound of the range being sorted
     */
    public void shuttleSort(int[] from, int[] to, int low, int high) {
        if (high - low < 2) {
            return;
        }
        int middle = (low + high) / 2;
        shuttleSort(to, from, low, middle);
        shuttleSort(to, from, middle, high);

        int p = low;
        int q = middle;

        /*
         * This is an optional short-cut; at each recursive call, check to see
         * if the elements in this subset are already ordered. If so, no further
         * comparisons are needed; the sub-array can just be copied. The array
         * must be copied rather than assigned otherwise sister calls in the
         * recursion might get out of sinc. When the number of elements is three
         * they are partitioned so that the first set, [low, mid), has one
         * element and and the second, [mid, high), has two. We skip the
         * optimisation when the number of elements is three or less as the
         * first compare in the normal merge will produce the same sequence of
         * steps. This optimisation seems to be worthwhile for partially ordered
         * lists but some analysis is needed to find out how the performance
         * drops to Nlog(N) as the initial order diminishes - it may drop very
         * quickly.
         */

        if (high - low >= 4 && compare(from[middle - 1], from[middle]) <= 0) {
            if (high - low >= 0) {
                System.arraycopy(from, low, to, low, high - low);
            }

            return;
        }

        // A normal merge.

        for (int i = low; i < high; i++) {
            if (q >= high || (p < middle && compare(from[p], from[q]) <= 0)) {
                to[i] = from[p++];
            } else {
                to[i] = from[q++];
            }
        }
    }

    /**
     * Sorts by the given column in ascending order. Shorthand for {@link #sortByColumn(int, boolean)}.
     */
    public void sortByColumn(int column) {
        sortByColumn(column, true);
    }

    /**
     * Replaces the current sort key with the single given column, performs the sort, then notifies listeners
     * (e.g. the JTable) that the table's contents have changed so the view repaints in the new order. Note this
     * does not update {@link #currentColumn}/{@link #currentOrder} itself - callers (e.g. the mouse listener
     * installed by {@link #addMouseListenerToHeaderInTable(javax.swing.JTable)}, or
     * {@link #restorePreviousSort()}) are responsible for keeping those fields in sync.
     *
     * @param column    the column to sort by
     * @param ascending whether to sort ascending ({@code true}) or descending ({@code false})
     */
    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
        sortingColumns.removeAllElements();
        sortingColumns.addElement(column);
        sort(this);
        super.tableChanged(new javax.swing.event.TableModelEvent(this));
    }

    /**
     * Method used to restore a pre-existing sort order after BM data is refreshed.
     */
    public void restorePreviousSort() {
        // only restore if a column was actually selected
        if (currentColumn != -2) {this.sortByColumn(currentColumn, currentOrder);}
    }// end restorePreviousSort

    /**
     * Persists the current sort column ({@link #currentColumn}) and direction ({@link #currentOrder}) to the
     * client's config, using the config key pair appropriate for this sorter's {@link #sortMode}, then saves the
     * config to disk and pushes the updated config back to the client via {@link IClient#setConfig()}.
     */
    public void saveSortPreferences() {
        if (sortMode == SORTER_BM) {
            client.getConfig().setParam("BMSORTCOLUMN", Integer.toString(currentColumn));
            client.getConfig().setParam("BMSORTORDER", Boolean.toString(currentOrder));
        } else if (sortMode == SORTER_BUILD_TABLES) {
            client.getConfig().setParam("TABLEBROWSERSORTCOLUMN", Integer.toString(currentColumn));
            client.getConfig().setParam("TABLEBROWSERSORTORDER", Boolean.toString(currentOrder));
        } else if (sortMode == SORTER_BATTLES) {
            client.getConfig().setParam("BATTLESSORTCOLUMN", Integer.toString(currentColumn));
            client.getConfig().setParam("BATTLESSORTORDER", Boolean.toString(currentOrder));
        } else if (sortMode == SORTER_BM_PARTS) {
            client.getConfig().setParam("BMESORTCOLUMN", Integer.toString(currentColumn));
            client.getConfig().setParam("BMESORTORDER", Boolean.toString(currentOrder));
        }
        client.getConfig().saveConfig();
        client.setConfig();
    }

    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    /**
     * Installs a mouse listener on the given table's column header so that clicking a header sorts the table by
     * that column, toggling ascending/descending when the same column is clicked again. Also disables column
     * selection (so header clicks aren't interpreted as column-selection drags) and persists the new sort
     * preference via {@link #saveSortPreferences()} after each click. When a column is clicked for the first time
     * (i.e. it wasn't already the current sort column), the default direction is ascending, except for column 3 of
     * a {@code SORTER_BUILD_TABLES} sorter, which defaults to descending (used for "frequency"-style columns in the
     * table browser).
     *
     * @param table the JTable whose header should become clickable for sorting
     */
    public void addMouseListenerToHeaderInTable(javax.swing.JTable table) {
        final mekwars.common.gui.TableSorter sorter = this;
        final javax.swing.JTable tableView = table;
        tableView.setColumnSelectionAllowed(false);
        java.awt.event.MouseAdapter listMouseListener = new java.awt.event.MouseAdapter() {

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                // JPopupMenu popup = new JPopupMenu();

                // check the column
                final int column = tableView.getColumnModel().getColumnIndexAtX(e.getX());

                // break out if its a bum column
                if (column == -1) {return;}

                // sorting the same column, again. toggle.
                if (column == currentColumn) {

                    // if currently ascending do an ascending sort,
                    // and vice versa.
                    if (currentOrder) {
                        sorter.sortByColumn(column, false);
                        tableView.repaint();
                        currentOrder = false;
                    } else {
                        sorter.sortByColumn(column, true);
                        tableView.repaint();
                        currentOrder = true;
                    }
                    sorter.saveSortPreferences();
                }// end if(sorting same column)

                else {

                    /*
                     * All sorts start ascending (true), except TableViewer
                     * Frequency sorts. These default to decending.
                     */
                    if (sortMode == SORTER_BUILD_TABLES && column == 3) {
                        sorter.sortByColumn(column, false);
                        currentOrder = false;
                    } else {
                        sorter.sortByColumn(column, true);
                        currentOrder = true;
                    }

                    tableView.repaint();
                    currentColumn = column;
                    sorter.saveSortPreferences();
                }
            }

        };
        javax.swing.table.JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(listMouseListener);
    }
}
