package com.kamikazejam.syncengine.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeFileHandler {
    private static final Map<String, ReentrantReadWriteLock> locks = new HashMap<>();

    public static @Nullable String readFile(@NotNull Path path) throws IOException {
        ReentrantReadWriteLock.ReadLock lock = getLock(path.toString()).readLock();
        lock.lock();
        try {
            if (!Files.exists(path)) { return null; }
            return Files.readString(path, StandardCharsets.UTF_8);
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void writeFile(@NotNull Path path, @NotNull String content) throws IOException {
        ReentrantReadWriteLock.WriteLock lock = getLock(path.toString()).writeLock();
        lock.lock();
        try {
            // Atomically create the destination file if required
            if (!path.toFile().exists()) {
                path.toFile().getParentFile().mkdirs();
                path.toFile().createNewFile();
            }

            // Write to a temp file, so that if the write fails or is interrupted, the original file is not corrupted.
            Path tempFile = Files.createTempFile(path.getParent(), path.getFileName().toString(), ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);

            // Atomically move the temp file to the target file (overwriting)
            //  - this method requires the file to exist with a valid directory path
            Files.move(tempFile, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            lock.unlock();
        }
    }

    private static ReentrantReadWriteLock getLock(String filePath) {
        return locks.computeIfAbsent(filePath, k -> new ReentrantReadWriteLock());
    }
}
