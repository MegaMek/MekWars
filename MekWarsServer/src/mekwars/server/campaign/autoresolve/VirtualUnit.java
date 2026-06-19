package mekwars.server.campaign.autoresolve;

import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.SUnit;

public class VirtualUnit {

    private final SUnit unit;
    private final SPlayer player;
    private final boolean attacker;
    private VirtualUnit.MovementMode movementMode;
    private int movementDistance;
    private VirtualUnit target;

    public VirtualUnit(SUnit unit, SPlayer player, boolean attacker) {
        this.unit = unit;
        this.player = player;
        this.attacker = attacker;
    }

    /**
     * Reports the State of the Unit to the Player
     */
    public void reportStateToPlayer() {
        unit.reportStateToPlayer(player);
    }

    public void setMovement(VirtualUnit.MovementMode mode, int distance) {
        this.movementMode = mode;
        this.movementDistance = distance;
    }

    public VirtualUnit.MovementMode getMovementMode() {
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

    public int getId() {
        return unit.getId();
    }

    public enum MovementMode {
        STANDING, WALKING, RUNNING, JUMPING
    }
}
