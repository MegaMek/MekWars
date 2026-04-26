package mekwars.common.gui.dialogs;

import java.io.Serial;

import megamek.common.loaders.MekSummary;

/**
 * A table model for displaying work items
 */
public class MekTableModel extends javax.swing.table.AbstractTableModel {

    @Serial
    private static final long serialVersionUID = -5457068129532709857L;
    final static int COL_CHASSIS = 0;
    final static int COL_MODEL = 1;
    final static int COL_WEIGHT = 2;
    final static int COL_BV = 3;
    final static int COL_YEAR = 4;
    final static int COL_COST = 5;
    final static int COL_LEVEL = 6;
    final static int N_COL = 7;

    private MekSummary[] data = new MekSummary[0];

    public int getRowCount() {
        return data.length;
    }

    public int getColumnCount() {
        return N_COL;
    }

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

    @Override
    public Class<?> getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }

    public MekSummary getMechSummary(int i) {
        return data[i];
    }

    //fill table with values
    public void setData(MekSummary[] ms) {
        data = ms;
        fireTableDataChanged();
    }

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

}
