package com.kamikazejam.syncengine.mode.profile;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import com.kamikazejam.syncengine.base.error.CacheLoggerService;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.index.IndexedField;
import com.kamikazejam.syncengine.base.save.ProfileAutoSaveInstantiator;
import com.kamikazejam.syncengine.base.save.ProfileAutoSaveTask;
import com.kamikazejam.syncengine.base.store.StoreDatabase;
import com.kamikazejam.syncengine.base.sync.CacheLoggerInstantiator;
import com.kamikazejam.syncengine.base.sync.SyncInstantiator;
import com.kamikazejam.syncengine.base.update.UpdateTask;
import com.kamikazejam.syncengine.mode.profile.handshake.ProfileHandshakeService;
import com.kamikazejam.syncengine.mode.profile.loader.SyncProfileLoader;
import com.kamikazejam.syncengine.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.mode.profile.store.ProfileStoreDatabase;
import com.kamikazejam.syncengine.mode.profile.store.ProfileStoreLocal;
import com.kamikazejam.syncengine.mode.profile.update.ProfileUpdater;
import com.kamikazejam.syncengine.util.SyncFileLogger;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Getter
@SuppressWarnings("unused")
public abstract class SyncProfileCache<X extends SyncProfile> extends SyncCache<UUID, X> implements ProfileCache<X> {

    protected final ProfileAutoSaveTask<X> autoSaveTask;
    private final ConcurrentMap<UUID, SyncProfileLoader<X>> loaders = new ConcurrentHashMap<>();
    private final ProfileStoreLocal<X> localStore = new ProfileStoreLocal<>();
    private final ProfileStoreDatabase<X> databaseStore = new ProfileStoreDatabase<>(this);

    private final @NotNull ProfileHandshakeService<X> handshakeService;
    private final @NotNull ProfileUpdater<X> updater;

    public SyncProfileCache(SyncRegistration module, SyncInstantiator<UUID, X> instantiator, String name, Class<X> syncClass) {
        // Optional Constructor that will use the default CacheLoggerService
        this(module, instantiator, name, syncClass, CacheLoggerService::new, null);
    }

    public SyncProfileCache(SyncRegistration module, SyncInstantiator<UUID, X> instantiator, String name, Class<X> syncClass, CacheLoggerInstantiator logger, @Nullable ProfileAutoSaveInstantiator<X> autoSaveInstantiator) {
        super(instantiator, name, UUID.class, syncClass, module, logger);

        // Setup Handshake and Updater
        handshakeService = new ProfileHandshakeService<>(this);
        updater = new ProfileUpdater<>(this);

        // Create the auto-save task
        if (autoSaveInstantiator == null) {
            this.autoSaveTask = new ProfileAutoSaveTask<>(this);
        } else {
            this.autoSaveTask = autoSaveInstantiator.run(this);
        }

        // Start this cache
        if (!start()) {
            // Data loss is not tolerated in SyncEngine, shutdown to prevent issues
            syncPlugin.getLogger().severe("Failed to start Profile Cache: " + name);
            Bukkit.shutdown();
        }
    }

    @Override
    protected boolean initialize() {
        boolean success = true;
        if (!handshakeService.start()) {
            success = false;
            loggerService.info("Failed to start Profile Handshake Service (Network Node mode) for cache: " + name);
        }
        if (!updater.start()) {
            success = false;
            loggerService.info("Failed to start Profile Updater (Network Node mode) for cache: " + name);
        }
        return success;
    }

    @Override
    protected boolean terminate() {
        // Saving all Profiles makes sense since they should be backed by this instance
        boolean success = true;
        AtomicInteger failedSaves = new AtomicInteger(0);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            getFromCache(player).ifPresent(sync -> {
                if (!saveSynchronously(sync)) {
                    failedSaves.getAndIncrement();
                }
            });
        }
        if (failedSaves.get() > 0) {
            loggerService.info(failedSaves + " objects failed to save during shutdown");
        }

        // Shutdown Handshake and Updater
        if (handshakeService.isRunning() && !handshakeService.shutdown()) {
            success = false;
            loggerService.info("Failed to shutdown Profile Handshake Service (Network Node mode) for cache: " + name);
        }
        if (updater.isRunning() && !updater.shutdown()) {
            success = false;
            loggerService.info("Failed to shutdown Profile Updater (Network Node mode) for cache: " + name);
        }

        loaders.clear();
        // Clear locals store (frees memory)
        localStore.clear();
        // Don't clear database (can't)

