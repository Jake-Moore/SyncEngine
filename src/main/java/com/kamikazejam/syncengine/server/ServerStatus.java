package com.kamikazejam.syncengine.server;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ServerStatus {

    JOIN("sync-server-join"),
    PING("sync-server-ping"),
    QUIT("sync-server-quit"),
    ;

    private final String channel;
    public static final ServerStatus[] CACHE = values();

    ServerStatus(String channel) {
        this.channel = channel;
    }

    public static ServerStatus fromChannel(String msg) {
        for (ServerStatus e : CACHE) {
            if (e.channel.equalsIgnoreCase(msg)) {
                return e;
            }
        }
        return null;
    }

    private static List<String> eventsCache = null;

    public static List<String> getChannels() {
        if (eventsCache == null) {
            eventsCache = Arrays.stream(CACHE).map(ServerStatus::getChannel).toList();
        }
        return eventsCache;
    }
}
