package com.kamikazejam.syncengine.base.sync;

import com.kamikazejam.syncengine.base.Sync;
import org.jetbrains.annotations.NotNull;

public interface SyncInstantiator<K, X extends Sync<K>> {

    @NotNull
    X instantiate();

}
