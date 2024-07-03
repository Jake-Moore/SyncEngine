package com.kamikazejam.syncengine.mode.object.update;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.update.SyncUpdater;
import com.kamikazejam.syncengine.base.update.UpdatePacket;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import org.jetbrains.annotations.NotNull;

/**
 * This class does nothing unless we have a RedisService (NETWORKED mode)
 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class ObjectUpdater<X extends SyncObject> extends SyncUpdater<String, X> {

    public ObjectUpdater(Cache<String, X> cache) {
        super(cache, "sync-object-updater");
    }

    @Override
    public void handleUpdateType(@NotNull UpdatePacket packet) {
        // ObjectUpdater does not send any other types, nothing to do here
    }

}
