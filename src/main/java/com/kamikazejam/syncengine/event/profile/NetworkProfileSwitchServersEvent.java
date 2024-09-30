package com.kamikazejam.syncengine.event.profile;

import com.kamikazejam.syncengine.network.profile.NetworkProfile;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
public class NetworkProfileSwitchServersEvent extends Event {

    private final Player player;
    private final NetworkProfile profile;

    public NetworkProfileSwitchServersEvent(Player player, NetworkProfile profile) {
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
