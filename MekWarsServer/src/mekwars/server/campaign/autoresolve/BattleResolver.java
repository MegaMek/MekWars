package mekwars.server.campaign.autoresolve;


import java.io.IOException;

import megamek.common.HitData;
import megamek.common.ToHitData;
import megamek.common.board.Coords;
import megamek.common.equipment.Mounted;
import megamek.common.game.Game;
import megamek.common.loaders.MapSettings;
import megamek.common.units.Entity;
import megamek.common.util.BoardUtilities;
import megamek.common.weapons.Weapon;
import megamek.logging.MMLogger;
import megamek.server.Server;
import megamek.server.totalWarfare.TWGameManager;
import mekwars.common.Unit;
import mekwars.common.campaign.operations.Operation;
import mekwars.server.campaign.CampaignMain;
import mekwars.server.campaign.SArmy;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.SUnit;
import mekwars.server.campaign.operations.ShortOperation;

public class BattleResolver {
    private static final MMLogger LOGGER = MMLogger.create(BattleResolver.class);
    private Server server;

    private BattleResolver() {
        try {
            server = new Server("", 50000, new TWGameManager());
        } catch (IOException e) {
            LOGGER.error(e, "Unable to start server");
        }
    }

    public static BattleResolver getInstance() {
        return BattleResolverHolder.instance;
    }

