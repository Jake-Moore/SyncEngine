package com.kamikazejam.syncengine.mode.object;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter
@SuppressWarnings({"rawtypes", "unchecked", "unused"})
public abstract class SyncObject implements Sync<String> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (as a user-defined String)
    @JsonProperty("_id")
    private @NotNull String syncId = UUID.randomUUID().toString();

    @JsonProperty("version")
    protected long version = 0L;


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    protected transient SyncObjectCache cache;
    protected transient long handshakeStartTimestamp = 0;
    protected transient @Nullable Long readOnlyTimeStamp = null;
    protected transient @Nullable Sync<String> cachedCopy;


    // ----------------------------------------------------- //
    //                     Constructors                      //
    // ----------------------------------------------------- //
    // For Jackson
    public SyncObject() {}
    public SyncObject(@NotNull String syncId) {
        this.syncId = syncId;
    }
    public SyncObject(SyncObjectCache cache) {
        this.cache = cache;
    }
    public SyncObject(@NotNull String syncId, SyncObjectCache cache) {
        this.syncId = syncId;
        this.cache = cache;
    }


    // ----------------------------------------------------- //
    //                        Methods                        //
    // ----------------------------------------------------- //
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
    public CompletableFuture<Boolean> save() {
        return this.getCache().save(this);
    }

    @Override
    public boolean saveSynchronously() {
        return this.getCache().saveSynchronously(this);
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
        Object $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) { return true; }
        if (!(o instanceof SyncObject other)) { return false; }
        return Objects.equals(this.syncId, other.syncId);
    }

    @Override
    public String getId() {
        return this.syncId;
    }

    @Override
    public void setId(@NotNull String id) {
        this.syncId = id;
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

    @Override
    public void setCachedCopy(@Nullable Sync<String> cachedCopy) {
        this.cachedCopy = cachedCopy;
    }

    @Override
    public @NotNull Sync<String> getCachedCopy() {
        Preconditions.checkNotNull(cachedCopy, "Cached copy is null!");
        return cachedCopy;
    }

    @Override
    public void setVersion(long version) {
        this.version = version;
    }

    @Override
    public long getVersion() {
        return version;
    }

    @Override
    public void loadLocalDeepCopy(Sync<String> o) {
        SyncObject that = (SyncObject) o;
        that.version = this.version;
        that.syncId = this.syncId;
    }

    @Override
    public void cacheCopy() {
        // Create a new Sync to hold the deep copy
        Sync<String> deepCopySync = cache.getInstantiator().instantiate();
        deepCopySync.setCache(cache);

        // Load parent (SyncObject or SyncProfile) data into the deep copy
        this.loadLocalDeepCopy(deepCopySync);
        // Load User data
        this.copyInto(deepCopySync);
        // Cache this Copy for VersionMismatchException correction
        this.setCachedCopy(deepCopySync);
    }
}
