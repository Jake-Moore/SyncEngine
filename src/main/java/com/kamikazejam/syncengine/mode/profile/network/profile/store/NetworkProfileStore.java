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

    /**
     * Retrieves a NetworkProfile by UUID
     */
    @NotNull
    public abstract Optional<NetworkProfile> get(@NotNull UUID uuid);

    /**
     * Retrieves all NetworkProfiles in this store
     * @param onlyOnline if true, only return online profiles
     */
    @NotNull
    public abstract List<NetworkProfile> getAll(boolean onlyOnline);

    /**
     * Checks if the store has a NetworkProfile with the given UUID
     */
    public abstract boolean has(@NotNull UUID uuid);


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
            this.get(player.getUniqueId()).ifPresent(networkProfile -> {
                networkProfile.markUnloaded(false);
                this.save(networkProfile);
            });
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
    public final @NotNull NetworkProfile create(@NotNull UUID uuid, @NotNull String username) {
        NetworkProfile np = new NetworkProfile(uuid, username);
        np.setThisServerName(getThisServerName());
        np.setUUID(uuid);
        np.setUsername(username);
        return np;
    }

    /**
     * Creates a new NetworkProfile from a Sync
     * Does NOT save the new NetworkProfile
     */
    public final <X extends SyncProfile> @NotNull NetworkProfile create(@NotNull X sync) {
        NetworkProfile np = new NetworkProfile();
        np.setThisServerName(getThisServerName());
        np.setUUID(sync.getUniqueId());
        sync.getUsername().ifPresent(np::setUsername);
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
        return o.orElseGet(() -> create(uuid, username));
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
        return o.orElseGet(() -> create(sync));
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
