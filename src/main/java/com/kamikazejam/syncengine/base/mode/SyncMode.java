package com.kamikazejam.syncengine.base.mode;

import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.syncengine.SyncEnginePlugin;
import org.jetbrains.annotations.NotNull;

public enum SyncMode {
    STANDALONE,
    NETWORKED;

    public @NotNull StorageMode getStorageMode(KamiConfig config) {
        if (this == NETWORKED) {
            return StorageMode.MONGODB;
        }
        return StorageMode.valueOf(config.getString("standalone.storage"));
    }

    public void enableServices() {
        // Enable REDIS
        //   MongoDB handled by StorageMode
        if (this != NETWORKED) { return; }
        SyncEnginePlugin.get().getRedisService();
    }
}
