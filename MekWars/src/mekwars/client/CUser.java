/*
 * MekWars - Copyright (C) 2004 
 * 
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Original author Helge Richter (McWizard)
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

package client;

import java.awt.Color;
import java.util.StringTokenizer;

import common.House;
import common.campaign.clientutils.IClientUser;
import common.util.MWLogger;
import common.util.StringUtils;

/*
 * Class for User objects held in userlist
 */

public class CUser implements Comparable<Object>, IClientUser {

    protected String Name;

    protected String Addon;

    protected int Userlevel = 0;

    protected String PlayerHouse;

    protected String Fluff;

    protected int Exp;

    protected float Rating;

    protected int Status;

    protected String HTMLColor;

    protected Color RGBColor;

    protected String Country;

    protected boolean LoggedIn = false;

    protected boolean isMerc = false;

    protected boolean isInvis = false;

    protected String subFaction = "";

    /**
     * Empty CUser.
     */
    public CUser() {
        Name = "";
        Addon = "";
        PlayerHouse = "";
        Fluff = "";
        Exp = 0;
        Rating = 0;
        Status = MWClient.STATUS_LOGGEDOUT;
        HTMLColor = "black";
        RGBColor = Color.black;
        Country = "";
        isMerc = false;
        isInvis = false;
    }

    /**
     * New CUser w/ data. Called NU|MWClientInfo.toString()|NEW/NONE command.
     */
    public CUser(String data) {

        StringTokenizer ST = null;

        Addon = "";
        PlayerHouse = "";
        Fluff = "";
        Exp = 0;
        Rating = 0;
        Status = MWClient.STATUS_LOGGEDOUT;
        RGBColor = Color.black;

        ST = new StringTokenizer(data, "~");
        try {
            Name = ST.nextToken();
            HTMLColor = ST.nextToken();
            Country = ST.nextToken();
            Userlevel = Integer.parseInt(ST.nextToken());
            isInvis = Boolean.parseBoolean(ST.nextToken());
        } catch (Exception ex) {
            MWLogger.errLog("Error in deserializing user");
        }
    }

    public void setName(String tname) {
        Name = tname;
    }

    public String getName() {
        return Name;
    }

    public void setAddon(String taddon) {
        Addon = taddon;
    }

    public String getAddon() {
        return Addon;
    }

    public void setCountry(String tcountry) {
        Country = tcountry;
    }

    public String getCountry() {
        return Country;
    }

    public void setColor(String tcolor) {
        HTMLColor = tcolor;
    }

    public String getColor() {
        return HTMLColor;
    }

    public void setUserlevel(int tlevel) {
        Userlevel = tlevel;
    }

    public int getUserlevel() {
        return Userlevel;
    }

    public void setFluff(String tfluff) {
        Fluff = tfluff;
    }

    public String getFluff() {
        return Fluff;
    }

    public String getHouse() {
        return PlayerHouse;
    }

    public void setExp(int texp) {
        Exp = texp;
    }

    public int getExp() {
        return Exp;
    }

    public void setRating(float trating) {
        Rating = trating;
    }

    public float getRating() {
        return Rating;
    }

    public boolean isInvis() {
        return isInvis;
    }

    public void setMercStatus(boolean merc) {
        isMerc = merc;
    }

    public boolean isMerc() {
        return this.isMerc;
    }

    public Color getRGBColor() {
        return RGBColor;
    }

    public void setSubFactionName(String subFaction) {
        this.subFaction = subFaction;
    }

    public void setCampaignData(MWClient mwclient, String data) {
        StringTokenizer ST = new StringTokenizer(data, "#");
        try {

            Exp = Integer.parseInt(ST.nextToken());
            Rating = Float.parseFloat(ST.nextToken());
            setStatus(Integer.parseInt(ST.nextToken()));

            if (ST.hasMoreTokens())
                Fluff = ST.nextToken();
            if (Fluff.equals(" ") || Fluff.equals("0"))
                Fluff = "";

            if (ST.hasMoreTokens())
                PlayerHouse = ST.nextToken();
            if (ST.hasMoreElements())
                isMerc = Boolean.parseBoolean(ST.nextToken());

            if (ST.hasMoreElements())
                subFaction = ST.nextToken();

            // Abbreviation and Color from House (sed to be sent as part of
            // player update)
            House playerH = mwclient.getData().getHouseByName(PlayerHouse);
            Addon = playerH.getAbbreviation();

            RGBColor = Color.black;
            RGBColor = StringUtils.html2Color(playerH.getHousePlayerColor());
        } catch (Exception ex) {
            MWLogger.errLog(ex);
        }
    }

