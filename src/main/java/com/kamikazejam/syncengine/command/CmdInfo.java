package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncengine.SyncEnginePlugin;

public class CmdInfo extends KamiCommand {
    public CmdInfo() {
        addAliases("info", "id");

        addRequirements(RequirementHasPerm.get("syncengine.command.info"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7--- &6SyncEngine Information&7---"));
        sender.sendMessage(StringUtil.t("&7Unique SyncEngine-ID for this server:"));
        sender.sendMessage(StringUtil.t("  &6" + SyncEnginePlugin.get().getSyncId()));
        sender.sendMessage(StringUtil.t("&7SyncEngine Group:"));
        sender.sendMessage(StringUtil.t("  &6" + SyncEnginePlugin.get().getSyncGroup()));
    }
}
