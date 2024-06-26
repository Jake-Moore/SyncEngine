package com.kamikazejam.syncraft.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.syncraft.SyncraftPlugin;

public class CmdInfo extends KamiCommand {
    public CmdInfo() {
        addAliases("info", "id");

        addRequirements(RequirementHasPerm.get("syncraft.command.info"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7--- &6Syncraft Information&7---"));
        sender.sendMessage(StringUtil.t("&7Unique Syncraft-ID for this server:"));
        sender.sendMessage(StringUtil.t("  &6" + SyncraftPlugin.get().getSyncId()));
        sender.sendMessage(StringUtil.t("&7Syncraft Group:"));
        sender.sendMessage(StringUtil.t("  &6" + SyncraftPlugin.get().getSyncGroup()));
    }
}
