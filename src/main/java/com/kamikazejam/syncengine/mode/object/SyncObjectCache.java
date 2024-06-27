package com.kamikazejam.syncengine.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import com.kamikazejam.syncengine.base.error.CacheLoggerService;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.store.StoreMethods;
import com.kamikazejam.syncengine.base.sync.CacheLoggerInstantiator;
import com.kamikazejam.syncengine.base.sync.SyncInstantiator;
import com.kamikazejam.syncengine.mode.object.store.ObjectStoreDatabase;
import com.kamikazejam.syncengine.mode.object.store.ObjectStoreLocal;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
@SuppressWarnings("unused")
public abstract class SyncObjectCache<X extends SyncObject> extends SyncCache<String, X> implements com.kamikazejam.syncengine.base.Cache<String, X> {
    private final ConcurrentMap<String, SyncObjectLoader<X>> controllers = new ConcurrentHashMap<>();
    private final StoreMethods<String, X> localStore = new ObjectStoreLocal<>();
    private final StoreMethods<String, X> databaseStore = new ObjectStoreDatabase<>(this);

    public SyncObjectCache(SyncRegistration module, SyncInstantiator<String, X> instantiator, String name, Class<X> syncClass) {
        // Optional Constructor that will use the default CacheLoggerService
        this(module, instantiator, name, syncClass, CacheLoggerService::new);
    }
    public SyncObjectCache(SyncRegistration module, SyncInstantiator<String, X> instantiator, String name, Class<X> syncClass, CacheLoggerInstantiator logger) {
        super(instantiator, name, String.class, syncClass, module, logger);

        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in SyncEngine, shutdown to prevent issues
            syncPlugin.getLogger().severe("Failed to start Object Cache: " + name);
            Bukkit.getServer().shutdown();
        }
    }

    @Override
    protected boolean initialize() {
        return true;
    }

    @Override
    protected boolean terminate() {
        // Saving all on shutdown from an arbitrary instance seems like a way to have save collisions
        //  It's the user's responsibility to save their objects when changed, which will trigger sync across instances
//        AtomicInteger failedSaves = new AtomicInteger(0);
//        getCached().forEach(sync -> {
//            if (!save(sync)) {
//                failedSaves.getAndIncrement();
//            }
//        });
//        if (failedSaves.get() > 0) {
//            loggerService.info(failedSaves + " objects failed to save during shutdown");
//            success = false;
//        }

        controllers.clear();
        // Clear local store (frees memory)
        localStore.clear();
        // Don't clear database (can't)

        return true;
    }

    @NotNull
    @Override
    public SyncObjectLoader<X> controller(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return controllers.computeIfAbsent(key, s -> new SyncObjectLoader<>(this, s));
    }

    @NotNull
    @Override
    public StoreMethods<String, X> getDatabaseStore() {
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

    // Used Internally by ObjectStoreMongo to obtain a new instance, but not one we'd ever use for anything
    @ApiStatus.Internal
    public X emptyCreate() {
        return instantiator.instantiate();
    }

    @Override
    public @NotNull X create() {
        return create(UUID.randomUUID());
    }

    public @NotNull X create(UUID uuid) {
        return create(uuid.toString());
    }

    @Override
    public @NotNull X create(@NotNull String id) {
        X o = super.create();
        o.setId(id);
        o.setCache(this);
        // Cache and save this object
        cache(o);
        o.save();
        return o;
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
        return controller(key).fetch(saveToLocalCache);
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
        // Create an Iterable that iterates through all database objects, and updates local objects as necessary
        Iterator<X> databaseIterable = databaseStore.getAll().iterator();
        return () -> new Iterator<>() {
            @Override
            public boolean hasNext() {
                // This Iterable extends the database Iterable
                return databaseIterable.hasNext();
            }

            @Override
            public X next() {
                // Load the next database object
                @NotNull X next = databaseIterable.next();

                // Load the local object, and if it exists -> update it from newer (database ver)
                Optional<X> o = localStore.get(next.getId());
                o.ifPresent(sync -> {
                    // Don't attempt to load database version if its older
                    if (next.getVersion() < sync.getVersion()) { return; }
                    SyncObjectCache.this.updateSyncFromNewer(sync, next);
                });
                // Find the sync object to return
                @NotNull X sync = o.orElse(next);

                // Verify it has the correct cache and cache it if necessary
                next.setCache(SyncObjectCache.this);
                if (o.isEmpty() && cacheSyncs) {
                    // Won't call updateSyncFromNewer since o is empty
                    SyncObjectCache.this.cache(next);
                }
                return sync;
            }
        };
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
            if (!save(object)) {
                failures.getAndIncrement();
            }
        }
        return new CacheSaveResult(total.get(), failures.get());
    }

    @NotNull
    @Override
    public Collection<X> getCached() {
        return ((ObjectStoreLocal<X>) localStore).getLocalCache().values();
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
}
