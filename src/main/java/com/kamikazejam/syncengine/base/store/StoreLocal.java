package com.kamikazejam.syncengine.base.store;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public abstract class StoreLocal<K, X extends Sync<K>> implements StoreMethods<K, X> {

    private final ConcurrentMap<K, X> localCache = new ConcurrentHashMap<>();

    // ----------------------------------------------------- //
    //                     Store Methods                     //
    // ----------------------------------------------------- //
    @Override
    public Optional<X> get(@NotNull K key) {
        Preconditions.checkNotNull(key);
        return Optional.ofNullable(this.localCache.get(key));
    }

    @Override
    public @NotNull TriState save(@NotNull X sync) {
        // If not called already, call initialized (since we're caching it)
        this.localCache.put(sync.getId(), sync);
        return TriState.TRUE;
    }

    @Override
    public boolean has(@NotNull K key) {
        return this.localCache.containsKey(key);
    }

    @Override
    public boolean has(@NotNull X sync) {
        return this.has(sync.getId());
    }

    @Override
    public boolean remove(@NotNull K key) {
        @Nullable X x = this.localCache.remove(key);
        if (x != null) {
            x.invalidate();
        }
        return x != null;
    }

    @Override
    public boolean remove(@NotNull X sync) {
        return this.remove(sync.getId());
    }

    @NotNull
    @Override
    public Iterable<X> getAll() {
        return this.localCache.values();
    }

    @NotNull
    @Override
    public Set<K> getKeys() {
        return this.localCache.keySet();
    }

    @Override
    public long clear() {
        final int size = this.localCache.size();
        this.localCache.clear();
        return size;
    }

    @Override
    public long size() {
        return this.localCache.size();
    }

    @Override
    public boolean isDatabase() {
        return false;
    }
}
