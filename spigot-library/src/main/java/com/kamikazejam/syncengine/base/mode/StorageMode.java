package com.kamikazejam.syncengine.base.mode;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.storage.FileStorage;
import com.kamikazejam.syncengine.connections.storage.MongoStorage;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public enum StorageMode {
    FILE,
    MONGODB;

    public void enableServices() {
        // Enable Storage Service
        if (this == FILE) {
            getFileStorage();
        } else if (this == MONGODB) {
            getMongoStorage();
        }
    }

    public @NotNull StorageService getStorageService() {
        return (this == FILE) ? getFileStorage() : getMongoStorage();
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

    private @NotNull MongoStorage getMongoStorage() {
        Preconditions.checkState(this == StorageMode.MONGODB, "MongoStorage is only available in MONGODB storage mode");
        if (mongoStorage == null) {
            mongoStorage = new MongoStorage();
            if (!mongoStorage.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start MongoStorage, shutting down..."));
                Bukkit.shutdown();
            }
        }
        return mongoStorage;
    }

    private FileStorage fileStorage = null;

    private @NotNull FileStorage getFileStorage() {
        Preconditions.checkState(this == StorageMode.FILE, "FileStorage is only available in FILE storage mode");
        if (fileStorage == null) {
            fileStorage = new FileStorage();
            if (!fileStorage.start()) {
                EngineSource.get().getLogger().severe(StringUtil.t("&cFailed to start FileStorage, shutting down..."));
                Bukkit.shutdown();
            }
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
