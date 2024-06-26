package com.kamikazejam.syncengine.mode.object.store;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.store.StoreMethods;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ObjectStoreLocal<X extends SyncObject> implements StoreMethods<String, X> {

    private final ConcurrentMap<String, X> localCache = new ConcurrentHashMap<>();

    public ObjectStoreLocal() {
    }

    @Override
    public Optional<X> get(@NotNull String key) {
        Preconditions.checkNotNull(key);
        return Optional.ofNullable(this.localCache.get(key.toLowerCase()));
    }

    @Override
    public boolean save(@NotNull X sync) {
        // If not called already, call initialized (since we're caching it)
        sync.initialized();
        this.localCache.put(sync.getIdentifier().toLowerCase(), sync);
        return true;
    }

    @Override
    public boolean has(@NotNull String key) {
        return this.localCache.containsKey(key.toLowerCase());
    }

    @Override
    public boolean has(@NotNull X sync) {
        return this.has(sync.getIdentifier().toLowerCase());
    }

    @Override
    public boolean remove(@NotNull String key) {
        @Nullable X x = this.localCache.remove(key.toLowerCase());
        if (x != null) {
            x.uninitialized();
        }
        return x != null;
    }

    @Override
    public boolean remove(@NotNull X sync) {
        return this.remove(sync.getIdentifier());
    }

    @NotNull
    @Override
    public Collection<X> getAll() {
        return this.localCache.values();
    }

    @Override
    public long clear() {
        final int size = this.localCache.size();
        this.localCache.values().forEach(SyncObject::uninitialized);
        this.localCache.clear();
        return size;
    }

    @NotNull
    @Override
    public String getStoreLayer() {
        return "Object Local";
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
