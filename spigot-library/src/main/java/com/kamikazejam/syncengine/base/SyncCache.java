package com.kamikazejam.syncengine.base;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.KUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.exception.DuplicateCacheException;
import com.kamikazejam.syncengine.base.sync.CacheLoggerInstantiator;
import com.kamikazejam.syncengine.base.sync.SyncInstantiator;
import com.kamikazejam.syncengine.update.SyncUpdater;
import lombok.Getter;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * The abstract backbone of all Sync cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Getter
public abstract class SyncCache<K, X extends Sync<K>> implements Comparable<SyncCache<?, ?>>, Cache<K, X> {

    protected final Set<String> dependingCaches = new HashSet<>();
    protected final Class<K> keyClass;
    protected final Class<X> syncClass;
    protected final String name;

    protected final SyncRegistration registration;
    protected final Plugin plugin;
    protected final JavaPlugin syncPlugin;

    protected SyncUpdater<K, X> updater;
    protected LoggerService loggerService;
    protected SyncInstantiator<K, X> instantiator;
    protected boolean debug = true;
    protected boolean running = false;

    public SyncCache(SyncInstantiator<K, X> instantiator, String name, Class<K> key, Class<X> syncClass, SyncRegistration registration, CacheLoggerInstantiator logger) {
        this.instantiator = instantiator;
        this.name = name;
        this.keyClass = key;
        this.syncClass = syncClass;
        this.registration = registration;
        this.plugin = registration.getPlugin();
        this.syncPlugin = EngineSource.get();
        this.loggerService = logger.instantiate(this);
    }

    /**
     * Start the Cache
     * Should be called by the external plugin during startup after the cache has been created
     *
     * @return Boolean successful
     */
    @Override
    public final boolean start() {
        Preconditions.checkState(!running, "Cache " + name + " is already started!");
        Preconditions.checkNotNull(instantiator, "Instantiator must be set before calling start() for cache " + name);
        boolean success = true;
        if (!initialize()) {
            success = false;
            loggerService.error("Failed to initialize internally for cache: " + name);
        }
        updater = new SyncUpdater<>(this);
        if (!updater.start()) {
            success = false;
            loggerService.error("Failed to start SyncUpdater for cache: " + name);
        }
        internalStartAutosave();
        running = true;

        // Register this cache
        try {
            SyncEngineAPI.saveCache(this);
        } catch (DuplicateCacheException e) {
            loggerService.severe("[DuplicateCacheException] Failed to register cache: " + name + " - Cache Name already exists!");
            return false;
        }
        return success;
    }

    /**
     * Stop the Cache
     * Should be called by the external plugin during shutdown
     *
     * @return Boolean successful
     */
    public final boolean shutdown() {
        Preconditions.checkState(running, "Cache " + name + " is not running!");
        boolean success = true;

        // If this cache is a player cache, save all profiles of online players before we shut down
        // TODO - uncomment when Profiles are added
//        if (this instanceof SyncProfileCache<?>) {
//            SyncProfileCache<?> cache = (SyncProfileCache<?>) this;
//            Bukkit.getOnlinePlayers().forEach(p ->
//                    ProfileListener.networkedQuit(cache.getApi(), p, cache, false));
//        }

        // terminate() handles the rest of the cache shutdown
        if (!terminate()) {
            success = false;
            loggerService.info("Failed to terminate internally for cache: " + name);
        }

        if (updater != null) {
            if (updater.isRunning()) {
                if (!updater.shutdown()) {
                    success = false;
                    loggerService.info("Failed to shutdown SyncUpdater for cache: " + name);
                }
            }
        }

        running = false;
        internalShutdownAutosave();

        // Unregister this cache
        SyncEngineAPI.removeCache(this);
        return success;
    }

    public void internalStartAutosave() {
    }

    public void internalShutdownAutosave() {
    }

    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     */
    @ApiStatus.Internal
    protected abstract boolean initialize();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     */
    @ApiStatus.Internal
    protected abstract boolean terminate();

    @Override
    public boolean pushUpdate(@NotNull X sync, boolean forceLoad, boolean async) {
        Preconditions.checkNotNull(sync, "Sync cannot be null for pushUpdate");
        if (updater != null) {
            loggerService.debug("PUSH " + keyToString(sync.getId()) + " (v" + sync.getVersion() + "): Force=" + forceLoad);
            return updater.pushUpdate(sync, forceLoad, async);
        } else {
            loggerService.info("Couldn't pushUpdate for Sync " + keyToString(sync.getId()) + ": SyncUpdater is null!");
            return false;
        }
    }

    @NotNull
    @Override
    public final String getName() {
        return name;
    }

    @NotNull
    @Override
    public final Plugin getPlugin() {
        return plugin;
    }

    @NotNull
    @Override
    public SyncRegistration getRegistration() {
        return registration;
    }

    @Override
    public @NotNull String getDatabaseName() {
        return registration.getDatabaseName();
    }

