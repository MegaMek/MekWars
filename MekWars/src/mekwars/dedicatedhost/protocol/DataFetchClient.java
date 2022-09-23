package dedicatedhost.protocol;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import com.Ostermiller.util.MD5;

import common.CampaignData;
import common.Influences;
import common.util.BinReader;
import common.util.BinWriter;

import dedicatedhost.MWDedHost;
import common.util.MWLogger;


/**
 * Calls to the data retrieving server and gets data for planets and factions
 * @author Imi (immanuel.scholz@gmx.de)
 */

public class DataFetchClient {
	
	private String hostAddr;
	private String cacheDir;
	private CampaignData data;
	private Map <Integer,Influences>changesSinceLastRefresh;
	private Date lastTimestamp = null;
    //private Date latestTimeStamp = null;
	private int dataPort = 4867;
	private Socket dataSocket = null;
    private int socketDelayTime = 2000;
    
	/**
	 * Constructor. This will not setup the connection. To actually transfer
	 * data use the get*() methods. Remember to set the host address before
	 * calling any get* methods. This cannot be set here, because 
	 * DataFetchClient is used with xstream and persistance and we want the
	 * users to change the address in the config, not the cache file.
	 */
	public DataFetchClient(int dataport, int socketDelayTime) {
		this.dataPort = dataport;
		changesSinceLastRefresh = new HashMap<Integer,Influences>();
        
        if ( socketDelayTime > 0 )
            this.socketDelayTime = socketDelayTime;
        else
            this.socketDelayTime = 2000;
        
	}
	
	/**
	 * Transfer the server configuration files. Used to
	 * set up verious portions of the GUI, determing proper
	 * Money/Flu names, and more.
	 */
	public void getServerConfigData(MWDedHost dedHost) throws IOException {
		
		/* 
		 * Look for an existing serverconfig.txt in the appropriate
		 * data dir. If it exists, MD5 is and request the MD5 of its
		 * server side analog.
		 * 
		 * If the MD5's don't match, or the local file doesnt exist,
		 * force a refresh from the data feeder.
		 */
		boolean timestampMatch = false;
		boolean localConfigExists = false;
		File localConfig = new File(cacheDir + "/campaignconfig.txt");
		
		//loacl read first
		if (localConfig.exists()) {
			
			//note that the config exists
			localConfigExists = true;
			
			//get the local timetamp
			String localConfigTimestamp = "";
			
			try {
				FileInputStream in = new FileInputStream(cacheDir + "/campaignconfig.txt");
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String tempTime = br.readLine();
				br.close();
				in.close();
				
				localConfigTimestamp = tempTime.substring(11);//remove "#Timestamp="
			} catch (Exception e) {
				MWLogger.errLog("Problems reading timestamp from local configuration.");
			}
			
			//now get the Server MD5
			String serverConfigTimestamp = "error";
			try {
				BinReader in = openConnection("ConfigTimestamp");
				serverConfigTimestamp = in.readLine("ConfigTimestamp");
			} catch (Exception e) {
				MWLogger.errLog("Problems connecting to server to get config timestamp.");
			}
			
			MWLogger.errLog("Local Config: " + localConfigTimestamp + " Server Config: " + serverConfigTimestamp);	
			if (localConfigTimestamp.equals(serverConfigTimestamp)) {
				timestampMatch = true;
				try {
					FileInputStream configFile = new FileInputStream(cacheDir + "/campaignconfig.txt");
					dedHost.serverConfigs.load(configFile);
					configFile.close();
				} catch (Exception ex) {
					timestampMatch = false;
				}
			}
			
		}//end if(localConfigExists, get MD5)
		
		/*
		 * If the config is missing, or the timestamps dont match, update
		 */
		if (!timestampMatch || !localConfigExists) {
			
			//delete the old file, if it exists
			if (localConfigExists) {
				File f = new File(cacheDir + "/campaignconfig.txt");
				f.delete();
			}
			
			//open the connection to the server, and write out the config
			try {
				
				BinReader in = openConnection("ServerConfig");
				FileOutputStream fops = new FileOutputStream(cacheDir + "/campaignconfig.txt");
				PrintStream out = new PrintStream(fops);
				
				//keep reading until there is an error.
				try {
					while(true){out.println(in.readLine("ConfigLine"));}
				} catch (Exception e) {
					
					//close the streams
					//in.close();
					out.close();
					fops.close();
					
					//read in the "complete" config
					try {
						FileInputStream configFile = new FileInputStream(cacheDir + "/campaignconfig.txt");
						dedHost.serverConfigs.load(configFile);
						configFile.close();
					} catch (Exception ex) {
						MWLogger.errLog(ex);
					}
				}//end catch for read-in
			} 
			
			//failed to open connection. try to load local defaults.
			catch (Exception exe) {
                if ( !dedHost.getConfig().isParam("DEDICATED") ){
    				Object[] options = { "Exit", "Continue" };
    				int selectedValue = JOptionPane.showOptionDialog(
    						null,"No campaignconfig.txt. This usually means that you were unable to\n\r" +
    						"connect to the server to fetch a copy. Do you wish to exit?","Startup\n\r" +
    						"error!",JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE,null,options,options[0]);
    				if (selectedValue == 0)
    					System.exit(0);//exit, if they so choose
                }
			}//end catch(Connection Failure)
			
		}//end if(!md5Match || !localConfigExists)
	}
	
