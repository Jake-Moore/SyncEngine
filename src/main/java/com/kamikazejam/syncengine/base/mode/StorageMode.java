package com.kamikazejam.syncengine.base.mode;

import com.kamikazejam.syncengine.SyncEnginePlugin;

public enum StorageMode {
    FILE,
    MONGODB;

    public void enableServices() {
        // Enable Storage Service
        if (this == FILE) {
            SyncEnginePlugin.get().getFileService();
        }else if (this == MONGODB) {
            SyncEnginePlugin.get().getMongoService();
        }
    }
}
