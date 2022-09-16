package server.campaign.operations.newopmanager;

import java.util.TreeMap;

import common.campaign.operations.Operation;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.operations.OpsScrapThread;
import server.campaign.operations.ShortOperation;
import server.campaign.operations.ShortValidator;

public interface I_OperationManager {

  //statics
    public static final int TERM_TERMCOMMAND = 0;
    public static final int TERM_NOATTACKERS = 1;
    public static final int TERM_NOPOSSIBLEDEFENDERS = 2;
    public static final int TERM_REPORTINGERROR = 3;
    public static final int TERM_NO_REMAINING_PLAYERS = 4;
    
    public String tick();
    public void resolveShortAttack(Operation o, ShortOperation so, String report);
    public void resolveShortAttack(Operation o, ShortOperation so, String winnerName, String loserName);
    public ShortOperation getShortOpForPlayer(SPlayer p);
    public Operation getOperation(String name);
    public void checkOperations(SArmy a, boolean display);
    public TreeMap<Integer, ShortOperation> getRunningOps();
    public void doDisconnectCheckOnPlayer(String name);
    public void doReconnectCheckOnPlayer(String name);
    public boolean playerHasActiveChickenThread(SPlayer p);
    public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator);
    public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator, boolean ignoreStatus);
    public void clearAllDisconnectionTracks(ShortOperation so);
    public void removePlayerFromAllAttackerLists(SPlayer p, ShortOperation so, boolean verbose);
    public void removePlayerFromAllDefenderLists(SPlayer p, ShortOperation so, boolean verbose);
    public void removePlayerFromAllPossibleDefenderLists(String playerName, boolean penalize);
    public TreeMap<String, OpsScrapThread> getScrapThreads();
    public boolean hasMULOnlyOps();
    public ShortValidator getShortValidator();
    public TreeMap<String, Operation> getOperations();
    public int getFreeShortID();
    public int getFreeLongID();
    public void loadOperations();
    public void addShortOperation(ShortOperation so, SPlayer ap, Operation o);
    public String validateShortDefense(SPlayer dp, SArmy da, Operation o,SPlanet target);
    public String validateShortAttack(SPlayer ap, SArmy aa, Operation o, SPlanet target, int longID, boolean joiningAttack);
    public int playerIsADefender(SPlayer p);
    public int playerIsAnAttacker(SPlayer p);
    public int getLongID(SHouse h, SPlanet p);
    public boolean hasSpecificLongOnPlanet(SHouse h, SPlanet p, Operation o);
    public boolean hasLongOnPlanet(SHouse h, SPlanet p);
    
}
