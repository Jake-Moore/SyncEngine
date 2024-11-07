package com.kamikazejam.syncengine;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.DuplicateCacheException;
import com.kamikazejam.syncengine.base.exception.DuplicateDatabaseException;
import com.kamikazejam.syncengine.util.struct.DatabaseRegistration;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SyncEngineAPI {
    @Getter
    private static final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

    // Key is databaseName stored lowercase for uniqueness checks
    @Getter
    private static final ConcurrentMap<String, DatabaseRegistration> databases = new ConcurrentHashMap<>();

    /**
     * Get a cache by name
     *
     * @param name Name of the cache
     * @return The Cache
     */
    public static @Nullable Cache<?,?> getCache(String name) {
        return caches.get(convertCacheName(name));
    }

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


    private static List<Cache<?,?>> _sortedCachesReversed = null;
    /**
     * Retrieve the caches in sorted order by dependencies (load order)
     */
    public static @NotNull List<Cache<?,?>> getSortedCachesByDependsReversed() {
        if (_sortedCachesReversed != null && !hasBeenModified()) {
            return _sortedCachesReversed;
        }
        _sortedCachesReversed = caches.values().stream().sorted().collect(Collectors.toList());
        return _sortedCachesReversed;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasBeenModified() {
        return caches.size() != _sortedCachesReversed.size();
    }

    /**
     * Adds the sync group and a '_' char to the beginning of the dbName,
     * to allow multiple sync group networks to operate on the same MongoDB server
     */
    public static @NotNull String getFullDatabaseName(String dbName) {
        // Just in case, don't add the sync group twice
        if (dbName.startsWith(EngineSource.getSyncServerGroup() + "_")) {
            return dbName;
        }
        return EngineSource.getSyncServerGroup() + "_" + dbName;
    }

    /**
     * Gets the sync ID of this server.
     */
    public static @NotNull String getSyncServerID() {
        return EngineSource.getSyncServerId();
    }

    /**
     * Gets the sync group of this server.
     */
    public static @NotNull String getSyncServerGroup() {
        return EngineSource.getSyncServerGroup();
    }

    /**
     * @return if SyncEngine is in debug mode
     */
    public static boolean isDebug() {
        return EngineSource.isDebug();
    }

    private static void registerDatabase(@NotNull Plugin owner, @NotNull String databaseName) throws DuplicateDatabaseException {
        @Nullable DatabaseRegistration registration = getDatabaseRegistration(databaseName);
        if (registration != null) {
            throw new DuplicateDatabaseException(registration);
        }
        databases.put(databaseName.toLowerCase(), new DatabaseRegistration(databaseName, owner));
    }

    private static @Nullable DatabaseRegistration getDatabaseRegistration(@NotNull String databaseName) {
        return databases.get(databaseName.toLowerCase());
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

        registerDatabase(plugin, getFullDatabaseName(databaseName));
        return new SyncRegistration(plugin, databaseName);
    }
}
