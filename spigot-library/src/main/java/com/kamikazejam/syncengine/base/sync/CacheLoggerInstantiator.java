package com.kamikazejam.syncengine.base.sync;

import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.error.LoggerService;
import org.jetbrains.annotations.NotNull;

public interface CacheLoggerInstantiator {

    @NotNull
    LoggerService instantiate(Cache<?,?> cache);

}
