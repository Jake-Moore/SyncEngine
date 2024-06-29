package com.kamikazejam.syncengine.mode.profile.handshake;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.data.Pair;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
@Getter
public class ProfileHandshakePacket {

    private static final String KEY_LOGIN = "login";
    private static final String KEY_MS_START_HANDSHAKE = "msStartHandshake";
    private static final String KEY_SENDER_SERVER = "senderServer";
    private static final String KEY_TARGET_SERVER = "targetServer";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_HANDSHAKE_ID = "handshakeId";
    private static final String KEY_SYNC_JSON = "syncJson";
    private static final String KEY_SYNC_VER = "syncVer";

    private final boolean login;
    private final long msStartHandshake;
    @Setter private String senderServer;
    private final @NotNull UUID uuid;
    @Setter private String targetServer;
    private final UUID handshakeId; // The id of this handshake, for completing it upon reply
    // <json, version>
    @Setter @Nullable private Pair<String, Long> data;

    public ProfileHandshakePacket(boolean login, long msStart, @NotNull String senderServer, @NotNull UUID uuid, @NotNull String targetServer, @NotNull UUID handshakeId, @Nullable Pair<String, Long> data) {
        this.login = login;
        this.msStartHandshake = msStart;
        this.senderServer = senderServer;
        this.uuid = uuid;
        this.targetServer = targetServer;
        this.handshakeId = handshakeId;
        this.data = data;
    }

    @Nullable
    public static ProfileHandshakePacket fromJSON(@NotNull String json) {
        Preconditions.checkNotNull(json, "JSON cannot be null for ProfileHandshakePacket");
        Document document = Document.parse(json);
        Boolean login = document.getBoolean(KEY_LOGIN);
        long msStartHandshake = document.getLong(KEY_MS_START_HANDSHAKE);
        String uuidString = document.getString(KEY_UUID);
        String targetServer = document.getString(KEY_TARGET_SERVER);
        String senderServer = document.getString(KEY_SENDER_SERVER);
        String handshakeIdString = document.getString(KEY_HANDSHAKE_ID);
        String syncJson = (document.containsKey(KEY_SYNC_JSON)) ? document.getString(KEY_SYNC_JSON) : null;
        long syncVer = (document.containsKey(KEY_SYNC_VER)) ? Long.parseLong(document.getString(KEY_SYNC_VER)) : -1;

        if (uuidString != null && targetServer != null && senderServer != null && handshakeIdString != null && login != null) {
            @Nullable Pair<String, Long> data = (syncJson == null || syncVer > -1) ? Pair.of(syncJson, syncVer) : null;

            return new ProfileHandshakePacket(login, msStartHandshake, senderServer, UUID.fromString(uuidString), targetServer, UUID.fromString(handshakeIdString), data);
        }
        return null;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append(KEY_LOGIN, login);
        document.append(KEY_MS_START_HANDSHAKE, msStartHandshake);
        document.append(KEY_TARGET_SERVER, targetServer);
        document.append(KEY_UUID, uuid.toString());
        document.append(KEY_SENDER_SERVER, senderServer);
        document.append(KEY_HANDSHAKE_ID, handshakeId.toString());
        if (data != null) {
            document.append(KEY_SYNC_JSON, data.getA());
            document.append(KEY_SYNC_VER, data.getB().toString());
        }
        return document;
    }

    public @NotNull String getVersion(String def) {
        if (data == null) {
            return def;
        }
        return data.getB().toString();
    }
}
