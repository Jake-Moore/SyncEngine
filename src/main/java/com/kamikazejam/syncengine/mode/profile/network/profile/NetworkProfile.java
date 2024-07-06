package com.kamikazejam.syncengine.mode.profile.network.profile;

import com.kamikazejam.kamicommon.json.JSONObject;
import com.kamikazejam.kamicommon.util.Preconditions;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * {@link NetworkProfile#markLoaded} called in SyncListener.onProfileCachingStart <br>
 * {@link NetworkProfile#markUnloaded} called in SyncProfileLoader.uncache
 */
@Data
@Accessors(chain = true)
@SuppressWarnings("unused")
public class NetworkProfile {

    @Setter
    protected transient String thisServerName;

    private long lastCached = System.currentTimeMillis();
    private long lastSaved = System.currentTimeMillis();
    @Getter(AccessLevel.NONE)
    private @NotNull UUID uuid;
    private @NotNull String username;

    private String lastSeenServer;
    private long lastSeen = 0L;
    private boolean online = false;
    @Setter
    private boolean firstJoinToSyncGroup = false;

    // For Jackson, must have a no-arg constructor
    public NetworkProfile() {}

    public NetworkProfile(@NotNull UUID uuid, @NotNull String username) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");
        Preconditions.checkNotNull(username, "Username cannot be null");
        this.uuid = uuid;
        this.username = username;
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
}
