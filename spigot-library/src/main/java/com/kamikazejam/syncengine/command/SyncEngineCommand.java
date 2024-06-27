package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;

public class SyncEngineCommand extends KamiCommand {
    public SyncEngineCommand() {
        addAliases("syncengine");

        addRequirements(RequirementHasPerm.get("syncengine.command.help"));

        addChild(new CmdInfo());
    }
}
