package com.kamikazejam.syncengine.mode.profile.handshake;

import com.kamikazejam.kamicommon.util.data.Pair;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@Getter
@SuppressWarnings({"DuplicatedCode", "unused"})
public class ProfileHandshakePacket {

    @Setter
    private boolean request; // If this is a request or a reply
    private boolean login;
    private long msStartHandshake;
    @Setter
    private String senderServer;
    private UUID uuid;
    @Setter
    private String targetServer;
    private UUID handshakeId; // The id of this handshake, for completing it upon reply
    // <json, version>
    @Setter @Nullable
    private Pair<String, Long> data;

    public ProfileHandshakePacket() {}
    public ProfileHandshakePacket(boolean request, boolean login, long msStart, @NotNull String senderServer, @NotNull UUID uuid, @NotNull String targetServer, @NotNull UUID handshakeId, @Nullable Pair<String, Long> data) {
        this.request = request;
        this.login = login;
        this.msStartHandshake = msStart;
        this.senderServer = senderServer;
        this.uuid = uuid;
        this.targetServer = targetServer;
        this.handshakeId = handshakeId;
        this.data = data;
    }

    public @NotNull String getVersion(String def) {
        if (data == null) {
            return def;
        }
        return data.getB().toString();
    }
}
