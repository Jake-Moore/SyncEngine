package com.kamikazejam.syncraft;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.KamiPlugin;
import com.kamikazejam.kamicommon.configuration.config.KamiConfig;
import com.kamikazejam.kamicommon.gson.JsonObject;
import com.kamikazejam.kamicommon.gson.JsonParser;
import com.kamikazejam.kamicommon.util.Txt;
import com.kamikazejam.syncraft.base.mode.StorageMode;
import com.kamikazejam.syncraft.base.mode.SyncMode;
import com.kamikazejam.syncraft.command.SyncraftCommand;
import com.kamikazejam.syncraft.connections.redis.RedisService;
import com.kamikazejam.syncraft.connections.storage.FileService;
import com.kamikazejam.syncraft.connections.storage.MongoService;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Getter
@SuppressWarnings("unused")
public class SyncraftPlugin extends KamiPlugin {
    // INSTANCE
    private static SyncraftPlugin instance = null;
    public static SyncraftPlugin get() { return instance; }

    private SyncraftCommand command;
    private SyncMode syncMode;
    private StorageMode storageMode;
    private String syncId;
    private String syncGroup;

    @Override
    public void onEnableInner() {
        instance = this;

        // Verify Dependencies
        if (!verifyDependencies()) {
            getLogger().severe("Failed to verify dependencies. (see above)");
            getPluginLoader().disablePlugin(this);
            return;
        }

        // Load Plugin Modes
        syncMode = SyncMode.valueOf(getKamiConfig().getString("mode"));
        storageMode = syncMode.getStorageMode(getKamiConfig());
        getLogger().info("Running in " + Txt.getNicedEnum(syncMode) + " mode with " + Txt.getNicedEnum(storageMode) + " storage.");

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
        command = new SyncraftCommand();
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

        // Shutdown services
        if (redisService != null && redisService.isRunning()) {
            redisService.shutdown();
        }
        if (mongoService != null && mongoService.isRunning()) {
            mongoService.shutdown();
        }
        if (fileService != null && fileService.isRunning()) {
            fileService.shutdown();
        }
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    public boolean isDebug() {
        return getKamiConfig().getBoolean("debug");
    }

    public boolean verifyDependencies() {
        // Load the properties.json file
        InputStream properties = getResource("properties.json");
        if (properties == null) {
            getLogger().severe("Could not find properties.json file");
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        // Load the data from properties.json
        JsonObject o = (JsonObject) JsonParser.parseReader(new InputStreamReader(properties, StandardCharsets.UTF_8));

        // Verify KamiCommon Version
        if (!verifyPluginVersion(o, "kamicommon.version", "KamiCommon", this::onVerFailure)) {
            Bukkit.getPluginManager().disablePlugin(this);
            return false;
        }
        return true;
    }
    private void onVerFailure(String pluginName, String minVer) {
        getLogger().severe(pluginName + " version is too old! (" + minVer + " or higher required)");
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
