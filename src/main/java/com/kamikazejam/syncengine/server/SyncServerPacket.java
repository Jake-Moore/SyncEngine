package com.kamikazejam.syncengine.server;

import lombok.Data;

@Data @SuppressWarnings("unused")
public class SyncServerPacket {

    private String syncID;
    private String syncGroup;

    public SyncServerPacket() {}
    public SyncServerPacket(String syncID, String syncGroup) {
        this.syncID = syncID;
        this.syncGroup = syncGroup;
    }

    public static SyncServerPacket of(String syncID, String syncGroup) {
        return new SyncServerPacket(syncID, syncGroup);
    }
}
