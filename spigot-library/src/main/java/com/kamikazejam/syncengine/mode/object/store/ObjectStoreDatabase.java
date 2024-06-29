package com.kamikazejam.syncengine.mode.object.store;

import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.store.SyncStore;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import com.kamikazejam.syncengine.mode.object.SyncObjectCache;
import com.kamikazejam.syncengine.util.VersionMismatchHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

/**
 * Wraps up the StorageService with the Cache backing the Syncs, and exposes ObjectStore methods
 *
 * @param <X>
 */
public class ObjectStoreDatabase<X extends SyncObject> extends SyncStore<String, X> {

    private final StorageService storageService;

    public ObjectStoreDatabase(SyncObjectCache<X> cache) {
        super(cache);
        this.storageService = EngineSource.getStorageService();
    }

    @Override
    public long clear() {
        // For safety reasons...
        throw new UnsupportedOperationException("Cannot clear a MongoDB database from within SyncEngine.");
    }

    @NotNull
    @Override
    public String getStoreLayer() {
        return "Object MongoDB";
    }

    @Override
    public boolean isDatabase() {
        return true;
    }


    // ---------------------------------------------------------------- //
    //                  Map SyncStore to StorageService                 //
    // ---------------------------------------------------------------- //
    @Override
    protected Optional<X> get(Cache<String, X> cache, @NotNull String key) {
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
    protected boolean save(Cache<String, X> cache, @NotNull X sync) {
        // All saves to Database Storage run through here

        try {
            boolean b = storageService.save(cache, sync);
            // DB has been updated, we should update the cache copy
            if (b) {
                sync.cacheCopy();
            }
            return b;
        }catch (VersionMismatchException ex) {
            // Handle VersionMismatchException
            @NotNull X updatedSync = VersionMismatchHandler.handObjectException(this::get, cache, sync, ex);
            // Save our new Sync version
            boolean b = this.save(cache, updatedSync);
            if (b) {
                // If saved properly, update our local object
                cache.updateSyncFromNewer(sync, updatedSync);
                sync.cacheCopy(); // need to call this again since data changed
            }
            return b;
        }
    }

    @Override
    protected boolean has(Cache<String, X> cache, @NotNull String key) {
        return storageService.has(cache, key);
    }

    @Override
    protected boolean remove(Cache<String, X> cache, @NotNull String key) {
        return storageService.remove(cache, key);
    }

    @Override
    protected @NotNull Iterable<X> getAll(Cache<String, X> cache) {
        // Fetch the storageService's Iterable
        Iterator<X> storage = storageService.getAll(cache).iterator();

        // Adapt the Storage Iterable to ensure the Sync has its dbJson set
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                return storage.hasNext();
            }
            @Override
            public X next() {
                X next = storage.next();
                // Save Copy for VersionMismatchException handling
                next.setCache(cache);
                next.cacheCopy();
                return next;
            }
        };
    }

    @Override
    public @NotNull Set<String> getKeys() {
        return storageService.getKeys(cache);
    }

    @Override
    protected long size(Cache<String, X> cache) {
        return storageService.size(cache);
    }
}
