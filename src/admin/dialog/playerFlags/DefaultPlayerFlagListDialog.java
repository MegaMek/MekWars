package admin.dialog.playerFlags;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import client.MWClient;
import common.VerticalLayout;
import common.flags.PlayerFlags;

public class DefaultPlayerFlagListDialog extends JDialog implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6517948686402015985L;

	private MWClient mwclient;
	private PlayerFlags flags = new PlayerFlags();
	private JScrollPane scrollPane;
	private JButton addButton = new JButton("Add");
	private Vector<String> pendingFlags = new Vector<String>();
	private Vector<String> flagNames = new Vector<String>();
	private Vector<String> deletedFlags = new Vector<String>();
	private JPanel mainPanel;
	private JTable flagTable;
	
	private JPopupMenu popup;
	
	private void loadPlayerFlags(String f) {
		flags.loadDefaults(f);
		flagNames = flags.getFlagNames();
	}
	
	private void buildGUI() {
		scrollPane = new JScrollPane(flagTable);
        scrollPane.setAlignmentX(LEFT_ALIGNMENT);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		addButton.setActionCommand("add");
		addButton.addActionListener(this);
		
		mainPanel = new JPanel();
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
		
		flagTable.addMouseListener( new MouseAdapter() {
			public void mousePressed( MouseEvent e) {
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
			if (input.trim().length() > 0) {
				input = input.trim().toUpperCase();
				// Replace all spaces with underscores - spaces are causing issues
				input = input.replace(' ', '_');
				if (flags.getFlagNames().contains(input) || pendingFlags.contains(input)) {
					JOptionPane.showMessageDialog(this, "Flag already exists");
				} else {
					// We'll add the flag to a list that needs to be sent to the
					// server after we're done here.
					pendingFlags.add(input);
					DefaultTableModel tableModel = (DefaultTableModel)flagTable.getModel();
					tableModel.addRow(new Object[]{input, false});					
				}
			}
		} else if (command.equalsIgnoreCase("Del")) {
			int row = flagTable.getSelectedRow();
			String flagName = (String)flagTable.getValueAt(row, 0);
			deletedFlags.add(flagName);
			DefaultTableModel tableModel = (DefaultTableModel) flagTable.getModel();
			tableModel.removeRow(row);
		} else if (command.equalsIgnoreCase("Cancel")) {
			this.dispose();
		} else if (command.equalsIgnoreCase("Save")) {
			StringBuilder sb = new StringBuilder();
			sb.append(MWClient.CAMPAIGN_PREFIX + "c adminUpdateDefaultPlayerFlags#");
			if (deletedFlags.size() > 0) {
				for (String s : deletedFlags) {
					sb.append("D#" + s + "#");
				}
			}
			
			if (pendingFlags.size() > 0) {
				for (String s : pendingFlags) {
					boolean value = false;
					int tableRow = searchTable(s);
					if (tableRow >= 0) {
						value = (Boolean) flagTable.getValueAt(searchTable(s), 1);						
						sb.append("S#" + s + "#" + Boolean.toString(value) + "#");	
					}
				}
			}
			mwclient.sendChat(sb.toString());
			this.dispose();
		}
	}
	
	private int searchTable(String needle) {
		for (int i = 0; i < flagTable.getRowCount(); i++) {
			if (((String)flagTable.getValueAt(i, 0)).equalsIgnoreCase(needle)) {
				return i;
			}
		}
		return -1;
	}
	
	public DefaultPlayerFlagListDialog(MWClient c) {
		super(new JFrame(),"Player Flags",true);
		mwclient = c;
		loadPlayerFlags(mwclient.getPlayer().getDefaultPlayerFlags().export());
		//flagTable = new JTable();
		String[] columnNames = {"Flag Name", "Set by Default"};

		PFTableModel model = new PFTableModel(columnNames);

		for(int i = 0; i < flagNames.size(); i++) {
			model.addRow(new Object[]{flagNames.get(i), flags.getFlagStatus(flagNames.get(i))});
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
				String flagName = (String)table.getValueAt(firstRow, 0);
				pendingFlags.add(flagName);
			}
		}
		
	}
}
