package com.kamikazejam.syncengine.base.store;

import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Represents the base class for both Local and Remote Object Stores.
 * - Local -> Memory Cache
 * - Remote -> Database (FILE, MONGODB)
 *
 * @param <X>
 */
@Getter
public abstract class SyncStore<K, X extends Sync<K>> implements StoreMethods<K, X> {

    protected final Cache<K, X> cache;

    public SyncStore(Cache<K, X> objectCache) {
        this.cache = objectCache;
    }

    // ---------------------------------------------------------------- //
    //                     Abstraction Conversion                       //
    // ---------------------------------------------------------------- //
    protected abstract Optional<X> get(Cache<K, X> cache, @NotNull K key);

    /**
     * @return if the Sync was saved successfully. (NOT_SET if we didn't have changes to save)
     */
    @NotNull
    protected abstract TriState save(Cache<K, X> cache, @NotNull X sync);

    protected abstract boolean has(Cache<K, X> cache, @NotNull K key);

    protected abstract boolean remove(Cache<K, X> cache, @NotNull K key);

    @NotNull
    protected abstract Iterable<X> getAll(Cache<K, X> cache);

    protected abstract long size(Cache<K, X> cache);


    // ---------------------------------------------------------------- //
    //                           StoreMethods                           //
    // ---------------------------------------------------------------- //
    @Override
    public Optional<X> get(@NotNull K key) {
        return this.get(this.cache, key);
    }

    @Override
    public @NotNull TriState save(@NotNull X sync) {
        return this.save(this.cache, sync);
    }

    @Override
    public boolean has(@NotNull K key) {
        return this.has(this.cache, key);
    }

    @Override
    public boolean has(@NotNull X sync) {
        return this.has(this.cache, sync.getId());
    }

    @Override
    public boolean remove(@NotNull K key) {
        return this.remove(this.cache, key);
    }

    @Override
    public boolean remove(@NotNull X sync) {
        return this.remove(this.cache, sync.getId());
    }

    @Override
    public @NotNull Iterable<X> getAll() {
        return this.getAll(this.cache);
    }

    @Override
    public long size() {
        return this.size(this.cache);
    }

}
