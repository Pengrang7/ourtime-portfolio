package com.placeboard.common;

public class MapProviderConnectionException extends RuntimeException {
    public MapProviderConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapProviderConnectionException(String message) {
        super(message);
    }
}
