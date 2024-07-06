package com.kamikazejam.syncengine.mode.profile.store;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.exception.VersionMismatchException;
import com.kamikazejam.syncengine.base.store.StoreDatabase;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.util.VersionMismatchHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProfileStoreDatabase<X extends SyncProfile> extends StoreDatabase<UUID, X> {

    public ProfileStoreDatabase(Cache<UUID, X> cache) {
        super(cache);
    }

    @Override
    public @NotNull X callVersionMismatch(Cache<UUID, X> cache, @NotNull X sync, VersionMismatchException ex) {
        return VersionMismatchHandler.handleProfileException(this::get, cache, sync, ex);
    }

    @Override
    public @NotNull String getLayerName() {
        return "Profile MongoDB";
    }
}
