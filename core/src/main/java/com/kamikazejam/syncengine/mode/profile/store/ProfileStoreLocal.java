package com.kamikazejam.syncengine.mode.profile.store;

import com.kamikazejam.syncengine.base.store.StoreLocal;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProfileStoreLocal<X extends SyncProfile> extends StoreLocal<UUID, X> {
    public ProfileStoreLocal() {}

    @Override
    public @NotNull String getLayerName() {
        return "Profile Local";
    }
}
