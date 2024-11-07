package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.kamicommon.gson.JsonElement;
import com.kamikazejam.kamicommon.gson.JsonObject;
import com.kamikazejam.kamicommon.gson.JsonParser;
import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.index.IndexedField;
import com.kamikazejam.syncengine.connections.storage.file.ThreadSafeFileIndexing;
import com.kamikazejam.syncengine.connections.storage.iterable.SyncFilesIterable;
import com.kamikazejam.syncengine.connections.storage.iterable.TransformingIterator;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.ThreadSafeFileHandler;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"unnused", "DuplicatedCode"})
public class FileStorage extends StorageService {
    private boolean running = false;

    public FileStorage() {}

    // ------------------------------------------------- //
    //                 StorageService                    //
    // ------------------------------------------------- //
    @Override
    public <K, X extends Sync<K>> @NotNull TriState save(Cache<K, X> cache, X sync) throws VersionMismatchException {
        File targetFile = getTargetFile(cache, sync.getId());

        // Write the Object json to the file
        try {
            @Nullable String json = readJsonFromFile(targetFile);
            @Nullable Long dbVer = getVersionFromJson(json);

            // Optimistic Versioning (only fails with a valid, newer database version)
            // Ideally this would check equality, but there's some weird stuff with the file system
            //  where this version read can be outdated (lower ver), and we don't want to fail for that
            if (dbVer != null && dbVer > sync.getVersion()) {
                throw new VersionMismatchException(cache, sync.getVersion(), dbVer);
            }

            // If we have no changes to the json, don't bother writing (save the IO)
            @NotNull String newJson = JacksonUtil.toJson(sync);
            if (newJson.equals(json)) {
                // It is 'saved' successfully (i.e. database has the same data)
                return TriState.NOT_SET;
            }

            // Increment the Version and write the file
            long newVersion = sync.getVersion() + 1;
            sync.setVersion(newVersion);
            // We already used Jackson to get the JSON, rather than calling it again
            //  we can just modify the json directly for this static field
            JsonObject syncJson = JsonParser.parseString(newJson).getAsJsonObject();
            syncJson.addProperty("version", newVersion);

            // Use ThreadSafeFileHandler, must obtain the json again since version field was updated
            ThreadSafeFileHandler.writeFile(targetFile.toPath(), syncJson.toString());

            // Cache Indexes
            cache.cacheIndexes(sync, true);
            return TriState.TRUE;
        } catch (VersionMismatchException v) {
            // pass through
            throw v;
        } catch (Throwable t) {
            cache.getLoggerService().severe(t, "Failed to write file: " + targetFile.getAbsolutePath());
            return TriState.FALSE;
        }
    }

    public static @Nullable String readJsonFromFile(@NotNull File targetFile) throws IOException {
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
            @Nullable String json = readJsonFromFile(targetFile);
            if (json == null) { return Optional.empty(); }
            Optional<X> o = Optional.of(JacksonUtil.fromJson(cache.getSyncClass(), json));
            o.ifPresent(sync -> cache.cacheIndexes(sync, true));
            return o;
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
        // Ensure Directory exists
        File cacheFolder = getCacheFolder(cache);
        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            throw new RuntimeException("Failed to create cache folder: " + cacheFolder.getAbsolutePath());
        }

        // Create an iterator that reads files as requested
        Iterator<X> iterator = new SyncFilesIterable<>(cache, cacheFolder.toPath()).iterator();
        // Adapt the iterator to peek the values as they are provided
        return () -> new TransformingIterator<>(iterator, x -> {
            // Ensure indexes are cached
            cache.cacheIndexes(x, false);
            return x;
        });
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

    @Override
    public <K, X extends Sync<K>> void onRegisteredCache(Cache<K, X> cache) {
        File cacheFolder = getCacheFolder(cache);
        if (!cacheFolder.exists() && !cacheFolder.mkdirs()) {
            throw new RuntimeException("Failed to create cache folder: " + cacheFolder.getAbsolutePath());
        }
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
    private <K, X extends Sync<K>> File getDbFolder(Cache<K, X> cache) {
        File storageFolder = EngineSource.getStorageMode().getFileStorageFolder();
        // Can use short db name since we have a local file system, no need for collision avoidance with the group name
        return new File(storageFolder + File.separator + cache.getDbNameShort());
    }
    private <K, X extends Sync<K>> File getCacheFolder(Cache<K, X> cache) {
        return new File(getDbFolder(cache) + File.separator + cache.getName());
    }
    private <K, X extends Sync<K>> File getTargetFile(Cache<K, X> cache, K key) {
        return new File(getCacheFolder(cache), cache.keyToString(key) + ".json");
    }
    private <K, X extends Sync<K>> File getIndexesFolder(Cache<K, X> cache) {
        return new File(getDbFolder(cache), ".indexes");
    }

    // ------------------------------------------------- //
    //                     Indexing                      //
    // ------------------------------------------------- //

    @Override
    public <K, X extends Sync<K>, T> void registerIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index) {
        ThreadSafeFileIndexing.consumeIndexes(cache, indexes -> indexes.add(index));
        this.loadIndexCache(cache, index);
    }

