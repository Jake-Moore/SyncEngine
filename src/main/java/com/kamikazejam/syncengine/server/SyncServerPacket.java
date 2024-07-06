package com.kamikazejam.syncengine.server;

import lombok.Data;

@Data @SuppressWarnings("unused")
public class SyncServerPacket {

    private String dbName;
    private String syncID;
    private String syncGroup;

    public SyncServerPacket() {}
    public SyncServerPacket(String dbName, String syncID, String syncGroup) {
        this.dbName = dbName;
        this.syncID = syncID;
        this.syncGroup = syncGroup;
    }

    public static SyncServerPacket of(String dbName, String syncID, String syncGroup) {
        return new SyncServerPacket(dbName, syncID, syncGroup);
    }
}
