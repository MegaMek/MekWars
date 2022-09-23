/*
 * MekWars - Copyright (C) 2006 
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
 * Created on 6.16.2006
 *  
 */
package server.campaign.commands;

import java.util.StringTokenizer;
import java.util.Vector;

import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.WeaponType;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.util.RepairTrackingThread;

/**
 * @author Torren (Jason Tighe)
 * this parses out what the User wants reparied on thier unit and sends that data
 * to the repair thread
 */
public class SimpleRepairCommand implements Command {
	
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
		
        Vector<Integer> techs = new Vector<Integer>(7,1);
        Vector<Integer> rolls = new Vector<Integer>(7,1);
        
        try{
        
            int unitID = Integer.parseInt(command.nextToken());
            //Filler. since only arrays 1-6 are used
            techs.add(-999);
            rolls.add(-999);
            for ( int type = 0; type < 6; type++ ){
                techs.add(Integer.parseInt(command.nextToken()));
                rolls.add(Integer.parseInt(command.nextToken()));
            }
            
            SPlayer player = CampaignMain.cm.getPlayer(Username);
            SUnit unit = player.getUnit(unitID);
            Pilot pilot = unit.getPilot();
            int ATLevel = 0;
            
            if ( pilot.getSkills().has(PilotSkill.AstechSkillID) )
                ATLevel = pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel();
            
            int cost = CampaignMain.cm.getTotalRepairCosts(unit.getEntity(),techs,rolls,ATLevel,player.getMyHouse());
            int time = 0; 
            
            if ( player.isUnitInLockedArmy(unitID) ){
                CampaignMain.cm.toUser("FSM|Sorry but that unit is currently in combat and may not be repaired.",Username,false);
                return;
            }
                
            if ( cost > player.getMoney() ){
                CampaignMain.cm.toUser("FSM|You do not have enough "+CampaignMain.cm.moneyOrFluMessage(true,false,-cost)+" to repair this location.",Username,false);
                return;
            }
            
            if ( player.getDutyStatus() == SPlayer.STATUS_ACTIVE && player.getAmountOfTimesUnitExistsInArmies(unitID) > 0 ){
                CampaignMain.cm.toUser("FSM|You man not repair that unit while it is in an active army.",Username,false);
                return;
            }

            
            for ( int type = UnitUtils.ARMOR; type <= UnitUtils.ENGINES; type++ ){
                
                int numberOfTechs = 1;
                int techType = techs.elementAt(type);
                if ( techType < UnitUtils.TECH_PILOT ){
                    numberOfTechs = player.getAvailableTechs().elementAt(techType);
                }
                
                if ( techType == UnitUtils.TECH_PILOT && unit.getPilot() != null
                        && unit.getLastCombatPilot() != unit.getPilot().getPilotId() ){
                    CampaignMain.cm.toUser("FSM|"+unit.getPilot().getName()+" refuses to repair a unit he does not remember damaging himself!",Username,false);
                    return;
                }

                if ( techType == UnitUtils.TECH_PILOT )
                    unit.setPilotIsRepairing(true);
    
                if ( numberOfTechs <= 0 ){
                    CampaignMain.cm.toUser("FSM|You do not have any "+UnitUtils.techDescription(techType)+" techs to do this repair!",Username,false);
                    return;
                }
            }
            
            if ( CampaignMain.cm.getRTT().getState() == Thread.State.TERMINATED ){
                CampaignMain.cm.toUser("FSM|Sorry your repair order could not be processed the repair thread has been terminated. Staff has been notified.",Username,false);
                MWLogger.errLog("AM:NOTE: Repair Thread has been terminated! Use the restartrepairthread command to restart it! If all else fails reboot!");
                return;
            }


            player.addMoney(-cost);
            //set Lifetime repair cost
            unit.addRepairCost(cost);
            unit.addRepairCost(-1);
            player.setSave();
            time = setWorkHours(rolls,techs,unit.getEntity(),player.getMyHouse());
            MWLogger.errLog("Repair Time: "+time);
            CampaignMain.cm.getRTT().getRepairList().add(
                    RepairTrackingThread.Repair(player, unitID, techs, time,false));
            CampaignMain.cm.toUser("FSM|Repairs have begone on your "+unit.getModelName()+" <b>At a Cost of "+CampaignMain.cm.moneyOrFluMessage(true,true,cost)+"</b>",Username,false);
            CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+unit.toString(true),Username,false);

        }catch(Exception ex){
            MWLogger.errLog("Unable to Process Repair Unit Command!");
            MWLogger.errLog(ex);
        }
        
	}//end process()

  
    private int setWorkHours(Vector<Integer> rolls, Vector<Integer> techs, Entity unit, SHouse house) {

        int techType;
        int baseRoll;
        int totalTime = 0;
        boolean engineFound = false;

        for ( int location = 0; location < unit.locations(); location++ ){
            if ( unit.getArmor(location) != unit.getOArmor(location) ){
                techType = techs.elementAt(UnitUtils.ARMOR);
                baseRoll = rolls.elementAt(UnitUtils.ARMOR);
                totalTime += calculateTime(unit,location,UnitUtils.LOC_FRONT_ARMOR,techType,baseRoll,true,house);
            }
            if ( unit.hasRearArmor(location) && unit.getArmor(location,true) != unit.getOArmor(location,true) ){
                techType = techs.elementAt(UnitUtils.ARMOR);
                baseRoll = rolls.elementAt(UnitUtils.ARMOR);
                totalTime += calculateTime(unit,location,UnitUtils.LOC_REAR_ARMOR,techType,baseRoll,true,house);
            }
            if ( unit.getInternal(location) != unit.getOInternal(location) ){
                techType = techs.elementAt(UnitUtils.INTERNAL);
                baseRoll = rolls.elementAt(UnitUtils.INTERNAL);
                totalTime += calculateTime(unit,location,UnitUtils.LOC_INTERNAL_ARMOR,techType,baseRoll,true,house);
            }
            
            for ( int slot = 0; slot < unit.getNumberOfCriticals(location); slot++ ){
                CriticalSlot cs = unit.getCritical(location,slot);
                if ( cs == null )
                    continue;
                
                if ( !cs.isBreached() && !cs.isDamaged() )
                    continue;
                if ( cs.getType() == CriticalSlot.TYPE_SYSTEM && cs.getIndex() != Mech.SYSTEM_ENGINE){
                    techType = techs.elementAt(UnitUtils.SYSTEMS);
                    baseRoll = rolls.elementAt(UnitUtils.SYSTEMS);
                    totalTime += calculateTime(unit,location,slot,techType,baseRoll,false,house);
                    //move the slot ahead if the Crit is more then 1 in size.
                    slot += Math.max(0,UnitUtils.getNumberOfCrits(unit,cs)-1);
                    continue;
                }
                if ( cs.getType() == CriticalSlot.TYPE_SYSTEM && cs.getIndex() == Mech.SYSTEM_ENGINE
                        && !engineFound ){
                    techType = techs.elementAt(UnitUtils.ENGINES);
                    baseRoll = rolls.elementAt(UnitUtils.ENGINES);
                    totalTime += calculateTime(unit,location,slot,techType,baseRoll,false,house);
                    engineFound = true;
                }
                if (cs.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                    Mounted mounted = cs.getMount();
                    
                    if ( !(mounted.getType() instanceof WeaponType) ){
                        techType = techs.elementAt(UnitUtils.EQUIPMENT);
                        baseRoll = rolls.elementAt(UnitUtils.EQUIPMENT);
                    }else{
                        techType = techs.elementAt(UnitUtils.WEAPONS);
                        baseRoll = rolls.elementAt(UnitUtils.WEAPONS);
                    }
                    totalTime += calculateTime(unit,location,slot,techType,baseRoll,false,house);
                    //move the slot ahead if the Crit is more then 1 in size.
                    slot += Math.max(0,UnitUtils.getNumberOfCrits(unit,cs)-1);
                    continue;
                }
            }
        }
        return totalTime;
    }

    private int calculateTime(Entity unit, int location, int slot, int tech, int baseRoll, boolean armor, SHouse house){
        int roll =0;
        int repairTime = CampaignMain.cm.getIntegerConfig("TimeForEachRepairPoint");
        
        roll = UnitUtils.getTechRoll(unit, location, slot,
                tech, armor,house.getTechLevel()) - baseRoll;
        
        if (!armor) {
            CriticalSlot cs = unit.getCritical(location, slot);
            int totalCrits = UnitUtils.getNumberOfCrits(unit, cs);
            repairTime *= totalCrits;
        }

        for ( int count = 0; count < roll; count++)
            repairTime *= 2;

        repairTime = (int)(repairTime * timeIncreaseBasedOnRoll(baseRoll));
        
        return repairTime;
    }
    
    private double timeIncreaseBasedOnRoll(int roll){
        if (roll <= 2) {
            return 1.0;
        } else if (roll > 12) {
            return 36.0;
        }
        final double[] payout = { 1.0, 1.0, 1.0, 1.03, 1.09, 1.20, 1.38,
                1.72, 2.40, 3.60, 5.92, 12.0, 36.0 };
        return payout[roll];
    }

}//end RepairUnitCommand