    @Override
    public @NotNull String getDbNameShort() {
        return registration.getDbNameShort();
    }

    @Override
    @ApiStatus.Internal
    public final void updateSyncFromNewer(@NotNull X sync, @NotNull X update) {
        Preconditions.checkNotNull(sync);
        Preconditions.checkNotNull(update);

        // Load the version from the update sync (IFF it's newer)
        if (update.getVersion() < sync.getVersion()) {
            throw new IllegalStateException("[" + getName() + "] Update Sync is OLDER? Loading: " + keyToString(sync.getId()) + " from v" + sync.getVersion() + " to v" + update.getVersion());
        }
        sync.load(update);
        sync.setVersion(update.getVersion());
    }

    @Override
    public boolean save(@NotNull X sync) {
        Preconditions.checkNotNull(sync);
        if (sync.isReadOnly()) {
            KUtil.printStackTrace("Cannot save a read-only Sync, cache: " + getName() + " id: " + sync.getId());
        }

        cache(sync);
        boolean mongo = getDatabaseStore().save(sync);
        if (!mongo) {
            loggerService.info("Failed to save Sync " + keyToString(sync.getId()));
        }

        // Push update so other servers can load the new data
        if (mongo) {
            // Try async, but if we're not allowed to create an async task, just do it sync
            try {
                pushUpdate(sync, true, true);
            } catch (IllegalPluginAccessException e) {
                pushUpdate(sync, true, false);
            }
        }

        return mongo;
    }

    @Override
    public CompletableFuture<Boolean> saveAsync(@NotNull X sync) {
        Preconditions.checkNotNull(sync);
        if (sync.isReadOnly()) {
            KUtil.printStackTrace("Cannot save a read-only Sync, cache: " + getName() + " id: " + sync.getId());
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        runAsync(() -> future.complete(save(sync)));
        return future;
    }

    @Override
    public void cache(@NotNull X sync) {
        Preconditions.checkNotNull(sync);
        Optional<X> o = getLocalStore().get(sync.getId());
        if (o.isPresent()) {
            updateSyncFromNewer(o.get(), sync);
        } else {
            getLocalStore().save(sync);
        }
    }

    @Override
    public void uncache(@NotNull K key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
    }

    @Override
    public void uncache(@NotNull X sync) {
        Preconditions.checkNotNull(sync);
        getLocalStore().remove(sync);
    }

    @Override
    public void delete(@NotNull K key) {
        Preconditions.checkNotNull(key);
        getLocalStore().remove(key);
        getDatabaseStore().remove(key);
    }

    @Override
    public void delete(@NotNull X sync) {
        Preconditions.checkNotNull(sync);
        getLocalStore().remove(sync);
        getDatabaseStore().remove(sync);
    }

    @Override
    public boolean isCached(@NotNull K key) {
        Preconditions.checkNotNull(key);
        return getLocalStore().has(key);
    }

    @Override
    public @NotNull X create() {
        X x = instantiator.instantiate();
        x.initialized();
        return x;
    }

    @Override
    public void runAsync(@NotNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
    }

    @Override
    public void runSync(@NotNull Runnable runnable) {
        Preconditions.checkNotNull(runnable);
        plugin.getServer().getScheduler().runTask(plugin, runnable);
    }

    @Override
    public void addDepend(@NotNull Cache<?, ?> cache) {
        Preconditions.checkNotNull(cache);
        this.dependingCaches.add(cache.getName());
    }

    @Override
    public boolean isDependentOn(@NotNull Cache<?, ?> cache) {
        Preconditions.checkNotNull(cache);
        return dependingCaches.contains(cache.getName());
    }

    @Override
    public boolean isDependentOn(@NotNull String cacheName) {
        Preconditions.checkNotNull(cacheName);
        return dependingCaches.contains(cacheName);
    }

    /**
     * Simple comparator method to determine order between caches based on dependencies
     *
     * @param o The {@link SyncCache} to compare.
     * @return Comparator sorting integer
     */
    @Override
    public int compareTo(@NotNull SyncCache<?, ?> o) {
        Preconditions.checkNotNull(o);
        if (this.isDependentOn(o)) {
            return -1;
        } else if (o.isDependentOn(this)) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public @NotNull Set<String> getDependencyNames() {
        return dependingCaches;
    }

    // TODO
//    @Override @NotNull
//    public final RedisNetworkService getNetworkService() {
//        return SyncEnginePlugin.get().getNetworkService();
//    }
//
//    @Override
//    public <T extends SyncProfile> Optional<NetworkProfile> getNetworked(@NotNull T sync) {
//        Preconditions.checkNotNull(sync);
//        return getNetworked(sync.getUniqueId());
//    }
//    @Override
//    public Optional<NetworkProfile> getNetworked(@NotNull UUID key) {
//        return getNetworkService().get(key);
//    }

    @Override
    public @NotNull SyncInstantiator<K, X> getInstantiator() {
        return instantiator;
    }
}
