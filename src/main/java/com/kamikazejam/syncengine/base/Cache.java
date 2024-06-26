package com.kamikazejam.syncengine.base;

import com.kamikazejam.syncengine.SyncRegistration;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import com.kamikazejam.syncengine.base.cache.SyncLoader;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.base.sync.SyncQueryModifier;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * A Cache holds Sync objects and manages their retrieval, caching, and saving.
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Cache<K, X extends Sync<K>> extends Service {

    /**
     * Gets the name of this cache.
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
    boolean save(@NotNull X sync);

    /**
     * Save a Sync to this cache and to the database asynchronously.
     *
     * @param sync The Sync to save.
     * @return A future that supplies if the Sync was saved.
     */
    CompletableFuture<Boolean> saveAsync(@NotNull X sync);

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
     * Retrieves ALL Syncs from the database. Optionally caches them.
     */
    @Blocking
    @NotNull
    Collection<X> getAll(boolean cacheSyncs);

    /**
     * Retrieves ALL Syncs that match the query in the database. Optionally caches them.
     */
    @NotNull
    Collection<X> getAll(boolean cacheSyncs, Set<SyncQueryModifier<X>> modifiers);

    /**
     * Retrieves ALL Syncs from the database, then sorts them by the comparator. Optionally caches them.
     *
     * @return a List to maintain order.
     */
    @Blocking
    @NotNull
    List<X> getAll(boolean cacheSyncs, Comparator<? super X> orderBy);

    /**
     * Retrieves ALL Syncs that match the query in the database, then sorts them by the comparator. Optionally caches them.
     *
     * @return a List to maintain order.
     */
    @NotNull
    List<X> getAll(boolean cacheSyncs, Comparator<? super X> orderBy, Set<SyncQueryModifier<X>> modifiers);

    /**
     * Gets all Sync objects that are in this cache.
     */
    @NotNull
    Collection<X> getCached();

    /**
     * Gets the {@link LoggerService} for this cache. For logging purposes.
     */
    @NotNull
    LoggerService getErrorService();

    /**
     * Sets the {@link LoggerService} for this cache.
     */
    void setErrorService(@NotNull LoggerService loggerService);

    /**
     * Saves all Sync objects in this cache to the database.
     *
     * @return a {@link CacheSaveResult} with information about how many objects were saved.
     */
    @NotNull
    CacheSaveResult saveAll();

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
     * Get the number of Sync objects currently stored locally in this cache
     */
    int getCacheSize();

    /**
     * Creates a new Sync object with a random key.
     */
    @NotNull
    X create();

    /**
     * Creates a new Sync object with the provided key.
     */
    @NotNull
    X create(@NotNull K key);

    /**
     * Gets the {@link SyncLoader} for the provided key.
     */
    @NotNull
    SyncLoader<X> controller(@NotNull K key);

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
}
