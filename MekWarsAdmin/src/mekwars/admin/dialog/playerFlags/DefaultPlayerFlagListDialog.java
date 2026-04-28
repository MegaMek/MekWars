package mekwars.admin.dialog.playerFlags;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serial;
import java.util.Vector;
import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import mekwars.common.VerticalLayout;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.flags.PlayerFlags;

public class DefaultPlayerFlagListDialog extends JDialog implements ActionListener {

    @Serial
    private static final long serialVersionUID = -6517948686402015985L;

    private final IClient mwClient;
    private final PlayerFlags flags = new PlayerFlags();
    private final JButton addButton = new JButton("Add");
    private final Vector<String> pendingFlags = new Vector<>();
    private final Vector<String> deletedFlags = new Vector<>();
    private final JTable flagTable;
    private final JPopupMenu popup;
    private Vector<String> flagNames = new Vector<>();

    public DefaultPlayerFlagListDialog(IClient iClient) {
        super(new JFrame(), "Player Flags", true);
        mwClient = iClient;
        loadPlayerFlags(mwClient.getPlayer().getDefaultPlayerFlags().export());
        //flagTable = new JTable();
        String[] columnNames = { "Flag Name", "Set by Default" };

        PFTableModel model = new PFTableModel(columnNames);

        for (String flagName : flagNames) {
            model.addRow(new Object[] { flagName, flags.getFlagStatus(flagName) });
        }

        popup = new JPopupMenu();
        JMenuItem delItem = new JMenuItem("Delete");
        delItem.setActionCommand("Del");
        delItem.addActionListener(this);
        popup.add(delItem);

        flagTable = new JTable(model);
        flagTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        flagTable.getColumnModel().getColumn(1).setPreferredWidth(40);

        flagTable.getModel().addTableModelListener(new PFTableChangeListener(flagTable, pendingFlags));

        buildGUI();
    }

    private void loadPlayerFlags(String f) {
        flags.loadDefaults(f);
        flagNames = flags.getFlagNames();
    }

    private void buildGUI() {
        JScrollPane scrollPane = new JScrollPane(flagTable);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        addButton.setActionCommand("add");
        addButton.addActionListener(this);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new VerticalLayout());
        mainPanel.add(scrollPane);

        JPanel bottomPanel = new JPanel();
        bottomPanel.add(addButton);

        JButton saveButton = new JButton("Save");
        saveButton.setActionCommand("Save");
        saveButton.addActionListener(this);
        bottomPanel.add(saveButton);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand("Cancel");
        cancelButton.addActionListener(this);
        bottomPanel.add(cancelButton);


        mainPanel.add(bottomPanel);

        flagTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }
        });

        this.getContentPane().add(mainPanel);
        this.pack();

        //this.checkMinimumSize();
        this.setResizable(true);

        //set a default button
        //this.getRootPane().setDefaultButton(okayButton);

        //center the dialog.
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void maybeShowPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            Point p = e.getPoint();
            int row = flagTable.rowAtPoint(p);
            ListSelectionModel model = flagTable.getSelectionModel();
            model.setSelectionInterval(row, row);
            popup.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.equalsIgnoreCase("Add")) {
            String input = JOptionPane.showInputDialog("New Player Flag Name");
            // now validate the input
            if (!input.trim().isEmpty()) {
                input = input.trim().toUpperCase();
                // Replace all spaces with underscores - spaces are causing issues
                input = input.replace(' ', '_');
                if (flags.getFlagNames().contains(input) || pendingFlags.contains(input)) {
                    JOptionPane.showMessageDialog(this, "Flag already exists");
                } else {
                    // We'll add the flag to a list that needs to be sent to the
                    // server after we're done here.
                    pendingFlags.add(input);
                    DefaultTableModel tableModel = (DefaultTableModel) flagTable.getModel();
                    tableModel.addRow(new Object[] { input, false });
                }
            }
        } else if (command.equalsIgnoreCase("Del")) {
            int row = flagTable.getSelectedRow();
            String flagName = (String) flagTable.getValueAt(row, 0);
            deletedFlags.add(flagName);
            DefaultTableModel tableModel = (DefaultTableModel) flagTable.getModel();
            tableModel.removeRow(row);
        } else if (command.equalsIgnoreCase("Cancel")) {
            this.dispose();
        } else if (command.equalsIgnoreCase("Save")) {
            StringBuilder sb = new StringBuilder();
            sb.append(IClient.CAMPAIGN_PREFIX).append("c adminUpdateDefaultPlayerFlags#");
            if (!deletedFlags.isEmpty()) {
                for (String s : deletedFlags) {
                    sb.append("D#").append(s).append("#");
                }
            }

            if (!pendingFlags.isEmpty()) {
                for (String s : pendingFlags) {
                    boolean value;
                    int tableRow = searchTable(s);
                    if (tableRow >= 0) {
                        value = (Boolean) flagTable.getValueAt(searchTable(s), 1);
                        sb.append("S#").append(s).append("#").append(value).append("#");
                    }
                }
            }
            mwClient.sendChat(sb.toString());
            this.dispose();
        }
    }

    private int searchTable(String needle) {
        for (int i = 0; i < flagTable.getRowCount(); i++) {
            if (((String) flagTable.getValueAt(i, 0)).equalsIgnoreCase(needle)) {
                return i;
            }
        }
        return -1;
    }

    private class PFTableModel extends DefaultTableModel {

        /**
         *
         */
        private static final long serialVersionUID = -4242279250379540474L;

        public PFTableModel(String[] columnNames) {
            setColumnCount(columnNames.length);
            setColumnIdentifiers(columnNames);
        }

        public boolean isCellEditable(int row, int col) {
            if (col == 0) {
                return false;
            } else {
                return true;
            }
        }

        @SuppressWarnings("unchecked")
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }
    }

    private class PFTableChangeListener implements TableModelListener {
        JTable table;
        Vector<String> pendingFlags;

        PFTableChangeListener(JTable table, Vector<String> pendingFlags) {
            this.table = table;
            this.pendingFlags = pendingFlags;
        }

        public void tableChanged(TableModelEvent e) {
            int firstRow = e.getFirstRow();

            if (e.getType() == TableModelEvent.UPDATE) {
                // The rows in the range [firstRow, lastRow] changed
                // In our case, it will only be one at a time, so we
                // only need to worry about firstRow
                String flagName = (String) table.getValueAt(firstRow, 0);
                pendingFlags.add(flagName);
            }
        }

    }
}
