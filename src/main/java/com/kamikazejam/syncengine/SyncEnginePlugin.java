package com.kamikazejam.syncengine;

import com.kamikazejam.kamicommon.KamiPlugin;
import com.kamikazejam.kamicommon.SpigotUtilProvider;
import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.kamicommon.util.Txt;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import com.kamikazejam.kamicommon.yaml.standalone.YamlUtil;
import com.kamikazejam.syncengine.base.mode.StorageMode;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.command.SyncEngineCommand;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.server.ServerService;
import lombok.Getter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

@Getter
@SuppressWarnings("unused")
public class SyncEnginePlugin extends KamiPlugin {
    // INSTANCE
    private static SyncEnginePlugin instance = null;

    @ApiStatus.Internal
    public static SyncEnginePlugin get() {
        return instance;
    }

    private SyncEngineCommand command;
    private SyncMode syncMode;
    private StorageMode storageMode;
    private String syncId;
    private String syncGroup;
    private StorageService storageService;

    @Override
    public void onEnableInner() {
        instance = this;

        // Load KamiCommon and some other time-consuming tasks
        SpigotUtilProvider.setPlugin(this);
        YamlUtil.getYaml();
        IdUtilLocal.setup(this);

        // Load Plugin Modes
        syncMode = SyncMode.valueOf(getKamiConfig().getString("mode"));
        storageMode = syncMode.getStorageMode(getKamiConfig());
        getLogger().info("Running in " + Txt.getNicedEnum(syncMode) + " mode with " + Txt.getNicedEnum(storageMode) + " storage.");
        storageService = storageMode.getStorageService();

        // Load Sync ID
        KamiConfig syncConf = new KamiConfig(this, new File(getDataFolder(), "syncid.yml"), true);
        syncId = syncConf.getString("sync-id", null);
        syncGroup = syncConf.getString("sync-group", "global");
        if (syncId == null || syncId.equalsIgnoreCase("null")) {
            syncId = UUID.randomUUID().toString();
            syncConf.set("sync-id", syncId);
            syncConf.save();
        }

        // Load Commands
        command = new SyncEngineCommand();
        command.registerCommand(this);

        syncMode.enableServices();
        storageMode.enableServices();
    }

    @Override
    public void onDisableInner() {
        // Unload Commands
        if (command != null) {
            command.unregisterCommand();
        }

        // Shutdown Services
        syncMode.disableServices();
        storageMode.disableServices();
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    public boolean isDebug() {
        return getKamiConfig().getBoolean("debug");
    }

    public @Nullable RedisService getRedisService() {
        return syncMode.getRedisService();
    }

    public @Nullable ServerService getServerService() {
        return syncMode.getServerService();
    }

    public @NotNull StorageService getStorageService() {
        return storageMode.getStorageService();
    }
}
