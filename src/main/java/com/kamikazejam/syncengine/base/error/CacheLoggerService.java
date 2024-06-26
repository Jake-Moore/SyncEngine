package com.kamikazejam.syncengine.base.error;

import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.base.Cache;
import org.bukkit.plugin.Plugin;

public class CacheLoggerService extends LoggerService {

    protected final Cache<?, ?> cache;

    public CacheLoggerService(Cache<?, ?> cache) {
        this.cache = cache;
    }

    @Override
    public boolean isDebug() {
        return SyncEngineAPI.isDebug();
    }

    @Override
    public String getLoggerName() {
        return "C: " + cache.getName();
    }

    @Override
    public Plugin getPlugin() {
        return cache.getPlugin();
    }
}
