package com.kamikazejam.syncengine.mode.profile.loader;

import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.kamicommon.util.data.Pair;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.Settings;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Class to help reduce the amount of code inside SyncProfileLoader
 * It just contains the cacheNetworkNode method in a dedicated class
 */
public class NetworkedProfileLoader {
    protected static <X extends SyncProfile> Optional<X> cacheNetworkNode(SyncProfileLoader<X> L) {
        ServerService serverService = EngineSource.getServerService();
        Preconditions.checkNotNull(serverService, "ServerService is null");

        if (!L.login) {
            @Nullable Player player = L.cache.getPlugin().getServer().getPlayer(L.uuid);
            // For PlayerJoinEvent calls, isValid may be false, but isOnline is true
            // We just care about them being online (indicator we can load this Profile from local)
            if (player != null && player.isOnline()) {
                L.load(true);
                // Just getting them from the cache when they are online, skip all the other shit and just return this.
                // For performance :)
                if (L.sync != null) {
                    if (!L.cache.isCached(L.uuid)) {
                        L.cache.cache(L.sync);
                    }
                    L.sync = L.cache.getLocalStore().get(L.uuid).orElseThrow();
                    L.sync.setCache(L.cache);
                    return Optional.of(L.sync);
                }
            }
        }

        NetworkProfile networkProfile = EngineSource.getNetworkStore().getOrCreate(L.uuid, L.username);
        L.cache.getLoggerService().debug("Caching Sync " + L.uuid + " (L: " + L.login + ")");
        if (networkProfile.isOnlineOtherServer()) {
            SyncServer server = serverService.get(networkProfile.getLastSeenServer()).orElse(null);
            if (server != null && server.isOnline()) {

                // Handshake
                final String name = server.getName();
                try {
                    // CompletableFuture allows us to complete the future from various threads/locations
                    //  And also have the 'slow' timeout loop run in the background, while the response can be
                    //  completed more instantly if the handshake is successful
                    // This also allows us to call this method multiple times asynchronously, each waiting for a result
                    L.cache.getLoggerService().debug("Starting handshake for " + L.uuid + " (login: " + L.login + ")");
                    long msStart = System.currentTimeMillis();
                    CompletableFuture<@Nullable Pair<String, Long>> future = L.cache.getHandshakeService().requestHandshake(L, server, L.login, msStart);
                    @Nullable Pair<String, Long> data = future.get(Settings.HANDSHAKE_TIMEOUT_SEC + 3L, TimeUnit.SECONDS);
                    @Nullable String syncJson = (data == null) ? null : data.getA();

                    @Nullable X temp = JacksonUtil.fromJson(L.cache.getSyncClass(), syncJson);
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
                    }

                    // if sync is still null (other server couldn't find it), load from DB
                    if (L.sync == null) {
                        L.cache.getLoggerService().debug("Handshake failed to find sync for " + L.uuid + ", loading from DB");
                        L.load(false);
                    }
                    // if sync is STILL null, something broke
                    if (L.sync == null) {
                        L.cache.getLoggerService().info("Handshake failed to find sync for " + L.uuid + ", both handshake and loading from DB FAILED");
                        L.denyJoin = true;
                        L.joinDenyReason = ChatColor.RED + "A error occurred while loading your profile.  Please try again.";
                        return Optional.empty();
                    }

                    // Update sync with data from this handshake
                    L.sync.setHandshakeJson(syncJson);
                    L.sync.setSyncServerId(name);
                    if (temp != null) {
                        L.sync.setHandshakeVersion(temp.getVersion());
                    }

                    L.cache.getLoggerService().debug("Handshake complete for " + L.uuid + " (in " + (System.currentTimeMillis() - msStart) + "ms)");

                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    // Timed out / failed handshake
                    L.denyJoin = true;
                    L.joinDenyReason = ChatColor.RED + "Timed out while loading your profile.  Please try again.";
                    L.cache.getLoggerService().debug("Handshake timed out for " + L.uuid);
                    EngineSource.getNetworkStore().verifyPlayerOrigin(L.uuid, L.username, server);
                }
            } else {
                // Target server isn't online, or there is no recent server
                L.cache.getLoggerService().debug("Target server '" + (server != null ? server.getName() : "n/a") + "' not online for handshake for " + L.uuid + ", loading from database");
                L.load(false);
            }
        } else {
            L.load(networkProfile.isOnlineThisServer()); // only load from local if they're online this server
        }

        if (L.sync != null && L.login) {
            L.cache.cache(L.sync);
        }
        return Optional.ofNullable(L.sync);
    }
}
