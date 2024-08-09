package com.kamikazejam.syncengine.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import com.kamikazejam.syncengine.base.error.CacheLoggerService;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.index.IndexedField;
import com.kamikazejam.syncengine.base.store.StoreDatabase;
import com.kamikazejam.syncengine.base.sync.CacheLoggerInstantiator;
import com.kamikazejam.syncengine.base.sync.SyncInstantiator;
import com.kamikazejam.syncengine.connections.storage.iterable.TransformingIterator;
import com.kamikazejam.syncengine.mode.object.store.ObjectStoreDatabase;
import com.kamikazejam.syncengine.mode.object.store.ObjectStoreLocal;
import com.kamikazejam.syncengine.mode.object.update.ObjectUpdater;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@SuppressWarnings("unused")
public abstract class SyncObjectCache<X extends SyncObject> extends SyncCache<String, X> implements ObjectCache<X> {
    private final ConcurrentMap<String, SyncObjectLoader<X>> loaders = new ConcurrentHashMap<>();
    private final ObjectStoreLocal<X> localStore = new ObjectStoreLocal<>();
    private final ObjectStoreDatabase<X> databaseStore = new ObjectStoreDatabase<>(this);
    private final @NotNull ObjectUpdater<X> updater;

    public SyncObjectCache(SyncRegistration module, SyncInstantiator<String, X> instantiator, String name, Class<X> syncClass) {
        // Optional Constructor that will use the default CacheLoggerService
        this(module, instantiator, name, syncClass, CacheLoggerService::new);
    }
    public SyncObjectCache(SyncRegistration module, SyncInstantiator<String, X> instantiator, String name, Class<X> syncClass, CacheLoggerInstantiator logger) {
        super(instantiator, name, String.class, syncClass, module, logger);

        // Start updater
        updater = new ObjectUpdater<>(this);

        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in SyncEngine, shutdown to prevent issues
            syncPlugin.getLogger().severe("Failed to start Object Cache: " + name);
            Bukkit.shutdown();
        }
    }

    @Override
    protected boolean initialize() {
        if (!updater.start()) {
            loggerService.error("Failed to start SyncUpdater for cache: " + name);
            return false;
        }
        return true;
    }

    @Override
    protected boolean terminate() {
        // Don't save -> saving all on shutdown from an arbitrary instance seems like a way to have save collisions
        //  It's the user's responsibility to save their objects when changed, which will trigger sync across instances
        boolean success = true;

        // Shutdown Updater
        if (updater.isRunning() && !updater.shutdown()) {
            success = false;
            loggerService.info("Failed to shutdown ObjectUpdater for cache: " + name);
        }

        loaders.clear();
        // Clear local store (frees memory)
        localStore.clear();
        // Don't clear database (can't)

        return success;
    }

    @NotNull
    @Override
    public SyncObjectLoader<X> loader(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return loaders.computeIfAbsent(key, s -> new SyncObjectLoader<>(this, s));
    }

    @NotNull
    @Override
    public StoreDatabase<String, X> getDatabaseStore() {
        return databaseStore;
    }

    @Override
    public @NotNull String keyToString(@NotNull String key) {
        return key;
    }

    @Override
    public @NotNull String keyFromString(@NotNull String key) {
        return key;
    }

    public @NotNull X create(UUID uuid) {
        return create(uuid.toString());
    }

    @NotNull
    public final Optional<X> get(@NotNull UUID key) {
        return this.get(key.toString());
    }

    @NotNull
    @Override
    public final Optional<X> get(@Nullable String key) {
        if (key == null) {
            return Optional.empty();
        }
        // Save to local cache if we don't have any modifiers specifying specific instance modifiers
        return get(key, true);
    }

    @NotNull
    public final Optional<X> get(@NotNull UUID key, boolean saveToLocalCache) {
        return this.get(key.toString(), saveToLocalCache);
    }

    @NotNull
    @Override
    public final Optional<X> get(@Nullable String key, boolean saveToLocalCache) {
        if (key == null) {
            return Optional.empty();
        }
        return loader(key).fetch(saveToLocalCache);
    }

    @NotNull
    @Override
    public X getOrCreate(@NotNull String key) {
        Preconditions.checkNotNull(key);
        @NotNull Optional<X> x = get(key, true);
        return x.orElseGet(() -> create(key));
    }

    @NotNull
    public X getOrCreate(@NotNull UUID key) {
        Preconditions.checkNotNull(key);
        return getOrCreate(key.toString());
    }

    @NotNull
    @Override
    public Iterable<X> getAll(boolean cacheSyncs) {
        Iterator<String> keysIterable = databaseStore.getKeys().iterator();

        // Create an Iterable that iterates through all database keys, transforming into Syncs
        return () -> new TransformingIterator<>(keysIterable, key -> {
            // 1. If we have the object in the cache -> return it
            Optional<X> o = localStore.get(key);
            if (o.isPresent()) {
                return o.get();
            }

            // 2. We don't have the object in the cache -> load it from the database
            // If for some reason this was deleted, or not found, we can just return null
            //  and the TransformingIterator will skip it
            Optional<X> db = databaseStore.get(key);
            if (db.isEmpty()) {
                return null;
            }

            // Optionally cache this loaded Sync
            if (cacheSyncs) {
                SyncObjectCache.this.cache(db.get());
            }
            return db.get();
        });
    }

    @Override
    public @NotNull Iterable<X> getAllFromDatabase(boolean cacheSyncs) {
        // Create an Iterable that iterates through all database objects, and updates local objects as necessary
        Iterator<X> dbIterator = databaseStore.getAll().iterator();
        return () -> new TransformingIterator<>(dbIterator, dbSync -> {
            // Load the local object
            Optional<X> local = localStore.get(dbSync.getId());

            // If we want to cache, and have a local sync that's newer -> update the local sync
            // Note, if not caching then we won't update any local syncs and won't cache the db sync
            if (cacheSyncs && local.isPresent() && dbSync.getVersion() >= local.get().getVersion()) {
                SyncObjectCache.this.updateSyncFromNewer(local.get(), dbSync);
                SyncObjectCache.this.cache(dbSync);
            }

            // Find the sync object to return
            @NotNull X ret = local.orElse(dbSync);
            // Verify it has the correct cache and cache it if necessary
            ret.setCache(SyncObjectCache.this);
            return ret;
        });
    }

    @Override
    public @NotNull CacheSaveResult saveAll() {
        AtomicInteger total = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        for (X object : localStore.getAll()) {
            if (object.isReadOnly()) {
                continue;
            }

            total.getAndIncrement();
            if (!saveSynchronously(object)) {
                failures.getAndIncrement();
            }
        }
        return new CacheSaveResult(total.get(), failures.get());
    }

    @NotNull
    @Override
    public Collection<X> getCached() {
        return localStore.getLocalCache().values();
    }

    @Override
    public boolean hasKey(@NotNull String key) {
        return localStore.has(key) || databaseStore.has(key);
    }

    @Override
    public Optional<X> getFromCache(@NotNull String key) {
        return localStore.get(key);
    }

    @Override
    public Optional<X> getFromDatabase(@NotNull String key, boolean cacheSync) {
        Optional<X> o = databaseStore.get(key);
        if (cacheSync) {
            o.ifPresent(this::cache);
        }
        return o;
    }

    @Override
    public void setLoggerService(@NotNull LoggerService loggerService) {
        this.loggerService = loggerService;
    }

    @Override
    public long getLocalCacheSize() {
        return localStore.size();
    }

    @Override
    public @NotNull Iterable<String> getIDs() {
        return databaseStore.getKeys();
    }



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    @Override
    public <T> @NotNull Optional<X> getByIndex(@NotNull IndexedField<X, T> field, @NotNull T value) {
        // 1. -> Check local cache (brute force)
        for (X sync : getLocalStore().getAll()) {
            if (field.equals(field.getValue(sync), value)) {
                return Optional.of(sync);
            }
        }

        // 2. -> Check database (uses cache or mongodb)
        @Nullable String syncId = EngineSource.getStorageService().getSyncIdByIndex(this, field, value);
        if (syncId == null) {
            return Optional.empty();
        }

        // 3. -> Obtain the Profile by its ID
        Optional<X> o = this.get(syncId);
        if (o.isPresent() && !field.equals(value, field.getValue(o.get()))) {
            // This can happen if:
            //    The local copy had its field changed
            //    and those changes were not saved to DB or Index Cache
            // This is not considered an error, but we should return empty
            return Optional.empty();
        }

        // Either the Optional is empty or the Sync has the correct value -> return
        return o;
    }
}