	/**
     * Check Server version against client if it doesn't match you can't connect
     *
     */
    public void checkServerVersion(MWDedHost dedHost) throws IOException {

        boolean mustUpdate = false;
        String clientVersion = MWDedHost.CLIENT_VERSION;
 
        clientVersion = clientVersion.substring(0,clientVersion.lastIndexOf("."));
        
        BinReader binreader = openConnection("ServerVersion");
        String serverVersion = binreader.readLine("ServerVersion");
        
        serverVersion = serverVersion.substring(0,serverVersion.lastIndexOf("."));
        
        MWLogger.errLog("Client Version: "+clientVersion+" Server Version: "+serverVersion);
        mustUpdate = !serverVersion.equalsIgnoreCase(clientVersion);
        
        //If the versions dont match then the client has to update anyways
        if ( !mustUpdate ){
            binreader = openConnection("ForceUpdateKey");
            String forceUpdateKey = binreader.readLine("ForceUpdateKey");
            String clientUpdateKey = dedHost.getConfigParam("UPDATEKEY");
        
            MWLogger.errLog("Server Key: "+forceUpdateKey);
            //the server update key starts out blank. So the update only works
            //after a key is set server side.
            if ( forceUpdateKey.trim().length() > 1 )
                mustUpdate = !forceUpdateKey.equals(clientUpdateKey);
        }
        
        if ( mustUpdate ){
            int update = JOptionPane.NO_OPTION;
            if ( !dedHost.isDedicated() ){
                update = JOptionPane.showConfirmDialog(null,"You have an invalid version\n\rof the MekWars Client\n\rWould you like to update now?","Invalid Client update now!",JOptionPane.YES_NO_OPTION);
            
                if ( update == JOptionPane.YES_OPTION ){
                    try{
                        dedHost.goodbye();
                        Runtime runtime = Runtime.getRuntime();
                        String[] call = {"java","-jar","./MekWarsAutoUpdate.jar","PLAYER"};
                        runtime.exec(call);
                        MWLogger.errLog("Starting Update!");
                    }catch(Exception ex){
                        MWLogger.errLog(ex);
                    }
                }
                System.exit(0);

            }else{//is Ded
                try{
                    /*dedHost.stopHost();
                    dedHost.goodbye();
                    Runtime runtime = Runtime.getRuntime();
                    String[] call = {"java","-jar","MekWarsAutoUpdate.jar","DEDICATED"};
                    runtime.exec(call);*/
                    
                    if ( Integer.parseInt(dedHost.getConfigParam("MAXPLAYERS")) > 0  ){
                        dedHost.getConfig().setParam("MAXPLAYERS", "0");
                        dedHost.getConfig().saveConfig();
                        dedHost.setConfig();
                        dedHost.resetGame();
                        dedHost.stopHost();
                        dedHost.startHost(true, false, false);
                        dedHost.sendChat(MWDedHost.PROTOCOL_PREFIX + "c mm# I need to be updated manually!");
                    }
   
                }catch(Exception ex){
                    MWLogger.errLog(ex);
                }

            }
        }
    }

    /**
	 * Transfer the server configuration files. Used to
	 * set up verious portions of the GUI, determing proper
	 * Money/Flu names, and more.
	 */
	public void checkForMostRecentOpList() throws IOException {
		
		/* 
		 * Look for an existing OpList.txt in the appropriate
		 * data dir. If it exists, MD5 it and request the MD5 of
		 * its server side analog.
		 * 
		 * If the timestamps don't match, force a refresh from the
		 * data feeder.
		 */
		boolean timestampMatch = false;
		File localList = new File(cacheDir + "/OpList.txt");
		if (localList.exists()) {
			
			//get the local timetamp
			String localListTimestamp = "";
			
			try {
				FileInputStream in = new FileInputStream(cacheDir + "/OpList.txt");
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String tempTime = br.readLine();
				br.close();
				in.close();
				
				localListTimestamp = tempTime.substring(11);//remove "#Timestamp="
			} catch (Exception e) {
				MWLogger.errLog("Problems reading timestamp from local OpList.");
			}
			
			//now get the server list's timestamp ...
			BinReader in = openConnection("OpListTimestamp");
			String serverListTimestamp = in.readLine("OpListTimestamp");
			MWLogger.errLog("Local OpList: " + localListTimestamp + " Server OpList: " + serverListTimestamp);	
			if (localListTimestamp.equals(serverListTimestamp))
				timestampMatch = true;

		}//end if(localList.exists)
		
		/*
		 * If the MD5s dont match, update
		 */
		if (!timestampMatch) {
			
			//delete the old file, if it exists
			File f = new File(cacheDir + "/OpList.txt");
			if (f.exists())
				f.delete();
			
			//open the connection to the server, and write out the list
			try {
				
				BinReader in = openConnection("OpList");
				FileOutputStream fops = new FileOutputStream(cacheDir + "/OpList.txt");
				PrintStream out = new PrintStream(fops);
				try {
					
					//keep reading new lines until there is an error.
					while (true){
						out.println(in.readLine("ListLine"));
                    }
				
				} catch (Exception e) {
					
					//close the streams
					////in.close();
					out.close();
					fops.close();
					
				}
			} catch (Exception exe) {
				MWLogger.errLog(exe);
			}
		}//end if(!md5Match)
	}//end getOpListMD5
	
