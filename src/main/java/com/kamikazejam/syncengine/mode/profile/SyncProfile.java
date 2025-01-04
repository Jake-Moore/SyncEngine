package com.kamikazejam.syncengine.mode.profile;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter @Setter
@SuppressWarnings({"unchecked", "rawtypes", "unused"})
public abstract class SyncProfile implements Sync<UUID> {
    // ----------------------------------------------------- //
    //                        Fields                         //
    // ----------------------------------------------------- //
    // The id of this object (a player uuid)
    @JsonProperty("_id")
    private @NotNull UUID syncId;

    @JsonProperty("version")
    private long version = 0L;

    @JsonProperty("username")
    private String username;


    // ----------------------------------------------------- //
    //                      Transients                       //
    // ----------------------------------------------------- //
    protected transient SyncProfileCache cache;
    protected transient long lastSaveTimestamp = 0;
    protected transient boolean saveFailed = false; // If the player's profile failed to auto-save/save on shutdown,
    // This will be set to true, and we will notify the player once their
    // Profile has been saved successfully
    protected transient String loadingSource = null;
    protected transient @Nullable Player player = null;
    protected transient long handshakeStartTimestamp = 0; // the time when a handshake starts (when another server requests that we save this profile)

    // The version of the object when it was loaded (FROM A HANDSHAKE), null otherwise (if not loaded from handshake)
    protected transient @Nullable Long handshakeVersion = null;
    protected transient @Nullable Long readOnlyTimeStamp = null; // when read-only was set (for a swap handshake)
    protected transient @Nullable Sync<UUID> cachedCopy;
    protected transient boolean validObject = true;


    // ----------------------------------------------------- //
    //                     Constructors                      //
    // ----------------------------------------------------- //
    // For Jackson
    public SyncProfile() {}
    public SyncProfile(@NotNull UUID syncId) {
        this.syncId = syncId;
    }
    public SyncProfile(SyncProfileCache cache) {
        this.cache = cache;
    }
    public SyncProfile(@NotNull UUID syncId, SyncProfileCache cache) {
        this.syncId = syncId;
        this.cache = cache;
    }
    public SyncProfile(@NotNull UUID syncId, @NotNull String username, SyncProfileCache cache) {
        this.syncId = syncId;
        this.username = username;
        this.cache = cache;
    }



    // ----------------------------------------------------- //
    //                      Sync Methods                     //
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
        return this.cache.save(this);
    }

    @Override
    public boolean saveSynchronously() {
        return this.cache.saveSynchronously(this);
    }

    @NotNull
    @Override
    public ProfileCache getCache() {
        return cache;
    }

    @Override
    public void setCache(Cache<UUID, ?> cache) {
        this.cache = (SyncProfileCache) cache;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getId(), this.getUniqueId());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) { return true; }
        if (!(o instanceof SyncProfile other)) { return false; }
        return Objects.equals(this.syncId, other.syncId);
    }

    @Override
    public UUID getId() {
        return this.syncId;
    }

    @Override
    public void setId(@NotNull UUID id) {
        this.syncId = id;
    }

    @Override
    public boolean isReadOnly() {
        return readOnlyTimeStamp != null && (System.currentTimeMillis() - readOnlyTimeStamp) <= 1000;
    }

    @Override
    public void setCachedCopy(@Nullable Sync<UUID> cachedCopy) {
        this.cachedCopy = cachedCopy;
    }

    @Override
    public @NotNull Sync<UUID> getCachedCopy() {
        com.google.common.base.Preconditions.checkNotNull(cachedCopy, "Cached copy is null!");
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
    public void loadLocalDeepCopy(Sync<UUID> o) {
        SyncProfile that = (SyncProfile) o;
        that.version = this.version;
        that.syncId = this.syncId;
        that.username = this.username;
    }

    @Override
    public void cacheCopy() {
        // Create a new Sync to hold the deep copy
        Sync<UUID> deepCopySync = cache.getInstantiator().instantiate();
        deepCopySync.setCache(cache);

        // Load parent (SyncObject or SyncProfile) data into the deep copy
        this.loadLocalDeepCopy(deepCopySync);
        // Load User data
        this.copyInto(deepCopySync);
        // Cache this Copy for VersionMismatchException correction
        this.setCachedCopy(deepCopySync);
    }



    // ----------------------------------------------------- //
    //                    Profile Methods                    //
    // ----------------------------------------------------- //
    /**
     * Get the Player represented by this player
     * @return The Optional - may not be online here
     */
    @NotNull
    public Optional<Player> getPlayer() {
        if (PlayerUtil.isFullyValidPlayer(this.player)) { return Optional.of(this.player); }

        this.player = Bukkit.getPlayer(this.getUniqueId());
        if (!PlayerUtil.isFullyValidPlayer(this.player)) { this.player = null; }
        return Optional.ofNullable(player);
    }

    /**
     * Get the UUID of the player
     */
    public UUID getUniqueId() {
        return this.syncId;
    }

    /**
     * Get the Name of the Player
     */
    public @NotNull Optional<String> getUsername() {
        if (this.username == null) {
            // Try to get the name from the cache
            this.username = IdUtilLocal.getName(this.getUniqueId()).orElse(null);
        }
        return Optional.ofNullable(this.username);
    }

    /**
     * Stores the Player object inside this Profile
     */
    @ApiStatus.Internal
    public final void initializePlayer(@NotNull Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null for initializePlayer");
        this.player = player;
    }

    /**
     * nullifies the Player object from this Profile
     */
    @ApiStatus.Internal
    public final void uninitializePlayer() {
        this.player = null;
    }

    /**
     * Check if the player behind this Profile is online (and valid)
     * @return Iff the Player is online (LOCALLY) - does not check sync-group
     */
    public boolean isOnlineAndValid() {
        // Fetch the player and check if they're online
        this.player = Bukkit.getPlayer(this.getUniqueId());
        return PlayerUtil.isFullyValidPlayer(this.player);
    }

    /**
     * Check if the player behind this Profile is online (and valid)
     * @return Iff the Player is online (LOCALLY) - does not check sync-group
     */
    public boolean isOnline() {
        // Fetch the player and check if they're online
        this.player = Bukkit.getPlayer(this.getUniqueId());
        return this.player != null && this.player.isOnline();
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
