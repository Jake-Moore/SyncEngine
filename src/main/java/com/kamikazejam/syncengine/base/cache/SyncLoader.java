package com.kamikazejam.syncengine.base.cache;

import com.kamikazejam.syncengine.base.Sync;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * This class is responsible for loading a {@link Sync} when requested from a Cache.
 */
@SuppressWarnings("rawtypes")
public interface SyncLoader<X extends Sync> {

    Optional<X> fetch(boolean saveToLocalCache);

    void uncache(@NotNull Player player, @NotNull X sync, boolean switchingServers);

}
