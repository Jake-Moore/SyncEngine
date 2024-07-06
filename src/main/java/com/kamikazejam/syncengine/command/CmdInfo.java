package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import org.jetbrains.annotations.Nullable;

public class CmdInfo extends KamiCommand {
    public CmdInfo() {
        addAliases("info", "id");

        addRequirements(RequirementHasPerm.get("syncengine.command.info"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7--- &6SyncEngine Information&7---"));
        sender.sendMessage(StringUtil.t("&7Unique SyncEngine-ID for this server:"));
        sender.sendMessage(StringUtil.t("  &6" + EngineSource.getSyncServerId()));
        sender.sendMessage(StringUtil.t("&7SyncEngine Group:"));
        sender.sendMessage(StringUtil.t("  &6" + EngineSource.getSyncServerGroup()));
        sender.sendMessage(StringUtil.t("&7Storage Service:"));
        sender.sendMessage(StringUtil.t("  &6Name: " + EngineSource.getStorageMode().name()));
        String r = (EngineSource.getStorageService().canCache()) ? "&aYes" : "&cNo";
        sender.sendMessage(StringUtil.t("  &6Ready: " + r));

        @Nullable RedisService redisService = EngineSource.getRedisService();
        if (redisService != null) {
            sender.sendMessage(StringUtil.t("&7Redis Connection:"));
            String c = (redisService.getApi().isConnected()) ? "&atrue" : "&cfalse";
            sender.sendMessage(StringUtil.t("  &6Connected: " + c));
        }
    }
}
