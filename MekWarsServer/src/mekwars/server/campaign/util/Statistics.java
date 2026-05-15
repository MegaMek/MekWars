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

package mekwars.server.campaign.util;

import common.House;
import common.Unit;
import common.util.MWLogger;
import mekwars.server.campaign.CampaignMain;
import server.util.StringUtil;


public class Statistics {

    public static String doGetMechStats(int size) {
        java.util.TreeSet<MekStatistics> Sorted = new java.util.TreeSet<MekStatistics>();
        java.util.Enumeration<MekStatistics> e = CampaignMain.campaignMain.getMechStats().elements();
        while (e.hasMoreElements()) {
            MekStatistics m = e.nextElement();
            if (m.getMekSize() == size) {Sorted.add(m);}
        }
        boolean color = false;
        java.util.Iterator<MekStatistics> i = Sorted.iterator();
        StringBuilder result = new StringBuilder();
        result.append("<h2>" + Unit.getWeightClassDesc(size) + " Units</h2>");
        result.append("<table cellpadding=\"3\" cellspacing=\"0\"><tr bgcolor=\"#0066FF\">" +
                            "<th><font color=\"#FFFFFF\">MekName</th>" +
                            "<th><font color=\"#FFFFFF\">Games Played</th>" +
                            "<th><font color=\"#FFFFFF\">Games Won</th>" +
                            "<th><font color=\"#FFFFFF\">Times Destroyed</th>" +
                            "<th><font color=\"#FFFFFF\">Times Scrapped</th>" +
                            "<th width=\"50\" align=\"center\"><font color=\"#FFFFFF\">BV</th>" +
                            "</font><th><font color=\"#FFFFFF\">Last Used</font></th></tr>");
        while (i.hasNext()) {
            MekStatistics m = i.next();
            //Get the color for this line
            if (color) {result.append("<tr class=\"trcolored\"><td>");} else {
                result.append("<tr class=\"truncolored\"><td>");
            }
            color = !color;
            //result.append("</td><td>");
            result.append("<center>");
            result.append(m.getMekFileName());
            result.append("</td><td>");
            result.append(m.getGamesPlayed());
            result.append("</td><td>");
            result.append(m.getGamesWon());
            result.append("</td><td>");
            result.append(m.getTimesDestroyed());
            result.append("</td><td>");
            result.append(m.getTimesScrapped());
            result.append("</td><td>");
            result.append(m.getOriginalBV());

            result.append("</td><td align=\"right\">");
            if (m.getLastTimeUpdated() > 0) {
                result.append(StringUtil.readableTime(System.currentTimeMillis() - m.getLastTimeUpdated()));
            } else {result.append("unknown");}
            result.append("</td></tr>\n\r");
        }
        result.append("</table>");
        return result.toString();
    }

