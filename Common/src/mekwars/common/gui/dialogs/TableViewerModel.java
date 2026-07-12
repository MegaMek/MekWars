package mekwars.common.gui.dialogs;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.TreeMap;

import javax.swing.table.AbstractTableModel;

import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.CUnitComparator;

// inner classes
/**
 * Table model backing the "build table" viewer, which lists the units contained in a
 * random-unit assignment table (RAT/build table) along with their weight, battle value, and
 * how frequently they appear in the table. Modeled on the {@code BlackMarketModel} from
 * {@code client.gui}.
 * <p>
 * The model wraps two parallel views of the same data: {@link #currentUnits}, a lookup keyed
 * by unit filename, and {@link #sortedUnits}, a flat array in the current display order.
 * Sorting is triggered by calling {@link #setSortMode(int)} followed by {@link #refreshModel()},
 * which re-sorts {@link #sortedUnits} from {@link #currentUnits} according to
 * {@link #currentSortMode} and fires a table-data-changed event.
 * <p>
 * Note: {@link #columnNames} only declares 3 header labels ("Unit", "Weight", "BV") while
 * {@link #getColumnCount()} exposes {@link #columnNames}.length columns and {@link #getValueAt}
 * has cases for a 4th (FREQUENCY) and 5th (FILENAME) column index; those extra columns exist in
 * the sorting logic but are not actually rendered as table columns since the column count is
 * driven by the (shorter) {@code columnNames} array.
 */
class TableViewerModel extends AbstractTableModel {
    // IVARS
    // static ints
    /** Column/sort-mode constant for the unit name/model column. */
    public final static int UNIT = 0;// model/name
    /** Column/sort-mode constant for the unit weight (tons) column. */
    public final static int WEIGHT = 1;
    /** Column/sort-mode constant for the Battle Value column. */
    public final static int BATTLEVALUE = 2;
    /** Column/sort-mode constant for the table-appearance frequency column. */
    public final static int FREQUENCY = 3;
    /** Column/sort-mode constant for the underlying unit filename (not shown as a header). */
    public final static int FILENAME = 4;
    private final static MMLogger LOGGER = MMLogger.create(TableViewerModel.class);
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4544599978221999391L;
    /** Master lookup of every unit in the table, keyed by filename (or other unique key). */
    java.util.TreeMap<Object, TableUnit> currentUnits;
    /** Flattened, currently-sorted view of {@link #currentUnits} used to back table rows. */
    TableUnit[] sortedUnits;

    /** Which column/criterion {@link #sortedUnits} is currently sorted by; defaults to frequency. */
    int currentSortMode = TableViewerModel.FREQUENCY;

    // column name array
    /** Visible column headers, in display order. */
    String[] columnNames = { "Unit", "Weight", "BV" };
    // client reference
    /** Reference to the owning client; currently unused within this class beyond storage. */
    IClient client;

    // CONSTRUCTOR
    /**
     * Creates the table model over an existing set of units.
     *
     * @param client  the owning client (kept for potential future use)
     * @param current master unit lookup keyed by filename
     * @param sorted  initial sorted array of units to display (should be consistent with
     *                {@code current} and {@link #currentSortMode})
     */
    public TableViewerModel(IClient client, TreeMap<Object, TableUnit> current, TableUnit[] sorted) {
        this.client = client;
        currentUnits = current;
        sortedUnits = sorted;
    }

    // rowcount, for AbstractModel
    @Override
    public int getRowCount() {
        return sortedUnits.length;
    }

    // column count, for AbstractModel
    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    /**
     * Returns the display value for a given cell. Row is looked up in {@link #sortedUnits};
     * the column determines which unit attribute is returned:
     * <ul>
     *   <li>{@link #UNIT}: an HTML string with "Chassis, Model" for non-omni Meks, or just the
     *       model name otherwise</li>
     *   <li>{@link #WEIGHT}: the unit's weight in tons, truncated to an int</li>
     *   <li>{@link #BATTLEVALUE}: the unit's calculated Battle Value</li>
     *   <li>{@link #FREQUENCY}: the unit's table-appearance frequency, rounded to 2 decimal
     *       places and then reparsed back into a double</li>
     *   <li>{@link #FILENAME}: the unit's real underlying filename</li>
     * </ul>
     * Returns an empty string for an out-of-range row, a null unit at that row, or an
     * unrecognized column index; also returns an empty string (and logs) if an exception is
     * thrown while formatting the UNIT column.
     *
     * @param row row index into {@link #sortedUnits}
     * @param col one of {@link #UNIT}, {@link #WEIGHT}, {@link #BATTLEVALUE}, {@link #FREQUENCY},
     *            {@link #FILENAME}
     * @return the formatted cell value, or "" if unavailable
     */
    @Override
    public Object getValueAt(int row, int col) {

        // invalid row
        if ((row < 0) || (row >= sortedUnits.length)) {
            return "";
        }

        TableUnit currU = sortedUnits[row];

        if (currU == null) {
            return "";
        }

        switch (col) {
            case UNIT:
                try {
                    if ((currU.getType() == mekwars.common.Unit.MEK) &&
                              (currU.getEntity() != null) &&
                              !currU.getEntity().isOmni()) {
                        return String.format("<html><body>%s, %s", currU.getEntity().getChassis(), currU.getModelName());
                    }
                    // else
                    return String.format("<html><body>%s", currU.getModelName());
                } catch (Exception ex) {
                    LOGGER.error(ex, "Error in TableViewerModel.getValueAt for UNIT: {}", ex.getLocalizedMessage());
                    return "";
                }
            case WEIGHT:
                return (int) currU.getEntity().getWeight();

            case BATTLEVALUE:
                return currU.getEntity().calculateBattleValue();

            case FREQUENCY:
                DecimalFormat myFormatter = new DecimalFormat("##0.00");
                String val = myFormatter.format(currU.getFrequency());
                return MathUtility.parseDouble(val, 0.0);

            case FILENAME:
                return currU.getRealFilename();

        }

        return "";
    }

