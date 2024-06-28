package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.base.Cache;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@SuppressWarnings("unused")
public class VersionMismatchException extends SyncException {

    private final long localVer;
    private final long databaseVer;
    private final @NotNull String databaseJson;
    public VersionMismatchException(Cache<?, ?> cache, long localVer, long databaseVer, @NotNull String databaseJson) {
        super("Version Mismatch: localVer=" + localVer + " databaseVer=" + databaseVer, cache);
        this.localVer = localVer;
        this.databaseVer = databaseVer;
        this.databaseJson = databaseJson;
    }

}
