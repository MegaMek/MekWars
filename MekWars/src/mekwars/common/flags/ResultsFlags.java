package common.flags;

import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import common.util.MWLogger;

public class ResultsFlags extends PlayerFlags {
	private Map<Integer, Integer> flagsApplyTo;
	
	public static final int APPLIESTO_ATTACKER = 1;
	public static final int APPLIESTO_DEFENDER = 2;
	
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
			return "";
		}
		toReturn.append(Integer.toString(this.flagType) + "$");
		for (int key : flagNames.keySet()) {
			String name = flagNames.get(key);
			String isTrue = Boolean.toString(flags.get(key));
			String appliesTo = Integer.toString(flagsApplyTo.get(key));
			toReturn.append(name + "#" + key + "#" + isTrue + "#" + appliesTo + "$");
		}
		return toReturn.toString();
	}
	
	/**
	 * Adds a flag to the list
	 * @param name
	 * @param id
	 * @param value
	 */
	public void addFlag(String name, int id, boolean value, boolean appliesToAttacker, boolean appliesToDefender) {
		setFlagName(id, name);
		setFlag(name, value);
		int appliesTo = 0;
		if (appliesToAttacker) {
			appliesTo += ResultsFlags.APPLIESTO_ATTACKER;
		}
		if (appliesToDefender) {
			appliesTo += ResultsFlags.APPLIESTO_DEFENDER;
		}
		flagsApplyTo.put(id, appliesTo);
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
		flagsApplyTo.remove(id);
	}
	
	public boolean flagAppliesToDefender(String name) {
		int id = getFlagKey(name);
		if (id == -1) {
			// invalid name
			return false;
		}
		int appliesTo = flagsApplyTo.get(id);
		if (appliesTo > 1) {
			return true;
		} else {
			return false;
		}
	}
	
	public boolean flagAppliesToAttacker(String name) {
		int id = getFlagKey(name);
		if (id == -1) {
			// invalid name
			return false;
		}
		int appliesTo = flagsApplyTo.get(id);
		if ((appliesTo%2) == 1) {
			return true;
		} else {
			return false;
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
		System.out.println(data);
		StringTokenizer st = new StringTokenizer(data, "$");
		while (st.hasMoreTokens()) {
    		String element = st.nextToken();
    		StringTokenizer elementToken = new StringTokenizer(element, "#");
    		String name = elementToken.nextToken();
    		Integer.parseInt(elementToken.nextToken());  // This isn't needed, but is included in the export (flag id).  Ignore it.
    		boolean value = Boolean.parseBoolean(elementToken.nextToken());
    		if (getFlagKey(name) >= 0) {
    			setFlag(name, value);
    		}
    		int appliesTo = 0;
    		if (elementToken.hasMoreTokens()) {
    			// Using the newer ResultsFlags, rather than the older
    			// PlayerFlags
    			appliesTo = Integer.parseInt(elementToken.nextToken());
    		}
    		
		}
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
	
	public ResultsFlags() {
		super();
		flagsApplyTo = new TreeMap<Integer, Integer>();
		flagType = FLAGTYPE_RESULTS;
	}
	
}
