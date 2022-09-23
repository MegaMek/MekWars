/*
 * MekWars - Copyright (C) 2007 
 * 
 * Original author - Torren (torren@users.sourceforge.net)
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

package common;

/**
 *	Unit Equipment Container
 */
public class Equipment {

	private String equipmentName = "";
	private String equipmentInternalName = "";
	private double minCost = 0;
	private double maxCost = 0;
	private int minProduction = 0;
	private int maxProduction = 0;
	private boolean updated = false;
	
	public void setEquipmentName(String name) {
		this.equipmentName = name;
	}
	
	public String getEquipmentName() {
		return this.equipmentName;
	}
	
	public void setEquipmentInternalName(String name) {
		this.equipmentInternalName = name;
	}
	
	public String getEquipmentInternalName() {
		return this.equipmentInternalName;
	}
	
	public void setMinCost(double cost) {
		
		if ( this.minCost != cost) {
			this.minCost = cost;
			this.updated = true;
		}
	}
	
	public double getMinCost() {
		return this.minCost;
	}
	
	public void setMaxCost(double cost) {
		
		if ( this.maxCost != cost ) {
			this.maxCost = cost;
			this.updated = true;
		}
	}
	
	public double getMaxCost() {
		return this.maxCost;
	}
	
	public void setMinProduction(int production) {
		
		if ( this.minProduction != production ) {
			this.minProduction = production;
			this.updated = true;
		}
	}
	
	public int getMinProduction() {
		return this.minProduction;
	}
	
	public void setMaxProduction(int production) {
		
		if ( this.maxProduction != production ) {
			this.maxProduction = production;
			this.updated = true;
		}
	}
	
	public int getMaxProduction() {
		return this.maxProduction;
	}
	
	public boolean isUpdated() {
		return this.updated;
	}
	
	public void setUpdated(boolean update) {
		this.updated = update;
	}
}
