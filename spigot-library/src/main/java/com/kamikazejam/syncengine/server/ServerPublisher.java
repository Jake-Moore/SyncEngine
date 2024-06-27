package com.kamikazejam.syncengine.server;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.PluginSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.server.event.SyncServerPublishJoinEvent;
import com.kamikazejam.syncengine.server.event.SyncServerPublishPingEvent;
import com.kamikazejam.syncengine.server.event.SyncServerPublishQuitEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServerPublisher {

    private final ServerService serverService;

    public ServerPublisher(ServerService serverService) {
        this.serverService = serverService;
    }

    public void publishPing(@NotNull String dbName) {
        @Nullable RedisService redisService = PluginSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(PluginSource.get(), () -> {
            try {
                String syncID = serverService.getThisServer().getName();
                String syncGroup = serverService.getThisServer().getGroup();

                String json = SyncServerPacket.of(dbName, syncID, syncGroup).toDocument().toJson();
                redisService.getRedis().async().publish(ServerEvent.PING.getEvent(), json);

                // Can be Async
                Bukkit.getPluginManager().callEvent(new SyncServerPublishPingEvent(syncID, syncGroup));

            } catch (Exception ex) {
                redisService.info(ex, "ServerService ServerPublisher: Error publishing PING event");
            }
        });
    }

    public void publishJoin(@NotNull String dbName) {
        @Nullable RedisService redisService = PluginSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        Preconditions.checkNotNull(serverService, "Server service");
        Preconditions.checkNotNull(redisService.getRedis(), "Redis");
        Preconditions.checkNotNull(redisService.getRedis().async(), "Redis async");
        Preconditions.checkNotNull(ServerEvent.JOIN.getEvent(), "Join event");
        Preconditions.checkNotNull(serverService.getThisServer(), "thisServer");
        Preconditions.checkNotNull(serverService.getThisServer().getName(), "thisServer#name");
        Bukkit.getScheduler().runTaskAsynchronously(PluginSource.get(), () -> {
            try {
                String syncID = serverService.getThisServer().getName();
                String syncGroup = serverService.getThisServer().getGroup();

                String json = SyncServerPacket.of(dbName, syncID, syncGroup).toDocument().toJson();
                redisService.getRedis().async().publish(ServerEvent.JOIN.getEvent(), json);

                Event e = new SyncServerPublishJoinEvent(syncID, syncGroup);
                Bukkit.getPluginManager().callEvent(e);

            } catch (Exception ex) {
                redisService.info(ex, "ServerService ServerPublisher: Error publishing JOIN event");
            }
        });
    }

    // Sync Method
    public void publishQuit(@NotNull String dbName, boolean callIsSync) {
        @Nullable RedisService redisService = PluginSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        PluginSource.info("Publishing QUIT event for server " + serverService.getThisServer().getName());

        try {
            // Run sync to ensure publish completes before shutdown
            String syncID = serverService.getThisServer().getName();
            String syncGroup = serverService.getThisServer().getGroup();

            String json = SyncServerPacket.of(dbName, syncID, syncGroup).toDocument().toJson();
            redisService.getRedis().sync().publish(ServerEvent.QUIT.getEvent(), json);

            SyncServerPublishQuitEvent event = new SyncServerPublishQuitEvent(syncID, syncGroup);
            if (callIsSync) {
                Bukkit.getPluginManager().callEvent(event);
            } else {
                Bukkit.getScheduler().runTask(PluginSource.get(), () -> Bukkit.getPluginManager().callEvent(event));
            }

        } catch (Exception ex) {
            redisService.info(ex, "ServerService ServerPublisher: Error publishing QUIT event");
        }
    }
}
