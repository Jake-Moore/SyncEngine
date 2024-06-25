package com.kamikazejam.syncraft;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.KamiPlugin;
import com.kamikazejam.kamicommon.util.Txt;
import com.kamikazejam.syncraft.base.mode.StorageMode;
import com.kamikazejam.syncraft.base.mode.SyncMode;
import com.kamikazejam.syncraft.connections.redis.RedisService;
import com.kamikazejam.syncraft.connections.storage.FileService;
import com.kamikazejam.syncraft.connections.storage.MongoService;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Getter
@SuppressWarnings("unused")
public class SyncraftPlugin extends KamiPlugin {
    // INSTANCE
    private static SyncraftPlugin instance = null;
    public static SyncraftPlugin get() { return instance; }

    private SyncMode syncMode;
    private StorageMode storageMode;

    @Override
    public void onEnableInner() {
        instance = this;
        this.copyResources();

        syncMode = SyncMode.valueOf(getKamiConfig().getString("mode"));
        storageMode = syncMode.getStorageMode(getKamiConfig());
        getLogger().info("Running in " + Txt.getNicedEnum(syncMode) + " mode with " + Txt.getNicedEnum(storageMode) + " storage.");

        syncMode.enableServices();
        storageMode.enableServices();
    }

    @Override
    public void onDisableInner() {

    }

    private void copyResources() {
        // Config Handled by KamiPlugin
        // Need to handle syncid.yml
        if (!(new File(getDataFolder(), "syncid.yml")).exists()) {
            this.saveResource("syncid.yml", false);
            getLogger().info("Generated default syncid.yml");
        }
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    public boolean isDebug() {
        return getKamiConfig().getBoolean("debug");
    }



    private RedisService redisService = null;
    public @NotNull RedisService getRedisService() {
        Preconditions.checkState(syncMode == SyncMode.NETWORKED, "RedisService is only available in NETWORKED mode");
        if (redisService == null) {
            redisService = new RedisService();
            redisService.start();
        }
        return redisService;
    }
    private MongoService mongoService = null;
    public @NotNull MongoService getMongoService() {
        Preconditions.checkState(storageMode == StorageMode.MONGODB, "MongoService is only available in MONGODB storage mode");
        if (mongoService == null) {
            mongoService = new MongoService();
            mongoService.start();
        }
        return mongoService;
    }
    private FileService fileService = null;
    public @NotNull FileService getFileService() {
        Preconditions.checkState(storageMode == StorageMode.FILE, "FileService is only available in FILE storage mode");
        if (fileService == null) {
            fileService = new FileService();
            fileService.start();
        }
        return fileService;
    }
}
