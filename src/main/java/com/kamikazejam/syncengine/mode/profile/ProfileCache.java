package com.kamikazejam.syncengine.mode.profile;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.cache.CacheSaveResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Defines Profile-specific getters for SyncObjects. They return non-null Optionals.
 * All get options (when not handshaking) will create if necessary. This is because every
 * Player UUID is assumed to have a SyncProfile
 */
@SuppressWarnings("unused")
public interface ProfileCache<X extends SyncProfile> extends Cache<UUID, X> {

    /**
     * Get a Sync profile (by player) from this cache (will load from DB if necessary, will create if necessary)
     * See {@link #getFromCache(Object)} if you want to avoid loading from the database.
     * @return SyncProfile since we have a player object (assumed online)
     */
    @NotNull
    X get(@NotNull Player player);

    /**
     * Get a Sync profile (by uuid) from this cache (will load from DB if necessary, will create if necessary)
     * @return CompletableFuture in case we have to handshake for this profile.
     */
    @NotNull
    CompletableFuture<X> get(@NotNull UUID uuid);




    /**
     * Retrieve a Sync from this cache (by player).
     * This method does NOT query the database.
     * @return The Sync if it was cached.
     */
    @NonBlocking
    Optional<X> getFromCache(@NotNull Player player);

    /**
     * Retrieve a Sync from the database (by player).
     * This method force queries the database, and updates this cache.
     * @param cacheSync If we should cache the Sync upon retrieval. (if it was found)
     * @return The Sync if it was found in the database.
     */
    @Blocking
    Optional<X> getFromDatabase(@NotNull Player player, boolean cacheSync);

    /**
     * Gets all online players' Profile objects. These should all be in the cache.
     */
    @NotNull
    Collection<X> getOnline();




    /**
     * Saves all Sync Profiles for online players (ensuring no handshakes)
     * Blocks until completion
     *
     * @return a {@link CacheSaveResult} with information about how many objects were saved.
     */
    @NotNull @Blocking
    CacheSaveResult saveAllOnline();

    void removeLoader(@NotNull UUID uuid);

    void onProfileLeavingLocal(@NotNull Player player, @NotNull X profile);

    void onProfileLeavingGlobal(@NotNull Player player, @NotNull X profile);

}
