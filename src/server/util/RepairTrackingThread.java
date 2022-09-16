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
 *
 * @author Torren (Jason Tighe) 10.6.05
 *
 */

package server.util;

import java.util.Calendar;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import common.campaign.pilot.Pilot;
import common.campaign.pilot.skills.PilotSkill;
import common.util.MWLogger;
import common.util.StringUtils;
import common.util.UnitUtils;
import megamek.common.AmmoType;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Mounted;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;
import server.campaign.SUnit;

/**
 *
 * @author Torren Oct 8, 2005
 *
 * Thread use to track all repairs made by all users on the server.
 */
public class RepairTrackingThread extends Thread{

    private Vector<Repair> repairList = new Vector<Repair>(1,1);
    private long repairtime = 0;


    public RepairTrackingThread(long Time) {
        super("Repair Tracking Thread");
        repairtime = Time;
    }


    @Override
    public synchronized void run (){
        try{
            while(true){
                //MWLogger.errLog("Wait time: "+repairtime);
                this.wait(repairtime);
                checkRepairs();
            }
        }catch(Exception ex){
            MWLogger.errLog("Error while trying to sleep in RepairTrackingThread");
            MWLogger.errLog(ex);
        }

    }

    public void checkRepairs(){

        try{
            ConcurrentLinkedQueue<Repair> tempVector = new ConcurrentLinkedQueue<Repair>(repairList);
            synchronized (tempVector) {
                for ( Repair repairOrder: tempVector ){
                    //MWLogger.errLog("Start Time: "+ new Date(repairOrder.getStartTime()).toString()+" End Time: "+new Date(repairOrder.getEndTime()).toString());
                    //double minutes = (repairOrder.getEndTime()-System.currentTimeMillis())/60000;
                    //MWLogger.errLog("ETA: "+Double.toString(minutes));
                    if ( repairOrder == null ){
                        repairList.removeElement(repairOrder);
                        return;
                    }
                    if ( repairOrder.getEndTime() <= System.currentTimeMillis() ){
                        try{
                            if ( (CampaignMain.cm.getPlayer(repairOrder.getUsername()) == null)
                                    || repairOrder.finishRepair() ){
                                repairList.removeElement(repairOrder);
                                SPlayer player = CampaignMain.cm.getPlayer(repairOrder.getUsername());
                                player.checkAndUpdateArmies(player.getUnit(repairOrder.getUnitID()));
                            }
                        }
                        catch(Exception ex){
                            MWLogger.errLog("Unable to finish repair for "+repairOrder.getUsername()+" for unit #"+repairOrder.getUnitID()+" "+repairOrder.getUnit().getShortNameRaw());
                            MWLogger.errLog(ex);
                        }
                    }
                }
            }
        }
        catch (Exception ex){
            MWLogger.errLog("Error while checking repair. Containing and continuing.");
            MWLogger.errLog(ex);
        }
    }

    public String unitRepairTimes(int unitID){

        String results = "";
        String unitName = "";
        int SEC = 1000;
        int MIN = SEC * 60;
        int HOUR = MIN * 60;
        int DAY = HOUR * 24;

        synchronized (repairList) {
            for ( Repair repairOrder: repairList ){
                if (repairOrder.getUnitID() == unitID){
                    if ( unitName.length() < 1) {
                        unitName = repairOrder.getUnit().getShortNameRaw();
                    }

                    long mills = (repairOrder.getEndTime()-System.currentTimeMillis());///60000;

                    if ( mills < 0 ){
                        mills += CampaignMain.cm.getDoubleConfig("TimeForEachRepairPoint")*1000;
                    }
                    Calendar time = Calendar.getInstance();
                    time.setTimeInMillis(mills);


                    String output = "";

                    if ( mills >= DAY ){
                        output += (mills/DAY) +"d ";
                        mills %= DAY;
                    }

                    if ( mills >= HOUR ){
                        output += (mills/HOUR) +"h ";
                        mills %= HOUR;
                    }

                    if ( mills >= MIN ){
                        output += (mills/MIN) +"m ";
                        mills %= MIN;
                    }

                    if ( mills >= SEC ){
                        output += (mills/SEC) +"s ";
                        mills %= SEC;
                    }

                    /*   if ( mills > 0 )
	                	output += mills +"ms ";*/

                    output = output.trim();

                    if ( repairOrder.isSimpleRepair()){
                        results = "#"+unitID+" "+unitName+" is undergoing a complete repair cycle. ETA: "+output+".";
                        return results;
                    }

                    if ( repairOrder.getArmor() ){
                        boolean rear = false;
                        int armorLocation = repairOrder.getLocation();
                        //External armor
                        if ( repairOrder.getSlot() < UnitUtils.LOC_INTERNAL_ARMOR ){

                            switch (armorLocation){
                                case UnitUtils.LOC_CTR:
                                    armorLocation = UnitUtils.LOC_CT;
                                    rear = true;
                                    break;
                                case UnitUtils.LOC_LTR:
                                    armorLocation = UnitUtils.LOC_LT;
                                    rear = true;
                                    break;
                                case UnitUtils.LOC_RTR:
                                    armorLocation = UnitUtils.LOC_RT;
                                    rear = true;
                                    break;
                                default:
                                    rear = false;
                                    break;
                            }

                            if ( rear ) {
                                results += "Repairing external armor "+repairOrder.getUnit().getLocationAbbr(armorLocation)+"(r) ETA: "+output+".";
                            } else {
                                results += "Repairing external armor "+repairOrder.getUnit().getLocationAbbr(armorLocation)+" ETA: "+output+".";
                            }
                        }//Internal armor
                        else{
                            results += "Repairing internal structure "+repairOrder.getUnit().getLocationAbbr(armorLocation)+" ETA: "+output+".";
                        }
                    }else{
                        CriticalSlot cs = repairOrder.getUnit().getCritical(repairOrder.getLocation(),repairOrder.getSlot());

                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            results +="Repairing "+mounted.getName()+"("+ repairOrder.getUnit().getLocationAbbr(repairOrder.getLocation())+") ETA: "+output+".";
                        }// end CS type if
                        else{
                            if (repairOrder.getUnit() instanceof Mech) {
                                results += "Repairing "+((Mech)repairOrder.getUnit()).getSystemName(cs.getIndex())+"("+repairOrder.getUnit().getLocationAbbr(repairOrder.getLocation())+") ETA: "+output+".";
                            }
                        }//end CS type else
                    }
                    results += " <a href=\"MEKWARS/c stoprepairjob#"+unitID+"#"+repairOrder.getLocation()+"#"+repairOrder.getSlot()+"#"+repairOrder.getArmor()+"\">Click here to stop</a><br>";
                }//end if
            }//end for

        }

