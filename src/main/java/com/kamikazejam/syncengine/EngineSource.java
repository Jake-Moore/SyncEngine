package com.kamikazejam.syncengine;

import com.kamikazejam.kamicommon.KamiPlugin;
import com.kamikazejam.kamicommon.SpigotUtilProvider;
import com.kamikazejam.kamicommon.configuration.spigot.KamiConfig;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import com.kamikazejam.kamicommon.yaml.standalone.YamlUtil;
import com.kamikazejam.syncengine.base.mode.StorageMode;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.command.SyncEngineCommand;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import com.kamikazejam.syncengine.mode.profile.listener.ProfileListener;
import com.kamikazejam.syncengine.mode.profile.handshake.swap.NetworkSwapService;
import com.kamikazejam.syncengine.networkprofile.service.NetworkProfileService;
import com.kamikazejam.syncengine.server.ServerService;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.UUID;

@SuppressWarnings("unused")
public class EngineSource {
    private static @Nullable KamiPlugin pluginSource;
    private static boolean enabled = false;
    @Getter private static long onEnableTime = 0;
    private static SyncEngineCommand command;

    // Modes
    @Getter private static SyncMode syncMode;
    @Getter private static StorageMode storageMode;
    // Server Identification
    @Getter private static String syncServerId;
    @Getter private static String syncServerGroup;

    /**
     * @return true IFF a plugin source was NEEDED and used for registration
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onEnable(@NotNull KamiPlugin plugin) {
        if (enabled) { return false; }
        pluginSource = plugin;
        enabled = true;

        // Provide KamiCommon utils with this plugin as well
        SpigotUtilProvider.setPlugin(plugin);
        YamlUtil.getYaml();
        IdUtilLocal.setup(plugin);

        // ----------------------------- //
        //      SyncEngine onEnable      //
        // ----------------------------- //
        // Load Plugin Modes
        syncMode = SyncMode.valueOf(getConfig().getString("mode"));
        storageMode = syncMode.getStorageMode(getConfig());
        info("Running in " + syncMode + " mode with " + storageMode + " storage.");

        // Load Sync ID
        KamiConfig syncConf = new KamiConfig(plugin, new File(plugin.getDataFolder(), "syncengine-server.yml"), true);
        syncServerId = syncConf.getString("sync-server-id", null);
        syncServerGroup = syncConf.getString("sync-server-group", "global");
        if (syncServerId == null || syncServerId.equalsIgnoreCase("null")) {
            syncServerId = UUID.randomUUID().toString();
            syncConf.set("sync-server-id", syncServerId);
            syncConf.save();
        }

        // Enable Services
        syncMode.enableServices();
        storageMode.enableServices();
        getSwapService();

        // Load Commands
        command = new SyncEngineCommand();
        command.registerCommand(plugin);

        // Register ProfileListener
        plugin.getServer().getPluginManager().registerEvents(new ProfileListener(), plugin);

        onEnableTime = System.currentTimeMillis();
        return true;
    }

    /**
     * @return true IFF this call triggered the singleton disable sequence, false it already disabled
     */
    @SuppressWarnings("UnusedReturnValue")
    public static boolean onDisable() {
        if (!enabled) { return false; }

        // Unload Commands
        if (command != null) {
            command.unregisterCommand();
        }

        // Shutdown Services
        syncMode.disableServices();
        storageMode.disableServices();

        // Set to disabled
        boolean prev = enabled;
        enabled = false;
        return prev;
    }

    public static @NotNull KamiPlugin get() {
        if (pluginSource == null) {
            throw new RuntimeException("Plugin source not set");
        }
        return pluginSource;
    }

    public static void info(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[INFO] " + msg);
        }else {
            pluginSource.getLogger().info(msg);
        }
    }
    public static void warning(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[WARNING] " + msg);
        }else {
            pluginSource.getLogger().warning(msg);
        }
    }
    public static void error(@NotNull String msg) {
        if (pluginSource == null) {
            System.out.println("[ERROR] " + msg);
        }else {
            pluginSource.getLogger().severe(msg);
        }
    }

    // KamiConfig access of syncengine.yml
    private static KamiConfig kamiConfig = null;
    public static @NotNull KamiConfig getConfig() {
        final JavaPlugin plugin = get();
        if (kamiConfig == null) {
            kamiConfig = new KamiConfig(plugin, new File(plugin.getDataFolder(), "syncengine.yml"), true, true);
        }
        return kamiConfig;
    }

    /**
     * @return If the plugin has debug logging enabled
     */
    public static boolean isDebug() {
        return getConfig().getBoolean("debug", false);
    }

    // --------------------------------------------------------------------------------------- //
    //                                  Service Accessors                                      //
    // --------------------------------------------------------------------------------------- //

    public static @Nullable RedisService getRedisService() {
        return syncMode.getRedisService();
    }

    public static @Nullable ServerService getServerService() {
        return syncMode.getServerService();
    }

    public static @NotNull NetworkSwapService getSwapService() {
        return syncMode.getSwapService();
    }

    public static @NotNull NetworkProfileService getNetworkService() {
        return syncMode.getNetworkService();
    }

    public static @NotNull StorageService getStorageService() {
        return storageMode.getStorageService();
    }
}
