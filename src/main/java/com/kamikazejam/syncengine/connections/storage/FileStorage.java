package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.kamicommon.gson.JsonObject;
import com.kamikazejam.kamicommon.gson.JsonParser;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.connections.storage.iterable.SyncFilesIterable;
import com.kamikazejam.syncengine.connections.storage.iterable.TransformingIterator;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.ThreadSafeFileHandler;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unnused")
public class FileStorage extends StorageService {
    private boolean running = false;

    public FileStorage() {}

    // ------------------------------------------------- //
    //                 StorageService                    //
    // ------------------------------------------------- //
    @Override
    public <K, X extends Sync<K>> boolean save(Cache<K, X> cache, X sync) throws VersionMismatchException {
        File targetFile = getTargetFile(cache, sync.getId());

        // Write the Object json to the file
        try {
            @Nullable String json = readJsonFromFile(targetFile);
            @Nullable Long dbVer = getVersionFromJson(json);

            // Optimistic Versioning (only fails with a valid, non-equal database version)
            if (dbVer != null && dbVer != sync.getVersion()) {
                throw new VersionMismatchException(cache, sync.getVersion(), dbVer);
            }

            // Increment the Version and write the file
            sync.setVersion(sync.getVersion() + 1);
            // Use ThreadSafeFileHandler
            ThreadSafeFileHandler.writeFile(targetFile.toPath(), JacksonUtil.toJson(sync));
            return true;
        } catch (VersionMismatchException v) {
            // pass through
            throw v;
        } catch (Throwable t) {
            cache.getLoggerService().severe(t, "Failed to write file: " + targetFile.getAbsolutePath());
            return false;
        }
    }

    private @Nullable String readJsonFromFile(@NotNull File targetFile) throws IOException {
        if (!targetFile.exists()) { return null; }
        @Nullable String json = ThreadSafeFileHandler.readFile(targetFile.toPath());
        return (json == null || json.isEmpty()) ? null : json;
    }

    private @Nullable Long getVersionFromJson(@Nullable String json) {
        if (json == null) { return null; }
        try {
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            if (!jsonObject.has("version")) { return null; }
            return jsonObject.get("version").getAsLong();
        }catch (Throwable t) {
            return null;
        }
    }

    @Override
    public @NotNull <K, X extends Sync<K>> Optional<X> get(Cache<K, X> cache, K key) {
        File targetFile = getTargetFile(cache, key);
        if (!targetFile.exists()) {
            return Optional.empty();
        }
        try {
            @Nullable String json = ThreadSafeFileHandler.readFile(targetFile.toPath());
            if (json == null || json.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(JacksonUtil.deserialize(cache.getSyncClass(), json));
        } catch (Throwable t) {
            cache.getLoggerService().severe(t, "Failed to read file: " + targetFile.getAbsolutePath());
            return Optional.empty();
        }
    }

    @Override
    public <K, X extends Sync<K>> long size(Cache<K, X> cache) {
        File cacheFolder = getCacheFolder(cache);
        if (!cacheFolder.exists()) {
            return 0;
        }
        @Nullable File[] files = cacheFolder.listFiles();
        return (files == null) ? 0 : files.length;
    }

    @Override
    public <K, X extends Sync<K>> boolean has(Cache<K, X> cache, K key) {
        return getTargetFile(cache, key).exists();
    }

    @Override
    public <K, X extends Sync<K>> boolean remove(Cache<K, X> cache, K key) {
        File targetFile = getTargetFile(cache, key);
        if (!targetFile.exists()) {
            // Did not exist previously, return false
            return false;
        }
        try {
            FileUtils.forceDelete(targetFile);
            return true;
        } catch (Throwable t) {
            cache.getLoggerService().severe(t, "Failed to remove file: " + targetFile.getAbsolutePath());
            return false;
        }
    }

    @Override
    public <K, X extends Sync<K>> Iterable<X> getAll(Cache<K, X> cache) {
        return new SyncFilesIterable<>(cache, getCacheFolder(cache).toPath());
    }

    @Override
    public <K, X extends Sync<K>> Iterable<K> getKeys(Cache<K, X> cache) {
        @Nullable File[] array = getCacheFolder(cache).listFiles();
        if (array == null) { return Set.of(); }
        // Convert to a non-null list
        List<File> files = new ArrayList<>();
        for (File file : array) {
            if (file == null) { continue; }
            files.add(file);
        }

        // Map each file to its key in an iterator
        return () -> new TransformingIterator<>(files.iterator(), f ->
                cache.keyFromString(f.getName().substring(0, f.getName().lastIndexOf('.')))
        );
    }

    @Override
    public boolean canCache() {
        return true;
    }

    // ------------------------------------------------- //
    //                 Service Methods                   //
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

    @Override
    public boolean isRunning() {
        return running;
    }

    // ------------------------------------------------- //
    //                   LoggerService                   //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return EngineSource.isDebug();
    }

    @Override
    public Plugin getPlugin() {
        return EngineSource.get();
    }

    @Override
    public String getLoggerName() {
        return "FileStorage";
    }

    // ------------------------------------------------- //
    //                 Helper Methods                    //
    // ------------------------------------------------- //
    private <K, X extends Sync<K>> File getCacheFolder(Cache<K, X> cache) {
        File storageFolder = EngineSource.getStorageMode().getFileStorageFolder();
        // Can use short db name since we have a local file system, no need for collision avoidance with the group name
        return new File(storageFolder + File.separator + cache.getDbNameShort() + File.separator + cache.getName());
    }

    private <K, X extends Sync<K>> File getTargetFile(Cache<K, X> cache, K key) {
        return new File(getCacheFolder(cache), cache.keyToString(key) + ".json");
    }
}
