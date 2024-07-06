package com.kamikazejam.syncengine.mode.profile.listener;

import com.kamikazejam.kamicommon.util.KUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.ProfileCache;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class QuitMethods {
    /**
     * Should only ever be called by {@link ProfileListener#quit(Player, ProfileCache, boolean)}
     */
    protected static <X extends SyncProfile> void standaloneQuit(@NotNull Player player, ProfileCache<X> cache, boolean saveAsync) {
        // save on quit in standalone mode
        Optional<X> o = cache.getFromCache(player.getUniqueId());
        if (o.isPresent()) {
            X profile = o.get();

            // ModificationRequest can be ignored since we are saving below
            cache.onProfileLeavingGlobal(player, profile);
            profile.uninitializePlayer();

            // Save the profile
            if (saveAsync && EngineSource.get().isEnabled()) {
                cache.save(profile);
            } else {
                cache.saveSynchronously(profile);
            }
            cache.removeLoader(profile.getUniqueId());
        }
    }

    /**
     * Should only ever be called by {@link ProfileListener#quit(Player, ProfileCache, boolean)}
     */
    protected static <X extends SyncProfile> void networkedQuit(@NotNull Player player, ProfileCache<X> cache, boolean saveAsync) {
        Optional<X> o = cache.getFromCache(player.getUniqueId());
        if (o.isEmpty()) {
            // This shouldn't happen
            cache.getLoggerService().debug("Profile null during logout for Sync '" + player.getName() + "': could not set online=false");
            KUtil.printStackTrace();
            return;
        }

        X profile = o.get();
        if (!profile.hasValidHandshake()) {
            // ----------------------------------------------------------------------- //
            //                  FULL QUIT (NO HANDSHAKE)                               //
            // ----------------------------------------------------------------------- //
            // !! only perform cache/profile operations here, any global logic may repeat for each cache processed !!

            // If we are NOT switching servers, tell this one to wrap up the profile object (player is full quitting)
            cache.onProfileLeavingLocal(player, profile);
            // Tell the cache that this profile is leaving the sync-group (full quit)
            cache.onProfileLeavingGlobal(player, profile);
            profile.uninitializePlayer();

            // Not switching servers (no incoming handshake) -- we can assume they are actually
            // Logging out, and not switching servers
            Runnable syncRunnable = () -> {
                cache.saveSynchronously(profile);
                cache.loader(player.getUniqueId()).uncache(player, profile, false);
                cache.removeLoader(player.getUniqueId());
                cache.getLoggerService().debug("Saving player " + player.getName() + " on logout (not switching servers)");
            };

            if (saveAsync && EngineSource.get().isEnabled()) {
                cache.runAsync(syncRunnable);
            } else {
                syncRunnable.run();
            }
        } else {
            // ----------------------------------------------------------------------- //
            //                  SWAPPING SERVERS (HANDSHAKE)                           //
            // ----------------------------------------------------------------------- //
            // Handshake means we are not saving to mongo
            // !! only perform cache/profile operations here, any global logic may repeat for each cache processed !!

            // onProfileLeavingLocal handled in ProfileHandshakeService
            profile.uninitializePlayer();

            cache.loader(player.getUniqueId()).uncache(player, profile, true);
            cache.getLoggerService().debug("Not saving player " + player.getName() + " on quit (is switching servers)");
        }
    }
}
