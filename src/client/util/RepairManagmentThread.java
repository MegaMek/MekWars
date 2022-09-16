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

/**
 * @author Torren (Jason Tighe)
 * Created 02/01/2006
 * 
 * This Thread is run by the client to allow them to queue up jobs.
 */
package client.util;

import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import client.MWClient;
import client.campaign.CUnit;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.UnitUtils;
import megamek.common.CriticalSlot;
import megamek.common.Mech;
import megamek.common.Mounted;

public class RepairManagmentThread extends Thread{
    
    private Vector<ConcurrentLinkedQueue<String>> workOrders = new Vector<ConcurrentLinkedQueue<String>>(5,1);
    private long averageRepairTime = 1000;
    private MWClient client = null;
    
    //Set the repair time and init the work order queue
    public RepairManagmentThread(Long repairTime, MWClient client){
        if ( repairTime > 1000)
            averageRepairTime = repairTime;
        
        this.client = client;
        
        for( int x = 0; x <= UnitUtils.TECH_PILOT; x++){
            ConcurrentLinkedQueue<String> tempVector = new ConcurrentLinkedQueue<String>();
            workOrders.add(tempVector);
        }
            
    }
    
    @Override
	public synchronized void run (){
        while(true){
            try{
                this.wait(averageRepairTime);
                processWorkOrders();
            }catch(Exception ex){
                client.systemMessage("Error proccessing Repair Management queue. Alert an SO and check your ./logs/error.0 for the error");
                MWLogger.errLog("Error in Repair Management Queue");
                MWLogger.errLog(ex);
            }
        }
    }
    
    //String format unitID#Location#SlotID(using the armor/is/rear armor slots as well)#baseRoll
    public void addWorkOrder(int techType, String workOrder){
        workOrders.elementAt(techType).add(workOrder);
    }
    
    public void removeAllWorkOrders(int unitID){
        String id = Integer.toString(unitID);
        
        for ( int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++){
            Iterator<String> repairs = workOrders.elementAt(tech).iterator();
            while ( repairs.hasNext()){
                String repair = repairs.next();
                if ( repair.startsWith(id+"#"))
                    repairs.remove();
                    
            }
        }
    }
    
    public void removeWorkOrder(int techType,String data){
        Iterator<String> repairs = workOrders.elementAt(techType).iterator();
        while ( repairs.hasNext()){
            String repair = repairs.next();
            if ( repair.equals(data) ){
                repairs.remove();
                break;
            }
        }
        client.systemMessage("Removed work orders for for "+UnitUtils.techDescription(techType)+" techs.");
    }
    
    public boolean isQueued(int Location,int slot, int unitid){
        
        for ( int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++){
            Iterator<String> repairs = workOrders.elementAt(tech).iterator();
            while ( repairs.hasNext()){
                String repair = repairs.next();
                if ( repair.indexOf(Integer.toString(unitid)) == 0){
                    StringTokenizer order = new StringTokenizer(repair,"#");
                    order.nextToken();//unit id Already Verified it.
                    int locationid = Integer.parseInt(order.nextToken());
                    int slotid = Integer.parseInt(order.nextToken());
                    if ( slotid == UnitUtils.LOC_REAR_ARMOR )
                        locationid -= 7;
                    
                    if ( locationid == Location && slotid == slot )
                        return true;
                    
                }
                    
            }
        }
        
        return false;
    }
    
    public boolean hasQueuedOrders(int unitid){
        
        String id = Integer.toString(unitid);
        
        for ( int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++){
            Iterator<String> repairs = workOrders.elementAt(tech).iterator();
            while ( repairs.hasNext() ){
                String repair = repairs.next();
                if ( repair.startsWith(id+"#")){
                        return true;
                }
                    
            }
        }
        return false;
    }
    