    @Override
    public <K, X extends Sync<K>> void cacheIndexes(@NotNull SyncCache<K, X> cache, @NotNull X sync, boolean save) {
        // Step 1 - Remove any data mapped to this sync (will be put back if still valid)
        this.invalidateIndexes(cache, sync.getId(), false);

        // Step 2 - Modify the Mappings
        ThreadSafeFileIndexing.consumeBoth(cache, (mappings, cacheIndexes) -> {
            // Update each index's mappings
            for (IndexedField<?, ?> index : cacheIndexes) {
                Map<String, String> indexCache = mappings.computeIfAbsent(index.getName(), k -> new HashMap<>());

                @Nullable Object value = index.getValue(sync);
                if (value == null) { continue; }

                // Cache this field's value mapped to our Sync
                @Nullable String old = indexCache.put(index.toString(value), cache.keyToString(sync.getId()));

                // Objects.equals valid for Strings
                if (old != null && !Objects.equals(old, cache.keyToString(sync.getId()))) {
                    cache.getLoggerService().severe("Duplicate index value for field " + index.getName() + " in cache " + cache.getName() + " Previous Sync Id: " + old + " New Sync Id: " + sync.getId());
                }
            }
        });

        // Step 3 - Save the Indexes
        if (save) {
            cache.tryAsync(() -> saveIndexCache(cache));
        }
    }

    @SneakyThrows
    @Override
    public <K, X extends Sync<K>> void saveIndexCache(@NotNull SyncCache<K, X> cache) {
        File indexesFolder = new File(getIndexesFolder(cache) + File.separator + cache.getName());
        Map<String, JsonObject> fieldJsons = serializeJsonCache(cache);

        // Write each index to its own file
        for (Map.Entry<String, JsonObject> entry : fieldJsons.entrySet()) {
            File indexFile = new File(indexesFolder, entry.getKey() + ".json");
            ThreadSafeFileHandler.writeFile(indexFile.toPath(), entry.getValue().toString());
        }
    }

    private <K, X extends Sync<K>> @NotNull Map<String, JsonObject> serializeJsonCache(@NotNull SyncCache<K, X> cache) {
        Map<String, JsonObject> fieldJsons = new HashMap<>();

        // Acquire both the mappings and indexes
        ThreadSafeFileIndexing.consumeBoth(cache, (indexMappings, cacheIndexes) -> {
            // Loop through all the registered indexes to fetch the mappings
            for (IndexedField<?, ?> index : cacheIndexes) {
                // Grab the mappings for this index
                Map<String, String> indexCache = indexMappings.get(index.getName());
                if (indexCache == null) { continue; }

                JsonObject indexJson = new JsonObject();
                for (Map.Entry<String, String> entry : indexCache.entrySet()) {
                    indexJson.addProperty(entry.getKey(), entry.getValue());
                }
                fieldJsons.put(index.getName(), indexJson);
            }
        });

        return fieldJsons;
    }

    @Override
    public <K, X extends Sync<K>, T> @Nullable K getSyncIdByIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index, T value) {
        AtomicReference<String> syncId = new AtomicReference<>();
        ThreadSafeFileIndexing.consumeMappings(cache, (indexMappings) -> {
            @Nullable Map<String, String> indexCache = indexMappings.get(index.getName());
            if (indexCache == null) { return; }

            syncId.set(indexCache.get(index.toString(value)));
        });
        return (syncId.get() == null) ? null : cache.keyFromString(syncId.get());
    }

    @SneakyThrows
    public <K, X extends Sync<K>, T> void loadIndexCache(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index) {
        File indexesFolder = new File(getIndexesFolder(cache) + File.separator + cache.getName());
        File indexFile = new File(indexesFolder, index.getName() + ".json");
        if (!indexFile.exists()) { return; }

        // 1 - Read the index file
        String jsonContent = ThreadSafeFileHandler.readFile(indexFile.toPath());
        if (jsonContent == null || jsonContent.isEmpty()) { return; }

        // 2 - Ensure maps are populated
        ThreadSafeFileIndexing.consumeMappings(cache, (mappings) -> {
            Map<String, String> indexCache = mappings.computeIfAbsent(index.getName(), k -> new HashMap<>());

            // 3 - Parse the JSON & load caches
            JsonObject json = JsonParser.parseString(jsonContent).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.asMap().entrySet()) {
                String value = entry.getValue().getAsString();

                // Ensure the value is a valid Sync ID
                if (!cache.getDatabaseStore().has(cache.keyFromString(value))) {
                    continue;
                }

                indexCache.put(entry.getKey(), value);
            }
        });
    }

    @Override
    public <K, X extends Sync<K>> void invalidateIndexes(@NotNull SyncCache<K, X> cache, @NotNull K syncId, boolean updateFile) {
        // Step 1 - Modify the Mappings
        ThreadSafeFileIndexing.consumeBoth(cache, (mappings, cacheIndexes) -> {
            // Remove any data mapped to this sync
            for (IndexedField<?, ?> index : cacheIndexes) {
                // Grab the cache for this field name
                @Nullable Map<String, String> indexCache = mappings.get(index.getName());
                if (indexCache == null) { continue; }

                // Compile a list of index mappings to remove
                List<String> toRemove = new ArrayList<>();
                for (Map.Entry<String, String> entry : indexCache.entrySet()) {
                    // Only scan for mappings pointing at this Sync
                    String k1 = entry.getValue();
                    String k2 = cache.keyToString(syncId);
                    // Objects.equals valid for Strings
                    if (!Objects.equals(k1, k2)) { continue; }

                    toRemove.add(entry.getKey());
                }
                toRemove.forEach(indexCache::remove);
            }
        });

        // Step 2 - Save the Indexes
        if (updateFile) {
            cache.tryAsync(() -> saveIndexCache(cache));
        }
    }

    @Override
    public long getPingNano() {
        return -1;
    }
}
