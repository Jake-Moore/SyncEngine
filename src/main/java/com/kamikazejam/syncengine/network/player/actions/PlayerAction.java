package com.kamikazejam.syncengine.network.player.actions;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Getter @Setter
@SuppressWarnings("unused")
public abstract class PlayerAction {
    // For Jackson
    public PlayerAction() {}

    @JsonProperty("type") // Must stay named "type" for PlayerActionDeserializer
    private PlayerActionType type;
    @JsonProperty("uuid")
    private UUID uuid;

    public PlayerAction(@NotNull PlayerActionType type, @NotNull UUID uuid) {
        this.type = type;
        this.uuid = uuid;
    }

    public abstract void perform(@NotNull Player player);

}