        return success;
    }


    // ----------------------------------------------------- //
    //                         Cache                         //
    // ----------------------------------------------------- //
    @NotNull
    @Override
    public SyncProfileLoader<X> loader(@NotNull UUID key) {
        Preconditions.checkNotNull(key);
        return loaders.computeIfAbsent(key, s -> new SyncProfileLoader<>(this, s));
    }

    @Override
    public @NotNull StoreDatabase<UUID, X> getDatabaseStore() {
        return databaseStore;
    }

    @Override
    public @NotNull String keyToString(@NotNull UUID key) {
        return key.toString();
    }

    @Override
    public @NotNull UUID keyFromString(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return UUID.fromString(key);
    }

    public @NotNull X create(@NotNull Player player) {
        return this.create(player.getUniqueId());
    }

    @Override
    public @NotNull CacheSaveResult saveAllOnline() {
        AtomicInteger total = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
            @NotNull X sync = this.get(p);
            if (sync.isReadOnly()) {
                continue;
            }

            total.getAndIncrement();
            if (!this.saveSynchronously(sync)) {
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
    public boolean hasKey(@NotNull UUID key) {
        return localStore.has(key) || databaseStore.has(key);
    }

    @Override
    public Optional<X> getFromCache(@NotNull UUID key) {
        return this.localStore.get(key);
    }

    @Override
    public Optional<X> getFromDatabase(@NotNull UUID key, boolean cacheSync) {
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
    public @NotNull Iterable<UUID> getIDs() {
        return databaseStore.getKeys();
    }

    @Override
    public final void internalStartAutosave() {
        this.autoSaveTask.start();
    }

    @Override
    public void internalShutdownAutosave() {
        this.autoSaveTask.stop();
    }



    // ----------------------------------------------------- //
    //                     ProfileCache                      //
    // ----------------------------------------------------- //
    @NotNull
    @Override
    public X get(@NotNull Player player) {
        Preconditions.checkNotNull(player);
        SyncProfileLoader<X> loader = this.loader(player.getUniqueId());
        loader.setLogin(false);
        try {
            return loader.cacheOrCreate().get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    @Override
    public CompletableFuture<X> get(@NotNull UUID uniqueId) {
        Preconditions.checkNotNull(uniqueId);
        SyncProfileLoader<X> loader = this.loader(uniqueId);
        loader.setLogin(false);
        return loader.cacheOrCreate();
    }

    @NotNull
    @Override
    public Set<X> getOnline() {
        // Stream online players and map them to their SyncProfile
        return Bukkit.getOnlinePlayers().stream()
                .filter(PlayerUtil::isFullyValidPlayer)
                .map(this::get)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<X> getFromCache(@NotNull Player player) {
        Preconditions.checkNotNull(player);
        return getFromCache(player.getUniqueId());
    }

    @Override
    public Optional<X> getFromDatabase(@NotNull Player player, boolean cacheSync) {
        Preconditions.checkNotNull(player);
        Optional<X> o = databaseStore.get(player.getUniqueId());
        if (cacheSync) {
            o.ifPresent(this::cache);
        }
        return o;
    }
    
    // Need custom Profile handling for saves
    @Override
    public boolean saveSynchronously(@NotNull X sync) {
        Preconditions.checkNotNull(sync, "Cannot save a null SyncProfile");
        if (sync.isReadOnly()) {
            SyncFileLogger.warn(this, "Cannot save a read-only SyncProfile, cache: " + getName() + " id: " + sync.getId());
            return false;
        }
        if (!sync.isValid()) {
            SyncFileLogger.warn(this, "Cannot save an invalid SyncProfile, cache: " + getName() + " id: " + sync.getId());
            return false;
        }

        cache(sync);

        // Require a NetworkProfile for saving in NETWORK_NODE mode
        NetworkProfile np = EngineSource.getNetworkService().getOrCreate(sync);

        // If the NetworkProfile is online on this server, then we can save to mongo and update the NetworkProfile
        boolean sameSyncServerId = EngineSource.getSyncServerId().equalsIgnoreCase(np.getLastSeenServer());
        if (sameSyncServerId) {
            np.markSaved();
        }

        if (saveToDatabase(sync)) {
            // If we are saving a SyncProfile from a different server from the one that has the Player
            //   then we should notify that other server that data has changed, so it can pull changes
            if (!sameSyncServerId) {
                pushUpdate(sync.getId(), UpdateTask.PULL_FROM_STORE, true);
                return true;
            }

            // We are saving a SyncProfile on the same server as the Player, update their NetworkProfile
            if (EngineSource.getNetworkService().saveSync(np)) {
                return true;
            } else {
                loggerService.info("Failed to save profile " + sync.getUniqueId() + ": Couldn't save network profile (but saved normal profile)");
                return false;
            }
        } else {
            loggerService.info("Failed to save profile " + sync.getUniqueId() + ": Failed to save to database (via saveMongo())");
            return false;
        }
    }

    private boolean saveToDatabase(@NotNull X sync) {
        Preconditions.checkNotNull(sync, "Cannot save a null Sync (saveMongo)");
        TriState state = databaseStore.save(sync);
        if (state != TriState.FALSE) {
            sync.setSaveFailed(false);
            sync.setLastSaveTimestamp(System.currentTimeMillis());
            return true;
        } else {
            sync.setSaveFailed(true);
            return false;
        }
    }

    @Override
    public void removeLoader(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.loaders.remove(uuid);
    }

    @Override
    public abstract void onProfileLeavingLocal(@NotNull Player player, @NotNull X profile);

    @Override
    public abstract void onProfileLeavingGlobal(@NotNull Player player, @NotNull X profile);



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    @Override
    public <T> @NotNull CompletableFuture<X> getByIndex(@NotNull IndexedField<X, T> field, @NotNull T value) {
        // 1. -> Check local cache (brute force) - skip offline players (could be outdated/require a handshake)
        for (X sync : getLocalStore().getAll()) {
            if (field.equals(field.getValue(sync), value) && sync.isOnline()) {
                return CompletableFuture.completedFuture(sync);
            }
        }

        // 2. -> Check database (uses cache or mongodb)
        @Nullable UUID syncId = EngineSource.getStorageService().getSyncIdByIndex(this, field, value);
        if (syncId == null) {
            return CompletableFuture.completedFuture(null);
        }

        // 3. -> Obtain the Profile by its ID
        CompletableFuture<X> o = this.get(syncId);

        // 4. Adapt this future so we can peek its contents
        CompletableFuture<X> future = new CompletableFuture<>();
        o.whenComplete((s, t) -> {
            if (t != null) {
                future.completeExceptionally(t);
                return;
            }

            // If we have a SyncProfile and the field matches the value, then we have our result
            if (s != null && field.equals(field.getValue(s), value)) {
                future.complete(s);
            } else {
                // Otherwise something changed in the Sync and we should not return it
                // This can happen if:
                //    The local copy had its field changed
                //    and those changes were not saved to DB or Index Cache
                // This is not considered an error
                future.complete(null);
            }
        });
        return future;
    }
}
