package com.kamikazejam.syncengine.server;

import dev.morphia.annotations.Entity;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity("SyncServer")
@SuppressWarnings("unused")
public class SyncServer {

    private String name;
    private long lastPing = 0;
    private boolean online = false;
    private String group;

    public SyncServer() {
    }

    public SyncServer(String name, String group) {
        this.name = name;
        this.group = group;
    }

    public SyncServer(String name, String group, long lastPing, boolean online) {
        this.name = name;
        this.group = group;
        this.lastPing = lastPing;
        this.online = online;
    }
}
