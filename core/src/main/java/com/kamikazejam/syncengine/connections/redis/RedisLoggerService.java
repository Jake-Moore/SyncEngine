package com.kamikazejam.syncengine.connections.redis;

import com.kamikazejam.kamicommon.util.LoggerService;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("DuplicatedCode")
public class RedisLoggerService extends LoggerService {

    @Override
    public String getLoggerName() {
        return "RedisService";
    }

    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
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
