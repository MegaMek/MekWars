/*
 * MekWars - Copyright (C) 2004, 2005
 *
 * @original author - nmorris (urgru@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */


package mekwars.common.util;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Spring;
import javax.swing.SpringLayout;

/**
 * Swing UI helper which arranges the existing children of a {@link JPanel} using {@link SpringLayout} into a
 * uniform grid: it loops through the panel's components in row-major order and wires up {@link Spring} constraints
 * so that every cell in a given row shares that row's tallest component's height, every cell in a given column
 * shares that column's widest component's width, and a fixed 4-pixel gap is kept between adjacent cells and around
 * the panel border. The panel passed in must already use a {@link SpringLayout} and already contain its child
 * components (added in the desired left-to-right, top-to-bottom order) before calling {@code setupSpringGrid}.
 *
 * @urgru
 */
public class SpringLayoutHelper {

    /**
     * Unused static holder field; not referenced anywhere in this class. Presumably intended at some point as a
     * shared/singleton instance, but this class is used purely through its static methods.
     */
    public static SpringLayoutHelper slh;

    /**
     * Arranges {@code panel}'s components into a grid with the given number of columns, computing the number of
     * rows automatically as {@code ceil(componentCount / columns)}. See {@link #setupSpringGrid(JPanel, int, int)}.
     *
     * @param panel   the panel (already using {@link SpringLayout}) whose children should be arranged
     * @param columns number of columns in the grid
     */
    public static void setupSpringGrid(JPanel panel, int columns) {
        int count = panel.getComponentCount();

        int rows = (int) Math.ceil((double) count / (double) columns);

        setupSpringGrid(panel, rows, columns);
    }

    /**
     * Arranges {@code panel}'s existing components into a {@code rows} x {@code columns} grid using
     * {@link SpringLayout} constraints, with a fixed 4-pixel gap between cells and around the panel's edges.
     * <p>
     * If the panel currently has fewer than {@code rows * columns} components, blank {@link JLabel} placeholders
     * (a single space) are appended until the count matches, so that every grid cell maps to a real component.
     * Components are assumed to already be present in row-major order (row 0 first, then row 1, etc.).
     * <p>
     * The algorithm makes two passes: first it computes each row's height (the max preferred height of any
     * component in that row) and assigns a common Y position and height to every component in the row; then it
     * does the same per column for X position and width. Finally, the panel's own {@code SOUTH} and {@code EAST}
     * constraints are set to the accumulated total height/width so the panel sizes itself to fit the grid.
     *
     * @param panel   the panel (already using {@link SpringLayout}) whose children should be arranged
     * @param rows    number of rows in the grid
     * @param columns number of columns in the grid
     */
    public static void setupSpringGrid(JPanel panel, int rows, int columns) {

        //setup new layout.
        SpringLayout layout = (SpringLayout) panel.getLayout();

        //add padding so that the count matches
        if (panel.getComponentCount() < (rows * columns)) {
            for (int x = panel.getComponentCount(); x < (rows * columns); x++) {panel.add(new JLabel(" "));}
        }

        //make all cells in each row same height.
        Spring y = Spring.constant(4);
        for (int r = 0; r < rows; r++) {
            Spring height = Spring.constant(0);
            for (int c = 0; c < columns; c++) {
                height = Spring.max(height, layout.getConstraints(panel.getComponent(r * columns + c)).getHeight());
            }
            for (int c = 0; c < columns; c++) {
                SpringLayout.Constraints constraints = layout.getConstraints(panel.getComponent(r * columns + c));
                constraints.setY(y);
                constraints.setHeight(height);
            }
            y = Spring.sum(y, Spring.sum(height, Spring.constant(4)));
        }

        //make all cells in each column the same width.
        Spring x = Spring.constant(4);
        for (int c = 0; c < columns; c++) {
            Spring width = Spring.constant(0);
            for (int r = 0; r < rows; r++) {
                width = Spring.max(width, layout.getConstraints(panel.getComponent(r * columns + c)).getWidth());
            }
            for (int r = 0; r < rows; r++) {
                SpringLayout.Constraints constraints = layout.getConstraints(panel.getComponent(r * columns + c));
                constraints.setX(x);
                constraints.setWidth(width);
            }
            x = Spring.sum(x, Spring.sum(width, Spring.constant(4)));
        }

        //Set the parent's size.
        SpringLayout.Constraints panelConstraints = layout.getConstraints(panel);
        panelConstraints.setConstraint(SpringLayout.SOUTH, y);
        panelConstraints.setConstraint(SpringLayout.EAST, x);

    }//end setupGrid
}//end SpringLayoutHelper
