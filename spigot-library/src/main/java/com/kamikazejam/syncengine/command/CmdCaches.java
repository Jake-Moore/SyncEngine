package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.base.Cache;

public class CmdCaches extends KamiCommand {
    public CmdCaches() {
        addAliases("caches");

        addRequirements(RequirementHasPerm.get("syncengine.command.caches"));
    }

    @Override
    public void perform() throws KamiCommonException {
        sender.sendMessage(StringUtil.t("&7***** &6Sync Caches &7*****"));
        for (Cache<?,?> c : SyncEngineAPI.getCaches().values()) {
            sender.sendMessage(StringUtil.t("&7" + c.getName() + " - " + c.getLocalCacheSize() + " local objects"));
        }
    }
}
