package com.kamikazejam.syncengine.mode.profile.handshake.data;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@SuppressWarnings("unused")
public class HandshakeData {
    private String json;
    private long version;

    public HandshakeData() {}
    public HandshakeData(String json, long version) {
        this.json = json;
        this.version = version;
    }
}
