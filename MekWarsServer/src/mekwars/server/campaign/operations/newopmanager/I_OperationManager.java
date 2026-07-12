/*
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

import mekwars.common.campaign.operations.Operation;
import mekwars.server.campaign.SArmy;
import mekwars.server.campaign.SHouse;
import mekwars.server.campaign.SPlanet;
import mekwars.server.campaign.SPlayer;
import mekwars.server.campaign.operations.OpsScrapThread;
import mekwars.server.campaign.operations.ShortOperation;
import mekwars.server.campaign.operations.ShortValidator;

public interface I_OperationManager {

    //statics
    int TERM_TERM_COMMAND = 0;
    int TERM_NO_ATTACKERS = 1;
    int TERM_NO_POSSIBLE_DEFENDERS = 2;
    int TERM_REPORT_ING_ERROR = 3;
    int TERM_NO_REMAINING_PLAYERS = 4;

    String tick();

    void resolveShortAttack(Operation operation, ShortOperation shortOperation, String report);

    void resolveShortAttack(Operation operation, ShortOperation shortOperation, String winnerName, String loserName);

    ShortOperation getShortOpForPlayer(SPlayer sPlayer);

    Operation getOperation(String name);

    void checkOperations(SArmy sArmy, boolean display);

    TreeMap<Integer, ShortOperation> getRunningOps();

    void doDisconnectCheckOnPlayer(String name);

    void doReconnectCheckOnPlayer(String name);

    boolean playerHasActiveChickenThread(SPlayer sPlayer);

    void terminateOperation(ShortOperation shortOperation, int termCode, SPlayer terminator);

    void terminateOperation(ShortOperation shortOperation, int termCode, SPlayer terminator, boolean ignoreStatus);

    void clearAllDisconnectionTracks(ShortOperation shortOperation);

    void removePlayerFromAllAttackerLists(SPlayer sPlayer, ShortOperation shortOperation, boolean verbose);

    void removePlayerFromAllDefenderLists(SPlayer sPlayer, ShortOperation shortOperation, boolean verbose);

    void removePlayerFromAllPossibleDefenderLists(String playerName, boolean penalize);

    TreeMap<String, OpsScrapThread> getScrapThreads();

    boolean hasMULOnlyOps();

    ShortValidator getShortValidator();

    TreeMap<String, Operation> getOperations();

    int getFreeShortID();

    int getFreeLongID();

    void loadOperations();

    void addShortOperation(ShortOperation shortOperation, SPlayer sPlayer, Operation operation);

    String validateShortDefense(SPlayer sPlayer, SArmy sArmy, Operation operation, SPlanet target);

    String validateShortAttack(SPlayer sPlayer, SArmy sArmy, Operation operation, SPlanet target, int longID,
          boolean joiningAttack);

    int playerIsADefender(SPlayer sPlayer);

    int playerIsAnAttacker(SPlayer sPlayer);

    int getLongID(SHouse sHouse, SPlanet sPlanet);

    boolean hasSpecificLongOnPlanet(SHouse sHouse, SPlanet sPlanet, Operation operation);

    boolean hasLongOnPlanet(SHouse sHouse, SPlanet sPlanet);

}
