package com.kamikazejam.syncengine.mode.profile.handshake;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.kamicommon.util.data.Pair;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.SyncProfileCache;
import com.kamikazejam.syncengine.mode.profile.loader.SyncProfileLoader;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import com.kamikazejam.syncengine.util.JacksonUtil;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileHandshakeService<X extends SyncProfile> implements Service {

    private final Map<UUID, CompletableFuture<@Nullable Pair<String, Long>>> handshakeMap = new HashMap<>();

    private final SyncProfileCache<X> cache;
    private final String channelRequest;
    private final String channelReply;

    private boolean running = false;
    private RedisPubSubReactiveCommands<String, String> reactive = null;

    public ProfileHandshakeService(@NotNull SyncProfileCache<X> cache) {
        Preconditions.checkNotNull(cache, "Cache cannot be null");
        Preconditions.checkNotNull(cache.getName(), "Cache name cannot be null");
        this.cache = cache;
        this.channelRequest = "sync-profile-handshake-" + cache.getName() + "-request";
        this.channelReply = "sync-profile-handshake-" + cache.getName() + "-reply";
    }

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "Profile Handshake Service is already running!");

        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (redisService == null || serverService == null) {
            // Do nothing if we don't have a RedisService or ServerService
            return running = true;
        }

        boolean sub = subscribe();
        running = true;
        return sub;
    }

    @Override
    @SuppressWarnings("all")
    public boolean shutdown() {
        Preconditions.checkState(running, "Profile Handshake Service is not running!");
        if (reactive != null) {
            reactive.unsubscribe(channelRequest, channelReply);
        }
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private boolean subscribe() {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");
        Preconditions.checkNotNull(serverService, "ServerService cannot be null");

        try {
            StatefulRedisPubSubConnection<String, String> connection = redisService.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe(channelRequest, channelReply).subscribe();

            reactive.observeChannels()
                    .filter(pm -> pm.getChannel().equals(channelRequest) || pm.getChannel().equals(channelReply))
                    .doOnNext(patternMessage -> {
                        ProfileHandshakePacket packet = ProfileHandshakePacket.fromJSON(patternMessage.getMessage());
                        if (packet != null) {
                            if (packet.getTargetServer().equalsIgnoreCase(serverService.getThisServer().getName())) {
                                if (patternMessage.getChannel().equals(channelRequest)) {
                                    handleRequest(packet);
                                } else if (patternMessage.getChannel().equals(channelReply)) {
                                    handleReply(packet);
                                }
                            }
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error subscribing in Profile Handshake Service for channels: " + channelRequest + ", " + channelReply);
            return false;
        }
    }

    private void handleRequest(@NotNull final ProfileHandshakePacket packet) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");
        Preconditions.checkNotNull(serverService, "ServerService cannot be null");


        Preconditions.checkNotNull(packet, "ProfileHandshakePacket cannot be null");
        // Another server is being joined by the player (who is assuming-ly on this server)
        // - check if they're online, if they are: save them
        // - after save (or if they're not online) send reply
        cache.getLoggerService().debug("Received handshake request for " + packet.getUuid() + " [" + packet.getSenderServer() + " -> " + packet.getTargetServer() + "] (login: " + packet.isLogin() + ")");
        final @Nullable Player player = cache.getPlugin().getServer().getPlayer(packet.getUuid());

        // Run async, asap (cache.runAsync uses Bukkit, which may wait for tick completion (which is slow: 0-50ms))
        CompletableFuture.supplyAsync(() -> {
            if (PlayerUtil.isFullyValidPlayer(player)) {
                // This cache operation is just a map lookup, it doesn't matter which async executor we use
                cache.getFromCache(player).ifPresent(profile -> {
                    // If this handshake represents a login from another server, fire the profile leaving method
                    if (packet.isLogin()) {
                        cache.onProfileLeavingLocal(player, profile);
                        // If receiving a request from another server, and currently swapping there:
                        //   1. set the object as 'read only'
                        //   2. disable the autosave for that sync in SyncProfileCache (via readOnly)
                        profile.setReadOnlyTimeStamp(System.currentTimeMillis());
                    }

                    packet.setData(Pair.of(JacksonUtil.toJson(profile), profile.getVersion()));
                    profile.setHandshakeStartTimestamp(System.currentTimeMillis());

                    // cache.save(profile); // no need to save when we're redis-ing the json
                });
            }
            // Time to send the packet back as a reply (swap target/sender servers and update the JSON)
            packet.setTargetServer(packet.getSenderServer());
            packet.setSenderServer(serverService.getThisServer().getName());
            String json = packet.toDocument().toJson();
            Preconditions.checkNotNull(json, "JSON cannot be null for sendReply in ProfileHandshakeService");
            redisService.getRedis().async().publish(channelReply, json);
            cache.getLoggerService().debug("Sending reply for handshake for UUID " + packet.getUuid() + " [" + packet.getSenderServer() + " -> " + packet.getTargetServer() + "] v" + packet.getVersion("?"));
            return null;
        });
    }

    private void handleReply(@NotNull ProfileHandshakePacket packet) {
        Preconditions.checkNotNull(packet, "ProfileHandshakePacket cannot be null");
        Preconditions.checkNotNull(packet.getData(), "JSON cannot be null for handshake in ProfileHandshakeService (in handleReply)");

        // Require valid handshake
        CompletableFuture<@Nullable Pair<String, Long>> future = handshakeMap.get(packet.getHandshakeId());
        if (future == null || future.isDone()) { return; }

        // Complete the future with the JSON of the profile
        future.complete(packet.getData());
    }

    public CompletableFuture<@Nullable Pair<String, Long>> requestHandshake(@NotNull SyncProfileLoader<?> loader, SyncServer targetServer, boolean login, long msStart) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");
        Preconditions.checkNotNull(serverService, "ServerService cannot be null");
        UUID handshakeId = UUID.randomUUID();

        ProfileHandshakePacket packet = new ProfileHandshakePacket(login, msStart, serverService.getThisServer().getName(), loader.getUuid(), targetServer.getName(), handshakeId, null);
        String json = packet.toDocument().toJson();
        Preconditions.checkNotNull(json, "JSON cannot be null for handshake in ProfileHandshakeService");

        // Send the handshake REQUEST to the target server
        CompletableFuture<@Nullable Pair<String, Long>> future = new CompletableFuture<>();
        handshakeMap.put(handshakeId, future);
        redisService.getRedis().async().publish(channelRequest, json);
        return future;
    }
}
