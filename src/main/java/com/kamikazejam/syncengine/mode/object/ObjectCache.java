package com.kamikazejam.syncengine.mode.object;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import com.kamikazejam.syncengine.base.index.IndexedField;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Defines Object-specific getters for SyncObjects. They return non-null Optionals.
 */
@SuppressWarnings("unused")
public interface ObjectCache<X extends SyncObject> extends Cache<String, X> {
    /**
     * Get a Sync object from this cache (will load from DB if necessary)
     * See {@link #getFromCache(Object)} if you want to avoid loading from the database.
     */
    @NotNull
    Optional<X> get(@Nullable String key);

    /**
     * Get a Sync object from this cache (will load from DB if necessary)
     * See {@link #getFromCache(Object)} if you want to avoid loading from the database.
     */
    @NotNull
    Optional<X> get(@Nullable String key, boolean saveToLocalCache);

    /**
     * Get a Sync object from this cache (loaded from DB if necessary) or create one with this key if not found.
     */
    @NotNull
    X getOrCreate(@NotNull String key);

    /**
     * Retrieves ALL Syncs, including cached values and additional values from database.
     * @param cacheSyncs If true, any additional Sync fetched from db will be cached.
     * @return An Iterable of all Syncs, for sequential processing.
     */
    @Blocking
    @NotNull
    Iterable<X> getAll(boolean cacheSyncs);

    /**
     * Loads all Syncs directly from db, bypassing the cache.
     * Unless you have a reason to use this, please use {@link #getAll(boolean)} instead.
     * @param cacheSyncs If true, syncs loaded from the database will be cached.
     * @return An Iterable of all Syncs, for sequential processing.
     */
    @Blocking
    @NotNull
    Iterable<X> getAllFromDatabase(boolean cacheSyncs);

    /**
     * Saves all Sync objects in this cache to the database.
     * Blocks until completion
     *
     * @return a {@link CacheSaveResult} with information about how many objects were saved.
     */
    @NotNull @Blocking
    CacheSaveResult saveAll();

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    /**
     * Retrieves an object by the provided index field and its value.
     */
    @NotNull
    <T> Optional<X> getByIndex(@NotNull IndexedField<X, T> field, @NotNull T value);
}
