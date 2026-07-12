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
 *
 * Represents a connected account/identity in the client's global user list (the server's roster of everyone
 * currently connected), as distinct from CPlayer, which is the local player's own in-campaign game state. A CUser
 * tracks lightweight, display-oriented account/session info: display name, connection status, moderator/admin
 * level, chosen house/faction and sub-faction (once they've joined a campaign), campaign exp/rating, and free-text
 * "fluff" bio text — everything the userlist and comm panels need to render other users, but none of a player's
 * actual units/army/personnel data (which lives on CPlayer instead, only for the locally-controlled account).
 */

public class CUser implements Comparable<CUser>, IClientUser {
    private final static MMLogger LOGGER = MMLogger.create(CUser.class);

    /** Display name / login handle of this user. */
    protected String name;
    /** House abbreviation tag appended to the display name while logged in and affiliated with a house; derived from {@link House#getAbbreviation()}. */
    protected String addon;
    /** Permission tier: &lt;100 = regular user, [100,200) = moderator, &gt;=200 = admin (see {@link #getShortInfo()}/{@link #getInfo(boolean)}). */
    protected int userLevel = 0;
    /** Name of the House/faction this user currently fights for, or empty if unaffiliated. */
    protected String playerHouse;
    /** Free-text biography/description the user has set for themselves; may contain HTML including images. */
    protected String fluff;
    /** Campaign experience points. */
    protected int exp;
    /** Campaign skill rating (e.g. ELO-style); a value below 1 is treated as "hidden by server" and not displayed. */
    protected float rating;
    /** Current connection/session status; one of the {@code IClient.STATUS_*} constants. */
    protected int status;
    /** House player color as an HTML color string. */
    protected String htmlColor;
    /** House player color as an AWT {@link Color}, parsed from {@link #htmlColor} or the house's configured color. */
    protected Color rgbColor;
    /** User's reported country (for display), or "unknown". */
    protected String country;
    /** Derived convenience flag: true whenever {@link #status} corresponds to an actively-connected-to-a-campaign state. */
    protected boolean loggedIn = false;
    /** Whether this user is currently flagged as a mercenary (rather than a fixed house regular). */
    protected boolean merc = false;
    /** Whether this user is hidden from normal userlist displays. */
    protected boolean invisible = false;
    /** Name of the sub-faction (within {@link #playerHouse}) this user belongs to, if any. */
    protected String subFaction = "";

    /**
     * Empty CUser. Fields are set to blank/zero/default values; {@link #status} starts at
     * {@code IClient.STATUS_LOGGED_OUT}.
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
     * <p>
     * Parses a "~"-delimited string containing, in order: name, HTML color, country, user level, and invisible
     * flag. This only covers the "who's online" roster info; campaign-specific fields (exp, rating, house, etc.)
     * are populated later, separately, via {@link #setCampaignData(IClient, String)}. Parsing failures are caught
     * and logged, potentially leaving the object partially initialized (later fields left at their pre-parse
     * defaults).
     *
     * @param data the "~"-delimited roster entry payload from the server.
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

    /** @return the house-abbreviation tag shown next to this user's name while logged in and affiliated. */
    public String getAddon() {
        return addon;
    }

    /** Sets the house-abbreviation display tag directly. */
    public void setAddon(String addon) {
        this.addon = addon;
    }

    /** Sets the house player color as an HTML color string (does not update {@link #rgbColor}). */
    public void setHTMLColor(String color) {
        this.htmlColor = color;
    }

    /** @return the house player color as an HTML color string. */
    public String getHtmlColor() {
        return htmlColor;
    }

    /** @return the permission tier (regular/moderator/admin — see field doc). */
    public int getUserLevel() {
        return userLevel;
    }

    /** Sets the permission tier. */
    public void setUserLevel(int level) {
        this.userLevel = level;
    }

    /** @return the name of the House/faction this user fights for, or empty if unaffiliated. */
    public String getHouse() {
        return playerHouse;
    }

    /** @return the user's free-text biography/description. */
    public String getFluff() {
        return this.fluff;
    }

    /** Sets the user's free-text biography/description. */
    public void setFluff(String fluff) {
        this.fluff = fluff;
    }

    /** @return the user's campaign experience points. */
    public int getExp() {
        return exp;
    }

    /** Sets the user's campaign experience points. */
    public void setExp(int exp) {
        this.exp = exp;
    }

    /** @return the user's campaign skill rating; below 1 means the server is hiding ratings. */
    public float getRating() {
        return rating;
    }

    /** Sets the user's campaign skill rating. */
    public void setRating(float rating) {
        this.rating = rating;
    }

    /** @return true if this user is hidden from normal userlist displays. */
    public boolean isInvisible() {
        return invisible;
    }

    /** Sets whether this user is currently flagged as a mercenary. */
    public void setMercStatus(boolean merc) {
        this.merc = merc;
    }

    /** @return true if this user is currently flagged as a mercenary. */
    public boolean isMerc() {
        return merc;
    }

    /** @return the house player color as an AWT {@link Color}. */
    public Color getRGBColor() {
        return rgbColor;
    }

    /** Sets the name of the sub-faction (within the current house) this user belongs to. */
    public void setSubFactionName(String subFaction) {
        this.subFaction = subFaction;
    }

    /**
     * Parses a "#"-delimited server payload of per-user campaign data: experience, rating, status (via
     * {@link #setStatus(int)}, which also derives {@link #loggedIn}), optional fluff bio text, optional house name,
     * optional mercenary flag, and optional sub-faction name — later fields are only read if present, allowing
     * shorter payloads for less-detailed updates.
     * <p>
     * If fluff comes back as a single space or the literal string {@code "0"} (server placeholder values meaning
     * "no fluff set"), it is normalized to an empty string. Once the house name is known, this looks up the
     * matching {@link House} via {@code client.getData().getHouseByName(...)} to derive {@link #addon} (house
     * abbreviation) and {@link #rgbColor} (house player color); if no matching house is found, {@link #rgbColor}
     * falls back to black and {@link #addon} is left at its previous value (not reset). Any parsing exception is
     * caught and logged, potentially leaving the object partially updated.
     *
     * @param client the client connection, used to resolve the user's house data.
     * @param data the "#"-delimited campaign data payload.
     */
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

    /** Resets all campaign-derived fields (house affiliation, fluff, exp, rating, color) back to their defaults, and status to logged-out. */
    public void clearCampaignData() {
        addon = "";
        playerHouse = "";
        fluff = "";
        exp = 0;
        rating = 0;
        status = IClient.STATUS_LOGGED_OUT;
        rgbColor = java.awt.Color.black;
    }

    /** @return the current connection/session status ({@code IClient.STATUS_*} constant). */
    public int getStatus() {
        return status;
    }

    /**
     * Sets the connection/session status and derives {@link #loggedIn} from it. Setting status to
     * {@code STATUS_LOGGED_OUT} also wipes all campaign data via {@link #clearCampaignData()}.
     * <p>
     * Quirk: {@link #loggedIn} is only ever explicitly set to {@code true} for
     * {@code STATUS_RESERVE}/{@code STATUS_ACTIVE}/{@code STATUS_FIGHTING}. Any other non-logged-out status value
     * (e.g. an "away" or other intermediate state, if one exists) leaves {@link #loggedIn} at whatever it was
     * before this call — it is not explicitly set to {@code false} in that branch.
     *
     * @param status the new status ({@code IClient.STATUS_*} constant) to apply.
     */
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

    /** @return true if this user is currently considered logged in to a campaign (see {@link #setStatus(int)}). */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * Builds a compact HTML snippet suitable for a userlist row: name, optional "(Moderator)"/"(Admin)" suffix
     * based on {@link #userLevel}, and optional "(country)" suffix (omitted when country is "unknown"). Does not
     * include house affiliation, fluff, or exp/rating — see {@link #getInfo(boolean)} for the fuller version.
     *
     * @return an HTML string for compact display.
     */
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

    /** @return this user's display name / login handle. */
    public String getName() {
        return name;
    }

    /** Sets this user's display name / login handle. */
    public void setName(String name) {
        this.name = name;
    }

    /** @return this user's reported country, or "unknown". */
    public String getCountry() {
        return country;
    }

    /** Sets this user's reported country. */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Builds a full HTML info block for this user (e.g. for a tooltip or profile popup): name, optional house-tag
     * suffix (only while {@link #loggedIn}), moderator/admin suffix, country, and — only while logged in — exp,
     * rating (only if &gt;= 1, i.e. not hidden), house affiliation and sub-faction, and fluff bio text.
     * <p>
     * When {@code removeImages} is true and the fluff text contains an {@code <img} tag, the region from that tag's
     * start through its next {@code >} character is stripped out and replaced with the literal text
     * "(img blocked)". Quirk: only the *first* {@code <img...>} tag found is removed this way — if the fluff
     * contains multiple image tags, any additional ones after the first remain in the output untouched.
     *
     * @param removeImages if true, strip (the first) embedded image tag out of the fluff text before display.
     * @return an HTML string with this user's full display info.
     */
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
     * Comparable, for PlayerNameDialog. Don't use elsewhere. Orders users purely by case-sensitive name comparison
     * (delegates to {@link String#compareTo(String)}); ignores every other field.
     *
     * @param rhs the other user to compare against.
     * @return negative/zero/positive per {@link String#compareTo(String)} on the two users' names.
     */
    public int compareTo(@Nonnull CUser rhs) {
        return this.getName().compareTo(rhs.getName());
    }

}
