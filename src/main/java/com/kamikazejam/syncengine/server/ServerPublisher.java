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

/**
 * Utility class for publishing server statues to the network
 */
public class ServerPublisher {

    public static void publish(@NotNull ServerService service, @NotNull ServerStatus status) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService == null) {
            // Do nothing, we aren't running NETWORKED
            return;
        }

        // Ensure validity of ServerService
        Preconditions.checkNotNull(service, "Server service");
        Preconditions.checkNotNull(service.getThisServer(), "thisServer");
        Preconditions.checkNotNull(service.getThisServer().getName(), "thisServer#name");

        // Handle individual statuses
        switch (status) {
            case JOIN -> Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () -> {
                try {
                    String syncID = service.getThisServer().getName();
                    String syncGroup = service.getThisServer().getGroup();

                    SyncServerPacket packet = SyncServerPacket.of(syncID, syncGroup);
                    service.info("ServerPublisher: Publishing JOIN event");
                    service.getMultiChannel().publishAsync(ServerStatus.JOIN.getChannel(), packet);

                    Bukkit.getPluginManager().callEvent(new SyncServerPublishJoinEvent(syncID, syncGroup));

                } catch (Exception ex) {
                    redisService.getLogger().info(ex, "ServerService ServerPublisher: Error publishing JOIN event");
                }
            });
            case PING -> Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () -> {
                try {
                    String syncID = service.getThisServer().getName();
                    String syncGroup = service.getThisServer().getGroup();

                    SyncServerPacket packet = SyncServerPacket.of(syncID, syncGroup);
                    // service.info("ServerPublisher: Publishing JOIN event"); // commented to prevent spam
                    service.getMultiChannel().publishAsync(ServerStatus.PING.getChannel(), packet);

                    Bukkit.getPluginManager().callEvent(new SyncServerPublishPingEvent(syncID, syncGroup));

                } catch (Exception ex) {
                    redisService.getLogger().info(ex, "ServerService ServerPublisher: Error publishing PING event");
                }
            });
            case QUIT -> {
                try {
                    // Run sync to ensure publish completes before shutdown
                    String syncID = service.getThisServer().getName();
                    String syncGroup = service.getThisServer().getGroup();

                    SyncServerPacket packet = SyncServerPacket.of(syncID, syncGroup);
                    service.info("ServerPublisher: Publishing QUIT event");
                    service.getMultiChannel().publishSync(ServerStatus.QUIT.getChannel(), packet);

                    Bukkit.getPluginManager().callEvent(new SyncServerPublishQuitEvent(syncID, syncGroup));

                } catch (Exception ex) {
                    service.info(ex, "ServerService ServerPublisher: Error publishing QUIT event");
                }
            }
        }
    }
}
