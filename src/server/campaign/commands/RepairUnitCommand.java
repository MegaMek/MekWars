/*
 * MekWars - Copyright (C) 2005 
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

/*
 * Created on 10.05.2005
 *  
 */
package server.campaign.commands;

import java.util.StringTokenizer;

import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.Tank;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.util.RepairTrackingThread;

/**
 * @author Torren (Jason Tighe)
 * this parses out what the User wants reparied on thier unit and sends that data
 * to the repair thread
 */
public class RepairUnitCommand implements Command {
	
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
		
        try{
        
            int unitID = Integer.parseInt(command.nextToken());
            int location = Integer.parseInt(command.nextToken());
            int slot = Integer.parseInt(command.nextToken());
            boolean armor = Boolean.parseBoolean(command.nextToken()); 
            int techType = Integer.parseInt(command.nextToken()); 
            int retries = Integer.parseInt(command.nextToken());
            int techWorkMod = Integer.parseInt(command.nextToken());
            boolean sendDialogUpdate = Boolean.parseBoolean(command.nextToken());
            
            retries = Math.max(0,retries);
            
            SPlayer player = CampaignMain.cm.getPlayer(Username);
            SUnit unit = player.getUnit(unitID);
            Entity entity = unit.getEntity();
            String repairMessage = "";
            int tabLocation = location;
            int cost = CampaignMain.cm.getRepairCost(entity,location,slot,techType,armor,techWorkMod);
            
            if ( unit.getType() == SUnit.INFANTRY ){
                CampaignMain.cm.toUser("FSM|Infantry cannot be repaired.",Username,false);
                return;
            }

            if ( CampaignMain.cm.getRTT().isBeingRepaired(unitID,location,slot,armor) ){
                CampaignMain.cm.toUser("FSM|That section is already being repaired wait for the work to finish before starting again.",Username,false);
                return;
            }
            
            if ( player.isUnitInLockedArmy(unitID) ){
                CampaignMain.cm.toUser("FSM|Sorry but that unit is currently in combat and may not be repaired.",Username,false);
                return;
            }
                
            if ( techType != UnitUtils.TECH_REWARD_POINTS && cost > player.getMoney() ){
                CampaignMain.cm.toUser("FSM|You do not have enough "+CampaignMain.cm.moneyOrFluMessage(true,false,-cost)+" to repair this location.",Username,false);
                return;
            }
            
            if ( techType == UnitUtils.TECH_REWARD_POINTS && cost > player.getReward() ){
                CampaignMain.cm.toUser("FSM|You do not have enough " + CampaignMain.cm.getConfig("RPLongName") + " to repair this location.",Username,false);
                return;
            }
            
            if ( player.getDutyStatus() == SPlayer.STATUS_ACTIVE && player.getAmountOfTimesUnitExistsInArmies(unitID) > 0 ){
                CampaignMain.cm.toUser("FSM|You may not repair that unit while it is in an active army.",Username,false);
                return;
            }

            int numberOfTechs = 1;
            
            if ( techType < UnitUtils.TECH_PILOT ){
                numberOfTechs = player.getAvailableTechs().elementAt(techType);
            }
            
            if ( techType == UnitUtils.TECH_PILOT && unit.getPilot() != null
                    && unit.getLastCombatPilot() != unit.getPilot().getPilotId() ){
                CampaignMain.cm.toUser("FSM|"+unit.getPilot().getName()+" refuses to repair a unit he does not remember damaging himself!",Username,false);
                return;
            }

            if ( numberOfTechs <= 0 ){
                CampaignMain.cm.toUser("FSM|You do not have any "+UnitUtils.techDescription(techType)+" techs to do this repair!",Username,false);
                return;
            }

            //if they are using RP to repair then it doesn't use parts from their stock pile
			if ( techType != UnitUtils.TECH_REWARD_POINTS && CampaignMain.cm.getBooleanConfig("UsePartsRepair") ) {
				String crit = UnitUtils.getCritName(entity, slot, location, armor);
				int damagedCrits = UnitUtils.getNumberOfDamagedCrits(entity,slot,location,armor);
				//MWLogger.errLog(crit+" Crits: "+player.getUnitParts().getPartsCritCount(crit)+" Needed: "+damagedCrits);
				if ( player.getPartsAmount(crit) < damagedCrits  ) {
					
					if ( player.getAutoReorder() ){
						
						String newCommand = crit+"#"+damagedCrits;
						
						CampaignMain.cm.getServerCommands().get("BUYPARTS").process(new StringTokenizer(newCommand,"#"), Username);
						if ( player.getPartsAmount(crit) >= damagedCrits ) {
							newCommand = unitID+"#"+location+"#"+slot+"#"+armor+"#"+techType+"#"+retries+"#"+techWorkMod+"#"+sendDialogUpdate;
							CampaignMain.cm.getServerCommands().get("REPAIRUNIT").process(new StringTokenizer(newCommand,"#"), Username);
							return;
						}
					}
	                String critPrettyname = UnitUtils.getCritExternalName(entity, slot, location, armor);
					CampaignMain.cm.toUser("FSM|You do not have enough "+critPrettyname+" crits to repair this.", Username,false);
					return;
				}
			}
			
            repairMessage = UnitUtils.getRepairMessage(entity,tabLocation,slot,armor);
            if ( repairMessage.length() > 0 ){
                CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
                return;
            }
            
            if ( armor ){
                boolean rear = false;
                
                //External armor
                if ( slot < UnitUtils.LOC_INTERNAL_ARMOR ){
                    
                	if ( entity instanceof Tank )
                		rear = false;
                	else {
	                    switch (location){
	                    case UnitUtils.LOC_CTR:
	                        tabLocation = UnitUtils.LOC_CT;
	                        rear = true;
	                        break;
	                    case UnitUtils.LOC_LTR:
	                        tabLocation = UnitUtils.LOC_LT;
	                        rear = true;
	                        break;
	                    case UnitUtils.LOC_RTR:
	                        tabLocation = UnitUtils.LOC_RT;
	                        rear = true;
	                        break;
	                    default:
                        	if ( slot == UnitUtils.LOC_REAR_ARMOR)
                        		rear = true;
                        	else
                        		rear = false;
	                        break;
	                    }
                	}

                    if ( rear )
                        repairMessage = "Repairs have begun on the external armor("+entity.getLocationAbbr(tabLocation)+"r) of your "+entity.getShortNameRaw()+".  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                    else
                        repairMessage = "Repairs have begun on the external armor("+entity.getLocationAbbr(tabLocation)+") of your "+entity.getShortNameRaw()+".  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                }//Internal armor
                else{
                    repairMessage = "Repairs have begun on the internal structure("+entity.getLocationAbbr(location)+") of your "+entity.getShortNameRaw()+".  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                }
                
            }else{
                CriticalSlot cs = entity.getCritical(location,slot);

                if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                    Mounted mounted = cs.getMount();
                    repairMessage ="Work has begun on the "+mounted.getName()+"("+ entity.getLocationAbbr(location)+") for your "+entity.getShortNameRaw()+".  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                }// end CS type if
                else{
                    if ( UnitUtils.isEngineCrit(cs) ){
                        repairMessage = "Work on your "+entity.getShortNameRaw()+"'s engine has begun.  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                    }
                    else{
                        if (entity instanceof Mech) 
                            repairMessage = "Work has begun on the "+((Mech)entity).getSystemName(cs.getIndex())+"("+entity.getLocationAbbr(location)+") for your "+entity.getShortName()+".  <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>";
                    }
                }//end CS type else

            }

            if ( CampaignMain.cm.getRTT().getState() == Thread.State.TERMINATED ){
                CampaignMain.cm.toUser("FSM|Sorry your repair order could not be processed, and the repair thread terminated. Staff was notified.",Username,false);
                MWLogger.errLog("NOTE: Repair Thread terminated! Use the restartrepairthread command to restart. If all else fails, reboot.");
                return;
            }
            if ( techType == UnitUtils.TECH_PILOT )
                unit.setPilotIsRepairing(true);
            //charge them for the repair now.
            if ( techType == UnitUtils.TECH_REWARD_POINTS ){
                player.addReward(-cost);
            }
            else {
                player.addMoney(-cost);
                unit.addRepairCost(cost);
                if ( CampaignMain.cm.getBooleanConfig("UsePartsRepair") ) {
    				String crit = UnitUtils.getCritName(entity, slot, location, armor);
    				int damagedCrits = UnitUtils.getNumberOfDamagedCrits(entity,slot,location,armor);
    				
    				if ( crit.indexOf("Ammo") > -1) {
    					crit = "Ammo Bin";
    					damagedCrits = 1;
    				}
    				player.updatePartsCache(crit, -damagedCrits);
                }
            }
            player.setSave();
            CampaignMain.cm.getRTT().getRepairList().add(RepairTrackingThread.Repair(player,unitID,armor,location,slot,techType,retries,techWorkMod,false));
            CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
            CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+unit.toString(true),Username,false);

            //call the repair dialog again witht he new unit info set.
            if ( sendDialogUpdate )
                CampaignMain.cm.toUser("ARD|"+unitID,Username,false);
        }catch(Exception ex){
            MWLogger.errLog("Unable to Process Repair Unit Command!");
            MWLogger.errLog(ex);
        }
        
	}//end process()

  

}//end RepairUnitCommand