package com.kamikazejam.syncengine.base.mode;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.connections.storage.FileService;
import com.kamikazejam.syncengine.connections.storage.MongoService;
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
            relativePath = SyncEnginePlugin.get().getKamiConfig().getString("connections.FILE.directoryName", "data");
        }
        File file = new File(SyncEnginePlugin.get().getDataFolder(), relativePath);
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Failed to create data directory: " + file.getAbsolutePath());
        }
        if (!file.isDirectory()) {
            file = new File(SyncEnginePlugin.get().getDataFolder(), "data");
            boolean ignored = file.mkdirs();
        }
        return file;
    }


    // ---------------------------------------------------------------------------- //
    //                         STORAGE SERVICE MANAGEMENT                           //
    // ---------------------------------------------------------------------------- //

    private MongoService mongoService = null;

    private @NotNull MongoService getMongoService() {
        Preconditions.checkState(this == StorageMode.MONGODB, "MongoService is only available in MONGODB storage mode");
        if (mongoService == null) {
            mongoService = new MongoService();
            mongoService.start();
        }
        return mongoService;
    }

    private FileService fileService = null;

    private @NotNull FileService getFileService() {
        Preconditions.checkState(this == StorageMode.FILE, "FileService is only available in FILE storage mode");
        if (fileService == null) {
            fileService = new FileService();
            fileService.start();
        }
        return fileService;
    }

    public void disableServices() {
        if (mongoService != null) {
            mongoService.shutdown();
            mongoService = null;
        }
        if (fileService != null) {
            fileService.shutdown();
            fileService = null;
        }
    }
}
