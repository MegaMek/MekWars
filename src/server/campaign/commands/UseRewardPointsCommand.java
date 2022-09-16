/*
 * MekWars - Copyright (C) 2004
 *
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original Author - Jason Tighe (Torren)
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

package server.campaign.commands;

import java.util.StringTokenizer;
import java.util.Vector;

import common.Unit;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import server.campaign.BuildTable;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.SUnitFactory;
import server.campaign.pilot.SPilot;

/**
 *
 * @author Torren Aug 28, 2004
 * allows users to redeem award points.
 * they can redeem for techs, influence, or units
 * syntax for techs and influence: /c userewardpoints#typeofreward#amountofrewardpointstouse
 * syntax for units /c userewardpoints#typeofreward#unittype#unitweight#[faction]/[rare]
 * items in brackets are optional. purchasing a rare unit will cost more rewardpoints
 *
 */
public class UseRewardPointsCommand implements Command {

	int accessLevel = 0;
	String syntax = "";
	public int getExecutionLevel(){return accessLevel;}
	public void setExecutionLevel(int i) {accessLevel = i;}
	public String getSyntax() { return syntax;}

	public void process(StringTokenizer command,String Username) {

		if (accessLevel != 0) {
			int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
			if(userLevel < getExecutionLevel()) {
				CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".",Username,true);
				return;
			}
		}

		/*
		 * rewardSelections:
		 * 0 Techs
		 * 1 Influence
		 * 2 Units
		 * 3 Repair
		 * 4 Cbills
		 */

		int rewardSelection = Integer.parseInt(command.nextToken());
		int rewardPoints = 0;
        String techs = "";
        String rewards = "";

