package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.command.type.TypeDatabase;
import com.kamikazejam.syncengine.server.ServerService;
import org.jetbrains.annotations.Nullable;

public class CmdDatabase extends KamiCommand {
    public CmdDatabase() {
        addAliases("database");

        addRequirements(RequirementHasPerm.get("syncengine.command.database"));

        addParameter(TypeDatabase.get(), "database", true);
    }

    @Override
    public void perform() throws KamiCommonException {
        String dbName = readArg();

        sender.sendMessage(StringUtil.t("&7***** &6Sync Database: " + dbName + " &7*****"));
        @Nullable ServerService serverService = EngineSource.getServerService();
        if (serverService != null) {
            sender.sendMessage(StringUtil.t("&7Registered Servers: &6" + serverService.getServers(dbName).size()));
            sender.sendMessage(StringUtil.t("&7Use '/sync servers " + dbName + "' to view a list of registered servers in this database for this network"));
        }
    }
}