        if ( results.length() > 1){
            results = "#"+unitID+" "+unitName+" has the following repair jobs pending:<br>"+results;
            return results;
        }

        return null;
    }

    public Vector<Repair> getRepairList(){
        return repairList;
    }

    public boolean isBeingRepaired(int unitID, int location, int slot, boolean armor){

        Vector<Repair> tempRepairList = new Vector<Repair>(getRepairList());
        synchronized (tempRepairList) {
            for ( Repair repairOrder: tempRepairList ){
                if ( repairOrder.matches(unitID,location,slot,armor) ) {
                    return true;
                }
            }
        }
        return false;
    }

    public void stopRepair(int unitID, int location, int slot, boolean armor){

        ConcurrentLinkedQueue<Repair> tempRepairList = new ConcurrentLinkedQueue<Repair>(getRepairList());

        synchronized (tempRepairList) {
            for ( Repair repairOrder:tempRepairList ){
                if ( repairOrder.matches(unitID,location,slot,armor) ){
                    SPlayer player = CampaignMain.cm.getPlayer(repairOrder.getUsername());
                    SUnit unit = player.getUnit(unitID);
                    CampaignMain.cm.toUser("FSM|Repair order cancelled.",repairOrder.getUsername(),false);
                    if ( repairOrder.getTechType() == UnitUtils.TECH_PILOT ) {
                        unit.setPilotIsRepairing(false);
                    } else {
                        player.addAvailableTechs(repairOrder.getTechType(),1);
                    }
                    repairOrder.stopRepair();
                    getRepairList().removeElement(repairOrder);
                    unit.setEntity(repairOrder.getUnit());
                    CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+unit.toString(true),player.getName(),false);
                    return;
                }
            }
        }
    }

    public void stopAllRepairJobs(int unitID, SPlayer player){

        ConcurrentLinkedQueue<Repair> tempRepairList = new ConcurrentLinkedQueue<Repair>(getRepairList());

        synchronized (tempRepairList) {

            for ( Repair repairOrder:tempRepairList ){
                if ( repairOrder.getUnitID() == unitID ){
                    try{
                        int techType = repairOrder.getTechType();
                        if ( techType != UnitUtils.TECH_PILOT ) {
                            player.addAvailableTechs(techType,1);
                        }
                    }catch(Exception ex){}
                    repairOrder.stopRepair();
                    getRepairList().removeElement(repairOrder);
                }
            }
        }
        try{
            SUnit unit = player.getUnit(unitID);
            CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+unit.toString(true),player.getName(),false);
        }catch (Exception ex){}

    }

    public static Repair Repair(SPlayer player, int unitID, boolean armor,int location,int slot,int techType,int retries,int techWorkMod, boolean salvage){

        return new Repair(player, unitID, armor,location,slot,techType,retries,techWorkMod,salvage);
    }

    public static Repair Repair(SPlayer player, int unitID, Vector<Integer>techs, int repairTime,boolean salvage){

        return new Repair(player, unitID, techs, repairTime,true,salvage);
    }
}

class Repair{

    private Entity unit;
    private String Username;
    private int unitID;
    private boolean armor = false;
    private int location = -1;
    private int slot;
    private long repairTime;
    private long startTime;
    private long endTime;
    private int techType;
    private int retries;
    private int techWorkMod;
    private boolean simpleRepair = false;
    private Vector<Integer>techs;
    private boolean salvage = false;

    public Repair(SPlayer player, int unitID, boolean armor,int location,int slot,int techType,int retries,int techWorkMod, boolean salvage){

        unit = player.getUnit(unitID).getEntity();
        this.location = location;
        this.slot = slot;
        this.unitID = unitID;
        Username = player.getName();
        this.armor = armor;
        this.techType = techType;
        this.retries = retries;
        this.techWorkMod = techWorkMod;
        simpleRepair = false;
        this.salvage = salvage;

        if ( techType == UnitUtils.TECH_REWARD_POINTS ) {
            repairTime = 0;
        } else{
            player.addAvailableTechs(techType,-1);

            setRepairTime();
        }
    }

