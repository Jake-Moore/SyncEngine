package com.kamikazejam.syncengine.mode.profile.loader;

import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;

import java.util.Optional;

public class StandaloneProfileLoader {
    protected static <X extends SyncProfile> Optional<X> loadStandalone(SyncProfileLoader<X> L) {
        // Try loading from local
        Optional<X> localSync = L.cache.getLocalStore().get(L.uuid);
        if (localSync.isPresent()) {
            return localSync;
        }

        // Try loading from database
        Optional<X> o = L.cache.getDatabaseStore().get(L.uuid);
        if (o.isEmpty()) {
            // Make a new profile if they are logging in
            if (L.login) {
                L.cache.getLoggerService().debug("Creating a new SyncProfile for: " + L.username);
                L.sync = L.cache.getInstantiator().instantiate();
                if (L.username != null) {
                    L.sync.setUsername(L.username);
                }
                L.sync.setId(L.uuid);
                L.sync.setLoadingSource("New Profile");
                L.sync.setCache(L.cache);
                L.cache.save(L.sync);
                return Optional.of(L.sync);
            }

            // Assume some other kind of failure:
            L.denyJoin = true;
            L.joinDenyReason = StringUtil.t(EngineSource.getConfig().getString("profiles.messages.beforeDbConnection")
                    .replace("{cacheName}", L.cache.getName()));
            L.sync = null;
            return Optional.empty();
        }

        // We have a valid sync from Database
        L.sync = o.get();
        L.sync.setCache(L.cache);

        // For logins -> mark the NetworkProfile as loaded
        if (L.login) {
            NetworkProfile networkProfile = L.cache.getNetworkStore().getOrCreate(L.sync);
            networkProfile.markLoaded(L.login);
            L.cache.runAsync(() -> L.cache.getNetworkStore().save(networkProfile));

            // Update their username
            if (L.username != null) {
                L.sync.setUsername(L.username);
            }
        }

        return Optional.of(L.sync);
    }
}
