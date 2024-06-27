package com.kamikazejam.syncengine.server.event;

import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class SyncServerPublishQuitEvent extends Event {
    private final String syncID;
    private final String syncGroup;

    public SyncServerPublishQuitEvent(String syncID, String syncGroup) {
        super(false);
        this.syncID = syncID;
        this.syncGroup = syncGroup;
    }


    private static final HandlerList handlers = new HandlerList();

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