    public void clearCampaignData() {
        Addon = "";
        PlayerHouse = "";
        Fluff = "";
        Exp = 0;
        Rating = 0;
        Status = MWClient.STATUS_LOGGEDOUT;
        RGBColor = Color.black;
    }

    public void setStatus(int status) {
        Status = status;
        if (Status == MWClient.STATUS_LOGGEDOUT) {
            LoggedIn = false;
            clearCampaignData();
        } else {
            if (Status == MWClient.STATUS_RESERVE || Status == MWClient.STATUS_ACTIVE || Status == MWClient.STATUS_FIGHTING) {
                LoggedIn = true;
            }
        }
    }

    public int getStatus() {
        return Status;
    }

    public boolean isLoggedIn() {
        return LoggedIn;
    }

    public String getShortInfo() {
        StringBuffer info = new StringBuffer("<html><body>");
        info.append(getName());
        if (Userlevel >= 100 && Userlevel < 200) {
            info.append(" (Moderator)");
        }
        if (Userlevel >= 200) {
            info.append(" (Admin)");
        }
        if (!Country.equals("unknown")) {
            info.append(" (");
            info.append(getCountry());
            info.append(")");
        }
        info.append("</body></html>");
        return info.toString();
    }

    public String getInfo(boolean removeImages) {

        StringBuffer info = new StringBuffer("<html><body>");
        info.append(Name);
        if (!Addon.equals("") && LoggedIn) {
            info.append(" [");
            info.append(Addon);
            info.append("]");
        }
        if (Userlevel >= 100 && Userlevel < 200) {
            info.append(" (Moderator)");
        }
        if (Userlevel >= 200) {
            info.append(" (Admin)");
        }
        if (!Country.equals("unknown")) {
            info.append(" (");
            info.append(getCountry());
            info.append(")");
        }
        if (LoggedIn) {
            info.append("<br>Exp: ");
            info.append(Exp);

            // only show the rating if its real. will be 0.0 if server is hiding
            // ELOs.
            if (Rating >= 1) {
                info.append(" Rating: ");
                info.append(Rating);
            }

            if (PlayerHouse.trim().length() > 0) {
                info.append("<br>Fights for ");
                info.append(PlayerHouse);
                if (subFaction.trim().length() > 0 && !subFaction.equalsIgnoreCase("none")) {
                    info.append("<br><center>");
                    info.append(subFaction);
                    info.append("</center>");
                }
            }

            if (!Fluff.equals("")) {

                // if the user wants to, remove any img tags in fluff
                if (removeImages) {

                    info.append("<br>");
                    int start = Fluff.toLowerCase().indexOf("<img");
                    int finish = -1;

                    if (start != -1)
                        finish = Fluff.indexOf(">", start);

                    if (start != -1 && finish != -1) {
                        String firstHalf = Fluff.substring(0, start);
                        String secondHalf = Fluff.substring(finish + 1, Fluff.length());

                        info.append(firstHalf);
                        info.append("(img blocked)");
                        info.append(secondHalf);
                    } else
                        info.append(Fluff);

                }

                // otherwise, just display the fluff
                else {
                    info.append("<br>");
                    info.append(Fluff);
                }
            }

        }
        info.append("</body></html>");
        return info.toString();
    }

    /**
     * Comparable, for PlayerNameDialog. Don't use elsewhere =)
     */
    @Override
	public int compareTo(Object o) {
        if (!(o instanceof CUser))
            return 0;

        CUser u = (CUser) o;
        return this.getName().compareTo(u.getName());
    }

}