	/**
	 * Transfer trait data. Used to generate the
	 * Trait dialogs in Help menu.
	 * 
	 * Regardless of the data sent, if there is a
	 * 0% chance for meks to get the trait skill
	 * the help menu will not be shown.
	 * @see CMainFrame.java
	 */
	public void getServerTraitFiles() throws IOException {
		
		try {
			//MMClient.MWDedHostLog.clientErrLog("- opening connection to datafeed. requesting Trait Files");
			BinReader in = openConnection("ServerTrait");
			
			//keep reading until there is an error.
			try {
				while(true){
					String faction = in.readLine("TraitLine");
					int count = in.readInt("TraitLine");
					FileOutputStream fops = new FileOutputStream(cacheDir + "/"+faction.toLowerCase()+"traitnames.txt");
					PrintStream out = new PrintStream(fops);
					for ( ;count > 0; count--)
						out.println(in.readLine("TraitLine"));
                    out.flush();
					out.close();
                    fops.flush();
					fops.close();
				}
			} catch (Exception e) {
				
				//close the streams
				//in.close();
			}
		} catch (Exception ex){
			MWLogger.errLog(ex);
		} 
		
	}
	
	public String getServerMegaMekGameOptionsMD5(){
		String result = "";
		try{
			BinReader in = openConnection("ServerMegaMekGameOptionsMD5");
			result =  in.readLine("ServerMegaMekGameOptionsMD5");
		}catch (Exception ex){
			MWLogger.errLog("Error retriving MD5 for game options");
			MWLogger.errLog(ex);
		}
		return result;
	}
	/**
	 * Transfer the server game options for MegaMek.
	 */
	public void getServerMegaMekGameOptions() throws IOException {
		
		File localGameOptions = new File("./mmconf");
		try {
            if ( !localGameOptions.exists() ){
                localGameOptions.mkdir();
            }
            localGameOptions = new File("./mmconf/gameoptions.xml");
			if (localGameOptions.exists()) {
				
				MWLogger.errLog("- local gameoptions.xml exists. checking MD5.");
				
				//try to connect
				try {
									
					//get the local MD5
					String localOptionsMD5 = MD5.getHashString(localGameOptions);
					
					//now get the Server MD5
					String ServerMegaMekGameOptionsMD5 = this.getServerMegaMekGameOptionsMD5();
					
					if (localOptionsMD5.equals(ServerMegaMekGameOptionsMD5)){
						MWLogger.errLog("- MD5 matches leaving alone.");
						return;
					}
				}catch(Exception ex){
					MWLogger.errLog("- Error checking gameoptions.xml");
				}
			}
			
			MWLogger.errLog("- MD5 mismatch. Pulling gameoptions.xml from the server!");
			//MMClient.MWDedHostLog.clientErrLog("- opening connection to datafeed. requesting Trait Files");
			BinReader in = openConnection("ServerMegaMekGameOptions");
			FileOutputStream fops = null;
			PrintStream out = null;
			//keep reading until there is an error.
			try {
				File options = new File("./mmconf/gameoptions.xml");
				fops = new FileOutputStream(options);
				out = new PrintStream(fops);
				while (true){
					String tempString =in.readStringLine("GameOption");
					if ( tempString.equals("NoFileFound")){
						out.close();
						fops.close();
						//in.close();
						options.delete();
						return;
					}
					System.err.println(tempString);
					out.println(tempString);
				}
			} catch (Exception e) {
				//close the streams
				out.close();
				fops.close();
				//in.close();
			}
		} catch (Exception ex){
			MWLogger.errLog(ex);
		} 
		
	}
	
