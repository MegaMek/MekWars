package mekwars.server.campaign.operations.newopmanager;

import common.campaign.operations.Operation;

public interface I_OperationManager {

    //statics
    public static final int TERM_TERMCOMMAND = 0;
    public static final int TERM_NOATTACKERS = 1;
    public static final int TERM_NOPOSSIBLEDEFENDERS = 2;
    public static final int TERM_REPORTINGERROR = 3;
    public static final int TERM_NO_REMAINING_PLAYERS = 4;

    public String tick();

    public void resolveShortAttack(Operation o, server.campaign.operations.ShortOperation so, String report);

    public void resolveShortAttack(Operation o, server.campaign.operations.ShortOperation so, String winnerName,
          String loserName);

    public server.campaign.operations.ShortOperation getShortOpForPlayer(server.campaign.SPlayer p);

    public Operation getOperation(String name);

    public void checkOperations(server.campaign.SArmy a, boolean display);

    public java.util.TreeMap<Integer, server.campaign.operations.ShortOperation> getRunningOps();

    public void doDisconnectCheckOnPlayer(String name);

    public void doReconnectCheckOnPlayer(String name);

    public boolean playerHasActiveChickenThread(server.campaign.SPlayer p);

    public void terminateOperation(server.campaign.operations.ShortOperation so, int termCode,
          server.campaign.SPlayer terminator);

    public void terminateOperation(server.campaign.operations.ShortOperation so, int termCode,
          server.campaign.SPlayer terminator, boolean ignoreStatus);

    public void clearAllDisconnectionTracks(server.campaign.operations.ShortOperation so);

    public void removePlayerFromAllAttackerLists(server.campaign.SPlayer p,
          server.campaign.operations.ShortOperation so, boolean verbose);

    public void removePlayerFromAllDefenderLists(server.campaign.SPlayer p,
          server.campaign.operations.ShortOperation so, boolean verbose);

    public void removePlayerFromAllPossibleDefenderLists(String playerName, boolean penalize);

    public java.util.TreeMap<String, server.campaign.operations.OpsScrapThread> getScrapThreads();

    public boolean hasMULOnlyOps();

    public server.campaign.operations.ShortValidator getShortValidator();

    public java.util.TreeMap<String, Operation> getOperations();

    public int getFreeShortID();

    public int getFreeLongID();

    public void loadOperations();

    public void addShortOperation(server.campaign.operations.ShortOperation so, server.campaign.SPlayer ap,
          Operation o);

    public String validateShortDefense(server.campaign.SPlayer dp, server.campaign.SArmy da, Operation o,
          server.campaign.SPlanet target);

    public String validateShortAttack(
          server.campaign.SPlayer ap, server.campaign.SArmy aa, Operation o, server.campaign.SPlanet target, int longID,
          boolean joiningAttack);

    public int playerIsADefender(server.campaign.SPlayer p);

    public int playerIsAnAttacker(server.campaign.SPlayer p);

    public int getLongID(server.campaign.SHouse h, server.campaign.SPlanet p);

    public boolean hasSpecificLongOnPlanet(server.campaign.SHouse h, server.campaign.SPlanet p, Operation o);

    public boolean hasLongOnPlanet(server.campaign.SHouse h, server.campaign.SPlanet p);

}
