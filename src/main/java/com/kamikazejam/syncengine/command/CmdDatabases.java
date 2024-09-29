package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.connections.storage.StorageService;
import org.jetbrains.annotations.Nullable;

public class CmdDatabases extends KamiCommand {
    public CmdDatabases() {
        addAliases("databases", "database");

        addRequirements(RequirementHasPerm.get("syncengine.command.databases"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7***** &6Sync Database &7*****"));
        StorageService store = EngineSource.getStorageService();
        sender.sendMessage(StringUtil.t("&7Storage Service: " + ((store.canCache()) ? "&aConnected" : "&cDisconnected")));
        @Nullable RedisService redis = EngineSource.getRedisService();
        if (redis != null) {
            sender.sendMessage(StringUtil.t("&7Redis: " + (redis.getApi().isConnected() ? "&aConnected" : "&cDisconnected")));
        }
        sender.sendMessage(StringUtil.t("&7Databases:"));
        SyncEngineAPI.getDatabases().values().forEach((n) -> sender.sendMessage(StringUtil.t("&7 - " + n)));
    }
}
