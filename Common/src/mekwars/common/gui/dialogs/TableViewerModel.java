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
/*
 * TableViewerModel is a model extension which sets up proper table viewer
 * sorting columns - name, weight, model, % frequency, etc. Modeled along
 * the BlackMarketModel from client.gui
 */
class TableViewerModel extends AbstractTableModel {
    // IVARS
    // static ints
    public final static int UNIT = 0;// model/name
    public final static int WEIGHT = 1;
    public final static int BATTLEVALUE = 2;
    public final static int FREQUENCY = 3;
    public final static int FILENAME = 4;
    private final static MMLogger LOGGER = MMLogger.create(TableViewerModel.class);
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4544599978221999391L;
    java.util.TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits;

    int currentSortMode = TableViewerModel.FREQUENCY;

    // column name array
    String[] columnNames = { "Unit", "Weight", "BV" };
    // client reference
    IClient client;

    // CONSTRUCTOR
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

    // getValueAt, for AbstractModel
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

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return getValueAt(0, columnIndex).getClass();
    }

    // isEditable, overridden from AbstractModel
    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public void setSortMode(int sortMode) {
        currentSortMode = sortMode;
    }

    /*
     * getRenderer, overridden from AbstractModel to use custom
     * renderer.
     */
    public TableViewerRenderer getRenderer() {
        return new TableViewerRenderer(this);
    }

    /*
     * refresh model to draw new contents, reorder existing
     * contents.
     */
    public void refreshModel() {
        sortedUnits = new TableUnit[] {};
        sortedUnits = sortUnits(currentSortMode);
        fireTableDataChanged();
    }

    /*
     * Method which sorts the units in currentUnits.
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
