package com.kamikazejam.syncengine.base.mode;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.storage.FileStorage;
import com.kamikazejam.syncengine.connections.storage.MongoStorage;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public enum StorageMode {
    FILE,
    MONGODB;

    public void enableServices() {
        // Enable Storage Service
        if (this == FILE) {
            getFileService();
        } else if (this == MONGODB) {
            getMongoService();
        }
    }

    public @NotNull StorageService getStorageService() {
        return (this == FILE) ? getFileService() : getMongoService();
    }

    private @Nullable String relativePath = null;

    public @NotNull File getFileStorageFolder() {
        if (relativePath == null) {
            relativePath = EngineSource.get().getConfig().getString("connections.FILE.directoryName", "data");
        }
        File file = new File(EngineSource.get().getDataFolder(), relativePath);
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Failed to create data directory: " + file.getAbsolutePath());
        }
        if (!file.isDirectory()) {
            file = new File(EngineSource.get().getDataFolder(), "data");
            boolean ignored = file.mkdirs();
        }
        return file;
    }


    // ---------------------------------------------------------------------------- //
    //                         STORAGE SERVICE MANAGEMENT                           //
    // ---------------------------------------------------------------------------- //

    private MongoStorage mongoStorage = null;

    private @NotNull MongoStorage getMongoService() {
        Preconditions.checkState(this == StorageMode.MONGODB, "MongoService is only available in MONGODB storage mode");
        if (mongoStorage == null) {
            mongoStorage = new MongoStorage();
            mongoStorage.start();
        }
        return mongoStorage;
    }

    private FileStorage fileStorage = null;

    private @NotNull FileStorage getFileService() {
        Preconditions.checkState(this == StorageMode.FILE, "FileService is only available in FILE storage mode");
        if (fileStorage == null) {
            fileStorage = new FileStorage();
            fileStorage.start();
        }
        return fileStorage;
    }

    public void disableServices() {
        if (mongoStorage != null) {
            mongoStorage.shutdown();
            mongoStorage = null;
        }
        if (fileStorage != null) {
            fileStorage.shutdown();
            fileStorage = null;
        }
    }
}
