package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.index.IndexedField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
     * @return if the Sync was saved successfully. (NOT_SET if we didn't have changes to save)
     */
    @NotNull
    public abstract <K, X extends Sync<K>> TriState save(Cache<K, X> cache, X sync) throws VersionMismatchException;

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
    public abstract boolean canCache();

    /**
     * Called when a cache is registered with the SyncEngine -> meant for internal initialization.
     */
    public abstract <K, X extends Sync<K>> void onRegisteredCache(Cache<K, X> cache);


    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //
    public abstract <K, X extends Sync<K>, T> void registerIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index);
    public abstract <K, X extends Sync<K>> void cacheIndexes(@NotNull SyncCache<K, X> cache, @NotNull X sync, boolean updateFile);
    public abstract <K, X extends Sync<K>> void saveIndexCache(@NotNull SyncCache<K, X> cache);
    public abstract <K, X extends Sync<K>, T> @Nullable K getSyncIdByIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index, T value);
    public abstract <K, X extends Sync<K>> void invalidateIndexes(@NotNull SyncCache<K, X> cache, @NotNull K syncId, boolean updateFile);

}
