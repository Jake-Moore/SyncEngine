package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Service;
import com.kamikazejam.syncengine.base.error.LoggerService;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

@Getter
public class FileService extends LoggerService implements Service {
    private boolean running = false;

    public FileService() {}


    // ------------------------------------------------- //
    //                StorageService                     //
    // ------------------------------------------------- //
    @Override
    public boolean start() {
        this.running = true;
        return true;
    }

    @Override
    public boolean shutdown() {
        this.running = false;
        return true;
    }



    // ------------------------------------------------- //
    //                   ErrorService                    //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return SyncEnginePlugin.get().isDebug();
    }
    @Override
    public Plugin getPlugin() {
        return SyncEnginePlugin.get();
    }
    @Override
    public String getLoggerName() {
        return "FileStorage";
    }
}
