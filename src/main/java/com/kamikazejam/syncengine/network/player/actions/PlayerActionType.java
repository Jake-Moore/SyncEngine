package com.kamikazejam.syncengine.network.player.actions;

import com.kamikazejam.syncengine.network.player.actions.impl.ActionClearInventory;
import lombok.Getter;

@Getter
public enum PlayerActionType {
    CLEAR_INVENTORY(ActionClearInventory.class),
    ;

    public static final PlayerActionType[] CACHE = values();

    private final Class<? extends PlayerAction> clazz;
    PlayerActionType(Class<? extends PlayerAction> clazz) {
        this.clazz = clazz;
    }
}
