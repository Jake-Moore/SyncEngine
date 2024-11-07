package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;

public class SyncEngineCommand extends KamiCommand {
    public SyncEngineCommand() {
        addAliases("syncengine", "sync");

        addRequirements(RequirementHasPerm.get("syncengine.command.help"));

        addChild(new CmdCache());
        addChild(new CmdCaches());
        addChild(new CmdDatabases());
        addChild(new CmdInfo());
        addChild(new CmdIsOnline());
        addChild(new CmdServers());
        addChild(new CmdNetworked());
        addChild(new CmdNetStat());
    }
}
