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
    protected transient @Nullable Long readOnlyTimeStamp = null;
    protected transient @Nullable Sync<String> cachedCopy;
    protected transient boolean validObject = true;


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
        // Has no meaning for SyncObject
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
        return Objects.hashCode(this.getId());
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

    @Override
    public boolean isValid() {
        return this.validObject;
    }

    @Override
    public void invalidate() {
        this.validObject = false;
    }
}
