package com.kamikazejam.syncraft.connections.redis;

import com.kamikazejam.syncraft.SyncraftPlugin;
import com.kamikazejam.syncraft.base.Service;
import com.kamikazejam.syncraft.base.error.LoggerService;
import com.kamikazejam.syncraft.connections.config.RedisConf;
import com.kamikazejam.syncraft.connections.monitor.RedisMonitor;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;

@Getter
public class RedisService extends LoggerService implements Service {
    private boolean running = false;
    @Setter private boolean redisInitConnect = false;
    @Setter private boolean redisConnected = false;
    @Setter private volatile long lastRedisConnectionAttempt = 0;

    // Redis
    @Getter private RedisClient redisClient = null;
    @Getter private StatefulRedisConnection<String, String> redis = null;
    @Getter private StatefulRedisPubSubConnection<String, String> redisPubSub = null;
    private RedisMonitor redisMonitor = null;

    public RedisService() {}

    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        this.debug("Connecting to Redis");

        boolean redis = this.connectRedis();
        this.running = true;

        if (!redis) {
            this.error("Failed to start RedisService, connection failed.");
            return false;
        }

        this.debug("Connected to Redis");
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("RedisService.shutdown() called while service is not running!");
            return true;
        }

        // Disconnect from Redis
        boolean redis = this.disconnectRedis();
        this.running = false;

        if (!redis) {
            this.error("Failed to shutdown RedisService, disconnect failed.");
            return false;
        }

        this.debug("Disconnected from Redis");
        return true;
    }



    // ------------------------------------------------- //
    //                 Redis Connection                 //
    // ------------------------------------------------- //
    public boolean connectRedis() {
        try {
            this.setLastRedisConnectionAttempt(System.currentTimeMillis());

            // Try connection
            if (redisClient == null) {
                redisClient = RedisClient.create(RedisConf.get().getRedisURI());
            }
            if (redis == null) {
                redis = redisClient.connect();
            }
            if (redisPubSub == null) {
                redisPubSub = redisClient.connectPubSub();
            }

            return true;
        } catch (Exception ex) {
            this.info(ex, "Failed Redis connection attempt");
            return false;
        } finally {
            if (this.redisMonitor == null) {
                this.redisMonitor = new RedisMonitor(this);
            }
            if (!this.redisMonitor.isRunning()) {
                this.redisMonitor.start();
            }
        }
    }
    private boolean disconnectRedis() {
        if (this.redisMonitor != null) {
            this.redisMonitor.shutdown();
        }
        if (this.redis != null && this.redis.isOpen()) {
            this.redis.close();
        }
        this.redis = null;

        if (this.redisClient != null) {
            this.redisClient.shutdown();
            this.redisClient = null;
        }
        return true;
    }




    // ------------------------------------------------- //
    //                   ErrorService                    //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return SyncraftPlugin.get().isDebug();
    }
    @Override
    public Plugin getPlugin() {
        return SyncraftPlugin.get();
    }
    @Override
    public String getLoggerName() {
        return "RedisService";
    }
}
