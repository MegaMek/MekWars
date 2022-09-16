package server.campaign.autoresolve;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import client.MWClient;
import common.Unit;
import common.campaign.operations.Operation;
import megamek.common.Coords;
import megamek.common.Entity;
import megamek.common.Game;
import megamek.common.HitData;
import megamek.common.MapSettings;
import megamek.common.Mounted;
import megamek.common.Player;
import megamek.common.Report;
import megamek.common.ToHitData;
import megamek.common.util.BoardUtilities;
import megamek.common.weapons.Weapon;
import megamek.server.Server;
import server.campaign.CampaignMain;
import server.campaign.SArmy;
import server.campaign.SPlayer;
import server.campaign.SUnit;
import server.campaign.autoresolve.VirtualUnit.MovementMode;
import server.campaign.operations.ShortOperation;

public class BattleResolver {
	
	private Server server;
	
	private static BattleResolver instance;
	
	private BattleResolver(){
		try {
			server = new Server("", 50000);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static BattleResolver getInstance(){
		if (instance == null){
			synchronized (BattleResolver.class) {
				if (instance == null){
					instance = new BattleResolver();
				}
			}
		}
		return instance;
	}

	public void resolve(ShortOperation so) {
		Battlefield bf = new Battlefield(prepareAttackers(so), prepareDefenders(so), new BattleReport());
		
		//Determine starting Distance
		//TODO: For now it's 21 hexes, later it will be a pilot Skill
		for (VirtualUnit unit: bf.getAttackers()){
			for (VirtualUnit enemy: bf.getDefenders()){
				bf.setDistance(unit, enemy,21);
			}
		}
		
		//Determine amount of turns, for now it's fixed
		int amountOfTurns = 10;
		
		for (int i=0; i < amountOfTurns; i++){
			//Resolve each turn
			//Targeting
			resolveTargeting(bf);
			//Movement
			resolveMovement(bf);
			//Firing
			resolveFiring(bf);
		}
		
		//Calculate Winner
		int endingBVAttacker = 0;
		int endingBVDefender = 0;
		for (VirtualUnit unit: bf.getAllUnits()){
			if (unit.isAttacker()){
				endingBVAttacker += unit.getUnit().getBV();
			} else {
				endingBVDefender += unit.getUnit().getBV();
			}
		}

		if (endingBVAttacker > endingBVDefender){
			SPlayer winner = CampaignMain.cm.getPlayer(so.getAttackers().firstKey());
			so.getWinners().put(winner.getName().toLowerCase(), winner);
			bf.addWinner(winner.getName());
			SPlayer loser = CampaignMain.cm.getPlayer(so.getDefenders().firstKey());
			so.getLosers().put(loser.getName().toLowerCase(), loser);
 		} else {
			SPlayer winner = CampaignMain.cm.getPlayer(so.getDefenders().firstKey());
			so.getWinners().put(winner.getName().toLowerCase(), winner);
			bf.addWinner(winner.getName());
			SPlayer loser = CampaignMain.cm.getPlayer(so.getAttackers().firstKey());
			so.getLosers().put(loser.getName().toLowerCase(), loser);
 		}
		
		//Report to Players and Campaign
		Operation o = CampaignMain.cm.getOpsManager().getOperation(so.getName());
		
		 // set to reporting status
//        so.changeStatus(ShortOperation.STATUS_REPORTING);
//        so.getReporter().setWinnersAndLosers(so.getWinners(), so.getLosers());
        
        //Build report String
        String report = buildReportString(bf);
        
        
        CampaignMain.cm.getOpsManager().resolveShortAttack(o, so, report);

		
		for (VirtualUnit unit: bf.getAllUnits()){
			unit.reportStateToPlayer();
		}
		
		//TODO: Better reporting
		for (String player: so.getAllPlayerNames()){
			CampaignMain.cm.toUser(bf.getBattleReport().getReport().toString(), player, true);
		}
	}
	
	private String buildReportString(Battlefield bf) {
		return MWClient.prepareReport(bf, true, null).toString();
	}

	private void resolveTargeting(Battlefield bf) {
		//For now: Random
		for (VirtualUnit unit: bf.getAllUnits()){
			unit.setTarget(findRandomEnemy(unit, bf));
			bf.getBattleReport().addTargetEvent(unit, unit.getTarget());
		}
		
	}

	private List<VirtualUnit> prepareAttackers(ShortOperation so) {
		ArrayList<VirtualUnit> result = new ArrayList<VirtualUnit>();
		 for (String attacker : so.getAttackers().keySet()) {
           SPlayer player = CampaignMain.cm.getPlayer(attacker);
           if (player != null) {
              SArmy army = player.getArmy(so.getAttackers().get(attacker));
               for (Unit unit: army.getUnits()){
               	SUnit sunit = (SUnit)unit;
               	result.add(new VirtualUnit(sunit, player, true));
               }
           }
		 }
		 return result;
	}
	
	private VirtualUnit findRandomEnemy(VirtualUnit unit, Battlefield bf){
		VirtualUnit target;
		if (unit.isAttacker()){
			target = bf.getDefenders().get(CampaignMain.cm.getR().nextInt(bf.getDefenders().size()));
		} else {
			target = bf.getAttackers().get(CampaignMain.cm.getR().nextInt(bf.getAttackers().size()));
		}
		return target;
	}

	private void resolveFiring(Battlefield bf) {
		for (VirtualUnit unit: bf.getAllUnits()){
			VirtualUnit target = unit.getTarget();
			Entity ent = unit.getUnit().getEntity();
			int toHit = unit.getUnit().getPilot().getGunnery();
			int distance = bf.getDistance(unit, target);
			for (Mounted mounted: ent.getWeaponList()){
				Weapon weapon = (Weapon)mounted.getType();
				if (distance <= weapon.getShortRange()){
					//No Modifier
				} else if (distance <= weapon.getMediumRange()){
					toHit = toHit + 2;
				} else if (distance <= weapon.getLongRange()){
					toHit = toHit + 4;
				} else {
					//Out of Range
					break;
				}
				//Consider unit movement
				toHit = toHit + unit.getMovementMode().ordinal();
				
				//Consider target movement
				if (target.getMovementDistance() <= 2){
					//No modifier
				} else if (target.getMovementDistance() <= 4){
					toHit = toHit + 1;
				} else if (target.getMovementDistance() <= 6){
					toHit = toHit + 2;
				} else if (target.getMovementDistance() <= 9){
					toHit = toHit + 3;
				} else {
					toHit = toHit + 4;
				}
				
				//Shall he fire?
				if (toHit <= 12){
					int roll = CampaignMain.cm.getR().nextInt(5) + 1 + CampaignMain.cm.getR().nextInt(5) + 1;
					if (roll >= toHit){
						damageEntity(target, weapon.getDamage(), target.getPlayer().getName(), bf);
					} else {
						bf.getBattleReport().addFireEvent(unit, ent.getDisplayName() + " tried to hit " + target.getUnit().getEntity().getDisplayName() + " at a distance of " + distance + " with a toHit of " + toHit + " but rolled a " + roll + "<br>");
					}
				}
			}
		}
	}

	private void resolveMovement(Battlefield bf) {
		//Easy for now.
		for (VirtualUnit unit: bf.getAllUnits()){
			VirtualUnit target = unit.getTarget();
			Entity ent = unit.getUnit().getEntity();
			int distance = bf.getDistance(unit, target);
			if (ent.getArmorRemainingPercent() > 0.5 && distance > 0){
				bf.setDistance(unit, target, Math.max(0, distance - ent.getRunMP()));
				unit.setMovement(MovementMode.RUNNING, Math.abs(distance - bf.getDistance(unit, target)));
			} else if (ent.getArmorRemainingPercent() <= 0.5){
				bf.setDistance(unit, target, distance + ent.getRunMP());
				unit.setMovement(MovementMode.RUNNING, Math.abs(distance - bf.getDistance(unit, target)));
			} else {
				unit.setMovement(MovementMode.STANDING, 0);
			}
			bf.getBattleReport().addMovementEvent(unit, "Moved from a distance of " + distance + " to " + bf.getDistance(unit, target) + "<br>");
		}
	}

	/**
	 * Converts the units from the operation into VirtualUnits
	 * @param op
	 * @return all units as virtual Units
	 */
	private List<VirtualUnit> prepareDefenders(ShortOperation so){
		ArrayList<VirtualUnit> result = new ArrayList<VirtualUnit>();
		 for (String defender : so.getDefenders().keySet()) {
			 SPlayer player = CampaignMain.cm.getPlayer(defender);
	         if (player != null) {
	        	 SArmy army = player.getArmy(so.getDefenders().get(defender));
	             for (Unit unit: army.getUnits()){
	            	 SUnit sunit = (SUnit)unit;
	            	 result.add(new VirtualUnit(sunit, player, false));
	             }
	         }
		 }
		return result;
	}
	
	private synchronized void damageEntity(VirtualUnit unit, int damage, String nameOfPlayer, Battlefield bf){
		//Owner
		Entity ent = unit.getUnit().getEntity(); 
		ent.setOwner(new Player(1, nameOfPlayer));
		//Position
		Game g = new Game();
		MapSettings mapsettings = MapSettings.getInstance();
		g.setBoard(BoardUtilities.generateRandom(mapsettings));
		ent.setPosition(new Coords(0,0));
		
		g.addEntity(1, ent);
		
		server.setGame(g);
		
		
		//Determine location
		HitData hd = ent.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
		
		//Do damage
		Vector<Report> report = server.damageEntity(ent, hd, damage);
		
		//Prepare Output
		for (Report r: report){
			String text = r.getText();
			bf.getBattleReport().addFireEvent(unit, text);
		}
	}

}
