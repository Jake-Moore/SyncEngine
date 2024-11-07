package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.util.struct.DatabaseRegistration;
import lombok.Getter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@Getter
@SuppressWarnings("unused")
public class DuplicateDatabaseException extends Exception {

    private final @NotNull DatabaseRegistration existingRegistration;
    public DuplicateDatabaseException(@NotNull DatabaseRegistration existing, @NotNull Plugin plugin) {
        super("Plugin '" + plugin.getName() + "' tried to register a database with the name '" + existing.getDatabaseName() + "' when it already exists. It has already been registered by '" + existing.getOwningPlugin().getName() + "'");
        this.existingRegistration = existing;
    }

}
