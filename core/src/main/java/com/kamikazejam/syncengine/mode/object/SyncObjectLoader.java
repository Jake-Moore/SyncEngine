package com.kamikazejam.syncengine.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.cache.SyncLoader;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Optional;

@Getter
@Setter
public class SyncObjectLoader<X extends SyncObject> implements SyncLoader<X> {
    private final SyncObjectCache<X> cache;
    private final String identifier;

    private WeakReference<X> sync = null;
    private boolean loadedFromLocal = false;

    SyncObjectLoader(@NotNull SyncObjectCache<X> cache, String identifier) {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(identifier);
        this.cache = cache;
        this.identifier = identifier;
    }

    @SuppressWarnings("SameParameterValue")
    private void load(boolean fromLocal) {
        if (fromLocal) {
            Optional<X> local = cache.getFromCache(identifier);
            if (local.isPresent()) {
                sync = new WeakReference<>(local.get());
                loadedFromLocal = true;
                return;
            }
        }
        Optional<X> db = cache.getFromDatabase(identifier, true);
        db.ifPresent(x -> sync = new WeakReference<>(x));
    }

    @Override
    public Optional<X> fetch(boolean saveToLocalCache) {
        load(true);

        if (sync != null) {
            X p = sync.get();
            if (saveToLocalCache && p != null && !loadedFromLocal) {
                this.cache.cache(p);
            }
            // Ensure the Sync has its cache set
            if (p != null) {
                p.setCache(cache);
            }
            return Optional.ofNullable(p);
        }
        return Optional.empty();
    }

    @Override
    public void uncache(@NotNull Player player, @NotNull X sync, boolean switchingServers) {
        if (cache.isCached(sync.getId())) {
            cache.uncache(sync);
        }
    }
}
