package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.sync.SyncQueryModifier;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * Defines the minimum set of methods all Storage services must implement.
 */
@SuppressWarnings({"UnusedReturnValue", "rawtypes", "unused"})
public interface SyncStore<K, X extends Sync> extends Service {

    /**
     * Retrieve a Sync from this store.
     */
    Optional<X> get(@NotNull K key);

    /**
     * Save a Sync to this store. (depends on storage type)
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
     */
    @Nullable
    X remove(@NotNull K key);

    /**
     * Remove a Sync from this store.
     */
    @Nullable
    X remove(@NotNull X sync);

    /**
     * Retrieve all Syncs from this store.
     */
    @NotNull
    Collection<X> getAll();

    /**
     * Retrieve all Syncs from this store, that meet the query.
     * @param modifiers Additional query modifiers (filters, sorts, etc.)
     */
    @NotNull
    Collection<X> getAll(Collection<SyncQueryModifier<X>> modifiers);

    /**
     * Identifying name for this storage.
     */
    @NotNull
    String getName();

}
