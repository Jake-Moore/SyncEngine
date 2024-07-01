package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Defines the minimum set of methods all Storage services must implement.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public abstract class StorageService extends LoggerService implements Service {

    /**
     * Save a Sync to this store. Requires the cache it belongs to.
     * Implementations of this class should handle optimistic versioning and throw errors accordingly.
     *
     * @throws VersionMismatchException if
     * @return if the Sync was saved successfully.
     */
    public abstract <K, X extends Sync<K>> boolean save(Cache<K, X> cache, X sync) throws VersionMismatchException;

    /**
     * Retrieve a Sync from this store. Requires the cache to fetch it from.
     */
    @NotNull
    public abstract <K, X extends Sync<K>> Optional<X> get(Cache<K, X> cache, K key);

    /**
     * @return How many Syncs are stored in a cache within this store.
     */
    public abstract <K, X extends Sync<K>> long size(Cache<K, X> cache);

    /**
     * Check if a Sync is stored in a given cache.
     */
    public abstract <K, X extends Sync<K>> boolean has(Cache<K, X> cache, K key);

    /**
     * Remove a Sync from a given cache.
     */
    public abstract <K, X extends Sync<K>> boolean remove(Cache<K, X> cache, K key);

    /**
     * Retrieve all Syncs from a specific cache.
     */
    public abstract <K, X extends Sync<K>> Iterable<X> getAll(Cache<K, X> cache);

    /**
     * Retrieve all Sync keys from a specific cache.
     */
    public abstract <K, X extends Sync<K>> Iterable<K> getKeys(Cache<K, X> cache);

    /**
     * @return If the StorageService is ready to be used for a cache.
     */
    public abstract <K, X extends Sync<K>> boolean canCache();

}
