package com.kamikazejam.syncengine.mode.profile.network.profile.store;

import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.loader.NetworkedProfileLoader;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.SyncServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public abstract class NetworkProfileStore extends LoggerService implements Service {

    private boolean running = false;
    public NetworkProfileStore() {}


    // ----------------------------------------------------- //
    //                      Abstraction                      //
    // ----------------------------------------------------- //
    /**
     * Pulses the other server to verify they do indeed have the player
     *   and our lastSeenServer is correct (prevents rare deadlocks)
     * This method is called in {@link NetworkedProfileLoader} when a handshake fails with a server
     * @param server The SyncServer that failed the handshake
     */
    public abstract void verifyPlayerOrigin(@NotNull UUID uuid, @NotNull String username, @NotNull SyncServer server);

    /**
     * Saves the NetworkProfile to this store
     * @return true if the save was successful
     */
    public abstract boolean save(@NotNull NetworkProfile profile);

    @ApiStatus.Internal
    protected abstract Optional<NetworkProfile> get(@NotNull UUID uuid);

    /**
     * Retrieves all NetworkProfiles in this store
     * @param onlyOnline if true, only return online profiles
     */
    @NotNull
    public abstract List<NetworkProfile> getAll(boolean onlyOnline);



    // ----------------------------------------------------- //
    //                         Service                       //
    // ----------------------------------------------------- //

    @Override
    public final boolean start() {
        running = true;
        return true;
    }

    @Override
    public final boolean shutdown() {
        running = false;

        // If there are any players online (won't be the case for server shutdown), then set them as offline
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Set their NetworkProfile to offline
            NetworkProfile np = this.getOrCreate(player);
            np.markUnloaded(false);
            this.save(np);
        }

        return true;
    }

    @Override
    public final boolean isRunning() {
        return running;
    }

    // ----------------------------------------------------- //
    //                    Internal Methods                   //
    // ----------------------------------------------------- //
    /**
     * Creates a new NetworkProfile with the given UUID and username
     * Does NOT save the new NetworkProfile
     */
    protected final @NotNull NetworkProfile create(@NotNull UUID uuid, @NotNull String username) {
        NetworkProfile np = new NetworkProfile(uuid, username);
        np.setThisServerName(getThisServerName());
        np.setUUID(uuid);
        np.setUsername(username);
        return np;
    }
    /**
     * Creates a new NetworkProfile with the given UUID
     * Does NOT save the new NetworkProfile
     */
    protected final @NotNull NetworkProfile create(@NotNull UUID uuid) {
        NetworkProfile np = new NetworkProfile(uuid);
        np.setThisServerName(getThisServerName());
        np.setUUID(uuid);
        return np;
    }

    /**
     * Gets a NetworkProfile by UUID
     */
    @NotNull
    public final NetworkProfile getOrCreate(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "uuid cannot be null");
        Optional<NetworkProfile> o = get(uuid);
        NetworkProfile np = o.orElseGet(() -> create(uuid));
        // Ensure the NetworkProfile is saved
        this.save(np);
        return np;
    }

    /**
     * Gets a NetworkProfile by UUID (and updates username if necessary)
     */
    @NotNull
    public final NetworkProfile getOrCreate(@NotNull UUID uuid, @NotNull String username) {
        Preconditions.checkNotNull(uuid, "uuid cannot be null");
        Optional<NetworkProfile> o = get(uuid);
        o.ifPresent(p -> p.setUsername(username));
        NetworkProfile np = o.orElseGet(() -> create(uuid, username));
        // Ensure the NetworkProfile is saved
        this.save(np);
        return np;
    }

    @NotNull
    public NetworkProfile getOrCreate(@NotNull Player player) {
        Preconditions.checkNotNull(player, "player cannot be null");
        return this.getOrCreate(player.getUniqueId(), player.getName());
    }

    @NotNull
    public <X extends SyncProfile> NetworkProfile getOrCreate(@NotNull X sync) {
        Preconditions.checkNotNull(sync, "Sync cannot be null");
        Optional<NetworkProfile> o = get(sync.getUniqueId());
        o.ifPresent(p -> sync.getUsername().ifPresent(p::setUsername));
        NetworkProfile np = o.orElseGet(() -> {
            // Creation Logic
            NetworkProfile newNp = new NetworkProfile();
            newNp.setThisServerName(getThisServerName());
            newNp.setUUID(sync.getUniqueId());
            sync.getUsername().ifPresent(newNp::setUsername);
            return newNp;
        });
        // Ensure the NetworkProfile is saved
        this.save(np);
        return np;
    }

    public final @NotNull String getHashKey() {
        return EngineSource.getSyncServerGroup();
    }
    public final @NotNull String getKeyString(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        return uuid.toString();
    }
    public final @NotNull String getThisServerName() {
        return EngineSource.getSyncServerId();
    }



    // ----------------------------------------------------- //
    //                      LoggerService                    //
    // ----------------------------------------------------- //
    @Override
    public final boolean isDebug() {
        return EngineSource.isDebug();
    }
    @Override
    public final Plugin getPlugin() {
        return EngineSource.get();
    }
}
