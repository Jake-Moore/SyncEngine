package com.kamikazejam.syncengine.base.index;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter @SuppressWarnings("unused")
public abstract class IndexedField<X extends Sync<?>, T> {
    private final @NotNull Cache<?, X> cache;
    private final @NotNull String name;
    public IndexedField(@NotNull Cache<?, X> cache, @NotNull String name) {
        this.cache = cache;
        this.name = name;
    }

    public abstract boolean equals(@Nullable T a, @Nullable T b);

    public @Nullable X getSync(@NotNull T value) {
        return cache.getByIndex(this, value);
    }

    public abstract <K, Y extends Sync<K>> T getValue(Y sync);
}
