package mekwars.common.flags;

import java.io.File;

public class PlayerFlags extends FlagSet {

    public PlayerFlags() {
        super();
        flagType = FLAG_TYPE_PLAYER;
    }

    public void save() {
        File file = new File("./data/pFlags.dat");
        super.save(file);
    }

    public void loadFromDisk() {
        File file = new File("./data/pFlags.dat");
        super.loadFromDisk(file);
    }

    public boolean isEmpty() {
        return (flags.isEmpty());
    }

}
