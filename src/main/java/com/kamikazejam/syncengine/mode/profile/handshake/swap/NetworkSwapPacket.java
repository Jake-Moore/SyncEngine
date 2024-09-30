package com.kamikazejam.syncengine.mode.profile.handshake.swap;

import com.kamikazejam.kamicommon.util.data.TriState;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@SuppressWarnings({"DuplicatedCode", "unused"})
@Getter @Accessors(chain = true)
public class NetworkSwapPacket {

    @Setter
    private boolean request;
    private long nanoStartHandshake;
    @Setter
    private String senderServer;
    @Setter
    private String targetServer;
    private UUID uuid;
    private String username;
    @Setter
    private TriState found = TriState.NOT_SET;

    private UUID handshakeId; // The id of this handshake, for completing it upon reply

    public NetworkSwapPacket() {}
    public NetworkSwapPacket(boolean request, long nanoStartHandshake, String senderServer, String targetServer, @NotNull UUID uuid, @NotNull String username, UUID handshakeId) {
        this.request = request;
        this.nanoStartHandshake = nanoStartHandshake;
        this.senderServer = senderServer;
        this.targetServer = targetServer;
        this.uuid = uuid;
        this.username = username;
        this.handshakeId = handshakeId;
    }

}