		SPlayer player = CampaignMain.cm.getPlayer(Username);
		SHouse house = player.getMyHouse();
		// Salient - added additional case for RP for CBills
		if (rewardSelection < 0 || rewardSelection > 4 ){
			CampaignMain.cm.toUser("AM:Invalid reward selection. 0 for techs, 1 for influence, 2 for units, 3 for repair, 4 for CBills.",Username,true);
			return;
		}
		switch(rewardSelection){
		case 0:  //buying techs.
			rewardPoints = Integer.parseInt(command.nextToken());

			if (rewardPoints < 0) {
				CampaignMain.cm.toUser("AM:Invalid input - negative " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if ( !(Boolean.parseBoolean(house.getConfig("AllowTechsForRewards"))) ){
				CampaignMain.cm.toUser("AM:Sorry but you are not allowed to buy techs with " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if ( rewardPoints > player.getReward() )
			{
				if ( player.getReward() == 1)
					CampaignMain.cm.toUser("AM:You only have 1 " + CampaignMain.cm.getConfig("RPShortName") + ". Try again later.",Username,true);
				else
					CampaignMain.cm.toUser("AM:You only have " + player.getReward() + " " + CampaignMain.cm.getConfig("RPShortName") + " . Try again later.",Username,true);
				return;
			}
            if ( CampaignMain.cm.isUsingAdvanceRepair() ){
                int typeOfTechToBuy = rewardPoints;
                int techCost = Integer.parseInt(house.getConfig("RewardPointsFor"+UnitUtils.techDescription(typeOfTechToBuy)));

                if ( player.getReward() < techCost ){
                    CampaignMain.cm.toUser("AM:You do not have enough " + CampaignMain.cm.getConfig("RPLongName") + " to buy this tech. You need "+techCost,Username,true);
                    return;
                }

                player.addReward(-techCost);
                player.addTotalTechs(typeOfTechToBuy,1);
                player.addAvailableTechs(typeOfTechToBuy,1);
                if (techCost > 1)
                    rewards = "s";

                CampaignMain.cm.toUser("AM:You hired "+ StringUtils.aOrAn(UnitUtils.techDescription(typeOfTechToBuy),true)+ " tech for "+techCost+"RP"+rewards+".",Username,true);

            } else {
    			int numOfTechBought = (Integer.parseInt(house.getConfig("TechsForARewardPoint")));
    			numOfTechBought *= rewardPoints;
    			if ( numOfTechBought > 1 )
    				techs ="s";
    			if (rewardPoints > 1)
    				rewards = "s";
    			CampaignMain.cm.toUser("AM:You hired " + numOfTechBought + " tech" + techs + " for " + rewardPoints + " " + CampaignMain.cm.getConfig("RPLongName") + rewards +".",Username,true);
    			player.addReward(-rewardPoints);
    			player.addTechnicians(numOfTechBought);
            }
			break;

		case 1: //buying influence
			rewardPoints = Integer.parseInt(command.nextToken());

			if (rewardPoints < 0) {
				CampaignMain.cm.toUser("AM:Invalid input - negative " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if ( !(Boolean.parseBoolean(house.getConfig("AllowInfluenceForRewards")))){
				CampaignMain.cm.toUser("Sorry but you are not allowed to buy influence with " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if (rewardPoints > player.getReward()) {

				if (player.getReward() == 0)
					CampaignMain.cm.toUser("AM:You don't have any " + CampaignMain.cm.getConfig("RPLongName") + ". Purchase fails.",Username,true);
				else {
					String toSend = "AM:You only have " + player.getReward() + CampaignMain.cm.getConfig("RPLongName") + StringUtils.addAnS(player.getReward()) + ". Try again.";
					CampaignMain.cm.toUser(toSend,Username,true);
				}

				return;
			}

			int amountOfInfluenceBought = (Integer.parseInt(house.getConfig("InfluenceForARewardPoint")));
			amountOfInfluenceBought *= rewardPoints;
			CampaignMain.cm.toUser("AM:You've bought " + CampaignMain.cm.moneyOrFluMessage(false,true,amountOfInfluenceBought)+" for " + rewardPoints + " "+ CampaignMain.cm.getConfig("RPLongName") + StringUtils.addAnS(rewardPoints) + ".",Username,true);

			player.addReward(-rewardPoints);
			player.addInfluence(amountOfInfluenceBought);
			break;

		case 2: //buying units
			if ( !(Boolean.parseBoolean(house.getConfig("AllowUnitsForRewards")))){
				CampaignMain.cm.toUser("AM:Sorry but you are not allowed to buy units with " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}
			int rewardPointsAvailable = player.getReward();
			int unitTotalRewardPointCost = 0;
			String typestring = command.nextToken();
			String weightstring = command.nextToken();
			int unitType = Unit.MEK;
			int unitWeight = Unit.LIGHT;
			SHouse faction = player.getHouseFightingFor();
			double rareCost = 1;
			boolean buyRareUnit = false;
			Vector<SUnit> newUnits = new Vector<SUnit>(1,1);
			SPilot newPilot = null;
			String factionstring = "common";

			if ( house.getBooleanConfig("AllowRareUnitsForRewards") )
				rareCost = (house.getDoubleConfig("RewardPointMultiplierForRare"));

			try {
				unitType = Integer.parseInt(typestring);
			} catch (Exception ex) {
				unitType = Unit.getTypeIDForName(typestring);
			}

			try {
				unitWeight = Integer.parseInt(weightstring);
			} catch (Exception ex) {
				unitWeight = Unit.getWeightIDForName(weightstring.toUpperCase());
			}

			if ( command.hasMoreElements()) {
				factionstring = command.nextToken();
				if ( factionstring.equalsIgnoreCase("rare") ) {

					if ( !(Boolean.parseBoolean(house.getConfig("AllowRareUnitsForRewards"))) ) {
						CampaignMain.cm.toUser("AM:Sorry. You are not allowed to buy rare units with your " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
						return;
					}

					//else
					buyRareUnit = true;
					factionstring = house.getConfig("RewardsRareBuildTable");

				}
				else if ( !factionstring.equalsIgnoreCase("common") )
					faction = CampaignMain.cm.getHouseFromPartialString(factionstring,Username);

				if ( faction == null ) {
					faction = player.getHouseFightingFor();
					if ( faction == null )
						factionstring = "Common";
				}
			}

			String configName = "";
			if (unitType == Unit.MEK) {
				configName = Unit.getWeightClassDesc(unitWeight) + "RP";
			} else {
				configName = Unit.getWeightClassDesc(unitWeight) + Unit.getTypeClassDesc(unitType) + "RP";
			}
			unitTotalRewardPointCost = Integer.parseInt(house.getConfig(configName));
			//unitTotalRewardPointCost = weightCost + typeCost;

			if ( !player.getHouseFightingFor().equals(faction) ) {
				double nonHouseUnitMod = Double.parseDouble(house.getConfig(player.getHouseFightingFor().getName()+"To"+faction.getName()+"RewardPointMultiplier"));
				if ( nonHouseUnitMod < 0)
					nonHouseUnitMod = Double.parseDouble(house.getConfig("RewardPointNonHouseMultiplier"));
				if ( nonHouseUnitMod > 0 )
					unitTotalRewardPointCost *= nonHouseUnitMod;
			}

			if (buyRareUnit)
				unitTotalRewardPointCost *= rareCost;

			if ( unitTotalRewardPointCost > rewardPointsAvailable ){
				CampaignMain.cm.toUser("AM:Sorry. You need more " + CampaignMain.cm.getConfig("RPLongName") + " to buy that kind of unit.",Username,true);
				return;
			}

			try {
				//Lets get us a pilot and a unit
				if ( Boolean.parseBoolean(house.getConfig("AllowPersonalPilotQueues")) && ( unitType == Unit.MEK || unitType == Unit.PROTOMEK) )
					newPilot = new SPilot("Vacant",99,99);
				else
					newPilot = player.getMyHouse().getNewPilot(unitType);

				newUnits.addAll(getUnitProduced(unitType,unitWeight,newPilot,factionstring,player.getMyHouse()));

				for ( SUnit newUnit : newUnits ){
					player.addUnit(newUnit, true);
					CampaignMain.cm.toUser("AM:You've bought a " + newUnit.getModelName() + " for " +unitTotalRewardPointCost + " " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				}
				player.addReward(-unitTotalRewardPointCost);
			} catch (Exception ex){
				CampaignMain.cm.toUser("AM:An error has occured while trying to create your requested unit. Please contact an admin. Faction: "+factionstring +" Type: "+unitType+" Class: "+unitWeight,Username,true);
				MWLogger.errLog(ex);
				MWLogger.errLog("Error creating unit in "+this.getClass().getName());
			}
            break;

        case 3://repairs
            rewardPoints = Integer.parseInt(house.getConfig("RewardPointsForRepair"));

            if ( rewardPoints > player.getReward() ){
                CampaignMain.cm.toUser("AM:You need more " + CampaignMain.cm.getConfig("RPLongName") + " to repair this unit (requires " + rewardPoints + " RP)", Username, true);
                return;
            }

            int unitID = Integer.parseInt(command.nextToken());
            SUnit unit = player.getUnit(unitID);

            //break out if the player doesn't have a unit with that id
            if (unit == null) {
                CampaignMain.cm.toUser("AM:You don't have a unit with ID# " + unitID + ".", Username, true);
                return;
            }

            Entity entity = unit.getEntity();

            if ( entity.getInternal(Mech.LOC_CT) < 1 ){
                CampaignMain.cm.toUser("AM:Sorry but cored units cannot be repaired with " + CampaignMain.cm.getConfig("RPLongName") + "!", Username);
                return;
            }

            for (int x = 0; x < entity.locations(); x++) {
                entity.setArmor(entity.getOArmor(x),x);
                if ( entity.hasRearArmor(x) )
                    entity.setArmor(entity.getOArmor(x,true),x,true);
                entity.setInternal(entity.getOInternal(x),x);
                for (int y = 0; y < entity.getNumberOfCriticals(x); y++) {
                    CriticalSlot cs = entity.getCritical(x,y);

                    if ( cs == null )
                        continue;

                    if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                        Mounted mounted = cs.getMount();
                        UnitUtils.repairEquipment(mounted,entity,x);
                    }// end CS type if
                    else{
                        if ( UnitUtils.isEngineCrit(cs) ){
                            UnitUtils.repairDamagedEngine(entity);
                        }
                        else{
                            if (entity instanceof Mech) {
                                //Fix both breached and damaged crits.
                                UnitUtils.fixCriticalSlot(cs,entity,true);
                                UnitUtils.fixCriticalSlot(cs,entity,false);
                            }
                            entity.setCritical(x,y,cs);
                        }
                    }//end CS type else

                }
            }

            //Fill up ammo.
            for (Mounted weap : entity.getAmmo()) {
            	if (weap.byShot()) {
            		weap.setShotsLeft(weap.getOriginalShots());
            	}else {
            		weap.setShotsLeft(((AmmoType)weap.getType()).getShots());
            	}
            }

            CampaignMain.cm.toUser("AM:Unit #" + unitID + " "+unit.getModelName()+" is now fully repaired.", Username, true);
            CampaignMain.cm.toUser("PL|UU|"+unit.getId()+"|"+unit.toString(true),Username,false);
            player.addReward(-rewardPoints);
            player.checkAndUpdateArmies(unit);
            player.setSave();
            break;

		// @Author Salient (mwosux@gmail.com) , Add RP for CBills
		case 4: //buying CBills
			rewardPoints = Integer.parseInt(command.nextToken());

			if (rewardPoints < 0) {
				CampaignMain.cm.toUser("AM:Invalid input - negative " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if ( !(Boolean.parseBoolean(house.getConfig("AllowCBillsForRewards")))){
				CampaignMain.cm.toUser("Sorry but you are not allowed to buy CBills with " + CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);
				return;
			}

			if (rewardPoints > player.getReward()) {

				if (player.getReward() == 0)
					CampaignMain.cm.toUser("AM:You don't have any " + CampaignMain.cm.getConfig("RPLongName") + ". Purchase fails.",Username,true);
				else {
					String toSend = "AM:You only have " + player.getReward() + CampaignMain.cm.getConfig("RPLongName") + StringUtils.addAnS(player.getReward()) + ". Try again.";
					CampaignMain.cm.toUser(toSend,Username,true);
				}

				return;
			}

			int amountOfCBillsBought = (Integer.parseInt(house.getConfig("CBillsForARewardPoint")));
			amountOfCBillsBought *= rewardPoints;
			CampaignMain.cm.toUser("AM:You've bought " + CampaignMain.cm.moneyOrFluMessage(true,false,amountOfCBillsBought)+" for " + rewardPoints + " "+ CampaignMain.cm.getConfig("RPLongName") + ".",Username,true);

			player.addReward(-rewardPoints);
			player.addMoney(amountOfCBillsBought);
			break;
		}

	}

	/**
	 * Build a unit. Derived from SUnitFactory.java's getUnitProduced()
	 *
	 * @return the Mek Produced
	 */
	private Vector<SUnit> getUnitProduced(int type_id, int weightClass, SPilot pilot, String faction, SHouse house) {

		SUnitFactory factory = new SUnitFactory();
		String unitSize = Unit.getWeightClassDesc(weightClass);
		factory.setFounder(faction);
		Vector<SUnit> units = new Vector<SUnit>(1,1);
		String Filename = "";

		//Use special RP-build fluff text for the unit
		String producer = "Reward Unit";

		if (Boolean.parseBoolean(house.getConfig("UseOnlyOneVehicleSize")) && type_id == Unit.VEHICLE)
			unitSize = Unit.getWeightClassDesc(CampaignMain.cm.getRandomNumber(4));

		Filename = BuildTable.getUnitFilename(faction,unitSize,type_id,BuildTable.REWARD);//build from rewards dir.

		if ( Filename.toLowerCase().endsWith(".mul") ){
			units.addAll(SUnit.createMULUnits(Filename,producer));
		}else{
			SUnit cm = new SUnit(producer,Filename,weightClass);
			cm.setPilot(pilot);
			units.add(cm);
		}
		factory = null;  // clear this out of memory
		return units;
	}
}
