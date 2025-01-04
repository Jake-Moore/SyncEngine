package com.kamikazejam.syncengine.command;

import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.Parameter;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.command.type.sender.TypeOfflinePlayer;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.kamicommon.util.id.IdUtilLocal;
import com.kamikazejam.syncengine.EngineSource;
import com.kamikazejam.syncengine.network.profile.NetworkProfile;
import org.bukkit.OfflinePlayer;

import java.util.Objects;

public class CmdIsOnline extends KamiCommand {
    public CmdIsOnline() {
        addAliases("isonline");

        addRequirements(RequirementHasPerm.get("syncengine.command.isonline"));

        addParameter(Parameter.of(TypeOfflinePlayer.get()).name("player"));
    }

    @Override
    public void perform() throws KamiCommonException {
        OfflinePlayer player = readArg();
        String name = Objects.requireNonNull(IdUtilLocal.getName(player.getUniqueId()).orElse(player.getName()));
        NetworkProfile profile = EngineSource.getNetworkService().getOrCreate(player.getUniqueId(), name);

        sender.sendMessage(StringUtil.t("&7***** &6NetworkProfile: " + player.getUniqueId() + " &7*****"));
        sender.sendMessage(StringUtil.t("&7Username: " + name));
        sender.sendMessage(StringUtil.t("&7Online: " + profile.isOnline()));
        sender.sendMessage(StringUtil.t("&7Last Seen: " + profile.getLastSeenServer()));
    }
}
