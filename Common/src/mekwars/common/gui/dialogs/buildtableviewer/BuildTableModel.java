/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package mekwars.common.gui.dialogs.buildtableviewer;

import java.io.Serial;
import javax.swing.table.DefaultTableModel;

/**
 * A {@link DefaultTableModel} for displaying build table data in a {@code JTable} within the build table viewer
 * dialog. The only customization over the default behavior is that all cells are read-only (see
 * {@link #isCellEditable(int, int)}) — column/row data is otherwise populated the standard {@link DefaultTableModel}
 * way (e.g. via {@code addRow}/{@code setDataVector}) by its caller.
 *
 * @author Spork
 */
public class BuildTableModel extends DefaultTableModel {

    @Serial
    private static final long serialVersionUID = -203607769972828592L;

    /**
     * Constructor
     */
    public BuildTableModel() {
        super();
    }

    /**
     * Disable editing the cells of the table
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
}
