package server.campaign.autoresolve;

import server.campaign.SPlayer;
import server.campaign.SUnit;

public class VirtualUnit {
	
	public enum MovementMode {
		STANDING, WALKING, RUNNING, JUMPING
	}

	private MovementMode movementMode;
	private int movementDistance;

	private VirtualUnit target;
	private SUnit unit;
	private SPlayer player;
	private boolean attacker;
	
	public VirtualUnit(SUnit unit, SPlayer player, boolean attacker){
		this.unit = unit;
		this.player = player;
		this.attacker = attacker;
	}
	
	/**
	 * Reports the State of the Unit to the Player
	 */
	public void reportStateToPlayer(){
    	unit.reportStateToPlayer(player);
	}
	
	public void setMovement(MovementMode mode, int distance){
		this.movementMode = mode;
		this.movementDistance = distance;
	}

	public MovementMode getMovementMode() {
		return movementMode;
	}
	
	
	public int getMovementDistance() {
		return movementDistance;
	}
	

	public SUnit getUnit() {
		return unit;
	}

	public SPlayer getPlayer() {
		return player;
	}

	public boolean isAttacker() {
		return attacker;
	}

	public VirtualUnit getTarget() {
		return target;
	}

	public void setTarget(VirtualUnit target) {
		this.target = target;
	}
	
	public int getId(){
		return unit.getId();
	}
	
}
