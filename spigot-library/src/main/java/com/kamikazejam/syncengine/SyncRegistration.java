package com.kamikazejam.syncengine;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.SyncCache;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

@Getter @SuppressWarnings("unused")
public class SyncRegistration {
    private final @NotNull JavaPlugin plugin;
    /**
     * The full database name as it would appear in MongoDB (or a folder name),
     * This includes the sync group prefix, described in {@link SyncEngineAPI#getFullDatabaseName(String)} (String)}
     * All plugin caches will be stored in this database as collections
     */
    private final String databaseName;
    private final String dbNameShort;

    // package-private because SyncEngine is the only one allowed to create this
    SyncRegistration(@NotNull JavaPlugin plugin, @NotNull String dbNameShort) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(dbNameShort);
        this.plugin = plugin;
        this.dbNameShort = dbNameShort;
        this.databaseName = SyncEngineAPI.getFullDatabaseName(dbNameShort);
    }

    private final List<Cache<?,?>> caches = new ArrayList<>();
    public void registerCache(Class<? extends SyncCache<?,?>> clazz) {
        // Find a constructor that takes a SyncRegistration
        try {
            Constructor<? extends SyncCache<?,?>> constructor = clazz.getConstructor(SyncRegistration.class);
            SyncCache<?,?> cache = constructor.newInstance(this);
            this.caches.add(cache);
            cache.getLoggerService().info("Cache Registered.");
        } catch (NoSuchMethodException ex1) {
            EngineSource.error("Failed to register cache " + clazz.getName() + " - No constructor that takes a SyncRegistration");
        } catch (Throwable t) {
            EngineSource.error("Failed to register cache " + clazz.getName() + " - " + t.getClass().getName() + ": " + t.getMessage());
        }
    }

    public void shutdown() {
        caches.forEach(Cache::shutdown);
        caches.clear();
    }
}