    private void processWorkOrders(){
            
        int availableTechs = 1;
        
        synchronized(workOrders){
            for ( int pos = UnitUtils.TECH_GREEN; pos <= UnitUtils.TECH_PILOT; pos++){
                
                //no work orders for these techs on to the next one
                if ( workOrders.elementAt(pos).size() <= 0 )
                    continue;
                
                //No techs for this type what so ever! buy more!
                if ( pos != UnitUtils.TECH_PILOT && client.getPlayer().getTotalTechs().get(pos) <= 0 ){
                    client.systemMessage("You have pending work orders for "+UnitUtils.techDescription(pos)+" techs, but have none on your pay roll.");
                    continue;
                }
    
                if ( pos != UnitUtils.TECH_PILOT )
                    availableTechs = client.getPlayer().getAvailableTechs().get(pos);
                
                //all techs are busy keep it moving.
                if ( availableTechs <= 0 ){
                    //client.systemMessage("You have pending work orders for "+UnitUtils.techDescription(pos)+" techs, but all of them are busy.");
                    continue;
                }
                
                    
                //Lets start to procces work orders.
                Iterator<String> workQueue = workOrders.elementAt(pos).iterator();
                
                while (workQueue.hasNext()){
                    
                    //no more techs can't continue;
                    if ( availableTechs <= 0)
                        break;
                    
                    StringTokenizer order = new StringTokenizer(workQueue.next(),"#");
                    
                    CUnit unit = client.getPlayer().getUnit(Integer.parseInt(order.nextToken()));
                    int location = Integer.parseInt(order.nextToken());
                    int slot = Integer.parseInt(order.nextToken());
                    int roll = Integer.parseInt(order.nextToken());
                    int retries = Integer.parseInt(order.nextToken());
                    
                    boolean armor = ( slot >= UnitUtils.LOC_FRONT_ARMOR);
                    
                    if ( unit == null ){
                        MWLogger.errLog("Unable to find unit to repair. removing repair job");
                        client.systemMessage("Unable to find unit to repair. removing repair job");
                        workQueue.remove();
                        continue;
                    }
                    
                    synchronized(unit){
                        if ( pos == UnitUtils.TECH_PILOT && !unit.getPilot().getSkills().has(PilotSkill.AstechSkillID) ){
                            client.systemMessage("Work order found for the pilot of "+unit.getModelName()+" however the pilot cannot repair this unit.<br>The work order has been terminated.");
                            workQueue.remove();
                            continue;
                        }
        
                        //Pilot is busy repairing wait for the next round.
                        if ( pos == UnitUtils.TECH_PILOT && unit.getPilotIsReparing() )
                            continue;
                        
                        //check to see if CS are viable before anything else.
                        if ( !armor ){
                            CriticalSlot cs = unit.getEntity().getCritical(location,slot);
        
                            if ( cs == null ){
                                client.systemMessage(UnitUtils.techDescription(pos)+" tech work order canceled because the critical doesn't exist.");
                                workQueue.remove();
                                continue;
                            }
                            
                            if ( !cs.isDamaged() && !cs.isBreached() ){
                                client.systemMessage(UnitUtils.techDescription(pos)+" tech work order canceled because the critical was not damaged.");
                                workQueue.remove();
                                continue;
                            }
                        }
                        else{
                            if ( slot == UnitUtils.LOC_FRONT_ARMOR ){
                                int tempLocation = location;
                                if ( location >= UnitUtils.LOC_CTR )
                                    tempLocation -= 7;
                                if ( unit.getEntity().getArmor(tempLocation) == unit.getEntity().getOArmor(tempLocation) ){
                                    client.systemMessage(UnitUtils.techDescription(pos)+" tech work order canceled due to an already repaired Armor.");
                                    workQueue.remove();
                                    continue;
                                }
                                
                            }else if ( slot == UnitUtils.LOC_REAR_ARMOR){
                                int tempLocation = location;
                                if ( location >= UnitUtils.LOC_CTR )
                                    tempLocation -= 7;
                                if ( unit.getEntity().getArmor(tempLocation,true) == unit.getEntity().getOArmor(tempLocation,true) ){
                                    client.systemMessage(UnitUtils.techDescription(pos)+" tech work order canceled due to an already repaired Rear Armor.");
                                    workQueue.remove();
                                    continue;
                                }
                            }
                            else{//Internal!
                                if ( unit.getEntity().getInternal(location) == unit.getEntity().getOInternal(location) ){
                                    client.systemMessage(UnitUtils.techDescription(pos)+" tech work order canceled due to an already repaired Internal Structure.");
                                    workQueue.remove();
                                    continue;
                                }
                            }
                        }
                        //check to see if we are able to procces this repair if not continue to the next if so great!
                        if ( !UnitUtils.checkRepairViability(unit.getEntity(),location,slot,armor) )
                            continue;
                        
                        int techWorkMod = roll - UnitUtils.getTechRoll(unit.getEntity(),location,slot,pos,armor,this.client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel()) ;
        
                        if ( pos == UnitUtils.TECH_PILOT ){
                            techWorkMod = roll - UnitUtils.getTechRoll(unit.getEntity(),location,slot,unit.getPilot().getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel(),armor,this.client.getData().getHouseByName(client.getPlayer().getHouse()).getTechLevel());
                        }
        
                        client.sendChat("/c repairunit#"+unit.getId()+"#"+location+"#"+slot+"#"+armor+"#"+pos+"#"+retries+"#"+techWorkMod+"#false");
                        workQueue.remove();
                        availableTechs--;
                    }
                }
                
            }
        }
    }
    
