package com.kamikazejam.syncengine.server;

import com.google.common.base.Preconditions;
import lombok.Data;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

@Data
public class SyncServerPacket {

    private static final String KEY_DB_NAME = "db-name";
    private static final String KEY_SYNC_ID = "sync-id";
    private static final String KEY_SYNC_GROUP = "sync-group";

    private final String dbName;
    private final String syncID;
    private final String syncGroup;

    public SyncServerPacket(String dbName, String syncID, String syncGroup) {
        this.dbName = dbName;
        this.syncID = syncID;
        this.syncGroup = syncGroup;
    }

    @Nullable
    public static SyncServerPacket fromJSON(@NotNull String json) {
        Preconditions.checkNotNull(json, "JSON cannot be null for SyncServerPacket");
        Document document = Document.parse(json);
        String dbName = document.getString(KEY_DB_NAME);
        String syncID = document.getString(KEY_SYNC_ID);
        String syncGroup = document.getString(KEY_SYNC_GROUP);

        if (dbName != null && syncID != null && syncGroup != null) {
            return new SyncServerPacket(dbName, syncID, syncGroup);
        }
        return null;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append(KEY_DB_NAME, dbName);
        document.append(KEY_SYNC_ID, syncID);
        document.append(KEY_SYNC_GROUP, syncGroup);
        return document;
    }

    public static SyncServerPacket of(String dbName, String syncID, String syncGroup) {
        return new SyncServerPacket(dbName, syncID, syncGroup);
    }
}
