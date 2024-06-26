package com.kamikazejam.syncengine.base.store;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
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

    protected abstract boolean save(Cache<K, X> cache, @NotNull X sync);

    protected abstract boolean has(Cache<K, X> cache, @NotNull K key);

    protected abstract boolean remove(Cache<K, X> cache, @NotNull K key);

    @NotNull
    protected abstract Collection<X> getAll(Cache<K, X> cache);

    protected abstract long size(Cache<K, X> cache);


    // ---------------------------------------------------------------- //
    //                           StoreMethods                           //
    // ---------------------------------------------------------------- //
    @Override
    public Optional<X> get(@NotNull K key) {
        return this.get(this.cache, key);
    }

    @Override
    public boolean save(@NotNull X sync) {
        return this.save(this.cache, sync);
    }

    @Override
    public boolean has(@NotNull K key) {
        return this.has(this.cache, key);
    }

    @Override
    public boolean has(@NotNull X sync) {
        return this.has(this.cache, sync.getIdentifier());
    }

    @Override
    public boolean remove(@NotNull K key) {
        return this.remove(this.cache, key);
    }

    @Override
    public boolean remove(@NotNull X sync) {
        return this.remove(this.cache, sync.getIdentifier());
    }

    @Override
    public @NotNull Collection<X> getAll() {
        return this.getAll(this.cache);
    }

    @Override
    public long size() {
        return this.size(this.cache);
    }

}
