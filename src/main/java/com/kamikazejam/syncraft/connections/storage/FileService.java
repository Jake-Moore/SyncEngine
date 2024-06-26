package com.kamikazejam.syncraft.connections.storage;

import com.kamikazejam.syncraft.SyncraftPlugin;
import com.kamikazejam.syncraft.base.Service;
import com.kamikazejam.syncraft.base.error.LoggerService;
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
        return SyncraftPlugin.get().isDebug();
    }
    @Override
    public Plugin getPlugin() {
        return SyncraftPlugin.get();
    }
    @Override
    public String getLoggerName() {
        return "FileStorage";
    }
}
