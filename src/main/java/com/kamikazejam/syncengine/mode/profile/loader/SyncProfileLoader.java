package com.kamikazejam.syncengine.mode.profile.loader;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.cache.SyncLoader;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.SyncProfileCache;
import com.kamikazejam.syncengine.mode.profile.listener.ProfileListener;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
@Setter @SuppressWarnings("unused")
public class SyncProfileLoader<X extends SyncProfile> implements SyncLoader<X> {

    protected final @NotNull SyncProfileCache<X> cache;
    protected final @NotNull UUID uuid;
    protected String username = null;
    /**
     * Whether this loader is being used during a login operation
     */
    protected boolean login = false;
    protected boolean denyJoin = false;
    protected String joinDenyReason = ChatColor.RED + "A caching error occurred. Please try again.";
    protected X sync = null;
    protected Player player = null;

    public SyncProfileLoader(@NotNull SyncProfileCache<X> cache, @NotNull UUID uuid) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(uuid);
        this.cache = cache;
        this.uuid = uuid;
    }

    protected void load(boolean fromLocal) {
        if (fromLocal) {
            Optional<X> o = cache.getLocalStore().get(uuid);
            if (o.isPresent()) {
                sync = o.get();
                return;
            }
        }
        Optional<X> o = cache.getDatabaseStore().get(uuid);
        if (o.isPresent()) {
            sync = o.get();
        } else {
            if (login) {
                // Create
                sync = cache.getInstantiator().instantiate();
                sync.setId(uuid);
                sync.setLoadingSource("New Profile");
                sync.setCache(cache);
            } else {
                // Doesn't exist
                sync = null;
            }
        }
    }

    @Override
    public Optional<X> fetch(boolean saveToLocalCache) { // boolean saveToLocalCache doesn't matter on SyncProfiles
        reset();

        if (login) {
            StorageService storageService = EngineSource.getStorageService();
            if (!storageService.canCache()) {
                denyJoin = true;
                joinDenyReason = StringUtil.t(EngineSource.getConfig().getString("profiles.messages.beforeDbConnection")
                        .replace("{cacheName}", cache.getName()));
                return Optional.empty();
            }
        }

        if (EngineSource.getSyncMode() == SyncMode.STANDALONE) {
            StandaloneProfileLoader.loadStandalone(this);
        }else {
            NetworkedProfileLoader.loadNetworkNode(this);
        }

        // The above two methods will load the sync into this variable if it exists
        Optional<X> o = Optional.ofNullable(this.sync);

        // Ensure the sync is cached and has a valid cache reference
        o.ifPresent(sync -> {
            @Nullable Player p = Bukkit.getPlayer(sync.getUniqueId());
            if (saveToLocalCache) {
                cache.cache(sync);
            }else {
                sync.setCache(cache);
            }
        });
        return o;
    }

    @Override
    public void uncache(@NotNull Player player, @NotNull X sync, boolean switchingServers) {
        // If they ARE switching servers, the handshake process will take care of calling this prior to
        //   handshaking the profile json
        if (cache.isCached(sync.getUniqueId())) {
            cache.uncache(sync.getUniqueId());
        }
    }

    public void reset() {
        denyJoin = false;
        sync = null;
    }

    public void login(@NotNull String username) {
        this.login = true;
        this.username = username;
    }

    @NotNull
    public CompletableFuture<X> cacheOrCreate() {
        return CompletableFuture.supplyAsync(() -> {
            Optional<X> o = fetch(true);
            if (o.isPresent()) {
                return o.get();
            }

            // Fake the login in order to trigger the logic needed to create a new profile
            login = true;
            @Nullable X sync = fetch(true).orElse(null);
            login = false;

            // Throw error if still null (can't return null, so throw exception)
            if (sync == null) {
                String msg = "Failed to create new profile for username: " + username + " (UUID: " + uuid + ")";
                cache.getLoggerService().severe(msg);
                throw new RuntimeException(msg);
            }

            // Save the new object, and return it
            sync.save();
            return sync;
        });
    }

    /**
     * Called in {@link ProfileListener#onProfileCachingInit(PlayerJoinEvent)}
     */
    public void initializeOnJoin(Player player) {
        this.player = player;
        if (sync == null) {
            sync = cache.getFromCache(player).orElse(null);
        }
        if (sync != null) {
            sync.initializePlayer(player);
        }
    }

}
