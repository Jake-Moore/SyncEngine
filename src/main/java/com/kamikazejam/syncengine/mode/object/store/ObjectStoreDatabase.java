package com.kamikazejam.syncengine.mode.object.store;

import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.store.SyncStore;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import com.kamikazejam.syncengine.mode.object.SyncObjectCache;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;

/**
 * Wraps up the StorageService with the Cache backing the Syncs, and exposes ObjectStore methods
 *
 * @param <X>
 */
public class ObjectStoreDatabase<X extends SyncObject> extends SyncStore<String, X> {

    private final StorageService storageService;

    public ObjectStoreDatabase(SyncObjectCache<X> cache) {
        super(cache);
        this.storageService = SyncEnginePlugin.get().getStorageService();
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
        return storageService.get(cache, key);
    }

    @Override
    protected boolean save(Cache<String, X> cache, @NotNull X sync) {
        return storageService.save(cache, sync);
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
    protected @NotNull Collection<X> getAll(Cache<String, X> cache) {
        return storageService.getAll(cache);
    }

    @Override
    protected long size(Cache<String, X> cache) {
        return storageService.size(cache);
    }
}
