package com.david.astrodrill.network;

public class BackendException extends RuntimeException {
    public final int statusCode;
    public final String errorCode;

    public BackendException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public BackendException(int statusCode, String message) {
        this(statusCode, null, message);
    }
}
