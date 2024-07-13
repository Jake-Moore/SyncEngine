package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.syncengine.server.ServerService;
import org.jetbrains.annotations.Nullable;

public class SyncEngineCommand extends KamiCommand {
    public SyncEngineCommand(@Nullable ServerService serverService) {
        addAliases("syncengine", "sync");

        addRequirements(RequirementHasPerm.get("syncengine.command.help"));

        addChild(new CmdCache());
        addChild(new CmdCaches());
        addChild(new CmdDatabase());
        addChild(new CmdDatabases());
        addChild(new CmdInfo());
        addChild(new CmdIsOnline());
        addChild(new CmdServers(serverService));
        addChild(new CmdNetworked());
    }
}
