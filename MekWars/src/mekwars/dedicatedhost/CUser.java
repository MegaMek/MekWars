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

package dedicatedhost;

import java.util.StringTokenizer;

import common.campaign.clientutils.IClientUser;
import common.util.MWLogger;

/*
 * Class for User objects held in userlist
 */

public class CUser implements Comparable<Object>, IClientUser {

	protected String Name;

	protected int Userlevel = 0;

	/**
	 * Empty CUser.
	 */
	public CUser() {
		Name = "";
	}

	/**
	 * New CUser w/ data. Called NU|MWDedHostInfo.toString()|NEW/NONE command.
	 */
	public CUser(String data) {

		StringTokenizer ST = null;

			ST = new StringTokenizer(data, "~");
		try {
			Name = ST.nextToken();
			ST.nextToken();
			ST.nextToken();
			Userlevel = Integer.parseInt(ST.nextToken());
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

	public void setUserlevel(int tlevel) {
		Userlevel = tlevel;
	}

	public int getUserlevel() {
		return Userlevel;
	}

	public void clearCampaignData() {
	}

	/**
	 * Comparable, for PlayerNameDialog. Don't use elsewhere =)
	 */
	public int compareTo(Object o) {
		if (!(o instanceof CUser))
			return 0;

		CUser u = (CUser) o;
		return this.getName().compareTo(u.getName());
	}

}