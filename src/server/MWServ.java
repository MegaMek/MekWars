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

package server;

//The MegaMek.NET Master Server Application
//@Author: Helge Richter (McWizard@gmx.de)
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Properties;
import java.util.SimpleTimeZone;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import org.mekwars.libpk.logging.PKLogManager;

import common.MMGame;
import common.comm.Command;
import common.comm.ServerCommand;
import common.util.MWLogger;
import server.MWChatServer.MWChatClient;
import server.MWChatServer.MWChatServer;
import server.MWChatServer.auth.IAuthenticator;
import server.campaign.CampaignMain;
import server.campaign.DefaultServerOptions;
import server.campaign.SPlayer;
import server.util.IpCountry;
import server.util.TrackerThread;

public class MWServ {

	// Static logging engine, and static version info.
    public static final String SERVER_VERSION = "0.8.0.0";


    private ServerWrapper myCommunicator;
	private Hashtable<String, MMGame> games = new Hashtable<String, MMGame>();
	private Hashtable<String, MWClientInfo> users = new Hashtable<String, MWClientInfo>();
	private Hashtable<InetAddress, Vector<MWClientInfo>> ips = new Hashtable<InetAddress, Vector<MWClientInfo>>();
	private Hashtable<InetAddress, Long> banips = new Hashtable<InetAddress, Long>();
	private Hashtable<String, String> banaccounts = new Hashtable<String, String>();
	private Hashtable<String, Long> ISPlog = new Hashtable<String, Long>();
	private Hashtable<MWClientInfo, InetAddress> iphelp = new Hashtable<MWClientInfo, InetAddress>();
	private Properties config = new Properties();
	private Hashtable<String, String> mails = new Hashtable<String, String>();
	private Hashtable<InetAddress, String> iplog = new Hashtable<InetAddress, String>();
	// private Hashtable versionsubids = new Hashtable();
	private IpCountry ipToCountry = null;
	// private String log = "";
	private CampaignMain campaign;
	private Command.Table myCommands = new Command.Table();
	private Vector<String> ignoreList = new Vector<String>(1, 1);
	private Vector<String> factionLeaderIgnoreList = new Vector<String>(1, 1);
    private TrackerThread trackerThread;
    
    private static MWLogger logger;
    
	// private boolean debug = true;

	/*
	 * List of Abreviations for the protocol used by the client only: NG = New
	 * Game (NG|<IP>|<Port>|<MaxPlayers>|<Version>|<Comment>) CG = Close Game
	 * (CG) RU = Refresh Users (RU) GB = Goodbye (Client exit) (GB) SO = Sign-On
	 * (SO|<Version>|<UserName>) Used by Both: CH = Chat Server news:(CH|<text>)
	 * Client Chat: (CH|<UserName>|<Color>|<Text>) Used only by the Server: GS =
	 * Games (GS|<MMGame.toString()>|<MMGame.toString()|...) US = Users
	 * (US|<MWClientInfo.toString()>|<MWClientInfo.toString()>|..) UR = Update
	 * Request (UR|<Text to Show>) UG = User Gone
	 * (UG|<MWClientInfo.toString>|[Gone]) Gone is used when the client didn't
	 * just change his name NU = New User (NU|<MWClientInfo.toString>|[NEW]) NEW
	 * is used the same way as GONE in UG ER = Error (Not yet used)
	 * (ER|<ErrorLevel>|<description>) CR = Campaign Result -> Just give to
	 * CampaignMain
	 */

	public static void main(String[] argv) {
		new MWServ(argv);
	}

	public static void stop() {
		System.exit(0);
	}

