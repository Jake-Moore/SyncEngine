package com.kamikazejam.syncengine.connections.redis;

import com.kamikazejam.kamicommon.redis.RedisAPI;
import com.kamikazejam.kamicommon.redis.RedisConnector;
import com.kamikazejam.kamicommon.util.LoggerService;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.connections.config.RedisConfig;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("DuplicatedCode")
@Getter
public class RedisService extends LoggerService implements Service {
    private boolean running = false;
    private RedisAPI api;

    public RedisService() {
    }

    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        // Create our RedisAPI instance
        this.api = RedisConnector.getAPI(RedisConfig.get(), this);
        this.running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        // If not running, warn and return true (we are already shutdown)
        if (!running) {
            this.warn("RedisService.shutdown() called while service is not running!");
            return true;
        }
        // Shutdown the RedisAPI instance
        if (this.api != null) {
            this.api.shutdown();
        }
        this.running = false;
        return true;
    }



    // ------------------------------------------------- //
    //                 LoggerService (KC)                //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
    }

    @Override
    public String getLoggerName() {
        return "RedisService";
    }

    public JavaPlugin getPlugin() {
        return EngineSource.get();
    }

    @Override
    public void logToConsole(String a, Level level) {
        // Add the logger name to the start of the msg
        String content = "[" + getLoggerName() + "] " + a;
        // Add the plugin name to the VERY start, so it matches existing logging format
        String plPrefix = "[" + getPlugin().getName() + "] ";
        if (level == Level.INFO) {
            Bukkit.getConsoleSender().sendMessage(StringUtil.t(plPrefix + content));
        } else if (level == Level.FINE) {
            Bukkit.getConsoleSender().sendMessage(StringUtil.t("&7[DEBUG] " + plPrefix + content));
        } else {
            Logger.getLogger("Minecraft").log(level, StringUtil.t(plPrefix + content));
        }
    }
}
