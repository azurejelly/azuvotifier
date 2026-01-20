package com.vexsoftware.votifier.update.exception;

public class UpdateFetchException extends RuntimeException {

    public UpdateFetchException(String message) {
        super(message);
    }

    public UpdateFetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
