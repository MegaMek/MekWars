package mekwars.server.campaign.commands;

import common.campaign.pilot.Pilot;
import mekwars.server.campaign.CampaignMain;
import server.campaign.pilot.SPilot;

/**
 * @author Torren (Jason Tighe)
 */
public class DonatePilotCommand implements Command {

    int accessLevel = 0;
    String syntax = "";

    public void process(java.util.StringTokenizer command, String Username) {

        if (accessLevel != 0) {
            int userLevel = CampaignMain.campaignMain.getServer().getUserLevel(Username);
            if (userLevel < getExecutionLevel()) {
                CampaignMain.campaignMain.toUser("AM:Insufficient access level for command. Level: " +
                                                       userLevel +
                                                       ". Required: " +
                                                       accessLevel +
                                                       ".", Username, true);
                return;
            }
        }

        server.campaign.SPlayer p = CampaignMain.campaignMain.getPlayer(Username);
        server.campaign.SHouse house = p.getMyHouse();

        int donationsAllowed = Integer.parseInt(house.getConfig("DonationsAllowed"));
        if (donationsAllowed <= 0) {
            CampaignMain.campaignMain.toUser("AM:Donations are not allowed on this server.", Username, true);
            return;
        }


        if (p.mayAcquireWelfareUnits()) {
            CampaignMain.campaignMain.toUser("AM:You may not donate any of your pilots while you are on welfare.",
                  Username,
                  true);

            return;
        }

        if (p.getMyHouse().isNewbieHouse()) {
            CampaignMain.campaignMain.toUser("AM:SOL Players are not allowed to donate pilots, sorry!",
                  Username,
                  true);
            return;
        }

        int unitType = Integer.parseInt(command.nextToken());
        int weightClass = Integer.parseInt(command.nextToken());
        int pilotLocation = Integer.parseInt(command.nextToken());

        Pilot pilot = p.getPersonalPilotQueue().getPilot(unitType, weightClass, pilotLocation);

        if (pilot == null) {
            CampaignMain.campaignMain.toUser("AM:Unable to find pilot!", Username, true);
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
                CampaignMain.campaignMain.toUser(
                      "AM:HQ has allowed you to retrain this pilot, at a reduced rate, due to your current situation.",
                      Username,
                      true);
                if (p.getMoney() >= mechdonateprize) {p.addMoney(-mechdonateprize);} else {
                    mechdonateprize = p.getMoney();
                    p.addMoney(-mechdonateprize);
                }
                p.addInfluence(-infToDonate);
            } else {
                CampaignMain.campaignMain.toUser("AM:You can't afford to retrain this pilot. You need " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(true,
                                                             true,
                                                             mechdonateprize) +
                                                       " and " +
                                                       CampaignMain.campaignMain.moneyOrFluMessage(false,
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
                            CampaignMain.campaignMain.moneyOrFluMessage(true, true, mechdonateprize) +
                            " for the transfer";
        }
        toUser += ".";

        p.getMyHouse().getPilotQueues().addPilot(unitType, (SPilot) pilot);
        CampaignMain.campaignMain.toUser("PL|RPPPQ|" + unitType + "|" + weightClass + "|" + pilotLocation,
              Username,
              false);
        CampaignMain.campaignMain.doSendHouseMail(p.getMyHouse(),
              "NOTE",
              p.getName() + " donated a " + pilot.getName().trim() + " to the faction pools!");


    }

    public int getExecutionLevel() {return accessLevel;}

    public void setExecutionLevel(int i) {accessLevel = i;}

    public String getSyntax() {return syntax;}
}

