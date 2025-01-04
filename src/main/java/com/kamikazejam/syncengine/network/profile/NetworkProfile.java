package com.kamikazejam.syncengine.network.profile;

import com.kamikazejam.kamicommon.json.JSONObject;
import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.network.player.NetworkPlayer;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * The profile for a given {@link Player} on a network (SyncEngine servers on the same SyncGroup & Redis).
 */
@Data
@Accessors(chain = true)
@SuppressWarnings({"unused", "BooleanMethodIsAlwaysInverted"})
public class NetworkProfile {

    @Setter
    protected transient String thisServerName;

    private long lastCached = System.currentTimeMillis();
    private long lastSaved = System.currentTimeMillis();
    @Getter(AccessLevel.NONE)
    private @NotNull UUID uuid;
    private @Nullable String username;
    private @Nullable String lastSeenIP;

    private String lastSeenServer;
    private long lastSeen = 0L;
    private boolean online = false;
    @Setter
    private boolean firstJoinToSyncGroup = false;

    // For Jackson, must have a no-arg constructor
    public NetworkProfile() {}

    public NetworkProfile(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        this.uuid = uuid;
        this.username = null;
    }

    public NetworkProfile(@NotNull UUID uuid, @NotNull String username) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        Preconditions.checkNotNull(username, "Username cannot be null");
        this.uuid = uuid;
        this.username = username;
    }

    // Maintain a lazy link to the NetworkPlayer
    private transient @Nullable NetworkPlayer player;
    public @NotNull NetworkPlayer getPlayer() {
        // We can cache NetworkPlayer since it's nothing more than a utility wrapper around the player UUID
        return (player == null) ? (player = new NetworkPlayer(uuid)) : player;
    }

    public boolean isOnlineOtherServer() {
        return isOnline() && (lastSeenServer == null || !lastSeenServer.equalsIgnoreCase(thisServerName));
    }

    public boolean isOnlineThisServer() {
        return isOnline() && lastSeenServer != null && lastSeenServer.equalsIgnoreCase(thisServerName);
    }

    public void markLoaded(boolean online) {
        this.online = online;
        lastCached = System.currentTimeMillis();
        if (online) {
            lastSeenServer = thisServerName;
            lastSeen = System.currentTimeMillis();
        }
    }

    public void markUnloaded(boolean switchingServers) {
        online = switchingServers;
        lastSeen = System.currentTimeMillis();
    }

    public void markSaved() {
        lastSaved = System.currentTimeMillis();
        if (isOnlineThisServer()) {
            lastSeen = System.currentTimeMillis();
        }
    }

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid);
        this.uuid = uuid;
    }

    public void setUsername(@NotNull String username) {
        Preconditions.checkNotNull(username);
        this.username = username;
    }

    public @NotNull String getUsername() {
        if (username == null) {
            username = IdUtilLocal.getName(uuid).orElse(null);
        }
        if (username == null) {
            throw new IllegalStateException("[NetworkProfile] Username is null for UUID: " + uuid);
        }
        return username;
    }

    public @NotNull String getUsername(@NotNull String def) {
        if (username == null) {
            username = IdUtilLocal.getName(uuid).orElse(null);
        }
        if (username == null) {
            return def;
        }
        return username;
    }

    public @NotNull Optional<String> getUsernameOptional() {
        if (username == null) {
            username = IdUtilLocal.getName(uuid).orElse(null);
        }
        return Optional.ofNullable(username);
    }

    public static NetworkProfile deserialize(@NotNull String json) {
        try {
            JSONObject obj = new JSONObject(json);
            return new NetworkProfile(UUID.fromString(obj.getString("uuid")), obj.getString("username"))
                    .setLastCached(obj.getLong("lastCached"))
                    .setLastSaved(obj.getLong("lastSaved"))
                    .setLastSeenServer(obj.getString("lastSeenServer"))
                    .setLastSeen(obj.getLong("lastSeen"))
                    .setOnline(obj.getBoolean("online"))
                    .setFirstJoinToSyncGroup(obj.getBoolean("firstJoinToSyncGroup"));
        } catch (Throwable t) {
            Bukkit.getLogger().severe("MALFORMED JSON: " + json);
            throw t;
        }
    }

    @Blocking
    public void saveSync() {
        // Save this profile to the network
        EngineSource.getNetworkService().saveSync(this);
    }

    @NonBlocking
    public void saveAsync() {
        EngineSource.getNetworkService().saveAsync(this);
    }
}
