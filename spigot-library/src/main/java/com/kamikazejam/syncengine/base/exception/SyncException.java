package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.base.Cache;

@SuppressWarnings("unused")
public class SyncException extends Exception {

    public SyncException(Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception");
    }

    public SyncException(String message, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message);
    }

    public SyncException(String message, Throwable cause, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message, cause);
    }

    public SyncException(Throwable cause, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: ", cause);
    }

    public SyncException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, Cache<?, ?> cache) {
        super("C: [" + cache.getName() + "] exception: " + message, cause, enableSuppression, writableStackTrace);
    }
}
