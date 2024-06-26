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

    private final String event;
    public static final ServerEvent[] CACHE = values();

    ServerEvent(String event) {
        this.event = event;
    }

    public static ServerEvent fromChannel(String msg) {
        for (ServerEvent e : values()) {
            if (e.event.equalsIgnoreCase(msg)) {
                return e;
            }
        }
        return null;
    }

    private static List<String> eventsCache = null;

    public static List<String> getEvents() {
        if (eventsCache == null) {
            eventsCache = Arrays.stream(CACHE).map(ServerEvent::getEvent).toList();
        }
        return eventsCache;
    }
}
