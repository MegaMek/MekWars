package mekwars.common.gui.dialogs;

import java.io.Serial;

import megamek.common.loaders.MekSummary;

/**
 * A {@link javax.swing.table.AbstractTableModel} backing a plain summary-style table of MegaMek units (e.g. a
 * mek-picker/browse list). Each row corresponds to one {@link MekSummary} (a lightweight, pre-parsed
 * description of a unit design pulled from MegaMek's unit cache, not a fully-loaded {@code Entity}), and each
 * column shows one attribute of that design: chassis, model, weight, Battle Value, introduction year, cost, and
 * tech level.
 * <p>
 * NOTE: This class lives in {@code mekwars.common.gui.dialogs} and is unrelated to (and should not be confused
 * with) {@code mekwars.common.gui.models.MekTableModel}, a differently-implemented table model in a different
 * package.
 * <p>
 * The model is populated after construction via {@link #setData(MekSummary[])}; there is no way to add or
 * remove individual rows once data has been set other than replacing the whole array.
 */
public class MekTableModel extends javax.swing.table.AbstractTableModel {

    /** Column index for the unit's chassis name. */
    final static int COL_CHASSIS = 0;
    /** Column index for the unit's model/variant designation. */
    final static int COL_MODEL = 1;
    /** Column index for the unit's tonnage. */
    final static int COL_WEIGHT = 2;
    /** Column index for the unit's Battle Value. */
    final static int COL_BV = 3;
    /** Column index for the unit's introduction year. */
    final static int COL_YEAR = 4;
    /** Column index for the unit's in-game purchase cost. */
    final static int COL_COST = 5;
    /** Column index for the unit's tech/rules level. */
    final static int COL_LEVEL = 6;
    /** Total number of columns exposed by this model. */
    final static int N_COL = 7;
    @Serial
    private static final long serialVersionUID = -5457068129532709857L;
    // Backing row data; replaced wholesale by setData(). Defaults to an empty array so the table starts empty
    // rather than null before the first setData() call.
    private MekSummary[] data = new MekSummary[0];

    /** @return the current number of rows (unit summaries) in the model. */
    public int getRowCount() {
        return data.length;
    }

    /** @return the fixed number of columns this model always exposes ({@link #N_COL}). */
    public int getColumnCount() {
        return N_COL;
    }

    /**
     * Returns the cell value for the given row/column, pulled from the corresponding {@link MekSummary}.
     *
     * @param row the row index (0-based)
     * @param col the column index; one of the {@code COL_*} constants
     * @return the requested attribute of the unit summary at {@code row}, or the literal string {@code "?"} if
     *         {@code row} is out of range or {@code col} does not match a known column
     */
    public Object getValueAt(int row, int col) {
        if (data.length <= row) {
            return "?";
        }

        MekSummary ms = data[row];
        if (col == COL_MODEL) {
            return ms.getModel();
        }
        if (col == COL_CHASSIS) {
            return ms.getChassis();
        }
        if (col == COL_WEIGHT) {
            return ms.getTons();
        }
        if (col == COL_BV) {
            return ms.getBV();
        }
        if (col == COL_YEAR) {
            return ms.getYear();
        }
        if (col == COL_COST) {
            //return NumberFormat.getInstance().format(ms.getCost());
            return ms.getCost();
        }
        if (col == COL_LEVEL) {
            return ms.getLevel();
        }
        return "?";
    }

    /**
     * @param column the column index; one of the {@code COL_*} constants
     * @return the human-readable header text for the given column, or {@code "?"} for an unrecognized index
     */
    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case COL_MODEL -> "Model";
            case COL_CHASSIS -> "Chassis";
            case COL_WEIGHT -> "Weight";
            case COL_BV -> "BV";
            case COL_YEAR -> "Year";
            case COL_COST -> "Price";
            case COL_LEVEL -> "Level";
            default -> "?";
        };
    }

    /**
     * Determines a column's runtime type by inspecting the value in row 0. NOTE: this means the model requires
     * at least one row of data to be present before {@code JTable} can safely query column classes (e.g. for
     * choosing a renderer/comparator) -- calling this while {@link #data} is empty will throw a
     * {@link ArrayIndexOutOfBoundsException} / NullPointerException from {@link #getValueAt(int, int)}.
     *
     * @param c the column index
     * @return the runtime {@link Class} of the value found in row 0, column {@code c}
     */
    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    /**
     * @param i the row index
     * @return the {@link MekSummary} backing row {@code i}
     */
    public MekSummary getMechSummary(int i) {
        return data[i];
    }

    /**
     * Replaces all row data with the given array and notifies listeners (e.g. the JTable) that the entire table
     * contents changed.
     *
     * @param ms the new set of unit summaries to display, one per row
     */
    //fill table with values
    public void setData(MekSummary[] ms) {
        data = ms;
        fireTableDataChanged();
    }

}
