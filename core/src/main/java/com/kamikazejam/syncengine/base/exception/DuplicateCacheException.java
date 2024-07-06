package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.base.Cache;

@SuppressWarnings("unused")
public class DuplicateCacheException extends SyncException {

    public DuplicateCacheException(Cache<?, ?> cache) {
        super(cache);
    }

    public DuplicateCacheException(String message, Cache<?, ?> cache) {
        super(message, cache);
    }

    public DuplicateCacheException(String message, Throwable cause, Cache<?, ?> cache) {
        super(message, cause, cache);
    }

    public DuplicateCacheException(Throwable cause, Cache<?, ?> cache) {
        super(cause, cache);
    }

    public DuplicateCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Cache<?, ?> cache) {
        super(message, cause, enableSuppression, writableStackTrace, cache);
    }
}
