package com.kamikazejam.syncengine;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.DuplicateCacheException;
import com.kamikazejam.syncengine.base.exception.DuplicateDatabaseException;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public class SyncEngineAPI {
    @Getter
    private static final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, String> databases = new ConcurrentHashMap<>(); // Stored lowercase for uniqueness checks

    /**
     * Register a cache w/ a hook
     *
     * @param cache {@link Cache}
     */
    public static void saveCache(Cache<?, ?> cache) throws DuplicateCacheException {
        if (caches.containsKey(convertCacheName(cache.getName()))) {
            throw new DuplicateCacheException(cache);
        }
        caches.put(convertCacheName(cache.getName()), cache);
    }

    /**
     * Unregister a cache w/ a hook
     *
     * @param cache {@link com.kamikazejam.syncengine.base.SyncCache}
     */
    public static void removeCache(Cache<?, ?> cache) {
        caches.remove(convertCacheName(cache.getName()));
    }

    /**
     * Removes all spaces from the name and converts it to lowercase.
     */
    public static String convertCacheName(String name) {
        return name.toLowerCase().replace(" ", "");
    }

    /**
     * Adds the sync group and a '_' char to the beginning of the dbName,
     * to allow multiple sync group networks to operate on the same MongoDB server
     */
    public static @NotNull String getFullDatabaseName(String dbName) {
        // Just in case, don't add the sync group twice
        if (dbName.startsWith(PluginSource.getSyncServerGroup() + "_")) {
            return dbName;
        }
        return PluginSource.getSyncServerGroup() + "_" + dbName;
    }

    /**
     * Gets the sync ID of this server.
     */
    public static @NotNull String getSyncServerID() {
        return PluginSource.getSyncServerId();
    }

    /**
     * Gets the sync group of this server.
     */
    public static @NotNull String getSyncServerGroup() {
        return PluginSource.getSyncServerGroup();
    }

    /**
     * @return if SyncEngine is in debug mode
     */
    public static boolean isDebug() {
        return PluginSource.isDebug();
    }

    public static void registerDatabase(String databaseName) throws DuplicateDatabaseException {
        if (isDatabaseNameRegistered(databaseName)) {
            throw new DuplicateDatabaseException(databaseName);
        }
        databases.put(databaseName.toLowerCase(), databaseName);
    }

    public static boolean isDatabaseNameRegistered(String databaseName) {
        return databases.containsKey(databaseName.toLowerCase());
    }

    /**
     * Register your plugin and reserve a database name for your plugin's caches.
     * @return Your SyncRegistration (to be passed into your cache constructors)
     * @throws DuplicateDatabaseException - if this databaseName is already in use
     */
    public static SyncRegistration register(@NotNull JavaPlugin plugin, @NotNull String databaseName) throws DuplicateDatabaseException {
        Preconditions.checkNotNull(databaseName);

        registerDatabase(getFullDatabaseName(databaseName));
        return new SyncRegistration(plugin, databaseName);
    }
}