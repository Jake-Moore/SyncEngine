package com.kamikazejam.syncengine.connections.redis;

import com.kamikazejam.kamicommon.redis.RedisAPI;
import com.kamikazejam.kamicommon.redis.RedisConnector;
import com.kamikazejam.kamicommon.util.log.LoggerService;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.connections.config.RedisConfig;
import lombok.Getter;
import org.jetbrains.annotations.Blocking;

@SuppressWarnings("DuplicatedCode")
@Getter
public class RedisService implements Service {
    private boolean running = false;
    private RedisAPI api;
    private final LoggerService logger;

    public RedisService() {
        this.logger = new RedisLoggerService();
    }

    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        // Create our RedisAPI instance
        logger.debug("Starting RedisService...");
        this.api = RedisConnector.getAPI(RedisConfig.get(), logger);
        this.running = true;
        logger.info("RedisService started!");
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            logger.warn("RedisService.shutdown() called while service is not running!");
            return true;
        }
        // Shutdown the RedisAPI instance
        if (this.api != null) {
            this.api.shutdown();
        }
        this.running = false;
        return true;
    }

    /**
     * Test the ping to the storage service. Will block thread until ping is calculated.
     * @return The ping (in Nanoseconds) to the storage service.
     */
    @Blocking
    public long getPingNano() {
        long start = System.nanoTime();
        String ignored = api.getCmdsSync().ping();
        return System.nanoTime() - start;
    }
}
