package com.distributed.urlshortener.exception;

public class CustomAliasConflictException extends RuntimeException {
    public CustomAliasConflictException(String alias) {
        super("Custom alias '" + alias + "' is already in use");
    }
}
