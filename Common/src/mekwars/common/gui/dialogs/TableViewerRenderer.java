package mekwars.common.gui.dialogs;

import java.io.Serial;

/*
 * TableViewerRenderer ... This needs quite a bit more polish ...
 */
class TableViewerRenderer extends javax.swing.table.DefaultTableCellRenderer {

    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -8249928299962506117L;

    private final mekwars.common.gui.dialogs.TableViewerModel tableViewerModel;

    public TableViewerRenderer(
          mekwars.common.gui.dialogs.TableViewerModel tableViewerModel) {this.tableViewerModel = tableViewerModel;}

    @Override
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
          boolean isSelected, boolean hasFocus, int row, int column) {
        java.awt.Component d = super.getTableCellRendererComponent(table,
              value,
              isSelected,
              hasFocus,
              row,
              column);

        javax.swing.JLabel c = new javax.swing.JLabel(); // use a new label for everything
        // (should be made better later)
        c.setOpaque(true);

        if ((row >= tableViewerModel.currentUnits.size()) || (row < 0)) {
            return c;
        }

        if (table.getModel().getValueAt(row, column) != null) {
            c.setText(table.getModel().getValueAt(row, column).toString());
        }
        c.setToolTipText("");

        // get the unit from the tree
        Object unit = table.getModel()
                            .getValueAt(row,
                                  TableViewerModel.FILENAME);
        mekwars.common.gui.dialogs.TableUnit currU = tableViewerModel.currentUnits.get(unit);

        if (currU == null) {
            return null;
        }

        // set up description
        StringBuilder description = new StringBuilder();

        if ((currU.getType() == mekwars.common.Unit.MEK) && !currU.getEntity().isOmni()) {
            description.append("<html><body><u>")
                  .append(currU.getEntity().getChassis())
                  .append(", ")
                  .append(currU.getModelName())
                  .append("</u><br>");
        } else {
            description.append("<html><body><u>").append(currU.getModelName()).append("</u><br>");
        }

        // show the percent frequency for each table
        description.append("Sources:");

        //DecimalFormat formatter = new DecimalFormat("##0.0##");
        for (String tableName : currU.getTables().keySet()) {
            description.append("<br>- ").append(tableName);
        }

        c.setToolTipText(description.toString());

        if (isSelected) {
            c.setForeground(d.getForeground());
            c.setBackground(d.getBackground());
            return c;
        }

        // always a white background
        c.setBackground(java.awt.Color.white);

        return c;
    }
}