    public static void doRanking() {
        try {
            //String result = "<html><body bgcolor=\"#000000\" text=\"#009900\">";
            StringBuilder result = new StringBuilder();
            result.append(
                  "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"format.css\"><style type=\"text/css\"></style></head><body><font face=\"Verdana, Arial, Helvetica, sans-serif\">");
            result.append("<h2>Player Ranking:</h2><p>");
            //result.append("(Only Players with more than 1000 EXP shown)<p>";
            java.util.Iterator<House> e = CampaignMain.campaignMain.getData().getAllHouses().iterator();
            java.util.Hashtable<String, server.campaign.SmallPlayer> allplayers = new java.util.Hashtable<String, server.campaign.SmallPlayer>();
            //Player DefaultPlayer = null;
            synchronized (allplayers) {
                while (e.hasNext()) {
                    server.campaign.SHouse h = (server.campaign.SHouse) e.next();
                    if (!h.isNewbieHouse()) {allplayers.putAll(h.getSmallPlayers());}
                }
            }
            java.util.TreeSet<server.campaign.SmallPlayer> Sorted = new java.util.TreeSet<server.campaign.SmallPlayer>(
                  allplayers.values());
            java.util.Iterator<server.campaign.SmallPlayer> i = Sorted.iterator();
            java.util.Vector<server.campaign.SmallPlayer> v = new java.util.Vector<server.campaign.SmallPlayer>(1, 1);
            boolean color = false;
            while (i.hasNext()) {v.add(i.next());}
            result.append(
                  "<table cellpadding=\"3\" cellspacing=\"0\"><tr bgcolor=\"#0066FF\"><th><font color=\"#FFFFFF\">Rank</font></th><th><font color=\"#FFFFFF\">Name</th><th><font color=\"#FFFFFF\">Rating</th><th><font color=\"#FFFFFF\">House</th><th><font color=\"#FFFFFF\">House Rank</th><th><font color=\"#FFFFFF\">Comment</th></tr>");
            int rank = 1;
            //  for (int j = v.size() - 1;j >= 0 && j >= v.size() - 1000;j--) {
            for (int j = v.size() - 1; j >= 0; j--) {
                server.campaign.SmallPlayer p = v.elementAt(j);
                try {
                    //Show the limiter at the END of all Players with the limited Rating.
                    if (color) {result.append("<tr class=\"trcolored\">");} else {
                        result.append("<tr class=\"truncolored\">");
                    }
                    color = !color;
                    result.append("<td>" + rank + "</td>");
                    result.append("<td>" + p.getName() + "</td>");
                    if (CampaignMain.campaignMain.getBooleanConfig("HideELO")) {
                        result.append("<td> -- </td>");
                    } else {result.append("<td>" + p.getRatingRounded() + "</td>");}
                    result.append("<td>" + p.getMyHouse().getColoredName() + "</td>");
                    if (p.getFluffText().equals("0")) {result.append("<td> </td>");} else {
                        result.append("<td>" + p.getFluffText() + "</td>");
                    }
                    result.append("</tr>");
                    rank++;
                } catch (Exception ex) {
                    MWLogger.errLog("Error while Referencing player: " + p.getName());
                    MWLogger.errLog(ex);
                }
            }
            result.append("</table>");
            result.append("</body></html>");

            //Save Planets
            //      FileOutputStream out = new FileOutputStream("Ranking.htm");
            java.io.FileOutputStream out = new java.io.FileOutputStream(
                  CampaignMain.campaignMain.getConfig("RankingPath"));
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println(result.toString());
            p.close();
            out.close();
            mekwars.server.campaign.util.Statistics.doEXPRanking();
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public static void doEXPRanking() {
        StringBuilder result = new StringBuilder();
        result.append(
              "<html><head><link rel=\"stylesheet\" type=\"text/css\" href=\"format.css\"><style type=\"text/css\"></style></head><body><font face=\"Verdana, Arial, Helvetica, sans-serif\">");
        result.append("<h2>Player Ranking:</h2><p>");
        //  result.append("(Only Players with more than 1000 EXP shown)<p>";
        java.util.Iterator<House> e = CampaignMain.campaignMain.getData().getAllHouses().iterator();
        java.util.Hashtable<String, EXPRankingContainer> allplayers = new java.util.Hashtable<String, EXPRankingContainer>();
        //Player DefaultPlayer = null;
        while (e.hasNext()) {
            server.campaign.SHouse h = (server.campaign.SHouse) e.next();
            if (!h.isNewbieHouse()) {
                java.util.Enumeration<server.campaign.SmallPlayer> en = h.getSmallPlayers().elements();
                while (en.hasMoreElements()) {
                    EXPRankingContainer EXPRankPlayer = new EXPRankingContainer(en.nextElement());
                    allplayers.put(EXPRankPlayer.getName(), EXPRankPlayer);
                }
            }
        }
        java.util.TreeSet<EXPRankingContainer> Sorted = new java.util.TreeSet<EXPRankingContainer>(allplayers.values());

        java.util.Iterator<EXPRankingContainer> i = Sorted.iterator();
        java.util.Vector<EXPRankingContainer> v = new java.util.Vector<EXPRankingContainer>(1, 1);
        while (i.hasNext()) {v.add(i.next());}
        result.append(
              "<table cellpadding=\"3\" cellspacing=\"0\"><tr bgcolor=\"#0066FF\"><th><font color=\"#FFFFFF\">Rank</font></th><th><font color=\"#FFFFFF\">Name</th><th><font color=\"#FFFFFF\">Experience</th><th><font color=\"#FFFFFF\">House</th><th><font color=\"#FFFFFF\">House Rank</th><th><font color=\"#FFFFFF\">Comment</th></tr>");
        int rank = 1;
        boolean color = false;
        for (int j = v.size() - 1; j > 0 && j >= v.size() - 1000; j--) {
            if (color) {result.append("<tr class=\"trcolored\">");} else {result.append("<tr class=\"truncolored\">");}
            color = !color;
            EXPRankingContainer p = v.elementAt(j);
            result.append("<td>" + rank + "</td>");
            result.append("<td>" + p.getName() + "</td>");
            result.append("<td>" + p.getExperience() + "</td>");
            result.append("<td>" + p.getMyHouse().getColoredName() + "</td>");
            if (p.getFluffText().equals("0")) {result.append("<td> </td>");} else {
                result.append("<td>" + p.getFluffText() + "</td>");
            }
            result.append("</tr>");
            rank++;
        }
        result.append("</table>");
        result.append("</body></html>");

        try {
            java.io.FileOutputStream out = new java.io.FileOutputStream(CampaignMain.campaignMain.getConfig(
                  "EXPRankingPath"));
            java.io.PrintStream p = new java.io.PrintStream(out);
            p.println(result.toString());
            p.close();
            out.close();
        } catch (Exception ex) {
            //snark
        }
    }

    public static String getReadableHouseRanking(boolean useHTML) {

        String result = "";
        if (useHTML) {result += "<b><i>Faction Ranking: </i><br>";} else {result += "Faction Ranking: ";}

        java.util.TreeSet<HouseRankingHelpContainer> s = CampaignMain.campaignMain.getHouseRanking();
        if (s.size() < 1) {return "";}

        for (HouseRankingHelpContainer h : s) {
            if (useHTML) {result += h.getHouse().getColoredNameAsLink() + " (";} else {
                result += h.getHouse().getName() + " (";
            }

            int diff = h.getAmount() - h.getHouse().getInitialHouseRanking();
            if (diff > 0) {result += "+" + diff;} else {result += diff;}
            result += "/" + h.getAmount() + "), ";
        }

        int sendLength = result.lastIndexOf(",");
        result = result.substring(0, sendLength) + ".";

        if (useHTML) {result += "</b>";}
        return result;
    }

}
