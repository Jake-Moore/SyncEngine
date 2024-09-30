package com.kamikazejam.syncengine.base.store;

import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.connections.storage.iterable.TransformingIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;

/**
 * Wraps up the StorageService with the Cache backing the Syncs, and exposes ObjectStore methods
 *
 * @param <X>
 */
public abstract class StoreDatabase<K, X extends Sync<K>> extends SyncStore<K, X> {

    private final StorageService storageService;

    public StoreDatabase(Cache<K, X> cache) {
        super(cache);
        this.storageService = EngineSource.getStorageService();
    }

    // ----------------------------------------------------- //
    //                   Abstract Methods                    //
    // ----------------------------------------------------- //
    public abstract @NotNull X callVersionMismatch(Cache<K, X> cache, @NotNull X sync, VersionMismatchException ex);



    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within SyncEngine.");
    }

    @Override
    public boolean isDatabase() {
        return true;
    }


    // ---------------------------------------------------------------- //
    //                  Map SyncStore to StorageService                 //
    // ---------------------------------------------------------------- //
    @Override
    protected Optional<X> get(Cache<K, X> cache, @NotNull K key) {
        // Fetch the Sync from the database
        Optional<X> o = storageService.get(cache, key);
        // Save Copy for VersionMismatchException handling
        o.ifPresent(s -> {
            s.setCache(cache);
            s.cacheCopy();
        });
        // Return the Sync
        return o;
    }

    @Override
    protected @NotNull TriState save(Cache<K, X> cache, @NotNull X sync) {
        // All saves to Database Storage run through here

        try {
            TriState state = storageService.save(cache, sync);
            // DB has been updated, we should update the cache copy
            if (state != TriState.FALSE) {
                sync.cacheCopy();
            }
            return state;
        }catch (VersionMismatchException ex) {
            // Handle VersionMismatchException
            @NotNull X updatedSync = this.callVersionMismatch(cache, sync, ex);
            // Save our new Sync version
            TriState state = this.save(cache, updatedSync);
            if (state != TriState.FALSE) {
                // If saved properly, update our local object
                cache.updateSyncFromNewer(sync, updatedSync);
                sync.cacheCopy(); // need to call this again since data changed
            }
            return state;
        }
    }

    @Override
    protected boolean has(Cache<K, X> cache, @NotNull K key) {
        return storageService.has(cache, key);
    }

    @Override
    protected boolean remove(Cache<K, X> cache, @NotNull K key) {
        return storageService.remove(cache, key);
    }

    @Override
    protected @NotNull Iterable<X> getAll(Cache<K, X> cache) {
        // Fetch the storageService's Iterable
        Iterator<X> storage = storageService.getAll(cache).iterator();
        return () -> new TransformingIterator<>(storage, x -> {
            // Make sure to set the cache and cacheCopy as we load the Syncs
            x.setCache(cache);
            x.cacheCopy();
            return x;
        });
    }

    @Override
    public @NotNull Iterable<K> getKeys() {
        return storageService.getKeys(cache);
    }

    @NotNull
    @Override
    public Iterable<String> getKeyStrings(@NotNull Cache<K, X> cache) {
        Iterator<K> keys = storageService.getKeys(cache).iterator();
        return () -> new TransformingIterator<>(keys, cache::keyToString);
    }

    @Override
    protected long size(Cache<K, X> cache) {
        return storageService.size(cache);
    }
}
