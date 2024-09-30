package com.kamikazejam.syncengine.mode.profile.loader;

import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.networkprofile.NetworkProfile;

import java.util.Optional;

public class StandaloneProfileLoader {

    protected static <X extends SyncProfile> void loadStandalone(SyncProfileLoader<X> loader) {
        // Try loading from local
        Optional<X> localSync = loader.cache.getLocalStore().get(loader.uuid);
        if (localSync.isPresent()) {
            loader.sync = localSync.get();
            return;
        }

        // Try loading from database
        Optional<X> o = loader.cache.getDatabaseStore().get(loader.uuid);
        if (o.isEmpty()) {
            // Make a new profile if they are logging in
            if (loader.login) {
                loader.cache.getLoggerService().debug("Creating a new SyncProfile for: " + loader.username);
                loader.sync = loader.cache.getInstantiator().instantiate();
                if (loader.username != null) {
                    loader.sync.setUsername(loader.username);
                }
                loader.sync.setId(loader.uuid);
                loader.sync.setLoadingSource("New Profile");
                loader.sync.setCache(loader.cache);
                loader.cache.save(loader.sync);
                return;
            }

            // Assume some other kind of failure:
            loader.denyJoin = true;
            loader.joinDenyReason = StringUtil.t(EngineSource.getConfig().getString("profiles.messages.beforeDbConnection")
                    .replace("{cacheName}", loader.cache.getName()));
            loader.sync = null;
            return;
        }

        // We have a valid sync from Database
        loader.sync = o.get();
        loader.sync.setCache(loader.cache);

        // For logins -> mark the NetworkProfile as loaded
        if (loader.login) {
            NetworkProfile networkProfile = loader.cache.getNetworkStore().getOrCreate(loader.sync);
            networkProfile.markLoaded(loader.login);
            loader.cache.runAsync(() -> loader.cache.getNetworkStore().save(networkProfile));

            // Update their username
            if (loader.username != null) {
                loader.sync.setUsername(loader.username);
            }
        }
    }
}
