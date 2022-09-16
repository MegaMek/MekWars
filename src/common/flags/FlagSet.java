package common.flags;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.BitSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Vector;

import common.util.MWLogger;

/**
 * 
 */
public class FlagSet {
	protected BitSet flags = new BitSet();
	protected Map<Integer, String> flagNames;
	protected int flagType;
	
	// Flag Types - since they load differently and all
	public static final int FLAGTYPE_PLAYER = 0;
	public static final int FLAGTYPE_RESULTS = 1;
	
	/**
	 * Adds the flag name to the map.  Used so that the SOs can 
	 * use flag names that make sense to them, rather than integers
	 * @param key
	 * @param name
	 */
	public void setFlagName(int key, String name) {
		flagNames.put(key, name);
	}
	
	/**
	 * Returns a Vector<String> of all flag names.  Used to create
	 * menus and such with the names
	 * @return Vector<String>
	 */
	public Vector<String> getFlagNames() {
		Vector<String> v = new Vector<String>();
		for (int i : flagNames.keySet()) {
			v.add(flagNames.get(i));
		}
		return v;
	}
	
	/**
	 * Returns the integer key for a given name.  Needed to map
	 * between a flag name and the actual bitset
	 * @param name
	 * @return integer key ID
	 */
	protected int getFlagKey(String name) {
		if (flagNames.size() < 1) {
			return -1;
		}
		for (int i = 0; i < flagNames.size(); i++) {
			if (flagNames.get(i).equalsIgnoreCase(name)) {
				return i;
			}
		}
		return -1;
	}
	
	/**
	 * Sets a named flag to true or false
	 * @param name
	 * @param value
	 */
	public void setFlag (String name, boolean value) {
		int flag = getFlagKey(name);
		if (flag != -1) {
			flags.set(flag, value);
		} else {
			MWLogger.errLog("Unknown Flag checked: " + name);
		}
	}
	
	/**
	 * Gets the boolean status of a named flag
	 * @param name
	 * @return
	 */
	public boolean getFlagStatus(String name) {
		int flag = getFlagKey(name);
		if (flag != -1) {
			return flags.get(flag);
		} else {
			MWLogger.errLog("Unknown Flag checked: " + name);
			return false;
		}
	}
	
	/**
	 * Loads a set of flags from a string.  This will be called only
	 * at player logon.  This should only be used to load the defaults
	 * so we can make sure all the proper flags exist.  If you're 
	 * loading personal flags, use loadPersonal() instead
	 * @see loadPersonal
	 * @param data
	 */
	public void loadDefaults(String data) {
		if (data.equalsIgnoreCase(" ")) {
			return;
		}
		// clear out the existing flags, just in case
		
		empty();
    	StringTokenizer st = new StringTokenizer(data, "$");
    	while (st.hasMoreTokens()) {
    		String element = st.nextToken();
    		StringTokenizer elementToken = new StringTokenizer(element, "#");
    		String name = elementToken.nextToken();
    		int id = Integer.parseInt(elementToken.nextToken());
    		boolean value = Boolean.parseBoolean(elementToken.nextToken());
    		addFlag(name, id, value);
    	}
	}
	
	/**
	 * Loads personally set flags from a string.  This should only
	 * be called after defaults are set, as any flags that are listed
	 * in this string that do not already exist due to defaults
	 * will be ignored.  This way, old flags that may have been
	 * deleted by the admins will not continue to hang around, but
	 * will be pruned every time a player loads.
	 * 
	 * @param data
	 */
	public void loadPersonal(String data) {
		if (data.equalsIgnoreCase(" ")) {
			return;
		}
		
		StringTokenizer st = new StringTokenizer(data, "$");
		while (st.hasMoreTokens()) {
    		String element = st.nextToken();
    		StringTokenizer elementToken = new StringTokenizer(element, "#");
    		String name = elementToken.nextToken();
    		int id = Integer.parseInt(elementToken.nextToken());  // This isn't needed, but is included in the export.  Ignore it.
    		boolean value = Boolean.parseBoolean(elementToken.nextToken());
    		if (getFlagKey(name) >= 0) {
    			setFlag(name, value);
    		}
		}
	}
	
	/**
	 * Removes all player flags
	 */
	public void empty() {
		flagNames.clear();
		flags.clear();
	}
	
	/**
	 * Adds a flag to the list
	 * @param name
	 * @param id
	 * @param value
	 */
	public void addFlag(String name, int id, boolean value) {
		setFlagName(id, name);
		setFlag(name, value);
		
		//MWLogger.debugLog("Setting flag " + name + "(id: " + id + ") to value " + value);
	}
	
	/**
	 * Clears a single flag, removing it from the names and flags
	 * @param name
	 */
	public void clearFlag(String name) {
		int id = getFlagKey(name);
		if (id == -1) {
			// invalid name
			return;
		}
		flagNames.remove(id);
		flags.clear(id);
	}
	
	/**
	 * Builds the string that is imported by load(String data) above
	 * Used server-side only, as I envision it, so I might move this
	 * method to SPlayer
	 * 
	 * @return String flag settings - name, ID, and value
	 */
	public String export() {
		StringBuilder toReturn = new StringBuilder();
		if (flagNames.size() == 0) {
			return " ";
		}
		for (int key : flagNames.keySet()) {
			String name = flagNames.get(key);
			String isTrue = Boolean.toString(flags.get(key));
			toReturn.append(name + "#" + key + "#" + isTrue + "$");
		}
		return toReturn.toString();
	}
	
	/**
	 * Saves the flags to disk.  This should be overloaded in any
	 * class that extends the FlagSet, so a simple ".save()" can be sent
	 * to the instance
	 */
	public void save(File file) {
		//File file = new File("./data/pFlags.dat");
		if (!file.exists()) {
			try{
				file.createNewFile();
			} catch (IOException e) {
				MWLogger.errLog(e);
				MWLogger.errLog("Unable to create pFlags.dat");
				return;
			}
		}
		try {
			FileWriter fstream = new FileWriter("./data/pFlags.dat");
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(export());
			out.close();
			fstream.close();
		} catch (IOException e) {
			MWLogger.errLog(e);
			MWLogger.errLog("Error saving pFlags.dat");
		}
		
	}
	
	
	/**
	 * Reads data file from disk.  This should be overloaded
	 * by any class extending FlagSet to allow for a simple
	 * .loadFromDisk() to be sent.
	 */
	public void loadFromDisk(File file) {
		try {
			//File file = new File("./data/pFlags.dat");
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String s;
			try {
				while((s = br.readLine()) != null) {
					loadDefaults(s);
				}
			} catch (IOException e) {
				MWLogger.errLog(e);
				MWLogger.errLog("Error reading pFlags.dat");
			}
		} catch (FileNotFoundException e) {
			MWLogger.errLog("No pFlags.dat. Returning");
			return;
		}
	}
	
	public int getAvailableID() {
		int toReturn = -1;
		for(int i = 0; i <= flagNames.size(); i++) {
			if (!flagNames.containsKey(i)) {
				toReturn = i;
			}
		}
		return toReturn;
	}
	
	public FlagSet() {
		flagNames = new TreeMap<Integer, String>();
	}

	public Set<Integer> getKeySet() {
		return flagNames.keySet();
	}
}
