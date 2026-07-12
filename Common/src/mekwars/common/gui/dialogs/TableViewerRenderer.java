package mekwars.common.gui.dialogs;

import java.io.Serial;

/**
 * Table cell renderer for the "build table" unit viewer (paired with {@link TableViewerModel} and
 * {@link TableUnit}). Beyond drawing the plain cell text, its main job is building an HTML tooltip for each row
 * that shows the unit's chassis/model and the list of build tables ("Sources") that unit design was pulled from,
 * by looking up the row's filename in {@code tableViewerModel.currentUnits} to find the corresponding
 * {@link TableUnit}.
 * <p>
 * As the original author's own comment notes, this class "needs quite a bit more polish" -- in particular it
 * always constructs a brand-new {@link javax.swing.JLabel} per cell render rather than reusing/configuring the
 * label {@code DefaultTableCellRenderer} itself would provide.
 */
class TableViewerRenderer extends javax.swing.table.DefaultTableCellRenderer {

    @Serial
    private static final long serialVersionUID = -8249928299962506117L;

    /** The table model backing the viewer; consulted here only for its {@code currentUnits} map and column constants. */
    private final mekwars.common.gui.dialogs.TableViewerModel tableViewerModel;

    /**
     * @param tableViewerModel the model whose {@code currentUnits} map is used to look up the {@link TableUnit}
     *                         for each rendered row
     */
    public TableViewerRenderer(
          mekwars.common.gui.dialogs.TableViewerModel tableViewerModel) {this.tableViewerModel = tableViewerModel;}

    /**
     * Renders a single table cell. Delegates to the superclass first (purely to obtain the selection-appropriate
     * foreground/background colors, since its returned component is otherwise discarded), then builds a fresh
     * {@link javax.swing.JLabel} with the cell's text and, if a matching {@link TableUnit} can be found for this
     * row, an HTML tooltip describing the unit and which build tables it appears on.
     *
     * @param table      the JTable being rendered
     * @param value      the cell's value (not used directly; the label's text is instead re-read from the
     *                   table model)
     * @param isSelected whether the row is currently selected, in which case the selection colors are applied
     *                   and the tooltip is otherwise still assigned before returning
     * @param hasFocus   whether the cell has focus (unused beyond passing through to the superclass call)
     * @param row        the row index being rendered
     * @param column     the column index being rendered
     * @return a JLabel configured for this cell, or {@code null} if the row's filename does not resolve to a
     *         known {@link TableUnit} in {@code tableViewerModel.currentUnits} (NOTE: returning {@code null}
     *         from a cell renderer is unusual and can cause a {@code NullPointerException} inside
     *         {@code JTable}'s painting code -- this looks like a latent bug rather than intentional behavior)
     */
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

        // Out-of-range row (can legitimately happen mid-refresh while the table model is being repopulated):
        // return a bare, textless label rather than trying to look anything up.
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
