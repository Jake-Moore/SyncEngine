package com.kamikazejam.syncengine.base.exception;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class DuplicateDatabaseException extends Exception {

    private final String databaseName;
    public DuplicateDatabaseException(String databaseName) {
        this.databaseName = databaseName;
    }

}
