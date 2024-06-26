package com.kamikazejam.syncengine;

import org.jetbrains.annotations.NotNull;

public class SyncEngineAPI {
    /**
     * Adds the sync group and a '_' char to the beginning of the dbName,
     * to allow multiple sync group networks to operate on the same MongoDB server
     */
    public static @NotNull String getFullDatabaseName(String dbName) {
        return SyncEnginePlugin.get().getSyncGroup() + "_" + dbName;
    }
}
