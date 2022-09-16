package common.flags;

import java.io.File;


public class PlayerFlags extends FlagSet {

	public void save() {
		File file = new File("./data/pFlags.dat");
		super.save(file);
	}
	
	public void loadFromDisk() {
		File file = new File("./data/pFlags.dat");
		super.loadFromDisk(file);
	}
	
	public PlayerFlags() {
		super();
		flagType = FLAGTYPE_PLAYER;
	}
	
	public boolean isEmpty() {
		return (flags.isEmpty());
	}

}
