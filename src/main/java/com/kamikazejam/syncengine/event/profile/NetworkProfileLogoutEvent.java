package com.kamikazejam.syncengine.event.profile;

import com.kamikazejam.syncengine.networkprofile.NetworkProfile;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player disconnects from a Sync-Group
 * At the time of calling, NetworkProfile has already been marked as online=false (offline)
 * IdProfile is also marked as cachedOnline=false
 */
@Getter
public class NetworkProfileLogoutEvent extends Event {

    private final Player player;
    private final NetworkProfile profile;

    public NetworkProfileLogoutEvent(Player player, NetworkProfile profile) {
        this.player = player;
        this.profile = profile;
    }

    private static final HandlerList handlers = new HandlerList();
    public static HandlerList getHandlerList() { return handlers; }
    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
