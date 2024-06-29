package com.kamikazejam.syncengine.base.mode;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.server.ServerService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public enum SyncMode {
    STANDALONE,
    NETWORKED;

    public @NotNull StorageMode getStorageMode(KamiConfig config) {
        if (this == NETWORKED) {
            return StorageMode.MONGODB;
        }
        return StorageMode.valueOf(config.getString("standalone.storage"));
    }


    // ---------------------------------------------------------------------------- //
    //                           MODE SERVICE MANAGEMENT                            //
    // ---------------------------------------------------------------------------- //
    public void enableServices() {
        // Enable REDIS
        //   MongoDB handled by StorageMode
        if (this != NETWORKED) {
            return;
        }
        this.getRedisService();
    }

    private RedisService redisService = null;

    public @Nullable RedisService getRedisService() {
        if (this != SyncMode.NETWORKED) {
            return null;
        }

        if (redisService == null) {
            redisService = new RedisService();
            if (!redisService.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start RedisService, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return redisService;
    }

    private ServerService serverService = null;

    public @Nullable ServerService getServerService() {
        if (this != SyncMode.NETWORKED) {
            return null;
        }

        if (serverService == null) {
            serverService = new ServerService();
            if (!serverService.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start ServerService, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return serverService;
    }

    public void disableServices() {
        if (redisService != null) {
            redisService.shutdown();
            redisService = null;
        }
        if (serverService != null) {
            serverService.shutdown();
            serverService = null;
        }
    }
}
