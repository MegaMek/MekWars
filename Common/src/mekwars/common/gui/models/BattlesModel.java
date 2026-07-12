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

/**
 * Table model backing the "servers"/"battles" list shown in {@link CBattlePanel} — the lobby
 * screen where a player picks which running {@link MMGame} (a hosted MegaMek game server the
 * MekWars client can join) to connect to. Each row represents one advertised game server; the
 * columns summarize its name, current player load, MegaMek version, host comment, and the names
 * of players currently connected.
 * <p>
 * The row ordering ({@link #sortedGames}) is a plain snapshot of the currently known servers;
 * actual sorting of the visible table is performed elsewhere (see {@link CBattlePanel}'s sorter),
 * not by this model.
 */
public class BattlesModel extends AbstractTableModel {

    /** Column index: host/server name. */
    public final static int NAME = 0;
    /** Column index: "current/max" player count string, e.g. "3/8". */
    public final static int PLAYER_COUNT = 1;
    /** Column index: MegaMek version the server is running. */
    public final static int VERSION = 2;
    /** Column index: free-text comment set by the host. */
    public final static int COMMENT = 3;
    /** Column index: comma-separated list of players currently connected to the game. */
    public final static int PLAYER_NAMES = 4;
    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -6384905445657195650L;
    /** Header labels, indexed by the column constants above. */
    final String[] columnNames = {
          "Name",
          "Players",
          "Version",
          "Comment",
          "Player Names"
    };
    /**
     * Dummy "wide" strings used only to measure preferred column widths (see column-sizing helpers
     * elsewhere in the codebase); never displayed.
     */
    final String[] longValues = {
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          "XXXXXXXXXXXXXXXXX",
          };
    /** The panel this model backs; used by the inner {@link Renderer} to reach the client and sorter. */
    private final CBattlePanel cBattlePanel;
    /** Snapshot of known games in row order. Despite the name, this list is not actually kept
     *  sorted by this class — see the comment below. */
    private List<MMGame> sortedGames; //not really though, sort is handled elsewhere...

    /**
     * Creates the model and takes an initial snapshot of the servers currently known to the client.
     *
     * @param cBattlePanel the owning battle-list panel, used to reach the client's server map
     */
    public BattlesModel(CBattlePanel cBattlePanel) {
        this.cBattlePanel = cBattlePanel;
        this.sortedGames = new ArrayList<>(cBattlePanel.getClient().getServers().values());
    }

    /** @return the current row-order snapshot of known {@link MMGame} servers. */
    public List<MMGame> getSortedGames() {
        return sortedGames;
    }

    /** @return the panel this model is backing. */
    public CBattlePanel getCBattlePanel() {
        return cBattlePanel;
    }

    /**
     * Re-reads the client's current server map into {@link #sortedGames} and notifies listeners
     * that the entire table contents changed. Called whenever the underlying server list may have
     * changed (new game advertised, game closed, player counts updated, etc.).
     */
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

    /**
     * Returns the display value for a given cell. Rows outside the current bounds of
     * {@link #sortedGames} (including negative rows) return an empty string rather than throwing,
     * which makes this safe to call during transient resize/resort races.
     *
     * @param row row index into {@link #sortedGames}
     * @param col one of the column constants ({@link #NAME}, {@link #PLAYER_COUNT},
     *            {@link #VERSION}, {@link #COMMENT}, {@link #PLAYER_NAMES})
     * @return the cell's display value, or {@code ""} if out of range or column is unrecognized
     */
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

                // Build "name1, name2, name3" by joining with ", " then trimming the trailing
                // separator; note this only strips the LAST comma found, which happens to be the
                // trailing one appended after the final name.
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

    /** @return a fresh cell renderer bound to this model, for use by the backing {@link JTable}. */
    public Renderer getRenderer() {
        return new Renderer(this);
    }

    /*
     * Renderer cannot be static because it uses parent data structs.
     */
    /**
     * Cell renderer for the battles table that color-codes each row's background by the
     * corresponding {@link MMGame}'s fullness/status: red when the server is full, green when
     * "Open", yellow when "Running", and the default background otherwise. Selected rows are left
     * with the look-and-feel's normal selection colors (no status color applied).
     * <p>
     * Despite the class-level comment claiming it "cannot be static", this nested class is in fact
     * declared {@code static} and holds an explicit reference to its owning {@link BattlesModel}
     * instead of an implicit outer-class reference.
     */
    public static class Renderer extends DefaultTableCellRenderer {

        /**
         *
         */
        @Serial
        private static final long serialVersionUID = -2353501701911884548L;

        private final BattlesModel battlesModel;

        /**
         * @param battlesModel the model whose game data and owning panel/sorter are consulted to
         *                     determine row colors
         */
        public Renderer(BattlesModel battlesModel) {
            this.battlesModel = battlesModel;
        }

        /**
         * Colors the row background based on the {@link MMGame} for this row's host name.
         * <p>
         * Note: the host name (and therefore which game is being rendered) is looked up via
         * {@code battlesModel.getCBattlePanel().getBattleSorter().getValueAt(row, 0)} — i.e. through
         * the panel's separate row sorter rather than directly from
         * {@code battlesModel.getSortedGames().get(row)}. If the sorter's row order or contents
         * ever diverges from this model's {@link #sortedGames} snapshot, the wrong game's status
         * could be used to color a given row.
         */
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
