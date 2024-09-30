package com.kamikazejam.syncengine.base.store;

import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface StoreMethods<K, X extends Sync<K>> {

    /**
     * Retrieve a Sync from this store.
     */
    Optional<X> get(@NotNull K key);

    /**
     * Save a Sync to this store.
     * @return if the Sync was saved successfully. (NOT_SET if we didn't have changes to save)
     */
    @NotNull
    TriState save(@NotNull X sync);

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
    Iterable<K> getKeys();

    /**
     * Retrieve all Sync keys (in string form) from this store.
     * Uses {@link com.kamikazejam.syncengine.base.Cache#keyToString(Object)} to convert keys to strings.
     */
    @NotNull
    Iterable<String> getKeyStrings(@NotNull Cache<K, X> cache);

    /**
     * Clear all Syncs from this store. No Syncs are deleted, just removed from memory.
     */
    long clear();

    /**
     * Gets the name of this storage layer.
     */
    @NotNull
    String getLayerName();

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
