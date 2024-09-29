package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.TimeUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.base.mode.SyncMode;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import org.jetbrains.annotations.Nullable;

public class CmdServers extends KamiCommand {
    public CmdServers() {
        addAliases("servers");

        addRequirements(RequirementHasPerm.get("syncengine.command.servers"));
    }

    @Override
    public void perform() throws KamiCommonException {
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (serverService == null) {
            if (EngineSource.getSyncMode() == SyncMode.STANDALONE) {
                throw new KamiCommonException().addMsg("&cServerService is not enabled since the server is running in standalone mode.");
            }else {
                throw new KamiCommonException().addMsg("&cServerService is not available. (error?)");
            }
        }

        String group = EngineSource.getSyncServerGroup();
        sender.sendMessage(StringUtil.t("&7***** &6Sync Servers Connected In Group: '" + group + "' &7*****"));
        for (SyncServer server : serverService.getSyncServers()) {
            String online = server.isOnline() ? "&aOnline" : "&cOffline";
            sender.sendMessage(StringUtil.t("&7 - &e" + server.getName() + " &7- " + online + " &7- Last pinged &6" + convertPing(server.getLastPing())));
        }
    }

    private String convertPing(long lastPing) {
        long diff = System.currentTimeMillis() - lastPing;
        long seconds = (diff / 1000L);
        return TimeUtil.getSecondsToTimeString(seconds);
    }
}
