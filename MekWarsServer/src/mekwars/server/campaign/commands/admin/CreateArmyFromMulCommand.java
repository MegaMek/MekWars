/*
 * MekWars - Copyright (C) 2007
 *
 * Original author - Jason Tighe (torren@users.sourceforge.net)
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */

package mekwars.server.campaign.commands.admin;

import mekwars.server.campaign.CampaignMain;

public class CreateArmyFromMulCommand implements server.campaign.commands.Command {

    int accessLevel = server.MWChatServer.auth.IAuthenticator.ADMIN;

    String syntax = "Filename#Army Name#[Target Player]";

    public String getSyntax() {
        return syntax;
    }

    public void process(java.util.StringTokenizer command, String Username) {

        // access level check
        int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
        if (userLevel < getExecutionLevel()) {
            CampaignMain.campaignMain.toUser(
                  "AM:Insufficient access level for command. Level: "
                        + userLevel + ". Required: " + accessLevel + ".",
                  Username, true);
            return;
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        String filename;
        String armyname;

        try {
            filename = command.nextToken();
            armyname = command.nextToken();
            if (command.hasMoreTokens()) {p = CampaignMain.campaignMain.getPlayer(command.nextToken());}
        } catch (Exception ex) {
            CampaignMain.campaignMain.toUser("Syntax Error: /createarmyfrommul " + syntax, Username);
            return;
        }

        if (p == null) {
            CampaignMain.campaignMain.toUser("Unable to find target player", Username);
            return;
        }

        if (p.getArmies().size() >= CampaignMain.campaignMain.getIntegerConfig("MaxLancesPerPlayer")) {
            CampaignMain.campaignMain.toUser(p.getName() + " has too many armies already!", Username);
            return;
        }

        if (!new java.io.File("./data/armies").exists()) {
            CampaignMain.campaignMain.toUser("directory ./data/armies does not exist", Username);
            new java.io.File("./data/armies").mkdir();
            return;
        }

        java.util.Vector<server.campaign.SUnit> units = new java.util.Vector<server.campaign.SUnit>(1, 1);
        units.addAll(server.campaign.SUnit.createMULUnits(filename));

        server.campaign.SArmy army = new server.campaign.SArmy(p.getName());

        army.setID(p.getFreeArmyId());
        army.setName(armyname);
        for (server.campaign.SUnit cm : units) {
            cm.setProducer("Mul Army Unit " + armyname);
            p.addUnit(cm, true);
            army.addUnit(cm);
        }
        p.getArmies().add(army);
        army.getBV();
        army.setOpForceSize(army.getAmountOfUnits());

        CampaignMain.campaignMain.toUser("PL|SAD|" + army.toString(true, "%"), p.getName(), false);
        CampaignMain.campaignMain.toUser("army created: " + armyname, p.getName(), true);
        CampaignMain.campaignMain.doSendModMail("NOTE", Username + " has created an army from file " + filename);

    }

    public int getExecutionLevel() {
        return accessLevel;
    }

    public void setExecutionLevel(int i) {
        accessLevel = i;
    }
}
