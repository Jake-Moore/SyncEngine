package com.kamikazejam.syncraft.base.sync;

import com.kamikazejam.syncraft.base.Sync;
import org.jetbrains.annotations.NotNull;

public interface SyncInstantiator<K, X extends Sync<K>> {

    @NotNull
    X instantiate();

}
