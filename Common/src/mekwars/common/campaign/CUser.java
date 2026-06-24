/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * Copyright (C) 2004 Helge Richter (McWizard)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */


package mekwars.common.campaign;

import java.awt.Color;
import java.util.StringTokenizer;

import jakarta.annotation.Nonnull;
import megamek.codeUtilities.MathUtility;
import megamek.logging.MMLogger;
import mekwars.common.House;
import mekwars.common.campaign.clientutils.IClientUser;
import mekwars.common.campaign.clientutils.protocol.IClient;
import mekwars.common.util.StringUtils;

/*
 * Class for User objects held in userlist
 */

public class CUser implements Comparable<CUser>, IClientUser {
    private final static MMLogger LOGGER = MMLogger.create(CUser.class);

    protected String name;
    protected String addon;
    protected int userLevel = 0;
    protected String playerHouse;
    protected String fluff;
    protected int exp;
    protected float rating;
    protected int status;
    protected String htmlColor;
    protected Color rgbColor;
    protected String country;
    protected boolean loggedIn = false;
    protected boolean merc = false;
    protected boolean invisible = false;
    protected String subFaction = "";

    /**
     * Empty CUser.
     */
    public CUser() {
        name = "";
        addon = "";
        playerHouse = "";
        fluff = "";
        exp = 0;
        rating = 0;
        status = IClient.STATUS_LOGGED_OUT;
        htmlColor = "black";
        rgbColor = Color.black;
        country = "";
        merc = false;
        invisible = false;
    }

    /**
     * New CUser w/ data. Called NU|MWDedHostInfo.toString()|NEW/NONE command.
     */
    public CUser(String data) {

        StringTokenizer stringTokenizer;

        addon = "";
        playerHouse = "";
        fluff = "";
        exp = 0;
        rating = 0;
        status = IClient.STATUS_LOGGED_OUT;
        rgbColor = Color.black;

        stringTokenizer = new StringTokenizer(data, "~");

        try {
            name = stringTokenizer.nextToken();
            htmlColor = stringTokenizer.nextToken();
            country = stringTokenizer.nextToken();
            userLevel = Integer.parseInt(stringTokenizer.nextToken());
            invisible = Boolean.parseBoolean(stringTokenizer.nextToken());
        } catch (Exception ex) {
            LOGGER.error(ex, "Error in deserializing user");
        }
    }

    public String getAddon() {
        return addon;
    }

    public void setAddon(String addon) {
        this.addon = addon;
    }

    public void setHTMLColor(String color) {
        this.htmlColor = color;
    }

    public String getHtmlColor() {
        return htmlColor;
    }

    public int getUserLevel() {
        return userLevel;
    }

    public void setUserLevel(int level) {
        this.userLevel = level;
    }

    public String getHouse() {
        return playerHouse;
    }

    public String getFluff() {
        return this.fluff;
    }

    public void setFluff(String fluff) {
        this.fluff = fluff;
    }

