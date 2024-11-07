package com.kamikazejam.syncengine.network.player.actions.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kamikazejam.kamicommon.nms.NmsAPI;
import com.kamikazejam.syncengine.network.player.actions.PlayerAction;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ActionClearInventory extends PlayerAction {
    @JsonProperty("armorSlots")
    private boolean armorSlots = true;          // 4 slots of armor

    @JsonProperty("inventorySlots")
    private boolean inventorySlots = true;      // The upper 27 slots of the inventory

    @JsonProperty("hotbarSlots")
    private boolean hotbarSlots = true;         // The lower 9 slots of the inventory (hotbar)

    @JsonProperty("offHandSlot")
    private boolean offHandSlot = true;         // The off-hand slot

    public ActionClearInventory() {}

    @Override
    public void perform(@NotNull Player player) {
        if (armorSlots) {
            player.getInventory().setArmorContents(null);
        }
        if (inventorySlots) {
            for (int i = 9; i < 36; i++) {
                player.getInventory().setItem(i, null);
            }
        }
        if (hotbarSlots) {
            for (int i = 0; i < 9; i++) {
                player.getInventory().setItem(i, null);
            }
        }
        if (offHandSlot) {
            try {
                NmsAPI.setItemInOffHand(player, null);
            }catch (UnsupportedOperationException ignored) {}
        }
    }
}
