package com.distributed.urlshortener.exception;

public class UrlExpiredException extends RuntimeException {
    public UrlExpiredException(String shortCode) {
        super("Short URL for code '" + shortCode + "' has expired");
    }
}