    public int getExp() {
        return exp;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public boolean isInvisible() {
        return invisible;
    }

    public void setMercStatus(boolean merc) {
        this.merc = merc;
    }

    public boolean isMerc() {
        return merc;
    }

    public Color getRGBColor() {
        return rgbColor;
    }

    public void setSubFactionName(String subFaction) {
        this.subFaction = subFaction;
    }

    public void setCampaignData(IClient client, String data) {
        StringTokenizer stringTokenizer = new StringTokenizer(data, "#");

        try {
            exp = MathUtility.parseInt(stringTokenizer.nextToken(), 0);
            rating = MathUtility.parseFloat(stringTokenizer.nextToken(), 0.0f);
            setStatus(MathUtility.parseInt(stringTokenizer.nextToken(), 0));

            if (stringTokenizer.hasMoreTokens()) {
                fluff = stringTokenizer.nextToken();
            }

            if (fluff.equals(" ") || fluff.equals("0")) {
                fluff = "";
            }

            if (stringTokenizer.hasMoreTokens()) {playerHouse = stringTokenizer.nextToken();}
            if (stringTokenizer.hasMoreElements()) {merc = Boolean.parseBoolean(stringTokenizer.nextToken());}

            if (stringTokenizer.hasMoreElements()) {subFaction = stringTokenizer.nextToken();}

            // Abbreviation and Color from House (sed to be sent as part of
            // player update)
            House playerH = client.getData().getHouseByName(playerHouse);

            rgbColor = java.awt.Color.black;

            if (playerH != null) {
                addon = playerH.getAbbreviation();
                rgbColor = StringUtils.html2Color(playerH.getHousePlayerColor());
            }
        } catch (Exception ex) {
            LOGGER.error(ex, "Unable to set Campaign Data: {}", ex.getLocalizedMessage());
        }
    }

    public void clearCampaignData() {
        addon = "";
        playerHouse = "";
        fluff = "";
        exp = 0;
        rating = 0;
        status = IClient.STATUS_LOGGED_OUT;
        rgbColor = java.awt.Color.black;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;

        if (status == IClient.STATUS_LOGGED_OUT) {
            loggedIn = false;
            clearCampaignData();
        } else {
            if (this.status == IClient.STATUS_RESERVE ||
                      this.status == IClient.STATUS_ACTIVE ||
                      this.status == IClient.STATUS_FIGHTING) {
                this.loggedIn = true;
            }
        }
    }

    public boolean isLoggedIn() {
        return loggedIn;
    }

    public String getShortInfo() {
        StringBuilder info = new StringBuilder("<html><body>");
        info.append(getName());

        if (userLevel >= 100 && userLevel < 200) {
            info.append(" (Moderator)");
        }

        if (userLevel >= 200) {
            info.append(" (Admin)");
        }

        if (!country.equals("unknown")) {
            info.append(" (");
            info.append(getCountry());
            info.append(")");
        }

        info.append("</body></html>");

        return info.toString();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getInfo(boolean removeImages) {

        StringBuilder info = new StringBuilder("<html><body>");
        info.append(name);
        if (!addon.isEmpty() && loggedIn) {
            info.append(" [");
            info.append(addon);
            info.append("]");
        }

        if (userLevel >= 100 && userLevel < 200) {
            info.append(" (Moderator)");
        }

        if (userLevel >= 200) {
            info.append(" (Admin)");
        }
        if (!country.equals("unknown")) {
            info.append(" (");
            info.append(getCountry());
            info.append(")");
        }
        if (loggedIn) {
            info.append("<br>Exp: ");
            info.append(exp);

            // only show the rating if it's real. will be 0.0 if server is hiding
            // ELOs.
            if (rating >= 1) {
                info.append(" Rating: ");
                info.append(rating);
            }

            if (!playerHouse.trim().isEmpty()) {
                info.append("<br>Fights for ");
                info.append(playerHouse);
                if (!subFaction.trim().isEmpty() && !subFaction.equalsIgnoreCase("none")) {
                    info.append("<br><center>");
                    info.append(subFaction);
                    info.append("</center>");
                }
            }

            if (!fluff.isEmpty()) {

                // if the user wants to, remove any img tags in fluff
                if (removeImages) {

                    info.append("<br>");
                    int start = fluff.toLowerCase().indexOf("<img");
                    int finish = -1;

                    if (start != -1) {finish = fluff.indexOf(">", start);}

                    if (start != -1 && finish != -1) {
                        String firstHalf = fluff.substring(0, start);
                        String secondHalf = fluff.substring(finish + 1);

                        info.append(firstHalf);
                        info.append("(img blocked)");
                        info.append(secondHalf);
                    } else {info.append(fluff);}

                }

                // otherwise, just display the fluff
                else {
                    info.append("<br>");
                    info.append(fluff);
                }
            }

        }
        info.append("</body></html>");
        return info.toString();
    }


    /**
     * Comparable, for PlayerNameDialog. Don't use elsewhere
     */
    public int compareTo(@Nonnull CUser rhs) {
        return this.getName().compareTo(rhs.getName());
    }

}
