package com.kamikazejam.syncengine.mode.profile.network.handshake;

import com.google.common.base.Preconditions;
import com.kamikazejam.kamicommon.util.data.TriState;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("DuplicatedCode")
@Getter @Accessors(chain = true)
public class NetworkSwapPacket {
    private static final String KEY_NANO = "nano";
    private static final String KEY_SENDER_SERVER = "senderServer";
    private static final String KEY_TARGET_SERVER = "targetServer";
    private static final String KEY_UUID = "uuid";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_HANDSHAKE_ID = "handshakeId";
    private static final String KEY_TRI_STATE = "triState";

    private final long nanoStartHandshake;
    @Setter private String senderServer;
    @Setter private String targetServer;
    private @NotNull final UUID uuid;
    private @NotNull final String username;
    @Setter private TriState found = TriState.NOT_SET;

    private final UUID handshakeId; // The id of this handshake, for completing it upon reply

    public NetworkSwapPacket(long nanoStartHandshake, String senderServer, String targetServer, @NotNull UUID uuid, @NotNull String username, UUID handshakeId) {
        this.nanoStartHandshake = nanoStartHandshake;
        this.senderServer = senderServer;
        this.targetServer = targetServer;
        this.uuid = uuid;
        this.username = username;
        this.handshakeId = handshakeId;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append(KEY_NANO, nanoStartHandshake);
        document.append(KEY_SENDER_SERVER, senderServer);
        document.append(KEY_TARGET_SERVER, targetServer);
        document.append(KEY_UUID, uuid.toString());
        document.append(KEY_USERNAME, username);
        document.append(KEY_HANDSHAKE_ID, handshakeId.toString());
        document.append(KEY_TRI_STATE, found.name());
        return document;
    }

    @Nullable
    public static NetworkSwapPacket fromJSON(@NotNull String json) {
        Preconditions.checkNotNull(json, "JSON cannot be null for ProfileHandshakePacket");
        Document document = Document.parse(json);
        long nanoStartHandshake = document.getLong(KEY_NANO);
        String uuidString = document.getString(KEY_UUID);
        String username = document.getString(KEY_USERNAME);
        String targetServer = document.getString(KEY_TARGET_SERVER);
        String senderServer = document.getString(KEY_SENDER_SERVER);
        String handshakeIdString = document.getString(KEY_HANDSHAKE_ID);
        TriState found = TriState.valueOf(document.getString(KEY_TRI_STATE));

        if (uuidString != null && username != null && targetServer != null && senderServer != null && handshakeIdString != null) {
            UUID uuid = UUID.fromString(uuidString);
            UUID handshakeId = UUID.fromString(handshakeIdString);
            NetworkSwapPacket handshake = new NetworkSwapPacket(nanoStartHandshake, senderServer, targetServer, uuid, username, handshakeId);
            return handshake.setFound(found);
        }
        return null;
    }





}
