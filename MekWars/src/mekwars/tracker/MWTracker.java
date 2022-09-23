/*
 * MekWars - Copyright (C) 2005
 * 
 * Original author - nmorris (urgru@users.sourceforge.net)  
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
package tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Vector;

import common.util.MWLogger;

/**
 * Starts a server which listens for information
 * from running MekWarsServers and periodically
 * generates informational webpages.
 * 
 * Final b/c threads start in constructor.
 */

public final class MWTracker {
	
	//VARIABLES
	public static final String VERSION = "0.1.0.4";
		
	private static final int listenPort = 13731;//random number ...
	private String infoFilePath = "./infofiles/";
	private String recordFilePath = "./recordfiles/";
	private String outputPath;
	
	//blocking booleans.
	private Vector<Thread> processingThreads;
	private Vector<Thread> purgingThreads;
	private Vector<Thread> htmlThreads;

	private int oneWeek = (7*24*60*60*1000);
	
	//MAIN METHOD [Create the mwtracker]
	public static void main(String[] args) {
		
		try {
			//MWTracker tracker = 
            new MWTracker();
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}
	
	//CONSTRUCTOR
	public MWTracker() {
		
		addToLog("MWTracker initiated. Version " + MWTracker.VERSION);
		
		//initialize vectors (thread safe! yay!)
		processingThreads = new Vector<Thread>(1,1);
		purgingThreads = new Vector<Thread>(1,1);
		htmlThreads = new Vector<Thread>(1,1);
		
		/*
		 * Ensure that required directories are in place. If
		 * ./infofiles and ./recordfiles do not exist, make them.
		 */
		File infDir = new File("./infofiles");
		File recDir = new File("./recordfiles");
		if (!infDir.exists())
			infDir.mkdir();
		if (!recDir.exists())
			recDir.mkdir();
		
		/*
		 * Draw the output path from path.txt
		 */
		try {
			FileInputStream in = new FileInputStream("./outputdir.txt");
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			outputPath = br.readLine();
			br.close();
			in.close();
		} catch (Exception e) {
			addToLog("No outputdir.txt. Using local folder.");
			outputPath = "./";
		}
		
		/*
		 * Create two threads. One is a blocking thread which
		 * listens for new connections from MekWarsServers. The
		 * other is a timing thread which periodically generates
		 * new webpages and savs them to a configured path.
		 */
		ListenerThread lthread = new ListenerThread(this);
		HTMLGenerationThread htmlthread = new HTMLGenerationThread(this);
		PurgeThread purgethread = new PurgeThread(this);
		addToLog("pthread, htmlthread and lthread created.");
		
		/*
		 * On start, purge old records
		 */
		purgethread.start();
		htmlthread.start();
		lthread.start();
	}
	
	//METHODS
	public boolean isProcessing() {
		if (processingThreads.size() > 0)
			return true;
		//else
		return false;
	}
	
	public boolean isPurging() {
		if (purgingThreads.size() > 0)
			return true;
		//else
		return false;
	}
	
	public boolean isGeneratingHTML() {
		if (htmlThreads.size() > 0)
			return true;
		//else
		return false;
	}
	
	public String getInfoPath() {
		return infoFilePath;
	}
	
	public String getRecordPath() {
		return recordFilePath;
	}
	
	public Vector<Thread> getProcessingThreads() {
		return processingThreads;
	}
	
	public Vector<Thread> getPurgingThreads() {
		return purgingThreads;
	}
	
	public Vector<Thread> getHTMLThreads() {
		return htmlThreads;
	}
	
	public String getReadibleTime() {
		Date d = new Date(System.currentTimeMillis());
		String dateTimeFormat1 = "MMMMMMMMMMMMMMM dd 'at' hh:mm:ss a z";
        SimpleDateFormat sdf1 = new SimpleDateFormat(dateTimeFormat1);
        return sdf1.format(d);
	}
	
	public void addToLog(String s) {
		boolean loggingEnabled = true;//Turn on if testing.
		if (loggingEnabled) {
			String fileName = "log.txt";
			try {
				FileOutputStream out = new FileOutputStream(fileName, true);
				PrintStream p = new PrintStream(out);
				p.println(s);//1st line is server name
				p.close();
				out.close();
			} catch (Exception e) {
				System.out.println("Error writing to log file!");
				return;
			}
		}
	}//end addToLog
	
	public void addToLog(Exception e) {
		boolean loggingEnabled = true;
		if (loggingEnabled) {
			String fileName = "log.txt";
			try {
				FileOutputStream out = new FileOutputStream(fileName, true);
				PrintStream p = new PrintStream(out);
				e.printStackTrace(p);
			} catch (Exception ex) {
				System.out.println("Error writing to log file!");
				return;
			}
		}
	}//end addToLog
	
	//INNER CLASSES
	/**
	 * Package inner thread. Simple blocking thread
	 * which listens for information from servers. As
	 * new connections come in sockets are created and
	 * fed to ProcessingThreads.
	 */
	class ListenerThread extends Thread {
		
		//VARIABLES
		MWTracker tracker;
		
		//CONSTRUCTOR
		public ListenerThread(MWTracker t) {
			tracker = t;
		}
		
		@Override
		public synchronized void run() {
			
			tracker.addToLog("lthread running!");
			
			/*
			 * Attempt to open a socket, then wait for
			 * incommming calls. Sys-exit if the server
			 * socket cannot be initialized.
			 * 
			 * As calls come in, spin them into seperate
			 * processing threads which handle communications
			 * over discrete sockets.
			 */
			ServerSocket server = null;
			try {
				tracker.addToLog("attempt to open a serversocket on port " + listenPort);
				server = new ServerSocket(listenPort,0,InetAddress.getLocalHost());
				while(true){//always listen for calls
					Socket sock = server.accept();
					tracker.addToLog("lthread: connection accepted from " + sock.getInetAddress() + ", being processing.");
					new ProcessingThread(sock, tracker).start();
				}
			} catch (IOException e) {
				tracker.addToLog("Server socket creation failed. Exiting.");
				System.exit(0);
			} finally {
				try {
					server.close();
				} catch (IOException e) {
					MWLogger.errLog(e);
				}
			}
			
		}//end run()    	
	}//end ListenerThread
	
	/**
	 * Package inner thread which processes information transmitted to the
	 * tracker by MekWarsServers. Servers send simple strings which note the
	 * type of information to follow with a header, and use %'s as delimits.
	 * 
	 * Types are as follows:
	 * 1) ServerStart - SS% header. Sent when a server boots. Contains vital
	 *                  information like server name and link to homepage.
	 * 2) UpdateInfo  - Overwrites select information from SS%, including the
	 *                  description and link. Server name may not be changed.
	 *                  Uses UI% as lead.
	 * 3) PhoneHome   - Most common command. Sent by server on a schedule set
	 *                  by a server operator (eg - every 10 minutes). Contains
	 *                  information re: number of players currently online, the
	 *                  number of running/recently completed games, and more.
	 *                  Uses PH% as lead.
	 */
	class ProcessingThread extends Thread {
		
		//VARIABLES
		Socket sock;
		MWTracker tracker;
		
		//CONSTRUCTOR
		public ProcessingThread(Socket s, MWTracker t) {
			sock = s;
			tracker = t;
			tracker.addToLog("- new processing thread");
		}
		
		@Override
		public synchronized void run() {
			
			String line = "";
			try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
				line = reader.readLine();
				reader.close();
				sock.close();
				tracker.addToLog("- line read, socket closed.");
			} catch (Exception e) {
				tracker.addToLog("- line read failed. terminating this processingthread.");
				return;//kill this processing attempt if cant read line
			}
			
			/*
			 * Line was read and stored, and socket is closed. Ensure
			 * that writing to the record now would not interfere with a
			 * purge or page generation by pausing until neither operation
			 * is in progress, then process the line.
			 */
			while (tracker.isGeneratingHTML() || tracker.isPurging()) {
				try {
					tracker.addToLog("- processing blocked. wait 5 seconds.");
					this.wait(5000);//5 seconds
				} catch (Exception e) {
					//do nothing on interrupt
				}
			}
			
			/*
			 * Now that we're going to start processing the line, add
			 * to the vector in order to block other operations. Note that
			 * multiple processing threads can be run simultanously as they
			 * will be writing to different files.
			 */
			//add this thread to the processing vector
			tracker.getProcessingThreads().add(this);
			tracker.addToLog("- being processing line. block other threads (" + tracker.getProcessingThreads().size() + " record(s) being processed)");
			
			//break out if there's no actual text in line
			if (line == null || line.trim().equals("")) {
				tracker.addToLog("- empty line. stop processing. unblock.");
				tracker.getProcessingThreads().remove(this);
				return;
			}
			
			//Create tokenizer and draw header
			StringTokenizer st = new StringTokenizer(line, "%");
			String lead = st.nextToken();
			
			/*
			 * If the header is SS%, this is a server starting up. Delete
			 * any old info file and replace with these values.
			 */
			if (lead.equals("SS")) {
				
				//load server name
				String serverName = st.nextToken();
				tracker.addToLog("- ServerStart from " + serverName);
				
				//put together filename
				String fileName = tracker.getInfoPath() + serverName + "_info.txt";
				
				//if an old version of the file exists, delete it
				File oldF = new File(fileName);
				if (oldF.exists()) {
					oldF.delete();
					tracker.addToLog("- deleted old info file.");
				}
				
				//Set up an output stream and generate a new info.txt
				try {
					tracker.addToLog("- attempt to write info file.");
					FileOutputStream out = new FileOutputStream(fileName);
					PrintStream p = new PrintStream(out);
					p.println(serverName);//1st line is server name
					
					//print all remaning tokens on seperate lines
					while (st.hasMoreTokens())
						p.println(st.nextToken());
					
					tracker.addToLog("- wrote info file.");
					p.close();
					out.close();
					
				} catch (Exception e) {
					tracker.addToLog("Error processing SS%");
					tracker.addToLog(e);
					tracker.getProcessingThreads().remove(this);
					return;
				}
			}//end "SS"
			
			/*
			 * If the header begins with UI%, update link and
			 * description but leave server name in-tact.
			 */
			else if (lead.equals("UI")) {
				
				//load server name and addemble filename
				String serverName = st.nextToken();
				tracker.addToLog("- UpdateInfo from " + serverName);
				String fileName = tracker.getInfoPath() + serverName + "_info.txt";
				
				try {
					
					//set up a reader and store old values
					FileInputStream in = new FileInputStream(fileName);
					BufferedReader br = new BufferedReader(new InputStreamReader(in));
					String name = br.readLine();
					String link = br.readLine();
					String vers = br.readLine();
					String desc = br.readLine();
					
					//close the reader and stream
					br.close();
					in.close();
					tracker.addToLog("- read in old info values");
					
					//overwrite link and desc with new values
					link = st.nextToken();
					desc = st.nextToken();
					tracker.addToLog("- overwrite link and desc");
					
					//delete the old file ...
					File oldF = new File(fileName);
					if (oldF.exists()) {
						tracker.addToLog("- delete the old info file");
						oldF.delete();
					}
					
					//... and then write a new one
					FileOutputStream out = new FileOutputStream(fileName);
					PrintStream p = new PrintStream(out);
					p.println(name);
					p.println(link);
					p.println(vers);
					p.println(desc);
					
					//close outstreams
					p.close();
					out.close();
					tracker.addToLog("- wrote new info file");
				} catch (Exception e) {
					tracker.addToLog("Error while processing UI%");
					tracker.addToLog(e);
					tracker.getProcessingThreads().remove(this);
					return;
				}	
			}//end "UI" 
			
			/*
			 * If the header begins with PH, substitute a timestamp 
			 * for the server name and store the record in _records.txt.
			 */
			else if (lead.equals("PH")) {
				
				/*
				 * Alas ... StringTokenizer doesn't let us simply flush
				 * remaining tokens into a single string w/ deliminters.
				 * We could do a loop and append, but for the sake of
				 * clarity (see whats in the tokens, etc) we'll store the
				 * items in strings and assemble them before writing out.
				 */
				String serverName = st.nextToken();
				tracker.addToLog("- PH% from " + serverName);
				String numOnline = st.nextToken();
				String numRunning = st.nextToken();//number of running games
				String numFinished = st.nextToken();//number of completed games
				
				//generate filename
				String fileName = tracker.getRecordPath() + serverName + "_record.txt";
				
				try {
					
					//setup an output stream
					FileOutputStream out = new FileOutputStream(fileName, true);
					PrintStream p = new PrintStream(out);
					tracker.addToLog("- opening PrintStream, append mode.");
					
					//assemble record entry
					String record = 
						System.currentTimeMillis() + "%" 
						+ numOnline + "%" 
						+ numRunning + "%" 
						+ numFinished + "%";//include trailing % for future records
					
					//print the line
					p.println(record);
					
					//close the streams
					p.close();
					out.close();
					tracker.addToLog("- wrote line. closing stream.");
					
				} catch (Exception e) {
					tracker.addToLog("Error while processing PH%");
					tracker.addToLog(e);
					tracker.getProcessingThreads().remove(this);
					return;
				}
			}//end "PH" 
			
			//processing is finished. remove from vec then return.
			tracker.getProcessingThreads().remove(this);
			return;
			
		}//end run()
	}//end ProcessingThread
	
