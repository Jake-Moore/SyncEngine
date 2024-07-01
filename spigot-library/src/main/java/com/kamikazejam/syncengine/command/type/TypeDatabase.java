package com.kamikazejam.syncengine.command.type;

import com.kamikazejam.kamicommon.command.type.TypeAbstract;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;
import com.kamikazejam.syncengine.SyncEngineAPI;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class TypeDatabase extends TypeAbstract<String> {
    private static final TypeDatabase i = new TypeDatabase();
    public TypeDatabase() { super(String.class); }
    public static TypeDatabase get() {
        return i;
    }

    @Override
    public String read(String str, CommandSender sender) throws KamiCommonException {
        @Nullable String dbName = SyncEngineAPI.getDatabases().get(str.toLowerCase());
        if (dbName == null) {
            throw new KamiCommonException().addMsg("<b>No Database matching \"<p>%s<b>\". See `/sync databases` for a list of them.", str);
        }
        return dbName;
    }

    @Override
    public Collection<String> getTabList(CommandSender commandSender, String s) {
        return SyncEngineAPI.getDatabases().values()
                .stream()
                .filter(id -> id.toLowerCase().startsWith(s.toLowerCase()))
                .limit(20)
                .toList();
    }
}
