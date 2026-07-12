/*
 * Copyright (C) 2007 Torren (torren@users.sourceforge.net)
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

package mekwars.common.threads;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import megamek.logging.MMLogger;

/**
 * Application-wide singleton that hands background {@link Thread}s off to a shared, cached
 * {@link ExecutorService} instead of letting every caller spawn (and manage) raw {@code Thread}
 * objects itself.
 * <p>
 * This is the common entry point used across the client and server code whenever a piece of work
 * (for example the various worker threads in this package: {@link ReaderThread}, {@link WriterThread},
 * {@link RepairManagmentThread}, {@link SalvageManagmentThread}, {@link ClientThread}, etc.) needs to
 * run on a background thread. Because {@link Executors#newCachedThreadPool()} is used, idle threads
 * are reused and reaped after 60 seconds of inactivity, and new threads are created on demand for
 * bursts of work.
 * <p>
 * The class is effectively a classic eager-initialized singleton: there is exactly one instance for
 * the whole JVM, obtained via {@link #getInstance()}.
 */
public class ThreadManager {
    private static final MMLogger LOGGER = MMLogger.create(ThreadManager.class);

    /** The single, eagerly-created instance shared by the entire application. */
    private static final ThreadManager instance = new ThreadManager();

    /** Cached thread pool backing every task submitted via {@link #runInThreadFromPool(Thread)}. */
    private final ExecutorService executor;

    /**
     * Creates the executor backing this manager. Protected (rather than private) so the singleton
     * pattern could be relaxed by a subclass, but in practice only {@link #instance} is ever created.
     */
    protected ThreadManager() {
        executor = Executors.newCachedThreadPool();
    }

    /**
     * @return the single shared {@code ThreadManager} instance for this JVM.
     */
    public static ThreadManager getInstance() {
        return instance;
    }

    /**
     * Submits the given {@link Thread} (or {@code Runnable}) to the shared cached thread pool for
     * execution. Any exception thrown while submitting the task (e.g. if the executor has already
     * been {@link #shutdown()}) is caught and logged rather than propagated to the caller.
     *
     * @param runnable the thread/task to run in the background
     */
    public void runInThreadFromPool(Thread runnable) {
        try {
            executor.execute(runnable);
        } catch (Exception ex) {
            LOGGER.error(ex, "Task not accepted or not executable: {}", ex.getLocalizedMessage());
        }
    }


    /**
     * Initiates an orderly shutdown of the shared executor: previously submitted tasks continue to
     * run, but no new tasks are accepted afterward. Does not wait for in-flight tasks to finish.
     */
    public void shutdown() {
        executor.shutdown();
    }
}
