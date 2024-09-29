package com.kamikazejam.syncengine.server;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.redis.RedisMultiChannel;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings({"UnusedReturnValue", "unused"})
@Getter
public class ServerService extends LoggerService implements Runnable, Service {

    public static final long ASSUME_OFFLINE_SECONDS = 70; // If missed 2 pings, assume offline
    public static final long PING_FREQUENCY_SECONDS = 30;

    private final JavaPlugin plugin;
    private final SyncServer thisServer;

    // The list of all externally connected servers (should also contain thisServer)
    // Map<ServerName, SyncServer>
    private final ConcurrentMap<String, SyncServer> syncServerMap = new ConcurrentHashMap<>(); // Case SENSITIVE

    private BukkitTask pingTask = null;
    @Getter
    private boolean running = false;
    @Getter
    private RedisMultiChannel<SyncServerPacket> multiChannel = null;

    public ServerService() {
        this.plugin = EngineSource.get();
        this.thisServer = new SyncServer(EngineSource.getSyncServerId(), EngineSource.getSyncServerGroup(), System.currentTimeMillis(), true);
        this.syncServerMap.put(thisServer.getName(), thisServer);
    }

    public boolean start() {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService == null) {
            EngineSource.get().getColorLogger().warn("ServerService: RedisService is null, not starting ServerService");
            // Do nothing if we don't have a RedisService (i.e. we are not NETWORKED)
            return true;
        }

        try {
            boolean sub = subscribe(redisService);
            ServerPublisher.publish(this, ServerStatus.JOIN);
            this.pingTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this, (PING_FREQUENCY_SECONDS * 20), (PING_FREQUENCY_SECONDS * 20));
            running = true;
            return sub;
        } catch (Exception ex) {
            this.info(ex, "Error starting Server Service");
            return false;
        }
    }

    // Require a RedisService to call this
    private boolean subscribe(@NotNull RedisService redis) {
        if (this.multiChannel == null) {
            this.multiChannel = redis.getApi().registerMultiChannel(SyncServerPacket.class, ServerStatus.getChannels().toArray(new String[0]));
        }

        multiChannel.subscribe((c, packet) -> {
            ServerStatus event = ServerStatus.fromChannel(c);
            if (event == null) { return; }

            // Check that this redis message is for us (same group)
            if (!packet.getSyncGroup().equalsIgnoreCase(EngineSource.getSyncServerGroup())) { return; }
            // Ignore updates that this server sent
            if (packet.getSyncID().equalsIgnoreCase(thisServer.getName())) { return; }

            if (event.equals(ServerStatus.JOIN)) {
                handleJoin(packet.getSyncID());
            } else if (event.equals(ServerStatus.QUIT)) {
                handleQuit(packet.getSyncID());
            } else if (event.equals(ServerStatus.PING)) {
                handlePing(packet.getSyncID());
            }
        });
        return true;
    }

    @NotNull
    public Collection<SyncServer> getSyncServers() {
        return syncServerMap.values();
    }

    /**
     * Register a server as a part of the same SyncGroup as thisServer<br>
     * It will be cached in the syncServerMap, and should receive pings every few seconds
     */
    @NotNull
    public SyncServer register(@NotNull String name, boolean online) {
        Preconditions.checkNotNull(name);
        final String serverGroup = EngineSource.getSyncServerGroup();

        // Case 1 - ThisServer - edit the local object
        if (name.equalsIgnoreCase(thisServer.getName())) {
            thisServer.setOnline(online);
            thisServer.setGroup(serverGroup);
            return thisServer;
        }

        // Case 2 - other server - create a new object and store it
        SyncServer server = new SyncServer(name, serverGroup, System.currentTimeMillis(), online);
        syncServerMap.put(name, server);
        return server;
    }

    public boolean has(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return syncServerMap.containsKey(name);
    }

    public Optional<SyncServer> get(@NotNull String name) {
        Preconditions.checkNotNull(name);
        return Optional.ofNullable(syncServerMap.get(name));
    }

    private void handleJoin(@NotNull String serverName) {
        final String serverGroup = EngineSource.getSyncServerGroup();
        this.debug("Server: " + serverName + " Joined Group: " + serverGroup + " (adding)");

        @Nullable SyncServer server = get(serverName).orElse(null);
        boolean newlyOnline = (server == null || !server.isOnline());

        if (server != null) {
            server.setOnline(true);
            server.setLastPing(System.currentTimeMillis());
        } else {
            this.register(serverName, true);
        }

        if (newlyOnline) {
            // Send a ping to the new server, so that it can add this server to its list
            doPing();
        }
    }

    private void handlePing(@NotNull String serverName) {
        if (syncServerMap.containsKey(serverName)) {
            syncServerMap.get(serverName).setOnline(true);
            syncServerMap.get(serverName).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(serverName);
        }
    }

    private void handleQuit(@NotNull String serverName) {
        final String serverGroup = EngineSource.getSyncServerGroup();
        if (syncServerMap.containsKey(serverName)) {
            this.debug("Server: " + serverName + " Quit Group: " + serverGroup + " (removing)");
            syncServerMap.get(serverName).setOnline(false);
        }
    }

    void handleUpdateName(String oldName, String serverName) {
        if (syncServerMap.containsKey(oldName)) {
            SyncServer server = syncServerMap.get(oldName);
            server.setName(serverName);
            syncServerMap.remove(oldName);
            syncServerMap.put(serverName, server);
        }
    }

    private void doPing() {
        // Compile a list of all database names with caches connected
        ServerPublisher.publish(this, ServerStatus.PING);

        this.thisServer.setLastPing(System.currentTimeMillis());
        this.thisServer.setOnline(true);
    }

    public boolean shutdown() {
        try {
            if (this.pingTask != null) {
                this.pingTask.cancel();
            }

            ServerPublisher.publish(this, ServerStatus.QUIT);
            running = false;
            return true;
        } catch (Exception ex) {
            this.info("Error shutting down Server Service");
            return false;
        }
    }

    @Override
    public void run() {
        this.doPing();

        this.syncServerMap.forEach((name, server) -> {
            if (name.equalsIgnoreCase(this.thisServer.getName())) { return; }
            if (!server.isOnline()) { return; }

            long pingExpiredAt = System.currentTimeMillis() - (ServerService.ASSUME_OFFLINE_SECONDS * 1000L);
            if (server.getLastPing() <= pingExpiredAt) {
                // Assume they're offline
                server.setOnline(false);
            }
        });
    }


    // ------------------------------------------------------------------------------- //
    //                                  ERROR SERVICE                                  //
    // ------------------------------------------------------------------------------- //
    @Override
    public String getLoggerName() {
        return "ServerService";
    }

    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
    }
}
