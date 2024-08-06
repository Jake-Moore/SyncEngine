package com.kamikazejam.syncengine.connections.storage.iterable;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.util.JacksonUtil;
import com.kamikazejam.syncengine.util.ThreadSafeFileHandler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Objects;

public class SyncFilesIterable<K, X extends Sync<K>> implements Iterable<X> {
    private final Cache<K, X> cache;
    private final Path folderPath;
    public SyncFilesIterable(Cache<K, X> cache, Path folderPath) {
        this.cache = cache;
        this.folderPath = folderPath;
    }

    @Override
    public @NotNull Iterator<X> iterator() {
        try {
            return new Iterator<>() {
                private final Iterator<Path> pathIterator = Files.newDirectoryStream(folderPath).iterator();

                @Override
                public boolean hasNext() {
                    return pathIterator.hasNext();
                }

                @Override
                public X next() {
                    Path filePath = pathIterator.next();
                    try {
                        String json = Objects.requireNonNull(ThreadSafeFileHandler.readFile(filePath));
                        return JacksonUtil.fromJson(cache.getSyncClass(), json);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file: " + filePath, e);
                    }
                }
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory stream", e);
        }
    }
}
