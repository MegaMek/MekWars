package mekwars.server.campaign.autoresolve;

import common.GameInterface;
import megamek.common.Entity;

public class Battlefield implements GameInterface {

    private java.util.Hashtable<VirtualUnit, java.util.Hashtable<VirtualUnit, Integer>> distance = new java.util.Hashtable<VirtualUnit, java.util.Hashtable<VirtualUnit, Integer>>();
    private java.util.List<VirtualUnit> attackers;
    private java.util.List<VirtualUnit> defenders;
    private java.util.List<VirtualUnit> allUnits;
    private BattleReport battleReport;

    private int startingBVAttacker = 0;
    private int startingBVDefender = 0;

    private java.util.List<String> winners = new java.util.ArrayList<String>();

    public Battlefield(java.util.List<VirtualUnit> attackers, java.util.List<VirtualUnit> defenders,
          BattleReport battleReport) {
        this.attackers = attackers;
        for (VirtualUnit unit : attackers) {
            startingBVAttacker += unit.getUnit().getBV();
        }
        this.defenders = defenders;
        for (VirtualUnit unit : defenders) {
            startingBVDefender += unit.getUnit().getBV();
        }
        this.battleReport = battleReport;
        allUnits = new java.util.ArrayList<VirtualUnit>();
        allUnits.addAll(attackers);
        allUnits.addAll(defenders);
    }

    public int getDistance(VirtualUnit a, VirtualUnit b) {
        return distance.get(a).get(b);
    }

    public void setDistance(VirtualUnit a, VirtualUnit b, int newDistance) {
        //Do it in both ways, so we don't have to care..
        fillDistance(a, b, newDistance);
        fillDistance(b, a, newDistance);
    }

    private void fillDistance(VirtualUnit a, VirtualUnit b, int newDistance) {
        java.util.Hashtable<VirtualUnit, Integer> dist = distance.get(a);
        if (dist == null) {
            dist = new java.util.Hashtable<VirtualUnit, Integer>();
            distance.put(a, dist);
        }
        dist.put(b, newDistance);
    }

    public java.util.List<VirtualUnit> getAttackers() {
        return attackers;
    }

    public java.util.List<VirtualUnit> getDefenders() {
        return defenders;
    }

    public java.util.List<VirtualUnit> getAllUnits() {
        return allUnits;
    }

    public BattleReport getBattleReport() {
        return battleReport;
    }


    public java.util.List<String> getWinners() {
        return winners;
    }


    public boolean hasWinner() {
        return true; //For now there is no draw..
    }


    public java.util.Enumeration<Entity> getDevastatedEntities() {
        //For now there is no way to retreat
        return new java.util.Vector<Entity>().elements();
    }


    public java.util.Enumeration<Entity> getGraveyardEntities() {
        //For now there is no way to retreat
        return new java.util.Vector<Entity>().elements();
    }


    public java.util.Iterator<Entity> getEntities() {
        java.util.Vector<Entity> result = new java.util.Vector<Entity>();
        for (VirtualUnit unit : getAllUnits()) {
            result.add(unit.getUnit().getEntity());
        }
        return result.iterator();
    }


    public java.util.Enumeration<Entity> getRetreatedEntities() {
        //For now there is no way to retreat
        return new java.util.Vector<Entity>().elements();
    }

    protected void addWinner(String winnerName) {
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
