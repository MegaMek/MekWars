/*
 * MekWars - Copyright (C) 2007
 *
 * original author: jtighe (torren@users.sourceforge.net)
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

package mekwars.server.campaign.market;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.StringTokenizer;

import megamek.logging.MMLogger;
import mekwars.common.BMEquipment;
import mekwars.common.Equipment;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SPlayer;

public class PartsMarket {
    private final static MMLogger LOGGER = MMLogger.create(PartsMarket.class);

    // IVARS
    private final HashMap<String, BMEquipment> equipmentList = new HashMap<>();
    private final HashMap<String, BMEquipment> lastTickList = new HashMap<>();

    // CONSTRUCTOR

    /**
     *
     */
    public PartsMarket() {

        this.loadParts();
    }

    // METHODS

    private void loadParts() {
        int year = CampaignMain.campaignMain.getIntegerConfig("CampaignYear");
        BufferedReader dis = null;
        try {
            File bmFile = new File("./data/partsblackmarket.dat");

            if (!bmFile.exists()) {
                return;
            }

            FileInputStream fis = new FileInputStream(bmFile);
            dis = new BufferedReader(new InputStreamReader(fis));

            if (dis.ready()) {
                String line = dis.readLine();
                StringTokenizer data = new StringTokenizer(line, "#");

                while (data.hasMoreElements()) {
                    BMEquipment bme = new BMEquipment();
                    bme.setEquipmentInternalName(data.nextToken());
                    bme.setCost(Double.parseDouble(data.nextToken()));
                    bme.setAmount(Integer.parseInt(data.nextToken()));
                    bme.getEquipmentName();
                    bme.getTech(year);
                    this.equipmentList.put(bme.getEquipmentInternalName(), bme);
                    this.lastTickList.put(bme.getEquipmentInternalName(), bme.clone(year));
                }
            }

        } catch (Exception ex) {
            LOGGER.error(ex, "Error loading parts");
        } finally {
            try {
                if (dis != null) {
                    dis.close();
                }
            } catch (java.io.IOException e) {
                LOGGER.error(e, "Unable to close the stream.");
            }
        }
    }

    /**
     *
     */
    public synchronized void tick() {
        int year = CampaignMain.campaignMain.getIntegerConfig("CampaignYear");

        for (String key : CampaignMain.campaignMain.getBlackMarketEquipmentTable().keySet()) {
            BMEquipment bmEquipment = this.equipmentList.get(key);
            BMEquipment tickList = this.lastTickList.get(key);
            Equipment masterEq = CampaignMain.campaignMain.getBlackMarketEquipmentTable().get(key);


            if ((bmEquipment == null || tickList == null) && masterEq.getMaxProduction() > 0) {
                bmEquipment = new BMEquipment();
                bmEquipment.setEquipmentInternalName(key);
                bmEquipment.setAmount(Math.max(masterEq.getMinProduction(),
                      CampaignMain.campaignMain.getRandomNumber(masterEq.getMaxProduction()) + 1));
                bmEquipment.setCost(Math.max(masterEq.getMinCost(),
                      CampaignMain.campaignMain.getRandom().nextDouble() * masterEq.getMaxCost()));
                bmEquipment.setCostUp(false);
                bmEquipment.getEquipmentName();
                bmEquipment.getTech(year);

                this.lastTickList.put(key, bmEquipment.clone(year));
                this.equipmentList.put(key, bmEquipment);
                continue;
            }

            if (bmEquipment != null && tickList != null) {
                try {
                    Thread.sleep(10);
                    //Remove from the list since it's no longer being produced.
                    if (masterEq.getMaxProduction() == 0) {
                        this.equipmentList.remove(key);
                        this.lastTickList.remove(key);
                        continue;
                    }

                    //Stuff got bought let's raise the price
                    if (bmEquipment.getAmount() < tickList.getAmount()) {
                        bmEquipment.setCostUp(true);
                        double costIncrease = ((double) (masterEq.getMaxProduction() - bmEquipment.getAmount()) /
                                                     (double) masterEq.getMaxProduction()) + 1;

                        bmEquipment.setCost(Math.clamp(bmEquipment.getCost() * costIncrease,
                              masterEq.getMinCost(),
                              masterEq.getMaxCost()));

                        if (bmEquipment.getAmount() < masterEq.getMaxProduction()) {
                            int difference = masterEq.getMaxProduction() - bmEquipment.getAmount();
                            int amountIncrease = Math.min(1,
                                  Math.min(difference / 2,
                                        CampaignMain.campaignMain.getRandomNumber(difference + 1)));
                            bmEquipment.setAmount(bmEquipment.getAmount() + amountIncrease);
                        }

                    }//Ok, no one bought anything, so lets lower the price and add to the amount
                    else {
                        bmEquipment.setCostUp(false);
                        if (bmEquipment.getAmount() < masterEq.getMaxProduction()) {
                            int difference = masterEq.getMaxProduction() - bmEquipment.getAmount();
                            int amountIncrease = Math.min(1,
                                  Math.min(difference / 2,
                                        CampaignMain.campaignMain.getRandomNumber(difference) + 1));
                            bmEquipment.setAmount(bmEquipment.getAmount() + amountIncrease);
                        }

                        //Only want the price to go down 10% max.
                        double newCost = Math.max(masterEq.getMinCost(),
                              Math.max(bmEquipment.getCost() * 0.9,
                                    CampaignMain.campaignMain.getRandom().nextDouble() * bmEquipment.getCost()));
                        bmEquipment.setCost(newCost);
                    }
                } catch (IllegalArgumentException iae) {
                    bmEquipment.setCost(Math.abs(bmEquipment.getCost()));
                    bmEquipment.setAmount(Math.abs(bmEquipment.getAmount()));
                } catch (Exception ex) {

                    MWLogger.errLog(ex);
                }

                bmEquipment.setAmount(Math.max(bmEquipment.getAmount(), masterEq.getMinProduction()));
                bmEquipment.getEquipmentName();
                bmEquipment.getTech(year);
                this.lastTickList.put(key, bmEquipment.clone(year));
                this.equipmentList.put(key, bmEquipment);
            }
        }

        updatePartsBlackMarketAllPlayers();

        saveParts();

    }

    public synchronized void updatePartsBlackMarketAllPlayers() {
        String result = this.getPartsUpdateString();
        CampaignMain.campaignMain.doSendToAllOnlinePlayers(result, false);
    }

    private void saveParts() {
        try {
            PrintStream ps = new PrintStream(new FileOutputStream("./data/partsblackmarket.dat"));

            for (String key : this.equipmentList.keySet()) {
                BMEquipment bme = this.equipmentList.get(key);

                if (CampaignMain.campaignMain.getBlackMarketEquipmentTable().get(key) == null ||
                          CampaignMain.campaignMain.getBlackMarketEquipmentTable().get(key).getMaxProduction() ==
                                0) {
                    continue;
                }

                ps.print(bme.getEquipmentInternalName());
                ps.print("#");
                ps.print(bme.getCost());
                ps.print("#");
                ps.print(bme.getAmount());
                ps.print("#");
            }
            ps.close();
        } catch (FileNotFoundException fe) {
            LOGGER.error(fe, "partsblackmarket.dat not found");
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to save parts.");
        }
    }

    public String getPartsUpdateString() {
        StringBuilder result = new StringBuilder("PL|UPBM|");
        String delimiter = "#";

        try {

            for (String key : this.equipmentList.keySet()) {

                BMEquipment eq = this.equipmentList.get(key);

                result.append(eq.getEquipmentInternalName());
                result.append(delimiter);

                result.append(eq.getAmount());
                result.append(delimiter);

                result.append(eq.getCost());
                result.append(delimiter);

                result.append(eq.isCostUp());
                result.append(delimiter);

            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to updated String");
        }

        return result.toString();
    }

    public synchronized void updatePartsBlackMarketPlayer(SPlayer player) {
        CampaignMain.campaignMain.toUser(this.getPartsUpdateString(), player.getName(), false);
    }

    public HashMap<String, BMEquipment> getEquipmentList() {
        return this.equipmentList;
    }

}// end Market.java
