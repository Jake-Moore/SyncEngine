package com.kamikazejam.syncengine.network.profile.service;

import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.syncengine.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.SyncServer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * The Standalone NetworkProfileService implementation
 */
public class NetworkProfileServiceLocal extends NetworkProfileService {

    private final ConcurrentMap<UUID, NetworkProfile> localCache = new ConcurrentHashMap<>();

    // ----------------------------------------------------- //
    //                  NetworkProfileStore                  //
    // ----------------------------------------------------- //
    @Override
    public void verifyPlayerOrigin(@NotNull UUID uuid, @NotNull String username, @NotNull SyncServer server) {
        // In the local case, we have nothing to do, except update the NetworkProfile to reflect them being on this server
        //   We can ignore whatever the other server thinks it is, we are Standalone
        NetworkProfile profile = this.getOrCreate(uuid, username);

        // Update their online state and that the last seen server is this one, then save the profile
        profile.setOnline(PlayerUtil.isFullyValidPlayer(Bukkit.getPlayer(uuid)));
        profile.setLastSeenServer(profile.getThisServerName());
        this.saveSync(profile);
    }

    @Override
    public boolean saveSync(@NotNull NetworkProfile profile) {
        profile.markSaved();
        localCache.put(profile.getUUID(), profile);
        return true;
    }

    @Override
    public boolean saveAsync(@NotNull NetworkProfile profile) {
        // No difference between sync and async saving for this local service
        return saveSync(profile);
    }

    @Override
    public @NotNull List<NetworkProfile> getAll(boolean onlyOnline) {
        return localCache.values().stream()
                .filter(profile -> !onlyOnline || profile.isOnline())
                .toList();
    }

    @Override @ApiStatus.Internal
    protected Optional<NetworkProfile> get(@NotNull UUID uuid) {
        return Optional.ofNullable(localCache.get(uuid));
    }


    // ----------------------------------------------------- //
    //                      LoggerService                    //
    // ----------------------------------------------------- //
    @Override
    public String getLoggerName() {
        return "NetworkProfileLocal";
    }
}
