package mekwars.server.campaign.commands;

import common.campaign.pilot.Pilot;
import server.campaign.pilot.SPilot;

/**
 * @author Torren (Jason Tighe)
 */
public class DonatePilotCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}

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
        server.campaign.SHouse house = p.getMyHouse();

        int donationsAllowed = Integer.parseInt(house.getConfig("DonationsAllowed"));
        if (donationsAllowed <= 0) {
            server.campaign.CampaignMain.cm.toUser("AM:Donations are not allowed on this server.", Username, true);
            return;
        }


        if (p.mayAcquireWelfareUnits()) {
            server.campaign.CampaignMain.cm.toUser("AM:You may not donate any of your pilots while you are on welfare.",
                  Username,
                  true);

            return;
        }

        if (p.getMyHouse().isNewbieHouse()) {
            server.campaign.CampaignMain.cm.toUser("AM:SOL Players are not allowed to donate pilots, sorry!",
                  Username,
                  true);
            return;
        }

        int unitType = Integer.parseInt(command.nextToken());
        int weightClass = Integer.parseInt(command.nextToken());
        int pilotLocation = Integer.parseInt(command.nextToken());

        Pilot pilot = p.getPersonalPilotQueue().getPilot(unitType, weightClass, pilotLocation);

        if (pilot == null) {
            server.campaign.CampaignMain.cm.toUser("AM:Unable to find pilot!", Username, true);
            return;
        }
        int mechdonateprize = 0;
        if (Boolean.parseBoolean(house.getConfig("DonatingCostsBills"))) {
            mechdonateprize = Integer.parseInt(house.getConfig("CostToBuyNewPilot")) / 2;
            int infToDonate = weightClass * 2;
            if ((p.getMoney() >= mechdonateprize) && (p.getInfluence() >= infToDonate)) {
                p.addMoney(-mechdonateprize);
                p.addInfluence(-infToDonate);
            } else if (p.getUnits().size() < 4) {
                server.campaign.CampaignMain.cm.toUser(
                      "AM:HQ has allowed you to retrain this pilot, at a reduced rate, due to your current situation.",
                      Username,
                      true);
                if (p.getMoney() >= mechdonateprize) {p.addMoney(-mechdonateprize);} else {
                    mechdonateprize = p.getMoney();
                    p.addMoney(-mechdonateprize);
                }
                p.addInfluence(-infToDonate);
            } else {
                server.campaign.CampaignMain.cm.toUser("AM:You can't afford to retrain this pilot. You need " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(true,
                                                                   true,
                                                                   mechdonateprize) +
                                                             " and " +
                                                             server.campaign.CampaignMain.cm.moneyOrFluMessage(false,
                                                                   false,
                                                                   infToDonate) +
                                                             ".", Username, true);
                //send the pilot back to the players queue.
                p.getPersonalPilotQueue().addPilot(pilot, weightClass);
                return;
            }
        }

        //set up strign to send user
        String toUser = "AM:You've sent pilot " + pilot.getName() + " back to the faction for more training";
        if (mechdonateprize > 0) {
            toUser += ". Your faction charges you " +
                            server.campaign.CampaignMain.cm.moneyOrFluMessage(true, true, mechdonateprize) +
                            " for the transfer";
        }
        toUser += ".";

        p.getMyHouse().getPilotQueues().addPilot(unitType, (SPilot) pilot);
        server.campaign.CampaignMain.cm.toUser("PL|RPPPQ|" + unitType + "|" + weightClass + "|" + pilotLocation,
              Username,
              false);
        server.campaign.CampaignMain.cm.doSendHouseMail(p.getMyHouse(),
              "NOTE",
              p.getName() + " donated a " + pilot.getName().trim() + " to the faction pools!");


    }
}

