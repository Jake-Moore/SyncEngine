package com.kamikazejam.syncengine.mode.object.store;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.store.StoreDatabase;
import com.kamikazejam.syncengine.mode.object.SyncObject;
import com.kamikazejam.syncengine.util.VersionMismatchHandler;
import org.jetbrains.annotations.NotNull;

public class ObjectStoreDatabase<X extends SyncObject> extends StoreDatabase<String, X> {

    public ObjectStoreDatabase(Cache<String, X> cache) {
        super(cache);
    }

    @Override
    public @NotNull X callVersionMismatch(Cache<String, X> cache, @NotNull X sync, VersionMismatchException ex) {
        return VersionMismatchHandler.handleObjectException(this::get, cache, sync, ex);
    }

    @Override
    public @NotNull String getLayerName() {
        return "Object MongoDB";
    }
}
