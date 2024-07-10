package com.kamikazejam.syncengine.connections.storage;

import com.kamikazejam.kamicommon.gson.JsonObject;
import com.kamikazejam.kamicommon.gson.JsonParser;
import com.kamikazejam.kamicommon.util.data.TriState;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.index.IndexedField;
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

            // Optimistic Versioning (only fails with a valid, non-equal database version)
            if (dbVer != null && dbVer != sync.getVersion()) {
                throw new VersionMismatchException(cache, sync.getVersion(), dbVer);
            }

            // If we have no changes to the json, don't bother writing (save the IO)
            @NotNull String newJson = JacksonUtil.toJson(sync);
            if (newJson.equals(json)) {
                cache.getLoggerService().debug("No changes to save for: " + sync.getId());
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
            Optional<X> o = Optional.of(JacksonUtil.deserialize(cache.getSyncClass(), json));
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
        // Create an iterator that reads files as requested
        Iterator<X> iterator = new SyncFilesIterable<>(cache, getCacheFolder(cache).toPath()).iterator();
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
    //  Map<CacheName,   List<IndexedField>  >
    protected final Map<String, List<IndexedField<?, ?>>> cacheIndexes = new HashMap<>();
    //  Map<CacheName,   Map<FieldName,   Map<FieldValue, SyncKey>   >   >
    protected final Map<String, Map<String, Map<Object, Object>>> indexMappings = new HashMap<>();

    @Override
    public <K, X extends Sync<K>, T> void registerIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index) {
        List<IndexedField<?,?>> fields = cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>());
        fields.add(index);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, X extends Sync<K>> void cacheIndexes(@NotNull SyncCache<K, X> cache, @NotNull X sync, boolean save) {
        // Step 0 - Ensure maps are populated
        List<IndexedField<?, ?>> cacheIndexes = this.cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>());
        Map<String, Map<Object, Object>> indexMappings = this.indexMappings.computeIfAbsent(cache.getName(), k -> new HashMap<>());

        // Step 1 - Remove any data mapped to this sync (will be put back if still valid)
        for (IndexedField<?, ?> index : cacheIndexes) {
            // Grab the cache for this field name
            Map<Object, Object> indexCache = indexMappings.get(index.getName());
            if (indexCache == null) { continue; }

            // Compile a list of index mappings to remove
            List<Object> toRemove = new ArrayList<>();
            for (Map.Entry<Object, Object> entry : indexCache.entrySet()) {
                // Only scan for mappings pointing at this Sync
                String k1 = cache.keyToString((K) entry.getValue());
                String k2 = cache.keyToString(sync.getId());
                // Objects.equals valid for Strings
                if (!Objects.equals(k1, k2)) { continue; }

                toRemove.add(entry.getKey());
            }
            toRemove.forEach(indexCache::remove);
        }

        // Step 2 - Update the cache with current field data (update field -> Sync mapping)
        for (IndexedField<?, ?> index : cacheIndexes) {
            Map<Object, Object> indexCache = indexMappings.computeIfAbsent(index.getName(), k -> new HashMap<>());

            @Nullable Object value = index.getValue(sync);
            if (value == null) { continue; }

            // Cache this field's value mapped to our Sync
            @Nullable K old = (K) indexCache.put(value, sync.getId());

            // Objects.equals valid for Strings
            if (old != null && !Objects.equals(cache.keyToString(old), cache.keyToString(sync.getId()))) {
                cache.getLoggerService().severe("Duplicate index value for field " + index.getName() + " in cache " + cache.getName() + " Previous Sync Id: " + old + " New Sync Id: " + sync.getId());
            }
        }

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

        // Step 0 - Ensure maps are populated
        List<IndexedField<?, ?>> cacheIndexes = this.cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>());
        Map<String, Map<Object, Object>> indexMappings = this.indexMappings.computeIfAbsent(cache.getName(), k -> new HashMap<>());

        // Loop through all the registered indexes to fetch the mappings
        for (IndexedField<?, ?> indexed : cacheIndexes) {
            // Grab the mappings for this index
            Map<Object, Object> indexCache = indexMappings.get(indexed.getName());
            if (indexCache == null) { continue; }

            JsonObject indexJson = new JsonObject();
            for (Map.Entry<Object, Object> entry : indexCache.entrySet()) {
                indexJson.addProperty(entry.getKey().toString(), entry.getValue().toString());
            }
            fieldJsons.put(indexed.getName(), indexJson);
        }

        return fieldJsons;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, X extends Sync<K>, T> @Nullable K getSyncIdByIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index, T value) {
        Map<Object, Object> indexCache = indexMappings.getOrDefault(cache.getName(), new HashMap<>()).get(index.getName());
        if (indexCache == null) { return null; }

        Object syncId = indexCache.get(value);
        if (syncId == null) { return null; }
        return (K) syncId;
    }
}
