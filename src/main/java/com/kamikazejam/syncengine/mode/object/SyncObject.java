package com.kamikazejam.syncengine.mode.object;

import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Property;
import dev.morphia.annotations.Version;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public abstract class SyncObject implements Sync<String> {
    protected transient SyncObjectCache cache;

    @Property("syncId")
    protected String syncId;

    @Id
    protected String identifier;

    // Implement Optimistic Locking
    @Version
    protected long version = 0;

    protected transient long handshakeStartTimestamp = 0;

    protected transient @Nullable Long readOnlyTimeStamp = null;

    // For Morphia
    public SyncObject() {}

    public SyncObject(SyncObjectCache cache) {
        this.cache = cache;
        this.syncId = SyncEngineAPI.getSyncID();
    }

    @Override
    public boolean hasValidHandshake() {
        if (handshakeStartTimestamp > 0) {
            long ago = System.currentTimeMillis() - handshakeStartTimestamp;
            long seconds = ago / 1000;
            return seconds < 10;
        }
        return false;
    }

    @Override
    public boolean save() {
        return this.getCache().save(this);
    }

    @Override
    public CompletableFuture<Boolean> saveAsync() {
        return this.getCache().saveAsync(this);
    }

    @NotNull
    @Override
    public SyncObjectCache getCache() {
        return cache;
    }

    @Override
    public void setCache(Cache<String, ?> cache) {
        this.cache = (SyncObjectCache) cache;
    }

    @Override
    public int hashCode() {
        int result = 1;
        Object $id = this.getIdentifier();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof SyncObject other)) {
            return false;
        }
        if (this.identifier == null || other.identifier == null) {
            return false;
        }
        return Objects.equals(this.identifier, other.identifier);
    }

    @NotNull
    @Override
    public String identifierFieldName() {
        return "identifier";
    }

    @Override
    public String getId() {
        return getIdentifier();
    }


    private transient boolean initialized = false;

    @Override
    public final void initialized() {
        if (!initialized) {
            initialized = true;
            afterInitialized();
        }
    }

    @Override
    public void afterInitialized() {
    }


    private transient boolean uninitialized = false;

    @Override
    public final void uninitialized() {
        if (!uninitialized) {
            uninitialized = true;
            beforeUninitialized();
        }
    }

    @Override
    public void beforeUninitialized() {
    }

    @Override
    public boolean isReadOnly() {
        return readOnlyTimeStamp != null && (System.currentTimeMillis() - readOnlyTimeStamp) <= 1000;
    }
}
