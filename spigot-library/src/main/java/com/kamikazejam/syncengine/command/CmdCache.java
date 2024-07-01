package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.command.type.TypeCache;

public class CmdCache extends KamiCommand {
    public CmdCache() {
        addAliases("cache");

        addRequirements(RequirementHasPerm.get("syncengine.command.cache"));

        addParameter(TypeCache.get(), "cache", true);
    }

    @Override
    public void perform() throws KamiCommonException {
        Cache<?,?> cache = readArg();

        sender.sendMessage(StringUtil.t("&7***** &6Sync Cache: " + cache.getName() + " &7*****"));
        sender.sendMessage(StringUtil.t("&7" + cache.getLocalCacheSize() + " objects in local cache"));
        sender.sendMessage(StringUtil.t("&7Current State: " + (cache.isRunning() ? "&aRunning" : "&cNot running")));
    }
}
