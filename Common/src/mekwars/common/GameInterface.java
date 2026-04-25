package common;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import megamek.common.Entity;

public interface GameInterface {
	List<String> getWinners();

	boolean hasWinner();

	Enumeration<Entity> getDevastatedEntities();

	Enumeration<Entity> getGraveyardEntities();

	Iterator<Entity> getEntities();

	Enumeration<Entity> getRetreatedEntities();

}
