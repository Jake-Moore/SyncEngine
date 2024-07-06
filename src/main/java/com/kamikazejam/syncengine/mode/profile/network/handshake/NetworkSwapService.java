package com.kamikazejam.syncengine.mode.profile.network.handshake;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.redis.RedisChannel;
import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.mode.profile.listener.ProfileListener;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Only enables if there is a RedisService to use.

 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class NetworkSwapService extends LoggerService implements Service {

    private final Map<UUID, CompletableFuture<Boolean>> verifications = new HashMap<>();
    private final String channelName = "sync-network-swap-service";

    private boolean running = false;
    protected RedisChannel<NetworkSwapPacket> channel = null;

    public NetworkSwapService() {}

    @Override
    public boolean start() {
        info("Starting NetworkSwapService...");
        Preconditions.checkState(!running, "NetworkSwapService is already running!");

        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (redisService == null || serverService == null) {
            // Do nothing without a RedisService and ServerService
            return true;
        }

        boolean sub = subscribe(redisService, serverService);
        running = true;
        info("NetworkSwapService started!");
        return sub;
    }

    @Override
    @SuppressWarnings("all")
    public boolean shutdown() {
        Preconditions.checkState(running, "NetworkSwapService is not running!");
        running = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    private boolean subscribe(@NotNull RedisService redis, @NotNull ServerService server) {
        if (this.channel == null) {
            this.channel = redis.getApi().registerChannel(NetworkSwapPacket.class, this.channelName);
        }

        // Listen for messages
        channel.subscribe((c, packet) -> {
            if (!packet.getTargetServer().equalsIgnoreCase(server.getThisServer().getName())) { return; }
            // debug("Received message on channel: " + this.channelName + " - " + JacksonUtil.serialize(packet));

            if (packet.isRequest()) {
                handleRequest(packet);
            } else {
                handleReply(packet);
            }
        });
        return true;
    }

    // Handle the REQUEST for a handshake (from another server)
    // Another server is being joined by the player (who is assuming-ly on this server)
    // - check if they're online, if they are: save them
    // - after save (or if they're not online) send reply
    @SuppressWarnings("UnstableApiUsage")
    private void handleRequest(@NotNull final NetworkSwapPacket packet) {
        ServerService serverService = EngineSource.getServerService();
        RedisService redisService = EngineSource.getRedisService();
        Preconditions.checkNotNull(serverService, "ServerService cannot be null");
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");

        Preconditions.checkNotNull(packet, "NetworkSwapService cannot be null");
        final @Nullable Player player = Bukkit.getServer().getPlayer(packet.getUuid());

        // Should be close to immediate, not requiring a future or async thread
        boolean online = PlayerUtil.isFullyValidPlayer(player);
        packet.setFound(TriState.byBoolean(online));

        // if online, store that knowledge so we can stop the .markUnloaded on player quit
        if (online) {
            ProfileListener.swapMap.put(packet.getUuid(), System.currentTimeMillis());
        }

        // Swap the target/sender servers
        packet.setTargetServer(packet.getSenderServer());
        packet.setSenderServer(serverService.getThisServer().getName());
        packet.setRequest(false); // Is a reply

        // Send the reply
        channel.publishAsync(packet);
    }

    private void handleReply(@NotNull NetworkSwapPacket packet) {
        Preconditions.checkNotNull(packet, "NetworkSwapService cannot be null");

        // Require valid handshake
        CompletableFuture<Boolean> future = verifications.get(packet.getHandshakeId());
        if (future == null || future.isDone()) { return; }
        TriState triState = packet.getFound();
        if (triState == null || triState == TriState.NOT_SET) {
            future.completeExceptionally(new IllegalStateException("TriState is not set in NetworkSwapService (in handleReply)"));
            return;
        }
        future.complete(packet.getFound().toBoolean());
    }

    public CompletableFuture<Boolean> requestVerification(NetworkProfile np, SyncServer targetServer) {
        ServerService serverService = EngineSource.getServerService();
        RedisService redisService = EngineSource.getRedisService();
        Preconditions.checkNotNull(serverService, "ServerService cannot be null");
        Preconditions.checkNotNull(redisService, "RedisService cannot be null");

        UUID handshakeId = UUID.randomUUID();
        NetworkSwapPacket packet = new NetworkSwapPacket(true, System.nanoTime(), serverService.getThisServer().getName(), targetServer.getName(), np.getUUID(), np.getUsername(), handshakeId);

        // Send the handshake REQUEST to the target server
        CompletableFuture<Boolean> future = new CompletableFuture<Boolean>()
                .orTimeout(5, TimeUnit.SECONDS);
        verifications.put(handshakeId, future);

        channel.publishAsync(packet);
        return future;
    }

    // ----------------------------------------------------- //
    //                      LoggerService                    //
    // ----------------------------------------------------- //
    @Override
    public String getLoggerName() {
        return "NetworkSwapService";
    }
    @Override
    public Plugin getPlugin() {
        return EngineSource.get();
    }
    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
    }
}