    // override naming
    @Override
    public String getColumnName(int column) {
        return (columnNames[column]);
    }

    /**
     * Determines a column's runtime class by sampling the value from row 0 and calling
     * {@code getClass()} on it. Note: this will throw a {@link NullPointerException} if there
     * are zero rows, since {@link #getValueAt(int, int)} returns the string {@code ""} (not
     * null) for an out-of-range row only when the row index itself is invalid relative to
     * {@link #sortedUnits}.length — with zero rows, row 0 is out of range and this still
     * returns "" safely, so in practice this only NPEs if a case is added to
     * {@code getValueAt} that can return null.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getValueAt(0, columnIndex).getClass();
    }

    // isEditable, overridden from AbstractModel
    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    /**
     * Changes which criterion future sorts (via {@link #refreshModel()}) will use. Does not
     * itself re-sort or repaint; call {@link #refreshModel()} afterward to apply it.
     *
     * @param sortMode one of {@link #UNIT}, {@link #WEIGHT}, {@link #BATTLEVALUE},
     *                  {@link #FREQUENCY}
     */
    public void setSortMode(int sortMode) {
        currentSortMode = sortMode;
    }

    /**
     * Builds a fresh {@link TableViewerRenderer} bound to this model, used to render cells with
     * unit-specific tooltips (source table breakdown, chassis/model formatting).
     *
     * @return a new renderer instance for this model
     */
    public TableViewerRenderer getRenderer() {
        return new TableViewerRenderer(this);
    }

    /**
     * Re-sorts {@link #sortedUnits} from {@link #currentUnits} using {@link #currentSortMode}
     * and notifies listeners (e.g. the JTable) that the table data has changed, causing a
     * repaint with the new order.
     */
    public void refreshModel() {
        sortedUnits = new TableUnit[] {};
        sortedUnits = sortUnits(currentSortMode);
        fireTableDataChanged();
    }

    /**
     * Sorts all units in {@link #currentUnits} according to the given mode and returns the
     * resulting array (also stored back into {@link #sortedUnits} as a side effect for the
     * first three modes, since {@link Arrays#sort} mutates the array in place).
     *
     * @param sortMode one of {@link #UNIT} (alphabetical by name), {@link #WEIGHT} (ascending
     *                  tonnage), {@link #BATTLEVALUE} (ascending BV), or {@link #FREQUENCY}
     *                  (ascending table-appearance frequency, via an inline comparator that
     *                  treats a null unit as frequency 0.0)
     * @return the sorted array of units, or an empty array if {@code sortMode} matches none of
     *         the known constants (failsafe case)
     */
    public TableUnit[] sortUnits(int sortMode) {

        // a comparator
        CUnitComparator comparator;

        switch (sortMode) {
            case TableViewerModel.UNIT:
                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_NAME);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.WEIGHT:
                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_WEIGHT_TONS);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.BATTLEVALUE:
                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_BV);
                Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case TableViewerModel.FREQUENCY:
                sortedUnits = currentUnits.values().toArray(sortedUnits);
                Arrays.sort(sortedUnits,
                      (o1, o2) -> {
                          Double d1 = 0.0;
                          double d2 = 0.0;

                          if (o1 != null) {
                              d1 = o1.getFrequency();
                          }

                          if (o2 != null) {
                              d2 = o2.getFrequency();
                          }

                          return d1.compareTo(d2);
                      });
                return sortedUnits;

        }// end switch

        // failsafe return
        return new TableUnit[] {};
    }

}// end TableViewerModel class
