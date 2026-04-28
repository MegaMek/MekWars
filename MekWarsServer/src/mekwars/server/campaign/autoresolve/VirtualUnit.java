package mekwars.server.campaign.autoresolve;

import server.campaign.SUnit;

public class VirtualUnit {

    private mekwars.server.campaign.autoresolve.VirtualUnit.MovementMode movementMode;
    private int movementDistance;
    private mekwars.server.campaign.autoresolve.VirtualUnit target;
    private SUnit unit;
    private server.campaign.SPlayer player;
    private boolean attacker;
    public VirtualUnit(SUnit unit, server.campaign.SPlayer player, boolean attacker) {
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

    public void setMovement(mekwars.server.campaign.autoresolve.VirtualUnit.MovementMode mode, int distance) {
        this.movementMode = mode;
        this.movementDistance = distance;
    }

    public mekwars.server.campaign.autoresolve.VirtualUnit.MovementMode getMovementMode() {
        return movementMode;
    }

    public int getMovementDistance() {
        return movementDistance;
    }

    public SUnit getUnit() {
        return unit;
    }

    public server.campaign.SPlayer getPlayer() {
        return player;
    }

    public boolean isAttacker() {
        return attacker;
    }

    public mekwars.server.campaign.autoresolve.VirtualUnit getTarget() {
        return target;
    }

    public void setTarget(mekwars.server.campaign.autoresolve.VirtualUnit target) {
        this.target = target;
    }

    public int getId() {
        return unit.getId();
    }

    public enum MovementMode {
        STANDING, WALKING, RUNNING, JUMPING
    }

}
