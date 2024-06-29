package com.kamikazejam.syncengine.mode.profile.network.profile.store;

import com.kamikazejam.kamicommon.util.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.SyncServer;
import com.kamikazejam.syncengine.util.JacksonUtil;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Networked NetworkProfileStore implementation
 * This class assumes the presence of a RedisService.
 */
@Getter
@SuppressWarnings({"DuplicatedCode", "unused"})
public class NetworkProfileRedis extends NetworkProfileStore {

    public NetworkProfileRedis() {}

    // ----------------------------------------------------- //
    //                  NetworkProfileStore                  //
    // ----------------------------------------------------- //
    @Override
    public void verifyPlayerOrigin(@NotNull UUID uuid, @NotNull String username, @NotNull SyncServer server) {
        final String hashKey = getHashKey();
        final String keyString = getKeyString(uuid);

        // Check 1 - Use our server service to make sure that server is alive
        if (!server.isOnline()) {
            try {
                // Server is offline, we should unmark this player as being from there
                getRedis().sync().hdel(hashKey, keyString);
            } catch (Exception ex) {
                info(ex, "Error on hdel: " + uuid + " in Redis Network Service");
            }
            return;
        }

        // Check 2 - Ping the server and verify they have the player we're looking for
        // TODO implement
        // EngineSource.getPlayerService().getPublisher().publishIsOnline(uuid);
    }

    @Override
    public boolean save(@NotNull NetworkProfile networkProfile) {
        Preconditions.checkNotNull(networkProfile, "NetworkProfile cannot be null");
        Preconditions.checkNotNull(networkProfile.getUUID(), "NetworkProfile UUID cannot be null");
        Preconditions.checkNotNull(networkProfile.getUsername(), "NetworkProfile username cannot be null");

        final String hashKey = getHashKey();
        final String keyString = getKeyString(networkProfile.getUUID());
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");

        // Convert networkProfile into a json string
        String json = JacksonUtil.toJson(networkProfile);
        Preconditions.checkNotNull(json, "JSON cannot be null");

        try {
            getRedis().async().hset(hashKey, keyString, json);
            return true;
        } catch (Exception ex) {
            info(ex, "Error saving NetworkProfile in Redis Network Service for UUID: " + networkProfile.getUUID());
            return false;
        }
    }

    @Override
    public @NotNull Optional<NetworkProfile> get(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");

        final String hashKey = getHashKey();
        final String keyString = getKeyString(uuid);
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");

        // We just need a connection to the redis, so we use IdProfile's since we know it's available
        try {
            if (getRedis().sync().hexists(hashKey, keyString)) {
                final String json = getRedis().sync().hget(hashKey, keyString);
                if (json != null && !json.isEmpty()) {
                    return Optional.of(createNetworkProfile(json));
                } else {
                    return Optional.empty();
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception ex) {
            info(ex, "Error getting network profile from Key in Redis Network Service");
            return Optional.empty();
        }
    }

    @Override
    public @NotNull List<NetworkProfile> getAll(boolean online) {
        final String hashKey = getHashKey();
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");

        try {
            return getRedis().sync().hgetall(hashKey).values().stream()
                    .filter(json -> json != null && !json.isEmpty())
                    .map(this::createNetworkProfile)
                    .filter(profile -> !online || profile.isOnline())
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            info(ex, "Error getting all network profiles from Redis Network Service");
            return Collections.emptyList();
        }
    }

    @Override
    public boolean has(@NotNull UUID uuid) {
        Preconditions.checkNotNull(uuid, "UUID cannot be null");

        final String hashKey = getHashKey();
        final String keyString = getKeyString(uuid);
        Preconditions.checkNotNull(hashKey, "Hash key cannot be null");
        Preconditions.checkNotNull(keyString, "Key cannot be null");

        try {
            return getRedis().sync().hexists(hashKey, keyString);
        } catch (Exception ex) {
            info("Error checking if hexists in Redis Network Service for UUID:" + keyString);
        }
        return false;
    }


    // ----------------------------------------------------- //
    //                     Helper Methods                    //
    // ----------------------------------------------------- //
    public @NotNull StatefulRedisConnection<String, String> getRedis() {
        RedisService redisService = EngineSource.getRedisService();
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");
        return redisService.getRedis();
    }
    @SneakyThrows
    private @NotNull NetworkProfile createNetworkProfile(@NotNull String json) {
        NetworkProfile profile = JacksonUtil.getMapper().readValue(json, NetworkProfile.class);
        profile.setThisServerName(getThisServerName());
        return profile;
    }

    // ----------------------------------------------------- //
    //                      LoggerService                    //
    // ----------------------------------------------------- //
    @Override
    public String getLoggerName() {
        return "NetworkProfileRedis";
    }
}
