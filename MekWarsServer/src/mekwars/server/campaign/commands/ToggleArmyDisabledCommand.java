	/*
     * MekWars - Copyright (C) 2007
     *
     * Original author - Bob Eldred (billypinhead@users.sourceforge.net)
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


    package mekwars.server.campaign.commands;

    public class ToggleArmyDisabledCommand implements Command {
        int accessLevel = 0;
        String syntax = "";

        public void process(java.util.StringTokenizer command, String Username) {

            if (accessLevel != 0) {
                int userLevel = server.campaign.CampaignMain.cm.getServer().getUserLevel(Username);
                if (userLevel < getExecutionLevel()) {
                    server.campaign.CampaignMain.cm.toUser("AM:Insufficient access level for command. Level: " +
                                                                 userLevel +
                                                                 ". Required: " +
                                                                 accessLevel +
                                                                 ".", Username, true);
                    return;
                }
            }

            server.campaign.SPlayer p = server.campaign.CampaignMain.cm.getPlayer(Username);
            if (p == null) {
                server.campaign.CampaignMain.cm.toUser("AM:Null Player while Disabling/Enabling army. Report This!.",
                      Username,
                      true);
                return;
            }
            int aid = -1;
            try {
                aid = Integer.parseInt((String) command.nextElement());
            } catch (Exception e) {
                server.campaign.CampaignMain.cm.toUser("AM:Improper format. Try: /c togglearmydisabled#ID",
                      Username,
                      true);
                return;
            }
            server.campaign.SArmy army = p.getArmy(aid);
            if (army == null) {
                server.campaign.CampaignMain.cm.toUser("AM:Could not find an Army #" + aid + ".", Username, true);
                return;
            }

            if (server.campaign.CampaignMain.cm.getOpsManager().getShortOpForPlayer(p) != null) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not modify your armies while in a game.",
                      Username,
                      true);
                return;
            }

            if (p.getDutyStatus() == server.campaign.SPlayer.STATUS_ACTIVE) {
                server.campaign.CampaignMain.cm.toUser("AM:You may not modify armies while active.", Username, true);
                return;
            }

            army.toggleArmyDisabled();
            p.resetWeightedArmyNumber();
            server.campaign.CampaignMain.cm.toUser("AM:Army " +
                                                         army.getID() +
                                                         (army.isDisabled() ? " disabled." : " enabled."),
                  Username,
                  true);
        }

        public int getExecutionLevel() {return accessLevel;}

        public void setExecutionLevel(int i) {accessLevel = i;}

        public String getSyntax() {return syntax;}
    }
