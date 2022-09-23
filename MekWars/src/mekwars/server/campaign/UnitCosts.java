/*
 * MekWars - Copyright (C) 2005
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)  
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

package server.campaign;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import common.Unit;
import common.util.MWLogger;
import megamek.common.Entity;
import megamek.common.MechFileParser;
import megamek.common.MechSummary;
import megamek.common.MechSummaryCache;

/**
 * @version 2016.10.26
 */
public class UnitCosts {

    Vector<Vector<Double>> minCostUnitList = new Vector<Vector<Double>>(4, 1);
    Vector<Vector<Double>> maxCostUnitList = new Vector<Vector<Double>>(4, 1);

    public UnitCosts() {
        for (int weight = Unit.LIGHT; weight <= Unit.ASSAULT; weight++) {
            Vector<Double> maxVector = new Vector<Double>(4, 1);
            Vector<Double> minVector = new Vector<Double>(4, 1);
            for (int type = Unit.MEK; type < Unit.MAXBUILD; type++) {
                maxVector.add(0.0);
                minVector.add(0.0);
            }
            maxCostUnitList.add(maxVector);
            minCostUnitList.add(minVector);
        }
    }

    public void addMaxCost(int weight, int type, double amount) {
        Vector<Double> maxCost = getMaxCost(weight);
        maxCost.removeElementAt(type);
        maxCost.add(type, amount);
        setMaxCost(weight, maxCost);
    }

    public void addMinCost(int weight, int type, double amount) {
        Vector<Double> minCost = getMinCost(weight);
        minCost.removeElementAt(type);
        minCost.add(type, amount);
        setMinCost(weight, minCost);
    }

    public Vector<Double> getMaxCost(int weight) {
        return maxCostUnitList.get(weight);
    }

    public Vector<Double> getMinCost(int weight) {
        return minCostUnitList.get(weight);
    }

    public Double getMaxCostValue(int weight, int type) {
        Vector<Double> maxCostVector = maxCostUnitList.get(weight);
        return maxCostVector.get(type);
    }

    public Double getMinCostValue(int weight, int type) {
        Vector<Double> minCostVector = minCostUnitList.get(weight);
        return minCostVector.get(type);
    }

    public void setMaxCost(int weight, Vector<Double> cost) {
        maxCostUnitList.removeElementAt(weight);
        maxCostUnitList.add(weight, cost);
    }

    public void setMinCost(int weight, Vector<Double> cost) {
        minCostUnitList.removeElementAt(weight);
        minCostUnitList.add(weight, cost);
    }

    public void loadUnitCosts() {

        String entityName = "";

        if (new File("./data/mechfiles").exists()) {
            try {
                MechSummary[] units = MechSummaryCache.getInstance().getAllMechs();
                for (MechSummary unit : units) {
                    Entity ent = new MechFileParser(unit.getSourceFile(), unit.getEntryName()).getEntity();
                    double cost = unit.getCost();
                    double maxCost = getMaxCostValue(Unit.getEntityWeight(ent), Unit.getEntityType(ent));
                    double minCost = getMinCostValue(Unit.getEntityWeight(ent), Unit.getEntityType(ent));
                    addMaxCost(Unit.getEntityWeight(ent), Unit.getEntityType(ent), Math.max(cost, maxCost));
                    addMinCost(Unit.getEntityWeight(ent), Unit.getEntityType(ent), Math.min(cost, minCost));
                }
            } catch (Exception ex) {

            }
            return;
        }
        try {
            FileInputStream in = new FileInputStream("./data/mechfiles/Meks.zip");
            ZipInputStream zipFile = new ZipInputStream(in);

            while (zipFile.available() == 1) {

                ZipEntry entry = zipFile.getNextEntry();
                entityName = entry.getName();
                if (entityName.startsWith("Error")) {
                    continue;
                }

                SUnit unit = new SUnit("null", entityName, Unit.LIGHT);
                double cost = unit.getEntity().getCost(true);

                double maxCost = getMaxCostValue(unit.getWeightclass(), unit.getType());
                if (cost > maxCost) {
                    addMaxCost(unit.getWeightclass(), unit.getType(), cost);
                    continue;
                }

                double minCost = getMinCostValue(unit.getWeightclass(), unit.getType());
                if (minCost == 0 || cost < minCost) {
                    addMinCost(unit.getWeightclass(), unit.getType(), cost);
                }
            }

            zipFile.close();
            in.close();

        } catch (FileNotFoundException fnf) {
            MWLogger.errLog("Unable to load Meks.zip for UnitCosts.loadUnitCosts");
        } catch (Exception ex) {
            MWLogger.errLog("Error with Meks.zip file " + entityName);
            MWLogger.errLog(ex);
        }

        try {
            FileInputStream in = new FileInputStream("./data/mechfiles/Vehicles.zip");
            ZipInputStream zipFile = new ZipInputStream(in);

            while (zipFile.available() == 1) {
                ZipEntry entry = zipFile.getNextEntry();
                entityName = entry.getName();
                SUnit unit = new SUnit("null", entityName, Unit.LIGHT);
                double cost = unit.getEntity().getCost(true);

                double maxCost = getMaxCostValue(unit.getWeightclass(), unit.getType());
                if (cost > maxCost) {
                    addMaxCost(unit.getWeightclass(), unit.getType(), cost);
                    continue;
                }

                double minCost = getMinCostValue(unit.getWeightclass(), unit.getType());
                if (minCost == 0 || cost < minCost) {
                    addMinCost(unit.getWeightclass(), unit.getType(), cost);
                }

            }

            zipFile.close();
            in.close();

        } catch (FileNotFoundException fnf) {
            MWLogger.errLog("Unable to load Vehicles.zip for UnitCosts.loadUnitCosts");
        } catch (Exception ex) {
            MWLogger.errLog("Error with Vehicles.zip file " + entityName);
            MWLogger.errLog(ex);
        }

        try {
            FileInputStream in = new FileInputStream("./data/mechfiles/Infantry.zip");
            ZipInputStream zipFile = new ZipInputStream(in);

            while (zipFile.available() == 1) {
                ZipEntry entry = zipFile.getNextEntry();
                entityName = entry.getName();
                SUnit unit = new SUnit("null", entityName, Unit.LIGHT);
                double cost = unit.getEntity().getCost(true);

                double maxCost = getMaxCostValue(unit.getWeightclass(), unit.getType());
                if (cost > maxCost) {
                    addMaxCost(unit.getWeightclass(), unit.getType(), cost);
                    continue;
                }
                double minCost = getMinCostValue(unit.getWeightclass(), unit.getType());
                if (minCost == 0 || cost < minCost) {
                    addMinCost(unit.getWeightclass(), unit.getType(), cost);
                }

            }

            zipFile.close();
            in.close();

        } catch (FileNotFoundException fnf) {
            MWLogger.errLog("Unable to load Infantry.zip for UnitCosts.loadUnitCosts");
        } catch (Exception ex) {
            MWLogger.errLog("Error with Infantry.zip file " + entityName);
            MWLogger.errLog(ex);
        }
    }

    public String displayUnitCostsLists() {
        StringBuilder result = new StringBuilder("<b>Max Costs</b><br>");
        for (int weight = 0; weight <= Unit.ASSAULT; weight++) {
            for (int type = 0; type < Unit.MAXBUILD; type++) {
                result.append(Unit.getWeightClassDesc(weight) + " " + Unit.getTypeClassDesc(type) + " MaxCost: " + getMaxCostValue(weight, type) + " MinCost: " + getMinCostValue(weight, type) + ".<br>");
            }
        }
        return result.toString();
    }
}