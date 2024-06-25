package com.kamikazejam.syncraft.base.mode;

import com.kamikazejam.syncraft.SyncraftPlugin;

public enum StorageMode {
    FILE,
    MONGODB;

    public void enableServices() {
        // Enable Storage Service
        if (this == FILE) {
            SyncraftPlugin.get().getFileService();
        }else if (this == MONGODB) {
            SyncraftPlugin.get().getMongoService();
        }
    }
}
