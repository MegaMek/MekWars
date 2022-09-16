package common.campaign.clientutils;

public interface IClientConfig {

	String CONFIG_FILE = "./data/mwconfig.txt";
	String CONFIG_BACKUP_FILE = "./data/mwconfig.txt.bak";

	// Creates a new config file
	/*
	 * All this does ATM is create an empty mwconfig.txt. Lines commented out
	 * are old MMNET options that the client code supports, but which are not
	 * presented to the user in the MekWars client GUI. The vast majority are
	 * totally unused because the players don't know about them. Over time, the
	 * options will be made public or removed.
	 */
	void createConfig();

	/**
	 * Get a config value.
	 */
	String getParam(String param);

	/**
	 * Set a config value.
	 */
	void setParam(String param, String value);

	/**
	 * See if a paramater is enabled (YES, TRUE or ON).
	 */
	boolean isParam(String param);

	/**
	 * Return the int value of a given config property. Return a 0 if the
	 * property is a non-number. Used mostly by the misc. mail tab checks.
	 */
	int getIntParam(String param);

	/**
	 * Write the config file out to ./data/mwconfig.txt.
	 */
	void saveConfig();

}