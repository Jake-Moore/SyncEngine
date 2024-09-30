package com.kamikazejam.syncengine.base.update;

public enum UpdateTask {
    PULL_FROM_STORE,            // Pull from database (if we have this cached, will not pull if not cached)
    DELETE_AND_INVALIDATE,      // Mark any local cached data as invalid + remove from cache
}
