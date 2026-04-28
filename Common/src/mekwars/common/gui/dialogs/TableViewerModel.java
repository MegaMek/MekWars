package mekwars.common.gui.dialogs;

import java.io.Serial;
import java.util.TreeMap;

import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.CUnitComparator;

// inner classes
/*
 * TableViewerModel is a model extension which sets up proper table viewer
 * sorting columns - name, weight, model, % frequency, etc. Modeled along
 * the BlackMarketModel from client.gui
 */
class TableViewerModel extends javax.swing.table.AbstractTableModel {

    // IVARS
    // static ints
    public final static int UNIT = 0;// model/name
    public final static int WEIGHT = 1;
    public final static int BATTLEVALUE = 2;
    public final static int FREQUENCY = 3;
    public final static int FILENAME = 4;
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = 4544599978221999391L;
    java.util.TreeMap<Object, TableUnit> currentUnits;
    TableUnit[] sortedUnits;

    int currentSortMode = mekwars.common.gui.dialogs.TableViewerModel.FREQUENCY;

    // column name array
    //String[] columnNames = { "Unit", "Weight", "BV", "Frequency" };
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
                        return STR."<html><body>\{currU.getEntity().getChassis()}, \{currU.getModelName()}";
                    }
                    // else
                    return STR."<html><body>\{currU.getModelName()}";
                } catch (Exception ex) {
                    mekwars.common.util.MWLogger.errLog(ex);
                    return "";
                }
            case WEIGHT:
                return (int) currU.getEntity().getWeight();

            case BATTLEVALUE:

                return currU.getEntity().calculateBattleValue();

            case FREQUENCY:

                java.text.DecimalFormat myFormatter = new java.text.DecimalFormat("##0.00");
                String val = myFormatter.format(currU.getFrequency());
                //Double returnVal = Double.parseDouble(val);
                double returnVal = 0.0;
                try {
                    returnVal = java.text.NumberFormat.getNumberInstance().parse(val).doubleValue();
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }

                return returnVal;

            case FILENAME:
                return currU.getRealFilename();

        }

        return "";
    }

    // override naming
    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
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
     * getRenderer, overridden from AbstractModel in order to use custom
     * renderer.
     */
    public TableViewerRenderer getRenderer() {
        return new TableViewerRenderer(this);
    }

    /*
     * refresh model in order to draw new contents, reorder existin
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
            case mekwars.common.gui.dialogs.TableViewerModel.UNIT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_NAME);
                java.util.Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case mekwars.common.gui.dialogs.TableViewerModel.WEIGHT:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_WEIGHT_TONS);
                java.util.Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case mekwars.common.gui.dialogs.TableViewerModel.BATTLEVALUE:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                comparator = new CUnitComparator(CUnitComparator.HQ_SORT_BV);
                java.util.Arrays.sort(sortedUnits, comparator);
                return sortedUnits;

            case mekwars.common.gui.dialogs.TableViewerModel.FREQUENCY:

                sortedUnits = currentUnits.values().toArray(sortedUnits);
                java.util.Arrays.sort(sortedUnits,
                      (o1, o2) -> {

                          try {
                              Double d1 = 0.0;
                              double d2 = 0.0;

                              if (o1 != null) {
                                  d1 = o1.getFrequency();
                              }

                              if (o2 != null) {
                                  d2 = o2.getFrequency();
                              }

                              return d1.compareTo(d2);
                          } catch (Exception ex) {
                              mekwars.common.util.MWLogger.errLog(ex);
                              return 0;
                          }
                      });
                return sortedUnits;

        }// end switch

        // failsafe return
        return new TableUnit[] {};
    }

}// end TableViewerModel class
