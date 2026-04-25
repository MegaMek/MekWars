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

package common.util;

public class Position {
	public double x, y;

	private Integer id;
    private String color;

	public Position(double xpos, double ypos){
		x = xpos;
		y = ypos;
	}

    /**
     *
     * @return
     */
    public String getColor() {
        return color;
    }

    /**
     *
     * @param color
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

	public double distanceSq(double xpos, double ypos){
		return Math.sqrt(Math.pow(x - xpos,2) + Math.pow(y - ypos,2));
	}

	public double distanceSq(Position p){
		return distanceSq(p.x, p.y);
	}
}