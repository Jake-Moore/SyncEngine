package com.kamikazejam.syncengine.base.update;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

@Getter @SuppressWarnings("unused")
public class UpdatePacket {
    private String sourceGroup;
    private String sourceServer;
    private String identifier;
    private UpdateTask task;
    @Setter
    private boolean forSyncUpdater;

    public UpdatePacket() {}
    public UpdatePacket(@NotNull String sourceGroup, @NotNull String sourceServer, @NotNull String identifier, @NotNull UpdateTask task, boolean forSyncUpdater) {
        this.sourceGroup = sourceGroup;
        this.sourceServer = sourceServer;
        this.identifier = identifier;
        this.task = task;
        this.forSyncUpdater = forSyncUpdater;
    }
}
