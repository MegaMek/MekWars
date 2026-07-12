package mekwars.common.gui;

import java.io.Serial;

/**
 * In a chain of data manipulators some behaviour is common. TableMap provides most of this behavour and can be
 * subclassed by filters that only need to override a handful of specific methods. TableMap implements TableModel by
 * routing all requests to its model, and TableModelListener by routing all events to its listeners. Inserting a
 * TableMap which has not been subclassed into a chain of table filters should have no effect.
 * <p>
 * In the MekWars client this is a well-known Sun/Java Tutorial pattern (the classic "TableSorter" example) used as
 * the base class for {@link TableSorter}, which is the sole subclass used in this codebase to add click-to-sort
 * behaviour to the various {@link javax.swing.JTable}s in the UI (Black Market, Battles list, table browser, etc.)
 * without altering the underlying data model.
 *
 * @author Philip Milne
 * @version 1.4 12/17/97
 */

public class TableMap extends javax.swing.table.AbstractTableModel implements javax.swing.event.TableModelListener {


    /**
     * Serialization version identifier for this {@link javax.swing.table.TableModel}.
     */
    @Serial
    private static final long serialVersionUID = 4993341398579103253L;

    /**
     * The wrapped/underlying table model that this map forwards all requests to. In MekWars this is typically
     * wrapped by {@link TableSorter}, which overrides selected methods to reorder rows without touching the
     * underlying data.
     */
    protected javax.swing.table.TableModel model;

    /**
     * @return the underlying (wrapped) table model, or {@code null} if none has been set yet.
     */
    public javax.swing.table.TableModel getModel() {
        return model;
    }

    /**
     * Sets the underlying table model and registers this {@code TableMap} as one of its listeners, so that model
     * change events (rows added/removed/updated) are forwarded through {@link #tableChanged(javax.swing.event.TableModelEvent)}.
     *
     * @param model the table model to wrap
     */
    public void setModel(javax.swing.table.TableModel model) {
        this.model = model;
        model.addTableModelListener(this);
    }

    // By default, implement TableModel by forwarding all messages
    // to the model.

    /**
     * @return the row count of the wrapped model, or {@code 0} if no model has been set.
     */
    public int getRowCount() {
        return (model == null) ? 0 : model.getRowCount();
    }

    /**
     * @return the column count of the wrapped model, or {@code 0} if no model has been set.
     */
    public int getColumnCount() {
        return (model == null) ? 0 : model.getColumnCount();
    }

    /**
     * Forwards directly to the wrapped model. Note: unlike {@link #getRowCount()}/{@link #getColumnCount()}, this
     * does not guard against a {@code null} model and will throw a {@link NullPointerException} if called before
     * {@link #setModel} has been invoked.
     *
     * @param aRow    the row whose value is to be queried
     * @param aColumn the column whose value is to be queried
     *
     * @return the value at the given cell of the wrapped model
     */
    public Object getValueAt(int aRow, int aColumn) {
        return model.getValueAt(aRow, aColumn);
    }

    /**
     * Forwards to the wrapped model's column name.
     */
    @Override
    public String getColumnName(int aColumn) {
        return model.getColumnName(aColumn);
    }

    /**
     * Forwards to the wrapped model's column class.
     */
    @Override
    public Class<?> getColumnClass(int aColumn) {
        return model.getColumnClass(aColumn);
    }

    /**
     * Forwards to the wrapped model to determine cell editability.
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return model.isCellEditable(row, column);
    }

    /**
     * Forwards the value update straight to the wrapped model (row/column indices are not translated - subclasses
     * such as {@link TableSorter} override this to map view indices to model indices).
     */
    @Override
    public void setValueAt(Object aValue, int aRow, int aColumn) {
        model.setValueAt(aValue, aRow, aColumn);
    }

    //
    // Implementation of the TableModelListener interface,
    //
    // By default forward all events to all the listeners.
    /**
     * Called when the wrapped model fires a {@link javax.swing.event.TableModelEvent}. The default implementation
     * simply re-fires the same event to this {@code TableMap}'s own listeners (via
     * {@link javax.swing.table.AbstractTableModel#fireTableChanged}), effectively passing the change straight
     * through. Subclasses (like {@link TableSorter}) override this to also update their own internal state (e.g.
     * re-sort) before/after forwarding.
     *
     * @param e the table model event received from the wrapped model
     */
    public void tableChanged(javax.swing.event.TableModelEvent e) {
        fireTableChanged(e);
    }
}
