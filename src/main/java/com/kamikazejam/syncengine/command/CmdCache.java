package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.Parameter;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.base.Cache;
import com.kamikazejam.syncengine.base.Sync;
import com.kamikazejam.syncengine.command.type.TypeCache;

import java.util.Arrays;
import java.util.List;

public class CmdCache extends KamiCommand {
    public CmdCache() {
        addAliases("cache");

        addRequirements(RequirementHasPerm.get("syncengine.command.cache"));

        addParameter(Parameter.of(TypeCache.get()).name("cache").concatFromHere(true));
    }

    @Override
    public void perform() throws KamiCommonException {
        Cache<?,?> cache = readArg();
        List<String> localCacheKeys = getSomeKeyStrings(cache);

        sender.sendMessage(StringUtil.t("&7***** &6Sync Cache: " + cache.getName() + " &7*****"));
        sender.sendMessage(StringUtil.t("&7" + cache.getLocalCacheSize() + " objects in local cache, first 10: " + Arrays.toString(localCacheKeys.toArray())));
        sender.sendMessage(StringUtil.t("&7Current State: " + (cache.isRunning() ? "&aRunning" : "&cNot running")));
    }

    @SuppressWarnings("unchecked")
    private <K, X extends Sync<K>> List<String> getSomeKeyStrings(Cache<?, ?> c) {
        Cache<K, X> cache = (Cache<K, X>) c;
        return cache.getLocalStore().getKeyStrings(cache).stream()
                .limit(10)
                .toList();
    }
}
