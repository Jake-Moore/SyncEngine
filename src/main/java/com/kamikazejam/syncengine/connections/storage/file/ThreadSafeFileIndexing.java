package com.kamikazejam.syncengine.connections.storage.file;

import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.SyncCache;
import com.kamikazejam.syncengine.base.index.IndexedField;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadSafeFileIndexing {
    public interface MappingsConsumer {
        void consume(@NotNull Map<String, Map<String, String>> mappings);
    }
    public interface IndexesConsumer {
        void consume(@NotNull List<IndexedField<?, ?>> indexes);
    }
    public interface IndexesMappingsConsumer {
        void consume(@NotNull Map<String, Map<String, String>> mappings, @NotNull List<IndexedField<?, ?>> indexes);
    }

    //  Map<CacheName,   List<IndexedField>  >
    private static final Map<String, List<IndexedField<?, ?>>> cacheIndexes = new HashMap<>();
    private static final ReentrantLock cacheIndexLock = new ReentrantLock();

    //  Map<CacheName,   Map<FieldName,   Map<FieldValue, SyncKey>   >   >
    private static final Map<String, Map<String, Map<String, String>>> indexMappings = new HashMap<>();
    private static final ReentrantLock mappingsLock = new ReentrantLock();

    public static <K, X extends Sync<K>> void consumeIndexes(@NotNull SyncCache<K, X> cache, @NotNull IndexesConsumer consumer) {
        cacheIndexLock.lock();
        try {
            List<IndexedField<?, ?>> indexes = cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>());
            consumer.consume(indexes);
        } finally {
            cacheIndexLock.unlock();
        }
    }

    public static <K, X extends Sync<K>> void consumeMappings(@NotNull SyncCache<K, X> cache, @NotNull MappingsConsumer consumer) {
        mappingsLock.lock();
        try {
            Map<String, Map<String, String>> mappings = indexMappings.computeIfAbsent(cache.getName(), k -> new HashMap<>());
            consumer.consume(mappings);
        } finally {
            mappingsLock.unlock();
        }
    }

    public static <K, X extends Sync<K>> void consumeBoth(@NotNull SyncCache<K, X> cache, @NotNull IndexesMappingsConsumer consumer) {
        cacheIndexLock.lock();
        mappingsLock.lock();
        try {
            List<IndexedField<?, ?>> indexes = cacheIndexes.computeIfAbsent(cache.getName(), k -> new ArrayList<>());
            Map<String, Map<String, String>> mappings = indexMappings.computeIfAbsent(cache.getName(), k -> new HashMap<>());
            consumer.consume(mappings, indexes);
        }finally {
            // Reverse order
            mappingsLock.unlock();
            cacheIndexLock.unlock();
        }
    }
}
