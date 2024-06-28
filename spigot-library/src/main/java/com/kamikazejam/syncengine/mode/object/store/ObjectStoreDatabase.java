package com.kamikazejam.syncengine.mode.object.store;

import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.store.SyncStore;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import com.kamikazejam.syncengine.mode.object.SyncObjectCache;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.ReflectionUtil;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Objects;
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
            s.saveCacheCopy();
        });
        // Return the Sync
        return o;
    }

    @Override
    protected boolean save(Cache<String, X> cache, @NotNull X sync) {
        // All saves to Database Storage run through here

        try {
            return storageService.save(cache, sync);
        }catch (VersionMismatchException ex) {

            // We can fetch the last known CachedCopy of this Sync, and use it to compare fields
            Sync<String> cachedCopy = sync.getCachedCopy();
            // Also fetch the current database version
            X database = this.get(cache, sync.getSyncId()).orElseThrow(() -> new IllegalStateException("Sync not found in database in VersionMismatchException!?"));
            long newVer = database.getVersion();
            boolean changed = (newVer != sync.getVersion());

            // Use reflection to grab all fields and compare them
            //  The extra cost of reflection is accepted as this is a repair operation (should be async)
            Bukkit.getLogger().info(" ");
            Bukkit.getLogger().info("VersionMismatchException - Detecting Field Changes:");
            for (Field field : ReflectionUtil.getAllFields(cache.getSyncClass())) {
                // Skip the version field
                if (field.getName().equals("version")) { continue; }

                try {
                    field.setAccessible(true);
                    Object cachedValue = field.get(cachedCopy);
                    Object currentValue = field.get(sync);

                    // Use Jackson since .equals might not work for all types
                    if (!Objects.equals(JacksonUtil.toJson(cachedValue), JacksonUtil.toJson(currentValue))) {
                        // Update the database object with the current value
                        field.set(database, currentValue);
                        System.out.println("\tUpdated field " + field.getName() + " from " + cachedValue + " to " + currentValue);
                        changed = true;
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            // Make sure the version matches db, so save will work
            database.setVersion(newVer);
            if (!changed) {
                throw new IllegalStateException("VersionMismatchException but no changes detected!?");
            }
            Bukkit.getLogger().info("Field Comparison Completed.");
            Bukkit.getLogger().info(" ");

            return this.save(cache, database);
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
                next.saveCacheCopy();
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
