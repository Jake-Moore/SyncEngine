package com.kamikazejam.syncengine.event.profile;

import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is called when a player logs into a Sync-Group (for the FIRST time ONLY)
 * This is a global event, i.e. it is called once for when a player joins a Sync-Group
 * And not called for swaps within that group
 */
@Getter
public class NetworkProfileLoginEvent extends Event {

    private final Player player;
    private final NetworkProfile profile;

    public NetworkProfileLoginEvent(Player player, NetworkProfile profile) {
        this.player = player;
        this.profile = profile;
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
