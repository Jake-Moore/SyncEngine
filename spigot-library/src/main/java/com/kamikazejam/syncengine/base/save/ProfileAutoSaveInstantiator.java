package com.kamikazejam.syncengine.base.save;

import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.SyncProfileCache;

public interface ProfileAutoSaveInstantiator<X extends SyncProfile> {
    ProfileAutoSaveTask<X> run(SyncProfileCache<X> cache);
}
