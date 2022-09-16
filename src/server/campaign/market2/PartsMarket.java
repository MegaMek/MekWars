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

package server.campaign.market2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.StringTokenizer;

import common.BMEquipment;
import common.Equipment;
import common.util.MWLogger;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;

public class PartsMarket {
	
	// IVARS
	private HashMap<String,BMEquipment> equipmentList = new HashMap<String, BMEquipment>();
	private HashMap<String,BMEquipment> lastTickList = new HashMap<String, BMEquipment>();
	
	// CONSTRUCTOR
	/**
	 */
	public PartsMarket() {
		
		this.loadParts();
	}
	
	// METHODS
	/**
	 */
	public synchronized void tick() {
		
		int year = CampaignMain.cm.getIntegerConfig("CampaignYear");
		for ( String key : CampaignMain.cm.getBlackMarketEquipmentTable().keySet() ) {
			BMEquipment eq = this.equipmentList.get(key);
			BMEquipment tickList = this.lastTickList.get(key);
			Equipment masterEq = CampaignMain.cm.getBlackMarketEquipmentTable().get(key);
			
			
			try {
				Thread.sleep(10);
				//Remove from the list since its no longer being produced.
				if ( masterEq.getMaxProduction() == 0 ) {
					this.equipmentList.remove(key);
					this.lastTickList.remove(key);
					continue;
				}

				if ( (eq == null || tickList == null) && masterEq.getMaxProduction() > 0 ) {
					
					eq = new BMEquipment();
					
					eq.setEquipmentInternalName(key);
					eq.setAmount(Math.max(masterEq.getMinProduction(),CampaignMain.cm.getRandomNumber(masterEq.getMaxProduction())+1));
					eq.setCost(Math.max(masterEq.getMinCost(), CampaignMain.cm.getR().nextDouble()*masterEq.getMaxCost()));
					eq.setCostUp(false);
					eq.getEquipmentName();
					eq.getTech(year);
					
					this.lastTickList.put(key, eq.clone(year));
					this.equipmentList.put(key,eq);
					continue;
				}
				//Stuff got bought lets raise the price
				if ( eq.getAmount() < tickList.getAmount() ) {
					eq.setCostUp(true);
					double costIncrease = ((double)(masterEq.getMaxProduction()-eq.getAmount())/(double)masterEq.getMaxProduction())+1;
					
					eq.setCost(Math.min(masterEq.getMaxCost(),Math.max(masterEq.getMinCost(), eq.getCost()*costIncrease)));
					
					if ( eq.getAmount() < masterEq.getMaxProduction() ) {
						int difference = masterEq.getMaxProduction()-eq.getAmount();
						int amountIncrease = Math.min(1,Math.min(difference/2, CampaignMain.cm.getRandomNumber(difference+1)));
						eq.setAmount(eq.getAmount()+amountIncrease);
					}
					
				}//Ok no one bought anything so lets lower the price and add to the amount
				else {
					eq.setCostUp(false);
					if ( eq.getAmount() < masterEq.getMaxProduction() ) {
						int difference = masterEq.getMaxProduction()-eq.getAmount();
						int amountIncrease = Math.min(1, Math.min(difference/2, CampaignMain.cm.getRandomNumber(difference)+1));
						eq.setAmount(eq.getAmount()+amountIncrease);
					}
					
					//Only want the price to go down 10% max.
					double newCost = Math.max(masterEq.getMinCost(),Math.max(eq.getCost()*0.9, CampaignMain.cm.getR().nextDouble()*eq.getCost()));
					eq.setCost(newCost);
				}
			}catch ( IllegalArgumentException iae) {
				eq.setCost(Math.abs(eq.getCost()));
				eq.setAmount(Math.abs(eq.getAmount()));
			}catch (Exception ex) {

				MWLogger.errLog(ex);
			}
			eq.setAmount(Math.max(eq.getAmount(), masterEq.getMinProduction()));
			eq.getEquipmentName();
			eq.getTech(year);
			this.lastTickList.put(key, eq.clone(year));
			this.equipmentList.put(key,eq);
			
		}
		
		updatePartsBlackMarketAllPlayers();
		
		saveParts();
		
	}
	
	
	public synchronized void updatePartsBlackMarketAllPlayers() {
		
		String result = this.getPartsUpdateString();
		CampaignMain.cm.doSendToAllOnlinePlayers(result, false);
	}
	

	public String getPartsUpdateString() {
		StringBuilder result = new StringBuilder("PL|UPBM|");
		String delimiter = "#";
	
		try {
			
			for ( String key : this.equipmentList.keySet() ) {
				
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
		}catch(Exception ex) {
			MWLogger.errLog(ex);
		}
		
		return result.toString();
	}
	
	public synchronized void updatePartsBlackMarketPlayer(SPlayer player) {
		CampaignMain.cm.toUser(this.getPartsUpdateString(),player.getName(), false);
	}

	private void saveParts() {
		try{
			PrintStream ps = new PrintStream(new FileOutputStream("./data/partsblackmarket.dat"));
            
			for ( String key : this.equipmentList.keySet() ) {
				
				BMEquipment bme = this.equipmentList.get(key);

				if (CampaignMain.cm.getBlackMarketEquipmentTable().get(key) == null ||
						CampaignMain.cm.getBlackMarketEquipmentTable().get(key).getMaxProduction() == 0 )
					continue;
				
				ps.print(bme.getEquipmentInternalName());
				ps.print("#");
				ps.print(bme.getCost());
				ps.print("#");
				ps.print(bme.getAmount());
				ps.print("#");
			}
			ps.close();
		} catch (FileNotFoundException fe) {
			MWLogger.errLog("partsblackmarket.dat not found");
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}   

	}

	private void loadParts(){
		int year = CampaignMain.cm.getIntegerConfig("CampaignYear");
		BufferedReader dis = null;
		try{
	        File bmFile = new File("./data/partsblackmarket.dat");
	        
	        if ( !bmFile.exists() )
	        	return;
	        
			FileInputStream fis = new FileInputStream(bmFile);
			dis = new BufferedReader(new InputStreamReader(fis));

			if ( dis.ready() ) {
				String line = dis.readLine();
				StringTokenizer data = new StringTokenizer(line,"#");
		        
				while ( data.hasMoreElements() ) {
					
					BMEquipment bme = new BMEquipment();
					bme.setEquipmentInternalName(data.nextToken());
					bme.setCost(Double.parseDouble(data.nextToken()));
					bme.setAmount(Integer.parseInt(data.nextToken()));
					bme.getEquipmentName();
					bme.getTech(year);
					this.equipmentList.put(bme.getEquipmentInternalName(),bme);
					this.lastTickList.put(bme.getEquipmentInternalName(),bme.clone(year));
				}
			}
			
		}catch (Exception ex){
			MWLogger.errLog(ex);
		} finally {
			try {
				if (dis!=null)
				dis.close();
			} catch (IOException e) {
				MWLogger.errLog(e);
			}
		}
	}
	
	public HashMap<String, BMEquipment>getEquipmentList(){
		return this.equipmentList;
	}

}// end Market.java
