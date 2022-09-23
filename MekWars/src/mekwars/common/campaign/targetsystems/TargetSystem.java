package common.campaign.targetsystems;

import java.util.Vector;

import common.util.MWLogger;
import megamek.common.Entity;

public class TargetSystem {

	public final static int TS_TYPE_STANDARD = 0;
	public final static int TS_TYPE_ANTIAIR = 1;
	public final static int TS_TYPE_SHORT = 2;
	public final static int TS_TYPE_MEDIUM = 3;
	public final static int TS_TYPE_LONG = 4;
	
	public final static int TS_TYPE_MAX = 4;
	
	private Entity entity;
	private int currentType = TS_TYPE_STANDARD;
	
	public void setTargetSystem(int type) throws TargetTypeOutOfBoundsException, TargetTypeNotImplementedException {
		if (type < 0 || type > TS_TYPE_MAX) {
			throw new TargetTypeOutOfBoundsException(type);
		}
		if (type == TS_TYPE_ANTIAIR) {
			setTargetSystem("anti_air", true);
			setTargetSystem("imp_target_short", false);
			setTargetSystem("poor_target_short", true);
			setTargetSystem("imp_target_med", false);
			setTargetSystem("poor_target_med", false);
			setTargetSystem("imp_target_long", false);
			setTargetSystem("poor_target_long", false);
			currentType = TS_TYPE_ANTIAIR;
		} else if (type == TS_TYPE_STANDARD) {
			setTargetSystem("anti_air", false);
			setTargetSystem("imp_target_short", false);
			setTargetSystem("poor_target_short", false);
			setTargetSystem("imp_target_med", false);
			setTargetSystem("poor_target_med", false);
			setTargetSystem("imp_target_long", false);
			setTargetSystem("poor_target_long", false);
			currentType = TS_TYPE_STANDARD;
		} else if (type == TS_TYPE_SHORT) {
			setTargetSystem("anti_air", false);
			setTargetSystem("imp_target_short", true);
			setTargetSystem("poor_target_short", false);
			setTargetSystem("imp_target_med", false);
			setTargetSystem("poor_target_med", true);
			setTargetSystem("imp_target_long", false);
			setTargetSystem("poor_target_long", true);
			currentType = TS_TYPE_SHORT;
		} else if (type == TS_TYPE_MEDIUM) {
			setTargetSystem("anti_air", false);
			setTargetSystem("imp_target_short", false);
			setTargetSystem("poor_target_short", true);
			setTargetSystem("imp_target_med", true);
			setTargetSystem("poor_target_med", false);
			setTargetSystem("imp_target_long", false);
			setTargetSystem("poor_target_long", true);
			currentType = TS_TYPE_MEDIUM;
		} else if (type == TS_TYPE_LONG) {
			setTargetSystem("anti_air", false);
			setTargetSystem("imp_target_short", false);
			setTargetSystem("poor_target_short", true);
			setTargetSystem("imp_target_med", false);
			setTargetSystem("poor_target_med", true);
			setTargetSystem("imp_target_long", true);
			setTargetSystem("poor_target_long", false);
			currentType = TS_TYPE_LONG;
		} else {
			throw new TargetTypeNotImplementedException(type);
		}
	}
	
	public String getTypeName(int type) throws TargetTypeOutOfBoundsException, TargetTypeNotImplementedException {
		if (type < 0 || type > TS_TYPE_MAX) {
			throw new TargetTypeOutOfBoundsException(type);
		} else if (type == TS_TYPE_STANDARD) {
			return "Standard";
		} else if (type == TS_TYPE_ANTIAIR) {
			return "Anti-Air";
		} else if (type == TS_TYPE_SHORT) {
			return "Short-Range";
		} else if (type == TS_TYPE_MEDIUM) {
			return "Medium-Range";
		} else if (type == TS_TYPE_LONG) {
			return "Long-Range";
		} else {
			throw new TargetTypeNotImplementedException(type);
		}
	}
	
	public int getTypeByName(String name) {
		if (name.equalsIgnoreCase("anti-air")) {
			return TS_TYPE_ANTIAIR;
		} else if (name.equalsIgnoreCase("Standard")) {
			return TS_TYPE_STANDARD;
		} else if (name.equalsIgnoreCase("Short-Range")) {
			return TS_TYPE_SHORT;
		} else if (name.equalsIgnoreCase("Medium-Range")) {
			return TS_TYPE_MEDIUM;
		} else if (name.equalsIgnoreCase("Long-Range")) {
			return TS_TYPE_LONG;
		} else {
			return 0;
		}
	}
	
	private void setTargetSystem(String type, boolean on) {
		if (entity != null)
			entity.getQuirks().getOption(type).setValue(on);
	}
	
	public void setEntity(Entity e) {
		entity = e;
	}
	
	public int getCurrentType() {
		return currentType;
	}
	
	public TargetSystem() {
		
	}

	public String getCurrentTypeName() {
		String name = "";
		try {
			name = getTypeName(currentType);
		} catch (TargetTypeOutOfBoundsException e) {
			// TODO Auto-generated catch block
			MWLogger.errLog(e);
		} catch (TargetTypeNotImplementedException e) {
			// TODO Auto-generated catch block
			MWLogger.errLog(e);
		}
		return name;
	}

	public String[] getNameArray() {
		Vector<String> names = new Vector<String>(1,1);
		
		for (int i = TS_TYPE_STANDARD; i <= TS_TYPE_MAX; i++) {
			try {
				names.add(getTypeName(i));
			} catch (TargetTypeOutOfBoundsException e) {
				MWLogger.errLog(e);
			} catch (TargetTypeNotImplementedException e) {
				MWLogger.errLog(e);
			}
		}
		String toReturn[] = new String[names.size()];
		names.toArray(toReturn);
		return toReturn;
	}

	public String[] getNonBannedNameArray(Vector<Integer> bans) {
		Vector<String> names = new Vector<String>(1,1);
		for (int i = TS_TYPE_STANDARD; i <= TS_TYPE_MAX; i++) {
			try {
				if(!bans.contains(i)) {
					names.add(getTypeName(i));
				}
			} catch (TargetTypeOutOfBoundsException e) {
				MWLogger.errLog(e);
			} catch (TargetTypeNotImplementedException e) {
				MWLogger.errLog(e);
			}
		}
		String toReturn[] = new String[names.size()];
		names.toArray(toReturn);
		return toReturn;
	}
}