    public void resolve(ShortOperation shortOperation) {
        Battlefield bf = new Battlefield(prepareAttackers(shortOperation),
              prepareDefenders(shortOperation),
              new BattleReport());

        //Determine starting Distance
        //TODO: For now it's 21 hexes, later it will be a pilot Skill
        for (VirtualUnit unit : bf.getAttackers()) {
            for (VirtualUnit enemy : bf.getDefenders()) {
                bf.setDistance(unit, enemy, 21);
            }
        }

        //Determine amount of turns, for now it's fixed
        int amountOfTurns = 10;

        for (int i = 0; i < amountOfTurns; i++) {
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
        for (VirtualUnit unit : bf.getAllUnits()) {
            if (unit.isAttacker()) {
                endingBVAttacker += unit.getUnit().getBV();
            } else {
                endingBVDefender += unit.getUnit().getBV();
            }
        }

        if (endingBVAttacker > endingBVDefender) {
            SPlayer winner = CampaignMain.campaignMain.getPlayer(shortOperation.getAttackers().firstKey());
            shortOperation.getWinners().put(winner.getName().toLowerCase(), winner);
            bf.addWinner(winner.getName());
            server.campaign.SPlayer loser = CampaignMain.campaignMain.getPlayer(shortOperation.getDefenders()
                                                                                      .firstKey());
            shortOperation.getLosers().put(loser.getName().toLowerCase(), loser);
        } else {
            server.campaign.SPlayer winner = CampaignMain.campaignMain.getPlayer(shortOperation.getDefenders()
                                                                                       .firstKey());
            shortOperation.getWinners().put(winner.getName().toLowerCase(), winner);
            bf.addWinner(winner.getName());
            server.campaign.SPlayer loser = CampaignMain.campaignMain.getPlayer(shortOperation.getAttackers()
                                                                                      .firstKey());
            shortOperation.getLosers().put(loser.getName().toLowerCase(), loser);
        }

        //Report to Players and Campaign
        Operation o = CampaignMain.campaignMain.getOpsManager().getOperation(shortOperation.getName());

        // set to reporting status
        //        shortOperation.changeStatus(ShortOperation.STATUS_REPORTING);
        //        shortOperation.getReporter().setWinnersAndLosers(shortOperation.getWinners(), shortOperation.getLosers());

        //Build report String
        String report = buildReportString(bf);


        CampaignMain.campaignMain.getOpsManager().resolveShortAttack(o, shortOperation, report);


        for (VirtualUnit unit : bf.getAllUnits()) {
            unit.reportStateToPlayer();
        }

        //TODO: Better reporting
        for (String player : shortOperation.getAllPlayerNames()) {
            CampaignMain.campaignMain.toUser(bf.getBattleReport().getReport().toString(), player, true);
        }
    }

    private java.util.List<mekwars.server.campaign.autoresolve.VirtualUnit> prepareAttackers(ShortOperation so) {
        java.util.ArrayList<mekwars.server.campaign.autoresolve.VirtualUnit> result = new java.util.ArrayList<mekwars.server.campaign.autoresolve.VirtualUnit>();
        for (String attacker : so.getAttackers().keySet()) {
            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(attacker);
            if (player != null) {
                SArmy army = player.getArmy(so.getAttackers().get(attacker));
                for (Unit unit : army.getUnits()) {
                    SUnit sunit = (SUnit) unit;
                    result.add(new VirtualUnit(sunit, player, true));
                }
            }
        }
        return result;
    }

    /**
     * Converts the units from the operation into VirtualUnits
     *
     * @param op
     *
     * @return all units as virtual Units
     */
    private java.util.List<mekwars.server.campaign.autoresolve.VirtualUnit> prepareDefenders(ShortOperation so) {
        java.util.ArrayList<mekwars.server.campaign.autoresolve.VirtualUnit> result = new java.util.ArrayList<mekwars.server.campaign.autoresolve.VirtualUnit>();
        for (String defender : so.getDefenders().keySet()) {
            server.campaign.SPlayer player = CampaignMain.campaignMain.getPlayer(defender);
            if (player != null) {
                SArmy army = player.getArmy(so.getDefenders().get(defender));
                for (Unit unit : army.getUnits()) {
                    SUnit sunit = (SUnit) unit;
                    result.add(new VirtualUnit(sunit, player, false));
                }
            }
        }
        return result;
    }

    private void resolveTargeting(Battlefield bf) {
        //For now: Random
        for (VirtualUnit unit : bf.getAllUnits()) {
            unit.setTarget(findRandomEnemy(unit, bf));
            bf.getBattleReport().addTargetEvent(unit, unit.getTarget());
        }

    }

    private void resolveMovement(Battlefield bf) {
        //Easy for now.
        for (VirtualUnit unit : bf.getAllUnits()) {
            VirtualUnit target = unit.getTarget();
            Entity ent = unit.getUnit().getEntity();
            int distance = bf.getDistance(unit, target);
            if (ent.getArmorRemainingPercent() > 0.5 && distance > 0) {
                bf.setDistance(unit, target, Math.max(0, distance - ent.getRunMP()));
                unit.setMovement(MovementMode.RUNNING, Math.abs(distance - bf.getDistance(unit, target)));
            } else if (ent.getArmorRemainingPercent() <= 0.5) {
                bf.setDistance(unit, target, distance + ent.getRunMP());
                unit.setMovement(MovementMode.RUNNING, Math.abs(distance - bf.getDistance(unit, target)));
            } else {
                unit.setMovement(MovementMode.STANDING, 0);
            }
            bf.getBattleReport()
                  .addMovementEvent(unit,
                        "Moved from a distance of " + distance + " to " + bf.getDistance(unit, target) + "<br>");
        }
    }

    private void resolveFiring(Battlefield bf) {
        for (VirtualUnit unit : bf.getAllUnits()) {
            VirtualUnit target = unit.getTarget();
            Entity ent = unit.getUnit().getEntity();
            int toHit = unit.getUnit().getPilot().getGunnery();
            int distance = bf.getDistance(unit, target);
            for (Mounted mounted : ent.getWeaponList()) {
                Weapon weapon = (Weapon) mounted.getType();
                if (distance <= weapon.getShortRange()) {
                    //No Modifier
                } else if (distance <= weapon.getMediumRange()) {
                    toHit = toHit + 2;
                } else if (distance <= weapon.getLongRange()) {
                    toHit = toHit + 4;
                } else {
                    //Out of Range
                    break;
                }
                //Consider unit movement
                toHit = toHit + unit.getMovementMode().ordinal();

                //Consider target movement
                if (target.getMovementDistance() <= 2) {
                    //No modifier
                } else if (target.getMovementDistance() <= 4) {
                    toHit = toHit + 1;
                } else if (target.getMovementDistance() <= 6) {
                    toHit = toHit + 2;
                } else if (target.getMovementDistance() <= 9) {
                    toHit = toHit + 3;
                } else {
                    toHit = toHit + 4;
                }

                //Shall he fire?
                if (toHit <= 12) {
                    int roll = CampaignMain.campaignMain.getRandom().nextInt(5) +
                                     1 +
                                     CampaignMain.campaignMain.getRandom().nextInt(5) +
                                     1;
                    if (roll >= toHit) {
                        damageEntity(target, weapon.getDamage(), target.getPlayer().getName(), bf);
                    } else {
                        bf.getBattleReport()
                              .addFireEvent(unit,
                                    ent.getDisplayName() +
                                          " tried to hit " +
                                          target.getUnit().getEntity().getDisplayName() +
                                          " at a distance of " +
                                          distance +
                                          " with a toHit of " +
                                          toHit +
                                          " but rolled a " +
                                          roll +
                                          "<br>");
                    }
                }
            }
        }
    }

    private String buildReportString(Battlefield bf) {
        return MWClient.prepareReport(bf, true, null).toString();
    }

    private VirtualUnit findRandomEnemy(VirtualUnit unit, Battlefield bf) {
        VirtualUnit target;
        if (unit.isAttacker()) {
            target = bf.getDefenders().get(CampaignMain.campaignMain.getRandom().nextInt(bf.getDefenders().size()));
        } else {
            target = bf.getAttackers().get(CampaignMain.campaignMain.getRandom().nextInt(bf.getAttackers().size()));
        }
        return target;
    }

    private synchronized void damageEntity(VirtualUnit unit, int damage, String nameOfPlayer, Battlefield bf) {
        //Owner
        Entity ent = unit.getUnit().getEntity();
        ent.setOwner(new Player(1, nameOfPlayer));
        //Position
        Game g = new Game();
        MapSettings mapsettings = MapSettings.getInstance();
        g.setBoard(BoardUtilities.generateRandom(mapsettings));
        ent.setPosition(new Coords(0, 0));

        g.addEntity(1, ent);

        server.setGame(g);


        //Determine location
        HitData hd = ent.rollHitLocation(ToHitData.HIT_NORMAL, ToHitData.SIDE_FRONT);
    }

    private static final class BattleResolverHolder {
        private static final BattleResolver instance = new BattleResolver();
    }

}
