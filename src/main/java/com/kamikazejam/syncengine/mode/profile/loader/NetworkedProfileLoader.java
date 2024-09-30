package com.kamikazejam.syncengine.mode.profile.loader;

import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.handshake.struct.HandshakeData;
import com.kamikazejam.syncengine.networkprofile.NetworkProfile;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Class to help reduce the amount of code inside SyncProfileLoader
 * It just contains code to load a profile from a networked source
 */
public class NetworkedProfileLoader {
    protected static <X extends SyncProfile> void loadNetworked(SyncProfileLoader<X> loader) {
        ServerService serverService = EngineSource.getServerService();
        Preconditions.checkNotNull(serverService, "ServerService is null");

        @Nullable Player player = Bukkit.getServer().getPlayer(loader.uuid);
        if (!loader.login && player != null && player.isOnline()) {
            // If they are not logging in (no handshake needed), and they are online:
            //   we can just load from local

            loader.load(true);
            // Try to retrieve from the cache when they are online, skipping handshake logic.
            if (loader.sync != null) {
                return;
            }
        }

        // Otherwise we need to check for handshakes
        NetworkProfile networkProfile = EngineSource.getNetworkService().getOrCreate(loader.uuid, loader.username);
        loader.cache.getLoggerService().debug("NetworkLoad Sync " + loader.uuid + " (L: " + loader.login + ")");

        // If they are not on another server, load from local
        if (!networkProfile.isOnlineOtherServer()) {
            loader.load(networkProfile.isOnlineThisServer());
            return;
        }

        // Fetch the server they were last seen on
        SyncServer server = serverService.get(networkProfile.getLastSeenServer()).orElse(null);

        // Target server isn't online, or there is no recent server -> load from database
        if (server == null || !server.isOnline()) {
            loader.cache.getLoggerService().debug("Target server '" + (server != null ? server.getName() : "n/a") + "' not online for handshake for " + loader.uuid + ", loading from database");
            loader.load(false);
            return;
        }

        // We have a valid server -> handshake
        long msStart = System.currentTimeMillis();
        loader.cache.getLoggerService().debug("Starting handshake for " + loader.uuid + " (login: " + loader.login + ")");

        // Perform the handshake
        if (handshake(loader, server, msStart)) {
            // Log how long it took
            loader.cache.getLoggerService().debug("Handshake complete for " + loader.uuid + " (in " + (System.currentTimeMillis() - msStart) + "ms)");
        }
    }


    /**
     * @return True iff the handshake was successful, should log errors if returning false
     */
    private static <X extends SyncProfile> boolean handshake(SyncProfileLoader<X> L, SyncServer server, long msStart) {
        try {
            // CompletableFuture allows us to complete the future from various threads/locations
            //  And also have the 'slow' timeout loop run in the background, while the response can be
            //  completed more instantly if the handshake is successful
            // This also allows us to call this method multiple times asynchronously, each waiting for a result

            CompletableFuture<@Nullable HandshakeData> future = L.cache.getHandshakeService().requestHandshake(L, server, L.login, msStart);
            // Retrieve the handshake data (syncJson, version)
            @Nullable HandshakeData data = future.get(Settings.HANDSHAKE_TIMEOUT_SEC + 3L, TimeUnit.SECONDS);
            @Nullable String syncJson = (data == null) ? null : data.getJson();

            // Attempt to deserialize the Sync object received from the handshake
            @Nullable X temp = JacksonUtil.deserialize(L.cache.getSyncClass(), syncJson);
            // If we have a Sync from the handshake:
            if (temp != null) {
                if (L.sync == null) {
                    // If there is no sync, use the one we received from the handshake
                    L.sync = temp;
                    L.sync.setLoadingSource("Redis Handshake");
                    L.sync.setCache(L.cache);
                } else {
                    // Otherwise, Update our current object with the newer one
                    L.cache.updateSyncFromNewer(L.sync, temp);
                }
                L.cache.getLoggerService().debug("Updated Sync from handshake for " + L.uuid + " version: " + L.sync.getVersion() + " | HandShakeVer: " + temp.getVersion());
                return true;
            }

            // Otherwise, there was no Sync in handshake -> load from DB
            L.cache.getLoggerService().debug("Handshake failed to find sync for " + L.uuid + ", loading from DB");
            L.load(false);

            if (L.sync != null) {
                return true;
            }

            // if sync is STILL null, something broke
            L.cache.getLoggerService().info("Handshake failed to find sync for " + L.uuid + ", both handshake and loading from DB FAILED");
            L.denyJoin = true;
            L.joinDenyReason = ChatColor.RED + "A error occurred while loading your profile.  Please try again.";
            return false;

        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Timed out / failed handshake
            L.denyJoin = true;
            L.joinDenyReason = ChatColor.RED + "Timed out while loading your profile.  Please try again.";
            L.cache.getLoggerService().debug("Handshake timed out for " + L.uuid);
            EngineSource.getNetworkService().verifyPlayerOrigin(L.uuid, L.username, server);
            return false;
        }
    }
}
