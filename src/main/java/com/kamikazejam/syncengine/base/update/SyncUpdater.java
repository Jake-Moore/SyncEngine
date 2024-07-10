package com.kamikazejam.syncengine.base.update;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.redis.RedisChannel;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.server.ServerService;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class does nothing unless we have a RedisService (NETWORKED mode)
 */
public abstract class SyncUpdater<K, X extends Sync<K>> implements Service {

    private boolean running = false;

    protected final Cache<K, X> cache;
    protected final String channelName;
    protected RedisChannel<UpdatePacket> channel = null;

    public SyncUpdater(Cache<K, X> cache, String channel) {
        this.cache = cache;
        this.channelName = channel;
    }

    // ----------------------------------------------------- //
    //                       Abstract                        //
    // ----------------------------------------------------- //
    public abstract void handleUpdateType(@NotNull UpdatePacket packet);
    public boolean postShutdown() { return true; }


    // ----------------------------------------------------- //
    //                        Service                        //
    // ----------------------------------------------------- //
    @Override
    public final boolean start() {
        Preconditions.checkState(!running, "SyncUpdater is already running for cache: " + cache.getName());

        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (redisService == null || serverService == null) {
            // Do nothing if we don't have a RedisService
            return running = true;
        }

        if (!subscribe(redisService)) {
            cache.getLoggerService().info("Failed to subscribe to channel " + this.channelName + " in SyncUpdater for cache: " + cache.getName());
            return false;
        }
        return running = true;
    }

    @Override
    @SuppressWarnings("all")
    public final boolean shutdown() {
        Preconditions.checkState(running, "SyncUpdater is not running for cache: " + cache.getName());
        running = false;
        return postShutdown();
    }

    @Override
    public final boolean isRunning() {
        return running;
    }



    // ----------------------------------------------------- //
    //                         Redis                         //
    // ----------------------------------------------------- //
    /**
     * Subscribe to redis so we can receive updates, can't call this unless we are in NETWORKED mode
     */
    private boolean subscribe(@NotNull RedisService redis) {
        if (this.channel == null) {
            this.channel = redis.getApi().registerChannel(UpdatePacket.class, this.channelName);
        }

        // Listen for messages on this channel
        channel.subscribe((c, packet) -> {
            // Require that this redis message is for our sync-group
            ServerService service = Objects.requireNonNull(EngineSource.getServerService());
            String sourceGroup = packet.getSourceGroup();
            if (!service.getThisServer().getGroup().equalsIgnoreCase(sourceGroup)) { return; }

            if (packet.isForSyncUpdater()) {
                // Only message within SyncUpdater is an update (PUSH) request
                receiveUpdateRequest(packet);
            } else {
                // Send to super class for processing
                handleUpdateType(packet);
            }
        });
        return true;
    }

    // Receives a PUSH call from another server
    private void receiveUpdateRequest(@NotNull UpdatePacket packet) {
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (serverService == null) {
            // Do nothing if we don't have a ServerService
            return;
        }

        try {
            // Verify document keys
            String sourceServer = packet.getSourceServer();
            String sourceGroup = packet.getSourceGroup();
            String identifierString = packet.getIdentifier();
            boolean force = packet.isForceLoad();
            if (sourceServer == null || identifierString == null) {
                cache.getLoggerService().info("Source Server or Identifier were null during receiveUpdateRequest in SyncUpdater for packet: " + packet.getIdentifier());
                return;
            }

            // As long as the source server wasn't this one
            if (sourceServer.equalsIgnoreCase(serverService.getThisServer().getName())) {
                return;
            }
            // And the sync group is the same
            if (!sourceGroup.equalsIgnoreCase(serverService.getThisServer().getGroup())) {
                return;
            }

            final K identifier = cache.keyFromString(identifierString);
            if (cache.isCached(identifier) || force) {
                // Using getFromDatabase triggers Sync.cacheCopy, as intended
                cache.runAsync(() -> cache.getFromDatabase(identifier, true).ifPresent(sync -> {
                    cache.getLoggerService().debug("Received update request in SyncUpdater for " + cache.getName() + ":" + cache.keyToString(sync.getId()) + " version: " + sync.getVersion());
                    cache.cache(sync);
                }));
            }
        } catch (IllegalPluginAccessException e) {
            if (!EngineSource.get().isEnabled()) {
                return;
            }
            cache.getLoggerService().info(e, "Error2 with received update request in SyncUpdater for PUSH: " + packet.getIdentifier());

        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error with received update request in SyncUpdater for PUSH: " + packet.getIdentifier());
        }
    }
    
    
    
    

    // Default: force=false, async=true
    public final boolean pushUpdate(X sync, boolean force, boolean async) {
        @Nullable RedisService redis = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (redis == null || serverService == null) {
            // Do nothing if we don't have one of the NETWORKED services
            return true;
        }

        try {
            Preconditions.checkNotNull(sync, "Sync cannot be null in SyncUpdater (pushUpdate)");
            UpdatePacket packet = createPacket(sync.getId(), force);
            packet.setForSyncUpdater(true);
            channel.publish(packet, !async);
            return true;
        } catch (IllegalPluginAccessException e) {
            // Try again sync
            if (async) {
                return this.pushUpdate(sync, force, false);
            }
            return false;
        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Failed to push update from SyncUpdater for Sync: " + cache.keyToString(sync.getId()));
            return false;
        }
    }

    private @NotNull UpdatePacket createPacket(K identifier, boolean forceLoad) {
        ServerService serverService = Objects.requireNonNull(EngineSource.getServerService());

        return new UpdatePacket(
                serverService.getThisServer().getGroup(),
                serverService.getThisServer().getName(),
                cache.keyToString(identifier),
                forceLoad,
                false
        );
    }

    /**
     * @return If we are in NETWORKED mode (with redis and server service available)
     */
    public boolean isEnabled() {
        return EngineSource.getRedisService() != null && EngineSource.getServerService() != null;
    }
}
