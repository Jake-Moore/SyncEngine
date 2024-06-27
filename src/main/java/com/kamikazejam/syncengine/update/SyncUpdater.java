package com.kamikazejam.syncengine.update;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.SyncEnginePlugin;
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

/**
 * This class does nothing unless we have a RedisService (NETWORKED mode)
 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class SyncUpdater<K, X extends Sync<K>> implements Service {

    private static final String KEY_SOURCE_SERVER = "sourceServer";
    private static final String KEY_SOURCE_GROUP = "sourceGroup";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_FORCE_LOAD = "forceLoad";

    private final Cache<K, X> cache;
    private final String channel;
    private RedisPubSubReactiveCommands<String, String> reactive = null;
    private boolean running = false;

    public SyncUpdater(Cache<K, X> cache) {
        this.cache = cache;
        this.channel = "sync-updater-" + cache.getName();
    }

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "SyncUpdater is already running for cache: " + cache.getName());

        @Nullable RedisService redisService = SyncEnginePlugin.get().getRedisService();
        if (redisService == null) {
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
    public boolean shutdown() {
        Preconditions.checkState(running, "SyncUpdater is not running for cache: " + cache.getName());
        if (reactive != null) {
            reactive.unsubscribe(channel);
        }
        running = false;
        return true;
    }

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
                        if (patternMessage != null && patternMessage.getMessage() != null) {
                            receiveUpdateRequest(patternMessage.getMessage());
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error subscribing in SyncUpdater");
            return false;
        }
    }

    private void receiveUpdateRequest(@NotNull String msg) {
        @Nullable ServerService serverService = SyncEnginePlugin.get().getServerService();
        if (serverService == null) {
            // Do nothing if we don't have a ServerService
            return;
        }

        Preconditions.checkNotNull(msg, "Message (packet) cannot be null in SyncUpdater for receiveUpdateRequest");
        try {
            // Verify document
            Document document = Document.parse(msg);
            if (document == null) {
                cache.getLoggerService().info("Document parsed was null during receiveUpdateRequest in SyncUpdater for packet: " + msg);
                return;
            }

            // Verify document keys
            String sourceServerString = document.getString(KEY_SOURCE_SERVER);
            String sourceGroup = document.getString(KEY_SOURCE_GROUP);
            String identifierString = document.getString(KEY_IDENTIFIER);
            boolean force = document.getBoolean(KEY_FORCE_LOAD, false);
            if (sourceServerString == null || identifierString == null) {
                cache.getLoggerService().info("Source Server or Identifier were null during receiveUpdateRequest in SyncUpdater for packet: " + msg);
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
                cache.runAsync(() -> cache.getFromDatabase(identifier, true).ifPresent(sync -> {
                    cache.getLoggerService().debug("Received update request in SyncUpdater for " + cache.getName() + ":" + cache.keyToString(sync.getId()) + " version: " + sync.getVersion());

                    cache.cache(sync);
                }));
            }
        } catch (IllegalPluginAccessException e) {
            if (!SyncEnginePlugin.get().isEnabled()) {
                return;
            }
            cache.getLoggerService().info(e, "Error2 with received update request in SyncUpdater for packet: " + msg);

        } catch (Exception ex) {
            cache.getLoggerService().info(ex, "Error with received update request in SyncUpdater for packet: " + msg);
        }
    }

    // Default: force=false, async=true
    public boolean pushUpdate(@NotNull X sync, boolean force, boolean async) {
        @Nullable RedisService redisService = SyncEnginePlugin.get().getRedisService();
        @Nullable ServerService serverService = SyncEnginePlugin.get().getServerService();
        if (redisService == null || serverService == null) {
            // Do nothing if we don't have one of the NETWORKED services
            return true;
        }

        try {
            Preconditions.checkNotNull(sync, "Sync cannot be null in SyncUpdater (pushUpdate)");
            final Document document = new Document();
            document.append(KEY_SOURCE_SERVER, serverService.getThisServer().getName());
            document.append(KEY_SOURCE_GROUP, serverService.getThisServer().getGroup());
            document.append(KEY_IDENTIFIER, cache.keyToString(sync.getId()));
            document.append(KEY_FORCE_LOAD, force);
            final String json = document.toJson();
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


    @Override
    public boolean isRunning() {
        return running;
    }
}
