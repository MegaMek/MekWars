package server.campaign.operations.newopmanager;

import java.util.TreeMap;

import common.campaign.operations.ModifyingOperation;
import common.campaign.operations.Operation;
import server.campaign.operations.LongOperation;
import server.campaign.operations.OperationLoader;
import server.campaign.operations.OperationWriter;
import server.campaign.operations.OpsDisconnectionThread;
import server.campaign.operations.OpsScrapThread;
import server.campaign.operations.ShortOperation;
import server.campaign.operations.ShortResolver;
import server.campaign.operations.ShortValidator;

public abstract class AbstractOperationManager {

    //red/write classes
    protected OperationLoader opLoader;
    protected OperationWriter opWriter;
    
    //resolvers
    protected ShortResolver shortResolver;
    
    //validators
    protected ShortValidator shortValidator;
    //private LongValidator  longValidator;
    
    //local maps
    protected TreeMap<String, Operation> ops;
    protected TreeMap<String, ModifyingOperation> mods;
    
    //running operations
    protected TreeMap<Integer, ShortOperation> runningOperations;//shorts
    
    //disonnection and scrap handling
    protected TreeMap<String, OpsDisconnectionThread> disconnectionThreads;
    protected TreeMap<String, OpsScrapThread> scrapThreads; 
    
    protected TreeMap<String, Long> disconnectionTimestamps;
    protected TreeMap<String, Long> disconnectionDurations;
    
    //Map of outstanding long operations
    //ISSUE: should these be somehow sorted by faction?
    protected TreeMap<Integer, LongOperation> activeLongOps;
    
    protected boolean MULOnlyArmiesOpsLoad = false;
    
}
