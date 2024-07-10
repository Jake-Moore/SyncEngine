package com.kamikazejam.syncengine.base;

import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.cache.SyncLoader;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.exception.DuplicateCacheException;
import com.kamikazejam.syncengine.base.index.IndexedField;
import com.kamikazejam.syncengine.base.store.StoreMethods;
import com.kamikazejam.syncengine.base.sync.SyncInstantiator;
import com.kamikazejam.syncengine.mode.object.ObjectCache;
import com.kamikazejam.syncengine.mode.profile.ProfileCache;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.mode.profile.network.profile.store.NetworkProfileStore;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * A Cache holds Sync objects and manages their retrieval, caching, and saving.
 * Getters vary by Sync type, they are defined in the sync-specific interfaces:
 * {@link ObjectCache} and {@link ProfileCache}
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Cache<K, X extends Sync<K>> extends Service {

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A {@link DuplicateCacheException} error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    @NotNull
    String getName();

    /**
     * Retrieve a Sync from this cache. (Does not query the database)
     *
     * @return The Sync if it was cached.
     */
    Optional<X> getFromCache(@NotNull K key);

    /**
     * Retrieve a Sync from the database. (Force queries the database, and updates this cache)
     *
     * @param cacheSync If we should cache the Sync upon retrieval. (if it was found)
     * @return The Sync if it was found in the database.
     */
    Optional<X> getFromDatabase(@NotNull K key, boolean cacheSync);

    /**
     * Save a Sync to this cache and to the database.
     *
     * @param sync The Sync to save.
     * @return If the Sync was saved.
     */
    @Blocking
    boolean saveSynchronously(@NotNull X sync);

    /**
     * Save a Sync to this cache and to the database asynchronously.
     *
     * @param sync The Sync to save.
     * @return A future that supplies if the Sync was saved.
     */
    @NonBlocking
    CompletableFuture<Boolean> save(@NotNull X sync);

    /**
     * Adds a Sync to this cache.
     */
    void cache(@NotNull X sync);

    /**
     * Removes a Sync from this cache.
     */
    void uncache(@NotNull K key);

    /**
     * Removes a Sync from this cache.
     */
    void uncache(@NotNull X sync);

    /**
     * Deletes a Sync from this cache and from the database.
     */
    void delete(@NotNull K key);

    /**
     * Deletes a Sync from this cache and from the database.
     */
    void delete(@NotNull X sync);

    /**
     * Checks if a Sync is in this Cache.
     *
     * @return True if the Sync is cached. False if not (for instance if it was deleted)
     */
    boolean isCached(@NotNull K key);

    /**
     * Retrieves ALL Sync IDs from the database.
     * @return An Iterable of all Syncs, for sequential processing.
     */
    @Blocking
    @NotNull
    Iterable<K> getIDs();

    /**
     * Gets all Sync objects that are in this cache.
     */
    @NotNull
    Collection<X> getCached();

    /**
     * Gets the {@link LoggerService} for this cache. For logging purposes.
     */
    @NotNull
    LoggerService getLoggerService();

    /**
     * Sets the {@link LoggerService} for this cache.
     */
    void setLoggerService(@NotNull LoggerService loggerService);

    /**
     * Gets the {@link StoreMethods} that handles local storage for this cache.
     */
    @NotNull
    StoreMethods<K, X> getLocalStore();

    /**
     * Gets the {@link StoreMethods} that handles database storage for this cache.
     */
    @NotNull
    StoreMethods<K, X> getDatabaseStore();

    /**
     * Gets the plugin that set up this cache.
     */
    @NotNull
    Plugin getPlugin();

    /**
     * Gets the registration the parent plugin used to create this cache.
     */
    @NotNull
    SyncRegistration getRegistration();

    /**
     * Return the name of actual the MongoDB database this cache is stored in
     * This is different from the developer supplied db name, and is calculated from
     * {@link com.kamikazejam.syncengine.SyncEngineAPI#getFullDatabaseName(String)}.
     */
    @NotNull
    String getDatabaseName();

    @NotNull @ApiStatus.Internal
    String getDbNameShort();

    /**
     * Converts a Cache key to a string. Key uniqueness should be maintained.
     */
    @NotNull
    String keyToString(@NotNull K key);

    /**
     * Converts a string to a Cache key. Key uniqueness should be maintained.
     */
    @NotNull
    K keyFromString(@NotNull String key);

    /**
     * Add a dependency on another Cache. This Cache will be loaded after the dependency.
     */
    void addDepend(@NotNull Cache<?, ?> cache);

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    boolean isDependentOn(@NotNull Cache<?, ?> cache);

    /**
     * Check if this Cache is dependent on the provided cache.
     */
    boolean isDependentOn(@NotNull String cacheName);

    /**
     * Gets the name of all Cache objects this Cache is dependent on.
     */
    @NotNull
    Set<String> getDependencyNames();

    /**
     * Helper method to use the {@link #getPlugin()} plugin to run an async bukkit task.
     */
    @ApiStatus.Internal
    void runAsync(@NotNull Runnable runnable);

    /**
     * Helper method to use the {@link #getPlugin()} plugin to run a sync bukkit task.
     */
    @ApiStatus.Internal
    void runSync(@NotNull Runnable runnable);

    /**
     * Get the number of Sync objects currently stored locally in this cache
     */
    long getLocalCacheSize();

    /**
     * Creates a new Sync object with the provided key.
     */
    @NotNull
    X create(@NotNull K key);

    /**
     * @return True iff the cache contains a Sync with the provided key.
     */
    boolean hasKey(@NotNull K key);

    /**
     * Gets the {@link SyncLoader} for the provided key.
     */
    @NotNull
    SyncLoader<X> loader(@NotNull K key);

    /**
     * Push (through the Redis network) an update of this Sync.
     * This is used internally to notify other servers of changes.
     *
     * @param forceLoad If true, receiving servers are forced to load this Sync. If false, they will only load if they had it cached.
     * @param async     If we should send the redis message asynchronously.
     * @return If the redis message could be sent.
     */
    @ApiStatus.Internal
    boolean pushUpdate(@NotNull X sync, boolean forceLoad, boolean async);

    @NotNull
    NetworkProfileStore getNetworkStore();

    @NotNull
    Optional<NetworkProfile> getNetworked(@NotNull UUID key);

    @NotNull
    <T extends SyncProfile> Optional<NetworkProfile> getNetworked(@NotNull T sync);

    /**
     * Internal method used by SyncEngine to forcefully update a local instance of a Sync object with a newer one,
     * allowing your references to the existing Sync to remain intact and up-to-date.
     * Note that this only effects persistent (non-transient) fields.
     *
     * @param sync   The Sync to update
     * @param update The newer version of said Sync to replace the values of {@param sync} with.
     */
    @ApiStatus.Internal
    void updateSyncFromNewer(@NotNull X sync, @NotNull X update);

    /**
     * Gets the class of the sync object this cache is associated with.
     */
    @ApiStatus.Internal
    @NotNull
    Class<X> getSyncClass();

    /**
     * Returns the SyncInstantiator for the Sync object in this cache.
     */
    @NotNull SyncInstantiator<K, X> getInstantiator();



    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    /**
     * Register an index for this cache.
     * @return The registered index (for chaining)
     */
    <T> IndexedField<X, T> registerIndex(@NotNull IndexedField<X, T> field);

    /**
     * Updates the indexes cache with the provided Sync object.
     */
    @ApiStatus.Internal
    void cacheIndexes(@NotNull X sync, boolean save);

    /**
     * Saves the index cache to storage.
     */
    @ApiStatus.Internal
    void saveIndexCache();

}

