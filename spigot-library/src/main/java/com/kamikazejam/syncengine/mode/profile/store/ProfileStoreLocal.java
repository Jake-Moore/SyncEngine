package com.kamikazejam.syncengine.mode.profile.store;

import com.kamikazejam.kamicommon.util.PlayerUtil;
import com.kamikazejam.syncengine.base.store.StoreLocal;
import com.kamikazejam.syncengine.mode.profile.SyncProfile;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class ProfileStoreLocal<X extends SyncProfile> extends StoreLocal<UUID, X> {
    public ProfileStoreLocal() {}

    @Override
    public @NotNull String getLayerName() {
        return "Profile Local";
    }

    @Override
    public boolean save(@NotNull X sync) {
        // Don't locally cache Profile objects for Players not on this server
        UUID uuid = sync.getUniqueId();
        if (!PlayerUtil.isFullyValidPlayer(Bukkit.getPlayer(uuid))) {
            return false;
        }
        // If they are online, allow the save to cache
        return super.save(sync);
    }
}