	/**
	 * Inner thread which generates HTML pages from the information
	 * stored in ./infofiles and ./recordfiles.
	 * 
	 * Could use some better formatting ...
	 */
	class HTMLGenerationThread extends Thread {
		
		//VARIABLES
		MWTracker tracker;
		
		//CONSTRUCTOR
		public HTMLGenerationThread(MWTracker t) {
			tracker = t;
		}
		
		//METHODS
		public void ExtendedWait(long time) {
			try {
				wait(time);
			} catch (Exception ex) {
				//do not respond to waitbreaks
			}
		}//end ExtendedWait(time)
		
		public String readableTime(long elapsed) {
			
			//to return
			String result = "";
			
			long elapsedDays = (elapsed / 86400000);
			long elapsedHours = (elapsed % 86400000) / 3600000;
			long elapsedMinutes = (elapsed % 3600000) / 60000;
			
			if (elapsedDays > 0)
				result += elapsedDays + "d ";
			
			if (elapsedHours > 0 || elapsedDays > 0)
				result += elapsedHours + "h ";
			
			result += elapsedMinutes + "m";
			
			return result;
		}
		
		@Override
		public synchronized void run() {
			
			tracker.addToLog("htmlthread running!");
			
			//This continues forever ...
				
				while(true) {
					
					/*
					 * The HTML generation should be held over if there is
					 * a blocking call outstanding on one of the files.
					 */
					while (tracker.isProcessing() || tracker.isPurging()) {
						this.ExtendedWait(2000);//2 second wait
						tracker.addToLog("hthread: blocked. delaying update.");
					}
					
					/*
					 * No blocking calls, so construct the HTML and then
					 * write it out to proper path for web display.
					 * 
					 * Set up processing and purging block.
					 */
					tracker.addToLog("hthread: generating new page");
					tracker.getHTMLThreads().add(this);
					tracker.addToLog("- begin blocking");
					
					/*
					 * For now, we'll make a very simple page, and will
					 * NOT look for player peaks or add the games completed
					 */
					File infoDirectory = new File(tracker.getInfoPath());
					String[] infoNames = infoDirectory.list();
					Arrays.sort(infoNames);
					String superString = "";
					
					int length = infoNames.length;
					tracker.addToLog("- " + length + "  info files to progess.");
					
					//set up the page header and table header
					if (length > 0) {
						superString = 
							"<html>" +
							"<head>" +
							"<title>MekWars Servers</title>" +
							"<style type=\"text/css\">" +
							"<!--" +
							  "body { background-color: #849EC6; color: #2E4F8E; margin-top: 2%; margin-left: 2%; margin-right: 2%; margin-bottom: 2%; }" +
							  "td { font-family: \"Verdana\", serif; color: #FFFFFF; background-color: #849EC6; font-size: 9pt; }" +
							  "a { color: #FFFFFF; text-decoration: none; }" +
							  "a:hover { color: #2E4F8E; text-decoration: none; }" +
							"-->" +
							"</style>" +
							"</head>" + 
							
							"<body>" +
							"<table cellspacing=\"1%\" cellpadding=\"6%\" align=left border=0 width=\"100%\">" +
							"<tr><td><h1 style=\"font-size: 220%;\"><b>MekWars Servers<b></h1>" +
							"<table cellspacing=\"1%\" cellpadding=\"6%\" align=left border=0 style=\"background-color: #2E4F8E;\">" +

							"<tr>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Server</b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Version</b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Current Players</b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Current Games</b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Max Players<br></b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Total Games<br></b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Description</b>" +
							"<td STYLE=\"background-color: #2E4F8E; text-align: center;\"><b>Last Tracked</b>" +
							"</tr>";
					}
					
					//add table records
					tracker.addToLog("- begin readin loop");
					for (int i = 0; i < length; i++) {
						
						String currInfoName = infoNames[i];
						//tracker.addToLog("- reading " + currInfoName);
						
						//set up a reader and store old values
						String name = "";
						String link = "";
						String vers = "";
						String desc = "";
						try {
							FileInputStream in = new FileInputStream(tracker.getInfoPath() + currInfoName);
							BufferedReader br = new BufferedReader(new InputStreamReader(in));
							name = br.readLine();
							link = br.readLine();
							vers = br.readLine();
							desc = br.readLine();
							br.close();
							in.close();
						}
						catch (Exception e) {
							tracker.addToLog("Error while reading info file.");
							tracker.addToLog(e);
						}
						
						//what will become the table entry. Link, Name, Version setup for all here.
						String tableListing = 
							"<tr>" +
							"<td><a href=\"" + link + "\">" + name + "</a></td>" +
							"<td STYLE=\"text-align: center;\">" + vers + "</td>";
						
						File currRecordFile = new File(tracker.getRecordPath() + name + "_record.txt");
						
						//no record submission yet. in the interim, use blanks for most data, and real description
						if (!currRecordFile.exists()) {
							
							tableListing +=
								"<td STYLE=\"text-align: center;\">0</td>" + //players
								"<td STYLE=\"text-align: center;\">0</td>" + //games
								"<td STYLE=\"text-align: center;\">0</td>" + //maxp
								"<td STYLE=\"text-align: center;\">0</td>" + //tgames
								"<td STYLE=\"text-align: center;\">" + desc + "</td>" + //desc
								"<td STYLE=\"text-align: center;\">Info Only</td>" + //update
								"</tr>";
						}
						
						//record exists. parse like a crazy person!
						else {
							
							int maxPlayers = 0;
							int gamesPlayed = 0;
							int currPlayers = 0;
							int currGames = 0;
							long timestamp = 0;
							long timeDifference = 0;
						
							try {
								FileInputStream in = new FileInputStream(currRecordFile);
								BufferedReader br = new BufferedReader(new InputStreamReader(in));
								
								//loop until all lines are checked (as indicated by a null)
								StringTokenizer st = null;
								String currLine = br.readLine();
								while (currLine != null) {
									
									//set up tokenizer
									st = new StringTokenizer(currLine, "%");
									timestamp = Long.parseLong(st.nextToken());
									
									//only check remaining tokens if within the week timeframe.
									timeDifference = System.currentTimeMillis() - timestamp;
									if (timeDifference <= oneWeek) {
										
										//players online and maxplayers
										currPlayers = Integer.parseInt(st.nextToken());
										if (currPlayers > maxPlayers)
											maxPlayers = currPlayers;
										
										currGames = Integer.parseInt(st.nextToken());//games in progress
										gamesPlayed += Integer.parseInt(st.nextToken());//finished games
									}
									
									//load next line
									currLine = br.readLine();
								}//end while (lines remain to tokenize)
								
								//close the streams
								br.close();
								in.close();
							}
							catch (Exception e) {
								tracker.addToLog("Error while reading records.");
								tracker.addToLog(e);
							}
							
							/* 
							 * All lines have been processed. 
							 */
							
							//last report > 10 minutes old. replace current counts with "N/A"
							if (timeDifference > 605000) {
								tableListing +=
									"<td STYLE=\"text-align: center;\">" + "N/A" + "</td>" + //players
									"<td STYLE=\"text-align: center;\">" + "N/A" + "</td>" + //games
									"<td STYLE=\"text-align: center;\">" + maxPlayers + "</td>" + //maxp
									"<td STYLE=\"text-align: center;\">" + gamesPlayed + "</td>" + //tgames
									"<td STYLE=\"text-align: center;\">" + desc + "</td>" + //desc
									"<td STYLE=\"text-align: center;\">" + this.readableTime(timeDifference) + " ago</td>" + //update
									"</tr>";
							}
							//last report was timely. include current counts.
							else {
								tableListing +=
									"<td STYLE=\"text-align: center;\">" + currPlayers + "</td>" + //players
									"<td STYLE=\"text-align: center;\">" + currGames + "</td>" + //games
									"<td STYLE=\"text-align: center;\">" + maxPlayers + "</td>" + //maxp
									"<td STYLE=\"text-align: center;\">" + gamesPlayed + "</td>" + //tgames
									"<td STYLE=\"text-align: center;\">" + desc + "</td>" + //desc
									"<td STYLE=\"text-align: center;\">" + this.readableTime(timeDifference) + " ago</td>" + //update
									"</tr>";
							}
							
						}//end else(records file exists)
						
						superString += tableListing;
						
					}//end for(all files in info dir)
					tracker.addToLog("- end readin loop");
					
					
					/*
					 * Add the footer (or, a "No Tracks" message)
					 * to the page. 
					 */
					if (superString.trim().equals(""))
						superString = "No Tracked Servers At This Time.";
					
					else {
						
						superString += 
							
							"</table>" +
							"<tr><td><h3 style=\"font-size: 90%;\">- Table generated " + tracker.getReadibleTime() + ". Refresh every 5 min.<br>- Max/Totals include data from last 7 days only.</td></tr></h3>" +
							"<tr><td>" +
							"<a href=\"http://sourceforge.net\"><img src=\"http://sourceforge.net/sflogo.php?group_id=122002&amp;type=1\" width=\"88\" height=\"31\" border=\"0\" alt=\"SourceForge.net Logo\" /></a> " +
							"<a href=\"http://sourceforge.net/donate/index.php?group_id=122002\"><img src=\"http://images.sourceforge.net/images/project-support.jpg\" width=\"88\" height=\"31\" border=\"0\" alt=\"Support This Project\" /> </a>" +
							"</td></tr>" +
							"</body>" +
							"</html>";
						
					}
					
					/*
					 * We've been through all the files. Now
					 * delete and replace the previous file. 
					 */
					
					File f = new File(outputPath + "mwtrackerindex.html");
					if (f.exists()) {
						//tracker.addToLog("- deleting old page");
						f.delete();
					}
					
					//setup an output stream
					try {
						FileOutputStream out = new FileOutputStream(outputPath + "./mwtrackerindex.html");
						PrintStream p = new PrintStream(out);
						
						//print the line
						p.println(superString);
						
						//close the streams
						p.close();
						out.close();
						tracker.addToLog("- wrote new page");
					} catch (Exception e) {
						tracker.addToLog("Error while writing new page.");
						tracker.addToLog(e);
					}
					
					/*
					 * Remove block.
					 */
					tracker.getHTMLThreads().remove(this);
					tracker.addToLog("- removed blocking entry");
					
					/*
					 * Delay is at the end of loop in order to
					 * ensure that a page is generated when the
					 * tracker is first run.
					 */
					//pause for 1 minute
					ExtendedWait(60000);
					
				}//end while(forever)

		}
		
	}//end HTMLThread
	
