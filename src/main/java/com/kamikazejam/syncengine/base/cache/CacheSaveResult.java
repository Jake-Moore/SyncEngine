package com.kamikazejam.syncengine.base.cache;

import lombok.Getter;

@Getter
public class CacheSaveResult {
    /**
     * How many Sync saves were attempted.
     */
    private final int total;
    /**
     * How many Sync saves failed.
     */
    private final int failed;

    public CacheSaveResult(int total, int failed) {
        this.total = total;
        this.failed = failed;
    }
}
