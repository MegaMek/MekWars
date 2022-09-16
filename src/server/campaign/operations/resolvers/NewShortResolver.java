package server.campaign.operations.resolvers;

import common.UnitFactory;
import server.campaign.SPlanet;
import server.campaign.operations.ShortOperation;

public class NewShortResolver {
	int gameID;
	ShortOpPlayers players;
	SPlanet planet;
	ShortOperation op;
	UnitFactory factory;
	String opName;
	
	boolean canTakeLand;
	boolean canTakeUnits;
	boolean canTakeComponents;
	boolean affectsELO;
	
	private int calculateLandExchange() {
		return 0;
	}
	
	private int calculateUnitExchange() {
		return 0;
	}
	
	private int calculateComponentExchange() {
		return 0;
	}
	
	public NewShortResolver(int gameId, SPlanet p, ShortOperation o, ShortOpPlayers sop) {
		this.gameID = gameId;
		planet = p;
		op = o;
		players = sop;
		opName = op.getName();
		
		
	}
}