    public String getRepairQueue(int unitID){
        String data = "None.";
        CUnit unit = client.getPlayer().getUnit(unitID);
        
        for ( int tech = UnitUtils.TECH_GREEN; tech <= UnitUtils.TECH_PILOT; tech++){
            Iterator<String> repairs = workOrders.elementAt(tech).iterator();
            while ( repairs.hasNext() ){
                String repair = repairs.next();
                if ( repair.indexOf(Integer.toString(unitID)) == 0){
                    StringTokenizer order = new StringTokenizer(repair,"#");
                    order.nextToken();//unit id Already Verified it.
                    int locationid = Integer.parseInt(order.nextToken());
                    int slotid = Integer.parseInt(order.nextToken());
                    
                    if ( data.equals("None.") )
                        data = "";
                    
                    if ( slotid == UnitUtils.LOC_FRONT_ARMOR )
                        data += UnitUtils.techDescription(tech)+" tech queued for external armor repair "+unit.getEntity().getLocationAbbr(locationid)+".";
                    else if ( slotid == UnitUtils.LOC_REAR_ARMOR )
                        data += UnitUtils.techDescription(tech)+" tech queued for external armor repair "+unit.getEntity().getLocationAbbr(locationid-7)+"(r).";
                    else if ( slotid == UnitUtils.LOC_INTERNAL_ARMOR )
                         data += UnitUtils.techDescription(tech)+" tech queued for internal structure repair "+unit.getEntity().getLocationAbbr(locationid)+".";
                    else{
                        CriticalSlot cs = unit.getEntity().getCritical(locationid,slotid);
                        
                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            data +=UnitUtils.techDescription(tech)+" tech queued for repair of "+mounted.getName()+"("+ unit.getEntity().getLocationAbbr(locationid)+").";
                        }// end CS type if
                        else{
                            if (unit.getEntity() instanceof Mech) 
                                data += UnitUtils.techDescription(tech)+" tech queued for repair of "+((Mech)unit.getEntity()).getSystemName(cs.getIndex())+"("+unit.getEntity().getLocationAbbr(locationid)+").";
                        }//end CS type else

                    }
                    data += " <a href=\"REMOVEQUEUEDWORKORDER|" + tech + "|" + repair + "\">click here to remove work order</a>.<br>";
                }
                    
            }
        }
        return data;
    }
}