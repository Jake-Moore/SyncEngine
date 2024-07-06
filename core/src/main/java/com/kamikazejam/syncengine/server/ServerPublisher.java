package com.kamikazejam.syncengine.server;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.event.syncserver.SyncServerPublishJoinEvent;
import com.kamikazejam.syncengine.event.syncserver.SyncServerPublishPingEvent;
import com.kamikazejam.syncengine.event.syncserver.SyncServerPublishQuitEvent;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerPublisher {

    private final ServerService server;
    public ServerPublisher(ServerService server) {
        this.server = server;
    }

    public void publishPing(@NotNull String dbName) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () -> {
            try {
                String syncID = server.getThisServer().getName();
                String syncGroup = server.getThisServer().getGroup();

                SyncServerPacket packet = SyncServerPacket.of(dbName, syncID, syncGroup);
                server.getChannel().publishAsync(ServerEvent.PING.getChannel(), packet);

                // Can be Async
                Bukkit.getPluginManager().callEvent(new SyncServerPublishPingEvent(syncID, syncGroup));

            } catch (Exception ex) {
                redisService.getLogger().info(ex, "ServerService ServerPublisher: Error publishing PING event");
            }
        });
    }

    public void publishJoin(@NotNull String dbName) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        Preconditions.checkNotNull(server, "Server service");
        Preconditions.checkNotNull(server.getThisServer(), "thisServer");
        Preconditions.checkNotNull(server.getThisServer().getName(), "thisServer#name");

        Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () -> {
            try {
                String syncID = server.getThisServer().getName();
                String syncGroup = server.getThisServer().getGroup();

                SyncServerPacket packet = SyncServerPacket.of(dbName, syncID, syncGroup);
                server.getChannel().publishAsync(ServerEvent.JOIN.getChannel(), packet);

                // Can be Async
                Bukkit.getPluginManager().callEvent(new SyncServerPublishJoinEvent(syncID, syncGroup));

            } catch (Exception ex) {
                redisService.getLogger().info(ex, "ServerService ServerPublisher: Error publishing JOIN event");
            }
        });
    }

    // Sync Method
    public void publishQuit(@NotNull String dbName, boolean callIsSync) {
        EngineSource.info("Publishing QUIT event for server " + server.getThisServer().getName());

        try {
            // Run sync to ensure publish completes before shutdown
            String syncID = server.getThisServer().getName();
            String syncGroup = server.getThisServer().getGroup();

            SyncServerPacket packet = SyncServerPacket.of(dbName, syncID, syncGroup);
            server.getChannel().publishSync(ServerEvent.QUIT.getChannel(), packet);

            SyncServerPublishQuitEvent event = new SyncServerPublishQuitEvent(syncID, syncGroup);
            if (callIsSync) {
                Bukkit.getPluginManager().callEvent(event);
            } else {
                Bukkit.getScheduler().runTask(EngineSource.get(), () -> Bukkit.getPluginManager().callEvent(event));
            }

        } catch (Exception ex) {
            server.info(ex, "ServerService ServerPublisher: Error publishing QUIT event");
        }
    }
}
