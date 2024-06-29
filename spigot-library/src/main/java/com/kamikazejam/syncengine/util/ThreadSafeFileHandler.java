package com.kamikazejam.syncengine.util;

import org.jetbrains.annotations.NotNull;

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

    public static @NotNull String readFile(@NotNull Path path) throws IOException {
        ReentrantReadWriteLock.ReadLock lock = getLock(path.toString()).readLock();
        lock.lock();
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } finally {
            lock.unlock();
        }
    }

    public static void writeFile(@NotNull Path path, @NotNull String content) throws IOException {
        ReentrantReadWriteLock.WriteLock lock = getLock(path.toString()).writeLock();
        lock.lock();
        try {
            // Write to a temp file, so that if the write fails or is interrupted, the original file is not corrupted.
            Path tempFile = Files.createTempFile("temp-", ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);

            // Atomically move the temp file to the target file.
            Files.move(tempFile, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            lock.unlock();
        }
    }

    private static ReentrantReadWriteLock getLock(String filePath) {
        return locks.computeIfAbsent(filePath, k -> new ReentrantReadWriteLock());
    }
}
