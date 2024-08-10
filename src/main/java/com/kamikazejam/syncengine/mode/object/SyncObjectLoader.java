package com.kamikazejam.syncengine.mode.object;

import com.google.common.base.Preconditions;
import com.kamikazejam.syncengine.base.cache.SyncLoader;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
            Optional<X> local = cache.getLocalStore().get(identifier);
            if (local.isPresent()) {
                // Ensure our Sync is valid (not recently deleted)
                X sync = local.get();
                if (sync.isValid()) {
                    this.sync = new WeakReference<>(sync);
                    loadedFromLocal = true;
                    return;
                }

                // Nullify the reference if the Sync is invalid
                // Don't quit, we could in theory still pull from the database
                cache.getLocalStore().remove(identifier);
                this.sync = new WeakReference<>(null);
            }
        }

        Optional<X> db = cache.getFromDatabase(identifier, true);
        db.ifPresent(x -> {
            sync = new WeakReference<>(x);
            loadedFromLocal = false;
        });
    }

    @Override
    public Optional<X> fetch(boolean saveToLocalCache) {
        load(true);

        if (sync == null) {
            return Optional.empty();
        }

        // Double check validity here too
        @Nullable X p = sync.get();
        if (p != null && !p.isValid()) {
            sync = new WeakReference<>(null);
            return Optional.empty();
        }

        // Save to local cache if necessary
        if (saveToLocalCache && p != null && !loadedFromLocal) {
            this.cache.cache(p);
        }

        // Ensure the Sync has its cache set
        if (p != null) {
            p.setCache(cache);
        }
        return Optional.ofNullable(p);
    }

    @Override
    public void uncache(@NotNull Player player, @NotNull X sync, boolean switchingServers) {
        if (cache.isCached(sync.getId())) {
            cache.uncache(sync);
        }
    }
}
