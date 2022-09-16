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

/**
 * Represents a line in the passwd file.  All fields are public
 */
public class MWPasswdRecord {
    public String userId;
    public String passwd;
    public int access;
    public long time;
    private Long id;
    
    public MWPasswdRecord(){
    	//for serialization
    }
    
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the passwd.
	 */
	public String getPasswd() {
		return passwd;
	}
	/**
	 * @param passwd The passwd to set.
	 */
	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}
	/**
	 * @return Returns the userId.
	 */
	public String getUserId() {
		return userId;
	}
	/**
	 * @param userId The userId to set.
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}
	/**
	 * @return Returns the access.
	 */
	public int getAccess() {
		return access;
	}
	/**
	 * @return Returns the time.
	 */
	public long getTime() {
		return time;
	}
    /**
     * Constructor for convenience
     */
    public MWPasswdRecord(String userId, int access, String cryptedPasswd,long time, String logo) {
        this.userId = userId;
        this.access = access;
        this.passwd = cryptedPasswd;
        this.time = time;
    }
    
	/**
	 * @param time The time to set.
	 */
	public void setTime(long time) {
		this.time = time;
	}

	/**
	 * @param access The access to set.
	 */
	public void setAccess(int access) {
		this.access = access;
	}

}