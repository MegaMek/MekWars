package server.campaign.autoresolve;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import common.GameInterface;
import megamek.common.Entity;

public class Battlefield implements GameInterface {
	
	private Hashtable<VirtualUnit, Hashtable<VirtualUnit, Integer>> distance = new Hashtable<VirtualUnit, Hashtable<VirtualUnit, Integer>>(); 
	private List<VirtualUnit> attackers;
	private List<VirtualUnit> defenders;
	private List<VirtualUnit> allUnits;
	private BattleReport battleReport;
	
	private int startingBVAttacker = 0;
	private int startingBVDefender = 0;
	
	private List<String> winners = new ArrayList<String>();
	
	public Battlefield(List<VirtualUnit> attackers,	List<VirtualUnit> defenders, BattleReport battleReport) {
		this.attackers = attackers;
		for (VirtualUnit unit: attackers){
			startingBVAttacker += unit.getUnit().getBV();
		}
		this.defenders = defenders;
		for (VirtualUnit unit: defenders){
			startingBVDefender += unit.getUnit().getBV();
		}
		this.battleReport = battleReport;
		allUnits = new ArrayList<VirtualUnit>();
		allUnits.addAll(attackers);
		allUnits.addAll(defenders);
	}
	
	public int getDistance(VirtualUnit a, VirtualUnit b){
		return distance.get(a).get(b);
	}
	
	public void setDistance(VirtualUnit a, VirtualUnit b, int newDistance){
		//Do it in both ways, so we don't have to care..
		fillDistance(a, b, newDistance);
		fillDistance(b, a, newDistance);
	}
	
	private void fillDistance(VirtualUnit a, VirtualUnit b, int newDistance){
		Hashtable<VirtualUnit, Integer> dist = distance.get(a);
		if (dist == null){
			dist = new Hashtable<VirtualUnit, Integer>();
			distance.put(a, dist);
		}
		dist.put(b, newDistance);
	}

	public List<VirtualUnit> getAttackers() {
		return attackers;
	}

	public List<VirtualUnit> getDefenders() {
		return defenders;
	}

	public List<VirtualUnit> getAllUnits() {
		return allUnits;
	}

	public BattleReport getBattleReport() {
		return battleReport;
	}

	
	public List<String> getWinners() {
		return winners;
	}

	
	public boolean hasWinner() {
		return true; //For now there is no draw..
	}

	
	public Enumeration<Entity> getDevastatedEntities() {
		//For now there is no way to retreat
		return new Vector<Entity>().elements();
	}

	
	public Enumeration<Entity> getGraveyardEntities() {
		//For now there is no way to retreat
		return new Vector<Entity>().elements();
	}

	
	public Iterator<Entity> getEntities() {
		Vector<Entity> result = new Vector<Entity>();
		for (VirtualUnit unit: getAllUnits()){
			result.add(unit.getUnit().getEntity());
		}
		return result.iterator();
	}

	
	public Enumeration<Entity> getRetreatedEntities() {
		//For now there is no way to retreat
		return new Vector<Entity>().elements();
	}
	
	protected void addWinner(String winnerName){
		winners.add(winnerName);
	}

	public int getStartingBVAttacker() {
		return startingBVAttacker;
	}

	public void setStartingBVAttacker(int startingBVAttacker) {
		this.startingBVAttacker = startingBVAttacker;
	}

	public int getStartingBVDefender() {
		return startingBVDefender;
	}

	public void setStartingBVDefender(int startingBVDefender) {
		this.startingBVDefender = startingBVDefender;
	}
	
	
	
	
}
