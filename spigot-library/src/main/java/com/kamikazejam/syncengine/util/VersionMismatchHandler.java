package com.kamikazejam.syncengine.util;

import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class VersionMismatchHandler {

    public static <X extends Sync<String>> boolean handObjectException(Save<String, X> saver, Get<String, X> getter, Cache<String, X> cache, @NotNull X sync, VersionMismatchException ex) {
        // Fetch the new Sync and then try to save it again
        @NotNull X updatedSync = resolve(getter, cache, sync, ex);
        return saver.save(cache, updatedSync);
    }

    public static <X extends Sync<UUID>> boolean handProfileException(Save<UUID, X> saver, Get<UUID, X> getter, Cache<UUID, X> cache, @NotNull X sync, VersionMismatchException ex) {
        // Fetch the new Sync and then try to save it again
        @NotNull X updatedSync = resolve(getter, cache, sync, ex);
        return saver.save(cache, updatedSync);
    }

    /**
     * @return Iff we need to retry save
     */
    private static <K, X extends Sync<K>> @NotNull X resolve(Get<K, X> getter, Cache<K, X> cache, @NotNull X sync, VersionMismatchException ex) {
        // We can fetch the last known CachedCopy of this Sync, and use it to compare fields
        Sync<K> cachedCopy = sync.getCachedCopy();
        // Also fetch the current database version
        X database = getter.get(cache, sync.getId()).orElseThrow(() -> new IllegalStateException("Sync not found in database in VersionMismatchException!?"));
        long newVer = database.getVersion();
        boolean changed = (newVer != sync.getVersion());

        // Use reflection to grab all fields and compare them
        //  The extra cost of reflection is accepted as this is a repair operation (should be async)
        info(" ", "VersionMismatchException (local:" + ex.getLocalVer() + " db:" + ex.getDatabaseVer() + ") - Detecting Field Changes:");

        for (Field field : ReflectionUtil.getAllFields(cache.getSyncClass())) {
            // Skip the version field
            if (field.getName().equals("version")) { continue; }

            try {
                field.setAccessible(true);
                Object cachedValue = field.get(cachedCopy);
                Object currentValue = field.get(sync);

                // Use Jackson since .equals might not work for all types
                if (!Objects.equals(JacksonUtil.toJson(cachedValue), JacksonUtil.toJson(currentValue))) {
                    // Update the database object with the current value
                    field.set(database, currentValue);
                    info("\tUpdated field " + field.getName() + " from " + cachedValue + " to " + currentValue);
                    changed = true;
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        // Make sure the version matches db, so save will work
        database.setVersion(newVer);
        if (!changed) {
            throw new IllegalStateException("VersionMismatchException but no changes detected!?");
        }
        info("Field Comparison Completed, re-trying save...", " ");
        return database;
    }



    // Utility method to log to SyncEngine logger if debug is enabled
    private static void info(String... s) {
        if (!EngineSource.isDebug()) { return; }
        Logger logger =  EngineSource.get().getLogger();
        Arrays.stream(s).forEach(m -> logger.info(StringUtil.t("&c" + m)));
    }

    // Generic Interfaces so we can call the SyncStore methods for getting and saving
    public interface Save<K, X extends Sync<K>> {
        boolean save(Cache<K, X> cache, @NotNull X sync);
    }
    public interface Get<K, X extends Sync<K>> {
        @NotNull Optional<X> get(Cache<K, X> cache, @NotNull K key);
    }
}
