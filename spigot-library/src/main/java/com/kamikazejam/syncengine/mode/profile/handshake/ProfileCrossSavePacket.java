package com.kamikazejam.syncengine.mode.profile.handshake;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ProfileCrossSavePacket {

    private static final String KEY_UUID = "uuid";
    private static final String KEY_SAVED_FROM = "savedFromServer";

    private final UUID uuid;
    private String savedFromServer;   // The server that saved the profile, and is publishing the cross save packet

    @Nullable
    public static ProfileCrossSavePacket fromJSON(@NotNull String json) {
        Preconditions.checkNotNull(json, "JSON cannot be null for ProfileHandshakePacket");
        Document document = Document.parse(json);
        String uuidString = document.getString(KEY_UUID);
        String savedFromServer = document.getString(KEY_SAVED_FROM);

        if (uuidString != null && savedFromServer != null) {
            return new ProfileCrossSavePacket(UUID.fromString(uuidString), savedFromServer);
        }
        return null;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append(KEY_UUID, uuid.toString());
        document.append(KEY_SAVED_FROM, savedFromServer);
        return document;
    }

}
