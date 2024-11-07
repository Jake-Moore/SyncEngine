package com.kamikazejam.syncengine.util.struct;

import lombok.Data;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

@Data
public class DatabaseRegistration {
    private final @NotNull String databaseName; // Actual Database name (case sensitive)
    private final @NotNull Plugin owningPlugin; // Plugin that registered this database
}
