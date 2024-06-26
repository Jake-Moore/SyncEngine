package com.kamikazejam.syncengine;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.DuplicateCacheException;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("unused")
public class SyncEngineAPI {
    @Getter
    private static final ConcurrentMap<String, Cache<?, ?>> caches = new ConcurrentHashMap<>();

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
        return SyncEnginePlugin.get().getSyncGroup() + "_" + dbName;
    }

    /**
     * Gets the sync ID of this server.
     */
    public static @NotNull String getSyncID() {
        return SyncEnginePlugin.get().getSyncId();
    }

    /**
     * Gets the sync group of this server.
     */
    public static @NotNull String getSyncGroup() {
        return SyncEnginePlugin.get().getSyncGroup();
    }

    /**
     * @return if SyncEngine is in debug mode
     */
    public static boolean isDebug() {
        return SyncEnginePlugin.get().isDebug();
    }
}
