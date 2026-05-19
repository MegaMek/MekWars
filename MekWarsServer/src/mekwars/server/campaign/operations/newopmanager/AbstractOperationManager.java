/*
 * Copyright (C) 2005 - nmorris (urgru@users.sourceforge.net)
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekWars.
 *
 * MekWars is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekWars is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekWars was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekwars.server.campaign.operations.newopmanager;

import java.util.TreeMap;

import mekwars.common.campaign.operations.ModifyingOperation;
import mekwars.common.campaign.operations.Operation;
import mekwars.server.campaign.operations.LongOperation;
import mekwars.server.campaign.operations.OperationLoader;
import mekwars.server.campaign.operations.OperationWriter;
import mekwars.server.campaign.operations.OpsDisconnectionThread;
import mekwars.server.campaign.operations.OpsScrapThread;
import mekwars.server.campaign.operations.ShortOperation;
import mekwars.server.campaign.operations.ShortResolver;
import mekwars.server.campaign.operations.ShortValidator;

public abstract class AbstractOperationManager {

    //red/write classes
    protected OperationLoader opLoader;
    protected OperationWriter opWriter;

    //resolvers
    protected ShortResolver shortResolver;

    //validators
    protected ShortValidator shortValidator;

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
