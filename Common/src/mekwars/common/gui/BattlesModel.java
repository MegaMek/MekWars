package mekwars.common.gui;

import mekwars.common.MMGame;

public class BattlesModel extends javax.swing.table.AbstractTableModel {

    public final static int NAME = 0;
    public final static int PLAYERCOUNT = 1;
    public final static int VERSION = 2;
    public final static int COMMENT = 3;
    public final static int PLAYERNAMES = 4;
    /**
     *
     */
    private static final long serialVersionUID = -6384905445657195650L;
    final String[] columnNames = {
          "Name",
          "Players",
          "Version",
          "Comment",
          "Player Names"
    };
    final String[] longValues = {
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          };
    private final CBattlePanel cBattlePanel;
    public Object[] sortedGames; //not really though, sort is handled elsewhere...

    public BattlesModel(CBattlePanel cBattlePanel) {
        this.cBattlePanel = cBattlePanel;
        this.sortedGames = cBattlePanel.mwclient.getServers().values().toArray();
    }

    public void refreshModel() {
        //do a resort
        this.sortedGames = cBattlePanel.mwclient.getServers().values().toArray();
        this.fireTableDataChanged();
    }

    public int getRowCount() {
        return this.sortedGames.length;
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public Object getValueAt(int row, int col) {

        if (row < 0) {return "";}

        if (row >= sortedGames.length) {return "";}

        MMGame aGame = (MMGame) this.sortedGames[row];

        switch (col) {
            case NAME:
                return aGame.getHostName();
            case PLAYERCOUNT:
                return aGame.getCurrentPlayers().size() + "/" + aGame.getMaxPlayers();
            case VERSION:
                return aGame.getVersion();
            case COMMENT:
                return aGame.getComment();
            case PLAYERNAMES:

                StringBuffer result = new StringBuffer();
                for (String currName : aGame.getCurrentPlayers()) {result.append(currName + ", ");}

                String toReturn = result.toString().trim();
                if (toReturn.lastIndexOf(",") >= 0) {toReturn = toReturn.substring(0, toReturn.lastIndexOf(","));}

                return toReturn;
        }

        return "";
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    public Renderer getRenderer() {
        return new Renderer();
    }

    /*
     * Renderer cannot be static because it uses parent data structs.
     */

    class Renderer extends javax.swing.table.DefaultTableCellRenderer {

        /**
         *
         */
        private static final long serialVersionUID = -2353501701911884548L;

        @Override
        public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value,
              boolean isSelected, boolean hasFocus, int row, int column) {
            java.awt.Component c = super.getTableCellRendererComponent(table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column);

            if (sortedGames.length <= row) {return c;}
            if (isSelected) {return c;}

            String gameName = (String) cBattlePanel.battleSorter.getValueAt(row, 0);//host name
            MMGame game = cBattlePanel.mwclient.getServers().get(gameName);

            //set background color
            if (game.getCurrentPlayers().size() >= game.getMaxPlayers()) {
                c.setBackground(java.awt.Color.red);
            } else if (game.getStatus().equals("Open")) {
                c.setBackground(java.awt.Color.green);
            } else if (game.getStatus().equals("Running")) {c.setBackground(java.awt.Color.yellow);} else {
                c.setBackground(getBackground());
            }

            return c;
        }
    }
}
