package com.kamikazejam.syncengine.mode.profile.update;

import com.kamikazejam.syncengine.base.update.SyncUpdater;
import com.kamikazejam.syncengine.base.update.UpdatePacket;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.SyncProfileCache;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * This class does nothing unless we have a RedisService (NETWORKED mode)
 */
@SuppressWarnings({"DuplicatedCode", "unused"})
public class ProfileUpdater<X extends SyncProfile> extends SyncUpdater<UUID, X> {

    public ProfileUpdater(SyncProfileCache<X> cache) {
        super(cache, "sync-profile-updater");
    }

    @Override
    public void handleUpdateType(@NotNull UpdatePacket packet) {
        // Nothing to do here
    }
}
