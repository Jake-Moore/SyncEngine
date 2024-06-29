package com.kamikazejam.syncengine.base.save;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Only for Profiles. Objects can exist in various copies around various servers
 * If we auto-saved Objects, we would increase the risk of save collisions and VersionMismatchExceptions
 * Profiles are guaranteed to be on the server with the player, so we can auto-save them
 */
@SuppressWarnings("unused")
public class ProfileAutoSaveTask<K, X extends Sync<K>> implements Runnable {

    private final Cache<K, X> cache;

    private BukkitTask task = null;

    public ProfileAutoSaveTask(@NotNull Cache<K, X> cache) {
        Preconditions.checkNotNull(cache);
        this.cache = cache;
    }

    @Override
    public void run() {
        CacheSaveResult result = cache.saveAll();
        if (result.getFailed() > 0) {
            cache.getLoggerService().info(result.getFailed() + " Payload objects failed to save during auto-save. (Out of " + result.getTotal() + " total)");
        } else if (result.getTotal() > 0) {
            cache.getLoggerService().debug("Auto-saved " + result.getTotal() + " " + cache.getName() + " objects.");
        }
    }

    public boolean isRunning() {
        return this.task != null;
    }

    public void start() {
        if (!this.isRunning()) {
            // We add 4 ticks to the delay (seemingly randomly) to avoid running with other looping tasks that may be
            //   Scheduled on startup. This prevents simultaneous saving from any other loops running, and avoids
            //   Morphia errors from that.
            BukkitScheduler scheduler = cache.getPlugin().getServer().getScheduler();
            long period = EngineSource.get().getConfig().getLong("profiles.autoSaveIntervalSec");
            // Run the task every 600 seconds (by default)
            this.task = scheduler.runTaskTimerAsynchronously(cache.getPlugin(), this, (period * 20L) + 4, (period * 20L));
        }
    }

    public void stop() {
        if (this.isRunning()) {
            this.task.cancel();
            this.task = null;
        }
    }

}
