package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.command.type.TypeDatabase;
import com.kamikazejam.syncengine.connections.redis.RedisService;
import com.kamikazejam.syncengine.server.ServerService;
import com.kamikazejam.syncengine.server.SyncServer;
import org.jetbrains.annotations.Nullable;

public class CmdServers extends KamiCommand {
    private final @Nullable ServerService serverService;
    public CmdServers(@Nullable ServerService serverService) {
        this.serverService = serverService;
        addAliases("servers");

        addRequirements(RequirementHasPerm.get("syncengine.command.servers"));

        addParameter(TypeDatabase.get(), "database");
    }

    @Override
    public void perform() throws KamiCommonException {
        String dbName = readArg();
        if (serverService == null) {
            throw new KamiCommonException().addMsg("<b>ServerService is not available.");
        }
        sender.sendMessage(StringUtil.t("&7***** &6Sync Servers in Database: " + dbName + " &7*****"));
        for (SyncServer server : serverService.getSyncServers(dbName)) {
            String online = server.isOnline() ? "&aOnline" : "&cOffline";
            sender.sendMessage(StringUtil.t("&7 - &e" + server.getName() + " &7- " + online + " &7- Last pinged &6" + convertPing(server.getLastPing())));
        }
    }

    private String convertPing(long lastPing) {
        long diff = System.currentTimeMillis() - lastPing;
        int seconds = (int) (diff / 1000);
        if (seconds > 600) {
            return "more than 10 minutes ago";
        }
        return seconds + " seconds ago";
    }
}
