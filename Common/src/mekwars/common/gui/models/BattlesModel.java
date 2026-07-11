package mekwars.common.gui.models;

import java.awt.Component;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import mekwars.common.MMGame;
import mekwars.common.gui.panels.CBattlePanel;

public class BattlesModel extends AbstractTableModel {

    public final static int NAME = 0;
    public final static int PLAYER_COUNT = 1;
    public final static int VERSION = 2;
    public final static int COMMENT = 3;
    public final static int PLAYER_NAMES = 4;
    /**
     *
     */
    @Serial
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
    private List<MMGame> sortedGames; //not really though, sort is handled elsewhere...

    public BattlesModel(CBattlePanel cBattlePanel) {
        this.cBattlePanel = cBattlePanel;
        this.sortedGames = new ArrayList<>(cBattlePanel.getClient().getServers().values());
    }

    public List<MMGame> getSortedGames() {
        return sortedGames;
    }

    public CBattlePanel getCBattlePanel() {
        return cBattlePanel;
    }

    public void refreshModel() {
        //do a resort
        this.sortedGames = new ArrayList<>(cBattlePanel.getClient().getServers().values());
        this.fireTableDataChanged();
    }

    public int getRowCount() {
        return this.sortedGames.size();
    }

    public int getColumnCount() {
        return this.columnNames.length;
    }

    public Object getValueAt(int row, int col) {

        if (row < 0) {
            return "";
        }

        if (row >= sortedGames.size()) {
            return "";
        }

        MMGame aGame = sortedGames.get(row);

        switch (col) {
            case NAME:
                return aGame.getHostName();
            case PLAYER_COUNT:
                return String.format("%s/%s", aGame.getCurrentPlayers().size(), aGame.getMaxPlayers());
            case VERSION:
                return aGame.getVersion();
            case COMMENT:
                return aGame.getComment();
            case PLAYER_NAMES:

                StringBuilder result = new StringBuilder();
                for (String currName : aGame.getCurrentPlayers()) {
                    result.append(currName).append(", ");
                }

                String toReturn = result.toString().trim();
                if (toReturn.lastIndexOf(",") >= 0) {
                    toReturn = toReturn.substring(0, toReturn.lastIndexOf(","));
                }

                return toReturn;
        }

        return "";
    }

    @Override
    public String getColumnName(int col) {
        return (columnNames[col]);
    }

    public Renderer getRenderer() {
        return new Renderer(this);
    }

    /*
     * Renderer cannot be static because it uses parent data structs.
     */
    public static class Renderer extends DefaultTableCellRenderer {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -2353501701911884548L;

        private final BattlesModel battlesModel;

        public Renderer(BattlesModel battlesModel) {
            this.battlesModel = battlesModel;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
              int row, int column) {
            Component tableCellRendererComponent = super.getTableCellRendererComponent(table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column);

            if (battlesModel.getSortedGames().size() <= row) {
                return tableCellRendererComponent;
            }

            if (isSelected) {
                return tableCellRendererComponent;
            }

            String gameName = (String) battlesModel.getCBattlePanel().getBattleSorter().getValueAt(row, 0);//host name
            MMGame game = battlesModel.getCBattlePanel().getClient().getServers().get(gameName);

            //set background color
            if (game.getCurrentPlayers().size() >= game.getMaxPlayers()) {
                tableCellRendererComponent.setBackground(java.awt.Color.red);
            } else if (game.getStatus().equals("Open")) {
                tableCellRendererComponent.setBackground(java.awt.Color.green);
            } else if (game.getStatus().equals("Running")) {
                tableCellRendererComponent.setBackground(java.awt.Color.yellow);
            } else {
                tableCellRendererComponent.setBackground(getBackground());
            }

            return tableCellRendererComponent;
        }
    }
}
