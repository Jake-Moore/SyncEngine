package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.base.Cache;
import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class VersionMismatchException extends SyncException {

    private final long localVer;
    private final long databaseVer;
    public VersionMismatchException(Cache<?, ?> cache, long localVer, long databaseVer) {
        super("Version Mismatch: localVer=" + localVer + " databaseVer=" + databaseVer, cache);
        this.localVer = localVer;
        this.databaseVer = databaseVer;
    }

}
