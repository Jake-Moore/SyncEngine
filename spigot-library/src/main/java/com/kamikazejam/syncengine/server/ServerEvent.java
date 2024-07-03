package com.kamikazejam.syncengine.server;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum ServerEvent {

    JOIN("sync-server-join"),
    PING("sync-server-ping"),
    QUIT("sync-server-quit"),
    UPDATE("sync-server-update-name"),
    ;

    private final String channel;
    public static final ServerEvent[] CACHE = values();

    ServerEvent(String channel) {
        this.channel = channel;
    }

    public static ServerEvent fromChannel(String msg) {
        for (ServerEvent e : values()) {
            if (e.channel.equalsIgnoreCase(msg)) {
                return e;
            }
        }
        return null;
    }

    private static List<String> eventsCache = null;

    public static List<String> getChannels() {
        if (eventsCache == null) {
            eventsCache = Arrays.stream(CACHE).map(ServerEvent::getChannel).toList();
        }
        return eventsCache;
    }
}
