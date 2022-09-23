/*
 * MekWars - Copyright (C) 2007
 * 
 * Original author - jtighe (torren@users.sourceforge.net)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package server.campaign.commands.leader;

import java.util.StringTokenizer;

import common.util.ComponentToCritsConverter;
import server.campaign.CampaignMain;
import server.campaign.SHouse;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.commands.Command;

public class SetComponentConversionCommand implements Command {

    int accessLevel = CampaignMain.cm.getIntegerConfig("factionLeaderLevel");

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }

    String syntax = "Crit Name#Weight#Type#Max Production#House[Optional Staff Only]";

    public String getSyntax() {
        return syntax;
    }

    public void process(StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.cm.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " + userLevel + ". Required: " + accessLevel + ".", Username, true);
                return;
            }
        }
        
        SPlayer player = CampaignMain.cm.getPlayer(Username);
        SHouse house = player.getMyHouse();
        String crit = command.nextToken();
        int weight = Integer.parseInt(command.nextToken());
        int type = Integer.parseInt(command.nextToken());
        int maxProduction = Integer.parseInt(command.nextToken());
        
        if ( CampaignMain.cm.getServer().isModerator(Username) && command.hasMoreElements() )
            house = CampaignMain.cm.getHouseFromPartialString(command.nextToken(),Username);
        
        if( crit.equalsIgnoreCase("all") ) {
            house.getComponentConverter().clear();
            ComponentToCritsConverter converter = new ComponentToCritsConverter();
            converter.setComponentUsedType(type);
            converter.setComponentUsedWeight(weight);
            converter.setMinCritLevel(maxProduction);
            house.getComponentConverter().put(converter.getCritName(),converter);
        }else {
            house.getComponentConverter().remove("All");
            ComponentToCritsConverter converter = new ComponentToCritsConverter();
            converter.setCritName(crit);
            converter.setComponentUsedType(type);
            converter.setComponentUsedWeight(weight);
            converter.setMinCritLevel(maxProduction);
            house.getComponentConverter().put(converter.getCritName(),converter);
        }
        
        CampaignMain.cm.doSendHouseMail(house, "NOTE", player.getName()+" has set components to crit conversion for "+crit+" for "+SUnit.getWeightClassDesc(weight)+"/"+SUnit.getTypeClassDesc(type)+" to a max of "+maxProduction+"  crits.");
    }
}