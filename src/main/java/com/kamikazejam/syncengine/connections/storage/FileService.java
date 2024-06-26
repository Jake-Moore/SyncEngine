package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.syncengine.SyncEnginePlugin;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import dev.morphia.mapping.codec.pojo.MorphiaCodec;
import dev.morphia.mapping.codec.reader.DocumentReader;
import org.apache.commons.io.FileUtils;
import org.bson.BsonDocumentWrapper;
import org.bson.Document;
import org.bson.codecs.DecoderContext;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("unnused")
public class FileService extends StorageService {
    private boolean running = false;

    public FileService() {
    }


    // ------------------------------------------------- //
    //                 StorageService                    //
    // ------------------------------------------------- //
    @Override
    public <K, X extends Sync<K>> boolean save(Cache<K, X> cache, X sync) {
        File targetFile = getTargetFile(cache, sync.getId());

        // Write the Object json to the file
        try {
            String json = toJson(cache, sync);
            FileUtils.write(targetFile, json, StandardCharsets.UTF_8);
            return true;
        } catch (Throwable t) {
            cache.getLoggerService().severe(t, "Failed to write file: " + targetFile.getAbsolutePath());
            return false;
        }
    }

    @Override
    public @NotNull <K, X extends Sync<K>> Optional<X> get(Cache<K, X> cache, K key) {
        File targetFile = getTargetFile(cache, key);
        if (!targetFile.exists()) {
            return Optional.empty();
        }
        try {
            String json = FileUtils.readFileToString(targetFile, StandardCharsets.UTF_8);
            return Optional.ofNullable(fromJson(cache, cache.getSyncClass(), json));
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
    public <K, X extends Sync<K>> Collection<X> getAll(Cache<K, X> cache) {
        return List.of();
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
    //                   ErrorService                    //
    // ------------------------------------------------- //
    @Override
    public boolean isDebug() {
        return SyncEnginePlugin.get().isDebug();
    }

    @Override
    public Plugin getPlugin() {
        return SyncEnginePlugin.get();
    }

    @Override
    public String getLoggerName() {
        return "FileStorage";
    }

    public static <K, X extends Sync<K>> String toJson(Cache<K, X> cache, X sync) {
        // After a ton of digging, this appears to be the MongoDB operation that is used to save a document
        return BsonDocumentWrapper.asBsonDocument(sync, BsonDocumentWrapper.DEFAULT_CODEC_REGISTRY).toJson();
    }

    public static <K, X extends Sync<K>> @Nullable X fromJson(Cache<K, X> cache, Class<X> clazz, @Nullable String json) {
        if (json == null) {
            return null;
        }

        // Now we have to find a method to convert the json to the Profile again
        MorphiaCodec<X> codec = (MorphiaCodec<X>) BsonDocumentWrapper.DEFAULT_CODEC_REGISTRY.get(clazz);
        return codec.decode(new DocumentReader(Document.parse(json)), DecoderContext.builder().build());
    }


    // ------------------------------------------------- //
    //                 Helper Methods                    //
    // ------------------------------------------------- //
    private <K, X extends Sync<K>> File getCacheFolder(Cache<K, X> cache) {
        File storageFolder = SyncEnginePlugin.get().getStorageMode().getFileStorageFolder();
        return new File(storageFolder + File.separator + cache.getDatabaseName() + File.separator + cache.getName());
    }

    private <K, X extends Sync<K>> File getTargetFile(Cache<K, X> cache, K key) {
        return new File(getCacheFolder(cache), cache.keyToString(key) + ".json");
    }
}
