package server.campaign.commands.admin;

import java.util.StringTokenizer;

import server.MWChatServer.auth.IAuthenticator;
import server.campaign.commands.Command;

public class AdminDestroyFactionCommand implements Command {

	int accessLevel = IAuthenticator.ADMIN;
	String syntax = "name";

	
	public int getExecutionLevel() {
		return accessLevel;
	}

	public void setExecutionLevel(int i) {
		accessLevel = i;
		}
	
	public String getSyntax() {
		return syntax;
		}

	public void process(StringTokenizer command, String Username) {
		// TODO Auto-generated method stub
		
		/*
		 * Need to:
		 * 
		 * 1) Reassign planetary points
		 * 2) Reassign players
		 * 3) Take care of house bay units
		 * 4) Reassign factories to new build tables
		 * 5) Remove subfaction affiliations
		 * 6) Remove faction / subfaction access from operations
		 * 7) Update clients to remove faction
		 * 8) Remove faction from memory
		 * 
		 */

	}


}