	/**
	 * Transfer the whole planet data xml.
	 */
	public CampaignData getAllData() throws IOException {
		BinReader in = openConnection("All");
		CampaignData data = new CampaignData(in);
		//in.close();
		this.data = data;
        
		store();
		
		return data;
	}
	
	/**
	 * Transfer the data from cache.
	 */
	public CampaignData getCacheData(String cachePath) throws IOException {
		BinReader in = new BinReader(new FileReader(cachePath+"/data.dat"));
		CampaignData data = new CampaignData(in);
		in.close();
		this.data = data;
		store();
		
		return data;
	}
	
	/**
	 * Transfer the Access levels of all the commands
	 * but only save the ones that matchs the users.
	 * 
	 * @author Torren (Jason Tighe)
	 */
	public boolean getAccessLevels(CampaignData Data) {
		try {
			BinReader in = openConnection("CommandAccessLevels");
			
			Data.importAccessLevels(in);
			//in.close();
		} catch (IOException e) {
			MWLogger.errLog(e);
			return false;
		} catch (RuntimeException e) {
			MWLogger.errLog(e);
			return false;
		}
		return true;
	}
	
    private BinReader openConnection(String cmd) throws IOException {
        return openConnection(cmd,socketDelayTime);
    }
    
	/**
	 * Open a connection to the server.
	 * @return
	 */
	private BinReader openConnection(String cmd, int timeout) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        MWLogger.infoLog("Command: "+cmd);
        if ( dataSocket == null
                || dataSocket.isClosed() 
                || dataSocket.isInputShutdown()
                || dataSocket.isOutputShutdown() ){
            this.closeDataConnection();
            MWLogger.infoLog("Trying to connect to "+hostAddr+" at port "+dataPort);
            dataSocket = new Socket(hostAddr, dataPort);
            dataSocket.setKeepAlive(true);
        }else{//clean out any old data first.
            dataSocket.getOutputStream().flush();
        }
        dataSocket.setSoTimeout(timeout);
		BinWriter out = new BinWriter(new PrintWriter(dataSocket.getOutputStream()));
		out.println(cmd, "cmd");
		if (lastTimestamp == null)
			out.println("", "lasttimestamp");
		else {
			MWLogger.infoLog("writing timestamp "+sdf.format(lastTimestamp));
			out.println(sdf.format(lastTimestamp), "lasttimestamp");
		}
		out.flush();
        BinReader in = null;
        try {
             in = new BinReader(new InputStreamReader(dataSocket.getInputStream()));
             //lastTimestamp = 
             sdf.parse(in.readLine("lasttimestamp"));
		} catch (ParseException e) {
			MWLogger.errLog(e);
			MWLogger.infoLog("Timestamp could not be parsed.. left unchanged.");
		}catch (SocketException se){
			MWLogger.errLog("Socket Exception Error: DataFetchClient");
			MWLogger.errLog(se);
            this.closeDataConnection();
            return openConnection(cmd, timeout);
        }catch ( NullPointerException NPE){
            this.closeDataConnection();
            return openConnection(cmd, timeout);
        }
		return in;
	}
	
	/**
	 * @param hostAddr The hostAddr to set.
	 */
	public void setData(String hostAddr, String cacheDir) {
		this.hostAddr = hostAddr;
		this.cacheDir = cacheDir;
	}
	
	/**
	 * Store itself to disk.
	 */
	public void store() {
        
        if ( lastTimestamp != null ){
    		try {
    			FileWriter fw = new FileWriter(cacheDir+"/dataLastUpdated.dat");
                //write the time out in Milliseconds
                //lastTimestamp = latestTimeStamp;
    		
                fw.write(Long.toString(lastTimestamp.getTime()));
                fw.close();
    		} catch (IOException e) {
    			MWLogger.errLog(e);
    		}
        }
		try {
			BinWriter binOut = new BinWriter(new PrintWriter(new FileWriter(cacheDir+"/data.dat")));
			data.binOut(binOut);
			binOut.close();
		}
		catch (Exception ex)
		{
			MWLogger.errLog(ex);
			MWLogger.errLog("Error saving data.");
		}
	}
	
	/**
	 * @return Returns the changesSinceLastRefresh.
	 */
	public Map<Integer,Influences> getChangesSinceLastRefresh() {
		return changesSinceLastRefresh;
	}
	
	/**
	 * @param lastTimestamp The lastTimestamp to set.
	 */
	public void setLastTimestamp(Date lastTimestamp) {
		this.lastTimestamp = lastTimestamp;
	}
    
    public void closeDataConnection(){
        try{
            if ( dataSocket == null )
                return;
            MWLogger.infoLog("Closing Socket.");
            dataSocket.shutdownInput();
            dataSocket.shutdownOutput();
            dataSocket.close();
            dataSocket = null;
        }catch(Exception ex){
            MWLogger.errLog(ex);
            dataSocket = null;
        }
        
    }
	
}
