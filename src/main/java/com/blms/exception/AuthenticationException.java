package com.blms.exception;

/** UC01 extension: Incorrect username or password. */
public class AuthenticationException extends BlmsException {

    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
