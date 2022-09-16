package server.campaign.operations.newopmanager;

import java.util.TreeMap;

import common.campaign.operations.Operation;
import server.campaign.SArmy;
import server.campaign.SHouse;
import server.campaign.SPlanet;
import server.campaign.SPlayer;
import server.campaign.operations.OperationManager;
import server.campaign.operations.OpsScrapThread;
import server.campaign.operations.ShortOperation;
import server.campaign.operations.ShortValidator;

public class NewOperationManager extends AbstractOperationManager implements I_OperationManager {

    private OperationManager tempOpManager = new OperationManager();
    
    private int nextShortID = 0;
    private int nextLongID = 0;
    
    @Override
    public String tick() {
        // TODO Auto-generated method stub
        return tempOpManager.tick();
    }

    @Override
    public void resolveShortAttack(Operation o, ShortOperation so, String report) {
        // TODO Auto-generated method stub
        tempOpManager.resolveShortAttack(o,so,report);
    }

    @Override
    public void resolveShortAttack(Operation o, ShortOperation so, String winnerName, String loserName) {
        // TODO Auto-generated method stub
        tempOpManager.resolveShortAttack(o,  so,  winnerName, loserName);
    }

    @Override
    public ShortOperation getShortOpForPlayer(SPlayer p) {
        // TODO Auto-generated method stub
        return tempOpManager.getShortOpForPlayer(p);
    }

    @Override
    public Operation getOperation(String name) {
        // TODO Auto-generated method stub
        return tempOpManager.getOperation(name);
    }

    @Override
    public void checkOperations(SArmy a, boolean display) {
        // TODO Auto-generated method stub
        tempOpManager.checkOperations(a, display);
    }

    @Override
    public TreeMap<Integer, ShortOperation> getRunningOps() {
        // TODO Auto-generated method stub
        return tempOpManager.getRunningOps();
    }

    @Override
    public void doDisconnectCheckOnPlayer(String name) {
        // TODO Auto-generated method stub
        tempOpManager.doDisconnectCheckOnPlayer(name);
    }

    @Override
    public void doReconnectCheckOnPlayer(String name) {
        // TODO Auto-generated method stub
        tempOpManager.doReconnectCheckOnPlayer(name);
    }

    @Override
    public boolean playerHasActiveChickenThread(SPlayer p) {
        // TODO Auto-generated method stub
        return tempOpManager.playerHasActiveChickenThread(p);
    }

    @Override
    public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator) {
        // TODO Auto-generated method stub
        tempOpManager.terminateOperation(so, termCode, terminator);
    }

    @Override
    public void terminateOperation(ShortOperation so, int termCode, SPlayer terminator, boolean ignoreStatus) {
        // TODO Auto-generated method stub
        tempOpManager.terminateOperation(so, termCode, terminator, ignoreStatus);
    }

    @Override
    public void clearAllDisconnectionTracks(ShortOperation so) {
        // TODO Auto-generated method stub
        tempOpManager.clearAllDisconnectionTracks(so);
    }

    @Override
    public void removePlayerFromAllAttackerLists(SPlayer p, ShortOperation so, boolean verbose) {
        // TODO Auto-generated method stub
        tempOpManager.removePlayerFromAllAttackerLists(p, so, verbose);
    }

    @Override
    public void removePlayerFromAllDefenderLists(SPlayer p, ShortOperation so, boolean verbose) {
        // TODO Auto-generated method stub
        tempOpManager.removePlayerFromAllDefenderLists(p, so, verbose);
    }

    @Override
    public void removePlayerFromAllPossibleDefenderLists(String playerName, boolean penalize) {
        // TODO Auto-generated method stub
        tempOpManager.removePlayerFromAllPossibleDefenderLists(playerName, penalize);
    }

    @Override
    public TreeMap<String, OpsScrapThread> getScrapThreads() {
        // TODO Auto-generated method stub
        return tempOpManager.getScrapThreads();
    }

    @Override
    public boolean hasMULOnlyOps() {
        // TODO Auto-generated method stub
        return tempOpManager.hasMULOnlyOps();
    }

    @Override
    public ShortValidator getShortValidator() {
        // TODO Auto-generated method stub
        return tempOpManager.getShortValidator();
    }

    @Override
    public TreeMap<String, Operation> getOperations() {
        // TODO Auto-generated method stub
        return tempOpManager.getOperations();
    }

    /**
     * Method to get a short ID.  In order to stop the issue we're seeing where scrap is being sent to the wrong
     * players, I am not reusing IDs
     * 
     * @return the next available ShortID
     */
    @Override
    public int getFreeShortID() {
        return nextShortID++;
    }

    /**
     * Method to get a long ID.  In order to stop the issue we're seeing where scrap is being sent to the wrong
     * players, I am not reusing IDs
     * 
     * @return the next available LongID
     *
     */
    @Override
    public int getFreeLongID() {
        // TODO Auto-generated method stub
        return nextLongID++;
    }

    @Override
    public void loadOperations() {
        // TODO Auto-generated method stub
        tempOpManager.loadOperations();
    }

    @Override
    public void addShortOperation(ShortOperation so, SPlayer ap, Operation o) {
        // TODO Auto-generated method stub
        tempOpManager.addShortOperation(so, ap, o);
    }

    @Override
    public String validateShortDefense(SPlayer dp, SArmy da, Operation o, SPlanet target) {
        // TODO Auto-generated method stub
        return tempOpManager.validateShortDefense(dp, da, o, target);
    }

    @Override
    public String validateShortAttack(SPlayer ap, SArmy aa, Operation o, SPlanet target, int longID,
            boolean joiningAttack) {
        // TODO Auto-generated method stub
        return tempOpManager.validateShortAttack(ap, aa, o, target, longID, joiningAttack);
    }

    @Override
    public int playerIsADefender(SPlayer p) {
        // TODO Auto-generated method stub
        return tempOpManager.playerIsADefender(p);
    }

    @Override
    public int playerIsAnAttacker(SPlayer p) {
        // TODO Auto-generated method stub
        return tempOpManager.playerIsAnAttacker(p);
    }

    @Override
    public int getLongID(SHouse h, SPlanet p) {
        // TODO Auto-generated method stub
        return tempOpManager.getLongID(h, p);
    }

    @Override
    public boolean hasSpecificLongOnPlanet(SHouse h, SPlanet p, Operation o) {
        // TODO Auto-generated method stub
        return tempOpManager.hasSpecificLongOnPlanet(h, p, o);
    }

    @Override
    public boolean hasLongOnPlanet(SHouse h, SPlanet p) {
        // TODO Auto-generated method stub
        return tempOpManager.hasLongOnPlanet(h, p);
    }

}
