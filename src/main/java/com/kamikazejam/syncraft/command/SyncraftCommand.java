package com.kamikazejam.syncraft.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.KamiCommandVersion;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;

public class SyncraftCommand extends KamiCommand {
    public SyncraftCommand() {
        addAliases("syncraft");

        addRequirements(RequirementHasPerm.get("syncraft.command.help"));

        addChild(new KamiCommandVersion());
        addChild(new CmdInfo());
    }
}