    public Repair(SPlayer player, int unitID, Vector<Integer>techs, int repairTime, boolean simpleRepair,boolean salvage){

        this.simpleRepair = simpleRepair;
        this.repairTime = repairTime;
        this.techs = techs;
        this.unitID = unitID;
        unit = player.getUnit(unitID).getEntity();
        Username = player.getName();
        location = UnitUtils.LOC_CT;
        slot = UnitUtils.LOC_FRONT_ARMOR;
        armor = true;
        unit.setArmor(0,UnitUtils.LOC_CT);
        UnitUtils.setArmorRepair(unit,UnitUtils.LOC_FRONT_ARMOR,UnitUtils.LOC_CT);
        startTime = System.currentTimeMillis();
        endTime = startTime+(repairTime*1000);
        this.salvage = salvage;
        //CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+unit.toString(true),Username,false);
    }

    public boolean finishRepair(){

        CriticalSlot cs = null;
        Pilot pilot = null;

        SPlayer player = CampaignMain.cm.getPlayer(Username);
        if ( player == null ){
            MWLogger.errLog("Could not find player "+Username+" removing repair job from queue.");
            return true;
        }
        SUnit mek = player.getUnit(unitID);

        if ( mek == null ){
            MWLogger.errLog("Could not find unit # "+unitID+" for player "+player.getName()+ " removing repair job from queue.");
            return true;
        }
        pilot = mek.getPilot();

        try{
            int roll = 12;

            int die1 = CampaignMain.cm.getRandomNumber(6)+1;
            int die2 = CampaignMain.cm.getRandomNumber(6)+1;

            boolean levelTech = false;
            boolean pilotIsRepairing = false;
            boolean rear = false;
            boolean retireTech = false;
            boolean techDeath = false;
            boolean repairTech = techType == UnitUtils.TECH_REWARD_POINTS;
            boolean useCrits = CampaignMain.cm.getBooleanConfig("UsePartsRepair");
            String critName = "";
            int damagedCrits = 0;
            boolean disableTechAdvancement = CampaignMain.cm.getBooleanConfig("DisableTechAdvancement");


            if ( useCrits ) {
                critName = UnitUtils.getCritName(unit, slot, location, armor);

                //Need to turn off armor repair so we can get an acurate account of whats needs to
                //be repaired.
                if ( armor ) {
                    UnitUtils.removeArmorRepair(unit,slot,location);
                }

                if ( salvage ){
                    if ( armor ) {
                        damagedCrits = UnitUtils.getNumberOfCrits(unit, slot,location);
                    } else {
                        damagedCrits = UnitUtils.getNumberOfCrits(unit, slot,location)-UnitUtils.getNumberOfDamagedCrits(unit,slot,location,armor);
                    }
                } else {
                    damagedCrits = UnitUtils.getNumberOfDamagedCrits(unit,slot,location,armor);
                }
                //Ok turn armor repairing back on.
                if ( armor ) {
                    UnitUtils.setArmorRepair(unit,slot,location);
                }
            }


            //Repairs will not finish on units that are on the front lines.
            if ((player.getAmountOfTimesUnitExistsInArmies(unitID) > 0)
                    && (player.getDutyStatus() == SPlayer.STATUS_ACTIVE)) {
                player.setSave();
                return false;
            }

            //Do simpleRepairs first.
            if ( simpleRepair ){
                Entity entity = mek.getEntity();
                for (int x = 0; x < entity.locations(); x++) {
                    entity.setArmor(entity.getOArmor(x),x);
                    if ( entity.hasRearArmor(x) ) {
                        entity.setArmor(entity.getOArmor(x,true),x,true);
                    }
                    entity.setInternal(entity.getOInternal(x),x);
                    for (int y = 0; y < entity.getNumberOfCriticals(x); y++) {
                        cs = entity.getCritical(x,y);

                        if ( cs == null ) {
                            continue;
                        }

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

                // Meks Repaired now lets upgrade the techs.
                for (int type = UnitUtils.ARMOR; type <= UnitUtils.ENGINES; type++) {

                    techType = techs.elementAt(type);
                    // Tech made it so he can try and level
                    die1 = CampaignMain.cm.getRandomNumber(6) + 1;
                    die2 = CampaignMain.cm.getRandomNumber(6) + 1;

                    // MWLogger.errLog("tech level roll: "+(die1+die2)+"
                    // base: "+(10+techType));

                    if(!disableTechAdvancement) {
                        if (((die1 + die2) >= (10 + techType)) && !pilotIsRepairing && !repairTech) {
                            levelTech = true;
                        } else if (((die1 + die2) >= (11 + techType)) && pilotIsRepairing) {
                            levelTech = true;
                        }
                    }

                    // Roll to see if the Tech Retires.
                    die1 = CampaignMain.cm.getRandomNumber(6) + 1;
                    die2 = CampaignMain.cm.getRandomNumber(6) + 1;

                    if ((techType > UnitUtils.TECH_GREEN)
                            && ((die1 + die2) <= ((9 + techType) / 4// Regs & Vets retire on 2. Elites on 3.
)
)
                            && !pilotIsRepairing && !levelTech && !repairTech && !disableTechAdvancement) {
                        retireTech = true;
                    }

                    // not more repairing pilots.
                    if (pilotIsRepairing) {
                        mek.setPilotIsRepairing(false);
                    }

                    if (levelTech) {
                        if (pilotIsRepairing) {
                            pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).setLevel(pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel() + 1);
                            CampaignMain.cm.toUser("FSM|"
                                    + pilot.getName()
                                    + " has graduated to "
                                    + StringUtils.aOrAn(UnitUtils.techDescription(techType + 1),true)
                                    + " tech.", Username, false);
                        } else if ( player.getTotalTechs().elementAt(techType) > 0){
                            // AvailableTech was already removed so just move the tech to the next level
                            player.addAvailableTechs(techType + 1, 1);
                            // Now remove the tech from its old class and move it to its new class
                            player.addTotalTechs(techType, -1);
                            player.addTotalTechs(techType + 1, 1);
                            CampaignMain.cm.toUser("FSM|One of your "
                                    + UnitUtils.techDescription(techType)
                                    + " techs has graduated to "
                                    + StringUtils.aOrAn(UnitUtils.techDescription(techType + 1),true)
                                    + " tech.", Username, false);
                        }
                    }

                    if (retireTech && (player.getTotalTechs().elementAt(techType) > 0) ) {
                        CampaignMain.cm.toUser(
                                "FSM|<font color=#ff80ff>One of your "
                                        + UnitUtils.techDescription(techType)
                                        + " techs retired.</font>", Username,
                                        false);
                        player.addAvailableTechs(techType, -1);
                        player.addTotalTechs(techType, -1);
                    }

                    CampaignMain.cm.toUser("PL|UU|" + unitID + "|"
                            + mek.toString(true), Username, false);
                    player.checkAndUpdateArmies(mek);
                }
                CampaignMain.cm.toUser("FSM|Your "+ unit.getShortNameRaw()
                        + " is now fully operational and combat ready again!",Username, false);
                player.setSave();
                return true;
            }


            if ( techType == UnitUtils.TECH_PILOT){
                pilot = mek.getPilot();
                techType = pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel();
                pilotIsRepairing = true;
            }

            if ( techWorkMod > 0) {
                roll = UnitUtils.getTechRoll(unit,location,slot,techType-techWorkMod,armor,player.getMyHouse().getTechLevel(),salvage);
            } else {
                roll = UnitUtils.getTechRoll(unit,location,slot,techType,armor,player.getMyHouse().getTechLevel(),salvage);
                roll += techWorkMod;
            }

            if ( !armor
                    && (unit.getCritical(location, slot) != null)
                    && unit.getCritical(location, slot).isBreached() ) {
                roll = Math.max(roll,2);
            } else {
                roll = Math.max(roll,3);
            }

            if ( repairTech ){
                roll = 2;
                die1 = die2 = 6;
            }

            //Check to see if the pilot died while failing the repair.
            if ( ((die1+die2) == 2) && !pilotIsRepairing ){
                int chance = CampaignMain.cm.getIntegerConfig("ChanceTechDiesOnFailedRepair");
                if (chance > CampaignMain.cm.getRandomNumber(100)) {
                    techDeath = true;
                    retries = 0;
                }
            }

            CampaignMain.cm.toUser("FSM|Tech Roll Base: "+roll+" Roll: "+(die1+die2),Username,false);
            //failed the tech roll
            if ( (die1+die2) < roll ){
                if ( armor ){
                    int armorRepaired = 0;
                    int armorToRepair = 0;
                    rear = false;
                    //External armor
                    if ( slot != UnitUtils.LOC_INTERNAL_ARMOR ){

                        switch (location){
                            case UnitUtils.LOC_CTR:
                                location = UnitUtils.LOC_CT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_LTR:
                                location = UnitUtils.LOC_LT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_RTR:
                                location = UnitUtils.LOC_RT;
                                rear = true;
                                break;
                            default:
                                if ( slot == UnitUtils.LOC_REAR_ARMOR) {
                                    rear = true;
                                } else {
                                    rear = false;
                                }
                                break;
                        }

                        if ( salvage ) {
                            UnitUtils.removeArmorRepair(unit,slot,location);
                            armorToRepair = unit.getArmor(location,rear);


                            if ( armorToRepair <= 3) {
                                armorRepaired = 1;
                            } else {
                                armorRepaired = CampaignMain.cm.getRandomNumber(armorToRepair-2)+1;
                            }

                            //the more they miss the roll by the less armor is repaired.
                            armorRepaired = Math.max(1,armorRepaired-(roll-(die1+die2)));

                            //if the roll is higher then 12 make them pay for it.
                            if ( roll > 12 ) {
                                armorRepaired = 0;
                            }

                            player.updatePartsCache(critName, armorRepaired);

                            unit.setArmor(0,location,rear);
                            String points = " points ";
                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            if ( rear ) {
                                CampaignMain.cm.toUser("FSM|The salvage job was not completely successful only "+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+"r) were salvaged from your "+unit.getShortNameRaw()+".",Username,false);
                            } else {
                                CampaignMain.cm.toUser("FSM|The salvage job was not completely successful only "+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+") were salvaged from your "+unit.getShortNameRaw()+".",Username,false);
                            }

                        }else {
                            UnitUtils.removeArmorRepair(unit,slot,location);
                            armorToRepair = unit.getOArmor(location,rear)-unit.getArmor(location,rear);

                            if ( armorToRepair <= 3) {
                                armorRepaired = 1;
                            } else {
                                armorRepaired = CampaignMain.cm.getRandomNumber(armorToRepair-2)+1;
                            }

                            //the more they miss the roll by the less armor is repaired.
                            armorRepaired = Math.max(1,armorRepaired-(roll-(die1+die2)));

                            //if the roll is higher then 12 make them pay for it.
                            if ( roll > 12 ) {
                                armorRepaired = 0;
                            }

                            unit.setArmor((unit.getArmor(location,rear)+armorRepaired),location,rear);
                            String points = " points ";
                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            if ( rear ) {
                                CampaignMain.cm.toUser("FSM|The repair job was not completely successful only "+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+"r) were repaired on your "+unit.getShortNameRaw()+".",Username,false);
                            } else {
                                CampaignMain.cm.toUser("FSM|The repair job was not completely successful only "+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+") were repaired on your "+unit.getShortNameRaw()+".",Username,false);
                            }

                            if ( unit.getArmor(location,rear) == unit.getOArmor(location,rear) ){
                                if ( rear ) {
                                    CampaignMain.cm.toUser("FSM|However the external armor("+unit.getLocationAbbr(location)+"r) is now fully repaired.",Username,false);
                                } else {
                                    CampaignMain.cm.toUser("FSM|However the external armor("+unit.getLocationAbbr(location)+") is now fully repaired.",Username,false);
                                }
                                //set the armor and set them free.
                                unit.setArmor(unit.getOArmor(location,rear),location,rear);
                                retries = 0;
                            }
                        }
                        //unit.setArmor((unit.getArmor(location,rear)+99),location,rear);
                    }//Internal armor
                    else{

                        if ( salvage ) {
                            UnitUtils.removeArmorRepair(unit,UnitUtils.LOC_INTERNAL_ARMOR,location);

                            armorToRepair = unit.getInternal(location);


                            //MWLogger.errLog("IS Amount1: "+armorToRepair);
                            if ( armorToRepair <= 3) {
                                armorRepaired = 1;
                            } else {
                                armorRepaired = CampaignMain.cm.getRandomNumber(armorToRepair-2)+1;
                            }

                            //MWLogger.errLog("IS Amount2: "+armorRepaired);
                            //the more they miss the roll by the less armor is repaired.
                            armorRepaired = Math.max(1,armorRepaired-(roll-(die1+die2)));
                            //MWLogger.errLog("IS Amount3: "+armorRepaired);

                            //if the roll is higher then 12 make them pay for it.
                            if ( roll > 12 ) {
                                armorRepaired = 0;
                            }

                            player.updatePartsCache(critName, armorRepaired);

                            unit.setInternal(0,location);
                            String points = " points ";

                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            CampaignMain.cm.toUser("FSM|The repair job was not completely successful only "+armorRepaired+points+"of internal structure("+unit.getLocationAbbr(location)+") were salvaged from your "+unit.getShortNameRaw()+".",Username,false);

                        }else {
                            UnitUtils.removeArmorRepair(unit,UnitUtils.LOC_INTERNAL_ARMOR,location);
                            //if IS is less then 0 then its been destroyed fully. so reset to 0 for the repair
                            if ( unit.getInternal(location) < 0 ) {
                                unit.setInternal(0,location);
                            }
                            armorToRepair = unit.getOInternal(location)-unit.getInternal(location);

                            if ( armorToRepair <= 3) {
                                armorRepaired = 1;
                            } else {
                                armorRepaired = CampaignMain.cm.getRandomNumber(armorToRepair-2)+1;
                            }

                            //the more they miss the roll by the less armor is repaired.
                            armorRepaired = Math.max(1,armorRepaired-(roll-(die1+die2)));

                            //if the roll is higher then 12 make them pay for it.
                            if ( roll > 12 ) {
                                armorRepaired = 0;
                            }

                            unit.setInternal((unit.getInternal(location)+armorRepaired),location);
                            String points = " points ";

                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            CampaignMain.cm.toUser("FSM|The repair job was not completely successful only "+armorRepaired+points+"of internal structure("+unit.getLocationAbbr(location)+") were repaired on your "+unit.getShortNameRaw()+".",Username,false);


                            if ( unit.getOInternal(location) == unit.getInternal(location) ){
                                CampaignMain.cm.toUser("FSM|However the internal structure("+unit.getLocationAbbr(location)+") is now fully repaired.",Username,false);
                                //set the armor and set them free.
                                unit.setInternal(unit.getOInternal(location),location);
                                retries = 0;
                            }
                            //unit.setInternal((unit.getInternal(location)+99),location);
                        }//end internal message
                    }
                    //partial armor repairs are allowed so their for we need to send an update
                    //end armor failure
                }else{
                    String repairMessage = "";

                    cs = unit.getCritical(location,slot);

                    if ( salvage ) {
                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            repairMessage ="The salvage job for the "+mounted.getName()+"("+ unit.getLocationAbbr(location)+") on the "+unit.getShortNameRaw()+" was a complete failure and the part was lost.";
                            UnitUtils.salvageEquipment(mounted, unit, location);
                        }// end CS type if
                        else{
                            if ( UnitUtils.isEngineCrit(cs) ){
                                repairMessage = "The salvage job on the engines for the "+unit.getShortNameRaw()+" was a complete failure and the engines are now completely destroyed.";
                                UnitUtils.destroyAllEngineCrits(unit);
                            }
                            else if (unit instanceof Mech) {
                                repairMessage = "The salvage job for the "+((Mech)unit).getSystemName(cs.getIndex())+"("+unit.getLocationAbbr(location)+") of the "+unit.getShortName()+" was a complete failure and the part was lost.";
                                UnitUtils.salvageSystemCrit(location, cs, unit);
                            }
                        }//end CS type else

                        CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
                    }else {
                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            repairMessage ="The repair job for the "+mounted.getName()+"("+ unit.getLocationAbbr(location)+") on the "+unit.getShortNameRaw()+" was a complete failure.";
                        }// end CS type if
                        else{
                            if ( UnitUtils.isEngineCrit(cs) ){
                                repairMessage = "The repair job on the engines for the "+unit.getShortNameRaw()+" was a complete failure.";
                            }
                            else{
                                if (unit instanceof Mech) {
                                    repairMessage = "The repair job for the "+((Mech)unit).getSystemName(cs.getIndex())+"("+unit.getLocationAbbr(location)+") of the "+unit.getShortName()+" was a complete failure.";
                                }
                            }
                        }//end CS type else

                        CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
                    }
                }//end of failed roll
                if ( (retries > 0) &&  (roll < 13)){
                    //Reset back to pilot.
                    if ( pilotIsRepairing ) {
                        techType = UnitUtils.TECH_PILOT;
                    }

                    int cost = CampaignMain.cm.getRepairCost(unit,location,slot,techType,armor,techWorkMod,salvage);
                    if ( player.getAutoReorder() && (player.getPartsAmount(critName) < damagedCrits) ) {
                        String newCommand = critName+"#"+damagedCrits;
                        CampaignMain.cm.getServerCommands().get("BUYPARTS").process(new StringTokenizer(newCommand,"#"), Username);
                    }

                    if ( (cost < player.getMoney()) && (player.getPartsAmount(critName) >= damagedCrits)){

                        retries--;
                        setRepairTime();
                        CampaignMain.cm.toUser("FSM|The tech responsible rededicates himself to the task.",Username,false);
                        player.addMoney(-cost);
                        player.getUnit(unitID).addRepairCost(cost);
                        if ( armor ) {
                            UnitUtils.setArmorRepair(unit,slot,location);
                        } else if ( cs != null ) {
                            UnitUtils.setRepairing(unit,cs);
                        }

                        //set the location back to a rear location number i.e. LOC_CTR, LOC_RTR, LOC_LTR
                        if ( rear ) {
                            location += 7;
                        }
                        mek.setEntity(unit);
                        CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+mek.toString(true),Username,false);
                        player.checkAndUpdateArmies(mek);

                        if ( useCrits ) {
                            if ( critName.indexOf("Ammo") > -1) {
                                player.updatePartsCache("Ammo Bin", -1);
                            } else {
                                player.updatePartsCache(critName, -1);
                            }
                        }
                        player.setSave();
                        return false;
                    }
                }
                if ( armor ) {
                    UnitUtils.removeArmorRepair(unit,slot,location);
                } else if ( cs != null ) {
                    UnitUtils.removeRepairing(unit,cs);
                }

                mek.setEntity(unit);
                CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+mek.toString(true),Username,false);
                player.checkAndUpdateArmies(mek);
                //end Failure
            }else{
                if ( armor ){
                    int armorRepaired = 0;
                    rear = false;
                    //External armor
                    if ( slot != UnitUtils.LOC_INTERNAL_ARMOR ){
                        switch (location){
                            case UnitUtils.LOC_CTR:
                                location = UnitUtils.LOC_CT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_LTR:
                                location = UnitUtils.LOC_LT;
                                rear = true;
                                break;
                            case UnitUtils.LOC_RTR:
                                location = UnitUtils.LOC_RT;
                                rear = true;
                                break;
                            default:
                                if ( slot == UnitUtils.LOC_REAR_ARMOR) {
                                    rear = true;
                                } else {
                                    rear = false;
                                }
                                break;
                        }

                        UnitUtils.removeArmorRepair(unit,slot,location);

                        if ( salvage ) {
                            armorRepaired = unit.getArmor(location,rear);
                            unit.setArmor(0,location,rear);

                            player.updatePartsCache(critName, armorRepaired);

                            String points = " points ";
                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            if ( rear ) {
                                CampaignMain.cm.toUser("FSM|"+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+"r) were salvaged from your "+unit.getShortNameRaw()+".",Username,false);
                            } else {
                                CampaignMain.cm.toUser("FSM|"+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+") were salvaged from your "+unit.getShortNameRaw()+".",Username,false);
                            }
                        }else {
                            armorRepaired = unit.getOArmor(location,rear)-unit.getArmor(location,rear);
                            unit.setArmor(unit.getOArmor(location,rear),location,rear);

                            String points = " points ";
                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            if ( rear ) {
                                CampaignMain.cm.toUser("FSM|"+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+"r) were repaired on your "+unit.getShortNameRaw()+".",Username,false);
                            } else {
                                CampaignMain.cm.toUser("FSM|"+armorRepaired+points +"of external armor("+unit.getLocationAbbr(location)+") were repaired on your "+unit.getShortNameRaw()+".",Username,false);
                            }
                        }
                    }//Internal armor
                    else{

                        UnitUtils.removeArmorRepair(unit,slot,location);

                        if ( salvage ) {
                            armorRepaired = unit.getInternal(location);
                            unit.setInternal(0,location);
                            String points = " points ";

                            player.updatePartsCache(critName, armorRepaired);

                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            CampaignMain.cm.toUser("FSM|"+armorRepaired+points+"of internal structure("+unit.getLocationAbbr(location)+") were salvaged from your "+unit.getShortNameRaw()+".",Username,false);

                        }else {
                            armorRepaired = unit.getOInternal(location)-unit.getInternal(location);
                            unit.setInternal(unit.getOInternal(location),location);
                            String points = " points ";

                            if ( armorRepaired == 1) {
                                points = " point ";
                            }
                            CampaignMain.cm.toUser("FSM|"+armorRepaired+points+"of internal structure("+unit.getLocationAbbr(location)+") were repaired on your "+unit.getShortNameRaw()+".",Username,false);
                        }
                    }
                    //end of armor repair
                }else{

                    String repairMessage = "";

                    cs = unit.getCritical(location,slot);

                    if ( salvage ) {
                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            UnitUtils.salvageEquipment(mounted,unit,location);
                            repairMessage ="The "+mounted.getName()+"("+ unit.getLocationAbbr(location)+") on the "+unit.getShortNameRaw()+" was succesfully salvaged.";
                            //Ammo Bins have both the bin and the ammo
                            if ( mounted.getType() instanceof AmmoType ) {
                                player.updatePartsCache("Ammo Bin",1);
                                damagedCrits = mounted.getUsableShotsLeft();
                                critName = ((AmmoType)mounted.getType()).getInternalName();
                                if ( damagedCrits > 0 ) {
                                    repairMessage += "<br>"+damagedCrits + " rounds of "+((AmmoType)mounted.getType()).getName()+" where recovered along with the bin.";
                                }
                            }

                        }// end CS type if
                        else{
                            if ( UnitUtils.isEngineCrit(cs) ){
                                UnitUtils.destroyAllEngineCrits(unit);
                                repairMessage = "All viable engines parts on the "+unit.getShortNameRaw()+" have been successfully salvaged.";
                            }
                            else{
                                if (unit instanceof Mech) {
                                    repairMessage = "The "+((Mech)unit).getSystemName(cs.getIndex())+"("+unit.getLocationAbbr(location)+") of your "+unit.getShortName()+" has been salvaged.";
                                }
                                //UnitUtils.fixCriticalSlot(cs,unit);
                                UnitUtils.salvageSystemCrit(location,cs,unit);
                                unit.setCritical(location,slot,cs);
                            }
                        }//end CS type else

                        player.updatePartsCache(critName, damagedCrits);

                        CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
                    }else {
                        if ( cs.getType() == CriticalSlot.TYPE_EQUIPMENT ){
                            Mounted mounted = cs.getMount();
                            UnitUtils.repairEquipment(mounted,unit,location);
                            if ( mounted.getType() instanceof AmmoType ) {
                                critName = "Ammo Bin";
                            }
                            repairMessage ="The "+mounted.getName()+"("+ unit.getLocationAbbr(location)+") on the "+unit.getShortNameRaw()+" is now fully functional.";
                        }// end CS type if
                        else{
                            if ( UnitUtils.isEngineCrit(cs) ){
                                UnitUtils.repairDamagedEngine(unit);
                                repairMessage = "All engines on the "+unit.getShortNameRaw()+" are now fully functional.";
                            }
                            else{
                                if (unit instanceof Mech) {
                                    repairMessage = "The "+((Mech)unit).getSystemName(cs.getIndex())+"("+unit.getLocationAbbr(location)+") of the "+unit.getShortName()+" is now fully functional.";
                                }
                                //UnitUtils.fixCriticalSlot(cs,unit);
                                UnitUtils.repairSystemCrit(location,cs,unit);
                                unit.setCritical(location,slot,cs);
                            }
                        }//end CS type else

                        CampaignMain.cm.toUser("FSM|"+repairMessage,Username,false);
                    }
                }//end of crit repair
                //Tech made it so he can try and level
                die1 = CampaignMain.cm.getRandomNumber(6)+1;
                die2 = CampaignMain.cm.getRandomNumber(6)+1;

                //MWLogger.errLog("tech level roll: "+(die1+die2)+" base: "+(10+techType));

                if(!disableTechAdvancement) {
                    if ( ((die1+die2) >= (10+techType)) && !pilotIsRepairing && !repairTech){
                        levelTech = true;
                    }else if ( ((die1+die2) >= (11+techType)) && pilotIsRepairing){
                        levelTech = true;
                    }
                }
                //Roll to see if the Tech Retires.
                die1 = CampaignMain.cm.getRandomNumber(6)+1;
                die2 = CampaignMain.cm.getRandomNumber(6)+1;

                if ( (techType > UnitUtils.TECH_GREEN)
                        && ((die1+die2) <= ((9+techType)/4//Regs and Vets retire on 2 Elites on 3
)
)
                        && !pilotIsRepairing
                        && !levelTech
                        && !repairTech
                        && !disableTechAdvancement){
                    retireTech = true;
                }

            }//end of successful repair

            //not more repairing pilots.
            if ( pilotIsRepairing ) {
                mek.setPilotIsRepairing(false);
            }

            if ( !UnitUtils.hasArmorDamage(unit) && !UnitUtils.hasCriticalDamage(unit) && !UnitUtils.isRepairing(unit) ) {
                SUnit su = player.getUnit(unitID);
                CampaignMain.cm.toUser("FSM|Your "+unit.getShortNameRaw()+" is now fully operational and combat ready again!<br>Total Cost was "+CampaignMain.cm.moneyOrFluMessage(true, true, su.getCurrentRepairCost()),Username,false);
                su.addRepairCost(-1);
            }

            if ( levelTech ){
                if (pilotIsRepairing) {
                    pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).setLevel(pilot.getSkills().getPilotSkill(PilotSkill.AstechSkillID).getLevel()+1);
                    CampaignMain.cm.toUser("FSM|<font color=#C11B17>"+pilot.getName()+" advanced in skill and is now " + StringUtils.aOrAn(UnitUtils.techDescription(techType+1),true) + " tech.</font>",Username,false);
                }else{
                    //AvailableTech was already removed so just move the tech to the next level
                    player.addAvailableTechs(techType+1,1);
                    //Now remove the tech from its old class and move it to its new class
                    player.addTotalTechs(techType,-1);
                    player.addTotalTechs(techType+1,1);
                    CampaignMain.cm.toUser("FSM|<font color=#C11B17>One of your " + UnitUtils.techDescription(techType) + " techs advanced in skill and is now "+ StringUtils.aOrAn(UnitUtils.techDescription(techType+1),true) + " tech.</font>",Username,false);
                }
            }else if ( !pilotIsRepairing && !repairTech) {
                player.addAvailableTechs(techType,1);
            }

            if ( retireTech ){
                CampaignMain.cm.toUser("FSM|<font color=#ff80ff>One of your "+UnitUtils.techDescription(techType)+" techs retired.</font>",Username,false);
                player.addAvailableTechs(techType,-1);
                player.addTotalTechs(techType,-1);
            }

            if ( techDeath ){
                CampaignMain.cm.toUser("FSM|<font color=#302226>One of your "+UnitUtils.techDescription(techType)+" techs was killed in an accident while repairing "+unit.getShortNameRaw()+".</font>",Username,false);
                player.addAvailableTechs(techType,-1);
                player.addTotalTechs(techType,-1);
            }
            mek.setEntity(unit);
            CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+mek.toString(true),Username,false);
            player.setSave();
            player.checkAndUpdateArmies(mek);
            return true;
        }catch(Exception ex){
            MWLogger.errLog("Failed to trap the following error removing repair job from queue: ");
            MWLogger.errLog(ex);
            if ( (mek != null) && (player != null) ){
                mek.setEntity(unit);
                CampaignMain.cm.toUser("PL|UU|"+unitID+"|"+mek.toString(true),Username,false);
                player.setSave();
                player.checkAndUpdateArmies(mek);
            }
            return true;
        }
    }

    public long getEndTime(){
        return endTime;
    }

    public long getStartTime(){
        return startTime;
    }

    public boolean matches(int unitID, int location, int slot, boolean armor){
        if ( (this.location == location) && (this.unitID == unitID) && (this.slot == slot) && (this.armor == armor)) {
            return true;
        }
        return false;
    }

    public int getUnitID(){
        return unitID;
    }

    public boolean getArmor(){
        return armor;
    }

    public int getLocation(){
        return location;
    }

    public int getSlot(){
        return slot;
    }

    public Entity getUnit(){
        return unit;
    }

    public String getUsername(){
        return Username;
    }

    public int getTechType(){
        return techType;
    }

    public boolean isSimpleRepair(){
        return simpleRepair;
    }

    public void setRepairTime(){
        //repair time in MS
        repairTime = Long.parseLong(CampaignMain.cm.getConfig("TimeForEachRepairPoint"))*1000;
        if ( !armor ){
            CriticalSlot cs = unit.getCritical(location,slot);
            UnitUtils.setRepairing(unit,cs);
            //MWLogger.errLog("number of crits "+ UnitUtils.getNumberOfCrits(unit,cs));
            repairTime *= UnitUtils.getNumberOfCrits(unit,cs);
            //MWLogger.errLog("Repair Time: "+repairTime);
        }else{
            int templocation = location;
            if ( slot != UnitUtils.LOC_INTERNAL_ARMOR ){
                switch (location){
                    case UnitUtils.LOC_CTR:
                        templocation = UnitUtils.LOC_CT;
                        break;
                    case UnitUtils.LOC_LTR:
                        templocation = UnitUtils.LOC_LT;
                        break;
                    case UnitUtils.LOC_RTR:
                        templocation = UnitUtils.LOC_RT;
                        break;
                    default:
                        break;
                }
            }
            UnitUtils.setArmorRepair(unit,slot,templocation);
        }

        if ( techWorkMod > 0){
            repairTime /= 2;
        }if ( techWorkMod < 0){
            for ( int x = 0; x > techWorkMod; x--){
                repairTime *= 2;
            }
        }
        startTime = System.currentTimeMillis();
        endTime = startTime+repairTime;

        //MWLogger.errLog("Start Time: "+startTime+" End Time: "+endTime);
    }

    public void stopRepair(){
        if ( armor){
            //External armor
            if ( slot != UnitUtils.LOC_INTERNAL_ARMOR){
                switch (location){
                    case UnitUtils.LOC_CTR:
                        location = UnitUtils.LOC_CT;
                        break;
                    case UnitUtils.LOC_LTR:
                        location = UnitUtils.LOC_LT;
                        break;
                    case UnitUtils.LOC_RTR:
                        location = UnitUtils.LOC_RT;
                        break;
                    default:
                        break;
                }
                UnitUtils.removeArmorRepair(unit,slot,location);
            }else{
                UnitUtils.removeArmorRepair(unit,slot,location);
            }
        }else{
            CriticalSlot cs = unit.getCritical(location,slot);
            UnitUtils.removeRepairing(unit,cs);
        }
    }
}
