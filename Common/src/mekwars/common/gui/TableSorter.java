package mekwars.common.gui;

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
 * @version 1.5 12/17/97
 * @author Philip Milne
 */

import java.io.Serial;
import java.io.Serializable;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.MWLogger;

public class TableSorter extends TableMap implements Serializable {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -3715062654870040447L;
    // VARIABLES
    public final static int SORTER_BM = 0;
    public final static int SORTER_BUILD_TABLES = 1;
    public final static int SORTER_BATTLES = 2;
    public final static int SORTER_BM_PARTS = 3;

    int[] indexes;
    java.util.Vector<Integer> sortingColumns = new java.util.Vector<>(1, 1);
    boolean ascending = true;
    int compares;
    int sortMode;

    // store the "current" column in order to toggle
    // acending/decending sorts @urgru 3.27.05
    int currentColumn = -2;
    boolean currentOrder = true;// ascending
    IClient client = null;

    // CONSTRUCTOR
    public TableSorter(javax.swing.table.TableModel model, IClient client, int mode) {
        setModel(model);

        this.client = client;
        this.sortMode = mode;
        this.loadSavedSortPreferences(mode);
    }

    @Override
    public void setModel(javax.swing.table.TableModel model) {
        super.setModel(model);
        reallocateIndexes();
    }

    // METHODS
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

    @Override
    public void tableChanged(javax.swing.event.TableModelEvent e) {
        // MMClient.mwClientLog.clientOutputLog("Sorter: tableChanged");
        reallocateIndexes();

        super.tableChanged(e);

        // table changed, now restore the old sort
        this.restorePreviousSort();
    }

    public void checkModel() {
        if (indexes.length != model.getRowCount()) {
            MWLogger.errLog("Sorter not informed of a change in model.");
        }
    }

    public void sort(Object sender) {
        checkModel();

        compares = 0;
        shuttleSort(indexes.clone(), indexes, 0, indexes.length);
    }

    public void n2sort() {
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = i + 1; j < getRowCount(); j++) {
                if (compare(indexes[i], indexes[j]) == -1) {
                    swap(i, j);
                }
            }
        }
    }

    // This is a home-grown implementation which we have not had time
    // to research - it may perform poorly in some circumstances. It
    // requires twice the space of an in-place algorithm and makes
    // NlogN assigments shuttling the values between the two
    // arrays. The number of compares appears to vary between N-1 and
    // NlogN depending on the initial order but the main reason for
    // using it here is that, unlike qsort, it is stable.
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

    public void swap(int i, int j) {
        int tmp = indexes[i];
        indexes[i] = indexes[j];
        indexes[j] = tmp;
    }

    // The mapping only affects the contents of the data rows.
    // Pass all requests to these rows through the mapping array: "indexes".

    @Override
    public Object getValueAt(int aRow, int aColumn) {
        if (aRow < 0 || aRow >= indexes.length) {return null;}
        checkModel();
        return model.getValueAt(indexes[aRow], aColumn);
    }

    @Override
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        checkModel();
        model.setValueAt(aValue, indexes[aRow], aColumn);
    }

    public void sortByColumn(int column) {
        sortByColumn(column, true);
    }

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

    public void loadSavedSortPreferences(int mode) {

        if (mode == mekwars.common.gui.TableSorter.SORTER_BM) {
            currentColumn = Integer.parseInt(client.getConfigParam("BMSORTCOLUMN"));
            currentOrder = Boolean.parseBoolean(client.getConfigParam("BMSORTORDER"));
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
