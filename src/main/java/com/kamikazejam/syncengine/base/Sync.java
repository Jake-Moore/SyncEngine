package com.kamikazejam.syncengine.base;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * A Sync is an object that can be cached, saved, or loaded within SyncEngine.
 * Generics: K = Identifier Object Type (i.e. String, UUID)
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public interface Sync<K> {

    /**
     * Used to call afterInitialized (only once)
     */
    @ApiStatus.Internal
    void initialized();

    /**
     * Called after this object is created and initialized <p>
     * Called: On Instantiator, On Load from MongoDb
     */
    @ApiStatus.Experimental
    void afterInitialized();

    /**
     * Used to call beforeUninitialized (only once)
     */
    @ApiStatus.Internal
    void uninitialized();

    /**
     * Called before this object is deleted / uninitialized <p>
     * Called: On Remove from Local Cache (which includes deletes)
     */
    @ApiStatus.Experimental
    void beforeUninitialized();

    /**
     * Gets the unique identifier of our Sync. This can be a String, UUID, or any another type.
     * It just needs to be unique and able to be used as a key in a HashMap.
     *
     * @return K Identifier
     */
    K getIdentifier();

    /**
     * Gets the unique identifier of our Sync. This can be a String, UUID, or any another type.
     * It just needs to be unique and able to be used as a key in a HashMap.
     *
     * @return K Identifier
     */
    K getId();

    /**
     * Gets the field name that {@link #getIdentifier()} uses (the field in the superclass)
     * This is used for MongoDB lookups, it must be correct for proper functionality.
     *
     * @return field name
     */
    @NotNull
    String identifierFieldName();

    /**
     * Gets the cache associated with this Sync object.
     * Every Sync has its cache stored (non-persistent / transient) for easy access.
     *
     * @return Cache
     */
    @NotNull
    Cache<K, ?> getCache();

    /**
     * Save this Sync object (its json) to the storage.
     * @return if the save was successful
     */
    boolean save();

    /**
     * Save this Sync object (its json) to the storage asynchronously.
     * @return CompletableFuture<Boolean> if the save was successful
     */
    CompletableFuture<Boolean> saveAsync();

    /**
     * @return A hash code based on any identifying fields for this Sync.
     */
    int hashCode();

    /**
     * Use identifying fields to determine if two Sync objects are equal.
     * @return if the two Sync objects have matching identification
     */
    boolean equals(Object object);

    /**
     * Load another Sync object's data into this Sync object. (Overwrite/Update)
     */
    void load(Sync<K> other);

    /**
     * Sets the optimistic versioning field, used during the update process
     */
    @ApiStatus.Internal
    void setVersion(long version);

    /**
     * Gets the optimistic versioning field
     */
    long getVersion();

}
