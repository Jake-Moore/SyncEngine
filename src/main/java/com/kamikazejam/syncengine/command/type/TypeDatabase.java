package com.kamikazejam.syncengine.command.type;

import com.kamikazejam.kamicommon.command.type.TypeAbstract;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.SyncEngineAPI;
import com.kamikazejam.syncengine.util.struct.DatabaseRegistration;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public class TypeDatabase extends TypeAbstract<String> {
    private static final TypeDatabase i = new TypeDatabase();
    public TypeDatabase() { super(String.class); }
    public static TypeDatabase get() {
        return i;
    }

    @Override
    public String read(String str, CommandSender sender) throws KamiCommonException {
        @Nullable DatabaseRegistration registration = SyncEngineAPI.getDatabases().get(str.toLowerCase());
        if (registration == null) {
            throw new KamiCommonException().addMsg("<b>No Database matching \"<p>%s<b>\". See `/sync databases` for a list of them.", str);
        }
        return registration.getDatabaseName();
    }

    @Override
    public Collection<String> getTabList(CommandSender commandSender, String s) {
        return SyncEngineAPI.getDatabases().values()
                .stream()
                .filter(Objects::nonNull)
                .map(DatabaseRegistration::getDatabaseName)
                .filter(id -> id.toLowerCase().startsWith(s.toLowerCase()))
                .limit(20)
                .toList();
    }
}
