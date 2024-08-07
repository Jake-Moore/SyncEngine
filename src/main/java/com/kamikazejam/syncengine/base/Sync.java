package com.kamikazejam.syncengine.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A Sync is an object that can be cached, saved, or loaded within SyncEngine.
 * Generics: K = Identifier Object Type (i.e. String, UUID)
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Sync<K> {

    // ----------------------------------------------------- //
    //                  User Defined Methods                 //
    // ----------------------------------------------------- //

    /**
     * Perform a deep copy of this Sync object into the provided Sync object.
     * All fields should be copied over (deep copy).
     */
    void copyInto(@NotNull Sync<K> other);


    // ----------------------------------------------------- //
    //                Api / Internal Methods                 //
    // ----------------------------------------------------- //

    /**
     * Gets the unique identifier of our Sync. This can be a String representation of anything (like a UUID).
     * It just needs to be unique and able to be used as a key in a HashMap.
     *
     * @return K Identifier
     */
    K getId();

    /**
     * Sets the unique identifier of our Sync. This can be a String representation of anything (like a UUID).
     * It just needs to be unique and able to be used as a key in a HashMap.
     */
    void setId(@NotNull K id);

    /**
     * Gets the cache associated with this Sync object.
     * Every Sync has its cache stored (non-persistent / transient) for easy access.
     *
     * @return Cache
     */
    @NotNull
    Cache<K, ?> getCache();

    /**
     * Sets the cache associated with this Sync object.
     */
    void setCache(Cache<K, ?> cache);

    /**
     * Save this Sync object (its json) to the storage.
     *
     * @return if the save was successful
     */
    @Blocking
    boolean saveSynchronously();

    /**
     * Save this Sync object (its json) to the storage asynchronously.
     *
     * @return CompletableFuture<Boolean> if the save was successful
     */
    @NonBlocking
    CompletableFuture<Boolean> save();

    /**
     * @return A hash code based on any identifying fields for this Sync.
     */
    int hashCode();

    /**
     * Use identifying fields to determine if two Sync objects are equal.
     *
     * @return if the two Sync objects have matching identification
     */
    boolean equals(Object object);

    /**
     * @return True if there is an active handshake for this Sync object.
     */
    boolean hasValidHandshake();

    /**
     * Sets the optimistic versioning field, used during the update process
     */
    @ApiStatus.Internal
    void setVersion(long version);

    /**
     * Gets the optimistic versioning field
     */
    long getVersion();

    /**
     * @return If this Sync is read-only right now
     */
    boolean isReadOnly();

    @ApiStatus.Internal
    void setCachedCopy(Sync<K> copy);

    @ApiStatus.Internal
    @NotNull Sync<K> getCachedCopy();

    @ApiStatus.Internal
    void loadLocalDeepCopy(Sync<K> other);

    /**
     * Creates a new Sync (a copy) and saves it as this Sync's cached copy.
     */
    void cacheCopy();

    /**
     * @return If this Sync is valid and can be saved / updated. (i.e. not deleted)
     */
    boolean isValid();

    /**
     * Makes this Sync object invalid. It can no longer receive updates or be saved. (i.e. it was deleted)
     */
    void invalidate();
}
