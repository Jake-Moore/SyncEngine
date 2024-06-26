package com.kamikazejam.syncengine.connections.monitor;

import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class RedisMonitor implements Runnable, Service {

    private final RedisService service;
    private BukkitTask task = null;

    public RedisMonitor(RedisService service) {
        this.service = service;
    }

    @Override
    public boolean start() {
        if (this.task == null) {
            this.task = Bukkit.getScheduler().runTaskTimerAsynchronously(SyncEnginePlugin.get(), this, 0L, 20L);
        } else {
            throw new IllegalStateException("Redis Monitor is already running: cannot start");
        }
        return true;
    }

    @Override
    public boolean shutdown() {
        if (this.task != null) {
            task.cancel();
        } else {
            throw new IllegalStateException("Redis Monitor is not running: cannot stop");
        }
        return true;
    }

    @Override
    public boolean isRunning() {
        return this.task != null;
    }

    @Override
    public void run() {
        // Check if connection is alive
        if (service.getRedis() == null) return;

        try {
            String reply = service.getRedis().sync().ping();

            if (reply.contains("PONG")) {
                this.handleConnected();
            } else {
                this.handleDisconnected();
                service.info("Non-PONG ping reply in Redis Monitor: " + reply);
            }
        } catch (Exception ex) {
            // Failed, assume disconnected
            this.handleDisconnected();
            this.service.info(ex, "Error in Redis Monitor task");
        }
    }

    private void handleConnected() {
        if (!this.service.isRedisConnected()) {
            this.service.setRedisConnected(true);
            if (!this.service.isRedisInitConnect()) {
                this.service.setRedisInitConnect(true);
                service.debug("Redis initial connection succeeded");
            } else {
                service.debug("Redis connection restored");
            }
        }
    }

    private void handleDisconnected() {
        if (this.service.isRedisConnected()) {
            this.service.setRedisConnected(false);
            service.info("Redis connection lost");
        }
        this.service.connectRedis();
    }
}
