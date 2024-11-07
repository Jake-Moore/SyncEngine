package com.kamikazejam.syncengine.base.exception;

import com.kamikazejam.syncengine.util.struct.DatabaseRegistration;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
@SuppressWarnings("unused")
public class DuplicateDatabaseException extends Exception {

    private final @NotNull DatabaseRegistration existingRegistration;
    public DuplicateDatabaseException(@NotNull DatabaseRegistration existing) {
        super("A database with the name '" + existing.getDatabaseName() + "' already exists. It was registered by " + existing.getOwningPlugin().getName());
        this.existingRegistration = existing;
    }

}
