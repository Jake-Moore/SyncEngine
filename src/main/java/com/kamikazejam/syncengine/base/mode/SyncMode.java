package com.kamikazejam.syncengine.base.mode;

import com.kamikazejam.kamicommon.configuration.spigot.KamiConfig;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.mode.profile.handshake.swap.NetworkSwapService;
import com.kamikazejam.syncengine.networkprofile.service.NetworkProfileServiceLocal;
import com.kamikazejam.syncengine.networkprofile.service.NetworkProfileServiceRedis;
import com.kamikazejam.syncengine.networkprofile.service.NetworkProfileService;
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
        EngineSource.get().getColorLogger().info("Enabling services for " + this + " mode...");
        // !! Enable redis & server services before others !!
        this.getRedisService();
        this.getServerService();
        this.getSwapService();
        this.getNetworkService();
    }

    private RedisService redisService = null;
    public @Nullable RedisService getRedisService() {
        if (this != SyncMode.NETWORKED) {
            return null;
        }

        if (redisService == null) {
            EngineSource.get().getColorLogger().info("Enabling RedisService...");
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
            EngineSource.get().getColorLogger().info("Enabling ServerService...");
            serverService = new ServerService();
            if (!serverService.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start ServerService, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return serverService;
    }

    private NetworkSwapService swapService = null;
    public @NotNull NetworkSwapService getSwapService() {
        // SwapService was written to ignore calls when not in networked
        // Therefore we can always safely create and return the service
        if (swapService == null) {
            EngineSource.get().getColorLogger().info("Enabling NetworkSwapService...");
            swapService = new NetworkSwapService();
            if (!swapService.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start NetworkSwapService, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return swapService;
    }

    private NetworkProfileService networkService = null;
    public @NotNull NetworkProfileService getNetworkService() {
        if (networkService == null) {
            EngineSource.get().getColorLogger().info("Enabling NetworkStore...");
            if (this == NETWORKED) {
                networkService = new NetworkProfileServiceRedis();
            } else {
                networkService = new NetworkProfileServiceLocal();
            }
        }
        return networkService;
    }

    public void disableServices() {
        if (serverService != null) {
            serverService.shutdown();
            serverService = null;
        }
        if (networkService != null) {
            networkService.shutdown();
            networkService = null;
        }
        // Shutdown redis last (after final messages are sent)
        if (redisService != null) {
            redisService.shutdown();
            redisService = null;
        }
    }
}
