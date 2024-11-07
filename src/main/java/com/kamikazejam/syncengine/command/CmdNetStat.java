package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class CmdNetStat extends KamiCommand {
    public CmdNetStat() {
        addAliases("netstat", "netinfo");

        addRequirements(RequirementHasPerm.get("syncengine.command.netstat"));
    }

    @Override
    public void perform() {
        if (EngineSource.getSyncMode() != SyncMode.NETWORKED) {
            sender.sendMessage(StringUtil.t("&cThis command is only available in networked mode."));
            return;
        }
        StorageService service = EngineSource.getStorageMode().getStorageService();
        if (!service.canCache()) {
            sender.sendMessage(StringUtil.t("&cStorage service is not ready yet, please try again shortly."));
            return;
        }

        // Run some tests async to gather information
        sender.sendMessage(StringUtil.t("&7Gathering network information..."));

        final CommandSender sender = this.sender; // Hold onto this variable for now
        Bukkit.getScheduler().runTaskAsynchronously(EngineSource.get(), () -> {
            // Get ping to MongoDB
            StorageService storage = EngineSource.getStorageMode().getStorageService();
            long mongoPing = storage.getPingNano();

            // Get ping to Redis
            RedisService redisService = Objects.requireNonNull(EngineSource.getRedisService());
            long redisPing = redisService.getPingNano();

            // Relay information to the sender
            sender.sendMessage(StringUtil.t("&8&m-------------------------------------------------------"));
            sender.sendMessage(StringUtil.t("&7Network Information:"));
            sender.sendMessage(StringUtil.t("  &7MongoDB Ping &8- " + pingColor(mongoPing) + (mongoPing / 1_000_000L) + "ms"));
            sender.sendMessage(StringUtil.t("  &7Redis Ping   &8- " + pingColor(redisPing) + (redisPing / 1_000_000L) + "ms"));
            sender.sendMessage(StringUtil.t("&8NOTE: For best performance, ping should be under 20ms."));
            sender.sendMessage(StringUtil.t("&8&m-------------------------------------------------------"));
        });
    }

    private @NotNull String pingColor(long pingNS) {
        long ms = Math.floorDiv(pingNS, 1_000_000L);
        if (ms < 20L) {
            return "&a";
        } else if (ms < 40) {
            return "&6";
        } else {
            return "&c";
        }
    }
}
