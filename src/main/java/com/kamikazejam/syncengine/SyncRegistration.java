package com.kamikazejam.syncengine;

import com.google.common.base.Preconditions;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

// TODO this should be the API entry point for each plugin
//  devs should be able to obtain this object from SyncEngineAPI
//  and use it to register caches and other services they need
@Getter
public class SyncRegistration {
    private final @NotNull JavaPlugin plugin;
    /**
     * The full database name as it would appear in MongoDB (or a folder name),
     * This includes the payload group prefix, described in {@link SyncEngineAPI#getFullDatabaseName(String)} (String)}
     * All plugin caches will be stored in this database as collections
     */
    private final String databaseName;

    // package-private because SyncEngine is the only one allowed to create this
    SyncRegistration(@NotNull JavaPlugin plugin, @NotNull String databaseName) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(databaseName);
        this.plugin = plugin;
        this.databaseName = databaseName;
    }
}
