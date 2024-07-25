package com.kamikazejam.syncengine.connections.storage.file;

import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.index.IndexedField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeFileIndexing {
    public interface MappingsModifier {
        @NotNull
        Map<String, Map<String, String>> modify(Map<String, Map<String, String>> mappings);
    }

    //  Map<CacheName,   List<IndexedField>  >
    private static final Map<String, List<IndexedField<?, ?>>> cacheIndexes = new HashMap<>();
    private static final ReentrantReadWriteLock cacheIndexLock = new ReentrantReadWriteLock();

    //  Map<CacheName,   Map<FieldName,   Map<FieldValue, SyncKey>   >   >
    private static final Map<String, Map<String, Map<String, String>>> indexMappings = new HashMap<>();
    private static final ReentrantReadWriteLock mappingsLock = new ReentrantReadWriteLock();


    public static <K, X extends Sync<K>, T> void addCacheIndex(@NotNull SyncCache<K, X> cache, IndexedField<X, T> index) {
        ReentrantReadWriteLock.WriteLock lock = cacheIndexLock.writeLock();
        lock.lock();
        try {
            cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>()).add(index);
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    public static <K, X extends Sync<K>> List<IndexedField<?, ?>> getCacheIndexes(@NotNull SyncCache<K, X> cache) {
        ReentrantReadWriteLock.ReadLock lock = cacheIndexLock.readLock();
        lock.lock();
        try {
            return cacheIndexes.getOrDefault(cache.getName(), new ArrayList<>());
        } finally {
            lock.unlock();
        }
    }

    @NotNull
    public static <K, X extends Sync<K>> Map<String, Map<String, String>> getIndexMappings(@NotNull SyncCache<K, X> cache) {
        ReentrantReadWriteLock.ReadLock lock = mappingsLock.readLock();
        lock.lock();
        try {
            return indexMappings.getOrDefault(cache.getName(), new HashMap<>());
        } finally {
            lock.unlock();
        }
    }

    public static <K, X extends Sync<K>> void modifyIndexMappings(@NotNull SyncCache<K, X> cache, MappingsModifier modifier) {
        ReentrantReadWriteLock.WriteLock lock = mappingsLock.writeLock();
        lock.lock();
        try {
            Map<String, Map<String, String>> modified = modifier.modify(indexMappings.getOrDefault(cache.getName(), new HashMap<>()));
            indexMappings.put(cache.getName(), modified);
        } finally {
            lock.unlock();
        }
    }

}
