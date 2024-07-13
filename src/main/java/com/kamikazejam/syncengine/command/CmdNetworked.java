package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.command.type.sender.TypeOfflinePlayer;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.mode.profile.network.profile.NetworkProfile;
import com.kamikazejam.syncengine.util.JacksonUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import java.util.UUID;

public class CmdNetworked extends KamiCommand {
    public CmdNetworked() {
        addAliases("networkprofile", "np", "networked");

        addParameter(TypeOfflinePlayer.get(), "player");

        addRequirements(RequirementHasPerm.get("syncengine.command.networked"));
    }

    @Override
    public void perform() throws KamiCommonException {
        OfflinePlayer target = readArg();
        UUID uuid = target.getUniqueId();

        NetworkProfile profile;
        if (target.getName() == null) {
            profile = EngineSource.getNetworkStore().getOrCreate(uuid);
        }else {
            profile = EngineSource.getNetworkStore().getOrCreate(uuid, target.getName());
        }

        msg(sender, ("&7***** &6NetworkProfile: " + uuid + " &7*****"));
        msg(sender, ("&7Username: " + profile.getUsername("@Unknown")));
        msg(sender, ("&7IsOnline: " + profile.isOnline()));
        msg(sender, ("&7Last Seen: " + profile.getLastSeenServer()));
        msg(sender, ("&7First Join: " + profile.isFirstJoinToSyncGroup()));
        msg(sender, ("&7JSON: " + JacksonUtil.serialize(profile)));
    }

    private void msg(CommandSender sender, String msg) {
        sender.sendMessage(StringUtil.t(msg));
    }
}
