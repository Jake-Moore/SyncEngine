package com.kamikazejam.syncengine.mode.profile.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.base.exception.CachingError;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.event.profile.NetworkProfileLoginEvent;
import com.kamikazejam.syncengine.event.profile.NetworkProfileLogoutEvent;
import com.kamikazejam.syncengine.event.profile.NetworkProfileSwitchServersEvent;
import com.kamikazejam.syncengine.mode.profile.ProfileCache;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.SyncProfileCache;
import com.kamikazejam.syncengine.mode.profile.loader.SyncProfileLoader;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import com.kamikazejam.syncengine.util.AsyncCachesExecutor;
import com.kamikazejam.syncengine.util.Settings;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"unused", "UnstableApiUsage"})
public class ProfileListener implements Listener {

    public ProfileListener() {}

    public static <X extends SyncProfile> void quit(@NotNull Player player, ProfileCache<X> cache, boolean isEnabled) {
        if (EngineSource.getSyncMode() == SyncMode.STANDALONE) {
            QuitMethods.standaloneQuit(player, cache, isEnabled);
        } else {
            QuitMethods.networkedQuit(player, cache, isEnabled);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingStart(AsyncPlayerPreLoginEvent event) {
        final long ms = System.currentTimeMillis();
        final String username = event.getName();
        final UUID uniqueId = event.getUniqueId();
        final String ip = event.getAddress().getHostAddress();

        StorageService storageService = EngineSource.getStorageService();
        if (!storageService.canCache()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "Server is starting, please wait.");
            return;
        }

        // Trigger a NetworkSwapHandshake in order to let the other server know about the swap
        //  and to validate that the player is on the lastSeenServer (if set in NetworkProfile)
        NetworkProfile networkProfile = EngineSource.getNetworkStore().getOrCreate(uniqueId, username);
        try {
            validateSwap(networkProfile);
        }catch (Throwable t) {
            t.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "A caching error occurred. Please try again.");
            return;
        }
        // Keep track of when a player FIRST joins a sync group
        networkProfile.setFirstJoinToSyncGroup(!networkProfile.isOnline());

        // Run a special fully parallelized execution for caches based on their depends
        try {
            long timeout = Settings.HANDSHAKE_TIMEOUT_SEC;
            cachePlayerProfiles(username, uniqueId, ip, timeout).get(timeout + 3, TimeUnit.SECONDS);
        }catch (Throwable t) {
            if (t instanceof ExecutionException e) {
                if (e.getCause() instanceof CachingError error) {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, error.getMessage());
                    return;
                }
            }

            t.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ChatColor.RED + "A caching error occurred.  Please try again.");
            return;
        }

        // Mark loaded (only after all caches have been initialized and all handshakes have been finished)
        networkProfile.markLoaded(true);
        networkProfile.setUUID(uniqueId);       // ensure valid and up-to-date data
        networkProfile.setUsername(username);   // ensure valid and up-to-date data
        networkProfile.markSaved();

        // Save async to prevent unnecessary blocking on join
        Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () ->
                EngineSource.getNetworkStore().save(networkProfile)
        );
        EngineSource.getNetworkStore().debug("Player " + username + " (" + uniqueId + ") marked as loaded in " + (System.currentTimeMillis() - ms) + "ms");
    }

    private void validateSwap(NetworkProfile networkProfile) throws Exception {
        if (!networkProfile.isOnlineOtherServer()) { return; }

        ServerService serverService = EngineSource.getServerService();
        RedisService redisService = EngineSource.getRedisService();
        if (serverService == null || redisService == null) {
            // Nothing to do in the Standalone case
            return;
        }

        // Check that the server is valid
        SyncServer server = serverService.get(networkProfile.getLastSeenServer()).orElse(null);
        if (server == null || !server.isOnline()) { return; }

        // Let the other server know they're swapping
        CompletableFuture<Boolean> future = EngineSource.getSwapService().requestHandshake(
                networkProfile, server
        );
        // If we got a reply and it said the player was not on that server, update NetworkProfile
        if (!future.get()) {
            // This will hopefully prevent the upcoming caches from trying to handshake
            //  since we just found the player is NOT on the lastSeenServer we thought they were
            networkProfile.setOnline(false);
        }
    }


    @SuppressWarnings("unchecked")
    private <X extends SyncProfile> CompletableFuture<Void> cachePlayerProfiles(String username, UUID uniqueId, String ip, long timeoutSec) {
        // Compile all the ProfileCaches
        List<SyncProfileCache<X>> caches = new ArrayList<>();
        SyncEngineAPI.getCaches().values().forEach(c -> {
            if (c instanceof SyncProfileCache<?>) { caches.add((SyncProfileCache<X>) c); }
        });
        AsyncCachesExecutor<SyncProfileCache<X>> executor = new AsyncCachesExecutor<>(caches, (cache) -> {
            long ms2 = System.currentTimeMillis();
            SyncProfileLoader<X> loader = cache.loader(uniqueId);
            loader.login(username);
            loader.fetch();

            if (loader.isDenyJoin()) {
                // For the first 100 seconds, don't give the nasty loader reason, but a pretty server start error
                String message = (System.currentTimeMillis() - EngineSource.getOnEnableTime() < 100_000L)
                        ? StringUtil.t("&cServer is starting, please wait.")
                        : loader.getJoinDenyReason();

                // If denied, throw an exception (will be caught by original join event)
                throw new CachingError(message);
            }
        }, timeoutSec);

        // Execute the cache list in order
        return executor.executeInOrder();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCachingInit(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SyncEngineAPI.getCaches().values().forEach(c -> {
            if (c instanceof SyncProfileCache<?> cache) {
                SyncProfileLoader<?> loader = cache.loader(player.getUniqueId());
                loader.initializeOnJoin(player);
            }
        });

        // Call the NetworkProfileLoginEvent if this is the first join for this Sync-Group
        NetworkProfile profile = EngineSource.getNetworkStore().getOrCreate(player);
        if (profile.isFirstJoinToSyncGroup()) {
            NetworkProfileLoginEvent e = new NetworkProfileLoginEvent(player, profile);
            Bukkit.getServer().getPluginManager().callEvent(e);
        }
    }

    public static final Cache<UUID, Long> swapMap = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();
    @EventHandler(priority = EventPriority.HIGH)
    public void onProfileQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        // Best way to do this for now, is to do this sync and in order of reverse depends
        SyncEngineAPI.getSortedCachesByDependsReversed().forEach(c -> {
            if (c instanceof SyncProfileCache<?> cache) {
                quit(player, cache, true);
            }
        });

        // Mark the NetworkProfile as offline IFF the player is not swapping servers
        boolean swappingServers = swapMap.asMap().containsKey(player.getUniqueId());
        NetworkProfile profile = EngineSource.getNetworkStore().getOrCreate(player);
        profile.markUnloaded(swappingServers);
        profile.markSaved();
        Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () ->
                EngineSource.getNetworkStore().save(profile)
        );
        swapMap.invalidate(player.getUniqueId());

        if (swappingServers) {
            NetworkProfileSwitchServersEvent switchEvent = new NetworkProfileSwitchServersEvent(player, profile);
            Bukkit.getServer().getPluginManager().callEvent(switchEvent);
        }else {
            NetworkProfileLogoutEvent logoutEvent = new NetworkProfileLogoutEvent(player, profile);
            Bukkit.getServer().getPluginManager().callEvent(logoutEvent);
        }
    }
}
