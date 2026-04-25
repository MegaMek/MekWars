package mekwars.server.campaign.operations.newopmanager;

import common.campaign.operations.ModifyingOperation;
import common.campaign.operations.Operation;

public abstract class AbstractOperationManager {

    //red/write classes
    protected server.campaign.operations.OperationLoader opLoader;
    protected server.campaign.operations.OperationWriter opWriter;

    //resolvers
    protected server.campaign.operations.ShortResolver shortResolver;

    //validators
    protected server.campaign.operations.ShortValidator shortValidator;
    //private LongValidator  longValidator;

    //local maps
    protected java.util.TreeMap<String, Operation> ops;
    protected java.util.TreeMap<String, ModifyingOperation> mods;

    //running operations
    protected java.util.TreeMap<Integer, server.campaign.operations.ShortOperation> runningOperations;//shorts

    //disonnection and scrap handling
    protected java.util.TreeMap<String, server.campaign.operations.OpsDisconnectionThread> disconnectionThreads;
    protected java.util.TreeMap<String, server.campaign.operations.OpsScrapThread> scrapThreads;

    protected java.util.TreeMap<String, Long> disconnectionTimestamps;
    protected java.util.TreeMap<String, Long> disconnectionDurations;

    //Map of outstanding long operations
    //ISSUE: should these be somehow sorted by faction?
    protected java.util.TreeMap<Integer, server.campaign.operations.LongOperation> activeLongOps;

    protected boolean MULOnlyArmiesOpsLoad = false;

}
