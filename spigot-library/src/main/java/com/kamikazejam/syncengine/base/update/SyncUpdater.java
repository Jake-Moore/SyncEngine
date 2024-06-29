package com.kamikazejam.syncengine.base.update;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.server.ServerService;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.bson.Document;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * This class does nothing unless we have a RedisService (NETWORKED mode)
 */
public abstract class SyncUpdater<K, X extends Sync<K>> implements Service {
    protected static final String FOR_SYNC_UPDATER = "forSyncUpdater";
    protected static final String KEY_SOURCE_SERVER = "sourceServer";
    protected static final String KEY_SOURCE_GROUP = "sourceGroup";
    protected static final String KEY_IDENTIFIER = "identifier";
    protected static final String KEY_FORCE_LOAD = "forceLoad";

    private boolean running = false;
    private RedisPubSubReactiveCommands<String, String> reactive = null;

    protected final Cache<K, X> cache;
    protected final String channel;
    public SyncUpdater(Cache<K, X> cache, String channel) {
        this.cache = cache;
        this.channel = channel;
    }

    // ----------------------------------------------------- //
    //                       Abstract                        //
    // ----------------------------------------------------- //
    public abstract void handleUpdateType(Document document);
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
            cache.getLoggerService().info("Failed to subscribe to channel " + this.channel + " in SyncUpdater for cache: " + cache.getName());
            return false;
        }
        return running = true;
    }

    @Override
    @SuppressWarnings("all")
    public final boolean shutdown() {
        Preconditions.checkState(running, "SyncUpdater is not running for cache: " + cache.getName());
        if (reactive != null) {
            reactive.unsubscribe(channel);
        }
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
    private boolean subscribe(@NotNull RedisService redisService) {
        try {
            StatefulRedisPubSubConnection<String, String> connection = redisService.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe(channel).subscribe();

            reactive.observeChannels()
                    .filter(pm -> pm.getChannel().equals(channel))
                    .doOnNext(patternMessage -> {
                        // Can't process invalid data
                        if (patternMessage == null || patternMessage.getMessage() == null) { return; }
                        
                        try {
                            Document document = Document.parse(patternMessage.getMessage());
                            if (document == null) { return; } // Invalid message / document -> can't process

                            // Require that this redis message is for our sync-group
                            ServerService serverService = Objects.requireNonNull(EngineSource.getServerService());
                            String sourceGroupString = document.getString(KEY_SOURCE_GROUP);
                            if (!serverService.getThisServer().getGroup().equalsIgnoreCase(sourceGroupString)) { return; }

                            boolean forHere = document.getBoolean(FOR_SYNC_UPDATER);
                            if (forHere) {
                                // Only message within SyncUpdater is an update (PUSH) request
                                receiveUpdateRequest(document);
                            } else {
                                // Send to super class for processing
                                handleUpdateType(document);
                            }
                        } catch (IllegalArgumentException e) {
                            cache.getLoggerService().severe("Invalid UpdateType in SyncUpdater for packet: '" + patternMessage.getMessage() + "' in channel: " + channel);
                        } catch (Exception ex) {
                            cache.getLoggerService().severe(ex, "Error reading incoming packet in SyncUpdater for packet: '" + patternMessage.getMessage() + "' in channel: " + channel);
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error subscribing in SyncUpdater");
            return false;
        }
    }

    // Receives a PUSH call from another server
    private void receiveUpdateRequest(@NotNull Document document) {
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (serverService == null) {
            // Do nothing if we don't have a ServerService
            return;
        }

        try {
            // Verify document keys
            String sourceServerString = document.getString(KEY_SOURCE_SERVER);
            String sourceGroup = document.getString(KEY_SOURCE_GROUP);
            String identifierString = document.getString(KEY_IDENTIFIER);
            boolean force = document.getBoolean(KEY_FORCE_LOAD, false);
            if (sourceServerString == null || identifierString == null) {
                cache.getLoggerService().info("Source Server or Identifier were null during receiveUpdateRequest in SyncUpdater for packet: " + document);
                return;
            }

            // As long as the source server wasn't this one
            if (sourceServerString.equalsIgnoreCase(serverService.getThisServer().getName())) {
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
            cache.getLoggerService().info(e, "Error2 with received update request in SyncUpdater for PUSH: " + document);

        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error with received update request in SyncUpdater for PUSH: " + document);
        }
    }
    
    
    
    

    // Default: force=false, async=true
    public final boolean pushUpdate(X sync, boolean force, boolean async) {
        @Nullable RedisService redisService = EngineSource.getRedisService();
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (redisService == null || serverService == null) {
            // Do nothing if we don't have one of the NETWORKED services
            return true;
        }

        try {
            Preconditions.checkNotNull(sync, "Sync cannot be null in SyncUpdater (pushUpdate)");
            final String json = createSyncUpdaterDocument(sync.getId())
                    .append(KEY_FORCE_LOAD, force)
                    .toJson();
            if (async) {
                cache.runAsync(() -> redisService.getRedis().async().publish(channel, json));
            } else {
                redisService.getRedis().sync().publish(channel, json);
            }
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

    private @NotNull Document createDocument(K identifier, boolean forSyncUpdater) {
        ServerService serverService = Objects.requireNonNull(EngineSource.getServerService());

        Document document = new Document();
        document.append(FOR_SYNC_UPDATER, forSyncUpdater);
        document.append(KEY_SOURCE_SERVER, serverService.getThisServer().getName());
        document.append(KEY_SOURCE_GROUP, serverService.getThisServer().getGroup());
        document.append(KEY_IDENTIFIER, cache.keyToString(identifier));
        return document;
    }

    // Marks a message as being for this abstract class' receiver
    private @NotNull Document createSyncUpdaterDocument(K identifier) {
        return this.createDocument(identifier, true);
    }

    public final @NotNull Document createDocument(K identifier) {
        return this.createDocument(identifier, false);
    }
}
