package com.kamikazejam.syncraft;

import org.jetbrains.annotations.NotNull;

public class SyncraftAPI {
    /**
     * Adds the sync group and a '_' char to the beginning of the dbName,
     * to allow multiple sync group networks to operate on the same MongoDB server
     */
    public static @NotNull String getFullDatabaseName(String dbName) {
        return SyncraftPlugin.get().getSyncGroup() + "_" + dbName;
    }
}