	/**
	 * Inner thread which purges old lines from record files (ie -
	 * entries which are over 1 week old) to improve HTMLGeneration
	 * times. Run infrequently. 
	 */
	class PurgeThread extends Thread {
		
		//VARIABLES
		String infoPath;
		String recordPath;
		MWTracker tracker;
		
		//CONSTRUCTOR
		public PurgeThread(MWTracker t) {
			tracker = t;
		}
		
		//METHODS
		public void ExtendedWait(long time) {
			try {
				wait(time);
			} catch (Exception ex) {
				//a-splode
			}
		}//end ExtendedWait(time)
		
		@Override
		public synchronized void run() {
			
			/*
			 * Pause the thread for 5 minutes initially while the first
			 * HTML page is generated and the server socket is constructed.
			 */
			this.ExtendedWait(300000);
			
			/*
			 * Purge periodically for as long as the tracker is running.
			 * If the tracker is processing or generating a page, wait
			 * for a minute and try again. Otherwise, wait 12 hours.
			 */
			while(true) {
				
				if (tracker.isProcessing() || tracker.isGeneratingHTML())
					this.ExtendedWait(90000);
				
				/*
				 * Begin blocking
				 */
				tracker.getPurgingThreads().add(this);
				
				/*
				 * We don't care about the info files - we simply want
				 * to purge every record file and remove those files
				 * which are old enough that no record entries survive
				 * the purge.
				 */
				File recDir = new File(tracker.getRecordPath());
				String[] fileNames = recDir.list();
				
				/*
				 * Loop through all files.
				 */
				StringTokenizer st = null;
				FileInputStream in = null;
				FileOutputStream out = null;
				PrintStream ps = null;
				BufferedReader br = null;
				int numFiles = fileNames.length;
				for (int i = 0; i < numFiles; i++) {
					
					//load the working file 
					File currRecordFile = new File(tracker.getRecordPath() + fileNames[i]);
					
					//set up stream/buffer
					try {
						
						//input streams
						in = new FileInputStream(currRecordFile);
						br = new BufferedReader(new InputStreamReader(in));
						
						//output streams
						File tmpFile = new File(tracker.getRecordPath() + fileNames[i] + ".tmp");
						out = new FileOutputStream(tmpFile, true);
						ps = new PrintStream(out);							
						
						/*
						 * read in one line, to ensure that
						 * the record is not empty.
						 */
						boolean linesInTmp = false;
						String currLine = br.readLine();
						while (currLine != null) {
							
							//set up tokenizer
							st = new StringTokenizer(currLine, "%");
							
							//if the record is less than a week old, preserve
							//the entry by reprinting in tmp file.
							if (System.currentTimeMillis() - Long.parseLong(st.nextToken()) <= 604800000) {
								ps.println(currLine);
								linesInTmp = true;
							}
							
							//load the next line
							currLine = br.readLine();
						}
						
						//close all streams
						in.close();
						out.close();
						ps.close();
						br.close();
						
						//save old name, then delete the old file
						String currRecordName = currRecordFile.getName();
						currRecordFile.delete();
						
						//if the tmp had lines, rename. else, clear the records.
						if (linesInTmp) {
							tmpFile.renameTo(new File(tracker.getRecordPath() + currRecordName));
						} else {
							tmpFile.delete();
						}
						
					} catch (Exception e) {
						tracker.getPurgingThreads().remove(this);
						//returning would break permanence of thread
					}
				}
				
				/*
				 * Unblock and sleep for 12 hours
				 */
				tracker.getPurgingThreads().remove(this);
				ExtendedWait(43200000);//12 hours
				
			}//end while(forever)
		}//end run()
	}//end PurgeThread
	
}//end MWTracker class