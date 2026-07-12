/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author - helge richter (mcwizard@gmx.de)
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

/**
 * Simple mutable 2D point/grid-cell value class used for map and layout coordinates (e.g. planet/system positions
 * on the campaign starmap). Beyond raw x/y coordinates it carries an optional numeric {@link #id} and an optional
 * display {@link #color} (normalized to an HTML {@code "#rrggbb"}-style string), used by map rendering code that
 * needs to draw and identify points.
 */
public class Position {
    /** Coordinates of this position, in whatever unit the caller's coordinate space uses (e.g. map pixels). */
    public double x, y;

    /** Optional identifier associating this position with some other entity (e.g. a planet/system ID). */
    private Integer id;
    /** Display color for this position, always stored with a leading {@code "#"} (see {@link #setColor}). */
    private String color;

    /**
     * @param xpos initial x coordinate
     * @param ypos initial y coordinate
     */
    public Position(double xpos, double ypos) {
        x = xpos;
        y = ypos;
    }

    /**
     * @return the display color for this position, in {@code "#rrggbb"} form, or {@code null} if never set
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the display color, normalizing it to always start with {@code "#"}: if the given string does not
     * already start with {@code "#"}, one is prepended.
     *
     * @param color the color string (with or without a leading {@code "#"}); must not be {@code null}
     */
    public void setColor(String color) {

        if (color.startsWith("#")) {
            this.color = color;
        } else {
            this.color = "#" + color;
        }

    }

    /**
     * @return Integer
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id The id to set.
     */
    public void setId(Integer id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    /**
     * @param x The x to set.
     */
    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    /**
     * @param y The y to set.
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Convenience overload of {@link #distanceSq(double, double)} taking another {@link Position}'s coordinates.
     *
     * @param p the other position
     * @return see {@link #distanceSq(double, double)}
     */
    public double distanceSq(Position p) {
        return distanceSq(p.x, p.y);
    }

    /**
     * Computes the straight-line (Euclidean) distance between this position and the given coordinates.
     * <p><b>Note:</b> despite the {@code distanceSq} name (which conventionally implies a squared distance, i.e.
     * no {@code sqrt}), this method applies {@link Math#sqrt} and therefore returns the actual distance, not the
     * squared distance. Callers relying on the "Sq" naming to skip a sqrt should be aware this does not hold here.
     *
     * @param xpos x coordinate to measure distance to
     * @param ypos y coordinate to measure distance to
     * @return the Euclidean distance from {@code (x, y)} to {@code (xpos, ypos)}
     */
    public double distanceSq(double xpos, double ypos) {
        return Math.sqrt(Math.pow(x - xpos, 2) + Math.pow(y - ypos, 2));
    }
}
