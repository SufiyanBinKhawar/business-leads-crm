package com.blms.exception;

/**
 * Root of the application's checked-at-runtime errors (infrastructure; not drawn in D5).
 * The UI catches this type and shows {@link #getMessage()} - the message is always user-safe.
 */
public class BlmsException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BlmsException(String message) {
        super(message);
    }

    public BlmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
