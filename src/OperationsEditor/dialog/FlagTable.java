package OperationsEditor.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import common.flags.FlagSet;
import common.flags.PlayerFlags;
import common.flags.ResultsFlags;

public class FlagTable extends JTable implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1674365115046546502L;
	private PlayerFlags flags;
	private PlayerFlags availableFlags = new PlayerFlags();
	
	private JPopupMenu popup;
	
	private Component parent;
	
	private int flagType = 0;
	
	/**
	 * A Table to store PlayerFlag settings for the operation
	 * @param parentObject
	 */
	public FlagTable(Component parentObject, int flagType) {
		if (flagType == FlagSet.FLAGTYPE_PLAYER) {
			flags = new PlayerFlags();
		} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
			flags = new ResultsFlags();
		}
		parent = parentObject;
		availableFlags.loadFromDisk();
		this.flagType = flagType;
		
		String[] cNames = {"Bad FlagType"};
		if (flagType == FlagSet.FLAGTYPE_PLAYER) {
			cNames = new String[]{"Flag Name", "Value"};
		} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
			cNames = new String[] {"Flag Name", "Value", "Apply To Attacker", "Apply To Defender"};
		}
		
		PFTableModel model = new PFTableModel(cNames);
		this.setModel(model);
		if (flagType == FlagSet.FLAGTYPE_PLAYER) {
			model.addRow(new Object[]{" ", false});
		} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
			model.addRow(new Object[]{ " ", false, false, false});
		}
				
		this.getColumnModel().getColumn(0).setPreferredWidth(200);
		this.getColumnModel().getColumn(1).setPreferredWidth(40);
		
		if (flagType == FlagSet.FLAGTYPE_RESULTS) {
			//this.getColumnModel().getColumn(2).setPreferredWidth(40);
			//this.getColumnModel().getColumn(3).setPreferredWidth(40);
		}
		
		this.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		this.setVisible(true);
		
		this.addMouseListener( new MouseAdapter() {
			public void mousePressed( MouseEvent e) {
				maybeShowPopup(e);
			}
			
			public void mouseReleased(MouseEvent e) {
				maybeShowPopup(e);
			}
		});
		
		popup = new JPopupMenu();
		JMenuItem addItem = new JMenuItem("Add");
		addItem.setActionCommand("Add");
		addItem.addActionListener(this);
		popup.add(addItem);
		JMenuItem delItem = new JMenuItem("Delete");
		delItem.setActionCommand("Del");
		delItem.addActionListener(this);
		popup.add(delItem);
		
	}
	
	private void maybeShowPopup(MouseEvent e) {
		if (e.isPopupTrigger()) {
			Point p = e.getPoint();
			int row = this.rowAtPoint(p);
			ListSelectionModel model = this.getSelectionModel();
			model.setSelectionInterval(row, row);
			popup.show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	/**
	 * Populates the table and the Flag Settings for the operation
	 * @param flagSettings
	 */
	public void importFlagString(String flagSettings) {
		if (flagSettings == null || flagSettings.trim().length() == 0) {
			return;
		}
		StringTokenizer st = new StringTokenizer(flagSettings, "$");
		while (st.hasMoreTokens()) {
			String el = st.nextToken();
			StringTokenizer element = new StringTokenizer(el, "#");
			String flagName = element.nextToken();
			Boolean value = Boolean.parseBoolean(element.nextToken());
			boolean appliesToDefender = false;
			boolean appliesToAttacker = false;
			if (element.hasMoreTokens()) {
				int appliesTo = Integer.parseInt(element.nextToken());
				if (appliesTo >= ResultsFlags.APPLIESTO_DEFENDER) {
					appliesToDefender = true;
					appliesTo -= ResultsFlags.APPLIESTO_DEFENDER;
				}
				if (appliesTo >= ResultsFlags.APPLIESTO_ATTACKER) {
					appliesToAttacker = true;
				}
			}
			if (flagType == FlagSet.FLAGTYPE_PLAYER) {
				flags.addFlag(flagName, flags.getAvailableID(), value);
			} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
				((ResultsFlags)flags).addFlag(flagName, flags.getAvailableID(), value, appliesToAttacker, appliesToDefender);
			}
			DefaultTableModel model = (DefaultTableModel)this.getModel();
			if (flagType == FlagSet.FLAGTYPE_PLAYER) {
				model.addRow(new Object[]{flagName, value});
			} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
				model.addRow(new Object[]{flagName, value, appliesToAttacker, appliesToDefender});
			}
		}
	}
	
	/**
	 * Returns the operations Flag string
	 * @return
	 */
	public String exportFlagString() {
		StringBuilder toReturn = new StringBuilder();
		
		replaceFlagsFromTable();
		
		for(String flag : flags.getFlagNames()) {
			if (flag.equalsIgnoreCase(" ")) {
				continue;
			}
			toReturn.append(flag);
			toReturn.append("#");
			toReturn.append(Boolean.toString(flags.getFlagStatus(flag)));
			if (flagType == FlagSet.FLAGTYPE_RESULTS) {
				int appliesTo = 0;
				if (((ResultsFlags)flags).flagAppliesToDefender(flag)) {
					appliesTo += ResultsFlags.APPLIESTO_DEFENDER;
				}
				if (((ResultsFlags)flags).flagAppliesToAttacker(flag)) {
					appliesTo += ResultsFlags.APPLIESTO_ATTACKER;
				}
				toReturn.append("#");
				toReturn.append(Integer.toString(appliesTo));
				toReturn.append("#");
			}
			toReturn.append("$");
		}
		if (toReturn.toString().trim().length() == 0) {
			return "";
		}
		return toReturn.toString();
	}

	public void clear() {
		flags.empty();
		PFTableModel model = (PFTableModel)this.getModel();
		for (int i=this.getRowCount()-1; i >= 0; i--) {
			String flagName = (String)this.getValueAt(i, 0);
			if (!flagName.equalsIgnoreCase(" ")) {
				model.removeRow(i);
			}
		}
	}
	
	private void replaceFlagsFromTable() {
		if (flagType == FlagSet.FLAGTYPE_PLAYER) {
			flags = new PlayerFlags();
		
			for (int i = 0; i < this.getRowCount(); i++) {
				String flagName = (String)this.getValueAt(i, 0);
				if (!flagName.equalsIgnoreCase(" ")) {
					flags.addFlag(flagName, flags.getAvailableID(), (Boolean)getValueAt(i,1));
				}
			}
		} else if (flagType == FlagSet.FLAGTYPE_RESULTS) {
			flags = new ResultsFlags();
			for (int i = 0; i < this.getRowCount(); i++) {
				String flagName = (String)this.getValueAt(i, 0);
				if (!flagName.equalsIgnoreCase(" ")) {
					((ResultsFlags)flags).addFlag(flagName, flags.getAvailableID(), (Boolean)getValueAt(i,1), (Boolean)getValueAt(i,2), (Boolean)getValueAt(i,3));
				}
			}
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		
		if (command.equalsIgnoreCase("Del")) {
			int row = this.getSelectedRow();
			PFTableModel tableModel = (PFTableModel) getModel();
			tableModel.removeRow(row);
		} else if (command.equalsIgnoreCase("Add")) {
			DefaultTableModel tableModel = (DefaultTableModel) getModel();
			String flagName = (String) JOptionPane.showInputDialog(parent, "Add which flag", "Select a flag", JOptionPane.PLAIN_MESSAGE, null, availableFlags.getFlagNames().toArray(), "");
			if (flagName != null) {
				tableModel.addRow(new Object[]{flagName, false, false, false});
			}
		}
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
		
		public void addRow(Object[] rowData) {
			if (this.getRowCount() == 1 && this.getValueAt(0, 0).equals(" ")) {
				this.removeInitialRow();
			}
			super.addRow(rowData);
		}
		
		private void removeInitialRow() {
			super.removeRow(0);
		}
		
		public void removeRow(int row) {
			super.removeRow(row);
			if (this.getRowCount() == 0) {
				this.addRow(new Object[] {" ", false});
			}
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
			if (c == 0) {
				return String.class;
			}
			if (c >= 1) {
				return Boolean.class;
			}
			return null;
		}
	}
}
