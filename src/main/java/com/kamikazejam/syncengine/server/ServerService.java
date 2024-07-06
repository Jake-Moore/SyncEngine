package com.kamikazejam.syncengine.server;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.redis.RedisMultiChannel;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings({"UnusedReturnValue", "unused"})
@Getter
public class ServerService extends LoggerService implements Runnable, Service {

    public static final long ASSUME_OFFLINE_SECONDS = 60;
    public static final long PING_FREQUENCY_SECONDS = 30;

    private final JavaPlugin plugin;
    private final SyncServer thisServer;
    // Map<DbName, List<ServerName>> // names in the list are Case SENSITIVE
    private final ConcurrentMap<String, Set<String>> dbNameServersMap = new ConcurrentHashMap<>();
    // Map<ServerName, SyncServer>
    private final ConcurrentMap<String, SyncServer> syncServerMap = new ConcurrentHashMap<>(); // Case SENSITIVE

    private ServerPublisher publisher = null;
    private BukkitTask pingTask = null;
    @Getter
    private boolean running = false;
    @Getter
    private RedisMultiChannel<SyncServerPacket> channel = null;

    public ServerService() {
        this.plugin = EngineSource.get();
        this.thisServer = new SyncServer(EngineSource.getSyncServerId(), EngineSource.getSyncServerGroup(), System.currentTimeMillis(), true);
        this.syncServerMap.put(thisServer.getName(), thisServer);
    }

    public boolean start() {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService == null) {
            // Do nothing if we don't have a RedisService (i.e. we are not NETWORKED)
            return true;
        }

        try {
            this.publisher = new ServerPublisher(this);

            boolean sub = subscribe(redisService);
            SyncEngineAPI.getCaches().forEach((name, cache) -> this.publisher.publishJoin(cache.getDatabaseName()));
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
        if (this.channel == null) {
            this.channel = redis.getApi().registerMultiChannel(SyncServerPacket.class, ServerEvent.getChannels().toArray(new String[0]));
        }

        channel.subscribe((c, packet) -> {
            ServerEvent event = ServerEvent.fromChannel(c);
            if (event == null) { return; }

            // Check that this redis message is for us
            if (!packet.getSyncGroup().equalsIgnoreCase(EngineSource.getSyncServerGroup())) { return; }

            if (event.equals(ServerEvent.JOIN)) {
                handleJoin(packet.getDbName(), packet.getSyncID(), packet.getSyncGroup());
            } else if (event.equals(ServerEvent.QUIT)) {
                handleQuit(packet.getDbName(), packet.getSyncID());
            } else if (event.equals(ServerEvent.PING)) {
                handlePing(packet.getDbName(), packet.getSyncID(), packet.getSyncGroup());
            }
        });
        return true;
    }

    @NotNull
    public Set<String> getServers(@NotNull String dbName) {
        // Add thisServer as a server connected to this db
        Set<String> servers = new HashSet<>();
        servers.add(thisServer.getName());
        servers.addAll(dbNameServersMap.getOrDefault(dbName, new HashSet<>()));
        return servers;
    }

    @NotNull
    public Set<SyncServer> getSyncServers(@NotNull String dbName) {
        Set<SyncServer> syncServers = new HashSet<>();
        getServers(dbName).forEach((serverName) -> {
            SyncServer server = syncServerMap.getOrDefault(serverName, null);
            if (server == null) {
                Bukkit.getLogger().warning("[ServerService] Could not find Server: " + serverName + " in dbMap: " + dbName);
                return;
            }
            syncServers.add(server);
        });
        return syncServers;
    }

    @NotNull
    public SyncServer register(@NotNull String dbName, @NotNull String name, @NotNull String group, boolean online) {
        Preconditions.checkNotNull(name);
        // Update the dbMap to include this server
        Set<String> servers = getServers(dbName);
        servers.add(name);
        dbNameServersMap.put(dbName, servers);

        // Case 1 - ThisServer - edit the local object
        if (name.equalsIgnoreCase(thisServer.getName())) {
            thisServer.setOnline(online);
            thisServer.setGroup(group);
            return thisServer;
        }
        // Case 2 - other server - create a new object and store it
        SyncServer server = new SyncServer(name, group, System.currentTimeMillis(), online);
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

    void handlePing(@NotNull String dbName, @NotNull String serverName, @NotNull String serverGroup) {
        if (syncServerMap.containsKey(serverName)) {
            syncServerMap.get(serverName).setLastPing(System.currentTimeMillis());
        } else {
            this.handleJoin(dbName, serverName, serverGroup);
        }
    }

    void handleJoin(@NotNull String dbName, @NotNull String serverName, @NotNull String serverGroup) {
        if (syncServerMap.containsKey(serverName)) {
            syncServerMap.get(serverName).setOnline(true);
            syncServerMap.get(serverName).setLastPing(System.currentTimeMillis());
        } else {
            this.debug("Server: " + serverName + " Started for DB: " + dbName + " (adding)");
            this.register(dbName, serverName, serverGroup, true);

            // Send other servers a ping of this server, so that this existing server
            //  can be added to the new 'joining' server's list
            doPing();
        }
    }

    void handleQuit(@NotNull String dbName, @NotNull String serverName) {
        if (syncServerMap.containsKey(serverName)) {
            this.debug("Server: " + serverName + " Stopped for DB: " + dbName + " (removing)");
            syncServerMap.get(serverName).setOnline(false);
        }
    }

    void handleUpdateName(@NotNull String dbName, String oldName, String serverName) {
        if (syncServerMap.containsKey(oldName)) {
            SyncServer server = syncServerMap.get(oldName);
            server.setName(serverName);
            syncServerMap.remove(oldName);
            syncServerMap.put(serverName, server);
        }
    }

    private void doPing() {
        SyncEngineAPI.getCaches().forEach((name, cache) -> this.publisher.publishPing(cache.getDatabaseName()));
        this.thisServer.setLastPing(System.currentTimeMillis());
        this.thisServer.setOnline(true);
    }

    public boolean shutdown() {
        try {
            if (this.pingTask != null) {
                this.pingTask.cancel();
            }

            SyncEngineAPI.getCaches().forEach((name, cache) -> this.publisher.publishQuit(cache.getDatabaseName(), true));
            this.publisher = null;
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
            if (!name.equalsIgnoreCase(this.thisServer.getName())) {
                if (server.isOnline()) {
                    long pingExpiredAt = System.currentTimeMillis() - (ServerService.ASSUME_OFFLINE_SECONDS * 1000);
                    if (server.getLastPing() <= pingExpiredAt) {
                        // Assume they're offline
                        server.setOnline(false);
                    }
                }
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
