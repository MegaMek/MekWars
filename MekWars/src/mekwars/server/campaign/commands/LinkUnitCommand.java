/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
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

import java.util.Enumeration;
import java.util.StringTokenizer;

import common.Unit;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;


public class LinkUnitCommand implements Command {
	
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
		
    //syntax /c linkunit#army#slave#master enter -1 in master to release
    if (command.hasMoreElements()) {
      SPlayer p = CampaignMain.cm.getPlayer(Username);
      int armyid = Integer.parseInt(command.nextToken());
      int slaveid = Integer.parseInt(command.nextToken());
      int masterid = -1;
      int linkid = -1;
      if (command.hasMoreElements())
        masterid = Integer.parseInt(command.nextToken());

      SArmy a = p.getArmy(armyid);
      if (a == null) {
        CampaignMain.cm.toUser("AM:Army #" + armyid + " does not exist.", Username, true);
        return;
      }

      //Is the army in a fight atm?
      if (a.isLocked()) {
        CampaignMain.cm.toUser("AM:You may not change C3 networks while an Army is in combat.", Username, true);
        return;
      }

      if (p.getDutyStatus() == SPlayer.STATUS_ACTIVE) {
          CampaignMain.cm.toUser("AM:You may not change C3 networks while on active duty.", Username, true);
          return;
      }
      
      //throw out if the target slave doesnt exist
      Unit slaveUnit = a.getUnit(slaveid);
      if ( slaveUnit == null ) {
          CampaignMain.cm.toUser("AM:Could not find unit #"+slaveid+" in army #"+armyid+".",Username,true);
          return;
      }

      if ( slaveUnit.hasBeenC3LinkedTo(a) && masterid == -1) {
          Enumeration<Integer> c3Key = a.getC3Network().keys();
          Enumeration<Integer> c3Unit = a.getC3Network().elements();
          
          while ( c3Key.hasMoreElements()){
              Integer keyId = c3Key.nextElement();
              Integer unitId = c3Unit.nextElement();
              
              if ( slaveUnit.getId() == unitId.intValue())
                  a.getC3Network().remove(keyId);
          }
          a.setRawForceSize(0);
          a.setBV(0);
          String toReturn = "AM:Unit #"+ slaveid + " was removed from its C3 network. New BV: " + a.getBV();
          
          CampaignMain.cm.toUser(toReturn,Username,true);
          CampaignMain.cm.toUser("PL|SAD|"+a.toString(true,"%"),Username,false);
          CampaignMain.cm.getOpsManager().checkOperations(a,true);//update legal ops
          return;
      }
      
      else if (masterid == -1) {
          a.getC3Network().remove(slaveid);
          a.setRawForceSize(0);
          a.setBV(0);
          
          String toReturn = "AM:Unit #"+ slaveid + " was removed from its C3 network. New BV: " + a.getBV();
          
          CampaignMain.cm.toUser(toReturn,Username,true);
          CampaignMain.cm.toUser("PL|SAD|"+a.toString(true,"%"),Username,false);
          CampaignMain.cm.getOpsManager().checkOperations(a,true);//update legal ops
          return;
      }
      
      //throw out if the master unit doesnt exist.
      Unit masterUnit = a.getUnit(masterid);
      if ( masterUnit == null ){
          CampaignMain.cm.toUser("AM:Unable to find master unit (#"+masterid+") in Army #"+armyid+".",Username,true);
          return;
      }
      
      linkid = slaveUnit.linkToC3Network(a,masterUnit);
      if ( linkid == masterUnit.getId() ) {
          a.setRawForceSize(0);
          a.setBV(0);
          
          String toReturn = "AM:Unit #"+ slaveUnit.getId() + " is now linked to Unit #"+masterUnit.getId()+". New BV: " + a.getBV();
          
          CampaignMain.cm.toUser(toReturn,Username,true);
          CampaignMain.cm.toUser("PL|SAD|"+a.toString(true,"%"),Username,false);
          CampaignMain.cm.getOpsManager().checkOperations(a,true);//update legal ops
      }
      
      else
          CampaignMain.cm.toUser("AM:Unabled to Link Unit #"+slaveUnit.getId()+" to unit #"+masterUnit.getId()+".",Username,true);
    }//end if(command has more elements)
    
  }//end process()
}//end LinkUnitCommand.java