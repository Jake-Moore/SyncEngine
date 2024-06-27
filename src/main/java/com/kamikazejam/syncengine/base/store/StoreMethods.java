package com.kamikazejam.syncengine.base.store;

import com.kamikazejam.syncengine.base.Sync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.Set;

@SuppressWarnings({"UnusedReturnValue", "rawtypes", "unused"})
public interface StoreMethods<K, X extends Sync> {

    /**
     * Retrieve a Sync from this store.
     */
    Optional<X> get(@NotNull K key);

    /**
     * Save a Sync to this store.
     */
    boolean save(@NotNull X sync);

    /**
     * Check if a Sync is stored in this store.
     */
    boolean has(@NotNull K key);

    /**
     * Check if a Sync is stored in this store.
     */
    boolean has(@NotNull X sync);

    /**
     * Remove a Sync from this store.
     *
     * @return If the Sync existed, and was removed.
     */
    boolean remove(@NotNull K key);

    /**
     * Remove a Sync from this store.
     *
     * @return If the Sync existed, and was removed.
     */
    boolean remove(@NotNull X sync);

    /**
     * Retrieve all Syncs from this store.
     */
    @NotNull
    Iterable<X> getAll();

    /**
     * Retrieve all Sync keys from this store.
     */
    @NotNull
    Set<String> getKeys();

    /**
     * Clear all Syncs from this store. No Syncs are deleted, just removed from memory.
     */
    long clear();

    /**
     * Gets the name of this storage layer.
     */
    @NotNull
    String getStoreLayer();

    /**
     * @return How many objects are in this Store
     */
    long size();

    /**
     * @return True IFF this Store is a database (stores data elsewhere)
     */
    @ApiStatus.Internal
    boolean isDatabase();

}