	MWServ(String[] argv) {

		String logFileName = "./logs/logFile.txt";
		String errorFileName = "./logs/errorFile.txt";
		logger = MWLogger.getInstance();
		createLoggers();

		try {
			// MWLogger.mainLog("Redirecting output to " +
			// logFileName);
			PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileName), 64));
			System.setOut(ps);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
			MWLogger.errLog("Unable to redirect standard output to " + logFileName);
		}

		try {
			// MWLogger.mainLog("Redirecting output to " +
			// logFileName);
			PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(errorFileName), 64));
			System.setErr(ps);
		} catch (Exception ex) {
			MWLogger.errLog(ex);
			MWLogger.errLog("Unable to redirect standard error to " + errorFileName);
		}

		MWLogger.mainLog("----- MekWars Server V " + SERVER_VERSION + " is starting up... -----");
		/*** Required to kick off the Server ***/
		MWLogger.mainLog("Loading configuration...");
		loadConfig();
		MWLogger.mainLog("Configuration loaded.");
		/*
		 * for (int i = 0; i < argv.length; i++) { if (argv[0].equals("debug"))
		 * debug = false; }
		 */
		if (Boolean.parseBoolean(getConfigParam("RESOLVECOUNTRY"))) {
			ipToCountry = new IpCountry("./data/iplist.txt", "./data/countrynames.txt");
		}

		MWLogger.mainLog("Loading mail file...");
		mails = checkAndCreateConfig("./data/mails.txt");
		MWLogger.mainLog("Mail file loaded.");
		MWLogger.mainLog("Creating new campaign environment...");
		campaign = new server.campaign.CampaignMain(this);
		MWLogger.mainLog("Environment created.");

		// Touch log files
		MWLogger.mainLog("Initializing log subsystem. Touching log files.");
		MWLogger.mainLog("Main channel log touched.");
		MWLogger.gameLog("Game log touched.");
		MWLogger.cmdLog("Command log touched.");
		MWLogger.pmLog("Private messages (PM) log touched.");
		MWLogger.bmLog("Black Market (BM) log touched.");
		MWLogger.infoLog("Server info log touched.");
		MWLogger.warnLog("Server warnings log touched.");
		MWLogger.errLog("Server errors log touched.");
		MWLogger.modLog("Moderators log touched.");
		MWLogger.tickLog("Tick report log touched.");

		// start the TrackerThread if using tracker
		this.startTracker();
		
		//start server
		MWLogger.mainLog("Entering main loop cycle. Starting the server...");
		startServer(argv);
	}

	public void startTracker() {
		if (Boolean.parseBoolean(getCampaign().getConfig("UseTracker"))) {
			
			if(this.trackerThread != null) {
				this.trackerThread.interrupt();
			}
			MWLogger.infoLog("Attempting to create TrackerThread in MWServ.");
			TrackerThread trackT = new TrackerThread(this);
			trackT.start();
			this.trackerThread = trackT;
		}
	}
	
	public void loadConfig() {
		try {
			config.load(new FileInputStream("./data/serverconfig.txt"));
		} catch (Exception e) {
			config.setProperty("INFOMESSAGE", "For MekWars project info, visit http://www.sourceforge.net/projects/mekwars");
			config.setProperty("RESOLVECOUNTRY", "true");
			config.setProperty("CAMPAIGNCONFIG", "./data/campaignconfig.txt");
			config.setProperty("DATAPORT", "4867");
			config.setProperty("SERVERIP", "-1");// this binds to all local IPs
													// in MWChatServer.java
			try {
				config.store(new FileOutputStream("./data/serverconfig.txt"), "Server config File");
			} catch (Exception e1) {
				MWLogger.errLog("config file could not be read or written, defaults will be used.");
				MWLogger.errLog(e1);
			}
		}

		loadBanPlayers();
		loadBanIP();
		loadISPs();
	}

	private void createLoggers() {
        PKLogManager logger = PKLogManager.getInstance();
        logger.addLog("infolog");
        logger.addLog(PKLogManager.ERRORLOG);
        logger.addLog("debuglog");
        logger.addLog("mainlog");
        logger.addLog("gamelog");
        logger.addLog("resultslog");
        logger.addLog("cmdlog");
        logger.addLog("pmlog");
        logger.addLog("bmlog");
        logger.addLog("warnlog");
        logger.addLog("modlog");
        logger.addLog("ticklog");
        logger.addLog("iplog");
        logger.addLog("testlog");
	}
	
	// this will just loop and take in info...
	public void startServer(String[] args) {
		if (args == null) {
			MWLogger.infoLog("Server started without parameters");
		}
		try {
			myCommunicator = ServerWrapper.createServer(this);
			myCommunicator.start();
		} catch (Exception e) {
			MWLogger.errLog("== PROBLEM STARTING SERVER WRAPPER ==");
			MWLogger.errLog(e);
		}
	}

	public Hashtable<String, String> checkAndCreateConfig(String filename) {
		try {
			File configFile = new File(filename);
			FileInputStream fis = new FileInputStream(configFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			dis.readLine();
			dis.close();
			fis.close();
		} catch (Exception ex) {
			try {
				MWLogger.infoLog("Creating new File");
				MWLogger.mainLog("Creating new File");
				if (filename.equals("./data/mails.txt")) {
					FileOutputStream out = new FileOutputStream(filename);
					PrintStream p = new PrintStream(out);
					p.println("<EOF>");
					p.close();
					out.close();
				}
			} catch (Exception e) {
				MWLogger.errLog(e);
				MWLogger.mainLog("No file named " + filename + " was found and cannot create one!");
				System.exit(1);
			}
		}
		if (filename.equals("./data/mails.txt")) {
			return getMails();
		}
		return new Hashtable<String, String>();
	}

	/*** Once a user(lPID) logs in this function is kicked off ****/
	public boolean clientLogin(String name) {
		String originalName = name;
		name = name.toLowerCase();
		InetAddress hisip = getIP(name);
		MWChatClient client = myCommunicator.getClient(name);

		
		MWLogger.ipLog("Connection from " + getIP(name) + " (" + name + ")");
		// Double account check
		// Don't worry about deds or nobodies.
		if (!originalName.startsWith("[Dedicated]") && !originalName.startsWith("Nobody")) {
			if (iplog.get(hisip) != null) {
				String logname = (String) iplog.get(hisip);
				if (!name.equals(logname)) {
					String nametmp = name;

					SPlayer a = getCampaign().getPlayer(name);
					if (a != null) {
						nametmp += " " + getCampaign().getPlayer(name).getMyHouse().getAbbreviation();
					}

					SPlayer b = getCampaign().getPlayer(logname);
					if (b != null) {
						logname += " " + getCampaign().getPlayer(logname).getMyHouse().getAbbreviation();
					}
					if ((a != null) && (b != null)) {
						if (a.getGroupAllowance() != 0 && a.getGroupAllowance() != b.getGroupAllowance()) {
							// MWLogger.modLog("Double Accounting: " +
							// nametmp + " and " + logname + " IP: " + hisip);
							getCampaign().doSendModMail("NOTE:", "Double Accounting: " + nametmp + " Group: " + a.getGroupAllowance() + " and " + logname + " Group: " + b.getGroupAllowance() + " IP: " + hisip);
						}
					} else {
						// MWLogger.modLog("Double Accounting: " +
						// nametmp + " and " + logname + " IP: " + hisip);
						getCampaign().doSendModMail("NOTE:", "Double Accounting: " + nametmp + " and " + logname + " IP: " + hisip);
					}
				}
			}
			iplog.put(hisip, name);
		}

		// Banned IP-check
		if (banips.get(hisip) != null) {
			Long until = banips.get(hisip);
			if (until.longValue() > System.currentTimeMillis() || until.longValue() == 0) {
				if (until.longValue() != 0) {
					clientSend("CH|You are banned. You may not join this server until " + new Date(until.longValue()).toString(), name);
					getCampaign().doSendModMail("NOTE:", name + " (IP: " + hisip + ") tried to gain access to the server");
				}
				try {
					Thread.sleep(125);
				} catch (Exception ex) {
				}

				myCommunicator.kill(name, "");
				return false;
			}

			// else
			banips.remove(hisip);
			bansUpdate();
		}
		// Banned Accounts name Check
		if (banaccounts.get(name.toLowerCase()) != null) {
			Long until = Long.valueOf(banaccounts.get(name.toLowerCase()));
			if (until.longValue() > System.currentTimeMillis() || until.longValue() == 0) {
				if (until.longValue() != 0) {
					clientSend("CH|You are banned. You may not join this server until " + new Date(until.longValue()).toString(), name);
					getCampaign().doSendModMail("NOTE:", name + " (IP: " + hisip + ") tried to gain access to the server");
				}
				try {
					Thread.sleep(125);
				} catch (Exception ex) {
				}
				myCommunicator.kill(name, "");
				return false;
			}

			// else
			banaccounts.remove(name);
			bansUpdate();
		}
		// Banned Accounts name Check
		if (ISPlog.containsKey(client.getClientVersion())) {
			Long until = ISPlog.get(client.getClientVersion());
			if (until > System.currentTimeMillis() || until == 0) {
				if (until.longValue() != 0) {
					clientSend("CH|You have been banned. You may not join this server until " + new Date(until.longValue()).toString(), name);
					getCampaign().doSendModMail("NOTE:", name + " (IP: " + hisip + ") tried to gain access to the server");
				}
				try {
					Thread.sleep(125);
				} catch (Exception ex) {
				}

				myCommunicator.kill(name, "");
				return false;
			}

			// else
			ISPlog.remove(name);
			bansUpdate();
		}
		int status = 2;
		MWChatClient c = myCommunicator.getClient(MWChatServer.clientKey(name));
		if (c != null) {
			status = c.getAccessLevel();
		}
		boolean invis = false;

		if (getCampaign().getPlayer(name) != null) {
			invis = getCampaign().getPlayer(name).isInvisible();
		}
		MWClientInfo newUser = new MWClientInfo(originalName, getIP(name), System.currentTimeMillis(), status, invis);
		MWLogger.infoLog(originalName + " logged in from " + getIP(name).toString() + " at " + new Date(System.currentTimeMillis()).toString());

		// Double IP Check
		if (!originalName.startsWith("[Dedicated]")) {
			if (ips.get(hisip) != null) {
				Vector<MWClientInfo> allthose = ips.get(hisip);
				StringBuilder result = new StringBuilder("Warning: " + originalName + " has the same IP as ");
				boolean allowed = true;
				int groupid = 0;
				for (int i = 0; i < allthose.size(); i++) {
					MWClientInfo user = (MWClientInfo) allthose.elementAt(i);

					if (originalName.equalsIgnoreCase(user.getName())) {
						continue;
					}

					// Check if they're allowed to do so.
					SPlayer p = getCampaign().getPlayer(user.getName());
					if (p != null) {
						// Is this the first player to check? Then set the Group
						if (i == 0) {
							groupid = p.getGroupAllowance();
						} else if (p.getGroupAllowance() != groupid || groupid == 0) {
							allowed = false;
						}
					} else {
						// One non-campaign player involved. Illegal!
						allowed = false;
					}

					result.append(user.getName());
					if (allthose.size() > i + 1) {
						result.append(" and ");
					}
				}
				if (!allowed) {
					getCampaign().doSendModMail("NOTE:", result.toString());
				}
				allthose.add(newUser);
			} else {
				Vector<MWClientInfo> allthose = new Vector<MWClientInfo>(1, 1);
				allthose.add(newUser);
				ips.put(hisip, allthose);
			}
			iphelp.put(newUser, hisip);
		}

		String clientVersion = "any " + SERVER_VERSION.substring(0, SERVER_VERSION.lastIndexOf(".")) + ".x";
		clientSend("CH|Welcome to " + getCampaign().getConfig("ServerName") + " (Server Version: " + SERVER_VERSION + ", Compatible Clients: " + clientVersion + ")", name);
		clientSend("CH|" + getConfigParam("INFOMESSAGE"), name);

		// send MMGame info for currently open hosts. future updates
		// incremental.
		for (MMGame currGame : games.values()) {
			if (currGame == null) {
				continue;
			}
			clientSend("SL|NG|" + currGame.toString(), name);
			clientSend("SL|SHS|" + currGame.getHostName() + "|" + currGame.getStatus(), name);
		}

		// send all online users to the client. future updates incremental.
		Enumeration<MWClientInfo> u = users.elements();
		String toSend = "US";
		while (u.hasMoreElements()) {
			toSend = toSend.concat("|" + u.nextElement().toString());
		}
		clientSend(toSend, name);

		if (Boolean.parseBoolean(getConfigParam("RESOLVECOUNTRY"))) {
			// Get the Country of the User (getIP returns "/127.0.0.1" so remove
			// the trailing /
			newUser.setCountry(getCountryString(getIP(name).toString().substring(1)));
		}
		users.put(name, newUser);

		/*
		 * Sending mail before the players GUi has finished constructing can
		 * hang clients. Wait for a client to send a "/getsavedmail" command,
		 * instead.
		 */
		// send him any saved mail
		// checkAndSendMail(name);
		return true;
	}

	public InetAddress getIP(String name) {
		return myCommunicator.getIP(name);
	}

	public boolean isAdmin(String username) {
		MWChatClient c = myCommunicator.getClient(MWChatServer.clientKey(username));
		if (username.startsWith("[Dedicated] ")) {
			return true;
		}
		if (c != null) {
			if (c.getAccessLevel() >= IAuthenticator.ADMIN) {
				return true;
			}
		}
		return false;

	}

	public boolean isModerator(String username) {
		MWChatClient c = myCommunicator.getClient(MWChatServer.clientKey(username));
		if (username.startsWith("[Dedicated] ")) {
			return true;
		}
		if (c != null) {
			if (c.getAccessLevel() >= IAuthenticator.MODERATOR) {
				return true;
			}
		}
		return false;
	}

	public int getUserLevel(String username) {
		MWChatClient c = myCommunicator.getClient(MWChatServer.clientKey(username));
		if (c != null) {
			return c.getAccessLevel();
		}
		return 0;
	}

	public void clientLogout(String name) {

		// name can be null if the user was never logged in
		if (name == null) {
			return;
		}

		// get the user to log out. may be null if banned.
		MWClientInfo user = getUser(name);
		if (user == null) {
			return;
		}

		InetAddress hisip = iphelp.get(user);
		if (hisip != null) {
			Vector<MWClientInfo> all = ips.get(hisip);
			if (all != null) {
				if (all.size() == 1) {
					ips.remove(hisip);
				} else {
					while (all.remove(user)) {
					}
				}
			}
			iphelp.remove(user);
		}

		campaign.getOpsManager().doDisconnectCheckOnPlayer(name);
		campaign.doLogoutPlayer(name);

		if (hisip != null) {
			sendRemoveUserToAll(name, true, hisip.toString());
		} else { 
			sendRemoveUserToAll(name, true);
		}
		MWLogger.infoLog("Client " + name + "logged out.");
		users.remove(name.toLowerCase());

		// remove his host, if he has a game open
		if (games.get(name) != null) {
			MMGame game = games.get(name);
			games.remove(name);
			doCloseGame(game);
		}

		/*
		 * Attempt to remove his name from all MMGames. If successfully removed,
		 * doRemoveUserFromGame will send an update to all users.
		 */
		for (MMGame currGame : games.values()) {
			doRemoveUserFromGame(name, currGame);
		}
	}

	public void clientSend(String msg, String name) {
		// Don't send empty messages
		if (msg != null && !msg.trim().equals("CH|")) {
			myCommunicator.sendServerMessage(msg, name);
		}
	}

	public void broadcastRaw(String msg) {
		myCommunicator.broadcastComm(msg);
	}

	/*** when the user sends information to the server this function kicks off ***/
	public void clientRecieve(String lineIn, String name) {
		StringTokenizer st = new StringTokenizer(lineIn, "|");
		try {
			String task = (String) st.nextElement();
			ServerCommand c = null;
			if ((c = (ServerCommand) myCommands.get(task.toUpperCase())) != null) {
				c.reset();
				c.setUsername(name);
				c.parseArguments(lineIn.substring(lineIn.indexOf("|") + 1));
				return;
			}

			/*
			 * Commands related to hosting.
			 */
			if (task.equals("NG")) { // new game in hosts list.
										// NG|<MMGame.toString()>

				MMGame newGame = new MMGame(st.nextToken());

				// replace localhost and 127.0.0.1
				if (newGame.getIp().equals("127.0.0.1") || newGame.getIp().equals("localhost")) {
					newGame.setIp(myCommunicator.getClient(name).getHost());
				}

				games.put(name, newGame);
				myCommunicator.broadcastComm("SL|NG|" + newGame.toString());

			} else if (task.equals("CG")) { // close game command, received from
											// player
				MMGame game = games.remove(name);
				doCloseGame(game);
			} else if (task.equals("LG")) { // player leaving game
				MMGame toUpdate = games.get(st.nextToken());
				if (toUpdate != null) {
					doRemoveUserFromGame(name, toUpdate);
				}
			} else if (task.equals("JG")) {// join a specified host. JG|HostName

				String hostName = st.nextToken();
				MMGame toUpdate = games.get(hostName);
				if (toUpdate != null) {
					if (toUpdate.getCurrentPlayers().add(name)) {
						myCommunicator.broadcastComm("SL|JG|" + toUpdate.getHostName() + "|" + name);
					}
				}

				if (hostName.startsWith("[Dedicated]")) {
					clientSend("PM|SERVER|You joined " + hostName + ". Please remember where you parked.", name);
				}

			} else if (task.equals("SHS")) { //@salient - i found this elsewhere -> Set Host Status (SHS|<GameID>|<Status>) US = Users

				MMGame toUpdate = games.get(st.nextToken());
				if (toUpdate.getHostName().startsWith("[Dedicated]") || name.equals(toUpdate.getHostName())) {
					toUpdate.setStatus(st.nextToken());
					myCommunicator.broadcastComm("SL|SHS|" + toUpdate.getHostName() + "|" + toUpdate.getStatus());
				}

			}

			else if (task.equals("CH")) {
				// CHAT
				String text = st.nextToken();

				// allow | in text
				while (st.hasMoreElements()) {
					text += "|" + st.nextToken();
				}

				// Ensure no links come from the players.. at least no harmful
				// ones)
				if (text.indexOf("href") != -1 && text.indexOf("MEKWARS") != -1 && !isAdmin(name)) {
					clientSend("CH|Message deleted (external hyperlinks not allowed).", name);
					return;
				}
				if (!text.startsWith("/") && text.length() > 450 && !isModerator(name)) {
					clientSend("CH|Message blocked by the spam protector (450 char limit).", name);
					return;
				}
				if (text.startsWith("/")) {
					text = text.substring(1);
					// StringTokenizer commandTokenizer = new
					// StringTokenizer(text);
					// String taskcommand = commandTokenizer.nextToken();

					if (text.toLowerCase().startsWith("c ")) {
						campaign.fromUser(text, name);
					} else {
						// Replace the first space with a # to mimic the /c
						// commands
						// Just prepend c+space so that it can be chopped off in
						// the
						// fromUser method --Torren
						text = "c " + text.trim().replaceFirst(" ", "#");
						campaign.fromUser(text, name);
					}

				} else {
					MWClientInfo client = getUser(name);
					if (client != null) {
						if (!ignoreList.contains(client.getName()) && !factionLeaderIgnoreList.contains(client.getName())) {
							sendChat(name + "|" + text);
						} else {
							clientSend("CH|You've been set to ignore mode and cannot participate in chat.", name);
						}
					} else {
						sendChat(name + "|" + text);
					}

				}
			} else if (task.equals("CR")) {
				String result = st.nextToken();
				MWLogger.gameLog("Starting report process by " + name);
				MWLogger.gameLog(name + " reported: " + lineIn);
				getCampaign().doProcessAutomaticReport(result, name);
			} else if (task.equals("IPU")) {// InProgressUpdate
				String result = st.nextToken();
				getCampaign().addInProgressUpdate(result, name);
			} else {
				clientSend("CH|Unknown command. Please make sure your client is up to date.", name);
				MWLogger.warnLog("Got a strange command, " + task + ", from " + name);
			}
		} catch (Exception ex) {
			// The GB doesn't arrive at the server because of the client
			// disconnecting
			if (!lineIn.equals("GB")) {
				// Most propably an out of date client. Send him the request to
				// update
				clientSend("CH|Your Client sent a false packet or caused a server error. You probably entered an illegal server command.", name);
				MWLogger.errLog("False packet/illegal command (from " + name + "):");
				MWLogger.errLog(ex);
			}
		}
	}

	public void statusMessage() {
		MWLogger.mainLog("Open Games: " + games.size());
		MWLogger.infoLog("Open Games: " + games.size());
	}

	private void doCloseGame(MMGame game) {
		if (game != null) {
			myCommunicator.broadcastComm("SL|CG|" + game.getHostName());
		}
	}

	public void retreiveISPS(Long time, String name) {

		File tempFile = new File("./data/Providers/");

		if (!tempFile.exists()) {
			return;
		}

		FilenameFilter filter = new ISPFilter();
		File[] fileList = tempFile.listFiles(filter);
		for (File newFile : fileList) {

			try {
				FileInputStream in = new FileInputStream(newFile);
				BufferedReader dis = new BufferedReader(new InputStreamReader(in));

				while (dis.ready()) {
					String player = dis.readLine();
					if (player.equalsIgnoreCase(name)) {
						String provderName = newFile.getName().substring(0, newFile.getName().lastIndexOf(".prv"));
						MWLogger.errLog("Provider: " + provderName);
						ISPlog.put(provderName, time);
						in.close();
						dis.close();
						break;
					}
				}

				dis.close();
				in.close();

			} catch (Exception ex) {
				// Do something?
			}
		}// end For
	}

	public void sendRemoveUserToAll(String name, boolean userGone, String ip) {
		if (userGone && ip != null) {
			MWLogger.infoLog(name + " left the room (IP:" + ip + ").");
		} else if (userGone) {
			MWLogger.infoLog(name + " left the room");
		}
		myCommunicator.broadcastComm("UG|" + getUser(name) + (userGone ? "|GONE" : ""));
	}
	
	public void sendRemoveUserToAll(String name, boolean userGone) {
		if (userGone) {
			MWLogger.infoLog(name + " left the room.");
		}
		myCommunicator.broadcastComm("UG|" + getUser(name) + (userGone ? "|GONE" : ""));
	}

	public void sendNewUserToAll(String name, boolean newUser) {
		myCommunicator.broadcastComm("NU|" + getUser(name) + (newUser ? "|NEW" : ""));
	}

	public void sendChat(String s) {
		myCommunicator.broadcastComm("CH|" + s);
		MWLogger.mainLog(s);
	}

	// Check for new Mail
	public void checkAndSendMail(String name) {
		if (mails.get(name.toLowerCase()) != null) {
			clientSend("PM|SERVER|You have stored mail.[<a href=\"MEKWARS/c requestservermail\">Read</a>]", name);
		}
	}

	/**
	 * Method which returns the number of users who are not Dedicated hosts or
	 * Nobodies.
	 */
	public int userCount(boolean includeDeds) {

		if (includeDeds) {
			return users.size();
		}

		// else, no deds. filter the list.
		synchronized (users) {
			int toReturn = users.size();
			for (String client : users.keySet()) {
				if (client.toLowerCase().indexOf("[dedicated]") >= 0 || client.toLowerCase().startsWith("nobody")) {
					toReturn--;// decrease count
				}
			}
			return toReturn;
		}

	}

	public Hashtable<String, MWClientInfo> getUsers() {
		return users;
	}

	public server.campaign.CampaignMain getCampaign() {
		return campaign;
	}

	public Hashtable<String, String> getMails() {
		Hashtable<String, String> result = new Hashtable<String, String>();
		try {
			File configFile = new File("./data/mails.txt");
			FileInputStream fis = new FileInputStream(configFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			String tmp = dis.readLine();
			while (!(tmp == null)) {
				StringTokenizer st = new StringTokenizer(tmp, "|");
				if (st.hasMoreElements()) {
					String name = (String) st.nextElement();
					if (st.hasMoreElements()) {
						String mail = (String) st.nextElement();
						if (result.get(name) != null) {
							result.put(name, mails.get(name) + "<br>" + mail);
						} else {
							result.put(name, mail);
						}
					}
				}
				tmp = dis.readLine();
			}

			dis.close();
			fis.close();
		} catch (Exception ex) {
			MWLogger.errLog("Problems reading mail file:");
			MWLogger.errLog(ex);
		}
		return result;
	}

	public void doWriteMailFile() {
		try {
			FileOutputStream out = new FileOutputStream("./data/mails.txt");
			PrintStream p = new PrintStream(out);
			Enumeration<String> e = mails.keys();
			while (e.hasMoreElements()) {
				String key = (String) e.nextElement();
				p.println(key + "|" + (String) mails.get(key));
			}
			p.close();
			out.close();
		} catch (Exception ex) {
			MWLogger.errLog("Problems writing mail file:");
			MWLogger.errLog(ex);
		}
	}

	public void doStoreMailToHashtable(String from, String name, String text) {
		if (name == null || text == null) {
			return;
		}

		if (text.trim().startsWith("CH|")) {
			text = text.substring(3); // get rid of chat market
		}
		if (text.trim().startsWith("FSM|")) {
			text = text.substring(4); // get rid of the full system message mark
		}

		// Add Timestamp
		String[] ids = TimeZone.getAvailableIDs(0);
		// if no ids were returned, something is wrong. get out.
		if (ids.length != 0) {
			SimpleTimeZone pdt = new SimpleTimeZone(0, ids[0]);
			Calendar now = new GregorianCalendar(pdt);
			Date currentTime = new Date();
			now.setTime(currentTime);
			text = (now.get(Calendar.MONTH) + 1) + "/" + now.get(Calendar.DATE) + " " + now.get(Calendar.HOUR) + ":" + now.get(Calendar.MINUTE) + " " + text;
		}
		if (mails.get(name.toLowerCase()) == null) {
			mails.put(name.toLowerCase(), text);
		} else {
			mails.put(name.toLowerCase(), (String) (mails.get(name.toLowerCase())) + "<br>" + text);
		}
		doWriteMailFile();
	}

	public void doStoreMail(String s, String name) {
		// MWLogger.mainLog("Debug: " + s);
		StringTokenizer st = new StringTokenizer(s, ",");
		String target = "";
		String text = "";
		if (st.hasMoreElements()) {
			target = ((String) (st.nextElement())).trim();
			if (st.hasMoreElements()) {
				text = "from " + name + ": ";
				String mailtext = "";
				while (st.hasMoreElements()) {
					mailtext = mailtext + st.nextElement();
				}
				text = text + mailtext;
				if (getUser(target).getName().equalsIgnoreCase(target)) {
					if (getUser(target).isInvis() && getUser(target).getLevel() > getUser(name).getLevel()) {
						clientSend("CH|AM:Saved mail to " + target + ".", name);
					}
					clientSend("PM|" + name + "|" + mailtext, target);
				} else {
					doStoreMailToHashtable(name, target, text);
					clientSend("CH|AM:Saved mail to " + target + ".", name);
				}

				if (campaign.getPlayer(name) != null) {
					if (campaign.getPlayer(target) != null) {
						MWLogger.pmLog(name + "[" + campaign.getPlayer(name).getMyHouse().getAbbreviation() + "] -> " + target + "[" + campaign.getPlayer(target).getMyHouse().getAbbreviation() + "]: " + mailtext);
					} else {
						MWLogger.pmLog(name + "[" + campaign.getPlayer(name).getMyHouse().getAbbreviation() + "] -> " + target + ": " + mailtext);
					}
				} else {
					MWLogger.pmLog(name + " -> " + target + ": " + mailtext);
				}

			}
		}
	}

	public String getCountryString(String ip) {

		if (ipToCountry == null) {
			return "";
		}
		if (ip.startsWith("/")) {
			ip = ip.substring(1);
		}
		String result = ipToCountry.seachIpCountry(ip);
		if (result.equals("LOC")) {
			result = "The LAN";
		}
		return result;
		/*
		 * String country = getLandForIP(ip); if (country.equals("DE")) return
		 * "Germany";
		 *
		 * // || country.length() > 7 if (country.equals("US")) return
		 * "the USA"; if (country.equals("CA")) return "Canada"; else return
		 * country;
		 */
	}

	public MWClientInfo getUser(String name) {
		if (name == null || users.get(name.toLowerCase()) == null) {
			return new MWClientInfo();
		}
		// else
		return users.get(name.toLowerCase());
	}

	public String getConfigParam(String p) {
		String res = config.getProperty(p);
		if (res != null) {
			return res;
		}
		return "-1";
	}

	public void setConfigParam(String config, String text) {
		String res = this.config.getProperty(config);
		if (res != null) {
			this.config.setProperty(config, text);
		}
	}

	public void fromCampaignMod(String txt, String username) {
		/*
		 * Enumeration e = users.elements(); boolean found = false; while
		 * (e.hasMoreElements()) { MWClientInfo usr = (MWClientInfo)
		 * e.nextElement(); if (usr.getName().equals(username)) {
		 * clientSend(txt, username); found = true; } }
		 */
		try {
			MWClientInfo usr = users.get(username.toLowerCase());
			if (usr != null) {
				clientSend(txt, username);
			} else if (txt.startsWith("CH|") || txt.startsWith("FSM|")) {
				doStoreMailToHashtable(null, username, txt);
			}
		} catch (Exception ex) {
			MWLogger.errLog(ex);
		}
	}

	public void doRemoveUserFromGame(String name, MMGame mygame) {
		if (mygame.getCurrentPlayers().remove(name)) {
			myCommunicator.broadcastComm("SL|LG|" + mygame.getHostName() + "|" + name);
		}
	}

	/**
	 * Method to update the two ban files (to be used everytime bans are
	 * updated, to keep the file in sync with the in-mem status)
	 */
	public void bansUpdate() {
		try {
			// Updating ban file for account names
			FileOutputStream out = new FileOutputStream("./data/accountbans.txt");
			PrintStream p = new PrintStream(out);
			for (Enumeration<String> e = banaccounts.keys(); e.hasMoreElements();) {
				Object q = e.nextElement();
				p.println(q + "=" + banaccounts.get(q));
			}
			p.close();
			out.close();
			// Updating ban file for IP addresses
			out = new FileOutputStream("./data/ipbans.txt");
			p = new PrintStream(out);
			for (Enumeration<InetAddress> e = banips.keys(); e.hasMoreElements();) {
				Object q = e.nextElement();
				p.println(q + "=" + banips.get(q));
			}
			p.close();
			out.close();
			// Updating ISP List
			out = new FileOutputStream("./data/isps.txt");
			p = new PrintStream(out);
			for (String key : ISPlog.keySet()) {
				p.println(key + "=" + ISPlog.get(key));
			}
			p.close();
			out.close();
		} catch (Exception e) {
			MWLogger.errLog("Problem updating ban files:");
			MWLogger.errLog(e);
		}
	}

	public Vector<String> getIgnoreList() {
		return ignoreList;
	}

	public Vector<String> getFactionLeaderIgnoreList() {
		return factionLeaderIgnoreList;
	}

	public Hashtable<String, String> getServerMail() {
		return mails;
	}

	public void loadBanIP() {
		// Load the Permanently Banned IP'S
		try {
			File banFile = new File("./data/ipbans.txt");

			// make the file, if its missing
			if (!banFile.exists()) {
				banFile.createNewFile();
			}

			FileInputStream fis = new FileInputStream(banFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()) {
				String line = dis.readLine();
				/*
				 * System.err.println("System property hostname: "+System.getProperty
				 * ("hostname")); if
				 * (line.startsWith(System.getProperty("hostname"))) { continue;
				 * // skip fake IPs }
				 */
				StringTokenizer ST = new StringTokenizer(line, "=");
				String ip = ST.nextToken();
				Long time = Long.valueOf(ST.nextToken());
				// get rid of any actual system names and just grab the IP.
				if (ip.indexOf("/") > -1) {
					ip = ip.substring(ip.indexOf("/") + 1);
				}
				InetAddress ia = InetAddress.getByName(ip);
				if (ia != null) {
					banips.put(ia, time);
					MWLogger.infoLog("Added " + line + " to the list of banned IPs");
					MWLogger.mainLog("Added " + line + " to the list of banned IP's");
				} else {
					MWLogger.warnLog("Importing IP bans; offending line: " + line);
				}
			}
			dis.close();
			fis.close();
		} catch (Exception ex) {
			MWLogger.errLog("Problems with loading IP banlist:");
			MWLogger.errLog(ex);
		}
	}

	public void loadBanPlayers() {
		// Loading the banned players file.
		try {
			MWLogger.infoLog("Loading Ban Players");
			File banFile = new File("./data/accountbans.txt");

			// make the file, if its missing
			if (!banFile.exists()) {
				banFile.createNewFile();
			}

			FileInputStream fis = new FileInputStream(banFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()) {
				String input = dis.readLine();
				StringTokenizer st = new StringTokenizer(input, "=");
				String toBan = st.nextToken().trim();
				String howLong = st.nextToken().trim();
				if ((toBan != null) && (howLong != null)) {
					banaccounts.put(toBan.toLowerCase(), howLong);
					MWLogger.infoLog("Added " + toBan + " to the banlist (for " + howLong + ")");
					MWLogger.mainLog("Added " + toBan + " to the banlist (for " + howLong + ")");
				} else {
					MWLogger.warnLog("Initial bans warning: " + toBan + " / " + howLong);
				}
			}
			dis.close();
			fis.close();
		} catch (Exception ex) {
			MWLogger.errLog("Problems reading ban file at startup!");
		}
	}

	public void loadISPs() {
		// Loading the ISP file.
		try {
			MWLogger.infoLog("Loading ISPs");
			File ispFile = new File("./data/isps.txt");

			// make the file, if its missing
			if (!ispFile.exists()) {
				ispFile.createNewFile();
			}

			FileInputStream fis = new FileInputStream(ispFile);
			BufferedReader dis = new BufferedReader(new InputStreamReader(fis));
			while (dis.ready()) {
				String input = dis.readLine();
				StringTokenizer st = new StringTokenizer(input, "=");
				String isp = st.nextToken().trim();
				Long address = Long.parseLong(st.nextToken().trim());
				if ((isp != null) && (address != null)) {
					ISPlog.put(isp.toLowerCase(), address);
					MWLogger.infoLog("Added " + isp + " to the ISP List");
					MWLogger.mainLog("Added " + isp + " to the ISP List");
				}
			}
			dis.close();
			fis.close();
		} catch (Exception ex) {
			MWLogger.errLog("Problems reading ISP file at startup!");
		}
	}

	public void killClient(String toKick, String kicker) {
		myCommunicator.kill(toKick, myCommunicator.getClient(MWChatServer.clientKey(myCommunicator.getClient(kicker))), "");
	}

	public MWChatClient getClient(String name) {
		return myCommunicator.getClient(name);
	}

	public Hashtable<MWClientInfo, InetAddress> getIPHelp() {
		return iphelp;
	}

	public Hashtable<InetAddress, Long> getBanIps() {
		return banips;
	}

	public Hashtable<String, String> getBanAccounts() {
		return banaccounts;
	}
	
	public void saveConfigs() {
        DefaultServerOptions dso = new DefaultServerOptions();
        dso.createConfig();
	}
}

class ISPFilter implements FilenameFilter {
	public boolean accept(File dir, String name) {
		return (name.endsWith(".prv"));
	}
}
