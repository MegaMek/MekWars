/*
 * Copyright (c) 2000 Lyrisoft Solutions, Inc.
 * Used by permission
 */

/*
 * Derived from MegaMekNET (http://www.sourceforge.net/projects/megameknet)
 * 
 * Changes to IAuthenticator made by Helge Richter and MMNET
 * developers. 2002-2003.
 */

package server.util;

import java.io.IOException;

import common.util.MWLogger;
import server.MWChatServer.commands.ICommands;
import server.MWChatServer.translator.jcrypt;
import server.campaign.CampaignMain;
import server.campaign.SPlayer;

/*
 * Modified 2/26/2003 by Jonathan Ellis
 * - rereading file into memory is nice b/c it allows
 * users to be manually added externally while server is running,
 * but it's prohibitively slow past a few thousand users.
 * changed to only read once.  Now that PasswdAuthenticator
 * will auto-add users, the old feature really isn't necessary.
 * - removed some unnecessary synchronization.  Remember
 * Hashtable synchronizes automatically.
 */

/**
 * Represents a unix-style passwd file with three colon-delimited fields:
 * <li>userId (String)
 * <li>access level (int)
 * <li>crypted password (String)
 */

public class MWPasswd implements ICommands{
    
    public static String getUserId(String target)
    {
        SPlayer player = CampaignMain.cm.getPlayer(target);
        
        if ( player == null )
            return null;
            
    	return player.getName();
    }    
    public static final MWPasswdRecord getRecord(String userId){
        SPlayer player = CampaignMain.cm.getPlayer(userId);
        
        if ( player == null ){
        	//MWLogger.errLog("Player is null");
        	return null;
        }
        
        if ( player.getPassword() == null ){
            //MWPasswd.reloadFile();
        	//MWLogger.errLog("password is null");
            return null;
        }
        //else
    	return player.getPassword(); 
    }
    
    /**
     * Gets a PasswdRecord.
     *
     * @param userId the user Id
     * @param password the password in plaintext
     * @return the PasswdRecord or null if the user was not found
     * @throw AccessDenied if the user was found, but his password did not match the contents
     *                        of the passwd file.
     */
    public static final MWPasswdRecord getRecord(String userId, String password)
        throws IOException, Exception
    {
    	MWPasswdRecord r;
       	r = getRecord(userId.toLowerCase());

        if (r == null) {
        	//MWLogger.errLog("r is null");
            return null;
        }
        if (password == null) {
            password = "";
        }
        if (password.length() < 2) {
            MWLogger.infoLog("Access denied: " + userId);
            throw new Exception(userId);
        }

        try{
	        String salt = r.passwd.substring(0, 2);
	        if (jcrypt.crypt(salt, password).equals(r.passwd)) {
	        	//r.setTime(System.currentTimeMillis());
	        	//writeRecord(r,userId);
	            return r;
	        }
        }catch(Exception ex){
        	return null;
        }
        
        //else
        MWLogger.errLog("Access denied: " + userId);
        throw new Exception(ACCESS_DENIED);
    }

    /**
     * Write a PasswdRecord to the passwd file.
     * If an line already existed for the user specified, it gets overwritten, otherwise,
     * it is appended.
     *
     * @param r the record to write
     */
    public static final void writeRecord(MWPasswdRecord r,String userId) throws IOException {
        SPlayer player = CampaignMain.cm.getPlayer(userId);
        
        if ( player == null )
            return;
        
        player.setPassword(r);
    }
    
    public static final void removeRecord(String userid) {
        SPlayer player = CampaignMain.cm.getPlayer(userid);
        
        if ( player == null )
            return;
        
        if ( player.getPassword() != null )
            player.setPassword(null);
            
    }

    /**
     * Write a new entry to the passwd file.  If an entry already exists for the given userId,
     * it gets overwritten, otherwise, it is appended.  The password specified here gets encrypted.
     *
     * @param userId the user Id
     * @param access the access level
     * @param passwd the plaintext password that will get encrypted
     */
    public static final void writeRecord(String userId, int access, String passwd)
        throws IOException
    {
        SPlayer player = CampaignMain.cm.getPlayer(userId);
        
        if ( player == null ){
        	MWLogger.errLog("writeRecord::Player is null");
            return;
        }
        
    	MWPasswdRecord r = new MWPasswdRecord(userId, access, passwd,System.currentTimeMillis(),"");
        String salt = String.valueOf(System.currentTimeMillis());
        int len = salt.length();
        salt = salt.substring(len-2, len);
        r.passwd = jcrypt.crypt(salt, passwd);
        player.setPassword(r);
        //writeRecord(r);
    }

    /**
     * Save the in-memory Hashtable of PasswdRecords out to disk.
     */
    public  synchronized static final void save() throws IOException {
/*        PrintWriter out = null;

        try {
            out = new PrintWriter(new FileWriter(getPasswdFileName()));
            for (Enumeration e = records.elements(); e.hasMoreElements(); ) {
                saveRecord(out, (MWPasswdRecord)e.nextElement());
            }
        }
        finally {
            if (out != null) {
                out.close();
            }
        }*/
    }

    /**
     * Load the whole passwd file into memory, as a Hashtable of PasswdRecords
     */
	static {
     reloadFile();
    }

	public static void reloadFile(){
/*		records = new Hashtable<String,MWPasswdRecord>();

		BufferedReader reader = null;
		int lineno = 1;
		try {
			InputStream is = ResourceLoader.getResource("./conf/nfc.passwd");
			reader = new BufferedReader(new InputStreamReader(is));
			String s;
			while ((s = reader.readLine()) != null) {
				MWPasswdRecord r = parseRecord(s);
				records.put(r.userId.toLowerCase(), r);
			}
			lineno++;
		}
		catch (FileNotFoundException FNFE) {
		}
		catch(Exception ex){
            MWLogger.errLog("Error reading passwd file, line " + lineno);
            MWLogger.errLog(ex);
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (Exception e) {
                    MWLogger.errLog(e);
				}
			}
		}
        return;*/
	}
	
	public static void main(String[] args) {
		try {
			writeRecord(args[0], Integer.parseInt(args[1]), args[2]);
		}
		catch (IOException e) {
			MWLogger.errLog("An I/O error occurred: " + e.getMessage());
		}
		catch (Exception e) {
			showUsageAndExit();
		}
	}

	private static final void showUsageAndExit() {
		MWLogger.errLog("Passwd Program.  Adds new line to the passwd file, encrypting the password.");
		MWLogger.errLog("usage: java com.lyrisoft.chat.server.remote.auth.Passwd " +
						   "[user id] [access level] [password] [timeoflastuse]");
		System.exit(1);
	}

    /*public static String getUserId(String target)
    {
       MWPasswdRecord record = (MWPasswdRecord)records.get(target.toLowerCase());
       
       if ( record == null )
           return null;
       
       return record.userId;
    }*/
    

}
